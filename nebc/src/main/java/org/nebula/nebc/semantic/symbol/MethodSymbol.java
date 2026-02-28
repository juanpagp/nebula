package org.nebula.nebc.semantic.symbol;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.Modifier;
import org.nebula.nebc.semantic.types.FunctionType;
import org.nebula.nebc.semantic.types.TypeParameterType;

import java.util.List;

/**
 * Represents a method or function declaration.
 * The associated type is always a {@link FunctionType}.
 */
public final class MethodSymbol extends Symbol
{
	private final List<Modifier> modifiers;
	private final boolean isExtern;
	private final List<TypeParameterType> typeParameters;

	public MethodSymbol(String name, FunctionType type, List<Modifier> modifiers, boolean isExtern, ASTNode declarationNode, List<TypeParameterType> typeParameters)
	{
		super(name, type, declarationNode);
		this.modifiers = List.copyOf(modifiers);
		this.isExtern = isExtern;
		this.typeParameters = List.copyOf(typeParameters);
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

	public boolean isExtern()
	{
		return isExtern;
	}

	public List<TypeParameterType> getTypeParameters()
	{
		return typeParameters;
	}

	public boolean hasModifier(Modifier modifier)
	{
		return modifiers.contains(modifier);
	}
}
