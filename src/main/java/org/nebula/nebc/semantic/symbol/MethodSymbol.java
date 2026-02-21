package org.nebula.nebc.semantic.symbol;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.Modifier;
import org.nebula.nebc.semantic.types.FunctionType;

import java.util.List;

/**
 * Represents a method or function declaration.
 * The associated type is always a {@link FunctionType}.
 */
public final class MethodSymbol extends Symbol
{

	private final List<Modifier> modifiers;

	public MethodSymbol(String name, FunctionType type, List<Modifier> modifiers, ASTNode declarationNode)
	{
		super(name, type, declarationNode);
		this.modifiers = List.copyOf(modifiers);
	}

	@Override
	public FunctionType getType()
	{
		return (FunctionType) super.getType();
	}

	public List<Modifier> getModifiers()
	{
		return modifiers;
	}

	public boolean hasModifier(Modifier modifier)
	{
		return modifiers.contains(modifier);
	}
}
