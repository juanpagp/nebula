package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class IfExpression extends Expression
{
	public final Expression condition;
	public final ExpressionBlock thenExpressionBlock;
	public final ExpressionBlock elseExpressionBlock;

	public IfExpression(SourceSpan span, Expression condition, ExpressionBlock thenExpressionBlock, ExpressionBlock elseExpressionBlock)
	{
		super(span);
		this.condition = condition;
		this.thenExpressionBlock = thenExpressionBlock;
		this.elseExpressionBlock = elseExpressionBlock;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitIfExpression(this);
	}
}