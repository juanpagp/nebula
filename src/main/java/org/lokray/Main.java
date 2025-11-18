package org.lokray;

import org.lokray.cli.NebcCli;

public class Main
{
	public static void main(String[] args)
	{
		// Delegate entirely to the CLI orchestrator
		NebcCli cli = new NebcCli();
		int exitCode = cli.execute(args);

		System.exit(exitCode);
	}
}