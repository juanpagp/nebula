package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class AssignmentExpression extends Expression
{
	public final Expression target;
	public final String operator; // =, +=, -=, etc.
	public final Expression value;

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