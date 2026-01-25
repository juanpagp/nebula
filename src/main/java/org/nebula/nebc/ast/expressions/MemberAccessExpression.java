package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class MemberAccessExpression extends Expression
{
	public final Expression target;
	public final String memberName;

	public MemberAccessExpression(SourceSpan span, Expression target, String memberName)
	{
		super(span);
		this.target = target;
		this.memberName = memberName;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitMemberAccessExpression(this);
	}
}