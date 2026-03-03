package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * Represents the {@code none} absence literal.
 * <p>
 * {@code none} may only be used where an optional type ({@code T?}) is
 * expected.  The type-checker enforces this constraint; a {@code none} that
 * appears in a non-optional context is a compile-time error.
 *
 * <pre>
 * User? user = none;      // valid — T? context
 * User  user = none;      // compile-time error
 * </pre>
 */
public class NoneExpression extends Expression
{
	public NoneExpression(SourceSpan span)
	{
		super(span);
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitNoneExpression(this);
	}
}
