package nl.liacs.subdisc;

/**
 * SearchStrategy contains all available search strategies.
 */
public enum SearchStrategy implements EnumInterface
{
	BEAM("beam", true, true),
	ROC_BEAM("ROC beam", true, false),
	COVER_BASED_BEAM_SELECTION("cover-based beam selection", true, true),
	BEST_FIRST("best first", false, false),
	DEPTH_FIRST("depth first", false, false),
	BREADTH_FIRST("breadth first",false, false);

	/**
	 * For each SearchStrategy, this is the text that will be used in the
	 * GUI.
	 * This is also the <code>String</code> that will be returned by the
	 * {@link #toString()} method.
	 */
	public final String GUI_TEXT;
	// enforce implementation for new NumericOperatorSettings
	private final boolean isBeam;
	private final boolean requiresSearchWidthParameter;

	private SearchStrategy(String theGuiText, boolean isBeam, boolean requiresSearchWidthParameter)
	{
		GUI_TEXT = theGuiText;
		this.isBeam = isBeam;
		this.requiresSearchWidthParameter = requiresSearchWidthParameter;
	}

	/**
	 * Returns the SearchStartegy corresponding to the <code>String</code>
	 * parameter. This method is case insensitive.
	 *
	 * @param theText the <code>String</code>
	 * ({@link SearchStrategy#GUI_TEXT}) corresponding to a SearchStrategy.
	 *
	 * @return the SearchStrategy corresponding to the <code>String</code>
	 * parameter, or the default SearchStrategy
	 * (as per {@link SearchStrategy#getDefault()}) if no corresponding 
	 * SearchStrategy can be found.
	 */
	public static SearchStrategy fromString(String theText)
	{
		for (SearchStrategy s : SearchStrategy.values())
			if (s.GUI_TEXT.equalsIgnoreCase(theText))
				return s;

		/*
		 * theType cannot be resolved to a SearchStrategy. Log error and
		 * return default.
		 */
		Log.logCommandLine(
			String.format("'%s' is not a valid SearchStrategy. Returning '%s'.",
					theText,
					SearchStrategy.getDefault().GUI_TEXT));
		return SearchStrategy.getDefault();
	}

	/**
	 * Returns the default SearchStrategy {@link SearchStrategy#BEAM}.
	 *
	 * @return the default SearchStrategy.
	 */
	public static SearchStrategy getDefault()
	{
		return SearchStrategy.BEAM;
	}

	/**
	 * Indicates whether this SearchStrategy uses a candidate beam.
	 * 
	 * @return Is this SearchStrategy a beam strategy.
	 */
	public final boolean isBeam()
	{
		return isBeam;
	}

	/**
	 * Indicates whether this SearchStrategy requires a search width
	 * parameter.
	 * 
	 * In general, beam strategies require a search width parameter, whereas
	 * exhaustive search strategies do not.
	 * 
	 * @return Does this SearchStrategy requires a search width parameter.
	 */
	public final boolean requiresSearchWidthParameter()
	{
		return requiresSearchWidthParameter;
	}

	// uses Javadoc from EnumInterface
	@Override
	public String toString()
	{
		return GUI_TEXT;
	}
}
