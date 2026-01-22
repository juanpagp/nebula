package org.nebula.nebc.ast.types;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

/**
 * Base class for all type-related nodes in the AST.
 * Used in declarations, casts, and generic arguments.
 */
public abstract class Type extends ASTNode
{
	protected Type(SourceSpan span)
	{
		super(span);
	}
}