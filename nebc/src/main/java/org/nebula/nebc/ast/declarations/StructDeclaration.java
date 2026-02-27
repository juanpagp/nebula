package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class StructDeclaration extends Declaration
{
	public final String name;
	public final List<TypeNode> inheritance;
	public final List<Declaration> members;

	public StructDeclaration(SourceSpan span, String name, List<TypeNode> inheritance, List<Declaration> members)
	{
		super(span);
		this.name = name;
		this.inheritance = inheritance;
		this.members = members;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitStructDeclaration(this);
	}
}