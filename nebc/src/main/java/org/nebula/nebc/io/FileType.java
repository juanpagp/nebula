package org.nebula.nebc.io;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the defined file types within the Nebula Compiler (NEBC) system.
 * <p>Each constant is associated with a specific file extension, allowing for
 * fast and type-safe determination of a file's purpose based on its path.</p>
 */
public enum FileType
{
	ERROR(""), NEBULA_SOURCE("neb"), NEBULA_SYMBOL_TABLE("nebsym"), NEBULA_LIBRARY("neblib"), NEBULA_PROJECT("nebproj"), NATIVE_SOURCE("cpp"), NATIVE_HEADER("h");

	private final String extension;

	/**
	 * A static map used for efficient O(1) lookup of a FileType constant
	 * based on its file extension string.
	 */
	private static final Map<String, FileType> EXTENSION_MAP = new HashMap<>();

	static
	{
		for (FileType type : FileType.values())
		{
			if (!type.extension.isEmpty())
			{
				EXTENSION_MAP.put(type.extension, type);
			}
		}
	}

	/**
	 * Constructs a new FileType constant with the specified file extension.
	 *
	 * @param extension The file extension associated with this type (e.g., "neb").
	 */
	private FileType(String extension)
	{
		this.extension = extension;
	}

	/**
	 * Retrieves a {@code FileType} constant based on the file extension string.
	 * <p>This method performs a constant-time lookup using a static internal map.</p>
	 *
	 * @param extension The file extension string to look up (e.g., "cpp").
	 * @return The matching {@code FileType} constant, or {@link #ERROR} if the
	 * extension is not recognized.
	 */
	public static FileType getByExtension(String extension)
	{
		return EXTENSION_MAP.getOrDefault(extension, ERROR);
	}

	/**
	 * Returns the file extension string associated with this FileType constant.
	 *
	 * @return The extension string.
	 */
	public String getExtension()
	{
		return extension;
	}
}