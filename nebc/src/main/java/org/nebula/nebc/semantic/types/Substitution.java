package org.nebula.nebc.semantic.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper to substitute {@link TypeParameterType}s with concrete {@link Type}s.
 * Used during monomorphization and generic method calls.
 */
public class Substitution
{
    private final Map<TypeParameterType, Type> mapping = new HashMap<>();

    public void bind(TypeParameterType param, Type concrete)
    {
        mapping.put(param, concrete);
    }

    /**
     * Recursively replaces all type parameters in the given type with their
     * bound concrete types.
     */
    public Type substitute(Type type)
    {
        if (type instanceof TypeParameterType tpt)
        {
            return mapping.getOrDefault(tpt, tpt);
        }
        if (type instanceof ArrayType at)
        {
            Type substitutedBase = substitute(at.baseType);
            if (substitutedBase == at.baseType)
                return at;
            return new ArrayType(substitutedBase, at.elementCount);
        }
        if (type instanceof TupleType tt)
        {
            List<Type> substitutedElements = tt.elementTypes.stream().map(this::substitute).collect(Collectors.toList());
            // Optimization: if no elements changed, return original
            boolean changed = false;
            for (int i = 0; i < tt.elementTypes.size(); i++)
            {
                if (substitutedElements.get(i) != tt.elementTypes.get(i))
                {
                    changed = true;
                    break;
                }
            }
            return changed ? new TupleType(substitutedElements) : tt;
        }
        if (type instanceof FunctionType ft)
        {
            Type substitutedReturn = substitute(ft.returnType);
            List<Type> substitutedParams = ft.parameterTypes.stream().map(this::substitute).collect(Collectors.toList());

            boolean changed = substitutedReturn != ft.returnType;
            if (!changed)
            {
                for (int i = 0; i < ft.parameterTypes.size(); i++)
                {
                    if (substitutedParams.get(i) != ft.parameterTypes.get(i))
                    {
                        changed = true;
                        break;
                    }
                }
            }

            if (!changed)
                return ft;
            return new FunctionType(substitutedReturn, substitutedParams, ft.parameterInfo);
        }
        // NOTE: if Classes/Structs ever become generic, they'd need substitution here
        // too.
        return type;
    }

    /** Returns true if there are no bindings in this substitution. */
    public boolean isEmpty()
    {
        return mapping.isEmpty();
    }

    /** Returns the underling mapping. */
    public Map<TypeParameterType, Type> getMapping()
    {
        return mapping;
    }
}
