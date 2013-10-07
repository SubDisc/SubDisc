package nl.liacs.subdisc;

import java.util.*;

/*
 * Class does not extend ArrayList<?>, hides storage class for points.
 * Avoids the need to overwrite all of the (Array)List methods to ensure proper
 * internal state.
 * Also allows using different storage class without need for API changes.
 */
// TODO MM how to deal with multiple Points with FPR=0, but different TPR
// TODO MM how to deal with multiple Points with TPR=1, but different FPR
public class ConvexHullROCNaive
{
	private static final PointDouble ZERO_ZERO = new PointDouble(0.0, 0.0);
	private static final PointDouble ONE_ONE = new PointDouble(1.0, 1.0);

	private static final int INIT_SIZE = 32; // TODO MM find sane size
	// FIXME MM find run max size
private static int DEBUG_MAX_SIZE = 0;
	// FIXME MM private after debug
	final List<SubgroupROCPoint> itsHull;

	// for ROC_BEAM search
	ConvexHullROCNaive()
	{
		itsHull = new ArrayList<SubgroupROCPoint>(INIT_SIZE);
	}

	// for SubgroupSet.getROCList()
	ConvexHullROCNaive(SubgroupSet theSubgroupSet)
	{
		itsHull = new ArrayList<SubgroupROCPoint>(INIT_SIZE);

		for (Subgroup s : theSubgroupSet)
			if (add(new SubgroupROCPoint(s)))
			{
//				System.out.println("ADD: " + s.getID());
//				debug();
//				System.out.println();
			}
	}

	// return indicates change
	// as long a this is the only public method, synchronized call is enough
	public synchronized boolean add(SubgroupROCPoint thePoint)
	{
// DEBUG ONLY, may be off by 1
if (itsHull.size() > DEBUG_MAX_SIZE)
	DEBUG_MAX_SIZE = itsHull.size();

		// always under hull
		if (thePoint.y < thePoint.x)
			return false;

		int i = itsHull.size();
		if (i == 0)
			return itsHull.add(thePoint);

		// find insertion index for thePoint
		// looping back simplifies == handling
		while (--i >= 0)
		{
			PointDouble q = itsHull.get(i);

			if (thePoint.x < q.x)
				continue;
			else if (thePoint.x == q.x)
			{
				// equal FPR, compare TPR
				if (thePoint.y < q.y)
					return false;
				else if (thePoint.y == q.y)
				{
					itsHull.add(i+1, thePoint);
					// no need to update hull
					return true;
				}
				else // (thePoint.y > q.y)
				{
					itsHull.set(i, thePoint);
					// update needed as multiple points
					// left of i may be identical to i
					break;
				}
			}
			else // (thePoint.x > q.x)
			{
				// check if valid insertion
				if (below(q, thePoint, i))
					return false;

				itsHull.add(++i, thePoint);
				// break after first time
				break;
			}
		}

		// thePoint.x is smallest x, put it in front
		if (i == -1)
		{
			// check if valid insertion
			if (below(ZERO_ZERO, thePoint, 0))
				return false;

			itsHull.add(++i, thePoint);
		}

		// i = index of thePoint
		assert(itsHull.get(i) == thePoint);

		// FIXME MM use of both indices AND Points is cluttered

		// update of hull is needed
		PointDouble l = ZERO_ZERO;
		PointDouble m;
		PointDouble r;

		// at least two points are in the list at this point in code
		// code tests every point on hull
		// this is not strictly needed as only points left and right of
		// insertion point i need to be checked (until some condition)
		// but checking the whole hull yields simple, clean code
		for (int im = 0, ir = 1; im < itsHull.size(); )
		{
			m = itsHull.get(im);
			r = (ir == itsHull.size()) ? ONE_ONE : itsHull.get(ir);

			if (l.x == m.x)
			{
				l = m;
				++im;
				++ir;
				continue;
			}

			// do not remove any points on the top line
			if (m.y == 1.0)
				break;

			if (m.x == r.x && m.y == r.y)
			{
				// identical points, find first different one
				++ir;
				continue;
			}

			double lm = slope(l, m);
			double lr = slope(l, r);

			if (lm < lr)
			{
				// calls removeRange()
				itsHull.subList(im, ir).clear();
				// go back to start, could be smarter, but the
				// duplicates complicate code, keep it simple
				l = ZERO_ZERO;
				im = 0;
				ir = 1;
			}
			else
			{
				if (r == ONE_ONE)
					break;

				l = m;
				//m = r;
				++im;
				// reset ir to index right after im
				ir = im+1;
			}
		}

		return true;
	}

	private boolean below(PointDouble left, PointDouble thePoint, int index)
	{
		PointDouble right = (index == itsHull.size()-1) ? ONE_ONE : itsHull.get(index+1);

		// loop guarantees left.x < thePoint.x < right.x
		if (thePoint.y >= right.y)
			return false;

		return slope(left, thePoint) < slope(left, right);
	}

	private static final double slope(PointDouble a, PointDouble b)
	{
		return (b.y - a.y) / (b.x - a.x);
	}

	int size()
	{
		synchronized(this) { return itsHull.size(); }
	}

	TreeSet<Candidate> toTreeSet()
	{
		final TreeSet<Candidate> candidates = new TreeSet<Candidate>();
		synchronized(this)
		{
			for (SubgroupROCPoint p : itsHull)
				candidates.add(new Candidate(p.getSubgroup()));

			assert(itsHull.size() == candidates.size());
		}

		return candidates;
	}

	/**
	 * Sorts the points on this hull by comparing FPR, when equal, TPR, when
	 * equal, Subgroup ID.
	 * 
	 * Do not call this method before setting the Subgroup IDs using
	 * {@link SubgroupSet#setIDs()}, as all IDs will be equal before that.
	 * Because of this, this method is package-private.
	 * 
	 * This method exists solely to ensure invocation invariant results in a
	 * multi-threaded context.
	 */
	ConvexHullROCNaive sort()
	{
		synchronized (this)
		{
			Collections.sort(itsHull, new SubgroupROCPointComparator());
			return this;
		}
	}

	private static final class SubgroupROCPointComparator implements Comparator<SubgroupROCPoint>
	{
		@Override
		public int compare(SubgroupROCPoint a, SubgroupROCPoint b)
		{
			if (a == b)
				return 0;

			// safe for NaN, but no NaNs by construction
			int cmp = Double.compare(a.x, b.x);
			if (cmp != 0)
				return cmp;

			cmp = Double.compare(a.y, b.y);
			return cmp != 0 ? cmp : a.ID - b.ID;
		}
	}

	/* DEBUG METHODS - WILL BE REMOVED ************************************/

	void debug()
	{
		synchronized(this)
		{
			final int size = itsHull.size();
			System.out.println("size = " + size + " max_size = " + DEBUG_MAX_SIZE);

			if (size == 0)
			{
				debug(ZERO_ZERO, ONE_ONE);
				return;
			}

			debug(ZERO_ZERO, itsHull.get(0));

			for (int i = 1; i < size; ++i)
				debug(itsHull.get(i-1), itsHull.get(i));

			debug(itsHull.get(itsHull.size()-1), ONE_ONE);
		}
		System.out.println();
	}

	private static void debug(PointDouble p, PointDouble q)
	{
		System.out.println(q);
		System.out.println("\t" + slope(p, q));
	}

	// assume thePivot is correct
	static final boolean debugCompare(ROCList thePivot, ConvexHullROCNaive theConvexHullROC, ConvexHullROC theROCBeam)
	{
		synchronized (thePivot) {
		synchronized (theConvexHullROC)
		{
			// compare ROCList to ConvexHullROC
			List<SubgroupROCPoint> c = filter(theConvexHullROC.itsHull);
			boolean rc = debugCompare(thePivot, c, theConvexHullROC.getClass().getSimpleName());

			// compare ROCList to ROCConvexHull
			//boolean rr = true;
			List<SubgroupROCPoint> r = filter(theROCBeam.itsHull);
			boolean rr = debugCompare(thePivot, r, theROCBeam.getClass().getSimpleName());

			System.out.println();
			return rc && rr;
		}
		}
	}

	private static final List<SubgroupROCPoint> filter(List<? extends SubgroupROCPoint> theHull)
	{
		// filter all distinct SubgroupROCPoints on the hull
		List<? extends SubgroupROCPoint> h = theHull;
		List<SubgroupROCPoint> c = new ArrayList<SubgroupROCPoint>(h.size());

		if (h.isEmpty())
			return c;
		// safe as h not empty
		c.add(h.get(0));

		for (int i = 1, j = 0, k = h.size(); i < k; ++i)
		{
			SubgroupROCPoint pi = h.get(i);
			SubgroupROCPoint pj = c.get(j);

			// for multiple points with FPR=0, use highest TPR
			if (pi.x == 0.0 && pj.x == 0.0)
			{
				c.set(j, pi);
				continue;
			}

			// add only the first point with a TPR of one
			if (pi.y == 1.0)
			{
				c.add(pi);
				break;
			}

			// ignore identical points
			if (pi.x == pj.x && pi.y == pj.y)
				continue;

			c.add(pi);
			++j;
		}

		return c;
	}

	static final boolean debugCompare(ROCList theROCList, List<? extends SubgroupROCPoint> theHull, String theClass)
	{
		// compare sizes
		if (theROCList.size() != theHull.size())
		{
			System.out.println("ERROR: ROCList.size() != " + theClass + ".size()");
			debugPrint("ROCList", theROCList);
			debugPrint(theClass, theHull);
			return false;
		}

		// compare individual points
		for (int i = 0, j = theHull.size(); i < j; ++i)
		{
			SubgroupROCPoint r = theROCList.get(i);
			SubgroupROCPoint s = theHull.get(i);

			if (r.x != s.x || r.y != s.y)
			{
				System.out.println("ERROR: ROCList.points != " + theClass + ".points");
				debugPrint("ROCList", theROCList);
				debugPrint(theClass, theHull);
				return false;
			}
		}

		// made it here, all seems fine
		System.out.println("SUCCESS: ROCList.points == " + theClass + ".points");
		return true;
	}

	private static final void debugPrint(String clazz, List<? extends SubgroupROCPoint> list)
	{
		System.out.println(clazz);
		for (SubgroupROCPoint s : list)
			System.out.println(s);
	}
}
