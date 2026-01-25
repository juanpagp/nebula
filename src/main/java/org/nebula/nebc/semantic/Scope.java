package org.nebula.nebc.semantic;

import org.nebula.nebc.semantic.types.Type;

import java.util.HashMap;
import java.util.Map;

class Scope
{
	private final Scope parent;
	private final Map<String, Type> symbols = new HashMap<>();

	public Scope(Scope parent)
	{
		this.parent = parent;
	}

	public boolean define(String name, Type type)
	{
		if (symbols.containsKey(name))
			return false; // Duplicate!
		symbols.put(name, type);
		return true;
	}

	public Type resolve(String name)
	{
		if (symbols.containsKey(name))
			return symbols.get(name);
		if (parent != null)
			return parent.resolve(name);
		return null;
	}

	public Scope getParent()
	{
		return parent;
	}
}