package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.gui.*;

import org.w3c.dom.*;

/**
 * A Column contains all data from a column read from a <code>File</code> or
 * database. Its members store some of the important characteristics of the
 * Column.
 * A type of a Column is always of one of the available {@link AttributeType}s.
 * One important member stores the value for data that was missing
 * (having a value of '?') in the original data.
 * See {@link AttributeType} for the default missing values for the various
 * AttributeTypes.
 */
public class Column implements XMLNodeInterface
{
	public static final int DEFAULT_INIT_SIZE = 2048;
	private static final int MAP_DEFAULT_INIT_SIZE = DEFAULT_INIT_SIZE / 32;

	// NOTE when adding members, update constructors, copy() and select()
	private AttributeType itsType;
	private String itsName;
	private String itsShort;
	private int itsIndex;

	// when adding/removing members be sure to update addNodeTo() and loadNode()
	private float[] itsFloatz;
//	private List<String> itsNominals;
	private int[] itsNominalz;
	private BitSet itsBinaries;
	// TODO new needs field tests (eg. when switching AttributeTypes)
	// store only references to unique Strings, not individual Strings
	// List is not the most ideal interface, bi-directional map or
	// itsNominals storing Integer indices would be better/ faster/ leaner
	private List<String> itsDistinctValues;
	// for supporting a quicker contains() and get() test
	// Should have the same contents as itsDistinctValues, but unordered.
	// TODO MM see above
	// will be merged with itsDistinctValues, itsNominals will become int[]
	// this allows for (int == int) comparison instead of String.equals()
	// storage tradeoff lies around (n == 16c, plus some implementation type
	// dependent storage requirement
	private Map<String, Integer> itsDistinctValuesMap;

	private String itsMissingValue;
	private BitSet itsMissing = new BitSet();
	private boolean itsMissingValueIsUnique = true;
	private int itsSize = 0;
	private int itsCardinality = 0;
	private float itsMin = Float.POSITIVE_INFINITY;
	private float itsMax = Float.NEGATIVE_INFINITY;
	private boolean isEnabled = true;

//	private static final String falseFloat = "[-+]?0*(\\.0+)?"; // DO NOT REMOVE
	private static final String trueFloat = "\\+?0*1(\\.0+)?";
	private static final String trueInteger = "[-+]?\\d+(\\.0+)?";

	private int itsTargetStatus;
	public static final int NONE      = 0;
	public static final int PRIMARY   = 1;
	public static final int SECONDARY = 2;
	public static final int TERTIARY  = 3;

	public static final int FIRST_TARGET_STATUS = NONE;
	public static final int LAST_TARGET_STATUS = TERTIARY;

	/**
	 * Create a Column with the specified name, short name,
	 * {@link AttributeType}, index and (initial) number of rows.
	 *
	 * Always call {@link #close()} after the last element is
	 * added to this Column.
	 *
	 * @see #add(String)
	 * @see #add(float)
	 * @see #add(boolean)
	 * @see #close()
	 * @see AttributeType
	 */
	public Column(String theName, String theShort, AttributeType theType, int theIndex, int theNrRows)
	{
		if (!isValidIndex(theIndex))
			return;
		else
			itsIndex = theIndex;

		checkAndSetName(theName);
		itsShort = theShort;
		checkAndSetType(theType);

		setupColumn(theNrRows <= 0 ? DEFAULT_INIT_SIZE : theNrRows);

		if (itsType == AttributeType.NUMERIC)
			itsTargetStatus = SECONDARY;
		else
			itsTargetStatus = NONE;
	}

	private boolean isValidIndex(int theIndex)
	{
		boolean isValid = (theIndex >= 0);

		if (!isValid)
			Log.logCommandLine("Column<init>: index can not be < 0. No index set.");

		return isValid;
	}

	private void checkAndSetName(String theName)
	{
		if (theName == null || theName.isEmpty())
		{
			itsName = String.valueOf(System.nanoTime());
			constructorErrorLog("name can not be 'null' or empty. Using: ",
						itsName);
		}
		else
			itsName = theName;
	}

	private void checkAndSetType(AttributeType theType)
	{
		if (theType == null)
		{
			itsType = AttributeType.getDefault();
			constructorErrorLog("type can not be 'null'. Using: ",
						itsType.name());
		}
		else
			itsType = theType;
	}

	private void constructorErrorLog(String theMessage, String theAlternative)
	{
		Log.logCommandLine("Column<init>: " + theMessage + theAlternative);
	}

	//called after removing (domain) columns from Table
	void setIndex(int theIndex)
	{
		if (isValidIndex(theIndex))
			itsIndex = theIndex;
	}

	private void setupColumn(int theNrRows)
	{
		switch (itsType)
		{
			case NOMINAL :
			{
//				itsNominals = new ArrayList<String>(theNrRows);
				itsNominalz = new int[theNrRows];
				itsDistinctValues = new ArrayList<String>();
				itsDistinctValuesMap = new HashMap<String, Integer>(MAP_DEFAULT_INIT_SIZE);
				itsMissingValue = AttributeType.NOMINAL.DEFAULT_MISSING_VALUE;
				return;
			}
			case NUMERIC :
			{
				itsFloatz = new float[theNrRows];
				itsMissingValue = AttributeType.NUMERIC.DEFAULT_MISSING_VALUE;
				return;
			}
			case ORDINAL :
			{
				throw new AssertionError(itsType);
			}
			case BINARY :
			{
				itsBinaries = new BitSet(theNrRows);
				itsMissingValue = AttributeType.BINARY.DEFAULT_MISSING_VALUE;
				return;
			}
			default :
			{
				logTypeError("Column<init>");
				throw new AssertionError(itsType);
			}
		}
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
			if ("type".equalsIgnoreCase(aNodeName))
				itsType = AttributeType.fromString(aSetting.getTextContent());
			else if ("name".equalsIgnoreCase(aNodeName))
				itsName = aSetting.getTextContent();
			else if ("short".equalsIgnoreCase(aNodeName))
				itsShort = aSetting.getTextContent();
			else if ("index".equalsIgnoreCase(aNodeName))
				itsIndex = Integer.parseInt(aSetting.getTextContent());
			else if ("missing_value".equalsIgnoreCase(aNodeName))
				itsMissingValue = aSetting.getTextContent();
			else if ("enabled".equalsIgnoreCase(aNodeName))
				isEnabled = Boolean.valueOf(aSetting.getTextContent());
		}
		setupColumn(DEFAULT_INIT_SIZE);
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
		XMLNode.addNodeTo(aNode, "type", itsType);
		XMLNode.addNodeTo(aNode, "name", itsName);
		XMLNode.addNodeTo(aNode, "short", itsShort == null ? "" : itsShort);
		XMLNode.addNodeTo(aNode, "index", itsIndex);
		XMLNode.addNodeTo(aNode, "missing_value", itsMissingValue);
		XMLNode.addNodeTo(aNode, "enabled", isEnabled);
	}

	public Column copy()
	{
		Column aCopy = new Column(itsName, itsShort, itsType, itsIndex, itsSize);
		aCopy.itsFloatz = itsFloatz;
//		aCopy.itsNominals = itsNominals;
		aCopy.itsNominalz = itsNominalz;
		aCopy.itsBinaries = itsBinaries;
		aCopy.itsDistinctValues = itsDistinctValues;
		aCopy.itsDistinctValuesMap = itsDistinctValuesMap;
		aCopy.itsMissingValue = itsMissingValue;
		aCopy.itsMissing = itsMissing;
		aCopy.itsMissingValueIsUnique = itsMissingValueIsUnique;
		aCopy.itsSize = itsSize; // NOTE not set through constructor
		aCopy.itsCardinality = itsCardinality;
		aCopy.itsMin = itsMin;
		aCopy.itsMax = itsMax;
		aCopy.isEnabled = isEnabled;
		aCopy.itsTargetStatus = itsTargetStatus;

		return aCopy;
	}

	/**
	 * Creates a copy of the current column with some records removed.
	 * <p>
	 * NOTE the new Column is not a true deep-copy.
	 *
	 * @param theSet BitSet indicating which records to use.
	 *
	 * @return a new Column consisting of a selection of the original one.
	*/
	/*
	 * NOTE not all members are copied.
	 *
	 * COPIED:
	 * 	private AttributeType itsType;
	 * 	private String itsName;
	 * 	private String itsShort;
	 * 	private int itsIndex;
	 * 	private float[] itsFloatz;
	 * 	private List<String> itsNominals;
	 * 	private BitSet itsBinaries;
	 * 	private List<String> itsDistinctValues; (shallow copy)
	 * 	private String itsMissingValue;
	 * 	...
	 * 	private int itsSize; (as Column.add() is by-passed completely)
	 * 	...
	 * 	private boolean isEnabled;
	 * 	private int itsTargetStatus;
	 *
	 * NOT COPIED (should be re-assessed after selection):
	 * 	private BitSet itsMissing = new BitSet();
	 * 	private boolean itsMissingValueIsUnique = true;
	 * 	private int itsCardinality = 0;
	 * 	private float itsMin = Float.POSITIVE_INFINITY;
	 * 	private float itsMax = Float.NEGATIVE_INFINITY;
	 */
	public Column select(BitSet theSet)
	{
		int aColumnSize = theSet.cardinality();
		Column aColumn = new Column(itsName, itsShort, itsType, itsIndex, aColumnSize);
		aColumn.itsDistinctValues = this.itsDistinctValues;
		aColumn.itsDistinctValuesMap = this.itsDistinctValuesMap;
		aColumn.itsMissingValue = this.itsMissingValue;
		aColumn.itsSize = aColumnSize;
		aColumn.isEnabled = this.isEnabled;
		aColumn.itsTargetStatus = this.itsTargetStatus;

		switch(itsType)
		{
			case NOMINAL :
			{
//				aColumn.itsNominals = new ArrayList<String>(aColumnSize);
				aColumn.itsNominalz = new int[aColumnSize];
				//preferred way to loop over BitSet (itsSize for safety)
//				for (int i = theSet.nextSetBit(0); i >= 0 && i < itsSize; i = theSet.nextSetBit(i + 1))
//					aColumn.itsNominals.add(getNominal(i));
				for (int i = theSet.nextSetBit(0), j = -1; i >= 0 && i < itsSize; i = theSet.nextSetBit(i + 1))
					aColumn.itsNominalz[++j] = this.itsNominalz[i];
				break;
			}
			case NUMERIC :
			{
				aColumn.itsFloatz = new float[aColumnSize];
				for (int i = theSet.nextSetBit(0), j = -1; i >= 0 && i < itsSize; i = theSet.nextSetBit(i + 1))
					aColumn.itsFloatz[++j] = this.itsFloatz[i];
				break;
			}
			case ORDINAL :
			{
				throw new AssertionError(itsType);
			}
			case BINARY :
			{
				aColumn.itsBinaries = new BitSet(itsSize);
				for (int i = theSet.nextSetBit(0), j = 0; i >= 0 && i < itsSize; i = theSet.nextSetBit(i + 1))
					aColumn.itsBinaries.set(j++ , getBinary(i));
				break;
			}
			default :
			{
				logTypeError("Column.select()");
				throw new AssertionError(itsType);
			}
		}

		// not needed Column.add() is bypassed, but itsSize=aColumnSize
		// aColumn.close();

		return aColumn;
	}

	/**
	 * Appends the specified element to the end of this Column.
	 *
	 * Always call {@link #close()} after the last element is added to this
	 * Column.
	 *
	 * @param theNominal the value to append to this Column.
	 */
//	public void add(String theNominal)
//	{
//		if (theNominal == null)
//			throw new NullPointerException();
//
//		Integer i = itsDistinctValuesMap.get(theNominal);
//		if (i == null)
//		{
//			// String constructor trims of baggage
//			theNominal = new String(theNominal);
//			int size = itsDistinctValues.size();
//			// keep identical sets of values
//			itsDistinctValues.add(theNominal);
//			itsDistinctValuesMap.put(theNominal, size);
//			itsNominals.add(theNominal);
//		}
//		else
//			itsNominals.add(itsDistinctValues.get(i));
//
//		++itsSize;
//	}
	public void add(String theNominal)
	{
		if (theNominal == null)
			throw new NullPointerException();

		if (itsSize == itsNominalz.length)
			itsNominalz = Arrays.copyOf(itsNominalz, itsSize*2);

		Integer i = itsDistinctValuesMap.get(theNominal);
		if (i == null)
		{
			// String constructor trims of baggage
			theNominal = new String(theNominal);
			int size = itsDistinctValues.size();
			// keep identical sets of values
			itsDistinctValues.add(theNominal);
			itsDistinctValuesMap.put(theNominal, size);
			itsNominalz[itsSize] = size;
		}
		else
			itsNominalz[itsSize] = i;

		++itsSize;
	}

	/**
	 * Appends the specified element to the end of this Column.
	 *
	 * Always call {@link #close()} after the last element is added to this
	 * Column.
	 *
	 * @param theFloat the value to append to this Column.
	 */
	public void add(float theFloat)
	{
		if (itsSize == itsFloatz.length)
			itsFloatz = Arrays.copyOf(itsFloatz, itsSize*2);

		itsFloatz[itsSize] = theFloat;
		++itsSize;
	}

	/**
	 * Appends the specified element to the end of this Column.
	 *
	 * Always call {@link #close()} after the last element is added to this
	 * Column.
	 *
	 * @param theBinary the value to append to this Column.
	 */
	public void add(boolean theBinary)
	{
		if (theBinary)
			itsBinaries.set(itsSize);
		++itsSize;
	}

	/**
	 * Always call this method after creating a Column.
	 */
	public void close()
	{
//		if (itsNominals != null)
//			((ArrayList<String>) itsNominals).trimToSize();
		if ((itsNominalz != null) && (itsNominalz.length > itsSize))
			itsNominalz = Arrays.copyOf(itsNominalz, itsSize);
		if (itsDistinctValues != null)
			((ArrayList<String>) itsDistinctValues).trimToSize();
		// trimming is not necessary for itsDistinctValuesMap
		if ((itsFloatz != null) && (itsFloatz.length > itsSize))
			itsFloatz = Arrays.copyOf(itsFloatz, itsSize);
	}

	// package private, for use by FileLoaderGeneRank only
	void set(int theIndex, float theValue)
	{
		if (!isOutOfBounds(theIndex))
			itsFloatz[theIndex] = theValue;
	}

	public int size() { return itsSize; }
	public String getName() { return itsName; }
	public String getShort() { return hasShort() ? itsShort : ""; }
	public boolean hasShort() { return (itsShort != null) ; }
	public String getNameAndShort()
	{
		return itsName + (hasShort() ? " (" + getShort() + ")" : "");
	}
	public AttributeType getType() { return itsType; }
	public int getIndex() { return itsIndex; }	// is never set for MRML
	public String getNominal(int theIndex)
	{
		//return isOutOfBounds(theIndex) ? "" : itsNominals.get(theIndex);
		return isOutOfBounds(theIndex) ? "" : itsDistinctValues.get(itsNominalz[theIndex]);
	}
	public float getFloat(int theIndex)
	{
		return isOutOfBounds(theIndex) ? Float.NaN : itsFloatz[theIndex];
	}
	public boolean getBinary(int theIndex)
	{
		return isOutOfBounds(theIndex) ? false : itsBinaries.get(theIndex);
	}
	public String getString(int theIndex)
	{
		switch (itsType)
		{
			case NOMINAL : return getNominal(theIndex);
			case NUMERIC : return Float.toString(getFloat(theIndex));
			case ORDINAL : throw new AssertionError(itsType);
			case BINARY : return getBinary(theIndex) ? "1" : "0";
			default :
			{
				logTypeError("Column.getString()");
				throw new AssertionError(itsType);
			}
		}
	}

	/**
	 * Return a clone of the binary values of this Column if this Column is
	 * of type {@link AttributeType#BINARY}.
	 *
	 * @return a BitSet, with the same bits set as this Column.
	 *
	 * @throws NullPointerException if this Column is not of type
	 * {@link AttributeType#BINARY}.
	 */
	public BitSet getBinaries() throws NullPointerException
	{
		return (BitSet) itsBinaries.clone();
	}

	/**
	 * Return a copy of the float values of this Column if this Column is
	 * of type {@link AttributeType#NUMERIC}.
	 *
	 * @return a float[], with the same values as this Column.
	 *
	 * @param theBinary the value to append to this Column.
	 * 
	 * @throws NullPointerException if this Column is not of type
	 * {@link AttributeType#NUMERIC}.
	 */
	public float[] getFloats() throws NullPointerException
	{
		return Arrays.copyOf(itsFloatz, itsFloatz.length);
	}

	private boolean isOutOfBounds(int theIndex)
	{
		boolean isOutOfBounds = (theIndex < 0 || theIndex >= itsSize);

		if (isOutOfBounds)
			Log.logCommandLine("indexOutOfBounds: " + theIndex);

		return isOutOfBounds;
	}

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
		//not computed yet?
		if (itsMax == Float.NEGATIVE_INFINITY)
		{
			for (int i=0; i<itsSize; i++)
			{
				float aValue = getFloat(i);
				if (aValue > itsMax)
					itsMax = aValue;
				if (aValue < itsMin)
					itsMin = aValue;
			}
		}
	}

	/**
	 * NOTE this method is destructive. If this Column needs to be restored
	 * to its original state, be sure to make a {@link #copy() backup}
	 * before calling this method.
	 *
	 * @param thePermutation the int[] indicating how to perform the
	 * swap-randomisation.
	 *
	 * @see Table#swapRandomizeTarget(TargetConcept)
	 * @see Validation#getQualities(String[])
	 * @see RandomQualitiesWindow#isValidRandomQualitiesSetup(String[])
	 */
	public void permute(int[] thePermutation)
	{
		switch (itsType)
		{
			case NOMINAL :
//				List<String> aNominals = new ArrayList<String>(thePermutation.length);
//				for (int i : thePermutation)
//					aNominals.add(itsNominals.get(i));
//				itsNominals = aNominals;
				int[] aNominalz = new int[thePermutation.length];
				for (int i = 0, j = thePermutation.length; i < j; ++i)
					aNominalz[i] = itsNominalz[thePermutation[i]];
				itsNominalz = aNominalz;
				break;
			case NUMERIC :
				float[] aFloats = new float[thePermutation.length];
				for (int i = 0, j = thePermutation.length; i < j; ++i)
					aFloats[i] = itsFloatz[thePermutation[i]];
				itsFloatz = aFloats;
				break;
			case ORDINAL :
				throw new AssertionError(itsType);
			case BINARY :
				int n = thePermutation.length;
				BitSet aBinaries = new BitSet(n);
				for (int i : thePermutation)
					aBinaries.set(i, itsBinaries.get(thePermutation[i]));
				itsBinaries = aBinaries;
				break;
			default :
			{
				logTypeError("Column.permute()");
				throw new AssertionError(itsType);
			}
		}
	}

	public void print()
	{
		Log.logCommandLine(itsIndex + ":" + getNameAndShort() + " " + itsType);

		switch(itsType)
		{
//			case NOMINAL : Log.logCommandLine(itsNominals.toString()); break;
			case NOMINAL : Log.logCommandLine(Arrays.toString(itsNominalz)); break;
			case NUMERIC : Log.logCommandLine(Arrays.toString(itsFloatz)); break;
			case ORDINAL : throw new AssertionError(itsType);
			case BINARY : Log.logCommandLine(itsBinaries.toString()); break;
			default :
			{
				logTypeError("Column.print()");
				throw new AssertionError(itsType);
			}
		}
	}

	/**
	 * Sets a new type for this Column. This is done by changing the
	 * {@link AttributeType} of this Column.
	 *
	 * @param theAttributeType a valid AttributeType.
	 *
	 * @return {@code true} if the change was successful, {@code false}
	 * otherwise.
	 */
	public boolean setType(AttributeType theAttributeType)
	{
		// == is preferred on Enums and is null safe
		if (itsType == theAttributeType)
			return true;

		/*
		 * getAttributeType() always returns an AttributeType, even if
		 * theType cannot be resolved. So null checks  are not needed in
		 * the (private) toNewType() methods.
		 */
		switch (theAttributeType)
		{
			case NOMINAL : return toNominalType();
			case NUMERIC : return toNumericType(theAttributeType);
			case ORDINAL : throw new AssertionError(itsType);
			case BINARY : return toBinaryType();
			default :
			{
				logTypeError("Column.setType()");
				throw new AssertionError(theAttributeType);
			}
		}
	}

	/*
	 * The AttributeType of a Column can always be changed to NOMINAL.
	 * The old itsMissingValue can always be used as itsMissingValue, and
	 * will not be converted to AttributeType.NOMINAL.DEFAULT_MISSING_VALUE.
	 */
	private boolean toNominalType()
	{
		assert (itsType != AttributeType.NOMINAL);

		// relies on itsCardinality to be set at this time
		itsDistinctValues = new ArrayList<String>(itsCardinality);
		// warning (itsCardinality * 2) without overflow check is unsafe
		// besides HashMap constructor always uses next power of 2
		itsDistinctValuesMap = new HashMap<String, Integer>(itsCardinality);
//		itsNominals = new ArrayList<String>(itsSize);
		itsNominalz = new int[itsSize];
		itsSize = 0;

		switch (itsType)
		{
			case NOMINAL :
				throw new AssertionError(itsType);
			case NUMERIC :
			{
				int aNrTrueIntegers = 0;

				// 2 loops over itsFloatz to check if all are actually integers
				for (float f : itsFloatz)
					if (Float.toString(f).matches(trueInteger))
						++aNrTrueIntegers ;

				// NOTE uses this.add(String) to populate
				// itsDistinctValues(Map), requires itsSize = 0;
				if (aNrTrueIntegers == itsSize)
				{
					for (int i = 0, j = itsNominalz.length; i < j; ++i)
						this.add(new String(Float.toString(itsFloatz[i]).split(".")[0]));

					// no missing values or itsMissingValue is a true Integer
					itsMissingValue = String.valueOf(Float.valueOf(itsMissingValue).intValue());
				}
				else
					for (int i = 0, j = itsNominalz.length; i < j; ++i)
						this.add(Float.toString(itsFloatz[i]));

				// Cleanup (for GarbageCollector).
				itsFloatz = null;
				break;
			}
			case ORDINAL :
				throw new AssertionError(itsType);
			case BINARY :
			{

// this code is slightly faster, than the code at the bottom as it does not call
// itsDistinctValuesMap.get(String) to determine existence of key
// but switching of Column type is hardly ever done
// and calling add(String) is more robust as it takes care of maintaining sane
// internal state
//				if (itsCardinality == 2)
//				{
//					itsDistinctValues.add("0");
//					itsDistinctValuesMap.put("0", 0);
//					itsDistinctValues.add("1");
//					itsDistinctValuesMap.put("1", 1);
//				}
//				else if (itsBinaries.cardinality() == 0 && itsSize > 0)
//				{
//					itsDistinctValues.add("0");
//					itsDistinctValuesMap.put("0", 0);
//				}
//				else if (itsSize > 0)
//				{
//					itsDistinctValues.add("1");
//					itsDistinctValuesMap.put("1", 0);
//				}
//
//				for (int i = 0, j = itsSize; i < j; ++i)
//					itsNominals.add(itsBinaries.get(i) ? "1" : "0");

				// new code, replaces all above
				String f = AttributeType.DEFAULT_BINARY_FALSE_STRING;
				String t = AttributeType.DEFAULT_BINARY_TRUE_STRING;
				// NOTE uses this.add(String) to populate
				// itsDistinctValues(Map), requires itsSize = 0;
				for (int i = 0, j = itsNominalz.length; i < j; ++i)
					this.add(itsBinaries.get(i) ? t : f);

				// Cleanup (for GarbageCollector).
				itsBinaries = null;
				break;
			}
			default :
			{
				logTypeError("Column.toNominalType()");
				throw new AssertionError(itsType);
			}
		}

		itsType = AttributeType.NOMINAL;
		return true;
	}

	/*
	 * Specifically needed during loading, to change a column that at first appeared to be binary to nominal.
	 * It requires at least one of the two values that were erroneously interpreted as a binary value
	 * This function also sets the missing value to nominal.
	 */
	boolean toNominalType(String aTrue, String aFalse)
	{
		// relies on itsCardinality to be set at this time
		itsDistinctValues = new ArrayList<String>(itsCardinality);
		itsDistinctValuesMap = new HashMap<String, Integer>(itsCardinality);
//		itsNominals = new ArrayList<String>(itsSize);
		itsNominalz = new int[itsSize];
		itsMissingValue = AttributeType.NOMINAL.DEFAULT_MISSING_VALUE;

// this code is slightly faster, than the code at the bottom as it does not call
// itsDistinctValuesMap.get(String) to determine existence of key
// but switching of Column type is hardly ever done
// and calling add(String) is more robust as it takes care of maintaining sane
// internal state
//		if (aFalse != null)
//		{
//			itsDistinctValues.add(aFalse);
//			itsDistinctValuesMap.add(aFalse);
//		}
//		if (aTrue != null)
//		{
//			itsDistinctValues.add(aTrue);
//			itsDistinctValuesMap.add(aTrue);
//		}
//
//		for (int i = 0, j = itsSize; i < j; ++i)
//			if (aTrue == null && aFalse == null) //just missing values so far
//				itsNominals.add(itsMissingValue);
//			else
//				itsNominals.add(itsFloatz[i] > 0.5f ? aTrue : aFalse);

		// new code replaces all above
		// NOTE uses this.add(String) to populate
		// itsDistinctValues(Map), requires itsSize = 0;
		itsSize = 0;
		for (int i = 0, j = itsNominalz.length; i < j; ++i)
		{
			// just missing values so far
			if (aTrue == null && aFalse == null)
				this.add(itsMissingValue);
			else
				this.add(itsFloatz[i] > 0.5f ? aTrue : aFalse);
		}

		// Cleanup (for GarbageCollector).
		itsBinaries = null;
		itsType = AttributeType.NOMINAL;
		return true;
	}

	/*
	 * Switching between Column AttributeTypes of NUMERIC and ORDINAL is
	 * always possible, without any other further changes.
	 * Changing from a BINARY AttributeType is also possible.
	 * Changing from a NOMINAL AttributeType is only possible if all values
	 * in itsNominals can be parsed as Floats.
	 * The old itsMissingValue can only be used if it can be parsed as Float
	 * , else itsMissingValue will be set to
	 * AttributeType.theNewType.DEFAULT_MISSING_VALUE.
	 */
	private boolean toNumericType(AttributeType theNewType)
	{
		switch (itsType)
		{
			/*
			 * for he NOMINAL case, parsing each value in
			 * itsDistinctValues would probably be much faster
			 * if it fails, this method would return early
			 * if it succeeds, all values in itsNominals/z are
			 * guaranteed to be 'floats'
			 */
			case NOMINAL :
			{
				itsFloatz = new float[itsSize];
				for (int i = 0; i < itsSize; ++i)
				{
					// TODO '?' could be caught here and replaced by DEFAULT_MISSING_VALUE
					// complicates cardinality logic
					try
					{
//						itsFloatz[i] = Float.valueOf(itsNominals.get(i));
						itsFloatz[i] = Float.valueOf(itsDistinctValues.get(itsNominalz[i]));
					}
					catch (NumberFormatException e)
					{
						/*
						 * If there is a value that can not be parsed as float:
						 * abort, cleanup (for GarbageCollector) and return.
						 */
						itsFloatz = null;
						return false;
					}
				}
				/*
				 * Only gets here if all values are parsed successfully, (or
				 * itsSize == 0). Cleanup (for GarbageCollector).
				 */
				itsDistinctValues = null;
				itsDistinctValuesMap = null;
//				itsNominals = null;
				itsNominalz = null;

				if (itsMissing.cardinality() == 0 && !isValidValue(theNewType, itsMissingValue))
				{
					itsMissingValue = theNewType.DEFAULT_MISSING_VALUE;
					// check could be done in for-loop above
					// XXX may give rounding issues
					itsMissingValueIsUnique = true;
					float aMissingValue = Float.parseFloat(itsMissingValue);
					for (float f : itsFloatz)
					{
						if (f == aMissingValue)
						{
							itsMissingValueIsUnique = false;
							break;
						}
					}
				}
				/*
				 * else old ArrayList<?> contained only valid
				 * Floats, also for missing values, we use a
				 * Float version of itsMissingValue
				 */
				else
					itsMissingValue = Float.valueOf(itsMissingValue).toString();
				break;
			}
			case NUMERIC :
				break; // nothing to do
			case ORDINAL :
				throw new AssertionError(itsType);
			case BINARY :
			{
				itsFloatz = new float[itsSize];

				// all 0.0f, change to 1.0f only for set bits
				for (int i = itsBinaries.nextSetBit(0); i >= 0; i = itsBinaries.nextSetBit(i + 1))
					itsFloatz[i] = 1.0f;

				// Cleanup (for GarbageCollector).
				itsBinaries = null;
				itsMissingValue = Float.valueOf(itsMissingValue).toString();
				break;
			}
			default :
			{
				logTypeError("Column.toNumericType()");
				throw new AssertionError(itsType);
			}
		}

		itsType = theNewType;
		return true;
	}

	/*
	 * If a Column with a NUMERIC/ORDINAL/NOMINAL AttributeType has at most
	 * two distinct values, its type can be changed to BINARY.
	 * If the values in the old Column evaluate to 'true' for
	 * (AttributeType.isValidBinaryTrueValue(itsMissingValue) ||
	 * AttributeType.isValidBinaryFalseValue(itsMissingValue)), the bits in
	 * the new BitSet will be set accordingly.
	 * If one of the two classes is '?'
	 * (AttributeType.NOMINAL.DEFAULT_MISSING_VALUE), the corresponding bits
	 * will be set using AttributeType.BINARY.DEFAULT_MISSING_VALUE.
	 * The value of the first value in itsFloatz/itsNominals will serve as
	 * the 'true' case, meaning the bits in the new itsBinary BitSet will be
	 * set ('true') for all instances having that value, all others will be
	 * 'false'.
	 * The old itsMissingValue can only be used if
	 * (AttributeType.isValidBinaryTrueValue(itsMissingValue) ||
	 * AttributeType.isValidBinaryFalseValue(itsMissingValue)) evaluates
	 * 'true'.
	 * In that case the itsMissingValue will be set to the '0'/'1'
	 * equivalent of itsMissingValue, else itsMissingValue will be set to
	 * AttributeType.BINARY.DEFAULT_MISSING_VALUE.
	 * This will also happen if the old itsMissingValue is the
	 * DEFAULT_MISSING_VALUE for the current AttributeType.
	 */
	private boolean toBinaryType()
	{
		assert (itsType != AttributeType.BINARY);

		if (getCardinality() > 2 || getCardinality() < 0)
			return false;

		switch (itsType)
		{
			case NOMINAL :
			{
//				itsBinaries = new BitSet(itsSize);
//
//				if (itsSize > 0)
//				{
//					String aValue = itsNominals.get(0);
//
//					if (AttributeType.isValidBinaryTrueValue(aValue))
//					{
//						// All false initially, only set 'true' bits.
//						for (int i = 0; i < itsSize; i++)
//							if (aValue.equals(itsNominals.get(i)))
//								itsBinaries.set(i);
//					}
//					else if (AttributeType.isValidBinaryFalseValue(aValue))
//					{
//						// All false initially, only set 'true' bits.
//						for (int i = 0; i < itsSize; i++)
//							if (!aValue.equals(itsNominals.get(i)))
//								itsBinaries.set(i);
//					}
//					// TODO ask user which value to use as 'true'
//					// now sets all non-missing to 'true'
//					else if (AttributeType.NOMINAL.DEFAULT_MISSING_VALUE.equals(aValue))
//					{
//						if ((itsMissing.cardinality() < itsSize) && AttributeType.isValidBinaryTrueValue(itsNominals.get(itsMissing.nextClearBit(0))))
//							// All false initially, only set non-missing values to 'true'.
//							for (int i = itsMissing.nextClearBit(0); i >= 0 && i < itsSize; i = itsMissing.nextClearBit(i + 1))
//								itsBinaries.set(i);
//					}
//					// TODO ask user which value to use as 'true'
//					// now sets all itsNominals.get(0) values to 'true'
//					else
//					{
//						// All false initially, only set 'true' bits.
//						for (int i = 0; i < itsSize; i++)
//							if (aValue.equals(itsNominals.get(i)))
//								itsBinaries.set(i);
//					}
//				}
//				itsDistinctValues = null;
//				itsDistinctValuesMap = null;
//				itsNominals = null;
//				break;
				itsBinaries = new BitSet(itsSize);

				if (itsSize > 0)
				{
					// String aValue = itsNominals.get(0);
					int v = itsNominalz[0];
					String aValue = itsDistinctValues.get(v);

					if (AttributeType.isValidBinaryTrueValue(aValue))
					{
						// All false initially, only set 'true' bits.
						for (int i = 0; i < itsSize; ++i)
							if (v == itsNominalz[i])
								itsBinaries.set(i);
					}
					else if (AttributeType.isValidBinaryFalseValue(aValue))
					{
						// All false initially, only set 'true' bits.
						for (int i = 0; i < itsSize; ++i)
							if (v != itsNominalz[i])
								itsBinaries.set(i);
					}
					// TODO ask user which value to use as 'true'
					// now sets all non-missing to 'true'
					else if (AttributeType.NOMINAL.DEFAULT_MISSING_VALUE.equals(aValue))
					{
						if ((itsMissing.cardinality() < itsSize) && AttributeType.isValidBinaryTrueValue(itsDistinctValues.get(itsNominalz[itsMissing.nextClearBit(0)])))
							// All false initially, only set non-missing values to 'true'.
							for (int i = itsMissing.nextClearBit(0); i >= 0 && i < itsSize; i = itsMissing.nextClearBit(i + 1))
								itsBinaries.set(i);
					}
					// TODO ask user which value to use as 'true'
					// now sets all itsNominals.get(0) values to 'true'
					else
					{
						// All false initially, only set 'true' bits.
						for (int i = 0; i < itsSize; ++i)
							if (v == itsNominalz[i])
								itsBinaries.set(i);
					}
				}
				itsDistinctValues = null;
				itsDistinctValuesMap = null;
				itsNominalz = null;
				break;
			}
			case NUMERIC :
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
					float aValue = itsFloatz[0];

					if (aValue == 1.0f)
					{
						// All false initially, only set 'true' bits.
						for (int i = 0; i < itsSize; i++)
							if (itsFloatz[i] == 1.0f)
								itsBinaries.set(i);
					}
					else if (aValue == 0.0f)
					{
						// All false initially, only set 'true' bits.
						for (int i = 0; i < itsSize; i++)
							if (itsFloatz[i] != 0.0f)
								itsBinaries.set(i);
					}
					// TODO ask user which value to use as 'true'
					// now sets all non-missing to 'true'
					else if ((Float.parseFloat(itsMissingValue) == aValue) &&
							(itsMissing.cardinality() < itsSize) &&
							(Float.toString(itsFloatz[itsMissing.nextClearBit(0)]).matches(trueFloat)))
					{
						// All false initially, only set non-missing values to 'true'.
						for (int i = itsMissing.nextClearBit(0); i >= 0 && i < itsSize; i = itsMissing.nextClearBit(i + 1))
							itsBinaries.set(i);
					}
					// TODO ask user which value to use as 'true'
					// now sets NaN to 'false'
					else if (Float.isNaN(aValue))
					{
						// All false initially, only set 'true' bits.
						for (int i = 0; i < itsSize; i++)
							if (!Float.isNaN(itsFloatz[i]))
									itsBinaries.set(i);
					}
					// TODO ask user which value to use as 'true'
					// now sets all itsFloatz[0] values to 'true'
					else
					{
						// All false initially, only set 'true' bits.
						for (int i = 0; i < itsSize; i++)
							if (itsFloatz[i] == aValue)
								itsBinaries.set(i);
					}
				}
				itsFloatz = null;
				break;
			}
			case ORDINAL : throw new AssertionError(itsType);
			case BINARY :  throw new AssertionError(itsType);
			default :
			{
				logTypeError("Column.toBinaryType()");
				throw new AssertionError(itsType);
			}
		}
		try
		{
			// always change itsMissingValue to "0" or "1"
			if (Float.parseFloat(itsMissingValue) == 0.0f)
				itsMissingValue = "0";
			else if (Float.parseFloat(itsMissingValue) == 1.0f)
				itsMissingValue = "1";
			else if (itsType.DEFAULT_MISSING_VALUE.equals(itsMissingValue))
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

		updateCardinality();

		itsType = AttributeType.BINARY;
		return true;
	}

	private void logTypeError(String theMethodName)
	{
		Log.logCommandLine(
			String.format("Error in %s: Column '%s' has AttributeType '%s'.",
					theMethodName,
					getName(),
					itsType));
	}

	/**
	 * Returns whether this Column is enabled.
	 * @return {@code true} if this Column is enabled, {@code false}
	 * otherwise.
	 */
	public boolean getIsEnabled() { return isEnabled; }

	/**
	 * Set whether this Column is enabled.
	 * @param theSetting use {@code true} to enable this Column, and
	 * {@code false} to disable it.
	 */
	public void setIsEnabled(boolean theSetting) { isEnabled = theSetting; }

	/**
	 * Returns whether this Column is has missing values or not.
	 *
	 * @return {@code true} if this Column has missing values {@code false}
	 * otherwise.
	 */
	public boolean getHasMissingValues() { return !itsMissing.isEmpty(); }

	/**
	 * Returns a <b>copy of</b> a BitSet representing the missing values for
	 * this Column. Bits of this BitSet are set for those values that, in
	 * the original data, were of the form '?'.
	 * <p>
	 * NOTE: use {@link #setMissing} to set missing values for this Column.
	 * Modifications to the BitSet retrieved through this method have no
	 * effect on the original missing values BitSet of this Column.
	 *
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
	 * <p>
	 * If this Column {@link #getHasMissingValues() does not have} any
	 * missing values the empty String "" is returned.
	 *
	 * @return the value currently set for all missing values.
	 */
	public String getMissingValue()
	{
		return itsMissing.isEmpty() ? "" : itsMissingValue;
	}

	/**
	 * Sets the new missing value for this Column. The missing value is used
	 * as replacement value for all values that where '?' in the original
	 * data.
	 *
	 * @param theNewValue the value to use as new missing value.
	 *
	 * @return {@code true} if setting the new missing value is successful,
	 * {@code false} otherwise.
	 */
	public boolean setNewMissingValue(String theNewValue)
	{
		if (theNewValue == null)
			throw new IllegalArgumentException("arguments can not be null");

		if (itsMissingValue.equals(theNewValue))
			return true;
		else if (!isValidValue(itsType, theNewValue))
			return false;

		switch (itsType)
		{
			case NOMINAL :
			{
				// remove Entry for old itsMissingValue
				Integer value = itsDistinctValuesMap.remove(itsMissingValue);
				// indicates no missing values in data
				if (value == null)
				{
					Log.logCommandLine("no missing values in the data");
					return true;
				}
				// add Entry for new itsMissingValue
				itsDistinctValuesMap.put(theNewValue, value);
				int v = value;
				assert (itsDistinctValues.get(v).equals(itsMissingValue));
				itsDistinctValues.set(v, theNewValue);

				itsMissingValue = theNewValue;
				updateCardinality(itsDistinctValues);
//				for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
//					itsNominals.set(i, itsMissingValue);
				return true;
			}
			case NUMERIC :
			{
				itsMissingValue = Float.valueOf(theNewValue).toString();
				updateCardinality(Arrays.asList(itsFloatz));
				Float aNewValue = Float.valueOf(itsMissingValue);
				for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
					itsFloatz[i] = aNewValue;
				return true;
			}
			case ORDINAL :
			{
				throw new AssertionError(itsType);
			}
			case BINARY :
			{
				itsMissingValue =
					(AttributeType.isValidBinaryTrueValue(theNewValue) ? "1" : "0");
				boolean aNewValue = "0".equals(itsMissingValue);
				for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
				{
					if (aNewValue)
						itsBinaries.clear(i);
					else
						itsBinaries.set(i);
				}
				updateCardinality();
				return true;
			}
			default :
			{
				logTypeError("Column.setNewMissingValue()");
				throw new AssertionError(itsType);
			}
		}
	}

	// "?" is invalid for float/ binary, to be handled by conversion-methods
	private boolean isValidValue(AttributeType theAttributeType, String theNewValue)
	{
		switch (theAttributeType)
		{
			case NOMINAL :
				return true;
			case NUMERIC :
			{
				try { Float.parseFloat(theNewValue); return true; }
				catch (NumberFormatException anException) { return false; }
			}
			case ORDINAL :
				throw new AssertionError(theAttributeType);
			case BINARY :
			{
				return AttributeType.isValidBinaryTrueValue(theNewValue) ||
					AttributeType.isValidBinaryFalseValue(theNewValue);
			}
			default :
			{
				logTypeError("Column.isValidValue()");
				throw new AssertionError(theAttributeType);
			}
		}
	}

	/*
	 * NOTE This may not be the fastest implementation, but it avoids HashSets'
	 * hashCode() collisions.
	 */
	/**
	 * Counts the number of distinct values, or cardinality, of this Column.
	 * It is recommended to run this function after all data is loaded into
	 * a Column, as correct counts depend on the unmodified original data.
	 *
	 * @return the number of distinct values, {@code 0} when this Column
	 * contains no data ({@link #size} {@code == 0}).
	 *
	 * @see #getDomain()
	 * @see #getUniqueNominalBinaryDomain(BitSet)
	 * @see #getUniqueNumericDomain(BitSet)
	 */
	public int getCardinality()
	{
		if (itsSize == 0 || itsSize == 1)
			return itsSize;

		// already set
		if (itsCardinality != 0)
			return itsCardinality;

		// not set yet
		switch (itsType)
		{
			case NOMINAL :
			{
				itsMissingValueIsUnique = true;
				itsCardinality = itsDistinctValues.size();
				return itsCardinality;
			}
			case NUMERIC :
			{
				float aMissingValue = Float.valueOf(itsType.DEFAULT_MISSING_VALUE).floatValue();
				for (int i = 0; i < itsSize; i++)
				{
					if (itsFloatz[i] == aMissingValue && !itsMissing.get(i))
					{
						itsMissingValueIsUnique = false;
						break;
					}
				}

				// XXX inefficient but good enough
				BitSet b = new BitSet();
				b.set(0, itsSize);
				itsCardinality = getUniqueNumericDomain(b).length;
				return itsCardinality;
			}
			case ORDINAL :
				throw new AssertionError(itsType);
			case BINARY :
			{
				// BINARY.DEFAULT_MISSING_VALUE is "0", but could be "1"
				if ("0".equals(itsType.DEFAULT_MISSING_VALUE))
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

				itsCardinality = getBinariesCardinality();
				return itsCardinality;
			}
			default :
			{
				logTypeError("Column.getNrDistinct()");
				throw new AssertionError(itsType);
			}
		}
	}

	private void updateCardinality(List<?> theColumnData)
	{
		// not set yet, or no data
		if (itsCardinality == 0)
			getCardinality();
		else
		{
			Object aValue;
			if (itsType == AttributeType.NOMINAL)
				aValue = itsMissingValue;
			else
				aValue = Float.valueOf(itsMissingValue);

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

		itsCardinality = getBinariesCardinality();
	}

	// 0 if Column is empty, 1 if all 0s or all 1s, 2 if both 0s and 1s
	private int getBinariesCardinality()
	{
		if (itsSize == 0)
			return 0;

		// check if all true/ all false
		final int set = itsBinaries.nextSetBit(0);

		// no 1's, just 0's
		if (set < 0 || set >= itsSize)
			return 1;

		// there is at least one 1

		// 1 occurs after (a number of) 0's
		if (set > 0)
			return 2;

		// is there a 0 somewhere, or only 1's
		final int clear = itsBinaries.nextClearBit(0);
		if (clear >= 0 && clear < itsSize)
			return 2;
		else
			return 1;
	}

	public void makeNoTarget() { if (itsType == AttributeType.NUMERIC) itsTargetStatus = NONE; }
	public void makePrimaryTarget() { if (itsType == AttributeType.NUMERIC) itsTargetStatus = PRIMARY; }
	public void makeSecondaryTarget() { if (itsType == AttributeType.NUMERIC) itsTargetStatus = SECONDARY; }
	public void makeTertiaryTarget() { if (itsType == AttributeType.NUMERIC) itsTargetStatus = TERTIARY; }
	public int getTargetStatus() { return itsTargetStatus; }
	public String displayTargetStatus()
	{
		return getTargetText(itsTargetStatus);
	}

	public static String getTargetText(int theTargetStatus)
	{
		switch (theTargetStatus)
		{
			case NONE	: return " none";
			case PRIMARY	: return " primary";
			case SECONDARY	: return " secondary";
			case TERTIARY	: return " tertiary";
			default :
			{
				throw new AssertionError("unknown TargetStatus: " + theTargetStatus);
			}
		}
	}

	public void setTargetStatus(String theTargetStatus)
	{
		if (itsType == AttributeType.NUMERIC)
		{
			if (theTargetStatus.equals(" none"))
				makeNoTarget();
			else if (theTargetStatus.equals(" primary"))
				makePrimaryTarget();
			else if (theTargetStatus.equals(" secondary"))
				makeSecondaryTarget();
			else if (theTargetStatus.equals(" tertiary"))
				makeTertiaryTarget();
			else itsTargetStatus = Integer.MAX_VALUE; // should be impossible
		}
	}

	public void setTargetStatus(int theTargetStatus)
	{
		if (itsType == AttributeType.NUMERIC)
			itsTargetStatus = theTargetStatus;
	}

	/**
	 * Evaluates the supplied {@link Condition} for this Column.
	 *
	 * @param theCondition the Condition to test for this Column.
	 *
	 * @return a {@code BitSet} with bits set to {@code true} for members
	 * of this Column for which the supplied Condition holds.
	 *
	 * @throws IllegalArgumentException if the supplied Condition does not
	 * apply to this Column.
	 *
	 * @see Condition
	 */
	public BitSet evaluate(Condition theCondition) throws IllegalArgumentException
	{
		// XXX evaluation logic is deeply flawed, this hack is needed
		if (theCondition.getColumn() != this)
			throw new IllegalArgumentException(
					String.format("%s does not apply to %s",
							theCondition.toString(),
							getName()));

		BitSet aSet = new BitSet(itsSize);

		switch (itsType)
		{
			case NOMINAL :
			{
//				for (int i = 0, j = itsSize; i < j; ++i)
//					if (theCondition.evaluate(itsNominals.get(i)))
//						aSet.set(i);
				for (int i = 0, j = itsSize; i < j; ++i)
					if (theCondition.evaluate(itsDistinctValues.get(itsNominalz[i])))
						aSet.set(i);
				break;
			}
			case NUMERIC :
			{
				for (int i = 0, j = itsSize; i < j; ++i)
					if (theCondition.evaluate(itsFloatz[i]))
						aSet.set(i);
				break;
			}
			case ORDINAL :
				throw new AssertionError(itsType);
			case BINARY :
			{
				for (int i = 0, j = itsSize; i < j; ++i)
					if (theCondition.evaluate(itsBinaries.get(i)))
						aSet.set(i);
				break;
			}
			default :
			{
				logMessage("evaluate", String.format("unknown AttributeType '%s'", itsType));
				throw new AssertionError(itsType);
			}
		}

		return aSet;
	}

	/**
	 * Evaluates the supplied Condition, but only for the records of this
	 * Column selected by the set bits in the supplied BitSet.
	 *
	 * This method does not modify the input BitSet.
	 *
	 * @param theBitSet the Subgroup members.
	 * @param theCondition the Condition to evaluate.
	 *
	 * @return a BitSet with bits set for those records for which the
	 * supplied Condition holds, bits are clear otherwise.
	 *
	 * @throws IllegalArgumentException when the supplied Condition is not
	 * about this Column.
	 */
	BitSet evaluate(BitSet theBitSet, Condition theCondition) throws IllegalArgumentException
	{
		if (theBitSet == null)
			throw new IllegalArgumentException("BitSet can not be null");
		// XXX evaluation logic is deeply flawed, this hack is needed
		if (theCondition.getColumn() != this)
			throw new IllegalArgumentException(
					String.format("%s does not apply to %s",
							theCondition.toString(),
							getName()));

		BitSet aResult;
		Operator anOperator = theCondition.getOperator();

		// methods do not return immediately, allows for assert below
		switch (itsType)
		{
			case NOMINAL :
			{
				aResult = new BitSet(itsSize);

				switch (anOperator)
				{
					case ELEMENT_OF :
						nominalElementOf(theBitSet, theCondition.getNominalValueSet(), aResult);
						break;
					case EQUALS :
						nominalEquals(theBitSet, theCondition.getNominalValue(), aResult);
						break;
					default :
						throw new AssertionError(itsType + " incompatible with " + anOperator);
				}
				break;
			}
			case NUMERIC :
			{
				aResult = new BitSet(itsSize);

				switch (anOperator)
				{
					case EQUALS :
						numericEquals(theBitSet, theCondition.getNumericValue(), aResult);
						break;
					case LESS_THAN_OR_EQUAL :
						numericLEQ(theBitSet, theCondition.getNumericValue(), aResult);
						break;
					case GREATER_THAN_OR_EQUAL :
						numericGEQ(theBitSet, theCondition.getNumericValue(), aResult);
						break;
					case BETWEEN :
						numericBetween(theBitSet, theCondition.getNumericInterval(), aResult);
						break;
					default :
						throw new AssertionError(itsType + " incompatible with " + anOperator);
				}
				break;
			}
			case ORDINAL :
			{
				throw new AssertionError(itsType);
			}
			case BINARY :
			{
				assert(anOperator == Operator.EQUALS);
				aResult =  binaryEquals(theBitSet, theCondition.getBinaryValue());
				break;
			}
			default :
			{
				logMessage("evaluate", String.format("unknown AttributeType '%s'", itsType));
				throw new AssertionError(itsType);
			}
		}

		// XXX compare against old code, may be removed one day
		// as it evaluates theCondition twice
		assert (postTest(aResult, evaluate(theCondition)));

		return aResult;
	}

	// account for d>1, where _new will not be the same as old
	// every bit set in new, must also be set in old, and no more
	private static final boolean postTest(BitSet _new, BitSet old)
	{
		// do not modify _new, as it is needed as result
		BitSet b = (BitSet)_new.clone();
		b.andNot(old);
		return b.isEmpty();
	}

	private BitSet nominalElementOf(BitSet theMembers, ValueSet theValueSet, BitSet theResult)
	{
//		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
//			if (theValueSet.contains(itsNominals.get(i)))
//				theResult.set(i);

		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			if (theValueSet.contains(itsDistinctValues.get(itsNominalz[i])))
				theResult.set(i);

		// TODO MM loop could be faster not using itsDistinctValues
		// instead ValueSet would be transformed into its equivalent
		// (sorted) int[] using itsDistinctValuesMap
//		int[] vs = new int[theValueSet.size()];
//		for (int i = 0; i < vs.length; ++i)
//			vs[i] = theValueSet.get(i);
//		Arrays.sort(vs);
//		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
//			if (Arrays.binarySearch(vs, itsNominalz[i]) >= 0)
//				theResult.set(i);

		return theResult;
	}

	/*
	 * package-private evaluate() calls this private method
	 * if itsDistinctValuesMap.get(theValue) returns null something is very
	 * wrong and this method will generate a NullPointerException
	 */
	private BitSet nominalEquals(BitSet theMembers, String theValue, BitSet theResult)
	{
//		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
//			if (itsNominals.get(i).equals(theValue))
//				theResult.set(i);
		int v = itsDistinctValuesMap.get(theValue);
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			if (itsNominalz[i] == v)
				theResult.set(i);

		return theResult;
	}

	private BitSet numericEquals(BitSet theMembers, float theValue, BitSet theResult)
	{
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			if (itsFloatz[i] == theValue)
				theResult.set(i);

		return theResult;
	}

	private BitSet numericLEQ(BitSet theMembers, float theValue, BitSet theResult)
	{
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			if (itsFloatz[i] <= theValue)
				theResult.set(i);

		return theResult;
	}

	private BitSet numericGEQ(BitSet theMembers, float theValue, BitSet theResult)
	{
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			if (itsFloatz[i] >= theValue)
				theResult.set(i);

		return theResult;
	}

	private BitSet numericBetween(BitSet theMembers, Interval theInterval, BitSet theResult)
	{
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			if (theInterval.between(itsFloatz[i]))
				theResult.set(i);

		return theResult;
	}

	private BitSet binaryEquals(BitSet theMembers, boolean theValue)
	{
		// faster than or
		BitSet aResult = (BitSet)itsBinaries.clone();

		if (!theValue)
			aResult.flip(0, itsSize);

		// faster than bit-loop, operates on underlying longs in long[]
		aResult.and(theMembers);

		return aResult;
	}

	/**
	 * Returns the statistics needed in the computation of quality measures
	 * for Columns of {@link AttributeType} {@link AttributeType#NUMERIC}
	 * and {@link AttributeType#ORDINAL}.
	 * <p>
	 * The bits set in the BitSet supplied as argument indicate which values
	 * of the Column should be used for the calculation.
	 * When the BitSet represents the members of a {@link Subgroup}, this
	 * method calculates the relevant arguments to determine the quality of
	 * that Subgroup.
	 * <p>
	 * The {@code getMedianAndMedianAD} parameter controls what statistics
	 * are to be computed. {@code getMedianAndMedianAD} needs only be
	 * {@code true} in case of
	 * {@link QualityMeasure#calculate(int, float, float, float, float,
	 * int[], ProbabilityDensityFunction)}
	 * for the Median MAD metric ({@link QM#MMAD}).
	 * <p>
	 * The resulting {@code float[]} is always of length {@code 4}, and, in
	 * order, holds the following values: sum, sum of squared deviation,
	 * median and median absolute deviation. Of these, the last two are only
	 * computed for the quality measure MMAD, and set to {@code Float.NaN}
	 * otherwise.
	 * <p>
	 * When the {@link java.util.BitSet#cardinality()} is {@code 0}, no
	 * meaningful statistics can be computed, and a {@code float[4]}
	 * containing {@code 4} {@code Float.NaN}s will be returned.
	 * <p>
	 * When the Column is not of type NUMERIC or ORDINAL a {@code float[4]}
	 * containing {@code 4} {@code Float.NaN}s will be returned.
	 *
	 * @param theBitSet the BitSet indicating what values of this Column to
	 * use in the calculations.
	 *
	 * @param getMedianAndMedianAD the {@code boolean} indicating whether
	 * the median and median absolute deviation should be calculated.
	 *
	 * @return a {@code float[4]}, holding the arguments relevant for the
	 * setting supplied as argument.
	 *
	 * @see AttributeType
	 * @see QualityMeasure
	 * @see QM
	 * @see Subgroup
	 * @see java.util.BitSet
	 */
	public Statistics getStatistics(BitSet theBitSet, boolean getMedianAndMedianAD, boolean addComplement)
	{
		if (!isValidCall("getStatistics", theBitSet))
			return new Statistics();
		// not all methods below are safe for divide by 0
		else if (theBitSet.cardinality() == 0)
			return new Statistics();

		final Statistics aResult;
		// if (!MMAD) do not store and sort the values, else do
		if (!getMedianAndMedianAD)
		{
			float aSum = computeSum(theBitSet);
			aResult = new Statistics(theBitSet.cardinality(), aSum, computeSumSquaredDeviations(aSum, theBitSet));
		}
		else
		{
			// this code path needs a copy of the data for median
			// get relevant values and do summing in single loop
			float aSum = 0.0f;
			float[] aValues = new float[theBitSet.cardinality()];
			for (int i = theBitSet.nextSetBit(0), j = -1; i >= 0; i = theBitSet.nextSetBit(i + 1))
				aSum += (aValues[++j] = itsFloatz[i]);
			Arrays.sort(aValues);
			float aMedian = computeMedian(aValues);

			aResult = new Statistics(
				theBitSet.cardinality(), 
				aSum, 
				computeSumSquaredDeviations(aSum, aValues), 
				aMedian, 
				computeMedianAbsoluteDeviations(aMedian, aValues));
		}

		if (addComplement)
		{
			BitSet aComplement = (BitSet) theBitSet.clone();
			aComplement.flip(0, itsSize);
			float aComplementSum = computeSum(aComplement);
			aResult.addComplement(aComplement.cardinality(), aComplementSum, computeSumSquaredDeviations(aComplementSum, aComplement));
			aResult.addDatasetSSD(computeSumSquaredDeviations(aResult.getAverage()));
		}
		return aResult;
	}

	// for all data, not just subgroup
	private float computeSum()
	{
		float aSum = 0.0f;
		for (int i=0; i<itsSize; i++)
			aSum += itsFloatz[i];
		return aSum;
	}

	private float computeSum(BitSet theBitSet)
	{
		float aSum = 0.0f;
		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
			aSum += itsFloatz[i];
		return aSum;
	}

	// always called after computeSum for !MMAD
	// not safe for divide by 0 (theBitSet.cardinality() == 0)
	private float computeSumSquaredDeviations(float theSum, BitSet theBitSet)
	{
		float aMean = theSum / theBitSet.cardinality();
		float aSum = 0.0f;
		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
			aSum += Math.pow((itsFloatz[i]-aMean), 2);
		return aSum;
	}

	// for all data, not just subgroup
	private float computeSumSquaredDeviations(float theMean)
	{
		float aSum = 0.0f;
		for (int i=0; i<itsSize; i++)
			aSum += Math.pow((itsFloatz[i]-theMean), 2);
		return aSum;
	}

	// always called after computeSum for MMAD
	// not safe for divide by 0 (theSortedValues.length == 0)
	private float computeSumSquaredDeviations(float theSum, float[] theSortedValues)
	{
		float aMean = theSum / theSortedValues.length;
		float aSum = 0.0f;
		for (float f : theSortedValues)
			aSum += Math.pow((f-aMean), 2);
		return aSum;
	}

	// always called after computeSumSquaredDeviations for MMAD
	// throws indexOutOfBoundsException if (theSortedValues.length == 0)
	private float computeMedian(float[] theSortedValue)
	{
		int aLength = theSortedValue.length;
		// even, bit check (even numbers end with 0 as last bit)
		if ((aLength & 1) == 0)
			return (theSortedValue[(aLength/2)-1] + theSortedValue[aLength/2]) / 2;
		else
			return theSortedValue[aLength/2];
	}

	// always called after computeMedian for MMAD
	// may re-throw computeMedian()'s indexOutOfBoundsException
	private float computeMedianAbsoluteDeviations(float theMedian, float[] theSortedValues)
	{
		// compute absolute deviations of the elements in the subgroup
		// store these in the original array for efficiency
		for (int i = 0, j = theSortedValues.length; i < j; ++i)
			theSortedValues[i] = Math.abs(theSortedValues[i]-theMedian);

		//compute the MAD: the median of absolute deviations
		Arrays.sort(theSortedValues);
		return computeMedian(theSortedValues);
	}

	/**
	 * Returns a {@code java.util.TreeSet} with all distinct values for this
	 * Column, with values ordered according to their natural ordering.
	 *
	 * @return the domain for this Column.
	 *
	 * @see #getUniqueNominalBinaryDomain(BitSet)
	 * @see #getUniqueNumericDomain(BitSet)
	 * @see #getCardinality()
	 */
	public TreeSet<String> getDomain()
	{
		switch (itsType)
		{
			case NOMINAL :
			{
				return new TreeSet<String>(itsDistinctValues);
			}
			case NUMERIC :
			{
				TreeSet<String> aResult = new TreeSet<String>();
				for (float f : itsFloatz)
					aResult.add(Float.toString(f));
				return aResult;
			}
			case ORDINAL :
			{
				throw new AssertionError(itsType);
			}
			case BINARY :
			{
				final TreeSet<String> aResult = new TreeSet<String>();

				final int aSize = getBinariesCardinality();
				switch (aSize)
				{
					case 0 : return aResult;
					case 1 :
					{
						final int set = itsBinaries.nextSetBit(0);
						// no 1's, just 0's
						if (set < 0 || set >= itsSize)
							aResult.add("0");
						// FIXME MM what happens when
						// there is only 1's
						// result should be {1}
						// else
						//	aResult.add("1");
						return aResult;
					}
					case 2 :
					{
						aResult.add("0");
						aResult.add("1");
						return aResult;
					}
					default :
					{
						throw new AssertionError(aSize);
					}
				}
			}
			default :
			{
				logTypeError("Column.getDomain()");
				throw new AssertionError(itsType);
			}
		}
	}

	/**
	 * Returns the unique, sorted domain for Columns of
	 * {@link AttributeType} {@link AttributeType#NOMINAL} and
	 * {@link AttributeType#BINARY}.
	 * <p>
	 * The bits set in the BitSet supplied as argument indicate which values
	 * of the Column should be used for the creation of the domain.
	 * When the BitSet represents the members of a {@link Subgroup}, this
	 * method returns the domain covered by that Subgroup.
	 * <p>
	 * The maximum size of the returned String[] is the minimum of
	 * {@link java.util.BitSet#cardinality() BitSet.cardinality()} and this
	 * Columns {@link #getCardinality() cardinality}}.</br>
	 * The minimum size is 0, if the BitSet has cardinality {@code 0} or is
	 * {@code null}.
	 * <p>
	 * When the Column is not of {@link AttributeType}
	 * {@link AttributeType#NOMINAL} or {@link AttributeType#BINARY} a
	 * {@code new String[0]} will be returned.
	 *
	 * @param theBitSet the BitSet indicating what values of this Column to
	 * use for the creation of the domain.
	 *
	 * @return a String[], holding the distinct values for the domain.
	 *
	 * @see #getDomain()
	 * @see #getUniqueNominalBinaryDomain(BitSet)
	 * @see #getCardinality()
	 * @see AttributeType
	 * @see Subgroup
	 * @see java.util.BitSet
	 */
	// TODO MM return Set, instead of creating Set, and returning toArray()
	public String[] getUniqueNominalBinaryDomain(BitSet theBitSet)
	{
		if (theBitSet.length() > itsSize)
			throw new IllegalArgumentException("theBitSet.length() > " + itsSize);

		final Set<String> aUniqueValues = new TreeSet<String>();

		switch (itsType)
		{
			case NOMINAL :
			{
				// abort when all distinct values are added
//				for (int i = theBitSet.nextSetBit(0); i >= 0 && i < itsSize; i = theBitSet.nextSetBit(i + 1))
//					if (aUniqueValues.add(itsNominals.get(i)))
//						if (aUniqueValues.size() == itsCardinality)
//							break;
				for (int i = theBitSet.nextSetBit(0); i >= 0 && i < itsSize; i = theBitSet.nextSetBit(i + 1))
					if (aUniqueValues.add(itsDistinctValues.get(itsNominalz[i])))
						if (aUniqueValues.size() == itsCardinality)
							break;
				break;
			}
			case BINARY :
			{
				// abort when all distinct values are added
				for (int i = theBitSet.nextSetBit(0); i >= 0 && i < itsSize; i = theBitSet.nextSetBit(i + 1))
					if (aUniqueValues.add(itsBinaries.get(i) ? "1" : "0"))
						if (aUniqueValues.size() == itsCardinality)
							break;
				break;
			}
			default :
			{
				logMessage("getUniqueNominalBinaryDomain",
						getTypeError("NOMINAL or BINARY"));
			}
		}

		return aUniqueValues.toArray(new String[0]);
	}

	/**
	 * Returns the unique, sorted domain for Columns of
	 * {@link AttributeType} {@link AttributeType#NUMERIC} and
	 * {@link AttributeType#ORDINAL}.
	 *
	 * The bits set in the BitSet supplied as argument indicate which values
	 * of the Column should be used for the creation of the domain.
	 * When the BitSet represents the members of a {@link Subgroup}, this
	 * method returns the domain covered by that Subgroup.
	 *
	 * The resulting float[] has a maximum size of the
	 * {@link java.util.BitSet#cardinality() BitSet.cardinality()}. The
	 * minimum size is 0, if the BitSet has cardinality 0 or is
	 * <code>null</code>.
	 *
	 * When the Column is not of {@link AttributeType}
	 * {@link AttributeType#NUMERIC} or {@link AttributeType#ORDINAL} a
	 * {@code new float[0]} will be returned.
	 *
	 * @param theBitSet the BitSet indicating what values of this Column to
	 * use for the creation of the domain.
	 *
	 * @return a float[], holding the distinct values for the domain.
	 *
	 * @see #getDomain()
	 * @see #getUniqueNominalBinaryDomain(BitSet)
	 * @see #getCardinality()
	 * @see AttributeType
	 * @see Subgroup
	 * @see java.util.BitSet
	 */
	public float[] getUniqueNumericDomain(BitSet theBitSet)
	{
		if (!isValidCall("getUniqueNumericDomain", theBitSet))
			return new float[0];

		// First create TreeSet<Float>, then copy to float[], not ideal,
		// but I lack the inspiration to write a RB-tree for floats
		Set<Float> aUniqueValues = new TreeSet<Float>();
		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
			aUniqueValues.add(itsFloatz[i]);

		float[] aResult = new float[aUniqueValues.size()];
		int i = -1;
		for (Float f : aUniqueValues)
			aResult[++i] = f.floatValue();
/*
		// FIXME MM use only this alternative
		int aNrMembers = theBitSet.cardinality();
		float[] aResult2 = new float[aNrMembers];
		for (int ii = 0, j = theBitSet.nextSetBit(0); j >= 0; j = theBitSet.nextSetBit(j + 1), ++ii)
			aResult2[ii] = itsFloatz[j];

		// sort
		Arrays.sort(aResult2);

		// filter duplicates, assume high cardinality
		int j = 0;
		// for tighter bound, but on Column.init (itsCardinality == 0)
		int min = (itsCardinality == 0) ?
					aNrMembers-1 :
					Math.min(aNrMembers, itsCardinality)-1;
		// k < aNrMembers not needed, j < min could be (--min != 0)
		for (int k = 1; k < aNrMembers && j < min; ++k)
			if (aResult2[k] != aResult2[j])
				aResult2[++j] = aResult2[k];
		// truncate everything after last unique value
		aResult2 = Arrays.copyOf(aResult2, j+1);
		assert (Arrays.equals(aResult, aResult2));
*/
		return aResult;
	}

	public SortedMap<Float, Integer> getUniqueNumericDomainMap(BitSet theBitSet)
	{
		if (!isValidCall("getUniqueNumericDomain", theBitSet))
			return null;

		SortedMap<Float, Integer> aUniqueValues = new TreeMap<Float, Integer>();
		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
		{
			Float f = Float.valueOf(itsFloatz[i]);
			Integer aCount = aUniqueValues.get(f);
			if (aCount == null)
				aCount = Integer.valueOf(0);

			aUniqueValues.put(f, Integer.valueOf(aCount+1));
		}

		return aUniqueValues;
	}

	/**
	 * Returns the split-points for Columns of
	 * {@link AttributeType} {@link AttributeType#NUMERIC} and
	 * {@link AttributeType#ORDINAL}, this method is used for the creation
	 * of equal-height bins ({@link NumericStrategy#NUMERIC_BINS} and
	 * {@link NumericStrategy#NUMERIC_BESTBINS}).
	 *
	 * The bits set in the BitSet supplied as argument indicate which values
	 * of the Column should be used for the creation of the split-points.
	 * When the BitSet represents the members of a {@link Subgroup}, this
	 * method returns the split-points relevant to that Subgroup.
	 *
	 * The resulting float[] has the size of the supplied theNrSplits
	 * parameter. If
	 * {@link java.util.BitSet#cardinality() theBitSet.cardinality()} is
	 * {@code 0}, the {@code float[theNrSplits]} will contain only
	 * {@code 0.0f}'s.
	 * If the BitSet is {@code null} a {@code new float[0]} is returned.
	 *
	 * When the Column is not of {@link AttributeType}
	 * {@link AttributeType#NUMERIC} or {@link AttributeType#ORDINAL} a
	 * {@code new float[0]} will be returned.
	 *
	 * @param theBitSet the BitSet indicating what values of this Column to
	 * use for the creation of the split-points.
	 *
	 * @param theNrSplits the number of split-point to return.
	 *
	 * @return a float[], holding the (possibly non-distinct) split-points.
	 *
	 * @throws IllegalArgumentException when theNrSplits < 0.
	 *
	 * @see AttributeType
	 * @see Subgroup
	 * @see NumericStrategy
	 * @see java.util.BitSet
	 */
	public float[] getSplitPoints(BitSet theBitSet, int theNrSplits) throws IllegalArgumentException
	{
		if (!isValidCall("getSplitPoints", theBitSet))
			return new float[0];

		if (theNrSplits < 0)
			throw new IllegalArgumentException(theNrSplits + " (theNrSplits) < 0");
		// valid, but useless
		if (theNrSplits == 0)
			return new float[0];

		final float[] aSplitPoints = new float[theNrSplits];
		final int size = theBitSet.cardinality();

		// FIXME MM
		// (size == 0) check is incorrect
		// it would return an array of 0's
		// and then 0 would be considered to be a bin boundary
		// more generally the code below leads to awkward results when
		// (theNrSplits > theBitSet.cardinality())
		// as the aSplitPoints[] will be populated with 0's
		// for every index >= theBitSet.cardinality(), the values will
		// not be set to anything else

		// prevent crash in aSplitPoints populating loop
		if (size == 0)
			return aSplitPoints;

		float[] aDomain = new float[size];
		for (int i = theBitSet.nextSetBit(0), j = -1; i >= 0; i = theBitSet.nextSetBit(i + 1))
			aDomain[++j] = itsFloatz[i];

		Arrays.sort(aDomain);

		// N.B. Order matters to prevent integer division from yielding zero.
		for (int j=0; j<theNrSplits; j++)
			aSplitPoints[j] = aDomain[size*(j+1)/(theNrSplits+1)];

		return aSplitPoints;
	}

	// FIXME MM - implement
	public SortedMap<Float, Integer> getSplitPointsMap(BitSet theBitSet, int theNrSplits) throws IllegalArgumentException
	{
		if (!isValidCall("getSplitPoints", theBitSet))
			return new TreeMap<Float, Integer>();

		if (theNrSplits < 0)
			throw new IllegalArgumentException(theNrSplits + " (theNrSplits) < 0");
		// valid, but useless
		if (theNrSplits == 0)
			return new TreeMap<Float, Integer>();

//		final float[] aSplitPoints = new float[theNrSplits];
//		final int size = theBitSet.cardinality();
//
//		// FIXME MM
//		// (size == 0) check is incorrect
//		// it would return an array of 0's
//		// and then 0 would be considered to be a bin boundary
//		// more generally the code below leads to awkward results when
//		// (theNrSplits > theBitSet.cardinality())
//		// as the aSplitPoints[] will be populated with 0's
//		// for every index >= theBitSet.cardinality(), the values will
//		// not be set to anything else
//
//		// prevent crash in aSplitPoints populating loop
//		if (size == 0)
//			return aSplitPoints;
//
//		float[] aDomain = new float[size];
//		for (int i = theBitSet.nextSetBit(0), j = -1; i >= 0; i = theBitSet.nextSetBit(i + 1))
//			aDomain[++j] = itsFloatz[i];
//
//		Arrays.sort(aDomain);
//
//		// N.B. Order matters to prevent integer division from yielding zero.
//		for (int j=0; j<theNrSplits; j++)
//			aSplitPoints[j] = aDomain[size*(j+1)/(theNrSplits+1)];
//
		return null;
		//return aSplitPoints;
	}

	/**
	 * Returns the consecutive Intervals for Columns of
	 * {@link AttributeType} {@link AttributeType#NUMERIC} and
	 * {@link AttributeType#ORDINAL}, this method is used for the creation
	 * of equal-height bounded intervals
	 * ({@link NumericStrategy#NUMERIC_VIKAMINE_CONSECUTIVE_ALL} and
	 * {@link NumericStrategy#NUMERIC_VIKAMINE_CONSECUTIVE_BEST}).
	 *
	 * The bits set in the BitSet supplied as argument indicate which values
	 * of the Column should be used for the creation of the Intervals.
	 * When the BitSet represents the members of a {@link Subgroup}, this
	 * method returns the Intervals relevant to that Subgroup.
	 *
	 * The resulting Interval[] has the size of the supplied theNrSplits
	 * parameter plus 1. If
	 * {@link java.util.BitSet#cardinality() theBitSet.cardinality()} is
	 * {@code 0}, the {@code Interval[theNrSplits+1]} will contain only
	 * {@code null}'s.
	 * If the BitSet is {@code null} a {@code new float[0]} is returned.
	 *
	 * When the Column is not of {@link AttributeType}
	 * {@link AttributeType#NUMERIC} or {@link AttributeType#ORDINAL} a
	 * {@code new Interval[0]} will be returned.
	 *
	 * @param theBitSet the BitSet indicating what values of this Column to
	 * use for the creation of the Intervals.
	 *
	 * @param theNrSplits the number of split-point between Intervals.
	 *
	 * @return an Interval[], holding the (possibly non-distinct) Intervals.
	 *
	 * @throws IllegalArgumentException when theNrSplits < 0.
	 *
	 * @see AttributeType
	 * @see Subgroup
	 * @see NumericStrategy
	 * @see java.util.BitSet
	 */
	public Interval[] getSplitPointsBounded(BitSet theBitSet, int theNrSplits) throws IllegalArgumentException
	{
		if (!isValidCall("getSplitPointsBounded", theBitSet))
			return new Interval[0];

		if (theNrSplits < 0)
			throw new IllegalArgumentException(theNrSplits + " (theNrSplits) < 0");
		// valid, but useless
		if (theNrSplits == 0)
			return new Interval[0];

		Interval[] aSplitPoints = new Interval[theNrSplits+1];
		final int size = theBitSet.cardinality();

		// FIXME MM
		// (size == 0) check is incorrect
		// it would return an array of 0's
		// and then 0 would be considered to be a bin boundary
		// more generally the code below leads to awkward results when
		// (theNrSplits > theBitSet.cardinality())
		// as the aSplitPoints[] will be populated with 0's
		// for every index >= theBitSet.cardinality(), the values will
		// not be set to anything else

		// prevent crash in aSplitPoints populating loop
		if (size == 0)
			return aSplitPoints;

		float[] aDomain = new float[size];
		for (int i = theBitSet.nextSetBit(0), j = -1; i >= 0; i = theBitSet.nextSetBit(i + 1))
			aDomain[++j] = itsFloatz[i];

		Arrays.sort(aDomain);

		// N.B. Order matters to prevent integer division from yielding zero.
		float lower = Float.NEGATIVE_INFINITY;
		float upper = Float.NaN; // theNrSplits is never 0 at this point
		for (int j=0; j<theNrSplits; j++)
		{
			// as before
			upper = aDomain[size*(j+1)/(theNrSplits+1)];

			// add Interval
			aSplitPoints[j] = new Interval(lower, upper);

			// set lower for next iteration
			lower = upper;
		}
		// add the final Interval - could be set in loop using if-check
		aSplitPoints[aSplitPoints.length-1] = new Interval(lower, Float.POSITIVE_INFINITY);

		return aSplitPoints;
	}

	/**
	 * Returns the average of all values for Columns of
	 * {@link AttributeType} {@link AttributeType#NUMERIC} and
	 * {@link AttributeType#ORDINAL}.
	 * <p>
	 * Note, this method has the risk of overflow, in which case
	 * {@link Float#NaN} is returned.
	 *
	 * @return the average, or {@link Float#NaN} if this Column
	 * is not of type {@link AttributeType#NUMERIC} or
	 * {@link AttributeType#ORDINAL}.
	 *
	 * @see #getStatistics(BitSet, boolean)
	 */
	public float getAverage()
	{
		if (itsType != AttributeType.NUMERIC)
			throw new IllegalArgumentException("Invalid call for AttriuteType: " + itsType);

		float aSum = 0.0f;
		for (float f : itsFloatz)
			aSum += f;

		return aSum / itsSize;
	}

	/**
	 * Returns the average label ranking
	 */
	public LabelRanking getAverageRanking(Subgroup theSubgroup)
	{
		if (itsType != AttributeType.NOMINAL)
		{
			logMessage("getAverageRanking", getTypeError("NOMINAL"));
			return null;
		}
		
		LabelRanking aResult = new LabelRanking(itsDistinctValues.get(itsNominalz[0]));
		int aSize = aResult.getSize(); //number of labels
		int[] aTotalRanks = new int[aSize];
		Arrays.fill(aTotalRanks, 0);

		BitSet aMembers = (theSubgroup == null) ? null : theSubgroup.getMembers();
		//summation of rankings (not necessary to divide by aSize, when you just look at the order)
		for (int i=0; i<itsSize; ++i)
			if (aMembers == null || aMembers.get(i)) //part of the subgroup?
			{
				String aValue = itsDistinctValues.get(itsNominalz[i]);
				LabelRanking aRanking = new LabelRanking(aValue);
				for (int j=0; j<aSize; j++)
					aTotalRanks[j] += aRanking.getRank(j);
			}

		//make copy that can be sorted
		int[] aRanks = new int[aSize];
		for (int i=0; i<aSize; i++)
			aRanks[i] = aTotalRanks[i];
		Arrays.sort(aRanks);

		//translate average ranks to a ranking
		for (int i=0; i<aSize; i++)
		{
			int aLookup = aTotalRanks[i];
			int aFirst = -1;
			int aLast = -1;
			//look up rank for this average
			for (int j=0; j<aSize; j++)
				if (aLookup == aRanks[j])
				{
					if (aFirst >= 0)
						aLast = j;
					aFirst = j;
				}
			//TODO: properly deal with ties in the ranking
			aResult.setRank(i, aFirst);
		}
//		for (int i=0; i<itsSize; i++)
//		{
//			itsIndex[itsRanking[i]] += getLetter(i);
//		}

		return aResult;
	}

	/**
	 * Returns the average label ranking, as a LabelRankingMatrix
	 */
	public LabelRankingMatrix getAverageRankingMatrix(Subgroup theSubgroup)
	{
		if (itsType != AttributeType.NOMINAL)
		{
			logMessage("getAverageRankingMatrix", getTypeError("NOMINAL"));
			return null;
		}

		//take the size of the first example as the total number of labels
//		LabelRankingMatrix aResult = new LabelRankingMatrix(itsNominals.get(0).length());
		LabelRankingMatrix aResult = new LabelRankingMatrix(itsDistinctValues.get(itsNominalz[0]).replace(">","").length());
		// equivalent to below, as itsNominalz[0] should always be 0
		//LabelRankingMatrix aResult = new LabelRankingMatrix(itsDistinctValues.get(0).length());
		int aCount = 0;

		BitSet aMembers = (theSubgroup == null) ? null : theSubgroup.getMembers();
		//summation of rankings
		for (int i=0; i<itsSize; ++i)
			if (aMembers == null || aMembers.get(i)) //part of the subgroup?
			{
//				String aValue = itsNominals.get(i);
				String aValue = itsDistinctValues.get(itsNominalz[i]);
				LabelRanking aRanking = new LabelRanking(aValue);
				LabelRankingMatrix aRankingMatrix = new LabelRankingMatrix(aRanking); //translate to LRM
				aResult.add(aRankingMatrix);
				aCount++;
			}
		aResult.divide(aCount); //divide by zero is not possible, as subgroups always have members
		
		return aResult;
	}

	public LabelRankingMatrix getAverageRankingMatrix0(Subgroup theSubgroup)
	{
		if (itsType != AttributeType.NOMINAL)
		{
			logMessage("getAverageRankingMatrix", getTypeError("NOMINAL"));
			return null;
		}

		//take the size of the first example as the total number of labels
//		LabelRankingMatrix aResult = new LabelRankingMatrix(itsNominals.get(0).length());
//		LabelRankingMatrix aResult = new LabelRankingMatrix(itsDistinctValues.get(itsNominalz[0]).replace(">","").length());
//		LabelRankingMatrix minusOnesMat = onesMat;
//		LabelRankingMatrix zerosMat = onesMat;
		// equivalent to below, as itsNominalz[0] should always be 0
		//LabelRankingMatrix aResult = new LabelRankingMatrix(itsDistinctValues.get(0).length());
		//int aCount = 0;

		BitSet aMembers = (theSubgroup == null) ? null : theSubgroup.getMembers();
		//summation of rankings
		
		int rankSize = itsDistinctValues.get(itsNominalz[0]).replace(">","").length();
		LabelRankingMatrix LRmode = new LabelRankingMatrix(rankSize);
		
		
		
		int[][][] theModeMatrix = new int[3][rankSize][rankSize];
		for (int s=0; s<3; s++)
			for (int i=0; i<rankSize; i++)
				for (int j=0; j<rankSize; j++)
					theModeMatrix[s][i][j] = 0;
		
		for (int k=0; k<itsSize; ++k)
			if (aMembers == null || aMembers.get(k)) //part of the subgroup?
			{
//				String aValue = itsNominals.get(i);
				String aValue = itsDistinctValues.get(itsNominalz[k]);
				LabelRanking aRanking = new LabelRanking(aValue);
				LabelRankingMatrix aRankingMatrix = new LabelRankingMatrix(aRanking); //translate to LRM
				
				int[][][] aModeMatrix = aRankingMatrix.getModeMatrix();
				
				for (int s=0; s<3; s++)
					for (int i=0; i<rankSize; i++)
						for (int j=0; j<rankSize; j++)
							theModeMatrix[s][i][j] += aModeMatrix[s][i][j];
				
			}
		
		for (int i=0; i<rankSize; i++)
			for (int j=0; j<rankSize; j++) {
				if (theModeMatrix[0][i][j] > theModeMatrix[1][i][j]){
					if (theModeMatrix[0][i][j] >  theModeMatrix[2][i][j])
						LRmode.itsMatrix[i][j] = -1;
					else
						LRmode.itsMatrix[i][j] = 1;
				} else {
					if (theModeMatrix[1][i][j] >  theModeMatrix[2][i][j])
						LRmode.itsMatrix[i][j] = 0;
					else
						LRmode.itsMatrix[i][j] = 1;
				}
			}
		
		return LRmode;
	}
	

	
	/**
	 * Returns the number of times the value supplied as parameter occurs in
	 * the Column. This method only works on Columns of type
	 * {@link AttributeType} {@link AttributeType#NOMINAL} and
	 * {@link AttributeType#BINARY}.
	 * <p>
	 * For Columns of {@link AttributeType} {@link AttributeType#BINARY},
	 * use the String "1" for {@code true}, all other values are considered
	 * to represent {@code false}.
	 *
	 * @param theValue the value to count the number of occurrences for.
	 *
	 * @return an {@code int} indicating the number of occurrences of
	 * the value supplied as parameter, or {@code 0} if this Column is not
	 * of type NOMINAL or BINARY.
	 *
	 * @see #getDomain()
	 * @see #getCardinality()
	 * @see AttributeType
	 */
	public int countValues(String theValue)
	{
		switch (itsType)
		{
			case NOMINAL :
			{
				int aResult = 0;
//				for (String s : itsNominals)
//					if (s.equals(theValue))
//						++aResult;
				Integer v = itsDistinctValuesMap.get(theValue);
				if (v == null)
				{
					// actually AssertionError(), but public
					Log.logCommandLine(theValue + " does not occur in Column " + itsName);
					return 0;
				}

				int value = v;
				for (int i = 0, j = itsNominalz.length; i < j; ++i)
					if (itsNominalz[i] == value)
						++aResult;
				return aResult;
			}
			case BINARY :
			{
				return "1".equals(theValue) ?
						itsBinaries.cardinality() :
						itsSize - itsBinaries.cardinality();
			}
			default :
			{
				logMessage("countValues", getTypeError("NOMINAL or BINARY"));
				return 0;
			}
		}
	}

	private boolean isValidCall(String theSource, BitSet theBitSet)
	{
		String anError = null;
		if (!(itsType == AttributeType.NUMERIC || itsType == AttributeType.ORDINAL))
			anError = getTypeError("NUMERIC or ORDINAL");
		else if (theBitSet == null)
			anError = "Argument can not be 'null'";
		else if (theBitSet.length() > itsSize)
			anError = String.format("BitSet can not be bigger then: %s", itsSize);

		if (anError != null)
			logMessage(theSource, anError);
		return (anError == null);
	}

	private String getTypeError(String theValidType)
	{
		return String.format("Column can not be of type: %s, must be %s", itsType, theValidType);
	}

	private void logMessage(String theSource, String theError)
	{
		Log.logCommandLine(String.format("%s.%s(): %s", itsName, theSource, theError));
	}

	/*
	 * NOTE No checks on (itsType == AttributeType.NOMINAL), use with care.
	 *
	 * Avoids recreation of TreeSet aDomain in
	 * SubgroupDiscovery.evaluateNominalBinaryRefinement().
	 * Memory usage is minimal.
	 *
	 * Bits set in i represent value-indices go retrieve from
	 * itsDistinctValues. This just works.
	 */
	public String[] getSubset(int i) {
		// Don Clugston approves
		// count bits set in integer type (12 ops instead of naive 32)
		// http://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetParallel
		int v = i;
		v = v - ((v >> 1) & 0x55555555);			// reuse input as temporary
		v = (v & 0x33333333) + ((v >> 2) & 0x33333333);		// temp
		v = ((v + (v >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;	// count

		String[] aResult = new String[v];
		for (int j = -1, k = v, m = itsDistinctValues.size()-1; k > 0; --m)
			if (((i >>> ++j) & 1) == 1) // so no shift in first loop
				aResult[--k] = itsDistinctValues.get(m);

		return aResult;
	}
}
