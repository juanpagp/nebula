package org.nebula.nebc.core;

import org.nebula.cli.ArgParseError;
import org.nebula.cli.CliParser;
import org.nebula.nebc.io.FileType;
import org.nebula.nebc.io.SourceFile;
import org.nebula.util.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Immutable configuration object for the compiler pipeline.
 */
public record CompilerConfig(
								// Note: helpRequested and versionRequested are removed,
								// as the parser handles them directly as exit-early commands.

								List<SourceFile> nebSources, List<SourceFile> nebLibraries, List<SourceFile> symbolFiles, List<SourceFile> nativeSources,

								String entryPoint, String outputFile, List<File> librarySearchPaths, String targetPlatform,

								boolean verbose, boolean compileAsLibrary, boolean checkOnly, boolean ignoreWarnings, CompilerConfig.BorrowCKLevel borrowCheckingLevel, boolean isStatic, boolean useStdLib)
{

	/**
	 * Parses CLI arguments into a fully validated {@link CompilerConfig}.
	 * This is the single entry point for configuration.
	 *
	 * @param args The raw arguments from main().
	 * @return An Optional containing a Result:
	 *         - Empty: Clean exit (help/version).
	 *         - Present(Ok(config)): Successful parse.
	 *         - Present(Err(error)): Parsing error.
	 */
	public static Optional<Result<CompilerConfig, ArgParseError>> fromArgs(String[] args)
	{
		CliParser parser = new CliParser();
		// The parser handles all logic, including printing errors or help.
		return parser.parse(args);
	}

	@Override
	public String toString()
	{
		return "CompilerConfig{" + "nebSources=" + nebSources + ", nebLibraries=" + nebLibraries + ", symbolFiles=" + symbolFiles + ", nativeSources=" + nativeSources + ", entryPoint='" + entryPoint + '\'' + ", outputFile='" + outputFile + '\'' + ", borrowChecking=" + borrowCheckingLevel + ", isStatic=" + isStatic + ", useStdLib=" + useStdLib + '}';
	}

	/**
	 * Defines the supported borrow checking levels.
	 */
	public enum BorrowCKLevel
	{
		none, allowed, strict
	}

	/**
	 * A mutable builder for creating an immutable CompilerConfig.
	 * This isolates the complex construction logic from the parser.
	 */
	public static class Builder
	{
		private final List<String> rawNebSources = new ArrayList<>();
		private final List<String> rawLinkLibraries = new ArrayList<>();
		private final List<String> rawSymbolFiles = new ArrayList<>();
		private final List<String> rawNativeSources = new ArrayList<>();
		private final List<String> rawLibraryPaths = new ArrayList<>();

		private String entryPoint = null;
		private String outputFile = null;
		private String targetPlatform = null;

		private boolean verbose = false;
		private boolean compileAsLibrary = false;
		private boolean checkOnly = false;
		private boolean ignoreWarnings = false;
		private BorrowCKLevel borrowCheckingLevel = BorrowCKLevel.allowed; // Default
		private boolean isStatic = false;
		private boolean useStdLib = true;

		// --- Getters for validation ---
		public List<String> getNebSources()
		{
			return rawNebSources;
		}

		// --- Setters for raw string values ---
		public void addNebSource(String path)
		{
			this.rawNebSources.add(path);
		}

		public void entryPoint(String entry)
		{
			this.entryPoint = entry;
		}

		public void outputFile(String file)
		{
			this.outputFile = file;
		}

		public void targetPlatform(String target)
		{
			this.targetPlatform = target;
		}

		public void addLibraryPath(String path)
		{
			this.rawLibraryPaths.add(path);
		}

		public void addLinkLibrary(String file)
		{
			this.rawLinkLibraries.add(file);
		}

		public void addSymbolFile(String file)
		{
			this.rawSymbolFiles.add(file);
		}

		public void addNativeSource(String file)
		{
			this.rawNativeSources.add(file);
		}

		// --- Setters for flags ---
		public void verbose(boolean v)
		{
			this.verbose = v;
		}

		public void compileAsLibrary(boolean v)
		{
			this.compileAsLibrary = v;
		}

		public void checkOnly(boolean v)
		{
			this.checkOnly = v;
		}

		public void ignoreWarnings(boolean v)
		{
			this.ignoreWarnings = v;
		}

		public void isStatic(boolean v)
		{
			this.isStatic = v;
		}

		public void useStdLib(boolean v)
		{
			this.useStdLib = v;
		}

		public void borrowCheckingLevel(String level)
		{
			try
			{
				this.borrowCheckingLevel = BorrowCKLevel.valueOf(level.toLowerCase());
			}
			catch (IllegalArgumentException e)
			{
				// Re-throw as a parse exception the parser can catch
				throw new RuntimeException("Error: Invalid value for --borrow-checking: '" + level + "'. " + "Expected 'none', 'allowed', or 'strict'.");
			}
		}

		/**
		 * Performs the final conversion from raw strings to domain objects
		 * (like SourceFile) and builds the immutable CompilerConfig.
		 * This logic was previously in ArgsParser.toCompilerArguments().
		 */
		public CompilerConfig build()
		{
			List<SourceFile> nebSources = new ArrayList<>();
			for (String path : rawNebSources)
			{
				File file = new File(path);
				if (file.isDirectory())
				{
					scanDirectoryRecursively(file, nebSources);
				}
				else
				{
					SourceFile sf = new SourceFile(path);
					if (sf.type() == FileType.NEBULA_SOURCE)
					{
						nebSources.add(sf);
					}
					else
					{
						throw new RuntimeException("Error: Invalid source file provided. Expected *.neb file: " + path);
					}
				}
			}

			List<SourceFile> nebLibrariesList = rawLinkLibraries.stream().map(SourceFile::new).toList();

			List<SourceFile> symbolFilesList = rawSymbolFiles.stream().map(SourceFile::new).toList();

			List<SourceFile> nativeSourcesList = rawNativeSources.stream().map(SourceFile::new).filter(sf -> sf.type() == FileType.NATIVE_SOURCE || sf.type() == FileType.NATIVE_HEADER).toList();

			List<File> libraryDirs = rawLibraryPaths.stream().map(File::new).collect(Collectors.toList());

			return new CompilerConfig(
					nebSources, nebLibrariesList, symbolFilesList, nativeSourcesList, entryPoint, outputFile, libraryDirs, targetPlatform, verbose, compileAsLibrary, checkOnly, ignoreWarnings, borrowCheckingLevel, isStatic, useStdLib);
		}

		private void scanDirectoryRecursively(File dir, List<SourceFile> sources)
		{
			File[] files = dir.listFiles();
			if (files == null)
				return;
			for (File f : files)
			{
				if (f.isDirectory())
				{
					scanDirectoryRecursively(f, sources);
				}
				else if (f.getName().endsWith(".neb"))
				{
					sources.add(new SourceFile(f.getAbsolutePath()));
				}
			}
		}
	}
}