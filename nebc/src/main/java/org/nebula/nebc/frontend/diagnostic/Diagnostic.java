package org.nebula.nebc.frontend.diagnostic;

public record Diagnostic(DiagnosticCode code, String message, SourceSpan span)
{
	public static Diagnostic of(DiagnosticCode code, SourceSpan span, Object... args)
	{
		return new Diagnostic(code, code.format(args), span);
	}

	@Override
	public String toString()
	{
		return span.file() + ":" + span.startLine() + " - " + message;
	}
}
