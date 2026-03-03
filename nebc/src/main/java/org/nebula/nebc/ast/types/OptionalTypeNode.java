package org.nebula.nebc.ast.types;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * AST representation of an optional type: {@code T?}.
 * <p>
 * Wraps any other {@link TypeNode} to express that the value may be absent
 * ({@code none}).  The compiler enforces that {@code none} is only assignable
 * to optional types, and that optional values may not be accessed via {@code .}
 * without first being unwrapped with {@code ??}, {@code !}, or a
 * flow-sensitive guard.
 *
 * <pre>
 * User?        // optional class
 * i32?         // optional primitive
 * (i32, str)?  // optional tuple                                       
 * </pre>
 */
public class OptionalTypeNode extends TypeNode
{
	/** The inner type being wrapped, e.g. {@code User} inside {@code User?}. */
	public final TypeNode innerType;

	public OptionalTypeNode(SourceSpan span, TypeNode innerType)
	{
		super(span);
		this.innerType = innerType;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTypeReference(this);
	}
}
