package nl.liacs.subdisc;

import java.util.*;

//Michael says: this is basically a copy of NominalCrossTable

public class RealBaseIntervalCrossTable
{
	private float[] itsSplitPoints;
	private int[] itsPositiveCounts;
	private int[] itsNegativeCounts;
	private int itsPositiveCount; //sum
	private int itsNegativeCount; //sum


	public RealBaseIntervalCrossTable(float[] theSplitPoints, Column theColumn, Subgroup theSubgroup, BitSet theTarget)
	{
		itsSplitPoints = new float[theSplitPoints.length];
		itsPositiveCounts = new int[getNrBaseIntervals()];
		itsNegativeCounts = new int[getNrBaseIntervals()];

		int aCount = 0;
		for (float aSplitPoint : theSplitPoints)
		{
			itsSplitPoints[aCount] = aSplitPoint;
			aCount++;
		}
		//sort(itsSplitPoints);

		for (int i=0; i<theColumn.size(); i++) //loop over all records (AK could be faster? ok for now)
		{
			if (theSubgroup.covers(i))
			{
				float aValue = theColumn.getFloat(i);
				int anIndex = Arrays.binarySearch(itsSplitPoints, aValue);
				if (anIndex < 0)
					anIndex = -anIndex - 1;
				if (theTarget.get(i))
					itsPositiveCounts[anIndex]++;
				else
					itsNegativeCounts[anIndex]++;
			}
		}
		for (int i=0; i<getNrBaseIntervals(); i++)
		{
			itsPositiveCount += itsPositiveCounts[i];
			itsNegativeCount += itsNegativeCounts[i];
		}
	}

	
    public float getSplitPoint(int theIndex)
	{
		return itsSplitPoints[theIndex];
	}


	public Interval getBaseInterval(int theIndex)
	{
		if (itsSplitPoints.length == 0)
			return new Interval(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
		if (theIndex == 0)
			return new Interval(Float.NEGATIVE_INFINITY, itsSplitPoints[0]);
		else if (theIndex == itsSplitPoints.length)
			return new Interval(itsSplitPoints[theIndex-1], Float.POSITIVE_INFINITY);
		else
			return new Interval(itsSplitPoints[theIndex-1], itsSplitPoints[theIndex]);
	}


	public int getPositiveCount(int theIndex)
	{
		return itsPositiveCounts[theIndex];
	}
	
	public int getNegativeCount(int theIndex)
	{
		return itsNegativeCounts[theIndex];
	}

	public int getPositiveCount()
	{ 
		return itsPositiveCount;
	}

	public int getNegativeCount()
	{
		return itsNegativeCount;
	}

	public int getNrSplitPoints()
	{
		return itsSplitPoints.length;
	}

	public int getNrBaseIntervals() {
		return itsSplitPoints.length + 1; 
	}


	public float[] getSplitPoints()
	{
		return Arrays.copyOf(itsSplitPoints, itsSplitPoints.length);
    }


	// eliminate split points that separate base intervals with equal distributions
	// only to be used for convex quality measures
	public void aggregateIntervals()
	{
		ArrayList<Float> aNewSplitPoints = new ArrayList<Float>();
		for (int i = 0; i < itsSplitPoints.length; i++)
			if (getPositiveCount(i) * getNegativeCount(i+1) != getPositiveCount(i+1) * getNegativeCount(i))
				aNewSplitPoints.add(new Float(itsSplitPoints[i]));
		
		if (aNewSplitPoints.size() == 0)
		{
			itsSplitPoints = new float[0];
			itsPositiveCounts = new int[1];
			itsNegativeCounts = new int[1];
			itsPositiveCounts[0] = itsPositiveCount;
			itsNegativeCounts[0] = itsNegativeCount;
			return;
		}

		int [] aNewPositiveCounts = new int[aNewSplitPoints.size()+1];
		int [] aNewNegativeCounts = new int[aNewSplitPoints.size()+1];

		int aPi = 0;
		int aNi = 0;
		int aS = 0;
		for (int i = 0; i < getNrBaseIntervals(); i++)
		{
			aPi += getPositiveCount(i);
			aNi += getNegativeCount(i);
			if ((aS < aNewSplitPoints.size() && itsSplitPoints[i] == aNewSplitPoints.get(aS)) || i == getNrBaseIntervals() - 1)
			{
				aNewPositiveCounts[aS] = aPi;
				aNewNegativeCounts[aS] = aNi;
				aPi = 0;
				aNi = 0;
				aS++;
			}
		}

		itsPositiveCounts = aNewPositiveCounts;
		itsNegativeCounts = aNewNegativeCounts;

		itsSplitPoints = new float[aNewSplitPoints.size()];
		for (int i = 0; i < aNewSplitPoints.size(); i++)
		{
			itsSplitPoints[i] = aNewSplitPoints.get(i);
		}

		return;
	}


	public void print()
	{
		for (int i = 0; i < getNrBaseIntervals(); i++)
			Log.logCommandLine(getBaseInterval(i) + ": (" + itsPositiveCounts[i] + ", " + itsNegativeCounts[i] + ")");
		return;
	}

}
