package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.Type;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class VariableDeclaration extends Declaration
{
	private final Type type; // null if using 'var'
	private final List<VariableDeclarator> declarators;
	private final boolean isVar; // true if 'var' was used
	private final boolean isConst;

	public VariableDeclaration(SourceSpan span, Type type, List<VariableDeclarator> declarators, boolean isVar, boolean isConst)
	{
		super(span);
		this.type = type;
		this.declarators = declarators;
		this.isVar = isVar;
		this.isConst = isConst;
	}

	public Type getType()
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

	public boolean isConst()
	{
		return isConst;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitVariableDeclaration(this);
	}
}