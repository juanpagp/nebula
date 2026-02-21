package org.nebula.nebc.semantic.symbol;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.semantic.SymbolTable;

/**
 * Represents a namespace declaration.
 * Namespaces are not values — they have no type. They exist purely as scoping
 * containers that hold their own {@link SymbolTable}.
 */
public final class NamespaceSymbol extends Symbol
{

	private final SymbolTable memberTable;

	public NamespaceSymbol(String name, SymbolTable memberTable, ASTNode declarationNode)
	{
		super(name, null, declarationNode); // no type — namespaces aren't values
		this.memberTable = memberTable;
	}

	public SymbolTable getMemberTable()
	{
		return memberTable;
	}
}
