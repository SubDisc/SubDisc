package nl.liacs.subdisc;

public class Attribute
{
	private int itsIndex;
	private AttributeType itsType;
	private String itsName;
	private String itsShort;

	public enum AttributeType { NOMINAL, NUMERIC, ORDINAL, BINARY; }

	public Attribute(int theIndex, String theName, String theShort, AttributeType theType)
	{
		itsIndex = theIndex;
		itsName = (theName == null ? String.valueOf(System.nanoTime()) : theName);	// TODO throw warning
		itsShort = (theShort == null ? "" :theShort);
		itsType = theType;
	}

	//MRML
	public Attribute(String theName, String theShort, AttributeType theType)
	{
		itsName = (theName == null ? String.valueOf(System.nanoTime()) : theShort);	// TODO throw warning
		itsShort = (theShort == null ? "" :theShort);
		itsType = theType;
	}

	public int getIndex() { return itsIndex; }	// TODO check, is null for ARFF/MRML
	public AttributeType getType() { return itsType; }
	public String getName() { return itsName; }
	public String getShort() { return itsShort; }
	public boolean hasShort() { return (!itsShort.isEmpty()); }
	public String getNameAndShort() { return itsName + (hasShort() ? " (" + getShort() + ")" : ""); }
	public String getNameOrShort() { return hasShort() ? itsShort : itsName; }
	public String getTypeName() { return itsType.name().toLowerCase(); }

	public void print() { Log.logCommandLine(itsIndex + ":" + getNameAndShort() + " " + getTypeName()); }

	public boolean isNominalType() { return itsType == AttributeType.NOMINAL; }
	public boolean isNumericType() { return itsType == AttributeType.NUMERIC; }
	public boolean isOrdinalType() { return itsType == AttributeType.ORDINAL; }
	public boolean isBinaryType() { return itsType == AttributeType.BINARY; }

	/**
	 * NEW Methods for AttributeType change
	 * Needs more data/type checking
	 */
	public void setType(String theType)
	{
		for(AttributeType at : AttributeType.values())
		{
			if(at.toString().equalsIgnoreCase(theType))
			{
				itsType = at;
				break;
			}
		}
	}
	
}
