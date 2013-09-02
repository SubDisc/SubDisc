package nl.liacs.subdisc;

import java.util.*;

public class RefinementList extends ArrayList<Refinement>
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;
	private Subgroup itsSubgroup;

	@Deprecated
	public RefinementList(Subgroup theSubgroup, Table theTable, SearchParameters theSearchParameters)
	{
		// crude estimate based on 2 operators per column, all numeric
		final int init = theTable.getNrColumns() * 2 * 32;
		final StringBuilder sb = new StringBuilder(init);
		sb.append("refinementlist\n");

		itsSubgroup = theSubgroup;
		itsTable = theTable;

		final SearchParameters aSP = theSearchParameters;
		final boolean useSets = aSP.getNominalSets();
		final NumericOperatorSetting aNO = aSP.getNumericOperatorSetting();
		final TargetConcept aTC = aSP.getTargetConcept();
		final boolean isSingleNominalTT = (aTC.getTargetType() == TargetType.SINGLE_NOMINAL);

		Condition aCondition = itsTable.getFirstCondition();
		AttributeType aType;
		do
		{
			Column aColumn = aCondition.getColumn();

			if (aColumn.getIsEnabled() && !aTC.isTargetAttribute(aColumn))
			{
				boolean add = false;
				aType = aColumn.getType();

				//check validity of operator
				//numeric
				if (aType == AttributeType.NUMERIC && NumericOperatorSetting.check(aNO, aCondition.getOperator()))
					add = true;
				//nominal
				else if (aType == AttributeType.NOMINAL && !useSets && aCondition.isEquals())
				{
					// set-valued only allowed for SINGLE_NOMINAL
					if (isSingleNominalTT || aCondition.getOperator() != Operator.ELEMENT_OF)
						add = true;
					// TODO MM aCondition.isEquals() -> implies 'aCondition.getOperator() != Operator.ELEMENT_OF'
					// so || is always true
					// if check is redundant
				}
				else if (aType == AttributeType.NOMINAL && useSets && aCondition.isElementOf())
					// TODO MM SINGLE_NOMINAL check should be here?
					// probably other code ensured proper
					// execution coincidentally
					add = true;
				//binary
				else if (aType == AttributeType.BINARY)
					add = true;

				if (add)
				{
					add(new Refinement(aCondition, itsSubgroup));
					sb.append("   condition: ");
					sb.append(aCondition.toString());
					sb.append("\n");
				}
			}
		}
		while ((aCondition = itsTable.getNextCondition(aCondition)) != null);

		Log.logCommandLine(sb.toString());
	}

	public RefinementList(Subgroup theSubgroup, ConditionBaseSet theConditionBaseSet)
	{
		List<Condition> aConditions = theConditionBaseSet.copy();
		StringBuilder sb = new StringBuilder(aConditions.size() * 32);
		sb.append("refinementlist\n");

		for (Condition c : aConditions)
		{
			super.add(new Refinement(c, theSubgroup));
			sb.append("   condition: ");
			sb.append(c.toString());
			sb.append("\n");
		}

		Log.logCommandLine(sb.toString());
	}
}
