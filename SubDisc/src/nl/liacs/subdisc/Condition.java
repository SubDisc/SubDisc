package nl.liacs.subdisc;

import java.util.*;

public class Condition implements Comparable<Condition>
{
	private final Column itsColumn;
	private final Operator itsOperator;

	private String itsNominalValue = null;		// ColumnType = NOMINAL
	private ValueSet itsNominalValueSet = null;	// ColumnType = NOMINAL
	private float itsNumericValue = Float.NaN;	// ColumnType = NUMERIC
	private Interval itsInterval = null;		// ColumnType = NUMERIC
	private boolean itsBinaryValue = false;		// ColumnType = BINARY

/* *****************************************************************************
 * START OF OLD CONDITION CODE, SHOULD NOT BE USED ANYMORE
 ******************************************************************************/

	private static final Set<Operator> OPERATORS = Operator.set();
	/*
	 * FIXME these should be defined in terms of EnumSets
	 * NOTE ConditionBaseSet.getBaseConditions() assumes there is just 1
	 * BINARY operator, if this ever changes, update that code.
	 */
	// Binary Operator Constants
	public static final Operator FIRST_BINARY_OPERATOR	= Operator.EQUALS;
	public static final Operator LAST_BINARY_OPERATOR	= Operator.EQUALS;

	// Nominal Operator  Constants
	// this allows in (ELEMENT_OF) and =
	public static final Operator FIRST_NOMINAL_OPERATOR	= Operator.ELEMENT_OF;
	public static final Operator LAST_NOMINAL_OPERATOR	= Operator.EQUALS;

	// Numeric Operator  Constants
	// this allows =, <=, >= and in (BETWEEN)
	public static final Operator FIRST_NUMERIC_OPERATOR	= Operator.EQUALS;
	public static final Operator LAST_NUMERIC_OPERATOR	= Operator.BETWEEN;

	/**
	 * Default initialisation values for {@link Column}} of
	 * {@link AttributeType}:<br>
	 * {@link AttributeType#NOMINAL} = <code>null</code>,<br>
	 * {@link AttributeType#NUMERIC} = Float.NaN,<br>
	 * {@link AttributeType#BINARY} = <code>false</code>.
	 *
	 * @param theColumn The Column on which this Condition will be defined.
	 * 
	 * @throws {@link NullPointerException} if the parameter is
	 * <code>null</code>.
	 */
	@Deprecated
	public Condition(Column theColumn)
	{
		itsColumn = theColumn;

		// causes NullPointerException if (theColumn == null)
		switch (itsColumn.getType())
		{
			case NOMINAL : itsOperator = FIRST_NOMINAL_OPERATOR; return;
			case NUMERIC : itsOperator = FIRST_NUMERIC_OPERATOR; return;
			case ORDINAL : itsOperator = FIRST_NUMERIC_OPERATOR; return;
			case BINARY : itsOperator = FIRST_BINARY_OPERATOR; return;
			default :
			{
				itsOperator = FIRST_NOMINAL_OPERATOR;
				logTypeError("<init>");
				return;
			}
		}
	}

	/**
	 * Default initialisation values for {@link Column}} of
	 * {@link AttributeType}:<br>
	 * {@link AttributeType#NOMINAL} = <code>null</code>,<br>
	 * {@link AttributeType#NUMERIC} = Float.NaN,<br>
	 * {@link AttributeType#BINARY} = <code>false</code>.
	 *
	 * @param theColumn The Column on which this Condition will be defined.
	 * 
	 * @throws {@link NullPointerException} if the parameter is
	 * <code>null</code>.
	 */
	@Deprecated
	public Condition(Column theColumn, Operator theOperator)
	{
		if (theColumn == null)
			throw new NullPointerException();

		itsColumn = theColumn;
		itsOperator = theOperator;
	}

	// obviously does not deep-copy itsColumn
	// itsOperator is primitive type, no need for deep-copy
	@Deprecated
	public Condition copy()
	{
		Condition aCopy = new Condition(itsColumn, itsOperator);
		aCopy.itsNominalValue = this.itsNominalValue;
		aCopy.itsNominalValueSet = this.itsNominalValueSet; //shallow copy!
		aCopy.itsNumericValue = this.itsNumericValue;
		aCopy.itsInterval = this.itsInterval; //shallow copy!
		aCopy.itsBinaryValue = this.itsBinaryValue;
		return aCopy;
	}

	@Deprecated
	public boolean hasNextOperator()
	{
		final AttributeType aType = itsColumn.getType();
		if (itsOperator == LAST_BINARY_OPERATOR && aType == AttributeType.BINARY)
			return false;
		if (itsOperator == LAST_NOMINAL_OPERATOR && aType == AttributeType.NOMINAL)
			return false;
		if (itsOperator == LAST_NUMERIC_OPERATOR && aType == AttributeType.NUMERIC)
			return false;
		return true;
	}

	@Deprecated
	public Operator getNextOperator()
	{
		//return hasNextOperator() ? itsOperator+1 : NOT_AN_OPERATOR;
		if (hasNextOperator())
		{
			// hasNextOperator() sort of guarantees i.hasNext() 
			for (Iterator<Operator> i = OPERATORS.iterator(); i.hasNext(); )
				if (itsOperator == i.next())
					return i.next();
		}

		return Operator.NOT_AN_OPERATOR;
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
/*
	@Override
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

	/* EVALUATION IS PUSHED TO Column.evaluate(BitSet, Condition) */

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
		switch (itsOperator)
		{
			case ELEMENT_OF :
				return itsNominalValueSet.contains(theValue);
			case EQUALS :
				return theValue.equals(itsNominalValue);
			default :
			{
				logError("nominal");
				return false; // FIXME MM IllegalArgumentException
			}
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
	public boolean evaluate(float theValue)
	{
		switch (itsOperator)
		{
			// FIXME MM should throw error on evaluate(float) call
			// on a Condition with nominal Column + EQUALS operator
			case EQUALS :
				return theValue == itsNumericValue;
			case LESS_THAN_OR_EQUAL :
				return theValue <= itsNumericValue;
			case GREATER_THAN_OR_EQUAL :
				return theValue >= itsNumericValue;
			case BETWEEN:
				return itsInterval.between(theValue);
			default :
			{
				logError("numeric");
				return false; // FIXME MM IllegalArgumentException
			}
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
		if (itsOperator != Operator.EQUALS)
			logError("binary"); // FIXME MM IllegalArgumentException
		return itsBinaryValue == theValue;
	}

	private void logError(String theColumnType)
	{
		Log.error(String.format("incorrect operator for %s column",
					theColumnType));
	}

	private void logTypeError(String theMethod)
	{
		Log.logCommandLine(String.format("%s.%s(): unknown AttributeType '%s'. Returning '%s'.",
							getClass().getSimpleName(),
							theMethod,
							itsColumn.getType(),
							itsOperator));
	}

/* *****************************************************************************
 * END OF OLD CONDITION CODE
 ******************************************************************************/

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

		itsColumn = theConditionBase.getColumn();
		itsOperator = theConditionBase.getOperator();
		itsNominalValue = theValue;
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

		itsColumn = theConditionBase.getColumn();
		itsOperator = theConditionBase.getOperator();
		itsNominalValueSet = theValueSet;
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

		itsColumn = aColumn;
		itsOperator = theConditionBase.getOperator();
		itsBinaryValue = theValue;
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

		itsColumn = aColumn;
		itsOperator = anOperator;
		itsInterval = theInterval;
	}

	/** Condition for NUMERIC Column, single value, NaN is not allowed. */
	public Condition(ConditionBase theConditionBase, float theValue)
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

		itsColumn = aColumn;
		itsOperator = anOperator;
		itsNumericValue = theValue;
	}

	private static final IllegalArgumentException exception(String pre, String post)
	{
		return new IllegalArgumentException(pre + " can not be " + post);
	}

	public Column getColumn() { return itsColumn; }

	public Operator getOperator() { return itsOperator; }

	// no type validity checks are performed
	public String getNominalValue() { return itsNominalValue; }
	public ValueSet getNominalValueSet() { return itsNominalValueSet; }
	public float getNumericValue() { return itsNumericValue; }
	public Interval getNumericInterval() { return itsInterval; }
	public boolean getBinaryValue() { return itsBinaryValue; }

	/* Assumes values are set in constructor, and per Column/Operator type*/
	private String getValue()
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
		return new StringBuilder(32)
					.append(itsColumn.getName()).append(" ")
					.append(itsOperator).append(" ")
					.append(getValue())
					.toString();
	}

	// throws NullPointerException if theCondition is null.
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
				if (itsNominalValue != null) //single value
					return itsNominalValue.compareTo(theCondition.itsNominalValue);
				else // assumes ValueSet
				{
					assert (itsNominalValueSet != null);
					if (itsNominalValueSet != theCondition.itsNominalValueSet)
						throw new AssertionError(String.format("Multiple %ss for %s '%s'",
											itsNominalValueSet.getClass().getSimpleName(),
											itsColumn.getClass().getSimpleName(),
											itsColumn.getName()));
					return 0;
				}
			}
			// do not use fall-through to ORDINAL
			case NUMERIC :
			{
				return Float.compare(itsNumericValue, theCondition.itsNumericValue);
			}
			case ORDINAL :
			{
				return Float.compare(itsNumericValue, theCondition.itsNumericValue);
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
				throw new AssertionError(String.format("ERROR: compareTo()\n%s\n%s",
									this.toString(),
									theCondition.toString()));
			}
		}
	}
}
