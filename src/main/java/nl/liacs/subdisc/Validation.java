package nl.liacs.subdisc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import nl.liacs.subdisc.ConditionListBuilder.ConditionListA;
import nl.liacs.subdisc.gui.*;

/**
 * Functionality related to the statistical validation of subgroups.
 */
public class Validation
{
	private SearchParameters itsSearchParameters;
	private TargetConcept itsTargetConcept;
	private QualityMeasure itsQualityMeasure;
	private Table itsTable;
	private BitSet itsSelection;

	/**
	 * NOTE theQualityMeasure is only used for 'Random Subsets' and
	 * 'Random Descriptions', and then only for the {@link TargetType}
	 * {@link TargetType#SINGLE_NOMINAL} and {@link TargetType#MULTI_LABEL}.
	 *
	 * @param theSearchParameters
	 * @param theTable
	 * @param theQualityMeasure
	 */
	public Validation(SearchParameters theSearchParameters, Table theTable, BitSet theSelection, QualityMeasure theQualityMeasure)
	{
		itsSearchParameters = theSearchParameters;
		itsTargetConcept = theSearchParameters.getTargetConcept();
		itsTable = theTable;
		itsSelection = theSelection;
		itsQualityMeasure = theQualityMeasure;
	}

	// FIXME MM - this method should not be separate from the constructor
	//            the data can be changed in between, causing bugs
	//            also, the whole class should consist of just static methods
	//            of split constructor (not all TargetTypes use QualityMeasure)
	public double[] getQualities(String[] theSetup)
	{
		if (!RandomQualitiesWindow.isValidRandomQualitiesSetup(theSetup))
			return null;

		String aMethod = theSetup[0];
		int aNrRepetitions = Integer.parseInt(theSetup[1]);

		if (RandomQualitiesWindow.RANDOM_SUBSETS.equals(aMethod))
			return getRandomQualities(true, aNrRepetitions);
		else if (RandomQualitiesWindow.RANDOM_DESCRIPTIONS.equals(aMethod))
			return getRandomQualities(false, aNrRepetitions);
		else if (RandomQualitiesWindow.SWAP_RANDOMIZATION.equals(aMethod))
			return swapRandomization(aNrRepetitions);

		return null;
	}

	// add TargetTypes implemented in getRandomQualitites() to this list
	public static boolean isValidRandomQualitiesTargetType(TargetType theTargetType)
	{
		return theTargetType == TargetType.SINGLE_NOMINAL ||
			theTargetType == TargetType.SINGLE_NUMERIC ||
			theTargetType == TargetType.DOUBLE_REGRESSION ||
            theTargetType == TargetType.DOUBLE_CORRELATION ||
            theTargetType == TargetType.DOUBLE_BINARY ||
			theTargetType == TargetType.MULTI_LABEL ||
			theTargetType == TargetType.LABEL_RANKING;
	}

	public double[] getRandomQualities(boolean forSubgroups, int theNrRepetitions)
	{
		final int aMinimumCoverage = itsSearchParameters.getMinimumCoverage();
		//final Random aRandom = new Random(System.currentTimeMillis());
		final Random aRandom = new Random(10);
		final int aDepth = itsSearchParameters.getSearchDepth();

		final TargetType aTargetType = itsTargetConcept.getTargetType();
		switch (aTargetType)
		{
			case SINGLE_NOMINAL :
			{
				return getSingleNominalQualities(forSubgroups, theNrRepetitions, aMinimumCoverage, aRandom, aDepth);
			}
			case SINGLE_NUMERIC :
			{
				return getSingleNumericQualities(forSubgroups, theNrRepetitions, aMinimumCoverage, aRandom, aDepth);
			}
			case SINGLE_ORDINAL:
			{
				throw new AssertionError(aTargetType);
			}
			case DOUBLE_REGRESSION :
			{
				return getDoubleRegressionQualities(forSubgroups, theNrRepetitions, aMinimumCoverage, aRandom, aDepth);
			}
			case DOUBLE_CORRELATION :
			{
				return getDoubleCorrelationQualities(forSubgroups, theNrRepetitions, aMinimumCoverage, aRandom, aDepth);
			}
			case DOUBLE_BINARY :
			{
				return getDoubleBinaryQualities(forSubgroups, theNrRepetitions, aMinimumCoverage, aRandom, aDepth);
			}
			case MULTI_LABEL :
			{
				return getMultiLabelQualities(forSubgroups, theNrRepetitions, aMinimumCoverage, aRandom, aDepth);
			}
			case LABEL_RANKING :
			{
				return getLabelRankingQualities(forSubgroups, theNrRepetitions, aMinimumCoverage, aRandom, aDepth);
			}
			case MULTI_BINARY_CLASSIFICATION :
			{
				throw new AssertionError(aTargetType);
			}
			default :
			{
				throw new AssertionError(aTargetType);
			}
		}
	}

	// if forSubgroups is true, create Subgroups, else create Conditions
	// if forSubgroups is true, theDepth is ignored
	private double[] getSingleNominalQualities(boolean forSubgroups, int theNrRepetitions, int theMinimumCoverage, Random theRandom, int theDepth)
	{
////////////////////////////////////////////////////////////////////////////////
///// FIXME - WHY IS THIS HERE, itsBinaryTarget IS AVAILABLE ALREADY       /////
/////         TECHNICALLY, IT IS ALSO WRONG -> BUG                         /////
/////         WHEN DATA IS CHANGED AFTER THE ResultWindow IS CREATED, THE  /////
/////         THE ORIGINAL Condition MIGHT NOT BE ABLE TO RECREATE THE     /////
/////         SAME BitSet AS itsBinaryTarget, BUT THIS IS A WIDER PROBLEM  /////
////////////////////////////////////////////////////////////////////////////////
		// create a binary target
		Column aTarget = itsTargetConcept.getPrimaryTarget();
		ConditionBase aConditionBase = new ConditionBase(aTarget, Operator.EQUALS);

		String aValue = itsTargetConcept.getTargetValue();
		Condition aCondition;
		switch (aTarget.getType())
		{
			case NOMINAL :
				aCondition = new Condition(aConditionBase, aValue);
				break;
			case BINARY :
				if (!AttributeType.isValidBinaryValue(aValue))
					throw new IllegalArgumentException(aValue + " is not a valid BINARY value");
				aCondition = new Condition(aConditionBase, AttributeType.isValidBinaryTrueValue(aValue));
				break;
			default :
				throw new AssertionError(aTarget.getType());
		}

		BitSet b = new BitSet(itsTable.getNrRows());
		b.set(0, itsTable.getNrRows());
		b = aTarget.evaluate(b, aCondition);
////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

		final double[] aQualities = new double[theNrRepetitions];

		for (int i = 0; i < theNrRepetitions; ++i)
		{
			Subgroup aSubgroup;

			// essential switch between Subgroups/ Conditions
			if (forSubgroups)
				aSubgroup = getValidSubgroup(theMinimumCoverage, theRandom);
			else
				aSubgroup = getValidSubgroup(theDepth, theMinimumCoverage, theRandom);

			BitSet aMembers = aSubgroup.getMembers();
			// aMembers is a clone so this is safe
			aMembers.and(b);
			int aCountHeadBody = aMembers.cardinality();

			aQualities[i] = itsQualityMeasure.calculate(aCountHeadBody, aSubgroup.getCoverage());
		}

		return aQualities;
	}

	// if forSubgroups is true, create Subgroups, else create Conditions
	// if forSubgroups is true, theDepth is ignored
	private double[] getSingleNumericQualities(boolean forSubgroups, int theNrRepetitions, int theMinimumCoverage, Random theRandom, int theDepth)
	{
		final double[] aQualities = new double[theNrRepetitions];

		final Column aTarget = itsTargetConcept.getPrimaryTarget();

		for (int i = 0; i < theNrRepetitions; ++i)
		{
			Subgroup aSubgroup;

			// essential switch between Subgroups/ Conditions
			if (forSubgroups)
				aSubgroup = getValidSubgroup(theMinimumCoverage, theRandom);
			else
				aSubgroup = getValidSubgroup(theDepth, theMinimumCoverage, theRandom);

			BitSet aMembers = aSubgroup.getMembers();

			QM aQM = itsSearchParameters.getQualityMeasure();
			Statistics aStatistics = aTarget.getStatistics(null, aMembers, aQM == QM.MMAD, QM.requiredStats(aQM).contains(Stat.COMPL)); //TODO check for theSelection

			ProbabilityDensityFunction aPDF = null;
			// DEBUG
			if (!ProbabilityDensityFunction.USE_ProbabilityDensityFunction2)
				aPDF = new ProbabilityDensityFunction(itsQualityMeasure.getProbabilityDensityFunction(), aMembers);
			else
				aPDF = new ProbabilityDensityFunction2(itsQualityMeasure.getProbabilityDensityFunction(), aMembers);
			aPDF.smooth();

			aQualities[i] = itsQualityMeasure.calculate(aStatistics, aPDF);
		}

		return aQualities;
	}

	// if forSubgroups is true, create Subgroups, else create Conditions
	// if forSubgroups is true, theDepth is ignored
	private double[] getDoubleRegressionQualities(boolean forSubgroups, int theNrRepetitions, int theMinimumCoverage, Random theRandom, int theDepth)
	{
		final double[] aQualities = new double[theNrRepetitions];

		Column aPrimaryColumn = itsTargetConcept.getPrimaryTarget();
		Column aSecondaryColumn = itsTargetConcept.getSecondaryTarget();
		RegressionMeasure itsBaseRM =
			new RegressionMeasure(itsSearchParameters.getQualityMeasure(), aPrimaryColumn, aSecondaryColumn);

		for (int i = 0; i < theNrRepetitions; ++i)
		{
			Subgroup aSubgroup;

			// essential switch between Subgroups/ Conditions
			if (forSubgroups)
				aSubgroup = getValidSubgroup(theMinimumCoverage, theRandom);
			else
				aSubgroup = getValidSubgroup(theDepth, theMinimumCoverage, theRandom);

			BitSet aMembers = aSubgroup.getMembers();

			RegressionMeasure aRM = new RegressionMeasure(itsBaseRM, aMembers);

			aQualities[i] = aRM.getEvaluationMeasureValue();
		}

		return aQualities;
	}

	// if forSubgroups is true, create Subgroups, else create Conditions
	// if forSubgroups is true, theDepth is ignored
	private double[] getDoubleCorrelationQualities(boolean forSubgroups, int theNrRepetitions, int theMinimumCoverage, Random theRandom, int theDepth)
	{
		final double[] aQualities = new double[theNrRepetitions];

		Column aPrimaryColumn = itsTargetConcept.getPrimaryTarget();
		Column aSecondaryColumn = itsTargetConcept.getSecondaryTarget();
		CorrelationMeasure itsBaseCM =
			new CorrelationMeasure(itsSearchParameters.getQualityMeasure(), aPrimaryColumn, aSecondaryColumn);

		for (int i = 0; i < theNrRepetitions; ++i)
		{
			Subgroup aSubgroup;

			// essential switch between Subgroups/ Conditions
			if (forSubgroups)
				aSubgroup = getValidSubgroup(theMinimumCoverage, theRandom);
			else
				aSubgroup = getValidSubgroup(theDepth, theMinimumCoverage, theRandom);

			BitSet aMembers = aSubgroup.getMembers();

			CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);

			for (int k = aMembers.nextSetBit(0); k >= 0; k = aMembers.nextSetBit(k))
				aCM.addObservation(aPrimaryColumn.getFloat(k), aSecondaryColumn.getFloat(k));

			aQualities[i] = aCM.getEvaluationMeasureValue();
		}

		return aQualities;
	}

    // if forSubgroups is true, create Subgroups, else create Conditions
    // if forSubgroups is true, theDepth is ignored
    private double[] getDoubleBinaryQualities(boolean forSubgroups, int theNrRepetitions, int theMinimumCoverage, Random theRandom, int theDepth)
    {
        final double[] aQualities = new double[theNrRepetitions];

        Column aPrimaryColumn = itsTargetConcept.getPrimaryTarget();
        Column aSecondaryColumn = itsTargetConcept.getSecondaryTarget();
        CorrelationMeasure itsBaseCM =
            new CorrelationMeasure(itsSearchParameters.getQualityMeasure(), aPrimaryColumn, aSecondaryColumn);

        for (int i = 0; i < theNrRepetitions; ++i)
        {
            Subgroup aSubgroup;

            // essential switch between Subgroups/ Conditions
            if (forSubgroups)
                aSubgroup = getValidSubgroup(theMinimumCoverage, theRandom);
            else
                aSubgroup = getValidSubgroup(theDepth, theMinimumCoverage, theRandom);

            BitSet aMembers = aSubgroup.getMembers();

            CorrelationMeasure aCM = new CorrelationMeasure(itsBaseCM);

            for (int k = aMembers.nextSetBit(0); k >= 0; k = aMembers.nextSetBit(k))
                aCM.addObservation(aPrimaryColumn.getFloat(k), aSecondaryColumn.getFloat(k));

            aQualities[i] = aCM.getEvaluationMeasureValue();
        }

        return aQualities;
    }

	// if forSubgroups is true, create Subgroups, else create Conditions
	// if forSubgroups is true, theDepth is ignored
	private double[] getMultiLabelQualities(boolean forSubgroups, int theNrRepetitions, int theMinimumCoverage, Random theRandom, int theDepth)
	{
		final double[] aQualities = new double[theNrRepetitions];

		// base model
		BinaryTable aBaseTable = new BinaryTable(itsTable, itsTargetConcept.getMultiTargets());
		Bayesian aBayesian = new Bayesian(aBaseTable);
		aBayesian.climb();

		for (int i = 0, j = aQualities.length; i < j; ++i)
		{
			Subgroup aSubgroup;

			// essential switch between Subgroups/ Conditions
			if (forSubgroups)
				aSubgroup = getValidSubgroup(theMinimumCoverage, theRandom);
			else
				aSubgroup = getValidSubgroup(theDepth, theMinimumCoverage, theRandom);

			// build model
			BinaryTable aBinaryTable = aBaseTable.selectRows(aSubgroup.getMembers());
			aBayesian = new Bayesian(aBinaryTable);
			aBayesian.climb();
			aSubgroup.setDAG(aBayesian.getDAG()); // store DAG with subgroup for later use

			aQualities[i] = itsQualityMeasure.calculate(aSubgroup);

			// XXX original code only did this for Condition
			if (!forSubgroups)
				Log.logCommandLine((i + 1) + "," + aSubgroup.getCoverage() + "," + aQualities[i]);
		}

		return aQualities;
	}

	//TODO: fix implementation
	// if forSubgroups is true, create Subgroups, else create Conditions
	// if forSubgroups is true, theDepth is ignored
	private double[] getLabelRankingQualities(boolean forSubgroups, int theNrRepetitions, int theMinimumCoverage, Random theRandom, int theDepth)
	{
		final double[] aQualities = new double[theNrRepetitions];

		Column aTarget = itsTargetConcept.getPrimaryTarget();
		LabelRanking aLR = aTarget.getAverageRanking(null); //average ranking over entire dataset
		LabelRankingMatrix aLRM = aTarget.getAverageRankingMatrix(null);
		QualityMeasure aQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsTable.getNrRows(), aLR, aLRM);

		//temp<--
		BufferedWriter br = null;
		try
		{
		final File f  = new File("output.txt");
		br = new BufferedWriter(new FileWriter(f));
		//temp-->

		for (int i = 0; i < theNrRepetitions; ++i)
		{
			Subgroup aSubgroup;

			// essential switch between Subgroups/ Conditions
			if (forSubgroups)
				aSubgroup = getValidSubgroup(theMinimumCoverage, theRandom);
			else
				aSubgroup = getValidSubgroup(theDepth, theMinimumCoverage, theRandom);

			LabelRankingMatrix aSubgroupLRM = aTarget.getAverageRankingMatrix(aSubgroup);
			aQualities[i] = aQualityMeasure.computeLabelRankingDistance(aSubgroup.getCoverage(), aSubgroupLRM);
			Log.logCommandLine("qual: " + aQualities[i]);

			//temp<--
			br.write(aQualities[i] + "\r");
			//temp-->
		}
		}
		//temp<--
		catch (IOException e) {}
		finally
		{
			if (br != null)
			{
				try { br.close(); }
				catch (IOException e) {}
			}
		}
		//temp-->

		return aQualities;
	}

	// for RANDOM_SUBSETS/Subgroups, always uses an updated Random value
	private Subgroup getValidSubgroup(int theMinimumCoverage, Random theRandom)
	{
		final int aNrRows = itsTable.getNrRows();
		int aSubgroupSize;

		do
			aSubgroupSize = (int) (theRandom.nextDouble() * aNrRows);
		while (aSubgroupSize < theMinimumCoverage || aSubgroupSize == aNrRows);

		return new Subgroup(itsTable.getRandomBitSet(aSubgroupSize));
	}

	// for RANDOM_DESCRIPTIONS/Conditions, always uses the same Random value
	private Subgroup getValidSubgroup(int theDepth, int theMinimumCoverage, Random theRandom)
	{
		final int aNrRows = itsTable.getNrRows();
		int aSubgroupSize;

		//ConditionList aCL;
		ConditionListA aCL;
		BitSet aMembers;

		do
		{
			aCL = getRandomConditionList(theDepth, theRandom);
			aMembers = itsTable.evaluate(aCL);
			aSubgroupSize = aMembers.cardinality();
		}
		while (aSubgroupSize < theMinimumCoverage || aSubgroupSize == aNrRows);

		Log.logCommandLine(aCL.toString());

		return new Subgroup(aMembers);
	}

	/**
	 * Swap randomizes the original {@link Table} and restores it to the
	 * original state afterwards.
	 *
	 * @param theNrRepetitions the number of times to perform a permutation
	 * of the {@link TargetConcept}.
	 *
	 * @return an array holding the qualities of the best scoring
	 * {@link Subgroup} of each permutation.
	 */
	private double[] swapRandomization(int theNrRepetitions)
	{
		// Memorize COMMANDLINELOG setting
		boolean aCOMMANDLINELOGmem = Log.COMMANDLINELOG;
		double[] aQualities = new double[theNrRepetitions];

		// Always back up and restore columns that will be swap randomized.
		final TargetType aTargetType = itsTargetConcept.getTargetType();
		switch (aTargetType)
		{
			case SINGLE_NOMINAL :
			{
				// back up column that will be swap randomized
				Column aPrimaryCopy = itsTargetConcept.getPrimaryTarget().copy();
				int aPositiveCount =
					itsTargetConcept.getPrimaryTarget().countValues(itsTargetConcept.getTargetValue(), itsSelection);

				// generate swap randomized random results
				for (int i = 0, j = theNrRepetitions; i < j; ++i)
				{
					itsTable.swapRandomizeTarget(itsTargetConcept);
					i = runSRSD(new SubgroupDiscovery(itsSearchParameters, itsTable, itsSelection, aPositiveCount, null), aQualities, i);
				}

				// restore column that was swap randomized
				itsTargetConcept.setPrimaryTarget(aPrimaryCopy);
				itsTable.getColumns().set(aPrimaryCopy.getIndex(), aPrimaryCopy);

				break;
			}
			case SINGLE_NUMERIC :
			{
				// back up column that will be swap randomized
				Column aPrimaryCopy = itsTargetConcept.getPrimaryTarget().copy();
				float aTargetAverage = itsTargetConcept.getPrimaryTarget().getAverage(itsSelection);

				// generate swap randomized random results
				for (int i = 0, j = theNrRepetitions; i < j; ++i)
				{
					itsTable.swapRandomizeTarget(itsTargetConcept);
					SubgroupDiscovery anSD = new SubgroupDiscovery(itsSearchParameters, itsTable, itsSelection, aTargetAverage, null);
					i = runSRSD(anSD, aQualities, i);
				}

				// restore column that was swap randomized
				itsTargetConcept.setPrimaryTarget(aPrimaryCopy);
				itsTable.getColumns().set(aPrimaryCopy.getIndex(), aPrimaryCopy);

				break;
			}
			case DOUBLE_REGRESSION :
			{
				// back up columns that will be swap randomized
				Column aPrimaryCopy = itsTargetConcept.getPrimaryTarget().copy();
				Column aSecondaryCopy = itsTargetConcept.getSecondaryTarget().copy();

				// generate swap randomized random results
				for (int i = 0, j = theNrRepetitions; i < j; ++i)
				{
					itsTable.swapRandomizeTarget(itsTargetConcept);
					i = runSRSD(new SubgroupDiscovery(itsSearchParameters, itsTable, itsSelection, true, null), aQualities, i);
				}

				// restore columns that were swap randomized
				itsTargetConcept.setPrimaryTarget(aPrimaryCopy);
				itsTable.getColumns().set(aPrimaryCopy.getIndex(), aPrimaryCopy);
				itsTargetConcept.setSecondaryTarget(aSecondaryCopy);
				itsTable.getColumns().set(aSecondaryCopy.getIndex(), aSecondaryCopy);

				break;
			}
			case DOUBLE_CORRELATION :
			{
				// back up columns that will be swap randomized
				Column aPrimaryCopy = itsTargetConcept.getPrimaryTarget().copy();
				Column aSecondaryCopy = itsTargetConcept.getSecondaryTarget().copy();

				// generate swap randomized random results
				for (int i = 0, j = theNrRepetitions; i < j; ++i)
				{
					itsTable.swapRandomizeTarget(itsTargetConcept);
					i = runSRSD(new SubgroupDiscovery(itsSearchParameters, itsTable, itsSelection, false, null), aQualities, i);
				}

				// restore columns that were swap randomized
				itsTargetConcept.setPrimaryTarget(aPrimaryCopy);
				itsTable.getColumns().set(aPrimaryCopy.getIndex(), aPrimaryCopy);
				itsTargetConcept.setSecondaryTarget(aSecondaryCopy);
				itsTable.getColumns().set(aSecondaryCopy.getIndex(), aSecondaryCopy);

				break;
			}
			case DOUBLE_BINARY :
			{
				// back up columns that will be swap randomized
				Column aPrimaryCopy = itsTargetConcept.getPrimaryTarget().copy();
				Column aSecondaryCopy = itsTargetConcept.getSecondaryTarget().copy();

				// generate swap randomized random results
				for (int i = 0, j = theNrRepetitions; i < j; ++i)
				{
					itsTable.swapRandomizeTarget(itsTargetConcept);
					i = runSRSD(new SubgroupDiscovery(itsSearchParameters, itsTable, itsSelection, false, null), aQualities, i);
				}

				// restore columns that were swap randomized
				itsTargetConcept.setPrimaryTarget(aPrimaryCopy);
				itsTable.getColumns().set(aPrimaryCopy.getIndex(), aPrimaryCopy);
				itsTargetConcept.setSecondaryTarget(aSecondaryCopy);
				itsTable.getColumns().set(aSecondaryCopy.getIndex(), aSecondaryCopy);

				break;
			}
			case MULTI_LABEL :
			{
				// back up columns that will be swap randomized
				List<Column> aMultiCopy =
					new ArrayList<Column>(itsTargetConcept.getMultiTargets().size());
				for (Column c : itsTargetConcept.getMultiTargets())
					aMultiCopy.add(c.copy());

				// generate swap randomized random results
				for (int i = 0, j = theNrRepetitions; i < j; ++i)
				{
					// swapRandomization should be performed before creating new SubgroupDiscovery
					itsTable.swapRandomizeTarget(itsTargetConcept);
					i = runSRSD(new SubgroupDiscovery(itsSearchParameters, itsTable, itsSelection, null), aQualities, i);
				}

				// restore columns that were swap randomized
				itsTargetConcept.setMultiTargets(aMultiCopy);
				for (Column c : aMultiCopy)
					itsTable.getColumns().set(c.getIndex(), c);

				break;
			}
			case MULTI_BINARY_CLASSIFICATION :
			{
				throw new AssertionError(aTargetType);
			}
			case LABEL_RANKING:
			{
				// back up column that will be swap randomized
				Column aPrimaryCopy = itsTargetConcept.getPrimaryTarget().copy();

				// generate swap randomized random results
				for (int i = 0, j = theNrRepetitions; i < j; ++i)
				{
					itsTable.swapRandomizeTarget(itsTargetConcept);
					i = runSRSD(new SubgroupDiscovery(itsSearchParameters, null, itsTable, itsSelection), aQualities, i);
				}

				// restore column that was swap randomized
				itsTargetConcept.setPrimaryTarget(aPrimaryCopy);
				itsTable.getColumns().set(aPrimaryCopy.getIndex(), aPrimaryCopy);

				break;
			}
			default :
			{
				throw new AssertionError(aTargetType);
			}
		}

		Log.COMMANDLINELOG = aCOMMANDLINELOGmem;
		return aQualities;
	}

	//returns the 5% significance of swap randomisation.
	public float getSignWithSwapRand(int theNrRepetitions)
	{
		double[] aQualities = swapRandomization(theNrRepetitions);
		NormalDistribution aDistro = new NormalDistribution(aQualities);
		return aDistro.getFivePercentSignificance();
	}

	/*
	 * NOTE for the first result (i = 0) to be not equal to the original
	 * mining result the calling function should run:
	 * itsTable.swapRandomizeTarget(itsTargetConcept);
	 * before creating the new theSubgroupDiscovery, and calling this
	 * method.
	 */
	private int runSRSD(SubgroupDiscovery theSubgroupDiscovery, double[] theQualities, int theRepetition)
	{
		//quality minimum should not be taken into account when computing distribution of random qualities
		theSubgroupDiscovery.ignoreQualityMinimum();

		Log.COMMANDLINELOG = false;
		theSubgroupDiscovery.mine(System.currentTimeMillis(), itsSearchParameters.getNrThreads());
		Log.COMMANDLINELOG = true;
		SubgroupSet aSubgroupSet = theSubgroupDiscovery.getResult();
		if (aSubgroupSet.size() == 0)
			--theRepetition; // if no subgroups are found, try again
		else
		{
			theQualities[theRepetition] = aSubgroupSet.getBestScore();
			Log.logCommandLine((theRepetition + 1) + ", " + theQualities[theRepetition]);
		}

		return theRepetition;
	}

	//private ConditionList getRandomConditionList(int theDepth, Random theRandom)
	private ConditionListA getRandomConditionList(int theDepth, Random theRandom)
	{
		int aDepth = 1+theRandom.nextInt(theDepth); //random nr between 1 and theDepth (incl)
		//ConditionList aCL = new ConditionList(aDepth);
		ConditionListA aCL = ConditionListBuilder.emptyList();
		int aNrColumns = itsTable.getNrColumns();

		for (int j = 0; j < aDepth; j++) // j conditions
		{
			Column aColumn;
			do
				aColumn = itsTable.getColumn(theRandom.nextInt(aNrColumns));
			while (itsTargetConcept.isTargetAttribute(aColumn) || !aColumn.getIsEnabled());

			ConditionBase aConditionBase;
			Condition aCondition;
			switch (aColumn.getType())
			{
				case NOMINAL :
				{
					// select a random value from the domain
					TreeSet<String> aDomain = aColumn.getDomain();
					int aNrDistinct = aDomain.size();
					int aRandomIndex = (int) (theRandom.nextDouble() * aNrDistinct);
					Iterator<String> anIterator = aDomain.iterator();
					String aValue = anIterator.next();
					for (int i=0; i<aRandomIndex; i++)
						aValue = anIterator.next();

					aConditionBase = new ConditionBase(aColumn, Operator.EQUALS);
					aCondition = new Condition(aConditionBase, aValue);
					break;
				}
				case NUMERIC :
				{
					Operator anOperator = theRandom.nextBoolean() ?
						Operator.LESS_THAN_OR_EQUAL : Operator.GREATER_THAN_OR_EQUAL;

					float aMin = aColumn.getMin();
					float aMax = aColumn.getMax();
					float aRange = aMax - aMin;
					// fairly crude way of producing random thresholds, but will do for now
					float aValue = (aMin + 0.1f*aRange + 0.8f*aRange*theRandom.nextFloat());

					aConditionBase = new ConditionBase(aColumn, anOperator);
					// value may not occur in Column, so can not set sort index
					aCondition = new Condition(aConditionBase, aValue, Condition.UNINITIALISED_SORT_INDEX);
					break;
				}
				case BINARY :
				{
					aConditionBase = new ConditionBase(aColumn, Operator.EQUALS);
					aCondition = new Condition(aConditionBase, theRandom.nextBoolean());
					break;
				}
				default :
					throw new AssertionError(aColumn.getType());
			}
			//aCL.addCondition(aCondition);
			aCL = ConditionListBuilder.createList(aCL, aCondition);
		}
		return aCL;
	}

	public static final double[] performRegressionTest(double[] theQualities, SubgroupSet theSubgroupSet)
	{
		double aOne = performRegressionTest(theQualities, 1, theSubgroupSet);
		double aTen = Math.PI;
		if (theSubgroupSet.size()>=10)
			aTen = performRegressionTest(theQualities, 10, theSubgroupSet);
		double[] aResult = {aOne, aTen};
		return aResult;
	}

	private static final double performRegressionTest(double[] theQualities, int theK, SubgroupSet theSubgroupSet)
	{
		//extract average quality
		double aTopKQuality = 0.0;
		for (Subgroup aSubgroup : theSubgroupSet)
			aTopKQuality += aSubgroup.getMeasureValue();
		aTopKQuality /= theK;

		// make deep copy of double array
		//public static double[] copyOf(double[] original, int newLength)
		int theNrRandomSubgroups = theQualities.length;
		double[] aCopy = Arrays.copyOf(theQualities, theQualities.length);

		// rescale all qualities between 0 and 1
		// also compute some necessary statistics
		Arrays.sort(aCopy);

		double aMin = Math.min(aCopy[0], aTopKQuality);
		double aMax = Math.max(aCopy[theNrRandomSubgroups-1], aTopKQuality);
		double xBar = 0.5; // given our scaling this always holds
		double yBar = 0.0; // initial value
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
		{
			anXs[i] = ((double)i) / ((double)theNrRandomSubgroups);
			Log.logCommandLine("" + anXs[i] + "\t" + aCopy[i]);
		}

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

	public static final double computeEmpiricalPValue(double[] theQualities, SubgroupSet theSubgroupSet)
	{
		//hardcoded
		int aK = 1;

		// extract average quality of top-k subgroups
		Iterator<Subgroup> anIterator = theSubgroupSet.iterator();
		double aTopKQuality = 0.0;
		for (int i=0; i<aK; i++)
		{
			Subgroup aSubgroup = anIterator.next();
			aTopKQuality += aSubgroup.getMeasureValue();
		}
		aTopKQuality /= aK;

		int aCount = 0;
		for (double aQuality : theQualities)
			if (aQuality > aTopKQuality)
				aCount++;

		Arrays.sort(theQualities);
		Log.logCommandLine("Empirical p-value: " + aCount/(double)theQualities.length);
//		Log.logCommandLine("score at alpha = 1%: " + theQualities[theQualities.length-theQualities.length/100]);
//		Log.logCommandLine("score at alpha = 5%: " + theQualities[theQualities.length-theQualities.length/20]);
//		Log.logCommandLine("score at alpha = 10%: " + theQualities[theQualities.length-theQualities.length/10]);
		return aCount / ((double) theQualities.length);
	}
}
