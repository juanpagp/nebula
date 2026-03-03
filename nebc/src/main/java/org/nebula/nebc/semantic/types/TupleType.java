package org.nebula.nebc.semantic.types;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TupleType extends Type
{
    public final List<Type> elementTypes;
    /** Optional field names parallel to {@code elementTypes}; may be empty or contain {@code null}s. */
    public final List<String> fieldNames;

    public TupleType(List<Type> elementTypes, List<String> fieldNames)
    {
        this.elementTypes = List.copyOf(elementTypes);
        this.fieldNames = (fieldNames != null) ? Collections.unmodifiableList(fieldNames) : Collections.emptyList();
    }

    /** Convenience constructor for unnamed tuples. */
    public TupleType(List<Type> elementTypes)
    {
        this(elementTypes, Collections.emptyList());
    }

    /**
     * Returns the field index for the given name, or {@code -1} if not found.
     */
    public int indexOfField(String name)
    {
        for (int i = 0; i < fieldNames.size(); i++)
        {
            if (name.equals(fieldNames.get(i)))
                return i;
        }
        return -1;
    }

    @Override
    public String name()
    {
        return "(" + elementTypes.stream().map(Type::name).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public boolean isAssignableTo(Type destination)
    {
        if (this == destination)
            return true;
        if (destination instanceof TupleType other)
        {
            if (this.elementTypes.size() != other.elementTypes.size())
                return false;
            for (int i = 0; i < elementTypes.size(); i++)
            {
                if (!elementTypes.get(i).isAssignableTo(other.elementTypes.get(i)))
                    return false;
            }
            return true;
        }
        return super.isAssignableTo(destination);
    }
}
