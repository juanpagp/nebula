package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * Represents a {@code continue} statement, which skips to the next iteration
 * of the nearest enclosing loop.
 *
 * <pre>
 * continue;
 * </pre>
 */
public class ContinueStatement extends Statement
{
	public ContinueStatement(SourceSpan span)
	{
		super(span);
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitContinueStatement(this);
	}
}
