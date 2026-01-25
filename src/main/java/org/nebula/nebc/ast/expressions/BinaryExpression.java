package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.BinaryOperator;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class BinaryExpression extends Expression
{
	public final Expression left;
	public final BinaryOperator operator;
	public final Expression right;

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