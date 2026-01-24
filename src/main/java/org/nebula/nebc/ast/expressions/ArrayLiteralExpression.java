package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class ArrayLiteralExpression extends Expression
{
	public final List<Expression> elements;

	public ArrayLiteralExpression(SourceSpan span, List<Expression> elements)
	{
		super(span);
		this.elements = elements;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitArrayLiteralExpression(this);
	}
}