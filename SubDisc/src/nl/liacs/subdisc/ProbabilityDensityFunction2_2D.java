package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.histo.*;
import nl.liacs.subdisc.FileHandler.Action;
import nl.liacs.subdisc.gui.*;
import Jama.*;

// for 2D now - should be extended to higher dimensions
// NOTE 2D N^2 Gaussian kernel smoothing can be decomposed into 2N
// this can create ellipses along the x and y axis
// but can take into account orientation (diagonal ellipses)
// these would require a Covariace matrix with entries outside the main diagonal
// beside the variances on the diagonal
public class ProbabilityDensityFunction2_2D
{
	private static final double CUTOFF = 4.0;
	private static final double SAMPLES = 101;

	private final double[][] data;
	private final double[][] hs;
	// NOTE L2=Euclidean-norm = sqrt(inner or dot product) (Euclidean space)
//	private double norm_h;
	private final double[] std_devs;
	// pre-compute grids instead of recomputing them time and time again
	// uses a lot of memory -> probably better to only store
	// grid_min[] and grid_dx[]
	// limits are at [0] and [|grid|-1]
	private final double[][] grids;
	private final double[][] density;

	/*
	 * data.length = number of variables (dimension)
	 * data[dimension][nr_rows]
	 * grids[dimension][range/dx]
	 * densitiy[dimension][range/dx]
	 * hs[dimension]
	 * std_devs[dimension]
	 */
	ProbabilityDensityFunction2_2D(double[][] data)
	{
		// no checks for now
		this.data = data;
		this.hs = ProbabilityDensityFunctionMV.h_silverman(this.data);
		this.std_devs = computeStdDevs(this.data);
		this.grids = computeGrids(this.data, this.hs, this.std_devs, -1);
		this.density = density(grids[0], grids[1], data, hs);
	}

	// NOTE this only computes the diagonal of the Covariance Matrix
	// so this bandwidth matrix contains only variances, not covariances
	private static final double[] computeHs(double[][] data)
	{
		double[] da = new double[data.length];
		for (int i = 0; i < da.length; ++i)
			da[i] = Vec.h_silverman(data[i]);
		return da;
	}

	// NOTE could use 2 running sum for 1-pass std_dev computation
	// std_dev_i = (1/n * sum_i_n(x_i)^2) - (1/n * sum_i_n(x_i))^2
	// but it may become unstable
	private static final double[] computeStdDevs(double[][] data)
	{
		double[] da = new double[data.length];
		for (int i = 0; i < da.length; ++i)
			da[i] = Vec.std_dev(data[i], false);
		return da;
	}

	private static final double[][] computeGrids(double[][] data, double[][] hs, double[] std_devs, int n)
	{
		double[][] daa = new double[data.length][];
		for (int i = 0; i < daa.length; ++i)
		{
			n = computeGridSize(data[i], hs[i==0 ? 0 : 1][i==0 ? 0 : 1], std_devs[i]);
			daa[i] = computeGrid(data[i], hs[i==0 ? 0 : 1][i==0 ? 0 : 1], std_devs[i], n);
		}
		return daa;
	}

	private static final double[][] computeGrids_old(double[][] data, double[] hs, double[] std_devs, int n)
	{
		// must be same number for all grids
		if (n <= 0)
			n = computeGridSize(data[0], hs[0], std_devs[0]);

		double[][] daa = new double[data.length][];
		for (int i = 0; i < daa.length; ++i)
			daa[i] = computeGrid(data[i], hs[i], std_devs[i], n);
		return daa;
	}

	private static final int computeGridSize(double[] data, double h, double std_dev)
	{
		double min = Vec.minimum(data);
		double max = Vec.maximum(data);
		double g_min = min - (CUTOFF*std_dev);
		double g_max = max + (CUTOFF*std_dev);
		double range = g_max-g_min;
		// s = sigmas covered by range
		double s = range/h;
		// 1 sigma=(samples/(2*CUTOFF))
		int k = (int) ((Math.ceil(s) * SAMPLES) / (2.0 * CUTOFF));
System.out.format("%f %f %f %f %f %d %n", g_min, g_max, range, h, s, k);
		return k;
	}

	private static final double[] computeGrid(double[] data, double h, double std_dev, int n)
	{
		double min = Vec.minimum(data);
		double max = Vec.maximum(data);
		double g_min = min - (CUTOFF*std_dev);
		double g_max = max + (CUTOFF*std_dev);
		double range = g_max-g_min;
		// dx = grid increment
		double dx = range/n;

		double[] da = new double[n];
		for (int i = 0; i < da.length; ++i)
			da[i] = g_min + (i*dx);
		// correct possible rounding error
		da[da.length-1] = g_max;

		return da;
	}

	private static final double[][] density(double[] x_grid, double[] y_grid, double[][] data, double[][] hs)
	{
		int d = data.length;
		int n = data[0].length;

		Matrix dm = new Matrix(n, d);
		for (int i = 0; i < n; ++i)
			for (int j = 0; j < d; ++j)
				dm.set(i , j, data[j][i]);

		Matrix cmm = new Matrix(d, d);
		// assume 2x2
		cmm.set(0, 0, hs[0][0]);
		cmm.set(0, 1, hs[0][1]);
		cmm.set(1, 0, hs[1][0]);
		cmm.set(1, 1, hs[1][1]);

		return density(x_grid, y_grid, dm, cmm).getArray();
	}

	// NOTE data is in 'SubDisc form', so column oriented
	// therefore the normal computation is changed from 
	// (X^T * CM^-1 * X) to (X * CM^-1 * X^T),
	// where X=(x-mu) and CM=CovarianceMatrix
	private static final Matrix density(double[] x_grid, double[] y_grid, Matrix data, Matrix cm)
	{
		int n = data.getRowDimension();
		int d = data.getColumnDimension();
		int max_di = d-1;

		Matrix densities = new Matrix(x_grid.length, y_grid.length);
		// cache inversion of covariance matrix
		Matrix cm_i = cm.inverse();
		// point matrix, cache to avoid creating new Objects
		Matrix p = new Matrix(1, d);
		double f = 1.0 / Math.sqrt(Math.pow(2.0*Math.PI, d) * cm.det());

		for (int i = 0; i < x_grid.length; ++i)
		{
			p.set(0, 0, x_grid[i]);
			for (int j = 0; j < y_grid.length; ++j)
			{
				p.set(0, 1, y_grid[j]);

				double density = 0.0;
				for (int k = 0; k < n; ++k)
				{
					Matrix diff = p.minus(data.getMatrix(k, k, 0, max_di));
					density += Math.exp(-0.5*diff.times(cm_i).times(diff.transpose()).get(0, 0));
				}
				density *= f;

				densities.set(i, j, density);
			}
		}
// densities.print(8, 6);
		return densities;
	}

	private static final double[][] density(double[][] grids, double[][] data, double[][] cm)
	{
		double[] x_data = data[0];
		double[] y_data = data[1];
		int d = data.length;
		int n = x_data.length;
		System.out.println("x=" + x_data.length);
		System.out.println("y=" + y_data.length);

		double xstd = Math.sqrt(cm[0][0]);
		double ystd = Math.sqrt(cm[1][1]);
		double rho = cm[0][1] / (xstd*ystd); // rho*xstd*ystd
		System.out.format("%f %f %f%n", xstd, ystd, rho);

		// create suitable matrix
		Matrix m = new Matrix(data, d, n);
		System.out.format("%d x %d%n" ,m.getRowDimension(), m.getColumnDimension());

		double[] x_grid = grids[0];
		int gxn = x_grid.length;
		double[] y_grid = grids[1];
		int gyn = grids[1].length;

		double[][] density = new double[gxn][gyn];
		for (int i = 0; i < gxn; ++i)
			density[i] = new double[gyn];

		Matrix CMI = new Matrix(cm).inverse();
		double f = (2.0*Math.PI*xstd*ystd*Math.sqrt(1.0-(rho*rho)));
		double fn = f * (gxn*gyn);

		for (int i = 0; i < gxn; ++i)
		{
			// get grid-point (x,-)
			double gpx = x_grid[i];
			// cache density row
			double[] d_row = density[i];

			for (int j = 0; j < gyn; ++j)
			{
				// get grid-point (-,y)
				double gpy = y_grid[j];

				// distance gp to every point in data
				double p = 0.0;
				for (int k = 0; k < n; ++k)
					p += g(gpx, gpy, x_data[k], y_data[k], xstd, ystd, rho);
				p /= fn;

				d_row[j] = p;
			}
		}

		return density;
	}

	private static final double g(double x, double y, double xmu, double ymu, double xstd, double ystd, double rho)
	{
		double f = -1.0 / (2.0*(1.0-(rho*rho)));

		double dx = x-xmu;
		double dy = y-ymu;

		double xfac = h(dx, xstd);
		double yfac = h(dy, ystd);
		double xyfac = h2(dx, dy, xstd, ystd, rho);

		return Math.exp(f  * (xfac + yfac - xyfac));
	}

	private static final double h(double d, double sigma)
	{
		return (d*d) / (sigma*sigma);
	}

	private static final double h2(double d1, double d2, double sigma1, double sigma2, double rho)
	{
		return (2.0*rho*d1*d2) / (sigma1*sigma2);
	}

	// sqrt(inner and dot product) -> sqrt(X^T * X) -> Vec.dot(X, X)
	private static final double computeL2(double[] vec)
	{
		double d = 0.0;
		for (int i = 0; i < vec.length; ++i)
			d += (vec[i] * vec[i]);
		return Math.sqrt(d);
	}

	private static final double cov1(double[] x, double[] y, boolean isSample)
	{
		double sum_x = 0.0;
		double sum_y = 0.0;
		double sum_xy = 0.0;

		for (int i = 0; i < x.length; ++i)
		{
			sum_x += x[i];
			sum_y += y[i];
			sum_xy += (x[i]*y[i]);
		}

		int n = x.length - (isSample ? 1 : 0);
		return (sum_xy - ((sum_x*sum_y)/x.length)) / n;
	}

	// two-pass, more stable than single pass
	private static final double cov(double[] x, double[] y, boolean isSample)
	{
		double mean_x = Vec.mean(x);
		double mean_y = Vec.mean(y);

		int n = x.length - (isSample ? 1 : 0);
		double cov = 0.0;
		for (int i = 0; i < x.length; ++i)
			cov += (((x[i]-mean_x)*(y[i]-mean_y)) / n);

		return cov;
	}

	// single-pass, numerically stable, based on cov_n = co-moment_n / n
	private static final double cov_cm(double[] x, double[] y, boolean isSample)
	{
		// assume |x| > 0, mean and covariance for single point xy[0]
		double mean_x = x[0];
		double mean_y = y[0];
		double cov = 0.0;

//		for (int i = 1, n = 2; i < x.length; ++i, ++n)
//		{
//			cov = (((cov * (n-1)) + (((n-1.0)/n) * ((x[i]-mean_x)*(y[i]-mean_y)))) / n);
//			// mean_x_n = mean_x_n-1 + ((x[n]-mean_x_n-1)/n)
//			mean_x = (mean_x + ((x[i]-mean_x)/n));
//			// mean_y_n = mean_y_n-1 + ((y[n]-mean_y_n-1)/n)
//			mean_y = (mean_y + ((y[i]-mean_y)/n));
//		}
		// simplified: NOTE uses i for n-1
		for (int i = 1, n = 2; i < x.length; ++i, ++n)
		{
			double dx = x[i]-mean_x;
			double dy = y[i]-mean_y;
			cov = (((cov * i) + ((i/(double)n) * (dx*dy))) / n);
			mean_x += (dx/n);
			mean_y += (dy/n);
		}

		// bias correction - too complicated within loop
		if (isSample)
			//cov *= (x.length/(x.length-1.0));
			cov += (cov /(x.length-1));
		return cov;
	}

	public static void main(String[] args)
	{
		// load file
//		FileHandler fh = new FileHandler(Action.OPEN_FILE);
//		Table t = fh.getTable();
//
//		// data
//		final int rows = t.getNrRows();
//		double[][] data = new double[2][rows];
//		for (int i = 0, j = t.getNrColumns(), k = 0; i < j && k < 2; ++i)
//		{
//			Column c = t.getColumn(i);
//			//if (c.getType() == AttributeType.NUMERIC)
//			if (c.getName().equals("Education num") || c.getName().equals("Capital loss"))
//			{
//				double[] da = new double[rows];
//				for (int m = 0; m < rows; ++m)
//					da[m] = c.getFloat(m);
//				data[k++] = da;
//			}
//		}

		double[] x_data = { 0.62, 0.50, 0.70, 0.51, 0.32 };
		double[] y_data = { 0.51, 0.32, 0.51, 0.62, 0.41 };
		double[][] data = new double[][] { x_data, y_data };
		ProbabilityDensityFunction2_2D pdf = new ProbabilityDensityFunction2_2D(data);
		System.out.println("START PLOT");
		new TMPW(pdf.density, "");

//		double[] x = { 1,3,2,5,8,7,12,2,4 };
//		double[] y = { 8,6,9,4,3,3,2,7,7 };
//		//double[] x = { 1,3,2,5,8,7,12,2,4,2,3,4,6,9,4,6,1 };
//		//double[] y = { 8,6,9,4,3,3,2,7,7,2,5,9,5,7,3,4,8 };
//		boolean isSample = true;
//		System.out.println(cov1(x,y,isSample));
//		System.out.println(cov(x,y,isSample));
//		System.out.println(cov_cm(x,y, isSample));
//		isSample = false;
//		System.out.println(cov1(x,y,isSample));
//		System.out.println(cov(x,y,isSample));
//		System.out.println(cov_cm(x,y, isSample));
//
//		//
//		int gn = 20;
//		double[] x_grid = new double[gn];
//		double[] y_grid = new double[gn];
//		for (int i = 0; i < gn; ++i)
//			x_grid[i] = y_grid[i] = (i * (1.0 / gn));
//
//		double[] x_data = { 0.52, 0.50, 0.50, 0.51, 0.52 };
//		double[] y_data = { 0.51, 0.52, 0.51, 0.52, 0.51 };
//		double[] x_data = { 0.40, 0.50, 0.60, 0.55, 0.52 };
//		double[] y_data = { 0.42, 0.53, 0.55, 0.49, 0.51 };
//
//		double xvar = Vec.variance(x_data, false);
//		double yvar = Vec.variance(y_data, false);
//		double xycov = cov(x_data, y_data, false);
//		double[][] cm = { {xvar, xycov}, {xycov, yvar} };
//		cm = ProbabilityDensityFunctionMV.h_silverman(new double[][] { x_data, y_data });
//
//		double[][] density = density(new double[][] {x_grid, y_grid}, new double[][] {x_data, y_data}, cm);
//		for (int i = 0; i < gn; ++i)
//			System.out.println(Arrays.toString(density[i]));
//		for (int i = 0; i < gn; ++i)
//			Vec.divide(density[i], 100.0);
//
//		LabelRankingMatrix l = new LabelRankingMatrix(density);
//		new TMPW(density, "");
//
//		Matrix data = new Matrix(x_data.length, 2);
//		for (int i = 0; i < x_data.length; ++i)
//		{
//			data.set(i, 0, x_data[i]);
//			data.set(i, 1, y_data[i]);
//		}
//		data.print(6, 2);
//
//		Matrix cov_mx = new Matrix(2, 2);
//		cov_mx.set(0, 0, xvar);
//		cov_mx.set(0, 1, xycov);
//		cov_mx.set(1, 0, xycov);
//		cov_mx.set(1, 1, yvar);
//
//		Matrix densities = density(x_grid, y_grid, data, cov_mx);
//		densities.print(16, 14);
	}
}
