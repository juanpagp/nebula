package org.nebula.nebc.ast.tags;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.Modifier;
import org.nebula.nebc.ast.statements.Statement;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class TagStatement extends Statement
{
	public final Modifier visibility;
	public final TagExpression tagExpression;
	public final String alias;

	public TagStatement(SourceSpan span, Modifier visibility, TagExpression tagExpression, String alias)
	{
		super(span);
		this.visibility = visibility;
		this.tagExpression = tagExpression;
		this.alias = alias;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTagStatement(this);
	}
}