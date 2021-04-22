package nl.liacs.subdisc.histo;

import java.util.Arrays;

public class Vec
{
	/** data will be modified - return is unique, sorted domain */
	/*
	 * data is sorted first, then unique values are retrieved from it
	 * this is done using an array (not Tree), to restrict memory use
	 * Tree(Map) Nodes are very large, only when the ratio unique/total is
	 * very low (say < 1/8, depends on system configuration / JVM), a Tree
	 * is more memory efficient
	 * but even then, it is a very unpredictable data structure
	 */
	static final double[] uniqueSorted(double[] data)
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
	static final double[] compact(double[] data)
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

	private static final double[] ILLEGAL =
	{ Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };

	static final boolean isValid(double[] data)
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

	static final boolean isSorted(double[] data)
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
	static final boolean contains(double[] data, double... keys)
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
	static final boolean containsSorted(double[] data, double... keys)
	{
		for (int i = 0, j = keys.length; i < j; ++i)
			if (Arrays.binarySearch(data, keys[i]) >= 0)
				return true;

		// none of the keys found
		return false;
	}

	/*
	 * VECTOR METHODS
	 **********************************************************************/

	/** data is NOT modified */
	// not safe against overflow and rounding errors
	static final double sum(double[] data)
	{
		double sum = 0.0;
		for (int i = 0, j = data.length; i < j; ++i)
			sum += data[i];
		return sum;
	}

	/** data is NOT modified */
	// not safe against overflow and rounding errors or divide_by_zero
	private static final double average(double[] data)
	{
		return sum(data) / data.length;
	}

	/** data is NOT modified */
	// not safe against overflow and rounding errors or divide_by_zero
	private static final double ssd(double[] data)
	{
		double average = average(data);
		double ssd = 0.0;
		for (int i = 0, j = data.length; i < j; ++i)
		{
			double d = (data[i] - average);
			ssd += (d * d);
		}
		return ssd;
	}

	/**
	 * (sample == false): variance = sum_squared_deviations / n <br>
	 * (sample == true):  variance = sum_squared_deviations / (n-1)
	 * 
	 * data is NOT modified
	 */
	// not safe against overflow and rounding errors or divide_by_zero
	private static final double variance(double[] data, boolean sample)
	{
		return ssd(data) / (data.length - (sample ? 1 : 0));
	}

	/**
	 * (sample == false): std_dev = sqrt(sum_squared_deviations / n) <br>
	 * (sample == true):  std_dev = sqrt(sum_squared_deviations / (n-1))
	 * 
	 * data is NOT modified
	 */
	// not safe against overflow and rounding errors or divide_by_zero
	private static final double std_dev(double[] data, boolean sample)
	{
		return Math.sqrt(variance(data, sample));
	}

	/** vector div - data is modified */
	// for private use only - no checks on data or divisor
	static final void div(double[] data, double divisor)
	{
		for (int i = 0, j = data.length; i < j; ++i)
			data[i] /= divisor;
	}

	static void print(double[] data)
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
	static final double[] getCounts(double[] data, double[] keys)
	{
		double[] out = new double[keys.length];

		// safe up till 2^53 integers (> max arrays size)
		for (int i = 0, j = data.length; i < j; ++i)
			out[Arrays.binarySearch(keys, data[i])] += 1.0;

		return out;
	}
}
