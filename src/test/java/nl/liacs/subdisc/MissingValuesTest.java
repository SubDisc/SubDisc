package nl.liacs.subdisc;

// Java lib
import java.io.*;
import java.util.*;
//import java.util.Map.Entry;

import nl.liacs.subdisc.*;

// Testing lib
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class MissingValuesTest 
{
	@Test
	@DisplayName("Check current outcome on adult with missing.txt using SINGLE_NOMINAL")
	public void testAdult1()
	{
		//SINGLE_NOMINAL
		//d=1
		//numeric strategy = best

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult with missing.txt"));
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

		//disable most columns
		//don't disable 1: age
		aTable.getColumns().get(1).setIsEnabled(false);
		aTable.getColumns().get(2).setIsEnabled(false);
		aTable.getColumns().get(3).setIsEnabled(false);
		aTable.getColumns().get(4).setIsEnabled(false);
		//don't disable 5: marital-status
		aTable.getColumns().get(6).setIsEnabled(false);
		aTable.getColumns().get(7).setIsEnabled(false);
		aTable.getColumns().get(8).setIsEnabled(false);
		aTable.getColumns().get(9).setIsEnabled(false);
		aTable.getColumns().get(10).setIsEnabled(false);
		aTable.getColumns().get(11).setIsEnabled(false);
		aTable.getColumns().get(12).setIsEnabled(false);
		aTable.getColumns().get(13).setIsEnabled(false);
		aTable.getColumns().get(14).setIsEnabled(false);

		//set search parameters
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTC);
		anSP.setQualityMeasure(QM.CORTANA_QUALITY);
		anSP.setQualityMeasureMinimum(-1f);
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
		assertEquals(anSD.getNumberOfSubgroups(), 7);
		SubgroupSet aResult = anSD.getResult();
		Iterator<Subgroup> anIterator = aResult.iterator();

		//subgroup 1
		Subgroup aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "marital-status = 'Married-civ-spouse'");
		assertEquals(aSubgroup.getCoverage(), 444);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0.5163f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 0.43919f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 195.0f);

		//subgroup 2
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "age >= 37.0");
		assertEquals(aSubgroup.getCoverage(), 498);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0.33374f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 175.0f);
	}

	@Test
	@DisplayName("Check current outcome on adult with missing.txt using SINGLE_NUMERIC")
	public void testAdult2()
	{
		//SINGLE_NOMINAL
		//d=1
		//numeric strategy = best

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult with missing.txt"));
        	Table aTable = aLoader.getTable();

		//sanity check
	        assertEquals(aTable.getNrRows(), 1000);
        	assertEquals(aTable.getNrColumns(), 15);

		//set target concept
		Column aTarget = aTable.getColumns().get(14); //get target
		TargetConcept aTC = new TargetConcept();
		aTC.setTargetType(TargetType.SINGLE_NUMERIC);
		aTC.setPrimaryTarget(aTarget);

		assertEquals(45.70549f, roundToFive(aTable.getColumns().get(0).getAverage(null)));
	}

	private float roundToFive(double f) { return (float) Math.round(f*100000)/100000; }
}
