package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * Represents a null-coalescing expression: {@code left ?? right}.
 * <p>
 * Evaluates to {@code left} when it is non-null/non-nil, otherwise
 * evaluates and returns {@code right}.
 *
 * <pre>
 * var name = maybeNull ?? "default"
 * </pre>
 */
public class NullCoalescingExpression extends Expression
{
	public final Expression left;
	public final Expression right;

	public NullCoalescingExpression(SourceSpan span, Expression left, Expression right)
	{
		super(span);
		this.left = left;
		this.right = right;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitNullCoalescingExpression(this);
	}
}
