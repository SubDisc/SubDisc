package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.histo.*;
import nl.liacs.subdisc.Jama.*;

import org.jfree.data.xy.*;

public class ProbabilityDensityFunctionMV
{
	public static void main(String[] args)
	{
		//FileHandler fh = new FileHandler(Action.OPEN_FILE);
		//Table t = fh.getTable();

		//ProbabilityDensityFunctionMV p = new ProbabilityDensityFunctionMV(t);

		double[] x = { 0, 1, 2, 3 };
		double[] y = { 0, 1, 2, 3 };
		ProbabilityDensityFunctionMV p = new ProbabilityDensityFunctionMV(new double[][] {x, y});

	}

	ProbabilityDensityFunctionMV(Table t)
	{
//		int rows = t.getNrRows();
//		int cols = t.getNrColumns();
//
//		int[][] types = t.getTypeCounts();
//		double[][] data = new double[types[1][0]][rows];
//
//		for (int i = 0, j = 0; i < cols; ++i)
//		{
//			Column c = t.getColumn(i);
//			if (c.getType() == AttributeType.NUMERIC)
//			{
//				double[] da = data[j++];
//				for (int k = 0; k < rows; ++k)
//					da[k] = c.getFloat(k);
//			}
//		}
//
//		Covariance cov = new Covariance(data);
//		RealMatrix cm = cov.getCovarianceMatrix();
//		for (int i = 0; i < rows; ++i)
//			System.out.println(Arrays.toString(cm.getRow(i)));
	}

	ProbabilityDensityFunctionMV(double[][] data)
	{
//		int rows = data.length;
//
//		Covariance cov = new Covariance(data);
//		RealMatrix cm = cov.getCovarianceMatrix();
//		double[][] data2 = cm.getData();
//		for (int i = 0; i < data2.length; ++i)
//			System.out.println(Arrays.toString(data2[i]));
	}

	// Silverman's rule suggests using
	// \sqrt{\mathbf{H}_{ii}} = \left(\frac{4}{d+2}\right)^{\frac{1}{d+4}} n^{\frac{-1}{d+4}} \sigma_i
	// where \sigma_i is the standard deviation of the ith variable and \mathbf{H}_{ij} = 0, i\neq j.
	static double[][] h_silverman(double[][] data)
	{
		// constants
		int d = data.length;
		double n = data[0].length;
		double pow = 1.0 / (d+4.0);
		//
		double d_fac = Math.pow((4.0 / (d+2.0)) , pow);
		double n_fac = Math.pow(n, -pow);
		double fac = d_fac * n_fac;

		double[][] H = new double[d][d];
		for (int i = 0; i < d; ++i)
		{
			H[i] = new double[d];
			double h_ii = fac * Vec.std_dev(data[i], true);
			H[i][i] = h_ii*h_ii; // sqrt(H_ii) = fac*std_dev_i
		}

		return H;
	}

	// http://dgpf.sourceforge.net/scripts/source/source.php?file=org.jfree.experimental.chart.demo.XYBlockChartDemo2.java
	// http://www.jfree.org/jfreechart/api/javadoc/org/jfree/chart/renderer/xy/XYBlockRenderer.html
	// http://www.jfree.org/phpBB2/viewtopic.php?f=3&t=24693
	private static final void plot3d(double[] x, double[] y, double[] z)
	{
		DefaultXYZDataset xyz = new DefaultXYZDataset();
		
	}
	
	
	
	
	
	
	
	
	/* Univariate kernel density estimation at point x with gaussian kernel of variance sigma
	 */
	public static double kernelDensity(Matrix x, Matrix val, Matrix Var) {
		int N = val.getRowDimension(); int d = val.getColumnDimension();
		double density = 0;
		
		for(int i =0; i<N; i++) {
			Matrix tmp = x.minus(val.getMatrix(i, i, 0, d-1));
			density += Math.exp(-0.5*tmp.times(Var.inverse()).times(tmp).get(0, 0));
		}
		density /= Math.pow(2*Math.PI*Var.det(), d/2.0) * N;
		return density;
	}
	
	/* Analytic calculation of L2 distance (Mean integrated squared error) for two gaussian kernel density estimators
	 */
	public static double L2distance(Matrix data1, Matrix data2, Matrix Var1, Matrix Var2) {
		double L2divergence = 0;
		int N1=data1.getRowDimension(); int N2 = data2.getRowDimension(); int d = data1.getColumnDimension();
		
		
		for (int i=0; i < N1; i++){
			for(int j=0; j<N2; j++) {
				Matrix tmp = data1.getMatrix(i, i, 0, d-1).minus(data2.getMatrix(j, j, 0, d-1));
				L2divergence += Math.exp(-0.5*(tmp.times((Var1.plus(Var2)).inverse()).times(tmp).get(0, 0)));
			}
		}
		
		L2divergence /= Math.pow(2*Math.PI, d/2.0) * Math.pow(Var1.plus(Var2).det(), 0.5)*(N1*N2);
		
		return L2divergence;
	}
}
