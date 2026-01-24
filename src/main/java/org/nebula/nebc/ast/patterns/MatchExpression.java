package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.Collections;
import java.util.List;

public class MatchExpression extends Expression
{
	public final Expression selector;
	public final List<MatchArm> arms;

	public MatchExpression(SourceSpan span, Expression selector, List<MatchArm> arms)
	{
		super(span);
		this.selector = selector;
		this.arms = arms;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitMatchExpression(this);
	}
}