package org.nebula.nebc.ast.types;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.Collections;
import java.util.List;

public class NamedType extends Type
{
	private final String qualifiedName;
	private final List<Type> genericArguments;

	public NamedType(SourceSpan span, String qualifiedName, List<Type> genericArguments)
	{
		super(span);
		this.qualifiedName = qualifiedName;
		this.genericArguments = genericArguments != null ? genericArguments : Collections.emptyList();
	}

	public String getQualifiedName()
	{
		return qualifiedName;
	}

	public List<Type> getGenericArguments()
	{
		return genericArguments;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTypeReference(this);
	}
}