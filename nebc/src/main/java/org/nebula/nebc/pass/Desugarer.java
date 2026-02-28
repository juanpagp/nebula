package org.nebula.nebc.pass;

import org.nebula.nebc.ast.*;
import org.nebula.nebc.ast.declarations.*;
import org.nebula.nebc.ast.expressions.*;
import org.nebula.nebc.ast.patterns.*;
import org.nebula.nebc.ast.statements.*;
import org.nebula.nebc.ast.tags.TagAtom;
import org.nebula.nebc.ast.tags.TagOperation;
import org.nebula.nebc.ast.tags.TagStatement;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.Diagnostic;
import org.nebula.nebc.frontend.diagnostic.DiagnosticCode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lowers syntactic sugar into core AST nodes before semantic analysis.
 */
public class Desugarer implements ASTVisitor<ASTNode>
{
	private final List<Diagnostic> errors = new ArrayList<>();

	public List<Diagnostic> process(CompilationUnit unit)
	{
		for (int i = 0; i < unit.declarations.size(); i++)
		{
			unit.declarations.set(i, (Declaration) unit.declarations.get(i).accept(this));
		}
		return errors;
	}

	private void error(DiagnosticCode code, ASTNode node, Object... args)
	{
		var span = (node != null) ? node.getSpan() : SourceSpan.unknown();
		errors.add(Diagnostic.of(code, span, args));
	}

	@Override
	public ASTNode visitCompilationUnit(CompilationUnit node)
	{
		return node;
	}

	// ----------------------
	// ---- Declarations ----
	// ----------------------

	@Override
	public ASTNode visitExternDeclaration(ExternDeclaration node)
	{
		List<MethodDeclaration> members = new ArrayList<>();
		for (int i = 0; i < node.members.size(); i++)
		{
			members.add((MethodDeclaration) node.members.get(i).accept(this));
		}
		return new ExternDeclaration(node.getSpan(), node.language, members, node.isPrivate);
	}

	@Override
	public ASTNode visitNamespaceDeclaration(NamespaceDeclaration node)
	{
		List<ASTNode> members = new ArrayList<>();
		for (int i = 0; i < node.members.size(); i++)
		{
			members.add(node.members.get(i).accept(this));
		}
		return new NamespaceDeclaration(node.getSpan(), node.name, members, node.isBlockDeclaration);
	}

	@Override
	public ASTNode visitVariableDeclaration(VariableDeclaration node)
	{
		List<VariableDeclarator> decls = new ArrayList<>();
		for (int i = 0; i < node.declarators.size(); i++)
		{
			VariableDeclarator decl = node.declarators.get(i);
			if (decl.hasInitializer())
			{
				decls.add(new VariableDeclarator(decl.name(), (Expression) decl.initializer().accept(this)));
			}
			else
			{
				decls.add(decl);
			}
		}
		return new VariableDeclaration(node.getSpan(), node.type, decls, node.isVar);
	}

	@Override
	public ASTNode visitConstDeclaration(ConstDeclaration node)
	{
		VariableDeclaration vd = (VariableDeclaration) node.declaration.accept(this);
		return new ConstDeclaration(node.getSpan(), vd);
	}

	@Override
	public ASTNode visitMethodDeclaration(MethodDeclaration node)
	{
		ASTNode body = node.body != null ? node.body.accept(this) : null;
		return new MethodDeclaration(node.getSpan(), node.isExtern, node.modifiers, node.returnType, node.name, node.typeParams, node.parameters, body);
	}

	@Override
	public ASTNode visitClassDeclaration(ClassDeclaration node)
	{
		List<Declaration> members = new ArrayList<>();
		for (int i = 0; i < node.members.size(); i++)
		{
			members.add((Declaration) node.members.get(i).accept(this));
		}
		return new ClassDeclaration(node.getSpan(), node.name, node.typeParams, node.inheritance, members);
	}

	@Override
	public ASTNode visitStructDeclaration(StructDeclaration node)
	{
		List<Declaration> members = new ArrayList<>();
		for (int i = 0; i < node.members.size(); i++)
		{
			members.add((Declaration) node.members.get(i).accept(this));
		}
		return new StructDeclaration(node.getSpan(), node.name, node.typeParams, node.inheritance, members);
	}

	@Override
	public ASTNode visitTraitDeclaration(TraitDeclaration node)
	{
		List<MethodDeclaration> members = new ArrayList<>();
		for (int i = 0; i < node.members.size(); i++)
		{
			members.add((MethodDeclaration) node.members.get(i).accept(this));
		}
		return new TraitDeclaration(node.getSpan(), node.name, node.typeParams, members);
	}

	@Override
	public ASTNode visitImplDeclaration(ImplDeclaration node)
	{
		List<MethodDeclaration> members = new ArrayList<>();
		for (int i = 0; i < node.members.size(); i++)
		{
			members.add((MethodDeclaration) node.members.get(i).accept(this));
		}
		return new ImplDeclaration(node.getSpan(), (TypeNode) node.traitType.accept(this), (TypeNode) node.targetType.accept(this), members);
	}

	@Override
	public ASTNode visitEnumDeclaration(EnumDeclaration node)
	{
		return node;
	}

	@Override
	public ASTNode visitUnionDeclaration(UnionDeclaration node)
	{
		return node;
	}

	@Override
	public ASTNode visitUnionVariant(UnionVariant node)
	{
		return node;
	}

	@Override
	public ASTNode visitOperatorDeclaration(OperatorDeclaration node)
	{
		ASTNode body = node.body != null ? node.body.accept(this) : null;
		return new OperatorDeclaration(node.getSpan(), node.operatorToken, node.parameters, body);
	}

	@Override
	public ASTNode visitConstructorDeclaration(ConstructorDeclaration node)
	{
		ASTNode body = node.body != null ? node.body.accept(this) : null;
		return new ConstructorDeclaration(node.getSpan(), node.name, node.parameters, body);
	}

	// --------------------
	// ---- Statements ----
	// --------------------

	@Override
	public ASTNode visitStatementBlock(StatementBlock node)
	{
		List<Statement> stmts = new ArrayList<>();
		for (int i = 0; i < node.statements.size(); i++)
		{
			stmts.add((Statement) node.statements.get(i).accept(this));
		}
		return new StatementBlock(node.getSpan(), stmts);
	}

	@Override
	public ASTNode visitTagStatement(TagStatement node)
	{
		return node;
	}

	@Override
	public ASTNode visitUseStatement(UseStatement node)
	{
		return node;
	}

	@Override
	public ASTNode visitIfStatement(IfStatement node)
	{
		Expression cond = (Expression) node.condition.accept(this);
		Statement thenB = (Statement) node.thenBranch.accept(this);
		Statement elseB = node.elseBranch != null ? (Statement) node.elseBranch.accept(this) : null;
		return new IfStatement(node.getSpan(), cond, thenB, elseB);
	}

	@Override
	public ASTNode visitForStatement(ForStatement node)
	{
		// 1. Desugar the body first
		Statement desugaredBody = node.body != null ? (Statement) node.body.accept(this) : null;

		// 2. Check if this is a syntactic-sugar loop: NO initializer, NO iterators, BUT
		// has a condition.
		if (node.initializer == null && (node.iterators == null || node.iterators.isEmpty()) && node.condition != null)
		{
			BinaryExpression binExpr = null;
			String iteratorName = null;
			Expression initialValue = null;

			if (node.condition instanceof BinaryExpression b)
			{
				binExpr = b;
				if (binExpr.left instanceof IdentifierExpression ident)
				{
					iteratorName = ident.name;
					initialValue = new LiteralExpression(node.getSpan(), 0L, LiteralExpression.LiteralType.INT);
				}
			}
			else if (node.condition instanceof AssignmentExpression assign)
			{
				if (assign.value instanceof BinaryExpression b && assign.target instanceof IdentifierExpression ident)
				{
					binExpr = b;
					iteratorName = ident.name;
					initialValue = b.left;
				}
			}

			if (binExpr != null && iteratorName != null)
			{
				boolean isDescending = binExpr.operator == BinaryOperator.GT || binExpr.operator == BinaryOperator.GE;
				boolean isAscending = binExpr.operator == BinaryOperator.LT || binExpr.operator == BinaryOperator.LE;

				if (isDescending || isAscending)
				{
					if (isDescending && !(node.condition instanceof AssignmentExpression))
					{
						error(DiagnosticCode.INTERNAL_ERROR, node, "Descending loops require an explicit iterator initial value.");
					}
					else
					{
						VariableDeclarator decl = new VariableDeclarator(iteratorName, initialValue);
						VariableDeclaration initDecl = new VariableDeclaration(node.getSpan(), new org.nebula.nebc.ast.types.NamedType(node.getSpan(), "i32", Collections.emptyList()), List.of(decl), false);

						Expression cleanCondition = new BinaryExpression(node.getSpan(), new IdentifierExpression(node.getSpan(), iteratorName), binExpr.operator, binExpr.right);

						UnaryOperator unaryOp = isAscending ? UnaryOperator.INCREMENT : UnaryOperator.DECREMENT;
						Expression iteratorUpdate = new UnaryExpression(node.getSpan(), unaryOp, new IdentifierExpression(node.getSpan(), iteratorName), true);

						return new ForStatement(node.getSpan(), initDecl, cleanCondition, List.of(iteratorUpdate), desugaredBody);
					}
				}
			}
		}

		// If no desugaring applied, simply recurse
		Statement init = node.initializer != null ? (Statement) node.initializer.accept(this) : null;
		Expression cond = node.condition != null ? (Expression) node.condition.accept(this) : null;
		List<Expression> iters = new ArrayList<>();
		if (node.iterators != null)
		{
			for (Expression e : node.iterators)
			{
				iters.add((Expression) e.accept(this));
			}
		}

		return new ForStatement(node.getSpan(), init, cond, iters, desugaredBody);
	}

	@Override
	public ASTNode visitForeachStatement(ForeachStatement node)
	{
		Expression iterable = (Expression) node.iterable.accept(this);
		Statement body = (Statement) node.body.accept(this);
		return new ForeachStatement(node.getSpan(), node.variableType, node.variableName, iterable, body);
	}

	@Override
	public ASTNode visitWhileStatement(WhileStatement node)
	{
		Expression condition = (Expression) node.condition.accept(this);
		Statement body = (Statement) node.body.accept(this);
		return new WhileStatement(node.getSpan(), condition, body);
	}

	@Override
	public ASTNode visitReturnStatement(ReturnStatement node)
	{
		if (node.value != null)
		{
			return new ReturnStatement(node.getSpan(), (Expression) node.value.accept(this));
		}
		return node;
	}

	@Override
	public ASTNode visitExpressionStatement(ExpressionStatement node)
	{
		return new ExpressionStatement(node.getSpan(), (Expression) node.expression.accept(this));
	}

	// ---------------------
	// ---- Expressions ----
	// ---------------------

	@Override
	public ASTNode visitExpressionBlock(ExpressionBlock node)
	{
		List<Statement> stmts = new ArrayList<>();
		for (int i = 0; i < node.statements.size(); i++)
		{
			stmts.add((Statement) node.statements.get(i).accept(this));
		}
		Expression tail = null;
		if (node.hasTail())
		{
			tail = (Expression) node.tail.accept(this);
		}
		return new ExpressionBlock(node.getSpan(), stmts, tail);
	}

	@Override
	public ASTNode visitBinaryExpression(BinaryExpression node)
	{
		Expression left = (Expression) node.left.accept(this);
		Expression right = (Expression) node.right.accept(this);
		return new BinaryExpression(node.getSpan(), left, node.operator, right);
	}

	@Override
	public ASTNode visitUnaryExpression(UnaryExpression node)
	{
		Expression op = (Expression) node.operand.accept(this);
		return new UnaryExpression(node.getSpan(), node.operator, op, node.isPostfix);
	}

	@Override
	public ASTNode visitAssignmentExpression(AssignmentExpression node)
	{
		Expression target = (Expression) node.target.accept(this);
		Expression value = (Expression) node.value.accept(this);
		return new AssignmentExpression(node.getSpan(), target, node.operator, value);
	}

	@Override
	public ASTNode visitCastExpression(CastExpression node)
	{
		Expression expr = (Expression) node.expression.accept(this);
		return new CastExpression(node.getSpan(), node.targetType, expr);
	}

	@Override
	public ASTNode visitMatchExpression(MatchExpression node)
	{
		Expression selector = (Expression) node.selector.accept(this);
		List<MatchArm> arms = new ArrayList<>();
		for (MatchArm arm : node.arms)
		{
			arms.add((MatchArm) arm.accept(this));
		}
		return new MatchExpression(node.getSpan(), selector, arms);
	}

	@Override
	public ASTNode visitIfExpression(IfExpression node)
	{
		Expression condition = (Expression) node.condition.accept(this);
		ExpressionBlock thenB = (ExpressionBlock) node.thenExpressionBlock.accept(this);
		ExpressionBlock elseB = node.elseExpressionBlock != null ? (ExpressionBlock) node.elseExpressionBlock.accept(this) : null;
		return new IfExpression(node.getSpan(), condition, thenB, elseB);
	}

	@Override
	public ASTNode visitNewExpression(NewExpression node)
	{
		List<Expression> args = new ArrayList<>();
		for (Expression arg : node.arguments)
		{
			args.add((Expression) arg.accept(this));
		}
		return new NewExpression(node.getSpan(), node.typeName, args);
	}

	@Override
	public ASTNode visitInvocationExpression(InvocationExpression node)
	{
		Expression target = (Expression) node.target.accept(this);
		List<Expression> args = new ArrayList<>();
		for (Expression arg : node.arguments)
		{
			args.add((Expression) arg.accept(this));
		}
		return new InvocationExpression(node.getSpan(), target, args);
	}

	@Override
	public ASTNode visitMemberAccessExpression(MemberAccessExpression node)
	{
		Expression target = (Expression) node.target.accept(this);
		return new MemberAccessExpression(node.getSpan(), target, node.memberName);
	}

	@Override
	public ASTNode visitIndexExpression(IndexExpression node)
	{
		Expression target = (Expression) node.target.accept(this);
		List<Expression> args = new ArrayList<>();
		for (Expression arg : node.indices)
		{
			args.add((Expression) arg.accept(this));
		}
		return new IndexExpression(node.getSpan(), target, args);
	}

	@Override
	public ASTNode visitArrayLiteralExpression(ArrayLiteralExpression node)
	{
		List<Expression> args = new ArrayList<>();
		for (Expression arg : node.elements)
		{
			args.add((Expression) arg.accept(this));
		}
		return new ArrayLiteralExpression(node.getSpan(), args);
	}

	@Override
	public ASTNode visitTupleLiteralExpression(TupleLiteralExpression node)
	{
		List<Expression> args = new ArrayList<>();
		for (Expression arg : node.elements)
		{
			args.add((Expression) arg.accept(this));
		}
		return new TupleLiteralExpression(node.getSpan(), args);
	}

	@Override
	public ASTNode visitLiteralExpression(LiteralExpression node)
	{
		return node;
	}

	@Override
	public ASTNode visitIdentifierExpression(IdentifierExpression node)
	{
		return node;
	}

	@Override
	public ASTNode visitThisExpression(ThisExpression node)
	{
		return node;
	}

	@Override
	public ASTNode visitStringInterpolationExpression(StringInterpolationExpression node)
	{
		return node;
	}

	// ------------------
	// ---- Patterns ----
	// ------------------

	@Override
	public ASTNode visitMatchArm(MatchArm node)
	{
		Pattern p = (Pattern) node.pattern.accept(this);
		Expression e = (Expression) node.result.accept(this);
		return new MatchArm(node.getSpan(), p, e);
	}

	@Override
	public ASTNode visitLiteralPattern(LiteralPattern node)
	{
		return node;
	}

	@Override
	public ASTNode visitTypePattern(TypePattern node)
	{
		return node;
	}

	@Override
	public ASTNode visitWildcardPattern(WildcardPattern node)
	{
		return node;
	}

	@Override
	public ASTNode visitOrPattern(OrPattern node)
	{
		List<Pattern> alts = new ArrayList<>();
		for (Pattern p : node.alternatives)
		{
			alts.add((Pattern) p.accept(this));
		}
		return new OrPattern(node.getSpan(), alts);
	}

	// --------------
	// ---- Tags ----
	// --------------

	@Override
	public ASTNode visitTagAtom(TagAtom node)
	{
		return node;
	}

	@Override
	public ASTNode visitTagOperation(TagOperation node)
	{
		ASTNode left = node.left.accept(this);
		ASTNode right = node.right != null ? node.right.accept(this) : null;
		return new TagOperation(node.getSpan(), node.operator, (org.nebula.nebc.ast.tags.TagExpression) left, (org.nebula.nebc.ast.tags.TagExpression) right);
	}

	// ---------------
	// ---- Types ----
	// ---------------

	@Override
	public ASTNode visitTypeReference(TypeNode node)
	{
		return node;
	}
}
