package nl.liacs.subdisc;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A SubgroupSet is a <code>TreeSet</code> of {@link Subgroup Subgroup}s. If its size is set to <= 0, the SubgroupSet has no maximum size, else the number of Subgroups it can contain is limited by its size. 
 * In a nominal target setting
 * ({@link TargetType}) a
 * {@link ROCList ROCList} can be obtained from this SubgroupSet to create a
 * {@link nl.liacs.subdisc.gui.ROCCurve} in a
 * {@link nl.liacs.subdisc.gui.ROCCurveWindow}.
 *
 * Note that only the add method is thread safe with respect to concurrent access, and possible additions. None of the other methods of this class currently are.
 *
 * @see ROCList
 * @see nl.liacs.subdisc.gui.ROCCurve
 * @see nl.liacs.subdisc.gui.ROCCurveWindow
 * @see Subgroup
 */

public class SubgroupSet extends TreeSet<Subgroup>
{
	private static final long serialVersionUID = 1L;
	// the method is fundamentally flawed (in paper), and the code has a bug
	private static final boolean USE_OLD_COVER_BASED_SUBGROUP_SELECTION = false;
	// hard-code top-k, for CBSS maximum_subgroups must be set to 0 (unlimited)
	private static final int COVER_BASED_SUBGROUP_SELECTION_TOP_K = 100;

	// for SubgroupSet in nominal target setting (used for TPR/FPR in ROCList)
	private final boolean nominalTargetSetting;
	private final int itsNrRows; //size of the original table, regardless of selection
	private int itsTotalCoverage; //size of the dataset used to produce this SS, either the original table or a selection
	private final BitSet itsAllDataBitSet; // for SubgroupDiscovery and Subgroup
	private BitSet itsBinaryTarget;        // no longer final for CAUC
	private int itsMaximumSize;
	private ROCList itsROCList;
	// used as quick check for add(), tests on NaN always return false
	// could use AtomicLong.doubleBits
	private double itsLowestScore = Double.NaN;
	private double itsJointEntropy = Double.NaN; //initially not set

	private BinaryTable itsBinaryTable = null;

	// this is the long way around, new Subgroups are added to QUEUE when QUEUE.size() >= itsMaximumSize all Subgroups in QUEUE are added to this SubgroupSet, much better for concurrency
	private final int MAX_QUEUE_SIZE = 1; // arbitrarily chosen
	private final BlockingQueue<Subgroup> QUEUE = new ArrayBlockingQueue<Subgroup>(MAX_QUEUE_SIZE);

	/* private, meant to be used within this class only */
	private SubgroupSet(int theSize, BitSet theSelection, int theNrRows, BitSet theBinaryTarget, boolean theNominalTargetSetting)
	{
		itsMaximumSize = theSize <= 0 ? Integer.MAX_VALUE : theSize;
		itsNrRows = theNrRows;
		nominalTargetSetting = theNominalTargetSetting;
		itsBinaryTarget = theBinaryTarget;

		if (theSelection == null)
		{
			BitSet aBitSet = new BitSet(itsNrRows);
			aBitSet.set(0, itsNrRows);
			itsAllDataBitSet = aBitSet;
			itsTotalCoverage = itsNrRows;
		}
		else
		{
			itsAllDataBitSet = theSelection;
			if (itsBinaryTarget != null)
				itsBinaryTarget.and(theSelection); 
			itsTotalCoverage = theSelection.cardinality();
		}
	}

	/**
	 * Creates a SubgroupSet of a certain size, but in a nominal target setting theTotalCoverage and theBinaryTarget should also be set.
	 *
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no maximum size (technically it is limited to Integer.MAX_VALUE).
	 * @param theTotalCoverage the total number of instances in the data (number of rows in the {@link Table}).
	 * @param theBinaryTarget a {@code BitSet} with {@code bit}s set for the instances covered by the target value.
	 */
	public SubgroupSet(int theSize, BitSet theSelection, int theTotalCoverage)
	{
		this(theSize, theSelection, theTotalCoverage, null, false);
	}

	/**
	 * Creates a SubgroupSet of a certain size, but in a nominal target setting theTotalCoverage and theBinaryTarget should also be set.
	 *
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no maximum size (technically it is limited to Integer.MAX_VALUE).
	 * @param theTotalCoverage the total number of instances in the data (number of rows in the {@link Table}).
	 * @param theBinaryTarget a {@code BitSet} with {@code bit}s set for the instances covered by the target value.
	 */
	public SubgroupSet(int theSize, BitSet theSelection, int theTotalCoverage, BitSet theBinaryTarget)
	{
		this(theSize, theSelection, theTotalCoverage, theBinaryTarget, true);
	}

	// package-private, for CAUC(Heavy) setting only (see Process)
	SubgroupSet(Comparator<Subgroup> cmp)
	{
		super(cmp);
		nominalTargetSetting = false;
		itsMaximumSize = Integer.MAX_VALUE;
		itsNrRows = -1;
		itsBinaryTarget = null;
		// FIXME this class has the worst programming available in SubDisc FIXME
		itsAllDataBitSet = null;
	}

	/**
	 * Creates a SubgroupSet just like the argument, except empty.
	 * The following members are copied:
	 * itsMaximumSize,
	 * itsNrRows,
	 * itsBinaryTarget,
	 * nominalTargetSetting
	 * itsROCList (shallow copy.)
	 */
	// used by getPatternTeam(), postProcess(), getROCListSubgroupSet()
	/*
	 * FIXME MM some code comment mention something like:
	 * 'create SubgroupSet like this one but empty'
	 * it seems strange to use this method for that purpose, as the ROCList is copied, and it may very well not be empty and worse, if not, it is irrelevant to the 'clone' SubgroupSet
	 * it holds no subgroups, so its ROCList based on the Subgroups should be in line with that, meaning it should be empty
	 * 
	 * to create a clone, use a copy-through-constructor pattern
	 * to create an empty SubgroupSet, create a new one, with arguments taken from the original if it should be similar to that it
	 * DO NOT mix the two strategies
	 * some of the SubgroupSets that are currently create make no sense as they are internally inconsistent
	 */
	private SubgroupSet(SubgroupSet theOriginal)
	{
		this(theOriginal.itsMaximumSize, null, theOriginal.itsNrRows, theOriginal.itsBinaryTarget, theOriginal.nominalTargetSetting);
		itsROCList = theOriginal.itsROCList;
		itsTotalCoverage = theOriginal.itsTotalCoverage;
	}

	/*
	 * Only the top result is needed in this setting. Setting maximum size to 1 saves memory and insertion lookup time (Olog(n) for Java's red-black tree implementation of TreeSet).
	 *
	 * NOTE this is a failed attempt to speedup calculation in the swap-randomise setting. Storing just the top-1 result is only sufficient for the last depth.
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

	// includes equal scores, as new Subgroups with the same score might be
	// ordered before the old one, pushing the old one out
	// note that this is an unsynchronised check, might use AtomicDouble one day
	// isNaN() check is needed as itsLowestScore starts out like that
	// FIXME use isEmpty() instead of NaN check
	boolean hasPotential(float theQuality)
	{
		return (Double.isNaN(itsLowestScore) || (theQuality >= itsLowestScore));
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

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// general methods to return information about this instance        /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	//@return the total number of rows in the tabel, regardless of any selection
	public int getNrRows()
	{
		return itsNrRows;
	}

	//@return the total number of examples involved in the mining process, either all examples in the tabel, or those in the specified selection
	public int getTotalCoverage()
	{
		return itsTotalCoverage;
	}

	public int getTotalTargetCoverage()
	{
		return itsBinaryTarget.cardinality();
	}

	final BitSet getAllDataBitSetClone()
	{
		return (BitSet) itsAllDataBitSet.clone();
	}

	public double getBestScore()
	{
		update();
		return isEmpty() ? Double.NaN : first().getMeasureValue();
	}

	/**
	 * Destructive method. When called always reset the binary target to its original state, else all ROC related functionalities break down, and and probably much more.
	 *
	 * @param theBinaryTarget the new binary target members.
	 */
	public void setBinaryTarget(BitSet theBinaryTarget)
	{
		itsBinaryTarget = theBinaryTarget;
	}

	// FIXME this is why a SubgroupSet should NOT extend a TreeSet: encapsulate
	/*
	 * NOTE since the copy-through-constructor creates a shallow copy of itsROCList, calling this method may have side effects.
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

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// start of post-processing code                                    /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

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
		if (theSearchStrategy != SearchStrategy.COVER_BASED_BEAM_SELECTION)
			return this;

		if (!USE_OLD_COVER_BASED_SUBGROUP_SELECTION)
		{
			// FIXME MM see constructor comment, it is not truly empty
			SubgroupSet aCopy = new SubgroupSet(this); //make empty copy
			for (Candidate c : CoverBasedSubgroupSelection.postProcessResultSet(this, COVER_BASED_SUBGROUP_SELECTION_TOP_K))
				aCopy.add(c.getSubgroup()); // add not linear, but fine for now
			aCopy.update();
			aCopy.itsLowestScore = aCopy.last().getMeasureValue();

			return aCopy;
		}

		// old code starts here
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

	public void setIDs()
	{
		update();
		int aCount = 0;
		for (Subgroup s : this)
			s.setID(++aCount);
	}

	// obviously should not be called while mining...
	/** topK must be > 0, only joint entropy is returned (CR is printed) */
	public double postProcessGetCoverRedundancyAndJointEntropy(int topK)
	{
		if (topK <= 0)
			throw new IllegalArgumentException(getClass().getName() + ".postProcessGetCoverRedundancyAndJointEntropy() invalid topK: " + topK);

		update();

		int max = Math.min(topK, super.size());
		if (max == 0) // when size == 0
			return 0.0;

		BitSet[] aRows      = new BitSet[itsNrRows];
		int[] aCoverCounts  = new int[itsNrRows];
		long aCoverCountSum = 0L;
		for (int i=0; i<itsNrRows; i++)
			aRows[i] = new BitSet(max);

		int idx = -1;
		for (Subgroup s : this)
		{
			if (++idx == max)
				break;

			BitSet b = s.getMembers();
			// no s.killMembers(); when called from ResultWindow members are set
			// because a Table modification would make it impossible to evaluate
			// the Subgroups (due to changed missing value, AttributeType, ...)

			for (int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i+1))
			{
				aRows[i].set(idx);
				++aCoverCounts[i];
				++aCoverCountSum;
			}
		}

		double aTotalCount          = itsNrRows;
		double anExpectedCoverCount = (aCoverCountSum / aTotalCount);
		double aCoverRedundancy     = 0.0;
		for (int i : aCoverCounts)
			aCoverRedundancy += (Math.abs(i - anExpectedCoverCount));
		aCoverRedundancy = (aCoverRedundancy/anExpectedCoverCount/aTotalCount);

		// TODO
		// when there are many duplicates, a HashSet/TreeSet might be more
		// efficient, as they allow to prevent duplicate rows permutations
		// but these Sets (or Map<rowPermutation, count>) use a lot of memory
		// and are often not faster, because of array data locality
		// so the current n log n sorting + n count loop might well be faster
		// than a Hash structure based O(n + n)
		// a Tree would be O(n log m + n), where m = nrDistinctRowsPermutations
		// it would need proper profiling to find the trade-off
		BitSetComperator bc = new BitSetComperator();
		Arrays.sort(aRows, bc);

		double anEntropy = 0.0;
		for (int i = 0, j = 0; ; )
		{
			if (bc.compare(aRows[i], aRows[j]) != 0)
			{
				double aFraction = (j-i) / aTotalCount;
				anEntropy += (-aFraction * Math.log(aFraction));
				i = j;
			}
			else if (++j == itsNrRows)
			{
				double aFraction = (j-i) / aTotalCount;
				anEntropy += (-aFraction * Math.log(aFraction));
				break;
			}
		}
		anEntropy /= Math.log(2.0);

		// prints topK (not max), fixed value is easier for log file parsing
		// the value for max is logged as:
		//   for !CBSS: number of subgroups
		//   for  CBSS: NR CANDIDATES FOR NEXT LEVEL (last entry, with a -)
//		Log.logCommandLine(String.format("CR(%d)=%f\tH(%1$d)=%f", max, aCoverRedundancy, anEntropy));
		Log.logCommandLine(String.format("CCSUM=%d\tN=%d\tCCEXPECTED=%f\tCR(%d)=%f\tH(%4$d)=%f", aCoverCountSum, itsNrRows, anExpectedCoverCount, topK, aCoverRedundancy, anEntropy));

		return anEntropy;
	}

	private static final class BitSetComperator implements Comparator<BitSet>
	{
		@Override
		public int compare(BitSet a, BitSet b)
		{
			if (a == b)
				return 0;

			int cmp = (a.cardinality() - b.cardinality());
			if (cmp != 0)
				return cmp;

			// NOTE size could be larger than topK, but BitSets will be replaced
			for (int i = 0, j = a.size(); i < j; ++i)
			{
				boolean ai = a.get(i);
				if (ai ^ b.get(i))
					return (ai ? -1 : 1);
			}

			// all bits the same, enforce total ordering not required for array
			return 0;
		}
	}

	public SubgroupSet getPatternTeam(Table theTable, int k)
	{
		update();

		itsBinaryTable = new BinaryTable(theTable, this);
		ItemSet aSubset = itsBinaryTable.getApproximateMiki(k);

		SubgroupSet aResult = new SubgroupSet(this);
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

	public ArrayList<SubgroupSet> getGrouping(SubgroupSet aPatternTeam)
	{
		ArrayList<SubgroupSet> aGrouping = new ArrayList<SubgroupSet>();
		for (Subgroup aDummy : aPatternTeam)
			aGrouping.add(new SubgroupSet(this));
		if (itsBinaryTable == null)
			return aGrouping; //empty grouping. This only happens if getPatternTeam() is not called before this function

		//compute grouping
		int i = 0;
		for (Subgroup aSubgroup : this) //a pattern is a subgroup that is a member of a pattern team
		{
			Subgroup aClosestPattern = null;
			int aClosestID = -1;
			float aBest = Float.NEGATIVE_INFINITY;
			int j = 0;

			for (Subgroup aPattern : aPatternTeam)
			{
				float aCorrelation = Math.abs(itsBinaryTable.computeCorrelation(i, j));
				if (aClosestPattern == null || aCorrelation > aBest)
				{
					aClosestPattern = aPattern;
					aClosestID = j;
					aBest = aCorrelation;
				}
				j++;
			}
			aGrouping.get(aClosestID).add(aSubgroup);
			i++;
		}

		return aGrouping;
	}

	public double getJointEntropy() { return itsJointEntropy; }

	//Determines for each Subgroup in the result whether it should remain there. If the Subgroup is a specialisation of another Subgroup, and it has lower quality, it will be removed.
	//This is a quadratic process (in the size of the result), so could be slow for very large result sets.
	public void filterSubgroups(float theMinimumImprovement)
	{
		int aSize = size();
		Iterator<Subgroup> anIterator = iterator();

		while (anIterator.hasNext())
		{
			Subgroup aFirstSubgroup = anIterator.next();
			for (Subgroup aSecondSubgroup : this)
				if (aFirstSubgroup != aSecondSubgroup)
				{
					if (aFirstSubgroup.strictlySpecialises(aSecondSubgroup) && aFirstSubgroup.getMeasureValue() <= aSecondSubgroup.getMeasureValue() + theMinimumImprovement)
					{
						anIterator.remove();
						break;
					}
				}
		}
		System.out.println("Raw result set size: " + aSize);
		System.out.println("Filtered result set size: " + size());
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

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// start of obsolete code                                           /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	@Deprecated
	public BinaryTable getBinaryTable(Table theTable)
	{
		return new BinaryTable(theTable, this);
	}

	@Deprecated
	public void print()
	{
		update();
		for (Subgroup s : this)
			Log.logCommandLine(String.format("%d,%d,%d",
								s.getID(),
								s.getCoverage(),
								s.getMeasureValue()));
	}

	// see extensive comment on CBSS implementation at bottom of CandidateQueue
	/**
	 * Computes the cover count of a particular example: the number of times
	 * this example is a member of a subgroup.<br>
	 * See van Leeuwen & Knobbe, ECML PKDD 2011
	 */
	@Deprecated
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
	@Deprecated
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
}
