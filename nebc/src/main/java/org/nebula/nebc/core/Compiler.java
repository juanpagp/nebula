package org.nebula.nebc.core;

import org.nebula.nebc.ast.ASTBuilder;
import org.nebula.nebc.ast.CompilationUnit;
import org.nebula.nebc.codegen.CodegenException;
import org.nebula.nebc.codegen.LLVMCodeGenerator;
import org.nebula.nebc.codegen.NativeCompiler;
import org.nebula.nebc.frontend.diagnostic.Diagnostic;
import org.nebula.nebc.frontend.parser.Parser;
import org.nebula.nebc.io.SourceFile;
import org.nebula.nebc.semantic.SemanticAnalyzer;
import org.nebula.nebc.util.Log;
import org.nebula.util.ExitCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Compiler {
	private final CompilerConfig config;
	private List<CompilationUnit> compilationUnits;

	public Compiler(CompilerConfig config) {
		this.config = config;
	}

	public ExitCode run() {
		// 1. Frontend: Lexing & Parsing
		List<SourceFile> stdLib = discoverStdLib();
		Parser parser = new Parser(config, stdLib);
		int frontendExitCode = parser.parse();
		if (frontendExitCode != 0)
			return ExitCode.SYNTAX_ERROR;

		// 2. Build the AST for each parse tree
		this.compilationUnits = ASTBuilder.buildAST(parser.getParsingResultList());
		for (var cu : compilationUnits) {
			Log.debug(cu.toString());
		}

		// 3. Desugaring (Lowering pseudo-while, syntactical sugar loops, traits)
		org.nebula.nebc.pass.Desugarer desugarer = new org.nebula.nebc.pass.Desugarer();
		for (var cu : compilationUnits) {
			List<Diagnostic> desugarErrors = desugarer.process(cu);
			if (!desugarErrors.isEmpty()) {
				for (var e : desugarErrors)
					Log.err(e.toString());
				return ExitCode.SEMANTIC_ERROR;
			}
		}

		// 4. Semantic Analysis (Type checking, symbol resolution)
		SemanticAnalyzer analyzer = new SemanticAnalyzer(config);
		for (var cu : compilationUnits) {
			List<Diagnostic> errors = analyzer.analyze(cu);
			if (!errors.isEmpty()) {
				for (var e : errors)
					Log.err(e.toString());
				return ExitCode.SEMANTIC_ERROR;
			}
		}

		// Skip codegen if --check-only was specified
		if (config.checkOnly()) {
			Log.info("Check-only mode: skipping code generation.");
			return ExitCode.SUCCESS;
		}

		// 4. Validate entry point (unless compiling as a library)
		if (!config.compileAsLibrary() && analyzer.getMainMethod() == null) {
			Diagnostic d = Diagnostic.of(org.nebula.nebc.frontend.diagnostic.DiagnosticCode.MISSING_MAIN_METHOD,
					org.nebula.nebc.frontend.diagnostic.SourceSpan.unknown());
			Log.err(d.toString());
			return ExitCode.CODEGEN_ERROR;
		}

		// 5. Code Generation (LLVM IR)
		LLVMCodeGenerator codegen = new LLVMCodeGenerator();
		try {
			codegen.generate(compilationUnits, analyzer);

			// Print LLVM IR in verbose mode
			if (config.verbose()) {
				Log.info("=== LLVM IR ===");
				Log.debug(codegen.dumpIR());
				Log.info("=== END IR ===");
			}

			// 6. Emit native binary
			String outputPath = config.outputFile() != null ? config.outputFile() : "a.out";
			List<Path> extraObjects = new ArrayList<>();

			// Always compile and link the Nebula runtime
			List<Path> runtimeObjs = compileRuntime();
			if (runtimeObjs != null) {
				extraObjects.addAll(runtimeObjs);
			}

			NativeCompiler.compile(codegen.getModule(), outputPath, config.targetPlatform(), config.isStatic(),
					extraObjects);

			// Cleanup extra objects
			for (Path p : extraObjects) {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignored) {
				}
			}

			Log.info("Compiled successfully: " + outputPath);
			return ExitCode.SUCCESS;
		} catch (CodegenException e) {
			Log.err("Code generation failed: " + e.getMessage());
			return ExitCode.CODEGEN_ERROR;
		} finally {
			codegen.dispose();
		}
	}

	private List<Path> compileRuntime() {
		List<Path> objects = new ArrayList<>();
		Path runtimeDir = Path.of("runtime");
		if (!Files.exists(runtimeDir)) {
			Log.warn("Runtime directory not found.");
			return null;
		}

		try (java.util.stream.Stream<Path> stream = Files.walk(runtimeDir)) {
			List<Path> cFiles = stream.filter(p -> p.toString().endsWith(".c")).toList();
			for (Path cFile : cFiles) {
				Path objFile = Files.createTempFile("neb_rt_" + cFile.getFileName().toString(), ".o");
				ProcessBuilder pb = new ProcessBuilder("clang", "-c", cFile.toAbsolutePath().toString(),
						"-o", objFile.toAbsolutePath().toString(), "-O3", "-fno-stack-protector", "-ffreestanding");

				int exitCode = pb.start().waitFor();
				if (exitCode != 0) {
					Log.err("Failed to compile runtime file: " + cFile);
					return null;
				}
				objects.add(objFile);
			}
		} catch (IOException | InterruptedException e) {
			Log.err("Error compiling runtime: " + e.getMessage());
			return null;
		}
		return objects;
	}

	private List<SourceFile> discoverStdLib() {
		List<SourceFile> stdFiles = new ArrayList<>();
		Path stdPath = Path.of("std");
		if (Files.exists(stdPath) && Files.isDirectory(stdPath)) {
			try (java.util.stream.Stream<Path> stream = Files.walk(stdPath)) {
				stream.filter(p -> p.toString().endsWith(".neb"))
						.forEach(p -> stdFiles.add(new SourceFile(p.toString())));
			} catch (IOException e) {
				Log.warn("Failed to walk std directory: " + e.getMessage());
			}
		}
		return stdFiles;
	}
}