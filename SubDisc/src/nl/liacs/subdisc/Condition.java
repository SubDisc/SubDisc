package nl.liacs.subdisc;

public class Condition
{
	// Operator Constants
	public static final int EQUALS                  = 0;
	public static final int LESS_THAN_OR_EQUAL      = 1;
	public static final int GREATER_THAN_OR_EQUAL   = 2;
	public static final int NOT_AN_OPERATOR			= 99;

	// Nominal Operator  Constants
	public static final int FIRST_NOMINAL_OPERATOR  = EQUALS;
	public static final int LAST_NOMINAL_OPERATOR   = FIRST_NOMINAL_OPERATOR;

	// Numeric Operator  Constants
	public static final int FIRST_NUMERIC_OPERATOR  = LESS_THAN_OR_EQUAL;
	public static final int SECOND_NUMERIC_OPERATOR = GREATER_THAN_OR_EQUAL;
	public static final int LAST_NUMERIC_OPERATOR   = SECOND_NUMERIC_OPERATOR;

	private Attribute itsAttribute;
	private int itsOperator;
	private String itsValue = null;

	public Condition(Attribute theAttribute)
	{
		itsAttribute = theAttribute;
		if (itsAttribute.isNumericType())
			itsOperator = FIRST_NUMERIC_OPERATOR;
		else // if (itsAttribute.isNominalType())
			itsOperator = FIRST_NOMINAL_OPERATOR;
	}

	public Condition(Attribute theAttribute, int theOperator)
	{
		itsAttribute = theAttribute;
		itsOperator = theOperator;
	}

	public Condition copy()
	{
		Condition aNewCondition = new Condition(itsAttribute, itsOperator);
		aNewCondition.setValue(new String(getValue()));
		return aNewCondition;
	}

	public boolean equals(Object theObject)
	{
		if (theObject.getClass() != this.getClass())
			return false;
		Condition aCondition = (Condition) theObject;
		if (itsAttribute == aCondition.getAttribute() && itsOperator == aCondition.getOperator() &&
			itsValue.equals(aCondition.getValue()))
			return true;
		return false;
	}

	public boolean hasNextOperator()
	{
		return ((this.itsOperator != LAST_NOMINAL_OPERATOR)&&(this.itsOperator != LAST_NUMERIC_OPERATOR));
	}

	public int getNextOperator()
	{
		if ( this.hasNextOperator() )
			return itsOperator+1;
		else	// No Next Operators
			return NOT_AN_OPERATOR;
	}

	public String getValue() { return itsValue; }

	public void setValue(String theValue)
	{
		itsValue = theValue;
	}

	public String getAttributeName() { return itsAttribute.getName(); }

	public Attribute getAttribute() { return itsAttribute; }

	public String getAggregateString()
	{
		String anAggregateString = null;
		switch(itsOperator)
		{
			case LESS_THAN_OR_EQUAL		: { anAggregateString = "MIN"; break; }
			case GREATER_THAN_OR_EQUAL	: { anAggregateString = "MAX"; break; }
		}
		return anAggregateString;
	}

	public String toString()
	{
		return itsAttribute.getName() + " " + getOperatorString() + " '" + itsValue + "'";
	}

	public String toCleanString()
	{
		String aName = itsAttribute.getName();
		if (itsAttribute.hasShort())
			aName = itsAttribute.getShort();

		if (itsAttribute.isNumericType())
			return aName  + " " + getOperatorString() + " " + itsValue;
		else
			return aName + " " + getOperatorString() + " '" + itsValue + "'";
	}

	public String getOperatorString()
	{
		String anOperatorString = "";
		switch(itsOperator)
		{
			case EQUALS					: { anOperatorString = "="; break; }
			case LESS_THAN_OR_EQUAL		: { anOperatorString = "<="; break; }
			case GREATER_THAN_OR_EQUAL	: { anOperatorString = ">="; break; }
		}
		return anOperatorString;
	}

	public int getOperator() { return itsOperator; }

	public boolean evaluate(String theValue)
	{
		switch(itsOperator)
		{
			case EQUALS					:
			{
				return (theValue.equals(itsValue));
			}
			case LESS_THAN_OR_EQUAL		:
			{
				return (Float.parseFloat(theValue) <= Float.parseFloat(itsValue));
			}
			case GREATER_THAN_OR_EQUAL	:
			{
				return (Float.parseFloat(theValue) >= Float.parseFloat(itsValue));
			}
		}
		return false;
	}

	//for boolean values only
	public boolean evaluate(boolean theValue)
	{
		boolean aResult;
		if (itsOperator != EQUALS)
			Log.error("incorrect operator for boolean attribute");
		if (theValue) //value=1
			aResult = itsValue.equals("1");
		else
			aResult = itsValue.equals("0");

		return aResult;
	}
}