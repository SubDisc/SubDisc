package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.ConditionListBuilder.ConditionListA;

public class LocalKnowledge
{
//	private List<ConditionList> itsExplanatoryConditions;
//	//private String[][] itsColumnsInvolved; //describes the columns that are involved in a subgroup 
//	//private BitSet[] tableWithExplanatoryVariables; //a table containing the members of each condition. 
//	private Map<Column, List<ConditionList>> mapColumnToConditionList;
//	//private Map<Column, List<BitSet>> mapColumnToBitSet;
//	private Map<ConditionList, BitSet> mapConditionListToBitSet;
//	//each knowledge component is described by one (or more) conditions from the condition list
//	private Map<ConditionList, StatisticsBayesRule> mapConditionListBayesRule;
//	private BitSet itsTarget;
//
//	public LocalKnowledge(List<ConditionList> theExplanatoryConditions, BitSet theTarget)
//	{
//		itsExplanatoryConditions = theExplanatoryConditions;
//		itsTarget = theTarget;
//
//		mapColumnToConditionList = new HashMap<Column, List<ConditionList>>();
//		for (ConditionList cl : itsExplanatoryConditions)
//		{
//			for (Condition c : cl)
//			{
//				System.out.println("Local Knowledge variables");
//				System.out.println(c.getColumn().getName());
//				if (!mapColumnToConditionList.containsKey(c.getColumn()))
//				{
//					List<ConditionList> aList = new ArrayList<ConditionList>();
//					aList.add(cl);
//					mapColumnToConditionList.put(c.getColumn(), aList);
//				}
//				else
//				{
////					List<ConditionList> aList = mapColumnToConditionList.get(c.getColumn());
////					aList.add(cl);
////					mapColumnToConditionList.put(c.getColumn(), aList);
//					mapColumnToConditionList.get(c.getColumn()).add(cl);
//				}
//			}
//		}
//
//		mapConditionListToBitSet = new HashMap<ConditionList, BitSet>();
//		mapConditionListBayesRule = new HashMap<ConditionList, StatisticsBayesRule>();
//
//		for (ConditionList cl: itsExplanatoryConditions)
//		{
//			BitSet aBitSetCl = new BitSet(cl.get(0).getColumn().size()); // the bit set of the conditionlist, also set the size here, and all bits to one
//			aBitSetCl.set(0, cl.get(0).getColumn().size());// or col.size -1? 
//			for (Condition c : cl)
//			{
//				BitSet aBitSetCondition = c.getColumn().evaluate(c);
//				System.out.println("cardinality condition");
//				System.out.println(aBitSetCondition.cardinality());
//				aBitSetCl.and(aBitSetCondition);
//			}
//
//			mapConditionListToBitSet.put(cl,aBitSetCl);
//			// now calculate the statistics for Bayes Rule
//			System.out.println("cardinality target");
//			System.out.println(itsTarget.cardinality());
//			System.out.println("cardinality known subgroup");
//			System.out.println(aBitSetCl.cardinality());
//			StatisticsBayesRule aStatisticsBR = new StatisticsBayesRule(aBitSetCl,itsTarget);
//			mapConditionListBayesRule.put(cl, aStatisticsBR);
//		}
//	}//constructor
//
///*
//		mapColumnToBitSet = new HashMap<Column, List<BitSet>>();
//		for (Column col : mapColumnToConditionList.keySet()){
//			//if (mapColumnToConditionList.containsKey(c))
//			List<ConditionList> aListOfCl;
//			aListOfCl = mapColumnToConditionList.get(col); 
//			List<BitSet> aList = new ArrayList<BitSet>();
//			for (ConditionList cl : aListOfCl){
//				BitSet aBitSetCl = new BitSet(col.size()); // the bit set of the conditionlist, also set the size here, and all bits to one
//				aBitSetCl.set(0, col.size());// or col.size -1? 
//				for (Condition c : cl){
//					BitSet aBitSetCondition = new BitSet();
//					aBitSetCondition = c.getColumn().evaluate(c);
//					aBitSetCl.and(aBitSetCondition);
//				}
//				aList.add(aBitSetCl);
//			}
//			mapColumnToBitSet.put(col, aList);
//		}
//*/
//
///*
//		itsColumnsInvolved = new String[explanatoryConditions.length][];
//		for (int i=0; i<explanatoryConditions.length; i++){
//			itsColumnsInvolved[i] = new String[itsExplanatoryConditions[i].size()];
//		}
//		// fill list with strings to match later
//		for (int i=0; i<explanatoryConditions.length; i++){
//			for (int j=1;j<itsExplanatoryConditions[i].size();j++)
//			{
//				itsColumnsInvolved[i][j] = itsExplanatoryConditions[i].get(j).getColumn().getName();
//			}
//		}
//	*/
//
///*
//	public BitSet[] getBitSetsExplanatoryConditions(String[] attributeNames){ //returns an array of explanatory variables to be used as input into a global model estimator.
//		//get dummy variables as input for logistic regression from known subgroups.
//		
//		BitSet[] aBitSetsExplanatoryConditions;
//		aBitSetsExplanatoryConditions = new BitSet[itsExplanatoryConditions.length];
//		for (int i=1;i<itsExplanatoryConditions.length;i++){
//			aBitSetsExplanatoryConditions[i] =  itsExplanatoryConditions[i].getMembers();
//		}
//	return aBitSetsExplanatoryConditions;
//	}
//*/
//
//	//Use HashSet or List??
//	public Set<BitSet> getBitSets(Subgroup theSubgroupToEvaluate)
//	{
//		//returns bitsets corresponding to the attributes that are involved in the subgroup
//		Set<BitSet> aBitSetsExplanatoryConditionLists = new HashSet<BitSet>();
//		Set<ConditionList> aConditionListsInvolvedWithColumn = new HashSet<ConditionList>();
//		// First obtain conditionLists that are involved with the subgroup
////		for (Condition c : theSubgroupToEvaluate.getConditions())
////			if (mapColumnToConditionList.get(c.getColumn()) != null)
////				aConditionListsInvolvedWithColumn.addAll(mapColumnToConditionList.get(c.getColumn()));
//		ConditionListA aConditions = theSubgroupToEvaluate.getConditions();
//		for (int i = 0, j = aConditions.size(); i < j; ++i)
//		{
//			Condition c = aConditions.get(i);
//			if (mapColumnToConditionList.get(c.getColumn()) != null)
//				aConditionListsInvolvedWithColumn.addAll(mapColumnToConditionList.get(c.getColumn()));
//		}
//
//		//now get the bitsets from  conditionLists involved from the mapping 
//		for (ConditionList cl : aConditionListsInvolvedWithColumn)
//			aBitSetsExplanatoryConditionLists.add(mapConditionListToBitSet.get(cl));
//
//		return aBitSetsExplanatoryConditionLists;
//	}
//
//	public Set<StatisticsBayesRule> getStatisticsBayesRule(Subgroup theSubgroupToEvaluate)
//	{
//		//returns statistics corresponding to the attributes that are involved in the subgroup
//		Set<StatisticsBayesRule> aSetStatisticsBayesRule = new HashSet<StatisticsBayesRule>();
//		Set<ConditionList> aConditionListsInvolvedWithColumn = new HashSet<ConditionList>();
//		// First obtain conditionLists that are involved with the subgroup
////		for (Condition c : theSubgroupToEvaluate.getConditions())
////		{
////			System.out.println(c.getColumn().getName());
////			if (mapColumnToConditionList.get(c.getColumn()) != null)
////				aConditionListsInvolvedWithColumn.addAll(mapColumnToConditionList.get(c.getColumn()));
////
////		}
//		ConditionListA aConditions = theSubgroupToEvaluate.getConditions();
//		for (int i = 0, j = aConditions.size(); i < j; ++i)
//		{
//			Condition c = aConditions.get(i);
//			System.out.println(c.getColumn().getName());
//			if (mapColumnToConditionList.get(c.getColumn()) != null)
//				aConditionListsInvolvedWithColumn.addAll(mapColumnToConditionList.get(c.getColumn()));
//		}
//
//		//now get the statistics from  conditionLists involved from the mapping 
//		for (ConditionList cl : aConditionListsInvolvedWithColumn)
//			aSetStatisticsBayesRule.add(mapConditionListBayesRule.get(cl));
//
//		return aSetStatisticsBayesRule;
//	}
//
	////////////////////////////////////////////////////////////////////////////
	///// NEW VERSION - CLEAN AND USING ConditionListA /////////////////////////
	///// TODO - check with Rob whether HashSet would not give wrong results ///
	////////////////////////////////////////////////////////////////////////////
	// FIXME make final after testing
	private Map<Column, List<ConditionListA>>        itsColumnToConditionListMap;
	private Map<ConditionListA, BitSet>              itsConditionListToBitSetMap;
	private Map<ConditionListA, StatisticsBayesRule> itsConditionListBayesRuleMap;

	// FIXME pretty large for a constructor
	public LocalKnowledge(BitSet theTarget, List<ConditionListA> theExplanatoryConditions)
	{
		if (theExplanatoryConditions.size() == 0)
		{
			itsColumnToConditionListMap  = Collections.emptyMap();
			itsConditionListToBitSetMap  = Collections.emptyMap();
			itsConditionListBayesRuleMap = Collections.emptyMap();
			return;
		}

		Map<Column, List<ConditionListA>>        cm = new HashMap<>();
		Map<ConditionListA, BitSet>              bm = new HashMap<>();
		Map<ConditionListA, StatisticsBayesRule> sm = new HashMap<>();

		for (ConditionListA cl : theExplanatoryConditions)
		{
			for (int i = 0, j = cl.size(); i < j; ++i)
			{
				Column c = cl.get(i).getColumn();
				Log.logCommandLine("Local Knowledge variables");
				Log.logCommandLine(c.getName());

				List<ConditionListA> l = cm.get(c);
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
		for (ConditionListA cl: theExplanatoryConditions)
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
	public Set<BitSet> getBitSets(ConditionListA theConditions)
	{
		Set<BitSet> s = new HashSet<BitSet>();

		for (ConditionListA cl : getConditionLists(theConditions, itsColumnToConditionListMap, false))
			s.add(itsConditionListToBitSetMap.get(cl));

		return s;
	}

	// returns Statistics corresponding to attributes involved in Subgroup
	public Set<StatisticsBayesRule> getStatisticsBayesRule(ConditionListA theConditions)
	{
		Set<StatisticsBayesRule> s = new HashSet<StatisticsBayesRule>();

		for (ConditionListA cl : getConditionLists(theConditions, itsColumnToConditionListMap, true))
			s.add(itsConditionListBayesRuleMap.get(cl));

		return s;
	}

	// get ConditionLists that are involved with the Subgroup.ConditionListA
	private static final Set<ConditionListA> getConditionLists(ConditionListA theConditions, Map<Column, List<ConditionListA>> theMap, boolean isBayes)
	{
		Set<ConditionListA> s = new HashSet<ConditionListA>();

		for (int i = 0, j = theConditions.size(); i < j; ++i)
		{
			List<ConditionListA> l = theMap.get(theConditions.get(i).getColumn());
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
