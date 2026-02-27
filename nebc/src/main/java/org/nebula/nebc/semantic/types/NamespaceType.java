package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.SymbolTable;

/**
 * Represents a namespace. While conceptually namespaces aren't values,
 * keeping them as a Type subclass simplifies certain resolution patterns
 * (e.g. member access on namespace-qualified names).
 */
public class NamespaceType extends Type
{

	private final String name;
	private final SymbolTable memberScope;

	public NamespaceType(String name, SymbolTable parentScope)
	{
		this.name = name;
		this.memberScope = new SymbolTable(parentScope);
	}

	public SymbolTable getMemberScope()
	{
		return memberScope;
	}

	@Override
	public String name()
	{
		return name;
	}
}