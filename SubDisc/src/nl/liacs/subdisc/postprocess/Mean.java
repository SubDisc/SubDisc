package nl.liacs.subdisc.postprocess;

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

	// sorting logic
}
