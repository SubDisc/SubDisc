/**
 * TODO Needs better trim()
 */
package nl.liacs.subdisc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.BitSet;

import nl.liacs.subdisc.Attribute.AttributeType;

public class FileLoaderTXT implements FileLoaderInterface
{
	private Table itsTable;
	private String itsSeparator = FileLoaderInterface.DEFAULT_SEPARATOR;

	@Override
	public Table loadFile(File theFile) throws Exception
	{
		if(checkFormatAndType(theFile))
		{
			int aNrColumns = itsTable.getNrColumns();

			BufferedReader aReader = new BufferedReader(new FileReader(theFile));
			String aLine = aReader.readLine(); //skip header

			while ((aLine = aReader.readLine()) != null)
			{
				String[] anImportRow = aLine.split(itsSeparator,-1);
				//read fields
				for (int i = 0; i < aNrColumns; i++)
				{
					Column aColumn = itsTable.getColumns().get(i);
					if (itsTable.getAttribute(i).isNominalType()) 		//NOMINAL
						aColumn.add(anImportRow[i]);
					else if (itsTable.getAttribute(i).isBinaryType()) 	//BINARY
						aColumn.add(anImportRow[i].equals("1"));
					else 											//NUMERIC
						aColumn.add(Float.parseFloat(anImportRow[i]));
				}

			}

			aReader.close();
			itsTable.update();
			Log.logCommandLine("File loaded: " + aNrColumns + " columns, " + itsTable.getNrRows() + " rows.");
		}
		else
		{
			Log.logCommandLine("File " + theFile + " is not well-formed, i.e. not all records have the same number of attributes.");
		}
		return itsTable;
	}

	private boolean checkFormatAndType(File theFile) throws Exception
	{
		BufferedReader aReader = new BufferedReader(new FileReader(theFile));
		boolean isWellFormedFile = true;
		BitSet aNominals = new BitSet();
		BitSet aNotZeroOne = new BitSet();
		String aLine = aReader.readLine(); //first line is header
		String[] aHeaders = aLine.split(itsSeparator,-1);
		int aNrColumns = aHeaders.length;
		int aNrRows = 0;

		while ((aLine = aReader.readLine()) != null)
		{
			aNrRows++;

			String[] aRow = aLine.split(itsSeparator,-1);
			int aLineNrColumns = aRow.length;

			for (int i=0; i<aLineNrColumns; i++)
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
		aReader.close();

		//assign types
		itsTable = new Table(aNrRows, aNrColumns);
		itsTable.itsName = theFile.getName();
		for (int i=0; i<aNrColumns; i++)
		{
			if (aNominals.get(i))
				itsTable.getColumns().add(new Column(new Attribute(i, aHeaders[i], null, AttributeType.NOMINAL), aNrRows));
			else if (aNotZeroOne.get(i))
				itsTable.getColumns().add(new Column(new Attribute(i, aHeaders[i], null, AttributeType.NUMERIC), aNrRows));
			else
				itsTable.getColumns().add(new Column(new Attribute(i, aHeaders[i], null, AttributeType.BINARY), aNrRows));
		}
		itsTable.update();
		for (Attribute anAttribute : itsTable.getAttributes())
			anAttribute.print();
		return isWellFormedFile;
	}

//	@Override
	public void setSeparator(String theNewSeparator)
	{
		itsSeparator = theNewSeparator;
	}

}
