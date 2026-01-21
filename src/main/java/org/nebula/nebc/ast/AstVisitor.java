package org.nebula.nebc.ast;

public abstract class AstVisitor<R>
{

	/**
	 * Called for every node
	 */
	public R visitNode(ASTNode node)
	{
		R result = enter(node);

		for (ASTNode child : node.children())
		{
			child.visit(this);
		}

		exit(node);
		return result;
	}

	/**
	 * Override selectively
	 */
	protected R enter(ASTNode node)
	{
		return null;
	}

	protected void exit(ASTNode node)
	{
	}
}