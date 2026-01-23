package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

public class ForStatement extends Statement
{
	private final Statement initializer; // Can be a VariableDeclaration
	private final Expression condition;
	private final Expression iterator;
	private final Statement body;

	public ForStatement(SourceSpan span, Statement initializer, Expression condition, Expression iterator, Statement body)
	{
		super(span);
		this.initializer = initializer;
		this.condition = condition;
		this.iterator = iterator;
		this.body = body;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitForStatement(this);
	}
}