package nl.liacs.subdisc;

public class Refinement
{
	private Subgroup itsSubgroup;
	private Condition itsCondition;

	public Refinement(Condition aCondition, Subgroup aSubgroup)
	{
		itsCondition = aCondition;
		itsSubgroup = aSubgroup;
	}

	public Subgroup getRefinedSubgroup(String theValue)
	{
		// see remarks at Subgroup.copy about deep-copy
		Subgroup aRefinedSubgroup = itsSubgroup.copy();
		// see remarks at Condition.copy about deep-copy
		Condition aCondition = itsCondition.copy();
		// only set new value for copied Condition, not for itsCondition
		aCondition.setValue(theValue);
		aRefinedSubgroup.addCondition(aCondition);
		return aRefinedSubgroup;
	}

	public Subgroup getRefinedSubgroup(ValueSet theValue)
	{
		Subgroup aRefinedSubgroup = itsSubgroup.copy();
		Condition aCondition = itsCondition.copy();
		aCondition.setValue(theValue);
		aRefinedSubgroup.addCondition(aCondition);
		return aRefinedSubgroup;
	}
	public Condition getCondition() { return itsCondition; }

	public void setCondition(Condition theCondition) { itsCondition = theCondition; }

	public Subgroup getSubgroup() { return itsSubgroup; }
}
