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
 * This is a pure utility class with no mutable state. Every method requires an
 * explicit {@link LLVMContextRef} so type refs are always bound to the correct
 * LLVM context.
 */
public final class LLVMTypeMapper {

	private LLVMTypeMapper() {
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
	public static LLVMTypeRef map(LLVMContextRef ctx, Type type) {
		if (type == null) {
			throw new CodegenException("Internal error: Attempted to map a null type to LLVM.");
		}
		if (type instanceof PrimitiveType pt) {
			return mapPrimitive(ctx, pt);
		}
		if (type instanceof FunctionType ft) {
			return mapFunction(ctx, ft);
		}
		if (type instanceof CompositeType) {
			// Classes and structs are pointer-to-opaque-struct for now
			return LLVMPointerTypeInContext(ctx, /* AddressSpace */ 0);
		}
		throw new CodegenException("Unmappable type: " + type.name());
	}

	// ── Primitives ──────────────────────────────────────────────

	private static LLVMTypeRef mapPrimitive(LLVMContextRef ctx, PrimitiveType pt) {
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
		if (pt == PrimitiveType.STR) {
			LLVMTypeRef[] fields = {
					LLVMPointerTypeInContext(ctx, 0), // ptr
					LLVMInt64TypeInContext(ctx) // len
			};
			PointerPointer<LLVMTypeRef> fieldTypes = new PointerPointer<>(fields);
			return LLVMStructTypeInContext(ctx, fieldTypes, 2, /* isPacked */ 0);
		}

		// Ref, ANY → i8* (pointer)
		if (pt == PrimitiveType.REF || pt == (PrimitiveType) Type.ANY)
			return LLVMPointerTypeInContext(ctx, 0);

		throw new CodegenException("Unmappable primitive type: " + pt.name());
	}

	// ── Functions ───────────────────────────────────────────────

	private static LLVMTypeRef mapFunction(LLVMContextRef ctx, FunctionType ft) {
		LLVMTypeRef returnType = map(ctx, ft.returnType);
		int paramCount = ft.parameterTypes.size();
		if (paramCount == 0) {
			return LLVMFunctionType(returnType, new LLVMTypeRef(), 0, /* isVarArg */ 0);
		}
		// Build PointerPointer array of param type refs
		PointerPointer<LLVMTypeRef> paramTypes = new PointerPointer<>(paramCount);
		for (int i = 0; i < paramCount; i++) {
			paramTypes.put(i, map(ctx, ft.parameterTypes.get(i)));
		}
		return LLVMFunctionType(returnType, paramTypes, paramCount, /* isVarArg */ 0);
	}
}
