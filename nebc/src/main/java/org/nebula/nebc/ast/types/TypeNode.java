package org.nebula.nebc.ast.types;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * Base class for all type-related nodes in the AST.
 * Used in declarations, casts, and generic arguments.
 */
public abstract class TypeNode extends ASTNode
{
	protected TypeNode(SourceSpan span)
	{
		super(span);
	}
}