package nl.liacs.subdisc;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class SubgroupDiscovery extends MiningAlgorithm
{
	private final Table itsTable;
	private final int itsMaximumCoverage;	// itsTable.getNrRows()
	private final QualityMeasure itsQualityMeasure;
	private SubgroupSet itsResult;
	private CandidateQueue itsCandidateQueue;
	private AtomicInteger itsCandidateCount= new AtomicInteger(0);

	//target concept type-specific information, including base models
	private BitSet itsBinaryTarget;		//SINGLE_NOMINAL
	private Column itsNumericTarget;	//SINGLE_NUMERIC
	private Column itsPrimaryColumn;	//DOUBLE_CORRELATION / DOUBLE_REGRESSION
	private Column itsSecondaryColumn;	//DOUBLE_CORRELATION / DOUBLE_REGRESSION
	private CorrelationMeasure itsBaseCM;	//DOUBLE_CORRELATION
	private RegressionMeasure itsBaseRM;	//DOUBLE_REGRESSION
	private BinaryTable itsBinaryTable;	//MULTI_LABEL
//	private String[] itsTargets;	//MULTI_LABEL
	private List<Column> itsTargets;	//MULTI_LABEL
//	private DAG itsBaseDAG;				//MULTI_LABEL

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

	//SINGLE_NOMINAL
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, int theNrPositive)
	{
		super(theSearchParameters);
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();
		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsMaximumCoverage, theNrPositive);

		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		Condition aCondition = new Condition(aTC.getPrimaryTarget(), Condition.EQUALS);
		aCondition.setValue(aTC.getTargetValue());
		itsBinaryTarget = itsTable.evaluate(aCondition);

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsMaximumCoverage, itsBinaryTarget);
	}

	//SINGLE_NUMERIC
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, float theAverage)
	{
		super(theSearchParameters);
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();
		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		itsNumericTarget = aTC.getPrimaryTarget();
		NumericDomain aDomain = new NumericDomain(itsNumericTarget);

		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsMaximumCoverage,
			aDomain.computeSum(0, itsMaximumCoverage),
			aDomain.computeSumSquaredDeviations(0, itsMaximumCoverage),
			aDomain.computeMedian(0, itsMaximumCoverage),
			aDomain.computeMedianAD(0, itsMaximumCoverage));

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups(), itsMaximumCoverage, null); //TODO
	}

	//DOUBLE_CORRELATION and DOUBLE_REGRESSION
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, boolean isRegression)
	{
		super(theSearchParameters);
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();
		itsQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsMaximumCoverage, 100); //TODO

		//N.B.: Temporary lines for fetching Cook's experimental statistics
		if (theSearchParameters.getQualityMeasure() == QualityMeasure.COOKS_DISTANCE)
		{
			Log.REFINEMENTLOG = true;
			Log.openFileOutputStreams();
//			Log.logRefinement("Bound graph for "+itsTable.getName());
//			Log.logRefinement("SubgroupSize,AvgRegressionTime,AvgCook,AvgBoundSeven,AvgBoundSix,AvgBoundFive,AvgBoundFour,CookComputable,BoundSevenComputable,BoundSixComputable,BoundFiveComputable,BoundFourComputable");
		}
		
		TargetConcept aTC = itsSearchParameters.getTargetConcept();
		if (isRegression)
		{
			itsBaseRM = new RegressionMeasure(itsSearchParameters.getQualityMeasure(), aTC);

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
			itsPrimaryColumn = aTC.getPrimaryTarget();
			itsSecondaryColumn = aTC.getSecondaryTarget();
			itsBaseCM = new CorrelationMeasure(itsSearchParameters.getQualityMeasure(), itsPrimaryColumn, itsSecondaryColumn);
		}

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups());
	}

	//MULTI_LABEL
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable)
	{
		super(theSearchParameters);
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();

		//compute base model
		itsTargets = itsSearchParameters.getTargetConcept().getMultiTargets();
		itsBinaryTable = new BinaryTable(itsTable, itsTargets);

		Bayesian aBayesian = new Bayesian(itsBinaryTable, itsTargets);
		aBayesian.climb();

		itsQualityMeasure = new QualityMeasure(itsSearchParameters,
												aBayesian.getDAG(),
												itsMaximumCoverage);

		itsResult = new SubgroupSet(itsSearchParameters.getMaximumSubgroups());
	}

	/**
	 * Only the top result is used in this setting. Maximum coverage and
	 * binary target constructor parameters are not needed.
	 */
	protected void useSwapRandomisationSetting() {
		itsResult.useSwapRandomisationSetting();
	}

	public void mine(long theBeginTime)
	{
		//make subgroup to start with, containing all elements
		Subgroup aStart = new Subgroup(0.0, itsMaximumCoverage, 0, itsResult);
		BitSet aBitSet = new BitSet(itsMaximumCoverage);
		aBitSet.set(0,itsMaximumCoverage);
		aStart.setMembers(aBitSet);

		itsCandidateQueue = new CandidateQueue(itsSearchParameters, new Candidate(aStart, 0.0f));

		int aSearchDepth = itsSearchParameters.getSearchDepth();

		long theEndTime = theBeginTime + (((long) itsSearchParameters.getMaximumTime()) * 60 * 1000);
		if (theEndTime <= theBeginTime)
			theEndTime = Long.MAX_VALUE;

		while ((itsCandidateQueue.size() > 0 ) && (System.currentTimeMillis() <= theEndTime))
		{
			Candidate aCandidate = itsCandidateQueue.removeFirst(); // take off first Candidate from Queue
			Subgroup aSubgroup = aCandidate.getSubgroup();

			if (aSubgroup.getDepth() < aSearchDepth)
			{
				RefinementList aRefinementList = new RefinementList(aSubgroup, itsTable, itsSearchParameters);

				for (int i = 0, j = aRefinementList.size(); i < j; i++)
				{
					if (System.currentTimeMillis() > theEndTime)
						break;

					Refinement aRefinement = aRefinementList.get(i);
					// if refinement is (num_attr = value) then treat it as nominal
					if (aRefinement.getCondition().getAttribute().isNumericType() && aRefinement.getCondition().getOperator() != Condition.EQUALS)
						evaluateNumericRefinements(aSubgroup, aRefinement);
					else
						evaluateNominalBinaryRefinements(aSubgroup, aRefinement);
				}
			}
			
			if (itsCandidateQueue.size() ==0)
				flushBuffer();
		}
		Log.logCommandLine("number of candidates: " + itsCandidateCount.get());
		if (itsSearchParameters.getQualityMeasure() == QualityMeasure.COOKS_DISTANCE)
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


		if ((itsSearchParameters.getTargetType() == TargetType.MULTI_LABEL) && itsSearchParameters.getPostProcessingDoAutoRun())
			postprocess();

		//now just for cover-based beam search post selection
		itsResult = itsResult.postProcess(itsSearchParameters.getSearchStrategy());

		itsResult.setIDs(); //assign 1 to n to subgroups, for future reference in subsets
	}

	private void evaluateNumericRefinements(Subgroup theSubgroup, Refinement theRefinement)
	{
		int anAttributeIndex = theRefinement.getCondition().getAttribute().getIndex();
		int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		int anOldCoverage = theSubgroup.getCoverage();
		float aQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		switch (itsSearchParameters.getNumericStrategy())
		{
			case NUMERIC_ALL :
			{
				float[] aSplitPoints = itsTable.getUniqueNumericDomain(anAttributeIndex, theSubgroup.getMembers());
				for (float aSplit : aSplitPoints)
				{
					Subgroup aNewSubgroup = makeNewSubgroup(Float.toString(aSplit), theRefinement);
					addToBuffer(aNewSubgroup);
					//checkAndLog(aNewSubgroup, aMinimumCoverage, anOldCoverage, aQualityMeasureMinimum);
				}
				break;
			}
			case NUMERIC_BINS :
			{
				int aNrSplitPoints = itsSearchParameters.getNrBins() - 1;  //this is the crucial translation from nr bins to nr splitpoint
				float[] aSplitPoints = itsTable.getSplitPoints(anAttributeIndex, theSubgroup.getMembers(), aNrSplitPoints);
				boolean first = true;
				for (int j=0; j<aNrSplitPoints; j++)
				{
					if (first || aSplitPoints[j] != aSplitPoints[j-1])
					{
						Subgroup aNewSubgroup = makeNewSubgroup(Float.toString(aSplitPoints[j]), theRefinement);
						addToBuffer(aNewSubgroup);
						//checkAndLog(aNewSubgroup, aMinimumCoverage, anOldCoverage, aQualityMeasureMinimum);
					}
					first = false;
				}
				break;
			}
			case NUMERIC_BEST :
			{
				float[] aSplitPoints = itsTable.getUniqueNumericDomain(anAttributeIndex, theSubgroup.getMembers());
				float aMax = Float.NEGATIVE_INFINITY;
				float aBest = aSplitPoints[0];
				Subgroup aBestSubgroup = null;
				for (float aSplit : aSplitPoints)
				{
					Subgroup aNewSubgroup = makeNewSubgroup(Float.toString(aSplit), theRefinement);

					int aNewCoverage = aNewSubgroup.getCoverage();
					if (aNewCoverage >= aMinimumCoverage && aNewCoverage < anOldCoverage)
					{
						float aQuality = evaluateCandidate(aNewSubgroup);
						if (aQuality > aMax)
						{
							aMax = aQuality;
							aNewSubgroup.setMeasureValue(aQuality);
							aBestSubgroup = aNewSubgroup;
							aBest = aSplit;
						}
					}
				}

				//add best
				if (aBestSubgroup!=null) //at least one threshold found that has enough quality and coverage
					addToBuffer(aBestSubgroup);
					// unnecessarily re-evaluates result
					//checkAndLog(aBestSubgroup, aMinimumCoverage, anOldCoverage, aQualityMeasureMinimum);

				break;
			}
		}
	}

	private void evaluateNominalBinaryRefinements(Subgroup theSubgroup, Refinement theRefinement)
	{
		TreeSet<String> aDomain = itsTable.getDomain(theRefinement.getCondition().getAttribute().getIndex());
		int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		int anOldCoverage = theSubgroup.getCoverage();
		float aQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		for (String aConditionValue : aDomain)
		{
			Subgroup aNewSubgroup = makeNewSubgroup(aConditionValue, theRefinement);
			addToBuffer(aNewSubgroup);
			//checkAndLog(aNewSubgroup, aMinimumCoverage, anOldCoverage, aQualityMeasureMinimum);
		}
	}

	private Subgroup makeNewSubgroup(String theConditionValue, Refinement theRefinement)
	{
		Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(theConditionValue);
		BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
		aNewSubgroup.setMembers(aMembers);
		return aNewSubgroup;
	}

	/*
	 * Access to itsResult is synchronized, as many threads may try to add
	 * results concurrently.
	 * 
	 * Access to itsCandidateQueue is synchronized, as many threads may try
	 * to add candidates concurrently.
	 * 
	 * itsCandidateCount is Atomic (synchronized by nature).
	 */
	private void checkAndLog(Subgroup theSubgroup, int aMinimumCoverage, int theOldCoverage, float aQualityMeasureMinimum)
	{
		int aNewCoverage = theSubgroup.getCoverage();
		if (aNewCoverage >= aMinimumCoverage && aNewCoverage < theOldCoverage)
		{
			float aQuality = evaluateCandidate(theSubgroup);
			theSubgroup.setMeasureValue(aQuality);

			if (aQuality > aQualityMeasureMinimum)
			{
				synchronized (itsResult) { itsResult.add(theSubgroup); }
			}
			synchronized (itsCandidateQueue) { itsCandidateQueue.add(new Candidate(theSubgroup, aQuality)); };

			logCandidateAddition(theSubgroup);
		}
		itsCandidateCount.getAndIncrement();
	}

	private void logCandidateAddition(Subgroup theSubgroup)
	{
		StringBuffer sb = new StringBuffer(200);
		sb.append("candidate ");
		sb.append(theSubgroup.getConditions());
		sb.append(" size: ");
		sb.append(theSubgroup.getCoverage());
		Log.logCommandLine(sb.toString());

		Log.logCommandLine(String.format("  subgroup nr. %d; quality %s",
											itsCandidateCount.get(),
											Double.toString(theSubgroup.getMeasureValue())));
	}

	private float evaluateCandidate(Subgroup theNewSubgroup)
	{
		float aQuality = 0.0f;

		switch (itsSearchParameters.getTargetType())
		{
			case SINGLE_NOMINAL :
			{
				BitSet aTarget = (BitSet)itsBinaryTarget.clone();
				aTarget.and(theNewSubgroup.getMembers());
				int aCountHeadBody = aTarget.cardinality();
				aQuality = itsQualityMeasure.calculate(aCountHeadBody, theNewSubgroup.getCoverage());
				break;
			}
			case SINGLE_NUMERIC :
			{
				NumericDomain aDomain = new NumericDomain(itsNumericTarget, theNewSubgroup.getMembers());
				aQuality = itsQualityMeasure.calculate(theNewSubgroup.getCoverage(),
					aDomain.computeSum(0, theNewSubgroup.getCoverage()),
					aDomain.computeSumSquaredDeviations(0, theNewSubgroup.getCoverage()),
					aDomain.computeMedian(0, theNewSubgroup.getCoverage()),
					aDomain.computeMedianAD(0, theNewSubgroup.getCoverage()),
					null); //TODO fix this parameter. only used by X2
				break;
			}
			case DOUBLE_REGRESSION :
			{
				switch (itsBaseRM.itsType)
				{
/*					case QualityMeasure.LINEAR_REGRESSION:
					{
						RegressionMeasure aRM = new RegressionMeasure(itsBaseRM, theNewSubgroup.getMembers());
						aQuality = (float) aRM.getEvaluationMeasureValue();
						break;
					}*/
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
						if ( itsResult.size() >= aBorderlineSubgroupNumber )
							aThreshold = itsResult.last().getMeasureValue();
						else { aNeedToComputeBounds = false; }
						
						// start actual computation
						Log.logCommandLine("");
						BitSet aMembers = theNewSubgroup.getMembers();
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
				}

				break;
			}
			case DOUBLE_CORRELATION :
			{
				CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);
				for (int i = 0; i < itsMaximumCoverage; i++)
					if (theNewSubgroup.getMembers().get(i))
						aCM.addObservation(itsPrimaryColumn.getFloat(i), itsSecondaryColumn.getFloat(i));

				aQuality = (float) aCM.getEvaluationMeasureValue();
				break;
			}
			case MULTI_LABEL :
			{
				aQuality = weightedEntropyEditDistance(theNewSubgroup); //also stores DAG in Subgroup
				break;
			}
			default : break;
		}
		return aQuality;
	}

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
	
	private float weightedEntropyEditDistance(Subgroup theSubgroup)
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
		int itsNrRecords = itsTable.getNrRows();

		QualityMeasure[] aQMs = new QualityMeasure[aPostProcessingCount];
		for (int i = 0; i < aPostProcessingCount; i++)
		{
			Bayesian aGlobalBayesian = new Bayesian(itsBinaryTable);
			aGlobalBayesian.climb();
			aQMs[i] = new QualityMeasure(itsSearchParameters, aGlobalBayesian.getDAG(), itsNrRecords);
		}

		// Iterate over subgroups
		SubgroupSet aNewSubgroupSet = new SubgroupSet(itsSearchParameters.getMaximumSubgroups());
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
		itsResult = aNewSubgroupSet;
	}

	public int getNumberOfSubgroups() { return itsResult.size(); }
	public SubgroupSet getResult() { return itsResult; }
	public void clearResult() { itsResult.clear(); }
	public BitSet getBinaryTarget() { return (BitSet)itsBinaryTarget.clone(); }
	public QualityMeasure getQualityMeasure() { return itsQualityMeasure; }
	public SearchParameters getSearchParameters() { return itsSearchParameters; }


	/*
	 * TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
	 * 
	 * same as public void mine(long theBeginTime)
	 * but allows nrThreads to be set
	 * use theNrThreads = 0 to run old mine(theBeginTime)
	 */
	public void mine(long theBeginTime, int theNrThreads)
	{
		if (theNrThreads <= 0)
		{
			mine(theBeginTime);
			return;
		}

		//make subgroup to start with, containing all elements
		Subgroup aStart = new Subgroup(0.0, itsMaximumCoverage, 0, itsResult);
		BitSet aBitSet = new BitSet(itsMaximumCoverage);
		aBitSet.set(0,itsMaximumCoverage);
		aStart.setMembers(aBitSet);

		itsCandidateQueue = new CandidateQueue(itsSearchParameters, new Candidate(aStart, 0.0f));

		int aSearchDepth = itsSearchParameters.getSearchDepth();

		long theEndTime = theBeginTime + (((long) itsSearchParameters.getMaximumTime()) * 60 * 1000);
		if (theEndTime <= theBeginTime)
			theEndTime = Long.MAX_VALUE;

		/*
		 * essential multi-thread setup
		 * uses semaphores so only nrThreads can run at the same time
		 * AND ExecutorService can only start new Test after old one
		 * completes, resulting in a more stable itsCandidateQueue
		 */
		ExecutorService es = Executors.newFixedThreadPool(theNrThreads);
		Semaphore s = new Semaphore(theNrThreads);
		// hack to populate Queue for the first time
		// however if after populating (theNrCandidates < theNrThreads)
		// while loop below will still/ might? break prematurely
		// TODO
		try { s.acquire(); }
		catch (InterruptedException e) { e.printStackTrace(); }
		es.execute(new Test(itsCandidateQueue.removeFirst(), aSearchDepth, theEndTime, s));
		// wait for this Thread to fully populate itsCandidateQueue
		while (s.availablePermits() != theNrThreads) {};

		while (System.currentTimeMillis() <= theEndTime)
		{
			// wait until a Thread becomes available
			try { s.acquire(); }
			catch (InterruptedException e) { e.printStackTrace(); }

			Candidate aCandidate;
			synchronized (itsCandidateQueue) {
				// abort if
				//   1 this is the only Thread still running AND
				//   2 itsCandidateQueue.size() == 0
				// else others may be populating it
				if (!(itsCandidateQueue.size() > 0))
				{
					if (s.availablePermits() == theNrThreads-1)
						break;
					else
					{
						s.release();
						continue;
					}
				}
				aCandidate = itsCandidateQueue.removeFirst(); // take off first Candidate from Queue
			}
			es.execute(new Test(aCandidate, aSearchDepth, theEndTime, s));
		}
		es.shutdown();
		// wait for last active threads to complete
		while(!es.isTerminated()) {};

		Log.logCommandLine("number of candidates: " + itsCandidateCount.get());
		if (itsSearchParameters.getQualityMeasure() == QualityMeasure.COOKS_DISTANCE)
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


		if ((itsSearchParameters.getTargetType() == TargetType.MULTI_LABEL) && itsSearchParameters.getPostProcessingDoAutoRun())
			postprocess();

		//now just for cover-based beam search post selection
		itsResult = itsResult.postProcess(itsSearchParameters.getSearchStrategy());

		itsResult.setIDs(); //assign 1 to n to subgroups, for future reference in subsets
		
		//N.B.: Temporary lines for fetching Cook's experimental statistics		
		if (itsSearchParameters.getQualityMeasure() == QualityMeasure.COOKS_DISTANCE)
			Log.closeFileOutputStreams();
	}

	/*
	 * Essential Runnable, code copied from old mine().
	 * After Test is done, semaphore is release, so ExecutorService can
	 * start a new Test.
	 */
	private class Test implements Runnable
	{
		private final Candidate itsCandidate;
		private final int itsSearchDepth;
		private final long itsEndTime;
		private final Semaphore itsSemaphore;

		public Test(Candidate theCandidate, int theSearchDepth, long theEndTime, Semaphore theSemaphore)
		{
			itsCandidate = theCandidate;
			itsSearchDepth= theSearchDepth;
			itsEndTime = theEndTime;
			itsSemaphore = theSemaphore;
		}

		@Override
		public void run()
		{
			Subgroup aSubgroup = itsCandidate.getSubgroup();

			if (aSubgroup.getDepth() < itsSearchDepth)
			{
				RefinementList aRefinementList = new RefinementList(aSubgroup, itsTable, itsSearchParameters);

				for (int i = 0, j = aRefinementList.size(); i < j; i++)
				{
					if (System.currentTimeMillis() > itsEndTime)
						break;

					Refinement aRefinement = aRefinementList.get(i);
					// if refinement is (num_attr = value) then treat it as nominal
					if (aRefinement.getCondition().getAttribute().isNumericType() && aRefinement.getCondition().getOperator() != Condition.EQUALS)
						evaluateNumericRefinements(aSubgroup, aRefinement);
					else
						evaluateNominalBinaryRefinements(aSubgroup, aRefinement);
				}
			}
			itsSemaphore.release();
		}
	}
	
	private void addToBuffer( Subgroup theSubgroup )
	{
		int aCoverage = theSubgroup.getCoverage();
		itsBaseRM.computeRemovedIndices(theSubgroup.getMembers(),aCoverage);
		itsBaseRM.updateSquaredResidualSum();
		itsBaseRM.updateRemovedTrace();
		double aPriority = itsBaseRM.computeBoundFour(aCoverage);
		Log.logCommandLine(""+theSubgroup.getConditions().toString() + " --- bound : " + aPriority);
		itsBuffer.add(new Candidate(theSubgroup,aPriority));
	}
	
	private void flushBuffer()
	{
		Iterator<Candidate> anIterator = itsBuffer.iterator();
		while (anIterator.hasNext())
		{
			Candidate aCandidate = anIterator.next();
			Subgroup aSubgroup = aCandidate.getSubgroup();
			int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
			int anOldCoverage = itsTable.getNrRows();
			float aQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();
			checkAndLog(aSubgroup, aMinimumCoverage, anOldCoverage, aQualityMeasureMinimum);
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
}
