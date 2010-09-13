/**
 * @author marvin
 * This will change and use the FileNameExtensionFilter class.
 * Use only lowercase extensions, filenames will always be set to lowercase.
 */
package nl.liacs.subdisc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum FileType
{
	TXT("Text Files")
	{
		@Override
		public List<String> getExtensions()
		{
			return new ArrayList<String>(
						Arrays.asList(new String[] {".txt", ".text", ".csv" }));
		}
	},
	ARFF("ARFF Files")
	{
		@Override
		public List<String> getExtensions()
		{
			return new ArrayList<String>(Collections.singletonList(".arff"));
		}
	},
	XML("XML Files")
	{
		@Override
		public List<String> getExtensions()
		{
			return new ArrayList<String>(Collections.singletonList(".xml"));
		}
	},
	ALL_DATA_FILES("Data Files")
	{
		@Override
		public List<String> getExtensions()
		{
			List<String> returnList =
				new ArrayList<String>(TXT.getExtensions());
			returnList.addAll(ARFF.getExtensions());
			returnList.addAll(XML.getExtensions());
			return returnList;
		}
	};

	/**
	 * The description used in a <code>JFileChooser</code>.
	 */
	public final String DESCRIPTION;

	private FileType(String theDescription) { DESCRIPTION = theDescription; }

	abstract List<String> getExtensions();

	/**
	 * Returns a String for a File that is equal to File.getName(), but with the
	 * extension removed (that is everything starting from the last '.').
	 * 
	 * @param theFile the <code>File<code> from which to remove the extension
	 * 
	 * @return a String of the <code>File.getName()</code> for the parameter,
	 * with the extension removed, or the empty Sting if the parameter is
	 * <code>null</code>
	 */
	public static String removeExtension(File theFile)
	{
		if (theFile == null || !theFile.exists())
			return "";
		else
		{
			String aString = theFile.getName();
			return aString.substring(0, aString.lastIndexOf('.'));
		}
	}

	/**
	 * Return the FileType corresponding to the filename parameter. The filename
	 * will be checked for its extension, and if the extension is registered
	 * with a FileType, that FileType will be returned.
	 * 
	 * @param theFile the <code>File</code> to get the FileType for
	 * 
	 * @return the FileType for the parameter, if the parameters' extension is
	 * known, <code>null</code> otherwise
	 */
	public static FileType getFileType(File theFile)
	{
		if (theFile == null || !theFile.exists())
			return null;

		String aFileName = theFile.getName().toLowerCase();
		String anExtension = aFileName.substring(aFileName.lastIndexOf('.'));

		for (FileType aFileType : FileType.values())
			if (aFileType.getExtensions().contains(anExtension))
				return aFileType;

		// FileType not found, log warning, return 'null'
		Log.logCommandLine(
			String.format("FileType.getFileType(): The extension '%s' of File" +
							" '%s', can not be resolved to a known FileType.",
							anExtension,
							aFileName));
		return null;
	}
}
