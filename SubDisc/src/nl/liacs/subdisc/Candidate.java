package nl.liacs.subdisc;

import java.util.*;

public class Candidate implements Comparable<Candidate>
{
	private final Subgroup itsSubgroup;
	private double itsPriority;

	// FIXME MM disallow Candidates of size 0 and 1, they can not be refined
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

		// Candidates with a higher come first
		int cmp = Double.compare(this.itsPriority, theCandidate.itsPriority);
		if (cmp != 0)
			return -cmp;

		//equal priorities
		int aTest = itsSubgroup.compareTo(theCandidate.getSubgroup());
		if (aTest != 0)
			return aTest;

		// this should never happen
		// equal priority, subgroup, condition list, condition(s) would
		// mean it is the exact same Candidate
		// FIXME MM / test and remove this code
		// UPDATE MM: since revision r2174 this happens as a result of
		// the canonical ordering of Conditions
		// two equivalent ConditionLists (same set of Conditions)
		// return a compareTo()  of 0, which is correct
		// the return of this method is changed from 1 to 0
		// this removes redundancy
		// Note that creating (A ^ B) and (B ^ A) is invalid in a depth
		// first search, but valid in beam or breadth first search
		// checking for already tested Candidates is probably more work
		// than re-evaluation of a small number of them
		// at some point the return will be:
		// return itsSubgroup.compareTo(theCandidate.getSubgroup());
		// NOTE THAT THIS BREAKS THE (x.compareTo(y)==0)==(x.equals(y)) contract
		// ANOTHER SOLUTION WOULD BE TO USE -1 +1 FOR GENERAL CASES
		// AND -2 +2 TO INDICATE THAT THE ITEMS ARE EQUIVALENT (IN CANONICAL
		// FORM) BUT NOT EQUAL (IN SEARCH ORDER FORM), 0 MEANS SAME SEARCH ORDER
		// FORM AND IMPLIES SAME CANONINCAL FORM (SHOULD NEVER HAPPEN BTW)
		//
		// FIXME MM
		// to create invocation-invariant results, without calling
		// ConditionListBuilder.toCanonicalOrder(), a compareCanonicalOrder()
		// method would be called when compareTo() returns 0, which happens
		// when two ConditionListsAs hold the same set of Conditions, but in a
		// (possibly) different search order
		// compareCanonicalOrder() would only return 0 when two ConditionListsAs
		// are identical (same Object, same canonical order of Conditions should
		// not happen otherwise), else its return indicates which of the two is
		// (closer to) canonical order
		// could be a static method, or one for each ConditionListA subclass
		// NOTE to ensure proper functioning code, the compareTo() call is
		// required first, as at a return of 0, one of the the Candidates needs
		// to be removed from the CandidateSet or SubgroupSet (= ResultSet)
		System.out.format("WARNING REDUNDANCY: Candidate.compareTo()%n\t%s%n\t%s%n",
					itsSubgroup.toString(),
					theCandidate.getSubgroup().toString());
		return 0;
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
			return (x == y) ? 0 : x.compareTo(y);
		}
	}

	/* see comment @CandidateComparatorBreadthFirst */
	static class CandidateComparatorDepthFirst implements Comparator<Candidate>
	{
		@Override
		public int compare(Candidate x, Candidate y)
		{
			if (x == y)
				return 0;

			// check on depth first, higher depth comes first
			// on equal depth, perform normal comparison
			int cmp = x.itsSubgroup.getDepth() - y.itsSubgroup.getDepth();
//			return (cmp != 0) ? -cmp : x.compareTo(y);
			if (cmp != 0)
				return -cmp;

			// DEPTH_FIRST does not work on depth=0
			// however it can be made to work by ordering the
			// Candidates coming from depth=0
			cmp = x.itsSubgroup.getConditions().compareTo(y.itsSubgroup.getConditions());

			// for two distinct Candidates the ConditionLists should
			// never ever be equal in the DEPTH_FIRST setting
			// however, the current implementation does not restrict
			// itself to only forward search space exploration
			// for any depth it does N*N instead of N*N/2
//			assert (cmp != 0);
			// FIXME MM
			// below is a hack
			// the assertion above is disabled
			// the current search algorithm would yield both
			// 'A and B' and 'B and A' (instead of just the first)
			// ConditionListA.compareTo() considers the two equal,
			// so: cmp = 0
			// this is desired behaviour, but makes the assert fail
			// the assert below is not based on canonical ordering
			// but on search order
			// for two distinct Candidates, the two Strings should
			// never be the same
			assert (x.itsSubgroup.getConditions().toString().compareTo(
				y.itsSubgroup.getConditions().toString()) != 0);

			return cmp;
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
			if (x == y)
				return 0;

			// check on depth first, lower depth comes first
			// on equal depth, perform normal comparison
			int cmp = x.itsSubgroup.getDepth() - y.itsSubgroup.getDepth();
			return (cmp != 0) ? cmp : x.compareTo(y);
		}
	}
}
