package nl.liacs.subdisc;

import java.util.*;

public class SubgroupDiscovery extends MiningAlgorithm
{
	private SubgroupSet itsResult;
	private CandidateQueue itsCandidateQueue;
	private int itsCandidateCount;
	private int itsMaximumCoverage;
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
		itsResult = new SubgroupSet(theSearchParameters.getMaximumSubgroups());
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();

		itsQualityMeasure = new QualityMeasure(theSearchParameters.getQualityMeasure(), itsTable.getNrRows(), theNrPositive);
		Attribute aTarget = theSearchParameters.getTargetConcept().getPrimaryTarget();
		Condition aCondition = new Condition(aTarget, Condition.EQUALS);
		aCondition.setValue(theSearchParameters.getTargetConcept().getTargetValue());
		itsBinaryTarget = itsTable.evaluate(aCondition);
	}

	//DOUBLE_CORRELATION
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, boolean isRegression)
	{
		super(theSearchParameters);
		itsResult = new SubgroupSet(theSearchParameters.getMaximumSubgroups());
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();

		TargetConcept aTC = theSearchParameters.getTargetConcept();
		itsQualityMeasure = new QualityMeasure(theSearchParameters.getQualityMeasure(), itsTable.getNrRows(), 100); //TODO
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
	public SubgroupDiscovery(SearchParameters theSearchParameters, Table theTable, BitSet theColumns)
	{
		super(theSearchParameters);
		itsResult = new SubgroupSet(theSearchParameters.getMaximumSubgroups());
		itsTable = theTable;
		itsMaximumCoverage = itsTable.getNrRows();

		//compute base model
		itsBinaryTable = new BinaryTable(itsTable, theColumns);
		Bayesian aBayesian = new Bayesian(itsBinaryTable);
		aBayesian.climb();
		itsBaseDAG = aBayesian.getDAG();
		itsBaseDAG.print();
		//TODO fix alpha, beta
		itsQualityMeasure = new QualityMeasure(itsBaseDAG, itsTable.getNrRows(), 0.5f, 1f);
	}

	public void Mine(long theBeginTime)
	{
		//make subgroup to start with, containing all elements
		ConditionList aConditions = new ConditionList();
		Subgroup aStart = new Subgroup(aConditions, 0.0, itsMaximumCoverage, 0);
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

			long theEndTime = theBeginTime + (long)(itsSearchParameters.getMaximumTime()*60*1000);
			while (( itsCandidateQueue != null && itsCandidateQueue.size() > 0 )&& (System.currentTimeMillis() <= theEndTime))
			{
				Candidate aCandidate = itsCandidateQueue.removeFirst(); // take off first Candidate from Queue
				Subgroup aSubgroup = aCandidate.getSubgroup();

				if ( aSubgroup.getDepth() < itsSearchParameters.getSearchDepth() )
				{
					RefinementList aRefinementList = new RefinementList(aSubgroup, itsTable);

					for (int i = 0; i < aRefinementList.size(); i++)
					{
						if (System.currentTimeMillis() > theEndTime)
							break;

						Refinement aRefinement = aRefinementList.get(i);
						Attribute anAttribute = aRefinement.getCondition().getAttribute();
						if (anAttribute.isNumericType())
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
		float[] aSplitPoints = itsTable.getSplitPoints(anAttributeIndex, theSubgroup.getMembers(), itsSearchParameters.getNrSplitPoints());

		boolean first = true;
		for (int j=0; j<itsSearchParameters.getNrSplitPoints(); j++)
		{
			if (first || aSplitPoints[j] != aSplitPoints[j-1])
			{
				String aConditionValue = Float.toString(aSplitPoints[j]);
				Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
				BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
				aNewSubgroup.setMembers(aMembers);

				if (aNewSubgroup.getCoverage() >= itsSearchParameters.getMinimumCoverage())
				{
					Log.logCommandLine("candidate " + aNewSubgroup.getConditions() + " size: " + aNewSubgroup.getCoverage());
					double aQuality = evaluateCandidate(aNewSubgroup);
					aNewSubgroup.setMeasureValue(aQuality);
					if (aQuality > itsSearchParameters.getQualityMeasureMinimum())
						itsResult.add(aNewSubgroup);
					itsCandidateQueue.add(new Candidate(aNewSubgroup, aQuality));
				}
				itsCandidateCount++;
				Log.logCommandLine("Subgroup nr. " + itsCandidateCount + "; quality " + aNewSubgroup.getMeasureValue());
				Log.logCommandLine("================================================================================");
			}
			first = false;
		}
	}

	private void evaluateNominalBinaryRefinements(long theBeginTime, Subgroup theSubgroup, Refinement theRefinement)
	{
		Attribute anAttribute = theRefinement.getCondition().getAttribute();
		int anAttributeIndex = anAttribute.getIndex();
		TreeSet<String> aDomain = itsTable.getDomain(anAttributeIndex);

		boolean first = true;
		for (String aConditionValue : aDomain)
		{
			Subgroup aNewSubgroup = theRefinement.getRefinedSubgroup(aConditionValue);
			BitSet aMembers = itsTable.evaluate(aNewSubgroup.getConditions());
			aNewSubgroup.setMembers(aMembers);

			if (aNewSubgroup.getCoverage() >= itsSearchParameters.getMinimumCoverage())
			{
				Log.logCommandLine("candidate " + aNewSubgroup.getConditions() + " size: " + aNewSubgroup.getCoverage());
				double aQuality = evaluateCandidate(aNewSubgroup);
				aNewSubgroup.setMeasureValue(aQuality);
				if (aQuality > itsSearchParameters.getQualityMeasureMinimum())
					itsResult.add(aNewSubgroup);
				itsCandidateQueue.add(new Candidate(aNewSubgroup, aQuality));
			}
			itsCandidateCount++;
			Log.logCommandLine("Subgroup nr. " + itsCandidateCount + "; quality " + aNewSubgroup.getMeasureValue());
			Log.logCommandLine("================================================================================");
		}
	}

	private double evaluateCandidate(Subgroup theSubgroup)
	{
		double aQuality = 0.0;

		switch (itsSearchParameters.getTargetType())
		{
			case TargetConcept.SINGLE_NOMINAL :
			{
				Attribute aPrimaryTarget = itsSearchParameters.getTargetConcept().getPrimaryTarget();
				Log.logCommandLine("target: " + aPrimaryTarget.getName());
				BitSet aTarget = (BitSet)itsBinaryTarget.clone();
				aTarget.and(theSubgroup.getMembers());
				int aCountHeadBody = aTarget.cardinality();
				aQuality = itsQualityMeasure.calculate(aCountHeadBody, theSubgroup.getCoverage());
				break;
			}
			case TargetConcept.DOUBLE_REGRESSION :
			{
				RegressionMeasure aRM = new RegressionMeasure(itsBaseRM);
				for (int i=0; i<itsTable.getNrRows(); i++)
					if (theSubgroup.getMembers().get(i))
						aRM.addObservation(itsPrimaryColumn.getFloat(i), itsSecondaryColumn.getFloat(i));

				aQuality = aRM.getEvaluationMeasureValue();
				break;
			}
			case TargetConcept.DOUBLE_CORRELATION :
			{
				CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);
				for (int i=0; i<itsTable.getNrRows(); i++)
					if (theSubgroup.getMembers().get(i))
						aCM.addObservation(itsPrimaryColumn.getFloat(i), itsSecondaryColumn.getFloat(i));

				aQuality = aCM.getEvaluationMeasureValue();
				break;
			}
			case TargetConcept.MULTI_LABEL :
			{
				aQuality = weightedEntropyEditDistance(theSubgroup); //also stores DAG in Subgroup
				break;
			}
		}
		return aQuality;
	}

	private double weightedEntropyEditDistance(Subgroup theSubgroup)
	{
		BinaryTable aBinaryTable = itsBinaryTable.selectRows(theSubgroup.getMembers());
		Bayesian aBayesian = new Bayesian(aBinaryTable);
		aBayesian.climb();  //induce DAG
		DAG aDAG = aBayesian.getDAG();
		theSubgroup.setDAG(aDAG); //store DAG with subgroup for later use
		return itsQualityMeasure.calculateEDIT_DISTANCE(theSubgroup);
	}

	public int getNumberOfSubgroups() { return itsResult.size(); }
	public SubgroupSet getResult() { return itsResult; }
}