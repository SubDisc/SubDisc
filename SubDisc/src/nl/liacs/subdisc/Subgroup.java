package nl.liacs.subdisc;

import java.util.*;
import java.util.concurrent.locks.*;

import nl.liacs.subdisc.ConditionListBuilder.ConditionListA;

/**
 * A Subgroup contains a number of instances from the original data.
 * 
 * Subgroups are formed by, a number of, {@link Condition}s. Its members include
 * : a {@link ConditionListA}, a BitSet representing the instances included in
 * this Subgroup, the number of instances in this Subgroup (its coverage),
 * an identifier and a {@link SubgroupSet}.
 * It may also contain a {@link DAG}.
 * 
 * Note this class is not thread safe.
 * 
 * @see Condition
 * @see ConditionListA
 * @see DAG
 * @see nl.liacs.subdisc.gui.MiningWindow
 * @see SubgroupDiscovery
 * @see SubgroupSet
 */
public class Subgroup implements Comparable<Subgroup>
{
//	private ConditionList itsConditions;
	private ConditionListA itsConditions;
	private BitSet itsMembers;
	// not a ReadWriteLock, members may be nulled at any moment
	private final Lock itsMembersLock = new ReentrantLock();
	private int itsID = 0;
	private int itsCoverage; // crucial to keep it in sync with itsMembers
	private DAG itsDAG;
	private LabelRanking itsLabelRanking;
	private LabelRankingMatrix itsLabelRankingMatrix;
	private double itsMeasureValue = 0.0;
	private double itsSecondaryStatistic = 0.0;
	private double itsTertiaryStatistic = 0.0;
	private final SubgroupSet itsParentSet;
	// XXX not strictly needed when setting itsPValue to NaN
	private boolean isPValueComputed;
	private double itsPValue;
	private String itsRegressionModel;

	/*
	 * member BitSet compressed using (G)ZIP (LZ77)
	 * NOTE logical operations on this Object require it to be unpacked
	 * so it should not be used for that
	 * however, for large (sparse) BitSets that remain unchanged, it saves
	 * a lot of space (true for Subgroups shown in ResultWindow)
	 * during search, the member BitSets are modified, and used in logical
	 * operations
	 * BBC, (E/PL)WAH and other compression schemes exist that offer both
	 * compression and fast modification, as decompression is not needed
	 * additionally, for Cortana's Subgroup Discovery algorithm Refinements
	 * are guaranteed to not increase the number of Subgroup members
	 */
//	private byte[] itsCompressedMembers;
//	ObjectOutputStream os = new ObjectOutputStream(new GZIPOutputStream(new ByteArrayOutputStream(), itsMembers.length())));
//	os.writeObject(itsMembers);
//	os.close();
//	itsCompressedMembers = os.toByteArray();

	/**
	 * Creates a Subgroup with initial measureValue of 0.0 and a depth of 0.
	 * <p>
	 * The {@link BitSet} can not be {@code null} and at least 1 bit must be
	 * set, each set bit represents a member of this Subgroup.
	 * <p>
	 * the {@link ConditionListA} and {@link SubgroupSet} argument can not
	 * be {@code null}.
	 *
	 * @param theConditions the ConditionList for this Subgroup.
	 * @param theMembers the BitSet representing members of this Subgroup.
	 * @param theSubgroupSet the SubgroupSet this Subgroup is contained in.
	 *
	 * @throws IllegalArgumentException if any of the arguments is
	 * {@code null} and when (theMembers.cardinality() == 0).
	 */
	//public Subgroup(ConditionList theConditions, BitSet theMembers, SubgroupSet theSubgroupSet) throws IllegalArgumentException
	public Subgroup(ConditionListA theConditions, BitSet theMembers, SubgroupSet theSubgroupSet) throws IllegalArgumentException
	{
		if (theConditions == null || theMembers == null || theSubgroupSet == null)
			throw new IllegalArgumentException("arguments can not be null");
		if (theMembers.length() == 0)
			throw new IllegalArgumentException("Subgroups must have members");

		itsConditions = theConditions;

		constructorMembersInit(theMembers);

		itsParentSet = theSubgroupSet;

		itsMeasureValue = 0.0f;
		itsDAG = null;	//not set yet
		itsLabelRanking = null;
		itsLabelRankingMatrix = null;
		isPValueComputed = false;
	}

	/*
	 * package private
	 * used only by Validation.getValidSubgroup((int,) int, Random)
	 * most Subgroup methods will not work when using this Constructor
	 * but Validation is only interested in:
	 * getMembers() (they should never be null in the Validation setting)
	 * getCoverage()
	 * getDAG() (multi-label only)
	 */
	Subgroup(BitSet theMembers) throws IllegalArgumentException
	{
		if (theMembers == null)
			throw new IllegalArgumentException("arguments can not be null");
		if (theMembers.length() == 0)
			throw new IllegalArgumentException("Subgroups must have members");

		constructorMembersInit(theMembers);
		itsDAG = null;

		// final, needs to be set
		itsParentSet = null;
	}

	// XXX MM - NOTE
	// in the original Subgroup.copy() code itsMeasureValue,
	// itsSecondaryStatistic and itsTertiaryStatistic were copied
	// however, right after obtaining a new Subgroup through copy(), a
	// Condition was added to it
	// from that point on, these measures, plus itsMembers and itsCoverage
	// become invalid, and should be re-evaluated
	// TODO inspect code if copying is needed
	// at least itsMeasureValue is used by CandidateQueue to order Subgroups
	// maybe more code relies on these values set to something/anything
	//
	// private constructor, do not use outside class, no argument checks
	// used for getRefinedSubgroup()
	private Subgroup(Subgroup theSubgroup, Condition theCondition)
	{
		// constructorMembersInit() creates itsMembers based on this
		itsConditions = ConditionListBuilder.createList(theSubgroup.itsConditions, theCondition);
		// itsID		ignored
		// itsMembers		set through constructorMembersInit below
		// itsCoverage		set through constructorMembersInit below
		// itsDag		ignored
		// itsLabelRanking	ignored
		// itsLabelRankingMatrix ignored
		itsMeasureValue		= theSubgroup.itsMeasureValue;		// see NOTE
		itsSecondaryStatistic	= theSubgroup.itsSecondaryStatistic;	// see NOTE
		itsTertiaryStatistic	= theSubgroup.itsTertiaryStatistic;	// see NOTE
		itsParentSet		= theSubgroup.itsParentSet; 
		// isPValueComputed	ignored
		// itsPValue		ignored
		// itsRegressionModel	ignored

		// set itsMembers and itsCoverage based on new Condition
		// evaluate() should not modify input, else use getMembers()
		int check = theSubgroup.itsCoverage;
		Column c = theCondition.getColumn();
		BitSet aParentBitSet = theSubgroup.getMembersUnsafe();

		constructorMembersInit(c.evaluate(aParentBitSet, theCondition));

		assert (aParentBitSet.cardinality() == check);
	}

	private void constructorMembersInit(BitSet theMembers)
	{
		itsMembersLock.lock();
		try
		{
			// TODO MM a copy of theMembers would be safer
			itsMembers = theMembers;
			itsCoverage = itsMembers.cardinality();
		}
		finally
		{
			itsMembersLock.unlock();
		}
	}

	// safe, clean code using private copy constructor
	public Subgroup getRefinedSubgroup(Condition theCondition)
	{
		if (theCondition == null)
			throw new IllegalArgumentException("arguments can not be null");

		return new Subgroup(this, theCondition);
	}

	// for SINGLE_NOMINAL: re-use BitSet obtained from Column.getBinaryDomain()
	Subgroup getRefinedSubgroup(Condition theCondition, BitSet theNewMembers, int theCoverage)
	{
		// not a public method, assert is enough
		assert (theCondition != null);
		assert (theNewMembers != null);
		assert (theNewMembers.cardinality() == theCoverage);
		assert (theCoverage > 0);
		assert (theCondition.getColumn().evaluate(getMembersUnsafe(), theCondition).cardinality() == theCoverage);

		return new Subgroup(this, theCondition, theNewMembers, theCoverage);
	}

	private Subgroup(Subgroup theSubgroup, Condition theCondition, BitSet theNewSubgroupMembers, int theCoverage)
	{
		itsConditions = ConditionListBuilder.createList(theSubgroup.itsConditions, theCondition);
		itsParentSet  = theSubgroup.itsParentSet;
		itsCoverage   = theCoverage;

		// not setting these fields, see NOTE at Subgroup(Subgroup, Condition)
		// itsMeasureValue, itsSecondaryStatistic, theSecondaryStatistic

		itsMembersLock.lock();
		try { itsMembers = theNewSubgroupMembers; }
		finally { itsMembersLock.unlock(); }
	}

	// for direct computation of qualities scores, bypass BitSet evaluation
	Subgroup getRefinedSubgroup(Condition theAddedCondition, double theQuality, double theSecondaryStatistic, double theTertiaryStatistic, int theChildCoverage)
	{
		// not a public method, assert is enough
		assert (theAddedCondition != null);
		assert (theChildCoverage > 0);
		assert (theAddedCondition.getColumn().evaluate(getMembersUnsafe(), theAddedCondition).cardinality() == theChildCoverage);

		return new Subgroup(this, theAddedCondition, theQuality, theSecondaryStatistic, theTertiaryStatistic, theChildCoverage);
	}

	private Subgroup(Subgroup theParent, Condition theAddedCondition, double theQuality, double theSecondaryStatistic, double theTertiaryStatistic, int theChildCoverage)
	{
		itsConditions         = ConditionListBuilder.createList(theParent.itsConditions, theAddedCondition);
		itsMeasureValue       = theQuality;
		itsSecondaryStatistic = theSecondaryStatistic;
		itsTertiaryStatistic  = theTertiaryStatistic;
		itsParentSet          = theParent.itsParentSet;

		itsCoverage = theChildCoverage;
	}

	// private, for use within this class only, do no expose members
	// does not return a clone, but the actual itsMembers
	// re-instantiates itsMembers, not in separate method, as it requires
	// extra Lock logic
	private final BitSet getMembersUnsafe()
	{
		itsMembersLock.lock();
		try
		{
			if (itsMembers == null)
			{
				// the default Constructor ensures SubgroupSet
				BitSet b = itsParentSet.getAllDataBitSetClone();

				// does nothing when ConditionListA is empty
				for (int i = 0, j = itsConditions.size(); i < j; ++i)
				{
					Condition c = itsConditions.get(i);
					b = c.getColumn().evaluate(b, c);
				}

				// only assign to itsMembers when aBitSet is in
				// its final state, avoid intermediate non-null
				// state of itsMembers
				itsMembers = b;
				// coverage should not have changed
				assert (itsCoverage == itsMembers.cardinality());
			}

			// within lock, so no other Thread can null itsMembers
			return itsMembers;
		}
		finally
		{
			itsMembersLock.unlock();
		}
	}

	/**
	 * Returns a {@link BitSet} where each set bits represents a member of
	 * this Subgroup.
	 * <p>
	 * Each returned BitSet is a new clone of the actual members, so
	 * changing the returned BitSet has no effect on this Subgroup.
	 * This is unlikely to be a performance penalty in most situations, but
	 * some may want to cache the return BitSet.
	 * Most callers need the returned BitSet for nothing more than looping
	 * over all members, or retrieve the cardinality.
	 *
	 * @return a BitSet representing this Subgroups members.
	 */
	public BitSet getMembers() { return (BitSet) getMembersUnsafe().clone(); }

	/*
	 * package private, called by CandidateQueue to reduce memory load
	 * the rationale is as follows:
	 * (depending on the search strategy) many Candidates go into the Queue
	 * but most will be purged from it, as they will not have a high enough
	 * score (priority)
	 * for those that do remain, the members BitSet is generally not needed
	 * straight away, but would claim large amounts of memory (especially
	 * for long data sets (many rows))
	 * only when the BitSet is needed, it will be re-instantiated
	 * the cost for this is re-evaluation every Condition in the
	 * ConditionList itsConditions (generally few)
	 * and the few evaluations this takes is far less than the number of
	 * Refinements that is evaluated for the Subgroup
	 * so the re-evaluation does not substantially impact performance
	 */
	void killMembers()
	{
		itsMembersLock.lock();
		try { itsMembers = null; }
		finally { itsMembersLock.unlock(); }
	}
	void reviveMembers() { getMembersUnsafe(); }

	public boolean covers(int theRow) { return getMembersUnsafe().get(theRow); }

	public int getID() { return itsID; }
	public void setID(int theID) { itsID = theID; }

	public double getMeasureValue() { return itsMeasureValue; }
	public void setMeasureValue(double theMeasureValue) { itsMeasureValue = theMeasureValue; }
	public double getSecondaryStatistic() { return itsSecondaryStatistic; }
	public void setSecondaryStatistic(double theSecondaryStatistic) { itsSecondaryStatistic = theSecondaryStatistic; }
	public double getTertiaryStatistic() { return itsTertiaryStatistic; }
	public void setTertiaryStatistic(double theTertiaryStatistic) { itsTertiaryStatistic = theTertiaryStatistic; }

	public void setDAG(DAG theDAG) { itsDAG = theDAG; }
	public DAG getDAG() { return itsDAG; }

	public void setLabelRanking(LabelRanking theLabelRanking) { itsLabelRanking = theLabelRanking; }
	public LabelRanking getLabelRanking() { return itsLabelRanking; }
	public void setLabelRankingMatrix(LabelRankingMatrix theLabelRankingMatrix) { itsLabelRankingMatrix = theLabelRankingMatrix; }
	public LabelRankingMatrix getLabelRankingMatrix() { return itsLabelRankingMatrix; }

	// could be out of sync with itsMembers in between addCondition() update
	public int getCoverage() { return itsCoverage; }

	//public ConditionList getConditions() { return itsConditions; }
	public ConditionListA getConditions() { return itsConditions; }

	public int getDepth() { return itsConditions.size(); }

	/**
	 * NOTE For now this equals implementation is only used for the ROCList
	 * HashSet implementation.
	 * Two subgroups are considered equal if:
	 * for each condition(Attribute-Operator pair) in this.conditionList there
	 * is a matching condition(Attribute-Operator pair) in other.conditionList
	 * and both itsMembers are equal.
	 */
/*
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Subgroup))
			return false;

		Subgroup s = (Subgroup) o;

		for(Condition c : itsConditions.itsConditions)
		{
			boolean hasSameAttributeAndOperator = false;
			for(Condition sc : s.itsConditions.itsConditions)
			{
				if(c.getAttribute().getName().equalsIgnoreCase(sc.getAttribute().getName()) &&
						c.getOperatorString().equalsIgnoreCase(sc.getOperatorString()))
				{
					hasSameAttributeAndOperator = true;
					System.out.println(this.getID()+ " " + s.getID());
					this.print();
					s.print();
					break;
				}
			}
			if(!hasSameAttributeAndOperator)
				return false;
		}

		return itsMembers.equals(s.itsMembers);
		//getTruePositiveRate().equals(s.getTruePositiveRate()) &&
			//	getFalsePositiveRate().equals(s.getFalsePositiveRate());
	}
*/
	/*
	 * TODO Even for the SubgroupSet.getROCList code this is NOT enough.
	 * All subgroups are from the same SubgroupSet/ experiment with the same target.
	 * However, two subgroups formed from different Attributes in itsConditions
	 * should be considered unequal. This requires an @Override from itsConditions
	 * hashCode(), as it should not include condition values.
	 * Eg. two subgroups that have the same members and are formed from:
	 * (x < 10) and (x < 11) should be considered equal
	 * (y < 10) and (x < 10) should be considered different
	 */
/*
	@Override
	public int hashCode()
	{
		int hashCode = 0;
		for(Condition c : itsConditions.itsConditions)
			hashCode += (c.getAttribute().getName().hashCode() + c.getOperatorString().hashCode());
		return 31*itsMembers.hashCode() + hashCode;
	}
*/

	// this code is never called
	final int countCommon(BitSet theBitSet)
	{
		if (theBitSet == null)
			throw new IllegalArgumentException("arguments can not be null");

		itsMembersLock.lock();
		// throughout countCommon() itsMembers must not be modified
		try { return countCommon(getMembersUnsafe(), theBitSet); }
		finally { itsMembersLock.unlock(); }
	}

	// FIXME MM - there is a reasonable chance that an and() on the two BitSets
	// is faster, as it operates on the underlying longs in the long[], instead
	// of evaluating the individual bits (which requires visiting every long
	// also), the only problem is that BitSet.and(BitSet) modifies the first Set
	private static final int countCommon(BitSet a, BitSet b)
	{
		int aCount = 0;

		for (int ai = a.nextSetBit(0), bi = b.nextSetBit(0); ((ai >= 0) && (bi >= 0)); /* increments inside loop */)
		{
			if (ai < bi)
				ai = a.nextSetBit(bi);
			else if (ai > bi)
				bi = b.nextSetBit(ai);
			else
			{
				// equal index set, increment, find new indexes
				++aCount;
				ai = a.nextSetBit(ai + 1);
				bi = b.nextSetBit(bi + 1);
			}
		}

		return aCount;
	}

	// used to determine TP/FP
	public SubgroupSet getParentSet()
	{
		return itsParentSet;
	}

	/**
	 * Returns the TruePositiveRate for this Subgroup.
	 * If no itsParentSet was set for this SubGroup, or no itsBinaryTarget
	 * was set for this SubGroups' itsParentSet this function returns 0.0.
	 *
	 * @return the TruePositiveRate, also known as TPR.
	 */
	/* FIXME MM need only cardinality(), obtain without expensive clone */
	public double getTruePositiveRate()
	{
		BitSet tmp = itsParentSet.getBinaryTargetClone();

		if (tmp == null)
			return 0.0;

		// TODO MM - USE countCommon(tmp, getMembersUnsafe())
		tmp.and(getMembersUnsafe());
		// NOTE now tmp.cardinality() = aHeadBody

		int aTotalTargetCoverage = itsParentSet.getTotalTargetCoverage();

		// aTotalTargetCoverage can not be 0, as the target value used
		// should not have been allowed to be selected
		if (aTotalTargetCoverage <= 0)
			throw new AssertionError();

		return ((double)tmp.cardinality()) / aTotalTargetCoverage;
	}

	/**
	 * Returns the FalsePositiveRate for this Subgroup.
	 * If no itsParentSet was set for this subgroup, or no itsBinaryTarget
	 * was set for this subgroups' itsParentSet this function returns 0.0f.
	 *
	 * @return the FalsePositiveRate, also known as FPR.
	 */
	/* FIXME MM need only cardinality(), obtain without expensive clone */
	public double getFalsePositiveRate()
	{
		BitSet tmp = itsParentSet.getBinaryTargetClone();

		if (tmp == null)
			return 0.0;

		// TODO MM - USE countCommon(tmp, getMembersUnsafe())
		tmp.and(getMembersUnsafe());
		// NOTE now tmp.cardinality() = aHeadBody

		int aTotalCoverage = itsParentSet.getTotalCoverage();
		int aTotalTargetCoverage = itsParentSet.getTotalTargetCoverage();
		int aNotHead = (aTotalCoverage - aTotalTargetCoverage);

		// aTotalTargetCoverage can not be 0, as the target value used
		// should not have been allowed to be selected
		// aNotHead can be 0 for a target concept that contains just 1
		// target value, this is awkward, but not incorrect and this is
		// not the location to check for this
		if (aTotalCoverage <= 0 || aTotalTargetCoverage < 0 ||
			aTotalCoverage < aTotalTargetCoverage || aNotHead < 0)
			throw new AssertionError();

		// (FP / !H)
		return ((double)(itsCoverage - tmp.cardinality())) / aNotHead;
	}

	public double getPValue()
	{
		return (isPValueComputed ? itsPValue : Double.NaN);
	}

	// FIXME MM find better solution for this
	void setPValue(double theFakePValue) { isPValueComputed = true; itsPValue = theFakePValue; }
	public void setPValue(NormalDistribution theDistro)
	{
		isPValueComputed = true;
		itsPValue = 1 - theDistro.calcCDF(itsMeasureValue);
	}

	public void setEmpiricalPValue(double[] theQualities)
	{
		isPValueComputed = true;
		int aLength = theQualities.length;
		double aP = 0.0;
		for (int i=0; i<aLength; i++)
		{
			if (theQualities[i]>=itsMeasureValue)
				aP++;
		}
		itsPValue = aP/aLength;
	}

	public void renouncePValue() { isPValueComputed = false; }

	public String getRegressionModel() { return itsRegressionModel; }
	public void setRegressionModel(String theModel) { itsRegressionModel = theModel; }

	/*
	 * Compare two Subgroups based on (in order) measureValue, coverage,
	 * ConditionList.
	 * 
	 * Per Comparable Javadoc compareTo(null) throws a NullPointerException.
	 * 
	 * Do not use this compareTo() for the CAUC(Heavy) setting of Process.
	 * itsMeasureValue will vary for Subgroups with the same ConditionList
	 * (because the target for each run is different).
	 * 
	 * NOTE Map interface expects compareTo and equals to be consistent.
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 * 
	 * throws NullPointerException if theSubgroup is null.
	 */
	@Override
	public int compareTo(Subgroup theSubgroup)
	{
		if (this == theSubgroup)
			return 0;

		// Subgroups that score better come first
		int cmp = Double.compare(this.itsMeasureValue, theSubgroup.itsMeasureValue);
		if (cmp != 0)
			return -cmp;

		// Subgroups that are larger come first
		// this implies that when all of the conditions of a Subgroup s1 are
		// contained in the set of Conditions for s2 (a proper subset), s1 ranks
		// before s2
		cmp = this.itsCoverage - theSubgroup.itsCoverage;
		if (cmp != 0)
			return -cmp;

		// equal score and coverage, compare ConditionLists
		return this.itsConditions.compareTo(theSubgroup.itsConditions);
	}

	@Override
	public String toString()
	{
		return itsConditions.toString();
	}
}
