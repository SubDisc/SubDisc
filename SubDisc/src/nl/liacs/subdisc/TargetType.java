package nl.liacs.subdisc;

public enum TargetType
{
	/*
	 * When implementing/adding TargetTypes, all static methods should be
	 * checked and updated.
	 */
	SINGLE_NOMINAL("single nominal"),
	SINGLE_NUMERIC("single numeric"),
	SINGLE_ORDINAL("single ordinal"),
	DOUBLE_REGRESSION("double regression"),
	DOUBLE_CORRELATION("double correlation"),
	MULTI_LABEL("multi-label"),
	MULTI_BINARY_CLASSIFICATION("multi binary classification");

	public final String TEXT;

	private TargetType(String theText) { TEXT = theText; }

	public static TargetType getTargetType(String theType)
	{
		for (TargetType t : TargetType.values())
			if (t.TEXT.equalsIgnoreCase(theType))
				return t;

		/*
		 * theType cannot be resolved to a TargetType. Log error and return
		 * default.
		 */
		Log.logCommandLine(
			String.format("'%s' is not a valid TargetType. Returning '%s'.",
							theType,
							TargetType.getDefaultType().TEXT));
		return TargetType.getDefaultType();
	}

	public static TargetType getDefaultType()
	{
		return TargetType.SINGLE_NOMINAL;
	}

	public static boolean isImplemented(TargetType theType)
	{
		switch (theType)
		{
			case SINGLE_NOMINAL					: return true;
			case SINGLE_NUMERIC					: return true;
			case SINGLE_ORDINAL					: return false;
			case DOUBLE_REGRESSION				: return false;
			case DOUBLE_CORRELATION				: return true;
			case MULTI_LABEL					: return true;
			case MULTI_BINARY_CLASSIFICATION	: return false;
			default :
			{
				unknownTargetType("TargetType.isImplemented: " , theType);
				return false;
			}
		}
	}

	// used by XMLNodeTargetConcept
	public static boolean hasSecondaryTarget(TargetType theType)
	{
		switch (theType)
		{
			case SINGLE_NOMINAL					: return false;
			case SINGLE_NUMERIC					: return false;
			case SINGLE_ORDINAL					: return false;
			case DOUBLE_REGRESSION				: return true;
			case DOUBLE_CORRELATION				: return true;
			case MULTI_LABEL					: return false;
			case MULTI_BINARY_CLASSIFICATION	: return false;
			default :
			{
				unknownTargetType("TargetType.hasSecondaryTarget: " , theType);
				return false;
			}
		}
	}

	// used by MiningWindow.jComboBoxTargetAttributeActionPerformed()
	public static boolean hasSecondaryTargets(TargetType theType)
	{
		switch (theType)
		{
			case SINGLE_NOMINAL					: return false;
			case SINGLE_NUMERIC					: return false;
			case SINGLE_ORDINAL					: return false;
			case DOUBLE_REGRESSION				: return false;
			case DOUBLE_CORRELATION				: return false;
			case MULTI_LABEL					: return true;
			case MULTI_BINARY_CLASSIFICATION	: return true;
			default :
			{
				unknownTargetType("TargetType.hasSecondaryTargets: " , theType);
				return false;
			}
		}
	}

	// used by MiningWindow.jComboBoxTargetAttributeActionPerformed()
	public static boolean hasMiscField(TargetType theType)
	{
		switch (theType)
		{
			case SINGLE_NOMINAL					: return true;
			case SINGLE_NUMERIC					: return false;
			case SINGLE_ORDINAL					: return false;
			case DOUBLE_REGRESSION				: return true;
			case DOUBLE_CORRELATION				: return true;
			case MULTI_LABEL					: return false;
			case MULTI_BINARY_CLASSIFICATION	: return true;	// TODO true?
			default :
			{
				unknownTargetType("TargetType.hasMiscField: " , theType);
				return false;
			}
		}
	}

	// used by MiningWindow.jComboBoxTargetAttributeActionPerformed()
	public static boolean hasTargetAttribute(TargetType theType)
	{
		switch (theType)
		{
			case SINGLE_NOMINAL					: return true;
			case SINGLE_NUMERIC					: return true;
			case SINGLE_ORDINAL					: return true;
			case DOUBLE_REGRESSION				: return true;
			case DOUBLE_CORRELATION				: return true;
			case MULTI_LABEL					: return false;
			case MULTI_BINARY_CLASSIFICATION	: return false;	// TODO true?
			default :
			{
				unknownTargetType("TargetType.hasTargetAttribute: " , theType);
				return false;
			}
		}
	}

	// used by ResultWindow.setTitle()
	public static boolean hasTargetValue(TargetType theType)
	{
		switch (theType)
		{
			case SINGLE_NOMINAL					: return true;
			case SINGLE_NUMERIC					: return false;
			case SINGLE_ORDINAL					: return false;
			case DOUBLE_REGRESSION				: return true;
			case DOUBLE_CORRELATION				: return true;
			case MULTI_LABEL					: return false;
			case MULTI_BINARY_CLASSIFICATION	: return false;	// TODO true?
			default :
			{
				unknownTargetType("TargetType.hasTargetValue: " , theType);
				return false;
			}
		}
	}

	// used by MiningWindow.jComboBoxTargetAttributeActionPerformed()
	public static boolean hasBaseModel(TargetType theType)
	{
		switch (theType)
		{
			case SINGLE_NOMINAL					: return false;
			case SINGLE_NUMERIC					: return false;
			case SINGLE_ORDINAL					: return false;
			case DOUBLE_REGRESSION				: return true;
			case DOUBLE_CORRELATION				: return true;
			case MULTI_LABEL					: return true;
			case MULTI_BINARY_CLASSIFICATION	: return false;	// TODO true?
			default :
			{
				unknownTargetType("TargetType.hasBaseModel: " , theType);
				return false;
			}
		}
	}
/*
	public static boolean isEMM(TargetType theType)
	{
		switch (theType)
		{
			case SINGLE_NOMINAL					: return false;
			case SINGLE_NUMERIC					: return false;
			case SINGLE_ORDINAL					: return false;
			case DOUBLE_REGRESSION				: return true;
			case DOUBLE_CORRELATION				: return true;
			case MULTI_LABEL					: return true;
			case MULTI_BINARY_CLASSIFICATION	: return true;
			default :
			{
				unknownTargetType("TargetType.isEMM: " , theType);
				return false;
			}
		}
	}
*/
	private static void unknownTargetType(String theSource, TargetType theType)
	{
		Log.logCommandLine(
				String.format(theSource + "unknown TargetType '%s'", theType));
	}
}

