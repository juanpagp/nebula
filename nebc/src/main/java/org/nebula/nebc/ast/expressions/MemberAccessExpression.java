package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class MemberAccessExpression extends Expression
{
	public final Expression target;
	public final String memberName;
	/**
	 * {@code true} when this access was written as {@code expr?.member}
	 * (optional chaining).  The result type is then {@code MemberType?}.
	 */
	public final boolean isSafe;

	public MemberAccessExpression(SourceSpan span, Expression target, String memberName)
	{
		this(span, target, memberName, false);
	}

	public MemberAccessExpression(SourceSpan span, Expression target, String memberName, boolean isSafe)
	{
		super(span);
		this.target = target;
		this.memberName = memberName;
		this.isSafe = isSafe;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitMemberAccessExpression(this);
	}
}