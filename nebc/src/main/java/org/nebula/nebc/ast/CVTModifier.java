package org.nebula.nebc.ast;

/**
 * Causal Validity Tracking (CVT) Modifiers for FFI Parameters
 * <p>
 * These modifiers are used exclusively in `extern "C"` function declarations
 * to specify whether a parameter is borrowed (`keeps`) or consumed (`drops`)
 * by the foreign function.
 * <p>
 * - KEEPS:  The function reads the parameter but does not take ownership.
 * The caller remains responsible for cleanup.
 * Used for read-only references: `keeps Ref<u8> buf`
 * <p>
 * - DROPS:  The function takes ownership and is responsible for cleanup.
 * The caller must not use the parameter after the call.
 * Used for transferred references: `drops string s`
 * <p>
 * - NONE:   No CVT modifier (used in normal Nebula functions).
 */
public enum CVTModifier
{
	KEEPS("keeps"),
	DROPS("drops"),
	NONE("none");

	private final String keyword;

	CVTModifier(String keyword)
	{
		this.keyword = keyword;
	}

	/**
	 * Parse a CVT modifier from a string (e.g., "keeps", "drops")
	 *
	 * @param text The text to parse
	 * @return The corresponding CVTModifier, or NONE if not recognized
	 */
	public static CVTModifier fromString(String text)
	{
		if (text == null)
			return NONE;
		return switch (text.toLowerCase())
		{
			case "keeps" ->
					KEEPS;
			case "drops" ->
					DROPS;
			default ->
					NONE;
		};
	}

	/**
	 * Get the keyword string for this modifier (e.g., "keeps" or "drops")
	 */
	public String keyword()
	{
		return keyword;
	}

	/**
	 * Check if this modifier indicates FFI usage (KEEPS or DROPS)
	 *
	 * @return true if this is a CVT modifier for FFI (not NONE)
	 */
	public boolean isFFIModifier()
	{
		return this != NONE;
	}
}
