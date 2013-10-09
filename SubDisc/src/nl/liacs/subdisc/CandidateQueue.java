package nl.liacs.subdisc;

import java.util.*;

/*
 * NOTE As it stands, this class has deadlock potential.
 * Always obtain lock in fixed order: itsQueue -> itsNextQueue -> itsTempQueue.
 * 
 * TODO There is no need to store itsNextQueue items when max_depth is reached.
 * 
 * TODO Queue classes in Concurrency framework allow for better concurrency. Eg.
 * Higher concurrency through non-locking algorithms and compareAndSwap methods.
 * 
 * NOTE ROC_BEAM implementation uses Lists for sorted, indexed, storage of
 * Candidates. It allows for faster modification of the hull. Consequently,
 * itsNextQueue is not a TreeSet, however, when moving to a next level the,
 * itsNextQueue is 'transformed' into TreeSet itsQueue.
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
	private ConvexHullROCNaive itsNextQueueConvexHullROC;
	private ROCList itsNextQueueROCList;		// debug only
	private ConvexHullROC itsNextQueueROCBeam;	// debug only
	private TreeSet<Candidate> itsTempQueue;
	private final int itsMaximumQueueSize;

	public CandidateQueue(SearchParameters theSearchParameters, Candidate theRootCandidate)
	{
		itsSearchStrategy = theSearchParameters.getSearchStrategy();

		// all SearchStrategies use itsQueue
		// most use Candidate's natural ordering (no Comparator)
		// (though CandidateComparatorBestFirst has same behaviour)
		// DEPTH_FIRST and BREADTH_FIRST use a different Comparator
		// setup additional beam constructs if needed
		switch (itsSearchStrategy)
		{
			case BEAM :
				itsQueue = new TreeSet<Candidate>();
				itsNextQueue = new TreeSet<Candidate>();
				itsMaximumQueueSize = theSearchParameters.getSearchStrategyWidth();
				break;
			case ROC_BEAM :
			{
				itsQueue = new TreeSet<Candidate>();
				itsNextQueueConvexHullROC = new ConvexHullROCNaive();
				itsNextQueueROCList = new ROCList();
				itsNextQueueROCBeam = new ConvexHullROC(true);
				itsMaximumQueueSize = Integer.MAX_VALUE;
				break;
			}
			case COVER_BASED_BEAM_SELECTION :
				itsQueue = new TreeSet<Candidate>();
				// initialise now, avoids NullPointerException later
				itsNextQueue = new TreeSet<Candidate>();
				itsTempQueue = new TreeSet<Candidate>();
				itsMaximumQueueSize = theSearchParameters.getSearchStrategyWidth();
				break;
			case BEST_FIRST :
				itsQueue = new TreeSet<Candidate>();
				itsMaximumQueueSize = Integer.MAX_VALUE;
				break;
			case DEPTH_FIRST :
				itsQueue = new TreeSet<Candidate>(Candidate.getComparator(itsSearchStrategy));
				itsMaximumQueueSize = Integer.MAX_VALUE;
				break;
			case BREADTH_FIRST :
				itsQueue = new TreeSet<Candidate>(Candidate.getComparator(itsSearchStrategy));
				itsMaximumQueueSize = Integer.MAX_VALUE;
				break;
			// unknown SearchStrategy / null - no AssertionError as
			// this is a public constructor
			default :
				throw new IllegalArgumentException(itsSearchStrategy.toString());
		}

		itsQueue.add(theRootCandidate);
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
			case ROC_BEAM :
			{
				final SubgroupROCPoint p =
					new SubgroupROCPoint(theCandidate.getSubgroup());
				boolean isAdded =
				itsNextQueueConvexHullROC.add(p);
				itsNextQueueROCList.add(p);
				itsNextQueueROCBeam.add(new CandidateROCPoint(theCandidate));

// FIXME MM debug check
if (Process.ROC_BEAM_TEST)
{
//itsNextQueueConvexHullROC.debug();
//itsNextQueueROCBeam.debug();
//System.out.println();
//ConvexHullROCNaive.debugCompare(itsNextQueueROCList, itsNextQueueConvexHullROC, itsNextQueueROCBeam);
}

				return isAdded;
			}
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

		// if (depth == max_depth) no beam selection needs to be done

		switch (itsSearchStrategy)
		{
			case BEAM :
			{
				// make next level current
				// synchronized (itsQueue) done by removeFirst()
				itsQueue = itsNextQueue;
				synchronized (itsNextQueue)
				{
					itsNextQueue = new TreeSet<Candidate>();
				}
				break;
			}
			case ROC_BEAM :
			{
				// synchronized (itsQueue) done by removeFirst()
				synchronized (itsNextQueueConvexHullROC)
				{
// FIXME MM REMOVE
if (Process.ROC_BEAM_TEST)
{
System.out.println("ROC_BEAM for next level:");
System.out.println("PRE (0.0, TPR) removal:");
itsNextQueueConvexHullROC.debug();
itsNextQueueConvexHullROC.removePureSubgroups();
System.out.println("POST (0.0, TPR) removal:");
itsNextQueueConvexHullROC.debug();
}
					itsQueue = itsNextQueueConvexHullROC.toTreeSet();
					itsNextQueueConvexHullROC = new ConvexHullROCNaive();
					itsNextQueueROCList = new ROCList();
					itsNextQueueROCBeam = new ConvexHullROC(true);
				}
				break;
			}
			case COVER_BASED_BEAM_SELECTION :
			{
				// lock in fixed order to avoid deadlock, excuse indenting
				// synchronized (itsQueue) done by removeFirst()
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
				break;
			}
			// should never happen
			default :
				throw new AssertionError(itsSearchStrategy.toString());
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
			final int qs = itsQueue.size();

			switch(itsSearchStrategy)
			{
				case BEAM :
				{
					synchronized (itsNextQueue)
					{
						return qs + itsNextQueue.size();
					}
				}
				case ROC_BEAM :
				{
					synchronized (itsNextQueueConvexHullROC)
					{
						return qs + itsNextQueueConvexHullROC.size();
					}
				}
				case COVER_BASED_BEAM_SELECTION :
				{
					synchronized (itsTempQueue)
					{
						return qs + itsTempQueue.size();
					}
				}
				// do not use fall-through
				case BEST_FIRST : return qs;
				case DEPTH_FIRST : return qs;
				case BREADTH_FIRST : return qs;
				// should never happen
				default :
					throw new AssertionError(itsSearchStrategy.toString());
			}
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
