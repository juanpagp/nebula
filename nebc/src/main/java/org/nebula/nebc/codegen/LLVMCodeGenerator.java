package org.nebula.nebc.codegen;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.nebula.nebc.ast.*;
import org.nebula.nebc.ast.declarations.*;
import org.nebula.nebc.ast.expressions.*;
import org.nebula.nebc.ast.patterns.*;
import org.nebula.nebc.ast.statements.*;
import org.nebula.nebc.ast.tags.TagAtom;
import org.nebula.nebc.ast.tags.TagOperation;
import org.nebula.nebc.ast.tags.TagStatement;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.semantic.SemanticAnalyzer;
import org.nebula.nebc.semantic.symbol.MethodSymbol;
import org.nebula.nebc.semantic.symbol.Symbol;
import org.nebula.nebc.semantic.symbol.TypeSymbol;
import org.nebula.nebc.semantic.symbol.VariableSymbol;
import org.nebula.nebc.semantic.SymbolTable;
import org.nebula.nebc.semantic.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.bytedeco.llvm.global.LLVM.*;

/**
 * LLVM IR code generator for the Nebula language.
 * <p>
 * Implements {@link ASTVisitor}{@code <LLVMValueRef>} and walks a
 * semantically-validated AST to emit LLVM IR using the bytedeco LLVM C API
 * bindings. The visitor returns the {@link LLVMValueRef} produced by each node
 * (or {@code null} for nodes that do not produce a value, e.g. declarations and
 * statements).
 *
 * <h3>Lifecycle</h3>
 * <ol>
 * <li>Construct with {@link #LLVMCodeGenerator()}.</li>
 * <li>Call {@link #generate(List, SemanticAnalyzer)} which creates the LLVM
 * context, module, and builder, walks every compilation unit, emits the
 * {@code main} entry-point wrapper, verifies the module, and returns the
 * module ref.</li>
 * <li>The caller is responsible for disposing the module and context via
 * {@link #dispose()} after the object file has been emitted.</li>
 * </ol>
 */
public class LLVMCodeGenerator implements ASTVisitor<LLVMValueRef>
{
	/**
	 * Tracks requested memory allocations (alloca pointers) per function for local
	 * variables.
	 */
	private final Map<String, LLVMValueRef> namedValues = new HashMap<>();
	/**
	 * Names of variables whose namedValues entry is an inline struct alloca (alloca %T),
	 * as opposed to an alloca-of-ptr (alloca ptr) that holds a pointer to the struct.
	 * For inline struct vars, visitIdentifierExpression returns the alloca directly
	 * (it IS the ptr-to-struct for method dispatch) rather than loading through it.
	 */
	private final Set<String> inlineStructVars = new HashSet<>();
	/**
	 * Compile-time element count of array variables declared in the current function scope.
	 * Populated when a variable is initialised from an {@link ArrayLiteralExpression}.
	 * Used by {@link #visitForeachStatement} to generate iteration bounds.
	 */
	private final Map<String, Integer> arrayElementCounts = new HashMap<>();
	private final Map<String, LLVMValueRef> specializations = new HashMap<>();
	// ── LLVM Core Handles ───────────────────────────────────────
	private LLVMContextRef context;
	private LLVMModuleRef module;
	private LLVMBuilderRef builder;
	// ── Codegen State ───────────────────────────────────────────
	private SemanticAnalyzer analyzer;
	/**
	 * The LLVM function currently being built (set during visitMethodDeclaration).
	 */
	private LLVMValueRef currentFunction;
	private Type currentMethodReturnType;
	/**
	 * Whether the current basic block has already been terminated (ret/br).
	 */
	private boolean currentBlockTerminated;
	private Substitution currentSubstitution = null;
	/** Target basic block for 'break' in the innermost loop. */
	private LLVMBasicBlockRef currentLoopExitBB = null;
	/** Target basic block for 'continue' in the innermost loop (latch or header). */
	private LLVMBasicBlockRef currentLoopContinueBB = null;

	// Enum / tagged-union discriminant tables.
	// Key: "TypeName.VariantName", Value: integer discriminant (0-based).
	private final Map<String, Integer> enumDiscriminants  = new HashMap<>();
	private final Map<String, Integer> unionDiscriminants = new HashMap<>();
	// Ordered variant names per union (needed for switch generation).
	private final Map<String, List<String>> unionVariantOrder = new HashMap<>();

	public LLVMCodeGenerator()
	{
	}

	// =================================================================
	// PUBLIC API
	// =================================================================

	private LLVMValueRef emitCast(LLVMValueRef value, Type srcSemType, Type targetSemType)
	{
		if (value == null || srcSemType == null || targetSemType == null || srcSemType.equals(targetSemType))
			return value;

		// Optional coercions
		if (targetSemType instanceof OptionalType targetOpt)
		{
			// none (opt.<any>) being assigned to a concrete T? → emit typed none
			if (srcSemType instanceof OptionalType srcOpt && srcOpt.innerType == Type.ANY)
			{
				return emitNoneOfType(targetOpt);
			}
			// Plain T being assigned to T? → wrap in { true, value }
			if (!(srcSemType instanceof OptionalType))
			{
				return emitWrapInOptional(value, srcSemType, targetOpt);
			}
		}

		LLVMTypeRef targetType = toLLVMType(targetSemType);

		if (srcSemType instanceof PrimitiveType src && targetSemType instanceof PrimitiveType target)
		{
			if (src.isInteger() && target.isInteger())
			{
				int srcWidth = src.getBitWidth();
				int targetWidth = target.getBitWidth();

				if (srcWidth > targetWidth)
				{
					return LLVMBuildTrunc(builder, value, targetType, "trunc");
				}
				else if (srcWidth < targetWidth)
				{
					boolean isUnsigned = src.name().startsWith("u");
					return isUnsigned ? LLVMBuildZExt(builder, value, targetType, "zext") : LLVMBuildSExt(builder, value, targetType, "sext");
				}
			}
			else if (src.isFloat() && target.isFloat())
			{
				int srcWidth = src.getBitWidth();
				int targetWidth = target.getBitWidth();

				if (srcWidth > targetWidth)
				{
					return LLVMBuildFPTrunc(builder, value, targetType, "fptrunc");
				}
				else if (srcWidth < targetWidth)
				{
					return LLVMBuildFPExt(builder, value, targetType, "fpext");
				}
			}
			else if (src.isInteger() && target.isFloat())
			{
				boolean isUnsigned = src.name().startsWith("u");
				return isUnsigned ? LLVMBuildUIToFP(builder, value, targetType, "uitofp") : LLVMBuildSIToFP(builder, value, targetType, "sitofp");
			}
			else if (src.isFloat() && target.isInteger())
			{
				boolean targetUnsigned = target.name().startsWith("u");
				return targetUnsigned ? LLVMBuildFPToUI(builder, value, targetType, "fptoui") : LLVMBuildFPToSI(builder, value, targetType, "fptosi");
			}
		}

		return value;
	}

	// ── Type Analysis Utilities ──────────────────────────────────────
	private boolean isFloatType(Type type)
	{
		return type instanceof PrimitiveType p && p.isFloat();
	}

	private boolean isUnsignedType(Type type)
	{
		return type instanceof PrimitiveType p && p.name().startsWith("u");
	}

	private boolean isIntegerType(Type type)
	{
		return type instanceof PrimitiveType p && p.isInteger();
	}

	private int getBitWidth(Type type)
	{
		return type instanceof PrimitiveType p ? p.getBitWidth() : 0;
	}

	private Type getPromotedType(Type left, Type right)
	{
		if (left.equals(right))
			return left;
		if (left instanceof PrimitiveType pLeft && right instanceof PrimitiveType pRight)
		{
			if (pLeft.isFloat() || pRight.isFloat())
			{
				if (pLeft.isFloat() && pRight.isFloat())
				{
					return pLeft.getBitWidth() >= pRight.getBitWidth() ? pLeft : pRight;
				}
				return pLeft.isFloat() ? pLeft : pRight;
			}
			if (pLeft.isInteger() && pRight.isInteger())
			{
				if (pLeft.getBitWidth() > pRight.getBitWidth())
					return pLeft;
				if (pRight.getBitWidth() > pLeft.getBitWidth())
					return pRight;
				// Same width: if one is signed, prefer signed (match SemanticAnalyzer logic)
				boolean leftUnsigned = pLeft.name().startsWith("u");
				boolean rightUnsigned = pRight.name().startsWith("u");
				if (leftUnsigned && !rightUnsigned)
					return pRight;
				return pLeft;
			}
		}
		return left;
	}

	/**
	 * Generates an LLVM module from the semantically-validated compilation units.
	 *
	 * @param units    The list of AST compilation units to emit.
	 * @param analyzer The semantic analyzer that holds entry-point metadata.
	 * @return The populated and verified {@link LLVMModuleRef}.
	 * @throws CodegenException if the module fails verification.
	 */
	public LLVMModuleRef generate(List<CompilationUnit> units, SemanticAnalyzer analyzer)
	{
		this.analyzer = analyzer;
		LLVMTypeMapper.clearCache();

		// 1. Initialise LLVM infrastructure
		context = LLVMContextCreate();
		module = LLVMModuleCreateWithNameInContext("nebula_module", context);
		builder = LLVMCreateBuilderInContext(context);

		// 2. Visit every compilation unit
		for (CompilationUnit cu : units)
		{
			cu.accept(this);
		}

		// 4. Verify the module (removed from here to allow IR dump in Compiler.java even if invalid)
		// verifyModule();

		// verifyModule();

		return module;
	}

	/**
	 * Returns the LLVM module, only valid after {@link #generate} has been called.
	 */
	public LLVMModuleRef getModule()
	{
		return module;
	}

	/**
	 * Returns the LLVM context, only valid after {@link #generate} has been called.
	 */
	public LLVMContextRef getContext()
	{
		return context;
	}

	/**
	 * Prints the LLVM IR of the module to a string.
	 * Useful for verbose / debug output.
	 */
	public String dumpIR()
	{
		BytePointer ir = LLVMPrintModuleToString(module);
		String result = ir.getString();
		LLVMDisposeMessage(ir);
		return result;
	}

	/**
	 * Disposes all LLVM resources. Must be called after the object file has
	 * been emitted.
	 */
	public void dispose()
	{
		if (builder != null)
		{
			LLVMDisposeBuilder(builder);
			builder = null;
		}
		if (module != null)
		{
			LLVMDisposeModule(module);
			module = null;
		}
		if (context != null)
		{
			LLVMContextDispose(context);
			context = null;
		}
	}

	// =================================================================
	// MODULE VERIFICATION
	// =================================================================

	public void verifyModule()
	{
		BytePointer errorMsg = new BytePointer();
		if (LLVMVerifyModule(module, LLVMPrintMessageAction, errorMsg) != 0)
		{
			String msg = errorMsg.getString();
			LLVMDisposeMessage(errorMsg);
			throw new CodegenException("LLVM module verification failed:\n" + msg);
		}
		LLVMDisposeMessage(errorMsg);
	}

	// =================================================================
	// UTILITY METHODS
	// =================================================================

	/**
	 * Maps a Nebula {@link Type} to an {@link LLVMTypeRef} in the current context.
	 * Applies the current substitution if one is active.
	 */
	private LLVMTypeRef toLLVMType(Type type)
	{
		Type substituted = (currentSubstitution != null) ? currentSubstitution.substitute(type) : type;
		return LLVMTypeMapper.map(context, substituted);
	}

	// =================================================================
	// DECLARATIONS
	// =================================================================

	@Override
	public LLVMValueRef visitCompilationUnit(CompilationUnit node)
	{
		for (ASTNode decl : node.declarations)
		{
			decl.accept(this);
		}
		return null;
	}

	@Override
	public LLVMValueRef visitExternDeclaration(ExternDeclaration node)
	{
		for (MethodDeclaration member : node.members)
		{
			member.accept(this);
		}
		return null;
	}

	@Override
	public LLVMValueRef visitNamespaceDeclaration(NamespaceDeclaration node)
	{
		// Namespaces are a semantic-only concept — just recurse into members
		for (ASTNode member : node.members)
		{
			member.accept(this);
		}
		return null;
	}

	@Override
	public LLVMValueRef visitMethodDeclaration(MethodDeclaration node)
	{
		// 1. Retrieve the pre-resolved symbol from the analyzer
		MethodSymbol symbol = analyzer.getSymbol(node, MethodSymbol.class);
		if (symbol == null)
		{
			throw new CodegenException("Internal Error: Method " + node.name + " was never semantically validated.");
		}

		// If this is a generic method, skip normal emission unless we are specializing.
		if (!symbol.getTypeParameters().isEmpty() && currentSubstitution == null)
		{
			return null;
		}

		// 2. Extract types directly from the symbol
		FunctionType funcType = symbol.getType();
		Type returnType = funcType.getReturnType();
		LLVMTypeRef llvmFuncType = toLLVMType(funcType);

		// 4. Add the function to the module (or retrieve if already declared)
		String funcName = (currentSubstitution != null) ? getSpecializationName(symbol) : symbol.getMangledName();
		LLVMValueRef function = LLVMGetNamedFunction(module, funcName);
		if (function == null || function.isNull())
		{
			function = LLVMAddFunction(module, funcName, llvmFuncType);
		}
		else if (LLVMCountBasicBlocks(function) > 0)
		{
			// If already emitted with a body (could happen if specialization called twice in same unit)
			return function;
		}

		LLVMSetLinkage(function, LLVMExternalLinkage);
		if (symbol.isExtern())
		{
			return function;
		}

		// 5. Setup Entry Block
		LLVMBasicBlockRef prevInsertBlock = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef entryBB = LLVMAppendBasicBlockInContext(context, function, "entry");
		LLVMPositionBuilderAtEnd(builder, entryBB);

		// 6. Manage Codegen State
		LLVMValueRef prevFunction = currentFunction;
		boolean prevTerminated = currentBlockTerminated;
		currentFunction = function;
		currentBlockTerminated = false;
		Type prevReturnType = currentMethodReturnType;
		currentMethodReturnType = returnType;

		Map<String, LLVMValueRef> prevNamedValues = new HashMap<>(namedValues);
		Set<String> prevInlineStructVars = new HashSet<>(inlineStructVars);
		Map<String, Integer> prevArrayElementCounts = new HashMap<>(arrayElementCounts);
		namedValues.clear();
		inlineStructVars.clear();
		arrayElementCounts.clear();

		// 6.5. Allocate and bind parameters to namedValues
		int llvmParamIdx = 0;
		// If this is a member method (represented by having 'this' in the FunctionType),
		// bind the first LLVM parameter to "this".
		if (funcType.parameterTypes.size() > node.parameters.size())
		{
			LLVMValueRef thisValue = LLVMGetParam(function, llvmParamIdx++);
			Type thisType = funcType.parameterTypes.get(0);
			LLVMValueRef alloca = LLVMBuildAlloca(builder, toLLVMType(thisType), "this");
			LLVMBuildStore(builder, thisValue, alloca);
			namedValues.put("this", alloca);
		}

		for (int i = 0; i < node.parameters.size(); i++)
		{
			Parameter param = node.parameters.get(i);
			LLVMValueRef paramValue = LLVMGetParam(function, llvmParamIdx++);

			// Allocate space for the parameter
			// The parameter type index accounts for 'this' if this is a member method
			int paramTypeIdx = llvmParamIdx - 1;
			if (paramTypeIdx >= funcType.parameterTypes.size())
			{
				throw new CodegenException("Parameter index out of bounds for function: " + node.name);
			}
			Type paramType = funcType.parameterTypes.get(paramTypeIdx);
			LLVMValueRef alloca = LLVMBuildAlloca(builder, toLLVMType(paramType), param.name());
			LLVMBuildStore(builder, paramValue, alloca);
			namedValues.put(param.name(), alloca);
		}

		// 7. Emit Body
		LLVMValueRef bodyResult = null;
		if (node.body != null)
		{
			bodyResult = node.body.accept(this);
		}

		// 8. Handle Implicit Returns (Now using the verified returnType object)
		if (!currentBlockTerminated)
		{
			if (returnType == PrimitiveType.VOID)
			{
				LLVMBuildRetVoid(builder);
			}
			else if (bodyResult != null)
			{
				Type bodySemType = (node.body instanceof ExpressionBlock eb) ? analyzer.getType(eb) : analyzer.getType(node.body);
				LLVMValueRef castedResult = emitCoerceToReturnType(bodyResult, bodySemType, returnType);
				LLVMBuildRet(builder, castedResult);
			}
			else
			{
				LLVMBuildRet(builder, LLVMGetUndef(toLLVMType(returnType)));
			}
		}

		// 9. Restore State
		currentFunction = prevFunction;
		currentBlockTerminated = prevTerminated;
		currentMethodReturnType = prevReturnType;
		namedValues.clear();
		namedValues.putAll(prevNamedValues);
		inlineStructVars.clear();
		inlineStructVars.addAll(prevInlineStructVars);
		arrayElementCounts.clear();
		arrayElementCounts.putAll(prevArrayElementCounts);
		if (prevInsertBlock != null)
		{
			LLVMPositionBuilderAtEnd(builder, prevInsertBlock);
		}

		return function;
	}

	@Override
	public LLVMValueRef visitImplDeclaration(ImplDeclaration node)
	{
		for (MethodDeclaration method : node.members)
		{
			method.accept(this);
		}
		return null;
	}

	@Override
	public LLVMValueRef visitClassDeclaration(ClassDeclaration node)
	{
		// Generic classes are only emitted when monomorphized — skip template declarations
		if (node.typeParams != null && !node.typeParams.isEmpty())
			return null;
		for (ASTNode member : node.members)
		{
			if (member instanceof MethodDeclaration || member instanceof ConstructorDeclaration)
			{
				member.accept(this);
			}
		}
		return null;
	}

	@Override
	public LLVMValueRef visitStructDeclaration(StructDeclaration node)
	{
		// Generic structs are only emitted when monomorphized — skip template declarations
		if (node.typeParams != null && !node.typeParams.isEmpty())
			return null;
		for (ASTNode member : node.members)
		{
			if (member instanceof MethodDeclaration || member instanceof ConstructorDeclaration
					|| member instanceof OperatorDeclaration)
			{
				member.accept(this);
			}
		}
		return null;
	}

	@Override
	public LLVMValueRef visitVariableDeclaration(VariableDeclaration node)
	{
		for (VariableDeclarator decl : node.declarators)
		{
			String varName = decl.name();
			Type type = resolveDeclaratorType(node);

			if (type instanceof StructType structTy)
			{
				// Structs are value types: allocate the full struct inline on the stack.
				// namedValues[varName] holds the alloca pointer (a ptr to the struct data),
				// which is passed directly as 'self' in method calls — no load needed.
				LLVMTypeRef structLlvmType = LLVMTypeMapper.getOrCreateStructType(context, structTy);
				LLVMValueRef alloca = LLVMBuildAlloca(builder, structLlvmType, varName);
				if (decl.hasInitializer())
				{
					LLVMValueRef initVal = decl.initializer().accept(this);
					if (initVal != null)
					{
						// Operator/method calls return a ptr to a (possibly short-lived) alloca.
						// Load the struct value through the pointer immediately so we get a
						// fresh copy in our own alloca before any subsequent call clobbers the
						// source frame.  Constructor calls already produce a struct value
						// (via emitConstructorCall's load), so no extra load is needed for them.
						if (LLVMGetTypeKind(LLVMTypeOf(initVal)) == LLVMPointerTypeKind)
						{
							initVal = LLVMBuildLoad2(builder, structLlvmType, initVal, "struct_copy");
						}
						LLVMBuildStore(builder, initVal, alloca);
					}
				}
				namedValues.put(varName, alloca);
				inlineStructVars.add(varName);
			}
			else
			{
				// Primitives, class references, pointers: standard alloca-of-the-LLVM-type.
				LLVMValueRef alloca = LLVMBuildAlloca(builder, toLLVMType(type), varName);
				if (decl.hasInitializer())
				{
					LLVMValueRef initVal = decl.initializer().accept(this);
					if (initVal != null)
					{
						LLVMValueRef castedVal = emitCast(initVal, analyzer.getType(decl.initializer()), type);
						LLVMBuildStore(builder, castedVal, alloca);
					}
					// Track array element counts for foreach iteration bounds.
					if (type instanceof ArrayType && decl.initializer() instanceof ArrayLiteralExpression ale)
					{
						arrayElementCounts.put(varName, ale.elements.size());
					}
				}
				namedValues.put(varName, alloca);
			}
		}
		return null;
	}

	private Type resolveDeclaratorType(VariableDeclaration node)
	{
		org.nebula.nebc.semantic.symbol.Symbol sym = analyzer.getSymbol(node, org.nebula.nebc.semantic.symbol.VariableSymbol.class);
		return sym != null ? sym.getType() : PrimitiveType.I32;
	}

	@Override
	public LLVMValueRef visitConstDeclaration(ConstDeclaration node)
	{
		// Delegate to the inner variable declaration.
		// Inside a function: emitted as alloca (the 'const' constraint is semantic-only).
		// At top-level (currentFunction == null): emitted as a global constant.
		if (currentFunction == null || currentFunction.isNull())
		{
			for (VariableDeclarator decl : node.declaration.declarators)
			{
				String varName = decl.name();
				Type type = resolveDeclaratorType(node.declaration);
				LLVMTypeRef llvmType = toLLVMType(type);

				LLVMValueRef globalVar = LLVMGetNamedGlobal(module, varName);
				if (globalVar == null || globalVar.isNull())
				{
					globalVar = LLVMAddGlobal(module, llvmType, varName);
				}

				LLVMValueRef initVal = null;
				if (decl.hasInitializer())
				{
					// For global consts the initializer must be a constant expression.
					// Visiting a LiteralExpression in codegen always returns an LLVM constant.
					initVal = decl.initializer().accept(this);
				}

				if (initVal == null)
				{
					initVal = LLVMGetUndef(llvmType);
				}

				LLVMSetInitializer(globalVar, initVal);
				LLVMSetGlobalConstant(globalVar, 1);
				LLVMSetLinkage(globalVar, LLVMInternalLinkage);
				namedValues.put(varName, globalVar);
			}
			return null;
		}

		// Local const — same as var
		return visitVariableDeclaration(node.declaration);
	}

	@Override
	public LLVMValueRef visitTraitDeclaration(TraitDeclaration node)
	{
		// Traits are a semantic-only concept — default impls emitted via ImplDeclaration
		return null;
	}


	@Override
	public LLVMValueRef visitEnumDeclaration(EnumDeclaration node)
	{
		// Register each variant as an i32 discriminant in the lookup table.
		// No IR is emitted at the declaration site — enum member accesses produce
		// inline i32 constants.
		for (int i = 0; i < node.variants.size(); i++)
		{
			enumDiscriminants.put(node.name + "." + node.variants.get(i), i);
		}
		return null;
	}

	@Override
	public LLVMValueRef visitUnionDeclaration(UnionDeclaration node)
	{
		// Register discriminant indices and record variant order for match codegen.
		List<String> order = new ArrayList<>();
		for (int i = 0; i < node.variants.size(); i++)
		{
			String key = node.name + "." + node.variants.get(i).name;
			unionDiscriminants.put(key, i);
			order.add(node.variants.get(i).name);
		}
		unionVariantOrder.put(node.name, order);

		// Look up the union type from the semantic analyser
		Type semType = null;
		{
			org.nebula.nebc.semantic.symbol.Symbol sym = analyzer.getSymbol(node, org.nebula.nebc.semantic.symbol.TypeSymbol.class);
			if (sym != null)
				semType = sym.getType();
		}
		if (!(semType instanceof UnionType ut))
			return null;

		// Emit constructor functions for variants that carry a payload.
		for (int i = 0; i < node.variants.size(); i++)
		{
			UnionVariant variant = node.variants.get(i);
			if (variant.payload != null)
			{
				emitUnionVariantConstructor(ut, variant, i);
			}
		}

		return null;
	}

	/**
	 * Emits an LLVM function for a payload-carrying union variant constructor.
	 *
	 * <pre>
	 *   %union.Shape @Circle(float %radius) {
	 *     %tmp = alloca %union.Shape
	 *     %tag = getelementptr %union.Shape, ptr %tmp, 0, 0
	 *     store i32 0, ptr %tag
	 *     %payload = getelementptr %union.Shape, ptr %tmp, 0, 1
	 *     %payload_ptr = bitcast ptr %payload to ptr   ; cast to f32*
	 *     store float %radius, ptr %payload_ptr
	 *     %result = load %union.Shape, ptr %tmp
	 *     ret %union.Shape %result
	 *   }
	 * </pre>
	 */
	private void emitUnionVariantConstructor(UnionType ut, UnionVariant variant, int discriminant)
	{
		// Look up the MethodSymbol registered for this variant
		Symbol sym = ut.getMemberScope().resolveLocal(variant.name);
		if (!(sym instanceof MethodSymbol ms))
			return;

		String funcName = ms.getMangledName();

		// Skip if already defined
		LLVMValueRef existing = LLVMGetNamedFunction(module, funcName);
		if (existing != null && !existing.isNull() && LLVMCountBasicBlocks(existing) > 0)
			return;

		FunctionType funcType = ms.getType();
		LLVMTypeRef llvmFuncType = toLLVMType(funcType);

		LLVMValueRef function = existing;
		if (function == null || function.isNull())
		{
			function = LLVMAddFunction(module, funcName, llvmFuncType);
		}
		LLVMSetLinkage(function, LLVMExternalLinkage);

		LLVMBasicBlockRef prevBlock = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef entryBB = LLVMAppendBasicBlockInContext(context, function, "entry");
		LLVMPositionBuilderAtEnd(builder, entryBB);

		LLVMTypeRef i32t = LLVMInt32TypeInContext(context);
		LLVMTypeRef unionStructType = LLVMTypeMapper.getOrCreateUnionStructType(context, ut);

		// Allocate union struct on stack
		LLVMValueRef unionAlloca = LLVMBuildAlloca(builder, unionStructType, "union_ctor");

		// Store discriminant tag
		LLVMValueRef tagGep = LLVMBuildStructGEP2(builder, unionStructType, unionAlloca, 0, "tag");
		LLVMBuildStore(builder, LLVMConstInt(i32t, discriminant, 0), tagGep);

		// Store payload into the payload buffer (field 1 = byte array)
		if (funcType.parameterTypes.size() >= 1)
		{
			Type payloadSemType = funcType.parameterTypes.get(0);
			LLVMTypeRef payloadLLVMType = toLLVMType(payloadSemType);
			LLVMValueRef payloadParam = LLVMGetParam(function, 0);

			LLVMValueRef payloadGep = LLVMBuildStructGEP2(builder, unionStructType, unionAlloca, 1, "payload");
			// The payload GEP gives us a pointer to the byte buffer; store the param through it
			LLVMBuildStore(builder, payloadParam, payloadGep);
		}

		// Load and return the union value
		LLVMValueRef result = LLVMBuildLoad2(builder, unionStructType, unionAlloca, "union_val");
		LLVMBuildRet(builder, result);

		if (prevBlock != null && !prevBlock.isNull())
		{
			LLVMPositionBuilderAtEnd(builder, prevBlock);
		}
	}

	@Override
	public LLVMValueRef visitUnionVariant(UnionVariant node)
	{
		// Handled by visitUnionDeclaration / emitUnionVariantConstructor.
		return null;
	}

	@Override
	public LLVMValueRef visitOperatorDeclaration(OperatorDeclaration node)
	{
		// Operator overloads are emitted as regular LLVM functions using the mangled
		// name recorded by the semantic analyser.  If the analyser has associated a
		// MethodSymbol with this node we use that symbol (and its getMangledName());
		// otherwise we fall back to synthesising a name from the enclosing type.
		MethodSymbol symbol = analyzer.getSymbol(node, MethodSymbol.class);
		if (symbol == null)
		{
			// No semantic symbol was attached (e.g. analyser stub).  Skip silently.
			return null;
		}

		FunctionType funcType = symbol.getType();
		Type returnType = funcType.getReturnType();
		LLVMTypeRef llvmFuncType = toLLVMType(funcType);
		String funcName = symbol.getMangledName();

		LLVMValueRef function = LLVMGetNamedFunction(module, funcName);
		if (function == null || function.isNull())
		{
			function = LLVMAddFunction(module, funcName, llvmFuncType);
		}
		else if (LLVMCountBasicBlocks(function) > 0)
		{
			return function;
		}

		LLVMSetLinkage(function, LLVMExternalLinkage);

		LLVMBasicBlockRef prevInsertBlock = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef entryBB = LLVMAppendBasicBlockInContext(context, function, "entry");
		LLVMPositionBuilderAtEnd(builder, entryBB);

		LLVMValueRef prevFunction = currentFunction;
		boolean prevTerminated = currentBlockTerminated;
		currentFunction = function;
		currentBlockTerminated = false;
		Type prevReturnType = currentMethodReturnType;
		currentMethodReturnType = returnType;

		Map<String, LLVMValueRef> prevNamedValues = new HashMap<>(namedValues);
		Set<String> prevInlineStructVars = new HashSet<>(inlineStructVars);
		Map<String, Integer> prevArrayElementCounts = new HashMap<>(arrayElementCounts);
		namedValues.clear();
		inlineStructVars.clear();
		arrayElementCounts.clear();

		// Bind parameters (first param is 'this' when inside an impl/class)
		int llvmParamIdx = 0;
		if (funcType.parameterTypes.size() > node.parameters.size())
		{
			LLVMValueRef thisValue = LLVMGetParam(function, llvmParamIdx++);
			Type thisType = funcType.parameterTypes.get(0);
			LLVMValueRef alloca = LLVMBuildAlloca(builder, toLLVMType(thisType), "this");
			LLVMBuildStore(builder, thisValue, alloca);
			namedValues.put("this", alloca);
		}

		for (int i = 0; i < node.parameters.size(); i++)
		{
			Parameter param = node.parameters.get(i);
			LLVMValueRef paramValue = LLVMGetParam(function, llvmParamIdx++);
			Type paramType = funcType.parameterTypes.get(llvmParamIdx - 1);
			LLVMValueRef alloca = LLVMBuildAlloca(builder, toLLVMType(paramType), param.name());
			LLVMBuildStore(builder, paramValue, alloca);
			namedValues.put(param.name(), alloca);
		}

		LLVMValueRef bodyResult = null;
		if (node.body != null)
		{
			bodyResult = node.body.accept(this);
		}

		if (!currentBlockTerminated)
		{
			if (returnType == PrimitiveType.VOID)
			{
				LLVMBuildRetVoid(builder);
			}
			else if (bodyResult != null)
			{
				Type bodyType = null;
				if (node.body != null)
					bodyType = analyzer.getType(node.body);
				if (bodyType == null)
					bodyType = returnType;
				LLVMValueRef coerced = emitCoerceToReturnType(bodyResult, bodyType, returnType);
				LLVMBuildRet(builder, coerced);
			}
			else
			{
				LLVMBuildRet(builder, LLVMGetUndef(toLLVMType(returnType)));
			}
		}

		currentFunction = prevFunction;
		currentBlockTerminated = prevTerminated;
		currentMethodReturnType = prevReturnType;
		namedValues.clear();
		namedValues.putAll(prevNamedValues);
		inlineStructVars.clear();
		inlineStructVars.addAll(prevInlineStructVars);
		arrayElementCounts.clear();
		arrayElementCounts.putAll(prevArrayElementCounts);
		if (prevInsertBlock != null)
		{
			LLVMPositionBuilderAtEnd(builder, prevInsertBlock);
		}

		return function;
	}

	@Override
	public LLVMValueRef visitConstructorDeclaration(ConstructorDeclaration node)
	{
		MethodSymbol symbol = analyzer.getSymbol(node, MethodSymbol.class);
		if (symbol == null)
		{
			throw new CodegenException("Internal Error: Constructor " + node.name + " was never semantically validated.");
		}

		// If this is a generic method, skip normal emission unless we are specializing.
		if (!symbol.getTypeParameters().isEmpty() && currentSubstitution == null)
		{
			return null;
		}

		FunctionType funcType = symbol.getType();
		Type returnType = funcType.getReturnType();
		LLVMTypeRef llvmFuncType = toLLVMType(funcType);

		String funcName = (currentSubstitution != null) ? getSpecializationName(symbol) : symbol.getMangledName();
		LLVMValueRef function = LLVMGetNamedFunction(module, funcName);
		if (function == null || function.isNull())
		{
			function = LLVMAddFunction(module, funcName, llvmFuncType);
		}
		else if (LLVMCountBasicBlocks(function) > 0)
		{
			return function;
		}

		LLVMSetLinkage(function, LLVMExternalLinkage);

		LLVMBasicBlockRef prevInsertBlock = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef entryBB = LLVMAppendBasicBlockInContext(context, function, "entry");
		LLVMPositionBuilderAtEnd(builder, entryBB);

		LLVMValueRef prevFunction = currentFunction;
		boolean prevTerminated = currentBlockTerminated;
		currentFunction = function;
		currentBlockTerminated = false;
		Type prevReturnType = currentMethodReturnType;
		currentMethodReturnType = returnType;

		Map<String, LLVMValueRef> prevNamedValues = new HashMap<>(namedValues);
		Set<String> prevInlineStructVars = new HashSet<>(inlineStructVars);
		Map<String, Integer> prevArrayElementCounts = new HashMap<>(arrayElementCounts);
		namedValues.clear();
		inlineStructVars.clear();
		arrayElementCounts.clear();

		int llvmParamIdx = 0;
		LLVMValueRef thisValue = LLVMGetParam(function, llvmParamIdx++);
		Type thisType = funcType.parameterTypes.get(0);
		LLVMValueRef thisAlloca = LLVMBuildAlloca(builder, toLLVMType(thisType), "this");
		LLVMBuildStore(builder, thisValue, thisAlloca);
		namedValues.put("this", thisAlloca);

		for (int i = 0; i < node.parameters.size(); i++)
		{
			Parameter param = node.parameters.get(i);
			LLVMValueRef paramValue = LLVMGetParam(function, llvmParamIdx++);

			// Allocate space for the parameter
			// The parameter type index accounts for 'this' as the first parameter
			int paramTypeIdx = llvmParamIdx - 1;
			if (paramTypeIdx >= funcType.parameterTypes.size())
			{
				throw new CodegenException("Parameter index out of bounds for method: " + node.name);
			}
			Type paramType = funcType.parameterTypes.get(paramTypeIdx);
			LLVMValueRef alloca = LLVMBuildAlloca(builder, toLLVMType(paramType), param.name());
			LLVMBuildStore(builder, paramValue, alloca);
			namedValues.put(param.name(), alloca);
		}

		if (node.body != null)
		{
			// Before body, initialize fields that have default values
			Symbol owner = symbol.getDefinedIn().getOwner();
			if (owner instanceof TypeSymbol ts && ts.getDeclarationNode() instanceof ClassDeclaration cd)
			{
				initializeFields(thisValue, (ClassType) ts.getType(), cd.members);
			}
			else if (owner instanceof TypeSymbol ts && ts.getDeclarationNode() instanceof StructDeclaration sd)
			{
				initializeFields(thisValue, (StructType) ts.getType(), sd.members);
			}

			node.body.accept(this);
		}

		if (!currentBlockTerminated)
		{
			LLVMBuildRetVoid(builder); // constructors always return void
		}

		currentFunction = prevFunction;
		currentBlockTerminated = prevTerminated;
		currentMethodReturnType = prevReturnType;
		namedValues.clear();
		namedValues.putAll(prevNamedValues);
		inlineStructVars.clear();
		inlineStructVars.addAll(prevInlineStructVars);
		arrayElementCounts.clear();
		arrayElementCounts.putAll(prevArrayElementCounts);
		if (prevInsertBlock != null)
		{
			LLVMPositionBuilderAtEnd(builder, prevInsertBlock);
		}
		return function;
	}

	// =================================================================
	// STATEMENTS
	// =================================================================

	@Override
	public LLVMValueRef visitStatementBlock(StatementBlock node)
	{
		for (Statement stmt : node.statements)
		{
			if (currentBlockTerminated)
				break; // Dead code after return
			stmt.accept(this);
		}
		return null;
	}

	@Override
	public LLVMValueRef visitReturnStatement(ReturnStatement node)
	{
		if (currentBlockTerminated)
			return null;

		if (node.value == null)
		{
			LLVMBuildRetVoid(builder);
		}
		else
		{
			LLVMValueRef value = node.value.accept(this);
			if (value != null)
			{
				Type valueType = analyzer.getType(node.value);
				LLVMValueRef result = emitCoerceToReturnType(value, valueType, currentMethodReturnType);
				LLVMBuildRet(builder, result);
			}
			else
			{
				LLVMBuildRetVoid(builder);
			}
		}
		currentBlockTerminated = true;
		return null;
	}

	/**
	 * Coerces a value of {@code srcType} to {@code retType} for a return statement.
	 * Handles two important Optional cases:
	 * <ol>
	 *   <li>Returning a plain T from a T?-returning function → wrap in { true, value }.</li>
	 *   <li>Returning {@code none} (OptionalType(ANY)) from a T?-returning function → emit typed none.</li>
	 * </ol>
	 * Falls back to {@link #emitCast} for all other cases.
	 */
	private LLVMValueRef emitCoerceToReturnType(LLVMValueRef value, Type srcType, Type retType)
	{
		if (retType instanceof OptionalType retOpt)
		{
			// Case 1: returning none (opt.<any>) — re-emit using the concrete return type
			if (srcType instanceof OptionalType srcOpt && srcOpt.innerType == Type.ANY)
			{
				return emitNoneOfType(retOpt);
			}

			// Case 2: returning a non-optional value that must be wrapped
			if (!(srcType instanceof OptionalType))
			{
				return emitWrapInOptional(value, srcType, retOpt);
			}

			// Case 3: same optional type (or compatible) — just cast
			return emitCast(value, srcType, retType);
		}

		// Composite (struct) values are passed by pointer in LLVM IR.  If the body
		// produced a struct value directly (e.g. fat-arrow operator body), store it
		// to an alloca and return the pointer so the LLVM function signature matches.
		if (retType instanceof CompositeType retCt && srcType instanceof CompositeType)
		{
			LLVMTypeRef structType = LLVMTypeMapper.getOrCreateStructType(context, retCt);
			LLVMValueRef alloca = LLVMBuildAlloca(builder, structType, "ret_struct");
			LLVMBuildStore(builder, value, alloca);
			return alloca;
		}

		return emitCast(value, srcType, retType);
	}

	/**
	 * Emits an absent optional value {@code { i1 false, undef }} for the given {@link OptionalType}.
	 */
	private LLVMValueRef emitNoneOfType(OptionalType ot)
	{
		LLVMTypeRef optStructType = LLVMTypeMapper.getOrCreateOptionalStructType(context, ot);
		LLVMValueRef optAlloca = LLVMBuildAlloca(builder, optStructType, "none");

		LLVMValueRef presentGep = LLVMBuildStructGEP2(builder, optStructType, optAlloca, 0, "none_present");
		LLVMBuildStore(builder, LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0), presentGep);

		LLVMValueRef valueGep = LLVMBuildStructGEP2(builder, optStructType, optAlloca, 1, "none_payload");
		LLVMTypeRef innerLLVMType = toLLVMType(ot.innerType);
		LLVMBuildStore(builder, LLVMGetUndef(innerLLVMType), valueGep);

		return LLVMBuildLoad2(builder, optStructType, optAlloca, "none_val");
	}

	/**
	 * Wraps a concrete value into an optional struct {@code { i1 true, value }}.
	 */
	private LLVMValueRef emitWrapInOptional(LLVMValueRef innerValue, Type innerSrcType, OptionalType targetOpt)
	{
		LLVMTypeRef optStructType = LLVMTypeMapper.getOrCreateOptionalStructType(context, targetOpt);
		LLVMValueRef optAlloca = LLVMBuildAlloca(builder, optStructType, "opt_wrap");

		LLVMValueRef presentGep = LLVMBuildStructGEP2(builder, optStructType, optAlloca, 0, "opt_present");
		LLVMBuildStore(builder, LLVMConstInt(LLVMInt1TypeInContext(context), 1, 0), presentGep);

		LLVMValueRef valueGep = LLVMBuildStructGEP2(builder, optStructType, optAlloca, 1, "opt_val");
		LLVMValueRef castedInner = emitCast(innerValue, innerSrcType, targetOpt.innerType);
		LLVMBuildStore(builder, castedInner, valueGep);

		return LLVMBuildLoad2(builder, optStructType, optAlloca, "opt_val");
	}

	// ── Block Management Helpers ────────────────────────────────────
	private void emitBasicBlock(LLVMBasicBlockRef bb, Statement stmt, LLVMBasicBlockRef mergeBB)
	{
		LLVMPositionBuilderAtEnd(builder, bb);
		currentBlockTerminated = false;
		stmt.accept(this);
		if (!currentBlockTerminated)
		{
			LLVMBuildBr(builder, mergeBB);
		}
	}

	@Override
	public LLVMValueRef visitIfStatement(IfStatement node)
	{
		LLVMValueRef cond = node.condition.accept(this);
		if (cond == null)
			return null;

		LLVMBasicBlockRef thenBB = LLVMAppendBasicBlockInContext(context, currentFunction, "if_then");
		LLVMBasicBlockRef elseBB = node.elseBranch != null ? LLVMAppendBasicBlockInContext(context, currentFunction, "if_else") : null;
		LLVMBasicBlockRef mergeBB = LLVMAppendBasicBlockInContext(context, currentFunction, "if_merge");

		LLVMBuildCondBr(builder, cond, thenBB, elseBB != null ? elseBB : mergeBB);

		emitBasicBlock(thenBB, node.thenBranch, mergeBB);
		if (elseBB != null)
		{
			emitBasicBlock(elseBB, node.elseBranch, mergeBB);
		}

		LLVMPositionBuilderAtEnd(builder, mergeBB);
		currentBlockTerminated = false;
		return null;
	}

	@Override
	public LLVMValueRef visitForStatement(ForStatement node)
	{
		// 1. Initializer
		if (node.initializer != null)
		{
			node.initializer.accept(this);
		}

		// 2. Basic Blocks
		LLVMBasicBlockRef headerBB = LLVMAppendBasicBlockInContext(context, currentFunction, "for_header");
		LLVMBasicBlockRef bodyBB = LLVMAppendBasicBlockInContext(context, currentFunction, "for_body");
		LLVMBasicBlockRef latchBB = LLVMAppendBasicBlockInContext(context, currentFunction, "for_latch");
		LLVMBasicBlockRef exitBB = LLVMAppendBasicBlockInContext(context, currentFunction, "for_exit");

		LLVMBuildBr(builder, headerBB);

		// 3. Header: Condition
		LLVMPositionBuilderAtEnd(builder, headerBB);
		currentBlockTerminated = false;
		if (node.condition != null)
		{
			LLVMValueRef cond = node.condition.accept(this);
			LLVMBuildCondBr(builder, cond, bodyBB, exitBB);
		}
		else
		{
			LLVMBuildBr(builder, bodyBB);
		}

		// 4. Body — push loop targets
		LLVMBasicBlockRef savedExit = currentLoopExitBB;
		LLVMBasicBlockRef savedContinue = currentLoopContinueBB;
		currentLoopExitBB = exitBB;
		currentLoopContinueBB = latchBB;

		LLVMPositionBuilderAtEnd(builder, bodyBB);
		currentBlockTerminated = false;
		if (node.body != null)
		{
			node.body.accept(this);
		}
		if (!currentBlockTerminated)
		{
			LLVMBuildBr(builder, latchBB);
		}

		// restore loop targets
		currentLoopExitBB = savedExit;
		currentLoopContinueBB = savedContinue;

		// 5. Latch: Iterators
		LLVMPositionBuilderAtEnd(builder, latchBB);
		currentBlockTerminated = false;
		if (node.iterators != null)
		{
			for (Expression iter : node.iterators)
			{
				iter.accept(this);
			}
		}
		LLVMBuildBr(builder, headerBB);

		// 6. Exit
		LLVMPositionBuilderAtEnd(builder, exitBB);
		currentBlockTerminated = false;

		return null;
	}

	@Override
	public LLVMValueRef visitWhileStatement(WhileStatement node)
	{
		// 1. Create the basic blocks for the loop structure
		LLVMBasicBlockRef headerBB = LLVMAppendBasicBlockInContext(context, currentFunction, "loop_header");
		LLVMBasicBlockRef bodyBB = LLVMAppendBasicBlockInContext(context, currentFunction, "loop_body");
		LLVMBasicBlockRef exitBB = LLVMAppendBasicBlockInContext(context, currentFunction, "loop_exit");

		// 2. Jump from the current block to the header (start the loop)
		// Only insert this branch if the previous code didn't already return/break
		if (!currentBlockTerminated)
		{
			LLVMBuildBr(builder, headerBB);
		}

		// ---------------------------------------------------------
		// 3. Header Block: Evaluate Condition
		// ---------------------------------------------------------
		LLVMPositionBuilderAtEnd(builder, headerBB);
		currentBlockTerminated = false;

		LLVMValueRef condition = node.condition.accept(this);
		if (condition == null)
		{
			// If condition failed to generate (unlikely), abort safely
			return null;
		}

		// Conditional Jump: If True -> Body, False -> Exit
		LLVMBuildCondBr(builder, condition, bodyBB, exitBB);

		// ---------------------------------------------------------
		// 4. Body Block: Execute Statements — push loop targets
		// ---------------------------------------------------------
		LLVMBasicBlockRef savedExit = currentLoopExitBB;
		LLVMBasicBlockRef savedContinue = currentLoopContinueBB;
		currentLoopExitBB = exitBB;
		currentLoopContinueBB = headerBB;

		LLVMPositionBuilderAtEnd(builder, bodyBB);
		currentBlockTerminated = false;

		if (node.body != null)
		{
			node.body.accept(this);
		}

		// If the body didn't explicitly return, jump back to the header to re-evaluate
		if (!currentBlockTerminated)
		{
			LLVMBuildBr(builder, headerBB);
		}

		// Restore enclosing loop targets
		currentLoopExitBB = savedExit;
		currentLoopContinueBB = savedContinue;

		// ---------------------------------------------------------
		// 5. Exit Block: Continue compilation
		// ---------------------------------------------------------
		LLVMPositionBuilderAtEnd(builder, exitBB);
		currentBlockTerminated = false;

		return null; // Statements do not produce an LLVMValueRef
	}

	@Override
	public LLVMValueRef visitForeachStatement(ForeachStatement node)
	{
		// Iterates over an array-like value.
		// Expected iterable: ArrayLiteralExpression or a variable whose type is REF/STR.
		// Layout assumed: selectorVal is a pointer to the first element.
		// Length must be known; for array literals we use the element count.
		// For other iterables we fall back to a sentinel-terminated scan (future work).

		LLVMValueRef iterableVal = node.iterable.accept(this);
		if (iterableVal == null)
			return null;

		// Determine element type and count
		Type iterableType = analyzer.getType(node.iterable);
		LLVMTypeRef elemLLVMType;
		LLVMValueRef lengthVal;

		if (node.iterable instanceof ArrayLiteralExpression ale)
		{
			// Array literal: determine element type from the first element or fall back to i32
			Type elemSemType = ale.elements.isEmpty()
				? PrimitiveType.I32
				: analyzer.getType(ale.elements.get(0));
			elemLLVMType = toLLVMType(elemSemType);
			lengthVal = LLVMConstInt(LLVMInt64TypeInContext(context), ale.elements.size(), 0);
		}
		else if (iterableType instanceof ArrayType at
				&& node.iterable instanceof IdentifierExpression ie
				&& arrayElementCounts.containsKey(ie.name))
		{
			// Named array variable whose element count was recorded at declaration time.
			// iterableVal is already a pointer to the first element (array decay).
			elemLLVMType = toLLVMType(at.baseType);
			lengthVal = LLVMConstInt(LLVMInt64TypeInContext(context), arrayElementCounts.get(ie.name), 0);
		}
		else
		{
			// For str: element is i8
			elemLLVMType = LLVMInt8TypeInContext(context);
			// Length from the str struct field 1
			if (iterableType == PrimitiveType.STR)
			{
				lengthVal = LLVMBuildExtractValue(builder, iterableVal, 1, "str_len");
				iterableVal = LLVMBuildExtractValue(builder, iterableVal, 0, "str_ptr");
			}
			else
			{
				// Unknown iterable — emit zero-iteration loop to keep IR valid
				lengthVal = LLVMConstInt(LLVMInt64TypeInContext(context), 0, 0);
			}
		}

		LLVMTypeRef i64t = LLVMInt64TypeInContext(context);

		// index alloca
		LLVMValueRef idxAlloca = LLVMBuildAlloca(builder, i64t, "foreach_idx");
		LLVMBuildStore(builder, LLVMConstInt(i64t, 0, 0), idxAlloca);

		// element binding alloca
		LLVMValueRef elemAlloca = LLVMBuildAlloca(builder, elemLLVMType, node.variableName);

		LLVMBasicBlockRef headerBB = LLVMAppendBasicBlockInContext(context, currentFunction, "foreach_hdr");
		LLVMBasicBlockRef bodyBB   = LLVMAppendBasicBlockInContext(context, currentFunction, "foreach_body");
		LLVMBasicBlockRef latchBB  = LLVMAppendBasicBlockInContext(context, currentFunction, "foreach_latch");
		LLVMBasicBlockRef exitBB   = LLVMAppendBasicBlockInContext(context, currentFunction, "foreach_exit");

		LLVMBuildBr(builder, headerBB);

		// Header: check idx < length
		LLVMPositionBuilderAtEnd(builder, headerBB);
		currentBlockTerminated = false;
		LLVMValueRef idxCur = LLVMBuildLoad2(builder, i64t, idxAlloca, "idx");
		LLVMValueRef cond   = LLVMBuildICmp(builder, LLVMIntULT, idxCur, lengthVal, "foreach_cond");
		LLVMBuildCondBr(builder, cond, bodyBB, exitBB);

		// Body: load element, bind variable, emit body
		LLVMPositionBuilderAtEnd(builder, bodyBB);
		currentBlockTerminated = false;

		LLVMValueRef[] gepIdx = {idxCur};
		LLVMValueRef elemPtr = LLVMBuildGEP2(builder, elemLLVMType, iterableVal,
			new PointerPointer<>(gepIdx), 1, "foreach_elem_ptr");
		LLVMValueRef elemLoaded = LLVMBuildLoad2(builder, elemLLVMType, elemPtr, node.variableName + "_val");
		LLVMBuildStore(builder, elemLoaded, elemAlloca);

		namedValues.put(node.variableName, elemAlloca);

		LLVMBasicBlockRef savedExit     = currentLoopExitBB;
		LLVMBasicBlockRef savedContinue = currentLoopContinueBB;
		currentLoopExitBB    = exitBB;
		currentLoopContinueBB = latchBB;

		node.body.accept(this);

		currentLoopExitBB    = savedExit;
		currentLoopContinueBB = savedContinue;

		if (!currentBlockTerminated)
			LLVMBuildBr(builder, latchBB);

		// Latch: increment index
		LLVMPositionBuilderAtEnd(builder, latchBB);
		currentBlockTerminated = false;
		LLVMValueRef idxNext = LLVMBuildAdd(builder,
			LLVMBuildLoad2(builder, i64t, idxAlloca, "idx_latch"),
			LLVMConstInt(i64t, 1, 0), "idx_inc");
		LLVMBuildStore(builder, idxNext, idxAlloca);
		LLVMBuildBr(builder, headerBB);

		// Exit
		LLVMPositionBuilderAtEnd(builder, exitBB);
		currentBlockTerminated = false;
		namedValues.remove(node.variableName);

		return null;
	}

	@Override
	public LLVMValueRef visitExpressionStatement(ExpressionStatement node)
	{
		// Emit the expression for its side effects, discard the result
		node.expression.accept(this);
		return null;
	}

	@Override
	public LLVMValueRef visitTagStatement(TagStatement node)
	{
		// TODO: Tag statements are metadata — may not need IR
		return null;
	}

	@Override
	public LLVMValueRef visitUseStatement(UseStatement node)
	{
		// Import/use statements don't produce IR
		return null;
	}

	// =================================================================
	// EXPRESSIONS
	// =================================================================

	@Override
	public LLVMValueRef visitLiteralExpression(LiteralExpression node)
	{
		Type semType = analyzer.getType(node);
		LLVMTypeRef llvmType = toLLVMType(semType);

		return switch (node.type)
		{
			case INT -> {
				long val = ((Number) node.value).longValue();

				yield LLVMConstInt(llvmType, val, /* signExtend */ 1);
			}
			case FLOAT ->

			{
				double val = ((Number) node.value).doubleValue();

				yield LLVMConstReal(llvmType, val);
			}
			case BOOL ->

			{
				boolean val = (Boolean) node.value;

				yield LLVMConstInt(llvmType, val ? 1 : 0, 0);
			}
			case CHAR ->

			{
				// char → i32 codepoint
				int codePoint;
				if (node.value instanceof Character c)
				{
					codePoint = c;
				}
				else
				{
					codePoint = ((Number) node.value).intValue();
				}

				yield LLVMConstInt(llvmType, codePoint, 0);
			}
			case STRING -> {
				// str → { i8*, i64 }
				String text = node.value.toString();
				LLVMValueRef globalPtr = LLVMBuildGlobalStringPtr(builder, text, ".str");
				LLVMValueRef length = LLVMConstInt(LLVMInt64TypeInContext(context), text.length(), 0);

				LLVMValueRef structVal = LLVMGetUndef(llvmType);
				structVal = LLVMBuildInsertValue(builder, structVal, globalPtr, 0, "str_ptr");
				structVal = LLVMBuildInsertValue(builder, structVal, length, 1, "str_len");

				yield structVal;
			}

		};
	}

	// ── Binary Operation Helpers ─────────────────────────────────────
	private LLVMValueRef emitArithmeticOp(LLVMValueRef lVal, LLVMValueRef rVal, BinaryOperator op, boolean isFloat)
	{
		return switch (op)
		{
			case ADD -> isFloat ? LLVMBuildFAdd(builder, lVal, rVal, "fadd") : LLVMBuildAdd(builder, lVal, rVal, "add");
			case SUB -> isFloat ? LLVMBuildFSub(builder, lVal, rVal, "fsub") : LLVMBuildSub(builder, lVal, rVal, "sub");
			case MUL -> isFloat ? LLVMBuildFMul(builder, lVal, rVal, "fmul") : LLVMBuildMul(builder, lVal, rVal, "mul");
			default -> null;
		};
	}

	private LLVMValueRef emitDivisionOp(LLVMValueRef lVal, LLVMValueRef rVal, boolean isFloat, boolean isUnsigned)
	{
		if (isFloat)
			return LLVMBuildFDiv(builder, lVal, rVal, "fdiv");
		return isUnsigned ? LLVMBuildUDiv(builder, lVal, rVal, "udiv") : LLVMBuildSDiv(builder, lVal, rVal, "sdiv");
	}

	private LLVMValueRef emitModuloOp(LLVMValueRef lVal, LLVMValueRef rVal, boolean isFloat, boolean isUnsigned)
	{
		if (isFloat)
			return LLVMBuildFRem(builder, lVal, rVal, "frem");
		return isUnsigned ? LLVMBuildURem(builder, lVal, rVal, "urem") : LLVMBuildSRem(builder, lVal, rVal, "srem");
	}

	private LLVMValueRef emitComparisonOp(LLVMValueRef lVal, LLVMValueRef rVal, BinaryOperator op, boolean isFloat, boolean isUnsigned)
	{
		if (isFloat)
		{
			return switch (op)
			{
				case EQ -> LLVMBuildFCmp(builder, LLVMRealOEQ, lVal, rVal, "feq");
				case NE -> LLVMBuildFCmp(builder, LLVMRealONE, lVal, rVal, "fne");
				case LT -> LLVMBuildFCmp(builder, LLVMRealOLT, lVal, rVal, "flt");
				case GT -> LLVMBuildFCmp(builder, LLVMRealOGT, lVal, rVal, "fgt");
				case LE -> LLVMBuildFCmp(builder, LLVMRealOLE, lVal, rVal, "fle");
				case GE -> LLVMBuildFCmp(builder, LLVMRealOGE, lVal, rVal, "fge");
				default -> null;
			};
		}
		return switch (op)
		{
			case EQ -> LLVMBuildICmp(builder, LLVMIntEQ, lVal, rVal, "eq");
			case NE -> LLVMBuildICmp(builder, LLVMIntNE, lVal, rVal, "ne");
			case LT -> isUnsigned ? LLVMBuildICmp(builder, LLVMIntULT, lVal, rVal, "ult") : LLVMBuildICmp(builder, LLVMIntSLT, lVal, rVal, "slt");
			case GT -> isUnsigned ? LLVMBuildICmp(builder, LLVMIntUGT, lVal, rVal, "ugt") : LLVMBuildICmp(builder, LLVMIntSGT, lVal, rVal, "sgt");
			case LE -> isUnsigned ? LLVMBuildICmp(builder, LLVMIntULE, lVal, rVal, "ule") : LLVMBuildICmp(builder, LLVMIntSLE, lVal, rVal, "sle");
			case GE -> isUnsigned ? LLVMBuildICmp(builder, LLVMIntUGE, lVal, rVal, "uge") : LLVMBuildICmp(builder, LLVMIntSGE, lVal, rVal, "sge");
			default -> null;
		};
	}

	private LLVMValueRef emitBitwiseOp(LLVMValueRef lVal, LLVMValueRef rVal, BinaryOperator op, boolean isUnsigned)
	{
		return switch (op)
		{
			case LOGICAL_AND, BIT_AND -> LLVMBuildAnd(builder, lVal, rVal, "and");
			case LOGICAL_OR, BIT_OR -> LLVMBuildOr(builder, lVal, rVal, "or");
			case BIT_XOR -> LLVMBuildXor(builder, lVal, rVal, "xor");
			case SHL -> LLVMBuildShl(builder, lVal, rVal, "shl");
			case SHR -> isUnsigned ? LLVMBuildLShr(builder, lVal, rVal, "lshr") : LLVMBuildAShr(builder, lVal, rVal, "ashr");
			default -> null;
		};
	}

	@Override
	public LLVMValueRef visitBinaryExpression(BinaryExpression node)
	{
		Type leftType = analyzer.getType(node.left);
		Type rightType = analyzer.getType(node.right);

		// Optional == none / != none: compare the presence bit
		if ((leftType instanceof OptionalType || rightType instanceof OptionalType)
			&& (node.operator == BinaryOperator.EQ || node.operator == BinaryOperator.NE))
		{
			return emitOptionalNoneComparison(node, leftType, rightType);
		}

		// Operator overloading for composite types (e.g. Vector3 + Vector3)
		if (leftType instanceof CompositeType lct && (node.operator == BinaryOperator.ADD
				|| node.operator == BinaryOperator.SUB || node.operator == BinaryOperator.MUL
				|| node.operator == BinaryOperator.DIV || node.operator == BinaryOperator.EQ
				|| node.operator == BinaryOperator.NE || node.operator == BinaryOperator.LT
				|| node.operator == BinaryOperator.GT || node.operator == BinaryOperator.LE
				|| node.operator == BinaryOperator.GE))
		{
			String opMethodName = operatorMethodName(node.operator);
			if (opMethodName != null)
			{
				Symbol opSym = lct.getMemberScope().resolve(opMethodName);
				if (opSym instanceof MethodSymbol ms)
				{
					return emitOperatorOverloadCall(node, ms, leftType);
				}
			}
		}

		LLVMValueRef lVal = node.left.accept(this);
		LLVMValueRef rVal = node.right.accept(this);

		if (lVal == null || rVal == null)
			return null;

		Type operandType = getPromotedType(leftType, rightType);

		// Cast operands to promoted type
		lVal = emitCast(lVal, leftType, operandType);
		rVal = emitCast(rVal, rightType, operandType);

		boolean isFloat = isFloatType(operandType);
		boolean isUnsigned = isUnsignedType(operandType);

		// Dispatch to appropriate operation handler
		return switch (node.operator)
		{
			case ADD, SUB, MUL -> emitArithmeticOp(lVal, rVal, node.operator, isFloat);
			case DIV -> emitDivisionOp(lVal, rVal, isFloat, isUnsigned);
			case MOD -> emitModuloOp(lVal, rVal, isFloat, isUnsigned);
			case POW -> emitPowOp(lVal, rVal, operandType);
			case EQ, NE, LT, GT, LE, GE -> emitComparisonOp(lVal, rVal, node.operator, isFloat, isUnsigned);
			case LOGICAL_AND, LOGICAL_OR, BIT_AND, BIT_OR, BIT_XOR, SHL, SHR -> emitBitwiseOp(lVal, rVal, node.operator, isUnsigned);
			default -> null;
		};
	}

	/**
	 * Emits a comparison of an optional value against {@code none}.
	 * Extracts the presence bit (field 0) and checks it with ICmp.
	 */
	private LLVMValueRef emitOptionalNoneComparison(BinaryExpression node, Type leftType, Type rightType)
	{
		// Determine which side is the optional
		boolean leftIsOpt = leftType instanceof OptionalType;
		LLVMValueRef optVal = leftIsOpt ? node.left.accept(this) : node.right.accept(this);
		OptionalType ot = (OptionalType) (leftIsOpt ? leftType : rightType);

		LLVMTypeRef optStructType = LLVMTypeMapper.getOrCreateOptionalStructType(context, ot);
		LLVMValueRef optAlloca = LLVMBuildAlloca(builder, optStructType, "opt_cmp_tmp");
		LLVMBuildStore(builder, optVal, optAlloca);

		LLVMValueRef presentGep = LLVMBuildStructGEP2(builder, optStructType, optAlloca, 0, "opt_present");
		LLVMValueRef presentBit = LLVMBuildLoad2(builder, LLVMInt1TypeInContext(context), presentGep, "present_bit");

		// != none → present == true (i1 1)
		// == none → present == false (i1 0)
		if (node.operator == BinaryOperator.NE)
		{
			return LLVMBuildICmp(builder, LLVMIntEQ, presentBit, LLVMConstInt(LLVMInt1TypeInContext(context), 1, 0), "ne_none");
		}
		else
		{
			return LLVMBuildICmp(builder, LLVMIntEQ, presentBit, LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0), "eq_none");
		}
	}

	/**
	 * Emits an operator overload call: resolves the self pointer and calls the operator method.
	 *
	 * <p>The {@code self} pointer must be a {@code ptr} to the struct data.  If the
	 * LHS produces a struct value (e.g. from a constructor expression), we spill it to a
	 * temporary alloca first.  If it is already a pointer (e.g. from an inline-struct
	 * variable lookup), we use it directly to avoid an extra layer of indirection.
	 */
	private LLVMValueRef emitOperatorOverloadCall(BinaryExpression node, MethodSymbol ms, Type leftType)
	{
		LLVMValueRef lhsVal = node.left.accept(this);
		if (lhsVal == null)
			return null;

		// selfPtr must be a ptr → struct data.  lhsVal is either:
		//  • A pointer  (visitIdentifierExpression for inline-struct vars)  → use directly.
		//  • A struct value (emitConstructorCall returns a loaded struct value) → spill to alloca.
		LLVMValueRef selfPtr;
		if (LLVMGetTypeKind(LLVMTypeOf(lhsVal)) == LLVMPointerTypeKind)
		{
			selfPtr = lhsVal;
		}
		else
		{
			LLVMTypeRef structTy = LLVMTypeMapper.getOrCreateStructType(context, (CompositeType) leftType);
			selfPtr = LLVMBuildAlloca(builder, structTy, "op_lhs_tmp");
			LLVMBuildStore(builder, lhsVal, selfPtr);
		}

		LLVMValueRef rhsVal = node.right.accept(this);
		if (rhsVal == null)
			return null;

		String mangledName = ms.getMangledName();
		LLVMValueRef func = LLVMGetNamedFunction(module, mangledName);
		if (func == null || func.isNull())
		{
			FunctionType ft = ms.getType();
			func = LLVMAddFunction(module, mangledName, toLLVMType(ft));
		}

		// params: (ptr this, rhs)
		Type rhsParamType = ms.getType().parameterTypes.size() > 1
				? ms.getType().parameterTypes.get(1)
				: analyzer.getType(node.right);
		LLVMValueRef castedRhs = emitCast(rhsVal, analyzer.getType(node.right), rhsParamType);

		LLVMValueRef[] args = {selfPtr, castedRhs};
		return LLVMBuildCall2(builder, toLLVMType(ms.getType()), func,
				new PointerPointer<>(args), 2, "op_result");
	}

	/**
	 * Maps a {@link BinaryOperator} to its operator method name for overload lookup.
	 * Returns {@code null} for operators that cannot be overloaded.
	 */
	/** Returns the scope lookup key used by the SA for an operator declaration. */
	private static String operatorDeclKey(OperatorDeclaration od)
	{
		return "operator" + od.operatorToken;
	}

	/**
	 * Emits safe-navigation ({@code opt?.member}): evaluates the optional base,
	 * accesses the member field when the optional is present, and returns a
	 * default (zero/undef) when absent.
	 *
	 * <p>The semantic analyser resolves the result type as the raw member type
	 * (not wrapped in Optional), so we simply produce a branch that stores the
	 * actual field value on the present path and zero-initialises on the absent
	 * path, then loads the result.
	 *
	 * @param node The member-access node (must have {@code isSafe == true}).
	 * @param base The LLVM value of the optional struct (already evaluated).
	 * @param ot   The {@link OptionalType} of the base expression.
	 */
	private LLVMValueRef emitSafeNavigation(MemberAccessExpression node, LLVMValueRef base, OptionalType ot)
	{
		Type resultType = analyzer.getType(node);
		if (resultType == null)
			return null;

		// The SA resolves the result as the raw member type (e.g. i32, not i32?).
		LLVMTypeRef resultLLVMType = toLLVMType(resultType);
		LLVMValueRef resultAlloca  = LLVMBuildAlloca(builder, resultLLVMType, "safe_nav_result");

		// ── Extract present flag and inner value from the optional struct ─────
		LLVMValueRef presentFlag = LLVMBuildExtractValue(builder, base, 0, "safe_present");
		LLVMValueRef innerVal    = LLVMBuildExtractValue(builder, base, 1, "safe_inner");

		LLVMBasicBlockRef presentBB = LLVMAppendBasicBlockInContext(context, currentFunction, "safe_present");
		LLVMBasicBlockRef absentBB  = LLVMAppendBasicBlockInContext(context, currentFunction, "safe_absent");
		LLVMBasicBlockRef mergeBB   = LLVMAppendBasicBlockInContext(context, currentFunction, "safe_merge");

		LLVMBuildCondBr(builder, presentFlag, presentBB, absentBB);

		// ── Present branch: access the member ─────────────────────────────────
		LLVMPositionBuilderAtEnd(builder, presentBB);
		currentBlockTerminated = false;

		Type innerType = ot.innerType;
		LLVMValueRef fieldVal = null;

		if (innerType instanceof CompositeType innerCt)
		{
			LLVMValueRef fieldGep = emitMemberPointer(innerVal, innerCt, node.memberName);
			if (fieldGep != null)
			{
				Symbol memberSym2 = innerCt.getMemberScope().resolve(node.memberName);
				if (memberSym2 instanceof VariableSymbol vs2)
				{
					fieldVal = LLVMBuildLoad2(builder, toLLVMType(vs2.getType()), fieldGep,
						node.memberName + "_safe");
					fieldVal = emitCast(fieldVal, vs2.getType(), resultType);
				}
			}
		}

		if (fieldVal != null)
		{
			LLVMBuildStore(builder, fieldVal, resultAlloca);
		}
		else
		{
			LLVMBuildStore(builder, LLVMConstNull(resultLLVMType), resultAlloca);
		}
		LLVMBuildBr(builder, mergeBB);

		// ── Absent branch: store a zero/null default ──────────────────────────
		LLVMPositionBuilderAtEnd(builder, absentBB);
		currentBlockTerminated = false;
		LLVMBuildStore(builder, LLVMConstNull(resultLLVMType), resultAlloca);
		LLVMBuildBr(builder, mergeBB);

		// ── Merge ─────────────────────────────────────────────────────────────
		LLVMPositionBuilderAtEnd(builder, mergeBB);
		currentBlockTerminated = false;
		return LLVMBuildLoad2(builder, resultLLVMType, resultAlloca, "safe_nav_val");
	}

	private static String operatorMethodName(BinaryOperator op)
	{
		return switch (op)
		{
			case ADD -> "operator+";
			case SUB -> "operator-";
			case MUL -> "operator*";
			case DIV -> "operator/";
			case MOD -> "operator%";
			case EQ  -> "operator==";
			case NE  -> "operator!=";
			case LT  -> "operator<";
			case GT  -> "operator>";
			case LE  -> "operator<=";
			case GE  -> "operator>=";
			default  -> null;
		};
	}

	/**
	 * Emits a power operation (base ** exp).
	 * For floating-point types, delegates to the {@code llvm.pow.f64} intrinsic.
	 * For integer types, delegates to {@code llvm.powi.i32} (integer exponent, f64 base)
	 * and converts back to the target integer type.
	 */
	private LLVMValueRef emitPowOp(LLVMValueRef base, LLVMValueRef exp, Type semType)
	{
		boolean isFloat = isFloatType(semType);
		LLVMTypeRef f64t = LLVMDoubleTypeInContext(context);
		LLVMTypeRef i32t = LLVMInt32TypeInContext(context);

		if (isFloat)
		{
			// llvm.pow.f64(f64 base, f64 exp)
			LLVMValueRef powFn = getOrDeclareIntrinsic("llvm.pow.f64",
				LLVMFunctionType(f64t, new PointerPointer<>(new LLVMTypeRef[]{f64t, f64t}), 2, 0));
			LLVMValueRef baseF64 = emitCast(base, semType, PrimitiveType.F64);
			LLVMValueRef expF64  = emitCast(exp, semType, PrimitiveType.F64);
			LLVMValueRef[] args = {baseF64, expF64};
			LLVMValueRef result = LLVMBuildCall2(builder,
				LLVMFunctionType(f64t, new PointerPointer<>(new LLVMTypeRef[]{f64t, f64t}), 2, 0),
				powFn, new PointerPointer<>(args), 2, "pow");
			return emitCast(result, PrimitiveType.F64, semType);
		}
		else
		{
			// powi: llvm.powi.f64.i32(f64 base, i32 exp) — integer exponent
			LLVMTypeRef[] powiParams = {f64t, i32t};
			LLVMValueRef powiFn = getOrDeclareIntrinsic("llvm.powi.f64.i32",
				LLVMFunctionType(f64t, new PointerPointer<>(powiParams), 2, 0));
			LLVMValueRef baseF64 = LLVMBuildSIToFP(builder, base, f64t, "pow_base_f64");
			LLVMValueRef expI32  = LLVMBuildTrunc(builder, exp, i32t, "pow_exp_i32");
			LLVMValueRef[] args = {baseF64, expI32};
			LLVMValueRef resultF64 = LLVMBuildCall2(builder,
				LLVMFunctionType(f64t, new PointerPointer<>(powiParams), 2, 0),
				powiFn, new PointerPointer<>(args), 2, "powi");
			// Convert result back to the integer target type
			return LLVMBuildFPToSI(builder, resultF64, toLLVMType(semType), "pow_result");
		}
	}

	/**
	 * Declares (or retrieves) an LLVM intrinsic / external function by name.
	 */
	private LLVMValueRef getOrDeclareIntrinsic(String name, LLVMTypeRef fnType)
	{
		LLVMValueRef fn = LLVMGetNamedFunction(module, name);
		if (fn == null || fn.isNull())
		{
			fn = LLVMAddFunction(module, name, fnType);
		}
		return fn;
	}

	@Override
	public LLVMValueRef visitUnaryExpression(UnaryExpression node)
	{
		LLVMValueRef operand = node.operand.accept(this);
		if (operand == null)
			return null;

		Type semType = analyzer.getType(node.operand);
		boolean isFloat = isFloatType(semType);

		return switch (node.operator)
		{
			case MINUS -> isFloat ? LLVMBuildFNeg(builder, operand, "fneg") : LLVMBuildNeg(builder, operand, "neg");
			case PLUS -> operand;
			case NOT, BIT_NOT -> LLVMBuildNot(builder, operand, node.operator == UnaryOperator.NOT ? "not" : "bitnot");
			case INCREMENT, DECREMENT -> {
				LLVMValueRef ptr = emitPointer(node.operand);
				if (ptr == null)
					throw new CodegenException("Cannot increment/decrement non-lvalue");

				LLVMTypeRef type = toLLVMType(semType);
				LLVMValueRef oldVal = LLVMBuildLoad2(builder, type, ptr, "incdec_load");

				LLVMValueRef one = isFloat ? LLVMConstReal(type, 1.0) : LLVMConstInt(type, 1, 0);
				LLVMValueRef newVal = (node.operator == UnaryOperator.INCREMENT) ? (isFloat ? LLVMBuildFAdd(builder, oldVal, one, "finc") : LLVMBuildAdd(builder, oldVal, one, "inc")) : (isFloat ? LLVMBuildFSub(builder, oldVal, one, "fdec") : LLVMBuildSub(builder, oldVal, one, "dec"));

				LLVMBuildStore(builder, newVal, ptr);
				yield node.isPostfix ? oldVal : newVal;
			}
			default -> operand;
		};
	}

	@Override
	public LLVMValueRef visitAssignmentExpression(AssignmentExpression node)
	{
		LLVMValueRef value = node.value.accept(this);
		if (value == null)
			return null;

		if (node.target instanceof IdentifierExpression idExpr)
		{
			LLVMValueRef pointer = namedValues.get(idExpr.name);
			if (pointer == null)
			{
				throw new CodegenException("Cannot assign to undeclared variable: " + idExpr.name);
			}

			Type targetSemType = analyzer.getType(node.target);
			LLVMValueRef rhs = emitCast(value, analyzer.getType(node.value), targetSemType);
			LLVMValueRef storeVal = emitCompoundRhs(pointer, targetSemType, node.operator, rhs);
			LLVMBuildStore(builder, storeVal, pointer);
			return storeVal;
		}
		else if (node.target instanceof MemberAccessExpression mae)
		{
			LLVMValueRef pointer = emitMemberPointer(mae);
			if (pointer == null)
			{
				throw new CodegenException("Cannot get pointer for member: " + mae.memberName);
			}

			Type targetSemType = analyzer.getType(mae);
			LLVMValueRef rhs = emitCast(value, analyzer.getType(node.value), targetSemType);
			LLVMValueRef storeVal = emitCompoundRhs(pointer, targetSemType, node.operator, rhs);
			LLVMBuildStore(builder, storeVal, pointer);
			return storeVal;
		}
		else if (node.target instanceof IndexExpression indexExpr)
		{
			LLVMValueRef base = indexExpr.target.accept(this);
			LLVMValueRef index = indexExpr.indices.get(0).accept(this);
			Type baseType = analyzer.getType(indexExpr.target);

			if (baseType == PrimitiveType.REF || baseType == PrimitiveType.STR)
			{
				LLVMValueRef[] indices = {index};
				LLVMTypeRef elemType = LLVMInt8TypeInContext(context);
				LLVMValueRef gep = LLVMBuildGEP2(builder, elemType, base, new PointerPointer<>(indices), 1, "ptr_idx");

				Type targetSemType = analyzer.getType(indexExpr);
				LLVMValueRef rhs = emitCast(value, analyzer.getType(node.value), targetSemType);
				LLVMValueRef storeVal = emitCompoundRhs(gep, targetSemType, node.operator, rhs);
				LLVMBuildStore(builder, storeVal, gep);
				return storeVal;
			}

			if (baseType instanceof ArrayType at)
			{
				LLVMTypeRef elemType = toLLVMType(at.baseType);
				LLVMValueRef[] indices = {index};
				LLVMValueRef gep = LLVMBuildGEP2(builder, elemType, base, new PointerPointer<>(indices), 1, "arr_idx");

				Type targetSemType = analyzer.getType(indexExpr);
				LLVMValueRef rhs = emitCast(value, analyzer.getType(node.value), targetSemType);
				LLVMValueRef storeVal = emitCompoundRhs(gep, targetSemType, node.operator, rhs);
				LLVMBuildStore(builder, storeVal, gep);
				return storeVal;
			}
		}

		throw new CodegenException("Unsupported assignment target: " + node.target.getClass().getSimpleName());
	}

	/**
	 * For compound operators (+=, -=, etc.) loads the current value at {@code ptr},
	 * applies the arithmetic, and returns the new value to store.
	 * For plain {@code =} simply returns {@code rhs} unchanged.
	 */
	private LLVMValueRef emitCompoundRhs(LLVMValueRef ptr, Type semType, String op, LLVMValueRef rhs)
	{
		if (op.equals("="))
			return rhs;

		LLVMTypeRef llvmType = toLLVMType(semType);
		LLVMValueRef lhs = LLVMBuildLoad2(builder, llvmType, ptr, "compound_load");
		boolean isFloat = isFloatType(semType);
		boolean isUnsigned = isUnsignedType(semType);

		return switch (op)
		{
			case "+="  -> emitArithmeticOp(lhs, rhs, BinaryOperator.ADD, isFloat);
			case "-="  -> emitArithmeticOp(lhs, rhs, BinaryOperator.SUB, isFloat);
			case "*="  -> emitArithmeticOp(lhs, rhs, BinaryOperator.MUL, isFloat);
			case "/="  -> emitDivisionOp(lhs, rhs, isFloat, isUnsigned);
			case "%="  -> emitModuloOp(lhs, rhs, isFloat, isUnsigned);
			case "**=" -> emitPowOp(lhs, rhs, semType);
			case "&="  -> LLVMBuildAnd(builder, lhs, rhs, "and_assign");
			case "|="  -> LLVMBuildOr(builder, lhs, rhs, "or_assign");
			case "^="  -> LLVMBuildXor(builder, lhs, rhs, "xor_assign");
			case "<<="  -> LLVMBuildShl(builder, lhs, rhs, "shl_assign");
			case ">>="  -> isUnsigned
				? LLVMBuildLShr(builder, lhs, rhs, "lshr_assign")
				: LLVMBuildAShr(builder, lhs, rhs, "ashr_assign");
			default -> throw new CodegenException("Unknown compound operator: " + op);
		};
	}

	private Type getVariableType(Expression var)
	{
		if (var instanceof IdentifierExpression idExpr)
		{
			org.nebula.nebc.semantic.symbol.Symbol sym = analyzer.getSymbol(var, org.nebula.nebc.semantic.symbol.VariableSymbol.class);
			return sym != null ? sym.getType() : PrimitiveType.I32;
		}
		return PrimitiveType.I32;
	}

	@Override
	public LLVMValueRef visitCastExpression(CastExpression node)
	{
		LLVMValueRef val = node.expression.accept(this);
		if (val == null)
			return null;

		Type srcSemType = analyzer.getType(node.expression);
		Type targetSemType = analyzer.getType(node);

		return emitCast(val, srcSemType, targetSemType);
	}

	@Override
	public LLVMValueRef visitExpressionBlock(ExpressionBlock node)
	{
		for (Statement stmt : node.statements)
		{
			if (currentBlockTerminated)
				break; // Dead code
			stmt.accept(this);
		}

		if (!currentBlockTerminated && node.hasTail())
		{
			return node.tail.accept(this);
		}

		return null;
	}

	// ── Symbol Resolution Helpers ──────────────────────────────────
	private LLVMValueRef resolveFunctionReference(IdentifierExpression node)
	{
		org.nebula.nebc.semantic.symbol.Symbol sym = analyzer.getSymbol(node, org.nebula.nebc.semantic.symbol.Symbol.class);

		if (sym == null)
		{
			return null;
		}

		if (!(sym instanceof org.nebula.nebc.semantic.symbol.MethodSymbol methodSym))
		{
			return null;
		}

		String mangledName = methodSym.getMangledName();
		String actualName = "main".equals(mangledName) ? "__nebula_main" : mangledName;

		LLVMValueRef func = LLVMGetNamedFunction(module, actualName);
		if (func != null)
		{
			return func;
		}

		// Forward declaration
		FunctionType ft = methodSym.getType();
		return LLVMAddFunction(module, actualName, toLLVMType(ft));
	}

	private LLVMValueRef resolveFunctionByName(String functionName)
	{
		LLVMValueRef func = LLVMGetNamedFunction(module, functionName);
		if (func != null && !func.isNull())
		{
			return func;
		}

		// Try with mangled name pattern
		LLVMValueRef mangledFunc = LLVMGetNamedFunction(module, functionName);
		if (mangledFunc != null && !mangledFunc.isNull())
		{
			return mangledFunc;
		}

		return null;
	}

	@Override
	public LLVMValueRef visitIdentifierExpression(IdentifierExpression node)
	{
		LLVMValueRef pointer = namedValues.get(node.name);
		if (pointer != null)
		{
			Type type = getVariableType(node);
			// Inline struct vars (local variables declared with StructType) hold the
			// struct data directly in their alloca — the alloca IS the ptr-to-struct.
			// Return it without loading; parameters of struct type use alloca-of-ptr
			// and are NOT in inlineStructVars, so they still go through LLVMBuildLoad2.
			if (inlineStructVars.contains(node.name))
			{
				return pointer;
			}
			LLVMTypeRef expectedType = toLLVMType(type);
			return LLVMBuildLoad2(builder, expectedType, pointer, node.name + "_load");
		}

		// Try to resolve as a function or constructor
		LLVMValueRef func = resolveFunctionReference(node);
		if (func != null)
		{
			return func;
		}

		// Try to resolve as a struct/class type — look up its constructor by mangled name
		Symbol sym = analyzer.getSymbol(node, Symbol.class);
		if (sym instanceof TypeSymbol ts && ts.getType() instanceof CompositeType ct)
		{
			// Find the constructor method in the composite type's member scope
			Symbol ctorSym = ct.getMemberScope().resolveLocal(node.name);
			if (ctorSym instanceof MethodSymbol ctorMs)
			{
				String mangledName = ctorMs.getMangledName();
				LLVMValueRef ctorFunc = LLVMGetNamedFunction(module, mangledName);
				if (ctorFunc == null || ctorFunc.isNull())
				{
					// Forward-declare the constructor
					ctorFunc = LLVMAddFunction(module, mangledName, toLLVMType(ctorMs.getType()));
				}
				return ctorFunc;
			}
		}

		// Try to resolve as a global constant or global variable (e.g. top-level consts)
		LLVMValueRef globalVar = LLVMGetNamedGlobal(module, node.name);
		if (globalVar != null && !globalVar.isNull())
		{
			Type type = getVariableType(node);
			LLVMTypeRef expectedType = toLLVMType(type);
			return LLVMBuildLoad2(builder, expectedType, globalVar, node.name + "_load");
		}

		throw new CodegenException("Undeclared identifier referenced in codegen: " + node.name);
	}

	@Override
	public LLVMValueRef visitInvocationExpression(InvocationExpression node)
	{
		LLVMValueRef function = null;

		// Detect generic call and trigger monomorphization
		if (node.getTypeArguments() != null && !node.getTypeArguments().isEmpty())
		{
			Symbol sym = analyzer.getSymbol(node.target, Symbol.class);
			if (sym instanceof MethodSymbol ms)
			{
				String specializationName = getSpecializationName(ms, node.getTypeArguments());
				function = LLVMGetNamedFunction(module, specializationName);
				if (function == null)
				{
					// Emit the specialization
					Substitution prevSub = currentSubstitution;
					currentSubstitution = new Substitution();
					for (int i = 0; i < ms.getTypeParameters().size(); i++)
					{
						currentSubstitution.bind(ms.getTypeParameters().get(i), node.getTypeArguments().get(i));
					}

					// Re-visit the method declaration directly
					MethodDeclaration decl = (MethodDeclaration) ms.getDeclarationNode();
					if (decl == null)
					{
						throw new CodegenException(
							"Cannot monomorphize generic method '" + ms.getName() +
							"': its declaration node is unavailable. " +
							"Ensure the standard library source is being compiled alongside this project.");
					}
					function = visitMethodDeclaration(decl);

					currentSubstitution = prevSub;
				}
			}
			else if (sym instanceof TypeSymbol ts && ts.getType() instanceof CompositeType genericCt
					&& node.target instanceof IdentifierExpression ctorIdent)
			{
				// Generic struct/class constructor call: Pair(3, 7) where SA inferred T=i32.
				// Collect type parameters (TypeSymbol entries with TypeParameterType) from the
				// struct's member scope and build a Substitution from inferred type args.
				List<TypeParameterType> typeParams = genericCt.getMemberScope().getSymbols().values()
					.stream()
					.filter(s -> s instanceof org.nebula.nebc.semantic.symbol.TypeSymbol tts
						&& tts.getType() instanceof TypeParameterType)
					.map(s -> (TypeParameterType) s.getType())
					.collect(java.util.stream.Collectors.toList());

				if (!typeParams.isEmpty() && typeParams.size() == node.getTypeArguments().size())
				{
					// The SA recorded the monomorphized return type on the invocation node itself.
					Type monoType = analyzer.getType(node);
					if (!(monoType instanceof CompositeType monoCt))
					{
						throw new CodegenException("Expected monomorphized composite type for generic constructor call, got: "
							+ (monoType != null ? monoType.name() : "null"));
					}

					Substitution prevSub = currentSubstitution;
					currentSubstitution = new Substitution();
					for (int i = 0; i < typeParams.size(); i++)
					{
						currentSubstitution.bind(typeParams.get(i), node.getTypeArguments().get(i));
					}

					// Find the constructor MethodSymbol and emit/look up its specialization.
					Symbol ctorSym = genericCt.getMemberScope().resolveLocal(ctorIdent.name);
					if (ctorSym instanceof MethodSymbol ctorMs)
					{
						LLVMValueRef ctorFunction = LLVMGetNamedFunction(module, getSpecializationName(ctorMs));
						if (ctorFunction == null && ctorMs.getDeclarationNode() instanceof ConstructorDeclaration ctorDecl)
						{
							ctorFunction = visitConstructorDeclaration(ctorDecl);
						}
						if (ctorFunction != null)
						{
							// Also emit all other methods/operators of this generic struct under
							// the current substitution so that calls to them later resolve.
							if (ts.getDeclarationNode() instanceof StructDeclaration genericSd)
							{
								for (ASTNode member : genericSd.members)
								{
									if (member instanceof MethodDeclaration md)
									{
										Symbol memberSym = genericCt.getMemberScope().resolveLocal(md.name);
										if (memberSym instanceof MethodSymbol memberMs)
										{
											String specName = getSpecializationName(memberMs);
											LLVMValueRef existing = LLVMGetNamedFunction(module, specName);
											if (existing == null || existing.isNull()
													|| LLVMCountBasicBlocks(existing) == 0)
											{
												visitMethodDeclaration(md);
											}
										}
									}
									else if (member instanceof OperatorDeclaration od)
									{
										Symbol memberSym = genericCt.getMemberScope()
												.resolveLocal(operatorDeclKey(od));
										if (memberSym instanceof MethodSymbol memberMs)
										{
											String specName = getSpecializationName(memberMs);
											LLVMValueRef existing = LLVMGetNamedFunction(module, specName);
											if (existing == null || existing.isNull()
													|| LLVMCountBasicBlocks(existing) == 0)
											{
												visitOperatorDeclaration(od);
											}
										}
									}
								}
							}

							// Emit the full constructor call using the monomorphized composite type.
							LLVMValueRef result = emitConstructorCall(node, monoCt, ctorFunction);
							currentSubstitution = prevSub;
							return result;
						}
					}

					currentSubstitution = prevSub;
				}
			}
		}

		if (function == null)
		{
			function = node.target.accept(this);
		}

		if (function == null)
		{
			throw new CodegenException("Could not resolve function target for call");
		}

		Type targetType = analyzer.getType(node.target);
		if (targetType == null)
		{
			throw new CodegenException("Target of invocation has no type recorded: " + node.target);
		}
		if (!(targetType instanceof FunctionType ft))
		{
			// Struct/class constructor call: Vec2(x, y) where target resolved to StructType/ClassType
			if (targetType instanceof StructType st)
			{
				return emitConstructorCall(node, st, function);
			}
			if (targetType instanceof ClassType clst)
			{
				return emitConstructorCall(node, clst, function);
			}
			throw new CodegenException("Target of invocation is not a function: " + targetType.name() + " (at " + node.target + ")");
		}

		// Detect bare constructor call from inside the struct body:
		// FunctionType(VOID, [REF, ...]) with an IdentifierExpression whose name matches
		// a type in scope — this means Vec2(x, y) was called within the struct body.
		if (ft.returnType == PrimitiveType.VOID
				&& !ft.parameterTypes.isEmpty()
				&& ft.parameterTypes.get(0) == PrimitiveType.REF
				&& node.target instanceof IdentifierExpression ie)
		{
			Symbol targetSym = analyzer.getSymbol(node.target, Symbol.class);
			if (targetSym instanceof MethodSymbol ms2
					&& ms2.getDeclarationNode() instanceof org.nebula.nebc.ast.declarations.ConstructorDeclaration)
			{
				// Determine the composite type from the constructor's defining scope
				org.nebula.nebc.semantic.symbol.TypeSymbol owner = ms2.getDefinedIn() != null
						? (ms2.getDefinedIn().getOwner() instanceof org.nebula.nebc.semantic.symbol.TypeSymbol ts ? ts : null)
						: null;
				if (owner != null && owner.getType() instanceof CompositeType compositeOwner)
				{
					return emitConstructorCall(node, compositeOwner, function);
				}
			}
		}

		// Apply substitution to the function type used for calling
		if (node.getTypeArguments() != null)
		{
			Substitution callSub = new Substitution();
			Symbol sym = analyzer.getSymbol(node.target, Symbol.class);
			if (sym instanceof MethodSymbol ms)
			{
				for (int i = 0; i < ms.getTypeParameters().size(); i++)
				{
					callSub.bind(ms.getTypeParameters().get(i), node.getTypeArguments().get(i));
				}
				ft = (FunctionType) callSub.substitute(ft);
			}
		}

		if (currentSubstitution != null)
		{
			ft = (FunctionType) currentSubstitution.substitute(ft);
		}

		LLVMTypeRef llvmFuncType = toLLVMType(ft);

		// Robustness: if we are calling a concrete function declaration, use its type
		// this ensures we don't have a mismatch between 'ft' (which might be a template)
		// and the actual specialized function in LLVM.
		if (function != null && !function.isNull() && LLVMIsAFunction(function) != null && !LLVMIsAFunction(function).isNull())
		{
			LLVMTypeRef actualType = LLVMGlobalGetValueType(function);
			if (actualType != null)
			{
				llvmFuncType = actualType;
			}
		}

		int nebulaArgCount = node.arguments.size();
		int llvmArgCount = ft.parameterTypes.size();

		LLVMValueRef[] argsArr = new LLVMValueRef[llvmArgCount];
		int llvmArgIdx = 0;

		// System.out.println("[DEBUG] Calling " + ((LLVMIsAFunction(function) != null && !LLVMIsAFunction(function).isNull()) ? LLVMGetValueName(function).getString() : "function"));
		// System.out.println("[DEBUG]   Target Nebula Type: " + ft.name());
		// System.out.println("[DEBUG]   LLVM Call Type: " + LLVMPrintTypeToString(llvmFuncType).getString());
		// System.out.println("[DEBUG]   Arg Count: " + llvmArgCount);

		// If this is a member call, prepend the receiver as 'this'
		if (node.target instanceof MemberAccessExpression mae && llvmArgCount > nebulaArgCount)
		{
			LLVMValueRef receiver = mae.target.accept(this);
			Type receiverSemType = analyzer.getType(mae.target);
			Type thisParamType = ft.parameterTypes.get(0);

			// System.out.println("[DEBUG]   Receiver: " + receiverSemType.name() + " -> " + thisParamType.name());
			argsArr[llvmArgIdx++] = emitCast(receiver, receiverSemType, thisParamType);
		}

		for (int i = 0; i < nebulaArgCount; i++)
		{
			Expression argNode = node.arguments.get(i);
			LLVMValueRef argValue = argNode.accept(this);

			Type paramType = ft.parameterTypes.get(llvmArgIdx);
			Type argSemType = analyzer.getType(argNode);

			// System.out.println("[DEBUG]   Arg " + i + ": " + argSemType.name() + " -> " + paramType.name());
			argsArr[llvmArgIdx++] = emitCast(argValue, argSemType, paramType);
		}

		PointerPointer<LLVMValueRef> args = new PointerPointer<>(argsArr);
		String callName = ft.returnType == PrimitiveType.VOID ? "" : "call_tmp";

		return LLVMBuildCall2(builder, llvmFuncType, function, args, llvmArgCount, callName);
	}

	/**
	 * Emits a struct/class constructor call: allocates stack space, calls the
	 * constructor LLVM function with the alloca as 'this', and returns the loaded value.
	 *
	 * @param node     The invocation node (carries the user-provided arguments).
	 * @param ct       The composite type being constructed.
	 * @param ctorFunc The LLVM function value for the constructor (already resolved).
	 */
	private LLVMValueRef emitConstructorCall(InvocationExpression node, CompositeType ct, LLVMValueRef ctorFunc)
	{
		// Get the constructor MethodSymbol to obtain the full LLVM function type
		Symbol sym = analyzer.getSymbol(node.target, Symbol.class);
		FunctionType ctorFnType = null;
		if (sym instanceof MethodSymbol ms)
		{
			ctorFnType = ms.getType();
		}
		if (ctorFnType == null)
		{
			// Fall back: build param types manually from the arguments
			List<Type> pts = new ArrayList<>();
			pts.add(PrimitiveType.REF); // 'this'
			for (Expression arg : node.arguments)
			{
				pts.add(analyzer.getType(arg));
			}
			ctorFnType = new FunctionType(PrimitiveType.VOID, pts);
		}

		LLVMTypeRef structLlvmType = LLVMTypeMapper.getOrCreateStructType(context, ct);
		LLVMValueRef alloca = LLVMBuildAlloca(builder, structLlvmType, ct.name() + "_ctor");

		LLVMTypeRef ctorLlvmType = toLLVMType(ctorFnType);
		int llvmArgCount = ctorFnType.parameterTypes.size();
		LLVMValueRef[] argsArr = new LLVMValueRef[llvmArgCount];
		int argIdx = 0;

		// First param is 'this' — the alloca pointer
		argsArr[argIdx++] = alloca;

		// Remaining params come from node.arguments
		for (int i = 0; i < node.arguments.size() && argIdx < llvmArgCount; i++)
		{
			Expression argNode = node.arguments.get(i);
			LLVMValueRef argVal = argNode.accept(this);
			Type paramType = ctorFnType.parameterTypes.get(argIdx);
			Type argSemType = analyzer.getType(argNode);
			argsArr[argIdx++] = emitCast(argVal, argSemType, paramType);
		}

		PointerPointer<LLVMValueRef> argsPtr = new PointerPointer<>(argsArr);
		LLVMBuildCall2(builder, ctorLlvmType, ctorFunc, argsPtr, llvmArgCount, "");

		// Load and return the constructed value
		return LLVMBuildLoad2(builder, structLlvmType, alloca, ct.name() + "_val");
	}

	private String getSpecializationName(MethodSymbol ms)
	{
		if (currentSubstitution == null)
			return ms.getMangledName();
		List<Type> args = new ArrayList<>();
		if (!ms.getTypeParameters().isEmpty())
		{
			// Explicit type parameters on the method itself (e.g. generic functions)
			for (TypeParameterType tpt : ms.getTypeParameters())
			{
				args.add(currentSubstitution.substitute(tpt));
			}
		}
		else
		{
			// The method has no explicit type params but a substitution is active (e.g.
			// a constructor or non-generic method being monomorphized as part of a generic
			// struct instantiation).  Use all current substitution values, sorted by name
			// for determinism.
			currentSubstitution.getMapping().entrySet().stream()
				.sorted(java.util.Comparator.comparing(e -> e.getKey().name()))
				.forEach(e -> args.add(e.getValue()));
		}
		if (args.isEmpty())
			return ms.getMangledName();
		return getSpecializationName(ms, args);
	}

	private String getSpecializationName(MethodSymbol ms, List<Type> typeArgs)
	{
		StringBuilder sb = new StringBuilder(ms.getMangledName());
		sb.append("__");
		for (Type t : typeArgs)
		{
			sb.append("_").append(t.name().replaceAll("[^a-zA-Z0-9]", "_"));
		}
		return sb.toString();
	}


	@Override
	public LLVMValueRef visitMemberAccessExpression(MemberAccessExpression node)
	{
		Type baseType = analyzer.getType(node.target);
		LLVMValueRef base = null;
		// For type-namespaces (enums, unions, and namespace types) the target expression
		// has no LLVM value — we only need the type to look up the member discriminant.
		boolean baseIsTypeOnly = baseType instanceof NamespaceType
				|| baseType instanceof EnumType
				|| baseType instanceof UnionType;
		if (!baseIsTypeOnly)
		{
			base = node.target.accept(this);
		}

		if (currentSubstitution != null)
		{
			baseType = currentSubstitution.substitute(baseType);
		}

		// ── Safe navigation: opt?.member → Optional<memberType> ──────────────
		if (node.isSafe && baseType instanceof OptionalType ot)
		{
			return emitSafeNavigation(node, base, ot);
		}

		// ── Enum member access: Direction.North → i32 constant ───────────────
		if (baseType instanceof EnumType et)
		{
			String key = et.name() + "." + node.memberName;
			Integer disc = enumDiscriminants.get(key);
			if (disc != null)
			{
				return LLVMConstInt(LLVMInt32TypeInContext(context), disc, 0);
			}
		}

		// ── Namespace used as enum/union type name (e.g. Direction.North when
		//    Direction is resolved as a NamespaceType by the semantic analyser) ─
		if (baseType instanceof NamespaceType nt)
		{
			// Check enum discriminants
			String key = nt.name() + "." + node.memberName;
			Integer disc = enumDiscriminants.get(key);
			if (disc != null)
			{
				return LLVMConstInt(LLVMInt32TypeInContext(context), disc, 0);
			}
			// Check union discriminants (bare no-payload variant used as value)
			Integer uDisc = unionDiscriminants.get(key);
			if (uDisc != null)
			{
				// Emit a tagged union value with no payload (tag only)
				// We need to look up the union type; use the namespace name.
				// The union struct type was registered with key "union.TypeName".
				LLVMTypeRef i8t = LLVMInt8TypeInContext(context);
				LLVMTypeRef i32t = LLVMInt32TypeInContext(context);
				LLVMTypeRef payloadArr = LLVMArrayType(i8t, LLVMTypeMapper.UNION_PAYLOAD_BYTES);
				LLVMTypeRef unionStructType = LLVMGetTypeByName2(context, "union." + nt.name());
				if (unionStructType == null || unionStructType.isNull())
				{
					LLVMTypeRef[] fields = {i32t, payloadArr};
					unionStructType = LLVMStructCreateNamed(context, "union." + nt.name());
					LLVMStructSetBody(unionStructType, new PointerPointer<>(fields), 2, 0);
				}
				LLVMValueRef unionAlloca = LLVMBuildAlloca(builder, unionStructType, "union_tag_only");
				LLVMValueRef tagGep = LLVMBuildStructGEP2(builder, unionStructType, unionAlloca, 0, "tag_gep");
				LLVMBuildStore(builder, LLVMConstInt(i32t, uDisc, 0), tagGep);
				return LLVMBuildLoad2(builder, unionStructType, unionAlloca, "union_val");
			}
		}

		if (baseType == PrimitiveType.STR)
		{
			if (node.memberName.equals("ptr"))
			{
				return LLVMBuildExtractValue(builder, base, 0, "str_ptr_extract");
			}
			else if (node.memberName.equals("len"))
			{
				return LLVMBuildExtractValue(builder, base, 1, "str_len_extract");
			}
		}

		// Tuple member access: .0, .1 (positional) or .fieldName (named)
		if (baseType instanceof TupleType tt)
		{
			int fieldIdx = -1;
			try
			{
				fieldIdx = Integer.parseInt(node.memberName);
			}
			catch (NumberFormatException e)
			{
				fieldIdx = tt.indexOfField(node.memberName);
			}
			if (fieldIdx >= 0 && fieldIdx < tt.elementTypes.size())
			{
				// base is a struct value (loaded); extract the field by index
				LLVMTypeRef tupleStructType = LLVMTypeMapper.getOrCreateTupleType(context, tt);
				Type elemType = tt.elementTypes.get(fieldIdx);
				// Store to alloca and use GEP for field access
				LLVMValueRef tupleAlloca = LLVMBuildAlloca(builder, tupleStructType, "tuple_acc");
				LLVMBuildStore(builder, base, tupleAlloca);
				LLVMValueRef fieldGep = LLVMBuildStructGEP2(builder, tupleStructType, tupleAlloca, fieldIdx, "tuple_field");
				return LLVMBuildLoad2(builder, toLLVMType(elemType), fieldGep, node.memberName + "_load");
			}
		}

		// Handle trait method dispatch or normal member access
		Symbol memberSym = null;
		if (baseType instanceof CompositeType ct)
		{
			memberSym = ct.getMemberScope().resolve(node.memberName);
		}
		else if (baseType instanceof PrimitiveType pt)
		{
			SymbolTable tbl = analyzer.getPrimitiveImplScopes().get(pt);
			if (tbl != null)
			{
				memberSym = tbl.resolve(node.memberName);
			}
		}
		else if (baseType instanceof NamespaceType nt)
		{
			memberSym = nt.getMemberScope().resolve(node.memberName);
		}

		if (memberSym instanceof MethodSymbol ms)
		{
			// Return the function ref. Static trait dispatch means we call the concrete
			// implementation.
			String mangledName = ms.getMangledName();
			if (!ms.getTypeParameters().isEmpty())
			{
				mangledName = getSpecializationName(ms);
			}
			else if (baseType instanceof CompositeType ct && ct.name().contains("<"))
			{
				// The base type is a monomorphized generic (e.g. Pair<i32>).  The method
				// was emitted with a specialization suffix derived from the concrete type
				// args; reconstruct the same suffix here so the call resolves correctly.
				// Format mirrors getSpecializationName: base + "__" + ("_" + typeArg)*
				String typeArgPart = ct.name().substring(ct.name().indexOf('<') + 1, ct.name().lastIndexOf('>'));
				String[] typeArgNames = typeArgPart.split(",");
				StringBuilder specSuffix = new StringBuilder("__");
				for (String ta : typeArgNames)
				{
					specSuffix.append("_").append(ta.trim().replaceAll("[^a-zA-Z0-9]", "_"));
				}
				mangledName = mangledName + specSuffix;
			}

			LLVMValueRef func = LLVMGetNamedFunction(module, mangledName);
			if (func == null || func.isNull())
			{
				// Forward declaration
				FunctionType ft = ms.getType();
				if (currentSubstitution != null)
				{
					ft = (FunctionType) currentSubstitution.substitute(ft);
				}
				func = LLVMAddFunction(module, mangledName, toLLVMType(ft));
			}
			return func;
		}
		else if (memberSym instanceof VariableSymbol vs)
		{
			// Field access
			if (baseType == PrimitiveType.STR)
			{
				int fieldIdx = node.memberName.equals("ptr") ? 0 : 1;
				// str is a struct value in LLVM
				return LLVMBuildExtractValue(builder, base, fieldIdx, node.memberName + "_extract");
			}
			else if (baseType instanceof UnionType ut)
			{
				// No-payload union variant used as a value: Event.Stop → emit tag-only union struct.
				String key = ut.name() + "." + node.memberName;
				Integer uDisc = unionDiscriminants.get(key);
				if (uDisc != null)
				{
					LLVMTypeRef i8t = LLVMInt8TypeInContext(context);
					LLVMTypeRef i32t = LLVMInt32TypeInContext(context);
					LLVMTypeRef payloadArr = LLVMArrayType(i8t, LLVMTypeMapper.UNION_PAYLOAD_BYTES);
					LLVMTypeRef unionStructType = LLVMTypeMapper.getOrCreateUnionStructType(context, ut);
					LLVMValueRef unionAlloca = LLVMBuildAlloca(builder, unionStructType, "union_tag_only");
					LLVMValueRef tagGep = LLVMBuildStructGEP2(builder, unionStructType, unionAlloca, 0, "tag_gep");
					LLVMBuildStore(builder, LLVMConstInt(i32t, uDisc, 0), tagGep);
					return LLVMBuildLoad2(builder, unionStructType, unionAlloca, "union_val");
				}
			}
			else if (baseType instanceof CompositeType ct)
			{
				LLVMValueRef gep = emitMemberPointer(node);
				if (gep != null)
				{
					return LLVMBuildLoad2(builder, toLLVMType(vs.getType()), gep, node.memberName + "_load");
				}
			}
		}

		return null;
	}

	@Override
	public LLVMValueRef visitNewExpression(NewExpression node)
	{
		Type type = analyzer.getType(node);
		if (!(type instanceof ClassType ct))
		{
			throw new CodegenException("Cannot instantiate non-class type: " + type.name());
		}

		// 1. Allocate memory using neb_alloc
		LLVMValueRef nebAlloc = LLVMGetNamedFunction(module, "neb_alloc");
		if (nebAlloc == null)
		{
			FunctionType ft = new FunctionType(PrimitiveType.REF, List.of(PrimitiveType.U64), null);
			nebAlloc = LLVMAddFunction(module, "neb_alloc", toLLVMType(ft));
		}

		// Calculate size using LLVMSizeOf
		LLVMTypeRef structType = LLVMTypeMapper.getOrCreateStructType(context, ct);
		LLVMValueRef sizeVal = LLVMSizeOf(structType);

		LLVMValueRef[] allocArgsArr = {sizeVal};
		LLVMValueRef pointer = LLVMBuildCall2(builder, toLLVMType(new FunctionType(PrimitiveType.REF, List.of(PrimitiveType.U64), null)), nebAlloc, new PointerPointer<>(allocArgsArr), 1, "malloc_tmp");

		// 2. Resolve and call constructor
		Symbol constructorSym = ct.getMemberScope().resolve(ct.name());
		if (constructorSym instanceof MethodSymbol ms)
		{
			LLVMValueRef constructor = LLVMGetNamedFunction(module, ms.getMangledName());
			if (constructor == null)
			{
				constructor = LLVMAddFunction(module, ms.getMangledName(), toLLVMType(ms.getType()));
			}

			LLVMValueRef[] argsArr = new LLVMValueRef[node.arguments.size() + 1];
			argsArr[0] = pointer; // 'this'
			for (int i = 0; i < node.arguments.size(); i++)
			{
				LLVMValueRef rawArg = node.arguments.get(i).accept(this);
				Type argSemType = analyzer.getType(node.arguments.get(i));
				Type paramType = ms.getType().parameterTypes.get(i + 1);
				argsArr[i + 1] = emitCast(rawArg, argSemType, paramType);
			}

			LLVMBuildCall2(builder, toLLVMType(ms.getType()), constructor, new PointerPointer<>(argsArr), argsArr.length, "");
		}

		return pointer;
	}

	private void initializeFields(LLVMValueRef thisPtr, CompositeType ct, List<Declaration> members)
	{
		for (Declaration member : members)
		{
			if (member instanceof VariableDeclaration vd)
			{
				for (VariableDeclarator decl : vd.declarators)
				{
					if (decl.hasInitializer())
					{
						LLVMValueRef initVal = decl.initializer().accept(this);
						if (initVal != null)
						{
							LLVMValueRef gep = emitMemberPointer(thisPtr, ct, decl.name());
							if (gep != null)
							{
								Type fieldType = ((VariableSymbol) ct.getMemberScope().resolve(decl.name())).getType();
								LLVMValueRef castedVal = emitCast(initVal, analyzer.getType(decl.initializer()), fieldType);
								LLVMBuildStore(builder, castedVal, gep);
							}
						}
					}
				}
			}
		}
	}

	private LLVMValueRef emitMemberPointer(MemberAccessExpression node)
	{
		LLVMValueRef base = node.target.accept(this);
		Type baseType = analyzer.getType(node.target);
		if (currentSubstitution != null)
		{
			baseType = currentSubstitution.substitute(baseType);
		}

		if (!(baseType instanceof CompositeType ct))
		{
			return null;
		}

		return emitMemberPointer(base, ct, node.memberName);
	}

	private LLVMValueRef emitPointer(Expression expr)
	{
		if (expr instanceof IdentifierExpression id)
		{
			return namedValues.get(id.name);
		}
		else if (expr instanceof MemberAccessExpression mae)
		{
			return emitMemberPointer(mae);
		}
		else if (expr instanceof IndexExpression indexExpr)
		{
			LLVMValueRef base = indexExpr.target.accept(this);
			LLVMValueRef index = indexExpr.indices.get(0).accept(this);
			Type baseType = analyzer.getType(indexExpr.target);

			if (baseType == PrimitiveType.REF || baseType == PrimitiveType.STR)
			{
				LLVMValueRef[] indices = {index};
				LLVMTypeRef elemType = LLVMInt8TypeInContext(context);
				return LLVMBuildGEP2(builder, elemType, base, new PointerPointer<>(indices), 1, "ptr_idx");
			}
		}
		return null;
	}

	private LLVMValueRef emitMemberPointer(LLVMValueRef base, CompositeType ct, String memberName)
	{
		Symbol memberSym = ct.getMemberScope().resolve(memberName);
		if (!(memberSym instanceof VariableSymbol vs))
		{
			return null;
		}

		int fieldIdx = 0;
		boolean found = false;
		for (Symbol s : ct.getMemberScope().getSymbols().values())
		{
			if (s instanceof VariableSymbol field && !field.getName().equals("this"))
			{
				if (field == vs)
				{
					found = true;
					break;
				}
				fieldIdx++;
			}
		}

		if (found)
		{
			LLVMTypeRef structType = LLVMTypeMapper.getOrCreateStructType(context, ct);
			// Classes are pointers to structs, so base is already a pointer (i8*)
			// We can use it directly with GEP2 if we provide the correct struct type as the pointee.
			return LLVMBuildStructGEP2(builder, structType, base, fieldIdx, memberName + "_gep");
		}

		return null;
	}

	@Override
	public LLVMValueRef visitIndexExpression(IndexExpression node)
	{
		LLVMValueRef base = node.target.accept(this);
		LLVMValueRef index = node.indices.get(0).accept(this);
		Type baseType = analyzer.getType(node.target);

		if (baseType == PrimitiveType.REF || baseType == PrimitiveType.STR)
		{
			// Pointer indexing: GEP + Load
			LLVMValueRef[] indices = {index};
			LLVMTypeRef elemType = LLVMInt8TypeInContext(context);
			LLVMValueRef gep = LLVMBuildGEP2(builder, elemType, base, new PointerPointer<>(indices), 1, "ptr_idx");
			return LLVMBuildLoad2(builder, elemType, gep, "idx_load");
		}

		if (baseType instanceof ArrayType at)
		{
			// Array pointer indexing: GEP + Load
			LLVMTypeRef elemType = toLLVMType(at.baseType);
			LLVMValueRef[] indices = {index};
			LLVMValueRef gep = LLVMBuildGEP2(builder, elemType, base, new PointerPointer<>(indices), 1, "arr_idx");
			return LLVMBuildLoad2(builder, elemType, gep, "arr_load");
		}

		throw new CodegenException("Indexing only supported on Ref/string/array types for now.");
	}

	@Override
	public LLVMValueRef visitArrayLiteralExpression(ArrayLiteralExpression node)
	{
		if (node.elements.isEmpty())
		{
			// Return a null pointer for an empty array
			return LLVMConstPointerNull(LLVMPointerTypeInContext(context, 0));
		}

		// Determine element type from the first element
		Type elemSemType = analyzer.getType(node.elements.get(0));
		LLVMTypeRef elemType = toLLVMType(elemSemType);
		int count = node.elements.size();

		// Allocate [N x ElemType] on the stack
		LLVMTypeRef arrayType = LLVMArrayType(elemType, count);
		LLVMValueRef arrayAlloca = LLVMBuildAlloca(builder, arrayType, "arr_lit");

		// Store each element via GEP
		LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
		for (int i = 0; i < count; i++)
		{
			LLVMValueRef elemVal = node.elements.get(i).accept(this);
			if (elemVal == null)
				continue;
			LLVMValueRef castedElem = emitCast(elemVal, analyzer.getType(node.elements.get(i)), elemSemType);
			LLVMValueRef[] indices = {
				LLVMConstInt(i64t, 0, 0),
				LLVMConstInt(i64t, i, 0)
			};
			LLVMValueRef gep = LLVMBuildGEP2(builder, arrayType, arrayAlloca,
				new PointerPointer<>(indices), 2, "arr_elem_" + i);
			LLVMBuildStore(builder, castedElem, gep);
		}

		// Return a pointer to the first element (like C array decay)
		LLVMValueRef[] firstIdx = {
			LLVMConstInt(i64t, 0, 0),
			LLVMConstInt(i64t, 0, 0)
		};
		return LLVMBuildGEP2(builder, arrayType, arrayAlloca,
			new PointerPointer<>(firstIdx), 2, "arr_ptr");
	}

	@Override
	public LLVMValueRef visitTupleLiteralExpression(TupleLiteralExpression node)
	{
		int count = node.elements.size();
		if (count == 0)
			return null;

		// Use the SA-inferred TupleType to produce a consistent named LLVM struct.
		Type semType = analyzer.getType(node);
		LLVMTypeRef tupleType;
		if (semType instanceof TupleType tt)
		{
			tupleType = LLVMTypeMapper.getOrCreateTupleType(context, tt);
		}
		else
		{
			// Fallback: anonymous struct from element types
			LLVMTypeRef[] fieldTypes = new LLVMTypeRef[count];
			for (int i = 0; i < count; i++)
			{
				Type elemSemType = analyzer.getType(node.elements.get(i));
				fieldTypes[i] = toLLVMType(elemSemType);
			}
			tupleType = LLVMStructTypeInContext(context, new PointerPointer<>(fieldTypes), count, 0);
		}

		LLVMValueRef tupleAlloca = LLVMBuildAlloca(builder, tupleType, "tuple");

		for (int i = 0; i < count; i++)
		{
			LLVMValueRef elemVal = node.elements.get(i).accept(this);
			if (elemVal == null)
				continue;
			Type elemSemType = analyzer.getType(node.elements.get(i));
			Type targetElemType = (semType instanceof TupleType tt && i < tt.elementTypes.size())
					? tt.elementTypes.get(i)
					: elemSemType;
			LLVMValueRef castedElem = emitCast(elemVal, elemSemType, targetElemType);
			LLVMValueRef gep = LLVMBuildStructGEP2(builder, tupleType, tupleAlloca, i,
				"tuple_f" + i);
			LLVMBuildStore(builder, castedElem, gep);
		}

		return LLVMBuildLoad2(builder, tupleType, tupleAlloca, "tuple_val");
	}

	@Override
	public LLVMValueRef visitThisExpression(ThisExpression node)
	{
		LLVMValueRef pointer = namedValues.get("this");
		if (pointer != null)
		{
			Type type = analyzer.getType(node);
			LLVMTypeRef expectedType = toLLVMType(type);
			return LLVMBuildLoad2(builder, expectedType, pointer, "this_load");
		}
		throw new CodegenException("'this' referenced outside of method context");
	}

	@Override
	public LLVMValueRef visitStringInterpolationExpression(StringInterpolationExpression node)
	{
		// Strategy: build a format string from the parts, then call snprintf into a
		// fixed-size stack buffer, and return a { i8*, i64 } str struct.
		//
		// Literal string parts are emitted as-is; expression parts are formatted
		// with a type-specific printf specifier.

		final int BUF_SIZE = 512;
		LLVMTypeRef i8t   = LLVMInt8TypeInContext(context);
		LLVMTypeRef i32t  = LLVMInt32TypeInContext(context);
		LLVMTypeRef i64t  = LLVMInt64TypeInContext(context);
		LLVMTypeRef ptrT  = LLVMPointerTypeInContext(context, 0);

		// Allocate output buffer
		LLVMTypeRef bufType  = LLVMArrayType(i8t, BUF_SIZE);
		LLVMValueRef bufAlloca = LLVMBuildAlloca(builder, bufType, "interp_buf");
		LLVMValueRef[] firstIdx = {LLVMConstInt(i64t, 0, 0), LLVMConstInt(i64t, 0, 0)};
		LLVMValueRef bufPtr = LLVMBuildGEP2(builder, bufType, bufAlloca,
			new PointerPointer<>(firstIdx), 2, "buf_ptr");

		// Build the format string and collect expression args
		StringBuilder fmtSb = new StringBuilder();
		List<LLVMValueRef> fmtArgs = new ArrayList<>();

		for (Expression part : node.parts)
		{
			if (part instanceof LiteralExpression le && le.type == LiteralExpression.LiteralType.STRING)
			{
				// Raw string fragment — append verbatim (escape % signs)
				fmtSb.append(le.value.toString().replace("%", "%%"));
			}
			else
			{
				LLVMValueRef argVal = part.accept(this);
				if (argVal == null)
					continue;
				Type partType = analyzer.getType(part);
				if (partType == PrimitiveType.STR)
				{
					// Extract the i8* pointer from the str struct
					fmtSb.append("%s");
					fmtArgs.add(LLVMBuildExtractValue(builder, argVal, 0, "str_arg_ptr"));
				}
				else if (partType == PrimitiveType.F64 || partType == PrimitiveType.F32)
				{
					fmtSb.append("%g");
					fmtArgs.add(emitCast(argVal, partType, PrimitiveType.F64));
				}
				else if (partType == PrimitiveType.BOOL)
				{
					fmtSb.append("%d");
					fmtArgs.add(emitCast(argVal, partType, PrimitiveType.I32));
				}
				else if (partType == PrimitiveType.I64 || partType == PrimitiveType.U64)
				{
					fmtSb.append("%ld");
					fmtArgs.add(argVal);
				}
				else
				{
					// Default: treat as i32
					fmtSb.append("%d");
					fmtArgs.add(emitCast(argVal, partType, PrimitiveType.I32));
				}
			}
		}

		// Emit the format string as a global constant
		String fmtStr = fmtSb.toString();
		LLVMValueRef fmtGlobal = LLVMBuildGlobalStringPtr(builder, fmtStr, "interp_fmt");

		// Declare snprintf if needed: i32 snprintf(i8*, i64, i8*, ...)
		LLVMTypeRef snprintfType = LLVMFunctionType(i32t,
			new PointerPointer<>(new LLVMTypeRef[]{ptrT, i64t, ptrT}), 3, /* isVarArg */ 1);
		LLVMValueRef snprintf = getOrDeclareIntrinsic("snprintf", snprintfType);

		// Build arg list: buf, BUF_SIZE, fmt, [args...]
		int totalArgs = 3 + fmtArgs.size();
		LLVMValueRef[] callArgs = new LLVMValueRef[totalArgs];
		callArgs[0] = bufPtr;
		callArgs[1] = LLVMConstInt(i64t, BUF_SIZE, 0);
		callArgs[2] = fmtGlobal;
		for (int i = 0; i < fmtArgs.size(); i++)
			callArgs[3 + i] = fmtArgs.get(i);

		LLVMValueRef writtenI32 = LLVMBuildCall2(builder, snprintfType, snprintf,
			new PointerPointer<>(callArgs), totalArgs, "snprintf_res");

		// Build the resulting str struct: { i8* buf, i64 len }
		LLVMValueRef writtenI64 = LLVMBuildSExt(builder, writtenI32, i64t, "interp_len");
		LLVMTypeRef strStructType = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);
		LLVMValueRef strStruct = LLVMBuildAlloca(builder, strStructType, "interp_str");
		LLVMValueRef ptrField = LLVMBuildStructGEP2(builder, strStructType, strStruct, 0, "istr_ptr_f");
		LLVMValueRef lenField = LLVMBuildStructGEP2(builder, strStructType, strStruct, 1, "istr_len_f");
		LLVMBuildStore(builder, bufPtr, ptrField);
		LLVMBuildStore(builder, writtenI64, lenField);
		return LLVMBuildLoad2(builder, strStructType, strStruct, "interp_str_val");
	}

	private void emitIfExpressionBranch(LLVMBasicBlockRef bb, ExpressionBlock expr, LLVMValueRef resultPtr, LLVMBasicBlockRef mergeBB)
	{
		LLVMPositionBuilderAtEnd(builder, bb);
		currentBlockTerminated = false;
		LLVMValueRef val = expr.accept(this);
		if (!currentBlockTerminated)
		{
			if (resultPtr != null && val != null)
			{
				LLVMBuildStore(builder, val, resultPtr);
			}
			LLVMBuildBr(builder, mergeBB);
		}
	}

	@Override
	public LLVMValueRef visitIfExpression(IfExpression node)
	{
		Type resultType = analyzer.getType(node);
		LLVMValueRef resultPtr = resultType != PrimitiveType.VOID ? LLVMBuildAlloca(builder, toLLVMType(resultType), "if_expr_res") : null;

		LLVMValueRef cond = node.condition.accept(this);
		if (cond == null)
			return null;

		LLVMBasicBlockRef thenBB = LLVMAppendBasicBlockInContext(context, currentFunction, "if_expr_then");
		LLVMBasicBlockRef elseBB = LLVMAppendBasicBlockInContext(context, currentFunction, "if_expr_else");
		LLVMBasicBlockRef mergeBB = LLVMAppendBasicBlockInContext(context, currentFunction, "if_merge");

		LLVMBuildCondBr(builder, cond, thenBB, elseBB);

		// Emit "then" and "else" branches
		emitIfExpressionBranch(thenBB, node.thenExpressionBlock, resultPtr, mergeBB);
		emitIfExpressionBranch(elseBB, node.elseExpressionBlock, resultPtr, mergeBB);

		LLVMPositionBuilderAtEnd(builder, mergeBB);
		currentBlockTerminated = false;

		return resultPtr != null ? LLVMBuildLoad2(builder, toLLVMType(resultType), resultPtr, "if_expr_val") : null;
	}

	@Override
	public LLVMValueRef visitMatchExpression(MatchExpression node)
	{
		// -----------------------------------------------------------------
		// 1. Evaluate the selector
		// -----------------------------------------------------------------
		LLVMValueRef selectorVal = node.selector.accept(this);
		if (selectorVal == null)
			return null;

		Type selectorType = analyzer.getType(node.selector);
		Type resultType   = analyzer.getType(node);
		LLVMTypeRef llvmResultType = (resultType != null && resultType != PrimitiveType.VOID)
			? toLLVMType(resultType)
			: null;

		// -----------------------------------------------------------------
		// 2. Allocate a result slot and create the merge block
		// -----------------------------------------------------------------
		LLVMValueRef resultPtr = (llvmResultType != null)
			? LLVMBuildAlloca(builder, llvmResultType, "match_res")
			: null;

		LLVMBasicBlockRef mergeBB = LLVMAppendBasicBlockInContext(context, currentFunction, "match_merge");

		// -----------------------------------------------------------------
		// 3. Determine selector kind and extract tag / value to match on
		// -----------------------------------------------------------------
		boolean isEnumMatch  = selectorType instanceof EnumType;
		boolean isUnionMatch = selectorType instanceof UnionType;

		LLVMValueRef tagVal = null;
		String unionTypeName = null;
		if (isUnionMatch)
		{
			// tagged-union: field 0 is the i32 discriminant
			LLVMTypeRef unionStructType = LLVMTypeMapper.getOrCreateUnionStructType(context, (UnionType) selectorType);
			tagVal = LLVMBuildExtractValue(builder, selectorVal, 0, "union_tag");
			unionTypeName = selectorType.name();
		}
		else if (isEnumMatch)
		{
			// enum is already an i32 value
			tagVal = selectorVal;
		}

		// -----------------------------------------------------------------
		// 4. Emit each arm
		// -----------------------------------------------------------------
		LLVMBasicBlockRef defaultBB = null; // used for wildcard arm

		for (int armIdx = 0; armIdx < node.arms.size(); armIdx++)
		{
			MatchArm arm = node.arms.get(armIdx);
			Pattern pat  = arm.pattern;

			LLVMBasicBlockRef armBB = LLVMAppendBasicBlockInContext(context, currentFunction,
				"match_arm_" + armIdx);
			LLVMBasicBlockRef nextBB = LLVMAppendBasicBlockInContext(context, currentFunction,
				"match_next_" + armIdx);

			// --- Condition check (position = current insert point) ---
			LLVMValueRef cond = emitPatternCondition(pat, selectorVal, selectorType, tagVal, unionTypeName);

			if (cond == null)
			{
				// Wildcard / always-true: fall through unconditionally
				LLVMBuildBr(builder, armBB);
				defaultBB = armBB;
			}
			else
			{
				LLVMBuildCondBr(builder, cond, armBB, nextBB);
			}

			// --- Arm body ---
			LLVMPositionBuilderAtEnd(builder, armBB);
			currentBlockTerminated = false;

			// For destructuring patterns: bind payload fields into namedValues
			Map<String, LLVMValueRef> armBindings = emitDestructuringBindings(
				pat, selectorVal, selectorType);
			Map<String, LLVMValueRef> prevNamedValues = null;
			if (!armBindings.isEmpty())
			{
				prevNamedValues = new HashMap<>(namedValues);
				namedValues.putAll(armBindings);
			}

			LLVMValueRef armResult = arm.result.accept(this);

			if (!currentBlockTerminated)
			{
				if (resultPtr != null && armResult != null)
					LLVMBuildStore(builder, armResult, resultPtr);
				LLVMBuildBr(builder, mergeBB);
			}

			if (prevNamedValues != null)
			{
				namedValues.clear();
				namedValues.putAll(prevNamedValues);
			}

			// --- Prepare for next arm ---
			LLVMPositionBuilderAtEnd(builder, nextBB);
			currentBlockTerminated = false;
		}

		// If the last arm was not a wildcard the final 'nextBB' is the fallthrough;
		// jump to merge (undefined behaviour in Nebula to reach here, but keep IR valid)
		if (!currentBlockTerminated)
			LLVMBuildBr(builder, mergeBB);

		// -----------------------------------------------------------------
		// 5. Merge block
		// -----------------------------------------------------------------
		LLVMPositionBuilderAtEnd(builder, mergeBB);
		currentBlockTerminated = false;

		return (resultPtr != null)
			? LLVMBuildLoad2(builder, llvmResultType, resultPtr, "match_val")
			: null;
	}

	/**
	 * Emits an {@code i1} condition that is true when {@code pat} matches
	 * {@code selectorVal}.  Returns {@code null} for always-matching patterns
	 * (wildcard, type patterns we don't yet lower).
	 */
	private LLVMValueRef emitPatternCondition(
		Pattern pat,
		LLVMValueRef selectorVal,
		Type selectorType,
		LLVMValueRef tagVal,
		String unionTypeName)
	{
		if (pat instanceof WildcardPattern)
			return null; // unconditional match

		if (pat instanceof LiteralPattern lp)
		{
			LLVMValueRef litVal = lp.value.accept(this);
			Type litType = analyzer.getType(lp.value);
			boolean isFloat = isFloatType(selectorType);
			if (isFloat)
				return LLVMBuildFCmp(builder, LLVMRealOEQ, selectorVal,
					emitCast(litVal, litType, selectorType), "lit_feq");
			return LLVMBuildICmp(builder, LLVMIntEQ, selectorVal,
				emitCast(litVal, litType, selectorType), "lit_eq");
		}

		if (pat instanceof TypePattern tp)
		{
			// Enum member access — TypePattern wraps a NamedType whose qualifiedName is
			// e.g. "Direction.North" or just "North" when matched against a Direction.
			// We need to resolve this to a discriminant and compare tagVal.
			String typeName = (tp.type instanceof org.nebula.nebc.ast.types.NamedType nt2)
				? nt2.qualifiedName
				: tp.type.getClass().getSimpleName();
			if (tagVal != null)
			{
				// Try qualified key first, then unqualified with union/enum prefix.
				Integer disc = enumDiscriminants.get(typeName);
				if (disc == null)
					disc = unionDiscriminants.get(typeName);
				if (disc == null && selectorType != null)
				{
					disc = enumDiscriminants.get(selectorType.name() + "." + typeName);
					if (disc == null)
						disc = unionDiscriminants.get(selectorType.name() + "." + typeName);
				}
				if (disc != null)
				{
					LLVMValueRef discConst = LLVMConstInt(LLVMInt32TypeInContext(context), disc, 0);
					return LLVMBuildICmp(builder, LLVMIntEQ, tagVal, discConst, "tag_eq");
				}
			}
			// Fallback: unknown type pattern — treat as wildcard
			return null;
		}

		if (pat instanceof DestructuringPattern dp)
		{
			// Union variant destructuring — compare the tag discriminant.
			if (tagVal == null)
				return null;
			String key = (unionTypeName != null)
				? unionTypeName + "." + dp.variantName
				: dp.variantName;
			Integer disc = unionDiscriminants.get(key);
			if (disc == null)
				disc = unionDiscriminants.get(dp.variantName);
			if (disc == null)
				return null;
			LLVMValueRef discConst = LLVMConstInt(LLVMInt32TypeInContext(context), disc, 0);
			return LLVMBuildICmp(builder, LLVMIntEQ, tagVal, discConst, "dtag_eq");
		}

		if (pat instanceof OrPattern op)
		{
			// Emit a chain of OR conditions across all alternatives.
			LLVMValueRef result = null;
			for (Pattern alt : op.alternatives)
			{
				LLVMValueRef altCond = emitPatternCondition(alt, selectorVal, selectorType, tagVal, unionTypeName);
				if (altCond == null)
					return null; // one alternative is wildcard — whole OR matches everything
				result = (result == null) ? altCond : LLVMBuildOr(builder, result, altCond, "or_pat");
			}
			return result;
		}

		return null; // unknown pattern — treat as wildcard
	}

	/**
	 * For destructuring patterns on tagged unions, allocates binding allocas
	 * for each bound variable and extracts the payload.
	 */
	private Map<String, LLVMValueRef> emitDestructuringBindings(
		Pattern pat,
		LLVMValueRef selectorVal,
		Type selectorType)
	{
		Map<String, LLVMValueRef> bindings = new HashMap<>();
		if (!(pat instanceof DestructuringPattern dp))
			return bindings;
		if (!(selectorType instanceof UnionType ut))
			return bindings;

		// Locate the variant's payload type via the union's member scope
		String key = ut.name() + "." + dp.variantName;
		Integer disc = unionDiscriminants.get(key);
		if (disc == null)
			return bindings;

		// Field 1 of the union struct is the opaque [16 x i8] payload.
		// We bitcast it to a pointer of the payload struct and GEP into it.
		LLVMTypeRef unionStructType = LLVMTypeMapper.getOrCreateUnionStructType(context, ut);
		LLVMTypeRef i8PtrType = LLVMPointerTypeInContext(context, 0);
		LLVMTypeRef i8t = LLVMInt8TypeInContext(context);

		// alloca the whole union struct, store the value, then GEP into it
		LLVMValueRef unionAlloca = LLVMBuildAlloca(builder, unionStructType, "union_match");
		LLVMBuildStore(builder, selectorVal, unionAlloca);

		// GEP to field 1 (payload)
		LLVMValueRef payloadGep = LLVMBuildStructGEP2(builder, unionStructType, unionAlloca, 1, "payload_gep");

		// Resolve binding types from the union's variant scope
		org.nebula.nebc.semantic.SymbolTable memberScope = ut.getMemberScope();
		for (int i = 0; i < dp.bindings.size(); i++)
		{
			String bindingName = dp.bindings.get(i);
			// The payload is at byte offset i * sizeof(T).
			// For simplicity emit an i64 load and let the caller use it.
			// A proper implementation would look up the variant field type from the AST.
			// Here we use i64 as a safe default (covers pointer-sized and smaller types).
			LLVMTypeRef bindType = LLVMInt64TypeInContext(context);

			// Byte offset: i * 8 (assuming 8-byte fields)
			LLVMValueRef[] gepIdx = {
				LLVMConstInt(LLVMInt64TypeInContext(context), (long) i * 8, 0)
			};
			LLVMValueRef fieldPtr = LLVMBuildGEP2(builder, i8t, payloadGep,
				new PointerPointer<>(gepIdx), 1, bindingName + "_field_ptr");

			LLVMValueRef bindingAlloca = LLVMBuildAlloca(builder, bindType, bindingName);
			LLVMValueRef loaded = LLVMBuildLoad2(builder, bindType, fieldPtr, bindingName + "_raw");
			LLVMBuildStore(builder, loaded, bindingAlloca);
			bindings.put(bindingName, bindingAlloca);
		}

		return bindings;
	}

	@Override
	public LLVMValueRef visitMatchArm(MatchArm node)
	{
		// Arms are emitted inline inside visitMatchExpression.
		return null;
	}

	@Override
	public LLVMValueRef visitLiteralPattern(LiteralPattern node)
	{
		// Literal pattern values are emitted inside emitPatternCondition.
		return node.value.accept(this);
	}

	@Override
	public LLVMValueRef visitTypePattern(TypePattern node)
	{
		// Handled inside emitPatternCondition.
		return null;
	}

	@Override
	public LLVMValueRef visitWildcardPattern(WildcardPattern node)
	{
		// Wildcard always matches — no IR needed
		return null;
	}

	@Override
	public LLVMValueRef visitOrPattern(OrPattern node)
	{
		// Handled inside emitPatternCondition.
		return null;
	}

	@Override
	public LLVMValueRef visitTagAtom(TagAtom node)
	{
		// Tags are metadata — no IR
		return null;
	}

	@Override
	public LLVMValueRef visitTagOperation(TagOperation node)
	{
		// Tags are metadata — no IR
		return null;
	}

	@Override
	public LLVMValueRef visitTypeReference(TypeNode node)
	{
		// Type references don't produce runtime values
		return null;
	}

	// =========================================================================
	// Optional type operations
	// =========================================================================

	@Override
	public LLVMValueRef visitNoneExpression(NoneExpression node)
	{
		// Emit { i1 false, undef } for the contextual optional type.
		Type optType = analyzer.getType(node);
		if (!(optType instanceof OptionalType ot))
		{
			// Fallback: just return an i1 false
			return LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0);
		}

		LLVMTypeRef optStructType = LLVMTypeMapper.getOrCreateOptionalStructType(context, ot);
		LLVMValueRef optAlloca = LLVMBuildAlloca(builder, optStructType, "none");

		// Field 0 = i1 false (absent)
		LLVMValueRef presentGep = LLVMBuildStructGEP2(builder, optStructType, optAlloca, 0, "none_present");
		LLVMBuildStore(builder, LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0), presentGep);

		// Field 1 = undef (value is irrelevant when absent)
		LLVMValueRef valueGep = LLVMBuildStructGEP2(builder, optStructType, optAlloca, 1, "none_val");
		LLVMTypeRef innerLLVMType = toLLVMType(ot.innerType);
		LLVMBuildStore(builder, LLVMGetUndef(innerLLVMType), valueGep);

		return LLVMBuildLoad2(builder, optStructType, optAlloca, "none_val");
	}

	@Override
	public LLVMValueRef visitForcedUnwrapExpression(ForcedUnwrapExpression node)
	{
		// Emit presence check: if absent, panic; otherwise extract value.
		LLVMValueRef optVal = node.operand.accept(this);
		if (optVal == null)
			return null;

		Type optType = analyzer.getType(node.operand);
		if (!(optType instanceof OptionalType ot))
		{
			// Not actually optional — just pass through
			return optVal;
		}

		LLVMTypeRef optStructType = LLVMTypeMapper.getOrCreateOptionalStructType(context, ot);

		// Extract the present flag (field 0)
		LLVMValueRef presentFlag = LLVMBuildExtractValue(builder, optVal, 0, "present_flag");

		LLVMBasicBlockRef okBB    = LLVMAppendBasicBlockInContext(context, currentFunction, "unwrap_ok");
		LLVMBasicBlockRef panicBB = LLVMAppendBasicBlockInContext(context, currentFunction, "unwrap_panic");
		LLVMBuildCondBr(builder, presentFlag, okBB, panicBB);

		// Panic block
		LLVMPositionBuilderAtEnd(builder, panicBB);
		currentBlockTerminated = false;
		emitPanicCall("Forced unwrap of none optional");
		LLVMBuildUnreachable(builder);
		currentBlockTerminated = true;

		// OK block — extract value (field 1)
		LLVMPositionBuilderAtEnd(builder, okBB);
		currentBlockTerminated = false;
		return LLVMBuildExtractValue(builder, optVal, 1, "unwrap_val");
	}

	@Override
	public LLVMValueRef visitNullCoalescingExpression(NullCoalescingExpression node)
	{
		// Emit: if left.present → left.value, else → right
		LLVMValueRef optVal = node.left.accept(this);
		if (optVal == null)
			return node.right.accept(this);

		Type optType = analyzer.getType(node.left);
		if (!(optType instanceof OptionalType ot))
		{
			// Left is not optional — just return it
			return optVal;
		}

		LLVMTypeRef optStructType = LLVMTypeMapper.getOrCreateOptionalStructType(context, ot);
		Type resultType = analyzer.getType(node);
		LLVMTypeRef resultLLVMType = toLLVMType(resultType);

		LLVMValueRef resultAlloca = LLVMBuildAlloca(builder, resultLLVMType, "coalesce_res");

		LLVMValueRef presentFlag = LLVMBuildExtractValue(builder, optVal, 0, "coalesce_present");

		LLVMBasicBlockRef presentBB = LLVMAppendBasicBlockInContext(context, currentFunction, "coalesce_present");
		LLVMBasicBlockRef absentBB  = LLVMAppendBasicBlockInContext(context, currentFunction, "coalesce_absent");
		LLVMBasicBlockRef mergeBB   = LLVMAppendBasicBlockInContext(context, currentFunction, "coalesce_merge");

		LLVMBuildCondBr(builder, presentFlag, presentBB, absentBB);

		// Present branch
		LLVMPositionBuilderAtEnd(builder, presentBB);
		currentBlockTerminated = false;
		LLVMValueRef innerVal = LLVMBuildExtractValue(builder, optVal, 1, "coalesce_inner");
		LLVMValueRef castedInner = emitCast(innerVal, ot.innerType, resultType);
		LLVMBuildStore(builder, castedInner, resultAlloca);
		LLVMBuildBr(builder, mergeBB);

		// Absent branch
		LLVMPositionBuilderAtEnd(builder, absentBB);
		currentBlockTerminated = false;
		LLVMValueRef fallback = node.right.accept(this);
		if (fallback != null)
		{
			LLVMValueRef castedFallback = emitCast(fallback, analyzer.getType(node.right), resultType);
			LLVMBuildStore(builder, castedFallback, resultAlloca);
		}
		LLVMBuildBr(builder, mergeBB);

		// Merge
		LLVMPositionBuilderAtEnd(builder, mergeBB);
		currentBlockTerminated = false;
		return LLVMBuildLoad2(builder, resultLLVMType, resultAlloca, "coalesce_val");
	}

	/**
	 * Emits a call to the runtime panic function (declared in std/runtime).
	 * On failure to find the function, emits a no-op and continues (for tests
	 * that don't link the runtime).
	 */
	private void emitPanicCall(String message)
	{
		LLVMTypeRef i8t   = LLVMInt8TypeInContext(context);
		LLVMTypeRef i64t  = LLVMInt64TypeInContext(context);
		LLVMTypeRef ptrT  = LLVMPointerTypeInContext(context, 0);
		LLVMTypeRef strStructType = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);

		// Try known panic function names
		String[] candidates = {"panic_msg", "neb__panic", "nebula_panic"};
		LLVMValueRef panicFn = null;
		for (String name : candidates)
		{
			panicFn = LLVMGetNamedFunction(module, name);
			if (panicFn != null && !panicFn.isNull())
				break;
		}

		if (panicFn == null || panicFn.isNull())
		{
			// Declare a void panic_msg(str) for linking later
			LLVMTypeRef panicType = LLVMFunctionType(LLVMVoidTypeInContext(context),
				new PointerPointer<>(new LLVMTypeRef[]{strStructType}), 1, 0);
			panicFn = LLVMAddFunction(module, "panic_msg", panicType);
		}

		// Build a str constant for the message
		LLVMValueRef msgPtr = LLVMBuildGlobalStringPtr(builder, message, "panic_lit");
		LLVMValueRef msgLen = LLVMConstInt(i64t, message.length(), 0);

		LLVMValueRef msgAlloca = LLVMBuildAlloca(builder, strStructType, "panic_str");
		LLVMValueRef ptrF = LLVMBuildStructGEP2(builder, strStructType, msgAlloca, 0, "pmsg_ptr");
		LLVMValueRef lenF = LLVMBuildStructGEP2(builder, strStructType, msgAlloca, 1, "pmsg_len");
		LLVMBuildStore(builder, msgPtr, ptrF);
		LLVMBuildStore(builder, msgLen, lenF);
		LLVMValueRef msgVal = LLVMBuildLoad2(builder, strStructType, msgAlloca, "panic_str_val");

		LLVMTypeRef panicFnType = LLVMGlobalGetValueType(panicFn);
		LLVMValueRef[] args = {msgVal};
		LLVMBuildCall2(builder, panicFnType, panicFn, new PointerPointer<>(args), 1, "");
	}

	@Override
	public LLVMValueRef visitDestructuringPattern(DestructuringPattern node)
	{
		// Destructuring patterns are handled inside match codegen — no standalone IR.
		return null;
	}

	// =========================================================================
	// Break / Continue
	// =========================================================================

	@Override
	public LLVMValueRef visitBreakStatement(BreakStatement node)
	{
		if (currentLoopExitBB != null && !currentBlockTerminated)
		{
			LLVMBuildBr(builder, currentLoopExitBB);
			currentBlockTerminated = true;
		}
		return null;
	}

	@Override
	public LLVMValueRef visitContinueStatement(ContinueStatement node)
	{
		if (currentLoopContinueBB != null && !currentBlockTerminated)
		{
			LLVMBuildBr(builder, currentLoopContinueBB);
			currentBlockTerminated = true;
		}
		return null;
	}

}
