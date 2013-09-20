package nl.liacs.subdisc;

import java.util.*;

/*
 * TODO
 * FIRST/ LAST_OPERATORS should be defined in terms of (unmodifiable) EnumSets.
 * Condition Objects should have a boolean member indicating whether a
 * value is set for it.
 * Most LogTypeError calls should be changed to new AssertionError().
 */
public class Condition implements Comparable<Condition>
{
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

	private final Column itsColumn;
	private final Operator itsOperator;

	private String itsNominalValue = null;		// ColumnType = NOMINAL
	private ValueSet itsNominalValueSet = null;	// ColumnType = NOMINAL
	private float itsNumericValue = Float.NaN;	// ColumnType = NUMERIC
	private Interval itsInterval = null;		// ColumnType = NUMERIC
	private boolean itsBinaryValue = false;		// ColumnType = BINARY

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

	/*
	 * TODO MM strict constructors, replaces all above, allows for final
	 * value field, and simplified syntax and error checking
	 * value type must be appropriate for Column.type + Operator.type
	 * might get 2 final fields: ColumnBase, and Value
	 * in the end 1 ConditionX per ConditionBase combination would be ideal
	 * as it allows only valid Column-Operator-Value combinations and needs
	 * no checking (implemented using abstract class Condition)
	 * 
	 * TODO MM null check + assert (Column.domain.contains(theValue))
	 * TODO MM check that Operator is valid for Value type
	 */
	/** Condition for NOMINAL Column, single value. */
	public Condition(ConditionBase theConditionBase, String theValue)
	{
		itsColumn = theConditionBase.getColumn();
		itsOperator = theConditionBase.getOperator();
		itsNominalValue = theValue;
	}

	/** Condition for NOMINAL Column, ValueSet. */
	public Condition(ConditionBase theConditionBase, ValueSet theValueSet)
	{
		itsColumn = theConditionBase.getColumn();
		itsOperator = theConditionBase.getOperator();
		itsNominalValueSet = theValueSet;
	}

	/** Condition for BINARY Column. */
	public Condition(ConditionBase theConditionBase, boolean theValue)
	{
		itsColumn = theConditionBase.getColumn();
		itsOperator = theConditionBase.getOperator();
		itsBinaryValue = theValue;
	}

	/** Condition for NUMERIC Column, Interval. */
	public Condition(ConditionBase theConditionBase, Interval theInterval)
	{
		itsColumn = theConditionBase.getColumn();
		itsOperator = theConditionBase.getOperator();
		itsInterval = theInterval;
	}

	/** Condition for NUMERIC Column, single value. */
	public Condition(ConditionBase theConditionBase, float theValue)
	{
		itsColumn = theConditionBase.getColumn();
		itsOperator = theConditionBase.getOperator();
		itsNumericValue = theValue;
	}

	public Column getColumn() { return itsColumn; }

	public Operator getOperator() { return itsOperator; }

	// package-private as ValueSet and Interval are mutable
	// no type validity checks are performed
	String getNominalValue() { return itsNominalValue; }
	ValueSet getNominalValueSet() { return itsNominalValueSet; }
	float getNumericValue() { return itsNumericValue; }
	Interval getNumericInterval() { return itsInterval; }
	boolean getBinaryValue() { return itsBinaryValue; }

	// see class comment on valueIsSet-boolean indicating (non)-virgin state
	private String getValue()
	{
		switch (itsColumn.getType())
		{
			case NOMINAL :
				if (itsNominalValue != null) //single value?
					return itsNominalValue;
				else if (itsNominalValueSet != null) //value set?
					return itsNominalValueSet.toString();
				else
					return null; // TODO no value is set yet

			case NUMERIC :
				if (!Float.isNaN(itsNumericValue)) //single value?
					return Float.toString(itsNumericValue);
				else if (itsInterval != null) //interval?
					return itsInterval.toString();
				else
					return null; // TODO no value is set yet

			/*
			 * TODO a "NaN" return may mean that no value is set yet
			 * or that the value Float.NaN is set deliberately
			 */
			case ORDINAL : return Float.toString(itsNumericValue);

			/*
			 * TODO a "0" return may mean that no value is set yet
			 * or that the value "0" is set deliberately
			 */
			case BINARY : return itsBinaryValue ? "1" : "0";
			default : logTypeError("getValue"); return "";
		}
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

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(32)
					.append(itsColumn.getName()).append(" ")
					.append(itsOperator).append(" ");

		if (itsColumn.getType() == AttributeType.NUMERIC || itsOperator == Operator.ELEMENT_OF)
			sb.append(getValue());
		else
			sb.append("'").append(getValue()).append("'");

		return sb.toString();
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
				/*
				 * reasoning based on (itsNominalValue != null)
				 * is flawed, if setValue(null) is used to set
				 * itsNominalValue for a 'SINGLE_NOMINAL'
				 * Condition, this code erroneously assumes
				 * ValueSet, which comparison will crash with a
				 * NullPointerException.
				 * FIXME add setValue() sanity-checks
				 */
				if (itsNominalValue != null) //single value
				{
					return itsNominalValue.compareTo(theCondition.itsNominalValue);
				}
				else // assumes ValueSet
				{
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
