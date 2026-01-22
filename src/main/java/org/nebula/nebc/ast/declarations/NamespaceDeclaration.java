package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class NamespaceDeclaration extends Declaration
{
	private final String name;
	private final List<ASTNode> members;

	public NamespaceDeclaration(SourceSpan span, String name, List<ASTNode> members)
	{
		super(span);
		this.name = name;
		this.members = members;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitNamespaceDeclaration(this);
	}
}