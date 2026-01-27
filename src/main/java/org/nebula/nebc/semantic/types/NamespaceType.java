package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.Scope;

public class NamespaceType extends Type
{
	private final String name;
	private final Scope memberScope;

	public NamespaceType(String name, Scope parentScope)
	{
		this.name = name;
		// Each namespace has its own scope pointing back to the parent
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