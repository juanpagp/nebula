package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * Represents an executable action that does not necessarily return a value.
 */
public abstract class Statement extends ASTNode
{
	protected Statement(SourceSpan span)
	{
		super(span);
	}
}