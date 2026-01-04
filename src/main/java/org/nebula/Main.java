package org.nebula;

import org.nebula.cli.NebcCli;
import org.nebula.util.ExitCode;

public class Main
{
	public static void main(String[] args)
	{
		// Delegate entirely to the CLI orchestrator
		NebcCli cli = new NebcCli();

		// Receive the type-safe Enum result
		ExitCode status = cli.execute(args);

		// Extract the integer value for the OS
		System.exit(status.getCode());
	}
}