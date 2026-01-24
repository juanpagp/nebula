package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.Type;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class ForeachStatement extends Statement
{
	public final Type variableType; // null if 'var'
	public final String variableName;
	public final Expression iterable;
	public final Statement body;

	public ForeachStatement(SourceSpan span, Type variableType, String variableName, Expression iterable, Statement body)
	{
		super(span);
		this.variableType = variableType;
		this.variableName = variableName;
		this.iterable = iterable;
		this.body = body;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitForeachStatement(this);
	}
}