package nl.liacs.subdisc;

import java.util.ArrayList;

public class RefinementList extends ArrayList<Refinement>
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;
	private Subgroup itsSubgroup;

	public RefinementList(Subgroup theSubgroup, Table theTable, SearchParameters theSearchParameters)
	{
		Log.logCommandLine("refinementlist");

		itsSubgroup = theSubgroup;
		itsTable = theTable;

		final SearchParameters aSP = theSearchParameters;
		final TargetConcept aTC = aSP.getTargetConcept();
		final NumericOperators aNO = aSP.getNumericOperators();
		final boolean useNotEquals = theSearchParameters.getNominalNotEquals();

		Condition aCondition = itsTable.getFirstCondition();
		do
		{
			boolean add = false;
			Column aColumn = aCondition.getColumn();

			if (aColumn.getIsEnabled() && !aTC.isTargetAttribute(aColumn))
			{
				Refinement aRefinement = new Refinement(aCondition, itsSubgroup);

				//check validity of operator
				//numeric
				if (aColumn.isNumericType() && NumericOperators.check(aNO, aCondition.getOperator()))
					add = true;
				//nominal
				else if (!aColumn.isNumericType() && (useNotEquals || !aCondition.checksNotEquals()))
				{
					if (aTC.isSingleNominal() || aCondition.getOperator() != Condition.ELEMENT_OF) // set-valued only allowed for SINGLE_NOMINAL
						add = true;
				}

				if (add)
				{
					add(aRefinement);
					Log.logCommandLine("   condition: " + aCondition.toString());
				}
			}
		}
		while ((aCondition = itsTable.getNextCondition(aCondition)) != null);
	}
}
