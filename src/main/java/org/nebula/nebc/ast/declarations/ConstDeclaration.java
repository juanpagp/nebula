package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class ConstDeclaration extends Declaration
{
	public final VariableDeclaration declaration;

	public ConstDeclaration(SourceSpan span, VariableDeclaration declaration)
	{
		super(span);
		this.declaration = declaration;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitConstDeclaration(this);
	}
}
