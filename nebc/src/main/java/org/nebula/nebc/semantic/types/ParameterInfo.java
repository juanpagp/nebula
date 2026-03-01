package org.nebula.nebc.semantic.types;

import org.nebula.nebc.ast.CVTModifier;

/**
 * Represents a function parameter in the semantic layer with CVT information.
 * 
 * This extends beyond the AST Parameter by associating a resolved Type
 * with the CVT modifier, allowing semantic analysis to validate
 * FFI parameter constraints.
 */
public class ParameterInfo
{
	public final CVTModifier cvtModifier;
	public final Type type;
	public final String name;

	public ParameterInfo(CVTModifier cvtModifier, Type type, String name)
	{
		this.cvtModifier = cvtModifier;
		this.type = type;
		this.name = name;
	}

	public String name()
	{
		return name;
	}

	public Type type()
	{
		return type;
	}

	public String cvtHint()
	{
		return cvtModifier != null ? cvtModifier.keyword() : null;
	}

	/**
	 * Check if this parameter has a CVT modifier (keeps or drops)
	 */
	public boolean hasCVTModifier()
	{
		return cvtModifier != null && cvtModifier.isFFIModifier();
	}

	/**
	 * Check if this parameter uses the "keeps" modifier
	 */
	public boolean isKeeps()
	{
		return cvtModifier == CVTModifier.KEEPS;
	}

	/**
	 * Check if this parameter uses the "drops" modifier
	 */
	public boolean isDrops()
	{
		return cvtModifier == CVTModifier.DROPS;
	}

	@Override
	public String toString()
	{
		String mod = hasCVTModifier() ? (cvtModifier.keyword() + " ") : "";
		return mod + type.name() + " " + name;
	}
}
