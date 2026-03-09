package org.nebula.nebc.semantic.symbol;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.semantic.SymbolTable;
import org.nebula.nebc.semantic.types.Type;

import java.util.Collections;
import java.util.List;

/**
 * A Symbol represents a named entity in the program: a variable, method, type,
 * or namespace.
 * Symbols live in a {@link org.nebula.nebc.semantic.SymbolTable} and carry
 * metadata about the
 * declaration (name, type, source location, modifiers) that is separate from
 * the Type itself.
 *
 * <p>
 * The key distinction: a <b>Type</b> describes the <i>shape</i> of a value
 * (e.g. "32-bit int",
 * "function (i32)->void", "class Foo with fields a,b"). A <b>Symbol</b>
 * describes a <i>named
 * entity</i> in a particular scope (e.g. "variable x of type i32, mutable,
 * declared on line 5").
 */
public abstract sealed class Symbol permits VariableSymbol, MethodSymbol, TypeSymbol, NamespaceSymbol
{
	/**
	 * Represents a single compile-time attribute attached to a symbol.
	 *
	 * <p>{@code path} is the qualified attribute name (e.g. {@code "test"} or
	 * {@code "std::derive"}).  {@code args} holds the string representation of
	 * each argument expression as it appeared in the source, which is sufficient
	 * for the reflection use-cases envisioned by {@code std::attributes}.</p>
	 */
	public record AttributeInfo(String path, List<String> args)
	{
		public AttributeInfo
		{
			args = Collections.unmodifiableList(args);
		}
	}

	private final String name;
	private final Type type;
	private final ASTNode declarationNode; // nullable for built-ins
	protected SymbolTable definedIn;
	private List<AttributeInfo> attributes = Collections.emptyList();

	protected Symbol(String name, Type type, ASTNode declarationNode)
	{
		this.name = name;
		this.type = type;
		this.declarationNode = declarationNode;
	}

	public SymbolTable getDefinedIn()
	{
		return definedIn;
	}

	public void setDefinedIn(SymbolTable definedIn)
	{
		this.definedIn = definedIn;
	}

	/**
	 * Returns the LLVM-compatible mangled name for this symbol.
	 * For extern functions, returns the original name.
	 * For others, returns a qualified name (e.g. std_io_println).
	 */
	public String getMangledName()
	{
		if (this instanceof MethodSymbol ms)
		{
			if (ms.isExtern())
				return name;
			if (ms.getOverriddenMangledName() != null)
				return ms.getOverriddenMangledName();
		}

		java.util.List<String> parts = new java.util.ArrayList<>();
		parts.add(name);

		if (this instanceof MethodSymbol ms && ms.getTraitName() != null)
		{
			parts.add(0, ms.getTraitName());
		}

		SymbolTable current = definedIn;
		while (current != null && current.getOwner() != null)
		{
			parts.add(0, current.getOwner().getName());
			current = current.getParent();
		}

		return String.join("__", parts);
	}

	/**
	 * Returns a human-readable qualified name using {@code ::} as the namespace
	 * separator (e.g. {@code mylib::run_test}).  Used by the attribute registry
	 * so that {@code std::reflect} exposes clean names to user-land code.
	 */
	public String getQualifiedName()
	{
		java.util.List<String> parts = new java.util.ArrayList<>();
		parts.add(name);

		if (this instanceof MethodSymbol ms && ms.getTraitName() != null)
		{
			parts.add(0, ms.getTraitName());
		}

		SymbolTable current = definedIn;
		while (current != null && current.getOwner() != null)
		{
			parts.add(0, current.getOwner().getName());
			current = current.getParent();
		}

		return String.join("::", parts);
	}

	public String getName()
	{
		return name;
	}

	/**
	 * Returns the type associated with this symbol.
	 * <ul>
	 * <li>For a variable: the variable's declared/inferred type (e.g. i32).</li>
	 * <li>For a method: the method's FunctionType signature.</li>
	 * <li>For a type symbol (class/struct/union): the Type it defines (e.g.
	 * ClassType).</li>
	 * <li>For a namespace: null — namespaces are not values.</li>
	 * </ul>
	 */
	public Type getType()
	{
		return type;
	}

	/**
	 * The AST node where this symbol was declared. May be null for built-in
	 * symbols.
	 */
	public ASTNode getDeclarationNode()
	{
		return declarationNode;
	}

	/**
	 * Returns the compile-time attributes attached to this symbol.
	 * Empty for symbols that have no {@code #[...]} annotations.
	 */
	public List<AttributeInfo> getAttributes()
	{
		return attributes;
	}

	/**
	 * Attaches compile-time attribute metadata to this symbol.
	 * Called by the semantic analyser after the symbol is created.
	 */
	public void setAttributes(List<AttributeInfo> attrs)
	{
		this.attributes = attrs != null ? List.copyOf(attrs) : Collections.emptyList();
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(" + name + (type != null ? " : " + type.name() : "") + ")";
	}
}
