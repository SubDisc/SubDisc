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

	/** Nominal Column, single value. */
	public Subgroup getRefinedSubgroup(String theValue)
	{
		Condition aCondition = new Condition(itsConditionBase, theValue);
		return getRefinedSubgroup(itsSubgroup, aCondition);
	}

	/** Nominal Column, ValueSet. */
	public Subgroup getRefinedSubgroup(ValueSet theValue)
	{
		Condition aCondition = new Condition(itsConditionBase, theValue);
		return getRefinedSubgroup(itsSubgroup, aCondition);
	}

	/** Binary Column. */
	public Subgroup getRefinedSubgroup(boolean theValue)
	{
		Condition aCondition = new Condition(itsConditionBase, theValue);
		return getRefinedSubgroup(itsSubgroup, aCondition);
	}

	/** Numeric Column, single value. */
	public Subgroup getRefinedSubgroup(float theValue)
	{
		Condition aCondition = new Condition(itsConditionBase, theValue);
		return getRefinedSubgroup(itsSubgroup, aCondition);
	}

	/** Numeric Column, Interval. */
	public Subgroup getRefinedSubgroup(Interval theValue)
	{
		Condition aCondition = new Condition(itsConditionBase, theValue);
		return getRefinedSubgroup(itsSubgroup, aCondition);
	}

	public ConditionBase getConditionBase() { return itsConditionBase; }

	public Subgroup getSubgroup() { return itsSubgroup; }

	private static final Subgroup getRefinedSubgroup(Subgroup theSubgroup, Condition theCondition)
	{
		assert (theSubgroup != null);
		assert (theCondition != null);

		// see remarks at Subgroup.copy about deep-copy
		Subgroup aRefinedSubgroup = theSubgroup.copy();
		aRefinedSubgroup.addCondition(theCondition);

		return aRefinedSubgroup;
	}
}
