package org.nebula.nebc.ast;

import org.nebula.nebc.ast.declarations.*;
import org.nebula.nebc.ast.expressions.*;
import org.nebula.nebc.ast.patterns.*;
import org.nebula.nebc.ast.statements.*;
import org.nebula.nebc.ast.tags.*;
import org.nebula.nebc.ast.types.Type;

/**
 * Interface for the Visitor pattern that allows AST traversal.
 * Each `visit` method corresponds to a specific AST node type.
 *
 * @param <R> The return type of the visit operation.
 */
public interface ASTVisitor<R>
{
	/**
	 * Visits the root node of the AST.
	 */
	R visitCompilationUnit(CompilationUnit node);

	// ----------------------
	// ---- Declarations ----
	// ----------------------

	/**
	 * @grammar namespace name { ... } or namespace name;
	 */
	R visitNamespaceDeclaration(NamespaceDeclaration node);

	/**
	 * Includes member variable declarations.
	 */
	R visitVariableDeclaration(VariableDeclaration node);

	/**
	 * Includes both top-level and member const declarations.
	 */
	R visitConstDeclaration(ConstDeclaration constDeclaration);


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
	 * An expression used as a statement (e.g., a function call or assignment).
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
	 */
	R visitBinaryExpression(BinaryExpression node);

	/**
	 * @grammar !expr, -expr, ~expr, expr++, expr--
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
	R visitMatchExpression(org.nebula.nebc.ast.patterns.MatchExpression node);

	/**
	 * @grammar if (expr) block else block
	 */
	R visitIfExpression(IfExpression node);

	/**
	 * @grammar new Type(args)
	 */
	R visitNewExpression(NewExpression node);

	/**
	 * Handles method calls and constructor invocations.
	 */
	R visitInvocationExpression(InvocationExpression node);

	/**
	 * @grammar obj.member or tuple.0
	 */
	R visitMemberAccessExpression(MemberAccessExpression node);

	/**
	 * @grammar array[index] or map[key]
	 */
	R visitIndexExpression(IndexExpression node);

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

	/**
	 * @grammar this
	 */
	R visitThisExpression(ThisExpression node);

	/**
	 * @grammar "Hello {name}"
	 */
	R visitStringInterpolationExpression(StringInterpolationExpression node);

	// ------------------
	// ---- Patterns ----
	// ------------------

	/**
	 * A single branch in a match expression.
	 */
	R visitMatchArm(MatchArm node);

	/**
	 * Matches a literal value (e.g., case 10 => ...).
	 */
	R visitLiteralPattern(LiteralPattern node);

	/**
	 * Matches a type (e.g., case String s => ...).
	 */
	R visitTypePattern(TypePattern node);

	/**
	 * Matches anything (e.g., case _ => ...).
	 */
	R visitWildcardPattern(WildcardPattern node);

	/**
	 * Matches multiple alternatives (e.g., case A | B => ...).
	 */
	R visitOrPattern(OrPattern node);

	// --------------
	// ---- Tags ----
	// --------------

	/**
	 * A tag atom (usually a Type).
	 */
	R visitTagAtom(TagAtom node);

	/**
	 * Tag operations like Union (|), Intersection (&), or Negation (!).
	 */
	R visitTagOperation(TagOperation node);

	// ---------------
	// ---- Types ----
	// ---------------

	/**
	 * Used for type references (e.g., in variable declarations or casts).
	 */
	R visitTypeReference(Type node);
}