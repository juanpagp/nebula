package org.lokray.cli;

import org.lokray.nebc.core.Compiler;
import org.lokray.nebc.core.CompilerConfig;
import org.lokray.nebc.error.ErrorReporter; // Import the new reporter
import org.lokray.util.Result;

import java.util.Optional;

public class NebcCli
{
    /**
     * Orchestrates the CLI lifecycle.
     * @return The exit code (0 for success, 1 for failure).
     */
    public int execute(String[] args)
    {
        // 1. Parse
        Optional<Result<CompilerConfig, ArgParseError>> parseResult = CompilerConfig.fromArgs(args);

        // 2. Handle "clean exit" (Help/Version already printed by parser logic)
        if (parseResult.isEmpty())
        {
            return 0;
        }

        var result = parseResult.get();

        // 3. Handle Errors via the Central Reporter
        if (result.isErr())
        {
            // Pass the raw error record to the static reporter
            ErrorReporter.reportCliError(result.unwrapErr());
            return 1;
        }

        // 4. Run Compiler
        CompilerConfig config = result.unwrap();
        Compiler compiler = new Compiler(config);

        return compiler.run();
    }
}