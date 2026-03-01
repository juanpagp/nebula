package org.nebula.nebc.codegen;

import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.nebula.nebc.semantic.types.CompositeType;
import org.nebula.nebc.semantic.types.FunctionType;
import org.nebula.nebc.semantic.types.PrimitiveType;
import org.nebula.nebc.semantic.types.Type;

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
		if (type instanceof CompositeType ct)
		{
			return mapComposite(ctx, ct);
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
