package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.histo.*;

// drop-in replacement for ProbabilityDensityFunction
// class does not store whole density f, only N samples from it
// not required for 1D, but for 2D computational burden remains reasonable
//
// whole input domain D is (re-)used
// the alternative is to use a <value,density> tuple for each unique value in D
// if (cardinality(D) < 0.5*|D|) then the alternative requires less memory
// IGNORE ALL OF THE ABOVE

// for now - extend ProbabilityDensityFunction for easy drop-in testing
public class ProbabilityDensityFunction2 extends ProbabilityDensityFunction
{
	// related to Gaussian | standard normal distribution
	// kernel spans [-CUTOFF : CUTOFF], and consists of SAMPLES points
	private static final double CUTOFF = 4.0;
	private static final double SAMPLES = 1001;

	// related to original domain
	private final Column itsData;
	private final double itsMin;
	private final double itsMax;
	// related to density function
	private final double itsLo;
	private final double itsHi;
	private double itsH; // h
	private final double dx;
	// do not not cache (large) x-grid, re-computation is fast enough
	// private final float[] x_grid;
	private final float[] itsDensity;

	public ProbabilityDensityFunction2(Column theData)
	{
		super(theData);

		itsData = theData;
		itsMin = itsData.getMin();
		itsMax = itsData.getMax();
		int data_size = itsData.size();
		// TODO MM create Vec.h_silverman(Column)
		double[] data = new double[data_size];
		for (int i = 0, j = data.length; i < j; ++i)
			data[i] = itsData.getFloat(i);
		Arrays.sort(data);

		// NOTE Silverman tends to oversmooth
		// plug-in or cross-validation bandwidth selectors are preferred
		itsH = Vec.h_silverman(data);

		itsLo = itsMin-(CUTOFF*itsH);
		itsHi = itsMax+(CUTOFF*itsH);
		double range = itsHi-itsLo;
		itsH = Math.max(itsH, range/1000.0); //to avoid problem with Silverman returning 0

		// s = sigmas covered by range
		double s = range/itsH;
		// 1 sigma=(samples/16)
		int k = (int) ((Math.ceil(s) * SAMPLES) / (2.0 * CUTOFF));
		k = Math.min(k, 1000); // k could grow unlimited, producing a negative array size
		dx = range/k;
System.out.format("h=%f lo=%f hi=%f range=%f s=%f k=%d dx=%f%n", itsH, itsLo, itsHi, range, s, k, dx);

		// hack
		BitSet b = new BitSet(data_size);
		b.set(0, data_size);

		itsDensity = getDensity(itsData, b, k+1);
	}

	// create for subgroup, relative to existing PDF (use same Column data)
	public ProbabilityDensityFunction2(ProbabilityDensityFunction thePDF, BitSet theMembers)
	{
		super(thePDF, theMembers);

		ProbabilityDensityFunction2 aPDF = (ProbabilityDensityFunction2) thePDF;
		itsData = aPDF.itsData;
		itsMin = aPDF.itsMin;
		itsMax = aPDF.itsMax;
		itsH = aPDF.itsH;
		itsHi = aPDF.itsHi;
		itsLo = aPDF.itsLo;
		dx = aPDF.dx;
		itsDensity = aPDF.getDensity(aPDF.itsData, theMembers, aPDF.itsDensity.length);
	}

	// TODO MM rounding error might cause: itsLo+(n*dx) < itsHi
	private final float[] getDensity_(Column theData, BitSet theMembers, int n)
	{
		float[] density = new float[n];

		for (int i = 0; i < density.length; ++i)
		{
			double mu = itsLo + (i * dx);

			for (int j = theMembers.nextSetBit(0); j >= 0; j = theMembers.nextSetBit(j + 1))
				density[i] += Gaussian.phi((itsData.getFloat(j)-mu) / itsH);
		}
		//Vec.divide(density, theMembers.cardinality()*itsH);
		double nh = theMembers.cardinality()*itsH;
		for (int i = 0; i < density.length; ++i)
			density[i] /= nh;

		return density;
	}

	private final float[] getDensity(Column theData, BitSet theMembers, int n)
	{
		// cache these values first
		// results in higher memory use + slightly larger rounding error
		double[] d_h = new double[theMembers.cardinality()];
		for (int i = 0, j = theMembers.nextSetBit(0); j >= 0; j = theMembers.nextSetBit(j + 1), ++i)
			d_h[i] = itsData.getFloat(j) / itsH;

		float[] density = new float[n];
		for (int i = 0; i < density.length; ++i)
		{
			double mu = itsLo + (i * dx); // XXX recompute x_grid
			double mu_h = mu / itsH;

			for (int j = 0; j < d_h.length; ++j)
			{
				double diff = d_h[j] - mu_h;
				// hack - should find start i before this loop
				// and break as soon as diff > CUTOFF
				if (Math.abs(diff) < CUTOFF)
					density[i] += Gaussian.phi(diff);
			}
		}
		//Vec.divide(density, theMembers.cardinality()*itsH);
		double nh = d_h.length*itsH;
		for (int i = 0; i < density.length; ++i)
			density[i] /= nh;
/*
double[] density_d = new double[density.length];
for (int i = 0; i < density_d.length; ++i)
	density_d[i] = density[i];
double[] bins = new double[density.length];
for (int i = 0; i < bins.length; ++i)
	bins[i] = itsLo + (i * dx);
System.out.format("integral(bins, density)=%.16f%n", Vec.integral(bins, density_d));
System.out.println("E[bins, density]="+Vec.expected_value(bins, density_d));
*/
		return density;
	}

	// as for ProbabilityDensityHistogram - all @Override
	@Override public float getDensity(int theIndex) { return itsDensity[theIndex]; }
	@Override public float getMiddle(int theIndex) { return (float)(itsLo + (theIndex + 0.5f)*dx); }
	@Override public int size() { return itsDensity.length; }
	// NOTE all original code calls smooth() just once
	// this is 'equivalent' to returning the 'KDE-based' density
	@Override public float[] smooth() { return itsDensity; }
}
