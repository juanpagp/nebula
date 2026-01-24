package org.nebula.nebc.ast;

import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * The root AST node representing an entire Nebula program.
 * It serves as the container for global directives and top-level declarations.
 */
public class CompilationUnit extends ASTNode
{
	public final List<ASTNode> directives;
	public final List<ASTNode> declarations;

	/**
	 * Constructs a new CompilationUnit.
	 * * @param directives   The list of directives (like alias or use statements).
	 *
	 * @param declarations The list of top-level declarations (classes, methods, namespaces, etc.).
	 */
	public CompilationUnit(SourceSpan span, List<ASTNode> directives, List<ASTNode> declarations)
	{
		super(span);
		this.directives = new ArrayList<>(directives);
		this.declarations = new ArrayList<>(declarations);
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitCompilationUnit(this);
	}
}