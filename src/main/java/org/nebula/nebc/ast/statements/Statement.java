package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.AstVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public abstract class Statement extends ASTNode
{
	protected Statement(SourceSpan span)
	{
		super(span);
	}

	@Override
	public void visit(AstVisitor v)
	{

	}
}
