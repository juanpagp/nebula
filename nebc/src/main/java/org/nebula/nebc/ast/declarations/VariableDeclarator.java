package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.expressions.Expression;

/**
 * Represents a single variable name and its optional initializer.
 * Example: 'x = 10' in 'int x = 10, y;'
 */
public record VariableDeclarator(
		String name,
		Expression initializer)
{
	public boolean hasInitializer()
	{
		return initializer != null;
	}
}