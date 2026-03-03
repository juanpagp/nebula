package org.nebula.nebc.semantic.types;

/**
 * Semantic representation of an optional type {@code T?}.
 * <p>
 * An optional value is in one of two states:
 * <ul>
 *   <li><b>Present</b> — contains a value of type {@code T}.</li>
 *   <li><b>Absent</b>  — the {@code none} state; no value is held.</li>
 * </ul>
 *
 * <h3>Type rules enforced by the semantic analyser</h3>
 * <ol>
 *   <li>{@code none} is only assignable to {@code T?}, never to {@code T}.</li>
 *   <li>A {@code T?} may not be accessed via {@code .member} — safe access
 *       ({@code ?.}) or an explicit unwrap ({@code !} / {@code ??}) is
 *       required.</li>
 *   <li>A {@code T} is implicitly liftable to {@code T?}.</li>
 * </ol>
 *
 * <h3>LLVM runtime representation</h3>
 * Optionals are emitted as a two-field named struct
 * {@code { i1 present, T value }}.  For pointer-sized inner types the
 * {@code value} field is the pointer itself (a null pointer represents absent).
 */
public class OptionalType extends Type
{
	/** The non-optional inner type. */
	public final Type innerType;

	public OptionalType(Type innerType)
	{
		this.innerType = innerType;
	}

	@Override
	public String name()
	{
		return innerType.name() + "?";
	}

	@Override
	public boolean isAssignableTo(Type target)
	{
		if (this.equals(target))
			return true;
		if (target == Type.ANY)
			return true;
		// none (OptionalType wrapping ANY) is assignable to any T?
		if (innerType == Type.ANY && target instanceof OptionalType)
			return true;
		// T? is not implicitly assignable to T — must be unwrapped
		return false;
	}

	/**
	 * Returns {@code true} if this optional wraps exactly the given inner type.
	 * Used by the semantic analyser for {@code none}-assignment and lifting checks.
	 */
	public boolean wraps(Type t)
	{
		return innerType.equals(t);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof OptionalType other))
			return false;
		return innerType.equals(other.innerType);
	}

	@Override
	public int hashCode()
	{
		return 31 + innerType.hashCode();
	}
}
