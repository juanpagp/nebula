package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class IfExpression extends Expression
{
	public final Expression condition;
	public final Block thenBlock;
	public final Block elseBlock;

	public IfExpression(SourceSpan span, Expression condition, Block thenBlock, Block elseBlock)
	{
		super(span);
		this.condition = condition;
		this.thenBlock = thenBlock;
		this.elseBlock = elseBlock;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitIfExpression(this);
	}
}