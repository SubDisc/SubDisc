package nl.liacs.subdisc;

import java.util.Iterator;
import java.util.TreeSet;

public class SubgroupSet extends TreeSet<Subgroup>
{
	private static final long serialVersionUID = 1L;

	private int itsMaximumSize = -1; // no maximum

	public SubgroupSet(int theSize)
	{
		itsMaximumSize = theSize;
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
}