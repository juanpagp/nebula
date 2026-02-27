package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class VariableDeclaration extends Declaration
{
	public final TypeNode type; // null if using 'var'
	public final List<VariableDeclarator> declarators;
	public final boolean isVar; // true if 'var' was used

	public VariableDeclaration(SourceSpan span, TypeNode type, List<VariableDeclarator> declarators, boolean isVar)
	{
		super(span);
		this.type = type;
		this.declarators = declarators;
		this.isVar = isVar;
	}

	public TypeNode getType()
	{
		return type;
	}

	public List<VariableDeclarator> getDeclarators()
	{
		return declarators;
	}

	public boolean isVar()
	{
		return isVar;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitVariableDeclaration(this);
	}
}