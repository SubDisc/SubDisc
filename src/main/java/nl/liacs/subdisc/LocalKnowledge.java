package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.ConditionListBuilder.ConditionList;

public class LocalKnowledge
{
	////////////////////////////////////////////////////////////////////////////
	///// NEW VERSION - CLEAN AND USING ConditionList /////////////////////////
	///// TODO - check with Rob whether HashSet would not give wrong results ///
	////////////////////////////////////////////////////////////////////////////
	// FIXME make final after testing
	private Map<Column, List<ConditionList>>        itsColumnToConditionListMap;
	private Map<ConditionList, BitSet>              itsConditionListToBitSetMap;
	private Map<ConditionList, StatisticsBayesRule> itsConditionListBayesRuleMap;

	// FIXME pretty large for a constructor
	public LocalKnowledge(BitSet theTarget, List<ConditionList> theExplanatoryConditions)
	{
		if (theExplanatoryConditions.size() == 0)
		{
			itsColumnToConditionListMap  = Collections.emptyMap();
			itsConditionListToBitSetMap  = Collections.emptyMap();
			itsConditionListBayesRuleMap = Collections.emptyMap();
			return;
		}

		Map<Column, List<ConditionList>>        cm = new HashMap<>();
		Map<ConditionList, BitSet>              bm = new HashMap<>();
		Map<ConditionList, StatisticsBayesRule> sm = new HashMap<>();

		for (ConditionList cl : theExplanatoryConditions)
		{
			for (int i = 0, j = cl.size(); i < j; ++i)
			{
				Column c = cl.get(i).getColumn();
				Log.logCommandLine("Local Knowledge variables");
				Log.logCommandLine(c.getName());

				List<ConditionList> l = cm.get(c);
				if (l == null)
				{
					l = new ArrayList<>();
					cm.put(c, l);
				}

				l.add(cl);
			}
		}

		int aNrRows = theExplanatoryConditions.get(0).get(0).getColumn().size();
		BitSet all  = new BitSet(aNrRows);
		all.set(0, aNrRows);

		int aTargetCardilaity = theTarget.cardinality();
		for (ConditionList cl: theExplanatoryConditions)
		{
			// BitSet of ConditionList, also set the size, and all bits to 1
			BitSet bs = (BitSet) all.clone();

			for (int i = 0, j = cl.size(); i < j; ++i)
			{
				Condition c = cl.get(i);
				BitSet aBitSetCondition = c.getColumn().evaluate(all, c); // all
				bs.and(aBitSetCondition);
				print("condition", aBitSetCondition.cardinality());
			}

			bm.put(cl, bs);
			print("target", aTargetCardilaity); // MM - why, it never changes
			print("known subgroup", bs.cardinality());
			sm.put(cl, new StatisticsBayesRule(bs,theTarget));
		}

		itsColumnToConditionListMap  = Collections.unmodifiableMap(cm);
		itsConditionListToBitSetMap  = Collections.unmodifiableMap(bm);
		itsConditionListBayesRuleMap = Collections.unmodifiableMap(sm);
	}

	// returns BitSets corresponding to attributes involved in Subgroup
	public Set<BitSet> getBitSets(ConditionList theConditions)
	{
		Set<BitSet> s = new HashSet<BitSet>();

		for (ConditionList cl : getConditionLists(theConditions, itsColumnToConditionListMap, false))
			s.add(itsConditionListToBitSetMap.get(cl));

		return s;
	}

	// returns Statistics corresponding to attributes involved in Subgroup
	public Set<StatisticsBayesRule> getStatisticsBayesRule(ConditionList theConditions)
	{
		Set<StatisticsBayesRule> s = new HashSet<StatisticsBayesRule>();

		for (ConditionList cl : getConditionLists(theConditions, itsColumnToConditionListMap, true))
			s.add(itsConditionListBayesRuleMap.get(cl));

		return s;
	}

	// get ConditionLists that are involved with the Subgroup.ConditionList
	private static final Set<ConditionList> getConditionLists(ConditionList theConditions, Map<Column, List<ConditionList>> theMap, boolean isBayes)
	{
		Set<ConditionList> s = new HashSet<ConditionList>();

		for (int i = 0, j = theConditions.size(); i < j; ++i)
		{
			List<ConditionList> l = theMap.get(theConditions.get(i).getColumn());
			if (l != null)
				s.addAll(l);

			// FIXME MM - delete print after comparison - why was it here anyway
			if (isBayes)
				Log.logCommandLine(theConditions.get(i).getColumn().getName());
		}

		return s;
	}

	private static final void print(String theSubject, int theCardinality)
	{
		// FIXME after comparing, change to single line output
		Log.logCommandLine(String.format("cardinality %s%n%d", theSubject, theCardinality));
	}
}
