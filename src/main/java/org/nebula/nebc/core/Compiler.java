package org.nebula.nebc.core;

import org.nebula.nebc.frontend.parser.Parser;
import org.nebula.util.ExitCode;

public class Compiler
{
	private final CompilerConfig config;

	public Compiler(CompilerConfig config)
	{
		this.config = config;
	}

	public ExitCode run()
	{
		// 1. Frontend: Lexing & Parsing
		// This phase converts source files into Abstract Syntax Trees (via Parse Trees).
		Parser parser = new Parser(config);

		// The parse() method now handles both tokenization and parsing internally.
		int frontendExitCode = parser.parse();

		if (frontendExitCode != 0)
		{
			return ExitCode.SYNTAX_ERROR;
		}

		// TODO: 2. Semantic Analysis (Type checking, symbol resolution)
		// TODO: 3. Code Generation (LLVM IR)

		return ExitCode.SUCCESS;
	}
}