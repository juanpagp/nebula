package org.nebula.nebc.semantic.symbol;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.semantic.types.Type;

/**
 * Represents a variable, parameter, or field declaration.
 * Carries mutability information beyond what the Type provides.
 */
public final class VariableSymbol extends Symbol
{

	private final boolean mutable;

	public VariableSymbol(String name, Type type, boolean mutable, ASTNode declarationNode)
	{
		super(name, type, declarationNode);
		this.mutable = mutable;
	}

	/**
	 * Whether this variable can be reassigned (var vs const).
	 */
	public boolean isMutable()
	{
		return mutable;
	}
}
