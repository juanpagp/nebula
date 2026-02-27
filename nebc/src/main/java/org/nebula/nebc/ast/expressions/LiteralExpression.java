package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * A literal value in the source code (integer, float, string, bool, char).
 * The semantic type is resolved by the SemanticAnalyzer, not by this AST node.
 */
public class LiteralExpression extends Expression
{

	public final Object value;
	public final LiteralType type;

	public LiteralExpression(SourceSpan span, Object value, LiteralType type)
	{
		super(span);
		this.value = value;
		this.type = type;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitLiteralExpression(this);
	}

	public enum LiteralType
	{
		INT, FLOAT, STRING, BOOL, CHAR
	}
}