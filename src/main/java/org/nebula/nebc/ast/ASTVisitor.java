package org.nebula.nebc.ast;

import org.nebula.nebc.ast.declarations.*;
import org.nebula.nebc.ast.expressions.*;
import org.nebula.nebc.ast.statements.*;
import org.nebula.nebc.ast.types.*;
import org.nebula.nebc.ast.statements.UseStatement;

/**
 * Interface for the Visitor pattern that allows AST traversal.
 * Each `visit` method corresponds to a specific AST node type.
 */
public interface ASTVisitor<R>
{
	R visitCompilationUnit(CompilationUnit node);

	// ----------------------
	// ---- Declarations ----
	// ----------------------

	/**
	 * @grammar namespace name { ... } or namespace name;
	 */
	R visitNamespaceDeclaration(NamespaceDeclaration node);

	/**
	 * Includes both top-level and member variable declarations.
	 */
	R visitVariableDeclaration(VariableDeclaration node);

	/**
	 * @grammar modifiers returnType name<T>(params) { body }
	 */
	R visitMethodDeclaration(MethodDeclaration node);

	/**
	 * @grammar class name<T> : Base { members }
	 */
	R visitClassDeclaration(ClassDeclaration node);

	/**
	 * @grammar struct name<T> : Base { members }
	 */
	R visitStructDeclaration(StructDeclaration node);

	/**
	 * @grammar trait name { members }
	 */
	R visitTraitDeclaration(TraitDeclaration node);

	/**
	 * @grammar tagged union name { Variant(Type), ... }
	 */
	R visitUnionDeclaration(UnionDeclaration node);

	/**
	 * Handles the specific variant/payload inside a Union.
	 */
	R visitUnionVariant(UnionVariant node);

	/**
	 * @grammar operator + (params) { body }
	 */
	R visitOperatorDeclaration(OperatorDeclaration node);

	/**
	 * @grammar Identifier(params) { body }
	 */
	R visitConstructorDeclaration(ConstructorDeclaration node);

	// --------------------
	// ---- Statements ----
	// --------------------

	/**
	 * @grammar [visibility] tag declaration as Identifier;
	 */
	R visitTagStatement(TagStatement node);

	/**
	 * @grammar use qualified::name [as Alias];
	 */
	R visitUseStatement(UseStatement node);

	/**
	 * @grammar if (expr) stmt else stmt
	 */
	R visitIfStatement(IfStatement node);

	/**
	 * @grammar for (init; cond; iter) stmt
	 */
	R visitForStatement(ForStatement node);

	/**
	 * @grammar foreach (var x in collection) stmt
	 */
	R visitForeachStatement(ForeachStatement node);

	/**
	 * @grammar return [expression];
	 */
	R visitReturnStatement(ReturnStatement node);

	/**
	 * An expression used as a statement (e.g., a function call).
	 */
	R visitExpressionStatement(ExpressionStatement node);

	// ---------------------
	// ---- Expressions ----
	// ---------------------

	/**
	 * @grammar { statements; optionalExpression }
	 */
	R visitBlock(Block node);

	/**
	 * Collapses additive, multiplicative, relational, etc.
	 * Use an 'Operator' enum inside the node to distinguish them.
	 */
	R visitBinaryExpression(BinaryExpression node);

	/**
	 * @grammar !expr, -expr, ~expr, etc.
	 */
	R visitUnaryExpression(UnaryExpression node);

	/**
	 * @grammar target = value, target += value, etc.
	 */
	R visitAssignmentExpression(AssignmentExpression node);

	/**
	 * @grammar (Type)expression
	 */
	R visitCastExpression(CastExpression node);

	/**
	 * @grammar match (expr) { pattern => expr, ... }
	 */
	R visitMatchExpression(MatchExpression node);

	/**
	 * @grammar if (expr) block else block
	 */
	R visitIfExpression(IfExpression node);

	/**
	 * @grammar new Type(args)
	 */
	R visitNewExpression(NewExpression node);

	/**
	 * Handles method calls and constructor calls.
	 */
	R visitInvocationExpression(InvocationExpression node);

	/**
	 * @grammar obj.member or tuple.0
	 */
	R visitMemberAccessExpression(MemberAccessExpression node);

	/**
	 * @grammar [expr, expr, expr]
	 */
	R visitArrayLiteralExpression(ArrayLiteralExpression node);

	/**
	 * @grammar (expr, expr)
	 */
	R visitTupleLiteralExpression(TupleLiteralExpression node);

	/**
	 * Handles all basic types: Int, Float, String, Bool, etc.
	 */
	R visitLiteralExpression(LiteralExpression node);

	/**
	 * Accessing a variable by name.
	 */
	R visitIdentifierExpression(IdentifierExpression node);

	// ---------------
	// ---- Types ----
	// ---------------

	/**
	 * Used for type references (e.g., in variable declarations or casts).
	 */
	R visitTypeReference(Type node);
}