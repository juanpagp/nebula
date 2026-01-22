package org.nebula.nebc.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The root AST node representing an entire Nebula program.
 * It serves as the container for global directives and top-level declarations.
 */
public class CompilationUnit implements ASTNode
{
	private final List<ASTNode> directives;
	private final List<ASTNode> declarations;

	/**
	 * Constructs a new CompilationUnit.
	 * * @param directives   The list of directives (like alias or use statements).
	 * @param declarations The list of top-level declarations (classes, methods, namespaces, etc.).
	 */
	public CompilationUnit(List<ASTNode> directives, List<ASTNode> declarations)
	{
		this.directives = new ArrayList<>(directives);
		this.declarations = new ArrayList<>(declarations);
	}

	/**
	 * @return An unmodifiable list of directives in this unit.
	 */
	public List<ASTNode> getDirectives()
	{
		return Collections.unmodifiableList(directives);
	}

	/**
	 * @return An unmodifiable list of top-level declarations (Class, Struct, Method, etc.).
	 */
	public List<ASTNode> getDeclarations()
	{
		return Collections.unmodifiableList(declarations);
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitCompilationUnit(this);
	}

	@Override
	public String toString()
	{
		return "CompilationUnit{" +
				"directives=" + directives.size() +
				", declarations=" + declarations.size() +
				'}';
	}
}