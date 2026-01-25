package org.nebula.nebc.semantic;

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
import org.nebula.nebc.semantic.types.PrimitiveType;
import org.nebula.nebc.semantic.types.Type;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer implements ASTVisitor<Type>
{
	private final List<SemanticError> errors = new ArrayList<>();
	private Scope currentScope = new Scope(null); // Global scope

	// --- Entry Point ---
	public List<SemanticError> analyze(CompilationUnit unit)
	{
		unit.accept(this);
		return errors;
	}

	private void error(String msg, ASTNode node)
	{
		// Collect the error, don't throw exception
		errors.add(new SemanticError(msg, node.getSpan()));
	}

	// --- Scope Management Helpers ---
	private void enterScope()
	{
		currentScope = new Scope(currentScope);
	}

	private void exitScope()
	{
		currentScope = currentScope.getParent();
	}

	// ==========================================
	// Visitor Implementation
	// ==========================================

	@Override
	public Type visitCompilationUnit(CompilationUnit node)
	{
		// First pass: Declare types (classes/structs) so they are visible everywhere
		// Second pass: Analyze bodies
		for (ASTNode decl : node.declarations)
		{
			decl.accept(this);
		}
		return Type.VOID;
	}

	@Override
	public Type visitNamespaceDeclaration(NamespaceDeclaration node)
	{
		return null;
	}

	@Override
	public Type visitVariableDeclaration(VariableDeclaration node)
	{
		// 1. Resolve the declared type (convert AST Type to Type)
		Type declaredType = resolveType(node.type);

		for (var decl : node.declarators)
		{
			// 2. Check Initializer
			if (decl.initializer() != null)
			{
				Type initType = decl.initializer().accept(this);

				if (!initType.isAssignableTo(declaredType))
				{
					error("Type mismatch: Cannot assign " + initType.name() + " to " + declaredType.name(), node);
				}
			}

			// 3. Define in Scope
			// Check for duplicates first
			if (currentScope.resolve(decl.name()) != null)
			{ // simplistic check, ideally check current scope only
				error("Variable '" + decl.name() + "' is already defined.", node);
			}
			else
			{
				currentScope.define(decl.name(), declaredType);
			}
		}
		return Type.VOID;
	}

	@Override
	public Type visitConstDeclaration(ConstDeclaration constDeclaration)
	{
		return null;
	}

	@Override
	public Type visitMethodDeclaration(MethodDeclaration node)
	{
		return null;
	}

	@Override
	public Type visitClassDeclaration(ClassDeclaration node)
	{
		return null;
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
	public Type visitBinaryExpression(BinaryExpression node)
	{
		Type left = node.left.accept(this);
		Type right = node.right.accept(this);

		// Don't cascade errors: if operands are already errors, stop checking here
		if (left == Type.ERROR || right == Type.ERROR)
			return Type.ERROR;

		switch (node.operator)
		{
			case ADD:
			case SUB:
			case MUL:
			case DIV:
				if (left == Type.I32 && right == Type.I32)
					return Type.I32;
				// Add logic for Floats, String concatenation, etc.
				error("Operator " + node.operator + " not defined for " + left.name() + " and " + right.name(), node);
				return Type.ERROR; // Return Error type so parents know something went wrong

			case EQ:
			case NE:
				// Type check left and right are comparable...
				return Type.BOOL;

			default:
				return Type.ERROR;
		}
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
	public Type visitIdentifierExpression(IdentifierExpression node)
	{
		Type type = currentScope.resolve(node.name);
		if (type == null)
		{
			error("Undefined variable '" + node.name + "'", node);
			return Type.ERROR; // Return ERROR type prevents cascading "type mismatch" errors later
		}
		return type;
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
	public Type visitBlock(Block node)
	{
		enterScope();
		for (Statement stmt : node.statements)
		{
			stmt.accept(this);
		}
		Type tailType = Type.VOID;
		if (node.hasTail())
		{
			tailType = node.tail.accept(this);
		}
		exitScope();
		return tailType;
	}

	@Override
	public Type visitLiteralExpression(LiteralExpression node)
	{
		return switch (node.type)
		{
			case INT ->
					Type.I32;
			case BOOL ->
					Type.BOOL;
			case STRING ->
					new PrimitiveType("string"); // Define constant somewhere
			default ->
					Type.VOID;
		};
	}

	// ... Implement the rest (loops, if, classes) ...

	// Helper to convert AST Type nodes to Type objects
	private Type resolveType(TypeNode astType)
	{
		if (astType == null)
			return Type.ANY; // 'var' inference placeholder
		// logic to look up named types, etc.
		return Type.I32; // Placeholder
	}

	// Stub methods for other visitor interface requirements
	@Override
	public Type visitIfStatement(IfStatement node)
	{
		return Type.VOID;
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

	@Override
	public Type visitReturnStatement(ReturnStatement node)
	{
		return Type.VOID;
	}

	@Override
	public Type visitExpressionStatement(ExpressionStatement node)
	{
		return null;
	}
	// ... etc ...
}