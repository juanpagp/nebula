package org.nebula.nebc.error;

import org.nebula.cli.ArgParseError;
import org.nebula.nebc.util.Log;

/**
 * Centralized static mechanism for reporting errors to the user.
 * Separates error data from error display logic.
 */
public class ErrorReporter
{
	// Prevent instantiation
	private ErrorReporter()
	{
	}

	/**
	 * Reports a fatal CLI argument parsing error.
	 *
	 * @param error The data record containing the error details.
	 */
	public static void reportCliError(ArgParseError error)
	{
		// 1. Log the main error message using your existing Log utility
		Log.err(error.message());

		// 2. Apply specific logic for CLI hints (previously inside the Record)
		if (error.type() != ArgParseError.Type.INVALID_VALUE)
		{
			System.err.println("Use 'nebc --help' for a list of available options.");
		}
	}

	/**
	 * Future-proofing: Generic method for compiler internal errors.
	 */
	public static void fatal(String message)
	{
		Log.err("Fatal: " + message);
		// We can choose to exit here, or let the caller handle the exit.
		// Usually, a reporter just reports.
	}
}