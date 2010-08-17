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

	public Subgroup(ConditionList theConditions, double theMeasureValue, int theCoverage, int theDepth, SubgroupSet theSubgroupSet)
	{
		itsConditions = theConditions;
		itsMeasureValue = theMeasureValue;
		itsCoverage = theCoverage;
		itsMembers = new BitSet(1000);
		itsDepth = theDepth;
		itsDAG = null;	//not set yet
		itsParentSet = theSubgroupSet;
	}

	public Subgroup(ConditionList theConditions, BitSet theMembers, int theDepth)
	{
		itsConditions = theConditions;
		itsMeasureValue = 0.0f;
		itsCoverage = theMembers.cardinality();
		itsMembers = theMembers;
		itsDepth = theDepth;
		itsDAG = null;
		itsParentSet = null;
	}

	public void addCondition(Condition theCondition)
	{
		itsConditions.addCondition(theCondition);
		itsDepth++;
	}

	//TODO: check correctheid van diepe copy
	public Object copy()
	{
		Subgroup aReturn = new Subgroup(itsConditions, itsMeasureValue, itsCoverage, itsDepth, itsParentSet);
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
								return -1; // NB. 0 causes a failure in adding A to RuleList
	}

	/**
	 * NOTE For now this equals implementation is only used for the ROCList
	 * HashSet implementation.
	 */
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Subgroup))
			return false;

		Subgroup s = (Subgroup) o;
		// TODO remove
		System.out.println("Comparing:\n");
		this.print();
		s.print();
		return getTruePositiveRate().equals(s.getTruePositiveRate()) &&
				getFalsePositiveRate().equals(s.getFalsePositiveRate());
	}

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
	@Override
	public int hashCode()
	{
		int hashCode = 0;
		for(Condition c : itsConditions.itsConditions)
			hashCode += (c.getAttribute().hashCode() + c.getOperatorString().hashCode());
		return 31*itsMembers.hashCode() + hashCode;
	}

	// TODO check if returned BitSet is null, ie. not set for SubgroupSet
	private Float getTruePositiveRate()
	{
		BitSet tmp = (BitSet)itsParentSet.getBinaryTarget();
		getMembers().and(tmp);
		int aHeadBody = tmp.cardinality();

		System.out.println("TPR: " + aHeadBody + " / " + itsParentSet.getTotalTargetCoverage());
		return aHeadBody / itsParentSet.getTotalTargetCoverage();
	}

	private Float getFalsePositiveRate()
	{
		BitSet tmp = (BitSet)itsParentSet.getBinaryTarget();
		getMembers().and(tmp);
		int aHeadBody = tmp.cardinality();

		System.out.println("FRP: (" + getCoverage() + " - " + aHeadBody +
							") / (" + itsParentSet.getTotalCoverage() +
							" - " + itsParentSet.getTotalTargetCoverage() + ")");
		return (getCoverage() - aHeadBody) / (itsParentSet.getTotalCoverage() - itsParentSet.getTotalTargetCoverage());
	}
}
