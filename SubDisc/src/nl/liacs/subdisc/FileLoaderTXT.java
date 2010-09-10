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
	private String itsSeparator = FileLoaderInterface.DEFAULT_SEPARATOR;

	public FileLoaderTXT(File theFile)
	{
		if(theFile != null && theFile.exists())
			loadFile(theFile);
		else
			;	// new ErrorDialog(e, ErrorDialog.noSuchFileError);
	}

	private Table loadFile(File theFile)
	{
		if(checkFormatAndType(theFile))
		{
			int aNrColumns = itsTable.getNrColumns();
			BufferedReader aReader = null;

			try
			{

				aReader = new BufferedReader(new FileReader(theFile));
				String aLine = aReader.readLine(); //skip header

				while ((aLine = aReader.readLine()) != null)
				{
					String[] anImportRow = aLine.split(itsSeparator,-1);
					//read fields
					for (int i = 0; i < aNrColumns; ++i)
					{
						Column aColumn = itsTable.getColumns().get(i);
						if (itsTable.getAttribute(i).isNominalType())		//NOMINAL
							aColumn.add(anImportRow[i].trim());
						else if (itsTable.getAttribute(i).isBinaryType())	//BINARY
							aColumn.add(anImportRow[i].trim().equals("1"));
						else												//NUMERIC
							aColumn.add(Float.parseFloat(anImportRow[i]));
					}
				}

				itsTable.update();
				Log.logCommandLine("File loaded: " + aNrColumns + " columns, " + itsTable.getNrRows() + " rows.");
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
		else
		{
			Log.logCommandLine("File " + theFile + " is not well-formed, i.e. not all records have the same number of attributes.");
		}
		return itsTable;
	}

	private boolean checkFormatAndType(File theFile)
	{
		BufferedReader aReader = null;
		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			boolean isWellFormedFile = true;
			BitSet aNominals = new BitSet();
			BitSet aNotZeroOne = new BitSet();
			String aLine = aReader.readLine(); //first line is header
			String[] aHeaders = aLine.split(itsSeparator,-1);
			int aNrColumns = aHeaders.length;
			int aNrRows = 0;
	
			while ((aLine = aReader.readLine()) != null)
			{
				++aNrRows;

				String[] aRow = aLine.split(itsSeparator, -1);
				int aLineNrColumns = aRow.length;

				for (int i = 0; i < aLineNrColumns; ++i)
				{
					try
					{
						Float.parseFloat(aRow[i]);
						if (!aRow[i].equals("0") && !aRow[i].equals("1")) //numeric could be binary also
							aNotZeroOne.set(i);
					}
					catch (NumberFormatException anException) //if not a float
					{
						aNominals.set(i);
					}
				}

				if( aLineNrColumns != aNrColumns)
				{
					Log.logCommandLine("Line " + aNrRows + " has " + aLineNrColumns + " columns, instead of the expected " + aNrColumns);
					isWellFormedFile = false; //continue checking
				}
			}

			//assign types
			itsTable = new Table(FileType.removeExtension(theFile), theFile.getName(), aNrRows, aNrColumns);

			for (int i=0; i<aNrColumns; i++)
			{
				if (aNominals.get(i))
					itsTable.getColumns().add(new Column(new Attribute(aHeaders[i].trim(), null, AttributeType.NOMINAL, i), aNrRows));
				else if (aNotZeroOne.get(i))
					itsTable.getColumns().add(new Column(new Attribute(aHeaders[i].trim(), null, AttributeType.NUMERIC, i), aNrRows));
				else
					itsTable.getColumns().add(new Column(new Attribute(aHeaders[i].trim(), null, AttributeType.BINARY, i), aNrRows));
			}
			itsTable.update();
			for (Attribute anAttribute : itsTable.getAttributes())
				anAttribute.print();
			return isWellFormedFile;
		}
		catch (IOException e)
		{
			e.printStackTrace();
//			new ErrorDialog(e, ErrorDialog.fileReaderError);
			return false;
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
//				new ErrorDialog(e, ErrorDialog.fileReaderError);
			}
		}

	}

	public void setSeparator(String theNewSeparator)
	{
		itsSeparator = theNewSeparator;
	}

	public Table getTable()
	{
		return itsTable;
	}
}
