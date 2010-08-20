/**
 * @author marvin
 * This will change and use the FileNameExtensionFilter class.
 * Use only lowercase extensions, filenames will always be set to lowercase.
 */
package nl.liacs.subdisc;

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
			return new ArrayList<String>(Arrays.asList(new String[] {".txt", ".text", ".csv" }));
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
	ALL_DATA("Data Files")
	{
		@Override
		public List<String> getExtensions()
		{
			List<String> returnList = new ArrayList<String>(TXT.getExtensions());
			returnList.addAll(ARFF.getExtensions());
			returnList.addAll(XML.getExtensions());
			return returnList;
		}
	};

	public final String DESCRIPTION;
	private FileType(String theDescription) { DESCRIPTION = theDescription; }
	abstract List<String> getExtensions();
}