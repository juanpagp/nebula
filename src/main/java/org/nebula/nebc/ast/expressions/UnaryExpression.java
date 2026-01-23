package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.UnaryOperator;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class UnaryExpression extends Expression
{
	private final UnaryOperator operator;
	private final Expression operand;

	public UnaryExpression(SourceSpan span, UnaryOperator operator, Expression operand)
	{
		super(span);
		this.operator = operator;
		this.operand = operand;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitUnaryExpression(this);
	}
}