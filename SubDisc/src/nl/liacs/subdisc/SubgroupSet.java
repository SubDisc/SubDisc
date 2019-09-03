package nl.liacs.subdisc;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A SubgroupSet is a <code>TreeSet</code> of {@link Subgroup Subgroup}s. If its
 * size is set to <= 0, the SubgroupSet has no maximum size, else the number of
 * Subgroups it can contain is limited by its size. In a nominal target setting
 * ({@link TargetType}) a
 * {@link ROCList ROCList} can be obtained from this SubgroupSet to create a
 * {@link nl.liacs.subdisc.gui.ROCCurve} in a
 * {@link nl.liacs.subdisc.gui.ROCCurveWindow}.
 *
 * Note that only the add method is thread safe with respect to concurrent
 * access, and possible additions. None of the other methods of this class
 * currently are.
 *
 * @see ROCList
 * @see nl.liacs.subdisc.gui.ROCCurve
 * @see nl.liacs.subdisc.gui.ROCCurveWindow
 * @see Subgroup
 */
public class SubgroupSet extends TreeSet<Subgroup>
{
	private static final long serialVersionUID = 1L;

	// for SubgroupSet in nominal target setting (used for TPR/FPR in ROCList)
	private final boolean nominalTargetSetting;
	private final int itsTotalCoverage;
	private final BitSet itsAllDataBitSet; // for SubgroupDiscovery and Subgroup
	private BitSet itsBinaryTarget;        // no longer final for CAUC
	private int itsMaximumSize;
	private ROCList itsROCList;
	// used as quick check for add(), tests on NaN always return false
	// could use AtomicLong.doubleBits
	private double itsLowestScore = Double.NaN;
	private double itsJointEntropy = Double.NaN; //initially not set

	// this is the long way around, new Subgroups are added to QUEUE
	// when QUEUE.size() >= itsMaximumSize all Subgroups in QUEUE
	// are added to this SubgroupSet, much better for concurrency
	private final int MAX_QUEUE_SIZE = 1; // arbitrarily chosen
	private final BlockingQueue<Subgroup> QUEUE =
			new ArrayBlockingQueue<Subgroup>(MAX_QUEUE_SIZE);

	/* private, meant to be used within this class only */
	private SubgroupSet(int theSize, int theTotalCoverage, BitSet theBinaryTarget, boolean theNominalTargetSetting)
	{
		// basic checks
		if (theTotalCoverage <= 0)
			throw new IllegalArgumentException("SubgroupSet<init>: theTotalCoverage must be > 0, not: " + theTotalCoverage);
		if (theNominalTargetSetting)
		{
			if (theBinaryTarget == null)
				throw new IllegalArgumentException("SubgroupSet<init>: theBinaryTarget can not be null");
			if (theBinaryTarget.length() > theTotalCoverage)
				throw new IllegalArgumentException("SubgroupSet<init>:  theBinaryTarget.length() > theTotalCoverage");
		}
		assert (!theNominalTargetSetting ^ theBinaryTarget != null);

		itsMaximumSize = theSize <= 0 ? Integer.MAX_VALUE : theSize;
		itsTotalCoverage = theTotalCoverage;
		nominalTargetSetting = theNominalTargetSetting;
		itsBinaryTarget = theBinaryTarget;

		BitSet aBitSet = new BitSet(itsTotalCoverage);
		aBitSet.set(0, itsTotalCoverage);
		itsAllDataBitSet = aBitSet;
	}

	/**
	 * Creates a SubgroupSet of a certain size, but in a nominal target
	 * setting theTotalCoverage and theBinaryTarget should also be set.
	 *
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no
	 * maximum size (technically it is limited to Integer.MAX_VALUE).
	 * @param theTotalCoverage the total number of instances in the data
	 * (number of rows in the {@link Table}).
	 * @param theBinaryTarget a {@code BitSet} with {@code bit}s set for the
	 * instances covered by the target value.
	 */
	// TODO optionally this class could take a MINSCORE threshold parameter
	public SubgroupSet(int theSize, int theTotalCoverage)
	{
		this(theSize, theTotalCoverage, null, false);
	}

	/**
	 * Creates a SubgroupSet of a certain size, but in a nominal target
	 * setting theTotalCoverage and theBinaryTarget should also be set.
	 *
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no
	 * maximum size (technically it is limited to Integer.MAX_VALUE).
	 * @param theTotalCoverage the total number of instances in the data
	 * (number of rows in the {@link Table}).
	 * @param theBinaryTarget a {@code BitSet} with {@code bit}s set for the
	 * instances covered by the target value.
	 */
	// TODO optionally this class could take a MINSCORE threshold parameter
	public SubgroupSet(int theSize, int theTotalCoverage, BitSet theBinaryTarget)
	{
		this(theSize, theTotalCoverage, theBinaryTarget, true);
	}

	// package-private, for CAUC(Heavy) setting only (see Process)
	SubgroupSet(Comparator<Subgroup> cmp)
	{
		super(cmp);
		nominalTargetSetting = false;
		itsMaximumSize = Integer.MAX_VALUE;
		itsTotalCoverage = -1;
		itsBinaryTarget = null;
		// FIXME this class has the worst programming available in Cortana FIXME
		itsAllDataBitSet = null;
	}

	/**
	 * Creates a SubgroupSet just like the argument, except empty.
	 * The following members are copied:
	 * itsMaximumSize,
	 * itsTotalCoverage,
	 * itsBinaryTarget,
	 * nominalTargetSetting
	 * itsROCList (shallow copy.)
	 */
	// used by getPatternTeam(), postProcess(), getROCListSubgroupSet()
	/*
	 * FIXME MM some code comment mention something like:
	 * 'create SubgroupSet like this one but empty'
	 * it seems strange to use this method for that purpose, as the ROCList
	 * is copied, and it may very well not be empty
	 * and worse, if not, it is irrelevant to the 'clone' SubgroupSet
	 * it holds no subgroups, so its ROCList based on the Subgroups should
	 * be in line with that, meaning it should be empty
	 * 
	 * to create a clone, use a copy-through-constructor pattern
	 * to create an empty SubgroupSet, create a new one, with arguments
	 * taken from the original if it should be similar to that it
	 * DO NOT mix the two strategies
	 * some of the SubgroupSets that are currently create make no sense
	 * as they are internally inconsistent
	 */
	private SubgroupSet(SubgroupSet theOriginal)
	{
		this(theOriginal.itsMaximumSize,
			theOriginal.itsTotalCoverage,
			theOriginal.itsBinaryTarget,
			theOriginal.nominalTargetSetting);
		//itsTotalTargetCoverage = theOriginal.itsTotalTargetCoverage;
		itsROCList = theOriginal.itsROCList;
		// needs to be set by postProcess()
		//double itsLowestScore = Double.NaN;
	}

	/*
	 * Only the top result is needed in this setting. Setting maximum size
	 * to 1 saves memory and insertion lookup time (Olog(n) for Java's
	 * red-black tree implementation of TreeSet).
	 *
	 * NOTE this is a failed attempt to speedup calculation in the
	 * swap-randomise setting. Storing just the top-1 result is only
	 * sufficient for the last depth.
	 * It may be enabled again in the future.
	 *
	 * LEAVE THIS IN.
	 */
	//protected void useSwapRandomisationSetting() {
	//	itsMaximumSize = 1;
	//}


	/**
	 * Tries to add the {@link Subgroup Subgroup} passed in as parameter to
	 * this SubgroupSet. Also ensures this SubgroupSet never exceeds its
	 * maximum size (if one is set).
	 *
	 * Note that this method is thread safe with respect to concurrent
	 * access, and possible additions. However, none of the other methods of
	 * this class currently are.
	 *
	 * @param theSubgroup theSubgroup to add to this SubgroupSet.
	 *
	 * @return <code>true</code> if this SubgroupSet did not already contain
	 * the specified {@link Subgroup Subgroup}, <code>false</code> if the
	 * Subgroup is <code>null</code>, if its score is lower than the score
	 * of the lowest scoring Subgroup in this SubgroupSet, and if this
	 * SubgroupSet already contains the specified Subgroup.
	 *
	 * NOTE DO NOT RELY ON RETURN VALUE
	 * <code>false</code> means failure,
	 * but <code>true</code> only means the Subgroup is added to the
	 * internal Queue for processing later, it might not get added to this
	 * SubgroupSet.
	 */
	@Override
	public boolean add(Subgroup theSubgroup)
	{
		if (theSubgroup == null)
			return false;
		// avoid log(n) of TreeMap.put() (called by TreeSet.add())
		// NOTE itsLowestScore is un-synchronized / non-volatile
		// so some of these tests may succeed erroneously and the else
		// below is run, this may be faster than synchronized/ volatile
		else if (theSubgroup.getMeasureValue() < itsLowestScore)
			return false;
		else
		{
			/*
			 * using ConcurrentSkipList would be problematic because
			 * of resetting of itsWorseScore as the add and poll
			 * operations of concurrent threads might be interleaved
			 *
			 * similar problems arise from fixed maxSize (when used)
			 * a soft maxSize would handle concurrent adds better
			 *
			 * NOTE calls to add() and size() need to be a compound
			 * action to prevent concurrency related problems
			 * but synchronized(this) would result in concurrent
			 * threads being blocked from calling this method
			 * during the lock
			 * even though they might fail fast (when the score of
			 * the candidate Subgroup is lower than the lowest
			 * scoring Subgroup present in this SubgroupSet)
			 * in the light of concurrent access, a splitting the
			 * check and add into two methods would be a bad idea
			 */
			try { QUEUE.put(theSubgroup); }
			catch (InterruptedException e) { e.printStackTrace(); }
			/*
			 * NOTE drainTo/ addAll do not work, as they calls this
			 * add() method again
			 *
			 * NOTE although MAX_QUEUE_SIZE prevents a lock after
			 * every addition, it may actually be detrimental to
			 * execution speed, as all threads will have to wait
			 * till the QUEUE is completely emptied, update() after
			 * each addition may actually be faster, but there is no
			 * good way (access to massive concurrent systems) to
			 * test this
			 */
			if (QUEUE.size() >= MAX_QUEUE_SIZE)
				update();

			return true;
		}
	}

	public BinaryTable getBinaryTable(Table theTable)
	{
		return new BinaryTable(theTable, this);
	}

	public SubgroupSet getPatternTeam(Table theTable, int k)
	{
		BinaryTable aBinaryTable = getBinaryTable(theTable);
		ItemSet aSubset = aBinaryTable.getApproximateMiki(k);
		// FIXME MM see constructor comment, it is not truly empty
		SubgroupSet aResult = new SubgroupSet(this); //make empty copy
		int index = 0;

		Iterator<Subgroup> anIterator = this.iterator();
		while (anIterator.hasNext())
		{
			Subgroup aSubgroup = anIterator.next();
			if (aSubset.get(index))
				aResult.add(aSubgroup);
			index++;
		}

		aResult.itsJointEntropy = aSubset.getJointEntropy();
		aResult.update();
		return aResult;
	}

	private void update()
	{
		// make all put()'s wait until this QUEUE is empty again
		synchronized (QUEUE)
		{
			while (QUEUE.size() > 0)
			{
				Subgroup s = QUEUE.poll();
				if (s.getMeasureValue() < itsLowestScore)
					QUEUE.clear();
				super.add(s);
			}
			// outside synchronized block leads to troubles if
			// multiple (QUEUE.size() > MAX) call update
			while (itsMaximumSize < super.size())
				remove(last());
			// null safe as itsMaximumSize is always > 0
			if (itsMaximumSize == super.size())
				itsLowestScore = last().getMeasureValue();
		}
	}

	public double getBestScore()
	{
		update();
		return isEmpty() ? Float.NaN : first().getMeasureValue();
	}

	public void setIDs()
	{
		update();
		int aCount = 0;
		for (Subgroup s : this)
			s.setID(++aCount);
	}

	public void print()
	{
		update();
		for (Subgroup s : this)
			Log.logCommandLine(String.format("%d,%d,%d",
								s.getID(),
								s.getCoverage(),
								s.getMeasureValue()));
	}

	public void saveExtent(BufferedWriter theWriter, Table theTable, BitSet theSubset, TargetConcept theTargetConcept)
	{
		update();
		Log.logCommandLine("saving extent...");
		try
		{
			// get SubgroupMembers only once
			List<BitSet> aMembers = new ArrayList<BitSet>(this.size());
			for (Subgroup s : this)
				aMembers.add(s.getMembers());

			// row length = 5 + size()*(,1) + \n
			int aNrChars = this.size()*2 + 6;

			StringBuilder aRow = new StringBuilder(aNrChars);
			aRow.append("test ");
			for (int i = 0, j = this.size(); i < j; ++i)
				aRow.append(",0");
			String aTestRow = aRow.append("\n").toString();

			for (int i = 0, j = theTable.getNrRows(), k = 0; i < j; ++i)
			{
				// add subgroup extents to current row
				// since Cross-Validation Columns are shorter
				// than the original Columns, we need to pad
				if (theSubset.get(i))
				{
					aRow = new StringBuilder(aNrChars);
					aRow.append("train");
					for (BitSet b : aMembers)
						aRow.append(b.get(k) ? ",1" : ",0");
					theWriter.write(aRow.append("\n").toString());
					++k;
				}
				else
					theWriter.write(aTestRow);
			}
		}
		catch (IOException e)
		{
			Log.logCommandLine("SubgroupSet.saveExtent(): error on file: " + e.getMessage());
		}
	}

	/*
	 * ROCList functions.
	 * TODO update a single ROCList instance?
	 */
	/**
	 * Returns a <b>copy of</b> this SubgroupSets' BinaryTarget
	 * <code>BitSet</code>. SubgroupSets only have a BinaryTarget
	 * <code>BitSet<code> in a nominal target setting, meaning the
	 * {@link nl.liacs.subdisc.AttributeType AttributeType} of the
	 * PrimaryTarget in the {@link TargetConcept TargetConcept} is of type
	 * AttributeType.NOMINAL.
	 *
	 * @return a clone of this SubgroupSets' BinaryTarget <code>BitSet</code>,
	 * or <code>null</code> if this SubgroupSet has no BinaryTarget
	 * <code>BitSet</code>.
	 */
	public BitSet getBinaryTargetClone()
	{
		// TODO not so wise may break other code
		//if (!nominalTargetSetting || itsBinaryTarget == null)
		if (itsBinaryTarget == null)
			return null;
		else
			return (BitSet) itsBinaryTarget.clone();
	}

	final BitSet getAllDataBitSetClone()
	{
		return (BitSet) itsAllDataBitSet.clone();
	}

	/**
	 * Destructive method. When called always reset the binary target to its
	 * original state, else all ROC related functionalities break down, and
	 * and probably much more.
	 *
	 * @param theBinaryTarget the new binary target members.
	 */
	public void setBinaryTarget(BitSet theBinaryTarget)
	{
		itsBinaryTarget = theBinaryTarget;
	}

	public int getTotalCoverage() { return itsTotalCoverage; }

	/** Throws NullPointerException when not in a nominal setting. */
	public int getTotalTargetCoverage()
	{
		if (!nominalTargetSetting)
			throw new NullPointerException("only available in nominal setting");
		return itsBinaryTarget.cardinality();
	}

	/*
	 * FIXME MM
	 * this methods return either just this SubgroupSet
	 * of another new one
	 * this is confusing, and not needed
	 * this methods should just update the current instance
	 * however, it is broken anyway
	 * so needs additional updates as well
	 */
	/**
	* Computes the multiplicative weight of a subgroup<br>
	* See van Leeuwen & Knobbe, ECML PKDD 2011.
	*/
	/**
	* Computes the cover count of a particular example: the number of times
	* this example is a member of a subgroup<br>
	* See van Leeuwen & Knobbe, ECML PKDD 2011
	*/
	public SubgroupSet postProcess(SearchStrategy theSearchStrategy)
	{
		update();
		if (theSearchStrategy != SearchStrategy.COVER_BASED_BEAM_SELECTION) //only valid for COVER_BASED_BEAM_SELECTION
			return this;

		int aSize = 100; //TODO
		Log.logCommandLine("subgroups found: " + size());
		// FIXME MM see constructor comment, it is not truly empty
		SubgroupSet aResult = new SubgroupSet(this); //make empty copy
		int aLoopSize = Math.min(aSize, size());
		BitSet aUsed = new BitSet(size());
		for (int i=0; i<aLoopSize; i++)
		{
			Log.logCommandLine("loop " + i);
			Subgroup aBest = null;
			double aMaxQuality = Float.NEGATIVE_INFINITY;
			int aCount = 0;
			int aChosen = 0;
			for (Subgroup aSubgroup : this)
			{
				if (!aUsed.get(aCount)) //is this one still available
				{
					double aQuality = computeMultiplicativeWeight(aResult, aSubgroup) * aSubgroup.getMeasureValue();
					if (aQuality > aMaxQuality)
					{
						aMaxQuality = aQuality;
						/*
						 * MM hunge, to be followed up
						 *
						 * as a result of the line below
						 * the present itsResult can never be removed by GC
						 * as references to it (its members) remain
						 * leading to both the old and new set remaining in memory
						 * for each level
						 * if after the next level ANY of the candidates remain
						 * yet another set will be created, without allowing the GC
						 * to clean up any old one
						 *
						 * 2 strategies to fix this:
						 *
						 * 1.
						 * aBest = aSubgroup.copy()
						 * allows GC to remove the old SubgroupSet
						 * PRO: easy
						 * CON: needs a lot of copying (up to aLoopSize)
						 * 	and (temporarily) a lot of memory
						 *
						 * 2.
						 * explicitly remove all non-used Subgroups from current SubgroupSet
						 * after selecting all relevant ones (outer for-loop completes)
						 * and return this SubgroupSet (make method void :) )
						 *
						 * int i = 0;
						 * for (Subgroup s : this) // uses TreeSet iterator
						 * 	if (!aUsed.get(i))
						 * 		remove(s); // may not work on for-each loop
						 *
						 * but this requires another change:
						 * computeMultiplicativeWeight(SubgroupSet, Subgroup) becomes
						 * computeMultiplicativeWeight(BitSet aUsed, Subgroup)
						 * where the loops in computeMultiplicativeWeight/ computeCover
						 * does its magic only on Subgroups in the 'new SubgroupSet':
						 *
						 * int i = 0;
						 * for (Subgroup s : this)
						 * 	if (aUsed.get(i))
						 * 		doMagic();
						 *
						 * PRO: super efficient
						 * CON: more complex code changes
						 *
						 * NOTE loops can be aborted when
						 * (aMember.nextSetBit() = -1) in computeMultiplicativeWeight
						 * (aUsed.nextSetBit() = -1) in computeCoverCount
						 */
						aBest = aSubgroup;
						aChosen = aCount;
					}
				}
				aCount++;
			}
			Log.logCommandLine("best (" + aChosen + "): " + aBest.getMeasureValue() + ", " + computeMultiplicativeWeight(aResult, aBest) + ", " + aMaxQuality + "\n");
			aUsed.set(aChosen, true);
			aResult.add(aBest);
		}
		aResult.update();
		aResult.itsLowestScore = aResult.last().getMeasureValue();

		Log.logCommandLine("========================================================");
		Log.logCommandLine("used: " + aUsed.toString());
		for (Subgroup aSubgroup : aResult)
			Log.logCommandLine("result: " + aSubgroup.getMeasureValue());
		return aResult;
	}

	/**
	* Computes the cover count of a particular example: the number of times
	* this example is a member of a subgroup.<br>
	* See van Leeuwen & Knobbe, ECML PKDD 2011
	*/
	private int computeCoverCount(SubgroupSet theSet, int theRow)
	{
		int aResult = 0;
		for (Subgroup aSubgroup : theSet)
		{
			if (aSubgroup.covers(theRow))
				aResult++;
		}
		return aResult;
	}

	/**
	* Computes the multiplicative weight of a subgroup.<br>
	* See van Leeuwen & Knobbe, ECML PKDD 2011.
	*/
	private double computeMultiplicativeWeight(SubgroupSet theSet, Subgroup theSubgroup)
	{
		double aResult = 0;
		double anAlpha = 0.9;
		BitSet aMember = theSubgroup.getMembers();

		for(int i=aMember.nextSetBit(0); i>=0; i=aMember.nextSetBit(i+1))
		{
			int aCoverCount = computeCoverCount(theSet, i);
			aResult += Math.pow(anAlpha, aCoverCount);
		}
		return aResult/theSubgroup.getCoverage();
	}

	/**
	 * Returns a new {@link ROCList}. If {@link Subgroup Subgroups} are
	 * removed from this SubgroupSet, this new ROCList reflects these
	 * changes.
	 * This method only returns a ROCList in a nominal target setting,
	 * meaning the {@link nl.liacs.subdisc.AttributeType} of the
	 * {@link TargetConcept#getPrimaryTarget()} in the {@link TargetConcept}
	 *  is of type {@link AttributeType#NOMINAL}.
	 *
	 * @return a ROCList, or <code>null</code> if not in a nominal target
	 * setting.
	 */
	public ROCList getROCList()
	{
		if (!nominalTargetSetting || itsBinaryTarget == null)
			return null;
		else
		{
			update();
			itsROCList = new ROCList(this);

// FIXME MM DEBUG ONLY
if (Process.ROC_BEAM_TEST)
{
Log.logCommandLine("COMPARE ROC HULL CLASSES");
print("ROCList", itsROCList);
ConvexHullROCNaive c = new ConvexHullROCNaive(this);
print("ConvexHullROCNaive", c.itsHull);
c.debug();
ConvexHullROC b = new ConvexHullROC(this);
print("ConvexHullROC", b.itsHull);
b.debug();
ConvexHullROCNaive.debugCompare(itsROCList, c, b);
// AUC
Log.logCommandLine("\nAUC: " + itsROCList.getAreaUnderCurve());
}
			return itsROCList;
		}
	}

	private static final void print(String clazz, List<? extends SubgroupROCPoint> list)
	{
		StringBuilder sb = new StringBuilder(256);
		sb.append(clazz).append("\n");
		for (SubgroupROCPoint s : list)
			sb.append(s.toString()).append("\n");
		Log.logCommandLine(sb.toString());
	}

	/*
	 * solely for ROCCurveWindow
	 * extremely inefficient, should be member of ROCList
	 * could be more efficient when first ordering ROCList
	 * but most ROC code should change as it is overly complex
	 */
	public static final Object[] ROC_HEADER = { "ID", "FPR", "TPR", "Conditions" };
	public Object[][] getROCListSubgroups()
	{
		update();
		int aSize = itsROCList.size();
		Object[][] aSubgroupList = new Object[aSize][ROC_HEADER.length];

		for (int i = 0, j = aSize; i < j; ++i)
		{
			SubgroupROCPoint p = itsROCList.get(i);
			Subgroup s;
			Iterator<Subgroup> it = iterator();

			while ((s = it.next()).getID() < p.ID);

			aSubgroupList[i] =
				new Object[] { s.getID(),
						p.getFPR(),
						p.getTPR(),
						s.getConditions().toString() };
		}

		return aSubgroupList;
	}

	// TODO should me merged with getROCListSubgroups()
	public SubgroupSet getROCListSubgroupSet()
	{
		update();
		int aSize = itsROCList.size();
		// FIXME MM see constructor comment
		// aResult will consist of items in the ROCList
		// but aResult.itsROCList may contain more points / Subgroups
		// that aResult itself
		SubgroupSet aResult = new SubgroupSet(this);

		for (int i = 0, j = aSize; i < j; ++i)
		{
			SubgroupROCPoint p = itsROCList.get(i);
			Subgroup s;
			Iterator<Subgroup> it = iterator();

			while ((s = it.next()).getID() < p.ID); // <- NOTE ;

			aResult.add(s);
		}

		return aResult;
	}

	/*
	 * NOTE since the copy-through-constructor creates a shallow copy of
	 * itsROCList, calling this method may have side effects.
	 * All clones of this SubgroupSet will be affected.
	 */
	@Override
	public void clear()
	{
		super.clear();
		if (itsROCList != null)
			itsROCList.clear();
		itsLowestScore = Double.NaN;
		itsJointEntropy = Double.NaN;
	}

	@Override
	public int size()
	{
		update();
		return super.size();
	}

	public double getJointEntropy() { return itsJointEntropy; }

	public static final boolean CHECK_FOR_ALTERNATIVE_DESCRIPTIONS = false;
	public final void markAlternativeDescriptions()
	{
		if (!CHECK_FOR_ALTERNATIVE_DESCRIPTIONS)
			return;

		update();

		if (this.size() <= 1)
			return;

		// first select all rows that might be duplicates based on cqst:
		//   Coverage+Quality+Probability+Positives (nominal)
		//   Coverage+Quality+Average    +St. Dev.  (numeric)
		// if unique                               -> write line
		// else canonicalise these lines
		//   for all lines that might be duplicates:
		//     if later lines reduce to an earlier -> write DELETE_SYMBOL + line
		//     else if later is subsets of earlier -> write DELETE_SYMBOL + line
		//     else                                -> write line
		List<Subgroup> aSameCQST = new ArrayList<Subgroup>();

		// fake this one - could pass itsAllDataBitSet, but clone is more safe
		Subgroup aLast = new Subgroup(ConditionListBuilder.emptyList(), getAllDataBitSetClone(), this);
		// compares false for first Subgroup, even if its score is NaN
		aLast.setMeasureValue(Double.NaN);

		for (Subgroup s : this)
		{
			// FIXME MM overwrite any value that might be present, abuse p-value
			s.setPValue(Double.NaN);

			boolean hasSameCQPP =
				(aLast.getCoverage() == s.getCoverage()) &&
				(aLast.getMeasureValue() == s.getMeasureValue()) &&
				(aLast.getSecondaryStatistic() == s.getSecondaryStatistic()) &&
				(aLast.getTertiaryStatistic() == s.getTertiaryStatistic());

			if (!hasSameCQPP && aSameCQST.isEmpty())
			{
				aLast = s;
				continue;
			}
			else if (!hasSameCQPP && !aSameCQST.isEmpty())
			{
				// new CQPP found, but all lines in
				// sameCQPP need to be processed first
				markAlternativeDescriptionsProcess(aSameCQST);
				aSameCQST.clear();
				aLast = s;
				continue;
			}
			else if (hasSameCQPP && aSameCQST.isEmpty())
			{
				// first duplicate, so add previous also
				aSameCQST.add(aLast);
				aSameCQST.add(s);
			}
			else // (hasSameCQPP && !sameCQPP.isEmpty())
			{
				// multiple duplicates, add current too
				aSameCQST.add(s);
			}
		}
	}

	private static final void markAlternativeDescriptionsProcess(List<Subgroup> sameCQST)
	{
		for (int i = 0, j = sameCQST.size(); i < j-1; ++i)
		{
			Subgroup si = sameCQST.get(i);
			if (!Double.isNaN(si.getPValue()))
				continue;

			// set to lowest id, possibly multiple times for si
			int id = si.getID();

			for (int k = i+1; k < j; ++k)
			{
				Subgroup sk = sameCQST.get(k);
				if (!Double.isNaN(sk.getPValue()))
					continue;

				if (!si.getMembers().equals(sk.getMembers()))
					continue;

				// of equal-scoring Subgroups in a SubgroupSet those with the
				// smaller ConditionListA come first
				assert (si.getConditions().size() <= sk.getConditions().size());
				if (ConditionListBuilder.isCanonicalisedProperSubSetOf(si.getConditions(), sk.getConditions()))
					if (id > 0)
						id = -id;

				si.setPValue(id);
				sk.setPValue(id);
			}
		}
	}
}
