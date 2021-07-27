package nl.liacs.subdisc;

import java.util.*;
import java.util.Map.Entry;

import nl.liacs.subdisc.gui.*;

// could be enum like ConditionBuilder, but inner classes have static methods
public final class DistributionBuilder
{
	// prints debug info when true
	private static final boolean DEBUG = true;
	private static final String DIST_INFO = "P size=%d sum(p(x))=%.17f%n";
	private static final String BIN_INFO = "x=%s p(x)=%.17f%n";

	public interface Distribution
	{
		public Column getColumn();
		public int getSize();
		// XXX tmp for debug only
		public void print();
	}

	public interface PDF extends Distribution
	{
		// only implemented for PDF (Columns with AttributeType.NUMERIC)
		public double getDensity(float theValue);
	}

	public interface Histo extends Distribution
	{
		// only implemented for Histo
		// (Columns with AttributeType.BINARY / AttributeType.NOMINAL)
		public double getDensity(String theValue);
	}

	/** PDF for data */
	public static PDF pdf(Column theData, boolean storeDelta)
	{
		if (theData == null)
			throw new IllegalArgumentException("arguments can not be null");
		if (theData.getType() != AttributeType.NUMERIC)
			throw new IllegalArgumentException("Column is not of type " + AttributeType.NUMERIC);

		return storeDelta ? new PDFHash(theData) : new PDFHashDelta(theData);
	}

	/** PDF for subgroup, relative to existing PDF (use same Column data) */
	public static PDF pdf(PDF theDataPDF, BitSet theMembers, boolean sorted)
	{
		if (theDataPDF == null || theMembers == null)
			throw new IllegalArgumentException("arguments can not be null");

		return sorted ? new PDFSorted(theDataPDF.getColumn(), theMembers) : new PDFHash(theDataPDF.getColumn());
	}

	/** Histo for data */
	public static Histo histo(Column theData, boolean storeDelta)
	{
		if (theData == null)
			throw new IllegalArgumentException("arguments can not be null");
		if (theData.getType() != AttributeType.BINARY || theData.getType() != AttributeType.NOMINAL)
			throw new IllegalArgumentException("Column is not of type " + AttributeType.BINARY + 
								" or: " + AttributeType.NOMINAL);

		return null;
	}

	// no 'abstract' PDF base class for now
	private static final class PDFHash implements PDF
	{
		private final Column itsColumn;
		private final Map<Float, Double> itsPDF;

		PDFHash(Column theData)
		{
			if (theData == null)
				throw new IllegalArgumentException("arguments can not be null");
			if (theData.getType() != AttributeType.NUMERIC)
				throw new IllegalArgumentException("Column is not of type " + AttributeType.NUMERIC);

			itsColumn = theData;
			itsPDF = createPDF(itsColumn);
		}

		private static final Double ONE = Double.valueOf(1.0);
		private static final Map<Float, Double> createPDF(Column theColumn)
		{
			final int size = theColumn.size();

			// nothing to do
			if (size <= 0)
				return Collections.emptyMap();

			/*
			 * HashMap with counts for each unique value in domain
			 * counts are divided once, to keep rounding error small
			 * 
			 * NOTE creates just 1 map, and updates it
			 * NOTE double for counts is save (will not lead to 
			 * rounding errors)
			 * IEEE-754 double precision floating point numbers use
			 * 53 significant bits / can store 2^54 integers
			 */
			Map<Float, Double> pdf = new HashMap<Float, Double>(theColumn.getCardinality());
			for (int i = 0; i < size; ++i)
			{
				Float f = theColumn.getFloat(i);
				Double p = pdf.get(f);
				if (p == null)
					pdf.put(f, ONE);
				else
					pdf.put(f, p+1.0);
			}

			final double div = size; // avoid casts
			for (Entry<Float, Double> e : pdf.entrySet())
				pdf.put(e.getKey(), e.getValue()/div);

			if (DEBUG)
			{
				double d = 0.0;
				for (Double p : pdf.values())
					d += p;
				System.out.format(DIST_INFO, pdf.size(), d);
			}

			return pdf;
		}

		@Override
		public Column getColumn() { return itsColumn; }

		@Override
		public int getSize() { return itsPDF.size(); }

		@Override
		public double getDensity(float theValue)
		{
			// unboxing of null would throw NullPointerException
			Double p = itsPDF.get(theValue);
			return (p == null) ? 0.0 : p;
		}

		public final void print()
		{
			SortedMap<Float, Double> map = new TreeMap<Float, Double>(itsPDF);
			for (Entry<Float, Double> e : map.entrySet())
				System.out.format(BIN_INFO, e.getKey(), e.getValue());
		}
	}

	private static final class PDFHashDelta implements PDF
	{
		private final Column itsColumn;
		private final Map<Float, PDFNode> itsPDF;

		PDFHashDelta(Column theData)
		{
			if (theData == null)
				throw new IllegalArgumentException("arguments can not be null");
			if (theData.getType() != AttributeType.NUMERIC)
				throw new IllegalArgumentException("Column is not of type " + AttributeType.NUMERIC);

			itsColumn = theData;
			itsPDF = createPDF(itsColumn);
		}

		// JSL guarantees no duplicates for values exist for [-128;127]
		private static final Integer ONE = Integer.valueOf(1);
		private static final Map<Float, PDFNode> createPDF(Column theColumn)
		{
			final int size = theColumn.size();

			// nothing to do
			if (size <= 0)
				return Collections.emptyMap();

			// SortedMap with counts for each unique value in domain
			// counts are divided once, to keep rounding error small
			SortedMap<Float, Integer> counts = new TreeMap<Float, Integer>();
			for (int i = 0; i < size; ++i)
			{
				Float f = theColumn.getFloat(i);
				Integer p = counts.get(f);
				if (p == null)
					counts.put(f, ONE);
				else
					counts.put(f, p+1);
			}

			// create final Map
			final double div = size; // no casts
			Map<Float, PDFNode> pdf = new HashMap<Float, PDFNode>(counts.size());
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

			if (DEBUG)
			{
				double d = 0.0;
				for (PDFNode p : pdf.values())
					d += p.itsProbability;
				System.out.format(DIST_INFO, pdf.size(), d);
			}

			return pdf;
		}

		@Override
		public Column getColumn() { return itsColumn; }

		@Override
		public int getSize() { return itsPDF.size(); }

		@Override
		public double getDensity(float theValue)
		{
			PDFNode p = itsPDF.get(theValue);
			return (p == null) ? 0.0 : p.itsProbability;
		}

		public final void print()
		{
			SortedMap<Float, PDFNode> map = new TreeMap<Float, PDFNode>(itsPDF);
			for (Entry<Float, PDFNode> e : map.entrySet())
			{
				System.out.format(BIN_INFO, e.getKey(), e.getValue().itsProbability);
				System.out.println("\t" + e.getValue());
			}
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
	}

	private static final class PDFSorted implements PDF
	{
		private final Column itsColumn;
		// Map<Float, Double> requires +/- 10x more space, use arrays
		private final float[] itsDomain;
		private final double[] itsDensities;

		PDFSorted(Column theData, BitSet theMembers)
		{
			if (theData == null)
				throw new IllegalArgumentException("arguments can not be null");
			if (theData.getType() != AttributeType.NUMERIC)
				throw new IllegalArgumentException("Column is not of type " + AttributeType.NUMERIC);
			if (theMembers == null)
				throw new IllegalArgumentException("arguments can not be null");
			if (theMembers.length() > theData.size())
				throw new IllegalArgumentException("|members| > |Column|");

			itsColumn = theData;
			Map<Float, Double> pdf = createPDF(itsColumn, theMembers);

			int size = pdf.size();
			itsDomain = new float[size];
			itsDensities = new double[size];

			// avoid casts, Map.values are counts, not densities
			// assumes pdf Map is sorted by key
			final double div = itsColumn.size();
			int i = 0;
			for (Entry<Float, Double> e : pdf.entrySet())
			{
				itsDomain[i] = e.getKey();
				itsDensities[i] = e.getValue()/div;
				++i;
			}
		}

		private static final Double ONE = Double.valueOf(1.0);
		// XXX Not a PDF, but a Map<value, count> instead
		private static final Map<Float, Double> createPDF(Column theColumn, BitSet theMembers)
		{
			final int size = theColumn.size();

			// nothing to do
			if (size <= 0)
				return Collections.emptyMap();

			/*
			 * TreeMap with counts for each unique value in domain
			 * counts are divided once, to keep rounding error small
			 * 
			 * NOTE creates just 1 map, and updates it
			 * NOTE double for counts is save (will not lead to 
			 * rounding errors)
			 * IEEE-754 double precision floating point numbers use
			 * 53 significant bits / can store 2^54 integers
			 */
			Map<Float, Double> pdf = new TreeMap<Float, Double>();
			for (int i = 0; i < size; ++i)
			{
				Float f = theColumn.getFloat(i);
				Double p = pdf.get(f);
				if (p == null)
					pdf.put(f, ONE);
				else
					pdf.put(f, p+1.0);
			}

			// XXX setting densities is done at array creation
			// this saves 1 loop over the values-domain
			// and better: unboxing V, v/div -> p, put new Double(p)
//			final double div = size; // avoid casts
//			for (Entry<Float, Double> e : pdf.entrySet())
//				pdf.put(e.getKey(), e.getValue()/div);

			if (DEBUG)
			{
				final double div = size; // avoid casts
				double d = 0.0;
				for (Double p : pdf.values())
					d += (p/div);
				System.out.format(DIST_INFO, pdf.size(), d);
			}

			return pdf;
		}

		@Override
		public final Column getColumn() { return itsColumn; }

		@Override
		public final int getSize() { return itsDomain.length; }

		@Override
		public final double getDensity(float theValue)
		{
			int i = Arrays.binarySearch(itsDomain, theValue);
			return (i < 0) ? 0.0 : itsDensities[i];
		}

		public final void print()
		{
			for (int i = 0, j = getSize(); i < j; ++i)
				System.out.format(BIN_INFO, itsDomain[i], itsDensities[i]);
		}
	}

	// never sorted, uses HashMap for O(1) lookup
	private static final class Histogram implements Histo
	{
		// fake BitSet to bypass null-check
		private static final BitSet ALL = new BitSet(0);

		private final Column itsColumn;
		private final Map<String, Double> itsDensities;

		Histogram(Column theColumn)
		{
			this(theColumn, ALL);
		}

		Histogram(Column theColumn, BitSet theMembers)
		{
			if (theColumn == null)
				throw new IllegalArgumentException("arguments can not be null");
			if (theColumn.getType() != AttributeType.BINARY && theColumn.getType() != AttributeType.NOMINAL)
				throw new IllegalArgumentException("Column is not of type " + AttributeType.BINARY +
									" or " + AttributeType.NOMINAL);
			if (theMembers == null)
				throw new IllegalArgumentException("arguments can not be null");
			if (theMembers.length() > theColumn.size())
				throw new IllegalArgumentException("|members| > |Column|");


			itsColumn = theColumn;
			itsDensities = createHisto(itsColumn, theMembers);
		}

		private static final Double ONE = Double.valueOf(1.0);
		private static final Map<String, Double> createHisto(Column theColumn, BitSet theMembers)
		{
			final int size = theColumn.size();

			// nothing to do
			if (size <= 0 || (theMembers != ALL && theMembers.length() == 0))
				return Collections.emptyMap();

			/*
			 * TreeMap with counts for each unique value in domain
			 * counts are divided once, to keep rounding error small
			 * 
			 * NOTE creates just 1 map, and updates it
			 * NOTE double for counts is save (will not lead to 
			 * rounding errors)
			 * IEEE-754 double precision floating point numbers use
			 * 53 significant bits / can store 2^54 integers
			 */
			Map<String, Double> histo = new HashMap<String, Double>(theColumn.getCardinality());
			if (theMembers == ALL)
			{
				for (int i = 0; i < size; ++i)
				{
					String s = theColumn.getString(i);
					Double d = histo.get(s);
					if (d == null)
						histo.put(s, ONE);
					else
						histo.put(s, d+1.0);
				}
			}
			else
			{
				for (int i = theMembers.nextSetBit(0); i >= 0; i = theMembers.nextSetBit(i+1))
				{
					String s = theColumn.getString(i);
					Double d = histo.get(s);
					if (d == null)
						histo.put(s, ONE);
					else
						histo.put(s, d+1.0);
				}
			}

			final double div = size; // avoid casts
			for (Entry<String, Double> e : histo.entrySet())
				histo.put(e.getKey(), e.getValue()/div);

			if (DEBUG)
			{
				double d = 0.0;
				for (Double p : histo.values())
					d += p;
				System.out.format(DIST_INFO, histo.size(), d);
			}

			return histo;
		}

		@Override
		public Column getColumn()
		{
			return itsColumn;
		}

		@Override
		public int getSize()
		{
			return itsDensities.size();
		}

		@Override
		public void print()
		{
			// sorted for invocation invariant order
			SortedMap<String, Double> map = new TreeMap<String, Double>(itsDensities);
			for (Entry<String, Double> e : map.entrySet())
				System.out.format(BIN_INFO, e.getKey(), e.getValue());
		}

		@Override
		public double getDensity(String theValue)
		{
			Double d = itsDensities.get(theValue);
			return (d == null) ? 0.0 : d;
		}
	}

	// nrRows=S, domain=S, outlier=S -> ProbDenF +- PDF
	// nrRows=S, domain=S, outlier=B -> ProbDenF -- PDF
	// nrRows=S, domain=B, outlier=S -> ProbDenF -- PDF
	// nrRows=S, domain=B, outlier=B -> ProbDenF -- PDF
	// nrRows=B, domain=S, outlier=S -> ProbDenF +- PDF
	// nrRows=B, domain=S, outlier=B -> ProbDenF -- PDF
	// nrRows=B, domain=B, outlier=S -> ProbDenF  - PDF
	// nrRows=B, domain=B, outlier=B -> ProbDenF  - PDF
	public static void main(String[] args)
	{
		int S = 100;
		int B = 100000;

		int nrRows = S;
		Random r = new Random();
/*
		Column c = new Column("TEST", "TEST", AttributeType.NUMERIC, 0, nrRows);
		for (int i = 0; i < nrRows; ++i)
			c.add(r.nextInt(S));
		// outlier
		c.add(r.nextInt(B));

		BitSet b = new BitSet();
		b.set(0, nrRows);

		PDFHash pdf_h = new PDFHash(c);
		PDFHashDelta pdf_hd = new PDFHashDelta(c);
		PDFSorted pdf_s = new PDFSorted(c, b);

		PDF[] pdfs = { pdf_h, pdf_hd, pdf_s };

		for (PDF pdf : pdfs)
		{
			System.out.println("\n###############################");
			pdf.print();

//			Column col = pdf.getColumn();
//			for (int i = 0, j = col.size(); i < j; ++i)
//				System.out.format("%f\t%.17f%n", col.getFloat(i), pdf.getDensity(col.getFloat(i)));
		}

		// ProbabilityDensityFunction
		ProbabilityDensityFunction old = new ProbabilityDensityFunction(c);
		// old_pre
		Column old_pre = new Column("old_pre", "old_pre", AttributeType.NUMERIC, 0, old.size());
		for (int i = 0, j = old.size(); i < j; ++i)
			old_pre.add(old.getDensity(i));
		new PlotWindow(old_pre);
		// old_post
		old.smooth();
		Column old_post = new Column("old_post", "old_post", AttributeType.NUMERIC, 0, old.size());
		for (int i = 0, j = old.size(); i < j; ++i)
			old_post.add(old.getDensity(i));
		new PlotWindow(old_post);
//		new ModelWindow(old_post, old, null, "OLD");

		// new PDF
		double[] da = pdf_s.itsDensities;
		Column d = new Column("PDF", "PDF", AttributeType.NUMERIC, 0, da.length);
		for (double p : da)
			d.add((float) p);
		new PlotWindow(d);
*/
		// Histo test
		Column hc = new Column("TEST", "TEST", AttributeType.NOMINAL, 0, nrRows);
		for (int i = 0; i < nrRows; ++i)
			hc.add(String.valueOf((char) (r.nextInt(26) + 65)));
		Histo h = new Histogram(hc);
		h.print();

		for (int i = 0, j = hc.size(); i < j; ++i)
			System.out.format("%s\t%.17f%n", hc.getNominal(i), h.getDensity(hc.getNominal(i)));
	}
}
