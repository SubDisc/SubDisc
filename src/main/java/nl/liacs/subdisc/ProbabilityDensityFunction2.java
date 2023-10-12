package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.histo.*;

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
	private double itsMin = Float.POSITIVE_INFINITY;
	private double itsMax = Float.NEGATIVE_INFINITY;

	// related to density function
	private final double itsLo;
	private final double itsHi;
	private double itsH; // h
	private final double dx;

	// do not cache (large) x-grid, re-computation is fast enough
	// private final float[] x_grid;
	private final float[] itsDensity;

	//TODO: check for selection
	public ProbabilityDensityFunction2(Column theData, BitSet theSelection)
	{
		super(theData);

		itsData = theData;

		// TODO MM create Vec.h_silverman(Column)
		//pull out relevant data (ignoring the missing data (NaN) and respecting the selection)
		ArrayList<Float> aList = new ArrayList<Float>(itsData.size());
		if (theSelection == null)
		{
			for (int i = 0, j = itsData.size(); i < j; ++i)
				if (!itsData.getMissing(i)) //skip NaN values
					aList.add(itsData.getFloat(i));
		}
		else
		{
			for (int i = 0, j = itsData.size(); i < j; ++i)
				if (!itsData.getMissing(i) && theSelection.get(i))
					aList.add(itsData.getFloat(i));
		}
		int aSize = aList.size();
		double[] data = new double[aSize];
		for (int i=0; i<aSize; i++)
		{
			data[i] = (double) aList.get(i);
			if (data[i] > itsMax)
				itsMax = data[i];
			if (data[i] < itsMin)
				itsMin = data[i];
		}
		Arrays.sort(data);

		itsH = Vec.h_silverman(data);

		itsLo = itsMin-(CUTOFF*itsH);
		itsHi = itsMax+(CUTOFF*itsH);
		double range = itsHi-itsLo;
		itsH = Math.max(itsH, range/1000); //to avoid problem with Silverman returning 0

		// s = sigmas covered by range
		double s = range/itsH;
		// 1 sigma=(samples/16)
		int k = (int) ((Math.ceil(s) * SAMPLES) / (2.0 * CUTOFF));
		k = Math.min(k, 1000); // k could grow unlimited, producing a negative array size
		dx = range/k;
		System.out.format("h=%f lo=%f hi=%f range=%f s=%f k=%d dx=%f%n", itsH, itsLo, itsHi, range, s, k, dx);

		if (theSelection == null)
		{
			BitSet anAllData = new BitSet(aSize);
			anAllData.set(0, aSize);
			itsDensity = getDensity(itsData, anAllData, k+1);
		}
		else
			itsDensity = getDensity(itsData, theSelection, k+1);
	}

	private float[] itsComplementDensity; // FIXME MM --- HACK
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

		BitSet aComplement = (BitSet) theMembers.clone();
		// DO NOT USE theMembers.size()
		aComplement.flip(0, itsData.size());
		itsComplementDensity = aPDF.getDensity(aPDF.itsData, aComplement, aPDF.itsDensity.length);
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
				// TODO MM should find start i before this loop
				// and break as soon as diff > CUTOFF
				if (Math.abs(diff) < CUTOFF)
					density[i] += Gaussian.phi(diff);
			}
		}
		//Vec.divide(density, theMembers.cardinality()*itsH);
		double nh = d_h.length*itsH;
		for (int i = 0; i < density.length; ++i)
			density[i] /= nh;

		return density;
	}

	// as for ProbabilityDensityHistogram - all @Override
	@Override public float getDensity(int theIndex) { return itsDensity[theIndex]; }
	// FIXME MM --- HACK
	public float getComplementDensity(int theIndex) { return itsComplementDensity[theIndex]; }
	@Override public float getMiddle(int theIndex) { return (float)(itsLo + (theIndex + 0.5f)*dx); }
	@Override public int size() { return itsDensity.length; }
	// NOTE all original code calls smooth() just once
	// this is 'equivalent' to returning the 'KDE-based' density
	@Override public float[] smooth() { return itsDensity; }
}
