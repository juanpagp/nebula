package org.nebula.nebc.ast.tags;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public abstract class TagExpression extends ASTNode
{
	protected TagExpression(SourceSpan span)
	{
		super(span);
	}
}