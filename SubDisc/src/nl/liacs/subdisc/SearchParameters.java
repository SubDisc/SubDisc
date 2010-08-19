package nl.liacs.subdisc;

import nl.liacs.subdisc.TargetConcept.TargetType;

public enum SearchParameters
{
	THE_ONLY_INSTANCE;

	private TargetConcept	itsTargetConcept;
	private int				itsQualityMeasure;
	private float			itsQualityMeasureMinimum;

	private int				itsSearchDepth;
	private int				itsMinimumCoverage;
	private float			itsMaximumCoverage;
	private int				itsMaximumSubgroups;
	private float			itsMaximumTime;

	private int				itsSearchStrategy;
	private int				itsSearchWidth;
	private NumericStrategy	itsNumericStrategy;

	private int				itsNrSplitPoints;
	private float			itsAlpha;
	private float			itsBeta;
	private int				itsPostProcessingCount;
	private int				itsMaximumPostProcessingSubgroups;

	public static enum NumericStrategy
	{
		NUMERIC_BINS("bins"), NUMERIC_BEST("best"), NUMERIC_ALL("all");

		public final String text;

		private NumericStrategy(String theText) { text = theText; }
	}

	/* QUALITY MEASURE */
	public TargetConcept getTargetConcept() { return itsTargetConcept; }
	public void setTargetConcept(TargetConcept theTargetConcept) { itsTargetConcept = theTargetConcept; }
	public TargetType getTargetType() { return itsTargetConcept.getTargetType(); }
	public int getQualityMeasure() { return itsQualityMeasure; }
	public String getQualityMeasureString() { return QualityMeasure.getMeasureString(itsQualityMeasure); }
	public float getQualityMeasureMinimum() { return itsQualityMeasureMinimum; }
	public void setQualityMeasureMinimum(float theQualityMeasureMinimum) { itsQualityMeasureMinimum = theQualityMeasureMinimum; }
	public void setQualityMeasure(String theQualityMeasure) { itsQualityMeasure = QualityMeasure.getMeasureCode(theQualityMeasure); }
	public void setQualityMeasure(int theQualityMeasure) { itsQualityMeasure = theQualityMeasure; }

	/* SEARCH CONDITIONS */
	public int getSearchDepth() { return itsSearchDepth; }
	public void setSearchDepth(int theSearchDepth) { itsSearchDepth = theSearchDepth; }
	public int getMinimumCoverage() { return itsMinimumCoverage; }
	public void setMinimumCoverage(int theMinimumCoverage) { itsMinimumCoverage = theMinimumCoverage; }
	public float getMaximumCoverage() { return itsMaximumCoverage; }
	public void setMaximumCoverage(float theMaximumCoverage) { itsMaximumCoverage = theMaximumCoverage; }
	public int getMaximumSubgroups() { return itsMaximumSubgroups; }
	public void setMaximumSubgroups(int theMaximumSubgroups) { itsMaximumSubgroups  = theMaximumSubgroups; }
	public float getMaximumTime() { return itsMaximumTime; }
	public void setMaximumTime(float theMaximumTime) { itsMaximumTime = theMaximumTime; }

	/* SEARCH STRATEGY */
	public int getSearchStrategy() { return itsSearchStrategy; }

	public static String getSearchStrategyName(int theSearchStrategy)
	{
		switch(theSearchStrategy)
		{
			case CandidateQueue.BFS		: return "breadth first";
			case CandidateQueue.DFS		: return "depth first";
			case CandidateQueue.BESTFIRST	: return "best first";
			case CandidateQueue.BEAM	: return "beam";
			default						: return "";
		}
	}

	public void setSearchStrategy(String theSearchStrategyName)
	{
		if (theSearchStrategyName.equals("breadth first"))
			itsSearchStrategy = CandidateQueue.BFS;
		else if (theSearchStrategyName.equals("depth first"))
			itsSearchStrategy = CandidateQueue.DFS;
		else if (theSearchStrategyName.equals("best first"))
			itsSearchStrategy = CandidateQueue.BESTFIRST;
		else if (theSearchStrategyName.equals("beam"))
			itsSearchStrategy = CandidateQueue.BEAM;
		else
			itsSearchStrategy = CandidateQueue.BESTFIRST; // default
	}

	public NumericStrategy getNumericStrategy() { return itsNumericStrategy; }

	public void setNumericStrategy(String theNumericStrategy)
	{
/*
		// TODO not safe if theNumericStrategy is 'illegal'
		for(NumericStrategy n : NumericStrategy.values())
			if(n.text.equalsIgnoreCase(theNumericStrategy))
				itsNumericStrategy = n;
*/		if(theNumericStrategy.equalsIgnoreCase("all"))
			itsNumericStrategy = NumericStrategy.NUMERIC_ALL;
		else if(theNumericStrategy.equalsIgnoreCase("bin"))
			itsNumericStrategy = NumericStrategy.NUMERIC_BINS;
		else
			itsNumericStrategy = NumericStrategy.NUMERIC_BEST;
	}

	public void setSearchStrategyWidth(int theWidth)	{ itsSearchWidth = theWidth; }
	public int getSearchStrategyWidth()		{ return itsSearchWidth; }
	public int getNrSplitPoints()			{ return itsNrSplitPoints; }
	public void setNrSplitPoints(int theNr)	{ itsNrSplitPoints = theNr; }
	public float getAlpha()					{ return itsAlpha; }
	public void setAlpha(float theAlpha)	{ itsAlpha = theAlpha; }
	public float getBeta()					{ return itsBeta; }
	public void setBeta(float theBeta)		{ itsBeta = theBeta; }
	public int getPostProcessingCount()		{ return itsPostProcessingCount; }
	public void setPostProcessingCount(int theNr)	{ itsPostProcessingCount = theNr; }
	public int getMaximumPostProcessingSubgroups()	{ return itsMaximumPostProcessingSubgroups; }
	public void setMaximumPostProcessingSubgroups(int theNr)	{ itsMaximumPostProcessingSubgroups = theNr; }
}
