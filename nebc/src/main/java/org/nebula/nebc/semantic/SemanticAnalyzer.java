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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	/**
	 * Tracks (type::trait) pairs that have been *fully* resolved by
	 * {@link #visitImplDeclarationForType}.  Used by the overlap check in
	 * {@link #visitImplDeclarationForTag} so that pre-registered stubs from
	 * {@link #preRegisterImplSignaturesForType} are not mistaken for a real
	 * prior impl and do not trigger false-positive overlap errors.
	 */
	private final java.util.Set<String> resolvedImplPairs = new java.util.HashSet<>();

	public SemanticAnalyzer(CompilerConfig config)
	{
		this.config = config;
	}

	public void declareTypes(CompilationUnit unit)
	{
		currentScope = globalScope;
		// Initialize built-in primitive types as TypeSymbols
		PrimitiveType.defineAll(globalScope);

		declareTypesRecursive(unit.declarations);
	}

	private void declareTypesRecursive(List<ASTNode> declarations)
	{
		if (declarations == null)
			return;
		for (ASTNode decl : declarations)
		{
			if (decl instanceof NamespaceDeclaration nd)
			{
				if (nd.isBlockDeclaration)
				{
					SymbolTable original = currentScope;
					currentScope = enterNamespace(nd, currentScope);
					declareTypesRecursive(nd.members);
					currentScope = original;
				}
				else
				{
					currentScope = enterNamespace(nd, globalScope);
				}
			}
			else if (decl instanceof ClassDeclaration cd)
			{
				ClassType classType = new ClassType(cd.name, currentScope);
				TypeSymbol sym = new TypeSymbol(cd.name, classType, cd);
				classType.getMemberScope().setOwner(sym);
				if (!currentScope.forceDefine(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, cd, cd.name);
				}
				if (!cd.attributes.isEmpty())
					sym.setAttributes(collectAttributeInfos(cd.attributes));
			}
			else if (decl instanceof StructDeclaration sd)
			{
				StructType structType = new StructType(sd.name, currentScope);
				TypeSymbol sym = new TypeSymbol(sd.name, structType, sd);
				structType.getMemberScope().setOwner(sym);
				if (!currentScope.forceDefine(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, sd, sd.name);
				}
				if (!sd.attributes.isEmpty())
					sym.setAttributes(collectAttributeInfos(sd.attributes));
			}
			else if (decl instanceof EnumDeclaration ed)
			{
				EnumType enumType = new EnumType(ed.name, currentScope);
				TypeSymbol sym = new TypeSymbol(ed.name, enumType, ed);
				if (!currentScope.forceDefine(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, ed, ed.name);
				}
				if (!ed.attributes.isEmpty())
					sym.setAttributes(collectAttributeInfos(ed.attributes));
				// Pre-register variants in Phase 1 so unqualified variant names
				// (e.g. Mushroom instead of Colletable.Mushroom) are resolvable
				// in any method body regardless of source declaration order.
				for (String variant : ed.variants)
				{
					enumType.getMemberScope().define(
						new VariableSymbol(variant, enumType, false, ed));
				}
			}
			else if (decl instanceof UnionDeclaration ud)
			{
				UnionType unionType = new UnionType(ud.name, currentScope);
				TypeSymbol sym = new TypeSymbol(ud.name, unionType, ud);
				if (!currentScope.forceDefine(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, ud, ud.name);
				}
				if (!ud.attributes.isEmpty())
					sym.setAttributes(collectAttributeInfos(ud.attributes));
			}
			else if (decl instanceof TraitDeclaration td)
			{
				TraitType traitType = new TraitType(td.name, currentScope);
				TypeSymbol sym = new TypeSymbol(td.name, traitType, td);
				if (!currentScope.forceDefine(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, td, td.name);
				}
				if (!td.attributes.isEmpty())
					sym.setAttributes(collectAttributeInfos(td.attributes));
			}
			else if (decl instanceof TagStatement ts)
			{
				// Register a stub so the tag alias is resolvable as a bound
				// in generic parameter declarations (phase 1.5). The member types
				// are populated later in declareTagBodies (phase 1.8).
				TagType tagType = new TagType(ts.alias, currentScope);
				TypeSymbol sym = new TypeSymbol(ts.alias, tagType, ts);
				if (!currentScope.forceDefine(sym))
				{
					error(DiagnosticCode.TYPE_ALREADY_DEFINED, ts, ts.alias);
				}
			}
		}
	}

	// =========================================================================
	// Phase 1.8: Resolve tag expressions and build tag member scopes
	// =========================================================================

	/**
	 * Phase 1.8: Resolves each tag expression and populates the corresponding
	 * {@link TagType} with its member types. Must run after
	 * {@code declareTraitBodies} so that trait types referenced inside tags
	 * already have their member scopes populated.
	 */
	public void declareTagBodies(CompilationUnit unit)
	{
		currentScope = globalScope;
		processDirectives(unit);
		declareTagBodiesRecursive(unit.declarations);
	}

	private void declareTagBodiesRecursive(List<ASTNode> declarations)
	{
		if (declarations == null)
			return;
		for (ASTNode decl : declarations)
		{
			if (decl instanceof NamespaceDeclaration nd)
			{
				if (nd.isBlockDeclaration)
				{
					SymbolTable original = currentScope;
					currentScope = enterNamespace(nd, currentScope);
					declareTagBodiesRecursive(nd.members);
					currentScope = original;
				}
				else
				{
					currentScope = enterNamespace(nd, globalScope);
				}
			}
			else if (decl instanceof TagStatement ts)
			{
				TypeSymbol sym = currentScope.resolveType(ts.alias);
				if (sym != null && sym.getType() instanceof TagType tagType)
				{
					java.util.Set<String> visiting = new java.util.HashSet<>();
					visiting.add(tagType.name());
					resolveTagExpression(ts.tagExpression, tagType, visiting);
					tagType.buildMemberScope();
				}
			}
		}
	}

	/**
	 * Recursively traverses a {@link TagExpression} and adds each resolved
	 * {@link Type} to the given {@link TagType}.
	 */
	private void resolveTagExpression(
			org.nebula.nebc.ast.tags.TagExpression expr, TagType tagType,
			java.util.Set<String> visiting)
	{
		if (expr instanceof TagAtom atom)
		{
			Type resolved = resolveTagMemberType(atom.type);
			if (resolved == Type.ERROR)
				return;
			if (resolved instanceof TagType inner)
			{
				// Tag-of-tag: check for cycles, then inline expanded members
				if (visiting.contains(inner.name()))
				{
					error(DiagnosticCode.TAG_CYCLE, atom, tagType.name());
					return;
				}
				// Inline all concrete members (no TagType nested further)
				for (Type m : inner.getMemberTypes())
					if (!(m instanceof TagType))
						tagType.addMember(m);
			}
			else
			{
				tagType.addMember(resolved);
			}
		}
		else if (expr instanceof TagOperation op)
		{
			resolveTagExpression(op.left, tagType, visiting);
			if (op.right != null)
				resolveTagExpression(op.right, tagType, visiting);
		}
	}

	/**
	 * Phase 1.75: Populate trait member scopes by visiting trait declarations.
	 */
	public void declareTraitBodies(CompilationUnit unit)
	{
		currentScope = globalScope;
		processDirectives(unit);
		declareTraitBodiesRecursive(unit.declarations);
	}

	private void declareTraitBodiesRecursive(List<ASTNode> declarations)
	{
		if (declarations == null)
			return;
		for (ASTNode decl : declarations)
		{
			if (decl instanceof NamespaceDeclaration nd)
			{
				if (nd.isBlockDeclaration)
				{
					SymbolTable original = currentScope;
					currentScope = enterNamespace(nd, currentScope);
					declareTraitBodiesRecursive(nd.members);
					currentScope = original;
				}
				else
				{
					currentScope = enterNamespace(nd, globalScope);
				}
			}
			else if (decl instanceof TraitDeclaration td)
			{
				visitTraitDeclaration(td);
			}
		}
	}

	public void declareMethods(CompilationUnit unit)
	{
		currentScope = globalScope;
		processDirectives(unit);
		declareMethodsRecursive(unit.declarations);
	}

	private void declareMethodsRecursive(List<ASTNode> declarations)
	{
		if (declarations == null)
			return;
		for (ASTNode decl : declarations)
		{
			if (decl instanceof NamespaceDeclaration nd)
			{
				if (nd.isBlockDeclaration)
				{
					SymbolTable original = currentScope;
					currentScope = enterNamespace(nd, currentScope);
					declareMethodsRecursive(nd.members);
					currentScope = original;
				}
				else
				{
					currentScope = enterNamespace(nd, globalScope);
				}
			}
			else if (decl instanceof MethodDeclaration md)
			{
				defineMethodSignature(md);
			}
			else if (decl instanceof ExternDeclaration ed)
			{
				ed.accept(this);
			}
		}
	}

	// =========================================================================
	// Phase 1.9: Resolve class inheritance and import inherited members
	// =========================================================================

	/**
	 * Phase 1.9: For every {@link ClassDeclaration} that has an inheritance
	 * clause, resolves each parent type that is itself a {@link ClassType},
	 * registers it on the {@link ClassType}, and imports all parent-scope symbols
	 * into the child's member scope (excluding names already declared in the
	 * child — child declarations always win).
	 *
	 * <p>Must run after Phase 1.5 ({@code declareMethods}) so that all class
	 * member scopes already hold their own method stubs.
	 */
	public void declareInheritance(CompilationUnit unit)
	{
		currentScope = globalScope;
		processDirectives(unit);
		declareInheritanceRecursive(unit.declarations);
	}

	private void declareInheritanceRecursive(List<ASTNode> declarations)
	{
		if (declarations == null)
			return;
		for (ASTNode decl : declarations)
		{
			if (decl instanceof NamespaceDeclaration nd)
			{
				if (nd.isBlockDeclaration)
				{
					SymbolTable original = currentScope;
					currentScope = enterNamespace(nd, currentScope);
					declareInheritanceRecursive(nd.members);
					currentScope = original;
				}
				else
				{
					currentScope = enterNamespace(nd, globalScope);
				}
			}
			else if (decl instanceof ClassDeclaration cd && cd.inheritance != null)
			{
				TypeSymbol sym = currentScope.resolveType(cd.name);
				if (sym == null || !(sym.getType() instanceof ClassType classType))
					continue;
				for (org.nebula.nebc.ast.types.TypeNode inheritedNode : cd.inheritance)
				{
					Type resolved = resolveType(inheritedNode);
					if (resolved instanceof ClassType parentClassType)
					{
						classType.addParent(parentClassType);
						registerSuperScopes(classType, parentClassType);
					}
				}
			}
		}
	}

	/**
	 * Recursively registers {@code parent}'s member scope (and all its ancestor
	 * scopes) as super-scopes of {@code child}'s member scope.
	 *
	 * <p>The registration is done depth-first so that the most-distant ancestor
	 * is consulted before closer ones when there is a name conflict.  Lookup is
	 * lazy — the actual symbol walk happens during Phase 2 when the parent
	 * member scopes are fully populated by {@code visitCompositeBody}.
	 */
	private void registerSuperScopes(ClassType child, ClassType parent)
	{
		// Register grandparents first (depth-first, most-distant ancestors first)
		for (ClassType grandParent : parent.getParentTypes())
		{
			registerSuperScopes(child, grandParent);
		}
		// Register parent's member scope as a super scope for lazy inherited lookup
		child.getMemberScope().addSuperScope(parent.getMemberScope());
	}

	// =========================================================================
	// Phase 1.95: Pre-populate class member scopes across all units
	// =========================================================================

	/**
	 * Phase 1.95: For every {@link ClassDeclaration} in the unit, enters its
	 * member scope and pre-declares {@code this} plus all method / constructor /
	 * operator signatures <em>without</em> visiting method bodies.
	 *
	 * <p>This ensures that parent class member scopes are fully populated with
	 * their own symbols before Phase 2 starts analysing child classes, so that
	 * inherited-member resolution via super-scopes works regardless of the order
	 * in which compilation units are processed in Phase 2.
	 */
	public void declareClassBodies(CompilationUnit unit)
	{
		currentScope = globalScope;
		processDirectives(unit);
		declareClassBodiesRecursive(unit.declarations);
	}

	private void declareClassBodiesRecursive(List<ASTNode> declarations)
	{
		if (declarations == null)
			return;
		for (ASTNode decl : declarations)
		{
			if (decl instanceof NamespaceDeclaration nd)
			{
				if (nd.isBlockDeclaration)
				{
					SymbolTable original = currentScope;
					currentScope = enterNamespace(nd, currentScope);
					declareClassBodiesRecursive(nd.members);
					currentScope = original;
				}
				else
				{
					currentScope = enterNamespace(nd, globalScope);
				}
			}
			else if (decl instanceof ClassDeclaration cd)
			{
				TypeSymbol sym = currentScope.resolveType(cd.name);
				if (sym == null || !(sym.getType() instanceof ClassType classType))
					continue;

				SymbolTable outerScope = currentScope;
				Type prevTypeDef = currentTypeDefinition;
				currentScope = classType.getMemberScope();
				currentTypeDefinition = classType;
				defineTypeParamsInScope(cd.typeParams, classType.getMemberScope());

				// 'this' — no-op if already defined
				currentScope.define(new VariableSymbol("this", classType, false, cd));

				// Pre-declare each member signature, guarding against duplicates
				for (Declaration member : cd.members)
				{
					if (member instanceof MethodDeclaration md
							&& currentScope.resolveLocal(md.name) == null)
					{
						defineMethodSignature(md);
					}
					else if (member instanceof ConstructorDeclaration ctorDecl
							&& currentScope.resolveLocal(ctorDecl.name) == null)
					{
						defineConstructorSignature(ctorDecl);
					}
					else if (member instanceof OperatorDeclaration od
							&& currentScope.resolveLocal("operator" + od.operatorToken) == null)
					{
						defineOperatorSignature(od);
					}
				}

				currentTypeDefinition = prevTypeDef;
				currentScope = outerScope;
			}
			else if (decl instanceof StructDeclaration sd)
			{
				TypeSymbol sym = currentScope.resolveType(sd.name);
				if (sym == null || !(sym.getType() instanceof StructType structType))
					continue;

				SymbolTable outerScope = currentScope;
				Type prevTypeDef = currentTypeDefinition;
				currentScope = structType.getMemberScope();
				currentTypeDefinition = structType;
				defineTypeParamsInScope(sd.typeParams, structType.getMemberScope());

				// 'this' — no-op if already defined
				currentScope.define(new VariableSymbol("this", structType, false, sd));

				// Pre-declare each member signature, guarding against duplicates
				for (Declaration member : sd.members)
				{
					if (member instanceof MethodDeclaration md
							&& currentScope.resolveLocal(md.name) == null)
					{
						defineMethodSignature(md);
					}
					else if (member instanceof ConstructorDeclaration ctorDecl
							&& currentScope.resolveLocal(ctorDecl.name) == null)
					{
						defineConstructorSignature(ctorDecl);
					}
					else if (member instanceof OperatorDeclaration od
							&& currentScope.resolveLocal("operator" + od.operatorToken) == null)
					{
						defineOperatorSignature(od);
					}
				}

				currentTypeDefinition = prevTypeDef;
				currentScope = outerScope;
			}
		}
	}

	private void processDirectives(CompilationUnit unit)
	{
		for (ASTNode directive : unit.directives)
		{
			directive.accept(this);
		}
	}

	private SymbolTable enterNamespace(NamespaceDeclaration node, SymbolTable baseScope)
	{
		String[] parts = node.name.split("::");
		SymbolTable table = baseScope;

		for (String part : parts)
		{
			Symbol existing = table.resolveLocal(part);
			NamespaceSymbol nsSym;
			if (existing instanceof NamespaceSymbol ns)
			{
				nsSym = ns;
			}
			else
			{
				NamespaceType nsType = new NamespaceType(part, table);
				nsSym = new NamespaceSymbol(part, nsType, node);
				table.define(nsSym);
			}
			// Ensure the member table's owner is set so that getMangledName() can
			// walk the definedIn chain and produce fully-qualified mangled names
			// (e.g. std__reflect__total instead of just total).
			nsSym.getMemberTable().setOwner(nsSym);
			table = nsSym.getMemberTable();
		}
		return table;
	}

	// =========================================================================
	// Phase 1.97: Pre-register all impl method signatures on target types.
	// Running this before Phase 2 ensures that a type's trait methods are
	// visible to any code that references the type, regardless of source order.
	// =========================================================================

	public void declareImplBodies(CompilationUnit unit)
	{
		currentScope = globalScope;
		processDirectives(unit);
		declareImplBodiesRecursive(unit.declarations);
	}

	private void declareImplBodiesRecursive(List<ASTNode> declarations)
	{
		if (declarations == null)
			return;
		for (ASTNode decl : declarations)
		{
			if (decl instanceof NamespaceDeclaration nd)
			{
				if (nd.isBlockDeclaration)
				{
					SymbolTable original = currentScope;
					currentScope = enterNamespace(nd, currentScope);
					declareImplBodiesRecursive(nd.members);
					currentScope = original;
				}
				else
				{
					currentScope = enterNamespace(nd, globalScope);
				}
			}
			else if (decl instanceof ImplDeclaration id)
			{
				preRegisterImplSignatures(id);
			}
		}
	}

	/**
	 * Registers the method signatures of an {@code impl} block on the target
	 * type's member scope <em>without</em> visiting method bodies.
	 * This makes trait methods (e.g. {@code toStr}) available to the type
	 * checker in Phase 2 regardless of source declaration order.
	 */
	private void preRegisterImplSignatures(ImplDeclaration node)
	{
		Type traitResolved = resolveTagMemberType(node.traitType);
		if (!(traitResolved instanceof TraitType traitType))
			return;

		Type targetType = resolveTagMemberType(node.targetType);
		if (targetType == Type.ERROR)
			return;

		if (targetType instanceof TagType tagTarget)
		{
			for (Type member : tagTarget.getMemberTypes())
			{
				if (member instanceof TagType || member instanceof TraitType)
					continue;
				preRegisterImplSignaturesForType(node, traitType, member);
			}
		}
		else
		{
			preRegisterImplSignaturesForType(node, traitType, targetType);
		}
	}

	private void preRegisterImplSignaturesForType(
			ImplDeclaration node, TraitType traitType, Type targetType)
	{
		SymbolTable targetScope;
		if (targetType instanceof CompositeType composite)
		{
			targetScope = composite.getMemberScope();
		}
		else if (targetType instanceof PrimitiveType pt)
		{
			targetScope = primitiveImplScopes.computeIfAbsent(targetType, t ->
			{
				SymbolTable st = new SymbolTable(currentScope);
				st.setOwner(new org.nebula.nebc.semantic.symbol.TypeSymbol(pt.name(), pt, null));
				return st;
			});
			targetScope.addSuperScope(currentScope);
		}
		else
		{
			return;
		}

		SymbolTable outerScope = currentScope;
		Type prevTypeDef = currentTypeDefinition;
		currentScope = targetScope;
		currentTypeDefinition = targetType;

		try
		{
			targetScope.define(new org.nebula.nebc.semantic.symbol.VariableSymbol("this", targetType, false, node));
			for (MethodDeclaration method : node.members)
			{
				if (targetScope.resolveLocal(method.name) == null)
				{
					defineMethodSignature(method);
					org.nebula.nebc.semantic.symbol.MethodSymbol ms = getSymbol(method, org.nebula.nebc.semantic.symbol.MethodSymbol.class);
					if (ms != null)
						ms.setTraitName(traitType.name());
				}
			}
		}
		finally
		{
			currentScope = outerScope;
			currentTypeDefinition = prevTypeDef;
		}
	}

	public List<Diagnostic> analyze(CompilationUnit unit)
	{
		currentScope = globalScope;
		processDirectives(unit);
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

	public SymbolTable getGlobalScope()
	{
		return globalScope;
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
	 * <p>
	 * <strong>Tag guard:</strong> if the resolved type is a {@link TagType}, this
	 * method emits {@link DiagnosticCode#TAG_AS_VALUE_TYPE} and returns
	 * {@link Type#ERROR}. Tags are compile-time-only constraints and may
	 * <em>not</em> appear in value-type position (variable declarations, parameter
	 * types, cast targets, etc.).
	 * <p>
	 * Use {@link #resolveTagMemberType} when resolving types that appear inside a
	 * tag expression or in a generic-bound position.
	 */
	private Type resolveType(TypeNode astType)
	{
		Type result = resolveTagMemberType(astType);
		if (result instanceof TagType tag)
		{
			error(DiagnosticCode.TAG_AS_VALUE_TYPE, astType, tag.name(), tag.name());
			return Type.ERROR;
		}
		return result;
	}

	/**
	 * Resolves a syntactic AST TypeNode to a semantic Type object without
	 * applying the "tag-as-value-type" guard.
	 * <p>
	 * This is the internal resolution path used by:
	 * <ul>
	 *   <li>Tag expression members ({@code tag { str, Signed } as X})</li>
	 *   <li>Generic bound positions ({@code <T: X>})</li>
	 * </ul>
	 * All other callers should use {@link #resolveType}.
	 */
	private Type resolveTagMemberType(TypeNode astType)
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
			Type baseType = ts.getType();
			// If the type has generic arguments (e.g. Pair<i32>), monomorphize by substituting
			if (!nt.genericArguments.isEmpty() && baseType instanceof CompositeType ct)
			{
				// Collect type parameters from the composite's member scope
				List<TypeParameterType> typeParams = ct.getMemberScope().getSymbols().values().stream()
					.filter(s -> s instanceof TypeSymbol tts && tts.getType() instanceof TypeParameterType)
					.map(s -> (TypeParameterType) s.getType())
					.collect(java.util.stream.Collectors.toList());
				if (!typeParams.isEmpty() && typeParams.size() == nt.genericArguments.size())
				{
					Substitution sub = new Substitution();
					for (int i = 0; i < typeParams.size(); i++)
					{
						Type argType = resolveTagMemberType(nt.genericArguments.get(i));
						sub.bind(typeParams.get(i), argType);
					}
					return sub.substitute(baseType);
				}
			}
			return baseType;
		}
		if (astType instanceof org.nebula.nebc.ast.types.OptionalTypeNode otn)
		{
			Type inner = resolveTagMemberType(otn.innerType);
			if (inner == Type.ERROR)
				return Type.ERROR;
			return new OptionalType(inner);
		}
		if (astType instanceof org.nebula.nebc.ast.types.ArrayType atn)
		{
			Type elem = resolveTagMemberType(atn.baseType);
			if (elem == Type.ERROR)
				return Type.ERROR;
			return new ArrayType(elem, 0);
		}
		if (astType instanceof org.nebula.nebc.ast.types.TupleType ttn)
		{
			java.util.List<Type> elemTypes = new java.util.ArrayList<>();
			for (org.nebula.nebc.ast.types.TypeNode t : ttn.elementTypes)
			{
				Type resolved = resolveTagMemberType(t);
				if (resolved == Type.ERROR)
					return Type.ERROR;
				elemTypes.add(resolved);
			}
			return new TupleType(elemTypes, ttn.fieldNames);
		}
		return Type.ANY;
	}

	// =================================================================
	// DECLARATIONS
	// =================================================================

	@Override
	public Type visitCompilationUnit(CompilationUnit node)
	{
		currentScope = globalScope;
		// Phase 2: Full visitation
		for (ASTNode directive : node.directives)
		{
			directive.accept(this);
		}
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
		SymbolTable baseScope = node.isBlockDeclaration ? currentScope : globalScope;
		SymbolTable originalScope = currentScope;

		currentScope = enterNamespace(node, baseScope);

		// Pre-pass methods — only define if not already registered by Phase 1
		for (ASTNode member : node.members)
		{
			if (member instanceof MethodDeclaration md)
			{
				if (getSymbol(md, MethodSymbol.class) == null)
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
			currentScope = originalScope;
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
		// Record so codegen can retrieve the TypeSymbol via getSymbol(node, TypeSymbol.class).
		recordSymbol(node, existingSym);
		ClassType classType = (ClassType) existingSym.getType();
		defineTypeParamsInScope(node.typeParams, classType.getMemberScope());
		Type result = visitCompositeBody(node, classType, node.members);

		// Trait implementation check (Phase 1.9 already handled ClassType parents)
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
		// Record so codegen can retrieve the TypeSymbol via getSymbol(node, TypeSymbol.class).
		recordSymbol(node, existingSym);
		StructType structType = (StructType) existingSym.getType();
		defineTypeParamsInScope(node.typeParams, structType.getMemberScope());
		Type result = visitCompositeBody(node, structType, node.members);
		return result;
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

		// Pre-pass 1: Register fields (VariableDeclarations) so that constructors and
		// methods declared before a field in the source can still reference it.
		// Only non-initializer fields are pre-registered here; fields with initializers
		// are left to the main loop.
		for (Declaration member : members)
		{
			if (member instanceof VariableDeclaration vd && !vd.isVar)
			{
				// Only pre-register if all declarators lack an initializer (struct fields)
				boolean allNoInit = vd.declarators.stream().noneMatch(VariableDeclarator::hasInitializer);
				if (allNoInit)
				{
					Type fieldType = resolveType(vd.type);
					if (fieldType != null && !(fieldType instanceof TagType))
					{
						for (VariableDeclarator decl : vd.declarators)
						{
							if (currentScope.resolveLocal(decl.name()) == null)
							{
								VariableSymbol vs = new VariableSymbol(
									decl.name(), fieldType, false, vd.isBacklink, vd);
								currentScope.define(vs);
								recordSymbol(vd, vs);
							}
						}
					}
				}
			}
		}

		// Pre-pass 2: Register methods — skip any signature already declared by Phase 1.95
		// to avoid duplicate-symbol errors when the same class is processed twice.
		for (Declaration member : members)
		{
			if (member instanceof MethodDeclaration md
					&& currentScope.resolveLocal(md.name) == null)
			{
				defineMethodSignature(md);
			}
			else if (member instanceof ConstructorDeclaration cd
					&& currentScope.resolveLocal(cd.name) == null)
			{
				defineConstructorSignature(cd);
			}
			else if (member instanceof OperatorDeclaration od
					&& currentScope.resolveLocal("operator" + od.operatorToken) == null)
			{
				defineOperatorSignature(od);
			}
			else if (member instanceof ExternDeclaration ed)
			{
				ed.accept(this);
			}
		}

		// Visit members — skip VariableDeclarations that were already pre-registered
		// (i.e. plain struct fields without initializers).
		for (Declaration member : members)
		{
			if (member instanceof VariableDeclaration vd && !vd.isVar)
			{
				boolean allNoInit = vd.declarators.stream().noneMatch(VariableDeclarator::hasInitializer);
				if (allNoInit)
				{
					// Already handled in pre-pass; just ensure the symbol is recorded.
					continue;
				}
			}
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

		// Record the symbol so codegen can retrieve the TypeSymbol via getSymbol(node).
		recordSymbol(node, existingSym);

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
		boolean isExternC = "C".equals(node.language);
		for (MethodDeclaration member : node.members)
		{
			// Guard against re-registration: declareMethodsRecursive (Phase 1.5),
			// the namespace/composite pre-pass, and the main member loop all call
			// visitExternDeclaration on the same node. Only define on first encounter.
			if (getSymbol(member, MethodSymbol.class) == null)
			{
				defineMethodSignature(member);
			}
			// Mark the symbol so codegen can apply C-ABI lowering (str → ptr)
			// only for genuine extern "C" declarations, not for Nebula-aware extern
			// functions imported from .nebsym symbol files.
			if (isExternC)
			{
				MethodSymbol ms = getSymbol(member, MethodSymbol.class);
				if (ms != null)
					ms.setExplicitExternC(true);
			}
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
				CompositeType bound = null;
				if (gp.hasBound() && gp.bound() instanceof org.nebula.nebc.ast.types.NamedType nt)
				{
					TypeSymbol boundSym = outerScope.resolveType(nt.qualifiedName);
					if (boundSym != null && boundSym.getType() instanceof TraitType tt)
					{
						bound = tt;
					}
					else if (boundSym != null && boundSym.getType() instanceof TagType tag)
					{
						bound = tag;
					}
					else
					{
						error(DiagnosticCode.UNDEFINED_SYMBOL, node, "Unknown trait/tag bound '" + nt.qualifiedName + "'");
					}
				}
				TypeParameterType tpt = new TypeParameterType(gp.name(), bound);
				typeParams.add(tpt);
				currentScope.define(new TypeSymbol(gp.name(), tpt, node));
			}
		}

		Type returnType = (node.returnType == null) ? PrimitiveType.VOID : resolveType(node.returnType);

		// 1. Build function signature
		// Always create paramInfos so the codegen can see CVT modifiers (drops/keeps)
		// on both extern "C" declarations and normal Nebula functions.
		List<Type> paramTypes = new ArrayList<>();
		List<ParameterInfo> paramInfos = new ArrayList<>();

		for (Parameter p : node.parameters)
		{
			Type pType = resolveType(p.type());
			// A tag cannot appear as a parameter type — the caller must use a
			// type parameter constrained by that tag: <T: TagName>(T param).
			if (pType instanceof TagType tag)
			{
				error(DiagnosticCode.TAG_IN_PARAM_POSITION, node, tag.name(), tag.name());
				pType = Type.ERROR;
			}
			paramTypes.add(pType == Type.ERROR ? Type.ANY : pType);
			paramInfos.add(new ParameterInfo(p.cvtModifier(), pType, p.name()));
		}

		// Prepend 'this' parameter for member methods
		if (currentTypeDefinition != null)
		{
			paramTypes.add(0, currentTypeDefinition);
			paramInfos.add(0, new ParameterInfo(null, currentTypeDefinition, "this"));
		}

		FunctionType methodType = new FunctionType(returnType, paramTypes, paramInfos);

		// 2. Define method in the OUTER scope (not the type-param scope)
		MethodSymbol methodSym = new MethodSymbol(node.name, methodType, node.modifiers, isInsideExtern || node.isExtern, node, typeParams);
		if (!node.attributes.isEmpty())
			methodSym.setAttributes(collectAttributeInfos(node.attributes));
		recordSymbol(node, methodSym);
		SymbolTable defineIn = (outerScope != null) ? outerScope : currentScope;
		if (!defineIn.forceDefine(methodSym))
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
				if (!isValidMainParams(paramTypes))
				{
					error(DiagnosticCode.INVALID_MAIN_SIGNATURE, node);
				}
				mainMethod = node;
				mainMethodReturnType = returnType;
			}
		}
	}

	/**
	 * Returns true if the given resolved parameter type list is a valid signature
	 * for the 'main' entry point.  The only valid forms are:
	 * <ul>
	 *   <li>{@code main()}         — no parameters</li>
	 *   <li>{@code main(str[] args)} — exactly one {@code str[]} parameter</li>
	 * </ul>
	 */
	private boolean isValidMainParams(List<Type> paramTypes)
	{
		if (paramTypes.isEmpty())
			return true;
		if (paramTypes.size() == 1
			&& paramTypes.get(0) instanceof ArrayType at
			&& at.baseType == PrimitiveType.STR)
		{
			return true;
		}
		return false;
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
		// Tags are compile-time-only; resolveType already emits the diagnostic,
		// but we short-circuit here to avoid cascading errors.
		if (explicitType instanceof TagType)
			return null;
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
						// Allow integer literal narrowing when the literal value fits the declared type
						boolean literalNarrowing = initType instanceof PrimitiveType pi && pi.isInteger()
							&& explicitType instanceof PrimitiveType pe && pe.isInteger()
							&& decl.initializer() instanceof org.nebula.nebc.ast.expressions.LiteralExpression lit
							&& lit.value instanceof Long lv
							&& intLiteralFitsInType(lv, pe);
						if (!literalNarrowing)
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
				VariableSymbol varSym = new VariableSymbol(
					decl.name(), actualType, mutable, node.isBacklink, node);
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

		// Check operator overloading on composite types before primitive dispatch
		if (left instanceof CompositeType ct)
		{
			String opName = operatorMethodName(node.operator);
			if (opName != null)
			{
				Symbol opSym = ct.getMemberScope().resolve(opName);
				if (opSym instanceof MethodSymbol ms)
				{
					FunctionType ft = ms.getType();
					Type result = ft.returnType;
					recordType(node, result);
					return result;
				}
			}
		}

		Type result = Type.ERROR;
		switch (node.operator)
		{
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:
			case POW:
				if (left instanceof TypeParameterType || right instanceof TypeParameterType)
				{
					// Generic type parameter — resolved at monomorphization; return left type
					result = left instanceof TypeParameterType ? right : left;
					if (result instanceof TypeParameterType)
						result = left;
				}
				else if (isNumeric(left) && isNumeric(right))
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
			{
				// Allow comparison with none (OptionalType(ANY)) — always valid for optional types
				boolean eitherIsNone = isNoneLiteral(left) || isNoneLiteral(right);
				boolean eitherIsOptional = left instanceof OptionalType || right instanceof OptionalType;
				if (!left.equals(right) && !(isNumeric(left) && isNumeric(right)) && !(eitherIsNone && eitherIsOptional))
				{
					error(DiagnosticCode.COMPARING_DISTINCT_TYPES, node, left.name(), right.name());
				}
				result = PrimitiveType.BOOL;
				break;
			}

			case LT:
			case GT:
			case LE:
			case GE:
				if (isNumeric(left) && isNumeric(right))
					result = PrimitiveType.BOOL;
				else if (left instanceof TypeParameterType || right instanceof TypeParameterType)
					result = PrimitiveType.BOOL; // generic type — resolved at monomorphization
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

			case BIT_AND:
			case BIT_OR:
			case BIT_XOR:
			case SHL:
			case SHR:
				if (isIntegral(left) && isIntegral(right))
				{
					PrimitiveType pLeft = (PrimitiveType) left;
					PrimitiveType pRight = (PrimitiveType) right;
					result = pLeft.getBitWidth() >= pRight.getBitWidth() ? pLeft : pRight;
				}
				else if (isIntegral(left))
				{
					// Shift: result is the left operand's type
					result = left;
				}
				else
				{
					error(DiagnosticCode.OPERATOR_NOT_DEFINED, node, node.operator, left.name(), right.name());
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
			// Bare constructor call — identifier resolves to the constructor MethodSymbol whose
			// first parameter is the implicit 'this' (REF).  At the call site the caller does NOT
			// provide 'this'; skip that parameter when validating argument count / types so that
			// Vec2(x, y) with ctor FunctionType([REF, f32, f32]) is treated as expecting 2 args.
			if (node.target instanceof IdentifierExpression ie
					&& !fn.parameterTypes.isEmpty()
					&& fn.parameterTypes.get(0) == PrimitiveType.REF)
			{
				TypeSymbol ts = currentScope.resolveType(ie.name);
				if (ts != null && ts.getType() instanceof CompositeType ct)
				{
					// Reconstruct FunctionType without the 'this' parameter for argument checking
					List<Type> reducedParams = fn.parameterTypes.subList(1, fn.parameterTypes.size());

					// If the struct is generic, infer type arguments from the call-site arguments
					// to produce a monomorphized return type (e.g. Pair(3, 7) -> Pair<i32>).
					boolean hasTypeParams = reducedParams.stream().anyMatch(p -> p instanceof TypeParameterType);
					if (hasTypeParams && reducedParams.size() == node.arguments.size())
					{
						Substitution ctorSub = new Substitution();
						for (int i = 0; i < node.arguments.size(); i++)
						{
							Type argType = node.arguments.get(i).accept(this);
							infer(reducedParams.get(i), argType, ctorSub);
						}
						if (!ctorSub.isEmpty())
						{
							// Substitute params and return type
							List<Type> concreteParams = reducedParams.stream()
								.map(ctorSub::substitute)
								.collect(java.util.stream.Collectors.toList());
							Type concreteReturn = ctorSub.substitute(ct);
							fn = new FunctionType(concreteReturn, concreteParams);
							targetType = concreteReturn;
							methodSym = null;
							// Record the inferred type arguments on the node for codegen
							List<Type> typeArgsList = new java.util.ArrayList<>();
							for (java.util.Map.Entry<TypeParameterType, Type> entry : ctorSub.getMapping().entrySet())
							{
								typeArgsList.add(entry.getValue());
							}
							node.setTypeArguments(typeArgsList);
						}
						else
						{
							fn = new FunctionType(ct, reducedParams);
							targetType = ct;
							methodSym = null;
						}
					}
					else
					{
						fn = new FunctionType(ct, reducedParams);
						methodSym = null;
						targetType = ct;
					}
				}
			}
		}
		else if (targetType instanceof CompositeType compositeTarget)
		{
			// Constructor call — the identifier resolved directly to the TypeSymbol.
			// Check whether the struct/class is generic; if so, look up its constructor
			// MethodSymbol and perform type inference to monomorphize the return type.
			if (node.target instanceof IdentifierExpression ie)
			{
				// Qualified constructor call: std::fn::FnRef(x) has ie.name = "std::fn::FnRef".
				// The constructor is stored in the member scope under the simple type name only
				// (e.g. "FnRef"), so strip any namespace prefix before the lookup.
				String ctorKey = ie.name.contains("::")
						? ie.name.substring(ie.name.lastIndexOf("::") + 2)
						: ie.name;
				Symbol ctorSym = compositeTarget.getMemberScope().resolveLocal(ctorKey);
				if (ctorSym instanceof MethodSymbol ctorMethod)
				{
					FunctionType ctorFnType = ctorMethod.getType();
					List<Type> reducedParams = ctorFnType.parameterTypes.subList(1, ctorFnType.parameterTypes.size()); // skip REF
					boolean hasTypeParams = reducedParams.stream().anyMatch(p -> p instanceof TypeParameterType);
					if (hasTypeParams && reducedParams.size() == node.arguments.size())
					{
						Substitution ctorSub = new Substitution();
						for (int i = 0; i < node.arguments.size(); i++)
						{
							Type argType = node.arguments.get(i).accept(this);
							infer(reducedParams.get(i), argType, ctorSub);
						}
						if (!ctorSub.isEmpty())
						{
							List<Type> concreteParams = reducedParams.stream()
								.map(ctorSub::substitute)
								.collect(java.util.stream.Collectors.toList());
							Type concreteReturn = ctorSub.substitute(compositeTarget);
							fn = new FunctionType(concreteReturn, concreteParams);
							targetType = concreteReturn;
							methodSym = null;
							// Record inferred type arguments on the node for codegen
							List<Type> typeArgsList = new java.util.ArrayList<>();
							for (java.util.Map.Entry<TypeParameterType, Type> entry : ctorSub.getMapping().entrySet())
							{
								typeArgsList.add(entry.getValue());
							}
							node.setTypeArguments(typeArgsList);
						}
						else
						{
							fn = null; // fall through to simple constructor path
						}
					}
					else
					{
						fn = null; // non-generic constructor
					}
				}
				else
				{
					// No constructor symbol found at all.  For structs this is a hard error —
					// all fields must be initialised through an explicit constructor.
					if (compositeTarget instanceof StructType)
					{
						error(DiagnosticCode.STRUCT_MISSING_CONSTRUCTOR, node, compositeTarget.name());
						return Type.ERROR;
					}
					fn = null;
				}
			}
			else
			{
				fn = null;
			}
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
			// only if the function's first parameter is the object type (i.e. 'this').
			if (node.target instanceof MemberAccessExpression mae && !fn.parameterTypes.isEmpty())
			{
				Type receiverType = mae.target.accept(this);
				Type firstParam = fn.parameterTypes.get(0);
				if (receiverMatchesFirstParam(receiverType, firstParam))
				{
					effectiveArgs.add(0, mae.target);
				}
			}

			// Bare inherited member call: greet() inside Child.run() where greet() is defined
			// in Base.  The resolved FunctionType has `this: Base` as its first parameter, but
			// the call site provides no explicit receiver.  When we are inside a composite body
			// (currentTypeDefinition != null) and the first param is a CompositeType that the
			// current class is assignable to, synthesise an implicit 'this' expression.
			if (node.target instanceof IdentifierExpression
					&& !fn.parameterTypes.isEmpty()
					&& fn.parameterTypes.get(0) instanceof CompositeType firstParamComposite
					&& currentTypeDefinition instanceof ClassType callerClass
					&& (callerClass == firstParamComposite || callerClass.isAssignableTo((ClassType) firstParamComposite))
					&& effectiveArgs.size() < fn.parameterTypes.size())
			{
				// Inject a synthetic 'this' identifier as the implicit receiver
				effectiveArgs.add(0, new org.nebula.nebc.ast.expressions.IdentifierExpression(node.getSpan(), "this"));
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

					// Validate bounds (trait or tag)
					if (tpt.getBound() != null)
					{
						if (tpt.getBound() instanceof TraitType traitBound)
						{
							SymbolTable memberScope = null;
							if (concrete instanceof CompositeType ct)
								memberScope = ct.getMemberScope();
							else if (concrete instanceof PrimitiveType pt)
								memberScope = primitiveImplScopes.get(pt);

							if (memberScope == null)
							{
								error(DiagnosticCode.TYPE_MISMATCH, node, traitBound.name(),
										concrete.name() + " (Cannot implement traits or no trait implementation found)");
							}
							else
							{
								String missing = traitBound.findMissingMethod(memberScope);
								if (missing != null)
								{
									error(DiagnosticCode.TYPE_MISMATCH, node, traitBound.name(),
											concrete.name() + " (missing method '" + missing + "')");
								}
							}
						}
						else if (tpt.getBound() instanceof TagType tagBound)
						{
							if (!tagBound.isSatisfiedBy(concrete, primitiveImplScopes))
							{
								error(DiagnosticCode.TYPE_MISMATCH, node, tagBound.name(),
										concrete.name() + " (type is not a member of tag '" + tagBound.name() + "')");
							}
							else if (concrete instanceof TupleType || concrete instanceof ArrayType)
							{
								// Eagerly register the synthetic Stringable impl scope so the
								// code-generator can build vtable wrappers for this structural type.
								getOrCreateStructuralStringableScope(concrete);
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
			// Constructor call — targetType is already the ClassType/StructType.
			// Still visit each argument so the SA records their types for codegen.
			for (Expression arg : node.arguments)
			{
				arg.accept(this);
			}
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

		// Guard: bare member access on an optional type requires '?.' safe access
		if (objectType instanceof OptionalType ot && !node.isSafe)
		{
			error(DiagnosticCode.UNSAFE_MEMBER_ACCESS_ON_OPTIONAL, node, objectType.name());
			return Type.ERROR;
		}

		// For safe optional chaining '?.', unwrap the optional and resolve on inner type
		if (node.isSafe && objectType instanceof OptionalType ot)
		{
			objectType = ot.innerType;
		}

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
		if (objectType instanceof TupleType tt)
		{
			// Tuple member access: .0, .1 (positional) or .fieldName (named)
			try
			{
				int index = Integer.parseInt(node.memberName);
				if (index >= 0 && index < tt.elementTypes.size())
				{
					Type result = tt.elementTypes.get(index);
					recordType(node, result);
					return result;
				}
				error(DiagnosticCode.MEMBER_NOT_FOUND, node, node.memberName, objectType.name());
				return Type.ERROR;
			}
			catch (NumberFormatException e)
			{
				// Named tuple member access: try field names first
				int idx = tt.indexOfField(node.memberName);
				if (idx >= 0)
				{
					Type result = tt.elementTypes.get(idx);
					recordType(node, result);
					return result;
				}
				// Fall through: check for trait methods via synthetic Stringable scope
				memberScope = getOrCreateStructuralStringableScope(tt);
				if (memberScope == null)
				{
					error(DiagnosticCode.MEMBER_NOT_FOUND, node, node.memberName, objectType.name());
					return Type.ERROR;
				}
			}
		}
		else if (objectType instanceof CompositeType ct)
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
		else if (objectType instanceof ArrayType)
		{
			if (node.memberName.equals("len"))
			{
				recordType(node, PrimitiveType.I64);
				return PrimitiveType.I64;
			}
			// Arrays expose trait methods (e.g. toStr) via the synthetic Stringable scope
			memberScope = getOrCreateStructuralStringableScope(objectType);
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
		recordSymbol(node, memberSym);
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitIdentifierExpression(IdentifierExpression node)
	{
		Symbol sym = currentScope.resolve(node.name);
		if (sym == null)
			sym = resolveEnumVariant(node.name);
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
		if (ts.getType() instanceof TagType tag)
		{
			error(DiagnosticCode.TAG_AS_VALUE_TYPE, node, tag.name(), tag.name());
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

	/**
	 * Fallback identifier resolution that searches all enum member scopes reachable
	 * from the current scope chain.  Allows unqualified enum variant names (e.g.
	 * {@code Super} instead of {@code Form.Super}) to be used as expressions when
	 * the context type is already constrained.
	 *
	 * <p>Walks each scope in the parent chain, inspects every {@link TypeSymbol}
	 * whose underlying type is an {@link EnumType}, and returns the first variant
	 * symbol whose name matches {@code name}.
	 */
	private Symbol resolveEnumVariant(String name)
	{
		SymbolTable scope = currentScope;
		while (scope != null)
		{
			for (Symbol sym : scope.getSymbols().values())
			{
				if (sym instanceof TypeSymbol ts && ts.getType() instanceof EnumType et)
				{
					Symbol variant = et.getMemberScope().resolveLocal(name);
					if (variant != null)
						return variant;
				}
			}
			scope = scope.getParent();
		}
		return null;
	}

	private boolean isIntegral(Type t)
	{
		if (t instanceof PrimitiveType p)
		{
			return p.isInteger();
		}
		return false;
	}

	/** Returns true if the type represents the 'none' literal (OptionalType wrapping ANY). */
	private boolean isNoneLiteral(Type t)
	{
		return t instanceof OptionalType ot && ot.innerType == Type.ANY;
	}

	/** Returns true if a long literal value fits within the range of the given integer primitive type. */
	private boolean intLiteralFitsInType(long value, PrimitiveType targetType)
	{
		return switch (targetType.name())
		{
			case "i8"  -> value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
			case "u8"  -> value >= 0 && value <= 255;
			case "i16" -> value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
			case "u16" -> value >= 0 && value <= 65535;
			case "i32" -> value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
			case "u32" -> value >= 0 && value <= 4294967295L;
			default    -> true; // i64, u64 — always fits
		};
	}

	/**
	 * Determines whether a receiver type matches the first parameter of a method,
	 * which is used to decide if the receiver should be prepended to the argument list.
	 * Handles direct equality, composite name equality, and TypeParameterType with
	 * a bound that matches a trait-typed first parameter.
	 */
	private boolean receiverMatchesFirstParam(Type receiverType, Type firstParam)
	{
		if (firstParam.equals(receiverType))
			return true;
		if (firstParam instanceof CompositeType && receiverType instanceof CompositeType)
		{
			// Exact name match
			if (firstParam.name().equals(receiverType.name()))
				return true;
			// Monomorphized receiver: e.g. receiver is "Pair<i32>" and param is "Pair".
			// The base name (before '<') must match.
			String fpBase = compositeBaseName(firstParam.name());
			String rvBase = compositeBaseName(receiverType.name());
			if (fpBase.equals(rvBase))
				return true;
			// Class inheritance: receiver is a subclass of the parameter type
			if (receiverType instanceof ClassType childClass
					&& firstParam instanceof ClassType parentClass
					&& childClass.isAssignableTo(parentClass))
				return true;
		}
		// REF is the implicit 'this' pointer for struct/class methods (constructors).
		if (firstParam == PrimitiveType.REF && receiverType instanceof CompositeType)
			return true;
		// TypeParameterType<T: Bound> — method declared on the trait or tag, receiver is T
		if (receiverType instanceof TypeParameterType tpt && tpt.hasBound()
				&& firstParam instanceof CompositeType ft
				&& ft.name().equals(tpt.getBound().name()))
			return true;
		// TypeParameterType<T: Bound> — method declared on a composite type (impl for T)
		if (receiverType instanceof TypeParameterType tpt2 && tpt2.hasBound()
				&& firstParam instanceof CompositeType)
			return true;
		return false;
	}

	/** Returns the base name of a composite type, stripping any generic parameters. */
	private static String compositeBaseName(String name)
	{
		int lt = name.indexOf('<');
		return lt >= 0 ? name.substring(0, lt) : name;
	}

	/**
	 * Maps a binary operator token to its Nebula operator declaration name.
	 * Returns null for operators that cannot be overloaded.
	 */
	private String operatorMethodName(org.nebula.nebc.ast.BinaryOperator op)
	{
		return switch (op)
		{
			case ADD  -> "operator+";
			case SUB  -> "operator-";
			case MUL  -> "operator*";
			case DIV  -> "operator/";
			case MOD  -> "operator%";
			case EQ   -> "operator==";
			case NE   -> "operator!=";
			case LT   -> "operator<";
			case GT   -> "operator>";
			case LE   -> "operator<=";
			case GE   -> "operator>=";
			default   -> null;
		};
	}

	/**
	 * Pushes a new scope containing TypeParameterType symbols for each generic
	 * parameter in {@code typeParams}. Returns the old scope so the caller can
	 * restore it, or {@code null} if there were no type params.
	 */
	private SymbolTable pushTypeParamScope(List<GenericParam> typeParams)
	{
		if (typeParams == null || typeParams.isEmpty())
			return null;
		SymbolTable outerScope = currentScope;
		currentScope = new SymbolTable(outerScope);
		for (GenericParam gp : typeParams)
		{
			CompositeType bound = null;
			if (gp.hasBound() && gp.bound() instanceof org.nebula.nebc.ast.types.NamedType nt)
			{
				TypeSymbol boundSym = outerScope.resolveType(nt.qualifiedName);
				if (boundSym != null && boundSym.getType() instanceof TraitType tt)
					bound = tt;
				else if (boundSym != null && boundSym.getType() instanceof TagType tag)
					bound = tag;
			}
			TypeParameterType tpt = new TypeParameterType(gp.name(), bound);
			currentScope.define(new TypeSymbol(gp.name(), tpt, null));
		}
		return outerScope;
	}

	/**
	 * Defines generic type parameters directly in the given {@code scope}.
	 * Used for struct/class declarations so that type parameters are visible
	 * inside the member scope when resolving field and method signatures.
	 */
	private void defineTypeParamsInScope(List<GenericParam> typeParams, SymbolTable scope)
	{
		if (typeParams == null || typeParams.isEmpty())
			return;
		for (GenericParam gp : typeParams)
		{
			CompositeType bound = null;
			if (gp.hasBound() && gp.bound() instanceof org.nebula.nebc.ast.types.NamedType nt)
			{
				TypeSymbol boundSym = currentScope.resolveType(nt.qualifiedName);
				if (boundSym != null && boundSym.getType() instanceof TraitType tt)
					bound = tt;
				else if (boundSym != null && boundSym.getType() instanceof TagType tag)
					bound = tag;
			}
			TypeParameterType tpt = new TypeParameterType(gp.name(), bound);
			scope.forceDefine(new TypeSymbol(gp.name(), tpt, null));
		}
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
		Type result = resolveTagMemberType(node.targetType);
		if (result instanceof TagType tag)
		{
			error(DiagnosticCode.TAG_AS_VALUE_TYPE, node, tag.name(), tag.name());
			return Type.ERROR;
		}
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

		// Unwrap optional target
		Type matchedType = (targetType instanceof OptionalType ot) ? ot.innerType : targetType;

		Type commonType = null;
		for (MatchArm arm : node.arms)
		{
			enterScope();
			// Bind destructuring variables before visiting the arm body
			if (arm.pattern instanceof DestructuringPattern dp)
			{
				if (!dp.bindings.isEmpty() && matchedType instanceof CompositeType ct)
				{
					SymbolTable memberScope = ct.getMemberScope();
					Symbol variantSym = memberScope.resolve(variantSimpleName(dp.variantName));
					if (variantSym instanceof MethodSymbol ms)
					{
						// FunctionType(unionType, [payloadType]) — skip the first 'this'-like param
						FunctionType fnType = ms.getType();
						// payload params start at index 0 (no prepended 'this' for variants)
						for (int i = 0; i < dp.bindings.size() && i < fnType.parameterTypes.size(); i++)
						{
							Type bindingType = fnType.parameterTypes.get(i);
							currentScope.define(new VariableSymbol(dp.bindings.get(i), bindingType, false, node));
						}
					}
				}
				else if (!dp.bindings.isEmpty())
				{
					// Unknown payload type — bind as ANY so the arm body can still resolve
					for (String binding : dp.bindings)
					{
						currentScope.define(new VariableSymbol(binding, Type.ANY, false, node));
					}
				}
			}
			arm.pattern.accept(this);
			Type armType = arm.result.accept(this);
			exitScope();
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

		// Exhaustiveness check: only for finite types (enums, unions, tuples of those)
		checkMatchExhaustiveness(node, matchedType);

		Type result = commonType != null ? commonType : Type.ANY;
		recordType(node, result);
		return result;
	}

	// =========================================================================
	// Match exhaustiveness helpers
	// =========================================================================

	/**
	 * Strips a qualified variant name down to just the simple variant identifier.
	 * E.g. {@code "Form::Normal"} → {@code "Normal"}, {@code "Normal"} → {@code "Normal"}.
	 */
	private static String variantSimpleName(String name)
	{
		int sep = name.lastIndexOf("::");
		return sep >= 0 ? name.substring(sep + 2) : name;
	}

	/**
	 * Checks that the match expression covers all possible values of the selector
	 * type. Emits {@link DiagnosticCode#NON_EXHAUSTIVE_MATCH} when patterns are
	 * missing. Only checks finite types (enums, unions, and tuples thereof).
	 */
	private void checkMatchExhaustiveness(MatchExpression node, Type selectorType)
	{
		if (selectorType == null || selectorType == Type.ANY || selectorType == Type.ERROR)
			return;

		if (selectorType instanceof EnumType et)
		{
			checkFiniteTypeExhaustiveness(node, et, getAllEnumVariants(et));
		}
		else if (selectorType instanceof UnionType ut)
		{
			checkFiniteTypeExhaustiveness(node, ut, getAllUnionVariants(ut));
		}
		else if (selectorType instanceof TupleType tt)
		{
			checkTupleExhaustiveness(node, tt);
		}
	}

	/**
	 * Returns all variant names declared in an {@link EnumType}.
	 * Only {@link VariableSymbol} entries whose declaration node is an
	 * {@link EnumDeclaration} are included — this precisely identifies enum
	 * variants and excludes synthetic {@code this} parameters from methods.
	 */
	private Set<String> getAllEnumVariants(EnumType et)
	{
		Set<String> result = new LinkedHashSet<>();
		for (Map.Entry<String, Symbol> entry : et.getMemberScope().getSymbols().entrySet())
		{
			Symbol sym = entry.getValue();
			if (sym instanceof VariableSymbol vs
					&& vs.getDeclarationNode() instanceof EnumDeclaration)
				result.add(entry.getKey());
		}
		return result;
	}

	/**
	 * Returns all variant names declared in a {@link UnionType}.
	 * No-payload variants are {@link VariableSymbol}s declared by a
	 * {@link UnionDeclaration}; payload variants are {@link MethodSymbol}s with
	 * the same declaration node. Both are included; {@code this} and other
	 * injected symbols are excluded.
	 */
	private Set<String> getAllUnionVariants(UnionType ut)
	{
		Set<String> result = new LinkedHashSet<>();
		for (Map.Entry<String, Symbol> entry : ut.getMemberScope().getSymbols().entrySet())
		{
			Symbol sym = entry.getValue();
			if (sym.getDeclarationNode() instanceof UnionDeclaration)
				result.add(entry.getKey());
		}
		return result;
	}

	/**
	 * Checks exhaustiveness for a single-dimension finite type (enum or union).
	 *
	 * @param node        the match expression (used for error reporting)
	 * @param type        the selector type
	 * @param allVariants the complete set of variant names for that type
	 */
	private void checkFiniteTypeExhaustiveness(
			MatchExpression node, CompositeType type, Set<String> allVariants)
	{
		Set<String> uncovered = new LinkedHashSet<>(allVariants);

		for (MatchArm arm : node.arms)
		{
			if (isWildcardArm(arm.pattern))
				return; // Wildcard covers everything

			Set<String> armCovered = collectSingleDimCovered(arm.pattern, type, allVariants);
			if (armCovered == null)
				return; // Wildcard inside arm — exhaustive
			uncovered.removeAll(armCovered);
			if (uncovered.isEmpty())
				return; // All covered
		}

		if (!uncovered.isEmpty())
		{
			error(DiagnosticCode.NON_EXHAUSTIVE_MATCH, node,
				type.name(),
				String.join(", ", uncovered));
		}
	}

	/**
	 * Checks exhaustiveness for a tuple selector whose element types are all
	 * finite (enum or union). Computes the Cartesian product and subtracts each
	 * arm's covered tuples.
	 */
	private void checkTupleExhaustiveness(MatchExpression node, TupleType tt)
	{
		// Build the list of variants per dimension
		List<List<String>> dimensions = new ArrayList<>();
		for (Type elemType : tt.elementTypes)
		{
			List<String> variants = getVariantsForType(elemType);
			if (variants == null)
				return; // Not a finite type — skip check
			dimensions.add(variants);
		}

		// Guard against combinatorial explosion
		int total = dimensions.stream().mapToInt(List::size).reduce(1, (a, b) -> a * b);
		if (total > 512)
			return;

		// Start with the full Cartesian product as uncovered
		Set<String> uncovered = new LinkedHashSet<>();
		buildCartesianProduct(dimensions, 0, new ArrayList<>(), uncovered);

		for (MatchArm arm : node.arms)
		{
			if (isWildcardArm(arm.pattern))
				return; // Wildcard covers everything

			if (!(arm.pattern instanceof TuplePattern tp))
				continue;

			Set<String> armCovered = computeTupleCoverage(tp, dimensions, tt.elementTypes);
			if (armCovered == null)
				return; // Contains wildcard — exhaustive
			uncovered.removeAll(armCovered);
			if (uncovered.isEmpty())
				return;
		}

		if (!uncovered.isEmpty())
		{
			String missing = uncovered.stream()
				.limit(5)
				.map(s -> "(" + s + ")")
				.collect(java.util.stream.Collectors.joining(", "));
			if (uncovered.size() > 5)
				missing += ", ...";
			error(DiagnosticCode.NON_EXHAUSTIVE_MATCH, node, tt.name(), missing);
		}
	}

	/**
	 * Returns the full list of variant names for a type, or {@code null} if the
	 * type is not a finite (enum/union) type.
	 */
	private List<String> getVariantsForType(Type type)
	{
		if (type instanceof EnumType et)
			return new ArrayList<>(getAllEnumVariants(et));
		if (type instanceof UnionType ut)
			return new ArrayList<>(getAllUnionVariants(ut));
		return null;
	}

	/**
	 * Determines which single-dimension variants are covered by {@code pat},
	 * given the expected {@code type}.
	 *
	 * @return the set of covered variant names, or {@code null} if {@code pat}
	 *         contains a wildcard (meaning all variants are covered).
	 */
	private Set<String> collectSingleDimCovered(Pattern pat, CompositeType type, Set<String> allVariants)
	{
		if (isWildcardArm(pat))
			return null;

		if (pat instanceof TypePattern tp && tp.type instanceof NamedType nt)
		{
			String variantName = resolvePatternVariantSimpleName(nt.qualifiedName, type);
			if (variantName != null)
			{
				Set<String> result = new LinkedHashSet<>();
				result.add(variantName);
				return result;
			}
		}

		if (pat instanceof DestructuringPattern dp)
		{
			String variantName = variantSimpleName(dp.variantName);
			if (type.getMemberScope().resolveLocal(variantName) != null)
			{
				Set<String> result = new LinkedHashSet<>();
				result.add(variantName);
				return result;
			}
		}

		if (pat instanceof OrPattern op)
		{
			Set<String> result = new LinkedHashSet<>();
			for (Pattern alt : op.alternatives)
			{
				Set<String> altCovered = collectSingleDimCovered(alt, type, allVariants);
				if (altCovered == null)
					return null; // One alt is wildcard — covers all
				result.addAll(altCovered);
			}
			return result;
		}

		return new LinkedHashSet<>();
	}

	/**
	 * Resolves a pattern name (possibly qualified, e.g. {@code "Form::Normal"})
	 * to the simple variant name within {@code type}, or {@code null} if it does
	 * not belong to that type.
	 */
	private String resolvePatternVariantSimpleName(String patternName, CompositeType type)
	{
		if (patternName.contains("::"))
		{
			int sep = patternName.lastIndexOf("::");
			String prefix = patternName.substring(0, sep);
			String simpleName = patternName.substring(sep + 2);
			// If the prefix doesn't match this type, it's for a different type
			if (!prefix.equals(type.name()))
				return null;
			return type.getMemberScope().resolveLocal(simpleName) != null ? simpleName : null;
		}
		// Unqualified: verify it's a member of this type
		return type.getMemberScope().resolveLocal(patternName) != null ? patternName : null;
	}

	/**
	 * Computes the set of tuple-combos covered by a {@link TuplePattern}.
	 * Each combo is represented as a comma-joined string of variant names.
	 *
	 * @return the covered set, or {@code null} if the tuple pattern contains a
	 *         wildcard that covers all combos.
	 */
	private Set<String> computeTupleCoverage(
			TuplePattern tp,
			List<List<String>> allDimensions,
			List<Type> elementTypes)
	{
		// For each position, determine the set of variants covered by the sub-pattern.
		List<List<String>> perDimCovered = new ArrayList<>();
		for (int i = 0; i < tp.elements.size() && i < allDimensions.size(); i++)
		{
			Pattern subPat = tp.elements.get(i);
			Type elemType = (i < elementTypes.size()) ? elementTypes.get(i) : null;
			CompositeType compositeElem = (elemType instanceof CompositeType ct) ? ct : null;

			if (isWildcardArm(subPat))
			{
				// Wildcard in this dimension: covers ALL variants for that dimension
				perDimCovered.add(new ArrayList<>(allDimensions.get(i)));
			}
			else if (compositeElem != null)
			{
				Set<String> dimCovered = collectSingleDimCovered(subPat, compositeElem, new LinkedHashSet<>(allDimensions.get(i)));
				if (dimCovered == null)
				{
					// Wildcard found inside an or-pattern chain — covers all
					perDimCovered.add(new ArrayList<>(allDimensions.get(i)));
				}
				else
				{
					perDimCovered.add(new ArrayList<>(dimCovered));
				}
			}
			else
			{
				// Can't determine coverage for this dimension
				perDimCovered.add(new ArrayList<>(allDimensions.get(i)));
			}
		}

		// Compute Cartesian product of the covered sets
		Set<String> result = new LinkedHashSet<>();
		buildCartesianProduct(perDimCovered, 0, new ArrayList<>(), result);
		return result;
	}

	/** Returns {@code true} if the pattern is unconditionally a wildcard. */
	private boolean isWildcardArm(Pattern pat)
	{
		return pat instanceof WildcardPattern;
	}

	/**
	 * Recursively builds the Cartesian product of {@code dimensions} and adds
	 * each combination (comma-joined variant names) to {@code result}.
	 */
	private void buildCartesianProduct(
			List<List<String>> dimensions,
			int dim,
			List<String> current,
			Set<String> result)
	{
		if (dim == dimensions.size())
		{
			result.add(String.join(", ", current));
			return;
		}
		for (String variant : dimensions.get(dim))
		{
			current.add(variant);
			buildCartesianProduct(dimensions, dim + 1, current, result);
			current.remove(current.size() - 1);
		}
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
		// Analyse each part so that expression parts get their types recorded.
		for (org.nebula.nebc.ast.expressions.Expression part : node.parts)
		{
			part.accept(this);
		}
		recordType(node, PrimitiveType.STR);
		return PrimitiveType.STR;
	}

	@Override
	public Type visitFormattedInterpolationExpression(org.nebula.nebc.ast.expressions.FormattedInterpolationExpression node)
	{
		// Analyse the inner expression so its type is recorded.
		node.expression.accept(this);
		recordType(node, PrimitiveType.STR);
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
		// Visit the literal expression so its type is recorded for codegen.
		node.value.accept(this);
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
		// Visit each sub-pattern so their literals are typed.
		for (Pattern sub : node.alternatives)
		{
			sub.accept(this);
		}
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
					CompositeType bound = null;
					if (gp.hasBound() && gp.bound() instanceof NamedType nt)
					{
						TypeSymbol boundSym = methodTypeParamScope.resolveType(nt.qualifiedName);
						if (boundSym != null && boundSym.getType() instanceof TraitType tt)
							bound = tt;
						else if (boundSym != null && boundSym.getType() instanceof TagType tag)
							bound = tag;
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
			// Prepend 'this' for trait methods too, so call sites calculate args correctly
			fnType.parameterTypes.add(0, traitType);

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
		// 1. Resolve the trait — use resolveTagMemberType so TraitType is not
		//    incorrectly blocked by the tag-as-value-type guard.
		Type traitResolved = resolveTagMemberType(node.traitType);
		if (!(traitResolved instanceof TraitType traitType))
		{
			if (traitResolved != Type.ERROR)
				error(DiagnosticCode.TYPE_MISMATCH, node, "Expected a trait name, got '" + traitResolved.name() + "'");
			return null;
		}

		// 2. Process the target type — allow TagType here (we expand it below)
		Type targetType = resolveTagMemberType(node.targetType);
		if (targetType == Type.ERROR)
			return null;

		// 3a. If the target is a TagType, expand to concrete per-member impls.
		//     This desugars  `impl Stringable for Signed`  into individual impls
		//     for each concrete type in the tag, validating for overlap as we go.
		if (targetType instanceof TagType tagTarget)
		{
			return visitImplDeclarationForTag(node, traitType, tagTarget);
		}

		// 3b. Normal single-type impl path (unchanged)
		return visitImplDeclarationForType(node, traitType, targetType);
	}

	/**
	 * Expands {@code impl Trait for TagName} into one concrete impl per member
	 * type of the tag and validates that no member already has an impl for this
	 * trait (overlap rule).
	 */
	private Type visitImplDeclarationForTag(
			ImplDeclaration node, TraitType traitType, TagType tagTarget)
	{
		for (Type member : tagTarget.getMemberTypes())
		{
			if (member instanceof TagType)
				// Nested tags were already flattened during declareTagBodies; skip
				continue;
			if (member instanceof TraitType)
				// A trait-constraint member (e.g. tag { str, Stringable }) cannot
				// itself receive a concrete impl.
				continue;

			// Overlap check: only fire when a *different* impl block has already
			// fully resolved (member, trait).  Pre-registered stubs from
			// preRegisterImplSignaturesForType do NOT count — resolvedImplPairs is
			// only populated by visitImplDeclarationForType after full resolution.
			String implKey = member.name() + "::" + traitType.name();
			if (resolvedImplPairs.contains(implKey))
			{
				error(DiagnosticCode.TAG_IMPL_OVERLAP, node, traitType.name(), member.name());
				continue;
			}

			visitImplDeclarationForType(node, traitType, member);
		}
		return null;
	}

	// ── Structural Stringable helpers ────────────────────────────────────────────

	/**
	 * Returns {@code true} when {@code type} can produce a string representation
	 * without an explicit {@code impl Stringable} declaration.
	 * <ul>
	 *   <li>Any primitive type that already has an impl scope is directly stringable.</li>
	 *   <li>A {@link TupleType} is stringable when all its element types are.</li>
	 *   <li>An {@link ArrayType} is stringable when its base element type is.</li>
	 * </ul>
	 */
	private boolean isStructurallyStringable(Type type)
	{
		if (primitiveImplScopes.containsKey(type))
			return true;
		if (type instanceof TupleType tt)
			return tt.elementTypes.stream().allMatch(this::isStructurallyStringable);
		if (type instanceof ArrayType at)
			return isStructurallyStringable(at.baseType);
		return false;
	}

	/**
	 * Produces an identifier suitable for use as the owner name of a synthetic
	 * impl scope. The result contains only ASCII letters, digits, and underscores.
	 */
	private static String toStructuralSafeName(Type type)
	{
		if (type instanceof TupleType tt)
		{
			StringBuilder sb = new StringBuilder("tuple");
			for (Type elem : tt.elementTypes)
			{
				sb.append('_').append(elem.name().replaceAll("[^a-zA-Z0-9]", "_"));
			}
			return sb.toString();
		}
		if (type instanceof ArrayType at)
		{
			String base = at.baseType.name().replaceAll("[^a-zA-Z0-9]", "_");
			return "array_" + base + (at.elementCount > 0 ? "_" + at.elementCount : "");
		}
		return type.name().replaceAll("[^a-zA-Z0-9]", "_");
	}

	/**
	 * Returns (creating on first call) a synthetic {@link SymbolTable} that
	 * exposes a {@code toStr()} {@link MethodSymbol} for a structurally-stringable
	 * compound type such as a {@link TupleType} or {@link ArrayType}.
	 * <p>
	 * The scope is stored in {@link #primitiveImplScopes} so that subsequent
	 * look-ups hit the cache and the same {@code MethodSymbol} instance is reused.
	 *
	 * @return the synthetic scope, or {@code null} when the type is not
	 *         structurally stringable.
	 */
	private SymbolTable getOrCreateStructuralStringableScope(Type type)
	{
		if (!isStructurallyStringable(type))
			return null;

		return primitiveImplScopes.computeIfAbsent(type, t ->
		{
			String safeName = toStructuralSafeName(t);
			TypeSymbol owner = new TypeSymbol(safeName, t, null);
			SymbolTable scope = new SymbolTable(globalScope);
			scope.setOwner(owner);

			// toStr : (T) -> str  — first (and only) parameter is the receiver itself
			FunctionType toStrFnType = new FunctionType(PrimitiveType.STR, List.of(t));
			MethodSymbol toStrMethod = new MethodSymbol(
					"toStr", toStrFnType, List.of(), false, null, List.of());
			toStrMethod.setTraitName("Stringable");
			toStrMethod.setSyntheticStructural(true);
			scope.define(toStrMethod);
			return scope;
		});
	}

	/** Resolves the impl scope for a given concrete type (primitive or composite). */
	private SymbolTable resolveImplScopeForType(Type type)
	{
		if (type instanceof PrimitiveType pt)
			return primitiveImplScopes.get(pt);
		if (type instanceof CompositeType ct)
			return ct.getMemberScope();
		return null;
	}

	/**
	 * Single-type impl body: enters the target scope, defines and validates
	 * method bodies, and checks that all required trait methods are satisfied.
	 */
	private Type visitImplDeclarationForType(
			ImplDeclaration node, TraitType traitType, Type targetType)
	{
		// Resolve or create the member scope for this type
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
				SymbolTable st = new SymbolTable(currentScope);
				// Set the owner so MethodSymbol.getMangledName() prefixes methods with 'i32_', etc.
				st.setOwner(new TypeSymbol(pt.name(), pt, null));
				return st;
			});
			// When multiple files add impl blocks for the same primitive (e.g. str),
			// the cached scope's parent may point to a different file's namespace.
			// Add the current scope as a super-scope so that symbols declared in
			// THIS file's namespace (such as private extern "C" FFI functions) are
			// reachable from the impl method bodies.
			targetScope.addSuperScope(currentScope);
		}
		else
		{
			error(DiagnosticCode.TYPE_MISMATCH, node.targetType,
					"trait implementor", targetType.name() + " (cannot implement trait for this type)");
			return null;
		}

		// Enter the target scope and define methods
		SymbolTable outerScope = currentScope;
		currentScope = targetScope;
		currentTypeDefinition = targetType;

		try
		{
			// Add a 'this' symbol for method bodies
			targetScope.define(new VariableSymbol("this", targetType, false, node));

			// Register method signatures — skip any already pre-registered by Phase 1.97
			// to avoid duplicate-symbol errors when the same impl is visited twice.
			for (MethodDeclaration method : node.members)
			{
				if (targetScope.resolveLocal(method.name) == null)
				{
					defineMethodSignature(method);
				}
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
			else
			{
				// Mark this (type, trait) pair as fully resolved so that the overlap
				// check in visitImplDeclarationForTag can detect genuine duplicates.
				resolvedImplPairs.add(targetType.name() + "::" + traitType.name());
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
		Symbol sym = getSymbol(node, Symbol.class);
		if (sym == null)
			return null;

		MethodSymbol methodSym = (MethodSymbol) sym;
		FunctionType fnType = methodSym.getType();

		SymbolTable prevScope = currentScope;
		Type prevMethodReturn = currentMethodReturnType;

		currentScope = new SymbolTable(prevScope);
		currentMethodReturnType = fnType.returnType;

		try
		{
			// Skip 'this' (index 0), define explicit params starting at index 1
			int paramIdx = 1;
			for (Parameter p : node.parameters)
			{
				Type pType = fnType.parameterTypes.get(paramIdx++);
				currentScope.define(new VariableSymbol(p.name(), pType, false, null));
			}

			if (node.body != null)
				node.body.accept(this);
		}
		finally
		{
			currentScope = prevScope;
			currentMethodReturnType = prevMethodReturn;
		}
		return null;
	}

	/**
	 * Registers an operator declaration as a {@link MethodSymbol} in the current (member) scope.
	 * The symbol name is {@code "operator" + token} (e.g. {@code "operator+"}).
	 * Return type is inferred: {@code bool} for comparison operators, receiver type otherwise.
	 */
	private void defineOperatorSignature(OperatorDeclaration od)
	{
		Type receiverType = currentTypeDefinition;
		List<Type> paramTypes = new ArrayList<>();
		paramTypes.add(receiverType); // 'this'

		for (Parameter p : od.parameters)
		{
			Type pType = (p.type() != null) ? resolveType(p.type()) : Type.ANY;
			paramTypes.add(pType != null ? pType : Type.ERROR);
		}

		// Infer return type: bool for equality/relational, receiver type for arithmetic/bitwise
		Type returnType = switch (od.operatorToken)
		{
			case "==", "!=", "<", ">", "<=", ">=" -> PrimitiveType.BOOL;
			default                               -> receiverType;
		};

		FunctionType fnType = new FunctionType(returnType, paramTypes);
		String symbolName = "operator" + od.operatorToken;
		MethodSymbol sym = new MethodSymbol(symbolName, fnType, java.util.Collections.emptyList(), false, od, java.util.Collections.emptyList());
		currentScope.forceDefine(sym);
		recordSymbol(od, sym);
	}

	@Override
	public Type visitConstructorDeclaration(ConstructorDeclaration node)
	{
		Symbol sym = getSymbol(node, Symbol.class);
		if (sym == null)
			return null;

		SymbolTable prevScope = currentScope;
		Type prevMethodReturn = currentMethodReturnType;

		// Methods get their own scope for parameters and locals
		currentScope = new SymbolTable(prevScope);
		currentMethodReturnType = PrimitiveType.VOID;

		try
		{
			MethodSymbol methodSym = (MethodSymbol) sym;
			FunctionType fnType = methodSym.getType();

			// Define parameters inside the new scope
			int fnParamIdx = 1; // skip 'this'
			for (int i = 0; i < node.parameters.size(); i++)
			{
				Parameter p = node.parameters.get(i);
				Type paramType = fnType.parameterTypes.get(fnParamIdx++);
				VariableSymbol paramSym = new VariableSymbol(p.name(), paramType, false, null);
				currentScope.define(paramSym);
			}

			if (node.body != null)
			{
				node.body.accept(this);
			}
		}
		finally
		{
			currentScope = prevScope;
			currentMethodReturnType = prevMethodReturn;
		}

		return null;
	}

	private void defineConstructorSignature(ConstructorDeclaration node)
	{
		List<Type> paramTypes = new ArrayList<>();
		List<ParameterInfo> paramInfos = new ArrayList<>();

		paramTypes.add(PrimitiveType.REF); // 'this' parameter
		paramInfos.add(new ParameterInfo(null, PrimitiveType.REF, "this"));

		for (Parameter p : node.parameters)
		{
			Type t = resolveType(p.type());
			paramTypes.add(t == Type.ERROR ? Type.ANY : t);
			paramInfos.add(new ParameterInfo(p.cvtModifier(), t, p.name()));
		}

		FunctionType fnType = new FunctionType(PrimitiveType.VOID, paramTypes, paramInfos);
		// Constructors are technically methods attached to the class name
		MethodSymbol ms = new MethodSymbol(node.name, fnType, java.util.Collections.emptyList(), false, node, java.util.Collections.emptyList());

		if (currentScope.resolveLocal(node.name) != null)
		{
			error(DiagnosticCode.DUPLICATE_SYMBOL, node, node.name);
			return;
		}

		currentScope.define(ms);
		recordSymbol(node, ms);
	}

	@Override
	public Type visitTagStatement(TagStatement node)
	{
		return null;
	}

	@Override
	public Type visitUseStatement(UseStatement node)
	{
		// ----------------------------------------------------------------
		// Glob import: use foo::*
		// ----------------------------------------------------------------
		if (node.isGlob)
		{
			importGlobFrom(node, node.qualifiedName);
			return null;
		}

		// ----------------------------------------------------------------
		// Multi-item import: use foo::{a, b as c}
		// ----------------------------------------------------------------
		if (node.items != null)
		{
			for (UseStatement.UseItem item : node.items)
			{
				if (item.isGlob())
				{
					importGlobFrom(node, node.qualifiedName);
				}
				else
				{
					importSingleItem(node, node.qualifiedName, item.name(), item.alias());
				}
			}
			return null;
		}

		// ----------------------------------------------------------------
		// Simple namespace import (legacy): use foo;  or  use foo as bar;
		// ----------------------------------------------------------------
		Symbol sym = currentScope.resolve(node.qualifiedName);
		if (sym == null)
			sym = globalScope.resolve(node.qualifiedName);

		if (sym instanceof NamespaceSymbol ns)
		{
			if (node.alias != null)
			{
				// Alias: register the namespace under the alias name
				currentScope.importSymbol(node.alias, ns);
			}
			else
			{
				currentScope.addImport(ns);
			}
		}
		else if (sym instanceof TypeSymbol ts)
		{
			// Direct type import (e.g. use MyEnum;): allow using variant names unqualified
			String localName = (node.alias != null) ? node.alias : ts.getName();
			currentScope.importSymbol(localName, ts);
		}
		else if (sym != null)
		{
			String localName = (node.alias != null) ? node.alias : sym.getName();
			currentScope.importSymbol(localName, sym);
		}
		else
		{
			error(DiagnosticCode.UNDEFINED_SYMBOL, node, node.qualifiedName);
		}
		return null;
	}

	/**
	 * Resolves the base path and imports all of its members into the current scope.
	 * Supports namespaces, enums, and unions as the base type.
	 */
	private void importGlobFrom(UseStatement node, String basePath)
	{
		SymbolTable memberScope = resolveMemberScope(node, basePath);
		if (memberScope != null)
			currentScope.importScope(memberScope);
	}

	/**
	 * Resolves {@code basePath} and imports a single named item from it into the
	 * current scope, optionally under {@code alias}.
	 */
	private void importSingleItem(UseStatement node, String basePath, String itemName, String alias)
	{
		SymbolTable memberScope = resolveMemberScope(node, basePath);
		if (memberScope == null)
			return;

		Symbol target = memberScope.resolveLocal(itemName);
		if (target == null)
			target = memberScope.resolve(itemName);

		if (target == null)
		{
			error(DiagnosticCode.USE_ITEM_NOT_FOUND, node, itemName, basePath);
			return;
		}

		String localName = (alias != null) ? alias : itemName;
		currentScope.importSymbol(localName, target);
	}

	/**
	 * Resolves {@code path} to a scope that can be glob-imported or item-imported.
	 * Accepts namespaces, enums, unions, and classes/structs.
	 * Returns {@code null} and emits an error if the path cannot be resolved.
	 */
	private SymbolTable resolveMemberScope(UseStatement node, String path)
	{
		Symbol sym = currentScope.resolve(path);
		if (sym == null)
			sym = globalScope.resolve(path);

		if (sym instanceof NamespaceSymbol ns)
			return ns.getMemberTable();

		if (sym instanceof TypeSymbol ts && ts.getType() instanceof CompositeType ct)
			return ct.getMemberScope();

		if (sym == null)
			error(DiagnosticCode.UNDEFINED_SYMBOL, node, path);
		else
			error(DiagnosticCode.UNDEFINED_SYMBOL, node, path + " (not a namespace or type)");

		return null;
	}

	@Override
	public Type visitConstDeclaration(ConstDeclaration node)
	{
		return node.declaration.accept(this);
	}

	// =========================================================================
	// Optional type expressions
	// =========================================================================

	@Override
	public Type visitNoneExpression(NoneExpression node)
	{
		// 'none' is the absent value; its type is an optional wrapping any T.
		// The concrete T? is resolved by the assignment context during inference.
		// Return a sentinel OptionalType(ANY) so callers know it's optional-compatible.
		Type result = new OptionalType(Type.ANY);
		recordType(node, result);
		return result;
	}

	@Override
	public Type visitForcedUnwrapExpression(ForcedUnwrapExpression node)
	{
		Type operandType = node.operand.accept(this);
		if (operandType instanceof OptionalType ot)
		{
			// Strip optional wrapper — caller gets the inner T
			Type result = ot.innerType;
			recordType(node, result);
			return result;
		}
		// Forced-unwrap on a non-optional is a type error
		error(DiagnosticCode.FORCED_UNWRAP_ON_NON_OPTIONAL, node, operandType != null ? operandType.name() : "unknown");
		return Type.ERROR;
	}

	@Override
	public Type visitNullCoalescingExpression(NullCoalescingExpression node)
	{
		Type leftType  = node.left.accept(this);
		Type rightType = node.right.accept(this);

		if (leftType instanceof OptionalType ot)
		{
			// left ?? right  =>  type is T when right : T (or T?)
			if (rightType instanceof OptionalType rt)
			{
				// both optional: result is T?
				Type result = new OptionalType(ot.innerType);
				recordType(node, result);
				return result;
			}
			// right is T: result is T
			recordType(node, ot.innerType);
			return ot.innerType;
		}

		// left side is not optional — coalescing is a no-op, still valid
		recordType(node, leftType);
		return leftType;
	}

	@Override
	public Type visitDestructuringPattern(DestructuringPattern node)
	{
		// Binding declarations are handled by visitMatchExpression's arm loop,
		// which pushes a child scope and defines each binding variable.
		// At this call-site there is nothing type-level to check; return null.
		return null;
	}

	@Override
	public Type visitTuplePattern(TuplePattern node)
	{
		for (Pattern sub : node.elements)
			sub.accept(this);
		return null;
	}

	// =========================================================================
	// Break / Continue
	// =========================================================================

	@Override
	public Type visitBreakStatement(BreakStatement node)
	{
		if (!insideLoop)
		{
			error(DiagnosticCode.INVALID_BREAK_OUTSIDE_LOOP, node, "break");
		}
		return null;
	}

	@Override
	public Type visitContinueStatement(ContinueStatement node)
	{
		if (!insideLoop)
		{
			error(DiagnosticCode.INVALID_BREAK_OUTSIDE_LOOP, node, "continue");
		}
		return null;
	}

	// =========================================================================
	// Attributes
	// =========================================================================

	/**
	 * Converts a list of {@link org.nebula.nebc.ast.declarations.AttributeNode}s
	 * from the AST into the simplified {@link Symbol.AttributeInfo} representation
	 * stored on symbols.
	 *
	 * <p>For simple literal arguments the raw value is extracted (e.g. a string
	 * literal {@code "foo"} becomes {@code "foo"}, not the ASTPrinter debug text).
	 * Complex expression arguments fall back to the ASTPrinter serialisation so
	 * that no information is lost.</p>
	 */
	private List<Symbol.AttributeInfo> collectAttributeInfos(
		List<org.nebula.nebc.ast.declarations.AttributeNode> nodes)
	{
		if (nodes == null || nodes.isEmpty())
			return java.util.Collections.emptyList();

		List<Symbol.AttributeInfo> result = new ArrayList<>();
		for (var attr : nodes)
		{
			List<String> argTexts = new ArrayList<>();
			for (var arg : attr.args)
			{
				argTexts.add(extractArgText(arg));
			}
			result.add(new Symbol.AttributeInfo(attr.path, argTexts));
		}
		return result;
	}

	/**
	 * Extracts a clean string representation of an attribute argument expression.
	 * Simple literals are returned as their raw value; complex expressions fall
	 * back to the ASTPrinter serialisation.
	 */
	private String extractArgText(org.nebula.nebc.ast.ASTNode arg)
	{
		if (arg instanceof org.nebula.nebc.ast.expressions.LiteralExpression le)
		{
			return le.value.toString();
		}
		return org.nebula.nebc.ast.util.ASTPrinter.print(arg).trim();
	}
}