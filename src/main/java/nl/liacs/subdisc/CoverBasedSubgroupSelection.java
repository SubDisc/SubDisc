package nl.liacs.subdisc;

import java.text.*;
import java.util.*;

public class CoverBasedSubgroupSelection
{
	private static final boolean USE_CORRECTED_MEASURE = true;  // true in git
	private static final boolean DEBUG_PRINTS          = false; // false in git
	private static final double  ALPHA                 = 0.9;   // as in papers

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

		Log.logCommandLine("cbss temporary queue: " + aSize);

		if (aSize == 0)
			return Collections.emptySortedSet();

		if (aSize <= theTopK)
			return theCandidates;

		BitSet aUsed = new BitSet(aSize);
		// make copy to allow index-based access, making the loop more efficient
		Candidate[] aCandidates = theCandidates.toArray(new Candidate[0]);
		// in each iteration of the loop, a Candidate will be added to aResult
		// and the cover counts go up, so the multiplicative weights go down
		// thus for each iteration the maximum score a Candidate can attain is
		// upper bounded by the score attained in a previous iteration
		// this allows for a useful optimisation, see main loop below
		double[] aLastScores = new double[aSize];
		// cache BitSets, as each call to Subgroup.getMembers() creates a clone
		// TODO
		// when aSize or aNrRows is very high, this uses a lot of memory
		// Runtime.maxMemory/totalMemory/freeMemory can be used to estimate
		// how many of the BitSets can be cached without OutOfMemory errors
		BitSet[] aMembers = new BitSet[aSize];
		int idx = -1;
		for (Candidate c : theCandidates)
		{
			++idx;
			aCandidates[idx] = c;
			aLastScores[idx] = c.getPriority();
			Subgroup s       = c.getSubgroup();
			aMembers[idx]    = s.getMembers();
			// NOTE when called from ResultWindow, members are set because a
			// Table modification would make it impossible to evaluate the
			// Subgroups (due to changed missing value, AttributeType, ...)
			// currently there is no way to find out if this is a GUI-based call
			if (isForCandidateSet)
				s.killMembers();
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

		// the first loop is special, as there are no Subgroups in aResult yet,
		// the multiplicative weight for each Candidate is equal to 1.0
		// so the Candidate with the highest priority wins
		// and its new priority = 1.0 * c.getPriority()
		// therefore the first iteration is taken out of the loop
		print("loop 0");
		update(isForCandidateSet, aUsed, aCandidates, aMembers, aCoverCounts, aResult, 0, 1.0, aLastScores[0]);

		// keep unset outside of the valid range of indexes (so not 0)
		for (int i = 1, min = Math.min(aSize, theTopK), unset = -1; i < min; ++i)
		{
			print("loop " + i);

			// always use unset, both NEGATIVE_INFINITY and NaN are possible
			// priorities/qualities, as some model classes do not check them
			// and NEGATIVE_INFINITY might be a valid result for some
			//
			// the use of (aQuality > aMaxScore) would ignore all values
			// that are equal to Double.NEGATIVE_INFINITY, which is incorrect
			// behaviour, and would lead to too few Candidates in aResult
			//
			// the values below could be set using the first clear index, such
			// that the unset-checks can be removed from loop, but this is fine
			int    aBestIndex           = unset;
			double aMaxScore            = Double.NaN;
			double aBestCandidateWeight = Double.NaN;

			// a minor optimisation would keep track of the smallest clear index
			// and start at that index, instead of 1 (index 0 is always set)
			for (int j = aUsed.nextClearBit(1); j < aSize; j = aUsed.nextClearBit(j+1))
			{
				// score can not get better than last time, so if that would not
				// have been good enough there is no use in checking Candidate
				//
				// do not use <= here
				// a Candidate (value) might be skipped for a number of
				// loop-iterations, by the time it is used, multiple (remaining)
				// values in aLastQualities might be equal, but not all based on
				// the last state of the cover counts, and need to be updated
				if ((aBestIndex != unset) && (aLastScores[j] < aMaxScore))
					continue;

				Candidate aCandidate = aCandidates[j];
				double aPriority     = aCandidate.getPriority();
				double aWeight       = computeMultiplicativeWeight(aMembers[j], aCandidate.getSubgroup().getCoverage(), aCoverCounts);
				double aScore        = (USE_CORRECTED_MEASURE ? (aPriority - ((1.0 - aWeight) * aPriority)) : (aWeight * aPriority));
				aLastScores[j]       = aScore;

				// the use of > causes the first (encountered) of multiple
				// equal-scoring (remaining) Candidates to be selected
				// single-thread experiment results are invocation-invariant
				// for multi-threaded experiments this was not true for the old
				// implementation, as either [x AND y] or [y AND x] might arrive
				// first, based on Operating System-controlled scheduling of
				// Thread execution and time-slicing
				// these are out of the control of the algorithm/SubDisc/JVM
				// TODO check: the new algorithm should be invocation-invariant
				if ((aBestIndex == unset) || (aScore > aMaxScore))
				{
					aBestIndex           = j;
					aMaxScore            = aScore;
					aBestCandidateWeight = aWeight;
				}
			}

			update(isForCandidateSet, aUsed, aCandidates, aMembers, aCoverCounts, aResult, aBestIndex, aBestCandidateWeight, aMaxScore);
		}

		if (DEBUG_PRINTS)
		{
			print(String.format("used: %s%n", aUsed));
			String s = (isForCandidateSet ? "priority: " : "result: ");
			for (Candidate aCandidate : aResult)
				print(s + (isForCandidateSet ? aCandidate.getPriority() : aCandidate.getSubgroup().getMeasureValue()));
		}

		return aResult;
	}

	// hope JVM inlines this
	private static final void update(boolean isForCandidateSet, BitSet theUsed, Candidate[] theCandidates, BitSet[] theMembers, int[] theCoverCounts, SortedSet<Candidate> theResult, int theBestIndex, double theBestCandidateWeight, double theMaxScore)
	{
		DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		df.setMaximumFractionDigits(340); // 340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

		theUsed.set(theBestIndex);
		Candidate c = theCandidates[theBestIndex];
		print(c.getSubgroup().toString());
		print(String.format("best (%d): %s, %s, %s%n", theBestIndex, df.format(c.getPriority()), df.format(theBestCandidateWeight), df.format(theMaxScore)));
		if (isForCandidateSet) // could be unconditional, ResultSet ignores it
			c.setPriority(theMaxScore);
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

		for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i+1))
			aResult += Math.pow(ALPHA, theCoverCounts[i]);

		return aResult / theCoverage;
	}

	private static final void print(String theMessage)
	{
		if (DEBUG_PRINTS)
			Log.logCommandLine(theMessage);
	}
}
