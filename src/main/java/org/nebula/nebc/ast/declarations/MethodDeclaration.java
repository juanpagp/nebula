package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.*;
import org.nebula.nebc.ast.types.Type;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.List;

public class MethodDeclaration extends Declaration
{
	private final List<Modifier> modifiers;
	private final Type returnType; // null for void
	private final String name;
	private final List<Parameter> parameters;
	private final ASTNode body; // Can be a Block or an Expression (for =>)

	public MethodDeclaration(SourceSpan span, List<Modifier> modifiers, Type returnType, String name, List<Parameter> parameters, ASTNode body)
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