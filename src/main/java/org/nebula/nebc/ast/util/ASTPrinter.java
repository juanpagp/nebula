package org.nebula.nebc.ast.util;

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
import org.nebula.nebc.ast.types.ArrayType;
import org.nebula.nebc.ast.types.NamedType;
import org.nebula.nebc.ast.types.TupleType;
import org.nebula.nebc.ast.types.TypeNode;

import java.util.List;

/**
 * An AST Debug Printer.
 * Generates a recursive, tree-like string representation of the AST.
 */
public class ASTPrinter implements ASTVisitor<String>
{
	private static final String INDENT = "  ";
	private int indentLevel = 0;

	public static String print(ASTNode node)
	{
		if (node == null)
			return "null";
		return node.accept(new ASTPrinter());
	}

	private String line(String text)
	{
		return INDENT.repeat(indentLevel) + text + "\n";
	}

	private String visitNodes(String label, List<? extends ASTNode> nodes)
	{
		if (nodes == null || nodes.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		sb.append(line(label + ":"));
		indentLevel++;
		for (ASTNode node : nodes)
		{
			if (node != null)
				sb.append(node.accept(this));
		}
		indentLevel--;
		return sb.toString();
	}

	private String visitNode(String label, ASTNode node)
	{
		if (node == null)
			return "";
		StringBuilder sb = new StringBuilder();
		sb.append(line(label + ":"));
		indentLevel++;
		sb.append(node.accept(this));
		indentLevel--;
		return sb.toString();
	}

	// =========================================================================
	// Root & Declarations
	// =========================================================================

	@Override
	public String visitCompilationUnit(CompilationUnit node)
	{
		StringBuilder sb = new StringBuilder(line("CompilationUnit"));
		indentLevel++;
		sb.append(visitNodes("Directives", node.directives));
		sb.append(visitNodes("Declarations", node.declarations));
		indentLevel--;
		return sb.toString();
	}

	@Override
	public String visitNamespaceDeclaration(NamespaceDeclaration node)
	{
		return line("Namespace: " + node.name) + visitNodes("Members", node.members);
	}

	@Override
	public String visitClassDeclaration(ClassDeclaration node)
	{
		return line("Class: " + node.name) + visitNodes("Inheritance", node.inheritance) +
				visitNodes("Members", node.members);
	}

	@Override
	public String visitMethodDeclaration(MethodDeclaration node)
	{
		String mods = node.modifiers.toString();
		StringBuilder sb = new StringBuilder(line("Method: " + node.name + " " + mods));
		sb.append(visitNode("ReturnType", node.returnType));

		// Manual loop for Parameters
		sb.append(line("Parameters:"));
		indentLevel++;
		for (Parameter p : node.parameters)
		{
			sb.append(line("Param: " + p.name()));
			sb.append(visitNode("Type", p.type()));
			if (p.defaultValue() != null)
				sb.append(visitNode("Default", p.defaultValue()));
		}
		indentLevel--;

		sb.append(visitNode("Body", node.body));
		return sb.toString();
	}

	@Override
	public String visitVariableDeclaration(VariableDeclaration node)
	{
		StringBuilder sb = new StringBuilder(line("VariableDecl (isVar=" + node.isVar() + ")"));
		sb.append(visitNode("Type", node.getType()));
		indentLevel++;
		for (VariableDeclarator dec : node.getDeclarators())
		{
			sb.append(line("Declarator: " + dec.name()));
			if (dec.initializer() != null)
			{
				sb.append(visitNode("Initializer", dec.initializer()));
			}
		}
		indentLevel--;
		return sb.toString();
	}

	@Override
	public String visitConstDeclaration(ConstDeclaration node)
	{
		return line("Const") + visitNode("Inner", node.declaration);
	}

	@Override
	public String visitStructDeclaration(StructDeclaration node)
	{
		return line("Struct: " + node.name) + visitNodes("Members", node.members);
	}

	@Override
	public String visitTraitDeclaration(TraitDeclaration node)
	{
		return line("Trait: " + node.name) + visitNodes("Members", node.members);
	}

	@Override
	public String visitUnionDeclaration(UnionDeclaration node)
	{
		return line("Union: " + node.name) + visitNodes("Variants", node.variants);
	}

	@Override
	public String visitUnionVariant(UnionVariant node)
	{
		return line("Variant: " + node.name) + visitNode("Payload", node.payload);
	}

	@Override
	public String visitConstructorDeclaration(ConstructorDeclaration node)
	{
		StringBuilder sb = new StringBuilder(line("Constructor"));

		sb.append(line("Params:"));
		indentLevel++;
		for (Parameter p : node.parameters)
		{
			sb.append(line("Param: " + p.name()));
			sb.append(visitNode("Type", p.type()));
		}
		indentLevel--;

		sb.append(visitNode("Body", node.body));
		return sb.toString();
	}

	@Override
	public String visitOperatorDeclaration(OperatorDeclaration node)
	{
		return line("Operator: " + node.operatorToken) + visitNode("Body", node.body);
	}

	// =========================================================================
	// Statements
	// =========================================================================

	@Override
	public String visitExpressionBlock(ExpressionBlock node)
	{
		StringBuilder sb = new StringBuilder(line("ExprBlock"));
		sb.append(visitNodes("Statements", node.statements));
		if (node.hasTail())
		{
			sb.append(visitNode("TailExpr", node.tail));
		}
		return sb.toString();
	}

	@Override
	public String visitStatementBlock(StatementBlock node)
	{
		return line("StmtBlock") + visitNodes("Statements", node.statements);
	}

	@Override
	public String visitIfStatement(IfStatement node)
	{
		return line("IfStmt") + visitNode("Cond", node.condition) + visitNode("Then", node.thenBranch) + visitNode("Else", node.elseBranch);
	}

	@Override
	public String visitForStatement(ForStatement node)
	{
		return line("ForStmt") + visitNode("Init", node.initializer) + visitNode("Cond", node.condition) + visitNode("Body", node.body);
	}

	@Override
	public String visitForeachStatement(ForeachStatement node)
	{
		return line("Foreach: " + node.variableName) + visitNode("Iterable", node.iterable) + visitNode("Body", node.body);
	}

	@Override
	public String visitReturnStatement(ReturnStatement node)
	{
		return line("Return") + visitNode("Value", node.value);
	}

	@Override
	public String visitExpressionStatement(ExpressionStatement node)
	{
		return line("ExprStmt") + visitNode("Expr", node.expression);
	}

	@Override
	public String visitUseStatement(UseStatement node)
	{
		return line("Use: " + node.qualifiedName + (node.alias != null ? " as " + node.alias : ""));
	}

	// =========================================================================
	// Expressions
	// =========================================================================

	@Override
	public String visitBinaryExpression(BinaryExpression node)
	{
		return line("BinaryExpr: " + node.operator) + visitNode("L", node.left) + visitNode("R", node.right);
	}

	@Override
	public String visitUnaryExpression(UnaryExpression node)
	{
		return line("UnaryExpr: " + node.operator + (node.isPostfix ? " (postfix)" : "")) + visitNode("Operand", node.operand);
	}

	@Override
	public String visitLiteralExpression(LiteralExpression node)
	{
		return line("Literal (" + node.type + "): " + node.value);
	}

	@Override
	public String visitIdentifierExpression(IdentifierExpression node)
	{
		return line("Identifier: " + node.name);
	}

	@Override
	public String visitInvocationExpression(InvocationExpression node)
	{
		return line("Invoke") + visitNode("Target", node.target) + visitNodes("Args", node.arguments);
	}

	@Override
	public String visitMemberAccessExpression(MemberAccessExpression node)
	{
		return line("MemberAccess: ." + node.memberName) + visitNode("Object", node.target);
	}

	@Override
	public String visitAssignmentExpression(AssignmentExpression node)
	{
		return line("Assign: " + node.operator) + visitNode("Target", node.target) + visitNode("Value", node.value);
	}

	@Override
	public String visitCastExpression(CastExpression node)
	{
		return line("Cast") + visitNode("To", node.targetType) + visitNode("Expr", node.expression);
	}

	@Override
	public String visitMatchExpression(MatchExpression node)
	{
		return line("Match") + visitNode("Selector", node.selector) + visitNodes("Arms", node.arms);
	}

	@Override
	public String visitIfExpression(IfExpression node)
	{
		return line("IfExpr") + visitNode("Cond", node.condition) + visitNode("Then", node.thenExpressionBlock) + visitNode("Else", node.elseExpressionBlock);
	}

	@Override
	public String visitNewExpression(NewExpression node)
	{
		return line("New: " + node.typeName) + visitNodes("Args", node.arguments);
	}

	@Override
	public String visitIndexExpression(IndexExpression node)
	{
		return line("Index") + visitNode("Target", node.target) + visitNodes("Indices", node.indices);
	}

	@Override
	public String visitArrayLiteralExpression(ArrayLiteralExpression node)
	{
		return line("ArrayLit") + visitNodes("Elements", node.elements);
	}

	@Override
	public String visitTupleLiteralExpression(TupleLiteralExpression node)
	{
		return line("TupleLit") + visitNodes("Elements", node.elements);
	}

	@Override
	public String visitThisExpression(ThisExpression node)
	{
		return line("this");
	}

	@Override
	public String visitStringInterpolationExpression(StringInterpolationExpression node)
	{
		return line("StringInterpolation") + visitNodes("Parts", node.parts);
	}

	// =========================================================================
	// Patterns & Tags & Types
	// =========================================================================

	@Override
	public String visitMatchArm(MatchArm node)
	{
		return line("MatchArm") + visitNode("Pattern", node.pattern) + visitNode("Result", node.result);
	}

	@Override
	public String visitLiteralPattern(LiteralPattern node)
	{
		return line("LiteralPattern") + visitNode("Lit", node.value);
	}

	@Override
	public String visitTypePattern(TypePattern node)
	{
		return line("TypePattern (var=" + node.variableName + ")") + visitNode("Type", node.type);
	}

	@Override
	public String visitWildcardPattern(WildcardPattern node)
	{
		return line("_ (Wildcard)");
	}

	@Override
	public String visitOrPattern(OrPattern node)
	{
		return line("OrPattern") + visitNodes("Alts", node.alternatives);
	}

	@Override
	public String visitTagStatement(TagStatement node)
	{
		return line("TagStmt: " + node.alias) + visitNode("Expr", node.tagExpression);
	}

	@Override
	public String visitTagAtom(TagAtom node)
	{
		return line("TagAtom") + visitNode("Type", node.type);
	}

	@Override
	public String visitTagOperation(TagOperation node)
	{
		return line("TagOp: " + node.operator) + visitNode("L", node.left) + visitNode("R", node.right);
	}

	@Override
	public String visitTypeReference(TypeNode node)
	{
		if (node instanceof NamedType t)
			return line("NamedType: " + t.qualifiedName);
		if (node instanceof ArrayType t)
			return line("ArrayType") + visitNode("Of", t.baseType);
		if (node instanceof TupleType t)
			return line("TupleType") + visitNodes("Of", t.elementTypes);
		return line("Type: " + node.getClass().getSimpleName());
	}
}