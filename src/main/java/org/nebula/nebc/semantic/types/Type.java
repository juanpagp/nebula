package org.nebula.nebc.semantic.types;

// A simple hierarchy to represent resolved types
public abstract class Type
{
	// Singleton types for basic primitives
	public static final Type I8 = new PrimitiveType("i8");
	public static final Type U8 = new PrimitiveType("u8");
	public static final Type I16 = new PrimitiveType("i16");
	public static final Type U16 = new PrimitiveType("u16");
	public static final Type I32 = new PrimitiveType("i32");
	public static final Type U32 = new PrimitiveType("u32");
	public static final Type F32 = new PrimitiveType("f32");
	public static final Type F64 = new PrimitiveType("f64");
	public static final Type BOOL = new PrimitiveType("bool");
	public static final Type VOID = new PrimitiveType("void");
	public static final Type STRING = new PrimitiveType("string");
	public static final Type ERROR = new PrimitiveType("<error>"); // The "Recovery" type
	public static final Type ANY = new PrimitiveType("<any>"); // For wildcards

	public abstract String name();

	public boolean isAssignableTo(Type destination)
	{
		if (this == ERROR || destination == ERROR)
			return true; // Prevent cascading errors
		if (destination == ANY)
			return true;
		return this.name().equals(destination.name()); // Simple name-based equality for now
	}
}

// You will eventually need ClassType, FunctionType, etc.