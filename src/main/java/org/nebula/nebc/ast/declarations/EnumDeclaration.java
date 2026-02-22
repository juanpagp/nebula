package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class EnumDeclaration extends Declaration
{
	public final String name;
	public final List<String> variants;

	public EnumDeclaration(SourceSpan span, String name, List<String> variants)
	{
		super(span);
		this.name = name;
		this.variants = variants;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitEnumDeclaration(this);
	}
}
