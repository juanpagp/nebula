package org.nebula.nebc.semantic.types;

/**
 * Represents a generic type parameter placeholder such as {@code T} in
 * {@code void println<T: Stringable>(T item)}.
 * <p>
 * During semantic analysis this type is used as a stand-in for the real
 * concrete type. During code generation the monomorphizer substitutes
 * it with a concrete {@link Type}.
 * <p>
 * The bound can be either a {@link TraitType} (trait-based constraint) or a
 * {@link TagType} (tag-based set constraint), both of which extend
 * {@link CompositeType} and expose a member scope for method resolution.
 */
public final class TypeParameterType extends Type
{
    private final String name;

    /**
     * The bound on this parameter, or {@code null} if unconstrained.
     * Can be a {@link TraitType} (e.g. {@code T: Stringable}) or a
     * {@link TagType} (e.g. {@code T: Printable} where Printable is a tag).
     */
    private final CompositeType bound;

    public TypeParameterType(String name, CompositeType bound)
    {
        this.name = name;
        this.bound = bound;
    }

    @Override
    public String name()
    {
        return name;
    }

    /**
     * Returns the bound (a {@link TraitType} or {@link TagType}), or
     * {@code null} when unconstrained.
     */
    public CompositeType getBound()
    {
        return bound;
    }

    /** Returns {@code true} when this parameter carries a bound. */
    public boolean hasBound()
    {
        return bound != null;
    }

    /**
     * A type parameter is assignable to its bound, and anything is assignable
     * to an unconstrained type parameter (within the same generic context).
     * During semantic analysis, type parameters are treated as compatible with
     * any concrete type, since monomorphization resolves the actual types.
     */
    @Override
    public boolean isAssignableTo(Type target)
    {
        if (this.equals(target))
            return true;
        if (target == Type.ANY)
            return true;
        // Unconstrained type params are compatible with any type at the SA level
        if (bound == null)
            return true;
        // Bounded type param is assignable to its bound
        if (bound.equals(target))
            return true;
        // Bounded type param is also compatible with concrete types (resolved at monomorphization)
        return true;
    }
}
