package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

public class UseStatement extends Statement
{
	public final String qualifiedName;
	public final String alias;

	public UseStatement(SourceSpan span, String qualifiedName, String alias)
	{
		super(span);
		this.qualifiedName = qualifiedName;
		this.alias = alias;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitUseStatement(this);
	}
}