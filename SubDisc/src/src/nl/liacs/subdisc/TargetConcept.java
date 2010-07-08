package nl.liacs.subdisc;

import java.util.*;

public class TargetConcept
{
	private int itsNrTargetAttributes;
	private int itsTargetType;
	private Attribute itsPrimaryTarget;
	private String itsTargetValue;
	private Attribute itsSecondaryTarget;
	private ArrayList<Attribute> itsSecondaryTargets;

	public static final int SINGLE_NOMINAL 				= 0;
	public static final int SINGLE_NUMERIC 				= 1;
	public static final int SINGLE_ORDINAL 				= 2;
	public static final int DOUBLE_REGRESSION			= 3;
	public static final int DOUBLE_CORRELATION			= 4;
	public static final int MULTI_LABEL	   				= 5;
	public static final int MULTI_BINARY_CLASSIFICATION = 6;

	public TargetConcept()
	{
		itsNrTargetAttributes = 1;
		itsTargetType = SINGLE_NOMINAL;
	}

	public TargetConcept(int theTargetType, int theNrTargetAttributes)
	{
		itsNrTargetAttributes = theNrTargetAttributes;
		itsTargetType = theTargetType;
	}

	public TargetConcept(String theTargetType, int theNrTargetAttributes)
	{
		itsNrTargetAttributes = theNrTargetAttributes;
		itsTargetType = getTypeCode(theTargetType);
	}

	public Attribute getPrimaryTarget() { return itsPrimaryTarget; }
	public void setPrimaryTarget(Attribute thePrimaryTarget) { itsPrimaryTarget = thePrimaryTarget; }
	public String getTargetValue() { return itsTargetValue; }
	public void setTargetValue(String theTargetValue) { itsTargetValue = theTargetValue; }
	public Attribute getSecondaryTarget() { return itsSecondaryTarget; }
	public void setSecondaryTarget(Attribute theSecondaryTarget) { itsSecondaryTarget = theSecondaryTarget; }

	public int getNrTargetAttributes() { return itsNrTargetAttributes; }
	public int getTargetType() { return itsTargetType; }

	public boolean isSingleNominal() { return (itsTargetType == SINGLE_NOMINAL); }
	public boolean isEMM()
	{
		return (itsTargetType == DOUBLE_REGRESSION ||
			itsTargetType == DOUBLE_CORRELATION ||
			itsTargetType == MULTI_LABEL ||
			itsTargetType == MULTI_BINARY_CLASSIFICATION);
	}
	public boolean isImplemented()
	{
		return (itsTargetType == SINGLE_NOMINAL ||
			itsTargetType == DOUBLE_REGRESSION ||
			itsTargetType == DOUBLE_CORRELATION ||
			itsTargetType == MULTI_LABEL);
	}
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
}
