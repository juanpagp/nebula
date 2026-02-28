package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.GenericParam;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class TraitDeclaration extends Declaration
{
	public final String name;
	public final List<GenericParam> typeParams;
	public final List<MethodDeclaration> members;

	public TraitDeclaration(SourceSpan span, String name, List<GenericParam> typeParams, List<MethodDeclaration> members)
	{
		super(span);
		this.name = name;
		this.typeParams = typeParams;
		this.members = members;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTraitDeclaration(this);
	}
}