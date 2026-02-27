// Generated from NebulaParser.g4 by ANTLR 4.13.1

    package org.nebula.nebc.frontend.parser.generated;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link NebulaParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface NebulaParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link NebulaParser#compilation_unit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilation_unit(NebulaParser.Compilation_unitContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#extern_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtern_declaration(NebulaParser.Extern_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#extern_member}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtern_member(NebulaParser.Extern_memberContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#use_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUse_statement(NebulaParser.Use_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#use_tail}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUse_tail(NebulaParser.Use_tailContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#use_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUse_alias(NebulaParser.Use_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#tag_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTag_statement(NebulaParser.Tag_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#tag_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTag_declaration(NebulaParser.Tag_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#tag_enumeration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTag_enumeration(NebulaParser.Tag_enumerationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#tag_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTag_expression(NebulaParser.Tag_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#namespace_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespace_declaration(NebulaParser.Namespace_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#qualified_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualified_name(NebulaParser.Qualified_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#enum_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_declaration(NebulaParser.Enum_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#enum_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnum_block(NebulaParser.Enum_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#top_level_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTop_level_declaration(NebulaParser.Top_level_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(NebulaParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#expression_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_statement(NebulaParser.Expression_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#expression_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_block(NebulaParser.Expression_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#statement_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement_block(NebulaParser.Statement_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#block_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock_statements(NebulaParser.Block_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#block_tail}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock_tail(NebulaParser.Block_tailContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#if_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_expression(NebulaParser.If_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#if_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_statement(NebulaParser.If_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#match_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatch_expression(NebulaParser.Match_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#for_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_statement(NebulaParser.For_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#traditional_for_control}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTraditional_for_control(NebulaParser.Traditional_for_controlContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#for_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_initializer(NebulaParser.For_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#for_iterator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_iterator(NebulaParser.For_iteratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#while_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_statement(NebulaParser.While_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#foreach_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeach_statement(NebulaParser.Foreach_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#foreach_control}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeach_control(NebulaParser.Foreach_controlContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#return_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_statement(NebulaParser.Return_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#match_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatch_body(NebulaParser.Match_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#match_arm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatch_arm(NebulaParser.Match_armContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern(NebulaParser.PatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#pattern_or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern_or(NebulaParser.Pattern_orContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#pattern_atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern_atom(NebulaParser.Pattern_atomContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#parenthesized_pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesized_pattern(NebulaParser.Parenthesized_patternContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#const_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConst_declaration(NebulaParser.Const_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#variable_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declaration(NebulaParser.Variable_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#visibility_modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVisibility_modifier(NebulaParser.Visibility_modifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#cvt_modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCvt_modifier(NebulaParser.Cvt_modifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#modifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifiers(NebulaParser.ModifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#field_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField_declaration(NebulaParser.Field_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#variable_declarators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declarators(NebulaParser.Variable_declaratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#variable_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declarator(NebulaParser.Variable_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#method_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_declaration(NebulaParser.Method_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#constructor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor_declaration(NebulaParser.Constructor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameters(NebulaParser.ParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_list(NebulaParser.Parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(NebulaParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#method_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_body(NebulaParser.Method_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#operator_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_declaration(NebulaParser.Operator_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#overloadable_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverloadable_operator(NebulaParser.Overloadable_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#class_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_declaration(NebulaParser.Class_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#class_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_body(NebulaParser.Class_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#class_member}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_member(NebulaParser.Class_memberContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#struct_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_declaration(NebulaParser.Struct_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#struct_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_body(NebulaParser.Struct_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#struct_member}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_member(NebulaParser.Struct_memberContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#inheritance_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInheritance_clause(NebulaParser.Inheritance_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#trait_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrait_declaration(NebulaParser.Trait_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#trait_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrait_body(NebulaParser.Trait_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#trait_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrait_block(NebulaParser.Trait_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#trait_member}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrait_member(NebulaParser.Trait_memberContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#union_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnion_declaration(NebulaParser.Union_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#union_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnion_body(NebulaParser.Union_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#union_payload}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnion_payload(NebulaParser.Union_payloadContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#union_variant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnion_variant(NebulaParser.Union_variantContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#return_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_type(NebulaParser.Return_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(NebulaParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#tuple_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTuple_type(NebulaParser.Tuple_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#tuple_type_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTuple_type_element(NebulaParser.Tuple_type_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#class_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_type(NebulaParser.Class_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#predefined_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredefined_type(NebulaParser.Predefined_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#numeric_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_type(NebulaParser.Numeric_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#integral_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegral_type(NebulaParser.Integral_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#floating_point_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloating_point_type(NebulaParser.Floating_point_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#rank_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRank_specifier(NebulaParser.Rank_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#generic_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_parameter(NebulaParser.Generic_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint(NebulaParser.ConstraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#type_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_parameters(NebulaParser.Type_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#type_argument_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_argument_list(NebulaParser.Type_argument_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#nested_gt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNested_gt(NebulaParser.Nested_gtContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#tuple_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTuple_literal(NebulaParser.Tuple_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#parenthesized_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesized_expression(NebulaParser.Parenthesized_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#nonAssignmentExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonAssignmentExpression(NebulaParser.NonAssignmentExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(NebulaParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#assignment_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_expression(NebulaParser.Assignment_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#new_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_expression(NebulaParser.New_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#assignment_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_operator(NebulaParser.Assignment_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#binary_or_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinary_or_expression(NebulaParser.Binary_or_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#binary_and_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinary_and_expression(NebulaParser.Binary_and_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#inclusive_or_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInclusive_or_expression(NebulaParser.Inclusive_or_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#exclusive_or_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExclusive_or_expression(NebulaParser.Exclusive_or_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#and_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnd_expression(NebulaParser.And_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#equality_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEquality_expression(NebulaParser.Equality_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#relational_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_expression(NebulaParser.Relational_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#shift_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShift_expression(NebulaParser.Shift_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#left_shift}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLeft_shift(NebulaParser.Left_shiftContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#right_shift}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRight_shift(NebulaParser.Right_shiftContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#additive_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditive_expression(NebulaParser.Additive_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#multiplicative_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicative_expression(NebulaParser.Multiplicative_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#exponentiation_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExponentiation_expression(NebulaParser.Exponentiation_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#unary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_expression(NebulaParser.Unary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#cast_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast_expression(NebulaParser.Cast_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#primary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_expression(NebulaParser.Primary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#primary_expression_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_expression_start(NebulaParser.Primary_expression_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#postfix_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfix_operator(NebulaParser.Postfix_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#array_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_literal(NebulaParser.Array_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#expression_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression_list(NebulaParser.Expression_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(NebulaParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#argument_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument_list(NebulaParser.Argument_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#namedArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedArgument(NebulaParser.NamedArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#positionalArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionalArgument(NebulaParser.PositionalArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(NebulaParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(NebulaParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#string_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_literal(NebulaParser.String_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#interpolated_regular_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolated_regular_string(NebulaParser.Interpolated_regular_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#interpolated_regular_string_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolated_regular_string_part(NebulaParser.Interpolated_regular_string_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link NebulaParser#interpolated_string_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolated_string_expression(NebulaParser.Interpolated_string_expressionContext ctx);
}