package org.nebula.nebc.frontend.diagnostics;

public record SourceSpan(
		String file,
		int startLine,
		int startCol,
		int endLine,
		int endCol
)
{
	public static SourceSpan unknown()
	{
		return new SourceSpan("<unknown>", 0, 0, 0, 0);
	}
}
