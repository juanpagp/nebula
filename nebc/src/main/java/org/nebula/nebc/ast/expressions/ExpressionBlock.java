package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.statements.Statement;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

/**
 * Represents a sequence of statements followed by an optional trailing expression.
 * In Nebula, a block is an expression. If no tail exists, it evaluates to void and is discarded.
 */
public class ExpressionBlock extends Expression
{
	public final List<Statement> statements;
	public final Expression tail; // The optional result expression (block_tail)

	public ExpressionBlock(SourceSpan span, List<Statement> statements, Expression tail)
	{
		super(span);
		this.statements = statements;
		this.tail = tail;
	}

	public boolean hasTail()
	{
		return tail != null;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitExpressionBlock(this);
	}
}