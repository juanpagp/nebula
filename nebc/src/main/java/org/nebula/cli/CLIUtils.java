package org.nebula.cli;

import org.nebula.nebc.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Collectors;

public class CLIUtils
{
	// These fields will be populated from compiler.properties
	public static final String NAME;
	public static final String VERSION;

	// This field will be populated from nebc-help.txt
	public static final String HELP_MESSAGE;

	// Static initializer block to load properties and help message once.
	static
	{
		// --- Load compiler.properties ---
		Properties props = new Properties();
		String propsFile = "/compiler.properties";
		try (InputStream is = CLIUtils.class.getResourceAsStream(propsFile))
		{
			if (is == null)
			{
				throw new RuntimeException("Fatal: Could not find " + propsFile + " in classpath.");
			}
			props.load(is);
		}
		catch (Exception e)
		{
			// This is a fatal startup error
			Log.err("Fatal: Could not load compiler properties.");
			throw new RuntimeException(e);
		}

		NAME = props.getProperty("compiler.name", "nebc");
		VERSION = props.getProperty("compiler.version", "unknown");

		// --- Load nebc-help.txt ---
		String helpFile = "/info/nebc-help.txt";
		try (InputStream is = CLIUtils.class.getResourceAsStream(helpFile))
		{
			if (is == null)
			{
				throw new RuntimeException("Fatal: Could not find " + helpFile + " in classpath.");
			}
			// Read the entire file into a single string
			HELP_MESSAGE = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
					.lines()
					.collect(Collectors.joining("\n"));

		}
		catch (Exception e)
		{
			Log.err("Fatal: Could not load help file.");
			throw new RuntimeException(e);
		}
	}

	/**
	 * Prints the compiler name and version.
	 */
	public static void printVersion()
	{
		System.out.println(NAME + " " + VERSION);
	}

	/**
	 * Prints the formatted help text.
	 */
	public static void printHelp()
	{
		// Print the help message, replacing a placeholder with the dynamic version
		System.out.println(
				HELP_MESSAGE.replace("{{VERSION}}", VERSION));
	}
}