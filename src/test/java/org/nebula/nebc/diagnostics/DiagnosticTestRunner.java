package org.nebula.nebc.diagnostics;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.nebula.nebc.ast.ASTBuilder;
import org.nebula.nebc.ast.CompilationUnit;
import org.nebula.nebc.core.CompilerConfig;
import org.nebula.nebc.frontend.parser.Parser;
import org.nebula.nebc.semantic.SemanticAnalyzer;
import org.nebula.nebc.semantic.SemanticError;

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

public class DiagnosticTestRunner {

    private static final String DIAGNOSTICS_DIR = "src/test/resources/diagnostics";
    private static final Pattern ERROR_PATTERN = Pattern.compile("//\\s*ERROR:\\s*(.*)");

    private record ExpectedError(int line, String messageSubstring) {
    }

    @TestFactory
    Stream<DynamicTest> runDiagnosticTests() throws IOException {
        Path dirPath = Paths.get(DIAGNOSTICS_DIR);
        if (!Files.exists(dirPath)) {
            return Stream.empty();
        }

        return Files.walk(dirPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".neb"))
                .map(this::createTestForFile);
    }

    private DynamicTest createTestForFile(Path testFile) {
        return DynamicTest.dynamicTest("Diagnostic Test: " + testFile.getFileName(), () -> {
            runTest(testFile.toFile());
        });
    }

    private void runTest(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        List<ExpectedError> expectedErrors = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = ERROR_PATTERN.matcher(line);
            if (matcher.find()) {
                String expectedMsg = matcher.group(1).trim();
                expectedErrors.add(new ExpectedError(i + 1, expectedMsg));
            }
        }

        CompilerConfig.Builder configBuilder = new CompilerConfig.Builder();
        configBuilder.addNebSource(file.getAbsolutePath());
        CompilerConfig config = configBuilder.build();

        Parser parser = new Parser(config);
        parser.parse();

        // Even if there are syntax errors, we still try to build AST and semantics
        // because we might be testing syntax error diagnostics eventually.
        // For now, assume syntax is mostly correct or we only test semantic errors.
        List<CompilationUnit> cus = ASTBuilder.buildAST(parser.getParsingResultList());

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        List<SemanticError> actualErrors = new ArrayList<>();
        for (CompilationUnit cu : cus) {
            actualErrors.addAll(analyzer.analyze(cu));
        }

        // Check each actual error against expected
        List<SemanticError> unmatchedActualErrors = new ArrayList<>(actualErrors);
        List<ExpectedError> unmatchedExpectedErrors = new ArrayList<>(expectedErrors);

        for (int i = unmatchedActualErrors.size() - 1; i >= 0; i--) {
            SemanticError actual = unmatchedActualErrors.get(i);

            boolean matched = false;
            for (int j = 0; j < unmatchedExpectedErrors.size(); j++) {
                ExpectedError expected = unmatchedExpectedErrors.get(j);
                if (actual.span().startLine() == expected.line() &&
                        actual.message().toLowerCase().contains(expected.messageSubstring().toLowerCase())) {
                    unmatchedExpectedErrors.remove(j);
                    matched = true;
                    break;
                }
            }
            if (matched) {
                unmatchedActualErrors.remove(i);
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
        for (ExpectedError expected : expectedErrors) {
            // Check if it was matched (not in unmatched list)
            boolean wasMatched = !unmatchedExpectedErrors.contains(expected);

            if (wasMatched) {
                System.out.println(
                        "  [" + GREEN + "✓" + RESET + "] Line " + expected.line() + ": " + expected.messageSubstring());
            } else {
                allPassed = false;
                System.out.println("  [" + RED + "✗" + RESET + "] Line " + expected.line() + ": (expected) "
                        + expected.messageSubstring());

                // See if there's an actual unexpected error on that same line
                boolean foundAltError = false;
                for (SemanticError unexpected : unmatchedActualErrors) {
                    if (unexpected.span().startLine() == expected.line()) {
                        System.out.println("                (actual)   " + unexpected.message());
                        foundAltError = true;
                    }
                }
                if (!foundAltError) {
                    System.out.println("                (actual)   <No error found>");
                }
            }
            System.out.println();
        }

        // 2. Process unexpected errors that weren't on an expected line
        for (SemanticError unexpected : unmatchedActualErrors) {
            // Don't report it again if we just reported it as an "actual" mismatch above
            boolean alreadyReported = expectedErrors.stream().anyMatch(e -> e.line() == unexpected.span().startLine());

            if (!alreadyReported) {
                allPassed = false;
                System.out.println("  [" + RED + "✗" + RESET + "] Line " + unexpected.span().startLine()
                        + ": (unexpected) " + unexpected.message());
                System.out.println();
            }
        }

        System.out.println("======================================================================\n");

        if (!allPassed) {
            fail("Diagnostic test failed for " + file.getName() + " (see console output above)");
        }
    }
}
