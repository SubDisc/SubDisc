package nl.liacs.subdisc;

import java.util.*;

/**
 * NumericStrategy contains all available NumericStrategies.
 */
public enum NumericStrategy implements EnumInterface
{
	NUMERIC_BEST_BINS("best-bins", true),
	NUMERIC_BINS("bins", true),
	NUMERIC_BEST("best", true),
	NUMERIC_ALL("all", true),
	NUMERIC_INTERVALS("intervals", false); // only valid for SINGLE_NOMINAL

	/**
	 * For each NumericStrategy, this is the text that will be used in the GUI.
	 * This is also the <code>String</code> that will be returned by the
	 * toString() method.
	 */
	public final String GUI_TEXT;
	private final boolean isNormalValue;

	private NumericStrategy(String theGuiText, boolean isNormalValue)
	{
		GUI_TEXT = theGuiText;
		this.isNormalValue = isNormalValue;
	}

	// dynamically build single immutable instance of 'normal values'
	// robust against code changes, as long a enum declarations are correct
	private static final Set<NumericStrategy> NORMAL_VALUES;
	static
	{
		Set<NumericStrategy> aSet = EnumSet.noneOf(NumericStrategy.class);
		for (NumericStrategy n : NumericStrategy.values())
			if (n.isNormalValue)
				aSet.add(n);
		NORMAL_VALUES = Collections.unmodifiableSet(aSet);
	}

	/**
	 * Returns the NumericStrategy corresponding to the <code>String</code>
	 * parameter. This method is case insensitive.
	 *
	 * @param theText the <code>String</code> corresponding to a
	 * NumericStrategy.
	 *
	 * @return the NumericStrategy corresponding to the <code>String</code>
	 * parameter, or the default NumericStrategy <code>NUMERIC_BINS</code>
	 * if no corresponding NumericStrategy can not be found.
	 */
	public static NumericStrategy fromString(String theText)
	{
		for (NumericStrategy n : NumericStrategy.values())
			if (n.GUI_TEXT.equalsIgnoreCase(theText))
				return n;

		/*
		 * theType cannot be resolved to a NumericStrategy. Log error and
		 * return default.
		 */
		Log.logCommandLine(
			String.format("'%s' is not a valid NumericStrategy. Returning '%s'.",
					theText,
					NumericStrategy.getDefault().GUI_TEXT));
		return NumericStrategy.getDefault();
	}

	/**
	 * Returns an immutable Set of 'normal' NumericStrategy enums, it
	 * contains the same elements as NumericStrategy.values(), except for
	 * NUMERIC_INTERVALS.
	 *
	 * @return the default NumericStrategy.
	 */
	public static Set<NumericStrategy> getNormalValues()
	{
		return NORMAL_VALUES;
	}

	/**
	 * Returns the default NumericStrategy.
	 *
	 * @return the default NumericStrategy.
	 */
	public static NumericStrategy getDefault()
	{
		return NumericStrategy.NUMERIC_BEST_BINS;
	}

	// uses Javadoc from EnumInterface
	@Override
	public String toString()
	{
		return GUI_TEXT;
	}
}
