package nl.liacs.subdisc;

public class Interval implements Comparable<Interval>
{
	private final float itsLower;
	private final float itsUpper;

	public Interval(float theLower, float theUpper)
	{
		if (Float.isNaN(theLower))
			throw new IllegalArgumentException("Interval<init>: theLower can not be NaN.");
		if (Float.isNaN(theUpper))
			throw new IllegalArgumentException("Interval<init>: theUpper can not be NaN.");
		// -0.0 < 0.0, also NaN is > than anything else, but dealt with
		if (Float.compare(theLower, theUpper) > 0)
			throw new IllegalArgumentException("Interval<init>: the theLower must be <= theUpper.");

		itsLower = theLower;
		itsUpper = theUpper;
	}

	public boolean between(float theValue)
	{
		return (itsLower < theValue) && (theValue <= itsUpper);
	}

	@Override
	public String toString()
	{
		String aLeft = (itsLower == Float.NEGATIVE_INFINITY) ? "-inf" : Float.toString(itsLower);
		String aRight = (itsUpper == Float.POSITIVE_INFINITY) ? "inf)" : (Float.toString(itsUpper) + "]");
		return new StringBuilder(32).append("(").append(aLeft).append(", ").append(aRight).toString();
	}

	@Override
	public int compareTo(Interval theInterval)
	{
		if (this == theInterval)
			return 0;

		// NOTE considers 0.0 to be greater than -0.0
		int cmp = Float.compare(itsLower, theInterval.itsLower);
		return (cmp != 0) ? cmp : Float.compare(itsUpper, theInterval.itsUpper);
	}
}
