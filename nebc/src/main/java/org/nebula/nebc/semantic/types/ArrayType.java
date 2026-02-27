package org.nebula.nebc.semantic.types;

public class ArrayType extends Type
{
	public final Type baseType;
	public final int elementCount; // 0 for slices/dynamic arrays if applicable

	public ArrayType(Type baseType, int elementCount)
	{
		this.baseType = baseType;
		this.elementCount = elementCount;
	}

	@Override
	public String name()
	{
		return baseType.name() + "[]";
	}

	@Override
	public boolean isAssignableTo(Type destination)
	{
		if (this == destination)
			return true;
		if (destination instanceof ArrayType arr)
		{
			return this.baseType.isAssignableTo(arr.baseType);
		}
		return super.isAssignableTo(destination);
	}
}
