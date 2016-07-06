/*
 * TODO rewrite this class, fileLoading is a mess. Split loadFile() into
 * loadFileAndCreateTable() and loadFileCheckWithXMLTable()
 */
package nl.liacs.subdisc;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class FileLoaderARFF implements FileLoaderInterface
{
	private Table itsTable = null;
	private boolean checkDataWithXMLTable = false; // if loaded from XML
	private int itsNrDataRows = 0;
	// used to check data declarations
	private List<NominalAttribute> itsNominalAttributes =
					new ArrayList<NominalAttribute>();
	private int itsNrBadRows = 0;

	private static float MISSING_NUMERIC =
		Float.parseFloat(AttributeType.NUMERIC.DEFAULT_MISSING_VALUE);
	private static enum Keyword
	{
		COMMENT("%"),
		RELATION("@relation"),
		ATTRIBUTE("@attribute"),
		DATA("@data"),
		END("@end");

		private final Pattern text;

		Keyword(String theKeyword)
		{
			text = Pattern.compile("^\\s*" + theKeyword + "\\s*",
						Pattern.CASE_INSENSITIVE);
		}

		// FIXME MM - returns true if KEYWORD occurs anywhere in String
		boolean atStartOfLine(String theString)
		{
			return text.matcher(theString).find();
		}
	}

	// This is not used atm., it will be in the future
	private class NominalAttribute
	{
/*
		final Attribute itsAttribute;
		final List<String> itsNominalClasses;

		NominalAttribute(Attribute theAttribute, List<String> theNominalClasses)
		{
			itsAttribute = theAttribute;
			itsNominalClasses = theNominalClasses;
		}
*/
	}

	public FileLoaderARFF(File theFile)
	{
		if (theFile == null || !theFile.exists())
		{
			// TODO new ErrorDialog(e, ErrorDialog.noSuchFileError);
			Log.logCommandLine(
				String.format("FileLoaderARFF: can not open File '%s'",
						theFile.getAbsolutePath()));
			return;
		}
		else
			loadFile(theFile);
	}

	public FileLoaderARFF(File theFile, Table theTable)
	{
		if (theFile == null || !theFile.exists())
		{
			// TODO new ErrorDialog(e, ErrorDialog.noSuchFileError);
			Log.logCommandLine(
				String.format("FileLoaderARFF: can not open File '%s'",
						theFile.getAbsolutePath()));
			return;
		}
		else if (theTable == null)
			// TODO warning, try normal loading
			loadFile(theFile);
		else
		{
			checkDataWithXMLTable = true;
			itsTable = theTable;
			loadFile(theFile);
		}
	}

	// TODO multiple '@relation' and '@data' declarations should throw error
	// TODO check for keywords after next keyword already appeared (malformed)
	private void loadFile(File theFile)
	{
		BufferedReader aReader = null;

		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aLine;
			boolean relationFound = false;
			boolean dataFound = false;
			int anAttributeIndex = 0;	// if > 0, same as attributeFound = true
			Matcher aMatcher;
			int aLineNr = 0;	// TODO use for error reporting

			// .toLowerCase()
			while ((aLine = aReader.readLine()) != null)
			{
				aLineNr++;

				if (Keyword.COMMENT.atStartOfLine(aLine) || aLine.isEmpty())
					continue;

				else if (Keyword.END.atStartOfLine(aLine))
				{
					String aMissing = "";

					if (!relationFound)
						aMissing += "\nNo '@relation' found.";
					if (anAttributeIndex == 0)
						aMissing += "\nNo '@attribute' found.";
					if (!dataFound)
						aMissing += "\nNo '@data' found.";

					if (!aMissing.isEmpty())
						Log.logCommandLine(
							"FileLoaderARFF: premature '@end' declaration in" +
							" File '" + theFile.getName() + "'." + aMissing);
					break;
				}

				else if (Keyword.RELATION.atStartOfLine(aLine))
				{
					if (!relationFound)
					{
						relationFound = true;
						// TODO if TableNames don't match that's OK for now
						// if Table was provided by 2-argument constructor
						if (checkDataWithXMLTable)
						{
							continue;
						}
						else
							itsTable = new Table(theFile, removeOuterQuotes(aLine.split("\\s", 2)[1]));
//							itsTable.setName();	// TODO use m.end()
//							itsTable.setSource(theFile.getName());
					}
					else
					{
						// if (otherKeyWordFound) { criticalError(noRelationError); }
						Log.logCommandLine(
							String.format(
								"FileLoaderARFF: multiple '@relation' declarations in File '%s', using: '%s'.",
								theFile.getName(),
								itsTable.getName()));
					}
				}

				else if (Keyword.ATTRIBUTE.atStartOfLine(aLine))
				{
					if (!relationFound)
					{
						// malformedFileWarning();
						// if Table was provided by constructor, try to continue
						if (checkDataWithXMLTable)
							continue;
						// no table created yet, abort
						else
							break;
					}
					else if (dataFound)
					{
						// TODO malformedFileWarning(), try to continue
						// malformedFileWarning();
					}

					aMatcher = Keyword.ATTRIBUTE.text.matcher(aLine);
					if (aMatcher.find())
					{
						if (checkDataWithXMLTable)
						{
							if (parseAttribute(aLine.substring(aMatcher.end()), 
								anAttributeIndex).getName().equals(itsTable.getColumn(anAttributeIndex).getName()))
							{
								++anAttributeIndex;
								continue;
							}
							else
							{
								// TODO malformedFileWarning(), abort
								break;
							}
						}
						else
							itsTable.getColumns()
								.add(parseAttribute(aLine.substring(aMatcher.end()), anAttributeIndex++));
					}
				}

				else if (Keyword.DATA.atStartOfLine(aLine))
				{
					if (!relationFound)
					{
						// TODO malformedFileWarning(), no table created yet, abort
						// malformedFileWarning();
						break;
					}
					else if (anAttributeIndex == 0)
					{
						// TODO malformedFileWarning(), no attributes yet, abort
						// malformedFileWarning();
						break;
					}
					else if (dataFound)
					{
						// TODO malformedFileWarning(), try to continue
						continue;
					}
					else
						dataFound = true;
				}

				else if (dataFound)
				{
					loadData(aLine.trim(), itsNrDataRows);
					itsNrDataRows++;
				}
//				else
				{
					// TODO malformedFileWarning(), try to continue
				}
				if (itsNrDataRows > 0 && itsNrDataRows % 10000 == 0)
					Log.logCommandLine("loadFile: " + itsNrDataRows + " lines read");
			}
			if (itsNrBadRows > 0)
				Log.logCommandLine("FileLoaderARFF: " + itsNrBadRows + " offending lines encounterd.");
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
	// attribute type(s), only NUMERIC/NOMINAL/BINARY for now, not ORDINAL
	private Column parseAttribute(String theLine, int theIndex)
	{
		String aName;

		// get attribute name (not so clean)
		if (theLine.startsWith("\'"))
			aName = removeOuterQuotes(theLine);
		else
			aName = theLine.split("\\s", 2)[0];

		theLine = theLine.replaceFirst("\\'?" + aName + "\\'?", "").trim();

		// (aName, theLine) HACK for NominalAttribute
		return new Column(aName, null, declaredType(aName, theLine), theIndex, Column.DEFAULT_INIT_SIZE);
	}

	// TODO do this only once for the whole line
	private static String removeOuterQuotes(String theString)
	{
		if (theString.startsWith("\'"))
		{
			// find first unescaped "'"
			char[] aCharArray = theString.toCharArray();

			for (int i = 1, j = aCharArray.length; i < j; i++)
			{
				if (aCharArray[i] == '\\')	// jump beyond escaped char
					i++;
				else if (aCharArray[i] == '\'')
					return theString.substring(1, i);
			}
			// no closing ' found, log message, return theString asis
			Log.logCommandLine(
				"FileLoaderARFF: could not find a closing ' for String:\n\t" +
				theString + "\nReturning whole String.");
		}
		return theString;
	}

	// TODO checking of declared nominal classes for @attributes { class1, class2, ..} declarations
	private void loadData(String theLine, int theLineNr)
	{
		// String argument should not start with whitespace
		assert (!Character.isWhitespace(theLine.charAt(0)));

		String aCell;
		boolean isBad = false;

		for (Column c : itsTable.getColumns())
		{
			// XXX MM - ugly hack for now
			// NOTE trim() only makes a copy if required
			theLine = theLine.trim();

			if (theLine.startsWith("\'"))
			{
				aCell = removeOuterQuotes(theLine);

				// use offset, there might be ',' within quotes
				// ',' need not be directly after closing quote
				// +2 for quotes
				int idx = theLine.indexOf(',', aCell.length()+2);
				if (idx >= 0)
					// remove "'aCell'\\s*,"
					theLine = theLine.substring(idx+1);
				else
					theLine = theLine.substring(aCell.length()+2);
			}
			else
			{
				// just find first "," and trim if needed
				int idx = theLine.indexOf(",");
				if (idx >= 0)
					aCell = theLine.substring(0, idx).trim();
				else
					aCell = theLine.trim();

				if (idx >= 0)
					// remove "aCell\\s*,"
					theLine = theLine.substring(idx+1);
				else
					theLine = theLine.substring(aCell.length());
			}

			if (aCell.equals("?"))
			{
				c.setMissing(itsNrDataRows);
				addMissingToColumn(c);
			}
			else
				isBad |= !addValueToColumn(c, aCell, theLineNr);
		}

		// empty numeric cell (missing value) or parseFloat(cell) failed
		if (isBad)
		{
			if (itsNrBadRows == 0)
				Log.logCommandLine("FileLoaderARFF: more lines with erroneous values for numeric columns will not be reported per line.");
			itsNrBadRows++;
		}

		if (!theLine.isEmpty())
			if (!Keyword.COMMENT.atStartOfLine(theLine))
				Log.logCommandLine("FileLoaderARFF: many arguments at line:\n\t " + theLine);
				// TODO criticalError(toManyArgumentsError);
	}

	// returns true if added correctly
	// false if numeric cell was empty, or could not be parsed as Float
	private boolean addValueToColumn(Column theColumn, String theCell, int theLine)
	{
		switch (theColumn.getType())
		{
			case NOMINAL : theColumn.add(theCell); break;
			case NUMERIC :
			{
				try
				{
					theColumn.add(Float.parseFloat(theCell));
				}
				catch (NumberFormatException e)
				{
					theColumn.add(MISSING_NUMERIC);

					// only log once
					if (itsNrBadRows == 0)
					{
						String aCause;
						if (theCell.isEmpty())
							aCause = "missing value";
						else
							aCause = "unparsable value '" + theCell + "'";
						logNumericError(theLine, aCause, theColumn);
					}

					return false;
				}
				break;
			}
			case ORDINAL :
			{
				throw new AssertionError(theColumn.getType());
			}
			case BINARY :
			{
				// TODO this will fail on AutoRun loading files where
				// isValidBinaryValue(theCell) does not hold
				// ie. data with 2 values, set to BINARY by user
				if (!AttributeType.isValidBinaryValue(theCell))
					throw new IllegalArgumentException(getClass().getSimpleName() + ": invalid BINARY value: " + theCell);
				theColumn.add(AttributeType.isValidBinaryTrueValue(theCell));
				break;
			}
			default :
			{
				String.format(getClass().getSimpleName() + ": unknown AttributeType '%s'", theColumn.getType());
				throw new AssertionError(theColumn.getType());
			}
		}
		return true;
	}

	private static final void logNumericError(int theLine, String theCause, Column theColumn)
	{
		Log.logCommandLine(
			String.format("FileLoaderARFF: line %d, %s in numeric column '%s', inserting %.0f.",
					theLine, theCause, theColumn.getName(), MISSING_NUMERIC));
	}

	private void addMissingToColumn(Column theColumn)
	{
		switch (theColumn.getType())
		{
			case NOMINAL :
			{
				if (checkDataWithXMLTable)
					theColumn.add(theColumn.getMissingValue());
				else
					theColumn.add(AttributeType.NOMINAL.DEFAULT_MISSING_VALUE);
				break;
			}
			case NUMERIC :
			{
				if (checkDataWithXMLTable)
					theColumn.add(Float.parseFloat(theColumn.getMissingValue()));
				else
					theColumn.add(MISSING_NUMERIC);
				break;
			}
			case ORDINAL :
			{
				throw new AssertionError(theColumn.getType());
			}
			case BINARY :
			{
				if (checkDataWithXMLTable)
					theColumn.add(AttributeType.isValidBinaryTrueValue(theColumn.getMissingValue()));
				else
					theColumn.add(AttributeType.isValidBinaryTrueValue(AttributeType.BINARY.DEFAULT_MISSING_VALUE));
				break;
			}
			default :
			{
				String.format(getClass().getSimpleName() + ": unknown AttributeType '%s'", theColumn.getType());
				throw new AssertionError(theColumn.getType());
			}
		}
	}

	// determine attribute type(s), only NUMERIC/NOMINAL for now, not ORDINAL/BINARY
	private AttributeType declaredType(String theAttributeName, String theString)
	{
		String s = theString.toLowerCase();

		if (s.startsWith("real") || s.startsWith("integer") || s.startsWith("numeric"))
			return AttributeType.NUMERIC;

		else if (s.startsWith("{"))
		{
			theString = theString.substring(1);
			ArrayList<String> nominalClasses = new ArrayList<String>(10);
			String aNominalClass;

			// duplicate code
			while (!theString.startsWith("}"))
			{
				int offset = 0;

				if (theString.startsWith("\'"))
				{
					aNominalClass = removeOuterQuotes(theString);
					offset = 3;
				}
				else
				{
					if (theString.contains(","))
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

//			itsNominalAttributes.add(new NominalAttribute(itsTable.getAttribute(theAttributeName), nominalClasses));


			// TODO use enum
			if (nominalClasses.size() == 2)
			{
				String a = nominalClasses.get(0);
				String b = nominalClasses.get(1);

				if ((AttributeType.isValidBinaryTrueValue(a) &&
						AttributeType.isValidBinaryFalseValue(b))
					||
						(AttributeType.isValidBinaryTrueValue(b) &&
							AttributeType.isValidBinaryFalseValue(a)))
					return AttributeType.BINARY;
			}
			// TODO present attributeType change dialog to user
			return AttributeType.NOMINAL;
		}

		// TODO parseDate using dateFormat
		else if (s.startsWith("date"))
			return AttributeType.NOMINAL;

		return AttributeType.NOMINAL;
	}

	// TODO create ErrorDialog class, that also logs the error
	private static void criticalError() { Log.logCommandLine("ERROR"); }

	@Override
	public Table getTable() { return itsTable; }
}
