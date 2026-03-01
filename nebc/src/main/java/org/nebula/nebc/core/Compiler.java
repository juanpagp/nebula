package org.nebula.nebc.core;

import org.nebula.nebc.ast.ASTBuilder;
import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.CompilationUnit;
import org.nebula.nebc.ast.statements.UseStatement;
import org.nebula.nebc.codegen.CodegenException;
import org.nebula.nebc.codegen.LLVMCodeGenerator;
import org.nebula.nebc.codegen.NativeCompiler;
import org.nebula.nebc.frontend.diagnostic.Diagnostic;
import org.nebula.nebc.frontend.parser.Parser;
import org.nebula.nebc.frontend.parser.ParsingResult;
import org.nebula.nebc.io.SourceFile;
import org.nebula.nebc.semantic.SemanticAnalyzer;
import org.nebula.nebc.semantic.SymbolExporter;
import org.nebula.nebc.semantic.SymbolImporter;
import org.nebula.nebc.util.Log;
import org.nebula.util.ExitCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
		// We start by parsing user sources. Std lib will be loaded on demand.
		Parser parser = new Parser(config, new ArrayList<>());
		int frontendExitCode = parser.parse();
		if (frontendExitCode != 0)
			return ExitCode.SYNTAX_ERROR;

		// 2. Build the AST for user sources
		this.compilationUnits = ASTBuilder.buildAST(parser.getParsingResultList());

		// 3. Semantic Analysis (Type checking, symbol resolution)
		SemanticAnalyzer analyzer = new SemanticAnalyzer(config);

		// 3.1 Load external symbols
		loadExternalSymbols(analyzer);

		// 3.2 Resolve Standard Library Dependencies recursively (on-demand loading of .neb files)
		// This is for source dependencies, mostly for when compiling the std itself or
		// when user explicitly wants to compile from source.
		resolveDependencies(analyzer);
		for (var cu : compilationUnits)
		{
			Log.debug(cu.toString());
		}

		// 3. Desugaring (Lowering pseudo-while, syntactical sugar loops, traits)
		org.nebula.nebc.pass.Desugarer desugarer = new org.nebula.nebc.pass.Desugarer();
		for (var cu : compilationUnits)
		{
			List<Diagnostic> desugarErrors = desugarer.process(cu);
			if (!desugarErrors.isEmpty())
			{
				for (var e : desugarErrors)
					Log.err(e.toString());
				return ExitCode.SEMANTIC_ERROR;
			}
		}

		// 4. Semantic Analysis (Type checking, symbol resolution)
		// Forward-declare all top-level types across all units

		// Phase 1: Forward-declare all top-level types across all units
		for (var cu : compilationUnits)
		{
			analyzer.declareTypes(cu);
		}

		// Phase 1.5: Pre-declare all method signatures across all units
		for (var cu : compilationUnits)
		{
			analyzer.declareMethods(cu);
		}

		// Phase 1.75: Process all trait bodies so their member scopes are populated
		// This allows generic method bodies to resolve trait-bound member access
		for (var cu : compilationUnits)
		{
			analyzer.declareTraitBodies(cu);
		}

		// Phase 2: Full visitation — solve bodies, check types.
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
			if (config.compileAsLibrary())
			{
				exportSymbols(analyzer);
			}
			Log.info("Check-only mode: skipping code generation.");
			return ExitCode.SUCCESS;
		}

		// 4. Validate entry point (unless compiling as a library)
		if (!config.compileAsLibrary() && analyzer.getMainMethod() == null)
		{
			Diagnostic d = Diagnostic.of(org.nebula.nebc.frontend.diagnostic.DiagnosticCode.MISSING_MAIN_METHOD, org.nebula.nebc.frontend.diagnostic.SourceSpan.unknown());
			Log.err(d.toString());
			return ExitCode.CODEGEN_ERROR;
		}

		// 5. Code Generation (LLVM IR)
		LLVMCodeGenerator codegen = new LLVMCodeGenerator();
		try
		{
			List<CompilationUnit> unitsToCompile = compilationUnits;
			if (!config.compileAsLibrary())
			{
				unitsToCompile = compilationUnits.stream().filter(cu -> !cu.getSpan().file().startsWith("std/") && !cu.getSpan().file().contains("/std/") && !cu.getSpan().file().contains("\\std\\")).toList();
			}
			codegen.generate(unitsToCompile, analyzer);

			// Print LLVM IR in verbose mode
			if (config.verbose())
			{
				Log.info("=== LLVM IR ===");
				Log.debug(codegen.dumpIR());
				Log.info("=== END IR ===");
			}

			// Save IR to file for debugging
			try (java.io.PrintWriter out = new java.io.PrintWriter("generated.ll"))
			{
				out.println(codegen.dumpIR());
			}
			catch (java.io.IOException e)
			{
				Log.err("Could not write IR to file: " + e.getMessage());
			}

			// 6. Verify the module only after dumping it
			codegen.verifyModule();

			// 6. Emit native binary
			String outputPath = config.outputFile() != null ? config.outputFile() : "a.out";
			List<Path> extraObjects = new ArrayList<>();

			// Always compile runtime objects. For libraries, this compiles everything
			// except start.c.
			// For executables, this compiles only start.c to be statically linked.
			List<Path> runtimeObjs = compileRuntime(config.compileAsLibrary());
			if (runtimeObjs != null)
			{
				extraObjects.addAll(runtimeObjs);
			}

			NativeCompiler.compile(codegen.getModule(), outputPath, config.targetPlatform(), config.isStatic(), config.compileAsLibrary(), extraObjects, config.librarySearchPaths(), config.nebLibraries());

			// Cleanup extra objects
			for (Path p : extraObjects)
			{
				try
				{
					Files.deleteIfExists(p);
				}
				catch (IOException ignored)
				{
				}
			}

			if (config.compileAsLibrary())
			{
				exportSymbols(analyzer);
			}

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

	private List<Path> compileRuntime(boolean isLibrary)
	{
		List<Path> objects = new ArrayList<>();
		Path runtimeDir = Path.of("runtime");
		if (!Files.exists(runtimeDir))
		{
			runtimeDir = Path.of("../runtime");
			if (!Files.exists(runtimeDir))
			{
				Log.warn("Runtime directory not found.");
				return null;
			}
		}

		try (java.util.stream.Stream<Path> stream = Files.walk(runtimeDir))
		{
			List<Path> cFiles = stream.filter(p -> p.toString().endsWith(".c")).toList();
			for (Path cFile : cFiles)
			{
				boolean isStartFile = cFile.getFileName().toString().equals("start.c");
				if (isLibrary && isStartFile)
					continue;

				Path objFile = Files.createTempFile("neb_rt_" + cFile.getFileName().toString(), ".o");
				// For the library we also want -fPIC so the objects can be linked dynamically
				List<String> cmd = new ArrayList<>(List.of("clang", "-c", cFile.toAbsolutePath().toString(), "-o", objFile.toAbsolutePath().toString(), "-O3", "-fno-stack-protector", "-ffreestanding"));
				if (isLibrary)
					cmd.add("-fPIC");

				ProcessBuilder pb = new ProcessBuilder(cmd);
				int exitCode = pb.start().waitFor();
				if (exitCode != 0)
				{
					Log.err("Failed to compile runtime file: " + cFile);
					return null;
				}
				objects.add(objFile);
			}
		}
		catch ( IOException |
				InterruptedException e)
		{
			Log.err("Error compiling runtime: " + e.getMessage());
			return null;
		}
		return objects;
	}

	private void loadExternalSymbols(SemanticAnalyzer analyzer)
	{
		SymbolImporter importer = new SymbolImporter();

		// Load default std if not disabled
		if (config.useStdLib())
		{
			try
			{
				Path stdSyms = Path.of("std.nebsym");
				if (!Files.exists(stdSyms))
				{
					stdSyms = Path.of("..", "std.nebsym"); // Try parent for dev environment
				}

				if (Files.exists(stdSyms))
				{
					Log.info("Loading standard library symbols: " + stdSyms);
					importer.importSymbols(stdSyms.toString(), analyzer.getGlobalScope());
				}
				else
				{
					Log.warn("Standard library symbols (std.nebsym) not found. On-demand source loading will be used.");
				}
			}
			catch (IOException e)
			{
				Log.err("Failed to load standard library symbols: " + e.getMessage());
			}
		}

		// Load user-specified symbol files
		for (SourceFile sf : config.symbolFiles())
		{
			try
			{
				Log.info("Loading symbols: " + sf.path());
				importer.importSymbols(sf.path(), analyzer.getGlobalScope());
			}
			catch (IOException e)
			{
				Log.err("Failed to load symbols from " + sf.path() + ": " + e.getMessage());
			}
		}
	}

	private void exportSymbols(SemanticAnalyzer analyzer)
	{
		String outputPath = config.outputFile() != null ? config.outputFile() : "out";
		// Remove extension if present
		if (outputPath.contains("."))
		{
			outputPath = outputPath.substring(0, outputPath.lastIndexOf('.'));
		}
		String symPath = outputPath + ".nebsym";

		SymbolExporter exporter = new SymbolExporter();
		try
		{
			Log.info("Exporting symbols to: " + symPath);
			exporter.export(analyzer.getGlobalScope(), outputPath, symPath);
		}
		catch (IOException e)
		{
			Log.err("Failed to export symbols: " + e.getMessage());
		}
	}

	private void resolveDependencies(SemanticAnalyzer analyzer)
	{
		if (config.compileAsLibrary())
			return;

		java.util.Set<String> loadedFiles = new java.util.HashSet<>();
		java.util.List<String> toLoad = new ArrayList<>();

		// Standard library modules will be loaded on-demand based on 'use' statements.

		// Also check user code for explicit 'use std::...'
		for (CompilationUnit cu : compilationUnits)
		{
			for (ASTNode directive : cu.directives)
			{
				if (directive instanceof UseStatement use && use.qualifiedName.startsWith("std::"))
				{
					// Only load from source if it wasn't already loaded as an external symbol (library)
					if (analyzer.getGlobalScope().resolve(use.qualifiedName) == null)
					{
						toLoad.add(use.qualifiedName);
					}
				}
			}
		}

		while (!toLoad.isEmpty())
		{
			String dep = toLoad.remove(0);
			if (loadedFiles.contains(dep))
				continue;
			loadedFiles.add(dep);

			// Map std::foo::bar to std/foo/bar.neb or std/foo.neb
			Path path = resolveStdPath(dep);
			if (path != null && Files.exists(path))
			{
				Log.info("Loading dependency: " + dep + " (" + path + ")");
				SourceFile sf = new SourceFile(path.toString());
				Parser p = new Parser(config, List.of(sf));
				if (p.parse() == 0)
				{
					// Filter p.getParsingResultList() to only include sf
					List<ParsingResult> results = p.getParsingResultList().stream().filter(r -> Path.of(r.file().path()).toAbsolutePath().toString().equals(Path.of(sf.path()).toAbsolutePath().toString())).toList();

					List<CompilationUnit> cus = ASTBuilder.buildAST(results);
					for (CompilationUnit cu : cus)
					{
						this.compilationUnits.add(0, cu); // Prepend std units
						// Check this new unit for more dependencies
						for (ASTNode directive : cu.directives)
						{
							if (directive instanceof UseStatement use && use.qualifiedName.startsWith("std::"))
							{
								if (analyzer.getGlobalScope().resolve(use.qualifiedName) == null)
								{
									toLoad.add(use.qualifiedName);
								}
							}
						}
					}
				}
			}
			else
			{
				Log.warn("Could not resolve standard library dependency: " + dep);
			}
		}
	}

	private Path resolveStdPath(String qualifiedName)
	{
		// std::io -> std/io.neb
		String relative = qualifiedName.replace("::", "/");

		Path p = Path.of(relative + ".neb");
		if (Files.exists(p))
			return p;

		// Try checking project root
		Path parentP = Path.of("..").resolve(relative + ".neb");
		if (Files.exists(parentP))
			return parentP;

		return null;
	}
}