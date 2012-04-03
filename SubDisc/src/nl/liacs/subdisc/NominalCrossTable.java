package nl.liacs.subdisc;

import java.util.*;

public class NominalCrossTable
{
	private String[] itsValues;
	private int[] itsPositiveCounts;
	private int[] itsNegativeCounts;
	private int itsPositiveCount; //sum
	private int itsNegativeCount; //sum

	public NominalCrossTable(TreeSet<String> theDomain, Column theColumn, Subgroup theSubgroup, BitSet theTarget)
	{
		itsValues = new String[theDomain.size()];
		itsPositiveCounts = new int[size()];
		itsNegativeCounts = new int[size()];

		int aCount=0;
		for (String aValue : theDomain)
		{
			itsValues[aCount] = aValue;
			aCount++;
		}

		for (int i=0; i<theColumn.size(); i++) //loop over all records (AK could be faster? ok for now)
		{
			if (theSubgroup.covers(i))
			{
				String aValue = theColumn.getNominal(i);
				int theIndex = Arrays.binarySearch(itsValues, aValue);
				if (theTarget.get(i))
					itsPositiveCounts[theIndex]++;
				else
					itsNegativeCounts[theIndex]++;
			}
		}
		for (int i=0; i<size(); i++)
		{
			itsPositiveCount += itsPositiveCounts[i];
			itsNegativeCount += itsNegativeCounts[i];
		}
	}

    public String getValue(int index) { return itsValues[index]; }
    public int getPositiveCount(int theIndex) { return itsPositiveCounts[theIndex]; }
    public int getNegativeCount(int theIndex) { return itsNegativeCounts[theIndex]; }
    public int getPositiveCount() { return itsPositiveCount; }
    public int getNegativeCount() { return itsNegativeCount; }
	public int size() { return itsValues.length; }

    public HashSet<String> getDomain()
	{
        HashSet<String> aSet = new HashSet<String>();
        for (int i = 0; i < itsValues.length; i++)
			aSet.add(itsValues[i]);
        return aSet;
    }

	public void print()
	{

		for (int i = 0; i < size(); i++)
			Log.logCommandLine(itsValues[i] + ": (" + itsPositiveCounts[i] + ", " + itsNegativeCounts[i] + ")");
	}
}
