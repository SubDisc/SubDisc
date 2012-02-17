package nl.liacs.subdisc;

import java.util.ArrayList;

public class RefinementList extends ArrayList<Refinement>
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;
	private Subgroup itsSubgroup;

	public RefinementList(Subgroup theSubgroup, Table theTable, SearchParameters theSearchParameters)
	{
		itsSubgroup = theSubgroup;
		itsTable = theTable;
		Log.logCommandLine("refinementlist");

		Condition aCondition = itsTable.getFirstCondition();
		do
		{
			Column aColumn = aCondition.getColumn();
			if (aColumn.getIsEnabled() && !theSearchParameters.getTargetConcept().isTargetAttribute(aColumn))
			{
				Refinement aRefinement = new Refinement(aCondition, itsSubgroup);

				//check validity of operator
				//numeric
				if (aColumn.isNumericType() && NumericOperators.check(theSearchParameters.getNumericOperators(), aCondition.getOperator()))
				{
					add(aRefinement);
					Log.logCommandLine("   condition: " + aCondition.toString());
				}
				//nominal
				if (!aColumn.isNumericType() && (theSearchParameters.getNominalNotEquals() || !aCondition.checksNotEquals()))
				{
					add(aRefinement);
					Log.logCommandLine("   condition: " + aCondition.toString());
				}
			}
		}
		while ((aCondition = itsTable.getNextCondition(aCondition)) != null);
	}
}
