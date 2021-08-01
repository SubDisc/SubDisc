package nl.liacs.subdisc;

// Java lib
import java.io.*;
import java.util.*;
//import java.util.Map.Entry;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.ConditionListBuilder.ConditionListA;

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
	@DisplayName("Check end-to-end run on Adult.txt")
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

		//set selection
		Column aTarget = aTable.getColumns().get(14); //get target
		TargetConcept aTC = new TargetConcept();
		aTC.setTargetType("single nominal");
		aTC.setPrimaryTarget(aTarget);
		aTC.setTargetValue("gr50K");

		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTC);
		anSP.setQualityMeasure(QM.CORTANA_QUALITY);
		anSP.setQualityMeasureMinimum(0.1f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(1000);
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy("beam");
		anSP.setNominalSets(false);
		anSP.setNumericOperators("<html>&#8804;, &#8805;</html>");
		anSP.setNumericStrategy("best");
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);
		SubgroupDiscovery anSD = Process.runSubgroupDiscovery(aTable, 0, null, anSP, true, 1, null);

		//actual tests
		assertEquals(aTarget.countValues("gr50K", null), 232); //how many positives in dataset

		assertEquals(anSD.getNumberOfSubgroups(), 10);
		SubgroupSet aResult = anSD.getResult();
		Iterator<Subgroup> anIterator = aResult.iterator();

		//subgroup 1
		Subgroup aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "marital-status = 'Married-civ-spouse'");
		assertEquals(aSubgroup.getCoverage(), 443);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0.51760f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 0.44018f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 195.0f);

		//subgroup 2
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "relationship = 'Husband'");
		assertEquals(aSubgroup.getCoverage(), 376);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0,45330f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 0,44680f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 168.0f);

		//subgroup 3
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "education-num >= 11.0");
		assertEquals(aSubgroup.getCoverage(), 327);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0,35995f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 0,42813f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 140.0f);

		//subgroup 4
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "age >= 33.0");
		assertEquals(aSubgroup.getCoverage(), 616);

		//subgroup 5
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "hours-per-week >= 43.0");
		assertEquals(aSubgroup.getCoverage(), 268);

		//subgroup 6
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "occupation = 'Exec-managerial'");
		assertEquals(aSubgroup.getCoverage(), 124);

		//subgroup 7
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "sex = 'Male'");
		assertEquals(aSubgroup.getCoverage(), 671);

		//subgroup 8
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "education = 'Bachelors'");
		assertEquals(aSubgroup.getCoverage(), 166);

		//subgroup 9
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "capital-gain >= 4386.0");
		assertEquals(aSubgroup.getCoverage(), 50);

		//subgroup 10
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "occupation = 'Prof-specialty'");
		assertEquals(aSubgroup.getCoverage(), 124);
		assertEquals(roundToFive(aSubgroup.getMeasureValue()), 0,12477f);
		assertEquals(roundToFive(aSubgroup.getSecondaryStatistic()), 0,41129f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 51.0f);
	}

	private float roundToFive(double f) { return (float) Math.round(f*100000)/100000; }
}