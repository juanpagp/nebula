package org.nebula.nebc.ast.declarations;

import org.nebula.nebc.ast.*;
import org.nebula.nebc.frontend.diagnostic.SourceSpan;

import java.util.List;

/**
 * Represents an `extern "C" { ... }` block declaring foreign function interfaces.
 * 
 * Example:
 *   extern "C" {
 *       void __nebula_rt_write(keeps Ref<u8> buf, i32 len);
 *   }
 * 
 * Fields:
 * - language:  The ABI/language for the FFI (e.g., "C")
 * - members:   List of method declarations (function signatures without bodies)
 * - isPrivate: Whether the extern block is marked private (visibility modifier)
 */
public class ExternDeclaration extends Declaration
{
	public final String language;  // e.g., "C", "C++"
	public final List<MethodDeclaration> members;  // Extern function signatures
	public final boolean isPrivate;

	public ExternDeclaration(SourceSpan span, String language, List<MethodDeclaration> members, boolean isPrivate)
	{
		super(span);
		this.language = language;
		this.members = members;
		this.isPrivate = isPrivate;
	}

	@Override
	public <R> R accept(ASTVisitor<R> visitor)
	{
		return visitor.visitExternDeclaration(this);
	}
}
