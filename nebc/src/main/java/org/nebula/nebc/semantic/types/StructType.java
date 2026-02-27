package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.SymbolTable;

public class StructType extends CompositeType
{

	public StructType(String name, SymbolTable parentScope)
	{
		super(name, parentScope);
	}
}