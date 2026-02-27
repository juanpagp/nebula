package org.nebula.nebc.frontend.parser;

import org.antlr.v4.runtime.tree.ParseTree;
import org.nebula.nebc.io.SourceFile;

public record ParsingResult(SourceFile file, ParseTree compilationUnitRoot)
{
}
