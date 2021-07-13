package nl.liacs.histo;

import java.util.Arrays;

public final class Gaussian
{
	/*
	 * Gaussian instance methods
	 **********************************************************************/
	private final double mu;
	private final double sigma;
	private final double[] x; // could always be recreated if needed
	private final double[] fx;

	/** normal distribution - N(mu,sigma^2) */
	public Gaussian(double mu, double sigma, int samples)
	{
		if (sigma <= 0.0)
			throw new IllegalArgumentException("sigma must be > 0.0");
		if (samples < 0 || (samples & 1) == 0)
			throw new IllegalArgumentException("samples must be odd and positive");

		int half = (samples+1)/2;
		double[] p = Vec.range(0.5, 0.5/(half-1), half);
		p[p.length-1] = 1.0; // adjust for possible rounding errors
		double[] z = z(p);

		this.mu = mu;
		this.sigma = sigma;
		// create standard normal distribution N(0,1)
		x = mirror(z); // hack
		fx = n(z);

		// convert to normal(mu,sigma^2)
		if (mu != 0.0 || sigma != 1.0)
			for (int i = 0, j = x.length; i < j; ++i)
				x[i] = (mu + x[i]*sigma);

		if (sigma != 1.0)
			Vec.divide(fx, sigma);

		// re-correct for AUC of 1
//		double sum = Vec.sum(fx);
//		Vec.divide(fx, sum);
	}

	private static final double[] mirror(double[] z)
	{
		int size = (2 * z.length) - 1;
		double[] out = new double[size];

		// fast copy for right side
		System.arraycopy(z, 0, out, z.length-1, z.length);
		// loop and negate for left side
		for (int i = z.length-1, j = 0; i > 0; --i, ++j)
			out[j] = -z[i];

		debug(out);
		return out;
	}

	final double mu() { return mu; }
	final double sigma() { return sigma; }
	// XXX OH NO
	final double[] x() { return x; }
	final double[] fx() { return fx; }

	/**
	 * This kernel is placed on each of the data points in data.
	 * This can be used for a kernel density estimate, either directly, or
	 * through binning.
	 */
	final double[][] applyTo(double[] data)
	{
		double[] kernel = fx;
		int length = kernel.length;
		double[][] out = new double[data.length][length];

		// XXX prime target for parallelisation
		for (int i = 0, j = data.length; i < j ;++i)
			out[i] = Vec.multiply(Arrays.copyOf(kernel, length), data[i]);

		return out;
	}

	/*
	 * Gaussian static methods
	 **********************************************************************/
	private static final boolean DEBUG = false;

	/*
	 * single point methods
	 **********************************************************************/

	// return phi(x) = standard Gaussian pdf
	public final static double phi(double x)
	{
		return Math.exp(-x*x / 2.0) / Math.sqrt(2.0 * Math.PI);
	}

	// return phi(x, mu, sigma) = Gaussian pdf with mean mu and stddev sigma
	public final static double phi(double x, double mu, double sigma)
	{
		return phi((x - mu) / sigma) / sigma;
	}

	// return Phi(z) = standard Gaussian cdf using Taylor approximation
	public final static double Phi(double z)
	{
		if (z < -8.0)
			return 0.0;
		if (z >  8.0)
			return 1.0;

		double sum = 0.0;
		double term = z;
		for (int i = 3; sum + term != sum; i += 2)
		{
			sum  = sum + term;
			term = term * z * z / i;
			// sum += term;
			// term = ((term * zz) / i);
		}
		return 0.5 + sum * phi(z);
	}

	// return Phi(z, mu, sigma) = Gaussian cdf with mean mu and stddev sigma
	public final static double Phi(double z, double mu, double sigma)
	{
		return Phi((z - mu) / sigma);
	}

	// Compute z such that Phi(z) = y via bisection search
	public final static double PhiInverse(double y)
	{
		if (y < 0.0 || y > 1.0)
			throw new IllegalArgumentException("0 <= y <= 1 violated: " + y);
		if (y == 0.0)
			return -8.0;
		if (y == 0.5)
			return 0.0;
		if (y == 1.0)
			return 8.0;
		return PhiInverse(y, 0.00000001, -8.0, 8.0);
	}

	// bisection search
	private final static double PhiInverse(double y, double delta, double lo, double hi)
	{
		double mid = lo + ((hi - lo) / 2.0);
		if (hi - lo < delta)
			return mid;
		if (Phi(mid) > y)
			return PhiInverse(y, delta, lo, mid);
		else
			return PhiInverse(y, delta, mid, hi);
	}

	/*
	 * vector methods
	 **********************************************************************/

	static final double[] g(double sigma, int samples)
	{
		// standard normal distribution
		double[] out = n(samples);
		// convert to normal(mu, sigma)
		Vec.divide(out, sigma);

		debug(out);
		return out;
	}

	// assumes samples = odd
	static final double[] n(int samples)
	{
		int half = (samples+1)/2;
		double[] p = Vec.range(0.5, 0.5/(half-1), half);
		double[] z = z(p);

		return n(z);
	}

	// assumes z is unique, sorted, and ranges 0.5 <= z[i] <= 1.0
	// standard normal, mu=0 sigma=1 N(0,1)
	// 1/(sigma*sqrt(2*pi)) * e^-( (x-mu)^2) / (2*(sigma)^2) )
	// 1/(sqrt(2*pi)) * e^-( (x^2) / 2 )
	// 1/(sqrt(pi+pi)) * e^-( (x^2) * 0.5 )
//	static final double[] n(double[] z)
//	{
//		int size = (2 * z.length) - 1;
//		double[] out = new double[size];
//
//		// fast copy for right side
//		System.arraycopy(z, 0, out, z.length-1, z.length);
//		// loop and negate for left side
//		for (int i = z.length-1, j = 0; i > 0; --i, ++j)
//			out[j] = -z[i];
//
//		// TODO MM copy in duplication is not needed
//		// should perform fill symmetrically
//		double factor = 1.0 / Math.sqrt((Math.PI + Math.PI));
//		for (int i = 0, j = out.length; i < j; ++i)
//			out[i] = factor * Math.exp(-((out[i]*out[i]) * 0.5));
//
//		debug(out);
//		return out;
//	}
	static final double[] n(double[] z)
	{
		int size = (2 * z.length) - 1;
		double[] out = new double[size];

		for (int zi = 0, j = z.length, mid = size/2; zi < j; ++zi)
			out[mid-zi] = out[mid+zi] = phi(z[zi]);

		debug(out);
		return out;
	}

	static final double[] z(double[] p)
	{
		double[] out = new double[p.length];
		for (int i = 0, j = out.length; i < j; ++i)
			out[i] = PhiInverse(p[i]);

		debug(out);
		return out;
	}

	private static final void debug(double[] data)
	{
		if (DEBUG)
			print(data);
	}

	static void print(double[] data)
	{
		System.out.println("|N|=" + data.length);

		int digits = (int) Math.ceil(Math.log10(data.length));
		String format = String.format("%%%dd %%20.16f%%n", digits);

		for (int i = 0, j = data.length; i < j; ++i)
			System.out.format(format, i, data[i]);

		System.out.println();
	}
}
