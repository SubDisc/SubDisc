package nl.liacs.subdisc.cui;

import java.io.*;
import java.util.*;

import nl.liacs.subdisc.*;

/**
 * This class is not part of the public API, as it relies on a correct memory
 * setting, and correct CUI files.
 * This class creates GENE_CUI files for the various CUI-domains. It does not do
 * any checking, as the structure of these files is known. No lineNumeber is
 * written to <code>File</code>, this keeps the files smaller, and during
 * loading the lineNumber for each GENE_CUI can be calculated, as each one is on
 * a new line. NOTE the 
 */
class DomainCuiCreator
{
	// may create setters for this one day
//	private final String itsSeparator= ",";
	private final String itsLineEnd = "\n";

	public static void main(String[] args)
	{
		long aBegin = System.currentTimeMillis();
		new DomainCuiCreator(new File("/home/marvin/SubDisc/CUI/expr2biological_process.txt"));
		System.out.println((System.currentTimeMillis() - aBegin) / 1000);
	}

	// TODO return boolean indicating success?
	DomainCuiCreator(File theFile)
	{
		if (theFile == null || !theFile.exists())
		{
			Log.logCommandLine("File does not exist.");	// TODO
			return;
		}
		else
		{
			parseFile(theFile);
		}
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

		writeGeneCuiFile(theFile, aList);
	}

	private void writeGeneCuiFile(File theFile, List<String> theGeneCuiList)
	{
		BufferedWriter aWriter = null;

		try
		{
			aWriter = new BufferedWriter(new FileWriter(theFile.getAbsolutePath() + ".csv"));

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
			Log.logCommandLine("Writer failure.");	// TODO
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
				e.printStackTrace();
//				new ErrorDialog(e, ErrorDialog.fileWriterError); // TODO generic
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
