package nl.liacs.subdisc;

import java.util.*;

public class CandidateQueue
{
	private SearchStrategy itsSearchStrategy;
	private TreeSet<Candidate> itsQueue;
	private TreeSet<Candidate> itsNextQueue;
	private TreeSet<Candidate> itsTempQueue;
	private int itsMaximumQueueSize = 1000;

	public CandidateQueue(SearchParameters theSearchParameters, Candidate theRootCandidate)
	{
		itsSearchStrategy = theSearchParameters.getSearchStrategy();
		if (itsSearchStrategy == SearchStrategy.BEAM)
			itsNextQueue = new TreeSet<Candidate>();
		if (itsSearchStrategy == SearchStrategy.COVER_BASED_BEAM_SELECTION)
			itsTempQueue = new TreeSet<Candidate>();
		itsQueue = new TreeSet<Candidate>();
		itsQueue.add(theRootCandidate);

		itsMaximumQueueSize = theSearchParameters.getSearchStrategyWidth();
	}

	public boolean add(Candidate theCandidate)
	{
		if (itsSearchStrategy == SearchStrategy.BEAM)
			return addToQueue(itsNextQueue, theCandidate);
		else if (itsSearchStrategy == SearchStrategy.COVER_BASED_BEAM_SELECTION)
			return itsTempQueue.add(theCandidate); //simply add candidate, regardless of the current size of itsTempQueue
		else
			return addToQueue(itsQueue, theCandidate);
	}

	//add candidate and trim queue to specified size itsMaximumQueueSize
	private boolean addToQueue(TreeSet<Candidate> theQueue, Candidate theCandidate)
	{
		boolean isAdded = theQueue.add(theCandidate);

		if (isAdded && (theQueue.size() > itsMaximumQueueSize))
		{
			Iterator<Candidate> anIterator = theQueue.descendingIterator();
			anIterator.next();
			anIterator.remove();
		}

		return isAdded;
	}

	/*
	 * TODO itsQueue.remove(aFirstCandidate) does not work properly because of
	 * the incomplete Candicate.compareTo() method.
	 */
	 /**
	 * Retrieves first candidate from Queue, and moves to next level if required.
	 */
	public Candidate removeFirst()
	{
		if (itsSearchStrategy.isBeam() && itsQueue.size() == 0)
			moveToNextLevel();

		Candidate aFirstCandidate = itsQueue.first();
		Iterator<Candidate> anIterator = itsQueue.iterator();
		anIterator.next();
		anIterator.remove();
		return aFirstCandidate;
	}

	public void moveToNextLevel()
	{
		Log.logCommandLine("\nLevel finished --------------------------------------------\n");
		if (itsSearchStrategy == SearchStrategy.BEAM) //make next level current
		{
			itsQueue = itsNextQueue;
			itsNextQueue = new TreeSet<Candidate>();
		}
		else // COVER_BASED_BEAM_SELECTION
		{
			Log.logCommandLine("candidates: " + itsTempQueue.size());
			itsNextQueue = new TreeSet<Candidate>();
			int aLoopSize = Math.min(itsMaximumQueueSize, itsTempQueue.size());
			BitSet aUsed = new BitSet(itsTempQueue.size());
			for (int i=0; i<aLoopSize; i++) //copy canidates into itsNextQueue
			{
				Log.logCommandLine("loop " + i);
				Candidate aBestCandidate = null;
				double aMaxQuality = Float.MIN_VALUE;
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
							Log.logCommandLine("    ---" + aMaxQuality);
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
			for (Candidate aCandidate : itsQueue)
				Log.logCommandLine("itsQueue: " + aCandidate.getPriority());

			itsNextQueue = new TreeSet<Candidate>();
			itsTempQueue = new TreeSet<Candidate>();
		}
	}

	public int size()
	{
		if (itsSearchStrategy == SearchStrategy.BEAM)
			return itsQueue.size() + itsNextQueue.size();
		else if (itsSearchStrategy == SearchStrategy.COVER_BASED_BEAM_SELECTION)
			return itsQueue.size() + itsTempQueue.size();
		else
			return itsQueue.size();
	}

	public void setMaximumQueueSize(int theMax) { itsMaximumQueueSize = theMax; }

	/**
	* Computes the cover count of a particular example: the number of times this example is a member of a subgroup. \n
	* See van Leeuwen & Knobbe, ECML PKDD 2011. \n
	* Only applies to beam search
	*/
	public int computeCoverCount(int theRow)
	{
		int aResult = 0;

		for (Candidate aCandidate: itsNextQueue)
		{
			Subgroup aSubgroup = aCandidate.getSubgroup();
			if (aSubgroup.covers (theRow))
				aResult++;
		}
		return aResult;
	}

	/**
	* Computes the multiplicative weight of a subgroup \n
	* See van Leeuwen & Knobbe, ECML PKDD 2011. \n
	* Only applies to beam search
	*/
	public double computeMultiplicativeWeight(Candidate theCandidate)
	{
		double aResult = 0;
		double anAlpha = 0.7;
		Subgroup aSubgroup = theCandidate.getSubgroup();
		BitSet aMember = aSubgroup.getMembers();

		for(int i=aMember.nextSetBit(0); i>=0; i=aMember.nextSetBit(i+1))
		{
			int aCoverCount = computeCoverCount(i);
			aResult += Math.pow(anAlpha, aCoverCount);
		}
		return aResult/aSubgroup.getCoverage();
	}
}
