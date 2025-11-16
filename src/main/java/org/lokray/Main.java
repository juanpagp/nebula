package org.lokray;

import org.lokray.nebc.core.Compiler;
import org.lokray.nebc.core.CompilerConfig;

public class Main
{
	public static void main(String[] args)
	{
		CompilerConfig config = CompilerConfig.fromArgs(args);

		Compiler compilerPipeline = new Compiler(config);
		compilerPipeline.run();
	}
}