package nl.liacs.subdisc;

import java.util.*;

public class RegressionMeasure
{
	private int itsSampleSize;
	private double itsXSum; //The sum of all X-values
	private double itsYSum; // The sum of all Y-values
	private double itsXYSum;// SUM(x*y)
	private double itsXSquaredSum;// SUM(x*x)
	private double itsYSquaredSum;// SUM(y*y)

	private double itsErrorTermSquaredSum;//Sum of all the squared error terms, changes whenever the regression function is updated
	private double itsComplementErrorTermSquaredSum;//Sum of all the squared error terms of this complement, changes whenever the regression function is updated

	private double itsSlope; //The slope-value of the regression function
	private double itsIntercept;//The intercept-value of the regression function

	private double itsCorrelation;

	private ArrayList<DataPoint> itsData;//Stores all the datapoints for this measure
	private ArrayList<DataPoint> itsComplementData = new ArrayList<DataPoint>();//Stores all the datapoints for the complement

	public static int itsType;
	private RegressionMeasure itsBase = null;

	//make a base model from two columns
	public RegressionMeasure(int theType, Column thePrimaryColumn, Column theSecondaryColumn)
	{
		itsBase = null; //this *is* the base
		itsType = theType;
		itsSampleSize = thePrimaryColumn.size();
		itsData = new ArrayList<DataPoint>(itsSampleSize);
		itsComplementData = null; //will remain empty for the base RM

		for(int i=0; i<itsSampleSize; i++)
		{
			itsXSum += thePrimaryColumn.getFloat(i);
			itsYSum += theSecondaryColumn.getFloat(i);
			itsXYSum += thePrimaryColumn.getFloat(i)*theSecondaryColumn.getFloat(i);
			itsXSquaredSum += thePrimaryColumn.getFloat(i)*thePrimaryColumn.getFloat(i);
			itsYSquaredSum += theSecondaryColumn.getFloat(i)*theSecondaryColumn.getFloat(i);

			//for each entry added to this measure
			itsData.add(new DataPoint(thePrimaryColumn.getFloat(i), theSecondaryColumn.getFloat(i)) );
		}

		//After all data is included, update the regression function and its error terms
		update();
	}

	//constructor for non-base RM. It derives from a base-RM
	public RegressionMeasure(RegressionMeasure theBase, BitSet theMembers)
	{
		itsType = theBase.itsType;
		itsBase = theBase;

		//Create an empty measure
		itsSampleSize = 0;
		itsXSum = 0;
		itsYSum = 0;
		itsXYSum = 0;
		itsXSquaredSum = 0;
		itsYSquaredSum = 0;

		itsData = new ArrayList<DataPoint>(theMembers.cardinality());
		itsComplementData =
			new ArrayList<DataPoint>(itsBase.getSampleSize() - theMembers.cardinality()); //create empty one. will be filled after update()

		for (int i=0; i<itsBase.getSampleSize(); i++)
		{
			DataPoint anObservation = itsBase.getObservation(i);
			if (theMembers.get(i))
				addObservation(anObservation);
			else //complement
				itsComplementData.add(anObservation);
		}

		//After all data is included, update the regression function and its error terms
		update();
	}


	//TODO test and verify method
	public double getEvaluationMeasureValue()
	{
		//determine the sums for the complement
		double aComplementXSum = itsBase.getXSum()-itsXSum;
		double aComplementYSum = itsBase.getYSum()-itsYSum;
		double aComplementXSquaredSum = itsBase.getXSquaredSum()-itsXSquaredSum;
		double aComplementXYSum = itsBase.getXYSum()-itsXYSum;
		double aComplementSampleSize = itsBase.getSampleSize()-itsSampleSize;

		//determine variance for the distribution
		double aNumerator = getErrorTermVariance(itsErrorTermSquaredSum, itsSampleSize);
		double aDenominator = itsXSquaredSum - 2*itsXSum*itsXSum/itsSampleSize + itsXSum*itsXSum/itsSampleSize;
		double aVariance = aNumerator / aDenominator;

		//determine variance for the complement distribution
		aNumerator = getErrorTermVariance(itsComplementErrorTermSquaredSum, aComplementSampleSize);
		aDenominator = aComplementXSquaredSum - 2*aComplementXSum*aComplementXSum/aComplementSampleSize + aComplementXSum*aComplementXSum/aComplementSampleSize;
		double aComplementVariance = aNumerator/aDenominator;

		//calculate the difference between slopes of this measure and its complement
		double aSlope = getSlope(itsXSum, itsYSum, itsXSquaredSum, itsXYSum, itsSampleSize);
		double aComplementSlope = getSlope(aComplementXSum, aComplementYSum, aComplementXSquaredSum, aComplementXYSum, aComplementSampleSize);
		double aSlopeDifference = Math.abs(aComplementSlope - aSlope);

		Log.logCommandLine("\n           slope: " + aSlope);
		Log.logCommandLine("complement slope: " + aComplementSlope);
		Log.logCommandLine("           variance : " + aVariance);
		Log.logCommandLine("complement variance: " + aComplementVariance);

		double result = Math.PI; // throw PI; if this number is not overridden in one of the following cases, something went horribly wrong.
		switch(itsType)
		{
			//TODO turn this t-value into a p-value.
			case QualityMeasure.LINEAR_REGRESSION:
			{
				result = -aSlopeDifference / Math.sqrt(aVariance+aComplementVariance);
				break;
			}
			case QualityMeasure.COOKS_DISTANCE:
			{
				result = Math.random();
				break;
			}
		}

		return result;
	}

	/**
	 * Updates the slope and intercept of the regression function.
	 * Function used to determine slope:
	 * b = SUM( (x_n - x_mean)*(y_n - y_mean) ) / SUM( (x_n - x_mean)*(x_n - x_mean) )
	 * this can be rewritten to
	 * b = ( SUM(x_n*y_n) - x_mean*y_sum - y_mean*x_sum + n*x_mean*y_mean ) / ( SUM(x_n*x_n) - 2*x_mean*x_sum + n*x_mean*x_mean )
	 *
	 */
	private void updateRegressionFunction()
	{
		double aXMean = itsXSum / itsSampleSize;
		double aYMean = itsYSum / itsSampleSize;
		itsSlope = getSlope(itsXSum, itsYSum, itsXSquaredSum, itsXYSum, itsSampleSize);
		itsIntercept = aYMean - itsSlope*aXMean;
	}

	private double getSlope(double theXSum, double theYSum, double theXSquaredSum, double theXYSum, double theSampleSize)
	{
		double aXMean = theXSum / theSampleSize;
		double aYMean = theYSum / theSampleSize;
		double aNumerator = theXYSum - aXMean*theYSum - aYMean*theXSum + theSampleSize*aXMean*aYMean;
		double aDenominator = theXSquaredSum - 2*aXMean*theXSum + theXSum*aXMean;
		return aNumerator/aDenominator;
	}

	/**
	 * Add a new datapoint to this measure, where the Y-value is the target variable.
	 * Always call update() after all datapoints have been added.
	 * @param theY the Y-value, the target
	 * @param theX the X-value
	 */
	public void addObservation(float theY, float theX)
	{
		//adjust the sums
		itsSampleSize++;
		itsXSum += theX;
		itsYSum += theY;
		itsXYSum += theX*theY;
		itsXSquaredSum += theX*theX;
		itsYSquaredSum += theY*theY;

		//Add to its own lists
		DataPoint aDataPoint = new DataPoint(theX,theY);
		itsData.add(aDataPoint);
	}

	public void addObservation(DataPoint theObservation)
	{
		float anX = theObservation.getX();
		float aY = theObservation.getY();

		//adjust the sums
		itsSampleSize++;
		itsXSum += anX;
		itsYSum += aY;
		itsXYSum += anX*aY;
		itsXSquaredSum += anX*anX;
		itsYSquaredSum += aY*aY;

		//Add to its own lists
		itsData.add(theObservation);
	}

	public DataPoint getObservation(int theIndex)
	{
		return itsData.get(theIndex);
	}

	public void update()
	{
		updateRegressionFunction();
		updateErrorTerms();
	}

	/**
	 * calculates the error terms for the distribution and recomputes the
	 * sum of the squared error term
	 *
	 */
	private void updateErrorTerms()
	{
		itsErrorTermSquaredSum = 0;
		for(int i=0; i<itsSampleSize; i++)
		{
			double anErrorTerm = getErrorTerm(itsData.get(i));
			itsErrorTermSquaredSum += anErrorTerm*anErrorTerm;
		}

		//update the error terms of the complement of this measure, if present
		if(itsBase!=null)
		{
			itsComplementErrorTermSquaredSum=0;
			for(int i=0; i<(itsBase.getSampleSize()-itsSampleSize); i++)
			{
				if(itsComplementData.size()!=itsBase.getSampleSize()-itsSampleSize)
					System.err.println("incorrect computation of complement!");
				double anErrorTerm = getErrorTerm(itsComplementData.get(i));
				itsComplementErrorTermSquaredSum += anErrorTerm*anErrorTerm;
			}
		}

	}

	/**
	 * Determine the error term for a given point
	 *
	 * @param theX the x-value
	 * @param theY the y-value
	 * @return the error term
	 */
	private double getErrorTerm(double theX, double theY)
	{
		return theY - (itsSlope*theX+itsIntercept);
	}

	private double getErrorTerm(DataPoint theDataPoint)
	{
		return getErrorTerm(theDataPoint.getX(), theDataPoint.getY());
	}

	private double getErrorTermVariance(double theErrorTermSquaredSum, double theSampleSize)
	{
		return theErrorTermSquaredSum / (theSampleSize - 2 );
	}

	public int getSampleSize()
	{
		return itsSampleSize;
	}

	public double getXSum()
	{
		return itsXSum;
	}

	public double getYSum()
	{
		return itsYSum;
	}

	public double getXYSum()
	{
		return itsXYSum;
	}

	public double getXSquaredSum()
	{
		return itsXSquaredSum;
	}

	public double getYSquaredSum()
	{
		return itsYSquaredSum;
	}

	/**
	 * Computes and returns the correlation given the observations contained by CorrelationMeasure
	 * @return the computed correlation
	 */
	public double getCorrelation()
	{
		itsCorrelation = (itsSampleSize*itsXYSum - itsXSum*itsYSum)/Math.sqrt((itsSampleSize*itsXSquaredSum - itsXSum*itsXSum) * (itsSampleSize*itsYSquaredSum - itsYSum*itsYSum));
		//itsCorrelationIsOutdated = false; //set flag to false, so subsequent calls to getCorrelation don't need anymore computation.
		return itsCorrelation;
	}

	public double getSlope()
	{
		return itsSlope;
	}

	public double getIntercept()
	{
		return itsIntercept;
	}

	public double getBaseFunctionValue(double theX)
	{
		return theX*itsSlope + itsIntercept;
	}
}
