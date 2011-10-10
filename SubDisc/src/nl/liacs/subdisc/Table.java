package nl.liacs.subdisc;

import java.io.*;
import java.util.*;

import javax.swing.*;

import nl.liacs.subdisc.FileHandler.Action;

import org.w3c.dom.*;

public class Table
{
	// all but Random can be made final
	private String itsSource;
	private String itsName;
	private int itsNrRows;
	private int itsNrColumns;
	private ArrayList<Column> itsColumns = new ArrayList<Column>();
//	private int itsNrNominals = 0;
//	private int itsNrNumerics = 0;
//	private int itsNrOrdinals = 0;
//	private int itsNrBinaries = 0;
	private Random itsRandomNumber = new Random(System.currentTimeMillis());
	private List<String> itsDomains;
	private List<Integer> itsDomainIndices; //allows for much faster removal

	public String getName() { return itsName; }
	public String getSource() { return itsSource; }

	// NOTE itsNrColumns is not tied to itsColumns.size()
	public int getNrRows() { return itsNrRows; }
	public int getNrColumns() { return itsNrColumns; } //just the descriptors

//	public Attribute getAttribute(int i) { return itsColumns.get(i).getAttribute(); }
//	public Column getColumn(Attribute theAttribute) { return itsColumns.get(theAttribute.getIndex()); }
	public Column getColumn(int theIndex) { return itsColumns.get(theIndex); }

	public ArrayList<Column> getColumns() { return itsColumns; };

	// Empty table, meant for creating a copy with a subset of data. See select().
	public Table(String theTableName)
	{
		itsName = theTableName;
	}

	// FileLoaderARFF
	public Table(File theSource, String theTableName)
	{
		itsSource = theSource.getName();
		itsName = theTableName;
	}

	// FileLoaderTXT
	public Table(File theSource, int theNrRows, int theNrColumns)
	{
		itsSource = theSource.getName();
		itsName = FileType.removeExtension(theSource);
		itsNrRows = theNrRows;
		itsNrColumns = theNrColumns;
		itsColumns.ensureCapacity(theNrColumns);
	}

	// TODO order of nodes is known, when all is stable
	// FileLoaderXML
	public Table(Node theTableNode, String theXMLFileDirectory)
	{
		NodeList aChildren = theTableNode.getChildNodes();
		for (int i = 0, j = aChildren.getLength(); i < j; ++i)
		{
			Node aSetting = aChildren.item(i);
			String aNodeName = aSetting.getNodeName();
			if ("table_name".equalsIgnoreCase(aNodeName))
				itsName = aSetting.getTextContent();
			else if ("source".equalsIgnoreCase(aNodeName))
				itsSource = aSetting.getTextContent();
			else if ("column".equalsIgnoreCase(aNodeName))
				itsColumns.add(new Column(aSetting));
		}
		/*
		 * now all columns are know, check if data (Attributes) is valid by
		 * loading actual data from itsSource
		 */
		new FileHandler(new File(theXMLFileDirectory + "/"+ itsSource), this);
	}

	/*
	 * TODO maintaining itsNrColumns becomes more difficult now
	 * complete new Column functionality to be implemented later
	 * TODO throw dialog on duplicate domain?
	 * add CUI Domain, relies on caller for Table.update()
	 * FileHandler.printLoadingInfo calls Table.update();
	 */
	boolean addDomain(String theDomainName)
	{
//not public
//		if (theDomainName == null)
//			return false;

		if (itsDomains == null)
		{
			itsDomains = new ArrayList<String>();
			itsDomainIndices = new ArrayList<Integer>();
		}

		if (itsDomains.contains(theDomainName))
		{
			Log.logCommandLine(
				String.format(
					"A domain with the name '%s' is already present.",
					theDomainName));
			return false;
		}
		else
		{
			itsDomains.add(theDomainName);

			/*
			 * a rank column will be inserted if it is not present
			 * offset ensures it will not be removed when the first
			 * domain is removed from the Table
			 */
			if (itsDomainIndices.size() == 0)
			{
				int offset = 1;
				for (Column c : itsColumns)
				{
					if (c.getName().equalsIgnoreCase("rank"))
					{
						--offset;
						break;
					}
				}
				itsDomainIndices.add(itsColumns.size() + offset);
			}
			else
				itsDomainIndices.add(itsColumns.size());

			return true;
		}
	}

	//FileHandler.printLoadingInfo calls Table.update();
	//TODO removeDomains(int[] theDomainIndices)
	public void removeDomain(int theDomainIndex)
	{
		if (itsDomains == null || theDomainIndex < 0 || theDomainIndex > itsDomains.size() - 1)
		{
			Log.logCommandLine(String.format("Domain '%s' not found."));
			return;
		}

		itsDomains.remove(theDomainIndex);
		int aStartIndex = itsDomainIndices.remove(theDomainIndex).intValue();
		int anEndIndex; //check whether it is the last domain

		if (theDomainIndex == itsDomainIndices.size())
			anEndIndex = itsColumns.size();
		else
			anEndIndex = itsDomainIndices.get(theDomainIndex);

		/*
		 * removing from ArrayList backwards avoids expensive arrayCopy
		 * if domain is at the end
		 */
		for (int i = anEndIndex - 1; i >= aStartIndex ; --i)
			itsColumns.remove(i);

		int aNrDeletedColumns = anEndIndex - aStartIndex;
		for (int i = theDomainIndex, j = itsDomainIndices.size(); i < j; ++i)
			itsDomainIndices.set(i, itsDomainIndices.get(i) - aNrDeletedColumns);

		itsNrColumns = itsColumns.size();
		itsColumns.trimToSize();

		//reset indices for all Columns after removed domain
		//inefficient if multiple domains are removed at once
		if (aStartIndex < itsNrColumns)
			for (int i = aStartIndex; i < itsNrColumns; ++i)
				itsColumns.get(i).setIndex(i);

		if (itsDomains.isEmpty())
		{
			itsDomains = null;
			itsDomainIndices = null;
		}
	}

	public JList getDomainList()
	{
		/*
		 * MiningWindow should guarantee 'Remove' is only available when
		 * itsDomains is not null/empty
		 */
		if (itsDomains == null)
			return null;
		else
			return new JList(itsDomains.toArray());
	}

	/*
	 * TODO change this method, goal is to create a lock() function that 'locks'
	 * the table. itsNrRows/itsNrColumn and itsAttributes/itsColumns.size() do
	 * not change anymore. Update is expensive right now. If itsAttributes would
	 * be implemented as a HashSet/TreeSet adding would be less of a problem.
	 */
	/**
	 * Updates this Table. This means the number of rows and columns are set,
	 * and this Tables' list of {@link Attribute Attribute}s is updated.
	 */
	public void update()
	{
		itsNrRows = itsColumns.size() > 0 ? itsColumns.get(0).size() : 0;
		itsNrColumns = itsColumns.size();	// needed for MiningWindow

		for (Column c : itsColumns)
			c.getCardinality();
	}

	/**
	 * Retrieves an array of <code>int[]</code>s, containing the number of
	 * {@link Column Column}s for each {@link AttributeType AttributeType}, and
	 * the number of those Columns that are enabled. The <code>int[]</code>s are
	 * for AttributeTypes: <code>NOMINAL</code>, <code>NUMERIC</code>,
	 * <code>ORDINAL</code> and <code>BINARY</code>, respectively.
	 * @return an array of <code>int[]</code>s, containing for each
	 * AttributeType the number of Columns of that type, and the number of
	 * those Columns that is enabled
	 */
	public int[][] getTypeCounts()
	{
		int[][] aCounts = new int[4][2];
		for(Column c : itsColumns)
		{
			switch(c.getType())
			{
				case NOMINAL :
				{
					++aCounts[0][0];
					if (c.getIsEnabled())
						++aCounts[0][1];
					break;
				}
				case NUMERIC :
				{
					++aCounts[1][0];
					if (c.getIsEnabled())
						++aCounts[1][1];
					break;
				}
				case ORDINAL :
				{
					++aCounts[2][0];
					if (c.getIsEnabled())
						++aCounts[2][1];
					break;
				}
				case BINARY :
				{
					++aCounts[3][0];
					if (c.getIsEnabled())
						++aCounts[3][1];
					break;
				}
			}
		}
		return aCounts;
	}

	/*
	 * TODO Would be more intuitive, but current is more efficient as it only
	 * loops over all Columns once. An alternative would be to ensure the Table
	 * is always updated after AttributeType changes/Column.setIsEnabled() and
	 * use itsNominals/itsNrNumerics/itsNrOrdinals/itsNrBinaries members.
	 */
//	public int getNrNominals() {};
//	public int getNrNumerics() {};
//	public int getNrOrdinals() {};
//	public int getNrBinaries() {};
/*
	public BitSet evaluate(Condition theCondition)
	{
		BitSet aSet = new BitSet(itsNrRows);

		Attribute anAttribute = theCondition.getAttribute();
		int anIndex = anAttribute.getIndex();
		Column aColumn = itsColumns.get(anIndex);
		for (int j=0; j<itsNrRows; j++)
		{
			if (anAttribute.isBinaryType())
			{
				if (theCondition.evaluate(aColumn.getBinary(j)))
					aSet.set(j);
			}
			else
			{
				String aValue;
				if (anAttribute.isNominalType())
					aValue = aColumn.getNominal(j);
				else
					aValue = Float.toString(aColumn.getFloat(j));
				if (theCondition.evaluate(aValue))
					aSet.set(j);
			}
		}

		return aSet;
	}
*/

	// as above, but only checks type once
	public BitSet evaluate(Condition theCondition)
	{
		BitSet aSet = new BitSet(itsNrRows);
		Column aColumn = theCondition.getAttribute();

		switch (aColumn.getType())
		{
			case NOMINAL :;
			{
				for (int j = 0; j < itsNrRows; ++j)
					if (theCondition.evaluate(aColumn.getNominal(j)))
						aSet.set(j);
				break;
			}
			case NUMERIC :
			case ORDINAL :
			{
				for (int j = 0; j < itsNrRows; ++j)
					if (theCondition.evaluate(Float.toString(aColumn.getFloat(j))))
						aSet.set(j);
				break;
			}
			case BINARY :
			{
				for (int j = 0; j < itsNrRows; ++j)
						if (theCondition.evaluate(aColumn.getBinary(j)))
							aSet.set(j);
				break;
			}
			default :
			{
				Log.logCommandLine(
					String.format(
						"Table.evaluate(): unknown AttributeType '%s'",
									aColumn.getType()));
					break;
			}
		}

		return aSet;
	}

/*
	public BitSet evaluate(ConditionList theList)
	{
		BitSet aSet = new BitSet(itsNrRows);
		aSet.set(0, itsNrRows); //set all to true first, because of conjunction

		for (int i=0; i<theList.size(); i++) //loop over conditions
		{
			Condition aCondition = theList.get(i);
			Attribute anAttribute = aCondition.getAttribute();
			int anIndex = anAttribute.getIndex();
			Column aColumn = itsColumns.get(anIndex);
			for (int j=0; j<itsNrRows; j++)
			{
				if (anAttribute.isNumericType() && !aCondition.evaluate(Float.toString(aColumn.getFloat(j))))
					aSet.set(j, false);
				if (anAttribute.isNominalType() && !aCondition.evaluate(aColumn.getNominal(j)))
					aSet.set(j, false);
				if (anAttribute.isBinaryType() && !aCondition.evaluate(aColumn.getBinary(j)))
					aSet.set(j, false);
			}
		}
		return aSet;
	}
*/

	// as above but only checks type once
	public BitSet evaluate(ConditionList theList)
	{
		BitSet aSet = new BitSet(itsNrRows);
		aSet.set(0, itsNrRows); //set all to true first, because of conjunction

		for (Condition aCondition : theList) //loop over conditions
		{
			Column aColumn = aCondition.getAttribute();

			switch (aColumn.getType())
			{
				case NOMINAL:
				{
					for (int j = 0; j < itsNrRows; ++j)
						if (!aCondition.evaluate(aColumn.getNominal(j)))
							aSet.set(j, false);
					break;
				}
				case NUMERIC:
				case ORDINAL:
				{
					for (int j = 0; j < itsNrRows; ++j)
						if (!aCondition.evaluate(Float.toString(aColumn.getFloat(j))))
							aSet.set(j, false);
					break;
				}
				case BINARY:
				{
					for (int j = 0; j < itsNrRows; ++j)
						if (!aCondition.evaluate(aColumn.getBinary(j)))
							aSet.set(j, false);
					break;
				}
				default :
				{
					Log.logCommandLine(
						String.format(
								"Table.evaluate(): unknown AttributeType '%s'",
											aColumn.getType()));
					break;
				}
			}
		}

		return aSet;
	}

	//returns a complete column (as long as it is binary)
	public BitSet getBinaryColumn(int i)
	{
		return itsColumns.get(i).getBinaries();
	}

	//Data Model ===========================================================================

	public Column getAttribute(String theName)
	{
		for (Column c : itsColumns)
		{
			if (c.getName().equals(theName))
				return c;
		}
		return null; //not found
	}

	public int getIndex(String theName)
	{
		for (Column c : itsColumns)
			if (c.getName().equals(theName))
				return c.getIndex();
		return -1; // not found (causes ArrayIndexOutOfBounds)
	}

	public Condition getFirstCondition()
	{
		return new Condition(itsColumns.get(0));
	}

	//TODO this code is overly complex, due to its Safarii-background
	//Arno needs to fix this, at some point.

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
				aCondition = new Condition(itsColumns.get(anIndex + 1));
		}

		return aCondition;
	}


	// Misc ===============================

	public float[] getNumericDomain(int theColumn, BitSet theSubset)
	{
		float[] aResult = new float[theSubset.cardinality()];

		Column aColumn = itsColumns.get(theColumn);
		for (int i = 0, j = 0; i < itsNrRows; i++)
			if (theSubset.get(i))
				aResult[j++] = aColumn.getFloat(i);

		Arrays.sort(aResult);
		return aResult;
	}

	// does not handle float.NaN well, but neither does the rest of the code
	//returns the unique, sorted domain
	public float[] getUniqueNumericDomain(int theColumn, BitSet theSubset)
	{
		//get domain including doubles
		float[] aDomain = getNumericDomain(theColumn, theSubset);
/*
		//count uniques
		float aCurrent = aDomain[0];
		int aCount = 1;
		for (float aFloat : aDomain)
			if (aFloat != aCurrent)
			{
				aCurrent = aFloat;
				aCount++;
			}

		float[] aResult = new float[aCount];
		aCurrent = aDomain[0];
		aCount = 1;
		aResult[0] = aDomain[0];
		for (float aFloat : aDomain)
			if (aFloat != aCurrent)
			{
				aCurrent = aFloat;
				aResult[aCount] = aFloat;
				aCount++;
			}
 */
		int aCount = itsColumns.get(theColumn).getCardinality();
		float[] aResult = new float[aCount];
		float aCurrent = aResult[0] = aDomain[0];

		for (int i = 1, j = aDomain.length, k = 1; i < j ; ++i)
		{
			float aFloat = aDomain[i];
			if (aFloat != aCurrent)
			{
				aResult[k] = aCurrent = aFloat;

				if (++k == aCount)
					break;
			}
		}

		return aResult;
	}

	// TODO check for out of range
	public TreeSet<String> getDomain(int theColumn)
	{
		Column aColumn = itsColumns.get(theColumn);
		return aColumn.getDomain();
	}

	public float[] getSplitPoints(int theColumn, BitSet theSubset, int theNrSplits)
	{
		float[] aDomain = getNumericDomain(theColumn, theSubset);
		float[] aSplitPoints = new float[theNrSplits];
		for (int j=0; j<theNrSplits; j++)
			aSplitPoints[j] = aDomain[aDomain.length*(j+1)/(theNrSplits+1)];	// N.B. Order matters to prevent integer division from yielding zero.
		return aSplitPoints;
	}

	//only works for nominals and binary
	public int countValues(int theColumn, String theValue)
	{
		int aResult = 0;
		Column aColumn = itsColumns.get(theColumn);

		switch (aColumn.getType())
		{
			case NOMINAL :
			{
				for (int i=0, j = aColumn.size(); i < j; ++i)
					if (aColumn.getNominal(i).equals(theValue))
						++aResult;
				break;
			}
			case BINARY :
			{
				for (int i=0, j = aColumn.size(); i < j; ++i)
					if (aColumn.getBinary(i)=="1".equals(theValue))
						++aResult;
				break;
			}
			default :
			{
				Log.logCommandLine("Do not use a Column of type '" +
									aColumn.getType().toString() +
									"' with Table.countValues(). Please use NOMINAL or BINARY."
						);
				break;
			}
		}

		return aResult;
	}
/*
	//only works for nominals and binary
	public int countValues(int theColumn, String theValue)
	{
		int aResult = 0;
		Column aColumn = itsColumns.get(theColumn);

		for (int i=0, j = aColumn.size(); i < j; i++)
		{
			if (aColumn.isNominalType() && aColumn.getNominal(i).equals(theValue))
				aResult++;
			else if (aColumn.isBinaryType() && aColumn.getBinary(i)=="1".equals(theValue))
				aResult++;
		}
		return aResult;
	}
*/
	public float getAverage(int theColumn)
	{
		float aResult = 0;
		Column aColumn = itsColumns.get(theColumn);
		for (int i=0, j=aColumn.size(); i<j; i++)
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
		Subgroup aSubgroup = new Subgroup(0.0, theSize, 0, null);
		aSubgroup.setMembers(aSample);
		return aSubgroup;
	}

	public Table select(BitSet theSet)
	{
		Table aResult = new Table(itsName);
		aResult.itsSource = itsSource;
		aResult.itsNrColumns = itsNrColumns;
		aResult.itsNrRows = theSet.cardinality();

		//copy each column, while leaving out some of the data
		for (Column aColumn : itsColumns)
			aResult.itsColumns.add(aColumn.select(theSet));

		return aResult;
	}

	public void swapRandomizeTarget(TargetConcept theTC)
	{
		List<Column> aTargets = new ArrayList<Column>(2);

		//find all targets
		switch (theTC.getTargetType())
		{
			case DOUBLE_REGRESSION:
			case DOUBLE_CORRELATION:
				aTargets.add(theTC.getSecondaryTarget());
				//no break
			case SINGLE_NOMINAL:
			case SINGLE_NUMERIC:
				aTargets.add(theTC.getPrimaryTarget());
				break;
			case MULTI_LABEL:
				aTargets = theTC.getMultiTargets();
				break;
			default : ; // TODO should never get here, throw warning
		}

		int n = getNrRows();
		//start with regular order
		int[] aPermutation = new int[n];
		for (int i=0; i<n; i++)
			aPermutation[i] = i;

		//randomize
		for (int i=0; i<n-1; i++)
		{
			int aFirst = i;
			int aSecond = i+itsRandomNumber.nextInt(n-i);

			//swap first and second
			int aSwap = aPermutation[aFirst];
			aPermutation[aFirst] = aPermutation[aSecond];
			aPermutation[aSecond] = aSwap;
		}

		//execute permutation on all targets
		for (Column anAttribute : aTargets)
		{
			Log.logCommandLine("permuting " + anAttribute.getName());
			anAttribute.permute(aPermutation);
		}
	}

	public void print()
	{
		Log.logCommandLine("Types ===========================================");
		for (Column c : itsColumns)
			c.print();
		Log.logCommandLine("Table ===========================================");
		for (int i = 0, j = itsColumns.get(0).size(); i < j; i++)
		{
			StringBuilder aRows = new StringBuilder("Row ");
			aRows.append(i + 1);
			aRows.append(": ");
			for (Column aColumn : itsColumns)
			{
				aRows.append(aColumn.getString(i));
				aRows.append(", ");
			}
			Log.logCommandLine(aRows
								.substring(0, aRows.length() - 2)
								.toString());
		}
		Log.logCommandLine("=================================================");
	}

	// TODO arff-writer, AttributeType inclusion makes is load faster/safer
	/**
	 * Write this Table to <code>File</code>.
	 * 
	 * @param theMembers the indices of the set bits in this <BitSet>
	 * correspond to the row numbers of this Table that should be written to
	 * the output <code>File</code>. If the parameter is <code>null</code>,
	 * the whole Table will be written.
	 */
	public void toFile(BitSet theMembers)
	{
		BufferedWriter aWriter = null;

		File aFile = new FileHandler(Action.SAVE).getFile();

		if (aFile == null)
			return;
		else if (theMembers == null)
			toFile(aFile);
		else
		{
			try
			{
				aWriter = new BufferedWriter(new FileWriter(aFile));
				int aNrColumnsMinusOne = itsNrColumns - 1;

				for (int i = 0; i < aNrColumnsMinusOne; ++i)
				{
					aWriter.write(itsColumns.get(i).getName());
					aWriter.write(",");
				}
				aWriter.write(itsColumns.get(aNrColumnsMinusOne).getName());
				aWriter.write("\n");

				// could return if (theMembers.cardinality == 0)
				// lookup columnTypes only once
				AttributeType[] aTypes = new AttributeType[itsNrColumns];
				for (int i = 0, j = itsNrColumns; i < j; ++i)
					aTypes[i] = itsColumns.get(i).getType();

				// itsNrRows for safety
				for (int i = theMembers.nextSetBit(0); i >= 0 && i < itsNrRows; i = theMembers.nextSetBit(i + 1))
				{
					for (int k = 0; k < aNrColumnsMinusOne; ++k)
					{
						switch(aTypes[k])
						{
							case NOMINAL : aWriter.write(itsColumns.get(k).getNominal(i)); break;
							case NUMERIC : aWriter.write(String.valueOf(itsColumns.get(k).getFloat(i))); break;
							case ORDINAL : aWriter.write(String.valueOf(itsColumns.get(k).getFloat(i))); break;
							case BINARY : aWriter.write(itsColumns.get(k).getBinary(i) ? "1" : "0"); break;
							default : Log.logCommandLine("Unknown AttributeType: " + aTypes[k]); break;
						}
						aWriter.write(",");
					}
					switch(aTypes[aNrColumnsMinusOne])
					{
						case NOMINAL : aWriter.write(itsColumns.get(aNrColumnsMinusOne).getNominal(i)); break;
						case NUMERIC : aWriter.write(String.valueOf(itsColumns.get(aNrColumnsMinusOne).getFloat(i))); break;
						case ORDINAL : aWriter.write(String.valueOf(itsColumns.get(aNrColumnsMinusOne).getFloat(i))); break;
						case BINARY : aWriter.write(itsColumns.get(aNrColumnsMinusOne).getBinary(i) ? "1" : "0"); break;
						default : Log.logCommandLine("Unknown AttributeType: " + aTypes[aNrColumnsMinusOne]); break;
					}
					aWriter.write("\n");
				}
				aWriter.write("\n");
			}
			catch (IOException e)
			{
				Log.logCommandLine("Error while writing: " + aFile);
			}
			finally
			{
				try
				{
					if (aWriter != null)
						aWriter.close();
				}
				catch (IOException e)
				{
					Log.logCommandLine("Error while writing: " + aFile);
				}
			}
		}
	}

	// as above, but writes whole Table (no row inclusion test)
	private void toFile(File theFile)
	{
		BufferedWriter aWriter = null;

		try
		{
			aWriter = new BufferedWriter(new FileWriter(theFile));
			int aNrColumnsMinusOne = itsNrColumns - 1;

			for (int i = 0; i < aNrColumnsMinusOne; ++i)
			{
				aWriter.write(itsColumns.get(i).getName());
				aWriter.write(",");
			}
			aWriter.write(itsColumns.get(aNrColumnsMinusOne).getName());
			aWriter.write("\n");

			// lookup columnTypes only once
			AttributeType[] aTypes = new AttributeType[itsNrColumns];
			for (int i = 0, j = itsNrColumns; i < j; ++i)
				aTypes[i] = itsColumns.get(i).getType();

			for (int i = 0, j = itsNrRows; i < j; ++i)
			{
				for (int k = 0; k < aNrColumnsMinusOne; ++k)
				{
					switch(aTypes[k])
					{
						case NOMINAL : aWriter.write(itsColumns.get(k).getNominal(i)); break;
						case NUMERIC : aWriter.write(String.valueOf(itsColumns.get(k).getFloat(i))); break;
						case ORDINAL : aWriter.write(String.valueOf(itsColumns.get(k).getFloat(i))); break;
						case BINARY : aWriter.write(itsColumns.get(k).getBinary(i) ? "1" : "0"); break;
						default : Log.logCommandLine("Unknown AttributeType: " + aTypes[k]); break;
					}
					aWriter.write(",");
				}
				switch(aTypes[aNrColumnsMinusOne])
				{
					case NOMINAL : aWriter.write(itsColumns.get(aNrColumnsMinusOne).getNominal(i)); break;
					case NUMERIC : aWriter.write(String.valueOf(itsColumns.get(aNrColumnsMinusOne).getFloat(i))); break;
					case ORDINAL : aWriter.write(String.valueOf(itsColumns.get(aNrColumnsMinusOne).getFloat(i))); break;
					case BINARY : aWriter.write(itsColumns.get(aNrColumnsMinusOne).getBinary(i) ? "1" : "0"); break;
					default : Log.logCommandLine("Unknown AttributeType: " + aTypes[aNrColumnsMinusOne]); break;
				}
				aWriter.write("\n");
			}
			aWriter.write("\n");
		}
		catch (IOException e)
		{
			Log.logCommandLine("Error while writing: " + theFile);
		}
		finally
		{
			try
			{
				if (aWriter != null)
					aWriter.close();
			}
			catch (IOException e)
			{
				Log.logCommandLine("Error while writing: " + theFile);
			}
		}
	}

	public void addNodeTo(Node theParentNode)
	{
		Node aNode = XMLNode.addNodeTo(theParentNode, "table");
		XMLNode.addNodeTo(aNode, "table_name", itsName);
		XMLNode.addNodeTo(aNode, "source", itsSource);

		for (int i = 0, j = itsColumns.size(); i < j; ++i)
		{
			itsColumns.get(i).addNodeTo(aNode);
			// FIXME index is not always set in Column
			((Element)aNode.getLastChild()).setAttribute("nr", String.valueOf(i));
		}
	}
}
