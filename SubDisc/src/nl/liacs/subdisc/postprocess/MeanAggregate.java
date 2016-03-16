package nl.liacs.subdisc.postprocess;

import java.util.*;

public class MeanAggregate
{
	private final List<Mean> itsMeans;
	private final double itsAggregate;

	MeanAggregate(List<Mean> theMeans)
	{
		assert (canAggregate(theMeans));
		itsMeans = Collections.unmodifiableList(theMeans);

		int i = 0;
		double sum = 0.0;
		for (Mean m : itsMeans)
		{
			if (!Double.isNaN(m.itsMean))
			{
				++i;
				sum += m.itsMean;
			}
		}

		itsAggregate = sum/i;
	}

	List<Mean> getMeans() { return itsMeans; }
	double getAggregate() { return itsAggregate; }

	private static final boolean canAggregate(List<Mean> theMeans)
	{
		Mean pivot = theMeans.get(0);
		for (Mean m : theMeans)
			if (!pivot.canAggregate(m))
				return false;

		return true;
	}
}
