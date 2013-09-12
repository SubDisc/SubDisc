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

	/*
	 * relevant settings:
	 * search strategy,
	 * numeric strategy,,
	 * nominal operator setting,
	 * binary operator setting,
	 * numeric operator setting.
	 * 
	 * === SearchStratey ===
	 * for beam searches, Subgroup.ConditionList needs to be combined with
	 * every Column O(n^2)
	 * for non-beam searches only Columns following the last
	 * Subgroup.ConditionList.Condition.Column need to be combined with the
	 * current Subgroup.ConditionList O((n*(n-1))/2)
	 * still O(n^2), but evaluation is expensive and this reduces redundancy
	 * 
	 * NOTE
	 * some of the checks below could also be moved to
	 * SubgroupDiscovery.evaluate*Refinements()
	 * that code should also do (nominal/ numeric) domain checks
	 * the domain code should be updated to avoid creating useless
	 * Refinements
	 * examples are a domain A [1,2,3] and a ConditionList (A>=3),
	 * creating and evaluating Refinement (A>=3 ^ A>=1) makes no sense
	 * nor does (A>=3) ^ A<=1)
	 * both waste a lot of valuable CPU time for the NumericStrategy BINS,
	 * BEST and ALL (see INTERVALS below for its specific case)
	 * 
	 * === NumericStratey ===
	 * NumericStrategy.BINS
	 * in SubgroupDiscovery.evaluateNumericRefinements(), there is no use in
	 * testing the same Column.Operator.value as the last
	 * Subgroup.ConditionList.Column.Operator.value
	 * this would just create a ConditionList with the same Condition
	 * duplicated at the end
	 * current code does check for multiple identical values in the returned
	 * domain, but does perform validity checks on them in combination with
	 * existing Column.Operator.values in the ConditionList
	 * see NOTE above, some tests make no sense
	 * (A>=x ^ A>=x) does not, (A>=x ^ A<=x) does, as it selects just A=x
	 * by construction, (A>=x ^ A<=y), where y<x, is not possible
	 * 
	 * Numeric.BEST
	 * there is no use in including the same Column.Operator as the last
	 * Subgroup.ConditionList.Column.Operator
	 * this would just create a ConditionList with the same Condition
	 * duplicated at the end
	 * if on the previous depth (C>=v) was the best, on the next level
	 * it will be also, because if any w>v would now be the best, it was the
	 * best also on the previous depth (only >= and <= tests are available,
	 * the = part is key here)
	 * the new Subgroup would not decrease in size, its QM.score would not
	 * change, but the evaluation does unnecessarily take up CPU time
	 * this is particularly bad in this setting, as all unique numeric
	 * values in the domain are tested, and for continuous attributes these
	 * are many (likely about as much as (old) Subgroup.coverage)
	 * 
	 * NumericStrategy.ALL
	 * in a non-beam setting there is no use in including a Condition with
	 * a Column-Operator pair that already occurs in Subgroup.ConditionList
	 * in a beam setting this is not applicable
	 * assume (A>=1), (A>=2), (A>=3) are all valid, and score in that order
	 * a non-beam would evaluate all 3 combined with every Column.Operator
	 * on the next depth (except for 'A>=' that is)
	 * say the 9 Candidates (A>=[1,2,3] ^ B=[x,y,z])
	 * a beam of say size 1, would only take (A>=1) to the next depth
	 * where it may be combined with (A>=3), resulting in the top-scoring
	 * Candidate for that depth, as it scores better that any of the 9
	 * Candidates (A>=[1,2,3] ^ B=[x,y,z])
	 * now (A>=1 ^ A>=3) would be the seed for the next depth
	 * this is essentially (A>=3), and all ((A>=1 ^ A>=3) ^ other)
	 * combinations tested on the next depth, would have been tested on the
	 * previous depth as (A>=3 ^ other) if the beam were large enough to
	 * have included (A>=3) in the first place (as a non-beam would have
	 * done also)
	 * but the creation of (A>=1 ^ A>=3) is only possible when
	 * Column.Operator pairs, (here 'A>='), are created that already occur
	 * in Subgroup.ConditionList
	 * non-beams do not suffer from this, as they guarantee that for every
	 * Column-Operation pair all of its Column.values are tested and
	 * included (in a ALL setting)
	 * 
	 * NumericStrategy.INTERVALS
	 * there is no use in including the same Column.BETWEEN as the last
	 * Subgroup.ConditionList.Column.BETWEEN
	 * this would just create a ConditionList with the same Condition
	 * duplicated at the end
	 * although there may be less Subgroup members, the set of split-points
	 * for these members will be a subset of the set for the 'parent'
	 * Subgroup
	 * so a RealBaseIntervalCrosstable will be constructed with this subset
	 * as a result the same single point ConvexHulls will be constructed
	 * as on the previous depth, minus those for members that were removed
	 * on the previous depth, as they were outside the BEST_INTERVAL
	 * from this set of ConvexHulls the exact same concatenated result
	 * ConvexHull will be formed, yielding the same BEST_INTERVAL, and
	 * therefore the same new Subgroup.members
	 * TODO MM appears to be valid, check
	 * 
	 * === * Operator settings ===
	 * 
	 * NOMINAL Operator settings: EQUAL, ELEMENT_OF
	 * there is no use in including a Condition with a Column-EQUAL pair
	 * that already occurs in Subgroup.ConditionList
	 * this is true for all SearchSettings
	 * TODO MM ELEMENT_OF, inspect code, probably the same applies as for
	 * NumericStrategy.INTERVALS
	 * 
 	 * BINARY Operator settings: EQUAL
	 * there is no use in including a Condition with a Column-EQUAL pair
	 * that already occurs in Subgroup.ConditionList
	 * this is true for all SearchSettings
	 * 
	 * NUMERIC Operator settings: EQUAL, LEQ, GEQ, BETWEEN
	 * see NOTE above
	 * many combinations on a single Column do not make sense
	 * but the check should be in SubgroupDiscovery.evaluate*Refinements()
	 * 
	 * there is no use in including a Condition with a Column-EQUAL pair
	 * that already occurs in Subgroup.ConditionList
	 * this is true for all SearchSettings
	 * also see FIXME SubgroupDiscovery.evaluateNominalBinaryRefinement()
	 * 
	 * depending on the SearchStrategy and NumericOperatorSetting it may or
	 * may not be useless to create and evaluate some
	 * Column.Operator.value Refinements for LEQ and GEQ (and BETWEEN)
	 * part of these checks could be done at RefinementList creation time,
	 * and part of these can only be performed in the
	 * Subgroup.evaluateNumericRefinement(), based on the domain retrieved
	 * based on the 'old' Subgroup.members
	 * for example: (A>=1 ... ^ A>=2) is redundant in a non-beam ALL setting
	 * however it is valid in a beam setting (even though it may still be
	 * redundant, depending on what was in the beam, so it may be required)
	 */

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
