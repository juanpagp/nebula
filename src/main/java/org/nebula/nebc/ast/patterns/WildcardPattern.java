package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

/**
 * Represents the `_` pattern which matches anything but binds nothing.
 */
public class WildcardPattern extends Pattern
{
	public WildcardPattern(SourceSpan span)
	{
		super(span);
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitWildcardPattern(this);
	}
}