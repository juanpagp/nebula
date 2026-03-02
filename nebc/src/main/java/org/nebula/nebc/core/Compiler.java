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
import org.nebula.nebc.semantic.symbol.NamespaceSymbol;
import org.nebula.nebc.semantic.symbol.Symbol;
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

		// 3.2 Resolve Standard Library Dependencies recursively (on-demand loading of .neb files).
		// Prelude source namespaces are also seeded here so that generic prelude functions
		// (e.g. println<T>) have real AST declaration nodes available for monomorphization.
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

		// 3.1.5 Export prelude symbols (make commonly-used std symbols globally available).
		// Must run AFTER declareMethods so that source-loaded symbols have their declaration
		// nodes populated (via forceDefine), ensuring generic prelude functions are
		// monomorphizable during code generation.
		exportPreludeSymbols(analyzer);

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
			/* 
			try (java.io.PrintWriter out = new java.io.PrintWriter("generated.ll"))
			{
				out.println(codegen.dumpIR());
			}
			catch (java.io.IOException e)
			{
				Log.err("Could not write IR to file: " + e.getMessage());
			}
			*/

			// 6. Verify the module only after dumping it
			codegen.verifyModule();

			// 6. Emit native binary
			String outputPath = config.outputFile() != null ? config.outputFile() : "a.out";
			List<Path> extraObjects = new ArrayList<>();

			// 6. Native Compilation Phase: Compile all C/C++ sources provided
			List<Path> nativeObjects = compileNativeSources();
			if (nativeObjects != null)
			{
				extraObjects.addAll(nativeObjects);
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

	private List<Path> compileNativeSources()
	{
		List<Path> objects = new ArrayList<>();
		List<org.nebula.nebc.io.SourceFile> nativeSources = new ArrayList<>(config.nativeSources());

		// 1. Automatically include standard library runtime if enabled
		if (config.useStdLib())
		{
			Path stdRuntimeDir = Path.of("std", "runtime");
			if (!Files.exists(stdRuntimeDir))
			{
				stdRuntimeDir = Path.of("..", "std", "runtime"); // Try parent for dev environment
			}

			if (Files.exists(stdRuntimeDir))
			{
				try (java.util.stream.Stream<Path> stream = Files.walk(stdRuntimeDir))
				{
					List<Path> stdCFiles = stream.filter(p -> p.toString().endsWith(".c") || p.toString().endsWith(".cpp")).toList();
					for (Path cFile : stdCFiles)
					{
						String fileName = cFile.getFileName().toString();
						// Skip start.c if compiling as a library, it's the entry point wrapper
						if (config.compileAsLibrary() && fileName.equals("start.c"))
							continue;
						// Skip deprecated wrapper file - syscalls.c is superseded by runtime.c and linux_syscalls.c
						if (fileName.equals("syscalls.c"))
							continue;
						nativeSources.add(new org.nebula.nebc.io.SourceFile(cFile.toAbsolutePath().toString()));
					}
				}
				catch (IOException e)
				{
					Log.warn("Failed to scan standard library runtime: " + e.getMessage());
				}
			}
			else
			{
				Log.warn("Standard library runtime directory (std/runtime) not found.");
			}
		}

		// 2. Compile each native source to a temporary object file
		for (org.nebula.nebc.io.SourceFile sf : nativeSources)
		{
			if (sf.type() == org.nebula.nebc.io.FileType.NATIVE_HEADER || sf.type() == org.nebula.nebc.io.FileType.NATIVE_CPP_HEADER)
				continue;

			try
			{
				Path cFile = Path.of(sf.path());
				Path objFile = Files.createTempFile("neb_native_" + cFile.getFileName().toString(), ".o");

				List<String> cmd = new ArrayList<>(List.of("clang", "-c", cFile.toAbsolutePath().toString(), "-o", objFile.toAbsolutePath().toString(), "-O3", "-fno-stack-protector", "-ffreestanding"));

				if (config.compileAsLibrary())
				{
					cmd.add("-fPIC");
				}

				// If it's a C++ file, use clang++ or add flags
				if (sf.type() == org.nebula.nebc.io.FileType.NATIVE_CPP_SOURCE)
				{
					cmd.set(0, "clang++");
				}

				ProcessBuilder pb = new ProcessBuilder(cmd);
				int exitCode = pb.start().waitFor();
				if (exitCode != 0)
				{
					Log.err("Failed to compile native source: " + cFile);
					return null;
				}
				objects.add(objFile);
			}
			catch ( IOException |
					InterruptedException e)
			{
				Log.err("Error compiling native source: " + sf.path() + " - " + e.getMessage());
				return null;
			}
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
				Path stdSyms = Path.of("neb.nebsym");
				if (!Files.exists(stdSyms))
				{
					stdSyms = Path.of("..", "neb.nebsym"); // Try parent for dev environment
				}

				if (Files.exists(stdSyms))
				{
					Log.info("Loading standard library symbols: " + stdSyms);
					importer.importSymbols(stdSyms.toString(), analyzer.getGlobalScope());
				}
				else
				{
					Log.warn("Standard library symbols (neb.nebsym) not found. On-demand source loading will be used.");
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

	private void exportPreludeSymbols(SemanticAnalyzer analyzer)
	{
		System.out.println("DEBUG: Exporting prelude symbols...");
		// Export prelude symbols: make commonly-used std::io symbols available globally
		// without requiring explicit 'use' statements
		if (!config.useStdLib())
			return;

		// Resolve std::io namespace
		Symbol stdIoSymbol = analyzer.getGlobalScope().resolve("std");
		if (stdIoSymbol == null || !(stdIoSymbol instanceof NamespaceSymbol stdNs))
		{
			Log.debug("Couldnt resolve the std namespace");
			return;
		}

		Symbol ioSymbol = stdNs.getMemberTable().resolve("io");
		if (ioSymbol == null || !(ioSymbol instanceof NamespaceSymbol ioNs))
		{
			Log.debug("Couldnt resolve the std::io namespace");
			return;
		}

		// Export commonly-used I/O functions to the global scope
		// This creates aliases in the global scope that point to std::io symbols
		String[] preludeSymbols = { "print", "println" };

		for (String symbolName : preludeSymbols)
		{
			Symbol symbol = ioNs.getMemberTable().resolve(symbolName);
			if (symbol != null)
			{
				// Define the symbol in the global scope (as an alias/import)
				analyzer.getGlobalScope().define(symbol);
				if (config.verbose())
				{
					Log.debug("Prelude: exporting std::io::" + symbolName + " to global scope");
				}
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

		// Always load prelude source namespaces so that generic prelude functions
		// (e.g. println<T: Displayable>) have real AST declaration nodes available
		// for monomorphization, even when the user writes no explicit 'use' statement.
		if (config.useStdLib())
		{
			toLoad.add("std::io");
		}

		// Also check user code for explicit 'use std::...'
		for (CompilationUnit cu : compilationUnits)
		{
			for (ASTNode directive : cu.directives)
			{
				if (directive instanceof UseStatement use && use.qualifiedName.startsWith("std::"))
				{
					// Only load from source if it wasn't already loaded as an external symbol (library)
					// We check if the EXACT namespace exists.
					   Symbol existingNs = analyzer.getGlobalScope().resolve(use.qualifiedName);
					   if (existingNs == null || existingNs.getDeclarationNode() == null)
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
								   Symbol existingNs = analyzer.getGlobalScope().resolve(use.qualifiedName);
								   if (existingNs == null || existingNs.getDeclarationNode() == null)
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