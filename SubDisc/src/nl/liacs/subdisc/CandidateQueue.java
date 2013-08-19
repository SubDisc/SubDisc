package nl.liacs.subdisc;

import java.util.*;

/*
 * NOTE As it stands, this class has deadlock potential.
 * Always obtain lock in fixed order: itsQueue -> itsNextQueue -> itsTempQueue.
 * 
 * TODO Queue classes in Concurrency framework allow for better concurrency. Eg.
 * Higher concurrency through non-locking algorithms and compareAndSwap methods.
 */
/**
 * A CandidateQueue holds a collection of {@link Candidate Candidate}s for
 * future processing. These are ordered as dictated by Candidate's
 * {@link Candidate#compareTo(Candidate) compareTo(Candidate)} method.
 * 
 * This class is thread save.
 * 
 * @see Candidate
 */
public class CandidateQueue
{
	private SearchStrategy itsSearchStrategy;
	private TreeSet<Candidate> itsQueue;
	private TreeSet<Candidate> itsNextQueue;
	private List<Candidate> itsNextQueueROC; // special
	private TreeSet<Candidate> itsTempQueue;
	private final int itsMaximumQueueSize;

	// special Candidates for itsNextQueueROC, may be removed
//	private static final Candidate ZERO_ZERO = new Candidate(null);
//	private static final Candidate ONE_ONE = new Candidate(null);

	public CandidateQueue(SearchParameters theSearchParameters, Candidate theRootCandidate)
	{
		itsSearchStrategy = theSearchParameters.getSearchStrategy();

		// setup additional beam constructs if needed
		switch (itsSearchStrategy)
		{
			case BEAM :
				itsNextQueue = new TreeSet<Candidate>();
				break;
//			case ROC_BEAM :
//				itsNextQueueROC = new ArrayList<Candidate>();
//				itsNextQueueROC.add(ZERO_ZERO);
//				itsNextQueueROC.add(ONE_ONE);
//				break;
			case COVER_BASED_BEAM_SELECTION :
				// initialise now, avoids NullPointerException later
				itsNextQueue = new TreeSet<Candidate>();
				itsTempQueue = new TreeSet<Candidate>();
				break;
			// other known SearchStrategies, NO FALL-THROUGH
			case BEST_FIRST : break;
			case DEPTH_FIRST : break;
			case BREADTH_FIRST : break;
			// unknown SearchStrategy / null - no AssertionError as
			// this is a public constructor
			default :
				throw new IllegalArgumentException(itsSearchStrategy.toString());
		}

		// all SearchStrategies use itsQueue
		itsQueue = new TreeSet<Candidate>();
		itsQueue.add(theRootCandidate);

		itsMaximumQueueSize = theSearchParameters.getSearchStrategyWidth();
	}

	// package private special constructor for Fraunhofer random seeds
//	CandidateQueue()
//	{
//		itsSearchStrategy = theSearchParameters.getSearchStrategy();
//		if (itsSearchStrategy == SearchStrategy.BEAM)
//			itsNextQueue = new TreeSet<Candidate>();
//		if (itsSearchStrategy == SearchStrategy.COVER_BASED_BEAM_SELECTION)
//		{
//			// initialise now, avoids nullPointerException later
//			itsNextQueue = new TreeSet<Candidate>();
//			itsTempQueue = new TreeSet<Candidate>();
//		}
//		itsQueue = new TreeSet<Candidate>();
//		itsQueue.add(theRootCandidate);
//
//		itsMaximumQueueSize = theSearchParameters.getSearchStrategyWidth();
//	}

	/**
	 * Adds a {@link Candidate Candidate} to this CandidateQueue.
	 * The add() and removeFirst() methods are thread save.
	 * 
	 * @param theCandidate the Candidate to add.
	 * 
	 * @return <code>true</code> if Candidate is added, <code>false</code>
	 * otherwise.
	 * 
	 * @see CandidateQueue#removeFirst()
	 * @see Candidate
	 */
	public boolean add(Candidate theCandidate)
	{
		switch (itsSearchStrategy)
		{
			case BEAM :
				return addToQueue(itsNextQueue, theCandidate);
//			case ROC_BEAM :
//				// special add for ROC_BEAM as it is updated on-the-fly
//				return addToQueueROC(theCandidate);
			case COVER_BASED_BEAM_SELECTION :
			{
				//simply add candidate, regardless of the current size of itsTempQueue
				synchronized (itsTempQueue) { return itsTempQueue.add(theCandidate); }
			}
			case BEST_FIRST :
				return addToQueue(itsQueue, theCandidate);
			case DEPTH_FIRST :
				return addToQueue(itsQueue, theCandidate);
			case BREADTH_FIRST :
				return addToQueue(itsQueue, theCandidate);
			// SearchStrategy is checked by constructor
			default :
				throw new AssertionError(itsSearchStrategy.toString());
		}
	}

	//add candidate and trim queue to specified size itsMaximumQueueSize
	private boolean addToQueue(TreeSet<Candidate> theQueue, Candidate theCandidate)
	{
		boolean isAdded;
		synchronized (theQueue)
		{
			isAdded = theQueue.add(theCandidate);

			if (isAdded && (theQueue.size() > itsMaximumQueueSize))
				theQueue.pollLast();
		}
		return isAdded;
	}

	// special add for ROC_BEAM as it is updated on-the-fly
	//
	// TODO MM
	// similar code is used in ROCList and MiMa ConvexHull constructs
	// a general ConvexHull class would remove needless code duplication
	private boolean addToQueueROC(Candidate theCandidate)
	{
		final double aPriority = theCandidate.getPriority();
		final Subgroup aSubgroup = theCandidate.getSubgroup();
		// TPR and FPR can not be NaN
		final double aTPR = aSubgroup.getTruePositiveRate();
		final double aFPR = aSubgroup.getFalsePositiveRate();

		// always under curve
		if (aTPR < aFPR)
			return false;

		synchronized (itsNextQueueROC)
		{
			int size = itsNextQueueROC.size();
			if (size == 2)
			{
				itsNextQueueROC.add(1, theCandidate);
				return true;
			}

			// between (0,0) and (1,1)
			for (int i = 1; i < size-1; ++i)
			{
				Candidate c = itsNextQueueROC.get(i);
				Subgroup s = c.getSubgroup();
				// FIXME MM get*Rate() is expensive, needs fix
				double sFPR = s.getFalsePositiveRate();
				// variables not needed in all cases
				double sTPR = s.getTruePositiveRate();
				double sPriority = c.getPriority();

				if (aFPR > sFPR)
					continue;
				else if (aFPR == sFPR)
				{
					if (aTPR < sTPR)
						return false;
					// there may be many points (x,y)==(q,r)
					else if (aTPR == sTPR)
					{
						// may end up at (aFPR < sFPR)
						if (aPriority <= sPriority)
							continue;
						// add, no need to update hull
						else if (aPriority > sPriority)
						{
							itsNextQueueROC.add(i, c);
							return true;
						}
						else // nan
							throw new AssertionError(aPriority + " incomparible to " + sPriority);
					}
					else // (aTPR > sTPR)
					{
						itsNextQueueROC.set(i, theCandidate);
						updateHull(i);
						return true;
					}
				}
				else
				{
					assert(aFPR < sFPR);
					// add, update hull
					itsNextQueueROC.add(i, c);
					updateHull(i);
					return true;
				}
			}

			// no insertion point found, insert right before (1,1)
			assert (check(itsNextQueueROC, theCandidate));
			itsNextQueueROC.add(size-1, theCandidate);
			updateHull(size-2);
			return true;
		}
	}

	private final void updateHull(int index)
	{
		// updateLeft()
		// remove all points l between [(0,0), i] with l.slope < i.slope
		// remove l-(i-1) when l.slope < i.slope (since hull is convex)

		// updateRight()
		// remove all points r right of i while i.tpr > r.tpr
		// for any two points r, rr between [i, (1,1)]
		//     remove r while (slope(p,r) < slope(p,rr))
		//     (stop when == or >= since hull is convex)
	}

	private static boolean check(List<Candidate> theNextQueueROC, Candidate theCandidate)
	{
		double cPriority = theCandidate.getPriority();
		Subgroup cs = theCandidate.getSubgroup();
		double csFPR = cs.getFalsePositiveRate();

		Candidate o = theNextQueueROC.get(theNextQueueROC.size()-2);
		double oPriority = o.getPriority();
		Subgroup os = o.getSubgroup();
		double osFPR = os.getFalsePositiveRate();

		if (osFPR > csFPR)
			return false;
		else if (osFPR == csFPR)
		{
			double csTPR = cs.getTruePositiveRate();
			double osTPR = os.getTruePositiveRate();

			if (osTPR == csTPR)
			{
				if (oPriority < cPriority)
					return false;
				else if (oPriority == cPriority)
					return true;
				else if (oPriority > cPriority)
					return false;
				else // nan
					throw new AssertionError(cPriority + " incomparible to " + oPriority);
			}

			assert(osTPR > csTPR || osTPR < csTPR);
			return false;
		}
		else
		{
			assert(osFPR < csFPR);
			return true;
		}
	}

	/**
	 * Retrieves first {@link Candidate Candidate} from this CandidateQueue,
	 * and moves to next level if required.
	 * The add() and removeFirst() methods are thread save.
	 * 
	 * @return the Candidate at the head of this CandidateQueue.
	 * 
	 * @see CandidateQueue#add(Candidate)
	 * @see Candidate
	 */
	public Candidate removeFirst()
	{
		synchronized (itsQueue)
		{
			if (itsSearchStrategy.isBeam() && itsQueue.size() == 0)
				moveToNextLevel();

			return itsQueue.pollFirst();
		}
	}

	/*
	 * removeFirst locks itsQueue, additional locks are acquired here
	 * NOTE to avoid potential deadlock always obtain locks in fixed order:
	 * itsQueue -> itsNextQueue -> itsTempQueue
	 */
	private void moveToNextLevel()
	{
		Log.logCommandLine("\nLevel finished --------------------------------------------\n");

		// FIXME MM use switch instead of if/ else
		if (itsSearchStrategy == SearchStrategy.BEAM) //make next level current
		{
			itsQueue = itsNextQueue;
			synchronized (itsNextQueue) { itsNextQueue = new TreeSet<Candidate>(); }
		}
		else // COVER_BASED_BEAM_SELECTION
		{
		// lock in fixed order to avoid deadlock, excuse indenting
		synchronized (itsNextQueue) {
		synchronized (itsTempQueue) {
			Log.logCommandLine("candidates: " + itsTempQueue.size());
			int aLoopSize = Math.min(itsMaximumQueueSize, itsTempQueue.size());
			BitSet aUsed = new BitSet(itsTempQueue.size());
			for (int i=0; i<aLoopSize; i++) //copy candidates into itsNextQueue
			{
				Log.logCommandLine("loop " + i);
				Candidate aBestCandidate = null;
				double aMaxQuality = Float.NEGATIVE_INFINITY;
				int aCount = 0;
				int aChosen = 0;
				for (Candidate aCandidate : itsTempQueue)
				{
					if (!aUsed.get(aCount)) //is this one still available
					{
						double aQuality = computeMultiplicativeWeight(aCandidate) * aCandidate.getPriority();
						if (aQuality > aMaxQuality)
						{
							aMaxQuality = aQuality;
							aBestCandidate = aCandidate;
							aChosen = aCount;
						}
					}
					aCount++;
				}
				Log.logCommandLine("best (" + aChosen + "): " + aBestCandidate.getPriority() + ", " + computeMultiplicativeWeight(aBestCandidate) + ", " + aMaxQuality);
				aUsed.set(aChosen, true);
				aBestCandidate.setPriority(aMaxQuality);
				addToQueue(itsNextQueue, aBestCandidate);
			}
			itsQueue = itsNextQueue;

			Log.logCommandLine("========================================================");
			Log.logCommandLine("used: " + aUsed.toString());
			for (Candidate aCandidate : itsQueue)
				Log.logCommandLine("priority: " + aCandidate.getPriority());

			itsNextQueue = new TreeSet<Candidate>();
			itsTempQueue = new TreeSet<Candidate>();
		}
		}
		}
	}

	/**
	 * Returns the total number of {@link Candidate Candidate}s in this
	 * CandidateQueue.
	 * Thread save with respect to add() and removeFirst().
	 * 
	 * @return the size of the current queue level.
	 * 
	 * @see CandidateQueue#add(Candidate)
	 * @see CandidateQueue#removeFirst
	 * @see Candidate
	 */
	public int size()
	{
		synchronized (itsQueue)
		{
			// FIXME MM use switch instead of if/ else
			if (itsSearchStrategy == SearchStrategy.BEAM)
				synchronized (itsNextQueue) { return itsQueue.size() + itsNextQueue.size(); }
			else if (itsSearchStrategy == SearchStrategy.COVER_BASED_BEAM_SELECTION)
				synchronized (itsTempQueue) { return itsQueue.size() + itsTempQueue.size(); }
			else
				return itsQueue.size();
		}
	}

	/**
	 * Returns the number of {@link Candidate Candidate}s in the current
	 * queue level of this CandidateQueue.
	 * Thread save with respect to add() and removeFirst().
	 * 
	 * @return the size of the current queue level.
	 * 
	 * @see CandidateQueue#add(Candidate)
	 * @see CandidateQueue#removeFirst
	 * @see Candidate
	 */
	public int currentLevelQueueSize()
	{
		synchronized (itsQueue) { return itsQueue.size(); }
	}

	/**
	* Computes the cover count of a particular example: the number of times this example is a member of a subgroup. \n
	* See van Leeuwen & Knobbe, ECML PKDD 2011. \n
	*/
	private int computeCoverCount(int theRow)
	{
		int aResult = 0;

		synchronized (itsNextQueue)
		{
			for (Candidate aCandidate: itsNextQueue)
				if (aCandidate.getSubgroup().covers(theRow))
					++aResult;
		}

		return aResult;
	}

	/**
	* Computes the multiplicative weight of a subgroup \n
	* See van Leeuwen & Knobbe, ECML PKDD 2011. \n
	*/
	private double computeMultiplicativeWeight(Candidate theCandidate)
	{
		double aResult = 0;
		double anAlpha = 0.9;
		Subgroup aSubgroup = theCandidate.getSubgroup();
		BitSet aMember = aSubgroup.getMembers();

		for(int i=aMember.nextSetBit(0); i>=0; i=aMember.nextSetBit(i+1))
			aResult += Math.pow(anAlpha, computeCoverCount(i));

		return aResult/aSubgroup.getCoverage();
	}
}
