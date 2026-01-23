package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.BinaryOperator;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class BinaryExpression extends Expression
{
	private final Expression left;
	private final BinaryOperator operator;
	private final Expression right;

	public BinaryExpression(SourceSpan span, Expression left, BinaryOperator operator, Expression right)
	{
		super(span);
		this.left = left;
		this.operator = operator;
		this.right = right;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitBinaryExpression(this);
	}
}