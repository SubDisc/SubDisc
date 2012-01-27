package nl.liacs.subdisc;

import java.text.*;
import java.util.*;
import nl.liacs.subdisc.Jama.*;

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

//	private ArrayList<DataPoint> itsData;//Stores all the datapoints for this measure
//	private ArrayList<DataPoint> itsComplementData = new ArrayList<DataPoint>();//Stores all the datapoints for the complement

	public static int itsType;
	private RegressionMeasure itsBase = null;
	
	private Matrix itsXMatrix;
	private Matrix itsYMatrix;
	private Matrix itsXTXInverseMatrix;
	private Matrix itsZMatrix;
	private Matrix itsBetaHat;
	private Matrix itsHatMatrix;
	private Matrix itsResidualMatrix;
	
	private double itsP;
	private double itsQ;
	private double itsSSquared;
	private double[] itsRSquared;
	private double[] itsT;
	private double[] itsSVP;
	
	private int itsBoundSevenCount;
	private int itsBoundSixCount;
	private int itsBoundFiveCount;
	private int itsBoundFourCount;
	private int itsRankDefCount;
	
	private int itsI; // = itsNrSecondaryTargets
	private int itsJ; // = itsNrTertiaryTargets
						// maar dat ga ik niet iedere keer uittypen.
	
	private boolean itsInterceptRelevance;
	
	private String itsPrimaryName;
	private List<String> itsSecondaryNames;
	private List<String> itsTertiaryNames;
	
	private String itsGlobalModel;
	private static final DecimalFormat aDf = new DecimalFormat("#.#####");

	//make a base model from multiple columns
	public RegressionMeasure(int theType, TargetConcept theTargetConcept)
	{//TODO: Either remove legacy code, or make something decent out of it. For now, it is hacked.
		//get target data
		Column aPrimaryTarget = theTargetConcept.getPrimaryTarget(); 
		List<Column> aSecondaryTargets = theTargetConcept.getSecondaryTargets();
		List<Column> aTertiaryTargets = theTargetConcept.getTertiaryTargets();

		itsI = aSecondaryTargets.size();
		itsJ = aTertiaryTargets.size();
		itsP = 1+itsI+itsJ;
		itsQ = itsI;
		itsInterceptRelevance = theTargetConcept.getInterceptRelevance(); 
		if (itsInterceptRelevance)
			itsQ++;
		
		itsPrimaryName = aPrimaryTarget.getName();
		itsSecondaryNames = new ArrayList<String>(itsI);
		itsTertiaryNames = new ArrayList<String>(itsJ);
		for (Column c : aSecondaryTargets)
			itsSecondaryNames.add(c.getName());
		for (Column c : aTertiaryTargets)
			itsTertiaryNames.add(c.getName());

		itsType = theType;
		itsSampleSize = aPrimaryTarget.size();
/*		itsData = new ArrayList<DataPoint>(itsSampleSize);
		for(int i=0; i<itsSampleSize; i++)
		{
/*			itsXSum += thePrimaryColumn.getFloat(i);
			itsYSum += theSecondaryColumn.getFloat(i);
			itsXYSum += thePrimaryColumn.getFloat(i)*theSecondaryColumn.getFloat(i);
			itsXSquaredSum += thePrimaryColumn.getFloat(i)*thePrimaryColumn.getFloat(i);
			itsYSquaredSum += theSecondaryColumn.getFloat(i)*theSecondaryColumn.getFloat(i);

			itsData.add(new DataPoint(thePrimaryColumn.getFloat(i), theSecondaryColumn.getFloat(i)) );
		}*/
			
		switch(itsType)
		{
			case QualityMeasure.LINEAR_REGRESSION:
			{/* TODO: fix or remove
				itsBase = null; //this *is* the base
				itsComplementData = null; //will remain empty for the base RM
				updateRegressionFunction();
				updateErrorTerms();*/
			}
			case QualityMeasure.COOKS_DISTANCE:
			{
				//updateRegressionFunction(); //updating error terms unnecessary since Cook's distance does not care
				//"I see no reason for this to continue"
				//  -- Electric Six, `Lenny Kravitz'
				
				/*fill arrays which will contain the data. Schematically it looks like this (where x denotes a secondary and x' a tertiary target):
				 * aData =
				 *   x_1^1 ... x_1^i  x'_1^1 ... x'_1^j  y_1 
				 *     .   .     .       .   .      .     .
				 *     .    .    .       .    .     .     .
				 *     .     .   .       .     .    .     .   
				 *   x_n^1 ... x_n^i  x'_n^1 ... x'_n^j  y_n
				 * 
				 * anXValues =
				 *   1  x_1^1 ... x_1^i  x'_1^1 ... x'_1^j 
				 *   .    .   .     .       .   .      .
				 *   .    .    .    .       .    .     .
				 *   .    .     .   .       .     .    .   
				 *   1  x_n^1 ... x_n^i  x'_n^1 ... x'_n^j
				 *
				 * aYValues =
				 *   y_1 
				 *    .
				 *    .
				 *    .   
				 *   y_n
				 *   
				 * the indices in the for-loops will correspond to the indices used here.  
				 */

				double[][] anXValues = new double[itsSampleSize][(int) itsP];
				double[][] aYValues = new double[itsSampleSize][1];
				double[][] aZValues = new double[(int) itsQ][(int) itsP];
				
				for (int n=0; n<itsSampleSize; n++)
				{
					anXValues[n][0]=1;
					for (int i=0; i<itsI; i++)
					{	
						Column aSecondaryColumn = aSecondaryTargets.get(i);
						anXValues[n][1+i] = aSecondaryColumn.getFloat(n);
//						aData[n][i] = anXValues[n][1+i];
					}
					for (int j=0; j<itsJ; j++)
					{	
						Column aTertiaryColumn = aTertiaryTargets.get(j);
						anXValues[n][1+itsI+j] = aTertiaryColumn.getFloat(n);
//						aData[n][itsI+j] = anXValues[n][1+itsI+j];
					}
					aYValues[n][0] = aPrimaryTarget.getFloat(n); 
//					aData[n][itsI+itsJ] = aYValues[n][0];
				}
//				itsDataMatrix = new Matrix(aData);

				// build Z-matrix values; indicating which subset of beta we're interested in
				int anOffset = 1;
				if (itsInterceptRelevance)
					anOffset--;
				for (int q=0; q<itsQ; q++)
					for (int p=0; p<itsP; p++)
						if (p==q+anOffset)
							aZValues[q][p] = 1;
						else
							aZValues[q][p] = 0;

				itsXMatrix = new Matrix(anXValues);
				itsYMatrix = new Matrix(aYValues);
				itsZMatrix = new Matrix(aZValues);
				
				//do the regression math
				//TODO: refuse to do anything in the unlikely case that the data matrix is row deficient.
				//      If this unlikely event is not caught, the program will crash.
				computeRegression();
				
				theTargetConcept.setGlobalRegressionModel(spellFittedModel(itsBetaHat));
				
				//fill R^2 array
				double[] aResiduals = new double[itsSampleSize];
				for (int i=0; i<itsSampleSize; i++)
					aResiduals[i] = itsResidualMatrix.get(i,0)*itsResidualMatrix.get(i,0);
				Arrays.sort(aResiduals);
				//now we have squared residuals in ascending order, but we want array[i] = sum(array[i]...array[n])
				for (int i=itsSampleSize-2; i>=0; i--)
					aResiduals[i] += aResiduals[i+1];
				itsRSquared = aResiduals;
				
				//fill T array
				double[] aT = new double[itsSampleSize];
				for (int i=0; i<itsSampleSize; i++)
					aT[i] = itsHatMatrix.get(i,i);
				Arrays.sort(aT);
				for (int i=itsSampleSize-2; i>=0; i--)
					aT[i] += aT[i+1];
				itsT = aT;
				
				//fill SVP array; this would be Cook's distance when removing only single points. Cf. Cook&Weisberg p.117, Cook1977a
				double[] aSVP = new double[itsSampleSize];
				for (int i=0; i<itsSampleSize; i++)
					aSVP[i] = itsResidualMatrix.get(i,0)*itsResidualMatrix.get(i,0)/itsP * itsHatMatrix.get(i,i)/(1-itsHatMatrix.get(i,i));
				itsSVP = aSVP;
				
				//initialize bounds
				itsBoundSevenCount=0;
				itsBoundSixCount=0;
				itsBoundFiveCount=0;
				itsBoundFourCount=0;
				itsRankDefCount=0;
			}
		}
	}
	
	public String spellFittedModel(Matrix theBetaHat)
	{
//		String aRegressionModel = "  fitted model: "+ itsPrimaryName + " = " + aDf.format(theBetaHat.get(0,0));
		String aRegressionModel = "  fitted model: "+ itsPrimaryName + " = " + theBetaHat.get(0,0);
		for (int i=0; i<itsI; i++)
//			aRegressionModel = aRegressionModel + " + " + aDf.format(theBetaHat.get(i+1,0)) + " * " + itsSecondaryNames.get(i);
			aRegressionModel = aRegressionModel + " + " + theBetaHat.get(i+1,0) + " * " + itsSecondaryNames.get(i);
		for (int j=0; j<itsJ; j++)
//			aRegressionModel = aRegressionModel + " + " + aDf.format(theBetaHat.get(j+itsI+1,0)) + " * " + itsTertiaryNames.get(j);
			aRegressionModel = aRegressionModel + " + " + theBetaHat.get(j+itsI+1,0) + " * " + itsTertiaryNames.get(j);
		Log.logCommandLine(aRegressionModel);
		return aRegressionModel;
	}
	
	public void computeRegression()
	{
		itsXTXInverseMatrix = (itsXMatrix.transpose().times(itsXMatrix)).inverse();
		itsBetaHat = itsXTXInverseMatrix.times(itsXMatrix.transpose()).times(itsYMatrix);
		itsHatMatrix = itsXMatrix.times(itsXTXInverseMatrix).times(itsXMatrix.transpose());
		itsResidualMatrix = (Matrix.identity(itsSampleSize,itsSampleSize).minus(itsHatMatrix)).times(itsYMatrix);
		
		itsSSquared = (itsResidualMatrix.transpose().times(itsResidualMatrix)).get(0,0)/((double) itsSampleSize-itsP);
	}

	//constructor for non-base RM. It derives from a base-RM
/*	public RegressionMeasure(RegressionMeasure theBase, BitSet theMembers)
	{
		itsType = theBase.itsType;
		itsBase = theBase;

		//Create an empty measure
		itsSampleSize = theMembers.cardinality();

		itsXMatrix;
		private Matrix itsYMatrix;

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
	}*/

	//TODO test and verify method
/*	public double getEvaluationMeasureValue()
	{
		updateRegressionFunction();
		updateErrorTerms();
		return getSSD();
	}
	
	//TODO turn this t-value into a p-value.
	public double getSSD()
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
		
		//if we divided by zero along the way, we are considering a degenerate candidate subgroup, hence quality=0 
		if (itsSampleSize==0 || itsSampleSize==2 || aDenominator==0)
			return 0;
		
		//determine variance for the complement distribution
		aNumerator = getErrorTermVariance(itsComplementErrorTermSquaredSum, aComplementSampleSize);
		aDenominator = aComplementXSquaredSum - 2*aComplementXSum*aComplementXSum/aComplementSampleSize + aComplementXSum*aComplementXSum/aComplementSampleSize;
		double aComplementVariance = aNumerator/aDenominator;

		//if we divided by zero along the way, we are considering a degenerate candidate subgroup complement, hence quality=0 
		if (aComplementSampleSize==0 || aComplementSampleSize==2 || aDenominator==0)
			return 0;
		
		//calculate the difference between slopes of this measure and its complement
		double aSlope = getSlope(itsXSum, itsYSum, itsXSquaredSum, itsXYSum, itsSampleSize);
		double aComplementSlope = getSlope(aComplementXSum, aComplementYSum, aComplementXSquaredSum, aComplementXYSum, aComplementSampleSize);
		double aSlopeDifference = Math.abs(aComplementSlope - aSlope);

		Log.logCommandLine("\n           slope: " + aSlope);
		Log.logCommandLine("complement slope: " + aComplementSlope);
		Log.logCommandLine("           variance: " + aVariance);
		Log.logCommandLine("complement variance: " + aComplementVariance);

		if (aVariance+aComplementVariance==0)
			return 0;
		else {return aSlopeDifference / Math.sqrt(aVariance+aComplementVariance);}
	}
*/	
	public double calculate(Subgroup theNewSubgroup)
	{
		Log.logCommandLine("");
		BitSet aMembers = theNewSubgroup.getMembers();
		int aSampleSize = aMembers.cardinality();
		
		//filter out rank deficient model that crash matrix multiplication library
		if (aSampleSize<2)
		{
			itsRankDefCount++;
			return -Double.MAX_VALUE;
		}
		
		//calculate the upper bound values. Before each bound, only the necessary computations are done.
		double aT = itsT[aSampleSize];
		double aRSquared = itsRSquared[aSampleSize];
		
		double aBoundSeven = computeBoundSeven(aSampleSize, aT, aRSquared);
		if (aBoundSeven>-Double.MAX_VALUE)
		{
			Log.logCommandLine("                   Bound 7: " + aBoundSeven);
			itsBoundSevenCount++;
		}
		
		int[] anIndices = new int[aSampleSize];
		int[] aRemovedIndices = new int[itsSampleSize-aSampleSize];
		int anIndex=0;
		int aRemovedIndex=0;
		for (int i=0; i<itsSampleSize; i++)
		{
			if (aMembers.get(i))
			{
				anIndices[anIndex] = i;
				anIndex++;
			}
			else
			{
				aRemovedIndices[aRemovedIndex] = i;
				aRemovedIndex++;
			}
		}

		Matrix aRemovedResiduals = itsResidualMatrix.getMatrix(aRemovedIndices,0,0);
		double aSquaredResidualSum = squareSum(aRemovedResiduals);
		double aBoundSix = computeBoundSix(aSampleSize, aT, aSquaredResidualSum);		
		if (aBoundSix>-Double.MAX_VALUE)
		{
			Log.logCommandLine("                   Bound 6: " + aBoundSix);
			itsBoundSixCount++;
		}
		
		Matrix aRemovedHatMatrix = itsHatMatrix.getMatrix(aRemovedIndices,aRemovedIndices);
		double aRemovedTrace = aRemovedHatMatrix.trace();
		double aBoundFive = computeBoundFive(aSampleSize, aRemovedTrace, aRSquared);
		if (aBoundFive>-Double.MAX_VALUE)
		{
			Log.logCommandLine("                   Bound 5: " + aBoundFive);
			itsBoundFiveCount++;
		}
		
		double aBoundFour = computeBoundFour(aSampleSize, aRemovedTrace, aSquaredResidualSum);
		if (aBoundFour>-Double.MAX_VALUE)
		{
			Log.logCommandLine("                   Bound 4: " + aBoundFour);
			itsBoundFourCount++;
		}
		
		//compute estimate based on projection of single influence values 
//		double anSVPDistance = computeSVPDistance(itsSampleSize-aSampleSize, aRemovedIndices);
//		Log.logCommandLine("                   SVP est: " + anSVPDistance);

		//make submatrices
		Matrix anXMatrix = itsXMatrix.getMatrix(anIndices,0,itsI+itsJ);
		Matrix aYMatrix = itsYMatrix.getMatrix(anIndices,0,0);
		
		//filter out rank-deficient cases; these regressions cannot be computed, hence low quality
		LUDecomposition itsDecomp = new LUDecomposition(anXMatrix);
		if (!itsDecomp.isNonsingular())
		{
			itsRankDefCount++;
			return -Double.MAX_VALUE;
		}
		
		//compute regression
		Matrix anXTXMatrix = anXMatrix.transpose().times(anXMatrix); 
		LUDecomposition itsXTXDecomp = new LUDecomposition(anXTXMatrix);
		if (!itsXTXDecomp.isNonsingular())
		{
			itsRankDefCount++;
			return -Double.MAX_VALUE;
		}
		Matrix anXTXInverseMatrix = anXTXMatrix.inverse();
		
		Matrix aBetaHat = anXTXInverseMatrix.times(anXMatrix.transpose()).times(aYMatrix);
		Matrix aHatMatrix = anXMatrix.times(anXTXInverseMatrix).times(anXMatrix.transpose());
		Matrix aResidualMatrix = (Matrix.identity(aSampleSize,aSampleSize).minus(aHatMatrix)).times(aYMatrix);
		
		theNewSubgroup.setRegressionModel(spellFittedModel(aBetaHat));
		
		//compute Cook's distance
		double aP = aBetaHat.getRowDimension();
		double anSSquared = (aResidualMatrix.transpose().times(aResidualMatrix)).get(0,0)/((double) itsSampleSize-aP);
		
		//double aQuality = aBetaHat.minus(itsBetaHat).transpose().times(anXMatrix.transpose()).times(anXMatrix).times(aBetaHat.minus(itsBetaHat)).get(0,0)/(aP*anSSquared);
		Matrix aZXTXInverseZTInverseMatrix = itsZMatrix.times(anXTXInverseMatrix.times(itsZMatrix.transpose())); 
		LUDecomposition itsOtherDecomp = new LUDecomposition(aZXTXInverseZTInverseMatrix);
		if (!itsOtherDecomp.isNonsingular())
		{
			itsRankDefCount++;
			return -Double.MAX_VALUE;
		}
		
		double aQuality = aBetaHat.minus(itsBetaHat).transpose().times(
			itsZMatrix.transpose()
			).times(aZXTXInverseZTInverseMatrix.inverse()
			).times(itsZMatrix).times(aBetaHat.minus(itsBetaHat)).get(0,0)/(itsQ*anSSquared);
		//N.B.: Temporary line for fetching Cook's experimental statistics
//		Log.logRefinement(""+aQuality+","+anSVPDistance+","+aSampleSize);
		return aQuality;
	}
	
	private double computeBoundSeven(int theSampleSize, double theT, double theRSquared)
	{
		if (theT>=1)
			return -Double.MAX_VALUE;
		return itsP/itsQ*theT/((1-theT)*(1-theT))*theRSquared/(itsP*itsSSquared);
	}

	private double computeBoundSix(int theSampleSize, double theT, double theSquaredResidualSum)
	{
		if (theT>=1)
			return -Double.MAX_VALUE;
		return itsP/itsQ*theT/((1-theT)*(1-theT))*theSquaredResidualSum/(itsP*itsSSquared);
	}

	private double computeBoundFive(int theSampleSize, double theRemovedTrace, double theRSquared)
	{
		if (theRemovedTrace>=1)
			return -Double.MAX_VALUE;
		return itsP/itsQ*theRemovedTrace/((1-theRemovedTrace)*(1-theRemovedTrace))*theRSquared/(itsP*itsSSquared);
	}
	
	private double computeBoundFour(int theSampleSize, double theRemovedTrace, double theSquaredResidualSum)
	{
		if (theRemovedTrace>=1)
			return -Double.MAX_VALUE;
		return itsP/itsQ*theRemovedTrace/((1-theRemovedTrace)*(1-theRemovedTrace))*theSquaredResidualSum/(itsP*itsSSquared);
	}
	
	private double computeSVPDistance(int theNrRemoved, int[] theIndices)
	{
		double result = 0.0;
		for (int i=0; i<theNrRemoved; i++)
			result += itsSVP[theIndices[i]];
		return result;
	}
	
	private double squareSum(Matrix itsMatrix)
	{
		int aSampleSize = itsMatrix.getRowDimension();
		double[] itsValues = itsMatrix.getRowPackedCopy();
		double aSum = 0.0;
		for (int i=0; i<aSampleSize; i++)
			aSum += itsValues[i]*itsValues[i];
		return aSum;
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
/*	public void addObservation(float theY, float theX)
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
*/
 	/**
	 * calculates the error terms for the distribution and recomputes the
	 * sum of the squared error term
	 *
	 */
/*	private void updateErrorTerms()
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

	}*/

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
	
	public int getNrBoundSeven() { return itsBoundSevenCount; }
	public int getNrBoundSix() { return itsBoundSixCount; }
	public int getNrBoundFive() { return itsBoundFiveCount; }
	public int getNrBoundFour() { return itsBoundFourCount; }
	public int getNrRankDef() { return itsRankDefCount; }
	
	public String getGlobalModel() { return itsGlobalModel; }
}
