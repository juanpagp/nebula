package org.nebula.nebc.ast;

import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.ast.expressions.Expression;

public record Parameter(
		TypeNode type, String name, Expression defaultValue)
{
}