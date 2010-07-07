package nl.liacs.subdisc;

import java.util.Arrays;
import java.util.BitSet;

//implements an n-dimensional cube of counts.

public class CrossCube extends DataCube
{
	protected int itsCounts[];
	protected int itsTotalCount;
	protected int itsDimensions;

	public CrossCube(int theDimensions)
	{
		int aPower = (int)Math.pow(2, theDimensions);
		setSize(aPower);
		itsDimensions = theDimensions;
		itsCounts = new int[aPower];
		Arrays.fill(itsCounts, 0);
	}

	public void setCount(BitSet theBitSet, int theCount) { setCount(getIndex(theBitSet), theCount); }

	public void setCount(int theIndex, int theCount)
	{
		if (theIndex >= itsSize)
			Log.error("out of bounds");
		else
		{
			int aCount = itsCounts[theIndex]; //save old count in case not 0;
			itsCounts[theIndex] = theCount;
			itsTotalCount += theCount - aCount;
		}
	}

	public int getCount(int theIndex) { return itsCounts[theIndex]; }
	public int getCount(BitSet theBitSet) { return getCount(getIndex(theBitSet)); }

	public void incrementCount(int theIndex)
	{
		if (theIndex >= itsSize)
			Log.error("out of bounds");
		else
		{
			itsCounts[theIndex]++;
			itsTotalCount++;
		}
	}

	public void incrementCount(BitSet theBitSet) { incrementCount(getIndex(theBitSet));}

	public double getEntropy()
	{
		double anEntropy = 0;

		if (itsTotalCount == 0)
			return 0;

		for (int i=0; i<itsSize; i++)
		{
			double aFraction = itsCounts[i]/(double)itsTotalCount;
			if (aFraction > 0)
				anEntropy += -aFraction * Math.log(aFraction) / Math.log(2);
		}
		return anEntropy;
	}

	//computes coverage of itemsets containing only 1 item
	public double getExclusiveCoverage()
	{
		double aCoverage = 0;

		if (itsTotalCount == 0)
			return 0;

		for (int i=0; i<itsDimensions; i++)
			aCoverage += itsCounts[(int)Math.pow(2, i)];

		return aCoverage;
	}

	//computes overlap of all itemsets
	public int getOverlap()
	{
		return itsCounts[itsCounts.length-1]; //last cell represents all 1's
	}

	// Riggelsen 2006
	// page 27, 3.15
	//Bayesian Dirichlet Equivalent Uniform
	public double getBDeu()
	{
		double aQuality = 0;

		if (itsTotalCount == 0)
			return 0;

		int q_i = getSize() / 2;
		double alpha_ijk = 1 / (double) getSize();
		double alpha_ij  = 1 / (double) q_i;
		double LogGam_alpha_ijk = LogGamma(alpha_ijk); //uniform prior BDeu metric
		double LogGam_alpha_ij = LogGamma(alpha_ij);

		for (int j=0; j<q_i; j++)
		{
			double aSum = 0;
			double aPost = 0;

			//child = 0;
			aPost += LogGamma(alpha_ijk + getCount(j*2)) - LogGam_alpha_ijk;
			aSum += getCount(j*2);
			//child = 1;
			aPost += LogGamma(alpha_ijk + getCount(j*2 + 1)) - LogGam_alpha_ijk;
			aSum += getCount(j*2 + 1);

			aQuality += LogGam_alpha_ij - LogGamma(alpha_ij + aSum) + aPost;
		}
		return aQuality;
	}

	// TODO duplicate code, same as BinaryTable.LogGamma
	public double LogGamma(double x)
	{
		double stp = 2.50662827465;
		double c1 = 76.18009173;
		double c2 = -86.50532033;
		double c3 = 24.01409822;
		double c4 = -1.231739516;
		double c5 = 1.20858003e-3;
		double c6 = -5.36382e-6;

		double ser = 1.0 + c1 / x + c2 / (x + 1.0) + c3 / (x + 2.0) + c4 / (x + 3.0) + c5 / (x + 4.0) + c6 / (x + 5.0);
		return (x - 0.5) * Math.log(x + 4.5) - x - 4.5 + Math.log(stp * ser);
	}

	public int getIndex(BitSet theBitSet)
	{
		int anIndex = 0;

		for (int i=0; i<itsDimensions; i++)
			if (theBitSet.get(i))
				anIndex += (int)Math.pow(2, itsDimensions-i-1);

		return anIndex;
	}

	public BitSet getBitSet(int theIndex)
	{
		BitSet aBitSet = new BitSet(itsDimensions);
		int anIndex = theIndex;

		for (int i=itsDimensions - 1; i >= 0; i--)
			if (anIndex >= (int) Math.pow(2, i))
			{
				aBitSet.set(itsDimensions - i - 1);
				anIndex -= (int) Math.pow(2, i);
			}

		return aBitSet;
	}

	public int getDimensions() { return itsDimensions; }
	public int getTotalCount() { return itsTotalCount; }
}
