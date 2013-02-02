package nl.liacs.subdisc;

import java.util.*;

public enum QM implements EnumInterface
{
	// TargetType is a single value, but could be an EnumSet if needed
	// ENUM		GUI text	default measure minimum	TargetType

	// SINGLE_NOMINAL quality measures
	WRACC		("WRAcc",		"0.02",	TargetType.SINGLE_NOMINAL),
	ABSWRACC	("Abs WRAcc",		"0.02",	TargetType.SINGLE_NOMINAL),
	CHI_SQUARED	("Chi-squared",		"50",	TargetType.SINGLE_NOMINAL),
	INFORMATION_GAIN("Information gain",	"0.02",	TargetType.SINGLE_NOMINAL),
	BINOMIAL	("Binomial test",	"0.05",	TargetType.SINGLE_NOMINAL),
	ACCURACY	("Accuracy",		"0.0",	TargetType.SINGLE_NOMINAL),
	PURITY		("Purity",		"0.5",	TargetType.SINGLE_NOMINAL),
	JACCARD		("Jaccard",		"0.2",	TargetType.SINGLE_NOMINAL),
	COVERAGE	("Coverage",		"10",	TargetType.SINGLE_NOMINAL),
	SPECIFICITY	("Specificity",		"0.5",	TargetType.SINGLE_NOMINAL),
	SENSITIVITY	("Sensitivity",		"0.5",	TargetType.SINGLE_NOMINAL),
	LAPLACE		("Laplace",		"0.2",	TargetType.SINGLE_NOMINAL),
	F_MEASURE	("F-measure",		"0.2",	TargetType.SINGLE_NOMINAL),
	G_MEASURE	("G-measure",		"0.2",	TargetType.SINGLE_NOMINAL),
	CORRELATION	("Correlation",		"0.1",	TargetType.SINGLE_NOMINAL),
	PROP_SCORE_WRACC("Propensity score wracc",	"-0.25",	TargetType.SINGLE_NOMINAL),
	PROP_SCORE_RATIO("Propensity score ratio",	"1.0",		TargetType.SINGLE_NOMINAL),
	BAYESIAN_SCORE	("Bayesian Score", "0.0",	TargetType.SINGLE_NOMINAL),

	// SINGLE_NUMERIC quality measures
	Z_SCORE		("Z-Score",		"1.0",	TargetType.SINGLE_NUMERIC),
	INVERSE_Z_SCORE	("Inverse Z-Score",	"1.0",	TargetType.SINGLE_NUMERIC),
	ABS_Z_SCORE	("Abs Z-Score",		"1.0",	TargetType.SINGLE_NUMERIC),
	AVERAGE		("Average",		"0.0",	TargetType.SINGLE_NUMERIC),	// bogus minimum value, should come from data
	INVERSE_AVERAGE	("Inverse Average",	"0.0",	TargetType.SINGLE_NUMERIC),	// bogus minimum value, should come from data
	MEAN_TEST	("Mean Test",		"0.01",	TargetType.SINGLE_NUMERIC),
	INVERSE_MEAN_TEST("Inverse Mean Test",	"0.01",	TargetType.SINGLE_NUMERIC),
	ABS_MEAN_TEST	("Abs Mean Test",	"0.01",	TargetType.SINGLE_NUMERIC),
	T_TEST		("t-Test",		"1.0",	TargetType.SINGLE_NUMERIC),
	INVERSE_T_TEST	("Inverse t-Test",	"1.0",	TargetType.SINGLE_NUMERIC),
	ABS_T_TEST	("Abs t-Test",		"1.0",	TargetType.SINGLE_NUMERIC),
	CHI2_TEST	("Median Chi-squared test",	"2.5",	TargetType.SINGLE_NUMERIC),
	HELLINGER	("Squared Hellinger distance",	"0.0",	TargetType.SINGLE_NUMERIC),
	KULLBACKLEIBLER	("Kullback-Leibler divergence",	"0.0",	TargetType.SINGLE_NUMERIC),
	CWRACC		("CWRAcc",		"0.0",	TargetType.SINGLE_NUMERIC),

	// SINGLE_ORDINAL quality measures
	AUC		("AUC of ROC",		"0.5",	TargetType.SINGLE_ORDINAL),
	WMW_RANKS	("WMW-Ranks test",	"1.0",	TargetType.SINGLE_ORDINAL),
	INVERSE_WMW_RANKS("Inverse WMW-Ranks test",	"1.0",	TargetType.SINGLE_ORDINAL),
	ABS_WMW_RANKS	("Abs WMW-Ranks test",	"1.0",	TargetType.SINGLE_ORDINAL),
	MMAD		("Median MAD metric",	"0",	TargetType.SINGLE_ORDINAL),

	// MULTI_LABEL quality measures
	WEED		("Wtd Ent Edit Dist",	"0",	TargetType.MULTI_LABEL),
	EDIT_DISTANCE	("Edit Distance",	"0",	TargetType.MULTI_LABEL),

	// DOUBLE_CORRELATION  quality measures
	CORRELATION_R		("r",			"0.2",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_R_NEG	("Negative r",		"0.2",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_R_NEG_SQ	("Neg Sqr r",		"0.2",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_R_SQ	("Squared r",		"0.2",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_DISTANCE	("Distance",		"0.0",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_P		("p-Value Distance",	"0.0",	TargetType.DOUBLE_CORRELATION),
	CORRELATION_ENTROPY	("Wtd Ent Distance",	"0.0",	TargetType.DOUBLE_CORRELATION),
	ADAPTED_WRACC		("Adapted WRAcc",	"0.0",	TargetType.DOUBLE_CORRELATION),
	COSTS_WRACC		("Costs WRAcc",		"0.0",	TargetType.DOUBLE_CORRELATION),

	// DOUBLE_REGRESSION quality measures
	LINEAR_REGRESSION	("Significance of Slope Difference", "0.0", TargetType.DOUBLE_REGRESSION),
	COOKS_DISTANCE		("Cook's Distance",	"0.0",	TargetType.DOUBLE_REGRESSION);

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
		EnumSet<QM> aSet = EnumSet.noneOf(QM.class);
		for (QM qm : QM.values())
			if (qm.TARGET_TYPE == theTargetType)
				aSet.add(qm);

		// remove non implemented methods
		if (TargetType.SINGLE_NOMINAL == theTargetType)
		{
			aSet.remove(PROP_SCORE_WRACC);
			aSet.remove(PROP_SCORE_RATIO);
		}
		else if (TargetType.SINGLE_NUMERIC == theTargetType)
			aSet.remove(CHI2_TEST);
		else if (TargetType.DOUBLE_REGRESSION == theTargetType)
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

	@Override
	public String toString() { return GUI_TEXT; };
}
