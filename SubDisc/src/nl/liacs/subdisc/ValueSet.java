package nl.liacs.subdisc;

import java.util.*;

public class ValueSet extends ArrayList<String> implements List<String>
{
	private static final long serialVersionUID = 1L;

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
