package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class NamespaceDeclaration extends Declaration
{
	public final String name;
	public final List<ASTNode> members; // Null or empty implies file-scoped

	public NamespaceDeclaration(SourceSpan span, String name, List<ASTNode> members)
	{
		super(span);
		this.name = name;
		this.members = members;
	}

	public boolean isFileScoped()
	{
		return members == null || members.isEmpty();
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitNamespaceDeclaration(this);
	}
}