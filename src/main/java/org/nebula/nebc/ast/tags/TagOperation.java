package org.nebula.nebc.ast.tags;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class TagOperation extends TagExpression
{
	private final Operator operator;
	private final TagExpression left;
	private final TagExpression right; // Null if unary (NOT)

	public TagOperation(SourceSpan span, Operator operator, TagExpression left, TagExpression right)
	{
		super(span);
		this.operator = operator;
		this.left = left;
		this.right = right;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTagOperation(this);
	}

	public enum Operator
	{INTERSECT, UNION, NOT}
}