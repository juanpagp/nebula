package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;
import org.nebula.nebc.semantic.types.Type;

import java.util.List;

public class InvocationExpression extends Expression
{
	public final Expression target;
	public final List<Expression> arguments;

	/**
	 * For
	 * generic
	 * calls:
	 * explicit
	 * or
	 * inferred
	 * concrete
	 * types
	 * for
	 * the
	 * target's
	 * type
	 * params.
	 */
	private List<Type> typeArguments;

	public InvocationExpression(SourceSpan span, Expression target, List<Expression> arguments)
	{
		super(span);
		this.target = target;
		this.arguments = arguments;
	}

	public List<Type> getTypeArguments()
	{
		return typeArguments;
	}

	public void setTypeArguments(List<Type> typeArguments)
	{
		this.typeArguments = typeArguments;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitInvocationExpression(this);
	}
}