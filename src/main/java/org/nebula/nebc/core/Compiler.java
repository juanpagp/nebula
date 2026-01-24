package org.nebula.nebc.core;

import org.nebula.nebc.ast.ASTBuilder;
import org.nebula.nebc.ast.CompilationUnit;
import org.nebula.nebc.frontend.parser.Parser;
import org.nebula.util.ExitCode;

import java.util.List;

public class Compiler
{
	private final CompilerConfig config;
	private List<CompilationUnit> compilationUnits;

	public Compiler(CompilerConfig config)
	{
		this.config = config;
	}

	public ExitCode run()
	{
		// 1. Frontend: Lexing & Parsing
		// This phase converts source files into Abstract Syntax Trees (via Parse Trees).
		Parser parser = new Parser(config);
		int frontendExitCode = parser.parse();
		if (frontendExitCode != 0) return ExitCode.SYNTAX_ERROR;

		// 2. Build the AST for each parse tree
		this.compilationUnits = ASTBuilder.buildAST(parser.getParsingResultList());
		for (var cu : compilationUnits)
		{
			System.out.println(cu);
		}

		// TODO: 2. Semantic Analysis (Type checking, symbol resolution)
		// TODO: 3. Code Generation (LLVM IR)

		return ExitCode.SUCCESS;
	}
}