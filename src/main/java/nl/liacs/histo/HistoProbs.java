package nl.liacs.histo;

import java.util.Arrays;

public class HistoProbs
{
	private final double[] x;
	private final double[] y;
	private final int nr_items; // might be useful for later

	/** data is not modified */
	HistoProbs(double[] data)
	{
		// copy - data remains unchanged
		double[] tmp = Arrays.copyOf(data, data.length);

		// 2-stage process, single Map would uses a lot of memory
		x = Vec.uniqueSorted(tmp);
		y = Vec.getCounts(data, x);
		nr_items = data.length;

		// convert counts into probabilities
		Vec.divide(y, nr_items);
System.out.format("HistoProbs |N|=%d E(y)=%f%n", x.length, Vec.sum(y));
	}

	double[] getCounts(double[] data)
	{
		return Vec.getCounts(data, x);
	}

	// might all be moved to Vec
	double[] ewBins(int nrBins)
	{
		double min = x[0];
		double max = x[x.length-1];
		double interval = (max-min)/nrBins;

		double[] range = Vec.range(min+interval, interval, nrBins);
		// rounding error might cause: range[last] < max
		range[range.length] = max;

		return range;
	}

	// naive  - last bin will be smallest
	// return may be smaller than nrBins as empty bins are removed
	double[] ehBins(int nrBins)
	{
		double[] out = new double[nrBins];
		double expected = 1.0 / nrBins;

		int bounds = 0;
		double sum = 0.0;
		for (int i = 0, j = out.length; i < j; ++i)
		{
			sum += y[i];
			if (sum >= expected)
			{
				out[bounds] = x[i];
				++bounds;
				sum = 0.0;
			}
		}

		return Arrays.copyOf(out, bounds);
	}

	double[] fayyadIraniBins()
	{
		return null;
	}
}
