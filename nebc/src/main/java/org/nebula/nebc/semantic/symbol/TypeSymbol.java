package org.nebula.nebc.semantic.symbol;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.semantic.types.Type;

/**
 * Represents a named type declaration (class, struct, union, trait, or built-in
 * primitive).
 * This is the "name → type" binding: when someone writes {@code class Foo}, a
 * {@code TypeSymbol("Foo", classType)} is placed into the symbol table.
 *
 * <p>
 * When resolving type annotations (e.g. {@code var x: Foo}), the analyzer looks
 * up
 * TypeSymbols specifically, not all symbols.
 */
public final class TypeSymbol extends Symbol
{

	public TypeSymbol(String name, Type type, ASTNode declarationNode)
	{
		super(name, type, declarationNode);
	}

	/**
	 * Convenience factory for built-in types that have no AST declaration node.
	 */
	public static TypeSymbol builtIn(String name, Type type)
	{
		return new TypeSymbol(name, type, null);
	}
}
