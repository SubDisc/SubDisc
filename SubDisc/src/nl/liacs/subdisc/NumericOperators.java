package nl.liacs.subdisc;

public enum NumericOperators implements EnumInterface
{
	NUMERIC_NORMAL("<=, >="),
	NUMERIC_LEQ("<="),
	NUMERIC_GEQ(">="),
	NUMERIC_ALL("<=, >=, ="),
	NUMERIC_EQ("=");

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
		for(NumericOperators n : NumericOperators.values())
			if(n.GUI_TEXT.equalsIgnoreCase(theType))
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

	public static boolean check(NumericOperators theNO, int theOperator)
	{
		if (theNO == NUMERIC_NORMAL && (theOperator == Condition.LESS_THAN_OR_EQUAL ||
										theOperator == Condition.GREATER_THAN_OR_EQUAL))
			return true;
		if (theNO == NUMERIC_LEQ && (theOperator == Condition.LESS_THAN_OR_EQUAL))
			return true;
		if (theNO == NUMERIC_GEQ && (theOperator == Condition.GREATER_THAN_OR_EQUAL))
			return true;
		if (theNO == NUMERIC_EQ && (theOperator == Condition.EQUALS))
			return true;
		if (theNO == NUMERIC_ALL )
			return true;

		return false;
	}
}