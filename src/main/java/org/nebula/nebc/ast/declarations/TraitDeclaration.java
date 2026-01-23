package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class TraitDeclaration extends Declaration
{
	private final String name;
	private final List<MethodDeclaration> members;

	public TraitDeclaration(SourceSpan span, String name, List<MethodDeclaration> members)
	{
		super(span);
		this.name = name;
		this.members = members;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTraitDeclaration(this);
	}
}