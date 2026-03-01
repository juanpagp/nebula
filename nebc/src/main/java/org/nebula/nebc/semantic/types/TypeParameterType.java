package org.nebula.nebc.semantic.types;

/**
 * Represents a generic type parameter placeholder such as {@code T} in
 * {@code void println<T: Displayable>(T item)}.
 * <p>
 * During semantic analysis this type is used as a stand-in for the real
 * concrete type. During code generation the monomorphizer substitutes
 * it with a concrete {@link Type}.
 */
public final class TypeParameterType extends Type
{
    private final String name;

    /**
     * The trait bound on this parameter, or {@code null} if unconstrained.
     * E.g. for {@code T: Displayable}, {@code bound} = TraitType("Displayable").
     */
    private final TraitType bound;

    public TypeParameterType(String name, TraitType bound)
    {
        this.name = name;
        this.bound = bound;
    }

    @Override
    public String name()
    {
        return name;
    }

    /** Returns the trait bound, or {@code null} when unconstrained. */
    public TraitType getBound()
    {
        return bound;
    }

    /** Returns {@code true} when this parameter carries a trait bound. */
    public boolean hasBound()
    {
        return bound != null;
    }

    /**
     * A type parameter is assignable to its bound, and anything is assignable
     * to an unconstrained type parameter (within the same generic context).
     */
    @Override
    public boolean isAssignableTo(Type target)
    {
        if (this.equals(target))
            return true;
        if (target == Type.ANY)
            return true;
        if (bound != null && target instanceof TraitType tt)
        {
            return bound.equals(tt);
        }
        return false;
    }
}
