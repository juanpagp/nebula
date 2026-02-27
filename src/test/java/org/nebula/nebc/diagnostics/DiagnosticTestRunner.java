package org.nebula.nebc.diagnostics;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.nebula.nebc.ast.ASTBuilder;
import org.nebula.nebc.core.CompilerConfig;
import org.nebula.nebc.frontend.diagnostic.Diagnostic;
import org.nebula.nebc.frontend.parser.Parser;
import org.nebula.nebc.semantic.SemanticAnalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class DiagnosticTestRunner
{

	// Regex to find: // ERROR: <message>
	private static final Pattern ERROR_PATTERN = Pattern.compile("//\\s*ERROR\\s*:\\s*(.*)", Pattern.CASE_INSENSITIVE);
	private static final String DIAGNOSTICS_DIR = "src/test/resources/diagnostics";

	@TestFactory
	Stream<DynamicTest> diagnosticTests() throws IOException
	{
		Path startPath = Paths.get(DIAGNOSTICS_DIR);
		if (!Files.exists(startPath))
		{
			System.out.println("Warning: Diagnostics directory not found: " + startPath.toAbsolutePath());
			return Stream.empty();
		}

		return Files.walk(startPath)
				.filter(p -> p.toString().endsWith(".neb"))
				.map(this::createTestForFile);
	}

	private DynamicTest createTestForFile(Path testFile)
	{
		return DynamicTest.dynamicTest("Diagnostic Test: " + testFile.getFileName(), () ->
		{
			runTest(testFile.toFile());
		});
	}

	private void runTest(File file) throws IOException
	{
		List<String> lines = Files.readAllLines(file.toPath());
		List<ExpectedError> expectedErrors = new ArrayList<>();

		for (int i = 0; i < lines.size(); i++)
		{
			Matcher m = ERROR_PATTERN.matcher(lines.get(i));
			if (m.find())
			{
				expectedErrors.add(new ExpectedError(i + 1, m.group(1).trim()));
			}
		}

		// Dummy CompilerConfig for parsing and semantic analysis
		CompilerConfig.Builder builder = new CompilerConfig.Builder();
		builder.addNebSource(file.getAbsolutePath());
		builder.compileAsLibrary(false);
		CompilerConfig config = builder.build();

		Parser parser = new Parser(config);
		List<Diagnostic> actualErrors = new ArrayList<>();
		int parserErrors = parser.parse();
		if (parserErrors != 0)
		{
			fail("Parser failed for " + file.getName() + " with " + parserErrors + " errors.");
		}

		// Even if there are syntax errors, we still try to build AST and semantics
		// because we might be testing syntax error diagnostics eventually.
		// For now, assume syntax is mostly correct or we only test semantic errors.
		var compilationUnits = ASTBuilder.buildAST(parser.getParsingResultList());

		SemanticAnalyzer analyzer = new SemanticAnalyzer(config);
		actualErrors.addAll(analyzer.analyze(compilationUnits));

		// Emulate the entry point check from Compiler.run()
		if (!config.compileAsLibrary() && analyzer.getMainMethod() == null)
		{
			actualErrors.add(Diagnostic.of(org.nebula.nebc.frontend.diagnostic.DiagnosticCode.MISSING_MAIN_METHOD,
					org.nebula.nebc.frontend.diagnostic.SourceSpan.unknown()));
		}

		List<ExpectedError> unmatchedExpectedErrors = new ArrayList<>(expectedErrors);
		List<Diagnostic> unmatchedActualErrors = new ArrayList<>(actualErrors);

		// --- Matching Logic ---
		// Iterate through expected errors and try to match them with actual errors
		for (int i = unmatchedExpectedErrors.size() - 1; i >= 0; i--)
		{
			ExpectedError expected = unmatchedExpectedErrors.get(i);
			boolean matched = false;
			for (int j = unmatchedActualErrors.size() - 1; j >= 0; j--)
			{
				Diagnostic actual = unmatchedActualErrors.get(j);
				if (actual.span().startLine() == expected.line() &&
						actual.message().toLowerCase().contains(expected.messageSubstring().toLowerCase()))
				{
					unmatchedActualErrors.remove(j);
					unmatchedExpectedErrors.remove(i);
					matched = true;
					break;
				}
			}
		}

		// --- Console Output Formatting ---
		String GREEN = "\033[92m";
		String RED = "\033[91m";
		String BOLD = "\033[1m";
		String RESET = "\033[0m";

		System.out.println("\n======================================================================");
		System.out.println("  Diagnostic Test: " + file.getName());
		System.out.println("======================================================================\n");

		boolean allPassed = true;

		// 1. Process all expected errors first (Matched or Unmatched)
		for (ExpectedError expected : expectedErrors)
		{
			// Check if it was matched (not in unmatched list)
			boolean wasMatched = !unmatchedExpectedErrors.contains(expected);

			if (wasMatched)
			{
				System.out.println(
						"  [" + GREEN + "✓" + RESET + "] Line " + expected.line() + ": " + expected.messageSubstring());
			}
			else
			{
				allPassed = false;
				System.out.println("  [" + RED + "✗" + RESET + "] Line " + expected.line() + ": (expected) "
						+ expected.messageSubstring());

				// See if there's an actual unexpected error on that same line
				boolean foundAltError = false;
				for (Diagnostic unexpected : unmatchedActualErrors)
				{
					if (unexpected.span().startLine() == expected.line())
					{
						System.out.println("                (actual)   " + unexpected.message());
						foundAltError = true;
					}
				}
				if (!foundAltError)
				{
					System.out.println("                (actual)   <No error found>");
				}
			}
			System.out.println();
		}

		// 2. Process unexpected errors that weren't on an expected line
		for (Diagnostic unexpected : unmatchedActualErrors)
		{
			// Don't report it again if we just reported it as an "actual" mismatch above
			boolean alreadyReported = expectedErrors.stream()
					.anyMatch(e -> e.line() == unexpected.span().startLine());

			if (!alreadyReported)
			{
				allPassed = false;
				System.out.println("  [" + RED + "✗" + RESET + "] Line " + unexpected.span().startLine()
						+ ": (unexpected) " + unexpected.message());
				System.out.println();
			}
		}

		System.out.println("======================================================================\n");

		if (!allPassed)
		{
			fail("Diagnostic test failed for " + file.getName() + " (see console output above)");
		}
	}

	private record ExpectedError(int line, String messageSubstring)
	{
	}
}
