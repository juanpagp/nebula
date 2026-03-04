package org.nebula.nebc.semantic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.nebula.nebc.semantic.symbol.*;
import org.nebula.nebc.semantic.types.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

public class SymbolExporter
{
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Exports global-scope symbols and primitive-trait-impl scopes into a
     * {@code .nebsym} JSON file.
     *
     * <p>The {@code primitiveImplScopes} map is optional (may be null or empty).
     * Generic methods whose erased LLVM bitcode has been pre-computed (by the
     * compilation pipeline via
     * {@link org.nebula.nebc.codegen.LLVMCodeGenerator#emitErasedFunctionBitcode})
     * are stored with an {@code "erased_bitcode"} field containing the bitcode
     * bytes encoded as Base64.  Consumers link this bitcode at compile-time and
     * let LLVM's inliner eliminate all vtable indirection — zero runtime cost.</p>
     */
    public void export(
            SymbolTable table,
            Map<Type, SymbolTable> primitiveImplScopes,
            String projectName,
            String outputPath) throws IOException
    {
        JsonObject root = new JsonObject();
        root.addProperty("name", projectName);
        root.add("symbols", exportTable(table));

        if (primitiveImplScopes != null && !primitiveImplScopes.isEmpty())
        {
            root.add("primitive_impls", exportPrimitiveImpls(primitiveImplScopes));
        }

        try (FileWriter writer = new FileWriter(outputPath))
        {
            gson.toJson(root, writer);
        }
    }

    /** Backwards-compatible overload without primitive-impl scopes. */
    public void export(SymbolTable table, String projectName, String outputPath) throws IOException
    {
        export(table, null, projectName, outputPath);
    }

    // ── Symbol table export ─────────────────────────────────────────────────────

    private JsonArray exportTable(SymbolTable table)
    {
        JsonArray array = new JsonArray();
        for (Map.Entry<String, Symbol> entry : table.getSymbols().entrySet())
        {
            Symbol sym = entry.getValue();
            if (sym instanceof NamespaceSymbol ns)
            {
                JsonObject obj = new JsonObject();
                obj.addProperty("kind", "namespace");
                obj.addProperty("name", ns.getName());
                obj.add("symbols", exportTable(ns.getMemberTable()));
                array.add(obj);
            }
            else if (sym instanceof MethodSymbol ms)
            {
                array.add(exportMethod(ms));
            }
            else if (sym instanceof TypeSymbol ts)
            {
                // Skip primitive types as they are built-in
                if (!(ts.getType() instanceof PrimitiveType))
                {
                    array.add(exportType(ts));
                }
            }
            else if (sym instanceof VariableSymbol vs)
            {
                // Don't export local variables, only global ones if they exist
                if (table.getParent() == null || table.getOwner() instanceof NamespaceSymbol)
                {
                    array.add(exportVariable(vs));
                }
            }
        }
        return array;
    }

    private JsonObject exportMethod(MethodSymbol ms)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("kind", "method");
        obj.addProperty("name", ms.getName());
        obj.addProperty("is_extern", ms.isExtern());

        if (ms.getTraitName() != null)
        {
            obj.addProperty("trait_name", ms.getTraitName());
        }

        FunctionType type = ms.getType();
        obj.addProperty("return_type", type.getReturnType().name());

        JsonArray params = new JsonArray();
        for (int i = 0; i < type.getParameterTypes().size(); i++)
        {
            Type pType = type.getParameterTypes().get(i);
            JsonObject pObj = new JsonObject();

            if (type.getParameterInfos() != null && i < type.getParameterInfos().size())
            {
                ParameterInfo info = type.getParameterInfos().get(i);
                pObj.addProperty("name", info.name());
                pObj.addProperty("type", info.type().name());
                if (info.cvtHint() != null)
                {
                    pObj.addProperty("cvt_hint", info.cvtHint());
                }
            }
            else
            {
                pObj.addProperty("name", "p" + i);
                pObj.addProperty("type", pType.name());
            }
            params.add(pObj);
        }
        obj.add("parameters", params);

        if (!ms.getModifiers().isEmpty())
        {
            JsonArray mods = new JsonArray();
            for (var m : ms.getModifiers())
                mods.add(m.name());
            obj.add("modifiers", mods);
        }

        if (!ms.getTypeParameters().isEmpty())
        {
            JsonArray tps = new JsonArray();
            for (TypeParameterType tpt : ms.getTypeParameters())
            {
                JsonObject tpObj = new JsonObject();
                tpObj.addProperty("name", tpt.name());
                if (tpt.getBound() != null)
                {
                    tpObj.addProperty("bound", tpt.getBound().name());
                }
                tps.add(tpObj);
            }
            obj.add("type_parameters", tps);

            // Embed pre-compiled erased bitcode so consumers can compile generics
            // without the original source (zero-cost via LLVM link + inline).
            byte[] bitcode = ms.getGenericBitcode();
            if (bitcode != null && bitcode.length > 0)
            {
                obj.addProperty("erased_bitcode", Base64.getEncoder().encodeToString(bitcode));
            }
        }

        return obj;
    }

    private JsonObject exportType(TypeSymbol ts)
    {
        JsonObject obj = new JsonObject();
        Type type = ts.getType();
        obj.addProperty("name", ts.getName());

        if (type instanceof ClassType ct)
        {
            obj.addProperty("kind", "class");
            obj.add("members", exportTable(ct.getMemberScope()));
        }
        else if (type instanceof StructType st)
        {
            obj.addProperty("kind", "struct");
            obj.add("members", exportTable(st.getMemberScope()));
        }
        else if (type instanceof EnumType et)
        {
            obj.addProperty("kind", "enum");
            JsonArray variants = new JsonArray();
            for (String v : et.getMemberScope().getSymbols().keySet())
                variants.add(v);
            obj.add("variants", variants);
        }
        else if (type instanceof UnionType)
        {
            obj.addProperty("kind", "union");
        }
        else if (type instanceof TraitType tt)
        {
            obj.addProperty("kind", "trait");
            obj.add("members", exportTable(tt.getMemberScope()));
        }
        else if (type instanceof TagType tag)
        {
            // Export the tag as a list of its member type names so that
            // consumers can reconstruct the TagType for bound checking.
            obj.addProperty("kind", "tag");
            JsonArray members = new JsonArray();
            for (Type m : tag.getMemberTypes())
                members.add(m.name());
            obj.add("members", members);
        }

        return obj;
    }

    private JsonObject exportVariable(VariableSymbol vs)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("kind", "variable");
        obj.addProperty("name", vs.getName());
        obj.addProperty("type", vs.getType().name());
        obj.addProperty("mutable", vs.isMutable());
        return obj;
    }

    // ── Primitive impl export ────────────────────────────────────────────────────

    private JsonArray exportPrimitiveImpls(Map<Type, SymbolTable> primitiveImplScopes)
    {
        JsonArray array = new JsonArray();
        for (Map.Entry<Type, SymbolTable> entry : primitiveImplScopes.entrySet())
        {
            Type primType       = entry.getKey();
            SymbolTable scope   = entry.getValue();

            JsonObject obj = new JsonObject();
            obj.addProperty("type", primType.name());

            JsonArray methods = new JsonArray();
            for (Symbol sym : scope.getSymbols().values())
            {
                if (sym instanceof MethodSymbol ms)
                {
                    methods.add(exportMethod(ms));
                }
            }
            obj.add("methods", methods);
            array.add(obj);
        }
        return array;
    }

}
