package nl.liacs.subdisc;

public class Candidate implements Comparable<Candidate>
{
	private Subgroup itsSubgroup;
	private double itsPriority;

	public Candidate(Subgroup theSubgroup)
	{
		itsSubgroup = theSubgroup;
		itsPriority = theSubgroup.getMeasureValue();
	}

	@Deprecated
	// For Wouter only, for the time being
	public Candidate(Subgroup theSubgroup, double thePriority)
	{
		itsSubgroup = theSubgroup;
		itsPriority = thePriority;
	}

	/*
	 * TODO this implementation is to narrow. This prevents CandidateQueue's
	 * internal TreeSet<Candidate> .remove(Object o) from working correctly.
	 */
	@Override
	public int compareTo(Candidate theCandidate)
	{
		if (itsPriority > theCandidate.itsPriority)
			return -1;

		if (itsPriority < theCandidate.itsPriority)
			return 1;

		//equal priorities
		if (itsSubgroup.getDepth() > theCandidate.itsSubgroup.getDepth())
			return -1;

		return 1;
	}

	// MM: LEAVE THIS IN IT WILL REPLACE OLD CODE
	// throws NullPointerException if theCandidate is null.
	//@Override
	public int compareTo2(Candidate theCandidate)
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

		return 1; // ?
	}

	public double getPriority() { return itsPriority; }
	public void setPriority(double thePriority) { itsPriority = thePriority; }
	public Subgroup getSubgroup() { return itsSubgroup; }
}
