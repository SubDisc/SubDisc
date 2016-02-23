package nl.liacs.subdisc;

/*
 * Class intended to replace the current usage of a float array for obtaining various statistics about the subgroup (and its complement)
 */
public class Statistics
{
	private float itsSum = Float.NaN;
	private float itsSumSquaredDeviations = Float.NaN;
	private float itsMedian = Float.NaN;
	private float itsMedianAbsoluteDeviations = Float.NaN;

	//empty statistics object
	Statistics()
	{
	}

	Statistics(float theSum, float theSumSquaredDeviations)
	{
		itsSum = theSum;
		itsSumSquaredDeviations = theSumSquaredDeviations;
	}

	Statistics(float theSum, float theSumSquaredDeviations, float theMedian, float theMedianAbsoluteDeviations)
	{
		itsSum = theSum;
		itsSumSquaredDeviations = theSumSquaredDeviations;
		itsMedian = theMedian;
		itsMedianAbsoluteDeviations = theMedianAbsoluteDeviations;
	}

	public float getSum() { return itsSum; }
	public float getSumSquaredDeviations() { return itsSumSquaredDeviations; }
	public float getMedian() { return itsMedian; }
	public float getMedianAbsoluteDeviations() { return itsMedianAbsoluteDeviations; }
}
