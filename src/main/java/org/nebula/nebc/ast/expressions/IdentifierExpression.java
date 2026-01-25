package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class IdentifierExpression extends Expression
{
	public final String name;

	public IdentifierExpression(SourceSpan span, String name)
	{
		super(span);
		this.name = name;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitIdentifierExpression(this);
	}
}