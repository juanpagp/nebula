package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.Collections;
import java.util.List;

public class ForStatement extends Statement
{
	public final Statement initializer;
	public final Expression condition;
	public final List<Expression> iterators; // Updated from single Expression
	public final Statement body;

	public ForStatement(SourceSpan span, Statement initializer, Expression condition, List<Expression> iterators, Statement body)
	{
		super(span);
		this.initializer = initializer;
		this.condition = condition;
		this.iterators = iterators;
		this.body = body;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitForStatement(this);
	}
}