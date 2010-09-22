package nl.liacs.subdisc;

import java.io.*;
import java.util.*;

import nl.liacs.subdisc.Attribute.*;
import nl.liacs.subdisc.FileHandler.*;
import nl.liacs.subdisc.cui.*;
import nl.liacs.subdisc.gui.*;

public class FileLoaderGeneRank
{
	private final static int DEFAULT_SIZE = 10000;
	private final Map<String, String[]> itsGeneRank;
	private static final Map<String, String> itsGene2CuiMap = Gene2CuiMap.ENTREZ2CUI.getMap(); // only ENTREZ for now
	private static Table itsTable;

	// TODO for testing only
	private static Map<String, Integer> itsCui2LineNrMap = new Cui2LineNrMap(new File("/home/marvin/SubDisc/CUI/test_expr2biological_process.txt")).getMap();
	private static final String SEPARATOR = " ";
/*
	public static void main(String[] args)
	{
		long aBegin = System.currentTimeMillis();

		new FileLoaderGeneRank(new File("/home/marvin/SubDisc/CUI/test_all_gene_rank.txt"));

		System.out.println((System.currentTimeMillis() - aBegin) / 1000 + " s.");

		for (String aGene : itsGeneRank.keySet())
		{
			System.out.print(aGene);
//			System.out.print(SEPARATOR + itsGeneRank.get(aGene)[0] + SEPARATOR + itsGeneRank.get(aGene)[1]);
			for (String aCorrelationValue : itsGeneRank.get(aGene))
				System.out.print(SEPARATOR + aCorrelationValue);
			System.out.println();
		}

		itsTable.update();
		new MiningWindow(itsTable);
	}
*/
	public FileLoaderGeneRank(File theFile)
	{
		if (theFile == null || !theFile.exists())
		{
			itsGeneRank = null;
			ErrorLog.log(theFile, new FileNotFoundException());
			return;
		}
		else
		{
			itsGeneRank = new LinkedHashMap<String, String[]>(DEFAULT_SIZE);

			itsCui2LineNrMap = new CuiDomainChooser().getMap();
			if (itsCui2LineNrMap == null)
				return;	// TODO
			else
				parseFile(theFile);
		}
	}

	/*
	 * aLineNrs is used to read the CUI-Domain file effectively, ie. from begin
	 * to end in one pass.
	 */
	private void parseFile(File theFile)
	{
		BufferedReader aReader = null;
		Set<Integer> aLineNrs = new TreeSet<Integer>();

		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aLine;
			String aGene;

			aReader.readLine();	// skip headerLine
			while ((aLine = aReader.readLine()) != null)
			{
				// TODO compare with speed of Scanner();
				aGene = aLine.split(",")[0];
				if (itsGene2CuiMap.containsKey(aGene))
				{
					itsGeneRank.put(itsGene2CuiMap.get(aGene), null);	// for now only add CUIs, not entrez/go/ensemble
					aLineNrs.add(itsCui2LineNrMap.get(itsGene2CuiMap.get(aGene)));
				}
			}

			// all CUI lineNrs known
			getCuiCorrelationInfo(aLineNrs);
		}
		catch (FileNotFoundException e)
		{
			Log.logCommandLine("File failure.");	// TODO
			return;
		}
		catch (IOException e)
		{
			Log.logCommandLine("Reader failure.");	// TODO
			return;
		}
		finally
		{
			try
			{
				if (aReader != null)
					aReader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
//				new ErrorDialog(e, ErrorDialog.fileReaderError); // TODO generic
			}
		}
	}

	/*
	 * theLineNrSet is sorted. All lineNumbers are looked-up in the CUI-Domain
	 * file in order, and the information is added to the relevant gene in
	 * itsGeneRank.
	 */
	private void getCuiCorrelationInfo(Set<Integer> theLineNrSet)
	{
		BufferedReader aReader = null;

		try
		{
			aReader = new BufferedReader(new FileReader("/home/marvin/SubDisc/CUI/expr2biological_process.txt"));
			Iterator<Integer> anIterator = theLineNrSet.iterator();

			// headerLine is used for ColumnNames
			createTable(aReader.readLine(), theLineNrSet.size());

			// TODO test auto(un)boxing performace hit
			Integer aCurrentLine = new Integer(0);
			Integer aNeededLine;
			while (anIterator.hasNext())// && (aLine = aReader.readLine()) != null) // should not happen
			{
				aNeededLine = anIterator.next();
				while (++aCurrentLine < aNeededLine)
					aReader.readLine();
				// TODO compare with speed of Scanner(); splitting on long lines is slow
				String[] aLineArray = aReader.readLine().split(",", -1);
				itsGeneRank.put(aLineArray[0], aLineArray);	// this maintains order of geneRanking
			}

			// all data available
			populateTable();
		}
		catch (FileNotFoundException e)
		{
			Log.logCommandLine("File failure.");	// TODO
			return;
		}
		catch (IOException e)
		{
			Log.logCommandLine("Reader failure.");	// TODO
			return;
		}
		finally
		{
			try
			{
				if (aReader != null)
					aReader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
//				new ErrorDialog(e, ErrorDialog.fileReaderError); // TODO generic
			}
		}
	}

	private void createTable(String theHeaderLine, int theNrRows)
	{
		String[] aHeaders = theHeaderLine.split(",", -1);
		int aNrColumns = aHeaders.length;
		itsTable =  new Table(new File("TMP_NAME"), theNrRows, aNrColumns);

		// uses NUMERIC as hack
		itsTable.getColumns()
				.add(new Column(new Attribute("CUI",
												"CUI",
												AttributeType.NUMERIC,
												0),
								theNrRows));

		for (int i = 1; i < aNrColumns; i++)
		{
			itsTable.getColumns()
					.add(new Column(new Attribute(aHeaders[i],
													"",
													AttributeType.NUMERIC,
													i),
									theNrRows));
		}
	}

	// TODO
	private void populateTable()
	{
		int aNrColumns = itsTable.getNrColumns();
		for (String[] aData : itsGeneRank.values())
			for (int i = 0; i < aNrColumns; i++)
				itsTable.getColumn(i).add(Float.parseFloat(aData[i]));
	}
}
