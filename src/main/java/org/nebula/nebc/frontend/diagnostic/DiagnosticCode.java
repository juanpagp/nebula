package org.nebula.nebc.frontend.diagnostic;

public enum DiagnosticCode
{
	// --- General & Entry Point ---
	MISSING_MAIN_METHOD("No 'main' entry point found."),
	DUPLICATE_MAIN_METHOD("Duplicate 'main' method. Entry point already defined."),
	INVALID_MAIN_SIGNATURE("'main' must return 'i32' or 'void', and have no parameters."),

	// --- Symbols & Types ---
	UNDEFINED_SYMBOL("Undefined symbol '%s'."),
	DUPLICATE_SYMBOL("Symbol '%s' is already defined."),
	UNKNOWN_TYPE("Unknown type '%s'."),
	TYPE_ALREADY_DEFINED("Type '%s' is already defined."),
	TYPE_MISMATCH("Type mismatch. Expected '%s', got '%s'."),
	NOT_CALLABLE("Expression of type '%s' is not callable."),
	NOT_HAS_MEMBERS("Type '%s' does not have members."),
	MEMBER_NOT_FOUND("Member '%s' not found in type '%s'."),
	DUPLICATE_PARAMETER("Duplicate parameter '%s'."),

	// --- Syntax & Structural ---
	RETURN_OUTSIDE_METHOD("Return statement outside of method body."),
	IF_CONDITION_NOT_BOOL("If condition must be boolean, got '%s'."),
	FOR_CONDITION_NOT_BOOL("For condition must be boolean, got '%s'."),
	WHILE_CONDITION_NOT_BOOL("While condition must be boolean, got '%s'."),
	OPERATOR_NOT_DEFINED("Operator '%s' not defined for '%s' and '%s'."),
	// Logical & Relational
	COMPARING_DISTINCT_TYPES("Comparing distinct types '%s' and '%s' is always false."),
	RELATIONAL_NUMERIC("Relational operator requires numeric types."),
	LOGICAL_BOOLEAN("Logical operators require boolean operands."),

	// Internal
	INTERNAL_ERROR("Internal compiler error: %s"),
	NO_MEMBERS("Type '%s' does not have members."),
	UNARY_MATH_NUMERIC("Unary math requires numeric operand."),
	UNARY_NOT_BOOLEAN("Operator '!' requires boolean operand."),

	// --- Function & Method ---
	ARGUMENT_COUNT_MISMATCH("Argument count mismatch. Expected %d, got %d."),
	ARGUMENT_TYPE_MISMATCH("Argument %d: expected '%s', got '%s'."),
	UNINITIALIZED_VARIABLE("Implicit variable '%s' must be initialized."),

	// --- FFI & I/O ---
	EXTERN_METHOD_HAS_BODY("Method '%s' is declared in an 'extern' block and cannot have a body.");

	private final String messageTemplate;

	DiagnosticCode(String messageTemplate)
	{
		this.messageTemplate = messageTemplate;
	}

	public String format(Object... args)
	{
		return String.format(messageTemplate, args);
	}
}
