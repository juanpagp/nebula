package org.nebula.nebc.ast.tags;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.Modifier;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class TagStatement extends Statement
{
	private final Modifier visibility;
	private final TagExpression tagExpression;
	private final String alias;

	public TagStatement(SourceSpan span, Modifier visibility, TagExpression tagExpression, String alias)
	{
		super(span);
		this.visibility = visibility;
		this.tagExpression = tagExpression;
		this.alias = alias;
	}

	public TagExpression getTagExpression()
	{
		return tagExpression;
	}

	public String getAlias()
	{
		return alias;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTagStatement(this);
	}
}