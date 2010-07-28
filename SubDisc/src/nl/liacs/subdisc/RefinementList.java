package nl.liacs.subdisc;

import java.util.ArrayList;

public class RefinementList extends ArrayList<Refinement>
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;
	private Subgroup itsSubgroup;

//	TODO
//	public RefinementList(Subgroup theSubgroup, Table theTable, SearchParameters theParameters)
	public RefinementList(Subgroup theSubgroup, Table theTable)
	{
		itsSubgroup = theSubgroup;
		itsTable = theTable;
		Log.logCommandLine("refinementlist");

		Condition aCondition = itsTable.getFirstCondition();

		do
		{
			Refinement aRefinement = new Refinement(aCondition, itsSubgroup);
			add(aRefinement);
			Log.logCommandLine("   condition: " + aCondition.toString());
		}
		while ((aCondition = itsTable.getNextCondition(aCondition)) != null);
	}
}
