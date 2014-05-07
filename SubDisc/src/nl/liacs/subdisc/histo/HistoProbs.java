package nl.liacs.subdisc.histo;

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
		Vec.div(y, nr_items);
	}

	double[] getCounts(double[] data)
	{
		return Vec.getCounts(data, x);
	}
}
