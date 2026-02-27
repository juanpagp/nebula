package org.nebula.nebc.semantic.types;

// A simple hierarchy to represent resolved types
public abstract class Type
{
	// Singleton types for basic primitives
	public static final Type ERROR = new PrimitiveType("<error>"); // The "Recovery" type
	public static final Type ANY = new PrimitiveType("<any>"); // For wildcards

	public abstract String name();

	public boolean isAssignableTo(Type destination)
	{
		if (this == ERROR || destination == ERROR)
			return true; // Prevent cascading errors
		if (this == ANY || destination == ANY)
			return true;
		return this.name().equals(destination.name()); // Simple name-based equality for now
	}
}

// You will eventually need ClassType, FunctionType, etc.