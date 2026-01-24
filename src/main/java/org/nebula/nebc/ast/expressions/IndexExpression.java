package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class IndexExpression extends Expression
{
	private final Expression target;     // The array/map
	private final List<Expression> indices; // Supports arr[x, y] if grammar allows, or just List.of(index)

	public IndexExpression(SourceSpan span, Expression target, List<Expression> indices)
	{
		super(span);
		this.target = target;
		this.indices = indices;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitIndexExpression(this);
	}
}