package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.GenericParam;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class ClassDeclaration extends Declaration
{
	public final String name;
	public final List<GenericParam> typeParams;
	public final List<TypeNode> inheritance;
	public final List<Declaration> members;

	public ClassDeclaration(SourceSpan span, String name, List<GenericParam> typeParams, List<TypeNode> inheritance, List<Declaration> members)
	{
		super(span);
		this.name = name;
		this.typeParams = typeParams;
		this.inheritance = inheritance;
		this.members = members;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitClassDeclaration(this);
	}
}