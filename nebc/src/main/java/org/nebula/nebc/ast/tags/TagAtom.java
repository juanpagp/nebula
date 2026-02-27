package org.nebula.nebc.ast.tags;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class TagAtom extends TagExpression
{
	public final TypeNode type;

	public TagAtom(SourceSpan span, TypeNode type)
	{
		super(span);
		this.type = type;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTagAtom(this);
	}
}