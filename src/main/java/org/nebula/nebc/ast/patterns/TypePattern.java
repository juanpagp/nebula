package org.nebula.nebc.ast.patterns;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.types.Type;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

/**
 * Matches a specific type and optionally binds it to a variable.
 * Example: `match x { String s => ... }` or `match x { Int => ... }`
 */
public class TypePattern extends Pattern
{
	private final Type type;
	private final String variableName; // Nullable (if just checking type without binding)

	public TypePattern(SourceSpan span, Type type, String variableName)
	{
		super(span);
		this.type = type;
		this.variableName = variableName;
	}

	public Type getType()
	{
		return type;
	}

	public String getVariableName()
	{
		return variableName;
	}

	public boolean isBinding()
	{
		return variableName != null;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitTypePattern(this);
	}
}