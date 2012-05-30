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

    public float getSplitPoint(int theIndex) { return itsSplitPoints[theIndex]; }
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
    
	public int getPositiveCount(int theIndex) { return itsPositiveCounts[theIndex]; }
    public int getNegativeCount(int theIndex) { return itsNegativeCounts[theIndex]; }
    public int getPositiveCount() { return itsPositiveCount; }
    public int getNegativeCount() { return itsNegativeCount; }
	//public int size() { return itsSplitPoints.length + 1; }
	public int getNrSplitPoints() { return itsSplitPoints.length; }
	public int getNrBaseIntervals() { return itsSplitPoints.length + 1; }

    public float[] getSplitPoints()
	{
		return Arrays.copyOf(itsSplitPoints, itsSplitPoints.length);
    }
	
	
	public void print()
	{

		for (int i = 0; i < getNrBaseIntervals(); i++)
			Log.logCommandLine(getBaseInterval(i) + ": (" + itsPositiveCounts[i] + ", " + itsNegativeCounts[i] + ")");
	}
}
