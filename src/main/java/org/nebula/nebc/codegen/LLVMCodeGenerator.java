package org.nebula.nebc.codegen;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.llvm.LLVM.*;
import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.CompilationUnit;
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
import org.nebula.nebc.semantic.types.FunctionType;
import org.nebula.nebc.semantic.types.PrimitiveType;
import org.nebula.nebc.semantic.types.Type;

import java.util.List;

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

	/**
	 * Whether the current basic block has already been terminated (ret/br).
	 */
	private boolean currentBlockTerminated;

	// =================================================================
	// PUBLIC API
	// =================================================================

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

		// 1. Initialise LLVM infrastructure
		context = LLVMContextCreate();
		module = LLVMModuleCreateWithNameInContext("nebula_module", context);
		builder = LLVMCreateBuilderInContext(context);

		// 2. Visit every compilation unit
		for (CompilationUnit cu : units)
		{
			cu.accept(this);
		}

		// 3. Emit the C-ABI 'main' entry-point wrapper
		emitEntryPoint();

		// 4. Verify the module
		verifyModule();

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
	// ENTRY POINT WRAPPER
	// =================================================================

	/**
	 * Emits a C-ABI-compatible {@code main} function that calls the user's
	 * Nebula {@code main} (renamed to {@code __nebula_main}).
	 * <ul>
	 * <li>{@code i32 main()} → wraps with {@code ret i32 call __nebula_main()}</li>
	 * <li>{@code void main()} → calls {@code __nebula_main()}, then
	 * {@code ret i32 0}</li>
	 * </ul>
	 */
	private void emitEntryPoint()
	{
		MethodDeclaration mainDecl = analyzer.getMainMethod();
		if (mainDecl == null)
		{
			// Library mode or no entry point — nothing to wrap
			return;
		}

		Type returnType = analyzer.getMainMethodReturnType();
		LLVMTypeRef i32Type = LLVMInt32TypeInContext(context);

		// Build: i32 @main()
		LLVMTypeRef mainFnType = LLVMFunctionType(i32Type, new LLVMTypeRef(), 0, 0);
		LLVMValueRef mainFn = LLVMAddFunction(module, "main", mainFnType);

		LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(context, mainFn, "entry");
		LLVMPositionBuilderAtEnd(builder, entry);

		// Call the user's __nebula_main
		LLVMValueRef nebulaMain = LLVMGetNamedFunction(module, "__nebula_main");
		if (nebulaMain == null || nebulaMain.isNull())
		{
			throw new CodegenException("Internal error: __nebula_main not found in module.");
		}

		// Build the function type for __nebula_main: either () -> i32 or () -> void
		LLVMTypeRef nebulaRetType = toLLVMType(returnType);
		LLVMTypeRef nebulaMainType = LLVMFunctionType(nebulaRetType, new LLVMTypeRef(), 0, 0);

		if (returnType == PrimitiveType.VOID)
		{
			// void main → call, then implicit return 0
			LLVMBuildCall2(builder, nebulaMainType, nebulaMain,
					new LLVMValueRef(), 0, new BytePointer(""));
			LLVMBuildRet(builder, LLVMConstInt(i32Type, 0, 0));
		}
		else
		{
			// i32 main → call and return the result
			LLVMValueRef callResult = LLVMBuildCall2(builder, nebulaMainType, nebulaMain,
					new LLVMValueRef(), 0, new BytePointer("call"));
			LLVMBuildRet(builder, callResult);
		}
	}
	// =================================================================
	// MODULE VERIFICATION
	// =================================================================

	private void verifyModule()
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
	 */
	private LLVMTypeRef toLLVMType(Type type)
	{
		return LLVMTypeMapper.map(context, type);
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

		// 2. Extract types directly from the symbol
		FunctionType funcType = symbol.getType();
		Type returnType = funcType.getReturnType();
		LLVMTypeRef llvmFuncType = toLLVMType(funcType);

		// 3. Determine function name (handling main wrapper)
		String funcName = "main".equals(node.name) ? "__nebula_main" : node.name;

		// 4. Add the function to the module
		LLVMValueRef function = LLVMAddFunction(module, funcName, llvmFuncType);
		LLVMSetLinkage(function, LLVMExternalLinkage);

		// 5. Setup Entry Block
		LLVMBasicBlockRef entryBB = LLVMAppendBasicBlockInContext(context, function, "entry");
		LLVMPositionBuilderAtEnd(builder, entryBB);

		// 6. Manage Codegen State
		LLVMValueRef prevFunction = currentFunction;
		boolean prevTerminated = currentBlockTerminated;
		currentFunction = function;
		currentBlockTerminated = false;

		// 7. Emit Body
		if (node.body != null)
		{
			node.body.accept(this);
		}

		// 8. Handle Implicit Returns (Now using the verified returnType object)
		if (!currentBlockTerminated)
		{
			if (returnType == PrimitiveType.VOID)
			{
				LLVMBuildRetVoid(builder);
			}
			else if (returnType == PrimitiveType.I32)
			{
				// Robust check: we are comparing Type objects now, not Strings
				LLVMBuildRet(builder, LLVMConstInt(toLLVMType(PrimitiveType.I32), 2, 0));
			}
			else
			{
				LLVMBuildRet(builder, LLVMGetUndef(toLLVMType(returnType)));
			}
		}

		// 9. Restore State
		currentFunction = prevFunction;
		currentBlockTerminated = prevTerminated;

		return function;
	}

	@Override
	public LLVMValueRef visitClassDeclaration(ClassDeclaration node)
	{
		// TODO: Emit struct type and method definitions for classes
		return null;
	}

	@Override
	public LLVMValueRef visitStructDeclaration(StructDeclaration node)
	{
		// TODO: Emit struct type definition
		return null;
	}

	@Override
	public LLVMValueRef visitVariableDeclaration(VariableDeclaration node)
	{
		// TODO: Emit alloca + optional store for local variables
		return null;
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
		// TODO: Traits are a semantic-only concept for now
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
		// TODO: Emit constructor function
		return null;
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
				LLVMBuildRet(builder, value);
			}
			else
			{
				// Fallback: the expression didn't produce a value (stub)
				LLVMBuildRetVoid(builder);
			}
		}
		currentBlockTerminated = true;
		return null;
	}

	@Override
	public LLVMValueRef visitIfStatement(IfStatement node)
	{
		// TODO: Emit conditional branching (br i1 cond, then_bb, else_bb)
		return null;
	}

	@Override
	public LLVMValueRef visitForStatement(ForStatement node)
	{
		// TODO: Emit loop control flow (header, body, latch, exit blocks)
		return null;
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
		return switch (node.type)
		{
			case INT ->
			{
				long val = ((Number) node.value).longValue();
				// INT literals are i64 per the semantic analyzer
				yield LLVMConstInt(LLVMInt64TypeInContext(context), val, /* signExtend */ 1);
			}
			case FLOAT ->
			{
				double val = ((Number) node.value).doubleValue();
				yield LLVMConstReal(LLVMDoubleTypeInContext(context), val);
			}
			case BOOL ->
			{
				boolean val = (Boolean) node.value;
				yield LLVMConstInt(LLVMInt1TypeInContext(context), val ? 1 : 0, 0);
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
				yield LLVMConstInt(LLVMInt32TypeInContext(context), codePoint, 0);
			}
			case STRING ->
			{
				// String → global constant + pointer
				String str = node.value.toString();
				yield LLVMBuildGlobalStringPtr(builder, str, ".str");
			}
		};
	}

	@Override
	public LLVMValueRef visitBinaryExpression(BinaryExpression node)
	{
		// TODO: Emit binary operations (add, sub, mul, div, comparisons, logical)
		return null;
	}

	@Override
	public LLVMValueRef visitUnaryExpression(UnaryExpression node)
	{
		// TODO: Emit unary operations (neg, not, pre/post increment/decrement)
		return null;
	}

	@Override
	public LLVMValueRef visitAssignmentExpression(AssignmentExpression node)
	{
		// TODO: Emit store to alloca'd variable
		return null;
	}

	@Override
	public LLVMValueRef visitCastExpression(CastExpression node)
	{
		// TODO: Emit type cast (trunc, zext, sext, fpcast, bitcast, etc.)
		return null;
	}

	@Override
	public LLVMValueRef visitExpressionBlock(ExpressionBlock node)
	{
		// TODO: Emit block expression (statements + optional tail expression)
		return null;
	}

	@Override
	public LLVMValueRef visitIdentifierExpression(IdentifierExpression node)
	{
		// TODO: Emit load from alloca'd variable
		return null;
	}

	@Override
	public LLVMValueRef visitInvocationExpression(InvocationExpression node)
	{
		// TODO: Emit function call
		return null;
	}

	@Override
	public LLVMValueRef visitMemberAccessExpression(MemberAccessExpression node)
	{
		// TODO: Emit GEP (GetElementPtr) for member access
		return null;
	}

	@Override
	public LLVMValueRef visitNewExpression(NewExpression node)
	{
		// TODO: Emit heap allocation + constructor call
		return null;
	}

	@Override
	public LLVMValueRef visitIndexExpression(IndexExpression node)
	{
		// TODO: Emit GEP for array/map indexing
		return null;
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
		// TODO: Emit load of 'this' pointer from first function parameter
		return null;
	}

	@Override
	public LLVMValueRef visitStringInterpolationExpression(StringInterpolationExpression node)
	{
		// TODO: Emit sprintf-like string building
		return null;
	}

	@Override
	public LLVMValueRef visitIfExpression(IfExpression node)
	{
		// TODO: Emit phi-based if expression
		return null;
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
