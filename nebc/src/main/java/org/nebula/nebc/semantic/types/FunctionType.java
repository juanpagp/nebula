package org.nebula.nebc.semantic.types;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a function type with optional CVT (Causal Validity Tracking) parameter information.
 * <p>
 * This is used for both normal Nebula functions (with plain Type parameters)
 * and extern "C" declarations (with ParameterInfo that includes CVT modifiers).
 * <p>
 * For normal functions:
 * - parameterTypes contains the resolved types
 * - parameterInfo is null
 * <p>
 * For extern "C" functions:
 * - parameterTypes contains the resolved types
 * - parameterInfo contains the parameters with CVT modifiers
 * - This allows semantic analysis to validate FFI constraints
 */
public class FunctionType extends Type
{
	public final Type returnType;
	public final List<Type> parameterTypes;
	public final List<ParameterInfo> parameterInfo;  // Optional: for extern functions with CVT info

	/**
	 * Constructor for normal Nebula functions (no CVT modifiers)
	 */
	public FunctionType(Type returnType, List<Type> parameterTypes)
	{
		this(returnType, parameterTypes, null);
	}

	/**
	 * Constructor for extern "C" functions with CVT parameter information
	 */
	public FunctionType(Type returnType, List<Type> parameterTypes, List<ParameterInfo> parameterInfo)
	{
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
		this.parameterInfo = parameterInfo;
	}

	@Override
	public String name()
	{
		String params;
		if (parameterInfo != null && !parameterInfo.isEmpty())
		{
			// Use CVT-annotated parameter info for extern functions
			params = parameterInfo.stream().map(ParameterInfo::toString).collect(Collectors.joining(", "));
		}
		else
		{
			// Use plain types for normal functions
			params = parameterTypes.stream().map(Type::name).collect(Collectors.joining(", "));
		}
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

	/**
	 * Check if this is an extern "C" function (has CVT parameter info)
	 */
	public boolean isExternFunction()
	{
		return parameterInfo != null;
	}

	/**
	 * Get parameter info for a specific parameter index
	 * Only valid for extern functions with parameterInfo
	 */
	public ParameterInfo getParameterInfo(int index)
	{
		if (parameterInfo == null || index < 0 || index >= parameterInfo.size())
			return null;
		return parameterInfo.get(index);
	}
}
