package nl.liacs.subdisc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.liacs.subdisc.Attribute.AttributeType;

public class FileLoaderARFF implements FileLoaderInterface
{
	private Table itsTable = null;
	private int itsNrDataRows = 0;
	private ArrayList<NominalAttribute> itsNominalAttributes = new ArrayList<NominalAttribute>();	// used to check data declarations
	public static  ArrayList<String> BOOLEAN_NEGATIVES = new ArrayList<String>(Arrays.asList(new String[] { "0", "false", "F", "no" }));
	public static  ArrayList<String> BOOLEAN_POSITIVES = new ArrayList<String>(Arrays.asList(new String[] { "1", "true", "T", "yes" }));

	private static enum Keyword
	{
		COMMENT("%"),
		RELATION("@relation"),
		ATTRIBUTE("@attribute"),
		DATA("@data"),
		END("@end");

		private final Pattern text;

		Keyword(String theKeyword) { text = Pattern.compile("^\\s*" + theKeyword + "\\s*", Pattern.CASE_INSENSITIVE); }

		boolean occursIn(String theString) { return this.text.matcher(theString).find(); }
	}

	private class NominalAttribute
	{
		final Attribute itsAttribute;
		final List<String> itsNominalClasses;

		NominalAttribute(Attribute theAttribute, List<String> theNominalClasses)
		{
			itsAttribute = theAttribute;
			itsNominalClasses = theNominalClasses;
		}
	}

	public FileLoaderARFF(File theFile)
	{
		if(theFile != null && theFile.exists())
			loadFile(theFile);
		else
			;	// new ErrorDialog(e, ErrorDialog.noSuchFileError);
	}

	// TODO multiple '@relation' and '@data' declarations should throw error
	// TODO rewrite parser, use keyword check on each line
	private void loadFile(File theFile)
	{
		BufferedReader aReader = null;

		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aLine;
			boolean relationFound = false;
			boolean attributeFound = false;
			boolean dataFound = false;

			// .toLowerCase()
			while((aLine = aReader.readLine()) != null)
			{
				aLine.trim();
				if(Keyword.COMMENT.occursIn(aLine) || aLine.isEmpty())
					continue;

				if(Keyword.END.occursIn(aLine))
					break;

				if(!relationFound)
				{
					if(Keyword.RELATION.occursIn(aLine))
					{
						itsTable = new Table();
						itsTable.itsName = removeOuterQuotes(aLine.split("\\s", 2)[1]);	// TODO use m.end()
						relationFound = true;
					}
					else
					{
						// if(otherKeyWordFound) { criticalError(noRelationError); }
						continue;
					}
				}

				else if(!attributeFound)
				{
					if(Keyword.ATTRIBUTE.occursIn(aLine))
					{
						Matcher m;
						int aCount = 0;

						do
						{
							if(Keyword.COMMENT.occursIn(aLine))
								continue;

							m = Keyword.ATTRIBUTE.text.matcher(aLine);
							if(m.find())
							{
								itsTable.getColumns().add(new Column(parseAttribute(aLine.substring(m.end()), aCount), 1000));	// TODO other default? 1000
								aCount++;
							}
							else if(Keyword.DATA.occursIn(aLine))
							{
								dataFound = true;
								break;
							}
						}
						while((aLine = aReader.readLine()) != null);

						attributeFound = true;
					}
					else
					{
						// if(dataKeyWordFound) { criticalError(noAttributeError); }
						continue;
					}
				}

				else if(dataFound)
				{
					loadData(aLine);
					++itsNrDataRows;
				}
			}
		}
		catch (IOException e)
		{
			// criticalError(e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (aReader != null)
					aReader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		itsTable.update();
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
	// TODO we can not handle STRING/DATE appropriately
	// attribute type(s), only NUMERIC/NOMINAL for now, not ORDINAL/BINARY
	private Attribute parseAttribute(String theLine, int theIndex)
	{
		String aName;

		// get attribute name (not so clean)
		if(theLine.startsWith("\'"))
			aName = removeOuterQuotes(theLine);
		else
			aName = theLine.split("\\s", 2)[0];

		theLine = theLine.replaceFirst("\\'?" + aName + "\\'?", "").trim();

		return new Attribute(theIndex, aName, null, declaredType(aName, theLine));	// (aName, theLine) HACK for NominalAttribute
	}

	// TODO do this only once for the whole line
	// TODO catch ArrayIndexOutOfBoounds if i > length (missing closing ')
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

	// TODO checking of declared nominal classes for @attributes { class1, class2, ..} declarations
	private String loadData(String theString)
	{
		String s;

		for(int i = 0, j = itsTable.getColumns().size(); i < j; ++i)
		{
			Column aColumn = itsTable.getColumn(i);
			int offset = 0;

			if(theString.trim().startsWith("\'"))
			{
				s = removeOuterQuotes(theString);
				offset = 2;
			}
			else
			{
				s = theString.split(",\\s*", 2)[0];
			}

			theString = theString.substring(s.length() + offset).replaceFirst(",\\s*", "");

			if(s.equalsIgnoreCase("?"))
				aColumn.setMissing(itsNrDataRows);

			//System.out.println(s + " " + itsTable.getAttribute(i).getName() + " " + itsTable.getColumn(itsTable.getAttribute(i)).size());
			// TODO determine default for missing NUMERIC/ORDINAL/BINARY values
			// itsTable.getColumn(itsTable.getAttribute(i)).add(s); break;
			switch(aColumn.getType())
			{
				case NUMERIC :
				{
					if(s.equalsIgnoreCase("?"))
						aColumn.add(0f);
					else
						aColumn.add(Float.valueOf(s));
					break;
				}
				case NOMINAL :
					aColumn.add(s); break;
				case BINARY :
				{
					if(BOOLEAN_POSITIVES.contains(s.toLowerCase()))
						aColumn.add(true);
					break;
				}
			}
		}

		if(!theString.isEmpty())
			if(!Keyword.COMMENT.occursIn(theString))
				System.out.println("ERROR: " + theString);
				// criticalError(toManyArgumentsError);

		return theString;
	}

	// determine attribute type(s), only NUMERIC/NOMINAL for now, not ORDINAL/BINARY
	private AttributeType declaredType(String theAttributeName, String theString)
	{
		String s = theString.toLowerCase();
//		System.out.println("declaredType: " + s);

		if(s.startsWith("real") || s.startsWith("integer") || s.startsWith("numeric"))
			return AttributeType.NUMERIC;

		else if(s.startsWith("{"))
		{
			theString = theString.substring(1);
			ArrayList<String> nominalClasses= new ArrayList<String>(10);
			String aNominalClass;

			// duplicate code
			while(!theString.startsWith("}"))
			{
//				System.out.println("String: " + theString);
				int offset = 0;

				if(theString.startsWith("\'"))
				{
					aNominalClass = removeOuterQuotes(theString);
					offset = 3;
				}
				else
				{
					if(theString.contains(","))
					{
						aNominalClass = theString.split(",\\s*", 2)[0];
						offset = 1;
					}
					else
					{
						aNominalClass = theString.split("}", 2)[0];
						nominalClasses.add(aNominalClass);
						break;
					}
				}

				nominalClasses.add(aNominalClass);
				theString = theString.substring(aNominalClass.length() + offset).replaceFirst(",\\s*", "");
			}

			itsNominalAttributes.add(new NominalAttribute(itsTable.getAttribute(theAttributeName), nominalClasses));

			// TODO use enum
			if(nominalClasses.size() == 2)
			{
				String a = nominalClasses.get(0).toLowerCase();
				String b = nominalClasses.get(1).toLowerCase();

				if((BOOLEAN_NEGATIVES.contains(a) && BOOLEAN_POSITIVES.contains(b)) ||
					(BOOLEAN_NEGATIVES.contains(b) && BOOLEAN_POSITIVES.contains(a)))
					return AttributeType.BINARY;
			}
			// TODO present attributeType change dialog to user
			return AttributeType.NOMINAL;
		}

		// TODO parseDate using dateFormat
		else if(s.startsWith("date"))
			return AttributeType.NOMINAL;

		return AttributeType.NOMINAL;
	}

	// TODO create ErrorDialog class, that also logs the error
	private static void criticalError()
	{
		Log.logCommandLine("ERROR");
	}

	public Table getTable()
	{
		return itsTable;
	}
}
