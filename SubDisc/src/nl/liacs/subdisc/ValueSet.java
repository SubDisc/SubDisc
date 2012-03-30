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
}
