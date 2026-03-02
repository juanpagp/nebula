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
import java.util.Collections;
import java.util.List;

public class SymbolImporter
{
    private final Gson gson = new Gson();

    public void importSymbols(String path, SymbolTable targetTable) throws IOException
    {
        try (FileReader reader = new FileReader(path))
        {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray symbols = root.getAsJsonArray("symbols");
            importTable(symbols, targetTable);
        }
    }

    private void importTable(JsonArray symbols, SymbolTable table)
    {
        if (symbols == null)
            return;
        for (JsonElement el : symbols)
        {
            JsonObject obj = el.getAsJsonObject();
            if (obj.get("kind") == null)
            {
                continue;
            }
            String kind = obj.get("kind").getAsString();

            switch (kind)
            {
                case "namespace":
                    String nsName = obj.get("name").getAsString();
                    NamespaceType nsType = new NamespaceType(nsName, table);
                    NamespaceSymbol nsSym = new NamespaceSymbol(nsName, nsType, null);
                    table.define(nsSym);
                    importTable(obj.getAsJsonArray("symbols"), nsSym.getMemberTable());
                    break;
                case "method":
                    table.define(importMethod(obj, table));
                    break;
                case "class":
                case "struct":
                case "trait":
                    table.define(importCompositeType(obj, table, kind));
                    break;
                case "enum":
                    table.define(importEnumType(obj, table));
                    break;
                case "variable":
                    table.define(importVariable(obj, table));
                    break;
            }
        }
    }

    private MethodSymbol importMethod(JsonObject obj, SymbolTable table)
    {
        String name = obj.get("name").getAsString();
        boolean isExtern = obj.get("is_extern").getAsBoolean();

        List<TypeParameterType> typeParams = new ArrayList<>();
        SymbolTable resolveTable = table;

        if (obj.has("type_parameters"))
        {
            resolveTable = new SymbolTable(table);
            JsonArray tps = obj.getAsJsonArray("type_parameters");
            for (JsonElement tpEl : tps)
            {
                JsonObject tpObj = tpEl.getAsJsonObject();
                String tpName = tpObj.get("name").getAsString();
                TraitType bound = null;
                if (tpObj.has("bound"))
                {
                    TypeSymbol boundSym = table.resolveType(tpObj.get("bound").getAsString());
                    if (boundSym != null && boundSym.getType() instanceof TraitType tt)
                    {
                        bound = tt;
                    }
                }
                TypeParameterType tpt = new TypeParameterType(tpName, bound);
                typeParams.add(tpt);
                resolveTable.define(new TypeSymbol(tpName, tpt, null));
            }
        }

        Type returnType = resolveType(obj.get("return_type").getAsString(), resolveTable);

        List<Type> paramTypes = new ArrayList<>();
        List<ParameterInfo> paramInfos = new ArrayList<>();
        JsonArray params = obj.getAsJsonArray("parameters");
        for (JsonElement pEl : params)
        {
            JsonObject pObj = pEl.getAsJsonObject();
            Type pType = resolveType(pObj.get("type").getAsString(), resolveTable);
            paramTypes.add(pType);

            String pName = pObj.get("name").getAsString();
            CVTModifier hint = null;
            if (pObj.has("cvt_hint"))
            {
                hint = CVTModifier.fromString(pObj.get("cvt_hint").getAsString());
            }
            paramInfos.add(new ParameterInfo(hint, pType, pName));
        }

        FunctionType type = new FunctionType(returnType, paramTypes, paramInfos);
        return new MethodSymbol(name, type, Collections.emptyList(), isExtern, null, typeParams);
    }

    private TypeSymbol importCompositeType(JsonObject obj, SymbolTable table, String kind)
    {
        String name = obj.get("name").getAsString();
        CompositeType type;
        if (kind.equals("class"))
        {
            type = new ClassType(name, table);
        }
        else if (kind.equals("struct"))
        {
            type = new StructType(name, table);
        }
        else
        { // trait
            type = new TraitType(name, table);
        }

        TypeSymbol sym = new TypeSymbol(name, type, null);
        type.getMemberScope().setOwner(sym);

        if (obj.has("members"))
        {
            importTable(obj.getAsJsonArray("members"), type.getMemberScope());
        }

        return sym;
    }

    private TypeSymbol importEnumType(JsonObject obj, SymbolTable table)
    {
        String name = obj.get("name").getAsString();
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
        return sym;
    }

    private VariableSymbol importVariable(JsonObject obj, SymbolTable table)
    {
        String name = obj.get("name").getAsString();
        Type type = resolveType(obj.get("type").getAsString(), table);
        boolean mutable = obj.get("mutable").getAsBoolean();
        return new VariableSymbol(name, type, mutable, null);
    }

    private Type resolveType(String name, SymbolTable table)
    {
        // Try primitives first
        Type t = PrimitiveType.getByName(name);
        if (t != null)
            return t;

        // Try resolving in table
        TypeSymbol sym = table.resolveType(name);
        if (sym != null)
            return sym.getType();

        // Fallback to ANY if not found (might be forward ref or external lib)
        return Type.ANY;
    }
}
