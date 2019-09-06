package nl.liacs.subdisc;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import javax.swing.*;

import nl.liacs.subdisc.Column.DomainMapNumeric;
import nl.liacs.subdisc.Column.ValueInfo;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBases;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBasesBinary;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBasesNominalElementOf;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBasesNominalEquals;
import nl.liacs.subdisc.ColumnConditionBasesBuilder.ColumnConditionBasesNumeric;
import nl.liacs.subdisc.ConditionListBuilder.ConditionListA;
import nl.liacs.subdisc.ConvexHull.HullPoint;
import nl.liacs.subdisc.gui.*;

public class SubgroupDiscovery
{
	// log slows down mining a lot, but leave NO_CANDIDATE_LOG at false in git
	private static final boolean NO_CANDIDATE_LOG = false;
	private static final boolean ENABLE_POC_SETTINGS = true;
	private static final boolean DEBUG_POC_BINS = true;
	// leave TEMPORARY_CODE at false in git
	// when true, creates PMF instead of PDF in single numeric H^2 setting
	static boolean TEMPORARY_CODE = false;
	static int TEMPORARY_CODE_NR_SPLIT_POINTS = -1;
	static boolean TEMPORARY_CODE_USE_EQUAL_WIDTH = false;

	private final SearchParameters itsSearchParameters;
	private final Table itsTable;
	private final int itsNrRows;          // itsTable.getNrRows()
	private final int itsMinimumCoverage; // itsSearchParameters.getMinimumCoverage();
	private final int itsMaximumCoverage; // itsNrRows * itsSearchParameters.getMaximumCoverageFraction();

	private final QualityMeasure itsQualityMeasure;
	private final float itsQualityMeasureMinimum;   // itsSearchParameters.getQualityMeasureMinimum();
	private boolean ignoreQualityMinimum = false;   //used for swap-randomization purposes, and to get random qualities

	// target concept type-specific information, including base models
	private BitSet itsBinaryTarget;                 // SINGLE_NOMINAL
	private Column itsTargetRankings;               // SINGLE_NOMINAL (label ranking)
	private Column itsNumericTarget;                // SINGLE_NUMERIC
	private Column itsPrimaryColumn;                // DOUBLE_CORRELATION / DOUBLE_REGRESSION / SCAPE
	private Column itsSecondaryColumn;              // DOUBLE_CORRELATION / DOUBLE_REGRESSION / SCAPE
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

//	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, JFrame theMainWindow)
//	{
//// GENERIC
//		super(theSearchParameters);
//		itsTable = theTable;
//		itsNrRows = itsTable.getNrRows();
//		itsMainWindow = theMainWindow;
//		TargetConcept aTC = itsSearchParameters.getTargetConcept();
//		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
//		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());
//
//// TYPE SPECIFICS
//// NOMINAL
//// NUMERIC
////		itsNumericTarget = aTC.getPrimaryTarget();
//// DOUBLE CORRELATION
//// DOUBLE REGRESSION
//// SCAPE
////		itsPrimaryColumn = aTC.getPrimaryTarget();
////		itsSecondaryColumn = aTC.getSecondaryTarget();
//// MULTI_LABEL
////		itsBinaryTable = new BinaryTable(itsTable, itsTargets);
////		Bayesian aBayesian = new Bayesian(itsBinaryTable, itsTargets);
//// LABEL RANKING
////		itsTargetRankings = aTC.getPrimaryTarget();
////		LabelRanking aLR = itsTargetRankings.getAverageRanking(null); //average ranking over entire dataset
////		LabelRankingMatrix aLRM = itsTargetRankings.getAverageRankingMatrix(null); //average ranking over entire dataset
//// MULTI_NUMERIC
////
//
//// GENERIC
////		itsQualityMeasure
////		itsQualityMeasureMinimum
////		itsResult = new SubgroupSet()
//	}

	//SINGLE_NOMINAL
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, int theNrPositive, JFrame theMainWindow)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsNrRows = itsTable.getNrRows();
		itsMainWindow = theMainWindow;
		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());
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

		BitSet aBitSet = new BitSet(itsNrRows);
		aBitSet.set(0, itsNrRows);
		itsBinaryTarget = aTC.getPrimaryTarget().evaluate(aBitSet, aCondition);
		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsNrRows, itsBinaryTarget);
	}

	//SINGLE_NUMERIC, float > signature differs from multi-label constructor
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, float theAverage, JFrame theMainWindow)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsNrRows = itsTable.getNrRows();
		itsMainWindow = theMainWindow;
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());
		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		itsNumericTarget = aTC.getPrimaryTarget();

		BitSet aBitSet = new BitSet(itsNrRows);
		aBitSet.set(0, itsNrRows);
		Statistics aStatistics = itsNumericTarget.getStatistics(aBitSet, false, true); // no median, yes complement
		ProbabilityDensityFunction aPDF;
if (!TEMPORARY_CODE)
{
		// DEBUG
		if (!ProbabilityDensityFunction.USE_ProbabilityDensityFunction2)
			aPDF = new ProbabilityDensityFunction(itsNumericTarget);
		else
			aPDF = new ProbabilityDensityFunction2(itsNumericTarget);
		aPDF.smooth();
}
else
{
aPDF = new ProbabilityMassFunction_ND(itsNumericTarget, TEMPORARY_CODE_NR_SPLIT_POINTS, TEMPORARY_CODE_USE_EQUAL_WIDTH);
}

		itsQualityMeasure = new QualityMeasure(
			itsSearchParameters.getQualityMeasure(),
			itsNrRows,
			aStatistics.getSubgroupSum(),
			aStatistics.getSubgroupSumSquaredDeviations(),
			aPDF);
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsNrRows);
	}

	//DOUBLE_CORRELATION and DOUBLE_REGRESSION
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, boolean isRegression, JFrame theMainWindow)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsNrRows = itsTable.getNrRows();
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
//			Log.logRefinement("Bound graph for "+itsTable.getName());
//			Log.logRefinement("SubgroupSize,AvgRegressionTime,AvgCook,AvgBoundSeven,AvgBoundSix,AvgBoundFive,AvgBoundFour,CookComputable,BoundSevenComputable,BoundSixComputable,BoundFiveComputable,BoundFourComputable");
		}

		TargetConcept aTC = itsSearchParameters.getTargetConcept();
// TODO for stable jar, initiated here, SubgroupDiscovery revision 893 moved this to else below
		itsPrimaryColumn = aTC.getPrimaryTarget();
		itsSecondaryColumn = aTC.getSecondaryTarget();
		if (isRegression)
		{
// TODO RegressionMeasure revision 851 introduces the new RegressionMeasure constructor below (not mentioned in log)
			itsBaseRM = new RegressionMeasure(itsSearchParameters.getQualityMeasure(), itsPrimaryColumn, itsSecondaryColumn);
// TODO for stable jar, disabled, causes compile errors, reinstate later
//			itsBaseRM = new RegressionMeasure(itsSearchParameters.getQualityMeasure(), aTC);

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

// temp for testing
			//generateBoundGraph();
		}
		else
		{
// TODO for stable jar, disabled, initiated above, reinstate later as per SubgroupDiscovery revision 893
//			itsPrimaryColumn = aTC.getPrimaryTarget();
//			itsSecondaryColumn = aTC.getSecondaryTarget();
			itsBaseCM = new CorrelationMeasure(itsSearchParameters.getQualityMeasure(), itsPrimaryColumn, itsSecondaryColumn);
		}

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsNrRows);
	}

	//SCAPE
	public SubgroupDiscovery(JFrame theMainWindow, SearchParameters theSearchParameters, Table theTable)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsNrRows = itsTable.getNrRows();
		itsMainWindow = theMainWindow;
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());

		//compute base model
		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		// TODO for stable jar, initiated here, SubgroupDiscovery revision 893 moved this to else below
		itsPrimaryColumn = aTC.getPrimaryTarget();
		itsSecondaryColumn = aTC.getSecondaryTarget();
		// original code hack as default constructor would not work
		//itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsNrRows, itsPrimaryColumn.getBinaries().cardinality(), itsPrimaryColumn, itsSecondaryColumn, itsSearchParameters.getOverallRankingLoss());
		// unable to reproduce error MM
		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsNrRows, itsPrimaryColumn.getBinaries().cardinality(), itsPrimaryColumn, itsSecondaryColumn);
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsNrRows);
	}

	//MULTI_LABEL
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, JFrame theMainWindow)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsNrRows = itsTable.getNrRows();
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

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsNrRows);
	}

	//LABEL_RANKING
	public SubgroupDiscovery(SearchParameters theSearchParameters, JFrame theMainWindow, Table theTable)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsNrRows = itsTable.getNrRows();
		itsMainWindow = theMainWindow;
		TargetConcept aTC = itsSearchParameters.getTargetConcept();

		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());

		itsTargetRankings = aTC.getPrimaryTarget();
		LabelRanking aLR = itsTargetRankings.getAverageRanking(null); //average ranking over entire dataset
		LabelRankingMatrix aLRM = itsTargetRankings.getAverageRankingMatrix(null); //average ranking over entire dataset
		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsNrRows, aLR, aLRM);
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsNrRows);
	}

	// MULTI_NUMERIC
	public SubgroupDiscovery(Table theTable, JFrame theMainWindow, SearchParameters theSearchParameters)
	{
		itsSearchParameters = theSearchParameters;
		itsTable = theTable;
		itsNrRows = itsTable.getNrRows();
		itsMainWindow = theMainWindow;
		itsMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		itsMaximumCoverage = (int) (itsNrRows * itsSearchParameters.getMaximumCoverageFraction());

		// compute base model
		itsTargets = itsSearchParameters.getTargetConcept().getMultiTargets();
		itsPDF_ND = new ProbabilityDensityFunction_ND(itsTargets.toArray(new Column[0]));

		itsQualityMeasure = null;
		itsQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsNrRows);
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

		postMining(System.currentTimeMillis() - theBeginTime);
	}

	private List<ColumnConditionBases> itsColumnConditionBasesSet;
	/* use theNrThreads < 0 to run old mine(theBeginTime) */
	public void mine(long theBeginTime, int theNrThreads)
	{
		// not a member field, final and unmodifiable, good for concurrency
		final ConditionBaseSet aConditions = preMining(theBeginTime, theNrThreads);

		// FIXME for testing, obtain another set again, ignore preMining version
		// make it a member field, so Test signature need not be changed
		itsColumnConditionBasesSet = ColumnConditionBasesBuilder.FACTORY.getColumnConditionBasesSet(itsTable, itsSearchParameters);

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
				// take off first Candidate from Queue
				if (itsCandidateQueue.currentLevelQueueSize() > 0)
					aCandidate = itsCandidateQueue.removeFirst();
				// obviously (currentLevelQueueSize <= 0)
				// take solely when this is only active thread
				else if ((aTotalSize > 0) && alone)
					aCandidate = itsCandidateQueue.removeFirst();
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

				es.execute(new Test(aSubgroup, s, aConditions));
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

		postMining(System.currentTimeMillis() - theBeginTime);
	}

	private final ConditionBaseSet preMining(long theBeginTime, int theNrThreads)
	{
		// setup
		// TODO this should not be here but in the SINGLE_NOMINAL constructor
		loadExternalKnowledge();

		// not in Constructor, Table / SearchParameters may change
		// TODO not sure if this is still true
		final ConditionBaseSet aConditions = new ConditionBaseSet(itsTable, itsSearchParameters);
		logExperimentSettings(aConditions);

		// make subgroup to start with, containing all elements
		Subgroup aStart = new Subgroup(ConditionListBuilder.emptyList(), itsResult.getAllDataBitSetClone(), itsResult);

		// set number of true positives for dataset
		if (isPOCSetting())
			aStart.setTertiaryStatistic(itsQualityMeasure.getNrPositives());

		if ((itsSearchParameters.getBeamSeed() == null) || (theNrThreads < 0))
			itsCandidateQueue = new CandidateQueue(itsSearchParameters, new Candidate(aStart));
		else
			itsCandidateQueue = getCandidateQueueFromBeamSeed();

		prepareData(isPOCSetting(), itsBinaryTarget, itsTable.getColumns());

		long anEndTime = theBeginTime + (long) (((double) itsSearchParameters.getMaximumTime()) * 60.0 * 1000.0);
		itsEndTime = (anEndTime <= theBeginTime) ? Long.MAX_VALUE : anEndTime;

		return aConditions;
	}

	private final void loadExternalKnowledge()
	{
		QM aQualityMeasure = itsSearchParameters.getQualityMeasure();

		// fill the conditionList of local and global knowledge, Rob
		if (aQualityMeasure == QM.PROP_SCORE_WRACC || aQualityMeasure == QM.PROP_SCORE_RATIO)
		{
			ExternalKnowledgeFileLoader extKnowledge;
			extKnowledge = new ExternalKnowledgeFileLoader(new File("").getAbsolutePath());
			extKnowledge.createConditionListLocal(itsTable);
			extKnowledge.createConditionListGlobal(itsTable);
			itsLocalKnowledge = new LocalKnowledge(extKnowledge.getLocal(), itsBinaryTarget);
			itsGlobalKnowledge = new GlobalKnowledge(extKnowledge.getGlobal(), itsBinaryTarget);
		}
	}

	private void logExperimentSettings(ConditionBaseSet theConditionBaseSet)
	{
		Log.logCommandLine("");
		Log.logCommandLine(itsSearchParameters.getTargetConcept().toString());
		Log.logCommandLine(itsSearchParameters.toString());
		Log.logCommandLine(theConditionBaseSet.toString());
	}

	private static final void prepareData(boolean isPOCSetting, BitSet theBinaryTarget, List<Column> theColumns)
	{
		// just wraps itsDistinctValues in a shared unmodifiable Collection
		for (Column c : theColumns)
			if (c.getType() == AttributeType.NOMINAL)
				c.buildSharedDomain();

		// some settings should always build a sorted domain for all TargetTypes

		// currently numeric with EQUALS is not a valid setting
		if (!isPOCSetting)
			return;

		Timer aTotal = new Timer();

		for (Column c : theColumns)
		{
			if (c.getType() != AttributeType.NUMERIC)
				continue;

			Log.logCommandLine(c.getName());
			Timer t = new Timer();
			c.buildSorted(theBinaryTarget); // build SORTED and SORT_INDEX
			Log.logCommandLine("sorted domain build: " + t.getElapsedTimeString());
		}

		Log.logCommandLine("total sorting time : " + aTotal.getElapsedTimeString());
	}

	// direct computation is relevant only for a SINGLE_NOMINAL target as it
	// relates to tracking the true positive counts
	// evaluateBinary(): for BINARY description Attributes use directComputation
	//                   for other types this is not possible
	// evaluateNominalBestValueSet(): description Attribute is always NOMINAL
	//                                and directComputation is possible
	// numericIntervals(): see evaluateNominalBestValueSet()
	//
	// for PROP_SCORE_WRACC/PROP_SCORE_RATIO directComputation is not possible
	private final boolean isDirectSetting()
	{
		EnumSet<QM> anInvalid = EnumSet.of(QM.PROP_SCORE_WRACC, QM.PROP_SCORE_RATIO);

		// checking this all the time is a bit wasteful, but fine for now
		SearchParameters s = itsSearchParameters;
		return ((s.getTargetType() == TargetType.SINGLE_NOMINAL) && !anInvalid.contains(s.getQualityMeasure()));
	}

	// POCSetting relates to model updates for numeric half-intervals, and it
	// requires sorted numeric domains to be available for all NUMERIC Columns
	//
	// currently only an implementation for SINGLE_NOMINAL is available, it
	// requires the number of TruePositives to be set for the parent, most
	// notably for the initial Start Subgroup (preMining should take care of it)
	//
	// future updates will add other TargetTypes
	//
	// NOTE
	// aValid includes NUMERIC_INTERVALS, as it requires sorted NUMERIC domains
	// it does not use model updates (at least not like the half-intervals)
	private final boolean isPOCSetting()
	{
		EnumSet<NumericStrategy> aValid = EnumSet.of(
											NumericStrategy.NUMERIC_ALL,
											NumericStrategy.NUMERIC_BEST,
											NumericStrategy.NUMERIC_BEST_BINS,
											NumericStrategy.NUMERIC_BINS,
											NumericStrategy.NUMERIC_INTERVALS);

		EnumSet<QM> anInvalid = EnumSet.of(QM.PROP_SCORE_WRACC, QM.PROP_SCORE_RATIO);

		SearchParameters s = itsSearchParameters;
		return (aValid.contains(s.getNumericStrategy()) &&
				((s.getTargetType() == TargetType.SINGLE_NOMINAL) && !anInvalid.contains(s.getQualityMeasure())) &&
				ENABLE_POC_SETTINGS);
	}

	private static final void deleteSortData(List<Column> theColumns)
	{
		for (Column c : theColumns)
		{
			c.SORTED = null;
			c.SORT_INDEX = null;
		}
	}

	private final CandidateQueue getCandidateQueueFromBeamSeed()
	{
		//List<ConditionList> aBeamSeed = itsSearchParameters.getBeamSeed();
		List<ConditionListA> aBeamSeed = itsSearchParameters.getBeamSeed();
		//ConditionList aFirstConditionList = aBeamSeed.get(0);
		ConditionListA aFirstConditionList = aBeamSeed.get(0);
		//TODO there may be no members, in which case the following statement crashes
		BitSet aFirstMembers = itsTable.evaluate(aFirstConditionList);
		Subgroup aFirstSubgroup = new Subgroup(aFirstConditionList, aFirstMembers, itsResult);
		CandidateQueue aSeededCandidateQueue = new CandidateQueue(itsSearchParameters, new Candidate(aFirstSubgroup));
		// no useful Refinement from this can result
		if (aFirstSubgroup.getCoverage() <= 1)
			aSeededCandidateQueue.removeFirst();

		int aNrEmptySeeds = 0;
		//for (ConditionList aConditionList : aBeamSeed)
		for (ConditionListA aConditionList : aBeamSeed)
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
	 * After Test is done, its releases its semaphore, so ExecutorService can
	 * start a new Test.
	 */
	private class Test implements Runnable
	{
		private final Subgroup itsSubgroup;
		private final Semaphore itsSemaphore;
		private final ConditionBaseSet itsConditionBaseSet;

		public Test(Subgroup theSubgroup, Semaphore theSemaphore, ConditionBaseSet theConditionBaseSet)
		{
			itsSubgroup = theSubgroup;
			itsSemaphore = theSemaphore;
			itsConditionBaseSet = theConditionBaseSet;
		}

		//@Override
		public void runx()
		{
			// Subgroup.getMembers() creates expensive clone, reuse
			final BitSet aMembers = itsSubgroup.getMembers();
			final int aCoverage = itsSubgroup.getCoverage();
			assert (aMembers.cardinality() == aCoverage);

			final RefinementList aRefinementList = new RefinementList(itsSubgroup, itsConditionBaseSet);

			for (int i = 0, j = aRefinementList.size(); i < j && !isTimeToStop(); i++)
			{
				final Refinement aRefinement = aRefinementList.get(i);

				ConditionBase aConditionBase = aRefinement.getConditionBase();
				Column aColumn = aConditionBase.getColumn();

				// no useful Refinements are possible
				if (aColumn.getCardinality() <= 1)
					continue;

				// if refinement is (num_attr = value) then treat it as nominal
				if (aColumn.getType() == AttributeType.NUMERIC && aConditionBase.getOperator() != Operator.EQUALS)
					evaluateNumericRefinements(aMembers, aCoverage, aRefinement);
				else
					evaluateNominalBinaryRefinements(aMembers, aCoverage, aRefinement);
			}

			itsSemaphore.release();
		}

		@Override
		public void run()
		{
			// Subgroup.getMembers() creates expensive clone, reuse
			final BitSet aMembers = itsSubgroup.getMembers();
			final int aCoverage = itsSubgroup.getCoverage();
			assert (aMembers.cardinality() == aCoverage);

			for (int i = 0, j = itsColumnConditionBasesSet.size(); i < j && !isTimeToStop(); ++i)
			{
				ColumnConditionBases c = itsColumnConditionBasesSet.get(i);

				if (c instanceof ColumnConditionBasesBinary)
					evaluateBinary(itsSubgroup, aMembers, (ColumnConditionBasesBinary) c);
				else if (c instanceof ColumnConditionBasesNominalElementOf)
					evaluateNominalElementOf(itsSubgroup, aMembers, (ColumnConditionBasesNominalElementOf) c);
				else if (c instanceof ColumnConditionBasesNominalEquals)
					evaluateNominalBinaryRefinements(aMembers, aCoverage, new Refinement(c.get(0), itsSubgroup));
				else if (c instanceof ColumnConditionBasesNumeric)
				{
					for (int k = 0; k < c.size(); ++k)
						evaluateNumericRefinements(aMembers, aCoverage, new Refinement(c.get(k), itsSubgroup));
				}
				else
					throw new AssertionError();
			}

			itsSemaphore.release();
		}
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// when done                                                        /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	private void postMining(long theElapsedTime)
	{
		Process.echoMiningEnd(theElapsedTime, getNumberOfSubgroups());

		long aNrCandidates = itsCandidateCount.get();
		setTitle(itsMainWindow, theElapsedTime, aNrCandidates);

		deleteSortData(itsTable.getColumns());

		// postProcessCook() output is supposed to go in between
		Log.logCommandLine("number of candidates: " + aNrCandidates);
		postProcessCook();
		Log.logCommandLine("number of subgroups: " + getNumberOfSubgroups());

		// assign 1 to n to subgroups, for future reference in subsets
		itsResult.setIDs();

		postProcessMultiLabelAutoRun(); // IDs must be set first,  might set new
		postProcessCBBS();
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

	private void postProcessCBBS()
	{
		// just for cover-based beam search post selection, see note at
		// SubgroupSet.postProcess(), all itsResults will remain in memory
		SubgroupSet aSet = itsResult.postProcess(itsSearchParameters.getSearchStrategy());

		// FIXME MM hack to deal with strange postProcess implementation
		if (itsResult != aSet)
		{
			// no reassign, we want itsResult to be final
			itsResult.clear();
			itsResult.addAll(aSet);
			// in COVER_BASED_BEAM_SELECTION, subgroups may have been removed
			itsResult.setIDs();
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
		SubgroupSet aNewSubgroupSet = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsNrRows);
		// most methods of SubgroupSet are not thread save, but this is
		// no problem for this method as it is run by a single thread
		// however all itsResult sets, of all refinement depths,  will
		// be kept in memory
		// see comment in SubgroupSet.postProcess()
		for (Subgroup s : itsResult)
		{
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
	private void evaluateNominalBinaryRefinements(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		ConditionBase aConditionBase = theRefinement.getConditionBase();

		// split code paths for ValueSet/class labels (nominal/numeric/binary)
		if (aConditionBase.getOperator() == Operator.ELEMENT_OF)
		{
			assert (false); // code should never get here

			// currently BestValueSet implies the target type is SINGLE_NOMINAL
			assert (itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL);
			assert (itsSearchParameters.getNominalSets());

			//evaluateNominalElementOf(theParentMembers, theParentCoverage, theRefinement);
			return;
		}

		assert (aConditionBase.getOperator() == Operator.EQUALS);

		// FIXME: NUMERIC Refinement should not be done here
		switch (aConditionBase.getColumn().getType())
		{
			case NOMINAL : evaluateNominalRefinements(theParentMembers, theParentCoverage, theRefinement); break;
			case NUMERIC : evaluateNumericEqualsRefinements(theParentMembers, theParentCoverage, theRefinement); break;
			case ORDINAL : throw new AssertionError(AttributeType.ORDINAL);
			//case BINARY  : evaluateBinaryRefinements(theParentMembers, theParentCoverage, theRefinement); break;
			default      : throw new AssertionError(aConditionBase.getColumn().getType());
		}
	}

	// XXX (c = false) is checked first, (c = true) is conditionally, it depends
	//       on data and search characteristics  whether this is the best order
//	private final void evaluateBinaryRefinements(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	private final void evaluateBinary(Subgroup theParent, BitSet theParentMembers, ColumnConditionBasesBinary theColumnConditionBases)
	{
		assert (theColumnConditionBases.size() == 1);
		assert (theColumnConditionBases.get(0).getOperator() == Operator.EQUALS);

		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theColumnConditionBases.get(0);
		Column aColumn = aConditionBase.getColumn();
		int aParentCoverage = theParent.getCoverage();

		BitSet aChildMembers = aColumn.evaluateBinary(theParentMembers, false);
		int aChildCoverage = (aChildMembers == null ? 0 : aChildMembers.cardinality());

		// ignore both f and t
		if ((aChildCoverage == 0) || (aChildCoverage == aParentCoverage))
			return;

		int aNrTruePositives = INVALID_NR_TRUE_POSITIVES;

		// check for (aColumn = false)
		if (aChildCoverage >= itsMinimumCoverage)
			aNrTruePositives = evaluateBinaryRefinementsHelper(theParent, new Condition(aConditionBase, false), aChildMembers, aChildCoverage);

		// binary check is fast, but for some models evaluateCandidate() is not
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
				aNrTruePositives = (((int) theParent.getTertiaryStatistic()) - aNrTruePositives);
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
		boolean isDirectSetting = isDirectSetting();
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
		float q = (float) theQualityScore;
		double s = ((double) theNrTruePositives) / theChildCoverage;
		double t = ((double) theNrTruePositives);

		return theParent.getRefinedSubgroup(theAddedCondition, q, s, t, theChildCoverage);
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// nominal                                                          /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	private final void evaluateNominalRefinements(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		boolean isFilterNull = (itsFilter == null);
		Subgroup aParent = theRefinement.getSubgroup();
		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		Column aColumn = aConditionBase.getColumn();
		ConditionListA aParentConditions = (isFilterNull ? null : aParent.getConditions());

		int[] aCounts = aColumn.getUniqueNominalDomainCounts(theParentMembers, theParentCoverage);

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

			-- j;

			if (aCount < itsMinimumCoverage)
				continue;

			if (aCount == theParentCoverage)
				break;

			Condition aCondition = new Condition(aConditionBase, aDomain.get(i));

			if (!isFilterNull && !itsFilter.isUseful(aParentConditions, aCondition))
				continue;

			Subgroup aNewSubgroup = aParent.getRefinedSubgroup(aCondition);
			checkAndLog(aNewSubgroup, theParentCoverage);
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
		assert (isDirectSetting());
		assert (theColumnConditionBases.size() == 1);
		assert (theColumnConditionBases.get(0).getOperator() == Operator.ELEMENT_OF);

		ConditionBase aConditionBase = theColumnConditionBases.get(0);

		// as for BestIntervals -> use new half-interval code, it is 70x faster
		NominalCrossTable aNCT = new NominalCrossTable(aConditionBase.getColumn(), theParentMembers, itsBinaryTarget);
		SortedSet<String> aDomainBestSubSet = new TreeSet<String>();

		// final: if-else is long, ensure value is set before creating Subgroup
		final int aCountHeadBody;
		final int aCoverage;
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

			aCountHeadBody = aP;
			aCoverage = (aP + aN);
			aFinalBestQuality = itsQualityMeasure.calculate(aCountHeadBody, aCoverage);
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

			aCountHeadBody = aBestP;
			aCoverage = (aBestP + aBestN);
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
		Subgroup aChild = directComputation(theParent, anAddedCondition, aFinalBestQuality, aCoverage, aCountHeadBody);
		checkAndLog(aChild, theParent.getCoverage());
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// numeric                                                          /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	// for SINGLE_NOMINAL this could call getUniqueNumericDomainMap
	// because EQUAL was treated as NOMINAL no 'best' version exists, also the
	// GUI is a mess, it is not clear what ALL/BEST/BEST_BINS/BINS mean for
	// EQUALS, for example: BINS with EQUALS could be interpreted as what is
	// currently CONSECUTIVE bins
	// TODO when finished -> update isPOCSetting() related codes
	//
	//
	// NOTE
	// historically, numeric equals was treated as a nominal Refinement
	// this should be considered a bug
	// when user selects numeric operator setting NUMERIC_ALL (=, <=, >=), both
	// <= and >= use bins when NumericStrategy.NUMERIC_(BEST_)BINS is used, but
	// = does not, it always evaluated all numeric values in a domain
	//
	// so for NumericOperatorSetting.NUMERIC_EQ, which uses Operator.EQUALS, the
	// NumericStrategy is not respected
	// the behaviour is always like NumericStrategy.NUMERIC_ALL
	// there is no NUMERIC_BEST, NUMERIC_BEST_BINS and NUMERIC_BINS alternative
	private final void evaluateNumericEqualsRefinements(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		//NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		NumericStrategy THIS_IS_A_FAKE = NumericStrategy.NUMERIC_ALL;
		assert (itsSearchParameters.getNumericOperatorSetting().includesEquals());

		////////////////////////////////////////////////////////////////////////
		boolean isFilterNull = (itsFilter == null);
		Subgroup aParent = theRefinement.getSubgroup();

		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		Column aColumn = aConditionBase.getColumn();
		ConditionListA aParentConditions = (isFilterNull ? null : aParent.getConditions());
		Operator anOperator = aConditionBase.getOperator();

		assert (anOperator == Operator.EQUALS);

		// FIXME this is why this is such a weird strategy - leave it for now
		// might require update when more strategies are added
		//boolean isAllStrategy = (aNumericStrategy == NumericStrategy.NUMERIC_ALL || aNumericStrategy == NumericStrategy.NUMERIC_BINS);

		//Subgroup aBestSubgroup = null;
		////////////////////////////////////////////////////////////////////////

		// POCSetting ensures a sorted NUMERIC domain for SINGLE_NOMINAL
		// DirectSetting is implied
		// NUMERIC_VIKAMINE_CONSECUTIVE_ALL|BEST are currently not a POCSetting
		boolean direct = isPOCSetting();
		// final: ensure value is set before use in loop
		final int aSize;
		final float [] aDomain;
		final int[] aCounts;
		final int[] aTPs;

		// split code paths, for most SINGLE_NOMINAL settings versus the rest
		if (direct)
		{
			ValueInfo via = aColumn.getUniqueNumericDomainMap(theParentMembers);
			aDomain = aColumn.SORTED;
			aCounts = via.itsCounts;
			aSize = aDomain.length;
			aTPs = via.itsRecords; // this is the TruePositive count
		}
		else
		{
//			DomainMapNumeric m = getDomainMapD(theParentMembers, theParentCoverage, aNumericStrategy, aColumn, itsSearchParameters.getNrBins(), anOperator);
			DomainMapNumeric m = getDomainMapD(theParentMembers, theParentCoverage, THIS_IS_A_FAKE, aColumn, 0, anOperator);
			aDomain = m.itsDomain;
			aCounts = m.itsCounts;
			aSize = m.itsSize;
			aTPs = null;
			if (aSize <= 1)
				return;
		}

		for (int i = 0; i < aSize && !isTimeToStop(); ++i)
		{
			int aCoverage = aCounts[i];

			// for ValueInfo this includes the (aCounts[i] == 0) check
			if (aCoverage < itsMinimumCoverage)
				continue;

			// NOTE latter check not required for aDomainMap as size would be 1
			if (direct && (aCoverage == theParentCoverage))
				break;

			Condition anAddedCondition = new Condition(aConditionBase, aDomain[i]);

			if (!isFilterNull && !itsFilter.isUseful(aParentConditions, anAddedCondition))
				continue;

			final Subgroup aChild;
			if (direct)
				aChild = directComputation(aParent, anAddedCondition, itsQualityMeasure, aCoverage, aTPs[i]);
			else
				aChild = aParent.getRefinedSubgroup(anAddedCondition);

			checkAndLog(aChild, theParentCoverage);
		}
	}

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
	 *
	 * TODO MM
	 * for *_BEST strategies, in case of ties, multiple subgroups
	 * attaining the best score, this implementation retains only the first
	 * instead it could retain all best scoring subgroups
	 *
	 * moreover, the treatment of <= and >= is dubious
	 * for <= the first, and therefore smallest best Subgroup is retained
	 * for >= the first, and therefore largest best Subgroup is retained
	 */
	private void evaluateNumericRefinements(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		switch (itsSearchParameters.getNumericStrategy())
		{
			case NUMERIC_ALL :
				numericHalfIntervals(theParentMembers, theParentCoverage, theRefinement);
				break;
			// NUMERIC_BEST is like NUMERIC_ALL, but uses only best scoring subgroup
			case NUMERIC_BEST :
				numericHalfIntervals(theParentMembers, theParentCoverage, theRefinement);
				break;
			case NUMERIC_BINS :
				numericHalfIntervals(theParentMembers, theParentCoverage, theRefinement);
				break;
			// NUMERIC_BEST_BINS is like NUMERIC_BINS, but uses only best scoring subgroup
			case NUMERIC_BEST_BINS :
				numericHalfIntervals(theParentMembers, theParentCoverage, theRefinement);
				break;
			case NUMERIC_INTERVALS :
				numericIntervals(theParentMembers, theParentCoverage, theRefinement);
				break;
			case NUMERIC_VIKAMINE_CONSECUTIVE_ALL :
				numericConsecutiveBins(theParentMembers, theParentCoverage, theRefinement);
				break;
			// NUMERIC_VIKAMINE_CONSECUTIVE_BEST is like NUMERIC_VIKAMINE_CONSECUTIVE_ALL, but uses only best scoring subgroup
			case NUMERIC_VIKAMINE_CONSECUTIVE_BEST :
				numericConsecutiveBins(theParentMembers, theParentCoverage, theRefinement);
				break;
//			case NUMERIC_VIKAMINE_CARTESIAN_ALL :
//				throw new AssertionError(itsSearchParameters.getNumericStrategy() + " not implemented");
//			case NUMERIC_VIKAMINE_CARTESIAN_BEST :
//				throw new AssertionError(itsSearchParameters.getNumericStrategy() + " not implemented");
			default :
				Log.logCommandLine("SubgroupDiscovery.evaluateNumericRefinements(): unknown Numeric Strategy: " +
							itsSearchParameters.getNumericStrategy());
				break;
		}
	}

	// FIXME MM historically numeric EQUALS uses nominal code, it should change
	private final void numericHalfIntervalsDomainMapNumeric(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		// evaluateNumericRefinements() should prevent getting here for
		// NUMERIC_INTERVALS and NUMERIC_VIKAMINE_CONSECUTIVE_ALL|BEST
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		assert (aNumericStrategy == NumericStrategy.NUMERIC_ALL || aNumericStrategy == NumericStrategy.NUMERIC_BEST ||
				aNumericStrategy == NumericStrategy.NUMERIC_BINS || aNumericStrategy == NumericStrategy.NUMERIC_BEST_BINS);

		////////////////////////////////////////////////////////////////////////
		boolean isFilterNull = (itsFilter == null);
		Subgroup aParent = theRefinement.getSubgroup();

		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		Column aColumn = aConditionBase.getColumn();
		ConditionListA aParentConditions = (isFilterNull ? null : aParent.getConditions());
		Operator anOperator = aConditionBase.getOperator();

		// (cover-update and check) order relies on binary choice <= or >=
		assert (anOperator == Operator.LESS_THAN_OR_EQUAL || anOperator == Operator.GREATER_THAN_OR_EQUAL);

		// might require update when more strategies are added
		boolean isAllStrategy = (aNumericStrategy == NumericStrategy.NUMERIC_ALL || aNumericStrategy == NumericStrategy.NUMERIC_BINS);

		Subgroup aBestSubgroup = null;
		////////////////////////////////////////////////////////////////////////

		DomainMapNumeric m = getDomainMapD(theParentMembers, theParentCoverage, aNumericStrategy, aColumn, itsSearchParameters.getNrBins(), anOperator);
		float[] aDomain = m.itsDomain;
		int[] aCounts = m.itsCounts;

		// code paths might be split one day, to avoid isLEQ check inside loop
		boolean isLEQ = (anOperator == Operator.LESS_THAN_OR_EQUAL);
		// for bins: <= with last value, >= with first, might select all data
		boolean selectsAllData = (m.itsCountsSum == theParentCoverage);
		int s = (!isLEQ && selectsAllData ? 1 : 0);
		int e = ( isLEQ && selectsAllData ? m.itsSize-1 : m.itsSize);
		int c = (isLEQ ? 0 : (!selectsAllData ? m.itsCountsSum : (m.itsCountsSum-aCounts[0])));

		//System.out.format("%s AND %s [value]%n", anOldSubgroup, aConditionBase);
		for (int i = s, j = e, cover = c; i < j && !isTimeToStop(); ++i)
		{
			int cnt = aCounts[i];
			assert (cnt != 0);
			//System.out.format("m.size=%d m.countSum=%d s=%d e=%d c=%d i=%d cnt=%d v=%f%n", m.itsSize, m.itsCountsSum, s, e, c, i, cnt, aDomain[i]);

			if (isLEQ)
			{
				cover += cnt;
				if (cover < itsMinimumCoverage)
					continue;
			}
			else
			{
				if (cover < itsMinimumCoverage)
					break;
				cover -= cnt;
			}

			Condition aCondition = new Condition(aConditionBase, aDomain[i]);

			if (!isFilterNull && !itsFilter.isUseful(aParentConditions, aCondition))
				continue;

			Subgroup aNewSubgroup = aParent.getRefinedSubgroup(aCondition);

			if (isAllStrategy)
			{
				//addToBuffer(aNewSubgroup);
				checkAndLog(aNewSubgroup, theParentCoverage);
			}
			else
			{
				// more clear than using else-if
				if (isValidAndBest(aNewSubgroup, theParentCoverage, aBestSubgroup))
					aBestSubgroup = aNewSubgroup;
			}
		}

		if (!isAllStrategy && (aBestSubgroup != null))
			bestAdd(aBestSubgroup, theParentCoverage);
	}

	private final void numericHalfIntervals(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		// currently only for SINGLE_NOMINAL (and not for propensity scores)
		if (!isPOCSetting())
		{
			numericHalfIntervalsDomainMapNumeric(theParentMembers, theParentCoverage, theRefinement);
			return;
		}

		////////////////////////////////////////////////////////////////////////
		Subgroup aParent = theRefinement.getSubgroup();

		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		Column aColumn = aConditionBase.getColumn();
		Operator anOperator = aConditionBase.getOperator();

		// (cover-update and check) order relies on binary choice <= or >=
		assert (anOperator == Operator.LESS_THAN_OR_EQUAL || anOperator == Operator.GREATER_THAN_OR_EQUAL);

		// might require update when more strategies are added
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		boolean isAllStrategy = (aNumericStrategy == NumericStrategy.NUMERIC_ALL);

		Subgroup aBestSubgroup = null;
		////////////////////////////////////////////////////////////////////////

		ValueInfo via = aColumn.getUniqueNumericDomainMap(theParentMembers);

		// split path split - BEST_BINS/BINS use substantially different loop
		if (aNumericStrategy.isDiscretiser())
		{
			numericHalfIntervalsValueCountsCoarsePOC(aParent, aConditionBase, via);
			return;
		}

		// via.print(aColumn, true); // can not be used, code abuses ValueInfo
		int[] aCounts = via.itsCounts;
		int[] aRecords = via.itsRecords; // this is the TruePositive count

		if (anOperator == Operator.LESS_THAN_OR_EQUAL)
		{
			for (int i = 0, j = aCounts.length, cover = 0, tp = 0; i < j && !isTimeToStop(); ++i)
			{
				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				cover += aCount;

				if (cover == theParentCoverage)
					break;

				// count true positives fort this value
				tp += aRecords[i];
				// generally, when tp does not increase, AND last Subgroup was
				// >= minimum coverage, the loop should ignore this Candidate
				// but this requires a check on all (convex) quality measures
				// FIXME (but profile and test current code first)

				// no need to evaluate
				if (cover < itsMinimumCoverage)
					continue;

				Condition aCondition = new Condition(aConditionBase, aColumn.SORTED[i], i);
				// always assign: returns null, aBestSugroup, or aNewSubgroup
				aBestSubgroup = evaluateCandidate(aParent, aCondition, cover, tp, isAllStrategy, aBestSubgroup);
			}
		}
		else
		{
			// NOTE getTertiaryStatistic() only works for SINGLE_NOMINAL
			for (int i = 0, j = aCounts.length, cover = theParentCoverage, tp = (int) aParent.getTertiaryStatistic(); i < j && !isTimeToStop(); ++i)
			{
				if (cover < itsMinimumCoverage)
					break;

				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				// >= with the first value select the same subset as the parent
				// old tp and cover, as counts for this value should be included
				if (cover != theParentCoverage)
				{
					Condition aCondition = new Condition(aConditionBase, aColumn.SORTED[i], i);
					aBestSubgroup = evaluateCandidate(aParent, aCondition, cover, tp, isAllStrategy, aBestSubgroup);
				}

				// before moving to next, subtract counts related to this value
				cover -= aCount;
				tp -= aRecords[i];
			}
		}

		if (!isAllStrategy && (aBestSubgroup != null))
			bestAdd(aBestSubgroup, theParentCoverage);
	}

	// NOTE for comparison bugs in original code are deliberately reproduced atm
	@SuppressWarnings("unused") // suppress warnings when DEBUG_POC_BINS = false
	private final void numericHalfIntervalsValueCountsCoarsePOC(Subgroup theParent, ConditionBase theConditionBase, ValueInfo theValueInfo)
	{
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		assert (aNumericStrategy == NumericStrategy.NUMERIC_BEST_BINS || aNumericStrategy == NumericStrategy.NUMERIC_BINS);

		// (cover-update and check) order relies on binary choice <= or >=
		Operator anOperator = theConditionBase.getOperator();
		assert (anOperator == Operator.LESS_THAN_OR_EQUAL || anOperator == Operator.GREATER_THAN_OR_EQUAL);

		long aNrBins = itsSearchParameters.getNrBins();

		// not checked in MiningWindow/XML, do nothing for now (no error)
		if (aNrBins <= 1L)
			return;

		// long to prevent overflow for multiplication
		long aParentCoverage = theParent.getCoverage();
		int[] aCounts = theValueInfo.itsCounts;
		int[] aRecords = theValueInfo.itsRecords; // holds true positive counts
		float[] theSortedValues = theConditionBase.getColumn().SORTED;
		boolean isAllStrategy = (aNumericStrategy == NumericStrategy.NUMERIC_BINS);
		Subgroup aBestSubgroup = null;

		// for debugging, code currently reproduces faulty behaviour of original
		// leave this in to evaluate how correct behaviour changes output
		List<Float> aCheck = DEBUG_POC_BINS ? new ArrayList<Float>() : null;
		List<Float> aValid = DEBUG_POC_BINS ? new ArrayList<Float>() : null;

		if (anOperator == Operator.LESS_THAN_OR_EQUAL)
		{
			for (int i = 0, j = 1, next = ((int) (aParentCoverage / aNrBins)), cover = 0, tp = 0; j < (int) aNrBins && !isTimeToStop(); ++i)
			{
				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				cover += aCount;
				tp += aRecords[i];

				// for debugging do not continue on (cover < itsMinimumCoverage)

				if (cover <= next  || (!DEBUG_POC_BINS && (cover < itsMinimumCoverage)))
					continue;

				// for debugging do not break on (cover == anOldCoverage)
				if (!DEBUG_POC_BINS && (cover == aParentCoverage))
					break;

				Condition aCondition = new Condition(theConditionBase, theSortedValues[i], i);
				aBestSubgroup = evaluateCandidate(theParent, aCondition, cover, tp, isAllStrategy, aBestSubgroup);

				while (((next = ((int) ((++j * aParentCoverage) / aNrBins)))) <= cover-1)
					; // deliberately empty

				// debug only
				if (DEBUG_POC_BINS)
				{
					aCheck.add(theSortedValues[i]);
					if (!((cover < itsMinimumCoverage) || (cover == aParentCoverage)))
						aValid.add(theSortedValues[i]);
				}
			}
		}
		else
		{
			// NOTE getTertiaryStatistic() only works for SINGLE_NOMINAL
			// NOTE division in old code rounds down for <= and >=
			for (int i = 0, j = 1, next = (int) (aParentCoverage - (aParentCoverage / aNrBins)), cover = (int) aParentCoverage, tp = (int) theParent.getTertiaryStatistic(); j < (int) aNrBins && !isTimeToStop(); ++i)
			{
				// for debugging do not break on (cover < itsMinimumCoverage)
				if (!DEBUG_POC_BINS && (cover < itsMinimumCoverage))
					break;

				int aCount = aCounts[i];
				if (aCount == 0)
					continue;

				// last value with required cover, use old cover and tp
				// for debugging: no 'continue' on (cover < itsMinimumCoverage)
				if (((cover-aCount) < next) && (DEBUG_POC_BINS || (cover != aParentCoverage)))
				{
					Condition aCondition = new Condition(theConditionBase, theSortedValues[i], i);
					aBestSubgroup = evaluateCandidate(theParent, aCondition, cover, tp, isAllStrategy, aBestSubgroup);

					while ((next = (int) (aParentCoverage - ((++j * aParentCoverage) / aNrBins))) > (cover-aCount))
						; // deliberately empty

					if (DEBUG_POC_BINS)
					{
						aCheck.add(theSortedValues[i]);
						if ((cover >= itsMinimumCoverage) && (cover != aParentCoverage))
							aValid.add(theSortedValues[i]);
					}
				}

				// before moving to next, subtract counts related to this value
				cover -= aCount;
				tp -= aRecords[i];
			}
		}

		if (DEBUG_POC_BINS)
		{
			float[] orig = Column.getUniqueValues(theConditionBase.getColumn().getUniqueSplitPoints(theParent.getMembers(), (int) aNrBins-1, anOperator));
			int size = aCheck.size();
			float[] check = new float[size];
			for (int i = 0; i < size; ++i)
				check[i] = aCheck.get(i);
			if (!Arrays.equals(orig, check))
			{
				StringBuilder sb = new StringBuilder((int) Math.min(Integer.MAX_VALUE, (aCounts.length + aRecords.length + orig.length + check.length + aValid.size()) * 8L));
				sb.append("WARNING: numericHalfIntervalsCoarsePOC()");
				sb.append(String.format("%n%s AND %s [value] (anOldCoverage=%d)%n", theParent, theConditionBase, aParentCoverage));
				sb.append(Arrays.toString(aCounts));
				sb.append(Arrays.toString(aRecords));
				sb.append("");
				sb.append("\t orig: " + Arrays.toString(orig));
				sb.append("\tcheck: " + Arrays.toString(check));
				sb.append("\tvalid: " + aValid.toString());
				Log.logCommandLine(sb.toString());
				// no AssertionError() - original code has unexpected bugs
				try { Thread.sleep(10000); }
				catch (InterruptedException e) {};
			}
		}

		if (!isAllStrategy && (aBestSubgroup != null))
			bestAdd(aBestSubgroup, (int) aParentCoverage);
	}

	@SuppressWarnings("unused")
	private static final void prettyPrint(BitSet theBitSet, int theSize)
	{
		StringBuilder sb = new StringBuilder(theSize + 2);
		sb.append("{");
		for (int i = 0; i < theSize; ++i)
			sb.append(theBitSet.get(i) ? "1" : "0");
		Log.logCommandLine(sb.append("}").toString());
	}

	private static final int getTruePositiveCount(BitSet theBinaryTarget, int[] theRecords, int theStart, int theEnd)
	{
		int aCount = 0;

		// TODO profile loop over BitSet or Record (time depends on cardinality)
		// loop over record indexes (assumes cardinality is lower than BitSet)
		for (int i = theStart; i < (theStart + theEnd); ++i)
			if (theBinaryTarget.get(theRecords[i]))
			++aCount;

		return aCount;
	}

	// return is only relevant (and non-null) in BEST and BEST_BINS settings
	private Subgroup evaluateCandidate(Subgroup theParent, Condition theAddedCondition, int theChildCoverage, int theNrTruePositives, boolean isAllStrategy, Subgroup theBestSubgroup)
	{
		// currently only for SINGLE_NOMINAL (and not for propensity scores)
		assert (isPOCSetting());

		if ((itsFilter != null) && !itsFilter.isUseful(theParent.getConditions(), theAddedCondition))
			return theBestSubgroup; // null or the best so far

		int aParentCoverage = theParent.getCoverage();
		Subgroup aNewSubgroup = directComputation(theParent, theAddedCondition, itsQualityMeasure, theChildCoverage, theNrTruePositives);

		// ALL or BINS
		if (isAllStrategy)
		{
			//addToBuffer(aNewSubgroup);
			checkAndLog(aNewSubgroup, aParentCoverage);
			return null; // always null in this setting
		}

		// BEST or BESTBINS
		if (isValidAndBest(aNewSubgroup, aParentCoverage, theBestSubgroup))
			return aNewSubgroup; // if the new subgroup is better
		else
			return theBestSubgroup; // if the old subgroup is better (or null)
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// numeric consecutive intervals                                    /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	private void numericConsecutiveBins(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		// evaluateNumericRefinements() should prevent getting here for others
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		assert (aNumericStrategy == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_ALL || aNumericStrategy == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_BEST);

		////////////////////////////////////////////////////////////////////////
		boolean isFilterNull = (itsFilter == null);
		Subgroup aParent = theRefinement.getSubgroup();

		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		ConditionListA aParentConditions = (isFilterNull ? null : aParent.getConditions());
		Operator anOperator = aConditionBase.getOperator();

		// 'in' is the only valid Operator for this setting
		assert (anOperator == Operator.BETWEEN);

		// might require update when more strategies are added
		boolean isAllStrategy = (aNumericStrategy == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_ALL);

		Subgroup aBestSubgroup = null;
		////////////////////////////////////////////////////////////////////////

		// this is the crucial translation from nr bins to nr splitpoints
		int aNrSplitPoints = itsSearchParameters.getNrBins() - 1;
		// useless, Column.getSplitPointsBounded() would return an empty array
		if (aNrSplitPoints <= 0)
			return;
		//SortedSet<Interval> anIntervals = aConditionBase.getColumn().getUniqueSplitPointsBounded(theParentMembers, aNrSplitPoints);
		SortedMap<Interval, Integer> anIntervals = aConditionBase.getColumn().getUniqueSplitPointsBounded(theParentMembers, theParentCoverage, aNrSplitPoints);

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

			if (aCount == theParentCoverage)
				break;

			Condition aCondition = new Condition(aConditionBase, e.getKey());

			if (!isFilterNull && !itsFilter.isUseful(aParentConditions, aCondition))
				continue;

			Subgroup aNewSubgroup = aParent.getRefinedSubgroup(aCondition);

			if (isAllStrategy)
			{
				//addToBuffer(aNewSubgroup);
				checkAndLog(aNewSubgroup, theParentCoverage);
			}
			else
			{
				// more clear than using else-if
				if (isValidAndBest(aNewSubgroup, theParentCoverage, aBestSubgroup))
					aBestSubgroup = aNewSubgroup;
			}
		}

		if (!isAllStrategy && (aBestSubgroup != null))
			bestAdd(aBestSubgroup, theParentCoverage);
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

	private void numericIntervals(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		// this method is a deviant case, but ValueInfo relies on isPOCSetting
		assert (isPOCSetting());

		// evaluateNumericRefinements() should prevent getting here for others
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		assert (aNumericStrategy == NumericStrategy.NUMERIC_INTERVALS);

		////////////////////////////////////////////////////////////////////////
		Subgroup aParent = theRefinement.getSubgroup();

		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		Column aColumn = aConditionBase.getColumn();
		Operator anOperator = aConditionBase.getOperator();

		// 'in' is the only valid Operator for this setting
		assert (anOperator == Operator.BETWEEN);
		////////////////////////////////////////////////////////////////////////

		// copy, as RealBaseIntervalCrossTable will modify the supplied array
		float [] aDomain = Arrays.copyOf(aColumn.SORTED, aColumn.SORTED.length);
		ValueInfo via = aColumn.getUniqueNumericDomainMap(theParentMembers);

		RealBaseIntervalCrossTable aRBICT = new RealBaseIntervalCrossTable(theParentCoverage, (int) aParent.getTertiaryStatistic(), aDomain, via);

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
			// brute force method, keep for now for testing purposes
			/*
			aSplitPoints = aRBICT.getSplitPoints();
			for (int i=0; i<aSplitPoints.length; i++)
			{
				Interval aNewInterval = new Interval(aSplitPoints[i], Float.POSITIVE_INFINITY);
				Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aNewInterval);
				double aQuality = evaluateCandidate(aNewSubgroup);
				if (aQuality > aBestQuality) {
					aBestQuality = aQuality;
					aBestInterval = aNewInterval;
				}
				aNewInterval = new Interval(Float.NEGATIVE_INFINITY, aSplitPoints[i]);
				aNewSubgroup = theRefinement.getRefinedSubgroup(aNewInterval);
				aQuality = evaluateCandidate(aNewSubgroup);
				if (aQuality > aBestQuality) {
					aBestQuality = aQuality;
					aBestInterval = aNewInterval;
				}
				for (int j=i+1; j<aSplitPoints.length; j++)
				{
					aNewInterval = new Interval(aSplitPoints[i], aSplitPoints[j]);
					aNewSubgroup = theRefinement.getRefinedSubgroup(aNewInterval);
					aQuality = evaluateCandidate(aNewSubgroup);
					if (aQuality > aBestQuality) {
						aBestQuality = aQuality;
						aBestInterval = aNewInterval;
					}
				}
			}*/

			// the linear algo
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
		}

		Condition anAddedCondition = new Condition(aConditionBase, aBestInterval);
		Subgroup aChild = directComputation(aParent, anAddedCondition, aBestQuality, (aBestNrTruePositives + aBestNrFalsePositives), aBestNrTruePositives);
		checkAndLog(aChild, theParentCoverage);
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// numeric domain code - some methods now bypass these methods      /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	// called by numericHalfIntervalsDomainMapNumeric()
	private static final DomainMapNumeric getDomainMapD(BitSet theMembers, int theMembersCardinality, NumericStrategy theNumericStrategy, Column theColumn, int theNrBins, Operator theOperator)
	{
		switch (theNumericStrategy)
		{
			case NUMERIC_ALL	: return theColumn.getUniqueNumericDomainMap(theMembers, theMembersCardinality);
			case NUMERIC_BEST	: return theColumn.getUniqueNumericDomainMap(theMembers, theMembersCardinality);
			case NUMERIC_BINS	: return theColumn.getUniqueSplitPointsMap(theMembers, theMembersCardinality, theNrBins-1, theOperator);
			case NUMERIC_BEST_BINS	: return theColumn.getUniqueSplitPointsMap(theMembers, theMembersCardinality, theNrBins-1, theOperator);
			case NUMERIC_INTERVALS	:
			{
				throw new AssertionError("NUMERIC_STRATEGY NOT IMPLEMENTED: " + theNumericStrategy);
				//return theColumn.getUniqueNumericDomainMap(theMembers);
			}
			default :
				throw new AssertionError("invalid Numeric Strategy: " + theNumericStrategy);
		}
	}

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

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// generic Candidate evaluation code - some methods now bypass this /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	/*
	 * REQUIREMENT 1
	 * additions to itsResult and itsCandidateQueue need to be performed as
	 * a logical unit, else their contents would become undefined in the
	 * following multi-threaded scenario:
	 *
	 * Thread 1 itsResult.add()
	 * Thread 2 itsResult.add()
	 * Thread 2 itsCandidateQueue.add()
	 * Thread 1 itsCandidateQueue.add()
	 *
	 * both itsResults and itsCandidateQueue are trimmed if they have a max
	 * capacity and a candidate may end up in the one, but not in the other
	 * 
	 * REQUIREMENT 2
	 * evaluateCandidate() is expensive for complex models and should not be
	 * executed in a synchonized block
	 *
	 * REQUIREMENT 3
	 * additionally the value of itsCandidateCount.getAndIncrement() should
	 * indicate the n-th call to this method, so the n-th checked Candidate
	 * and the subgroup.nr should be this value also
	 * this can only be guaranteed by doing it in the same synchronized
	 * block
	 * but to keep the scope of the synchronized method small (synchronized
	 * blocks execute many times slower) the logging is not done in the
	 * synchronized method, but guarantees to use to the correct value
	 * FIXME MM
	 * if fact, this makes no sense at all in a multi-threaded environment
	 * because of the unpredictable order in which Subgroups generated from
	 * ('thread local') refinements arrive at check(), the count for any
	 * Subgroup is unpredictable anyway
	 * so coupling the check()-count to the subgroup number reported in the
	 * log does not guarantee invocation invariant logs, where a subgroup
	 * has the same number over consecutive identical experiments
	 * for a single thread, the behaviour will hold regardless, so no extra
	 * care is required
	 * TL;DR count + subgroup number will be decoupled / unsynchronised
	 *
	 * technically synchronisation needs only execute the addition to the
	 * result and candidate set as a logical block
	 *
	 * NOTE that in case of ties on the itsResult / itsCandidateQueue
	 * max_size boundary this may effect the final search result
	 * this is related to the fixed max size and has the potential to break
	 * invocation invariant results in multi-threaded settings
	 * FIXME MM
	 * this is probably no longer true for beam strategies, that move from
	 * one level to the next, when all candidates on the first are evaluated
	 * the new ConditionListA compares Conditions in canonical, so
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

		// FIXME MM this check should be made obsolete
		int aChildCoverage = theChild.getCoverage();
		boolean isValid = (aChildCoverage >= itsMinimumCoverage && aChildCoverage < theParentCoverage);

		if (isValid)
		{
			// NOTE
			// NumericStrategy could get a method like isBestStrategy(), but
			// than still it gives no guarantee that code in SubgroupDiscovery
			// sets the quality/secondary/tertiary statistics for theSubgroup
			// NOTE NUMERIC_INTERVALS is both a isPOCSetting and a aNumericBest
			EnumSet<NumericStrategy> aNumericBest = EnumSet.of(NumericStrategy.NUMERIC_BEST,
																NumericStrategy.NUMERIC_BEST_BINS,
																NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_BEST,
																NumericStrategy.NUMERIC_INTERVALS);
			AttributeType lastAdded = theChild.getConditions().get(theChild.getDepth()-1).getColumn().getType();
			boolean isLastNumeric = (lastAdded == AttributeType.NUMERIC);

			// final: ensure value is set before aResultAddition-check
			final float aQuality;

			if ((lastAdded == AttributeType.BINARY) && isDirectSetting())
			{
				// NOTE this path already performed the isValid-coverage check
				aQuality = (float) theChild.getMeasureValue();
			}
			else if ((lastAdded == AttributeType.NOMINAL) && itsSearchParameters.getNominalSets())
			{
				// BestValueset already set quality (BestInterval did also, but
				// is picked up by the BEST* check below)
				// NOTE both code paths did not performed the isValid-coverage
				//      check refer to the methods for comments on this issue
				aQuality = (float) theChild.getMeasureValue();
			}
			else if (isLastNumeric && isPOCSetting())
			{
				// currently only for SINGLE_NOMINAL (and no propensity scores)
				// NOTE this path already performed the isValid-coverage check
				aQuality = (float) theChild.getMeasureValue();
			}
			else if (isLastNumeric && aNumericBest.contains(itsSearchParameters.getNumericStrategy()))
			{
				// NOTE for BEST* Subgroup is already evaluated and quality is
				// set by isValidAndBest() or numericIntervals() (BestInterval)
				// NOTE isValidAndBest() performs an incorrect isValid-coverage
				// check, and BestInterval does not perform one at all
				aQuality = (float) theChild.getMeasureValue();
			}
			else
			{
				aQuality = evaluateCandidate(theChild);
				theChild.setMeasureValue(aQuality);
			}

			Candidate aCandidate = new Candidate(theChild);

			boolean aResultAddition = false;
			// if quality should be ignored or is enough
			// and the coverage is not too high
			if ((ignoreQualityMinimum || aQuality > itsQualityMeasureMinimum) && (aChildCoverage <= itsMaximumCoverage))
				aResultAddition = true;

			// all logic is performed outside of synchronized block
			// to keep it as small as possible
			synchronized (itsCheckLock)
			{
				if (aResultAddition)
					itsResult.add(theChild);

				itsCandidateQueue.add(aCandidate);
			}
		}

		// prevent OutOfMemory / GC Overhead Limit errors, some code paths
		// bypass evaluateCandidate(Subgroup) so calling it there is no good
		// and this is the sole method to add to Candidate and Result sets
		theChild.killMembers();

		// incrementing after expensive check() makes subgroup numbers
		// in log 'closer to being consecutive' when multi-threading
		// a synchronized block with itsCandidateCount.getAndIncrement()
		// and logCandidateAddition() would yield consecutive numbers
		// but is slower and does not yield useful practical benefits
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
			// what needs to be calculated for this QM
			// NOTE requiredStats could be a final SubgroupDiscovery.class member
			QM aQM = itsSearchParameters.getQualityMeasure();
			Set<Stat> aRequiredStats = QM.requiredStats(aQM);
			//Statistics aStatistics = itsNumericTarget.getStatistics(aMembers, aRequiredStats);
			// TODO MM - implement better solution than below two checks
			Statistics aStatistics = itsNumericTarget.getStatistics(aChildMembers, aQM == QM.MMAD, true);

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
			theChild.setTertiaryStatistic(aStatistics.getSubgroupStandardDeviation());
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

		// FIXME why is this a static member field
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
				o == NumericOperatorSetting.NUMERIC_ALL;

			// [ (C >= x)  ^ ... ^ (C <= x) ] ^ (C.op.v) is useless
			isNumericNormalOrNumericAll =
				o == NumericOperatorSetting.NUMERIC_NORMAL ||
				o == NumericOperatorSetting.NUMERIC_ALL;
		}

		// tests on existing ConditionList and 'value-free' Refinement
		boolean isUseful(Refinement theRefinement)
		{
			// TODO MM could create Refinement.isSelfReferencing()
			// to indicate that Refinement is about a Column that
			// already occurs in the ConditionList for its Subgroup
			//ConditionList aConditionList = theRefinement.getSubgroup().getConditions();
			ConditionListA aConditionList = theRefinement.getSubgroup().getConditions();

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
		boolean isUseful(ConditionListA theConditionList, Condition theCondition)
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
	 * test assumption that NUMERIC_ALL is the only NumericOperatorSetting
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
				if (s != NumericOperatorSetting.NUMERIC_ALL)
					// assumption fails
					return false;
			}
		}

		// assumption holds
		return true;
	}
	/*
	 * test assumption that NUMERIC_NORMAL and NUMERIC_ALL are the only
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
				if (s != NumericOperatorSetting.NUMERIC_NORMAL &&
					s != NumericOperatorSetting.NUMERIC_ALL)
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
	private static final boolean isOverlap(ConditionListA theConditionList, Column theColumn)
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
	private static final boolean hasOverridingEquals(ConditionListA theConditionList, Column theColumn)
	{
		return hasEquals(theConditionList) && hasRelevantEquals(theConditionList, theColumn);
	}
	//private static final boolean hasEquals(ConditionList theConditionList)
	private static final boolean hasEquals(ConditionListA theConditionList)
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
	private static final boolean hasRelevantEquals(ConditionListA theConditionList, Column theColumn)
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
	private static final boolean contains(ConditionListA theConditionList, Condition theCondition)
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
	private static final boolean createsRedundantEquals(ConditionListA theConditionList, Condition theCondition)
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
	private static final boolean hasRelevantEqualsThroughLeqGeq(ConditionListA theConditionList, Column theColumn)
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
		boolean isUseful(ConditionListA theConditionList, Condition theCondition)
		{
			boolean b = super.isUseful(theConditionList, theCondition);
			//if (!b) // uncomment to only print refused Refinements
			print(b, "POST\t", theConditionList, theCondition.toString());
			return b;
		}

		// could be faster in calling method - but debug only anyway
		//private final void print(boolean useful, String phase, ConditionList conditionList, String value)
		private final void print(boolean useful, String phase, ConditionListA conditionList, String value)
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

	private final void evaluateNominalRefinementsObsolete(BitSet theParentMembers, int theParentCoverage, Refinement theRefinement)
	{
		// evaluateNominalRefinements() should prevent getting here for ValueSet
		assert (!itsSearchParameters.getNominalSets());
		// should have been checked by evaluateNominalBinaryRefinements()
		assert (theParentMembers.cardinality() == theParentCoverage);
		assert (theParentCoverage > 1);

		boolean isFilterNull = (itsFilter == null);
		Subgroup aParent = theRefinement.getSubgroup();
		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		Column aColumn = aConditionBase.getColumn();
		ConditionListA aParentConditions = (isFilterNull ? null : aParent.getConditions());

		//String[] aDomain = aColumn.getUniqueNominalBinaryDomain(theParentMembers);
		// obsolete code calls obsolete code, ignore: change required to compile
		List<String> aColumnDomain = aColumn.itsDistinctValuesU;
		int[] aCounts = aColumn.getUniqueNominalDomainCounts(theParentMembers, theParentCoverage);

		String[] aDomain = new String[aCounts[aCounts.length-1]];
		for (int i = 0, j = -1; i < aCounts.length-1; ++i)
			if (aCounts[i] > 0)
				aDomain[++j] = aColumnDomain.get(i);
		Arrays.sort(aDomain);

		// no useful Refinements are possible
		if (aDomain.length <= 1)
			return;

		for (int i = 0, j = aDomain.length; i < j && !isTimeToStop(); ++i)
		{
			Condition aCondition = new Condition(aConditionBase, aDomain[i]);

			if (!isFilterNull && !itsFilter.isUseful(aParentConditions, aCondition))
				continue;

			Subgroup aNewSubgroup = aParent.getRefinedSubgroup(aCondition);
			checkAndLog(aNewSubgroup, theParentCoverage);
		}
	}

	// this method is currently unnecessary, but addToBuffer will return one day
	private final void bestAdd(Subgroup theBestSubgroup, int theOldCoverage)
	{
		assert (theBestSubgroup != null);

		//addToBuffer(aBestSubgroup);
		checkAndLog(theBestSubgroup, theOldCoverage);
	}

	// NOTE this is the original code, it remains for debugging
	private final void numericHalfIntervalsObsolete(BitSet theParentMembers, Refinement theRefinement)
	{
		// evaluateNumericRefinements() should prevent getting here for
		// NUMERIC_INTERVALS and NUMERIC_VIKAMINE_CONSECUTIVE_ALL|BEST
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		assert (aNumericStrategy == NumericStrategy.NUMERIC_ALL || aNumericStrategy == NumericStrategy.NUMERIC_BEST ||
				aNumericStrategy == NumericStrategy.NUMERIC_BINS || aNumericStrategy == NumericStrategy.NUMERIC_BEST_BINS);

		////////////////////////////////////////////////////////////////////////
		boolean isFilterNull = (itsFilter == null);
		Subgroup aParent = theRefinement.getSubgroup();
		int aParentCoverage = aParent.getCoverage();
		// should have been checked by evaluateNumericRefinements()
		assert (theParentMembers.cardinality() == aParentCoverage);
		assert (aParentCoverage > 1);

		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		Column aColumn = aConditionBase.getColumn();
		ConditionListA aParentConditions = (isFilterNull ? null : aParent.getConditions());
		Operator anOperator = aConditionBase.getOperator();

		// (cover-update and check) order relies on binary choice <= or >=
		assert (anOperator == Operator.LESS_THAN_OR_EQUAL || anOperator == Operator.GREATER_THAN_OR_EQUAL);

		// no useful Refinements are possible
		if (aColumn.getCardinality() <= 1)
			return;

		// might require update when more strategies are added
		boolean isAllStrategy = (aNumericStrategy == NumericStrategy.NUMERIC_ALL || aNumericStrategy == NumericStrategy.NUMERIC_BINS);

		Subgroup aBestSubgroup = null;
		////////////////////////////////////////////////////////////////////////

		float[] aDomain = getDomain(theParentMembers, aParentCoverage, aNumericStrategy, aColumn, itsSearchParameters.getNrBins(), anOperator);

		for (int i = 0, j = aDomain.length; i < j && !isTimeToStop(); ++i)
		{
			Condition aCondition = new Condition(aConditionBase, aDomain[i]);

			if (!isFilterNull && !itsFilter.isUseful(aParentConditions, aCondition))
				continue;

			Subgroup aNewSubgroup = aParent.getRefinedSubgroup(aCondition);

			if (isAllStrategy)
			{
				//addToBuffer(aNewSubgroup);
				checkAndLog(aNewSubgroup, aParentCoverage);
			}
			else
			{
				// more clear than using else-if
				if (isValidAndBest(aNewSubgroup, aParentCoverage, aBestSubgroup))
					aBestSubgroup = aNewSubgroup;
			}
		}

		if (!isAllStrategy && (aBestSubgroup != null))
			bestAdd(aBestSubgroup, aParentCoverage);
	}

	// called by numericIntervals() - it does not use value counts at the moment
	private static final float[] getDomain(BitSet theMembers, int theMembersCardinality, NumericStrategy theNumericStrategy, Column theColumn, int theNrBins, Operator theOperator)
	{
		switch (theNumericStrategy)
		{
			case NUMERIC_ALL	: return theColumn.getUniqueNumericDomainTest(theMembers, theMembersCardinality);
			case NUMERIC_BEST	: return theColumn.getUniqueNumericDomainTest(theMembers, theMembersCardinality);
			case NUMERIC_BINS	: return getUniqueSplitPoints(theMembers, theColumn, theNrBins-1, theOperator);
			case NUMERIC_BEST_BINS	: return getUniqueSplitPoints(theMembers, theColumn, theNrBins-1, theOperator);
//			case NUMERIC_BINS	: return theColumn.getUniqueSplitPoints(theMembers, theNrBins-1, theOperator);
//			case NUMERIC_BEST_BINS	: return theColumn.getUniqueSplitPoints(theMembers, theNrBins-1, theOperator);
			case NUMERIC_INTERVALS	: return theColumn.getUniqueNumericDomainTest(theMembers, theMembersCardinality);
			default :
				throw new AssertionError("invalid Numeric Strategy: " + theNumericStrategy);
		}
	}

	// see comment theColumn.getUniqueSplitPoints()
	private static final float[] getUniqueSplitPoints(BitSet theMembers, Column theColumn, int theNrSplits, Operator theOperator)
	{
		float[] aSplitPoints = theColumn.getUniqueSplitPoints(theMembers, theNrSplits, theOperator);

		// if new code is run, aSplitPoints is already filtered
		if (Column.USE_NEW_BINNING)
			return aSplitPoints;
		else
			return Column.getUniqueValues(aSplitPoints);
	}

	// TODO method is never used, but EQUALS and BETWEEN might benefit from the
	// technique, leave it in; current code already does these check for <=, >=
	private final void filterDomain(NavigableMap<Float, Integer> theSplitPoints, Operator theOperator, int theMinimumCoverage)
	{
		if (!(theOperator == Operator.EQUALS || theOperator == Operator.BETWEEN))
			throw new AssertionError("NOT IMPLEMENTED YET: " + theOperator);

		if (theOperator == Operator.EQUALS)
		{
			for (Iterator<Entry<Float, Integer>> it = theSplitPoints.entrySet().iterator(); it.hasNext(); )
				if (it.next().getValue() < theMinimumCoverage)
					it.remove();
		}
		else
		{
			// FIXME MM
			// perhaps no filtering needed in this setting
			// Interval code might take care of it itself
			// need to check code
			// Interval might be using values that can never
			// lead to a valid Subgroup
			// happens when Interval covers to few items
			int sum = 0;

			for (Iterator<Entry<Float, Integer>> it = theSplitPoints.entrySet().iterator(); it.hasNext(); )
				if ((sum += it.next().getValue()) > theMinimumCoverage)
					break; // could return

			if (sum < theMinimumCoverage)
				theSplitPoints.clear();
		}
	}
}
