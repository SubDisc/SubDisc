package nl.liacs.subdisc.cui;

import java.io.*;
import java.util.*;

import nl.liacs.subdisc.*;

public class Cui2LineNrMap
{
	// TODO check number of cui in each domain
	private static final int DEFAULT_SIZE = 50000;
	private final Map<String, Integer> itsCui2LineNrMap;

	public static void main(String[] args)
	{
//		long aBegin = System.currentTimeMillis();

//		Cui2LineNrMap aMap = new Cui2LineNrMap(new File("/home/marvin/SubDisc/CUI/entrez2cui.txt"));
		Map<String, Integer> aMap = new Cui2LineNrMap(new File("/home/marvin/SubDisc/CUI/test_expr2biological_process.txt")).getMap();

//		System.out.println((System.currentTimeMillis() - aBegin) / 1000 + " s.");

//		for (String s : aMap.keySet())
//			System.out.println(s + " " + aMap.get(s));
	}

	public Cui2LineNrMap(File theFile)
	{
		if (theFile == null || !theFile.exists())
		{
			itsCui2LineNrMap = null;
			ErrorLog.log(theFile, new FileNotFoundException(""));
			return;
		}
		else
		{
			itsCui2LineNrMap = new HashMap<String, Integer>(DEFAULT_SIZE);
			parseFile(theFile);
		}
	}

	private void parseFile(File theFile)
	{
		BufferedReader aReader = null;

		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aLine;
			int aLineNr = 0;

			while ((aLine = aReader.readLine()) != null)
			{
				// TODO compare with speed of Scanner();
//				anIndexMap.put(aLine.split(",")[0], ++aLineNr);
				itsCui2LineNrMap.put(aLine, ++aLineNr);
			}
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

	/**
	 * Returns the <code>Map<String, Integer></code> for this Cui2LineNrMap.
	 * 
	 * @return the <code>Map<String, Integer></code> for this Cui2LineNrMap, or
	 * <code>null</code> if there is none.
	 */
	public Map<String, Integer> getMap()
	{
		if (itsCui2LineNrMap == null)
			return null;
		else
			return Collections.unmodifiableMap(itsCui2LineNrMap);
	}
}
