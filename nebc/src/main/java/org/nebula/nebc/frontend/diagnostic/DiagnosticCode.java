package org.nebula.nebc.frontend.diagnostic;

public enum DiagnosticCode
{
	// --- General & Entry Point ---
	MISSING_MAIN_METHOD("No 'main' entry point found."), DUPLICATE_MAIN_METHOD("Duplicate 'main' method. Entry point already defined."), INVALID_MAIN_SIGNATURE("'main' must return 'i32' or 'void', and take no parameters or exactly '(str[] args)'."),

	// --- Symbols & Types ---
	UNDEFINED_SYMBOL("Undefined symbol '%s'."), DUPLICATE_SYMBOL("Symbol '%s' is already defined."), UNKNOWN_TYPE("Unknown type '%s'."), TYPE_ALREADY_DEFINED("Type '%s' is already defined."), TYPE_MISMATCH("Type mismatch. Expected '%s', got '%s'."), NOT_CALLABLE("Expression of type '%s' is not callable."), NOT_HAS_MEMBERS("Type '%s' does not have members."), MEMBER_NOT_FOUND("Member '%s' not found in type '%s'."), DUPLICATE_PARAMETER("Duplicate parameter '%s'."),
	UNQUALIFIED_ENUM_VARIANT("Enum variant '%s' must be qualified as '%s::%s', or import variants with 'use %s::*'."),

	// --- Syntax & Structural ---
	RETURN_OUTSIDE_METHOD("Return statement outside of method body."), THIS_OUTSIDE_TYPE("'this' expression outside of a type or impl block."), IF_CONDITION_NOT_BOOL("If condition must be boolean, got '%s'."), FOR_CONDITION_NOT_BOOL("For condition must be boolean, got '%s'."), WHILE_CONDITION_NOT_BOOL("While condition must be boolean, got '%s'."), OPERATOR_NOT_DEFINED("Operator '%s' not defined for '%s' and '%s'."),
	// Logical & Relational
	COMPARING_DISTINCT_TYPES("Comparing distinct types '%s' and '%s' is always false."), RELATIONAL_NUMERIC("Relational operator requires numeric types."), LOGICAL_BOOLEAN("Logical operators require boolean operands."),

	// Internal
	INTERNAL_ERROR("Internal compiler error: %s"), NO_MEMBERS("Type '%s' does not have members."), UNARY_MATH_NUMERIC("Unary math requires numeric operand."), UNARY_NOT_BOOLEAN("Operator '!' requires boolean operand."),

	// --- Function & Method ---
	ARGUMENT_COUNT_MISMATCH("Argument count mismatch. Expected %d, got %d."), ARGUMENT_TYPE_MISMATCH("Argument %d: expected '%s', got '%s'."), UNINITIALIZED_VARIABLE("Implicit variable '%s' must be initialized."),

	// --- Arrays & Indexing ---
	INDEX_NOT_INTEGER("Array index must be an integer, got '%s'."), TYPE_NOT_INDEXABLE("Type '%s' is not indexable."), ARRAY_LITERAL_MISMATCH("Array literal element type mismatch. Expected '%s', got '%s'."),

	// --- FFI & I/O ---
	EXTERN_METHOD_HAS_BODY("Method '%s' is declared in an 'extern' block and cannot have a body."),

	// --- Control-flow ---
	INVALID_BREAK_OUTSIDE_LOOP("'%s' statement used outside of a loop."),

	// --- Optionals ---
	FORCED_UNWRAP_ON_NON_OPTIONAL("Forced unwrap '!' applied to non-optional type '%s'."),
	UNSAFE_MEMBER_ACCESS_ON_OPTIONAL("Optional type '%s' requires safe access '?.' or explicit unwrap before member access."),
	NONE_ASSIGNED_TO_NON_OPTIONAL("'none' cannot be assigned to non-optional type '%s'."),

	// --- Structs ---
	STRUCT_MISSING_CONSTRUCTOR("Struct '%s' has no constructor. All fields must be explicitly initialized via a constructor."),

	// --- Tags ---
	TAG_AS_VALUE_TYPE("Tag '%s' cannot be used as a value type. Tags are compile-time-only constraints; use '<T: %s>' in a generic."),
	TAG_IN_PARAM_POSITION("Parameter type is tag '%s'. Use the generic type parameter instead: '<T: %s>(T param)'."),
	TAG_CYCLE("Circular tag definition detected: tag '%s' references itself (directly or indirectly)."),
	TAG_IMPL_OVERLAP("Overlapping 'impl %s': type '%s' is already covered by a previous impl for this tag."),

	// --- Match / Exhaustiveness ---
	NON_EXHAUSTIVE_MATCH("Non-exhaustive match on type '%s'. Missing patterns: %s."),
	UNKNOWN_VARIANT("'%s' is not a variant of '%s'."),

	// --- Use ---
	USE_ITEM_NOT_FOUND("'%s' is not a member of '%s'."),

	// --- CVT (Causal Validity Tracking) ---
	CVT_USE_AFTER_DROP("Variable '%s' used after its region was consumed by a 'drops' call."),
	CVT_KEEPS_PARAM_PASSED_TO_DROPS("Parameter '%s' is declared 'keeps' but is being passed to a 'drops' function. This violates the caller's guarantee that the region will remain valid."),
	BARE_FIELD_ACCESS("Field '%s' must be accessed via 'this.%s' inside a method body.");

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
