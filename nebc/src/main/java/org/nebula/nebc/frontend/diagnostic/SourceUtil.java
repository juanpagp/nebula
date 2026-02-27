package org.nebula.nebc.frontend.diagnostic;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Utility for extracting source location information from ANTLR components.
 */
public class SourceUtil
{

	/**
	 * Creates a SourceSpan from an ANTLR ParserRuleContext (a full grammar rule).
	 * This covers everything from the first token to the last token of the rule.
	 */
	public static SourceSpan createSpan(ParserRuleContext ctx, String fileName)
	{
		if (ctx == null || ctx.start == null || ctx.stop == null)
		{
			return SourceSpan.unknown();
		}

		Token start = ctx.getStart();
		Token stop = ctx.getStop();

		return new SourceSpan(
				fileName,
				start.getLine(),
				start.getCharPositionInLine(),
				stop.getLine(),
				// We add the length of the last token text to get the true end column
				stop.getCharPositionInLine() + stop.getText().length()
		);
	}

	/**
	 * Creates a SourceSpan from a single ANTLR Token.
	 */
	public static SourceSpan createSpan(Token token, String fileName)
	{
		if (token == null)
			return SourceSpan.unknown();

		return new SourceSpan(
				fileName,
				token.getLine(),
				token.getCharPositionInLine(),
				token.getLine(),
				token.getCharPositionInLine() + token.getText().length()
		);
	}

	/**
	 * Creates a SourceSpan from a TerminalNode (like an IDENTIFIER).
	 */
	public static SourceSpan createSpan(TerminalNode node, String fileName)
	{
		if (node == null)
			return SourceSpan.unknown();
		return createSpan(node.getSymbol(), fileName);
	}
}