package nl.liacs.subdisc;

import java.util.*;

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
		final boolean useSets = theSearchParameters.getNominalSets();

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
				else if (aColumn.isNominalType() && !useSets && aCondition.isEquals())
				{
					if (aTC.isSingleNominal() || aCondition.getOperator() != Condition.ELEMENT_OF) // set-valued only allowed for SINGLE_NOMINAL
						add = true;
				}
				else if (aColumn.isNominalType() && useSets && aCondition.isElementOf())
					add = true;
				//binary
				else if (aColumn.isBinaryType())
					add = true;

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
