package nl.liacs.subdisc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.TreeSet;

public class Table
{
	// all but Random can be made final
	public String itsName = "no tablename";
	int itsNrRows;
	int itsNrColumns;
	private ArrayList<Attribute> itsAttributes = new ArrayList<Attribute>();
	private ArrayList<Column> itsColumns = new ArrayList<Column>();
//	private String itsSeparator = ",";	// TODO remove
	private Random itsRandomNumber;

	// NOTE itsNrColumns is not tied to itsColumns.size()
	public int getNrRows() { return itsNrRows; }
	public int getNrColumns() { return itsNrColumns; } //just the descriptors

//	public String getSeparator() { return itsSeparator; }
	public Attribute getAttribute(int i) { return itsAttributes.get(i); }
	public Column getColumn(Attribute theAttribute) { return itsColumns.get(theAttribute.getIndex()); }	// index == null for ARFF/MRML

	public ArrayList<Attribute> getAttributes() { return itsAttributes; };
	public ArrayList<Column> getColumns() { return itsColumns; };

	// TODO some constructors and builder functions, may change
	public Table()
	{
		itsRandomNumber = new Random(System.currentTimeMillis());	// Not needed in constructor?
	}

	public Table(int theNrRows, int theNrColumns)
	{
		itsNrRows = theNrRows;
		itsNrColumns = theNrColumns;
		itsAttributes.ensureCapacity(theNrColumns);
		itsColumns.ensureCapacity(theNrColumns);
		itsRandomNumber = new Random(System.currentTimeMillis());
	}

	// allows making itsAttributes unmodifiable/final
	public Table setAttributes(ArrayList<Attribute> theAttributes)
	{
		itsAttributes = theAttributes;
		return this;
	}

	public Table addAttribute(Attribute theAttribute)
	{
		itsAttributes.add(theAttribute);
		return this;
	}

	public Table setColumns(ArrayList<Column> theColumns)
	{
		itsColumns = theColumns;
		return this;
	}

	public Table addColumn(Column theColumn)
	{
		itsColumns.add(theColumn);
		return this;
	}

/*
	// From file
	public Table(File theFile)
	{
		itsNrRows = 0;
		itsNrColumns = 0;
		itsRandomNumber = new Random(System.currentTimeMillis());
		try
		{
			loadFile(theFile);
		}
		catch(Exception e)
		{
			Log.logCommandLine("Loading file failed: "+e.getMessage());
			e.printStackTrace();
		}
	}

	//current loader guesses some of the types
	//text -> nominal
	//{0,1} -> binary
	//other -> numeric
	//TODO implement ARFF or MRML loading
	public void loadFile(File theFile) throws Exception
	{
		if(checkFormatAndType(theFile))
		{
			itsColumns = new ArrayList<Column>(itsNrColumns);
			for (int i=0; i<itsNrColumns; i++)
				itsColumns.add(new Column(itsAttributes.get(i).getType(), itsNrRows));

			BufferedReader aReader = new BufferedReader(new FileReader(theFile));
			String aSeparator = getSeparator();
			String aLine = aReader.readLine(); //skip header
			int aRowCount = 0;

			while ((aLine = aReader.readLine()) != null)
			{
				String[] anImportRow = aLine.split(aSeparator,-1);
				//read fields
				for (int i = 0; i < itsNrColumns; i++)
				{
					Column aColumn = itsColumns.get(i);
					if (itsAttributes.get(i).isNominalType()) 		//NOMINAL
						aColumn.add(anImportRow[i]);
					else if (itsAttributes.get(i).isBinaryType()) 	//BINARY
						aColumn.add(anImportRow[i].equals("1"));
					else 											//NUMERIC
						aColumn.add(Float.parseFloat(anImportRow[i]));
				}
				aRowCount++;
			}
			aReader.close();
			Log.logCommandLine("File loaded: " + itsNrColumns + " columns, " + itsNrRows + " rows.");
		}
		else
		{
			Log.logCommandLine("File " + theFile + " is not well-formed, i.e. not all records have the same number of attributes.");
		}
	}

	public boolean checkFormatAndType(File theFile) throws Exception
	{
		BufferedReader aReader = new BufferedReader(new FileReader(theFile));
		String aSeparator = getSeparator();
		boolean isWellFormedFile = true;
		BitSet aNominals = new BitSet();
		BitSet aNotZeroOne = new BitSet();
		String aLine = aReader.readLine(); //first line is header
		String[] aHeaders = aLine.split(aSeparator,-1);
		itsNrColumns = aHeaders.length;

		while ((aLine = aReader.readLine()) != null)
		{
			itsNrRows++;

			String[] aRow = aLine.split(aSeparator,-1);
			for (int i=0; i<aRow.length; i++)
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
			int aLineNrColumns = aRow.length;

			if( aLineNrColumns != itsNrColumns)
			{
				Log.logCommandLine("Line " + itsNrRows + " has " + aLineNrColumns + " columns, instead of the expected " + itsNrColumns);
				isWellFormedFile = false; //continue checking
			}
		}
		aReader.close();

		//assign types
		itsAttributes = new ArrayList<Attribute>(itsNrColumns);
		for (int i=0; i<itsNrColumns; i++)
		{
			if (aNominals.get(i))
				itsAttributes.add(new Attribute(i, aHeaders[i], null, Attribute.NOMINAL));
			else if (aNotZeroOne.get(i))
				itsAttributes.add(new Attribute(i, aHeaders[i], null, Attribute.NUMERIC));
			else
				itsAttributes.add(new Attribute(i, aHeaders[i], null, Attribute.BINARY));
		}
		for (Attribute anAttribute : itsAttributes)
			anAttribute.print();
		return isWellFormedFile;
	}
*/

	public BitSet evaluate(Condition theCondition)
	{
		BitSet aSet = new BitSet(itsNrRows);

		Attribute anAttribute = theCondition.getAttribute();
		int anIndex = anAttribute.getIndex();
		Column aColumn = itsColumns.get(anIndex);
		for (int j=0; j<itsNrRows; j++)
		{
			String aValue;
			if (anAttribute.isNominalType())
				aValue = aColumn.getNominal(j);
			else if (anAttribute.isBinaryType())
				aValue = aColumn.getBinary(j)?"1":"0";
			else
				aValue = Float.toString(aColumn.getFloat(j));
			if (theCondition.evaluate(aValue))
				aSet.set(j);
		}

		return aSet;
	}

	public BitSet evaluate(ConditionList theList)
	{
		BitSet aSet = new BitSet(itsNrRows);
		aSet.set(0, itsNrRows); //set all to true first, because of conjunction

		for (int i=0; i<theList.size(); i++) //loop over conditions
		{
			Condition aCondition = theList.getCondition(i);
			Attribute anAttribute = aCondition.getAttribute();
			int anIndex = anAttribute.getIndex();
			Column aColumn = itsColumns.get(anIndex);
			for (int j=0; j<itsNrRows; j++)
				if (!aCondition.evaluate(Float.toString(aColumn.getFloat(j)))) //TODO implement for nominal
					aSet.set(j, false);
		}
		return aSet;
	}

	//returns a complete column (as long as it is binary)
	public BitSet getBinaryColumn(int i)
	{
		Column aColumn = itsColumns.get(i);
		return aColumn.getBinaries();
	}

	//returns the index of the theIndex'th binary column
	public int getBinaryIndex(int theIndex) throws ArrayIndexOutOfBoundsException
	{
		int anIndex = 0;
		for (int i=0; i<itsNrColumns; i++)
		{
			Attribute anAttribute = itsAttributes.get(i);
			if (anAttribute.isBinaryType())
			{
				if (anIndex == theIndex)
					return i;
				anIndex++; //found one binary column, but not the right one.
			}
		}
		throw(new ArrayIndexOutOfBoundsException("trying to acces index " + theIndex));
	}


	//Data Model ===========================================================================

	public Attribute getAttribute(String theName)
	{
		for (int i=0; i<itsAttributes.size(); i++)
		{
			Attribute anAttribute = itsAttributes.get(i);
			if (anAttribute.getName().equals(theName))
				return anAttribute;
		}
		return null; //not found
	}

	public Condition getFirstCondition()
	{
		return new Condition(itsAttributes.get(0), Condition.FIRST_NUMERIC_OPERATOR);
	}

	public Condition getNextCondition(Condition theCurrentCondition)
	{
		Condition aCondition;

		if (theCurrentCondition.hasNextOperator())
			aCondition = new Condition(theCurrentCondition.getAttribute(), theCurrentCondition.getNextOperator());
		else
		{
			int anIndex = theCurrentCondition.getAttribute().getIndex();
			if (anIndex == itsNrColumns-1) // No more attributes
				aCondition = null;
			else
				aCondition = new Condition(itsAttributes.get(anIndex+1), Condition.FIRST_NUMERIC_OPERATOR);
		}

		return aCondition;
	}


	// Misc ===============================


	//TODO sort the output?
	public float[] getNumericDomain(int theColumn, BitSet theSubset)
	{
		int aNrMembers = theSubset.cardinality();
		float[] aResult = new float[aNrMembers];
		int k=0;

		Column aColumn = itsColumns.get(theColumn);
		for (int i=0; i<itsNrRows; i++)
		{
			if (theSubset.get(i))
			{
				aResult[k] = aColumn.getFloat(i);
				k++;
			}
		}

		return aResult;
	}

	//TODO implement for nominal values, fix binary
	public TreeSet<String> getDomain(int theColumn)
	{
		Column aColumn = itsColumns.get(theColumn);
		return aColumn.getDomain();
	}

	public float[] getSplitPoints(int theColumn, BitSet theSubset, int theNrSplits)
	{
		float[] aDomain = getNumericDomain(theColumn, theSubset);
		Arrays.sort(aDomain);
		float[] aSplitPoints = new float[theNrSplits];
		for (int j=0; j<theNrSplits; j++)
			aSplitPoints[j] = aDomain[aDomain.length*(j+1)/(theNrSplits+1)];		// N.B. Order matters to prevent integer division from yielding zero.
		return aSplitPoints;
	}

	//only works for nominals and binary
	public int countValues(int theColumn, String theValue)
	{
		int aResult = 0;
		Column aColumn = itsColumns.get(theColumn);
		boolean aOne = theValue.equals("1");

		for (int i=0; i<aColumn.size(); i++)
		{
			if (aColumn.isNominalType() && aColumn.getNominal(i).equals(theValue))
				aResult++;
			if (aColumn.isBinaryType() && aColumn.getBinary(i)==aOne)
				aResult++;
		}
		return aResult;
	}

	public float getAverage(int theColumn)
	{
		float aResult = 0;
		Column aColumn = itsColumns.get(theColumn);
		for (int i=0; i<aColumn.size(); i++)
			aResult += aColumn.getFloat(i);
		return aResult/itsNrRows;
	}

	public Subgroup getRandomSubgroup(int theSize)
	{
		BitSet aSample = new BitSet(itsNrRows);
		int m = 0;
		int t = 0;

		for (int i = 0; i < itsNrRows; i++)
		{
			double aThresholdValue1 = (double) theSize - m;
			double aThresholdValue2 = (double) itsNrRows - t;

			if ((aThresholdValue2 * itsRandomNumber.nextDouble()) < aThresholdValue1)
			{
				aSample.set(i);
				m++;
				t++;
				if (m >= theSize)
					break;
			}
			else
				t++;
		}
		Subgroup aSubgroup = new Subgroup(new ConditionList(), 0, theSize, 0);
		aSubgroup.setMembers(aSample);
		return aSubgroup;
	}

	public void print()
	{
		Log.logCommandLine("Types ===========================================");
		for (Attribute anAttribute : itsAttributes)
			anAttribute.print();
		Log.logCommandLine("Table ===========================================");
		for (int i=0; i<itsNrRows; i++)
		{
			String aRow = "Row "+(i+1)+": ";
			for (int j=0; j<itsNrColumns; j++)
			{
				Column aColumn = itsColumns.get(j);

				String aValue = aColumn.getString(i);
				aRow += aValue;
				if (j<itsNrColumns-1)
					aRow += ", ";
			}
			Log.logCommandLine(aRow);
		}
		Log.logCommandLine("=================================================");
	}
}