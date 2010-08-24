package nl.liacs.subdisc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Table
{
	// all but Random can be made final
	private String itsName = "no tablename";
	private String itsSource = "no known source";
	private int itsNrRows;
	private int itsNrColumns;
	private ArrayList<Attribute> itsAttributes = new ArrayList<Attribute>();
	private ArrayList<Column> itsColumns = new ArrayList<Column>();
//	private String itsSeparator = ",";	// TODO remove
	private Random itsRandomNumber;

	public String getName() { return itsName; }
	public void setName(String theName) { itsName = theName; }
	public String getSource() { return itsSource; }
	public void setSource(String theSource) { itsSource = theSource; }

	// NOTE itsNrColumns is not tied to itsColumns.size()
	public int getNrRows() { return itsNrRows; }
	public int getNrColumns() { return itsNrColumns; } //just the descriptors

//	public String getSeparator() { return itsSeparator; }
	public Attribute getAttribute(int i) { return itsAttributes.get(i); }
	public Column getColumn(Attribute theAttribute) { return itsColumns.get(theAttribute.getIndex()); }	// index == null for ARFF/MRML
	public Column getColumn(int theIndex) { return itsColumns.get(theIndex); }

	public ArrayList<Attribute> getAttributes() { return itsAttributes; };
	public ArrayList<Column> getColumns() { return itsColumns; };

	public void update()
	{
		itsNrRows = itsColumns.size() > 0 ? itsColumns.get(0).size() : 0;
		itsNrColumns = getColumns().size();	// needed for MiningWindow
		for(Column c : itsColumns)
			itsAttributes.add(c.getAttribute());
	}

	public int[] getTypeCounts()
	{
		update();
		int[] theCounts = new int[3];
		for(Column c : itsColumns)
		{
			switch(c.getType())
			{
				case NUMERIC :
				case ORDINAL : ++theCounts[1]; break;
				case NOMINAL : ++theCounts[0]; break;
				case BINARY : ++theCounts[2]; break;
			}
		}
		return theCounts;
	}

//	public int getNrNNominals() {};
//	public int getNrNumerics() {};
//	public int getNrBinaries() {};

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

	public Table(Node theTableNode)
	{
		/*
		private String itsName = "no tablename";
		private String itsSource = "no known source";
		private int itsNrRows;
		private int itsNrColumns;
		private ArrayList<Attribute> itsAttributes = new ArrayList<Attribute>();
		private ArrayList<Column> itsColumns = new ArrayList<Column>();
		*/
		NodeList aChildren = theTableNode.getChildNodes();
		for(int i = 0, j = aChildren.getLength(); i < j; ++i)
		{
			Node aSetting = aChildren.item(i);
			String aNodeName = aSetting.getNodeName();
			if("name".equalsIgnoreCase(aNodeName))
				itsName = aSetting.getTextContent();
			else if("source".equalsIgnoreCase(aNodeName))
				itsSource = aSetting.getTextContent();
			else if("nr_rows".equalsIgnoreCase(aNodeName))
				itsNrRows = Integer.valueOf(aSetting.getTextContent());
			else if("nr_columns".equalsIgnoreCase(aNodeName))
				itsNrColumns = Integer.valueOf(aSetting.getTextContent());
			else if("column".equalsIgnoreCase(aNodeName))
				itsColumns.add(new Column(aSetting));
		}
	}

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
		Attribute aFirstAttribute = itsAttributes.get(0);
		if (aFirstAttribute.isNumericType())
			return new Condition(aFirstAttribute, Condition.FIRST_NUMERIC_OPERATOR);
		else
			return new Condition(aFirstAttribute, Condition.FIRST_NOMINAL_OPERATOR);
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
			{
				Attribute anAttribute = itsAttributes.get(anIndex+1);
				if (anAttribute.isNumericType())
					aCondition = new Condition(anAttribute, Condition.FIRST_NUMERIC_OPERATOR);
				else
					aCondition = new Condition(anAttribute, Condition.FIRST_NOMINAL_OPERATOR);
			}
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
		Subgroup aSubgroup = new Subgroup(0, theSize, 0, null);
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

	public void addNodeTo(Node theParentNode)
	{
		Node aNode = XMLNode.addNodeTo(theParentNode, "table");
		XMLNode.addNodeTo(aNode, "name", itsName);
		XMLNode.addNodeTo(aNode, "source", itsSource);
		XMLNode.addNodeTo(aNode, "nr_rows", itsNrRows);
		XMLNode.addNodeTo(aNode, "nr_columns", itsNrColumns);

		for(int i = 0, j = itsColumns.size(); i < j; ++i)
		{
			itsColumns.get(i).addNodeTo(aNode);
			((Element)aNode.getLastChild()).setAttribute("id", String.valueOf(i));
		}

	}

}
