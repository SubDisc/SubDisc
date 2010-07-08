package nl.liacs.subdisc;

public class SearchParameters
{
	private TargetConcept itsTargetConcept;
	private	int			itsQualityMeasure;
	private	float		itsQualityMeasureMinimum;

	private	int			itsSearchDepth;
	private	int			itsMinimumCoverage;
	private	float		itsMaximumCoverage;
	private	int			itsMaximumSubgroups;
	private	float		itsMaximumTime;

	private int			itsSearchStrategy;
	private int			itsSearchWidth;
	private	int			itsNumericStrategy;

	private int			itsNrSplitPoints;
	private float		itsAlpha;
	private float		itsBeta;
	private int			itsPostProcessingCount;
	private int			itsMaximumPostProcessingSubgroups;

	//numeric strategy
	public static final int NUMERIC_BINS = 0;
	public static final int NUMERIC_BEST = 1;
	public static final int NUMERIC_ALL = 2;
	public static final int LAST_NUMERIC = NUMERIC_ALL;

	//methods

	public SearchParameters()
	{
	}

	/* QUALITY MEASURE */

	public TargetConcept getTargetConcept() { return itsTargetConcept; }
	public void setTargetConcept(TargetConcept theTargetConcept) { itsTargetConcept = theTargetConcept; }
	public int getTargetType() { return itsTargetConcept.getTargetType(); }
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

	public static String getSearchStrategyName( int theSearchStrategy)
	{
		String aString = "";
		switch(theSearchStrategy)
		{
			case CandidateQueue.BFS		: { aString = "breadth first"; break; }
			case CandidateQueue.DFS		: { aString = "depth first"; break; }
			case CandidateQueue.BESTFIRST	: { aString = "best first"; break; }
			case CandidateQueue.BEAM	: { aString = "beam"; break; }
			default			: { aString = ""; break; }
		}
		return aString;
	}

	public void setSearchStrategy( String theSearchStrategyName)
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

	public int getNumericStrategy()	{ return itsNumericStrategy; }

	public static String getNumericStrategyName(int theNumericStrategy)
	{
		String aString = "";
		switch(theNumericStrategy)
		{
			case NUMERIC_ALL: { aString = "all"; break; }
			case NUMERIC_BEST: { aString = "best"; break; }
			case NUMERIC_BINS: { aString = "bins"; break; }
			default			: { aString = "unknown"; break; }
		}
		return aString;
	}

	public void setNumericStrategy(String theNumericStrategy)
	{
		if (theNumericStrategy.equals("all"))
			itsNumericStrategy = NUMERIC_ALL;
		else if (theNumericStrategy.equals("bins"))
			itsNumericStrategy = NUMERIC_BINS;
		else
			itsNumericStrategy = NUMERIC_BEST;
	}

	public	void setNumericStrategy(int theVal) { itsNumericStrategy = theVal; }

	public	void	setSearchStrategyWidth( int aWidth) { itsSearchWidth = aWidth; }
	public	int		getSearchStrategyWidth() { return itsSearchWidth; }
	public	int		getNrSplitPoints()	{ return itsNrSplitPoints; }
	public	void	setNrSplitPoints(int theNr)	{ itsNrSplitPoints = theNr; }
	public	float	getAlpha()			{ return itsAlpha; }
	public	void	setAlpha(float theAlpha) { itsAlpha = theAlpha; }
	public	float	getBeta()			{ return itsBeta; }
	public	void	setBeta(float theBeta) { itsBeta = theBeta; }
	public	int		getPostProcessingCount()	{ return itsPostProcessingCount; }
	public	void	setPostProcessingCount(int theNr)	{ itsPostProcessingCount = theNr; }
	public	int		getMaximumPostProcessingSubgroups()	{ return itsMaximumPostProcessingSubgroups; }
	public	void	setMaximumPostProcessingSubgroups(int theNr)	{ itsMaximumPostProcessingSubgroups = theNr; }
}