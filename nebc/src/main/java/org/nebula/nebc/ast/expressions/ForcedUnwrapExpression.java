package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * Represents the forced-unwrap postfix operator {@code !}.
 * <p>
 * Forcibly unwraps an optional value.  If the optional is present its
 * inner value is returned; if it is {@code none} a runtime panic is triggered.
 * The {@code !} is an explicit assertion by the programmer that the value is
 * guaranteed to be present.
 *
 * <pre>
 * User user = getUser()!;        // panics at runtime if getUser() == none
 * String name = user!.name;      // forced unwrap before member access
 * </pre>
 *
 * <ul>
 *   <li>{@link #operand} — must have type {@code T?}</li>
 *   <li>Result type — {@code T}</li>
 * </ul>
 */
public class ForcedUnwrapExpression extends Expression
{
	public final Expression operand;

	public ForcedUnwrapExpression(SourceSpan span, Expression operand)
	{
		super(span);
		this.operand = operand;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitForcedUnwrapExpression(this);
	}
}
