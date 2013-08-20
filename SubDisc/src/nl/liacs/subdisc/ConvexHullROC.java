package nl.liacs.subdisc;

import java.util.*;

/*
 * ConvexHull class for ROC convex hull, bounded by [0, 1] in x and y direction.
 * More general ConvexHull class for 'free' coordinates will follow.
 * 
 * Assumes not so many points are on the hull: linear ArrayList is good enough.
 * Lookup could be log(n) when using Tree, but construct is more expensive.
 */
public class ConvexHullROC extends ArrayList<SubgroupROCPoint>
{
	private static final long serialVersionUID = 1L;
	private static final int NO_INSERT = -1;
	private static final int INSERT_NO_UPDATE = -2;

	ConvexHullROC() {}

	ConvexHullROC(SubgroupSet theSubgroupSet)
	{
		for (Subgroup s : theSubgroupSet)
			add(new SubgroupROCPoint(s));
	}

	@Override
	public boolean add(SubgroupROCPoint thePoint)
	{
		// ROC specific check
		if (!isValid(thePoint))
			throw new IllegalArgumentException(thePoint.toString());

		synchronized(this)
		{
			int i = insertionIndex(thePoint);
			if (i == NO_INSERT)
				return false;

			update(i);
		}

		return true;
	}

	private static final boolean isValid(PointDouble thePoint)
	{
		return isValid(thePoint.x) && isValid(thePoint.y);
	}

	// hard-coded bounds [0, 1]
	private static final boolean isValid(double theNumber)
	{
		// -0.0 is not valid, check behaves correctly on NaN
		return (theNumber >= 0.0) && (theNumber <= 1.0);
	}

	// assumes synchronized(this)
	private int insertionIndex(SubgroupROCPoint thePoint)
	{
		if (this.isEmpty())
		{
			super.add(thePoint);
			return INSERT_NO_UPDATE;
		}

		for (int i = this.size()-1; i >= 0; --i)
		{
			SubgroupROCPoint s = this.get(i);

			if (thePoint.x < s.x)
				continue;
			else if (thePoint.x == s.x)
			{
				if (thePoint.y < s.y)
					return NO_INSERT;
				else if (thePoint.y == s.y)
				{
					this.add(i+1, thePoint);
					return INSERT_NO_UPDATE;
				}
				else // (thePoint.y > s.y), no NaNs by construction
				{
					// remove s
					this.set(i, thePoint);
					return i;
				}
			}
			else // (thePoint.x > s.x), no NaNs by construction
			{
				// return after first time (thePoint.x > s.x)
				return i+1;
			}
		}

		// put thePoint all the way in front
		this.add(0, thePoint);
		return 0;
	}

	private void update(int index)
	{
		
	}

	/**
	 * Compares FPR, when equal, compares TPR, when equal, compares Subgroup
	 * ID.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
/*
	@Override
	public int compareTo(SubgroupROCPoint other)
	{
		if (this == other)
			return 0;

		// safe for NaN
		int cmp = Double.compare(this.x, other.x);
		if (cmp != 0)
			return cmp;

		cmp = Double.compare(this.y, other.y);
		return cmp != 0 ? cmp : this.ID - other.ID;

		subgroup IDs are only set after mining run finishes
	}
*/
}
