/*
 * TODO this class does not remove quotes: 'value'
 */
package nl.liacs.subdisc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.BitSet;

import nl.liacs.subdisc.Attribute.AttributeType;

public class FileLoaderTXT implements FileLoaderInterface
{
	private Table itsTable = null;
	private boolean checkDataWithXMLTable = false;
	private String itsSeparator = FileLoaderInterface.DEFAULT_SEPARATOR;

	public FileLoaderTXT(File theFile)
	{
		if (theFile == null || !theFile.exists())
		{
			// TODO new ErrorDialog(e, ErrorDialog.noSuchFileError);
			Log.logCommandLine(
					String.format("FileLoaderTXT: can not open File '%s'",
									theFile.getAbsoluteFile()));
			return;
		}
		else
			loadFile(theFile);
	}

	public FileLoaderTXT(File theFile, Table theTable)
	{
		if (theFile == null || !theFile.exists())
		{
			// TODO new ErrorDialog(e, ErrorDialog.noSuchFileError);
			Log.logCommandLine(
					String.format("FileLoaderTXT: can not open File '%s'",
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

	private void loadFile(File theFile)
	{
		if (checkFormatAndType(theFile))
		{
			//does not call getNrColumns(), itsNrRows is not set yet
			int aNrColumns = itsTable.getColumns().size();
			BufferedReader aReader = null;

			try
			{
				aReader = new BufferedReader(new FileReader(theFile));
				String aLine;

				//skip header, make sure line is not empty/null
				while ((aLine = aReader.readLine()) != null)
					if (!aLine.isEmpty())
						break;

				while ((aLine = aReader.readLine()) != null)
				{
					if (aLine.isEmpty())
						continue;

					String[] anImportRow = aLine.split(itsSeparator, -1);
					//read fields
					for (int i = 0; i < aNrColumns; i++)
					{
						Column aColumn = itsTable.getColumns().get(i);

						switch (aColumn.getType())
						{
							case NOMINAL :
							{
								aColumn.add(anImportRow[i].trim());
								break;
							}
							case NUMERIC :
							case ORDINAL :
							{
								aColumn.add(Float.parseFloat(anImportRow[i]));
								break;
							}
							case BINARY :
							{
								aColumn.add(anImportRow[i].trim().equals("1"));
								break;
							}
							default : break; // TODO ERROR
						}
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
//				new ErrorDialog(e, ErrorDialog.fileReaderError);
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
//					new ErrorDialog(e, ErrorDialog.fileReaderError);
				}
			}
		}
	}

	private boolean checkFormatAndType(File theFile)
	{
		boolean isWellFormedFile = false; //default should always be false
		BufferedReader aReader = null;

		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			BitSet aNominals = new BitSet();
			BitSet aNotZeroOne = new BitSet();
			String aLine;

			// make sure line is not empty/null
			while ((aLine = aReader.readLine()) != null)
				if (!aLine.isEmpty())
					break;
			isWellFormedFile = true;

			String[] aHeaders = aLine.split(itsSeparator, -1);
			int aNrColumns = aHeaders.length;
			int aNrRows = 0;

			if (checkDataWithXMLTable)
			{
				if (aNrColumns != itsTable.getColumns().size())
				{
					Log.logCommandLine(
						"The number of Attributes for the Table read from " +
						"XML is not the same as that for the File " +
						theFile.getName() + "'.");
					return false;
				}
				else
					for (int i = 0; i < aNrColumns; i++)
					{
						if (!aHeaders[i].trim().equals(itsTable
														.getColumn(i)
														.getName()))
						{
							Log.logCommandLine(
								String.format(
								"At index %d: Attribute '%s' from XML does " +
								"not match Attribute '%s' from File '%s'.",
								(i + 1),
								itsTable.getColumn(i).getName(),
								aHeaders[i].trim(),
								theFile.getName()));
							break;
						}
					}
			}

			while ((aLine = aReader.readLine()) != null)
			{
				++aNrRows;

				String[] aRow = aLine.split(itsSeparator, -1);
				int aLineNrColumns = aRow.length;

				if (aLineNrColumns != aNrColumns)
				{
					Log.logCommandLine(
						String.format(
							"Line %d has %d columns instead of the expected %d."
							, aNrRows, aLineNrColumns, aNrColumns));
					isWellFormedFile = false;
					//continues checking to inform about more malformed lines
				}

				for (int i = 0; i < aLineNrColumns; i++)
				{
					String aCell = aRow[i];
					try
					{
						Float.parseFloat(aCell);
						//numeric could be binary also
						if (!aCell.equals("0") && !aCell.equals("1"))
							aNotZeroOne.set(i);
					}
					catch (NumberFormatException anException) //if not a float
					{
						aNominals.set(i);
					}
				}
			}

			//assign types
			itsTable = new Table(theFile, aNrRows, aNrColumns);

			for (int i = 0; i < aNrColumns; i++)
			{
				if (aNominals.get(i))
					itsTable.getColumns()
					.add(new Column(new Attribute(aHeaders[i].trim(),
													"",
													AttributeType.NOMINAL,
													i)
									, aNrRows));
				else if (aNotZeroOne.get(i))
					itsTable.getColumns()
					.add(new Column(new Attribute(aHeaders[i].trim(),
													"",
													AttributeType.NUMERIC,
													i)
									, aNrRows));
				else
					itsTable.getColumns()
					.add(new Column(new Attribute(aHeaders[i].trim(),
													"",
													AttributeType.BINARY,
													i)
									, aNrRows));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
//			new ErrorDialog(e, ErrorDialog.fileReaderError);
			return false;
		}
		finally
		{
			if (!isWellFormedFile)
				Log.logCommandLine(
					"File '" +
					theFile + 
					"' is not well-formed,\n i.e. not all records have the " +
					"same number of attributes.");

			try
			{
				if (aReader != null)
					aReader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
//				new ErrorDialog(e, ErrorDialog.fileReaderError);
			}
		}
		return isWellFormedFile;
	}

	public void setSeparator(String theNewSeparator)
	{
		itsSeparator = theNewSeparator;
	}

	@Override
	public Table getTable()
	{
		// TODO will still return a table, even if no data is loaded, change
		// MiningWindow could fall back to 'no table' if itsTable.getNrRows == 0
		return itsTable;
	}
}
