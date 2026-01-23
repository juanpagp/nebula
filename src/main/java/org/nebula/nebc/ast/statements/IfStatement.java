package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class IfStatement extends Statement
{
	private final Expression condition;
	private final Statement thenBranch;
	private final Statement elseBranch;

	public IfStatement(SourceSpan span, Expression condition, Statement thenBranch, Statement elseBranch)
	{
		super(span);
		this.condition = condition;
		this.thenBranch = thenBranch;
		this.elseBranch = elseBranch;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitIfStatement(this);
	}
}