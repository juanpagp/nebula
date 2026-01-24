package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.Collections;
import java.util.List;

/**
 * Represents a disjunction of patterns.
 * Example: `match x { 1 | 2 | 3 => ... }`
 */
public class OrPattern extends Pattern
{
	private final List<Pattern> alternatives;

	public OrPattern(SourceSpan span, List<Pattern> alternatives)
	{
		super(span);
		this.alternatives = alternatives;
	}

	public List<Pattern> getAlternatives()
	{
		return Collections.unmodifiableList(alternatives);
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitOrPattern(this);
	}
}