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
import org.nebula.nebc.semantic.types.ClassType;
import org.nebula.nebc.semantic.types.Type;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer implements ASTVisitor<Type>
{

	private final List<SemanticError> errors = new ArrayList<>();
	private Scope currentScope = new Scope(null); // Global Scope

	// Context tracking (are we inside a loop? a class?)
	private Type currentMethodReturnType = null;

	public List<SemanticError> analyze(CompilationUnit unit)
	{
		// Pre-define primitives in global scope
		currentScope.define("i32", Type.I32);
		currentScope.define("string", Type.STRING);
		currentScope.define("bool", Type.BOOL);
		currentScope.define("void", Type.VOID);

		unit.accept(this);
		return errors;
	}

	// --- Helpers ---
	private void error(String msg, ASTNode node)
	{
		errors.add(new SemanticError(msg, node.getSpan()));
	}

	private void enterScope()
	{
		currentScope = new Scope(currentScope);
	}

	private void exitScope()
	{
		currentScope = currentScope.getParent();
	}

	/**
	 * Converts AST Type nodes (syntax) into Type objects (logic).
	 */
	private Type resolveType(TypeNode astType)
	{
		if (astType == null)
			return Type.VOID; // Should handle 'var' logic elsewhere

		// This is where you connect 'Int' (string in AST) to Type.INT (object)
		if (astType instanceof NamedType nt)
		{
			Type t = currentScope.resolve(nt.qualifiedName);
			if (t == null)
			{
				error("Unknown type '" + nt.qualifiedName + "'", astType);
				return Type.ERROR;
			}
			return t; // In a real language, check if 't' is actually a type definition, not a variable!
		}

		// TODO: Handle ArrayType, TupleType recursively
		return Type.ANY;
	}

	// =================================================================
	// VISITOR IMPLEMENTATION
	// =================================================================

	@Override
	public Type visitCompilationUnit(CompilationUnit node)
	{
		for (ASTNode decl : node.declarations)
		{
			decl.accept(this);
		}
		return Type.VOID;
	}

	/**
	 * Namespace: Creates a logical scope for its members.
	 */
	@Override
	public Type visitNamespaceDeclaration(NamespaceDeclaration node)
	{
		// 1. Enter Namespace Scope
		// Note: Real compilers might merge scopes if namespace is repeated.
		enterScope();

		// 2. Visit all members
		if (node.members != null)
		{
			for (ASTNode member : node.members)
			{
				member.accept(this);
			}
		}

		// 3. Exit
		exitScope();
		return Type.VOID;
	}

	/**
	 * Class: Defines a new Type and creates a scope for members.
	 */
	@Override
	public Type visitClassDeclaration(ClassDeclaration node)
	{
		System.out.println("________________________ VISITING CLASS DECLARATION: " + node.name);
		// 1. Define the class name in the CURRENT scope (so others can see it)
		Type classType = new ClassType(node.name);
		boolean defined = currentScope.define(node.name, classType);
		if (!defined)
		{
			error("Class '" + node.name + "' is already defined in this scope.", node);
		}

		// 2. Enter Class Scope (for fields/methods)
		enterScope();

		// Optional: Define 'this' in class scope
		currentScope.define("this", classType);

		// 3. Analyze Inheritance (Simplified)
		if (node.inheritance != null)
		{
			for (TypeNode parentSyntax : node.inheritance)
			{
				Type parentSem = resolveType(parentSyntax);
				if (parentSem == Type.ERROR)
					continue;
				// Logic to check cyclic inheritance or final classes goes here
			}
		}

		// 4. Visit Members
		for (Declaration member : node.members)
		{
			member.accept(this);
		}

		// 5. Exit
		exitScope();
		return Type.VOID;
	}

	@Override
	public Type visitStructDeclaration(StructDeclaration node)
	{
		return null;
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
	public Type visitIfStatement(IfStatement node)
	{
		return null;
	}

	@Override
	public Type visitForStatement(ForStatement node)
	{
		return null;
	}

	@Override
	public Type visitForeachStatement(ForeachStatement node)
	{
		return null;
	}

	/**
	 * Variable: Handles `int x = 1` and `var y = "hello"`.
	 */
	@Override
	public Type visitVariableDeclaration(VariableDeclaration node)
	{
		// 1. Determine the Target Type
		Type explicitType = null;

		if (!node.isVar)
		{
			explicitType = resolveType(node.type);
			if (explicitType == Type.ERROR)
				return Type.VOID; // Stop if type doesn't exist
		}

		// 2. Iterate over declarators (e.g., int x = 1, y = 2;)
		for (VariableDeclarator declarator : node.declarators)
		{
			Type actualType = explicitType;

			// 3. Analyze Initializer (if present)
			if (declarator.hasInitializer())
			{
				Type exprType = declarator.initializer().accept(this);

				if (node.isVar)
				{
					// Type Inference
					if (exprType == Type.VOID || exprType == Type.ERROR)
					{
						error("Cannot infer type for variable '" + declarator.name() + "' from void or error.", node);
						actualType = Type.ERROR;
					}
					else
					{
						actualType = exprType;
					}
				}
				else
				{
					// Type Checking
					if (!exprType.isAssignableTo(explicitType))
					{
						error("Type mismatch for '" + declarator.name() + "'. Expected " + explicitType.name() + ", got " + exprType.name(), declarator.initializer());
					}
				}
			}
			else
			{
				// No initializer
				if (node.isVar)
				{
					error("Implicitly typed variable '" + declarator.name() + "' must be initialized.", node);
					actualType = Type.ERROR;
				}
			}

			// 4. Define in Scope
			if (actualType != Type.ERROR)
			{
				boolean defined = currentScope.define(declarator.name(), actualType);
				if (!defined)
				{
					error("Variable '" + declarator.name() + "' is already defined.", node);
				}
			}
		}

		return Type.VOID;
	}

	@Override
	public Type visitConstDeclaration(ConstDeclaration constDeclaration)
	{
		return null;
	}

	/**
	 * Method: Defines signature, checks parameters, validates return.
	 */
	@Override
	public Type visitMethodDeclaration(MethodDeclaration node)
	{
		// 1. Resolve Return Type
		Type returnType = (node.returnType == null) ? Type.VOID : resolveType(node.returnType);

		// 2. Define Method in CURRENT scope (so it can be called recursively)
		// Note: For full function types (Func<A,B>), you'd create a FunctionType here. 
		// For now, we just treating it like a variable with the return type for simple lookup, 
		// OR simpler: we don't define it in the variable map, but a separate method map.
		// Let's assume for this snippet we treat the name as a symbol resolving to its return type 
		// (Simplification for brevity).
		if (!currentScope.define(node.name, returnType))
		{
			error("Method '" + node.name + "' already defined.", node);
		}

		// 3. Enter Method Scope
		enterScope();

		// Store return type for return statements to check against
		Type previousReturnType = currentMethodReturnType;
		currentMethodReturnType = returnType;

		// 4. Define Parameters
		for (Parameter param : node.parameters)
		{
			Type paramType = resolveType(param.type());
			if (paramType == Type.ERROR)
				continue;

			if (!currentScope.define(param.name(), paramType))
			{
				error("Duplicate parameter name '" + param.name() + "'", node);
			}

			// Optional: Check default value types
			if (param.defaultValue() != null)
			{
				Type defType = param.defaultValue().accept(this);
				if (!defType.isAssignableTo(paramType))
				{
					error("Default value type mismatch for '" + param.name() + "'", param.defaultValue());
				}
			}
		}

		// 5. Visit Body
		if (node.body != null)
		{
			node.body.accept(this);
			// Note: ASTNode doesn't guarantee 'Block', but MethodDeclaration usually has one.
			// If body is an expression (=> expr), visit it and check return type.
		}

		// 6. Restore Context and Exit
		currentMethodReturnType = previousReturnType;
		exitScope();
		return Type.VOID;
	}

	// Stub for ReturnStatement (vital for Method semantics)
	@Override
	public Type visitReturnStatement(org.nebula.nebc.ast.statements.ReturnStatement node)
	{
		Type valType = (node.value == null) ? Type.VOID : node.value.accept(this);

		if (currentMethodReturnType == null)
		{
			error("Return statement outside of method.", node);
		}
		else if (!valType.isAssignableTo(currentMethodReturnType))
		{
			error("Return type mismatch. Expected " + currentMethodReturnType.name() + ", got " + valType.name(), node);
		}
		return Type.VOID;
	}

	@Override
	public Type visitExpressionStatement(ExpressionStatement node)
	{
		return null;
	}

	// --- Boilerplate stubs to make it compile if you paste this ---
	// You will implement these next!
	@Override
	public Type visitBlock(org.nebula.nebc.ast.expressions.Block node)
	{
		return Type.VOID;
	}

	@Override
	public Type visitBinaryExpression(BinaryExpression node)
	{
		return null;
	}

	@Override
	public Type visitUnaryExpression(UnaryExpression node)
	{
		return null;
	}

	@Override
	public Type visitAssignmentExpression(AssignmentExpression node)
	{
		return null;
	}

	@Override
	public Type visitCastExpression(CastExpression node)
	{
		return null;
	}

	@Override
	public Type visitMatchExpression(MatchExpression node)
	{
		return null;
	}

	@Override
	public Type visitIfExpression(IfExpression node)
	{
		return null;
	}

	@Override
	public Type visitNewExpression(NewExpression node)
	{
		return null;
	}

	@Override
	public Type visitInvocationExpression(InvocationExpression node)
	{
		return null;
	}

	@Override
	public Type visitMemberAccessExpression(MemberAccessExpression node)
	{
		return null;
	}

	@Override
	public Type visitIndexExpression(IndexExpression node)
	{
		return null;
	}

	@Override
	public Type visitArrayLiteralExpression(ArrayLiteralExpression node)
	{
		return null;
	}

	@Override
	public Type visitTupleLiteralExpression(TupleLiteralExpression node)
	{
		return null;
	}

	@Override
	public Type visitIdentifierExpression(org.nebula.nebc.ast.expressions.IdentifierExpression node)
	{
		return Type.ANY;
	}

	@Override
	public Type visitThisExpression(ThisExpression node)
	{
		return null;
	}

	@Override
	public Type visitStringInterpolationExpression(StringInterpolationExpression node)
	{
		return null;
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
		return null;
	}

	@Override
	public Type visitLiteralExpression(org.nebula.nebc.ast.expressions.LiteralExpression node)
	{
		return Type.I32;
	}
}