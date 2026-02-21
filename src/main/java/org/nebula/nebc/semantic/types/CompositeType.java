package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.SymbolTable;

/**
 * Base class for types that have members: Classes, Structs, Traits, Unions.
 * The member scope is a {@link SymbolTable} that holds field and method
 * symbols.
 */
public abstract class CompositeType extends Type
{

	protected final String name;
	protected final SymbolTable memberScope;

	public CompositeType(String name, SymbolTable parentScope)
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