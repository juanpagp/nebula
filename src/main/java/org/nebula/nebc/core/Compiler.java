package org.nebula.nebc.core;

import org.nebula.nebc.ast.ASTBuilder;
import org.nebula.nebc.ast.CompilationUnit;
import org.nebula.nebc.codegen.CodegenException;
import org.nebula.nebc.codegen.LLVMCodeGenerator;
import org.nebula.nebc.codegen.NativeCompiler;
import org.nebula.nebc.frontend.diagnostic.Diagnostic;
import org.nebula.nebc.frontend.parser.Parser;
import org.nebula.nebc.semantic.SemanticAnalyzer;
import org.nebula.nebc.util.Log;
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
		// This phase converts source files into Abstract Syntax Trees (via Parse
		// Trees).
		Parser parser = new Parser(config);
		int frontendExitCode = parser.parse();
		if (frontendExitCode != 0)
			return ExitCode.SYNTAX_ERROR;

		// 2. Build the AST for each parse tree
		this.compilationUnits = ASTBuilder.buildAST(parser.getParsingResultList());
		for (var cu : compilationUnits)
		{
			System.out.println(cu);
		}

		// 3. Semantic Analysis (Type checking, symbol resolution)
		SemanticAnalyzer analyzer = new SemanticAnalyzer(config);
		for (var cu : compilationUnits)
		{
			List<Diagnostic> errors = analyzer.analyze(cu);
			if (!errors.isEmpty())
			{
				for (var e : errors)
					Log.err(e.toString());
				return ExitCode.SEMANTIC_ERROR;
			}
		}

		// Skip codegen if --check-only was specified
		if (config.checkOnly())
		{
			Log.info("Check-only mode: skipping code generation.");
			return ExitCode.SUCCESS;
		}

		// 4. Validate entry point (unless compiling as a library)
		if (!config.compileAsLibrary() && analyzer.getMainMethod() == null)
		{
			Diagnostic d = Diagnostic.of(org.nebula.nebc.frontend.diagnostic.DiagnosticCode.MISSING_MAIN_METHOD,
					org.nebula.nebc.frontend.diagnostic.SourceSpan.unknown());
			Log.err(d.toString());
			return ExitCode.CODEGEN_ERROR;
		}

		// 5. Code Generation (LLVM IR)
		LLVMCodeGenerator codegen = new LLVMCodeGenerator();
		try
		{
			codegen.generate(compilationUnits, analyzer);

			// Print LLVM IR in verbose mode
			if (config.verbose())
			{
				Log.info("=== LLVM IR ===");
				System.out.println(codegen.dumpIR());
				Log.info("=== END IR ===");
			}

			// 6. Emit native binary
			String outputPath = config.outputFile() != null ? config.outputFile() : "a.out";
			NativeCompiler.compile(codegen.getModule(), outputPath, config.targetPlatform());

			Log.info("Compiled successfully: " + outputPath);
			return ExitCode.SUCCESS;
		}
		catch (CodegenException e)
		{
			Log.err("Code generation failed: " + e.getMessage());
			return ExitCode.CODEGEN_ERROR;
		}
		finally
		{
			codegen.dispose();
		}
	}
}