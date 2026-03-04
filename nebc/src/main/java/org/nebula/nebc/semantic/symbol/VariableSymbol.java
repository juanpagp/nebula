package org.nebula.nebc.semantic.symbol;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.semantic.types.Type;

/**
 * Represents a variable, parameter, or field declaration.
 * Carries mutability and ownership information beyond what the Type provides.
 */
public final class VariableSymbol extends Symbol
{

	private final boolean mutable;

	/**
	 * {@code true} when the field was declared with the {@code backlink} modifier,
	 * meaning it is a non-owning back-reference and must never be freed by the
	 * owning object's drop function.
	 */
	private final boolean backlink;

	/** Create a regular (non-backlink) symbol. */
	public VariableSymbol(String name, Type type, boolean mutable, ASTNode declarationNode)
	{
		this(name, type, mutable, false, declarationNode);
	}

	/** Create a symbol with an explicit backlink flag. */
	public VariableSymbol(String name, Type type, boolean mutable, boolean isBacklink,
						  ASTNode declarationNode)
	{
		super(name, type, declarationNode);
		this.mutable = mutable;
		this.backlink = isBacklink;
	}

	/** Whether this variable can be reassigned (var vs const). */
	public boolean isMutable()
	{
		return mutable;
	}

	/**
	 * Whether this is a non-owning back-reference ({@code backlink} modifier).
	 * When {@code true}, the destructor generated for the containing class must
	 * NOT free this field, and assigning a local into this field must NOT
	 * transfer ownership of that local.
	 */
	public boolean isBacklink()
	{
		return backlink;
	}
}
