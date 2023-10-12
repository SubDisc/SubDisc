package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.ConditionListBuilder.ConditionList;

public class GlobalKnowledge
{
	private Set<BitSet> itsBitSets;
	private Set<StatisticsBayesRule> itsStatisticsBayesRules;

	/** This does not modify the BitSet argument. */
	// TODO split this constructor, only one of the two Sets is required
	public GlobalKnowledge(BitSet theTarget, List<ConditionList> theExplanatoryConditions)
	{
		if (theExplanatoryConditions.size() == 0)
		{
			itsBitSets              = Collections.emptySet();
			itsStatisticsBayesRules = Collections.emptySet();
			return;
		}

		int aNrRows = theExplanatoryConditions.get(0).get(0).getColumn().size();
		BitSet all  = new BitSet(aNrRows);
		all.set(0, aNrRows);

		Set<BitSet> b              = new HashSet<>();
		Set<StatisticsBayesRule> s = new HashSet<>();

		for (ConditionList cl: theExplanatoryConditions)
		{
			// BitSet of ConditionList, also set the size, and all bits to 1
			BitSet bs = (BitSet) all.clone();

			for (int i = 0, j = cl.size(); i < j; ++i)
			{
				Condition c = cl.get(i);
				bs = c.getColumn().evaluate(bs, c);
			}

			b.add(bs);
			s.add(new StatisticsBayesRule(bs, theTarget));
		}

		itsBitSets              = Collections.unmodifiableSet(b);
		itsStatisticsBayesRules = Collections.unmodifiableSet(s);
	}

	public Set<BitSet> getBitSets2() { return itsBitSets; }
	public Set<StatisticsBayesRule> getStatisticsBayesRule2() { return itsStatisticsBayesRules; }
}
