package nl.liacs.subdisc;

/*
 * The logic and safety of this class would benefit from a separation between
 * the different Condition types.
 * That is, a separate ConditionNominal, ConditionNominalSet, ConditionNumeric,
 * and so on.
 * In that case each ConditionX needs to hold only one value member field 
 * specific for that Condition (Column) type.
 * This need not be exposed to the external API, that just sees a Condition.
 * Conditions would be smaller, and safer to compare, copy and so forth.
 */
public class Condition implements Comparable<Condition>
{
	private final Column   itsColumn;
	private final Operator itsOperator;
	// for non-String/non-float based Column.evaluate() (use index instead)
	private final int      itsSortIndex; // ColumnType = NUMERIC (for now) Integer.MIN_VALUE

	// value member fields, all final
	// forces constructors to set a value instead of relying on default
	// Conditions are completely immutable, safe (and fast in concurrency)
	private final String   itsNominalValue;    // ColumnType = NOMINAL null
	private final ValueSet itsNominalValueSet; // ColumnType = NOMINAL null
	private final float    itsNumericValue;    // ColumnType = NUMERIC NaN
	private final Interval itsInterval;        // ColumnType = NUMERIC null
	private final boolean  itsBinaryValue;     // ColumnType = BINARY  false

	// defaults for uninitialised value member fields
	private static final String   UNINITIALISED_NOMINAL     = null;
	private static final ValueSet UNINITIALISED_NOMINAL_SET = null;
	private static final float    UNINITIALISED_NUMERIC     = Float.NaN;
	private static final Interval UNINITIALISED_INTERVAL    = null;
	private static final boolean  UNINITIALISED_BINARY      = false;
	// NOTE relies on the fact that MIN_VALUE is smaller than -MAX_VALUE
	//      MAX_VALUE is the largest valid (sort) index that can occur
	static final int              UNINITIALISED_SORT_INDEX  = Integer.MIN_VALUE;

	/*
	 * strict constructors, replaces all above, allows for final
	 * value field, and simplified syntax and error checking
	 * value type must be appropriate for both Column.type and Operator.type
	 * 
	 * might get 2 final fields: ColumnBase, and Value
	 * in the end 1 ConditionX per ConditionBase combination would be ideal
	 * as it allows only valid Column-Operator-Value combinations and needs
	 * no checking (implemented using abstract class Condition)
	 * 
	 * TODO MM
	 * assert (Column.domain.contains(theValue)) but this is extremely heavy
	 * also, it would not work for Validation.getRandomConditionList() for
	 * NUMERIC Columns, as it uses a 'random' float value
	 * (likewise for 1-valued BINARY Columns)
	 */
	/** Condition for NOMINAL Column, single value. */
	public Condition(ConditionBase theConditionBase, String theValue)
	{
		// check Column
		Column aColumn = theConditionBase.getColumn();
		if (aColumn.getType() != AttributeType.NOMINAL)
			throw exception("ConditionBase.Column.AttributeType", aColumn.getType().toString());

		// assume that ConditionBase checked validity of Column-Operator
		// String can only be used with EQUALS
		Operator anOperator = theConditionBase.getOperator();
		if (anOperator != Operator.EQUALS)
			throw exception("ConditionBase.Operator", anOperator.GUI_TEXT);

		// check value
		if (theValue == null)
			throw exception(String.class.getSimpleName(), "null");

		itsColumn          = theConditionBase.getColumn();
		itsOperator        = theConditionBase.getOperator();
		itsNominalValue    = theValue;

		// set non used value member fields to uninitialised default
		itsNominalValueSet = UNINITIALISED_NOMINAL_SET;
		itsNumericValue    = UNINITIALISED_NUMERIC;
		itsInterval        = UNINITIALISED_INTERVAL;
		itsBinaryValue     = UNINITIALISED_BINARY;
		itsSortIndex       = UNINITIALISED_SORT_INDEX;
	}

	/** Condition for NOMINAL Column, ValueSet. */
	public Condition(ConditionBase theConditionBase, ValueSet theValueSet)
	{
		// check Column
		Column aColumn = theConditionBase.getColumn();
		if (aColumn.getType() != AttributeType.NOMINAL)
			throw exception("ConditionBase.Column.AttributeType", aColumn.getType().toString());

		// assume that ConditionBase checked validity of Column-Operator
		// ValueSet can only be used with ELEMENt_OF
		Operator anOperator = theConditionBase.getOperator();
		if (anOperator != Operator.ELEMENT_OF)
			throw exception("ConditionBase.Operator", anOperator.GUI_TEXT);

		// check value
		if (theValueSet == null)
			throw exception(ValueSet.class.getSimpleName(), "null");

		itsColumn          = theConditionBase.getColumn();
		itsOperator        = theConditionBase.getOperator();
		itsNominalValueSet = theValueSet;

		// set non used value member fields to uninitialised default
		itsNominalValue    = UNINITIALISED_NOMINAL;
		itsNumericValue    = UNINITIALISED_NUMERIC;
		itsInterval        = UNINITIALISED_INTERVAL;
		itsBinaryValue     = UNINITIALISED_BINARY;
		itsSortIndex       = UNINITIALISED_SORT_INDEX;
	}

	/** Condition for BINARY Column. */
	public Condition(ConditionBase theConditionBase, boolean theValue)
	{
		// check Column
		Column aColumn = theConditionBase.getColumn();
		if (aColumn.getType() != AttributeType.BINARY)
			throw exception("ConditionBase.Column.AttributeType", aColumn.getType().toString());

		// assume that ConditionBase checked validity of Column-Operator
		// there is only one possibility
		assert (theConditionBase.getOperator() == Operator.EQUALS);

		itsColumn          = aColumn;
		itsOperator        = theConditionBase.getOperator();
		itsBinaryValue     = theValue;

		// set non used value member fields to uninitialised default
		itsNominalValue    = UNINITIALISED_NOMINAL;
		itsNominalValueSet = UNINITIALISED_NOMINAL_SET;
		itsNumericValue    = UNINITIALISED_NUMERIC;
		itsInterval        = UNINITIALISED_INTERVAL;
		itsSortIndex       = UNINITIALISED_SORT_INDEX;
	}

	/** Condition for NUMERIC Column, Interval. */
	public Condition(ConditionBase theConditionBase, Interval theInterval)
	{
		// check Column
		Column aColumn = theConditionBase.getColumn();
		if (aColumn.getType() != AttributeType.NUMERIC)
			throw exception("ConditionBase.Column.AttributeType", aColumn.getType().toString());

		// assume that ConditionBase checked validity of Column-Operator
		// Interval can only be used with BETWEEN
		Operator anOperator = theConditionBase.getOperator();
		if (anOperator != Operator.BETWEEN)
			throw exception("ConditionBase.Operator", anOperator.GUI_TEXT);

		// check value
		if (theInterval == null)
			throw exception(Interval.class.getSimpleName(), "null");

		itsColumn          = aColumn;
		itsOperator        = anOperator;
		itsInterval        = theInterval;

		// set non used value member fields to uninitialised default
		itsNominalValue    = UNINITIALISED_NOMINAL;
		itsNominalValueSet = UNINITIALISED_NOMINAL_SET;
		itsNumericValue    = UNINITIALISED_NUMERIC;
		itsBinaryValue     = UNINITIALISED_BINARY;
		itsSortIndex       = UNINITIALISED_SORT_INDEX;
	}

	// Column.evaluate(BitSet, Condition): only 1 NUMERIC Constructor must exist
	/** Condition for NUMERIC Column, single value, NaN is not allowed as a condition (it is allowed as a value to check the condition on). */
	public Condition(ConditionBase theConditionBase, float theValue, int theIndex)
	{
		// check Column
		Column aColumn = theConditionBase.getColumn();
		if (aColumn.getType() != AttributeType.NUMERIC)
			throw exception("ConditionBase.Column.AttributeType", aColumn.getType().toString());

		// assume that ConditionBase checked validity of Column-Operator
		// check NUMERIC operators only
		Operator anOperator = theConditionBase.getOperator();
		switch (anOperator)
		{
			case EQUALS :
				break;
			case LESS_THAN_OR_EQUAL :
				break;
			case GREATER_THAN_OR_EQUAL :
				break;
			case BETWEEN :
				throw exception("ConditionBase.Operator", anOperator.GUI_TEXT);
			default :
				throw new AssertionError(anOperator);
		}

		// check value
		if (Float.isNaN(theValue))
			throw exception("float value", "NaN");

		itsColumn          = aColumn;
		itsOperator        = anOperator;
		itsNumericValue    = theValue;
		itsSortIndex       = theIndex;

		// set non used value member fields to uninitialised default
		itsNominalValue    = UNINITIALISED_NOMINAL;
		itsNominalValueSet = UNINITIALISED_NOMINAL_SET;
		itsInterval        = UNINITIALISED_INTERVAL;
		itsBinaryValue     = UNINITIALISED_BINARY;
	}

	private static final IllegalArgumentException exception(String pre, String post)
	{
		return new IllegalArgumentException(pre + " can not be " + post);
	}

	public Column getColumn()            { return itsColumn; }
	public Operator getOperator()        { return itsOperator; }
	public int getSortIndex()            { return itsSortIndex; }
	// no type validity checks are performed
	public String getNominalValue()      { return itsNominalValue; }
	public ValueSet getNominalValueSet() { return itsNominalValueSet; }
	public float getNumericValue()       { return itsNumericValue; }
	public Interval getNumericInterval() { return itsInterval; }
	public boolean getBinaryValue()      { return itsBinaryValue; }

	/* Assumes values are set in constructor, and per Column/Operator type*/
	public String getValue()
	{
		switch (itsColumn.getType())
		{
			case NOMINAL :
			{
				switch (itsOperator)
				{
					case EQUALS :
						return "'" + itsNominalValue + "'";
					case ELEMENT_OF :
						return itsNominalValueSet.toString();
					default :
						throw new AssertionError(AttributeType.NOMINAL);
				}
			}
			case NUMERIC :
			{
				switch (itsOperator)
				{
					case EQUALS :
						return Float.toString(itsNumericValue);
					case LESS_THAN_OR_EQUAL :
						return Float.toString(itsNumericValue);
					case GREATER_THAN_OR_EQUAL :
						return Float.toString(itsNumericValue);
					case BETWEEN :
						return itsInterval.toString();
					default :
						throw new AssertionError(AttributeType.NUMERIC);
				}
			}
			case ORDINAL :
				throw new AssertionError(AttributeType.ORDINAL);
			case BINARY :
			{
				assert (itsOperator == Operator.EQUALS);
				return itsBinaryValue ? "'1'" : "'0'";
			}
			default :
				throw new AssertionError(itsColumn.getType());
		}
	}

	@Override
	public String toString()
	{
		return new StringBuilder(32).append(itsColumn.getName()).append(" ").append(itsOperator).append(" ").append(getValue()).toString();
	}

	// throws NullPointerException if theCondition is null
	@Override
	public int compareTo(Condition theCondition)
	{
		if (this == theCondition)
			return 0;

		// Conditions about Column with smaller index come first
		int cmp = this.itsColumn.getIndex() - theCondition.itsColumn.getIndex();
		if (cmp != 0)
			return cmp;

		// same column, check operator
		cmp = this.itsOperator.ordinal() - theCondition.itsOperator.ordinal();
		if (cmp != 0)
			return cmp;

		// same column, same operator, check on value
		switch (itsColumn.getType())
		{
			case NOMINAL :
			{
				// one of these must hold, but not both
				assert ((itsNominalValue != UNINITIALISED_NOMINAL) ^ (itsNominalValueSet != UNINITIALISED_NOMINAL_SET));

				if (itsNominalValue != UNINITIALISED_NOMINAL) //single value
					return itsNominalValue.compareTo(theCondition.itsNominalValue);

				// else assumes ValueSet
				return this.itsNominalValueSet.compareTo(theCondition.itsNominalValueSet);
			}
			case NUMERIC :
			{
				// one of these must hold, but not both
				assert ((Float.compare(itsNumericValue, UNINITIALISED_NUMERIC) != 0) ^ (itsInterval != UNINITIALISED_INTERVAL));

				if (itsInterval != UNINITIALISED_INTERVAL)
					return itsInterval.compareTo(theCondition.itsInterval);

				// NOTE considers 0.0 to be greater than -0.0
				return Float.compare(itsNumericValue, theCondition.itsNumericValue);
			}
			case ORDINAL :
			{
				throw new AssertionError(AttributeType.ORDINAL);
			}
			case BINARY :
			{
				if (!itsBinaryValue)
					return theCondition.itsBinaryValue ? -1 : 0;
				else
					return theCondition.itsBinaryValue ? 0 : 1;
			}
			// should never happen
			default :
			{
				throw new AssertionError(String.format("ERROR: compareTo()\n%s\n%s", this.toString(), theCondition.toString()));
			}
		}
	}

	//the result is true only if everything is similar, and the attribute is numeric, and the value of this is tighter
	//example: a < 10 specialises a < 20
	public boolean strictlySpecialises(Condition theCondition)
	{
		if (getColumn() != theCondition.getColumn())
			return false;
		if (getOperator() != theCondition.getOperator())
			return false;
		if (getValue() == theCondition.getValue()) //essentially the same conditions, so not a strict specialisation
			return false;
		if (getColumn().getType() != AttributeType.NUMERIC || theCondition.getColumn().getType() != AttributeType.NUMERIC)
			return false;

		// strictly specialises if attribute and operator are the same, and value of this is tighter
		float aValue = Float.parseFloat(getValue());
		float anOtherValue = Float.parseFloat(theCondition.getValue());
		if (getOperator() == Operator.LESS_THAN_OR_EQUAL && aValue >= anOtherValue)
			return false;
		if (getOperator() == Operator.GREATER_THAN_OR_EQUAL && aValue <= anOtherValue)
			return false;

		return true;
	}

	//the result is true only if everything is the same
	public boolean logicallyEquivalent(Condition theCondition)
	{
		if (getColumn() != theCondition.getColumn())
			return false;
		if (getOperator() != theCondition.getOperator())
			return false;
		if (!getValue().equals(theCondition.getValue()))
			return false;
		return true;
	}

}
