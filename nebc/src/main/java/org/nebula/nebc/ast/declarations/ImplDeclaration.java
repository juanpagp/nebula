package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

/**
 * Represents {@code impl TraitName for Type1, Type2 { ... }}.
 */
public class ImplDeclaration extends Declaration
{
    /** The trait being implemented. */
    public final TypeNode traitType;

    /** The type implementing the trait. */
    public final TypeNode targetType;

    /** The method implementations in this block. */
    public final List<MethodDeclaration> members;

    public ImplDeclaration(SourceSpan span, TypeNode traitType, TypeNode targetType, List<MethodDeclaration> members)
    {
        super(span);
        this.traitType = traitType;
        this.targetType = targetType;
        this.members = members;
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor)
    {
        return visitor.visitImplDeclaration(this);
    }
}
