package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.SymbolTable;
import org.nebula.nebc.semantic.symbol.TypeSymbol;

/**
 * Represents a built-in primitive type (i32, f64, bool, etc.).
 * Instances are singletons defined as static fields.
 */
public class PrimitiveType extends Type
{

	public static final PrimitiveType I8 = new PrimitiveType("i8");
	public static final PrimitiveType U8 = new PrimitiveType("u8");
	public static final PrimitiveType I16 = new PrimitiveType("i16");
	public static final PrimitiveType U16 = new PrimitiveType("u16");
	public static final PrimitiveType I32 = new PrimitiveType("i32");
	public static final PrimitiveType U32 = new PrimitiveType("u32");
	public static final PrimitiveType I64 = new PrimitiveType("i64");
	public static final PrimitiveType U64 = new PrimitiveType("u64");
	public static final PrimitiveType F32 = new PrimitiveType("f32");
	public static final PrimitiveType F64 = new PrimitiveType("f64");
	public static final PrimitiveType BOOL = new PrimitiveType("bool");
	public static final PrimitiveType VOID = new PrimitiveType("void");
	public static final PrimitiveType CHAR = new PrimitiveType("char");
	public static final PrimitiveType STRING = new PrimitiveType("string");

	private final String name;

	public PrimitiveType(String name)
	{
		this.name = name;
	}

	/**
	 * Registers all built-in primitive types as {@link TypeSymbol}s in the given
	 * scope.
	 */
	public static void defineAll(SymbolTable scope)
	{
		scope.define(TypeSymbol.builtIn("i8", I8));
		scope.define(TypeSymbol.builtIn("i16", I16));
		scope.define(TypeSymbol.builtIn("i32", I32));
		scope.define(TypeSymbol.builtIn("i64", I64));
		scope.define(TypeSymbol.builtIn("u8", U8));
		scope.define(TypeSymbol.builtIn("u16", U16));
		scope.define(TypeSymbol.builtIn("u32", U32));
		scope.define(TypeSymbol.builtIn("u64", U64));

		scope.define(TypeSymbol.builtIn("f32", F32));
		scope.define(TypeSymbol.builtIn("f64", F64));

		scope.define(TypeSymbol.builtIn("bool", BOOL));
		scope.define(TypeSymbol.builtIn("void", VOID));
		scope.define(TypeSymbol.builtIn("char", CHAR));
		scope.define(TypeSymbol.builtIn("string", STRING));
	}

	public boolean isValidMainMethodReturnType()
	{
		return this == I32 || this == VOID;
	}

	@Override
	public boolean isAssignableTo(Type target)
	{
		if (this.equals(target))
			return true;

		if (target instanceof PrimitiveType pTarget)
		{
			// Permissive numeric system: any numeric can be assigned to any other numeric
			// (widening, narrowing, float<->int)
			if ((this.isInteger() || this.isFloat()) && (pTarget.isInteger() || pTarget.isFloat()))
			{
				return true;
			}
		}

		return super.isAssignableTo(target);
	}

	public boolean isInteger()
	{
		return this == I8 || this == U8 || this == I16 || this == U16 ||
				this == I32 || this == U32 || this == I64 || this == U64;
	}

	public boolean isFloat()
	{
		return this == F32 || this == F64;
	}

	public int getBitWidth()
	{
		return switch (this.name)
		{
			case "i8", "u8" ->
					8;
			case "i16", "u16" ->
					16;
			case "i32", "u32", "f32" ->
					32;
			case "i64", "u64", "f64" ->
					64;
			default ->
					0;
		};
	}

	@Override
	public String name()
	{
		return name;
	}
}
