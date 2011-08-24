package nl.liacs.subdisc;

public class Candidate implements Comparable<Object>
{
	private Subgroup itsSubgroup;
	private double itsPriority;

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
	public int compareTo(Object theObject)
	{
		if (itsPriority > ((Candidate)theObject).itsPriority)
			return -1;

		if (itsPriority < ((Candidate)theObject).itsPriority)
			return 1;

		//equal priorities
		if (itsSubgroup.getDepth() > ((Candidate)theObject).itsSubgroup.getDepth())
			return -1;

		return 1;
	}

	public double getPriority() { return itsPriority; }
	public void setPriority(double thePriority) { itsPriority = thePriority; }
	public Subgroup getSubgroup() { return itsSubgroup; }
}