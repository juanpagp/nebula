package org.lokray;

import org.lokray.cli.ArgParseError;
import org.lokray.nebc.core.Compiler;
import org.lokray.nebc.core.CompilerConfig;

public class Main
{
	public static void main(String[] args)
	{
		var parseResult = CompilerConfig.fromArgs(args);

		if (parseResult.isEmpty())
		{
			System.exit(0);
			return;
		}

		var result = parseResult.get();

		// Parse Error
		if (result.isErr())
		{
			ArgParseError error = result.unwrapErr();
			System.err.println(error.message());

			// Only print the "use --help" for specific, correctable errors
			if (error.type() != ArgParseError.Type.INVALID_VALUE)
			{
				System.err.println("Use 'nebc --help' for a list of available options.");
			}
			System.exit(1);
			return;
		}

		CompilerConfig config = result.unwrap();

		Compiler compilerPipeline = new Compiler(config);
		int exitCode = compilerPipeline.run();

		System.exit(exitCode);
	}
}