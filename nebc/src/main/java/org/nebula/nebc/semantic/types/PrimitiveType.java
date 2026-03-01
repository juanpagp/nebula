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
	public static final PrimitiveType STR = new PrimitiveType("str");

	// Special FFI/CVT primitive: Ref<T> — a raw view into a Region, used in extern
	// "C"
	public static final PrimitiveType REF = new PrimitiveType("Ref")
	{
		@Override
		public boolean isAssignableTo(Type target)
		{
			return true; // Ref is an opaque FFI type; allow passing to any parameter
		}
	};

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
		scope.define(TypeSymbol.builtIn("str", STR));

		// FFI/CVT primitives
		scope.define(TypeSymbol.builtIn("Ref", REF));
		scope.define(TypeSymbol.builtIn("Region", REF)); // Reuse REF logic for Region for now as a catch-all
	}

	public boolean isValidMainMethodReturnType()
	{
		return this == I32 || this == VOID;
	}

	@Override
	public boolean isAssignableTo(Type target)
	{
		if (this == Type.ANY || target == Type.ANY)
			return true;
		if (this.equals(target))
			return true;

		// string is now STR (a struct-like value type)
		if (this == STR && target instanceof PrimitiveType pt && pt == REF)
		{
			return true; // str is implicitly compatible with Ref for FFI compatibility
		}

		if (target instanceof PrimitiveType pTarget)
		{
			if (this.isInteger() && pTarget.isInteger())
			{
				// Signed to unsigned: only if they are the same type (already handled by
				// equals)
				// or if it's a widening conversion between SAME signedness.
				boolean thisSigned = this.name.startsWith("i");
				boolean targetSigned = pTarget.name.startsWith("i");

				if (thisSigned != targetSigned)
				{
					// Allow widening even if signedness differs.
					// LLVMCodeGenerator.emitCast handles this using SExt if source is signed,
					// or ZExt if source is unsigned.
					return this.getBitWidth() <= pTarget.getBitWidth();
				}

				return this.getBitWidth() <= pTarget.getBitWidth();
			}
			else if (this.isFloat() && pTarget.isFloat())
			{
				return this.getBitWidth() <= pTarget.getBitWidth();
			}
			else if (this.isInteger() && pTarget.isFloat())
			{
				return true; // Widening integer to float
			}
			return false; // Disallow narrowing and float-to-int implicit casts
		}

		return super.isAssignableTo(target);
	}

	public boolean isInteger()
	{
		return this == I8 || this == U8 || this == I16 || this == U16 || this == I32 || this == U32 || this == I64 || this == U64;
	}

	public boolean isFloat()
	{
		return this == F32 || this == F64;
	}

	public int getBitWidth()
	{
		return switch (this.name)
		{
			case "i8", "u8" -> 8;
			case "i16", "u16" -> 16;
			case "i32", "u32", "f32" -> 32;
			case "i64", "u64", "f64", "str" -> 64;
			default -> 0;
		};
	}

	public String name()
	{
		return name;
	}

	public static PrimitiveType getByName(String name)
	{
		return switch (name)
		{
			case "i8" -> I8;
			case "u8" -> U8;
			case "i16" -> I16;
			case "u16" -> U16;
			case "i32" -> I32;
			case "u32" -> U32;
			case "i64" -> I64;
			case "u64" -> U64;
			case "f32" -> F32;
			case "f64" -> F64;
			case "bool" -> BOOL;
			case "void" -> VOID;
			case "char" -> CHAR;
			case "str" -> STR;
			case "Ref" -> REF;
			default -> null;
		};
	}
}
