package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class ReturnStatement extends Statement
{
	public final Expression value;

	public ReturnStatement(SourceSpan span, Expression value)
	{
		super(span);
		this.value = value;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitReturnStatement(this);
	}
}