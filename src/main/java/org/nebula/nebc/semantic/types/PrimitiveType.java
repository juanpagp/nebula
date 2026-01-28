package org.nebula.nebc.semantic.types;

import org.nebula.nebc.semantic.Scope;

public class PrimitiveType extends Type
{
	public static final PrimitiveType I8   = new PrimitiveType("i8");
	public static final PrimitiveType U8   = new PrimitiveType("u8");
	public static final PrimitiveType I16  = new PrimitiveType("i16");
	public static final PrimitiveType U16  = new PrimitiveType("u16");
	public static final PrimitiveType I32  = new PrimitiveType("i32");
	public static final PrimitiveType U32  = new PrimitiveType("u32");
	public static final PrimitiveType I64  = new PrimitiveType("i64");
	public static final PrimitiveType U64  = new PrimitiveType("u64");
	public static final PrimitiveType F32  = new PrimitiveType("f32");
	public static final PrimitiveType F64  = new PrimitiveType("f64");
	public static final PrimitiveType BOOL = new PrimitiveType("bool");
	public static final PrimitiveType VOID = new PrimitiveType("void");
	public static final PrimitiveType CHAR = new PrimitiveType("char");
	public static final PrimitiveType STRING = new PrimitiveType("string");

	private final String name;

	public PrimitiveType(String name)
	{
		this.name = name;
	}

	public static void defineAll(Scope currentScope)
	{
		currentScope.define("i8", PrimitiveType.I8);
		currentScope.define("i16", PrimitiveType.I16);
		currentScope.define("i32", PrimitiveType.I32);
		currentScope.define("i64", PrimitiveType.I64);
		currentScope.define("u8", PrimitiveType.U8);
		currentScope.define("u16", PrimitiveType.U16);
		currentScope.define("u32", PrimitiveType.U32);
		currentScope.define("u64", PrimitiveType.U64);

		currentScope.define("f32", PrimitiveType.F32);
		currentScope.define("f64", PrimitiveType.F64);

		currentScope.define("char", PrimitiveType.CHAR);
		currentScope.define("string", PrimitiveType.STRING);
	}

	@Override
	public String name()
	{
		return name;
	}
}
