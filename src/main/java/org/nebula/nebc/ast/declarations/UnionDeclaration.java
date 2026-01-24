package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class UnionDeclaration extends Declaration
{
	public final String name;
	public final List<UnionVariant> variants;

	public UnionDeclaration(SourceSpan span, String name, List<UnionVariant> variants)
	{
		super(span);
		this.name = name;
		this.variants = variants;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitUnionDeclaration(this);
	}
}