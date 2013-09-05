package nl.liacs.subdisc;

import java.util.*;

public class Candidate implements Comparable<Candidate>
{
	private final Subgroup itsSubgroup;
	private double itsPriority;

	public Candidate(Subgroup theSubgroup)
	{
		itsSubgroup = theSubgroup;
		itsPriority = theSubgroup.getMeasureValue();
	}

	// throws NullPointerException if theCandidate is null.
	@Override
	public int compareTo(Candidate theCandidate)
	{
		if (this == theCandidate)
			return 0;
		else if (itsPriority > theCandidate.itsPriority)
			return -1;
		else if (itsPriority < theCandidate.itsPriority)
			return 1;
		//equal priorities
		int aTest = itsSubgroup.compareTo(theCandidate.getSubgroup());
		if (aTest != 0)
			return aTest;

		// this should never happen
		// equal priority, subgroup, condition list, condition(s) would
		// mean it is the exact same Candidate
		System.out.println(new AssertionError("ERROR: Candidate.compareTo()"));
		return 1; // ?
	}

	public double getPriority() { return itsPriority; }
	public void setPriority(double thePriority) { itsPriority = thePriority; }
	public Subgroup getSubgroup() { return itsSubgroup; }

	@Override
	public String toString()
	{
		return "Candidate: priority=" + itsPriority +
				" ConditionsList=" + itsSubgroup.toString();
	}

	static Comparator<Candidate> getComparator(SearchStrategy theSearchStrategy)
	{
		switch (theSearchStrategy)
		{
			case BEAM :
				throw new AssertionError(theSearchStrategy);
			case ROC_BEAM :
				throw new AssertionError(theSearchStrategy);
			case COVER_BASED_BEAM_SELECTION :
				throw new AssertionError(theSearchStrategy);
			case BEST_FIRST :
				throw new AssertionError(theSearchStrategy);
			case DEPTH_FIRST :
				return new CandidateComparatorDepthFirst();
			case BREADTH_FIRST :
				return new CandidateComparatorBreadthFirst();
			default :
				throw new AssertionError(theSearchStrategy);
		}
	}

	// default Comparator, provided for completeness, do not use
	// orders by the natural ordering of Candidates, so it is redundant
	/* see comment @CandidateComparatorBreadthFirst */
	static class CandidateComparatorBestFirst implements Comparator<Candidate>
	{
		@Override
		public int compare(Candidate x, Candidate y)
		{
			return x.compareTo(y);
		}
	}

	/* see comment @CandidateComparatorBreadthFirst */
	static class CandidateComparatorDepthFirst implements Comparator<Candidate>
	{
		@Override
		public int compare(Candidate x, Candidate y)
		{
			// check on depth first, higher depth comes first
			// on equal depth, perform normal comparison
			int cmp = x.itsSubgroup.itsDepth - y.itsSubgroup.itsDepth;
			return (cmp != 0) ? -cmp : x.compareTo(y);
		}
	}

	/*
	 * this scenario is awkward in concurrent setups as Subgroups of
	 * different depths may be added by concurrent threads
	 * as a result a Subgroup of depth d+1 may be tested before a Subgroup
	 * of depth d, depending on when various threads add new Candidates to
	 * the CandidateQueue
	 * when using just a single thread all Subgroups of depth d should be
	 * placed into the Queue, and subsequently tested, before any Subgroup
	 * of depth d+1
	 */
	static class CandidateComparatorBreadthFirst implements Comparator<Candidate>
	{
		@Override
		public int compare(Candidate x, Candidate y)
		{
			// check on depth first, lower depth comes first
			// on equal depth, perform normal comparison
			int cmp = x.itsSubgroup.itsDepth - y.itsSubgroup.itsDepth;
			return (cmp != 0) ? cmp : x.compareTo(y);
		}
	}
}
