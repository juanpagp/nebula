package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.LiteralExpression;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class LiteralPattern extends Pattern
{
	private final LiteralExpression value;

	public LiteralPattern(SourceSpan span, LiteralExpression value)
	{
		super(span);
		this.value = value;
	}

	public LiteralExpression getValue()
	{
		return value;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitLiteralPattern(this);
	}
}