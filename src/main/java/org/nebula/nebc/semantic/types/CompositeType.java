package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.Scope;

/**
 * Base class for things that have members: Classes, Structs, Traits, Unions.
 */
public abstract class CompositeType extends Type
{
	protected final String name;
	protected final Scope memberScope;

	public CompositeType(String name, Scope parentScope)
	{
		this.name = name;
		this.memberScope = new Scope(parentScope);
	}

	public Scope getMemberScope()
	{
		return memberScope;
	}

	@Override
	public String name()
	{
		return name;
	}
}