package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class ThisExpression extends Expression
{
	public ThisExpression(SourceSpan span)
	{
		super(span);
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitThisExpression(this);
	}
}