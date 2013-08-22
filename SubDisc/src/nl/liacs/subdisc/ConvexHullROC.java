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

	private static final PointDouble ZERO_ZERO = new PointDouble(0.0, 0.0);
	private static final PointDouble ONE_ONE = new PointDouble(1.0, 1.0);

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
// XXX debug, get a feeling of how may points are on the hull
System.out.println("ROC Hull size: " + this.size());
			int i = insertionIndex(thePoint);
			if (i == NO_INSERT)
				return false;
			else if (i == INSERT_NO_UPDATE)
				return true;
			// check if point is on or above existing hull
			else if (!validInsertion(i))
			{
				this.remove(i);
				return false;
			}
			else
			{
				update(i);
				return true;
			}
		}
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
				this.add(i+1, thePoint);
				return i+1;
			}
		}

		// thePoint.x smaller than all other points, put it in front
		this.add(0, thePoint);
		return 0;
	}

	/*
	 * assumes synchronized(this)
	 * l is left neighbour of index i, r is right neighbour
	 * if slope(l, i) >= slope(l, r), insertion of index was valid 
	 */
	private boolean validInsertion(int index)
	{
		PointDouble l = (index == 0) ? ZERO_ZERO : this.get(index-1);
		PointDouble r = (index == this.size()-1) ? ONE_ONE : this.get(index+1);
		double lr = slope(l, r);
		double li = slope(l, this.get(index));

		return li >= lr;
	}

	/*
	 * assumes synchronized(this)
	 * index is position of last inserted value
	 * update left and right part of hull separately
	 * all is done in a synchronized(this) context, so all other threads are
	 * waiting for this one to finish
	 */
	// TODO MM updateRight first, updateLeft after
	private void update(int index)
	{
		// not first point
		if (index > 0)
			updateLeft(index);

		// not last point
		if (index < this.size()-1)
			updateRight(index);
	}

	/*
	 * assumes synchronized(this)
	 * all points l left of index that have a l.slope < i.slope can be
	 * removed
	 * since hull is convex, every l up to index can be removed as soon as a
	 * l.slope < index.slope
	 */
	private void updateLeft(int index)
	{
		SubgroupROCPoint p = this.get(index);
		double ps = slope(ZERO_ZERO, p);

		for (int i = 0; i < index; ++i)
		{
			SubgroupROCPoint q = this.get(i);
			double qs = slope(ZERO_ZERO, q);

			if (qs < ps)
			{
				this.removeRange(i, index);
				return;
			}
		}
	}

	/*
	 * assumes synchronized(this)
	 * uses 'local slope'
	 */
	private void updateRight(int index)
	{
		// simple check first
		updateRightTPR(index);

		// not last point
		if (index < this.size()-1)
			updateRightSlope(index);
	}

	// assumes synchronized(this)
	private void updateRightTPR(int index)
	{
		SubgroupROCPoint p = this.get(index);
		double pTPR = p.y;

		for (int i = this.size()-1; i > index; --i)
		{
			SubgroupROCPoint q = this.get(i);
			double qTPR = q.y;

			if (qTPR < pTPR)
			{
				this.removeRange(index+1, i+1);
				return;
			}
			// no == check
		}
	}

	/*
	 * assumes synchronized(this)
	 * NOTE index is NOT the last point
	 */
	private void updateRightSlope(int index)
	{
		PointDouble p = this.get(index);
		int ir = this.size()-1;
		PointDouble rr = ONE_ONE;

		do {
			PointDouble r = this.get(ir);
			double sr = slope(p, r);
			double srr = slope(p, rr);

			if (sr < srr)
			{
				this.removeRange(index+1, ir+1);
				return;
			}

			rr = r;
		}
		while (--ir > index);
	}

	// assumes a.x <= b.x, a.y <= b.y
	private static final double slope(PointDouble a, PointDouble b)
	{
		return (b.y - a.y) / (b.x - a.x);
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
