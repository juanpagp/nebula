package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.*;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class MethodDeclaration extends Declaration
{
	public final List<Modifier> modifiers;
	public final TypeNode returnType; // null for void
	public final String name;
	public final List<Parameter> parameters;
	public final ASTNode body; // Can be a Block or an Expression (for =>)

	public MethodDeclaration(SourceSpan span, List<Modifier> modifiers, TypeNode returnType, String name, List<Parameter> parameters, ASTNode body)
	{
		super(span);
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;
		this.parameters = parameters;
		this.body = body;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitMethodDeclaration(this);
	}
}