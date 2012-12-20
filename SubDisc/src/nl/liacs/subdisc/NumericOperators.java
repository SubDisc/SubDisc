package nl.liacs.subdisc;

import java.util.*;

public enum NumericOperators implements EnumInterface
{
	NUMERIC_NORMAL("<html>&#8804;, &#8805;</html>"),
	NUMERIC_LEQ("<html>&#8804;</html>"),
	NUMERIC_GEQ("<html>&#8805;</html>"),
	NUMERIC_ALL("<html>&#8804;, &#8805;, =</html>"),
	NUMERIC_EQ("="),
	NUMERIC_INTERVALS("in");

	/**
	 * For each NumericOperators, this is the text that will be used in the GUI.
	 * This is also the <code>String</code> that will be returned by the
	 * toString() method.
	 */
	public final String GUI_TEXT;

	private NumericOperators(String theGuiText)
	{
		GUI_TEXT = theGuiText;
	}

	/**
	 * Returns the NumericOperators corresponding to the <code>String</code>
	 * parameter. This method is case insensitive.
	 *
	 * @param theType the <code>String</code> corresponding to a
	 * NumericOperators.
	 *
	 * @return the NumericOperators corresponding to the <code>String</code>
	 * parameter, or the default NumericOperators <code>NUMERIC_BINS</code> if no
	 * corresponding NumericOperators can not be found.
	 */
	public static NumericOperators getNumericOperators(String theType)
	{
		for (NumericOperators n : NumericOperators.values())
			if (n.GUI_TEXT.equalsIgnoreCase(theType))
				return n;

		/*
		 * theType cannot be resolved to a NumericOperators. Log error and
		 * return default.
		 */
		Log.logCommandLine(
			String.format(
					"'%s' is not a valid NumericOperators. Returning '%s'.",
					theType,
					NumericOperators.getDefault().GUI_TEXT));
		return NumericOperators.getDefault();
	}

	// EnumSet
	public static ArrayList<NumericOperators> getNormalValues()
	{
		ArrayList<NumericOperators> aResult = new ArrayList<NumericOperators>(5);
		aResult.add(NUMERIC_NORMAL);
		aResult.add(NUMERIC_LEQ);
		aResult.add(NUMERIC_GEQ);
		aResult.add(NUMERIC_ALL);
		aResult.add(NUMERIC_EQ);
		//no intervals!
		return aResult;
	}

	/**
	 * Returns the default NumericOperators.
	 *
	 * @return the default NumericOperators.
	 */
	public static NumericOperators getDefault()
	{
		return NumericOperators.NUMERIC_NORMAL;
	}

	// uses Javadoc from EnumInterface
	@Override
	public String toString()
	{
		return GUI_TEXT;
	}

	public static boolean check(NumericOperators theNO, Operator theOperator)
	{
		if (theNO == NUMERIC_NORMAL && (theOperator == Operator.LESS_THAN_OR_EQUAL || theOperator == Operator.GREATER_THAN_OR_EQUAL))
			return true;
		if (theNO == NUMERIC_LEQ && theOperator == Operator.LESS_THAN_OR_EQUAL)
			return true;
		if (theNO == NUMERIC_GEQ && theOperator == Operator.GREATER_THAN_OR_EQUAL)
			return true;
		if (theNO == NUMERIC_EQ && theOperator == Operator.EQUALS)
			return true;
		if (theNO == NUMERIC_ALL && (theOperator == Operator.LESS_THAN_OR_EQUAL || theOperator == Operator.GREATER_THAN_OR_EQUAL || theOperator == Operator.EQUALS))
			return true;
		if (theNO == NUMERIC_INTERVALS && theOperator == Operator.BETWEEN)
			return true;

		return false;
	}
}
