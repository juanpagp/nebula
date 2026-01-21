package org.nebula.nebc.ast.directives;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public abstract class Directive extends ASTNode
{
	protected Directive(SourceSpan span)
	{
		super(span);
	}
}
