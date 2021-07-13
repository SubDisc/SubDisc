package nl.liacs.subdisc;

import java.util.*;

/*
 * NOTE As it stands, this class has deadlock potential.
 * Always obtain lock in fixed order: itsQueue -> itsNextQueue -> itsTempQueue.
 * 
 * TODO Queue classes in Concurrency framework allow for better concurrency. Eg.
 * Higher concurrency through non-locking algorithms and compareAndSwap methods.
 * TODO additions to CandidateQueue and SubgroupSet need to be atomic, so not
 * only individual classes need to be thread safe
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
	// the method is fundamentally flawed (in paper), and the code has a bug
	private static final boolean USE_OLD_COVER_BASED_SUBGROUP_SELECTION = false;

	private final SearchStrategy itsSearchStrategy;
	private final boolean isBeamSearchStrategy;
	private final int itsMaxDepth;
	private TreeSet<Candidate> itsQueue;
	private TreeSet<Candidate> itsNextQueue;
	private ConvexHullROCNaive itsNextQueueConvexHullROC;
//	private ROCList itsNextQueueROCList;		// debug only
//	private ConvexHullROC itsNextQueueROCBeam;	// debug only
	private TreeSet<Candidate> itsTempQueue;
	private final int itsMaximumQueueSize;

	public CandidateQueue(SearchParameters theSearchParameters, Candidate theRootCandidate)
	{
		if (theSearchParameters == null ||
			theSearchParameters.getSearchStrategy() == null ||
			theRootCandidate == null)
			throw new IllegalArgumentException("arguments can not be null");

		itsSearchStrategy = theSearchParameters.getSearchStrategy();
		isBeamSearchStrategy = itsSearchStrategy.isBeam();
		itsMaxDepth = theSearchParameters.getSearchDepth();
		if (itsMaxDepth <= 0)
			throw new IllegalArgumentException("search depth must be > 0");

		// TODO MM - check (itsMaximumQueueSize > 0)
		if (itsSearchStrategy.requiresSearchWidthParameter())
			itsMaximumQueueSize = theSearchParameters.getSearchStrategyWidth();
		else
			itsMaximumQueueSize = Integer.MAX_VALUE;

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
				break;
			case ROC_BEAM :
			{
				itsQueue = new TreeSet<Candidate>();
				itsNextQueueConvexHullROC = new ConvexHullROCNaive();
//				itsNextQueueROCList = new ROCList();
//				itsNextQueueROCBeam = new ConvexHullROC(true);
				break;
			}
			case COVER_BASED_BEAM_SELECTION :
				itsQueue = new TreeSet<Candidate>();
				// initialise now, avoids NullPointerException later
				itsNextQueue = new TreeSet<Candidate>();
				itsTempQueue = new TreeSet<Candidate>();
				break;
			case BEST_FIRST :
				itsQueue = new TreeSet<Candidate>();
				break;
			case DEPTH_FIRST :
				itsQueue = new TreeSet<Candidate>(Candidate.getComparator(itsSearchStrategy));
				break;
			case BREADTH_FIRST :
				itsQueue = new TreeSet<Candidate>(Candidate.getComparator(itsSearchStrategy));
				break;
			// unknown SearchStrategy / null - no AssertionError as
			// this is a public constructor
			default :
				throw new IllegalArgumentException(itsSearchStrategy.toString());
		}

		// this.add() would be better
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
	// CandidateQueue does not assert (theCandidate.size() >= minimum coverage)
	// this is left to SubgroupDiscovery.check(), which is bad design
	public boolean add(Candidate theCandidate)
	{
		// these are checked by SubgroupDiscovery.checkAndLog(), but leave it in
		Subgroup aSubgroup = theCandidate.getSubgroup();
		if (aSubgroup.getDepth() >= itsMaxDepth)
			return false;

		// no useful Refinement from this can result
		if (aSubgroup.getCoverage() <= 1)
			return false;

		// kill members in all settings for now
		// COVER_BASED_BEAM_SELECTION actually still needs the members
		// but that code needs revisions anyway
		// ROC_BEAM kills members AFTER the SubgroupROCPoint is created
		if (itsSearchStrategy != SearchStrategy.ROC_BEAM)
			aSubgroup.killMembers();

		switch (itsSearchStrategy)
		{
			case BEAM :
				return addToQueue(itsNextQueue, theCandidate);
			case ROC_BEAM :
			{
				final SubgroupROCPoint p =
					new SubgroupROCPoint(theCandidate.getSubgroup());
				boolean isAdded = itsNextQueueConvexHullROC.add(p);
//				itsNextQueueROCList.add(p);
//				itsNextQueueROCBeam.add(new CandidateROCPoint(theCandidate));

// FIXME MM debug check
if (Process.ROC_BEAM_TEST)
{
//itsNextQueueConvexHullROC.debug();
//itsNextQueueROCBeam.debug();
//System.out.println();
//ConvexHullROCNaive.debugCompare(itsNextQueueROCList, itsNextQueueConvexHullROC, itsNextQueueROCBeam);
}

				aSubgroup.killMembers();
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
		synchronized (theQueue)
		{
			boolean isAdded = theQueue.add(theCandidate);

			if (isAdded && (theQueue.size() > itsMaximumQueueSize))
				theQueue.pollLast();

			return isAdded;
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
			if (isBeamSearchStrategy && itsQueue.size() == 0)
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
//					itsNextQueueROCList = new ROCList();
//					itsNextQueueROCBeam = new ConvexHullROC(true);
				}
				break;
			}
			case COVER_BASED_BEAM_SELECTION :
			{
				// lock in fixed order to avoid deadlock, excuse indenting
				// synchronized (itsQueue) done by removeFirst()
				synchronized (itsNextQueue) {
				synchronized (itsTempQueue) {
					if (!USE_OLD_COVER_BASED_SUBGROUP_SELECTION)
					{
						itsQueue     = (TreeSet<Candidate>) CoverBasedSubgroupSelection.postProcessCandidateSet(itsTempQueue, itsMaximumQueueSize);
						itsNextQueue = new TreeSet<Candidate>();
						itsTempQueue = new TreeSet<Candidate>();
						// old optimised code - used up till Cortana.3138.jar
						// postProcessCBBS();
					}
					else
					{
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
		// do not use synchronized block on itsQueue and then place
		// switch inside it, keep lock as short as possible
		switch (itsSearchStrategy)
		{
			case BEAM :
			{
				synchronized (itsQueue)
				{
					int qs = itsQueue.size();
					synchronized (itsNextQueue)
					{
						return qs + itsNextQueue.size();
					}
				}
			}
			case ROC_BEAM :
			{
				synchronized (itsQueue)
				{
					int qs = itsQueue.size();
					synchronized (itsNextQueueConvexHullROC)
					{
						return qs + itsNextQueueConvexHullROC.size();
					}
				}
			}
			case COVER_BASED_BEAM_SELECTION :
			{
				synchronized (itsQueue)
				{
					int qs = itsQueue.size();
					synchronized (itsTempQueue)
					{
						return qs + itsTempQueue.size();
					}
				}
			}
			// do not use fall-through
			case BEST_FIRST :
				synchronized (itsQueue) { return itsQueue.size(); }
			case DEPTH_FIRST :
				synchronized (itsQueue) { return itsQueue.size(); }
			case BREADTH_FIRST :
				synchronized (itsQueue) { return itsQueue.size(); }
			// should never happen
			default :
				throw new AssertionError(itsSearchStrategy.toString());
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

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// start of obsolete code - replaced by CoverBasedSubgroupSelection /////
	/////                                                                  /////
	///// the CBSS technique, as presented in the papers, is fundamentally /////
	///// flawed                                                           /////
	///// it can not properly handle quality measures that might yield a   /////
	///// negative quality                                                 /////
	///// among these are the WRAcc and Numeric WRAcc measures as offered  /////
	///// in the papers                                                    /////
	///// for negative qualities the technique does the exact opposite of  /////
	///// what it claims to do: selecting Subgroups with HIGH cover counts /////
	///// and for a quality of 0, the cover counts are ignored completely  /////
	/////                                                                  /////
	///// the original implementation by Arno called the two methods below /////
	///// from the (now) unused code in moveToNextLevel()                  /////
	/////   computeCoverCount(int)                                         /////
	/////   computeMultiplicativeWeight(Candidate)                         /////
	///// it is extremely slow, and flawed as in the paper                 /////
	///// it also contains a bug                                           /////
	/////                                                                  /////
	///// the optimised implementation consists of                         /////
	/////   postProcessCBBS()                                              /////
	/////   updateCoverCounts(int[], Candidate)                            /////
	/////   computeMultiplicativeWeight(Candidate, int[])                  /////
	///// it is based on the original code                                 /////
	///// it is much faster, flawed as in the paper, contains the same bug /////
	/////                                                                  /////
	///// the correct, bug-free CoverBasedSubgroupSelection is based on it /////
	/////                                                                  /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	/**
	* Computes the cover count of a particular example: the number of times this example is a member of a subgroup. \n
	* See van Leeuwen & Knobbe, ECML PKDD 2011. \n
	*/
	@Deprecated
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
	@Deprecated
	private double computeMultiplicativeWeight(Candidate theCandidate)
	{
		double aResult = 0;
		double anAlpha = 0.9;
		Subgroup aSubgroup = theCandidate.getSubgroup();
		/*
		 * FIXME MM
		 *
		 * this is one of the things that makes cbbs extremely slow
		 * getMembers() returns a clone of the members
		 * for large dataset this is costly
		 */
		BitSet aMember = aSubgroup.getMembers();

		for(int i=aMember.nextSetBit(0); i>=0; i=aMember.nextSetBit(i+1))
			aResult += Math.pow(anAlpha, computeCoverCount(i));

		return aResult/aSubgroup.getCoverage();
	}

	/*
	 * TODO MM
	 * from here on itsTempQueue is not needed as Tree structure anymore
	 * creating a linear access linear access data view of its items may be
	 * faster to access, as it can be better predicted by the cpu
	 * and avoids the use of extra positioning variables
	 */
	@Deprecated
	private final void postProcessCBBS()
	{
		int aSize = itsTempQueue.size();

		if (aSize == 0)
		{
			Log.logCommandLine("========================================================");
			itsQueue = itsNextQueue;
			itsNextQueue = new TreeSet<Candidate>();
			itsTempQueue = new TreeSet<Candidate>();
			return;
		}

		// first() is safe when itsTempQueue is not empty
		int aNrRows = itsTempQueue.first().getSubgroup().getParentSet().getTotalCoverage();
		int[] aCoverCounts = new int[aNrRows];

		// in each execution of the loop, a Candidate will be added to
		// itsNextQueue, and the cover counts go up
		// as cover counts go up, multiplicative weights go down
		// so for each round, the maximum score a Candidate can attain
		// is upper bounded by the score attained in the previous round
		// this is useful for pruning, see main loop below
		// the scores are initiates to c.getPriority()
		// see the comment on the first loop below
		double[] aLastQuality = new double[aSize];
		int idx = -1;
		for (Candidate c : itsTempQueue)
			aLastQuality[++idx] = c.getPriority();

		int aLoopSize = Math.min(itsMaximumQueueSize, aSize);
		BitSet aUsed = new BitSet(aSize);

		Log.logCommandLine("candidates: " + aSize);
		// the first loop is special, as there are no Candidates in itsNextQueue
		// yet, the multiplicative weight for each Candidate is equal to 1.0
		// so the Candidate with the highest priority wins
		// and its new quality = 1.0 * c.getPriority()
		// therefore the first execution of the loop is taken out
		Log.logCommandLine("loop 0");
		Log.logCommandLine(String.format("best (0): %f, 1.0, %1$f", aLastQuality[0]));
		Candidate aFirst = itsTempQueue.first();
		aUsed.set(0);
		addToQueue(itsNextQueue, aFirst);
		updateCoverCounts(aCoverCounts, aFirst);

		// copy candidates into itsNextQueue
		for (int i = 1; i < aLoopSize; ++i)
		{
			Log.logCommandLine("loop " + i);

			// TODO MM - check the following suspicion
			// all qualities can be NEGATIVE_INFINITY, it is a possible result
			// for some quality measures (whether it is valid is something else)
			// then <= aMaxQuality would always be true, and the loop does not
			// select any Candidate, causing NullPointerExceptions below
			double aMaxQuality          = Double.NEGATIVE_INFINITY;
			Candidate aBestCandidate    = null;
			double aBestCandidateWeight = Double.NaN;
			int aCount                  = -1;
			int aChosen                 = -1;

			// it might be faster to copy Candidates to array or List first
			// and then use index-based access using first clear Bit of used
			for (Candidate aCandidate : itsTempQueue)
			{
				if (aUsed.get(++aCount))
					continue;

				// score can not get better than last time, so if that would not
				// have been good enough there is no use in checking Candidate
				if (aLastQuality[aCount] <= aMaxQuality)
					continue;

				// could get members here and reuse for update()
				double aWeight       = computeMultiplicativeWeight(aCandidate, aCoverCounts);
				double aQuality      = (aWeight * aCandidate.getPriority());
				aLastQuality[aCount] = aQuality;

				if (aQuality > aMaxQuality)
				{
					aMaxQuality          = aQuality;
					aBestCandidate       = aCandidate;
					aBestCandidateWeight = aWeight;
					aChosen              = aCount;
				}
			}

			Log.logCommandLine(String.format("best (%d): %f, %f, %f", aChosen, aBestCandidate.getPriority(), aBestCandidateWeight, aMaxQuality));
			aUsed.set(aChosen);
			aBestCandidate.setPriority(aMaxQuality);
			addToQueue(itsNextQueue, aBestCandidate);
			updateCoverCounts(aCoverCounts, aBestCandidate);
		}
		itsQueue = itsNextQueue;

		Log.logCommandLine("========================================================");
		Log.logCommandLine("used: " + aUsed.toString());
		for (Candidate aCandidate : itsQueue)
			Log.logCommandLine("priority: " + aCandidate.getPriority());

		itsNextQueue = new TreeSet<Candidate>();
		itsTempQueue = new TreeSet<Candidate>();
	}

	@Deprecated
	private static final void updateCoverCounts(int[] theCoverCounts, Candidate theCandidate)
	{
		BitSet b = theCandidate.getSubgroup().getMembers();

		for (int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i+1))
			++theCoverCounts[i];
	}

	private static final double ALPHA = 0.9;
	@Deprecated
	private static final double computeMultiplicativeWeight(Candidate theCandidate, int[] theCoverCounts)
	{
		Subgroup aSubgroup = theCandidate.getSubgroup();
		// this creates a clone of the members BitSet every time in the loop
		// the BitSet should be cached
		BitSet b = aSubgroup.getMembers();

		double aResult = 0.0;
		for (int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i+1))
			aResult += Math.pow(ALPHA, theCoverCounts[i]);

		return aResult / aSubgroup.getCoverage();
	}
}
