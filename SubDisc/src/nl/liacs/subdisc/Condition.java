package nl.liacs.subdisc;

public class Condition implements Comparable<Condition>
{
	// Operator Constants
	public static final int DOES_NOT_EQUAL		= -1;
	public static final int EQUALS			= 0;
	public static final int LESS_THAN_OR_EQUAL	= 1;
	public static final int GREATER_THAN_OR_EQUAL	= 2;
	public static final int NOT_AN_OPERATOR		= 99;

	// Binary Operator Constants
	public static final int FIRST_BINARY_OPERATOR	= EQUALS;
	public static final int LAST_BINARY_OPERATOR	= EQUALS;

	// Nominal Operator  Constants
	public static final int FIRST_NOMINAL_OPERATOR	= DOES_NOT_EQUAL;
	public static final int LAST_NOMINAL_OPERATOR	= EQUALS;

	// Numeric Operator  Constants
	//this allows =, <= and >=
	public static final int FIRST_NUMERIC_OPERATOR	= EQUALS;
	public static final int LAST_NUMERIC_OPERATOR	= GREATER_THAN_OR_EQUAL;

	private Column itsColumn;
	private int itsOperator;
	private String itsValue = null;

	/*
	 * Conditions are evaluated often (ie. for each item of the Column)
	 * the constant parsing of a String to float makes evaluate(float)
	 * extremely inefficient.
	 * Performance of evaluate(boolean) is also sub-optimal, and return
	 * values may be unexpected.
	 */
	// private float itsNumericValue;
	// private boolean itsBooleanValue;

	public Condition(Column theColumn)
	{
		// TODO null check
		itsColumn = theColumn;
		switch (itsColumn.getType())
		{
			case NOMINAL : itsOperator = FIRST_NOMINAL_OPERATOR; return;
			case NUMERIC : itsOperator = FIRST_NUMERIC_OPERATOR; return;
			case ORDINAL : itsOperator = FIRST_NUMERIC_OPERATOR; return;
			case BINARY : itsOperator = FIRST_BINARY_OPERATOR; return;
			default :
			{
				itsOperator = FIRST_NOMINAL_OPERATOR;
				Log.logCommandLine(
					String.format(
						"Condition<init>: unknown AttributeType '%s'. Returning '%s'.",
						itsColumn.getType(),
						getOperatorString()));
				return;
			}
		}
	}

	// TODO null check
	public Condition(Column theColumn, int theOperator)
	{
		itsColumn = theColumn;
		itsOperator = theOperator;
	}

	// obviously does not deep-copy itsColumn
	// itsOperator is primitive type, no need for deep-copy
	// itsValue new String not really needed, as none of current code ever
	// changes it, beside it can be overridden through setValue anyway.
	public Condition copy()
	{
		Condition aNewCondition = new Condition(itsColumn, itsOperator);
		// new for deep-copy? not strictly needed for code
		aNewCondition.setValue(itsValue == null ? null : new String(getValue()));
		return aNewCondition;
	}

	public Column getColumn() { return itsColumn; }

	public int getOperator() { return itsOperator; }

	private String getValue() { return itsValue; }

	// Refinement getRefinedSubgroup
	// SubgroupDiscovery single nominal constructor
	// Validation getRandomConditionList randomConditions randomSubgroups
	public void setValue(String theValue) { itsValue = theValue; }

	public boolean checksNotEquals() { return itsOperator == DOES_NOT_EQUAL; }

	public boolean hasNextOperator()
	{
		if (itsOperator == LAST_BINARY_OPERATOR && itsColumn.isBinaryType())
			return false;
		if (itsOperator == LAST_NOMINAL_OPERATOR && itsColumn.isNominalType())
			return false;
		if (itsOperator == LAST_NUMERIC_OPERATOR && itsColumn.isNumericType())
			return false;
		return true;
	}

	public int getNextOperator()
	{
		return hasNextOperator() ? itsOperator+1 : NOT_AN_OPERATOR;
	}

	/**
	 * Evaluate Condition for {@link Column Column} of type
	 * {@link AttributeType#NOMINAL AttributeType.NOMINAL}.
	 * <p>
	 * The evaluation is performed using the operator and value set for this
	 * Condition, and {@link String#equals(Object) String.equals()}.
	 * 
	 * @param theValue the value to compare to the value of this Condition.
	 * 
	 * @return <code>true</code> if the evaluation yields <code>true</code>,
	 * <code>false</code> otherwise.
	 */
	public boolean evaluate(String theValue)
	{
		switch(itsOperator)
		{
			case DOES_NOT_EQUAL :
				return (!theValue.equals(itsValue));
			case EQUALS :
				return (theValue.equals(itsValue));
			case LESS_THAN_OR_EQUAL :
			case GREATER_THAN_OR_EQUAL :
			{
				logError("nominal");
				return false;
			}
			default : return false;
		}
	}

	/**
	 * Evaluate Condition for {@link Column Column} of type
	 * {@link AttributeType#NUMERIC AttributeType.NUMERIC}.
	 * <p>
	 * The evaluation is performed using the operator and value set for this
	 * Condition.
	 * 
	 * @param theValue the value to compare to the value of this Condition.
	 * 
	 * @return <code>true</code> if the evaluation yields <code>true</code>,
	 * <code>false</code> otherwise.
	 */
	public boolean evaluate(Float theValue)
	{
		switch(itsOperator)
		{
			case DOES_NOT_EQUAL :
			{
				logError("numeric");
				return false;
			}
			case EQUALS :
				return theValue == Float.parseFloat(itsValue);
			case LESS_THAN_OR_EQUAL :
				return theValue <= Float.parseFloat(itsValue);
			case GREATER_THAN_OR_EQUAL :
				return theValue >= Float.parseFloat(itsValue);
			default : return false;
		}
	}

	/**
	 * Evaluate Condition for {@link Column Column} of type
	 * {@link AttributeType#BINARY AttributeType.BINARY}.
	 * <p>
	 * The evaluation is performed using the operator and value set for this
	 * Condition.
	 * 
	 * @param theValue the value to compare to the value of this Condition.
	 * 
	 * @return <code>true</code> if the evaluation yields <code>true</code>,
	 * <code>false</code> otherwise.
	 */
	public boolean evaluate(boolean theValue)
	{
		if (itsOperator != EQUALS)
			logError("binary");
		// MM 'boolean' test may return true for numeric columns?
		// all evaluate()'s should test on ColumnType, not on Operator
		return itsValue.equals(theValue ? "1" : "0");
	}

	private void logError(String theColumnType)
	{
		Log.error(String.format("incorrect operator for %s column",
					theColumnType));
	}

	private String getOperatorString()
	{
		switch(itsOperator)
		{
			case DOES_NOT_EQUAL		: return "!=";
			case EQUALS			: return "=";
			case LESS_THAN_OR_EQUAL		: return "<=";
			case GREATER_THAN_OR_EQUAL	: return ">=";
			default : return "";
		}
	}

	// never used atm
	private String toCleanString()
	{
		String aName = itsColumn.hasShort() ? itsColumn.getShort() : itsColumn.getName();

		if (itsColumn.isNumericType())
			return String.format("%s %s %s", aName, getOperatorString(), itsValue);
		else
			return String.format("%s %s '%s'", aName, getOperatorString(), itsValue);
	}

	// used by ConditionList.toString()
	@Override
	public String toString()
	{
		return String.format("%s %s '%s'", itsColumn.getName(), getOperatorString(), itsValue);
	}

	/*
	 * NOTE
	 * Never override equals() without also overriding hashCode().
	 * Some (Collection) classes use equals to determine equality, others
	 * use hashCode() (eg. java.lang.HashMap).
	 * Failing to override both methods will result in strange behaviour.
	 *
 	 * NOTE
	 * Map interface expects compareTo and equals to be consistent.
	 *
	 * Used by ConditionList.findCondition().
	 * @see java.lang.Object#equals(java.lang.Object)
	 */

/*	@Override
	public boolean equals(Object theObject)
	{
		if (theObject == null || this.getClass() != theObject.getClass())
			return false;
		Condition aCondition = (Condition) theObject;
		if (itsColumn == aCondition.getColumn() &&
			itsOperator == aCondition.getOperator() &&
			itsValue.equals(aCondition.getValue()))
			return true;
		return false;
	}
*/
	// throws NullPointerException if theCondition is null.
	@Override
	public int compareTo(Condition theCondition)
	{
		if (this == theCondition)
			return 0;
		else if (this.getColumn().getIndex() < theCondition.getColumn().getIndex())
			return -1;
		else if (this.getColumn().getIndex() > theCondition.getColumn().getIndex())
			return 1;
		// same column, check operator
		else if (this.getOperator() < theCondition.getOperator())
			return -1;
		else if (this.getOperator() > theCondition.getOperator())
			return 1;
		// same column, same operator, check on value
		else if (this.getColumn().isNumericType())
			return (Float.valueOf(this.getValue()).compareTo(Float.valueOf(theCondition.getValue())));
		else
		{
			// String.compareTo() does not strictly return -1, 0, 1
			int aCompare = this.getValue().compareTo(theCondition.getValue());
			return (aCompare < 0 ? -1 : aCompare > 0 ? 1 : 0);
		}
	}
}

