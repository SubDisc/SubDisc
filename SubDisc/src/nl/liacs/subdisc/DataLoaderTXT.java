package nl.liacs.subdisc;

import java.io.*;
import java.util.*;

// TODO
// set MissingValues
// DELIMITERs should be available to all Loaders
public class DataLoaderTXT implements FileLoaderInterface
{
	// should be made available to all loaders (through FileLoaderInterface)
	private static final String[] DELIMITERS = { "\\s*\t\\s*", "\\s*,\\s*", "\\s*;\\s*" };
	private static final String[] CLEAN_DELIMITERS = { "\t", ",", ";" };

	private Table itsTable = null;
	private int itsDelimiter = 0;
	private int itsNrLines = 0;

	// default file loader
	public DataLoaderTXT(File theFile)
	{
		String aWarning = null;

		if (theFile == null)
			aWarning = "file can not be null";
		else if (!theFile.exists())
			aWarning = theFile.getAbsolutePath() + ", file does not exist";
		else if (!theFile.canRead())
			aWarning = theFile.getAbsolutePath() + ", file not readable";

		if (aWarning != null)
		{
			// TODO new ErrorDialog(e, ErrorDialog.noSuchFileError);
			message("<init>", aWarning);
			return;
		}

		loadFile(theFile);
	}

	// XML-loader, Table is created based on XML, data is loaded here
	public DataLoaderTXT(File theFile, Table theTable)
	{
		String aWarning = null;

		if (theFile == null)
			aWarning = "file can not be null";
		else if (!theFile.exists())
			aWarning = theFile.getAbsolutePath() + ", file does not exist";
		else if (!theFile.canRead())
			aWarning = theFile.getAbsolutePath() + ", file not readable";

		if (aWarning != null)
		{
			// TODO new ErrorDialog(e, ErrorDialog.noSuchFileError);
			message("<init>", aWarning);
			return;
		}

		itsTable = theTable;
		if (itsTable == null)
			message("<init>", "Table is null, attempting regular file-load.");
		loadFile(theFile);
	}

	private void message(String theMethod, String theMessage)
	{
		Log.logCommandLine(String.format("%s.%s(): %s",
							this.getClass().getSimpleName(),
							theMethod,
							theMessage));
	}

	private void loadFile(File theFile)
	{
		// this will get two BufferedReaders, prevents TOCTOE bugs
		// analyse establishes the number of data lines and delimiter
		if (!analyse(theFile))
			return;

		BufferedReader aReader = null;
		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aHeaderLine = null;
			String aLine;
			int aLineNr = 0;
			final boolean aMissingBinary = Boolean.parseBoolean(AttributeType.BINARY.DEFAULT_MISSING_VALUE);
			final float aMissingNumeric = Float.parseFloat(AttributeType.NUMERIC.DEFAULT_MISSING_VALUE);
			// print progress every once in a while
			int aPrintTrigger = 1000;
			int aPrintUpdate = 1000;

			// skip header, make sure line is not empty/ null
			while ((aLine = aReader.readLine()) != null)
			{
				++aLineNr;
				if (!aLine.isEmpty())
				{
					aHeaderLine = aLine;
					break;
				}
			}

			// used for XML sanity check later
			AttributeType[] anOriginalTypes = null;
			// loaded from XML
			if (itsTable != null)
			{
				anOriginalTypes = checkXMLTable(aHeaderLine, theFile);
				// something is seriously wrong
				if (anOriginalTypes == null)
					return;
			}
			else
			{
				// read first data line, create Table based on it
				while ((aLine = aReader.readLine()) != null)
				{
					++aLineNr;
					if (!aLine.isEmpty())
					{
						createTable(theFile, aHeaderLine, aLine);
						break;
					}
				}
			}

			List<Column> aColumns = itsTable.getColumns();
			final int aNrColumns = aColumns.size();
			BitSet aBinaries = new BitSet(aNrColumns);
			BitSet aFloats = new BitSet(aNrColumns);
			String[] aTrueBinaryValues = new String[aNrColumns];
			String[] aFalseBinaryValues = new String[aNrColumns];
			// Scanner is faster for long lines, but it is harder to identify faulty lines. Using .split() this would be trivial.
			Scanner aScanner = new Scanner(aLine).useDelimiter(getDelimiter());
			//initialise the true and false binary values for the columns that appear to be binary
			int aColumn = 0;
			while (aScanner.hasNext() && aColumn < aNrColumns)
			{

				if (aColumns.get(aColumn).getType() == AttributeType.BINARY)
				{
					String s = aScanner.next();
					removeQuotes(s);
					if (AttributeType.isValidBinaryValue(s))
					{
						boolean aValue = AttributeType.isValidBinaryTrueValue(s);
						if (aValue) //this was true
							aTrueBinaryValues[aColumn] = s;
						else
							aFalseBinaryValues[aColumn] = s;
					}
				}
				aColumn++;
			}

			for (int i=0; i<aNrColumns; i++)
				if (AttributeType.BINARY == aColumns.get(i).getType())
					aBinaries.set(i);
				else if (AttributeType.NUMERIC == aColumns.get(i).getType())
					aFloats.set(i);

			message("loadFile", "loading data");
			// code ignores AttributeType.ORDINAL
			while ((aLine = aReader.readLine()) != null)
			{
				aLineNr++;
				if (aLine.isEmpty())
					continue;
				aScanner = new Scanner(aLine).useDelimiter(getDelimiter());

				//read fields
				aColumn = -1;
				while (aScanner.hasNext() && aColumn < aNrColumns)
				{
					aColumn++;
					String s = aScanner.next();
					removeQuotes(s);


					if (aBinaries.get(aColumn)) // is it currently set to binary? (this may change as more lines are read)
					{
						// check if it is a missing value or a known binary value
						if (isEmptyString(s))
						{
							aColumns.get(aColumn).add(aMissingBinary);
							continue;
						}
						else if (AttributeType.isValidBinaryValue(s))
						{
							boolean aValue = AttributeType.isValidBinaryTrueValue(s);
							aColumns.get(aColumn).add(aValue);
							if (aValue) //this was true
								aTrueBinaryValues[aColumn] = s;
							else
								aFalseBinaryValues[aColumn] = s;
							continue;
						}

						// if neither missing nor binary, then it shouldn't be binary
						aBinaries.set(aColumn, false);
						
						try //if it is a float
						{
							float f = Float.parseFloat(s);
							aFloats.set(aColumn, true);
							aColumns.get(aColumn).setType(AttributeType.NUMERIC);
							aColumns.get(aColumn).add(f);
							Log.logCommandLine(aColumns.get(aColumn).getName() + " was binary, is numeric (line " + aLineNr + ")");
						}
						catch (NumberFormatException e) // guess it's a nominal then
						{
							aColumns.get(aColumn).toNominalType(aTrueBinaryValues[aColumn], aFalseBinaryValues[aColumn]);
							Log.logCommandLine(aColumns.get(aColumn).getName() + " was binary, is nominal (line " + aLineNr + ")");
						}
							
					}
					else if (aFloats.get(aColumn))
					{
						try //if it is a float
						{
							if (isEmptyString(s))
								aColumns.get(aColumn).add(aMissingNumeric);
							else
							{
								float f = Float.parseFloat(s);
								aColumns.get(aColumn).add(f);
							}
						}
						catch (NumberFormatException e) // guess it's a nominal then
						{
							aFloats.set(aColumn, false);
							aColumns.get(aColumn).setType(AttributeType.NOMINAL);
							aColumns.get(aColumn).add(s);
							Log.logCommandLine(aColumns.get(aColumn).getName() + " was float, is nominal (line " + aLineNr + ")");
						}
					}
					else //it was nominal
					{
						if (isEmptyString(s))
							aColumns.get(aColumn).add(AttributeType.NOMINAL.DEFAULT_MISSING_VALUE);
						else
							aColumns.get(aColumn).add(s);
					}
				}
				if (aColumn != aNrColumns-1)
					message("loadFile", "incorrect number of fields on line " + aLineNr +". " + aNrColumns + " expected, " + (aColumn+1) + " found.");
				if (aLineNr == aPrintTrigger)
				{
					message("loadFile", aLineNr + " lines read");
					aPrintTrigger += aPrintUpdate;
					// increase print update interval, but
					// print at least every 10,000 lines
					if ((aPrintTrigger == (aPrintUpdate * 10)) && (aPrintUpdate != 10000))
					{
						aPrintUpdate *= 10;
						message("loadFile", "number of lines for update interval changed to: " + aPrintUpdate);
					}
				}
			}
			for (Column c : aColumns)
				System.out.println("Column " + c.getName() + " (" + c.getType() + ")");

			// one final check about the validity of the XML file
			if (anOriginalTypes != null)
				evaluateXMLLoading(anOriginalTypes, theFile);
		}
		catch (IOException e)
		{
//			new ErrorDialog(e, ErrorDialog.fileReaderError);
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
//				new ErrorDialog(e, ErrorDialog.fileReaderError);
				e.printStackTrace();
			}
		}
	}

	// cumbersome, but cleanly handles empty lines before/ after header line
	private boolean analyse(File theFile)
	{
		message("analyse", "analysing " + theFile.getAbsolutePath());
		boolean aSuccess = false;
		BufferedReader aReader = null;

		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aHeaderLine;
			String aLine;
			int aNrDataLines = 0;

			// find first non empty line (header line)
			while ((aHeaderLine = aReader.readLine()) != null)
				if (!aHeaderLine.isEmpty())
					break;

			// find second non empty line (to determine delimiter)
			while ((aLine = aReader.readLine()) != null)
			{
				if (!aLine.isEmpty())
				{
					++aNrDataLines;
					establishDelimiter(aHeaderLine, aLine);
					break;
				}
			}

			// check on number of columns is deferred to loadFile
			while ((aLine = aReader.readLine()) != null)
				if (!aLine.isEmpty())
					++aNrDataLines;

			message("analyse", aNrDataLines + " lines of data found");
			itsNrLines = aNrDataLines;
			aSuccess = true;
		}
		catch (IOException e)
		{
			message("analyse", "IOException caused by file: " + theFile.getAbsolutePath());
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
				message("analyse", "IOException caused by file: " + theFile.getAbsolutePath());
				e.printStackTrace();
			}
		}
		return aSuccess;
	}

	private void establishDelimiter(String theFirstLine, String theSecondLine)
	{
		int aNrDelimiters = DELIMITERS.length;
		int[] aCounts = new int[aNrDelimiters];
		int aNrOptions = 0;
		String aMessage = "";

		for (int i = 0, j = aNrDelimiters; i < j; ++i)
		{
			aCounts[i] = theFirstLine.split(DELIMITERS[i], -1).length;
			if (aCounts[i] > 1)
				++aNrOptions;
		}

		if (aNrOptions == 0)
			aMessage = "unable to determine delimiter, using \'" + CLEAN_DELIMITERS[0] + "\'";
		else if (aNrOptions == 1)
		{
			for (int i = 0, j = aNrDelimiters; i < j; ++i)
				if (aCounts[i] > 1)
				{
					itsDelimiter = i;
					aMessage = "successfully established delimiter, using \'" + CLEAN_DELIMITERS[i] + "\'";
				}
		}
		else // (aNrOptions > 1)
		{
			for (int i = 0, j = aNrDelimiters; i < j; ++i)
			{
				if (aCounts[i] > 1)
				{
					// just pick the first one
					if (aCounts[i] == theSecondLine.split(DELIMITERS[i], -1).length)
					{
						itsDelimiter = i;
						aMessage = "unsure about delimiter, using \'" + CLEAN_DELIMITERS[i] + "\'";
						break;
					}
				}
			}

			aMessage = "unable to determine delimiter, using \'" + CLEAN_DELIMITERS[0] + "\'";
		}
		message("establishDelimiter", aMessage);
	}

	// check the XML declared Table ColumnNames against the HeaderLine
	// the returned array contains the ColumnTypes as declared in XML
	private AttributeType[] checkXMLTable(String theHeaderLine, File theFile)
	{
		final String[] aHeaders = theHeaderLine.split(getDelimiter(), -1);
		final int aNrColumns = aHeaders.length;
		boolean returnNull = false;

		// check if number of columns is equal in XML and File
		if (aNrColumns != itsTable.getColumns().size())
		{
			message("checkXMLTable",
					String.format("ERROR%nNumber of Columns declared in XML: %d%nNumber of Columns retrieved from File %s: %d",
							itsTable.getColumns().size(),
							theFile.getName(),
							aNrColumns));
			returnNull = true;
		}

		// check whether ColumnNames are equal in XML and File
		for (int i = 0; i < aNrColumns; ++i)
		{
			if (!aHeaders[i].equals(itsTable.getColumn(i).getName()))
			{
				message("checkXMLTable",
					String.format("ERROR on index %d%nColumn '%s' from XML does not match Column '%s' from File '%s'",
							(i+1),
							itsTable.getColumn(i).getName(),
							aHeaders[i].trim(),
							theFile.getName()));
				returnNull = true;
				break;
			}
		}

		if (returnNull)
			return null;
		else
		{
			final AttributeType[] theOriginalTypes = new AttributeType[aNrColumns];
			for (int i = 0, j = aNrColumns; i < j; ++i)
				theOriginalTypes[i] = itsTable.getColumn(i).getType();
			return theOriginalTypes;
		}
	}

	// create Table Columns using HeaderLine names, base Type on DataLine
	private void createTable(File theFile, String aHeaderLine, String aDataLine)
	{
		message("createTable", "creating Table");
		String[] aHeaders = aHeaderLine.split(getDelimiter());
		String[] aData = aDataLine.split(getCleanDelimiter());

		// for-each loop might not work for data changes
		for (String s : aHeaders)
			removeQuotes(s);

		// create Table and Columns
		itsTable = new Table(theFile, itsNrLines, aHeaders.length);
		List<Column> aColumns = itsTable.getColumns();

		for (int i = 0, j = aHeaders.length; i < j; ++i)
		{
			String s = aData[i];
			removeQuotes(s);

			// is it binary (or empty String)
			// TODO set itsMissing
			if (AttributeType.isValidBinaryValue(s) || isEmptyString(s))
			{
				aColumns.add(new Column(aHeaders[i],
							null,
							AttributeType.BINARY,
							i,
							itsNrLines));
				if (isEmptyString(s))
					aColumns.get(i).add(Boolean.parseBoolean(AttributeType.BINARY.DEFAULT_MISSING_VALUE));
				else
					aColumns.get(i).add(AttributeType.isValidBinaryTrueValue(s));
				continue;
			}

			// is it numeric
			try
			{
				// empty String is handled by BINARY case
				float f = Float.parseFloat(s);
				aColumns.add(new Column(aHeaders[i],
							null,
							AttributeType.NUMERIC,
							i,
							itsNrLines));
				aColumns.get(i).add(f);
				continue;
			}
			catch (NumberFormatException e) {}

			// is it ordinal
			// NO USE CASE YET

			// it is nominal
			aColumns.add(new Column(aHeaders[i],
						null,
						AttributeType.NOMINAL,
						i,
						itsNrLines));
			aColumns.get(i).add(s);
		}
	}

	// NOTE null is never passed as input parameter
	private void removeQuotes(String theString)
	{
		// avoid references to arrays/ CharSequences, create new String
		theString = new String(theString);

		// fail fast
		if (theString.isEmpty() || ((theString.charAt(0) != '\"') && (theString.charAt(0) != '\'')))
			return;

		int aLength = theString.length();
		if (	(((theString.charAt(0) == '\"') && (theString.charAt(aLength-1) == '\"')) ||
			((theString.charAt(0) == '\'') && (theString.charAt(aLength -1) == '\''))) &&
			(aLength > 2))
				theString = theString.substring(1, theString.length()-1);
	}

	// NOTE null is never passed as input parameter
	private boolean isEmptyString(String s)
	{
		return (s.matches("\\s*"));
	}

	private void evaluateXMLLoading(AttributeType[] theOriginalTypes, File theFile)
	{
		for (int i = 0, j = theOriginalTypes.length; i < j; ++i)
			if (itsTable.getColumn(i).getType() != theOriginalTypes[i])
				message("evaluateXMLLoading",
					String.format("WARNING Column '%s'%n\tXML declared AttributeType: '%s'%n\tAttributeType after parsing File '%s': '%s'",
							itsTable.getColumn(i).getName(),
							theOriginalTypes[i].toString(),
							theFile.getAbsolutePath(),
							itsTable.getColumn(i).getType()));
	}

	@Override
	public Table getTable()
	{
		// TODO will still return a table, even if no data is loaded, change
		// MiningWindow could fall back to 'no table' if itsTable.getNrRows == 0
		return itsTable;
	}
	
	public String getDelimiter()
	{
		return DELIMITERS[itsDelimiter];
	}
	
	public String getCleanDelimiter()
	{
		return CLEAN_DELIMITERS[itsDelimiter];
	}
}
