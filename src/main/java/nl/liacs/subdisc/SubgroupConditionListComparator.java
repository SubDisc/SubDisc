package nl.liacs.subdisc;

import java.util.*;

public class SubgroupConditionListComparator implements Comparator<Subgroup>
{
	// throws a NullPointerException when either argument is null
	@Override
	public int compare(Subgroup x, Subgroup y)
	{
		return x.getConditions().compareTo(y.getConditions());
	}
}
