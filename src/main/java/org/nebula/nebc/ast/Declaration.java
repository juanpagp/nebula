package org.nebula.nebc.ast;

import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public abstract class Declaration extends ASTNode
{
	protected Declaration(SourceSpan span)
	{
		super(span);
	}
}