package org.nebula.nebc.ast.types;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class TupleType extends Type
{
	public final List<Type> elementTypes;

	public TupleType(SourceSpan span, List<Type> elementTypes)
	{
		super(span);
		this.elementTypes = elementTypes;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTypeReference(this);
	}
}