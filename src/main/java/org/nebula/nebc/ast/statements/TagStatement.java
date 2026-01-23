package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.Modifier;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class TagStatement extends Statement
{
	private final Modifier visibility;
	private final Object tagContent; // Could be a Type or a specific TagExpression node
	private final String alias;

	public TagStatement(SourceSpan span, Modifier visibility, Object tagContent, String alias)
	{
		super(span);
		this.visibility = visibility;
		this.tagContent = tagContent;
		this.alias = alias;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTagStatement(this);
	}
}