package nl.liacs.subdisc;

import java.util.Iterator;
import java.util.TreeSet;

public class CandidateQueue
{
	public static final int BEAM	= 0;
	public static final int BESTFIRST 		= 1;
	public static final int DFS 		= 2;
	public static final int BFS		= 3;
	public static final int LAST_SEARCH_STRATEGY = BFS;

	private int	itsSearchStrategy;
	private TreeSet<Candidate> itsQueue;
	private TreeSet<Candidate> itsNextQueue;
	private int itsMaximumQueueSize = 1000;

	public CandidateQueue(int theSearchStrategy, int theMaximumQueueSize, Candidate theRootCandidate)
	{
		itsSearchStrategy = theSearchStrategy;
		if (theSearchStrategy == BEAM)
			itsNextQueue = new TreeSet<Candidate>();
		itsQueue = new TreeSet<Candidate>();
		itsQueue.add(theRootCandidate);

		itsMaximumQueueSize = theMaximumQueueSize;
	}

	public boolean add(Candidate theCandidate)
	{
		boolean aResult;

		if (itsSearchStrategy == BEAM)
			aResult = itsNextQueue.add(theCandidate);
		else
			aResult = itsQueue.add(theCandidate);
		if (aResult)
			trimQueue();

		return aResult;
	}

	public Candidate removeFirst()
	{
		if ((itsSearchStrategy == BEAM) && (itsQueue.size() == 0))
		{
			itsQueue = itsNextQueue;
			itsNextQueue = new TreeSet<Candidate>();
		}
		Candidate aFirstCandidate = itsQueue.first();
		Iterator<Candidate> anIterator = itsQueue.iterator();
		anIterator.next();
		anIterator.remove();
		return aFirstCandidate;
	}

	public double getMinimumPriority()
	{
		if (size() > 0)
		{
			if (itsSearchStrategy == BEAM)
			{
				if (itsQueue.last().getPriorityLevel() > itsNextQueue.last().getPriorityLevel())
					return itsNextQueue.last().getPriorityLevel();
				else
					return itsQueue.last().getPriorityLevel();
			}
			else
				return itsQueue.last().getPriorityLevel();
		}
		else
			return 0;
	}

	public double getMaximumPriority()
	{
		if (size() > 0)
		{
			if (itsSearchStrategy == BEAM)
			{
				if (itsQueue.first().getPriorityLevel() < itsNextQueue.first().getPriorityLevel())
					return itsNextQueue.first().getPriorityLevel();
				else
					return itsQueue.first().getPriorityLevel();
			}
			else
				return itsQueue.first().getPriorityLevel();
		}
		else
			return 0;
	}

	public void trimQueue()
	{
		if (itsSearchStrategy == BEAM)
			while (itsNextQueue.size() > itsMaximumQueueSize)
			{
				Iterator<Candidate> anIterator = itsNextQueue.iterator();
				while (anIterator.hasNext())
					anIterator.next();
				anIterator.remove();
			}
		else
			while (itsQueue.size() > itsMaximumQueueSize)
			{
				Iterator<Candidate> anIterator = itsQueue.iterator();
				while (anIterator.hasNext())
					anIterator.next();
				anIterator.remove();
			}
	}

	public int size()
	{
		if (itsSearchStrategy == BEAM)
			return itsQueue.size() + itsNextQueue.size();
		else
			return itsQueue.size();
	}

	public void setMaximumQueueSize(int theMax) { itsMaximumQueueSize = theMax; }
}
