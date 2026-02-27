package org.nebula.nebc.codegen;

/**
 * Unchecked exception for fatal LLVM code generation errors.
 * Thrown when the generated module fails verification or when LLVM
 * target/machine initialisation fails unrecoverably.
 */
public class CodegenException extends RuntimeException
{

	public CodegenException(String message)
	{
		super(message);
	}

	public CodegenException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
