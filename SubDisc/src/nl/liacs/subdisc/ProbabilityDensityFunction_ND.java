package nl.liacs.subdisc;

import java.io.*;
import java.util.*;

import Jama.*;

public class ProbabilityDensityFunction_ND
{
	private static final double CUTOFF = 3.0; // |CUTOFF*SIGMA| = 0.0
	private static final int SAMPLES = 35; // [-LIMIT:+LIMIT]
	private static final int GRID_STATS = 3; // min, max, samples
	private static final int MIN = 0;
	private static final int MAX = 1;
	private static final int GRID_SIZE = 2;
	// #_RESOLUTION is used like SAMPLES
	// getDensityDifference2D() scales by (dxdy*X_R*Y_R)
	// so with sqrt(2), (dxdy*X_R*Y_R) takes twice as long
	// the larger the #_RESOLUTION the closer the integral is to 1.0
	// but a #_RESOLUTION of 1 or sqrt(2) is good enough 
	private static final double X_RESOLUTION = Math.sqrt(2.0);
	private static final double Y_RESOLUTION = Math.sqrt(2.0);

	// structured as: [ x1d1, x1d2, ..., x1dd, x2d1, x2d2, x2dd, ..., xndd ]
	private final float[] itsData;
	// structured as: [ d1[min,max,n], d2[min,max,n], ... , dd[min,max,n] ]
	private final double[][] itsGrid;
	// XXX MM - will replace itsGrid
	// structured as: [ d1[min,max], d2[min,max], ... , dd[min,max] ]
	private final double[][] itsLimits;

	ProbabilityDensityFunction_ND(Column[] theColumns)
	{
		this(theColumns, CUTOFF, SAMPLES);
	}

	ProbabilityDensityFunction_ND(Column[] theColumns, double theCutoff, int theNrSamples)
	{
		itsData = getData(theColumns);
		itsGrid = getGrid(theColumns, theCutoff, theNrSamples);
		itsLimits = getLimits(theColumns);
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
			throw new ArrayIndexOutOfBoundsException("TOO MANY GRID POINTS");

		double[] stats = new double[GRID_STATS];
		stats[0] = min;
		stats[1] = max;
		stats[2] = n;

		return stats;
	}

	private static final double[][] getLimits(Column[] theColumns)
	{
		double[][] limits = new double[theColumns.length][2];
		for (int i = 0; i < theColumns.length; ++i)
		{
			Column c = theColumns[i];
			limits[i] = new double[] { c.getMin(), c.getMax() };
		}
		return limits;
	}

	public final int getNrDimensions() { return itsGrid.length; }

	// 2D version
	public final float[][] getDensity(BitSet theBitSet)
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
		double result = 0.0;

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
	// FIXME MM this can be done without using tmp arrays, see L2
	private static final double mahalanobisSquared(float[] x, float[] theMu, float[][] theCovarianceMatrixInverse, float[] diff, double[] transpose)
	{
		final int d = x.length;
		double result = 0.0f;
		Arrays.fill(transpose, 0.0);

		// (x-mu)
		for (int i = 0; i < d; ++i)
			diff[i] = (x[i]-theMu[i]);
		// (x-mu)^T * theCovarianceMatrix
		// tmp to loop over covariance matrix by row, not column
		for (int i = 0; i < d; ++i)
		{
			float f = diff[i];
			float[] r = theCovarianceMatrixInverse[i];
			for (int j = 0; j < d; ++j)
				transpose[j] += (f*r[j]);
		}
		// ((x-mu)^T * theCovarianceMatrix) * (x-mu)
		for (int i = 0; i < d; ++i)
			result += (transpose[i]*diff[i]);

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

	// indexes for statistics
	private static final int MEAN_D1 = 0;
	private static final int MEAN_D2 = 1;
	private static final int VAR_D1 = 2;
	private static final int VAR_D2 = 3;
	private static final int COV_1_2 = 4;
	private static final int H = 5;
	private static final int SIZE_N = 6;
	private static final int NR_STATS = 7;
	// single-pass, numerically stable, where cov_n = co-moment_n / n
	// statistics for 2D, subgroup versus complement
	// (isSample == true) uses unbiased n-1, (isSample == false) uses n
	private static final double[][] stats(float[] theData, BitSet theBitSet, boolean isSample)
	{
		int D = 2;
		int N = theData.length/D;

		double[] sg_stats = new double[NR_STATS];
		double[] co_stats = new double[NR_STATS];

		// Welford (Knuth TAOCP volume 2, 3rd edition, page 232)
		// en.wikipedia.org/wiki/Algorithms_for_calculating_variance
		for (int i = 0, j = 0; i < N; ++i, ++j)
		{
			double[] stats = theBitSet.get(i) ? sg_stats : co_stats;
			double x = theData[  j];
			double y = theData[++j];			// NOTE increment
			double n = stats[SIZE_N] += 1.0;		// n
			double dx = x - stats[MEAN_D1];			// x_n - ux_n-1
			double dy = y - stats[MEAN_D2];			// y_n - ux_n-1
			stats[MEAN_D1] += (dx/n);			// ux_n = ux_n-1 + (x_n - ux_n-1)
			stats[MEAN_D2] += (dy/n);			// uy_n = uy_n-1 + (y_n - uy_n-1)
			stats[VAR_D1] += (dx * (x-stats[MEAN_D1]));	// varx_n = varx_n-1 + ((x_n - ux_n-1)*(x_n - ux_n))
			stats[VAR_D2] += (dy * (y-stats[MEAN_D2]));	// vary_n = vary_n-1 + ((y_n - uy_n-1)*(y_n - uy_n))
			stats[COV_1_2] = (((stats[COV_1_2] * (n-1.0)) + (dx*(y-stats[MEAN_D2]))) / n);
			// covxy_n = [{covxy_n-1 * (n-1)} + {((n-1)/n) * ((x_n - ux_n-1) * (y_n - uy_n))}] / n
		}

		double sg_n = sg_stats[SIZE_N];
		double co_n = co_stats[SIZE_N];

		// bias corrections - too complicated within loop
		double div = sg_n - (isSample ? 1.0 : 0.0);
		sg_stats[VAR_D1] /= div;
		sg_stats[VAR_D2] /= div;
		if (isSample)
			sg_stats[COV_1_2] *= (sg_n/div);
		// for complement
		div = co_n - (isSample ? 1.0 : 0.0);
		co_stats[VAR_D1] /= div;
		co_stats[VAR_D2] /= div;
		if (isSample)
			co_stats[COV_1_2] *= (co_n/div);

		// compute (Silverman) h, for 2D: d_fac cancels out (see below)
		sg_stats[H] = Math.pow(sg_n, -(1.0/6.0));
		co_stats[H] = Math.pow(co_n, -(1.0/6.0));

		return new double[][] { sg_stats, co_stats };
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

	private static final float[][] createBandwidthMatrix(double[] stats)
	{
		double h = stats[H];
		double h2 = h*h;
		return new float[][] { {(float)(stats[VAR_D1] * h2), (float)(stats[COV_1_2] * h2)},
					{(float)(stats[COV_1_2] * h2), (float)(stats[VAR_D2] * h2)} };
	}

	private static final float[][] inverse(float[][] theMatrix)
	{
		if (!isSquared(theMatrix))
			throw new IllegalArgumentException("Not a squared matrix");

		int d = theMatrix.length;

		// 2D special case: 1/det(M) * [d,-b][-c,a]
		if (d == 2)
		{
			float f = det(theMatrix);
			float[][] inv = new float[2][2];
			inv[0][0] = +theMatrix[1][1]/f;
			inv[0][1] = -theMatrix[0][1]/f;
			inv[1][0] = -theMatrix[1][0]/f;
			inv[1][1] = +theMatrix[0][0]/f;
			return inv;
		}

		return toFloat(new Matrix(toDouble(theMatrix), d, d).inverse().getArray());
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
			return (float) new Matrix(toDouble(theMatrix)).det();
	}

	// NO CHECKS!!! assumes 2x2 Matrix covariance matrix ([0][1]==[1][0])
	private static final boolean isDegenerate(float[][] theMatrix)
	{
		return ((theMatrix[0][0] == 0.0f) ||
			(theMatrix[1][1] == 0.0f) ||
			(Math.abs(theMatrix[0][1]) == 1.0f));
	}

	private static final boolean isSquared(float[][] theMatrix)
	{
		int r = theMatrix.length;
		for (float[] fa : theMatrix)
			if (fa.length != r)
				return false;
		return true;
	}

	private static final float[][] toFloat(double[][] theMatrix)
	{
		int d = theMatrix.length;
		// to float[][]
		float[][] fa = new float[d][d];
		for (int i = 0; i < d; ++i)
		{
			fa[i] = new float[d];
			for (int j = 0; j < d; ++j)
				fa[i][j] = (float) theMatrix[i][j];
		}
		return fa;
	}

	private static final double[][] toDouble(float[][] theMatrix)
	{
		int d = theMatrix.length;
		// to double[][]
		double[][] da = new double[d][d];
		for (int i = 0; i < d; ++i)
		{
			da[i] = new double[d];
			for (int j = 0; j < d; ++j)
				da[i][j] = theMatrix[i][j];
		}
		return da;
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

	float getDensityDifference(BitSet theSubgroup, boolean toComplement, QM theQM)
	{
		if (theQM.TARGET_TYPE != TargetType.MULTI_NUMERIC)
			throw new IllegalArgumentException(theQM + " != " + TargetType.MULTI_NUMERIC);
		if (theSubgroup == null)
			throw new IllegalArgumentException("theSubgroup can not be null");
		if (theSubgroup.cardinality() == 0)
			throw new IllegalArgumentException("theSubgroup can not be empty");

		if (theQM == QM.L2)
			return (float) L2(theSubgroup, toComplement);
		else
		{
			if (itsGrid.length != 2)
				throw new IllegalArgumentException(theQM + " only implemented for 2D");
			return getDensityDifference2D(theSubgroup, toComplement, theQM)[0][0];
		}
	}

	// boolean toComplement:
	// true for subgroup versus complement, false for subgroup versus data
	//
	// if theQM != null this call returns just a single number
	// it represents the difference according to theQM, it is in float[0][0]
	/** toComplement boolean is ignored, always test against complement */
	final float[][] getDensityDifference2D_FIXED_GRID(BitSet theSubgroup, boolean toComplement, QM theQM)
	{
		assert (itsGrid.length == 2);
		assert (itsData.length >= 2);
		assert (theSubgroup != null);
		assert (theSubgroup.cardinality() > 0);

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
		double dxdy = dx*dy;

		int D = itsGrid.length;
		int N = itsData.length/D;

		// S = subgroup, C = complement or complete data
		double[][] stats = stats(itsData, theSubgroup, true);
		int S_size = (int)stats[0][SIZE_N];
		int C_size = (int)stats[1][SIZE_N];
		float[][] S_cm = createBandwidthMatrix(stats[0]);
		float[][] C_cm = createBandwidthMatrix(stats[1]);
		float[][] S_cm_inv = inverse(S_cm);
		float[][] C_cm_inv = inverse(C_cm);
		// for direct computation
		double S_xvar_i = S_cm_inv[0][0];
		double S_cov2_i = S_cm_inv[0][1] * 2.0; // 0.0 for diagonal H
		double S_yvar_i = S_cm_inv[1][1];
		double C_xvar_i = C_cm_inv[0][0];
		double C_cov2_i = C_cm_inv[0][1] * 2.0; // 0.0 for diagonal H
		double C_yvar_i = C_cm_inv[1][1];

		boolean qm_only = (theQM != null);
		float[][] densityDifference = qm_only ? null : new float[x_n][y_n];
		double difference = 0.0;
		// Kh = 1/n*sum(1/h*K(x/h)), 1/n*1/sqrt(2*PI)^k*|SIGMA|)
		double S_f = dxdy / (2.0 * Math.PI * Math.sqrt(det(S_cm)) * S_size);
		double C_f = dxdy / (2.0 * Math.PI * Math.sqrt(det(C_cm)) * C_size);

////////////////////////////////////////////////////////////////////////////////
debug("\nSG");
debug("stats " + Arrays.toString(stats[0]));
debug("cm    " + Arrays.toString(S_cm[0]) + "\n      " + Arrays.toString(S_cm[1]));
debug("cm^-1 " + Arrays.toString(S_cm_inv[0]) + "\n      " + Arrays.toString(S_cm_inv[1]));
debug("!SG");
debug("stats " + Arrays.toString(stats[1]));
debug("cm    " + Arrays.toString(C_cm[0]) + "\n      " + Arrays.toString(C_cm[1]));
debug("cm^-1 " + Arrays.toString(C_cm_inv[0]) + "\n      " + Arrays.toString(C_cm_inv[1]));
debug("NOTE if ((#_xvar * #_yvar) < dxdy) then #_intregral < 1.0");
debug(String.format("      dx=%f\t      dy=%f\t     dxdy=%f", dx, dy, dxdy));
debug(String.format(" SG_xvar=%f\t SG_yvar=%f\txvar*yvar=%f", S_cm[0][0], S_cm[1][1], (S_cm[0][0]*S_cm[1][1])));
debug(String.format("!SG_xvar=%f\t!SG_yvar=%f\txvar*yvar=%f", C_cm[0][0], C_cm[1][1], (C_cm[0][0]*C_cm[1][1])));
Timer t = new Timer();
double S_kde_integral = 0.0;
double C_kde_integral = 0.0;
////////////////////////////////////////////////////////////////////////////////
		for (int i = 0; i < x_n; ++i)
		{
if (i % 100 == 0)
System.out.println("ROW:"+i);
			float[] r = qm_only ? null : new float[y_n];
			if (!qm_only)
				densityDifference[i] = r;

			double x = (x_min + (i*dx)); // x-coord
			for (int j = 0; j < y_n; ++j)
			{
				double y = (y_min + (j*dy)); // y-coord

				double S_kde = 0.0;
				double C_kde = 0.0;
				for (int k = 0, m = 0; k < N; ++k, ++m)
				{
					double px = x - itsData[  m];
					double 	py = y - itsData[++m]; // NOTE increment

					// TODO MM CUTOFF checks go here

					if (theSubgroup.get(k))
						S_kde += Math.exp(-0.5 * ((px*px*S_xvar_i) + (px*py*S_cov2_i) + (py*py*S_yvar_i)));
					// toComplement check would go here
					else
						C_kde += Math.exp(-0.5 * ((px*px*C_xvar_i) + (px*py*C_cov2_i) + (py*py*C_yvar_i)));
				}
				S_kde *= S_f;
				C_kde *= C_f;
				S_kde_integral += S_kde;
				C_kde_integral += C_kde;

				if (!qm_only)
					r[i] = (float)(S_kde-C_kde);
				else
					difference += divergence(theQM, S_kde, C_kde, S_size, N);

				// no need to continue
				if (Double.isInfinite(difference))
				{
					debug(String.format("[%d,%d]=(%20.16f,%20.16f) %20.16f %20.16f", i, j, x, y, S_kde, C_kde));
					debug("QM = " + difference);
					debug(t.getElapsedTimeString());
					return new float[][] { {(float)difference}, null };
				}
			}
		}
System.out.println("S_kde_integral = " + S_kde_integral);
System.out.println("C_kde_integral = " + C_kde_integral);

		if (qm_only)
		{
			debug("QM = " + difference);
			debug(t.getElapsedTimeString());
			return new float[][] { {(float)difference}, null };
		}

		debug(t.getElapsedTimeString());
		return densityDifference;
	}

public double[] lastDXDY = null;
	// GRID IS CREATED BASED ON SUBGROUP AND COMPLEMENT STATISTICS
	public final float[][] getDensityDifference2D(BitSet theSubgroup, boolean toComplement, QM theQM)
	{
		assert (itsGrid.length == 2);
		assert (itsData.length >= 2);
		assert (theSubgroup != null);
		assert (theSubgroup.cardinality() > 0);

		int D = itsGrid.length;
		int N = itsData.length/D;

		// S = subgroup, C = complement or complete data
		double[][] stats = stats(itsData, theSubgroup, true);
		int S_size = (int)stats[0][SIZE_N];
		int C_size = (int)stats[1][SIZE_N];
		float[][] S_cm = createBandwidthMatrix(stats[0]);
		float[][] C_cm = createBandwidthMatrix(stats[1]);
		// can not invert degenerate matrix
		if (isDegenerate(S_cm) || isDegenerate(C_cm))
		{
			System.out.format("%nERROR: degenerate covariance matrix: %n SG %s%n    %s%n!SG %s%n    %s%n",
						Arrays.toString(S_cm[0]),
						Arrays.toString(S_cm[1]),
						Arrays.toString(C_cm[0]),
						Arrays.toString(C_cm[1]));
			return new float[][] { { 0.0f }, null };
		}

		float[][] S_cm_inv = inverse(S_cm);
		float[][] C_cm_inv = inverse(C_cm);
		// for direct computation
		double S_xvar_i = S_cm_inv[0][0];
		double S_cov2_i = S_cm_inv[0][1] * 2.0; // 0.0 for diagonal H
		double S_yvar_i = S_cm_inv[1][1];
		double C_xvar_i = C_cm_inv[0][0];
		double C_cov2_i = C_cm_inv[0][1] * 2.0; // 0.0 for diagonal H
		double C_yvar_i = C_cm_inv[1][1];

// SETUP OF GRID - BASED ON SUBGROUP | COMPLEMENT STATISTICS
// COULD ALL BE MOVED TO SEPARATE METHOD
		double S_xsigma = Math.sqrt(S_cm[0][0]);
		double S_ysigma = Math.sqrt(S_cm[1][1]);
		double C_xsigma = Math.sqrt(C_cm[0][0]);
		double C_ysigma = Math.sqrt(C_cm[1][1]);
		// extend x range using largest #_xsigma
		double x_ext = CUTOFF * Math.max(S_xsigma, C_xsigma);
		double x_min = itsLimits[0][MIN] - x_ext;
		double x_max = itsLimits[0][MAX] + x_ext;
		// extend y range using largest #_ysigma
		double y_ext = CUTOFF * Math.max(S_ysigma, C_ysigma);
		double y_min = itsLimits[1][MIN] - y_ext;
		double y_max = itsLimits[1][MAX] + y_ext;
		// smallest #_#sigma determines integration step size
		// NOTE the smaller d# is, the closer #_integral is to 1.0
		// but O(n) becomes O(n*X_RESOLUTION*Y_RESOLUTION)
		double dx = (Math.min(S_xsigma, C_xsigma) / X_RESOLUTION); 
		double dy = (Math.min(S_ysigma, C_ysigma) / Y_RESOLUTION);
		double dxdy = dx*dy;
		// number of samples (needed for densityDifference[x_n][y_n])
		double x_range = x_max-x_min;
		int x_n = (int)Math.floor(x_range / dx);
		double y_range = y_max-y_min;
		int y_n = (int)Math.floor(y_range / dy);
// SETUP OF GRID COMPLETE

// XXX TEMP
lastDXDY = new double[] { x_min, x_max, x_n, y_min, y_max, y_n, dx, dy };

////////////////////////////////////////////////////////////////////////////////
debug("\nSG");
debug("stats " + Arrays.toString(stats[0]));
debug("cm    " + Arrays.toString(S_cm[0]) + "\n      " + Arrays.toString(S_cm[1]));
debug("cm^-1 " + Arrays.toString(S_cm_inv[0]) + "\n      " + Arrays.toString(S_cm_inv[1]));
debug("!SG");
debug("stats " + Arrays.toString(stats[1]));
debug("cm    " + Arrays.toString(C_cm[0]) + "\n      " + Arrays.toString(C_cm[1]));
debug("cm^-1 " + Arrays.toString(C_cm_inv[0]) + "\n      " + Arrays.toString(C_cm_inv[1]));
debug("GRID");
debug(String.format("x_min=%f x_max=%f x_range=%f dx=%f x_n=%d (X_RESOLUTION=%f)", x_min, x_max, x_range, dx, x_n, X_RESOLUTION));
debug(String.format("y_min=%f y_max=%f y_range=%f dy=%f y_n=%d (Y_RESOLUTION=%f)", y_min, y_max, y_range, dy, y_n, Y_RESOLUTION));
if ((S_xsigma * S_ysigma < dxdy) || (C_xsigma * C_ysigma < dxdy))
	debug("ERROR");
debug("NOTE if ((#_xsigma * #_ysigma) < dxdy) then #_intregral < 1.0");
debug(String.format("        dx=%f\t        dy=%f\t         dxdy=%f", dx, dy, dxdy));
debug(String.format(" SG_xsigma=%f\t SG_ysigma=%f\txsigma*ysigma=%f", S_xsigma, S_ysigma, (S_xsigma * S_ysigma)));
debug(String.format("!SG_xsigma=%f\t!SG_ysigma=%f\txsigma*ysigma=%f", C_xsigma, C_ysigma, (C_xsigma * C_ysigma)));
Timer t = new Timer();
double S_kde_integral = 0.0;
double C_kde_integral = 0.0;
////////////////////////////////////////////////////////////////////////////////

		boolean qm_only = (theQM != null);
		float[][] densityDifference = qm_only ? null : new float[x_n][y_n];
		double difference = 0.0;
		// Kh = 1/n*sum(1/h*K(x/h)), 1/n*1/sqrt(2*PI)^k*|SIGMA|)
		double S_f = dxdy / (2.0 * Math.PI * Math.sqrt(det(S_cm)) * S_size);
		double C_f = dxdy / (2.0 * Math.PI * Math.sqrt(det(C_cm)) * C_size);

		for (int i = 0; i < x_n; ++i)
		{
			float[] r = qm_only ? null : new float[y_n];
			if (!qm_only)
				densityDifference[i] = r;

			double x = (x_min + (i*dx)); // x-coord
			for (int j = 0; j < y_n; ++j)
			{
				double y = (y_min + (j*dy)); // y-coord

				double S_kde = 0.0;
				double C_kde = 0.0;
				for (int k = 0, m = 0; k < N; ++k, ++m)
				{
					double px = x - itsData[  m];
					double py = y - itsData[++m]; // NOTE increment

					// TODO MM CUTOFF checks go here

					if (theSubgroup.get(k))
						S_kde += Math.exp(-0.5 * ((px*px*S_xvar_i) + (px*py*S_cov2_i) + (py*py*S_yvar_i)));
					// toComplement check would go here
					else
						C_kde += Math.exp(-0.5 * ((px*px*C_xvar_i) + (px*py*C_cov2_i) + (py*py*C_yvar_i)));
				}
				S_kde *= S_f;
				C_kde *= C_f;
				S_kde_integral += S_kde;
				C_kde_integral += C_kde;

				if (!qm_only)
					r[j] = (float)(S_kde-C_kde);
				else
					difference += divergence(theQM, S_kde, C_kde, S_size, N);

				// no need to continue
				if (Double.isInfinite(difference))
				{
					debug(String.format("[%d,%d]=(%20.16f,%20.16f) %20.16f %20.16f", i, j, x, y, S_kde, C_kde));
					debug("QM = " + difference);
					debug(t.getElapsedTimeString());
					return new float[][] { {(float)difference}, null };
				}
			}
		}
System.out.println("S_kde_integral = " + S_kde_integral);
System.out.println("C_kde_integral = " + C_kde_integral);

		if (qm_only)
		{
			debug("QM = " + difference);
			debug(t.getElapsedTimeString());
			return new float[][] { {(float)difference}, null };
		}

		debug(t.getElapsedTimeString());
		return densityDifference;
	}

	/*
	 * NOTE this code needs special cases for 0 and Infinity
	 * for example 0 * Infinity = NaN
	 * but for KL, P_i = 0.0 is not always 0.0
	 * it is just the exp(-x) that returned 0.0
	 * because |x| was to to large, the result is infinitesimally small
	 * then P_i*Infinite should be defined as Infinite (not 0.0)
	 */
	private static final double divergence(QM theQM, double P_i, double Q_i, int theCoverage, int theNrRecords)
	{
		assert(theCoverage > 0);
		assert(theNrRecords > 0);

		switch(theQM)
		{
			case SQUARED_HELLINGER_2D :
			{
				double d = Math.sqrt(P_i) - Math.sqrt(Q_i);
				return (0.5 * (d*d));
			}
			case SQUARED_HELLINGER_WEIGHTED_2D :
			{
				// avoid possible 0 * Inf
//				if (theCoverage == 0)
//					return 0.0;
				return (divergence(QM.SQUARED_HELLINGER_2D, P_i, Q_i, theCoverage, theNrRecords) * theCoverage) / theNrRecords;
			}
			case SQUARED_HELLINGER_WEIGHTED_ADJUSTED_2D :
				// now weight SQUARED_HELLINGER
				// magic number = maximum possible score
				// it lies at (5/9, 4/27)
				return divergence(QM.SQUARED_HELLINGER_WEIGHTED_2D, P_i, Q_i, theCoverage, theNrRecords) / (4.0/27.0);
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
				//if (P_i == 0.0)
				//	return 0.0;

				// NOTE the above is no longer true when testing
				// against complement
				// see javadoc comment
//				if (P_i == 0.0)
//				{
//					// (0/0)=NaN, log(NaN)=NaN, 0*NaN=NaN
//					if (Q_i == 0.0)
//						return Double.POSITIVE_INFINITY;
//					// else
//					// (0/x)=0, log(0)=-Inf, 0*-Inf=NaN
//					return Double.POSITIVE_INFINITY;
//				}
//				else if (Q_i == 0.0) // and P_i != 0.0
//				{
//					// (x/0)=+Inf, log(+Inf)=+Inf, x*+Inf=+Inf|-Inf
//					return P_i*Double.POSITIVE_INFINITY;
//				}
				// equivalent to the checks above
				if (P_i == 0.0)
					return Double.POSITIVE_INFINITY;
				return P_i * Math.log(P_i/Q_i);
			}
			case KULLBACK_LEIBLER_WEIGHTED_2D :
			{
				// avoid possible 0 * Inf
//				if (theCoverage == 0)
//					return 0.0;
				return (divergence(QM.KULLBACK_LEIBLER_2D, P_i, Q_i, theCoverage, theNrRecords) * theCoverage) / theNrRecords;
			}
			case CWRACC_2D :
			{
				// avoid possible 0 * Inf
//				if (theCoverage == 0)
//					return 0.0;
				return (divergence(QM.CWRACC_UNWEIGHTED_2D, P_i, Q_i, theCoverage, theNrRecords) * theCoverage) / theNrRecords;
			}
			case CWRACC_UNWEIGHTED_2D :
				return Math.abs(Q_i - P_i);
			default :
				throw new AssertionError(theQM);
		}
	}

	// will load a huge frame stack
	final double L2(BitSet theSubgroup, boolean toComplement)
	{
		int D = itsGrid.length;
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

		float[][] S_cm = getCovarianceMatrix(itsData, D, theSubgroup, S_mu);
		float[][] C_cm = getCovarianceMatrix(itsData, D, theNotSubgroup, C_mu);
		// V1+V2
		float[][] cm = new float[D][D];
		for (int i = 0; i < D; ++i)
		{
			float[] si = S_cm[i];
			float[] ci = C_cm[i];
			float[] cmi = new float[D];
			cm[i] = cmi;
			for (int j = 0; j < D; ++j)
				cmi[j] = (si[j] + ci[j]);
		}
		// (V1+V2)^-1
		float[][] cm_inv = inverse(cm);
		if (!isSquared(cm_inv))
			throw new AssertionError("SIGMA^-1 is not a squared 2D matrix");

		double L2 = 0.0;
		double f = (1.0 / (Math.pow(2.0 * Math.PI, D/2.0) * Math.sqrt(det(cm) * (S_size*C_size))));
		float[] x = new float[D];
		float[] diff = new float[D];
		for (int i = theSubgroup.nextSetBit(0); i >= 0; i = theSubgroup.nextSetBit(i+1))
		{
			// load subgroup vector data
			for(int j = 0, k = i*D; j < D; ++j, ++k)
				x[j] = itsData[k];
			for (int j = theNotSubgroup.nextSetBit(0); j >= 0; j = theNotSubgroup.nextSetBit(j+1))
			{
				// x - mu (here Subgroup-NotSubgroup)
				for (int k = 0, m = j*D; k < D; ++k, ++m)
					diff[k] = x[k]-itsData[m];
				// alternative mahalanobis squared
				// 1 running sum, no (temporary) array/matrix
				// uses fact that cm is squared and symmetric
				// loops over upper triangle only
				double M2 = 0.0;
				for (int k = 0; k < D; ++k)
				{
					double p = diff[k];
					float[] r = cm_inv[k];
					M2 += (p * p * r[k]);
					for (int m = k+1; m < D; ++m)
						M2 += (2.0 * p * diff[m] * r[m]);
				}
				L2 += Math.exp(-0.5 * M2);
			}
		}

		return f*L2;
	}

	// will load a huge frame stack
	// massive code duplication - will be refactored some day
	final double L2_(BitSet theSubgroup)
	{
		assert (itsGrid.length == 2);
		assert (itsData.length >= 2);
		assert (theSubgroup != null);
		assert (theSubgroup.cardinality() > 0);

		int D = itsGrid.length;
		int N = itsData.length/D;

		// S = subgroup, C = complement or complete data
		double[][] stats = stats(itsData, theSubgroup, true);
		int S_size = (int)stats[0][SIZE_N];
		int C_size = (int)stats[1][SIZE_N];
		float[][] S_cm = createBandwidthMatrix(stats[0]);
		float[][] C_cm = createBandwidthMatrix(stats[1]);

		// (V1+V2)
		// f2 = f^2 for Subgroup
		float[][] f2_cm = new float[D][D];
		for (int i = 0; i < D; ++i)
		{
			float[] si = S_cm[i];
			float[] cmi = new float[D];
			f2_cm[i] = cmi;
			for (int j = 0; j < D; ++j)
				cmi[j] = (si[j] + si[j]);
		}
		// fg = f*g for (Subgroup*!Subgroup)
		float[][] fg_cm = new float[D][D];
		for (int i = 0; i < D; ++i)
		{
			float[] si = S_cm[i];
			float[] ci = C_cm[i];
			float[] cmi = new float[D];
			fg_cm[i] = cmi;
			for (int j = 0; j < D; ++j)
				cmi[j] = (si[j] + ci[j]);
		}
		// g2 = g^2 for !Subgroup
		float[][] g2_cm = new float[D][D];
		for (int i = 0; i < D; ++i)
		{
			float[] si = S_cm[i];
			float[] ci = C_cm[i];
			float[] cmi = new float[D];
			g2_cm[i] = cmi;
			for (int j = 0; j < D; ++j)
				cmi[j] = (si[j] + ci[j]);
		}
//		// can not invert degenerate matrix
//		if (isDegenerate(cm))
//		{
//			System.out.format("%nERROR: degenerate covariance matrix: %n CM %s%n    %s%n SG %s%n    %s%n!SG %s%n    %s%n",
//						Arrays.toString(cm[0]),
//						Arrays.toString(cm[1]),
//						Arrays.toString(S_cm[0]),
//						Arrays.toString(S_cm[1]),
//						Arrays.toString(C_cm[0]),
//						Arrays.toString(C_cm[1]));
//			return 0.0;
//		}
		// (V1+V2)^-1
		float[][] f2_cm_inv = inverse(f2_cm);
		float[][] fg_cm_inv = inverse(fg_cm);
		float[][] g2_cm_inv = inverse(g2_cm);


		double f2 = 0.0;
		double fg = 0.0;
		double g2 = 0.0;
		// L2 = f^2 - 2fg + g^2
		double L2 = 0.0;

		double f2_fac = (1.0 / (Math.pow(2.0 * Math.PI, D/2.0) * Math.sqrt(det(f2_cm) * (S_size*S_size))));
		double fg_fac = (1.0 / (Math.pow(2.0 * Math.PI, D/2.0) * Math.sqrt(det(fg_cm) * (S_size*C_size))));
		double g2_fac = (1.0 / (Math.pow(2.0 * Math.PI, D/2.0) * Math.sqrt(det(g2_cm) * (C_size*C_size))));

		float[] x = new float[D]; // cache data vector outer loop
		float[] diff = new float[D]; // tmp - will be re-used many times

		for (int i = 0; i < N; ++i)
		{
			boolean inSG1 = theSubgroup.get(i);
			for (int j = 0, k = i*D; j < D; ++j, ++k)
				x[j] = itsData[k];

			for (int j = 0; j < N; ++j)
			{
				boolean inSG2 = theSubgroup.get(j);
				// diff = x-mu
				for (int k = 0, m = j*D; k < D; ++k, ++m)
					diff[k] = x[k] - itsData[m];

				double M2 = 0.0;
				// f^2 -> subgroup only part
				// diff = x-mu (here Subgroup[i]-Subgroup[j])
				if (inSG1 && inSG2)
				{
					// alternative mahalanobis squared
					// 1 running sum, no (temporary) array/matrix
					// uses fact that #_cm is squared and symmetric
					// loops over upper triangle only
					for (int k = 0; k < D; ++k)
					{
						double p = diff[k];
						float[] r = f2_cm_inv[k];	// f2_cm_inv
						M2 += (p * p * r[k]);
						for (int m = k+1; m < D; ++m)
							M2 += (2.0 * p * diff[m] * r[m]);
					}
					f2 += Math.exp(-0.5 * M2);
				}
				// f*g or g*f - > final result requires (fg * 2)
				// diff = x-mu (here Subgroup[i]-!Subgroup[j])
				else if (inSG1 ^ inSG2)
				{
					for (int k = 0; k < D; ++k)
					{
						double p = diff[k];
						float[] r = fg_cm_inv[k];	// fg_cm_inv
						M2 += (p * p * r[k]);
						for (int m = k+1; m < D; ++m)
							M2 += (2.0 * p * diff[m] * r[m]);
					}
					fg += Math.exp(-0.5 * M2);
				}
				// g^2 -> !subgroup only part
				// diff = x-mu (here !Subgroup[i]-!Subgroup[j])
				else // (!inSG1 && !inSG2)
				{
					for (int k = 0; k < D; ++k)
					{
						double p = diff[k];
						float[] r = g2_cm_inv[k];	// g2_cm_inv
						M2 += (p * p * r[k]);
						for (int m = k+1; m < D; ++m)
							M2 += (2.0 * p * diff[m] * r[m]);
					}
					g2 += Math.exp(-0.5 * M2);
				}
			}
		}

		f2 *= f2_fac;
		fg *= fg_fac;
		g2 *= g2_fac;
		L2 = (f2 + (2.0*fg) + g2);
debug(String.format("L2 = %f - (2 * %f) + %f = %f%n", f2, fg, g2, L2));

		return L2;
	}

	private static final boolean DEBUG = true;
	private static final void debug(String theMessage)
	{
		if (DEBUG)
			System.out.println(theMessage);
	}

	public static void main(String[] args)
	{
		//File f = new File("/home/marvin/data/svn/SubgroupDiscovery/SubDisc/Regr_datasets/boston-housing.csv");
		File f = new File("/home/marvin/data/wrk/svn/subgroupDiscovery/SubDisc/Regr_datasets/boston-housing.csv");
		Table t = new DataLoaderTXT(f).getTable();
		Column[] c = { t.getColumn(0), t.getColumn(13)};
		ProbabilityDensityFunction_ND pdf = new ProbabilityDensityFunction_ND(c);
		BitSet b = new BitSet(t.getNrRows());
		b.set(0, t.getNrRows());
		b.set(0, 50, false);
		b.set(100, 150, false);
		double d = pdf.L2_(b);
//		float[][] d = pdf.getDensityDifference2D(b, true, QM.KULLBACK_LEIBLER_2D);

//		check();
	}

	private static final void check()
	{
		// NOTE
		// Math.exp(x) is +Infinity for all x >= 710
		// Math.exp(-x) is 0 for all x >= 746
		for (int i = 0; i < 1000; ++i)
		{
			System.out.println("i = " + i);
			System.out.println("exp(i) = " + Math.exp(i));
			System.out.println("exp(-i) = " + Math.exp(-i));
		}

		// base cases
		System.out.println(0.0 / 0.0);
		System.out.println(0.0 / Double.POSITIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY / 0.0);
		System.out.println(Double.POSITIVE_INFINITY / Double.POSITIVE_INFINITY);
		System.out.println();
		System.out.println(0.0 * Double.POSITIVE_INFINITY);
		System.out.println(0.0 * Double.NEGATIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY * Double.POSITIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY * Double.NEGATIVE_INFINITY);
		System.out.println();
		System.out.println(1.0 * Double.POSITIVE_INFINITY);
		System.out.println();
		System.out.println(Double.POSITIVE_INFINITY - Double.POSITIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY - Double.NEGATIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY + Double.POSITIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY + Double.NEGATIVE_INFINITY);
		System.out.println();
		System.out.println(Double.NEGATIVE_INFINITY - Double.POSITIVE_INFINITY);
		System.out.println(Double.NEGATIVE_INFINITY - Double.NEGATIVE_INFINITY);
		System.out.println(Double.NEGATIVE_INFINITY + Double.POSITIVE_INFINITY);
		System.out.println(Double.NEGATIVE_INFINITY + Double.NEGATIVE_INFINITY);
	}
}
