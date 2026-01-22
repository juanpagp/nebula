package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

/**
 * Represents a construct that evaluates to a value.
 */
public abstract class Expression extends ASTNode
{
	protected Expression(SourceSpan span)
	{
		super(span);
	}
}