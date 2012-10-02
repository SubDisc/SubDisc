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

	//Bayesian
	private DAG itsDAG;
	private static int itsNrNodes;
	private static float itsAlpha;
	private static float itsBeta;
	private static boolean[][] itsVStructures;

	//SINGLE_NOMINAL quality measures
	public static final int WRACC     = 0;
	public static final int ABSWRACC  = 1;
	public static final int CHI_SQUARED    = 2;
	public static final int INFORMATION_GAIN    = 3;
	public static final int BINOMIAL    = 4;
	public static final int ACCURACY    = 5;
	public static final int PURITY 		= 6;
	public static final int JACCARD     = 7;
	public static final int COVERAGE    = 8;
	public static final int SPECIFICITY = 9;
	public static final int SENSITIVITY = 10;
	public static final int LAPLACE     = 11;
	public static final int F_MEASURE   = 12;
	public static final int G_MEASURE   = 13;
	public static final int CORRELATION = 14;
	//SINGLE_NUMERIC quality measures
	public static final int Z_SCORE = 15;
	public static final int INVERSE_Z_SCORE = 16;
	public static final int ABS_Z_SCORE = 17;
	public static final int AVERAGE = 18;
	public static final int INVERSE_AVERAGE = 19;
	public static final int MEAN_TEST = 20;
	public static final int INVERSE_MEAN_TEST = 21;
	public static final int ABS_MEAN_TEST = 22;
	public static final int T_TEST = 23;
	public static final int INVERSE_T_TEST = 24;
	public static final int ABS_T_TEST = 25;
	public static final int CHI2_TEST = 26;	// TODO see itsPopulationCounts
	//SINGLE_ORDINAL quality measures
	public static final int AUC = 27;
	public static final int WMW_RANKS = 28;
	public static final int INVERSE_WMW_RANKS = 29;
	public static final int ABS_WMW_RANKS = 30;
	public static final int MMAD = 31;
	//MULTI_LABEL quality measures
	public static final int WEED = 32;
	public static final int EDIT_DISTANCE = 33;
	//DOUBLE_CORRELATION
	public static final int CORRELATION_R = 34;
	public static final int CORRELATION_R_NEG = 35;
	public static final int CORRELATION_R_NEG_SQ = 36;
	public static final int CORRELATION_R_SQ = 37;
	public static final int CORRELATION_DISTANCE = 38;
	public static final int CORRELATION_P = 39;
	public static final int CORRELATION_ENTROPY = 40;
	public static final int ADAPTED_WRACC = 41;
	public static final int COSTS_WRACC = 42;
	//DOUBLE_REGRESSION
	public static final int LINEAR_REGRESSION = 43;
	public static final int COOKS_DISTANCE = 44;

	//SINGLE =========================================================================================

	//SINGLE_NOMINAL
	public QualityMeasure(int theMeasure, int theTotalCoverage, int theTotalTargetCoverage)
	{
		itsMeasure = theMeasure;
		itsNrRecords = theTotalCoverage;
		itsTotalTargetCoverage = theTotalTargetCoverage;
	}

	//SINGLE_NUMERIC
	public QualityMeasure(int theMeasure, int theTotalCoverage, float theTotalSum, float theTotalSSD)
	{
		itsMeasure = theMeasure;
		itsNrRecords = theTotalCoverage;
		if (itsNrRecords > 0)
			itsTotalAverage = theTotalSum/itsNrRecords;
		if (itsNrRecords > 1)
			itsTotalSampleStandardDeviation = Math.sqrt(theTotalSSD/(itsNrRecords-1));
		//itsPopulationCounts = null;	// TODO see itsPopulationCounts
	}

//	public void setTotalTargetCoverage(int theCount) { itsTotalTargetCoverage = theCount; }

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
			case SINGLE_NOMINAL		: return CORRELATION;
			//case SINGLE_NUMERIC		: return CHI2_TEST;	// TODO see itsPopulationCounts
			case SINGLE_NUMERIC		: return ABS_T_TEST;
			case SINGLE_ORDINAL		: return MMAD;
			case MULTI_LABEL		: return EDIT_DISTANCE;
			case DOUBLE_CORRELATION		: return COSTS_WRACC;
			case DOUBLE_REGRESSION		: return LINEAR_REGRESSION;
			// TODO for stable jar, disabled
			//case DOUBLE_REGRESSION 	: return COOKS_DISTANCE;
			default					: return WRACC;
		}
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
			case JACCARD: {		returnValue = theCountHeadBody /(theCountHeadBody + aCountNotHeadBody + aTotalTargetCoverageNotBody);
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
				returnValue = theCountHeadBody /aCountBody;
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

	public float calculate(int theCoverage, float theSum, float theSSD,
			float theMedian, float theMedianAD, int[] theSubgroupCounts)
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
