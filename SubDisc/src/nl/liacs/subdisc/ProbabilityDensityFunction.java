package nl.liacs.subdisc;

import java.util.*;

public class ProbabilityDensityFunction
{
	private float[] itsDensity;
	private float itsMin, itsMax, itsBinWidth;
	private int itsNrBins = 30;

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
		float anIncrement = 1f/aSize;
		for (int i=0; i<aSize; i++)
		{
			float aValue = theData.getFloat(i);
			add(aValue, anIncrement);
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

	/*
	 * accumulates rounding errors, alternative would be to just count the
	 * absolute number of items in a bin and report the density for a
	 * particular bin as: (bin_nr_items / total_nr_items)
	 */
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

	private static final double CUTOFF = 3.0;	// for now
	public static double[] getGaussianDistribution(double theSigma)
	{
		if (theSigma <= 0.0 || Double.isInfinite(theSigma) || Double.isNaN(theSigma))
			throw new IllegalArgumentException("Invalid sigma: " + theSigma);

		// mu = 0.0
		int aWidth = (int)(2.0 * CUTOFF * theSigma);
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

		System.out.println(Arrays.toString(aKernel));
		double sum = 0.0;
		for (double d : aKernel)
			sum += d;
		System.out.println("sum=" + sum);

		return aKernel;
	}

	// can not be applied if width > theInput
	public float[] smooth(float theSigma)
	{
		final int length = itsNrBins;

		double aSigma = theSigma/itsBinWidth;
		final double[] aKernel = getGaussianDistribution(aSigma);

		// initialised to 0.0
		final float[] anOutput = new float[length];

		// values where no full window/kernel can be applied
		final int aWidth = aKernel.length;
		final int halfWidth = aWidth / 2;
		System.out.println("HALFWIDTH=" + halfWidth);
/*
		// work in progress
		for (int i = 0, j = halfWidth; i < j; ++i)
		{
			Log.logCommandLine("left: " + i);
			for (int k = halfWidth+i, m = aWidth, n = 0; k < m; ++k, ++n)
				anOutput[i] += (aKernel[k] * itsDensity[n]);
		}
		// TODO adjust this portion to sum(used_kernel_values)
		for (int i = length-1, j = 0; j < halfWidth; --i, ++j)
		{
			Log.logCommandLine("right: " + i);
			for (int k = halfWidth+j, n = length-1; k < aWidth; ++k, --n)
			{
				anOutput[i] += aKernel[k] * itsDensity[n];
			}
		}
		// TODO adjust this portion to sum(used_kernel_values)
		// end work in progress
*/
		// work in progress
		for (int i = 0, j = halfWidth; i < j; ++i)
			for (int k = halfWidth+i, n = 0; k >= 0; --k, ++n)
				anOutput[i] += (aKernel[k] * itsDensity[n]);
		// TODO adjust this portion to sum(used_kernel_values)
		for (int i = length-1, j = 0; j < halfWidth; --i, ++j)
			for (int k = halfWidth+j, n = length-1; k >= 0; --k, --n)
				anOutput[i] += aKernel[k] * itsDensity[n];
		// TODO adjust this portion to sum(used_kernel_values)
		// end work in progress

		// apply kernel on theInput
		for (int i = halfWidth, j = length - halfWidth; i < j; ++i)
		{
			//System.out.println ("i=" + i + " " + itsDensity[i]);
			for (int k = 0, m = aWidth, n = i-halfWidth; k < m; ++k, ++n)
			{
				//System.out.print("i=" + i + " " + itsDensity[i]);
				anOutput[i] += (aKernel[k] * itsDensity[n]);
			}
		}

		itsDensity = anOutput;
		return anOutput;
	}

	public static void main(String[] args)
	{
		int nrRows = 100;
		Column c = new Column("TEST", "TEST", AttributeType.NUMERIC, 0, nrRows);
		c.add(0.0f);
		for (int i = 0; i < 49; ++i)
			c.add(30.0f);
		for (int i = 0; i < 49; ++i)
			c.add(70.0f);
		c.add(100.0f);
		c.print();
		System.out.println();

		ProbabilityDensityFunction pdf;
		for (int i = 1; i <= 5; i+=2)
		{

			pdf = new ProbabilityDensityFunction(c);
			System.out.println(Arrays.toString((pdf.itsDensity)));
			pdf.smooth(i);
			System.out.println(Arrays.toString((pdf.itsDensity)));
			System.out.println();
		}
	}
}
