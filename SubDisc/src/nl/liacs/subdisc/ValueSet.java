package nl.liacs.subdisc;

import java.util.*;

/*
 * TODO
 * A ValueSet should be a Set, not a List, this natively prevents duplicates.
 * A ValueSet could be a SortedSet, making ValuesSet comparisons a lot easier.
 * However, this would destroy any deliberate value-declaration order (in the
 * case of .arff NOMINAL attributes).
 * Sets disallow duplicates, but do not normally maintain insertion order.
 * List do allow duplicates, do maintain insertion order.
 */
public class ValueSet extends ArrayList<String> implements List<String>, Comparable<ValueSet>
{
	private static final long serialVersionUID = 1L;

	/**
	 * Compares this <code>ValueSet</code> against the argument.
	 * <p>
	 * The equality tests are performed in the following order:</br>
	 * if the sets contain exactly the same values, they compare equal,</br>
	 * if the sets are not of equal size, comparison is based on size,</br>
	 * if the sets are of equal size, the non-shared values are ordered
	 * lexicographically, and the String comparison of the lexicographically
	 * smallest value of each set is used for the return value.
	 */
	/*
	 * XXX could use less memory by not creating TreeSets and explicitly
	 * loop over all values (may even be faster)
	 * but this class may need revision anyway, so we leave it as it is
	 */
	@Override
	public int compareTo(ValueSet theOtherSet)
	{
		// removes possible duplicates, see class comment
		final TreeSet<String> A = new TreeSet<String>(this);
		final TreeSet<String> B = new TreeSet<String>(theOtherSet);
		final int sizeA = A.size();
		final int sizeB = B.size();

		// shared values
		final HashSet<String> I = new HashSet<String>(A);
		I.retainAll(B);

		// all values are equal
		if ((sizeA == sizeB) && (sizeA == I.size()))
			return 0;

		// not all values are equal, compare size of A and B
		if (sizeA < sizeB)
			return -1;
		if (sizeA > sizeB)
			return 1;


		// sets are of same size, but not all values are shared
		// remove all shared values from A and B
		A.removeAll(I);
		B.removeAll(I);
		// safe as (sizeA == sizeB) + at least 1 value is not shared
		// TreeSets are ordered so compares 'smallest' String of A and B
		return A.iterator().next().compareTo(B.iterator().next());

		// alternative that does not modify A and B
		//return getSmallest(A, I).compareTo(getSmallest(B, I));
	}

	private static String getSmallest(Set<String> theSet, Set<String> theShared)
	{
		final Iterator<String> i = theSet.iterator();
		String smallest;

		// safe under assumption that theSet contains at least 1 value
		// that is not in theShared
		while (theShared.contains(smallest = i.next()));
		while (i.hasNext())
		{
			final String s = i.next();
			if (!theShared.contains(s) && s.compareTo(smallest) < 0)
				smallest = s;
		}

		return smallest;
	}

	@Override
	public String toString()
	{
		StringBuilder aResult = new StringBuilder(size() * 25);
		aResult.append("{");
		for(String aValue : this)
		{
			aResult.append(aValue);
			aResult.append(",");
		}
		return aResult.substring(0, aResult.length()-1) + "}";
	}

	public static ArrayList<ValueSet> getPowerSet(ValueSet theSet)
	{
		ArrayList<ValueSet> aResult = new ArrayList<ValueSet>();
		if (theSet.size() > 16) //too big
			return aResult;

		//terminate recursion
		if (theSet.isEmpty())
		{
			aResult.add(new ValueSet());
			return aResult;
		}

		//split head and rest
		String aHead = theSet.get(0);
		ValueSet aRest = new ValueSet();
		for (int i=0; i<theSet.size(); i++)
			if (i>0)
				aRest.add(theSet.get(i));

		//loop over powersets of the rest (computed by recursion)
		for (ValueSet aSet : getPowerSet(aRest))
		{
			ValueSet aNewSet = new ValueSet();
			aNewSet.add(aHead);
			aNewSet.addAll(aSet);
			aResult.add(aNewSet);
			aResult.add(aSet);
		}
		return aResult;
	}

	/*
	 * ValueSet should not extend any class. The values should just be
	 * stored in a String[].
	 * Storing in reverse order would speedup getSubset() slightly.
	 */
/*
	private final String[] itsValues;
	public ValueSet(Set<String> theDomain) {
		itsValues = new String[theDomain.size()];
		// iterator preserves order, also for TreeSet
		int i = -1;
		for (Iterator<String> it = theDomain.iterator(); it.hasNext(); )
			itsValues[++i] = it.next();
	}
*/

	/*
	 * NOTE superseded by Column-only version, less = more.
	 * 
	 * Avoids recreation of TreeSet aDomain in
	 * SubgroupDiscovery.evaluateNominalBinaryRefinement().
	 * Memory usage is minimal.
	 * 
	 * Bits set in i represent value-indices go retrieve from this ValueSet.
	 * This just works.
	 */
	public String[] getSubset(int i) {
		// Don Clugston approves
		// count bits set in integer type (12 ops instead of naive 32)
		// http://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetParallel
		int v = i;
		v = v - ((v >> 1) & 0x55555555);			// reuse input as temporary
		v = (v & 0x33333333) + ((v >> 2) & 0x33333333);		// temp
		v = ((v + (v >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;	// count

		String[] aResult = new String[v];
		// k not needed if Strings in itsValues are in reversed order
		for (int j = -1, k = v, m = size()-1; k > 0; --m)
			if (((i >>> ++j) & 1) == 1)	// so no shift in first loop
				aResult[--k] = get(m);	// itsValues[--k]

		return aResult;
	}
}
