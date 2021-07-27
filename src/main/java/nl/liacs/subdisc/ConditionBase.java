package nl.liacs.subdisc;

public class ConditionBase
{
	private final Column itsColumn;
	private final Operator itsOperator;

	public ConditionBase(Column theColumn, Operator theOperator)
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

	public Column getColumn() { return itsColumn; }
	public Operator getOperator() { return itsOperator; }

	@Override
	public String toString()
	{
		return itsColumn.getName() + " " + itsOperator.GUI_TEXT;
	}
}
