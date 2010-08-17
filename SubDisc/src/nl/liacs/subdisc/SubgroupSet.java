package nl.liacs.subdisc;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

public class SubgroupSet extends TreeSet<Subgroup>
{
	private static final long serialVersionUID = 1L;

	private int itsMaximumSize = -1; // no maximum

	// for subgroupset in nominal target setting (used for TPR/FPR)
	private final int itsTotalCoverage;
	private final float itsTotalTargetCoverage;
	private final BitSet itsBinaryTarget;
	private HashSet<Subgroup> itsROCList;

	public SubgroupSet(int theSize, int theTotalCoverage, BitSet theBinaryTarget)
	{
		itsMaximumSize = theSize;
		itsTotalCoverage = theTotalCoverage;
		itsBinaryTarget = theBinaryTarget;
		itsTotalTargetCoverage = (float)theBinaryTarget.cardinality();
	}

	public SubgroupSet(int theSize)
	{
		itsMaximumSize = theSize;
		itsTotalCoverage = -1;
		itsBinaryTarget = new BitSet(0);
		itsTotalTargetCoverage = -1;
	}

	public boolean add(Subgroup theSubgroup)
	{
		boolean aResult = super.add(theSubgroup);
		if (aResult)
			trimQueue();

		return aResult;
	}

	public void trimQueue()
	{
		if (itsMaximumSize < 0) // no maximum
			return;
		while (size() > itsMaximumSize)
		{
			Iterator<Subgroup> anIterator = iterator();
			while (anIterator.hasNext())
				anIterator.next();
			anIterator.remove();
		}
	}

	public Subgroup getBestSubgroup() { return first(); }

	public void setIDs()
	{
		int aCount = 1;
		Iterator<Subgroup> anIterator = this.iterator();
		while (anIterator.hasNext())
		{
			Subgroup aSubgroup = anIterator.next();
			aSubgroup.setID(aCount);
			aCount++;
		}
	}

	public void print()
	{
		for (Subgroup aSubgroup : this)
			Log.logCommandLine("" + aSubgroup.getID() + "," + aSubgroup.getCoverage() + "," + aSubgroup.getMeasureValue());
	}

	/**
	 * ROCList HashSet functions
	 */
	// TODO null-safe?
	public BitSet getBinaryTarget()
	{
		return (itsBinaryTarget == null) ? null : (BitSet) itsBinaryTarget.clone();
	}

	public float getTotalCoverage()
	{
		return itsTotalCoverage;
	}

	public float getTotalTargetCoverage()
	{
		return itsTotalTargetCoverage;
	}

	public HashSet<Subgroup> getROCList()
	{
		if(itsROCList == null)
			itsROCList = new HashSet<Subgroup>(this);

		return new HashSet<Subgroup>(itsROCList);
	}

}
