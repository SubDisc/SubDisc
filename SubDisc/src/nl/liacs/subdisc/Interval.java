package nl.liacs.subdisc;

public class Interval
{
	private float itsLower;
	private float itsUpper;

	public Interval(float theLower, float theUpper)
	{
		itsLower = theLower;
		itsUpper = theUpper;
	}

	public boolean between(float theValue)
	{
		return (itsLower < theValue) && (theValue <= itsUpper);
	}

	public String toString()
	{
		String aLeft = (itsLower == Float.NEGATIVE_INFINITY) ? "-inf" : Float.toString(itsLower);
		String aRight = (itsUpper == Float.POSITIVE_INFINITY) ? "inf)" : (Float.toString(itsUpper) + "]");
		return new StringBuilder(32).append("(").append(aLeft).append(", ").append(aRight).toString();
	}

	public float getLower() { return itsLower; }
	public float getUpper() { return itsUpper; }
}
