package nl.liacs.subdisc.postprocess;

import java.util.*;

public class Mean
{
	final String itsFile;
	final int itsResultFileSize;

	// these fields are parsed into fields for faster comparisons
	final String itsDataset; // on which experiment was performed
	final int itsDepth;
	final String itsStrategy;
	final int itsNrBins;
	final int itsTopK;
	final double itsMean;

	Mean(ResultFile theResult, int topK)
	{
		itsFile = theResult.getFile();
		itsResultFileSize = theResult.size();

		itsDataset = Parser.getDatasetName(itsFile);
		itsDepth = Parser.getDepth(itsFile);
		itsStrategy = Parser.getStrategy(itsFile);
		itsNrBins = Parser.getNrBins(itsFile);
		itsTopK = topK;
		// NOTE will return NaN when (theResult.size() < topK)
		itsMean = theResult.getMean(itsTopK);
	}

	boolean canAggregate(Mean theMean)
	{
		if (itsDepth != theMean.itsDepth)
			return false;
		if (!itsStrategy.equals(theMean.itsStrategy))
			return false;
		if (itsNrBins != theMean.itsNrBins)
			return false;
		if (itsTopK != theMean.itsTopK)
			return false;

		return true;
	}

	static final Set<String> distinctDatasets(List<Mean> theMeans)
	{
		Set<String> aSet = new TreeSet<String>();

		for (Mean m : theMeans)
			aSet.add(m.itsDataset);

		return aSet;
	}
	static final Set<String> distinctDepths(List<Mean> theMeans)
	{
		return getDistinct(theMeans, true);
	}
	static final Set<String> distinctTopKs(List<Mean> theMeans)
	{
		return getDistinct(theMeans, false);
	}

	private static final Set<String> getDistinct(List<Mean> theMeans, boolean isDepth)
	{
		// Integers for numeric ordering
		Set<Integer> aSet = new TreeSet<Integer>();

		for (Mean m : theMeans)
			aSet.add(isDepth ? m.itsDepth : m.itsTopK);

		// Set promises unique items
		// LinkedHashSet promises predictable ordering
		Set<String> aResult = new LinkedHashSet<String>(aSet.size());
		for (Integer i : aSet)
			aResult.add(i.toString());

		return aResult;
	}

	// sorting logic
}
