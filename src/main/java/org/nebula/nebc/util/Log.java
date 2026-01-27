package org.nebula.nebc.util;

/**
 * Simple static logging utility for the CLI/runtime.
 * Not used for compiler diagnostics.
 */
public final class Log
{
	public static final String RED = "\u001B[31m";
	public static final String RESET = "\u001B[0m";
	// Default log level (changeable)
	private static Level currentLevel = Level.INFO;

	// prevent instantiation
	private Log()
	{
	}

	public static void setLevel(Level level)
	{
		currentLevel = level;
	}

	public static void err(String msg)
	{
		System.err.println(RED + "[ERR] " + msg + RESET);
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

	public enum Level
	{
		ERROR,
		WARNING,
		INFO,
		DEBUG
	}
}