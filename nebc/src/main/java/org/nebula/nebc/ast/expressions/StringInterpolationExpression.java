package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class StringInterpolationExpression extends Expression
{
	public final List<Expression> parts; // A mix of LiteralExpressions (strings) and other Expressions (variables)

	public StringInterpolationExpression(SourceSpan span, List<Expression> parts)
	{
		super(span);
		this.parts = parts;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitStringInterpolationExpression(this);
	}
}