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
import org.nebula.nebc.semantic.types.*;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer implements ASTVisitor<Type>
{

	private final List<SemanticError> errors = new ArrayList<>();

	// Global/Root scope
	private Scope currentScope = new Scope(null);

	// --- Context Tracking ---
	// Track the expected return type of the method we are currently inside
	private Type currentMethodReturnType = null;
	// Track if we are inside a loop (for break/continue checks - strictness)
	private boolean insideLoop = false;
	// Track the current class/struct we are inside (for 'this' resolution)
	private CompositeType currentTypeDefinition = null;

	public List<SemanticError> analyze(CompilationUnit unit)
	{
		// 1. Initialize Primitives (i32, f64, etc.)
		PrimitiveType.defineAll(currentScope);

		// 2. Visit the AST
		unit.accept(this);

		return errors;
	}

	// --- Utilities ---

	private void error(String msg, ASTNode node)
	{
		// If node is null, we can't get a span, fallback to unknown
		var span = (node != null) ? node.getSpan() : org.nebula.nebc.frontend.diagnostic.SourceSpan.unknown();
		errors.add(new SemanticError(msg, span));
	}

	private void enterScope()
	{
		currentScope = new Scope(currentScope);
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
	 */
	private Type resolveType(TypeNode astType)
	{
		if (astType == null)
			return PrimitiveType.VOID;

		if (astType instanceof NamedType nt)
		{
			Type t = currentScope.resolve(nt.qualifiedName);
			if (t == null)
			{
				error("Unknown type '" + nt.qualifiedName + "'", astType);
				return Type.ERROR;
			}
			return t;
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
		// Pre-pass: We could optionally register all types first to handle
		// out-of-order definitions. For now, we process linearly.
		for (ASTNode decl : node.declarations)
		{
			decl.accept(this);
		}
		return null;
	}

	@Override
	public Type visitNamespaceDeclaration(NamespaceDeclaration node)
	{
		// Resolve or create namespace scope
		NamespaceType nsType;
		Type existing = currentScope.resolve(node.name);

		if (existing instanceof NamespaceType ns)
		{
			nsType = ns;
		}
		else
		{
			nsType = new NamespaceType(node.name, currentScope);
			currentScope.define(node.name, nsType);
		}

		Scope previousScope = currentScope;
		currentScope = nsType.getMemberScope();

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
		return visitCompositeDeclaration(node, new ClassType(node.name, currentScope), node.members);
	}

	@Override
	public Type visitStructDeclaration(StructDeclaration node)
	{
		return visitCompositeDeclaration(node, new StructType(node.name, currentScope), node.members);
	}

	/**
	 * Shared logic for Classes and Structs
	 */
	private Type visitCompositeDeclaration(ASTNode node, CompositeType type, List<Declaration> members)
	{
		// 1. Define Type in current scope
		if (!currentScope.define(type.name(), type))
		{
			error("Type '" + type.name() + "' is already defined.", node);
		}

		// 2. Enter Member Scope
		Scope outerScope = currentScope;
		CompositeType prevTypeDef = currentTypeDefinition;

		currentScope = type.getMemberScope();
		currentTypeDefinition = type;

		// Define 'this'
		currentScope.define("this", type);

		// 3. Visit Members
		for (Declaration member : members)
		{
			member.accept(this);
		}

		// 4. Restore Context
		currentTypeDefinition = prevTypeDef;
		currentScope = outerScope;
		return null;
	}

	@Override
	public Type visitMethodDeclaration(MethodDeclaration node)
	{
		Type returnType = (node.returnType == null) ? PrimitiveType.VOID : resolveType(node.returnType);

		// 1. Build Function Signature
		List<Type> paramTypes = new ArrayList<>();
		for (Parameter p : node.parameters)
		{
			Type pType = resolveType(p.type());
			paramTypes.add(pType == Type.ERROR ? Type.ANY : pType);
		}
		FunctionType methodType = new FunctionType(returnType, paramTypes);

		// 2. Define Method in Current Scope (e.g., inside the Class/Struct scope)
		if (!currentScope.define(node.name, methodType))
		{
			error("Method '" + node.name + "' already defined.", node);
		}

		// 3. Analyze Body
		enterScope();
		Type prevRet = currentMethodReturnType;
		currentMethodReturnType = returnType;

		// Define parameters as local variables
		for (Parameter param : node.parameters)
		{
			Type pType = resolveType(param.type());
			if (!currentScope.define(param.name(), pType))
			{
				error("Duplicate parameter '" + param.name() + "'", node);
			}
			// Check default value type if present
			if (param.defaultValue() != null)
			{
				Type defType = param.defaultValue().accept(this);
				if (!defType.isAssignableTo(pType))
				{
					error("Default value type mismatch. Expected " + pType.name(), param.defaultValue());
				}
			}
		}

		if (node.body != null)
		{
			node.body.accept(this);
			// If body is an expression (fat arrow), we might check semantic compatibility here
			// but the ReturnStatement visitor usually handles void/non-void checks inside blocks.
			// If it's a direct expression body (=> expr), we need to ensure it matches return type.
			if (!(node.body instanceof StatementBlock))
			{
				// Assuming node.body is an Expression if not a block (depending on AST impl)
				// The provided ASTNode body is generic, usually we'd cast.
				// For now, let visitor flow handle it.
			}
		}

		currentMethodReturnType = prevRet;
		exitScope();
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitVariableDeclaration(VariableDeclaration node)
	{
		Type explicitType = node.isVar ? null : resolveType(node.type);

		for (VariableDeclarator decl : node.declarators)
		{
			Type actualType = explicitType;

			if (decl.hasInitializer())
			{
				Type initType = decl.initializer().accept(this);

				if (node.isVar)
				{
					// Type Inference
					actualType = (initType == null || initType == Type.ERROR) ? Type.ERROR : initType;
				}
				else
				{
					// Type Checking
					if (!initType.isAssignableTo(explicitType))
					{
						error("Type mismatch. Expected " + explicitType.name() + ", got " + initType.name(), decl.initializer());
					}
				}
			}
			else if (node.isVar)
			{
				error("Implicit variable '" + decl.name() + "' must be initialized.", node);
				actualType = Type.ERROR;
			}

			if (actualType != Type.ERROR)
			{
				if (!currentScope.define(decl.name(), actualType))
				{
					error("Variable '" + decl.name() + "' already defined.", node);
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
			error("Return statement outside of method.", node);
		}
		else if (!valType.isAssignableTo(currentMethodReturnType))
		{
			error("Return type mismatch. Expected " + currentMethodReturnType.name() + ", got " + valType.name(), node);
		}
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitIfStatement(IfStatement node)
	{
		Type condType = node.condition.accept(this);
		if (condType != PrimitiveType.BOOL && condType != Type.ERROR)
		{
			error("If condition must be boolean, got " + condType.name(), node.condition);
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

		// 1. Initializer (e.g., int i = 0)
		if (node.initializer != null)
			node.initializer.accept(this);

		// 2. Condition
		if (node.condition != null)
		{
			Type cond = node.condition.accept(this);
			if (cond != PrimitiveType.BOOL && cond != Type.ERROR)
			{
				error("For condition must be boolean.", node.condition);
			}
		}

		// 3. Iterators (e.g., i++)
		// CORRECTED: using 'iterators' list, not 'update'
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
		// In a real implementation, checking if iterableType implements Iterable<T> happens here.
		// For now, we assume it's valid or assume ANY.

		Type itemType = Type.ANY; // Should extract T from Iterable<T>

		if (node.variableType != null)
		{
			// Explicit type: foreach(int x in list)
			itemType = resolveType(node.variableType);
		}

		// Define the loop variable
		currentScope.define(node.variableName, itemType);

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

		// Simplified Operator Overloading Logic
		// In a real compiler, we would look for operator methods on 'left'.
		// e.g., left.operator+(right)

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
				// TODO: Check for custom operator overload here
				error("Operator '" + node.operator + "' not defined for " + left.name() + " and " + right.name(), node);
				return Type.ERROR;

			case EQ:
			case NE:
				if (!left.equals(right))
				{
					// Warning usually, unless strict types
					error("Comparing distinct types " + left.name() + " and " + right.name() + " is always false.", node);
				}
				return PrimitiveType.BOOL;

			case LT:
			case GT:
			case LE:
			case GE:
				if (left.equals(right) && isNumeric(left))
					return PrimitiveType.BOOL;
				error("Relational operator requires numeric types.", node);
				return Type.ERROR;

			case LOGICAL_AND:
			case LOGICAL_OR:
				if (left == PrimitiveType.BOOL && right == PrimitiveType.BOOL)
					return PrimitiveType.BOOL;
				error("Logical operators require boolean operands.", node);
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

		// Case A: It's a proper function type (e.g. a variable holding a lambda, or a resolved method)
		if (targetType instanceof FunctionType f)
		{
			fn = f;
		}
		// Case B: It looks like a struct construction? e.g. Vec2(1, 2)
		// In this case, 'targetType' is the StructType itself.
		// We need to find if there is a constructor (which acts like a function) or default init.
		else if (targetType instanceof StructType || targetType instanceof ClassType)
		{
			// For simplicity in this analyzer, we treat the type itself as callable
			// if it implies a constructor call.
			// We'd ideally look up a special "__init__" or similar in the type's scope.
			// Let's assume a generic constructor for now that accepts ANY args (placeholder)
			return targetType;
		}
		else
		{
			error("Expression of type '" + targetType.name() + "' is not callable.", node.target);
			return Type.ERROR;
		}

		// Validate Arguments
		if (fn != null)
		{
			List<Expression> args = node.arguments;
			if (args.size() != fn.parameterTypes.size())
			{
				error("Argument count mismatch. Expected " + fn.parameterTypes.size() + ", got " + args.size(), node);
				return fn.returnType;
			}

			for (int i = 0; i < args.size(); i++)
			{
				Type argType = args.get(i).accept(this);
				Type paramType = fn.parameterTypes.get(i);
				if (!argType.isAssignableTo(paramType))
				{
					error("Argument " + (i + 1) + ": expected " + paramType.name() + ", got " + argType.name(), args.get(i));
				}
			}
			return fn.returnType;
		}

		return Type.ANY;
	}

	@Override
	public Type visitMemberAccessExpression(MemberAccessExpression node)
	{
		// 1. Resolve object: e.g., 'obj' in 'obj.field'
		Type objectType = node.target.accept(this);
		if (objectType == Type.ERROR)
			return Type.ERROR;

		// 2. Get the scope of that object
		Scope memberScope = null;
		if (objectType instanceof CompositeType ct)
		{
			memberScope = ct.getMemberScope();
		}
		else if (objectType instanceof NamespaceType nt)
		{
			memberScope = nt.getMemberScope();
		}
		else
		{
			// Primitives don't have members in this simple pass (unless we add extension methods later)
			error("Type '" + objectType.name() + "' does not have members.", node.target);
			return Type.ERROR;
		}

		// 3. Resolve member
		Type memberType = memberScope.resolve(node.memberName);
		if (memberType == null)
		{
			error("Member '" + node.memberName + "' not found in " + objectType.name(), node);
			return Type.ERROR;
		}

		return memberType;
	}

	@Override
	public Type visitIdentifierExpression(IdentifierExpression node)
	{
		Type t = currentScope.resolve(node.name);
		if (t == null)
		{
			error("Undefined symbol '" + node.name + "'", node);
			return Type.ERROR;
		}
		return t;
	}

	@Override
	public Type visitNewExpression(NewExpression node)
	{
		// node.typeName is a String. In a real AST it should ideally be a TypeNode.
		Type t = currentScope.resolve(node.typeName);
		if (t == null)
		{
			error("Unknown type '" + node.typeName + "'", node);
			return Type.ERROR;
		}

		// TODO: Validate constructor arguments against t's constructor
		// For now, iterate args to ensure they are valid expressions
		for (Expression arg : node.arguments)
		{
			arg.accept(this);
		}

		return t;
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
			error("Cannot assign " + valueType.name() + " to " + targetType.name(), node);
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
					error("Operator '!' requires boolean operand.", node);
					yield Type.ERROR;
				}
				yield PrimitiveType.BOOL;
			}
			case MINUS, PLUS ->
			{
				if (!isNumeric(operand))
				{
					error("Unary math requires numeric operand.", node);
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
		return node.getType() != null ? node.getType() : Type.ERROR;
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
		// This is a simplification. A real compiler handles type promotion (i32 -> i64).
		return t == PrimitiveType.I32 || t == PrimitiveType.F64 ||
				t == PrimitiveType.I64 || t == PrimitiveType.F32 ||
				t == PrimitiveType.U8 || t == PrimitiveType.I8; // etc
	}

	// --- Stubs for unused features to fulfill ASTVisitor interface ---
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
	} // TODO: Implement Match logic

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
			error("'this' used outside of class/struct.", node);
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