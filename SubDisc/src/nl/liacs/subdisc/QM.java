package nl.liacs.subdisc;

public enum QM implements EnumInterface
{
	// ENUM		GUI text		default measure minimum
	// SINGLE_NOMINAL quality measures
	NOVELTY		("WRAcc",		"0.01"),
	ABSNOVELTY	("Abs WRAcc",		"0.01"),
	CHI_SQUARED	("Chi-squared",		"50"),
	INFORMATION_GAIN("Information gain",	"0"),
	BINOMIAL	("Binomial test", "0.0"),
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
	//SINGLE_NUMERIC quality measures
	Z_SCORE		("Z-Score",		"1.0"),
	INVERSE_Z_SCORE	("Inverse Z-Score",	"1.0"),
	ABS_Z_SCORE	("Abs Z-Score",		"1.0"),
	AVERAGE		("Average",		"0.0"),	// TODO
	INVERSE_AVERAGE	("Inverse Average",	"0.0"),	// TODO
	MEAN_TEST	("Mean Test",		"0.01"),
	INVERSE_MEAN_TEST("Inverse Mean Test",	"0.01"),
	ABS_MEAN_TEST	("Abs Mean Test",	"0.01"),
	T_TEST		("t-Test",		"1.0"),
	INVERSE_T_TEST	("Inverse t-Test",	"1.0"),
	ABS_T_TEST	("Abs t-Test",		"1.0"),
	CHI2_TEST	("Median Chi-squared test", "2.5"),
	//SINGLE_ORDINAL quality measures
	AUC		("AUC of ROC",		"0.5"),
	WMW_RANKS	("WMW-Ranks test",	"1.0"),
	INVERSE_WMW_RANKS("Inverse WMW-Ranks test", "1.0"),
	ABS_WMW_RANKS	("Abs WMW-Ranks test",	"1.0"),
	MMAD		("Median MAD metric",	"0"),
	//MULTI_LABEL quality measures
	WEED		("Wtd Ent Edit Dist",	"0"),
	EDIT_DISTANCE	("Edit Distance",	"0"),
	//DOUBLE_CORRELATION  quality measures
	CORRELATION_R		("r",			"0.2"),
	CORRELATION_R_NEG	("Negative r",		"0.2"),
	CORRELATION_R_SQ	("Squared r",		"0.2"),
	CORRELATION_R_NEG_SQ	("Neg Sqr r",		"0.2"),
	CORRELATION_DISTANCE	("Distance",		"0.0"),
	CORRELATION_P		("p-Value Distance",	"0.0"),
	CORRELATION_ENTROPY	("Wtd Ent Distance",	"0.0"),
	//DOUBLE_REGRESSION quality measures
	LINEAR_REGRESSION	("Significance of Slope Difference", "0.0"),
	COOKS_DISTANCE		("Cook's Distance",	"0.0");

	public final String GUI_TEXT;
	public final String MEASURE_DEFAULT;

	private QM(String theGuiText, String theMeasureDefault)
	{
		this.GUI_TEXT = theGuiText;
		this.MEASURE_DEFAULT = theMeasureDefault;
	}

	public static final QM getFirstEvaluationMeasure(TargetType theTargetType)
	{
		// TODO
		return NOVELTY;
	}

	public static final QM getLastEvaluationMesure(TargetType theTargetType)
	{
		// TODO
		return CORRELATION;
	}

	@Override
	public String toString() { return GUI_TEXT; };
}
