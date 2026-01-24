package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class MatchArm extends ASTNode
{
	private final Pattern pattern;
	private final Expression result;

	public MatchArm(SourceSpan span, Pattern pattern, Expression result)
	{
		super(span);
		this.pattern = pattern;
		this.result = result;
	}

	public Pattern getPattern()
	{
		return pattern;
	}

	public Expression getResult()
	{
		return result;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitMatchArm(this); // You'll need to add this to ASTVisitor
	}
}