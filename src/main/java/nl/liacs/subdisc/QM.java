package nl.liacs.subdisc;

import java.util.*;

public enum QM implements EnumInterface
{
	// TargetType is a single value, but could be an EnumSet if needed
	// ENUM		GUI text	default measure minimum	TargetType

	// SINGLE_NOMINAL quality measures
	// NOTE when adding a new SINGLE_NOMINAL QM -> add it to getDefinition()
	CORTANA_QUALITY		("Cortana Quality",	"0.1",	TargetType.SINGLE_NOMINAL),
	WRACC			("WRAcc",		"0.02",	TargetType.SINGLE_NOMINAL),
	MUTUAL_INFORMATION	("Mutual Information",	"0.01",	TargetType.SINGLE_NOMINAL),
	ABSWRACC		("Abs WRAcc",		"0.02",	TargetType.SINGLE_NOMINAL),
	CHI_SQUARED		("Chi-squared",		"50",	TargetType.SINGLE_NOMINAL),
	INFORMATION_GAIN	("Information gain",	"0.02",	TargetType.SINGLE_NOMINAL),
	BINOMIAL		("Binomial test",	"0.05",	TargetType.SINGLE_NOMINAL),
	ACCURACY		("Accuracy",		"0.0",	TargetType.SINGLE_NOMINAL),
	PURITY			("Purity",		"0.5",	TargetType.SINGLE_NOMINAL),
	JACCARD			("Jaccard",		"0.2",	TargetType.SINGLE_NOMINAL),
	COVERAGE		("Coverage",		"10",	TargetType.SINGLE_NOMINAL),
	SPECIFICITY		("Specificity",		"0.5",	TargetType.SINGLE_NOMINAL),
	SENSITIVITY		("Sensitivity",		"0.5",	TargetType.SINGLE_NOMINAL),
	LAPLACE			("Laplace",		"0.2",	TargetType.SINGLE_NOMINAL),
	F_MEASURE		("F-measure",		"0.2",	TargetType.SINGLE_NOMINAL),
	G_MEASURE		("G-measure",		"0.2",	TargetType.SINGLE_NOMINAL),
	CORRELATION		("Correlation",		"0.1",	TargetType.SINGLE_NOMINAL),
	PROP_SCORE_WRACC	("Propensity score wracc",	"-0.25",	TargetType.SINGLE_NOMINAL),
	PROP_SCORE_RATIO	("Propensity score ratio",	"1.0",		TargetType.SINGLE_NOMINAL),
	BAYESIAN_SCORE		("Bayesian Score",	"0.0",	TargetType.SINGLE_NOMINAL),
    LIFT            ("Lift",        "1.0",    TargetType.SINGLE_NOMINAL),
    RELATIVE_LIFT   ("Relative Lift",        "1.0",    TargetType.SINGLE_NOMINAL),

	// SINGLE_NUMERIC quality measures
	// NOTE when adding a new SINGLE_NUMERIC QM -> add it to requiredStats()
	Z_SCORE			("Z-Score",		"1.0",	TargetType.SINGLE_NUMERIC),
	INVERSE_Z_SCORE		("Inverse Z-Score",	"1.0",	TargetType.SINGLE_NUMERIC),
	ABS_Z_SCORE		("Abs Z-Score",		"1.0",	TargetType.SINGLE_NUMERIC),
	AVERAGE			("Average",		"0.0",	TargetType.SINGLE_NUMERIC),	// bogus minimum value, should come from data
	INVERSE_AVERAGE		("Inverse Average",	"0.0",	TargetType.SINGLE_NUMERIC),	// bogus minimum value, should come from data
	QM_SUM			("Sum",			"0.0",	TargetType.SINGLE_NUMERIC),	// bogus minimum value, should come from data, note the irregular name, to avoid conflict with SUM
	INVERSE_SUM		("Inverse Sum",		"0.0",	TargetType.SINGLE_NUMERIC),	// bogus minimum value, should come from data
	ABS_DEVIATION	("Abs Deviation",	"0.0",	TargetType.SINGLE_NUMERIC),
	MEAN_TEST		("Mean Test",		"0.01",	TargetType.SINGLE_NUMERIC),
	INVERSE_MEAN_TEST	("Inverse Mean Test",	"0.01",	TargetType.SINGLE_NUMERIC),
	ABS_MEAN_TEST		("Abs Mean Test",	"0.01",	TargetType.SINGLE_NUMERIC),
	T_TEST			("t-Test",		"1.0",	TargetType.SINGLE_NUMERIC),
	INVERSE_T_TEST		("Inverse t-Test",	"1.0",	TargetType.SINGLE_NUMERIC),
	ABS_T_TEST		("Abs t-Test",		"1.0",	TargetType.SINGLE_NUMERIC),
	EXPLAINED_VARIANCE	("Explained Variance",	"0.0",	TargetType.SINGLE_NUMERIC),
	SQUARED_HELLINGER			("Squared Hellinger distance",		"0.0",	TargetType.SINGLE_NUMERIC),
	SQUARED_HELLINGER_WEIGHTED		("Weighted Squared Hellinger distance",	"0.0",	TargetType.SINGLE_NUMERIC),
	SQUARED_HELLINGER_WEIGHTED_ADJUSTED	("Adjusted Squared Hellinger distance",	"0.0",	TargetType.SINGLE_NUMERIC),
	KULLBACK_LEIBLER			("Kullback-Leibler divergence",		"0.0",	TargetType.SINGLE_NUMERIC),
	KULLBACK_LEIBLER_WEIGHTED		("Weighted Kullback-Leibler divergence","0.0",	TargetType.SINGLE_NUMERIC),
	CWRACC					("CWRAcc",				"0.0",	TargetType.SINGLE_NUMERIC),
	CWRACC_UNWEIGHTED			("CWRAcc Unweighted",			"0.0",	TargetType.SINGLE_NUMERIC),

	// MULTI_NUMERIC quality measures
	SQUARED_HELLINGER_2D			("Squared Hellinger distance 2D",		"0.0",	TargetType.MULTI_NUMERIC),
	SQUARED_HELLINGER_WEIGHTED_2D		("Weighted Squared Hellinger distance 2D",	"0.0",	TargetType.MULTI_NUMERIC),
	SQUARED_HELLINGER_WEIGHTED_ADJUSTED_2D	("Adjusted Squared Hellinger distance 2D",	"0.0",	TargetType.MULTI_NUMERIC),
	KULLBACK_LEIBLER_2D			("Kullback-Leibler divergence 2D",		"0.0",	TargetType.MULTI_NUMERIC),
	KULLBACK_LEIBLER_WEIGHTED_2D		("Weighted Kullback-Leibler divergence 2D",	"0.0",	TargetType.MULTI_NUMERIC),
	CWRACC_2D				("CWRAcc_2D",					"0.0",	TargetType.MULTI_NUMERIC),
	CWRACC_UNWEIGHTED_2D			("CWRAcc Unweighted_2D",			"0.0",	TargetType.MULTI_NUMERIC),
	L2					("L2",						"0.0",	TargetType.MULTI_NUMERIC),

	// SINGLE_ORDINAL quality measures
	// NOTE when adding a new SINGLE_ORDINAL QM -> add it to requiredStats()
	AUC			("AUC of ROC",			"0.5",	TargetType.SINGLE_ORDINAL),
	WMW_RANKS		("WMW-Ranks test",		"1.0",	TargetType.SINGLE_ORDINAL),
	INVERSE_WMW_RANKS	("Inverse WMW-Ranks test",	"1.0",	TargetType.SINGLE_ORDINAL),
	ABS_WMW_RANKS		("Abs WMW-Ranks test",		"1.0",	TargetType.SINGLE_ORDINAL),
	MMAD			("Median MAD metric",		"0",	TargetType.SINGLE_ORDINAL),

	// MULTI_LABEL quality measures
	WEED			("Wtd Ent Edit Dist",	"0",	TargetType.MULTI_LABEL),
	EDIT_DISTANCE		("Edit Distance",	"0",	TargetType.MULTI_LABEL),

	// LABEL_RANKING
	LR_NORM			("LRM Norm",			"0.0",	TargetType.LABEL_RANKING),
	LR_NORM_MODE		("LRM Norm Mode",		"0.0",	TargetType.LABEL_RANKING),
	LR_WNORM		("LRM Norm & homog",		"0.0",	TargetType.LABEL_RANKING),
	LR_LABELWISE_MIN	("Labelwise Minimum",		"0.0",	TargetType.LABEL_RANKING),
	LR_LABELWISE_MAX	("Labelwise Maximization",	"0.0",	TargetType.LABEL_RANKING),
	LR_PAIRWISE_MAX		("Pairwise Maximization",	"0.0",	TargetType.LABEL_RANKING),
	LR_COVARIANCE		("Covariance",			"0.0",	TargetType.LABEL_RANKING),

	// DOUBLE_CORRELATION quality measures
	CORRELATION_R		("r",			"0.2",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_R_NEG	("Negative r",		"0.2",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_R_NEG_SQ	("Neg Sqr r",		"0.2",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_R_SQ	("Squared r",		"0.2",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_DISTANCE	("Distance",		"0.0",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_P		("p-Value Distance",	"0.0",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_ENTROPY	("Wtd Ent Distance",	"0.0",	TargetType.DOUBLE_CORRELATION),
	ADAPTED_WRACC		("Adapted WRAcc",	"0.0",	TargetType.DOUBLE_CORRELATION),
	COSTS_WRACC		    ("Costs WRAcc",		"0.0",	TargetType.DOUBLE_CORRELATION),
	CWTPD			    ("CWTPD",		"0.0",	TargetType.DOUBLE_CORRELATION),
	TMCC			    ("TMCC",		"0.0",	TargetType.DOUBLE_CORRELATION),
	MCC			        ("MCC",			"0.0",	TargetType.DOUBLE_CORRELATION),
	PDC			        ("PDC",			"0.0",	TargetType.DOUBLE_CORRELATION),
	MVPDC			    ("MVPDC",		"0.0",	TargetType.DOUBLE_CORRELATION),

	// DOUBLE_BINARY
	RELATIVE_WRACC      ("Relative WRAcc", "0.0", TargetType.DOUBLE_BINARY),
	ABSOLUTE_WRACC      ("Absolute WRAcc", "0.0", TargetType.DOUBLE_BINARY),
	RELATIVE_RISK       ("Relative Risk", "0.0", TargetType.DOUBLE_BINARY),
	ABSOLUTE_RISK       ("Absolute Risk", "0.0", TargetType.DOUBLE_BINARY),

	// DOUBLE_REGRESSION quality measures
	REGRESSION_SSD_COMPLEMENT	("Sign. of Slope Diff. (complement)",	"0.0",	TargetType.DOUBLE_REGRESSION),
	REGRESSION_SSD_DATASET		("Sign. of Slope Diff. (dataset)",	"0.0",	TargetType.DOUBLE_REGRESSION),
	REGRESSION_FLATNESS		("Flatness",				"0.0",	TargetType.DOUBLE_REGRESSION),
	REGRESSION_SSD_4		("Sign. of Slope Diff. 4",		"0.0",	TargetType.DOUBLE_REGRESSION),
	COOKS_DISTANCE			("Cook's Distance",			"0.0",	TargetType.DOUBLE_REGRESSION),

	// SCAPE quality measures
	SUBRANKING_LOSS				("Subranking loss",			"0.0",		TargetType.SCAPE),
	NEGATIVE_SUBRANKING_LOSS		("Negative subranking loss",		"-8243721.5",	TargetType.SCAPE), // #1
	RELATIVE_SUBRANKING_LOSS		("Relative subranking loss",		"0.0",		TargetType.SCAPE),
	REVERSE_RELATIVE_SUBRANKING_LOSS	("Reverse relative subranking loss",	"0.0",		TargetType.SCAPE); // #2
	// WD:
	// #1. I needed something fierce, so here's Ernie's favorite number, but negative.
	// #2. This is relative to the ranking loss in the overall dataset, so only positive values are interesting.
	//     Hence, here, 0 will do as minimum.

	// to enforce implementation of:
	// getDefinition(QM) for SINGLE_NOMINAL QMs
	// requiredStats(QM) for SINGLE_NUMERIC and SINGLE_ORDINAL QMs
	static { getDefinitionTest(); requiredStatsTest(); };

	public final String GUI_TEXT;
	public final String MEASURE_DEFAULT;
	public final TargetType TARGET_TYPE;

	private QM(String theGuiText, String theMeasureDefault, TargetType theTargetType)
	{
		this.GUI_TEXT = theGuiText;
		this.MEASURE_DEFAULT = theMeasureDefault;
		this.TARGET_TYPE = theTargetType;
	}

	public static final Set<QM> getQualityMeasures(TargetType theTargetType)
	{
		Set<QM> aSet = EnumSet.noneOf(QM.class);
		for (QM qm : QM.values())
			if (qm.TARGET_TYPE == theTargetType)
				aSet.add(qm);

		// remove non implemented methods
		aSet.remove(PROP_SCORE_WRACC);
		aSet.remove(PROP_SCORE_RATIO);
		aSet.remove(COOKS_DISTANCE);

		return aSet;
	}

	/**
	 * Returns the QM corresponding to the supplied {@code String} parameter
	 *  based on the various {@link QM#GUI_TEXT}s.
	 *
	 * @param theText the {@code String} corresponding to a QM.
	 *
	 * @return a QM, or {@code null} if no corresponding QM is found.
	 */
	public static QM fromString(String theText)
	{
		for (QM qm : QM.values())
			if (qm.GUI_TEXT.equalsIgnoreCase(theText))
				return qm;

		return null;
	}

	/**
	 * Returns a String with the definition for the supplied QM parameter,
	 * in a format that can be used in a GnuPlot script.
	 *
	 * Note that some definitions call functions defined in:
	 * {@link ROCCurveWindow#getGnuPlotString()}.
	 *
	 * @param theQM the {@link TargetType#SINGLE_NOMINAL} QM for which to
	 * obtain the GnuPlot definition String.
	 *
	 * @return a String
	 *
	 * @see ROCCurveWindow#getGnuPlotString())
	 */
	public static String getDefinition(QM theQM)
	{
		// create 'QM(x,y) = ' -> replace space and dash by underscore
		// throws NullPointerException if theQM == null
		String s = theQM.GUI_TEXT.replaceAll(" ", "_").replaceAll("-", "_") + "(x,y) = ";

		switch (theQM)
		{
			case CORTANA_QUALITY	: return s + "WRAcc(x,y) / ((p/N)-((p/N)*(p/N)))";
			case WRACC		: return s + "(pos(y)/N)-(p/N)*(aCountBody(x,y)/N)";
			case MUTUAL_INFORMATION	: return s +
							" mi( (pos(y)/N)                        , (aTotalTargetCoverageNotBody(y)/N), (aCountNotHeadBody(x,y)/N)         ) +" +
							" mi( (aCountNotHeadBody(x,y)/N)        , (aCountNotHeadNotBody(x)/N)       , (pos(y)/N)                         ) +" +
							" mi( (aTotalTargetCoverageNotBody(y)/N), (pos(y)/N)                        , (aCountNotHeadNotBody(x)/N)        ) +" +
							" mi( (aCountNotHeadNotBody(x)/N)       , (aCountNotHeadBody(x,y)/N)        , (aTotalTargetCoverageNotBody(y)/N) )";
			case ABSWRACC		: return s + "abs(WRAcc(x,y))";
			case CHI_SQUARED	: return s + 
							" sqr(pos(y) - e11(N,aCountBody(x,y),p)) / e11(N,aCountBody(x,y),p) +" +
							" sqr(p - pos(y) - e01(N,aCountBody(x,y),p)) / e01(N,aCountBody(x,y),p) +" +
							" sqr(aCountBody(x,y) - pos(y) - e10(N,aCountBody(x,y),p)) / e10(N,aCountBody(x,y),p) +" +
							" sqr(N - p - aCountBody(x,y) + pos(y) - e00(N,aCountBody(x,y),p)) / e00(N,aCountBody(x,y),p)";
			case INFORMATION_GAIN	: return s + "1 - 0.5*(x+y)*H(x/(x+y)) - 0.5*(2-x-y)*H((1-x)/(2-x-y))";
			case BINOMIAL		: return s + "sqrt(aCountBody(x,y)/N) * (pos(y)/aCountBody(x,y) - p/N)";
			case ACCURACY		: return s + "pos(y)/aCountBody(x,y)";
			case PURITY		: return s + "max(pos(y)/aCountBody(x,y), 1.0-pos(y)/aCountBody(x,y))";
			case JACCARD		: return s + "pos(y)/(aCountBody(x,y) + aTotalTargetCoverageNotBody(y))";
			case COVERAGE		: return s + "aCountBody(x,y)";
			case SPECIFICITY	: return s + "aCountNotHeadNotBody(x)/(N - p)";
			case SENSITIVITY	: return s + "pos(y)/p";
			case LAPLACE		: return s + "(pos(y)+1)/(aCountBody(x,y)+2)";
			case F_MEASURE		: return s + "pos(y)/(p+aCountBody(x,y))";
			case G_MEASURE		: return s + "pos(y)/(aCountNotHeadBody(x,y)+p)";
			case CORRELATION	: return s + "(pos(y)*n - p*aCountNotHeadBody(x,y)) / sqrt(p*n*aCountBody(x,y)*(N-aCountBody(x,y)))";
			// TODO MM
			case PROP_SCORE_WRACC	: return s + "1/0 # TODO";
			case PROP_SCORE_RATIO	: return s + "1/0 # TODO";
			case BAYESIAN_SCORE	: return s + "1/0 # TODO";
            case LIFT           : return s + "(pos(y) * N) / (aCountBody(x,y) * p)";
            case RELATIVE_LIFT  : return s + "(pos(y) * N) / (aCountBody(x,y) * p)"; //FIXME, currently lift
			default :
			{
				// throws NullPointerException if theQM == null
				if (theQM.TARGET_TYPE == TargetType.SINGLE_NOMINAL)
					throw new AssertionError("QM.getDefinition() not implemented for: " + theQM);
				else
					throw new IllegalArgumentException(
						String.format("%s not for %s", theQM, TargetType.SINGLE_NOMINAL));
			}
		}
	}

	/*
	 * throws an AssertionError(QM) if a SINGLE_NOMINAL QM is not completely
	 * implemented (missing case in getDefinition(QM))
	 */
	private static final void getDefinitionTest()
	{
		for (QM qm : QM.values())
			if (qm.TARGET_TYPE == TargetType.SINGLE_NOMINAL)
				getDefinition(qm);

	}

	// NOTE EnumSets are modifiable like any other set, prevent this
	// EnumSet < 65 items are internally represented as a single long
	private static final Set<Stat> SUM = Collections.unmodifiableSet(EnumSet.of(Stat.SUM));
	private static final Set<Stat> SUM_SSD = Collections.unmodifiableSet(EnumSet.of(Stat.SUM, Stat.SSD));
	private static final Set<Stat> SUM_SSD_COMPL = Collections.unmodifiableSet(EnumSet.of(Stat.SUM, Stat.SSD, Stat.COMPL));
	private static final Set<Stat> MEDIAN_MAD = Collections.unmodifiableSet(EnumSet.of(Stat.MEDIAN, Stat.MAD));
	private static final Set<Stat> PDF = Collections.unmodifiableSet(EnumSet.of(Stat.PDF));

	/*
	 * In general, the splitting of an Enum declaration and its logic is a
	 * bad practice. However, the file structure would suffer greatly by
	 * adding a getRequiredStats() method to each Enum declaration.
	 * Also, only SINGLE_NUMERIC QMs require such a method.
	 * But, when a new SINGLE_NUMERIC QM declaration is added, it should
	 * also be added here.
	 */
	/**
	 * Returns a set with the {@link Stat} enums that are required for
	 * computations using the supplied QM parameter.
	 *
	 * @param theQM the {@link TargetType#SINGLE_NUMERIC} QM for which to
	 * obtain the Set<Stat> enums.
	 *
	 * @return a Set<Stat>
	 *
	 * @see Stat
	 * @see SubgroupDiscovery#evaluateCandidate(Subgroup)
	 */
	public static Set<Stat> requiredStats(QM theQM)
	{
		switch(theQM)
		{
			// SINGLE_NUMERIC
			case Z_SCORE :				return SUM;
			case INVERSE_Z_SCORE :		return SUM;
			case ABS_Z_SCORE :			return SUM;
			case AVERAGE :				return SUM;
			case INVERSE_AVERAGE :		return SUM;
			case QM_SUM :				return SUM;
			case INVERSE_SUM :			return SUM;
			case ABS_DEVIATION :		return SUM;
			case MEAN_TEST :			return SUM;
			case INVERSE_MEAN_TEST :	return SUM;
			case ABS_MEAN_TEST :		return SUM;
			case T_TEST :				return SUM_SSD;
			case INVERSE_T_TEST :		return SUM_SSD;
			case ABS_T_TEST :			return SUM_SSD;
			case EXPLAINED_VARIANCE :	return SUM_SSD_COMPL;
			case SQUARED_HELLINGER :					return PDF;
			case SQUARED_HELLINGER_WEIGHTED :			return PDF;
			case SQUARED_HELLINGER_WEIGHTED_ADJUSTED :	return PDF;
			case KULLBACK_LEIBLER :						return PDF;
			case KULLBACK_LEIBLER_WEIGHTED :			return PDF;
			case CWRACC :								return PDF;
			case CWRACC_UNWEIGHTED : 					return PDF;
			// SINGLE_ORDINAL
			case AUC :					return SUM;
			case WMW_RANKS :			return SUM;
			case INVERSE_WMW_RANKS :	return SUM;
			case ABS_WMW_RANKS :		return SUM;
			case MMAD :					return MEDIAN_MAD;
			default :
			{
				// throws NullPointerException if theQM == null
				if (theQM.TARGET_TYPE == TargetType.SINGLE_NUMERIC ||
					theQM.TARGET_TYPE == TargetType.SINGLE_ORDINAL)
					throw new AssertionError(
						"QM.requiredStats() not implemented for: " + theQM);
				else
					throw new IllegalArgumentException(
						String.format("%s not for %s or %s",
								theQM,
								TargetType.SINGLE_NUMERIC,
								TargetType.SINGLE_ORDINAL));
			}
		}
	}

	/*
	 * throws an AssertionError(QM) if a SINGLE_NUMERIC or SINGLE_ORDINAL
	 * QM is not completely implemented (missing case in requiredStats(QM))
	 */
	private static void requiredStatsTest()
	{
		for (QM qm : QM.values())
			if (qm.TARGET_TYPE == TargetType.SINGLE_NUMERIC ||
				qm.TARGET_TYPE == TargetType.SINGLE_ORDINAL)
				requiredStats(qm);
	}

	@Override
	public String toString() { return GUI_TEXT; }
}
