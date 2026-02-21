package org.nebula.nebc.semantic;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.CompilationUnit;
import org.nebula.nebc.ast.Parameter;
import org.nebula.nebc.ast.declarations.*;
import org.nebula.nebc.ast.expressions.*;
import org.nebula.nebc.ast.patterns.*;
import org.nebula.nebc.ast.statements.*;
import org.nebula.nebc.ast.tags.TagAtom;
import org.nebula.nebc.ast.tags.TagOperation;
import org.nebula.nebc.ast.tags.TagStatement;
import org.nebula.nebc.ast.types.NamedType;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.core.CompilerConfig;
import org.nebula.nebc.frontend.diagnostic.Diagnostic;
import org.nebula.nebc.frontend.diagnostic.DiagnosticCode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;
import org.nebula.nebc.semantic.symbol.*;
import org.nebula.nebc.semantic.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticAnalyzer implements ASTVisitor<Type>
{

	private final List<Diagnostic> errors = new ArrayList<>();

	// Symbol table (replaces old Scope)
	private final SymbolTable globalScope = new SymbolTable(null);
	private final Map<ASTNode, Symbol> nodeSymbols = new HashMap<>();
	private final CompilerConfig config;
	private SymbolTable currentScope = globalScope;
	private MethodDeclaration mainMethod = null;
	private Type mainMethodReturnType = null;
	// --- Context Tracking ---
	private Type currentMethodReturnType = null;
	private boolean insideLoop = false;
	private CompositeType currentTypeDefinition = null;

	public SemanticAnalyzer(CompilerConfig config)
	{
		this.config = config;
	}

	public List<Diagnostic> analyze(CompilationUnit unit)
	{
		// 1. Initialize built-in primitive types as TypeSymbols
		PrimitiveType.defineAll(currentScope);

		// 2. Visit the AST
		unit.accept(this);

		return errors;
	}

	/**
	 * Associates a symbol with an AST node.
	 * Called during analysis (e.g., when visiting a MethodDeclaration).
	 */
	private void recordSymbol(ASTNode node, Symbol symbol)
	{
		nodeSymbols.put(node, symbol);
	}

	/**
	 * Helper for the CodeGen to retrieve resolved metadata.
	 */
	public <T extends Symbol> T getSymbol(ASTNode node, Class<T> type)
	{
		Symbol sym = nodeSymbols.get(node);
		if (sym == null)
			return null;
		return type.isInstance(sym) ? type.cast(sym) : null;
	}

	/**
	 * Returns the AST node of the validated 'main' method, or null if none was
	 * found.
	 */
	public MethodDeclaration getMainMethod()
	{
		return mainMethod;
	}

	/**
	 * Returns the resolved return type of the 'main' method (i32 or void), or null.
	 */
	public Type getMainMethodReturnType()
	{
		return mainMethodReturnType;
	}

	// --- Utilities ---

	private void error(DiagnosticCode code, ASTNode node, Object... args)
	{
		var span = (node != null) ? node.getSpan() : SourceSpan.unknown();
		errors.add(Diagnostic.of(code, span, args));
	}

	private void enterScope()
	{
		currentScope = new SymbolTable(currentScope);
	}

	private void exitScope()
	{
		if (currentScope.getParent() != null)
		{
			currentScope = currentScope.getParent();
		}
	}

	/**
	 * Resolves a syntactic AST TypeNode to a semantic Type object.
	 * Uses the symbol table to find TypeSymbols specifically.
	 */
	private Type resolveType(TypeNode astType)
	{
		if (astType == null)
			return PrimitiveType.VOID;

		if (astType instanceof NamedType nt)
		{
			TypeSymbol ts = currentScope.resolveType(nt.qualifiedName);
			if (ts == null)
			{
				error(DiagnosticCode.UNKNOWN_TYPE, astType, nt.qualifiedName);
				return Type.ERROR;
			}
			return ts.getType();
		}
		// TODO: Handle ArrayType and TupleType recursion here
		return Type.ANY;
	}

	// =================================================================
	// DECLARATIONS
	// =================================================================

	@Override
	public Type visitCompilationUnit(CompilationUnit node)
	{
		// Phase 1: Forward-declare all top-level type names.
		// This allows classes/structs to reference each other regardless of order.
		for (ASTNode decl : node.declarations)
		{
			if (decl instanceof ClassDeclaration cd)
			{
				ClassType classType = new ClassType(cd.name, currentScope);
				TypeSymbol sym = new TypeSymbol(cd.name, classType, cd);
				if (!currentScope.define(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, cd, cd.name);
				}
			}
			else if (decl instanceof StructDeclaration sd)
			{
				StructType structType = new StructType(sd.name, currentScope);
				TypeSymbol sym = new TypeSymbol(sd.name, structType, sd);
				if (!currentScope.define(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, sd, sd.name);
				}
			}
			// TODO: Forward-declare unions, traits, etc.
		}

		// Phase 2: Full visitation — resolve bodies, check types.
		for (ASTNode decl : node.declarations)
		{
			decl.accept(this);
		}
		return null;
	}

	@Override
	public Type visitNamespaceDeclaration(NamespaceDeclaration node)
	{
		// Resolve or create namespace
		NamespaceSymbol nsSym;
		Symbol existing = currentScope.resolve(node.name);

		if (existing instanceof NamespaceSymbol ns)
		{
			nsSym = ns;
		}
		else
		{
			SymbolTable nsTable = new SymbolTable(currentScope);
			nsSym = new NamespaceSymbol(node.name, nsTable, node);
			currentScope.define(nsSym);
		}

		SymbolTable previousScope = currentScope;
		currentScope = nsSym.getMemberTable();

		for (ASTNode member : node.members)
		{
			member.accept(this);
		}

		// Only restore scope if it's a block declaration.
		// File-scoped namespaces (namespace foo;) stay active for the file.
		if (node.isBlockDeclaration)
		{
			currentScope = previousScope;
		}

		return null;
	}

	@Override
	public Type visitClassDeclaration(ClassDeclaration node)
	{
		// The TypeSymbol was already forward-declared in Phase 1.
		// Look it up and populate its member scope.
		TypeSymbol existingSym = currentScope.resolveType(node.name);
		if (existingSym == null)
		{
			error(DiagnosticCode.INTERNAL_ERROR, node, "class '" + node.name + "' was not forward-declared.");
			return null;
		}
		ClassType classType = (ClassType) existingSym.getType();
		return visitCompositeBody(node, classType, node.members);
	}

	@Override
	public Type visitStructDeclaration(StructDeclaration node)
	{
		// The TypeSymbol was already forward-declared in Phase 1.
		TypeSymbol existingSym = currentScope.resolveType(node.name);
		if (existingSym == null)
		{
			error(DiagnosticCode.INTERNAL_ERROR, node, "struct '" + node.name + "' was not forward-declared.");
			return null;
		}
		StructType structType = (StructType) existingSym.getType();
		return visitCompositeBody(node, structType, node.members);
	}

	/**
	 * Shared logic for populating the member scope of Classes and Structs.
	 */
	private Type visitCompositeBody(ASTNode node, CompositeType type, List<Declaration> members)
	{
		// Enter member scope
		SymbolTable outerScope = currentScope;
		CompositeType prevTypeDef = currentTypeDefinition;

		currentScope = type.getMemberScope();
		currentTypeDefinition = type;

		// Define 'this' as a variable symbol pointing to the type
		currentScope.define(new VariableSymbol("this", type, false, node));

		// Visit members
		for (Declaration member : members)
		{
			member.accept(this);
		}

		// Restore context
		currentTypeDefinition = prevTypeDef;
		currentScope = outerScope;
		return null;
	}

	@Override
	public Type visitMethodDeclaration(MethodDeclaration node)
	{
		Type returnType = (node.returnType == null) ? PrimitiveType.VOID : resolveType(node.returnType);

		// 1. Build function signature
		List<Type> paramTypes = new ArrayList<>();
		for (Parameter p : node.parameters)
		{
			Type pType = resolveType(p.type());
			paramTypes.add(pType == Type.ERROR ? Type.ANY : pType);
		}
		FunctionType methodType = new FunctionType(returnType, paramTypes);

		// 2. Define method as a MethodSymbol in current scope
		MethodSymbol methodSym = new MethodSymbol(node.name, methodType, node.modifiers, node);
		recordSymbol(node, methodSym);
		if (!currentScope.define(methodSym))
		{
			error(DiagnosticCode.DUPLICATE_SYMBOL, node, node.name);
		}

		// 3. Check for entry point
		if ("main".equals(node.name) && currentTypeDefinition == null)
		{
			if (mainMethod != null)
			{
				error(DiagnosticCode.DUPLICATE_MAIN_METHOD, node);
			}
			else
			{
				if (returnType != PrimitiveType.I32 && returnType != PrimitiveType.VOID)
				{
					error(DiagnosticCode.INVALID_MAIN_SIGNATURE, node);
				}
				if (!node.parameters.isEmpty())
				{
					error(DiagnosticCode.INVALID_MAIN_SIGNATURE, node);
				}
				mainMethod = node;
				mainMethodReturnType = returnType;
			}
		}

		// 3. Analyze body
		enterScope();
		Type prevRet = currentMethodReturnType;
		currentMethodReturnType = returnType;

		// Define parameters as variable symbols
		for (Parameter param : node.parameters)
		{
			Type pType = resolveType(param.type());
			VariableSymbol paramSym = new VariableSymbol(param.name(), pType, false, node);
			if (!currentScope.define(paramSym))
			{
				error(DiagnosticCode.DUPLICATE_PARAMETER, node, param.name());
			}
			// Check default value type if present
			if (param.defaultValue() != null)
			{
				Type defType = param.defaultValue().accept(this);
				if (!defType.isAssignableTo(pType))
				{
					error(DiagnosticCode.TYPE_MISMATCH, param.defaultValue(), pType.name(), defType.name());
				}
			}
		}

		if (node.body != null)
		{
			node.body.accept(this);
		}

		currentMethodReturnType = prevRet;
		exitScope();
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitVariableDeclaration(VariableDeclaration node)
	{
		Type explicitType = node.isVar ? null : resolveType(node.type);
		boolean mutable = node.isVar; // var = mutable, explicit type = immutable by default

		for (VariableDeclarator decl : node.declarators)
		{
			Type actualType = explicitType;

			if (decl.hasInitializer())
			{
				Type initType = decl.initializer().accept(this);

				if (node.isVar)
				{
					// Type inference
					actualType = (initType == null || initType == Type.ERROR) ? Type.ERROR : initType;
				}
				else
				{
					// Type checking
					if (!initType.isAssignableTo(explicitType))
					{
						error(DiagnosticCode.TYPE_MISMATCH, decl.initializer(), explicitType.name(), initType.name());
					}
				}
			}
			else if (node.isVar)
			{
				error(DiagnosticCode.UNINITIALIZED_VARIABLE, node, decl.name());
				actualType = Type.ERROR;
			}

			if (actualType != Type.ERROR)
			{
				VariableSymbol varSym = new VariableSymbol(decl.name(), actualType, mutable, node);
				if (!currentScope.define(varSym))
				{
					error(DiagnosticCode.DUPLICATE_SYMBOL, node, decl.name());
				}
			}
		}
		return PrimitiveType.VOID;
	}

	// =================================================================
	// STATEMENTS
	// =================================================================

	@Override
	public Type visitStatementBlock(StatementBlock node)
	{
		enterScope();
		for (ASTNode stmt : node.statements)
		{
			stmt.accept(this);
		}
		exitScope();
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitReturnStatement(ReturnStatement node)
	{
		Type valType = (node.value == null) ? PrimitiveType.VOID : node.value.accept(this);

		if (currentMethodReturnType == null)
		{
			error(DiagnosticCode.RETURN_OUTSIDE_METHOD, node);
		}
		else if (!valType.isAssignableTo(currentMethodReturnType))
		{
			error(DiagnosticCode.TYPE_MISMATCH, node, currentMethodReturnType.name(), valType.name());
		}
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitIfStatement(IfStatement node)
	{
		Type condType = node.condition.accept(this);
		if (condType != PrimitiveType.BOOL && condType != Type.ERROR)
		{
			error(DiagnosticCode.IF_CONDITION_NOT_BOOL, node.condition, condType.name());
		}
		node.thenBranch.accept(this);
		if (node.elseBranch != null)
		{
			node.elseBranch.accept(this);
		}
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitForStatement(ForStatement node)
	{
		enterScope();

		if (node.initializer != null)
			node.initializer.accept(this);

		if (node.condition != null)
		{
			Type cond = node.condition.accept(this);
			if (cond != PrimitiveType.BOOL && cond != Type.ERROR)
			{
				error(DiagnosticCode.FOR_CONDITION_NOT_BOOL, node.condition, cond.name());
			}
		}

		if (node.iterators != null)
		{
			for (Expression expr : node.iterators)
			{
				expr.accept(this);
			}
		}

		boolean prevLoop = insideLoop;
		insideLoop = true;
		node.body.accept(this);
		insideLoop = prevLoop;

		exitScope();
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitForeachStatement(ForeachStatement node)
	{
		enterScope();

		Type iterableType = node.iterable.accept(this);
		// In a real implementation, checking if iterableType implements Iterable<T>
		Type itemType = Type.ANY; // Should extract T from Iterable<T>

		if (node.variableType != null)
		{
			itemType = resolveType(node.variableType);
		}

		// Define the loop variable as a VariableSymbol
		VariableSymbol loopVar = new VariableSymbol(node.variableName, itemType, false, node);
		currentScope.define(loopVar);

		boolean prevLoop = insideLoop;
		insideLoop = true;
		node.body.accept(this);
		insideLoop = prevLoop;

		exitScope();
		return PrimitiveType.VOID;
	}

	// =================================================================
	// EXPRESSIONS
	// =================================================================

	@Override
	public Type visitBinaryExpression(BinaryExpression node)
	{
		Type left = node.left.accept(this);
		Type right = node.right.accept(this);

		if (left == Type.ERROR || right == Type.ERROR)
			return Type.ERROR;

		switch (node.operator)
		{
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:
			case POW:
				if (left.equals(right) && isNumeric(left))
					return left;
				error(DiagnosticCode.OPERATOR_NOT_DEFINED, node, node.operator, left.name(), right.name());
				return Type.ERROR;

			case EQ:
			case NE:
				if (!left.equals(right))
				{
					error(DiagnosticCode.COMPARING_DISTINCT_TYPES, node, left.name(), right.name());
				}
				return PrimitiveType.BOOL;

			case LT:
			case GT:
			case LE:
			case GE:
				if (left.equals(right) && isNumeric(left))
					return PrimitiveType.BOOL;
				error(DiagnosticCode.RELATIONAL_NUMERIC, node);
				return Type.ERROR;

			case LOGICAL_AND:
			case LOGICAL_OR:
				if (left == PrimitiveType.BOOL && right == PrimitiveType.BOOL)
					return PrimitiveType.BOOL;
				error(DiagnosticCode.LOGICAL_BOOLEAN, node);
				return Type.ERROR;

			default:
				return Type.ERROR;
		}
	}

	@Override
	public Type visitInvocationExpression(InvocationExpression node)
	{
		Type targetType = node.target.accept(this);
		if (targetType == Type.ERROR)
			return Type.ERROR;

		FunctionType fn = null;

		if (targetType instanceof FunctionType f)
		{
			fn = f;
		}
		else if (targetType instanceof StructType || targetType instanceof ClassType)
		{
			// Constructor call — return the type itself
			// TODO: Validate constructor arguments against constructors in the type's scope
			return targetType;
		}
		else
		{
			error(DiagnosticCode.NOT_CALLABLE, node.target, targetType.name());
			return Type.ERROR;
		}

		// Validate arguments
		if (fn != null)
		{
			List<Expression> args = node.arguments;
			if (args.size() != fn.parameterTypes.size())
			{
				error(DiagnosticCode.ARGUMENT_COUNT_MISMATCH, node, fn.parameterTypes.size(), args.size());
				return fn.returnType;
			}

			for (int i = 0; i < args.size(); i++)
			{
				Type argType = args.get(i).accept(this);
				Type paramType = fn.parameterTypes.get(i);
				if (!argType.isAssignableTo(paramType))
				{
					error(DiagnosticCode.ARGUMENT_TYPE_MISMATCH, args.get(i), (i + 1), paramType.name(), argType.name());
				}
			}
			return fn.returnType;
		}

		return Type.ANY;
	}

	@Override
	public Type visitMemberAccessExpression(MemberAccessExpression node)
	{
		Type objectType = node.target.accept(this);
		if (objectType == Type.ERROR)
			return Type.ERROR;

		SymbolTable memberScope = null;
		if (objectType instanceof CompositeType ct)
		{
			memberScope = ct.getMemberScope();
		}
		else if (objectType instanceof NamespaceType nt)
		{
			memberScope = nt.getMemberScope();
			error(DiagnosticCode.NO_MEMBERS, node.target, objectType.name());
			return Type.ERROR;
		}

		// Resolve member as a Symbol, return its type
		Symbol memberSym = memberScope.resolve(node.memberName);
		if (memberSym == null)
		{
			error(DiagnosticCode.MEMBER_NOT_FOUND, node, node.memberName, objectType.name());
			return Type.ERROR;
		}

		return memberSym.getType();
	}

	@Override
	public Type visitIdentifierExpression(IdentifierExpression node)
	{
		Symbol sym = currentScope.resolve(node.name);
		if (sym == null)
		{
			error(DiagnosticCode.UNDEFINED_SYMBOL, node, node.name);
			return Type.ERROR;
		}
		return sym.getType();
	}

	@Override
	public Type visitNewExpression(NewExpression node)
	{
		TypeSymbol ts = currentScope.resolveType(node.typeName);
		if (ts == null)
		{
			error(DiagnosticCode.UNKNOWN_TYPE, node, node.typeName);
			return Type.ERROR;
		}

		// TODO: Validate constructor arguments against the type's constructors
		for (Expression arg : node.arguments)
		{
			arg.accept(this);
		}

		return ts.getType();
	}

	@Override
	public Type visitAssignmentExpression(AssignmentExpression node)
	{
		Type targetType = node.target.accept(this);
		Type valueType = node.value.accept(this);

		if (targetType == Type.ERROR || valueType == Type.ERROR)
			return Type.ERROR;

		if (!valueType.isAssignableTo(targetType))
		{
			error(DiagnosticCode.TYPE_MISMATCH, node, targetType.name(), valueType.name());
			return Type.ERROR;
		}
		return targetType;
	}

	@Override
	public Type visitUnaryExpression(UnaryExpression node)
	{
		Type operand = node.operand.accept(this);
		if (operand == Type.ERROR)
			return Type.ERROR;

		return switch (node.operator)
		{
			case NOT ->
			{
				if (operand != PrimitiveType.BOOL)
				{
					error(DiagnosticCode.UNARY_NOT_BOOLEAN, node, operand.name());
					yield Type.ERROR;
				}
				yield PrimitiveType.BOOL;
			}
			case MINUS, PLUS ->
			{
				if (!isNumeric(operand))
				{
					error(DiagnosticCode.UNARY_MATH_NUMERIC, node, operand.name());
					yield Type.ERROR;
				}
				yield operand;
			}
			default ->
					operand;
		};
	}

	@Override
	public Type visitLiteralExpression(LiteralExpression node)
	{
		// Type resolution lives in the analyzer, not in the AST node
		return switch (node.type)
		{
			case INT ->
					PrimitiveType.I64;
			case FLOAT ->
					PrimitiveType.F64;
			case BOOL ->
					PrimitiveType.BOOL;
			case CHAR ->
					PrimitiveType.CHAR;
			case STRING ->
					PrimitiveType.STRING;
		};
	}

	@Override
	public Type visitExpressionBlock(ExpressionBlock node)
	{
		enterScope();
		for (Statement stmt : node.statements)
		{
			stmt.accept(this);
		}

		Type resultType = PrimitiveType.VOID;
		if (node.hasTail())
		{
			resultType = node.tail.accept(this);
		}
		exitScope();
		return resultType;
	}

	// --- Helpers ---

	private boolean isNumeric(Type t)
	{
		return t == PrimitiveType.I32 || t == PrimitiveType.F64 ||
				t == PrimitiveType.I64 || t == PrimitiveType.F32 ||
				t == PrimitiveType.U8 || t == PrimitiveType.I8;
	}

	// --- Stubs for features not yet fully implemented ---

	@Override
	public Type visitExpressionStatement(ExpressionStatement node)
	{
		return node.expression.accept(this);
	}

	@Override
	public Type visitCastExpression(CastExpression node)
	{
		return resolveType(node.targetType);
	}

	@Override
	public Type visitMatchExpression(MatchExpression node)
	{
		return Type.ANY;
	}

	@Override
	public Type visitIfExpression(IfExpression node)
	{
		return Type.ANY;
	}

	@Override
	public Type visitIndexExpression(IndexExpression node)
	{
		return Type.ANY;
	}

	@Override
	public Type visitArrayLiteralExpression(ArrayLiteralExpression node)
	{
		return Type.ANY;
	}

	@Override
	public Type visitTupleLiteralExpression(TupleLiteralExpression node)
	{
		return Type.ANY;
	}

	@Override
	public Type visitThisExpression(ThisExpression node)
	{
		if (currentTypeDefinition == null)
		{
			error(DiagnosticCode.RETURN_OUTSIDE_METHOD, node);
			return Type.ERROR;
		}
		return currentTypeDefinition;
	}

	@Override
	public Type visitStringInterpolationExpression(StringInterpolationExpression node)
	{
		return PrimitiveType.STRING;
	}

	@Override
	public Type visitMatchArm(MatchArm node)
	{
		return null;
	}

	@Override
	public Type visitLiteralPattern(LiteralPattern node)
	{
		return null;
	}

	@Override
	public Type visitTypePattern(TypePattern node)
	{
		return null;
	}

	@Override
	public Type visitWildcardPattern(WildcardPattern node)
	{
		return null;
	}

	@Override
	public Type visitOrPattern(OrPattern node)
	{
		return null;
	}

	@Override
	public Type visitTagAtom(TagAtom node)
	{
		return null;
	}

	@Override
	public Type visitTagOperation(TagOperation node)
	{
		return null;
	}

	@Override
	public Type visitTypeReference(TypeNode node)
	{
		return resolveType(node);
	}

	@Override
	public Type visitTraitDeclaration(TraitDeclaration node)
	{
		return null;
	}

	@Override
	public Type visitUnionDeclaration(UnionDeclaration node)
	{
		return null;
	}

	@Override
	public Type visitUnionVariant(UnionVariant node)
	{
		return null;
	}

	@Override
	public Type visitOperatorDeclaration(OperatorDeclaration node)
	{
		return null;
	}

	@Override
	public Type visitConstructorDeclaration(ConstructorDeclaration node)
	{
		return null;
	}

	@Override
	public Type visitTagStatement(TagStatement node)
	{
		return null;
	}

	@Override
	public Type visitUseStatement(UseStatement node)
	{
		return null;
	}

	@Override
	public Type visitConstDeclaration(ConstDeclaration node)
	{
		return null;
	}
}