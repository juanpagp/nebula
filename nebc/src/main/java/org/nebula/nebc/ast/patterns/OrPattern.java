package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

/**
 * Represents a disjunction of patterns.
 * Example: `match x { 1 | 2 | 3 => ... }`
 */
public class OrPattern extends Pattern
{
	public final List<Pattern> alternatives;

	public OrPattern(SourceSpan span, List<Pattern> alternatives)
	{
		super(span);
		this.alternatives = alternatives;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitOrPattern(this);
	}
}