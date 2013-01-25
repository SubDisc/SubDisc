package nl.liacs.subdisc;

import java.util.*;

public enum QM implements EnumInterface
{
	// ENUM		GUI text		default measure minimum

	// SINGLE_NOMINAL quality measures
	WRACC		("WRAcc",		"0.02"),
	ABSWRACC	("Abs WRAcc",		"0.02"),
	CHI_SQUARED	("Chi-squared",		"50"),
	INFORMATION_GAIN("Information gain",	"0.02"),
	BINOMIAL	("Binomial test",	"0.05"),
	ACCURACY	("Accuracy",		"0.0"),
	PURITY		("Purity",		"0.5"),
	JACCARD		("Jaccard",		"0.2"),
	COVERAGE	("Coverage",		"10"),
	SPECIFICITY	("Specificity",		"0.5"),
	SENSITIVITY	("Sensitivity",		"0.5"),
	LAPLACE		("Laplace",		"0.2"),
	F_MEASURE	("F-measure",		"0.2"),
	G_MEASURE	("G-measure",		"0.2"),
	CORRELATION	("Correlation",		"0.1"),
	PROP_SCORE_WRACC("Propensity score wracc",	"-0.25"),
	PROP_SCORE_RATIO("Propensity score ratio",	"1.0"),
	BAYESIAN_SCORE	("Bayesian Score", "0.0"),

	// SINGLE_NUMERIC quality measures
	Z_SCORE		("Z-Score",		"1.0"),
	INVERSE_Z_SCORE	("Inverse Z-Score",	"1.0"),
	ABS_Z_SCORE	("Abs Z-Score",		"1.0"),
	AVERAGE		("Average",		"0.0"),	// XXX bogus value
	INVERSE_AVERAGE	("Inverse Average",	"0.0"),	// XXX bogus value
	MEAN_TEST	("Mean Test",		"0.01"),
	INVERSE_MEAN_TEST("Inverse Mean Test",	"0.01"),
	ABS_MEAN_TEST	("Abs Mean Test",	"0.01"),
	T_TEST		("t-Test",		"1.0"),
	INVERSE_T_TEST	("Inverse t-Test",	"1.0"),
	ABS_T_TEST	("Abs t-Test",		"1.0"),
// TODO ANYONE see QualityMeasure.itsPopulationCounts, disabled for now
//	CHI2_TEST	("Median Chi-squared test",	"2.5"),
	HELLINGER	("Squared Hellinger distance",	"0.0"),
	KULLBACKLEIBLER	("Kullback-Leibler divergence",	"0.0"),
	CWRACC		("CWRAcc", "0.0"),

	// SINGLE_ORDINAL quality measures
	AUC		("AUC of ROC",		"0.5"),
	WMW_RANKS	("WMW-Ranks test",	"1.0"),
	INVERSE_WMW_RANKS("Inverse WMW-Ranks test",	"1.0"),
	ABS_WMW_RANKS	("Abs WMW-Ranks test",	"1.0"),
	MMAD		("Median MAD metric",	"0"),

	// MULTI_LABEL quality measures
	WEED		("Wtd Ent Edit Dist",	"0"),
	EDIT_DISTANCE	("Edit Distance",	"0"),

	// DOUBLE_CORRELATION  quality measures
	CORRELATION_R		("r",			"0.2"),
	CORRELATION_R_NEG	("Negative r",		"0.2"),
	CORRELATION_R_NEG_SQ	("Neg Sqr r",		"0.2"),
	CORRELATION_R_SQ	("Squared r",		"0.2"),
	CORRELATION_DISTANCE	("Distance",		"0.0"),
	CORRELATION_P		("p-Value Distance",	"0.0"),
	CORRELATION_ENTROPY	("Wtd Ent Distance",	"0.0"),
	ADAPTED_WRACC		("Adapted WRAcc",	"0.0"),
	COSTS_WRACC		("Costs WRAcc",		"0.0"),

	// DOUBLE_REGRESSION quality measures
	LINEAR_REGRESSION	("Significance of Slope Difference", "0.0"),
	COOKS_DISTANCE		("Cook's Distance",	"0.0");

	public final String GUI_TEXT;
	public final String MEASURE_DEFAULT;

	private QM(String theGuiText, String theMeasureDefault)
	{
		this.GUI_TEXT = theGuiText;
		this.MEASURE_DEFAULT = theMeasureDefault;
	}

	/*
	 * FIXME MM the QM declaration should indicate for which TargetType it
	 * is valid, this is much more robust against future additions/
	 * re-orderings of the various QMs.
	 * 
	 * TODO MM first/ last QualityMeasure is only used to populate GUI with
	 * relevant QM Strings, this method could return the Strings directly
	 */
	public static final Set<QM> getQualityMeasures(TargetType theTargetType)
	{
		switch (theTargetType)
		{
			case SINGLE_NOMINAL :
				return EnumSet.range(WRACC, BAYESIAN_SCORE);
			case SINGLE_NUMERIC :
				return EnumSet.range(Z_SCORE, CWRACC);
			case SINGLE_ORDINAL :
				return EnumSet.range(AUC, MMAD);
			case DOUBLE_REGRESSION :
				return EnumSet.of(LINEAR_REGRESSION);
				// TODO MM COOKS_DISTANCE is not implemented
				//return EnumSet.range(LINEAR_REGRESSION, COOKS_DISTANCE);
			case DOUBLE_CORRELATION :
				return EnumSet.range(CORRELATION_R, COSTS_WRACC);
			case MULTI_LABEL :
				return EnumSet.range(WEED, EDIT_DISTANCE);
			case MULTI_BINARY_CLASSIFICATION :
				throw new AssertionError(theTargetType);
			default :
				throw new AssertionError(theTargetType);
		}
	}

	/**
	 * Returns the QM corresponding to the supplied {@code String} parameter
	 *  based on the various {@link QM#GUI_TEXT}s.
	 * 
	 * @param theText the {@codeString} corresponding to a QM.
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

	@Override
	public String toString() { return GUI_TEXT; };
}
