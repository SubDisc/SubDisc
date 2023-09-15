package nl.liacs.subdisc;

import java.util.*;

/*
 * ValueSet does not extend any class, its values are stored in a
 * SortedSet<String>.
 * ValueSets are mainly build from TreeSets, which are sorted also.
 * Insertion order seems not to be relevant to any code.
 * A Set<String> natively avoids duplicates.
 * Having it is a member avoids the need to Override every method that could
 * modify the ValueSet after its creation.
 * 
 * TODO MM
 * itsValues needs only be sorted for compare()/getSmallest() and toString()
 * for contains(String) this is not required
 * a (Linked)HashSet would give O(1) lookup cost
 * a sorted List<String> would give O(log(n)), just as the current TreeSet
 * but at a lower memory cost
 * a String[] would be even more memory efficient
 * 
 * sorted String[] will probably be faster than all other alternatives
 * as the size of ValueSets will not be to big in most cases
 * 
 * compareTo() should do intersection/retainAll() on a List, instead of HashSet 
 */
/**
 * ValueSets are sorted sets that hold a number of <code>String</code> values.
 */
public class ValueSet implements Comparable<ValueSet>
{
	private final SortedSet<String >itsValues;

	/**
	 * Creates a ValueSet, it can not be modified in any way after creation.
	 * 
	 * @param theDomain the values to use for this ValueSet.
	 * 
	 * @throws IllegalArgumentException if theDomain does not contain at
	 * least one value. 
	 */
	public ValueSet(SortedSet<String> theDomain) throws IllegalArgumentException
	{
		// throws a NullPointerException in case of null
		if (theDomain.size() == 0)
			throw new IllegalArgumentException("Domains must be > 0");

		itsValues = new TreeSet<String>(theDomain);
	}

	/**
	 * Returns whether the supplied value is present in this ValueSet.
	 * 
	 * @param theValue the value to check.
	 * 
	 * @return <code>true</code> if this ValueSet contains the supplied
	 * parameter, <code>false</code> otherwise.
	 */
	public boolean contains(String theValue)
	{
		return itsValues.contains(theValue);
	}

	/*
	 * NOTE that there is no real logic in testing just ValueSets, as there
	 * is no information about the Column they are ValueSets of
	 * so it is assumed that, when ValueSets are compared as part of a
	 * Condition comparison, this compareTo() is called on ValueSets that
	 * are build from the same Column
	 */
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
	// throws NullPointerException on null argument
	@Override
	public int compareTo(ValueSet theValueSet)
	{
		if (this == theValueSet)
			return 0;

		// smaller sets come first
		int cmp = this.itsValues.size() - theValueSet.itsValues.size();
		if (cmp != 0)
			return cmp;

		// equal size, determine shared values
		final HashSet<String> i = new HashSet<String>(this.itsValues);
		i.retainAll(theValueSet.itsValues);

		// if all values are in the intersection, ValueSets are equal
		if (itsValues.size() == i.size())
			return 0;

		// sets are of same size, but not all values are shared
		// could be done faster by looking at size of intersection and
		// complement, but this is good enough for now
		return getSmallest(this.itsValues, i).compareTo(getSmallest(theValueSet.itsValues, i));
	}

	// resurrected from r1545
	private static String getSmallest(Set<String> theSet, Set<String> theShared)
	{
		final Iterator<String> i = theSet.iterator();
		String smallest; // SortedSets do not allow nulls (by default)

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
		final int size = itsValues.size();
		if (size == 0)
			return "{}";

		StringBuilder aResult = new StringBuilder(size << 5);
		aResult.append("{");
		final Iterator<String> i = itsValues.iterator();
		aResult.append(i.next());
		while (i.hasNext())
		{
			aResult.append(",");
			aResult.append(i.next());
		}
		return aResult.append("}").toString();
	}
}
