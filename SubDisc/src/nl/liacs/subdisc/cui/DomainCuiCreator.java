package nl.liacs.subdisc.cui;

import java.io.*;
import java.util.*;

import nl.liacs.subdisc.*;

/**
 * This class is not part of the public API, as it relies on a correct memory
 * setting, and correct CUI files.
 * This class creates an expression.cui file for the CUI-domains. It does not do
 * any checking, as the structure of these files is known. No lineNumber is
 * written to the resulting <code>File</code>, this keeps the <code>File</code>
 * smaller, and during loading the lineNumber for each EXPRESSION_CUI can be
 * calculated, as each one is on a new line. NOTE all domain files use the exact
 * same EXPRESSION_CUIs (the first Column), so only one expression.cui
 * <code>File</code> is needed.
 */
class DomainCuiCreator
{
	// may create setters for this one day
//	private final String itsSeparator= ",";
	private final String itsLineEnd = "\n";

/*
	// TODO for testing, all files are identical
	public static void main(String[] args)
	{
		for (File f : new File("/host/data/cmsb/cui/data/final/20100104").listFiles())
		{
			if (f.getName().startsWith("expr2"))
			{
				System.out.print(f.getName() + " ");
				long aBegin = System.currentTimeMillis();
				new DomainCuiCreator(f);
				System.out.println((System.currentTimeMillis() - aBegin) / 1000);
			}
		}
	}
*/
	// TODO return boolean indicating success?
	DomainCuiCreator(File theFile)
	{
		if (theFile == null || !theFile.exists())
		{
			ErrorLog.log(theFile, new FileNotFoundException());
			return;
		}
		else
			parseFile(theFile);
	}

	/*
	 * Reading and writing on same disk may be relatively slow, GENE_CUI file is
	 * small enough to completely keep in memory, and only write after whole
	 * file is read.
	 */
	private void parseFile(File theFile)
	{
		BufferedReader aReader = null;
//		Map<String, Integer> anIndexMap = new HashMap<String, Integer>(50000);
		List<String> aList = new ArrayList<String>(50000);

		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aLine = aReader.readLine(); // headerLine
//			int aLineNr = 0;

			while ((aLine = aReader.readLine()) != null)
			{
				// TODO compare with speed of Scanner();
//				anIndexMap.put(aLine.split(",")[0], ++aLineNr);
				aList.add(new String(aLine.split(",")[0]));
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

		writeGeneCuiFile(theFile, aList);
	}

	private void writeGeneCuiFile(File theFile, List<String> theGeneCuiList)
	{
		BufferedWriter aWriter = null;

		try
		{
			aWriter = new BufferedWriter(new FileWriter(theFile.getAbsolutePath() + ".cui"));

//			aWriter.write(itsSeparator);
//			aWriter.write((++aLineNr));
			for (String s : theGeneCuiList)
			{
				aWriter.write(s);
				aWriter.write(itsLineEnd);
			}
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
				if (aWriter != null)
				{
					aWriter.flush();
					aWriter.close();
				}
			}
			catch (IOException e)
			{
				ErrorLog.log(theFile, e);
			}
		}

	}

/*
	public enum Separator { COMMA, TAB, SEMICOLON, COLON }
	public enum LineEnd { UNIX, WINDOWS, MACINTOSH, }

	public void setNewSeparator(Separator theNewSeparator)
	{
		if (theNewSeparator != null)
			itsSeparator = theNewSeparator;
	}

	public void setNewLine(LineEnd theNewLineEnd)
	{
		if (theNewLineEnd != null)
			itsLineEnd = theNewLineEnd;
	}
*/
}
