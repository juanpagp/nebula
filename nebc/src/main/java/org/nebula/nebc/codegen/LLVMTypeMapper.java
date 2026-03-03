package org.nebula.nebc.codegen;

import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.nebula.nebc.semantic.types.ArrayType;
import org.nebula.nebc.semantic.types.CompositeType;
import org.nebula.nebc.semantic.types.EnumType;
import org.nebula.nebc.semantic.types.FunctionType;
import org.nebula.nebc.semantic.types.OptionalType;
import org.nebula.nebc.semantic.types.PrimitiveType;
import org.nebula.nebc.semantic.types.TupleType;
import org.nebula.nebc.semantic.types.Type;
import org.nebula.nebc.semantic.types.TypeParameterType;
import org.nebula.nebc.semantic.types.UnionType;

import static org.bytedeco.llvm.global.LLVM.*;

/**
 * Maps Nebula semantic {@link Type} objects to LLVM {@link LLVMTypeRef} values.
 * <p>
 * This class caches named struct types to ensure type consistency within an
 * LLVM context.
 */
public final class LLVMTypeMapper
{

	private LLVMTypeMapper()
	{
		// Utility class
	}

	/**
	 * Converts a Nebula semantic type to its LLVM IR representation.
	 *
	 * @param ctx  The LLVM context in which the type is created.
	 * @param type The Nebula semantic type to map.
	 * @return The corresponding LLVM type ref.
	 * @throws CodegenException if the type cannot be mapped.
	 */
	public static LLVMTypeRef map(LLVMContextRef ctx, Type type)
	{
		if (type == null)
		{
			new RuntimeException("DEBUG: null type passed to LLVMTypeMapper.map").printStackTrace(System.err);
			throw new CodegenException("Internal error: Attempted to map a null type to LLVM.");
		}
		if (type instanceof PrimitiveType pt)
		{
			return mapPrimitive(ctx, pt);
		}
		if (type instanceof FunctionType ft)
		{
			return mapFunction(ctx, ft);
		}
		// Enums are represented as plain i32 discriminants.
		if (type instanceof EnumType)
		{
			return LLVMInt32TypeInContext(ctx);
		}
		// Tagged unions: { i32 tag, [UNION_PAYLOAD_BYTES x i8] payload }
		if (type instanceof UnionType ut)
		{
			return getOrCreateUnionStructType(ctx, ut);
		}
		// Optional T? → { i1 present, T value }
		if (type instanceof OptionalType ot)
		{
			return getOrCreateOptionalStructType(ctx, ot);
		}
		// Arrays are represented as opaque pointers (pointer to element type).
		if (type instanceof ArrayType at)
		{
			return LLVMPointerTypeInContext(ctx, 0);
		}
		if (type instanceof CompositeType ct)
		{
			return mapComposite(ctx, ct);
		}
		if (type instanceof TupleType tt)
		{
			return getOrCreateTupleType(ctx, tt);
		}
		if (type instanceof TypeParameterType tpt)
		{
			// Unsubstituted type parameter reached codegen — this indicates a generic
			// struct/class or method was not properly skipped before monomorphization.
			new RuntimeException("DEBUG: TypeParameterType '" + tpt.name() + "' reached LLVMTypeMapper").printStackTrace(System.err);
			throw new CodegenException(
				"Unsubstituted type parameter '" + tpt.name() + "' reached codegen. "
				+ "Generic instantiation (monomorphization) is not yet supported.");
		}
		throw new CodegenException("Unmappable type: " + type.name());
	}

	// ── Primitives ──────────────────────────────────────────────

	private static LLVMTypeRef mapPrimitive(LLVMContextRef ctx, PrimitiveType pt)
	{
		// Identity comparison — PrimitiveType uses singleton instances
		if (pt == PrimitiveType.VOID)
			return LLVMVoidTypeInContext(ctx);
		if (pt == PrimitiveType.BOOL)
			return LLVMInt1TypeInContext(ctx);

		if (pt == PrimitiveType.I8 || pt == PrimitiveType.U8)
			return LLVMInt8TypeInContext(ctx);
		if (pt == PrimitiveType.I16 || pt == PrimitiveType.U16)
			return LLVMInt16TypeInContext(ctx);
		if (pt == PrimitiveType.I32 || pt == PrimitiveType.U32)
			return LLVMInt32TypeInContext(ctx);
		if (pt == PrimitiveType.I64 || pt == PrimitiveType.U64)
			return LLVMInt64TypeInContext(ctx);

		if (pt == PrimitiveType.F32)
			return LLVMFloatTypeInContext(ctx);
		if (pt == PrimitiveType.F64)
			return LLVMDoubleTypeInContext(ctx);

		// char → i32 (Unicode code point)
		if (pt == PrimitiveType.CHAR)
			return LLVMInt32TypeInContext(ctx);

		// str → { i8*, i64 }
		if (pt == PrimitiveType.STR)
		{
			return getOrCreateStructType(ctx, pt);
		}

		// Ref, ANY → i8* (pointer)
		if (pt == PrimitiveType.REF || pt == (PrimitiveType) Type.ANY)
			return LLVMPointerTypeInContext(ctx, 0);

		throw new CodegenException("Unmappable primitive type: " + pt.name());
	}

	// ── Functions ───────────────────────────────────────────────

	private static LLVMTypeRef mapComposite(LLVMContextRef ctx, CompositeType ct)
	{
		// For now, we always use pointers to structs for composite types in Nebula
		LLVMTypeRef structType = getOrCreateStructType(ctx, ct);
		return LLVMPointerTypeInContext(ctx, 0);
	}

	private static final java.util.Map<String, LLVMTypeRef> structTypes = new java.util.HashMap<>();

	public static LLVMTypeRef getOrCreateStructType(LLVMContextRef ctx, Type type)
	{
		if (type == PrimitiveType.STR)
		{
			if (structTypes.containsKey("str"))
			{
				return structTypes.get("str");
			}

			LLVMTypeRef[] fields = {LLVMPointerTypeInContext(ctx, 0), LLVMInt64TypeInContext(ctx)};
			LLVMTypeRef structType = LLVMStructCreateNamed(ctx, "str");
			LLVMStructSetBody(structType, new PointerPointer<>(fields), 2, 0);
			structTypes.put("str", structType);
			return structType;
		}

		if (!(type instanceof CompositeType ct))
		{
			throw new CodegenException("Cannot get struct type for non-composite type: " + type.name());
		}

		if (structTypes.containsKey(ct.name()))
		{
			return structTypes.get(ct.name());
		}

		LLVMTypeRef structType = LLVMStructCreateNamed(ctx, ct.name());
		structTypes.put(ct.name(), structType);

		// Populate fields
		java.util.Collection<org.nebula.nebc.semantic.symbol.Symbol> symbols = ct.getMemberScope().getSymbols().values();
		java.util.List<org.nebula.nebc.semantic.symbol.VariableSymbol> fields = symbols.stream().filter(s -> s instanceof org.nebula.nebc.semantic.symbol.VariableSymbol vs && !vs.getName().equals("this")).map(s -> (org.nebula.nebc.semantic.symbol.VariableSymbol) s).toList();

		LLVMTypeRef[] fieldTypesArr = new LLVMTypeRef[fields.size()];
		for (int i = 0; i < fields.size(); i++)
		{
			fieldTypesArr[i] = map(ctx, fields.get(i).getType());
		}

		PointerPointer<LLVMTypeRef> fieldTypes = new PointerPointer<>(fieldTypesArr);
		LLVMStructSetBody(structType, fieldTypes, fields.size(), /* isPacked */ 0);

		return structType;
	}

	public static void clearCache()
	{
		structTypes.clear();
	}

	/**
	 * Maps a {@link TupleType} to an LLVM anonymous struct.
	 * Tuples are represented as {@code { T0, T1, ... }} where each field maps the
	 * corresponding element type.  The struct is cached by the tuple's display name.
	 */
	public static LLVMTypeRef getOrCreateTupleType(LLVMContextRef ctx, TupleType tt)
	{
		String key = "tuple." + tt.name();
		if (structTypes.containsKey(key))
			return structTypes.get(key);

		LLVMTypeRef structType = LLVMStructCreateNamed(ctx, key);
		structTypes.put(key, structType); // register early to break potential recursion

		LLVMTypeRef[] fieldTypesArr = new LLVMTypeRef[tt.elementTypes.size()];
		for (int i = 0; i < tt.elementTypes.size(); i++)
		{
			fieldTypesArr[i] = map(ctx, tt.elementTypes.get(i));
		}
		LLVMStructSetBody(structType, new PointerPointer<>(fieldTypesArr), fieldTypesArr.length, 0);
		return structType;
	}

	/**
	 * Tagged-union IR layout: {@code { i32 tag, [PAYLOAD_BYTES x i8] payload }}.
	 * The payload region is large enough to hold the widest variant payload.
	 * Variant-specific accessor structs use bitcasting at the call-site.
	 */
	public static final int UNION_PAYLOAD_BYTES = 16; // big enough for str {i8*,i64}

	public static LLVMTypeRef getOrCreateUnionStructType(LLVMContextRef ctx, UnionType ut)
	{
		String key = "union." + ut.name();
		if (structTypes.containsKey(key))
			return structTypes.get(key);

		LLVMTypeRef structType = LLVMStructCreateNamed(ctx, key);
		structTypes.put(key, structType); // register early to break recursion

		// { i32, [UNION_PAYLOAD_BYTES x i8] }
		LLVMTypeRef i32t    = LLVMInt32TypeInContext(ctx);
		LLVMTypeRef payload = LLVMArrayType(LLVMInt8TypeInContext(ctx), UNION_PAYLOAD_BYTES);
		LLVMTypeRef[] fields = {i32t, payload};
		LLVMStructSetBody(structType, new PointerPointer<>(fields), 2, 0);
		return structType;
	}

	/**
	 * Optional IR layout: {@code { i1 present, T value }}.
	 */
	public static LLVMTypeRef getOrCreateOptionalStructType(LLVMContextRef ctx, OptionalType ot)
	{
		String key = "opt." + ot.innerType.name();
		if (structTypes.containsKey(key))
			return structTypes.get(key);

		LLVMTypeRef structType = LLVMStructCreateNamed(ctx, key);
		structTypes.put(key, structType);

		LLVMTypeRef i1t    = LLVMInt1TypeInContext(ctx);
		LLVMTypeRef innerT = map(ctx, ot.innerType);
		LLVMTypeRef[] fields = {i1t, innerT};
		LLVMStructSetBody(structType, new PointerPointer<>(fields), 2, 0);
		return structType;
	}

	private static LLVMTypeRef mapFunction(LLVMContextRef ctx, FunctionType ft)
	{
		LLVMTypeRef returnType = map(ctx, ft.returnType);
		int paramCount = ft.parameterTypes.size();
		if (paramCount == 0)
		{
			return LLVMFunctionType(returnType, new LLVMTypeRef(), 0, /* isVarArg */ 0);
		}
		// Build PointerPointer array of param type refs
		PointerPointer<LLVMTypeRef> paramTypes = new PointerPointer<>(paramCount);
		for (int i = 0; i < paramCount; i++)
		{
			paramTypes.put(i, map(ctx, ft.parameterTypes.get(i)));
		}
		return LLVMFunctionType(returnType, paramTypes, paramCount, /* isVarArg */ 0);
	}
}
