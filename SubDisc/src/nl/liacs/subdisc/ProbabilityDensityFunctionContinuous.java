package nl.liacs.subdisc;

import java.util.*;
import java.util.Map.Entry;

/*
 * this class creates a pdf of the form HashMap<x, PDFNode(x)>
 * the keys are the unique values in the input domain
 * the values contain additional information needed for divergence computations
 * a PDFNode consists of [value, delta_to_previous_value, probability(value)]
 * 
 * the delta is determined at creation time, by sorting the original unique data
 * the continuous version of KL uses this delta for the integral
 * 
 * for both the data and every subgroup, a Map<value, occurrences> is created
 * from only the unique values that it covers (might be few for subgroups)
 * D_d = unique domain of the data, D_sg = unique domain of the subgroup
 * 
 * P=subgroup, Q=data
 * KL divergence DKL(P||Q): integral( ln( p(x)/q(x) ) * p(x) * delta(x))
 * only the non-zero subgroup values are required for the computation
 * because, when p(x) = 0 -> no contribution is made to the summation
 * 
 * for each value x in the subgroup, the lookup of delta(x) and q(x) is done in
 * constant time O(1), as the data.pdf is a HashMap
 * so the KL divergence can be computed in linear time O(|D_sg|)
 * a sorted pdf would require O(|D_sg| * log(|D_d|))
 * (+ time required to get D_sg -> O(|D_sg| * log(|D_sg|))
 * 
 * Hellinger H^2(P,Q): 1/2 * integral( sqrt(dP/dl) - sqrt(dQ/dl) )^2 * dl
 * visits all unique values in D_d
 */
public class ProbabilityDensityFunctionContinuous
{
	private final Column itsData;
	private final Map<Float, PDFNode> itsPDF;

	ProbabilityDensityFunctionContinuous(Column theData)
	{
		if (theData == null)
			throw new IllegalArgumentException("arguments can not be null");
		if (theData.getType() != AttributeType.NUMERIC)
			throw new IllegalArgumentException("Column is not of type:" + AttributeType.NUMERIC);

		itsData = theData;
		itsPDF = createPDF(itsData);
	}

	// this is not the same as Column.getUniqueNumericDomain()
	// this returns a sorted, unique domain, using PDFNodes
	// NOTE that this method is only called once, at creation time
	// so efficiency is not an issue
	private static final Integer ONE = new Integer(1);
	private static final Map<Float, PDFNode> createPDF(Column theData)
	{
		final int size = theData.size();

		// nothing to do
		if (size <= 0)
			return Collections.emptyMap();

		// create SortedMap with counts for each unique value in domain
		// counts are divided only once, to keep rounding error small
		SortedMap<Float, Integer> counts = new TreeMap<Float, Integer>();
		for (int i = 0; i < size; ++i)
		{
			Float f = theData.getFloat(i);
			Integer p = counts.get(f);
			if (p == null)
				counts.put(f, ONE);
			else
				counts.put(f, p.intValue()+1);
		}

		// create final Map
		final double div = size; // no casts
		Map<Float, PDFNode> pdf = new HashMap<Float, PDFNode>();
		Iterator<Entry<Float, Integer>> it = counts.entrySet().iterator();
		Entry<Float, Integer> first = it.next(); // save as size > 0
		Float f = first.getKey();
		pdf.put(f, new PDFNode(f, 1.0f, first.getValue()/div)); // delta=1.0f
		while (it.hasNext())
		{
			Entry<Float, Integer> e = it.next();
			Float g = e.getKey();
			pdf.put(g, new PDFNode(g, g-f, e.getValue()/div));
			f = g;
		}

		// debug only
		double d = 0.0;
		for (PDFNode p : pdf.values())
			d += p.itsProbability;
		System.out.format("pdf size=%d sum(p(x))=%.17f%n", pdf.size(), d);

		return pdf;
	}

	private void print()
	{
		SortedSet<PDFNode> set = new TreeSet<PDFNode>(itsPDF.values());
		for (PDFNode p : set)
			System.out.println(p);
	}

	// private, for use inside this class only
	private static final class PDFNode implements Comparable<PDFNode>
	{
		public final float itsValue; // not strictly needed
		public final float itsDelta;
		public final double itsProbability;

		public PDFNode(float theValue, float theDelta, double theProbability)
		{
			assert(!Float.isInfinite(theValue) && !Float.isNaN(theValue));
			assert(!Float.isInfinite(theDelta) && !Float.isNaN(theDelta));
			assert(theDelta > 0.0f);
			assert(!Double.isInfinite(theProbability) && !Double.isNaN(theProbability));
			assert(theProbability > 0.0);

			itsValue = theValue;
			itsDelta = theDelta;
			itsProbability = theProbability;
		}

		// XXX for use in this class only, compares only itsValue
		@Override
		public int compareTo(PDFNode theOther)
		{
			return Float.compare(this.itsValue, theOther.itsValue);
		}

		@Override
		public String toString()
		{
			return String.format("x=%f, dx=%f, p(x)=%f",
					itsValue, itsDelta, itsProbability);
		}
	}

	public static void main(String[] args)
	{
		int nrRows = 100;
		Column c = new Column("TEST", "TEST", AttributeType.NUMERIC, 0, nrRows);
		Random r = new Random();
		for (int i = 0; i < nrRows; ++i)
			c.add(r.nextInt(10));

		ProbabilityDensityFunctionContinuous pdf = new ProbabilityDensityFunctionContinuous(c);
		pdf.print();

		for (int i = 0, j = c.size(); i < j; ++i)
			System.out.format("%f\t%s%n", c.getFloat(i), pdf.itsPDF.get(c.getFloat(i)).toString());
	}
}
