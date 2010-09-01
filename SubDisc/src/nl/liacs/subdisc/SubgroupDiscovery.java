package nl.liacs.subdisc;

import java.util.*;

public class SubgroupDiscovery extends MiningAlgorithm
{
	private SubgroupSet itsResult;
	private CandidateQueue itsCandidateQueue;
	private int itsCandidateCount;
	private int itsMaximumCoverage;	// set by all constructors itsTable.getNrRows()
	private Table itsTable;
	private QualityMeasure itsQualityMeasure;

	//target concept type-specific information, including base models
	private BitSet itsBinaryTarget; 	//SINGLE_NOMINAL
	private Column itsPrimaryColumn; 	//DOUBLE_CORRELATION / DOUBLE_REGRESSION
	private Column itsSecondaryColumn; 	//DOUBLE_CORRELATION / DOUBLE_REGRESSION
	private CorrelationMeasure itsBaseCM; 	//DOUBLE_CORRELATION
	private RegressionMeasure itsBaseRM; 	//DOUBLE_REGRESSION
	private BinaryTable itsBinaryTable; //MULTI_LABEL
	private DAG itsBaseDAG; 			//MULTI_LABEL

	//SINGLE_NOMINAL
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, int theNrPositive)
	{
		super(theSearchParameters);
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();

		itsQualityMeasure = new QualityMeasure(theSearchParameters.getQualityMeasure(), itsMaximumCoverage, theNrPositive);
		Attribute aTarget = theSearchParameters.getTargetConcept().getPrimaryTarget();
		Condition aCondition = new Condition(aTarget, Condition.EQUALS);
		aCondition.setValue(theSearchParameters.getTargetConcept().getTargetValue());
		itsBinaryTarget = itsTable.evaluate(aCondition);

		itsResult = new SubgroupSet(theSearchParameters.getMaximumSubgroups(), itsMaximumCoverage, itsBinaryTarget);
	}

	//DOUBLE_CORRELATION and DOUBLE_REGRESSION
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, boolean isRegression)
	{
		super(theSearchParameters);
		itsResult = new SubgroupSet(theSearchParameters.getMaximumSubgroups());
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();

		TargetConcept aTC = theSearchParameters.getTargetConcept();
		itsQualityMeasure = new QualityMeasure(theSearchParameters.getQualityMeasure(), itsMaximumCoverage, 100); //TODO
		Attribute aPrimaryTarget = aTC.getPrimaryTarget();
		itsPrimaryColumn = itsTable.getColumn(aPrimaryTarget);
		Attribute aSecondaryTarget = aTC.getSecondaryTarget();
		itsSecondaryColumn = itsTable.getColumn(aSecondaryTarget);
		if (isRegression)
			itsBaseRM = new RegressionMeasure(theSearchParameters.getQualityMeasure(), itsPrimaryColumn, itsSecondaryColumn, null);
		else
			itsBaseCM = new CorrelationMeasure(theSearchParameters.getQualityMeasure(), itsPrimaryColumn, itsSecondaryColumn);
	}

	//MULTI_LABEL
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable)
	{
		super(theSearchParameters);
		itsResult = new SubgroupSet(theSearchParameters.getMaximumSubgroups());
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();

		//compute base model
		itsBinaryTable = new BinaryTable(itsTable, theSearchParameters.getTargetConcept().getMultiTargets());
		Bayesian aBayesian = new Bayesian(itsBinaryTable);
		aBayesian.climb();
		itsBaseDAG = aBayesian.getDAG();
		itsBaseDAG.print();
		//TODO fix alpha, beta
		itsQualityMeasure = new QualityMeasure(itsBaseDAG, itsMaximumCoverage, theSearchParameters.getAlpha(), theSearchParameters.getBeta());
	}

	public void Mine(long theBeginTime)
	{
		//make subgroup to start with, containing all elements
		Subgroup aStart = new Subgroup(0.0, itsMaximumCoverage, 0, itsResult);
		BitSet aBitSet = new BitSet(itsMaximumCoverage);
		aBitSet.set(0,itsMaximumCoverage);
		aStart.setMembers(aBitSet);

		Mine(theBeginTime, aStart);
		itsResult.setIDs(); //assign 1 to n to subgroups, for future reference in subsets
	}

	public void Mine(long theBeginTime, Subgroup theStart)
	{
		try
		{
			Candidate aRootCandidate = new Candidate(theStart, 0.0F);
			itsCandidateQueue = new CandidateQueue(itsSearchParameters.getSearchStrategy(),
													itsSearchParameters.getSearchStrategyWidth(),
													aRootCandidate);
			itsCandidateCount = 0;

			int aSearchDepth = itsSearchParameters.getSearchDepth();
			long theEndTime = theBeginTime + (long)(itsSearchParameters.getMaximumTime()*60*1000);
			while((itsCandidateQueue != null && itsCandidateQueue.size() > 0 )&& (System.currentTimeMillis() <= theEndTime))
			{
				Candidate aCandidate = itsCandidateQueue.removeFirst(); // take off first Candidate from Queue
				Subgroup aSubgroup = aCandidate.getSubgroup();

				if(aSubgroup.getDepth() < aSearchDepth)
				{
					RefinementList aRefinementList = new RefinementList(aSubgroup, itsTable, itsSearchParameters.getTargetConcept());

					for(int i = 0, j = aRefinementList.size(); i < j; i++)
					{
						if(System.currentTimeMillis() > theEndTime)
							break;

						Refinement aRefinement = aRefinementList.get(i);
						if(aRefinement.getCondition().getAttribute().isNumericType())
							evaluateNumericRefinements(theBeginTime, aSubgroup, aRefinement);
						else
							evaluateNominalBinaryRefinements(theBeginTime, aSubgroup, aRefinement);
					}
				}
			}
			Log.logCommandLine("number of candidates: " + itsCandidateCount);
			Log.logCommandLine("number of subgroups: " + getNumberOfSubgroups());
		}
		catch (Exception e)
		{
			ErrorWindow aWindow = new ErrorWindow(e);
			aWindow.setLocation(200, 200);
			aWindow.setVisible(true);
			e.printStackTrace();
		}
	}

	private void evaluateNumericRefinements(long theBeginTime, Subgroup theSubgroup, Refinement theRefinement)
	{
		Attribute anAttribute = theRefinement.getCondition().getAttribute();
		int anAttributeIndex = anAttribute.getIndex();
		int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		float aQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		switch (itsSearchParameters.getNumericStrategy())
		{
			case NUMERIC_ALL :
			{
				float[] aSplitPoints = itsTable.getUniqueNumericDomain(anAttributeIndex, theSubgroup.getMembers());
				for(float aSplit : aSplitPoints)
				{
					String aConditionValue = Float.toString(aSplit);
					Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
					BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
					aNewSubgroup.setMembers(aMembers);

					if(aNewSubgroup.getCoverage() >= aMinimumCoverage)
					{
						Log.logCommandLine("candidate " + aNewSubgroup.getConditions() + " size: " + aNewSubgroup.getCoverage());
						float aQuality = evaluateCandidate(aNewSubgroup);
						aNewSubgroup.setMeasureValue(aQuality);
						if(aQuality > aQualityMeasureMinimum)
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
				for(int j=0; j<aNrSplitPoints; j++)
				{
					if(first || aSplitPoints[j] != aSplitPoints[j-1])
					{
						String aConditionValue = Float.toString(aSplitPoints[j]);
						Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
						BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
						aNewSubgroup.setMembers(aMembers);

						if(aNewSubgroup.getCoverage() >= aMinimumCoverage)
						{
							Log.logCommandLine("candidate " + aNewSubgroup.getConditions() + " size: " + aNewSubgroup.getCoverage());
							float aQuality = evaluateCandidate(aNewSubgroup);
							aNewSubgroup.setMeasureValue(aQuality);
							if(aQuality > aQualityMeasureMinimum)
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
				for(float aSplit : aSplitPoints)
				{
					String aConditionValue = Float.toString(aSplit);
					Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
					BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
					aNewSubgroup.setMembers(aMembers);

					if(aNewSubgroup.getCoverage() >= aMinimumCoverage)
					{
						float aQuality = evaluateCandidate(aNewSubgroup);
						aNewSubgroup.setMeasureValue(aQuality);
						if(aQuality > aMax)
						{
							aMax = aQuality;
							aBest = aSplit;
							aBestSubgroup = aNewSubgroup;
						}
					}
				}

				//add best
				Log.logCommandLine("candidate " + aBestSubgroup.getConditions() + " size: " + aBestSubgroup.getCoverage());
				if(aMax > aQualityMeasureMinimum)
					itsResult.add(aBestSubgroup);
				itsCandidateQueue.add(new Candidate(aBestSubgroup, aMax));
				Log.logCommandLine("  subgroup nr. " + itsCandidateCount + "; quality " + aMax);
				itsCandidateCount++;
				break;
			}
		}
	}

	private void evaluateNominalBinaryRefinements(long theBeginTime, Subgroup theSubgroup, Refinement theRefinement)
	{
		Attribute anAttribute = theRefinement.getCondition().getAttribute();
		int anAttributeIndex = anAttribute.getIndex();
		TreeSet<String> aDomain = itsTable.getDomain(anAttributeIndex);
		int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		float aQualityMeasureMinimum = itsSearchParameters.getQualityMeasureMinimum();

		for(String aConditionValue : aDomain)
		{
			Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
			BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
			aNewSubgroup.setMembers(aMembers);

			if(aNewSubgroup.getCoverage() >= aMinimumCoverage)
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
				Attribute aPrimaryTarget = itsSearchParameters.getTargetConcept().getPrimaryTarget();
//				Log.logCommandLine("target: " + aPrimaryTarget.getName());
				BitSet aTarget = (BitSet)itsBinaryTarget.clone();
				aTarget.and(theSubgroup.getMembers());
				int aCountHeadBody = aTarget.cardinality();
				aQuality = itsQualityMeasure.calculate(aCountHeadBody, theSubgroup.getCoverage());
				break;
			}
			case DOUBLE_REGRESSION :
			{
				RegressionMeasure aRM = new RegressionMeasure(itsBaseRM);
				for(int i = 0; i < itsMaximumCoverage; i++)
					if (theSubgroup.getMembers().get(i))
						aRM.addObservation(itsPrimaryColumn.getFloat(i), itsSecondaryColumn.getFloat(i));

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
		Bayesian aBayesian = new Bayesian(aBinaryTable);
		aBayesian.climb(); //induce DAG
		DAG aDAG = aBayesian.getDAG();
		theSubgroup.setDAG(aDAG); //store DAG with subgroup for later use
//		return itsQualityMeasure.calculateEDIT_DISTANCE(theSubgroup);
		return itsQualityMeasure.calculateWEED(theSubgroup);
	}

	public int getNumberOfSubgroups() { return itsResult.size(); }
	public SubgroupSet getResult() { return itsResult; }
	public BitSet getBinaryTarget() { return (BitSet)itsBinaryTarget.clone(); }
}