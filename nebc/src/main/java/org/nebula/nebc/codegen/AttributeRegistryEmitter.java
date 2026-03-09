package org.nebula.nebc.codegen;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMBuilderRef;
import org.bytedeco.llvm.LLVM.LLVMContextRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.nebula.nebc.semantic.SemanticAnalyzer;
import org.nebula.nebc.semantic.SymbolTable;
import org.nebula.nebc.semantic.symbol.MethodSymbol;
import org.nebula.nebc.semantic.symbol.NamespaceSymbol;
import org.nebula.nebc.semantic.symbol.Symbol;
import org.nebula.nebc.semantic.symbol.TypeSymbol;
import org.nebula.nebc.semantic.symbol.VariableSymbol;
import org.nebula.nebc.semantic.types.ClassType;
import org.nebula.nebc.semantic.types.CompositeType;
import org.nebula.nebc.semantic.types.EnumType;
import org.nebula.nebc.semantic.types.PrimitiveType;
import org.nebula.nebc.semantic.types.StructType;
import org.nebula.nebc.semantic.types.TraitType;
import org.nebula.nebc.semantic.types.Type;
import org.nebula.nebc.semantic.types.UnionType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.llvm.global.LLVM.*;

/**
 * Emits the compile-time attribute registry into the current LLVM module.
 *
 * <p>After all user declarations have been code-generated this pass walks the
 * semantic symbol table, collects every symbol that carries at least one
 * {@code #[...]} attribute, and emits:
 * <ul>
 *   <li>A private constant array {@code @__nebula_attr_registry} whose elements
 *       are {@code %__NebAttrEntry_t} structs (one per symbol–attribute pair).</li>
 *   <li>Seven externally-visible accessor functions that {@code std::reflect}
 *       calls via its {@code extern "C"} declarations.</li>
 * </ul>
 *
 * <h3>Entry layout ({@code %__NebAttrEntry_t})</h3>
 * <pre>
 *   { %str_t sym_name, i32 sym_kind, %str_t attr_path,
 *     i32 args_count, ptr args_ptr, ptr fn_ptr }
 * </pre>
 * One entry is emitted per (symbol, attribute) pair; a symbol with three
 * attributes produces three entries.
 *
 * <h3>Exported functions</h3>
 * <pre>
 *   i64  __nebula_rt_attr_entry_count()
 *   %str __nebula_rt_attr_symbol_name(i64 idx)
 *   i32  __nebula_rt_attr_symbol_kind(i64 idx)
 *   %str __nebula_rt_attr_path(i64 idx)
 *   i32  __nebula_rt_attr_args_count(i64 idx)
 *   %str __nebula_rt_attr_arg(i64 idx, i32 arg_i)
 *   ptr  __nebula_rt_attr_fn_ptr(i64 idx)
 * </pre>
 */
final class AttributeRegistryEmitter
{
    // ── Symbol-kind constants (must mirror std::reflect::SymbolKind enum) ───────

    static final int KIND_METHOD = 0;
    static final int KIND_CLASS  = 1;
    static final int KIND_STRUCT = 2;
    static final int KIND_ENUM   = 3;
    static final int KIND_TRAIT  = 4;
    static final int KIND_UNION  = 5;
    static final int KIND_CONST  = 6;

    // ── LLVM handles ─────────────────────────────────────────────────────────────

    private final LLVMContextRef context;
    private final LLVMModuleRef  module;
    private final LLVMBuilderRef builder;

    // ── Cached LLVM types ─────────────────────────────────────────────────────────

    private LLVMTypeRef strType;    // %str_t  = { ptr, i64 }
    private LLVMTypeRef entryType;  // %__NebAttrEntry_t
    private LLVMTypeRef i32Type;
    private LLVMTypeRef i64Type;
    private LLVMTypeRef ptrType;

    /** Counter for unique internal global names. */
    private int nameSeq = 0;

    // ── Constructor ───────────────────────────────────────────────────────────────

    AttributeRegistryEmitter(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder)
    {
        this.context = context;
        this.module  = module;
        this.builder = builder;
    }

    // ── Public entry point ────────────────────────────────────────────────────────

    /**
     * Walks {@code analyzer}'s global symbol table, collects every attributed
     * symbol, and emits the constant registry plus its accessor functions into
     * the LLVM module.
     */
    void emit(SemanticAnalyzer analyzer)
    {
        initTypes();

        List<AttrEntry> entries = new ArrayList<>();
        collectEntries(analyzer.getGlobalScope(), entries);

        int           n        = entries.size();
        LLVMValueRef  registry = buildRegistry(entries, n);
        emitAccessors(registry, n);
    }

    // ── Internal record ───────────────────────────────────────────────────────────

    /** One (symbol, attribute) pair to be stored in the registry. */
    private record AttrEntry(
            String       symbolQualifiedName,
            int          symbolKind,
            String       attrPath,
            List<String> attrArgs,
            /** Mangled LLVM function name for method symbols; null otherwise. */
            String       mangledName
    ) {}

    // ── Type initialisation ───────────────────────────────────────────────────────

    private void initTypes()
    {
        i32Type = LLVMInt32TypeInContext(context);
        i64Type = LLVMInt64TypeInContext(context);
        ptrType = LLVMPointerTypeInContext(context, 0);

        // %str_t = { ptr, i64 } — reuse the shared cached type
        strType = LLVMTypeMapper.getOrCreateStructType(context, PrimitiveType.STR);

        // %__NebAttrEntry_t = { %str_t, i32, %str_t, i32, ptr, ptr }
        // Use a unique name to avoid clashing with user-defined types.
        LLVMTypeRef existing = LLVMGetTypeByName2(context, "__NebAttrEntry_t");
        if (existing == null || existing.isNull())
        {
            entryType = LLVMStructCreateNamed(context, "__NebAttrEntry_t");
            LLVMTypeRef[] fields = { strType, i32Type, strType, i32Type, ptrType, ptrType };
            LLVMStructSetBody(entryType, new PointerPointer<>(fields), fields.length, 0);
        }
        else
        {
            entryType = existing;
        }
    }

    // ── Collection pass ───────────────────────────────────────────────────────────

    /**
     * Recursively visits all symbols in {@code scope} and appends one
     * {@link AttrEntry} per (symbol, attribute) pair to {@code out}.
     * Namespaces and composite types are recursed into.
     */
    private void collectEntries(SymbolTable scope, List<AttrEntry> out)
    {
        if (scope == null)
            return;

        for (Symbol sym : scope.getSymbols().values())
        {
            // Emit one entry per attribute on this symbol.
            if (!sym.getAttributes().isEmpty())
            {
                String qualName    = sym.getQualifiedName();
                int    kind        = kindOf(sym);
                String mangledName = (sym instanceof MethodSymbol ms) ? ms.getMangledName() : null;

                for (Symbol.AttributeInfo attr : sym.getAttributes())
                {
                    out.add(new AttrEntry(qualName, kind, attr.path(), attr.args(), mangledName));
                }
            }

            // Recurse into containers regardless of whether they have attributes.
            if (sym instanceof NamespaceSymbol ns)
            {
                collectEntries(ns.getMemberTable(), out);
            }
            else if (sym instanceof TypeSymbol ts && ts.getType() instanceof CompositeType ct)
            {
                collectEntries(ct.getMemberScope(), out);
            }
        }
    }

    /** Maps a symbol to its {@code SymbolKind} integer constant. */
    private int kindOf(Symbol sym)
    {
        if (sym instanceof MethodSymbol)
            return KIND_METHOD;

        if (sym instanceof TypeSymbol ts)
        {
            Type t = ts.getType();
            if (t instanceof ClassType)  return KIND_CLASS;
            if (t instanceof StructType) return KIND_STRUCT;
            if (t instanceof EnumType)   return KIND_ENUM;
            if (t instanceof TraitType)  return KIND_TRAIT;
            if (t instanceof UnionType)  return KIND_UNION;
        }

        return KIND_CONST;
    }

    // ── Registry construction ─────────────────────────────────────────────────────

    private LLVMValueRef buildRegistry(List<AttrEntry> entries, int n)
    {
        // Always allocate at least 1 slot so LLVMArrayType(type, 0) is avoided.
        int slots = Math.max(n, 1);

        LLVMValueRef[] entryConsts = new LLVMValueRef[slots];
        for (int i = 0; i < n; i++)
            entryConsts[i] = buildEntryConst(entries.get(i), i);

        if (n == 0)
            entryConsts[0] = buildZeroEntry();

        LLVMTypeRef  arrayType  = LLVMArrayType(entryType, slots);
        LLVMValueRef arrayConst = LLVMConstArray(entryType,
                new PointerPointer<>(entryConsts), slots);

        LLVMValueRef global = LLVMAddGlobal(module, arrayType, "__nebula_attr_registry");
        LLVMSetInitializer(global, arrayConst);
        LLVMSetGlobalConstant(global, 1);
        LLVMSetLinkage(global, LLVMInternalLinkage);
        return global;
    }

    private LLVMValueRef buildEntryConst(AttrEntry e, int idx)
    {
        LLVMValueRef symNameStr = buildFatStrConst(e.symbolQualifiedName(), ".ra.sn" + idx);
        LLVMValueRef symKind    = LLVMConstInt(i32Type, e.symbolKind(), 0);
        LLVMValueRef attrStr    = buildFatStrConst(e.attrPath(), ".ra.ap" + idx);
        LLVMValueRef argCount   = LLVMConstInt(i32Type, e.attrArgs().size(), 0);
        LLVMValueRef argsPtr    = buildArgsConst(e.attrArgs(), idx);
        LLVMValueRef fnPtr      = buildFnPtrConst(e.mangledName());

        LLVMValueRef[] fields = { symNameStr, symKind, attrStr, argCount, argsPtr, fnPtr };
        return LLVMConstNamedStruct(entryType, new PointerPointer<>(fields), fields.length);
    }

    /** Sentinel zero entry used when the registry is empty. */
    private LLVMValueRef buildZeroEntry()
    {
        LLVMValueRef emptyStr = buildFatStrConst("", ".ra.empty" + (nameSeq++));
        LLVMValueRef zero32   = LLVMConstInt(i32Type, 0, 0);
        LLVMValueRef nullPtr  = LLVMConstPointerNull(ptrType);
        LLVMValueRef[] fields = { emptyStr, zero32, emptyStr, zero32, nullPtr, nullPtr };
        return LLVMConstNamedStruct(entryType, new PointerPointer<>(fields), fields.length);
    }

    // ── String constant helpers ───────────────────────────────────────────────────

    /**
     * Emits:
     * <pre>
     *   @name = private constant [N+1 x i8] c"...\00"
     * </pre>
     * and returns a constant {@code %str_t} struct {@code { ptr @name, i64 N }}.
     */
    private LLVMValueRef buildFatStrConst(String text, String globalName)
    {
        byte[]       bytes    = text.getBytes(StandardCharsets.UTF_8);
        long         byteLen  = bytes.length;

        // LLVMConstStringInContext with DontNullTerminate=0 appends a '\0'.
        LLVMValueRef strInit     = LLVMConstStringInContext(context,
                new BytePointer(text), (int) byteLen, /* DontNullTerminate= */ 0);
        LLVMTypeRef  charArrType = LLVMTypeOf(strInit);

        String       uniqueName  = globalName + "." + (nameSeq++);
        LLVMValueRef charGlobal  = LLVMAddGlobal(module, charArrType, uniqueName);
        LLVMSetInitializer(charGlobal, strInit);
        LLVMSetGlobalConstant(charGlobal, 1);
        LLVMSetLinkage(charGlobal, LLVMPrivateLinkage);

        // { ptr @charGlobal, i64 byteLen }
        LLVMValueRef[] strFields = { charGlobal, LLVMConstInt(i64Type, byteLen, 0) };
        return LLVMConstNamedStruct(strType, new PointerPointer<>(strFields), 2);
    }

    /**
     * Builds a {@code [N x %str_t]} global constant for the arg list.
     * Returns a {@code ptr} to it, or a null pointer if there are no args.
     */
    private LLVMValueRef buildArgsConst(List<String> args, int entryIdx)
    {
        if (args.isEmpty())
            return LLVMConstPointerNull(ptrType);

        LLVMValueRef[] argStrs = new LLVMValueRef[args.size()];
        for (int i = 0; i < args.size(); i++)
            argStrs[i] = buildFatStrConst(args.get(i), ".ra.ag" + entryIdx + "." + i);

        LLVMTypeRef  argsArrayType  = LLVMArrayType(strType, args.size());
        LLVMValueRef argsArrayConst = LLVMConstArray(strType,
                new PointerPointer<>(argStrs), args.size());

        String       name       = ".ra.args" + entryIdx + "." + (nameSeq++);
        LLVMValueRef argsGlobal = LLVMAddGlobal(module, argsArrayType, name);
        LLVMSetInitializer(argsGlobal, argsArrayConst);
        LLVMSetGlobalConstant(argsGlobal, 1);
        LLVMSetLinkage(argsGlobal, LLVMPrivateLinkage);
        return argsGlobal;
    }

    /**
     * Returns a constant {@code ptr} to the LLVM function identified by
     * {@code mangledName}, or a null pointer if the function is not (yet)
     * present in the module (e.g. imported symbols, generic templates).
     */
    private LLVMValueRef buildFnPtrConst(String mangledName)
    {
        if (mangledName == null)
            return LLVMConstPointerNull(ptrType);

        LLVMValueRef fn = LLVMGetNamedFunction(module, mangledName);
        return (fn != null && !fn.isNull()) ? fn : LLVMConstPointerNull(ptrType);
    }

    // ── Accessor function emission ────────────────────────────────────────────────

    private void emitAccessors(LLVMValueRef registry, int n)
    {
        emitEntryCount(n);
        emitStrFieldAccessor ("__nebula_rt_attr_symbol_name", registry, 0, n);
        emitI32FieldAccessor ("__nebula_rt_attr_symbol_kind", registry, 1, n);
        emitStrFieldAccessor ("__nebula_rt_attr_path",        registry, 2, n);
        emitI32FieldAccessor ("__nebula_rt_attr_args_count",  registry, 3, n);
        emitArgAccessor      (registry, n);
        emitPtrFieldAccessor ("__nebula_rt_attr_fn_ptr",      registry, 5, n);
    }

    /** {@code i64 __nebula_rt_attr_entry_count() { ret i64 N }} */
    private void emitEntryCount(int n)
    {
        LLVMTypeRef  fnType = LLVMFunctionType(i64Type, new LLVMTypeRef(), 0, 0);
        LLVMValueRef fn     = getOrAddFunction("__nebula_rt_attr_entry_count", fnType);
        if (LLVMCountBasicBlocks(fn) > 0)
            return;

        LLVMSetLinkage(fn, LLVMExternalLinkage);
        LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(context, fn, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);
        LLVMBuildRet(builder, LLVMConstInt(i64Type, n, 0));
    }

    /**
     * Emits a function that loads a {@code %str_t} field at {@code fieldIdx}
     * from the registry entry selected by the {@code i64} parameter.
     *
     * <pre>
     *   %str_t name(i64 idx) {
     *       %p = gep [N x %Entry], @registry, 0, idx, fieldIdx
     *       %v = load %str_t, ptr %p
     *       ret %str_t %v
     *   }
     * </pre>
     */
    private void emitStrFieldAccessor(String name, LLVMValueRef registry, int fieldIdx, int n)
    {
        LLVMTypeRef  fnType = LLVMFunctionType(strType,
                new PointerPointer<>(new LLVMTypeRef[]{ i64Type }), 1, 0);
        LLVMValueRef fn     = getOrAddFunction(name, fnType);
        if (LLVMCountBasicBlocks(fn) > 0)
            return;

        LLVMSetLinkage(fn, LLVMExternalLinkage);
        LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(context, fn, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);

        if (n == 0)
        {
            LLVMBuildRet(builder, buildFatStrConst("", ".ra.empty.ret." + (nameSeq++)));
            return;
        }

        LLVMValueRef[] indices = {
            LLVMConstInt(i64Type, 0, 0),
            LLVMGetParam(fn, 0),
            LLVMConstInt(i32Type, fieldIdx, 0)
        };
        LLVMTypeRef  arrType = LLVMArrayType(entryType, n);
        LLVMValueRef gep     = LLVMBuildGEP2(builder, arrType, registry,
                new PointerPointer<>(indices), 3, "fld");
        LLVMBuildRet(builder, LLVMBuildLoad2(builder, strType, gep, "val"));
    }

    /** Like {@link #emitStrFieldAccessor} but for {@code i32} fields. */
    private void emitI32FieldAccessor(String name, LLVMValueRef registry, int fieldIdx, int n)
    {
        LLVMTypeRef  fnType = LLVMFunctionType(i32Type,
                new PointerPointer<>(new LLVMTypeRef[]{ i64Type }), 1, 0);
        LLVMValueRef fn     = getOrAddFunction(name, fnType);
        if (LLVMCountBasicBlocks(fn) > 0)
            return;

        LLVMSetLinkage(fn, LLVMExternalLinkage);
        LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(context, fn, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);

        if (n == 0)
        {
            LLVMBuildRet(builder, LLVMConstInt(i32Type, 0, 0));
            return;
        }

        LLVMValueRef[] indices = {
            LLVMConstInt(i64Type, 0, 0),
            LLVMGetParam(fn, 0),
            LLVMConstInt(i32Type, fieldIdx, 0)
        };
        LLVMTypeRef  arrType = LLVMArrayType(entryType, n);
        LLVMValueRef gep     = LLVMBuildGEP2(builder, arrType, registry,
                new PointerPointer<>(indices), 3, "fld");
        LLVMBuildRet(builder, LLVMBuildLoad2(builder, i32Type, gep, "val"));
    }

    /** Like {@link #emitStrFieldAccessor} but for {@code ptr} fields. */
    private void emitPtrFieldAccessor(String name, LLVMValueRef registry, int fieldIdx, int n)
    {
        LLVMTypeRef  fnType = LLVMFunctionType(ptrType,
                new PointerPointer<>(new LLVMTypeRef[]{ i64Type }), 1, 0);
        LLVMValueRef fn     = getOrAddFunction(name, fnType);
        if (LLVMCountBasicBlocks(fn) > 0)
            return;

        LLVMSetLinkage(fn, LLVMExternalLinkage);
        LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(context, fn, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);

        if (n == 0)
        {
            LLVMBuildRet(builder, LLVMConstPointerNull(ptrType));
            return;
        }

        LLVMValueRef[] indices = {
            LLVMConstInt(i64Type, 0, 0),
            LLVMGetParam(fn, 0),
            LLVMConstInt(i32Type, fieldIdx, 0)
        };
        LLVMTypeRef  arrType = LLVMArrayType(entryType, n);
        LLVMValueRef gep     = LLVMBuildGEP2(builder, arrType, registry,
                new PointerPointer<>(indices), 3, "fld");
        LLVMBuildRet(builder, LLVMBuildLoad2(builder, ptrType, gep, "val"));
    }

    /**
     * Emits {@code %str_t __nebula_rt_attr_arg(i64 idx, i32 arg_i)}.
     *
     * <ol>
     *   <li>Loads {@code args_ptr} (field 4) from {@code registry[idx]}.</li>
     *   <li>Indexes into the resulting {@code [? x %str_t]} array at
     *       {@code arg_i}.</li>
     *   <li>Loads and returns the {@code %str_t}.</li>
     * </ol>
     */
    private void emitArgAccessor(LLVMValueRef registry, int n)
    {
        LLVMTypeRef[] params = { i64Type, i32Type };
        LLVMTypeRef   fnType = LLVMFunctionType(strType,
                new PointerPointer<>(params), 2, 0);
        LLVMValueRef fn = getOrAddFunction("__nebula_rt_attr_arg", fnType);
        if (LLVMCountBasicBlocks(fn) > 0)
            return;

        LLVMSetLinkage(fn, LLVMExternalLinkage);
        LLVMBasicBlockRef entry = LLVMAppendBasicBlockInContext(context, fn, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);

        if (n == 0)
        {
            LLVMBuildRet(builder, buildFatStrConst("", ".ra.empty.arg." + (nameSeq++)));
            return;
        }

        LLVMValueRef entryIdx = LLVMGetParam(fn, 0);
        LLVMValueRef argIdx   = LLVMGetParam(fn, 1);

        // Load args_ptr (field index 4) from registry[entryIdx].
        LLVMTypeRef  arrType     = LLVMArrayType(entryType, n);
        LLVMValueRef[] ptrIndices = {
            LLVMConstInt(i64Type, 0, 0),
            entryIdx,
            LLVMConstInt(i32Type, 4, 0)
        };
        LLVMValueRef argsPtrGep = LLVMBuildGEP2(builder, arrType, registry,
                new PointerPointer<>(ptrIndices), 3, "args_ptr_loc");
        LLVMValueRef argsPtr    = LLVMBuildLoad2(builder, ptrType, argsPtrGep, "args_ptr");

        // Index into [? x %str_t] at arg_i (widen i32 → i64 for GEP).
        LLVMValueRef argIdxW = LLVMBuildSExt(builder, argIdx, i64Type, "arg_i64");
        LLVMValueRef[] argIndices = { argIdxW };
        LLVMValueRef argGep  = LLVMBuildGEP2(builder, strType, argsPtr,
                new PointerPointer<>(argIndices), 1, "arg_ptr");
        LLVMBuildRet(builder, LLVMBuildLoad2(builder, strType, argGep, "arg_val"));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────────

    /** Retrieves or declares a function in the module by name. */
    private LLVMValueRef getOrAddFunction(String name, LLVMTypeRef fnType)
    {
        LLVMValueRef existing = LLVMGetNamedFunction(module, name);
        if (existing != null && !existing.isNull())
            return existing;
        return LLVMAddFunction(module, name, fnType);
    }
}
