package org.lokray.nebc.frontend.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.lokray.nebc.core.CompilerConfig;
import org.lokray.nebc.frontend.parser.generated.NebulaLexer;
import org.lokray.nebc.frontend.parser.generated.NebulaParser;
import org.lokray.nebc.io.SourceFile;
import org.lokray.nebc.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser
{
	private final List<ParseTree> parseTrees;
	private final CompilerConfig config;
	// Storage to pass valid token streams from the lexer phase to the parser phase
	private final Map<SourceFile, CommonTokenStream> validTokenStreams;

	public Parser(CompilerConfig config)
	{
		this.config = config;
		this.parseTrees = new ArrayList<>();
		this.validTokenStreams = new HashMap<>();
	}

	public List<ParseTree> getParseTrees()
	{
		return parseTrees;
	}

	/**
	 * Public entry point for the frontend.
	 * Orchestrates the Tokenization and Parsing phases.
	 *
	 * @return 0 if successful (no syntax errors), 1 otherwise.
	 */
	public int parse()
	{
		// 1. Tokenize all sources
		int lexingErrors = tokenize();
		if (lexingErrors > 0)
		{
			return 1; // Stop if lexing failed
		}

		// 2. Parse tokens into ASTs
		int parsingErrors = performParse();
		if (parsingErrors > 0)
		{
			return 1;
		}

		return 0;
	}

	/**
	 * Phase 1: Lexical Analysis.
	 * Reads source files, generates tokens, checks for lexical errors,
	 * and populates validTokenStreams for the next phase.
	 */
	private int tokenize()
	{
		int totalErrors = 0;
		validTokenStreams.clear();

		for (SourceFile sourceFile : config.nebSources())
		{
			try
			{
				if (config.verbose())
				{
					Log.info("Tokenizing: " + sourceFile.fileName());
				}

				// 1. Create the input stream from the file path
				CharStream input = CharStreams.fromFileName(sourceFile.path());

				// 2. Initialize the Lexer
				NebulaLexer lexer = new NebulaLexer(input);

				// Remove default console listener and add our own
				lexer.removeErrorListeners();
				SyntaxErrorCounter errorCounter = new SyntaxErrorCounter(sourceFile.fileName());
				lexer.addErrorListener(errorCounter);

				// 3. Create a TokenStream. We use fill() to force the lexer to process
				// the entire file immediately.
				CommonTokenStream tokenStream = new CommonTokenStream(lexer);
				tokenStream.fill();

				// 4. Check for lexical errors
				if (errorCounter.getErrorCount() > 0)
				{
					totalErrors += errorCounter.getErrorCount();
					Log.err("Lexing failed for " + sourceFile.fileName() + " with " + errorCounter.getErrorCount() + " errors.");
				}
				else
				{
					// Store the valid stream for the parsing phase
					validTokenStreams.put(sourceFile, tokenStream);
				}

				// 5. Debug: Print tokens if verbose mode is active
				if (config.verbose())
				{
					for (Token token : tokenStream.getTokens())
					{
						System.out.println(token.toString());
					}
				}

			}
			catch (IOException e)
			{
				Log.err("Failed to read source file: " + sourceFile.path());
				totalErrors++;
			}
		}

		return totalErrors;
	}

	/**
	 * Phase 2: Syntax Analysis (Parsing).
	 * Consumes the TokenStreams generated in phase 1 and builds ParseTrees.
	 */
	private int performParse()
	{
		int totalErrors = 0;

		// Iterate based on config order to maintain deterministic compilation order
		for (SourceFile sourceFile : config.nebSources())
		{
			CommonTokenStream tokenStream = validTokenStreams.get(sourceFile);

			// Skip files that failed lexing (though usually we abort before this if any failed)
			if (tokenStream == null)
			{
				continue;
			}

			// Rewind stream to the beginning
			tokenStream.seek(0);

			if (config.verbose())
			{
				Log.info("Parsing: " + sourceFile.fileName());
			}

			// 1. Initialize Parser
			NebulaParser parser = new NebulaParser(tokenStream);

			// 2. Setup Error Handling
			parser.removeErrorListeners();
			SyntaxErrorCounter errorCounter = new SyntaxErrorCounter(sourceFile.fileName());
			parser.addErrorListener(errorCounter);

			// 3. Generate Parse Tree (starting at 'compilation_unit' rule)
			ParseTree tree = parser.compilation_unit();

			// 4. Check for syntax errors
			if (errorCounter.getErrorCount() > 0)
			{
				totalErrors += errorCounter.getErrorCount();
				Log.err("Parsing failed for " + sourceFile.fileName() + " with " + errorCounter.getErrorCount() + " errors.");
			}
			else
			{
				parseTrees.add(tree);
			}
		}

		return totalErrors;
	}

	/**
	 * Simple listener to count and log syntax errors during lexing and parsing.
	 */
	private static class SyntaxErrorCounter extends BaseErrorListener
	{
		private int errorCount = 0;
		private final String fileName;

		public SyntaxErrorCounter(String fileName)
		{
			this.fileName = fileName;
		}

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
		{
			errorCount++;
			Log.err("[" + fileName + ":" + line + ":" + charPositionInLine + "] " + msg);
		}

		public int getErrorCount()
		{
			return errorCount;
		}
	}
}