package nl.liacs.subdisc;

import java.util.*;

public class SubgroupDiscovery extends MiningAlgorithm
{
	private final Table itsTable;
	private final int itsMaximumCoverage;	// itsTable.getNrRows()
	private final QualityMeasure itsQualityMeasure;
	private SubgroupSet itsResult;
	private CandidateQueue itsCandidateQueue;
	private int itsCandidateCount;

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
//		itsNumericTarget = itsTable.getColumn(aTC.getPrimaryTarget());
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

		TargetConcept aTC = itsSearchParameters.getTargetConcept();
//		itsPrimaryColumn = itsTable.getColumn(aTC.getPrimaryTarget());
//		itsSecondaryColumn = itsTable.getColumn(aTC.getSecondaryTarget());
		itsPrimaryColumn = aTC.getPrimaryTarget();
		itsSecondaryColumn = aTC.getSecondaryTarget();
		if (isRegression)
		{
			itsBaseRM = new RegressionMeasure(itsSearchParameters.getQualityMeasure(), itsPrimaryColumn, itsSecondaryColumn);
			Log.logCommandLine("base model: y = " + (float) itsBaseRM.getIntercept() + " + " + (float) itsBaseRM.getSlope()+ " * x");
		}
		else
			itsBaseCM = new CorrelationMeasure(itsSearchParameters.getQualityMeasure(), itsPrimaryColumn, itsSecondaryColumn);

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
		itsCandidateCount = 0;

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
						evaluateNumericRefinements(theBeginTime, aSubgroup, aRefinement);
					else
						evaluateNominalBinaryRefinements(theBeginTime, aSubgroup, aRefinement);
				}
			}
		}
		Log.logCommandLine("number of candidates: " + itsCandidateCount);
		Log.logCommandLine("number of subgroups: " + getNumberOfSubgroups());


		if ((itsSearchParameters.getTargetType() == TargetType.MULTI_LABEL) && itsSearchParameters.getPostProcessingDoAutoRun())
			postprocess();

		//now just for cover-based beam search post selection
		itsResult = itsResult.postProcess(itsSearchParameters.getSearchStrategy());

		itsResult.setIDs(); //assign 1 to n to subgroups, for future reference in subsets
	}

	private void evaluateNumericRefinements(long theBeginTime, Subgroup theSubgroup, Refinement theRefinement)
	{
		int anAttributeIndex = theRefinement.getCondition().getAttribute().getIndex();
		int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		float aQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		switch (itsSearchParameters.getNumericStrategy())
		{
			case NUMERIC_ALL :
			{
				float[] aSplitPoints = itsTable.getUniqueNumericDomain(anAttributeIndex, theSubgroup.getMembers());
				for (float aSplit : aSplitPoints)
				{
					String aConditionValue = Float.toString(aSplit);
					Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
					BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
					aNewSubgroup.setMembers(aMembers);

					if (aNewSubgroup.getCoverage() >= aMinimumCoverage)
					{
						Log.logCommandLine("candidate " + aNewSubgroup.getConditions() + " size: " + aNewSubgroup.getCoverage());
						float aQuality = evaluateCandidate(aNewSubgroup);
						aNewSubgroup.setMeasureValue(aQuality);
						if (aQuality > aQualityMeasureMinimum)
							itsResult.add(aNewSubgroup);
						itsCandidateQueue.add(new Candidate(aNewSubgroup, aQuality));
						Log.logCommandLine("  subgroup nr. " + itsCandidateCount + "; quality " + aNewSubgroup.getMeasureValue());
					}
					itsCandidateCount++;
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
						String aConditionValue = Float.toString(aSplitPoints[j]);
						Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
						BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
						aNewSubgroup.setMembers(aMembers);

						if (aNewSubgroup.getCoverage() >= aMinimumCoverage && aNewSubgroup.getCoverage()< itsMaximumCoverage)
						{
							Log.logCommandLine("candidate " + aNewSubgroup.getConditions() + " size: " + aNewSubgroup.getCoverage());
							float aQuality = evaluateCandidate(aNewSubgroup);
							aNewSubgroup.setMeasureValue(aQuality);
							if (aQuality > aQualityMeasureMinimum)
								itsResult.add(aNewSubgroup);
							itsCandidateQueue.add(new Candidate(aNewSubgroup, aQuality));
							Log.logCommandLine("  subgroup nr. " + itsCandidateCount + "; quality " + aNewSubgroup.getMeasureValue());
						}
						itsCandidateCount++;
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
					String aConditionValue = Float.toString(aSplit);
					Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
					BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
					aNewSubgroup.setMembers(aMembers);

					if (aNewSubgroup.getCoverage() >= aMinimumCoverage)
					{
						float aQuality = evaluateCandidate(aNewSubgroup);
						aNewSubgroup.setMeasureValue(aQuality);
						if (aQuality > aMax)
						{
							aMax = aQuality;
							aBest = aSplit;
							aBestSubgroup = aNewSubgroup;
						}
					}
				}

				//add best
				if (aBestSubgroup!=null) //at least one threshold found that has enough quality and coverage
				{
					Log.logCommandLine("candidate " + aBestSubgroup.getConditions() + " size: " + aBestSubgroup.getCoverage());
					if (aMax > aQualityMeasureMinimum)
						itsResult.add(aBestSubgroup);
					itsCandidateQueue.add(new Candidate(aBestSubgroup, aMax));
					Log.logCommandLine("  subgroup nr. " + itsCandidateCount + "; quality " + aMax);
					itsCandidateCount++;
				}
				break;
			}
		}
	}

	private void evaluateNominalBinaryRefinements(long theBeginTime, Subgroup theSubgroup, Refinement theRefinement)
	{
		TreeSet<String> aDomain = itsTable.getDomain(theRefinement.getCondition().getAttribute().getIndex());
		int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		float aQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		for (String aConditionValue : aDomain)
		{
			Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
			BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
			aNewSubgroup.setMembers(aMembers);

			if (aNewSubgroup.getCoverage() >= aMinimumCoverage)
			{
				Log.logCommandLine("candidate " + aNewSubgroup.getConditions() + " size: " + aNewSubgroup.getCoverage());
				float aQuality = evaluateCandidate(aNewSubgroup);
				aNewSubgroup.setMeasureValue(aQuality);
				if (aQuality > aQualityMeasureMinimum)
					itsResult.add(aNewSubgroup);
				itsCandidateQueue.add(new Candidate(aNewSubgroup, aQuality));
				Log.logCommandLine("  subgroup nr. " + itsCandidateCount + "; quality " + aNewSubgroup.getMeasureValue());
			}
			itsCandidateCount++;
		}
	}

	private float evaluateCandidate(Subgroup theSubgroup)
	{
		float aQuality = 0.0f;

		switch (itsSearchParameters.getTargetType())
		{
			case SINGLE_NOMINAL :
			{
				BitSet aTarget = (BitSet)itsBinaryTarget.clone();
				aTarget.and(theSubgroup.getMembers());
				int aCountHeadBody = aTarget.cardinality();
				aQuality = itsQualityMeasure.calculate(aCountHeadBody, theSubgroup.getCoverage());
				break;
			}
			case SINGLE_NUMERIC :
			{
				NumericDomain aDomain = new NumericDomain(itsNumericTarget, theSubgroup.getMembers());
				aQuality = itsQualityMeasure.calculate(theSubgroup.getCoverage(),
					aDomain.computeSum(0, theSubgroup.getCoverage()),
					aDomain.computeSumSquaredDeviations(0, theSubgroup.getCoverage()),
					aDomain.computeMedian(0, theSubgroup.getCoverage()),
					aDomain.computeMedianAD(0, theSubgroup.getCoverage()),
					null); //TODO fix this parameter. only used by X2
				break;
			}
			case DOUBLE_REGRESSION :
			{
				RegressionMeasure aRM = new RegressionMeasure(itsBaseRM, theSubgroup.getMembers());
				aRM.update(); //update after all points have been added
				aQuality = (float) aRM.getEvaluationMeasureValue();
				break;
			}
			case DOUBLE_CORRELATION :
			{
				CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);
				for (int i = 0; i < itsMaximumCoverage; i++)
					if (theSubgroup.getMembers().get(i))
						aCM.addObservation(itsPrimaryColumn.getFloat(i), itsSecondaryColumn.getFloat(i));

				aQuality = (float) aCM.getEvaluationMeasureValue();
				break;
			}
			case MULTI_LABEL :
			{
				aQuality = weightedEntropyEditDistance(theSubgroup); //also stores DAG in Subgroup
				break;
			}
			default : break;
		}
		return aQuality;
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
}
