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

	/** never used */
	@Deprecated
	public HashSet<String> getDomain()
	{
		HashSet<String> aSet = new HashSet<String>();
		for (int i = 0; i < itsValues.length; i++)
			aSet.add(itsValues[i]);
		return aSet;
	}

	// Get the domain sorted by p/n
	// Michael says: rather cumbersome, there must be a cleaner way to do this
	// MM probably yes, will look at this later
	// TODO using / auto-(un)boxing of integer <-> Integer is extremely slow
	// TODO code inefficiently mixes / duplicates arrays (data) and List (view)
	public List<String> getSortedDomain()
	{
		List<Integer> aSortedIndexList = new ArrayList<Integer>(itsValues.length);
		for (int i = 0; i < itsValues.length; i++)
			aSortedIndexList.add(new Integer(i));
/*
		Collections.sort(aSortedIndexList, new Comparator() {
					public int compare(Object i1, Object i2)
					{
						Integer index1 = (Integer) i1;
						Integer index2 = (Integer) i2;
						return itsPositiveCounts[index2.intValue()] * itsNegativeCounts[index1.intValue()] - itsPositiveCounts[index1.intValue()] * itsNegativeCounts[index2.intValue()];
					}
				}
		);
*/
		// as long a itsPositiveCounts / itsNegativeCounts do not change
		// CrossTableComparator could be saved as member of this class,
		// instead of being recreated for each call to getSortedDomain
		Collections.sort(aSortedIndexList, new CrossTableComparator(itsPositiveCounts, itsNegativeCounts));

		List<String> aSortedDomain = new ArrayList<String>(itsValues.length);
//		for (int i = 0; i < itsValues.length; i++)
//			aSortedDomain.add(new String(itsValues[aSortedIndexList.get(i).intValue()]));
		for (Integer i : aSortedIndexList)
			aSortedDomain.add(itsValues[i]);

		return aSortedDomain;
	}

	public void print()
	{
		for (int i = 0; i < size(); i++)
			Log.logCommandLine(itsValues[i] + ": (" + itsPositiveCounts[i] + ", " + itsNegativeCounts[i] + ")");
	}

	// move to separate class upon discretion
	private class CrossTableComparator implements Comparator<Integer>
	{
		private final int[] itsPosCounts;
		private final int[] itsNegCounts;

		/** no null check, may throw null pointer exception */
		CrossTableComparator(int[] thePositiveCounts, int[] theNegativeCounts)
		{
			itsPosCounts = itsPositiveCounts;
			itsNegCounts = itsPositiveCounts;
		}

		@Override
		public int compare(Integer index1, Integer index2)
		{
			// avoid explicit auto-(un)boxing, gives compiler / JVM
			// more freedom to optimise
			return (itsPosCounts[index2] * itsNegCounts[index1]) -
				(itsPosCounts[index1] * itsNegCounts[index2]);
		}
	}
}
