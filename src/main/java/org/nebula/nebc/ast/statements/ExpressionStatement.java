package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class ExpressionStatement extends Statement
{
	public final Expression expression;

	public ExpressionStatement(SourceSpan span, Expression expression)
	{
		super(span);
		this.expression = expression;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitExpressionStatement(this);
	}
}