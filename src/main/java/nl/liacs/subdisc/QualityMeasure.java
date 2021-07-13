package nl.liacs.subdisc;

import java.util.*;

// TODO MM put Contingency table here without screwing up package classes layout.
/**
 * The QualityMeasure class includes all quality measures used
 * ({@link #calculate(float, float) contingency table}).
 */
public class QualityMeasure
{
	// temporary PDF related code --- leave at false in svn
	private static final boolean TEMPORARY_CODE_SG_VS_COMPLEMENT = false;

	private final QM itsQualityMeasure;
	private final int itsNrRecords;

	//SINGLE_NOMINAL and SCAPE
	private int itsTotalTargetCoverage;

	// FIXME change to double
	//SINGLE_NUMERIC and SINGLE_ORDINAL
	private float itsTotalAverage           = Float.NaN;
	private float itsTotalStandardDeviation = Float.NaN;
//	private float itsTotalSSD               = Float.NaN;
	private ProbabilityDensityFunction itsPDF; // pdf for entire dataset

	// XXX MM - why are these static fields
	//MULTI_LABEL (Bayesian)
	private DAG itsDAG;
	private static int itsNrNodes;
	private static float itsAlpha;
	private static float itsBeta;
	private static boolean[][] itsVStructures;

	// XXX MM - why are these static fields
	//SCAPE
	private static Column itsBinaryTarget;
	private static Column itsNumericTarget;
	private static int[] itsDescendingOrderingPermutation;
	private static float itsOverallSubrankingLoss = 0.0f; // MUST BE 0.0f at first call

	//LABEL_RANKING
	private LabelRanking itsAverageRanking = null;
	private LabelRankingMatrix itsAverageRankingMatrix = null;

	//SINGLE_NOMINAL
	// TODO MM also used by DOUBLE_CORRELATION, DOUBLE_REGRESSION and DOUBLE_BINARY
	public QualityMeasure(QM theMeasure, int theTotalCoverage, int theTotalTargetCoverage)
	{
		if (theMeasure == null)
			throw new IllegalArgumentException("QualityMeasure: theMeasure can not be null");
//		if (!QM.getQualityMeasures(TargetType.SINGLE_NOMINAL).contains(theMeasure))
//			throw new IllegalArgumentException("QualityMeasure: not a SINGLE_NOMINAL measure");
		// TODO MM - DOUBLE_CORRELATION + DOUBLE_CORRELATION constructor
		if (!QM.getQualityMeasures(TargetType.SINGLE_NOMINAL).contains(theMeasure) &&
			!QM.getQualityMeasures(TargetType.DOUBLE_REGRESSION).contains(theMeasure) &&
            !QM.getQualityMeasures(TargetType.DOUBLE_CORRELATION).contains(theMeasure) &&
            !QM.getQualityMeasures(TargetType.DOUBLE_BINARY).contains(theMeasure))
			throw new IllegalArgumentException("QualityMeasure: not a SINGLE_NOMINAL, DOUBLE_CORRELATION, DOUBLE_REGRESSION or DOUBLE_BINARY measure");
		// N > 0
		if (theTotalCoverage <= 0)
			throw new IllegalArgumentException("QualityMeasure: theTotalCoverage must be > 0");
		// H > 0
		if (theTotalTargetCoverage <= 0)
			throw new IllegalArgumentException("QualityMeasure: theTotalTargetCoverage must be > 0");
		// H <= N
		if (theTotalCoverage < theTotalTargetCoverage)
			throw new IllegalArgumentException("QualityMeasure: theTotalCoverage < theTotalTargetCoverage");

		itsQualityMeasure = theMeasure;
		itsNrRecords = theTotalCoverage;
		itsTotalTargetCoverage = theTotalTargetCoverage;
	}

	//SINGLE_NUMERIC
	public QualityMeasure(QM theMeasure, int theTotalCoverage, float theTotalSum, float theTotalSSD, ProbabilityDensityFunction theDataPDF)
	{
		if (theMeasure == null)
			throw new IllegalArgumentException("QualityMeasure: theMeasure can not be null");
		if (!QM.getQualityMeasures(TargetType.SINGLE_NUMERIC).contains(theMeasure))
			throw new IllegalArgumentException("QualityMeasure: not a SINGLE_NUMERIC measure");
		if (theTotalCoverage <= 0)
			throw new IllegalArgumentException("QualityMeasure: theTotalCoverage must be > 0");
		if (Float.isNaN(theTotalSum) || Float.isInfinite(theTotalSum))
			throw new IllegalArgumentException("QualityMeasure: theTotalSum can not be NaN or (-)Infinity");
		if (Float.isNaN(theTotalSSD) || Float.isInfinite(theTotalSSD) || theTotalSSD < 0.0f)
			throw new IllegalArgumentException("QualityMeasure: theTotalSSD can not be NaN, (-)Infinity, or < 0.0f");
		if (theDataPDF == null)
			throw new IllegalArgumentException("QualityMeasure: theDataPDF can not be null");

		itsQualityMeasure = theMeasure;
		itsNrRecords = theTotalCoverage;
		itsTotalAverage = theTotalSum/itsNrRecords;
//		itsTotalSSD = theTotalSSD;
		itsTotalStandardDeviation = (float)Math.sqrt(theTotalSSD/itsNrRecords);
		itsPDF = theDataPDF;
	}

	//LABEL_RANKING
	public QualityMeasure(QM theMeasure, int theTotalCoverage, LabelRanking theAverageRanking, LabelRankingMatrix theAverageRankingMatrix)
	{
		if (theMeasure == null)
			throw new IllegalArgumentException("QualityMeasure: theMeasure can not be null");
		if (!QM.getQualityMeasures(TargetType.LABEL_RANKING).contains(theMeasure))
			throw new IllegalArgumentException("QualityMeasure: not a LabelRanking measure");
		if (theTotalCoverage <= 0)
			throw new IllegalArgumentException("QualityMeasure: theTotalCoverage must be > 0");
		if (theAverageRanking == null)
			throw new IllegalArgumentException("QualityMeasure: theAverageRanking can not be null");
		if (theAverageRankingMatrix == null)
			throw new IllegalArgumentException("QualityMeasure: theAverageRankingMatrix can not be null");

		itsQualityMeasure = theMeasure;
		itsNrRecords = theTotalCoverage;
		itsAverageRanking = theAverageRanking;
		itsAverageRankingMatrix = theAverageRankingMatrix;
		Log.logCommandLine("average ranking of entire dataset:");
		itsAverageRankingMatrix.print();
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
	 * <p>
	 *
	 * Where:
	 * <ul>
	 * <li>n(H) = TotalTargetCoverage (of the data),</li>
	 * <li>n(B) = Coverage (subgroup coverage),</li>
	 * <li>N = TotalCoverage (number of rows in the data).</li>
	 * </ul>
	 * 
	 * IllegalArgumentExceptions will be thrown when:</br>
	 * (theCountHeadBody < 0),</br>
	 * (theCoverage <= 0),</br>
	 * (theCountHeadBody > theCoverage),</br>
	 * (theCountHeadBody > theTotalTargetCoverage for this QualityMeasure),</br>
	 * (theCoverage > theTotalCoverage for this QualityMeasure).
	 */
	/* TODO MM
	 * setup of class forces switch(QM) on each call, useless expense
	 */
//	public float calculate(float theCountHeadBody, float theCoverage)
//	{
//		// check (theCountHeadBody <= theCoverage)
//		// check (theCountHeadBody <= itsTotalTargetCoverage)
//		// check (theCoverage <= itsNrRecords)
//
//		float aResult = calculate(itsQualityMeasure, itsNrRecords, itsTotalTargetCoverage, theCountHeadBody, theCoverage);
//		if (Float.isNaN(aResult)) // FIXME MM this does not seem wise, see comment below
//			return 0.0f;
//		else
//			return aResult;
//	}
	/*
	 * the following conditions must hold (lower and upper bounds)
	 * N > 0    : can not have empty data
	 * N <= MAX : can not have data size > MAX_ARRAY_SIZE (JVM specific)
	 * H > 0    : can not have data with 0 target records	// FIXME MM change to H >= 0
	 * H <= N   : can not have more target records than data size
	 * B > 0    : can not have empty subgroup
	 * B <= N   : can not have subgroup size bigger than data size
	 * HB >= 0  : can not have negative subgroup size #
	 * HB >= B-(N-H) <--> HB >= B-!H <--> (B-HB) <= (N-H)
	 *          : a subgroup size > !H selects at least X positives
	 *            where X = B-(N-H) = B-!H
	 * HB <= H  : can not have more positives then target records 
	 * HB <= B  : can not have more positives then subgroup size
	 *
	 * # 0 positives in subgroup is allowed, selecting all positives
	 *   or all negatives would be valued equally by a symmetric QM
	 *
	 * NOTE
	 * getROCHell() would fail check 'B > 0'
	 * it is a special case, not related to data mining
	 * therefore it can call calculate(QM, N, H, HB, B) directly
	 */
	public double calculate(int theCountHeadBody, int theCoverage)
	{
		// checked by constructor, N and H are constant/ fixed: asserts
		assert (itsNrRecords > 0);
		//assert (itsNrRecords <= Integer.MAX_VALUE-8); // JVM specific
		assert (itsTotalTargetCoverage > 0);
		assert (itsTotalTargetCoverage <= itsNrRecords);

		// HB and B are free parameters for this public method
		// so use IllegalArgumentExceptions instead of asserts
		if (theCoverage <= 0)
			throw new IllegalArgumentException("QualityMeasure: theCoverage <= 0");
		if (theCoverage > itsNrRecords)
			throw new IllegalArgumentException("QualityMeasure: theCoverage > itsNrRecords");
		if (theCountHeadBody < 0)
			throw new IllegalArgumentException("QualityMeasure: theCountHeadBody < 0");
		if (theCountHeadBody < (theCoverage - (itsNrRecords - itsTotalTargetCoverage)))
			throw new IllegalArgumentException("QualityMeasure: theCountHeadBody < theCoverage - (itsNrRecords - itsTotalTargetCoverage)");
		if (theCountHeadBody > itsTotalTargetCoverage)
			throw new IllegalArgumentException("QualityMeasure: theCountHeadBody > itsTotalTargetCoverage");
		if (theCountHeadBody > theCoverage)
			throw new IllegalArgumentException("QualityMeasure: theCountHeadBody > theCoverage");

		return calculate(itsQualityMeasure, itsNrRecords, itsTotalTargetCoverage, theCountHeadBody, theCoverage);
	}

	//SINGLE_NOMINAL =======================================================
	/*
	 * int counts are cast to double inside this method. Float should not be used to represent int counts
	 * 
	 * NOTE each case should check the input / result to avoid NaN returns
	 */
	private static final double calculate(QM theMeasure, int theTotalCoverage, int theTotalTargetCoverage, int theCountHeadBody, int theCoverage)
	{
		// FIXME MM if (H==N) -> return 0.0; (or QM specific value)

		// should be checked by constructor
		assert (!(theMeasure == null));
		assert (theTotalCoverage > 0);
		//assert (theTotalCoverage <= Integer.MAX_VALUE-8); // JVM specific

		//check whether the target value is the only value, or whether it never occurs (not possible through GUI, but perhaps otherwise)
		if (theTotalTargetCoverage == theTotalCoverage || theTotalTargetCoverage == 0)
			return 0.0;	// perhaps not an ideal number for some QMs, but better than throwing an exception.

		// should be checked by calculate(int, int)
		//assert (theCoverage > 0); // TODO MM disabled for getROCHell()
		assert (theCoverage <= theTotalCoverage);
		assert (theCountHeadBody >= 0);
		assert (theCountHeadBody >= (theCoverage - (theTotalCoverage - theTotalTargetCoverage)));
		assert (theCountHeadBody <= theTotalTargetCoverage);
		assert (theCountHeadBody <= theCoverage);

		// redefine, use double instead of float, see comment above
		double N = theTotalCoverage;		// constant
		double H = theTotalTargetCoverage;	// constant, Head
		double B = theCoverage;			// Body
		//
		double HB = theCountHeadBody;		// HeadBody
		double nHB = B - HB;			// NotHeadBody
		double HnB = H - HB;			// HeadNotBody
		double nHnB = (N - H) - nHB;		// NotHeadNotBody
		// nHnB is equivalent to:
		// double nHnB = N-HB-HnB-nHB;
		// but N and H are constant and could be cached

		double returnValue = Double.NaN;
		switch (theMeasure)
		{
			case WRACC:
			{
				returnValue = (HB/N) - (H/N) * (B/N);
				break;
			}
			case ABSWRACC:
			{
				returnValue = Math.abs((HB/N) - (H/N) * (B/N));
				break;
			}
			// TODO MM check return value is not NaN
			case MUTUAL_INFORMATION:
			{
				double p_HB = HB / N;
				double p_nHB = nHB / N;
				double p_HnB = HnB / N;
				double p_nHnB = nHnB / N;

				returnValue =
				(
					mi(p_HB, p_HnB, p_nHB) +
					mi(p_nHB, p_nHnB, p_HB) +
					mi(p_HnB, p_HB, p_nHnB) +
					mi(p_nHnB, p_nHB, p_HnB)
				);
				/*
				System.out.println("*********************************************************");
				System.out.println("   N = " + N);
				System.out.println("   H = " + H);
				System.out.println("   B = " + B);
				System.out.println();
				System.out.println("  (HB)/N = " + p_HB);
				System.out.println(" (!HB)/N = " + p_nHB);
				System.out.println(" (H!B)/N = " + p_HnB);
				System.out.println("(!H!B)/N = " + p_nHnB);
				System.out.println(" SUM = " + (p_HB + p_nHB + p_HnB + p_nHnB));
				System.out.println("   MI= " + returnValue);
				System.out.println("WRACC= " + calculate(QM.WRACC, theTotalCoverage, theTotalTargetCoverage, theCountHeadBody, theCoverage));
				System.out.println("*********************************************************");
				*/
				break;
			}
			case CORTANA_QUALITY:
			{
				// FIXME MM if(H=N)->aMaxWRAcc=0->divide_by_zero
				double aQuotient = H/N;
				double aWRAcc = (HB/N) - (aQuotient * (B/N));
				double aMaxWRAcc = aQuotient - (aQuotient*aQuotient);
				returnValue = aWRAcc / aMaxWRAcc;
				break;
			}
			case CHI_SQUARED:
			{
				// TODO MM check return value is not NaN
				returnValue = calculateChiSquared(N, H, B, HB);
				break;
			}
			case INFORMATION_GAIN:
			{
				// TODO MM check return value is not NaN
				returnValue = calculateInformationGain(N, H, B, HB);
				break;
			}
			case BINOMIAL:
			{
				if (B == 0)
					returnValue = 0.0;
				else
					returnValue = Math.sqrt(B/N) * (HB/B - H/N);
				break;
			}
			case JACCARD:
			{
				returnValue = HB / (B + HnB);
				break;
			}
			case COVERAGE:
			{
				returnValue = B;
				break;
			}
			// XXX this is generally called PRECISION
			// PRECISION = TP / (TP+FP)            = HB / B
			// ACCURACY  = (TP+TN) / (TP+FP+FN+TN) = (HB+nHnB) / N
			case ACCURACY:
			{
				if (B == 0)
					returnValue = 0.0;
				else
					returnValue = HB / B;
				break;
			}
			case SPECIFICITY:
			{
				returnValue = nHnB / (N - H);
				// handle divide by zero when (N==H)
				// this case is not checked for by the
				// SINGLE NOMINAL constructor
				// as it would be a valid setting (but useless)
				if (Double.isNaN(returnValue))
					returnValue = 0.0; // by definition?
				break;
			}
			// XXX also called RECALL
			case SENSITIVITY:
			{
				returnValue = HB / H;
				break;
			}
			case LAPLACE:
			{
				returnValue = (HB+1) / (B+2);
				break;
			}
			case F_MEASURE:
			{
				returnValue = HB / (B + H);
				break;
			}
			case G_MEASURE:
			{
				returnValue = HB / (nHB + H);
				break;
			}
			case CORRELATION:
			{
				double nH = N-H;
				returnValue = (HB*nH - H*nHB) / Math.sqrt(H*nH*B*(N-B));
				// handle divide by zero when (N==H) or (N==B)
				// this case is not checked for by the
				// SINGLE NOMINAL constructor
				// as it would be a valid setting (but useless)
				if (Double.isNaN(returnValue))
					returnValue = 0.0; // by definition?
				break;
			}
			case PURITY:
			{
				if (B == 0)
					returnValue = 0.0;
				else
					returnValue = HB / B;
				if (returnValue < 0.5)
					returnValue = 1.0 - returnValue;
				break;
			}
			case BAYESIAN_SCORE:
			{
				returnValue = calculateBayesianFactor(N, H, B, HB);
				break;
			}
			case LIFT:
            		{
				if (B == 0)
					returnValue = 0.0;
				else
					returnValue = (HB * N) / (B * H);
				// alternative has 3 divisions, but H/N is constant and could be cached
				// returnValue = (theCountHeadBody / theCoverage) / (theTotalTargetCoverage / theTotalCoverage);
				break;
			}
				case RELATIVE_LIFT:
			{
				if (B == 0)
					returnValue = 0.0;
				else
					returnValue = (HB * N) / (B * H) - 1;
				break;
			}
			default :
			{
				/*
				 * if the QM is valid for this TargetType
				 * 	it is not implemented here
				 * else
				 * 	this method should not have been called
				 */
				if (QM.getQualityMeasures(TargetType.SINGLE_NOMINAL).contains(theMeasure))
					throw new AssertionError(theMeasure);
				else
					throw new IllegalArgumentException("Invalid argument: " + theMeasure);
			}
		}

		if (Double.isNaN(returnValue))
			throw new AssertionError("QualityMeasure.calculate() NaN return");
		return returnValue;
	}

	private static final double mi(double a, double b, double c)
	{
		// by definition 0*log(x) = 0 (NOTE 0*Infinity would return NaN)
		if (a == 0.0)
			return 0.0;
		// (a+b)*(a+c) is never 0 (when b=>0 and c>=0), since a!=0
		return (a * Math.log(a / ((a+b)*(a+c))));
	}

	public static float calculatePropensityBased(QM theMeasure, int theCountHeadBody, int theCoverage, int theTotalCount, double theCountHeadPropensityScore)
	{
		// XXX MM - argument check

		float aCountHeadBody = (float) theCountHeadBody;
		float aCoverage = (float) theCoverage;
		float aTotalCount = (float) theTotalCount;
		float aCountHeadPropensityScore = (float) theCountHeadPropensityScore;
		float returnValue = Float.NaN;

		switch (theMeasure)
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
				break;
			}
			default :
			{
				// XXX MM - see argument check
				throw new IllegalArgumentException(QM.class.getSimpleName() + " invalid: " + theMeasure);
			}
		}

		return returnValue;
	}

	private static final double calculateChiSquared(double totalSupport, double headSupport, double bodySupport, double headBodySupport)
	{
		if ((bodySupport == totalSupport) || (bodySupport == 0.0))
			return 0.0;

		//HEADBODY
		double Eij = calculateExpectency(totalSupport, bodySupport, headSupport);
		double quality = calculatePowerTwo(headBodySupport - Eij) / Eij;

		//HEADNOTBODY
		Eij = calculateExpectency(totalSupport, (totalSupport - bodySupport), headSupport);
		quality += (calculatePowerTwo(headSupport - headBodySupport - Eij)) / Eij;

		//NOTHEADBODY
		Eij = calculateExpectency(totalSupport, (totalSupport - headSupport), bodySupport);
		quality += (calculatePowerTwo(bodySupport - headBodySupport - Eij)) / Eij;

		//NOTHEADNOTBODY
		Eij = calculateExpectency(totalSupport, (totalSupport - bodySupport), (totalSupport - headSupport));
		quality += (calculatePowerTwo((totalSupport - headSupport - bodySupport + headBodySupport) - Eij)) / Eij;

		return quality;
	}

	private static final double calculatePowerTwo(double value)
	{
		return (value * value);
	}

	private static final double calculateExpectency(double totalSupport, double bodySupport, double headSupport)
	{
		return totalSupport * (bodySupport / totalSupport) * (headSupport / totalSupport);
	}

	/**
	 * Computes the 2-log of p.
	 */
	private static final double lg(double p)
	{
		return Math.log(p) / Math.log(2.0);
	}

	static final double calculateEntropy(double bodySupport, double headBodySupport)
	{
		if (bodySupport == 0.0)
			return 0.0; //special case that should never occur

		if ((headBodySupport == 0.0) || (bodySupport == headBodySupport))
			return 0.0; // by definition

		double pj = headBodySupport / bodySupport;
		return -1.0*pj*lg(pj) - (1.0-pj)*lg(1.0-pj);
	}

	/**
	 * Calculates the ConditionalEntropy.
	 * By definition, 0*lg(0) is 0, such that any boundary cases return 0.
	 *
	 * @param bodySupport
	 * @param bodyHeadSupport
	 * @return the conditional entropy for given the two parameters.
	 */
	private static final double calculateConditionalEntropy(double bodySupport, double headBodySupport)
	{
		if (bodySupport == 0.0)
			return 0.0; //special case that should never occur

		double Phb = headBodySupport / bodySupport; //P(H|B)
		double Pnhb = (bodySupport - headBodySupport) / bodySupport; //P(H|B)
		if ((Phb == 0.0) || (Pnhb == 0.0))
			return 0.0; //by definition

		return -1.0*Phb*lg(Phb) - Pnhb*lg(Pnhb);
	}

	private static final double calculateInformationGain(double totalSupport, double headSupport, double bodySupport, double headBodySupport)
	{
		double aFraction = bodySupport / totalSupport;
		double aNotBodySupport = totalSupport - bodySupport;
		double aHeadNotBodySupport = headSupport - headBodySupport;

		return calculateEntropy(totalSupport, headSupport)
			- (aFraction * calculateConditionalEntropy(bodySupport, headBodySupport)) //inside the subgroup
			- ((1.0-aFraction) * calculateConditionalEntropy(aNotBodySupport, aHeadNotBodySupport)); //the complement
	}

	// TODO MM - just supply the int parameters in stead of casting doubles
	//Iyad Batal: Calculate the Bayesian score assuming uniform beta priors on all parameters
	private static final double calculateBayesianFactor(double totalSupport, double headSupport, double bodySupport, double headBodySupport)
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
	private static final double calculateBayesianScore(float totalSupport, float headSupport, float bodySupport, float headBodySupport)
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
	private static final double logAdd(double x, double y)
	{
		double res;
		if (Math.abs(x-y) >= 36.043)
			res = Math.max(x, y);
		else
			res= Math.log(1 + Math.exp(y - x)) + x;
		return res;
	}

	//Iyad Batal: auxiliary function to compute the difference of logarithms (input: log(a), log(b), output log(a-b))
	private static final double logDiff(double x, double y)
	{
		double res;
		if((x-y) >= 36.043)
			res = x;
		else
			res = Math.log(1-Math.exp(y - x)) + x;
		return res;
	}

	//Iyad Batal: auxiliary function to compute the marginal likelihood of model M_e (used in computing the Bayesian score)
	private static final double score_M_e(int N1, int N2, int alpha, int beta)
	{
		return Function.logGammaBig(alpha+beta) - Function.logGammaBig(alpha+N1+beta+N2) + Function.logGammaBig(alpha+N1) - Function.logGammaBig(alpha) + Function.logGammaBig(beta+N2) - Function.logGammaBig(beta);
	}

	//Iyad Batal: auxiliary function to compute the marginal likelihood of model M_h (used in computing the Bayesian score)
	private static final double[] score_M_h(int N11, int N12, int N21, int N22, int alpha1, int beta1, int alpha2, int beta2)
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

	public final int    getNrRecords()         { return itsNrRecords; }
	public final int    getNrPositives()       { return itsTotalTargetCoverage; }

	//get quality of upper left corner
	public final double getROCHeaven()
	{
		return calculate(itsTotalTargetCoverage, itsTotalTargetCoverage);
	}

	//lower right corner
	public final double getROCHell()
	{
		// do not call public method:
		// calculate(0, itsNrRecords - itsTotalTargetCoverage)
		// check 'B > 0' for externally supplied data could fail
		// in this case, B = (itsNrRecords - itsTotalTargetCoverage)
		// so when (itsNrRecords == itsTotalTargetCoverage), then B = 0
		// instead, call calculate(QM, N, H, HB, B) directly
		return calculate(itsQualityMeasure, itsNrRecords, itsTotalTargetCoverage, 0, itsNrRecords - itsTotalTargetCoverage);
	}

	//returns the average label ranking of the entire dataset, which this QM uses to compare rankings of subgroups to
	public final LabelRanking getBaseLabelRanking()
	{
		return itsAverageRanking;
	}

	//returns the average label ranking matrix of the entire dataset, which this QM uses to compare rankings of subgroups to
	public final LabelRankingMatrix getBaseLabelRankingMatrix()
	{
		return itsAverageRankingMatrix;
	}

	public final float computeLabelRankingDistance(int theSupport, LabelRankingMatrix theSubgroupRankingMatrix)
	{
		//Log.logCommandLine("computeLabelRankingDistance ===========================================");
		//Log.logCommandLine("support: " + Math.sqrt(theSupport));
		//Log.logCommandLine("subgroup matrix:");
		//theSubgroupRankingMatrix.print();

		float aDistance = 0.0f;
		switch (itsQualityMeasure)
		{
			case LR_NORM :
			{
				aDistance = itsAverageRankingMatrix.normDistance(theSubgroupRankingMatrix);
				break;
			}
			case LR_NORM_MODE :
			{
				aDistance = itsAverageRankingMatrix.normDistanceMode(theSubgroupRankingMatrix);
				break;
			}
			case LR_WNORM :
			{
				aDistance = itsAverageRankingMatrix.wnormDistance(theSubgroupRankingMatrix);
				break;
			}
			case LR_LABELWISE_MIN :
			{
				aDistance = itsAverageRankingMatrix.minDistance(theSubgroupRankingMatrix);
				break;
			}
			case LR_LABELWISE_MAX :
			{
				aDistance = itsAverageRankingMatrix.labelwiseMax(theSubgroupRankingMatrix);
				break;
			}
			case LR_PAIRWISE_MAX :
			{
				aDistance = itsAverageRankingMatrix.pairwiseMax(theSubgroupRankingMatrix);
				break;
			}
			case LR_COVARIANCE :
			{
				aDistance = itsAverageRankingMatrix.covDistance(theSubgroupRankingMatrix);
				break;
			}
			default :
			{
				/*
				 * if the QM is valid for this TargetType
				 * 	it is not implemented here
				 * else
				 * 	this method should not have been called
				 */
				if (QM.getQualityMeasures(TargetType.LABEL_RANKING).contains(itsQualityMeasure))
					throw new AssertionError(itsQualityMeasure);
				else
					throw new IllegalArgumentException("Invalid argument: " + itsQualityMeasure);
			}
		}

		//aDistance = (float) Math.pow(aDistance,2);
		float aPercentage = (float) theSupport/itsNrRecords;
		float aSize = (float) Math.sqrt(aPercentage);
		//float aSize = (float) Math.sqrt(theSupport);
		return aSize * aDistance;
	}

	//SINGLE_NUMERIC =======================================================

	/*
	 * MEAN = sqrt(sampleSize)*(sampleAvg-dataAvg);
	 * Z = MEAN / dataStdDev;   // using   dataSumSquaredDeviations/N
	 * T = MEAN / sampleStdDev; // using sampleSumSquaredDeviations/(n-1)
	 */
	/**
	 * Calculates the quality for a sample, or {@link Subgroup}.
	 *
	 * @param Statistics the necessary statistics
	 * @param thePDF the ProbabilityDensityFunction for the sample
	 *
	 * @return the quality
	 *
	 * @see Stat
	 * @see Column#getStatistics(java.util.BitSet, java.util.Set)
	 * @see ProbabilityDensityFunction
	 */
	// FIXME this code should no longer be used, see correct alternative below
	//       kept for historic reasons only: delete after comparative evaluation
	// FIXME there is no input validation, possible divide by zero (coverage)
	public float calculate(Statistics theStatistics, ProbabilityDensityFunction thePDF)
	{
		int aCoverage   = theStatistics.getCoverage();
		float aSum      = theStatistics.getSubgroupSum();
		float anSSD     = theStatistics.getSubgroupSumSquaredDeviations();
		float aMedian   = theStatistics.getMedian();
		float aMedianAD = theStatistics.getMedianAbsoluteDeviations();

		float aReturn = Float.NEGATIVE_INFINITY;
		switch (itsQualityMeasure)
		{
			//NUMERIC
			case EXPLAINED_VARIANCE :
			{
				if (itsNrRecords <= 1)
					aReturn = 0.0f;
				else
//					aReturn = 1.0f - ((anSSD + theStatistics.getComplementSumSquaredDeviations()) / itsTotalSSD);
					aReturn = (float) (1.0 - ((anSSD + theStatistics.getComplementSumSquaredDeviations()) / (calculatePowerTwo(itsTotalStandardDeviation) * itsNrRecords)));
				break;
			}
			case Z_SCORE :
			{
				if (itsNrRecords <= 1)
					aReturn = 0.0f;
				else
					aReturn = (float) ((Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage)) / itsTotalStandardDeviation);
				break;
			}
			case INVERSE_Z_SCORE :
			{
				if (itsNrRecords <= 1)
					aReturn = 0.0f;
				else
					aReturn = (float) -((Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage)) / itsTotalStandardDeviation);
				break;
			}
			case ABS_Z_SCORE :
			{
				if (itsNrRecords <= 1)
					aReturn = 0.0f;
				else
					aReturn = (float) (Math.abs((Math.sqrt(aCoverage) * (aSum/aCoverage - itsTotalAverage)) / itsTotalStandardDeviation));
				break;
			}
			case AVERAGE :
			{
				aReturn = aSum/aCoverage;
				break;
			}
			case INVERSE_AVERAGE :
			{
				aReturn = -aSum/aCoverage;
				break;
			}
			case QM_SUM :
			{
				aReturn = aSum;
				break;
			}
			case INVERSE_SUM :
			{
				aReturn = -aSum;
				break;
			}
			case ABS_DEVIATION :
			{
				aReturn = Math.abs(aSum/aCoverage - itsTotalAverage);
				break;
			}
			case MEAN_TEST :
			{
				aReturn = (float) (Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage));
				break;
			}
			case INVERSE_MEAN_TEST :
			{
				aReturn = (float) -(Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage));
				break;
			}
			case ABS_MEAN_TEST :
			{
				aReturn = (float) (Math.abs(Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage)));
				break;
			}
			case T_TEST :
			{
				if(aCoverage <= 1)
					aReturn = 0.0f;
				else
					aReturn = (float) ((Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage)) / Math.sqrt(anSSD/(aCoverage-1.0)));
				break;
			}
			case INVERSE_T_TEST :
			{
				if(aCoverage <= 1)
					aReturn = 0.0f;
				else
					aReturn = (float) -((Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage)) / Math.sqrt(anSSD/(aCoverage-1.0)));
				break;
			}
			case ABS_T_TEST :
			{
				if(aCoverage <= 1)
					aReturn = 0.0f;
				else
					aReturn = (float) (Math.abs((Math.sqrt(aCoverage) * (aSum/aCoverage - itsTotalAverage)) / Math.sqrt(anSSD/(aCoverage-1.0))));
				break;
			}
			//ORDINAL
			// MM note AUC = U / n1*n2
			//         where U = (aSum-aSequenceSum)
			//               n1 = aCoverage
			//               n2 = aComplementCoverage
			// U is Mann-Whitney U
			case AUC :
			{
				float aComplementCoverage = itsNrRecords - aCoverage;
				float aSequenceSum = aCoverage*(aCoverage+1.0f)/2.0f; //sum of all positive ranks, assuming ideal case
				aReturn = 1.0f + (aSequenceSum-aSum)/(aCoverage*aComplementCoverage);
				//aReturn = (aSum-aSequenceSum)/(aCoverage*aComplementCoverage);
				break;
			}
			// MM the name of this statistic is confusing
			// this is the Wilcoxon rank sum test
			// it is equivalent to the Mann-Whithey U test
			case WMW_RANKS :
			{
				float aComplementCoverage = itsNrRecords - aCoverage;
				float aMean = (aCoverage*(aCoverage+aComplementCoverage+1))/2.0f;
				float aStDev = (float) Math.sqrt((aCoverage*aComplementCoverage*(aCoverage+aComplementCoverage+1))/12.0f);
				aReturn = (aSum-aMean)/aStDev;
				break;
			}
			case INVERSE_WMW_RANKS :
			{
				float aComplementCoverage = itsNrRecords - aCoverage;
				float aMean = (aCoverage*(aCoverage+aComplementCoverage+1))/2.0f;
				float aStDev = (float) Math.sqrt((aCoverage*aComplementCoverage*(aCoverage+aComplementCoverage+1))/12.0f);
				aReturn = -((aSum-aMean)/aStDev);
				break;
			}
			case ABS_WMW_RANKS :
			{
				float aComplementCoverage = itsNrRecords - aCoverage;
				float aMean = (aCoverage*(aCoverage+aComplementCoverage+1))/2.0f;
				float aStDev = (float) Math.sqrt((aCoverage*aComplementCoverage*(aCoverage+aComplementCoverage+1))/12.0f);
				aReturn = Math.abs((aSum-aMean)/aStDev);
				break;
			}
			case MMAD :
			{
				aReturn = (aCoverage/(2.0f*aMedian+aMedianAD));
				break;
			}
			// DISTRIBUTION
			// normal H^2 for continuous PDFs
			case SQUARED_HELLINGER :
			{
				if (SubgroupDiscovery.TEMPORARY_CODE)
				{
					aReturn = (float) ((ProbabilityMassFunction_ND) thePDF).itsScore;
					break;
				}

				double aTotalSquaredDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					float aDensity = (TEMPORARY_CODE_SG_VS_COMPLEMENT ? ((ProbabilityDensityFunction2) thePDF).getComplementDensity(i) : itsPDF.getDensity(i));
					float aDensitySubgroup = thePDF.getDensity(i);
					double aDifference = Math.sqrt(aDensity) - Math.sqrt(aDensitySubgroup);
					aTotalSquaredDifference += (aDifference * aDifference);
					//Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				}
				Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				aReturn = (float) (0.5 * aTotalSquaredDifference);
				break;
			}
			case SQUARED_HELLINGER_WEIGHTED :
			{
				double aTotalSquaredDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					float aDensity = itsPDF.getDensity(i);
					float aDensitySubgroup = thePDF.getDensity(i);
					double aDifference = Math.sqrt(aDensity) - Math.sqrt(aDensitySubgroup);
					aTotalSquaredDifference += (aDifference * aDifference);
					//Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				}
				Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				aReturn = (float) ((0.5 * (aTotalSquaredDifference * aCoverage)) / itsNrRecords);
				break;
			}
			case SQUARED_HELLINGER_WEIGHTED_ADJUSTED :
			{
				// SQUARED_HELLINGER
				double aTotalSquaredDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					float aDensity = itsPDF.getDensity(i);
					float aDensitySubgroup = thePDF.getDensity(i);
					double aDifference = Math.sqrt(aDensity) - Math.sqrt(aDensitySubgroup);
					aTotalSquaredDifference += (aDifference * aDifference);
					//Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				}
				Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				aReturn = (float) ((0.5 * (aTotalSquaredDifference * aCoverage)) / itsNrRecords);

				// now weight SQUARED_HELLINGER
				// magic number = maximum possible score
				// it lies at (5/9, 4/27)
//				aReturn = (float) (aTotalSquaredDifference * (aCoverage / (2.0 * itsNrRecords)));
				aReturn = (float) (aReturn / (4.0/27.0));
				break;
			}
			case KULLBACK_LEIBLER :
			{
				double aTotalDivergence = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					float aDensity = itsPDF.getDensity(i);
					float aDensitySubgroup = thePDF.getDensity(i);
					/*
					 * avoid errors in Math.log() because of
					 * (0 / x) or (x / 0)
					 * returns 0 by definition according to
					 * http://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence
					 * NOTE this also catches DIVIVE_BY_0
					 * for (aDensity == 0) because
					 * (aSubgroupDensity == 0) for at least
					 * all situations where (aDenisity == 0)
					 */
					if (aDensitySubgroup == 0.0)
						continue;
					aTotalDivergence += (aDensitySubgroup * Math.log(aDensitySubgroup/aDensity));
				}
				aReturn = (float) aTotalDivergence;
				break;
			}
			case KULLBACK_LEIBLER_WEIGHTED :
			{
				double aTotalDivergence = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					float aDensity = itsPDF.getDensity(i);
					float aDensitySubgroup = thePDF.getDensity(i);
					/*
					 * avoid errors in Math.log() because of
					 * (0 / x) or (x / 0)
					 * returns 0 by definition according to
					 * http://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence
					 * NOTE this also catches DIVIVE_BY_0
					 * for (aDensity == 0) because
					 * (aSubgroupDensity == 0) for at least
					 * all situations where (aDenisity == 0)
					 */
					if (aDensitySubgroup == 0.0)
						continue;
					aTotalDivergence += (aDensitySubgroup * Math.log(aDensitySubgroup/aDensity));
				}
				aReturn = (float) ((aTotalDivergence * aCoverage) / itsNrRecords);
				break;
			}
			case CWRACC :
			{
				//some random code
				// http://en.wikipedia.org/wiki/Total_variation_distance ?
				double aTotalDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					float aDensity = itsPDF.getDensity(i);
					float aDensitySubgroup = thePDF.getDensity(i);
					aTotalDifference += Math.abs(aDensity - aDensitySubgroup);
				}
				Log.logCommandLine("difference in PDF: " + aTotalDifference);
				aReturn = (float) ((aTotalDifference * aCoverage) / itsNrRecords);
				break;
			}
			case CWRACC_UNWEIGHTED :
			{
				double aTotalDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					float aDensity = itsPDF.getDensity(i);
					float aDensitySubgroup = thePDF.getDensity(i);
					aTotalDifference += Math.abs(aDensity - aDensitySubgroup);
				}
				Log.logCommandLine("difference in PDF: " + aTotalDifference);
				aReturn = (float) aTotalDifference;
				break;
			}
			default :
			{
				/*
				 * if the QM is valid for this TargetType
				 * 	it is not implemented here
				 * else
				 * 	this method should not have been called
				 */
				if (QM.getQualityMeasures(TargetType.SINGLE_NUMERIC).contains(itsQualityMeasure) ||
						QM.getQualityMeasures(TargetType.SINGLE_ORDINAL).contains(itsQualityMeasure))
					throw new AssertionError(itsQualityMeasure);
				else
					throw new IllegalArgumentException("Invalid QM: " + itsQualityMeasure);
			}
		}

		return aReturn;
	}

	// FOR FUTURE USE - many calculations above are incorrect for large N
	//                  see comment on coverage for SingleBinary calculate
	private final double calculate2(Statistics theStatistics, ProbabilityDensityFunction thePDF)
	{
		double aCoverage = theStatistics.getCoverage();
		double aSum      = theStatistics.getSubgroupSum();
		double anSSD     = theStatistics.getSubgroupSumSquaredDeviations();
		double aMedian   = theStatistics.getMedian();
		double aMedianAD = theStatistics.getMedianAbsoluteDeviations();

		final double aReturn; // final to enforce each branch sets a value
		switch (itsQualityMeasure)
		{
			//NUMERIC
			case EXPLAINED_VARIANCE :
			{
				if (itsNrRecords <= 1)
					aReturn = 0.0;
				else
					aReturn = 1.0 - ((anSSD + theStatistics.getComplementSumSquaredDeviations()) / (calculatePowerTwo(itsTotalStandardDeviation) * itsNrRecords));
				break;
			}
			case Z_SCORE :
			{
				if (itsNrRecords <= 1)
					aReturn = 0.0;
				else
					aReturn = (Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage)) / itsTotalStandardDeviation;
				break;
			}
			case INVERSE_Z_SCORE :
			{
				if (itsNrRecords <= 1)
					aReturn = 0.0;
				else
					aReturn = -((Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage)) / itsTotalStandardDeviation);
				break;
			}
			case ABS_Z_SCORE :
			{
				if (itsNrRecords <= 1)
					aReturn = 0.0;
				else
					aReturn = Math.abs((Math.sqrt(aCoverage) * (aSum/aCoverage - itsTotalAverage)) / itsTotalStandardDeviation);
				break;
			}
			case AVERAGE :
			{
				aReturn = aSum/aCoverage;
				break;
			}
			case INVERSE_AVERAGE :
			{
				aReturn = -aSum/aCoverage;
				break;
			}
			case QM_SUM :
			{
				aReturn = aSum;
				break;
			}
			case INVERSE_SUM :
			{
				aReturn = -aSum;
				break;
			}
			case ABS_DEVIATION :
			{
				aReturn = Math.abs(aSum/aCoverage - itsTotalAverage);
				break;
			}
			case MEAN_TEST :
			{
				aReturn = Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage);
				break;
			}
			case INVERSE_MEAN_TEST :
			{
				aReturn = -(Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage));
				break;
			}
			case ABS_MEAN_TEST :
			{
				aReturn = Math.abs(Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage));
				break;
			}
			case T_TEST :
			{
				if(aCoverage <= 1)
					aReturn = 0.0;
				else
					aReturn = (Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage)) / Math.sqrt(anSSD/(aCoverage-1.0));
				break;
			}
			case INVERSE_T_TEST :
			{
				if(aCoverage <= 1)
					aReturn = 0.0;
				else
					aReturn = -((Math.sqrt(aCoverage) * ((aSum/aCoverage) - itsTotalAverage)) / Math.sqrt(anSSD/(aCoverage-1.0)));
				break;
			}
			case ABS_T_TEST :
			{
				if(aCoverage <= 1)
					aReturn = 0.0;
				else
					aReturn = Math.abs((Math.sqrt(aCoverage) * (aSum/aCoverage - itsTotalAverage)) / Math.sqrt(anSSD/(aCoverage-1.0)));
				break;
			}
			//ORDINAL
			// MM note AUC = U / n1*n2
			//         where U = (aSum-aSequenceSum)
			//               n1 = aCoverage
			//               n2 = aComplementCoverage
			// U is Mann-Whitney U
			case AUC :
			{
				double aComplementCoverage = itsNrRecords - aCoverage;
				double aSequenceSum = aCoverage*(aCoverage+1.0) / 2.0; //sum of all positive ranks, assuming ideal case
				aReturn = 1.0 + ((aSequenceSum-aSum) / (aCoverage*aComplementCoverage));
				break;
			}
			// MM the name of this statistic is confusing
			// this is the Wilcoxon rank sum test
			// it is equivalent to the Mann-Whithey U test
			case WMW_RANKS :
			{
				double aComplementCoverage = itsNrRecords - aCoverage;
				double aMean  = (aCoverage*(aCoverage+aComplementCoverage+1.0))/2.0;
				double aStDev = Math.sqrt((aCoverage*aComplementCoverage*(aCoverage+aComplementCoverage+1.0)) / 12.0);
				aReturn = (aSum-aMean) / aStDev;
				break;
			}
			case INVERSE_WMW_RANKS :
			{
				double aComplementCoverage = itsNrRecords - aCoverage;
				double aMean  = (aCoverage*(aCoverage+aComplementCoverage+1.0)) / 2.0;
				double aStDev = Math.sqrt((aCoverage*aComplementCoverage*(aCoverage+aComplementCoverage+1.0)) / 12.0);
				aReturn = -((aSum-aMean) / aStDev);
				break;
			}
			case ABS_WMW_RANKS :
			{
				double aComplementCoverage = itsNrRecords - aCoverage;
				double aMean  = (aCoverage*(aCoverage+aComplementCoverage+1.0)) / 2.0;
				double aStDev = Math.sqrt((aCoverage*aComplementCoverage*(aCoverage+aComplementCoverage+1.0)) / 12.0);
				aReturn = Math.abs((aSum-aMean) / aStDev);
				break;
			}
			case MMAD :
			{
				aReturn = aCoverage/(2.0*aMedian+aMedianAD);
				break;
			}
			// DISTRIBUTION
			// normal H^2 for continuous PDFs
			case SQUARED_HELLINGER :
			{
				if (SubgroupDiscovery.TEMPORARY_CODE)
				{
					aReturn = ((ProbabilityMassFunction_ND) thePDF).itsScore;
					break;
				}

				double aTotalSquaredDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					double aDensity = (TEMPORARY_CODE_SG_VS_COMPLEMENT ? ((ProbabilityDensityFunction2) thePDF).getComplementDensity(i) : itsPDF.getDensity(i));
					double aDensitySubgroup = thePDF.getDensity(i);
					double aDifference = Math.sqrt(aDensity) - Math.sqrt(aDensitySubgroup);
					aTotalSquaredDifference += (aDifference * aDifference);
					//Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				}
				Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				aReturn = 0.5 * aTotalSquaredDifference;
				break;
			}
			case SQUARED_HELLINGER_WEIGHTED :
			{
				double aTotalSquaredDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					double aDensity = itsPDF.getDensity(i);
					double aDensitySubgroup = thePDF.getDensity(i);
					double aDifference = Math.sqrt(aDensity) - Math.sqrt(aDensitySubgroup);
					aTotalSquaredDifference += (aDifference * aDifference);
					//Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				}
				Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				aReturn = (0.5 * (aTotalSquaredDifference * aCoverage)) / itsNrRecords;
				break;
			}
			case SQUARED_HELLINGER_WEIGHTED_ADJUSTED :
			{
				// SQUARED_HELLINGER
				double aTotalSquaredDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					double aDensity = itsPDF.getDensity(i);
					double aDensitySubgroup = thePDF.getDensity(i);
					double aDifference = Math.sqrt(aDensity) - Math.sqrt(aDensitySubgroup);
					aTotalSquaredDifference += (aDifference * aDifference);
					//Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				}
				Log.logCommandLine("difference in PDF: " + aTotalSquaredDifference);
				double d = (0.5 * (aTotalSquaredDifference * aCoverage)) / itsNrRecords;

				// now weight SQUARED_HELLINGER
				// magic number = maximum possible score
				// it lies at (5/9, 4/27)
//				aReturn = (float) (aTotalSquaredDifference * (aCoverage / (2.0 * itsNrRecords)));
				aReturn = d / (4.0/27.0);
				break;
			}
			case KULLBACK_LEIBLER :
			{
				double aTotalDivergence = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					double aDensity = itsPDF.getDensity(i);
					double aDensitySubgroup = thePDF.getDensity(i);
					/*
					 * avoid errors in Math.log() because of
					 * (0 / x) or (x / 0)
					 * returns 0 by definition according to
					 * http://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence
					 * NOTE this also catches DIVIVE_BY_0
					 * for (aDensity == 0) because
					 * (aSubgroupDensity == 0) for at least
					 * all situations where (aDenisity == 0)
					 */
					if (aDensitySubgroup == 0.0)
						continue;
					aTotalDivergence += (aDensitySubgroup * Math.log(aDensitySubgroup/aDensity));
				}
				aReturn = aTotalDivergence;
				break;
			}
			case KULLBACK_LEIBLER_WEIGHTED :
			{
				double aTotalDivergence = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					double aDensity = itsPDF.getDensity(i);
					double aDensitySubgroup = thePDF.getDensity(i);
					/*
					 * avoid errors in Math.log() because of
					 * (0 / x) or (x / 0)
					 * returns 0 by definition according to
					 * http://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence
					 * NOTE this also catches DIVIVE_BY_0
					 * for (aDensity == 0) because
					 * (aSubgroupDensity == 0) for at least
					 * all situations where (aDenisity == 0)
					 */
					if (aDensitySubgroup == 0.0)
						continue;
					aTotalDivergence += (aDensitySubgroup * Math.log(aDensitySubgroup/aDensity));
				}
				aReturn = (aTotalDivergence * aCoverage) / itsNrRecords;
				break;
			}
			case CWRACC :
			{
				//some random code
				// http://en.wikipedia.org/wiki/Total_variation_distance ?
				double aTotalDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					double aDensity = itsPDF.getDensity(i);
					double aDensitySubgroup = thePDF.getDensity(i);
					aTotalDifference += Math.abs(aDensity - aDensitySubgroup);
				}
				Log.logCommandLine("difference in PDF: " + aTotalDifference);
				aReturn = (aTotalDifference * aCoverage) / itsNrRecords;
				break;
			}
			case CWRACC_UNWEIGHTED :
			{
				double aTotalDifference = 0.0;
				for (int i = 0, j = itsPDF.size(); i < j; ++i)
				{
					double aDensity = itsPDF.getDensity(i);
					double aDensitySubgroup = thePDF.getDensity(i);
					aTotalDifference += Math.abs(aDensity - aDensitySubgroup);
				}
				Log.logCommandLine("difference in PDF: " + aTotalDifference);
				aReturn = aTotalDifference;
				break;
			}
			default :
			{
				/*
				 * if the QM is valid for this TargetType
				 * 	it is not implemented here
				 * else
				 * 	this method should not have been called
				 */
				if (QM.getQualityMeasures(TargetType.SINGLE_NUMERIC).contains(itsQualityMeasure) ||
						QM.getQualityMeasures(TargetType.SINGLE_ORDINAL).contains(itsQualityMeasure))
					throw new AssertionError(itsQualityMeasure);
				else
					throw new IllegalArgumentException("Invalid QM: " + itsQualityMeasure);
			}
		}

		return aReturn;
	}


	//MULTI_LABEL (Bayesian) ===============================================

	public QualityMeasure(SearchParameters theSearchParameters, DAG theDAG, int theTotalCoverage)
	{
		if (theSearchParameters == null)
			throw new IllegalArgumentException("QualityMeasure: theSearchParameters can not be null");
		if (theSearchParameters.getQualityMeasure() == null)
			throw new IllegalArgumentException("QualityMeasure: theSearchParameters.getQualityMeasure() can not be null");
		if (!QM.getQualityMeasures(TargetType.MULTI_LABEL).contains(theSearchParameters.getQualityMeasure()))
			throw new IllegalArgumentException("QualityMeasure: not a MULTI_LABEL measure");
		if (theDAG == null)
			throw new IllegalArgumentException("QualityMeasure: theDAG can not be null");
		if (theTotalCoverage <= 0)
			throw new IllegalArgumentException("QualityMeasure: theTotalCoverage must be > 0");

		itsQualityMeasure = theSearchParameters.getQualityMeasure();
		itsNrRecords = theTotalCoverage;

		itsDAG = theDAG;
		itsNrNodes = itsDAG.getSize();
		itsAlpha = theSearchParameters.getAlpha();
		itsBeta = theSearchParameters.getBeta();
		itsVStructures = itsDAG.determineVStructures();
	}

	public float calculate(Subgroup theSubgroup)
	{
		switch (itsQualityMeasure)
		{
			case WEED :
			{
				return (float) (Math.pow(calculateEntropy(itsNrRecords, theSubgroup.getCoverage()), itsAlpha) *
						Math.pow(calculateEditDistance(theSubgroup.getDAG()), itsBeta));
			}
			case EDIT_DISTANCE :
			{
				return calculateEditDistance(theSubgroup.getDAG());
			}
			default :
			{
				/*
				 * if the QM is valid for this TargetType
				 * 	it is not implemented here
				 * else
				 * 	this method should not have been called
				 */
				if (QM.getQualityMeasures(TargetType.MULTI_LABEL).contains(itsQualityMeasure))
					throw new AssertionError(itsQualityMeasure);
				else
					throw new IllegalArgumentException("Invalid QM: " + itsQualityMeasure);
			}
		}
	}

	public float calculateEditDistance(DAG theDAG)
	{
		if (theDAG.getSize() != itsNrNodes)
		{
			Log.logCommandLine("Comparing incompatible DAG's. One has " + theDAG.getSize() + " nodes and the other has " + itsNrNodes + ". Throw pi.");
			// FIXME MM - throw IllegalArgumentException
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

	//SCAPE =======================================================

	public QualityMeasure(QM theQualityMeasure, int theTotalCoverage, int theTotalTargetCoverage, Column theBinaryTarget, Column theNumericTarget)
	{
		if (theQualityMeasure == null)
			throw new IllegalArgumentException("QualityMeasure: theQualityMeasure can not be null");
		if (theTotalCoverage <= 0)
			throw new IllegalArgumentException("QualityMeasure: theTotalCoverage must be > 0");
		if (theTotalTargetCoverage <= 0)
			throw new IllegalArgumentException("QualityMeasure: theTotalTargetCoverage must be > 0");
		if (theBinaryTarget == null)
			throw new IllegalArgumentException("QualityMeasure: theBinaryTarget can not be null");
		if (theNumericTarget == null)
			throw new IllegalArgumentException("QualityMeasure: theNumericTarget can not be null");

		itsQualityMeasure = theQualityMeasure;
		itsNrRecords = theTotalCoverage;
		itsTotalTargetCoverage = theTotalTargetCoverage;
		itsBinaryTarget = theBinaryTarget;
		itsNumericTarget = theNumericTarget;

		itsDescendingOrderingPermutation = generateOrderingPermutation();
		assert(generateOrderingPermutationTest());

		setAverageSubrankingLoss();
	}

	/*
	 * WD: The following additional constructor should not be necessary. However, Java is once again biting me;
	 * in the mining window my quality measure is perfectly able to compute the overall subranking loss and displaying
	 * it in the corresponding field, but when I want to incorporate it in quality measures in the SD process, its
	 * value magically reverts to zero. Hence, I'm passing it through the SearchParameters.
	 */
//	public QualityMeasure(QM theQualityMeasure, int theTotalCoverage, int theTotalTargetCoverage, Column theBinaryTarget, Column theNumericTarget, float theOverallSubrankingLoss)
//	{
//		if (theQualityMeasure == null)
//			throw new IllegalArgumentException("QualityMeasure: theQualityMeasure can not be null");
//		if (theTotalCoverage <= 0)
//			throw new IllegalArgumentException("QualityMeasure: theTotalCoverage must be > 0");
//		if (theTotalTargetCoverage <= 0)
//			throw new IllegalArgumentException("QualityMeasure: theTotalTargetCoverage must be > 0");
//		if (theBinaryTarget == null)
//			throw new IllegalArgumentException("QualityMeasure: theBinaryTarget can not be null");
//		if (theNumericTarget == null)
//			throw new IllegalArgumentException("QualityMeasure: theNumericTarget can not be null");
//
//		itsQualityMeasure = theQualityMeasure;
//		itsNrRecords = theTotalCoverage;
//		itsTotalTargetCoverage = theTotalTargetCoverage;
//		itsBinaryTarget = theBinaryTarget;
//		itsNumericTarget = theNumericTarget;
//		itsOverallSubrankingLoss = theOverallSubrankingLoss;
//
//		itsDescendingOrderingPermutation = generateOrderingPermutation();
//		assert(generateOrderingPermutationTest());
//	}

	/*
	 * Generates permutation that would order the values in the numeric target, using 
	 * a custom-built Comparator. This ordering is used in the computation of the 
	 * (sub-)ranking loss in the quality measures. 
	 */
	private int[] generateOrderingPermutation()
	{
		float[] afloatArray = itsNumericTarget.getFloats();
		Float[] aFloatArray = convertToFloats(afloatArray);

		ArrayIndexComparator comparator = new ArrayIndexComparator(aFloatArray);

		Integer[] anIntegerArray = comparator.createIndexArray();
		Arrays.sort(anIntegerArray, comparator);
		return convertToints(anIntegerArray);
	}

	private Float[] convertToFloats(float[] thefloatArray)
	{
		Float[] aResult = new Float[itsNrRecords];
		for (int i=0; i<itsNrRecords; i++)
			aResult[i] = (Float) thefloatArray[i];
		return aResult;
	}

	private int[] convertToints(Integer[] theIntegerArray)
	{
		int[] aResult = new int[itsNrRecords];
		for (int i=0; i<itsNrRecords; i++)
			aResult[i] = (int) theIntegerArray[i];
		return aResult;
	}

	// relies on itsOverallSubrankingLoss being 0.0f on first call
	private void setAverageSubrankingLoss()
	{
		BitSet theWholeDataset = new BitSet(itsNrRecords);
		theWholeDataset.set(0, itsNrRecords);
		itsOverallSubrankingLoss = calculate(theWholeDataset, itsNrRecords, itsTotalTargetCoverage);
	}

	public float getOverallSubrankingLoss() { return itsOverallSubrankingLoss; }

	public float calculate(BitSet theSubgroup, int theCoverage, int theTargetCoverage)
	{
		if (itsNrRecords <= 1)
			return 0.0f;

		switch (itsQualityMeasure)
		{
			case SUBRANKING_LOSS :
				return calculateSubrankingLoss(theSubgroup, theCoverage, theTargetCoverage);
			case NEGATIVE_SUBRANKING_LOSS :
				return -calculateSubrankingLoss(theSubgroup, theCoverage, theTargetCoverage);
			case RELATIVE_SUBRANKING_LOSS :
				return (calculateSubrankingLoss(theSubgroup, theCoverage, theTargetCoverage) - itsOverallSubrankingLoss);
			case REVERSE_RELATIVE_SUBRANKING_LOSS :
				return (itsOverallSubrankingLoss - calculateSubrankingLoss(theSubgroup, theCoverage, theTargetCoverage));
			default :
			{
				/*
				 * if the QM is valid for this TargetType
				 * 	it is not implemented here
				 * else
				 * 	this method should not have been called
				 */
				if (QM.getQualityMeasures(TargetType.SCAPE).contains(itsQualityMeasure))
					throw new AssertionError(itsQualityMeasure);
				else
					throw new IllegalArgumentException("Invalid QM: " + itsQualityMeasure);
			}
		}
	}

	/*
	 * This is the worky version that ignores ties in the numeric attribute. I would like to keep
	 * this available in comments for the time being, just in case that some reviewer doesn't like
	 * the 0.5 factor in the tie-incorporating version below.
	 */
/*	public float calculateSubrankingLoss(BitSet theSubgroup, int theCoverage, int theNrPositives)
	{
		int aNrNegatives = theCoverage-theNrPositives;
		int aPositiveLoopCount = 0;
		int aNegativeLoopCount = 0;
		float aTotalRankingLoss = 0.0f;
		
		/*
		 * Run in descending order through the numeric target. If the record is not a member of the
		 * subgroup, ignore it. If it is, check its binary target value. If it is positive, add
		 * the number of negatives seen so far to the total ranking loss. Update the positives or 
		 * negatives loop counter by the newly seen records. Stop if we have seen all the positives,
		 * or all the negatives in the subgroup. If we have seen all the negatives, we have to add
		 * punishment for the unseen positives to the ranking loss. Divide by the number of 
		 * positives to find an average.
		 
		boolean aContinueLoop = true;
		for (int i=0; aContinueLoop; i++)
		{
				int aCurrentIndex = itsDescendingOrderingPermutation[i];
				if (theSubgroup.get(aCurrentIndex))
				{
					if (itsBinaryTarget.getBinary(aCurrentIndex))
					{
						aPositiveLoopCount++;
						aTotalRankingLoss += aNegativeLoopCount;
					}
					else
					{
						aNegativeLoopCount++;
					}
				}
				if (aPositiveLoopCount == theNrPositives) 
					aContinueLoop = false;
				if (aNegativeLoopCount == aNrNegatives)
				{
					aContinueLoop = false;
					aTotalRankingLoss += (theNrPositives-aPositiveLoopCount)*aNrNegatives;
				}
		}
		return aTotalRankingLoss / (float) theNrPositives;
	}*/

	public float calculateSubrankingLoss(BitSet theSubgroup, int theCoverage, int theNrPositives)
	{
		// Statistics for the main loop, counting the results that have been completely handled so far
		int aNrNegatives = theCoverage-theNrPositives;
		int aPositiveLoopCount = 0;
		int aNegativeLoopCount = 0;
		float aTotalRankingLoss = 0.0f;

		// Statistics for tie breaking, maintaining the results that may still fall under ties in the numeric target
		int aPositiveTiesCount = 0;
		int aNegativeTiesCount = 0;
		float aPreviousFloatValue = Float.MAX_VALUE;

		boolean aContinueLoop = true;
		for (int i=0; aContinueLoop && i < itsNrRecords; i++)
		{
			// Get the current index
			int aCurrentIndex = itsDescendingOrderingPermutation[i];

			/*
			 * When the numeric target has changed, punish tied records by a factor 1/2. For each tied 
			 * positive I want to add to the total ranking loss the number of tied negatives divided
			 * by two. This can be expressed by tied positives * tied negatives * 1/2.
			 * 
			 * Afterwards, store the new numeric target value, flush the temporary counters into the 
			 * full loop counters, and reset the temporary counters to zero.
			 */

			float aCurrentFloatValue = itsNumericTarget.getFloat(aCurrentIndex); 
			if (aCurrentFloatValue < aPreviousFloatValue)
			{
				aTotalRankingLoss += aPositiveTiesCount * aNegativeTiesCount * 0.5f;
				aPreviousFloatValue = aCurrentFloatValue;
				aPositiveLoopCount += aPositiveTiesCount;
				aNegativeLoopCount += aNegativeTiesCount;
				aPositiveTiesCount = 0;
				aNegativeTiesCount = 0;

				/*
				 * Stop if we have seen all the positives,
				 * or all the negatives in the subgroup. If we have seen all the negatives, we have to add
				 * punishment for the unseen positives to the ranking loss. Divide by the number of 
				 * positives to find an average.
				 */
				if (aPositiveLoopCount == theNrPositives) 
					aContinueLoop = false;
				if (aNegativeLoopCount == aNrNegatives)
				{
					aContinueLoop = false;
					aTotalRankingLoss += (theNrPositives-aPositiveLoopCount)*aNrNegatives;
				}
			}

			/*
			 * Run in descending order through the numeric target. If the record is not a member of the
			 * subgroup, ignore it. If it is, check its binary target value. If it is positive, add
			 * the number of negatives handled so far to the total ranking loss. Update the corresponding
			 * temporary counter to handle ties.
			 * 
			 * Extra test in the loop for when the flushing of the tied records in the previous loop
			 * completes our mission; in this case every remaining positive has already been punished.
			 */
			if (theSubgroup.get(aCurrentIndex) && aContinueLoop)
			{
				if (itsBinaryTarget.getBinary(aCurrentIndex))
				{
					aPositiveTiesCount++;
					aTotalRankingLoss += aNegativeLoopCount;
				}
				else
				{
					aNegativeTiesCount++;
				}
			}
		}
		return aTotalRankingLoss / (float) theNrPositives;
	}

	//  N 16B Objects, 2N + N*log(N) operations instead of
	// 2N 16B Objects, 3N + N*log(N) operations (and higher data locality)
	private int[] generateOrderingPermutationAlt()
	{
		// couple value-to-index through Pair
		Pair[] pairs = new Pair[itsNrRecords];
		for (int i = 0; i < itsNrRecords; ++i)
			pairs[i] = new Pair(itsNumericTarget.getFloat(i), i);

		// sort Pairs, based on (descending) float-values
		Arrays.sort(pairs);

		// create result from permuted indexes
		int[] result = new int[itsNrRecords];
		for (int i = 0; i < itsNrRecords; ++i)
			result[i] = pairs[i].itsIndex;

		return result;
	}

	// couple value-to-index
	// could be Triple(float data, int index, boolean target)
	// and use sorted Triple[] in calculateSubrankingLoss() directly (not
	// 'extracting' sorted indexes into itsDescendingOrderingPermutation)
	// that way all required info is bundled (very local) - improving speed
	// but Triple[] uses more memory than int[]
	private static final class Pair implements Comparable<Pair>
	{
		final float itsData;
		final int itsIndex;

		Pair(float data, int index)
		{
			itsData = data;
			itsIndex = index;
		}

		// descending order
		// returns > 0, if (this.itsData < other.itsData)
		// returns < 0, if (this.itsData > other.itsData)
		@Override
		public int compareTo(Pair other)
		{
			// not strictly needed - but fast pointer comparison
			if (this == other)
				return 0;

			// at this point a return of 0, occurs only when
			// (other.itsIndex - this.itsIndex) == 0,
			// would indicate an error, as no two Pairs should have
			// the same float AND index
			int cmp = Float.compare(other.itsData, this.itsData);
			return (cmp != 0) ? cmp : (this.itsIndex - other.itsIndex);
		}
	}

	// debug only -> should always return true
	private boolean generateOrderingPermutationTest()
	{
		int[] itsDescendingOrderingPermutationAlt = generateOrderingPermutationAlt();

		boolean concordant = true;
		for (int i = 0; i < itsNrRecords; ++i)
		{
			int x = itsDescendingOrderingPermutation[i];
			int y = itsDescendingOrderingPermutationAlt[i];
			float fx = itsNumericTarget.getFloat(x);
			float fy = itsNumericTarget.getFloat(y);

			System.out.format("%d=%f    %d=%f    (x==y)=%s    (fx==fy)=%s%n", x, fx, y, fy, (x==y), (fx==fy));
			if (fx!=fy)
				concordant = false;
		}

		return concordant;
	}
}
