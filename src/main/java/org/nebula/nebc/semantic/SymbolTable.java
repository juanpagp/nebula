package org.nebula.nebc.semantic;

import org.nebula.nebc.semantic.symbol.NamespaceSymbol;
import org.nebula.nebc.semantic.symbol.Symbol;
import org.nebula.nebc.semantic.symbol.TypeSymbol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A scoped symbol table that maps names to {@link Symbol} objects.
 * Supports hierarchical (parent-chain) lookup and qualified name resolution via
 * "::".
 *
 * <p>
 * This replaces the old {@code Scope} which mapped names directly to
 * {@code Type}.
 * By mapping to {@code Symbol}, we preserve declaration metadata (modifiers,
 * mutability,
 * source spans) separately from the type information.
 */
public class SymbolTable
{

	private final SymbolTable parent;
	private final Map<String, Symbol> symbols = new LinkedHashMap<>();

	public SymbolTable(SymbolTable parent)
	{
		this.parent = parent;
	}

	/**
	 * Defines a symbol in this scope. Returns false if a symbol with the same name
	 * already exists in this scope (does not check parent scopes).
	 */
	public boolean define(Symbol symbol)
	{
		if (symbols.containsKey(symbol.getName()))
			return false;
		symbols.put(symbol.getName(), symbol);
		return true;
	}

	/**
	 * Resolves a name by searching this scope first, then walking up the parent
	 * chain.
	 * Supports qualified names using "::" (e.g. "ns::Foo").
	 */
	public Symbol resolve(String name)
	{
		// Handle qualified names: ns::User
		if (name.contains("::"))
		{
			String[] parts = name.split("::", 2);
			Symbol prefix = resolve(parts[0]);

			if (prefix instanceof NamespaceSymbol ns)
			{
				return ns.getMemberTable().resolve(parts[1]);
			}
			return null;
		}

		// Standard local → parent resolution
		Symbol sym = symbols.get(name);
		if (sym != null)
			return sym;
		if (parent != null)
			return parent.resolve(name);
		return null;
	}

	/**
	 * Resolves a name in the current scope only (no parent chain walk).
	 */
	public Symbol resolveLocal(String name)
	{
		return symbols.get(name);
	}

	/**
	 * Resolves a name as a type. Only returns a result if the resolved symbol is a
	 * {@link TypeSymbol}. This is used for type-annotation resolution (e.g.
	 * {@code var x: Foo}).
	 */
	public TypeSymbol resolveType(String name)
	{
		Symbol sym = resolve(name);
		if (sym instanceof TypeSymbol ts)
			return ts;
		return null;
	}

	/**
	 * Returns an unmodifiable view of all symbols defined directly in this scope.
	 */
	public Map<String, Symbol> getSymbols()
	{
		return Collections.unmodifiableMap(symbols);
	}

	public SymbolTable getParent()
	{
		return parent;
	}
}
