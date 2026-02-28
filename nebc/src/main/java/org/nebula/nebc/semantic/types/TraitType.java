package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.SymbolTable;
import org.nebula.nebc.semantic.symbol.MethodSymbol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the type of a declared trait (e.g. {@code trait Displayable}).
 * <p>
 * A TraitType owns a member scope containing the abstract method signatures
 * that any implementing class must provide.
 */
public final class TraitType extends CompositeType
{
    /** Abstract method requirements: method name → signature. */
    private final Map<String, MethodSymbol> requiredMethods = new LinkedHashMap<>();

    public TraitType(String name, SymbolTable parentScope)
    {
        super(name, parentScope);
    }

    /**
     * Registers a method signature as a requirement of this trait.
     * Also defines it in the member scope so that member-access type-checking
     * works.
     */
    public void addRequiredMethod(MethodSymbol method)
    {
        requiredMethods.put(method.getName(), method);
        memberScope.define(method);
    }

    /** Returns all method signatures that an implementor must satisfy. */
    public Map<String, MethodSymbol> getRequiredMethods()
    {
        return requiredMethods;
    }

    /**
     * Checks whether a given {@link CompositeType} satisfies all requirements
     * of this trait. Returns the name of the first missing method, or
     * {@code null} if all requirements are met.
     */
    public String findMissingMethod(CompositeType impl)
    {
        for (String methodName : requiredMethods.keySet())
        {
            if (impl.getMemberScope().resolveLocal(methodName) == null)
            {
                return methodName;
            }
        }
        return null;
    }
}
