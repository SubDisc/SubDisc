package nl.liacs.subdisc;

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
}
