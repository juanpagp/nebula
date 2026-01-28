package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

/**
 * Represents a sequence of statements followed by an optional trailing expression.
 * In Nebula, a block is an expression. If no tail exists, it evaluates to void and is discarded.
 */
public class StatementBlock extends Statement
{
	public final List<Statement> statements;

	public StatementBlock(SourceSpan span, List<Statement> statements)
	{
		super(span);
		this.statements = statements;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitStatementBlock(this);
	}
}