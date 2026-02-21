package org.nebula.nebc.semantic.types;

import java.util.List;
import java.util.stream.Collectors;

public class FunctionType extends Type
{
	public final Type returnType;
	public final List<Type> parameterTypes;

	public FunctionType(Type returnType, List<Type> parameterTypes)
	{
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
	}

	@Override
	public String name()
	{
		String params = parameterTypes.stream()
				.map(Type::name)
				.collect(Collectors.joining(", "));
		return "(" + params + ") -> " + returnType.name();
	}

	@Override
	public boolean isAssignableTo(Type destination)
	{
		// Structural equality for functions (simplified)
		if (destination instanceof FunctionType other)
		{
			if (this.parameterTypes.size() != other.parameterTypes.size())
				return false;
			// Note: In a full system, you'd check contravariance for args and covariance for return
			return this.returnType.isAssignableTo(other.returnType);
		}
		return super.isAssignableTo(destination);
	}

	public Type getReturnType()
	{
		return returnType;
	}
}