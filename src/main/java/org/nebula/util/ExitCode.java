package org.nebula.util;

/**
 * centralized definitions for application exit codes.
 * This replaces fragile integer literals (0, 1, etc.) with type-safe constants.
 */
public enum ExitCode
{
	SUCCESS(0),
	ARGUMENT_PARSING_ERROR(1),
	IO_ERROR(2),
	SYNTAX_ERROR(3),
	SEMANTIC_ERROR(4),
	FATAL_ERROR(5);

	private final int code;

	ExitCode(int code)
	{
		this.code = code;
	}

	/**
	 * Returns the actual integer code for use with System.exit().
	 */
	public int getCode()
	{
		return code;
	}
}