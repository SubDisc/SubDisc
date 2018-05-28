package nl.liacs.subdisc;

import java.util.*;

// XXX MM extends ProbabilityDensityFunction only to make temporary code simple
public class ProbabilityMassFunction_ND extends ProbabilityDensityFunction
{
	private final float[] itsData;
	private final float itsMin;
	private final float itsMax;
	// itsBounds.length = theNrSplitPoints+1, itsBounds[length-1] = itsMax
	// such that itsMax forms last right upper bound for ( <= bins)
	private final float[] itsBounds;
	// itsBinIndexes stores the index of (<= bin) for each itsData[i]
	private final int[] itsBinIndexes;

	// does not make any real sense for base PMF, will contain qm for child
	final double itsScore;

	ProbabilityMassFunction_ND(Column theColumn, int theNrSplitPoints, boolean useEqualWidth)
	{
		super(theColumn);

		itsData = theColumn.getFloats();
		itsMin = theColumn.getMin();
		itsMax = theColumn.getMax();

		if (useEqualWidth)
			itsBounds = ew(theNrSplitPoints, itsMin, itsMax);
		else
			itsBounds = eh(theNrSplitPoints, theColumn);

		itsBinIndexes = bin(itsBounds, itsData);
//		itsSplitPointsCounts = foo(itsSplitPoints, theColumn);
		itsScore = Double.NaN;

		histo(itsBounds, itsBinIndexes);
	}

	// HACK
	ProbabilityMassFunction_ND(ProbabilityMassFunction_ND theParent, BitSet theMembers)
	{
		super(new Column("FAKE", "", AttributeType.NUMERIC, 0, 0));

		itsData = null;
		itsMin = Float.NaN;
		itsMax = Float.NaN;
		itsBounds = null;
		itsBinIndexes = null;
		itsScore = qm(theMembers, theParent.itsBounds, theParent.itsBinIndexes);
	}

	private static final float[] ew(int theNrSplitPoints, float theMin, float theMax)
	{
		float[] aBounds = new float[theNrSplitPoints+1];
		float aWidth = (theMax-theMin) / (theNrSplitPoints+1);

		for (int i = 0; i < theNrSplitPoints; ++i)
			aBounds[i] = theMin + ((i+1) * aWidth);
		aBounds[aBounds.length-1] = theMax;

		return aBounds;
	}

	private static final float[] eh(int theNrSplitPoints, Column theColumn)
	{
		int aSize = theColumn.size();
		BitSet aBitSet = new BitSet(aSize);
		aBitSet.set(0, aSize);

		// to use new getUniqueSplitPoints() - set to true
		// else old getSplitPoints() is used (note that it is flawed)
		Column.USE_NEW_BINNING = true;
		float[] bounds = theColumn.getUniqueSplitPoints(aBitSet, theNrSplitPoints, Operator.LESS_THAN_OR_EQUAL);
		Column.USE_NEW_BINNING = false;

		// nrBounds <= theNrSplitPoints
		int nrBounds = bounds.length;

		// TODO MM - deal with this
		if (nrBounds == 0)
			throw new AssertionError("FIX eh()");

		// aBitSet selects all data, so theColumn.getMax() is correct
		// add max to bounds only if it is not in there
		float max = theColumn.getMax();
		if (bounds[nrBounds-1] != max)
		{
			// reuse bounds 
			bounds = Arrays.copyOf(bounds, nrBounds + 1);
			bounds[nrBounds] = max; 
		}

		return bounds;
	}

	private static final int[] bin(float[] theSplitPoints, float[] theData)
	{
		int[] aBinIndexes = new int[theData.length];

		for (int i = 0, j = theData.length; i < j; ++i)
		{
			int idx = Arrays.binarySearch(theSplitPoints, theData[i]);
			aBinIndexes[i] = (idx < 0 ? ~idx : idx);
		}

		return aBinIndexes;
	}

	private static final int[] foo(float[] theSplitPoints, Column theColumn)
	{
		int[] aCounts = new int[theSplitPoints.length+1];

		for (int i = 0, j = theColumn.size(); i < j; ++i)
		{
			float aValue = theColumn.getFloat(i);
			int anIndex = Arrays.binarySearch(theSplitPoints, aValue);
			if (anIndex < 0)
				anIndex = ~anIndex;
			++aCounts[anIndex];
		}

		return aCounts;
	}

	// not the fastest, but sufficient
	private static final double qm(BitSet theMembers, float[] theBounds, int[] theBinIndexes)
	{
		int aNrBounds = theBounds.length;
		// first get counts for subgroup and complement for each bin
		int[] aCountsSubgroup = new int[aNrBounds];
		int[] aCountsComplement = new int[aNrBounds];

		for (int i = 0, j = theBinIndexes.length; i < j; ++i)
		{
			int aBinIndex = theBinIndexes[i];
			if (theMembers.get(i))
				++aCountsSubgroup[aBinIndex];
			else
				++aCountsComplement[aBinIndex];
		}

		// transform bin counts into PMFs by dividing by |SG| or |COMPL|
		double sg = theMembers.cardinality();
		double co = theBinIndexes.length - sg;

// FIXME MM TEMP
System.out.println("#BOUND(<=)\tSUBGROUP\tCOMPLEMENT");
for (int i = 0, j = aNrBounds; i < j; ++i)
	System.out.format("%f\t%f\t%f%n", theBounds[i], aCountsSubgroup[i]/sg, aCountsComplement[i]/co);
System.out.println();

// FOR DEBUGGING
double sumPi = 0.0;
double sumQi = 0.0;
		// H(P,Q)= 1/sqrt(2) * sqrt( sum_i_k ([sqrt(p_i)-sqrt(q_i)]^2) )
		double aSum = 0.0;
		for (int i = 0; i < aNrBounds; ++i)
		{
			double p_i = aCountsSubgroup[i]/sg;
			double q_i = aCountsComplement[i]/co;

			double p_i_sqrt = Math.sqrt(p_i);
			double q_i_sqrt = Math.sqrt(q_i);

			double diff = (p_i_sqrt - q_i_sqrt);

			aSum += (diff * diff);

sumPi += p_i;
sumQi += q_i;
		}
System.out.format("#SUMS:\tsumPi=%f\tsumQi=%f%n", sumPi, sumQi);

		return (1.0 / Math.sqrt(2.0)) * Math.sqrt(aSum);
	}

	private final double[] getPMF(BitSet theMembers)
	{
		int aSize = itsBounds.length;
		int[] aCounts = new int[aSize];

		int c = 0; // instead of theMembers.cardinality()
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i + 1))
		{
			++c;
			++aCounts[itsBinIndexes[i]];
		}

		double d = c;
		double[] aPMF = new double[aSize];
		for (int i = 0; i < aSize; ++i)
			aPMF[i] = (aCounts[i] / d);

		return aPMF;
	}

	private static final void histo(float[] theBounds, int[] theBinIndexes)
	{
		int[] aCounts = new int[theBounds.length];

		for (int i : theBinIndexes)
			++aCounts[i];

		System.out.println("\nPMF histogram:");
		System.out.println("BOUND (<=)\tCOUNT\tRELATIVE");
		float n = theBinIndexes.length;
		for (int i = 0; i < theBounds.length; ++i)
			System.out.format("%f\t%d\t%f%n", theBounds[i], aCounts[i], aCounts[i]/n);
	}
}
