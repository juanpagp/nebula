package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class LiteralExpression extends Expression
{
	public final Object value;
	public final LiteralType type;

	public enum LiteralType
	{INT, FLOAT, STRING, BOOL, CHAR, NULL}

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
}