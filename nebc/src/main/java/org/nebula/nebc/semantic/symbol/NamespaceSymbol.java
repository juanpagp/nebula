package org.nebula.nebc.semantic.symbol;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.semantic.SymbolTable;
import org.nebula.nebc.semantic.types.NamespaceType;

/**
 * Represents a namespace declaration.
 * Namespaces are scoping containers that hold their own {@link SymbolTable}.
 */
public final class NamespaceSymbol extends Symbol
{
	public NamespaceSymbol(String name, NamespaceType type, ASTNode declarationNode)
	{
		super(name, type, declarationNode);
	}

	public SymbolTable getMemberTable()
	{
		return ((NamespaceType) getType()).getMemberScope();
	}
}
