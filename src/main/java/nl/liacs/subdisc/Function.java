/**
 * The Function class holds static methods for calculations that are shared
 * among multiple other classes.
 */
package nl.liacs.subdisc;

import java.util.*;

public class Function
{
	// for LogGamma
	private static final double GAMMA_STP = 2.50662827465;
	private static final double GAMMA_C1 = 76.18009173;
	private static final double GAMMA_C2 = -86.50532033;
	private static final double GAMMA_C3 = 24.01409822;
	private static final double GAMMA_C4 = -1.231739516;
	private static final double GAMMA_C5 = 1.20858003e-3;
	private static final double GAMMA_C6 = -5.36382e-6;
	private static final int TABULATION = 100000;
	private static final double[] itsPrecomputed = new double[TABULATION];
	static
	{
		double aValue = 0.0;
		for (int i=2; i<TABULATION; i++)
		{
			itsPrecomputed[i] = aValue;
			aValue += Math.log(i);
		}

//		double aValue = 0.0;
//		for (int i = 3, j = TABULATION; i < j; ++i)
//			itsPrecomputed[i] = (aValue += Math.log(i-1));
	}

	// uninstantiable
	private Function()
	{
	}

	/**
	 * Both {@link BinaryTable#computeBDeuFaster()} and
	 * {@link CrossCube#getBDeu()} use this method.
	 * 
	 * @param x
	 * 
	 * @return a <code>double</code>
	 */
	public static double logGamma(double x)
	{
		double ser = 1.0 + GAMMA_C1 / x + GAMMA_C2 / (x + 1.0) + GAMMA_C3 / (x + 2.0) + GAMMA_C4 / (x + 3.0) + GAMMA_C5 / (x + 4.0) + GAMMA_C6 / (x + 5.0);
		return (x - 0.5) * Math.log(x + 4.5) - x - 4.5 + Math.log(GAMMA_STP * ser);
	}

	//Iyad Batal: auxiliary function to compute logarithm of gamma
	//this is quite slow, if you have data larger than TABULATION
	// MM: see comment below
	public static double logGammaBig(int a)
	{
//		Log.logCommandLine("logGammaBig: " + a + ", (table) " + itsPrecomputed[a] + ", (slow) " + logGammaSlow(a) + ", (approx) " + logGamma(a));
		if (a<TABULATION)
			return itsPrecomputed[a];

		return logGammaSlow(a);

		// alternative, continue from last computed value
//		double res = itsPrecomputed[TABULATION-1];
//		for (int i = TABULATION, j = a; i < j; ++i)
//			res += Math.log(i);
//		return res;
	}

	public static double logGammaSlow(int a)
	{
		double res = 0;
		for (int i=2; i<a; i++)
			res += Math.log(i);
		return res;
	}

	/**
	 * Counts the number of distinct float values in the supplied array.
	 * <p>
	 * Float.compare(float, float) is used, such that <code>NaN</code>
	 * values are considered equal, and <code>-0.0f</code> is considered
	 * different fom <code>0.0f</code>.
	 * <p>
	 * NOTE the original array is not modified, and need not be sorted.
	 *
	 * @param theArray to count the distinct values of.
	 *
	 * @return an int representing the number of distinct values.
	*/
	public static int getCardinality(float[] theArray)
	{
		if (theArray.length <= 1)
			return theArray.length;

		float[] fa = Arrays.copyOf(theArray, theArray.length);
		Arrays.sort(fa);

		// NOTE
		// first Float.compare() should not return 0, so aLast is set to
		// set to a value that is unequal to fa[0] (safe as itsSize > 1)
		int aCount = 0;
		Float aLast = (Float.isNaN(fa[0]) ? 0.0f : Float.NaN);
		for (float f : fa)
		{
			if (Float.compare(f, aLast) == 0) // NOTE -0.0 < 0.0
				continue;
			aLast = f;
			++aCount;
		}

		return aCount;
	}

	/**
	 * Returns a sorted array with all and only unique values occurring in
	 * the supplied array.
	 * <p>
	 * Float.compare(float, float) is used, such that <code>NaN</code>
	 * values are considered equal, and <code>-0.0f</code> is considered
	 * smaller than <code>-0.0f</code>.
	 * <p>
	 * NOTE the original array is not modified, and need not be sorted.
	 *
	 * @param theArray to select the distinct values of.
	 *
	 * @return a new float[] with all unique values of the supplied array
	 *         in ascending order.
	*/
	public static float[] getUniqueValues(float[] theArray)
	{
		if (theArray.length <= 1)
			return Arrays.copyOf(theArray, theArray.length);

		float[] fa = Arrays.copyOf(theArray, theArray.length);
		Arrays.sort(fa);

		// NOTE
		// first Float.compare() should not return 0, so aLast is set to
		// set to a value that is unequal to fa[0] (safe as itsSize > 1)
		int idx = 0;
		Float aLast = fa[0];
		for (int i = 1; i < fa.length; ++i)
		{
			float f = fa[i];
			if (Float.compare(f, aLast) == 0) // NOTE -0.0 < 0.0
				continue;
			aLast = f;
			fa[++idx] = f;
		}

		if (idx+1 < fa.length)
			fa = Arrays.copyOf(fa, idx+1);

		return fa;
	}
}
