package nl.liacs.subdisc;

// Testing lib
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// Java lib
import java.io.*;


public class DataLoaderTxtTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "src/test/resources/adult.txt", 
        "src/test/resources/dataloader no missing.txt",
        "src/test/resources/long10k.txt",
//        "src/test/resources/long1m.txt",		//quite a large dataset. Primarily meant for scalability testing
//        "src/test/resources/commas in fields.txt",
        "src/test/resources/discretisation.txt",
        "src/test/resources/long100k.txt",
        "src/test/resources/long10.txt",
        "src/test/resources/long with unique nums.txt"
    })
    public void validtxt(String filename) 
    {
        DataLoaderTXT dltxt = new DataLoaderTXT(new File(filename));
        Table table = dltxt.getTable();

        assertNotNull(table);

    }

}
