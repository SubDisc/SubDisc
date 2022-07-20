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


public class SpecialisationTest 
{
	@Test
	@DisplayName("Check specialisation 1")
	public void test1()
	{
		// (workclass = "Private") does not specialise (education = Bachelors)
		// (education = Bachelors) does not specialise (workclass = "Private")

		System.out.println("\n==========Testing specialisation 1 ==========");

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
        	Table aTable = aLoader.getTable();

		Column aColumnA = aTable.getColumns().get(1); //attribute 'workclass'
		Operator anOperator = Operator.EQUALS;
		ConditionBase aCB = new ConditionBase(aColumnA, anOperator);
		Condition aConditionA = new Condition(aCB, "Private");
		ConditionList aConditionListA = ConditionListBuilder.createList(aConditionA);
		assertEquals(1, aConditionListA.size());

		Column aColumnB = aTable.getColumns().get(3); //attribute 'education'
		aCB = new ConditionBase(aColumnB, anOperator);
		Condition aConditionB = new Condition(aCB, "Bachelors");
		ConditionList aConditionListB = ConditionListBuilder.createList(aConditionB);
		assertEquals(1, aConditionListB.size());

		assertEquals(false, aConditionListA.strictlySpecialises(aConditionListB));
		assertEquals(false, aConditionListB.strictlySpecialises(aConditionListA));
	}
	
	@Test
	@DisplayName("Check specialisation 2")
	public void test2()
	{
		// (age <= 20) specialises (age <= 30), not vice versa

		System.out.println("\n========== Testing specialisation 2 ==========");

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
        	Table aTable = aLoader.getTable();

		Column aColumnA = aTable.getColumns().get(0); //attribute 'age'
		Operator anOperator = Operator.LESS_THAN_OR_EQUAL;
		ConditionBase aCB = new ConditionBase(aColumnA, anOperator);
		Condition aConditionA = new Condition(aCB, 20f, 0);				//not sure what the index should be
		ConditionList aConditionListA = ConditionListBuilder.createList(aConditionA);
		assertEquals(1, aConditionListA.size());

		Column aColumnB = aTable.getColumns().get(0); //attribute 'age'
		aCB = new ConditionBase(aColumnB, anOperator);
		Condition aConditionB = new Condition(aCB, 30f, 0);				//not sure what the index should be
		ConditionList aConditionListB = ConditionListBuilder.createList(aConditionB);
		assertEquals(1, aConditionListB.size());

		assertEquals(true, aConditionListA.strictlySpecialises(aConditionListB));
		assertEquals(false, aConditionListB.strictlySpecialises(aConditionListA));
	}
	
	@Test
	@DisplayName("Check specialisation 3")
	public void test3()
	{
		// (age <= 20) does not specialise (education-num <= 30)

		System.out.println("\n========== Testing specialisation 3 ==========");

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
        	Table aTable = aLoader.getTable();

		Column aColumnA = aTable.getColumns().get(0); //attribute 'age'
		Operator anOperator = Operator.LESS_THAN_OR_EQUAL;
		ConditionBase aCB = new ConditionBase(aColumnA, anOperator);
		Condition aConditionA = new Condition(aCB, 20f, 0);				//not sure what the index should be
		ConditionList aConditionListA = ConditionListBuilder.createList(aConditionA);
		assertEquals(1, aConditionListA.size());

		Column aColumnB = aTable.getColumns().get(4); //attribute 'education-num'
		aCB = new ConditionBase(aColumnB, anOperator);
		Condition aConditionB = new Condition(aCB, 8f, 0);				//not sure what the index should be
		ConditionList aConditionListB = ConditionListBuilder.createList(aConditionB);
		assertEquals(1, aConditionListB.size());

		assertEquals(false, aConditionListA.strictlySpecialises(aConditionListB));
		assertEquals(false, aConditionListB.strictlySpecialises(aConditionListA));
	}

	@Test
	@DisplayName("Check specialisation 4")
	public void test4()
	{
		// (age >= 40) specialises (age >= 30), not vice versa

		System.out.println("\n========== Testing specialisation 4 ==========");

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
        	Table aTable = aLoader.getTable();

		Column aColumnA = aTable.getColumns().get(0); //attribute 'age'
		Operator anOperator = Operator.GREATER_THAN_OR_EQUAL;
		ConditionBase aCB = new ConditionBase(aColumnA, anOperator);
		Condition aConditionA = new Condition(aCB, 40f, 0);				//not sure what the index should be
		ConditionList aConditionListA = ConditionListBuilder.createList(aConditionA);
		assertEquals(1, aConditionListA.size());

		Column aColumnB = aTable.getColumns().get(0); //attribute 'age'
		aCB = new ConditionBase(aColumnB, anOperator);
		Condition aConditionB = new Condition(aCB, 30f, 0);				//not sure what the index should be
		ConditionList aConditionListB = ConditionListBuilder.createList(aConditionB);
		assertEquals(1, aConditionListB.size());

		assertEquals(true, aConditionListA.strictlySpecialises(aConditionListB));
		assertEquals(false, aConditionListB.strictlySpecialises(aConditionListA));
	}

	@Test
	@DisplayName("Check specialisation 5")
	public void test5()
	{
		// (age >= 40 ^ workclass = "Private") specialises (age >= 40), not vice versa
		// (workclass = "Private" ^ age >= 40) specialises (age >= 40), not vice versa

		System.out.println("\n========== Testing specialisation 5 ==========");

		DataLoaderTXT aLoader = new DataLoaderTXT(new File("src/test/resources/adult.txt"));
        	Table aTable = aLoader.getTable();

		Column aColumnA = aTable.getColumns().get(0); //attribute 'age'
		Operator anOperator = Operator.GREATER_THAN_OR_EQUAL;
		ConditionBase aCB = new ConditionBase(aColumnA, anOperator);
		Condition aConditionA = new Condition(aCB, 40f, 0);				//not sure what the index should be
		ConditionList aConditionList = ConditionListBuilder.createList(aConditionA);	//build a size-1 CL

		Column aColumnB = aTable.getColumns().get(1); //attribute 'workclass'
		anOperator = Operator.EQUALS;
		aCB = new ConditionBase(aColumnB, anOperator);
		Condition aConditionB = new Condition(aCB, "Private");
		ConditionList aConditionListX = ConditionListBuilder.createList(aConditionList, aConditionB); //build a size-2 from the size-1
		System.out.println("\nCL: " + aConditionListX);
		assertEquals(2, aConditionListX.size());

		Column aColumnC = aTable.getColumns().get(0); //attribute 'age'
		anOperator = Operator.GREATER_THAN_OR_EQUAL;
		aCB = new ConditionBase(aColumnC, anOperator);
		Condition aConditionC = new Condition(aCB, 40f, 0);				//not sure what the index should be
		ConditionList aConditionListY = ConditionListBuilder.createList(aConditionC);
		System.out.println("CL: " + aConditionListY);
		assertEquals(1, aConditionListY.size());

		assertEquals(true, aConditionListX.strictlySpecialises(aConditionListY));
		assertEquals(false, aConditionListY.strictlySpecialises(aConditionListX));

		//invert the first CL, same result
		aConditionList = ConditionListBuilder.createList(aConditionB);
		aConditionListX = ConditionListBuilder.createList(aConditionList, aConditionA);
		System.out.println("\nCL: " + aConditionListX);
		System.out.println("CL: " + aConditionListY);
		assertEquals(true, aConditionListX.strictlySpecialises(aConditionListY));
		assertEquals(false, aConditionListY.strictlySpecialises(aConditionListX));

		//change numeric threshold the wrong way
		aConditionA = new Condition(aCB, 30f, 0);
		aConditionList = ConditionListBuilder.createList(aConditionA);
		aConditionListX = ConditionListBuilder.createList(aConditionList, aConditionB);
		System.out.println("\nCL: " + aConditionListX);
		System.out.println("CL: " + aConditionListY);
		assertEquals(false, aConditionListX.strictlySpecialises(aConditionListY));
		assertEquals(false, aConditionListY.strictlySpecialises(aConditionListX));

		//change numeric threshold the right way
		aConditionA = new Condition(aCB, 50f, 0);
		aConditionList = ConditionListBuilder.createList(aConditionA);
		aConditionListX = ConditionListBuilder.createList(aConditionList, aConditionB);
		System.out.println("\nCL: " + aConditionListX);
		System.out.println("CL: " + aConditionListY);
		assertEquals(true, aConditionListX.strictlySpecialises(aConditionListY));
		assertEquals(false, aConditionListY.strictlySpecialises(aConditionListX));
	}
}
