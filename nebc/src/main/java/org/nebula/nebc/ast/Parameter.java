package org.nebula.nebc.ast;

import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.ast.expressions.Expression;

/**
 * Represents a function parameter with optional CVT (Causal Validity Tracking) modifier.
 * 
 * Fields:
 * - cvtModifier: KEEPS, DROPS, or NONE. Only used in extern "C" declarations.
 * - type:        The parameter's type (e.g., i32, Ref<u8>, string)
 * - name:        The parameter's identifier
 * - defaultValue: Optional default value expression
 */
public record Parameter(
		CVTModifier cvtModifier, TypeNode type, String name, Expression defaultValue)
{
	/**
	 * Convenience constructor for parameters without CVT modifiers (normal Nebula functions)
	 */
	public Parameter(TypeNode type, String name, Expression defaultValue)
	{
		this(CVTModifier.NONE, type, name, defaultValue);
	}
}