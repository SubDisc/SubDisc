package nl.liacs.subdisc;

import java.util.*;

public enum NumericOperatorSetting implements EnumInterface
{
	NORMAL("<html>&#8804;, &#8805;</html>")
	{
		@Override
		EnumSet<Operator> getOperators()
		{
			return EnumSet.of(Operator.LESS_THAN_OR_EQUAL, Operator.GREATER_THAN_OR_EQUAL);
		}

		@Override
		boolean includesEquals()
		{
			return false;
		}
	},
	LEQ("<html>&#8804;</html>")
	{
		@Override
		EnumSet<Operator> getOperators()
		{
			return EnumSet.of(Operator.LESS_THAN_OR_EQUAL);
		}

		@Override
		boolean includesEquals()
		{
			return false;
		}
	},
	GEQ("<html>&#8805;</html>")
	{
		@Override
		EnumSet<Operator> getOperators()
		{
			return EnumSet.of(Operator.GREATER_THAN_OR_EQUAL);
		}

		@Override
		boolean includesEquals()
		{
			return false;
		}
	},
	ALL("<html>&#8804;, &#8805;, =</html>")
	{
		@Override
		EnumSet<Operator> getOperators()
		{
			// XXX leave EQUALS first
			// see SubgroupDiscovery.createsRedundantEquals()
			return EnumSet.of(Operator.EQUALS, Operator.LESS_THAN_OR_EQUAL,	Operator.GREATER_THAN_OR_EQUAL);
		}

		@Override
		boolean includesEquals()
		{
			return true;
		}
	},
	EQ("=")
	{
		@Override
		EnumSet<Operator> getOperators()
		{
			return EnumSet.of(Operator.EQUALS);
		}

		@Override
		boolean includesEquals()
		{
			return true;
		}
	},
	INTERVALS("in")
	{
		@Override
		EnumSet<Operator> getOperators()
		{
			return EnumSet.of(Operator.BETWEEN);
		}

		@Override
		boolean includesEquals()
		{
			return false;
		}
	};

	/**
	 * For each NumericOperatorSetting this is the text that will be used in
	 * the GUI.
	 * This is also the <code>String</code> that will be returned by the
	 * {@link #toString()} method.
	 */
	public final String GUI_TEXT;

	private NumericOperatorSetting(String theGuiText)
	{
		GUI_TEXT = theGuiText;
	}

	// enforce implementation for new NumericOperatorSettings
	abstract EnumSet<Operator> getOperators();
	abstract boolean includesEquals();

	// uses Javadoc from EnumInterface
	@Override
	public String toString()
	{
		return GUI_TEXT;
	}

	/**
	 * Returns the NumericOperatorSetting corresponding to the
	 * <code>String</code> argument, this method is case insensitive.
	 *
	 * @param theText The <code>String</code> corresponding to a
	 * NumericOperatorSetting.
	 *
	 * @return The NumericOperatorSetting corresponding to the
	 * <code>String</code> argument, or the default NumericOperatorSetting
	 * <code>NUMERIC_NORMAL</code> if no corresponding
	 * NumericOperatorSetting can not be found.
	 * 
	 * @see #getDefault()
	 */
	public static NumericOperatorSetting fromString(String theText)
	{
		for (NumericOperatorSetting n : NumericOperatorSetting.values())
			if (n.GUI_TEXT.equalsIgnoreCase(theText))
				return n;

		/*
		 * theType cannot be resolved to a NumericOperators. Log error and
		 * return default.
		 */
		Log.logCommandLine(
			String.format("'%s' is not a valid NumericOperators. Returning '%s'.",
					theText,
					NumericOperatorSetting.getDefault().GUI_TEXT));
		return NumericOperatorSetting.getDefault();
	}

	// not an EnumSet, List enforces order of Operators in GUI presentation
	public static List<NumericOperatorSetting> getNormalValues()
	{
		List<NumericOperatorSetting> aResult = new ArrayList<NumericOperatorSetting>(5);
		aResult.add(NORMAL);
		aResult.add(LEQ);
		aResult.add(GEQ);
		aResult.add(ALL);
		aResult.add(EQ);
		//no intervals!
		return aResult;
	}

	/**
	 * Returns the default NumericOperatorSetting.
	 *
	 * @return the default NumericOperatorSetting.
	 */
	public static NumericOperatorSetting getDefault()
	{
		return NumericOperatorSetting.NORMAL;
	}

	/*
	 * package-private (not in public API)
	 * deprecated, used only in the now obsolete RefinementList constructor 
	 */
	@Deprecated
	static boolean check(NumericOperatorSetting theNO, Operator theOperator)
	{
		if (theNO == NORMAL && (theOperator == Operator.LESS_THAN_OR_EQUAL || theOperator == Operator.GREATER_THAN_OR_EQUAL))
			return true;
		if (theNO == LEQ && theOperator == Operator.LESS_THAN_OR_EQUAL)
			return true;
		if (theNO == GEQ && theOperator == Operator.GREATER_THAN_OR_EQUAL)
			return true;
		if (theNO == EQ && theOperator == Operator.EQUALS)
			return true;
		if (theNO == ALL && (theOperator == Operator.LESS_THAN_OR_EQUAL || theOperator == Operator.GREATER_THAN_OR_EQUAL || theOperator == Operator.EQUALS))
			return true;
		if (theNO ==INTERVALS && theOperator == Operator.BETWEEN)
			return true;

		return false;
	}
}
