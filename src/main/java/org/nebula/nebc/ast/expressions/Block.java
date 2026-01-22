package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.statements.Statement;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a sequence of statements followed by an optional trailing expression.
 * In Nebula, a block is an expression. If no tail exists, it evaluates to void and is discarded.
 */
public class Block extends Expression
{
	private final List<Statement> statements;
	private final Expression tail; // The optional result expression (block_tail)

	public Block(SourceSpan span, List<Statement> statements, Expression tail)
	{
		super(span);
		this.statements = statements;
		this.tail = tail;
	}

	public List<Statement> getStatements()
	{
		return Collections.unmodifiableList(statements);
	}

	/**
	 * @return The trailing expression, if it exists.
	 */
	public Optional<Expression> getTail()
	{
		return Optional.ofNullable(tail);
	}

	public boolean hasTail()
	{
		return tail != null;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitBlock(this);
	}
}