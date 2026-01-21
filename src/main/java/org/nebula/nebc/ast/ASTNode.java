package org.nebula.nebc.ast;

import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public abstract class ASTNode
{
	public final SourceSpan span;

	protected ASTNode(SourceSpan span)
	{
		this.span = span;
	}

	public void visit(AstVisitor v)
	{
		for (ASTNode child : children())
		{
			child.visit(v);
		}
	}

	protected abstract List<ASTNode> children();
}
