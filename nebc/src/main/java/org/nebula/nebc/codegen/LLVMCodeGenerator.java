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
import org.nebula.nebc.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
	 * True when the current function is Nebula {@code void main()} promoted to LLVM {@code i32 main()}.
	 * Used by return-statement emission to produce {@code ret i32 0} instead of {@code ret void}.
	 */
	private boolean currentFunctionIsVoidMain;
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

	// ── Erased-mode state (used during emitErasedFunctionBitcode) ──────────────
	/**
	 * When {@code true} the visitor is emitting a type-erased generic function.
	 * TypeParameterType values become opaque {@code ptr} in LLVM, and calls to
	 * trait methods on type-parameter-typed receivers are dispatched via vtable.
	 */
	private boolean isErasedMode = false;
	/**
	 * Maps each type-parameter name (e.g. {@code "T"}) to the corresponding
	 * vtable {@link LLVMValueRef} parameter of the erased function being emitted.
	 */
	private final Map<String, LLVMValueRef> erasedVtableParams = new HashMap<>();
	/**
	 * Tracks erased-function names that have already been linked into {@code module}
	 * so we never link the same bitcode module twice.
	 */
	private final Set<String> linkedErasedFunctions = new HashSet<>();

	/**
	 * Tracks heap-allocated class instances and {@code drops} parameters in the
	 * current function, enabling deterministic deallocation via LUA (Last-Usage
	 * Analysis). Created fresh for each function body and emits conditional
	 * {@code neb_free} calls before every {@code ret}.
	 */
	private RegionTracker regionTracker;

	public LLVMCodeGenerator()
	{
	}

	// =================================================================
	// ALLOCA HOISTING
	// =================================================================

	/**
	 * Emits an {@code alloca} instruction unconditionally into the <em>entry basic
	 * block</em> of the current function, regardless of where the builder is
	 * currently positioned.
	 *
	 * <h3>Why this matters</h3>
	 * An {@code alloca} that lives in a non-entry basic block is a
	 * <em>dynamic alloca</em>: the stack pointer is decremented every time that
	 * block is executed. Inside a loop with N iterations this allocates N × size
	 * bytes on the stack, causing stack overflow / memory corruption for large N.
	 * LLVM's {@code mem2reg} pass also requires that promotable allocas reside in
	 * the entry block.
	 *
	 * <h3>Mechanism</h3>
	 * <ol>
	 * <li>Save the current insertion block.</li>
	 * <li>If the entry block already has a terminator (we are past the prologue),
	 *     position the builder <em>before</em> that terminator so the alloca is
	 *     appended to the alloca-region of the entry block.  Otherwise the builder
	 *     is still building the entry block — just append normally.</li>
	 * <li>Emit the {@code alloca}.</li>
	 * <li>Restore the builder to the saved insertion block.</li>
	 * </ol>
	 *
	 * @param type the LLVM type to allocate space for
	 * @param name debug name for the alloca instruction
	 * @return the {@link LLVMValueRef} of the emitted {@code alloca}
	 */
	private LLVMValueRef emitEntryAlloca(LLVMTypeRef type, String name)
	{
		LLVMBasicBlockRef currentBB = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef entryBB   = LLVMGetEntryBasicBlock(currentFunction);

		LLVMValueRef terminator = LLVMGetBasicBlockTerminator(entryBB);
		if (terminator != null && !terminator.isNull())
		{
			// Entry block already has a br / ret — insert the alloca right before it.
			LLVMPositionBuilderBefore(builder, terminator);
		}
		else
		{
			// Still building the entry block; append at its current end.
			LLVMPositionBuilderAtEnd(builder, entryBB);
		}

		LLVMValueRef alloca = LLVMBuildAlloca(builder, type, name);

		// Restore the original insertion point so the caller's stores / loads
		// are emitted at the correct position (e.g. inside the loop body).
		LLVMPositionBuilderAtEnd(builder, currentBB);
		return alloca;
	}

	// =================================================================
	// PUBLIC API
	// =================================================================

	private LLVMValueRef emitCast(LLVMValueRef value, Type srcSemType, Type targetSemType)
	{
		if (value == null || srcSemType == null || targetSemType == null)
			return value;

		// Struct value → ptr coercion: when a struct value (returned by-value from
		// a function/operator) is passed to a function that expects a pointer (the
		// default ABI for composite parameters), spill to a temporary alloca.
		// This must be checked BEFORE the short-circuit equality check because
		// the semantic types may be equal (both IVec2) but the LLVM types differ
		// (struct %IVec2 vs ptr).
		if (srcSemType instanceof StructType srcSt && targetSemType instanceof CompositeType)
		{
			if (LLVMGetTypeKind(LLVMTypeOf(value)) != LLVMPointerTypeKind)
			{
				LLVMTypeRef structTy = LLVMTypeMapper.getOrCreateStructType(context, srcSt);
				LLVMValueRef tmp = emitEntryAlloca(structTy, "struct_arg_tmp");
				LLVMBuildStore(builder, value, tmp);
				return tmp;
			}
			return value;
		}

		if (srcSemType.equals(targetSemType))
			return value;

		// str → cstr: extract the data pointer (field 0) from the { ptr, i64 } fat struct.
		// This is the automatic coercion used when passing a Nebula str to an
		// extern "C" function whose parameter is declared as 'cstr' (raw char*).
		if (srcSemType == PrimitiveType.STR && targetSemType == PrimitiveType.CSTR)
		{
			return LLVMBuildExtractValue(builder, value, 0, "str_cptr");
		}
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
			// Bool → integer: zero-extend i1 to target width
			if (src == PrimitiveType.BOOL && target.isInteger())
			{
				return LLVMBuildZExt(builder, value, targetType, "bool_to_int");
			}
			// Integer → bool: truncate to i1
			if (src.isInteger() && target == PrimitiveType.BOOL)
			{
				return LLVMBuildTrunc(builder, value, targetType, "int_to_bool");
			}

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
		return generate(units, analyzer, /* isLibraryBuild= */ false);
	}

	/**
	 * Generates LLVM IR for all compilation units in {@code units}.
	 *
	 * @param isLibraryBuild When {@code true} the attribute registry is <em>not</em>
	 *                       emitted, because libraries must not provide definitions
	 *                       for the {@code __nebula_rt_attr_*} accessor functions —
	 *                       those are always provided by the final executable.
	 */
	public LLVMModuleRef generate(List<CompilationUnit> units, SemanticAnalyzer analyzer,
	                              boolean isLibraryBuild)
	{
		this.analyzer = analyzer;
		LLVMTypeMapper.clearCache();

		// 1. Initialise LLVM infrastructure
		context = LLVMContextCreate();
		module = LLVMModuleCreateWithNameInContext("nebula_module", context);
		builder = LLVMCreateBuilderInContext(context);

		// 2a. Pre-pass: emit compiler-generated deep drop functions for all classes.
		//     This must happen BEFORE user function bodies so that RegionTracker
		//     can always find "TypeName__drop" in the module during cleanup emission.
		emitDropFunctionsPrePass(units);

		// 2b. Visit every compilation unit (methods, constructors, etc.)
		for (CompilationUnit cu : units)
		{
			cu.accept(this);
		}

		// 3. Emit the compile-time attribute registry unless this is a library build.
		//    (Library builds must not define __nebula_rt_attr_* — the executable does.)
		if (!isLibraryBuild)
		{
			new AttributeRegistryEmitter(context, module, builder).emit(analyzer);
		}

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
		// Pre-pass: register all enum discriminants before emitting any function body,
		// so that unqualified variant references (e.g. Mushroom, Super) resolve correctly
		// even when the enum declaration appears after its first use in source order.
		for (ASTNode decl : node.declarations)
		{
			if (decl instanceof EnumDeclaration ed)
			{
				for (int i = 0; i < ed.variants.size(); i++)
				{
					enumDiscriminants.put(ed.name + "." + ed.variants.get(i), i);
				}
			}
		}

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

		// 4. Add the function to the module (or retrieve if already declared)
		String funcName = (currentSubstitution != null) ? getSpecializationName(symbol) : symbol.getMangledName();

		// When emitting the entry-point `main`, always use the C ABI signature
		// (i32 argc, ptr argv) -> i32, regardless of whether the Nebula source
		// declared parameters or not.  This keeps binaries compatible with standard
		// OS loaders and lets start.c pass argc/argv without any tricks.
		boolean isMain = "main".equals(funcName);
		boolean isVoidMain = isMain && returnType == PrimitiveType.VOID;
		boolean mainHasArgs = isMain && !node.parameters.isEmpty();
		LLVMTypeRef llvmFuncType;
		if (isMain)
		{
			LLVMTypeRef i32t = LLVMInt32TypeInContext(context);
			LLVMTypeRef ptrt = LLVMPointerTypeInContext(context, 0);
			LLVMTypeRef[] mainParams = { i32t, ptrt };
			llvmFuncType = LLVMFunctionType(i32t, new PointerPointer<>(mainParams), 2, 0);
		}
		else
		{
			// All functions (including extern "C") use the Nebula ABI for str.
			// Functions that need raw char* should declare their str params as 'cstr'.
			llvmFuncType = toLLVMType(funcType);
		}

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
		boolean prevVoidMain = currentFunctionIsVoidMain;
		currentFunction = function;
		currentBlockTerminated = false;
		currentFunctionIsVoidMain = isVoidMain;
		Type prevReturnType = currentMethodReturnType;
		currentMethodReturnType = returnType;
		RegionTracker prevRegionTracker = regionTracker;
		regionTracker = new RegionTracker(context, builder, module);

		Map<String, LLVMValueRef> prevNamedValues = new HashMap<>(namedValues);
		Set<String> prevInlineStructVars = new HashSet<>(inlineStructVars);
		Map<String, Integer> prevArrayElementCounts = new HashMap<>(arrayElementCounts);
		namedValues.clear();
		inlineStructVars.clear();
		arrayElementCounts.clear();

		// 6.5. Allocate and bind parameters to namedValues
		if (isMain)
		{
			// main's LLVM signature is always (i32 argc, ptr argv).
			// When the Nebula source declares main(str[] args), build a Nebula str[]
			// from argc/argv via the runtime helper and bind it to the parameter name.
			// Also store argc as an i64 alloca under "__<name>_len" so that the
			// iterator support (.len, foreach) can load a runtime-known count.
			// When the source declares main() with no parameters, argc/argv are ignored.
			LLVMValueRef argc = LLVMGetParam(function, 0);
			LLVMValueRef argv = LLVMGetParam(function, 1);
			if (mainHasArgs)
			{
				LLVMValueRef argsPtr = emitBuildArgv(argc, argv);
				Parameter argsParam = node.parameters.get(0);
				// Data-pointer alloca
				LLVMTypeRef ptrType = LLVMPointerTypeInContext(context, 0);
				LLVMValueRef dataAlloca = LLVMBuildAlloca(builder, ptrType, argsParam.name());
				LLVMBuildStore(builder, argsPtr, dataAlloca);
				namedValues.put(argsParam.name(), dataAlloca);
				// Companion length alloca: argc sign-extended to i64
				LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
				LLVMValueRef lenVal = LLVMBuildSExt(builder, argc, i64t, "argc_i64");
				LLVMValueRef lenAlloca = LLVMBuildAlloca(builder, i64t, "__" + argsParam.name() + "_len");
				LLVMBuildStore(builder, lenVal, lenAlloca);
				namedValues.put("__" + argsParam.name() + "_len", lenAlloca);
			}
		}
		else
		{
			int llvmParamIdx = 0;
			// If this is a member method (represented by having 'this' in the FunctionType),
			// bind the first LLVM parameter to "this".
			boolean hasSyntheticThis = funcType.parameterTypes.size() > node.parameters.size();
			if (hasSyntheticThis)
			{
				LLVMValueRef thisValue = LLVMGetParam(function, llvmParamIdx++);
				Type thisType = funcType.parameterTypes.get(0);
				LLVMValueRef alloca = LLVMBuildAlloca(builder, toLLVMType(thisType), "this");
				LLVMBuildStore(builder, thisValue, alloca);
				namedValues.put("this", alloca);
			}

			// Nebula-level type index: starts after 'this' (if present).
			int nebulaParamIdx = hasSyntheticThis ? 1 : 0;

			for (int i = 0; i < node.parameters.size(); i++)
			{
				Parameter param = node.parameters.get(i);
				LLVMValueRef paramValue = LLVMGetParam(function, llvmParamIdx++);

				// Allocate space for the parameter using the Nebula-level type index.
				if (nebulaParamIdx >= funcType.parameterTypes.size())
				{
					throw new CodegenException("Parameter index out of bounds for function: " + node.name);
				}
				Type paramType = funcType.parameterTypes.get(nebulaParamIdx++);
				LLVMValueRef alloca = LLVMBuildAlloca(builder, toLLVMType(paramType), param.name());
				LLVMBuildStore(builder, paramValue, alloca);
				namedValues.put(param.name(), alloca);

				// Array parameters are expanded to (ptr, i64) in the LLVM ABI.
				// Bind the companion i64 length to a "__<name>_len" alloca so that
				// member access ".len" and foreach can load it at runtime.
				if (paramType instanceof ArrayType)
				{
					LLVMValueRef lenValue = LLVMGetParam(function, llvmParamIdx++);
					LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
					LLVMValueRef lenAlloca = LLVMBuildAlloca(builder, i64t, "__" + param.name() + "_len");
					LLVMBuildStore(builder, lenValue, lenAlloca);
					namedValues.put("__" + param.name() + "_len", lenAlloca);
				}

				// CVT: If this parameter has 'drops', we own the region and must free it.
				if (param.cvtModifier() == CVTModifier.DROPS && paramType instanceof ClassType ct)
				{
					regionTracker.registerRegion(param.name(), alloca, ct.name());
				}
			}
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
			// CVT/LUA: Emit deterministic cleanup for all tracked regions before return.
			regionTracker.emitCleanup(currentFunction);

			if (returnType == PrimitiveType.VOID)
			{
				if (isVoidMain)
				{
					LLVMBuildRet(builder, LLVMConstInt(LLVMInt32TypeInContext(context), 0, 0));
				}
				else
				{
					LLVMBuildRetVoid(builder);
				}
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
		currentFunctionIsVoidMain = prevVoidMain;
		currentMethodReturnType = prevReturnType;
		regionTracker = prevRegionTracker;
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

	/**
	 * Emits a call to the runtime helper {@code __nebula_build_argv(i32 argc, ptr argv)}
	 * which converts the C-level argc/argv into a heap-allocated array of Nebula
	 * {@code str} structs ({@code { ptr data, i64 len }}).
	 * Used by the compiler-generated main prologue when the Nebula entry point
	 * is declared as {@code main(str[] args)}.
	 *
	 * @param argc the LLVM {@code i32} parameter value from the LLVM-level main
	 * @param argv the LLVM {@code ptr} (char**) parameter value from the LLVM-level main
	 * @return an LLVM {@code ptr} pointing to the first element of the built str[]
	 */
	private LLVMValueRef emitBuildArgv(LLVMValueRef argc, LLVMValueRef argv)
	{
		LLVMTypeRef i32t = LLVMInt32TypeInContext(context);
		LLVMTypeRef ptrt = LLVMPointerTypeInContext(context, 0);
		LLVMTypeRef[] paramTypes = { i32t, ptrt };
		LLVMTypeRef fnType = LLVMFunctionType(ptrt, new PointerPointer<>(paramTypes), 2, 0);

		String fnName = "__nebula_build_argv";
		LLVMValueRef fn = LLVMGetNamedFunction(module, fnName);
		if (fn == null || fn.isNull())
		{
			fn = LLVMAddFunction(module, fnName, fnType);
			LLVMSetLinkage(fn, LLVMExternalLinkage);
		}

		LLVMValueRef[] args = { argc, argv };
		return LLVMBuildCall2(builder, fnType, fn, new PointerPointer<>(args), 2, "args_ptr");
	}

	// =================================================================
	// CVT: DEEP-DROP PRE-PASS
	// =================================================================

	/**
	 * Two-phase pre-pass that generates compiler-owned deep drop functions for
	 * every non-generic class declaration in {@code units}.
	 * <p>
	 * <b>Phase 1</b> — forward-declare {@code void TypeName__drop(ptr)} for all
	 * classes, so cross-references between drop functions always resolve.
	 * <b>Phase 2</b> — fill in each function body (GEP + recursive field drops +
	 * self {@code neb_free}).
	 */
	private void emitDropFunctionsPrePass(List<CompilationUnit> units)
	{
		// Collect all non-generic class declarations
		List<ClassDeclaration> classes = new ArrayList<>();
		for (CompilationUnit cu : units)
		{
			collectClassDeclarations(cu, classes);
		}

		// Phase 1: forward-declare void ClassName__drop(ptr)
		LLVMTypeRef ptrT  = LLVMPointerTypeInContext(context, 0);
		LLVMTypeRef voidT = LLVMVoidTypeInContext(context);
		LLVMTypeRef dropFnType = LLVMFunctionType(voidT,
			new PointerPointer<>(new LLVMTypeRef[]{ ptrT }), 1, 0);

		for (ClassDeclaration cd : classes)
		{
			String dropName = cd.name + "__drop";
			LLVMValueRef existing = LLVMGetNamedFunction(module, dropName);
			if (existing == null || existing.isNull())
			{
				LLVMAddFunction(module, dropName, dropFnType);
			}
		}

		// Phase 2: fill in function bodies
		for (ClassDeclaration cd : classes)
		{
			emitDropFunction(cd);
		}
	}

	/** Recursively collect all non-generic {@link ClassDeclaration} nodes. */
	private void collectClassDeclarations(ASTNode node, List<ClassDeclaration> out)
	{
		if (node instanceof ClassDeclaration cd)
		{
			if (cd.typeParams == null || cd.typeParams.isEmpty())
			{
				out.add(cd);
			}
		}
		else if (node instanceof CompilationUnit cu)
		{
			for (ASTNode decl : cu.declarations)
			{
				collectClassDeclarations(decl, out);
			}
		}
		else if (node instanceof NamespaceDeclaration nd)
		{
			for (ASTNode member : nd.members)
			{
				collectClassDeclarations(member, out);
			}
		}
	}

	/**
	 * Generate the body of {@code void ClassName__drop(ptr %this)}.
	 * <p>
	 * The generated function:
	 * <ol>
	 *   <li>For each non-{@code backlink} field of {@link ClassType}: loads the
	 *       field pointer and calls {@code FieldType__drop}.</li>
	 *   <li>For each non-{@code backlink} field of {@code OptionalType<ClassType>}:
	 *       conditionally loads and drops the inner pointer if present.</li>
	 *   <li>For each {@code str} field: extracts the data pointer and calls
	 *       {@code neb_free}.</li>
	 *   <li>Calls {@code neb_free(ptr %this)} to release the object itself.</li>
	 * </ol>
	 */
	private void emitDropFunction(ClassDeclaration node)
	{
		// Look up the resolved ClassType — recorded by SemanticAnalyzer.visitClassDeclaration.
		TypeSymbol typeSym = analyzer.getSymbol(node, TypeSymbol.class);
		if (typeSym == null || !(typeSym.getType() instanceof ClassType ct))
		{
			return;
		}

		String dropName = node.name + "__drop";
		LLVMValueRef dropFn = LLVMGetNamedFunction(module, dropName);
		if (dropFn == null || dropFn.isNull())
		{
			return; // should have been forward-declared in phase 1
		}

		// Save insertion point (we might be generating inside another context)
		LLVMBasicBlockRef prevBlock = LLVMGetInsertBlock(builder);

		LLVMBasicBlockRef entryBB = LLVMAppendBasicBlockInContext(context, dropFn, "entry");
		LLVMPositionBuilderAtEnd(builder, entryBB);

		LLVMValueRef thisPtr = LLVMGetParam(dropFn, 0);
		LLVMTypeRef structLlvmType = LLVMTypeMapper.getOrCreateStructType(context, ct);

		// Walk fields in declaration order (same order as LLVM struct body).
		// Use VariableSymbol.isBacklink() — the authoritative, symbol-table-side flag.
		int fieldIdx = 0;
		for (Symbol s : ct.getMemberScope().getSymbols().values())
		{
			if (!(s instanceof VariableSymbol vs) || vs.getName().equals("this"))
			{
				continue;
			}

			if (vs.isBacklink())
			{
				// Non-owning back-reference — must not be freed by this destructor.
				fieldIdx++;
				continue;
			}

			Type fieldType = vs.getType();
			String fieldName = vs.getName();

			// GEP to the field inside *this
			LLVMValueRef fieldPtr = LLVMBuildStructGEP2(builder, structLlvmType,
				thisPtr, fieldIdx, fieldName + "_gep");

			if (fieldType instanceof ClassType fieldCt)
			{
				// Non-optional owned class field — always present, drop unconditionally
				LLVMValueRef fieldVal = LLVMBuildLoad2(builder,
					LLVMPointerTypeInContext(context, 0),
					fieldPtr, fieldName + "_load");
				emitDropCall(fieldVal, fieldCt.name());
			}
			else if (fieldType instanceof OptionalType ot
				&& ot.innerType instanceof ClassType fieldCt)
			{
				// Optional owned class field — check presence flag first
				LLVMTypeRef optLlvmType = LLVMTypeMapper.getOrCreateOptionalStructType(
					context, ot);
				LLVMValueRef optVal = LLVMBuildLoad2(builder, optLlvmType,
					fieldPtr, fieldName + "_opt");
				LLVMValueRef hasValue = LLVMBuildExtractValue(builder, optVal, 0,
					fieldName + "_has");
				LLVMValueRef innerPtr = LLVMBuildExtractValue(builder, optVal, 1,
					fieldName + "_inner");

				LLVMBasicBlockRef dropBB = LLVMAppendBasicBlockInContext(context,
					dropFn, "drop_" + fieldName);
				LLVMBasicBlockRef skipBB = LLVMAppendBasicBlockInContext(context,
					dropFn, "skip_" + fieldName);
				LLVMBuildCondBr(builder, hasValue, dropBB, skipBB);

				LLVMPositionBuilderAtEnd(builder, dropBB);
				emitDropCall(innerPtr, fieldCt.name());
				LLVMBuildBr(builder, skipBB);

				LLVMPositionBuilderAtEnd(builder, skipBB);
			}
			// Primitives, structs, and str fields require no additional action here

			fieldIdx++;
		}

		// Free the object itself
		emitNebFreeCall(thisPtr);
		LLVMBuildRetVoid(builder);

		// Restore previous insertion point
		if (prevBlock != null && !prevBlock.isNull())
		{
			LLVMPositionBuilderAtEnd(builder, prevBlock);
		}
	}

	/**
	/**
	 * Emit a drop call for a heap pointer: calls {@code TypeName__drop(ptr)} if
	 * the function exists in the module, otherwise falls back to {@code neb_free}.
	 */
	private void emitDropCall(LLVMValueRef ptr, String typeName)
	{
		LLVMValueRef dropFn = LLVMGetNamedFunction(module, typeName + "__drop");
		if (dropFn != null && !dropFn.isNull())
		{
			LLVMTypeRef ptrT  = LLVMPointerTypeInContext(context, 0);
			LLVMTypeRef voidT = LLVMVoidTypeInContext(context);
			LLVMTypeRef fnType = LLVMFunctionType(voidT,
				new PointerPointer<>(new LLVMTypeRef[]{ ptrT }), 1, 0);
			LLVMValueRef[] args = { ptr };
			LLVMBuildCall2(builder, fnType, dropFn, new PointerPointer<>(args), 1, "");
		}
		else
		{
			emitNebFreeCall(ptr);
		}
	}

	/** Emit a plain {@code call void @neb_free(ptr %ptr)}. */
	private void emitNebFreeCall(LLVMValueRef ptr)
	{
		LLVMValueRef nebFree = LLVMGetNamedFunction(module, "neb_free");
		if (nebFree == null || nebFree.isNull())
		{
			LLVMTypeRef ptrT  = LLVMPointerTypeInContext(context, 0);
			LLVMTypeRef voidT = LLVMVoidTypeInContext(context);
			LLVMTypeRef ft = LLVMFunctionType(voidT,
				new PointerPointer<>(new LLVMTypeRef[]{ ptrT }), 1, 0);
			nebFree = LLVMAddFunction(module, "neb_free", ft);
		}
		LLVMTypeRef ptrT  = LLVMPointerTypeInContext(context, 0);
		LLVMTypeRef voidT = LLVMVoidTypeInContext(context);
		LLVMTypeRef ft = LLVMFunctionType(voidT,
			new PointerPointer<>(new LLVMTypeRef[]{ ptrT }), 1, 0);
		LLVMValueRef[] args = { ptr };
		LLVMBuildCall2(builder, ft, nebFree, new PointerPointer<>(args), 1, "");
	}

	/**
	 * Returns {@code true} if the named field of {@code ct} is declared with
	 * the {@code backlink} modifier (i.e. it is a non-owning weak reference).
	 */
	/**
	 * Returns {@code true} if the named member of {@code ct} carries the
	 * {@code backlink} modifier — i.e. it is a non-owning weak reference that
	 * must not trigger an ownership transfer when assigned.
	 */
	private boolean isBacklinkField(CompositeType ct, String memberName)
	{
		Symbol sym = ct.getMemberScope().resolveLocal(memberName);
		return sym instanceof VariableSymbol vs && vs.isBacklink();
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
				LLVMValueRef alloca = emitEntryAlloca(structLlvmType, varName);
				if (decl.hasInitializer())
				{
					LLVMValueRef initVal = decl.initializer().accept(this);
					if (initVal != null)
					{
						// If the initializer produced a pointer (e.g. a class-returning
						// function or an identifier that resolves to a ptr), load the
						// struct value through it.  Struct-returning functions and
						// constructors already produce a struct value directly.
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
				LLVMValueRef alloca = emitEntryAlloca(toLLVMType(type), varName);
				if (decl.hasInitializer())
				{
					LLVMValueRef initVal = decl.initializer().accept(this);
					if (initVal != null)
					{
						LLVMValueRef castedVal = emitCast(initVal, analyzer.getType(decl.initializer()), type);
						LLVMBuildStore(builder, castedVal, alloca);
					}

					// CVT/LUA: Track heap allocations for deterministic deallocation.
					// A variable initialised from a 'new' expression owns the region.
					if (type instanceof ClassType ct && decl.initializer() instanceof NewExpression
						&& regionTracker != null)
					{
						regionTracker.registerRegion(varName, alloca, ct.name());
					}
					// CVT/LUA: Track dynamic strings as Aggregate Proxies (§7.1).
					// String interpolation heap-allocates a buffer via neb_alloc,
					// so we track the %str struct and extract field 0 (ptr) to free.
					else if (type == PrimitiveType.STR
						&& decl.initializer() instanceof StringInterpolationExpression
						&& regionTracker != null)
					{
						regionTracker.registerRegion(varName, alloca, RegionTracker.RegionKind.STRING_PROXY);
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

	/**
	 * Coerces an LLVM constant value to the target LLVM type.
	 * <p>
	 * Integer literal initializers for global constants may be inferred with a
	 * wider type than the declared variable (e.g. {@code 0xFFFFFFFF} is
	 * semantically {@code i64} because it exceeds {@link Integer#MAX_VALUE}, but
	 * the declared type may be {@code u32} which maps to LLVM {@code i32}).
	 * The raw bit-pattern is extracted via {@link LLVM#LLVMConstIntGetZExtValue}
	 * and a fresh {@link LLVM#LLVMConstInt} is constructed with the target type,
	 * which truncates from above or zero-extends from below as needed.
	 *
	 * @param value      The constant LLVM value to coerce (may be {@code null}).
	 * @param targetType The required LLVM type.
	 * @return The coerced constant, or the original value if no cast is needed.
	 */
	private LLVMValueRef coerceConstantToType(LLVMValueRef value, LLVMTypeRef targetType)
	{
		if (value == null)
			return null;

		LLVMTypeRef srcType = LLVMTypeOf(value);
		if (LLVMGetTypeKind(srcType) == LLVMIntegerTypeKind
			&& LLVMGetTypeKind(targetType) == LLVMIntegerTypeKind
			&& LLVMGetIntTypeWidth(srcType) != LLVMGetIntTypeWidth(targetType))
		{
			// Extract the unsigned bit-pattern of the constant and re-create it
			// with the target integer type.  LLVMConstInt uses the lower N bits
			// of the supplied long, so this naturally truncates or zero-extends.
			long rawBits = LLVMConstIntGetZExtValue(value);
			return LLVMConstInt(targetType, rawBits, 0);
		}
		return value;
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
				Log.debug("Emitting global const " + varName + " of type " + type.name());
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

					// Coerce the constant initializer to the declared global type.
					// Integer literals whose value exceeds Integer.MAX_VALUE are inferred
					// as i64 by the semantic analyser (e.g. 0xFFFFFFFF → i64), but the
					// declared type may be u32 / i32, so we must truncate the constant.
					initVal = coerceConstantToType(initVal, llvmType);
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
		// Apply the active substitution so that type parameters inherited from the
		// enclosing generic struct (e.g. T → i32) are resolved before we map to LLVM
		// types.  The constructor's own MethodSymbol may still carry the raw
		// TypeParameterType when retrieved from the generic scope.
		if (currentSubstitution != null)
		{
			funcType = (FunctionType) currentSubstitution.substitute(funcType);
		}
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
		RegionTracker prevRegionTracker = regionTracker;
		regionTracker = new RegionTracker(context, builder, module);

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

			// CVT: If this parameter has 'drops', we own the region and must free it.
			if (param.cvtModifier() == CVTModifier.DROPS && paramType instanceof ClassType ct)
			{
				regionTracker.registerRegion(param.name(), alloca, ct.name());
			}
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
			// CVT/LUA: Emit deterministic cleanup for all tracked regions before return.
			regionTracker.emitCleanup(currentFunction);
			LLVMBuildRetVoid(builder); // constructors always return void
		}

		currentFunction = prevFunction;
		currentBlockTerminated = prevTerminated;
		currentMethodReturnType = prevReturnType;
		regionTracker = prevRegionTracker;
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

		// CVT/LUA: Emit deterministic cleanup for all tracked regions before return.
		if (regionTracker != null)
		{
			regionTracker.emitCleanup(currentFunction);
		}

		if (node.value == null)
		{
			if (currentFunctionIsVoidMain)
			{
				LLVMBuildRet(builder, LLVMConstInt(LLVMInt32TypeInContext(context), 0, 0));
			}
			else
			{
				LLVMBuildRetVoid(builder);
			}
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

		// Structs are value types — returned by value (the LLVM struct type itself).
		// The caller's function signature expects a struct aggregate, not a pointer.
		if (retType instanceof StructType && srcType instanceof CompositeType)
		{
			// The value is already a loaded struct aggregate — return it directly.
			return value;
		}

		// Tagged unions are also value types { i32 tag, [N x i8] payload } — return by value.
		if (retType instanceof UnionType && srcType instanceof CompositeType)
		{
			return value;
		}

		// Classes are reference types — return a pointer.
		if (retType instanceof CompositeType retCt && srcType instanceof CompositeType)
		{
			LLVMTypeRef structType = LLVMTypeMapper.getOrCreateStructType(context, retCt);
			LLVMValueRef alloca = emitEntryAlloca(structType, "ret_struct");
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
		LLVMValueRef optAlloca = emitEntryAlloca(optStructType, "none");

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
		LLVMValueRef optAlloca = emitEntryAlloca(optStructType, "opt_wrap");

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
		else if (iterableType instanceof ArrayType at
				&& node.iterable instanceof IdentifierExpression ie
				&& namedValues.containsKey("__" + ie.name + "_len"))
		{
			// Runtime-length array: length stored in companion alloca __<name>_len.
			// iterableVal is already a pointer to the first element.
			elemLLVMType = toLLVMType(at.baseType);
			LLVMValueRef lenAlloca = namedValues.get("__" + ie.name + "_len");
			lengthVal = LLVMBuildLoad2(builder, LLVMInt64TypeInContext(context), lenAlloca,
					ie.name + "_foreach_len");
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

		// index alloca — hoisted to entry block to avoid dynamic stack growth
		LLVMValueRef idxAlloca = emitEntryAlloca(i64t, "foreach_idx");
		LLVMBuildStore(builder, LLVMConstInt(i64t, 0, 0), idxAlloca);

		// element binding alloca — hoisted to entry block
		LLVMValueRef elemAlloca = emitEntryAlloca(elemLLVMType, node.variableName);

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
				// str → { ptr, i64 }
				String text = node.value.toString();
				LLVMValueRef globalPtr = emitGlobalStringPtr(text, "str");
				int utf8Len = text.getBytes(StandardCharsets.UTF_8).length;
				LLVMValueRef length = LLVMConstInt(LLVMInt64TypeInContext(context), utf8Len, 0);

				// Build a named-struct constant for NebulaStr — works both at
				// global-constant time (before any function body) and inside functions.
				LLVMTypeRef strNamedT = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);
				LLVMValueRef[] fields = { globalPtr, length };
				yield LLVMConstNamedStruct(strNamedT, new PointerPointer<>(fields), 2);
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

	/**
	 * Emits a call to {@code __nebula_rt_str_eq(str a, str b) -> i32} and
	 * converts the {@code i32} result to an {@code i1} boolean.
	 * Both {@code str} arguments are passed by value as {@code { ptr, i64 }}
	 * structs; the System V ABI splits each into two integer registers.
	 *
	 * @param lVal the LLVM {@code %str} value of the left-hand side
	 * @param rVal the LLVM {@code %str} value of the right-hand side
	 * @return an LLVM {@code i1} that is 1 when the strings are equal
	 */
	private LLVMValueRef emitStrEq(LLVMValueRef lVal, LLVMValueRef rVal)
	{
		LLVMTypeRef strT   = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);
		LLVMTypeRef i32t   = LLVMInt32TypeInContext(context);
		LLVMTypeRef fnType = LLVMFunctionType(i32t,
			new PointerPointer<>(new LLVMTypeRef[]{ strT, strT }), 2, 0);
		LLVMValueRef fn    = getOrDeclareIntrinsic("__nebula_rt_str_eq", fnType);

		LLVMValueRef[] args   = { lVal, rVal };
		LLVMValueRef   result = LLVMBuildCall2(builder, fnType, fn,
			new PointerPointer<>(args), 2, "str_eq_i32");

		// Convert i32 (1/0) → i1
		return LLVMBuildICmp(builder, LLVMIntNE,
			result, LLVMConstInt(i32t, 0, 0), "str_eq");
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

		// str == str / str != str: delegate to the runtime helper
		if (leftType == PrimitiveType.STR && rightType == PrimitiveType.STR
			&& (node.operator == BinaryOperator.EQ || node.operator == BinaryOperator.NE))
		{
			LLVMValueRef lVal = node.left.accept(this);
			LLVMValueRef rVal = node.right.accept(this);
			if (lVal == null || rVal == null)
				return null;
			LLVMValueRef eq = emitStrEq(lVal, rVal);
			return node.operator == BinaryOperator.NE
				? LLVMBuildNot(builder, eq, "str_ne")
				: eq;
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
		LLVMValueRef optAlloca = emitEntryAlloca(optStructType, "opt_cmp_tmp");
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
			selfPtr = emitEntryAlloca(structTy, "op_lhs_tmp");
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

			// CVT/LUA: Storing a class reference into a field captures it — the parent
			// object now owns the region. Mark the source variable as transferred,
			// unless the target field is a backlink (weak reference — not truly captured).
			if (regionTracker != null && node.operator.equals("=")
				&& (targetSemType instanceof ClassType
					|| (targetSemType instanceof OptionalType ot2 && ot2.innerType instanceof ClassType))
				&& node.value instanceof IdentifierExpression srcIdent
				&& regionTracker.isTracked(srcIdent.name))
			{
				Type baseType2 = analyzer.getType(mae.target);
				boolean isBacklink = baseType2 instanceof CompositeType compT2
					&& isBacklinkField(compT2, mae.memberName);
				if (!isBacklink)
				{
					regionTracker.markTransferred(srcIdent.name);
				}
			}

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
			// For qualified names like "std::fn::FnRef", strip the namespace prefix —
			// the constructor is stored in the member scope under the simple type name.
			String simpleName = node.name.contains("::")
					? node.name.substring(node.name.lastIndexOf("::") + 2)
					: node.name;
			Symbol ctorSym = ct.getMemberScope().resolveLocal(simpleName);
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

		// Try to resolve as an unqualified enum variant (e.g. `Super` instead of `Form.Super`).
		// The SA records the symbol as a VariableSymbol whose type is the EnumType.
		if (sym instanceof VariableSymbol vs && vs.getType() instanceof EnumType et)
		{
			String key = et.name() + "." + node.name;
			Integer disc = enumDiscriminants.get(key);
			if (disc != null)
				return LLVMConstInt(LLVMInt32TypeInContext(context), disc, 0);
		}

		throw new CodegenException("Undeclared identifier referenced in codegen: " + node.name);
	}

	@Override
	public LLVMValueRef visitInvocationExpression(InvocationExpression node)
	{
		LLVMValueRef function = null;

		// ── Erased-mode: intercept trait-method calls on TypeParameterType receivers ──
		// When emitting a type-erased generic function body, any call of the form
		//   item.traitMethod(...)  where item : T (TypeParameterType)
		// must be emitted as an indirect call through the vtable parameter for T.
		if (isErasedMode && node.target instanceof MemberAccessExpression mae)
		{
			Type receiverOriginalType = analyzer.getType(mae.target);
			if (receiverOriginalType instanceof TypeParameterType tpt && tpt.hasBound())
			{
				return emitErasedVtableCall(node, mae, tpt);
			}
		}

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
						// The declaration is unavailable (imported from a .nebsym library).
						// If this method carries pre-compiled erased bitcode, use vtable dispatch.
						if (ms.getGenericBitcode() != null && ms.getGenericBitcode().length > 0)
						{
							function = emitErasedCall(node, ms);
							currentSubstitution = prevSub;
							return function;
						}
						throw new CodegenException(
							"Cannot monomorphize generic method '" + ms.getName() +
							"': its declaration node is unavailable and no erased bitcode is present.");
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
		// The Nebula-level param count: used for 'this' detection.
		int nebulaParamCount = ft.parameterTypes.size();
		// Compute the expanded LLVM arg count: ArrayType params become (ptr, i64) pairs.
		int llvmArgCount = 0;
		for (Type t : ft.parameterTypes)
		{
			llvmArgCount++;
			if (t instanceof ArrayType) llvmArgCount++;
		}

		LLVMValueRef[] argsArr = new LLVMValueRef[llvmArgCount];
		int llvmArgIdx = 0;
		// Separate index tracking position in ft.parameterTypes (Nebula-level).
		int nebulaParamIdx = 0;

		// System.out.println("[DEBUG] Calling " + ((LLVMIsAFunction(function) != null && !LLVMIsAFunction(function).isNull()) ? LLVMGetValueName(function).getString() : "function"));
		// System.out.println("[DEBUG]   Target Nebula Type: " + ft.name());
		// System.out.println("[DEBUG]   LLVM Call Type: " + LLVMPrintTypeToString(llvmFuncType).getString());
		// System.out.println("[DEBUG]   Arg Count: " + llvmArgCount);

		// If this is a member call, prepend the receiver as 'this'
		if (node.target instanceof MemberAccessExpression mae && nebulaParamCount > nebulaArgCount)
		{
			LLVMValueRef receiver = mae.target.accept(this);
			Type receiverSemType = analyzer.getType(mae.target);
			Type thisParamType = ft.parameterTypes.get(0);

			// System.out.println("[DEBUG]   Receiver: " + receiverSemType.name() + " -> " + thisParamType.name());
			argsArr[llvmArgIdx++] = emitCast(receiver, receiverSemType, thisParamType);
			nebulaParamIdx++;
		}
		// Bare inherited member call: greet() inside Child.run() where greet() is defined in
		// Base.  The SA injects a synthetic 'this' identifier as the first effective argument.
		// Here in codegen we detect the same pattern: IdentifierExpression target whose resolved
		// FunctionType has a ClassType (not REF) as its first parameter, meaning the callee
		// expects an explicit receiver that we must supply from the current 'this' alloca.
		else if (node.target instanceof IdentifierExpression
				&& nebulaParamCount > nebulaArgCount
				&& !ft.parameterTypes.isEmpty()
				&& ft.parameterTypes.get(0) instanceof ClassType inheritedThisType)
		{
			LLVMValueRef thisAlloca = namedValues.get("this");
			if (thisAlloca == null)
			{
				throw new CodegenException("Bare inherited call to '" + node.target + "' outside method context");
			}
			// Load the class pointer from its alloca (same as visitThisExpression)
			LLVMTypeRef thisLlvmType = toLLVMType(inheritedThisType);
			LLVMValueRef thisVal = LLVMBuildLoad2(builder, thisLlvmType, thisAlloca, "this_load");
			argsArr[llvmArgIdx++] = thisVal;
			nebulaParamIdx++;
		}

		for (int i = 0; i < nebulaArgCount; i++)
		{
			Expression argNode = node.arguments.get(i);
			LLVMValueRef argValue = argNode.accept(this);

			Type paramType = ft.parameterTypes.get(nebulaParamIdx);
			Type argSemType = analyzer.getType(argNode);

			// System.out.println("[DEBUG]   Arg " + i + ": " + argSemType.name() + " -> " + paramType.name());
			argsArr[llvmArgIdx++] = emitCast(argValue, argSemType, paramType);

			// ArrayType parameters are expanded to (ptr, i64): pass the companion length.
			if (paramType instanceof ArrayType)
			{
				LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
				LLVMValueRef lenVal;
				if (argNode instanceof IdentifierExpression ie)
				{
					LLVMValueRef lenAlloca = namedValues.get("__" + ie.name + "_len");
					if (lenAlloca != null)
					{
						lenVal = LLVMBuildLoad2(builder, i64t, lenAlloca, ie.name + "_len_pass");
					}
					else
					{
						Integer staticLen = arrayElementCounts.get(ie.name);
						lenVal = LLVMConstInt(i64t, staticLen != null ? staticLen : 0, 0);
					}
				}
				else
				{
					lenVal = LLVMConstInt(i64t, 0, 0);
				}
				argsArr[llvmArgIdx++] = lenVal;
			}

			// CVT/LUA: If this argument is passed to a 'drops' parameter, mark it as
			// transferred — the callee takes ownership and is responsible for freeing.
			if (regionTracker != null && ft.parameterInfo != null)
			{
				ParameterInfo pi = ft.getParameterInfo(nebulaParamIdx);
				if (pi != null && pi.isDrops()
					&& argNode instanceof IdentifierExpression argIdent
					&& regionTracker.isTracked(argIdent.name))
				{
					regionTracker.markTransferred(argIdent.name);
				}
			}
			nebulaParamIdx++;
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
		// When the target resolves to a TypeSymbol (e.g. "Vec2" → Vec2's TypeSymbol),
		// retrieve the constructor's declared FunctionType from the composite type's
		// member scope so that parameter types (f32, i64, …) are used correctly
		// rather than the possibly-wider types inferred from the argument literals.
		if (ctorFnType == null)
		{
			String ctorKey = node.target instanceof IdentifierExpression ie ? ie.name : ct.name();
			Symbol ctorMember = ct.getMemberScope().resolveLocal(ctorKey);
			if (ctorMember instanceof MethodSymbol ctorMs)
			{
				ctorFnType = ctorMs.getType();
			}
		}
		if (ctorFnType == null)
		{
			// Last-resort fallback: infer param types from argument semantic types.
			// This may produce ABI mismatches for literal args (e.g. f64 passed to f32
			// parameter); prefer the declared constructor FunctionType above.
			List<Type> pts = new ArrayList<>();
			pts.add(PrimitiveType.REF); // 'this'
			for (Expression arg : node.arguments)
			{
				pts.add(analyzer.getType(arg));
			}
			ctorFnType = new FunctionType(PrimitiveType.VOID, pts);
		}

		LLVMTypeRef structLlvmType = LLVMTypeMapper.getOrCreateStructType(context, ct);

		// Classes are heap-allocated reference types.  Delegate to the same neb_alloc
		// path used by `visitNewExpression` so that `Child(42)` and `new Child(42)` are
		// equivalent (both return a heap-allocated `ptr`).
		final LLVMValueRef thisPtr;
		if (ct instanceof ClassType)
		{
			LLVMValueRef nebAlloc = LLVMGetNamedFunction(module, "neb_alloc");
			if (nebAlloc == null)
			{
				FunctionType allocFt = new FunctionType(PrimitiveType.REF, List.of(PrimitiveType.U64), null);
				nebAlloc = LLVMAddFunction(module, "neb_alloc", toLLVMType(allocFt));
			}
			LLVMValueRef sizeVal = LLVMSizeOf(structLlvmType);
			LLVMValueRef[] allocArgs = {sizeVal};
			FunctionType allocFt = new FunctionType(PrimitiveType.REF, List.of(PrimitiveType.U64), null);
			thisPtr = LLVMBuildCall2(builder, toLLVMType(allocFt), nebAlloc,
					new PointerPointer<>(allocArgs), 1, ct.name() + "_alloc");
		}
		else
		{
			thisPtr = LLVMBuildAlloca(builder, structLlvmType, ct.name() + "_ctor");
		}

		LLVMTypeRef ctorLlvmType = toLLVMType(ctorFnType);
		int llvmArgCount = ctorFnType.parameterTypes.size();
		LLVMValueRef[] argsArr = new LLVMValueRef[llvmArgCount];
		int argIdx = 0;

		// First param is 'this' — the alloca pointer (struct) or heap pointer (class)
		argsArr[argIdx++] = thisPtr;

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

		// CVT/LUA: Mark arguments as transferred if the constructor parameter
		// has 'drops' — the callee (constructor) takes ownership.
		if (regionTracker != null && ctorFnType.parameterInfo != null)
		{
			int ctorArgIdx = 1; // skip 'this'
			for (int i = 0; i < node.arguments.size() && ctorArgIdx < llvmArgCount; i++)
			{
				ParameterInfo pi = ctorFnType.getParameterInfo(ctorArgIdx);
				if (pi != null && pi.isDrops()
					&& node.arguments.get(i) instanceof IdentifierExpression argIdent
					&& regionTracker.isTracked(argIdent.name))
				{
					regionTracker.markTransferred(argIdent.name);
				}
				ctorArgIdx++;
			}
		}

		// Classes return a heap pointer; structs return a loaded value copy.
		if (ct instanceof ClassType)
		{
			// CVT/LUA: Track this heap allocation for deterministic deallocation.
			if (regionTracker != null && node.target instanceof IdentifierExpression)
			{
				// The caller will store into a named variable; tracking is deferred there.
			}
			return thisPtr;
		}
		// Load and return the constructed struct value
		return LLVMBuildLoad2(builder, structLlvmType, thisPtr, ct.name() + "_val");
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
				LLVMTypeRef payloadArr = LLVMArrayType(i8t, LLVMTypeMapper.UNION_MIN_PAYLOAD_BYTES);
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

		// ── Array .len — runtime-count companion alloca ──────────────────────
		if (baseType instanceof ArrayType && node.memberName.equals("len")
				&& node.target instanceof IdentifierExpression targetId)
		{
			String lenKey = "__" + targetId.name + "_len";
			LLVMValueRef lenAlloca = namedValues.get(lenKey);
			if (lenAlloca != null)
			{
				return LLVMBuildLoad2(builder, LLVMInt64TypeInContext(context), lenAlloca,
						targetId.name + "_len");
			}
			// Compile-time known count (array literal)
			Integer staticLen = arrayElementCounts.get(targetId.name);
			if (staticLen != null)
			{
				return LLVMConstInt(LLVMInt64TypeInContext(context), staticLen, 0);
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
		else if (baseType instanceof TupleType || baseType instanceof ArrayType)
		{
			// Structural types expose trait methods via their synthetic Stringable scope
			SymbolTable tbl = analyzer.getPrimitiveImplScopes().get(baseType);
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

			// For synthetic structural methods (tuple / array toStr), generate the
			// function body immediately so the call site can link against it.
			if (ms.isSyntheticStructural())
			{
				return getOrEmitStructuralToStrFunction(baseType, mangledName);
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
					LLVMTypeRef payloadArr = LLVMArrayType(i8t, LLVMTypeMapper.UNION_MIN_PAYLOAD_BYTES);
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

			// CVT/LUA: Mark arguments as transferred if the constructor parameter
			// has 'drops' — the callee (constructor) takes ownership.
			if (regionTracker != null && ms.getType().parameterInfo != null)
			{
				for (int i = 0; i < node.arguments.size(); i++)
				{
					ParameterInfo pi = ms.getType().getParameterInfo(i + 1);
					if (pi != null && pi.isDrops()
						&& node.arguments.get(i) instanceof IdentifierExpression argIdent
						&& regionTracker.isTracked(argIdent.name))
					{
						regionTracker.markTransferred(argIdent.name);
					}
				}
			}
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

		// Build the same ordered field list used by LLVMTypeMapper to assign indices:
		// inherited fields first (depth-first, ancestors before child), then own fields.
		java.util.List<VariableSymbol> orderedFields = new java.util.ArrayList<>();
		if (ct instanceof ClassType classType)
		{
			collectInheritedFieldsForGEP(classType, orderedFields, new java.util.HashSet<>());
		}
		for (Symbol s : ct.getMemberScope().getSymbols().values())
		{
			if (s instanceof VariableSymbol field && !field.getName().equals("this"))
				orderedFields.add(field);
		}

		// Find the index of our target field by name (not identity — inherited symbols
		// may have been resolved from a different scope instance).
		int fieldIdx = -1;
		for (int i = 0; i < orderedFields.size(); i++)
		{
			if (orderedFields.get(i).getName().equals(memberName))
			{
				fieldIdx = i;
				break;
			}
		}

		if (fieldIdx >= 0)
		{
			LLVMTypeRef structType = LLVMTypeMapper.getOrCreateStructType(context, ct);
			return LLVMBuildStructGEP2(builder, structType, base, fieldIdx, memberName + "_gep");
		}

		return null;
	}

	/**
	 * Mirrors {@link LLVMTypeMapper#collectInheritedFields} to build the same ordered
	 * field list for GEP index computation.
	 */
	private void collectInheritedFieldsForGEP(
			ClassType classType,
			java.util.List<VariableSymbol> out,
			java.util.Set<String> seen)
	{
		for (ClassType parent : classType.getParentTypes())
		{
			collectInheritedFieldsForGEP(parent, out, seen);
			for (Symbol s : parent.getMemberScope().getSymbols().values())
			{
				if (s instanceof VariableSymbol vs
						&& !vs.getName().equals("this")
						&& seen.add(vs.getName()))
				{
					out.add(vs);
				}
			}
		}
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
		// Strategy: convert each interpolated part to a NebulaStr using the runtime's
		// __nebula_rt_*_to_str helpers, store all parts in a stack-allocated array, and
		// call __nebula_rt_str_concat(parts, count) to produce a heap-allocated result.
		// This avoids any libc / snprintf dependency and correctly evaluates to a str value.

		LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
		LLVMTypeRef strT = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);

		// ── 1. Convert each interpolation part to a NebulaStr value ─────────────────
		List<LLVMValueRef> parts = new ArrayList<>();
		for (Expression part : node.parts)
		{
			LLVMValueRef strVal = emitInterpolationPartAsStr(part);
			if (strVal != null)
				parts.add(strVal);
		}

		int count = parts.size();

		// ── 2. Build a stack-allocated array of NebulaStr ────────────────────────────
		LLVMTypeRef arrType   = LLVMArrayType(strT, count);
		LLVMValueRef arrAlloca = LLVMBuildAlloca(builder, arrType, "interp_parts");

		for (int i = 0; i < count; i++)
		{
			LLVMValueRef[] indices = {
				LLVMConstInt(i64t, 0, 0),
				LLVMConstInt(i64t, i, 0)
			};
			LLVMValueRef elemPtr = LLVMBuildGEP2(builder, arrType, arrAlloca,
				new PointerPointer<>(indices), 2, "ipart_ptr_" + i);
			LLVMBuildStore(builder, parts.get(i), elemPtr);
		}

		// Get pointer to first element (i.e. ptr to the array start)
		LLVMValueRef[] firstIdx = { LLVMConstInt(i64t, 0, 0), LLVMConstInt(i64t, 0, 0) };
		LLVMValueRef partsPtr   = LLVMBuildGEP2(builder, arrType, arrAlloca,
			new PointerPointer<>(firstIdx), 2, "interp_parts_ptr");

		// ── 3. Call __nebula_rt_str_concat(parts_ptr, count) ────────────────────────
		LLVMTypeRef ptrT       = LLVMPointerTypeInContext(context, 0);
		LLVMTypeRef concatType = LLVMFunctionType(strT,
			new PointerPointer<>(new LLVMTypeRef[]{ ptrT, i64t }), 2, /* varArg */ 0);
		LLVMValueRef concatFn  = getOrDeclareIntrinsic("__nebula_rt_str_concat", concatType);

		LLVMValueRef[] callArgs = {
			partsPtr,
			LLVMConstInt(i64t, count, 0)
		};
		return LLVMBuildCall2(builder, concatType, concatFn,
			new PointerPointer<>(callArgs), 2, "interp_result");
	}

	/**
	 * A {@link FormattedInterpolationExpression} appearing outside of a string
	 * interpolation context (e.g. as a standalone expression) falls through to the
	 * standard interpolation path by immediately converting the inner expression.
	 * In practice these nodes only occur as parts of a
	 * {@link StringInterpolationExpression}, so this delegate is a safety net.
	 */
	@Override
	public LLVMValueRef visitFormattedInterpolationExpression(FormattedInterpolationExpression node)
	{
		return emitInterpolationPartAsStr(node);
	}

	/**
	 * Converts a single string-interpolation part expression to a {@code NebulaStr}
	 * ({@code { ptr, i64 }}) LLVM value.
	 *
	 * <ul>
	 *   <li>Literal string fragments → constant {@code { ptr, len }} struct.</li>
	 *   <li>{@code str} expressions → used directly.</li>
	 *   <li>Integer / float / bool types → forwarded to the appropriate
	 *       {@code __nebula_rt_*_to_str} runtime helper.</li>
	 *   <li>{@link FormattedInterpolationExpression} → forwarded to
	 *       {@code __nebula_rt_i64_to_str_fmt}.</li>
	 * </ul>
	 */
	private LLVMValueRef emitInterpolationPartAsStr(Expression part)
	{
		LLVMTypeRef i32t = LLVMInt32TypeInContext(context);
		LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
		LLVMTypeRef strT = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);

		// ── Formatted expression: {expr:formatSpec} ──────────────────────────────────
		if (part instanceof FormattedInterpolationExpression fmtExpr)
		{
			return emitFormattedInterpolationPart(fmtExpr);
		}

		// ── Literal string fragments ─────────────────────────────────────────────────
		if (part instanceof LiteralExpression le && le.type == LiteralExpression.LiteralType.STRING)
		{
			String text    = le.value.toString();
			int utf8Len    = text.getBytes(StandardCharsets.UTF_8).length;
			LLVMValueRef gstr = emitGlobalStringPtr(text, "istr");
			LLVMValueRef strAlloca = LLVMBuildAlloca(builder, strT, "istr_val");
			LLVMBuildStore(builder, gstr,
				LLVMBuildStructGEP2(builder, strT, strAlloca, 0, "istr_ptr"));
			LLVMBuildStore(builder, LLVMConstInt(i64t, utf8Len, 0),
				LLVMBuildStructGEP2(builder, strT, strAlloca, 1, "istr_len"));
			return LLVMBuildLoad2(builder, strT, strAlloca, "istr_lit_val");
		}

		// ── Evaluate the expression ──────────────────────────────────────────────────
		LLVMValueRef argVal = part.accept(this);
		if (argVal == null)
			return null;

		Type partType = analyzer.getType(part);
		if (partType == null)
			return null;

		// Already a str — use directly
		if (partType == PrimitiveType.STR)
			return argVal;

		// Check if the type implements Stringable — i.e. has a 'toStr' method in scope.
		// This covers enums, structs, and classes.  Primitives are excluded because they
		// are handled by the dedicated runtime helpers below (e.g. i32 → __nebula_rt_i32_to_str).
		if (partType instanceof CompositeType ct)
		{
			SymbolTable typeScope = ct.getMemberScope();
			Symbol toStrSym = typeScope.resolveLocal("toStr");
			if (toStrSym instanceof MethodSymbol toStrMs)
			{
				String mangledName = toStrMs.getMangledName();
				LLVMTypeRef strResultType = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);
				LLVMTypeRef thisLlvmType = toLLVMType(partType);
				LLVMTypeRef toStrFnType = LLVMFunctionType(strResultType,
					new PointerPointer<>(new LLVMTypeRef[]{ thisLlvmType }), 1, 0);
				LLVMValueRef toStrFn = LLVMGetNamedFunction(module, mangledName);
				if (toStrFn == null || toStrFn.isNull())
				{
					toStrFn = LLVMAddFunction(module, mangledName, toStrFnType);
				}
				else
				{
					// Use the already-emitted function's own type to avoid sig mismatches
					toStrFnType = LLVMGlobalGetValueType(toStrFn);
				}
				LLVMValueRef[] toStrArgs = { argVal };
				return LLVMBuildCall2(builder, toStrFnType, toStrFn,
					new PointerPointer<>(toStrArgs), 1, "tostr_result");
			}
		}

		// bool → __nebula_rt_bool_to_str(i32)
		if (partType == PrimitiveType.BOOL)
		{
			LLVMTypeRef fnType = LLVMFunctionType(strT,
				new PointerPointer<>(new LLVMTypeRef[]{ i32t }), 1, 0);
			LLVMValueRef fn    = getOrDeclareIntrinsic("__nebula_rt_bool_to_str", fnType);
			LLVMValueRef asI32 = LLVMBuildZExt(builder, argVal, i32t, "bool_i32");
			return LLVMBuildCall2(builder, fnType, fn,
				new PointerPointer<>(new LLVMValueRef[]{ asI32 }), 1, "bool_str");
		}

		// float / double → __nebula_rt_f64_to_str(f64)
		if (partType == PrimitiveType.F64 || partType == PrimitiveType.F32)
		{
			LLVMTypeRef f64t   = LLVMDoubleTypeInContext(context);
			LLVMTypeRef fnType = LLVMFunctionType(strT,
				new PointerPointer<>(new LLVMTypeRef[]{ f64t }), 1, 0);
			LLVMValueRef fn    = getOrDeclareIntrinsic("__nebula_rt_f64_to_str", fnType);
			LLVMValueRef asF64 = emitCast(argVal, partType, PrimitiveType.F64);
			return LLVMBuildCall2(builder, fnType, fn,
				new PointerPointer<>(new LLVMValueRef[]{ asF64 }), 1, "f64_str");
		}

		// unsigned integers → __nebula_rt_u64_to_str(u64)
		if (isUnsignedType(partType))
		{
			LLVMTypeRef fnType  = LLVMFunctionType(strT,
				new PointerPointer<>(new LLVMTypeRef[]{ i64t }), 1, 0);
			LLVMValueRef fn     = getOrDeclareIntrinsic("__nebula_rt_u64_to_str", fnType);
			LLVMValueRef asU64  = emitCast(argVal, partType, PrimitiveType.U64);
			return LLVMBuildCall2(builder, fnType, fn,
				new PointerPointer<>(new LLVMValueRef[]{ asU64 }), 1, "u64_str");
		}

		// signed integers (default) → __nebula_rt_i64_to_str(i64)
		{
			LLVMTypeRef fnType  = LLVMFunctionType(strT,
				new PointerPointer<>(new LLVMTypeRef[]{ i64t }), 1, 0);
			LLVMValueRef fn     = getOrDeclareIntrinsic("__nebula_rt_i64_to_str", fnType);
			LLVMValueRef asI64  = emitCast(argVal, partType, PrimitiveType.I64);
			return LLVMBuildCall2(builder, fnType, fn,
				new PointerPointer<>(new LLVMValueRef[]{ asI64 }), 1, "i64_str");
		}
	}

	/**
	 * Emits code for a {@link FormattedInterpolationExpression} by dispatching to
	 * the appropriate {@code __nebula_rt_*_to_str_fmt} runtime helper.
	 *
	 * <p>Currently supported: all integer types → {@code __nebula_rt_i64_to_str_fmt}.
	 * Float/str/bool fall back to their unformatted helpers (format spec ignored).
	 */
	private LLVMValueRef emitFormattedInterpolationPart(FormattedInterpolationExpression node)
	{
		LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
		LLVMTypeRef strT = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);

		LLVMValueRef argVal = node.expression.accept(this);
		if (argVal == null)
			return null;

		Type partType = analyzer.getType(node.expression);
		if (partType == null)
			return null;

		// Build a NebulaStr for the format specifier string
		LLVMValueRef fmtStr = emitStringLiteralValue(node.formatSpec);

		// Integers (signed or unsigned) → __nebula_rt_i64_to_str_fmt(i64, NebulaStr)
		if (partType == PrimitiveType.I8  || partType == PrimitiveType.I16
			|| partType == PrimitiveType.I32 || partType == PrimitiveType.I64
			|| partType == PrimitiveType.U8  || partType == PrimitiveType.U16
			|| partType == PrimitiveType.U32 || partType == PrimitiveType.U64)
		{
			LLVMTypeRef fnType = LLVMFunctionType(strT,
				new PointerPointer<>(new LLVMTypeRef[]{ i64t, strT }), 2, 0);
			LLVMValueRef fn    = getOrDeclareIntrinsic("__nebula_rt_i64_to_str_fmt", fnType);
			LLVMValueRef asI64 = emitCast(argVal, partType, PrimitiveType.I64);
			LLVMValueRef[] args = { asI64, fmtStr };
			return LLVMBuildCall2(builder, fnType, fn,
				new PointerPointer<>(args), 2, "fmt_i64_str");
		}

		// All other types: fall back to unformatted conversion
		return emitInterpolationPartAsStr(node.expression);
	}

	// ── Structural toStr helpers ─────────────────────────────────────────────────

	/**
	 * Returns a constant {@code i8*} (opaque {@code ptr}) that points to the
	 * first byte of a null-terminated LLVM global string for {@code text}.
	 *
	 * <p>Unlike {@link org.bytedeco.llvm.global.LLVM#LLVMBuildGlobalStringPtr
	 * LLVMBuildGlobalStringPtr}, this method works regardless of whether the
	 * IR builder currently has an active insertion point, making it safe to
	 * call while emitting global constants (before any function body exists).
	 *
	 * <p>Equivalent globals are de-duplicated by name so that each unique
	 * {@code text} only produces one {@code @.str.N} in the IR.
	 */
	private LLVMValueRef emitGlobalStringPtr(String text, String hint)
	{
		byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
		// Build a null-terminated byte array: [len+1 x i8] = <utf8 bytes..., 0x00>
		LLVMTypeRef i8t    = LLVMInt8TypeInContext(context);
		LLVMTypeRef arrT   = LLVMArrayType(i8t, utf8.length + 1);
		byte[] nullTerminated = new byte[utf8.length + 1];
		System.arraycopy(utf8, 0, nullTerminated, 0, utf8.length);
		nullTerminated[utf8.length] = 0;

		// Build an LLVM constant array from the bytes
		LLVMValueRef[] byteConsts = new LLVMValueRef[nullTerminated.length];
		for (int i = 0; i < nullTerminated.length; i++)
		{
			byteConsts[i] = LLVMConstInt(i8t, nullTerminated[i] & 0xFF, 0);
		}
		LLVMValueRef constArr = LLVMConstArray(i8t,
			new PointerPointer<>(byteConsts), byteConsts.length);

		// Use the hint as a unique-ish global name (duplicates are handled by LLVM)
		String globalName = ".str." + hint + "." + Integer.toHexString(text.hashCode());
		LLVMValueRef existing = LLVMGetNamedGlobal(module, globalName);
		if (existing == null || existing.isNull())
		{
			existing = LLVMAddGlobal(module, arrT, globalName);
			LLVMSetInitializer(existing, constArr);
			LLVMSetGlobalConstant(existing, 1);
			LLVMSetLinkage(existing, LLVMPrivateLinkage);
			LLVMSetUnnamedAddress(existing, LLVMGlobalUnnamedAddr);
		}

		// GEP [N x i8]* → i8* (ptr to first element) — this is a constant expression
		LLVMTypeRef i64t   = LLVMInt64TypeInContext(context);
		LLVMValueRef zero  = LLVMConstInt(i64t, 0, 0);
		LLVMValueRef[] idx = { zero, zero };
		return LLVMConstGEP2(arrT, existing, new PointerPointer<>(idx), 2);
	}

	/**
	 * Emits a constant {@code NebulaStr} ({@code { ptr, i64 }}) for a compile-time
	 * string literal, inserting instructions at the current builder position.
	 */
	private LLVMValueRef emitStringLiteralValue(String text)
	{
		LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
		LLVMTypeRef strT = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);
		int utf8Len = text.getBytes(StandardCharsets.UTF_8).length;
		LLVMValueRef gstr     = emitGlobalStringPtr(text, "lit");
		LLVMValueRef strAlloca = LLVMBuildAlloca(builder, strT, new BytePointer("lit_str"));
		LLVMBuildStore(builder, gstr,
			LLVMBuildStructGEP2(builder, strT, strAlloca, 0, new BytePointer("lit_ptr")));
		LLVMBuildStore(builder, LLVMConstInt(i64t, utf8Len, 0),
			LLVMBuildStructGEP2(builder, strT, strAlloca, 1, new BytePointer("lit_len")));
		return LLVMBuildLoad2(builder, strT, strAlloca, new BytePointer("lit_val"));
	}

	/**
	 * Concatenates a list of {@code NebulaStr} values into a single
	 * {@code NebulaStr} by calling {@code __nebula_rt_str_concat(parts, count)}.
	 */
	private LLVMValueRef emitStrConcatParts(List<LLVMValueRef> parts)
	{
		LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
		LLVMTypeRef strT = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);
		LLVMTypeRef ptrT = LLVMPointerTypeInContext(context, 0);

		int count = parts.size();
		LLVMTypeRef arrType    = LLVMArrayType(strT, count);
		LLVMValueRef arrAlloca = LLVMBuildAlloca(builder, arrType, new BytePointer("concat_parts"));

		for (int i = 0; i < count; i++)
		{
			LLVMValueRef[] idxs = {
				LLVMConstInt(i64t, 0, 0),
				LLVMConstInt(i64t, i, 0)
			};
			LLVMValueRef elemPtr = LLVMBuildGEP2(builder, arrType, arrAlloca,
				new PointerPointer<>(idxs), 2, "part_ptr_" + i);
			LLVMBuildStore(builder, parts.get(i), elemPtr);
		}

		LLVMValueRef[] firstIdx = { LLVMConstInt(i64t, 0, 0), LLVMConstInt(i64t, 0, 0) };
		LLVMValueRef partsPtr = LLVMBuildGEP2(builder, arrType, arrAlloca,
			new PointerPointer<>(firstIdx), 2, "parts_ptr");

		LLVMTypeRef concatFnType = LLVMFunctionType(strT,
			new PointerPointer<>(new LLVMTypeRef[]{ ptrT, i64t }), 2, 0);
		LLVMValueRef concatFn = getOrDeclareIntrinsic("__nebula_rt_str_concat", concatFnType);

		LLVMValueRef[] callArgs = { partsPtr, LLVMConstInt(i64t, count, 0) };
		return LLVMBuildCall2(builder, concatFnType, concatFn,
			new PointerPointer<>(callArgs), 2, "concat_result");
	}

	/**
	 * Converts a value of the given Nebula type to a {@code NebulaStr}.
	 * Inserts conversion instructions at the current builder insertion point.
	 * Handles all primitive scalars, {@code str}, and nested structural types.
	 */
	private LLVMValueRef emitValueToNebulaStr(LLVMValueRef val, Type type)
	{
		LLVMTypeRef i32t = LLVMInt32TypeInContext(context);
		LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
		LLVMTypeRef strT = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);

		if (val == null)
			return emitStringLiteralValue("<null>");

		if (type == PrimitiveType.STR)
			return val;

		if (type == PrimitiveType.BOOL)
		{
			LLVMTypeRef fnType = LLVMFunctionType(strT,
				new PointerPointer<>(new LLVMTypeRef[]{ i32t }), 1, 0);
			LLVMValueRef fn    = getOrDeclareIntrinsic("__nebula_rt_bool_to_str", fnType);
			LLVMValueRef asI32 = LLVMBuildZExt(builder, val, i32t, new BytePointer("bool_i32"));
			return LLVMBuildCall2(builder, fnType, fn,
				new PointerPointer<>(new LLVMValueRef[]{ asI32 }), 1, "bool_str");
		}

		if (type == PrimitiveType.F64 || type == PrimitiveType.F32)
		{
			LLVMTypeRef f64t   = LLVMDoubleTypeInContext(context);
			LLVMTypeRef fnType = LLVMFunctionType(strT,
				new PointerPointer<>(new LLVMTypeRef[]{ f64t }), 1, 0);
			LLVMValueRef fn    = getOrDeclareIntrinsic("__nebula_rt_f64_to_str", fnType);
			LLVMValueRef asF64 = emitCast(val, type, PrimitiveType.F64);
			return LLVMBuildCall2(builder, fnType, fn,
				new PointerPointer<>(new LLVMValueRef[]{ asF64 }), 1, "f64_str");
		}

		if (isUnsignedType(type))
		{
			LLVMTypeRef fnType  = LLVMFunctionType(strT,
				new PointerPointer<>(new LLVMTypeRef[]{ i64t }), 1, 0);
			LLVMValueRef fn     = getOrDeclareIntrinsic("__nebula_rt_u64_to_str", fnType);
			LLVMValueRef asU64  = emitCast(val, type, PrimitiveType.U64);
			return LLVMBuildCall2(builder, fnType, fn,
				new PointerPointer<>(new LLVMValueRef[]{ asU64 }), 1, "u64_str");
		}

		// Default: signed integer
		{
			LLVMTypeRef fnType  = LLVMFunctionType(strT,
				new PointerPointer<>(new LLVMTypeRef[]{ i64t }), 1, 0);
			LLVMValueRef fn     = getOrDeclareIntrinsic("__nebula_rt_i64_to_str", fnType);
			LLVMValueRef asI64  = emitCast(val, type, PrimitiveType.I64);
			return LLVMBuildCall2(builder, fnType, fn,
				new PointerPointer<>(new LLVMValueRef[]{ asI64 }), 1, "i64_str");
		}
	}

	/**
	 * Returns the LLVM type for a fat-pointer used to carry both an array pointer
	 * and its element count through the vtable interface.
	 * Layout: {@code { ptr, i64 }}.
	 */
	private LLVMTypeRef arrayFatPtrType()
	{
		LLVMTypeRef ptrT = LLVMPointerTypeInContext(context, 0);
		LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
		return LLVMStructTypeInContext(context,
			new PointerPointer<>(new LLVMTypeRef[]{ ptrT, i64t }), 2, 0);
	}

	/**
	 * Generates (or returns the cached) LLVM function that converts a {@link TupleType}
	 * value to a {@code NebulaStr} of the form {@code "(elem0, elem1, ...)"}.
	 * <p>
	 * The function signature is {@code TupleLLVMType -> {ptr, i64}} — the tuple
	 * is passed <em>by value</em>, so {@code extractvalue} is used for field access.
	 * </p>
	 */
	private LLVMValueRef getOrEmitTupleToStrFunction(TupleType tt, String funcName)
	{
		LLVMTypeRef strT   = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);
		LLVMTypeRef tupleT = LLVMTypeMapper.getOrCreateTupleType(context, tt);

		LLVMTypeRef fnType = LLVMFunctionType(strT,
			new PointerPointer<>(new LLVMTypeRef[]{ tupleT }), 1, 0);

		LLVMValueRef fn = LLVMGetNamedFunction(module, new BytePointer(funcName));
		if (fn == null || fn.isNull())
			fn = LLVMAddFunction(module, new BytePointer(funcName), fnType);
		if (LLVMCountBasicBlocks(fn) > 0)
			return fn;

		LLVMSetLinkage(fn, LLVMPrivateLinkage);

		LLVMBasicBlockRef savedBB = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef entry   = LLVMAppendBasicBlockInContext(context, fn, new BytePointer("entry"));
		LLVMPositionBuilderAtEnd(builder, entry);

		LLVMValueRef tupleVal = LLVMGetParam(fn, 0);

		List<LLVMValueRef> parts = new ArrayList<>();
		parts.add(emitStringLiteralValue("("));

		for (int i = 0; i < tt.elementTypes.size(); i++)
		{
			if (i > 0) parts.add(emitStringLiteralValue(", "));
			Type     elemType = tt.elementTypes.get(i);
			LLVMValueRef elem = LLVMBuildExtractValue(builder, tupleVal, i, new BytePointer("t_f" + i));
			parts.add(emitValueToNebulaStr(elem, elemType));
		}

		parts.add(emitStringLiteralValue(")"));
		LLVMBuildRet(builder, emitStrConcatParts(parts));

		if (savedBB != null && !savedBB.isNull())
			LLVMPositionBuilderAtEnd(builder, savedBB);

		return fn;
	}

	/**
	 * Generates (or returns the cached) LLVM function that converts an array
	 * (represented as a {@code { ptr, i64 }} fat pointer) to a {@code NebulaStr}
	 * of the form {@code "[elem0, elem1, ...]"}.
	 * <p>
	 * The function signature is {@code {ptr, i64} -> {ptr, i64}} — the fat pointer
	 * is passed by value.  A loop is emitted to convert each element to
	 * {@code NebulaStr} using a dynamic stack allocation, then
	 * {@code __nebula_rt_format_array_str} assembles the final string.
	 * </p>
	 */
	private LLVMValueRef getOrEmitArrayToStrFunction(ArrayType at, String funcName)
	{
		LLVMTypeRef strT    = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);
		LLVMTypeRef ptrT    = LLVMPointerTypeInContext(context, 0);
		LLVMTypeRef i64t    = LLVMInt64TypeInContext(context);
		LLVMTypeRef fatT    = arrayFatPtrType();
		LLVMTypeRef elemT   = toLLVMType(at.baseType);

		// Signature: { ptr, i64 } -> { ptr, i64 }
		LLVMTypeRef fnType = LLVMFunctionType(strT,
			new PointerPointer<>(new LLVMTypeRef[]{ fatT }), 1, 0);

		LLVMValueRef fn = LLVMGetNamedFunction(module, new BytePointer(funcName));
		if (fn == null || fn.isNull())
			fn = LLVMAddFunction(module, new BytePointer(funcName), fnType);
		if (LLVMCountBasicBlocks(fn) > 0)
			return fn;

		LLVMSetLinkage(fn, LLVMPrivateLinkage);

		LLVMBasicBlockRef savedBB = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef entry   = LLVMAppendBasicBlockInContext(context, fn, new BytePointer("entry"));
		LLVMPositionBuilderAtEnd(builder, entry);

		LLVMValueRef fatVal  = LLVMGetParam(fn, 0);
		LLVMValueRef arrPtr  = LLVMBuildExtractValue(builder, fatVal, 0, new BytePointer("arr_ptr"));
		LLVMValueRef count   = LLVMBuildExtractValue(builder, fatVal, 1, new BytePointer("arr_len"));

		// Allocate NebulaStr[count] on stack (variable-length alloca)
		LLVMValueRef partsAlloca = LLVMBuildArrayAlloca(builder, strT, count, new BytePointer("arr_parts"));

		// idx alloca
		LLVMValueRef idxAlloca = LLVMBuildAlloca(builder, i64t, new BytePointer("arr_idx"));
		LLVMBuildStore(builder, LLVMConstInt(i64t, 0, 0), idxAlloca);

		LLVMBasicBlockRef hdrBB  = LLVMAppendBasicBlockInContext(context, fn, new BytePointer("arr_hdr"));
		LLVMBasicBlockRef bodyBB = LLVMAppendBasicBlockInContext(context, fn, new BytePointer("arr_body"));
		LLVMBasicBlockRef exitBB = LLVMAppendBasicBlockInContext(context, fn, new BytePointer("arr_exit"));

		LLVMBuildBr(builder, hdrBB);

		// Header: check idx < count
		LLVMPositionBuilderAtEnd(builder, hdrBB);
		LLVMValueRef idx  = LLVMBuildLoad2(builder, i64t, idxAlloca, new BytePointer("idx"));
		LLVMValueRef cond = LLVMBuildICmp(builder, LLVMIntULT, idx, count, new BytePointer("loop_cond"));
		LLVMBuildCondBr(builder, cond, bodyBB, exitBB);

		// Body: load element, convert, store in parts
		LLVMPositionBuilderAtEnd(builder, bodyBB);
		LLVMValueRef[] gepIdx   = { idx };
		LLVMValueRef elemPtr    = LLVMBuildGEP2(builder, elemT, arrPtr,
			new PointerPointer<>(gepIdx), 1, "elem_ptr");
		LLVMValueRef elemVal    = LLVMBuildLoad2(builder, elemT, elemPtr, new BytePointer("elem_val"));
		LLVMValueRef elemStr    = emitValueToNebulaStr(elemVal, at.baseType);
		LLVMValueRef partPtr    = LLVMBuildGEP2(builder, strT, partsAlloca,
			new PointerPointer<>(gepIdx), 1, "part_ptr");
		LLVMBuildStore(builder, elemStr, partPtr);
		LLVMValueRef idxNext    = LLVMBuildAdd(builder, idx, LLVMConstInt(i64t, 1, 0),
			new BytePointer("idx_next"));
		LLVMBuildStore(builder, idxNext, idxAlloca);
		LLVMBuildBr(builder, hdrBB);

		// Exit: call __nebula_rt_format_array_str(parts, count)
		LLVMPositionBuilderAtEnd(builder, exitBB);
		LLVMTypeRef fmtFnType = LLVMFunctionType(strT,
			new PointerPointer<>(new LLVMTypeRef[]{ ptrT, i64t }), 2, 0);
		LLVMValueRef fmtFn = getOrDeclareIntrinsic("__nebula_rt_format_array_str", fmtFnType);
		LLVMValueRef[] fmtArgs = { partsAlloca, count };
		LLVMValueRef result = LLVMBuildCall2(builder, fmtFnType, fmtFn,
			new PointerPointer<>(fmtArgs), 2, "arr_str");
		LLVMBuildRet(builder, result);

		if (savedBB != null && !savedBB.isNull())
			LLVMPositionBuilderAtEnd(builder, savedBB);

		return fn;
	}

	/**
	 * Returns the LLVM function for structural {@code toStr} on a {@link TupleType}
	 * or {@link ArrayType}, generating its body on first call.
	 */
	private LLVMValueRef getOrEmitStructuralToStrFunction(Type type, String funcName)
	{
		if (type instanceof TupleType tt)
			return getOrEmitTupleToStrFunction(tt, funcName);
		if (type instanceof ArrayType at)
			return getOrEmitArrayToStrFunction(at, funcName);
		throw new CodegenException("Cannot generate structural toStr for type: " + type.name());
	}

	/**
	 * Emits a vtable-slot wrapper for a structural type ({@link TupleType} or
	 * {@link ArrayType}) implementing a trait method.
	 * <p>
	 * The wrapper has the universal vtable signature {@code (ptr %self) -> RetType}
	 * and bridges to the concrete structural {@code toStr} function.
	 * <ul>
	 *   <li><b>TupleType</b>: loads the tuple struct from {@code %self} and calls
	 *       the by-value structural function.</li>
	 *   <li><b>ArrayType</b>: loads the {@code {ptr,i64}} fat-pointer struct from
	 *       {@code %self} and calls the fat-pointer structural function.</li>
	 * </ul>
	 * </p>
	 */
	private LLVMValueRef getOrEmitStructuralVtableWrapper(
			TraitType bound, String methodName, Type concreteType)
	{
		String wrapperName = "__nebula_vtwrap_"
				+ concreteType.name().replaceAll("[^a-zA-Z0-9]", "_")
				+ "_" + bound.name() + "_" + methodName;

		LLVMValueRef existing = LLVMGetNamedFunction(module, new BytePointer(wrapperName));
		if (existing != null && !existing.isNull() && LLVMCountBasicBlocks(existing) > 0)
			return existing;

		MethodSymbol traitMethod = bound.getRequiredMethods().get(methodName);
		Type         returnType  = traitMethod.getType().getReturnType();
		LLVMTypeRef  retLlvmType = toLLVMType(returnType);
		LLVMTypeRef  ptrT        = LLVMPointerTypeInContext(context, 0);

		// ── Wrapper signature: (ptr %self) -> retType ────────────────────────────────
		LLVMTypeRef wrapperFnType = LLVMFunctionType(retLlvmType,
			new PointerPointer<>(new LLVMTypeRef[]{ ptrT }), 1, 0);

		LLVMValueRef wrapperFn = LLVMGetNamedFunction(module, new BytePointer(wrapperName));
		if (wrapperFn == null || wrapperFn.isNull())
			wrapperFn = LLVMAddFunction(module, new BytePointer(wrapperName), wrapperFnType);
		LLVMSetLinkage(wrapperFn, LLVMPrivateLinkage);

		LLVMBasicBlockRef savedBB = LLVMGetInsertBlock(builder);
		LLVMBasicBlockRef entry   = LLVMAppendBasicBlockInContext(context, wrapperFn, new BytePointer("entry"));
		LLVMPositionBuilderAtEnd(builder, entry);

		LLVMValueRef selfPtr = LLVMGetParam(wrapperFn, 0);

		// ── Determine concrete function name from the synthetic impl scope ───────────
		SymbolTable implScope = analyzer.getPrimitiveImplScopes().get(concreteType);
		if (implScope == null)
			throw new CodegenException("No impl scope for structural type: " + concreteType.name());
		Symbol sym = implScope.resolveLocal(methodName);
		if (!(sym instanceof MethodSymbol ms))
			throw new CodegenException("No MethodSymbol for '" + methodName + "' in structural scope of " + concreteType.name());
		String concreteFuncName = ms.getMangledName();

		LLVMValueRef result;

		if (concreteType instanceof TupleType tt)
		{
			// Load tuple struct from self and call the by-value toStr
			LLVMTypeRef tupleT    = LLVMTypeMapper.getOrCreateTupleType(context, tt);
			LLVMValueRef tupleVal = LLVMBuildLoad2(builder, tupleT, selfPtr, new BytePointer("tuple_self"));
			LLVMValueRef concreteFn = getOrEmitTupleToStrFunction(tt, concreteFuncName);
			LLVMTypeRef concreteFnType = LLVMGlobalGetValueType(concreteFn);
			LLVMValueRef[] args = { tupleVal };
			result = LLVMBuildCall2(builder, concreteFnType, concreteFn,
				new PointerPointer<>(args), 1, "tuple_str");
		}
		else if (concreteType instanceof ArrayType at)
		{
			// Load the {ptr,i64} fat pointer from self and call the fat-ptr toStr
			LLVMTypeRef fatT      = arrayFatPtrType();
			LLVMValueRef fatVal   = LLVMBuildLoad2(builder, fatT, selfPtr, new BytePointer("arr_fat_self"));
			LLVMValueRef concreteFn = getOrEmitArrayToStrFunction(at, concreteFuncName);
			LLVMTypeRef concreteFnType = LLVMGlobalGetValueType(concreteFn);
			LLVMValueRef[] args = { fatVal };
			result = LLVMBuildCall2(builder, concreteFnType, concreteFn,
				new PointerPointer<>(args), 1, "arr_str");
		}
		else
		{
			throw new CodegenException("Unsupported structural type for vtable wrapper: " + concreteType.name());
		}

		LLVMBuildRet(builder, result);

		if (savedBB != null && !savedBB.isNull())
			LLVMPositionBuilderAtEnd(builder, savedBB);

		return wrapperFn;
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
			Set<String> prevInlineStructVars = null;
			if (!armBindings.isEmpty())
			{
				prevNamedValues = new HashMap<>(namedValues);
				prevInlineStructVars = new HashSet<>(inlineStructVars);
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
				inlineStructVars.clear();
				inlineStructVars.addAll(prevInlineStructVars);
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
			// e.g. "Form::Normal" (new syntax) or "Form.Normal" / "Normal" (legacy).
			// Normalise the AST double-colon separator to the dot used as the discriminant key.
			String rawName = (tp.type instanceof org.nebula.nebc.ast.types.NamedType nt2)
				? nt2.qualifiedName
				: tp.type.getClass().getSimpleName();
			// "Enum::Variant" → "Enum.Variant"
			String typeName = rawName.replace("::", ".");
			if (tagVal != null)
			{
				// Try qualified key first, then unqualified with union/enum prefix.
				Integer disc = enumDiscriminants.get(typeName);
				if (disc == null)
					disc = unionDiscriminants.get(typeName);
				if (disc == null && selectorType != null)
				{
					// Strip any qualifier already present then re-attach the selector type name
					String simpleName = typeName.contains(".") ? typeName.substring(typeName.lastIndexOf('.') + 1) : typeName;
					disc = enumDiscriminants.get(selectorType.name() + "." + simpleName);
					if (disc == null)
						disc = unionDiscriminants.get(selectorType.name() + "." + simpleName);
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
			// Normalise "Union::Variant" → simple variant name for the key lookup.
			String simpleVariant = dp.variantName.contains("::")
				? dp.variantName.substring(dp.variantName.lastIndexOf("::") + 2)
				: dp.variantName;
			String key = (unionTypeName != null)
				? unionTypeName + "." + simpleVariant
				: simpleVariant;
			Integer disc = unionDiscriminants.get(key);
			if (disc == null)
				disc = unionDiscriminants.get(simpleVariant);
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

		if (pat instanceof TuplePattern tp)
		{
			// Match each element of the tuple independently, AND all conditions together.
			if (!(selectorType instanceof TupleType tt))
				return null; // type mismatch — treat as wildcard

			LLVMValueRef combined = null;
			for (int i = 0; i < tp.elements.size() && i < tt.elementTypes.size(); i++)
			{
				Pattern      elemPat      = tp.elements.get(i);
				Type         elemType     = tt.elementTypes.get(i);
				LLVMValueRef elemVal      = LLVMBuildExtractValue(builder, selectorVal, i, "tup_elem_" + i);
				LLVMValueRef elemTagVal   = (elemType instanceof EnumType)  ? elemVal : null;
				String       elemUnionNm  = (elemType instanceof UnionType) ? elemType.name() : null;

				LLVMValueRef elemCond = emitPatternCondition(elemPat, elemVal, elemType, elemTagVal, elemUnionNm);
				if (elemCond == null)
					continue; // wildcard element — always matches, skip

				combined = (combined == null)
					? elemCond
					: LLVMBuildAnd(builder, combined, elemCond, "tup_and");
			}
			return combined; // null if every sub-pattern is a wildcard
		}

		return null; // unknown pattern — treat as wildcard
	}

	/**
	 * For destructuring patterns on tagged unions, allocates binding allocas
	 * for each bound variable and extracts the payload.
	 * <p>
	 * The payload type is resolved from the variant's {@link MethodSymbol} in the
	 * union's member scope, ensuring the correct LLVM type and size are used for
	 * the alloca and load — including aggregate types such as structs.
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

		// Locate the variant's payload type via the union's member scope.
		// Normalise "Union::Variant" → "Variant" before building the dot key.
		String simpleVariant = dp.variantName.contains("::")
			? dp.variantName.substring(dp.variantName.lastIndexOf("::") + 2)
			: dp.variantName;
		String key = ut.name() + "." + simpleVariant;
		Integer disc = unionDiscriminants.get(key);
		if (disc == null)
			return bindings;

		LLVMTypeRef unionStructType = LLVMTypeMapper.getOrCreateUnionStructType(context, ut);

		// alloca the whole union struct, store the value, then GEP into it
		LLVMValueRef unionAlloca = LLVMBuildAlloca(builder, unionStructType, "union_match");
		LLVMBuildStore(builder, selectorVal, unionAlloca);

		// GEP to field 1 (the opaque [N x i8] payload buffer)
		LLVMValueRef payloadGep = LLVMBuildStructGEP2(builder, unionStructType, unionAlloca, 1, "payload_gep");

		// Resolve the variant's actual payload type from its constructor MethodSymbol.
		// Use the simple (unqualified) variant name for the member scope lookup.
		Symbol variantSym = ut.getMemberScope().resolveLocal(simpleVariant);
		if (variantSym instanceof MethodSymbol ms)
		{
			FunctionType ft = ms.getType();
			if (!ft.parameterTypes.isEmpty() && dp.bindings.size() == 1)
			{
				// Single binding: the entire payload is one value (may be a struct,
				// str, primitive, etc.).  Load the full payload type from the buffer.
				Type payloadType = ft.parameterTypes.get(0);
				LLVMTypeRef payloadLLVMType = toLLVMType(payloadType);
				String bindingName = dp.bindings.get(0);

				LLVMValueRef bindingAlloca = LLVMBuildAlloca(builder, payloadLLVMType, bindingName);
				LLVMValueRef loaded = LLVMBuildLoad2(builder, payloadLLVMType, payloadGep, bindingName + "_raw");
				LLVMBuildStore(builder, loaded, bindingAlloca);
				bindings.put(bindingName, bindingAlloca);

				// Struct payloads are value-types that live inline in the alloca;
				// register them so visitIdentifierExpression returns the pointer
				// directly (needed for GEP-based field access).
				if (payloadType instanceof StructType)
				{
					inlineStructVars.add(bindingName);
				}
			}
			else
			{
				// Multiple bindings: extract individual fields from the payload buffer
				// using each parameter's resolved type.
				LLVMTypeRef i8t = LLVMInt8TypeInContext(context);
				for (int i = 0; i < dp.bindings.size(); i++)
				{
					String bindingName = dp.bindings.get(i);
					Type fieldType = (i < ft.parameterTypes.size())
						? ft.parameterTypes.get(i)
						: null;
					LLVMTypeRef bindType = (fieldType != null)
						? toLLVMType(fieldType)
						: LLVMInt64TypeInContext(context);

					LLVMValueRef[] gepIdx = {
						LLVMConstInt(LLVMInt64TypeInContext(context), (long) i * 8, 0)
					};
					LLVMValueRef fieldPtr = LLVMBuildGEP2(builder, i8t, payloadGep,
						new PointerPointer<>(gepIdx), 1, bindingName + "_field_ptr");

					LLVMValueRef bindingAlloca = LLVMBuildAlloca(builder, bindType, bindingName);
					LLVMValueRef loaded = LLVMBuildLoad2(builder, bindType, fieldPtr, bindingName + "_raw");
					LLVMBuildStore(builder, loaded, bindingAlloca);
					bindings.put(bindingName, bindingAlloca);

					if (fieldType instanceof StructType)
					{
						inlineStructVars.add(bindingName);
					}
				}
			}
		}
		else
		{
			// No MethodSymbol found (shouldn't happen for payload variants);
			// fall back to i64 loads for best-effort extraction.
			LLVMTypeRef i8t = LLVMInt8TypeInContext(context);
			for (int i = 0; i < dp.bindings.size(); i++)
			{
				String bindingName = dp.bindings.get(i);
				LLVMTypeRef bindType = LLVMInt64TypeInContext(context);

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
	public LLVMValueRef visitTuplePattern(TuplePattern node)
	{
		// Tuple patterns are emitted inline inside emitPatternCondition.
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
		LLVMValueRef msgPtr = emitGlobalStringPtr(message, "panic");
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

	// =========================================================================
	// Erased-function emission (library generics / bitcode path)
	// =========================================================================

	/**
	 * Emits a type-erased LLVM function for {@code ms} into a fresh module that
	 * shares the current LLVM context.  Returns the bitcode bytes of that module.
	 *
	 * <p>The erased function replaces each type-parameter {@code T} (with bound
	 * {@code Trait}) by an opaque {@code ptr}, and adds one extra {@code ptr}
	 * parameter per type-parameter that carries the trait vtable.  Calls to trait
	 * methods inside the body are emitted as vtable-indirect calls.</p>
	 *
	 * <p>LLVM's constant-propagation / inlining eliminates all vtable overhead at
	 * consumer compile time when the vtable is a compile-time constant (which it
	 * always is in Nebula).</p>
	 *
	 * @param decl The method declaration AST node (required – must be non-null).
	 * @param ms   The corresponding semantic symbol (must have type parameters).
	 * @return Serialised LLVM bitcode bytes, or {@code null} on failure.
	 */
	public byte[] emitErasedFunctionBitcode(MethodDeclaration decl, MethodSymbol ms)
	{
		if (decl == null || ms.getTypeParameters().isEmpty())
			return null;

		// ── 1. Build the erased substitution: every TypeParam → REF (= opaque ptr) ─
		Substitution erasedSub = new Substitution();
		for (TypeParameterType tpt : ms.getTypeParameters())
		{
			erasedSub.bind(tpt, PrimitiveType.REF);
		}

		// ── 2. Save ALL codegen state ────────────────────────────────────────────────
		LLVMModuleRef  savedModule              = this.module;
		LLVMBuilderRef savedBuilder             = this.builder;
		boolean        savedErasedMode          = this.isErasedMode;
		Substitution   savedSub                 = this.currentSubstitution;
		LLVMValueRef   savedFunction            = this.currentFunction;
		boolean        savedTerminated          = this.currentBlockTerminated;
		Type           savedRetType             = this.currentMethodReturnType;
		Map<String, LLVMValueRef> savedNamed    = new HashMap<>(this.namedValues);
		Set<String>    savedInlineStruct        = new HashSet<>(this.inlineStructVars);
		Map<String, Integer> savedArrayCounts   = new HashMap<>(this.arrayElementCounts);
		Map<String, LLVMValueRef> savedVtable   = new HashMap<>(this.erasedVtableParams);

		// ── 3. Create a fresh module in the SAME context ─────────────────────────────
		String erasedModName = ms.getMangledName() + "__erased_mod";
		LLVMModuleRef erasedModule = LLVMModuleCreateWithNameInContext(
				new BytePointer(erasedModName), context);

		// Copy target triple & data layout so the consumer can link without issues
		BytePointer triple = LLVMGetTarget(savedModule);
		if (triple != null && !triple.isNull())
			LLVMSetTarget(erasedModule, triple);
		BytePointer layout = LLVMGetDataLayoutStr(savedModule);
		if (layout != null && !layout.isNull())
			LLVMSetDataLayout(erasedModule, layout);

		// Create a dedicated builder for the erased module
		LLVMBuilderRef erasedBuilder = LLVMCreateBuilderInContext(context);

		// ── 4. Switch to erased state ────────────────────────────────────────────────
		this.module             = erasedModule;
		this.builder            = erasedBuilder;
		this.isErasedMode       = true;
		this.currentSubstitution = erasedSub;
		this.namedValues.clear();
		this.inlineStructVars.clear();
		this.arrayElementCounts.clear();
		this.erasedVtableParams.clear();

		try
		{
			// ── 5. Build the erased function signature ───────────────────────────────
			// Original params with T → REF (ptr), plus one extra ptr per type param
			// that carries its bound trait's vtable.
			FunctionType origType = ms.getType();
			List<LLVMTypeRef> llvmParams = new ArrayList<>();
			LLVMTypeRef ptrType = LLVMPointerTypeInContext(context, 0);

			// Substituted parameter types (T → ptr)
			for (Type pt : origType.getParameterTypes())
			{
				llvmParams.add(toLLVMType(erasedSub.substitute(pt)));
			}

			// One vtable ptr per type parameter that has a bound
			Map<String, Integer> vtableParamOffsets = new java.util.LinkedHashMap<>();
			for (TypeParameterType tpt : ms.getTypeParameters())
			{
				if (tpt.hasBound())
				{
					vtableParamOffsets.put(tpt.name(), llvmParams.size());
					llvmParams.add(ptrType);
				}
			}

			LLVMTypeRef retType  = toLLVMType(erasedSub.substitute(origType.getReturnType()));
			LLVMTypeRef[] paramArr = llvmParams.toArray(new LLVMTypeRef[0]);
			LLVMTypeRef erasedFnType = LLVMFunctionType(
					retType,
					new PointerPointer<>(paramArr),
					paramArr.length,
					0);

			String erasedName = ms.getMangledName() + "__erased";
			LLVMValueRef erasedFn = LLVMAddFunction(erasedModule, new BytePointer(erasedName), erasedFnType);
			LLVMSetLinkage(erasedFn, LLVMExternalLinkage);

			// ── 6. Set up entry block and bind parameters ────────────────────────────
			LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(context, erasedFn, new BytePointer("entry"));
			LLVMPositionBuilderAtEnd(erasedBuilder, entry);

			this.currentFunction         = erasedFn;
			this.currentBlockTerminated  = false;
			this.currentMethodReturnType = origType.getReturnType();

			// Bind regular parameters
			int llvmParamIdx = 0;
			// Skip 'this' if first LLVM param count > declared params (member method)
			if (origType.getParameterTypes().size() > decl.parameters.size())
			{
				LLVMValueRef thisVal = LLVMGetParam(erasedFn, llvmParamIdx++);
				Type thisType = erasedSub.substitute(origType.getParameterTypes().get(0));
				LLVMValueRef thisAlloca = LLVMBuildAlloca(erasedBuilder, toLLVMType(thisType), new BytePointer("this"));
				LLVMBuildStore(erasedBuilder, thisVal, thisAlloca);
				namedValues.put("this", thisAlloca);
			}
			for (int i = 0; i < decl.parameters.size(); i++)
			{
				Parameter param  = decl.parameters.get(i);
				LLVMValueRef pv  = LLVMGetParam(erasedFn, llvmParamIdx++);
				Type paramSemType = erasedSub.substitute(origType.getParameterTypes().get(llvmParamIdx - 1));
				LLVMValueRef alloca = LLVMBuildAlloca(erasedBuilder, toLLVMType(paramSemType), new BytePointer(param.name()));
				LLVMBuildStore(erasedBuilder, pv, alloca);
				namedValues.put(param.name(), alloca);
			}
			// Bind vtable parameters
			for (Map.Entry<String, Integer> e : vtableParamOffsets.entrySet())
			{
				erasedVtableParams.put(e.getKey(), LLVMGetParam(erasedFn, e.getValue()));
			}

			// ── 7. Visit the method body ─────────────────────────────────────────────
			if (decl.body != null)
			{
				decl.body.accept(this);
			}
			if (!currentBlockTerminated)
			{
				if (origType.getReturnType() == PrimitiveType.VOID)
					LLVMBuildRetVoid(erasedBuilder);
				else
					LLVMBuildRet(erasedBuilder, LLVMGetUndef(retType));
			}

			// ── 8. Serialize to bitcode ──────────────────────────────────────────────
			LLVMMemoryBufferRef bitcodeBuf = LLVMWriteBitcodeToMemoryBuffer(erasedModule);
			long   size   = LLVMGetBufferSize(bitcodeBuf);
			BytePointer data = LLVMGetBufferStart(bitcodeBuf);
			byte[] result = new byte[(int) size];
			for (int i = 0; i < (int) size; i++)
				result[i] = data.get(i);
			LLVMDisposeMemoryBuffer(bitcodeBuf);
			return result;
		}
		catch (Exception e)
		{
			org.nebula.nebc.util.Log.warn("Failed to emit erased bitcode for " + ms.getName() + ": " + e.getMessage());
			return null;
		}
		finally
		{
			// ── 9. Dispose erased resources and restore main state ───────────────────
			LLVMDisposeBuilder(erasedBuilder);
			LLVMDisposeModule(erasedModule);

			this.module              = savedModule;
			this.builder             = savedBuilder;
			this.isErasedMode        = savedErasedMode;
			this.currentSubstitution = savedSub;
			this.currentFunction     = savedFunction;
			this.currentBlockTerminated = savedTerminated;
			this.currentMethodReturnType = savedRetType;
			this.namedValues.clear();
			this.namedValues.putAll(savedNamed);
			this.inlineStructVars.clear();
			this.inlineStructVars.addAll(savedInlineStruct);
			this.arrayElementCounts.clear();
			this.arrayElementCounts.putAll(savedArrayCounts);
			this.erasedVtableParams.clear();
			this.erasedVtableParams.putAll(savedVtable);
		}
	}

	// ── Erased vtable call (inside the erased function body) ────────────────────

	/**
	 * Emits an indirect vtable call for {@code item.method(args)} where
	 * {@code item} has a TypeParameterType bound to a trait.
	 *
	 * <p>The vtable is stored as a {@code ptr} parameter added to the erased
	 * function for the type parameter {@code T}.  The slot index is determined
	 * by the trait's stable method ordering.</p>
	 */
	private LLVMValueRef emitErasedVtableCall(
			InvocationExpression node,
			MemberAccessExpression mae,
			TypeParameterType tpt)
	{
		LLVMTypeRef ptrType = LLVMPointerTypeInContext(context, 0);

		// Emit the receiver (loads the ptr value from the parameter alloca)
		LLVMValueRef receiverPtr = mae.target.accept(this);

		// Get the vtable parameter for this type parameter
		LLVMValueRef vtable = erasedVtableParams.get(tpt.name());
		if (vtable == null)
			throw new CodegenException("No vtable parameter for type param '" + tpt.name() + "'");

		TraitType bound = resolveEffectiveTrait(tpt.getBound());
		if (bound == null)
			throw new CodegenException("No trait associated with bound '" + tpt.getBound().name()
					+ "' for method dispatch on '" + mae.memberName + "'");
		int slotIndex = bound.getVtableSlotIndex(mae.memberName);
		if (slotIndex < 0)
			throw new CodegenException("Method '" + mae.memberName + "' not found in trait '" + bound.name() + "'");

		// Build the vtable struct type: { ptr, ptr, ... } (one slot per required method)
		int numSlots = bound.getVtableMethodNames().size();
		LLVMTypeRef[] slotTypes = new LLVMTypeRef[numSlots];
		java.util.Arrays.fill(slotTypes, ptrType);
		LLVMTypeRef vtableStructType = LLVMStructTypeInContext(
				context, new PointerPointer<>(slotTypes), numSlots, 0);

		// GEP to the slot and load the function pointer
		LLVMValueRef slotAddr = LLVMBuildStructGEP2(
				builder, vtableStructType, vtable, slotIndex,
				new BytePointer("vtable_slot_" + slotIndex));
		LLVMValueRef fnPtr = LLVMBuildLoad2(builder, ptrType, slotAddr,
				new BytePointer("vtable_fn"));

		// The vtable slot fn type: (ptr %self) -> traitReturnType
		MethodSymbol traitMethod = bound.getRequiredMethods().get(mae.memberName);
		Type returnType = traitMethod.getType().getReturnType();
		LLVMTypeRef retLlvmType  = toLLVMType(returnType);
		LLVMTypeRef[] fnArgTypes = { ptrType };
		LLVMTypeRef vtableFnType = LLVMFunctionType(
				retLlvmType, new PointerPointer<>(fnArgTypes), 1, 0);

		// Call the slot fn
		LLVMValueRef[] callArgs = { receiverPtr };
		String callName = returnType == PrimitiveType.VOID ? "" : "vtable_call";
		return LLVMBuildCall2(builder, vtableFnType, fnPtr,
				new PointerPointer<>(callArgs), 1, callName);
	}

	// ── Erased call site (consumer compilation) ─────────────────────────────────

	/**
	 * Emits the complete call sequence for a library generic method that has
	 * pre-compiled erased bitcode.
	 *
	 * <p>Steps:</p>
	 * <ol>
	 *   <li>Link the erased bitcode module into {@code module} (once per function).</li>
	 *   <li>Box each argument: alloca + store, pass the stack address as {@code ptr}.</li>
	 *   <li>Build a compile-time vtable constant for each type argument.</li>
	 *   <li>Call the erased function with the boxed args + vtable ptrs.</li>
	 * </ol>
	 *
	 * <p>LLVM's inliner + constant-propagation eliminates all boxing and vtable
	 * overhead — the resulting assembly is identical to static monomorphization.</p>
	 */
	private LLVMValueRef emitErasedCall(InvocationExpression node, MethodSymbol ms)
	{
		String erasedName = ms.getMangledName() + "__erased";

		// ── 1. Link the bitcode module (idempotent) ──────────────────────────────────
		if (!linkedErasedFunctions.contains(erasedName))
		{
			linkErasedBitcode(ms, erasedName);
			linkedErasedFunctions.add(erasedName);
		}

		LLVMValueRef erasedFn = LLVMGetNamedFunction(module, new BytePointer(erasedName));
		if (erasedFn == null || erasedFn.isNull())
			throw new CodegenException("Erased function '" + erasedName + "' not found after linking");

		// ── 2. Collect call args: box originals + vtable ptrs ────────────────────────

		// Determine effective argument nodes (may need to prepend receiver for member calls)
		List<Expression> argNodes = new ArrayList<>(node.arguments);
		if (node.target instanceof MemberAccessExpression mae)
		{
			FunctionType ft = ms.getType();
			if (!ft.getParameterTypes().isEmpty())
			{
				Type firstParam = ft.getParameterTypes().get(0);
				Type receiverType = analyzer.getType(mae.target);
				if (receiverMatchesFirstParamErased(receiverType, firstParam))
					argNodes.add(0, mae.target);
			}
		}

		// Resolve concrete type arguments: explicit ones first, then inferred from args
		List<Type> typeArgs = (node.getTypeArguments() != null && !node.getTypeArguments().isEmpty())
				? node.getTypeArguments()
				: inferTypeArgsForErased(ms, argNodes);

		List<LLVMValueRef> callArgs = new ArrayList<>();

		// Box each argument
		for (Expression argNode : argNodes)
		{
			LLVMValueRef argVal    = argNode.accept(this);
			Type         argSemTy  = analyzer.getType(argNode);
			LLVMTypeRef  argLlvmTy = toLLVMType(argSemTy);

			// If argSemTy is a composite and argVal is already a pointer (e.g. local
			// variable alloca or inlined struct var), pass the pointer directly.
			if (inlineStructVars.contains(argNode instanceof IdentifierExpression ie ? ie.name : "")
					|| (argSemTy instanceof CompositeType && LLVMGetTypeKind(LLVMTypeOf(argVal)) == LLVMPointerTypeKind))
			{
				callArgs.add(argVal); // already a pointer to struct data
			}
			else
			{
				// Use the concrete struct LLVM type for composite args so the alloca is
				// correctly sized. toLLVMType() returns opaque `ptr` for composites,
				// which would silently mis-size the allocation for structs > ptr-width.
				LLVMTypeRef boxTy;
				LLVMValueRef valToBox = argVal;

				if (argSemTy instanceof ArrayType)
				{
					// Box array args as {ptr,i64} fat pointers so the vtable wrapper can
					// recover both the element pointer and the compile-time element count.
					boxTy = arrayFatPtrType();
					LLVMTypeRef ptrT = LLVMPointerTypeInContext(context, 0);
					LLVMTypeRef i64t = LLVMInt64TypeInContext(context);
					int elemCount = 0;
					if (argNode instanceof IdentifierExpression ie
							&& arrayElementCounts.containsKey(ie.name))
					{
						elemCount = arrayElementCounts.get(ie.name);
					}
					else if (argSemTy instanceof ArrayType at && at.elementCount > 0)
					{
						elemCount = at.elementCount;
					}
					LLVMValueRef fatAlloca = LLVMBuildAlloca(builder, boxTy,
						new BytePointer("arr_fat_box"));
					LLVMValueRef ptrField = LLVMBuildStructGEP2(builder, boxTy, fatAlloca,
						0, new BytePointer("fat_pf"));
					LLVMValueRef lenField = LLVMBuildStructGEP2(builder, boxTy, fatAlloca,
						1, new BytePointer("fat_lf"));
					LLVMBuildStore(builder, argVal, ptrField);
					LLVMBuildStore(builder, LLVMConstInt(i64t, elemCount, 0), lenField);
					callArgs.add(fatAlloca);
					continue;
				}
				else if (argSemTy instanceof CompositeType ctArg)
				{
					boxTy = LLVMTypeMapper.getOrCreateStructType(context, ctArg);
				}
				else if (argSemTy instanceof TupleType tt)
				{
					// Tuples are struct values; box using the concrete LLVM struct type
					// so the vtable wrapper can load the full struct.
					boxTy = LLVMTypeMapper.getOrCreateTupleType(context, tt);
				}
				else
				{
					boxTy = argLlvmTy;
				}

				LLVMValueRef alloca = LLVMBuildAlloca(builder, boxTy, new BytePointer("erased_arg_box"));
				LLVMBuildStore(builder, valToBox, alloca);
				callArgs.add(alloca);
			}
		}

		// One vtable ptr per type param with a bound
		for (int i = 0; i < ms.getTypeParameters().size(); i++)
		{
			TypeParameterType tpt = ms.getTypeParameters().get(i);
			if (tpt.hasBound() && i < typeArgs.size())
			{
				TraitType effectiveTrait = resolveEffectiveTrait(tpt.getBound());
				if (effectiveTrait != null)
				{
					LLVMValueRef vtable = getOrCreateConcreteVtable(effectiveTrait, typeArgs.get(i));
					callArgs.add(vtable);
				}
			}
		}

		// ── 3. Build the call ─────────────────────────────────────────────────────────
		LLVMTypeRef erasedFnType = LLVMGlobalGetValueType(erasedFn);
		LLVMValueRef[] argsArr   = callArgs.toArray(new LLVMValueRef[0]);
		Type retType = ms.getType().getReturnType();
		String callName = retType == PrimitiveType.VOID ? "" : "erased_call";
		return LLVMBuildCall2(builder, erasedFnType, erasedFn,
				new PointerPointer<>(argsArr), argsArr.length, callName);
	}

	/**
	 * Infers concrete type arguments for an erased call by matching actual argument
	 * types against the declared type-parameter positions in the method signature.
	 *
	 * <p>For {@code println<T: Stringable>(item: T)} called as {@code println(x)},
	 * this returns {@code [typeof(x)]} so the caller can build the vtable.</p>
	 */
	private List<Type> inferTypeArgsForErased(MethodSymbol ms, List<Expression> argNodes)
	{
		FunctionType ft = ms.getType();
		List<Type> paramTypes = ft.getParameterTypes();
		List<TypeParameterType> typeParams = ms.getTypeParameters();

		// Build a map: TypeParameterType → concrete Type by matching param positions
		Map<String, Type> resolved = new java.util.LinkedHashMap<>();
		int argOffset = (paramTypes.size() > argNodes.size()) ? (paramTypes.size() - argNodes.size()) : 0;

		for (int i = argOffset; i < paramTypes.size() && (i - argOffset) < argNodes.size(); i++)
		{
			Type declared = paramTypes.get(i);
			if (declared instanceof TypeParameterType tpt)
			{
				if (!resolved.containsKey(tpt.name()))
				{
					Type actual = analyzer.getType(argNodes.get(i - argOffset));
					if (actual != null)
						resolved.put(tpt.name(), actual);
				}
			}
		}

		// Return in type-parameter declaration order
		List<Type> result = new ArrayList<>();
		for (TypeParameterType tpt : typeParams)
		{
			Type concrete = resolved.get(tpt.name());
			result.add(concrete != null ? concrete : PrimitiveType.REF);
		}
		return result;
	}

	private boolean receiverMatchesFirstParamErased(Type receiverType, Type firstParam)
	{
		if (receiverType instanceof TypeParameterType || firstParam instanceof TypeParameterType)
			return false; // erased calls don't need this check
		return receiverType.name().equals(firstParam.name())
				|| (firstParam == PrimitiveType.REF && receiverType instanceof CompositeType);
	}

	/**
	 * Links the erased bitcode stored on {@code ms} into {@code module}.
	 * Uses {@link LLVM#LLVMParseBitcodeInContext2} to load into the current context
	 * and {@link LLVM#LLVMLinkModules2} to merge.
	 */
	private void linkErasedBitcode(MethodSymbol ms, String erasedName)
	{
		byte[] bitcode = ms.getGenericBitcode();
		if (bitcode == null || bitcode.length == 0)
			return;

		BytePointer dataPtr = new BytePointer(bitcode.length);
		dataPtr.put(bitcode, 0, bitcode.length);
		LLVMMemoryBufferRef buf = LLVMCreateMemoryBufferWithMemoryRangeCopy(
				dataPtr, bitcode.length, new BytePointer("erased_" + erasedName));
		dataPtr.deallocate();

		// Parse into the current context
		PointerPointer<LLVMModuleRef> outModPtr = new PointerPointer<>(1L);
		int parseErr = LLVMParseBitcodeInContext2(context, buf, outModPtr);
		LLVMDisposeMemoryBuffer(buf);

		if (parseErr != 0)
			throw new CodegenException("Failed to parse erased bitcode for '" + erasedName + "'");

		LLVMModuleRef parsedMod = new LLVMModuleRef(outModPtr.get());

		// Merge into main module (parsedMod is consumed / destroyed)
		if (LLVMLinkModules2(module, parsedMod) != 0)
		{
			throw new CodegenException("Failed to link erased bitcode module for '" + erasedName + "'");
		}
	}

	// ── Concrete vtable construction (call-site, consumer compilation) ─────────

	/**
	 * Resolves the effective {@link TraitType} from a generic parameter's bound.
	 * <p>
	 * For a {@link TraitType} bound the trait itself is returned. For a
	 * {@link TagType} bound the first trait member of the tag is returned
	 * (the "ambient trait") since vtable dispatch requires a concrete trait.
	 * Returns {@code null} when the bound carries no trait (e.g. a tag of only
	 * concrete types such as {@code tag { str, i8 }}).
	 */
	private TraitType resolveEffectiveTrait(
			org.nebula.nebc.semantic.types.CompositeType bound)
	{
		if (bound instanceof TraitType tt)
			return tt;
		if (bound instanceof org.nebula.nebc.semantic.types.TagType tag)
			return tag.findAmbientTrait().orElse(null);
		return null;
	}

	/**
	 * Returns a compile-time constant global containing the vtable for
	 * {@code concreteType} implementing {@code bound}.
	 *
	 * <p>The vtable is a struct of {@code ptr} values, one per required method
	 * in trait-declaration order.  Each slot points to a thin wrapper function
	 * that boxes/unboxes the {@code ptr %self} argument and calls the concrete
	 * implementation.</p>
	 */
	private LLVMValueRef getOrCreateConcreteVtable(TraitType bound, Type concreteType)
	{
		String vtableName = "__nebula_vtable_"
				+ concreteType.name().replaceAll("[^a-zA-Z0-9]", "_")
				+ "_" + bound.name();

		LLVMValueRef existing = LLVMGetNamedGlobal(module, new BytePointer(vtableName));
		if (existing != null && !existing.isNull())
			return existing;

		List<String> methodNames = bound.getVtableMethodNames();

		LLVMValueRef[] slotFns = new LLVMValueRef[methodNames.size()];
		for (int i = 0; i < methodNames.size(); i++)
		{
			String methodName = methodNames.get(i);
			// Structural types (tuples / arrays) use an inline vtable wrapper that
			// generates the toStr logic on demand, bypassing the external impl lookup.
			if (concreteType instanceof TupleType || concreteType instanceof ArrayType)
			{
				slotFns[i] = getOrEmitStructuralVtableWrapper(bound, methodName, concreteType);
			}
			else
			{
				MethodSymbol concrete = resolveConcreteTraitMethod(bound, methodName, concreteType);
				slotFns[i] = getOrEmitVtableWrapper(bound, methodName, concreteType, concrete);
			}
		}

		// Build the constant initialiser in-context and derive the global type from it,
		// guaranteeing that the element type and initializer type are identical.
		LLVMValueRef vtableConst = LLVMConstStructInContext(
				context, new PointerPointer<>(slotFns), slotFns.length, 0);
		LLVMTypeRef vtableStructType = LLVMTypeOf(vtableConst);

		LLVMValueRef global = LLVMAddGlobal(module, vtableStructType, new BytePointer(vtableName));
		LLVMSetInitializer(global, vtableConst);
		LLVMSetGlobalConstant(global, 1);
		LLVMSetLinkage(global, LLVMPrivateLinkage);
		return global;
	}

	/**
	 * Resolves the concrete {@link MethodSymbol} for a given trait method on a
	 * concrete type. Checks primitive impl scopes first, then composite member
	 * scopes.
	 */
	private MethodSymbol resolveConcreteTraitMethod(TraitType bound, String methodName, Type concreteType)
	{
		MethodSymbol ms = null;

		if (concreteType instanceof PrimitiveType pt)
		{
			SymbolTable primScope = analyzer.getPrimitiveImplScopes().get(pt);
			if (primScope != null)
			{
				Symbol sym = primScope.resolveLocal(methodName);
				if (sym instanceof MethodSymbol mSym)
					ms = mSym;
			}
		}
		else if (concreteType instanceof CompositeType ct)
		{
			Symbol sym = ct.getMemberScope().resolveLocal(methodName);
			if (sym instanceof MethodSymbol mSym)
				ms = mSym;
		}

		if (ms == null)
			throw new CodegenException("Cannot find concrete implementation of '"
					+ methodName + "' for type '" + concreteType.name()
					+ "' satisfying trait '" + bound.name() + "'");
		return ms;
	}

	/**
	 * Emits (or returns the cached) wrapper function for a vtable slot.
	 *
	 * <p>The wrapper has signature {@code (ptr %self) -> RETURN_TYPE} and calls
	 * the concrete implementation after loading {@code %self} with the concrete
	 * LLVM type.  This boxing is eliminated by LLVM's inliner when the vtable
	 * is a constant.</p>
	 */
	private LLVMValueRef getOrEmitVtableWrapper(
			TraitType bound,
			String methodName,
			Type concreteType,
			MethodSymbol concreteMs)
	{
		String wrapperName = "__nebula_vtwrap_"
				+ concreteType.name().replaceAll("[^a-zA-Z0-9]", "_")
				+ "_" + bound.name() + "_" + methodName;

		LLVMValueRef existing = LLVMGetNamedFunction(module, new BytePointer(wrapperName));
		if (existing != null && !existing.isNull())
			return existing;

		LLVMTypeRef ptrType    = LLVMPointerTypeInContext(context, 0);
		MethodSymbol traitMethod = bound.getRequiredMethods().get(methodName);
		Type         returnType  = traitMethod.getType().getReturnType();
		LLVMTypeRef  retLlvmType = toLLVMType(returnType);

		// Wrapper signature: (ptr %self) -> returnType
		LLVMTypeRef[] wrapperParams = { ptrType };
		LLVMTypeRef   wrapperFnType = LLVMFunctionType(
				retLlvmType, new PointerPointer<>(wrapperParams), 1, 0);
		LLVMValueRef  wrapperFn = LLVMAddFunction(module, new BytePointer(wrapperName), wrapperFnType);
		LLVMSetLinkage(wrapperFn, LLVMPrivateLinkage);

		// Save insertion point
		LLVMBasicBlockRef savedBB = LLVMGetInsertBlock(builder);

		LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(context, wrapperFn, new BytePointer("entry"));
		LLVMPositionBuilderAtEnd(builder, entry);

		LLVMValueRef selfPtr = LLVMGetParam(wrapperFn, 0);

		// For composite types (structs / classes) the concrete method takes `ptr this`
		// and selfPtr already points to the struct data stored in the erased arg box.
		// For primitive types (including str) the concrete method takes the value
		// directly, so we load it from selfPtr.
		LLVMValueRef selfVal;
		if (concreteType instanceof CompositeType)
		{
			selfVal = selfPtr; // pass the pointer to the struct data as `this`
		}
		else
		{
			LLVMTypeRef concreteLlvmType = toLLVMType(concreteType);
			selfVal = LLVMBuildLoad2(builder, concreteLlvmType, selfPtr, new BytePointer("self_val"));
		}

		// Get / forward-declare the concrete function
		String       concreteName   = concreteMs.getMangledName();
		FunctionType concreteFnSem  = concreteMs.getType();
		LLVMTypeRef  concreteFnType = toLLVMType(concreteFnSem);
		LLVMValueRef concreteFn     = LLVMGetNamedFunction(module, new BytePointer(concreteName));
		if (concreteFn == null || concreteFn.isNull())
			concreteFn = LLVMAddFunction(module, new BytePointer(concreteName), concreteFnType);

		// Build the call with the loaded value as the 'this' argument
		LLVMValueRef[] callArgs = { selfVal };
		String callResultName = returnType == PrimitiveType.VOID ? "" : "wrapper_result";
		LLVMValueRef callResult = LLVMBuildCall2(
				builder, concreteFnType, concreteFn,
				new PointerPointer<>(callArgs), 1, callResultName);

		if (returnType == PrimitiveType.VOID)
			LLVMBuildRetVoid(builder);
		else
			LLVMBuildRet(builder, callResult);

		// Restore insertion point
		if (savedBB != null && !savedBB.isNull())
			LLVMPositionBuilderAtEnd(builder, savedBB);

		return wrapperFn;
	}

}
