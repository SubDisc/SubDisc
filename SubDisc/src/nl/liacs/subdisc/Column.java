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

	private static final String falseFloat = "[-+]?0*(\\.0+)?";
	private static final String trueFloat = "\\+?0*1(\\.0+)?";
	private static final String trueInteger = "[-+]?\\d+(\\.0+)?";

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

	/**
	 * Creates a copy of the current column with some records removed.
	 * @param theSet
	*/
	public Column select(BitSet theSet)
	{
		int aColumnsSize = theSet.cardinality();
		Column aColumn = new Column(itsAttribute, aColumnsSize);

		switch(itsAttribute.getType())
		{
			case NOMINAL :
			{
				aColumn.itsNominals = new ArrayList<String>(aColumnsSize);
				//preferred way to loop over BitSet (itsSize for safety)
				for (int i = theSet.nextSetBit(0); i >= 0 && i < itsSize; i = theSet.nextSetBit(i + 1))
					aColumn.itsNominals.add(getNominal(i));
				break;
			}
			case NUMERIC :
			case ORDINAL :
			{
				aColumn.itsFloats = new ArrayList<Float>(aColumnsSize);
				for (int i = theSet.nextSetBit(0); i >= 0 && i < itsSize; i = theSet.nextSetBit(i + 1))
					aColumn.itsFloats.add(getFloat(i));
				break;
			}
			case BINARY :
			{
				aColumn.itsBinaries = new BitSet(itsSize);
				for (int i = theSet.nextSetBit(0), j = 0; i >= 0 && i < itsSize; i = theSet.nextSetBit(i + 1))
					aColumn.itsBinaries.set(j++ , getBinary(i));
				break;
			}
			default : logTypeError("Column.select()"); break;
		}
		return aColumn;
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
	// TODO throws IndexOutOfBoundsException
	public void set(int theIndex, float theValue)
	{
		itsFloats.set(theIndex, theValue);
	}
	public void set(int theIndex, String theValue)
	{
		itsNominals.set(theIndex, theValue);
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
			default : logTypeError("Column.getString()");
						return ("Unknown type: " + itsAttribute.getTypeName());
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
		if (itsMax == Float.NEGATIVE_INFINITY) //never computed yet?
			for (int i=0; i<itsSize; i++)
			{
				float aValue = getFloat(i);
				if (aValue > itsMax)
					itsMax = aValue;
				if (aValue < itsMin)
					itsMin = aValue;
			}
	}

	public void permute(int[] thePermutation)
	{
		switch (itsAttribute.getType())
		{
			case NOMINAL :
				ArrayList<String> aNominals = new ArrayList<String>(thePermutation.length);
				for (int i : thePermutation)
					aNominals.add(itsNominals.get(i));
				itsNominals = aNominals;
				break;
			case NUMERIC :
			case ORDINAL :
				ArrayList<Float> aFloats = new ArrayList<Float>(thePermutation.length);
				for (int i : thePermutation)
					aFloats.add(itsFloats.get(i));
				itsFloats = aFloats;
				break;
			case BINARY :
				int n = thePermutation.length;
				BitSet aBinaries = new BitSet(n);
				for (int i=0; i<n; i++)
					aBinaries.set(i, itsBinaries.get(thePermutation[i]));
				itsBinaries = aBinaries;
				break;
			default : logTypeError("Column.permute()"); break;
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
	 * {@link Attribute Attribute}.
	 * @param theAttributeType a valid AttributeType.
	 * @return <code>true</code> if the change was successful,
	 * <code>false</code> otherwise.
	 */
	public boolean setType(AttributeType theAttributeType)
	{
		if (theAttributeType == null)
			return false;
		else if (itsAttribute.getType().equals(theAttributeType))
			return true;
		else
		{
			/*
			 * getAttributeType() always returns an AttributeType, even if
			 * theType cannot be resolved. So null checks are not needed in the
			 * (private) toNewType() methods.
			 */
			switch (theAttributeType)
			{
				case NOMINAL : return toNominalType();
				case NUMERIC :
				case ORDINAL : return toNumericType(theAttributeType);
				case BINARY : return toBinaryType();
				default : logTypeError("Column.setType()"); return false;
			}
		}
	}

	/*
	 * The AttributeType of a Column can always be changed to NOMINAL.
	 * The old itsMissingValue can always be used as itsMissingValue, and will
	 * not be converted to AttributeType.NOMINAL.DEFAULT_MISSING_VALUE.
	 */
	private boolean toNominalType()
	{
		switch (itsAttribute.getType())
		{
			case NOMINAL : break;	// should not happen
			case NUMERIC :
			case ORDINAL :
			{
				itsNominals = new ArrayList<String>(itsSize);
				int aNrTrueIntegers = 0;

				// 2 loops over itsFloats to check if all are actually integers
				for (Float f : itsFloats)
					if (Float.toString(f).matches(trueInteger))
						++aNrTrueIntegers ;

				if (aNrTrueIntegers == itsSize)
				{
					for (Float f : itsFloats)
						itsNominals.add(String.valueOf(f.intValue()));

					// no missing values or itsMissingValue is a true Integer
					itsMissingValue = String.valueOf(Float.valueOf(itsMissingValue).intValue());
				}
				else
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
				itsNominals= new ArrayList<String>(Collections.nCopies(itsSize, "0"));
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
	 * AttributeType.theNewType.DEFAULT_MISSING_VALUE.
	 */
	private boolean toNumericType(AttributeType theNewType)
	{
		switch (itsAttribute.getType())
		{
			case NOMINAL :
			{
				itsFloats = new ArrayList<Float>(itsSize);
				for (String s : itsNominals)
				{
					// TODO '?' could be caught here and replaced by DEFAULT_MISSING_VALUE
					// complicates cardinality logic
					try { itsFloats.add(Float.valueOf(s)); }
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
				 * itsSize == 0). Cleanup (for GarbageCollector).
				 */
				itsNominals = null;

				if (itsMissing.cardinality() == 0 && !isValidValue(theNewType, itsMissingValue))
				{
					itsMissingValue = theNewType.DEFAULT_MISSING_VALUE;
					// check could be done in for-loop above
					itsMissingValueIsUnique = !itsFloats.contains(Float.valueOf(itsMissingValue));
				}
				/*
				 * else old ArrayList<?> contained only valid Floats, also for
				 * missing values, we a Float version of itsMissingValue
				 */
				else
					itsMissingValue = Float.valueOf(itsMissingValue).toString();
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
				itsFloats = new ArrayList<Float>(Collections.nCopies(itsSize, 0.0f));

				for (int i = itsBinaries.nextSetBit(0); i >= 0; i = itsBinaries.nextSetBit(i + 1))
					itsFloats.set(i, 1.0f);

				itsBinaries = null;
				itsMissingValue = Float.valueOf(itsMissingValue).toString();
				break;
			}
			default : logTypeError("Column.toNumericType()"); return false;
		}

		return itsAttribute.setType(theNewType);
	}

	/*
	 * If there are only two distinct values for a Column with a
	 * NUMERIC/ORDINAL/NOMINAL AttributeType, its type can be changed to BINARY.
	 * If the values in the old Column evaluate to 'true' for
	 * (AttributeType.isValidBinaryTrueValue(itsMissingValue) ||
	 * AttributeType.isValidBinaryFalseValue(itsMissingValue)), the Bits in the
	 * new BisSet will be set accordingly. If one of the two classes is '?'
	 * (AttributeType.NOMINAL.DEFAULT_MISSING_VALUE), the corresponding bits
	 * will be set using AttributeType.BINARY.DEFAULT_MISSING_VALUE.
	 * The value of the first value in itsFloats/itsNominals will serve as the
	 * 'true' case, meaning the bits in the new itsBinary BitSet will be set
	 * ('true') for all instances having that value, all others will be 'false'.
	 * The old itsMissingValue can only be used if
	 * (AttributeType.isValidBinaryTrueValue(itsMissingValue) ||
	 * AttributeType.isValidBinaryFalseValue(itsMissingValue)) evaluates 'true'.
	 * In that case the itsMissingValue will be set to the '0'/'1' equivalent of
	 * itsMissingValue, else itsMissingValue will be set to
	 * AttributeType.BINARY.DEFAULT_MISSING_VALUE. This will also happen if the
	 * old itsMissingValue is the DEFAULT_MISSING_VALUE for the current
	 * AttributeType.
	 */
	private boolean toBinaryType()
	{
		if (getCardinality() > 2 || getCardinality() < 0)
			return false;
		else
		{
			switch (itsAttribute.getType())
			{
				case NOMINAL :
				{
					itsBinaries = new BitSet(itsSize);

					if (itsSize > 0)
					{
						String aValue = itsNominals.get(0);

						if (AttributeType.isValidBinaryTrueValue(aValue))
						{
							// All false initially, only set 'true' bits.
							for (int i = 0; i < itsSize; i++)
								if (aValue.equals(itsNominals.get(i)))
									itsBinaries.set(i);
						}
						else if (AttributeType.isValidBinaryFalseValue(aValue))
						{
							// All false initially, only set 'true' bits.
							for (int i = 0; i < itsSize; i++)
								if (!aValue.equals(itsNominals.get(i)))
									itsBinaries.set(i);
						}
						// TODO ask user which value to use as 'true'
						// now sets all non-missing to 'true'
						else if (AttributeType.NOMINAL.DEFAULT_MISSING_VALUE.equals(aValue))
						{
							if ((itsMissing.cardinality() < itsSize) && AttributeType.isValidBinaryTrueValue(itsNominals.get(itsMissing.nextClearBit(0))))
								// All false initially, only set non-missing values to 'true'.
								for (int i = itsMissing.nextClearBit(0); i >= 0 && i < itsSize; i = itsMissing.nextClearBit(i + 1))
									itsBinaries.set(i);
						}
						// TODO ask user which value to use as 'true'
						// now sets all itsNominals.get(0) values to 'true'
						else
						{
							// All false initially, only set 'true' bits.
							for (int i = 0; i < itsSize; i++)
								if (aValue.equals(itsNominals.get(i)))
									itsBinaries.set(i);
						}
					}
					itsNominals = null;
					break;
				}
				case NUMERIC :
				case ORDINAL :
				{
					/*
					 * NOTE (+0.0f).equals(-0.0f) returns false by definition
					 * while +0.0f == -0.0f return true by definition
					 * (Float.NaN).equals(Float.NaN) returns true by definition
					 * while Float.NaN == Float.NaN return false by definition
					 */
					itsBinaries = new BitSet(itsSize);

					if (itsSize > 0)
					{
						float aValue = itsFloats.get(0).floatValue();

						if (aValue == 1f)
						{
							// All false initially, only set 'true' bits.
							for (int i = 0; i < itsSize; i++)
								if (itsFloats.get(i).floatValue() == 1f)
									itsBinaries.set(i);
						}
						else if (aValue == 0f)
						{
							// All false initially, only set 'true' bits.
							for (int i = 0; i < itsSize; i++)
								if (itsFloats.get(i) != 0f)
									itsBinaries.set(i);
						}
						// TODO ask user which value to use as 'true'
						// now sets all non-missing to 'true'
						else if (Float.parseFloat(itsMissingValue) == aValue)	// && &&
						{
							if (itsMissing.cardinality() < itsSize)
							{
								if (itsFloats.get(itsMissing.nextClearBit(0)).toString().matches(trueFloat))
									// All false initially, only set non-missing values to 'true'.
									for (int i = itsMissing.nextClearBit(0); i >= 0 && i < itsSize; i = itsMissing.nextClearBit(i + 1))
										itsBinaries.set(i);
							}
						}
						// TODO ask user which value to use as 'true'
						// now sets NaN to 'false'
						else if (Float.isNaN(aValue))
						{
							// All false initially, only set 'true' bits.
							for (int i = 0; i < itsSize; i++)
								if (!itsFloats.get(i).isNaN())
										itsBinaries.set(i);
						}
						// TODO ask user which value to use as 'true'
						// now sets all itsFloats.get(0) values to 'true'
						else
						{
							// All false initially, only set 'true' bits.
							for (int i = 0; i < itsSize; i++)
								if (itsFloats.get(i).floatValue() == aValue)
									itsBinaries.set(i);
						}
					}
					itsFloats = null;
					break;
				}
				case BINARY : break;	// should not happen
				default : logTypeError("Column.toBinaryType()"); return false;
			}
			try
			{
				// always change itsMissingValue to "0" or "1"
				if (Float.parseFloat(itsMissingValue) == 0f)
					itsMissingValue = "0";
				else if (Float.parseFloat(itsMissingValue) == 1f)
					itsMissingValue = "1";
				else if (itsAttribute.getType().DEFAULT_MISSING_VALUE.equals(itsMissingValue))
					itsMissingValue = AttributeType.BINARY.DEFAULT_MISSING_VALUE;
			}
			catch (NumberFormatException anException) {}

			if (isValidValue(AttributeType.BINARY, itsMissingValue))
			{
				itsMissingValue =
					AttributeType.isValidBinaryTrueValue(itsMissingValue) ? "1" : "0";
			}
			else
			{
				itsMissingValue = AttributeType.BINARY.DEFAULT_MISSING_VALUE;

				// BINARY.DEFAULT_MISSING_VALUE is "0", but could be "1"
				if ("0".equals(itsMissingValue))
				{
					for (int i = itsBinaries.nextClearBit(0); i >= 0 && i < itsSize; i = itsBinaries.nextClearBit(i + 1))
					{
						if (!itsMissing.get(i))
						{
							itsMissingValueIsUnique = false;
							break;
						}
					}
				}
				else
				{
					for (int i = itsBinaries.nextSetBit(0); i >= 0; i = itsBinaries.nextSetBit(i + 1))
					{
						if (!itsMissing.get(i))
						{
							itsMissingValueIsUnique = false;
							break;
						}
					}
				}
			}

			// check if BINARY.DEFAULT_MISSING_VALUE == 0 or 1 (should be 0)
			if (!"0".equals(AttributeType.BINARY.DEFAULT_MISSING_VALUE))
				for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
					itsBinaries.set(i);

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
	 * Returns whether this Column is has missing values or not.
	 *
	 * @return <code>true</code> if this Column has missing values
	 * <code>false</code> otherwise.
	 */
	public boolean getHasMissingValues() { return itsMissing.cardinality() > 0; }

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
		if (itsMissing.cardinality() == 0)
			return "";
		else
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
		else if (isValidValue(itsAttribute.getType(), theNewValue))
		{
			switch (itsAttribute.getType())
			{
				case NOMINAL :
				{
					itsMissingValue = theNewValue;
					updateCardinality(itsNominals);
					for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
						itsNominals.set(i, itsMissingValue);
					return true;
				}
				case NUMERIC :
				case ORDINAL :
				{
					itsMissingValue = Float.valueOf(theNewValue).toString();
					updateCardinality(itsFloats);
					Float aNewValue = Float.valueOf(itsMissingValue);
					for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
						itsFloats.set(i, aNewValue);
					return true;
				}
				case BINARY :
				{
					itsMissingValue =
						(AttributeType.isValidBinaryTrueValue(theNewValue) ? "1" : "0");
					updateCardinality();
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

	private boolean isValidValue(AttributeType theAttributeType, String theNewValue)
	{
		switch(theAttributeType)
		{
			case NOMINAL : return true;
			case NUMERIC :
			case ORDINAL :
			{
				// TODO could allow '?'
				try { Float.parseFloat(theNewValue); return true; }
				catch (NumberFormatException anException) { return false; }
			}
			case BINARY :
			{
				// TODO could allow '?'
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
	 * It is recommended to run this function after all data is loaded into a
	 * Column, as correct counts depend on the unmodified original data.
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

						itsMissingValueIsUnique = true;
						return itsCardinality;
					}
					// expect high itsCardinality/itsSize ratio
					// Arrays.sort = O(n*log(n))
					case NUMERIC:
					case ORDINAL :
					{
						float aMissingValue = Float.valueOf(itsAttribute.getType().DEFAULT_MISSING_VALUE).floatValue();
						for (int i = 0; i < itsSize; i++)
						{
							if (itsFloats.get(i).floatValue() == aMissingValue && !itsMissing.get(i))
							{
								itsMissingValueIsUnique = false;
								break;
							}
						}

						return new TreeSet<Float>(itsFloats).size();
					}
					case BINARY :
					{
						// BINARY.DEFAULT_MISSING_VALUE is "0", but could be "1"
						if ("0".equals(itsAttribute.getType().DEFAULT_MISSING_VALUE))
						{
							for (int i = itsBinaries.nextClearBit(0); i >= 0 && i < itsSize; i = itsBinaries.nextClearBit(i + 1))
							{
								if (!itsMissing.get(i))
								{
									itsMissingValueIsUnique = false;
									break;
								}
							}
						}
						else
						{
							for (int i = itsBinaries.nextSetBit(0); i >= 0; i = itsBinaries.nextSetBit(i + 1))
							{
								if (!itsMissing.get(i))
								{
									itsMissingValueIsUnique = false;
									break;
								}
							}
						}

						int setBits = itsBinaries.cardinality();
						return (setBits > 0 && setBits < itsSize) ? 2 : 1;
					}
					default : logTypeError("Column.getNrDistinct()"); return 0;
				}
			}
			else
				return itsCardinality;
		}
	}

	// parse(itsMissingValue) for itsFloats
	private void updateCardinality(ArrayList<?> theColumnData)
	{
		// not set yet, or no data
		if (itsCardinality == 0)
			getCardinality();
		else
		{
			Object aValue;
			if (itsAttribute.getType().equals(AttributeType.NOMINAL))
				aValue = itsMissingValue;
			else
				aValue = Float.parseFloat(itsMissingValue);

			if (itsMissingValueIsUnique)
			{
				if (theColumnData.contains(aValue))
				{
					--itsCardinality;
					itsMissingValueIsUnique = false;
				}
			}
			else
			{
				if (!theColumnData.contains(aValue))
				{
					++itsCardinality;
					itsMissingValueIsUnique = true;
				}
			}
		}
	}

	private void updateCardinality()
	{
		// not set yet, or no data
		if (itsCardinality == 0)
			getCardinality();
		else
		{
			if ("0".equals(itsMissingValue))
			{
				for (int i = itsBinaries.nextClearBit(0); i >= 0 && i < itsSize; i = itsBinaries.nextClearBit(i + 1))
				{
					if (!itsMissing.get(i))
					{
						itsMissingValueIsUnique = false;
						break;
					}
				}
			}
			else
			{
				for (int i = itsBinaries.nextSetBit(0); i >= 0; i = itsBinaries.nextSetBit(i + 1))
				{
					if (!itsMissing.get(i))
					{
						itsMissingValueIsUnique = false;
						break;
					}
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

