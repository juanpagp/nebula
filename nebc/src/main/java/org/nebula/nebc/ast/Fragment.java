package org.nebula.nebc.ast;

import org.nebula.nebc.frontend.diagnostic.SourceSpan;
import java.util.List;

/**
 * A temporary container for multiple AST nodes that should be flattened
 * into the parent's list of children.
 */
public class Fragment extends ASTNode
{
    public final List<ASTNode> nodes;

    public Fragment(SourceSpan span, List<ASTNode> nodes)
    {
        super(span);
        this.nodes = List.copyOf(nodes);
    }

    @Override
    public <R> R accept(ASTVisitor<R> visitor)
    {
        // Fragments should be flattened by the parent node during visit
        return null;
    }
}
