package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class WhileStatement extends Statement
{
	public final Expression condition;
	public final Statement body;

	public WhileStatement(SourceSpan span, Expression condition, Statement body)
	{
		super(span);
		this.condition = condition;
		this.body = body;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitWhileStatement(this);
	}
}
