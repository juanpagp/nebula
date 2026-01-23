package org.nebula.nebc.ast.expressions;

public record MatchArm(String patternLabel, Expression result)
{
}