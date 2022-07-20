package nl.liacs.subdisc;

import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import javax.swing.*;

import nl.liacs.subdisc.Column.DomainMapNumeric;
import nl.liacs.subdisc.Column.ValueCount;
import nl.liacs.subdisc.Column.ValueCountTP;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBases;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBasesBinary;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBasesNominalElementOf;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBasesNominalEquals;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBasesNumericIntervals;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBasesNumericRegular;
import nl.liacs.subdisc.ConditionListBuilder.ConditionList;
import nl.liacs.subdisc.ConvexHull.HullPoint;
import nl.liacs.subdisc.gui.*;

public class SubgroupDiscovery
{
	private static final boolean NO_CANDIDATE_LOG = false;				// log slows down mining a lot, but leave NO_CANDIDATE_LOG at false in git

	// old algorithm creates many useless Refinements, set these to true to avoid this for various scenarios, leave at false in git
	private static final boolean BEST_VALUESET_AT_LAST_POSITION_SKIP = false;
	private static final boolean BEST_INTERVAL_AT_LAST_POSITION_SKIP = false;

	// these two have been extensively tested, leave at true (gather statistics)
	private static final boolean BEST_VALUESET_NO_REFINEMENT_SKIP    = true;	
	private static final boolean BEST_INTERVAL_NO_REFINEMENT_SKIP    = true;

	private static final boolean BINARY_NOMINAL_EQUALS_SKIP          = false;	// for nominal class labels only, irrelevant for ValueSet-scenario

	// for numeric ALL|BEST + only EQUALS (not LEQ/GEQ), not for (Best)Intervals
	private static final boolean NUMERIC_EQ_SKIP                     = false;

	// data set/search parameter specific filter will replace all of the above
	// SET just tests/reports redundant refinements, USE actually skips them
	private static final boolean SET_SKIP_FILTER                     = false;
	private static final boolean USE_SKIP_FILTER                     = false;
	private static final boolean DEBUG_PRINTS_FOR_SKIP               = false;
	private static final boolean DEBUG_PRINTS_FOR_WARN               = false;

	private static final boolean DEBUG_PRINTS_FOR_BEST               = false;	// print how often Best Candidate differs from Best Result, see checkBest()

	private static final boolean USE_SINGLE_BEST_RESULT_LIKE_BEFORE  = false;	// when false: both BestForResultSet and BestForCandidateSet are used

	private static final boolean DEBUG_PRINTS_FOR_BEST_INTERVAL      = false;	// not used anymore, but will be again when debugging linear algorithm

	private static final boolean DEBUG_PRINTS_NEXT_LEVEL_CANDIDATES  = false;	// beam fill (not equal to nr. results at same level; for cbss includes -tmp

	private static final int[] FOR_REAL_PRINTS = { 10, 100 };			// print CoverRedundancy and JointEntropy for topK for For Real paper

	// statistics for debugging - related to booleans above
	private AtomicLong itsBestPairsCount  = new AtomicLong(0);
	private AtomicLong itsBestPairsDiffer = new AtomicLong(0);
	private AtomicLong itsSkipCount       = new AtomicLong(0);

	// BestInterval debugging - abuses BestSubgroupsForCandidateSetAndResultSet
	private AtomicLong itsBestIntervalsCount  = new AtomicLong(0);
	private AtomicLong itsBestIntervalsDiffer = new AtomicLong(0);
	private Queue<BestSubgroupsForCandidateSetAndResultSet> itsBestIntervalsErrors = new ConcurrentLinkedQueue<>();

	// only ever called by a single Thread, but make Thread-safe anyway
	private Queue<Integer> itsCandidateQueueSizes = new ConcurrentLinkedQueue<>();

	// leave TEMPORARY_CODE at false in git
	// when true, creates PMF instead of PDF in single numeric H^2 setting
	static boolean TEMPORARY_CODE                 = false;
	static int     TEMPORARY_CODE_NR_SPLIT_POINTS = -1;
	static boolean TEMPORARY_CODE_USE_EQUAL_WIDTH = false;

	// FIXME remove itsSearchParameters, to not allow changes after construction
	private final SearchParameters itsSearchParameters;
	private final Table itsTable;
	private final BitSet itsSelection;
	private final int itsNrRows;
	private final int itsMinimumCoverage;
	private final int itsMaximumCoverage;
	private int itsMinimumSupport = 0;

	private final QualityMeasure itsQualityMeasure;
	private final float itsQualityMeasureMinimum;   // itsSearchParameters.getQualityMeasureMinimum();
	private boolean ignoreQualityMinimum = false;   //used for swap-randomization purposes, and to get random qualities

	// target concept type-specific information, including base models
	private BitSet itsBinaryTarget;                 // SINGLE_NOMINAL
	private Column itsTargetRankings;               // SINGLE_NOMINAL (label ranking)
	private Column itsNumericTarget;                // SINGLE_NUMERIC
	private Column itsPrimaryColumn;                // DOUBLE_CORRELATION / DOUBLE_REGRESSION / DOUBLE_BINARY / SCAPE
	private Column itsSecondaryColumn;              // DOUBLE_CORRELATION / DOUBLE_REGRESSION / DOUBLE_BINARY / SCAPE
	private CorrelationMeasure itsBaseCM;           // DOUBLE_CORRELATION
	private RegressionMeasure itsBaseRM;            // DOUBLE_REGRESSION
	private BinaryTable itsBinaryTable;             // MULTI_LABEL
	private List<Column> itsTargets;                // MULTI_LABEL / MULTI_NUMERIC
	public ProbabilityDensityFunction_ND itsPDF_ND; // MULTI_NUMERIC

	private LocalKnowledge itsLocalKnowledge;       // PROPENSITY SCORE BASED
	private GlobalKnowledge itsGlobalKnowledge;     // PROPENSITY SCORE BASED

	// only use in disabled Cook code
	private int itsBoundSevenCount;
	private int itsBoundSixCount;
	private int itsBoundFiveCount;
	private int itsBoundFourCount;
	private int itsBoundSevenFired;
	private int itsBoundSixFired;
	private int itsBoundFiveFired;
	private int itsBoundFourFired;
	private int itsRankDefCount;
	private TreeSet<Candidate> itsBuffer;

	// candidate and result set - check() increments itsCandidateCount
	private AtomicLong itsCandidateCount = new AtomicLong(0);
	private CandidateQueue itsCandidateQueue;
	private final SubgroupSet itsResult;

	// for mining
	private long itsEndTime = Long.MIN_VALUE;

	// for feeding back progress info
	// to throttle GUI update, only one thread can obtain lock and update time
	private static final long INTERVAL = 100_000_000L;
	private static final DecimalFormat FORMATTER = (DecimalFormat) NumberFormat.getNumberInstance(Locale.GERMAN);
	private JFrame itsMainWindow;
	private final Lock itsClockLock = new ReentrantLock();
	private long itsThen = 0L;

	//SINGLE_NOMINAL
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, BitSet theSelection, int theNrPositive, JFrame theMainWindow)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsSelection = theSelection;
		itsNrRows = (theSelection == null) ? itsTable.getNrRows() : theSelection.cardinality();
		itsMainWindow = theMainWindow;
		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());
		itsMinimumSupport = itsSearchParameters.getMinimumSupport();
		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsNrRows, theNrPositive);
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		Column aTarget = aTC.getPrimaryTarget();
		ConditionBase aConditionBase = new ConditionBase(aTarget, Operator.EQUALS);
		String aValue = aTC.getTargetValue();
		Condition aCondition;
		switch (aTarget.getType())
		{
			case NOMINAL :
				aCondition = new Condition(aConditionBase, aValue);
				break;
			case NUMERIC :
				throw new AssertionError(AttributeType.NUMERIC);
			case ORDINAL :
				throw new AssertionError(AttributeType.ORDINAL);
			case BINARY :
				if (!AttributeType.isValidBinaryValue(aValue))
					throw new IllegalArgumentException(aValue + " is not a valid BINARY value");
				aCondition = new Condition(aConditionBase, AttributeType.isValidBinaryTrueValue(aValue));
				break;
			default :
				throw new AssertionError(aTarget.getType());
		}

		BitSet aBitSet = new BitSet(itsTable.getNrRows());
		aBitSet.set(0, itsTable.getNrRows());
		itsBinaryTarget = aTC.getPrimaryTarget().evaluate(aBitSet, aCondition);
		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), theSelection, itsTable.getNrRows(), itsBinaryTarget);
	}

	//SINGLE_NUMERIC, float > signature differs from multi-label constructor
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, BitSet theSelection, float theAverage, JFrame theMainWindow)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsSelection = theSelection;
		itsNrRows = (theSelection == null) ? itsTable.getNrRows() : theSelection.cardinality();
		itsMainWindow = theMainWindow;
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());
		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		itsNumericTarget = aTC.getPrimaryTarget();

		Statistics aStatistics = itsNumericTarget.getStatistics(theSelection, null, false, QM.requiredStats(itsSearchParameters.getQualityMeasure()).contains(Stat.COMPL));
		aStatistics.print();
		ProbabilityDensityFunction2 aPDF = new ProbabilityDensityFunction2(itsNumericTarget, theSelection);
		aPDF.smooth();

		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsNrRows, aStatistics.getSubgroupSum(), aStatistics.getSubgroupSumSquaredDeviations(), aPDF);
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), theSelection, itsTable.getNrRows());
	}

	//DOUBLE_CORRELATION, DOUBLE_REGRESSION, DOUBLE_BINARY
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, BitSet theSelection, boolean isRegression, JFrame theMainWindow)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsSelection = theSelection;
		itsNrRows = (theSelection == null) ? itsTable.getNrRows() : theSelection.cardinality();
		itsMainWindow = theMainWindow;
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());
		// ensure theCountHeadBody <= theCoverage
		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsNrRows, itsNrRows); //TODO
		//itsQualityMeasure = null; // itsQualityMeasure is not needed
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		//N.B.: Temporary lines for fetching Cook's experimental statistics
		if (theSearchParameters.getQualityMeasure() == QM.COOKS_DISTANCE)
		{
			Log.REFINEMENTLOG = true;
			Log.openFileOutputStreams();
			//Log.logRefinement("Bound graph for "+itsTable.getName());
			//Log.logRefinement("SubgroupSize,AvgRegressionTime,AvgCook,AvgBoundSeven,AvgBoundSix,AvgBoundFive,AvgBoundFour" +
			//	",CookComputable,BoundSevenComputable,BoundSixComputable,BoundFiveComputable,BoundFourComputable");
		}

		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		// TODO for stable jar, initiated here, SubgroupDiscovery revision 893 moved this to else below
		itsPrimaryColumn = aTC.getPrimaryTarget();
		itsSecondaryColumn = aTC.getSecondaryTarget();

		if (theSearchParameters.getTargetType() != TargetType.DOUBLE_BINARY)
		{
			if (isRegression)
			{
				// TODO RegressionMeasure revision 851 introduces the new RegressionMeasure constructor below (not mentioned in log)
				itsBaseRM = new RegressionMeasure(itsSearchParameters.getQualityMeasure(), itsPrimaryColumn, itsSecondaryColumn);
				// TODO for stable jar, disabled, causes compile errors, reinstate later
				// itsBaseRM = new RegressionMeasure(itsSearchParameters.getQualityMeasure(), aTC);

				//initialize bounds
				itsBoundSevenCount=0;
				itsBoundSixCount=0;
				itsBoundFiveCount=0;
				itsBoundFourCount=0;
				itsBoundSevenFired=0;
				itsBoundSixFired=0;
				itsBoundFiveFired=0;
				itsBoundFourFired=0;
				itsRankDefCount=0;

				itsBuffer = new TreeSet<Candidate>();
			}
			else
			{
				// TODO for stable jar, disabled, initiated above, reinstate later as per SubgroupDiscovery revision 893
				// itsPrimaryColumn = aTC.getPrimaryTarget();
				// itsSecondaryColumn = aTC.getSecondaryTarget();
                		itsBaseCM = new CorrelationMeasure(itsSearchParameters.getQualityMeasure(), itsPrimaryColumn, itsSecondaryColumn);
			}
        	}

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), theSelection, itsTable.getNrRows());
	}

	//SCAPE
	public SubgroupDiscovery(JFrame theMainWindow, SearchParameters theSearchParameters, Table theTable, BitSet theSelection)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsSelection = theSelection;
		itsNrRows = (theSelection == null) ? itsTable.getNrRows() : theSelection.cardinality();
		itsMainWindow = theMainWindow;
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());

		//compute base model
		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		// TODO for stable jar, initiated here, SubgroupDiscovery revision 893 moved this to else below
		itsPrimaryColumn = aTC.getPrimaryTarget();
		itsSecondaryColumn = aTC.getSecondaryTarget();
		// original code hack as default constructor would not work
		//itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsNrRows, itsPrimaryColumn.getBinaries().cardinality(), 
		//					 itsPrimaryColumn, itsSecondaryColumn, itsSearchParameters.getOverallRankingLoss());
		// unable to reproduce error MM
		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsNrRows, itsPrimaryColumn.getBinaries().cardinality(), itsPrimaryColumn, itsSecondaryColumn);
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), theSelection, itsTable.getNrRows());
	}

	//MULTI_LABEL
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, BitSet theSelection, JFrame theMainWindow)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsSelection = theSelection;
		itsNrRows = (theSelection == null) ? itsTable.getNrRows() : theSelection.cardinality();
		itsMainWindow = theMainWindow;
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());

		//compute base model
		itsTargets = itsSearchParameters.getTargetConcept().getMultiTargets();
		itsBinaryTable = new BinaryTable(itsTable, itsTargets);

		Bayesian aBayesian = new Bayesian(itsBinaryTable, itsTargets);
		aBayesian.climb();

		itsQualityMeasure = new QualityMeasure(itsSearchParameters,
							aBayesian.getDAG(),
							itsNrRows);
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), theSelection, itsTable.getNrRows());
	}

	//LABEL_RANKING
	public SubgroupDiscovery(SearchParameters theSearchParameters, JFrame theMainWindow, Table theTable, BitSet theSelection)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsSelection = theSelection;
		itsNrRows = (theSelection == null) ? itsTable.getNrRows() : theSelection.cardinality();
		itsMainWindow = theMainWindow;
		TargetConcept aTC = itsSearchParameters.getTargetConcept();

		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());

		itsTargetRankings = aTC.getPrimaryTarget();
		LabelRanking aLR = itsTargetRankings.getAverageRanking(null); //average ranking over entire dataset
		LabelRankingMatrix aLRM = itsTargetRankings.getAverageRankingMatrix(null); //average ranking over entire dataset
		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsNrRows, aLR, aLRM);
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), theSelection, itsTable.getNrRows());
	}

	// MULTI_NUMERIC
	public SubgroupDiscovery(Table theTable, BitSet theSelection, JFrame theMainWindow, SearchParameters theSearchParameters)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsSelection = theSelection;
		itsNrRows = (theSelection == null) ? itsTable.getNrRows() : theSelection.cardinality();
		itsMainWindow = theMainWindow;
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());

		// compute base model
		itsTargets = itsSearchParameters.getTargetConcept().getMultiTargets();
		itsPDF_ND = new ProbabilityDensityFunction_ND(itsTargets.toArray(new Column[0]));

		itsQualityMeasure = null;
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), theSelection, itsTable.getNrRows());
	}

	/*
	 * Only the top result is used in this setting. Maximum coverage and
	 * binary target constructor parameters are not needed.
	 * NOTE this is a failed attempt to speedup calculation in the
	 * swap-randomise setting. Storing just the top-1 result is only
	 * sufficient for the last depth.
	 * It may be enabled again in the future, LEAVE IT IN.
	 */
	//protected void useSwapRandomisationSetting() {
	//	itsResult.useSwapRandomisationSetting();
	//}

	public void ignoreQualityMinimum()
	{
		ignoreQualityMinimum = true;
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// start of mine() and related methods                              /////
	///// first version is the deprecated original (remains for debugging) /////
	///// only second version can be called by external code               /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	/*
	 * this method remains only for debugging purposes
	 * it should not be called by code outside this class
	 */
	private Filter itsFilter = null;
	@Deprecated
	private final void mine(long theBeginTime, ConditionBaseSet theConditions)
	{
		// to print filter output use: DebugFilter(itsSearchParameters);
		itsFilter = new Filter(itsSearchParameters);

		// TODO MM DEBUG only, set counts to 0
		//RefinementList.COUNT.set(0);
		//RefinementList.ADD.set(0);

		while ((itsCandidateQueue.size() > 0) && !isTimeToStop())
		{
			Candidate aCandidate = itsCandidateQueue.removeFirst(); // take off first Candidate from Queue
			Subgroup aSubgroup = aCandidate.getSubgroup();

			// Subgroup.getMembers() creates expensive clone, reuse
			BitSet aMembers = aSubgroup.getMembers();
			int aCoverage = aSubgroup.getCoverage();
			assert (aMembers.cardinality() == aCoverage);
			assert (aSubgroup.getDepth() < itsSearchParameters.getSearchDepth());

			RefinementList aRefinementList = new RefinementList(aSubgroup, theConditions);

			for (int i = 0, j = aRefinementList.size(); i < j && !isTimeToStop(); i++)
			{
				Refinement aRefinement = aRefinementList.get(i);

				if (!itsFilter.isUseful(aRefinement))
					continue;

				ConditionBase aConditionBase = aRefinement.getConditionBase();
				Column aColumn = aConditionBase.getColumn();

				// no useful Refinements are possible
				if (aColumn.getCardinality() <= 1)
					continue;

				// if refinement is (num_attr = value) then treat it as nominal
				// using EQUALS for numeric conditions is bad, see evaluateNominalBinaryRefinements()
				// evaluateNumericRefinements() should split code path for EQUALS and !EQUALS
				// only NUMERIC_BINS setting is affected
				if (aColumn.getType() == AttributeType.NUMERIC && aConditionBase.getOperator() != Operator.EQUALS)
					evaluateNumericRefinements(aMembers, aCoverage, aRefinement);
				else
					evaluateNominalBinaryRefinements(aMembers, aCoverage, aRefinement);
			}

			if (itsCandidateQueue.size() == 0)
				flushBuffer();
		}

		// TODO MM DEBUG only, set counts to 0
		//Log.logCommandLine("RefinementList.COUNT: " + RefinementList.COUNT);
		//Log.logCommandLine("RefinementList.ADD: " + RefinementList.ADD);

		postMining(theBeginTime);
	}

	/* use theNrThreads < 0 to run old mine(theBeginTime) */
	public void mine(long theBeginTime, int theNrThreads)
	{
		// not a member field, final and unmodifiable, good for concurrency
		final ConditionBaseSet aConditions = preMining(theBeginTime, theNrThreads);
		// FIXME obtain another set again, ignore preMining version
		final List<ColumnConditionBases> aColumnConditionBasesSet = ColumnConditionBasesBuilder.FACTORY.getColumnConditionBasesSet(itsTable, itsSearchParameters);
		final Fltr aFilter = Fltr.get(aColumnConditionBasesSet, itsSearchParameters);

		if (theNrThreads < 0)
		{
			mine(theBeginTime, aConditions);
			return;
		}

		if (theNrThreads == 0)
			theNrThreads = Runtime.getRuntime().availableProcessors();
		/*
		 * essential multi-thread setup
		 * uses semaphores so only nrThreads can run at the same time
		 * AND ExecutorService can only start new Test after old one
		 * completes
		 */
		ExecutorService es = Executors.newFixedThreadPool(theNrThreads);
		Semaphore s = new Semaphore(theNrThreads);
		int aSearchDepth = itsSearchParameters.getSearchDepth();

		while (!isTimeToStop())
		{
			// wait until a Thread becomes available
			try { s.acquire(); }
			catch (InterruptedException e) { e.printStackTrace(); }

			/*
			 * if other threads still have Candidates to add they
			 * are blocked from doing so through this lock on
			 * itsCandidateQueue, and therefore can not release
			 * their permit
			 * although they could have added their Candidates
			 * immediately prior this lock, without releasing their
			 * permit yet
			 *
			 * NOTE for beam search strategies (COVER-BASED/ BEAM)
			 * CandidateQueue will moveToNext level upon depletion
			 * of the current one, overriding the current one with
			 * the next, and creating a new next level
			 * therefore only after all but the last Candidates are
			 * processed (added to next level) can we take the last
			 * one and let the next level become the current
			 *
			 * NOTE 2 although individual methods of CandidateQueue
			 * are thread save, we need a compound action here
			 * so synchronized is still needed
			 */
			Candidate aCandidate = null;
			synchronized (itsCandidateQueue)
			{
				int aTotalSize = itsCandidateQueue.size();
				boolean alone = (s.availablePermits() == theNrThreads-1);
				if (itsCandidateQueue.currentLevelQueueSize() > 0)
					aCandidate = itsCandidateQueue.removeFirst();
				else if ((aTotalSize > 0) && alone)
				{
					aCandidate = itsCandidateQueue.removeFirst();
					if (DEBUG_PRINTS_NEXT_LEVEL_CANDIDATES)
					{
						SearchStrategy ss = itsSearchParameters.getSearchStrategy();
						boolean levelwise = (ss.isBeam() || (ss == SearchStrategy.BREADTH_FIRST));
						if (levelwise && (itsSearchParameters.getSearchDepth() > 1))
						{
							boolean cbss = (ss == SearchStrategy.COVER_BASED_BEAM_SELECTION);
							// use - to indicate size of intermediate beam
							itsCandidateQueueSizes.add(cbss ? -aTotalSize : aTotalSize);
							// first Candidate was just taken from the Queue
							if (cbss) itsCandidateQueueSizes.add(itsCandidateQueue.currentLevelQueueSize() + 1);
						}
					}
				}
				// no other thread can add new candidates
				else if ((aTotalSize == 0) && alone)
					break;
			}

			if (aCandidate != null)
			{
				Subgroup aSubgroup = aCandidate.getSubgroup();

				// Candidate should not be in CandidateQueue 
				assert (aSubgroup.getDepth() < aSearchDepth);
				assert (aSubgroup.getCoverage() > 1);

				es.execute(new Test(aSubgroup, s, aColumnConditionBasesSet, aFilter));
			}
			// queue was empty, but other threads were running, they
			// may be in the process of adding new Candidates
			// wait until at least one finishes, or this one becomes
			// only thread
			else
			{
				try
				{
					final int aNrFree = s.drainPermits();
					if (aNrFree < theNrThreads-1)
					{
						s.acquire();
						s.release(aNrFree+2);
					}
					else
						s.release(aNrFree+1);
					//continue;
				}
				catch (InterruptedException e) { e.printStackTrace(); }
			}
		}
		es.shutdown();
		// wait for last active threads to complete
		while (!es.isTerminated()) {};

		postMining(theBeginTime);

		// statistics - will move to a separate RunTimeStats class of some sort
		if (DEBUG_PRINTS_FOR_BEST && EnumSet.of(NumericStrategy.NUMERIC_BEST, NumericStrategy.NUMERIC_BEST_BINS).contains(itsSearchParameters.getNumericStrategy()))
			Log.logCommandLine("TWO DIFFERENT BEST SUBGROUPS: " + itsBestPairsDiffer + "/" + itsBestPairsCount);
		if (DEBUG_PRINTS_FOR_SKIP)
			Log.logCommandLine("SKIP COUNT: " + itsSkipCount);
		// super temporary - no boolean controlling logging output

		if (DEBUG_PRINTS_FOR_BEST_INTERVAL && NumericStrategy.NUMERIC_INTERVALS == itsSearchParameters.getNumericStrategy())
		{
			Log.logCommandLine("BEST INTERVAL ERRORS: " + itsBestIntervalsDiffer  + "/" + itsBestIntervalsCount);

			DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			df.setMaximumFractionDigits(340); // 340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

			String txt = "quality=%s\tcoverage=%d\ttrue_positives=%d\t%s";
			for (BestSubgroupsForCandidateSetAndResultSet b : itsBestIntervalsErrors)
			{
				Subgroup sg = b.itsBestForCandidateSet;
				String tail = (sg == null) ? "null" : String.format(txt, df.format(sg.getMeasureValue()), sg.getCoverage(), (int) sg.getTertiaryStatistic(), sg);
				Log.logCommandLine("brute force: " + tail);
				//
				sg   = b.itsBestForResultSet;
				tail = (sg == null) ? "null" : String.format(txt, df.format(sg.getMeasureValue()), sg.getCoverage(), (int) sg.getTertiaryStatistic(), sg);
				Log.logCommandLine("linear algo: " + tail);
			}
		}

		// ease log parsing: always print, also maxDepth=1/non-level-wise search
		if (DEBUG_PRINTS_NEXT_LEVEL_CANDIDATES)// && !itsCandidateQueueSizes.isEmpty())
			Log.logCommandLine("NR CANDIDATES FOR NEXT LEVEL: " + itsCandidateQueueSizes);
		for (int topK : FOR_REAL_PRINTS)
			itsResult.postProcessGetCoverRedundancyAndJointEntropy(topK);
	}

	private final ConditionBaseSet preMining(long theBeginTime, int theNrThreads)
	{
		// not in Constructor, Table / SearchParameters may change
		// TODO not sure if this is still true
		final ConditionBaseSet aConditions = new ConditionBaseSet(itsTable, itsSearchParameters);
		logExperimentSettings(aConditions);

		// uses ConditionBaseSet to check for irrelevant Conditions in knowledge
		loadExternalKnowledge(aConditions);

		// make subgroup to start with, containing all elements
		Subgroup aStart = new Subgroup(ConditionListBuilder.emptyList(), itsResult.getAllDataBitSetClone(), itsResult);

		// set number of true positives for data set
		if (isDirectSingleBinary())
			aStart.setTertiaryStatistic(itsQualityMeasure.getNrPositives());

		if ((itsSearchParameters.getBeamSeed() == null) || (theNrThreads < 0))
			itsCandidateQueue = new CandidateQueue(itsSearchParameters, new Candidate(aStart));
		else
			itsCandidateQueue = getCandidateQueueFromBeamSeed();

		// SINGLE_NOMINAL with propensity scores does not use direct computation
		prepareData(isDirectSingleBinary() ? itsBinaryTarget : null, itsTable.getColumns());

		long anEndTime = theBeginTime + (long) (((double) itsSearchParameters.getMaximumTime()) * 60.0 * 1000.0);
		itsEndTime = (anEndTime <= theBeginTime) ? Long.MAX_VALUE : anEndTime;

		return aConditions;
	}

	private final void loadExternalKnowledge(ConditionBaseSet theConditionBaseSet)
	{
		QM aQualityMeasure = itsSearchParameters.getQualityMeasure();

		// fill the conditionList of local and global knowledge, Rob
		if (aQualityMeasure == QM.PROP_SCORE_WRACC || aQualityMeasure == QM.PROP_SCORE_RATIO)
		{
//			ExternalKnowledgeFileLoader extKnowledge;
//			extKnowledge = new ExternalKnowledgeFileLoader(new File("").getAbsolutePath());
//			extKnowledge.createConditionListLocal(itsTable);
//			extKnowledge.createConditionListGlobal(itsTable);
//			itsLocalKnowledge = new LocalKnowledge(extKnowledge.getLocal(), itsBinaryTarget);
//			itsGlobalKnowledge = new GlobalKnowledge(extKnowledge.getGlobal(), itsBinaryTarget);
			ExternalKnowledgeFileLoader e = new ExternalKnowledgeFileLoader(itsTable, theConditionBaseSet);
			itsLocalKnowledge  = new LocalKnowledge(itsBinaryTarget, e.getKnowledge(false));
			itsGlobalKnowledge = new GlobalKnowledge(itsBinaryTarget, e.getKnowledge(true));
			Log.logCommandLine("");
		}
	}

	private void logExperimentSettings(ConditionBaseSet theConditionBaseSet)
	{
		Log.logCommandLine("");
		Log.logCommandLine(itsSearchParameters.getTargetConcept().toString());
		Log.logCommandLine(itsSearchParameters.toString());
		Log.logCommandLine(theConditionBaseSet.toString());
	}

	private static final void prepareData(BitSet theBinaryTarget, List<Column> theColumns)
	{
		//Log.logCommandLine("SubgroupDiscovery.prepareData(): do not change data until mining completes");
		//Log.logCommandLine("  so no MetaDataWindow enable/disable attribute, attribute type, missing value\n");

		Timer aTotal = new Timer();

		for (Column c : theColumns)
		{
			switch (c.getType())
			{
				// FIXME sort itsDistinctValues, reset indexes, create return
				// wraps itsDistinctValues in shared unmodifiable Collection
				case NOMINAL :
				{
					c.buildSharedDomain();
					break;
				}
				case NUMERIC :
				{
					Log.logCommandLine(c.getName());
					Timer t = new Timer();
					c.buildSorted(theBinaryTarget);
					//Log.logCommandLine(t.getElapsedTimeString());
					break;
				}
				case ORDINAL :
				{
					throw new AssertionError("SubgroupDiscovery.prepareData(): " + c.getType());
				}
				// nothing for now, but this will change for SINGLE_NOMINAL
				case BINARY :
				{
					break;
				}
				default :
					throw new AssertionError("SubgroupDiscovery.prepareData(): " + c.getType());
			}
		}

		//Log.logCommandLine("total preparation time:");
		//Log.logCommandLine(aTotal.getElapsedTimeString());
	}

	// direct computation is relevant only for a SINGLE_NOMINAL target as it
	// relates to tracking the true positive counts, methods using this setting:
	//
	// evaluateBinary()                    : for BINARY description Attributes
	// evaluateNominalElementOf()          : for NOMINAL description Attributes
	// evaluateNumericRegularSingleBinary(): for NUMERIC description Attributes
	// evaluateNumericBestInterval         : for NUMERIC description Attributes
	//
	// for PROP_SCORE_WRACC/PROP_SCORE_RATIO directComputation() is not possible
	private final boolean isDirectSingleBinary()
	{
		EnumSet<QM> anInvalid = EnumSet.of(QM.PROP_SCORE_WRACC, QM.PROP_SCORE_RATIO);

		// checking this all the time is a bit wasteful, but fine for now
		SearchParameters s = itsSearchParameters;
		return ((s.getTargetType() == TargetType.SINGLE_NOMINAL) && !anInvalid.contains(s.getQualityMeasure())
				&& (s.getNumericStrategy() != NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_ALL && s.getNumericStrategy() != NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_BEST));
	}

	private static final void deleteSortData(List<Column> theColumns)
	{
		for (Column c : theColumns)
			c.removeSorted();
	}

	private final CandidateQueue getCandidateQueueFromBeamSeed()
	{
		//List<ConditionList> aBeamSeed = itsSearchParameters.getBeamSeed();
		List<ConditionList> aBeamSeed = itsSearchParameters.getBeamSeed();
		//ConditionList aFirstConditionList = aBeamSeed.get(0);
		ConditionList aFirstConditionList = aBeamSeed.get(0);
		//TODO there may be no members, in which case the following statement crashes
		BitSet aFirstMembers = itsTable.evaluate(aFirstConditionList);
		Subgroup aFirstSubgroup = new Subgroup(aFirstConditionList, aFirstMembers, itsResult);
		CandidateQueue aSeededCandidateQueue = new CandidateQueue(itsSearchParameters, new Candidate(aFirstSubgroup));
		// no useful Refinement from this can result
		if (aFirstSubgroup.getCoverage() <= 1)
			aSeededCandidateQueue.removeFirst();

		int aNrEmptySeeds = 0;
		//for (ConditionList aConditionList : aBeamSeed)
		for (ConditionList aConditionList : aBeamSeed)
		//for (int i = 1, j = aBeamSeed.size(); i < j; ++i)
		{
			//ConditionList aConditionList = aBeamSeed.get(i);
			Log.logCommandLine(aConditionList.toString());
			BitSet aMembers = itsTable.evaluate(aConditionList);
			if (aMembers.cardinality() > 0)
			{
				Subgroup aSubgroup = new Subgroup(aConditionList,aMembers,itsResult);
				aSeededCandidateQueue.add(new Candidate(aSubgroup));
			}
			else
				aNrEmptySeeds++;
		}

		if (aNrEmptySeeds > 0)
			Log.logCommandLine("Number of empty seeds discarded: "+aNrEmptySeeds);
		Log.logCommandLine("Beam Seed size: " + aSeededCandidateQueue.size());

		return aSeededCandidateQueue;
	}

	private final boolean isTimeToStop()
	{
		if (System.currentTimeMillis() > itsEndTime)
			return true;

		if ((itsMainWindow != null) && ((MiningWindow) itsMainWindow).isCancelled())
			return true;

		return false;
	}

	// NOTE itsCandidateCount and currently refined subgroup are unrelated
	private final void setTitle(Subgroup theSubgroup)
	{
		if (itsMainWindow == null)
			return;

		// do not even try to obtain lock
		if ((System.nanoTime() - itsThen) < INTERVAL)
			return;

		if (!itsClockLock.tryLock())
			return;

		try
		{
			String aCurrent = theSubgroup.toString();

			StringBuilder sb = new StringBuilder(aCurrent.length() + 32);
			sb.append("d=").append(Integer.toString(theSubgroup.getDepth()))
				.append(" cands=").append(FORMATTER.format(itsCandidateCount.get()))
				.append(" evaluating: ").append(aCurrent);

			itsMainWindow.setTitle(sb.toString());

			itsThen = System.nanoTime();
		}
		finally
		{
			itsClockLock.unlock();
		}
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// start of essential parallelisation code                          /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	/*
	 * After Test is done, it releases its semaphore, so ExecutorService can start a new Test.
	 */
	private class Test implements Runnable
	{
		private final Subgroup itsSubgroup;
		private final Semaphore itsSemaphore;
		private final List<ColumnConditionBases> itsColumnConditionBasesSet;
		private final Fltr itsFilter;

//		public Test(Subgroup theSubgroup, Semaphore theSemaphore, ConditionBaseSet theConditionBaseSet)
		public Test(Subgroup theSubgroup, Semaphore theSemaphore, List<ColumnConditionBases> theColumnConditionBasesSet, Fltr theFilter)
		{
			itsSubgroup                = theSubgroup;
			itsSemaphore               = theSemaphore;
			itsColumnConditionBasesSet = theColumnConditionBasesSet;
			itsFilter                  = theFilter;
		}

		@Override
		public void run()
		{
			if (false) { runX(); return; }

			// Subgroup.getMembers() creates expensive clone, reuse
			BitSet aParentMembers = itsSubgroup.getMembers();
			int aParentCoverage   = itsSubgroup.getCoverage();
			assert (aParentMembers.cardinality() == aParentCoverage);

			ConditionList aConditionList = itsSubgroup.getConditions();
			int nextSkipSG = itsFilter.nextSkip(aConditionList, 0);
			assert ((nextSkipSG == Fltr.NOTHING_TO_SKIP) || ((nextSkipSG >= 0) && (nextSkipSG < aConditionList.size())));
			Column toSkip = (nextSkipSG != Fltr.NOTHING_TO_SKIP ?  aConditionList.getCanonical(nextSkipSG).getColumn() : null);

			for (int i = 0, j = itsColumnConditionBasesSet.size(); i < j && !isTimeToStop(); ++i)
			{
				ColumnConditionBases ccb = itsColumnConditionBasesSet.get(i);
				// FIXME one time operation per mine(), but cumbersome, optimise
				ConditionBase cb = ccb.get(0);
				if (cb == null) cb = ccb.get(1);
				if (cb == null) cb = ccb.get(2);

				// temporary: during testing do not actually continue OUT
				boolean skipThisCB = false;
				if (cb.getColumn() == toSkip)
				{
					skipThisCB = true;
					itsSkipCount.incrementAndGet();
					nextSkipSG = itsFilter.nextSkip(aConditionList, nextSkipSG+1);
					assert ((nextSkipSG == Fltr.NOTHING_TO_SKIP) || ((nextSkipSG >= 0) && (nextSkipSG < aConditionList.size())));
					toSkip = (nextSkipSG != Fltr.NOTHING_TO_SKIP ?  aConditionList.getCanonical(nextSkipSG).getColumn() : null);
					if (DEBUG_PRINTS_FOR_SKIP)
						Log.logCommandLine(String.format("EC-SKIP\t%s AND %s%n", itsSubgroup, cb));
					if (USE_SKIP_FILTER)
						continue;
				}

				if (!skipThisCB && DEBUG_PRINTS_FOR_SKIP)
					Log.logCommandLine(String.format("NO-SKIP\t%s AND %s%n", itsSubgroup, cb));

				// using a TestFactory some of the if-checks could be removed
				// ValueSets would never occur when !useBestValueSets
				// for BestInterval the reasoning is the same
				// TODO assert mutual-exclusivity of:
				//   ClassLabel+EQUALS                v. ValueSet+ELEMENT_OF
				//   regular+(EQUALS/BETWEEN,LEQ,GEQ) v. BestInterval+BETWEEN
				if (ccb instanceof ColumnConditionBasesBinary)
					evaluateBinary(itsSubgroup, aParentMembers, (ColumnConditionBasesBinary) ccb);
				else if (ccb instanceof ColumnConditionBasesNominalEquals)
					evaluateNominalEquals(itsSubgroup, aParentMembers, (ColumnConditionBasesNominalEquals) ccb);
				else if (ccb instanceof ColumnConditionBasesNominalElementOf)
					evaluateNominalElementOf(itsSubgroup, aParentMembers, (ColumnConditionBasesNominalElementOf) ccb);
				else if (ccb instanceof ColumnConditionBasesNumericRegular)
					evaluateNumericRegular(itsSubgroup, aParentMembers, (ColumnConditionBasesNumericRegular) ccb);
				else if (ccb instanceof ColumnConditionBasesNumericIntervals)
					evaluateNumericIntervals(itsSubgroup, aParentMembers, (ColumnConditionBasesNumericIntervals) ccb);
				else
					throw new AssertionError("Test.run() unexpected subclass of ColumnConditionBases");
			}

			itsSemaphore.release();
		}

		public void runX()
		{
			// NOTE
			// checking this all the time is wasteful, could be parameters to
			// constructor, but this version is for debug only, run() will be
			// the final implementation after testing, it has lower complexity
			boolean useBestValueSets        = itsSearchParameters.getNominalSets();
			NumericStrategy ns              = itsSearchParameters.getNumericStrategy();
			boolean useBestIntervals        = (ns == NumericStrategy.NUMERIC_INTERVALS);
			EnumSet<NumericStrategy> ab     = EnumSet.of(NumericStrategy.NUMERIC_ALL, NumericStrategy.NUMERIC_BEST);
//			EnumSet<NumericStrategy> bbbi   = EnumSet.of(NumericStrategy.NUMERIC_BEST_BINS, NumericStrategy.NUMERIC_BINS, NumericStrategy.NUMERIC_INTERVALS);
			boolean isAllBest               = ab.contains(ns);
			boolean isBestBinsBins          = EnumSet.of(NumericStrategy.NUMERIC_BEST_BINS, NumericStrategy.NUMERIC_BINS).contains(ns);
			// currently the choice between ALL|BEST and BEST_BINS|BINS|INTERVAL
			// is not binary, because of the (deprecated) consecutive bins
			// when the consecutive bins option are removed the following assert
			// can be used to check the (then) binary situation
			// such that a single boolean for NumericStrategy is sufficient
//			boolean isBestBinsBinsIntervals = bbbi.contains(ns);
//			assert (EnumSet.allOf(NumericStrategy.class).complementOf(ab).equals(bbbi));

			// Subgroup.getMembers() creates expensive clone, reuse
			BitSet aParentMembers = itsSubgroup.getMembers();
			int aParentCoverage   = itsSubgroup.getCoverage();
			assert (aParentMembers.cardinality() == aParentCoverage);

			// NOTE toCanonicalOrder() would be replaced by getCanonical(index)
			ConditionList aParentConditions = itsSubgroup.getConditions();
			Condition[] ca = ConditionListBuilder.toCanonicalOrder(aParentConditions);
			int size = ca.length;
			Condition last = ((size == 0) ? null : aParentConditions.get(size-1));
			// NOTE getNominalValueSet() returns null when last conjunct is not
			//      a ValueSet-Condition, likewise for getNumericInterval()
			Column biColumn = (                      useBestIntervals && (last != null) && (last.getNumericInterval() != null)) ? last.getColumn() : null;
			Column vsColumn = ((biColumn == null) && useBestValueSets && (last != null) && (last.getNominalValueSet() != null)) ? last.getColumn() : null;

			int NO_EQ_LEQ_GEQ_CHECK = -1;
		OUT: for (int i = 0, j = itsColumnConditionBasesSet.size(), k = ca.length; i < j && !isTimeToStop(); ++i)
			{
				ColumnConditionBases ccb = itsColumnConditionBasesSet.get(i);
				ConditionBase cb = ccb.get(0);
				boolean cbEqualsOrElementOfOrBetween = (cb != null);
				// TODO assert

				// TODO based on these checks, set a value outside the loop that
				//      indicated whether evaluate*() should check for duplicate
				//      Conjuncts, based on the added Condition.VALUE
				//      as the check is performed on the VALUE, it can not occur
				//      here, on the ConditionBase (as it has no VALUE)
				// this situation can arise for:
				// ValueSet when a Column occurs in aParentConditions, but not
				//          as last search-order conjunct (which is handled by
				//          the IN-SKIP) and the to-be-added ConditionBase
				//          concerns the same Column the added ValueSet VALUE
				//          added ValueSet VALUE might be the same, or smaller
				//          than, the existing one, requiring a check
				// BestInterval as for ValueSet
				//
				// temporary: during testing do not actually continue OUT
				boolean skipThisCB = false;
				int eq_WarningAtIndex = NO_EQ_LEQ_GEQ_CHECK;
				int leqWarningAtIndex = NO_EQ_LEQ_GEQ_CHECK;
				int geqWarningAtIndex = NO_EQ_LEQ_GEQ_CHECK;

				if (cbEqualsOrElementOfOrBetween)
				{
					Column cbc = cb.getColumn();
					// when the last conjunct in a ConditionList is a ValueSet,
					// adding another ValueSet for the same Column never leads
					// to a valid refinement, the algorithm just selects the
					// exact same ValueSet
					// for BestInterval the reasoning is the same
					//
					// FIXME assert that only one Operator is used for
					// BestValueSet and BestInterval: use of EQUALS and BETWEEN
					// must be exclusive
					if ((cbc == biColumn) || (cbc == vsColumn))
					{
						skipThisCB = true;
						itsSkipCount.incrementAndGet();
						if (DEBUG_PRINTS_FOR_SKIP)
							Log.logCommandLine(String.format("IN-SKIP\t%s AND %s%n", itsSubgroup, cb));
						if (skipThisCB && (BEST_VALUESET_AT_LAST_POSITION_SKIP || BEST_INTERVAL_AT_LAST_POSITION_SKIP))
							continue OUT;
					}
					// NOTE when enabling continue OUT above: remove !skipThisCB
					if (!skipThisCB)
					{
						Operator cbo = cb.getOperator();
						boolean isCBOEquals = (cbo == Operator.EQUALS);

						// NOTE binary choice in the loop below relies on this
						assert (
								(isCBOEquals && (
												(                     (cbc.getType() == AttributeType.BINARY )) ||
												(!useBestValueSets && (cbc.getType() == AttributeType.NOMINAL)) ||
												(!useBestIntervals && (cbc.getType() == AttributeType.NUMERIC) && isAllBest))
								)
								|| // !isEquals
								(
												( useBestValueSets && (cbc.getType() == AttributeType.NOMINAL) && (cbo == Operator.ELEMENT_OF)) ||
												( useBestIntervals && (cbc.getType() == AttributeType.NUMERIC) && (cbo == Operator.BETWEEN   )) ||
												(!useBestIntervals && (cbc.getType() == AttributeType.NUMERIC) && (cbo == Operator.BETWEEN   ) && (EnumSet.of(NumericStrategy.NUMERIC_BEST_BINS, NumericStrategy.NUMERIC_BINS).contains(ns)))
								)
								);

						// NOTE alternative run() avoids loop by using ordering
						for (int l = 0; l < k; ++l)
						{
							Condition sgCondition = ca[l];
							Column sgColumn       = sgCondition.getColumn();
							if (cbc != sgColumn)
								continue;

							AttributeType sgColumnType = sgColumn.getType();
							Operator sgOperator        = sgCondition.getOperator();

							// skipThisCB is used only for testing, as
							// 'continue OUT' is disabled for now
							// EQUALS on same Column is never useful for BINARY
							// and NOMINAL, and for NUMERIC in combination with
							// NumericOperatorSetting.NUMERIC_EQ (where EQUALS
							// is the only Operator used, for NUMERIC_ALL, which
							// also includesEquals(), the refinement is useful
							// as it might reduce the Subgroup size, for example
							// 'Age <= 32 AND Age = 32')
							// NOTE that BEST_BINS|BINS uses BETWEEN, not EQUALS
							if ((sgColumnType == AttributeType.BINARY) || ((sgColumnType == AttributeType.NOMINAL) && !useBestValueSets))
							{
								assert (isCBOEquals && (sgOperator == Operator.EQUALS));
								skipThisCB = true;
								itsSkipCount.incrementAndGet();
								if (DEBUG_PRINTS_FOR_SKIP)
									Log.logCommandLine(String.format("EC-SKIP\t%s AND %s%n", itsSubgroup, cb));
								if (skipThisCB && BINARY_NOMINAL_EQUALS_SKIP)
									continue OUT;
								break;
							}
//							if ((sgColumnType == AttributeType.NOMINAL) && useBestValueSets)
//							{
//								// ValueSet at last position should have been
//								// caught above
//								// FIXME the ValueSet check can be removed, it
//								//       is redundant, the ValueSet code now
//								//       always checks whether a child coverage
//								//       is smaller than the parent coverage
//								//       BEFORE a new ValueSet Condition is
//								//       created
//								//       that check handles both equal ValueSets
//								//       and unequal ones, no warning required
//								assert (l < (k-1));
//								assert (!isCBOEquals && (sgOperator == Operator.ELEMENT_OF));
//								eq_WarningAtIndex = l;
//								System.out.format("VSWARN%d\t%s AND %s%n", l, itsSubgroup, cb);
//								break; // DO NOT continue OUT;
//							}
//							if ((sgColumnType == AttributeType.NUMERIC) && useBestIntervals)
//							{
//								// BestInterval: see comments for ValueSet above
//								assert (l < (k-1));
//								assert (!isCBOEquals && (sgOperator == Operator.BETWEEN));
//								eq_WarningAtIndex = l;
//								System.out.format("BIWARN%d\t%s AND %s%n", l, itsSubgroup, cb);
//								// continue OUT is disabled for now
//								break; // DO NOT continue OUT;
//							}
							if ((sgColumnType == AttributeType.NUMERIC) && !useBestIntervals && isAllBest)
							{
								// no Refinement possible for this Column
								if (sgOperator == Operator.EQUALS)
								{
									assert (EnumSet.of(NumericOperatorSetting.ALL, NumericOperatorSetting.EQ).contains(itsSearchParameters.getNumericOperatorSetting()));
									skipThisCB = true;
									itsSkipCount.incrementAndGet();
									if (DEBUG_PRINTS_FOR_SKIP)
										Log.logCommandLine(String.format("EC-SKIP\t%s AND %s%n", itsSubgroup, cb));
									if (skipThisCB && NUMERIC_EQ_SKIP)
										continue OUT;
									break;
								}
								else // not required after break, but more clear
								{
									// FIXME
									// this check can be removed, it is
									// redundant, both
									// evaluateNumericRegularGeneric() and
									// evaluateNumericRegularSingleBinary()
									// always check whether a child coverage is
									// smaller than the parent coverage BEFORE
									// a new Condition is created
									// technically this check is correct, and it
									// can be performed slightly before the size
									// check (by one or two statement)
									// but setting up the warning and performing
									// the if-checks all the time costs more
									// than it saves
									assert (EnumSet.of(Operator.LESS_THAN_OR_EQUAL, Operator.GREATER_THAN_OR_EQUAL).contains(sgOperator) && EnumSet.of(NumericOperatorSetting.NORMAL, NumericOperatorSetting.LEQ, NumericOperatorSetting.GEQ, NumericOperatorSetting.ALL).contains(itsSearchParameters.getNumericOperatorSetting()));
									eq_WarningAtIndex = l;
									if (DEBUG_PRINTS_FOR_WARN)
										Log.logCommandLine(String.format("EQWARN%d\t%s AND %s%n", l, itsSubgroup, cb));
									break; // DO NOT continue OUT;
								}
							}
							// see NOTE above on non-binary allBest/BestBinsBins
							// FIXME this check can be removed, see above
							if ((sgColumnType == AttributeType.NUMERIC) && !useBestIntervals && isBestBinsBins)
							{
								// warn, never skip
								assert (!isCBOEquals && (EnumSet.of(Operator.BETWEEN, Operator.LESS_THAN_OR_EQUAL, Operator.GREATER_THAN_OR_EQUAL).contains(sgOperator) && EnumSet.of(NumericOperatorSetting.NORMAL, NumericOperatorSetting.LEQ, NumericOperatorSetting.GEQ, NumericOperatorSetting.ALL).contains(itsSearchParameters.getNumericOperatorSetting())));
								eq_WarningAtIndex = l;
								if (DEBUG_PRINTS_FOR_WARN)
									Log.logCommandLine(String.format("INWARN%d\t%s AND %s%n", l, itsSubgroup, cb));
								break; // DO NOT continue OUT;
							}
						}
					}
				}
				// FIXME these checks can be removed, see above
				else // !cbEqualsOrElementOfOrBetween
				{
					boolean hasLEQ = (ccb.get(1) != null);
					boolean hasGEQ = (ccb.get(2) != null);
					cb = (hasLEQ ? ccb.get(1) : ccb.get(2));
					Column cbc   = cb.getColumn();

					// only NUMERIC + LEQ|GEQ (for cb, sg can be anything)
					assert (!useBestIntervals && (EnumSet.of(Operator.LESS_THAN_OR_EQUAL, Operator.GREATER_THAN_OR_EQUAL).contains(cb.getOperator())));

					// FIXME do not loop by implementing ConditionBase.id
					for (int l = 0; l < k; ++l)
					{
						if (cbc != ca[l].getColumn())
							continue;

						Operator cao = ca[l].getOperator();

						if (hasLEQ && (cao == Operator.LESS_THAN_OR_EQUAL))
						{
							leqWarningAtIndex = l;
							if (DEBUG_PRINTS_FOR_WARN)
								Log.logCommandLine(String.format("LEWARN%d\t%s AND %s %s%n", l, itsSubgroup, cbc.getName(), cao));
							if (!hasGEQ)
								break; // DO NOT continue OUT;
							continue;
						}

						assert (hasGEQ);
						if (cao == Operator.GREATER_THAN_OR_EQUAL)
						{
							geqWarningAtIndex = l;
							if (DEBUG_PRINTS_FOR_WARN)
								Log.logCommandLine(String.format("GEWARN%d\t%s AND %s %s%n", l, itsSubgroup, cbc.getName(), cao));
							if (!hasLEQ)
								break; // DO NOT continue OUT;
							continue;
						}
					}
				}
				if (cbEqualsOrElementOfOrBetween && !skipThisCB && DEBUG_PRINTS_FOR_SKIP)
					Log.logCommandLine(String.format("NO-SKIP\t%s AND %s%n", itsSubgroup, cb));
				// test code; should be split over AttributeTypes and settings
				if (((eq_WarningAtIndex != NO_EQ_LEQ_GEQ_CHECK) || (leqWarningAtIndex != NO_EQ_LEQ_GEQ_CHECK) || (geqWarningAtIndex != NO_EQ_LEQ_GEQ_CHECK)) && DEBUG_PRINTS_FOR_SKIP)
					Log.logCommandLine(String.format("EQ=%d\tLEQ=%d\tGEQ=%d%n", eq_WarningAtIndex, leqWarningAtIndex, geqWarningAtIndex));

				// using a TestFactory some of the if-checks could be removed
				// ValueSets would never occur when !useBestValueSets
				// for BestInterval the reasoning is the same
				// TODO assert mutual-exclusivity of:
				//   ClassLabel+EQUALS                v. ValueSet+ELEMENT_OF
				//   regular+(EQUALS/BETWEEN,LEQ,GEQ) v. BestInterval+BETWEEN
				if (ccb instanceof ColumnConditionBasesBinary)
					evaluateBinary(itsSubgroup, aParentMembers, (ColumnConditionBasesBinary) ccb);
				else if (!useBestValueSets && ccb instanceof ColumnConditionBasesNominalEquals)
					evaluateNominalEquals(itsSubgroup, aParentMembers, (ColumnConditionBasesNominalEquals) ccb);
				else if ( useBestValueSets && ccb instanceof ColumnConditionBasesNominalElementOf)
					evaluateNominalElementOf(itsSubgroup, aParentMembers, (ColumnConditionBasesNominalElementOf) ccb);
				else if (!useBestIntervals && ccb instanceof ColumnConditionBasesNumericRegular)
					evaluateNumericRegular(itsSubgroup, aParentMembers, (ColumnConditionBasesNumericRegular) ccb);
				else if ( useBestIntervals && ccb instanceof ColumnConditionBasesNumericIntervals)
					evaluateNumericIntervals(itsSubgroup, aParentMembers, (ColumnConditionBasesNumericIntervals) ccb);
				else
					throw new AssertionError("Test.run() unexpected subclass of ColumnConditionBases");
			}

			itsSemaphore.release();
		}
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// when done                                                        /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	private void postMining(long theBeginTime)
	{
		if (itsSearchParameters.getFilterSubgroups())
			itsResult.filterSubgroups();

		int aNrSubgroups = getNumberOfSubgroups();

		// before mining end: post-processing must be included in total run time
		if (itsSearchParameters.getSearchStrategy() == SearchStrategy.COVER_BASED_BEAM_SELECTION)
		{
			itsCandidateQueueSizes.add(-aNrSubgroups);
			postProcessCBSS();
		}

		// for CBSS this reports aNrSubgroups, not 100 (might change one day)
		long anElapsedTime = (System.currentTimeMillis() - theBeginTime);
		Process.echoMiningEnd(anElapsedTime, aNrSubgroups);

		long aNrCandidates = itsCandidateCount.get();
		setTitle(itsMainWindow, anElapsedTime, aNrCandidates);

		deleteSortData(itsTable.getColumns());

		// postProcessCook() output is supposed to go in between
		Log.logCommandLine("number of candidates: " + aNrCandidates);
		postProcessCook();
		// for CBSS this reports 100, not aNrSubgroups
		Log.logCommandLine("number of subgroups : " + getNumberOfSubgroups());

		// assign 1 to n to subgroups, for future reference in subsets
		itsResult.setIDs();

		// TODO probably also should be called before Process.echoMiningEnd()
		postProcessMultiLabelAutoRun(); // IDs must be set first,  might set new
	}

	private static final void setTitle(JFrame theMainWindow, long theElapsedTime, long theNrCandidates)
	{
		if (theMainWindow == null)
			return;

		long minutes = theElapsedTime / 60_000l;
		float seconds = (theElapsedTime % 60_000l) / 1_000.0f;

		String aMessage = (minutes == 0 ? "" : (String.format(" %d minute%s and", minutes, (minutes == 1 ? "" : "s"))));
		aMessage = String.format(Locale.US, "%s %.3f seconds", aMessage, seconds);
		aMessage = String.format("Finished: evaluated %s candidate%s in%s.",
									FORMATTER.format(theNrCandidates),
									(theNrCandidates == 1 ? "" : "s"),
									aMessage);

		theMainWindow.setTitle(aMessage);
	}

	private void postProcessCook()
	{
		if (itsSearchParameters.getQualityMeasure() != QM.COOKS_DISTANCE)
			return;

		Log.logCommandLine("Bound seven computed "   + itsBoundSevenCount + " times");
		Log.logCommandLine("Bound six   computed "   + itsBoundSixCount   + " times");
		Log.logCommandLine("Bound five  computed "   + itsBoundFiveCount  + " times");
		Log.logCommandLine("Bound four  computed "   + itsBoundFourCount  + " times");
		Log.logCommandLine("Bound seven fired "      + itsBoundSevenFired + " times");
		Log.logCommandLine("Bound six   fired "      + itsBoundSixFired   + " times");
		Log.logCommandLine("Bound five  fired "      + itsBoundFiveFired  + " times");
		Log.logCommandLine("Bound four  fired "      + itsBoundFourFired  + " times");
		Log.logCommandLine("Rank deficient models: " + itsRankDefCount);
	}
/*
TODO for stable jar, disabled, causes compile errors, reinstate later
	private void addToBuffer(Subgroup theSubgroup )
	{
		int aCoverage = theSubgroup.getCoverage();
		itsBaseRM.computeRemovedIndices(theSubgroup.getMembers(), aCoverage);
		itsBaseRM.updateSquaredResidualSum();
		itsBaseRM.updateRemovedTrace();
		double aPriority = itsBaseRM.computeBoundFour(aCoverage);
		Log.logCommandLine(theSubgroup.getConditions().toString() + " --- bound : " + aPriority);
		// @deprecated constructor
		itsBuffer.add(new Candidate(theSubgroup, aPriority));
	}
*/

	private void flushBuffer()
	{
		if (itsBuffer == null)
			return;
		for (Iterator<Candidate> it = itsBuffer.iterator(); it.hasNext(); )
		{
			Candidate aCandidate = it.next();
			Subgroup aSubgroup = aCandidate.getSubgroup();
			checkAndLog(aSubgroup, itsNrRows);
		}
		itsBuffer = new TreeSet<Candidate>();
	}

	private void postProcessCBSS()
	{
		assert (itsSearchParameters.getSearchStrategy() == SearchStrategy.COVER_BASED_BEAM_SELECTION);

		// just for cover-based beam search post selection
		SubgroupSet aSet = itsResult.postProcess(itsSearchParameters.getSearchStrategy());

		// FIXME MM hack to deal with strange postProcess implementation
		if (itsResult != aSet)
		{
			// no reassign, we want itsResult to be final
			itsResult.clear();
			itsResult.addAll(aSet);
//			// in COVER_BASED_BEAM_SELECTION, subgroups may have been removed
//			itsResult.setIDs();
		}
	}

	private void postProcessMultiLabelAutoRun()
	{
		if (!((itsSearchParameters.getTargetType() == TargetType.MULTI_LABEL) && itsSearchParameters.getPostProcessingDoAutoRun()))
			return;

		if (itsResult.isEmpty())
			return;

		// Create quality measures on whole dataset
		Log.logCommandLine("Creating quality measures.");
		int aPostProcessingCount = itsSearchParameters.getPostProcessingCount();
		double aPostProcessingCountSquare = Math.pow(aPostProcessingCount, 2.0);

		QualityMeasure[] aQMs = new QualityMeasure[aPostProcessingCount];
		for (int i = 0; i < aPostProcessingCount; i++)
		{
			Bayesian aGlobalBayesian = new Bayesian(itsBinaryTable);
			aGlobalBayesian.climb();
			aQMs[i] = new QualityMeasure(itsSearchParameters, aGlobalBayesian.getDAG(), itsNrRows);
		}

		// Iterate over subgroups
		SubgroupSet aNewSubgroupSet = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsSelection, itsNrRows);
		// most methods of SubgroupSet are not thread save, but this is
		// no problem for this method as it is run by a single thread
		// however all itsResult sets, of all refinement depths,  will
		// be kept in memory
		// see comment in SubgroupSet.postProcess()
		for (Subgroup s : itsResult)
		{
			// FIXME
			// because s.getID() is used this method can not be called before
			// Process.echoMiningEnd() in postMining()
			// this is unnecessary, as the same output can be achieved by using
			// an old style (non-enhanced) for loop replacing s.getId() by i+1
			// then the post-processing time can be included in the run time
			Log.logCommandLine("Postprocessing subgroup " + s.getID());
			double aTotalQuality = 0.0;
			BinaryTable aSubgroupTable = itsBinaryTable.selectRows(s.getMembers());
			for (int i = 0; i < aPostProcessingCount; i++)
			{
				Bayesian aLocalBayesian = new Bayesian(aSubgroupTable);
				aLocalBayesian.climb();
				s.setDAG(aLocalBayesian.getDAG());
				for (int j = 0; j < aPostProcessingCount; j++)
					aTotalQuality += aQMs[j].calculate(s);
			}
			s.setMeasureValue(aTotalQuality / aPostProcessingCountSquare);
			s.renouncePValue();
			aNewSubgroupSet.add(s);
		}
		aNewSubgroupSet.setIDs();
		itsResult.clear();
		itsResult.addAll(aNewSubgroupSet);
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// start of Refinement generation and related methods               /////
	///// after Refinement generation follows evaluateCandidate()          /////
	///// but some methods bypass it and compute the result directly       /////
	///// binary                                                           /////
	///// nominal                                                          /////
	///// numeric                                                          /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	/*
	 * NOTE
	 * itsSubgroup / theMembers for each Refinement from the same RefinementList
	 * are always the same, supply a cached version of theMembers, as
	 * Subgroup.getMembers() creates a clone on each call
	 *
	 * NOTE
	 * all required data can come from a single Refinement parameter, but
	 * theParentMembers are obtained through an expensive Subgroup.getMembers()
	 * call, which creates a new copy every time, therefore it is cached
	 *
	 * NOTE
	 * these asserts are omitted as only the two mine() methods call this one:
	 * (theParentMembers.cardinality() == theParentCoverage)
	 * (theRefinement.getSubgroup().getMembers().equals(theParentMembers))
	 */
	private final void evaluateNominalBinaryRefinements(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		assert (false); // code should never get here

		ConditionBase aConditionBase = theRefinement.getConditionBase();

		// split code paths for ValueSet/class labels (nominal/numeric/binary)
		if (aConditionBase.getOperator() == Operator.ELEMENT_OF)
		{

			// currently BestValueSet implies the target type is SINGLE_NOMINAL
			assert (itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL);
			assert (itsSearchParameters.getNominalSets());

			//evaluateNominalElementOf(theParentMembers, theParentCoverage, theRefinement);
			return;
		}

		assert (aConditionBase.getOperator() == Operator.EQUALS);

		switch (aConditionBase.getColumn().getType())
		{
			case ORDINAL : throw new AssertionError(AttributeType.ORDINAL);
			default      : throw new AssertionError(aConditionBase.getColumn().getType());
		}
	}

	// XXX (c = false) is checked first, (c = true) is conditionally, it depends on data and search characteristics  whether this is the best order
	private final void evaluateBinary(Subgroup theParent, BitSet theParentMembers, ColumnConditionBasesBinary theColumnConditionBases)
	{
		assert (theColumnConditionBases.get(0).getOperator() == Operator.EQUALS);

		int aParentCoverage = theParent.getCoverage();
		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theColumnConditionBases.get(0);
		Column aColumn = aConditionBase.getColumn();

		BitSet aChildMembers = aColumn.evaluateBinary(theParentMembers, false);
		int aChildCoverage = (aChildMembers == null ? 0 : aChildMembers.cardinality());

		// ignore both f and t
		if ((aChildCoverage == 0) || (aChildCoverage == aParentCoverage))
			return;

		int aNrTruePositives = INVALID_NR_TRUE_POSITIVES;

		// check for (aColumn = false)
		if (aChildCoverage >= itsMinimumCoverage)
			aNrTruePositives = evaluateBinaryRefinementsHelper(theParent, new Condition(aConditionBase, false), aChildMembers, aChildCoverage);

		if (isTimeToStop())
			return;

		// check for (aColumn = true), do this only when useful
		// everything that is not positive is negative
		aChildCoverage = (aParentCoverage - aChildCoverage);
		if (aChildCoverage >= itsMinimumCoverage)
		{
			Condition aCondition = new Condition(aConditionBase, true);

			// (aColumn = false) is not evaluated when < itsMinimumCoverage
			// NOTE if-check can never be true unless isDirectSetting()
			if (aNrTruePositives != INVALID_NR_TRUE_POSITIVES)
			{
				aNrTruePositives = ((int) theParent.getTertiaryStatistic()) - aNrTruePositives;
				Subgroup aChildSubgroup = directComputation(theParent, aCondition, itsQualityMeasure, aChildCoverage, aNrTruePositives);
				checkAndLog(aChildSubgroup, aParentCoverage);
			}
			else
			{
				aChildMembers = aColumn.evaluateBinary(theParentMembers, true);
				evaluateBinaryRefinementsHelper(theParent, aCondition, aChildMembers, aChildCoverage);
			}
		}
	}

	// return is always INVALID_NR_TRUE_POSITIVES when !isDirectSetting()
	private static final int INVALID_NR_TRUE_POSITIVES = -1;
	private final int evaluateBinaryRefinementsHelper(Subgroup theParent, Condition theAddedCondition, BitSet theChildMembers, int theChildCoverage)
	{
		boolean isDirectSetting = isDirectSingleBinary();
		final Subgroup aChild;
		int aNrTruePositives = INVALID_NR_TRUE_POSITIVES;

		if (isDirectSetting)
		{
			// safe: it is a clone, and subgroup coverage is stored: theCoverage
			theChildMembers.and(itsBinaryTarget);
			aNrTruePositives = theChildMembers.cardinality();

			aChild = directComputation(theParent, theAddedCondition, itsQualityMeasure, theChildCoverage, aNrTruePositives);
		}
		else
		{
			if ((itsFilter != null) && !itsFilter.isUseful(theParent.getConditions(), theAddedCondition))
				return INVALID_NR_TRUE_POSITIVES;

			aChild = theParent.getRefinedSubgroup(theAddedCondition, theChildMembers, theChildCoverage);

			// SINGLE_NOMINAL only, not for PROP_SCORE_WRACC and PROP_SCORE_RATIO
			if (isDirectSetting)
				aNrTruePositives = (int) aChild.getTertiaryStatistic();
		}

		checkAndLog(aChild, theParent.getCoverage());

		return aNrTruePositives;
	}

	// two methods, as BestValueSet and BestInterval already computed the score
	private static final Subgroup directComputation(Subgroup theParent, Condition theAddedCondition, QualityMeasure theQualityMeasure, int theChildCoverage, int theNrTruePositives)
	{
		double aQualityScore = theQualityMeasure.calculate(theNrTruePositives, theChildCoverage);
		return directComputation(theParent, theAddedCondition, aQualityScore, theChildCoverage, theNrTruePositives);
	}

	private static final Subgroup directComputation(Subgroup theParent, Condition theAddedCondition, double theQualityScore, int theChildCoverage, int theNrTruePositives)
	{
		// FIXME MM q is only cast to float to make historic results comparable
		float  q = (float) theQualityScore;
		double s = ((double) theNrTruePositives) / theChildCoverage;
		double t = ((double) theNrTruePositives);

		return theParent.getRefinedSubgroup(theAddedCondition, q, s, t, theChildCoverage);
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// nominal                                                          /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	private final void evaluateNominalEquals(Subgroup theParent, BitSet theParentMembers, ColumnConditionBasesNominalEquals theColumnConditionBases)
	{
		assert (!itsSearchParameters.getNominalSets());
		assert (theColumnConditionBases.get(0).getOperator() == Operator.EQUALS);

		////////////////////////////////////////////////////////////////////////
		boolean isFilterNull = (itsFilter == null);
		int aParentCoverage = theParent.getCoverage();
		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theColumnConditionBases.get(0);
		Column aColumn = aConditionBase.getColumn();
		ConditionList aParentConditions = (isFilterNull ? null : theParent.getConditions());
		////////////////////////////////////////////////////////////////////////

		int[] aCounts = aColumn.getUniqueNominalDomainCounts(theParentMembers, aParentCoverage);

		// avoid entering loop and checking 0-count values, no useful Refinement
		// is possible, as it would have the same coverage as anOldCoverage
		int aNrDistinct = aCounts[aCounts.length-1];
		if (aNrDistinct <= 1)
			return;

		List<String> aDomain = aColumn.itsDistinctValuesU;
		for (int i = 0, j = aNrDistinct; j > 0 && !isTimeToStop(); ++i)
		{
			int aCount = aCounts[i];
			if (aCount == 0)
					continue;

			j--;

			if (aCount < itsMinimumCoverage)
				continue;

			if (aCount == aParentCoverage)
				break;

			Condition aCondition = new Condition(aConditionBase, aDomain.get(i));

			if (!isFilterNull && !itsFilter.isUseful(aParentConditions, aCondition))
				continue;

			Subgroup aNewSubgroup = theParent.getRefinedSubgroup(aCondition);
			checkAndLog(aNewSubgroup, aParentCoverage);
		}
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// BestValueSet algorithm                                           /////
	///// only works for SINGLE NOMINAL, other types not are implemented   /////
	///// BestValueSet is actually applicable to any convex measure        /////
	///// TODO not sure if this requirement is checked by the code below   /////
	///// FIXME this does not take maximum coverage into account           /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

//	private final void evaluateNominalBestValueSet(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	private final void evaluateNominalElementOf(Subgroup theParent, BitSet theParentMembers, ColumnConditionBasesNominalElementOf theColumnConditionBases)
	{
		// currently BestValueSet implies the target type is SINGLE_NOMINAL
		assert (itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL);
		assert (itsSearchParameters.getNominalSets());
		assert (isDirectSingleBinary());
		assert (theColumnConditionBases.get(0).getOperator() == Operator.ELEMENT_OF);
		// when last added Condition is 'Column_x in ValueSet', ConditionBase
		// for Column_x should be skipped, as no useful Refinement is possible
		// assumes only one Operator for Columns in ValueSet-scenario
		assert (!BEST_VALUESET_AT_LAST_POSITION_SKIP || 
			(ConditionListBuilder.toCanonicalOrder(theParent.getConditions())[theParent.getDepth()-1].getColumn() != theColumnConditionBases.get(0).getColumn()));

		int aParentCoverage = theParent.getCoverage();
		ConditionBase aConditionBase = theColumnConditionBases.get(0);

		// as for BestIntervals -> use new half-interval code, it is 70x faster
		NominalCrossTable aNCT = new NominalCrossTable(aConditionBase.getColumn(), theParentMembers, itsBinaryTarget);
		SortedSet<String> aDomainBestSubSet = new TreeSet<String>();

		// final: if-else is long, ensure value is set before creating Subgroup
		final int aCountHeadBody;
		final int aChildCoverage;
		final double aFinalBestQuality;

		QM aQualityMeasure = itsSearchParameters.getQualityMeasure();
		if (aQualityMeasure == QM.WRACC || aQualityMeasure == QM.CORTANA_QUALITY)
		{
			// NOTE
			// this path did not set aBestQuality, by keeping track of the sum
			// of both aPi and (aPi + aNi) the aBestQuality score can be
			// computed using: itsQualityMeasure.calculate(aPiSum, aPiPlusNiSum)
			//
			// NOTE
			// Michael used imprecise (and slower) floating-point calculation
			// by rewriting, the result can be computed using integer arithmetic
			long T = itsQualityMeasure.getNrPositives();
			long N = itsQualityMeasure.getNrRecords();
			int aP = 0;
			int aN = 0;

//			double aRatio = itsQualityMeasure.getNrPositives() / (double)(itsQualityMeasure.getNrRecords());
			for (int i = 0; i < aNCT.size() && !isTimeToStop(); i++)
			{
				int aPi = aNCT.getPositiveCount(i);
				int aNi = aNCT.getNegativeCount(i);
				// include values with WRAcc=0 too, result has same WRAcc but higher support
				// NOTE
				// with respect to the remark above, see the end of this method
				// including such values might result in a subgroup size that is
				// > maximum_coverage
//				if (aPi >= aRatio * (aPi + aNi))
//					aDomainBestSubSet.add(aNCT.getValue(i));
				int aCount = (aPi + aNi);
				if ((aPi * N) >= (T * aCount))
				{
					aP += aPi;
					aN += aNi;
					aDomainBestSubSet.add(aNCT.getValue(i));
				}

				// NOTE could happen because of floating-point rounding errors
				assert ((aPi >= ((T / ((double) N)) * aCount)) == ((aPi * N) >= (T * aCount)));
			}

			if (aDomainBestSubSet.size() == 0)
				return;

			// NOTE
			// when (aDomainBestSubSet.size() == oldValueSet.size()) the
			// subgroup size or ratios might have changed, yielding a different
			// aFinalBestQuality
			//
			// when (aDomainBestSubSet.size() < oldValueSet.size()) the new
			// ValueSet might still be a redundant addition, when an
			// 'intermediate' Condition has reduced the domain of oldValueSet,
			// the size-check below essentially includes a test for that also
			if ((aP + aN) == aParentCoverage) // (aP + aN) is aChildCoverage
			{
				itsSkipCount.incrementAndGet();
//				if (DEBUG_PRINTS_FOR_SKIP)
//					Log.logCommandLine(String.format("SZ-SKIP\t%s AND %s%n", theParent, aDomainBestSubSet));
				if (BEST_VALUESET_NO_REFINEMENT_SKIP)
					return;
			}

			aCountHeadBody    = aP;
			aChildCoverage    = (aP + aN);
			aFinalBestQuality = itsQualityMeasure.calculate(aCountHeadBody, aChildCoverage);
		}
		else // not WRACC
		{
			double aBestQuality = Double.NEGATIVE_INFINITY;
			int aBestP = Integer.MIN_VALUE;
			int aBestN = Integer.MIN_VALUE;

			// construct and check all subsets on the convex hull
			List<Integer> aSortedDomainIndices = aNCT.getSortedDomainIndices();
			int aSortedDomainIndicesSize = aSortedDomainIndices.size();

			// upper part of the hull
			int aP = 0;
			int aN = 0;
			int aPrevBestI = -1;
			for (int i = 0; i < aSortedDomainIndicesSize - 1 && !isTimeToStop(); i++)
			{
				int anIndex = aSortedDomainIndices.get(i);
				int aPi = aNCT.getPositiveCount(anIndex);
				int aNi = aNCT.getNegativeCount(anIndex);
				aP += aPi;
				aN += aNi;
				int aNextIndex = aSortedDomainIndices.get(i+1);
				// FIXME multiplications might overflow, though == remains valid
				if (i < aSortedDomainIndicesSize-2 && aPi * aNCT.getNegativeCount(aNextIndex) == aNCT.getPositiveCount(aNextIndex) * aNi) // skip checking degenerate hull points
					continue;
				double aQuality = itsQualityMeasure.calculate(aP, aP + aN);
				if (aQuality > aBestQuality)
				{
					aBestQuality = aQuality;
					aBestP = aP;
					aBestN = aN;

					for (int j = aPrevBestI+1; j <= i; j++)
					{
						String aValue = aNCT.getValue(aSortedDomainIndices.get(j));
						aDomainBestSubSet.add(aValue);
					}
					aPrevBestI = i;
				}
			}

			// lower part of the hull
			// TODO: complete list of QMs
			boolean aLowIsDominatedQM = false;
			boolean anAsymmetricQM = true;
			if (aQualityMeasure == QM.BINOMIAL)
				aLowIsDominatedQM = true;
			if (aQualityMeasure == QM.CHI_SQUARED || aQualityMeasure == QM.INFORMATION_GAIN)
				anAsymmetricQM = false;

			// NOTE if ever enabled: set aBestQuality, aCountHeadBody, aCoverage
			if (false && !anAsymmetricQM) // TODO: fix this for depth > 1, check only upper OR lower hull
			{
				if (aDomainBestSubSet.size() > aNCT.size()/2) // prefer complement if smaller
				{
					aDomainBestSubSet.clear();
					for (int j = aPrevBestI + 1; j < aSortedDomainIndicesSize; j++)
					{
						String aValue = aNCT.getValue(aSortedDomainIndices.get(j));
						aDomainBestSubSet.add(aValue);
					}
				}
			}
			else if (true || !aLowIsDominatedQM)
			{
				aP = 0;
				aN = 0;
				aPrevBestI = -1;
				for (int i = aSortedDomainIndicesSize - 1; i > 0 && !isTimeToStop(); i--)
				{
					int anIndex = aSortedDomainIndices.get(i).intValue();
					int aPi = aNCT.getPositiveCount(anIndex);
					int aNi = aNCT.getNegativeCount(anIndex);
					aP += aPi;
					aN += aNi;
					int aPrevIndex = aSortedDomainIndices.get(i-1);
					if (i > 1 && aPi * aNCT.getNegativeCount(aPrevIndex) == aNCT.getPositiveCount(aPrevIndex) * aNi)
						continue; // skip degenerate hull points
					double aQuality = itsQualityMeasure.calculate(aP, aP + aN);
					if (aQuality > aBestQuality)
					{
						aBestQuality = aQuality;
						aBestP = aP;
						aBestN = aN;

						if (aPrevBestI == -1)
						{
							aDomainBestSubSet.clear();
							aPrevBestI = aSortedDomainIndicesSize;
						}
						for (int j = aPrevBestI-1; j >= i; j--)
						{
							String aValue = aNCT.getValue(aSortedDomainIndices.get(j));
							aDomainBestSubSet.add(aValue);
						}
						aPrevBestI = i;
					}
				}
			}

			if (aDomainBestSubSet.size() == 0)
				return;

			// NOTE
			// when (aDomainBestSubSet.size() == oldValueSet.size()) the
			// subgroup size or ratios might have changed, yielding a different
			// aFinalBestQuality
			//
			// when (aDomainBestSubSet.size() < oldValueSet.size()) the new
			// ValueSet might still be a redundant addition, when an
			// 'intermediate' Condition has reduced the domain of oldValueSet,
			// the size-check below essentially includes a test for that also
			if ((aP + aN) == aParentCoverage) // (aP + aN) is aChildCoverage
			{
				itsSkipCount.incrementAndGet();
//				if (DEBUG_PRINTS_FOR_SKIP)
//					Log.logCommandLine(String.format("SZ-SKIP\t%s AND %s%n", theParent, aDomainBestSubSet));
				if (BEST_VALUESET_NO_REFINEMENT_SKIP)
					return;
			}

			aCountHeadBody    = aBestP;
			aChildCoverage    = (aBestP + aBestN);
			aFinalBestQuality = aBestQuality;
		}

		// FIXME MM this does not take maximum coverage into account
		// the best ValueSet might be > maxCov, and thus not be used
		// but when this is the last search level, the refined version
		// will also never be created
		// a better solution is to obtain the best VALID ValueSet for
		// this level, add it to the result set (possibly), and add the
		// BEST (valid or not) Candidate to the CandidateQueue
		// and then never refine a nominal attribute with itself
		//
		// this is especially relevant because for WRAcc the original
		// includes values with a score of 0, this increases the
		// support
		// in general this is a good approach, as larger Subgroups have
		// more possibilities as refinement seeds
		// but obviously, when having to choose between not returning a
		// ValueSet, and one that omits some 0-score labels the latter
		// seems the logical choice
		//
		// so before returning the ValueSet, as simple solution for now
		// is to reduce the ValueSet if it is too large, by removing
		// 0-score and labels, and, if needed, those with the worst
		// ratios, until the ValueSet < maximimum_coverage
		// then this method calls checkAndLog() twice, once with the
		// BEST VALID ValueSet (which can be added to the ResultSet)
		// and the BEST (even if too large) ValueSet, which will never
		// be added to the ResultSet, but could be added to the
		// CandidateSet
		ValueSet aBestSubset = new ValueSet(aDomainBestSubSet);
		Condition anAddedCondition = new Condition(aConditionBase, aBestSubset);
		Subgroup aChild = directComputation(theParent, anAddedCondition, aFinalBestQuality, aChildCoverage, aCountHeadBody);
		checkAndLog(aChild, aParentCoverage);
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// numeric                                                          /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	/*
	 * NOTE
	 * all required data can come from a single Refinement parameter, but
	 * theParentMembers are obtained through an expensive Subgroup.getMembers()
	 * call, which creates a new copy every time, therefore it is cached
	 *
	 * NOTE
	 * these asserts are omitted as only the two mine() methods call this one:
	 * (theParentMembers.cardinality() == theParentCoverage)
	 * (theRefinement.getSubgroup().getMembers().equals(theParentMembers))
	 *
	 * TODO
	 * for *_BEST strategies, in case of ties, multiple subgroups
	 * attaining the best score, implementations retain only the first
	 * instead it could retain all best scoring subgroups
	 *
	 * moreover, the treatment of <= and >= is dubious
	 * for <= the first, and therefore smallest best Subgroup is retained
	 * for >= the first, and therefore largest best Subgroup is retained
	 */
	private final void evaluateNumericRegular(Subgroup theParent, BitSet theParentMembers, ColumnConditionBasesNumericRegular theColumnConditionBases)
	{
		assert (EnumSet.of(NumericStrategy.NUMERIC_ALL, NumericStrategy.NUMERIC_BEST,
							NumericStrategy.NUMERIC_BEST_BINS, NumericStrategy.NUMERIC_BINS).contains(itsSearchParameters.getNumericStrategy()));

		ConditionBase e = theColumnConditionBases.get(0);
		ConditionBase l = theColumnConditionBases.get(1);
		ConditionBase g = theColumnConditionBases.get(2);

		// SearchSettings should not change, but checking all the time is cheap
		boolean doEq_Test = (e != null);
		boolean doLeqTest = (l != null);
		boolean doGeqTest = (g != null);

		assert (!doEq_Test || (e.getOperator() == (itsSearchParameters.getNumericStrategy().isDiscretiser() ? Operator.BETWEEN : Operator.EQUALS)));
		assert (!doLeqTest || (l.getOperator() == Operator.LESS_THAN_OR_EQUAL));
		assert (!doGeqTest || (g.getOperator() == Operator.GREATER_THAN_OR_EQUAL));

		final Column aColumn = (doEq_Test ? e.getColumn() : (doLeqTest ? l.getColumn() : g.getColumn()));

		// currently only for SINGLE_NOMINAL (and not for propensity scores)
		// but expect there to be more optimised settings, so keep split here
		if (isDirectSingleBinary())
		{
			ValueCountTP v = aColumn.getUniqueNumericDomainMap(theParentMembers);
			if (doEq_Test) evaluateNumericRegularSingleBinary(theParent, e, v);
			if (doLeqTest) evaluateNumericRegularSingleBinary(theParent, l, v);
			if (doGeqTest) evaluateNumericRegularSingleBinary(theParent, g, v);
		}
		else
		{
			ValueCount v = aColumn.getValueCount(theParentMembers);
			if (doEq_Test) evaluateNumericRegularGeneric(theParent, e, v);
			if (doLeqTest) evaluateNumericRegularGeneric(theParent, l, v);
			if (doGeqTest) evaluateNumericRegularGeneric(theParent, g, v);
		}
	}

	// generic version, use plain ValueCount, no model-specific info
	private final void evaluateNumericRegularGeneric(Subgroup theParent, ConditionBase theConditionBase, ValueCount theValueInfo)
	{
		NumericStrategy ns = itsSearchParameters.getNumericStrategy();

		// split code path - BEST_BINS/BINS use substantially different loop
		if (ns.isDiscretiser())
		{
			evaluateNumericRegularGenericCoarse(theParent, theConditionBase, theValueInfo);
			return;
		}

		////////////////////////////////////////////////////////////////////////
		int aParentCoverage  = theParent.getCoverage();
		Column aColumn        = theConditionBase.getColumn();
		Operator anOperator   = theConditionBase.getOperator();
		// might require update when more strategies are added
		boolean isAllStrategy = (ns == NumericStrategy.NUMERIC_ALL);
		BestSubgroupsForCandidateSetAndResultSet aBestSubgroups = (isAllStrategy ? null : new BestSubgroupsForCandidateSetAndResultSet());
		////////////////////////////////////////////////////////////////////////

		int[] aCounts = theValueInfo.itsCounts;

		// a lot of code, but keep it together for now, loops differ in subtle
		// ways, keeping them together for now aids interpretation
		if (anOperator == Operator.EQUALS)
		{
			for (int i = 0, j = aCounts.length; i < j && !isTimeToStop(); ++i)
			{
				int aCount =  aCounts[i];

				// no need to evaluate, it includes the (aCounts[i] == 0) check,
				//  assuming itsMinimumCoverage >= 1
				if (aCount < itsMinimumCoverage)
					continue;

				// no useful refinement possible, counts are 0 for other values
				if (aCount == aParentCoverage)
					break;

				Condition anAddedCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
				evaluateCandidate(theParent, anAddedCondition, aCount, isAllStrategy, aBestSubgroups);
			}
		}
		else if (anOperator == Operator.LESS_THAN_OR_EQUAL)
		{
			for (int i = 0, j = aCounts.length, cover = 0; i < j && !isTimeToStop(); ++i)
			{
				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				cover += aCount;

				if (cover == aParentCoverage)
					break;

				if (cover < itsMinimumCoverage)
					continue;

				Condition anAddedCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
				evaluateCandidate(theParent, anAddedCondition, cover, isAllStrategy, aBestSubgroups);
			}
		}
		else if (anOperator == Operator.GREATER_THAN_OR_EQUAL)
		{
			for (int i = 0, j = aCounts.length, cover = aParentCoverage; i < j && !isTimeToStop(); ++i)
			{
				if (cover < itsMinimumCoverage)
					break;

				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				// >= with the first value select the same subset as the parent
				// old tp and cover, as counts for this value should be included
				if (cover != aParentCoverage)
				{
					Condition anAddedCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
					evaluateCandidate(theParent, anAddedCondition, cover, isAllStrategy, aBestSubgroups);
				}

				// before moving to next, subtract counts related to this value
				cover -= aCount;
			}
		}
		else
			throw new AssertionError("SubgroupDiscovery.evaluateNumericRegularGeneric() + " + anOperator);

		if (!isAllStrategy)
		{
			checkAndLogBest(aBestSubgroups, aParentCoverage);
			// FIXME temporary checks
			debugBest(theParent, null, aBestSubgroups);
		}
	}

	// generic version, use plain ValueCount, no model-specific info
	private final void evaluateNumericRegularGenericCoarse(Subgroup theParent, ConditionBase theConditionBase, ValueCount theValueInfo)
	{
		NumericStrategy ns = itsSearchParameters.getNumericStrategy();
		assert (ns == NumericStrategy.NUMERIC_BEST_BINS || ns == NumericStrategy.NUMERIC_BINS);

		Operator anOperator = theConditionBase.getOperator();

		long aNrBins = itsSearchParameters.getNrBins();

		// not checked in MiningWindow/XML, do nothing for now (no error)
		if (aNrBins <= 1L)
			return;

		// long to prevent overflow for multiplication
		long aParentCoverage  = theParent.getCoverage();
		long b                = 1L;
		Column aColumn        = theConditionBase.getColumn();
		int[] aCounts         = theValueInfo.itsCounts;
		boolean isAllStrategy = (ns == NumericStrategy.NUMERIC_BINS);
		BestSubgroupsForCandidateSetAndResultSet aBestSubgroups = (isAllStrategy ? null : new BestSubgroupsForCandidateSetAndResultSet());

		// ColumnConditionBasesBuilder replaces Operator.EQUALS for (BEST_)BINS
		if (anOperator == Operator.BETWEEN)
		{
			// last cover used for evaluation, and last lower bound
			int last_cover = 0;
			float f = Float.NEGATIVE_INFINITY;
			for (int i = 0, next = next(aParentCoverage, b, aNrBins), cover = 0; i < aCounts.length && b < aNrBins && !isTimeToStop(); ++i)
			{
				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				cover += aCount;

				if (cover <= next)
					continue;

				if (cover == aParentCoverage)
					break;

				float n = aColumn.getSortedValue(i);
				Condition anAddedCondition = new Condition(theConditionBase, new Interval(f, n));
				evaluateCandidate(theParent, anAddedCondition, (cover-last_cover), isAllStrategy, aBestSubgroups);

				last_cover = cover;
				f = n;

				while ((next = next(aParentCoverage, ++b, aNrBins)) <= cover-1)
					; // deliberately empty
			}
			// POSITIVE_INFINITY should never be present, as this type of value
			// should not be in the data, but this is copied from the original
			// code, moreover Column.add() does not guard against +/- infinity,
			// -0.0 and NaN
			//
			// if there is already a Condition for POSITIVE_INFINITY, do not add
			// another one, it should be the last value anyway, and have a
			// correct count, if it exists (though Arrays.sort() would put NaNs
			// after it)
			//
			// if it is not in present(), two situations could occur
			// 1. the sum of the value.counts is equal to aParentCoverage (for a
			// half-interval: <= f would select all data, and be useless), or
			// 2. the sum is lower: add Interval that selects the remaining data
			// TODO both conditions should never be true -> could be assert
			if ((last_cover != aParentCoverage) && (Float.compare(Float.POSITIVE_INFINITY, f) != 0))
			{
				Condition anAddedCondition = new Condition(theConditionBase, new Interval(f, Float.POSITIVE_INFINITY));
				last_cover = (((int) aParentCoverage) - last_cover);
				evaluateCandidate(theParent, anAddedCondition, last_cover, isAllStrategy, aBestSubgroups);
			}
		}
		else if (anOperator == Operator.LESS_THAN_OR_EQUAL)
		{
			for (int i = 0, next = next(aParentCoverage, b, aNrBins), cover = 0; b < aNrBins && !isTimeToStop(); ++i)
			{
				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				cover += aCount;

				if (cover <= next)
					continue;

				if (cover == aParentCoverage)
					break;

				Condition aCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
				evaluateCandidate(theParent, aCondition, cover, isAllStrategy, aBestSubgroups);

				while ((next = next(aParentCoverage, ++b, aNrBins)) <= cover-1)
					; // deliberately empty
			}
		}
		else if (anOperator == Operator.GREATER_THAN_OR_EQUAL)
		{
			// NOTE getTertiaryStatistic() only works for SINGLE_NOMINAL
			// NOTE division in old code rounds down for <= and >=
			for (int i = 0, next = (int) (aParentCoverage - (aParentCoverage / aNrBins)), cover = (int) aParentCoverage; b < aNrBins && !isTimeToStop(); ++i)
			{
				if (cover < itsMinimumCoverage)
					break;

				int aCount = aCounts[i];
				if (aCount == 0)
					continue;


				// last value with required cover, use old cover and tp
				if (((cover-aCount) < next) && (cover != aParentCoverage))
				{
					Condition aCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
					evaluateCandidate(theParent, aCondition, cover, isAllStrategy, aBestSubgroups);

					while ((next = (int) (aParentCoverage - ((++b * aParentCoverage) / aNrBins))) > (cover-aCount))
						; // deliberately empty
				}

				// before moving to next, subtract counts related to this value
				cover -= aCount;
			}
		}
		else
			throw new AssertionError("SubgroupDiscovery.evaluateNumericRegularGenericCoarse() + " + anOperator);

		if (!isAllStrategy)
		{
			checkAndLogBest(aBestSubgroups, (int) aParentCoverage);
			// FIXME temporary checks
			debugBest(theParent, null, aBestSubgroups);
		}
	}
	public static final int next(long n, long b, long B) // public: MiningWidnow
	{
		long nb = (n * b);                 // original n, b are int: no overflow
		int i = (int) (nb / B);
		return ((nb % B) == 0L) ? i-1 : i; // get same behaviour as data.reverse
	}

	// this version includes true positive counts, and direct computation
	private final void evaluateNumericRegularSingleBinary(Subgroup theParent, ConditionBase theConditionBase, ValueCountTP theValueInfo)
	{
		NumericStrategy ns = itsSearchParameters.getNumericStrategy();

		// split code path - BEST_BINS/BINS use substantially different loop
		if (ns.isDiscretiser())
		{
			evaluateNumericRegularSingleBinaryCoarse(theParent, theConditionBase, theValueInfo);
			return;
		}

		////////////////////////////////////////////////////////////////////////
		int aParentCoverage   = theParent.getCoverage();
		Column aColumn        = theConditionBase.getColumn();
		Operator anOperator   = theConditionBase.getOperator();
		// might require update when more strategies are added
		boolean isAllStrategy = (ns == NumericStrategy.NUMERIC_ALL);
		BestSubgroupsForCandidateSetAndResultSet aBestSubgroups = (isAllStrategy ? null : new BestSubgroupsForCandidateSetAndResultSet());
		////////////////////////////////////////////////////////////////////////

		int[] aCounts = theValueInfo.itsCounts;
		int[] aTPs = theValueInfo.itsTruePositives;

		// a lot of code, but keep it together for now, loops differ in subtle
		// ways, keeping them together for now aids interpretation
		if (anOperator == Operator.EQUALS)
		{
			int aSize = aCounts.length;
			for (int i = 0; i < aSize && !isTimeToStop(); i++)
			{
				int aCount =  aCounts[i];

				// no need to evaluate, it includes the (aCounts[i] == 0) check,
				//  assuming itsMinimumCoverage >= 1
				if (aCount < itsMinimumCoverage)
					continue;

				if (aCount == aParentCoverage)
					break;

				if (!Float.isNaN(aColumn.getSortedValue(i))) //not a missing value
				{
					Condition anAddedCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
					evaluateCandidate(theParent, anAddedCondition, aCount, aTPs[i], isAllStrategy, aBestSubgroups);
				}
			}
		}
		else if (anOperator == Operator.LESS_THAN_OR_EQUAL)
		{
			int aSize = aCounts.length;
			int aCover = 0;
			int aTruePositiveCount = 0;
			for (int i=0; i<aSize && !isTimeToStop(); i++)
			{
				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				aCover += aCount;
				aTruePositiveCount += aTPs[i];

				if (aCover == aParentCoverage)
					break;

				if (aCover < itsMinimumCoverage || aTruePositiveCount < itsMinimumSupport)
					continue;

				if (!Float.isNaN(aColumn.getSortedValue(i))) //not a missing value
				{
					Condition aCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
					evaluateCandidate(theParent, aCondition, aCover, aTruePositiveCount, isAllStrategy, aBestSubgroups);
				}
			}
		}
		else if (anOperator == Operator.GREATER_THAN_OR_EQUAL)
		{
			int aSize = aCounts.length;
			int aCover = aParentCoverage - theValueInfo.itsMissingCount;
			int aTruePositiveCount = (int) theParent.getTertiaryStatistic() - theValueInfo.itsMissingPositiveCount; // NOTE getTertiaryStatistic() only works for SINGLE_NOMINAL
			for (int i=0; i<aSize && !isTimeToStop(); i++)
			{
				if (aCover < itsMinimumCoverage || aTruePositiveCount < itsMinimumSupport)
					break;

				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				// >= with the first value select the same subset as the parent
				if (aCover != aParentCoverage && !Float.isNaN(aColumn.getSortedValue(i))) //and check whether this is not the missing value NaN
				{
					Condition aCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
					evaluateCandidate(theParent, aCondition, aCover, aTruePositiveCount, isAllStrategy, aBestSubgroups);
				}

				// before moving to next, subtract counts related to this value
				aCover -= aCount;
				aTruePositiveCount -= aTPs[i];
			}
		}
		else
			throw new AssertionError("SubgroupDiscovery.evaluateNumericRegularSingleBinary() + " + anOperator);

		if (!isAllStrategy)
		{
			checkAndLogBest(aBestSubgroups, aParentCoverage);
			// FIXME temporary checks
			debugBest(theParent, null, aBestSubgroups);
		}
	}

	private final void evaluateNumericRegularSingleBinaryCoarse(Subgroup theParent, ConditionBase theConditionBase, ValueCountTP theValueInfo)
	{
		NumericStrategy ns = itsSearchParameters.getNumericStrategy();
		assert (ns == NumericStrategy.NUMERIC_BEST_BINS || ns == NumericStrategy.NUMERIC_BINS);

		Operator anOperator = theConditionBase.getOperator();

		long aNrBins = itsSearchParameters.getNrBins();

		// not checked in MiningWindow/XML, do nothing for now (no error)
		if (aNrBins <= 1L)
			return;

		// long to prevent overflow for multiplication
		long aParentCoverage  = theParent.getCoverage();
		long b                = 1L;
		Column aColumn        = theConditionBase.getColumn();
		int[] aCounts         = theValueInfo.itsCounts;
		int[] aTPs            = theValueInfo.itsTruePositives;
		boolean isAllStrategy = (ns == NumericStrategy.NUMERIC_BINS);
		BestSubgroupsForCandidateSetAndResultSet aBestSubgroups = (isAllStrategy ? null : new BestSubgroupsForCandidateSetAndResultSet());

		// ColumnConditionBasesBuilder replaces Operator.EQUALS for (BEST_)BINS
		if (anOperator == Operator.BETWEEN)
		{
			// last cover and tp used for evaluation, and last lower bound
			int last_cover = 0;
			int last_tp = 0;
			float f = Float.NEGATIVE_INFINITY;
			for (int i = 0, next = next(aParentCoverage, b, aNrBins), cover = 0, tp = 0; i < aCounts.length && b < aNrBins && !isTimeToStop(); ++i)
			{
				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				cover += aCount;
				tp += aTPs[i];

				if ((cover <= next) || (cover < itsMinimumCoverage))
					continue;

				if (cover == aParentCoverage)
					break;

				float n = aColumn.getSortedValue(i);
				Condition anAddedCondition = new Condition(theConditionBase, new Interval(f, n));
				evaluateCandidate(theParent, anAddedCondition, (cover-last_cover), (tp-last_tp), isAllStrategy, aBestSubgroups);

				last_cover = cover;
				last_tp = tp;
				f = n;

				while ((next = next(aParentCoverage, ++b, aNrBins)) <= cover-1)
					; // deliberately empty
			}
			// POSITIVE_INFINITY should never be present, as this type of value
			// should not be in the data, but this is copied from the original
			// code, moreover Column.add() does not guard against +/- infinity,
			// -0.0 and NaN
			//
			// if there is already a Condition for POSITIVE_INFINITY, do not add
			// another one, it should be the last value anyway, and have a
			// correct count, if it exists (though Arrays.sort() would put NaNs
			// after it)
			//
			// if it is not in present(), two situations could occur
			// 1. the sum of the value.counts is equal to aParentCoverage (for a
			// half-interval: <= f would select all data, and be useless), or
			// 2. the sum is lower: add Interval that selects the remaining data
			// TODO both conditions should never be true -> could be assert
			if ((last_cover != aParentCoverage) && (Float.compare(Float.POSITIVE_INFINITY, f) != 0))
			{
				Condition anAddedCondition = new Condition(theConditionBase, new Interval(f, Float.POSITIVE_INFINITY));
				last_cover = (((int) aParentCoverage) - last_cover);
				last_tp = (((int) theParent.getTertiaryStatistic()) - last_tp);
				evaluateCandidate(theParent, anAddedCondition, last_cover, last_tp, isAllStrategy, aBestSubgroups);
			}
		}
		else if (anOperator == Operator.LESS_THAN_OR_EQUAL)
		{
			if (aCounts.length <= aNrBins) //fewer unique values than bins?
			{
				int cover = 0;
				int tp = 0;
				for (int i=0; i<aCounts.length && !isTimeToStop(); ++i) //use all available cut points
				{
					int aCount = aCounts[i];
					if (aCount == 0)
						continue;

					cover += aCount;
					tp += aTPs[i];

					if (cover < itsMinimumCoverage)
						continue;

					if (cover == aParentCoverage)
						break;

					Condition aCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
					evaluateCandidate(theParent, aCondition, cover, tp, isAllStrategy, aBestSubgroups);
				}
			}
			else
			{
				int next = next(aParentCoverage, b, aNrBins);
				int cover = 0;
				int tp = 0;
				for (int i=0; b < aNrBins && !isTimeToStop(); i++)
				{
					int aCount = aCounts[i];
					if (aCount == 0)
						continue;

					cover += aCount;
					tp += aTPs[i];

					if (cover < itsMinimumCoverage || (cover <= next && aNrBins-b < aCounts.length-i-1))
						continue;

					if (cover == aParentCoverage)
						break;

					Condition aCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
					evaluateCandidate(theParent, aCondition, cover, tp, isAllStrategy, aBestSubgroups);

					while ((next = next(aParentCoverage, ++b, aNrBins)) <= cover-1)
						; // deliberately empty
				}
			}
		}
		else if (anOperator == Operator.GREATER_THAN_OR_EQUAL)
		{
			if (aCounts.length <= aNrBins) //fewer unique values than bins?
			{
				int cover = (int) aParentCoverage;
				int tp = (int) theParent.getTertiaryStatistic();
				for (int i=0; i<aCounts.length && !isTimeToStop(); i++) //use all available cut points
				{
					if (cover < itsMinimumCoverage)
						break;

					int aCount = aCounts[i];
					if (aCount == 0)
						continue;

					if (cover != aParentCoverage)
					{
						Condition aCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
						evaluateCandidate(theParent, aCondition, cover, tp, isAllStrategy, aBestSubgroups);
					}

					// before moving to next, subtract counts related to this value
					cover -= aCount;
					tp -= aTPs[i];
				}
			}
			else
			{
				int next = (int) (aParentCoverage - (aParentCoverage / aNrBins));
				int cover = (int) aParentCoverage;
				int tp = (int) theParent.getTertiaryStatistic();
				for (int i=0; b<aNrBins && !isTimeToStop(); i++)
				{
					if (cover < itsMinimumCoverage)
						break;

					int aCount = aCounts[i];
					if (aCount == 0)
						continue;

					// last value with required cover, use old cover and tp
					if (cover != aParentCoverage && (cover-aCount < next || aNrBins-b < aCounts.length-i-1))
					{
						Condition aCondition = new Condition(theConditionBase, aColumn.getSortedValue(i), i);
						evaluateCandidate(theParent, aCondition, cover, tp, isAllStrategy, aBestSubgroups);

						while ((next = (int) (aParentCoverage - ((++b * aParentCoverage) / aNrBins))) > (cover-aCount))
							; // deliberately empty
					}

					// before moving to next, subtract counts related to this value
					cover -= aCount;
					tp -= aTPs[i];
				}
			}
		}
		else
			throw new AssertionError("SubgroupDiscovery.evaluateNumericRegularSingleBinaryCoarse() + " + anOperator);

		if (!isAllStrategy)
		{
			checkAndLogBest(aBestSubgroups, (int) aParentCoverage);
			// FIXME temporary checks
			debugBest(theParent, null, aBestSubgroups);
		}
	}

	// this is the version used by evaluateNumericRegularGeneric(Coarse)
	// FIXME temporarily a separate method, will merge both evaluateCandidates()
	private final void evaluateCandidate(Subgroup theParent, Condition theAddedCondition, int theChildCoverage, boolean isAllStrategy, 
					     BestSubgroupsForCandidateSetAndResultSet theBestSubgroups)
	{
		if ((itsFilter != null) && !itsFilter.isUseful(theParent.getConditions(), theAddedCondition))
			return;

		int aParentCoverage = theParent.getCoverage();
		assert (theChildCoverage < aParentCoverage);

		Subgroup aChild = theParent.getRefinedSubgroup(theAddedCondition);

		// ALL or BINS
		if (isAllStrategy)
		{
			//addToBuffer(aChild);
			checkAndLog(aChild, aParentCoverage);
		}
		// BEST or BESTBINS
		else
			checkForBest(aChild, aParentCoverage, theBestSubgroups, false);
	}

	// this is the version used by evaluateNumericRegularSingleBinary(Coarse)
	// FIXME temporarily a separate method, will merge both evaluateCandidates()
	private final void evaluateCandidate(Subgroup theParent, Condition theAddedCondition, int theChildCoverage, int theNrTruePositives, 
					     boolean isAllStrategy, BestSubgroupsForCandidateSetAndResultSet theBestSubgroups)
	{
		// currently only for SINGLE_NOMINAL (and not for propensity scores)
		assert (isDirectSingleBinary());

		if ((itsFilter != null) && !itsFilter.isUseful(theParent.getConditions(), theAddedCondition))
			return;

		int aParentCoverage = theParent.getCoverage();
		assert (theChildCoverage < aParentCoverage);

		Subgroup aChild = directComputation(theParent, theAddedCondition, itsQualityMeasure, theChildCoverage, theNrTruePositives);

		// ALL or BINS
		if (isAllStrategy)
		{
			//addToBuffer(aChild);
			checkAndLog(aChild, aParentCoverage);
		}
		// BEST or BESTBINS
		else
			checkForBest(aChild, aParentCoverage, theBestSubgroups, true);
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// numeric intervals - BestInterval algorithm                       /////
	///// only works for SINGLE NOMINAL, other types not are implemented   /////
	///// BestInterval is actually applicable to any convex measure        /////
	///// TODO not sure if convex requirement is checked by the code below /////
	///// FIXME this does not take maximum coverage into account           /////
	/////       see comment at evaluateNominalBestValueSet() on this issue /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

//	private void numericIntervals(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	private final void evaluateNumericBestInterval(Subgroup theParent, BitSet theParentMembers, ColumnConditionBasesNumericIntervals theColumnConditionBases)
	{
		// currently BestIntervals implies the target type is SINGLE_NOMINAL
		assert (itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL);
		assert (isDirectSingleBinary());
		assert (theColumnConditionBases.get(0).getOperator() == Operator.BETWEEN);
		assert (itsSearchParameters.getNumericOperatorSetting() == NumericOperatorSetting.INTERVALS);
		// when last added Condition is 'Column_x in Interval', ConditionBase
		// for Column_x should be skipped, as no useful Refinement is possible
		// assumes only one Operator for Columns in BestInterval-scenario
		assert (!BEST_INTERVAL_AT_LAST_POSITION_SKIP || (ConditionListBuilder.toCanonicalOrder(theParent.getConditions())[theParent.getDepth()-1].getColumn() != (theColumnConditionBases.get(0).getColumn())));

		////////////////////////////////////////////////////////////////////////
		int aParentCoverage = theParent.getCoverage();
		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theColumnConditionBases.get(0);
		Column aColumn = aConditionBase.getColumn();
		////////////////////////////////////////////////////////////////////////

		// copy, as RealBaseIntervalCrossTable will modify the supplied array
		float [] aDomain = aColumn.getSortedValuesCopy();
		ValueCountTP via = aColumn.getUniqueNumericDomainMap(theParentMembers);

		RealBaseIntervalCrossTable aRBICT = new RealBaseIntervalCrossTable(aParentCoverage, (int) theParent.getTertiaryStatistic(), aDomain, via);

		// prune splitpoints for which adjacent base intervals have equal class distribution
		// TODO: check whether this preprocessing reduces *total* computation time
		aRBICT.aggregateIntervals();
		if (aRBICT.getNrSplitPoints() == 0)
			return; // no specialization improves quality

		double aBestQuality = Double.NEGATIVE_INFINITY;
		int aBestNrFalsePositives = Integer.MIN_VALUE;
		int aBestNrTruePositives = Integer.MIN_VALUE;
		Interval aBestInterval = new Interval(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

		// NOTE Michael disabled this setting, if ever enabled: set *NrPositives
		if (false && itsSearchParameters.getQualityMeasure() == QM.WRACC)
		{
			long aPg = (long)itsQualityMeasure.getNrPositives();
			long aNg = (long)itsQualityMeasure.getNrRecords() - aPg;
			int aPr = aRBICT.getPositiveCount();
			int aNr = aRBICT.getNegativeCount();
			int aPl = aPr;
			int aNl = aNr;

			long aMaxH = aPr * aNg - aNr * aPg;
			float aBestLHS = Float.NEGATIVE_INFINITY;

			for (int i = 0; i < aRBICT.getNrBaseIntervals() && !isTimeToStop(); i++)
			{
				long anH = aPr * aNg - aNr * aPg;
				if (anH > aMaxH)
				{
					aMaxH = anH;
					aBestLHS = aRBICT.getSplitPoint(i - 1);
					aPl = aPr;
					aNl = aNr;
				}
				aPr -= aRBICT.getPositiveCount(i);
				aNr -= aRBICT.getNegativeCount(i);
				double aQuality = itsQualityMeasure.calculate(aPl - aPr, aPl + aNl - aPr - aNr);
				if (aQuality > aBestQuality) {
					aBestQuality = aQuality;
					if (i == aRBICT.getNrSplitPoints())
						aBestInterval = new Interval(aBestLHS, Float.POSITIVE_INFINITY);
					else
						aBestInterval = new Interval(aBestLHS, aRBICT.getSplitPoint(i));
				}
			}
		}
		else // not WRAcc
		{
			/*
			////////////////////////////////////////////////////////////////////
			// MM - slightly adapted brute force method
			//      does not use direct computation, keeping track of the number
			//      of positives and subgroup size would allow to do so, but
			//      current implementation is a lazy hack for bug hunting only
			Subgroup aBestSubgroup = null;
			float[] aSplitPoints = aRBICT.getSplitPoints();
			for (int i=0; i<aSplitPoints.length; i++)
			{
				Interval aNewInterval = new Interval(aSplitPoints[i], Float.POSITIVE_INFINITY);
				Subgroup aNewSubgroup = theParent.getRefinedSubgroup(new Condition(aConditionBase, aNewInterval));
				double aQuality = evaluateCandidate(aNewSubgroup);
				if (aQuality > aBestQuality) {
					aBestQuality = aQuality;
					aBestInterval = aNewInterval;
					aBestSubgroup = aNewSubgroup;
				}
				aNewInterval = new Interval(Float.NEGATIVE_INFINITY, aSplitPoints[i]);
				aNewSubgroup = theParent.getRefinedSubgroup(new Condition(aConditionBase, aNewInterval));
				// MM - comparisons to NaN always return false
				//      if (itsUseNegInfty == true) first Interval = (-Inf,-Inf)
				aQuality = (aNewSubgroup.getCoverage() == 0) ? Double.NaN : evaluateCandidate(aNewSubgroup);
				if (aQuality > aBestQuality) {
					aBestQuality = aQuality;
					aBestInterval = aNewInterval;
					aBestSubgroup = aNewSubgroup;
				}
				for (int j=i+1; j<aSplitPoints.length; j++)
				{
					aNewInterval = new Interval(aSplitPoints[i], aSplitPoints[j]);
					aNewSubgroup = theParent.getRefinedSubgroup(new Condition(aConditionBase, aNewInterval));
					aQuality = evaluateCandidate(aNewSubgroup);
					if (aQuality > aBestQuality) {
						aBestQuality = aQuality;
						aBestInterval = aNewInterval;
						aBestSubgroup = aNewSubgroup;
					}
				}
			}
			if (aBestSubgroup == null)
				return;
			// MM - these numbers are required for direct computation below
			//      do not use getTruePositiveRate(): might give rounding errors
			aBestNrTruePositives  = (int) aBestSubgroup.getTertiaryStatistic();
			aBestNrFalsePositives = (aBestSubgroup.getCoverage() - aBestNrTruePositives);

			// super temporary
			itsBestIntervalsCount.incrementAndGet();
			aBestSubgroup.setMeasureValue(aBestQuality);
//			Subgroup sg = aBestSubgroup;
//			Log.logCommandLine(String.format("%nquality=%f\tcoverage=%d\ttrue_positives=%d\t%s", sg.getMeasureValue(), sg.getCoverage(), (int) sg.getTertiaryStatistic(), sg));
			Subgroup aBestBruteForce = evaluateNumericBestIntervalBruteForceMM(theParent, aConditionBase, aRBICT);
			if (aBestSubgroup.compareTo(aBestBruteForce) != 0)
			{
				itsBestIntervalsDiffer.incrementAndGet();

				// abuse this class to put pairs of Subgroups in the Queue
				// all errors are collected and printed at the end of mine()
				BestSubgroupsForCandidateSetAndResultSet b = new BestSubgroupsForCandidateSetAndResultSet();
				b.itsBestForCandidateSet = aBestSubgroup;
				b.itsBestForResultSet    = aBestBruteForce;
				itsBestIntervalsErrors.add(b);
			}
//			// compare to output of linear algorithm
//			itsBestIntervalsCount.incrementAndGet();
//			aBestSubgroup.setMeasureValue(evaluateCandidate(aBestSubgroup));
//			Subgroup aBestLinearSg = evaluateNumericBestIntervalLinear(theParent, aConditionBase, aRBICT);
//			if (aBestSubgroup.compareTo(aBestLinearSg) != 0)
//			{
//				itsBestIntervalsDiffer.incrementAndGet();
//
//				// abuse this class to put pairs of Subgroups in the Queue
//				// all errors are collected and printed at the end of mine()
//				BestSubgroupsForCandidateSetAndResultSet b = new BestSubgroupsForCandidateSetAndResultSet();
//				b.itsBestForCandidateSet = aBestSubgroup;
//				b.itsBestForResultSet    = aBestLinearSg;
//				itsBestIntervalsErrors.add(b);
//			}

			///// END OF MM VERSION ////////////////////////////////////////////
			*/

			// MM - FOR DEBUGGING: COPIED TO evaluateNumericBestIntervalLinear()
//			// the linear algo
//			ConvexHull [] aHulls = new ConvexHull[aRBICT.getNrBaseIntervals()];
//			int aPi = 0;
//			int aNi = 0;
//			for (int l = 0; l < aRBICT.getNrSplitPoints(); l++)
//			{
//				aPi += aRBICT.getPositiveCount(l);
//				aNi += aRBICT.getNegativeCount(l);
//				aHulls[l] = new ConvexHull(aNi, aPi, aRBICT.getSplitPoint(l), Float.NEGATIVE_INFINITY);
//			}
//			aHulls[aRBICT.getNrBaseIntervals()-1] = new ConvexHull(aRBICT.getNegativeCount(), aRBICT.getPositiveCount(), Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
//
//			for (int k = aRBICT.getNrBaseIntervals(); k > 1; k = (k+1)/2)
//			{
//				for (int l = 0; l+1 < k; l += 2)
//				{
//					ConvexHull aMinkDiff = aHulls[l].minkowskiDifference(aHulls[l+1], true);
//					for (int aSide = 0; aSide < 2; aSide++)
//					{
//						for (int i = 0; i < aMinkDiff.getSize(aSide) && !isTimeToStop(); i++)
//						{
//							if (aSide == 1 && (i == 0 || i == aMinkDiff.getSize(aSide)-1) )
//								continue; // no need to check duplicate hull points
//
//							HullPoint aCandidate = aMinkDiff.getPoint(aSide, i);
//							double aQuality = itsQualityMeasure.calculate(aCandidate.itsY, (aCandidate.itsX + aCandidate.itsY));
//
//							if (aQuality > aBestQuality)
//							{
//								aBestQuality = aQuality;
//								aBestNrFalsePositives = aCandidate.itsX;
//								aBestNrTruePositives = aCandidate.itsY;
//								aBestInterval = new Interval(aCandidate.getLabel2(), aCandidate.itsLabel1);
//							}
//						}
//					}
//				}
//
//				for (int l = 0; l+1 < k; l += 2)
//					aHulls[l/2] = aHulls[l].concatenate(aHulls[l+1]);
//
//				if (k % 2 == 1)
//					aHulls[k/2] = aHulls[k-1];
//			}
			// long way around, but this is debug code
			Subgroup aBestSubgroup = evaluateNumericBestIntervalBruteForceMM(theParent, aConditionBase, aRBICT);
			if (aBestSubgroup == null)
				return;
			// MM - these numbers are required for direct computation below
			//      do not use getTruePositiveRate(): might give rounding errors
			aBestNrTruePositives  = (int) aBestSubgroup.getTertiaryStatistic();
			aBestNrFalsePositives = (aBestSubgroup.getCoverage() - aBestNrTruePositives);
			aBestInterval         = aBestSubgroup.getConditions().get(aBestSubgroup.getDepth()-1).getNumericInterval();
			aBestQuality          = aBestSubgroup.getMeasureValue();
		}

		// FIXME both minimum and maximum coverage need to be checked
		//       evaluateNumericBestIntervalBruteForceMM() now does so

		// NOTE
		// when (aBestInterval.compareTo(oldInterval) == 0) the subgroup size or
		// ratios might have changed, yielding a different final quality
		//
		// when (aBestInterval < oldInterval) the new BestInterval might still
		// be a redundant addition, when an 'intermediate' Condition has reduced
		// the domain of oldInterval, the size-check below essentially includes
		// a test for that also
		int aChildCoverage = (aBestNrTruePositives + aBestNrFalsePositives);
		if (aChildCoverage == aParentCoverage)
		{
			itsSkipCount.incrementAndGet();
			if (BEST_INTERVAL_NO_REFINEMENT_SKIP)
				return;
		}

		Condition anAddedCondition = new Condition(aConditionBase, aBestInterval);
		Subgroup aChild = directComputation(theParent, anAddedCondition, aBestQuality, aChildCoverage, aBestNrTruePositives);
		checkAndLog(aChild, aParentCoverage);
	}

	// NOTE comparison is problematic when user presses stop/max time is reached
	private final Subgroup evaluateNumericBestIntervalBruteForceMM(Subgroup theParent, ConditionBase theConditionBase, RealBaseIntervalCrossTable theRBICT)
	{
		// code below relies on the the fact that these are both smaller than 0
		int unset       = Integer.MIN_VALUE;
		int useInfinite = -1;

		double aBestQuality      = Double.NEGATIVE_INFINITY;
		int aBestLo              = unset;
		int aBestHi              = unset;
		int aBestNrTruePositives = unset;
		int aBestCoverage        = unset;

		int aNrSplitPoints  = theRBICT.getNrSplitPoints();
		int aParentCoverage = theParent.getCoverage();
		int aParentTPsCount = (int) theParent.getTertiaryStatistic();

		for (int i = 0, head_cover = 0, head_tp = 0; i < aNrSplitPoints; ++i)
		{
			int aPi     = theRBICT.getPositiveCount(i);
			head_cover += (aPi + theRBICT.getNegativeCount(i));
			head_tp    += aPi;

			// from this split point to Infinity
			//
			// checking only ResultSet is not the best check, but fine for now
			// follows the original algorithm, the alternative would also keep
			// track of a Best for CandidateSet also (like Best/BestBins code)
			// NOTE isUsefulForResultSet() uses a different count every time
			if (isUsefulForResultSet((aParentCoverage-head_cover), aParentCoverage, itsMinimumCoverage, itsMaximumCoverage))
			{
				// NOTE float cast required for comparison to original code only
				double aQuality = (float) itsQualityMeasure.calculate(aParentTPsCount-head_tp, aParentCoverage-head_cover);
				if (aQuality > aBestQuality)
				{
					aBestQuality = aQuality;
					aBestLo = i+1;
					aBestHi = useInfinite;
					aBestNrTruePositives = (aParentTPsCount-head_tp);
					aBestCoverage        = (aParentCoverage-head_cover);
				}
			}

			// from -Infinity to this split point
			// if (itsUseNegInfty == true) first Interval is (-Inf, -Inf)
			if (isUsefulForResultSet(head_cover, aParentCoverage, itsMinimumCoverage, itsMaximumCoverage))
			{
				double aQuality = (head_cover == 0) ? Double.NaN : (float) itsQualityMeasure.calculate(head_tp, head_cover);
				if (aQuality > aBestQuality)
				{
					aBestQuality = aQuality;
					aBestLo = useInfinite;
					aBestHi = i;
					aBestNrTruePositives = head_tp;
					aBestCoverage        = head_cover;
				}
			}

			// from this split point to all subsequent split points
			// i is the left exclusive end point: all its counts are discarded
			for (int lo = i+1, j = lo, cover = 0, tp = 0; j < aNrSplitPoints; ++j)
			{
				int aPj = theRBICT.getPositiveCount(j);
				cover  += (aPj + theRBICT.getNegativeCount(j));
				tp     += aPj;

				if (isUsefulForResultSet(cover, aParentCoverage, itsMinimumCoverage, itsMaximumCoverage))
				{
					double aQuality = (float) itsQualityMeasure.calculate(tp, cover);
					if (aQuality > aBestQuality)
					{
						aBestQuality = aQuality;
						aBestLo = lo;
						aBestHi = j;
						aBestNrTruePositives = tp;
						aBestCoverage        = cover;
					}
				}
			}
		}

		if (aBestCoverage == unset)
			return null;

		// FIXME MM - getSplitPoints() is extremely inefficient: creates a copy
		//            of split points array, better create getSplitPoint(index)
		float[] aSplitPoints = ((aBestLo == unset) && (aBestLo == unset)) ? null : theRBICT.getSplitPoints();
		float l = (aBestLo < 0) ? Float.NEGATIVE_INFINITY : aSplitPoints[aBestLo-1]; // -1 should not be a problem with -Infinite
		float h = (aBestHi < 0) ? Float.POSITIVE_INFINITY : aSplitPoints[aBestHi];

		// MM - ignore the SKIP part for now
		Condition anAddedCondition = new Condition(theConditionBase, new Interval(l, h));
		return directComputation(theParent, anAddedCondition, aBestQuality, aBestCoverage, aBestNrTruePositives);
	}

	// NOTE comparison is problematic when user presses stop/max time is reached
	private final Subgroup evaluateNumericBestIntervalLinear(Subgroup theParent, ConditionBase aConditionBase, RealBaseIntervalCrossTable aRBICT)
	{
		double aBestQuality = Double.NEGATIVE_INFINITY;
		int aBestNrFalsePositives = Integer.MIN_VALUE;
		int aBestNrTruePositives = Integer.MIN_VALUE;
		Interval aBestInterval = new Interval(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

		ConvexHull [] aHulls = new ConvexHull[aRBICT.getNrBaseIntervals()];
		int aPi = 0;
		int aNi = 0;
		for (int l = 0; l < aRBICT.getNrSplitPoints(); l++)
		{
			aPi += aRBICT.getPositiveCount(l);
			aNi += aRBICT.getNegativeCount(l);
			aHulls[l] = new ConvexHull(aNi, aPi, aRBICT.getSplitPoint(l), Float.NEGATIVE_INFINITY);
		}
		aHulls[aRBICT.getNrBaseIntervals()-1] = new ConvexHull(aRBICT.getNegativeCount(), aRBICT.getPositiveCount(), Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);

		for (int k = aRBICT.getNrBaseIntervals(); k > 1; k = (k+1)/2)
		{
			for (int l = 0; l+1 < k; l += 2)
			{
				ConvexHull aMinkDiff = aHulls[l].minkowskiDifference(aHulls[l+1], true);
				for (int aSide = 0; aSide < 2; aSide++)
				{
					for (int i = 0; i < aMinkDiff.getSize(aSide) && !isTimeToStop(); i++)
					{
						if (aSide == 1 && (i == 0 || i == aMinkDiff.getSize(aSide)-1) )
							continue; // no need to check duplicate hull points

						HullPoint aCandidate = aMinkDiff.getPoint(aSide, i);
						double aQuality = itsQualityMeasure.calculate(aCandidate.itsY, (aCandidate.itsX + aCandidate.itsY));

						if (aQuality > aBestQuality)
						{
							aBestQuality = aQuality;
							aBestNrFalsePositives = aCandidate.itsX;
							aBestNrTruePositives = aCandidate.itsY;
							aBestInterval = new Interval(aCandidate.getLabel2(), aCandidate.itsLabel1);
						}
					}
				}
			}

			for (int l = 0; l+1 < k; l += 2)
				aHulls[l/2] = aHulls[l].concatenate(aHulls[l+1]);

			if (k % 2 == 1)
				aHulls[k/2] = aHulls[k-1];
		}

		int aChildCoverage = (aBestNrTruePositives + aBestNrFalsePositives);
		// MM ignore the SKIP part for now
		Condition anAddedCondition = new Condition(aConditionBase, aBestInterval);
		Subgroup aChild = directComputation(theParent, anAddedCondition, aBestQuality, aChildCoverage, aBestNrTruePositives);
//		checkAndLog(aChild, theParent.getCoverage());

		return aChild;
	}

	// temporary class to create pairs of Subgroups, will be removed, one Object
	// is created for each parent Subgroups that is refined, which is wasteful
	private static final class BestSubgroupsForCandidateSetAndResultSet
	{
		// NOTE these will often be the same Subgroup
		Subgroup itsBestForCandidateSet = null;
		Subgroup itsBestForResultSet    = null;
	}

	// FIXME boolean for SingleBinary setting, it already computed the quality
	//       will be replace by Subgroup.hasQuality() which returns true when a
	//       quality has already been computed for the Subgroup
	//       checkAndLog() will make use of it also, greatly simplifying code
	// NOTE  relying on (Subgroup.getQuality() == Double.NaN) is not possible
	//       first, the Subgroup constructor is broken, it copies all members
	//       fields from the parent, setting invalid values for the child
	//       second, all quality measures should be checked, some return NaN,
	//       such that Subgroup.setQuality(NaN) is possible
	//       obviously, a separate hasQuality() is not the preferred solution
	//       and should be replaced in a final implementation
	// NOTE  behaviour of LEQ and GEQ is inconsistent with respect Subgroup size
	//       boundaries are sorted ascendantly and for each bound the Best score
	//       is computed but the check below always uses (subgroup.score > Best)
	//       for LEQ the smaller of two equal-scoring Subgroups is selected
	//       for GEQ the larger  of two equal-scoring Subgroups is selected
	//       behaviour is identical when using (subgroup.score >= best) for LEQ
	private final void checkForBest(Subgroup theChild, int theParentCoverage, BestSubgroupsForCandidateSetAndResultSet theBestChilds, boolean hasQualityBeenComputed)
	{
		int aChildCoverage = theChild.getCoverage();

		boolean isUsefulForCandidateSet = isUsefulForCandidateSet(aChildCoverage, theParentCoverage, itsMinimumCoverage, theChild.getDepth(), itsSearchParameters.getSearchDepth());
		boolean isUsefulForResultSet = isUsefulForResultSet(aChildCoverage, theParentCoverage, itsMinimumCoverage, itsMaximumCoverage);

		if (!isUsefulForCandidateSet && !isUsefulForResultSet)
			return;

		if (!hasQualityBeenComputed)
			theChild.setMeasureValue(evaluateCandidate(theChild));

		double aQuality = theChild.getMeasureValue();

		if (isUsefulForCandidateSet && ((theBestChilds.itsBestForCandidateSet == null) || (aQuality > theBestChilds.itsBestForCandidateSet.getMeasureValue())))
			theBestChilds.itsBestForCandidateSet = theChild;

		if (isUsefulForResultSet    && ((theBestChilds.itsBestForResultSet == null)    || (aQuality > theBestChilds.itsBestForResultSet.getMeasureValue())))
			theBestChilds.itsBestForResultSet = theChild;
	}

	private static final boolean isUsefulForCandidateSet(int theChildCoverage, int theParentCoverage, int theMinimumCoverage, int theChildDepth, int theMaximumDepth)
	{
		// NOTE
		// in the following situations a Candidate can never yield a valid refinement at the next search level, and is therefore useless:
		//
		// when (theChildCoverage == theMinimumCoverage), as all Refinements would be too small, or of the same size as this Candidate, 
		//in which case they are also invalid (theChildCoverage == theParentCoverage)
		//
		// (theChildCoverage > 1) is added as the algorithm/GUI never checks theMinimumCoverage, it can even be negative when (theChildCoverage == 1) all refinements would either yield empty
		// Subgroups, or Refinements of the same size as this Candidate (Parent)
		// both are invalid
		//
		// when (theChildDepth == theMaximumDepth) there is no next search level

		return (theChildCoverage < theParentCoverage	&&
			theChildCoverage > theMinimumCoverage	&&
			theChildCoverage > 1 			&&
			theChildDepth < theMaximumDepth);
	}

	private static final boolean isUsefulForResultSet(int theChildCoverage, int theParentCoverage, int theMinimumCoverage, int theMaximumCoverage)
	{
		return (theChildCoverage < theParentCoverage	&&
			theChildCoverage >= theMinimumCoverage	&&
			theChildCoverage <= theMaximumCoverage);
	}

	// replacement of bestAdd(Subgroup, theParentCoverage) - allows two bests
	private final void checkAndLogBest(BestSubgroupsForCandidateSetAndResultSet theBestChildSubgroups, int theParentCoverage)
	{
		// original - NOTE behaviour of addToBuffer() needs to be reconsidered
//		assert (theBestSubgroup != null);
//
//		//addToBuffer(aBestSubgroup);
//		checkAndLog(theBestSubgroup, theOldCoverage);

		// new
		assert (theBestChildSubgroups != null);

		if (theBestChildSubgroups.itsBestForResultSet != null)
			checkAndLog(theBestChildSubgroups.itsBestForResultSet, theParentCoverage);

		if (USE_SINGLE_BEST_RESULT_LIKE_BEFORE)
			return;

		if (theBestChildSubgroups.itsBestForCandidateSet != null && (theBestChildSubgroups.itsBestForResultSet != theBestChildSubgroups.itsBestForCandidateSet))
			checkAndLog(theBestChildSubgroups.itsBestForCandidateSet, theParentCoverage);
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// generic Candidate evaluation code - some methods now bypass this /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	/*
	 * REQUIREMENT 1
	 * additions to itsResult and itsCandidateQueue need to be performed as a logical unit, else their contents would become undefined in the following multi-threaded scenario:
	 *
	 * Thread 1 itsResult.add()
	 * Thread 2 itsResult.add()
	 * Thread 2 itsCandidateQueue.add()
	 * Thread 1 itsCandidateQueue.add()
	 *
	 * both itsResults and itsCandidateQueue are trimmed if they have a max capacity and a candidate may end up in the one, but not in the other
	 * 
	 * REQUIREMENT 2
	 * evaluateCandidate() is expensive for complex models and should not be executed in a synchronized block
	 *
	 * REQUIREMENT 3
	 * additionally the value of itsCandidateCount.getAndIncrement() should indicate the n-th call to this method, so the n-th checked Candidate
	 * and the subgroup.nr should be this value also this can only be guaranteed by doing it in the same synchronized
	 * block but to keep the scope of the synchronized method small (synchronized blocks execute many times slower) the logging is not done in the
	 * synchronized method, but guarantees to use to the correct value
	 * FIXME MM
	 * if fact, this makes no sense at all in a multi-threaded environment because of the unpredictable order in which Subgroups generated from
	 * ('thread local') refinements arrive at check(), the count for any Subgroup is unpredictable anyway so coupling the check()-count to the subgroup number reported in the
	 * log does not guarantee invocation invariant logs, where a subgroup has the same number over consecutive identical experiments
	 * for a single thread, the behaviour will hold regardless, so no extra care is required
	 * TL;DR count + subgroup number will be decoupled / unsynchronised
	 *
	 * technically synchronisation needs only execute the addition to the result and candidate set as a logical block
	 *
	 * NOTE that in case of ties on the itsResult / itsCandidateQueue max_size boundary this may effect the final search result
	 * this is related to the fixed max size and has the potential to break invocation invariant results in multi-threaded settings
	 * FIXME MM
	 * this is probably no longer true for beam strategies, that move from
	 * one level to the next, when all candidates on the first are evaluated
	 * the new ConditionList compares Conditions in canonical, so
	 * regardless of the order in which they are inserted in to 
	 * itsResult / itsCandidateQueue, these constructs will always be
	 * ordered in the same way before moving to the next level / when an
	 * experiment completes without time constraints
	 * non-beam searches do not have a max size for itsCandidateQueue, so
	 * they do no suffer from this problem anyway
	 * (NOTE when a search is stopped because of max_time all bets are of)
	 */
	private final Object itsCheckLock = new Object();
	private void checkAndLog(Subgroup theChild, int theParentCoverage)
	{
		setTitle(theChild);

		int aChildCoverage = theChild.getCoverage();
		int aDepth         = theChild.getDepth();

		boolean isUsefulForCandidateSet = isUsefulForCandidateSet(aChildCoverage, theParentCoverage, itsMinimumCoverage, aDepth, itsSearchParameters.getSearchDepth());
		boolean isUsefulForResultSet    = isUsefulForResultSet(aChildCoverage, theParentCoverage, itsMinimumCoverage, itsMaximumCoverage);

		// FIXME MM this check should be made obsolete/checked by all callers
		boolean isValid = (isUsefulForCandidateSet || isUsefulForResultSet);

		if (isValid)
		{
			// NOTE
			// NumericStrategy could get a method like isBestStrategy(), but
			// than still it gives no guarantee that code in SubgroupDiscovery
			// sets the quality/secondary/tertiary statistics for theSubgroup
			// NOTE NUMERIC_INTERVALS is both a isPOCSetting and a aNumericBest
			EnumSet<NumericStrategy> aNumericBest = 
				EnumSet.of(NumericStrategy.NUMERIC_BEST, NumericStrategy.NUMERIC_BEST_BINS, NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_BEST, NumericStrategy.NUMERIC_INTERVALS);
			AttributeType lastAdded = theChild.getConditions().get(aDepth-1).getColumn().getType();
			boolean isLastNumeric = (lastAdded == AttributeType.NUMERIC);

			// final: ensure value is set before aResultAddition-check
			final float aQuality;

			// this is becoming a mess: to be replaced by Subgroup.hasQuality()
			if ((lastAdded == AttributeType.BINARY) && isDirectSingleBinary())
			{
				assert theChild.hasQuality();

				// NOTE this path already performed the isValid-coverage check
				aQuality = (float) theChild.getMeasureValue();
			}
			else if (isLastNumeric && isDirectSingleBinary())
			{
				assert theChild.hasQuality();

				// currently only for SINGLE_NOMINAL (and no propensity scores)
				// NOTE this path already performed the isValid-coverage check
				aQuality = (float) theChild.getMeasureValue();
			}
			else if (isLastNumeric && aNumericBest.contains(itsSearchParameters.getNumericStrategy()))
			{
				assert theChild.hasQuality();

				// NOTE for BEST* Subgroup is already evaluated and quality is
				// set by isValidAndBest() or numericIntervals() (BestInterval)
				// NOTE isValidAndBest() performs an incorrect isValid-coverage
				// check, and BestInterval does not perform one at all
				aQuality = (float) theChild.getMeasureValue();
			}
			else if ((lastAdded == AttributeType.NOMINAL) && itsSearchParameters.getNominalSets())
			{
				assert theChild.hasQuality();

				// BestValueset already set quality (BestInterval did also, but
				// is picked up by the BEST* check below)
				// NOTE both code paths did not performed the isValid-coverage
				//      check refer to the methods for comments on this issue
				aQuality = (float) theChild.getMeasureValue();
			}
			else
			{
				assert !theChild.hasQuality();

				aQuality = evaluateCandidate(theChild);
				theChild.setMeasureValue(aQuality);
			}

			//check for minimum support threshold
			if (itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL &&
			    theChild.getTertiaryStatistic() < itsMinimumSupport)			//in SINGLE_NOMINAL the tertiary statistic is the number of positives
			{
				isUsefulForResultSet = false;		// is below minsup
				isUsefulForCandidateSet = false;	// and no refinement will ever be above minsup again
System.out.println("================== discarded because below minsup==============");
			}

			// FIXME to avoid excessive locking, itsCandidateQueue should also have a (dirty) hasPotential() method, such that if there is
			//       no chance at all that the Subgroup would be added to either the CandidateSet or the ResultSet no lock is ever acquired
			isUsefulForResultSet &= (ignoreQualityMinimum || (aQuality > itsQualityMeasureMinimum));
			// currently not a synchronised call, but perform separately anyway
			if (isUsefulForResultSet)
				isUsefulForResultSet &= itsResult.hasPotential(aQuality);

			if (isUsefulForCandidateSet || isUsefulForResultSet)
			{
				// do not Construct this Object within the synchronized block
				Candidate aCandidate = (isUsefulForCandidateSet ? new Candidate(theChild) : null);

				// performed as a logical unit, see REQUIREMENT 1
				// all logic is performed outside of synchronized block
				// to keep it as small as possible
				synchronized (itsCheckLock)
				{
					if (isUsefulForResultSet)
						itsResult.add(theChild);

					if (isUsefulForCandidateSet)
						itsCandidateQueue.add(aCandidate);
				}
			}
		}

		// prevent OutOfMemory / GC Overhead Limit errors, some code paths bypass evaluateCandidate(Subgroup) so calling it there is no good
		// and this is the sole method to add to Candidate and Result sets
		theChild.killMembers();

		// incrementing after expensive check() makes subgroup numbers in log 'closer to being consecutive' when multi-threading a synchronized block with itsCandidateCount.getAndIncrement()
		// and logCandidateAddition() would yield consecutive numbers but is slower and does not yield useful practical benefits
		long count = itsCandidateCount.getAndIncrement();

		if (isValid)
			logCandidateAddition(theChild, count);
	}

	// log as a single message, else messages of other threads end up in between
	private void logCandidateAddition(Subgroup theSubgroup, long count)
	{
		if (NO_CANDIDATE_LOG)
			return;

		String aCandidate = theSubgroup.getConditions().toString();

		StringBuilder sb = new StringBuilder(aCandidate.length() + 100);
		sb.append("candidate ");
		sb.append(aCandidate);
		sb.append(" size: ");
		sb.append(theSubgroup.getCoverage());
		sb.append("\n  subgroup nr. ");
		sb.append(count);
		sb.append("; quality ");
		sb.append(theSubgroup.getMeasureValue());

		Log.logCommandLine(sb.toString());
	}

	private float evaluateCandidate(Subgroup theChild)
	{
		switch (itsSearchParameters.getTargetType())
		{
			case SINGLE_NOMINAL     : return evaluateCandidateSingleNominal(theChild);
			case SINGLE_NUMERIC     : return evaluateCandidateSingleNumeric(theChild);
			case MULTI_NUMERIC      : return evaluateCandidateMultiNumeric(theChild);
			case DOUBLE_REGRESSION  : return evaluateCandidateDoubleRegression(theChild);
			case DOUBLE_CORRELATION : return evaluateCandidateDoubleCorrelation(theChild);
			case DOUBLE_BINARY      : return evaluateCandidateDoubleBinary(theChild);
			case SCAPE              : return evaluateCandidateScape(theChild);
			case MULTI_LABEL        : return evaluateCandidateMultiLabel(theChild);
			case LABEL_RANKING      : return evaluateCandidateLabelRanking(theChild);
			default :
				throw new AssertionError(itsSearchParameters.getTargetType());
		}
	}

	private final float evaluateCandidateSingleNominal(Subgroup theChild)
	{
		int aCoverage = theChild.getCoverage();
		BitSet aChildMembers = theChild.getMembers();

		// getMembers() always returns a clone so and() is safe
		aChildMembers.and(itsBinaryTarget);
		int aCountHeadBody = aChildMembers.cardinality();
		// FIXME MM - hold: functional change + needs testing / profiling
		// int aCountHeadBody = theNewSubgroup.countCommon(itsBinaryTarget);

		final float aQuality;

		QM aMeasure = itsSearchParameters.getQualityMeasure();
		if ((aMeasure != QM.PROP_SCORE_WRACC) && (aMeasure != QM.PROP_SCORE_RATIO))
			aQuality = (float) itsQualityMeasure.calculate(aCountHeadBody, aCoverage);
		else
		{
			PropensityScore aPropensityScore = new PropensityScore(theChild, itsBinaryTarget, itsLocalKnowledge, itsGlobalKnowledge, PropensityScore.LOGISTIC_REGRESSION);
			double aCountHeadPropensityScore = aPropensityScore.getPropensityScoreSum();
			aQuality = QualityMeasure.calculatePropensityBased(aMeasure, aCountHeadBody, aCoverage, itsNrRows, aCountHeadPropensityScore);
		}

		theChild.setSecondaryStatistic(aCountHeadBody / (double) aCoverage);
		theChild.setTertiaryStatistic(aCountHeadBody);

		return aQuality;
	}

	private final float evaluateCandidateSingleNumeric(Subgroup theChild)
	{
		BitSet aChildMembers = theChild.getMembers();
		final float aQuality;

		if (!TEMPORARY_CODE)
		{
			QM aQM = itsSearchParameters.getQualityMeasure();
			Set<Stat> aRequiredStats = QM.requiredStats(aQM);
			Statistics aStatistics = itsNumericTarget.getStatistics(itsSelection, aChildMembers, aQM == QM.MMAD, aRequiredStats.contains(Stat.COMPL));

			ProbabilityDensityFunction aPDF = null;
			if (aRequiredStats.contains(Stat.PDF))
			{
				// FIXME MM TEMP
				System.out.format("#Subgroup: '%s' (size = %d)%n", theChild, theChild.getCoverage());

				// DEBUG
				if (!ProbabilityDensityFunction.USE_ProbabilityDensityFunction2)
					aPDF = new ProbabilityDensityFunction(itsQualityMeasure.getProbabilityDensityFunction(), aChildMembers);
				else
					aPDF = new ProbabilityDensityFunction2(itsQualityMeasure.getProbabilityDensityFunction(), aChildMembers);
				aPDF.smooth();
			}

			aQuality = itsQualityMeasure.calculate(aStatistics, aPDF);
			theChild.setSecondaryStatistic(aStatistics.getSubgroupAverage());
			theChild.setTertiaryStatistic(Math.sqrt(aStatistics.getSubgroupSumSquaredDeviations()/(theChild.getCoverage()-1.0))); // use n-1 like t-statistic
		}
		else
		{
			int aChildCoverage = theChild.getCoverage();

			// FIXME MM TEMP
			System.out.format("#Subgroup: '%s' (size = %d)%n", theChild, aChildCoverage);

			ProbabilityMassFunction_ND aPMF = new ProbabilityMassFunction_ND((ProbabilityMassFunction_ND) itsQualityMeasure.getProbabilityDensityFunction(), aChildMembers);
			aQuality = itsQualityMeasure.calculate(new Statistics(aChildCoverage, -1, -1, -1, -1), aPMF);
			theChild.setSecondaryStatistic(aChildCoverage);
			theChild.setTertiaryStatistic(itsNrRows - aChildCoverage);
		}

		return aQuality;
	}

	private final float evaluateCandidateMultiNumeric(Subgroup theChild)
	{
			// for now always test against complement
			float aQuality = itsPDF_ND.getDensityDifference(theChild.getMembers(), true, itsSearchParameters.getQualityMeasure());
			// for now set just the sizes, most computations
			// are expensive and neglected anyway
			theChild.setSecondaryStatistic(theChild.getCoverage());
			theChild.setTertiaryStatistic(itsNrRows - theChild.getCoverage());

			return aQuality;
	}

	private final float evaluateCandidateDoubleRegression(Subgroup theChild)
	{
		BitSet aChildMembers = theChild.getMembers();
		double aQuality;

		assert (itsSearchParameters.getQualityMeasure() == itsBaseRM.itsQualityMeasure);
		// TODO MM - was static member field of RM (probably bad programming)
		switch (itsBaseRM.itsQualityMeasure)
		{
			case REGRESSION_SSD_COMPLEMENT :
			case REGRESSION_SSD_DATASET    :
			case REGRESSION_FLATNESS       :
			case REGRESSION_SSD_4          :
			{
				RegressionMeasure aRM = new RegressionMeasure(itsBaseRM, aChildMembers);
				aQuality = aRM.getEvaluationMeasureValue();
				theChild.setSecondaryStatistic(aRM.getSlope());
				theChild.setTertiaryStatistic(aRM.getIntercept());
				break;
			}
			/*
			case COOKS_DISTANCE :
			{
				// initialize variables
				double aThreshold = -Double.MAX_VALUE;
				boolean aNeedToComputeRegression = true;
				boolean aNeedToComputeBounds = true;

				// check what the pruning quality will be, if this exists at all
				int aBorderlineSubgroupNumber;
				if (theChild.getDepth() < itsSearchParameters.getSearchDepth())
					aBorderlineSubgroupNumber = itsSearchParameters.getSearchStrategyWidth();
				else
					aBorderlineSubgroupNumber = itsSearchParameters.getMaximumSubgroups();

				// TODO these methods on itsResult are not (yet) thread save and
				// will cause problems during concurrent access, easy to fix
				if ( itsResult.size() >= aBorderlineSubgroupNumber)
					aThreshold = itsResult.last().getMeasureValue();
				else
					aNeedToComputeBounds = false;

				// start actual computation
				Log.logCommandLine("");
				int aSampleSize = theChild.getCoverage();

				// filter out rank deficient model that crash matrix
				// multiplication library
				// TODO: should read < itsP instead of < 2 !!!
				if (aSampleSize < 2)
				{
					itsRankDefCount++;
					return -Float.MAX_VALUE;
				}

				itsBaseRM.computeRemovedIndices(aChildMembers, aSampleSize);

				// calculate the upper bound values
				// before each bound, only the necessary computations are done
				if (aNeedToComputeBounds)
				{
					double aT = itsBaseRM.getT(aSampleSize);
					double aRSquared = itsBaseRM.getRSquared(aSampleSize);

					// bound seven
					double aBoundSeven = itsBaseRM.computeBoundSeven(aSampleSize, aT, aRSquared);
					if (aBoundSeven < Double.MAX_VALUE)
					{
						Log.logCommandLine("                   Bound 7: " + aBoundSeven);
						itsBoundSevenCount++;
					}

					if (aBoundSeven < aThreshold)
					{
						aNeedToComputeRegression = false;
						itsBoundSevenFired++;
					}
					else
					{	// bound six
						double aBoundSix = itsBaseRM.computeBoundSix(aSampleSize, aT);
						if (aBoundSix < Double.MAX_VALUE)
						{
							Log.logCommandLine("                   Bound 6: " + aBoundSix);
							itsBoundSixCount++;
						}
						if (aBoundSix < aThreshold)
						{
							aNeedToComputeRegression = false;
							itsBoundSixFired++;
						}
						else
						{	// bound five
							double aBoundFive = itsBaseRM.computeBoundFive(aSampleSize, aRSquared);
							if (aBoundFive < Double.MAX_VALUE)
							{
								Log.logCommandLine("                   Bound 5: " + aBoundFive);
								itsBoundFiveCount++;
							}
							if (aBoundFive < aThreshold)
							{
								aNeedToComputeRegression = false;
								itsBoundFiveFired++;
							}
							else
							{	// bound four
								double aBoundFour = itsBaseRM.computeBoundFour(aSampleSize);
								if (aBoundFour < Double.MAX_VALUE)
								{
									Log.logCommandLine("                   Bound 4: " + aBoundFour);
									itsBoundFourCount++;
								}
								if (aBoundFour < aThreshold)
								{
									aNeedToComputeRegression = false;
									itsBoundFourFired++;
								}
							}
						}
					}
				}

				// finally, compute regression
				if (aNeedToComputeRegression)
				{
					double aDoubleQuality = itsBaseRM.calculate(theChild);
					if (aDoubleQuality == -Double.MAX_VALUE)
						itsRankDefCount++;
					aQuality = (float) aDoubleQuality;
				}
				else
					aQuality = -Float.MAX_VALUE;
				break;
			}
			*/
			default :
				throw new AssertionError(itsBaseRM.itsQualityMeasure);
		}

		return (float) aQuality;
	}

    private final float evaluateCandidateDoubleCorrelation(Subgroup theChild)
    {
        BitSet theChildMembers = theChild.getMembers();
        CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);

        for (int i = theChildMembers.nextSetBit(0); i >= 0; i = theChildMembers.nextSetBit(i+1))
            aCM.addObservation(itsPrimaryColumn.getFloat(i), itsSecondaryColumn.getFloat(i));

        theChild.setSecondaryStatistic(aCM.getCorrelation());
        theChild.setTertiaryStatistic(aCM.computeCorrelationDistance()); // intercept
        double aQuality = aCM.getEvaluationMeasureValue();

        return (float) aQuality;
    }
    
    private final float evaluateCandidateDoubleBinary(Subgroup theChild)
    {
        BitSet theChildMembers = theChild.getMembers();
        
        //subgroup size
        int aCoverage = theChild.getCoverage();
//        System.out.println("coverage: " + aCoverage);

        //FIXME these general statistics don't change per subgroup, so could be obtained once and stored. Saves time
        //dataset statistics
        BitSet aPrimaryMembers = itsPrimaryColumn.getBinaries(); // dataset A
        BitSet aSecondaryMembers = itsSecondaryColumn.getBinaries();
        int aSizeA = aPrimaryMembers.cardinality();
	int aSizeB = itsNrRows-aSizeA;
//        System.out.println("dataset: " + itsNrRows + ", Dataset A: " + aSizeA + ", Dataset B: " + aSizeB);

        //target within A and B
        aSecondaryMembers.and(aPrimaryMembers);
        float aTargetA = aSecondaryMembers.cardinality()/(float)aSizeA;
//        System.out.println("target A: " + aSecondaryMembers.cardinality() + " (" + aTargetA + ")");
        aSecondaryMembers = itsSecondaryColumn.getBinaries();
        aSecondaryMembers.andNot(aPrimaryMembers);
        float aTargetB = aSecondaryMembers.cardinality()/(float)aSizeB;
//        System.out.println("target B: " + aSecondaryMembers.cardinality() + " (" + aTargetB + ")");

        //subgroup within A
        BitSet aSubset = theChild.getMembers(); //subgroup
        aSubset.and(aPrimaryMembers); //subgroup within A
        int aSubgroupPrimaryCountA = aSubset.cardinality();
//        System.out.println("subgroup within A: " + aSubset.cardinality());
        if (aSubgroupPrimaryCountA == 0) //FIXME
        {
            theChild.setSecondaryStatistic(0);
            return 0f;
        }

        //subgroup within A with target = true
        aSubset.and(itsSecondaryColumn.getBinaries()); //subgroup within A with target = true
        int aSubgroupPrimarySecondaryCountA = aSubset.cardinality();
        float aSubgroupTargetA = aSubgroupPrimarySecondaryCountA/(float)aSubgroupPrimaryCountA;
//        System.out.println("subgroup within A with target = true: " + aSubgroupPrimarySecondaryCountA + " (" + aSubgroupTargetA + ")");
        theChild.setTertiaryStatistic(aSubgroupTargetA); // used?

        //subgroup within B with target = true
        aSubset = theChild.getMembers(); //subgroup
        aSubset.andNot(aPrimaryMembers); //subgroup within B
        int aSubgroupPrimaryCountB = aSubset.cardinality();
        if (aSubgroupPrimaryCountB == 0) //FIXME
        {
            theChild.setSecondaryStatistic(0);
            return 0f;
        }
        aSubset.and(itsSecondaryColumn.getBinaries()); //subgroup within B with target = true
        int aSubgroupPrimarySecondaryCountB = aSubset.cardinality();
        float aSubgroupTargetB = aSubgroupPrimarySecondaryCountB/(float)aSubgroupPrimaryCountB;
//        System.out.println("subgroup within B with target = true: " + aSubgroupPrimarySecondaryCountB + " (" + aSubgroupTargetB + ")");

        theChild.setSecondaryStatistic(Math.signum((aSubgroupTargetA-aTargetA)/aTargetA)); //relative lift of subgroup in A

	float aWRAccA = aSubgroupPrimarySecondaryCountA/(float)aSizeA - aTargetA*aSubgroupPrimaryCountA/(float)aSizeA;
	float aWRAccB = aSubgroupPrimarySecondaryCountB/(float)aSizeB - aTargetB*aSubgroupPrimaryCountB/(float)aSizeB;
//        System.out.println("WRAcc A: " + aWRAccA);
//        System.out.println("WRAcc B: " + aWRAccB);

        switch (itsSearchParameters.getQualityMeasure())
        {
            case RELATIVE_WRACC :    return (aWRAccB!=0) ? aWRAccA/aWRAccB : Float.MAX_VALUE;
            case ABSOLUTE_WRACC :    return aWRAccA-aWRAccB;
            case RELATIVE_RISK :     return ((aSubgroupTargetA-aTargetA)/aTargetA) / ((aSubgroupTargetB-aTargetB)/aTargetB);
            case ABSOLUTE_RISK :     return ((aSubgroupTargetA-aTargetA)/aTargetA) - ((aSubgroupTargetB-aTargetB)/aTargetB);
            default :                   return 0; //should not happen
        }
    }

	private final float evaluateCandidateScape(Subgroup theChild)
	{
		int aCoverage = theChild.getCoverage();

		BitSet aChildMembers = theChild.getMembers();
		BitSet aClone = (BitSet) aChildMembers.clone();

		// getMembers() always returns a clone so and() is safe
		aChildMembers.and(itsPrimaryColumn.getBinaries());
		int aCountHeadBody = aChildMembers.cardinality();
		// FIXME MM - hold: functional change
		// int aCountHeadBody = theNewSubgroup.countCommon(itsPrimaryColumn.getBinaries());

		float aQuality = itsQualityMeasure.calculate(aClone, aCoverage, aCountHeadBody);
		theChild.setSecondaryStatistic(aCountHeadBody);
		theChild.setTertiaryStatistic(aCoverage - aCountHeadBody);

		return aQuality;
	}

	// FIXME
	// multiLabelCalculate(Subgroup) uses Subgroup.getMembers() to select a
	// subset of records from itsBinaryTable, supply BitSet as parameter
	private final float evaluateCandidateMultiLabel(Subgroup theChild)
	{
		BinaryTable aBinaryTable = itsBinaryTable.selectRows(theChild.getMembers());
		Bayesian aBayesian = new Bayesian(aBinaryTable, itsTargets);
		aBayesian.climb();               //induce DAG
		DAG aDAG = aBayesian.getDAG();
		theChild.setDAG(aDAG);           //store DAG with subgroup for later use

		float aQuality = itsQualityMeasure.calculate(theChild);
		theChild.setSecondaryStatistic(itsQualityMeasure.calculateEditDistance(theChild.getDAG()));
		theChild.setTertiaryStatistic(QualityMeasure.calculateEntropy(itsNrRows, theChild.getCoverage()));

		return aQuality;
	}

	// FIXME
	// getAverageRankingMatrix(Subgroup) and getAverageRanking(Subgroup)
	// only use Subgroup.getMembers(), supply BitSet as sole parameter
	private final float evaluateCandidateLabelRanking(Subgroup theChild)
	{
		int aCoverage = theChild.getCoverage();
		LabelRankingMatrix aLRM = itsTargetRankings.getAverageRankingMatrix(theChild);

		float aQuality = itsQualityMeasure.computeLabelRankingDistance(aCoverage, aLRM);
		theChild.setLabelRanking(itsTargetRankings.getAverageRanking(theChild));
		theChild.setLabelRankingMatrix(aLRM);

		// TODO make this more sensible
		//theChild.setSecondaryStatistic(aCountHeadBody / (double) aCoverage);
		//theChild.setTertiaryStatistic(aCountHeadBody);

		return aQuality;
	}

/*
TODO for stable jar, disabled, causes compile errors, reinstate later
	private void generateBoundGraph()
	{
		for (int aSampleSize = itsMaximumCoverage-1; aSampleSize >= 2; aSampleSize--)
		{
			Log.logCommandLine("aSampleSize = " + aSampleSize);
			int aBoundSevenComputable = 0;
			int aBoundSixComputable = 0;
			int aBoundFiveComputable = 0;
			int aBoundFourComputable = 0;
			int aCookComputable = 0;
			double avgBoundSeven = 0.0;
			double avgBoundSix = 0.0;
			double avgBoundFive = 0.0;
			double avgBoundFour = 0.0;
			double avgCook = 0.0;
			long avgRegressionTime = 0;
			for (int i=0; i<100; i++)
			{
				Subgroup aSubgroup = itsTable.getRandomSubgroup(aSampleSize);
				BitSet aMembers = aSubgroup.getMembers();
				itsBaseRM.computeRemovedIndices(aMembers, aSampleSize);
				double aT = itsBaseRM.getT(aSampleSize);
				double aRSquared = itsBaseRM.getRSquared(aSampleSize);
				double aBoundSeven = itsBaseRM.computeBoundSeven(aSampleSize, aT, aRSquared);
				double aBoundSix = itsBaseRM.computeBoundSix(aSampleSize, aT);
				double aBoundFive = itsBaseRM.computeBoundFive(aSampleSize, aRSquared);
				double aBoundFour = itsBaseRM.computeBoundFour(aSampleSize);
				long aStartTime = System.currentTimeMillis();
				double aDoubleQuality = itsBaseRM.calculate(aSubgroup);
				long anEndTime = System.currentTimeMillis();
				if (aBoundSeven<Double.MAX_VALUE)
				{
					aBoundSevenComputable++;
					avgBoundSeven += aBoundSeven;
				}
				if (aBoundSix<Double.MAX_VALUE)
				{
					aBoundSixComputable++;
					avgBoundSix += aBoundSix;
				}
				if (aBoundFive<Double.MAX_VALUE)
				{
					aBoundFiveComputable++;
					avgBoundFive += aBoundFive;
				}
				if (aBoundFour<Double.MAX_VALUE)
				{
					aBoundFourComputable++;
					avgBoundFour += aBoundFour;
				}
				if (aDoubleQuality > -Double.MAX_VALUE)
				{
					aCookComputable++;
					avgCook += aDoubleQuality;
					avgRegressionTime += (anEndTime - aStartTime);
				}
			}
			if (aBoundSevenComputable>0)
				avgBoundSeven /= aBoundSevenComputable;
			if (aBoundSixComputable>0)
				avgBoundSix /= aBoundSixComputable;
			if (aBoundFiveComputable>0)
				avgBoundFive /= aBoundFiveComputable;
			if (aBoundFourComputable>0)
				avgBoundFour /= aBoundFourComputable;
			if (aCookComputable>0)
			{
				avgCook /= aCookComputable;
				avgRegressionTime /= aCookComputable;
			}
			Log.logRefinement(""+aSampleSize+","+avgRegressionTime+","+avgCook+","+avgBoundSeven+","+avgBoundSix+","+avgBoundFive+","+avgBoundFour+","+aCookComputable+","+aBoundSevenComputable+","+aBoundSixComputable+","+aBoundFiveComputable+","+aBoundFourComputable);
		}
	}
*/
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// general methods to return information about this instance        /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	public int getNumberOfSubgroups() { return itsResult.size(); }
	public SubgroupSet getResult() { return itsResult; }
	public QualityMeasure getQualityMeasure() { return itsQualityMeasure; }
	public SearchParameters getSearchParameters() { return itsSearchParameters; }

	/**
	 * Return the base {@link RegressionMeasure} for this SubgroupDiscovery.
	 *
	 * @return the base RegressionMeasure, if this SubgroupDiscovery is of
	 * {@link TargetType} {@link TargetType#DOUBLE_REGRESSION}, {@code null}
	 * otherwise.
	 */
	public RegressionMeasure getRegressionMeasureBase()
	{
		return itsBaseRM;
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// remove useless refinements - replaces Filter class below         /////
	///// 18 combinations, tailored to the AttributeTypes occurring in the /////
	///// data, and the search parameters                                  /////
	///// + NEVER_SKIP: always returns false, to never skip any refinement /////
	/////                                                                  /////
	///// one of the Fltrs is selected at the start of mine(), thereafter  /////
	///// it is reused by all Tests for every Refinement of every Subgroup /////
	///// enums are immutable and can freely be shared among all Threads   /////
	/////                                                                  /////
	///// TODO assert mutual-exclusivity of:                               /////
	/////   ClassLabel+EQUALS                v. ValueSet+ELEMENT_OF        /////
	/////   regular+(EQUALS/BETWEEN,LEQ,GEQ) v. BestInterval+BETWEEN       /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	private static enum Fltr
	{
		// 1 - none of the data set/subgroup possibilities allow a skip
		NOBIN__NONOM__NONUMEQBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				// omit assert that verifies that all Columns are NUMERIC
				return NOTHING_TO_SKIP;
			}
		},
		// 2 - skip first Condition with Operator.EQUALS (must be NUMERIC)
		NOBIN__NONOM__NUMEQ
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				for (int i = fromIndex, j = theConditionList.size(); i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (c.getColumn().getType() == AttributeType.NUMERIC);

					// Operator is any of LEQ/GEQ/EQUALS, depending on setting
					if (c.getOperator() == Operator.EQUALS)
						return i;
				}

				// no NUMERIC equals found, nothing to skip
				return NOTHING_TO_SKIP;
			}
		},
		// 3 - skip, for any non-empty ConditionLists this setting implies that
		//     a BestInterval (NUMERIC) occurs at the last search-order position
		NOBIN__NONOM__NUMBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				int size = theConditionList.size();

				assert ((size == 0) ||
						(theConditionList.get(size-1).getColumn().getType() == AttributeType.NUMERIC && theConditionList.get(size-1).getNumericInterval() != null));

				// NOTE for (size == 0) this is out of range / NOTHING_TO_SKIP
				return size-1;
			}
		},
		// 4 - skip first Condition with Operator.EQUALS (must be NOMINAL)
		NOBIN__NOMCL__NONUMEQBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				for (int i = fromIndex, j = theConditionList.size(); i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (c.getColumn().getType() == AttributeType.NOMINAL);
						return i;
					}
				}

				return NOTHING_TO_SKIP;
			}
		},
		// 5 - skip first Condition with Operator.EQUALS (NOMINAL|NUMERIC)
		//     same loop as 4. except for the missing assert in the if-check
		NOBIN__NOMCL__NUMEQ
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				for (int i = fromIndex, j = theConditionList.size(); i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
						return i;
				}

				return NOTHING_TO_SKIP;
			}
		},
		// 6 - skip first Condition with Operator.EQUALS (NOMINAL) or when
		//     BestInterval (NUMERIC) occurs at the last search-order position
		NOBIN__NOMCL__NUMBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				// avoid IndexOutOfBoundsException() for EMPTY_LIST
				int size = theConditionList.size();
				if (size == 0)
					return NOTHING_TO_SKIP;

				Condition last = theConditionList.get(size-1);
				int bi = (last.getNumericInterval() == null) ? NOTHING_TO_SKIP : size-1;
				int eq = NOTHING_TO_SKIP;

				// loop over all remaining Conditions to search for EQUALS
				for (int i = fromIndex, j = size; i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (c.getColumn().getType() == AttributeType.NOMINAL);
						eq = i;
						break;
					}
				}

				// check that NOTHING_TO_SKIP is strictly smaller than fromIndex
				assert ((fromIndex >= 0) && (NOTHING_TO_SKIP < 0));

				if ((bi >= fromIndex) && (eq >= fromIndex))
					return Math.min(bi, eq);
				else if (bi >= fromIndex)
					return bi;
				else if (eq >= fromIndex)
					return eq;
				return NOTHING_TO_SKIP;
			}
		},
		// 7 - skip only when a BestValueSet (NOMINAL) occurs at the last
		//     search-order position
		NOBIN__NOMVS__NONUMEQBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				// avoid IndexOutOfBoundsException for EMPTY_LIST
				int size = theConditionList.size();
				if (size == 0)
					return NOTHING_TO_SKIP;

				Condition c = theConditionList.get(size-1);
				if (c.getNominalValueSet() == null)
				{
					assert (c.getColumn().getType() == AttributeType.NUMERIC);
					return NOTHING_TO_SKIP;
				}

				assert (c.getColumn().getType() == AttributeType.NOMINAL);
				int vs = size-1;
				return (vs >= fromIndex) ? vs : NOTHING_TO_SKIP;
			}
		},
		// 8 - skip first Condition with Operator.EQUALS (NUMERIC) or when
		//     BestValueSet (NOMINAL) occurs at the last search-order position
		NOBIN__NOMVS__NUMEQ
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				// avoid IndexOutOfBoundsException() for EMPTY_LIST
				int size = theConditionList.size();
				if (size == 0)
					return NOTHING_TO_SKIP;

				Condition last = theConditionList.get(size-1);
				int vs = (last.getNominalValueSet() == null) ? NOTHING_TO_SKIP : size-1;
				int eq = NOTHING_TO_SKIP;

				// loop over all remaining Conditions to search for EQUALS
				for (int i = fromIndex, j = size; i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (c.getColumn().getType() == AttributeType.NUMERIC);
						eq = i;
						break;
					}
				}

				// check that NOTHING_TO_SKIP is strictly smaller than fromIndex
				assert ((fromIndex >= 0) && (NOTHING_TO_SKIP < 0));

				if ((vs >= fromIndex) && (eq >= fromIndex))
					return Math.min(vs, eq);
				else if (vs >= fromIndex)
					return vs;
				else if (eq >= fromIndex)
					return eq;
				return NOTHING_TO_SKIP;
			}
		},
		// 9 - skip, for any non-empty ConditionLists this setting implies that
		//     either a BestValueSet (NOMINAL) or a BestInterval (NUMErIC)
		//     occurs at the last search-order position
		NOBIN__NOMVS__NUMBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				int size = theConditionList.size();

				assert ((size == 0) ||
						(theConditionList.get(size-1).getColumn().getType() == AttributeType.NOMINAL && theConditionList.get(size-1).getNominalValueSet() != null) ||
						(theConditionList.get(size-1).getColumn().getType() == AttributeType.NUMERIC && theConditionList.get(size-1).getNumericInterval() != null));

				// NOTE for (size == 0) this is out of range / NOTHING_TO_SKIP
				return size-1;
			}
		},
		// 10 - skip first Condition with Operator.EQUALS (must be BINARY)
		BIN__NONOM__NONUMEQBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				for (int i = fromIndex, j = theConditionList.size(); i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.BINARY, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (c.getColumn().getType() == AttributeType.BINARY);
						return i;
					}
				}

				return NOTHING_TO_SKIP;
			}
		},
		// 11 - skip first Condition with Operator.EQUALS (BINARY|NUMERIC)
		//      same loop as 10. except for the missing assert in the if-check
		BIN__NONOM__NUMEQ
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				for (int i = fromIndex, j = theConditionList.size(); i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.BINARY, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
						return i;
				}

				return NOTHING_TO_SKIP;
			}
		},
		// 12 - skip first Condition with Operator.EQUALS (BINARY) or when
		//      BestInterval (NUMERIC) occurs at last search-order position
		BIN__NONOM__NUMBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				// avoid IndexOutOfBoundsException() for EMPTY_LIST
				int size = theConditionList.size();
				if (size == 0)
					return NOTHING_TO_SKIP;

				Condition last = theConditionList.get(size-1);
				int bi = (last.getNumericInterval() == null) ? NOTHING_TO_SKIP : size-1;
				int eq = NOTHING_TO_SKIP;

				// loop over all remaining Conditions to search for EQUALS
				for (int i = fromIndex, j = size; i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.BINARY, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (c.getColumn().getType() == AttributeType.BINARY);
						eq = i;
						break;
					}
				}

				// check that NOTHING_TO_SKIP is strictly smaller than fromIndex
				assert ((fromIndex >= 0) && (NOTHING_TO_SKIP < 0));

				if ((bi >= fromIndex) && (eq >= fromIndex))
					return Math.min(bi, eq);
				else if (bi >= fromIndex)
					return bi;
				else if (eq >= fromIndex)
					return eq;
				return NOTHING_TO_SKIP;
			}
		},
		// 13 - skip first Condition with Operator.EQUALS (BINARY|NOMINAL)
		BIN__NOMCL__NONUMEQBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				for (int i = fromIndex, j = theConditionList.size(); i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.BINARY, AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (EnumSet.of(AttributeType.BINARY, AttributeType.NOMINAL).contains(c.getColumn().getType()));
						return i;
					}
				}

				return NOTHING_TO_SKIP;
			}
		},
		// 14 - skip first Condition with Operator.EQUALS BINARY|NOMINAL|NUMERIC
		//      same loop as 13. except for the missing assert in the if-check
		BIN__NOMCL__NUMEQ
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				for (int i = fromIndex, j = theConditionList.size(); i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.BINARY, AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
						return i;
				}

				return NOTHING_TO_SKIP;
			}
		},
		// 15 - skip first Condition with Operator.EQUALS (BINARY|NOMINAL) or
		//      when BestInterval (NUMERIC) occurs at last search-order position
		BIN__NOMCL__NUMBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				// avoid IndexOutOfBoundsException() for EMPTY_LIST
				int size = theConditionList.size();
				if (size == 0)
					return NOTHING_TO_SKIP;

				Condition last = theConditionList.get(size-1);
				int bi = (last.getNumericInterval() == null) ? NOTHING_TO_SKIP : size-1;
				int eq = NOTHING_TO_SKIP;

				// loop over all remaining Conditions to search for EQUALS
				for (int i = fromIndex, j = size; i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.BINARY, AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (EnumSet.of(AttributeType.BINARY, AttributeType.NOMINAL).contains(c.getColumn().getType()));
						eq = i;
						break;
					}
				}

				// check that NOTHING_TO_SKIP is strictly smaller than fromIndex
				assert ((fromIndex >= 0) && (NOTHING_TO_SKIP < 0));

				if ((bi >= fromIndex) && (eq >= fromIndex))
					return Math.min(bi, eq);
				else if (bi >= fromIndex)
					return bi;
				else if (eq >= fromIndex)
					return eq;
				return NOTHING_TO_SKIP;
			}
		},
		// 16 - skip first Condition with Operator.EQUALS (BINARY) or when
		//      BestValueSet (NOMINAL)occurs at last search-order position
		BIN__NOMVS__NONUMEQBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				// avoid IndexOutOfBoundsException() for EMPTY_LIST
				int size = theConditionList.size();
				if (size == 0)
					return NOTHING_TO_SKIP;

				Condition last = theConditionList.get(size-1);
				int vs = (last.getNominalValueSet() == null) ? NOTHING_TO_SKIP : size-1;
				int eq = NOTHING_TO_SKIP;

				// loop over all remaining Conditions to search for EQUALS
				for (int i = fromIndex, j = size; i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.BINARY, AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (EnumSet.of(AttributeType.BINARY).contains(c.getColumn().getType()));
						eq = i;
						break;
					}
				}

				// check that NOTHING_TO_SKIP is strictly smaller than fromIndex
				assert ((fromIndex >= 0) && (NOTHING_TO_SKIP < 0));

				if ((vs >= fromIndex) && (eq >= fromIndex))
					return Math.min(vs, eq);
				else if (vs >= fromIndex)
					return vs;
				else if (eq >= fromIndex)
					return eq;
				return NOTHING_TO_SKIP;
			}
		},
		// 17 - skip first Condition with Operator.EQUALS (BINARY|NUMERIC) or
		//      when BestValueSet (NOMINAL) occurs at last search-order position
		//      same loop as 16. except for different assert in the if-check
		BIN__NOMVS__NUMEQ
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				// avoid IndexOutOfBoundsException() for EMPTY_LIST
				int size = theConditionList.size();
				if (size == 0)
					return NOTHING_TO_SKIP;

				Condition last = theConditionList.get(size-1);
				int vs = (last.getNominalValueSet() == null) ? NOTHING_TO_SKIP : size-1;
				int eq = NOTHING_TO_SKIP;

				// loop over all remaining Conditions to search for EQUALS
				for (int i = fromIndex, j = size; i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.BINARY, AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (EnumSet.of(AttributeType.BINARY, AttributeType.NUMERIC).contains(c.getColumn().getType()));
						eq = i;
						break;
					}
				}

				// check that NOTHING_TO_SKIP is strictly smaller than fromIndex
				assert ((fromIndex >= 0) && (NOTHING_TO_SKIP < 0));

				if ((vs >= fromIndex) && (eq >= fromIndex))
					return Math.min(vs, eq);
				else if (vs >= fromIndex)
					return vs;
				else if (eq >= fromIndex)
					return eq;
				return NOTHING_TO_SKIP;
			}
		},
		// 18 - skip first Condition with Operator.EQUALS (BINARY) or when
		//      either a BestValueSet (NOMINAL) of BestInterval (NUMERIC) occurs
		//      at last search-order position
		//      similar to 16/17., but different asserts for mx and in if-check
		BIN__NOMVS__NUMBI
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				// avoid IndexOutOfBoundsException for EMPTY_LIST.get(0)
				int size = theConditionList.size();
				if (size == 0)
					return NOTHING_TO_SKIP;

				Condition last = theConditionList.get(size-1);
				int mx = ((last.getNominalValueSet() == null) && (last.getNumericInterval() == null)) ? NOTHING_TO_SKIP : size-1;
				assert (((mx != NOTHING_TO_SKIP) && EnumSet.of(AttributeType.NOMINAL, AttributeType.NUMERIC).contains(last.getColumn().getType())) ||
						((mx == NOTHING_TO_SKIP) && EnumSet.of(AttributeType.BINARY).contains(last.getColumn().getType())));
				int eq = NOTHING_TO_SKIP;

				// loop over all remaining Conditions to search for EQUALS
				for (int i = fromIndex, j = size; i < j; ++i)
				{
					Condition c = theConditionList.getCanonical(i);
					assert (EnumSet.of(AttributeType.BINARY, AttributeType.NOMINAL, AttributeType.NUMERIC).contains(c.getColumn().getType()));

					if (c.getOperator() == Operator.EQUALS)
					{
						assert (EnumSet.of(AttributeType.BINARY).contains(c.getColumn().getType()));
						eq = i;
						break;
					}
				}

				// check that NOTHING_TO_SKIP is strictly smaller than fromIndex
				assert ((fromIndex >= 0) && (NOTHING_TO_SKIP < 0));

				if ((mx >= fromIndex) && (eq >= fromIndex))
					return Math.min(mx, eq);
				else if (mx >= fromIndex)
					return mx;
				else if (eq >= fromIndex)
					return eq;
				return NOTHING_TO_SKIP;
			}
		},
		// 19 - debug option, never skip any (Column)ConditionBases
		NEVER_SKIP
		{
			@Override
			int nextSkip(ConditionList theConditionList, int fromIndex)
			{
				return NOTHING_TO_SKIP;
			}
		};

		// fromIndex = canonicalFromIndex, for SG.CanonicalConditions order
		abstract int nextSkip(ConditionList theConditionList, int fromIndex);

		// must be out of valid fromIndex range, and smaller than 0
		private static final int NOTHING_TO_SKIP = -1;

		static Fltr get(List<ColumnConditionBases> aColumnConditionBasesSet, SearchParameters theSearchParameters)
		{
			assert (!aColumnConditionBasesSet.isEmpty());

			if (!SET_SKIP_FILTER)
				return NEVER_SKIP;

			boolean dataHasBinary  = false;
			boolean dataHasNominal = false;
			boolean dataHasNumeric = false;

			for (ColumnConditionBases ccbs : aColumnConditionBasesSet)
			{
				// FIXME one time operation per mine(), but cumbersome, optimise
				ConditionBase   cb = ccbs.get(0);
				if (cb == null) cb = ccbs.get(1);
				if (cb == null) cb = ccbs.get(2);

				Column c = cb.getColumn();
				switch (c.getType())
				{
					// always (re)set, cheap enough
					case BINARY  : dataHasBinary  = true; break;
					case NOMINAL : dataHasNominal = true; break;
					case NUMERIC : dataHasNumeric = true; break;
					default      : throw new AssertionError("Fltr.get(): unknown Column.getType() " + c.getType());
				}

				if (dataHasBinary && dataHasNominal && dataHasNumeric)
					break;
			}

			if (!dataHasBinary && !dataHasNominal && !dataHasNumeric)
				throw new AssertionError("Fltr.get(): no Columns found of AttributeType BINARY/NOMINAL/NUMERIC");

			boolean useBestValueSets = theSearchParameters.getNominalSets();
			NumericStrategy ns       = theSearchParameters.getNumericStrategy();
			boolean useBestIntervals = (ns == NumericStrategy.NUMERIC_INTERVALS);
			boolean useNumericEquals = false;
			if (!useBestIntervals) // assumes mutual-exclusivity
			{
				// only ALL|BEST use Operator.EQUALS, BEST_BINS|BINS use BETWEEN
				// but all use NumericOperatorSetting ALL|EQ
				// so latter check is required
				boolean includesEquals = EnumSet.of(NumericOperatorSetting.ALL, NumericOperatorSetting.EQ).contains(theSearchParameters.getNumericOperatorSetting());
				boolean isAllBest      = EnumSet.of(NumericStrategy.NUMERIC_ALL, NumericStrategy.NUMERIC_BEST).contains(ns);
				useNumericEquals = (includesEquals && isAllBest);
			}

			// test mutual-exclusivity, can not both be true at the same time
			assert (!(useNumericEquals && useBestIntervals));

			// first 9 scenarios where data does not contain BINARY Columns
			// then  9 scenarios where data does     contain BINARY Columns
			if (!dataHasBinary)
			{
				// 3 possibilities for NOMINAL
				if (!dataHasNominal)
				{
					// 3 possibilities for NUMERIC
					if (!useNumericEquals && !useBestIntervals) return NOBIN__NONOM__NONUMEQBI;
					else if (useNumericEquals)                  return NOBIN__NONOM__NUMEQ;
					else if (useBestIntervals)                  return NOBIN__NONOM__NUMBI;
					else throw new AssertionError(); // logically impossible
				}
				else if (!useBestValueSets)
				{
					if (!useNumericEquals && !useBestIntervals) return NOBIN__NOMCL__NONUMEQBI;
					else if (useNumericEquals)                  return NOBIN__NOMCL__NUMEQ;
					else if (useBestIntervals)                  return NOBIN__NOMCL__NUMBI;
					else throw new AssertionError(); // logically impossible
				}
				else if (useBestValueSets)
				{
					if (!useNumericEquals && !useBestIntervals) return NOBIN__NOMVS__NONUMEQBI;
					else if (useNumericEquals)                  return NOBIN__NOMVS__NUMEQ;
					else if (useBestIntervals)                  return NOBIN__NOMVS__NUMBI;
					else throw new AssertionError(); // logically impossible
				}
				else
					throw new AssertionError("Fltr.get(): !dataHasBinary + unknown NOMINAL setting (dataHasNominal && !(!useBestValueSets || useBestValueSets))");
			}
			else
			{
				if (!dataHasNominal)
				{
					if (!useNumericEquals && !useBestIntervals) return BIN__NONOM__NONUMEQBI;
					else if (useNumericEquals)                  return BIN__NONOM__NUMEQ;
					else if (useBestIntervals)                  return BIN__NONOM__NUMBI;
					else throw new AssertionError(); // logically impossible
				}
				else if (!useBestValueSets)
				{
					if (!useNumericEquals && !useBestIntervals) return BIN__NOMCL__NONUMEQBI;
					else if (useNumericEquals)                  return BIN__NOMCL__NUMEQ;
					else if (useBestIntervals)                  return BIN__NOMCL__NUMBI;
					else throw new AssertionError(); // logically impossible
				}
				else if (useBestValueSets)
				{
					if (!useNumericEquals && !useBestIntervals) return BIN__NOMVS__NONUMEQBI;
					else if (useNumericEquals)                  return BIN__NOMVS__NUMEQ;
					else if (useBestIntervals)                  return BIN__NOMVS__NUMBI;
					else throw new AssertionError(); // logically impossible
				}
				else
					throw new AssertionError("Fltr.get(): dataHasBinary + unknown NOMINAL setting (dataHasNominal && !(!useBestValueSets || useBestValueSets))");
			}
		}
	}

	/* *********************************************************************
	 * REMOVE USELESS REFINEMENTS - MAY GO INTO DIFFERENT CLASS(ES)
	 **********************************************************************/
	private class Filter // not final for now, DebugFilter extends Filter
	{
		/*
		 * for Condition-free based checks
		 *
		 * EQUALS tests needed on
		 * NOMINAL: when Operator = EQUALS (thus not in set-valued mode)
		 * NUMERIC: when NominalOperatorSetting.includesEquals() == true
		 * BINARY : when Operator = EQUALS (always true in current code)
		 */
		private final boolean isNominalEqualsTestRequired;
		private final boolean isNumericEqualsTestRequired;
		private final boolean isBinaryEqualsTestRequired;
		private final boolean isDepthFirstNumericAllStrategy;
		/* for Condition based checks */
		private final boolean isBreadthFirstNumericAllStrategyNumericAllOperator;
		private final boolean isNumericNormalOrNumericAll;

		// assumes EQUALS is the only binary operator, see static below
		Filter(SearchParameters theSearchParameters)
		{
			// for EQUALS tests
			isNominalEqualsTestRequired = !theSearchParameters.getNominalSets();
			NumericOperatorSetting o = theSearchParameters.getNumericOperatorSetting();
			isNumericEqualsTestRequired = o.includesEquals();
			isBinaryEqualsTestRequired = true;
			SearchStrategy s = theSearchParameters.getSearchStrategy();
			NumericStrategy n = theSearchParameters.getNumericStrategy();
			// FIXME MM seems to be correct but needs more testing
			isDepthFirstNumericAllStrategy = false;
			// see isUseful(Refinement) comment below
//			isDepthFirstNumericAllStrategy =
//				s == SearchStrategy.DEPTH_FIRST &&
//				n == NumericStrategy.NUMERIC_ALL;

			// [ (C >= x)  ^ ... ^ (C <= x) ] -> [ (C = x)  ^ ... ]
			isBreadthFirstNumericAllStrategyNumericAllOperator =
				s == SearchStrategy.BREADTH_FIRST &&
				n == NumericStrategy.NUMERIC_ALL &&
				o == NumericOperatorSetting.ALL;

			// [ (C >= x)  ^ ... ^ (C <= x) ] ^ (C.op.v) is useless
			isNumericNormalOrNumericAll =
				o == NumericOperatorSetting.NORMAL ||
				o == NumericOperatorSetting.ALL;
		}

		// tests on existing ConditionList and 'value-free' Refinement
		boolean isUseful(Refinement theRefinement)
		{
			// TODO MM could create Refinement.isSelfReferencing()
			// to indicate that Refinement is about a Column that
			// already occurs in the ConditionList for its Subgroup
			//ConditionList aConditionList = theRefinement.getSubgroup().getConditions();
			ConditionList aConditionList = theRefinement.getSubgroup().getConditions();

			if (aConditionList.size() == 0)
				return true;

			ConditionBase b = theRefinement.getConditionBase();
			Column c = b.getColumn();

			// this is only for DEPTH_FIRST - Strategy NUMERIC_ALL
			// if c.index <= last.index, where last is the last
			// Column in theConditionList, evaluating the Refinement
			// is not needed (by construction of search algorithm)
			// reduces O(n^2) to O( (n*(n-1)) / 2)
			// TODO MM
			// could be in RefinementList, will decide later
			// false for most settings, skips fast
			if (isDepthFirstNumericAllStrategy)
			{
				Condition aLast = aConditionList.get(aConditionList.size()-1);
				int cmp = c.getIndex() - aLast.getColumn().getIndex();
				if (cmp < 0)
					return false;
				if (cmp == 0)
					if (b.getOperator().ordinal() <= aLast.getOperator().ordinal())
						return false;
				// else continue with other checks
			}

			// true most of the time, return fast
			if (!isOverlap(aConditionList, c))
				return true;

			// there is some overlap, extra tests are needed

			// EQUALS test
			switch (c.getType())
			{
				case NOMINAL :
				{
					if (isNominalEqualsTestRequired && hasOverridingEquals(aConditionList, c))
						return false;
					break;
				}
				case NUMERIC :
				{
					if (isNumericEqualsTestRequired && hasOverridingEquals(aConditionList, c))
						return false;
					break;
				}
				case BINARY :
				{
					if (isBinaryEqualsTestRequired && hasOverridingEquals(aConditionList, c))
						return false;
					break;
				}
				default :
					throw new AssertionError(c.getType());
			}

			// can not find a reason why Refinement should be denied
			return true;
		}

		// tests on existing ConditionList and Condition with value
		// NOTE requires Condition to be formed first, even when useless
		//boolean isUseful(ConditionList theConditionList, Condition theCondition)
		boolean isUseful(ConditionList theConditionList, Condition theCondition)
		{
			int aSize = theConditionList.size();
			if (aSize == 0)
				return true;

			final Column aColumn = theCondition.getColumn();

			// if there is no overlap, Refinement is useful
			if (!isOverlap(theConditionList, aColumn))
				return true;

			// if identical Condition exists, Refinement is useless
			if (contains(theConditionList, theCondition))
				return false;

			// specific tests for NUMERIC Columns
			// would hold for ORDINAL also, but is not implemented
			assert (aColumn.getType() != AttributeType.ORDINAL);
			if (aColumn.getType() != AttributeType.NUMERIC)
				return true;

			// NOTE for NumericOperatorSetting NUMERIC_ALL any
			// ConditionList with a Condition (C=x) would not allow
			// addition of any other Condition about C in the
			// 'value-free' pre-check
			// isUseful(ConditionList, Refinement)
			// and since there is overlap between the ConditionList
			// and the Condition
			// and the Column is NUMERIC
			// the ConditionList will be guaranteed to include
			// at least one LEQ/ GEQ about the relevant Column

			// specific case for BREADTH_FIRST
			// could hold for DEPTH_FIRST also, see implementation
			// != EQUALS assumes LEQ or GEQ
			if (isBreadthFirstNumericAllStrategyNumericAllOperator &&
				aSize > 0 &&
				theCondition.getOperator() != Operator.EQUALS)
			{
				if (createsRedundantEquals(theConditionList, theCondition))
					return false;
			}

			// relevant for NUMERIC_NORMAL and NUMERIC_ALL
			if (isNumericNormalOrNumericAll && aSize > 1)
				if (hasRelevantEqualsThroughLeqGeq(theConditionList, aColumn))
					return false;

			// can not find a reason why Refinement should be denied
			return true;
		}
	}
	static
	{
		// implemented as static block such that it is evaluated only
		// once at class initialisation
		// and so asserts are tested only once also
		// code is not executed when JVM is not started with -ea

		// if assert fails isBinaryEqualsTestRequired needs update
		assert(equalsIsOnlyBinaryOperator());
		// if assert fails *** needs update
		assert(numericAll());
		// if assert fails isNumericNormalOrNumericAll needs update
		assert(numericLeqGeq());
		// if assert fails isBreadthFirstNumericAllStrategyNumericAllOperator needs update
		assert(operatorOrder());
	}
	/* test assumption that EQUALS is only Operator for BINARY Columns */
	static final boolean equalsIsOnlyBinaryOperator()
	{
		Set<Operator> ops = Operator.getOperators(AttributeType.BINARY);
		return (ops.size() == 1) && (ops.contains(Operator.EQUALS));
	}
	/*
	 * test assumption that ALL is the only NumericOperatorSetting
	 * that may cause redundancy of the form:
	 * [ (C >= x)  ^ (C <= x) ^ ... ] -> which selects [ (C = x)  ^ ... ]
	 * for DEPTH_FIRST-NUMERIC_ALL (C = x) is created on depth=1
	 * and combined with every relevant other Condition
	 */
	private static final boolean numericAll()
	{
		for (NumericOperatorSetting s : NumericOperatorSetting.values())
		{
			EnumSet<Operator> set = s.getOperators();
			if (set.contains(Operator.LESS_THAN_OR_EQUAL) &&
				set.contains(Operator.GREATER_THAN_OR_EQUAL) &&
				set.contains(Operator.EQUALS))
			{
				if (s != NumericOperatorSetting.ALL)
					// assumption fails
					return false;
			}
		}

		// assumption holds
		return true;
	}

	/*
	 * test assumption that NORMAL and ALL are the only
	 * NumericOperatorSettings that contain both '>=' and '<='
	 * for any ConditionList of size > 2
	 * if it contains [ (C >= x)  ^ (C <= x) ^ ... ], there is no use in
	 * adding any other Condition involving C
	 */
	private static final boolean numericLeqGeq()
	{
		for (NumericOperatorSetting s : NumericOperatorSetting.values())
		{
			EnumSet<Operator> set = s.getOperators();
			if (set.contains(Operator.LESS_THAN_OR_EQUAL) &&
				set.contains(Operator.GREATER_THAN_OR_EQUAL))
			{
				if (s != NumericOperatorSetting.NORMAL &&
					s != NumericOperatorSetting.ALL)
					// assumption fails
					return false;
			}
		}

		// assumption holds
		return true;
	}
	/*
	 * test assumption that (C EQUALS v) Conditions are always created
	 * before (C >= v) and (C <= v)
	 * this is relevant for equals-creating ConditionLists like
	 * [ (C >= x)  ^ (C <= x) ^ ... ] for BREADTH_FIRST and in DEPTH_FIRST
	 */
	private static final boolean operatorOrder()
	{
		return (Operator.EQUALS.ordinal() < Operator.LESS_THAN_OR_EQUAL.ordinal()) &&
			(Operator.EQUALS.ordinal() < Operator.GREATER_THAN_OR_EQUAL.ordinal());
	}

	/*
	 * TODO MM
	 * could be ConditionList method, will decide later
	 * method returns false most of the time
	 */
	//private static final boolean isOverlap(ConditionList theConditionList, Column theColumn)
	private static final boolean isOverlap(ConditionList theConditionList, Column theColumn)
	{
		for (int i = 0, j = theConditionList.size(); i < j; ++i)
		{
			Condition c = theConditionList.get(i);
			if (c.getColumn() == theColumn)
				return true;
		}

		return false;
	}

	/*
	 * TODO MM
	 * could be ConditionList method, will decide later
	 *
	 * hasEquals() to be used in conjunction with hasRelevantEquals() if it
	 * returns true
	 * split into multiple methods to make test as light as possible
	 * as it will be performed on every RefinementList
	 * split may not improve speed though, will need to profile this
	 *
	 * If the current ConditionList  contains a Condition of the form
	 * (Column EQUALS value), and Refinement.ConditionBase.Column == Column,
	 * then non of the Refinements are useful.
	 * For Refinement (Column EQUALS v), where (v == value), the same
	 * Subgroup is created. When (v != value), the created Subgroup will be
	 * empty.
	 * This is true for NOMINAL, BINARY and NUMERIC Columns.
	 * This is true for BEAM and non-BEAM SearchSettings.
	 */
	//private static final boolean hasOverridingEquals(ConditionList theConditionList, Column theColumn)
	private static final boolean hasOverridingEquals(ConditionList theConditionList, Column theColumn)
	{
		return hasEquals(theConditionList) && hasRelevantEquals(theConditionList, theColumn);
	}
	//private static final boolean hasEquals(ConditionList theConditionList)
	private static final boolean hasEquals(ConditionList theConditionList)
	{
		for (int i = 0, j = theConditionList.size(); i < j; ++i)
		{
			Condition c = theConditionList.get(i);
			if (c.getOperator() == Operator.EQUALS)
				return true;
		}

		// no EQUALS
		return false;
	}
	//private static final boolean hasRelevantEquals(ConditionList theConditionList, Column theColumn)
	private static final boolean hasRelevantEquals(ConditionList theConditionList, Column theColumn)
	{
		for (int i = 0, j = theConditionList.size(); i < j; ++i)
		{
			Condition c = theConditionList.get(i);
			if (c.getOperator() == Operator.EQUALS && c.getColumn() == theColumn)
				return true;
		}

		// no EQUALS or not about the Refinement Column
		return false;
	}

	/*
	 * if an identical Condition exists in the ConditionList, the Refinement
	 * (Condition) is useless
	 * NOTE ConditionList extends ArrayList<Condition> so its contains()
	 * method would compare its elements using Condition.equals(), which is
	 * just Object.equals() as it is not overridden
	 * overriding would require also overriding hashcode() for correct use
	 * of a Condition in any Collection
	 * and per Map-contract equals() and compareTo() should be consistent
	 * current compareTo() implementation will do for now
	 *
	 * TODO MM solve this issue, it has been around forever
	 */
	//private static final boolean contains(ConditionList theConditionList, Condition theCondition)
	private static final boolean contains(ConditionList theConditionList, Condition theCondition)
	{
		for (int i = 0, j = theConditionList.size(); i < j; ++i)
		{
			Condition c = theConditionList.get(i);
			if (theCondition.compareTo(c) == 0)
				return true;
		}

		return false;
	}

	/*
	 * pre-conditions:
	 * -SearchStrategy = !beam (BREADTH_FIRST only at the moments see below)
	 * -NumericOperatorSetting contains (<=, >=, =) (only NUMERIC_ALL)
	 * -Condition.Column.AttributeType == AttributeType.NUMERIC
	 * -ConditionList.size() > 0
	 * -isOverlap(theConditionList, theColumn)
	 * -Condition.Operator has Equals-creation potential (is LEQ / GEQ)
	 *
	 * FIXME MM
	 * currently only for BREATDH_FIRST is this a useful check
	 * for any [ (C >= x)  ^ (C <= x) ^ ... ] at depth d
	 * an equivalent  [ (C = x)  ^ ... ] will have been created at depth d-1
	 *
	 * BREATDH_FIRST guarantees that all the Refinements for (C = x) will
	 * have been fully evaluated before moving to depth d
	 * so evaluating any [ (C >= x)  ^ (C <= x) ^ ... ] will be redundant
	 *
	 * DEPTH_FIRST could benefit from this also
	 * but the current order of Operators { LEQ, GEQ, EQUALS } in
	 * NumericOperatorSetting.NUMERIC_ALL does not allow this
	 * if EQUALS would be first one could guarantee that before any
	 * [ (C >= x)  ^ (C <= x) ^ ... ] will be evaluated
	 * every [ (C = x) ] ^ (...) will have been refined
	 * and the pre-phase of Filter.isUseful(ConditionList, Refinement)
	 * would then avoid creation of any [ (C = x)  ^ (C ... ) ^ ... ] anyway
	 * because of the EQUALS check
	 */
	//private static final boolean createsRedundantEquals(ConditionList theConditionList, Condition theCondition)
	private static final boolean createsRedundantEquals(ConditionList theConditionList, Condition theCondition)
	{
		Column aColumn = theCondition.getColumn();
		Operator anOperator = theCondition.getOperator();
		float aValue = theCondition.getNumericValue();

		assert (aColumn.getType() == AttributeType.NUMERIC);
		assert (theConditionList.size() > 0);
		assert (isOverlap(theConditionList, aColumn));
		assert (anOperator == Operator.LESS_THAN_OR_EQUAL ||
			anOperator == Operator.GREATER_THAN_OR_EQUAL);

		for (int i = 0, j = theConditionList.size(); i < j; ++i)
		{
			Condition c = theConditionList.get(i);
			if (c.getColumn() == aColumn &&
				c.getNumericValue() == aValue &&
				// harder test
				isLeqGeqPair(c.getOperator(), anOperator))
			{
				return true;
			}
		}

		return false;
	}

	private static final boolean isLeqGeqPair(Operator x, Operator y)
	{
		return ((x == Operator.LESS_THAN_OR_EQUAL && y == Operator.GREATER_THAN_OR_EQUAL) ||
			(x == Operator.GREATER_THAN_OR_EQUAL && y == Operator.LESS_THAN_OR_EQUAL));
	}

	/*
	 * pre-conditions:
	 * -NumericOperatorSetting has (<=, >=) (NUMERIC_NORMAL, NUMERIC_ALL)
	 * -Condition.Column.AttributeType == AttributeType.NUMERIC
	 * -ConditionList.size() > 1 (for at least one possible {<=, >=} pair)
	 * -isOverlap(theConditionList, theColumn)
	 *
	 * if the ConditionList selects a unique value through the combination
	 * of [ (C >= x)  ^ (C <= x) ^ ... ], then this case is similar to the
	 * isUseful(ConditionList, Refinement) EQUALS pre-check
	 * not any other Condition involving C will ever be useful
	 *
	 * FIXME MM this should be a PRE check, does not use new Condition.value
	 */
	//private static final boolean hasRelevantEqualsThroughLeqGeq(ConditionList theConditionList, Column theColumn)
	private static final boolean hasRelevantEqualsThroughLeqGeq(ConditionList theConditionList, Column theColumn)
	{
		// FIXME MM assertions go here

		for (int i = 0, j = theConditionList.size()-1; i < j; ++i)
		{
			Condition x = theConditionList.get(i);
			Column xc = x.getColumn();

			// existing Condition not about the relevant Column
			if (theColumn != xc)
				continue;

			Operator xo = x.getOperator();
			float xf = x.getNumericValue();

			// loop can be slightly optimised by marking used items
			// but ConditionLists are so small, it has no use
			for (int k = i+1; k != j; ++k)
			{
				Condition y = theConditionList.get(k);
				if (xc == y.getColumn() &&
					xf == y.getNumericValue() &&
					// harder test
					isLeqGeqPair(xo, y.getOperator()))
				{
					return true;
				}
			}
		}

		return false;
	}

	// TODO MM for debug only - wraps around Filter to print output
	private final class DebugFilter extends Filter
	{
		DebugFilter(SearchParameters theSearchParameters)
		{
			super(theSearchParameters);
		}

		@Override
		boolean isUseful(Refinement theRefinement)
		{
			boolean b = super.isUseful(theRefinement);
			//if (!b) // uncomment to only print refused Refinements
			print(b, "PRE\t", theRefinement.getSubgroup().getConditions(), theRefinement.getConditionBase().toString());
			return b;
		}

		@Override
		//boolean isUseful(ConditionList theConditionList, Condition theCondition)
		boolean isUseful(ConditionList theConditionList, Condition theCondition)
		{
			boolean b = super.isUseful(theConditionList, theCondition);
			//if (!b) // uncomment to only print refused Refinements
			print(b, "POST\t", theConditionList, theCondition.toString());
			return b;
		}

		// could be faster in calling method - but debug only anyway
		//private final void print(boolean useful, String phase, ConditionList conditionList, String value)
		private final void print(boolean useful, String phase, ConditionList conditionList, String value)
		{
			Log.logCommandLine(new StringBuilder(256)
						.append(useful ? "USEFUL\t" : "USELESS\t")
						.append(phase)
						.append(conditionList.toString())
						.append("\t")
						.append(value)
						.toString());
		}
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// start of obsolete code - some remains for debugging (for now)    /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////





	@Deprecated
	private void evaluateNumericRefinements(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		switch (itsSearchParameters.getNumericStrategy())
		{
			case NUMERIC_ALL :
				throw new AssertionError("SubgroupDiscovery.evaluateNumericRefinements() not available for NUMERIC_ALL");
			case NUMERIC_BEST : 					// NUMERIC_BEST is like NUMERIC_ALL, but uses only best scoring subgroup
				throw new AssertionError("SubgroupDiscovery.evaluateNumericRefinements() not available for NUMERIC_BEST");
			case NUMERIC_BINS :
				throw new AssertionError("SubgroupDiscovery.evaluateNumericRefinements() not available for NUMERIC_BINS");
			case NUMERIC_BEST_BINS :				// NUMERIC_BEST_BINS is like NUMERIC_BINS, but uses only best scoring subgroup
				throw new AssertionError("SubgroupDiscovery.evaluateNumericRefinements() not available for NUMERIC_BEST_BINS");
			case NUMERIC_INTERVALS :
				throw new AssertionError("SubgroupDiscovery.evaluateNumericRefinements() not available for NUMERIC_INTERVALS");
			case NUMERIC_VIKAMINE_CONSECUTIVE_ALL :
				throw new AssertionError("SubgroupDiscovery.evaluateNumericRefinements() not available for NUMERIC_VIKAMINE_CONSECUTIVE_ALL");
			case NUMERIC_VIKAMINE_CONSECUTIVE_BEST :		// NUMERIC_VIKAMINE_CONSECUTIVE_BEST is like NUMERIC_VIKAMINE_CONSECUTIVE_ALL, but uses only best scoring subgroup
				throw new AssertionError("SubgroupDiscovery.evaluateNumericRefinements() not available for NUMERIC_VIKAMINE_CONSECUTIVE_BEST");
			default :
				throw new AssertionError("SubgroupDiscovery.evaluateNumericRefinements(): unknown Numeric Strategy: " + itsSearchParameters.getNumericStrategy());
		}
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// numeric intervals - helper to differentiate between strategies   /////
	///// these NumericStrategies all use the same Operator and            /////
	///// NumericOperatorSetting                                           /////
	///// NUMERIC_VIKAMINE_CONSECUTIVE_ALL|BEST will be removed soon       /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	@Deprecated
	private final void evaluateNumericIntervals(Subgroup theParent, BitSet theParentMembers, ColumnConditionBasesNumericIntervals theColumnConditionBases)
	{
		assert (Operator.BETWEEN == theColumnConditionBases.get(0).getOperator());
		assert (NumericOperatorSetting.INTERVALS == itsSearchParameters.getNumericOperatorSetting());
		assert (EnumSet.of(NumericStrategy.NUMERIC_INTERVALS,
				NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_ALL,
				NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_BEST).contains(itsSearchParameters.getNumericStrategy()));

		if (NumericStrategy.NUMERIC_INTERVALS == itsSearchParameters.getNumericStrategy())
			evaluateNumericBestInterval(theParent, theParentMembers, theColumnConditionBases);
		else
			evaluateNumericConsecutiveIntervals(theParent, theParentMembers, theColumnConditionBases);
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// numeric consecutive intervals                                    /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	// NOTE
	// NUMERIC_VIKAMINE_CONSECUTIVE_ALL and NUMERIC_VIKAMINE_CONSECUTIVE_BEST
	// will be removed
	// the inconsistent behaviour of NumericOperatorSettings NUMERIC_EQ and
	// NUMERIC_ALL has been corrected
	// the original behaviour would test and return results for all numeric
	// bounds for =, and did not respect the ALL|BEST distinction
	// also, all bounds were tested, even when BEST_BINS|BINS was selected
	// this was especially inconsistent for NUMERIC_ALL (<=,=,>=), where both
	// <= and >= would perform binning and use a reduced number of bound, but in
	// the same setting = did not
	//
	// to get the behaviour of NUMERIC_VIKAMINE_CONSECUTIVE_ALL use BINS and =
	// for NUMERIC_VIKAMINE_CONSECUTIVE_BEST use BEST_BINS and =
	//
//	private void evaluateNumericConsecutiveIntervals(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	@Deprecated
	private final void evaluateNumericConsecutiveIntervals(Subgroup theParent, BitSet theParentMembers, ColumnConditionBasesNumericIntervals theColumnConditionBases)
	{
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		assert (aNumericStrategy == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_ALL || aNumericStrategy == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_BEST);

		////////////////////////////////////////////////////////////////////////
		boolean isFilterNull = (itsFilter == null);
		int aParentCoverage = theParent.getCoverage();
		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theColumnConditionBases.get(0);
		Column aColumn = aConditionBase.getColumn();
		ConditionList aParentConditions = (isFilterNull ? null : theParent.getConditions());
		// might require update when more strategies are added
		boolean isAllStrategy = (aNumericStrategy == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_ALL);
		Subgroup aBestSubgroup = null;
		////////////////////////////////////////////////////////////////////////

		int aNrSplitPoints = itsSearchParameters.getNrBins() - 1;
		// useless, Column.getSplitPointsBounded() would return an empty array
		if (aNrSplitPoints <= 0)
			return;
		//SortedSet<Interval> anIntervals = aConditionBase.getColumn().getUniqueSplitPointsBounded(theParentMembers, aNrSplitPoints);
		SortedMap<Interval, Integer> anIntervals = aColumn.getUniqueSplitPointsBounded(theParentMembers, aParentCoverage, aNrSplitPoints);
		// can happen for aNrSplitPoints >= 1: the underlying algorithm had bugs
		if (anIntervals.size() <= 1)
			return;

		//for (Interval anInterval : anIntervals)
		for (Entry<Interval, Integer> e : anIntervals.entrySet())
		{
			if (isTimeToStop())
				break;

			int aCount = e.getValue();

			if (aCount < itsMinimumCoverage)
				continue;

			if (aCount == aParentCoverage)
				break;

			Condition aCondition = new Condition(aConditionBase, e.getKey());

			if (!isFilterNull && !itsFilter.isUseful(aParentConditions, aCondition))
				continue;

			Subgroup aChild = theParent.getRefinedSubgroup(aCondition);

			if (isAllStrategy)
			{
				//addToBuffer(aNewSubgroup);
				checkAndLog(aChild, aParentCoverage);
			}
			else
			{
				// more clear than using else-if
				if (isValidAndBest(aChild, aParentCoverage, aBestSubgroup))
					aBestSubgroup = aChild;
			}
		}

		if (!isAllStrategy && (aBestSubgroup != null))
			bestAdd(aBestSubgroup, aParentCoverage);
	}


	// XXX replaced by checkForBest()
	@Deprecated
	private final boolean isValidAndBest(Subgroup theNewSubgroup, int theOldCoverage, Subgroup theBestSubgroup)
	{
		int aNewCoverage = theNewSubgroup.getCoverage();

		// FIXME MM the first and second check should be made obsolete
		// numericConsecutive bins should check this, then they can be removed
		//
		// FIXME this method is not consistent with its non-best equivalent
		// the <= itsMaximumCoverage check does not occur for all-strategies
		// the check below should be considered a bug, but resolving it makes
		// all historic results obsolete
		// the solution should distinguish between the best subgroup for the
		// current Refinement, and use that for the current level (ResultSet)
		// but for the next level (CandidateQueue) another subgroup might be
		// appropriate, as it could score better that the best subgroup, but be
		// the big to be considered to be included in the ResultSet
		//
		// is theNewSubgroup valid
		if (aNewCoverage >= itsMinimumCoverage && aNewCoverage < theOldCoverage && aNewCoverage <= itsMaximumCoverage)
		{
			// FIXME do not call evaluateCandidate() when it is computed already
			float aQuality = evaluateCandidate(theNewSubgroup);

			// is theNewSubgroup better than theBestSubgroup
			if ((theBestSubgroup == null) || (aQuality > theBestSubgroup.getMeasureValue()))
			{
				theNewSubgroup.setMeasureValue(aQuality);
				return true;
			}
		}

		return false;
	}

	// this method is currently unnecessary, but addToBuffer will return one day
	// XXX replaced by checkAndLogBest()
	@Deprecated
	private final void bestAdd(Subgroup theBestChild, int theParentCoverage)
	{
		assert (theBestChild != null);

		//addToBuffer(aBestSubgroup);
		checkAndLog(theBestChild, theParentCoverage);
	}

	// XXX leave it in for now
	@SuppressWarnings("unused")
	@Deprecated
	private final void debugBest(Subgroup theParent, Subgroup theBestSubgroup, BestSubgroupsForCandidateSetAndResultSet theBestSubgroups)
	{
		if (!DEBUG_PRINTS_FOR_BEST)
			return;

		Subgroup c = theBestSubgroups.itsBestForCandidateSet;
		Subgroup r = theBestSubgroups.itsBestForResultSet;
		assert ((theBestSubgroup == null) || (theBestSubgroup.compareTo(r) == 0));

		boolean cIsNull    = (c == null);
		boolean rIsNull    = (r == null);
		boolean isMaxDepth = (theParent.getDepth() == itsSearchParameters.getSearchDepth()-1);
		assert (cIsNull || !isMaxDepth);

		itsBestPairsCount.incrementAndGet();
		if (!isMaxDepth && (c != r))
		{
			itsBestPairsDiffer.incrementAndGet();
			if (DEBUG_PRINTS_FOR_BEST)
				Log.logCommandLine(String.format("NOTE TWO DIFFERENT BEST SUBGROUPS:%nfor CandidateSet: size=%d %s%nfor ResultSet   : size=%d %s%n", (cIsNull ? 0 : c.getCoverage()), (cIsNull ? "" : c), (rIsNull ? 0 : r.getCoverage()), (rIsNull ? "" : r)));
		}
	}
}
