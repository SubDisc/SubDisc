package nl.liacs.subdisc;

public class Refinement
{
    protected Subgroup itsSubgroup;
    private Condition itsCondition;

    public Refinement(Condition aCondition, Subgroup aSubgroup)
	{
        itsCondition = aCondition;
        itsSubgroup = aSubgroup;
    }

    public Subgroup getRefinedSubgroup(String theValue)
	{
        Subgroup aRefinedSubgroup = (Subgroup) itsSubgroup.copy(); // TODO: deep copy
        itsCondition.setValue(theValue);
        aRefinedSubgroup.addCondition(itsCondition.copy());
		return aRefinedSubgroup;
    }

	public Condition getCondition() { return itsCondition; }

	public void setCondition(Condition theCondition) { itsCondition = theCondition; }

	public Subgroup getSubgroup() { return itsSubgroup; }
}
