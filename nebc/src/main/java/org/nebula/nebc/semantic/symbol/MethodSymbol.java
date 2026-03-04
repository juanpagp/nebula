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
	private String traitName = null;
	/** LLVM bitcode for the type-erased version of this generic method (null for non-generics). */
	private byte[] genericBitcode = null;
	/**
	 * True when this symbol was synthesised by the compiler for a structural
	 * type (tuple / array) that implements a trait by structural recursion rather
	 * than via an explicit {@code impl} declaration in Nebula source code.
	 */
	private boolean syntheticStructural = false;

	public MethodSymbol(String name, FunctionType type, List<Modifier> modifiers, boolean isExtern, ASTNode declarationNode, List<TypeParameterType> typeParameters)
	{
		super(name, type, declarationNode);
		this.modifiers = List.copyOf(modifiers);
		this.isExtern = isExtern;
		this.typeParameters = List.copyOf(typeParameters);
	}

	public String getTraitName()
	{
		return traitName;
	}

	public void setTraitName(String traitName)
	{
		this.traitName = traitName;
	}

	public byte[] getGenericBitcode()
	{
		return genericBitcode;
	}

	public void setGenericBitcode(byte[] bitcode)
	{
		this.genericBitcode = bitcode;
	}

	public boolean isSyntheticStructural()
	{
		return syntheticStructural;
	}

	public void setSyntheticStructural(boolean value)
	{
		this.syntheticStructural = value;
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
