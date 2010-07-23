package nl.liacs.subdisc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileLoaderARFF implements FileLoaderInterface
{
	private Table itsTable;

//	private static final String[] keyword = { "%", "@relation", "@attribute", "@data", "@end" };
	private final static Pattern COMMENT = Pattern.compile("^\\s*%\\s*", Pattern.CASE_INSENSITIVE);
	private final static Pattern RELATION = Pattern.compile("^\\s*@relation\\s*", Pattern.CASE_INSENSITIVE);
	private final static Pattern ATTRIBUTE = Pattern.compile("^\\s*@attribute\\s*", Pattern.CASE_INSENSITIVE);
	private final static Pattern DATA = Pattern.compile("^\\s*@data\\s*", Pattern.CASE_INSENSITIVE);
	private final static Pattern END = Pattern.compile("^\\s*@end\\s*", Pattern.CASE_INSENSITIVE);
	private static Matcher m;
/*	
	private static enum Keyword
	{
		COMMENT("%"),
		RELATION("@relation"),
		ATTRIBUTE("@attribute"),
		DATA("@data"),
		END("@end");

		private final String inTxt;

		Keyword(String theTxt) { inTxt = theTxt; }

		@Override
		public String toString() { return inTxt; }
	}
*/
	// TODO multiple '@relation' and '@data' declarations should throw error
	// TODO rewrite parser, use keyword check on each line
	@Override
	public Table loadFile(File theFile)
	{
		BufferedReader aReader = null;

		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aLine;
//			EnumSet<Keyword> section = EnumSet.range(Keyword.RELATION, Keyword.DATA);
//			String sectionToFind = section.iterator().next().toString();	// @relation
			boolean relationFound = false;
			boolean attributeFound = false;
			boolean dataFound = false;

			// .toLowerCase()
			while((aLine = aReader.readLine()) != null)
			{
//				aLine.trim();
				if(COMMENT.matcher(aLine).find())
					continue;

				if(END.matcher(aLine).find())
					break;

				if(!relationFound)
				{
					if(RELATION.matcher(aLine).find())
					{
						itsTable = new Table();
						itsTable.itsName = removeOuterQuotes(aLine.split("\\s", 2)[1]);
						relationFound = true;
					}
					else
					{
						// if(otherKeyWordFound) { noRelationError(); }
						continue;
					}
				}

				else if(!attributeFound)
				{
					m = ATTRIBUTE.matcher(aLine);
					if(m.find())
					{
						ArrayList<Attribute> al = new ArrayList<Attribute>();

						do
						{
							if(COMMENT.matcher(aLine).find())
								continue;

							m = ATTRIBUTE.matcher(aLine);
							if(m.find())
								al.add(parseAttribute(aLine.substring(m.end())));
							else if(DATA.matcher(aLine).find())
							{
								dataFound = true;
								break; 
							}
						}
						while((aLine = aReader.readLine()) != null);

						itsTable.setAttributes(al);
						attributeFound = true;
					}
					else
					{
						// if(dataKeyWordFound) { noAttributeError(); }
						continue;
					}
				}

				else if(dataFound)
				{
					// careful splitting using .split(",", -1), check for escaped "\," in quoted strings 'some\,text'
					// aLine.length == itsTable.getNrAttributes()
					//loadData(aLine);
				}
			}
			/*
			 * TEST
			 * @relation 'labor-neg-data'
			 * @attribute 'duration' real
			 * @attribute 'wage-increase-first-year' real
			 */
			System.out.println("itsTable.itsName = " + itsTable.itsName);
			for(Attribute a : itsTable.getAttributes())
				a.print();
			System.out.println("dataFound = " + dataFound);
			
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return itsTable;
	}
/*
	private static boolean prematureEOF(File theFile, String theLine, String theSectionToFind)
	{
		if (theLine == null || theLine.equalsIgnoreCase(Keyword.END.toString()))
		{
			Log.logCommandLine("Error while parsing: " + theFile + ", " + theSectionToFind + " declaration missing.");
			return true;
		}
		return false;
	}
*/
	/*
	 * @attribute <name> numeric/real/integer - Numeric attributes can be real or integer numbers. 
	 * @attribute <name> {<nominal-name1>, <nominal-name2>, <nominal-name3>, ...} - Nominal values are defined by providing a <nominal-specification> listing the possible values.
	 * @attribute <name> string - String attributes allow us to create attributes containing arbitrary textual values.
	 * @attribute <name> date [<date-format>] - where <name> is the name for the attribute and <date-format> is an optional string specifying how date values should be parsed
	 * 
	 * strip "\\s*@attribute\\s*"
	 * if firstChar == ' find next unescaped "\'", this is the attributeName (minus '')
	 * else find next "\\s"
	 * if type == numeric/real/integer/string/date - done (for date check for date-format)
	 * else parse inner { }, not really needed for Attribute types, but useful for DATA checking
	 * each nominal-class is delimited by an unescaped "\\s*,\\s*"
	 */
	private static Attribute parseAttribute(String theLine)
	{
		Attribute anAttribute;
		String aName;

		// get attribute name (not so clean)
		if(theLine.startsWith("\'"))
			aName = removeOuterQuotes(theLine);
		else
			aName = theLine.split("\\s", 2)[0];

		theLine = theLine.replaceFirst("\\'?" + aName + "\\'?\\s*", "");

		// TODO we can not handle STRING/DATE appropriately
		// get attribute type(s), only NUMERIC/NOMINAL for now, not ORDINAL/BINARY
		if(declaresNumericType(theLine))
			anAttribute = new Attribute(aName, null, Attribute.NUMERIC);
		else
			anAttribute = new Attribute(aName, null, Attribute.NOMINAL);

		return anAttribute;
	}

	// TODO do this only once for the whole line
	private static String removeOuterQuotes(String theString)
	{
		if(theString.startsWith("\'"))
		{
			// find first unescaped "'"
			char[] aCharArray = theString.toCharArray();
			
			for(int i = 1, j = aCharArray.length; i < j; ++i)
			{
				if(aCharArray[i] == '\\')	// jump beyond escaped char
					++i;
				else if(aCharArray[i] == '\'')
				{
					return theString.substring(1, i);
				}
			}
		}
		return theString;
	}

	private static boolean declaresNumericType(String theString)
	{
		String s = theString.toLowerCase();
		return (s.startsWith("real") || s.startsWith("integer") || s.startsWith("numeric"));
	}
}
