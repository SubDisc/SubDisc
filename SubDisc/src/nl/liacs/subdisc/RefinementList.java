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
		final NumericOperatorSetting aNO = aSP.getNumericOperatorSetting();
		final boolean useSets = theSearchParameters.getNominalSets();

		Condition aCondition = itsTable.getFirstCondition();
		do
		{
			boolean add = false;
			Column aColumn = aCondition.getColumn();

			if (aColumn.getIsEnabled() && !aTC.isTargetAttribute(aColumn))
			{
				Refinement aRefinement = new Refinement(aCondition, itsSubgroup);

				// FIXME should be part of Operator
				//check validity of operator
				//numeric
				if (aColumn.isNumericType() && NumericOperatorSetting.check(aNO, aCondition.getOperator()))
					add = true;
				//nominal
				else if (aColumn.isNominalType() && !useSets && aCondition.isEquals())
				{
					if (aTC.isSingleNominal() || aCondition.getOperator() != Operator.ELEMENT_OF) // set-valued only allowed for SINGLE_NOMINAL
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
