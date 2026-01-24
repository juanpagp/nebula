package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public abstract class Pattern extends ASTNode
{
	protected Pattern(SourceSpan span)
	{
		super(span);
	}

	@Override
	public abstract <R> R accept(ASTVisitor<R> visitor);
}