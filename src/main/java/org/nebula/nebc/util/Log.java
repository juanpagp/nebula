package org.nebula.nebc.util;

/**
 * Simple static logging utility for the CLI/runtime.
 * Not used for compiler diagnostics.
 */
public final class Log
{
	// prevent instantiation
	private Log()
	{
	}

	public enum Level
	{
		ERROR,
		WARNING,
		INFO,
		DEBUG
	}

	// Default log level (changeable)
	private static Level currentLevel = Level.INFO;

	public static void setLevel(Level level)
	{
		currentLevel = level;
	}

	public static void err(String msg)
	{
		// Cannot hide errors
		System.err.println("[ERR] " + msg);
	}

	public static void warn(String msg)
	{
		if (currentLevel.ordinal() >= Level.WARNING.ordinal())
		{
			System.out.println("[WAR] " + msg);
		}
	}

	public static void info(String msg)
	{
		if (currentLevel.ordinal() >= Level.INFO.ordinal())
		{
			System.out.println("[INF] " + msg);
		}
	}

	public static void debug(String msg)
	{
		if (currentLevel.ordinal() >= Level.DEBUG.ordinal())
		{
			System.out.println("[DEB] " + msg);
		}
	}
}