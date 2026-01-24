package org.nebula.nebc.ast.statements;

import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;

import java.util.Collections;
import java.util.List;

public class ForStatement extends Statement
{
	private final Statement initializer;
	private final Expression condition;
	private final List<Expression> iterators; // Updated from single Expression
	private final Statement body;

	public ForStatement(SourceSpan span, Statement initializer, Expression condition, List<Expression> iterators, Statement body)
	{
		super(span);
		this.initializer = initializer;
		this.condition = condition;
		this.iterators = iterators;
		this.body = body;
	}

	public Statement getInitializer()
	{
		return initializer;
	}

	public Expression getCondition()
	{
		return condition;
	}

	public List<Expression> getIterators()
	{
		return Collections.unmodifiableList(iterators);
	}

	public Statement getBody()
	{
		return body;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitForStatement(this);
	}
}