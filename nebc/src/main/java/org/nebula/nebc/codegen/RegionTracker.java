package org.nebula.nebc.codegen;

import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.nebula.nebc.semantic.types.PrimitiveType;

import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;

/**
 * Tracks heap-allocated Regions (class instances and dynamic strings) within a
 * single function scope and emits deterministic deallocation via Last-Usage
 * Analysis (LUA).
 * <p>
 * Each tracked region gets a companion {@code i1} alloca (the "live flag") that
 * is initialised to {@code true}. When ownership of the region is transferred
 * (passed to a {@code drops} parameter or captured in a class field), the flag
 * is set to {@code false}. Before every {@code ret}, the tracker emits cleanup
 * for each region whose flag is still {@code true}.
 * <p>
 * <b>Deep Drop:</b> For HEAP_OBJECT regions with a known type name, cleanup
 * dispatches to the compiler-generated {@code TypeName__drop(ptr)} function
 * which recursively frees all owned fields before freeing {@code this}. If no
 * drop function exists (e.g. for imported library types), falls back to a plain
 * {@code neb_free}.
 * <p>
 * <b>Zero-cost optimisation:</b> If a region's flag was never toggled (no
 * conditional transfer ever occurred), the cleanup emits a raw free call without
 * branching, avoiding the extra basic blocks entirely.
 * <p>
 * <b>LIFO ordering:</b> Regions are freed in reverse-declaration order (last
 * declared, first freed), matching the standard destructor semantics of C++/Rust
 * and preventing use-after-free from forward references.
 */
public class RegionTracker
{
	// ─── Region kinds ────────────────────────────────────────────────────────

	/**
	 * Describes how the heap pointer should be obtained from the tracked alloca.
	 */
	public enum RegionKind
	{
		/**
		 * The alloca holds a {@code ptr} directly (class instances).
		 * Dispatches to {@code TypeName__drop(ptr)} if available, else
		 * falls back to a plain {@code neb_free(ptr)}.
		 */
		HEAP_OBJECT,

		/**
		 * The alloca holds a {@code %str} struct ({@code { ptr, i64 }}).
		 * Extracts field 0 (the heap data pointer) and calls {@code neb_free}.
		 */
		STRING_PROXY
	}

	// ─── Tracked region ──────────────────────────────────────────────────────

	private static final class TrackedRegion
	{
		final String variableName;
		final LLVMValueRef variableAlloca;
		final LLVMValueRef liveFlag;
		final RegionKind kind;
		/** For HEAP_OBJECT — the Nebula class name, used to dispatch to {@code TypeName__drop}. */
		final String typeName;
		boolean wasTransferred;

		TrackedRegion(String variableName, LLVMValueRef variableAlloca,
					  LLVMValueRef liveFlag, RegionKind kind, String typeName)
		{
			this.variableName = variableName;
			this.variableAlloca = variableAlloca;
			this.liveFlag = liveFlag;
			this.kind = kind;
			this.typeName = typeName;
			this.wasTransferred = false;
		}
	}

	// ─── Fields ──────────────────────────────────────────────────────────────

	private final List<TrackedRegion> regions = new ArrayList<>();
	private final LLVMContextRef context;
	private final LLVMBuilderRef builder;
	private final LLVMModuleRef module;

	public RegionTracker(LLVMContextRef context, LLVMBuilderRef builder, LLVMModuleRef module)
	{
		this.context = context;
		this.builder = builder;
		this.module = module;
	}

	// ─── Registration ────────────────────────────────────────────────────────

	/**
	 * Register a heap-allocated class instance, with deep-drop dispatch.
	 *
	 * @param variableName   The Nebula variable name.
	 * @param variableAlloca The LLVM alloca that holds the heap pointer.
	 * @param typeName       The Nebula class name (used to look up
	 *                       {@code TypeName__drop} in the module).
	 */
	public void registerRegion(String variableName, LLVMValueRef variableAlloca, String typeName)
	{
		registerRegion(variableName, variableAlloca, RegionKind.HEAP_OBJECT, typeName);
	}

	/**
	 * Register a heap-backed region with an explicit kind but no type name.
	 * Used for STRING_PROXY regions and HEAP_OBJECT regions whose type is unknown.
	 */
	public void registerRegion(String variableName, LLVMValueRef variableAlloca, RegionKind kind)
	{
		registerRegion(variableName, variableAlloca, kind, null);
	}

	private void registerRegion(String variableName, LLVMValueRef variableAlloca,
								RegionKind kind, String typeName)
	{
		LLVMValueRef flag = LLVMBuildAlloca(builder,
			LLVMInt1TypeInContext(context),
			variableName + "_cvt_live");
		LLVMBuildStore(builder,
			LLVMConstInt(LLVMInt1TypeInContext(context), 1, 0),
			flag);

		regions.add(new TrackedRegion(variableName, variableAlloca, flag, kind, typeName));
	}

	// ─── Ownership transfer ──────────────────────────────────────────────────

	/**
	 * Mark a tracked region as "transferred" — ownership has moved elsewhere.
	 * Sets the live flag to {@code false} and records that the flag was toggled
	 * (so {@link #emitCleanup} knows to emit a conditional branch).
	 */
	public void markTransferred(String variableName)
	{
		for (TrackedRegion r : regions)
		{
			if (r.variableName.equals(variableName))
			{
				LLVMBuildStore(builder,
					LLVMConstInt(LLVMInt1TypeInContext(context), 0, 0),
					r.liveFlag);
				r.wasTransferred = true;
				return;
			}
		}
	}

	// ─── Queries ─────────────────────────────────────────────────────────────

	public boolean isTracked(String variableName)
	{
		for (TrackedRegion r : regions)
		{
			if (r.variableName.equals(variableName))
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasTrackedRegions()
	{
		return !regions.isEmpty();
	}

	// ─── Cleanup emission ────────────────────────────────────────────────────

	/**
	 * Emit cleanup code before a {@code ret} instruction.
	 * <p>
	 * Regions are freed in <b>LIFO order</b> (last declared, first freed),
	 * matching destructor semantics in C++/Rust.
	 * <p>
	 * <b>Zero-cost optimisation:</b> If a region was never conditionally
	 * transferred, the flag is guaranteed to be {@code true}, so we emit a
	 * direct free call without the branch/extra basic blocks.
	 */
	public void emitCleanup(LLVMValueRef currentFunction)
	{
		if (regions.isEmpty())
		{
			return;
		}

		LLVMValueRef nebFree = getOrDeclareNebFree();

		// LIFO: iterate in reverse declaration order
		for (int i = regions.size() - 1; i >= 0; i--)
		{
			TrackedRegion r = regions.get(i);

			if (r.wasTransferred)
			{
				emitConditionalFree(r, nebFree, currentFunction);
			}
			else
			{
				emitUnconditionalFree(r, nebFree);
			}
		}
	}

	// ─── Emission helpers ────────────────────────────────────────────────────

	private void emitConditionalFree(TrackedRegion r, LLVMValueRef nebFree,
									 LLVMValueRef currentFunction)
	{
		LLVMValueRef flagVal = LLVMBuildLoad2(builder,
			LLVMInt1TypeInContext(context),
			r.liveFlag,
			r.variableName + "_cvt_chk");

		LLVMBasicBlockRef freeBB = LLVMAppendBasicBlockInContext(context,
			currentFunction, "cvt_free_" + r.variableName);
		LLVMBasicBlockRef skipBB = LLVMAppendBasicBlockInContext(context,
			currentFunction, "cvt_skip_" + r.variableName);

		LLVMBuildCondBr(builder, flagVal, freeBB, skipBB);

		LLVMPositionBuilderAtEnd(builder, freeBB);
		emitFreeCall(r, nebFree);
		LLVMBuildBr(builder, skipBB);

		LLVMPositionBuilderAtEnd(builder, skipBB);
	}

	private void emitUnconditionalFree(TrackedRegion r, LLVMValueRef nebFree)
	{
		emitFreeCall(r, nebFree);
	}

	/**
	 * Emit the actual free call, extracting the heap pointer from the tracked
	 * alloca according to the region kind.
	 * <p>
	 * For {@link RegionKind#HEAP_OBJECT} with a known type name, dispatches to
	 * {@code TypeName__drop(ptr)} (which handles recursive field cleanup AND
	 * calls {@code neb_free(this)} internally). Falls back to plain
	 * {@code neb_free} when no drop function exists in the module.
	 */
	private void emitFreeCall(TrackedRegion r, LLVMValueRef nebFree)
	{
		switch (r.kind)
		{
			case HEAP_OBJECT ->
			{
				LLVMValueRef heapPtr = LLVMBuildLoad2(builder,
					LLVMPointerTypeInContext(context, 0),
					r.variableAlloca,
					r.variableName + "_cvt_ptr");

				// Prefer the compiler-generated deep drop function when available.
				if (r.typeName != null)
				{
					LLVMValueRef dropFn = LLVMGetNamedFunction(module, r.typeName + "__drop");
					if (dropFn != null && !dropFn.isNull())
					{
						LLVMValueRef[] dropArgs = { heapPtr };
						LLVMBuildCall2(builder,
							getDropFnType(),
							dropFn,
							new PointerPointer<>(dropArgs),
							1,
							"");
						return; // drop function already calls neb_free(this) internally
					}
				}

				// Fallback: plain shallow free.
				LLVMValueRef[] args = { heapPtr };
				LLVMBuildCall2(builder, getNebFreeType(), nebFree,
					new PointerPointer<>(args), 1, "");
			}

			case STRING_PROXY ->
			{
				LLVMTypeRef strType = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);
				LLVMValueRef strVal = LLVMBuildLoad2(builder, strType,
					r.variableAlloca, r.variableName + "_cvt_str");
				LLVMValueRef dataPtr = LLVMBuildExtractValue(builder, strVal, 0,
					r.variableName + "_cvt_data_ptr");
				LLVMValueRef[] args = { dataPtr };
				LLVMBuildCall2(builder, getNebFreeType(), nebFree,
					new PointerPointer<>(args), 1, "");
			}

			default -> throw new IllegalStateException("Unknown region kind: " + r.kind);
		}
	}

	// ─── Internal helpers ────────────────────────────────────────────────────

	private LLVMValueRef getOrDeclareNebFree()
	{
		LLVMValueRef fn = LLVMGetNamedFunction(module, "neb_free");
		if (fn == null || fn.isNull())
		{
			fn = LLVMAddFunction(module, "neb_free", getNebFreeType());
		}
		return fn;
	}

	/** {@code void neb_free(ptr)} */
	private LLVMTypeRef getNebFreeType()
	{
		LLVMTypeRef ptrType = LLVMPointerTypeInContext(context, 0);
		LLVMTypeRef voidType = LLVMVoidTypeInContext(context);
		return LLVMFunctionType(voidType, new PointerPointer<>(new LLVMTypeRef[]{ ptrType }), 1, 0);
	}

	/** {@code void TypeName__drop(ptr)} — same signature as neb_free */
	private LLVMTypeRef getDropFnType()
	{
		return getNebFreeType();
	}
}
