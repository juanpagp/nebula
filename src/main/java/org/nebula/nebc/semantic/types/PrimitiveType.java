package org.nebula.nebc.semantic.types;

public class PrimitiveType extends Type
{
	private final String name;

	public PrimitiveType(String name)
	{
		this.name = name;
	}

	@Override
	public String name()
	{
		return name;
	}
}