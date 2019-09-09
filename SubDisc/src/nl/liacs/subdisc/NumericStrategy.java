package nl.liacs.subdisc;

import java.util.*;

/**
 * NumericStrategy contains all available NumericStrategies.
 */
public enum NumericStrategy implements EnumInterface
{
	NUMERIC_BINS("bins", true, true, true),
	NUMERIC_BEST_BINS("best-bins", true, true, true),
	NUMERIC_BEST("best", true, false, true),
	NUMERIC_ALL("all", true, false, true),
	NUMERIC_INTERVALS("intervals", false, false, false), // only valid for SINGLE_NOMINAL
	// XXX MM - temporary code
	// vm = VikaMine style bounded intervals, instead of half intervals
	//
	// consecutive = creates nrBins consecutive equal height intervals
	// cartesian = creates nrBins^2 intervals
	//
	// all = allow all candidates through
	// best = allow only best scoring candidate through
	@Deprecated
	NUMERIC_VIKAMINE_CONSECUTIVE_ALL("vm-consecutive-all", true, true, false),
	@Deprecated
	NUMERIC_VIKAMINE_CONSECUTIVE_BEST("vm-consecutive-best", true, true, false);
//	NUMERIC_VIKAMINE_CARTESIAN_ALL("vm-cartesian-all", true, true, false),
//	NUMERIC_VIKAMINE_CARTESIAN_BEST("vm-cartesian-best", true, true, false);

	/**
	 * For each NumericStrategy, this is the text that will be used in the GUI.
	 * This is also the <code>String</code> that will be returned by the
	 * toString() method.
	 */
	public final String GUI_TEXT;
	private final boolean isNormalValue;
	private final boolean isDiscretiser;
	private final boolean isForHalfInterval;

	private NumericStrategy(String theGuiText, boolean isNormalValue, boolean isDiscretiser, boolean isForHalfInterval)
	{
		GUI_TEXT = theGuiText;
		this.isNormalValue = isNormalValue;
		this.isDiscretiser = isDiscretiser;
		this.isForHalfInterval = isForHalfInterval;
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

		throw new IllegalArgumentException("NumericStrategy.fromString(): unknown NumericStrategy " + theText);
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
	 * Indicates whether this NumericStrategy discretises the input domain.
	 * 
	 * If so, not all values of the input domain are used, but only a
	 * limited number of them.
	 *
	 * @return Is this NumericStrategy a discretiser.
	 */
	public boolean isDiscretiser()
	{
		return isDiscretiser;
	}

	/**
	 * Indicates whether this NumericStrategy creates half-intervals or
	 * bounded intervals.
	 *
	 * @return Does this NumericStrategy create half-intervals.
	 */
	public boolean isForHalfInterval()
	{
		return isForHalfInterval;
	}

	// uses Javadoc from EnumInterface
	@Override
	public String toString()
	{
		return GUI_TEXT;
	}
}
