package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.Parameter;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class ConstructorDeclaration extends Declaration
{
	public final String name; // Usually matches class name or "new"
	public final List<Parameter> parameters;
	public final ASTNode body; // Block

	public ConstructorDeclaration(SourceSpan span, String name, List<Parameter> parameters, ASTNode body)
	{
		super(span);
		this.name = name;
		this.parameters = parameters;
		this.body = body;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitConstructorDeclaration(this);
	}
}