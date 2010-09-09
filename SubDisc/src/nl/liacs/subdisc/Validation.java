package nl.liacs.subdisc;

import java.util.*;

/**
 * Functionality related to the statistical validation of subgroups
 */
public class Validation
{
	private SearchParameters itsSearchParameters;
	private TargetConcept itsTargetConcept;
	private QualityMeasure itsQualityMeasure;
	private Table itsTable;

	public Validation(SearchParameters theSearchParameters, Table theTable, QualityMeasure theQualityMeasure)
	{
		itsSearchParameters = theSearchParameters;
		itsTargetConcept = theSearchParameters.getTargetConcept();
		itsTable = theTable;
		itsQualityMeasure = theQualityMeasure;
	}

	public NormalDistribution RandomSubgroups(int theNrRepetitions)
	{
		double[] aQualities = new double[theNrRepetitions];
		Random aRandom = new Random(System.currentTimeMillis());
		int aSubgroupSize;

		switch(itsTargetConcept.getTargetType())
		{
			case SINGLE_NOMINAL :
			{
				Attribute aTarget = itsTargetConcept.getPrimaryTarget();
				Condition aCondition = new Condition(aTarget, Condition.EQUALS);
				aCondition.setValue(itsTargetConcept.getTargetValue());
				BitSet aBinaryTarget = itsTable.evaluate(aCondition);

				for (int i=0; i<theNrRepetitions; i++)
				{
					do
						aSubgroupSize = (int) (aRandom.nextDouble() * itsTable.getNrRows());
					while (aSubgroupSize < itsSearchParameters.getMinimumCoverage());
					Subgroup aSubgroup = itsTable.getRandomSubgroup(aSubgroupSize);

					BitSet aColumnTarget = (BitSet) aBinaryTarget.clone();
					aColumnTarget.and(aSubgroup.getMembers());
					int aCountHeadBody = aColumnTarget.cardinality();
					aQualities[i] = itsQualityMeasure.calculate(aCountHeadBody, aSubgroup.getCoverage());
				}
				break;
			}
			case MULTI_LABEL :
			{
				//base model
				BinaryTable aBaseTable = new BinaryTable(itsTable, itsTargetConcept.getMultiTargets());
				Bayesian aBayesian = new Bayesian(aBaseTable);
				aBayesian.climb();

				for (int i=0; i<theNrRepetitions; i++)
				{
					do
						aSubgroupSize = (int) (aRandom.nextDouble() * itsTable.getNrRows());
					while (aSubgroupSize < itsSearchParameters.getMinimumCoverage());
					Subgroup aSubgroup = itsTable.getRandomSubgroup(aSubgroupSize);

					// build model
					BinaryTable aBinaryTable = aBaseTable.selectRows(aSubgroup.getMembers());
					aBayesian = new Bayesian(aBinaryTable);
					aBayesian.climb();
					aSubgroup.setDAG(aBayesian.getDAG()); // store DAG with subgroup for later use

					aQualities[i] = itsQualityMeasure.calculate(aSubgroup);
				}
				break;
			}
			case DOUBLE_REGRESSION :
				//TODO implement
			case DOUBLE_CORRELATION :
			{
				Column aPrimaryColumn = itsTable.getColumn(itsTargetConcept.getPrimaryTarget());
				Column aSecondaryColumn = itsTable.getColumn(itsTargetConcept.getSecondaryTarget());
				CorrelationMeasure itsBaseCM =
					new CorrelationMeasure(itsSearchParameters.getQualityMeasure(), aPrimaryColumn, aSecondaryColumn);

				for (int i=0; i<theNrRepetitions; i++)
				{
					do
						aSubgroupSize = (int) (aRandom.nextDouble() * itsTable.getNrRows());
					while (aSubgroupSize < itsSearchParameters.getMinimumCoverage());
					Subgroup aSubgroup = itsTable.getRandomSubgroup(aSubgroupSize);

					CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);

					for (int j=0; j<itsTable.getNrRows(); j++)
						if (aSubgroup.getMembers().get(j))
							aCM.addObservation(aPrimaryColumn.getFloat(j), aSecondaryColumn.getFloat(j));

					aQualities[i] = aCM.getEvaluationMeasureValue();
				}
				break;
			}
		}
		return new NormalDistribution(aQualities); //return the normal distribution belonging to this random sample
	}


	/**
	* Generates a set of random descriptions of subgroups, by randomly combining random conditions on
	* attributes in the table. The random descriptions adhere to the search parameters.
	* For each of the subgroups related to the random conditions, the quality is computed.
	* @return the normal distribution that matches the computed sample.
	*/
	public NormalDistribution RandomConditions(int theNrRepetitions)
	{
		double[] aQualities = new double[theNrRepetitions];
		Random aRandom = new Random(System.currentTimeMillis());

		switch(itsTargetConcept.getTargetType())
		{
			case SINGLE_NOMINAL :
			{
				Attribute aTarget = itsTargetConcept.getPrimaryTarget();
				Condition aCondition = new Condition(aTarget, Condition.EQUALS);
				aCondition.setValue(itsTargetConcept.getTargetValue());
				BitSet aBinaryTarget = itsTable.evaluate(aCondition);

				int aDepth = itsSearchParameters.getSearchDepth();
				int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
				for (int i=0; i<theNrRepetitions; i++)
				{
					ConditionList aCL;
					BitSet aMembers;
					do
					{
						aCL = getRandomConditionList(aDepth, aRandom);
						aMembers = itsTable.evaluate(aCL);
					}
					while (aMembers.cardinality() < aMinimumCoverage);
					Log.logCommandLine(aCL.toString());
					Subgroup aSubgroup = new Subgroup(aCL, aMembers, aCL.size());

					BitSet aColumnTarget = (BitSet) aBinaryTarget.clone();
					aColumnTarget.and(aSubgroup.getMembers());
					int aCountHeadBody = aColumnTarget.cardinality();
					aQualities[i] = itsQualityMeasure.calculate(aCountHeadBody, aSubgroup.getCoverage());
				}
				break;
			}
			case MULTI_LABEL :
			{
				// base model
				BinaryTable aBaseTable = new BinaryTable(itsTable, itsTargetConcept.getMultiTargets());
				Bayesian aBayesian = new Bayesian(aBaseTable);
				aBayesian.climb();

				int aDepth = itsSearchParameters.getSearchDepth();
				int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
				for (int i = 0; i < theNrRepetitions; i++) // random conditions
				{
					ConditionList aCL;
					BitSet aMembers;
					do
					{
						aCL = getRandomConditionList(aDepth, aRandom);
						aMembers = itsTable.evaluate(aCL);
					}
					while (aMembers.cardinality() < aMinimumCoverage);
					Log.logCommandLine(aCL.toString());
					Subgroup aSubgroup = new Subgroup(aCL, aMembers, aCL.size());

					// build model
					BinaryTable aBinaryTable = aBaseTable.selectRows(aMembers);
					aBayesian = new Bayesian(aBinaryTable);
					aBayesian.climb();
					aSubgroup.setDAG(aBayesian.getDAG()); // store DAG with subgroup for later use

					aQualities[i] = itsQualityMeasure.calculate(aSubgroup);
					Log.logCommandLine((i + 1) + "," + aSubgroup.getCoverage() + "," + aQualities[i]);
				}

				break;
			}
			case DOUBLE_REGRESSION :
				//TODO implement
			case DOUBLE_CORRELATION :
			{
				Column aPrimaryColumn = itsTable.getColumn(itsTargetConcept.getPrimaryTarget());
				Column aSecondaryColumn = itsTable.getColumn(itsTargetConcept.getSecondaryTarget());
				CorrelationMeasure itsBaseCM =
					new CorrelationMeasure(itsSearchParameters.getQualityMeasure(), aPrimaryColumn, aSecondaryColumn);

				int aDepth = itsSearchParameters.getSearchDepth();
				int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
				for (int i=0; i<theNrRepetitions; i++)
				{
					ConditionList aCL;
					BitSet aMembers;
					do
					{
						aCL = getRandomConditionList(aDepth, aRandom);
						aMembers = itsTable.evaluate(aCL);
					}
					while (aMembers.cardinality() < aMinimumCoverage);
					Log.logCommandLine(aCL.toString());
					Subgroup aSubgroup = new Subgroup(aCL, aMembers, aCL.size());

					CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);

					for (int j=0; j<itsTable.getNrRows(); j++)
						if (aSubgroup.getMembers().get(j))
							aCM.addObservation(aPrimaryColumn.getFloat(j), aSecondaryColumn.getFloat(j));

					aQualities[i] = aCM.getEvaluationMeasureValue();
				}
				break;
			}
		}
		return new NormalDistribution(aQualities); //return the normal distribution belonging to this random sample
	}

	public ConditionList getRandomConditionList(int theDepth, Random theRandom)
	{
		ConditionList aCL = new ConditionList();

		int aDepth = 1+theRandom.nextInt(theDepth); //random nr between 1 and theDepth (incl)

		for (int j = 0; j < aDepth; j++) // j conditions
		{
			Attribute anAttribute;
			do
			{
				anAttribute = itsTable.getAttribute(theRandom.nextInt(itsTable.getNrColumns()));
			}
			while (!anAttribute.isNumericType());
			int anOperator = theRandom.nextBoolean() ?
					Condition.LESS_THAN_OR_EQUAL : Condition.GREATER_THAN_OR_EQUAL;
			Condition aCondition = new Condition(anAttribute, anOperator);
			float aMin = itsTable.getColumn(anAttribute).getMin();
			float aMax = itsTable.getColumn(anAttribute).getMax();
			aCondition.setValue(
				Float.toString(aMin + (aMax - aMin) / 4 + (aMax - aMin) * theRandom.nextFloat() / 2));
			aCL.addCondition(aCondition);
		}
		return aCL;
	}
}
