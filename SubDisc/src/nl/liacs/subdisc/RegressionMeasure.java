package nl.liacs.subdisc;

import java.util.ArrayList;


//TODO: Make more efficient by not updating the regression function after every addObservation. only when necessary
public class RegressionMeasure
{
	private double itsSampleSize;
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

	private ArrayList<DataPoint> itsData = new ArrayList<DataPoint>();//Stores all the datapoints for this measure
	private ArrayList<DataPoint> itsComplementData = new ArrayList<DataPoint>();//Stores all the datapoints for the complement

	public static int itsType;
	private RegressionMeasure itsBase = null;

	//make a base model from two columns
	// TODO .clone makes a shallow copy
	// itsComplementData = new ArrayList<DataPoint>(theBase.getBase)
	public RegressionMeasure(int theType, Column thePrimaryColumn, Column theSecondaryColumn, RegressionMeasure theBase)
	{
		itsBase = theBase;
		if(itsBase!=null)
			itsComplementData = (ArrayList<DataPoint>) theBase.getData().clone();

		itsType = theType;
		itsSampleSize = thePrimaryColumn.size();
		for(int i=0; i<itsSampleSize; i++)
		{
			itsXSum += thePrimaryColumn.getFloat(i);
			itsYSum += theSecondaryColumn.getFloat(i);
			itsXYSum += thePrimaryColumn.getFloat(i)*theSecondaryColumn.getFloat(i);
			itsXSquaredSum += thePrimaryColumn.getFloat(i)*thePrimaryColumn.getFloat(i);
			itsYSquaredSum += theSecondaryColumn.getFloat(i)*theSecondaryColumn.getFloat(i);

			//for each entry added to this measure, remove it from its complement
			itsData.add(new DataPoint(thePrimaryColumn.getFloat(i), theSecondaryColumn.getFloat(i)) );
			itsComplementData.remove(new DataPoint(thePrimaryColumn.getFloat(i), theSecondaryColumn.getFloat(i)));
		}

		//After all data is included, update the regression function and its error terms
		updateRegressionFunction();
		updateErrorTerms();
	}

	public RegressionMeasure(RegressionMeasure theBase)
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

		//Init the complement measure. For an empty measure the complement is equal to the root
		// TODO .clone makes a shallow copy
		// itsComplementData = new ArrayList<DataPoint>(theBase.getBase
		if(theBase!=null)
			itsComplementData = (ArrayList<DataPoint>) theBase.getData().clone();
		//else
		//	If theBase==null it means this measure itself is the root and thus the complement is empty
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


		double aZValue = aSlopeDifference / (aVariance+aComplementVariance);
//		System.err.println("Z: "+aZValue+ " Slope1: "+aSlope + " Slope2: "+aComplementSlope);

		return aZValue;
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
	 * @param theY the Y-value, the target
	 * @param theX the X-value
	 */
	public void addObservation(double theY, double theX)
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

		//Remove from the complement
		itsComplementData.remove(aDataPoint);

		//update the new regression function
		updateRegressionFunction();

		//update the error terms of this measure
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
		for(int n=0; n<itsSampleSize; n++)
		{
			double anErrorTerm = getErrorTerm(itsData.get(n));
			itsErrorTermSquaredSum += anErrorTerm*anErrorTerm;
		}

		//update the error terms of the complement of this measure, if present
		if(itsBase!=null)
		{
			itsComplementErrorTermSquaredSum=0;
			for(int n=0; n < (itsBase.getSampleSize()-itsSampleSize); n++)
			{
				if(itsComplementData.size()!=itsBase.getSampleSize()-itsSampleSize)
					System.err.println("dD");

				double anErrorTerm = getErrorTerm(itsComplementData.get(n));
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

	public ArrayList<DataPoint> getData()
	{
		return itsData;
	}

	private double getErrorTermVariance(double theErrorTermSquaredSum, double theSampleSize)
	{
		return theErrorTermSquaredSum / (theSampleSize - 2 );
	}

	public double getSampleSize()
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


}
