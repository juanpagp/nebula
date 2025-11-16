package org.lokray.nebc.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;

public class Compiler
{

	private final CompilerConfig config;

	public Compiler(CompilerConfig config)
	{
		this.config = config;
		System.out.println();
	}

	public int run()
	{
		// ---- HELP ----
		if (config.helpRequested())
		{
			printHelp();
			return 0;
		}

		// ---- VERSION ----
		if (config.versionRequested())
		{
			System.out.println("nebc v0.0.1 pre-alpha");
			return 0;
		}

		// ---- VALIDATION ----
		if (config.nebSources() == null || config.nebSources().isEmpty())
		{
			System.err.println("Error: Missing required parameter: '<sourceFiles>'");
			return 1;
		}

		// ---- NORMAL COMPILATION ----
		System.out.println("--- Nebula Compiler 1.0 ---");
		System.out.println("Starting compilation process...");

		if (config.verbose())
		{
			System.out.println("Verbose mode enabled.");
		}
		if (config.checkOnly())
		{
			System.out.println("Running semantic analysis only.");
		}

		System.out.println("Nebula Sources: " + config.nebSources());
		System.out.println("Libraries: " + config.nebLibraries());
		System.out.println("Native Sources: " + config.nativeSources());
		System.out.println("Library Search Paths: " + config.librarySearchPaths());
		System.out.println("Entry Point: " + (config.entryPoint() == null ? "N/A" : config.entryPoint()));
		System.out.println("Output File: " + (config.outputFile() == null ? "Default" : config.outputFile()));
		System.out.println("Target Platform: " + (config.targetPlatform() == null ? "Default" : config.targetPlatform()));
		System.out.println("Borrow Checking Level: " + config.borrowCheckingLevel());
		System.out.println("Compile as Library: " + config.compileAsLibrary());

		System.out.println("Compilation successful! (Exit Code 0)");
		return 0;
	}

	private void printHelp()
	{
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Compiler.class.getResourceAsStream("/nebc-help.txt")))))
		{
			reader.lines().forEach(System.out::println);
		}
		catch (Exception e)
		{
			System.err.println("Error: Could not load help file.");
		}
	}
}