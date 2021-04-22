package nl.liacs.subdisc.postprocess;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.gui.*;

public class Parser
{
	// parameters related to ResultFile created by Cortana
	// could come from Cortana code - but this fine for now
	private static final int COLUMN_COUNT = ResultTableModel.COLUMN_COUNT;
	private static final String DELIMITER = XMLAutoRun.RESULT_SET_DELIMITER;
	private static final Pattern LINE_SPLITTER = Pattern.compile(DELIMITER);
	private static final int QUALITY_INDEX = 3; // hard-coded

	// in-file name conventions
	private static final int DATASET_INDEX = 0; // hard-coded
	private static final String EXT = ".txt";
	private static final FileFilter FILE_FILTER;
	static {
		FILE_FILTER = new FileFilter()
				{
					@Override
					public boolean accept(File pathname)
					{
						return pathname.getName().endsWith(EXT);
					}
				};
	}
	private static final String NAME_DELIMITER = "-";
	private static final Pattern NAME_SPLITTER = Pattern.compile(NAME_DELIMITER);
	private static final String DEPTH_REGEX = "^d\\d+$";
//	private static final String[] TYPES_REGEXES = { "^all_\\d+.txt$", "^best_\\d+.txt$", "^bins\\d+_\\d+.txt$", "^bestbins\\d+_\\d+.txt$" };
//	private static final String[] TYPES_CLEAN = {"all", "best", "bins", "bestbins"};
	// out-file
	private static final String COMMENT = "#";
	static final String OUT_DELIMITER = "\t";
	private static final String EXTENSION = ".csv";
	// misc
	// top-k to compute mean for
	private static final int[] TOP_K = { 1, 10, 100 };
	private static final boolean INCLUDE_MEAN_FOR_WHOLE_RESULT = true;
	private static final boolean DEBUG_PRINTS = true;

	static final ResultFile[] getResults(File theDirectory)
	{
		if (!theDirectory.isDirectory())
			throw new IllegalArgumentException();

		File[] aFiles = getResultFiles(theDirectory);
		ResultFile[] aResults = new ResultFile[aFiles.length];

		for (int i = 0; i < aFiles.length; ++i)
			aResults[i] = new ResultFile(getTable(aFiles[i]));

		return aResults;
	}

	private static final File[] getResultFiles(File theDirectory)
	{
		File[] aFiles = theDirectory.listFiles(FILE_FILTER);
		Arrays.sort(aFiles);
		return aFiles;
	}

	private static final Table getTable(File theFile)
	{
		return new DataLoaderTXT(theFile).getTable();
	}

	// just parse off until first NAME_DELIMITER, good enough in most cases
	static final String getDatasetName(String theFile)
	{
		return splitName(theFile)[DATASET_INDEX];
	}

	static final int getDepth(String theFile)
	{
		// the exact location of -d1- in String can differ
		for (String s : splitName(theFile))
			if (s.matches(DEPTH_REGEX))
				return Integer.parseInt(s.substring(1));

		throw new AssertionError();
	}

	private static final String getSettings(String theFile)
	{
		String[] sa = splitName(theFile);

		// -settings- are after last '-', and up to '_'
		return sa[sa.length-1].split("_")[0];
	}

	static final String getStrategy(String theFile)
	{
		String s = getSettings(theFile);

		for (int i = 0 ; i < s.length(); ++i)
			if (Character.isDigit(s.charAt(i)))
				return new String(s.substring(0, i));

		throw new AssertionError();
	}

	static final int getNrBins(String theFile)
	{
		String s = getSettings(theFile);
		for (int i = 0 ; i < s.length(); ++i)
			if (Character.isDigit(s.charAt(i)))
				return Integer.parseInt(s.substring(i, s.length()));

		throw new AssertionError();
	}

	private static final String[] splitName(String theFile)
	{
		return NAME_SPLITTER.split(theFile);
	}
}
