package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.SymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassType extends CompositeType
{

	/** Direct parent classes declared with `: Parent` syntax. */
	private final List<ClassType> parentTypes = new ArrayList<>();

	public ClassType(String name, SymbolTable parentScope)
	{
		super(name, parentScope);
	}

	/**
	 * Records a direct parent class. Called by the semantic analyser during
	 * {@code visitClassDeclaration} after all inheritance clauses are resolved.
	 */
	public void addParent(ClassType parent)
	{
		if (!parentTypes.contains(parent))
		{
			parentTypes.add(parent);
		}
	}

	/** Returns the ordered list of direct parent {@link ClassType}s. */
	public List<ClassType> getParentTypes()
	{
		return Collections.unmodifiableList(parentTypes);
	}

	/** Returns {@code true} if this class directly or transitively inherits from {@code other}. */
	public boolean isAssignableTo(ClassType other)
	{
		if (this == other)
			return true;
		for (ClassType p : parentTypes)
		{
			if (p.isAssignableTo(other))
				return true;
		}
		return false;
	}

	/**
	 * Overrides the general {@link Type#isAssignableTo(Type)} so that a child class
	 * is considered assignable to any of its ancestor {@link ClassType}s.
	 */
	@Override
	public boolean isAssignableTo(Type destination)
	{
		if (destination instanceof ClassType other)
			return isAssignableTo(other);
		return super.isAssignableTo(destination);
	}
}