package nl.liacs.subdisc; //Jeremie, is this correct?

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


public class SelectionTest 
{
	@Test
	@DisplayName("Check selection process on subset selection.txt")
	public void test()
	{
		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/subset selection.txt"));
        	Table aTable = aLoader.getTable();

		//sanity check
	        assertEquals(aTable.getNrRows(), 10);
        	assertEquals(aTable.getNrColumns(), 5);

		//set selection
		Column aTarget = aTable.getColumns().get(4); //get target

		Column aColumn = aTable.getColumns().get(1); //get selector 1
		Operator anOperator = Operator.EQUALS;
		ConditionBase aCB = new ConditionBase(aColumn, anOperator);
		Condition aCondition = new Condition(aCB, "A");
		ConditionList aConditionList = ConditionListBuilder.createList(aCondition);
		BitSet aSelection = aTable.evaluate(aConditionList);

		//create a subgroup on selection
		aColumn = aTable.getColumns().get(3); //get descriptor
		aCB = new ConditionBase(aColumn, anOperator);
		aCondition = new Condition(aCB, "a");
		aConditionList = ConditionListBuilder.createList(aCondition);
		BitSet aSubset = aTable.evaluate(aConditionList);

		TargetConcept aTC = new TargetConcept();
		aTC.setTargetType("single nominal");
		aTC.setPrimaryTarget(aTarget);
		aTC.setTargetValue("t");
		SearchParameters anSP = new SearchParameters();
		anSP.setTargetConcept(aTC);
		anSP.setQualityMeasure(QM.CORTANA_QUALITY);
		anSP.setQualityMeasureMinimum(0f);
		anSP.setSearchDepth(1);
		anSP.setMinimumCoverage(2);
		anSP.setMaximumCoverageFraction(1f);
		anSP.setMaximumSubgroups(2); //find only one subgroup
		anSP.setMaximumTime(1000); //1000 seconds
		anSP.setSearchStrategy("beam");
		anSP.setNominalSets(false);
		anSP.setNumericOperators("<html>&#8804;, &#8805;</html>");
		anSP.setNumericStrategy("best");
		anSP.setSearchStrategyWidth(10);
		anSP.setNrBins(8);
		anSP.setNrThreads(1);
		SubgroupDiscovery anSD = Process.runSubgroupDiscovery(aTable, 0, aSelection, anSP, false, 1, null);

		//actual tests
		assertEquals(aTarget.countValues("t", null), 5); //how many positives in dataset
		assertEquals(aSelection.cardinality(), 5); //selection of first 5 rows.
		assertEquals(aTarget.countValues("t", aSelection), 3); //how many positives in selection

		assertEquals(aSubset.cardinality(), 5); //size of subgroup on entire dataset

		assertEquals(anSD.getNumberOfSubgroups(), 2); //only one subgroup retained
		SubgroupSet aResult = anSD.getResult();
		Iterator<Subgroup> anIterator = aResult.iterator();

		//subgroup 1
		Subgroup aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "id <= 2.0");
		assertEquals(aSubgroup.getCoverage(), 2);
		assertEquals(aSubgroup.getMeasureValue(), 0.6666666865348816f);
		assertEquals(aSubgroup.getSecondaryStatistic(), 1.0f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 2.0f);

		//subgroup 2
		aSubgroup  = anIterator.next();
		assertEquals(aSubgroup.toString(), "selector 2 <= 2.0");
		assertEquals(aSubgroup.getCoverage(), 2);
		assertEquals(aSubgroup.getMeasureValue(), 0.6666666865348816f);
		assertEquals(aSubgroup.getMeasureValue(), 0.6666666865348816f);
		assertEquals(aSubgroup.getSecondaryStatistic(), 1.0f);
		assertEquals(aSubgroup.getTertiaryStatistic(), 2.0f);
	}
}
