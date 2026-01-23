package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class AssignmentExpression extends Expression
{
	private final Expression target;
	private final String operator; // =, +=, -=, etc.
	private final Expression value;

	public AssignmentExpression(SourceSpan span, Expression target, String operator, Expression value)
	{
		super(span);
		this.target = target;
		this.operator = operator;
		this.value = value;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitAssignmentExpression(this);
	}
}