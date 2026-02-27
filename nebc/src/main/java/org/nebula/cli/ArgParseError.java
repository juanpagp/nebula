package org.nebula.cli;

/**
 * A pure data carrier for CLI parsing errors.
 * Display logic is handled by {@link org.nebula.nebc.error.ErrorReporter}.
 */
public record ArgParseError(Type type, String message)
{
	public enum Type
	{
		UNKNOWN_OPTION,
		MISSING_VALUE,
		INVALID_VALUE,
		MISSING_SOURCE_FILES
	}
}