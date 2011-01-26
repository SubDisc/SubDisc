/*
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
	private static final Map<String, Integer> itsCui2LineNrMap;

	private Map<String, String> itsGene2CuiMap;
	private Table itsTable;
	private int identifierColumn = -1;
	private boolean hasRankColumn = false;

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

	public FileLoaderGeneRank(Table theTable)
	{
		if (theTable == null)
		{
			Log.logCommandLine(
			"FileLoaderGeneRank Constructor: parameter can not be 'null'.");
			return;
		}
		else
		{
			itsTable = theTable;

			if (itsCui2LineNrMap == null)
			{
//				itsDomainFile = null;
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
				// TODO pass itsDomainFile as parameter
				File itsDomainFile = aChooser.getFile();

				if (itsDomainFile == null || !itsDomainFile.exists())
				{
					ErrorLog.log(itsDomainFile, new FileNotFoundException());
					return;
				}
				else
				{
					// TODO remove debug only
					System.gc();
					long freePre = Runtime.getRuntime().freeMemory();
					long start = System.currentTimeMillis();
					foo(itsDomainFile);
					System.gc();
					System.out.println((freePre - Runtime.getRuntime().freeMemory())/1048576 + "MB used after adding domain info.");
					System.out.println((System.currentTimeMillis() - start)/1000 + "s. for loading of domain file.");
					
				}
			}
		}
	}

	private void foo(File theCUIFile)
	{
		BufferedReader aReader = null;

		try
		{
			aReader = new BufferedReader(new FileReader(theCUIFile));

			ArrayList<Column> aColumns = itsTable.getColumns();
			int aNrDataColumns = aColumns.size();	// safer than itsTable.getNrColumns()
			//int aNrDataColumns = itsTable.getNrColumns(); // relies on correct itsNrColumn/update
			int aNrRows = itsTable.getNrRows();
			String[] aCUIHeaderArray = aReader.readLine().split(",", -1);	// TODO fails if file is empty
			int aNrCUIColumns = aCUIHeaderArray.length;
			String aString = null;

			// get essential information about the existing Table
			for (int i = 0; i < aNrDataColumns; i++)
			{
				aString = aColumns.get(i).getName();

				if (aString.equalsIgnoreCase("rank"))
					hasRankColumn = true;
				else if (aString.equalsIgnoreCase("entrez"))
				{
					itsGene2CuiMap = Gene2CuiMap.ENTREZ2CUI.getMap();
					identifierColumn = i;
				}
				else if (aString.equalsIgnoreCase("go"))
				{
					itsGene2CuiMap = Gene2CuiMap.GO2CUI.getMap();
					identifierColumn = i;
				}

				if (hasRankColumn && identifierColumn != -1)
					break;
			}

			if (identifierColumn == -1)
				return;	// TODO show dialog to determine identifierColumn

			aColumns.ensureCapacity(aNrDataColumns + aNrCUIColumns+ (hasRankColumn ? 0 : 1));

			if (!hasRankColumn)
			{
				Column aRankColumn = new Column(new Attribute("RANK",
																"RANK",
																AttributeType.NUMERIC,
																aNrDataColumns++),
																aNrRows);

				for (float f = 1.0f, nrRows = (float)aNrRows; f <= nrRows; ++f)
					aRankColumn.add(f);	// relatively expensive, see comment XXX

				aColumns.add(aRankColumn);
			}
			// CUI is treated as NUMERIC (smaller size)
			// no cui2Name(aCUIHeaderArray[i]) yet
			for (int i = 0; i < aNrCUIColumns; i++)
			{
				aColumns.add(new Column(new Attribute(aCUIHeaderArray[i],
														"",
														AttributeType.NUMERIC,
														aNrDataColumns++),
										aNrRows));
			}

			// for each identifier in itsTable determine Domain file lineNr
			// itsCui2LineNrMap<K, V> uses String as Key
			/*
			 * aLineNrMap is used to read the CUI-Domain file effectively, that
			 * is, from begin to end in one pass.
			 */
			/*
			 * theLineNrSet is sorted. All lineNumbers are looked up in the CUI-Domain
			 * file in order, and the information is added to the relevant gene in
			 * itsGeneRank.
			 */
			Map<Integer, Integer> aLineNrMap = new TreeMap<Integer, Integer>();
			Column idColumn = aColumns.get(identifierColumn);
			for (int i = 0; i < aNrRows; i++)
			{
				// always set all association scores to 0.0f, update CUIs later
				// TODO XXX new constructor/method using name(ArrayList<Float>().nCopies(anrRows, 0.0f))
				for (int j = aNrCUIColumns, k = itsTable.getColumns().size(); j > 0; --j)
					itsTable.getColumn(--k).add(0.0f);

				// if CUI exists, put it in Map<cuiLineNr, identifierRowNr>
				String anIdentifier = String.valueOf(Float.valueOf(idColumn.getString(i)).intValue()); // TODO removes '.0' :)
				Integer aLineNr = itsCui2LineNrMap.get(itsGene2CuiMap.get(anIdentifier));
				if (aLineNr != null)
					aLineNrMap.put(aLineNr, i);
			}

			// populate the new CUI columns for identifiers mapped to CUIs
			int aCurrentLineNr = 1;	// headerLine read already
			int aNeededLineNr = 0;	// could be combined into 1 int
			for (Map.Entry<Integer, Integer> anEntry: aLineNrMap.entrySet())
			{
				aNeededLineNr = anEntry.getKey().intValue();
				// no EOF check, lineNrs should always be in file
				while (aCurrentLineNr++ < aNeededLineNr)
					aReader.readLine();

				/*
				 * For long lines aLine.split() is much faster then Scanner().
				 * To reduce memory usage values are directly stored as Float.
				 * Does not test for NumberFormatException, assumes well-formed
				 * CUI-Domain files.
				 */
				String[] anArray = aReader.readLine().split(",", -1);
				for (int j = aNrCUIColumns - 1, k = itsTable.getColumns().size(); j >= 0; --j)
					itsTable.getColumn(--k).set(anEntry.getValue(), Float.valueOf(anArray[j]));
			}

		}
		// TODO
		catch (FileNotFoundException e) {}
		catch (IOException e) {}
	}

	@Override
	public Table getTable()
	{
		return itsTable;
	}
}
