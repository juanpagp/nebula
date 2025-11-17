parser grammar NebulaParser;

options {
	tokenVocab = NebulaLexer;
	language = JavaScript;
}

// The entry point, now including directives and namespace declarations
compilation_unit
	: directive* top_level_declaration* EOF
	;

// A new rule for all top-level directives (Import and Alias)
directive
	: import_directive
	| alias_directive
	;

//=============================================================================
// Namespace Directives (Import, Alias, Namespace)
// =============================================================================

// Handles 'import fully.qualified.Name;' Handles 'import static fully.qualified.Name.staticMember;'
import_directive
	: IMPORT STATIC? qualified_identifier (DOT IDENTIFIER)? SEMICOLON
	;

// Handles alias definitions: 'Console.println() = println();' (Syntax used for alias functions/methods)
// 'namespace.to.User = namespace.to.Usuario;' (Alias Type/Namespace)
alias_directive
	: ALIAS qualified_identifier ASSIGNMENT qualified_identifier SEMICOLON
	;

// Handles namespace declarations: 1. Block-scoped: 'namespace Foo { ... }' (Allows nesting) 2. File-scoped: 'namespace
// Foo;' (All subsequent declarations are in Foo) Note: Namespace nesting 'namespace Foo.Bar' is handled by
// qualified_identifier.
namespace_declaration
	: NAMESPACE qualified_identifier (
		OPEN_BRACE top_level_declaration* CLOSE_BRACE
		| SEMICOLON
	)
	;

// The full path for namespaces and types, e.g., 'fully.qualified.Name'
qualified_identifier
	: IDENTIFIER (DOT IDENTIFIER)*
	;

//=============================================================================
// Top-Level Declarations =============================================================================

top_level_declaration
	: const_declaration
	| method_declaration
	| class_declaration
	| struct_declaration
	| trait_declaration
	| union_declaration
	| namespace_declaration
	;

//=============================================================================
// Statements =============================================================================

statement
	: const_declaration
	| variable_declaration
	| expression_statement
	| block
	| if_statement
	| for_statement
	| foreach_statement
	| return_statement
	| match_statement
	;

expression_statement
	: expression SEMICOLON
	;

block
	: OPEN_BRACE statement* CLOSE_BRACE
	;

if_statement
	: IF parenthesized_expression statement (ELSE statement)?
	;

for_statement
	: FOR (traditional_for_control | parenthesized_expression) statement
	;

traditional_for_control
	: OPEN_PARENS for_initializer? SEMICOLON expression? SEMICOLON for_iterator? CLOSE_PARENS
	;

for_initializer
	: variable_declaration
	| expression_list
	;

for_iterator
	: expression_list
	;

foreach_statement
	: FOREACH foreach_control statement
	;

foreach_control
	: OPEN_PARENS (VAR | type) IDENTIFIER IN expression CLOSE_PARENS
	;

return_statement
	: RETURN expression? SEMICOLON
	;

match_statement
	: MATCH expression match_body
	;

match_body
	: OPEN_BRACE match_arm* CLOSE_BRACE
	;

match_arm
	: expression block
	;

//=============================================================================
// Member Declarations
//=============================================================================
const_declaration
	: CONST variable_declaration
	;

variable_declaration
    : modifiers (VAR | type) variable_declarators SEMICOLON
    ;

modifiers
	: (PUBLIC | PRIVATE | PROTECTED | STATIC | OVERRIDE)*
	;

field_declaration
	: type variable_declarators SEMICOLON
	;

variable_declarators
    : variable_declarator (COMMA variable_declarator)*
    ;

variable_declarator
    : IDENTIFIER (ASSIGNMENT expression)?
    ;

method_declaration
	: modifiers return_type IDENTIFIER parameters method_body
	;

constructor_declaration
	: IDENTIFIER parameters block
	;

parameters
	: OPEN_PARENS parameter_list? CLOSE_PARENS
	;

parameter_list
	: parameter (COMMA parameter)*
	;

parameter
	: type IDENTIFIER (ASSIGNMENT expression)?
	;

method_body
	: block
	| FAT_ARROW expression SEMICOLON
	| SEMICOLON
	;

operator_declaration
	: OPERATOR overloadable_operator parameters method_body
	;

overloadable_operator
	: PLUS
	| MINUS
	| STAR
	| '/'
	| PERCENT
	| '^'
	| AMP
	| '|'
	| LT LT
	| GT GT
	| OP_EQ
	| OP_NE
	;

//=============================================================================
// Type Definition Declarations =============================================================================

class_declaration
	: CLASS IDENTIFIER type_parameters? inheritance_clause? class_body
	;

class_body
	: OPEN_BRACE class_member* CLOSE_BRACE
	;

class_member
	: field_declaration
	| method_declaration
	| operator_declaration
	| constructor_declaration
	;

struct_declaration
	: STRUCT IDENTIFIER type_parameters? inheritance_clause? struct_body
	;

struct_body
	: OPEN_BRACE struct_member* CLOSE_BRACE
	;

struct_member
	: field_declaration
	| method_declaration
	| operator_declaration
	| constructor_declaration
	;

inheritance_clause
	: COLON type (COMMA type)*
	;

trait_declaration
	: TRAIT trait_signature FAT_ARROW expression SEMICOLON
	| TRAIT IDENTIFIER trait_block
	;

trait_signature
	: return_type IDENTIFIER DOT IDENTIFIER parameters
	;

trait_block
	: OPEN_BRACE trait_member* CLOSE_BRACE
	;

trait_member
	: method_declaration
	;

union_declaration
	: TAGGED UNION IDENTIFIER type_parameters? union_body
	;

union_body
	: OPEN_BRACE union_variant (COMMA union_variant)* COMMA? CLOSE_BRACE
	;

union_payload
	: OPEN_PARENS type? CLOSE_PARENS
	;

union_variant
	: IDENTIFIER union_payload?
	;

//=============================================================================
// Core Types and Generics =============================================================================

return_type
	: VOID
	| type
	;

// Includes the optional constraint (e.g., 'T: Shape')
type
	: (IDENTIFIER COLON)? (
		class_type rank_specifier*
		| predefined_type rank_specifier*
	)
	;

class_type
	: qualified_identifier type_argument_list?
	;

predefined_type
	: BOOL
	| CHAR
	| STRING
	| numeric_type
	;

numeric_type
	: integral_type
	| floating_point_type
	;

integral_type
	: BYTE
	| SBYTE
	| SHORT
	| USHORT
	| INT
	| UINT
	| INT8
	| UINT8
	| INT16
	| UINT16
	| INT32
	| UINT32
	| INT64
	| UINT64
	| LONG
	| ULONG
	;

floating_point_type
	: FLOAT
	| DOUBLE
	| DECIMAL
	;

rank_specifier
	: OPEN_BRACKET (expression)? COMMA* CLOSE_BRACKET
	;

generic_parameter
	: IDENTIFIER (COLON constraint)?
	;

constraint
	: type
	;

type_parameters
	: '<' generic_parameter (COMMA generic_parameter)* '>'
	;

type_argument_list
	: '<' type (COMMA type)* '>'
	;

//=============================================================================
// Expression Hierarchy
// =============================================================================
parenthesized_expression
	: OPEN_PARENS expression CLOSE_PARENS
	;

expression
	: assignment_expression
	;

assignment_expression
	: conditional_expression (assignment_operator assignment_expression)?
	;

assignment_operator
	: ASSIGNMENT
	| OP_ADD_ASSIGNMENT
	| OP_SUB_ASSIGNMENT
	| OP_MULT_ASSIGNMENT
	| OP_DIV_ASSIGNMENT
	| OP_MOD_ASSIGNMENT
	| OP_AND_ASSIGNMENT
	| OP_OR_ASSIGNMENT
	| OP_XOR_ASSIGNMENT
	| OP_LEFT_SHIFT_ASSIGNMENT
	;

conditional_expression
	: binary_or_expression ('?' expression ':' expression)?
	;

binary_or_expression
	: binary_and_expression (OP_OR binary_and_expression)*
	;

binary_and_expression
	: inclusive_or_expression (OP_AND inclusive_or_expression)*
	;

inclusive_or_expression
	: exclusive_or_expression ('|' exclusive_or_expression)*
	;

exclusive_or_expression
	: and_expression ('^' and_expression)*
	;

and_expression
	: equality_expression ('&' equality_expression)*
	;

equality_expression
	: relational_expression ((OP_EQ | OP_NE) relational_expression)*
	;

relational_expression
	: shift_expression (
		(LT | GT | OP_LE | OP_GE) shift_expression
		| IS type (IDENTIFIER)?
	)*
	;

shift_expression
	: additive_expression (
		(OP_LEFT_SHIFT | OP_RIGHT_SHIFT) additive_expression
	)*
	;

additive_expression
	: multiplicative_expression (('+' | '-') multiplicative_expression)*
	;

multiplicative_expression
	: exponentiation_expression (('*' | '/' | '%') exponentiation_expression)*
	;

exponentiation_expression
	: unary_expression ('**' exponentiation_expression)?
	;

unary_expression
	: ('+' | '-' | '!' | '~')* primary_expression
	;

primary_expression
	: primary_expression_start (postfix_operator)*
	;

primary_expression_start
	: literal
	| qualified_identifier type_argument_list?
	| parenthesized_expression
	| THIS
	| array_literal
	;

postfix_operator
	: DOT IDENTIFIER
	| OPEN_PARENS argument_list? CLOSE_PARENS
	| OPEN_BRACKET expression_list CLOSE_BRACKET
	;

array_literal
	: OPEN_BRACKET expression_list? CLOSE_BRACKET
	;

expression_list
	: expression (COMMA expression)*
	;

argument_list
	: argument (COMMA argument)*
	;

argument
	: (IDENTIFIER COLON)? expression
	;

// In NebulaParser.g4
literal
	: TRUE
	| FALSE
	| INTEGER_LITERAL
	| HEX_INTEGER_LITERAL
	| BIN_INTEGER_LITERAL
	| REAL_LITERAL
	| CHARACTER_LITERAL
	| string_literal
	| LITERAL_ACCESS
	;

string_literal
	: REGULAR_STRING
	| interpolated_regular_string
	;

interpolated_regular_string
	: INTERPOLATED_REGULAR_STRING_START interpolated_regular_string_part* DOUBLE_QUOTE_INSIDE
	;

interpolated_regular_string_part
	: interpolated_string_expression
	| DOUBLE_CURLY_INSIDE
	| REGULAR_CHAR_INSIDE
	| REGULAR_STRING_INSIDE
	;

interpolated_string_expression
	// This parses the {expression} and {expression:format} parts
	: expression (COMMA expression)* (COLON FORMAT_STRING+)?
	;