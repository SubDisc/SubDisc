package nl.liacs.subdisc;

public class CorrelationMeasure
{
	private int itsSampleSize; //gives the size of this sample
	private double itsXSum;
	private double itsYSum;
	private double itsXYSum;
	private double itsXSquaredSum;
	private double itsYSquaredSum;
	private double itsCorrelation;
	private boolean itsCorrelationIsOutdated = true; //flag indicating whether the latest computed correlation is outdated and whether it should be computed again
	private boolean itsComplementIsOutdated = true; //flag indicating whether the latest computed correlation for its complement is outdated and whether it should be computed again
	private double itsComplementCorrelation = Double.NaN;
	private CorrelationMeasure itsBase = null;
//	public static int itsType = QualityMeasure.CORRELATION_R;
	private int itsType = QualityMeasure.CORRELATION_R;		// TODO check type

	//make a base model from two columns
	public CorrelationMeasure(int theType, Column thePrimaryColumn, Column theSecondaryColumn)
	{
		itsBase = null; //no base model to refer to yet
		itsSampleSize = 0;
		itsXSum = 0;
		itsYSum = 0;
		itsXYSum = 0;
		itsXSquaredSum = 0;
		itsYSquaredSum = 0;
		itsType = theType;
		for (int i = 0, j = thePrimaryColumn.size(); i < j; i++)
			addObservation(thePrimaryColumn.getFloat(i), theSecondaryColumn.getFloat(i));
	}

	public CorrelationMeasure(CorrelationMeasure theBase) throws NullPointerException
	{
		itsBase = theBase;
		if (theBase == null)
			throw (new NullPointerException("Implementation: theBase should not be null"));
		itsSampleSize = 0;
		itsXSum = 0;
		itsYSum = 0;
		itsXYSum = 0;
		itsXSquaredSum = 0;
		itsYSquaredSum = 0;
		itsType = theBase.itsType;
	}

	public CorrelationMeasure(double[] theXValues, double[] theYValues, int theType, CorrelationMeasure theBase)
	{
		itsBase = theBase;
		itsType = theType;
		if(theXValues.length == theYValues.length)
		{
			itsSampleSize = theXValues.length;
			for(int n = 0; n < itsSampleSize; n++)
			{
				itsXSum += theXValues[n];
				itsYSum += theYValues[n];
				itsXYSum += theXValues[n]*theYValues[n];
				itsXSquaredSum += theXValues[n]*theXValues[n];
				itsYSquaredSum += theYValues[n]*theYValues[n];
			}
		}
		else
		{
			Log.error("Length of X-values different from length of the Y-values. Disregarding the values and construct empty CorrelationMeasure");
			itsSampleSize = 0;
			itsXSum = 0;
			itsYSum = 0;
			itsXYSum = 0;
			itsXSquaredSum = 0;
			itsYSquaredSum = 0;
		}
	}

	/**
	 * Adds a new observation after which the current correlation will be outdated. getCorrelation automatically takes
	 * care that the current correlation is returned by checking whether CorrelationMeasure is outdated.
	 *
	 * @param theXValue the x-value of an observation
	 * @param theYValue the y-value of an observation
	 */
	public void addObservation(double theYValue, double theXValue)
	{
		itsSampleSize++;
		itsXSum += theXValue;
		itsYSum += theYValue;
		itsXYSum += theXValue*theYValue;
		itsXSquaredSum += theXValue*theXValue;
		itsYSquaredSum += theYValue*theYValue;
		itsCorrelationIsOutdated = true; //invalidate the computed correlation
		itsComplementIsOutdated = true; //invalidate the computed correlation for the complement
	}

	/**
	 * Returns the correlation given the observations contained by CorrelationMeasure
	 * @return the correlation
	 */
	public double getCorrelation()
	{
		if(itsCorrelationIsOutdated)
			return computeCorrelation();
		else
			return itsCorrelation;
	}

	/**
	 * Computes and returns the correlation given the observations contained by CorrelationMeasure
	 * @return the computed correlation
	 */
	private double computeCorrelation()
	{
		itsCorrelation = (itsSampleSize*itsXYSum - itsXSum*itsYSum)/
			Math.sqrt((itsSampleSize*itsXSquaredSum - itsXSum*itsXSum) * (itsSampleSize*itsYSquaredSum - itsYSum*itsYSum));
		itsCorrelationIsOutdated = false; //set flag to false, so subsequent calls to getCorrelation don't need anymore computation.
		return itsCorrelation;
	}

	/**
	 * Computes the difference between the correlations of this subset and its complement
	 *
	 * @return correlation distance
	 */
	public double computeCorrelationDistance()
	{
		int aSize = getSampleSize();
		int aComplementSize = itsBase.getSampleSize() - getSampleSize();
		if (aSize <= 2 || aComplementSize <=2) // either sample is too small
			return 0;
		else
			return Math.abs(getComplementCorrelation() - getCorrelation());
	}

	public int getSampleSize() { return itsSampleSize; }
	public double getXSum()	{ return itsXSum; }
	public double getYSum()	{ return itsYSum; }
	public double getXYSum() { return itsXYSum; }
	public double getXSquaredSum() { return itsXSquaredSum;	}
	public double getYSquaredSum() { return itsYSquaredSum;	}

	//TODO Verify whether logic as defined below is correct
	public int compareTo(CorrelationMeasure theOtherCorrelationMeasure)
	{
		if(this.getEvaluationMeasureValue() > theOtherCorrelationMeasure.getEvaluationMeasureValue())
			return 1;
		else if(this.getEvaluationMeasureValue() < theOtherCorrelationMeasure.getEvaluationMeasureValue())
			return -1;
		else
			return 0;
	}

	/**
	 * There are different types of quality measures possible all closely related the correlation value.
	 * Corresponding with the correct type as defined in the constructor, the correct QM value is returned.
	 *
	 * @return the quality measure value
	 */
	public double getEvaluationMeasureValue()
	{
		double aCorrelation = getCorrelation();
		switch(itsType)
		{
			case QualityMeasure.CORRELATION_R: 			{ return aCorrelation; }
			case QualityMeasure.CORRELATION_R_NEG: 		{ return -aCorrelation; }
			case QualityMeasure.CORRELATION_R_SQ: 		{ return aCorrelation*aCorrelation;}
			case QualityMeasure.CORRELATION_R_NEG_SQ: 	{ return -1*(aCorrelation*aCorrelation); }
			case QualityMeasure.CORRELATION_DISTANCE: 	{ return computeCorrelationDistance(); }
			case QualityMeasure.CORRELATION_P: 			{ return getPValue(); }
			case QualityMeasure.CORRELATION_ENTROPY: 	{ return computeEntropy(); }
		}
		return aCorrelation;
	}

	/**
	 * TODO: Make use of CorrelationMeasure being comparable
	 *
	 * @param theFirstValue the first parameter to be compared
	 * @param theSecondValue the second parameter to be compared
	 * @return true when the first parameter is better or equal than the second. false if the second value is better
	 */
	public static boolean compareEMValues(double theFirstValue, double theSecondValue)
	{
		if(Double.isNaN(theSecondValue))
			return true;
		else if(Double.isNaN(theFirstValue))
			return false;
		else
			return theFirstValue>=theSecondValue;
	}

	/**
	 * Returns the correlation value for the complement of this subset.
	 * Computes this value if it is not calculated yet and simply returns
	 * the value if it is.
	 *
	 * @return complement correlation value
	 */
	public double getComplementCorrelation()
	{
		if(itsComplementIsOutdated)
		{
			return computeComplementCorrelation();
		}
		else
			return itsComplementCorrelation;
	}

	/**
	 * Calculates the correlation value for the complement set
	 * @return the complement correlation value
	 */
	private double computeComplementCorrelation()
	{
		if(itsBase!=null)
		{
			double aSampleSize = itsBase.getSampleSize() - getSampleSize();
			double anXSum = itsBase.getXSum() - getXSum();
			double aYSum = itsBase.getYSum() - getYSum();
			double anXYSum = itsBase.getXYSum() - getXYSum();
			double anXSquaredSum = itsBase.getXSquaredSum() - getXSquaredSum();
			double aYSquaredSum = itsBase.getYSquaredSum() - getYSquaredSum();
			itsComplementCorrelation = (aSampleSize*anXYSum - anXSum*aYSum)/Math.sqrt((aSampleSize*anXSquaredSum - anXSum*anXSum) * (aSampleSize*aYSquaredSum - aYSum*aYSum));
			itsComplementIsOutdated = false; //Correlation for the complement is up to date, till the next observation is added
			return itsComplementCorrelation;
		}
		return Double.NaN;
	}

	/**
	 * TODO Verify whether solution is the same when z1 - z2 and z2 - z1
	 * @return the <i>p-value</i>
	 */
	public double getPValue()
	{
		int aSize = getSampleSize();
		int aComplementSize = itsBase.getSampleSize() - getSampleSize();
		if (aSize <= 2 || aComplementSize <=2) // either sample is too small
			return 0;

		NormalDistribution aNormalDistro = new NormalDistribution();
		double aComplementSampleSize = itsBase.getSampleSize() - getSampleSize();;
		double aSubgroupDeviation = 1 / Math.sqrt(getSampleSize() - 3);
		double aComplementDeviation = 1 / Math.sqrt(aComplementSampleSize - 3);
		double aZScore = (transform2FisherScore(getCorrelation()) - transform2FisherScore(getComplementCorrelation()))
						 / (aSubgroupDeviation+aComplementDeviation);

		//[example:] z = (obs - mean)/std = (0.9730 - 0.693)/0.333 = 0.841
		double anErfValue = aNormalDistro.calcErf(aZScore/Math.sqrt(2.0));
		double aPValue = 0.5*(1.0 + anErfValue);

		return (aPValue>0.5) ? aPValue : (1-aPValue);
	}

	private double transform2FisherScore(double theCorrelationValue)
	{
		//z' = 0.5 ln[(1+r)/(1-r)]
		return 0.5 * Math.log((1+theCorrelationValue)/(1-theCorrelationValue));
	}

	/**
	 * The correlation distance between the subset and its complement is weighted with the entropy.
	 * The entropy is defined by the function H(p) = -p*log(p)
	 * @return weighted correlation distance
	 */
	public double computeEntropy()
	{
		double aCorrelation = computeCorrelationDistance();
		if (aCorrelation == 0)
			return 0;
		double aFraction;
		aFraction = itsBase!=null ? itsSampleSize / (double) itsBase.getSampleSize() : 1;
		double aWeight = -1 * aFraction * Math.log(aFraction) / Math.log(2);
		return aWeight * aCorrelation;
	}
}
