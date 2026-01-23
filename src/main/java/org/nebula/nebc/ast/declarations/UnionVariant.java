package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.Type;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class UnionVariant extends ASTNode
{
	private final String name;
	private final Type payload; // null if no payload

	public UnionVariant(SourceSpan span, String name, Type payload)
	{
		super(span);
		this.name = name;
		this.payload = payload;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitUnionVariant(this);
	}
}