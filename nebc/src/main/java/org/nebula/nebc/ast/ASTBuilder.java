package org.nebula.nebc.ast;

import org.nebula.nebc.ast.declarations.*;
import org.nebula.nebc.ast.expressions.*;
import org.nebula.nebc.ast.expressions.LiteralExpression.LiteralType;
import org.nebula.nebc.ast.patterns.*;
import org.nebula.nebc.ast.statements.*;
import org.nebula.nebc.ast.tags.TagAtom;
import org.nebula.nebc.ast.tags.TagExpression;
import org.nebula.nebc.ast.tags.TagOperation;
import org.nebula.nebc.ast.tags.TagStatement;
import org.nebula.nebc.ast.types.ArrayType;
import org.nebula.nebc.ast.types.NamedType;
import org.nebula.nebc.ast.types.TupleType;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;
import org.nebula.nebc.frontend.diagnostic.SourceUtil;
import org.nebula.nebc.frontend.parser.ParsingResult;
import org.nebula.nebc.frontend.parser.generated.NebulaParser;
import org.nebula.nebc.frontend.parser.generated.NebulaParserBaseVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts the ANTLR Parse Tree into the Nebula Abstract Syntax Tree (AST).
 */
public class ASTBuilder extends NebulaParserBaseVisitor<ASTNode>
{
	public static String currentFileName = null;

	/**
	 * Entry point to convert a single Parse Tree into a Nebula AST.
	 */
	public static CompilationUnit buildAst(ParsingResult tree)
	{
		ASTBuilder builder = new ASTBuilder();
		currentFileName = tree.file().fileName();
		return (CompilationUnit) builder.visit(tree.compilationUnitRoot());
	}

	/**
	 * Entry point to convert multiple files at once.
	 */
	public static List<CompilationUnit> buildAST(List<ParsingResult> trees)
	{
		List<CompilationUnit> units = new ArrayList<>();
		for (ParsingResult tree : trees)
		{
			units.add(buildAst(tree));
		}
		return units;
	}

	@Override
	public ASTNode visitCompilation_unit(NebulaParser.Compilation_unitContext ctx)
	{
		List<ASTNode> directives = new ArrayList<>();
		List<ASTNode> declarations = new ArrayList<>();

		// 1. Visit all nodes
		for (var declCtx : ctx.top_level_declaration())
		{
			ASTNode node = visit(declCtx);
			if (node instanceof UseStatement || node instanceof TagStatement)
			{
				directives.add(node);
			}
			else if (node instanceof Declaration)
			{
				declarations.add(node);
			}
			else if (node instanceof NamespaceDeclaration)
			{
				declarations.add(node);
			}
		}

		// 2. Inject Prelude (auto-imports)
		// We avoid injecting if the file is part of the 'std' library itself to
		// prevent circularity.
		boolean isStdFile = false;
		if (currentFileName != null && (currentFileName.contains("/std/") || currentFileName.contains("\\std\\")
				|| currentFileName.startsWith("std/")))
		{
			isStdFile = true;
		}

		if (!isStdFile)
		{
			SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
			directives.add(new UseStatement(span, "std::io", null));
			directives.add(new UseStatement(span, "std::traits", null));
		}

		return new CompilationUnit(SourceUtil.createSpan(ctx, currentFileName), directives, declarations);
	}

	// =========================================================================
	// 1. Declarations & Top Level
	// =========================================================================

	@Override
	public ASTNode visitNamespace_declaration(NebulaParser.Namespace_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String name = ctx.qualified_name().getText();
		List<ASTNode> members = new ArrayList<>();

		boolean isBlockDeclaration = ctx.SEMICOLON() == null;

		if (ctx.top_level_declaration() != null)
		{
			for (var decl : ctx.top_level_declaration())
			{
				members.add(visit(decl));
			}
		}

		return new NamespaceDeclaration(span, name, members, isBlockDeclaration);
	}

	@Override
	public ASTNode visitUse_statement(NebulaParser.Use_statementContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String qualifiedName = ctx.qualified_name().getText();
		String alias = null;

		if (ctx.use_tail() != null)
		{
			if (ctx.use_tail().use_alias() != null)
			{
				alias = ctx.use_tail().use_alias().IDENTIFIER().getText();
			}
			else if (ctx.use_tail().IDENTIFIER() != null)
			{
				alias = ctx.use_tail().IDENTIFIER().getText();
			}
		}

		return new UseStatement(span, qualifiedName, alias);
	}

	@Override
	public ASTNode visitExtern_declaration(NebulaParser.Extern_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);

		// Extract language from REGULAR_STRING (e.g., "C" or "C++")
		String langStr = ctx.REGULAR_STRING().getText();
		// Remove quotes: "C" -> C
		String language = langStr.substring(1, langStr.length() - 1);

		// Check visibility modifier
		boolean isPrivate = false;
		if (ctx.modifiers() != null && !ctx.modifiers().visibility_modifier().isEmpty())
		{
			isPrivate = ctx.modifiers().visibility_modifier().get(0).PRIVATE() != null;
		}

		// Extract extern members (method declarations without bodies)
		List<MethodDeclaration> members = new ArrayList<>();
		if (ctx.extern_member() != null)
		{
			for (var member : ctx.extern_member())
			{
				// Each extern_member contains a method_declaration
				MethodDeclaration method = (MethodDeclaration) visit(member.method_declaration());
				members.add(method);
			}
		}

		return new ExternDeclaration(span, language, members, isPrivate);
	}

	@Override
	public ASTNode visitClass_declaration(NebulaParser.Class_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String name = ctx.IDENTIFIER().getText();

		List<TypeNode> inheritance = new ArrayList<>();
		if (ctx.inheritance_clause() != null)
		{
			for (var typeCtx : ctx.inheritance_clause().type())
			{
				inheritance.add((TypeNode) visit(typeCtx));
			}
		}

		List<Declaration> members = new ArrayList<>();
		for (var memberCtx : ctx.class_body().class_member())
		{
			members.add((Declaration) visit(memberCtx.getChild(0)));
		}

		return new ClassDeclaration(span, name, inheritance, members);
	}

	@Override
	public ASTNode visitStruct_declaration(NebulaParser.Struct_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String name = ctx.IDENTIFIER().getText();

		List<TypeNode> inheritance = new ArrayList<>();
		if (ctx.inheritance_clause() != null)
		{
			for (var typeCtx : ctx.inheritance_clause().type())
			{
				inheritance.add((TypeNode) visit(typeCtx));
			}
		}

		List<Declaration> members = new ArrayList<>();
		for (var memberCtx : ctx.struct_body().struct_member())
		{
			members.add((Declaration) visit(memberCtx.getChild(0)));
		}

		return new StructDeclaration(span, name, inheritance, members);
	}

	@Override
	public ASTNode visitEnum_declaration(NebulaParser.Enum_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String name = ctx.IDENTIFIER().getText();

		List<String> variants = new ArrayList<>();
		if (ctx.enum_block() != null)
		{
			for (var idCtx : ctx.enum_block().IDENTIFIER())
			{
				variants.add(idCtx.getText());
			}
		}

		return new EnumDeclaration(span, name, variants);
	}

	@Override
	public ASTNode visitTrait_declaration(NebulaParser.Trait_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String name = ctx.IDENTIFIER().getText();
		List<MethodDeclaration> members = new ArrayList<>();

		// Trait body can be a block or single method in grammar, handling block here
		if (ctx.trait_body().trait_block() != null)
		{
			for (var member : ctx.trait_body().trait_block().trait_member())
			{
				members.add((MethodDeclaration) visit(member.method_declaration()));
			}
		}
		else if (ctx.trait_body().method_declaration() != null)
		{
			members.add((MethodDeclaration) visit(ctx.trait_body().method_declaration()));
		}

		return new TraitDeclaration(span, name, members);
	}

	@Override
	public ASTNode visitUnion_declaration(NebulaParser.Union_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String name = ctx.IDENTIFIER().getText();

		List<UnionVariant> variants = new ArrayList<>();
		if (ctx.union_body() != null)
		{
			for (var variantCtx : ctx.union_body().union_variant())
			{
				String vName = variantCtx.IDENTIFIER().getText();
				TypeNode payload = null;
				if (variantCtx.union_payload() != null && variantCtx.union_payload().parameter() != null)
				{
					payload = (TypeNode) visit(variantCtx.union_payload().parameter().type());
				}
				variants.add(new UnionVariant(SourceUtil.createSpan(variantCtx, currentFileName), vName, payload));
			}
		}

		return new UnionDeclaration(span, name, variants);
	}

	@Override
	public ASTNode visitMethod_declaration(NebulaParser.Method_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		List<Modifier> modifiers = getModifiers(ctx.modifiers());

		TypeNode returnType = null;
		if (ctx.return_type().type() != null)
		{
			returnType = (TypeNode) visit(ctx.return_type().type());
		}

		String name = ctx.IDENTIFIER().getText();
		List<Parameter> parameters = getParameters(ctx.parameters());

		ASTNode body = getMethodBody(ctx.method_body());

		return new MethodDeclaration(span, false, modifiers, returnType, name, parameters, body);
	}

	@Override
	public ASTNode visitConstructor_declaration(NebulaParser.Constructor_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String name = ctx.IDENTIFIER().getText();
		List<Parameter> parameters = getParameters(ctx.parameters());
		StatementBlock body = (StatementBlock) visit(ctx.statement_block());

		return new ConstructorDeclaration(span, name, parameters, body);
	}

	@Override
	public ASTNode visitOperator_declaration(NebulaParser.Operator_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String operatorToken = ctx.overloadable_operator().getText();
		List<Parameter> parameters = getParameters(ctx.parameters());
		ASTNode body = getMethodBody(ctx.method_body());

		return new OperatorDeclaration(span, operatorToken, parameters, body);
	}

	@Override
	public ASTNode visitVariable_declaration(NebulaParser.Variable_declarationContext ctx)
	{
		// This handles "var x = 1;" or "int x = 1;" as a statement
		return buildVariableDeclaration(ctx, ctx.modifiers(), ctx.VAR() != null, ctx.type(),
				ctx.variable_declarators());
	}

	@Override
	public ASTNode visitField_declaration(NebulaParser.Field_declarationContext ctx)
	{
		// Field declaration in class context, usually has no modifiers in this specific
		// grammar rule
		// (modifiers are higher up in top-level, but fields inside structs/classes use
		// this rule)
		// If modifiers are needed for fields, the grammar might pass them down or they
		// are implicit.
		// Based on the grammar provided: field_declaration : type variable_declarators
		// SEMICOLON
		// It lacks explicit modifiers in the rule, defaulting to private/none.
		return buildVariableDeclaration(ctx, ctx.variable_declaration().modifiers(),
				ctx.variable_declaration().VAR() != null, ctx.variable_declaration().type(),
				ctx.variable_declaration().variable_declarators());
	}

	@Override
	public ASTNode visitConst_declaration(NebulaParser.Const_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		VariableDeclaration varDecl = (VariableDeclaration) visitVariable_declaration(ctx.variable_declaration());
		return new ConstDeclaration(span, varDecl);
	}

	private VariableDeclaration buildVariableDeclaration(org.antlr.v4.runtime.ParserRuleContext ctx,
														 NebulaParser.ModifiersContext modCtx,
														 boolean isVar,
														 NebulaParser.TypeContext typeCtx,
														 NebulaParser.Variable_declaratorsContext declsCtx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		// Modifiers are strictly for MethodDeclaration in this specific grammar version
		// usually,
		// but if VariableDeclaration supports them in AST, we parse them.
		// The AST Node VariableDeclaration currently doesn't store Modifiers (based on
		// ast.txt),
		// so we skip them here or you need to update ASTNode.
		// Assuming we just want the type and declarators:

		TypeNode type = isVar ? null : (TypeNode) visit(typeCtx);
		List<VariableDeclarator> declarators = new ArrayList<>();

		for (var decl : declsCtx.variable_declarator())
		{
			String name = decl.IDENTIFIER().getText();
			Expression init = null;
			if (decl.nonAssignmentExpression() != null)
			{
				init = (Expression) visit(decl.nonAssignmentExpression());
			}
			declarators.add(new VariableDeclarator(name, init));
		}

		return new VariableDeclaration(span, type, declarators, isVar);
	}

	// =========================================================================
	// 2. Statements
	// =========================================================================

	@Override
	public ASTNode visitExpression_block(NebulaParser.Expression_blockContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		List<Statement> statements = new ArrayList<>();

		if (ctx.block_statements() != null)
		{
			for (var stmt : ctx.block_statements().statement())
			{
				statements.add((Statement) visit(stmt));
			}
		}

		Expression tail = null;
		if (ctx.block_tail() != null && ctx.block_tail().expression() != null)
		{
			tail = (Expression) visit(ctx.block_tail().expression());
		}

		return new ExpressionBlock(span, statements, tail);
	}

	@Override
	public ASTNode visitStatement_block(NebulaParser.Statement_blockContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		List<Statement> statements = new ArrayList<>();

		if (ctx.block_statements() != null)
		{
			for (var stmt : ctx.block_statements().statement())
			{
				statements.add((Statement) visit(stmt));
			}
		}
		return new StatementBlock(span, statements);
	}

	@Override
	public ASTNode visitIf_statement(NebulaParser.If_statementContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		Expression condition = (Expression) visit(ctx.parenthesized_expression().expression());
		Statement thenBranch = (Statement) visit(ctx.statement(0));
		Statement elseBranch = ctx.statement().size() > 1 ? (Statement) visit(ctx.statement(1)) : null;

		return new IfStatement(span, condition, thenBranch, elseBranch);
	}

	@Override
	public ASTNode visitFor_statement(NebulaParser.For_statementContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);

		Statement initializer = null;
		Expression condition = null;
		List<Expression> iterators = new ArrayList<>();

		if (ctx.traditional_for_control() != null)
		{
			var control = ctx.traditional_for_control();

			// Initializer
			if (control.for_initializer() != null)
			{
				if (control.for_initializer().variable_declaration() != null)
				{
					initializer = (Statement) visit(control.for_initializer().variable_declaration());
				}
				else if (control.for_initializer().expression_list() != null)
				{
					// Wrap expression list in an ExpressionStatement (or Block if multiple?
					// simplified to first for now or single expr stmt)
					// Usually for-loops allow comma separated expressions.
					// Nebula AST ForStatement expects a 'Statement initializer'.
					// We can wrap the first expression or create a block if needed.
					// For simplicity, taking the first expression as a statement.
					Expression expr = (Expression) visit(control.for_initializer().expression_list().expression(0));
					initializer = new ExpressionStatement(expr.getSpan(), expr);
				}
			}

			// Condition
			if (control.expression() != null)
			{
				condition = (Expression) visit(control.expression());
			}

			// Iterators
			if (control.for_iterator() != null && control.for_iterator().expression_list() != null)
			{
				for (var exprCtx : control.for_iterator().expression_list().expression())
				{
					iterators.add((Expression) visit(exprCtx));
				}
			}
		}
		else if (ctx.parenthesized_expression() != null)
		{
			// "for (condition) stmt" style (while loop)
			condition = (Expression) visit(ctx.parenthesized_expression().expression());
		}

		Statement body = (Statement) visit(ctx.statement());
		return new ForStatement(span, initializer, condition, iterators, body);
	}

	@Override
	public ASTNode visitWhile_statement(NebulaParser.While_statementContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		Expression condition = (Expression) visit(ctx.parenthesized_expression().expression());
		Statement body = (Statement) visit(ctx.statement());
		return new WhileStatement(span, condition, body);
	}

	@Override
	public ASTNode visitForeach_statement(NebulaParser.Foreach_statementContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		var control = ctx.foreach_control();

		TypeNode type = control.VAR() != null ? null : (TypeNode) visit(control.type());
		String name = control.IDENTIFIER().getText();
		Expression iterable = (Expression) visit(control.expression());
		StatementBlock body = (StatementBlock) visit(ctx.statement_block());

		return new ForeachStatement(span, type, name, iterable, body);
	}

	@Override
	public ASTNode visitReturn_statement(NebulaParser.Return_statementContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		Expression value = ctx.expression() != null ? (Expression) visit(ctx.expression()) : null;
		return new ReturnStatement(span, value);
	}

	@Override
	public ASTNode visitExpression_statement(NebulaParser.Expression_statementContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		Expression expression = (Expression) visit(ctx.expression());
		return new ExpressionStatement(span, expression);
	}

	@Override
	public ASTNode visitTag_statement(NebulaParser.Tag_statementContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		Modifier visibility = mapVisibility(ctx.visibility_modifier());
		String alias = ctx.IDENTIFIER().getText();

		// Tag Declaration visiting
		TagExpression tagExpr = visitTagDeclaration(ctx.tag_declaration());

		return new TagStatement(span, visibility, tagExpr, alias);
	}

	// =========================================================================
	// 3. Expressions
	// =========================================================================

	@Override
	public ASTNode visitExpression(NebulaParser.ExpressionContext ctx)
	{
		return visit(ctx.assignment_expression());
	}

	@Override
	public ASTNode visitAssignment_expression(NebulaParser.Assignment_expressionContext ctx)
	{
		if (ctx.assignment_expression() == null)
		{
			return visit(ctx.binary_or_expression());
		}

		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		Expression target = (Expression) visit(ctx.binary_or_expression());
		Expression value = (Expression) visit(ctx.assignment_expression());
		String operator = ctx.assignment_operator().getText();

		return new AssignmentExpression(span, target, operator, value);
	}

	// --- Binary Expressions (Cascading) ---

	@Override
	public ASTNode visitBinary_or_expression(NebulaParser.Binary_or_expressionContext ctx)
	{
		return buildBinaryChain(ctx.binary_and_expression(), ctx.OP_OR(), ctx);
	}

	@Override
	public ASTNode visitBinary_and_expression(NebulaParser.Binary_and_expressionContext ctx)
	{
		return buildBinaryChain(ctx.inclusive_or_expression(), ctx.OP_AND(), ctx);
	}

	@Override
	public ASTNode visitInclusive_or_expression(NebulaParser.Inclusive_or_expressionContext ctx)
	{
		// Manually handle tokens '|' and PIPE
		if (ctx.exclusive_or_expression().size() == 1)
			return visit(ctx.exclusive_or_expression(0));
		Expression left = (Expression) visit(ctx.exclusive_or_expression(0));
		for (int i = 1; i < ctx.exclusive_or_expression().size(); i++)
		{
			Expression right = (Expression) visit(ctx.exclusive_or_expression(i));
			left = new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, BinaryOperator.BIT_OR,
					right);
		}
		return left;
	}

	@Override
	public ASTNode visitExclusive_or_expression(NebulaParser.Exclusive_or_expressionContext ctx)
	{
		// Manually handle token '^'
		if (ctx.and_expression().size() == 1)
			return visit(ctx.and_expression(0));
		Expression left = (Expression) visit(ctx.and_expression(0));
		for (int i = 1; i < ctx.and_expression().size(); i++)
		{
			Expression right = (Expression) visit(ctx.and_expression(i));
			left = new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, BinaryOperator.BIT_XOR,
					right);
		}
		return left;
	}

	@Override
	public ASTNode visitAnd_expression(NebulaParser.And_expressionContext ctx)
	{
		// Manually handle token '&'
		if (ctx.equality_expression().size() == 1)
			return visit(ctx.equality_expression(0));
		Expression left = (Expression) visit(ctx.equality_expression(0));
		for (int i = 1; i < ctx.equality_expression().size(); i++)
		{
			Expression right = (Expression) visit(ctx.equality_expression(i));
			left = new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, BinaryOperator.BIT_AND,
					right);
		}
		return left;
	}

	@Override
	public ASTNode visitEquality_expression(NebulaParser.Equality_expressionContext ctx)
	{
		if (ctx.relational_expression().size() == 1)
			return visit(ctx.relational_expression(0));
		Expression left = (Expression) visit(ctx.relational_expression(0));
		for (int i = 1; i < ctx.relational_expression().size(); i++)
		{
			String op = ctx.getChild(2 * i - 1).getText(); // OP_EQ or OP_NE
			Expression right = (Expression) visit(ctx.relational_expression(i));
			left = new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, mapBinaryOperator(op),
					right);
		}
		return left;
	}

	@Override
	public ASTNode visitRelational_expression(NebulaParser.Relational_expressionContext ctx)
	{
		if (ctx.shift_expression().size() == 1)
			return visit(ctx.shift_expression(0));
		Expression left = (Expression) visit(ctx.shift_expression(0));

		// This rule is tricky because it mixes binary ops (<, >) and 'IS type'.
		// Simplified: iterating children to find operators/operands.
		int childIndex = 1;
		while (childIndex < ctx.getChildCount())
		{
			var child = ctx.getChild(childIndex);
			String text = child.getText();

			if (text.equals("is"))
			{
				// Handle IS expression (often a Type check or pattern match)
				// For simplicity mapping to BinaryOperator.IS if exists, or treating as special
				// expression.
				// Assuming "IS" logic isn't in BinaryOperator yet, ignoring or treating as EQ
				// for structure.
				// *Correction*: Your BinaryOperator enum does not have IS.
				// This would typically be an 'InstanceCheckExpression'.
				// Since AST.txt doesn't have it, I'll return left (skip) or throw.
				childIndex += 2; // skip "is" and "type"
			}
			else
			{
				BinaryOperator op = mapBinaryOperator(text);
				Expression right = (Expression) visit(ctx.getChild(childIndex + 1));
				left = new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, op, right);
				childIndex += 2;
			}
		}
		return left;
	}

	@Override
	public ASTNode visitShift_expression(NebulaParser.Shift_expressionContext ctx)
	{
		// Similar manual loop for shift
		if (ctx.additive_expression().size() == 1)
			return visit(ctx.additive_expression(0));
		Expression left = (Expression) visit(ctx.additive_expression(0));
		for (int i = 1; i < ctx.additive_expression().size(); i++)
		{
			String op = ctx.getChild(2 * i - 1).getText();
			Expression right = (Expression) visit(ctx.additive_expression(i));
			left = new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, mapBinaryOperator(op),
					right);
		}
		return left;
	}

	@Override
	public ASTNode visitAdditive_expression(NebulaParser.Additive_expressionContext ctx)
	{
		if (ctx.multiplicative_expression().size() == 1)
			return visit(ctx.multiplicative_expression(0));
		Expression left = (Expression) visit(ctx.multiplicative_expression(0));
		for (int i = 1; i < ctx.multiplicative_expression().size(); i++)
		{
			String op = ctx.getChild(2 * i - 1).getText();
			Expression right = (Expression) visit(ctx.multiplicative_expression(i));
			left = new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, mapBinaryOperator(op),
					right);
		}
		return left;
	}

	@Override
	public ASTNode visitMultiplicative_expression(NebulaParser.Multiplicative_expressionContext ctx)
	{
		if (ctx.exponentiation_expression().size() == 1)
			return visit(ctx.exponentiation_expression(0));
		Expression left = (Expression) visit(ctx.exponentiation_expression(0));
		for (int i = 1; i < ctx.exponentiation_expression().size(); i++)
		{
			String op = ctx.getChild(2 * i - 1).getText();
			Expression right = (Expression) visit(ctx.exponentiation_expression(i));
			left = new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, mapBinaryOperator(op),
					right);
		}
		return left;
	}

	@Override
	public ASTNode visitExponentiation_expression(NebulaParser.Exponentiation_expressionContext ctx)
	{
		Expression left = (Expression) visit(ctx.unary_expression());
		if (ctx.exponentiation_expression() != null)
		{
			Expression right = (Expression) visit(ctx.exponentiation_expression());
			return new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, BinaryOperator.POW, right);
		}
		return left;
	}

	private Expression buildBinaryChain(List<? extends org.antlr.v4.runtime.ParserRuleContext> exprs,
										List<? extends org.antlr.v4.runtime.tree.TerminalNode> ops,
										org.antlr.v4.runtime.ParserRuleContext ctx)
	{
		Expression left = (Expression) visit(exprs.getFirst());
		for (int i = 0; i < ops.size(); i++)
		{
			Expression right = (Expression) visit(exprs.get(i + 1));
			BinaryOperator op = mapBinaryOperator(ops.get(i).getText());
			left = new BinaryExpression(SourceUtil.createSpan(ctx, currentFileName), left, op, right);
		}
		return left;
	}

	// --- Unary & Primary ---

	@Override
	public ASTNode visitUnary_expression(NebulaParser.Unary_expressionContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);

		if (ctx.primary_expression() != null)
			return visit(ctx.primary_expression());
		if (ctx.cast_expression() != null)
			return visit(ctx.cast_expression());

		// Prefix operators: +, -, !, ~
		String opText = ctx.getChild(0).getText();
		UnaryOperator op = mapUnaryOperator(opText);
		Expression operand = (Expression) visit(ctx.unary_expression());
		return new UnaryExpression(span, op, operand, false); // isPostfix = false
	}

	@Override
	public ASTNode visitCast_expression(NebulaParser.Cast_expressionContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		TypeNode type = (TypeNode) visit(ctx.type());
		Expression expr = (Expression) visit(ctx.unary_expression());
		return new CastExpression(span, type, expr);
	}

	@Override
	public ASTNode visitPrimary_expression(NebulaParser.Primary_expressionContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);

		// 1. Visit the start (Identifier, Literal, etc.)
		Expression current = (Expression) visit(ctx.primary_expression_start());

		// 2. Wrap with postfix operators
		for (var postfix : ctx.postfix_operator())
		{
			if (postfix.DOT() != null)
			{
				String member = postfix.IDENTIFIER() != null ? postfix.IDENTIFIER().getText()
						: postfix.INTEGER_LITERAL().getText();
				current = new MemberAccessExpression(span, current, member);
			}
			else if (postfix.OPEN_PARENS() != null)
			{
				// Invocation
				List<Expression> args = new ArrayList<>();
				if (postfix.argument_list() != null)
				{
					for (var arg : postfix.argument_list().argument())
					{
						args.add(extractArgument(arg));
					}
				}
				current = new InvocationExpression(span, current, args);
			}
			else if (postfix.OPEN_BRACKET() != null)
			{
				// Indexing
				List<Expression> indices = new ArrayList<>();
				for (var exprCtx : postfix.expression_list().expression())
				{
					indices.add((Expression) visit(exprCtx));
				}
				current = new IndexExpression(span, current, indices);
			}
			else if (postfix.OP_INC() != null || postfix.OP_DEC() != null)
			{
				UnaryOperator op = postfix.OP_INC() != null ? UnaryOperator.INCREMENT : UnaryOperator.DECREMENT;
				current = new UnaryExpression(span, op, current, true); // isPostfix = true
			}
		}
		return current;
	}

	@Override
	public ASTNode visitPrimary_expression_start(NebulaParser.Primary_expression_startContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);

		if (ctx.literal() != null)
			return visit(ctx.literal());
		if (ctx.IDENTIFIER() != null)
			return new IdentifierExpression(span, ctx.IDENTIFIER().getText());
		if (ctx.qualified_name() != null)
			return new IdentifierExpression(span, ctx.qualified_name().getText());
		if (ctx.THIS() != null)
			return new ThisExpression(span);
		if (ctx.parenthesized_expression() != null)
			return visit(ctx.parenthesized_expression().expression());
		if (ctx.tuple_literal() != null)
		{
			List<Expression> elements = new ArrayList<>();
			for (var arg : ctx.tuple_literal().argument())
				elements.add(extractArgument(arg));
			return new TupleLiteralExpression(span, elements);
		}
		if (ctx.array_literal() != null)
		{
			List<Expression> elements = new ArrayList<>();
			if (ctx.array_literal().expression_list() != null)
			{
				for (var e : ctx.array_literal().expression_list().expression())
					elements.add((Expression) visit(e));
			}
			return new ArrayLiteralExpression(span, elements);
		}
		if (ctx.if_expression() != null)
			return visit(ctx.if_expression());
		if (ctx.match_expression() != null)
			return visit(ctx.match_expression());
		if (ctx.expression_block() != null)
			return visit(ctx.expression_block());
		if (ctx.new_expression() != null)
			return visit(ctx.new_expression());

		return null;
	}

	@Override
	public ASTNode visitNew_expression(NebulaParser.New_expressionContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		String typeName = ctx.type().getText();
		List<Expression> args = new ArrayList<>();
		if (ctx.arguments().argument_list() != null)
		{
			for (var arg : ctx.arguments().argument_list().argument())
				args.add(extractArgument(arg));
		}
		return new NewExpression(span, typeName, args);
	}

	@Override
	public ASTNode visitIf_expression(NebulaParser.If_expressionContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		Expression condition = (Expression) visit(ctx.parenthesized_expression().expression());
		ExpressionBlock thenExpressionBlock = (ExpressionBlock) visit(ctx.expression_block(0));
		ExpressionBlock elseExpressionBlock = (ExpressionBlock) visit(ctx.expression_block(1));
		return new IfExpression(span, condition, thenExpressionBlock, elseExpressionBlock);
	}

	@Override
	public ASTNode visitMatch_expression(NebulaParser.Match_expressionContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		Expression selector = (Expression) visit(ctx.parenthesized_expression().expression());
		List<MatchArm> arms = new ArrayList<>();
		for (var armCtx : ctx.match_body().match_arm())
		{
			Pattern pat = (Pattern) visit(armCtx.pattern());
			Expression res = (Expression) visit(armCtx.expression());
			arms.add(new MatchArm(SourceUtil.createSpan(armCtx, currentFileName), pat, res));
		}
		return new MatchExpression(span, selector, arms);
	}

	// =========================================================================
	// 4. Patterns
	// =========================================================================

	@Override
	public ASTNode visitPattern(NebulaParser.PatternContext ctx)
	{
		return visit(ctx.pattern_or());
	}

	@Override
	public ASTNode visitPattern_or(NebulaParser.Pattern_orContext ctx)
	{
		if (ctx.pattern_atom().size() == 1)
			return visit(ctx.pattern_atom(0));

		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		List<Pattern> alts = new ArrayList<>();
		for (var atom : ctx.pattern_atom())
		{
			alts.add((Pattern) visit(atom));
		}
		return new OrPattern(span, alts);
	}

	@Override
	public ASTNode visitPattern_atom(NebulaParser.Pattern_atomContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);

		if (ctx.literal() != null)
		{
			// Unwrap ASTNode to LiteralExpression if possible, literal() returns
			// LiteralExpression
			LiteralExpression lit = (LiteralExpression) visit(ctx.literal());
			return new LiteralPattern(span, lit);
		}
		if (ctx.UNDERSCORE() != null)
		{
			return new WildcardPattern(span);
		}
		if (ctx.IDENTIFIER() != null)
		{
			// Could be a variable binding or a type pattern.
			// In simple grammar, "x" matches anything and binds to x.
			// "String x" is a type pattern.
			// Based on grammar, simple IDENTIFIER is likely a binding (Wildcard with name)
			// But if it's a Type name, we need context. For now, treating as
			// binding/TypePattern with null type?
			// Or simpler: TypePattern with NamedType and null variable?
			// Let's assume IDENTIFIER is a NamedType match for now (TypePattern).
			TypeNode type = new NamedType(span, ctx.IDENTIFIER().getText(), Collections.emptyList());
			return new TypePattern(span, type, null);
		}
		if (ctx.parenthesized_pattern() != null)
		{
			return visit(ctx.parenthesized_pattern().pattern());
		}
		return null;
	}

	// =========================================================================
	// 5. Literals
	// =========================================================================

	@Override
	public ASTNode visitLiteral(NebulaParser.LiteralContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		if (ctx.INTEGER_LITERAL() != null)
			return new LiteralExpression(span, Long.parseLong(ctx.INTEGER_LITERAL().getText()), LiteralType.INT);
		if (ctx.REAL_LITERAL() != null)
		{
			String text = ctx.REAL_LITERAL().getText();
			if (text.endsWith("f") || text.endsWith("F"))
			{
				return new LiteralExpression(span, Float.parseFloat(text), LiteralType.FLOAT);
			}
			return new LiteralExpression(span, Double.parseDouble(text), LiteralType.FLOAT);
		}
		if (ctx.TRUE() != null)
			return new LiteralExpression(span, true, LiteralType.BOOL);
		if (ctx.FALSE() != null)
			return new LiteralExpression(span, false, LiteralType.BOOL);
		if (ctx.string_literal() != null)
			return new LiteralExpression(span, ctx.string_literal().getText(), LiteralType.STRING);
		if (ctx.CHARACTER_LITERAL() != null)
			return new LiteralExpression(span, ctx.CHARACTER_LITERAL().getText(), LiteralType.CHAR);
		return null;
	}

	// =========================================================================
	// 6. Types & Tags
	// =========================================================================

	@Override
	public ASTNode visitType(NebulaParser.TypeContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);
		TypeNode base = null;

		if (ctx.predefined_type() != null)
		{
			base = new NamedType(span, ctx.predefined_type().getText(), Collections.emptyList());
		}
		else if (ctx.class_type() != null)
		{
			String name = ctx.class_type().qualified_name().getText();
			// Generic args logic if present in class_type
			base = new NamedType(span, name, Collections.emptyList());
		}
		else if (ctx.tuple_type() != null)
		{
			List<TypeNode> elems = new ArrayList<>();
			for (var elem : ctx.tuple_type().tuple_type_element())
			{
				elems.add((TypeNode) visit(elem.type()));
			}
			base = new TupleType(span, elems);
		}

		if (!ctx.rank_specifier().isEmpty())
		{
			// Handling array ranks
			for (int i = 0; i < ctx.rank_specifier().size(); i++)
			{
				base = new ArrayType(span, base, 1);
			}
		}
		return base;
	}

	public TagExpression visitTagDeclaration(NebulaParser.Tag_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);

		if (ctx.type() != null)
		{
			return new TagAtom(span, (TypeNode) visit(ctx.type()));
		}
		if (ctx.tag_expression() != null)
		{
			return visitTagExpression(ctx.tag_expression());
		}
		// Enum list handling
		if (ctx.tag_enumeration() != null)
		{
			TagExpression current = new TagAtom(span, (TypeNode) visit(ctx.tag_enumeration().type(0)));
			for (int i = 1; i < ctx.tag_enumeration().type().size(); i++)
			{
				TagExpression next = new TagAtom(span, (TypeNode) visit(ctx.tag_enumeration().type(i)));
				current = new TagOperation(span, TagOperation.Operator.UNION, current, next);
			}
			return current;
		}
		return null;
	}

	public TagExpression visitTagExpression(NebulaParser.Tag_expressionContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);

		if (ctx.type() != null)
		{
			return new TagAtom(span, (TypeNode) visit(ctx.type()));
		}
		if (ctx.tag_expression().size() == 1)
		{
			if (ctx.BANG() != null)
			{
				return new TagOperation(span, TagOperation.Operator.NOT, visitTagExpression(ctx.tag_expression(0)),
						null);
			}
			if (ctx.OPEN_PARENS() != null)
			{
				return visitTagExpression(ctx.tag_expression(0));
			}
		}
		if (ctx.tag_expression().size() == 2)
		{
			TagExpression left = visitTagExpression(ctx.tag_expression(0));
			TagExpression right = visitTagExpression(ctx.tag_expression(1));
			TagOperation.Operator op = (ctx.AMP() != null || ctx.getText().contains("&"))
					? TagOperation.Operator.INTERSECT
					: TagOperation.Operator.UNION;
			return new TagOperation(span, op, left, right);
		}
		return null;
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private Expression extractArgument(NebulaParser.ArgumentContext ctx)
	{
		if (ctx.namedArgument() != null)
			return (Expression) visit(ctx.namedArgument().expression());
		return (Expression) visit(ctx.positionalArgument().nonAssignmentExpression());
	}

	private ASTNode getMethodBody(NebulaParser.Method_bodyContext ctx)
	{
		if (ctx.expression_block() != null)
			return visit(ctx.expression_block());
		if (ctx.expression() != null)
			return visit(ctx.expression());
		return null;
	}

	private List<Parameter> getParameters(NebulaParser.ParametersContext ctx)
	{
		if (ctx.parameter_list() == null)
			return Collections.emptyList();
		List<Parameter> params = new ArrayList<>();
		for (var p : ctx.parameter_list().parameter())
		{
			// Extract CVT modifier (keeps/drops) if present
			CVTModifier cvtMod = CVTModifier.NONE;
			if (p.cvt_modifier() != null)
			{
				String modText = p.cvt_modifier().getText();
				cvtMod = CVTModifier.fromString(modText);
			}

			TypeNode t = (TypeNode) visit(p.type());
			String n = p.IDENTIFIER().getText();
			Expression def = p.expression() != null ? (Expression) visit(p.expression()) : null;
			params.add(new Parameter(cvtMod, t, n, def));
		}
		return params;
	}

	private List<Modifier> getModifiers(NebulaParser.ModifiersContext ctx)
	{
		if (ctx == null || ctx.children == null)
			return Collections.emptyList();
		List<Modifier> mods = new ArrayList<>();
		if (ctx.visibility_modifier() != null && !ctx.visibility_modifier().isEmpty())
		{
			mods.add(mapVisibility(ctx.visibility_modifier().getFirst()));
		}
		for (var child : ctx.children)
		{
			String txt = child.getText();
			if (txt.equals("static"))
				mods.add(Modifier.STATIC);
			if (txt.equals("override"))
				mods.add(Modifier.OVERRIDE);
		}
		return mods;
	}

	private Modifier mapVisibility(NebulaParser.Visibility_modifierContext ctx)
	{
		if (ctx == null)
			return Modifier.PRIVATE;
		if (ctx.PUBLIC() != null)
			return Modifier.PUBLIC;
		if (ctx.PROTECTED() != null)
			return Modifier.PROTECTED;
		return Modifier.PRIVATE;
	}

	private BinaryOperator mapBinaryOperator(String op)
	{
		return switch (op)
		{
			case "+" ->
					BinaryOperator.ADD;
			case "-" ->
					BinaryOperator.SUB;
			case "*" ->
					BinaryOperator.MUL;
			case "/" ->
					BinaryOperator.DIV;
			case "%" ->
					BinaryOperator.MOD;
			case "**" ->
					BinaryOperator.POW;
			case "&&" ->
					BinaryOperator.LOGICAL_AND;
			case "||" ->
					BinaryOperator.LOGICAL_OR;
			case "&" ->
					BinaryOperator.BIT_AND;
			case "|" ->
					BinaryOperator.BIT_OR;
			case "^" ->
					BinaryOperator.BIT_XOR;
			case "==" ->
					BinaryOperator.EQ;
			case "!=" ->
					BinaryOperator.NE;
			case "<" ->
					BinaryOperator.LT;
			case ">" ->
					BinaryOperator.GT;
			case "<=" ->
					BinaryOperator.LE;
			case ">=" ->
					BinaryOperator.GE;
			case "<<" ->
					BinaryOperator.SHL;
			case ">>" ->
					BinaryOperator.SHR;
			default ->
					throw new IllegalArgumentException("Unknown operator: " + op);
		};
	}

	private UnaryOperator mapUnaryOperator(String op)
	{
		return switch (op)
		{
			case "!" ->
					UnaryOperator.NOT;
			case "-" ->
					UnaryOperator.MINUS;
			case "+" ->
					UnaryOperator.PLUS;
			case "~" ->
					UnaryOperator.BIT_NOT;
			default ->
					throw new IllegalArgumentException("Unknown unary operator: " + op);
		};
	}
}