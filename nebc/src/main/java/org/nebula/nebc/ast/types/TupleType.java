package org.nebula.nebc.ast.types;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class TupleType extends TypeNode
{
	public final List<TypeNode> elementTypes;

	public TupleType(SourceSpan span, List<TypeNode> elementTypes)
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