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

	public double[] randomSubgroups(int theNrRepetitions)
	{
		double[] aQualities = new double[theNrRepetitions];
		Random aRandom = new Random(System.currentTimeMillis());
		int aNrRows  =itsTable.getNrRows();
		int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
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
						aSubgroupSize = (int) (aRandom.nextDouble() * aNrRows);
					while (aSubgroupSize < aMinimumCoverage  || aSubgroupSize==aNrRows);
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
						aSubgroupSize = (int) (aRandom.nextDouble() * aNrRows);
					while (aSubgroupSize < aMinimumCoverage  || aSubgroupSize==aNrRows);
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
						aSubgroupSize = (int) (aRandom.nextDouble() * aNrRows);
					while (aSubgroupSize < aMinimumCoverage  || aSubgroupSize==aNrRows);
					Subgroup aSubgroup = itsTable.getRandomSubgroup(aSubgroupSize);

					CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);

					for (int j=0; j<aNrRows; j++)
						if (aSubgroup.getMembers().get(j))
							aCM.addObservation(aPrimaryColumn.getFloat(j), aSecondaryColumn.getFloat(j));

					aQualities[i] = aCM.getEvaluationMeasureValue();
				}
				break;
			}
		}
		return aQualities; //return the qualities of the random sample
	}


	/**
	* Generates a set of random descriptions of subgroups, by randomly combining random conditions on
	* attributes in the table. The random descriptions adhere to the search parameters.
	* For each of the subgroups related to the random conditions, the quality is computed.
	* @return the computed qualities.
	*/
	public double[] randomConditions(int theNrRepetitions)
	{
		double[] aQualities = new double[theNrRepetitions];
		Random aRandom = new Random(System.currentTimeMillis());
		int aDepth = itsSearchParameters.getSearchDepth();
		int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		int aNrRows = itsTable.getNrRows();

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
					ConditionList aCL;
					BitSet aMembers;
					do
					{
						aCL = getRandomConditionList(aDepth, aRandom);
						aMembers = itsTable.evaluate(aCL);
					}
					while (aMembers.cardinality() < aMinimumCoverage || aMembers.cardinality()==aNrRows);
//					Log.logCommandLine(aCL.toString());
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

				for (int i = 0; i < theNrRepetitions; i++) // random conditions
				{
					ConditionList aCL;
					BitSet aMembers;
					do
					{
						aCL = getRandomConditionList(aDepth, aRandom);
						aMembers = itsTable.evaluate(aCL);
					}
					while (aMembers.cardinality() < aMinimumCoverage || aMembers.cardinality()==aNrRows);
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

				for (int i=0; i<theNrRepetitions; i++)
				{
					ConditionList aCL;
					BitSet aMembers;
					do
					{
						aCL = getRandomConditionList(aDepth, aRandom);
						aMembers = itsTable.evaluate(aCL);
					}
					while (aMembers.cardinality() < aMinimumCoverage || aMembers.cardinality()==aNrRows);
					Log.logCommandLine(aCL.toString());
					Subgroup aSubgroup = new Subgroup(aCL, aMembers, aCL.size());

					CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);

					for (int j=0; j<aNrRows; j++)
						if (aSubgroup.getMembers().get(j))
							aCM.addObservation(aPrimaryColumn.getFloat(j), aSecondaryColumn.getFloat(j));

					aQualities[i] = aCM.getEvaluationMeasureValue();
				}
				break;
			}
		}
		return aQualities; //return the qualities belonging to this random sample
	}

	public double performRegressionTest(double[] theQualities, int theK, SubgroupSet theSubgroupSet)
	{
for (int i=0; i<theQualities.length; i++)
	Log.logCommandLine("theQualities["+ i + "]: " + theQualities[i]);

		// extract average quality of top-k subgroups
		Iterator<Subgroup> anIterator = theSubgroupSet.iterator();
		double aTopKQuality = 0.0;
		for (int i=0; i<theK; i++)
		{
			Subgroup aSubgroup = anIterator.next();
			aTopKQuality += aSubgroup.getMeasureValue();
		}
		aTopKQuality /= ((double) theK);
Log.logCommandLine("k: " + theK);
Log.logCommandLine("aTopKQuality: " + aTopKQuality);

		// make deep copy of double array
		int theNrRandomSubgroups = theQualities.length;
		double[] aCopy = new double[theNrRandomSubgroups];
		for (int i=0; i<theNrRandomSubgroups; i++)
			aCopy[i] = theQualities[i];

		// rescale all qualities between 0 and 1
		// also compute some necessary statistics
		Arrays.sort(aCopy);

		double aMin = Math.min(aCopy[0], aTopKQuality);
		double aMax = Math.max(aCopy[theNrRandomSubgroups-1], aTopKQuality);
		double xBar = 0.5; // given our scaling this always holds
		double yBar = 0.0; // initial value
Log.logCommandLine("Min: " + aMin);
Log.logCommandLine("Max: " + aMax);
		for (int i=0; i<theNrRandomSubgroups; i++)
		{
			aCopy[i] = (aCopy[i]-aMin)/(aMax-aMin);
			yBar += aCopy[i];
		}
		aTopKQuality = (aTopKQuality-aMin)/(aMax-aMin);
		yBar = (yBar+aTopKQuality)/((double) theNrRandomSubgroups + 1);

		// perform least squares linear regression on equidistant x-values and computed y-values
		double xxBar = 0.25; // initial value: this equals the square of (the x-value of our subgroup minus xbar)
		double xyBar = 0.5 * (aTopKQuality - yBar);
		double[] anXs = new double[theNrRandomSubgroups];
		for (int i=0; i<theNrRandomSubgroups; i++)
			anXs[i] = ((double)i) / ((double)theNrRandomSubgroups);

		for (int i=0; i<theNrRandomSubgroups; i++)
		{
			xxBar += (anXs[i] - xBar) * (anXs[i] - xBar);
			xyBar += (anXs[i] - xBar) * (aCopy[i] - yBar);
		}
		double beta1 = xyBar / xxBar;
		double beta0 = yBar - beta1 * xBar;
		// this gives us the regression line y = beta1 * x + beta0
		Log.logCommandLine("Fitted regression line: y = " + beta1 + " * x + " + beta0);
		double aScore = aTopKQuality - beta1 - beta0; // the regression test score now equals the average quality of the top-k subgroups, minus the regression value at x=1.
		Log.logCommandLine("Regression test score: " + aScore);
		return aScore;
	}

	public double[] performRegressionTest(double[] theQualities, SubgroupSet theSubgroupSet)
	{
		double aOne = performRegressionTest(theQualities, 1, theSubgroupSet);
		double aTen = Math.PI;
		if (theSubgroupSet.size()>=10)
			aTen = performRegressionTest(theQualities, 10, theSubgroupSet);
		double[] aResult = {aOne, aTen};
		return aResult;
	}

	public ConditionList getRandomConditionList(int theDepth, Random theRandom)
	{
		ConditionList aCL = new ConditionList();

		int aDepth = 1+theRandom.nextInt(theDepth); //random nr between 1 and theDepth (incl)
		int aNrColumns = itsTable.getNrColumns();

		for (int j = 0; j < aDepth; j++) // j conditions
		{
/*
			Attribute anAttribute = itsTable.getAttribute(theRandom.nextInt(aNrColumns));
			while (itsTargetConcept.isTargetAttribute(anAttribute))
			{
				anAttribute = itsTable.getAttribute(theRandom.nextInt(aNrColumns));
			}
*/
			Attribute anAttribute;
			do
				anAttribute = itsTable.getAttribute(theRandom.nextInt(aNrColumns));
			while (itsTargetConcept.isTargetAttribute(anAttribute));

			int anOperator;
			Condition aCondition;
			switch(anAttribute.getType())
			{
				case BINARY :
				{
					anOperator = Condition.EQUALS;
					aCondition = new Condition(anAttribute, anOperator);
					aCondition.setValue(theRandom.nextBoolean() ? "1" : "0");
					break;
				}
				case NOMINAL :
				{
					anOperator = Condition.EQUALS;
					aCondition = new Condition(anAttribute, anOperator);
					TreeSet<String> aDomain = itsTable.getColumn(anAttribute).getDomain();
					int aNrDistinct = aDomain.size();
					int aRandomIndex = (int) (theRandom.nextDouble()* (double) aNrDistinct);
					Iterator<String> anIterator = aDomain.iterator();
					String aValue = anIterator.next();
					for (int i=0; i<aRandomIndex; i++)
						aValue = anIterator.next();
					aCondition.setValue(aValue);
					break;
				}
				case NUMERIC :
				default :
				{
					anOperator = theRandom.nextBoolean() ?
						Condition.LESS_THAN_OR_EQUAL : Condition.GREATER_THAN_OR_EQUAL;
					aCondition = new Condition(anAttribute, anOperator);
					float aMin = itsTable.getColumn(anAttribute).getMin();
					float aMax = itsTable.getColumn(anAttribute).getMax();
					aCondition.setValue(
						Float.toString(aMin + (aMax - aMin) / 4 + (aMax - aMin) * theRandom.nextFloat() / 2));
					break;
				}
			}
			aCL.addCondition(aCondition);
		}
		return aCL;
	}
}
