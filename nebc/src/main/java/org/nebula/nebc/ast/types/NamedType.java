package org.nebula.nebc.ast.types;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.Collections;
import java.util.List;

public class NamedType extends TypeNode
{
	public final String qualifiedName;
	public final List<TypeNode> genericArguments;

	public NamedType(SourceSpan span, String qualifiedName, List<TypeNode> genericArguments)
	{
		super(span);
		this.qualifiedName = qualifiedName;
		this.genericArguments = genericArguments != null ? genericArguments : Collections.emptyList();
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTypeReference(this);
	}
}