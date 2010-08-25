/**
 * TODO why not extend ArrayListList<Condition>
 */
package nl.liacs.subdisc;

import java.util.ArrayList;

public class ConditionList
{
	ArrayList<Condition> itsConditions;

	public ConditionList()
	{
		itsConditions = new ArrayList<Condition>();
	}

	public ConditionList copy()
	{
		ConditionList aNewConditionList = new ConditionList();
		for(Condition aCondition : itsConditions)
			aNewConditionList.addCondition(aCondition.copy());
		return aNewConditionList;
	}

	public String toString()
	{
		StringBuilder aResult = new StringBuilder(itsConditions.size() * 25);
		for(Condition aCondition : itsConditions)
			aResult.append(aCondition + " AND ");
		return aResult.replace(aResult.length() - 5, aResult.length(), "").toString();
	}

	//this method computes logical equivalence. This means that the actual number of conditions or the order may differ.
	//Just as long as it effectively selects the same subgroup, no matter what the database is.
	//This method currently doesn't consider equavalence of the type a<10&a<20 vs. a<10 etc.
	public boolean equals(Object theObject)
	{
		if (theObject.getClass() != this.getClass())
			return false;
		ConditionList aCL = (ConditionList) theObject;

		//check in one direction
		for (Condition aCondition : aCL.itsConditions)
			if (!findCondition(aCondition))
				return false;

		//check in the other direction
		for (Condition aCondition : itsConditions)
			if (!aCL.findCondition(aCondition))
				return false;

		return true;
	}

	public boolean findCondition(Condition theCondition)
	{
		for (Condition aCondition : itsConditions)
			if (theCondition.equals(aCondition))
				return true;
		return false;
	}

	public boolean addCondition(Condition aCondition)
	{
		if (itsConditions.indexOf(aCondition) == -1) //already present?
		{
			itsConditions.add(aCondition);
			return true;
		}
		else
			return false;
	}

	public Condition getCondition(int theIndex) { return itsConditions.get(theIndex); }
	public int size() { return itsConditions.size(); }
}
