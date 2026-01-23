package org.nebula.nebc.ast;

import org.nebula.nebc.ast.types.Type;
import org.nebula.nebc.ast.expressions.Expression;

public record Parameter(Type type, String name, Expression defaultValue)
{
}