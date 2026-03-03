package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

/**
 * Represents a destructuring match pattern for tagged-union variant payloads.
 * <p>
 * Syntax: {@code VariantName(binding1, binding2, ...)}
 * <p>
 * Example:
 * <pre>
 * match (status) {
 *     Running(progress) => { ... },
 *     Completed(result) => { ... },
 *     _                 => {}
 * }
 * </pre>
 *
 * <ul>
 *   <li>{@link #variantName} — the name of the union variant being matched.</li>
 *   <li>{@link #bindings}   — the identifiers bound to the payload fields in
 *       declaration order.</li>
 * </ul>
 */
public class DestructuringPattern extends Pattern
{
	public final String variantName;
	public final List<String> bindings;

	public DestructuringPattern(SourceSpan span, String variantName, List<String> bindings)
	{
		super(span);
		this.variantName = variantName;
		this.bindings = bindings;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitDestructuringPattern(this);
	}
}
