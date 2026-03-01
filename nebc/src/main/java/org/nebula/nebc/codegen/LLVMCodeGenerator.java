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
import java.util.List;
import java.util.Map;

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

	public LLVMCodeGenerator()
	{
	}

	// =================================================================
	// PUBLIC API
	// =================================================================

	private LLVMValueRef emitCast(LLVMValueRef value, Type srcSemType, Type targetSemType)
	{
		if (value == null || srcSemType.equals(targetSemType))
			return value;

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
		else
		{
			// If already emitted (could happen if specialization called twice in same unit)
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
		namedValues.clear();

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
			Type paramType = funcType.parameterTypes.get(llvmParamIdx - 1);
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
				LLVMValueRef castedResult = emitCast(bodyResult, bodySemType, returnType);
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
		if (prevInsertBlock != null)
		{
			LLVMPositionBuilderAtEnd(builder, prevInsertBlock);
		}

		namedValues.clear();
		namedValues.putAll(prevNamedValues);

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
	public LLVMValueRef visitVariableDeclaration(VariableDeclaration node)
	{
		for (VariableDeclarator decl : node.declarators)
		{
			String varName = decl.name();
			Type type = resolveDeclaratorType(node);
			LLVMValueRef alloca = LLVMBuildAlloca(builder, toLLVMType(type), varName);

			if (decl.hasInitializer())
			{
				LLVMValueRef initVal = decl.initializer().accept(this);
				if (initVal != null)
				{
					LLVMValueRef castedVal = emitCast(initVal, analyzer.getType(decl.initializer()), type);
					LLVMBuildStore(builder, castedVal, alloca);
				}
			}

			namedValues.put(varName, alloca);
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
		// TODO: Emit constant definitions
		return null;
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
		// Enums are typically just type definitions and do not generate code directly
		// at the declaration,
		// unless they have methods or we need to generate runtime type information.
		return null;
	}

	@Override
	public LLVMValueRef visitUnionDeclaration(UnionDeclaration node)
	{
		// TODO: Emit tagged union type
		return null;
	}

	@Override
	public LLVMValueRef visitUnionVariant(UnionVariant node)
	{
		// TODO: Emit union variant discriminator + payload
		return null;
	}

	@Override
	public LLVMValueRef visitOperatorDeclaration(OperatorDeclaration node)
	{
		// TODO: Emit operator overload as a named function
		return null;
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
		else
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
		namedValues.clear();

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

			Type paramType = funcType.parameterTypes.get(llvmParamIdx - 1);
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
				LLVMValueRef castedResult = emitCast(value, analyzer.getType(node.value), currentMethodReturnType);
				LLVMBuildRet(builder, castedResult);
			}
			else
			{
				LLVMBuildRetVoid(builder);
			}
		}
		currentBlockTerminated = true;
		return null;
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

		// 4. Body
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
		// 4. Body Block: Execute Statements
		// ---------------------------------------------------------
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
		// TODO: Emit iterator-based loop
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
		LLVMValueRef lVal = node.left.accept(this);
		LLVMValueRef rVal = node.right.accept(this);

		if (lVal == null || rVal == null)
			return null;

		Type leftType = analyzer.getType(node.left);
		Type rightType = analyzer.getType(node.right);
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
			case EQ, NE, LT, GT, LE, GE -> emitComparisonOp(lVal, rVal, node.operator, isFloat, isUnsigned);
			case LOGICAL_AND, LOGICAL_OR, BIT_AND, BIT_OR, BIT_XOR, SHL, SHR -> emitBitwiseOp(lVal, rVal, node.operator, isUnsigned);
			default -> null;
		};
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
			LLVMValueRef castedVal = emitCast(value, analyzer.getType(node.value), targetSemType);
			LLVMBuildStore(builder, castedVal, pointer);
			return castedVal;
		}
		else if (node.target instanceof MemberAccessExpression mae)
		{
			LLVMValueRef pointer = emitMemberPointer(mae);
			if (pointer == null)
			{
				throw new CodegenException("Cannot get pointer for member: " + mae.memberName);
			}

			Type targetSemType = analyzer.getType(mae);
			LLVMValueRef castedVal = emitCast(value, analyzer.getType(node.value), targetSemType);
			LLVMBuildStore(builder, castedVal, pointer);
			return castedVal;
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
				LLVMValueRef castedVal = emitCast(value, analyzer.getType(node.value), targetSemType);
				LLVMBuildStore(builder, castedVal, gep);
				return castedVal;
			}
		}

		throw new CodegenException("Unsupported assignment target: " + node.target.getClass().getSimpleName());
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

	@Override
	public LLVMValueRef visitIdentifierExpression(IdentifierExpression node)
	{
		LLVMValueRef pointer = namedValues.get(node.name);
		if (pointer != null)
		{
			Type type = getVariableType(node);
			LLVMTypeRef expectedType = toLLVMType(type);
			return LLVMBuildLoad2(builder, expectedType, pointer, node.name + "_load");
		}

		// Try to resolve as a function
		LLVMValueRef func = resolveFunctionReference(node);
		if (func != null)
		{
			return func;
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
					function = visitMethodDeclaration(decl);

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
		if (!(targetType instanceof FunctionType ft))
		{
			throw new CodegenException("Target of invocation is not a function: " + targetType.name());
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

		// Debugging
		System.out.println("[DEBUG] Calling " + ((LLVMIsAFunction(function) != null && !LLVMIsAFunction(function).isNull()) ? LLVMGetValueName(function).getString() : "function"));
		System.out.println("[DEBUG]   Target Nebula Type: " + ft.name());
		System.out.println("[DEBUG]   LLVM Call Type: " + LLVMPrintTypeToString(llvmFuncType).getString());
		System.out.println("[DEBUG]   Arg Count: " + llvmArgCount);

		// If this is a member call, prepend the receiver as 'this'
		if (node.target instanceof MemberAccessExpression mae && llvmArgCount > nebulaArgCount)
		{
			LLVMValueRef receiver = mae.target.accept(this);
			Type receiverSemType = analyzer.getType(mae.target);
			Type thisParamType = ft.parameterTypes.get(0);

			System.out.println("[DEBUG]   Receiver: " + receiverSemType.name() + " -> " + thisParamType.name());
			argsArr[llvmArgIdx++] = emitCast(receiver, receiverSemType, thisParamType);
		}

		for (int i = 0; i < nebulaArgCount; i++)
		{
			Expression argNode = node.arguments.get(i);
			LLVMValueRef argValue = argNode.accept(this);

			Type paramType = ft.parameterTypes.get(llvmArgIdx);
			Type argSemType = analyzer.getType(argNode);

			System.out.println("[DEBUG]   Arg " + i + ": " + argSemType.name() + " -> " + paramType.name());
			argsArr[llvmArgIdx++] = emitCast(argValue, argSemType, paramType);
		}

		PointerPointer<LLVMValueRef> args = new PointerPointer<>(argsArr);
		String callName = ft.returnType == PrimitiveType.VOID ? "" : "call_tmp";

		return LLVMBuildCall2(builder, llvmFuncType, function, args, llvmArgCount, callName);
	}

	private String getSpecializationName(MethodSymbol ms)
	{
		if (currentSubstitution == null)
			return ms.getMangledName();
		List<Type> args = new ArrayList<>();
		for (TypeParameterType tpt : ms.getTypeParameters())
		{
			args.add(currentSubstitution.substitute(tpt));
		}
		return getSpecializationName(ms, args);
	}

	private String getSpecializationName(MethodSymbol ms, List<Type> typeArgs)
	{
		StringBuilder sb = new StringBuilder(ms.getMangledName());
		sb.append("__");
		for (Type t : typeArgs)
		{
			sb.append("_").append(t.name().replaceAll("[^a-zA-Z0-0]", "_"));
		}
		return sb.toString();
	}


	@Override
	public LLVMValueRef visitMemberAccessExpression(MemberAccessExpression node)
	{
		LLVMValueRef base = node.target.accept(this);
		Type baseType = analyzer.getType(node.target);
		if (currentSubstitution != null)
		{
			baseType = currentSubstitution.substitute(baseType);
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

		if (memberSym instanceof MethodSymbol ms)
		{
			// Return the function ref. Static trait dispatch means we call the concrete
			// implementation.
			String mangledName = ms.getMangledName();
			if (!ms.getTypeParameters().isEmpty())
			{
				mangledName = getSpecializationName(ms);
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

		throw new CodegenException("Indexing only supported on Ref/string types for now.");
	}

	@Override
	public LLVMValueRef visitArrayLiteralExpression(ArrayLiteralExpression node)
	{
		// TODO: Emit array constant or alloca + element stores
		return null;
	}

	@Override
	public LLVMValueRef visitTupleLiteralExpression(TupleLiteralExpression node)
	{
		// TODO: Emit tuple as unnamed struct
		return null;
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
		// TODO: Emit sprintf-like string building
		return null;
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
		// TODO: Emit switch/jump-table for match
		return null;
	}

	@Override
	public LLVMValueRef visitMatchArm(MatchArm node)
	{
		// TODO: Emit match arm block
		return null;
	}

	@Override
	public LLVMValueRef visitLiteralPattern(LiteralPattern node)
	{
		// TODO: Emit pattern comparison
		return null;
	}

	@Override
	public LLVMValueRef visitTypePattern(TypePattern node)
	{
		// TODO: Emit runtime type check
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
		// TODO: Emit disjunctive pattern check
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

}
