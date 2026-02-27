package org.nebula.nebc.ast;

import org.nebula.nebc.frontend.diagnostic.SourceSpan;

/**
 * Base class for all nodes in the Nebula Abstract Syntax Tree (AST).
 */
public abstract class ASTNode
{
	private final SourceSpan span;

	protected ASTNode(SourceSpan span)
	{
		this.span = span != null ? span : SourceSpan.unknown();
	}

	/**
	 * @return The location of this node in the source code.
	 */
	public SourceSpan getSpan()
	{
		return span;
	}

	/**
	 * Visitor pattern entry point.
	 */
	public abstract <R> R accept(ASTVisitor<R> visitor);

	/**
	 * @return A tree-structured string representation of this node and its children.
	 */
	@Override
	public String toString()
	{
		return org.nebula.nebc.ast.util.ASTPrinter.print(this);
	}
}