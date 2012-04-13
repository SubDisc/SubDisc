package nl.liacs.subdisc;

import java.util.*;
import java.lang.*;

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
				int anIndex = Arrays.binarySearch(itsValues, aValue);
				if (theTarget.get(i))
					itsPositiveCounts[anIndex]++;
				else
					itsNegativeCounts[anIndex]++;
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

	// Get the domain sorted by p/n
	// Michael says: rather cumbersome, there must be a cleaner way to do this
	public ArrayList<String> getSortedDomain()
	{
		ArrayList<Integer> aSortedIndexList = new ArrayList<Integer>(itsValues.length);
		for (int i = 0; i < itsValues.length; i++)
			aSortedIndexList.add(new Integer(i));

		Collections.sort(aSortedIndexList, new Comparator() {
					public int compare(Object i1, Object i2)
					{
						Integer index1 = (Integer) i1;
						Integer index2 = (Integer) i2;
						return itsPositiveCounts[index2.intValue()] * itsNegativeCounts[index1.intValue()] - itsPositiveCounts[index1.intValue()] * itsNegativeCounts[index2.intValue()];
					}
				}
		);

		ArrayList<String> aSortedDomain = new ArrayList<String>();
		for (int i = 0; i < itsValues.length; i++)
			aSortedDomain.add(new String(itsValues[aSortedIndexList.get(i).intValue()]));

		return aSortedDomain;
	}

	public void print()
	{
		for (int i = 0; i < size(); i++)
			Log.logCommandLine(itsValues[i] + ": (" + itsPositiveCounts[i] + ", " + itsNegativeCounts[i] + ")");
	}
}
