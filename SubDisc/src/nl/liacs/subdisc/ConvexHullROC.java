package nl.liacs.subdisc;

import java.util.*;

/*
 * Class does not extend ArrayList<?>, hides storage class for points.
 * Avoids the need to overwrite all of the (Array)List methods to ensure proper
 * internal state.
 * Also allows using different storage class without need for API changes.
 * 
 * Strictly convex would require all internal angles to be < 180.
 * Meaning that only the end points of a line would be kept, points on the line
 * would be ignored.
 * ROCList seems to do this, which is fine for its original drawing purposes.
 * 
 * The sole boolean parameter controls whether duplicates are allowed.
 * Duplicates are subgroups for which the (FPR, TPR) are exactly the same.
 * Duplicates do not alter the form of the hull.
 * ROCList does not allow duplicates, and keeps just the last of equal points
 * added, this is fine for its original drawing purposes.
 * 
 * Note, the special points (0, 0) and (1, 1), representing the empty subgroup
 * and all data respectively, are implicitly assumed to be on the hull.
 * The calculation for AUC depends on this.
 * However, they will not be part of any return that holds the points for this
 * hull.
 */
public class ConvexHullROC
{
	// initial beam size
	private static final int INIT_SIZE = 32;
// FIXME MM find run max size
private static int DEBUG_MAX_SIZE = 0;
	//special Points
	private static final PointDouble ORIGIN = new PointDouble(0.0, 0.0);
	private static final PointDouble TOP = new PointDouble(1.0, 1.0);
	// special return codes
	private static final int NO_INSERT = -1;
	private static final int INSERT_NO_UPDATE = -2;

	// FIXME MM private member after debug
	final List<CandidateROCPoint> itsHull;
	private final boolean allowDuplicates;

	public ConvexHullROC(boolean allowDuplicates)
	{
		itsHull = new ArrayList<CandidateROCPoint>(INIT_SIZE);
		this.allowDuplicates = allowDuplicates;
	};

	// debug only
	ConvexHullROC(SubgroupSet set)
	{
		allowDuplicates = true;
		itsHull = new ArrayList<CandidateROCPoint>(set.size());
		for (Subgroup s : set)
			if (add(new CandidateROCPoint(new Candidate(s))))
			{
				System.out.println("ADD: " + s.getID());
				debug();
				System.out.println();
			}
	};

	void debug()
	{
		synchronized(this)
		{
			final int size = itsHull.size();
			System.out.println("size = " + size + " max_size = " + DEBUG_MAX_SIZE);

			System.out.println(ORIGIN);
			for (SubgroupROCPoint p : itsHull)
			{
				System.out.println(p);
				System.out.print("\t");
				System.out.println(((CandidateROCPoint)p).itsSlope);
			}
			System.out.println(TOP);
			System.out.print("\t");
			System.out.println(slope(size == 0 ? ORIGIN : itsHull.get(size-1), TOP));
		}
	}

	// throws NullPointerException when thePoint = null
	// when only non-private method, synchronized call is sufficient
	public synchronized boolean add(CandidateROCPoint thePoint)
	{
// FIXME MM DEBUG ONLY, may be off by 1
if (itsHull.size() > DEBUG_MAX_SIZE)
	DEBUG_MAX_SIZE = itsHull.size();

		// always under hull
		if (thePoint.y < thePoint.x)
			return false;

		int size = itsHull.size();
		if (size == 0)
		{
			thePoint.itsSlope = slope(ORIGIN, thePoint);
			return itsHull.add(thePoint);
		}

		// find virtual insertion index for thePoint
		int i = insertionIndex(thePoint);

		// NO_INSERT or INSERT_NO_UPDATE
		if (i < 0)
			return (i == INSERT_NO_UPDATE);

		// update needed, determine what range should be removed
		int fromIndex = (i == 0) ? i : updateLeft(i, thePoint);
		int toIndex = (i == size) ? size : updateRight(i, thePoint);

		assert(fromIndex >= 0);
		assert(toIndex <= size);
		assert(fromIndex <= toIndex);
		// use switch to avoid needless array-copy in some cases
		switch(toIndex-fromIndex)
		{
			case 0 :
			{
				itsHull.add(i, thePoint);
				return true;
			}
			case 1 :
			{
				// avoid array-copy, use set() instead of add()
				itsHull.set(fromIndex, thePoint);
				return true;
			}
			default :
			{
				// remove one less, use set() instead of add()
				itsHull.subList(fromIndex, toIndex-1).clear();
				itsHull.set(fromIndex, thePoint);
				return true;
			}
		}
	}

	private int insertionIndex(CandidateROCPoint thePoint)
	{
		int i = itsHull.size();
		do
		{
			CandidateROCPoint q = itsHull.get(--i);

			if (thePoint.x < q.x)
				continue;
			else if (thePoint.x == q.x)
			{
				// equal FPR, compare TPR
				if (thePoint.y < q.y)
					return NO_INSERT;
				else if (thePoint.y == q.y)
				{
					// copy slope
					thePoint.itsSlope = q.itsSlope;

					// use last addition, like ROCList
					if (!allowDuplicates)
						itsHull.set(i, thePoint);
					else
						itsHull.add(i+1, thePoint);

					// no need to update hull
					return INSERT_NO_UPDATE;
				}
				else // (thePoint.y > p.y)
				{
					// update needed as multiple points
					// left of i may be identical to i
					return i;
				}
			}
			else // (thePoint.x > q.x)
			{
				// check if valid insertion
				if (pos(q, thePoint, right(i+1)) < 0)
					return NO_INSERT;

				// TODO MM strictly convex check would go here
				return ++i;
			}
		}
		while (i > 0);

		// thePoint.x is smaller than any other x, put it in front
		if (pos(ORIGIN, thePoint, itsHull.get(0)) < 0)
			return NO_INSERT;

		// TODO MM strictly convex check would go here
		if (thePoint.x == 0.0)
			thePoint.itsSlope = Double.POSITIVE_INFINITY;
		else
			thePoint.itsSlope = slope(ORIGIN, thePoint);

		return 0;
	}

	private final PointDouble right(int index)
	{
		return (index == itsHull.size()) ? TOP : itsHull.get(index);
	}

	/*
	 *  1 when middle is above line through left-right
	 *  0 when middle is on line through left-right
	 * -1 when middle is below line through left-right
	 */
	private static final int pos(PointDouble left, PointDouble middle, PointDouble right)
	{
		// do not use Math.signum(d), no (-)Infinite / NaN occur
		double value = (middle.x - right.x) * (left.y - right.y) -
				(left.x - right.x) * (middle.y - right.y);

		// for primitive types 0.0 and -0.0 are numerically equal
		// contrary to Float/ Double.compare() where 0.0 > -0.0
		return (value < 0.0) ? -1 : (value == 0.0) ? 0 : 1;
	}

	// NOTE pos() checked that thePoint was not below line (index-1, index)
	// mark all points p left of thePoint that have p.slope < thePoint.slope
	private final int updateLeft(int index, CandidateROCPoint thePoint)
	{
		// TODO MM strictly convex check for fpr=0.0 would go here
		// keep all points on FPR-axis for now
		if (thePoint.x == 0.0)
		{
			thePoint.itsSlope = Double.POSITIVE_INFINITY;
			return index;
		}

		do
		{
			CandidateROCPoint p = itsHull.get(index-1);

			if (p.x == thePoint.x)
			{
				assert(p.y < thePoint.y);
				continue; // find first p with p.x < thePoint.x
			}
			else // p.x < thePoint.x
			{
				assert(p.x < thePoint.x);
				thePoint.itsSlope = slope(p, thePoint);

				if (p.itsSlope < thePoint.itsSlope)
					continue;
				// TODO MM strictly convex check would go here
				//else if (p.itsSlope == thePoint.itsSlope)
				//	;
				else
					return index;
			}

		}
		while (--index > 0);

		// all slopes left of thePoint are tested, angles are not convex
		thePoint.itsSlope = slope(ORIGIN, thePoint);
		return 0;
	}

	// NOTE pos() checked that thePoint was not below line (index-1, index)
	private final int updateRight(int index, CandidateROCPoint thePoint)
	{
		index = updateRightTPR(index, thePoint);

		// not last point
		if (index < itsHull.size())
			index = updateRightSlope(index, thePoint);

		return index;
	}

	private final int updateRightTPR(int index, PointDouble thePoint)
	{
		int size = itsHull.size();
		double x = thePoint.x;

		do
		{
			if (itsHull.get(index).x > x)
				return index;
		}
		while (++index < size);

		// thePoint.TPR > than any other TPR on its right
		return size;
	}

	private final int updateRightSlope(int index, CandidateROCPoint thePoint)
	{
		int size = itsHull.size();

		do
		{
			CandidateROCPoint middle = itsHull.get(index); // safe, < size
			PointDouble right = right(index+1);
			// account for duplicates
			if (middle.x == right.x)
				continue;

			if (pos(thePoint, middle, right) >= 0)
			{
				middle.itsSlope = slope(middle, thePoint);
				return index;
			}
			// TODO MM strictly convex check would go here
			// else if (pos == 0)
		
		}
		while (++index < size);

		// line through thePoint and TOP lies above all other points
		return size;
	}

	private static final double slope(PointDouble left, PointDouble right)
	{
		return (right.y - left.y) / (right.x - left.x);
	}
}
