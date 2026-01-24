package org.nebula.nebc.ast.tags;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.Type;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class TagAtom extends TagExpression
{
	private final Type type;

	public TagAtom(SourceSpan span, Type type)
	{
		super(span);
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTagAtom(this);
	}
}