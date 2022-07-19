package nl.liacs.subdisc;

// Java lib
import java.io.*;
import java.util.*;
//import java.util.Map.Entry;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.ConditionListBuilder.ConditionList;

// Testing lib
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class EndToEndTest 
{
	@Test
	@DisplayName("Check end-to-end run on Adult.txt using SINGLE_NOMINAL")
	public void testAdult1()
	{
		//SINGLE_NOMINAL
		//d=1
		//numeric strategy = best

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
		Table aTable = aLoader.getTable();

		//sanity check
		assertEquals(aTable.getNrRows(), 1000);
		assertEquals(aTable.getNrColumns(), 15);

		//set target concept
		Column aTarget = aTable.getColumns().get(14); //get target
		TargetConcept aTC = new TargetConcept();
		aTC.setTargetType(TargetType.SINGLE_NOMINAL);
		aTC.setPrimaryTarget(aTarget);
		aTC.setTargetValue("gr50K");

		//set search parameters
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTC);
		anSP.setQualityMeasure(QM.CORTANA_QUALITY);
		anSP.setQualityMeasureMinimum(0.1f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy(SearchStrategy.BEAM);
		anSP.setNominalSets(false);
		anSP.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSP.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);
		SubgroupDiscovery anSD = Process.runSubgroupDiscovery(aTable, 0, null, anSP, false, 1, null);

		//actual tests
		assertEquals(aTarget.countValues("gr50K", null), 232); //how many positives in dataset
		assertEquals(anSD.getNumberOfSubgroups(), 10);
		SubgroupSet aResult = anSD.getResult();
		Iterator<Subgroup> anIterator = aResult.iterator();

		//subgroup 1
		Subgroup aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "marital-status = 'Married-civ-spouse'");
		assertEquals(aSubgroup.getCoverage(), 443);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0.51760f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 0.44018f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 195.0f);

		//subgroup 2
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "relationship = 'Husband'");
		assertEquals(aSubgroup.getCoverage(), 376);
		assertEquals(0.4533f, roundToFive(aSubgroup.getMeasureValue()));
		assertEquals(0.44681f, roundToFive(aSubgroup.getSecondaryStatistic()));
		assertEquals(aSubgroup.getTertiaryStatistic(), 168f);

		//subgroup 3
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "education-num >= 11.0");
		assertEquals(aSubgroup.getCoverage(), 327);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0,35995f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 0,42813f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 140.0f);

		//subgroup 4
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "age >= 33.0");
		assertEquals(aSubgroup.getCoverage(), 616);

		//subgroup 5
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "hours-per-week >= 43.0");
		assertEquals(aSubgroup.getCoverage(), 268);

		//subgroup 6
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "occupation = 'Exec-managerial'");
		assertEquals(aSubgroup.getCoverage(), 124);

		//subgroup 7
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "sex = 'Male'");
		assertEquals(aSubgroup.getCoverage(), 671);

		//subgroup 8
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "education = 'Bachelors'");
		assertEquals(aSubgroup.getCoverage(), 166);

		//subgroup 9
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "capital-gain >= 4386.0");
		assertEquals(aSubgroup.getCoverage(), 50);

		//subgroup 10
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "occupation = 'Prof-specialty'");
		assertEquals(aSubgroup.getCoverage(), 124);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0,12477f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 0,41129f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 51.0f);
	}

	@Test
	@DisplayName("Check end-to-end run on Adult.txt using SINGLE_NUMERIC (Z-Score)")
	public void testAdult2()
	{
		//SINGLE_NUMERIC
		//d=1
		//numeric strategy = best

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
		Table aTable = aLoader.getTable();

		//sanity check
		assertEquals(aTable.getNrRows(), 1000);
		assertEquals(aTable.getNrColumns(), 15);

		//set target concept
		Column aTarget = aTable.getColumns().get(0); //get target (age)
		TargetConcept aTC = new TargetConcept();
		aTC.setTargetType(TargetType.SINGLE_NUMERIC);
		aTC.setPrimaryTarget(aTarget);

		//set search parameters
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTC);
		anSP.setQualityMeasure(QM.Z_SCORE);
		anSP.setQualityMeasureMinimum(1f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy(SearchStrategy.BEAM);
		anSP.setNominalSets(false);
		anSP.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSP.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);
		SubgroupDiscovery anSD = Process.runSubgroupDiscovery(aTable, 0, null, anSP, false, 1, null);

		//actual tests
		assertEquals(aTarget.getAverage(null), 38.051f); //how many positives in dataset
		assertEquals(31, anSD.getNumberOfSubgroups());
		SubgroupSet aResult = anSD.getResult();
		Iterator<Subgroup> anIterator = aResult.iterator();

		//subgroup 1
		Subgroup aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "marital-status = 'Widowed'");
		assertEquals(aSubgroup.getCoverage(), 33);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 8.10605f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 56.87879f);
		assertEquals(roundToFive(aSubgroup.getTertiaryStatistic()), 13.05220f);

		//subgroup 2
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "relationship = 'Husband'");
		assertEquals(376f, aSubgroup.getCoverage());
		assertEquals(7.90342f, roundToFive(aSubgroup.getMeasureValue()));
	}

	@Test
	@DisplayName("Check end-to-end run on Adult.txt using SINGLE_NUMERIC (Explained Variance)")
	public void testAdult3()
	{
		//SINGLE_NUMERIC
		//d=1
		//numeric strategy = best

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
		Table aTable = aLoader.getTable();

		//sanity check
		assertEquals(aTable.getNrRows(), 1000);
		assertEquals(aTable.getNrColumns(), 15);

		//set target concept
		Column aTarget = aTable.getColumns().get(0); //get target (age)
		TargetConcept aTC = new TargetConcept();
		aTC.setTargetType(TargetType.SINGLE_NUMERIC);
		aTC.setPrimaryTarget(aTarget);

		//set search parameters
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTC);
		anSP.setQualityMeasure(QM.EXPLAINED_VARIANCE);
		anSP.setQualityMeasureMinimum(0f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy(SearchStrategy.BEAM);
		anSP.setNominalSets(false);
		anSP.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSP.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);
		SubgroupDiscovery anSD = Process.runSubgroupDiscovery(aTable, 0, null, anSP, false, 1, null);

		//actual tests
		assertEquals(aTarget.getAverage(null), 38.051f); //how many positives in dataset
		assertEquals(anSD.getNumberOfSubgroups(), 90);
		SubgroupSet aResult = anSD.getResult();
		Iterator<Subgroup> anIterator = aResult.iterator();

		//subgroup 1
		Subgroup aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "marital-status = 'Never-married'");
		assertEquals(aSubgroup.getCoverage(), 344);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0.25513f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 28.74419f);
		assertEquals(roundToFive(aSubgroup.getTertiaryStatistic()), 10.95279f);

		//subgroup 2
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "relationship = 'Own-child'");
		assertEquals(aSubgroup.getCoverage(), 151f);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0.19836f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 23.96026f);
		assertEquals(roundToFive(aSubgroup.getTertiaryStatistic()), 7.10763f);

		//subgroup 3
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "relationship = 'Husband'");
		assertEquals(aSubgroup.getCoverage(), 376f);

		//subgroup 90
		for (int i=0; i<87; i++)
			aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "marital-status = 'Married-spouse-absent'");
		assertEquals(aSubgroup.getCoverage(), 15f);
	}

	@Test
	@DisplayName("Check end-to-end run on Adult.txt using SINGLE_NOMINAL, with DFD computation added")
	public void testAdult4()
	{
		//SINGLE_NOMINAL
		//d=1
		//numeric strategy = best

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
		Table aTable = aLoader.getTable();

		//sanity check
		assertEquals(aTable.getNrRows(), 1000);
		assertEquals(aTable.getNrColumns(), 15);

		//set target concept
		Column aTarget = aTable.getColumns().get(14); //get target
		TargetConcept aTC = new TargetConcept();
		aTC.setTargetType(TargetType.SINGLE_NOMINAL);
		aTC.setPrimaryTarget(aTarget);
		String aTargetValue = "gr50K";
		aTC.setTargetValue(aTargetValue);

		//set search parameters except minimum quality
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTC);
		anSP.setQualityMeasure(QM.CORTANA_QUALITY);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy(SearchStrategy.BEAM);
		anSP.setNominalSets(false);
		anSP.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSP.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);

		//DFD computation
		int aPositiveCount = aTC.getPrimaryTarget().countValues(aTargetValue, null);
		QualityMeasure aQualityMeasure = new QualityMeasure(anSP.getQualityMeasure(), aTable.getNrRows(), aPositiveCount);
		Validation aValidation = new Validation(anSP, aTable, null, aQualityMeasure);
		anSP.setQualityMeasureMinimum(aValidation.getSignWithSwapRand(100));

		//run SD with computed minimum quality
		SubgroupDiscovery anSD = Process.runSubgroupDiscovery(aTable, 0, null, anSP, false, 1, null);

		//actual tests
		assertEquals(aTarget.countValues("gr50K", null), 232); //how many positives in dataset
		assertEquals(anSD.getNumberOfSubgroups(), 10);
		SubgroupSet aResult = anSD.getResult();
		Iterator<Subgroup> anIterator = aResult.iterator();

		//subgroup 1
		Subgroup aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "marital-status = 'Married-civ-spouse'");
		assertEquals(aSubgroup.getCoverage(), 443);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0.51760f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 0.44018f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 195.0f);

		//subgroup 2
		aSubgroup = anIterator.next();
		assertEquals(aSubgroup.toString(), "relationship = 'Husband'");
		assertEquals(376, aSubgroup.getCoverage());
		assertEquals(0.4533f, roundToFive(aSubgroup.getMeasureValue()));
		assertEquals(0.44681f, roundToFive(aSubgroup.getSecondaryStatistic()));
		assertEquals(aSubgroup.getTertiaryStatistic(), 168f);
	}

	@Test
	@DisplayName("Check end-to-end run on Adult.txt using SINGLE_NOMINAL. Subset selection on Sex = Male")
	public void testAdult5()
	{
		//SINGLE_NOMINAL
		//d=1
		//numeric strategy = best

		System.out.println("\n\n----------- testAdult5---------");
		//loading two tables for comparison of results
		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
		Table aLargeTable = aLoader.getTable();
		aLoader = new DataLoaderTXT(new File("src/test/resources/adult_male.txt"));
		Table aSmallTable = aLoader.getTable();

		//Subset selection
		Column aSex = aLargeTable.getColumns().get(9); //get "sex" attribute
		Operator anOperator = Operator.EQUALS;
		ConditionBase aCB = new ConditionBase(aSex, anOperator);
		Condition aCondition = new Condition(aCB, "Male");
		ConditionList aConditionList = ConditionListBuilder.createList(aCondition);
		BitSet aSelection = aLargeTable.evaluate(aConditionList);
		assertEquals(671, aSelection.cardinality());

		//set target concept (large table)
		Column aTarget = aLargeTable.getColumns().get(14); //get target
		TargetConcept aTCLarge = new TargetConcept();
		aTCLarge.setTargetType(TargetType.SINGLE_NOMINAL);
		aTCLarge.setPrimaryTarget(aTarget);
		aTCLarge.setTargetValue("gr50K");

		//set target concept (small table)
		aTarget = aSmallTable.getColumns().get(14); //get target
		TargetConcept aTCSmall = new TargetConcept();
		aTCSmall.setTargetType(TargetType.SINGLE_NOMINAL);
		aTCSmall.setPrimaryTarget(aTarget);
		aTCSmall.setTargetValue("gr50K");

		//set search parameters (large table)
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTCLarge);
		anSP.setQualityMeasure(QM.CORTANA_QUALITY);
		anSP.setQualityMeasureMinimum(0.1f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy(SearchStrategy.BEAM);
		anSP.setNominalSets(false);
		anSP.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSP.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);

		//set search parameters (small table)
		SearchParameters anSPSmall = new SearchParameters();
		anSPSmall.setTargetConcept(aTCSmall);
		anSPSmall.setQualityMeasure(QM.CORTANA_QUALITY);
		anSPSmall.setQualityMeasureMinimum(0.1f);
		anSPSmall.setSearchDepth(1);
		anSPSmall.setMinimumCoverage(2);
		anSPSmall.setMaximumCoverageFraction(1f);
		anSPSmall.setMaximumSubgroups(1000);
		anSPSmall.setMaximumTime(1000); //1000 seconds
		anSPSmall.setSearchStrategy(SearchStrategy.BEAM);
		anSPSmall.setNominalSets(false);
		anSPSmall.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSPSmall.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSPSmall.setSearchStrategyWidth(10);
		anSPSmall.setNrBins(8);
		anSPSmall.setNrThreads(1);

		//SD run on entire table with selection (sex = Male) specified
		SubgroupDiscovery anSDLarge = Process.runSubgroupDiscovery(aLargeTable, 0, aSelection, anSP, false, 1, null);
		SubgroupSet aResultLarge = anSDLarge.getResult();
		Iterator<Subgroup> anIteratorLarge = aResultLarge.iterator();

		//for comparison, SD run on separate table with only males
		SubgroupDiscovery anSDSmall = Process.runSubgroupDiscovery(aSmallTable, 0, null, anSPSmall, false, 1, null);
		SubgroupSet aResultSmall = anSDSmall.getResult();
		Iterator<Subgroup> anIteratorSmall = aResultSmall.iterator();

		assertEquals(anSDLarge.getNumberOfSubgroups(), anSDSmall.getNumberOfSubgroups());
		while (anIteratorLarge.hasNext() && anIteratorSmall.hasNext())
		{
			Subgroup aSubgroupA = anIteratorLarge.next();
			Subgroup aSubgroupB = anIteratorSmall.next();
			assertEquals(aSubgroupA.toString(), aSubgroupB.toString());
			assertEquals(aSubgroupB.getCoverage() , aSubgroupB.getCoverage());
			assertEquals(roundToFive(aSubgroupA.getMeasureValue()), roundToFive(aSubgroupB.getMeasureValue()));
			assertEquals(roundToFive(aSubgroupA.getSecondaryStatistic()), roundToFive(aSubgroupB.getSecondaryStatistic()));
			assertEquals(aSubgroupA.getTertiaryStatistic(), aSubgroupB.getTertiaryStatistic());
		}
	}

	@Test
	@DisplayName("Check end-to-end run on Adult.txt using SINGLE_NUMERIC, Z-SCORE. Subset selection on Sex = Male")
	public void testAdult6()
	{
		//SINGLE_NOMINAL
		//d=1
		//numeric strategy = best

		System.out.println("\n\n----------- testAdult6---------");
		//loading two tables for comparison of results
		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
		Table aLargeTable = aLoader.getTable();
		aLoader = new DataLoaderTXT(new File("src/test/resources/adult_male.txt"));
		Table aSmallTable = aLoader.getTable();

		//Subset selection
		Column aSex = aLargeTable.getColumns().get(9); //get "sex" attribute
		Operator anOperator = Operator.EQUALS;
		ConditionBase aCB = new ConditionBase(aSex, anOperator);
		Condition aCondition = new Condition(aCB, "Male");
		ConditionList aConditionList = ConditionListBuilder.createList(aCondition);
		BitSet aSelection = aLargeTable.evaluate(aConditionList);
		assertEquals(671, aSelection.cardinality());

		//set target concept (large table)
		Column aTarget = aLargeTable.getColumns().get(0); //get target (age)
		TargetConcept aTCLarge = new TargetConcept();
		aTCLarge.setTargetType(TargetType.SINGLE_NUMERIC);
		aTCLarge.setPrimaryTarget(aTarget);

		//set target concept (small table)
		aTarget = aSmallTable.getColumns().get(0); //get target (age)
		TargetConcept aTCSmall = new TargetConcept();
		aTCSmall.setTargetType(TargetType.SINGLE_NUMERIC);
		aTCSmall.setPrimaryTarget(aTarget);

		//set search parameters (large table)
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTCLarge);
		anSP.setQualityMeasure(QM.Z_SCORE);
		anSP.setQualityMeasureMinimum(1f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy(SearchStrategy.BEAM);
		anSP.setNominalSets(false);
		anSP.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSP.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);

		//set search parameters (small table)
		SearchParameters anSPSmall = new SearchParameters();
		anSPSmall.setTargetConcept(aTCSmall);
		anSPSmall.setQualityMeasure(QM.Z_SCORE);
		anSPSmall.setQualityMeasureMinimum(1f);
		anSPSmall.setSearchDepth(1);
		anSPSmall.setMinimumCoverage(2);
		anSPSmall.setMaximumCoverageFraction(1f);
		anSPSmall.setMaximumSubgroups(1000);
		anSPSmall.setMaximumTime(1000); //1000 seconds
		anSPSmall.setSearchStrategy(SearchStrategy.BEAM);
		anSPSmall.setNominalSets(false);
		anSPSmall.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSPSmall.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSPSmall.setSearchStrategyWidth(10);
		anSPSmall.setNrBins(8);
		anSPSmall.setNrThreads(1);

		//SD run on entire table with selection (sex = Male) specified
		SubgroupDiscovery anSDLarge = Process.runSubgroupDiscovery(aLargeTable, 0, aSelection, anSP, false, 1, null);
		SubgroupSet aResultLarge = anSDLarge.getResult();
		Iterator<Subgroup> anIteratorLarge = aResultLarge.iterator();

		//for comparison, SD run on separate table with only males
		SubgroupDiscovery anSDSmall = Process.runSubgroupDiscovery(aSmallTable, 0, null, anSPSmall, false, 1, null);
		SubgroupSet aResultSmall = anSDSmall.getResult();
		Iterator<Subgroup> anIteratorSmall = aResultSmall.iterator();

		assertEquals(anSDLarge.getNumberOfSubgroups(), anSDSmall.getNumberOfSubgroups());
		while (anIteratorLarge.hasNext() && anIteratorSmall.hasNext())
		{
			Subgroup aSubgroupA = anIteratorLarge.next();
			Subgroup aSubgroupB = anIteratorSmall.next();
			assertEquals(aSubgroupA.toString(), aSubgroupB.toString());
			assertEquals(aSubgroupB.getCoverage() , aSubgroupB.getCoverage());
			assertEquals(roundToFive(aSubgroupA.getMeasureValue()), roundToFive(aSubgroupB.getMeasureValue()));
			assertEquals(roundToFive(aSubgroupA.getSecondaryStatistic()), roundToFive(aSubgroupB.getSecondaryStatistic()));
			assertEquals(aSubgroupA.getTertiaryStatistic(), aSubgroupB.getTertiaryStatistic());
		}
	}

	@Test
	@DisplayName("Check end-to-end run on Adult.txt using SINGLE_NUMERIC, EXPLAINED_VARIANCE. Subset selection on Sex = Male")
	public void testAdult7()
	{
		//SINGLE_NUMERIC
		//d=1
		//numeric strategy = best

		System.out.println("\n\n----------- testAdult7---------");
		//loading two tables for comparison of results
		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
		Table aLargeTable = aLoader.getTable();
		aLoader = new DataLoaderTXT(new File("src/test/resources/adult_male.txt"));
		Table aSmallTable = aLoader.getTable();

		//Subset selection
		Column aSex = aLargeTable.getColumns().get(9); //get "sex" attribute
		Operator anOperator = Operator.EQUALS;
		ConditionBase aCB = new ConditionBase(aSex, anOperator);
		Condition aCondition = new Condition(aCB, "Male");
		ConditionList aConditionList = ConditionListBuilder.createList(aCondition);
		BitSet aSelection = aLargeTable.evaluate(aConditionList);
		assertEquals(671, aSelection.cardinality());

		//set target concept (large table)
		Column aTarget = aLargeTable.getColumns().get(0); //get target (age)
		TargetConcept aTCLarge = new TargetConcept();
		aTCLarge.setTargetType(TargetType.SINGLE_NUMERIC);
		aTCLarge.setPrimaryTarget(aTarget);

		//set target concept (small table)
		aTarget = aSmallTable.getColumns().get(0); //get target (age)
		TargetConcept aTCSmall = new TargetConcept();
		aTCSmall.setTargetType(TargetType.SINGLE_NUMERIC);
		aTCSmall.setPrimaryTarget(aTarget);

		//set search parameters (large table)
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTCLarge);
		anSP.setQualityMeasure(QM.EXPLAINED_VARIANCE);
		anSP.setQualityMeasureMinimum(0f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy(SearchStrategy.BEAM);
		anSP.setNominalSets(false);
		anSP.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSP.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);

		//set search parameters (small table)
		SearchParameters anSPSmall = new SearchParameters();
		anSPSmall.setTargetConcept(aTCSmall);
		anSPSmall.setQualityMeasure(QM.EXPLAINED_VARIANCE);
		anSPSmall.setQualityMeasureMinimum(0f);
		anSPSmall.setSearchDepth(1);
		anSPSmall.setMinimumCoverage(2);
		anSPSmall.setMaximumCoverageFraction(1f);
		anSPSmall.setMaximumSubgroups(1000);
		anSPSmall.setMaximumTime(1000); //1000 seconds
		anSPSmall.setSearchStrategy(SearchStrategy.BEAM);
		anSPSmall.setNominalSets(false);
		anSPSmall.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSPSmall.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSPSmall.setSearchStrategyWidth(10);
		anSPSmall.setNrBins(8);
		anSPSmall.setNrThreads(1);

		//SD run on entire table with selection (sex = Male) specified
		SubgroupDiscovery anSDLarge = Process.runSubgroupDiscovery(aLargeTable, 0, aSelection, anSP, false, 1, null);
		SubgroupSet aResultLarge = anSDLarge.getResult();
		Iterator<Subgroup> anIteratorLarge = aResultLarge.iterator();

		//for comparison, SD run on separate table with only males
		SubgroupDiscovery anSDSmall = Process.runSubgroupDiscovery(aSmallTable, 0, null, anSPSmall, false, 1, null);
		SubgroupSet aResultSmall = anSDSmall.getResult();
		Iterator<Subgroup> anIteratorSmall = aResultSmall.iterator();

		assertEquals(anSDLarge.getNumberOfSubgroups(), anSDSmall.getNumberOfSubgroups());
		while (anIteratorLarge.hasNext() && anIteratorSmall.hasNext())
		{
			Subgroup aSubgroupA = anIteratorLarge.next();
			Subgroup aSubgroupB = anIteratorSmall.next();
			assertEquals(aSubgroupA.toString(), aSubgroupB.toString());
			assertEquals(aSubgroupB.getCoverage() , aSubgroupB.getCoverage());
			assertEquals(roundToFive(aSubgroupA.getMeasureValue()), roundToFive(aSubgroupB.getMeasureValue()));
			assertEquals(roundToFive(aSubgroupA.getSecondaryStatistic()), roundToFive(aSubgroupB.getSecondaryStatistic()));
			assertEquals(aSubgroupA.getTertiaryStatistic(), aSubgroupB.getTertiaryStatistic());
		}
	}

	@Test
	@DisplayName("Check end-to-end run on Adult.txt using SINGLE_NOMINAL. Subset selection on Sex = Male")
	public void testAdult8()
	{
		//SINGLE_NOMINAL
		//d=1
		//numeric strategy = best

		System.out.println("\n\n----------- testAdult8 ---------");
		//loading two tables for comparison of results
		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
		Table aTable = aLoader.getTable();


//%%%%% This block of text is the functionality that should be added to the R-wrapper
		//Subset selection
		Column aSex = aTable.getColumns().get(9); //get "sex" attribute
		Operator anOperator = Operator.EQUALS;
		ConditionBase aCB = new ConditionBase(aSex, anOperator);
		Condition aCondition = new Condition(aCB, "Male");
		ConditionList aConditionList = ConditionListBuilder.createList(aCondition);
		BitSet aSelection = aTable.evaluate(aConditionList);
		assertEquals(671, aSelection.cardinality());
//%%%%% There is one more line below that also needs attention

		//set target concept
		Column aTarget = aTable.getColumns().get(14); //get target
		TargetConcept aTC = new TargetConcept();
		aTC.setTargetType(TargetType.SINGLE_NOMINAL);
		aTC.setPrimaryTarget(aTarget);
		aTC.setTargetValue("gr50K");

		//set search parameters
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTC);
		anSP.setQualityMeasure(QM.CORTANA_QUALITY);
		anSP.setQualityMeasureMinimum(0.1f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy(SearchStrategy.BEAM);
		anSP.setNominalSets(false);
		anSP.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSP.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);

		//SD run on entire table with selection (sex = Male) specified
//%%%%% note that aSelection is added iso null
		SubgroupDiscovery anSDLarge = Process.runSubgroupDiscovery(aTable, 0, aSelection, anSP, false, 1, null);
	}

	@Test
	@DisplayName("Check end-to-end run on Adult.txt using SINGLE_NOMINAL. Subset selection on Sex = Male")
	public void testAdult9()
	{
		//SINGLE_NOMINAL
		//d=1
		//numeric strategy = best

		System.out.println("\n\n----------- testAdult9 ---------");
		//loading two tables for comparison of results
		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
		Table aTable = aLoader.getTable();


//%%%%% This block of text is the functionality that should be added to the R-wrapper
		//Subset selection
		Column anEducationNum = aTable.getColumns().get(4); //get "education-num" attribute
		Operator anOperator = Operator.GREATER_THAN_OR_EQUAL;
		ConditionBase aCB = new ConditionBase(anEducationNum, anOperator);
		float aValue = 10f;
		anEducationNum.buildSorted(new BitSet()); //provide empty BitSet as target. Irrelevant at the moment
		int i = anEducationNum.getSortedIndex(aValue); //look up sort index
		System.out.println("EducationNum: " + aValue + ", index: " + i);
		Condition aCondition = new Condition(aCB, aValue, i);
		ConditionList aConditionList = ConditionListBuilder.createList(aCondition);
		BitSet aSelection = aTable.evaluate(aConditionList);
		assertEquals(552, aSelection.cardinality());
//%%%%% There is one more line below that also needs attention

		//set target concept
		Column aTarget = aTable.getColumns().get(14); //get target
		TargetConcept aTC = new TargetConcept();
		aTC.setTargetType(TargetType.SINGLE_NOMINAL);
		aTC.setPrimaryTarget(aTarget);
		aTC.setTargetValue("gr50K");

		//set search parameters
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTC);
		anSP.setQualityMeasure(QM.CORTANA_QUALITY);
		anSP.setQualityMeasureMinimum(0.1f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy(SearchStrategy.BEAM);
		anSP.setNominalSets(false);
		anSP.setNumericOperators(NumericOperatorSetting.NORMAL);
		anSP.setNumericStrategy(NumericStrategy.NUMERIC_BEST);
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);

		//SD run on entire table with selection (sex = Male) specified
//%%%%% note that aSelection is added iso null
		SubgroupDiscovery anSDLarge = Process.runSubgroupDiscovery(aTable, 0, aSelection, anSP, false, 1, null);
	}

	private float roundToFive(double f) { return (float) Math.round(f*100000)/100000; }
}
