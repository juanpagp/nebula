package org.nebula.nebc.ast;

import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

/**
 * The root node of the AST, all other nodes are children of a CompilationUnit node.
 * There's exactly one CompilationUnit per AST instance and it represents a Nebula source file (.neb).
 */
public class CompilationUnit extends ASTNode
{

	protected CompilationUnit(SourceSpan span)
	{
		super(span);
	}

	@Override
	protected List<ASTNode> children()
	{
		return List.of();
	}
}
