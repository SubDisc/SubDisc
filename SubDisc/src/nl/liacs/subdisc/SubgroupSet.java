package nl.liacs.subdisc;

import java.util.*;

/**
 * A SubgroupSet is a <code>TreeSet</code> of {@link Subgroup Subgroup}s. If its
 * size is set to <= 0, the SubgroupSet has no maximum size, else the number of
 * Subgroups it can contain is limited by its size. In a nominal target setting
 * ({@link nl.liacs.subdisc.TargetConcept.TargetType TargetType}) a
 * {@link ROCList ROCList} can be obtained from this SubgroupSet to create a
 * {@link nl.liacs.subdisc.gui.ROCCurve ROCCurve} in a
 * {@link nl.liacs.subdisc.gui.ROCCurveWindow ROCCurveWindow}.
 * @see ROCList
 * @see nl.liacs.subdisc.gui.ROCCurve
 * @see nl.liacs.subdisc.gui.ROCCurveWindow
 * @see Subgroup
 */
public class SubgroupSet extends TreeSet<Subgroup>
{
	private static final long serialVersionUID = 1L;
	private final int itsMaximumSize;

	// For SubgroupSet in nominal target setting (used for TPR/FPR in ROCList)
	private final boolean nominalTargetSetting;
	private final int itsTotalCoverage;
	private final float itsTotalTargetCoverage;
	private final BitSet itsBinaryTarget;

	/*
	 * SubgroupSets' other members are only used in a nominal target setting,
	 * but still set so the members can be final.
	 */
	/**
	 * Create a SubgroupSet of a certain size.
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no
	 * maximum size.
	 */
	public SubgroupSet(int theSize)
	{
		nominalTargetSetting = false;
		itsMaximumSize = theSize;
		itsTotalCoverage = -1;
		itsBinaryTarget = null;
		itsTotalTargetCoverage = -1;
	}

	/**
	 * Creates a SubgroupSet of a certain size, but in a nominal target setting
	 * theTotalCoverage and theBinaryTarget should also be set.
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no
	 * maximum size.
	 * @param theTotalCoverage the total number of instances in the data (number
	 * of rows in the {@link Table Table}).
	 * @param theBinaryTarget a <code>BitSet</code> with <code>bit</code>s set
	 * for the instances covered by the target value.
	 */
	public SubgroupSet(int theSize, int theTotalCoverage, BitSet theBinaryTarget)
	{
		nominalTargetSetting = true;
		itsMaximumSize = theSize;
		itsTotalCoverage = theTotalCoverage;
		itsBinaryTarget = theBinaryTarget;

		if (theTotalCoverage <= 0)
			Log.logCommandLine("SubgroupSet constructor(): theTotalCoverage = '"
								+ theTotalCoverage + "', but can not be <= 0");

		if (itsBinaryTarget == null)
		{
			itsTotalTargetCoverage = -1.0f;
			Log.logCommandLine("SubgroupSet constructor() ERROR");	// TODO
		}
		else
			itsTotalTargetCoverage = (float)theBinaryTarget.cardinality();
	}

	/**
	 * Tries to add the {@link Subgroup Subgroup} passed in as parameter to this
	 * SubgroupSet. Also ensures this SubgroupSet never exceeds its maximum size
	 * (if one is set).
	 * @param theSubgroup theSubgroup to add to this SubgroupSet.
	 * @return <code>true</code> if this SubgroupSet did not already contain the
	 * specified SubGroup, <code>false</code> otherwise and if the Subgroup is
	 * <code>null</code>.
	 */
	@Override
	public boolean add(Subgroup theSubgroup)
	{
		if (theSubgroup == null)
			return false;
		else
		{
			boolean aResult = super.add(theSubgroup);
			if ((itsMaximumSize > 0) && (size() > itsMaximumSize))
				remove(last());
	
			return aResult;
		}
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
			Log.logCommandLine(String.format("%d,%d,%d",
												s.getID(),
												s.getCoverage(),
												s.getMeasureValue()));
	}

	/*
	 * ROCList functions.
	 * TODO update a single ROCList instance?
	 */
	/**
	 * Returns a <b>copy of</b> this SubgroupSets' BinaryTarget
	 * <code>BitSet</code>. SubgroupSets only have a BinaryTarget
	 * <code>BitSet<code> in a nominal target setting, meaning the
	 * {@link nl.liacs.subdisc.Attribute.AttributeType AttributeType} of the
	 * PrimaryTarget in the {@link TargetConcept TargetConcept} is of type
	 * AttributeType.NOMINAL.
	 * 
	 * @return a clone of this SubgroupSets' BinaryTarget <code>BitSet</code>,
	 * or <code>null</code> if this SubgroupSet has no BinaryTarget
	 * <code>BitSet</code>.
	 */
	public BitSet getBinaryTargetClone()
	{
		if (!nominalTargetSetting || itsBinaryTarget == null)
			return null;
		else
			return (BitSet) itsBinaryTarget.clone();
	}
	public int getTotalCoverage() { return itsTotalCoverage; }
	public float getTotalTargetCoverage() { return itsTotalTargetCoverage; }

	/**
	 * Returns a new {@link ROCList ROCList}. If {@link Subgroup Subgroup}s are
	 * removed from this SubgroupSet, this new ROCList reflects these changes.
	 * This method only returns a ROCList in a nominal target setting, meaning
	 * the {@link nl.liacs.subdisc.Attribute.AttributeType AttributeType} of the
	 * PrimaryTarget in the {@link TargetConcept TargetConcept} is of type
	 * AttributeType.NOMINAL.
	 * 
	 * @return a ROCList, or <code>null</code> if not in a nominal target
	 * setting.
	 */
	public ROCList getROCList()
	{
		if (!nominalTargetSetting || itsBinaryTarget == null)
			return null;
		else
			return new ROCList(new ArrayList<Subgroup>(this));
	}
}
