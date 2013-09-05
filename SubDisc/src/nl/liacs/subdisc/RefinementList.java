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
		sb.append("\t").append(theSubgroup.getConditions()).append("\n");

		for (Condition c : aConditions)
		{
			super.add(new Refinement(c, theSubgroup));
			sb.append("   condition: ");
			sb.append(c.toString());
			sb.append("\n");
		}

		Log.logCommandLine(sb.toString());
	}

//	// TMP constructor for new 'depth_first', will be merged
//	RefinementList(Subgroup theSubgroup, ConditionBaseSet theConditionBaseSet, SearchStrategy theSearchStrategy)
//	{
//		// for depth_first create only (n*(n-1)/2 Conditions
//		// for beams, no smart cut can be done -> n^2 Conditions
//		List<Condition> aConditions;
//
//		// switch is overkill, but properly handles all possibilities
//		switch (theSearchStrategy)
//		{
//			case BEAM :
//				aConditions = theConditionBaseSet.copy();
//				break;
//			case ROC_BEAM :
//				aConditions = theConditionBaseSet.copy();
//				break;
//			case COVER_BASED_BEAM_SELECTION :
//				aConditions = theConditionBaseSet.copy();
//				break;
//// FIXME MM BEST, DEPTH and BREADTH will be removed
//			case BEST_FIRST :
//				aConditions = theConditionBaseSet.copy();
//				break;
//			case DEPTH_FIRST :
//				aConditions = getConditions(theSubgroup, theConditionBaseSet);
//				break;
//			case NEW_DEPTH_FIRST :
//				aConditions = getConditions(theSubgroup, theConditionBaseSet);
//				break;
//			case BREADTH_FIRST :
//				aConditions = theConditionBaseSet.copy();
//				break;
//			default :
//				throw new AssertionError(theSearchStrategy);
//		}
//
//		createList(theSubgroup, aConditions);
//	}
//
//	private static final List<Condition> getConditions(Subgroup theSubgroup, ConditionBaseSet theConditionBaseSet)
//	{
//		// by construction need to only continue from last
//		if (theSubgroup.getDepth() > 0)
//		{
//			ConditionList cl = theSubgroup.getConditions();
//			return theConditionBaseSet.copyFrom(cl.get(cl.size()-1));
//		}
//		else // for root Candidate / Subgroup only
//			return theConditionBaseSet.copy();
//	}
//
//	private final void createList(Subgroup theSubgroup, List<Condition> theConditions)
//	{
//		StringBuilder sb = new StringBuilder(theConditions.size() * 32);
//		sb.append("refinementlist\n");
//		sb.append("\t").append(theSubgroup.getConditions()).append("\n");
//
//		for (Condition c : theConditions)
//		{
//			super.add(new Refinement(c, theSubgroup));
//			sb.append("   condition: ");
//			sb.append(c.toString());
//			sb.append("\n");
//		}
//
//		Log.logCommandLine(sb.toString());
//	}
}
