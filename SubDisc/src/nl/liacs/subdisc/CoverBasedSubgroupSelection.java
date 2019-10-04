package nl.liacs.subdisc;

import java.text.*;
import java.util.*;

public class CoverBasedSubgroupSelection
{
	private static final double ALPHA = 0.9;

	static final SortedSet<Candidate> postProcessCandidateSet(SortedSet<Candidate> theCandidates, int theTopK)
	{
		return postProcess(theCandidates, theTopK, true);
	}

	static final SortedSet<Candidate> postProcessResultSet(SortedSet<Subgroup> theSubgroups, int theTopK)
	{
		// lazy, do not write the whole method twice
		// assumption is that ResultSet is (much) smaller than CandidateSet
		// also, multiple CandidateSets might need to be processed during run
		// assumes theTopK is small also
		SortedSet<Candidate> aCandidates = new TreeSet<>();
		for (Subgroup s : theSubgroups)
			aCandidates.add(new Candidate(s));

		return postProcess(aCandidates, theTopK, false);
	}

	private static final SortedSet<Candidate> postProcess(SortedSet<Candidate> theCandidates, int theTopK, boolean isForCandidateSet)
	{
		// technically a Tree could hold more nodes than Integer.MAX_VALUE
		// but a lot of code would crash everywhere, so assume aSize is in range
		int aSize = theCandidates.size();

		if (aSize == 0)
			return Collections.emptySortedSet();

		BitSet aUsed = new BitSet(aSize);
		// make copy to allow index-based access, making the loop more efficient
		Candidate[] aCandidates = theCandidates.toArray(new Candidate[0]);
		// in each iteration of the loop, a Candidate will be added to aResult
		// and the cover counts go up, so the multiplicative weights go down
		// thus for each iteration the maximum quality a Candidate can attain is
		// upper bounded by the quality attained in a previous iteration
		// this allows for a useful optimisation, see main loop below
		double[] aLastQualities = new double[aSize];
		// cache BitSets, as each call to Subgroup.getMembers() creates a clone
		// TODO
		// when aSize or aNrRows are very high, this uses a lot of memory
		// Runtime.maxMemory/totalMemory/freeMemory can be used to estimate
		// how many of the BitSets can be cached without OutOfMemory errors
		BitSet[] aMembers = new BitSet[aSize];
		int idx = -1;
		for (Candidate c : theCandidates)
		{
			aCandidates[++idx]  = c;
			aLastQualities[idx] = c.getPriority();
			aMembers[idx]       = c.getSubgroup().getMembers();
		}

		int aNrRows = theCandidates.first().getSubgroup().getParentSet().getTotalCoverage();
		int[] aCoverCounts = new int[aNrRows];

		// Subgroups are added based on highest score, so an array would do, but
		// CandidateQueue/ResultSet would then take the Subgroups from the array
		// and put them in a TreeSet anyway, such that sorting still happens
		// moreover, as aResult is a TreeSet, addAll() operations of
		// CandidateQueue/ResultSet will be linear, as their underlying TreeMaps
		// call addAllForTreeSet(), see the java source code for that
		SortedSet<Candidate> aResult = new TreeSet<>();

		Log.logCommandLine("subgroups: " + aSize);
		// the first loop is special, as there are no Subgroups in aResult yet,
		// the multiplicative weight for each Candidate is equal to 1.0
		// so the Candidate with the highest priority wins
		// and its new priority = 1.0 * c.getPriority()
		// therefore the first iteration is taken out of the loop
		Log.logCommandLine("loop 0");
		update(isForCandidateSet, aUsed, aCandidates, aMembers, aCoverCounts, aResult, 0, 1.0, aLastQualities[0]);

		for (int i = 1, min = Math.min(aSize, theTopK), unset = -1, aBestIndex = unset; i < min; ++i, aBestIndex = unset)
		{
			Log.logCommandLine("loop " + i);

			// FIXME
			// the use of (aQuality > aMaxQuality) would ignore all values
			// that are equal to Double.NEGATIVE_INFINITY, which is incorrect
			// behaviour, and would lead to too few Candidates in aResult
			//
			// this can be fixed by adding the unset status in this check, such
			// that aMaxQuality is set based on the first (remaining) Candidate
			// then (aLastQualities[j] < aMaxQuality) also requires unset status
			double aMaxQuality          = Double.NEGATIVE_INFINITY;
			double aBestCandidateWeight = Double.NaN;

			// a minor optimisation would keep track of the smallest unset index
			// and start at that index, instead of 1 (index 0 is always set)
			for (int j = aUsed.nextClearBit(1); j < aSize; j = aUsed.nextClearBit(j+1))
			{
				// score can not get better than last time, so if that would not
				// have been good enough there is no use in checking Candidate
				//
				// do not use <= here, when all (remaining) values in
				// aLastQualities are equal to aMaxQuality, the loop would not
				// select a best Candidate
				// also, a Candidate (value) might be skipped for a number of
				// loop-iterations, by the time it is used, multiple (remaining)
				// values in aLastQualities might be equal, but not all based on
				// the last state of the cover counts, and need to be updated
				if (aLastQualities[j] < aMaxQuality)
					continue;

				Candidate aCandidate = aCandidates[j];
				double aPriority     = aCandidate.getPriority();
				double aWeight       = computeMultiplicativeWeight(aMembers[j], aCandidate.getSubgroup().getCoverage(), aCoverCounts);
				double aQuality      = aPriority - ((1.0 - aWeight) * aPriority);
				aLastQualities[j]    = aQuality;

				// the use of > causes the first (encountered) of multiple
				// equal-scoring (remaining) Candidates to be selected
				// single-thread experiment results are invocation-invariant
				// for multi-threaded experiments this was not true for the old
				// implementation, as either [x AND y] or [y AND x] might arrive
				// first, based on Operating System-controlled scheduling of
				// Thread execution and time-slicing
				// these are out of the control of the algorithm/Cortana/JVM
				// TODO check: the new algorithm should be invocation-invariant
				if (aQuality > aMaxQuality)
				{
					aBestIndex           = j;
					aMaxQuality          = aQuality;
					aBestCandidateWeight = aWeight;
				}
			}
//System.out.println("loop=" + i + " chosen=" + aBestIndex + "\tbest=" + aCandidates[aBestIndex] + "\tstart=" + aCandidates[aBestIndex]);

			// all priorities can be NEGATIVE_INFINITY, it is a possible result
			// for some quality measures (whether it is valid is something else)
			// see comment above
			if (aBestIndex != unset)
				update(isForCandidateSet, aUsed, aCandidates, aMembers, aCoverCounts, aResult, aBestIndex, aBestCandidateWeight, aMaxQuality);
		}

		Log.logCommandLine("========================================================");
		Log.logCommandLine("used: " + aUsed.toString());

		String s = (isForCandidateSet ? "priority: " : "result: ");
		for (Candidate aCandidate : aResult)
			Log.logCommandLine(s + (isForCandidateSet ? aCandidate.getPriority() :
														aCandidate.getSubgroup().getMeasureValue()));

		return aResult;
	}

	// hope JVM inlines this
	private static final void update(boolean isForCandidateSet, BitSet theUsed, Candidate[] theCandidates, BitSet[] theMembers, int[] theCoverCounts, SortedSet<Candidate> theResult, int theBestIndex, double theBestCandidateWeight, double theMaxQuality)
	{
		DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		df.setMaximumFractionDigits(340); // 340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

		theUsed.set(theBestIndex);
		Candidate c = theCandidates[theBestIndex];
		Log.logCommandLine(c.getSubgroup().toString());
		Log.logCommandLine(String.format("best (%d): %s, %s, %s%n", theBestIndex, df.format(c.getPriority()), df.format(theBestCandidateWeight), df.format(theMaxQuality)));
		if (isForCandidateSet) // could be unconditional, ResultSet ignores it
			c.setPriority(theMaxQuality);
		theResult.add(c);
		updateCoverCounts(theMembers[theBestIndex], theCoverCounts);
	}

	private static final void updateCoverCounts(BitSet theMembers, int[] theCoverCounts)
	{
		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i+1))
			++theCoverCounts[i];
	}

	private static final double computeMultiplicativeWeight(BitSet theMembers, int theCoverage, int[] theCoverCounts)
	{
		double aResult = 0.0;

		for (int i = theMembers.nextSetBit(0); i >= 0; i =  theMembers.nextSetBit(i+1))
			aResult += Math.pow(ALPHA, theCoverCounts[i]);

		return aResult / theCoverage;
	}
}
