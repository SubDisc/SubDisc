package nl.liacs.subdisc;

import java.util.*;

/**
 * A SubgroupSet is a TreeSet of {@link Subgroup Subgroup}s. If the size is set
 * to <= 0, the SubgroupSet has no maximum size, else the number of Subgroups it
 * can contain is limited by its size. In a nominal target setting
 * ({@link TargetType TargetType}) a {@link ROCList ROCList} can be obtained
 * from this SubgroupSet to create a {@link ROCCurve ROCCurve} in a
 * {@link ROCCurveWindow ROCCurveWindow}.
 * @see ROCList
 * @see ROCCurve
 * @see ROCCurveWindow
 * @see Subgroup
 */
public class SubgroupSet extends TreeSet<Subgroup>
{
	private static final long serialVersionUID = 1L;
	private final int itsMaximumSize;

	// For SubgroupSet in nominal target setting (used for TPR/FPR in ROCList)
	private final int itsTotalCoverage;
	private final float itsTotalTargetCoverage;
	private final BitSet itsBinaryTarget;

	/*
	 * SubgroupSets' other members are only used in a nominal target setting,
	 * but still set to avoid nulls.
	 */
	/**
	 * Create a SubgroupSet of a certain size.
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no
	 * maximum size.
	 */
	public SubgroupSet(int theSize)
	{
		itsMaximumSize = theSize;
		itsTotalCoverage = -1;
		itsBinaryTarget = new BitSet(0);
		itsTotalTargetCoverage = -1;
	}

	// TODO check if itsTotalCoverage / itsTotalTargetCoverage > 0
	/**
	 * Create a SubgroupSet of a certain size, but in a nominal target setting
	 * theTotalCoverage and theBinaryTarget should also be set.
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no
	 * maximum size.
	 * @param theTotalCoverage the total number of instances in the data (number
	 * of rows in the {@link Table Table}). 
	 * @param theBinaryTarget a BitSet with bits set for the instances covered
	 * by the target value.
	 */
	public SubgroupSet(int theSize, int theTotalCoverage, BitSet theBinaryTarget)
	{
		itsMaximumSize = theSize;
		itsTotalCoverage = theTotalCoverage;
		itsBinaryTarget = (theBinaryTarget == null ? new BitSet(0) : theBinaryTarget);
		itsTotalTargetCoverage = (float)theBinaryTarget.cardinality();
	}

	/**
	 * Tries to add the Subgroup passed in as parameter to this SubgroupSet.
	 * Also ensures this SubgroupSet never exceeds its maximum size (if one is
	 * set).
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

	/*
	 * ROCList functions.
	 * TODO update a single ROCList instance?
	 */
	public BitSet getBinaryTarget() { return (BitSet) itsBinaryTarget.clone(); }
	public int getTotalCoverage() { return itsTotalCoverage; }
	public float getTotalTargetCoverage() { return itsTotalTargetCoverage; }

	/* TODO if itsTotalTargetCoverage/itsTotalCoverage == -1 or
	 * itsBinaryTarget.cardinality == 0, not a nominal target. Abort.
	 */
	/**
	 * Always returns a new ROCList. If Subgroups are removed from this
	 * SubgroupSet, this new ROCList reflects these changes.
	 */
	public ROCList getROCList() { return new ROCList(new ArrayList<Subgroup>(this)); }
}
