package nl.liacs.subdisc;

import java.util.BitSet;

public class Subgroup implements Comparable<Object>
{
	private ConditionList itsConditions;
	private BitSet itsMembers;
	private int itsID = 0;
	private int itsCoverage;
	private DAG itsDAG;
	private double itsMeasureValue;
	int itsDepth;
	private final SubgroupSet itsParentSet;

	/**
	 * Create a Subgroup, called by SubgroupDiscovery.Mine() and via
	 * MiningWindow.jButtonRandomSubgroupsActionPerformed().
	 * In case no SubgroupSet is provided an empty one is created,
	 * this avoids extra checks in eg. getFalsePositiveRate().
	 * TODO null check ConditionList
	 * @param theMeasureValue
	 * @param theCoverage
	 * @param theDepth
	 * @param theSubgroupSet
	 */
	public Subgroup(double theMeasureValue, int theCoverage, int theDepth, SubgroupSet theSubgroupSet)
	{
		itsConditions = new ConditionList();
		itsMeasureValue = theMeasureValue;
		itsCoverage = theCoverage;
		itsMembers = new BitSet(1000);
		itsDepth = theDepth;
		itsDAG = null;	//not set yet
		itsParentSet = (theSubgroupSet == null ? new SubgroupSet(0) : theSubgroupSet);
	}

	/**
	 * Create a Subgroup, called by MiningWindow.jButtonRandomConditionsActionPerformed()
	 * Most of subgroups' other members are still set, this avoids extra checks
	 * in eg. getFalsePositiveRate().
	 * TODO null check theConditions
	 * @param theConditions
	 * @param theMembers
	 * @param theDepth
	 */
	public Subgroup(ConditionList theConditions, BitSet theMembers, int theDepth)
	{
		itsConditions = (theConditions == null ? new ConditionList() : theConditions);
		itsMeasureValue = 0.0f;
		itsMembers = (theMembers == null ? new BitSet(0) : theMembers);
		itsCoverage = theMembers.cardinality();
		itsDepth = theDepth;
		itsDAG = null;	//not set yet
		itsParentSet = new SubgroupSet(0);
	}

	public void addCondition(Condition theCondition)
	{
		itsConditions.addCondition(theCondition);
		itsDepth++;
	}

	//TODO: check correctheid van diepe copy
	public Object copy()
	{
		Subgroup aReturn = new Subgroup(itsMeasureValue, itsCoverage, itsDepth, itsParentSet);
		aReturn.itsConditions = itsConditions.copy();
		aReturn.itsMembers = (BitSet) itsMembers.clone();
		return (Object)aReturn;
	}

	public void print()
	{
		Log.logCommandLine("conditions: " + itsConditions.toString());
		Log.logCommandLine("bitset: " + itsMembers.toString());
	}

	public BitSet getMembers() { return itsMembers; }

	public void setMembers(BitSet theMembers)
	{
		itsMembers = theMembers;
		itsCoverage = theMembers.cardinality();
	}

	public int getID() { return itsID; }
	public void setID(int theID) { itsID = theID; }

	public double getMeasureValue() { return itsMeasureValue; }
	public void setMeasureValue(double theMeasureValue) { itsMeasureValue = theMeasureValue; }

	public void setDAG(DAG theDAG) { itsDAG = theDAG; }
	public DAG getDAG() { return itsDAG; }

	public int getCoverage() { return itsCoverage; }

	public ConditionList getConditions() { return itsConditions;  }
	public int getNrConditions() { return itsConditions.size();  }

	public int getDepth() { return itsDepth; }

	public int compareTo(Object o)
	{
		if(!(o instanceof Subgroup) || (o == null))
			return 1;

		Subgroup s = (Subgroup) o;
		
		if(getMeasureValue() > s.getMeasureValue())
			return -1;
		else if(getMeasureValue() < s.getMeasureValue())
			return 1;
		else if(getCoverage() > s.getCoverage())
			return -1;
		else if(getCoverage() < s.getCoverage())
			return 1;
		else if(itsConditions.itsConditions.size() > s.itsConditions.itsConditions.size())
			return -1;
		else if(itsConditions.itsConditions.size() < s.itsConditions.itsConditions.size())
			return 1;

		for(Condition c : itsConditions.itsConditions)
		{
			boolean hasSameAttributeAndOperator = false;
			for(Condition sc : s.itsConditions.itsConditions)
			{
				if(c.getAttribute().getName().equals(sc.getAttribute().getName()) &&
						c.getOperatorString().equals(sc.getOperatorString()))
				{
					hasSameAttributeAndOperator = true;
					break;
				}
			}
			if(!hasSameAttributeAndOperator)
				return 1;	// TODO arbitrary, could have been -1 also
		}

		return 0;
//		return itsMembers.equals(s.itsMembers);
	}
/*
	public int compareTo(Object theObject)
	{
		Subgroup A = this;
		Subgroup B = (Subgroup)theObject;	// TODO null safe?

		if (A.getMeasureValue() > B.getMeasureValue())
			return -1;
		else
			if (A.getMeasureValue() < B.getMeasureValue())
				return 1;
			else
				if (A.getCoverage() > B.getCoverage())
					return -1;
				else
					if (A.getCoverage() < B.getCoverage())
						return 1;
							else
								return -1; // TODO set to -1, 0 does not work as expected
	}
*/
	/**
	 * NOTE For now this equals implementation is only used for the ROCList
	 * HashSet implementation.
	 * Two subgroups are considered equal if:
	 * for each condition(Attribute-Operator pair) in this.conditionList there
	 * is a matching condition(Attribute-Operator pair) in other.conditionList
	 * and both itsMembers are equal.
	 */
/*
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Subgroup))
			return false;

		Subgroup s = (Subgroup) o;

		for(Condition c : itsConditions.itsConditions)
		{
			boolean hasSameAttributeAndOperator = false;
			for(Condition sc : s.itsConditions.itsConditions)
			{
				if(c.getAttribute().getName().equalsIgnoreCase(sc.getAttribute().getName()) &&
						c.getOperatorString().equalsIgnoreCase(sc.getOperatorString()))
				{
					hasSameAttributeAndOperator = true;
					System.out.println(this.getID()+ " " + s.getID());
					this.print();
					s.print();
					break;
				}
			}
			if(!hasSameAttributeAndOperator)
				return false;
		}

		return itsMembers.equals(s.itsMembers);
		//getTruePositiveRate().equals(s.getTruePositiveRate()) &&
			//	getFalsePositiveRate().equals(s.getFalsePositiveRate());
	}
*/
	/**
	 * TODO Even for the SubgroupSet.getROCList code this is NOT enough.
	 * All subgroups are from the same SubgroupSet/ experiment with the same target.
	 * However, two subgroups formed from different Attributes in itsConditions
	 * should be considered unequal. This requires an @Override from itsConditions
	 * hashCode(), as it should not include condition values. 
	 * Eg. two subgroups that have the same members and are formed from:
	 * (x < 10) and (x < 11) should be considered equal
	 * (y < 10) and (x < 10) should be considered different 
	 */
/*
	@Override
	public int hashCode()
	{
		int hashCode = 0;
		for(Condition c : itsConditions.itsConditions)
			hashCode += (c.getAttribute().getName().hashCode() + c.getOperatorString().hashCode());
		return 31*itsMembers.hashCode() + hashCode;
	}
*/
	/**
	 * If no itsParentSet was set for this subgroup or no itsBinaryTarget was
	 * set for this subgroups' itsParentSet this function return 0.0F.
	 * @return TruePositiveRate aka. TPR
	 */
	public Float getTruePositiveRate()
	{
		BitSet tmp = itsParentSet.getBinaryTarget();
		tmp.and(getMembers());
		int aHeadBody = tmp.cardinality();
//		print();
//		System.out.println("TPR: " + aHeadBody + " / " + itsParentSet.getTotalTargetCoverage());
		return aHeadBody / itsParentSet.getTotalTargetCoverage();
	}

	/**
	 * If no itsParentSet was set for this subgroup or no itsBinaryTarget was
	 * set for this subgroups' itsParentSet this function return 0.0F.
	 * @return FalsePositiveRate aka. FPR
	 */
	public Float getFalsePositiveRate()
	{
		BitSet tmp = (BitSet)itsParentSet.getBinaryTarget();
		tmp.and(getMembers());
		int aHeadBody = tmp.cardinality();
//		print();
//		System.out.println("FRP: (" + getCoverage() + " - " + aHeadBody +
//							") / (" + itsParentSet.getTotalCoverage() +
//							" - " + itsParentSet.getTotalTargetCoverage() + ")");
		return (getCoverage() - aHeadBody) / (itsParentSet.getTotalCoverage() - itsParentSet.getTotalTargetCoverage());
	}
}
