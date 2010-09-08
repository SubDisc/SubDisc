package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.Attribute.*;

import org.w3c.dom.*;

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
				itsMissingValue = String.valueOf(0.0f);
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
	 */
	public boolean setType(String theType)
	{

		if (itsAttribute.getTypeName().equals(theType))
			return true;
		else
		{
			AttributeType aNewType = AttributeType.getAttributeType(theType);
			switch (itsAttribute.getType())
			{
				case NUMERIC :
				case ORDINAL :
				{
					switch (aNewType)
					{
						case NUMERIC :
						case ORDINAL : return toNumericType(aNewType);
						case NOMINAL : return toNominalType();
						case BINARY : return toBinaryType();
						default : return false;
					}
				}
				case NOMINAL :
				{
					switch (aNewType)
					{
						case NUMERIC : 
						case ORDINAL : return toNumericType(aNewType);
						case NOMINAL : return true;	// not strictly needed
						case BINARY : return toBinaryType();
						default : return false;
					}
				}
				case BINARY :
				{
					switch (aNewType)
					{
						case NUMERIC :
						case ORDINAL : return toNumericType(aNewType);
						case NOMINAL : return toNominalType();
						case BINARY : return true;	// not strictly needed
						default : return false;
					}
				}
				default : return false;
			}
		}
	}

	/*
	 * Switching between Column AttributeTypes of NUMERIC and ORDINAL is always
	 * possible, without any other further changes. Changing from a BINARY
	 * AttributeType is also possible. Changing from a NOMINAL AttributeType is
	 * only possible is all values in itsNominal can be parsed ad Floats.
	 */
	private boolean toNumericType(AttributeType theType)
	{
		if (theType == null)
			return false;
		else
		{
			boolean aValidNewType = false;
			switch (itsAttribute.getType())
			{
				case NUMERIC :
				case ORDINAL : aValidNewType = itsAttribute.setType(theType); break;
				case NOMINAL :
				{
					itsFloats = new ArrayList<Float>(itsNominals.size());
					for (String s : itsNominals)
					{
						try { Float.valueOf(s); }
						catch (NumberFormatException e)
						{
							itsFloats = null;
							return false;
						}
					}
					// If here all values are parsed succesfully
					itsNominals = null;
					aValidNewType = true;
					break;
				}
				case BINARY :
				{
					// TODO
				}
				default : return false;
			}
			return aValidNewType;
		}
	}

	/*
	 * The AttributeType of a Column can always be changed to NOMINAL.
	 */
	private boolean toNominalType()
	{
		boolean aValidNewType = false;
		switch (itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL :
			{
				itsNominals = new ArrayList<String>(itsFloats.size());
				for (Float f : itsFloats)
					itsNominals.add(Float.toString(f));
				itsFloats = null;
				aValidNewType = true;
				break;
			}
			case NOMINAL : aValidNewType = true; break;	// should not happen
			case BINARY :
			{
				// Initialise to "0", then update only set bits in itsBitSet.
				itsNominals= new ArrayList<String>(Collections.nCopies(itsBinaries.size(), "0"));
				for (int i = itsBinaries.nextSetBit(0); i >= 0; i = itsBinaries.nextSetBit(i + 1))
					itsNominals.set(i, "1");
				itsBinaries = null;
				aValidNewType = true;
				break;
			}
			// itsAttribute.getType() unknown TODO throw warning
			default : aValidNewType = false; break;
		}
		itsAttribute.setType(AttributeType.NOMINAL);
		return aValidNewType;
	}

	/*
	 * If there are only two distinct values for a Column with a 
	 * NUMERIC/ORDINAL/NOMINAL AttributeType, its type can be changed to BINARY.
	 * The value of the first value in itsFloats/itsNominal will serve as the
	 * 'true' case, meaning the bits in the new itsBinary BitSet will be set for
	 * all instances having that value.
	 */
	private boolean toBinaryType()
	{
		if (getNrDistinct() != 2)
			return false;
		else
		{
			boolean aValidNewType = false;
			switch (itsAttribute.getType())
			{
				case NUMERIC :
				case ORDINAL :
				{
					itsBinaries = new BitSet(itsFloats.size());
					Float aTrue = itsFloats.get(0);
					// All false initially, only set 'true' bits.
					for (int i = 0, j = itsFloats.size(); i < j; i++)
						if (aTrue == itsFloats.get(i))
							itsBinaries.set(i);
					itsFloats = null;
					aValidNewType = true;
					break;
				}
				case NOMINAL :
				{
					itsBinaries = new BitSet(itsNominals.size());
					String aTrue = itsNominals.get(0);
					// All false initially, only set 'true' bits.
					for (int i = 0, j = itsNominals.size(); i < j; i++)
						if (aTrue.equals(itsNominals.get(i)))
								itsBinaries.set(i);
					itsNominals = null;
					aValidNewType = true;
					break;
				}
				case BINARY : aValidNewType = true; break;	// should not happen
				// itsAttribute.getType() unknown TODO throw warning
				default : aValidNewType = false; break;
			}
			itsAttribute.setType(AttributeType.BINARY);
			return aValidNewType;
		}
	}

	public boolean getIsEnabled() { return isEnabled; }
	public void setIsEnabled(boolean theSetting) { isEnabled = theSetting; }
	/**
	 * Returns a <b>copy of</b> a BitSet representing the missing values for
	 * this Column, those rows that contained a '?' value in the original
	 * data. NOTE: use {@link #setMissing(int) setMissing} to set missing values
	 * . Editing on the BitSet retrieved through getMissing() has no effect on 
	 * the original missing values BitSet.
	 * @return a clone of this columns itsMissing BitSet
	 */
	public BitSet getMissing() { return (BitSet) itsMissing.clone(); } 
	public void setMissing(int theIndex) { itsMissing.set(theIndex); }
	// TODO make it private and put check into setNewMissingValue()
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

	/*
	 * NOTE This may not be the fastest implementation, but it avoids HashSets'
	 * hashCode() clashes.
	 */
	/**
	 * Counts the number of distinct values, or cardinality, for this Column.
	 * @return The number of distinct values.
	 */
	public int getNrDistinct()
	{
		switch (getType())
		{
			case NUMERIC:
			case ORDINAL : return new TreeSet<Float>(itsFloats).size();
			case NOMINAL : return new TreeSet<String>(itsNominals).size();
			case BINARY : return 2;
			default : return 0;	// TODO throw warning
		}
	}

	/**
	 * Create a XML Node representation of this Column.
	 * Note: the value for missing values is included as missing_value, as
	 * described by itsMissingValue. When data is loaded, '?' values are
	 * replaced by itsMissingValue by the FileLoader. Default values for the
	 * AttributeTypes are: 0.0f for NUMERIC/ORDINAL, 0 for BINARY
	 * (indicating false) and "" for NOMINAL (the empty string).
	 * @param theParentNode the Node of which this Node will be a ChildNode.
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
