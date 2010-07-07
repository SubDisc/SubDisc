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

	public double getPriorityLevel() { return itsPriority; }
	public Subgroup getSubgroup() { return itsSubgroup; }
}