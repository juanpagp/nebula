package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class CastExpression extends Expression
{
	public final TypeNode targetType;
	public final Expression expression;

	public CastExpression(SourceSpan span, TypeNode targetType, Expression expression)
	{
		super(span);
		this.targetType = targetType;
		this.expression = expression;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitCastExpression(this);
	}
}