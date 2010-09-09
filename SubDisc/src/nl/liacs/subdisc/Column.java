package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.Attribute.*;

import org.w3c.dom.*;

/**
 * A Column contains all data from a column read from a file or database. Its
 * members store some of the important characteristics of a Column. A Column is
 * identified by its [@link Attribute Attribute}. One important member stores
 * the value for data that was missing (having a value of '?') in the original
 * data. Default values for the {@link AttributeType AttributeType}s are: 0.0f
 * for <code>NUMERIC</code>/<code>ORDINAL</code>, 0 for <code>BINARY</code>
 * (indicating <code>false</code>) and "" for <code>NOMINAL</code> (the empty
 * String).
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
			default : logTypeError("Column() Constructor"); break;
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
		switch(itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL : Log.logCommandLine(itsFloats.toString()); break;
			case NOMINAL : Log.logCommandLine(itsNominals.toString()); break;
			case BINARY : Log.logCommandLine(itsBinaries.toString()); break;
			default : logTypeError("Column.print()"); break;
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
	 * Sets a new type for this Column. This is done by changing the
	 * {@link AttributeType AttributeType} of this Columns'
	 * {@link Attribute Attribute}.
	 * @param theType a String with the AttributeType name
	 * @return <code>true</code> if the change was successful,
	 * <code>false</code> otherwise
	 */
	public boolean setType(String theType)
	{
		if (theType == null)
			return false;
		// matches only UPPERCASE
		else if (itsAttribute.toString().equals(theType))
			return true;
		else
		{
			/*
			 * getAttributeType() always returns an AttributeType, even if
			 * theType cannot be resolved. So null checks are not needed in the
			 * (private) toNewType() methods.
			 */
			AttributeType aNewType = AttributeType.getAttributeType(theType);
			switch (aNewType)
			{
				case NUMERIC :
				case ORDINAL : return toNumericType(aNewType);
				case NOMINAL : return toNominalType();
				case BINARY : return toBinaryType();
				default : logTypeError("Column.setType()"); return false;
			}
		}
	}

	/*
	 * Switching between Column AttributeTypes of NUMERIC and ORDINAL is always
	 * possible, without any other further changes. Changing from a BINARY
	 * AttributeType is also possible. Changing from a NOMINAL AttributeType is
	 * only possible if all values in itsNominals can be parsed as Floats.
	 */
	private boolean toNumericType(AttributeType theType)
	{
		switch (itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL : break;
			case NOMINAL :
			{
				itsFloats = new ArrayList<Float>(itsNominals.size());
				for (String s : itsNominals)
				{
					try { Float.valueOf(s); }
					catch (NumberFormatException e)
					{
						/*
						 * If there is a value that can not be parsed as Float:
						 * abort, cleanup (for GarbageCollector) and return.
						 */
						itsFloats = null;
						return false;
					}
				}
				/*
				 * Only gets here if all values are parsed successfully, (or
				 * itsNominals.size() == 0). Cleanup (for GarbageCollector).
				 */
				itsNominals = null;
				break;
			}
			case BINARY :
			{
				/*
				 * Initialise to 0.0f, then update only set bits in itsBitSet.
				 * Cleanup (for GarbageCollector).
				 */
				itsFloats = new ArrayList<Float>(Collections.nCopies(itsBinaries.size(), 0.0f));
				for (int i = itsBinaries.nextSetBit(0); i >= 0; i = itsBinaries.nextSetBit(i + 1))
					itsFloats.set(i, 1.0f);
				itsBinaries = null;
				break;
			}
			default : logTypeError("Column.toNumericType()"); return false;
		}
		return itsAttribute.setType(theType);
	}

	/*
	 * The AttributeType of a Column can always be changed to NOMINAL.
	 */
	private boolean toNominalType()
	{
		switch (itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL :
			{
				itsNominals = new ArrayList<String>(itsFloats.size());
				for (Float f : itsFloats)
					itsNominals.add(Float.toString(f));
				itsFloats = null;
				break;
			}
			case NOMINAL : break;	// should not happen
			case BINARY :
			{
				/*
				 * Initialise to "0", then update only set bits in itsBitSet.
				 * Cleanup (for GarbageCollector).
				 */
				itsNominals= new ArrayList<String>(Collections.nCopies(itsBinaries.size(), "0"));
				for (int i = itsBinaries.nextSetBit(0); i >= 0; i = itsBinaries.nextSetBit(i + 1))
					itsNominals.set(i, "1");
				itsBinaries = null;
				break;
			}
			default : logTypeError("Column.toNominalType()"); return false;
		}
		return itsAttribute.setType(AttributeType.NOMINAL);
	}

	/*
	 * If there are only two distinct values for a Column with a 
	 * NUMERIC/ORDINAL/NOMINAL AttributeType, its type can be changed to BINARY.
	 * The value of the first value in itsFloats/itsNominals will serve as the
	 * 'true' case, meaning the bits in the new itsBinary BitSet will be set
	 * ('true') for all instances having that value, all others will be 'false'.
	 */
	private boolean toBinaryType()
	{
		if (getNrDistinct() != 2)
			return false;
		else
		{
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
					break;
				}
				case BINARY : break;	// should not happen
				default : logTypeError("Column.toBinaryType()"); return false;
			}
			return itsAttribute.setType(AttributeType.BINARY);
		}
	}

	private void logTypeError(String theMethodName)
	{
		Log.logCommandLine(
			String.format("Error in %s: Column '%s' has AttributeType '%s'.",
							theMethodName,
							getName(),
							itsAttribute.getType()));
	}

	/**
	 * Returns whether this Column is enabled.
	 * @return <code>true</code> if this Column is enabled, <code>false</code>
	 * otherwise
	 */
	public boolean getIsEnabled() { return isEnabled; }

	/**
	 * Set whether this Column is enabled.
	 * @param theSetting use <code>true</code> to enable this Column, and <code>
	 * false</code> to disable it
	 */
	public void setIsEnabled(boolean theSetting) { isEnabled = theSetting; }

	/**
	 * Returns a <b>copy of</b> a BitSet representing the missing values for
	 * this Column. Members of this BitSet are set for those values that where
	 * '?' in the original data.
	 * NOTE: use {@link #setMissing setMissing} to set missing values. Editing
	 * on the BitSet retrieved through <code>getMissing</code> has no effect on
	 * the original missing values BitSet.
	 * @return a clone of this Columns' itsMissing BitSet
	 */
	public BitSet getMissing() { return (BitSet) itsMissing.clone(); }

	/**
	 * Sets the bit at the specified position in the itsMissing BisSet.
	 * @param theIndex the bit to set in the itsMissing BitSet
	 */
	public void setMissing(int theIndex) { itsMissing.set(theIndex); }

	/**
	 * Sets the new missing value for this Column. The missing value is used as
	 * replacement value for all values that where '?' in the original data.
	 * @param theNewValue the value to use as new missing value
	 * @return <code>true</code> if setting the new missing value successful,
	 * <code>false</code>otherwise
	 */
	public boolean setNewMissingValue(String theNewValue)
	{
		if (isValidValue(theNewValue))
		{
			for(int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
			{
				switch(getType())
				{
					case NUMERIC :
					// should do Float.valueOf(theNewValue) once outside loop
					case ORDINAL : itsFloats.set(i, Float.valueOf(theNewValue)); break;
					case NOMINAL : itsNominals.set(i, theNewValue); break;
					case BINARY :
					{
						// should do "0".equals(theNewValue) once outside loop
						if("0".equals(theNewValue))
							itsBinaries.clear(i);
						else
							itsBinaries.set(i);
						break;
					}
					default :
					{
						logTypeError("Column.setNewMissingValue()");
						return false;
					}
				}
			}
			return true;
		}
		else
			return false;
	}

	private boolean isValidValue(String theNewValue)
	{
		switch(itsAttribute.getType())
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
				// TODO take it out of FileLoaderARFF and put it into ?
				return (FileLoaderARFF.BOOLEAN_NEGATIVES.contains(theNewValue) ||
						FileLoaderARFF.BOOLEAN_POSITIVES.contains(theNewValue));
			}
			default : logTypeError("Column.isValidValue()"); return false;
		}
	}

	/*
	 * NOTE This may not be the fastest implementation, but it avoids HashSets'
	 * hashCode() clashes.
	 */
	/**
	 * Counts the number of distinct values, or cardinality, for this Column.
	 * @return the number of distinct values
	 */
	public int getNrDistinct()
	{
		switch (itsAttribute.getType())
		{
			case NUMERIC:
			case ORDINAL : return new TreeSet<Float>(itsFloats).size();
			case NOMINAL : return new TreeSet<String>(itsNominals).size();
			case BINARY : return 2;
			default : logTypeError("Column.getNrDistinct()"); return 0;
		}
	}

	/**
	 * Creates an {@link XMLNode XMLNode} representation of this Column.
	 * Note: the value for missing values is included as missing_value, as
	 * described by itsMissingValue. When data is loaded, '?' values are
	 * replaced by itsMissingValue by the FileLoader.
	 * @param theParentNode the Node of which this Node will be a ChildNode
	 * @return a Node that contains all the information of this column
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
