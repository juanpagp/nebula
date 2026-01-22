package org.nebula.nebc.ast;

import org.antlr.v4.runtime.tree.ParseTree;
import org.nebula.nebc.ast.declarations.VariableDeclaration;
import org.nebula.nebc.ast.declarations.VariableDeclarator;
import org.nebula.nebc.ast.expressions.Expression;
import org.nebula.nebc.ast.types.Type;
import org.nebula.nebc.frontend.diagnostics.SourceSpan;
import org.nebula.nebc.frontend.diagnostics.SourceUtil;
import org.nebula.nebc.frontend.parser.ParsingResult;
import org.nebula.nebc.frontend.parser.generated.NebulaParser;
import org.nebula.nebc.frontend.parser.generated.NebulaParserBaseVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility visitor to convert an ANTLR Parse Tree into a Nebula AST.
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

		for (NebulaParser.DirectiveContext dirCtx : ctx.directive())
		{
			directives.add(visit(dirCtx));
		}
		for (NebulaParser.Top_level_declarationContext declCtx : ctx.top_level_declaration())
		{
			declarations.add(visit(declCtx));
		}

		return new CompilationUnit(directives, declarations);
	}

	@Override
	public ASTNode visitVariable_declaration(NebulaParser.Variable_declarationContext ctx)
	{
		SourceSpan span = SourceUtil.createSpan(ctx, currentFileName);

		boolean isVar = ctx.VAR() != null;
		Type type = isVar ? null : (Type) visit(ctx.type());

		// 2. Collect all declarators (name + expression)
		List<VariableDeclarator> declarators = new ArrayList<>();
		for (NebulaParser.Variable_declaratorContext declCtx : ctx.variable_declarators().variable_declarator())
		{
			String name = declCtx.IDENTIFIER().getText();

			Expression initializer = null;
			if (declCtx.nonAssignmentExpression() != null)
			{
				initializer = (Expression) visit(declCtx.nonAssignmentExpression());
			}

			declarators.add(new VariableDeclarator(name, initializer));
		}

		return new VariableDeclaration(span, type, declarators, isVar);
	}
}