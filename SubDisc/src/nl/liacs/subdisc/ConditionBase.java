package nl.liacs.subdisc;

class ConditionBase
{
	private final Column itsColumn;
	private final Operator itsOperator;

	ConditionBase(Column theColumn, Operator theOperator)
	{
		if (theColumn == null)
			throw new NullPointerException("ConditionBase: Column can not be null");
		if (theOperator == null)
			throw new NullPointerException("ConditionBase: Operator can not be null");
		if (!theOperator.isValidFor(theColumn.getType()))
			throw new IllegalArgumentException(
				String.format("ConditionBase: Operator '%s' not valid for Column of AttributeType '%s'%n",
						theOperator.GUI_TEXT,
						theColumn.getType().toString()));

		itsColumn = theColumn;
		itsOperator = theOperator;
	}

	Column getColumn() { return itsColumn; }
	Operator getOperator() { return itsOperator; }
}
