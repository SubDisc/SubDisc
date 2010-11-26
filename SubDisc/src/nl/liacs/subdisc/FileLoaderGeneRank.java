/*
 * TODO this class could internalise (a copy of) the used geneRankFile for
 * showing which genes are mapped to which/a cui.
 * TODO this class could hold the geneRankFile and allow choosing another domain
 * to use for mining.
 * NOTE: this class may need a lot of memory. Use the virtual machine command
 * line parameter '-Xms1600m' and/or '-Xmx1600m' (or another suitable amount of
 * memory).
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
	// default size of gene rank file
	private static final int DEFAULT_SIZE = 10000;
	private static final Map<String, Integer> itsCui2LineNrMap;

	private final Map<String, float[]> itsGeneRank;
	private final Map<String, String> itsGene2CuiMap = Gene2CuiMap.ENTREZ2CUI.getMap(); // only ENTREZ for now
	private final File itsDomainFile;
	private Table itsTable;

	static
	{
		File aFile = new File(CuiMapInterface.GENE_IDENTIFIER_CUIS);

		if (!aFile.exists())
		{
			itsCui2LineNrMap = null;
			ErrorLog.log(aFile, new FileNotFoundException());
		}
		else
			itsCui2LineNrMap = new Cui2LineNrMap(aFile).getMap();
	}

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
			itsGeneRank = new LinkedHashMap<String, float[]>(DEFAULT_SIZE);

			if (itsCui2LineNrMap == null)
			{
				itsDomainFile = null;
				Log.logCommandLine(
					String.format("File: '%s' not found.",
									CuiMapInterface.GENE_IDENTIFIER_CUIS));
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
				// TODO pass as parameter
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
	 * aLineNrs is used to read the CUI-Domain file effectively, that is, from
	 * begin to end in one pass.
	 */
	private void parseFile(File theFile)
	{
		BufferedReader aReader = null;
		Set<Integer> aLineNrs = new TreeSet<Integer>();

		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aLine = aReader.readLine();	// skip headerLine
			String aGene;

			while ((aLine = aReader.readLine()) != null)
			{
				/*
				 * TODO auto check 'RANK', 'gene_identifier' and 'SCORE' column
				 * based on headerLine signal words like 'ENTREZ' and 'GO'.
				 */
				aGene = aLine.split(",")[0];
				if (itsGene2CuiMap.containsKey(aGene))
				{
					// for now only add CUIs, not entrez/go/ensemble
					itsGeneRank.put(itsGene2CuiMap.get(aGene), null);
					aLineNrs.add(itsCui2LineNrMap.get(itsGene2CuiMap.get(aGene)));
				}
			}
			// TODO print number of found expression-cui mappings?

			// all relevant CUI lineNrs known
			getCuiCorrelationInfo(theFile, aLineNrs);
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
	 * theLineNrSet is sorted. All lineNumbers are looked up in the CUI-Domain
	 * file in order, and the information is added to the relevant gene in
	 * itsGeneRank.
	 */
	private void getCuiCorrelationInfo(File theFile, Set<Integer> theLineNrSet)
	{
		BufferedReader aReader = null;

		try
		{
			aReader = new BufferedReader(new FileReader(itsDomainFile));
			Iterator<Integer> anIterator = theLineNrSet.iterator();

			// headerLine is used for ColumnNames
			createTable(theFile, theLineNrSet.size(), aReader.readLine());

			int aCurrentLine = 0;
			int aNeededLine;
			int aNrColumns = itsTable.getNrColumns();
			// NOTE: (aLine = aReader.readLine()) != null) should not happen
			while (anIterator.hasNext())
			{
				aNeededLine = anIterator.next().intValue();
				while (++aCurrentLine < aNeededLine)
					aReader.readLine();
				/*
				 * For long lines aLine.split() is much faster then Scanner().
				 * To reduce memory usage values are directly stored as float.
				 * Does not test for NumberFormatException, assumes well-formed
				 * CUI-Domain files.
				 */
				String[] aLineArray = aReader.readLine().split(",", -1);
				float[] anotherLineArray = new float[aNrColumns];
				for (int i = 0; i < aNrColumns; i++)
					anotherLineArray[i] = Float.parseFloat(aLineArray[i]);
				// this maintains order of geneRanking
				itsGeneRank.put(aLineArray[0], anotherLineArray);
			}
			// all data available
			populateTable();
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

	private void createTable(File theFile, int theNrRows, String theHeaderLine)
	{
		String[] aHeaders = theHeaderLine.split(",", -1);
		int aNrColumns = aHeaders.length;
		itsTable =  new Table(theFile, theNrRows, aNrColumns);

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
		for (float[] aData : itsGeneRank.values())
		{
			// first is CUI, aData[0] should always exist
			itsTable.getColumn(0).add(String.valueOf((int) aData[0]));
			for (int i = 1; i < aNrColumns; i++)
				itsTable.getColumn(i).add(aData[i]);
		}
	}

	@Override
	public Table getTable()
	{
		return itsTable;
	}
}
