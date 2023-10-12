package nl.liacs.subdisc.histo;

import java.util.Arrays;

public class Vec
{
	/*
	 * VECTOR METHODS - VALIDITY CHECKS
	 **********************************************************************/

	private static final double[] ILLEGAL =
	{ Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };

	public static final boolean isValid(double[] data)
	{
		if (data == null)
			return false;
		if (!isSorted(data))
			return false;
		// use version tuned to sorted arrays
		if (containsSorted(data, ILLEGAL))
			return false;

		return true;
	}

	public static final boolean isSorted(double[] data)
	{
		// 0 or 1 element array is always sorted
		// NOTE throws NullPointerException when (data == null)
		if (data.length <= 1)
			return true;

		// NOTE Arrays.sort(double[]) results in total ordering
		// using Double.compare(d1, d2) ensures
		// -0.0 and 0.0 are handled correctly, as are NaNs
		for (int i = 0, j = data.length-1; i < j; ++i)
			if (Double.compare(data[i], data[i+1]) > 0)
				return false;

		// each value is less than or equal to next value
		return true;
	}

	/**
	 * returns true if any of the keys is found
	 * O(k * n); where k=keys.length and n=data.length
	 * 
	 * Double.NaN is considered by this method to be equal to itself and
	 * greater than all other double values (including 
	 * Double.POSITIVE_INFINITY).
	 * 0.0d is considered by this method to be greater than -0.0d.
	 */
	public static final boolean contains(double[] data, double... keys)
	{
		for (int i = 0, j = data.length, k = keys.length; i < j; ++i)
		{
			double d = data[i];
			for (int m = 0; m < k; ++m)
				if (Double.compare(d, keys[m]) == 0)
					return true;
		}

		// none of the keys found
		return false;
	}

	/**
	 * returns true if any of the keys is found
	 * result is undefined when data is unsorted
	 * O(k * log(n)); where k=keys.length and n=data.length
	 * 
	 * Double.NaN is considered by this method to be equal to itself and
	 * greater than all other double values (including
	 * Double.POSITIVE_INFINITY).
	 * 0.0d is considered by this method to be greater than -0.0d.
	 */
	public static final boolean containsSorted(double[] data, double... keys)
	{
		for (int i = 0, j = keys.length; i < j; ++i)
			if (Arrays.binarySearch(data, keys[i]) >= 0)
				return true;

		// none of the keys found
		return false;
	}

	/*
	 * VECTOR METHODS - ARITHMETIC
	 **********************************************************************/

	public static final double sum(float[] data)
	{
		double sum = 0.0;
		for (int i = 0, j = data.length; i < j; ++i)
			sum += data[i];

		return sum;
	}

	/** data is NOT modified */
	// not safe against overflow and rounding errors
	public static final double sum(double[] data)
	{
		double sum = 0.0;
		for (int i = 0, j = data.length; i < j; ++i)
			sum += data[i];

		return sum;
	}

	// no SQRT and CBRT for now, use POWER(1/2 | 1/3)
	private static enum Op{ ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER };

	/** vector minus - data is modified */
	// for private use only - no checks on data or divisor
	public static final double[] add(double[] data, double addition)
	{
		return calc(data, addition, Op.ADD);
	}

	/** vector minus - data is modified */
	// for private use only - no checks on data or divisor
	public static final double[] subtract(double[] data, double subtraction)
	{
		return calc(data, subtraction, Op.SUBTRACT);
	}

	/** vector multiply - data is modified */
	// for private use only - no checks on data or divisor
	public static final double[] multiply(double[] data, double factor)
	{
		return calc(data, factor, Op.MULTIPLY);
	}

	/** vector division - data is modified */
	// for private use only - no checks on data or divisor
	public static final double[] divide(double[] data, double divisor)
	{
		return calc(data, divisor, Op.DIVIDE);
	}

	/** vector power - data is modified */
	// for private use only - no checks on data or divisor
	public static final double[] power(double[] data, double power)
	{
		return calc(data, power, Op.POWER);
	}

	// FIXME MM operator type switch is this very slow: just duplicate code
	private static final double[] calc(double[] data, double modifier, Op type)
	{
		for (int i = 0, j = data.length; i < j; ++i)
			data[i] = calc(data[i], modifier, type);

		return data;
	}

	private static final double calc(double original, double modifier, Op type)
	{
		switch (type)
		{
			case ADD :	return original + modifier;
			case SUBTRACT :	return original - modifier;
			case MULTIPLY :	return original * modifier;
			case DIVIDE :	return original / modifier;
			case POWER :	return Math.pow(original,  modifier);
			default :	throw new IllegalArgumentException(type.toString());
		}
	}

	// assumes x is unique and sorted
	public static final double integral(double[] x, double[] y)
	{
		if (x.length != y.length)
			throw new IllegalArgumentException("|x| != |y|");

		if (x.length == 1)
			return Double.NaN;

		double sum = Math.abs(y[0] * Math.abs(x[0])); // FIXME MM
		for (int i = 1, j = x.length; i < j; ++i)
			sum += (y[i] * Math.abs(x[i]-x[i-1]));

		return sum;
	}

	/** DOES NOT REQUIRE y TO SUM TO 1, but integral(x,y) should +/- be 1 */
	public static final double expected_value(double[] x, double[] y)
	{
		if (x.length != y.length)
			throw new IllegalArgumentException("|x| != |y|");

		double e = 0.0;
		for (int i = 0, j = x.length; i < j; ++i)
			e += x[i] * y[i];

		return e/(sum(y));
	}

	/*
	 * VECTOR METHODS - STATISTICS
	 **********************************************************************/

	/** data is NOT modified - NaN is larger than any other number */
	public static final double minimum(double[] data)
	{
		double min = data[0];
		for (int i = 1, j = data.length; i < j; ++i)
			if (Double.compare(data[i], min) < 0)
				min = data[i];

		return min;
	}

	/** data is NOT modified - NaN is larger than any other number */
	public static final double maximum(double[] data)
	{
		double max = data[0];
		for (int i = 1, j = data.length; i < j; ++i)
			if (Double.compare(data[i], max) > 0)
				max = data[i];

		return max;
	}

	/** data is NOT modified */
	// not safe against overflow and rounding errors or divide_by_zero
	public static final double mean(double[] data)
	{
		return sum(data) / data.length;
	}

	/** data is NOT modified - sum of power deviation for central moments */
	// not safe against overflow and rounding errors or divide_by_zero
	// XXX could allow non-integer power for SQRT and CBRT
	public static final double spd(double[] data, int power)
	{
		double average = mean(data);

		double spd = 0.0;
		for (int i = 0, j = data.length; i < j; ++i)
		{
			double d = (data[i] - average);
			spd += Math.pow(d, power);
		}

		return spd;
	}

	/**
	 * (sample == false): variance = sum_squared_deviations / n <br>
	 * (sample == true):  variance = sum_squared_deviations / (n-1)
	 * 
	 * data is NOT modified
	 */
	// not safe against overflow and rounding errors or divide_by_zero
	public static final double variance(double[] data, boolean sample)
	{
		return spd(data, 2) / (data.length - (sample ? 1 : 0));
	}

	/**
	 * (sample == false): std_dev = sqrt(sum_squared_deviations / n) <br>
	 * (sample == true):  std_dev = sqrt(sum_squared_deviations / (n-1))
	 * 
	 * data is NOT modified
	 */
	// not safe against overflow and rounding errors or divide_by_zero
	public static final double std_dev(double[] data, boolean sample)
	{
		return Math.sqrt(variance(data, sample));
	}

	/** data is NOT modified, assumes data is sorted */
	public static final double iqr(double[] data)
	{
		int length = data.length;
		int mid = length/2;
		double q1 = data[mid/2];
		double q3 = data[(mid + (((length & 1) == 1) ? 1 : 0)) + (mid/2)];

		return q3-q1;
	}

	/** data is NOT modified, assumes data is sorted */
	public static final double median(double[] data)
	{
		int length = data.length;
		int mid = length/2;

		if ((length & 1) == 1)
			return data[mid];
		else
			return (data[mid-1] + data[mid]) / 2.0;
	}

	/** data is NOT modified - better not use this */
	public static final double skewNonParametric(double[] data)
	{
		double mean = mean(data);
		double std_dev = std_dev(data, false);
		double median = median(data);

		return skewNonParametric(mean, median, std_dev);
	}

	/** data is NOT modified - better not use this */
	public static final double skewNonParametric(double mean, double median, double std_dev)
	{
		return (mean-median) / std_dev;
	}

	/** data is NOT modified */
	public static final double skew(double[] data)
	{
		return centralMoment(data, 3, false) / Math.pow(variance(data, false), 1.5);
	}

	/** data is NOT modified */
	public static final double kurtosis(double[] data)
	{
		return (centralMoment(data, 4, false) / Math.pow(variance(data, false), 2.0)) - 3.0;
	}

	/**
	 * 0-th central moment mu_0 = 1
	 * 1-st central moment mu_1 = 0
	 * 2-nd central moment mu_2 = variance (sigma^2)
	 * 3-rd and 4-th  are used to define the standardised moments which are
	 * used to define skewness and kurtosis, respectively.
	 * 
	 * data is NOT modified
	 */
	public static final double centralMoment(double[] data, int moment, boolean sample)
	{
		switch (moment)
		{
			case 0 : return 1.0;
			case 1 : return 0.0;
			case 2 : return variance(data, sample);
			//case 2 : return spd(data, 2) / data.length;
			case 3 : return spd(data, 3) / data.length;
			case 4 : return spd(data, 4) / data.length;
			default : throw new IllegalArgumentException("choose moment = {0,1,2,3,4}");
		}
	}

	/**
	 * 1-st standardised moment = 0
	 * 2-nd standardised moment = 1
	 * 3-rd standardised moment = skewness
	 * 4-th standardised moment = kurtosis
	 * 
	 * data is NOT modified
	 */
	public static final double standardisedMoment(double[] data, int moment, boolean sample)
	{
		switch (moment)
		{
			case 1 : return 0.0;
			case 2 : return 1.0;
			case 3 : return skew(data);
			case 4 : return kurtosis(data);
			default : throw new IllegalArgumentException("choose moment = {1,2,3,4}");
		}
	}

	/*
	 * VECTOR METHODS - KERNEL DENSITY ESTIMATION
	 * http://en.wikipedia.org/wiki/Histogram
	 * http://en.wikipedia.org/wiki/Kernel_density_estimation
	 * NOTE n^(1/3) = cbrt(n)
	 **********************************************************************/

	// k = ceil( (max-min) / h )
	public static final double nrBins(double min, double max, double h)
	{
		return Math.ceil((max-min) / h);
	}

	/** data is NOT modified */
	public static final double k_doane(double[] data)
	{
		int n = data.length;
		double g1 = skew(data);
		double sigma_g1 = Math.sqrt((6.0*(n-2)) / ((n+1)*(n+3)));

		return k_doane(n, g1, sigma_g1);
	}

	public static final double k_doane(int n, double g1, double sigma_g1)
	{
		return 1.0 + log2(n) + log2(1.0 + (Math.abs(g1) / sigma_g1));
	}

	/*
	 * Another approach is to use Sturges' rule: use a bin so large that
	 * there are about 1+ log_2(n) non-empty bins (Scott, 2009).
	 * This works well for n under 200, but was found to be inaccurate for
	 * large n. For a discussion and an alternative approach, see
	 * Birge and Rozenholc.
	 */
	// k = ceil( (max-min) / h ), where h = 2*IQR / cbrt(n)
	/** data is NOT modified */
	public static final double k_freemanDiaconis(double[] data)
	{
		double min = minimum(data);
		double max = maximum(data);
		double h = h_freemanDiaconis(data);

		return nrBins(min, max, h);
	}

	/** data is NOT modified */
	public static final double h_freemanDiaconis(double[] data)
	{
		return h_freemanDiaconis(data.length, iqr(data));
	}

	public static final double h_freemanDiaconis(int n, double iqr)
	{
		return (2.0 * iqr) / Math.cbrt(n);
	}

	// k = ceil(2*cbrt(n))
	/** data is NOT modified */
	public static final double k_rice(double[] data)
	{
		return k_rice(data.length);
	}

	public static final double k_rice(int n)
	{
		return Math.ceil(2.0 * Math.cbrt(n));
	}

	/** data is NOT modified */
	// k = ceil( (max-min) / h ), where h = 3.5*std_dev(data) / cbrt(n)
	public static final double k_scott(double[] data)
	{
		double min = minimum(data);
		double max = maximum(data);
		double h = h_scott(data);

		return nrBins(min, max, h);
	}

	/** data is NOT modified */
	public static final double h_scott(double[] data)
	{
		return h_scott(data.length, std_dev(data, false));
	}

	public static final double h_scott(int n, double std_dev)
	{
		return (3.5 * std_dev) / Math.cbrt(n);
	}

	/** data is NOT modified */
	// k =  k = ceil( (max-min) / h ), where
	// h = ((4*(sample_std_dev(data)^5)) / 3n)^.2
	public static final double k_silverman(double[] data)
	{
		double min = minimum(data);
		double max = maximum(data);
		double h = h_silverman(data);

		return nrBins(min, max, h);
	}

	/** data is NOT modified */
	public static final double h_silverman(double[] data)
	{
		return h_silverman(data.length, std_dev(data, true), iqr(data));
	}

	// original legacy formula, approximately: 1.06 * sd_s * n^{-0.2}
	public static final double h_silverman_legacy(int n, double std_dev_sample)
	{
		return Math.pow(((4.0 * Math.pow(std_dev_sample, 5.0)) / (3.0 * n)), 0.2);
	}

	// new alternative suggestion: 0.9 * min(sd_s, IQR/1.34) * n^{-0.2}
	public static final double h_silverman(int n, double std_dev_sample, double iqr)
	{
		System.out.println("std_dev=" + std_dev_sample + " iqr=" + iqr);
		return 0.9 * Math.min(std_dev_sample, (iqr/1.34)) * Math.pow(n, -0.2);
	}

	/** data is NOT modified */
	public static final double k_squareRoot(double[] data)
	{
		return k_squareRoot(data.length);
	}

	public static final double k_squareRoot(int n)
	{
		return Math.sqrt(n);
	}

	/** data is NOT modified */
	// k = ceil(1+log_2(n))
	public static final double k_sturges(double[] data)
	{
		return k_sturges(data.length);
	}

	public static final double k_sturges(int n)
	{
		return Math.ceil(1.0 + log2(n));
	}

	// h = MISE
	//

	public static final double log2(double x)
	{
		return Math.log(x) / Math.log(2.0);
	}

	/*
	 * VECTOR METHODS - DOMAIN RELATED
	 **********************************************************************/

	/** data WILL be modified - return is unique, sorted domain */
	/*
	 * data is sorted first, then unique values are retrieved from it
	 * this is done using an array (not Tree), to restrict memory use
	 * Tree(Map) Nodes are very large, only when the ratio unique/total is
	 * very low (say < 1/8, depends on system configuration / JVM), a Tree
	 * is more memory efficient
	 * but even then, it is a very unpredictable data structure
	 */
	public static final double[] uniqueSorted(double[] data)
	{
		// This method uses the total order imposed by the method
		// Double.compareTo(java.lang.Double):
		// -0.0d is treated as less than value 0.0d and
		// Double.NaN is considered greater than any other value and all
		// Double.NaN values are considered equal.
		// n*log(n)
		Arrays.sort(data);

		return compact(data);
	}

	/** data WILL be modified, assumes data is sorted */
	public static final double[] compact(double[] data)
	{
		assert (isValid(data)); // not null, sorted, no ILLEGAL values

		final int length = data.length;
		if (length == 0)
			return new double[0];
		if (length == 1)
			return new double[] { data[0] };

		// no new array is created, old one is re-used
		int i = 0;
		for (int j = 1, k = length; j < k; )
		{
			int cmp = Double.compare(data[i], data[j]);
			if (cmp < 0)
			{
				++i;
				data[i] = data[j];
				++j;
			}
			else if (cmp == 0)
				++j;
			else //(cmp > 0)
				throw new AssertionError("data[i] > data[j]");
		}

		// return truncated data containing all unique values
		return Arrays.copyOf(data, i+1);
	}

	public static final double[] range(double start, double interval, int size)
	{
		double[] out = new double[size];

		for (int i = 0, j = size; i < j; ++i)
			out[i] = start + (i*interval);

		return out;
	}

	/** data is NOT modified */
	public static final double[] mirror(double[] z)
	{
		int size = (2 * z.length) - 1;
		double[] out = new double[size];

		// fast copy for right side
		System.arraycopy(z, 0, out, z.length-1, z.length);
		// loop and negate for left side
		for (int i = z.length-1, j = 0; i > 0; --i, ++j)
			out[j] = -z[i];

		return out;
	}

	/** data is NOT modified */
	public static void print(double[] data)
	{
		int digits = (int) Math.ceil(Math.log10(data.length));
		String format = String.format("%%%dd %%f%%n", digits);

		for (int i = 0, j = data.length; i < j; ++i)
			System.out.format(format, i, data[i]);
	}

	/*
	 * BINNING METHODS
	 **********************************************************************/

	/**
	 * occurrences of each x in data
	 * 
	 * assumes keys is a unique, sorted domain
	 * return is array of size keys.length, where each return[i] is the
	 * number of times keys[i] occurs in data
	 * this means that any data[i] that does not occur in keys, is not
	 * adding to any count
	 * 
	 * data and keys are NOT modified
	 */
	// for private use only - no checks, assumes every v in data is in keys
	public static final double[] getCounts(double[] data, double[] keys)
	{
		double[] out = new double[keys.length];

		// safe up till 2^53 integers (> max arrays size)
		for (int i = 0, j = data.length; i < j; ++i)
			out[Arrays.binarySearch(keys, data[i])] += 1.0;

		return out;
	}

	 /** data and keys are NOT modified */
	// as above, but uses values in bins as right bounds
	// when data contains values larger than bins[last], the sum of the
	// counts in the return will be less then data.length
	public static final double[] getBinCounts(double[] data, double[] bins)
	{
		double[] out = new double[bins.length];

		// safe up till 2^53 integers (> max arrays size)
		for (int i = 0, j = data.length; i < j; ++i)
		{
			int idx = Arrays.binarySearch(bins, data[i]);
			out[idx < 0 ? ~idx : idx] += 1.0;
		}

		return out;
	}
}
