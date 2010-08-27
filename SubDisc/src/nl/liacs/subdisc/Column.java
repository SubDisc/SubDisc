package nl.liacs.subdisc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.TreeSet;

import nl.liacs.subdisc.Attribute.AttributeType;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A Column contains all data from a column read from a file or database. Its
 * members store some of the important characteristics of a Column. A Column is
 * identified by its Attribute.
 */
public class Column implements XMLNodeInterface
{
	// when adding/removing members be sure to update addNodeTo() and loadNode()
	private Attribute itsAttribute;
	private ArrayList<Float> itsFloats;
	private ArrayList<String> itsNominals;
	private BitSet itsBinaries;
	private String itsMissingValue;
	private BitSet itsMissing = new BitSet();
	private int itsSize;
	private float itsMin = Float.POSITIVE_INFINITY;
	private float itsMax = Float.NEGATIVE_INFINITY;
	private boolean isEnabled = true;

	public Column(Attribute theAttribute, int theNrRows)
	{
		itsSize = 0;
		itsAttribute = theAttribute;
		switch(itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL : 
			{
				itsFloats = new ArrayList<Float>(theNrRows);
				itsMissingValue = String.valueOf(0.0F);
				break;
			}
			case NOMINAL :
			{
				itsNominals = new ArrayList<String>(theNrRows);
				itsMissingValue = "";
				break;
			}
			case BINARY :
			{
				itsBinaries = new BitSet(theNrRows);
				itsMissingValue = "0";
				break;
			}
			default :
			{
				itsNominals = new ArrayList<String>(theNrRows);
				itsMissingValue = "";
				break;	// TODO throw warning
			}
		}
	}

	public Column(Node theColumnNode)
	{
		NodeList aChildren = theColumnNode.getChildNodes();
		for (int i = 0, j = aChildren.getLength(); i < j; i++)
		{
			Node aSetting = aChildren.item(i);
			String aNodeName = aSetting.getNodeName();
			if ("attribute".equalsIgnoreCase(aNodeName))
				itsAttribute = new Attribute(aSetting);
			else if ("missing_value".equalsIgnoreCase(aNodeName))
				itsMissingValue = aSetting.getTextContent();
			else if ("enabled".equalsIgnoreCase(aNodeName))
				isEnabled = Boolean.valueOf(aSetting.getTextContent());
		}
	}

	public void add(float theFloat) { itsFloats.add(new Float(theFloat)); itsSize++; }
	public void add(boolean theBinary)
	{
		if (theBinary)
			itsBinaries.set(itsSize);
		itsSize++;
	}
	public void add(String theNominal) { itsNominals.add(theNominal); itsSize++; }
	public int size() { return itsSize; }
	public Attribute getAttribute() { return itsAttribute; }	// TODO return copy of mutable type
	public AttributeType getType() { return itsAttribute.getType(); }
	public String getName() {return itsAttribute.getName(); }
	public float getFloat(int theIndex) { return itsFloats.get(theIndex).floatValue(); }
	public String getNominal(int theIndex) { return itsNominals.get(theIndex); }
	public boolean getBinary(int theIndex) { return itsBinaries.get(theIndex); }
	public String getString(int theIndex)
	{
		switch (itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL : return itsFloats.get(theIndex).toString();
			case NOMINAL : return getNominal(theIndex);
			case BINARY : return getBinary(theIndex)?"1":"0";
			default : return ("Unknown type: " + itsAttribute.getTypeName());
		}
	}
	public BitSet getBinaries() { return itsBinaries; }	// TODO return copy of mutable type

	public boolean isNominalType() { return itsAttribute.isNominalType(); }
	public boolean isNumericType() { return itsAttribute.isNumericType(); }
	public boolean isOrdinalType() { return itsAttribute.isOrdinalType(); }
	public boolean isBinaryType() { return itsAttribute.isBinaryType(); }

	public float getMin()
	{
		updateMinMax();
		return itsMin;
	}

	public float getMax()
	{
		updateMinMax();
		return itsMax;
	}

	private void updateMinMax()
	{
		if(itsMax == Float.NEGATIVE_INFINITY) //never computed?
			for(int i=0; i<itsSize; i++)
			{
				float aValue = getFloat(i);
				if (aValue > itsMax)
					itsMax = aValue;
				if (aValue < itsMin)
					itsMin = aValue;
			}
	}

	public void print()
	{
		Log.logCommandLine((isEnabled ? "Enabled" : "Disbled") + " Column " + this.getName() + ":"); // TODO TEST ONLY
		switch(itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL : Log.logCommandLine(itsFloats.toString()); break;
			case NOMINAL : Log.logCommandLine(itsNominals.toString()); break;
			case BINARY : Log.logCommandLine(itsBinaries.toString()); break;
			default : Log.logCommandLine("Unknown type: " + itsAttribute.getTypeName()); break;
		}
	}

	public TreeSet<String> getDomain()
	{
		TreeSet<String> aResult = new TreeSet<String>();
		if (isBinaryType())
		{
			aResult.add("0");
			aResult.add("1");
			return aResult;
		}

		for (int i=0; i<itsSize; i++)
			if (isNominalType())
				aResult.add(itsNominals.get(i));
			else if (isNumericType())
				aResult.add(Float.toString(itsFloats.get(i)));
			//TODO ordinal?

		return aResult;
	}

	/**
	 * NEW Methods for AttributeType change
	 * TODO update
	 * itsType
	 * (itsTable.)itsAttribute.setType()
	 * itsFloats / itsNominals / itsBinaries
	 * @return 
	 */
	public void setType(String theType) { itsAttribute.setType(theType); }
	public boolean getIsEnabled() { return isEnabled; }
	public void setIsEnabled(boolean theSetting) { isEnabled = theSetting; }
	/**
	 * NOTE use setMissing to set missing values
	 * Editing on the BitSet retrieved through getMissing() has no effect
	 * on the original Columns' itsMissing
	 * @return a clone of this columns itsMissing BitSet
	 */
	public BitSet getMissing() { return (BitSet) itsMissing.clone(); } 
	public void setMissing(int theIndex) { itsMissing.set(theIndex); }
	public boolean isValidValue(String theNewValue)
	{
		switch(getType())
		{
			case NUMERIC :
			case ORDINAL :
			{
				try { Float.parseFloat(theNewValue); return true; }
				catch (NumberFormatException anException) { return false; }
			}
			case NOMINAL : return true;
			case BINARY :
			{
				return (FileLoaderARFF.BOOLEAN_NEGATIVES.contains(theNewValue) ||
						FileLoaderARFF.BOOLEAN_POSITIVES.contains(theNewValue));
			}
			default : return false;
		}
	}
	public void setNewMissingValue(String theNewValue)
	{
		for(int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
		{
			switch(getType())
			{
				case NUMERIC :
				case ORDINAL : itsFloats.set(i, Float.valueOf(theNewValue)); break;
				case NOMINAL : itsNominals.set(i, theNewValue); break;
				case BINARY :
				{
					if("0".equalsIgnoreCase(theNewValue))
						itsBinaries.clear(i);
					else
						itsBinaries.set(i);
					break;
				}
			}
//			System.out.println("set: " + i);
		}
	}

	/**
	 * Create a XML Node representation of this Column.
	 * Note: the value for missing values is included as missing_value, as
	 * described by itsMissingValue. When data is loaded, '?' values are
	 * replaced by itsMissingValue by the FileLoader. Default values for the
	 * AttributeTypes are: 0.0F for NUMERIC/ORDINAL, 0 for BINARY
	 * (indicating false) and "" for NOMINAL (the empty string).
	 * @param theParentNode, the Node of which this Node will be a ChildNode.
	 * @return A Node that contains all the information of this column.
	 */
	@Override
	public void addNodeTo(Node theParentNode)
	{
		Node aNode = XMLNode.addNodeTo(theParentNode, "column");
		((Element)aNode).setAttribute("nr", "0");
		itsAttribute.addNodeTo(aNode);
		XMLNode.addNodeTo(aNode, "missing_value", itsMissingValue);
		XMLNode.addNodeTo(aNode, "enabled", isEnabled);
	}
}
