package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.SymbolTable;

public class ClassType extends CompositeType
{

	public ClassType(String name, SymbolTable parentScope)
	{
		super(name, parentScope);
	}
}