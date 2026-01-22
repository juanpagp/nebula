package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.statements.Statement;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

/**
 * Represents the definition of a new symbol (Variable, Class, Method, etc.).
 */
public abstract class Declaration extends Statement
{
	protected Declaration(SourceSpan span)
	{
		super(span);
	}
}