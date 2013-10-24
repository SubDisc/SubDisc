package nl.liacs.subdisc;

import java.util.*;

@Deprecated
public class ConditionList extends ArrayList<Condition> implements Comparable<ConditionList>
{
	private static final long serialVersionUID = 1L;

	public ConditionList(int size) { super(size); }

	public ConditionList copy()
	{
		// a copy is made just before adding a new Condition -> size+1
		ConditionList aNewConditionList = new ConditionList(this.size() + 1);
		// NOTE reuse would be safe as Conditions are immutable
		for (Condition aCondition : this)
			aNewConditionList.addCondition(new Condition(aCondition));
			//aNewConditionList.addCondition(aCondition);
		return aNewConditionList;
	}

	public boolean addCondition(Condition aCondition)
	{
		if (indexOf(aCondition) == -1) //already present?
		{
			add(aCondition);
			return true;
		}
		else
			return false;
	}

	// throws NullPointerException if theCondition is null.
	@Override
	public int compareTo(ConditionList theConditionList)
	{
		if (this == theConditionList)
			return 0;

		int cmp = this.size() - theConditionList.size();
		if (cmp != 0)
			return cmp;

		for (int i = 0, j = size(); i < j; ++i)
		{
			cmp = this.get(i).compareTo(theConditionList.get(i));
			if (cmp != 0)
				return cmp;
		}

		return 0;
	}

	/*
	 * equals() and findCondition() appear not to be used in current code
	 * these methods are broken in many ways
	 * 
	 * when overriding equals(), also override hashcode(), per Map-contract
	 * 
	 * findCondition() calls Condition.equals()
	 * Condition does not override Object.equals()
	 * but in current code, all Conditions in ConditionLists are copies
	 * so Objects reference-based comparison would always fail
	 */
	@Deprecated
	private boolean findCondition(Condition theCondition)
	{
		for (Condition aCondition : this)
			if (theCondition.equals(aCondition))
				return true;
		return false;
	}

	//this method computes logical equivalence. This means that the actual number of conditions or the order may differ.
	//Just as long as it effectively selects the same subgroup, no matter what the database is.
	//This method currently doesn't consider equivalence of the type a<10&a<20 vs. a<10 etc.
	@Deprecated
	@Override
	public boolean equals(Object theObject)
	{
		if (theObject == null || (theObject.getClass() != this.getClass()))
			return false;
		ConditionList aCL = (ConditionList) theObject;

		//check in one direction
		for (Condition aCondition : aCL)
			if (!findCondition(aCondition))
				return false;

		//check in the other direction
		for (Condition aCondition : this)
			if (!aCL.findCondition(aCondition))
				return false;

		return true;
	}

	@Override
	public String toString()
	{
		int size = size();
		if (size == 0)
			return "(empty)";

		StringBuilder aResult = new StringBuilder(size * 25);
		aResult.append(this.get(0)); // safe as size != 0
		for (int i = 1; i < size; ++i)
		{
			aResult.append(" AND ");
			aResult.append(this.get(i));
		}

		return aResult.toString();
	}
}
