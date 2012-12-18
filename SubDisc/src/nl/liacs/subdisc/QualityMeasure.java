package nl.liacs.subdisc;

// TODO put Contingency table here without screwing up package classes layout.
/**
 * The QualityMeasure class includes all quality measures used
 * ({@link #calculate(int, int) contingency table}).
 */
public class QualityMeasure
{
	private final int itsMeasure;
	private final int itsNrRecords;

	//SINGLE_NOMINAL
	private int itsTotalTargetCoverage;

	//SINGLE_NUMERIC and SINGLE_ORDINAL
	private float itsTotalAverage = 0.0f;
	private double itsTotalSampleStandardDeviation = 0.0;
	private int[] itsPopulationCounts;	// TODO implement for CHI2_TEST
	private ProbabilityDensityFunction itsPDF; // entire dataset

	//Bayesian
	private DAG itsDAG;
	private static int itsNrNodes;
	private static float itsAlpha;
	private static float itsBeta;
	private static boolean[][] itsVStructures;

	//SINGLE_NOMINAL quality measures
	public static final int WRACC     		= 0;
	public static final int ABSWRACC  		= WRACC+1;
	public static final int CHI_SQUARED 	= ABSWRACC+1;
	public static final int INFORMATION_GAIN = CHI_SQUARED+1;
	public static final int BINOMIAL    	= INFORMATION_GAIN+1;
	public static final int ACCURACY    	= BINOMIAL + 1;
	public static final int PURITY 			= ACCURACY + 1;
	public static final int JACCARD     	= PURITY + 1;
	public static final int COVERAGE    	= JACCARD + 1;
	public static final int SPECIFICITY 	= COVERAGE + 1;
	public static final int SENSITIVITY 	= SPECIFICITY + 1;
	public static final int LAPLACE     	= SENSITIVITY + 1;
	public static final int F_MEASURE   	= LAPLACE + 1;
	public static final int G_MEASURE   	= F_MEASURE + 1;
	public static final int CORRELATION 	= G_MEASURE + 1;
	public static final int PROP_SCORE_WRACC = CORRELATION + 1;
	public static final int PROP_SCORE_RATIO = PROP_SCORE_WRACC + 1;
	public static final int BAYESIAN_SCORE 	= PROP_SCORE_RATIO + 1;

	//SINGLE_NUMERIC quality measures
	public static final int Z_SCORE 		= 118;
	public static final int INVERSE_Z_SCORE = 119;
	public static final int ABS_Z_SCORE 	= 120;
	public static final int AVERAGE 		= 121;
	public static final int INVERSE_AVERAGE = 122;
	public static final int MEAN_TEST 		= 123;
	public static final int INVERSE_MEAN_TEST = 124;
	public static final int ABS_MEAN_TEST 	= 125;
	public static final int T_TEST = 126;
	public static final int INVERSE_T_TEST 	= 127;
	public static final int ABS_T_TEST 		= 128;
	public static final int CHI2_TEST 		= 129;	// TODO see itsPopulationCounts
	public static final int HELLINGER 		= 130;
	public static final int KULLBACKLEIBLER 	= 131;
	public static final int CWRACC 			= 132;

	//SINGLE_ORDINAL quality measures
	public static final int AUC = 30;
	public static final int WMW_RANKS = 31;
	public static final int INVERSE_WMW_RANKS = 32;
	public static final int ABS_WMW_RANKS = 33;
	public static final int MMAD = 34;
	//MULTI_LABEL quality measures
	public static final int WEED = 35;
	public static final int EDIT_DISTANCE = 36;
	//DOUBLE_CORRELATION
	public static final int CORRELATION_R = 37;
	public static final int CORRELATION_R_NEG = 38;
	public static final int CORRELATION_R_NEG_SQ = 39;
	public static final int CORRELATION_R_SQ = 40;
	public static final int CORRELATION_DISTANCE = 41;
	public static final int CORRELATION_P = 42;
	public static final int CORRELATION_ENTROPY = 43;
	public static final int ADAPTED_WRACC = 44;
	public static final int COSTS_WRACC = 45;
	//DOUBLE_REGRESSION
	public static final int LINEAR_REGRESSION = 46;
	public static final int COOKS_DISTANCE = 47;

	//SINGLE =========================================================================================

	//SINGLE_NOMINAL
	public QualityMeasure(int theMeasure, int theTotalCoverage, int theTotalTargetCoverage)
	{
		itsMeasure = theMeasure;
		itsNrRecords = theTotalCoverage;
		itsTotalTargetCoverage = theTotalTargetCoverage;
	}

	//SINGLE_NUMERIC
	public QualityMeasure(int theMeasure, int theTotalCoverage, float theTotalSum, float theTotalSSD, ProbabilityDensityFunction thePDF)
	{
		itsMeasure = theMeasure;
		itsNrRecords = theTotalCoverage;
		if (itsNrRecords > 0)
			itsTotalAverage = theTotalSum/itsNrRecords;
		if (itsNrRecords > 1)
			itsTotalSampleStandardDeviation = Math.sqrt(theTotalSSD/(itsNrRecords-1));
		//itsPopulationCounts = null;	// TODO see itsPopulationCounts
		itsPDF = thePDF;
	}

	public static int getFirstEvaluationMeasure(TargetType theTargetType)
	{
		if(theTargetType == null)
			return WRACC;	// MM TODO for now

		switch(theTargetType)
		{
			case SINGLE_NOMINAL		: return WRACC;
			case SINGLE_NUMERIC		: return Z_SCORE;
			case SINGLE_ORDINAL		: return AUC;
			case MULTI_LABEL		: return WEED;
			case DOUBLE_CORRELATION		: return CORRELATION_R;
			case DOUBLE_REGRESSION		: return LINEAR_REGRESSION;
			// TODO for stable jar, disabled
			//case DOUBLE_REGRESSION 	: return COOKS_DISTANCE;
			default					: return WRACC;
		}
	}

	public static int getLastEvaluationMesure(TargetType theTargetType)
	{
		if(theTargetType == null)
			return WRACC;	// MM TODO for now

		switch(theTargetType)
		{
			case SINGLE_NOMINAL		: return BAYESIAN_SCORE;
			//case SINGLE_NUMERIC		: return CHI2_TEST;	// TODO see itsPopulationCounts
			case SINGLE_NUMERIC		: return CWRACC;
			case SINGLE_ORDINAL		: return MMAD;
			case MULTI_LABEL		: return EDIT_DISTANCE;
			case DOUBLE_CORRELATION		: return COSTS_WRACC;
			case DOUBLE_REGRESSION		: return LINEAR_REGRESSION;
			// TODO for stable jar, disabled
			//case DOUBLE_REGRESSION 	: return COOKS_DISTANCE;
			default					: return WRACC;
		}
	}

	public ProbabilityDensityFunction getProbabilityDensityFunction()
	{
		return itsPDF;
	}

	/**
	 * Contingency table:</br>
	 * <table border="1" cellpadding="2" cellspacing="0">
	 * 	<tr align="center">
	 * 		<td></td>
	 * 		<td>B</td>
	 * 		<td><span style="text-decoration: overline">B</span></td>
	 * 		<td></td>
	 * 	</tr>
	 * 	<tr align="center">
	 * 		<td>H</td>
	 * 		<td><i>n</i>(HB)</td>
	 * 		<td><i>n</i>(H<span style="text-decoration: overline">B</span>)</td>
	 * 		<td><i>n</i>(H)</td>
	 * 	</tr>
	 * 	<tr align="center">
	 * 		<td><span style="text-decoration: overline">H</span></td>
	 * 		<td><i>n</i>(<span style="text-decoration: overline">H</span>B)</td>
	 * 		<td><i>n</i>(<span style="text-decoration: overline">HB</span>)</td>
	 * 		<td><i>n</i>(<span style="text-decoration: overline">H</span>)</td>
	 * 	</tr>
	 * 	<tr align="center">
	 * 		<td></td>
	 * 		<td><i>n</i>(B)</td>
	 * 		<td><i>n</i>(<span style="text-decoration: overline">B</span>)</td>
	 * 		<td>N</td>
	 * 	</tr>
	 * </table>
	 */
	public float calculate(float theCountHeadBody, float theCoverage)
	{
		float aResult = calculate(itsMeasure, itsNrRecords, itsTotalTargetCoverage, theCountHeadBody, theCoverage);
		if (Float.isNaN(aResult))
			return 0.0f;
		else
			return aResult;
	}

	//SINGLE_NOMINAL ============================================================

	public static float calculate(int theMeasure, int theTotalCoverage, float theTotalTargetCoverage,
							float theCountHeadBody, float theCoverage)
	{
		float aCountNotHeadBody		= theCoverage - theCountHeadBody;
		float aTotalTargetCoverageNotBody		= theTotalTargetCoverage - theCountHeadBody;
		float aCountNotHeadNotBody	= theTotalCoverage - (theTotalTargetCoverage + aCountNotHeadBody);
		float aCountBody			= aCountNotHeadBody + theCountHeadBody;

		float returnValue = -10f; //Bad measure value for default
		switch(theMeasure)
		{
			case WRACC:
			{
				returnValue =(theCountHeadBody/theTotalCoverage)-(theTotalTargetCoverage/theTotalCoverage)*(aCountBody/theTotalCoverage);
				break;
			}
			case CHI_SQUARED:
			{
				returnValue = calculateChiSquared(theTotalCoverage, theTotalTargetCoverage, aCountBody, theCountHeadBody);
				break;
			}
			case INFORMATION_GAIN:
			{
				returnValue = calculateInformationGain(theTotalCoverage, theTotalTargetCoverage, aCountBody, theCountHeadBody);
				break;
			}
			case BINOMIAL:
			{
				returnValue = ((float) Math.sqrt(aCountBody/theTotalCoverage)) * (theCountHeadBody/aCountBody - theTotalTargetCoverage/theTotalCoverage);
				break;
			}
			case JACCARD: {		returnValue = theCountHeadBody /(aCountBody + aTotalTargetCoverageNotBody);
								break; }
			case COVERAGE: {	returnValue = aCountBody;
								break; }
			case ACCURACY: {	returnValue = theCountHeadBody /aCountBody;
								break; }
			case SPECIFICITY: {	returnValue = aCountNotHeadNotBody / (theTotalCoverage - theTotalTargetCoverage);
								break; }
			case SENSITIVITY: {	returnValue = theCountHeadBody / theTotalTargetCoverage;
								break; }
			case LAPLACE: {		returnValue = (theCountHeadBody+1)/(aCountBody+2);
								break; }
			case F_MEASURE: {	returnValue = theCountHeadBody/(theTotalTargetCoverage+aCountBody);
								break; }
			case G_MEASURE: {	returnValue = theCountHeadBody/(aCountNotHeadBody+theTotalTargetCoverage);
								break; }
			case CORRELATION:
			{
				float aCountNotHead = theTotalCoverage-theTotalTargetCoverage;
				returnValue = (theCountHeadBody*aCountNotHead - theTotalTargetCoverage*aCountNotHeadBody)/((float)Math.sqrt(theTotalTargetCoverage*aCountNotHead*aCountBody*(theTotalCoverage-aCountBody)));
				break;
			}
			case PURITY:
			{
				returnValue = theCountHeadBody/aCountBody;
				if (returnValue < 0.5)
					returnValue = 1.0F - returnValue;
				break;
			}
			case ABSWRACC:
			{
				returnValue = theCountHeadBody/theTotalCoverage - ((theTotalTargetCoverage/theTotalCoverage) * aCountBody/theTotalCoverage);
				returnValue = Math.abs(returnValue);
				break;
			}
			case BAYESIAN_SCORE:
			{
				returnValue = (float)calculateBayesianFactor(theTotalCoverage, theTotalTargetCoverage, aCountBody, theCountHeadBody);
				break;

			}
		}
		return returnValue;
	}

	public float calculatePropensityBased(int theMeasure, int theCountHeadBody, int theCoverage, int theTotalCount ,double theCountHeadPropensityScore)
	{
		float aCountHeadBody = (float) theCountHeadBody;
		float aCoverage = (float) theCoverage;
		float aTotalCount = (float) theTotalCount;
		float aCountHeadPropensityScore = (float) theCountHeadPropensityScore;
		//float aCountNotHeadBody		= theCoverage - theCountHeadBody;
		//float aTotalTargetCoverageNotBody		= theTotalTargetCoverage - theCountHeadBody;
		//float aCountNotHeadNotBody	= theTotalCoverage - (theTotalTargetCoverage + aCountNotHeadBody);
		//float aCountBody			= aCountNotHeadBody + theCountHeadBody;
		float returnValue = -10;
		switch(theMeasure)
		{
			case PROP_SCORE_WRACC:
			{
				returnValue = ((aCountHeadBody/aCoverage - (aCountHeadPropensityScore/aCoverage) ) * aCoverage/aTotalCount);
				System.out.println("Calculate Propensity based WRAcc");
				System.out.println(returnValue);
				break;
			}
			case PROP_SCORE_RATIO:
			{
				returnValue = (aCountHeadBody/aTotalCount) / (aCountHeadPropensityScore/aTotalCount);
			}
		}
		return returnValue;
	}

	private static float calculateChiSquared(float totalSupport, float headSupport, float bodySupport, float bodyHeadSupport)
	{
		//HEADBODY
		float Eij = calculateExpectency(totalSupport, bodySupport, headSupport);
		float quality = (calculatePowerTwo(bodyHeadSupport - Eij))/ Eij;

		//HEADNOTBODY
		Eij = calculateExpectency(totalSupport, (totalSupport - bodySupport), headSupport);
		quality += (calculatePowerTwo(headSupport - bodyHeadSupport - Eij)) / Eij;

		//NOTHEADBODY
		Eij = calculateExpectency(totalSupport, (totalSupport - headSupport), bodySupport);
		quality += (calculatePowerTwo(bodySupport - bodyHeadSupport - Eij)) / Eij;

		//NOTHEADNOTBODY
		Eij = calculateExpectency(totalSupport, (totalSupport - bodySupport), (totalSupport - headSupport));
		quality += (calculatePowerTwo((totalSupport - headSupport - bodySupport + bodyHeadSupport) - Eij)) / Eij;

		return quality;
	}

	private static float calculatePowerTwo(float value)
	{
		return (value * value);
	}

	private static float calculateExpectency(float totalSupport, float bodySupport, float headSupport)
	{
		return totalSupport * (bodySupport / totalSupport) * (headSupport / totalSupport);
	}

	/**
	 * Computes the 2-log of p.
	 */
	private static float lg(float p)
	{
		return ((float) Math.log(p))/(float)Math.log(2);
	}

	public static float calculateEntropy(float bodySupport, float headBodySupport)
	{
		if (bodySupport == 0)
			return 0.0f; //special case that should never occur

		if (headBodySupport==0 || bodySupport==headBodySupport)
			return 0.0f; // by definition

		float pj = headBodySupport/bodySupport;
		return -1.0f*pj*lg(pj) - (1-pj)*lg(1-pj);
	}

	/**
	 * Calculates the ConditionalEntropy.
	 * By definition, 0*lg(0) is 0, such that any boundary cases return 0.
	 *
	 * @param bodySupport
	 * @param bodyHeadSupport
	 * @return the conditional entropy for given the two parameters.
	 */
	public static float calculateConditionalEntropy(float bodySupport, float bodyHeadSupport)
	{
		if (bodySupport == 0)
			return 0.0f; //special case that should never occur

		float Phb = bodyHeadSupport/bodySupport; //P(H|B)
		float Pnhb = (bodySupport - bodyHeadSupport)/bodySupport; //P(H|B)
		if (Phb == 0 || Pnhb == 0)
			return 0.0f; //by definition

		float quality = -1.0f*Phb*lg(Phb) - Pnhb*lg(Pnhb);
		return quality;
	}

	public static float calculateInformationGain(float totalSupport, float headSupport, float bodySupport, float headBodySupport)
	{
		float aFraction = bodySupport/totalSupport;
		float aNotBodySupport = totalSupport-bodySupport;
		float aHeadNotBodySupport = headSupport-headBodySupport;
		return calculateEntropy(totalSupport, headSupport)
			- aFraction*calculateConditionalEntropy(bodySupport, headBodySupport) //inside the subgroup
			- (1-aFraction)*calculateConditionalEntropy(aNotBodySupport, aHeadNotBodySupport); //the complement
	}

    //Iyad Batal: Calculate the Bayesian score assuming uniform beta priors on all parameters
	public static double calculateBayesianFactor(float totalSupport, float headSupport, float bodySupport, float headBodySupport)
	{
		//type=Bayes_factor: the score is the Bayes factor of model M_h (a number between [-Inf, + Inf])
		//score = P(M_h)*P(D|M_h) / (P(M_l)*P(D|M_l)+P(M_e)*P(D|M_e))

		//True Positive
		int N11 = (int)headBodySupport;
		//False Positive
		int N12 = (int)(bodySupport-headBodySupport);
		//False Negative
		int N21 = (int)(headSupport-headBodySupport);
		//True Negative
		int N22 = (int)(totalSupport-N11-N12-N21);

		int N1 = N11+N21;
		int N2 = N12+N22;

		//the parameter priors: uniform priors
		int alpha = 1, beta = 1;
		int alpha1 = 1, beta1 = 1;
		int alpha2 = 1, beta2 = 1;

		double logM_e = score_M_e(N1, N2, alpha, beta);
		double[] res = score_M_h(N11, N12, N21, N22, alpha1, beta1, alpha2, beta2);
		double logM_h = res[0];
		res = score_M_h(N21, N22, N11, N12, alpha2, beta2, alpha1, beta1);
		double logM_l = res[0];

		//assume uniform prior on all models
		double prior_M_e = 0.33333333, prior_M_h = 0.33333333, prior_M_l = 0.33333334;
		double log_numerator = Math.log(prior_M_h) + logM_h;
		double log_denom1 = logAdd(Math.log(prior_M_e) + logM_e, Math.log(prior_M_l) + logM_l);

		double bayesian_score = log_numerator - log_denom1;
		return bayesian_score;
	}

    //Iyad Batal: Calculate the Bayesian score assuming uniform beta priors on all parameters
	public static double calculateBayesianScore(float totalSupport, float headSupport, float bodySupport, float headBodySupport)
	{
		String type="Bayes_factor";
		//type=Bayes_factor: the score is the Bayes factor of model M_h (a number between [-Inf, + Inf])
		//score = P(M_h)*P(D|M_h) / (P(M_l)*P(D|M_l)+P(M_e)*P(D|M_e))

		//String type="posterior";
		//type=posterior: the score is the posterior of model M_h (a number between [0, 1])
		//score = P(M_h)*P(D|M_h) / (P(M_l)*P(D|M_l)+P(M_e)*P(D|M_e)+P(M_h)*P(D|M_h))

		//Both Bayes_factor and posterior provide the same ranking of the patterns!

		//True Positive
		int N11 = (int)headBodySupport;
		//False Positive
		int N12 = (int)(bodySupport-headBodySupport);
		//False Negative
		int N21 = (int)(headSupport-headBodySupport);
		//True Negative
		int N22 = (int)(totalSupport-N11-N12-N21);

		int N1 = N11+N21;
		int N2 = N12+N22;

		//the parameter priors: uniform priors
		int alpha = 1, beta = 1;
		int alpha1 = 1, beta1 = 1;
		int alpha2 = 1, beta2 = 1;

		double logM_e = score_M_e(N1, N2, alpha, beta);
		double[] res = score_M_h(N11, N12, N21, N22, alpha1, beta1, alpha2, beta2);
		double logM_h = res[0];
		res = score_M_h(N21, N22, N11, N12, alpha2, beta2, alpha1, beta1);
		double logM_l = res[0];

		//assume uniform prior on all models
		double prior_M_e = 0.33333333, prior_M_h = 0.33333333, prior_M_l = 0.33333334;
		double log_numerator = Math.log(prior_M_h) + logM_h;
		double log_denom1 = logAdd(Math.log(prior_M_e) + logM_e, Math.log(prior_M_l) + logM_l);
		double log_denominator = logAdd(log_denom1, Math.log(prior_M_h) + logM_h);

		//this is the posterior probability of model M_h
		double M_h_posterior = Math.exp(log_numerator - log_denominator);

		double bayesian_score = log_numerator - log_denom1;

		if(type.equals("Bayes_factor"))
			return bayesian_score;
		else
			return M_h_posterior;
	}

	//Iyad Batal: auxiliary function to compute the sum of logarithms (input: log(a), log(b), output log(a+b))
	private static double logAdd(double x, double y)
	{
		double res;
		if (Math.abs(x-y) >= 36.043)
			res = Math.max(x, y);
		else
			res= Math.log(1 + Math.exp(y - x)) + x;
		return res;
	}

	//Iyad Batal: auxiliary function to compute the difference of logarithms (input: log(a), log(b), output log(a-b))
	private static double logDiff(double x, double y)
	{
		double res;
		if((x-y) >= 36.043)
			res = x;
		else
			res = Math.log(1-Math.exp(y - x)) + x;
		return res;
	}

	//Iyad Batal: auxiliary function to compute the marginal likelihood of model M_e (used in computing the Bayesian score)
	private static double score_M_e(int N1, int N2, int alpha, int beta)
	{
		return Function.logGammaBig(alpha+beta) - Function.logGammaBig(alpha+N1+beta+N2) + Function.logGammaBig(alpha+N1) - Function.logGammaBig(alpha) + Function.logGammaBig(beta+N2) - Function.logGammaBig(beta);
	}

	//Iyad Batal: auxiliary function to compute the marginal likelihood of model M_h (used in computing the Bayesian score)
	private static double[] score_M_h(int N11, int N12, int N21, int N22, int alpha1, int beta1, int alpha2, int beta2)
	{

		int a = N21+alpha2;
		int b = N22+beta2;
		int c = N11+alpha1;
		int d = N12+beta1;

		double k = 0.5;
		double C = Function.logGammaBig(alpha1+beta1) - Function.logGammaBig(alpha1) - Function.logGammaBig(beta1) + Function.logGammaBig(alpha2+beta2) - Function.logGammaBig(alpha2) - Function.logGammaBig(beta2);

		double part2=0;
		for (int i=1; i<=b; i++)
		{
			 int j=a+i-1;
			 double temp = Function.logGammaBig(a) + Function.logGammaBig(b) - Function.logGammaBig(j+1) - Function.logGammaBig(a+b-j) + Function.logGammaBig(c+j) + Function.logGammaBig(a+b+d-1-j) - Function.logGammaBig(a+b+c+d-1);
			 if (i==1)
				 part2 = temp;
			 else
				 part2 = logAdd(part2,temp);
		}

		double part1 = Function.logGammaBig(a) + Function.logGammaBig(b) - Function.logGammaBig(a+b) + Function.logGammaBig(c) + Function.logGammaBig(d) - Function.logGammaBig(c+d);

		double[] res = new double[2];

		res[0] = -Math.log(k) + C + part2;
		res[1] = logDiff(-Math.log(k)+C+part1, res[0]);

		return res;
	}

	public int getNrRecords() { return itsNrRecords; }
	public int getNrPositives() { return itsTotalTargetCoverage; }

	//get quality of upper left corner
	public float getROCHeaven()
	{
		return calculate(itsTotalTargetCoverage, itsTotalTargetCoverage);
	}

	//lower right corner
	public float getROCHell()
	{
		return calculate(0, itsNrRecords - itsTotalTargetCoverage);
	}







	//SINGLE_NUMERIC ===============================================

	public float calculate(int theCoverage, float theSum, float theSSD,	float theMedian, float theMedianAD, int[] theSubgroupCounts, ProbabilityDensityFunction thePDF)
	{
		float aReturn = Float.NEGATIVE_INFINITY;
		switch(itsMeasure)
		{
			//NUMERIC
			case AVERAGE			: { aReturn = theSum/theCoverage; break; }
			case INVERSE_AVERAGE	: { aReturn = -theSum/theCoverage; break; }
			case MEAN_TEST			:
			{
				aReturn = (float) Math.sqrt(theCoverage) * (theSum/theCoverage-itsTotalAverage);
				break;
			}
			case INVERSE_MEAN_TEST :
			{
				aReturn = (float) -(Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalAverage));
				break;
			}
			case ABS_MEAN_TEST :
			{
				aReturn = (float) Math.abs(Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalAverage));
				break;
			}
			case Z_SCORE	:
			{
				if(itsNrRecords <= 1)
					aReturn = 0.0f;
				else
					aReturn = (float) ((Math.sqrt(theCoverage) * (theSum/theCoverage-itsTotalAverage))/
								itsTotalSampleStandardDeviation);
				break;
			}
			case INVERSE_Z_SCORE :
			{
				if(itsNrRecords <= 1)
					aReturn = 0.0f;
				else
					aReturn = (float) -((Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalAverage))/
								itsTotalSampleStandardDeviation);
				break;
			}
			case ABS_Z_SCORE :
			{
				if(itsNrRecords <= 1)
					aReturn = 0.0f;
				else
					aReturn = (float) Math.abs((Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalAverage))/
									itsTotalSampleStandardDeviation);
				break;
			}
			case T_TEST	:
			{
				if(theCoverage <= 2)
					aReturn = 0;
				else
					aReturn = (float) ((Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalAverage))/Math.sqrt(theSSD/(theCoverage-1)));
				break;
			}
			case INVERSE_T_TEST	:
			{
				if(theCoverage <= 2)
					aReturn = 0;
				else
					aReturn = (float) -((Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalAverage))/Math.sqrt(theSSD/(theCoverage-1)));
				break;
			}
			case ABS_T_TEST	:
			{
				if(theCoverage <= 2)
					aReturn = 0;
				else
					aReturn = (float) Math.abs((Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalAverage))/Math.sqrt(theSSD/(theCoverage-1)));
				break;
			}
			case CHI2_TEST :
			{
				if (itsPopulationCounts == null) {
					Log.logCommandLine("--- ERROR! QualityMeasure.calculate(): unimplemented method. ---");
					aReturn = Float.NaN;
					break;
				}
				// TODO see itsPopulationCounts
				float a = ((theSubgroupCounts[0]-itsPopulationCounts[0])*(theSubgroupCounts[0]-itsPopulationCounts[0]))/(float)itsPopulationCounts[0];
				float b = ((theSubgroupCounts[1]-itsPopulationCounts[1])*(theSubgroupCounts[1]-itsPopulationCounts[1]))/(float)itsPopulationCounts[1];
				aReturn = a+b; break;
			}
			//ORDINAL
			case AUC				:
			{
				float aComplementCoverage = itsNrRecords - theCoverage;
				float aSequenceSum = theCoverage*(theCoverage+1)/2.0f; //sum of all positive ranks, assuming ideal case
				aReturn = 1 + (aSequenceSum-theSum)/(theCoverage*aComplementCoverage);
				break;
			}
			case WMW_RANKS :
			{
				float aComplementCoverage = itsNrRecords - theCoverage;
				float aMean = (theCoverage*(theCoverage+aComplementCoverage+1))/2.0f;
				float aStDev = (float) Math.sqrt((theCoverage*aComplementCoverage*(theCoverage+aComplementCoverage+1))/12.0f);
				aReturn = (theSum-aMean)/aStDev; break;
			}
			case INVERSE_WMW_RANKS :
			{
				float aComplementCoverage = itsNrRecords - theCoverage;
				float aMean = (theCoverage*(theCoverage+aComplementCoverage+1))/2.0f;
				float aStDev = (float) Math.sqrt((theCoverage*aComplementCoverage*(theCoverage+aComplementCoverage+1))/12.0f);
				aReturn = -((theSum-aMean)/aStDev); break;
			}
			case ABS_WMW_RANKS :
			{
				float aComplementCoverage = itsNrRecords - theCoverage;
				float aMean = (theCoverage*(theCoverage+aComplementCoverage+1))/2.0f;
				float aStDev = (float) Math.sqrt((theCoverage*aComplementCoverage*(theCoverage+aComplementCoverage+1))/12.0f);
				aReturn = Math.abs((theSum-aMean)/aStDev); break;
			}
			case MMAD : { aReturn = (theCoverage/(2*theMedian+theMedianAD)); break; }
			case HELLINGER :
			{
				float aTotalSquaredDifference =0f;
				for (int i=0; i<itsPDF.size(); i++)
				{
					float aDensity = itsPDF.getDensity(i);
					float aDensitySubgroup = thePDF.getDensity(i);
					float aDifference = (float)(Math.sqrt(aDensity) - Math.sqrt(aDensitySubgroup));
					aTotalSquaredDifference += aDifference*aDifference;
					//Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				}
				Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				aReturn = 0.5f*(aTotalSquaredDifference*theCoverage)/itsNrRecords;
				break;
			} //TODO
			case KULLBACKLEIBLER :
			{
				float aTotalDivergence =0f;
				for (int i=0; i<itsPDF.size(); i++)
				{
					float aDensity = itsPDF.getDensity(i);
					float aDensitySubgroup = thePDF.getDensity(i);
					aTotalDivergence = aDensitySubgroup*(float)Math.log(aDensitySubgroup/aDensity);
				}
				aReturn = aTotalDivergence*theCoverage/(float)itsNrRecords; break;
			}
			case CWRACC :
			{
				//some random code
				float aTotalDifference = 0f;
				for(int i=0; i<itsPDF.size(); i++)
				{
					float aDensity = itsPDF.getDensity(i);
					float aDensitySubgroup = thePDF.getDensity(i);
					aTotalDifference += Math.abs(aDensity - aDensitySubgroup);
				}
				Log.logCommandLine("difference in PDF: " + aTotalDifference);
				aReturn = aTotalDifference*theCoverage/(float)itsNrRecords;
				break;
			}
		}
		return aReturn;
	}

	//==========================================

	public static String getMeasureMinimum(String theEvaluationMeasure, float theAverage)
	{
		String anEvaluationMinimum = new String();
		switch(getMeasureCode(theEvaluationMeasure))
		{
			//NOMINAL
			case WRACC	: 		{ anEvaluationMinimum = "0.02"; break; }
			case ABSWRACC : 		{ anEvaluationMinimum = "0.02"; break; }
			case CHI_SQUARED: 		{ anEvaluationMinimum = "50"; break; }
			case INFORMATION_GAIN: 	{ anEvaluationMinimum = "0.02"; break; }
			case BINOMIAL: 			{ anEvaluationMinimum = "0.05"; break; }
			case JACCARD	: 		{ anEvaluationMinimum = "0.2"; break; }
			case COVERAGE	: 		{ anEvaluationMinimum = "10"; break; }
			case ACCURACY	: 		{ anEvaluationMinimum = "0.0"; break; }
			case SPECIFICITY: 		{ anEvaluationMinimum = "0.5"; break; }
			case SENSITIVITY: 		{ anEvaluationMinimum = "0.5"; break; }
			case PURITY		: 		{ anEvaluationMinimum = "0.5"; break; }
			case LAPLACE	:		{ anEvaluationMinimum = "0.2"; break; }
			case F_MEASURE	:		{ anEvaluationMinimum = "0.2"; break; }
			case G_MEASURE	:		{ anEvaluationMinimum = "0.2"; break; }
			case CORRELATION:		{ anEvaluationMinimum = "0.1"; break; }
			case PROP_SCORE_WRACC:	{ anEvaluationMinimum = "-0.25"; break; }
			case PROP_SCORE_RATIO:	{ anEvaluationMinimum = "1.0"; break; }
			case BAYESIAN_SCORE:	{ anEvaluationMinimum = "0.0"; break; }

			//NUMERIC
			case AVERAGE	: 		{ anEvaluationMinimum = Float.toString(theAverage); break; }
			case INVERSE_AVERAGE: 	{ anEvaluationMinimum = Float.toString(-theAverage); break; }
			case MEAN_TEST  : 		{ anEvaluationMinimum = "0.01"; break; }
			case INVERSE_MEAN_TEST: { anEvaluationMinimum = "0.01"; break; }
			case ABS_MEAN_TEST: 	{ anEvaluationMinimum = "0.01"; break; }
			case Z_SCORE    : 		{ anEvaluationMinimum = "1.0"; break; }
			case INVERSE_Z_SCORE: 	{ anEvaluationMinimum = "1.0"; break; }
			case ABS_Z_SCORE: 		{ anEvaluationMinimum = "1.0"; break; }
			case T_TEST	    : 		{ anEvaluationMinimum = "1.0"; break; }
			case INVERSE_T_TEST : 	{ anEvaluationMinimum = "1.0"; break; }
			case ABS_T_TEST : 		{ anEvaluationMinimum = "1.0"; break; }
			case CHI2_TEST  : 		{ anEvaluationMinimum = "2.5"; break; }
			case HELLINGER  : 		{ anEvaluationMinimum = "0.0"; break; }
			case KULLBACKLEIBLER :	{ anEvaluationMinimum = "0.0"; break; }
			case CWRACC  	: 		{ anEvaluationMinimum = "0.0"; break; }

			//ORDINAL
			case AUC		: 		{ anEvaluationMinimum = "0.5"; break; }
			case WMW_RANKS  : 		{ anEvaluationMinimum = "1.0"; break; }
			case INVERSE_WMW_RANKS: { anEvaluationMinimum = "1.0"; break; }
			case ABS_WMW_RANKS: 	{ anEvaluationMinimum = "1.0"; break; }
			case MMAD   	: 		{ anEvaluationMinimum = "0"; break; }
			//MULTI_LABEL
			case WEED		: 		{ anEvaluationMinimum = "0"; break; }
			case EDIT_DISTANCE: 	{ anEvaluationMinimum = "0"; break; }
			//DOUBLE_CORRELATION
			case CORRELATION_R : 	{ anEvaluationMinimum = "0.2"; break; }
			case CORRELATION_R_NEG :{ anEvaluationMinimum = "0.2"; break; }
			case CORRELATION_R_NEG_SQ :	{ anEvaluationMinimum = "0.2"; break; }
			case CORRELATION_R_SQ :	{ anEvaluationMinimum = "0.2"; break; }
			case CORRELATION_DISTANCE: { anEvaluationMinimum = "0.0"; break; }
			case CORRELATION_P : 	{ anEvaluationMinimum = "0.0"; break; }
			case CORRELATION_ENTROPY : 	{ anEvaluationMinimum = "0.0"; break; }
			case ADAPTED_WRACC : 	{ anEvaluationMinimum = "0.0"; break; }
			case COSTS_WRACC : { anEvaluationMinimum = "0.0"; break; }
			case LINEAR_REGRESSION :{ anEvaluationMinimum = "0.0"; break; }
			case COOKS_DISTANCE	:	{ anEvaluationMinimum = "0.0"; break; }
		}
		return anEvaluationMinimum;
	}

	public static String getMeasureString(int aEvaluationMeasure)
	{
		String anEvaluationMeasure = new String();
		switch(aEvaluationMeasure)
		{
			//NOMINAL
			case WRACC	: { anEvaluationMeasure = "WRAcc"; break; }
			case ABSWRACC : { anEvaluationMeasure = "Abs WRAcc"; break; }
			case CHI_SQUARED: { anEvaluationMeasure = "Chi-squared"; break; }
			case INFORMATION_GAIN: { anEvaluationMeasure = "Information gain"; break; }
			case BINOMIAL: { anEvaluationMeasure = "Binomial test"; break; }
			case JACCARD	: { anEvaluationMeasure = "Jaccard"; break; }
			case COVERAGE	: { anEvaluationMeasure = "Coverage"; break; }
			case ACCURACY	: { anEvaluationMeasure = "Accuracy"; break; }
			case SPECIFICITY: { anEvaluationMeasure = "Specificity"; break; }
			case SENSITIVITY: { anEvaluationMeasure = "Sensitivity"; break; }
			case PURITY		: { anEvaluationMeasure = "Purity"; break; }
			case LAPLACE	: { anEvaluationMeasure = "Laplace"; break; }
			case F_MEASURE	: { anEvaluationMeasure = "F-measure"; break; }
			case G_MEASURE	: { anEvaluationMeasure = "G-measure"; break; }
			case CORRELATION: { anEvaluationMeasure = "Correlation"; break; }
			case PROP_SCORE_WRACC: { anEvaluationMeasure = "Propensity score wracc"; break; }
			case PROP_SCORE_RATIO: { anEvaluationMeasure = "Propensity score ratio"; break; }
			case BAYESIAN_SCORE: { anEvaluationMeasure = "Bayesian Score"; break; }
			//NUMERIC
			case AVERAGE	: { anEvaluationMeasure = "Average"; break; }
			case INVERSE_AVERAGE: { anEvaluationMeasure = "Inverse Average"; break; }
			case MEAN_TEST	: { anEvaluationMeasure = "Mean Test"; break; }
			case INVERSE_MEAN_TEST: { anEvaluationMeasure = "Inverse Mean Test"; break; }
			case ABS_MEAN_TEST: { anEvaluationMeasure = "Abs Mean Test"; break; }
			case Z_SCORE	: { anEvaluationMeasure = "Z-Score"; break; }
			case INVERSE_Z_SCORE: { anEvaluationMeasure = "Inverse Z-Score"; break; }
			case ABS_Z_SCORE: { anEvaluationMeasure = "Abs Z-Score"; break; }
			case T_TEST	: { anEvaluationMeasure = "t-Test"; break; }
			case INVERSE_T_TEST: { anEvaluationMeasure = "Inverse t-Test"; break; }
			case ABS_T_TEST: { anEvaluationMeasure = "Abs t-Test"; break; }
			case CHI2_TEST	: { anEvaluationMeasure = "Median Chi-squared test"; break; }
			case HELLINGER : { anEvaluationMeasure = "Squared Hellinger distance"; break; }
			case KULLBACKLEIBLER : { anEvaluationMeasure = "Kullback-Leibler divergence"; break; }
			case CWRACC : { anEvaluationMeasure = "CWRAcc"; break; }

			//ORDINAL
			case AUC		: { anEvaluationMeasure = "AUC of ROC"; break; }
			case WMW_RANKS	: { anEvaluationMeasure = "WMW-Ranks test"; break; }
			case INVERSE_WMW_RANKS: { anEvaluationMeasure = "Inverse WMW-Ranks test"; break; }
			case ABS_WMW_RANKS: { anEvaluationMeasure = "Abs WMW-Ranks test"; break; }
			case MMAD       : { anEvaluationMeasure = "Median MAD metric"; break; }
			//MULTI_LABEL
			case WEED		: { anEvaluationMeasure = "Wtd Ent Edit Dist"; break; }
			case EDIT_DISTANCE : { anEvaluationMeasure = "Edit Distance"; break; }
			//DOUBLE_CORRELATION
			case CORRELATION_R		: { anEvaluationMeasure = "r"; break; }
			case CORRELATION_R_NEG		: { anEvaluationMeasure = "Negative r"; break; }
			case CORRELATION_R_NEG_SQ		: { anEvaluationMeasure = "Neg Sqr r"; break; }
			case CORRELATION_R_SQ		: { anEvaluationMeasure = "Squared r"; break; }
			case CORRELATION_DISTANCE		: { anEvaluationMeasure = "Distance"; break; }
			case CORRELATION_P		: { anEvaluationMeasure = "p-Value Distance"; break; }
			case CORRELATION_ENTROPY		: { anEvaluationMeasure = "Wtd Ent Distance"; break; }
			case ADAPTED_WRACC		: { anEvaluationMeasure = "Adapted WRAcc"; break; }
			case COSTS_WRACC		: { anEvaluationMeasure = "Costs WRAcc"; break; }
			case LINEAR_REGRESSION		: { anEvaluationMeasure = "Significance of Slope Difference"; break; }
			case COOKS_DISTANCE			: { anEvaluationMeasure = "Cook's Distance"; break; }
		}
		return anEvaluationMeasure;
	}

	public static int getMeasureCode(String theEvaluationMeasure)
	{
		String anEvaluationMeasure = theEvaluationMeasure.toLowerCase().trim();
		//NOMINAL
		if ("wracc".equals(anEvaluationMeasure)) return WRACC;
		else if ("abs wracc".equals(anEvaluationMeasure)) return ABSWRACC;
		else if ("chi-squared".equals(anEvaluationMeasure)) return CHI_SQUARED;
		else if ("information gain".equals(anEvaluationMeasure)) return INFORMATION_GAIN;
		else if ("binomial test".equals(anEvaluationMeasure)) return BINOMIAL;
		else if ("coverage".equals(anEvaluationMeasure)) return COVERAGE;
		else if ("jaccard".equals(anEvaluationMeasure)) return JACCARD;
		else if ("accuracy".equals(anEvaluationMeasure)) return ACCURACY;
		else if ("specificity".equals(anEvaluationMeasure)) return SPECIFICITY;
		else if ("sensitivity".equals(anEvaluationMeasure)) return SENSITIVITY;
		else if ("purity".equals(anEvaluationMeasure)) return PURITY;
		else if ("laplace".equals(anEvaluationMeasure)) return LAPLACE;
		else if ("f-measure".equals(anEvaluationMeasure)) return F_MEASURE;
		else if ("g-measure".equals(anEvaluationMeasure)) return G_MEASURE;
		else if ("correlation".equals(anEvaluationMeasure)) return CORRELATION;
		else if ("propensity score wracc".equals(anEvaluationMeasure)) return PROP_SCORE_WRACC;
		else if ("propensity score ratio".equals(anEvaluationMeasure)) return PROP_SCORE_RATIO;
		else if ("bayesian score".equals(anEvaluationMeasure)) return BAYESIAN_SCORE;
		//NUMERIC
		else if ("average".equals(anEvaluationMeasure)) return AVERAGE;
		else if ("inverse average".equals(anEvaluationMeasure)) return INVERSE_AVERAGE;
		else if ("mean test".equals(anEvaluationMeasure)) return MEAN_TEST;
		else if ("inverse mean test".equals(anEvaluationMeasure)) return INVERSE_MEAN_TEST;
		else if ("abs mean test".equals(anEvaluationMeasure)) return ABS_MEAN_TEST;
		else if ("z-score".equals(anEvaluationMeasure)) return Z_SCORE;
		else if ("inverse z-score".equals(anEvaluationMeasure)) return INVERSE_Z_SCORE;
		else if ("abs z-score".equals(anEvaluationMeasure)) return ABS_Z_SCORE;
		else if ("t-test".equals(anEvaluationMeasure)) return T_TEST;
		else if ("inverse t-test".equals(anEvaluationMeasure)) return INVERSE_T_TEST;
		else if ("abs t-test".equals(anEvaluationMeasure)) return ABS_T_TEST;
		else if ("median chi-squared test".equals(anEvaluationMeasure)) return CHI2_TEST;
		else if ("squared hellinger distance".equals(anEvaluationMeasure)) return HELLINGER;
		else if ("kullback-leibler divergence".equals(anEvaluationMeasure)) return KULLBACKLEIBLER;
		else if ("cwracc".equals(anEvaluationMeasure)) return CWRACC;
		//ORDINAL
		else if ("auc of roc".equals(anEvaluationMeasure)) return AUC;
		else if ("wmw-ranks test".equals(anEvaluationMeasure)) return WMW_RANKS;
		else if ("inverse wmw-ranks test".equals(anEvaluationMeasure)) return INVERSE_WMW_RANKS;
		else if ("abs wmw-ranks test".equals(anEvaluationMeasure)) return ABS_WMW_RANKS;
		else if ("median mad metric".equals(anEvaluationMeasure)) return MMAD;
		//MULTI_LABEL
		else if ("wtd ent edit dist".equals(anEvaluationMeasure)) return WEED;
		else if ("edit distance".equals(anEvaluationMeasure)) return EDIT_DISTANCE;
		//DOUBLE_CORRELATION
		else if ("r".equals(anEvaluationMeasure)) return CORRELATION_R;
		else if ("negative r".equals(anEvaluationMeasure)) return CORRELATION_R_NEG;
		else if ("neg sqr r".equals(anEvaluationMeasure)) return CORRELATION_R_NEG_SQ;
		else if ("squared r".equals(anEvaluationMeasure)) return CORRELATION_R_SQ;
		else if ("distance".equals(anEvaluationMeasure)) return CORRELATION_DISTANCE;
		else if ("p-value distance".equals(anEvaluationMeasure)) return CORRELATION_P;
		else if ("wtd ent distance".equals(anEvaluationMeasure)) return CORRELATION_ENTROPY;
		else if ("adapted wracc".equals(anEvaluationMeasure)) return ADAPTED_WRACC;
		else if ("costs wracc".equals(anEvaluationMeasure)) return COSTS_WRACC;
		//DOUBLE_REGRESSION
		else if ("significance of slope difference".equals(anEvaluationMeasure)) return LINEAR_REGRESSION;
		else if ("cook's distance".equals(anEvaluationMeasure)) return COOKS_DISTANCE;

		return WRACC;
	}


	//Baysian ========================================================================================

	public QualityMeasure(SearchParameters theSearchParameters, DAG theDAG, int theNrRecords)
	{
		itsMeasure = theSearchParameters.getQualityMeasure();
		itsNrRecords = theNrRecords;
		itsDAG = theDAG;
		itsNrNodes = itsDAG.getSize();
		itsAlpha = theSearchParameters.getAlpha();
		itsBeta = theSearchParameters.getBeta();
		itsVStructures = itsDAG.determineVStructures();
	}

	public float calculate(Subgroup theSubgroup)
	{
		switch (itsMeasure)
		{
			case WEED :
				return (float) Math.pow(calculateEntropy(itsNrRecords, theSubgroup.getCoverage()), itsAlpha) *
						(float) Math.pow(calculateEditDistance(theSubgroup.getDAG()), itsBeta);
			case EDIT_DISTANCE :
				return calculateEditDistance(theSubgroup.getDAG());
			default : Log.logCommandLine("QualityMeasure not WEED or EDIT_DISTANCE.");
						return 0.0f; // TODO throw warning
		}
	}

	public float calculateEditDistance(DAG theDAG)
	{
		if (theDAG.getSize() != itsNrNodes)
		{
			Log.logCommandLine("Comparing incompatible DAG's. One has " + theDAG.getSize() + " nodes and the other has " + itsNrNodes + ". Throw pi.");
			return (float) Math.PI;
		}
		int nrEdits = 0;
		for (int i=0; i<itsNrNodes; i++)
			for (int j=0; j<i; j++)
				if ((theDAG.getNode(j).isConnected(i)==0 && itsDAG.getNode(j).isConnected(i)!=0) || (theDAG.getNode(j).isConnected(i)!=0 && itsDAG.getNode(j).isConnected(i)==0))
					nrEdits++;
				else if (theDAG.getNode(j).isConnected(i)==0)
						if (theDAG.testVStructure(j,i) != itsVStructures[j][i])
							nrEdits++;
		return (float) nrEdits / (float) (itsNrNodes*(itsNrNodes-1)/2); // Actually n choose 2, but this boils down to the same...
	}


}
