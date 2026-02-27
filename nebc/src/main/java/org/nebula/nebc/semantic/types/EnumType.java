package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.SymbolTable;

public class EnumType extends CompositeType
{
	public EnumType(String name, SymbolTable parentScope)
	{
		super(name, parentScope);
	}

	@Override
	public boolean isAssignableTo(Type target)
	{
		return this.equals(target) || target == Type.ANY;
	}
}
