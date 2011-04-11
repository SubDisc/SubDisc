package nl.liacs.subdisc;

import java.util.ArrayList;

public class ConditionList extends ArrayList<Condition>
{
	private static final long serialVersionUID = 1L;

	public ConditionList copy()
	{
		ConditionList aNewConditionList = new ConditionList();
		for(Condition aCondition : this)
			aNewConditionList.addCondition(aCondition.copy());
		return aNewConditionList;
	}

	@Override
	public String toString()
	{
		StringBuilder aResult = new StringBuilder(size() * 25);
		for(Condition aCondition : this)
		{
			aResult.append(aCondition);
			aResult.append(" AND ");
		}
		return aResult.substring(0, aResult.length() - 5);
	}

	//this method computes logical equivalence. This means that the actual number of conditions or the order may differ.
	//Just as long as it effectively selects the same subgroup, no matter what the database is.
	//This method currently doesn't consider equavalence of the type a<10&a<20 vs. a<10 etc.
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

	public boolean findCondition(Condition theCondition)
	{
		for (Condition aCondition : this)
			if (theCondition.equals(aCondition))
				return true;
		return false;
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
}
