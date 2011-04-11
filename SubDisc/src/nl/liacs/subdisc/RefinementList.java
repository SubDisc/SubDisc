package nl.liacs.subdisc;

import java.util.ArrayList;

public class RefinementList extends ArrayList<Refinement>
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;
	private Subgroup itsSubgroup;

	public RefinementList(Subgroup theSubgroup, Table theTable, TargetConcept theTC)
	{
		itsSubgroup = theSubgroup;
		itsTable = theTable;
		Log.logCommandLine("refinementlist");

		Condition aCondition = itsTable.getFirstCondition();

/*
		do
		{
			Attribute anAttribute = aCondition.getAttribute();
			Column aColumn = itsTable.getColumn(anAttribute);
			if (aColumn.getIsEnabled() && !theTC.isTargetAttribute(anAttribute))
			{
				Refinement aRefinement = new Refinement(aCondition, itsSubgroup);
				add(aRefinement);
				Log.logCommandLine("   condition: " + aCondition.toString());
			}
		}
		while ((aCondition = itsTable.getNextCondition(aCondition)) != null);
*/
		do
		{
			Column aColumn = aCondition.getAttribute();
			if (aColumn.getIsEnabled() && !theTC.isTargetAttribute(aColumn))
			{
				Refinement aRefinement = new Refinement(aCondition, itsSubgroup);
				add(aRefinement);
				Log.logCommandLine("   condition: " + aCondition.toString());
			}
		}
		while ((aCondition = itsTable.getNextCondition(aCondition)) != null);
	}
}
