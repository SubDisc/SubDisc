package nl.liacs.subdisc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.TreeSet;

public class SubgroupSet extends TreeSet<Subgroup>
{
	private static final long serialVersionUID = 1L;
	private final int itsMaximumSize;

	// For subgroupset in nominal target setting (used for TPR/FPR in ROCList)
	private final int itsTotalCoverage;
	private final float itsTotalTargetCoverage;
	private final BitSet itsBinaryTarget;

	/**
	 * Create a SubgroupSet of a certain size. In a nominal target setting
	 * theTotalCoverage and theBianryTarget should also be set.
	 * @param theSize, use theSize <= 0 for no maximum size
	 * @param theTotalCoverage
	 * @param theBinaryTarget
	 */
	public SubgroupSet(int theSize, int theTotalCoverage, BitSet theBinaryTarget)
	{
		itsMaximumSize = theSize;
		itsTotalCoverage = theTotalCoverage;
		itsBinaryTarget = (theBinaryTarget == null ? new BitSet(0) : theBinaryTarget);
		itsTotalTargetCoverage = (float)theBinaryTarget.cardinality();
	}

	/**
	 * Create a SubgroupSet of a certain size.
	 * SubgroupSets' other members are only used in a nominal target setting,
	 * but still set to avoid nulls.
	 * @param theSize, use theSize <= 0 for no maximum size
	 */
	public SubgroupSet(int theSize)
	{
		itsMaximumSize = theSize;
		itsTotalCoverage = -1;
		itsBinaryTarget = new BitSet(0);
		itsTotalTargetCoverage = -1;
	}

	/**
	 * Try to add theSubgroup to the SubgroupSet. Then check if the SubgroupSet
	 * did not exceed itsMaximumSize. If it does, remove the last Subgroup.
	 */
	@Override
	public boolean add(Subgroup theSubgroup)
	{
		boolean aResult = super.add(theSubgroup);
		if((itsMaximumSize > 0) && (size() > itsMaximumSize))
			remove(last());

		return aResult;
	}

	public Subgroup getBestSubgroup() { return first(); }

	public void setIDs()
	{
		int aCount = 1;
		for(Subgroup s : this)
			s.setID(aCount++);
	}

	public void print()
	{
		for (Subgroup s : this)
			Log.logCommandLine(s.getID() + "," + s.getCoverage() + "," + s.getMeasureValue());
	}

	/**
	 * ROCList functions.
	 * getROCList() return a new ROCList each time it is called. If subgroups
	 * are removed from the SubgroupSet the new ROCList reflects these changes.
	 * TODO update single ROCList instance?
	 */
	public BitSet getBinaryTarget() { return (BitSet) itsBinaryTarget.clone(); }
	public int getTotalCoverage() { return itsTotalCoverage; }
	public float getTotalTargetCoverage() { return itsTotalTargetCoverage; }
	public ROCList getROCList() { return new ROCList(new ArrayList<Subgroup>(this)); }
}
