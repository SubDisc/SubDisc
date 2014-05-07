package nl.liacs.histo;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

import javax.swing.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;

public class Test
{
	public static void main(String[] args)
	{
//		basic();
//		double[] kde = kde();
//		Gaussian[] ga = gaussian(kde);
//		plot(ga);
//		smooth(ga);
//		smooth2();
//		alt(Data.getSimple(true)); // sigma=2.25
//		alt(Data.getSimple(false)); // sigma=0.4
//		double[] data = Data.getForbes(false);
//		Arrays.sort(data);
//		alt(data);
		// 2^20 and up
		int[] n = { 1048576, 2097152, 4194304, 8388608, 16777216, 33554432 };
		for (int i = 0; i < n.length; ++i)
		{
			int size = n[i];
			float incr = 1.0f / size;
			float prod = size*incr;
			System.out.format("%d * %20.16f = %20.16f%n", size, incr, prod);
		}
		for (int i = 0; i < 100; ++i)
		{
			int size = 33554432 + i;
			float incr = 1.0f / size;
			float prod = size*incr;
			System.out.format("%d * %20.16f = %20.16f%n", size, incr, prod);
		}

	}

	private static final void basic()
	{
		int size = 1001;

		// get list of percentage points
		double[] p = Vec.range(0.5, 0.5/(size-1), size);

		// PhiInv(p) -> z
		double[] z = Gaussian.z(p);

		// n(mu, sigma^2), where mu = 0.0, sigma=1.0 
		double[] n = Gaussian.n(z);

		// gaussian
		double[] g1 = Gaussian.g(1.0, (size*2)-1);
		double[] g2 = Gaussian.g(Math.sqrt(5.0), (size*2)-1);

		double[] x = { 1, 2, 3, 4, 5, 6 };
		double c = 1.0/6.0;
		double[] y = { c, c, c, c, c, c };
		System.out.println("E[x]="+Vec.expected_value(x, y));
		x = new double[] { 1, 2, 3, 4, 5, 6 };
		y = new double[] { .5, .25, .0625, .0625, .0625, .0625 };
		System.out.println("E[x]="+Vec.expected_value(x, y));
	}

	private static final double[] kde()
	{
		double[] data = Data.getForbes(false);
		Arrays.sort(data);

		double min = Vec.minimum(data);
		double max = Vec.maximum(data);
		double r = max-min;

		double[] kde = new double[7];
		// compute k using h -> use h
		kde[0] = Vec.h_freemanDiaconis(data);
		kde[1] = Vec.h_scott(data);
		kde[2] = Vec.h_silverman(data);
		// compute k directly -> 'convert' to h, may be rounded up a bit
		// k = ceil((max-min) / h)
		kde[3] = r / Vec.k_doane(data);
		kde[4] = r / Vec.k_rice(data);
		kde[5] = r / Vec.k_squareRoot(data);
		kde[6] = r / Vec.k_sturges(data);

		System.out.println("kde=" + Arrays.toString(kde));
		return kde;
	}

	private static final Gaussian[] gaussian(double[] kde)
	{
		Gaussian[] ga = new Gaussian[kde.length+1];

		int samples = 1001;
		for (int i = 0; i < ga.length-1; ++i)
			ga[i] = new Gaussian(0.0, Math.sqrt(kde[i]), samples);
		ga[ga.length-1] = new Gaussian(0.0, 1.0, samples);

		return ga;
	}

	private static final void smooth(Gaussian[] ga)
	{
		double[] data = Data.getForbes(false);
		Arrays.sort(data);
		double min = data[0];
		double max = data[data.length-1];

		for (int i = 0; i < ga.length; ++i)
		{
			Gaussian g = ga[i];
			double sigma = g.sigma();
			double[] x = g.x();
			double interval = sigma;

			int k = (int)Math.ceil(Vec.nrBins(min, max, interval));
			double[] bins = Vec.range(data[0], interval, k+1);
			bins = Arrays.copyOf(bins, bins.length+2);
			bins[bins.length-2] = max + interval;
			bins[bins.length-1] = Double.POSITIVE_INFINITY;
			double[][] out = ga[i].applyTo(data);

			double[] density = new double[bins.length];
			for (int j = 0; j < out.length; ++j)
			{
				double v = data[j];
				double[] vx = Vec.add(Arrays.copyOf(x, x.length), v);
				double[] vy = Vec.getBinCounts(vx, bins);
				for (int w = 0; w < density.length; ++w)
					density[w] += vy[w];
			}
Vec.divide(density, data.length * x.length);
Vec.print(bins);
Vec.print(density);
System.out.println("sum(density)=" + Vec.sum(density));
plot(Arrays.copyOf(bins, k+2) , Arrays.copyOf(density, k+2));
		}

	}

	private static final void smooth2()
	{
		double[] data = { -2.1, -1.3, -0.4, 1.9, 5.1, 6.2 };

		int samples = 1001;
		double sigma = Math.sqrt(2.25);

		Gaussian[] ga = new Gaussian[data.length];
		for (int i = 0; i < data.length; ++i)
			ga[i] = new Gaussian(data[i], sigma, samples);
for (Gaussian g : ga)
	System.out.format("%f %f %n", g.mu(), Vec.sum(g.fx()));
for (Gaussian g : ga)
	Vec.divide(g.fx(), data.length);

		XYSeriesCollection c = new XYSeriesCollection();
		for (Gaussian g : ga)
		{
			double[] x = g.x();
			double[] fx = g.fx();
			XYSeries s = new XYSeries(g.mu(), false, false);
			for (int i = 0, j = x.length; i < j; ++i)
				s.addOrUpdate(x[i], fx[i]);
				//s.addOrUpdate(x[i], fx[i]/data.length);// hack
			c.addSeries(s);
		}

		double lo = data[0]-(8*sigma);
		double hi = data[data.length-1]+(8*sigma);
		double r = ((hi-lo) / sigma);
		int k = (int) ((Math.ceil(r/16.0))*samples);
System.out.println("lo="+lo);
System.out.println("hi="+hi);
System.out.println("r="+r);
System.out.println("k="+k);
		//double[] bins = Vec.range(lo, (hi-lo)/k, k+1);
		double[] bins = Vec.range(lo, (hi-lo)/samples, samples+1);
		//double[] bins = Vec.range(lo, 1.5, 22);
		// rounding error might cause: bins[last] < hi
		bins[bins.length-1] = hi;
//Vec.print(bins);
		double[] density = new double[bins.length];
		for (Gaussian g : ga)
		{
//			double[] x = g.x();
//			double[] cnt = Vec.getBinCounts(x, bins);
//			Vec.divide(cnt, x.length);
//			for (int w = 0; w < density.length; ++w)
//				density[w] += cnt[w];

			double[] x = g.x();
			double[] fx = g.fx();
			for (int i = 0; i < x.length; ++i)
			{
				int idx = Arrays.binarySearch(bins, x[i]);
				density[idx < 0 ? ~idx : idx] += fx[i];
			}
		}

//		Vec.print(density);
		Vec.divide(density, data.length*sigma);
		System.out.println("sum(density)=" + Vec.sum(density));

		XYSeries s = new XYSeries("kde", false, false);
		for (int i = 0; i < bins.length; ++i)
			s.addOrUpdate(bins[i], density[i]);
		c.addSeries(s);

		plot(c);
	}

	private static final void alt(double[] data)
	{
		int samples = 1001; // samples per gaussian [-8*sigma:+8*sigma]

		double min = Vec.minimum(data);
		double max = Vec.maximum(data);
		double r = max-min;

		double[] kde = new double[7];
		// compute k using h -> use h
		kde[0] = Vec.h_freemanDiaconis(data);
		kde[1] = Vec.h_scott(data);
		kde[2] = Vec.h_silverman(data);
		// compute k directly -> 'convert' to h, may be rounded up a bit
		// k = ceil((max-min) / h)
		kde[3] = r / Vec.k_doane(data);
		kde[4] = r / Vec.k_rice(data);
		kde[5] = r / Vec.k_squareRoot(data);
		kde[6] = r / Vec.k_sturges(data);
		System.out.println("kde=" + Arrays.toString(kde));

		//double sigma = kde[2];
		double sigma = kde[2]/2; // HACK Silverman tends to oversmooth
		System.out.println("sigma="+sigma);

		double lo = min-(8.0*sigma);
		double hi = max+(8.0*sigma);
		// s = sigmas covered by range
		double s = (hi-lo)/sigma;
		// 1 sigma=(samples/16)
		int k = (int) ((Math.ceil(s) * samples) / 16);
		System.out.println("lo="+lo);
		System.out.println("hi="+hi);
		System.out.println("r="+r);
		System.out.println("s="+s);
		System.out.println("k="+k);

		double[] bins = Vec.range(lo, (hi-lo)/k, k+1);
		// rounding error might cause: bins[last] < hi
		bins[bins.length-1] = hi;
//		Vec.print(bins);

		double[] density = new double[bins.length];
		double[] parzen = new double[bins.length];
		for (int i = 0; i < bins.length; ++i)
		{
			double mu = bins[i];

			// XXX could abort loop when |x| > cutoff (say 4 or 5)
			for (int j = 0; j < data.length; ++j)
			{
				//density[i] += Gaussian.phi(data[j], mu, sigma); // alt 1
				density[i] += Gaussian.phi((data[j]-mu) / sigma); // alt 2
				parzen[i] += ((Math.abs((data[j]-mu) / sigma)) < 1 ? 0.5 : 0); // ParzenRosenblatt
			}
		}
//		Vec.divide(density, data.length);	// for alt 1
		Vec.divide(density, data.length*sigma); // for alt 2
		Vec.divide(parzen, data.length*sigma); // for alt 2
		System.out.format("integral(bins, density)=%.16f%n", Vec.integral(bins, density));
		System.out.format("integral(bins, parzen)=%.16f%n", Vec.integral(bins, parzen));
		System.out.println("E[bins, density]="+Vec.expected_value(bins, density));

		// master stucture
		XYSeriesCollection c = new XYSeriesCollection();
		XYSeries den = new XYSeries("density", false, false);
		XYSeries par = new XYSeries("parzen", false, false);
		c.addSeries(den);
		c.addSeries(par);
		for (int i = 0, j = bins.length; i < j; ++i)
		{
			den.addOrUpdate(bins[i], density[i]);
			par.addOrUpdate(bins[i], parzen[i]);
		}
		double last = Double.POSITIVE_INFINITY;

		// NOTE JFreeChart does not have a rug plot - this is a fake
		// could use scatter plot or anything else instead
		if (data.length > 1000)
			; // do nothing - too many points leads to clutter
		else if (data.length > 20)
		{
			XYSeries xy = new XYSeries("rug", false, false);
			c.addSeries(xy);

			double bar = Vec.maximum(density)/100.0;
			for (double d : data)
			{
				// HACK to avoid repetitions - yields incorrect result
				if (d == last)
					continue;
				last = d;
				// straight line up
				xy.addOrUpdate(Math.nextAfter(d, Double.NEGATIVE_INFINITY), 0.0);
				xy.addOrUpdate(d, bar);
				xy.addOrUpdate(Math.nextUp(d), 0.0);
			}
		}
		else
		{
			for (double d : data)
			{
				// HACK to avoid repetitions - yields incorrect result
				if (d == last)
					continue;
				last = d;
				// lazy
				Gaussian g = new Gaussian(d, sigma, samples);
				double[] x = g.x();
				double[] fx = g.fx();
				XYSeries xy = new XYSeries(d, false, false);
				for (int i = 0, j = x.length; i < j; ++i)
					xy.addOrUpdate(x[i], fx[i]/data.length);
				c.addSeries(xy);
			}
		}
		plot(c);
	}

	public static final void plot(double[] boundaries, double[] densities)
	{
		XYSeries s = new XYSeries("", false, false);
		for (int i = 0, j = densities.length; i < j; ++i)
			s.addOrUpdate(boundaries[i], densities[i]);
		XYSeriesCollection c = new XYSeriesCollection(s);

		plot(c);
	}

	public static final void plot(XYSeriesCollection c)
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

		JFreeChart chart = ChartFactory.createXYLineChart(
						"pdf",
						"domain",
						"density",
						c,
						PlotOrientation.VERTICAL,
						false,
						true,
						false);
		XYPlot aPlot = chart.getXYPlot();
		chart.getTitle().setFont(aPlot.getDomainAxis().getLabelFont());
		aPlot.setBackgroundPaint(Color.WHITE);
		aPlot.setDomainGridlinePaint(Color.GRAY);
		aPlot.setRangeGridlinePaint(Color.GRAY);
		aPlot.getRenderer().setSeriesPaint(0, Color.BLACK);
		aPlot.getRenderer().setSeriesShape(0, new Ellipse2D.Float(0.0f, 0.0f, 1.0f, 1.0f));
		ChartPanel cp = new ChartPanel(chart);

		p.add(cp);
		JFrame j = new JFrame();
		j.add(p);
		j.setTitle("PlotWindow");
		//setIconImage(MainWindow.ICON);
		j.setLocation(0, 0);
		j.setSize(1200, 960);
		j.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		j.setVisible(true);
	}
}
