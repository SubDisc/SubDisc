package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.Attribute.*;

import org.w3c.dom.*;

/**
 * A Column contains all data from a column read from a <code>File</code> or
 * database. Its members store some of the important characteristics of the
 * Column. A Column is identified by its [@link Attribute Attribute}. One
 * important member stores the value for data that was missing (having a value
 * of '?') in the original data. See {@link AttributeType AttributeType} for the
 * default values for the various AttributeTypes.
 */
public class Column implements XMLNodeInterface
{
	private static final int DEFAULT_INIT_SIZE = 1000;

	// when adding/removing members be sure to update addNodeTo() and loadNode()
	private Attribute itsAttribute;
	private ArrayList<Float> itsFloats;
	private ArrayList<String> itsNominals;
	private BitSet itsBinaries;
	private String itsMissingValue;
	private BitSet itsMissing = new BitSet();
	private boolean itsMissingValueIsUnique = true;
	private int itsSize = 0;
	private int itsCardinality = 0;
	private float itsMin = Float.POSITIVE_INFINITY;
	private float itsMax = Float.NEGATIVE_INFINITY;
	private boolean isEnabled = true;

	/**
	 * 
	 * @param theAttribute
	 */
	public Column(Attribute theAttribute)
	{
		itsAttribute = theAttribute;
		setupColumn(DEFAULT_INIT_SIZE);
	}

	/**
	 * 
	 * @param theAttribute
	 */

	public Column(Attribute theAttribute, int theNrRows)
	{
		itsAttribute = theAttribute;
		setupColumn(theNrRows);
	}

	/**
	 * 
	 * @param theColumnNode
	 */
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
		setupColumn(DEFAULT_INIT_SIZE);
	}

	private void setupColumn(int theNrRows)
	{
		switch(itsAttribute.getType())
		{
			case NOMINAL :
			{
				itsNominals = new ArrayList<String>(theNrRows);
				itsMissingValue = AttributeType.NOMINAL.DEFAULT_MISSING_VALUE;
				break;
			}
			case NUMERIC :
			{
				itsFloats = new ArrayList<Float>(theNrRows);
				itsMissingValue = AttributeType.NUMERIC.DEFAULT_MISSING_VALUE;
				break;
			}
			case ORDINAL :
			{
				itsFloats = new ArrayList<Float>(theNrRows);
				itsMissingValue = AttributeType.ORDINAL.DEFAULT_MISSING_VALUE;
				break;
			}
			case BINARY :
			{
				itsBinaries = new BitSet(theNrRows);
				itsMissingValue = AttributeType.BINARY.DEFAULT_MISSING_VALUE;
				break;
			}
			default : logTypeError("Column() Constructor"); break;
		}
	}

	public void add(float theFloat) { itsFloats.add(new Float(theFloat)); itsSize++; }
	public void add(boolean theBinary)
	{
		if (theBinary)
			itsBinaries.set(itsSize);
		itsSize++;
	}
	public void add(String theNominal)
	{
		itsNominals.add(theNominal == null ? "" : theNominal);
		itsSize++;
	}
	public int size() { return itsSize; }
	public Attribute getAttribute() { return itsAttribute; }	// TODO return copy of mutable type
	public AttributeType getType() { return itsAttribute.getType(); }
	public String getName() {return itsAttribute.getName(); }
	// TODO these methods should all check for ArrayIndexOutOfBounds Exceptions
	public float getFloat(int theIndex) { return itsFloats.get(theIndex).floatValue(); }
	public String getNominal(int theIndex) { return itsNominals.get(theIndex); }
	public boolean getBinary(int theIndex) { return itsBinaries.get(theIndex); }
	public String getString(int theIndex)
	{
		switch (itsAttribute.getType())
		{
			case NOMINAL : return getNominal(theIndex);
			case NUMERIC :
			case ORDINAL : return itsFloats.get(theIndex).toString();
			case BINARY : return getBinary(theIndex)?"1":"0";
			default : return ("Unknown type: " + itsAttribute.getTypeName());
		}
	}
	public BitSet getBinaries() { return (BitSet) itsBinaries.clone(); }

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
			case NOMINAL : Log.logCommandLine(itsNominals.toString()); break;
			case NUMERIC :
			case ORDINAL : Log.logCommandLine(itsFloats.toString()); break;
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
	 * {@link Attribute Attribute}. This method is case insensitive.
	 * @param theType a <code>String</code> representing the AttributeType name.
	 * @return <code>true</code> if the change was successful,
	 * <code>false</code> otherwise.
	 */
	public boolean setType(String theType)
	{
		if (theType == null)
			return false;
		else if (itsAttribute.toString().equalsIgnoreCase(theType))
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
				case NOMINAL : return toNominalType();
				case NUMERIC :
				case ORDINAL : return toNumericType(aNewType);
				case BINARY : return toBinaryType();
				default : logTypeError("Column.setType()"); return false;
			}
		}
	}

	/*
	 * The AttributeType of a Column can always be changed to NOMINAL.
	 * The old itsMissingValue can always be used as itsMissingValue.
	 */
	private boolean toNominalType()
	{
		switch (itsAttribute.getType())
		{
			case NOMINAL : break;	// should not happen
			case NUMERIC :
			case ORDINAL :
			{
				itsNominals = new ArrayList<String>(itsFloats.size());
				for (Float f : itsFloats)
					itsNominals.add(Float.toString(f));
				itsFloats = null;
				break;
			}
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
	 * Switching between Column AttributeTypes of NUMERIC and ORDINAL is always
	 * possible, without any other further changes. Changing from a BINARY
	 * AttributeType is also possible. Changing from a NOMINAL AttributeType is
	 * only possible if all values in itsNominals can be parsed as Floats.
	 * The old itsMissingValue can only be used if it can be parsed as Float,
	 * else itsMissingValue will be set to
	 * AttributeType.theType.DEFAULT_MISSING_VALUE.
	 */
	private boolean toNumericType(AttributeType theNewType)
	{
		switch (itsAttribute.getType())
		{
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
			case NUMERIC :
			case ORDINAL : break;	// should not happen
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

		boolean aReturnValue = itsAttribute.setType(theNewType);

		if (!isValidValue(itsMissingValue))
			itsMissingValue = itsAttribute.getType().DEFAULT_MISSING_VALUE;

		return aReturnValue;
	}

	/*
	 * If there are only two distinct values for a Column with a 
	 * NUMERIC/ORDINAL/NOMINAL AttributeType, its type can be changed to BINARY.
	 * The value of the first value in itsFloats/itsNominals will serve as the
	 * 'true' case, meaning the bits in the new itsBinary BitSet will be set
	 * ('true') for all instances having that value, all others will be 'false'.
	 * The old itsMissingValue can only be used if
	 * (AttributeType.isValidBinaryTrueValue(itsMissingValue) ||
	 * AttributeType.isValidBinaryFalseValue(itsMissingValue)) evaluates 'true'.
	 * In that case the itsMissingValue will be set to the '0'/'1' equivalent of
	 * itsMissingValue, else itsMissingValue will be set to
	 * AttributeType.BINARY.DEFAULT_MISSING_VALUE.
	 */
	private boolean toBinaryType()
	{
		if (getCardinality() != 2)
			return false;
		else
		{
			switch (itsAttribute.getType())
			{
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
				case BINARY : break;	// should not happen
				default : logTypeError("Column.toBinaryType()"); return false;
			}

			boolean aReturnValue = itsAttribute.setType(AttributeType.BINARY);

			if (isValidValue(itsMissingValue))
				itsMissingValue =
					AttributeType.isValidBinaryTrueValue(itsMissingValue)
																	? "1" : "0";
			else
				itsMissingValue = AttributeType.BINARY.DEFAULT_MISSING_VALUE;

			return aReturnValue;
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
	 * otherwise.
	 */
	public boolean getIsEnabled() { return isEnabled; }

	/**
	 * Set whether this Column is enabled.
	 * @param theSetting use <code>true</code> to enable this Column, and
	 * <code>false</code> to disable it.
	 */
	public void setIsEnabled(boolean theSetting) { isEnabled = theSetting; }

	/**
	 * Returns a <b>copy of</b> a BitSet representing the missing values for
	 * this Column. Members of this BitSet are set for those values that where
	 * '?' in the original data.
	 * NOTE: use {@link #setMissing setMissing} to set missing values. Editing
	 * on the BitSet retrieved through <code>getMissing</code> has no effect on
	 * the original missing values BitSet.
	 * @return a clone of this Columns' itsMissing BitSet.
	 */
	public BitSet getMissing() { return (BitSet) itsMissing.clone(); }

	/**
	 * Sets the bit at the specified position in the itsMissing BisSet.
	 * @param theIndex the bit to set in the itsMissing BitSet.
	 */
	public void setMissing(int theIndex) { itsMissing.set(theIndex); }

	/**
	 * Retrieves the value currently set for all missing values.
	 * @return the value currently set for all missing values.
	 */
	public String getMissingValue()
	{
		return itsMissingValue;
	}

	/**
	 * Sets the new missing value for this Column. The missing value is used as
	 * replacement value for all values that where '?' in the original data.
	 * @param theNewValue the value to use as new missing value.
	 * @return <code>true</code> if setting the new missing value is successful,
	 * <code>false</code> otherwise.
	 */
	public boolean setNewMissingValue(String theNewValue)
	{
		if (itsMissingValue.equals(theNewValue))
			return true;
		else if (isValidValue(theNewValue))
		{
			itsMissingValue = theNewValue;

			switch (itsAttribute.getType())
			{
				case NOMINAL :
				{
					for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
						itsNominals.set(i, itsMissingValue);
					updateCardinality(itsNominals);
					return true;
				}
				case NUMERIC :
				case ORDINAL :
				{
					Float aNewValue = Float.valueOf(itsMissingValue);
					for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
						itsFloats.set(i, aNewValue);
					updateCardinality(itsFloats);
					return true;
				}
				case BINARY :
				{
					itsMissingValue =
						(AttributeType.isValidBinaryTrueValue(itsMissingValue) ? "1" : "0");
					boolean aNewValue = "0".equals(itsMissingValue);
					for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
						if (aNewValue)
							itsBinaries.clear(i);
						else
							itsBinaries.set(i);
					return true;
				}
				default :
				{
					logTypeError("Column.setNewMissingValue()");
					return false;
				}
			}
		}
		else
			return false;
	}

	private boolean isValidValue(String theNewValue)
	{
		switch(itsAttribute.getType())
		{
			case NOMINAL : return true;
			case NUMERIC :
			case ORDINAL :
			{
				try { Float.parseFloat(theNewValue); return true; }
				catch (NumberFormatException anException) { return false; }
			}
			case BINARY :
			{
				return (AttributeType.isValidBinaryTrueValue(theNewValue) ||
						AttributeType.isValidBinaryFalseValue(theNewValue));
			}
			default : logTypeError("Column.isValidValue()"); return false;
		}
	}

	/*
	 * NOTE This may not be the fastest implementation, but it avoids HashSets'
	 * hashCode() collisions.
	 */
	/**
	 * Counts the number of distinct values, or cardinality, for this Column.
	 * @return the number of distinct values, <code>0</code> when this Column
	 * contains no data.
	 */
	public int getCardinality()
	{
		if (itsSize == 0 || itsSize == 1)
			return itsSize;
		else
		{
			// not set yet
			if (itsCardinality == 0)
			{
				switch (itsAttribute.getType())
				{
					// expect low itsCardinality/itsSize ratio
					// TODO check JLS, how are TreeSets created from ArrayLists?
					case NOMINAL :
					{
						String[] anArray =
									itsNominals.toArray(new String[itsSize]);
						Arrays.sort(anArray);

						// should be safe as (itsSize > 1)
						String aLastValue = anArray[0];
						itsCardinality = 1;

						for (String aValue : anArray)
						{
							if (!aLastValue.equals(aValue))
							{
								aLastValue = (String)aValue;
								++itsCardinality;
							}
						}

						return itsCardinality;
					}
					// expect high itsCardinality/itsSize ratio
					// Arrays.sort = O(n*log(n))
					case NUMERIC:
					case ORDINAL : return new TreeSet<Float>(itsFloats).size();
					case BINARY : return 2;
					default : logTypeError("Column.getNrDistinct()"); return 0;
				}
			}
			else
				return itsCardinality;
		}
	}

	private void updateCardinality(ArrayList<?> theColumnData)
	{
		// not set yet, should not happen
		if (itsCardinality == 0)
			getCardinality();
		else
		{
			if (itsMissingValueIsUnique)
			{
				if (theColumnData.contains(itsMissingValue))
				{
					--itsCardinality;
					itsMissingValueIsUnique = false;
				}
			}
			else
			{
				if (!theColumnData.contains(itsMissingValue))
				{
					++itsCardinality;
					itsMissingValueIsUnique = true;
				}
			}
		}
	}

	/**
	 * Creates an {@link XMLNode XMLNode} representation of this Column.
	 * Note: the value for missing values is included as missing_value. When
	 * data is loaded, '?' values are replaced by missing_value by the
	 * appropriate FileLoader.
	 * @param theParentNode the Node of which this Node will be a ChildNode.
//	 * @return a Node that contains all the information of this column.
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

