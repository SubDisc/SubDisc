package nl.liacs.subdisc;

import nl.liacs.subdisc.TargetConcept.TargetType;

public class QualityMeasure
{
	int itsMeasure;
	private static int itsNrRecords;

	//SINGLE_NOMINAL
	int itsTotalTargetCoverage;

	//SINGLE_NUMERIC and SINGLE_ORDINAL
	double itsTotalSum;
	double itsTotalSSD;
	double itsTotalMedian;
	double itsTotalMedianAD;
	int[] itsPopulationCounts;

	//Bayesian
	private static DAG itsDAG;
	private static int itsNrNodes;
	private static float itsAlpha;
	private static float itsBeta;
	private static boolean[][] itsVStructures;

	//SINGLE_NOMINAL quality measures
	public static final int NOVELTY     = 0;
	public static final int ABSNOVELTY  = 1;
	public static final int CHI_SQUARED    = 2;
	public static final int INFORMATION_GAIN    = 3;
	public static final int ACCURACY    = 4;
	public static final int PURITY 		= 5;
	public static final int JACCARD     = 6;
	public static final int COVERAGE    = 7;
	public static final int SPECIFICITY = 8;
	public static final int SENSITIVITY = 9;
	//SINGLE_NUMERIC quality measures
	public static final int Z_SCORE = 10;
	public static final int INVERSE_Z_SCORE = 11;
	public static final int ABS_Z_SCORE = 12;
	public static final int AVERAGE = 13;
	public static final int INVERSE_AVERAGE = 14;
	public static final int MEAN_TEST = 15;
	public static final int INVERSE_MEAN_TEST = 16;
	public static final int ABS_MEAN_TEST = 17;
	public static final int T_TEST = 18;
	public static final int INVERSE_T_TEST = 19;
	public static final int ABS_T_TEST = 20;
	public static final int CHI2_TEST = 21;
	//SINGLE_ORDINAL quality measures
	public static final int AUC = 22;
	public static final int WMW_RANKS = 23;
	public static final int INVERSE_WMW_RANKS = 24;
	public static final int ABS_WMW_RANKS = 25;
	public static final int MMAD = 26;
	//MULTI_LABEL quality measures
	public static final int WEED = 27;
	public static final int EDIT_DISTANCE = 28;
	//DOUBLE_CORRELATION
	public static final int CORRELATION_R = 29;
	public static final int CORRELATION_R_NEG = 30;
	public static final int CORRELATION_R_NEG_SQ = 31;
	public static final int CORRELATION_R_SQ = 32;
	public static final int CORRELATION_DISTANCE = 33;
	public static final int CORRELATION_P = 34;
	public static final int CORRELATION_ENTROPY = 35;
	//DOUBLE_REGRESSION
	public static final int LINEAR_REGRESSION = 36;



	//SINGLE =========================================================================================

	public QualityMeasure(int theMeasure, int theTotalCoverage, int theTotalTargetCoverage)
	{
		itsMeasure = theMeasure;
		itsNrRecords = theTotalCoverage;
		itsTotalTargetCoverage = theTotalTargetCoverage;
	}

	public QualityMeasure(int theMeasure, int theTotalCoverage, int theTotalTargetCoverage,
					double theTotalSum, double theTotalSSD, double theTotalMedian,
					double theTotalMedianAD, int[] thePopulationCounts)
	{
		itsMeasure = theMeasure;
		itsNrRecords = theTotalCoverage;
		itsTotalTargetCoverage = theTotalTargetCoverage;
		itsTotalSum = theTotalSum;
		itsTotalSSD = theTotalSSD;
		itsTotalMedian = theTotalMedian;
		itsTotalMedianAD = theTotalMedianAD;
		itsPopulationCounts = thePopulationCounts;
	}

	public void setTotalTargetCoverage(int theCount) { itsTotalTargetCoverage = theCount; }
	public void setQualityMeasure(int theMeasure) { itsMeasure = theMeasure; }

	public static int getFirstEvaluationMesure(TargetType theTargetType)
	{
		if(theTargetType == null)
			return NOVELTY;	// MM TODO for now

		switch(theTargetType)
		{
			case SINGLE_NOMINAL		: return NOVELTY;
			case SINGLE_NUMERIC		: return Z_SCORE;
			case SINGLE_ORDINAL		: return AUC;
			case MULTI_LABEL		: return WEED;
			case DOUBLE_CORRELATION	: return CORRELATION_R;
			case DOUBLE_REGRESSION 	: return LINEAR_REGRESSION;
			default					: return NOVELTY;
		}
	}

	public static int getLastEvaluationMesure(TargetType theTargetType)
	{
		if(theTargetType == null)
			return NOVELTY;	// MM TODO for now

		switch(theTargetType)
		{
			case SINGLE_NOMINAL		: return SENSITIVITY;
			case SINGLE_NUMERIC		: return CHI2_TEST;
			case SINGLE_ORDINAL		: return MMAD;
			case MULTI_LABEL		: return EDIT_DISTANCE;
			case DOUBLE_CORRELATION	: return CORRELATION_ENTROPY;
			case DOUBLE_REGRESSION	: return LINEAR_REGRESSION;
			default					: return NOVELTY;
		}
	}

	public float calculate(int theCountHeadBody, int theCoverage)
	{
		return calculate(itsMeasure, itsNrRecords, itsTotalTargetCoverage, theCountHeadBody, theCoverage);
	}

	//SINGLE_NOMINAL ============================================================

	public static float calculate(int theMeasure, int theTotalCoverage, int theTotalTargetCoverage,
							int theCountHeadBody, int theCoverage)
	{
		float aCoverage				= (float)theCoverage;
		float aCountHeadBody		= (float)theCountHeadBody;
		float aCountHead			= (float)theTotalTargetCoverage;
		float aCountNotHeadBody		= aCoverage - aCountHeadBody;
		float aCountHeadNotBody		= aCountHead - aCountHeadBody;
		float aCountNotHeadNotBody	= theTotalCoverage - (aCountHead + aCountNotHeadBody);
		float aCountBody			= aCountNotHeadBody + aCountHeadBody;

		float returnValue = (float)-10.0; //Bad measure value for default
		switch(theMeasure)
		{
			case NOVELTY:
			{
				returnValue =(aCountHeadBody / (float)theTotalCoverage) -
							  ((aCountHead / (float)theTotalCoverage) *	(aCountBody/(float)theTotalCoverage));
				break;
			}
			case CHI_SQUARED:
			{
				returnValue = calculateChiSquared(theTotalCoverage, aCountHead, aCountBody, aCountHeadBody);
				break;
			}
			case INFORMATION_GAIN:
			{
				returnValue = calculateInformationGain(theTotalCoverage, aCountHead, aCountBody, aCountHeadBody);
				break;
			}
			case JACCARD: {		returnValue = aCountHeadBody /(aCountHeadBody + aCountNotHeadBody + aCountHeadNotBody);
								break; }
			case COVERAGE: {	returnValue = aCountBody;
								break; }
			case ACCURACY: {	returnValue = aCountHeadBody /aCountBody;
								break; }
			case SPECIFICITY: {	returnValue = aCountNotHeadNotBody / ((float)theTotalCoverage - aCountHead);
								break; }
			case SENSITIVITY: {	returnValue = aCountHeadBody / aCountHead;
								break; }
			case PURITY:
			{
				returnValue = aCountHeadBody /aCountBody;
				if (returnValue < 0.5)
					returnValue = 1.0F - returnValue;
				break;
			}
			case ABSNOVELTY:
			{
				returnValue =(aCountHeadBody / (float)theTotalCoverage) -
							  ((aCountHead / (float)theTotalCoverage) * (aCountBody / (float)theTotalCoverage));
				returnValue = java.lang.Math.abs(returnValue);
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

	public static float calculateEntropy(float bodySupport, float headBodySupport)
	{
		if (headBodySupport==0 || bodySupport==headBodySupport)
			return 0;
		float pj = headBodySupport/bodySupport;
		return (float)((-1 * pj * Math.log(pj)/Math.log(2)) - ((1 - pj) * Math.log(1 - pj)/Math.log(2)));
	}

	public static float calculateConditionalEntropy(float totalSupport, float headSupport, float bodySupport, float bodyHeadSupport)
	{
		float Phb = bodyHeadSupport/bodySupport; //P(H|B)
		float Pnhb = (bodySupport - bodyHeadSupport)/bodySupport; //P(H|B)
		float quality = (float)(-1 * ((Phb * Math.log(Phb)/Math.log(2)) + (Pnhb * Math.log(Pnhb)/Math.log(2))));
		return quality;
	}

	public static float calculateInformationGain(float totalSupport, float headSupport, float bodySupport, float headBodySupport)
	{
		return calculateEntropy(totalSupport, headSupport) - calculateConditionalEntropy(totalSupport, headSupport, bodySupport, headBodySupport);
	}



	//SINGLE_NUMERIC ===============================================

	public double calculate(int theMeasure, int theCoverage, double theSum, double theSSD,
			double theMedian, double theMedianAD, int[] theSubgroupCounts)
	{
		double aReturn = Double.NEGATIVE_INFINITY;
		switch(theMeasure)
		{
			//NUMERIC
			case AVERAGE			: { aReturn = theSum/theCoverage; break; }
			case INVERSE_AVERAGE	: { aReturn = -theSum/theCoverage; break; }
			case MEAN_TEST	: { aReturn = Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalSum/itsNrRecords); break; }
			case INVERSE_MEAN_TEST	: { aReturn = -(Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalSum/itsNrRecords)); break; }
			case ABS_MEAN_TEST	: { aReturn = Math.abs(Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalSum/itsNrRecords)); break; }
			case Z_SCORE	: { if(itsNrRecords <= 1) aReturn = 0;
						else aReturn = (Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalSum/itsNrRecords))/Math.sqrt(itsTotalSSD/(itsNrRecords-1)); break; }
			case INVERSE_Z_SCORE	: { if(itsNrRecords <= 1) aReturn = 0;
								else aReturn = -((Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalSum/itsNrRecords))/Math.sqrt(itsTotalSSD/(itsNrRecords-1))); break; }
			case ABS_Z_SCORE	: { if(itsNrRecords <= 1) aReturn = 0;
								else aReturn = Math.abs((Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalSum/itsNrRecords))/Math.sqrt(itsTotalSSD/(itsNrRecords-1))); break; }
			case T_TEST		: { if(theCoverage <= 1) aReturn = 0;
								else aReturn = (Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalSum/itsNrRecords))/Math.sqrt(theSSD/(theCoverage-1)); break; }
			case INVERSE_T_TEST	: { if(theCoverage <= 1) aReturn = 0;
								else aReturn = -((Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalSum/itsNrRecords))/Math.sqrt(theSSD/(theCoverage-1))); break; }
			case ABS_T_TEST		: { if(theCoverage <= 1) aReturn = 0;
							else aReturn = Math.abs((Math.sqrt(theCoverage)*(theSum/theCoverage-itsTotalSum/itsNrRecords))/Math.sqrt(theSSD/(theCoverage-1))); break; }
			case CHI2_TEST	:
			{
				double a = ((theSubgroupCounts[0]-itsPopulationCounts[0])*(theSubgroupCounts[0]-itsPopulationCounts[0]))/itsPopulationCounts[0];
				double b = ((theSubgroupCounts[1]-itsPopulationCounts[1])*(theSubgroupCounts[1]-itsPopulationCounts[1]))/itsPopulationCounts[1];
				aReturn = a+b; break;
			}
			//ORDINAL
			case AUC				:
			{
				double aComplementCoverage = itsNrRecords - theCoverage;
				double aSequenceSum = theCoverage*(theCoverage+1)/2; //sum of all positive ranks, assuming ideal case
				aReturn = 1 + (aSequenceSum-theSum)/(theCoverage*aComplementCoverage);
				break;
			}
			case WMW_RANKS :
			{
				double aComplementCoverage = itsNrRecords - theCoverage;
				double aMean = (theCoverage*(theCoverage+aComplementCoverage+1))/2;
				double aStDev = Math.sqrt((theCoverage*aComplementCoverage*(theCoverage+aComplementCoverage+1))/12);
				aReturn = (theSum-aMean)/aStDev; break;
			}
			case INVERSE_WMW_RANKS :
			{
				double aComplementCoverage = itsNrRecords - theCoverage;
				double aMean = (theCoverage*(theCoverage+aComplementCoverage+1))/2;
				double aStDev = Math.sqrt((theCoverage*aComplementCoverage*(theCoverage+aComplementCoverage+1))/12);
				aReturn = -((theSum-aMean)/aStDev); break;
			}
			case ABS_WMW_RANKS :
			{
				double aComplementCoverage = itsNrRecords - theCoverage;
				double aMean = (theCoverage*(theCoverage+aComplementCoverage+1))/2;
				double aStDev = Math.sqrt((theCoverage*aComplementCoverage*(theCoverage+aComplementCoverage+1))/12);
				aReturn = Math.abs((theSum-aMean)/aStDev); break;
			}
			case MMAD : { aReturn = (theCoverage/(2*theMedian+theMedianAD)); break; }
		}
		return aReturn;
	}

	//==========================================

	public static String getMeasureMinimum(String theEvaluationMeasure, double theAverage)
	{
		String anEvaluationMinimum = new String();
		switch(getMeasureCode(theEvaluationMeasure))
		{
			//NOMINAL
			case NOVELTY	: 		{ anEvaluationMinimum = "0.01"; break; }
			case ABSNOVELTY : 		{ anEvaluationMinimum = "0.01"; break; }
			case CHI_SQUARED: 		{ anEvaluationMinimum = "50"; break; }
			case INFORMATION_GAIN: 	{ anEvaluationMinimum = "0.2"; break; }
			case JACCARD	: 		{ anEvaluationMinimum = "0.2"; break; }
			case COVERAGE	: 		{ anEvaluationMinimum = "10"; break; }
			case ACCURACY	: 		{ anEvaluationMinimum = "0.8"; break; }
			case SPECIFICITY: 		{ anEvaluationMinimum = "0.8"; break; }
			case SENSITIVITY: 		{ anEvaluationMinimum = "0.8"; break; }
			case PURITY		: 		{ anEvaluationMinimum = "0.8"; break; }
			//NUMERIC
			case AVERAGE	: 		{ anEvaluationMinimum = Double.toString(theAverage); break; }
			case INVERSE_AVERAGE: 	{ anEvaluationMinimum = Double.toString(-theAverage); break; }
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
			case CORRELATION_R : 	{ anEvaluationMinimum = "0.8"; break; }
			case CORRELATION_R_NEG :{ anEvaluationMinimum = "0.8"; break; }
			case CORRELATION_R_NEG_SQ :	{ anEvaluationMinimum = "0.8"; break; }
			case CORRELATION_R_SQ :	{ anEvaluationMinimum = "0.8"; break; }
			case CORRELATION_DISTANCE: { anEvaluationMinimum = "0.0"; break; }
			case CORRELATION_P : 	{ anEvaluationMinimum = "0.0"; break; }
			case CORRELATION_ENTROPY : 	{ anEvaluationMinimum = "0.8"; break; }
			case LINEAR_REGRESSION :{ anEvaluationMinimum = "0.0"; break; }
		}
		return anEvaluationMinimum;
	}

	public static String getMeasureString(int aEvaluationMeasure)
	{
		String anEvaluationMeasure = new String();
		switch(aEvaluationMeasure)
		{
			//NOMINAL
			case NOVELTY	: { anEvaluationMeasure = "WRAcc"; break; }
			case ABSNOVELTY : { anEvaluationMeasure = "Abs WRAcc"; break; }
			case CHI_SQUARED: { anEvaluationMeasure = "Chi-squared"; break; }
			case INFORMATION_GAIN: { anEvaluationMeasure = "Information gain"; break; }
			case JACCARD	: { anEvaluationMeasure = "Jaccard"; break; }
			case COVERAGE	: { anEvaluationMeasure = "Coverage"; break; }
			case ACCURACY	: { anEvaluationMeasure = "Accuracy"; break; }
			case SPECIFICITY: { anEvaluationMeasure = "Specificity"; break; }
			case SENSITIVITY: { anEvaluationMeasure = "Sensitivity"; break; }
			case PURITY		: { anEvaluationMeasure = "Purity"; break; }
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
			case LINEAR_REGRESSION		: { anEvaluationMeasure = "Linear Regression"; break; }
		}
		return anEvaluationMeasure;
	}

	public static int getMeasureCode(String theEvaluationMeasure)
	{
		String anEvaluationMeasure = theEvaluationMeasure.toLowerCase().trim();
		//NOMINAL
		if ("wracc".equals(anEvaluationMeasure)) return NOVELTY;
		else if ("abs wracc".equals(anEvaluationMeasure)) return ABSNOVELTY;
		else if ("chi-squared".equals(anEvaluationMeasure)) return CHI_SQUARED;
		else if ("information gain".equals(anEvaluationMeasure)) return INFORMATION_GAIN;
		else if ("coverage".equals(anEvaluationMeasure)) return COVERAGE;
		else if ("jaccard".equals(anEvaluationMeasure)) return JACCARD;
		else if ("accuracy".equals(anEvaluationMeasure)) return ACCURACY;
		else if ("specificity".equals(anEvaluationMeasure)) return SPECIFICITY;
		else if ("sensitivity".equals(anEvaluationMeasure)) return SENSITIVITY;
		else if ("purity".equals(anEvaluationMeasure)) return PURITY;
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
		//DOUBLE_REGRESSION
		else if ("linear regression".equals(anEvaluationMeasure)) return LINEAR_REGRESSION;

		return NOVELTY;
	}


	//Baysian ========================================================================================
	public QualityMeasure(DAG theDAG, int theNrRecords)
	{
		itsDAG = theDAG;
		itsNrNodes = theDAG.getSize();
		itsNrRecords = theNrRecords;
		itsAlpha = 1f;
		itsBeta = 1f;
		itsVStructures = theDAG.determineVStructures();
	}

	public QualityMeasure(DAG theDAG, int theNrRecords, float theAlpha, float theBeta)
	{
		itsDAG = theDAG;
		itsNrNodes = theDAG.getSize();
		itsNrRecords = theNrRecords;
		itsAlpha = theAlpha;
		itsBeta = theBeta;
		itsVStructures = theDAG.determineVStructures();
	}

	public float calculateWEED(Subgroup theSubgroup)
	{
		return (float) Math.pow(calculateEntropy(theSubgroup.getMembers().cardinality()),itsAlpha) *
			   (float) Math.pow(calculateEditDistance(theSubgroup.getDAG()),itsBeta);
	}

	public double calculateEDIT_DISTANCE(Subgroup theSubgroup)
	{
		return calculateEditDistance(theSubgroup.getDAG());
	}

	public float calculateEditDistance(DAG theDAG)
	{
		if (theDAG.getSize() != itsNrNodes)
		{
			Log.logCommandLine("Comparing incompatible DAG's. One has " + theDAG.getSize() + " nodes and the other has " + itsNrNodes + ". Throw pi.");
			return (float) Math.PI;
		}
		int nrEdits = 0;
		for(int i=0; i<itsNrNodes; ++i)
			for(int j=0; j<i ; ++j)
				if ( (theDAG.getNode(j).isConnected(i)==0 && itsDAG.getNode(j).isConnected(i)!=0) || (theDAG.getNode(j).isConnected(i)!=0 && itsDAG.getNode(j).isConnected(i)==0) )
					nrEdits++;
				else if ( theDAG.getNode(j).isConnected(i)==0 )
						if ( theDAG.testVStructure(j,i) != itsVStructures[j][i] )
							nrEdits++;
		return (float) nrEdits / (float) (itsNrNodes*(itsNrNodes-1)/2); // Actually n choose 2, but this boils down to the same...
	}

	public float calculateEntropy(int theSubgroupSupport)
	{
		if (theSubgroupSupport==0 || itsNrRecords==theSubgroupSupport)
			return 0;
		float pj = (float)theSubgroupSupport / (float)itsNrRecords;
		return (float)((-1 * pj * Math.log(pj)/Math.log(2)) - ((1 - pj) * Math.log(1 - pj)/Math.log(2)));
	}
}