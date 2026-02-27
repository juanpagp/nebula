package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class MatchArm extends ASTNode
{
	public final Pattern pattern;
	public final Expression result;

	public MatchArm(SourceSpan span, Pattern pattern, Expression result)
	{
		super(span);
		this.pattern = pattern;
		this.result = result;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitMatchArm(this); // You'll need to add this to ASTVisitor
	}
}