package org.nebula.nebc.semantic;

import org.nebula.nebc.semantic.types.NamespaceType;
import org.nebula.nebc.semantic.types.Type;

import java.util.HashMap;
import java.util.Map;

public class Scope
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
		// Handle qualified names: nest::User
		if (name.contains("::"))
		{
			String[] parts = name.split("::", 2);
			Type prefix = resolve(parts[0]); // Find 'nest'

			if (prefix instanceof NamespaceType ns)
			{
				return ns.getMemberScope().resolve(parts[1]); // Find 'User' in 'nest'
			}
			return null;
		}

		// Standard local resolution
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