package org.nebula.nebc.semantic;

import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public record SemanticError(String message, SourceSpan span)
{
	public String toString()
	{
		return "[Semantic Error] " + span.file() + ":" + span.startLine() + " - " + message;
	}
}