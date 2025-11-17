package org.lokray.cli;

/**
 * Represents a specific error that occurred during CLI parsing.
 *
 * @param type    A high-level category for the error.
 * @param message A user-friendly error message.
 */
public record ArgParseError(Type type, String message)
{

	/**
	 * Categorizes the type of parsing error.
	 */
	public enum Type
	{
		/**
		 * An option was provided that is not recognized (e.g., --foo)
		 */
		UNKNOWN_OPTION,

		/**
		 * An option requiring a value was given none (e.g., -o at end of args)
		 */
		MISSING_VALUE,

		/**
		 * An option was given an invalid value (e.g., --borrow-checking=foo)
		 */
		INVALID_VALUE,

		/**
		 * No *.neb source files were provided
		 */
		MISSING_SOURCE_FILES
	}
}