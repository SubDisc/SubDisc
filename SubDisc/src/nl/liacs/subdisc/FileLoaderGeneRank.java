/*
 * TODO this class could internalise (a copy of) the used geneRankFile for
 * showing which genes are mapped to which/a cui.
 * TODO this class could hold the geneRankFile and allow choosing another domain
 * to use for mining.
 */
package nl.liacs.subdisc;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import nl.liacs.subdisc.Attribute.*;
import nl.liacs.subdisc.cui.*;
import nl.liacs.subdisc.gui.*;

public class FileLoaderGeneRank implements FileLoaderInterface
{
	// default size of ranked genes
	private static final int DEFAULT_SIZE = 10000;
	private static final Map<String, Integer> itsCui2LineNrMap;

	private final Map<String, String[]> itsGeneRank;
	private final Map<String, String> itsGene2CuiMap = Gene2CuiMap.ENTREZ2CUI.getMap(); // only ENTREZ for now
	private final File itsDomainFile;
	private Table itsTable;

	static
	{
		File aFile = new File(CuiMapInterface.EXPRESSION_CUIS);

		if (!aFile.exists())
		{
			itsCui2LineNrMap = null;
			ErrorLog.log(aFile, new FileNotFoundException());
		}
		else
			itsCui2LineNrMap = new Cui2LineNrMap(aFile).getMap();
	}

/*
	private static final String SEPARATOR = " ";

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
			itsDomainFile = null;
			ErrorLog.log(theFile, new FileNotFoundException());
			return;
		}
		else
		{
			itsGeneRank = new LinkedHashMap<String, String[]>(DEFAULT_SIZE);

			if (itsCui2LineNrMap == null)
			{
				itsDomainFile = null;
				Log.logCommandLine("File: 'expression_cuis.txt' not found.");
				return;
			}
			else
			{
				CountDownLatch aDoneSignal = new CountDownLatch(1);
				CuiDomainChooser aChooser = new CuiDomainChooser(aDoneSignal);

				try
				{
					aDoneSignal.await();
				}
				catch (InterruptedException e)
				{
					//TODO ErrorLog
					Log.logCommandLine(
						"FileLoaderGeneRank: CuiDomainChooser Waiting Error.");
				}

				itsDomainFile = aChooser.getFile();

				if (itsDomainFile == null || !itsDomainFile.exists())
				{
					ErrorLog.log(itsDomainFile, new FileNotFoundException());
					return;
				}
				else
					parseFile(theFile);
			}
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
			// TODO print number of found expression-cui mappings?

			// all relevant CUI lineNrs known
			getCuiCorrelationInfo(aLineNrs);
		}
		catch (FileNotFoundException e)
		{
			ErrorLog.log(theFile, e);
			return;
		}
		catch (IOException e)
		{
			ErrorLog.log(theFile, e);
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
				ErrorLog.log(theFile, e);
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
			aReader = new BufferedReader(new FileReader(itsDomainFile));
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
			ErrorLog.log(itsDomainFile, e);
			return;
		}
		catch (IOException e)
		{
			ErrorLog.log(itsDomainFile, e);
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
				ErrorLog.log(itsDomainFile, e);
			}
		}
	}

	private void createTable(String theHeaderLine, int theNrRows)
	{
		String[] aHeaders = theHeaderLine.split(",", -1);
		int aNrColumns = aHeaders.length;
		itsTable =  new Table(new File("TMP_NAME"), theNrRows, aNrColumns);	// TODO use itsFile

		itsTable.getColumns()
				.add(new Column(new Attribute("CUI",
												"CUI",
												AttributeType.NOMINAL,
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

	private void populateTable()
	{
		int aNrColumns = itsTable.getNrColumns();
		for (String[] aData : itsGeneRank.values())
		{
			// first is CUI, aData[0] should always exist
			itsTable.getColumn(0).add(aData[0]);
			for (int i = 1; i < aNrColumns; i++)
				itsTable.getColumn(i).add(Float.parseFloat(aData[i]));
		}
	}

	@Override
	public Table getTable()
	{
		// TODO for testing
		itsTable.update();
		return itsTable;
	}
}
