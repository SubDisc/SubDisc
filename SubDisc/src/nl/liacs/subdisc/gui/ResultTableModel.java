package nl.liacs.subdisc.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.Iterator;

import javax.swing.table.AbstractTableModel;

import nl.liacs.subdisc.SearchParameters;
import nl.liacs.subdisc.Subgroup;
import nl.liacs.subdisc.SubgroupSet;

public class ResultTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	private SubgroupSet itsSubgroupSet;
	private SearchParameters itsSearchParameters;

	public ResultTableModel(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters)
	{
		itsSubgroupSet = theSubgroupSet;
		itsSearchParameters = theSearchParameters;
	}

	public SearchParameters getSearchParameters() { return itsSearchParameters; }

	public int getRowCount()
	{
		return itsSubgroupSet.size();
	}

	public int getColumnCount() { return 5; }

	public String getColumnName(int theColumnIndex)
	{
		String aColumnName = new String();
		switch(theColumnIndex)
		{
			case 0	: { aColumnName = "Nr."; break; }
			case 1  : { aColumnName = "Depth"; break; }
			case 2	: { aColumnName = "Coverage"; break; }
			case 3	: { aColumnName = "Measure"; break; }
			case 4	: { aColumnName = "Conditions"; break; }
		}
		return aColumnName;
	}

	public Object getValueAt(int theRowIndex, int theColumnIndex)
	{
		NumberFormat aFormatter = NumberFormat.getNumberInstance();

		String aString = new String("---");
		Iterator<Subgroup> anIterator;
		anIterator = itsSubgroupSet.iterator();

		// Good way to walk trough sorted list?
		for (int i = 0 ; i < theRowIndex; i++)
			anIterator.next();

		// Next Subgroup is the one
		Subgroup aSubgroup = anIterator.next();

		switch(theColumnIndex)
		{
			case 0:	{	aString = Integer.toString(aSubgroup.getID());
						break; }
			case 1:	{	aString = Integer.toString(aSubgroup.getNrConditions());
						break; }
			case 2:	{	aString = Integer.toString(aSubgroup.getCoverage());
						break; }
			case 3: {
						aFormatter.setMaximumFractionDigits(6);
						aString = aFormatter.format(aSubgroup.getMeasureValue());
						break; }
			case 4: {
						aString = aSubgroup.getConditions().toString();
						break;
					}
		}
		return aString;
	}

	public void toUniqueFile(String theFileName)
	{
			boolean errorMade = false;
			java.io.OutputStream aFileStream = System.out;
			FileOutputStream aFile = null;
			String LOGPATH = new String("../log/");

			try
			{
				File alogPath = new File(LOGPATH);
				alogPath.mkdirs(); // create directories if not already there

				String aUniqueID = "" + System.currentTimeMillis();
				aFile = new java.io.FileOutputStream(LOGPATH + theFileName + aUniqueID + ".wri");
			} catch (Exception ex)
			{
				errorMade = true;
			}

			if (!errorMade)
			{
				aFileStream = aFile;
				try { // SubgroupSet ColumnHeader
					aFileStream.write(charsToBytes(getHeaderString().toCharArray()));
					aFileStream.write('\n');
				} catch (Exception ex) { }

				for (int row = 0; row < getRowCount(); row++)
				try {
					aFileStream.write(charsToBytes(getSubgroupString(row).toCharArray()));
					aFileStream.write('\n');
				} catch (Exception ex) { }
			}

			try {
				aFileStream.flush(); aFileStream.close();
			} catch (Exception ex) { }
	}

	public String getHeaderString()
	{
		String theString = "";

		// add column names
		for (int column = 0; column < getColumnCount(); column ++)
		{
			theString += getColumnName(column);
			if ( column < getColumnCount() - 1)
				theString += '\t';
			else
				theString += '\n';
		}

		return theString;
	}

	public String getSubgroupString(int row)
	{
		String theString = "";
		for (int column = 0; column < getColumnCount(); column ++)
		{
			theString += (String)getValueAt(row, column);
				if ( column < getColumnCount() - 1)
					theString += '\t';
				//else
					//theString += '\n';
		}
		return theString;
	}

	private static byte[] charsToBytes(char[] ca)
	{
		byte[] ba = new byte[ca.length];
		for (int i = 0; i < ca.length; i++)
			ba[i] = (byte)ca[i];
		return ba;
	}
}