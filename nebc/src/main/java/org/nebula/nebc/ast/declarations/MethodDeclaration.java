package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.GenericParam;
import org.nebula.nebc.ast.Modifier;
import org.nebula.nebc.ast.Parameter;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

public class MethodDeclaration extends Declaration
{
	public final List<Modifier> modifiers;
	public final TypeNode returnType; // null for void
	public final String name;
	public final List<GenericParam> typeParams;
	public final List<Parameter> parameters;
	public final ASTNode body; // Can be a Block or an Expression (for =>)
	public boolean isExtern;

	public MethodDeclaration(SourceSpan span, boolean isExtern, List<Modifier> modifiers, TypeNode returnType, String name, List<GenericParam> typeParams, List<Parameter> parameters, ASTNode body)
	{
		super(span);
		this.isExtern = isExtern;
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;
		this.typeParams = typeParams;
		this.parameters = parameters;
		this.body = body;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitMethodDeclaration(this);
	}
}