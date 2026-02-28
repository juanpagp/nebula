package org.nebula.nebc.ast;

import org.nebula.nebc.ast.types.TypeNode;

/**
 * Represents a single generic type parameter in a declaration.
 * <p>
 * Examples:
 * <ul>
 * <li>{@code <T>} → name = "T", bound = null</li>
 * <li>{@code <T: Displayable>} → name = "T", bound =
 * NamedType("Displayable")</li>
 * </ul>
 */
public record GenericParam(String name, TypeNode bound)
{
	/**
	 * {@code true} when this parameter has a trait bound.
	 */
	public boolean hasBound()
	{
		return bound != null;
	}
}
