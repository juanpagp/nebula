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
	private final java.util.List<NamespaceSymbol> imports = new java.util.ArrayList<>();
	private Symbol owner = null; // The symbol that owns this table (e.g. NamespaceSymbol)

	public SymbolTable(SymbolTable parent)
	{
		this.parent = parent;
	}

	public Symbol getOwner()
	{
		return owner;
	}

	public void setOwner(Symbol owner)
	{
		this.owner = owner;
	}

	/**
	 * Adds a namespace to the "opened" imports for this scope.
	 * When resolving a name, if it's not found locally, we'll check these
	 * namespaces.
	 */
	public void addImport(NamespaceSymbol ns)
	{
		if (!imports.contains(ns))
		{
			imports.add(ns);
		}
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
		symbol.setDefinedIn(this);
		return true;
	}

	/**
	 * Resolves a name by searching this scope first, then walking up the parent
	 * chain.
	 * Supports qualified names using "::" (e.g. "ns::Foo").
	 * Also searches imported namespaces.
	 */
	public Symbol resolve(String name)
	{
		return resolve(name, true);
	}

	/**
	 * Internal resolution logic.
	 *
	 * @param name      The name to resolve.
	 * @param useParent Whether to continue searching in parent scopes if not found
	 *                  locally or in imports.
	 */
	private Symbol resolve(String name, boolean useParent)
	{
		// Handle qualified names: ns::User
		if (name.contains("::"))
		{
			String[] parts = name.split("::", 2);
			Symbol prefix = resolve(parts[0], useParent);

			if (prefix instanceof NamespaceSymbol ns)
			{
				// When resolving a qualified name, we only look inside that namespace.
				// We don't want to walk up its parent chain if it's not found there,
				// because that would be searching outside the qualified namespace.
				return ns.getMemberTable().resolve(parts[1], false);
			}
			return null;
		}

		// 1. Standard local resolution
		Symbol sym = symbols.get(name);
		if (sym != null)
			return sym;

		// 2. Search in imported namespaces (e.g. if 'std::io' was imported, check it
		// for 'println')
		for (NamespaceSymbol ns : imports)
		{
			// CRITICAL: We only search the imported namespace's local symbols and ITS
			// imports.
			// We DO NOT walk up its parent chain (useParent=false), as that would likely
			// lead back to the global scope and cause infinite recursion.
			Symbol importedSym = ns.getMemberTable().resolve(name, false);
			if (importedSym != null)
				return importedSym;
		}

		// 3. Parent resolution
		if (useParent && parent != null)
			return parent.resolve(name, true);
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
