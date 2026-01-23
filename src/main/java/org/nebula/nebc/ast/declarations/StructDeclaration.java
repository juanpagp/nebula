package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.Type;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class StructDeclaration extends Declaration
{
	private final String name;
	private final List<Type> inheritance;
	private final List<Declaration> members;

	public StructDeclaration(SourceSpan span, String name, List<Type> inheritance, List<Declaration> members)
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