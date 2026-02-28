package org.nebula.nebc.semantic.types;

import org.nebula.nebc.ast.declarations.MethodDeclaration;
import org.nebula.nebc.semantic.SymbolTable;
import org.nebula.nebc.semantic.symbol.MethodSymbol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the type of a declared trait (e.g. {@code trait Displayable}).
 * <p>
 * A TraitType owns a member scope containing the abstract method signatures
 * that any implementing class must provide. Methods with default implementations
 * are optional to override.
 */
public final class TraitType extends CompositeType
{
    /** Abstract method requirements: method name → signature. */
    private final Map<String, MethodSymbol> requiredMethods = new LinkedHashMap<>();

    /** Default implementations: method name → AST node (body present). */
    private final Map<String, MethodDeclaration> defaultImpls = new LinkedHashMap<>();

    public TraitType(String name, SymbolTable parentScope)
    {
        super(name, parentScope);
    }

    /**
     * Registers a method signature as a requirement of this trait (no default body).
     * Also defines it in the member scope so that member-access type-checking works.
     */
    public void addRequiredMethod(MethodSymbol method)
    {
        requiredMethods.put(method.getName(), method);
        memberScope.define(method);
    }

    /**
     * Registers a method with a default implementation (body present in trait).
     * Implementors may override it but are not required to.
     */
    public void addDefaultMethod(MethodSymbol method, MethodDeclaration body)
    {
        defaultImpls.put(method.getName(), body);
        memberScope.define(method);
    }

    /** Returns all method signatures that an implementor must satisfy (no default). */
    public Map<String, MethodSymbol> getRequiredMethods()
    {
        return requiredMethods;
    }

    /** Returns all default method bodies keyed by method name. */
    public Map<String, MethodDeclaration> getDefaultImpls()
    {
        return defaultImpls;
    }

    /**
     * Checks whether a given {@link SymbolTable} scope satisfies all required
     * (non-defaulted) methods of this trait. Returns the name of the first
     * missing method, or {@code null} if all requirements are met.
     */
    public String findMissingMethod(SymbolTable scope)
    {
        for (String methodName : requiredMethods.keySet())
        {
            if (scope.resolveLocal(methodName) == null)
            {
                return methodName;
            }
        }
        return null;
    }

    /**
     * Overload for CompositeType (kept for compat): checks the composite's
     * member scope.
     */
    public String findMissingMethod(CompositeType impl)
    {
        return findMissingMethod(impl.getMemberScope());
    }
}
