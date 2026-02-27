// Generated from NebulaParser.g4 by ANTLR 4.13.1

    package org.nebula.nebc.frontend.parser.generated;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class NebulaParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		BYTE_ORDER_MARK=1, SINGLE_LINE_DOC_COMMENT=2, EMPTY_DELIMITED_DOC_COMMENT=3, 
		DELIMITED_DOC_COMMENT=4, SINGLE_LINE_COMMENT=5, DELIMITED_COMMENT=6, WHITESPACES=7, 
		AS=8, ASYNC=9, AWAIT=10, BOOL=11, BREAK=12, CASE=13, CHAR=14, CLASS=15, 
		CONST=16, CONTINUE=17, DECIMAL=18, DEFAULT=19, DROPS=20, ELSE=21, ENUM=22, 
		EXTERN=23, FALSE=24, F32=25, F64=26, FOR=27, FOREACH=28, IF=29, IN=30, 
		INT8=31, INT16=32, INT32=33, INT64=34, INTERFACE=35, IMPORT=36, IS=37, 
		KEEPS=38, NAMESPACE=39, NEW=40, MATCH=41, OPERATOR=42, OVERRIDE=43, PRIVATE=44, 
		PROTECTED=45, PUBLIC=46, RETURN=47, STATIC=48, STRING=49, STRUCT=50, SWITCH=51, 
		TAG=52, TAGGED=53, THIS=54, TRAIT=55, TRUE=56, UINT8=57, UINT16=58, UINT32=59, 
		UINT64=60, UNION=61, USE=62, VAR=63, VOID=64, WHILE=65, UNDERSCORE=66, 
		IDENTIFIER=67, LITERAL_ACCESS=68, INTEGER_LITERAL=69, HEX_INTEGER_LITERAL=70, 
		BIN_INTEGER_LITERAL=71, REAL_LITERAL=72, CHARACTER_LITERAL=73, REGULAR_STRING=74, 
		VERBATIUM_STRING=75, INTERPOLATED_REGULAR_STRING_START=76, INTERPOLATED_VERBATIUM_STRING_START=77, 
		OPEN_BRACE=78, CLOSE_BRACE=79, OPEN_BRACKET=80, CLOSE_BRACKET=81, OPEN_PARENS=82, 
		CLOSE_PARENS=83, DOT=84, COMMA=85, DOUBLE_COLON=86, COLON=87, SEMICOLON=88, 
		PLUS=89, MINUS=90, STAR=91, POW=92, DIV=93, PERCENT=94, AMP=95, PIPE=96, 
		CARET=97, BANG=98, TILDE=99, ASSIGNMENT=100, LT=101, GT=102, INTERR=103, 
		OP_COALESCING=104, OP_INC=105, OP_DEC=106, OP_AND=107, OP_OR=108, OP_EQ=109, 
		OP_NE=110, OP_LE=111, OP_GE=112, OP_ADD_ASSIGNMENT=113, OP_SUB_ASSIGNMENT=114, 
		OP_MULT_ASSIGNMENT=115, OP_POW_ASSIGNMENT=116, OP_DIV_ASSIGNMENT=117, 
		OP_MOD_ASSIGNMENT=118, OP_AND_ASSIGNMENT=119, OP_OR_ASSIGNMENT=120, OP_XOR_ASSIGNMENT=121, 
		OP_LEFT_SHIFT_ASSIGNMENT=122, OP_RANGE=123, FAT_ARROW=124, DOUBLE_CURLY_INSIDE=125, 
		OPEN_BRACE_INSIDE=126, REGULAR_CHAR_INSIDE=127, REGULAR_STRING_INSIDE=128, 
		VERBATIUM_DOUBLE_QUOTE_INSIDE=129, VERBATIUM_INSIDE_STRING=130, DOUBLE_QUOTE_INSIDE=131, 
		CLOSE_BRACE_INSIDE=132, FORMAT_STRING=133, DOUBLE_CURLY_CLOSE_INSIDE=134;
	public static final int
		RULE_compilation_unit = 0, RULE_extern_declaration = 1, RULE_extern_member = 2, 
		RULE_use_statement = 3, RULE_use_tail = 4, RULE_use_alias = 5, RULE_tag_statement = 6, 
		RULE_tag_declaration = 7, RULE_tag_enumeration = 8, RULE_tag_expression = 9, 
		RULE_namespace_declaration = 10, RULE_qualified_name = 11, RULE_enum_declaration = 12, 
		RULE_enum_block = 13, RULE_top_level_declaration = 14, RULE_statement = 15, 
		RULE_expression_statement = 16, RULE_expression_block = 17, RULE_statement_block = 18, 
		RULE_block_statements = 19, RULE_block_tail = 20, RULE_if_expression = 21, 
		RULE_if_statement = 22, RULE_match_expression = 23, RULE_for_statement = 24, 
		RULE_traditional_for_control = 25, RULE_for_initializer = 26, RULE_for_iterator = 27, 
		RULE_while_statement = 28, RULE_foreach_statement = 29, RULE_foreach_control = 30, 
		RULE_return_statement = 31, RULE_match_body = 32, RULE_match_arm = 33, 
		RULE_pattern = 34, RULE_pattern_or = 35, RULE_pattern_atom = 36, RULE_parenthesized_pattern = 37, 
		RULE_const_declaration = 38, RULE_variable_declaration = 39, RULE_visibility_modifier = 40, 
		RULE_cvt_modifier = 41, RULE_modifiers = 42, RULE_field_declaration = 43, 
		RULE_variable_declarators = 44, RULE_variable_declarator = 45, RULE_method_declaration = 46, 
		RULE_constructor_declaration = 47, RULE_parameters = 48, RULE_parameter_list = 49, 
		RULE_parameter = 50, RULE_method_body = 51, RULE_operator_declaration = 52, 
		RULE_overloadable_operator = 53, RULE_class_declaration = 54, RULE_class_body = 55, 
		RULE_class_member = 56, RULE_struct_declaration = 57, RULE_struct_body = 58, 
		RULE_struct_member = 59, RULE_inheritance_clause = 60, RULE_trait_declaration = 61, 
		RULE_trait_body = 62, RULE_trait_block = 63, RULE_trait_member = 64, RULE_union_declaration = 65, 
		RULE_union_body = 66, RULE_union_payload = 67, RULE_union_variant = 68, 
		RULE_return_type = 69, RULE_type = 70, RULE_tuple_type = 71, RULE_tuple_type_element = 72, 
		RULE_class_type = 73, RULE_predefined_type = 74, RULE_numeric_type = 75, 
		RULE_integral_type = 76, RULE_floating_point_type = 77, RULE_rank_specifier = 78, 
		RULE_generic_parameter = 79, RULE_constraint = 80, RULE_type_parameters = 81, 
		RULE_type_argument_list = 82, RULE_nested_gt = 83, RULE_tuple_literal = 84, 
		RULE_parenthesized_expression = 85, RULE_nonAssignmentExpression = 86, 
		RULE_expression = 87, RULE_assignment_expression = 88, RULE_new_expression = 89, 
		RULE_assignment_operator = 90, RULE_binary_or_expression = 91, RULE_binary_and_expression = 92, 
		RULE_inclusive_or_expression = 93, RULE_exclusive_or_expression = 94, 
		RULE_and_expression = 95, RULE_equality_expression = 96, RULE_relational_expression = 97, 
		RULE_shift_expression = 98, RULE_left_shift = 99, RULE_right_shift = 100, 
		RULE_additive_expression = 101, RULE_multiplicative_expression = 102, 
		RULE_exponentiation_expression = 103, RULE_unary_expression = 104, RULE_cast_expression = 105, 
		RULE_primary_expression = 106, RULE_primary_expression_start = 107, RULE_postfix_operator = 108, 
		RULE_array_literal = 109, RULE_expression_list = 110, RULE_arguments = 111, 
		RULE_argument_list = 112, RULE_namedArgument = 113, RULE_positionalArgument = 114, 
		RULE_argument = 115, RULE_literal = 116, RULE_string_literal = 117, RULE_interpolated_regular_string = 118, 
		RULE_interpolated_regular_string_part = 119, RULE_interpolated_string_expression = 120;
	private static String[] makeRuleNames() {
		return new String[] {
			"compilation_unit", "extern_declaration", "extern_member", "use_statement", 
			"use_tail", "use_alias", "tag_statement", "tag_declaration", "tag_enumeration", 
			"tag_expression", "namespace_declaration", "qualified_name", "enum_declaration", 
			"enum_block", "top_level_declaration", "statement", "expression_statement", 
			"expression_block", "statement_block", "block_statements", "block_tail", 
			"if_expression", "if_statement", "match_expression", "for_statement", 
			"traditional_for_control", "for_initializer", "for_iterator", "while_statement", 
			"foreach_statement", "foreach_control", "return_statement", "match_body", 
			"match_arm", "pattern", "pattern_or", "pattern_atom", "parenthesized_pattern", 
			"const_declaration", "variable_declaration", "visibility_modifier", "cvt_modifier", 
			"modifiers", "field_declaration", "variable_declarators", "variable_declarator", 
			"method_declaration", "constructor_declaration", "parameters", "parameter_list", 
			"parameter", "method_body", "operator_declaration", "overloadable_operator", 
			"class_declaration", "class_body", "class_member", "struct_declaration", 
			"struct_body", "struct_member", "inheritance_clause", "trait_declaration", 
			"trait_body", "trait_block", "trait_member", "union_declaration", "union_body", 
			"union_payload", "union_variant", "return_type", "type", "tuple_type", 
			"tuple_type_element", "class_type", "predefined_type", "numeric_type", 
			"integral_type", "floating_point_type", "rank_specifier", "generic_parameter", 
			"constraint", "type_parameters", "type_argument_list", "nested_gt", "tuple_literal", 
			"parenthesized_expression", "nonAssignmentExpression", "expression", 
			"assignment_expression", "new_expression", "assignment_operator", "binary_or_expression", 
			"binary_and_expression", "inclusive_or_expression", "exclusive_or_expression", 
			"and_expression", "equality_expression", "relational_expression", "shift_expression", 
			"left_shift", "right_shift", "additive_expression", "multiplicative_expression", 
			"exponentiation_expression", "unary_expression", "cast_expression", "primary_expression", 
			"primary_expression_start", "postfix_operator", "array_literal", "expression_list", 
			"arguments", "argument_list", "namedArgument", "positionalArgument", 
			"argument", "literal", "string_literal", "interpolated_regular_string", 
			"interpolated_regular_string_part", "interpolated_string_expression"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'\\u00EF\\u00BB\\u00BF'", null, "'/***/'", null, null, null, null, 
			"'as'", "'async'", "'await'", "'bool'", "'break'", "'case'", "'char'", 
			"'class'", "'const'", "'continue'", "'decimal'", "'default'", "'drops'", 
			"'else'", "'enum'", "'extern'", "'false'", "'f32'", "'f64'", "'for'", 
			"'foreach'", "'if'", "'in'", "'i8'", "'i16'", "'i32'", "'i64'", "'interface'", 
			"'import'", "'is'", "'keeps'", "'namespace'", "'new'", "'match'", "'operator'", 
			"'override'", "'private'", "'protected'", "'public'", "'return'", "'static'", 
			"'string'", "'struct'", "'switch'", "'tag'", "'tagged'", "'this'", "'trait'", 
			"'true'", "'u8'", "'u16'", "'u32'", "'u64'", "'union'", "'use'", "'var'", 
			"'void'", "'while'", "'_'", null, null, null, null, null, null, null, 
			null, null, null, null, "'{'", "'}'", "'['", "']'", "'('", "')'", "'.'", 
			"','", "'::'", "':'", "';'", "'+'", "'-'", "'*'", "'**'", "'/'", "'%'", 
			"'&'", "'|'", "'^'", "'!'", "'~'", "'='", "'<'", "'>'", "'?'", "'??'", 
			"'++'", "'--'", "'&&'", "'||'", "'=='", "'!='", "'<='", "'>='", "'+='", 
			"'-='", "'*='", "'**='", "'/='", "'%='", "'&='", "'|='", "'^='", "'<<='", 
			"'..'", "'=>'", "'{{'", null, null, null, null, null, null, null, null, 
			"'}}'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "BYTE_ORDER_MARK", "SINGLE_LINE_DOC_COMMENT", "EMPTY_DELIMITED_DOC_COMMENT", 
			"DELIMITED_DOC_COMMENT", "SINGLE_LINE_COMMENT", "DELIMITED_COMMENT", 
			"WHITESPACES", "AS", "ASYNC", "AWAIT", "BOOL", "BREAK", "CASE", "CHAR", 
			"CLASS", "CONST", "CONTINUE", "DECIMAL", "DEFAULT", "DROPS", "ELSE", 
			"ENUM", "EXTERN", "FALSE", "F32", "F64", "FOR", "FOREACH", "IF", "IN", 
			"INT8", "INT16", "INT32", "INT64", "INTERFACE", "IMPORT", "IS", "KEEPS", 
			"NAMESPACE", "NEW", "MATCH", "OPERATOR", "OVERRIDE", "PRIVATE", "PROTECTED", 
			"PUBLIC", "RETURN", "STATIC", "STRING", "STRUCT", "SWITCH", "TAG", "TAGGED", 
			"THIS", "TRAIT", "TRUE", "UINT8", "UINT16", "UINT32", "UINT64", "UNION", 
			"USE", "VAR", "VOID", "WHILE", "UNDERSCORE", "IDENTIFIER", "LITERAL_ACCESS", 
			"INTEGER_LITERAL", "HEX_INTEGER_LITERAL", "BIN_INTEGER_LITERAL", "REAL_LITERAL", 
			"CHARACTER_LITERAL", "REGULAR_STRING", "VERBATIUM_STRING", "INTERPOLATED_REGULAR_STRING_START", 
			"INTERPOLATED_VERBATIUM_STRING_START", "OPEN_BRACE", "CLOSE_BRACE", "OPEN_BRACKET", 
			"CLOSE_BRACKET", "OPEN_PARENS", "CLOSE_PARENS", "DOT", "COMMA", "DOUBLE_COLON", 
			"COLON", "SEMICOLON", "PLUS", "MINUS", "STAR", "POW", "DIV", "PERCENT", 
			"AMP", "PIPE", "CARET", "BANG", "TILDE", "ASSIGNMENT", "LT", "GT", "INTERR", 
			"OP_COALESCING", "OP_INC", "OP_DEC", "OP_AND", "OP_OR", "OP_EQ", "OP_NE", 
			"OP_LE", "OP_GE", "OP_ADD_ASSIGNMENT", "OP_SUB_ASSIGNMENT", "OP_MULT_ASSIGNMENT", 
			"OP_POW_ASSIGNMENT", "OP_DIV_ASSIGNMENT", "OP_MOD_ASSIGNMENT", "OP_AND_ASSIGNMENT", 
			"OP_OR_ASSIGNMENT", "OP_XOR_ASSIGNMENT", "OP_LEFT_SHIFT_ASSIGNMENT", 
			"OP_RANGE", "FAT_ARROW", "DOUBLE_CURLY_INSIDE", "OPEN_BRACE_INSIDE", 
			"REGULAR_CHAR_INSIDE", "REGULAR_STRING_INSIDE", "VERBATIUM_DOUBLE_QUOTE_INSIDE", 
			"VERBATIUM_INSIDE_STRING", "DOUBLE_QUOTE_INSIDE", "CLOSE_BRACE_INSIDE", 
			"FORMAT_STRING", "DOUBLE_CURLY_CLOSE_INSIDE"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "NebulaParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public NebulaParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Compilation_unitContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(NebulaParser.EOF, 0); }
		public List<Top_level_declarationContext> top_level_declaration() {
			return getRuleContexts(Top_level_declarationContext.class);
		}
		public Top_level_declarationContext top_level_declaration(int i) {
			return getRuleContext(Top_level_declarationContext.class,i);
		}
		public Compilation_unitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compilation_unit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterCompilation_unit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitCompilation_unit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitCompilation_unit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Compilation_unitContext compilation_unit() throws RecognitionException {
		Compilation_unitContext _localctx = new Compilation_unitContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_compilation_unit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(245);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 9130899292993996800L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 262153L) != 0)) {
				{
				{
				setState(242);
				top_level_declaration();
				}
				}
				setState(247);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(248);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Extern_declarationContext extends ParserRuleContext {
		public TerminalNode EXTERN() { return getToken(NebulaParser.EXTERN, 0); }
		public TerminalNode REGULAR_STRING() { return getToken(NebulaParser.REGULAR_STRING, 0); }
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public ModifiersContext modifiers() {
			return getRuleContext(ModifiersContext.class,0);
		}
		public List<Extern_memberContext> extern_member() {
			return getRuleContexts(Extern_memberContext.class);
		}
		public Extern_memberContext extern_member(int i) {
			return getRuleContext(Extern_memberContext.class,i);
		}
		public Extern_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extern_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterExtern_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitExtern_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitExtern_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Extern_declarationContext extern_declaration() throws RecognitionException {
		Extern_declarationContext _localctx = new Extern_declarationContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_extern_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(251);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(250);
				modifiers();
				}
				break;
			}
			setState(253);
			match(EXTERN);
			setState(254);
			match(REGULAR_STRING);
			setState(255);
			match(OPEN_BRACE);
			setState(259);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2162704219776501760L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 262153L) != 0)) {
				{
				{
				setState(256);
				extern_member();
				}
				}
				setState(261);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(262);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Extern_memberContext extends ParserRuleContext {
		public Method_declarationContext method_declaration() {
			return getRuleContext(Method_declarationContext.class,0);
		}
		public Extern_memberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extern_member; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterExtern_member(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitExtern_member(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitExtern_member(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Extern_memberContext extern_member() throws RecognitionException {
		Extern_memberContext _localctx = new Extern_memberContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_extern_member);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(264);
			method_declaration();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Use_statementContext extends ParserRuleContext {
		public TerminalNode USE() { return getToken(NebulaParser.USE, 0); }
		public Qualified_nameContext qualified_name() {
			return getRuleContext(Qualified_nameContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(NebulaParser.SEMICOLON, 0); }
		public Use_tailContext use_tail() {
			return getRuleContext(Use_tailContext.class,0);
		}
		public Use_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_use_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterUse_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitUse_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitUse_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Use_statementContext use_statement() throws RecognitionException {
		Use_statementContext _localctx = new Use_statementContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_use_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(266);
			match(USE);
			setState(267);
			qualified_name();
			setState(269);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS || _la==DOUBLE_COLON) {
				{
				setState(268);
				use_tail();
				}
			}

			setState(271);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Use_tailContext extends ParserRuleContext {
		public TerminalNode DOUBLE_COLON() { return getToken(NebulaParser.DOUBLE_COLON, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Use_aliasContext use_alias() {
			return getRuleContext(Use_aliasContext.class,0);
		}
		public Use_tailContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_use_tail; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterUse_tail(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitUse_tail(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitUse_tail(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Use_tailContext use_tail() throws RecognitionException {
		Use_tailContext _localctx = new Use_tailContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_use_tail);
		int _la;
		try {
			setState(279);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOUBLE_COLON:
				enterOuterAlt(_localctx, 1);
				{
				setState(273);
				match(DOUBLE_COLON);
				setState(274);
				match(IDENTIFIER);
				setState(276);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(275);
					use_alias();
					}
				}

				}
				break;
			case AS:
				enterOuterAlt(_localctx, 2);
				{
				setState(278);
				use_alias();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Use_aliasContext extends ParserRuleContext {
		public TerminalNode AS() { return getToken(NebulaParser.AS, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Use_aliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_use_alias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterUse_alias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitUse_alias(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitUse_alias(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Use_aliasContext use_alias() throws RecognitionException {
		Use_aliasContext _localctx = new Use_aliasContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_use_alias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(281);
			match(AS);
			setState(282);
			match(IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Tag_statementContext extends ParserRuleContext {
		public TerminalNode TAG() { return getToken(NebulaParser.TAG, 0); }
		public Tag_declarationContext tag_declaration() {
			return getRuleContext(Tag_declarationContext.class,0);
		}
		public TerminalNode AS() { return getToken(NebulaParser.AS, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public TerminalNode SEMICOLON() { return getToken(NebulaParser.SEMICOLON, 0); }
		public Visibility_modifierContext visibility_modifier() {
			return getRuleContext(Visibility_modifierContext.class,0);
		}
		public Tag_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tag_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTag_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTag_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTag_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Tag_statementContext tag_statement() throws RecognitionException {
		Tag_statementContext _localctx = new Tag_statementContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_tag_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(285);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 123145302310912L) != 0)) {
				{
				setState(284);
				visibility_modifier();
				}
			}

			setState(287);
			match(TAG);
			setState(288);
			tag_declaration();
			setState(289);
			match(AS);
			setState(290);
			match(IDENTIFIER);
			setState(291);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Tag_declarationContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public Tag_enumerationContext tag_enumeration() {
			return getRuleContext(Tag_enumerationContext.class,0);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public Tag_expressionContext tag_expression() {
			return getRuleContext(Tag_expressionContext.class,0);
		}
		public Tag_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tag_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTag_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTag_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTag_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Tag_declarationContext tag_declaration() throws RecognitionException {
		Tag_declarationContext _localctx = new Tag_declarationContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_tag_declaration);
		try {
			setState(302);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(293);
				type();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(294);
				match(OPEN_BRACE);
				setState(295);
				tag_enumeration();
				setState(296);
				match(CLOSE_BRACE);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(298);
				match(OPEN_BRACE);
				setState(299);
				tag_expression(0);
				setState(300);
				match(CLOSE_BRACE);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Tag_enumerationContext extends ParserRuleContext {
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Tag_enumerationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tag_enumeration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTag_enumeration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTag_enumeration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTag_enumeration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Tag_enumerationContext tag_enumeration() throws RecognitionException {
		Tag_enumerationContext _localctx = new Tag_enumerationContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_tag_enumeration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(304);
			type();
			setState(309);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(305);
				match(COMMA);
				setState(306);
				type();
				}
				}
				setState(311);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Tag_expressionContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public List<Tag_expressionContext> tag_expression() {
			return getRuleContexts(Tag_expressionContext.class);
		}
		public Tag_expressionContext tag_expression(int i) {
			return getRuleContext(Tag_expressionContext.class,i);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public TerminalNode BANG() { return getToken(NebulaParser.BANG, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode AMP() { return getToken(NebulaParser.AMP, 0); }
		public TerminalNode PIPE() { return getToken(NebulaParser.PIPE, 0); }
		public Tag_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tag_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTag_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTag_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTag_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Tag_expressionContext tag_expression() throws RecognitionException {
		return tag_expression(0);
	}

	private Tag_expressionContext tag_expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Tag_expressionContext _localctx = new Tag_expressionContext(_ctx, _parentState);
		Tag_expressionContext _prevctx = _localctx;
		int _startState = 18;
		enterRecursionRule(_localctx, 18, RULE_tag_expression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(320);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				{
				setState(313);
				match(OPEN_PARENS);
				setState(314);
				tag_expression(0);
				setState(315);
				match(CLOSE_PARENS);
				}
				break;
			case 2:
				{
				setState(317);
				match(BANG);
				setState(318);
				tag_expression(4);
				}
				break;
			case 3:
				{
				setState(319);
				type();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(330);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(328);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
					case 1:
						{
						_localctx = new Tag_expressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_tag_expression);
						setState(322);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						{
						setState(323);
						match(AMP);
						}
						setState(324);
						tag_expression(4);
						}
						break;
					case 2:
						{
						_localctx = new Tag_expressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_tag_expression);
						setState(325);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						{
						setState(326);
						match(PIPE);
						}
						setState(327);
						tag_expression(3);
						}
						break;
					}
					} 
				}
				setState(332);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Namespace_declarationContext extends ParserRuleContext {
		public TerminalNode NAMESPACE() { return getToken(NebulaParser.NAMESPACE, 0); }
		public Qualified_nameContext qualified_name() {
			return getRuleContext(Qualified_nameContext.class,0);
		}
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public TerminalNode SEMICOLON() { return getToken(NebulaParser.SEMICOLON, 0); }
		public List<Top_level_declarationContext> top_level_declaration() {
			return getRuleContexts(Top_level_declarationContext.class);
		}
		public Top_level_declarationContext top_level_declaration(int i) {
			return getRuleContext(Top_level_declarationContext.class,i);
		}
		public Namespace_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namespace_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterNamespace_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitNamespace_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitNamespace_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Namespace_declarationContext namespace_declaration() throws RecognitionException {
		Namespace_declarationContext _localctx = new Namespace_declarationContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_namespace_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(333);
			match(NAMESPACE);
			setState(334);
			qualified_name();
			setState(344);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				{
				setState(335);
				match(OPEN_BRACE);
				setState(339);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 9130899292993996800L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 262153L) != 0)) {
					{
					{
					setState(336);
					top_level_declaration();
					}
					}
					setState(341);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(342);
				match(CLOSE_BRACE);
				}
				break;
			case SEMICOLON:
				{
				setState(343);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Qualified_nameContext extends ParserRuleContext {
		public List<TerminalNode> IDENTIFIER() { return getTokens(NebulaParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(NebulaParser.IDENTIFIER, i);
		}
		public List<TerminalNode> DOUBLE_COLON() { return getTokens(NebulaParser.DOUBLE_COLON); }
		public TerminalNode DOUBLE_COLON(int i) {
			return getToken(NebulaParser.DOUBLE_COLON, i);
		}
		public Qualified_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualified_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterQualified_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitQualified_name(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitQualified_name(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Qualified_nameContext qualified_name() throws RecognitionException {
		Qualified_nameContext _localctx = new Qualified_nameContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_qualified_name);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(346);
			match(IDENTIFIER);
			setState(351);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(347);
					match(DOUBLE_COLON);
					setState(348);
					match(IDENTIFIER);
					}
					} 
				}
				setState(353);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Enum_declarationContext extends ParserRuleContext {
		public TerminalNode ENUM() { return getToken(NebulaParser.ENUM, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Enum_blockContext enum_block() {
			return getRuleContext(Enum_blockContext.class,0);
		}
		public Enum_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterEnum_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitEnum_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitEnum_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Enum_declarationContext enum_declaration() throws RecognitionException {
		Enum_declarationContext _localctx = new Enum_declarationContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_enum_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(354);
			match(ENUM);
			setState(355);
			match(IDENTIFIER);
			setState(356);
			enum_block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Enum_blockContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public List<TerminalNode> IDENTIFIER() { return getTokens(NebulaParser.IDENTIFIER); }
		public TerminalNode IDENTIFIER(int i) {
			return getToken(NebulaParser.IDENTIFIER, i);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Enum_blockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterEnum_block(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitEnum_block(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitEnum_block(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Enum_blockContext enum_block() throws RecognitionException {
		Enum_blockContext _localctx = new Enum_blockContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_enum_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(358);
			match(OPEN_BRACE);
			setState(359);
			match(IDENTIFIER);
			setState(364);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(360);
				match(COMMA);
				setState(361);
				match(IDENTIFIER);
				}
				}
				setState(366);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(367);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Top_level_declarationContext extends ParserRuleContext {
		public Use_statementContext use_statement() {
			return getRuleContext(Use_statementContext.class,0);
		}
		public Tag_statementContext tag_statement() {
			return getRuleContext(Tag_statementContext.class,0);
		}
		public Enum_declarationContext enum_declaration() {
			return getRuleContext(Enum_declarationContext.class,0);
		}
		public Const_declarationContext const_declaration() {
			return getRuleContext(Const_declarationContext.class,0);
		}
		public Method_declarationContext method_declaration() {
			return getRuleContext(Method_declarationContext.class,0);
		}
		public Class_declarationContext class_declaration() {
			return getRuleContext(Class_declarationContext.class,0);
		}
		public Struct_declarationContext struct_declaration() {
			return getRuleContext(Struct_declarationContext.class,0);
		}
		public Trait_declarationContext trait_declaration() {
			return getRuleContext(Trait_declarationContext.class,0);
		}
		public Union_declarationContext union_declaration() {
			return getRuleContext(Union_declarationContext.class,0);
		}
		public Namespace_declarationContext namespace_declaration() {
			return getRuleContext(Namespace_declarationContext.class,0);
		}
		public Extern_declarationContext extern_declaration() {
			return getRuleContext(Extern_declarationContext.class,0);
		}
		public Top_level_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_top_level_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTop_level_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTop_level_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTop_level_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Top_level_declarationContext top_level_declaration() throws RecognitionException {
		Top_level_declarationContext _localctx = new Top_level_declarationContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_top_level_declaration);
		try {
			setState(380);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(369);
				use_statement();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(370);
				tag_statement();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(371);
				enum_declaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(372);
				const_declaration();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(373);
				method_declaration();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(374);
				class_declaration();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(375);
				struct_declaration();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(376);
				trait_declaration();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(377);
				union_declaration();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(378);
				namespace_declaration();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(379);
				extern_declaration();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StatementContext extends ParserRuleContext {
		public Use_statementContext use_statement() {
			return getRuleContext(Use_statementContext.class,0);
		}
		public Tag_statementContext tag_statement() {
			return getRuleContext(Tag_statementContext.class,0);
		}
		public Const_declarationContext const_declaration() {
			return getRuleContext(Const_declarationContext.class,0);
		}
		public Variable_declarationContext variable_declaration() {
			return getRuleContext(Variable_declarationContext.class,0);
		}
		public Statement_blockContext statement_block() {
			return getRuleContext(Statement_blockContext.class,0);
		}
		public If_statementContext if_statement() {
			return getRuleContext(If_statementContext.class,0);
		}
		public For_statementContext for_statement() {
			return getRuleContext(For_statementContext.class,0);
		}
		public While_statementContext while_statement() {
			return getRuleContext(While_statementContext.class,0);
		}
		public Foreach_statementContext foreach_statement() {
			return getRuleContext(Foreach_statementContext.class,0);
		}
		public Return_statementContext return_statement() {
			return getRuleContext(Return_statementContext.class,0);
		}
		public Expression_statementContext expression_statement() {
			return getRuleContext(Expression_statementContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_statement);
		try {
			setState(393);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(382);
				use_statement();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(383);
				tag_statement();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(384);
				const_declaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(385);
				variable_declaration();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(386);
				statement_block();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(387);
				if_statement();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(388);
				for_statement();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(389);
				while_statement();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(390);
				foreach_statement();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(391);
				return_statement();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(392);
				expression_statement();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression_statementContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(NebulaParser.SEMICOLON, 0); }
		public Expression_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterExpression_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitExpression_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitExpression_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Expression_statementContext expression_statement() throws RecognitionException {
		Expression_statementContext _localctx = new Expression_statementContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_expression_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(395);
			expression();
			setState(396);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression_blockContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public Block_statementsContext block_statements() {
			return getRuleContext(Block_statementsContext.class,0);
		}
		public Block_tailContext block_tail() {
			return getRuleContext(Block_tailContext.class,0);
		}
		public Expression_blockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterExpression_block(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitExpression_block(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitExpression_block(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Expression_blockContext expression_block() throws RecognitionException {
		Expression_blockContext _localctx = new Expression_blockContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_expression_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(398);
			match(OPEN_BRACE);
			setState(400);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				{
				setState(399);
				block_statements();
				}
				break;
			}
			setState(403);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 90075291635941376L) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & 6455077631L) != 0)) {
				{
				setState(402);
				block_tail();
				}
			}

			setState(405);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Statement_blockContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public Block_statementsContext block_statements() {
			return getRuleContext(Block_statementsContext.class,0);
		}
		public Statement_blockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterStatement_block(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitStatement_block(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitStatement_block(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Statement_blockContext statement_block() throws RecognitionException {
		Statement_blockContext _localctx = new Statement_blockContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_statement_block);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(407);
			match(OPEN_BRACE);
			setState(409);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				setState(408);
				block_statements();
				}
				break;
			}
			setState(411);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Block_statementsContext extends ParserRuleContext {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public Block_statementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block_statements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterBlock_statements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitBlock_statements(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitBlock_statements(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Block_statementsContext block_statements() throws RecognitionException {
		Block_statementsContext _localctx = new Block_statementsContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_block_statements);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(416);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(413);
					statement();
					}
					} 
				}
				setState(418);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Block_tailContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Block_tailContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block_tail; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterBlock_tail(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitBlock_tail(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitBlock_tail(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Block_tailContext block_tail() throws RecognitionException {
		Block_tailContext _localctx = new Block_tailContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_block_tail);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(419);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class If_expressionContext extends ParserRuleContext {
		public TerminalNode IF() { return getToken(NebulaParser.IF, 0); }
		public Parenthesized_expressionContext parenthesized_expression() {
			return getRuleContext(Parenthesized_expressionContext.class,0);
		}
		public List<Expression_blockContext> expression_block() {
			return getRuleContexts(Expression_blockContext.class);
		}
		public Expression_blockContext expression_block(int i) {
			return getRuleContext(Expression_blockContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(NebulaParser.ELSE, 0); }
		public If_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_if_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterIf_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitIf_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitIf_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final If_expressionContext if_expression() throws RecognitionException {
		If_expressionContext _localctx = new If_expressionContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_if_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(421);
			match(IF);
			setState(422);
			parenthesized_expression();
			setState(423);
			expression_block();
			setState(424);
			match(ELSE);
			setState(425);
			expression_block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class If_statementContext extends ParserRuleContext {
		public TerminalNode IF() { return getToken(NebulaParser.IF, 0); }
		public Parenthesized_expressionContext parenthesized_expression() {
			return getRuleContext(Parenthesized_expressionContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(NebulaParser.ELSE, 0); }
		public If_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_if_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterIf_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitIf_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitIf_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final If_statementContext if_statement() throws RecognitionException {
		If_statementContext _localctx = new If_statementContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_if_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(427);
			match(IF);
			setState(428);
			parenthesized_expression();
			setState(429);
			statement();
			setState(432);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				{
				setState(430);
				match(ELSE);
				setState(431);
				statement();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Match_expressionContext extends ParserRuleContext {
		public TerminalNode MATCH() { return getToken(NebulaParser.MATCH, 0); }
		public Parenthesized_expressionContext parenthesized_expression() {
			return getRuleContext(Parenthesized_expressionContext.class,0);
		}
		public Match_bodyContext match_body() {
			return getRuleContext(Match_bodyContext.class,0);
		}
		public Match_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterMatch_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitMatch_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitMatch_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Match_expressionContext match_expression() throws RecognitionException {
		Match_expressionContext _localctx = new Match_expressionContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_match_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(434);
			match(MATCH);
			setState(435);
			parenthesized_expression();
			setState(436);
			match_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class For_statementContext extends ParserRuleContext {
		public TerminalNode FOR() { return getToken(NebulaParser.FOR, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public Traditional_for_controlContext traditional_for_control() {
			return getRuleContext(Traditional_for_controlContext.class,0);
		}
		public Parenthesized_expressionContext parenthesized_expression() {
			return getRuleContext(Parenthesized_expressionContext.class,0);
		}
		public For_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_for_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterFor_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitFor_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitFor_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final For_statementContext for_statement() throws RecognitionException {
		For_statementContext _localctx = new For_statementContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_for_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(438);
			match(FOR);
			setState(441);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				{
				setState(439);
				traditional_for_control();
				}
				break;
			case 2:
				{
				setState(440);
				parenthesized_expression();
				}
				break;
			}
			setState(443);
			statement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Traditional_for_controlContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public TerminalNode SEMICOLON() { return getToken(NebulaParser.SEMICOLON, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public For_initializerContext for_initializer() {
			return getRuleContext(For_initializerContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public For_iteratorContext for_iterator() {
			return getRuleContext(For_iteratorContext.class,0);
		}
		public Traditional_for_controlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_traditional_for_control; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTraditional_for_control(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTraditional_for_control(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTraditional_for_control(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Traditional_for_controlContext traditional_for_control() throws RecognitionException {
		Traditional_for_controlContext _localctx = new Traditional_for_controlContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_traditional_for_control);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(445);
			match(OPEN_PARENS);
			setState(447);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				setState(446);
				for_initializer();
				}
				break;
			}
			setState(450);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 90075291635941376L) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & 6455077631L) != 0)) {
				{
				setState(449);
				expression();
				}
			}

			setState(452);
			match(SEMICOLON);
			setState(454);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 90075291635941376L) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & 6455077631L) != 0)) {
				{
				setState(453);
				for_iterator();
				}
			}

			setState(456);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class For_initializerContext extends ParserRuleContext {
		public Variable_declarationContext variable_declaration() {
			return getRuleContext(Variable_declarationContext.class,0);
		}
		public Expression_listContext expression_list() {
			return getRuleContext(Expression_listContext.class,0);
		}
		public For_initializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_for_initializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterFor_initializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitFor_initializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitFor_initializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final For_initializerContext for_initializer() throws RecognitionException {
		For_initializerContext _localctx = new For_initializerContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_for_initializer);
		try {
			setState(460);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(458);
				variable_declaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(459);
				expression_list();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class For_iteratorContext extends ParserRuleContext {
		public Expression_listContext expression_list() {
			return getRuleContext(Expression_listContext.class,0);
		}
		public For_iteratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_for_iterator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterFor_iterator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitFor_iterator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitFor_iterator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final For_iteratorContext for_iterator() throws RecognitionException {
		For_iteratorContext _localctx = new For_iteratorContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_for_iterator);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(462);
			expression_list();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class While_statementContext extends ParserRuleContext {
		public TerminalNode WHILE() { return getToken(NebulaParser.WHILE, 0); }
		public Parenthesized_expressionContext parenthesized_expression() {
			return getRuleContext(Parenthesized_expressionContext.class,0);
		}
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public While_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_while_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterWhile_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitWhile_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitWhile_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final While_statementContext while_statement() throws RecognitionException {
		While_statementContext _localctx = new While_statementContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_while_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(464);
			match(WHILE);
			setState(465);
			parenthesized_expression();
			setState(466);
			statement();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Foreach_statementContext extends ParserRuleContext {
		public TerminalNode FOREACH() { return getToken(NebulaParser.FOREACH, 0); }
		public Foreach_controlContext foreach_control() {
			return getRuleContext(Foreach_controlContext.class,0);
		}
		public Statement_blockContext statement_block() {
			return getRuleContext(Statement_blockContext.class,0);
		}
		public Foreach_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_foreach_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterForeach_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitForeach_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitForeach_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Foreach_statementContext foreach_statement() throws RecognitionException {
		Foreach_statementContext _localctx = new Foreach_statementContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_foreach_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(468);
			match(FOREACH);
			setState(469);
			foreach_control();
			setState(470);
			statement_block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Foreach_controlContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public TerminalNode IN() { return getToken(NebulaParser.IN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public TerminalNode VAR() { return getToken(NebulaParser.VAR, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public Foreach_controlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_foreach_control; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterForeach_control(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitForeach_control(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitForeach_control(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Foreach_controlContext foreach_control() throws RecognitionException {
		Foreach_controlContext _localctx = new Foreach_controlContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_foreach_control);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(472);
			match(OPEN_PARENS);
			setState(475);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VAR:
				{
				setState(473);
				match(VAR);
				}
				break;
			case BOOL:
			case CHAR:
			case DECIMAL:
			case F32:
			case F64:
			case INT8:
			case INT16:
			case INT32:
			case INT64:
			case STRING:
			case UINT8:
			case UINT16:
			case UINT32:
			case UINT64:
			case IDENTIFIER:
			case OPEN_PARENS:
				{
				setState(474);
				type();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(477);
			match(IDENTIFIER);
			setState(478);
			match(IN);
			setState(479);
			expression();
			setState(480);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Return_statementContext extends ParserRuleContext {
		public TerminalNode RETURN() { return getToken(NebulaParser.RETURN, 0); }
		public TerminalNode SEMICOLON() { return getToken(NebulaParser.SEMICOLON, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Return_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_return_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterReturn_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitReturn_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitReturn_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Return_statementContext return_statement() throws RecognitionException {
		Return_statementContext _localctx = new Return_statementContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_return_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(482);
			match(RETURN);
			setState(484);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 90075291635941376L) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & 6455077631L) != 0)) {
				{
				setState(483);
				expression();
				}
			}

			setState(486);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Match_bodyContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public List<Match_armContext> match_arm() {
			return getRuleContexts(Match_armContext.class);
		}
		public Match_armContext match_arm(int i) {
			return getRuleContext(Match_armContext.class,i);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Match_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterMatch_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitMatch_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitMatch_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Match_bodyContext match_body() throws RecognitionException {
		Match_bodyContext _localctx = new Match_bodyContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_match_body);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(488);
			match(OPEN_BRACE);
			setState(489);
			match_arm();
			setState(494);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(490);
					match(COMMA);
					setState(491);
					match_arm();
					}
					} 
				}
				setState(496);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			}
			setState(498);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(497);
				match(COMMA);
				}
			}

			setState(500);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Match_armContext extends ParserRuleContext {
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public TerminalNode FAT_ARROW() { return getToken(NebulaParser.FAT_ARROW, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public Match_armContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_arm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterMatch_arm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitMatch_arm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitMatch_arm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Match_armContext match_arm() throws RecognitionException {
		Match_armContext _localctx = new Match_armContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_match_arm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(502);
			pattern();
			setState(503);
			match(FAT_ARROW);
			setState(504);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PatternContext extends ParserRuleContext {
		public Pattern_orContext pattern_or() {
			return getRuleContext(Pattern_orContext.class,0);
		}
		public PatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PatternContext pattern() throws RecognitionException {
		PatternContext _localctx = new PatternContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_pattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(506);
			pattern_or();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Pattern_orContext extends ParserRuleContext {
		public List<Pattern_atomContext> pattern_atom() {
			return getRuleContexts(Pattern_atomContext.class);
		}
		public Pattern_atomContext pattern_atom(int i) {
			return getRuleContext(Pattern_atomContext.class,i);
		}
		public List<TerminalNode> PIPE() { return getTokens(NebulaParser.PIPE); }
		public TerminalNode PIPE(int i) {
			return getToken(NebulaParser.PIPE, i);
		}
		public Pattern_orContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern_or; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterPattern_or(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitPattern_or(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitPattern_or(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Pattern_orContext pattern_or() throws RecognitionException {
		Pattern_orContext _localctx = new Pattern_orContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_pattern_or);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(508);
			pattern_atom();
			setState(513);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PIPE) {
				{
				{
				setState(509);
				match(PIPE);
				setState(510);
				pattern_atom();
				}
				}
				setState(515);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Pattern_atomContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public TerminalNode UNDERSCORE() { return getToken(NebulaParser.UNDERSCORE, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Parenthesized_patternContext parenthesized_pattern() {
			return getRuleContext(Parenthesized_patternContext.class,0);
		}
		public Pattern_atomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern_atom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterPattern_atom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitPattern_atom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitPattern_atom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Pattern_atomContext pattern_atom() throws RecognitionException {
		Pattern_atomContext _localctx = new Pattern_atomContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_pattern_atom);
		try {
			setState(520);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FALSE:
			case TRUE:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
				enterOuterAlt(_localctx, 1);
				{
				setState(516);
				literal();
				}
				break;
			case UNDERSCORE:
				enterOuterAlt(_localctx, 2);
				{
				setState(517);
				match(UNDERSCORE);
				}
				break;
			case IDENTIFIER:
				enterOuterAlt(_localctx, 3);
				{
				setState(518);
				match(IDENTIFIER);
				}
				break;
			case OPEN_PARENS:
				enterOuterAlt(_localctx, 4);
				{
				setState(519);
				parenthesized_pattern();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Parenthesized_patternContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public Parenthesized_patternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parenthesized_pattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterParenthesized_pattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitParenthesized_pattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitParenthesized_pattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Parenthesized_patternContext parenthesized_pattern() throws RecognitionException {
		Parenthesized_patternContext _localctx = new Parenthesized_patternContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_parenthesized_pattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(522);
			match(OPEN_PARENS);
			setState(523);
			pattern();
			setState(524);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Const_declarationContext extends ParserRuleContext {
		public TerminalNode CONST() { return getToken(NebulaParser.CONST, 0); }
		public Variable_declarationContext variable_declaration() {
			return getRuleContext(Variable_declarationContext.class,0);
		}
		public Const_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_const_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterConst_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitConst_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitConst_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Const_declarationContext const_declaration() throws RecognitionException {
		Const_declarationContext _localctx = new Const_declarationContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_const_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(526);
			match(CONST);
			setState(527);
			variable_declaration();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Variable_declarationContext extends ParserRuleContext {
		public ModifiersContext modifiers() {
			return getRuleContext(ModifiersContext.class,0);
		}
		public Variable_declaratorsContext variable_declarators() {
			return getRuleContext(Variable_declaratorsContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(NebulaParser.SEMICOLON, 0); }
		public TerminalNode VAR() { return getToken(NebulaParser.VAR, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode CONST() { return getToken(NebulaParser.CONST, 0); }
		public Variable_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterVariable_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitVariable_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitVariable_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Variable_declarationContext variable_declaration() throws RecognitionException {
		Variable_declarationContext _localctx = new Variable_declarationContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_variable_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(529);
			modifiers();
			setState(531);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==CONST) {
				{
				setState(530);
				match(CONST);
				}
			}

			setState(535);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VAR:
				{
				setState(533);
				match(VAR);
				}
				break;
			case BOOL:
			case CHAR:
			case DECIMAL:
			case F32:
			case F64:
			case INT8:
			case INT16:
			case INT32:
			case INT64:
			case STRING:
			case UINT8:
			case UINT16:
			case UINT32:
			case UINT64:
			case IDENTIFIER:
			case OPEN_PARENS:
				{
				setState(534);
				type();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(537);
			variable_declarators();
			setState(538);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Visibility_modifierContext extends ParserRuleContext {
		public TerminalNode PUBLIC() { return getToken(NebulaParser.PUBLIC, 0); }
		public TerminalNode PRIVATE() { return getToken(NebulaParser.PRIVATE, 0); }
		public TerminalNode PROTECTED() { return getToken(NebulaParser.PROTECTED, 0); }
		public Visibility_modifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_visibility_modifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterVisibility_modifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitVisibility_modifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitVisibility_modifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Visibility_modifierContext visibility_modifier() throws RecognitionException {
		Visibility_modifierContext _localctx = new Visibility_modifierContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_visibility_modifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(540);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 123145302310912L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Cvt_modifierContext extends ParserRuleContext {
		public TerminalNode KEEPS() { return getToken(NebulaParser.KEEPS, 0); }
		public TerminalNode DROPS() { return getToken(NebulaParser.DROPS, 0); }
		public Cvt_modifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cvt_modifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterCvt_modifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitCvt_modifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitCvt_modifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Cvt_modifierContext cvt_modifier() throws RecognitionException {
		Cvt_modifierContext _localctx = new Cvt_modifierContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_cvt_modifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(542);
			_la = _input.LA(1);
			if ( !(_la==DROPS || _la==KEEPS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ModifiersContext extends ParserRuleContext {
		public List<Visibility_modifierContext> visibility_modifier() {
			return getRuleContexts(Visibility_modifierContext.class);
		}
		public Visibility_modifierContext visibility_modifier(int i) {
			return getRuleContext(Visibility_modifierContext.class,i);
		}
		public List<TerminalNode> STATIC() { return getTokens(NebulaParser.STATIC); }
		public TerminalNode STATIC(int i) {
			return getToken(NebulaParser.STATIC, i);
		}
		public List<TerminalNode> OVERRIDE() { return getTokens(NebulaParser.OVERRIDE); }
		public TerminalNode OVERRIDE(int i) {
			return getToken(NebulaParser.OVERRIDE, i);
		}
		public ModifiersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_modifiers; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterModifiers(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitModifiers(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitModifiers(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModifiersContext modifiers() throws RecognitionException {
		ModifiersContext _localctx = new ModifiersContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_modifiers);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(549);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 413416372043776L) != 0)) {
				{
				setState(547);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case PRIVATE:
				case PROTECTED:
				case PUBLIC:
					{
					setState(544);
					visibility_modifier();
					}
					break;
				case STATIC:
					{
					setState(545);
					match(STATIC);
					}
					break;
				case OVERRIDE:
					{
					setState(546);
					match(OVERRIDE);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(551);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Field_declarationContext extends ParserRuleContext {
		public Variable_declarationContext variable_declaration() {
			return getRuleContext(Variable_declarationContext.class,0);
		}
		public Field_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterField_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitField_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitField_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Field_declarationContext field_declaration() throws RecognitionException {
		Field_declarationContext _localctx = new Field_declarationContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_field_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(552);
			variable_declaration();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Variable_declaratorsContext extends ParserRuleContext {
		public List<Variable_declaratorContext> variable_declarator() {
			return getRuleContexts(Variable_declaratorContext.class);
		}
		public Variable_declaratorContext variable_declarator(int i) {
			return getRuleContext(Variable_declaratorContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Variable_declaratorsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable_declarators; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterVariable_declarators(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitVariable_declarators(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitVariable_declarators(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Variable_declaratorsContext variable_declarators() throws RecognitionException {
		Variable_declaratorsContext _localctx = new Variable_declaratorsContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_variable_declarators);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(554);
			variable_declarator();
			setState(559);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(555);
				match(COMMA);
				setState(556);
				variable_declarator();
				}
				}
				setState(561);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Variable_declaratorContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public TerminalNode ASSIGNMENT() { return getToken(NebulaParser.ASSIGNMENT, 0); }
		public NonAssignmentExpressionContext nonAssignmentExpression() {
			return getRuleContext(NonAssignmentExpressionContext.class,0);
		}
		public Variable_declaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable_declarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterVariable_declarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitVariable_declarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitVariable_declarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Variable_declaratorContext variable_declarator() throws RecognitionException {
		Variable_declaratorContext _localctx = new Variable_declaratorContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_variable_declarator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(562);
			match(IDENTIFIER);
			setState(565);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASSIGNMENT) {
				{
				setState(563);
				match(ASSIGNMENT);
				setState(564);
				nonAssignmentExpression();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Method_declarationContext extends ParserRuleContext {
		public ModifiersContext modifiers() {
			return getRuleContext(ModifiersContext.class,0);
		}
		public Return_typeContext return_type() {
			return getRuleContext(Return_typeContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public ParametersContext parameters() {
			return getRuleContext(ParametersContext.class,0);
		}
		public Method_bodyContext method_body() {
			return getRuleContext(Method_bodyContext.class,0);
		}
		public Type_parametersContext type_parameters() {
			return getRuleContext(Type_parametersContext.class,0);
		}
		public Method_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterMethod_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitMethod_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitMethod_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Method_declarationContext method_declaration() throws RecognitionException {
		Method_declarationContext _localctx = new Method_declarationContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_method_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(567);
			modifiers();
			setState(568);
			return_type();
			setState(569);
			match(IDENTIFIER);
			setState(571);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(570);
				type_parameters();
				}
			}

			setState(573);
			parameters();
			setState(574);
			method_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Constructor_declarationContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public ParametersContext parameters() {
			return getRuleContext(ParametersContext.class,0);
		}
		public Statement_blockContext statement_block() {
			return getRuleContext(Statement_blockContext.class,0);
		}
		public Visibility_modifierContext visibility_modifier() {
			return getRuleContext(Visibility_modifierContext.class,0);
		}
		public Constructor_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructor_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterConstructor_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitConstructor_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitConstructor_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Constructor_declarationContext constructor_declaration() throws RecognitionException {
		Constructor_declarationContext _localctx = new Constructor_declarationContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_constructor_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(577);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 123145302310912L) != 0)) {
				{
				setState(576);
				visibility_modifier();
				}
			}

			setState(579);
			match(IDENTIFIER);
			setState(580);
			parameters();
			setState(581);
			statement_block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParametersContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public Parameter_listContext parameter_list() {
			return getRuleContext(Parameter_listContext.class,0);
		}
		public ParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParametersContext parameters() throws RecognitionException {
		ParametersContext _localctx = new ParametersContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_parameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(583);
			match(OPEN_PARENS);
			setState(585);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2162291078283413504L) != 0) || _la==IDENTIFIER || _la==OPEN_PARENS) {
				{
				setState(584);
				parameter_list();
				}
			}

			setState(587);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Parameter_listContext extends ParserRuleContext {
		public List<ParameterContext> parameter() {
			return getRuleContexts(ParameterContext.class);
		}
		public ParameterContext parameter(int i) {
			return getRuleContext(ParameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Parameter_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameter_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterParameter_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitParameter_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitParameter_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Parameter_listContext parameter_list() throws RecognitionException {
		Parameter_listContext _localctx = new Parameter_listContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_parameter_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(589);
			parameter();
			setState(594);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(590);
				match(COMMA);
				setState(591);
				parameter();
				}
				}
				setState(596);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ParameterContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Cvt_modifierContext cvt_modifier() {
			return getRuleContext(Cvt_modifierContext.class,0);
		}
		public TerminalNode ASSIGNMENT() { return getToken(NebulaParser.ASSIGNMENT, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParameterContext parameter() throws RecognitionException {
		ParameterContext _localctx = new ParameterContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_parameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(598);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DROPS || _la==KEEPS) {
				{
				setState(597);
				cvt_modifier();
				}
			}

			setState(600);
			type();
			setState(601);
			match(IDENTIFIER);
			setState(604);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASSIGNMENT) {
				{
				setState(602);
				match(ASSIGNMENT);
				setState(603);
				expression();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Method_bodyContext extends ParserRuleContext {
		public Expression_blockContext expression_block() {
			return getRuleContext(Expression_blockContext.class,0);
		}
		public TerminalNode FAT_ARROW() { return getToken(NebulaParser.FAT_ARROW, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(NebulaParser.SEMICOLON, 0); }
		public Method_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterMethod_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitMethod_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitMethod_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Method_bodyContext method_body() throws RecognitionException {
		Method_bodyContext _localctx = new Method_bodyContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_method_body);
		try {
			setState(610);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(606);
				expression_block();
				}
				break;
			case FAT_ARROW:
				enterOuterAlt(_localctx, 2);
				{
				setState(607);
				match(FAT_ARROW);
				setState(608);
				expression();
				}
				break;
			case SEMICOLON:
				enterOuterAlt(_localctx, 3);
				{
				setState(609);
				match(SEMICOLON);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Operator_declarationContext extends ParserRuleContext {
		public TerminalNode OPERATOR() { return getToken(NebulaParser.OPERATOR, 0); }
		public Overloadable_operatorContext overloadable_operator() {
			return getRuleContext(Overloadable_operatorContext.class,0);
		}
		public ParametersContext parameters() {
			return getRuleContext(ParametersContext.class,0);
		}
		public Method_bodyContext method_body() {
			return getRuleContext(Method_bodyContext.class,0);
		}
		public Operator_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operator_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterOperator_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitOperator_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitOperator_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Operator_declarationContext operator_declaration() throws RecognitionException {
		Operator_declarationContext _localctx = new Operator_declarationContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_operator_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(612);
			match(OPERATOR);
			setState(613);
			overloadable_operator();
			setState(614);
			parameters();
			setState(615);
			method_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Overloadable_operatorContext extends ParserRuleContext {
		public TerminalNode PLUS() { return getToken(NebulaParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(NebulaParser.MINUS, 0); }
		public TerminalNode STAR() { return getToken(NebulaParser.STAR, 0); }
		public TerminalNode DIV() { return getToken(NebulaParser.DIV, 0); }
		public TerminalNode PERCENT() { return getToken(NebulaParser.PERCENT, 0); }
		public TerminalNode CARET() { return getToken(NebulaParser.CARET, 0); }
		public TerminalNode AMP() { return getToken(NebulaParser.AMP, 0); }
		public TerminalNode PIPE() { return getToken(NebulaParser.PIPE, 0); }
		public List<TerminalNode> LT() { return getTokens(NebulaParser.LT); }
		public TerminalNode LT(int i) {
			return getToken(NebulaParser.LT, i);
		}
		public List<TerminalNode> GT() { return getTokens(NebulaParser.GT); }
		public TerminalNode GT(int i) {
			return getToken(NebulaParser.GT, i);
		}
		public TerminalNode OP_EQ() { return getToken(NebulaParser.OP_EQ, 0); }
		public TerminalNode OP_NE() { return getToken(NebulaParser.OP_NE, 0); }
		public Overloadable_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_overloadable_operator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterOverloadable_operator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitOverloadable_operator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitOverloadable_operator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Overloadable_operatorContext overloadable_operator() throws RecognitionException {
		Overloadable_operatorContext _localctx = new Overloadable_operatorContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_overloadable_operator);
		try {
			setState(631);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PLUS:
				enterOuterAlt(_localctx, 1);
				{
				setState(617);
				match(PLUS);
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 2);
				{
				setState(618);
				match(MINUS);
				}
				break;
			case STAR:
				enterOuterAlt(_localctx, 3);
				{
				setState(619);
				match(STAR);
				}
				break;
			case DIV:
				enterOuterAlt(_localctx, 4);
				{
				setState(620);
				match(DIV);
				}
				break;
			case PERCENT:
				enterOuterAlt(_localctx, 5);
				{
				setState(621);
				match(PERCENT);
				}
				break;
			case CARET:
				enterOuterAlt(_localctx, 6);
				{
				setState(622);
				match(CARET);
				}
				break;
			case AMP:
				enterOuterAlt(_localctx, 7);
				{
				setState(623);
				match(AMP);
				}
				break;
			case PIPE:
				enterOuterAlt(_localctx, 8);
				{
				setState(624);
				match(PIPE);
				}
				break;
			case LT:
				enterOuterAlt(_localctx, 9);
				{
				setState(625);
				match(LT);
				setState(626);
				match(LT);
				}
				break;
			case GT:
				enterOuterAlt(_localctx, 10);
				{
				setState(627);
				match(GT);
				setState(628);
				match(GT);
				}
				break;
			case OP_EQ:
				enterOuterAlt(_localctx, 11);
				{
				setState(629);
				match(OP_EQ);
				}
				break;
			case OP_NE:
				enterOuterAlt(_localctx, 12);
				{
				setState(630);
				match(OP_NE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_declarationContext extends ParserRuleContext {
		public TerminalNode CLASS() { return getToken(NebulaParser.CLASS, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Class_bodyContext class_body() {
			return getRuleContext(Class_bodyContext.class,0);
		}
		public Type_parametersContext type_parameters() {
			return getRuleContext(Type_parametersContext.class,0);
		}
		public Inheritance_clauseContext inheritance_clause() {
			return getRuleContext(Inheritance_clauseContext.class,0);
		}
		public Class_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterClass_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitClass_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitClass_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_declarationContext class_declaration() throws RecognitionException {
		Class_declarationContext _localctx = new Class_declarationContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_class_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(633);
			match(CLASS);
			setState(634);
			match(IDENTIFIER);
			setState(636);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(635);
				type_parameters();
				}
			}

			setState(639);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(638);
				inheritance_clause();
				}
			}

			setState(641);
			class_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_bodyContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public List<Class_memberContext> class_member() {
			return getRuleContexts(Class_memberContext.class);
		}
		public Class_memberContext class_member(int i) {
			return getRuleContext(Class_memberContext.class,i);
		}
		public Class_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterClass_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitClass_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitClass_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_bodyContext class_body() throws RecognitionException {
		Class_bodyContext _localctx = new Class_bodyContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_class_body);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(643);
			match(OPEN_BRACE);
			setState(647);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -7060663419031697408L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 262153L) != 0)) {
				{
				{
				setState(644);
				class_member();
				}
				}
				setState(649);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(650);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_memberContext extends ParserRuleContext {
		public Field_declarationContext field_declaration() {
			return getRuleContext(Field_declarationContext.class,0);
		}
		public Method_declarationContext method_declaration() {
			return getRuleContext(Method_declarationContext.class,0);
		}
		public Operator_declarationContext operator_declaration() {
			return getRuleContext(Operator_declarationContext.class,0);
		}
		public Constructor_declarationContext constructor_declaration() {
			return getRuleContext(Constructor_declarationContext.class,0);
		}
		public Class_memberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_member; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterClass_member(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitClass_member(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitClass_member(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_memberContext class_member() throws RecognitionException {
		Class_memberContext _localctx = new Class_memberContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_class_member);
		try {
			setState(656);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,51,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(652);
				field_declaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(653);
				method_declaration();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(654);
				operator_declaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(655);
				constructor_declaration();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Struct_declarationContext extends ParserRuleContext {
		public TerminalNode STRUCT() { return getToken(NebulaParser.STRUCT, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Struct_bodyContext struct_body() {
			return getRuleContext(Struct_bodyContext.class,0);
		}
		public Type_parametersContext type_parameters() {
			return getRuleContext(Type_parametersContext.class,0);
		}
		public Inheritance_clauseContext inheritance_clause() {
			return getRuleContext(Inheritance_clauseContext.class,0);
		}
		public Struct_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterStruct_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitStruct_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitStruct_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Struct_declarationContext struct_declaration() throws RecognitionException {
		Struct_declarationContext _localctx = new Struct_declarationContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_struct_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(658);
			match(STRUCT);
			setState(659);
			match(IDENTIFIER);
			setState(661);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(660);
				type_parameters();
				}
			}

			setState(664);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(663);
				inheritance_clause();
				}
			}

			setState(666);
			struct_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Struct_bodyContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public List<Struct_memberContext> struct_member() {
			return getRuleContexts(Struct_memberContext.class);
		}
		public Struct_memberContext struct_member(int i) {
			return getRuleContext(Struct_memberContext.class,i);
		}
		public Struct_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterStruct_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitStruct_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitStruct_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Struct_bodyContext struct_body() throws RecognitionException {
		Struct_bodyContext _localctx = new Struct_bodyContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_struct_body);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(668);
			match(OPEN_BRACE);
			setState(672);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -7060663419031697408L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 262153L) != 0)) {
				{
				{
				setState(669);
				struct_member();
				}
				}
				setState(674);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(675);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Struct_memberContext extends ParserRuleContext {
		public Field_declarationContext field_declaration() {
			return getRuleContext(Field_declarationContext.class,0);
		}
		public Method_declarationContext method_declaration() {
			return getRuleContext(Method_declarationContext.class,0);
		}
		public Operator_declarationContext operator_declaration() {
			return getRuleContext(Operator_declarationContext.class,0);
		}
		public Constructor_declarationContext constructor_declaration() {
			return getRuleContext(Constructor_declarationContext.class,0);
		}
		public Struct_memberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_member; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterStruct_member(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitStruct_member(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitStruct_member(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Struct_memberContext struct_member() throws RecognitionException {
		Struct_memberContext _localctx = new Struct_memberContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_struct_member);
		try {
			setState(681);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,55,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(677);
				field_declaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(678);
				method_declaration();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(679);
				operator_declaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(680);
				constructor_declaration();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Inheritance_clauseContext extends ParserRuleContext {
		public TerminalNode COLON() { return getToken(NebulaParser.COLON, 0); }
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Inheritance_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inheritance_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterInheritance_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitInheritance_clause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitInheritance_clause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Inheritance_clauseContext inheritance_clause() throws RecognitionException {
		Inheritance_clauseContext _localctx = new Inheritance_clauseContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_inheritance_clause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(683);
			match(COLON);
			setState(684);
			type();
			setState(689);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(685);
				match(COMMA);
				setState(686);
				type();
				}
				}
				setState(691);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Trait_declarationContext extends ParserRuleContext {
		public TerminalNode TRAIT() { return getToken(NebulaParser.TRAIT, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Trait_bodyContext trait_body() {
			return getRuleContext(Trait_bodyContext.class,0);
		}
		public Trait_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_trait_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTrait_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTrait_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTrait_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Trait_declarationContext trait_declaration() throws RecognitionException {
		Trait_declarationContext _localctx = new Trait_declarationContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_trait_declaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(692);
			match(TRAIT);
			setState(693);
			match(IDENTIFIER);
			setState(694);
			trait_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Trait_bodyContext extends ParserRuleContext {
		public Trait_blockContext trait_block() {
			return getRuleContext(Trait_blockContext.class,0);
		}
		public Method_declarationContext method_declaration() {
			return getRuleContext(Method_declarationContext.class,0);
		}
		public Trait_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_trait_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTrait_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTrait_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTrait_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Trait_bodyContext trait_body() throws RecognitionException {
		Trait_bodyContext _localctx = new Trait_bodyContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_trait_body);
		try {
			setState(698);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(696);
				trait_block();
				}
				break;
			case BOOL:
			case CHAR:
			case DECIMAL:
			case F32:
			case F64:
			case INT8:
			case INT16:
			case INT32:
			case INT64:
			case OVERRIDE:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case STATIC:
			case STRING:
			case UINT8:
			case UINT16:
			case UINT32:
			case UINT64:
			case VOID:
			case IDENTIFIER:
			case OPEN_PARENS:
				enterOuterAlt(_localctx, 2);
				{
				setState(697);
				method_declaration();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Trait_blockContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public List<Trait_memberContext> trait_member() {
			return getRuleContexts(Trait_memberContext.class);
		}
		public Trait_memberContext trait_member(int i) {
			return getRuleContext(Trait_memberContext.class,i);
		}
		public Trait_blockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_trait_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTrait_block(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTrait_block(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTrait_block(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Trait_blockContext trait_block() throws RecognitionException {
		Trait_blockContext _localctx = new Trait_blockContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_trait_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(700);
			match(OPEN_BRACE);
			setState(704);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2162704219776501760L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 262153L) != 0)) {
				{
				{
				setState(701);
				trait_member();
				}
				}
				setState(706);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(707);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Trait_memberContext extends ParserRuleContext {
		public Method_declarationContext method_declaration() {
			return getRuleContext(Method_declarationContext.class,0);
		}
		public Trait_memberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_trait_member; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTrait_member(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTrait_member(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTrait_member(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Trait_memberContext trait_member() throws RecognitionException {
		Trait_memberContext _localctx = new Trait_memberContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_trait_member);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(709);
			method_declaration();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Union_declarationContext extends ParserRuleContext {
		public TerminalNode UNION() { return getToken(NebulaParser.UNION, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Union_bodyContext union_body() {
			return getRuleContext(Union_bodyContext.class,0);
		}
		public TerminalNode TAGGED() { return getToken(NebulaParser.TAGGED, 0); }
		public Type_parametersContext type_parameters() {
			return getRuleContext(Type_parametersContext.class,0);
		}
		public Union_declarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_union_declaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterUnion_declaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitUnion_declaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitUnion_declaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Union_declarationContext union_declaration() throws RecognitionException {
		Union_declarationContext _localctx = new Union_declarationContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_union_declaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(712);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TAGGED) {
				{
				setState(711);
				match(TAGGED);
				}
			}

			setState(714);
			match(UNION);
			setState(715);
			match(IDENTIFIER);
			setState(717);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(716);
				type_parameters();
				}
			}

			setState(719);
			union_body();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Union_bodyContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACE() { return getToken(NebulaParser.OPEN_BRACE, 0); }
		public List<Union_variantContext> union_variant() {
			return getRuleContexts(Union_variantContext.class);
		}
		public Union_variantContext union_variant(int i) {
			return getRuleContext(Union_variantContext.class,i);
		}
		public TerminalNode CLOSE_BRACE() { return getToken(NebulaParser.CLOSE_BRACE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Union_bodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_union_body; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterUnion_body(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitUnion_body(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitUnion_body(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Union_bodyContext union_body() throws RecognitionException {
		Union_bodyContext _localctx = new Union_bodyContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_union_body);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(721);
			match(OPEN_BRACE);
			setState(722);
			union_variant();
			setState(727);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,61,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(723);
					match(COMMA);
					setState(724);
					union_variant();
					}
					} 
				}
				setState(729);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,61,_ctx);
			}
			setState(731);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(730);
				match(COMMA);
				}
			}

			setState(733);
			match(CLOSE_BRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Union_payloadContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public ParameterContext parameter() {
			return getRuleContext(ParameterContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public Union_payloadContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_union_payload; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterUnion_payload(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitUnion_payload(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitUnion_payload(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Union_payloadContext union_payload() throws RecognitionException {
		Union_payloadContext _localctx = new Union_payloadContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_union_payload);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(735);
			match(OPEN_PARENS);
			setState(736);
			parameter();
			setState(737);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Union_variantContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Union_payloadContext union_payload() {
			return getRuleContext(Union_payloadContext.class,0);
		}
		public Union_variantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_union_variant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterUnion_variant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitUnion_variant(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitUnion_variant(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Union_variantContext union_variant() throws RecognitionException {
		Union_variantContext _localctx = new Union_variantContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_union_variant);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(739);
			match(IDENTIFIER);
			setState(741);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN_PARENS) {
				{
				setState(740);
				union_payload();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Return_typeContext extends ParserRuleContext {
		public TerminalNode VOID() { return getToken(NebulaParser.VOID, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public Return_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_return_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterReturn_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitReturn_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitReturn_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Return_typeContext return_type() throws RecognitionException {
		Return_typeContext _localctx = new Return_typeContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_return_type);
		try {
			setState(745);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case VOID:
				enterOuterAlt(_localctx, 1);
				{
				setState(743);
				match(VOID);
				}
				break;
			case BOOL:
			case CHAR:
			case DECIMAL:
			case F32:
			case F64:
			case INT8:
			case INT16:
			case INT32:
			case INT64:
			case STRING:
			case UINT8:
			case UINT16:
			case UINT32:
			case UINT64:
			case IDENTIFIER:
			case OPEN_PARENS:
				enterOuterAlt(_localctx, 2);
				{
				setState(744);
				type();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeContext extends ParserRuleContext {
		public Class_typeContext class_type() {
			return getRuleContext(Class_typeContext.class,0);
		}
		public Predefined_typeContext predefined_type() {
			return getRuleContext(Predefined_typeContext.class,0);
		}
		public Tuple_typeContext tuple_type() {
			return getRuleContext(Tuple_typeContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public TerminalNode COLON() { return getToken(NebulaParser.COLON, 0); }
		public List<Rank_specifierContext> rank_specifier() {
			return getRuleContexts(Rank_specifierContext.class);
		}
		public Rank_specifierContext rank_specifier(int i) {
			return getRuleContext(Rank_specifierContext.class,i);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_type);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(749);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,65,_ctx) ) {
			case 1:
				{
				setState(747);
				match(IDENTIFIER);
				setState(748);
				match(COLON);
				}
				break;
			}
			setState(772);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IDENTIFIER:
				{
				setState(751);
				class_type();
				setState(755);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,66,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(752);
						rank_specifier();
						}
						} 
					}
					setState(757);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,66,_ctx);
				}
				}
				break;
			case BOOL:
			case CHAR:
			case DECIMAL:
			case F32:
			case F64:
			case INT8:
			case INT16:
			case INT32:
			case INT64:
			case STRING:
			case UINT8:
			case UINT16:
			case UINT32:
			case UINT64:
				{
				setState(758);
				predefined_type();
				setState(762);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,67,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(759);
						rank_specifier();
						}
						} 
					}
					setState(764);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,67,_ctx);
				}
				}
				break;
			case OPEN_PARENS:
				{
				setState(765);
				tuple_type();
				setState(769);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,68,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(766);
						rank_specifier();
						}
						} 
					}
					setState(771);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,68,_ctx);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Tuple_typeContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public List<Tuple_type_elementContext> tuple_type_element() {
			return getRuleContexts(Tuple_type_elementContext.class);
		}
		public Tuple_type_elementContext tuple_type_element(int i) {
			return getRuleContext(Tuple_type_elementContext.class,i);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Tuple_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tuple_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTuple_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTuple_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTuple_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Tuple_typeContext tuple_type() throws RecognitionException {
		Tuple_typeContext _localctx = new Tuple_typeContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_tuple_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(774);
			match(OPEN_PARENS);
			setState(775);
			tuple_type_element();
			setState(780);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(776);
				match(COMMA);
				setState(777);
				tuple_type_element();
				}
				}
				setState(782);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(783);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Tuple_type_elementContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Tuple_type_elementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tuple_type_element; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTuple_type_element(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTuple_type_element(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTuple_type_element(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Tuple_type_elementContext tuple_type_element() throws RecognitionException {
		Tuple_type_elementContext _localctx = new Tuple_type_elementContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_tuple_type_element);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(785);
			type();
			setState(787);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IDENTIFIER) {
				{
				setState(786);
				match(IDENTIFIER);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Class_typeContext extends ParserRuleContext {
		public Qualified_nameContext qualified_name() {
			return getRuleContext(Qualified_nameContext.class,0);
		}
		public Type_argument_listContext type_argument_list() {
			return getRuleContext(Type_argument_listContext.class,0);
		}
		public Class_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterClass_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitClass_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitClass_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_typeContext class_type() throws RecognitionException {
		Class_typeContext _localctx = new Class_typeContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_class_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(789);
			qualified_name();
			setState(791);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,72,_ctx) ) {
			case 1:
				{
				setState(790);
				type_argument_list();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Predefined_typeContext extends ParserRuleContext {
		public TerminalNode BOOL() { return getToken(NebulaParser.BOOL, 0); }
		public TerminalNode CHAR() { return getToken(NebulaParser.CHAR, 0); }
		public TerminalNode STRING() { return getToken(NebulaParser.STRING, 0); }
		public Numeric_typeContext numeric_type() {
			return getRuleContext(Numeric_typeContext.class,0);
		}
		public Predefined_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predefined_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterPredefined_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitPredefined_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitPredefined_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Predefined_typeContext predefined_type() throws RecognitionException {
		Predefined_typeContext _localctx = new Predefined_typeContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_predefined_type);
		try {
			setState(797);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BOOL:
				enterOuterAlt(_localctx, 1);
				{
				setState(793);
				match(BOOL);
				}
				break;
			case CHAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(794);
				match(CHAR);
				}
				break;
			case STRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(795);
				match(STRING);
				}
				break;
			case DECIMAL:
			case F32:
			case F64:
			case INT8:
			case INT16:
			case INT32:
			case INT64:
			case UINT8:
			case UINT16:
			case UINT32:
			case UINT64:
				enterOuterAlt(_localctx, 4);
				{
				setState(796);
				numeric_type();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Numeric_typeContext extends ParserRuleContext {
		public Integral_typeContext integral_type() {
			return getRuleContext(Integral_typeContext.class,0);
		}
		public Floating_point_typeContext floating_point_type() {
			return getRuleContext(Floating_point_typeContext.class,0);
		}
		public Numeric_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numeric_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterNumeric_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitNumeric_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitNumeric_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Numeric_typeContext numeric_type() throws RecognitionException {
		Numeric_typeContext _localctx = new Numeric_typeContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_numeric_type);
		try {
			setState(801);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INT8:
			case INT16:
			case INT32:
			case INT64:
			case UINT8:
			case UINT16:
			case UINT32:
			case UINT64:
				enterOuterAlt(_localctx, 1);
				{
				setState(799);
				integral_type();
				}
				break;
			case DECIMAL:
			case F32:
			case F64:
				enterOuterAlt(_localctx, 2);
				{
				setState(800);
				floating_point_type();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Integral_typeContext extends ParserRuleContext {
		public TerminalNode INT8() { return getToken(NebulaParser.INT8, 0); }
		public TerminalNode INT16() { return getToken(NebulaParser.INT16, 0); }
		public TerminalNode INT32() { return getToken(NebulaParser.INT32, 0); }
		public TerminalNode INT64() { return getToken(NebulaParser.INT64, 0); }
		public TerminalNode UINT8() { return getToken(NebulaParser.UINT8, 0); }
		public TerminalNode UINT16() { return getToken(NebulaParser.UINT16, 0); }
		public TerminalNode UINT32() { return getToken(NebulaParser.UINT32, 0); }
		public TerminalNode UINT64() { return getToken(NebulaParser.UINT64, 0); }
		public Integral_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_integral_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterIntegral_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitIntegral_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitIntegral_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Integral_typeContext integral_type() throws RecognitionException {
		Integral_typeContext _localctx = new Integral_typeContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_integral_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(803);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2161727853350092800L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Floating_point_typeContext extends ParserRuleContext {
		public TerminalNode F32() { return getToken(NebulaParser.F32, 0); }
		public TerminalNode F64() { return getToken(NebulaParser.F64, 0); }
		public TerminalNode DECIMAL() { return getToken(NebulaParser.DECIMAL, 0); }
		public Floating_point_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_floating_point_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterFloating_point_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitFloating_point_type(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitFloating_point_type(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Floating_point_typeContext floating_point_type() throws RecognitionException {
		Floating_point_typeContext _localctx = new Floating_point_typeContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_floating_point_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(805);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 100925440L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Rank_specifierContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACKET() { return getToken(NebulaParser.OPEN_BRACKET, 0); }
		public TerminalNode CLOSE_BRACKET() { return getToken(NebulaParser.CLOSE_BRACKET, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Rank_specifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rank_specifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterRank_specifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitRank_specifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitRank_specifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Rank_specifierContext rank_specifier() throws RecognitionException {
		Rank_specifierContext _localctx = new Rank_specifierContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_rank_specifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(807);
			match(OPEN_BRACKET);
			setState(809);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 90075291635941376L) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & 6455077631L) != 0)) {
				{
				setState(808);
				expression();
				}
			}

			setState(814);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(811);
				match(COMMA);
				}
				}
				setState(816);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(817);
			match(CLOSE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Generic_parameterContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public TerminalNode COLON() { return getToken(NebulaParser.COLON, 0); }
		public ConstraintContext constraint() {
			return getRuleContext(ConstraintContext.class,0);
		}
		public Generic_parameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_generic_parameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterGeneric_parameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitGeneric_parameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitGeneric_parameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Generic_parameterContext generic_parameter() throws RecognitionException {
		Generic_parameterContext _localctx = new Generic_parameterContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_generic_parameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(819);
			match(IDENTIFIER);
			setState(822);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(820);
				match(COLON);
				setState(821);
				constraint();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ConstraintContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public ConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constraint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitConstraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstraintContext constraint() throws RecognitionException {
		ConstraintContext _localctx = new ConstraintContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_constraint);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(824);
			match(IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_parametersContext extends ParserRuleContext {
		public TerminalNode LT() { return getToken(NebulaParser.LT, 0); }
		public List<Generic_parameterContext> generic_parameter() {
			return getRuleContexts(Generic_parameterContext.class);
		}
		public Generic_parameterContext generic_parameter(int i) {
			return getRuleContext(Generic_parameterContext.class,i);
		}
		public TerminalNode GT() { return getToken(NebulaParser.GT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Type_parametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_parameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterType_parameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitType_parameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitType_parameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_parametersContext type_parameters() throws RecognitionException {
		Type_parametersContext _localctx = new Type_parametersContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_type_parameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(826);
			match(LT);
			setState(827);
			generic_parameter();
			setState(832);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(828);
				match(COMMA);
				setState(829);
				generic_parameter();
				}
				}
				setState(834);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(835);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_argument_listContext extends ParserRuleContext {
		public TerminalNode LT() { return getToken(NebulaParser.LT, 0); }
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public Nested_gtContext nested_gt() {
			return getRuleContext(Nested_gtContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Type_argument_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_argument_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterType_argument_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitType_argument_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitType_argument_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Type_argument_listContext type_argument_list() throws RecognitionException {
		Type_argument_listContext _localctx = new Type_argument_listContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_type_argument_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(837);
			match(LT);
			setState(838);
			type();
			setState(843);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(839);
				match(COMMA);
				setState(840);
				type();
				}
				}
				setState(845);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(846);
			nested_gt();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Nested_gtContext extends ParserRuleContext {
		public TerminalNode GT() { return getToken(NebulaParser.GT, 0); }
		public Nested_gtContext nested_gt() {
			return getRuleContext(Nested_gtContext.class,0);
		}
		public Nested_gtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nested_gt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterNested_gt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitNested_gt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitNested_gt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Nested_gtContext nested_gt() throws RecognitionException {
		Nested_gtContext _localctx = new Nested_gtContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_nested_gt);
		try {
			setState(851);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,80,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(848);
				match(GT);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(849);
				match(GT);
				setState(850);
				nested_gt();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Tuple_literalContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Tuple_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tuple_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterTuple_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitTuple_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitTuple_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Tuple_literalContext tuple_literal() throws RecognitionException {
		Tuple_literalContext _localctx = new Tuple_literalContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_tuple_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(853);
			match(OPEN_PARENS);
			setState(854);
			argument();
			setState(857); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(855);
				match(COMMA);
				setState(856);
				argument();
				}
				}
				setState(859); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==COMMA );
			setState(861);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Parenthesized_expressionContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public Parenthesized_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parenthesized_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterParenthesized_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitParenthesized_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitParenthesized_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Parenthesized_expressionContext parenthesized_expression() throws RecognitionException {
		Parenthesized_expressionContext _localctx = new Parenthesized_expressionContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_parenthesized_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(863);
			match(OPEN_PARENS);
			setState(864);
			expression();
			setState(865);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NonAssignmentExpressionContext extends ParserRuleContext {
		public Binary_or_expressionContext binary_or_expression() {
			return getRuleContext(Binary_or_expressionContext.class,0);
		}
		public NonAssignmentExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonAssignmentExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterNonAssignmentExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitNonAssignmentExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitNonAssignmentExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonAssignmentExpressionContext nonAssignmentExpression() throws RecognitionException {
		NonAssignmentExpressionContext _localctx = new NonAssignmentExpressionContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_nonAssignmentExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(867);
			binary_or_expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpressionContext extends ParserRuleContext {
		public Assignment_expressionContext assignment_expression() {
			return getRuleContext(Assignment_expressionContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(869);
			assignment_expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Assignment_expressionContext extends ParserRuleContext {
		public Binary_or_expressionContext binary_or_expression() {
			return getRuleContext(Binary_or_expressionContext.class,0);
		}
		public Assignment_operatorContext assignment_operator() {
			return getRuleContext(Assignment_operatorContext.class,0);
		}
		public Assignment_expressionContext assignment_expression() {
			return getRuleContext(Assignment_expressionContext.class,0);
		}
		public Assignment_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterAssignment_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitAssignment_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitAssignment_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Assignment_expressionContext assignment_expression() throws RecognitionException {
		Assignment_expressionContext _localctx = new Assignment_expressionContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_assignment_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(871);
			binary_or_expression();
			setState(875);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 100)) & ~0x3f) == 0 && ((1L << (_la - 100)) & 8314881L) != 0)) {
				{
				setState(872);
				assignment_operator();
				setState(873);
				assignment_expression();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class New_expressionContext extends ParserRuleContext {
		public TerminalNode NEW() { return getToken(NebulaParser.NEW, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public New_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_new_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterNew_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitNew_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitNew_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final New_expressionContext new_expression() throws RecognitionException {
		New_expressionContext _localctx = new New_expressionContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_new_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(877);
			match(NEW);
			setState(878);
			type();
			setState(879);
			arguments();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Assignment_operatorContext extends ParserRuleContext {
		public TerminalNode ASSIGNMENT() { return getToken(NebulaParser.ASSIGNMENT, 0); }
		public TerminalNode OP_ADD_ASSIGNMENT() { return getToken(NebulaParser.OP_ADD_ASSIGNMENT, 0); }
		public TerminalNode OP_SUB_ASSIGNMENT() { return getToken(NebulaParser.OP_SUB_ASSIGNMENT, 0); }
		public TerminalNode OP_MULT_ASSIGNMENT() { return getToken(NebulaParser.OP_MULT_ASSIGNMENT, 0); }
		public TerminalNode OP_DIV_ASSIGNMENT() { return getToken(NebulaParser.OP_DIV_ASSIGNMENT, 0); }
		public TerminalNode OP_MOD_ASSIGNMENT() { return getToken(NebulaParser.OP_MOD_ASSIGNMENT, 0); }
		public TerminalNode OP_AND_ASSIGNMENT() { return getToken(NebulaParser.OP_AND_ASSIGNMENT, 0); }
		public TerminalNode OP_OR_ASSIGNMENT() { return getToken(NebulaParser.OP_OR_ASSIGNMENT, 0); }
		public TerminalNode OP_XOR_ASSIGNMENT() { return getToken(NebulaParser.OP_XOR_ASSIGNMENT, 0); }
		public TerminalNode OP_LEFT_SHIFT_ASSIGNMENT() { return getToken(NebulaParser.OP_LEFT_SHIFT_ASSIGNMENT, 0); }
		public Assignment_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment_operator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterAssignment_operator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitAssignment_operator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitAssignment_operator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Assignment_operatorContext assignment_operator() throws RecognitionException {
		Assignment_operatorContext _localctx = new Assignment_operatorContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_assignment_operator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(881);
			_la = _input.LA(1);
			if ( !(((((_la - 100)) & ~0x3f) == 0 && ((1L << (_la - 100)) & 8314881L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Binary_or_expressionContext extends ParserRuleContext {
		public List<Binary_and_expressionContext> binary_and_expression() {
			return getRuleContexts(Binary_and_expressionContext.class);
		}
		public Binary_and_expressionContext binary_and_expression(int i) {
			return getRuleContext(Binary_and_expressionContext.class,i);
		}
		public List<TerminalNode> OP_OR() { return getTokens(NebulaParser.OP_OR); }
		public TerminalNode OP_OR(int i) {
			return getToken(NebulaParser.OP_OR, i);
		}
		public Binary_or_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binary_or_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterBinary_or_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitBinary_or_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitBinary_or_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Binary_or_expressionContext binary_or_expression() throws RecognitionException {
		Binary_or_expressionContext _localctx = new Binary_or_expressionContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_binary_or_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(883);
			binary_and_expression();
			setState(888);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OP_OR) {
				{
				{
				setState(884);
				match(OP_OR);
				setState(885);
				binary_and_expression();
				}
				}
				setState(890);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Binary_and_expressionContext extends ParserRuleContext {
		public List<Inclusive_or_expressionContext> inclusive_or_expression() {
			return getRuleContexts(Inclusive_or_expressionContext.class);
		}
		public Inclusive_or_expressionContext inclusive_or_expression(int i) {
			return getRuleContext(Inclusive_or_expressionContext.class,i);
		}
		public List<TerminalNode> OP_AND() { return getTokens(NebulaParser.OP_AND); }
		public TerminalNode OP_AND(int i) {
			return getToken(NebulaParser.OP_AND, i);
		}
		public Binary_and_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binary_and_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterBinary_and_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitBinary_and_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitBinary_and_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Binary_and_expressionContext binary_and_expression() throws RecognitionException {
		Binary_and_expressionContext _localctx = new Binary_and_expressionContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_binary_and_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(891);
			inclusive_or_expression();
			setState(896);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OP_AND) {
				{
				{
				setState(892);
				match(OP_AND);
				setState(893);
				inclusive_or_expression();
				}
				}
				setState(898);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Inclusive_or_expressionContext extends ParserRuleContext {
		public List<Exclusive_or_expressionContext> exclusive_or_expression() {
			return getRuleContexts(Exclusive_or_expressionContext.class);
		}
		public Exclusive_or_expressionContext exclusive_or_expression(int i) {
			return getRuleContext(Exclusive_or_expressionContext.class,i);
		}
		public List<TerminalNode> PIPE() { return getTokens(NebulaParser.PIPE); }
		public TerminalNode PIPE(int i) {
			return getToken(NebulaParser.PIPE, i);
		}
		public Inclusive_or_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inclusive_or_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterInclusive_or_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitInclusive_or_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitInclusive_or_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Inclusive_or_expressionContext inclusive_or_expression() throws RecognitionException {
		Inclusive_or_expressionContext _localctx = new Inclusive_or_expressionContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_inclusive_or_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(899);
			exclusive_or_expression();
			setState(904);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PIPE) {
				{
				{
				setState(900);
				match(PIPE);
				setState(901);
				exclusive_or_expression();
				}
				}
				setState(906);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Exclusive_or_expressionContext extends ParserRuleContext {
		public List<And_expressionContext> and_expression() {
			return getRuleContexts(And_expressionContext.class);
		}
		public And_expressionContext and_expression(int i) {
			return getRuleContext(And_expressionContext.class,i);
		}
		public List<TerminalNode> CARET() { return getTokens(NebulaParser.CARET); }
		public TerminalNode CARET(int i) {
			return getToken(NebulaParser.CARET, i);
		}
		public Exclusive_or_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exclusive_or_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterExclusive_or_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitExclusive_or_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitExclusive_or_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Exclusive_or_expressionContext exclusive_or_expression() throws RecognitionException {
		Exclusive_or_expressionContext _localctx = new Exclusive_or_expressionContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_exclusive_or_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(907);
			and_expression();
			setState(912);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==CARET) {
				{
				{
				setState(908);
				match(CARET);
				setState(909);
				and_expression();
				}
				}
				setState(914);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class And_expressionContext extends ParserRuleContext {
		public List<Equality_expressionContext> equality_expression() {
			return getRuleContexts(Equality_expressionContext.class);
		}
		public Equality_expressionContext equality_expression(int i) {
			return getRuleContext(Equality_expressionContext.class,i);
		}
		public List<TerminalNode> AMP() { return getTokens(NebulaParser.AMP); }
		public TerminalNode AMP(int i) {
			return getToken(NebulaParser.AMP, i);
		}
		public And_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterAnd_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitAnd_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitAnd_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final And_expressionContext and_expression() throws RecognitionException {
		And_expressionContext _localctx = new And_expressionContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_and_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(915);
			equality_expression();
			setState(920);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AMP) {
				{
				{
				setState(916);
				match(AMP);
				setState(917);
				equality_expression();
				}
				}
				setState(922);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Equality_expressionContext extends ParserRuleContext {
		public List<Relational_expressionContext> relational_expression() {
			return getRuleContexts(Relational_expressionContext.class);
		}
		public Relational_expressionContext relational_expression(int i) {
			return getRuleContext(Relational_expressionContext.class,i);
		}
		public List<TerminalNode> OP_EQ() { return getTokens(NebulaParser.OP_EQ); }
		public TerminalNode OP_EQ(int i) {
			return getToken(NebulaParser.OP_EQ, i);
		}
		public List<TerminalNode> OP_NE() { return getTokens(NebulaParser.OP_NE); }
		public TerminalNode OP_NE(int i) {
			return getToken(NebulaParser.OP_NE, i);
		}
		public Equality_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_equality_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterEquality_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitEquality_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitEquality_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Equality_expressionContext equality_expression() throws RecognitionException {
		Equality_expressionContext _localctx = new Equality_expressionContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_equality_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(923);
			relational_expression();
			setState(928);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OP_EQ || _la==OP_NE) {
				{
				{
				setState(924);
				_la = _input.LA(1);
				if ( !(_la==OP_EQ || _la==OP_NE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(925);
				relational_expression();
				}
				}
				setState(930);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Relational_expressionContext extends ParserRuleContext {
		public List<Shift_expressionContext> shift_expression() {
			return getRuleContexts(Shift_expressionContext.class);
		}
		public Shift_expressionContext shift_expression(int i) {
			return getRuleContext(Shift_expressionContext.class,i);
		}
		public TerminalNode IS() { return getToken(NebulaParser.IS, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode LT() { return getToken(NebulaParser.LT, 0); }
		public TerminalNode GT() { return getToken(NebulaParser.GT, 0); }
		public TerminalNode OP_LE() { return getToken(NebulaParser.OP_LE, 0); }
		public TerminalNode OP_GE() { return getToken(NebulaParser.OP_GE, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Relational_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relational_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterRelational_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitRelational_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitRelational_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Relational_expressionContext relational_expression() throws RecognitionException {
		Relational_expressionContext _localctx = new Relational_expressionContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_relational_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(931);
			shift_expression();
			setState(939);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LT:
			case GT:
			case OP_LE:
			case OP_GE:
				{
				setState(932);
				_la = _input.LA(1);
				if ( !(((((_la - 101)) & ~0x3f) == 0 && ((1L << (_la - 101)) & 3075L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(933);
				shift_expression();
				}
				break;
			case IS:
				{
				setState(934);
				match(IS);
				setState(935);
				type();
				setState(937);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,89,_ctx) ) {
				case 1:
					{
					setState(936);
					match(IDENTIFIER);
					}
					break;
				}
				}
				break;
			case EOF:
			case BOOL:
			case CHAR:
			case CLASS:
			case CONST:
			case DECIMAL:
			case ENUM:
			case EXTERN:
			case FALSE:
			case F32:
			case F64:
			case IF:
			case INT8:
			case INT16:
			case INT32:
			case INT64:
			case NAMESPACE:
			case NEW:
			case MATCH:
			case OPERATOR:
			case OVERRIDE:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case STATIC:
			case STRING:
			case STRUCT:
			case TAG:
			case TAGGED:
			case THIS:
			case TRAIT:
			case TRUE:
			case UINT8:
			case UINT16:
			case UINT32:
			case UINT64:
			case UNION:
			case USE:
			case VAR:
			case VOID:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case OPEN_BRACE:
			case CLOSE_BRACE:
			case OPEN_BRACKET:
			case CLOSE_BRACKET:
			case OPEN_PARENS:
			case CLOSE_PARENS:
			case COMMA:
			case COLON:
			case SEMICOLON:
			case PLUS:
			case MINUS:
			case AMP:
			case PIPE:
			case CARET:
			case BANG:
			case TILDE:
			case ASSIGNMENT:
			case OP_AND:
			case OP_OR:
			case OP_EQ:
			case OP_NE:
			case OP_ADD_ASSIGNMENT:
			case OP_SUB_ASSIGNMENT:
			case OP_MULT_ASSIGNMENT:
			case OP_DIV_ASSIGNMENT:
			case OP_MOD_ASSIGNMENT:
			case OP_AND_ASSIGNMENT:
			case OP_OR_ASSIGNMENT:
			case OP_XOR_ASSIGNMENT:
			case OP_LEFT_SHIFT_ASSIGNMENT:
			case DOUBLE_CURLY_INSIDE:
			case REGULAR_CHAR_INSIDE:
			case REGULAR_STRING_INSIDE:
			case DOUBLE_QUOTE_INSIDE:
				break;
			default:
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Shift_expressionContext extends ParserRuleContext {
		public List<Additive_expressionContext> additive_expression() {
			return getRuleContexts(Additive_expressionContext.class);
		}
		public Additive_expressionContext additive_expression(int i) {
			return getRuleContext(Additive_expressionContext.class,i);
		}
		public List<Left_shiftContext> left_shift() {
			return getRuleContexts(Left_shiftContext.class);
		}
		public Left_shiftContext left_shift(int i) {
			return getRuleContext(Left_shiftContext.class,i);
		}
		public List<Right_shiftContext> right_shift() {
			return getRuleContexts(Right_shiftContext.class);
		}
		public Right_shiftContext right_shift(int i) {
			return getRuleContext(Right_shiftContext.class,i);
		}
		public Shift_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shift_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterShift_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitShift_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitShift_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Shift_expressionContext shift_expression() throws RecognitionException {
		Shift_expressionContext _localctx = new Shift_expressionContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_shift_expression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(941);
			additive_expression();
			setState(950);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,92,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(944);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case LT:
						{
						setState(942);
						left_shift();
						}
						break;
					case GT:
						{
						setState(943);
						right_shift();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(946);
					additive_expression();
					}
					} 
				}
				setState(952);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,92,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Left_shiftContext extends ParserRuleContext {
		public List<TerminalNode> LT() { return getTokens(NebulaParser.LT); }
		public TerminalNode LT(int i) {
			return getToken(NebulaParser.LT, i);
		}
		public Left_shiftContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_left_shift; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterLeft_shift(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitLeft_shift(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitLeft_shift(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Left_shiftContext left_shift() throws RecognitionException {
		Left_shiftContext _localctx = new Left_shiftContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_left_shift);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(953);
			match(LT);
			setState(954);
			match(LT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Right_shiftContext extends ParserRuleContext {
		public List<TerminalNode> GT() { return getTokens(NebulaParser.GT); }
		public TerminalNode GT(int i) {
			return getToken(NebulaParser.GT, i);
		}
		public Right_shiftContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_right_shift; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterRight_shift(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitRight_shift(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitRight_shift(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Right_shiftContext right_shift() throws RecognitionException {
		Right_shiftContext _localctx = new Right_shiftContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_right_shift);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(956);
			match(GT);
			setState(957);
			match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Additive_expressionContext extends ParserRuleContext {
		public List<Multiplicative_expressionContext> multiplicative_expression() {
			return getRuleContexts(Multiplicative_expressionContext.class);
		}
		public Multiplicative_expressionContext multiplicative_expression(int i) {
			return getRuleContext(Multiplicative_expressionContext.class,i);
		}
		public List<TerminalNode> PLUS() { return getTokens(NebulaParser.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(NebulaParser.PLUS, i);
		}
		public List<TerminalNode> MINUS() { return getTokens(NebulaParser.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(NebulaParser.MINUS, i);
		}
		public Additive_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_additive_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterAdditive_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitAdditive_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitAdditive_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Additive_expressionContext additive_expression() throws RecognitionException {
		Additive_expressionContext _localctx = new Additive_expressionContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_additive_expression);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(959);
			multiplicative_expression();
			setState(964);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(960);
					_la = _input.LA(1);
					if ( !(_la==PLUS || _la==MINUS) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(961);
					multiplicative_expression();
					}
					} 
				}
				setState(966);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Multiplicative_expressionContext extends ParserRuleContext {
		public List<Exponentiation_expressionContext> exponentiation_expression() {
			return getRuleContexts(Exponentiation_expressionContext.class);
		}
		public Exponentiation_expressionContext exponentiation_expression(int i) {
			return getRuleContext(Exponentiation_expressionContext.class,i);
		}
		public List<TerminalNode> STAR() { return getTokens(NebulaParser.STAR); }
		public TerminalNode STAR(int i) {
			return getToken(NebulaParser.STAR, i);
		}
		public List<TerminalNode> DIV() { return getTokens(NebulaParser.DIV); }
		public TerminalNode DIV(int i) {
			return getToken(NebulaParser.DIV, i);
		}
		public List<TerminalNode> PERCENT() { return getTokens(NebulaParser.PERCENT); }
		public TerminalNode PERCENT(int i) {
			return getToken(NebulaParser.PERCENT, i);
		}
		public Multiplicative_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiplicative_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterMultiplicative_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitMultiplicative_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitMultiplicative_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Multiplicative_expressionContext multiplicative_expression() throws RecognitionException {
		Multiplicative_expressionContext _localctx = new Multiplicative_expressionContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_multiplicative_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(967);
			exponentiation_expression();
			setState(972);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 91)) & ~0x3f) == 0 && ((1L << (_la - 91)) & 13L) != 0)) {
				{
				{
				setState(968);
				_la = _input.LA(1);
				if ( !(((((_la - 91)) & ~0x3f) == 0 && ((1L << (_la - 91)) & 13L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(969);
				exponentiation_expression();
				}
				}
				setState(974);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Exponentiation_expressionContext extends ParserRuleContext {
		public Unary_expressionContext unary_expression() {
			return getRuleContext(Unary_expressionContext.class,0);
		}
		public TerminalNode POW() { return getToken(NebulaParser.POW, 0); }
		public Exponentiation_expressionContext exponentiation_expression() {
			return getRuleContext(Exponentiation_expressionContext.class,0);
		}
		public Exponentiation_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exponentiation_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterExponentiation_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitExponentiation_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitExponentiation_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Exponentiation_expressionContext exponentiation_expression() throws RecognitionException {
		Exponentiation_expressionContext _localctx = new Exponentiation_expressionContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_exponentiation_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(975);
			unary_expression();
			setState(978);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==POW) {
				{
				setState(976);
				match(POW);
				setState(977);
				exponentiation_expression();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Unary_expressionContext extends ParserRuleContext {
		public Cast_expressionContext cast_expression() {
			return getRuleContext(Cast_expressionContext.class,0);
		}
		public Unary_expressionContext unary_expression() {
			return getRuleContext(Unary_expressionContext.class,0);
		}
		public TerminalNode PLUS() { return getToken(NebulaParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(NebulaParser.MINUS, 0); }
		public TerminalNode BANG() { return getToken(NebulaParser.BANG, 0); }
		public TerminalNode TILDE() { return getToken(NebulaParser.TILDE, 0); }
		public Primary_expressionContext primary_expression() {
			return getRuleContext(Primary_expressionContext.class,0);
		}
		public Unary_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unary_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterUnary_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitUnary_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitUnary_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Unary_expressionContext unary_expression() throws RecognitionException {
		Unary_expressionContext _localctx = new Unary_expressionContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_unary_expression);
		int _la;
		try {
			setState(984);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,96,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(980);
				cast_expression();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(981);
				_la = _input.LA(1);
				if ( !(((((_la - 89)) & ~0x3f) == 0 && ((1L << (_la - 89)) & 1539L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(982);
				unary_expression();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(983);
				primary_expression();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Cast_expressionContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public Unary_expressionContext unary_expression() {
			return getRuleContext(Unary_expressionContext.class,0);
		}
		public Cast_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cast_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterCast_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitCast_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitCast_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Cast_expressionContext cast_expression() throws RecognitionException {
		Cast_expressionContext _localctx = new Cast_expressionContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_cast_expression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(986);
			match(OPEN_PARENS);
			setState(987);
			type();
			setState(988);
			match(CLOSE_PARENS);
			setState(989);
			unary_expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Primary_expressionContext extends ParserRuleContext {
		public Primary_expression_startContext primary_expression_start() {
			return getRuleContext(Primary_expression_startContext.class,0);
		}
		public List<Postfix_operatorContext> postfix_operator() {
			return getRuleContexts(Postfix_operatorContext.class);
		}
		public Postfix_operatorContext postfix_operator(int i) {
			return getRuleContext(Postfix_operatorContext.class,i);
		}
		public Primary_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterPrimary_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitPrimary_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitPrimary_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Primary_expressionContext primary_expression() throws RecognitionException {
		Primary_expressionContext _localctx = new Primary_expressionContext(_ctx, getState());
		enterRule(_localctx, 212, RULE_primary_expression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(991);
			primary_expression_start();
			setState(995);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,97,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(992);
					postfix_operator();
					}
					} 
				}
				setState(997);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,97,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Primary_expression_startContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public Parenthesized_expressionContext parenthesized_expression() {
			return getRuleContext(Parenthesized_expressionContext.class,0);
		}
		public Tuple_literalContext tuple_literal() {
			return getRuleContext(Tuple_literalContext.class,0);
		}
		public If_expressionContext if_expression() {
			return getRuleContext(If_expressionContext.class,0);
		}
		public Match_expressionContext match_expression() {
			return getRuleContext(Match_expressionContext.class,0);
		}
		public Expression_blockContext expression_block() {
			return getRuleContext(Expression_blockContext.class,0);
		}
		public TerminalNode THIS() { return getToken(NebulaParser.THIS, 0); }
		public Array_literalContext array_literal() {
			return getRuleContext(Array_literalContext.class,0);
		}
		public New_expressionContext new_expression() {
			return getRuleContext(New_expressionContext.class,0);
		}
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public Qualified_nameContext qualified_name() {
			return getRuleContext(Qualified_nameContext.class,0);
		}
		public Primary_expression_startContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary_expression_start; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterPrimary_expression_start(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitPrimary_expression_start(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitPrimary_expression_start(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Primary_expression_startContext primary_expression_start() throws RecognitionException {
		Primary_expression_startContext _localctx = new Primary_expression_startContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_primary_expression_start);
		try {
			setState(1009);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,98,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(998);
				literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(999);
				parenthesized_expression();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1000);
				tuple_literal();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1001);
				if_expression();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1002);
				match_expression();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1003);
				expression_block();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1004);
				match(THIS);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1005);
				array_literal();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1006);
				new_expression();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1007);
				match(IDENTIFIER);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1008);
				qualified_name();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Postfix_operatorContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(NebulaParser.DOT, 0); }
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public TerminalNode INTEGER_LITERAL() { return getToken(NebulaParser.INTEGER_LITERAL, 0); }
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public Argument_listContext argument_list() {
			return getRuleContext(Argument_listContext.class,0);
		}
		public TerminalNode OPEN_BRACKET() { return getToken(NebulaParser.OPEN_BRACKET, 0); }
		public Expression_listContext expression_list() {
			return getRuleContext(Expression_listContext.class,0);
		}
		public TerminalNode CLOSE_BRACKET() { return getToken(NebulaParser.CLOSE_BRACKET, 0); }
		public TerminalNode OP_INC() { return getToken(NebulaParser.OP_INC, 0); }
		public TerminalNode OP_DEC() { return getToken(NebulaParser.OP_DEC, 0); }
		public Postfix_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_postfix_operator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterPostfix_operator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitPostfix_operator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitPostfix_operator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Postfix_operatorContext postfix_operator() throws RecognitionException {
		Postfix_operatorContext _localctx = new Postfix_operatorContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_postfix_operator);
		int _la;
		try {
			setState(1026);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,100,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1011);
				match(DOT);
				setState(1012);
				match(IDENTIFIER);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1013);
				match(DOT);
				setState(1014);
				match(INTEGER_LITERAL);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1015);
				match(OPEN_PARENS);
				setState(1017);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 90075291635941376L) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & 6455077631L) != 0)) {
					{
					setState(1016);
					argument_list();
					}
				}

				setState(1019);
				match(CLOSE_PARENS);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1020);
				match(OPEN_BRACKET);
				setState(1021);
				expression_list();
				setState(1022);
				match(CLOSE_BRACKET);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1024);
				match(OP_INC);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1025);
				match(OP_DEC);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Array_literalContext extends ParserRuleContext {
		public TerminalNode OPEN_BRACKET() { return getToken(NebulaParser.OPEN_BRACKET, 0); }
		public TerminalNode CLOSE_BRACKET() { return getToken(NebulaParser.CLOSE_BRACKET, 0); }
		public Expression_listContext expression_list() {
			return getRuleContext(Expression_listContext.class,0);
		}
		public Array_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterArray_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitArray_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitArray_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Array_literalContext array_literal() throws RecognitionException {
		Array_literalContext _localctx = new Array_literalContext(_ctx, getState());
		enterRule(_localctx, 218, RULE_array_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1028);
			match(OPEN_BRACKET);
			setState(1030);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 90075291635941376L) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & 6455077631L) != 0)) {
				{
				setState(1029);
				expression_list();
				}
			}

			setState(1032);
			match(CLOSE_BRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Expression_listContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Expression_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterExpression_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitExpression_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitExpression_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Expression_listContext expression_list() throws RecognitionException {
		Expression_listContext _localctx = new Expression_listContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_expression_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1034);
			expression();
			setState(1039);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1035);
				match(COMMA);
				setState(1036);
				expression();
				}
				}
				setState(1041);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentsContext extends ParserRuleContext {
		public TerminalNode OPEN_PARENS() { return getToken(NebulaParser.OPEN_PARENS, 0); }
		public TerminalNode CLOSE_PARENS() { return getToken(NebulaParser.CLOSE_PARENS, 0); }
		public Argument_listContext argument_list() {
			return getRuleContext(Argument_listContext.class,0);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1042);
			match(OPEN_PARENS);
			setState(1044);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 90075291635941376L) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & 6455077631L) != 0)) {
				{
				setState(1043);
				argument_list();
				}
			}

			setState(1046);
			match(CLOSE_PARENS);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Argument_listContext extends ParserRuleContext {
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public Argument_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterArgument_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitArgument_list(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitArgument_list(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Argument_listContext argument_list() throws RecognitionException {
		Argument_listContext _localctx = new Argument_listContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_argument_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1048);
			argument();
			setState(1053);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1049);
				match(COMMA);
				setState(1050);
				argument();
				}
				}
				setState(1055);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NamedArgumentContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(NebulaParser.IDENTIFIER, 0); }
		public TerminalNode COLON() { return getToken(NebulaParser.COLON, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public NamedArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterNamedArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitNamedArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitNamedArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedArgumentContext namedArgument() throws RecognitionException {
		NamedArgumentContext _localctx = new NamedArgumentContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_namedArgument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1056);
			match(IDENTIFIER);
			setState(1057);
			match(COLON);
			setState(1058);
			expression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PositionalArgumentContext extends ParserRuleContext {
		public NonAssignmentExpressionContext nonAssignmentExpression() {
			return getRuleContext(NonAssignmentExpressionContext.class,0);
		}
		public PositionalArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_positionalArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterPositionalArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitPositionalArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitPositionalArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PositionalArgumentContext positionalArgument() throws RecognitionException {
		PositionalArgumentContext _localctx = new PositionalArgumentContext(_ctx, getState());
		enterRule(_localctx, 228, RULE_positionalArgument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1060);
			nonAssignmentExpression();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentContext extends ParserRuleContext {
		public NamedArgumentContext namedArgument() {
			return getRuleContext(NamedArgumentContext.class,0);
		}
		public PositionalArgumentContext positionalArgument() {
			return getRuleContext(PositionalArgumentContext.class,0);
		}
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_argument);
		try {
			setState(1064);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1062);
				namedArgument();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1063);
				positionalArgument();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode TRUE() { return getToken(NebulaParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(NebulaParser.FALSE, 0); }
		public TerminalNode INTEGER_LITERAL() { return getToken(NebulaParser.INTEGER_LITERAL, 0); }
		public TerminalNode HEX_INTEGER_LITERAL() { return getToken(NebulaParser.HEX_INTEGER_LITERAL, 0); }
		public TerminalNode BIN_INTEGER_LITERAL() { return getToken(NebulaParser.BIN_INTEGER_LITERAL, 0); }
		public TerminalNode REAL_LITERAL() { return getToken(NebulaParser.REAL_LITERAL, 0); }
		public TerminalNode CHARACTER_LITERAL() { return getToken(NebulaParser.CHARACTER_LITERAL, 0); }
		public String_literalContext string_literal() {
			return getRuleContext(String_literalContext.class,0);
		}
		public TerminalNode LITERAL_ACCESS() { return getToken(NebulaParser.LITERAL_ACCESS, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_literal);
		try {
			setState(1075);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TRUE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1066);
				match(TRUE);
				}
				break;
			case FALSE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1067);
				match(FALSE);
				}
				break;
			case INTEGER_LITERAL:
				enterOuterAlt(_localctx, 3);
				{
				setState(1068);
				match(INTEGER_LITERAL);
				}
				break;
			case HEX_INTEGER_LITERAL:
				enterOuterAlt(_localctx, 4);
				{
				setState(1069);
				match(HEX_INTEGER_LITERAL);
				}
				break;
			case BIN_INTEGER_LITERAL:
				enterOuterAlt(_localctx, 5);
				{
				setState(1070);
				match(BIN_INTEGER_LITERAL);
				}
				break;
			case REAL_LITERAL:
				enterOuterAlt(_localctx, 6);
				{
				setState(1071);
				match(REAL_LITERAL);
				}
				break;
			case CHARACTER_LITERAL:
				enterOuterAlt(_localctx, 7);
				{
				setState(1072);
				match(CHARACTER_LITERAL);
				}
				break;
			case REGULAR_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
				enterOuterAlt(_localctx, 8);
				{
				setState(1073);
				string_literal();
				}
				break;
			case LITERAL_ACCESS:
				enterOuterAlt(_localctx, 9);
				{
				setState(1074);
				match(LITERAL_ACCESS);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class String_literalContext extends ParserRuleContext {
		public TerminalNode REGULAR_STRING() { return getToken(NebulaParser.REGULAR_STRING, 0); }
		public Interpolated_regular_stringContext interpolated_regular_string() {
			return getRuleContext(Interpolated_regular_stringContext.class,0);
		}
		public String_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterString_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitString_literal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitString_literal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final String_literalContext string_literal() throws RecognitionException {
		String_literalContext _localctx = new String_literalContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_string_literal);
		try {
			setState(1079);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case REGULAR_STRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(1077);
				match(REGULAR_STRING);
				}
				break;
			case INTERPOLATED_REGULAR_STRING_START:
				enterOuterAlt(_localctx, 2);
				{
				setState(1078);
				interpolated_regular_string();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interpolated_regular_stringContext extends ParserRuleContext {
		public TerminalNode INTERPOLATED_REGULAR_STRING_START() { return getToken(NebulaParser.INTERPOLATED_REGULAR_STRING_START, 0); }
		public TerminalNode DOUBLE_QUOTE_INSIDE() { return getToken(NebulaParser.DOUBLE_QUOTE_INSIDE, 0); }
		public List<Interpolated_regular_string_partContext> interpolated_regular_string_part() {
			return getRuleContexts(Interpolated_regular_string_partContext.class);
		}
		public Interpolated_regular_string_partContext interpolated_regular_string_part(int i) {
			return getRuleContext(Interpolated_regular_string_partContext.class,i);
		}
		public Interpolated_regular_stringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolated_regular_string; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterInterpolated_regular_string(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitInterpolated_regular_string(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitInterpolated_regular_string(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interpolated_regular_stringContext interpolated_regular_string() throws RecognitionException {
		Interpolated_regular_stringContext _localctx = new Interpolated_regular_stringContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_interpolated_regular_string);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1081);
			match(INTERPOLATED_REGULAR_STRING_START);
			setState(1085);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 90075291635941376L) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & 3746994896427330303L) != 0)) {
				{
				{
				setState(1082);
				interpolated_regular_string_part();
				}
				}
				setState(1087);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1088);
			match(DOUBLE_QUOTE_INSIDE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interpolated_regular_string_partContext extends ParserRuleContext {
		public Interpolated_string_expressionContext interpolated_string_expression() {
			return getRuleContext(Interpolated_string_expressionContext.class,0);
		}
		public TerminalNode DOUBLE_CURLY_INSIDE() { return getToken(NebulaParser.DOUBLE_CURLY_INSIDE, 0); }
		public TerminalNode REGULAR_CHAR_INSIDE() { return getToken(NebulaParser.REGULAR_CHAR_INSIDE, 0); }
		public TerminalNode REGULAR_STRING_INSIDE() { return getToken(NebulaParser.REGULAR_STRING_INSIDE, 0); }
		public Interpolated_regular_string_partContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolated_regular_string_part; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterInterpolated_regular_string_part(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitInterpolated_regular_string_part(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitInterpolated_regular_string_part(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interpolated_regular_string_partContext interpolated_regular_string_part() throws RecognitionException {
		Interpolated_regular_string_partContext _localctx = new Interpolated_regular_string_partContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_interpolated_regular_string_part);
		try {
			setState(1094);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FALSE:
			case IF:
			case NEW:
			case MATCH:
			case THIS:
			case TRUE:
			case IDENTIFIER:
			case LITERAL_ACCESS:
			case INTEGER_LITERAL:
			case HEX_INTEGER_LITERAL:
			case BIN_INTEGER_LITERAL:
			case REAL_LITERAL:
			case CHARACTER_LITERAL:
			case REGULAR_STRING:
			case INTERPOLATED_REGULAR_STRING_START:
			case OPEN_BRACE:
			case OPEN_BRACKET:
			case OPEN_PARENS:
			case PLUS:
			case MINUS:
			case BANG:
			case TILDE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1090);
				interpolated_string_expression();
				}
				break;
			case DOUBLE_CURLY_INSIDE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1091);
				match(DOUBLE_CURLY_INSIDE);
				}
				break;
			case REGULAR_CHAR_INSIDE:
				enterOuterAlt(_localctx, 3);
				{
				setState(1092);
				match(REGULAR_CHAR_INSIDE);
				}
				break;
			case REGULAR_STRING_INSIDE:
				enterOuterAlt(_localctx, 4);
				{
				setState(1093);
				match(REGULAR_STRING_INSIDE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interpolated_string_expressionContext extends ParserRuleContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(NebulaParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(NebulaParser.COMMA, i);
		}
		public TerminalNode COLON() { return getToken(NebulaParser.COLON, 0); }
		public List<TerminalNode> FORMAT_STRING() { return getTokens(NebulaParser.FORMAT_STRING); }
		public TerminalNode FORMAT_STRING(int i) {
			return getToken(NebulaParser.FORMAT_STRING, i);
		}
		public Interpolated_string_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolated_string_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).enterInterpolated_string_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof NebulaParserListener ) ((NebulaParserListener)listener).exitInterpolated_string_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof NebulaParserVisitor ) return ((NebulaParserVisitor<? extends T>)visitor).visitInterpolated_string_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Interpolated_string_expressionContext interpolated_string_expression() throws RecognitionException {
		Interpolated_string_expressionContext _localctx = new Interpolated_string_expressionContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_interpolated_string_expression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1096);
			expression();
			setState(1101);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1097);
				match(COMMA);
				setState(1098);
				expression();
				}
				}
				setState(1103);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1110);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(1104);
				match(COLON);
				setState(1106); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(1105);
					match(FORMAT_STRING);
					}
					}
					setState(1108); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==FORMAT_STRING );
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 9:
			return tag_expression_sempred((Tag_expressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean tag_expression_sempred(Tag_expressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		case 1:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u0086\u0459\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"+
		"\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"+
		"\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007"+
		"\u001b\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007"+
		"\u001e\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007"+
		"\"\u0002#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007"+
		"\'\u0002(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007"+
		",\u0002-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u0007"+
		"1\u00022\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u0007"+
		"6\u00027\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007"+
		";\u0002<\u0007<\u0002=\u0007=\u0002>\u0007>\u0002?\u0007?\u0002@\u0007"+
		"@\u0002A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007"+
		"E\u0002F\u0007F\u0002G\u0007G\u0002H\u0007H\u0002I\u0007I\u0002J\u0007"+
		"J\u0002K\u0007K\u0002L\u0007L\u0002M\u0007M\u0002N\u0007N\u0002O\u0007"+
		"O\u0002P\u0007P\u0002Q\u0007Q\u0002R\u0007R\u0002S\u0007S\u0002T\u0007"+
		"T\u0002U\u0007U\u0002V\u0007V\u0002W\u0007W\u0002X\u0007X\u0002Y\u0007"+
		"Y\u0002Z\u0007Z\u0002[\u0007[\u0002\\\u0007\\\u0002]\u0007]\u0002^\u0007"+
		"^\u0002_\u0007_\u0002`\u0007`\u0002a\u0007a\u0002b\u0007b\u0002c\u0007"+
		"c\u0002d\u0007d\u0002e\u0007e\u0002f\u0007f\u0002g\u0007g\u0002h\u0007"+
		"h\u0002i\u0007i\u0002j\u0007j\u0002k\u0007k\u0002l\u0007l\u0002m\u0007"+
		"m\u0002n\u0007n\u0002o\u0007o\u0002p\u0007p\u0002q\u0007q\u0002r\u0007"+
		"r\u0002s\u0007s\u0002t\u0007t\u0002u\u0007u\u0002v\u0007v\u0002w\u0007"+
		"w\u0002x\u0007x\u0001\u0000\u0005\u0000\u00f4\b\u0000\n\u0000\f\u0000"+
		"\u00f7\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0003\u0001\u00fc\b"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0005\u0001\u0102"+
		"\b\u0001\n\u0001\f\u0001\u0105\t\u0001\u0001\u0001\u0001\u0001\u0001\u0002"+
		"\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003\u010e\b\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004"+
		"\u0115\b\u0004\u0001\u0004\u0003\u0004\u0118\b\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0006\u0003\u0006\u011e\b\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0003\u0007\u012f\b\u0007\u0001\b\u0001\b\u0001\b\u0005"+
		"\b\u0134\b\b\n\b\f\b\u0137\t\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t"+
		"\u0001\t\u0001\t\u0001\t\u0003\t\u0141\b\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0005\t\u0149\b\t\n\t\f\t\u014c\t\t\u0001\n\u0001\n"+
		"\u0001\n\u0001\n\u0005\n\u0152\b\n\n\n\f\n\u0155\t\n\u0001\n\u0001\n\u0003"+
		"\n\u0159\b\n\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u015e\b\u000b"+
		"\n\u000b\f\u000b\u0161\t\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r"+
		"\u0001\r\u0001\r\u0001\r\u0005\r\u016b\b\r\n\r\f\r\u016e\t\r\u0001\r\u0001"+
		"\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0003\u000e"+
		"\u017d\b\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f"+
		"\u0003\u000f\u018a\b\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0011"+
		"\u0001\u0011\u0003\u0011\u0191\b\u0011\u0001\u0011\u0003\u0011\u0194\b"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0003\u0012\u019a"+
		"\b\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0005\u0013\u019f\b\u0013"+
		"\n\u0013\f\u0013\u01a2\t\u0013\u0001\u0014\u0001\u0014\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0003\u0016\u01b1\b\u0016\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001"+
		"\u0018\u0003\u0018\u01ba\b\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001"+
		"\u0019\u0003\u0019\u01c0\b\u0019\u0001\u0019\u0003\u0019\u01c3\b\u0019"+
		"\u0001\u0019\u0001\u0019\u0003\u0019\u01c7\b\u0019\u0001\u0019\u0001\u0019"+
		"\u0001\u001a\u0001\u001a\u0003\u001a\u01cd\b\u001a\u0001\u001b\u0001\u001b"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e"+
		"\u01dc\b\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001f\u0001\u001f\u0003\u001f\u01e5\b\u001f\u0001\u001f\u0001\u001f"+
		"\u0001 \u0001 \u0001 \u0001 \u0005 \u01ed\b \n \f \u01f0\t \u0001 \u0003"+
		" \u01f3\b \u0001 \u0001 \u0001!\u0001!\u0001!\u0001!\u0001\"\u0001\"\u0001"+
		"#\u0001#\u0001#\u0005#\u0200\b#\n#\f#\u0203\t#\u0001$\u0001$\u0001$\u0001"+
		"$\u0003$\u0209\b$\u0001%\u0001%\u0001%\u0001%\u0001&\u0001&\u0001&\u0001"+
		"\'\u0001\'\u0003\'\u0214\b\'\u0001\'\u0001\'\u0003\'\u0218\b\'\u0001\'"+
		"\u0001\'\u0001\'\u0001(\u0001(\u0001)\u0001)\u0001*\u0001*\u0001*\u0005"+
		"*\u0224\b*\n*\f*\u0227\t*\u0001+\u0001+\u0001,\u0001,\u0001,\u0005,\u022e"+
		"\b,\n,\f,\u0231\t,\u0001-\u0001-\u0001-\u0003-\u0236\b-\u0001.\u0001."+
		"\u0001.\u0001.\u0003.\u023c\b.\u0001.\u0001.\u0001.\u0001/\u0003/\u0242"+
		"\b/\u0001/\u0001/\u0001/\u0001/\u00010\u00010\u00030\u024a\b0\u00010\u0001"+
		"0\u00011\u00011\u00011\u00051\u0251\b1\n1\f1\u0254\t1\u00012\u00032\u0257"+
		"\b2\u00012\u00012\u00012\u00012\u00032\u025d\b2\u00013\u00013\u00013\u0001"+
		"3\u00033\u0263\b3\u00014\u00014\u00014\u00014\u00014\u00015\u00015\u0001"+
		"5\u00015\u00015\u00015\u00015\u00015\u00015\u00015\u00015\u00015\u0001"+
		"5\u00015\u00035\u0278\b5\u00016\u00016\u00016\u00036\u027d\b6\u00016\u0003"+
		"6\u0280\b6\u00016\u00016\u00017\u00017\u00057\u0286\b7\n7\f7\u0289\t7"+
		"\u00017\u00017\u00018\u00018\u00018\u00018\u00038\u0291\b8\u00019\u0001"+
		"9\u00019\u00039\u0296\b9\u00019\u00039\u0299\b9\u00019\u00019\u0001:\u0001"+
		":\u0005:\u029f\b:\n:\f:\u02a2\t:\u0001:\u0001:\u0001;\u0001;\u0001;\u0001"+
		";\u0003;\u02aa\b;\u0001<\u0001<\u0001<\u0001<\u0005<\u02b0\b<\n<\f<\u02b3"+
		"\t<\u0001=\u0001=\u0001=\u0001=\u0001>\u0001>\u0003>\u02bb\b>\u0001?\u0001"+
		"?\u0005?\u02bf\b?\n?\f?\u02c2\t?\u0001?\u0001?\u0001@\u0001@\u0001A\u0003"+
		"A\u02c9\bA\u0001A\u0001A\u0001A\u0003A\u02ce\bA\u0001A\u0001A\u0001B\u0001"+
		"B\u0001B\u0001B\u0005B\u02d6\bB\nB\fB\u02d9\tB\u0001B\u0003B\u02dc\bB"+
		"\u0001B\u0001B\u0001C\u0001C\u0001C\u0001C\u0001D\u0001D\u0003D\u02e6"+
		"\bD\u0001E\u0001E\u0003E\u02ea\bE\u0001F\u0001F\u0003F\u02ee\bF\u0001"+
		"F\u0001F\u0005F\u02f2\bF\nF\fF\u02f5\tF\u0001F\u0001F\u0005F\u02f9\bF"+
		"\nF\fF\u02fc\tF\u0001F\u0001F\u0005F\u0300\bF\nF\fF\u0303\tF\u0003F\u0305"+
		"\bF\u0001G\u0001G\u0001G\u0001G\u0005G\u030b\bG\nG\fG\u030e\tG\u0001G"+
		"\u0001G\u0001H\u0001H\u0003H\u0314\bH\u0001I\u0001I\u0003I\u0318\bI\u0001"+
		"J\u0001J\u0001J\u0001J\u0003J\u031e\bJ\u0001K\u0001K\u0003K\u0322\bK\u0001"+
		"L\u0001L\u0001M\u0001M\u0001N\u0001N\u0003N\u032a\bN\u0001N\u0005N\u032d"+
		"\bN\nN\fN\u0330\tN\u0001N\u0001N\u0001O\u0001O\u0001O\u0003O\u0337\bO"+
		"\u0001P\u0001P\u0001Q\u0001Q\u0001Q\u0001Q\u0005Q\u033f\bQ\nQ\fQ\u0342"+
		"\tQ\u0001Q\u0001Q\u0001R\u0001R\u0001R\u0001R\u0005R\u034a\bR\nR\fR\u034d"+
		"\tR\u0001R\u0001R\u0001S\u0001S\u0001S\u0003S\u0354\bS\u0001T\u0001T\u0001"+
		"T\u0001T\u0004T\u035a\bT\u000bT\fT\u035b\u0001T\u0001T\u0001U\u0001U\u0001"+
		"U\u0001U\u0001V\u0001V\u0001W\u0001W\u0001X\u0001X\u0001X\u0001X\u0003"+
		"X\u036c\bX\u0001Y\u0001Y\u0001Y\u0001Y\u0001Z\u0001Z\u0001[\u0001[\u0001"+
		"[\u0005[\u0377\b[\n[\f[\u037a\t[\u0001\\\u0001\\\u0001\\\u0005\\\u037f"+
		"\b\\\n\\\f\\\u0382\t\\\u0001]\u0001]\u0001]\u0005]\u0387\b]\n]\f]\u038a"+
		"\t]\u0001^\u0001^\u0001^\u0005^\u038f\b^\n^\f^\u0392\t^\u0001_\u0001_"+
		"\u0001_\u0005_\u0397\b_\n_\f_\u039a\t_\u0001`\u0001`\u0001`\u0005`\u039f"+
		"\b`\n`\f`\u03a2\t`\u0001a\u0001a\u0001a\u0001a\u0001a\u0001a\u0003a\u03aa"+
		"\ba\u0003a\u03ac\ba\u0001b\u0001b\u0001b\u0003b\u03b1\bb\u0001b\u0001"+
		"b\u0005b\u03b5\bb\nb\fb\u03b8\tb\u0001c\u0001c\u0001c\u0001d\u0001d\u0001"+
		"d\u0001e\u0001e\u0001e\u0005e\u03c3\be\ne\fe\u03c6\te\u0001f\u0001f\u0001"+
		"f\u0005f\u03cb\bf\nf\ff\u03ce\tf\u0001g\u0001g\u0001g\u0003g\u03d3\bg"+
		"\u0001h\u0001h\u0001h\u0001h\u0003h\u03d9\bh\u0001i\u0001i\u0001i\u0001"+
		"i\u0001i\u0001j\u0001j\u0005j\u03e2\bj\nj\fj\u03e5\tj\u0001k\u0001k\u0001"+
		"k\u0001k\u0001k\u0001k\u0001k\u0001k\u0001k\u0001k\u0001k\u0003k\u03f2"+
		"\bk\u0001l\u0001l\u0001l\u0001l\u0001l\u0001l\u0003l\u03fa\bl\u0001l\u0001"+
		"l\u0001l\u0001l\u0001l\u0001l\u0001l\u0003l\u0403\bl\u0001m\u0001m\u0003"+
		"m\u0407\bm\u0001m\u0001m\u0001n\u0001n\u0001n\u0005n\u040e\bn\nn\fn\u0411"+
		"\tn\u0001o\u0001o\u0003o\u0415\bo\u0001o\u0001o\u0001p\u0001p\u0001p\u0005"+
		"p\u041c\bp\np\fp\u041f\tp\u0001q\u0001q\u0001q\u0001q\u0001r\u0001r\u0001"+
		"s\u0001s\u0003s\u0429\bs\u0001t\u0001t\u0001t\u0001t\u0001t\u0001t\u0001"+
		"t\u0001t\u0001t\u0003t\u0434\bt\u0001u\u0001u\u0003u\u0438\bu\u0001v\u0001"+
		"v\u0005v\u043c\bv\nv\fv\u043f\tv\u0001v\u0001v\u0001w\u0001w\u0001w\u0001"+
		"w\u0003w\u0447\bw\u0001x\u0001x\u0001x\u0005x\u044c\bx\nx\fx\u044f\tx"+
		"\u0001x\u0001x\u0004x\u0453\bx\u000bx\fx\u0454\u0003x\u0457\bx\u0001x"+
		"\u0000\u0001\u0012y\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014"+
		"\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfh"+
		"jlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092"+
		"\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa"+
		"\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0\u00c2"+
		"\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8\u00da"+
		"\u00dc\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea\u00ec\u00ee\u00f0\u0000"+
		"\n\u0001\u0000,.\u0002\u0000\u0014\u0014&&\u0002\u0000\u001f\"9<\u0002"+
		"\u0000\u0012\u0012\u0019\u001a\u0003\u0000ddqsuz\u0001\u0000mn\u0002\u0000"+
		"efop\u0001\u0000YZ\u0002\u0000[[]^\u0002\u0000YZbc\u0491\u0000\u00f5\u0001"+
		"\u0000\u0000\u0000\u0002\u00fb\u0001\u0000\u0000\u0000\u0004\u0108\u0001"+
		"\u0000\u0000\u0000\u0006\u010a\u0001\u0000\u0000\u0000\b\u0117\u0001\u0000"+
		"\u0000\u0000\n\u0119\u0001\u0000\u0000\u0000\f\u011d\u0001\u0000\u0000"+
		"\u0000\u000e\u012e\u0001\u0000\u0000\u0000\u0010\u0130\u0001\u0000\u0000"+
		"\u0000\u0012\u0140\u0001\u0000\u0000\u0000\u0014\u014d\u0001\u0000\u0000"+
		"\u0000\u0016\u015a\u0001\u0000\u0000\u0000\u0018\u0162\u0001\u0000\u0000"+
		"\u0000\u001a\u0166\u0001\u0000\u0000\u0000\u001c\u017c\u0001\u0000\u0000"+
		"\u0000\u001e\u0189\u0001\u0000\u0000\u0000 \u018b\u0001\u0000\u0000\u0000"+
		"\"\u018e\u0001\u0000\u0000\u0000$\u0197\u0001\u0000\u0000\u0000&\u01a0"+
		"\u0001\u0000\u0000\u0000(\u01a3\u0001\u0000\u0000\u0000*\u01a5\u0001\u0000"+
		"\u0000\u0000,\u01ab\u0001\u0000\u0000\u0000.\u01b2\u0001\u0000\u0000\u0000"+
		"0\u01b6\u0001\u0000\u0000\u00002\u01bd\u0001\u0000\u0000\u00004\u01cc"+
		"\u0001\u0000\u0000\u00006\u01ce\u0001\u0000\u0000\u00008\u01d0\u0001\u0000"+
		"\u0000\u0000:\u01d4\u0001\u0000\u0000\u0000<\u01d8\u0001\u0000\u0000\u0000"+
		">\u01e2\u0001\u0000\u0000\u0000@\u01e8\u0001\u0000\u0000\u0000B\u01f6"+
		"\u0001\u0000\u0000\u0000D\u01fa\u0001\u0000\u0000\u0000F\u01fc\u0001\u0000"+
		"\u0000\u0000H\u0208\u0001\u0000\u0000\u0000J\u020a\u0001\u0000\u0000\u0000"+
		"L\u020e\u0001\u0000\u0000\u0000N\u0211\u0001\u0000\u0000\u0000P\u021c"+
		"\u0001\u0000\u0000\u0000R\u021e\u0001\u0000\u0000\u0000T\u0225\u0001\u0000"+
		"\u0000\u0000V\u0228\u0001\u0000\u0000\u0000X\u022a\u0001\u0000\u0000\u0000"+
		"Z\u0232\u0001\u0000\u0000\u0000\\\u0237\u0001\u0000\u0000\u0000^\u0241"+
		"\u0001\u0000\u0000\u0000`\u0247\u0001\u0000\u0000\u0000b\u024d\u0001\u0000"+
		"\u0000\u0000d\u0256\u0001\u0000\u0000\u0000f\u0262\u0001\u0000\u0000\u0000"+
		"h\u0264\u0001\u0000\u0000\u0000j\u0277\u0001\u0000\u0000\u0000l\u0279"+
		"\u0001\u0000\u0000\u0000n\u0283\u0001\u0000\u0000\u0000p\u0290\u0001\u0000"+
		"\u0000\u0000r\u0292\u0001\u0000\u0000\u0000t\u029c\u0001\u0000\u0000\u0000"+
		"v\u02a9\u0001\u0000\u0000\u0000x\u02ab\u0001\u0000\u0000\u0000z\u02b4"+
		"\u0001\u0000\u0000\u0000|\u02ba\u0001\u0000\u0000\u0000~\u02bc\u0001\u0000"+
		"\u0000\u0000\u0080\u02c5\u0001\u0000\u0000\u0000\u0082\u02c8\u0001\u0000"+
		"\u0000\u0000\u0084\u02d1\u0001\u0000\u0000\u0000\u0086\u02df\u0001\u0000"+
		"\u0000\u0000\u0088\u02e3\u0001\u0000\u0000\u0000\u008a\u02e9\u0001\u0000"+
		"\u0000\u0000\u008c\u02ed\u0001\u0000\u0000\u0000\u008e\u0306\u0001\u0000"+
		"\u0000\u0000\u0090\u0311\u0001\u0000\u0000\u0000\u0092\u0315\u0001\u0000"+
		"\u0000\u0000\u0094\u031d\u0001\u0000\u0000\u0000\u0096\u0321\u0001\u0000"+
		"\u0000\u0000\u0098\u0323\u0001\u0000\u0000\u0000\u009a\u0325\u0001\u0000"+
		"\u0000\u0000\u009c\u0327\u0001\u0000\u0000\u0000\u009e\u0333\u0001\u0000"+
		"\u0000\u0000\u00a0\u0338\u0001\u0000\u0000\u0000\u00a2\u033a\u0001\u0000"+
		"\u0000\u0000\u00a4\u0345\u0001\u0000\u0000\u0000\u00a6\u0353\u0001\u0000"+
		"\u0000\u0000\u00a8\u0355\u0001\u0000\u0000\u0000\u00aa\u035f\u0001\u0000"+
		"\u0000\u0000\u00ac\u0363\u0001\u0000\u0000\u0000\u00ae\u0365\u0001\u0000"+
		"\u0000\u0000\u00b0\u0367\u0001\u0000\u0000\u0000\u00b2\u036d\u0001\u0000"+
		"\u0000\u0000\u00b4\u0371\u0001\u0000\u0000\u0000\u00b6\u0373\u0001\u0000"+
		"\u0000\u0000\u00b8\u037b\u0001\u0000\u0000\u0000\u00ba\u0383\u0001\u0000"+
		"\u0000\u0000\u00bc\u038b\u0001\u0000\u0000\u0000\u00be\u0393\u0001\u0000"+
		"\u0000\u0000\u00c0\u039b\u0001\u0000\u0000\u0000\u00c2\u03a3\u0001\u0000"+
		"\u0000\u0000\u00c4\u03ad\u0001\u0000\u0000\u0000\u00c6\u03b9\u0001\u0000"+
		"\u0000\u0000\u00c8\u03bc\u0001\u0000\u0000\u0000\u00ca\u03bf\u0001\u0000"+
		"\u0000\u0000\u00cc\u03c7\u0001\u0000\u0000\u0000\u00ce\u03cf\u0001\u0000"+
		"\u0000\u0000\u00d0\u03d8\u0001\u0000\u0000\u0000\u00d2\u03da\u0001\u0000"+
		"\u0000\u0000\u00d4\u03df\u0001\u0000\u0000\u0000\u00d6\u03f1\u0001\u0000"+
		"\u0000\u0000\u00d8\u0402\u0001\u0000\u0000\u0000\u00da\u0404\u0001\u0000"+
		"\u0000\u0000\u00dc\u040a\u0001\u0000\u0000\u0000\u00de\u0412\u0001\u0000"+
		"\u0000\u0000\u00e0\u0418\u0001\u0000\u0000\u0000\u00e2\u0420\u0001\u0000"+
		"\u0000\u0000\u00e4\u0424\u0001\u0000\u0000\u0000\u00e6\u0428\u0001\u0000"+
		"\u0000\u0000\u00e8\u0433\u0001\u0000\u0000\u0000\u00ea\u0437\u0001\u0000"+
		"\u0000\u0000\u00ec\u0439\u0001\u0000\u0000\u0000\u00ee\u0446\u0001\u0000"+
		"\u0000\u0000\u00f0\u0448\u0001\u0000\u0000\u0000\u00f2\u00f4\u0003\u001c"+
		"\u000e\u0000\u00f3\u00f2\u0001\u0000\u0000\u0000\u00f4\u00f7\u0001\u0000"+
		"\u0000\u0000\u00f5\u00f3\u0001\u0000\u0000\u0000\u00f5\u00f6\u0001\u0000"+
		"\u0000\u0000\u00f6\u00f8\u0001\u0000\u0000\u0000\u00f7\u00f5\u0001\u0000"+
		"\u0000\u0000\u00f8\u00f9\u0005\u0000\u0000\u0001\u00f9\u0001\u0001\u0000"+
		"\u0000\u0000\u00fa\u00fc\u0003T*\u0000\u00fb\u00fa\u0001\u0000\u0000\u0000"+
		"\u00fb\u00fc\u0001\u0000\u0000\u0000\u00fc\u00fd\u0001\u0000\u0000\u0000"+
		"\u00fd\u00fe\u0005\u0017\u0000\u0000\u00fe\u00ff\u0005J\u0000\u0000\u00ff"+
		"\u0103\u0005N\u0000\u0000\u0100\u0102\u0003\u0004\u0002\u0000\u0101\u0100"+
		"\u0001\u0000\u0000\u0000\u0102\u0105\u0001\u0000\u0000\u0000\u0103\u0101"+
		"\u0001\u0000\u0000\u0000\u0103\u0104\u0001\u0000\u0000\u0000\u0104\u0106"+
		"\u0001\u0000\u0000\u0000\u0105\u0103\u0001\u0000\u0000\u0000\u0106\u0107"+
		"\u0005O\u0000\u0000\u0107\u0003\u0001\u0000\u0000\u0000\u0108\u0109\u0003"+
		"\\.\u0000\u0109\u0005\u0001\u0000\u0000\u0000\u010a\u010b\u0005>\u0000"+
		"\u0000\u010b\u010d\u0003\u0016\u000b\u0000\u010c\u010e\u0003\b\u0004\u0000"+
		"\u010d\u010c\u0001\u0000\u0000\u0000\u010d\u010e\u0001\u0000\u0000\u0000"+
		"\u010e\u010f\u0001\u0000\u0000\u0000\u010f\u0110\u0005X\u0000\u0000\u0110"+
		"\u0007\u0001\u0000\u0000\u0000\u0111\u0112\u0005V\u0000\u0000\u0112\u0114"+
		"\u0005C\u0000\u0000\u0113\u0115\u0003\n\u0005\u0000\u0114\u0113\u0001"+
		"\u0000\u0000\u0000\u0114\u0115\u0001\u0000\u0000\u0000\u0115\u0118\u0001"+
		"\u0000\u0000\u0000\u0116\u0118\u0003\n\u0005\u0000\u0117\u0111\u0001\u0000"+
		"\u0000\u0000\u0117\u0116\u0001\u0000\u0000\u0000\u0118\t\u0001\u0000\u0000"+
		"\u0000\u0119\u011a\u0005\b\u0000\u0000\u011a\u011b\u0005C\u0000\u0000"+
		"\u011b\u000b\u0001\u0000\u0000\u0000\u011c\u011e\u0003P(\u0000\u011d\u011c"+
		"\u0001\u0000\u0000\u0000\u011d\u011e\u0001\u0000\u0000\u0000\u011e\u011f"+
		"\u0001\u0000\u0000\u0000\u011f\u0120\u00054\u0000\u0000\u0120\u0121\u0003"+
		"\u000e\u0007\u0000\u0121\u0122\u0005\b\u0000\u0000\u0122\u0123\u0005C"+
		"\u0000\u0000\u0123\u0124\u0005X\u0000\u0000\u0124\r\u0001\u0000\u0000"+
		"\u0000\u0125\u012f\u0003\u008cF\u0000\u0126\u0127\u0005N\u0000\u0000\u0127"+
		"\u0128\u0003\u0010\b\u0000\u0128\u0129\u0005O\u0000\u0000\u0129\u012f"+
		"\u0001\u0000\u0000\u0000\u012a\u012b\u0005N\u0000\u0000\u012b\u012c\u0003"+
		"\u0012\t\u0000\u012c\u012d\u0005O\u0000\u0000\u012d\u012f\u0001\u0000"+
		"\u0000\u0000\u012e\u0125\u0001\u0000\u0000\u0000\u012e\u0126\u0001\u0000"+
		"\u0000\u0000\u012e\u012a\u0001\u0000\u0000\u0000\u012f\u000f\u0001\u0000"+
		"\u0000\u0000\u0130\u0135\u0003\u008cF\u0000\u0131\u0132\u0005U\u0000\u0000"+
		"\u0132\u0134\u0003\u008cF\u0000\u0133\u0131\u0001\u0000\u0000\u0000\u0134"+
		"\u0137\u0001\u0000\u0000\u0000\u0135\u0133\u0001\u0000\u0000\u0000\u0135"+
		"\u0136\u0001\u0000\u0000\u0000\u0136\u0011\u0001\u0000\u0000\u0000\u0137"+
		"\u0135\u0001\u0000\u0000\u0000\u0138\u0139\u0006\t\uffff\uffff\u0000\u0139"+
		"\u013a\u0005R\u0000\u0000\u013a\u013b\u0003\u0012\t\u0000\u013b\u013c"+
		"\u0005S\u0000\u0000\u013c\u0141\u0001\u0000\u0000\u0000\u013d\u013e\u0005"+
		"b\u0000\u0000\u013e\u0141\u0003\u0012\t\u0004\u013f\u0141\u0003\u008c"+
		"F\u0000\u0140\u0138\u0001\u0000\u0000\u0000\u0140\u013d\u0001\u0000\u0000"+
		"\u0000\u0140\u013f\u0001\u0000\u0000\u0000\u0141\u014a\u0001\u0000\u0000"+
		"\u0000\u0142\u0143\n\u0003\u0000\u0000\u0143\u0144\u0005_\u0000\u0000"+
		"\u0144\u0149\u0003\u0012\t\u0004\u0145\u0146\n\u0002\u0000\u0000\u0146"+
		"\u0147\u0005`\u0000\u0000\u0147\u0149\u0003\u0012\t\u0003\u0148\u0142"+
		"\u0001\u0000\u0000\u0000\u0148\u0145\u0001\u0000\u0000\u0000\u0149\u014c"+
		"\u0001\u0000\u0000\u0000\u014a\u0148\u0001\u0000\u0000\u0000\u014a\u014b"+
		"\u0001\u0000\u0000\u0000\u014b\u0013\u0001\u0000\u0000\u0000\u014c\u014a"+
		"\u0001\u0000\u0000\u0000\u014d\u014e\u0005\'\u0000\u0000\u014e\u0158\u0003"+
		"\u0016\u000b\u0000\u014f\u0153\u0005N\u0000\u0000\u0150\u0152\u0003\u001c"+
		"\u000e\u0000\u0151\u0150\u0001\u0000\u0000\u0000\u0152\u0155\u0001\u0000"+
		"\u0000\u0000\u0153\u0151\u0001\u0000\u0000\u0000\u0153\u0154\u0001\u0000"+
		"\u0000\u0000\u0154\u0156\u0001\u0000\u0000\u0000\u0155\u0153\u0001\u0000"+
		"\u0000\u0000\u0156\u0159\u0005O\u0000\u0000\u0157\u0159\u0005X\u0000\u0000"+
		"\u0158\u014f\u0001\u0000\u0000\u0000\u0158\u0157\u0001\u0000\u0000\u0000"+
		"\u0159\u0015\u0001\u0000\u0000\u0000\u015a\u015f\u0005C\u0000\u0000\u015b"+
		"\u015c\u0005V\u0000\u0000\u015c\u015e\u0005C\u0000\u0000\u015d\u015b\u0001"+
		"\u0000\u0000\u0000\u015e\u0161\u0001\u0000\u0000\u0000\u015f\u015d\u0001"+
		"\u0000\u0000\u0000\u015f\u0160\u0001\u0000\u0000\u0000\u0160\u0017\u0001"+
		"\u0000\u0000\u0000\u0161\u015f\u0001\u0000\u0000\u0000\u0162\u0163\u0005"+
		"\u0016\u0000\u0000\u0163\u0164\u0005C\u0000\u0000\u0164\u0165\u0003\u001a"+
		"\r\u0000\u0165\u0019\u0001\u0000\u0000\u0000\u0166\u0167\u0005N\u0000"+
		"\u0000\u0167\u016c\u0005C\u0000\u0000\u0168\u0169\u0005U\u0000\u0000\u0169"+
		"\u016b\u0005C\u0000\u0000\u016a\u0168\u0001\u0000\u0000\u0000\u016b\u016e"+
		"\u0001\u0000\u0000\u0000\u016c\u016a\u0001\u0000\u0000\u0000\u016c\u016d"+
		"\u0001\u0000\u0000\u0000\u016d\u016f\u0001\u0000\u0000\u0000\u016e\u016c"+
		"\u0001\u0000\u0000\u0000\u016f\u0170\u0005O\u0000\u0000\u0170\u001b\u0001"+
		"\u0000\u0000\u0000\u0171\u017d\u0003\u0006\u0003\u0000\u0172\u017d\u0003"+
		"\f\u0006\u0000\u0173\u017d\u0003\u0018\f\u0000\u0174\u017d\u0003L&\u0000"+
		"\u0175\u017d\u0003\\.\u0000\u0176\u017d\u0003l6\u0000\u0177\u017d\u0003"+
		"r9\u0000\u0178\u017d\u0003z=\u0000\u0179\u017d\u0003\u0082A\u0000\u017a"+
		"\u017d\u0003\u0014\n\u0000\u017b\u017d\u0003\u0002\u0001\u0000\u017c\u0171"+
		"\u0001\u0000\u0000\u0000\u017c\u0172\u0001\u0000\u0000\u0000\u017c\u0173"+
		"\u0001\u0000\u0000\u0000\u017c\u0174\u0001\u0000\u0000\u0000\u017c\u0175"+
		"\u0001\u0000\u0000\u0000\u017c\u0176\u0001\u0000\u0000\u0000\u017c\u0177"+
		"\u0001\u0000\u0000\u0000\u017c\u0178\u0001\u0000\u0000\u0000\u017c\u0179"+
		"\u0001\u0000\u0000\u0000\u017c\u017a\u0001\u0000\u0000\u0000\u017c\u017b"+
		"\u0001\u0000\u0000\u0000\u017d\u001d\u0001\u0000\u0000\u0000\u017e\u018a"+
		"\u0003\u0006\u0003\u0000\u017f\u018a\u0003\f\u0006\u0000\u0180\u018a\u0003"+
		"L&\u0000\u0181\u018a\u0003N\'\u0000\u0182\u018a\u0003$\u0012\u0000\u0183"+
		"\u018a\u0003,\u0016\u0000\u0184\u018a\u00030\u0018\u0000\u0185\u018a\u0003"+
		"8\u001c\u0000\u0186\u018a\u0003:\u001d\u0000\u0187\u018a\u0003>\u001f"+
		"\u0000\u0188\u018a\u0003 \u0010\u0000\u0189\u017e\u0001\u0000\u0000\u0000"+
		"\u0189\u017f\u0001\u0000\u0000\u0000\u0189\u0180\u0001\u0000\u0000\u0000"+
		"\u0189\u0181\u0001\u0000\u0000\u0000\u0189\u0182\u0001\u0000\u0000\u0000"+
		"\u0189\u0183\u0001\u0000\u0000\u0000\u0189\u0184\u0001\u0000\u0000\u0000"+
		"\u0189\u0185\u0001\u0000\u0000\u0000\u0189\u0186\u0001\u0000\u0000\u0000"+
		"\u0189\u0187\u0001\u0000\u0000\u0000\u0189\u0188\u0001\u0000\u0000\u0000"+
		"\u018a\u001f\u0001\u0000\u0000\u0000\u018b\u018c\u0003\u00aeW\u0000\u018c"+
		"\u018d\u0005X\u0000\u0000\u018d!\u0001\u0000\u0000\u0000\u018e\u0190\u0005"+
		"N\u0000\u0000\u018f\u0191\u0003&\u0013\u0000\u0190\u018f\u0001\u0000\u0000"+
		"\u0000\u0190\u0191\u0001\u0000\u0000\u0000\u0191\u0193\u0001\u0000\u0000"+
		"\u0000\u0192\u0194\u0003(\u0014\u0000\u0193\u0192\u0001\u0000\u0000\u0000"+
		"\u0193\u0194\u0001\u0000\u0000\u0000\u0194\u0195\u0001\u0000\u0000\u0000"+
		"\u0195\u0196\u0005O\u0000\u0000\u0196#\u0001\u0000\u0000\u0000\u0197\u0199"+
		"\u0005N\u0000\u0000\u0198\u019a\u0003&\u0013\u0000\u0199\u0198\u0001\u0000"+
		"\u0000\u0000\u0199\u019a\u0001\u0000\u0000\u0000\u019a\u019b\u0001\u0000"+
		"\u0000\u0000\u019b\u019c\u0005O\u0000\u0000\u019c%\u0001\u0000\u0000\u0000"+
		"\u019d\u019f\u0003\u001e\u000f\u0000\u019e\u019d\u0001\u0000\u0000\u0000"+
		"\u019f\u01a2\u0001\u0000\u0000\u0000\u01a0\u019e\u0001\u0000\u0000\u0000"+
		"\u01a0\u01a1\u0001\u0000\u0000\u0000\u01a1\'\u0001\u0000\u0000\u0000\u01a2"+
		"\u01a0\u0001\u0000\u0000\u0000\u01a3\u01a4\u0003\u00aeW\u0000\u01a4)\u0001"+
		"\u0000\u0000\u0000\u01a5\u01a6\u0005\u001d\u0000\u0000\u01a6\u01a7\u0003"+
		"\u00aaU\u0000\u01a7\u01a8\u0003\"\u0011\u0000\u01a8\u01a9\u0005\u0015"+
		"\u0000\u0000\u01a9\u01aa\u0003\"\u0011\u0000\u01aa+\u0001\u0000\u0000"+
		"\u0000\u01ab\u01ac\u0005\u001d\u0000\u0000\u01ac\u01ad\u0003\u00aaU\u0000"+
		"\u01ad\u01b0\u0003\u001e\u000f\u0000\u01ae\u01af\u0005\u0015\u0000\u0000"+
		"\u01af\u01b1\u0003\u001e\u000f\u0000\u01b0\u01ae\u0001\u0000\u0000\u0000"+
		"\u01b0\u01b1\u0001\u0000\u0000\u0000\u01b1-\u0001\u0000\u0000\u0000\u01b2"+
		"\u01b3\u0005)\u0000\u0000\u01b3\u01b4\u0003\u00aaU\u0000\u01b4\u01b5\u0003"+
		"@ \u0000\u01b5/\u0001\u0000\u0000\u0000\u01b6\u01b9\u0005\u001b\u0000"+
		"\u0000\u01b7\u01ba\u00032\u0019\u0000\u01b8\u01ba\u0003\u00aaU\u0000\u01b9"+
		"\u01b7\u0001\u0000\u0000\u0000\u01b9\u01b8\u0001\u0000\u0000\u0000\u01ba"+
		"\u01bb\u0001\u0000\u0000\u0000\u01bb\u01bc\u0003\u001e\u000f\u0000\u01bc"+
		"1\u0001\u0000\u0000\u0000\u01bd\u01bf\u0005R\u0000\u0000\u01be\u01c0\u0003"+
		"4\u001a\u0000\u01bf\u01be\u0001\u0000\u0000\u0000\u01bf\u01c0\u0001\u0000"+
		"\u0000\u0000\u01c0\u01c2\u0001\u0000\u0000\u0000\u01c1\u01c3\u0003\u00ae"+
		"W\u0000\u01c2\u01c1\u0001\u0000\u0000\u0000\u01c2\u01c3\u0001\u0000\u0000"+
		"\u0000\u01c3\u01c4\u0001\u0000\u0000\u0000\u01c4\u01c6\u0005X\u0000\u0000"+
		"\u01c5\u01c7\u00036\u001b\u0000\u01c6\u01c5\u0001\u0000\u0000\u0000\u01c6"+
		"\u01c7\u0001\u0000\u0000\u0000\u01c7\u01c8\u0001\u0000\u0000\u0000\u01c8"+
		"\u01c9\u0005S\u0000\u0000\u01c93\u0001\u0000\u0000\u0000\u01ca\u01cd\u0003"+
		"N\'\u0000\u01cb\u01cd\u0003\u00dcn\u0000\u01cc\u01ca\u0001\u0000\u0000"+
		"\u0000\u01cc\u01cb\u0001\u0000\u0000\u0000\u01cd5\u0001\u0000\u0000\u0000"+
		"\u01ce\u01cf\u0003\u00dcn\u0000\u01cf7\u0001\u0000\u0000\u0000\u01d0\u01d1"+
		"\u0005A\u0000\u0000\u01d1\u01d2\u0003\u00aaU\u0000\u01d2\u01d3\u0003\u001e"+
		"\u000f\u0000\u01d39\u0001\u0000\u0000\u0000\u01d4\u01d5\u0005\u001c\u0000"+
		"\u0000\u01d5\u01d6\u0003<\u001e\u0000\u01d6\u01d7\u0003$\u0012\u0000\u01d7"+
		";\u0001\u0000\u0000\u0000\u01d8\u01db\u0005R\u0000\u0000\u01d9\u01dc\u0005"+
		"?\u0000\u0000\u01da\u01dc\u0003\u008cF\u0000\u01db\u01d9\u0001\u0000\u0000"+
		"\u0000\u01db\u01da\u0001\u0000\u0000\u0000\u01dc\u01dd\u0001\u0000\u0000"+
		"\u0000\u01dd\u01de\u0005C\u0000\u0000\u01de\u01df\u0005\u001e\u0000\u0000"+
		"\u01df\u01e0\u0003\u00aeW\u0000\u01e0\u01e1\u0005S\u0000\u0000\u01e1="+
		"\u0001\u0000\u0000\u0000\u01e2\u01e4\u0005/\u0000\u0000\u01e3\u01e5\u0003"+
		"\u00aeW\u0000\u01e4\u01e3\u0001\u0000\u0000\u0000\u01e4\u01e5\u0001\u0000"+
		"\u0000\u0000\u01e5\u01e6\u0001\u0000\u0000\u0000\u01e6\u01e7\u0005X\u0000"+
		"\u0000\u01e7?\u0001\u0000\u0000\u0000\u01e8\u01e9\u0005N\u0000\u0000\u01e9"+
		"\u01ee\u0003B!\u0000\u01ea\u01eb\u0005U\u0000\u0000\u01eb\u01ed\u0003"+
		"B!\u0000\u01ec\u01ea\u0001\u0000\u0000\u0000\u01ed\u01f0\u0001\u0000\u0000"+
		"\u0000\u01ee\u01ec\u0001\u0000\u0000\u0000\u01ee\u01ef\u0001\u0000\u0000"+
		"\u0000\u01ef\u01f2\u0001\u0000\u0000\u0000\u01f0\u01ee\u0001\u0000\u0000"+
		"\u0000\u01f1\u01f3\u0005U\u0000\u0000\u01f2\u01f1\u0001\u0000\u0000\u0000"+
		"\u01f2\u01f3\u0001\u0000\u0000\u0000\u01f3\u01f4\u0001\u0000\u0000\u0000"+
		"\u01f4\u01f5\u0005O\u0000\u0000\u01f5A\u0001\u0000\u0000\u0000\u01f6\u01f7"+
		"\u0003D\"\u0000\u01f7\u01f8\u0005|\u0000\u0000\u01f8\u01f9\u0003\u00ae"+
		"W\u0000\u01f9C\u0001\u0000\u0000\u0000\u01fa\u01fb\u0003F#\u0000\u01fb"+
		"E\u0001\u0000\u0000\u0000\u01fc\u0201\u0003H$\u0000\u01fd\u01fe\u0005"+
		"`\u0000\u0000\u01fe\u0200\u0003H$\u0000\u01ff\u01fd\u0001\u0000\u0000"+
		"\u0000\u0200\u0203\u0001\u0000\u0000\u0000\u0201\u01ff\u0001\u0000\u0000"+
		"\u0000\u0201\u0202\u0001\u0000\u0000\u0000\u0202G\u0001\u0000\u0000\u0000"+
		"\u0203\u0201\u0001\u0000\u0000\u0000\u0204\u0209\u0003\u00e8t\u0000\u0205"+
		"\u0209\u0005B\u0000\u0000\u0206\u0209\u0005C\u0000\u0000\u0207\u0209\u0003"+
		"J%\u0000\u0208\u0204\u0001\u0000\u0000\u0000\u0208\u0205\u0001\u0000\u0000"+
		"\u0000\u0208\u0206\u0001\u0000\u0000\u0000\u0208\u0207\u0001\u0000\u0000"+
		"\u0000\u0209I\u0001\u0000\u0000\u0000\u020a\u020b\u0005R\u0000\u0000\u020b"+
		"\u020c\u0003D\"\u0000\u020c\u020d\u0005S\u0000\u0000\u020dK\u0001\u0000"+
		"\u0000\u0000\u020e\u020f\u0005\u0010\u0000\u0000\u020f\u0210\u0003N\'"+
		"\u0000\u0210M\u0001\u0000\u0000\u0000\u0211\u0213\u0003T*\u0000\u0212"+
		"\u0214\u0005\u0010\u0000\u0000\u0213\u0212\u0001\u0000\u0000\u0000\u0213"+
		"\u0214\u0001\u0000\u0000\u0000\u0214\u0217\u0001\u0000\u0000\u0000\u0215"+
		"\u0218\u0005?\u0000\u0000\u0216\u0218\u0003\u008cF\u0000\u0217\u0215\u0001"+
		"\u0000\u0000\u0000\u0217\u0216\u0001\u0000\u0000\u0000\u0218\u0219\u0001"+
		"\u0000\u0000\u0000\u0219\u021a\u0003X,\u0000\u021a\u021b\u0005X\u0000"+
		"\u0000\u021bO\u0001\u0000\u0000\u0000\u021c\u021d\u0007\u0000\u0000\u0000"+
		"\u021dQ\u0001\u0000\u0000\u0000\u021e\u021f\u0007\u0001\u0000\u0000\u021f"+
		"S\u0001\u0000\u0000\u0000\u0220\u0224\u0003P(\u0000\u0221\u0224\u0005"+
		"0\u0000\u0000\u0222\u0224\u0005+\u0000\u0000\u0223\u0220\u0001\u0000\u0000"+
		"\u0000\u0223\u0221\u0001\u0000\u0000\u0000\u0223\u0222\u0001\u0000\u0000"+
		"\u0000\u0224\u0227\u0001\u0000\u0000\u0000\u0225\u0223\u0001\u0000\u0000"+
		"\u0000\u0225\u0226\u0001\u0000\u0000\u0000\u0226U\u0001\u0000\u0000\u0000"+
		"\u0227\u0225\u0001\u0000\u0000\u0000\u0228\u0229\u0003N\'\u0000\u0229"+
		"W\u0001\u0000\u0000\u0000\u022a\u022f\u0003Z-\u0000\u022b\u022c\u0005"+
		"U\u0000\u0000\u022c\u022e\u0003Z-\u0000\u022d\u022b\u0001\u0000\u0000"+
		"\u0000\u022e\u0231\u0001\u0000\u0000\u0000\u022f\u022d\u0001\u0000\u0000"+
		"\u0000\u022f\u0230\u0001\u0000\u0000\u0000\u0230Y\u0001\u0000\u0000\u0000"+
		"\u0231\u022f\u0001\u0000\u0000\u0000\u0232\u0235\u0005C\u0000\u0000\u0233"+
		"\u0234\u0005d\u0000\u0000\u0234\u0236\u0003\u00acV\u0000\u0235\u0233\u0001"+
		"\u0000\u0000\u0000\u0235\u0236\u0001\u0000\u0000\u0000\u0236[\u0001\u0000"+
		"\u0000\u0000\u0237\u0238\u0003T*\u0000\u0238\u0239\u0003\u008aE\u0000"+
		"\u0239\u023b\u0005C\u0000\u0000\u023a\u023c\u0003\u00a2Q\u0000\u023b\u023a"+
		"\u0001\u0000\u0000\u0000\u023b\u023c\u0001\u0000\u0000\u0000\u023c\u023d"+
		"\u0001\u0000\u0000\u0000\u023d\u023e\u0003`0\u0000\u023e\u023f\u0003f"+
		"3\u0000\u023f]\u0001\u0000\u0000\u0000\u0240\u0242\u0003P(\u0000\u0241"+
		"\u0240\u0001\u0000\u0000\u0000\u0241\u0242\u0001\u0000\u0000\u0000\u0242"+
		"\u0243\u0001\u0000\u0000\u0000\u0243\u0244\u0005C\u0000\u0000\u0244\u0245"+
		"\u0003`0\u0000\u0245\u0246\u0003$\u0012\u0000\u0246_\u0001\u0000\u0000"+
		"\u0000\u0247\u0249\u0005R\u0000\u0000\u0248\u024a\u0003b1\u0000\u0249"+
		"\u0248\u0001\u0000\u0000\u0000\u0249\u024a\u0001\u0000\u0000\u0000\u024a"+
		"\u024b\u0001\u0000\u0000\u0000\u024b\u024c\u0005S\u0000\u0000\u024ca\u0001"+
		"\u0000\u0000\u0000\u024d\u0252\u0003d2\u0000\u024e\u024f\u0005U\u0000"+
		"\u0000\u024f\u0251\u0003d2\u0000\u0250\u024e\u0001\u0000\u0000\u0000\u0251"+
		"\u0254\u0001\u0000\u0000\u0000\u0252\u0250\u0001\u0000\u0000\u0000\u0252"+
		"\u0253\u0001\u0000\u0000\u0000\u0253c\u0001\u0000\u0000\u0000\u0254\u0252"+
		"\u0001\u0000\u0000\u0000\u0255\u0257\u0003R)\u0000\u0256\u0255\u0001\u0000"+
		"\u0000\u0000\u0256\u0257\u0001\u0000\u0000\u0000\u0257\u0258\u0001\u0000"+
		"\u0000\u0000\u0258\u0259\u0003\u008cF\u0000\u0259\u025c\u0005C\u0000\u0000"+
		"\u025a\u025b\u0005d\u0000\u0000\u025b\u025d\u0003\u00aeW\u0000\u025c\u025a"+
		"\u0001\u0000\u0000\u0000\u025c\u025d\u0001\u0000\u0000\u0000\u025de\u0001"+
		"\u0000\u0000\u0000\u025e\u0263\u0003\"\u0011\u0000\u025f\u0260\u0005|"+
		"\u0000\u0000\u0260\u0263\u0003\u00aeW\u0000\u0261\u0263\u0005X\u0000\u0000"+
		"\u0262\u025e\u0001\u0000\u0000\u0000\u0262\u025f\u0001\u0000\u0000\u0000"+
		"\u0262\u0261\u0001\u0000\u0000\u0000\u0263g\u0001\u0000\u0000\u0000\u0264"+
		"\u0265\u0005*\u0000\u0000\u0265\u0266\u0003j5\u0000\u0266\u0267\u0003"+
		"`0\u0000\u0267\u0268\u0003f3\u0000\u0268i\u0001\u0000\u0000\u0000\u0269"+
		"\u0278\u0005Y\u0000\u0000\u026a\u0278\u0005Z\u0000\u0000\u026b\u0278\u0005"+
		"[\u0000\u0000\u026c\u0278\u0005]\u0000\u0000\u026d\u0278\u0005^\u0000"+
		"\u0000\u026e\u0278\u0005a\u0000\u0000\u026f\u0278\u0005_\u0000\u0000\u0270"+
		"\u0278\u0005`\u0000\u0000\u0271\u0272\u0005e\u0000\u0000\u0272\u0278\u0005"+
		"e\u0000\u0000\u0273\u0274\u0005f\u0000\u0000\u0274\u0278\u0005f\u0000"+
		"\u0000\u0275\u0278\u0005m\u0000\u0000\u0276\u0278\u0005n\u0000\u0000\u0277"+
		"\u0269\u0001\u0000\u0000\u0000\u0277\u026a\u0001\u0000\u0000\u0000\u0277"+
		"\u026b\u0001\u0000\u0000\u0000\u0277\u026c\u0001\u0000\u0000\u0000\u0277"+
		"\u026d\u0001\u0000\u0000\u0000\u0277\u026e\u0001\u0000\u0000\u0000\u0277"+
		"\u026f\u0001\u0000\u0000\u0000\u0277\u0270\u0001\u0000\u0000\u0000\u0277"+
		"\u0271\u0001\u0000\u0000\u0000\u0277\u0273\u0001\u0000\u0000\u0000\u0277"+
		"\u0275\u0001\u0000\u0000\u0000\u0277\u0276\u0001\u0000\u0000\u0000\u0278"+
		"k\u0001\u0000\u0000\u0000\u0279\u027a\u0005\u000f\u0000\u0000\u027a\u027c"+
		"\u0005C\u0000\u0000\u027b\u027d\u0003\u00a2Q\u0000\u027c\u027b\u0001\u0000"+
		"\u0000\u0000\u027c\u027d\u0001\u0000\u0000\u0000\u027d\u027f\u0001\u0000"+
		"\u0000\u0000\u027e\u0280\u0003x<\u0000\u027f\u027e\u0001\u0000\u0000\u0000"+
		"\u027f\u0280\u0001\u0000\u0000\u0000\u0280\u0281\u0001\u0000\u0000\u0000"+
		"\u0281\u0282\u0003n7\u0000\u0282m\u0001\u0000\u0000\u0000\u0283\u0287"+
		"\u0005N\u0000\u0000\u0284\u0286\u0003p8\u0000\u0285\u0284\u0001\u0000"+
		"\u0000\u0000\u0286\u0289\u0001\u0000\u0000\u0000\u0287\u0285\u0001\u0000"+
		"\u0000\u0000\u0287\u0288\u0001\u0000\u0000\u0000\u0288\u028a\u0001\u0000"+
		"\u0000\u0000\u0289\u0287\u0001\u0000\u0000\u0000\u028a\u028b\u0005O\u0000"+
		"\u0000\u028bo\u0001\u0000\u0000\u0000\u028c\u0291\u0003V+\u0000\u028d"+
		"\u0291\u0003\\.\u0000\u028e\u0291\u0003h4\u0000\u028f\u0291\u0003^/\u0000"+
		"\u0290\u028c\u0001\u0000\u0000\u0000\u0290\u028d\u0001\u0000\u0000\u0000"+
		"\u0290\u028e\u0001\u0000\u0000\u0000\u0290\u028f\u0001\u0000\u0000\u0000"+
		"\u0291q\u0001\u0000\u0000\u0000\u0292\u0293\u00052\u0000\u0000\u0293\u0295"+
		"\u0005C\u0000\u0000\u0294\u0296\u0003\u00a2Q\u0000\u0295\u0294\u0001\u0000"+
		"\u0000\u0000\u0295\u0296\u0001\u0000\u0000\u0000\u0296\u0298\u0001\u0000"+
		"\u0000\u0000\u0297\u0299\u0003x<\u0000\u0298\u0297\u0001\u0000\u0000\u0000"+
		"\u0298\u0299\u0001\u0000\u0000\u0000\u0299\u029a\u0001\u0000\u0000\u0000"+
		"\u029a\u029b\u0003t:\u0000\u029bs\u0001\u0000\u0000\u0000\u029c\u02a0"+
		"\u0005N\u0000\u0000\u029d\u029f\u0003v;\u0000\u029e\u029d\u0001\u0000"+
		"\u0000\u0000\u029f\u02a2\u0001\u0000\u0000\u0000\u02a0\u029e\u0001\u0000"+
		"\u0000\u0000\u02a0\u02a1\u0001\u0000\u0000\u0000\u02a1\u02a3\u0001\u0000"+
		"\u0000\u0000\u02a2\u02a0\u0001\u0000\u0000\u0000\u02a3\u02a4\u0005O\u0000"+
		"\u0000\u02a4u\u0001\u0000\u0000\u0000\u02a5\u02aa\u0003V+\u0000\u02a6"+
		"\u02aa\u0003\\.\u0000\u02a7\u02aa\u0003h4\u0000\u02a8\u02aa\u0003^/\u0000"+
		"\u02a9\u02a5\u0001\u0000\u0000\u0000\u02a9\u02a6\u0001\u0000\u0000\u0000"+
		"\u02a9\u02a7\u0001\u0000\u0000\u0000\u02a9\u02a8\u0001\u0000\u0000\u0000"+
		"\u02aaw\u0001\u0000\u0000\u0000\u02ab\u02ac\u0005W\u0000\u0000\u02ac\u02b1"+
		"\u0003\u008cF\u0000\u02ad\u02ae\u0005U\u0000\u0000\u02ae\u02b0\u0003\u008c"+
		"F\u0000\u02af\u02ad\u0001\u0000\u0000\u0000\u02b0\u02b3\u0001\u0000\u0000"+
		"\u0000\u02b1\u02af\u0001\u0000\u0000\u0000\u02b1\u02b2\u0001\u0000\u0000"+
		"\u0000\u02b2y\u0001\u0000\u0000\u0000\u02b3\u02b1\u0001\u0000\u0000\u0000"+
		"\u02b4\u02b5\u00057\u0000\u0000\u02b5\u02b6\u0005C\u0000\u0000\u02b6\u02b7"+
		"\u0003|>\u0000\u02b7{\u0001\u0000\u0000\u0000\u02b8\u02bb\u0003~?\u0000"+
		"\u02b9\u02bb\u0003\\.\u0000\u02ba\u02b8\u0001\u0000\u0000\u0000\u02ba"+
		"\u02b9\u0001\u0000\u0000\u0000\u02bb}\u0001\u0000\u0000\u0000\u02bc\u02c0"+
		"\u0005N\u0000\u0000\u02bd\u02bf\u0003\u0080@\u0000\u02be\u02bd\u0001\u0000"+
		"\u0000\u0000\u02bf\u02c2\u0001\u0000\u0000\u0000\u02c0\u02be\u0001\u0000"+
		"\u0000\u0000\u02c0\u02c1\u0001\u0000\u0000\u0000\u02c1\u02c3\u0001\u0000"+
		"\u0000\u0000\u02c2\u02c0\u0001\u0000\u0000\u0000\u02c3\u02c4\u0005O\u0000"+
		"\u0000\u02c4\u007f\u0001\u0000\u0000\u0000\u02c5\u02c6\u0003\\.\u0000"+
		"\u02c6\u0081\u0001\u0000\u0000\u0000\u02c7\u02c9\u00055\u0000\u0000\u02c8"+
		"\u02c7\u0001\u0000\u0000\u0000\u02c8\u02c9\u0001\u0000\u0000\u0000\u02c9"+
		"\u02ca\u0001\u0000\u0000\u0000\u02ca\u02cb\u0005=\u0000\u0000\u02cb\u02cd"+
		"\u0005C\u0000\u0000\u02cc\u02ce\u0003\u00a2Q\u0000\u02cd\u02cc\u0001\u0000"+
		"\u0000\u0000\u02cd\u02ce\u0001\u0000\u0000\u0000\u02ce\u02cf\u0001\u0000"+
		"\u0000\u0000\u02cf\u02d0\u0003\u0084B\u0000\u02d0\u0083\u0001\u0000\u0000"+
		"\u0000\u02d1\u02d2\u0005N\u0000\u0000\u02d2\u02d7\u0003\u0088D\u0000\u02d3"+
		"\u02d4\u0005U\u0000\u0000\u02d4\u02d6\u0003\u0088D\u0000\u02d5\u02d3\u0001"+
		"\u0000\u0000\u0000\u02d6\u02d9\u0001\u0000\u0000\u0000\u02d7\u02d5\u0001"+
		"\u0000\u0000\u0000\u02d7\u02d8\u0001\u0000\u0000\u0000\u02d8\u02db\u0001"+
		"\u0000\u0000\u0000\u02d9\u02d7\u0001\u0000\u0000\u0000\u02da\u02dc\u0005"+
		"U\u0000\u0000\u02db\u02da\u0001\u0000\u0000\u0000\u02db\u02dc\u0001\u0000"+
		"\u0000\u0000\u02dc\u02dd\u0001\u0000\u0000\u0000\u02dd\u02de\u0005O\u0000"+
		"\u0000\u02de\u0085\u0001\u0000\u0000\u0000\u02df\u02e0\u0005R\u0000\u0000"+
		"\u02e0\u02e1\u0003d2\u0000\u02e1\u02e2\u0005S\u0000\u0000\u02e2\u0087"+
		"\u0001\u0000\u0000\u0000\u02e3\u02e5\u0005C\u0000\u0000\u02e4\u02e6\u0003"+
		"\u0086C\u0000\u02e5\u02e4\u0001\u0000\u0000\u0000\u02e5\u02e6\u0001\u0000"+
		"\u0000\u0000\u02e6\u0089\u0001\u0000\u0000\u0000\u02e7\u02ea\u0005@\u0000"+
		"\u0000\u02e8\u02ea\u0003\u008cF\u0000\u02e9\u02e7\u0001\u0000\u0000\u0000"+
		"\u02e9\u02e8\u0001\u0000\u0000\u0000\u02ea\u008b\u0001\u0000\u0000\u0000"+
		"\u02eb\u02ec\u0005C\u0000\u0000\u02ec\u02ee\u0005W\u0000\u0000\u02ed\u02eb"+
		"\u0001\u0000\u0000\u0000\u02ed\u02ee\u0001\u0000\u0000\u0000\u02ee\u0304"+
		"\u0001\u0000\u0000\u0000\u02ef\u02f3\u0003\u0092I\u0000\u02f0\u02f2\u0003"+
		"\u009cN\u0000\u02f1\u02f0\u0001\u0000\u0000\u0000\u02f2\u02f5\u0001\u0000"+
		"\u0000\u0000\u02f3\u02f1\u0001\u0000\u0000\u0000\u02f3\u02f4\u0001\u0000"+
		"\u0000\u0000\u02f4\u0305\u0001\u0000\u0000\u0000\u02f5\u02f3\u0001\u0000"+
		"\u0000\u0000\u02f6\u02fa\u0003\u0094J\u0000\u02f7\u02f9\u0003\u009cN\u0000"+
		"\u02f8\u02f7\u0001\u0000\u0000\u0000\u02f9\u02fc\u0001\u0000\u0000\u0000"+
		"\u02fa\u02f8\u0001\u0000\u0000\u0000\u02fa\u02fb\u0001\u0000\u0000\u0000"+
		"\u02fb\u0305\u0001\u0000\u0000\u0000\u02fc\u02fa\u0001\u0000\u0000\u0000"+
		"\u02fd\u0301\u0003\u008eG\u0000\u02fe\u0300\u0003\u009cN\u0000\u02ff\u02fe"+
		"\u0001\u0000\u0000\u0000\u0300\u0303\u0001\u0000\u0000\u0000\u0301\u02ff"+
		"\u0001\u0000\u0000\u0000\u0301\u0302\u0001\u0000\u0000\u0000\u0302\u0305"+
		"\u0001\u0000\u0000\u0000\u0303\u0301\u0001\u0000\u0000\u0000\u0304\u02ef"+
		"\u0001\u0000\u0000\u0000\u0304\u02f6\u0001\u0000\u0000\u0000\u0304\u02fd"+
		"\u0001\u0000\u0000\u0000\u0305\u008d\u0001\u0000\u0000\u0000\u0306\u0307"+
		"\u0005R\u0000\u0000\u0307\u030c\u0003\u0090H\u0000\u0308\u0309\u0005U"+
		"\u0000\u0000\u0309\u030b\u0003\u0090H\u0000\u030a\u0308\u0001\u0000\u0000"+
		"\u0000\u030b\u030e\u0001\u0000\u0000\u0000\u030c\u030a\u0001\u0000\u0000"+
		"\u0000\u030c\u030d\u0001\u0000\u0000\u0000\u030d\u030f\u0001\u0000\u0000"+
		"\u0000\u030e\u030c\u0001\u0000\u0000\u0000\u030f\u0310\u0005S\u0000\u0000"+
		"\u0310\u008f\u0001\u0000\u0000\u0000\u0311\u0313\u0003\u008cF\u0000\u0312"+
		"\u0314\u0005C\u0000\u0000\u0313\u0312\u0001\u0000\u0000\u0000\u0313\u0314"+
		"\u0001\u0000\u0000\u0000\u0314\u0091\u0001\u0000\u0000\u0000\u0315\u0317"+
		"\u0003\u0016\u000b\u0000\u0316\u0318\u0003\u00a4R\u0000\u0317\u0316\u0001"+
		"\u0000\u0000\u0000\u0317\u0318\u0001\u0000\u0000\u0000\u0318\u0093\u0001"+
		"\u0000\u0000\u0000\u0319\u031e\u0005\u000b\u0000\u0000\u031a\u031e\u0005"+
		"\u000e\u0000\u0000\u031b\u031e\u00051\u0000\u0000\u031c\u031e\u0003\u0096"+
		"K\u0000\u031d\u0319\u0001\u0000\u0000\u0000\u031d\u031a\u0001\u0000\u0000"+
		"\u0000\u031d\u031b\u0001\u0000\u0000\u0000\u031d\u031c\u0001\u0000\u0000"+
		"\u0000\u031e\u0095\u0001\u0000\u0000\u0000\u031f\u0322\u0003\u0098L\u0000"+
		"\u0320\u0322\u0003\u009aM\u0000\u0321\u031f\u0001\u0000\u0000\u0000\u0321"+
		"\u0320\u0001\u0000\u0000\u0000\u0322\u0097\u0001\u0000\u0000\u0000\u0323"+
		"\u0324\u0007\u0002\u0000\u0000\u0324\u0099\u0001\u0000\u0000\u0000\u0325"+
		"\u0326\u0007\u0003\u0000\u0000\u0326\u009b\u0001\u0000\u0000\u0000\u0327"+
		"\u0329\u0005P\u0000\u0000\u0328\u032a\u0003\u00aeW\u0000\u0329\u0328\u0001"+
		"\u0000\u0000\u0000\u0329\u032a\u0001\u0000\u0000\u0000\u032a\u032e\u0001"+
		"\u0000\u0000\u0000\u032b\u032d\u0005U\u0000\u0000\u032c\u032b\u0001\u0000"+
		"\u0000\u0000\u032d\u0330\u0001\u0000\u0000\u0000\u032e\u032c\u0001\u0000"+
		"\u0000\u0000\u032e\u032f\u0001\u0000\u0000\u0000\u032f\u0331\u0001\u0000"+
		"\u0000\u0000\u0330\u032e\u0001\u0000\u0000\u0000\u0331\u0332\u0005Q\u0000"+
		"\u0000\u0332\u009d\u0001\u0000\u0000\u0000\u0333\u0336\u0005C\u0000\u0000"+
		"\u0334\u0335\u0005W\u0000\u0000\u0335\u0337\u0003\u00a0P\u0000\u0336\u0334"+
		"\u0001\u0000\u0000\u0000\u0336\u0337\u0001\u0000\u0000\u0000\u0337\u009f"+
		"\u0001\u0000\u0000\u0000\u0338\u0339\u0005C\u0000\u0000\u0339\u00a1\u0001"+
		"\u0000\u0000\u0000\u033a\u033b\u0005e\u0000\u0000\u033b\u0340\u0003\u009e"+
		"O\u0000\u033c\u033d\u0005U\u0000\u0000\u033d\u033f\u0003\u009eO\u0000"+
		"\u033e\u033c\u0001\u0000\u0000\u0000\u033f\u0342\u0001\u0000\u0000\u0000"+
		"\u0340\u033e\u0001\u0000\u0000\u0000\u0340\u0341\u0001\u0000\u0000\u0000"+
		"\u0341\u0343\u0001\u0000\u0000\u0000\u0342\u0340\u0001\u0000\u0000\u0000"+
		"\u0343\u0344\u0005f\u0000\u0000\u0344\u00a3\u0001\u0000\u0000\u0000\u0345"+
		"\u0346\u0005e\u0000\u0000\u0346\u034b\u0003\u008cF\u0000\u0347\u0348\u0005"+
		"U\u0000\u0000\u0348\u034a\u0003\u008cF\u0000\u0349\u0347\u0001\u0000\u0000"+
		"\u0000\u034a\u034d\u0001\u0000\u0000\u0000\u034b\u0349\u0001\u0000\u0000"+
		"\u0000\u034b\u034c\u0001\u0000\u0000\u0000\u034c\u034e\u0001\u0000\u0000"+
		"\u0000\u034d\u034b\u0001\u0000\u0000\u0000\u034e\u034f\u0003\u00a6S\u0000"+
		"\u034f\u00a5\u0001\u0000\u0000\u0000\u0350\u0354\u0005f\u0000\u0000\u0351"+
		"\u0352\u0005f\u0000\u0000\u0352\u0354\u0003\u00a6S\u0000\u0353\u0350\u0001"+
		"\u0000\u0000\u0000\u0353\u0351\u0001\u0000\u0000\u0000\u0354\u00a7\u0001"+
		"\u0000\u0000\u0000\u0355\u0356\u0005R\u0000\u0000\u0356\u0359\u0003\u00e6"+
		"s\u0000\u0357\u0358\u0005U\u0000\u0000\u0358\u035a\u0003\u00e6s\u0000"+
		"\u0359\u0357\u0001\u0000\u0000\u0000\u035a\u035b\u0001\u0000\u0000\u0000"+
		"\u035b\u0359\u0001\u0000\u0000\u0000\u035b\u035c\u0001\u0000\u0000\u0000"+
		"\u035c\u035d\u0001\u0000\u0000\u0000\u035d\u035e\u0005S\u0000\u0000\u035e"+
		"\u00a9\u0001\u0000\u0000\u0000\u035f\u0360\u0005R\u0000\u0000\u0360\u0361"+
		"\u0003\u00aeW\u0000\u0361\u0362\u0005S\u0000\u0000\u0362\u00ab\u0001\u0000"+
		"\u0000\u0000\u0363\u0364\u0003\u00b6[\u0000\u0364\u00ad\u0001\u0000\u0000"+
		"\u0000\u0365\u0366\u0003\u00b0X\u0000\u0366\u00af\u0001\u0000\u0000\u0000"+
		"\u0367\u036b\u0003\u00b6[\u0000\u0368\u0369\u0003\u00b4Z\u0000\u0369\u036a"+
		"\u0003\u00b0X\u0000\u036a\u036c\u0001\u0000\u0000\u0000\u036b\u0368\u0001"+
		"\u0000\u0000\u0000\u036b\u036c\u0001\u0000\u0000\u0000\u036c\u00b1\u0001"+
		"\u0000\u0000\u0000\u036d\u036e\u0005(\u0000\u0000\u036e\u036f\u0003\u008c"+
		"F\u0000\u036f\u0370\u0003\u00deo\u0000\u0370\u00b3\u0001\u0000\u0000\u0000"+
		"\u0371\u0372\u0007\u0004\u0000\u0000\u0372\u00b5\u0001\u0000\u0000\u0000"+
		"\u0373\u0378\u0003\u00b8\\\u0000\u0374\u0375\u0005l\u0000\u0000\u0375"+
		"\u0377\u0003\u00b8\\\u0000\u0376\u0374\u0001\u0000\u0000\u0000\u0377\u037a"+
		"\u0001\u0000\u0000\u0000\u0378\u0376\u0001\u0000\u0000\u0000\u0378\u0379"+
		"\u0001\u0000\u0000\u0000\u0379\u00b7\u0001\u0000\u0000\u0000\u037a\u0378"+
		"\u0001\u0000\u0000\u0000\u037b\u0380\u0003\u00ba]\u0000\u037c\u037d\u0005"+
		"k\u0000\u0000\u037d\u037f\u0003\u00ba]\u0000\u037e\u037c\u0001\u0000\u0000"+
		"\u0000\u037f\u0382\u0001\u0000\u0000\u0000\u0380\u037e\u0001\u0000\u0000"+
		"\u0000\u0380\u0381\u0001\u0000\u0000\u0000\u0381\u00b9\u0001\u0000\u0000"+
		"\u0000\u0382\u0380\u0001\u0000\u0000\u0000\u0383\u0388\u0003\u00bc^\u0000"+
		"\u0384\u0385\u0005`\u0000\u0000\u0385\u0387\u0003\u00bc^\u0000\u0386\u0384"+
		"\u0001\u0000\u0000\u0000\u0387\u038a\u0001\u0000\u0000\u0000\u0388\u0386"+
		"\u0001\u0000\u0000\u0000\u0388\u0389\u0001\u0000\u0000\u0000\u0389\u00bb"+
		"\u0001\u0000\u0000\u0000\u038a\u0388\u0001\u0000\u0000\u0000\u038b\u0390"+
		"\u0003\u00be_\u0000\u038c\u038d\u0005a\u0000\u0000\u038d\u038f\u0003\u00be"+
		"_\u0000\u038e\u038c\u0001\u0000\u0000\u0000\u038f\u0392\u0001\u0000\u0000"+
		"\u0000\u0390\u038e\u0001\u0000\u0000\u0000\u0390\u0391\u0001\u0000\u0000"+
		"\u0000\u0391\u00bd\u0001\u0000\u0000\u0000\u0392\u0390\u0001\u0000\u0000"+
		"\u0000\u0393\u0398\u0003\u00c0`\u0000\u0394\u0395\u0005_\u0000\u0000\u0395"+
		"\u0397\u0003\u00c0`\u0000\u0396\u0394\u0001\u0000\u0000\u0000\u0397\u039a"+
		"\u0001\u0000\u0000\u0000\u0398\u0396\u0001\u0000\u0000\u0000\u0398\u0399"+
		"\u0001\u0000\u0000\u0000\u0399\u00bf\u0001\u0000\u0000\u0000\u039a\u0398"+
		"\u0001\u0000\u0000\u0000\u039b\u03a0\u0003\u00c2a\u0000\u039c\u039d\u0007"+
		"\u0005\u0000\u0000\u039d\u039f\u0003\u00c2a\u0000\u039e\u039c\u0001\u0000"+
		"\u0000\u0000\u039f\u03a2\u0001\u0000\u0000\u0000\u03a0\u039e\u0001\u0000"+
		"\u0000\u0000\u03a0\u03a1\u0001\u0000\u0000\u0000\u03a1\u00c1\u0001\u0000"+
		"\u0000\u0000\u03a2\u03a0\u0001\u0000\u0000\u0000\u03a3\u03ab\u0003\u00c4"+
		"b\u0000\u03a4\u03a5\u0007\u0006\u0000\u0000\u03a5\u03ac\u0003\u00c4b\u0000"+
		"\u03a6\u03a7\u0005%\u0000\u0000\u03a7\u03a9\u0003\u008cF\u0000\u03a8\u03aa"+
		"\u0005C\u0000\u0000\u03a9\u03a8\u0001\u0000\u0000\u0000\u03a9\u03aa\u0001"+
		"\u0000\u0000\u0000\u03aa\u03ac\u0001\u0000\u0000\u0000\u03ab\u03a4\u0001"+
		"\u0000\u0000\u0000\u03ab\u03a6\u0001\u0000\u0000\u0000\u03ab\u03ac\u0001"+
		"\u0000\u0000\u0000\u03ac\u00c3\u0001\u0000\u0000\u0000\u03ad\u03b6\u0003"+
		"\u00cae\u0000\u03ae\u03b1\u0003\u00c6c\u0000\u03af\u03b1\u0003\u00c8d"+
		"\u0000\u03b0\u03ae\u0001\u0000\u0000\u0000\u03b0\u03af\u0001\u0000\u0000"+
		"\u0000\u03b1\u03b2\u0001\u0000\u0000\u0000\u03b2\u03b3\u0003\u00cae\u0000"+
		"\u03b3\u03b5\u0001\u0000\u0000\u0000\u03b4\u03b0\u0001\u0000\u0000\u0000"+
		"\u03b5\u03b8\u0001\u0000\u0000\u0000\u03b6\u03b4\u0001\u0000\u0000\u0000"+
		"\u03b6\u03b7\u0001\u0000\u0000\u0000\u03b7\u00c5\u0001\u0000\u0000\u0000"+
		"\u03b8\u03b6\u0001\u0000\u0000\u0000\u03b9\u03ba\u0005e\u0000\u0000\u03ba"+
		"\u03bb\u0005e\u0000\u0000\u03bb\u00c7\u0001\u0000\u0000\u0000\u03bc\u03bd"+
		"\u0005f\u0000\u0000\u03bd\u03be\u0005f\u0000\u0000\u03be\u00c9\u0001\u0000"+
		"\u0000\u0000\u03bf\u03c4\u0003\u00ccf\u0000\u03c0\u03c1\u0007\u0007\u0000"+
		"\u0000\u03c1\u03c3\u0003\u00ccf\u0000\u03c2\u03c0\u0001\u0000\u0000\u0000"+
		"\u03c3\u03c6\u0001\u0000\u0000\u0000\u03c4\u03c2\u0001\u0000\u0000\u0000"+
		"\u03c4\u03c5\u0001\u0000\u0000\u0000\u03c5\u00cb\u0001\u0000\u0000\u0000"+
		"\u03c6\u03c4\u0001\u0000\u0000\u0000\u03c7\u03cc\u0003\u00ceg\u0000\u03c8"+
		"\u03c9\u0007\b\u0000\u0000\u03c9\u03cb\u0003\u00ceg\u0000\u03ca\u03c8"+
		"\u0001\u0000\u0000\u0000\u03cb\u03ce\u0001\u0000\u0000\u0000\u03cc\u03ca"+
		"\u0001\u0000\u0000\u0000\u03cc\u03cd\u0001\u0000\u0000\u0000\u03cd\u00cd"+
		"\u0001\u0000\u0000\u0000\u03ce\u03cc\u0001\u0000\u0000\u0000\u03cf\u03d2"+
		"\u0003\u00d0h\u0000\u03d0\u03d1\u0005\\\u0000\u0000\u03d1\u03d3\u0003"+
		"\u00ceg\u0000\u03d2\u03d0\u0001\u0000\u0000\u0000\u03d2\u03d3\u0001\u0000"+
		"\u0000\u0000\u03d3\u00cf\u0001\u0000\u0000\u0000\u03d4\u03d9\u0003\u00d2"+
		"i\u0000\u03d5\u03d6\u0007\t\u0000\u0000\u03d6\u03d9\u0003\u00d0h\u0000"+
		"\u03d7\u03d9\u0003\u00d4j\u0000\u03d8\u03d4\u0001\u0000\u0000\u0000\u03d8"+
		"\u03d5\u0001\u0000\u0000\u0000\u03d8\u03d7\u0001\u0000\u0000\u0000\u03d9"+
		"\u00d1\u0001\u0000\u0000\u0000\u03da\u03db\u0005R\u0000\u0000\u03db\u03dc"+
		"\u0003\u008cF\u0000\u03dc\u03dd\u0005S\u0000\u0000\u03dd\u03de\u0003\u00d0"+
		"h\u0000\u03de\u00d3\u0001\u0000\u0000\u0000\u03df\u03e3\u0003\u00d6k\u0000"+
		"\u03e0\u03e2\u0003\u00d8l\u0000\u03e1\u03e0\u0001\u0000\u0000\u0000\u03e2"+
		"\u03e5\u0001\u0000\u0000\u0000\u03e3\u03e1\u0001\u0000\u0000\u0000\u03e3"+
		"\u03e4\u0001\u0000\u0000\u0000\u03e4\u00d5\u0001\u0000\u0000\u0000\u03e5"+
		"\u03e3\u0001\u0000\u0000\u0000\u03e6\u03f2\u0003\u00e8t\u0000\u03e7\u03f2"+
		"\u0003\u00aaU\u0000\u03e8\u03f2\u0003\u00a8T\u0000\u03e9\u03f2\u0003*"+
		"\u0015\u0000\u03ea\u03f2\u0003.\u0017\u0000\u03eb\u03f2\u0003\"\u0011"+
		"\u0000\u03ec\u03f2\u00056\u0000\u0000\u03ed\u03f2\u0003\u00dam\u0000\u03ee"+
		"\u03f2\u0003\u00b2Y\u0000\u03ef\u03f2\u0005C\u0000\u0000\u03f0\u03f2\u0003"+
		"\u0016\u000b\u0000\u03f1\u03e6\u0001\u0000\u0000\u0000\u03f1\u03e7\u0001"+
		"\u0000\u0000\u0000\u03f1\u03e8\u0001\u0000\u0000\u0000\u03f1\u03e9\u0001"+
		"\u0000\u0000\u0000\u03f1\u03ea\u0001\u0000\u0000\u0000\u03f1\u03eb\u0001"+
		"\u0000\u0000\u0000\u03f1\u03ec\u0001\u0000\u0000\u0000\u03f1\u03ed\u0001"+
		"\u0000\u0000\u0000\u03f1\u03ee\u0001\u0000\u0000\u0000\u03f1\u03ef\u0001"+
		"\u0000\u0000\u0000\u03f1\u03f0\u0001\u0000\u0000\u0000\u03f2\u00d7\u0001"+
		"\u0000\u0000\u0000\u03f3\u03f4\u0005T\u0000\u0000\u03f4\u0403\u0005C\u0000"+
		"\u0000\u03f5\u03f6\u0005T\u0000\u0000\u03f6\u0403\u0005E\u0000\u0000\u03f7"+
		"\u03f9\u0005R\u0000\u0000\u03f8\u03fa\u0003\u00e0p\u0000\u03f9\u03f8\u0001"+
		"\u0000\u0000\u0000\u03f9\u03fa\u0001\u0000\u0000\u0000\u03fa\u03fb\u0001"+
		"\u0000\u0000\u0000\u03fb\u0403\u0005S\u0000\u0000\u03fc\u03fd\u0005P\u0000"+
		"\u0000\u03fd\u03fe\u0003\u00dcn\u0000\u03fe\u03ff\u0005Q\u0000\u0000\u03ff"+
		"\u0403\u0001\u0000\u0000\u0000\u0400\u0403\u0005i\u0000\u0000\u0401\u0403"+
		"\u0005j\u0000\u0000\u0402\u03f3\u0001\u0000\u0000\u0000\u0402\u03f5\u0001"+
		"\u0000\u0000\u0000\u0402\u03f7\u0001\u0000\u0000\u0000\u0402\u03fc\u0001"+
		"\u0000\u0000\u0000\u0402\u0400\u0001\u0000\u0000\u0000\u0402\u0401\u0001"+
		"\u0000\u0000\u0000\u0403\u00d9\u0001\u0000\u0000\u0000\u0404\u0406\u0005"+
		"P\u0000\u0000\u0405\u0407\u0003\u00dcn\u0000\u0406\u0405\u0001\u0000\u0000"+
		"\u0000\u0406\u0407\u0001\u0000\u0000\u0000\u0407\u0408\u0001\u0000\u0000"+
		"\u0000\u0408\u0409\u0005Q\u0000\u0000\u0409\u00db\u0001\u0000\u0000\u0000"+
		"\u040a\u040f\u0003\u00aeW\u0000\u040b\u040c\u0005U\u0000\u0000\u040c\u040e"+
		"\u0003\u00aeW\u0000\u040d\u040b\u0001\u0000\u0000\u0000\u040e\u0411\u0001"+
		"\u0000\u0000\u0000\u040f\u040d\u0001\u0000\u0000\u0000\u040f\u0410\u0001"+
		"\u0000\u0000\u0000\u0410\u00dd\u0001\u0000\u0000\u0000\u0411\u040f\u0001"+
		"\u0000\u0000\u0000\u0412\u0414\u0005R\u0000\u0000\u0413\u0415\u0003\u00e0"+
		"p\u0000\u0414\u0413\u0001\u0000\u0000\u0000\u0414\u0415\u0001\u0000\u0000"+
		"\u0000\u0415\u0416\u0001\u0000\u0000\u0000\u0416\u0417\u0005S\u0000\u0000"+
		"\u0417\u00df\u0001\u0000\u0000\u0000\u0418\u041d\u0003\u00e6s\u0000\u0419"+
		"\u041a\u0005U\u0000\u0000\u041a\u041c\u0003\u00e6s\u0000\u041b\u0419\u0001"+
		"\u0000\u0000\u0000\u041c\u041f\u0001\u0000\u0000\u0000\u041d\u041b\u0001"+
		"\u0000\u0000\u0000\u041d\u041e\u0001\u0000\u0000\u0000\u041e\u00e1\u0001"+
		"\u0000\u0000\u0000\u041f\u041d\u0001\u0000\u0000\u0000\u0420\u0421\u0005"+
		"C\u0000\u0000\u0421\u0422\u0005W\u0000\u0000\u0422\u0423\u0003\u00aeW"+
		"\u0000\u0423\u00e3\u0001\u0000\u0000\u0000\u0424\u0425\u0003\u00acV\u0000"+
		"\u0425\u00e5\u0001\u0000\u0000\u0000\u0426\u0429\u0003\u00e2q\u0000\u0427"+
		"\u0429\u0003\u00e4r\u0000\u0428\u0426\u0001\u0000\u0000\u0000\u0428\u0427"+
		"\u0001\u0000\u0000\u0000\u0429\u00e7\u0001\u0000\u0000\u0000\u042a\u0434"+
		"\u00058\u0000\u0000\u042b\u0434\u0005\u0018\u0000\u0000\u042c\u0434\u0005"+
		"E\u0000\u0000\u042d\u0434\u0005F\u0000\u0000\u042e\u0434\u0005G\u0000"+
		"\u0000\u042f\u0434\u0005H\u0000\u0000\u0430\u0434\u0005I\u0000\u0000\u0431"+
		"\u0434\u0003\u00eau\u0000\u0432\u0434\u0005D\u0000\u0000\u0433\u042a\u0001"+
		"\u0000\u0000\u0000\u0433\u042b\u0001\u0000\u0000\u0000\u0433\u042c\u0001"+
		"\u0000\u0000\u0000\u0433\u042d\u0001\u0000\u0000\u0000\u0433\u042e\u0001"+
		"\u0000\u0000\u0000\u0433\u042f\u0001\u0000\u0000\u0000\u0433\u0430\u0001"+
		"\u0000\u0000\u0000\u0433\u0431\u0001\u0000\u0000\u0000\u0433\u0432\u0001"+
		"\u0000\u0000\u0000\u0434\u00e9\u0001\u0000\u0000\u0000\u0435\u0438\u0005"+
		"J\u0000\u0000\u0436\u0438\u0003\u00ecv\u0000\u0437\u0435\u0001\u0000\u0000"+
		"\u0000\u0437\u0436\u0001\u0000\u0000\u0000\u0438\u00eb\u0001\u0000\u0000"+
		"\u0000\u0439\u043d\u0005L\u0000\u0000\u043a\u043c\u0003\u00eew\u0000\u043b"+
		"\u043a\u0001\u0000\u0000\u0000\u043c\u043f\u0001\u0000\u0000\u0000\u043d"+
		"\u043b\u0001\u0000\u0000\u0000\u043d\u043e\u0001\u0000\u0000\u0000\u043e"+
		"\u0440\u0001\u0000\u0000\u0000\u043f\u043d\u0001\u0000\u0000\u0000\u0440"+
		"\u0441\u0005\u0083\u0000\u0000\u0441\u00ed\u0001\u0000\u0000\u0000\u0442"+
		"\u0447\u0003\u00f0x\u0000\u0443\u0447\u0005}\u0000\u0000\u0444\u0447\u0005"+
		"\u007f\u0000\u0000\u0445\u0447\u0005\u0080\u0000\u0000\u0446\u0442\u0001"+
		"\u0000\u0000\u0000\u0446\u0443\u0001\u0000\u0000\u0000\u0446\u0444\u0001"+
		"\u0000\u0000\u0000\u0446\u0445\u0001\u0000\u0000\u0000\u0447\u00ef\u0001"+
		"\u0000\u0000\u0000\u0448\u044d\u0003\u00aeW\u0000\u0449\u044a\u0005U\u0000"+
		"\u0000\u044a\u044c\u0003\u00aeW\u0000\u044b\u0449\u0001\u0000\u0000\u0000"+
		"\u044c\u044f\u0001\u0000\u0000\u0000\u044d\u044b\u0001\u0000\u0000\u0000"+
		"\u044d\u044e\u0001\u0000\u0000\u0000\u044e\u0456\u0001\u0000\u0000\u0000"+
		"\u044f\u044d\u0001\u0000\u0000\u0000\u0450\u0452\u0005W\u0000\u0000\u0451"+
		"\u0453\u0005\u0085\u0000\u0000\u0452\u0451\u0001\u0000\u0000\u0000\u0453"+
		"\u0454\u0001\u0000\u0000\u0000\u0454\u0452\u0001\u0000\u0000\u0000\u0454"+
		"\u0455\u0001\u0000\u0000\u0000\u0455\u0457\u0001\u0000\u0000\u0000\u0456"+
		"\u0450\u0001\u0000\u0000\u0000\u0456\u0457\u0001\u0000\u0000\u0000\u0457"+
		"\u00f1\u0001\u0000\u0000\u0000q\u00f5\u00fb\u0103\u010d\u0114\u0117\u011d"+
		"\u012e\u0135\u0140\u0148\u014a\u0153\u0158\u015f\u016c\u017c\u0189\u0190"+
		"\u0193\u0199\u01a0\u01b0\u01b9\u01bf\u01c2\u01c6\u01cc\u01db\u01e4\u01ee"+
		"\u01f2\u0201\u0208\u0213\u0217\u0223\u0225\u022f\u0235\u023b\u0241\u0249"+
		"\u0252\u0256\u025c\u0262\u0277\u027c\u027f\u0287\u0290\u0295\u0298\u02a0"+
		"\u02a9\u02b1\u02ba\u02c0\u02c8\u02cd\u02d7\u02db\u02e5\u02e9\u02ed\u02f3"+
		"\u02fa\u0301\u0304\u030c\u0313\u0317\u031d\u0321\u0329\u032e\u0336\u0340"+
		"\u034b\u0353\u035b\u036b\u0378\u0380\u0388\u0390\u0398\u03a0\u03a9\u03ab"+
		"\u03b0\u03b6\u03c4\u03cc\u03d2\u03d8\u03e3\u03f1\u03f9\u0402\u0406\u040f"+
		"\u0414\u041d\u0428\u0433\u0437\u043d\u0446\u044d\u0454\u0456";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}