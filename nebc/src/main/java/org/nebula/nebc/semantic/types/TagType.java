package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.SymbolTable;
import org.nebula.nebc.semantic.symbol.MethodSymbol;
import org.nebula.nebc.semantic.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a named compile-time set of types defined by a {@code tag} declaration.
 * <p>
 * A {@code TagType} is not a concrete value type — it cannot appear as a variable's
 * declared type. Its sole purpose is to act as a <em>generic bound</em>:
 * {@code <T: MyTag>} means T must be one of the types (or satisfy one of the traits)
 * enumerated inside {@code MyTag}.
 * <p>
 * Examples:
 * <pre>
 *   tag { i8, i16, i32, i64 }  as Signed;    // set of concrete types
 *   tag { str, T: Stringable }  as Printable; // concrete type + trait constraint
 * </pre>
 * <p>
 * When the tag contains a {@link TraitType} member (e.g. {@code Stringable}), that
 * trait is the "ambient trait" for the tag: it populates the member scope so that
 * trait methods are resolvable on a tag-bound type parameter.
 */
public final class TagType extends CompositeType
{
    /**
     * The resolved member types of this tag.
     * Each entry is either a concrete {@link Type} (e.g. {@code str}, {@code i32})
     * or a {@link TraitType} (meaning "any type implementing this trait").
     */
    private final List<Type> memberTypes = new ArrayList<>();

    public TagType(String name, SymbolTable parentScope)
    {
        super(name, parentScope);
    }

    /**
     * Adds a resolved member type to this tag's set.
     * Called during the {@code declareTagBodies} phase of semantic analysis.
     */
    public void addMember(Type type)
    {
        memberTypes.add(type);
    }

    /**
     * Returns all member types registered in this tag.
     */
    public List<Type> getMemberTypes()
    {
        return memberTypes;
    }

    /**
     * Returns the first {@link TraitType} member of this tag, if any.
     * <p>
     * This is the "ambient trait" used for:
     * <ul>
     *   <li>Populating the tag's member scope with callable methods.</li>
     *   <li>vtable dispatch during code generation for erased generics.</li>
     * </ul>
     */
    public Optional<TraitType> findAmbientTrait()
    {
        return memberTypes.stream()
            .filter(t -> t instanceof TraitType)
            .map(t -> (TraitType) t)
            .findFirst();
    }

    /**
     * Populates this tag's member scope with all required-method signatures
     * from every {@link TraitType} member. This enables method resolution on
     * a type parameter bound to this tag (e.g. {@code item.toStr()} when
     * {@code T: Printable}).
     * <p>
     * Called once, after all member types have been added via {@link #addMember}.
     */
    public void buildMemberScope()
    {
        for (Type member : memberTypes)
        {
            if (member instanceof TraitType trait)
            {
                for (Symbol sym : trait.getMemberScope().getSymbols().values())
                {
                    if (sym instanceof MethodSymbol ms)
                        memberScope.forceDefine(ms);
                }
            }
        }
    }

    /**
     * Checks whether a concrete type satisfies this tag bound.
     * A type satisfies the bound if:
     * <ul>
     *   <li>Its name matches one of the concrete-type members directly, OR</li>
     *   <li>It provides implementations for all methods of a {@link TraitType} member, OR</li>
     *   <li>It is a {@link TupleType} or {@link ArrayType} whose element types all
     *       satisfy the trait structurally (recursive structural check).</li>
     * </ul>
     *
     * @param concrete    the concrete type to check
     * @param implScopes  a map from primitive/composite types to their impl scopes,
     *                    used to verify trait satisfaction
     */
    public boolean isSatisfiedBy(Type concrete, java.util.Map<Type, SymbolTable> implScopes)
    {
        for (Type member : memberTypes)
        {
            if (member instanceof TraitType trait)
            {
                // Direct impl scope check
                SymbolTable scope = resolveImplScope(concrete, implScopes);
                if (scope != null && trait.findMissingMethod(scope) == null)
                    return true;
                // Structural check for compound types (tuples / arrays)
                if (isStructurallySatisfied(concrete, trait, implScopes))
                    return true;
            }
            else if (member.name().equals(concrete.name()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively checks whether a structural type ({@link TupleType} or
     * {@link ArrayType}) satisfies a trait by verifying that every element type
     * either has a direct impl scope or is itself structurally satisfied.
     */
    private static boolean isStructurallySatisfied(
            Type concrete, TraitType trait, java.util.Map<Type, SymbolTable> implScopes)
    {
        if (concrete instanceof TupleType tt)
        {
            for (Type elem : tt.elementTypes)
            {
                SymbolTable scope = resolveImplScope(elem, implScopes);
                boolean directOk = (scope != null && trait.findMissingMethod(scope) == null);
                if (!directOk && !isStructurallySatisfied(elem, trait, implScopes))
                    return false;
            }
            return true;
        }
        if (concrete instanceof ArrayType at)
        {
            SymbolTable scope = resolveImplScope(at.baseType, implScopes);
            if (scope != null && trait.findMissingMethod(scope) == null)
                return true;
            return isStructurallySatisfied(at.baseType, trait, implScopes);
        }
        return false;
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private static SymbolTable resolveImplScope(
            Type concrete, java.util.Map<Type, SymbolTable> implScopes)
    {
        SymbolTable scope = implScopes.get(concrete);
        if (scope != null)
            return scope;
        if (concrete instanceof CompositeType ct)
            return ct.getMemberScope();
        return null;
    }
}
