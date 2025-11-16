package org.lokray.nebc.core;

import org.lokray.nebc.cli.ArgsParser;
import org.lokray.nebc.io.SourceFile;
import picocli.CommandLine;

import java.io.File;
import java.util.List;

/**
 * Immutable configuration object for the compiler pipeline.
 */
public record CompilerConfig(
		boolean helpRequested,
		boolean versionRequested,

		List<SourceFile> nebSources,
		List<SourceFile> nebLibraries,
		List<SourceFile> nativeSources,

		String entryPoint,
		String outputFile,
		List<File> librarySearchPaths,
		String targetPlatform,

		boolean verbose,
		boolean compileAsLibrary,
		boolean checkOnly,
		boolean ignoreWarnings,
		ArgsParser.BorrowCKLevel borrowCheckingLevel
)
{

	@Override
	public String toString()
	{
		return "CompilerConfig{" +
				"nebSources=" + nebSources +
				", nebLibraries=" + nebLibraries +
				", nativeSources=" + nativeSources +
				", entryPoint='" + entryPoint + '\'' +
				", outputFile='" + outputFile + '\'' +
				", borrowChecking=" + borrowCheckingLevel +
				'}';
	}

	/**
	 * Parses CLI arguments into a fully validated {@link CompilerConfig}.
	 */
	public static CompilerConfig fromArgs(String[] args)
	{
		ArgsParser parsed = new ArgsParser();
		CommandLine cli = new CommandLine(parsed);
		cli.parseArgs(args);
		return parsed.toCompilerArguments();
	}
}