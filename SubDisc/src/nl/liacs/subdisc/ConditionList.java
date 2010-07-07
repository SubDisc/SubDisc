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
		for (int i=0; i<itsConditions.size(); i++)
            aNewConditionList.addCondition(itsConditions.get(i).copy());
		return aNewConditionList;
	}

    public String toString()
	{
        String aResult = "";
        for (int i=0; i<itsConditions.size(); i++)
		{
            String aCondition = itsConditions.get(i).toString();
            if (i == 0)
                aResult += aCondition;
            else
                aResult += " AND " + aCondition;
        }
        return aResult;
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
