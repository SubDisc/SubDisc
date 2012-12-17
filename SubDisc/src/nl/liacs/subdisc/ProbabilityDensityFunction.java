package nl.liacs.subdisc;

import java.util.*;

public class ProbabilityDensityFunction
{
	private float[] itsDensity;
	private float itsMin, itsMax, itsBinWidth;
	private int itsNrBins = 1000;

	//create from entire dataset
	public ProbabilityDensityFunction(Column theData)
	{
		//TODO include outlier treatment
		itsMin = theData.getMin();
		itsMax = theData.getMax();
		itsBinWidth = (itsMax-itsMin)/itsNrBins;
		itsDensity = new float[itsNrBins];

		Log.logCommandLine("Min = " + itsMin);
		Log.logCommandLine("Max = " + itsMax);
		Log.logCommandLine("BinWidth = " + itsBinWidth);
		int aSize = theData.size();
		for (int i=0; i<aSize; i++)
		{
			float aValue = theData.getFloat(i);
			add(aValue, 1f/theData.size());
		}
	}

	//create for subgroup, relative to existing PDF
	// TODO subgroup PDF uses same Column as master PDF, enforce this
	public ProbabilityDensityFunction(ProbabilityDensityFunction thePDF, Column theData, BitSet theMembers)
	{
		itsMin = thePDF.itsMin;
		itsMax = thePDF.itsMax;
		itsBinWidth = thePDF.itsBinWidth;
		itsDensity = new float[itsNrBins];

		float anIncrement = 1f/theMembers.cardinality();
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
		{
			float aValue = theData.getFloat(i);
			add(aValue, anIncrement);
		}
	}

	public float getDensity(float theValue)
	{
		return getDensity(getIndex(theValue));
	}

	public float getDensity(int theIndex)
	{
		return itsDensity[theIndex];
	}

	private int getIndex(float aValue)
	{
		if (aValue == itsMax)
			return itsNrBins-1;
		else
			return (int) ((aValue-itsMin)/itsBinWidth);
	}

	private void add(float theValue, float theIncrement)
	{
		int aBin = getIndex(theValue);
		itsDensity[aBin] += theIncrement;
	}

	public void print()
	{
		Log.logCommandLine("ProbabilityDensityFunction:\n");
		for (int i = 0; i < itsDensity.length; i++)
			Log.logCommandLine("  " + i + "	" + itsDensity[i]);
		Log.logCommandLine("");
	}

	public int size()
	{
		return itsNrBins;
	}

	private final double CUTOFF = 3.0;	// for now
	public double[] getGaussianDistribution(double theSigma)
	{
		if (theSigma <= 0.0 || Double.isInfinite(theSigma) || Double.isNaN(theSigma))
			throw new IllegalArgumentException("Invalid sigma: " + theSigma);

		// mu = 0.0
		int aWidth = (int)(2.0 * CUTOFF * theSigma) + 1;
		double[] aKernel = new double[aWidth];
		double aCorrection = 0.0;	// to set AUC to 1.0

		int halfWidth = aWidth / 2;
		double variance = theSigma * theSigma;
		double doubleVariance = 2.0 * variance;
		double factor = 1.0 / Math.sqrt(Math.PI * doubleVariance);

		// NOTE this is Arno's simplified code for TimeSeries.Gaussian
		// it does no guarantee symmetry like the old code did
		// as is computes each x value twice for [-x , midpoint, +x]
		for (int i = 0, j = aWidth; i < j; ++i)
		{
			double anX = i-halfWidth;
			double anXSquared = -anX*anX;
			double aValue = factor * Math.exp(anXSquared / doubleVariance);
			aKernel[i] = aValue;
			aCorrection += aValue;
		}

		// correct all values such that they sum to 1.0
		// NOTE rounding errors may still prevent this
		for (int i = 0, j = aWidth; i < j; ++i)
			aKernel[i] /= aCorrection;

		return aKernel;
	}

	// can not be applied if width > theInput
	public float[] smooth(float theSigma)
	{
		final int length = itsNrBins;

		double aSigma = (theSigma-itsMin)/itsNrBins;
		final double[] aKernel = getGaussianDistribution(aSigma);

		// initialised to 0.0
		final float[] anOutput = new float[length];

		// values where no full window/kernel can be applied
		final int aWidth = aKernel.length;
		final int halfWidth = aWidth / 2;
		// ignore for now
		// FIXME put some corrected value here
		for (int i = 0, j = halfWidth, k = length; i < j; ++i)
			anOutput[i] = anOutput[--k] = Float.NaN;

		// work in progress
//		for (int i = 0, j = halfWidth; i < j; ++i)
//			for (int k = aWidth-1-i, m = aWidth; k < m; ++k)
//				anOutput[i] += (aKernel[k] * itsDensity[i]);
//		for (int i = length-halfWidth, j = length; i < j; ++i)
//			for (int k = 0, m = something; k < m; ++k)
//				anOutput[i] += (aKernel[k] * itsDensity[i]);
		// end work in progress

		// apply kernel on theInput
		for (int i = halfWidth, j = length - halfWidth; i < j; ++i)
			for (int k = 0, m = aWidth, n = i - halfWidth; k < m; ++k, ++n)
				anOutput[i] += (aKernel[k] * itsDensity[n]);

		itsDensity = anOutput;
		return anOutput;
	}
}
