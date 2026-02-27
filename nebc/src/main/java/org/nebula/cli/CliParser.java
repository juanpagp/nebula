package org.nebula.cli;

import org.nebula.nebc.core.CompilerConfig;
import org.nebula.util.Result;

import java.util.Optional;

/**
 * A custom, dependency-free command-line argument parser for the nebc compiler.
 * This class is responsible for iterating through the raw string arguments,
 * populating a CompilerConfig, and handling immediate-exit commands
 * like --help and --version.
 */
public class CliParser {

	/**
	 * Parses the raw string arguments into a fully validated CompilerConfig.
	 * <p>
	 * This method returns an {@link Optional} containing a {@link Result}:
	 * <ul>
	 * <li><b>{@code Optional.empty()}</b>:
	 * Indicates a clean exit. --help or --version was requested and has been
	 * printed.
	 * The application should exit with code 0.</li>
	 *
	 * <li><b>{@code Optional.of(Result.Ok(config))}</b>:
	 * Indicates a successful parse. The {@link CompilerConfig} is valid and
	 * compilation
	 * can proceed.</li>
	 *
	 * <li><b>{@code Optional.of(Result.Err(error))}</b>:
	 * Indicates a parsing error. The {@link ArgParseError} contains details.
	 * The application should print the error and exit with code 1.</li>
	 * </ul>
	 *
	 * @param args The string arguments from main().
	 * @return An Optional containing a Result, representing the parse outcome.
	 */
	public Optional<Result<CompilerConfig, ArgParseError>> parse(String[] args) {
		CompilerConfig.Builder builder = new CompilerConfig.Builder();

		try {
			// Iterate through the arguments
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];

				// Anything not starting with '-' is a source file
				if (!arg.startsWith("-")) {
					builder.addNebSource(arg);
					continue;
				}

				// Use a switch for simple, fast option matching
				switch (arg) {
					// --- Clean Exit Flags ---
					case "-h":
					case "--help":
						CLIUtils.printHelp();
						return Optional.empty(); // Clean exit

					case "--version":
						CLIUtils.printVersion();
						return Optional.empty(); // Clean exit

					// --- Boolean Flags ---
					case "-v":
					case "--verbose":
						builder.verbose(true);
						break;
					case "-B":
					case "--library":
						builder.compileAsLibrary(true);
						break;
					case "-k":
					case "--check":
						builder.checkOnly(true);
						break;
					case "--ignore-warnings":
						builder.ignoreWarnings(true);
						break;
					case "-s":
					case "--static":
						builder.isStatic(true);
						break;

					// --- Options with a single value ---
					case "-e":
					case "--entry":
						i++; // Move to the next token
						if (i >= args.length) {
							return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.MISSING_VALUE,
									"Error: Option '" + arg + "' requires an argument.")));
						}
						builder.entryPoint(args[i]);
						break;

					case "-o":
					case "--output":
						i++;
						if (i >= args.length) {
							return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.MISSING_VALUE,
									"Error: Option '" + arg + "' requires an argument.")));
						}
						builder.outputFile(args[i]);
						break;

					case "-t":
					case "--target":
						i++;
						if (i >= args.length) {
							return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.MISSING_VALUE,
									"Error: Option '" + arg + "' requires an argument.")));
						}
						builder.targetPlatform(args[i]);
						break;

					case "--borrow-checking":
						i++;
						if (i >= args.length) {
							return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.MISSING_VALUE,
									"Error: Option '" + arg + "' requires an argument.")));
						}
						// This can throw a RuntimeException from the builder, which we catch below
						builder.borrowCheckingLevel(args[i]);
						break;

					// --- Options that can be repeated (lists) ---
					case "-L":
						i++;
						if (i >= args.length) {
							return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.MISSING_VALUE,
									"Error: Option '" + arg + "' requires an argument.")));
						}
						builder.addLibraryPath(args[i]);
						break;

					case "-l":
						i++;
						if (i >= args.length) {
							return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.MISSING_VALUE,
									"Error: Option '" + arg + "' requires an argument.")));
						}
						builder.addLinkLibrary(args[i]);
						break;

					case "-n":
					case "--native":
						i++;
						if (i >= args.length) {
							return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.MISSING_VALUE,
									"Error: Option '" + arg + "' requires an argument.")));
						}
						builder.addNativeSource(args[i]);
						break;

					default:
						// Handle unknown options
						return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.UNKNOWN_OPTION,
								"Error: Unknown option '" + arg + "'")));
				}
			}
		} catch (RuntimeException e) {
			// This catches errors from the builder (e.g., invalid borrow check level)
			return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.INVALID_VALUE, e.getMessage())));
		}

		// ---- VALIDATION ----
		// This is a post-parsing validation check
		if (builder.getNebSources().isEmpty()) {
			return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.MISSING_SOURCE_FILES,
					"Error: Missing required <sourceFiles>.")));
		}

		// All checks passed, build the final, immutable config object
		try {
			// .build() can also throw an error (e.g., invalid file types)
			CompilerConfig config = builder.build();
			return Optional.of(Result.ok(config));
		} catch (Exception e) {
			return Optional.of(Result.err(new ArgParseError(ArgParseError.Type.INVALID_VALUE,
					"Error creating compiler config: " + e.getMessage())));
		}
	}
}