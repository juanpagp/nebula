package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * Represents a single interpolation hole that has a format specifier,
 * e.g. the {@code {i:000}} part of {@code $"Value: {i:000}"}.
 *
 * <p>The {@link #expression} is the value to format, and {@link #formatSpec}
 * is the raw format string after the colon (e.g. {@code "000"}).
 */
public class FormattedInterpolationExpression extends Expression
{
    public final Expression expression;
    public final String     formatSpec;

    public FormattedInterpolationExpression(SourceSpan span, Expression expression, String formatSpec)
    {
        super(span);
        this.expression = expression;
        this.formatSpec = formatSpec;
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor)
    {
        return visitor.visitFormattedInterpolationExpression(this);
    }
}
