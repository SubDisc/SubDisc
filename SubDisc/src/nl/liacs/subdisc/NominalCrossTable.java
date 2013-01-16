package nl.liacs.subdisc;

import java.util.*;

public class NominalCrossTable
{
	private String[] itsValues;
	private int[] itsPositiveCounts;
	private int[] itsNegativeCounts;
	private int itsPositiveCount = 0; //sum
	private int itsNegativeCount = 0; //sum

	public NominalCrossTable(TreeSet<String> theDomain, Column theColumn, Subgroup theSubgroup, BitSet theTarget)
	{
		itsValues = theDomain.toArray(new String[0]);
		itsPositiveCounts = new int[itsValues.length];
		itsNegativeCounts = new int[itsValues.length];

		//loop over all records (AK could be faster? ok for now)
		for (int i=0; i<theColumn.size(); i++)
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

		// faster alternative, check only SG.members, combine 2 loops
		// TODO test
//		final BitSet aMembers = theSubgroup.getMembers();
//		for (int i = aMembers.nextSetBit(0); i >= 0; i = aMembers.nextSetBit(i + 1))
//		{
//			int anIndex = Arrays.binarySearch(itsValues, theColumn.getNominal(i));
//			if (theTarget.get(i))
//			{
//				++itsPositiveCounts[anIndex];
//				++itsPositiveCount;
//			}
//			else
//			{
//				++itsNegativeCounts[anIndex];
//				++itsNegativeCount;
//			}
//		}
	}

	public String getValue(int index) { return itsValues[index]; }
	public int getPositiveCount(int theIndex) { return itsPositiveCounts[theIndex]; }
	public int getNegativeCount(int theIndex) { return itsNegativeCounts[theIndex]; }
	public int getPositiveCount() { return itsPositiveCount; }
	public int getNegativeCount() { return itsNegativeCount; }
	public int size() { return itsValues.length; }

	// never used
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
	public List<Integer> getSortedDomainIndices()
	{
		List<Integer> aSortedIndexList = new ArrayList<Integer>(itsValues.length);
		for (int i = 0; i < itsValues.length; i++)
			aSortedIndexList.add(new Integer(i));

		// as long a itsPositiveCounts / itsNegativeCounts do not change
		// CrossTableComparator could be saved as member of this class,
		// instead of being recreated for each call to getSortedDomainIndices
		// TODO: standard java implementation uses merge sort
		//  --> use a quicksort implementing dutch national flag for large lists
		Collections.sort(aSortedIndexList, new CrossTableComparator(itsPositiveCounts, itsNegativeCounts));

		return aSortedIndexList;
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
			// XXX NOTE int[] arguments are not used here!!!
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
