package org.nebula.nebc.semantic.types;

public class ClassType extends Type
{
	private final String name;

	// In the future: public final SemType parent;
	public ClassType(String name)
	{
		this.name = name;
	}

	@Override
	public String name()
	{
		return name;
	}
}