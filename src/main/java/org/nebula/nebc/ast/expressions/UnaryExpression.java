package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.UnaryOperator;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class UnaryExpression extends Expression
{
	private final UnaryOperator operator;
	private final Expression operand;
	private final boolean isPostfix;

	public UnaryExpression(SourceSpan span, UnaryOperator operator, Expression operand, boolean isPostfix)
	{
		super(span);
		this.operator = operator;
		this.operand = operand;
		this.isPostfix = isPostfix;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitUnaryExpression(this);
	}
}