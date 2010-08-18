package nl.liacs.subdisc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.TreeSet;

public class SubgroupSet extends TreeSet<Subgroup>
{
	private static final long serialVersionUID = 1L;

	private int itsMaximumSize = -1; // no maximum

	// for subgroupset in nominal target setting (used for TPR/FPR in ROCList)
	private final int itsTotalCoverage;
	private final float itsTotalTargetCoverage;
	private final BitSet itsBinaryTarget;

	public SubgroupSet(int theSize, int theTotalCoverage, BitSet theBinaryTarget)
	{
		itsMaximumSize = theSize;
		itsTotalCoverage = theTotalCoverage;
		itsBinaryTarget = theBinaryTarget;
		itsTotalTargetCoverage = (float)theBinaryTarget.cardinality();
	}

	// TODO other members are set to avoid nulls
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
			Log.logCommandLine(aSubgroup.getID() + "," + aSubgroup.getCoverage() + "," + aSubgroup.getMeasureValue());
	}

	/**
	 * ROCList functions
	 * getROCList return a new ROCList each time its called, if subgroups are 
	 * removed from the SubgroupSet the new ROCList reflects these changes
	 * TODO update single ROCList instance?
	 */
	public BitSet getBinaryTarget() { return (BitSet) itsBinaryTarget.clone(); }
	public int getTotalCoverage() { return itsTotalCoverage; }
	public float getTotalTargetCoverage() { return itsTotalTargetCoverage; }
	public ROCList getROCList() { return new ROCList(new ArrayList<Subgroup>(this)); }
}
