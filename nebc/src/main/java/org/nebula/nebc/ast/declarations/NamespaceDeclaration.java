package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class NamespaceDeclaration extends Declaration
{
	public final String name;
	public final List<ASTNode> members; // Null or empty implies file-scoped
	public final boolean isBlockDeclaration;

	public NamespaceDeclaration(SourceSpan span, String name, List<ASTNode> members, boolean isBlockDeclaration)
	{
		super(span);
		this.name = name;
		this.members = members;
		this.isBlockDeclaration = isBlockDeclaration;
	}

	public boolean isFileScoped()
	{
		return !isBlockDeclaration;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitNamespaceDeclaration(this);
	}
}