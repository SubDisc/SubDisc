package nl.liacs.subdisc;

class CandidateROCPoint extends SubgroupROCPoint
{
	private final Candidate itsCandidate;
	double itsSlope = Double.NaN;

	CandidateROCPoint(Candidate theCandidate)
	{
		super(theCandidate.getSubgroup());
		itsCandidate = theCandidate;
	}

	/**
	 * Overrides <code>Object</code>s' <code>toString()</code> method to get
	 * detailed information about this CandidateROCPoint.
	 * 
	 * @return a <code>String</code> representation of this
	 * CandidateROCPoint.
	 */
	@Override
	public String toString()
	{
		return
		"CandidateROCPoint("+x+" ,"+y+") " + itsCandidate.toString();
	}
}
