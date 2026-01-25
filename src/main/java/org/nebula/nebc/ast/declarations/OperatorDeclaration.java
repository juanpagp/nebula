package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.Parameter;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class OperatorDeclaration extends Declaration
{
	public final String operatorToken;
	public final List<Parameter> parameters;
	public final ASTNode body; // Block or FAT_ARROW expression

	public OperatorDeclaration(SourceSpan span, String operatorToken, List<Parameter> parameters, ASTNode body)
	{
		super(span);
		this.operatorToken = operatorToken;
		this.parameters = parameters;
		this.body = body;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitOperatorDeclaration(this);
	}
}