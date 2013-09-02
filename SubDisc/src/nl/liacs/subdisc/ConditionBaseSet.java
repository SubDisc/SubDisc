package nl.liacs.subdisc;

import java.util.*;

/*
 * Class that creates the base conditions just once.
 * After that, Refinement(List)s are created using different Subgroups, but
 * identical Conditions added to those.
 * To avoid any accidental overwriting / reuse of Conditions, copy() returns a
 * (semi-) deep-copy of the list of base Conditions.
 * (Note, Condition.itsColumns / .itsOperator are final, but 'itsValue' is not.)
 * 
 * This class is an improvement over the old RefinementList strategy, as that
 * recreated the RefinementList over and over again.
 * In the process, many invalid Conditions were created,and renounced.
 * This code does not create invalid Conditions.
 * A RefinementList need not be recreates and rechecked for each Candidate.
 * 
 * The copy() is not even needed at all, but fits better in current code.
 * No base Condition is ever used directly, only the refined derivatives are.
 * (Meaning Refinement.getRefinedSubgroup() does an internal Condition.copy().)
 */
class ConditionBaseSet
{
	private final List<Condition> itsConditions;

	ConditionBaseSet(Table theTable, SearchParameters theSearchParameters)
	{
		itsConditions = Collections.unmodifiableList(getBaseConditions(theTable, theSearchParameters));
	}

	// NOTE copy() is not strictly needed, as underlying Conditions are not
	// modified in any current code
	List<Condition> copy()
	{
		List<Condition> result = new ArrayList<Condition>(itsConditions.size());
		for (Condition c : itsConditions)
			result.add(c.copy());
		return Collections.unmodifiableList(result);
	}

	private static final List<Condition> getBaseConditions(Table theTable, SearchParameters theSearchParameters)
	{
		TargetConcept aTC = theSearchParameters.getTargetConcept();

		// set-valued only allowed for SINGLE_NOMINAL
		boolean isSingleNominalTT = (aTC.getTargetType() == TargetType.SINGLE_NOMINAL);
		boolean useSets = theSearchParameters.getNominalSets();
		Operator aNomOp = (isSingleNominalTT && useSets) ?
					Operator.ELEMENT_OF : Operator.EQUALS;
		EnumSet<Operator> aNumOps = theSearchParameters.getNumericOperatorSetting().getOperators();

		// overestimate, not all columns are numeric description columns
		int init = theTable.getNrColumns() * aNumOps.size();
		List<Condition> result = new ArrayList<Condition>(init);

		for (Column c : theTable.getColumns())
		{
			// ignore
			if (!c.getIsEnabled() || aTC.isTargetAttribute(c))
				continue;

			switch (c.getType())
			{
				case NOMINAL :
				{
					result.add(new Condition(c, aNomOp));
					break;
				}
				case NUMERIC :
				{
					for (Operator o : aNumOps)
						result.add(new Condition(c, o));
					break;
				}
				// no fall-through, ORDINAL is not implemented
				case ORDINAL :
				{
					throw new AssertionError(c.getType());
				}
				case BINARY :
				{
					// assumes just 1 BINARY Operator
					result.add(new Condition(c, Operator.EQUALS));
					break;
				}
				default :
					throw new AssertionError(c.getType());
			}
		}

		return result;
	}
}
