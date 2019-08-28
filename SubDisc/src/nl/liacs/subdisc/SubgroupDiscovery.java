package nl.liacs.subdisc;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import javax.swing.*;

import nl.liacs.subdisc.ConditionListBuilder.ConditionListA;
import nl.liacs.subdisc.ConvexHull.HullPoint;
import nl.liacs.subdisc.gui.*;

public class SubgroupDiscovery
{
	// log slows down mining a lot, but leave NO_CANDIDATE_LOG at false in git
	private static final boolean NO_CANDIDATE_LOG = false;
	// leave TEMPORARY_CODE at false in git
	// when true, creates PMF instead of PDF in single numeric H^2 setting
	static boolean TEMPORARY_CODE = false;
	static int TEMPORARY_CODE_NR_SPLIT_POINTS = -1;
	static boolean TEMPORARY_CODE_USE_EQUAL_WIDTH = false;

	private final SearchParameters itsSearchParameters;
	private final Table itsTable;
	private final int itsNrRows;		// itsTable.getNrRows()
	private final int itsMinimumCoverage;	// itsSearchParameters.getMinimumCoverage();
	private final int itsMaximumCoverage;	// itsNrRows * itsSearchParameters.getMaximumCoverageFraction();

	private final QualityMeasure itsQualityMeasure;
	private final float itsQualityMeasureMinimum;	// itsSearchParameters.getQualityMeasureMinimum();
	private boolean ignoreQualityMinimum = false; //used for swap-randomization purposes, and to get random qualities

	private final SubgroupSet itsResult;
	private CandidateQueue itsCandidateQueue;
	private AtomicLong itsCandidateCount = new AtomicLong(0);

	//target concept type-specific information, including base models
	private BitSet itsBinaryTarget;		//SINGLE_NOMINAL
	private Column itsTargetRankings;	//SINGLE_NOMINAL (label ranking)
	private Column itsNumericTarget;	//SINGLE_NUMERIC
	private Column itsPrimaryColumn;	//DOUBLE_CORRELATION / DOUBLE_REGRESSION / SCAPE
	private Column itsSecondaryColumn;	//DOUBLE_CORRELATION / DOUBLE_REGRESSION / SCAPE
	private CorrelationMeasure itsBaseCM;	//DOUBLE_CORRELATION
	private RegressionMeasure itsBaseRM;	//DOUBLE_REGRESSION
	private BinaryTable itsBinaryTable;	//MULTI_LABEL
	private List<Column> itsTargets;	//MULTI_LABEL / MULTI_NUMERIC
	public ProbabilityDensityFunction_ND itsPDF_ND; //MULTI_NUMERIC

	private LocalKnowledge itsLocalKnowledge; //PROPENSITY SCORE BASED
	private GlobalKnowledge itsGlobalKnowledge;//PROPENSITY SCORE BASED

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
	private JFrame itsMainWindow; //for feeding back progress info

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

		BitSet aBitSet = getAllDataBitSet(itsNrRows);
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

		BitSet aBitSet = getAllDataBitSet(itsNrRows);
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

	static final BitSet getAllDataBitSet(int theNrRows)
	{
		BitSet aBitSet = new BitSet(theNrRows);
		aBitSet.set(0, theNrRows);
		return aBitSet;
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

	/*
	 * this method remains only for debugging purposes
	 * it should not be called by code outside this class
	 */
	private Filter itsFilter = null;
	@Deprecated
	private final void mine(long theBeginTime)
	{
		// not in Constructor, Table / SearchParameters may change
		final ConditionBaseSet aConditions = new ConditionBaseSet(itsTable, itsSearchParameters);

		//make subgroup to start with, containing all elements
		BitSet aBitSet = getAllDataBitSet(itsNrRows);
		Subgroup aStart = new Subgroup(ConditionListBuilder.emptyList(), aBitSet, itsResult);

		// set number of true positives for dataset
		if (isPOCSetting())
			aStart.setTertiaryStatistic(itsQualityMeasure.getNrPositives());

		itsCandidateQueue = new CandidateQueue(itsSearchParameters, new Candidate(aStart));

		int aSearchDepth = itsSearchParameters.getSearchDepth();

		// to print filter output use: DebugFilter(itsSearchParameters);
		itsFilter = new Filter(itsSearchParameters);

// TODO MM DEBUG only, set counts to 0
//RefinementList.COUNT.set(0);
//RefinementList.ADD.set(0);
		while ((itsCandidateQueue.size() > 0) && !isTimeToStop())
		{
			Candidate aCandidate = itsCandidateQueue.removeFirst(); // take off first Candidate from Queue
			Subgroup aSubgroup = aCandidate.getSubgroup();

			if (aSubgroup.getDepth() < aSearchDepth)
			{
//				RefinementList aRefinementList = new RefinementList(aSubgroup, itsTable, itsSearchParameters);
				RefinementList aRefinementList = new RefinementList(aSubgroup, aConditions);
//				RefinementList aRefinementList = new RefinementList(aSubgroup, aConditions, aSearchStrategy);
				// .getMembers() creates expensive clone, reuse
				final BitSet aMembers = aSubgroup.getMembers();

				for (int i = 0, j = aRefinementList.size(); i < j; i++)
				{
					if (isTimeToStop())
						break;

					Refinement aRefinement = aRefinementList.get(i);

					if (!itsFilter.isUseful(aRefinement))
						continue;

					ConditionBase aConditionBase = aRefinement.getConditionBase();
					// if refinement is (num_attr = value) then treat it as nominal
					// using EQUALS for numeric conditions is bad, see evaluateNominalBinaryRefinements()
					// evaluateNumericRefinements() should split code path for EQUALS and !EQUALS
					// only NUMERIC_BINS setting is affected
					if (aConditionBase.getColumn().getType() == AttributeType.NUMERIC && aConditionBase.getOperator() != Operator.EQUALS)
						evaluateNumericRefinements(aMembers, aRefinement);
					else
						evaluateNominalBinaryRefinements(aMembers, aRefinement);
				}
			}

			if (itsCandidateQueue.size() == 0)
				flushBuffer();
		}

// TODO MM DEBUG only, set counts to 0
//Log.logCommandLine("RefinementList.COUNT: " + RefinementList.COUNT);
//Log.logCommandLine("RefinementList.ADD: " + RefinementList.ADD);

		Log.logCommandLine("number of candidates: " + itsCandidateCount.get());
		if (itsSearchParameters.getQualityMeasure() == QM.COOKS_DISTANCE)
		{
			Log.logCommandLine("Bound seven computed " + getNrBoundSeven() + " times");
			Log.logCommandLine("Bound six   computed " + getNrBoundSix() + " times");
			Log.logCommandLine("Bound five  computed " + getNrBoundFive() + " times");
			Log.logCommandLine("Bound four  computed " + getNrBoundFour() + " times");
			Log.logCommandLine("Bound seven fired " + getNrBoundSevenFired() + " times");
			Log.logCommandLine("Bound six   fired " + getNrBoundSixFired() + " times");
			Log.logCommandLine("Bound five  fired " + getNrBoundFiveFired() + " times");
			Log.logCommandLine("Bound four  fired " + getNrBoundFourFired() + " times");
			Log.logCommandLine("Rank deficient models: " + getNrRankDef());
		}
		Log.logCommandLine("number of subgroups: " + getNumberOfSubgroups());

		itsResult.setIDs(); //assign 1 to n to subgroups, for future reference in subsets
		if ((itsSearchParameters.getTargetType() == TargetType.MULTI_LABEL) && itsSearchParameters.getPostProcessingDoAutoRun())
			postprocess();

		//now just for cover-based beam search post selection
		// TODO MM see note at SubgroupSet.postProcess(), all itsResults will remain in memory
		SubgroupSet aSet = itsResult.postProcess(itsSearchParameters.getSearchStrategy());
		// FIXME MM hack to deal with strange postProcess implementation
		if (itsResult != aSet)
		{
			// no reassign, we want itsResult to be final
			itsResult.clear();
			itsResult.addAll(aSet);
		}

		// in MULTI_LABEL, order may have changed
		// in COVER_BASED_BEAM_SELECTION, subgroups may have been removed
		itsResult.setIDs(); //assign 1 to n to subgroups, for future reference in subsets
		deleteSortData(itsTable.getColumns());
	}

	private void logExperimentSettings(ConditionBaseSet theConditionBaseSet)
	{
		Log.logCommandLine("");
		Log.logCommandLine(itsSearchParameters.getTargetConcept().toString());
		Log.logCommandLine(itsSearchParameters.toString());
		Log.logCommandLine(theConditionBaseSet.toString());
	}

	/*
	 * NOTE itsSubgroup / theMembers for each Refinement from the same
	 * RefinementList are always the same
	 * supply a cached version of theMembers, as Subgroup.getMembers()
	 * creates a clone on each call
	 * 
	 * TODO MM - for *_BEST strategies, in case of ties, multiple subgroups
	 * attaining the best score, this implementation retains only the first
	 * instead it could retain all best scoring subgroups
	 */
	private void evaluateNumericRefinements(BitSet theMembers, Refinement theRefinement)
	{
		assert (theRefinement.getSubgroup().getCoverage() == theMembers.cardinality());
//		assert (theRefinement.getSubgroup().getCoverage() > 1);
		// this subgroup can not be refined - FIXME MM make this an assert
		if (theRefinement.getSubgroup().getCoverage() <= 1)
			return;

		switch (itsSearchParameters.getNumericStrategy())
		{
			case NUMERIC_ALL :
				numericHalfIntervals(theMembers, theRefinement);
				break;
			// NUMERIC_BEST is like NUMERIC_ALL, but uses only best scoring subgroup
			case NUMERIC_BEST :
				numericHalfIntervals(theMembers, theRefinement);
				break;
			case NUMERIC_BINS :
				numericHalfIntervals(theMembers, theRefinement);
				break;
			// NUMERIC_BEST_BINS is like NUMERIC_BINS, but uses only best scoring subgroup
			case NUMERIC_BEST_BINS :
				numericHalfIntervals(theMembers, theRefinement);
				break;
			case NUMERIC_INTERVALS :
				numericIntervals(theMembers, theRefinement);
				break;
			case NUMERIC_VIKAMINE_CONSECUTIVE_ALL :
				numericConsecutiveBins(theMembers, theRefinement);
				break;
			// NUMERIC_VIKAMINE_CONSECUTIVE_BEST is like NUMERIC_VIKAMINE_CONSECUTIVE_ALL, but uses only best scoring subgroup
			case NUMERIC_VIKAMINE_CONSECUTIVE_BEST :
				numericConsecutiveBins(theMembers, theRefinement);
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

// FIXME MM - DEBUG ONLY
AtomicInteger TOTAL_FILTERED = new AtomicInteger(0);
	private final void numericHalfIntervals(BitSet theMembers, Refinement theRefinement)
	{
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		assert (aNumericStrategy == NumericStrategy.NUMERIC_ALL || aNumericStrategy == NumericStrategy.NUMERIC_BEST ||
			aNumericStrategy == NumericStrategy.NUMERIC_BINS || aNumericStrategy == NumericStrategy.NUMERIC_BEST_BINS);

		// might require update when more strategies are added
		boolean isAllStrategy = (aNumericStrategy == NumericStrategy.NUMERIC_ALL || aNumericStrategy == NumericStrategy.NUMERIC_BINS);

		Subgroup anOldSubgroup = theRefinement.getSubgroup();
		// faster than theMembers.cardinality()
		// useless call, coverage never changes, could be parameter
		final int anOldCoverage = anOldSubgroup.getCoverage();
		assert (theMembers.cardinality() == anOldCoverage);

		// see comment at evaluateNominalBinaryRefinement()
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		ConditionListA aList = anOldSubgroup.getConditions();

		// FIXME MM
		// temporary hack for better getSplitPoints(), see getDomain()
		Column aColumn = aConditionBase.getColumn();
		Operator anOperator = aConditionBase.getOperator();
		float[] aSplitPoints = getDomain(theMembers, anOldCoverage, aNumericStrategy, aColumn, itsSearchParameters.getNrBins(), anOperator);
// TODO MM
// domain map contain using <value, count> nodes, useful for unique values and
// for histogram/KDE creation (latter can multiply result for 1 point by count)
//		SortedMap<Float, Integer> map = getDomainMap(theMembers, aNumericStrategy, aConditionBase.getColumn(), itsSearchParameters.getNrBins());
//Float[] fa = map.keySet().toArray(new Float[0]);
//Integer[] ia = map.values().toArray(new Integer[0]);
//System.out.println(Arrays.toString(aSplitPoints));
//System.out.println(Arrays.toString(fa));
//System.out.println(Arrays.toString(ia));
//		filterDomain(map, aConditionBase.getOperator(), itsSearchParameters.getMinimumCoverage());
//		Float[] aSplitPointsMap = map.keySet().toArray(new Float[0]);
//
// TODO MM - like above (above is the svn version, decide what to use later)
//Set<Float> aSplitPoints = getDomain(theMembers, aNumericStrategy, aConditionBase.getColumn(), itsSearchParameters.getNrBins());
//NavigableMap<Float, Integer> map = getDomainMap(theMembers, aNumericStrategy, aConditionBase.getColumn(), itsSearchParameters.getNrBins());
//assert(aSplitPoints.length == map.size());
//int pre = map.size();
//System.out.println(aConditionBase.getOperator());
//System.out.println(map);
//filterDomain(map, aConditionBase.getOperator(), itsSearchParameters.getMinimumCoverage());
//System.out.println(map);
//int post = map.size();
//int reduced = pre-post;
//System.out.format("PRE: %d\tPOST: %d\tREDUCED: %d%nTOTAL_FILTERED: %d%n%n", pre, post, reduced, TOTAL_FILTERED.addAndGet(reduced));

		Subgroup aBestSubgroup = null;
//		for (float aSplitPoint : map.keySet())
		for (float aSplitPoint : aSplitPoints)
		{
			if (isTimeToStop())
				break;

			if (!isUseful(itsFilter, aList, new Condition(aConditionBase, aSplitPoint)))
				continue;

			Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aSplitPoint);

			if (isAllStrategy)
			{
				//addToBuffer(aNewSubgroup);
				checkAndLog(aNewSubgroup, anOldCoverage);
			}
			else
			{
				// more clear than using else-if
				if (isValidAndBest(aNewSubgroup, anOldCoverage, aBestSubgroup))
					aBestSubgroup = aNewSubgroup;
			}
		}

		// FIXME quality is already known, re-evaluation in check() is useless
		if (!isAllStrategy && (aBestSubgroup != null))
			bestAdd(aBestSubgroup, anOldCoverage);
	}

	private void numericIntervals(BitSet theMembers, Refinement theRefinement)
	{
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		assert (aNumericStrategy == NumericStrategy.NUMERIC_INTERVALS);

		Subgroup anOldSubgroup = theRefinement.getSubgroup();
		// faster than theMembers.cardinality()
		// useless call, coverage never changes, could be parameter
		final int anOldCoverage = anOldSubgroup.getCoverage();
		assert (theMembers.cardinality() == anOldCoverage);

		// see comment at evaluateNominalBinaryRefinement()
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		Column aColumn = aConditionBase.getColumn();

		float[] aSplitPoints = getDomain(theMembers, anOldCoverage, aNumericStrategy, aColumn, itsSearchParameters.getNrBins(), null);

		// FIXME MM Subgroup -> theMembers
		RealBaseIntervalCrossTable aRBICT = new RealBaseIntervalCrossTable(aSplitPoints, aColumn, theRefinement.getSubgroup(), itsBinaryTarget);

		// prune splitpoints for which adjacent base intervals have equal class distribution
		// TODO: check whether this preprocessing reduces *total* computation time
		aRBICT.aggregateIntervals();
		if (aRBICT.getNrSplitPoints() == 0)
			return; // no specialization improves quality

		double aBestQuality = Double.NEGATIVE_INFINITY;
		Interval aBestInterval = new Interval(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

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

			for (int i = 0; i < aRBICT.getNrBaseIntervals(); i++)
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
			@SuppressWarnings("unused")
			int anEvalCounter = 0; // debug counter
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
						for (int i = 0; i < aMinkDiff.getSize(aSide); i++)
						{
							if (aSide == 1 && (i == 0 || i == aMinkDiff.getSize(aSide)-1) )
								continue; // no need to check duplicate hull points
							HullPoint aCandidate = aMinkDiff.getPoint(aSide, i);
							double aQuality = itsQualityMeasure.calculate(aCandidate.itsY, (aCandidate.itsX + aCandidate.itsY));
							anEvalCounter++;
							if (aQuality > aBestQuality) {
								aBestQuality = aQuality;
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

			//Log.logCommandLine("Evalutations: " + anEvalCounter);
		}

		// FIXME quality is known, re-evaluation in check() should be avoided
		Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aBestInterval);
		checkAndLog(aNewSubgroup, anOldCoverage);
	}

	private void numericConsecutiveBins(BitSet theMembers, Refinement theRefinement)
	{
		NumericStrategy aNumericStrategy = itsSearchParameters.getNumericStrategy();
		assert (aNumericStrategy == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_ALL || aNumericStrategy == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_BEST);
		// might require update when more strategies are added
		boolean isAllStrategy = (aNumericStrategy == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_ALL);

		Subgroup anOldSubgroup = theRefinement.getSubgroup();
		// faster than theMembers.cardinality()
		// useless call, coverage never changes, could be parameter
		final int anOldCoverage = anOldSubgroup.getCoverage();
		assert (theMembers.cardinality() == anOldCoverage);

		// see comment at evaluateNominalBinaryRefinement()
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		ConditionListA aList = anOldSubgroup.getConditions();

		// this is the crucial translation from nr bins to nr splitpoints
		int aNrSplitPoints = itsSearchParameters.getNrBins() - 1;
		// useless, Column.getSplitPointsBounded() would return an empty array
		if (aNrSplitPoints <= 0)
			return;
		SortedSet<Interval> anIntervals = aConditionBase.getColumn().getUniqueSplitPointsBounded(theMembers, aNrSplitPoints);

		Subgroup aBestSubgroup = null;
		for (Interval anInterval : anIntervals)
		{
			if (isTimeToStop())
				break;

			// catch invalid input
			if (anInterval == null)
				continue;

			if (itsFilter != null)
				if (!itsFilter.isUseful(aList, new Condition(aConditionBase, anInterval)))
					continue;

			Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(anInterval);

			if (isAllStrategy)
			{
				//addToBuffer(aNewSubgroup);
				checkAndLog(aNewSubgroup, anOldCoverage);
			}
			else
			{
				// more clear than using else-if
				if (isValidAndBest(aNewSubgroup, anOldCoverage, aBestSubgroup))
					aBestSubgroup = aNewSubgroup;
			}
		}

		// FIXME quality is already known, re-evaluation in check() is useless
		if (!isAllStrategy && (aBestSubgroup != null))
			bestAdd(aBestSubgroup, anOldCoverage);
	}

	private static final float[] getDomain(BitSet theMembers, int theMembersCardinality, NumericStrategy theNumericStrategy, Column theColumn, int theNrBins, Operator theOperator)
	{
		switch (theNumericStrategy)
		{
			case NUMERIC_ALL	: return theColumn.getUniqueNumericDomain(theMembers, theMembersCardinality);
			case NUMERIC_BEST	: return theColumn.getUniqueNumericDomain(theMembers, theMembersCardinality);
			case NUMERIC_BINS	: return getUniqueSplitPoints(theMembers, theColumn, theNrBins-1, theOperator);
			case NUMERIC_BEST_BINS	: return getUniqueSplitPoints(theMembers, theColumn, theNrBins-1, theOperator);
//			case NUMERIC_BINS	: return theColumn.getUniqueSplitPoints(theMembers, theNrBins-1, theOperator);
//			case NUMERIC_BEST_BINS	: return theColumn.getUniqueSplitPoints(theMembers, theNrBins-1, theOperator);
			case NUMERIC_INTERVALS	: return theColumn.getUniqueNumericDomain(theMembers, theMembersCardinality);
			default :
				throw new AssertionError("invalid Numeric Strategy: " + theNumericStrategy);
		}
	}

	// FIXME MM profiling will determine which versions to choose
	private static final SortedMap<Float, Integer> getDomainMap(BitSet theMembers, int theMembersCardinality, NumericStrategy theNumericStrategy, Column theColumn, int theNrBins, Operator theOperator)
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

	private final void filterDomain(NavigableMap<Float, Integer> theSplitPoints, Operator theOperator, int theMinimumCoverage)
	{
		// switch in for-loop is expensive, so check Operator only once
		switch (theOperator)
		{
			case EQUALS :
			{
				for (Iterator<Entry<Float, Integer>> it = theSplitPoints.entrySet().iterator(); it.hasNext(); )
					if (it.next().getValue() < theMinimumCoverage)
						it.remove();

				break;
			}
			case LESS_THAN_OR_EQUAL :
			{
				int sum = 0;

				for (Iterator<Entry<Float, Integer>> it = theSplitPoints.entrySet().iterator(); it.hasNext(); )
				{
					if ((sum += it.next().getValue()) < theMinimumCoverage)
						it.remove();
					else
						break;
				}

				break;
			}
			case GREATER_THAN_OR_EQUAL :
			{
				filterDomain(theSplitPoints.descendingMap(), Operator.LESS_THAN_OR_EQUAL, theMinimumCoverage);
				break;
			}
			case BETWEEN :
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

				throw new AssertionError("NOT IMPLEMENTED YET: " + theOperator);
				//break;
			}
			default :
				throw new AssertionError("invalid Operator: " + theOperator);
		}
	}

	private static final boolean isUseful(Filter theFilter, ConditionListA theConditionList, Condition theCondition)
	{
		return ((theFilter == null) || theFilter.isUseful(theConditionList, theCondition));
	}

	private final boolean isValidAndBest(Subgroup theNewSubgroup, int theOldCoverage, Subgroup theBestSubgroup)
	{
		final int aNewCoverage = theNewSubgroup.getCoverage();

		// FIXME MM the first and third check should be made obsolete
		// numericConsecutive bins should check this, then they can be removed
		// is theNewSubgroup valid
		if (aNewCoverage >= itsMinimumCoverage && aNewCoverage <= itsMaximumCoverage && aNewCoverage < theOldCoverage)
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

	private final void bestAdd(Subgroup theBestSubgroup, int theOldCoverage)
	{
		assert (theBestSubgroup != null);

		//addToBuffer(aBestSubgroup);
		checkAndLog(theBestSubgroup, theOldCoverage);
	}

	/*
	 * NOTE itsSubgroup / theMembers for each Refinement from the same
	 * RefinementList are always the same
	 * supply a cached version of theMembers, as Subgroup.getMembers()
	 * creates a clone on each call
	 */
	private void evaluateNominalBinaryRefinements(BitSet theMembers, Refinement theRefinement)
	{
		Subgroup aParent = theRefinement.getSubgroup();
		int aParentCoverage = aParent.getCoverage();
		assert (theMembers.cardinality() == aParentCoverage);

		// this subgroup can not be refined - FIXME MM make this an assert
		if (aParentCoverage <= 1)
			return;

		ConditionBase aConditionBase = theRefinement.getConditionBase();

		// this code path deliberately does not check isTimeToStop()
		if (aConditionBase.getOperator() == Operator.ELEMENT_OF)
		{
			// set-valued, implies target type is SINGLE_NOMINAL
			assert (itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL);

			NominalCrossTable aNCT = new NominalCrossTable(aConditionBase.getColumn(), theMembers, itsBinaryTarget);
			final SortedSet<String> aDomainBestSubSet = new TreeSet<String>();

			final QM aQualityMeasure = itsSearchParameters.getQualityMeasure();
			if (aQualityMeasure == QM.WRACC)
			{
				// FIXME MM floats are imprecise for data with N > 2^24
				float aRatio = itsQualityMeasure.getNrPositives() / (float)(itsQualityMeasure.getNrRecords());
				for (int i = 0; i < aNCT.size(); i++)
				{
					int aPi = aNCT.getPositiveCount(i);
					int aNi = aNCT.getNegativeCount(i);
					// include values with WRAcc=0 too, result has same WRAcc but higher support
					if (aPi >= aRatio * (aPi + aNi))
						aDomainBestSubSet.add(aNCT.getValue(i));
				}
			}
			else // not WRACC
			{
				// construct and check all subsets on the convex hull
				final List<Integer> aSortedDomainIndices = aNCT.getSortedDomainIndices();
				final int aSortedDomainIndicesSize = aSortedDomainIndices.size();
				double aBestQuality = Double.NEGATIVE_INFINITY;

				// upper part of the hull
				int aP = 0;
				int aN = 0;
				int aPrevBestI = -1;
				for (int i = 0; i < aSortedDomainIndicesSize - 1; i++)
				{
					int anIndex = aSortedDomainIndices.get(i);
					int aPi = aNCT.getPositiveCount(anIndex);
					int aNi = aNCT.getNegativeCount(anIndex);
					aP += aPi;
					aN += aNi;
					int aNextIndex = aSortedDomainIndices.get(i+1);
					if (i < aSortedDomainIndicesSize-2 && aPi * aNCT.getNegativeCount(aNextIndex) == aNCT.getPositiveCount(aNextIndex) * aNi) // skip checking degenerate hull points
						continue;
					double aQuality = itsQualityMeasure.calculate(aP, aP + aN);
					if (aQuality > aBestQuality)
					{
						aBestQuality = aQuality;
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
					for (int i = aSortedDomainIndicesSize - 1; i > 0; i--)
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
			}

			if (aDomainBestSubSet.size() != 0)
			{
				// FIXME MM this does not take maximum coverage into account
				// the best ValueSet might be > maxCov, and thus not be used
				// but when this is the last search level, the refined version
				// will also never be created
				// a better solution is to obtain the best VALID ValueSet for
				// this level, add it to the result set (possibly), and add the
				// BEST (valid or not) candidate to the CandidateQueue
				// and then never refine a nominal attribute with itself
				// FIXME quality is known, re-evaluation should be avoided
				final ValueSet aBestSubset = new ValueSet(aDomainBestSubSet);
				Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aBestSubset);
				valueSetCheckAndLog(aNewSubgroup, aParentCoverage);
			}
		}
		else // single-value conditions (class labels)
		{
			// FIXME: NUMERIC Refinement should not be done here
			switch (aConditionBase.getColumn().getType())
			{
				case NOMINAL : evaluateNominalRefinements(theMembers, theRefinement, aParentCoverage); break;
				case NUMERIC : evaluateNumericEqualsRefinements(theMembers, theRefinement, aParentCoverage); break;
				case ORDINAL : throw new AssertionError(AttributeType.ORDINAL);
				case BINARY  : evaluateBinaryRefinements(theMembers, theRefinement, aParentCoverage); break;
				default      : throw new AssertionError(aConditionBase.getColumn().getType());
			}
		}
	}

	private final void evaluateNominalRefinements(final BitSet theParentMembers, final Refinement theRefinement, int theParentCoverage)
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

		String[] aDomain = aColumn.getUniqueNominalBinaryDomain(theParentMembers);

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

	// XXX (c = false) is checked first, (c = true) is conditionally, it depends
	//       on data and search characteristics  whether this is the best order
	private final void evaluateBinaryRefinements(final BitSet theParentMembers, final Refinement theRefinement, int theParentCoverage)
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

		// no useful Refinements are possible 
		if (aColumn.getCardinality() != 2)
			return;

		BitSet aChildMembers = aColumn.evaluateBinary(theParentMembers, false);
		int aChildCoverage = (aChildMembers == null ? 0 : aChildMembers.cardinality());

		// ignore both f and t
		if ((aChildCoverage == 0) || (aChildCoverage == theParentCoverage))
			return;

		// check for (aColumn = false)
		if (aChildCoverage >= itsMinimumCoverage)
			evaluateBinary(aParent, new Condition(aConditionBase, false), aChildMembers, aChildCoverage, itsFilter, aParentConditions);

		// binary check is fast, but for some models evaluateCandidate() is not
		if (isTimeToStop())
			return;

		// check for (aColumn = true), do this only when useful
		// everything that is not positive is negative
		aChildCoverage = (theParentCoverage - aChildCoverage);
		if (aChildCoverage >= itsMinimumCoverage)
		{
			aChildMembers = aColumn.evaluateBinary(theParentMembers, true);
			evaluateBinary(aParent, new Condition(aConditionBase, true), aChildMembers, aChildCoverage, itsFilter, aParentConditions);
		}
	}

	private final boolean isDirectSettingBinary()
	{
		// checking this all the time is a bit wasteful, but fine for now
		return ((itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL) &&
				!itsSearchParameters.getNominalSets() &&
				!(itsSearchParameters.getQualityMeasure() == QM.PROP_SCORE_WRACC) || (itsSearchParameters.getQualityMeasure() == QM.PROP_SCORE_RATIO));
	}

	private final void evaluateBinary(Subgroup theParent, Condition theAddedCondition, BitSet theChildMembers, int theChildCoverage, Filter theFilter, ConditionListA theParentConditionList)
	{
		// evaluateNominalRefinements() should prevent getting here for ValueSet
		assert (!itsSearchParameters.getNominalSets());
		assert (theChildMembers.cardinality() == theChildCoverage);
		assert (theChildCoverage > 0);
		// must both be non-null, or both be null
		assert (!((theFilter == null) ^ (theParentConditionList == null)));

		final Subgroup aChildSubgroup;

		if (isDirectSettingBinary())
		{
			// safe: it is a clone, and subgroup coverage is stored: theCoverage
			theChildMembers.and(itsBinaryTarget);
			int aTruePositives = theChildMembers.cardinality();

			aChildSubgroup = directComputation(theParent, theAddedCondition, itsQualityMeasure, theChildCoverage, aTruePositives);
		}
		else
		{
			if ((theFilter != null) && !theFilter.isUseful(theParentConditionList, theAddedCondition))
				return;

			aChildSubgroup = theParent.getRefinedSubgroup(theAddedCondition, theChildMembers, theChildCoverage);
		}

		checkAndLog(aChildSubgroup, theParent.getCoverage());
	}

	private static final Subgroup directComputation(Subgroup theParent, Condition theAddedCondition, QualityMeasure theQualityMeasure, int theChildCoverage, int theTruePositives)
	{
		// FIXME MM q is only cast to float to make historic results comparable
		float q = (float) theQualityMeasure.calculate(theTruePositives, theChildCoverage);
		double s = ((double) theTruePositives)/theChildCoverage;
		double t = ((double) theTruePositives);

		return theParent.getRefinedSubgroup(theAddedCondition, q, s, t, theChildCoverage);
	}

	// shares a lot of code with evaluateNominalRefinements(), but will change
	private final void evaluateNumericEqualsRefinements(final BitSet theParentMembers, final Refinement theRefinement, int theParentCoverage)
	{
		// NUMERIC Refinement in evaluateNominalBinaryRefinements is a weird
		// setting, this check is irrelevant for NUMERIC Columns
		// evaluateNominalRefinements() should prevent getting here for ValueSet
		//assert (!itsSearchParameters.getNominalSets());
		// should have been checked by evaluateNominalBinaryRefinements()
		assert (theParentMembers.cardinality() == theParentCoverage);
		assert (theParentCoverage > 1);

		boolean isFilterNull = (itsFilter == null);
		Subgroup aParent = theRefinement.getSubgroup();
		// members-based domain, no empty Subgroups will occur
		ConditionBase aConditionBase = theRefinement.getConditionBase();
		Column aColumn = aConditionBase.getColumn();
		ConditionListA aParentConditions = (isFilterNull ? null : aParent.getConditions());

		float[] aDomain = aColumn.getUniqueNumericDomain(theParentMembers);

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

//	/*
//	 * keep output together using synchronized method
//	 * NOTE other threads calling this method are stalled for a while when
//	 * checkAndLog().check() needs to resize itsCandidateQueue/ itsResult
//	 *
//	 * NOTE that in case of ties on the itsCandidateQueue/ itsResult
//	 * max_size boundary this may effect the final search result
//	 * this is related to to the fixed max size and has the potential to
//	 * break invocation invariant results in multi-threaded settings
//	 */
//	// FIXME MM - since whole method is synchronised, evaluateCandidate()
//	// call in checkAndLog() is also, severely slowing down multi-threaded
//	// execution, especially when the models are computationally expensive
//	// there seems not need for a separate method for ValueSets when the
//	// printing of (ignored) values is omitted
//	private final synchronized void valueSetCheckAndLog(ValueSet theBestSubset, Subgroup theNewSubgroup, int theOldCoverage)
//	{
//		boolean isValid = checkAndLog(theNewSubgroup, theOldCoverage);
//
//		// required? - prevents multi-threaded evaluateCandidate()
//		String pre = isValid ? "  values: " : "ignored subgroup with values: ";
//
//		Log.logCommandLine(pre + theBestSubset.toString());
//	}
	private final void valueSetCheckAndLog(Subgroup theNewSubgroup, int theOldCoverage)
	{
		boolean isValid = checkAndLog(theNewSubgroup, theOldCoverage);

		// if (isValid), checkAndLog() already printed the Subgroup
		// for historic reasons, the ignored Subgroup is printed anyway
		// FIXME MM remove this special case and valueSetCheckAndLog()
		if (!isValid)
			Log.logCommandLine("ignored subgroup: " + theNewSubgroup.getConditions().toString());
	}

	// this method increments itsCandidateCount
	private final boolean checkAndLog(Subgroup theSubgroup, int theOldCoverage)
	{
		setTitle(theSubgroup);

		boolean isValid = check(theSubgroup, theOldCoverage);

		// incrementing after expensive check() makes subgroup numbers
		// in log 'closer to being consecutive' when multi-threading
		// a synchronized block with itsCandidateCount.getAndIncrement()
		// and logCandidateAddition() would yield consecutive numbers
		// but is slower and does not yield useful practical benefits
		long count = itsCandidateCount.getAndIncrement();

		if (isValid)
			logCandidateAddition(theSubgroup, count);

		return isValid;
	}

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
	private boolean check(Subgroup theSubgroup, int theOldCoverage)
	{
		// FIXME MM this check should be made obsolete
		int aNewCoverage = theSubgroup.getCoverage();
		boolean isValid = (aNewCoverage < theOldCoverage && aNewCoverage >= itsMinimumCoverage);

		if (isValid)
		{
			NumericStrategy ns = itsSearchParameters.getNumericStrategy();
			AttributeType lastAdded = theSubgroup.getConditions().get(theSubgroup.getDepth()-1).getColumn().getType();

			float aQuality;

			if (isPOCSetting() && (lastAdded == AttributeType.NUMERIC))
			{
				// NOTE this path already performed the isValid-coverage check
				aQuality = (float) theSubgroup.getMeasureValue();
			}
			else if (isDirectSettingBinary() && (lastAdded == AttributeType.BINARY))
			{
				// NOTE this path already performed the isValid-coverage check
				aQuality = (float) theSubgroup.getMeasureValue();
			}
			else if (ns == NumericStrategy.NUMERIC_BEST || ns == NumericStrategy.NUMERIC_BEST_BINS || ns == NumericStrategy.NUMERIC_VIKAMINE_CONSECUTIVE_BEST)
			{
				// NOTE for BEST* Subgroup is already evaluated and score is set
				//      by isValidAndBest()
				aQuality = (float) theSubgroup.getMeasureValue();
			}
			else if (false)
			{
				// FIXME BestInterval and BestValueset also already computed quality
			}
			else
			{
				aQuality = evaluateCandidate(theSubgroup);
				theSubgroup.setMeasureValue(aQuality);
			}

// FIXME MM check size is > 1, otherwise Candidate can never be refined anyway
			Candidate aCandidate = new Candidate(theSubgroup);

			boolean aResultAddition = false;
			// if quality should be ignored or is enough
			// and the coverage is not too high
			if ((ignoreQualityMinimum || aQuality > itsQualityMeasureMinimum) && (aNewCoverage <= itsMaximumCoverage))
				aResultAddition = true;

			// all logic is performed outside of synchronized block
			// to keep it as small as possible
			synchronized (itsCheckLock)
			{
				if (aResultAddition)
					itsResult.add(theSubgroup);

				itsCandidateQueue.add(aCandidate);
			}
		}

		// prevent OutOfMemory / GC Overhead Limit errors, some code paths
		// bypass evaluateCandidate(Subgroup) so calling it there is no good
		// and this is the sole method to add to Candidate and Result sets
		theSubgroup.killMembers();
		return isValid;
	}

//	private long check(Subgroup theSubgroup, int theOldCoverage)
//	{
//		final long count = itsCandidateCount.getAndIncrement();
//		final int aNewCoverage = theSubgroup.getCoverage();
//
//		Subgroup addToResults = null;
//		Subgroup addToCandidates = null;
//		boolean isValidCandidate = (aNewCoverage < theOldCoverage && aNewCoverage >= itsMinimumCoverage);
//
//		if (isValidCandidate)
//		{
//			// evaluations for complex models are expensive
//			// so this should not be done in a synchronised block
//			float aQuality = evaluateCandidate(theSubgroup);
//			theSubgroup.setMeasureValue(aQuality);
//
//			// if the quality should be ignored, or is good enough
//			// and the coverage is not too high
//			if ((ignoreQualityMinimum || aQuality > itsQualityMeasureMinimum) && (aNewCoverage <= itsMaximumCoverage))
//				addToResults = theSubgroup;
//
//			// always add to candidates
//			addToCandidates = theSubgroup;
//		}
//
//		// a synchronised call
//		addToResultsAndCandidates(count, addToResults, addToCandidates);
//
//		// only log addition 
//		return isValidCandidate ? count : DO_NOT_LOG;
//	}

//	// unsynchronized member field, should only be used by this method
//	// alternatively, a Concurrent Collection could be used that would
//	// allow each thread to insert a new item in the waiting list
//	// and a separate thread would be notified upon each insertion
//	// that thread would be responsible for checking the head of the list
//	// and insert into the result and candidate list
//	// but then a lock on the collection MUST be obtained before executing
//	// the [firstEntry() + pollFirstEntry()] checks as a single logical call
//	// to avoid TOCTOU bugs (time-of-check-time-of-use)
//	private long itsLastCount = itsCandidateCount.get()-1L; // same start point
//	private final TreeMap<Long, Subgroup[]> itsWaitingList = new TreeMap<Long, Subgroup[]>();
//	private synchronized void addToResultsAndCandidates(long theCount, Subgroup addToResults, Subgroup addToCandidates)
//	{
//		if (theCount == (itsLastCount+1L))
//		{
//			if (addToResults != null)
//				itsResult.add(addToResults);
//			if (addToCandidates != null)
//				itsCandidateQueue.add(new Candidate(addToCandidates));
//			itsLastCount = theCount;
//
//			// synchronized methods use reentrant locks
//			// so no other thread can modify itsWaintingList
//			Entry<Long, Subgroup[]> e = itsWaitingList.firstEntry();
//			if (e != null)
//			{
//				// recursively check if next item can be processed now
//				if (e.getKey() == (itsLastCount+1L))
//				{
//					e = itsWaitingList.pollFirstEntry();
//					addToResultsAndCandidates(e.getKey(), e.getValue()[0], e.getValue()[1]);
//				}
//			}
//		}
//		else
//			itsWaitingList.put(theCount, new Subgroup[] { addToResults, addToCandidates });
//	}

	// because of multi-theading consecutive log calls should be grouped
	// else logs from other threads could end up in between
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

	private float evaluateCandidate(Subgroup theNewSubgroup)
	{
		// theNewSubgroup.killMembers() is called at end of check(), not here
		BitSet aMembers = theNewSubgroup.getMembers();
		float aQuality = 0.0f;

		switch (itsSearchParameters.getTargetType())
		{
			case SINGLE_NOMINAL :
			{
				int aCoverage = theNewSubgroup.getCoverage();
				// NOTE aMembers is a clone so this is safe
				aMembers.and(itsBinaryTarget);
				int aCountHeadBody = aMembers.cardinality();
// FIXME MM - hold: functional change + needs testing / profiling
//				final int aCountHeadBody = theNewSubgroup.countCommon(itsBinaryTarget);
				QM aMeasure = itsSearchParameters.getQualityMeasure();

				//Rob
				if ((aMeasure == QM.PROP_SCORE_WRACC) || (aMeasure == QM.PROP_SCORE_RATIO))
				{
					PropensityScore aPropensityScore = new PropensityScore(theNewSubgroup, itsBinaryTarget, itsLocalKnowledge, itsGlobalKnowledge, PropensityScore.LOGISTIC_REGRESSION);
//					double[] aScores = aPropensityScore.getPropensityScore();
					double aCountHeadPropensityScore = aPropensityScore.getPropensityScoreSum();
					System.out.println("Evaluating subgroup");
					//System.out.println(itsBinaryTarget.cardinality());
//					double aSumTest = 0.0;
//					for (int i = aMembers.nextSetBit(0); i >= 0; i = aMembers.nextSetBit(i+1))
//					{
//						aCountHeadPropensityScore += aScores[i];
//						//if (aPropensityScore.getPropensityScore()[i]>0.24){
//						//	aSumTest++;
//						//}
//						// count propensity score for all points in subgroup (aMembers)
//						if (itsBinaryTarget.get(i))
//							++aCountHeadBody;
//					}
					System.out.print("Count head:");
					System.out.println(aCountHeadBody);
					System.out.print("Count expected head:");
					System.out.println(aCountHeadPropensityScore);
					//System.out.print("Propensity score bigger than PT:");
					//System.out.println(aSumTest);
					//double aSum =0; // small check for propensity score (should sum to #target)
					//for (int i=0;i<aPropensityScore.getPropensityScore().length;i++){
					//	aSum = aSum+ aPropensityScore.getPropensityScore()[i];
					//}
					//System.out.println("Sum propensity score");
					//System.out.println(aSum);
					aQuality = QualityMeasure.calculatePropensityBased(aMeasure, aCountHeadBody, aCoverage, itsNrRows , aCountHeadPropensityScore);
				}

				//TODO: is this special case still needed?
				// FIXME MM - this creates a new set FOR EVERY
				// tested nominal Subgroup, killing performance
				// also LABEL_RANKING is a separate TargetType
				// see case below
//				else if ( QM.getQualityMeasures(TargetType.LABEL_RANKING).contains(aMeasure) )
//				{
//					LabelRankingMatrix aLRM = itsTargetRankings.getAverageRankingMatrix(theNewSubgroup);
//					aQuality = itsQualityMeasure.computeLabelRankingDistance(aCoverage, aLRM);
//					theNewSubgroup.setLabelRanking(itsTargetRankings.getAverageRanking(theNewSubgroup)); //store the average ranking for later reference
//					theNewSubgroup.setLabelRankingMatrix(aLRM); //store the matrix for later reference
//				}
				else //normal SINGLE_NOMINAL
					aQuality = (float) itsQualityMeasure.calculate(aCountHeadBody, aCoverage);

				theNewSubgroup.setSecondaryStatistic(aCountHeadBody / (double) aCoverage); //relative occurence of positives in subgroup
				theNewSubgroup.setTertiaryStatistic(aCountHeadBody); //count of positives in the subgroup
				break;
			}
			case SINGLE_NUMERIC :
			{
if (!TEMPORARY_CODE)
{
				// what needs to be calculated for this QM
				// NOTE requiredStats could be a final SubgroupDiscovery.class member
				Set<Stat> aRequiredStats = QM.requiredStats(itsSearchParameters.getQualityMeasure());
				//Statistics aStatistics = itsNumericTarget.getStatistics(aMembers, aRequiredStats); // TODO MM - implement better solution than below two checks
				Statistics aStatistics = itsNumericTarget.getStatistics(aMembers, itsSearchParameters.getQualityMeasure() == QM.MMAD, true);
				ProbabilityDensityFunction aPDF = null;
				if (aRequiredStats.contains(Stat.PDF))
				{
// FIXME MM TEMP
System.out.format("#Subgroup: '%s' (size = %d)%n", theNewSubgroup, theNewSubgroup.getCoverage());
// DEBUG
if (!ProbabilityDensityFunction.USE_ProbabilityDensityFunction2)
	aPDF = new ProbabilityDensityFunction(itsQualityMeasure.getProbabilityDensityFunction(), aMembers);
else
	aPDF = new ProbabilityDensityFunction2(itsQualityMeasure.getProbabilityDensityFunction(), aMembers);
					aPDF.smooth();
				}

				aQuality = itsQualityMeasure.calculate(aStatistics, aPDF);
				theNewSubgroup.setSecondaryStatistic(aStatistics.getSubgroupAverage());
				theNewSubgroup.setTertiaryStatistic(aStatistics.getSubgroupStandardDeviation());
				break;
}
else
{
// FIXME MM TEMP
System.out.format("#Subgroup: '%s' (size = %d)%n", theNewSubgroup, theNewSubgroup.getCoverage());
	ProbabilityMassFunction_ND aPMF = new ProbabilityMassFunction_ND((ProbabilityMassFunction_ND) itsQualityMeasure.getProbabilityDensityFunction(), aMembers);
	aQuality = itsQualityMeasure.calculate(new Statistics(theNewSubgroup.getCoverage(), -1, -1, -1, -1), aPMF);
	theNewSubgroup.setSecondaryStatistic(theNewSubgroup.getCoverage());
	theNewSubgroup.setTertiaryStatistic(itsTable.getNrRows()-theNewSubgroup.getCoverage());
	break;
}
			}
			case MULTI_NUMERIC :
			{
				// for now always test against complement
				aQuality = itsPDF_ND.getDensityDifference(aMembers, true, itsSearchParameters.getQualityMeasure());
				// for now set just the sizes, most computations
				// are expensive and neglected anyway
				theNewSubgroup.setSecondaryStatistic(theNewSubgroup.getCoverage());
				theNewSubgroup.setTertiaryStatistic(itsNrRows-theNewSubgroup.getCoverage());
				break;
			}
			case DOUBLE_REGRESSION :
			{
				switch (itsBaseRM.itsQualityMeasure)
				{
					case REGRESSION_SSD_COMPLEMENT:
					case REGRESSION_SSD_DATASET:
					case REGRESSION_FLATNESS:
					case REGRESSION_SSD_4:
					{
						RegressionMeasure aRM = new RegressionMeasure(itsBaseRM, aMembers);
						aQuality = (float) aRM.getEvaluationMeasureValue();
						theNewSubgroup.setSecondaryStatistic(aRM.getSlope()); //slope
						theNewSubgroup.setTertiaryStatistic(aRM.getIntercept()); //intercept
						break;
					}
/*
 * LEAVE THIS CODE IN, it will be used one day
					case QualityMeasure.COOKS_DISTANCE:
					{
						// initialize variables
						double aThreshold = -Double.MAX_VALUE;
						boolean aNeedToComputeRegression = true;
						boolean aNeedToComputeBounds = true;

						// check what the pruning quality will be, if this exists at all
						int aBorderlineSubgroupNumber;
						if (theNewSubgroup.itsDepth < itsSearchParameters.getSearchDepth())
							aBorderlineSubgroupNumber = itsSearchParameters.getSearchStrategyWidth();
						else aBorderlineSubgroupNumber = itsSearchParameters.getMaximumSubgroups();
						// TODO these methods on itsResult are not (yet) thread save and will will cause
						// problems during concurrent access, easy to fix
						if ( itsResult.size() >= aBorderlineSubgroupNumber )
							aThreshold = itsResult.last().getMeasureValue();
						else { aNeedToComputeBounds = false; }

						// start actual computation
						Log.logCommandLine("");
						int aSampleSize = aMembers.cardinality();

						// filter out rank deficient model that crash matrix multiplication library // TODO: should read <itsP instead of <2!!!
						if (aSampleSize<2)
						{
							itsRankDefCount++;
							return -Float.MAX_VALUE;
						}

						itsBaseRM.computeRemovedIndices(aMembers, aSampleSize);

						// calculate the upper bound values. Before each bound, only the necessary computations are done.
						if (aNeedToComputeBounds)
						{
							double aT = itsBaseRM.getT(aSampleSize);
							double aRSquared = itsBaseRM.getRSquared(aSampleSize);

							// bound seven
							double aBoundSeven = itsBaseRM.computeBoundSeven(aSampleSize, aT, aRSquared);
							if (aBoundSeven<Double.MAX_VALUE)
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
								if (aBoundSix<Double.MAX_VALUE)
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
									if (aBoundFive<Double.MAX_VALUE)
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
										if (aBoundFour<Double.MAX_VALUE)
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
							double aDoubleQuality = itsBaseRM.calculate(theNewSubgroup);
							if (aDoubleQuality == -Double.MAX_VALUE)
								itsRankDefCount++;
							aQuality = (float) aDoubleQuality;
						}
						else aQuality = -Float.MAX_VALUE;
					}
*/
				}
				break;
			}
			case DOUBLE_CORRELATION :
			{
				CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);

				for (int i = aMembers.nextSetBit(0); i >= 0; i = aMembers.nextSetBit(i+1))
					aCM.addObservation(itsPrimaryColumn.getFloat(i), itsSecondaryColumn.getFloat(i));
				//for (int i = 0; i < itsNrRows; i++)
				//	if (aMembers.get(i))
				//		aCM.addObservation(itsPrimaryColumn.getFloat(i), itsSecondaryColumn.getFloat(i));
				theNewSubgroup.setSecondaryStatistic(aCM.getCorrelation()); //correlation
				theNewSubgroup.setTertiaryStatistic(aCM.computeCorrelationDistance()); //intercept
				aQuality = (float) aCM.getEvaluationMeasureValue();
				break;
			}
			case SCAPE :
			{
				int aCoverage = theNewSubgroup.getCoverage();
				// NOTE aMembers is a clone so this is safe
				aMembers.and(itsPrimaryColumn.getBinaries());
				int aCountHeadBody = aMembers.cardinality();
// FIXME MM - hold: functional change
//				int aCountHeadBody = theNewSubgroup.countCommon(itsPrimaryColumn.getBinaries());

				aQuality = itsQualityMeasure.calculate(theNewSubgroup.getMembers(), aCoverage, aCountHeadBody);
				theNewSubgroup.setSecondaryStatistic(aCountHeadBody); //count of positives in the subgroup
				theNewSubgroup.setTertiaryStatistic(aCoverage - aCountHeadBody); //count of negatives in the subgroup

				break;
			}
			case MULTI_LABEL :
			{
// FIXME multiLabelCalculate(Subgroup) uses Subgroup.getMembers() to select a
// subset of records from itsBinaryTable, supply BitSet as parameter, then all
// methods use BitSet aMembers, theNewSubgroup.killMembers() is called once at
// the end for evaluateCandidate(), and it is more clear why
				aQuality = multiLabelCalculate(theNewSubgroup); //also stores DAG in Subgroup
				theNewSubgroup.setSecondaryStatistic(itsQualityMeasure.calculateEditDistance(theNewSubgroup.getDAG())); //edit distance
				theNewSubgroup.setTertiaryStatistic(QualityMeasure.calculateEntropy(itsNrRows, theNewSubgroup.getCoverage())); //entropy
				break;
			}
			case LABEL_RANKING :
			{
// FIXME getAverageRankingMatrix(Subgroup) and getAverageRanking(Subgroup) only
// uses Subgroup.getMembers(), supply BitSet as sole parameter, then all
// methods use BitSet aMembers, theNewSubgroup.killMembers() is called once at
// the end for evaluateCandidate(), and it is more clear why
				int aCoverage = theNewSubgroup.getCoverage();
				LabelRankingMatrix aLRM = itsTargetRankings.getAverageRankingMatrix(theNewSubgroup);
				aQuality = itsQualityMeasure.computeLabelRankingDistance(aCoverage, aLRM);
				theNewSubgroup.setLabelRanking(itsTargetRankings.getAverageRanking(theNewSubgroup)); //store the average ranking for later reference
				theNewSubgroup.setLabelRankingMatrix(aLRM); //store the matrix for later reference

				//TODO: make this more sensible
				//theNewSubgroup.setSecondaryStatistic(aCountHeadBody / (double) aCoverage); //relative occurence of positives in subgroup
				//theNewSubgroup.setTertiaryStatistic(aCountHeadBody); //count of positives in the subgroup
				break;
			}
			default : throw new AssertionError(itsSearchParameters.getTargetType());
		}

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
	private float multiLabelCalculate(Subgroup theSubgroup)
	{
		BinaryTable aBinaryTable = itsBinaryTable.selectRows(theSubgroup.getMembers());
		Bayesian aBayesian = new Bayesian(aBinaryTable, itsTargets);
		aBayesian.climb(); //induce DAG
		DAG aDAG = aBayesian.getDAG();
		theSubgroup.setDAG(aDAG); //store DAG with subgroup for later use
		return itsQualityMeasure.calculate(theSubgroup);
	}

	private void postprocess()
	{
		if (itsResult.isEmpty())
			return;

		// Create quality measures on whole dataset
		Log.logCommandLine("Creating quality measures.");
		int aPostProcessingCount = itsSearchParameters.getPostProcessingCount();
		double aPostProcessingCountSquare = Math.pow(aPostProcessingCount, 2);

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

	public int getNumberOfSubgroups() { return itsResult.size(); }
	public SubgroupSet getResult() { return itsResult; }
	public void clearResult() { itsResult.clear(); }
	public BitSet getBinaryTarget() { return (BitSet)itsBinaryTarget.clone(); }
	public QualityMeasure getQualityMeasure() { return itsQualityMeasure; }
	public SearchParameters getSearchParameters() { return itsSearchParameters; }

	/*
	 * same as public void mine(long theBeginTime)
	 * but allows nrThreads to be set
	 * use theNrThreads < 0 to run old mine(theBeginTime)
	 */
	public void mine(long theBeginTime, int theNrThreads)
	{
		long anEndTime = theBeginTime + (long) (((double) itsSearchParameters.getMaximumTime()) * 60.0 * 1000.0);
		itsEndTime = (anEndTime <= theBeginTime) ? Long.MAX_VALUE : anEndTime;

		// some settings should always build a sorted domain for all TargetTypes
		preparePOCData(isPOCSetting(), itsBinaryTarget, itsTable.getColumns());

		final QM aQualityMeasure = itsSearchParameters.getQualityMeasure();

		//fill the conditionList of local and global knowledge, Rob
		if (aQualityMeasure == QM.PROP_SCORE_WRACC || aQualityMeasure == QM.PROP_SCORE_RATIO)
		{
			ExternalKnowledgeFileLoader extKnowledge;
			extKnowledge = new ExternalKnowledgeFileLoader(new File("").getAbsolutePath());
			extKnowledge.createConditionListLocal(itsTable);
			extKnowledge.createConditionListGlobal(itsTable);
			itsLocalKnowledge = new LocalKnowledge(extKnowledge.getLocal(), itsBinaryTarget);
			itsGlobalKnowledge = new GlobalKnowledge(extKnowledge.getGlobal(), itsBinaryTarget);
		}

		// not in Constructor, Table / SearchParameters may change
		final ConditionBaseSet aConditions = new ConditionBaseSet(itsTable, itsSearchParameters);

		logExperimentSettings(aConditions);

		if (theNrThreads < 0)
		{
			mine(theBeginTime);
			return;
		}
		else if (theNrThreads == 0)
			theNrThreads = Runtime.getRuntime().availableProcessors();

		// make subgroup to start with, containing all elements
		BitSet aBitSet = getAllDataBitSet(itsNrRows);
		Subgroup aStart = new Subgroup(ConditionListBuilder.emptyList(), aBitSet, itsResult);

		// set number of true positives for dataset
		if (isPOCSetting())
			aStart.setTertiaryStatistic(itsQualityMeasure.getNrPositives());

		if (itsSearchParameters.getBeamSeed() == null)
			itsCandidateQueue = new CandidateQueue(itsSearchParameters, new Candidate(aStart));
		else
		{
			// using a different CandidateQueue constructor would
			// make this code much cleaner, and avoid any crash

			//List<ConditionList> aBeamSeed = itsSearchParameters.getBeamSeed();
			List<ConditionListA> aBeamSeed = itsSearchParameters.getBeamSeed();
			//ConditionList aFirstConditionList = aBeamSeed.get(0);
			ConditionListA aFirstConditionList = aBeamSeed.get(0);
			//TODO there may be no members, in which case the following statement crashes
			BitSet aFirstMembers = itsTable.evaluate(aFirstConditionList);
			Subgroup aFirstSubgroup = new Subgroup(aFirstConditionList, aFirstMembers, itsResult);
			CandidateQueue aSeededCandidateQueue = new CandidateQueue(itsSearchParameters, new Candidate(aFirstSubgroup));
			aBeamSeed.remove(0); // does a full array-copy of aBeamSeed
			int aNrEmptySeeds = 0;
			//for (ConditionList aConditionList : aBeamSeed)
			for (ConditionListA aConditionList : aBeamSeed)
			//for (int i = 1, j = aBeamSeed.size(); i < j; ++i)
			{
				//ConditionList aConditionList = aBeamSeed.get(i);
				Log.logCommandLine(aConditionList.toString());
				BitSet aMembers = itsTable.evaluate(aConditionList);
				if (aMembers.cardinality()>0)
				{
					Subgroup aSubgroup = new Subgroup(aConditionList,aMembers,itsResult);
					aSeededCandidateQueue.add(new Candidate(aSubgroup));
				}
				else
					aNrEmptySeeds++;
			}
			itsCandidateQueue = aSeededCandidateQueue;
			if (aNrEmptySeeds>0)
				Log.logCommandLine("Number of empty seeds discarded: "+aNrEmptySeeds);
			Log.logCommandLine("Beam Seed size: " + itsCandidateQueue.size());
		}

		final int aSearchDepth = itsSearchParameters.getSearchDepth();

		/*
		 * essential multi-thread setup
		 * uses semaphores so only nrThreads can run at the same time
		 * AND ExecutorService can only start new Test after old one
		 * completes
		 */
		ExecutorService es = Executors.newFixedThreadPool(theNrThreads);
		Semaphore s = new Semaphore(theNrThreads);

		while (!isTimeToStop())
		{
			// wait until a Thread becomes available
			try { s.acquire(); }
			catch (InterruptedException e) { e.printStackTrace(); }

			Candidate aCandidate = null;
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
			 * the next, and creation a new next level
			 * therefore only after all but the last Candidates are
			 * processed (added to next level) can we take the last
			 * one and let the next level become the current
			 *
			 * NOTE 2 although individual methods of CandidateQueue
			 * are thread save, we need a compound action here
			 * so synchronized is still needed
			 */
			synchronized (itsCandidateQueue)
			{
				final int aTotalSize = itsCandidateQueue.size();
				final boolean alone = (s.availablePermits() == theNrThreads-1);
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
// FIXME a subgroup of size 0 or 1 can not be refined, check and disallow
				//assert (aSubgroup.getCoverage() > 1);

				es.execute(new Test(aSubgroup, aSearchDepth, s, aConditions));
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
		while(!es.isTerminated()) {};

		Log.logCommandLine("number of candidates: " + itsCandidateCount.get());
		if (aQualityMeasure == QM.COOKS_DISTANCE)
		{
			Log.logCommandLine("Bound seven computed " + getNrBoundSeven() + " times");
			Log.logCommandLine("Bound six   computed " + getNrBoundSix() + " times");
			Log.logCommandLine("Bound five  computed " + getNrBoundFive() + " times");
			Log.logCommandLine("Bound four  computed " + getNrBoundFour() + " times");
			Log.logCommandLine("Bound seven fired " + getNrBoundSevenFired() + " times");
			Log.logCommandLine("Bound six   fired " + getNrBoundSixFired() + " times");
			Log.logCommandLine("Bound five  fired " + getNrBoundFiveFired() + " times");
			Log.logCommandLine("Bound four  fired " + getNrBoundFourFired() + " times");
			Log.logCommandLine("Rank deficient models: " + getNrRankDef());
		}
		Log.logCommandLine("number of subgroups: " + getNumberOfSubgroups());

		itsResult.setIDs(); //assign 1 to n to subgroups, for future reference in subsets
		if ((itsSearchParameters.getTargetType() == TargetType.MULTI_LABEL) && itsSearchParameters.getPostProcessingDoAutoRun())
			postprocess();

		//now just for cover-based beam search post selection
		// TODO MM see note at SubgroupSet.postProcess(), all itsResults will remain in memory
		SubgroupSet aSet = itsResult.postProcess(itsSearchParameters.getSearchStrategy());
		// FIXME MM hack to deal with strange postProcess implementation
		if (itsResult != aSet)
		{
			// no reassign, we want itsResult to be final
			itsResult.clear();
			itsResult.addAll(aSet);
		}

		// in MULTI_LABEL, order may have changed
		// in COVER_BASED_BEAM_SELECTION, subgroups may have been removed
		itsResult.setIDs(); //assign 1 to n to subgroups, for future reference in subsets
// XXX MM : debug info not useful for general use
//Log.logCommandLine(String.format("CANDIDATES: %s\tFILTERED: %s\tRATIO:%f", itsCandidateCount.toString(), TOTAL_FILTERED.toString(), (TOTAL_FILTERED.doubleValue()/(itsCandidateCount.doubleValue()+TOTAL_FILTERED.doubleValue()))));
		deleteSortData(itsTable.getColumns());
	}

	private static final void preparePOCData(boolean isPOCSetting, BitSet theBinaryTarget, List<Column> theColumns)
	{
		// currently numeric with EQUALS is not a valid setting
		if (!isPOCSetting)
			return;

		Timer outer = new Timer();

		for (Column c : theColumns)
		{
			if (c.getType() == AttributeType.NUMERIC)
			{
				Log.logCommandLine(c.getName() + " building sorted domain");
				Timer t = new Timer();
				c.buildSorted(theBinaryTarget); // build SORTED and SORT_INDEX
				Log.logCommandLine(t.getElapsedTimeString());
			}
		}

		Log.logCommandLine("total sorting time: " + outer.getElapsedTimeString());
	}

	// not SINGLE_NOMINAL with ValueSet, NUMERIC_INTERVALS/CONSECUTIVE_ALL|BEST
	private final boolean isPOCSetting()
	{
		return (((itsSearchParameters.getNumericStrategy() == NumericStrategy.NUMERIC_ALL) || (itsSearchParameters.getNumericStrategy() == NumericStrategy.NUMERIC_BEST) ||
				 (itsSearchParameters.getNumericStrategy() == NumericStrategy.NUMERIC_BEST_BINS) || (itsSearchParameters.getNumericStrategy() == NumericStrategy.NUMERIC_BINS)) &&
				((itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL) && !itsSearchParameters.getNominalSets()) && false);
	}

	private static final void deleteSortData(List<Column> theColumns)
	{
		for (Column c : theColumns)
		{
			c.SORTED = null;
			c.SORT_INDEX = null;
		}
	}

	private long itsEndTime = Long.MIN_VALUE;
	private final boolean isTimeToStop()
	{
		if (System.currentTimeMillis() > itsEndTime)
			return true;

		if ((itsMainWindow != null) && ((MiningWindow) itsMainWindow).isCancelled())
			return true;

		return false;
	}

	// to throttle GUI update, only one thread can obtain lock and update time
	private final Lock itsClockLock = new ReentrantLock();
	private static final long INTERVAL = 100_000_000L; // in nanoseconds
	private long itsThen = 0L;
	private static final DecimalFormat FORMATTER = (DecimalFormat) NumberFormat.getNumberInstance(Locale.GERMAN);

	// NOTE itsCandidateCount and currently refined subgroup are unrelated
	private final void setTitle(Subgroup theSubgroup)
	{
		if (itsMainWindow == null)
			return;

		// do not even try to obtain lock
		if (System.nanoTime() - itsThen < INTERVAL)
			return;

		if (!itsClockLock.tryLock())
			return;

		try
		{
			itsThen = System.nanoTime();

			String aCurrent = theSubgroup.toString();

			StringBuilder sb = new StringBuilder(aCurrent.length() + 32);
			sb.append("d=").append(Integer.toString(theSubgroup.getDepth()))
				.append(" cands=").append(FORMATTER.format(itsCandidateCount.get()))
				.append(" evaluating: ").append(aCurrent);

			itsMainWindow.setTitle(sb.toString());
		}
		finally
		{
			itsClockLock.unlock();
		}
	}

	/*
	 * Essential Runnable, code copied from old mine().
	 * After Test is done, semaphore is release, so ExecutorService can
	 * start a new Test.
	 */
	private class Test implements Runnable
	{
		private final Subgroup itsSubgroup;
		private final int itsSearchDepth;
		private final Semaphore itsSemaphore;
		private final ConditionBaseSet itsConditionBaseSet;

		// XXX MM - theSearchDepth parameter will be removed
		public Test(Subgroup theSubgroup, int theSearchDepth, Semaphore theSemaphore, ConditionBaseSet theConditionBaseSet)
		{
			assert (theSubgroup.getDepth() < theSearchDepth);
// FIXME a subgroup of size 0 or 1 can not be refined, check and disallow
			//assert (theSubgroup.getCoverage() > 1);

			itsSubgroup = theSubgroup;
			itsSearchDepth = theSearchDepth;
			itsSemaphore = theSemaphore;
			itsConditionBaseSet = theConditionBaseSet;
		}

		@Override
		public void run()
		{
			// XXX MM - can be removed, check by Constructor
			// in the meantime getDepth() should not have changed
			if (itsSubgroup.getDepth() < itsSearchDepth)
			{
				RefinementList aRefinementList = new RefinementList(itsSubgroup, itsConditionBaseSet);
				// .getMembers() creates expensive clone, reuse
				final BitSet aMembers = itsSubgroup.getMembers();

				for (int i = 0, j = aRefinementList.size(); i < j; i++)
				{
					if (isTimeToStop())
						break;

					Refinement aRefinement = aRefinementList.get(i);

					ConditionBase aConditionBase = aRefinement.getConditionBase();
					// if refinement is (num_attr = value) then treat it as nominal
					if (aConditionBase.getColumn().getType() == AttributeType.NUMERIC && aConditionBase.getOperator() != Operator.EQUALS)
						evaluateNumericRefinements(aMembers, aRefinement);
					else
						evaluateNominalBinaryRefinements(aMembers, aRefinement);
				}
			}
			itsSemaphore.release();
		}
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

	public int getNrBoundSeven() { return itsBoundSevenCount; }
	public int getNrBoundSix() { return itsBoundSixCount; }
	public int getNrBoundFive() { return itsBoundFiveCount; }
	public int getNrBoundFour() { return itsBoundFourCount; }
	public int getNrBoundSevenFired() { return itsBoundSevenFired; }
	public int getNrBoundSixFired() { return itsBoundSixFired; }
	public int getNrBoundFiveFired() { return itsBoundFiveFired; }
	public int getNrBoundFourFired() { return itsBoundFourFired; }
	public int getNrRankDef() { return itsRankDefCount; }

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
			final int aSize = theConditionList.size();
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
}
