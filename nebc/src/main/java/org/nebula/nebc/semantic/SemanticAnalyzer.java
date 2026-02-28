package org.nebula.nebc.semantic;

import org.nebula.nebc.ast.ASTNode;
import org.nebula.nebc.ast.ASTVisitor;
import org.nebula.nebc.ast.CompilationUnit;
import org.nebula.nebc.ast.GenericParam;
import org.nebula.nebc.ast.Parameter;
import org.nebula.nebc.ast.declarations.*;
import org.nebula.nebc.ast.expressions.*;
import org.nebula.nebc.ast.patterns.*;
import org.nebula.nebc.ast.statements.*;
import org.nebula.nebc.ast.tags.TagAtom;
import org.nebula.nebc.ast.tags.TagOperation;
import org.nebula.nebc.ast.tags.TagStatement;
import org.nebula.nebc.ast.types.NamedType;
import org.nebula.nebc.ast.types.TypeNode;
import org.nebula.nebc.core.CompilerConfig;
import org.nebula.nebc.frontend.diagnostic.Diagnostic;
import org.nebula.nebc.frontend.diagnostic.DiagnosticCode;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;
import org.nebula.nebc.semantic.symbol.*;
import org.nebula.nebc.semantic.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticAnalyzer implements ASTVisitor<Type>
{

	private final List<Diagnostic> errors = new ArrayList<>();

	// Symbol table (replaces old Scope)
	private final SymbolTable globalScope = new SymbolTable(null);
	private final Map<ASTNode, Symbol> nodeSymbols = new HashMap<>();
	private final Map<ASTNode, Type> nodeTypes = new HashMap<>();
	private final CompilerConfig config;
	private SymbolTable currentScope = globalScope;
	private MethodDeclaration mainMethod = null;
	private Type mainMethodReturnType = null;
	// --- Context Tracking ---
	private Type currentMethodReturnType = null;
	private boolean insideLoop = false;
	private Type currentTypeDefinition = null;
	private boolean isInsideExtern = false; // Flag for extern "C" blocks
	/** Synthetic member scopes for primitive type trait implementations. */
	private final Map<Type, SymbolTable> primitiveImplScopes = new HashMap<>();

	public SemanticAnalyzer(CompilerConfig config)
	{
		this.config = config;
	}

	public void declareTypes(CompilationUnit unit)
	{
		// Initialize built-in primitive types as TypeSymbols
		PrimitiveType.defineAll(globalScope);

		// Phase 1: Forward-declare all top-level type names.
		for (ASTNode decl : unit.declarations)
		{
			if (decl instanceof ClassDeclaration cd)
			{
				ClassType classType = new ClassType(cd.name, globalScope);
				TypeSymbol sym = new TypeSymbol(cd.name, classType, cd);
				classType.getMemberScope().setOwner(sym);
				if (!globalScope.define(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, cd, cd.name);
				}
			}
			else if (decl instanceof StructDeclaration sd)
			{
				StructType structType = new StructType(sd.name, globalScope);
				TypeSymbol sym = new TypeSymbol(sd.name, structType, sd);
				structType.getMemberScope().setOwner(sym);
				if (!globalScope.define(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, sd, sd.name);
				}
			}
			else if (decl instanceof EnumDeclaration ed)
			{
				EnumType enumType = new EnumType(ed.name, globalScope);
				TypeSymbol sym = new TypeSymbol(ed.name, enumType, ed);
				if (!globalScope.define(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, ed, ed.name);
				}
			}
			else if (decl instanceof UnionDeclaration ud)
			{
				UnionType unionType = new UnionType(ud.name, globalScope);
				TypeSymbol sym = new TypeSymbol(ud.name, unionType, ud);
				if (!globalScope.define(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, ud, ud.name);
				}
			}
			else if (decl instanceof TraitDeclaration td)
			{
				TraitType traitType = new TraitType(td.name, globalScope);
				TypeSymbol sym = new TypeSymbol(td.name, traitType, td);
				if (!globalScope.define(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, td, td.name);
				}
			}
			// ImplDeclaration does not define a new type — processed later.
		}
	}

	/**
	 * Phase 1.75: Populate trait member scopes by visiting trait declarations.
	 * Must run after declareTypes (so TraitTypes exist) and before analyze()
	 * (so that member access on type-parameter bounds can resolve methods).
	 */
	public void declareTraitBodies(CompilationUnit unit)
	{
		for (ASTNode decl : unit.declarations)
		{
			if (decl instanceof TraitDeclaration td)
			{
				visitTraitDeclaration(td);
			}
		}
	}

	public void declareMethods(CompilationUnit unit)
	{
		// Phase 1.5: Pre-declare global methods
		for (ASTNode decl : unit.declarations)
		{
			if (decl instanceof MethodDeclaration md)
			{
				defineMethodSignature(md);
			}
			else if (decl instanceof ExternDeclaration ed)
			{
				ed.accept(this);
			}
		}
	}

	public List<Diagnostic> analyze(CompilationUnit unit)
	{
		// Phase 2: Full visitation — resolve bodies, check types.
		for (ASTNode decl : unit.declarations)
		{
			if (!(decl instanceof ExternDeclaration))
			{
				decl.accept(this);
			}
		}
		return errors;
	}

	/**
	 * Associates a symbol with an AST node.
	 * Called during analysis (e.g., when visiting a MethodDeclaration).
	 */
	private void recordSymbol(ASTNode node, Symbol symbol)
	{
		nodeSymbols.put(node, symbol);
	}

	private void recordType(ASTNode node, Type type)
	{
		nodeTypes.put(node, type);
	}

	/**
	 * Helper for the CodeGen to retrieve resolved metadata.
	 */
	public Type getType(ASTNode node)
	{
		return nodeTypes.get(node);
	}

	/**
	 * Helper for the CodeGen to retrieve resolved metadata.
	 */
	public <T extends Symbol> T getSymbol(ASTNode node, Class<T> type)
	{
		Symbol sym = nodeSymbols.get(node);
		if (sym == null)
			return null;
		return type.isInstance(sym) ? type.cast(sym) : null;
	}

	/**
	 * Returns the AST node of the validated 'main' method, or null if none was
	 * found.
	 */
	public MethodDeclaration getMainMethod()
	{
		return mainMethod;
	}

	/**
	 * Returns the resolved return type of the 'main' method (i32 or void), or null.
	 */
	public Type getMainMethodReturnType()
	{
		return mainMethodReturnType;
	}

	/**
	 * Returns the map of synthetic impl scopes for primitive types.
	 * Used by codegen to resolve trait method names on primitives.
	 */
	public Map<Type, SymbolTable> getPrimitiveImplScopes()
	{
		return primitiveImplScopes;
	}

	// --- Utilities ---

	private void error(DiagnosticCode code, ASTNode node, Object... args)
	{
		var span = (node != null) ? node.getSpan() : SourceSpan.unknown();
		errors.add(Diagnostic.of(code, span, args));
	}

	private void enterScope()
	{
		currentScope = new SymbolTable(currentScope);
	}

	private void exitScope()
	{
		if (currentScope.getParent() != null)
		{
			currentScope = currentScope.getParent();
		}
	}

	/**
	 * Resolves a syntactic AST TypeNode to a semantic Type object.
	 * Uses the symbol table to find TypeSymbols specifically.
	 */
	private Type resolveType(TypeNode astType)
	{
		if (astType == null)
			return PrimitiveType.VOID;

		if (astType instanceof NamedType nt)
		{
			TypeSymbol ts = currentScope.resolveType(nt.qualifiedName);
			if (ts == null)
			{
				error(DiagnosticCode.UNKNOWN_TYPE, astType, nt.qualifiedName);
				return Type.ERROR;
			}
			return ts.getType();
		}
		// TODO: Handle ArrayType and TupleType recursion here
		return Type.ANY;
	}

	// =================================================================
	// DECLARATIONS
	// =================================================================

	@Override
	public Type visitCompilationUnit(CompilationUnit node)
	{
		// Phase 1 & 1.5 are now handled by declareTypes() and declareMethods()
		// Phase 2: Full visitation
		for (ASTNode decl : node.declarations)
		{
			if (!(decl instanceof ExternDeclaration))
			{
				decl.accept(this);
			}
		}
		return null;
	}

	@Override
	public Type visitNamespaceDeclaration(NamespaceDeclaration node)
	{
		// Resolve or create namespace
		NamespaceSymbol nsSym;
		Symbol existing = currentScope.resolve(node.name);

		if (existing instanceof NamespaceSymbol ns)
		{
			nsSym = ns;
		}
		else
		{
			NamespaceType nsType = new NamespaceType(node.name, currentScope);
			nsSym = new NamespaceSymbol(node.name, nsType, node);
			currentScope.define(nsSym);
		}

		SymbolTable previousScope = currentScope;
		currentScope = nsSym.getMemberTable();

		// Pre-pass methods
		for (ASTNode member : node.members)
		{
			if (member instanceof MethodDeclaration md)
			{
				defineMethodSignature(md);
			}
			else if (member instanceof ExternDeclaration ed)
			{
				ed.accept(this);
			}
		}

		for (ASTNode member : node.members)
		{
			member.accept(this);
		}

		// Only restore scope if it's a block declaration.
		// File-scoped namespaces (namespace foo;) stay active for the file.
		if (node.isBlockDeclaration)
		{
			currentScope = previousScope;
		}

		return null;
	}

	@Override
	public Type visitClassDeclaration(ClassDeclaration node)
	{
		// The TypeSymbol was already forward-declared in Phase 1.
		// Look it up and populate its member scope.
		TypeSymbol existingSym = currentScope.resolveType(node.name);
		if (existingSym == null)
		{
			error(DiagnosticCode.INTERNAL_ERROR, node, "class '" + node.name + "' was not forward-declared.");
			return null;
		}
		ClassType classType = (ClassType) existingSym.getType();
		Type result = visitCompositeBody(node, classType, node.members);

		// Trait implementation check
		for (org.nebula.nebc.ast.types.TypeNode inheritedNode : node.inheritance)
		{
			Type resolved = resolveType(inheritedNode);
			if (resolved instanceof TraitType traitType)
			{
				String missing = traitType.findMissingMethod(classType);
				if (missing != null)
				{
					error(DiagnosticCode.TYPE_MISMATCH, node, "Class '" + node.name + "' implements '" + traitType.name() + "' but is missing method '" + missing + "'");
				}
			}
		}
		return result;
	}

	@Override
	public Type visitStructDeclaration(StructDeclaration node)
	{
		// The TypeSymbol was already forward-declared in Phase 1.
		TypeSymbol existingSym = currentScope.resolveType(node.name);
		if (existingSym == null)
		{
			error(DiagnosticCode.INTERNAL_ERROR, node, "struct '" + node.name + "' was not forward-declared.");
			return null;
		}
		StructType structType = (StructType) existingSym.getType();
		return visitCompositeBody(node, structType, node.members);
	}

	/**
	 * Shared logic for populating the member scope of Classes and Structs.
	 */
	private Type visitCompositeBody(ASTNode node, CompositeType type, List<Declaration> members)
	{
		// Enter member scope
		SymbolTable outerScope = currentScope;
		Type prevTypeDef = currentTypeDefinition;

		currentScope = type.getMemberScope();
		currentTypeDefinition = type;

		// Define 'this' as a variable symbol pointing to the type
		currentScope.define(new VariableSymbol("this", type, false, node));

		// Pre-pass methods
		for (Declaration member : members)
		{
			if (member instanceof MethodDeclaration md)
			{
				defineMethodSignature(md);
			}
			else if (member instanceof ExternDeclaration ed)
			{
				ed.accept(this);
			}
		}

		// Visit members
		for (Declaration member : members)
		{
			member.accept(this);
		}

		// Restore context
		currentTypeDefinition = prevTypeDef;
		currentScope = outerScope;
		return null;
	}

	@Override
	public Type visitEnumDeclaration(EnumDeclaration node)
	{
		TypeSymbol existingSym = currentScope.resolveType(node.name);
		if (existingSym == null)
			return null;
		EnumType enumType = (EnumType) existingSym.getType();

		SymbolTable outerScope = currentScope;
		Type prevTypeDef = currentTypeDefinition;

		currentScope = enumType.getMemberScope();
		currentTypeDefinition = enumType;

		for (String variant : node.variants)
		{
			VariableSymbol variantSym = new VariableSymbol(variant, enumType, false, node);
			currentScope.define(variantSym);
		}

		currentTypeDefinition = prevTypeDef;
		currentScope = outerScope;
		return null;
	}

	@Override
	public Type visitUnionDeclaration(UnionDeclaration node)
	{
		TypeSymbol existingSym = currentScope.resolveType(node.name);
		if (existingSym == null)
			return null;
		UnionType unionType = (UnionType) existingSym.getType();

		SymbolTable outerScope = currentScope;
		Type prevTypeDef = currentTypeDefinition;

		currentScope = unionType.getMemberScope();
		currentTypeDefinition = unionType;

		for (UnionVariant variant : node.variants)
		{
			Type payloadType = (variant.payload == null) ? PrimitiveType.VOID : resolveType(variant.payload);

			if (payloadType == PrimitiveType.VOID)
			{
				VariableSymbol variantSym = new VariableSymbol(variant.name, unionType, false, node);
				currentScope.define(variantSym);
			}
			else
			{
				FunctionType ctorType = new FunctionType(unionType, java.util.List.of(payloadType));
				MethodSymbol variantSym = new MethodSymbol(variant.name, ctorType, java.util.Collections.emptyList(), false, node, java.util.Collections.emptyList());
				currentScope.define(variantSym);
			}
		}

		currentTypeDefinition = prevTypeDef;
		currentScope = outerScope;
		return null;
	}

	@Override
	public Type visitUnionVariant(UnionVariant node)
	{
		return null; // Handled in visitUnionDeclaration
	}

	@Override
	public Type visitExternDeclaration(ExternDeclaration node)
	{
		boolean oldExtern = isInsideExtern;
		isInsideExtern = true;
		for (MethodDeclaration member : node.members)
		{
			defineMethodSignature(member);
		}
		isInsideExtern = oldExtern;
		return null;
	}

	private void defineMethodSignature(MethodDeclaration node)
	{

		// If method has type parameters, push a temporary scope.
		SymbolTable outerScope = null;
		List<TypeParameterType> typeParams = new ArrayList<>();
		if (node.typeParams != null && !node.typeParams.isEmpty())
		{
			outerScope = currentScope;
			currentScope = new SymbolTable(outerScope);
			for (GenericParam gp : node.typeParams)
			{
				TraitType bound = null;
				if (gp.hasBound() && gp.bound() instanceof org.nebula.nebc.ast.types.NamedType nt)
				{
					TypeSymbol boundSym = outerScope.resolveType(nt.qualifiedName);
					if (boundSym != null && boundSym.getType() instanceof TraitType tt)
					{
						bound = tt;
					}
					else
					{
						error(DiagnosticCode.UNDEFINED_SYMBOL, node, "Unknown trait bound '" + nt.qualifiedName + "'");
					}
				}
				TypeParameterType tpt = new TypeParameterType(gp.name(), bound);
				typeParams.add(tpt);
				currentScope.define(new TypeSymbol(gp.name(), tpt, node));
			}
		}

		Type returnType = (node.returnType == null) ? PrimitiveType.VOID : resolveType(node.returnType);

		// 1. Build function signature
		List<Type> paramTypes = new ArrayList<>();
		List<ParameterInfo> paramInfos = isInsideExtern ? new ArrayList<>() : null;

		for (Parameter p : node.parameters)
		{
			Type pType = resolveType(p.type());
			paramTypes.add(pType == Type.ERROR ? Type.ANY : pType);
			if (isInsideExtern)
			{
				paramInfos.add(new ParameterInfo(p.cvtModifier(), pType, p.name()));
			}
		}

		// Prepend 'this' parameter for member methods
		if (currentTypeDefinition != null)
		{
			paramTypes.add(0, currentTypeDefinition);
			if (paramInfos != null)
			{
				paramInfos.add(0, new ParameterInfo(null, currentTypeDefinition, "this"));
			}
		}

		FunctionType methodType = new FunctionType(returnType, paramTypes, paramInfos);

		// 2. Define method in the OUTER scope (not the type-param scope)
		MethodSymbol methodSym = new MethodSymbol(node.name, methodType, node.modifiers, isInsideExtern || node.isExtern, node, typeParams);
		recordSymbol(node, methodSym);
		SymbolTable defineIn = (outerScope != null) ? outerScope : currentScope;
		if (!defineIn.define(methodSym))
		{
			error(DiagnosticCode.DUPLICATE_SYMBOL, node, node.name);
		}

		// Pop type-param scope
		if (outerScope != null)
		{
			currentScope = outerScope;
		}

		// 3. Check for entry point
		if ("main".equals(node.name) && currentTypeDefinition == null)
		{
			if (mainMethod != null)
			{
				error(DiagnosticCode.DUPLICATE_MAIN_METHOD, node);
			}
			else
			{
				if (returnType != PrimitiveType.I32 && returnType != PrimitiveType.VOID)
				{
					error(DiagnosticCode.INVALID_MAIN_SIGNATURE, node);
				}
				if (!node.parameters.isEmpty())
				{
					error(DiagnosticCode.INVALID_MAIN_SIGNATURE, node);
				}
				mainMethod = node;
				mainMethodReturnType = returnType;
			}
		}
	}

	@Override
	public Type visitMethodDeclaration(MethodDeclaration node)
	{
		MethodSymbol methodSym = getSymbol(node, MethodSymbol.class);
		if (methodSym == null)
		{
			defineMethodSignature(node);
			methodSym = getSymbol(node, MethodSymbol.class);
			if (methodSym == null)
				return PrimitiveType.VOID;
		}

		Type returnType = methodSym.getType().getReturnType();

		// 3. Analyze body
		SymbolTable outerScope = null;
		if (!methodSym.getTypeParameters().isEmpty())
		{
			outerScope = currentScope;
			currentScope = new SymbolTable(outerScope);
			for (TypeParameterType tpt : methodSym.getTypeParameters())
			{
				currentScope.define(new TypeSymbol(tpt.name(), tpt, node));
			}
		}

		enterScope(); // Body scope
		Type prevRet = currentMethodReturnType;
		currentMethodReturnType = returnType;

		// Define parameters as variable symbols
		for (Parameter param : node.parameters)
		{
			Type pType = resolveType(param.type());
			VariableSymbol paramSym = new VariableSymbol(param.name(), pType, false, node);
			if (!currentScope.define(paramSym))
			{
				error(DiagnosticCode.DUPLICATE_PARAMETER, node, param.name());
			}
			// Check default value type if present
			if (param.defaultValue() != null)
			{
				Type defType = param.defaultValue().accept(this);
				if (!defType.isAssignableTo(pType))
				{
					error(DiagnosticCode.TYPE_MISMATCH, param.defaultValue(), pType.name(), defType.name());
				}
			}
		}

		if (node.body != null)
		{
			// FFI validation: extern methods cannot have a body
			if (isInsideExtern)
			{
				error(DiagnosticCode.EXTERN_METHOD_HAS_BODY, node, node.name);
			}

			Type bodyType = node.body.accept(this);
			if (returnType == PrimitiveType.VOID && bodyType != PrimitiveType.VOID)
			{
				error(DiagnosticCode.TYPE_MISMATCH, node.body, returnType.name(), bodyType.name());
			}
			else if (returnType != PrimitiveType.VOID && bodyType != PrimitiveType.VOID && !bodyType.isAssignableTo(returnType))
			{
				error(DiagnosticCode.TYPE_MISMATCH, node.body, returnType.name(), bodyType.name());
			}
		}

		currentMethodReturnType = prevRet;
		exitScope();
		if (outerScope != null)
		{
			currentScope = outerScope;
		}
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitVariableDeclaration(VariableDeclaration node)
	{
		Type explicitType = node.isVar ? null : resolveType(node.type);
		boolean mutable = node.isVar; // var = mutable, explicit type = immutable by default

		for (VariableDeclarator decl : node.declarators)
		{
			Type actualType = explicitType;

			if (decl.hasInitializer())
			{
				Type initType = decl.initializer().accept(this);

				if (node.isVar)
				{
					// Type inference
					actualType = (initType == null || initType == Type.ERROR) ? Type.ERROR : initType;
				}
				else
				{
					// Type checking
					if (!initType.isAssignableTo(explicitType))
					{
						error(DiagnosticCode.TYPE_MISMATCH, decl.initializer(), explicitType.name(), initType.name());
					}
				}
			}
			else if (node.isVar)
			{
				error(DiagnosticCode.UNINITIALIZED_VARIABLE, node, decl.name());
				actualType = Type.ERROR;
			}

			if (actualType != Type.ERROR)
			{
				VariableSymbol varSym = new VariableSymbol(decl.name(), actualType, mutable, node);
				recordSymbol(node, varSym);
				if (!currentScope.define(varSym))
				{
					error(DiagnosticCode.DUPLICATE_SYMBOL, node, decl.name());
				}
			}
		}
		return PrimitiveType.VOID;
	}

	// =================================================================
	// STATEMENTS
	// =================================================================

	@Override
	public Type visitStatementBlock(StatementBlock node)
	{
		enterScope();
		for (ASTNode stmt : node.statements)
		{
			stmt.accept(this);
		}
		exitScope();
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitReturnStatement(ReturnStatement node)
	{
		Type valType = (node.value == null) ? PrimitiveType.VOID : node.value.accept(this);

		if (currentMethodReturnType == null)
		{
			error(DiagnosticCode.RETURN_OUTSIDE_METHOD, node);
		}
		else if (!valType.isAssignableTo(currentMethodReturnType))
		{
			error(DiagnosticCode.TYPE_MISMATCH, node, currentMethodReturnType.name(), valType.name());
		}
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitIfStatement(IfStatement node)
	{
		Type condType = node.condition.accept(this);
		if (condType != PrimitiveType.BOOL && condType != Type.ERROR)
		{
			error(DiagnosticCode.IF_CONDITION_NOT_BOOL, node.condition, condType.name());
		}
		node.thenBranch.accept(this);
		if (node.elseBranch != null)
		{
			node.elseBranch.accept(this);
		}
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitWhileStatement(WhileStatement node)
	{
		Type condType = node.condition.accept(this);
		if (condType != PrimitiveType.BOOL && condType != Type.ERROR)
		{
			error(DiagnosticCode.WHILE_CONDITION_NOT_BOOL, node.condition, condType.name());
		}

		boolean oldInsideLoop = insideLoop;
		insideLoop = true;
		node.body.accept(this);
		insideLoop = oldInsideLoop;

		return PrimitiveType.VOID;
	}

	@Override
	public Type visitForStatement(ForStatement node)
	{
		enterScope();

		if (node.initializer != null)
			node.initializer.accept(this);

		if (node.condition != null)
		{
			Type cond = node.condition.accept(this);
			if (cond != PrimitiveType.BOOL && cond != Type.ERROR)
			{
				error(DiagnosticCode.FOR_CONDITION_NOT_BOOL, node.condition, cond.name());
			}
		}

		if (node.iterators != null)
		{
			for (Expression expr : node.iterators)
			{
				expr.accept(this);
			}
		}

		boolean prevLoop = insideLoop;
		insideLoop = true;
		node.body.accept(this);
		insideLoop = prevLoop;

		exitScope();
		return PrimitiveType.VOID;
	}

	@Override
	public Type visitForeachStatement(ForeachStatement node)
	{
		enterScope();

		Type iterableType = node.iterable.accept(this);
		// In a real implementation, checking if iterableType implements Iterable<T>
		Type itemType = Type.ANY; // Should extract T from Iterable<T>

		if (iterableType instanceof ArrayType arr)
		{
			itemType = arr.baseType;
		}
		else if (iterableType instanceof ClassType || iterableType instanceof StructType)
		{
			if (iterableType.name().startsWith("List<"))
			{
				String innerName = iterableType.name().substring(5, iterableType.name().length() - 1);
				TypeSymbol ts = currentScope.resolveType(innerName);
				if (ts != null)
				{
					itemType = ts.getType();
				}
			}
		}

		if (node.variableType != null)
		{
			itemType = resolveType(node.variableType);
		}

		// Define the loop variable as a VariableSymbol
		VariableSymbol loopVar = new VariableSymbol(node.variableName, itemType, false, node);
		currentScope.define(loopVar);

		boolean prevLoop = insideLoop;
		insideLoop = true;
		node.body.accept(this);
		insideLoop = prevLoop;

		exitScope();
		return PrimitiveType.VOID;
	}

	// =================================================================
	// EXPRESSIONS
	// =================================================================

	@Override
	public Type visitBinaryExpression(BinaryExpression node)
	{
		Type left = node.left.accept(this);
		Type right = node.right.accept(this);

		Type result = Type.ERROR;
		switch (node.operator)
		{
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:
			case POW:
				if (isNumeric(left) && isNumeric(right))
				{
					PrimitiveType pLeft = (PrimitiveType) left;
					PrimitiveType pRight = (PrimitiveType) right;
					if (pLeft.getBitWidth() > pRight.getBitWidth())
					{
						result = pLeft;
					}
					else if (pRight.getBitWidth() > pLeft.getBitWidth())
					{
						result = pRight;
					}
					else
					{
						// Same width: if one is signed, prefer signed?
						// Or just return left.
						// Rust doesn't allow cross-signedness without cast.
						// For Nebula, let's prefer signed if they differ but match width.
						boolean leftUnsigned = pLeft.name().startsWith("u");
						boolean rightUnsigned = pRight.name().startsWith("u");
						if (leftUnsigned && !rightUnsigned)
							result = pRight;
						else
							result = pLeft;
					}
				}
				else
				{
					error(DiagnosticCode.OPERATOR_NOT_DEFINED, node, node.operator, left.name(), right.name());
					result = Type.ERROR;
				}
				break;

			case EQ:
			case NE:
				if (!left.equals(right) && !(isNumeric(left) && isNumeric(right)))
				{
					error(DiagnosticCode.COMPARING_DISTINCT_TYPES, node, left.name(), right.name());
				}
				result = PrimitiveType.BOOL;
				break;

			case LT:
			case GT:
			case LE:
			case GE:
				if (isNumeric(left) && isNumeric(right))
					result = PrimitiveType.BOOL;
				else
				{
					error(DiagnosticCode.RELATIONAL_NUMERIC, node);
					result = Type.ERROR;
				}
				break;

			case LOGICAL_AND:
			case LOGICAL_OR:
				if (left == PrimitiveType.BOOL && right == PrimitiveType.BOOL)
					result = PrimitiveType.BOOL;
				else
				{
					error(DiagnosticCode.LOGICAL_BOOLEAN, node);
					result = Type.ERROR;
				}
				break;

			default:
				result = Type.ERROR;
		}
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitInvocationExpression(InvocationExpression node)
	{
		Type targetType = node.target.accept(this);
		if (targetType == Type.ERROR)
			return Type.ERROR;

		FunctionType fn = null;
		MethodSymbol methodSym = null;

		// Try to get the underlying method symbol to check for generics
		Symbol sym = nodeSymbols.get(node.target);
		if (sym instanceof MethodSymbol ms)
		{
			methodSym = ms;
		}

		if (targetType instanceof FunctionType f)
		{
			fn = f;
		}
		else if (targetType instanceof StructType || targetType instanceof ClassType)
		{
			// Constructor call — result is targetType
			fn = null;
		}
		else
		{
			error(DiagnosticCode.NOT_CALLABLE, node.target, targetType.name());
			return Type.ERROR;
		}

		// Validate and substitute for generics
		Type result = Type.ANY;
		if (fn != null)
		{
			List<Expression> effectiveArgs = new ArrayList<>(node.arguments);
			// If it's a member access call, prepend the receiver to effectiveArgs
			if (node.target instanceof MemberAccessExpression mae && !fn.parameterTypes.isEmpty())
			{
				// Assume the first parameter is 'this' if it's a member call
				effectiveArgs.add(0, mae.target);
			}

			// If it's a generic method, we need to perform type inference
			if (methodSym != null && !methodSym.getTypeParameters().isEmpty())
			{
				Substitution sub = new Substitution();
				// Basic inference from arguments
				if (effectiveArgs.size() == fn.parameterTypes.size())
				{
					for (int i = 0; i < effectiveArgs.size(); i++)
					{
						Type argType = effectiveArgs.get(i).accept(this);
						infer(fn.parameterTypes.get(i), argType, sub);
					}
				}

				// Perform substitution
				fn = (FunctionType) sub.substitute(fn);

				// Record the specialization (the concrete types used for the type params)
				List<Type> typeArgs = new ArrayList<>();
				for (TypeParameterType tpt : methodSym.getTypeParameters())
				{
					Type concrete = sub.substitute(tpt);
					typeArgs.add(concrete);

					// Validate trait bounds
					if (tpt.getBound() != null)
					{
						SymbolTable memberScope = null;
						if (concrete instanceof CompositeType ct)
						{
							memberScope = ct.getMemberScope();
						}
						else if (concrete instanceof PrimitiveType pt)
						{
							memberScope = primitiveImplScopes.get(pt);
						}

						if (memberScope == null)
						{
							error(DiagnosticCode.TYPE_MISMATCH, node, tpt.getBound().name(), concrete.name() + " (Cannot implement traits or no trait implementation found)");
						}
						else
						{
							String missing = tpt.getBound().findMissingMethod(memberScope);
							if (missing != null)
							{
								error(DiagnosticCode.TYPE_MISMATCH, node, tpt.getBound().name(), concrete.name() + " (missing method '" + missing + "')");
							}
						}
					}
				}
				node.setTypeArguments(typeArgs);
			}

			if (effectiveArgs.size() != fn.parameterTypes.size())
			{
				error(DiagnosticCode.ARGUMENT_COUNT_MISMATCH, node, fn.parameterTypes.size(), effectiveArgs.size());
				result = fn.returnType;
			}
			else
			{
				for (int i = 0; i < effectiveArgs.size(); i++)
				{
					Type argType = effectiveArgs.get(i).accept(this);
					Type paramType = fn.parameterTypes.get(i);
					if (!argType.isAssignableTo(paramType))
					{
						error(DiagnosticCode.ARGUMENT_TYPE_MISMATCH, effectiveArgs.get(i), (i + 1), paramType.name(), argType.name());
					}
				}
				result = fn.returnType;
			}
		}
		else
		{
			// Constructor call — targetType is already the ClassType/StructType
			result = targetType;
		}

		recordType(node, result);
		return result;
	}

	/**
	 * Basic type inference: binds type parameters in paramType based on concrete
	 * types in argType.
	 */
	private void infer(Type paramType, Type argType, Substitution sub)
	{
		if (paramType instanceof TypeParameterType tpt)
		{
			sub.bind(tpt, argType);
		}
		else if (paramType instanceof ArrayType pat && argType instanceof ArrayType aat)
		{
			infer(pat.baseType, aat.baseType, sub);
		}
		else if (paramType instanceof TupleType ptt && argType instanceof TupleType att)
		{
			if (ptt.elementTypes.size() == att.elementTypes.size())
			{
				for (int i = 0; i < ptt.elementTypes.size(); i++)
				{
					infer(ptt.elementTypes.get(i), att.elementTypes.get(i), sub);
				}
			}
		}
	}

	@Override
	public Type visitMemberAccessExpression(MemberAccessExpression node)
	{
		Type objectType = node.target.accept(this);
		if (objectType == Type.ERROR)
			return Type.ERROR;

		if (objectType == PrimitiveType.STR)
		{
			if (node.memberName.equals("ptr"))
			{
				Type result = PrimitiveType.REF;
				recordType(node, result);
				return result;
			}
			else if (node.memberName.equals("len"))
			{
				Type result = PrimitiveType.U64;
				recordType(node, result);
				return result;
			}
			// Fall through to check impl scope for str trait methods (e.g. toStr)
		}

		SymbolTable memberScope = null;
		if (objectType instanceof CompositeType ct)
		{
			memberScope = ct.getMemberScope();
		}
		else if (objectType instanceof NamespaceType nt)
		{
			memberScope = nt.getMemberScope();
		}
		else if (objectType instanceof TypeParameterType tpt && tpt.hasBound())
		{
			memberScope = tpt.getBound().getMemberScope();
		}
		else if (objectType instanceof PrimitiveType)
		{
			// Check if a trait impl was registered for this primitive
			memberScope = primitiveImplScopes.get(objectType);
		}

		if (memberScope == null)
		{
			error(DiagnosticCode.NO_MEMBERS, node.target, objectType.name());
			return Type.ERROR;
		}

		// Resolve member as a Symbol, return its type
		Symbol memberSym = memberScope.resolve(node.memberName);
		if (memberSym == null)
		{
			// Give a better error for str built-in fields
			if (objectType == PrimitiveType.STR && (node.memberName.equals("ptr") || node.memberName.equals("len")))
			{
				error(DiagnosticCode.MEMBER_NOT_FOUND, node, node.memberName, objectType.name());
			}
			else
			{
				error(DiagnosticCode.MEMBER_NOT_FOUND, node, node.memberName, objectType.name());
			}
			return Type.ERROR;
		}

		Type result = memberSym.getType();
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitIdentifierExpression(IdentifierExpression node)
	{
		Symbol sym = currentScope.resolve(node.name);
		if (sym == null)
		{
			error(DiagnosticCode.UNDEFINED_SYMBOL, node, node.name);
			return Type.ERROR;
		}
		recordSymbol(node, sym);
		Type result = sym.getType();
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitNewExpression(NewExpression node)
	{
		TypeSymbol ts = currentScope.resolveType(node.typeName);
		if (ts == null)
		{
			error(DiagnosticCode.UNKNOWN_TYPE, node, node.typeName);
			return Type.ERROR;
		}

		// TODO: Validate constructor arguments against the type's constructors
		for (Expression arg : node.arguments)
		{
			arg.accept(this);
		}

		Type result = ts.getType();
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitAssignmentExpression(AssignmentExpression node)
	{
		Type targetType = node.target.accept(this);
		Type valueType = node.value.accept(this);

		if (targetType == Type.ERROR || valueType == Type.ERROR)
			return Type.ERROR;

		if (!valueType.isAssignableTo(targetType))
		{
			error(DiagnosticCode.TYPE_MISMATCH, node, targetType.name(), valueType.name());
			return Type.ERROR;
		}
		recordType(node, targetType);
		return targetType;
	}

	@Override
	public Type visitUnaryExpression(UnaryExpression node)
	{
		Type operand = node.operand.accept(this);
		if (operand == Type.ERROR)
			return Type.ERROR;

		Type result = switch (node.operator)
		{
			case NOT -> {
				if (operand != PrimitiveType.BOOL)
				{
					error(DiagnosticCode.UNARY_NOT_BOOLEAN, node, operand.name());
					yield Type.ERROR;
				}
				yield PrimitiveType.BOOL;
			}
			case MINUS, PLUS -> {
				if (!isNumeric(operand))
				{
					error(DiagnosticCode.UNARY_MATH_NUMERIC, node, operand.name());
					yield Type.ERROR;
				}
				yield operand;
			}
			default -> operand;
		};
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitLiteralExpression(LiteralExpression node)
	{
		Type result = switch (node.type)
		{
			case INT -> {
				if (node.value instanceof Long l)
				{
					if (l >= Byte.MIN_VALUE && l <= Byte.MAX_VALUE)
						yield PrimitiveType.I8;
					if (l >= Short.MIN_VALUE && l <= Short.MAX_VALUE)
						yield PrimitiveType.I16;
					if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE)
						yield PrimitiveType.I32;
				}
				yield PrimitiveType.I64;
			}
			case FLOAT -> {
				if (node.value instanceof Float)
					yield PrimitiveType.F32;
				yield PrimitiveType.F64;
			}
			case BOOL -> PrimitiveType.BOOL;
			case CHAR -> PrimitiveType.CHAR;
			case STRING -> PrimitiveType.STR;
		};
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitExpressionBlock(ExpressionBlock node)
	{
		enterScope();
		for (Statement stmt : node.statements)
		{
			stmt.accept(this);
		}

		Type resultType = PrimitiveType.VOID;
		if (node.hasTail())
		{
			resultType = node.tail.accept(this);
		}
		exitScope();
		recordType(node, resultType);
		return resultType;
	}

	// --- Helpers ---

	private boolean isNumeric(Type t)
	{
		if (t instanceof PrimitiveType p)
		{
			return p.isInteger() || p.isFloat();
		}
		return false;
	}

	// --- Stubs for features not yet fully implemented ---

	@Override
	public Type visitExpressionStatement(ExpressionStatement node)
	{
		return node.expression.accept(this);
	}

	@Override
	public Type visitCastExpression(CastExpression node)
	{
		node.expression.accept(this);
		Type result = resolveType(node.targetType);
		recordType(node, result);
		return result;
	}

	private Type getPromotedType(Type left, Type right)
	{
		if (left.equals(right))
			return left;
		if (left instanceof PrimitiveType pLeft && right instanceof PrimitiveType pRight)
		{
			if (pLeft.isFloat() || pRight.isFloat())
			{
				if (pLeft.isFloat() && pRight.isFloat())
				{
					return pLeft.getBitWidth() >= pRight.getBitWidth() ? pLeft : pRight;
				}
				return pLeft.isFloat() ? pLeft : pRight;
			}
			if (pLeft.isInteger() && pRight.isInteger())
			{
				if (pLeft.getBitWidth() > pRight.getBitWidth())
					return pLeft;
				if (pRight.getBitWidth() > pLeft.getBitWidth())
					return pRight;
				boolean leftUnsigned = pLeft.name().startsWith("u");
				boolean rightUnsigned = pRight.name().startsWith("u");
				if (leftUnsigned && !rightUnsigned)
					return pRight;
				return pLeft;
			}
		}
		return left;
	}

	@Override
	public Type visitMatchExpression(MatchExpression node)
	{
		Type targetType = node.selector.accept(this);
		if (targetType == Type.ERROR)
			return Type.ERROR;

		Type commonType = null;
		for (MatchArm arm : node.arms)
		{
			arm.pattern.accept(this); // TODO: Pattern analysis
			Type armType = arm.result.accept(this);
			if (commonType == null)
			{
				commonType = armType;
			}
			else if (!armType.isAssignableTo(commonType))
			{
				// Pick the "wider" type or error
				if (isNumeric(commonType) && isNumeric(armType))
				{
					commonType = getPromotedType(commonType, armType);
				}
				else
				{
					error(DiagnosticCode.TYPE_MISMATCH, arm.result, commonType.name(), armType.name());
				}
			}
		}

		Type result = commonType != null ? commonType : Type.ANY;
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitIfExpression(IfExpression node)
	{
		Type condType = node.condition.accept(this);
		if (condType != PrimitiveType.BOOL && condType != Type.ERROR)
		{
			error(DiagnosticCode.IF_CONDITION_NOT_BOOL, node.condition, condType.name());
		}

		Type thenType = node.thenExpressionBlock.accept(this);
		Type elseType = node.elseExpressionBlock.accept(this);

		if (!thenType.equals(elseType))
		{
			if (isNumeric(thenType) && isNumeric(elseType))
			{
				Type result = getPromotedType(thenType, elseType);
				recordType(node, result);
				return result;
			}
			error(DiagnosticCode.TYPE_MISMATCH, node, thenType.name(), elseType.name());
			return Type.ERROR;
		}

		recordType(node, thenType);
		return thenType;
	}

	@Override
	public Type visitIndexExpression(IndexExpression node)
	{
		Type targetType = node.target.accept(this);
		// Currently only supporting 1D index for simplicity
		for (Expression index : node.indices)
		{
			Type idxType = index.accept(this);
			if (!idxType.isAssignableTo(PrimitiveType.I32) && idxType != Type.ERROR)
			{
				error(DiagnosticCode.INDEX_NOT_INTEGER, index, idxType.name());
			}
		}

		if (targetType instanceof ArrayType arr)
		{
			recordType(node, arr.baseType);
			return arr.baseType;
		}

		if (targetType != Type.ERROR)
		{
			error(DiagnosticCode.TYPE_NOT_INDEXABLE, node.target, targetType.name());
		}
		return Type.ERROR;
	}

	@Override
	public Type visitArrayLiteralExpression(ArrayLiteralExpression node)
	{
		if (node.elements.isEmpty())
		{
			// Empty array literal: type is unknown
			ArrayType result = new ArrayType(Type.ANY, 0);
			recordType(node, result);
			return result;
		}

		Type firstType = node.elements.get(0).accept(this);
		for (int i = 1; i < node.elements.size(); i++)
		{
			Type t = node.elements.get(i).accept(this);
			if (!t.equals(firstType))
			{
				error(DiagnosticCode.ARRAY_LITERAL_MISMATCH, node.elements.get(i), firstType.name(), t.name());
			}
		}

		ArrayType result = new ArrayType(firstType, node.elements.size());
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitTupleLiteralExpression(TupleLiteralExpression node)
	{
		List<Type> elementTypes = new ArrayList<>();
		for (Expression expr : node.elements)
		{
			elementTypes.add(expr.accept(this));
		}
		TupleType result = new TupleType(elementTypes);
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitThisExpression(ThisExpression node)
	{
		if (currentTypeDefinition == null)
		{
			error(DiagnosticCode.THIS_OUTSIDE_TYPE, node);
			return Type.ERROR;
		}
		recordType(node, currentTypeDefinition);
		return currentTypeDefinition;
	}

	@Override
	public Type visitStringInterpolationExpression(StringInterpolationExpression node)
	{
		return PrimitiveType.STR;
	}

	@Override
	public Type visitMatchArm(MatchArm node)
	{
		return null;
	}

	@Override
	public Type visitLiteralPattern(LiteralPattern node)
	{
		return null;
	}

	@Override
	public Type visitTypePattern(TypePattern node)
	{
		return null;
	}

	@Override
	public Type visitWildcardPattern(WildcardPattern node)
	{
		return null;
	}

	@Override
	public Type visitOrPattern(OrPattern node)
	{
		return null;
	}

	@Override
	public Type visitTagAtom(TagAtom node)
	{
		return null;
	}

	@Override
	public Type visitTagOperation(TagOperation node)
	{
		return null;
	}

	@Override
	public Type visitTypeReference(TypeNode node)
	{
		return resolveType(node);
	}

	@Override
	public Type visitTraitDeclaration(TraitDeclaration node)
	{
		// The TypeSymbol was already forward-declared in Phase 1. Fetch the TraitType.
		TypeSymbol existingSym = currentScope.resolveType(node.name);
		if (existingSym == null)
		{
			error(DiagnosticCode.INTERNAL_ERROR, node, "trait '" + node.name + "' was not forward-declared.");
			return null;
		}
		TraitType traitType = (TraitType) existingSym.getType();

		// Enter the trait's member scope to process abstract method signatures.
		SymbolTable outerScope = currentScope;
		currentScope = traitType.getMemberScope();

		for (MethodDeclaration method : node.members)
		{
			// Trait methods are abstract by default, so we only care about the signature.
			// We use the same signature building logic as in defineMethodSignature,
			// but trait methods are registered as requirements.

			// Temporarily push type param scope if method is generic
			SymbolTable methodTypeParamScope = null;
			if (method.typeParams != null && !method.typeParams.isEmpty())
			{
				methodTypeParamScope = currentScope;
				currentScope = new SymbolTable(methodTypeParamScope);
				for (GenericParam gp : method.typeParams)
				{
					TraitType bound = null;
					if (gp.hasBound() && gp.bound() instanceof NamedType nt)
					{
						TypeSymbol boundSym = methodTypeParamScope.resolveType(nt.qualifiedName);
						if (boundSym != null && boundSym.getType() instanceof TraitType tt)
						{
							bound = tt;
						}
					}
					TypeParameterType tpt = new TypeParameterType(gp.name(), bound);
					currentScope.define(new TypeSymbol(gp.name(), tpt, method));
				}
			}

			Type returnType = (method.returnType == null) ? PrimitiveType.VOID : resolveType(method.returnType);
			List<Type> paramTypes = new ArrayList<>();
			for (Parameter p : method.parameters)
			{
				Type t = resolveType(p.type());
				paramTypes.add(t == Type.ERROR ? Type.ANY : t);
			}
			FunctionType fnType = new FunctionType(returnType, paramTypes, null);
			MethodSymbol methodSym = new MethodSymbol(method.name, fnType, method.modifiers, false, method, java.util.Collections.emptyList());
			recordSymbol(method, methodSym);

			if (method.body != null)
			{
				// Has a default implementation — optional for implementors
				traitType.addDefaultMethod(methodSym, method);
			}
			else
			{
				// Abstract — required by implementors
				traitType.addRequiredMethod(methodSym);
			}

			// Pop type param scope
			if (methodTypeParamScope != null)
			{
				currentScope = methodTypeParamScope;
			}
		}

		currentScope = outerScope;
		return null;
	}

	@Override
	public Type visitImplDeclaration(ImplDeclaration node)
	{
		// 1. Resolve the trait
		Type traitResolved = resolveType(node.traitType);
		if (!(traitResolved instanceof TraitType traitType))
		{
			if (traitResolved != Type.ERROR)
				error(DiagnosticCode.TYPE_MISMATCH, node, "Expected a trait name, got '" + traitResolved.name() + "'");
			return null;
		}

		// 2. Process the target type
		Type targetType = resolveType(node.targetType);
		if (targetType == Type.ERROR)
			return null;

		// Get or create the member scope for this type
		SymbolTable targetScope;
		if (targetType instanceof CompositeType composite)
		{
			targetScope = composite.getMemberScope();
		}
		else if (targetType instanceof PrimitiveType pt)
		{
			// Primitives get a synthetic impl scope, owned by the primitive type symbol
			targetScope = primitiveImplScopes.computeIfAbsent(targetType, t ->
			{
				SymbolTable st = new SymbolTable(globalScope);
				// Set the owner so MethodSymbol.getMangledName() prefixes methods with 'i32_', etc.
				st.setOwner(new TypeSymbol(pt.name(), pt, null));
				return st;
			});
		}
		else
		{
			error(DiagnosticCode.TYPE_MISMATCH, node.targetType, "trait implementor", targetType.name() + " (cannot implement trait for this type)");
			return null;
		}

		// 3. Enter the target scope and define methods
		SymbolTable outerScope = currentScope;
		currentScope = targetScope;
		currentTypeDefinition = targetType;

		try
		{
			// Add a 'this' symbol for method bodies
			targetScope.define(new VariableSymbol("this", targetType, false, node));

			for (MethodDeclaration method : node.members)
			{
				defineMethodSignature(method);
				MethodSymbol ms = getSymbol(method, MethodSymbol.class);
				if (ms != null)
				{
					ms.setTraitName(traitType.name());
				}
			}

			// Now visit the bodies
			for (MethodDeclaration method : node.members)
			{
				visitMethodDeclaration(method);
			}

			// 4. Validate all required trait methods are present
			String missing = traitType.findMissingMethod(targetScope);
			if (missing != null)
			{
				error(DiagnosticCode.TYPE_MISMATCH, node, traitType.name(), targetType.name() + " (missing required method '" + missing + "')");
			}
		}
		finally
		{
			currentScope = outerScope;
			currentTypeDefinition = null;
		}
		return null;
	}

	@Override
	public Type visitOperatorDeclaration(OperatorDeclaration node)
	{
		return null;
	}

	@Override
	public Type visitConstructorDeclaration(ConstructorDeclaration node)
	{
		return null;
	}

	@Override
	public Type visitTagStatement(TagStatement node)
	{
		return null;
	}

	@Override
	public Type visitUseStatement(UseStatement node)
	{
		return null;
	}

	@Override
	public Type visitConstDeclaration(ConstDeclaration node)
	{
		return null;
	}
}