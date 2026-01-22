package org.nebula.nebc.ast.types;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class ArrayType extends Type
{
	private final Type baseType; // e.g., 'int' in 'int[]'
	private final int dimensions;        // 1 for [], 2 for [][], etc.

	public ArrayType(SourceSpan span, Type baseType, int dimensions)
	{
		super(span);
		this.baseType = baseType;
		this.dimensions = dimensions;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		// You can add visitArrayType to ASTVisitor or handle it in visitTypeReference
		return visitor.visitTypeReference(this);
	}
}