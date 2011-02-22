package nl.liacs.subdisc;

/**
 * SearchStrategy contains all available search strategies.
 */
public enum SearchStrategy implements EnumInterface
{
	BEAM("beam"),
	BEST_FIRST("best first"),
	DEPTH_FIRST("depth_first"),
	BREADTH_FIRST("breadth first");

	/**
	 * For each SearchStrategy, this is the text that will be used in the GUI.
	 * This is also the <code>String</code> that will be returned by the
	 * toString() method.
	 */
	public final String GUI_TEXT;

	private SearchStrategy(String theGuiText)
	{
		GUI_TEXT = theGuiText; 
	}

	/**
	 * Returns the SearchStartegy corresponding to the <code>String</code>
	 * parameter. This method is case insensitive.
	 * 
	 * @param theType the <code>String</code> corresponding to a SearchStrategy.
	 * 
	 * @return the SearchStrategy corresponding to the <code>String</code>
	 * parameter, or the default SearchStrategy <code>BEAM</code> if no
	 * corresponding SearchStrategy can not be found.
	 */
	public static SearchStrategy get(String theType)
	{
		for (SearchStrategy s : SearchStrategy.values())
			if (s.GUI_TEXT.equalsIgnoreCase(theType))
				return s;

		/*
		 * theType cannot be resolved to an SearchStrategy. Log error and
		 * return default.
		 */
		Log.logCommandLine(
			String.format(
					"'%s' is not a valid SearchStrategy. Returning '%s'.",
					theType,
					SearchStrategy.getDefault().GUI_TEXT));
		return SearchStrategy.getDefault();
	}

	/**
	 * Returns the default SearchStrategy.
	 * 
	 * @return the default SearchStrategy.
	 */
	public static SearchStrategy getDefault()
	{
		return SearchStrategy.BEAM;
	}

	// uses Javadoc from EnumInterface
	@Override
	public String toString()
	{
		return GUI_TEXT;
	}
}
