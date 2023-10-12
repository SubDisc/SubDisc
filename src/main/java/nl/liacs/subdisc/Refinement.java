package nl.liacs.subdisc;

public class Refinement
{
	private final ConditionBase itsConditionBase;
	private final Subgroup itsSubgroup;

	public Refinement(ConditionBase theConditionBase, Subgroup theSubgroup)
	{
		if (theConditionBase == null)
			throw new NullPointerException("Refinement: theConditionBase can not be null");
		if (theSubgroup == null)
			throw new NullPointerException("Refinement: theSubgroup can not be null");
		itsConditionBase = theConditionBase;
		itsSubgroup = theSubgroup;
	}

	public ConditionBase getConditionBase() { return itsConditionBase; }

	public Subgroup getSubgroup() { return itsSubgroup; }
}
