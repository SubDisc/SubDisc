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

	public Subgroup(ConditionList theConditions, double theMeasureValue, int theCoverage, int theDepth)
	{
		itsConditions = theConditions;
		itsMeasureValue = theMeasureValue;
		itsCoverage = theCoverage;
		itsMembers = new BitSet(1000);
		itsDepth = theDepth;
		itsDAG = null;	//not set yet
	}

	public Subgroup(ConditionList theConditions, BitSet theMembers, int theDepth)
	{
		itsConditions = theConditions;
		itsMeasureValue = 0.0f;
		itsCoverage = theMembers.cardinality();
		itsMembers = theMembers;
		itsDepth = theDepth;
		itsDAG = null;
	}

	public void addCondition(Condition theCondition)
	{
		itsConditions.addCondition(theCondition);
		itsDepth++;
	}

	//TODO: check correctheid van diepe copy
	public Object copy()
	{
		Subgroup aReturn = new Subgroup(itsConditions, itsMeasureValue, itsCoverage, itsDepth);
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
		Subgroup B = (Subgroup)theObject;

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
}