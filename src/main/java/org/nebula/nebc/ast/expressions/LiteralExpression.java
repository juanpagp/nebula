package org.nebula.nebc.ast.expressions;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;
import org.nebula.nebc.semantic.types.PrimitiveType;
import org.nebula.nebc.semantic.types.Type;

public class LiteralExpression extends Expression
{
	public final Object value;
	public final LiteralType type;

	public enum LiteralType
	{INT, FLOAT, STRING, BOOL, CHAR}

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

	public Type getType()
	{
		return switch (type)
		{
			case INT ->
					PrimitiveType.I64;
			case FLOAT ->
					PrimitiveType.F64;
			case BOOL ->
					PrimitiveType.BOOL;
			case CHAR ->
					PrimitiveType.CHAR;
			case STRING ->
					PrimitiveType.STRING;
		};
	}
}