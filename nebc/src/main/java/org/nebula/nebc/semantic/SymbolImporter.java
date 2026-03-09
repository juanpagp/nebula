package org.nebula.nebc.semantic;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.nebula.nebc.ast.CVTModifier;
import org.nebula.nebc.semantic.symbol.*;
import org.nebula.nebc.semantic.types.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;

/**
 * Imports symbols from a {@code .nebsym} JSON file into a {@link SymbolTable}.
 *
 * <p>In addition to the plain symbol table, this importer handles two extra
 * sections:
 * <ul>
 *   <li>{@code "primitive_impls"} – trait-impl scopes for primitive types
 *       (e.g. {@code impl Stringable for str}). These are populated directly
 *       into the provided {@code primitiveImplScopes} map.</li>
 *   <li>{@code "erased_bitcode"} on generic methods – Base64-encoded LLVM
 *       bitcode bytes emitted by the library compiler.  The bytes are decoded
 *       and stored on the {@link MethodSymbol} so the code generator can call
 *       the type-erased function at link time — no source required, zero runtime
 *       cost after LLVM inlining.</li>
 * </ul>
 */
public class SymbolImporter
{
    private final Gson gson = new Gson();

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Imports symbols from the given {@code .nebsym} file.
     *
     * @param path                Path to the {@code .nebsym} file.
     * @param targetTable         Global symbol table to populate.
     * @param primitiveImplScopes Mutable map to populate with primitive-type trait-impl
     *                            scopes (keyed by {@link PrimitiveType}).
     *                            May be {@code null} if the caller does not need them.
     */
    public void importSymbols(
            String path,
            SymbolTable targetTable,
            java.util.Map<Type, SymbolTable> primitiveImplScopes) throws IOException
    {
        try (FileReader reader = new FileReader(path))
        {
            JsonObject root    = gson.fromJson(reader, JsonObject.class);
            JsonArray  symbols = root.getAsJsonArray("symbols");
            importTable(symbols, targetTable, targetTable);

            if (primitiveImplScopes != null && root.has("primitive_impls"))
            {
                importPrimitiveImpls(root.getAsJsonArray("primitive_impls"),
                        primitiveImplScopes, targetTable);
            }
        }
    }

    /**
     * Backwards-compatible overload that ignores primitive impls.
     */
    public void importSymbols(String path, SymbolTable targetTable) throws IOException
    {
        importSymbols(path, targetTable, null);
    }

    // ── Symbol-table import ─────────────────────────────────────────────────────

    /**
     * Two-pass import:
     * <ol>
     *   <li>Pass 1 – {@link #importTypeSymbols}: recursively registers all namespace
     *       containers, classes, structs, traits and enums so every type name is
     *       resolvable before any method signature is parsed.</li>
     *   <li>Pass 2 – {@link #importMethodSymbols}: recursively imports all methods and
     *       variables, whose type-parameter bounds are now fully resolvable.</li>
     * </ol>
     */
    private void importTable(
            JsonArray symbols,
            SymbolTable table,
            SymbolTable globalScope)
    {
        if (symbols == null)
            return;

        importTypeSymbols(symbols, table, globalScope);
        importMethodSymbols(symbols, table, globalScope);
    }

    /** Pass 1 – registers namespaces, classes, structs, traits and enums. */
    private void importTypeSymbols(
            JsonArray symbols,
            SymbolTable table,
            SymbolTable globalScope)
    {
        if (symbols == null)
            return;

        for (JsonElement el : symbols)
        {
            JsonObject obj = el.getAsJsonObject();
            if (obj.get("kind") == null)
                continue;

            switch (obj.get("kind").getAsString())
            {
                case "namespace":
                {
                    String          nsName = obj.get("name").getAsString();
                    NamespaceSymbol nsSym  = (NamespaceSymbol) table.resolveLocal(nsName);
                    if (nsSym == null)
                    {
                        NamespaceType nsType = new NamespaceType(nsName, table);
                        nsSym = new NamespaceSymbol(nsName, nsType, null);
                        table.define(nsSym);
                    }
                    // Ensure the owner is set so getMangledName() traverses the
                    // full namespace chain (e.g. std__reflect__total).
                    nsSym.getMemberTable().setOwner(nsSym);
                    importTypeSymbols(obj.getAsJsonArray("symbols"),
                            nsSym.getMemberTable(), globalScope);
                    break;
                }
                case "class":
                case "struct":
                case "trait":
                    table.define(importCompositeType(obj, table, globalScope, obj.get("kind").getAsString()));
                    break;
                case "enum":
                    table.define(importEnumType(obj, table));
                    break;
                case "tag":
                {
                    // Register a stub TagType so that bound references like
                    // 'T: Printable' can be resolved during method import (pass 2).
                    // Members are populated in pass 2 once all trait types are available.
                    String  tagName = obj.get("name").getAsString();
                    TagType tagType = new TagType(tagName, globalScope);
                    table.define(new TypeSymbol(tagName, tagType, null));
                    break;
                }
                default:
                    break;
            }
        }
    }

    /** Pass 2 – imports methods and variables, with all type names already registered. */
    private void importMethodSymbols(
            JsonArray symbols,
            SymbolTable table,
            SymbolTable globalScope)
    {
        if (symbols == null)
            return;

        for (JsonElement el : symbols)
        {
            JsonObject obj = el.getAsJsonObject();
            if (obj.get("kind") == null)
                continue;

            switch (obj.get("kind").getAsString())
            {
                case "namespace":
                {
                    String          nsName = obj.get("name").getAsString();
                    NamespaceSymbol nsSym  = (NamespaceSymbol) table.resolveLocal(nsName);
                    if (nsSym != null)
                    {
                        // Ensure the owner is set so getMangledName() traverses the
                        // full namespace chain (e.g. std__reflect__total).
                        nsSym.getMemberTable().setOwner(nsSym);
                        importMethodSymbols(obj.getAsJsonArray("symbols"),
                                nsSym.getMemberTable(), globalScope);
                    }
                    break;
                }
                case "method":
                {
                    MethodSymbol ms = importMethod(obj, table, globalScope);
                    table.define(ms);
                    break;
                }
                case "variable":
                    table.define(importVariable(obj, table));
                    break;
                case "tag":
                {
                    // Find the stub TagType registered in pass 1 and populate its members.
                    String     tagName = obj.get("name").getAsString();
                    TypeSymbol tagSym  = table.resolveType(tagName);
                    if (tagSym != null && tagSym.getType() instanceof TagType tagType
                            && tagType.getMemberTypes().isEmpty()
                            && obj.has("members"))
                    {
                        for (JsonElement mEl : obj.getAsJsonArray("members"))
                        {
                            Type memberType = resolveTagMember(mEl.getAsString(), globalScope);
                            if (memberType != null)
                                tagType.addMember(memberType);
                        }
                        tagType.buildMemberScope();
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private MethodSymbol importMethod(
            JsonObject obj,
            SymbolTable table,
            SymbolTable globalScope)
    {
        String  name     = obj.get("name").getAsString();
        boolean isExtern = obj.get("is_extern").getAsBoolean();

        java.util.List<TypeParameterType> typeParams   = new ArrayList<>();
        SymbolTable                       resolveTable = table;

        if (obj.has("type_parameters"))
        {
            resolveTable = new SymbolTable(table);
            JsonArray tps = obj.getAsJsonArray("type_parameters");
            for (JsonElement tpEl : tps)
            {
                JsonObject tpObj  = tpEl.getAsJsonObject();
                String     tpName = tpObj.get("name").getAsString();
                CompositeType  bound  = null;
                if (tpObj.has("bound"))
                {
                    // Use a deep search from globalScope so that traits/tags defined
                    // in sibling namespaces (e.g. std::traits::Stringable) are
                    // found even when we are currently importing std::io.
                    bound = resolveBoundDeep(tpObj.get("bound").getAsString(), globalScope);
                }
                TypeParameterType tpt = new TypeParameterType(tpName, bound);
                typeParams.add(tpt);
                resolveTable.define(new TypeSymbol(tpName, tpt, null));
            }
        }

        Type returnType = resolveType(obj.get("return_type").getAsString(), resolveTable, globalScope);

        java.util.List<Type>          paramTypes = new ArrayList<>();
        java.util.List<ParameterInfo> paramInfos = new ArrayList<>();
        JsonArray                     params     = obj.getAsJsonArray("parameters");
        for (JsonElement pEl : params)
        {
            JsonObject pObj  = pEl.getAsJsonObject();
            Type       pType = resolveType(pObj.get("type").getAsString(), resolveTable, globalScope);
            paramTypes.add(pType);

            String      pName = pObj.get("name").getAsString();
            CVTModifier hint  = null;
            if (pObj.has("cvt_hint"))
                hint = CVTModifier.fromString(pObj.get("cvt_hint").getAsString());
            paramInfos.add(new ParameterInfo(hint, pType, pName));
        }

        FunctionType type = new FunctionType(returnType, paramTypes, paramInfos);
        MethodSymbol ms   = new MethodSymbol(name, type, Collections.emptyList(),
                isExtern, null, typeParams);

        if (obj.has("trait_name"))
            ms.setTraitName(obj.get("trait_name").getAsString());

        // Deserialise pre-compiled erased LLVM bitcode, if present.
        // The code generator will link this into the consumer module and let
        // LLVM's inliner devirtualise all vtable calls — zero runtime cost.
        if (obj.has("erased_bitcode") && !typeParams.isEmpty())
        {
            try
            {
                byte[] bitcode = Base64.getDecoder().decode(
                        obj.get("erased_bitcode").getAsString());
                ms.setGenericBitcode(bitcode);
            }
            catch (IllegalArgumentException e)
            {
                org.nebula.nebc.util.Log.warn(
                        "Skipping malformed erased_bitcode for method '" + name + "': " + e.getMessage());
            }
        }

        if (obj.has("attributes"))
            ms.setAttributes(importAttributes(obj.getAsJsonArray("attributes")));

        // Preserve the exact mangled name from the exporting compiler so that
        // getMangledName() returns the correct fully-qualified symbol regardless
        // of how this method's definedIn scope is reconstructed during import.
        if (obj.has("mangled_name"))
        {
            ms.setOverriddenMangledName(obj.get("mangled_name").getAsString());
        }

        return ms;
    }

    private TypeSymbol importCompositeType(
            JsonObject obj,
            SymbolTable table,
            SymbolTable globalScope,
            String kind)
    {
        String        name = obj.get("name").getAsString();
        CompositeType type;
        if (kind.equals("class"))
            type = new ClassType(name, table);
        else if (kind.equals("struct"))
            type = new StructType(name, table);
        else
            type = new TraitType(name, table);

        TypeSymbol sym = new TypeSymbol(name, type, null);
        type.getMemberScope().setOwner(sym);

        // Pre-register the TypeSymbol in the outer table BEFORE importing member
        // methods.  Without this, methods whose 'this' parameter references the
        // composite type itself (e.g. FnRef.isValid(this: FnRef)) would fail to
        // resolve the type and produce Type.ANY — breaking receiver-type matching.
        table.define(sym);

        if (obj.has("members"))
        {
            if (type instanceof TraitType traitType)
            {
                // For traits, methods must be registered via addRequiredMethod so
                // that getVtableMethodNames() / getVtableSlotIndex() return the
                // correct stable ordering.
                JsonArray membersArr = obj.getAsJsonArray("members");
                // Pass 1: nested type symbols within the trait member scope
                importTypeSymbols(membersArr, type.getMemberScope(), globalScope);
                // Pass 2: import each method and register it as a required method
                for (JsonElement mEl : membersArr)
                {
                    JsonObject mObj = mEl.getAsJsonObject();
                    if (mObj.get("kind") != null
                            && "method".equals(mObj.get("kind").getAsString()))
                    {
                        MethodSymbol ms = importMethod(mObj, type.getMemberScope(), globalScope);
                        traitType.addRequiredMethod(ms);
                    }
                }
            }
            else
            {
                importTable(obj.getAsJsonArray("members"),
                        type.getMemberScope(), globalScope);
            }
        }

        if (obj.has("attributes"))
            sym.setAttributes(importAttributes(obj.getAsJsonArray("attributes")));

        return sym;
    }

    private TypeSymbol importEnumType(JsonObject obj, SymbolTable table)
    {
        String   name = obj.get("name").getAsString();
        EnumType type = new EnumType(name, table);
        TypeSymbol sym = new TypeSymbol(name, type, null);
        type.getMemberScope().setOwner(sym);

        if (obj.has("variants"))
        {
            JsonArray variants = obj.getAsJsonArray("variants");
            for (JsonElement v : variants)
            {
                String vName = v.getAsString();
                type.getMemberScope().define(new VariableSymbol(vName, type, false, null));
            }
        }

        if (obj.has("attributes"))
            sym.setAttributes(importAttributes(obj.getAsJsonArray("attributes")));

        return sym;
    }

    private VariableSymbol importVariable(JsonObject obj, SymbolTable table)
    {
        String         name = obj.get("name").getAsString();
        Type           type = resolveType(obj.get("type").getAsString(), table);
        boolean        mut  = obj.get("mutable").getAsBoolean();
        VariableSymbol vs   = new VariableSymbol(name, type, mut, null);
        if (obj.has("attributes"))
            vs.setAttributes(importAttributes(obj.getAsJsonArray("attributes")));
        return vs;
    }

    // ── Attribute import ────────────────────────────────────────────────────────

    /**
     * Deserialises a {@code "attributes"} JSON array into a list of
     * {@link Symbol.AttributeInfo} records.
     */
    private java.util.List<Symbol.AttributeInfo> importAttributes(JsonArray arr)
    {
        java.util.List<Symbol.AttributeInfo> result = new ArrayList<>();
        for (JsonElement el : arr)
        {
            JsonObject   ao   = el.getAsJsonObject();
            String       path = ao.get("path").getAsString();
            java.util.List<String> args = new ArrayList<>();
            if (ao.has("args"))
            {
                for (JsonElement argEl : ao.getAsJsonArray("args"))
                    args.add(argEl.getAsString());
            }
            result.add(new Symbol.AttributeInfo(path, args));
        }
        return result;
    }

    // ── Primitive impl import ───────────────────────────────────────────────────

    private void importPrimitiveImpls(
            JsonArray array,
            java.util.Map<Type, SymbolTable> primitiveImplScopes,
            SymbolTable globalScope)
    {
        for (JsonElement el : array)
        {
            JsonObject obj      = el.getAsJsonObject();
            String     typeName = obj.get("type").getAsString();

            // Resolve the primitive type
            Type primType = PrimitiveType.getByName(typeName);
            if (primType == null)
                continue;

            // Create (or reuse) the impl scope for this primitive type
            SymbolTable scope = primitiveImplScopes.computeIfAbsent(primType, t ->
            {
                SymbolTable st = new SymbolTable(globalScope);
                st.setOwner(new TypeSymbol(typeName, primType, null));
                return st;
            });

            // Import each method into that scope
            if (obj.has("methods"))
            {
                for (JsonElement mEl : obj.getAsJsonArray("methods"))
                {
                    JsonObject   mObj = mEl.getAsJsonObject();
                    MethodSymbol ms   = importMethod(mObj, globalScope, globalScope);
                    scope.define(ms);
                }
            }
        }
    }

    // ── Type resolution helpers ─────────────────────────────────────────────────

    /**
     * Resolves a tag member type by name.
     * Tries primitive types first (e.g. {@code str}, {@code i32}), then falls back
     * to a deep composite-type search for traits, classes and other tags.
     */
    private Type resolveTagMember(String name, SymbolTable globalScope)
    {
        Type prim = PrimitiveType.getByName(name);
        if (prim != null)
            return prim;
        return resolveBoundDeep(name, globalScope);
    }

    /**
     * Recursively searches {@code scope} and all descendant namespace scopes for a
     * {@link CompositeType} ({@link TraitType} or {@link TagType}) with the given
     * simple name. Used when importing method type-parameter bounds from .nebsym files.
     */
    private CompositeType resolveBoundDeep(String name, SymbolTable scope)
    {
        TypeSymbol sym = scope.resolveType(name);
        if (sym != null && sym.getType() instanceof CompositeType ct)
            return ct;

        for (Symbol child : scope.getSymbols().values())
        {
            if (child instanceof NamespaceSymbol ns)
            {
                CompositeType found = resolveBoundDeep(name, ns.getMemberTable());
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Recursively searches {@code scope} and all descendant namespace scopes for a
     * {@link TraitType} with the given simple name.
     *
     * <p>This is needed because a trait bound like {@code "Stringable"} may be defined
     * in a sibling namespace (e.g. {@code std::traits::Stringable}) that would not be
     * reachable via the normal parent-chain lookup from the referencing namespace.</p>
     */
    private TraitType resolveTraitDeep(String name, SymbolTable scope)
    {
        // Direct lookup in this scope's chain
        TypeSymbol sym = scope.resolveType(name);
        if (sym != null && sym.getType() instanceof TraitType tt)
            return tt;

        // Recurse into namespace member scopes
        for (Symbol child : scope.getSymbols().values())
        {
            if (child instanceof NamespaceSymbol ns)
            {
                TraitType found = resolveTraitDeep(name, ns.getMemberTable());
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private Type resolveType(String name, SymbolTable table)
    {
        return resolveType(name, table, null);
    }

    /**
     * Resolves a type name first from the local {@code table} (and its parent
     * chain), then falls back to a deep recursive search from {@code fallback}
     * when the local lookup yields nothing.
     *
     * <p>The deep fallback is required for cross-namespace type references: e.g.
     * {@code std::reflect::entry_fn} returns {@code FnRef} which is defined in
     * {@code std::fn}, a sibling namespace not reachable via the parent chain of
     * the reflect member table.</p>
     */
    private Type resolveType(String name, SymbolTable table, SymbolTable fallback)
    {
        Type t = PrimitiveType.getByName(name);
        if (t != null)
            return t;

        TypeSymbol sym = table.resolveType(name);
        if (sym != null)
            return sym.getType();

        // Deep search across all namespace scopes from the fallback root.
        if (fallback != null)
        {
            TypeSymbol deep = resolveTypeDeep(name, fallback);
            if (deep != null)
                return deep.getType();
        }

        return Type.ANY;
    }

    /**
     * Recursively searches {@code scope} and all descendant {@link NamespaceSymbol}
     * member scopes for a {@link TypeSymbol} with the given simple name.
     *
     * <p>Used to resolve cross-namespace type references during symbol import
     * (e.g. a method in {@code std::reflect} that returns a type defined in
     * {@code std::fn}).</p>
     */
    private TypeSymbol resolveTypeDeep(String name, SymbolTable scope)
    {
        TypeSymbol sym = scope.resolveType(name);
        if (sym != null)
            return sym;

        for (Symbol child : scope.getSymbols().values())
        {
            if (child instanceof NamespaceSymbol ns)
            {
                TypeSymbol found = resolveTypeDeep(name, ns.getMemberTable());
                if (found != null)
                    return found;
            }
        }
        return null;
    }
}
