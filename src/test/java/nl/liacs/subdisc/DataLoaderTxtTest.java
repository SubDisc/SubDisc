package nl.liacs.subdisc;

import java.util.*;

// Testing lib
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Java lib
import java.io.*;


public class DataLoaderTxtTest 
{
	@ParameterizedTest
	@ValueSource(strings = {
		"src/test/resources/adult.txt", 
		"src/test/resources/dataloader no missing.txt",
		"src/test/resources/long10k.txt",
//		"src/test/resources/long1m.txt",		//quite a large dataset. Primarily meant for scalability testing
		"src/test/resources/commas in fields.txt",
		"src/test/resources/discretisation.txt",
		"src/test/resources/long100k.txt",
		"src/test/resources/long10.txt",
		"src/test/resources/long with unique nums.txt",
		"src/test/resources/missing values.txt"
	})
	public void validtxt(String filename) 
	{
		DataLoaderTXT dltxt = new DataLoaderTXT(new File(filename));
		Table table = dltxt.getTable();

		assertNotNull(table);
	}

	@Test
	@DisplayName("Check dimensions of long100k.txt")
	public void sizeof100k()
	{
		DataLoaderTXT dltxt = new DataLoaderTXT(new File("src/test/resources/long100k.txt"));
		Table table = dltxt.getTable();

		assertEquals(table.getNrRows(), 100000);
		assertEquals(table.getNrColumns(), 19);
		ArrayList<Column> aColumns = table.getColumns();
		assertEquals(aColumns.get(0).getType(), AttributeType.BINARY);	//A
		assertEquals(aColumns.get(1).getType(), AttributeType.NOMINAL); //RINpersoons
		assertEquals(aColumns.get(2).getType(), AttributeType.NOMINAL);
		assertEquals(aColumns.get(3).getType(), AttributeType.NOMINAL);
		assertEquals(aColumns.get(4).getType(), AttributeType.NUMERIC);
		assertEquals(aColumns.get(5).getType(), AttributeType.NOMINAL);
		assertEquals(aColumns.get(6).getType(), AttributeType.NUMERIC); //GBAburgerlijkestaatnw
		assertEquals(aColumns.get(7).getType(), AttributeType.NUMERIC);
		assertEquals(aColumns.get(8).getType(), AttributeType.NOMINAL);
		assertEquals(aColumns.get(9).getType(), AttributeType.NOMINAL);
		assertEquals(aColumns.get(10).getType(), AttributeType.NUMERIC);
		assertEquals(aColumns.get(11).getType(), AttributeType.NUMERIC);
		assertEquals(aColumns.get(12).getType(), AttributeType.NOMINAL); //oplnivSOI2016agg1HBMETNIRWO
		assertEquals(aColumns.get(13).getType(), AttributeType.NOMINAL); 
		assertEquals(aColumns.get(14).getType(), AttributeType.NUMERIC);
		assertEquals(aColumns.get(15).getType(), AttributeType.NUMERIC);
		assertEquals(aColumns.get(16).getType(), AttributeType.NUMERIC);
		assertEquals(aColumns.get(17).getType(), AttributeType.NUMERIC);
		assertEquals(aColumns.get(18).getType(), AttributeType.NUMERIC); //#veroordelingen_zeden

		assertEquals(aColumns.get(0).getCardinality(), 1);	//A
		assertEquals(aColumns.get(1).getCardinality(), 2); //RINpersoons
		assertEquals(aColumns.get(2).getCardinality(), 2);
		assertEquals(aColumns.get(3).getCardinality(), 3);
		assertEquals(aColumns.get(4).getCardinality(), 96);
		assertEquals(aColumns.get(5).getCardinality(), 8);
		assertEquals(aColumns.get(6).getCardinality(), 9000); //GBAburgerlijkestaatnw
		assertEquals(aColumns.get(7).getCardinality(), 60268);
		assertEquals(aColumns.get(8).getCardinality(), 2);
		assertEquals(aColumns.get(9).getCardinality(), 6);
		assertEquals(aColumns.get(10).getCardinality(), 9);
		assertEquals(aColumns.get(11).getCardinality(), 8);
		assertEquals(aColumns.get(12).getCardinality(), 3); //oplnivSOI2016agg1HBMETNIRWO
		assertEquals(aColumns.get(13).getCardinality(), 5); 
		assertEquals(aColumns.get(14).getCardinality(), 8);
		assertEquals(aColumns.get(15).getCardinality(), 8);
		assertEquals(aColumns.get(16).getCardinality(), 15);
		assertEquals(aColumns.get(17).getCardinality(), 6);
		assertEquals(aColumns.get(18).getCardinality(), 3); //#veroordelingen_zeden
	}
}
