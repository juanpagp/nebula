package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * Represents a {@code break} statement, which exits the nearest enclosing loop.
 *
 * <pre>
 * break;
 * </pre>
 */
public class BreakStatement extends Statement
{
	public BreakStatement(SourceSpan span)
	{
		super(span);
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitBreakStatement(this);
	}
}
