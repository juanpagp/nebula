package org.nebula.cli;

import org.nebula.nebc.core.Compiler;
import org.nebula.nebc.core.CompilerConfig;
import org.nebula.nebc.error.ErrorReporter;
import org.nebula.util.ExitCode;
import org.nebula.util.Result;

import java.util.Optional;

public class NebcCli
{
	/**
	 * Orchestrates the CLI lifecycle.
	 *
	 * @return The ExitCode enum representing the outcome.
	 */
	public ExitCode execute(String[] args)
	{
		// 1. Parse
		Optional<Result<CompilerConfig, ArgParseError>> parseResult = CompilerConfig.fromArgs(args);

		// 2. Handle "clean exit" (Help/Version already printed by parser logic)
		if (parseResult.isEmpty())
		{
			return ExitCode.SUCCESS;
		}

		var result = parseResult.get();

		// 3. Handle Errors via the Central Reporter
		if (result.isErr())
		{
			// Pass the raw error record to the static reporter
			ErrorReporter.reportCliError(result.unwrapErr());
			return ExitCode.ARGUMENT_PARSING_ERROR;
		}

		// 4. Run Compiler
		CompilerConfig config = result.unwrap();
		Compiler compiler = new Compiler(config);

		return compiler.run();
	}
}