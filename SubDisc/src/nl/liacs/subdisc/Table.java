package nl.liacs.subdisc;

import java.io.*;
import java.util.*;

import nl.liacs.subdisc.Attribute.*;
import nl.liacs.subdisc.gui.*;

import org.w3c.dom.*;

public class Table
{
	// all but Random can be made final
	private String itsSource;
	private String itsName;
	private int itsNrRows;
	private int itsNrColumns;
	private ArrayList<Attribute> itsAttributes = new ArrayList<Attribute>();
	private ArrayList<Column> itsColumns = new ArrayList<Column>();
	private Random itsRandomNumber = new Random(System.currentTimeMillis());

	public String getName() { return itsName; }
	public String getSource() { return itsSource; }

	// NOTE itsNrColumns is not tied to itsColumns.size()
	public int getNrRows() { return itsNrRows; }
	public int getNrColumns() { return itsNrColumns; } //just the descriptors

//	public String getSeparator() { return itsSeparator; }
	public Attribute getAttribute(int i) { return itsAttributes.get(i); }
	public Column getColumn(Attribute theAttribute)
	{
		// index == null for ARFF/MRML
		return itsColumns.get(theAttribute.getIndex());
	}
	public Column getColumn(int theIndex) { return itsColumns.get(theIndex); }

	public ArrayList<Attribute> getAttributes() { return itsAttributes; };
	public ArrayList<Column> getColumns() { return itsColumns; };

	// TODO some constructors and builder functions, may change
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
		itsAttributes.ensureCapacity(theNrColumns);
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
		itsAttributes.clear();	// expensive
		for (Column c : itsColumns)
			itsAttributes.add(c.getAttribute());
	}

	/**
	 * Retrieves an array of <code>int[]</code>s, containing the number of
	 * {@link Columns Columns} for each {@link AttributeType AttributeType}, and
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

	//returns a complete column (as long as it is binary)
	public BitSet getBinaryColumn(int i)
	{
		return itsColumns.get(i).getBinaries();
	}

	// Obsolete, see MiningWindow.jListSecondaryTargetsActionPerformed()
	//returns the index of the theIndex'th binary column
/*
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
*/

	//Data Model ===========================================================================

	public Attribute getAttribute(String theName)
	{
		for (Attribute anAttribute : itsAttributes)
		{
			if (anAttribute.getName().equals(theName))
				return anAttribute;
		}
		return null; //not found
	}

	public Condition getFirstCondition()
	{
		return new Condition(itsAttributes.get(0));
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
				aCondition = new Condition(itsAttributes.get(anIndex + 1));
//			{
//				Attribute anAttribute = itsAttributes.get(anIndex+1);
//				if (anAttribute.isNumericType())
//					aCondition = new Condition(anAttribute, Condition.FIRST_NUMERIC_OPERATOR);
//				else
//					aCondition = new Condition(anAttribute, Condition.FIRST_NOMINAL_OPERATOR);
//			}
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
/*
		for (int i=0; i<itsNrRows; i++)
		{
			if (theSubset.get(i))
			{
				aResult[k] = aColumn.getFloat(i);
				k++;
			}
		}
*/
		Arrays.sort(aResult);
		return aResult;
	}

	//returns the unique, sorted domain
	public float[] getUniqueNumericDomain(int theColumn, BitSet theSubset)
	{
		//get domain including doubles
		float[] aDomain = getNumericDomain(theColumn, theSubset);

		//count uniques
		float aCurrent = aDomain[0];
		int aCount = 1;
		for (Float aFloat : aDomain)
			if (aFloat != aCurrent)
			{
				aCurrent = aFloat;
				aCount++;
			}

		float[] aResult = new float[aCount];
		aCurrent = aDomain[0];
		aCount = 1;
		aResult[0] = aDomain[0];
		for (Float aFloat : aDomain)
			if (aFloat != aCurrent)
			{
				aCurrent = aFloat;
				aResult[aCount] = aFloat;
				aCount++;
			}

		return aResult;
	}

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

		for (int i=0, j = aColumn.size(); i < j; i++)
		{
			if (aColumn.isNominalType() && aColumn.getNominal(i).equals(theValue))
				aResult++;
			else if (aColumn.isBinaryType() && aColumn.getBinary(i)=="1".equals(theValue))
				aResult++;
		}
		return aResult;
	}

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
/*
	public void print()
	{
		Log.logCommandLine("Types ===========================================");
		for (Attribute anAttribute : itsAttributes)
			anAttribute.print();
		Log.logCommandLine("Table ===========================================");
		for (int i = 0; i < itsNrRows; i++)
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
*/
	// also works before Table.update()
	public void print()
	{
		Log.logCommandLine("Types ===========================================");
		for (Column c : itsColumns)
			c.getAttribute().print();
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

	public void addNodeTo(Node theParentNode)
	{
		Node aNode = XMLNode.addNodeTo(theParentNode, "table");
		XMLNode.addNodeTo(aNode, "table_name", itsName);
		XMLNode.addNodeTo(aNode, "source", itsSource);

		for (int i = 0, j = itsColumns.size(); i < j; ++i)
		{
			itsColumns.get(i).addNodeTo(aNode);
			((Element)aNode.getLastChild()).setAttribute("nr", String.valueOf(i));
		}
	}

}
