package org.lokray.nebc.cli;

import org.lokray.nebc.io.FileType;
import org.lokray.nebc.io.SourceFile;
import org.lokray.nebc.core.CompilerConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CLI argument model for picocli. Only stores raw values.
 * Conversion to typed {@link SourceFile} objects happens in toCompilerArguments().
 */
@Command(name = "nebc", version = "nebc v0.0.1 pre-alpha", description = "Nebula compiler.")
public class ArgsParser
{

	@Option(names = {"-h", "--help"})
	boolean helpRequested;

	@Option(names = {"-v", "--version"})
	boolean versionRequested;

	@Parameters(index = "0", arity = "0..*", description = "The source files (*.neb) to compile.")
	List<File> sourceFiles;

	@Option(names = {"-e", "--entry"}, paramLabel = "<class.name>")
	String entryPoint;

	@Option(names = {"-o", "--output"}, paramLabel = "<file>")
	String outputFile;

	@Option(names = "-L", paramLabel = "<path>")
	String[] libraryPaths;

	@Option(names = "-l", paramLabel = "<file>")
	String[] linkLibraries;

	@Option(names = {"-t", "--target"}, paramLabel = "<platform>")
	String targetPlatform;

	@Option(names = {"-n", "--native"}, paramLabel = "<files...>", arity = "1..*")
	String[] nativeSources;

	@Option(names = "--verbose")
	boolean verbose;

	@Option(names = {"-B", "--library"})
	boolean compileAsLibrary;

	@Option(names = {"-k", "--check"})
	boolean checkOnly;

	@Option(names = "--ignore-warnings")
	boolean ignoreWarnings;

	@Option(names = "--borrow-checking", paramLabel = "<level>",
			defaultValue = "allowed")
	BorrowCKLevel borrowCheckingLevel;

	/**
	 * Converts raw CLI values into categorized, validated lists of {@link SourceFile}.
	 */
	public CompilerConfig toCompilerArguments()
	{

		List<SourceFile> nebSources =
				sourceFiles == null ? List.of()
						: sourceFiles.stream()
						.map(f -> new SourceFile(f.getPath()))
						.filter(sf -> sf.type() == FileType.NEBULA_SOURCE)
						.toList();

		List<SourceFile> nebLibrariesList =
				linkLibraries == null ? List.of()
						: Arrays.stream(linkLibraries)
						.map(SourceFile::new)
						.filter(sf -> sf.type() == FileType.NEBULA_LIBRARY)
						.toList();

		List<SourceFile> nativeSourcesList =
				nativeSources == null ? List.of()
						: Arrays.stream(nativeSources)
						.map(SourceFile::new)
						.filter(sf -> sf.type() == FileType.NATIVE_SOURCE || sf.type() == FileType.NATIVE_HEADER)
						.toList();

		List<File> libraryDirs =
				libraryPaths == null ? List.of()
						: Arrays.stream(libraryPaths)
						.map(File::new)
						.collect(Collectors.toList());

		return new CompilerConfig(
				helpRequested,
				versionRequested,

				nebSources,
				nebLibrariesList,
				nativeSourcesList,

				entryPoint,
				outputFile,
				libraryDirs,
				targetPlatform,

				verbose,
				compileAsLibrary,
				checkOnly,
				ignoreWarnings,
				borrowCheckingLevel
		);
	}

	public enum BorrowCKLevel
	{
		none, allowed, strict
	}
}