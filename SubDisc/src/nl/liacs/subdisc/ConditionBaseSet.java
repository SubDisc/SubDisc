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
	private final List<ConditionBase> itsConditionBases;
	private final int itsSize;

	ConditionBaseSet(Table theTable, SearchParameters theSearchParameters)
	{
		itsConditionBases = Collections.unmodifiableList(getBaseConditions(theTable, theSearchParameters));
		itsSize = itsConditionBases.size();
	}

	/*
	 * safe as itsConditionBases is unmodifiable, and a ConditionBase
	 * contains only two final fields, Column and Operator
	 * this is not to say nothing can maliciously be broken, if a
	 * Column.AttributeType is changed, the accompanying Operator may no
	 * longer be valid
	 */
	final List<ConditionBase> getConditionBases() { return itsConditionBases; }

	final int size() { return itsSize; }

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(itsSize * 32);
		sb.append(this.getClass().getSimpleName()).append("\n");

		for (ConditionBase c : itsConditionBases)
			sb.append("\t").append(c.toString()).append("\n");

		return sb.toString();
	}

//	// for depth_first, leads to some duplicate testing, but OK for now
//	List<Condition> copyFrom(Condition theCondition)
//	{
//		final Column aColumn = theCondition.getColumn();
//		final Operator anOperator = theCondition.getOperator();
//
//		/*
//		 * start index is NOT at least aColumn.index, as disabled
//		 * Columns are not included in itsConditions
//		 */
//		for (int i = 0, j = itsConditions.size(); i < j; ++i)
//		{
//			Condition c = itsConditions.get(i);
//			if (c.getColumn() != aColumn)
//				continue;
//			if (c.getOperator() != anOperator)
//				continue;
//
//			// same Column and same Operator, use everything after i
//			List<Condition> result = new ArrayList<Condition>(j - i);
//			while (i < j)
//				result.add(itsConditions.get(i++));
//			return Collections.unmodifiableList(result);
//		}
//
//		// should never happen
//		throw new AssertionError(theCondition);
//	}
//
//	// for true depth_first (n*(n-1))/2, domain needs to be checked from
//	// last Condition.value, this check would go into evaluate*Refinement
//	// but is to intrusive to test at the moment
//	List<Condition> copyBeyond(Condition theCondition)
//	{
//		final Column aColumn = theCondition.getColumn();
//		final Operator anOperator = theCondition.getOperator();
//
//		/*
//		 * start index is NOT at least aColumn.index, as disabled
//		 * Columns are not included in itsConditions
//		 */
//		for (int i = 0, j = itsConditions.size(); i < j; ++i)
//		{
//			Condition c = itsConditions.get(i);
//			if (c.getColumn() != aColumn)
//				continue;
//			if (c.getOperator() != anOperator)
//				continue;
//
//			// same Column and same Operator, use everything after i
//			List<Condition> result = new ArrayList<Condition>(j - i - 1);
//			while (++i < j)
//				result.add(itsConditions.get(i));
//			return Collections.unmodifiableList(result);
//		}
//
//		// should never happen
//		throw new AssertionError(theCondition);
//	}

	private static final List<ConditionBase> getBaseConditions(Table theTable, SearchParameters theSearchParameters)
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
		List<ConditionBase> result = new ArrayList<ConditionBase>(init);

		for (Column c : theTable.getColumns())
		{
			// ignore
			if (!c.getIsEnabled() || aTC.isTargetAttribute(c))
				continue;

			switch (c.getType())
			{
				case NOMINAL :
				{
					result.add(new ConditionBase(c, aNomOp));
					break;
				}
				case NUMERIC :
				{
					for (Operator o : aNumOps)
						result.add(new ConditionBase(c, o));
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
					result.add(new ConditionBase(c, Operator.EQUALS));
					break;
				}
				default :
					throw new AssertionError(c.getType());
			}
		}

		return result;
	}
}
