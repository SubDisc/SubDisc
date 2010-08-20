package nl.liacs.subdisc;

import java.util.ArrayList;

public enum TargetConcept
{
	THE_ONLY_INSTANCE;

	// itsMembers
	private int			itsNrTargetAttributes = 1;	// always 1 in current code
	private TargetType	itsTargetType;
	private Attribute	itsPrimaryTarget;
	private String		itsTargetValue;
	private Attribute	itsSecondaryTarget;
	private ArrayList<Attribute> itsSecondaryTargets;	// better use empty one by default, for-each/null safe

	public enum TargetType
	{
		SINGLE_NOMINAL("single nominal"),
		SINGLE_NUMERIC("single numeric"),
		SINGLE_ORDINAL("single ordinal"),
		DOUBLE_REGRESSION("double regression"),
		DOUBLE_CORRELATION("double correlation"),
		MULTI_LABEL("multi-label"),
		MULTI_BINARY_CLASSIFICATION("multi binary classification");

		public final String text;

		private TargetType(String theText) { text = theText; }

		public boolean isEMM()
		{
			switch(this)
			{
				case DOUBLE_REGRESSION :
				case DOUBLE_CORRELATION :
				case MULTI_LABEL :
				case MULTI_BINARY_CLASSIFICATION : return true;
				default : return false;
			}
		}

		public boolean isImplemented()
		{
			switch(this)
			{
				case SINGLE_NOMINAL :
				case DOUBLE_REGRESSION :
				case DOUBLE_CORRELATION :
				case MULTI_LABEL : return true;
				default : return false;
			}
		}

		public boolean hasSecondaryTarget()
		{
			switch(this)
			{
				case DOUBLE_REGRESSION :
				case DOUBLE_CORRELATION : return true;
				default : return false;
			}
		}
	}

	// member methods
	public int getNrTargetAttributes() { return itsNrTargetAttributes; }
	public void setNrTargetAttributes(int theNr) { itsNrTargetAttributes = theNr; }
	public TargetType getTargetType() { return itsTargetType; }
	public void setTargetType(String theTargetType)
	{
		for(TargetType t : TargetType.values())
		{
			if(t.text.equalsIgnoreCase(theTargetType))
			{
				itsTargetType = t;
				return;
			}
		}
	}

	public Attribute getPrimaryTarget() { return itsPrimaryTarget; }
	public void setPrimaryTarget(Attribute thePrimaryTarget) { itsPrimaryTarget = thePrimaryTarget; }
	public String getTargetValue() { return itsTargetValue; }
	public void setTargetValue(String theTargetValue) { itsTargetValue = theTargetValue; }

	public Attribute getSecondaryTarget() { return itsSecondaryTarget; }
	public void setSecondaryTarget(Attribute theSecondaryTarget) { itsSecondaryTarget = theSecondaryTarget; }

	public boolean isSingleNominal() { return (itsTargetType == TargetType.SINGLE_NOMINAL); }
//	public boolean isEMM() { return itsTargetType.isEMM(); }
//	public boolean isImplemented() { return itsTargetType.isImplemented(); }

/*
	public static boolean isImplemented(int theTargetType )
	{
		return (theTargetType == SINGLE_NOMINAL ||
		theTargetType == DOUBLE_REGRESSION ||
		theTargetType == DOUBLE_CORRELATION ||
		theTargetType == MULTI_LABEL);
	}
	
	public static int getFirstTargetType()	{ return SINGLE_NOMINAL; }
	public static int getLastTargetType()	{ return MULTI_BINARY_CLASSIFICATION; }

	public static String getTypeString(int theType)
	{
		String aType = new String();
		switch(theType)
		{
			//NOMINAL
			case SINGLE_NOMINAL				: { aType = "single nominal"; break; }
			case SINGLE_NUMERIC 			: { aType = "single numeric"; break; }
			case SINGLE_ORDINAL				: { aType = "single ordinal"; break; }
			case DOUBLE_REGRESSION			: { aType = "double regression"; break; }
			case DOUBLE_CORRELATION			: { aType = "double correlation"; break; }
			case MULTI_LABEL				: { aType = "multi-label"; break; }
			case MULTI_BINARY_CLASSIFICATION: { aType = "multi binary classification"; break; }
		}
		return aType;
	}

	public static int getTypeCode(String theType)
	{
		String aType = theType.toLowerCase().trim();
		//NOMINAL
		if ("single nominal".equals(aType)) 					return SINGLE_NOMINAL;
		else if ("single numeric".equals(aType))				return SINGLE_NUMERIC;
		else if ("single ordinal".equals(aType)) 				return SINGLE_ORDINAL;
		else if ("double regression".equals(aType)) 			return DOUBLE_REGRESSION;
		else if ("double correlation".equals(aType)) 			return DOUBLE_CORRELATION;
		else if ("multi-label".equals(aType)) 					return MULTI_LABEL;
		else if ("multi binary classification".equals(aType)) 	return MULTI_BINARY_CLASSIFICATION;
		//default
		return SINGLE_NOMINAL;
	}

	public static boolean isEMM(String theType)
	{
		int aType = getTypeCode(theType);
		return (aType == DOUBLE_REGRESSION ||
			aType == DOUBLE_CORRELATION ||
			aType == MULTI_LABEL ||
			aType == MULTI_BINARY_CLASSIFICATION);
	}
*/
	
	
}
