package nl.liacs.subdisc;

public class Attribute
{
	private int itsIndex;
	private int itsType;
	private String itsName;
	private String itsShort;

	public static final int NUMERIC = 0;
	public static final int NOMINAL = 1;
	public static final int ORDINAL = 2;
	public static final int BINARY = 3;

	public Attribute(int theIndex, String theName, String theShort, int theType)
	{
		itsIndex = theIndex;
		itsName = theName;
		itsShort = theShort;
		itsType = theType;
	}

	//MRML
	public Attribute(String theName, String theShort, int theType)
	{
		itsName = theName;
		itsShort = theShort;
		itsType = theType;
	}

	public int getIndex() { return itsIndex; }
	public int getType() { return itsType; }
	public String getName() { return itsName; }
	public String getShort() { return itsShort; }
	public boolean hasShort() { return (itsShort != null); }
	public String getNameAndShort()
	{
		if (hasShort())
			return itsName + " (" + getShort() + ")";
		else
			return itsName;
	}

	public String getNameOrShort()
	{
		if (hasShort())
			return itsShort;
		else
			return itsName;
	}

	public String getTypeName()
	{
		switch (itsType)
		{
			case NUMERIC: return "numeric";
			case NOMINAL: return "nominal";
			case ORDINAL: return "ordinal";
			case BINARY: return "binary";
		}
		return "unknown type";
	}

	public void print()
	{
		Log.logCommandLine(getNameAndShort() + " " + getTypeName());
	}

	public boolean isNominalType() { return itsType == NOMINAL; }
	public boolean isNumericType() { return itsType == NUMERIC; }
	public boolean isOrdinalType() { return itsType == ORDINAL; }
	public boolean isBinaryType() { return itsType == BINARY; }
}
