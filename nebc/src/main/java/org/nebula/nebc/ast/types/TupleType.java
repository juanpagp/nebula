package org.nebula.nebc.ast.types;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class TupleType extends TypeNode
{
	public final List<TypeNode> elementTypes;
	/** Optional field names; element is {@code null} when the field is unnamed. */
	public final List<String> fieldNames;

	public TupleType(SourceSpan span, List<TypeNode> elementTypes, List<String> fieldNames)
	{
		super(span);
		this.elementTypes = elementTypes;
		this.fieldNames = fieldNames;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTypeReference(this);
	}
}