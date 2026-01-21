package org.nebula.nebc;

import org.antlr.v4.runtime.tree.ParseTree;
import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.CompilationUnit;

import java.util.List;

public class ASTBuilder
{
	/**
	 *
	 * @param trees - Always represents the root node, i.e.: compilation_unit
	 * @return the equivalent tree in the custom ASTNode format
	 */
	public static List<CompilationUnit> buildAST(List<ParseTree> trees)
	{
		for (ParseTree rootNode : trees)
		{
			System.out.println(rootNode.getText());
			System.out.println(rootNode.getChild(0).getChild(0));
		}

		return null;
	}
}
