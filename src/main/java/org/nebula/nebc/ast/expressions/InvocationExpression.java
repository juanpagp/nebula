package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class InvocationExpression extends Expression
{
	public final Expression target;
	public final List<Expression> arguments;

	public InvocationExpression(SourceSpan span, Expression target, List<Expression> arguments)
	{
		super(span);
		this.target = target;
		this.arguments = arguments;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitInvocationExpression(this);
	}
}