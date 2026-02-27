package org.nebula.nebc.semantic.types;

import java.util.List;
import java.util.stream.Collectors;

public class TupleType extends Type {
    public final List<Type> elementTypes;

    public TupleType(List<Type> elementTypes) {
        this.elementTypes = List.copyOf(elementTypes);
    }

    @Override
    public String name() {
        return "(" + elementTypes.stream().map(Type::name).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public boolean isAssignableTo(Type destination) {
        if (this == destination)
            return true;
        if (destination instanceof TupleType other) {
            if (this.elementTypes.size() != other.elementTypes.size())
                return false;
            for (int i = 0; i < elementTypes.size(); i++) {
                if (!elementTypes.get(i).isAssignableTo(other.elementTypes.get(i)))
                    return false;
            }
            return true;
        }
        return super.isAssignableTo(destination);
    }
}
