package org.nebula.nebc.io;

import java.io.File;

/**
 * Represents an input file to the compiler.
 * Stores the path, file name, and detected {@link FileType}.
 */
public class SourceFile
{
	private final String path;
	private final String fileName;
	private final FileType type;
	private final File file;

	public SourceFile(String path)
	{
		this.path = path;
		this.file = new File(path);
		this.fileName = extractFileName(path);
		this.type = getFileTypeFromPath(path);
	}

	public String path()
	{
		return path;
	}

	public String fileName()
	{
		return fileName;
	}

	public FileType type()
	{
		return type;
	}

	public File file()
	{
		return file;
	}

	private static String extractFileName(String fullPath)
	{
		if (fullPath == null)
		{
			return null;
		}

		int unix = fullPath.lastIndexOf('/');
		int win = fullPath.lastIndexOf('\\');
		int idx = Math.max(unix, win);

		return (idx < 0) ? fullPath : fullPath.substring(idx + 1);
	}

	/**
	 * Determines the file type based on its extension.
	 */
	public static FileType getFileTypeFromPath(String path)
	{
		int dot = path.lastIndexOf('.');
		if (dot <= 0 || dot == path.length() - 1)
		{
			return FileType.ERROR;
		}

		String extension = path.substring(dot + 1);
		return FileType.getByExtension(extension);
	}

	@Override
	public String toString()
	{
		return fileName + " (" + type + ")";
	}
}