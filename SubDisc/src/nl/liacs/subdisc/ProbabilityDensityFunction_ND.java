package nl.liacs.subdisc;

import java.util.*;

public class ProbabilityDensityFunction_ND
{
	private static final double CUTOFF = 4.0; // |CUTOFF*SIGMA| = 0.0
	private static final int SAMPLES = 101; // [-LIMIT:+LIMIT]
	private static final int GRID_STATS = 3; // min, max, samples
	private static final int MIN = 0;
	private static final int MAX = 1;
	private static final int GRID_SIZE = 2;

	// structured as: [ x1d1, x1d2, ..., x1dd, x2d1, x2d2, x2dd, ..., xndd ]
	private final float[] itsData;
	// structured as: [ d1[min,max,n], d2[min,max,n], ... , dd[min,max,n] ]
	private final double[][] itsGrid;

	ProbabilityDensityFunction_ND(Column[] theColumns)
	{
		this(theColumns, CUTOFF, SAMPLES);
	}

	ProbabilityDensityFunction_ND(Column[] theColumns, double theCutoff, int theNrSamples)
	{
		itsData = getData(theColumns);
		itsGrid = getGrid(theColumns, theCutoff, theNrSamples);
	}

	private static final float[] getData(Column[] theColumns)
	{
		int n = theColumns[0].size();
		int d = theColumns.length;

		// max array size = Integer.MAX_VALUE = 2^31-1
		// should be large enough even for large data at high dimensions
		// for example, 1.000.000 records * 20 dimensions still fits
		float[] data = new float[n*d];
		for (int i = 0; i < n; ++i)
			for (int j = 0, k = (i*d); j < d; ++j, ++k)
				data[k] = theColumns[j].getFloat(i);

		return data;
	}

	private static final double[][] getGrid(Column[] theColumns, double theCutoff, int theNrSamples)
	{
		int length = theColumns.length;
		double[][] stats = new double[length][GRID_STATS];
		// helper
		int size = theColumns[0].size();
		BitSet b = new BitSet(size);
		b.set(0, size);

		for (int i = 0; i < length; ++i)
			stats[i] = getGrid(theColumns[i], theCutoff, theNrSamples, b);

		return stats;
	}

	private static final double[] getGrid(Column theColumn, double theCutoff, int theNrSamples, BitSet theBitSet)
	{
		double min = theColumn.getMin();
		double max = theColumn.getMax();
		double ssd = theColumn.getStatistics(theBitSet, false)[1];
		double var = ssd / theColumn.size();
		double sigma = Math.sqrt(var);

		// extend grid to both sides
		double ext = theCutoff*sigma;
		min -= ext;
		max += ext;
		double range = max-min;
		double n = Math.floor((range / sigma) * theNrSamples);
System.out.format("min=%f max=%f range=%f sigma=%f n=%f%n", min, max, range, sigma, n);

		if (n > Integer.MAX_VALUE)
			throw new ArrayIndexOutOfBoundsException("NO MANY GRID POINTS");

		double[] stats = new double[GRID_STATS];
		stats[0] = min;
		stats[1] = max;
		stats[2] = n;

		return stats;
	}

	// 2D version
	final float[][] getDensity(BitSet theBitSet)
	{
		double[] x_stats = itsGrid[0];
		double[] y_stats = itsGrid[1];
		double x_min = x_stats[MIN];
		double y_min = y_stats[MIN];
		double x_max = x_stats[MAX];
		double y_max = y_stats[MAX];
		int x_n = (int)x_stats[GRID_SIZE];
		int y_n = (int)y_stats[GRID_SIZE];
		double dx = (x_max-x_min)/x_n;
		double dy = (y_max-y_min)/y_n;

		// mu vector and covariance matrix according to theBitSet
		float[] mu = getMuVector(itsData, 2, theBitSet);
		float[][] cm = getCovarianceMatrix(itsData, 2, theBitSet, mu);
		// inverse cm
		float[][] cm_inv = inverse(cm);
		if (!isSquared(cm_inv))
			throw new AssertionError("SIGMA^-1 is not a squared 2D matrix");

		// points too far are set to 0.0
		double x_std_dev = Math.sqrt(cm[0][0]);
		double y_std_dev = Math.sqrt(cm[1][1]);
		double x_cutoff = CUTOFF * x_std_dev;
		double y_cutoff = CUTOFF * y_std_dev;

		float[][] densities = new float[x_n][y_n];
		int n = theBitSet.cardinality();
		// Kh = 1/n*sum(1/h*K(x/h)), 1/n*1/sqrt(2*PI)^k*|SIGMA|)
		double f = (float)(1.0 / (2.0 * Math.PI * Math.sqrt(det(cm) * n)));

//		float[] xy = new float[2]; // re-used
//		for (int i = 0; i < x_n; ++i)
//		{
//			float[] r = new float[y_n];
//			densities[i] = r;
//
//			xy[0] = (float) (x_min + (i*dx)); // x-coord
//			for (int j = 0; j < y_n; ++j)
//			{
//				xy[1] = (float) (y_min + (j*dy)); // y-coord
//				// test xy against each point in (BitSet) data
//				r[j] = (float) (f * qf(xy, cm_inv, itsData, theBitSet));
//			}
//		}

		// alternative - direct computation
		double xvar_i = cm_inv[0][0];
		double cov2_i = cm_inv[0][1] * 2.0; // 0.0 for diagonal H
		double yvar_i = cm_inv[1][1];
		for (int i = 0; i < x_n; ++i)
		{
if (i % 1000 == 0)
System.out.println("grid row: " + i);
			float[] r = new float[y_n];
			densities[i] = r;

			double x = (x_min + (i*dx)); // x-coord
			for (int j = 0; j < y_n; ++j)
			{
				double y = (y_min + (j*dy)); // y-coord

				// test xy against each point in (BitSet) data
				double density = 0.0;
				for (int k = theBitSet.nextSetBit(0); k >= 0; k = theBitSet.nextSetBit(k+1))
				{
					int idx = k*2;
					double x_ = x-itsData[idx];	// (x-mu_x)
					double y_ = y-itsData[idx+1];	// (y-mu_y)
					if (Math.abs(x_/x_std_dev) > x_cutoff)
						continue;
					if (Math.abs(y_/y_std_dev) > y_cutoff)
						continue;
					// ellipse CUTOFF check would go here
					// if (x^2 + y^2 > z)
					density += Math.exp(-0.5 * ((x_*x_*xvar_i) + (x_*y_*cov2_i) + (y_*y_*yvar_i)));
				}
				r[j] = (float)(f * density);
			}
		}

		return densities;
	}

	/*
	 * d-dimensional x
	 * d*d covariance matrix
	 * n*d data[], uses for (x-mu)
	 * n BitSet (selects subset from data points)
	 */
	private static final double qf(float[] x, float[][] theCovarianceMatrixInverse, float[] theData, BitSet theBitSet)
	{
		final int d = x.length;
		final float[] mu = new float[d];
		final float[] diff = new float[d];
		final double[] transpose = new double[d];
		double result = 0.0f;

		for (int i = theBitSet.nextSetBit(0); i >= 0; i = theBitSet.nextSetBit(i+1))
		{
			// set next point as mu vector
			for (int j = 0, k = (i*d); j < d; ++j, ++k)
				mu[j] = theData[k];
			// add weight of N((x-mu)/h) to total
			result += Math.exp(-0.5 * mahalanobisSquared(x, mu, theCovarianceMatrixInverse, diff, transpose));
		}

		return result;
	}

	/** Input is NOT modified. */
	static final double mahalanobisSquared(float[] x, float[] theMu, float[][] theCovarianceMatrixInverse)
	{
		// uses two temporary arrays as scratchpad
		return mahalanobisSquared(x, theMu, theCovarianceMatrixInverse, new float[x.length], new double[x.length]);
	}

	// diff and transpose are used as scratchpad and modified
	private static final double mahalanobisSquared(float[] x, float[] theMu, float[][] theCovarianceMatrixInverse, float[] diff, double[] transpose)
	{
		final int d = x.length;
		double result = 0.0f;

		for (int i = 0; i >= 0; ++i)
		{
			// (x-mu)
			for (int j = 0, k = i*d; j < d; ++j, ++k)
				diff[j] = (x[j]-theMu[k]);

			// (x-mu)^T * theCovarianceMatrix
			// tmp to loop over covariance matrix by row, not column
			for (int j = 0; j < d; ++j)
			{
				float f = diff[j];
				float[] r = theCovarianceMatrixInverse[j];
				for (int k = 0; k < d; ++k)
					transpose[k] += (f*r[k]);
			}

			// ((x-mu)^T * theCovarianceMatrix) * (x-mu)
			for (int j = 0; j < d; ++j)
				result += (transpose[j]*diff[j]);
		}

		return result;
	}

	// TODO MM summation may have overflow or catastrophic cancellation
	private static final float[] getMuVector(float[] theData, int theNrDimensions, BitSet theBitSet)
	{
		float[] mus = new float[theNrDimensions];
		for (int i = theBitSet.nextSetBit(0); i>= 0; i = theBitSet.nextSetBit(i+1))
			for (int j = 0, k = (i*theNrDimensions); j < theNrDimensions; ++j, ++k)
				mus[j] += theData[k];

		int n = theBitSet.cardinality();
		for (int i = 0; i < theNrDimensions; ++i)
			mus[i] /= n;

		return mus;
	}

	// for now only use the diagonal cm suggested by Silverman
	private static final float[][] getCovarianceMatrix(float[] theData, int theNrDimensions, BitSet theBitSet, float[] theMus)
	{
		float h = h_silverman(theNrDimensions, theBitSet.cardinality());
		float[] stdDevs = stdDev(theData, theNrDimensions, theBitSet, theMus);

		float[][] cm = new float[theNrDimensions][theNrDimensions];
		for (int i = 0; i < theNrDimensions; ++i)
		{
			float h_ii = (h*stdDevs[i]);
			cm[i][i] = (h_ii*h_ii);
		}

		return cm;
	}

	// Silverman's rule suggests using
	// \sqrt{\mathbf{H}_{ii}} = \left(\frac{4}{d+2}\right)^{\frac{1}{d+4}} n^{\frac{-1}{d+4}} \sigma_i
	// where \sigma_i is the standard deviation of the ith variable and \mathbf{H}_{ij} = 0, i\neq j.
	private static float h_silverman(int theNrDimensions, int theDataSize)
	{
		// constants
		int d = theNrDimensions;
		double n = theDataSize;
		double pow = 1.0 / (d+4.0);
		//
		double d_fac = Math.pow((4.0 / (d+2.0)) , pow);
		double n_fac = Math.pow(n, -pow);
		double fac = d_fac * n_fac;

		return (float)fac;
	}

	// not safe against overflow and rounding errors or divide_by_zero
	private static final float[] stdDev(float[] theData, int theNrDimensions, BitSet theBitSet, float[] theMus)
	{
		float[] ssds = new float[theNrDimensions];
		for (int i = theBitSet.nextSetBit(0); i >= 0; i= theBitSet.nextSetBit(i+1))
		{
			for (int j = 0, k = (i*theNrDimensions); j < theNrDimensions; ++j, ++k)
			{
				float f = (theData[k] - theMus[j]);
				ssds[j] += (f*f);
			}
		}

		// always use n-1
		int n = theBitSet.cardinality()-1;
		for (int i = 0; i < theNrDimensions; ++i)
			ssds[i] = (float) Math.sqrt(ssds[i] / n);

		return ssds;
	}

	private static final float[][] inverse(float[][] theMatrix)
	{
		// 2D special case: 1/det(M) * M
		if (theMatrix.length == 2)
		{
			float f = det(theMatrix);
			float[][] inv = new float[2][2];
			inv[0][0] = theMatrix[0][0]/f;
			inv[0][1] = theMatrix[0][1]/f;
			inv[1][0] = theMatrix[1][0]/f;
			inv[1][1] = theMatrix[1][1]/f;
			return inv;
		}
		else
			throw new AssertionError("Not a 2D matrix");
	}

	private static final float det(float[][] theMatrix)
	{
		if (!isSquared(theMatrix))
			throw new AssertionError("Not a squared matrix");

		int d = theMatrix.length;
		if (d == 2)
			// ad-bc
			return (theMatrix[0][0]*theMatrix[1][1])-(theMatrix[0][1]*theMatrix[1][0]);
		else
			throw new AssertionError("Not a 2D matrix");
	}

	private static final boolean isSquared(float[][] theMatrix)
	{
		int r = theMatrix.length;
		for (float[] fa : theMatrix)
			if (fa.length != r)
				return false;
		return true;
	}

//	// single-pass, numerically stable, based on cov_n = co-moment_n / n
//	private static final double cov_cm(float[] data, int theNrDimensions, BitSet theBitSet, boolean isSample)
//	{
//		float[] means = new float[theNrDimensions];
//		for (int i = 0; i < theNrDimensions; ++i)
//			means[i] = data[i];
//		float[] covs = new float[theNrDimensions][theNrDimensions];
//
//		// simplified: NOTE uses i for n-1
//		for (int i = 1, n = 2; i < x.length; ++i, ++n)
//		{
//			double dx = x[i]-mean_x;
//			double dy = y[i]-mean_y;
//			cov = (((cov * i) + ((i/(double)n) * (dx*dy))) / n);
//			mean_x += (dx/n);
//			mean_y += (dy/n);
//		}
//
//		// bias correction - too complicated within loop
//		if (isSample)
//			//cov *= (x.length/(x.length-1.0));
//			cov += (cov /(x.length-1));
//		return cov;
//	}

	// 3D version
	private static final double[][] getDensity3D(BitSet theBitSet)
	{
		return null;
	}

	// boolean toComplement:
	// true for subgroup versus complement, false for subgroup versus data
	//
	// if theQM != null this call returns just a single number
	// it represents the difference according to theQM, is in float[0][0]
	//
	// most computations could be faster by looping over data only once
	// by combining subgroup versus not subgroup statistics
	// current implementation is simple alternative
	// NOTE when testing to data ALL statistics are recomputed for data
	// for now, the assumption is that tests are subgroup versus complement
	//
	final float[][] getDensityDifference2D(BitSet theSubgroup, boolean toComplement, QM theQM)
	{
		double[] x_stats = itsGrid[0];
		double[] y_stats = itsGrid[1];
		double x_min = x_stats[MIN];
		double y_min = y_stats[MIN];
		double x_max = x_stats[MAX];
		double y_max = y_stats[MAX];
		int x_n = (int)x_stats[GRID_SIZE];
		int y_n = (int)y_stats[GRID_SIZE];
		double dx = (x_max-x_min)/x_n;
		double dy = (y_max-y_min)/y_n;

		int D = itsData.length/itsGrid.length;
		int N = itsData.length/D;
		// S = subgroup, C = complement or complete data
		int S_size = theSubgroup.cardinality();
		int C_size = N - (toComplement ? S_size : 0);

		// create a BitSet for NotSubgroup
		// very inefficient but simple
		BitSet theNotSubgroup = new BitSet(N);
		theNotSubgroup.set(0, N);
		if (toComplement)
			theNotSubgroup.xor(theSubgroup);

		// mu vector and covariance matrix according to theBitSet
		float[] S_mu = getMuVector(itsData, D, theSubgroup);
		float[] C_mu = getMuVector(itsData, D, theNotSubgroup);
//		if (toComplement)
//		{
//			double S_frac = S_size / N;
//			double C_frac = C_size / N;
//			C_mu = new float[S_mu.length];
//			for (int i = 0; i < S_mu.length; ++i)
//				C_mu[i] = (float)((itsMus[i]-(S_frac*S_mu[i]))/C_frac);
//		}
//		else
//			C_mu = itsMus;

		float[][] S_cm = getCovarianceMatrix(itsData, 2, theSubgroup, S_mu);
		float[][] C_cm = getCovarianceMatrix(itsData, 2, theNotSubgroup, C_mu);
		// inverse cm
		float[][] S_cm_inv = inverse(S_cm);
		float[][] C_cm_inv = inverse(C_cm);
		if (!isSquared(S_cm_inv) || !isSquared(C_cm_inv))
			throw new AssertionError("SIGMA^-1 is not a squared 2D matrix");

		boolean qm_only = (theQM != null);
		float[][] densityDifference = qm_only ? null : new float[x_n][y_n];
		double difference = 0.0;
		// Kh = 1/n*sum(1/h*K(x/h)), 1/n*1/sqrt(2*PI)^k*|SIGMA|)
		double S_f = (float)(1.0 / (2.0 * Math.PI * Math.sqrt(det(S_cm) * S_size)));
		double C_f = (float)(1.0 / (2.0 * Math.PI * Math.sqrt(det(C_cm) * C_size)));
		float[] xy = new float[2]; // re-used

		for (int i = 0; i < x_n; ++i)
		{
			float[] r = qm_only ? null : new float[y_n];
			if (!qm_only)
				densityDifference[i] = r;

			xy[0] = (float) (x_min + (i*dx)); // x-coord
			for (int j = 0; j < y_n; ++j)
			{
				xy[1] = (float) (y_min + (j*dy)); // y-coord
				// test xy against each point in Subgroup data
				double S_i = S_f * qf(xy, S_cm_inv, itsData, theSubgroup);
				// subtract NotSubgroup data
				double C_i = (float) (C_f * qf(xy, C_cm_inv, itsData, theNotSubgroup));

				if (!qm_only)
					r[i] = (float)(S_i-C_i);
				else
					difference += divergence(theQM, S_i, C_i, S_size, N);
			}
		}

		if (qm_only)
			return new float[][] { {(float)difference}, null };
		return densityDifference;
	}

	private static final double divergence(QM theQM, double P_i, double Q_i, int theCoverage, int theNrRecords)
	{
		switch(theQM)
		{
			case SQUARED_HELLINGER_2D :
			{
				double d = Math.sqrt(P_i) - Math.sqrt(Q_i);
				return (0.5 * (d*d));
			}
			case SQUARED_HELLINGER_WEIGHTED_2D :
				return (divergence(QM.SQUARED_HELLINGER_2D, P_i, Q_i, 0, 0) * theCoverage) / theNrRecords;
			case SQUARED_HELLINGER_WEIGHTED_ADJUSTED_2D :
				// now weight SQUARED_HELLINGER
				// magic number = maximum possible score
				// it lies at (5/9, 4/27)
				return divergence(QM.SQUARED_HELLINGER_WEIGHTED_2D, P_i, Q_i, 0, 0) / (4.0/27.0);
			case KULLBACK_LEIBLER_2D :
			{
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
				if (P_i == 0.0)
					return 0.0;
				return P_i * Math.log(P_i/Q_i);
			}
			case KULLBACK_LEIBLER_WEIGHTED_2D :
				return (divergence(QM.KULLBACK_LEIBLER_2D, P_i, Q_i, 0, 0) * theCoverage) / theNrRecords;
			case CWRACC_2D :
				return (divergence(QM.CWRACC_UNWEIGHTED_2D, P_i, Q_i, 0, 0) * theCoverage) / theNrRecords;
			case CWRACC_UNWEIGHTED_2D :
				return Math.abs(Q_i - P_i);
			default :
				throw new AssertionError(theQM);
		}
	}
}
