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
	// three structures for fast contains() and get() test
	// itsDistinctValues and itsDistinctValuesMap should have the same data
	// but ordered and unordered , respectively
	// itsNominalz as int[] allows for (int == int) comparison instead of
	// String.equals()
	// storage tradeoff lies around (n == 16c, plus some implementation type
	// dependent storage requirement)
	// XXX MM - merge itsDistinctValues and itsDistinctValuesMap
	private int[] itsNominalz;
	private BitSet itsBinaries;
	private List<String> itsDistinctValues;
	private Map<String, Integer> itsDistinctValuesMap;

	private BitSet itsMissing = new BitSet();
	private boolean itsMissingValueIsUnique = true;
	private int itsSize = 0;
	private int itsCardinality = 0;
	private float itsMin = Float.POSITIVE_INFINITY;
	private float itsMax = Float.NEGATIVE_INFINITY;
	private boolean isEnabled = true;

	// update: all NUMERIC Conditions now set sort index, Column.evaluate() relies on it
	// Condition.UNINTIALISE_itsSortIndex is Integer.MIN_VALUE, such that it is outside of the range valid sort indexes (MIN_VALUE < -MAX_VALUE)
	// This also means bit invert is no longer even a possibility
	private static final int MASK_ON  = 0x80000000;
	private static final int MASK_OFF = 0x7fffffff;
	private float[] itsSortedFloats;
	private int[] itsSortIndex;

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
				itsNominalz = new int[theNrRows];
				itsDistinctValues = new ArrayList<String>();
				itsDistinctValuesMap = new HashMap<String, Integer>(MAP_DEFAULT_INIT_SIZE);
				return;
			}
			case NUMERIC :
			{
				itsFloatz = new float[theNrRows];
				return;
			}
			case ORDINAL :
			{
				throw new AssertionError(itsType);
			}
			case BINARY :
			{
				itsBinaries = new BitSet(theNrRows);
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
			else if ("enabled".equalsIgnoreCase(aNodeName))
				isEnabled = Boolean.valueOf(aSetting.getTextContent());
			else
				throw new AssertionError("unknown Node: " + aNodeName);
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
		XMLNode.addNodeTo(aNode, "enabled", isEnabled);
	}

	public Column copy()
	{
		Column aCopy = new Column(itsName, itsShort, itsType, itsIndex, itsSize);
		aCopy.itsFloatz = itsFloatz;
		aCopy.itsNominalz = itsNominalz;
		aCopy.itsBinaries = itsBinaries;
		aCopy.itsDistinctValues = itsDistinctValues;
		aCopy.itsDistinctValuesMap = itsDistinctValuesMap;
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
		aColumn.itsSize = aColumnSize;
		aColumn.isEnabled = this.isEnabled;
		aColumn.itsTargetStatus = this.itsTargetStatus;

		switch (itsType)
		{
			case NOMINAL :
			{
				aColumn.itsNominalz = new int[aColumnSize];
				//preferred way to loop over BitSet (itsSize for safety)
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

	public void addMissing()
	{
		setMissing(itsSize);
		switch (itsType)
		{
			case NOMINAL :
			{
				add("?");
				break;
			}
			case NUMERIC :
			{
				add(Float.NaN);
				break;
			}
			case BINARY :
			{
				add(false);
				break;
			}
			default :
			{
				logTypeError("Column.addMissing()");
				throw new AssertionError(itsType);
			}
		}
	}

	/**
	 * Appends the specified element to the end of this Column.
	 *
	 * Always call {@link #close()} after the last element is added to this
	 * Column.
	 *
	 * @param theNominal the value to append to this Column.
	 */
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
		itsSize++;
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
		itsSize++;
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
		itsSize++;
	}

	/**
	 * Always call this method after creating a Column.
	 */
	public void close()
	{
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
		if (itsType == theAttributeType)
			return true;

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
	 */
	private boolean toNominalType()
	{
		assert (itsType != AttributeType.NOMINAL);

		itsDistinctValues = new ArrayList<String>(itsCardinality);
		itsDistinctValuesMap = new HashMap<String, Integer>(itsCardinality);
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
					for (int i = 0, j = itsNominalz.length; i < j; ++i)
						this.add(new String(Float.toString(itsFloatz[i]).split(".")[0]));
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
	 */
	boolean toNominalType(String aTrue, String aFalse)
	{
		assert itsType == AttributeType.BINARY;

		// relies on itsCardinality to be set at this time
		itsDistinctValues = new ArrayList<String>(itsCardinality);
		itsDistinctValuesMap = new HashMap<String, Integer>(itsCardinality);
		itsNominalz = new int[itsSize];

		// NOTE uses this.add(String) to populate itsDistinctValues(Map), requires itsSize = 0;
		itsSize = 0;

		// just missing values so far
		if (aTrue == null && aFalse == null)
			for (int i = 0; i < itsNominalz.length; i++)
				this.add("?");
		else
			for (int i = 0; i < itsNominalz.length; i++)
				this.add(itsBinaries.get(i) ? aTrue : aFalse);

		// Cleanup (for GarbageCollector).
		itsBinaries = null;
		itsType = AttributeType.NOMINAL;
		return true;
	}

	/*
	 * Switching between Column AttributeTypes of NUMERIC and ORDINAL is always possible, without any other further changes.
	 * Changing from a BINARY AttributeType is also possible.
	 * Changing from a NOMINAL AttributeType is only possible if all values in itsNominals can be parsed as Floats.
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
					try
					{
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
				//Only gets here if all values are parsed successfully, (or itsSize == 0). Cleanup (for GarbageCollector).
				itsDistinctValues = null;
				itsDistinctValuesMap = null;
				itsNominalz = null;

				if (itsMissing.cardinality() == 0)
				{
					itsMissingValueIsUnique = true;
					for (float f : itsFloatz)
						if (Float.isNaN(f))
						{
							itsMissingValueIsUnique = false;
							break;
						}
				}

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
				itsBinaries = new BitSet(itsSize);

				if (itsSize > 0)
				{
					// String aValue = itsNominals.get(0);
					int v = itsNominalz[0];
					String aValue = itsDistinctValues.get(v);

					if (AttributeType.isValidBinaryTrueValue(aValue))
					{
						for (int i = 0; i < itsSize; ++i)
							if (v == itsNominalz[i])
								itsBinaries.set(i);
					}
					else if (AttributeType.isValidBinaryFalseValue(aValue))
					{
						for (int i = 0; i < itsSize; ++i)
							if (v != itsNominalz[i])
								itsBinaries.set(i);
					}
					//sets all non-missing to 'true'
					else if (aValue.equals("?"))
					{
						if (itsMissing.cardinality() < itsSize && AttributeType.isValidBinaryTrueValue(itsDistinctValues.get(itsNominalz[itsMissing.nextClearBit(0)])))
							for (int i = itsMissing.nextClearBit(0); i >= 0 && i < itsSize; i = itsMissing.nextClearBit(i+1))
								itsBinaries.set(i);
					}
					//sets all itsNominals.get(0) values to 'true'
					else
					{
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

				//translate all values other than zero to true
				for (int i = 0; i < itsSize; i++)
					itsBinaries.set(i, itsFloatz[i] != 0.0f);

				itsFloatz = null;
				break;
			}
			default :
			{
				logTypeError("Column.toBinaryType()");
				throw new AssertionError(itsType);
			}
		}

		//set all missing to false
		for (int i = itsMissing.nextSetBit(0); i >= 0; i = itsMissing.nextSetBit(i + 1))
			itsBinaries.clear(i);

		updateCardinality();

		itsType = AttributeType.BINARY;
		return true;
	}

	private void logTypeError(String theMethodName)
	{
		Log.logCommandLine(String.format("Error in %s: Column '%s' has AttributeType '%s'.", theMethodName, getName(), itsType));
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

	public boolean getMissing(int theIndex) { return itsMissing.get(theIndex); }

	/**
	 * Sets the bit at the specified position in the itsMissing BisSet.
	 * @param theIndex the bit to set in the itsMissing BitSet.
	 */
	public void setMissing(int theIndex) { itsMissing.set(theIndex); }

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
				for (int i = 0; i < itsSize; i++)
				{
					if (Float.isNaN(itsFloatz[i]) && !itsMissing.get(i))
					{
						itsMissingValueIsUnique = false;
						break;
					}
				}

				itsCardinality = Function.getCardinality(itsFloatz);
				return itsCardinality;
			}
			case ORDINAL :
				throw new AssertionError(itsType);
			case BINARY :
			{
				for (int i = itsBinaries.nextClearBit(0); i >= 0 && i < itsSize; i = itsBinaries.nextClearBit(i + 1))
				{
					if (!itsMissing.get(i))
					{
						itsMissingValueIsUnique = false;
						break;
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

/*	private void updateCardinality(List<?> theColumnData)
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
*/

	private void updateCardinality()
	{
		// not set yet, or no data
		if (itsCardinality == 0)
			getCardinality();
		else
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
			throw new IllegalArgumentException(String.format("%s does not apply to %s", theCondition.toString(), getName()));

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

				// the Condition constructor with float always sets sort index
				// the Condition constructor with Interval does not
				switch (anOperator)
				{
					case EQUALS :
						numericEquals(theBitSet, theCondition.getNumericValue(), theCondition.getSortIndex(), aResult);
						break;
					case LESS_THAN_OR_EQUAL :
						numericLEQ(theBitSet, theCondition.getNumericValue(), theCondition.getSortIndex(), aResult);
						break;
					case GREATER_THAN_OR_EQUAL :
						numericGEQ(theBitSet, theCondition.getNumericValue(), theCondition.getSortIndex(), aResult);
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
				assert (anOperator == Operator.EQUALS);
				aResult =  evaluateBinary(theBitSet, theCondition.getBinaryValue());
				break;
			}
			default :
			{
				logMessage("evaluate", String.format("unknown AttributeType '%s'", itsType));
				throw new AssertionError(itsType);
			}
		}


		return aResult;
	}

	// this replaces the BINARY part of evaluate(BitSet, Condition) above and
	// of getUniqueNominalBinaryDomain(): the two methods are merged
	// instead of determining the distinct values for this Column, it directly
	// returns the evaluation for the members BitSet and this Column
	// the old situation would first determine the domain { false, true }, and
	// then call Column.evaluate(BitSet, Condition)
	// and then use that to set the Subgroup members, using this method this can
	// be done directly, using the returned BitSet
	// also, for the special SINGLE_NOMINAL case, the caller can determine the
	// true positive count for the Subgroup by calling aResult.and(binaryTarget)
	//
	// NOTE
	// when (column.itsCardinality != 2) this method should not be called when
	// evaluating a Refinement during mining, as for a cardinality of 0 there
	// there are no Refinements, for 1 theBitSet selects the oldCoverage
	//
	// could be replaced by calls to evaluate(BitSet, Condition) (less code)
	// but the extra parameter checks and switch might reduce performance a lot
	final BitSet evaluateBinary(BitSet theBitSet, boolean theValue)
	{
		assert (itsType == AttributeType.BINARY);

		BitSet aResult;
		if (theValue)
		{
			aResult = (BitSet) itsBinaries.clone();
			aResult.and(theBitSet);
			return aResult;
		}
		else
		{
			aResult = (BitSet) theBitSet.clone();
			aResult.andNot(itsBinaries);
			return aResult;
		}
	}

	// account for d>1, where _new will not be the same as old
	// every bit set in new, must also be set in old, and no more
	@SuppressWarnings("unused") // keep, evaluate() will change, and be tested
	private static final boolean postTest(BitSet _new, BitSet old)
	{
		// do not modify _new, as it is needed as result
		BitSet b = (BitSet)_new.clone();
		b.andNot(old);
		return b.isEmpty();
	}

	private BitSet nominalElementOf(BitSet theMembers, ValueSet theValueSet, BitSet theResult)
	{
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
		// TODO set index in Condition: not even the HashMap lookup is required
		int v = itsDistinctValuesMap.get(theValue);
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			if (itsNominalz[i] == v && !getMissing(i))					//note that this removes X = '?' from the results, intentionally
				theResult.set(i);

		return theResult;
	}

	private BitSet numericEquals(BitSet theMembers, float theValue, BitSet theResult)
	{
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			if (itsFloatz[i] == theValue && !getMissing(i))
				theResult.set(i);

		return theResult;
	}

	// only SINGLE_NOMINAL needs MASK_OFF, unlikely to be a performance problem
	private BitSet numericEquals(BitSet theMembers, float theValue, int theValueSortIndex, BitSet theResult)
	{
		if ((itsSortIndex != null) && (theValueSortIndex != Condition.UNINITIALISED_SORT_INDEX))
		{
			for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
				if ((MASK_OFF & itsSortIndex[i]) == theValueSortIndex && !getMissing(i))
					theResult.set(i);
		}
		else
		{
			for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
				if (itsFloatz[i] == theValue && !getMissing(i))
					theResult.set(i);
		}

		return theResult;
	}

	private BitSet numericLEQ(BitSet theMembers, float theValue, int theValueSortIndex, BitSet theResult)
	{
		if ((itsSortIndex != null) && (theValueSortIndex != Condition.UNINITIALISED_SORT_INDEX))
		{
			for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
				if ((MASK_OFF & itsSortIndex[i]) <= theValueSortIndex && !getMissing(i))
					theResult.set(i);
		}
		else
		{
			for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			{
				if (itsFloatz[i] <= theValue && !getMissing(i))
					theResult.set(i);
			}
		}

		return theResult;
	}

	private BitSet numericGEQ(BitSet theMembers, float theValue, int theValueSortIndex, BitSet theResult)
	{
		if ((itsSortIndex != null) && (theValueSortIndex != Condition.UNINITIALISED_SORT_INDEX))
		{
			for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
				if ((MASK_OFF & itsSortIndex[i]) >= theValueSortIndex && !getMissing(i))
					theResult.set(i);
		}
		else
		{
			for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
				if (itsFloatz[i] >= theValue && !getMissing(i))
					theResult.set(i);
		}
		return theResult;
	}

	private BitSet numericBetween(BitSet theMembers, Interval theInterval, BitSet theResult)
	{
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
			if (theInterval.between(itsFloatz[i]) && !getMissing(i))
				theResult.set(i);

		return theResult;
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
		int aNrMembers = theBitSet.cardinality();
		if (aNrMembers == 0)
			return new Statistics();

		final Statistics aResult;
		// if (!MMAD) do not store and sort the values, else do
		if (!getMedianAndMedianAD)
		{
			float aSum = computeSum(theBitSet, itsFloatz);
			aResult = new Statistics(aNrMembers, aSum, computeSumSquaredDeviations(aSum, aNrMembers, theBitSet, itsFloatz));
		}
		else
		{
			// this code path needs a copy of the data for median
			// get relevant values and do summing in single loop
			float aSum = 0.0f;
			float[] aValues = new float[aNrMembers];
			for (int i = theBitSet.nextSetBit(0), j = -1; i >= 0; i = theBitSet.nextSetBit(i + 1))
				aSum += (aValues[++j] = itsFloatz[i]);
			Arrays.sort(aValues);
			float aMedian = computeMedian(aValues);

			aResult = new Statistics(aNrMembers,
				aSum,
				computeSumSquaredDeviations(aSum, aValues),
				aMedian,
				computeMedianAbsoluteDeviations(aMedian, aValues));
		}

		if (addComplement)
		{
			// XXX MM - do not clone(), use clear bits in theBitSet
			BitSet aComplement = (BitSet) theBitSet.clone();
			aComplement.flip(0, itsSize);
			int aNrComplementMembers = (itsSize - aNrMembers);
			assert (aNrComplementMembers == aComplement.cardinality());
			float aComplementSum = computeSum(aComplement, itsFloatz);
			aResult.addComplement(aNrComplementMembers, aComplementSum, computeSumSquaredDeviations(aComplementSum, aNrComplementMembers, aComplement, itsFloatz));

			aResult.addDatasetSSD(computeSumSquaredDeviations(aResult.getAverage()));
			// XXX MM - computeSumSquaredDeviations(sum, float[])
			//aResult.addDatasetSSD(computeSumSquaredDeviations((aResult.getSubgroupSum() + aComplementSum), itsFloatz));
		}

		return aResult;
	}

	private static final float computeSum(BitSet theBitSet, float[] theFloats)
	{
		float aSum = 0.0f;
		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
			if (!Float.isNaN(theFloats[i]))
				aSum += theFloats[i];
		return aSum;
	}

	// always called after computeSum for !MMAD
	// not safe for divide by 0 (theBitSet.cardinality() == 0)
	// uses theNrMembers, as theBitSet.cardinality() is expensive
	private static final float computeSumSquaredDeviations(float theSum, int theNrMembers, BitSet theBitSet, float[] theFloats)
	{
		assert (theNrMembers == theBitSet.cardinality());

		int aCount = 0;
		float aSum = 0f;
		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
			if (!Float.isNaN(theFloats[i]))
			{
				aSum += theFloats[i];
				aCount++;
			}
		float aMean = aSum / aCount;

		aSum = 0.0f;
		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
			aSum += squared(theFloats[i] - aMean);
		return aSum;
	}

	// for all data, not just subgroup
	// XXX MM only used by one method, merge with code below
	@Deprecated
	private float computeSumSquaredDeviations(float theMean)
	{
		float aSum = 0.0f;
		for (float f : itsFloatz)
			aSum += squared(f - theMean);
		return aSum;
	}

	// always called after computeSum for MMAD
	// not safe for divide by 0 (theFloats.length == 0)
	private static final float computeSumSquaredDeviations(float theSum, float[] theFloats)
	{
		float aMean = theSum / theFloats.length;
		float aSum = 0.0f;
		for (float f : theFloats)
			aSum += squared(f - aMean);
		return aSum;
	}

	// always called after computeSumSquaredDeviations for MMAD
	// throws indexOutOfBoundsException if (theSortedValues.length == 0)
	private static final float computeMedian(float[] theSortedValues)
	{
		int aLength = theSortedValues.length;
		// even, bit check (even numbers end with 0 as last bit)
		if ((aLength & 1) == 0)
			return (theSortedValues[(aLength/2)-1] + theSortedValues[aLength/2]) / 2;
		else
			return theSortedValues[aLength/2];
	}

	// always called after computeMedian for MMAD
	// may re-throw computeMedian()'s indexOutOfBoundsException
	private static final float computeMedianAbsoluteDeviations(float theMedian, float[] theSortedValues)
	{
		// compute absolute deviations of the elements in the subgroup
		// store these in the original array for efficiency
		for (int i = 0, j = theSortedValues.length; i < j; ++i)
			theSortedValues[i] = Math.abs(theSortedValues[i]-theMedian);

		//compute the MAD: the median of absolute deviations
		Arrays.sort(theSortedValues);
		return computeMedian(theSortedValues);
	}

	private static final float squared(float theFloat)
	{
		return (theFloat * theFloat);
	}

	/**
	 * Returns a {@code java.util.TreeSet} with all distinct values for this
	 * Column, with values ordered according to their natural ordering.
	 *
	 * @return the domain for this Column.
	 *
	 * @see #getUniqueNominalDomainCounts(BitSet, int)
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
				// FIXME profile: probably the following is faster:
				//       for (float f : Function.getUniqueValues(theArray))
				//         aResult.add(Float.toString(f));
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

	// callers obtain an int[] with counts for each value for this Column
	// counts can be zero, the index of non-zero counts should be used get the
	// corresponding value from column.itsDistinctValuesU(non_zero_count_index)
	// this uses aCollection.unmodifiableList<String> for the domain to avoids
	// creating Objects, sharing is safe, and the JVM knows it will not change
	List<String> itsDistinctValuesU = null;
	void buildSharedDomain() { itsDistinctValuesU = Collections.unmodifiableList(itsDistinctValues); }
	int[] getUniqueNominalDomainCounts(BitSet theBitSet, int theBitSetCardinality)
	{
		// not a public method, caller should ensure (theBitSetCardinality > 1)
		// as else no valid Refinement can be created
		// the cardinality() call is relatively expensive, and should only be
		// performed once for the parent BitSet
		assert (theBitSetCardinality > 1);
		assert (theBitSetCardinality == theBitSet.cardinality());

		// NOTE (itsCardinality == 0) when Column.close() is not called or
		// on empty data (occurs when the FileLoader can not load a file)
		if (itsCardinality == 0)
			return new int[] { 0 };

		// only one value in domain, obviously all members cover this value
		if (itsCardinality == 1)
			return new int[] { theBitSetCardinality, 1 };

		int aNrDistinct = 0;
		int[] aCounts = new int[itsCardinality + 1];

		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
			if ((++aCounts[itsNominalz[i]]) == 1)
				++aNrDistinct;

		// XXX WARNING
		// add aNrDistinct to aCounts at [itsCardinality+1], the danger is that
		// (itsCardinality+1) overflows int, indexing at a negative index
		aCounts[itsCardinality] = aNrDistinct;

		return aCounts;
	}

	static final class DomainMapNumeric
	{
		final int itsSize;       // itsSize indicates end-of-valid-input
		final int itsCountsSum;  // required only for BEST_BINS|BINS
		final float[] itsDomain; // values at [>= itsSize] are invalid, ignore them
		final int[] itsCounts;   // -1 at [itsSize] indicates end-of-valid-input

		DomainMapNumeric(int theSize, int theCountsSum, float[] theDomain, int[] theCounts)
		{
			itsSize = theSize;
			itsCountsSum = theCountsSum;
			itsDomain = theDomain;
			itsCounts = theCounts;
		}
	}

	// NOTE values itsDomain are unique up to itsCounts sentinel of -1
	public DomainMapNumeric getUniqueNumericDomainMap(BitSet theBitSet, int theBitSetCardinality)
	{
		assert (theBitSetCardinality == theBitSet.cardinality());

		if (!isValidCall("getUniqueNumericDomainMap", theBitSet))
			return null;

		float[] aDomain = new float[theBitSetCardinality];
		int[] aCounts = new int[theBitSetCardinality];
		for (int i = 0, j = theBitSet.nextSetBit(0); j >= 0; j = theBitSet.nextSetBit(j + 1), ++i)
			aDomain[i] = itsFloatz[j];

		// sort
		Arrays.sort(aDomain);

		float x = Float.NaN; // comparison is always false
		int idx = -1;
		for (int k = 0; k < theBitSetCardinality; ++k)
		{
			if (aDomain[k] != x)
			{
				x = aDomain[k];
				aDomain[++idx] = x;
			}

			++aCounts[idx];
		}
		if (idx < aCounts.length-1)
			aCounts[idx+1] = -1;

		return new DomainMapNumeric(idx+1, theBitSetCardinality, aDomain, aCounts);
	}

	public final void buildSorted(BitSet theTarget)
	{
		boolean isTargetNull = (theTarget == null);

		itsSortedFloats = Function.getUniqueValues(itsFloatz);	//this might include a NaN at the end in case of missing values
//		if (Float.isNaN(aSortedFloats[aSortedFloats.length-1]))		//are there missing values (placed at the end by getUniqueValues)?
//		{
//			itsSortedFloats = new float[aSortedFloats.length-1];
//			for (int i=0; i<aSortedFloats.length-1; i++)
//				itsSortedFloats[i] = aSortedFloats[i];
//		}
//		else
//			itsSortedFloats = aSortedFloats;
		
		// determine sort-index for each value in Column.itsFloatz
		itsSortIndex = new int[itsFloatz.length];
		for (int i = 0; i < itsFloatz.length; ++i)
		{
			int idx = Arrays.binarySearch(itsSortedFloats, itsFloatz[i]);
			itsSortIndex[i] = (isTargetNull || theTarget.get(i)) ? idx : (MASK_ON | idx);
		}
	}

	final void removeSorted() { itsSortedFloats = null; itsSortIndex = null; }

	public final float getSortedValue(int index) { return itsSortedFloats[index]; }

	public final int getSortedIndex(float theFloat) { return Arrays.binarySearch(itsSortedFloats, theFloat); }

	final float[] getSortedValuesCopy() { return Arrays.copyOf(itsSortedFloats, itsSortedFloats.length); }

	// a more generic version of this type of classes will follow later
	public static final class ValueCount
	{
		public final int[] itsCounts; // of size column.cardinality

		private ValueCount(int[] theCounts)
		{
			itsCounts = theCounts;
		}
	}

	static final class ValueCountTP
	{
		final int[] itsCounts;        // of size column.cardinality
		final int[] itsTruePositives; // of size column.cardinality
		final int itsMissingCount;
		final int itsMissingPositiveCount;

		private ValueCountTP(int[] theCounts, int[] theTruePositives, int theMissingCount, int theMissingPositiveCount)
		{
			itsCounts = theCounts;
			itsTruePositives = theTruePositives;
			itsMissingCount	= theMissingCount;
			itsMissingPositiveCount = theMissingPositiveCount;
		}
	}

	//TODO keep track of missing values (like above)
	static final class ValueCountSum
	{
		final int[]    itsCounts;     // of size column.cardinality
		final double[] itsSums;       // of size column.cardinality

		private ValueCountSum(int[] theCounts, double[] theSums)
		{
			itsCounts        = theCounts;
			itsSums          = theSums;
		}
	}

	// for t-statistic, maybe for explained variance
	static final class ValueCountSumSquaresSum
	{
		final int[]    itsCounts;      // of size column.cardinality
		final double[] itsSums;        // of size column.cardinality
		final double[] itsSquaresSums; // of size column.cardinality

		private ValueCountSumSquaresSum(int[] theCounts, double[] theSums, double[] theSquaresSums)
		{
			itsCounts        = theCounts;
			itsSums          = theSums;
			itsSquaresSums   = theSquaresSums;
		}
	}

	public ValueCount getValueCount(BitSet theBitSet)
	{
		if (!isValidCall("getValueCount", theBitSet))
			return new ValueCount(new int[0]);

		// NOTE (itsSortedFloats.length == itsCardinality)
		int[] aCnt = new int[itsSortedFloats.length];

		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
			++aCnt[itsSortIndex[i]];

		return new ValueCount(aCnt);
	}

	ValueCountTP getUniqueNumericDomainMap(BitSet theBitSet)
	{
		if (!isValidCall("getUniqueNumericDomainMap", theBitSet))
			return new ValueCountTP(new int[0], new int[0], 0, 0);

		int[] aCnt = new int[itsSortedFloats.length];
		int[] aPos = new int[itsSortedFloats.length];
		int aMissingCount = 0;
		int aMissingPositiveCount = 0;

//		System.out.println("count: " + theBitSet.cardinality());
		int c = 0;
		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
			if (!getMissing(i))
			{
				int idx = itsSortIndex[i];
				if (idx >= 0) //it's a positive example
				{
					++aCnt[idx];
					++aPos[idx];
					c++;
				}
				else
					++aCnt[(MASK_OFF & idx)];
			}
			else
			{
				aMissingCount++;
				//TODO: this doesn't work correctly
				if (itsSortIndex[i] >= 0) //it's a positive example
					aMissingPositiveCount++;
			}
//		System.out.println("pos count: " + c);
//		int aC = 0;
//		int aP = 0;
//		for (int i=0; i<aCnt.length; i++)
//		{
//			aC += aCnt[i];
//			aP += aPos[i];
//			System.out.println("---" + i + ", " + itsSortedFloats[i] + ", " + aCnt[i] + ", " + aPos[i] + ", " + aC + ", " + aP);
//		}
//		System.out.println("Missing: " + aMissingCount + ", positive: " + aMissingPositiveCount);

		return new ValueCountTP(aCnt, aPos, aMissingCount, aMissingPositiveCount);
	}

	//TOD fix this for missing values
	ValueCountSum getUniqueNumericDomainMap(BitSet theBitSet, Column theTarget)
	{
		if (!isValidCall("getUniqueNumericDomainMap", theBitSet))
			return new ValueCountSum(new int[0], new double[0]);

		// NOTE (itsSortedFloats.length == itsCardinality)
		int[]    aCnt = new int[itsSortedFloats.length];
		double[] aSum = new double[itsSortedFloats.length];

		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
		{
			int idx = itsSortIndex[i];
			++aCnt[idx];
			aSum[idx] += theTarget.itsFloatz[i];
		}

		return new ValueCountSum(aCnt, aSum);
	}

	// see comment SubgroupDiscovery.evaluateNumericRegularSingleNumericSumSSD()
	ValueCountSumSquaresSum getUniqueNumericDomainMapSq(BitSet theBitSet, Column theTarget)
	{
		if (!isValidCall("getUniqueNumericDomainMap", theBitSet))
			return new ValueCountSumSquaresSum(new int[0], new double[0], new double[0]);

		// NOTE (itsSortedFloats.length == itsCardinality)
		int[]    aCnt        = new int[itsSortedFloats.length];
		double[] aSum        = new double[itsSortedFloats.length];
		double[] aSquaresSum = new double[itsSortedFloats.length];

		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i + 1))
		{
			int idx = itsSortIndex[i];
			++aCnt[idx];
			double d          = theTarget.itsFloatz[i];
			aSum[idx]        += d;
			aSquaresSum[idx] += (d * d);
		}

		return new ValueCountSumSquaresSum(aCnt, aSum, aSquaresSum);
	}

	// NOTE
	// this setup differs from the SortedMap implementation
	// FIXME make the two equal, as it is easier to understand
	//       this setup is half-interval based
	//       but requires complicated logic for external callers
	//       also, when implementing nominal-bins and cross-product bins, the
	//       implementation from the SortedMap alternative is more convenient
	//
	// the returned split points should be considered differently for <= and >=
	// this setup allows identical loop behaviour for getUniqueSplitPointsMap()/
	// getUniqueNumericDomainMap() in SubgroupDiscovery.numericHalfIntervals()
	// for <= the split points in the array are inclusive end points
	//        and the count for this split point represents the bin count for
	//        the interval [input-domain-minimum-value, this-split-point]
	// for >= the split points in the array are inclusive start points
	//        and the count for this split point represents the bin count for
	//        the interval [this-split-point, input-domain-maximum-value]
	//
	// FIXME introduce new implementation, based on
	// Column.getUniqueNumericDomainMap() and
	// SubgroupDiscovery.evaluateNumericRegularSingleBinaryCoarse()
	@Deprecated
	DomainMapNumeric getUniqueSplitPointsMap(BitSet theBitSet, int theBitSetCardinality, int theNrSplits, Operator theOperator) throws IllegalArgumentException
	{
		assert (theOperator == Operator.LESS_THAN_OR_EQUAL || theOperator == Operator.GREATER_THAN_OR_EQUAL);

		if (!isValidCall("getSplitPointsMap", theBitSet))
			return new DomainMapNumeric(0, 0, new float[0], new int[0]);

		if (theNrSplits < 0)
			throw new IllegalArgumentException(theNrSplits + " (theNrSplits) < 0");
		// valid, but useless
		if (theNrSplits == 0)
			return new DomainMapNumeric(0, 0, new float[0], new int[0]);

		// usually theNrSplits is lowest, but this is not guaranteed
		int anUpper = Math.min(theBitSetCardinality, theNrSplits);
		if (itsCardinality != 0)
			anUpper = Math.min(anUpper, itsCardinality);

		float[] dDomain = new float[anUpper];
		int[] dCounts = new int[anUpper];

//		int size = theBitSetCardinality;

		// SEE NOTE ON BUG1 ABOVE
		if (theBitSetCardinality == 0)
			return new DomainMapNumeric(0, 0, new float[theNrSplits], new int[theNrSplits]);

		float[] aDomain = new float[theBitSetCardinality];
		for (int i = theBitSet.nextSetBit(0), j = -1; i >= 0; i = theBitSet.nextSetBit(i + 1))
			aDomain[++j] = itsFloatz[i];

		Arrays.sort(aDomain);

		// SEE NOTE ON BUG2, BUG3, BUG4 ABOVE
		// N.B. Order matters to prevent integer division from yielding zero.
//		for (int j=0; j<theNrSplits; j++)
//			aSplitPoints[j] = aDomain[size*(j+1)/(theNrSplits+1)];

		// NOTE
		// the goal is an output with the same values as getSplitPoints() even
		// even if it is incorrect
		// NOTE
		// code is not based on getUniqueNumericDomainMap(), and then used to
		// select only a limited number of values from those returned by adding
		// intermediate counts
		// here, only theNrSplits values are checked, corrected for possible
		// duplicates (the inner for-loop with int m)
		// NOTE
		// the final equal height histogram code will use all values and counts
		// like getUniqueNumericDomainMap(), such that bins can be filled in a
		// more balanced way (also avoiding BUG3)
		long nrBins = theNrSplits + 1;
		long size_L = theBitSetCardinality;
		int idx = -1;
		int sum = 0; // FIXME REMOVE j FROM LOOP
		if (theOperator == Operator.LESS_THAN_OR_EQUAL)
		{
			for (int i = 1, j = 0; i <= theNrSplits; ++i)
			{
				int k = (int)((size_L*i)/nrBins);
				if (k < j)
					continue;

				float aSplitPoint = aDomain[k];

				// produce correct count for value
				for (int m = k+1; m < theBitSetCardinality; ++m)
					if (Float.compare(aSplitPoint, aDomain[m]) == 0)
						++k;

				++idx;
				// 0-based index, so size is index +1
				dDomain[idx] = aSplitPoint;
				dCounts[idx] = (k-j+1);
				j = (k+1);
				sum = (k+1);
			}
		}
		else // theOperator == Operator.GREATER_THAN_OR_EQUAL
		{
			// NOTE
			// at i=1 the first Float.compare() should not return 0, so aLast is
			// set to a value that is unequal to aDomain[0]
			// checking aDomain[0] is safe as aDomain.length > 0, as size > 0
			Float aLast = (Float.isNaN(aDomain[0]) ? 0.0f : Float.NaN);
			// FIXME MM run this loop backwards i=NrSplits...i>0
			for (int i = 1, j = theBitSetCardinality; i <= theNrSplits; ++i)
			{
				int k = (int)((size_L*i)/nrBins);

				float aSplitPoint = aDomain[k];

				if (Float.compare(aSplitPoint, aLast) == 0) // NOTE -0.0 is smaller than 0.0
					continue;
				aLast = aSplitPoint;

				// produce correct count for value
				for (int m = k-1; m >= 0; --m)
					if (Float.compare(aSplitPoint, aDomain[m]) == 0) // NOTE -0.0 is smaller than 0.0
						--k;

				j = (theBitSetCardinality-k);
				++idx;
				dDomain[idx] = aSplitPoint;
				dCounts[idx] = j;
				if (idx == 0)
					sum = j;
				else
					dCounts[idx-1] -= j;
//				// if this selects all data, write special value -count
//				if (idx == 0)
//					dCounts[idx] = (j == theBitSetCardinality ? -j : j);
//				else
//				{
//					dCounts[idx] = j;
//					if ((idx == 1) && (dCounts[0] < 0))
//						dCounts[0] += j;
//					else
//						dCounts[idx-1] -= j;
//				}
			}
		}
		// note strictly needed, use DomainMapNumeric.itsSize for end-of-input
		if (idx < dCounts.length-1)
			dCounts[idx+1] = -1;

		if (false)
		{
			// compare to SortedMap version
			SortedMap<Float, Integer> s = getUniqueSplitPointsMapX(theBitSet, theBitSetCardinality, theNrSplits, theOperator);
			int ss = s.size();
			for (int i = 0; i < idx+1; ++i)
			{
				--ss;

				if (dCounts[i] != s.get(dDomain[i]))
					throw new AssertionError(String.format("SortedMap != DomainMapNumeric:%n%s%n%s%n%s%n", s, Arrays.toString(dDomain), Arrays.toString(dCounts)));
			}
			if (ss != 0)
				throw new AssertionError(String.format("SortedMap != DomainMapNumeric:%n%s%n%s%n%s%n", s, Arrays.toString(dDomain), Arrays.toString(dCounts)));
		}

		return new DomainMapNumeric(idx+1, sum, dDomain, dCounts);
	}

	/**
	 * Returns the average of all values for Columns of {@link AttributeType} {@link AttributeType#NUMERIC} and {@link AttributeType#ORDINAL}.
	 * <p>
	 * Note, this method has the risk of overflow, in which case {@link Float#NaN} is returned.
	 *
	 * @return the average, or {@link Float#NaN} if this Column is not of type {@link AttributeType#NUMERIC} or {@link AttributeType#ORDINAL}.
	 *
	 * @see #getStatistics(BitSet, boolean)
	 */
	public float getAverage(BitSet theSelection)
	{
		if (itsType != AttributeType.NUMERIC)
			throw new IllegalArgumentException("Invalid call for AttributeType: " + itsType);

		float aSum = 0.0f;
		if (theSelection == null)
		{
			int aCount = 0;
			for (int i=0; i<itsFloatz.length; i++)
				if (!getMissing(i))
				{
					aSum += itsFloatz[i];
					aCount++;
				}
			return aSum / aCount;
		}
		else
		{
			int aCount = 0;
			for (int i=0; i<itsFloatz.length; i++)
				if (theSelection.get(i) && !getMissing(i))
				{
					aSum += itsFloatz[i];
					aCount++;
				}
			return aSum / aCount;
		}
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

		BitSet aMembers = (theSubgroup == null) ? null : theSubgroup.getMembers();
		//summation of rankings (not necessary to divide by aSize, when you just look at the order)
		for (int i=0; i<itsSize; ++i)
		{
			if (aMembers == null || aMembers.get(i)) //part of the subgroup?
			{
				String aValue = itsDistinctValues.get(itsNominalz[i]);
				LabelRanking aRanking = new LabelRanking(aValue);
				for (int j=0; j<aSize; j++)
					aTotalRanks[j] += aRanking.getRank(j);
			}
		}

		//make copy that can be sorted
		int[] aRanks = Arrays.copyOf(aTotalRanks, aSize);
		Arrays.sort(aRanks);

		//translate average ranks to a ranking
		for (int i=0; i<aSize; i++)
		{
			int aLookup = aTotalRanks[i];
			int aFirst = -1;
			int aLast = -1;
			//look up rank for this average
			for (int j=0; j<aSize; j++)
			{
				if (aLookup == aRanks[j])
				{
					if (aFirst >= 0)
						aLast = j;
					aFirst = j;
				}
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
		LabelRankingMatrix aResult = new LabelRankingMatrix(itsDistinctValues.get(itsNominalz[0]).replace(">","").length());
		// equivalent to below, as itsNominalz[0] should always be 0
		//LabelRankingMatrix aResult = new LabelRankingMatrix(itsDistinctValues.get(0).length());
		int aCount = 0;

		BitSet aMembers = (theSubgroup == null) ? null : theSubgroup.getMembers();
		//summation of rankings
		for (int i=0; i<itsSize; ++i)
		{
			if (aMembers == null || aMembers.get(i)) //part of the subgroup?
			{
				String aValue = itsDistinctValues.get(itsNominalz[i]);
				LabelRanking aRanking = new LabelRanking(aValue);
				LabelRankingMatrix aRankingMatrix = new LabelRankingMatrix(aRanking); //translate to LRM
				aResult.add(aRankingMatrix);
				aCount++;
			}
		}
		//divide by zero is not possible, subgroups always have members
		aResult.divide(aCount);

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
		{
			if (aMembers == null || aMembers.get(k)) //part of the subgroup?
			{
				String aValue = itsDistinctValues.get(itsNominalz[k]);
				LabelRanking aRanking = new LabelRanking(aValue);
				LabelRankingMatrix aRankingMatrix = new LabelRankingMatrix(aRanking); //translate to LRM

				int[][][] aModeMatrix = aRankingMatrix.getModeMatrix();

				for (int s=0; s<3; s++)
					for (int i=0; i<rankSize; i++)
						for (int j=0; j<rankSize; j++)
							theModeMatrix[s][i][j] += aModeMatrix[s][i][j];
			}
		}

		for (int i=0; i<rankSize; i++)
		{
			for (int j=0; j<rankSize; j++)
			{
				if (theModeMatrix[0][i][j] > theModeMatrix[1][i][j])
				{
					if (theModeMatrix[0][i][j] >  theModeMatrix[2][i][j])
						LRmode.itsMatrix[i][j] = -1;
					else
						LRmode.itsMatrix[i][j] = 1;
				}
				else
				{
					if (theModeMatrix[1][i][j] >  theModeMatrix[2][i][j])
						LRmode.itsMatrix[i][j] = 0;
					else
						LRmode.itsMatrix[i][j] = 1;
				}
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
	 * use the String {@link AttributeType#DEFAULT_BINARY_TRUE_STRING} for
	 * {@code true}, all other values are considered to represent
	 * {@code false}.
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
	public int countValues(String theValue, BitSet theSelection)
	{
		switch (itsType)
		{
			case NOMINAL :
			{
				int aResult = 0;
				Integer v = itsDistinctValuesMap.get(theValue);
				if (v == null)
				{
					// actually AssertionError(), but public
					Log.logCommandLine(theValue + " does not occur in Column " + itsName);
					return 0;
				}

				int value = v;
				int l = itsNominalz.length;
				if (theSelection == null)
				{
					for (int i=0; i<l; ++i)
						if (itsNominalz[i] == value)
							++aResult;
				}
				else
					for (int i=0; i<l; ++i)
						if (itsNominalz[i] == value && theSelection.get(i))
							++aResult;
				return aResult;
			}
			case BINARY :
			{
				if (theSelection == null)
					return AttributeType.DEFAULT_BINARY_TRUE_STRING.equals(theValue) ? itsBinaries.cardinality() : itsSize - itsBinaries.cardinality();
				else
					if (AttributeType.DEFAULT_BINARY_TRUE_STRING.equals(theValue))
					{
						BitSet aSet = (BitSet) theSelection.clone();
						aSet.and(itsBinaries);
						return aSet.cardinality();
					}
					else
					{
						BitSet aSet = (BitSet) theSelection.clone();
						aSet.andNot(itsBinaries);
						return aSet.cardinality();
					}
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

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// start of obsolete code - some remains for debugging (for now)    /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

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
	 * @see #getCardinality()
	 * @see AttributeType
	 * @see Subgroup
	 * @see java.util.BitSet
	 */
	// TODO MM return Set, instead of creating Set, and returning toArray()
	@Deprecated
	private String[] getUniqueNominalBinaryDomain(BitSet theBitSet)
	{
		if (theBitSet.length() > itsSize)
			throw new IllegalArgumentException("theBitSet.length() > " + itsSize);

		final Set<String> aUniqueValues = new TreeSet<String>();

		switch (itsType)
		{
			case NOMINAL :
			{
				// abort when all distinct values are added
				for (int i = theBitSet.nextSetBit(0); i >= 0 && i < itsSize; i = theBitSet.nextSetBit(i + 1))
					if (aUniqueValues.add(itsDistinctValues.get(itsNominalz[i])))
						if (aUniqueValues.size() == itsCardinality)
							break;
				break;

// TODO MM - profile, could use the same strategy as getUniqueNumericDomain
//return getUniqueNominalDomain(theBitSet);
			}
			case BINARY :
			{
				// abort when all distinct values are added
				for (int i = theBitSet.nextSetBit(0); i >= 0 && i < itsSize; i = theBitSet.nextSetBit(i + 1))
					if (aUniqueValues.add(itsBinaries.get(i) ? AttributeType.DEFAULT_BINARY_TRUE_STRING : AttributeType.DEFAULT_BINARY_FALSE_STRING))
						if (aUniqueValues.size() == itsCardinality)
							break;
				break;
// TODO MM - profile, could use the same strategy as getUniqueNumericDomain
//return getUniqueBinaryDomain(theBitSet, itsCardinality, itsBinaries);
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
	@Deprecated
	private float[] getUniqueNumericDomain(BitSet theBitSet)
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

// TODO MM - profile
//		// float[] to retrieve all data, call Arrays.sort(), then filter
//		// a TreeSet would sort and filter automatically
//		// but test have shown that in the majority of configurations
//		// the current alternative is faster (and uses less memory)
//		// only when itsCaridinality is very low (<100) the following is
//		// faster: retrieve and filter data using HashSet, convert to
//		// float[], Arrays.sort(float[])
//		int aNrMembers = theBitSet.cardinality();
//		float[] aResult2 = new float[aNrMembers];
//
//		for (int k = 0, j = theBitSet.nextSetBit(0); j >= 0; j = theBitSet.nextSetBit(j + 1), ++k)
//			aResult2[k] = itsFloatz[j];
//
//		// sort
//		Arrays.sort(aResult2);
//
//		// filter duplicates, assume high cardinality
//		// use upper bound on number of distinct values that can be
//		// found, but on Column.init (itsCardinality == 0)
//		int anUpper = (itsCardinality == 0) ? aNrMembers : Math.min(aNrMembers, itsCardinality);
//		int aLast = 0;
//		for (int k = 1; k < aNrMembers && aLast < anUpper-1; ++k)
//			if (aResult2[k] != aResult2[aLast])
//				aResult2[++aLast] = aResult2[k];
//
//		// truncate everything after last unique value
//		// copy could be avoided using a sentinel that signals end of
//		// input, but then returned array would not contain only unique
//		// values and external code would need to check for sentinel
//		//return Arrays.copyOf(aResult, aLast+1);
//
//		System.out.println("EQUALS: " + (Arrays.equals(aResult, Arrays.copyOf(aResult2, aLast+1))));

		return aResult;
	}

	// FIXME MM - temporary hack for better bins
	// for GEQ: the old code is correct
	// for LEQ: the old code is incorrect
	//          each split point would always select (1/bins) too much data
	//
	// new code returns expected bin boundaries
	// for GEQ, behaves as old code
	// for LEQ, same code as GEQ, just loop over selected domain backwards
	//
	// examples of what happens in old code:
	//
	// data = [1,2,3,4] ; bins = 2 -> split-points { 3 }
	//                    >= 3 -> 50% is correct
	//                    <= 3 -> 75% is incorrect (1/bins) too much
	//                         -> should be <= 2
	//
	// data = [1,2,3,4] ; bins = 4 -> split-points { 2,3,4 }
	//                    >= 2 -> 75% is correct
	//                    >= 3 -> 50% is correct
	//                    >= 4 -> 25% is correct
	//
	//                    <= 2 ->  50% is incorrect (1/b) too much -> (<= 1)
	//                    <= 3 ->  75% is incorrect (1/b) too much -> (<= 2)
	//                    <= 4 -> 100% is incorrect (1/b) too much -> (<= 3)
	//                         -> 100% is useless anyway
	//
	// leave false in svn - set to true by code that needs it
	// NOTE: in svn revision 3430 USE_NEW_BINNING = true; in git revision ad14cb3 USE_NEW_BINNING = false;
	public static boolean USE_NEW_BINNING = false;
	@Deprecated
	public float[] getUniqueSplitPoints(BitSet theBitSet, int theNrSplits, Operator theOperator) throws IllegalArgumentException
	{
		if (!USE_NEW_BINNING)
			return getSplitPoints(theBitSet, theNrSplits);

		// should never happen when (USE_NEW_BINNING == true)
		if (theOperator != Operator.LESS_THAN_OR_EQUAL && theOperator != Operator.GREATER_THAN_OR_EQUAL)
			throw new AssertionError("FIX new getSplitPoints()");

// MM - COPY OF OLD CODE STARTS HERE
		if (!isValidCall("getSplitPoints", theBitSet))
			return new float[0];

		if (theNrSplits < 0)
			throw new IllegalArgumentException(theNrSplits + " (theNrSplits) < 0");
		// valid, but useless
		if (theNrSplits == 0)
			return new float[0];

		int size = theBitSet.cardinality();

		// new - removed error in old code, see comment there
		// prevent crash in aSplitPoints populating loop
		if (size == 0)
			return new float[0];

		float[] aDomain = new float[size];
		for (int i = theBitSet.nextSetBit(0), j = -1; i >= 0; i = theBitSet.nextSetBit(i + 1))
			aDomain[++j] = itsFloatz[i];

		Arrays.sort(aDomain);

		// new - for LEQ, reverse sorted domain, then run old code on it
		if (theOperator == Operator.LESS_THAN_OR_EQUAL)
			reverse(aDomain);

		float[] aSplitPoints = new float[theNrSplits];

		// the (old) code below basically determines a step size:
		// (size / (theNrSplits+1)) it is equivalent to (size / nrBins)
		// and selects indexes using x*step_size, with x = 1..(nrBins-1)
		// selected indexes are always [ >= 0 ; < size ]
		// NOTE can cause integer overflow on large data
		// N.B. Order matters to prevent integer division from yielding zero.
//		for (int j=0; j<theNrSplits; j++)
//			aSplitPoints[j] = aDomain[size*(j+1)/(theNrSplits+1)];

		// as above - but safe
		// avoid integer overflow
		//   theNrSplits + 1 : could overflow int -> use long nrBins
		//   (i+1)           : safe as i < theNrSplits (i+1 <= MAX_INT)
		//   size * (i+1)    : could overflow int
		//   size_L * (i+1)  : safe as (MAX_INT * MAX_INT) < MAX_LONG
		//
		// range for nrBins:
		//   (theNrSplits > 0) is checked above, so nrBins >= 2
		//
		// range for i:
		//   i     = [ 0 ; (theNrSplits-1) ] = [ 0 ; (nrBins-2) ]
		//   (i+1) = [ 1 ; (nrBins-1) ]
		//
		// aDomain[ idx ] is within int range, so safe to casts:
		//   idx = [ (size / nrBins)  ; ((size * (nrBins-1)) / nrBins) ]
		//   idx = [ (size / nrBins)  ;  (size * ((nrBins-1) / nrBins) ]
		//   idx = [ >= 0 && <=size/2 ; < size (as nrBins_factor < 1)  ]
		long nrBins = theNrSplits + 1;
		long size_L = size;
		for (int i = 0; i < theNrSplits; ++i)
			aSplitPoints[i] = aDomain[(int) ((size_L * (i+1)) / nrBins)];

		// remove duplicates, re-assign to aSplitPoints
		aSplitPoints = getUniqueValues(aSplitPoints);

		// new - not strictly needed, but return data in ascending order
		if (theOperator == Operator.LESS_THAN_OR_EQUAL)
			reverse(aSplitPoints);

		return aSplitPoints;
	}

	@Deprecated
	private static final void reverse(float[] theArray)
	{
		// generates null pointer exception on null input
		int size = theArray.length;

		// does nothing on sizes 0 and 1 (does not enter loop) 
		for (int i = 0, j = size - 1; i < size / 2; ++i, --j)
		{
			float aTemp = theArray[i];
			theArray[i] = theArray[j];
			theArray[j] = aTemp;
		}
	}

	// used by getUniqueSplitPoints()
	// TODO MM - also use this in getSplitPoints(), getUniqueNumericDomain()
	@Deprecated
	static final float[] getUniqueValues(float[] theArray)
	{
		int aLast = 0;
		for (int i = 1, j = theArray.length; i < j; ++i)
			if (theArray[aLast] != theArray[i])
				theArray[++aLast] = theArray[i];

		return Arrays.copyOf(theArray, ++aLast);
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
	// instead, use new getSplitPoints(BitSet, int, Operator)
	@Deprecated
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
			aSplitPoints[j] = aDomain[(size*(j+1)) / (theNrSplits+1)];

		return aSplitPoints;
	}

	// NOTE
	// for historic comparison and evaluation this implementation should be
	// exactly equal to getSplitPoints(), and have the same bugs
	// but not all faulty behaviour can be reproduced using a map implementation
	// as invalid split point values would have a count of 0 and are ignored,
	// whereas with getSplitPoints() they would lead to evaluated Conditions
	// also the final (alternative) implementation will change and be based on
	// the new getUniqueNumericDomainMap() code (and a better bin strategy)
	// BUG1: returns an array with 0s when theBitSet is empty (note that in that
	//       case the result will not be used, as the subgroup selects no data)
	//       so for the search result keeping or removing this bug has no effect
	//       therefore it is left in code for now
	//       note: the current implementation returns an empty SortedSet in this
	//       scenario, such that the return of this method is slightly different
	// BUG2: like above, but when there are less members than split points the
	//       original getSplitPoints() returns an array with 0s at
	//       indexes >= theBitSet.cardinality()
	//       in this case the 0s will: (incorrectly) be considered to be valid
	//       split points; be used in Conditions; and effect the search result
	//       the map implementation can not cause such errors, as the counts for
	//       invalid split points are 0, causing them to be ignored
	// BUG3: for example, when data has two values, and the first covers more
	//       than half of the records, then using two bins results in a split
	//       point that selects all data when using operator >=
	//       as such the condition using this split point does not select less
	//       records, and is therefore considered to be no valid refinement
	//       thus, effectively, no condition is created for this Column and >=
	//       (the same holds for <= and less than half of the data)
	// BUG4: the final loop could yield an array that contains the same split
	//       points multiple times, the SortedSet does not reproduce this
	//       the search algorithm already checked for this situation, so it has no
	//       effect on the search result, just on the returned split point array
	// NOTE
	// the returned split points should be considered differently for <= and >=
	// this setup allows identical loop behaviour for getUniqueSplitPointsMap()/
	// getUniqueNumericDomainMap() in SubgroupDiscovery.numericHalfIntervals()
	// for <= the split points in the array are inclusive end points
	//        and the count for this split point represents the bin count for
	//        the interval (exclusive-previous-split-point, this-split-point]
	// for >= the split points in the array are inclusive start points
	//        and the count for this split point represents the bin count for
	//        the interval [this-split-point, exclusive-next-split-point)
	@Deprecated
	private SortedMap<Float, Integer> getUniqueSplitPointsMapX(BitSet theBitSet, int theBitSetCardinality, int theNrSplits, Operator theOperator) throws IllegalArgumentException
	{
		assert (theOperator == Operator.LESS_THAN_OR_EQUAL || theOperator == Operator.GREATER_THAN_OR_EQUAL);

		if (!isValidCall("getSplitPointsMap", theBitSet))
			return Collections.emptySortedMap();

		if (theNrSplits < 0)
			throw new IllegalArgumentException(theNrSplits + " (theNrSplits) < 0");
		// valid, but useless
		if (theNrSplits == 0)
			return Collections.emptySortedMap();

		SortedMap<Float, Integer> aSplitPointsMap = new TreeMap<Float, Integer>();
		final int size = theBitSetCardinality;

		// SEE NOTE ON BUG1 ABOVE
		if (size == 0)
			return Collections.emptySortedMap();

		float[] aDomain = new float[size];
		for (int i = theBitSet.nextSetBit(0), j = -1; i >= 0; i = theBitSet.nextSetBit(i + 1))
			aDomain[++j] = itsFloatz[i];

		Arrays.sort(aDomain);

		// SEE NOTE ON BUG2, BUG3, BUG4 ABOVE
		// N.B. Order matters to prevent integer division from yielding zero.
//		for (int j=0; j<theNrSplits; j++)
//			aSplitPoints[j] = aDomain[size*(j+1)/(theNrSplits+1)];

		// NOTE
		// the goal is an output with the same values as getSplitPoints() even
		// even if it is incorrect
		// NOTE
		// code is not based on getUniqueNumericDomainMap(), and then used to
		// select only a limited number of values from those returned by adding
		// intermediate counts
		// here, only theNrSplits values are checked, corrected for possible
		// duplicates (the inner for-loop with int m)
		// NOTE
		// the final equal height histogram code will use all values and counts
		// like getUniqueNumericDomainMap(), such that bins can be filled in a
		// more balanced way (also avoiding BUG3)
		// NOTE
		// Column.add(float) allows NaN, -0.0, and 0.0, values to be added
		// the == and Float.compare() checks for floats differ for these values
		// (NaN == NaN) returns false; this is incorrect, as they are the same
		// (-0.0 == 0.0) returns true; assuming -0.0 numerically equal to 0.0 *
		// Float.compare(NaN, NaN) returns 0; this is correct, they are the same
		// Float.compare(-0.0, 0.0) returns -1; assuming -0.0 smaller than 0.0 *
		//
		// * so to correctly deal with NaN, Float.compare() is required
		//   but for data containing both -0.0 and 0.0 this results in two
		//   different boundaries, with different counts
		//   for the algorithm the choice of whether to allow NaN, -0.0, and 0.0
		//   is of major importance, and will have effects everywhere
		//   many (model) classes use either == or Float.compare()
		//   so, a choice should be made what values to allow, as currently the
		//   code has inconsistent behaviour, and all code needs a review
		//   the case for assuming that -0.0 and 0.0 are the same is that they
		//   are numerically equal
		//   the case for considering -0.0 smaller that 0.0 is to allow complete
		//   ordering of floats, and that the user might intent these values to
		//   be considered different, expecting them to be treated as such
		//   to best resolve this issue, the final solution lies with the values
		//   Column.add(float) allows
		//   in case it changes -0.0 to 0.0, users should be made aware of this
		//   and could be suggested to modify their data and use some very small
		//   value -0.00...00001 as substitute for  -0.0
		//
		// NOTE Arrays.sort() uses DualPivotQuicksort.sort(float[]), which puts:
		//      all NaN values at the end of the array
		//      -0.0 before 0.0, like Float.compare() behaviour
		//
		// FIXME review all code
		//       the evaluation code needs to be checked, as it might not deal
		//       correctly with the NaN, -0.0, and 0.0 situations
		//
		// the code below assumes the data does not contain both -0.0 and 0.0
		long nrBins = theNrSplits + 1;
		long size_L = size;
		if (theOperator == Operator.LESS_THAN_OR_EQUAL)
		{
			for (int i = 1, j = 0; i <= theNrSplits; ++i)
			{
				int k = (int)((size_L*i)/nrBins);
				if (k < j)
					continue;

				float aSplitPoint = aDomain[k];

				// produce correct count for value
				for (int m = k+1; m < size; ++m)
					if (Float.compare(aSplitPoint, aDomain[m]) == 0) // NOTE -0.0 is smaller than 0.0
						++k;

				// 0-based index, so size is index +1
				aSplitPointsMap.put(aSplitPoint, (k-j+1));
				j = (k+1);
			}
		}
		else // theOperator == Operator.GREATER_THAN_OR_EQUAL
		{
			// NOTE
			// at i=1 the first Float.compare() should not return 0, so aLast is
			// set to a value that is unequal to aDomain[0]
			// checking aDomain[0] is safe as aDomain.length > 0, as size > 0
			Float aLast = (Float.isNaN(aDomain[0]) ? 0.0f : Float.NaN);
			for (int i = 1, j = size; i <= theNrSplits; ++i)
			{
				int k = (int)((size_L*i)/nrBins);

				float aSplitPoint = aDomain[k];

				// check is faster than aSplitPointsMap.containsKey(aSplitPoint)
				if (Float.compare(aSplitPoint, aLast) == 0) // NOTE -0.0 is smaller than 0.0
					continue;

				// produce correct count for value
				for (int m = k-1; m >= 0; --m)
					if (Float.compare(aSplitPoint, aDomain[m]) == 0) // NOTE -0.0 is smaller than 0.0
						--k;

				j = (size-k);
				aSplitPointsMap.put(aSplitPoint, j);
				if (i > 1)
					aSplitPointsMap.put(aLast, aSplitPointsMap.get(aLast)-j);
				// only now set aLast to aSplitPoint, aLast was needed above
				aLast = aSplitPoint;
			}
		}

		if (false)
		{
			int sum = 0;
			for (int i : aSplitPointsMap.values())
				sum += i;
			if (sum > theBitSetCardinality)
				throw new AssertionError(String.format("%s.getSplitPointsMap() sum=%d > theBitSetCardinality=%d%n%s%n", this.getClass().getName(), sum, theBitSetCardinality, aSplitPointsMap));

			float[] fa = getSplitPoints(theBitSet, theNrSplits);
			for (float f : fa)
			{
				if (aSplitPointsMap.get(f) == null)
				{
					System.out.println("WARNING: getSplitPoinsMap() does not contain value = " + f + " (could be bug in original code)");
					System.out.format("SortedMap != getSplitPoints():%n%s%n%s%n", aSplitPointsMap, Arrays.toString(fa));
				}
			}
		}

		return aSplitPointsMap;
	}

	@Deprecated
	private SortedSet<Interval> getUniqueSplitPointsBoundedOriginal(BitSet theBitSet, int theNrSplits) throws IllegalArgumentException
	{
		if (!isValidCall("getSplitPointsBounded", theBitSet))
			return Collections.emptySortedSet();

		if (theNrSplits < 0)
			throw new IllegalArgumentException(theNrSplits + " (theNrSplits) < 0");

		// valid, but useless
		if (theNrSplits == 0)
			return Collections.emptySortedSet();

		// TODO use same procedure as pre-discretisation in MiningWindow
		boolean anOrig = Column.USE_NEW_BINNING ;
		Column.USE_NEW_BINNING = true;
		float[] aBounds = getUniqueSplitPoints(theBitSet, theNrSplits, Operator.LESS_THAN_OR_EQUAL);
		Column.USE_NEW_BINNING = anOrig;

		int aNrBounds = aBounds.length;
		if (aNrBounds == 0)
			return Collections.emptySortedSet();

		if (aBounds[aNrBounds - 1] != Float.POSITIVE_INFINITY)
		{
			aBounds = Arrays.copyOf(aBounds, aNrBounds + 1);
			aBounds[aNrBounds] = Float.POSITIVE_INFINITY;
			++aNrBounds;
		}

		SortedSet<Interval> set = new TreeSet<Interval>();
		for (int i = 0; i < aNrBounds; ++i)
			set.add(new Interval(i == 0 ? Float.NEGATIVE_INFINITY : aBounds[i-1], aBounds[i]));

		return set;
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
	 * @return a SortedSet<Interval>, holding the Intervals.
	 *
	 * @throws IllegalArgumentException when theNrSplits < 0.
	 *
	 * @see AttributeType
	 * @see Subgroup
	 * @see NumericStrategy
	 * @see java.util.BitSet
	 */
	// FIXME 
	@Deprecated
	SortedMap<Interval, Integer> getUniqueSplitPointsBounded(BitSet theBitSet, int theBitSetCardinality, int theNrSplits) throws IllegalArgumentException
	{
		// for testing only, final version will supply theBitSetCardinality
		int aCardinality = theBitSet.cardinality();
		SortedSet<Interval> set = getUniqueSplitPointsBoundedOriginal(theBitSet, theNrSplits);
		SortedMap<Interval, Integer> map = getUniqueSplitPointsBoundedMap(theBitSet, aCardinality, theNrSplits);

		int aSum = 0;
		Iterator<Interval> s = set.iterator();
		Iterator<Map.Entry<Interval, Integer>> m = map.entrySet().iterator();
		Log.logCommandLine("");
		while (s.hasNext() || m.hasNext())
		{
			boolean sn = s.hasNext();
			boolean mn = m.hasNext();

			Interval si = (sn ? s.next() : null);
			Map.Entry<Interval, Integer> me = (mn ? m.next() : null);

			aSum += (mn ? me.getValue() : 0);

			StringBuilder sb = new StringBuilder(128);
			// when the same, both are true -> non-null
			sb.append(sn ? si : "empty").append("\t");
			sb.append(mn ? me : "empty");

			sb.append(((sn != mn) || (si.compareTo(me.getKey()) != 0)) ? "\t<- NOTE" : "");

			Log.logCommandLine(sb.toString());
		}
		Log.logCommandLine("BitSet.cardinality = " + aCardinality + "\n");

		if (aCardinality != aSum)
			throw new AssertionError("getUniqueSplitPointsBounded(): " + aCardinality + " != " + aSum);

		return map;
	}

	@Deprecated
	private SortedMap<Interval, Integer> getUniqueSplitPointsBoundedMap(BitSet theBitSet, int theBitSetCardinality, int theNrSplits) throws IllegalArgumentException
	{
		if (!isValidCall("getSplitPointsBounded", theBitSet))
			return Collections.emptySortedMap();

		if (theNrSplits < 0)
			throw new IllegalArgumentException(theNrSplits + " (theNrSplits) < 0");

		// valid, but useless
		if (theNrSplits == 0)
			return Collections.emptySortedMap();

		// NOTE
		// the map version of this method does not have a USE_NEW_BINNING
		// variant, but getUniqueSplitPointsMap() does take into account the
		// Operator to manipulate the value counts which has similar effects
		//
		// this setting is not really used by anyone, and may be removed after
		// the paper, even when not, it is not a successful strategy anyway, it
		// should not be used, so not having it optimised is not a big problem
		//
		// this method uses the result of getUniqueSplitPointsMap(), and loops
		// over it again, so it is not the most efficient
		// but it is not too much of a problem, as getUniqueSplitPointsMap()
		// is fast, O(nrBins) for high-cardinality data, and the loop over its
		// result to establish the bounded intervals requires only O(nrBins)
		//
		// the rest of the comment below is from the original non-map version
		//
		// ##### ORIGINAL CODE STARTS HERE #####################################
		// TODO use same procedure as pre-discretisation in MiningWindow
		//boolean anOrig = Column.USE_NEW_BINNING ;
		//Column.USE_NEW_BINNING = true;
//		float[] aBounds = getUniqueSplitPoints(theBitSet, theNrSplits, Operator.LESS_THAN_OR_EQUAL);
//		//Column.USE_NEW_BINNING = anOrig;
//
//		int aNrBounds = aBounds.length;
//		if (aNrBounds == 0)
//			return Collections.emptySortedMap();
//
//		if (aBounds[aNrBounds - 1] != Float.POSITIVE_INFINITY)
//		{
//			aBounds = Arrays.copyOf(aBounds, aNrBounds + 1);
//			aBounds[aNrBounds] = Float.POSITIVE_INFINITY;
//			++aNrBounds;
//		}
//
//		SortedSet<Interval> set = new TreeSet<Interval>();
//		for (int i = 0; i < aNrBounds; ++i)
//			set.add(new Interval(i == 0 ? Float.NEGATIVE_INFINITY : aBounds[i-1], aBounds[i]));
		// ##### ORIGINAL CODE ENDS HERE #######################################

		SortedMap<Float, Integer> aMap = getUniqueSplitPointsMapX(theBitSet, theBitSetCardinality, theNrSplits, Operator.LESS_THAN_OR_EQUAL);
		if (aMap.isEmpty())
			return Collections.emptySortedMap();

		SortedMap<Interval, Integer> aResult = new TreeMap<Interval, Integer>();
		Iterator<Map.Entry<Float, Integer>> it = aMap.entrySet().iterator();
		int aSum = 0;

		// special treatment for first value, as the -infinity bound is used
		Map.Entry<Float, Integer> e = it.next(); // safe as !aMap.isEmpty()
		float f = e.getKey();
		Integer i = e.getValue();
		aSum += i; // auto cast, the put() does not
		aResult.put(new Interval(Float.NEGATIVE_INFINITY, f), i);

		while (it.hasNext())
		{
			e = it.next();
			float n = e.getKey();
			i = e.getValue();
			aSum += i;
			if (aSum == theBitSetCardinality)
				aResult.put(new Interval(f, Float.POSITIVE_INFINITY), i);
			else
				aResult.put(new Interval(f, n), i);
			f = n;
		}

		// POSITIVE_INFINITY should never be present, as this type of value
		// should not be in the data, but this is copied from the original code
		// moreover Column.add() does not guard against +/- infinity, -0 and NaN
		//
		// if there is already a key for POSITIVE_INFINITY, do not overwrite it
		// it should be the last value anyway, and have a correct count, if it
		// exists (though Arrays.sort() would put NaNs after it)
		//
		// if it is not in getUniqueSplitPointsMap(), two situations could occur
		// 1. the sum of the value.counts for the map entries is equal to
		// theBitSetCardinality (for a half-interval: <= lastKey() would select
		// the whole data, and be useless), or 2. the sum is lower
		//
		// the former case is handled by the loop, it replaces the last value
		// in the latter case, add the Interval that selects the remaining data
		if ((aSum != theBitSetCardinality) && (Float.compare(Float.POSITIVE_INFINITY, aMap.lastKey()) != 0))
			aResult.put(new Interval(f, Float.POSITIVE_INFINITY), (theBitSetCardinality - aSum));

		return aResult;
	}
}
