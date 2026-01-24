package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class NewExpression extends Expression
{
	public final String typeName;
	public final List<Expression> arguments;

	public NewExpression(SourceSpan span, String typeName, List<Expression> arguments)
	{
		super(span);
		this.typeName = typeName;
		this.arguments = arguments;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitNewExpression(this);
	}
}