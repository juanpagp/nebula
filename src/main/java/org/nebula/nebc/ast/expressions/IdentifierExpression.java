package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class IdentifierExpression extends Expression
{
	private final String name;

	public IdentifierExpression(SourceSpan span, String name)
	{
		super(span);
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitIdentifierExpression(this);
	}
}