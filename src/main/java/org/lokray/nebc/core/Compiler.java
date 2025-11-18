package org.lokray.nebc.core;

import org.lokray.nebc.frontend.parser.Parser;
import org.lokray.util.ExitCode;

public class Compiler
{
	private final CompilerConfig config;

	public Compiler(CompilerConfig config)
	{
		this.config = config;
	}

	public ExitCode run()
	{
		// 1. Tokenize
		int tokenizationExitCode = Parser.tokenize();
		if(tokenizationExitCode != 0) return ExitCode.SYNTAX_ERROR;

		

		return ExitCode.SUCCESS;
	}

}