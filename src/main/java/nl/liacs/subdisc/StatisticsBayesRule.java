package nl.liacs.subdisc;

import java.util.*;

/**
 For a {@link Subgroup} description ({@link Subgroup#getConditions()} with its
 corresponding <code>BitSet</code>), the statistics to calculate Bayes Rule.
 */
public class StatisticsBayesRule
{
	private BitSet itsX;
	private BitSet itsTarget;

	private double itsPT; 
//	private double itsPNotT;
	private double itsPXGivenT;
	private double itsPNotXGivenT;
	private double itsPXGivenNotT;
	private double itsPNotXGivenNotT;

	private double[] probabilitiesDataPXGivenT;
	private double[] probabilitiesDataPXGivenNotT;

	/*
	 * a number of things is wrong with this class
	 * 1. size() returns the size of the BitSet, this is not logically
	 *    tied to the size of the Table / nrRows
	 *    currently they are the same, but this is just a coincidence and
	 *    has not always been this way
	 * 2. cardinality() is called a number of times for the same BitSets
	 *    it is O(n), and very expensive on large data, as the BitSet will
	 *    not fit in cpu cache
	 * 3. the computation of these simple statistics does not need a clone
	 *    of potentially large BitSets, from which only cardinality and size
	 *    are required
	 *    there are better ways to compute these statistics
	 */
	public StatisticsBayesRule(BitSet theX, BitSet theTarget)
	{
		itsX = theX;
		itsTarget = theTarget;
		itsPT = (double) itsTarget.cardinality()/itsTarget.size();
//		itsPNotT = 1.0 - itsPT;
		System.out.println("PT:");
		System.out.println(itsPT);
		System.out.println("size target vector");
		System.out.println(itsTarget.size());

		//estimate probabilities P(X=1|T), P(X=0|T), P(X=1|not T) and P(X=0|not T)
		BitSet aNotTarget = (BitSet)itsTarget.clone();
		aNotTarget.flip(0,aNotTarget.size()); // or size +1?

		BitSet aX = (BitSet)theX.clone();
		BitSet aNotX = (BitSet)theX.clone();
		aNotX.flip(0,aNotX.size());

		aX.and(itsTarget);
		itsPXGivenT = (double) aX.cardinality()/itsTarget.cardinality();
		itsPNotXGivenT = 1-itsPXGivenT;

		aNotX.and(aNotTarget);
		itsPNotXGivenNotT = (double) aNotX.cardinality()/aNotTarget.cardinality();
		itsPXGivenNotT = 1-itsPNotXGivenNotT;

		// save the probabilities corresponding to the data points in a vector,
		// so we do not have to recalculate these probabilities each time
		probabilitiesDataPXGivenT = new double[itsX.size()];// or size -1?
		probabilitiesDataPXGivenNotT = new double[itsX.size()]; // or size -1?

		for (int i=0;i<itsX.size();i++)
		{
			if (itsX.get(i))
			{
				// point in the data is a 'member' of subgroup description', so P(X|T) estimate is P(X=1|T)
				probabilitiesDataPXGivenT[i] = itsPXGivenT;
				probabilitiesDataPXGivenNotT[i] = itsPXGivenNotT;
			}
			else
			{
				//point in the data is not a member of subgroup description so P(X|T) is P(X=0|T)
				probabilitiesDataPXGivenT[i] = itsPNotXGivenT;
				probabilitiesDataPXGivenNotT[i] = itsPNotXGivenNotT;
			}
		}
	}

	double[] getProbabilitiesDataPXGivenT()
	{
		return probabilitiesDataPXGivenT;
	}

	double[] getProbabilitiesDataPXGivenNotT()
	{
		return probabilitiesDataPXGivenNotT;
	}
}
