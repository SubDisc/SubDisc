package nl.liacs.subdisc;

import java.util.*;

public class Miki
{
	// max of 32-bits mikis, MSB/LSB order irrelevant, but this prints nicely
	private static final int MASK    = 0x8000_0000;
	private static final int NOT_SET = -1; // must be outside valid range: < 0

	// private (final) inner class, omit getters and setters
	// could be int[] replacing Permutation pointer by index; no difference for
	// heap space < 32GB and [] has length field; probably uses 32B: 8+5*4+align
	private static final class Permutation
	{
		int bits = 0; // maximum of 32-miki (never encountered in the wild)
		int size = 0; // number of bits in use, see below
		int sum  = 0; // sum - ones = zeros: allows testing only BitSet.set_bits
		int one  = 0; // number of ones for a possible extension
		// size can be inferred using variable i in main loop but it is used for
		// direct extension of bits + '1', this avoids array/Hash/Tree lookups
		Permutation extenedWithOne = null;
	}

	//  upper bound number combinations: min( 2^(min(k, aNrColumns)) , aNrRows )
	static final double getMiki(SortedSet<Subgroup> theSubgroups, int k)
	{
Timer t = new Timer();
		// package private, still check; but max NrRows (2^31) limits MaxCombis
		if (k <= 0 || k > 32)
			throw new IllegalArgumentException("Miki: 0 < k <= 32 required; k = " + k);

		// throws NullPointerException when null
		if (theSubgroups.isEmpty())
			return 0.0; // by definition, an empty set contains no information

		int aNrColumns      = theSubgroups.size();
		int aNrRows         = theSubgroups.first().getParentSet().getTotalCoverage();
		BitSet[] aBitSets   = new BitSet[aNrColumns];
		Permutation[] aRows = new Permutation[aNrRows];
		int aMaxNrBits      = Math.min(k, aNrColumns);
		int aMaxCombis      = Math.min((int) Math.pow(2.0, aMaxNrBits), aNrRows);
		List<Permutation> aPermutations = new ArrayList<>(aMaxCombis);
		// do not add yet, a (cross-validation) dataset might have zero 0s or 1s
		Permutation zero    = new Permutation(); zero.size = 1;
		Permutation one     = new Permutation(); one.size  = 1; one.bits = MASK;

		// first miki is always the Subgroup with the ratio closest to 50/50
		float aHalf = aNrRows / 2.0f; // mimic BinaryTable, no rounding error
		float aDiff = Float.POSITIVE_INFINITY;
		int aBest   = NOT_SET;

		int idx = -1;
		for (Subgroup s : theSubgroups)
		{
			aBitSets[++idx] = s.getMembers();
			// ResultWindow call, no s.killMembers: guards against Table changes

			if (aDiff == 0.0)
				continue;

			float f = (s.getCoverage() - aHalf);
			if (Math.abs(f) < Math.abs(aDiff))
			{
				aDiff = f;
				aBest = idx;
				Log.logCommandLine(String.format("N=%d\t|s|=%d\taBest=%d \taHalf=%f\taDiff=%f",
													aNrRows, s.getCoverage(), aBest, aHalf, aDiff));
			}
		}

		// computation starts here, first item sets up/is taken out of the loop
		BitSet aMiki = new BitSet(aNrColumns);
		aMiki.set(aBest);
		double aMaxH = 0.0;
		double N     = aNrRows;

		BitSet b = aBitSets[aBest];
		for (int i = 0; i < aNrRows; ++i)
			aRows[i] = b.get(i) ? one : zero;

		aBest = (int) (aDiff + aHalf); // subgroup.getCoverage(), cast is safe
		if (aDiff < aHalf)
		{
			zero.sum = (aNrRows - aBest);
			aMaxH   += fraction((zero.sum), N);
			aPermutations.add(zero);
		}
		if (aDiff > -aHalf)
		{
			one.sum = aBest;
			aMaxH  += fraction(aBest , N);
			aPermutations.add(one);
		}

		for (int i = 1; i < aMaxNrBits; ++i) // one bit set already so i is size
		{
			aBest = NOT_SET;

			for (int j = aMiki.nextClearBit(0); j < aNrColumns; j = aMiki.nextClearBit(j + 1))
			{
				b = aBitSets[j];
				for (int l = b.nextSetBit(0); l >= 0; l = b.nextSetBit(l + 1))
					++aRows[l].one;

				// do not loop aRows, aPermutations is smaller (no duplicates)
				// XXX this might not be true at some point, such that looping
				//     over rows becomes cheaper
				//     this is is because branches (Permutations) that die out
				//     (have a count of 0) are not removed, but are always
				//     extended with a 0, adjusting this behaviour would require
				//     checking that over the the whole of the round before the
				//     previous one a Permutation has not been used
				//     (needs not always setting size, and size = currentSize-2)
				double H = computeEntropy(aPermutations, N);

				if (H > aMaxH)
				{
					aMaxH = H;
					aBest = j;
					aMiki.set(aBest);   // for ease of printing
					Log.logCommandLine("found a new maximum: " + aMiki + ": " + (aMaxH / Math.log(2.0)));
					aMiki.clear(aBest); // <-- VERY IMPORTANT
				}
			}

			if (aBest == NOT_SET)
				break;

			aMiki.set(aBest);

			extendPermutations(aRows, aPermutations, aBitSets[aBest], i);
		}

System.out.println("Miki="+ aMiki + " H=" + (aMaxH / Math.log(2.0)));
System.out.println(t.getElapsedTimeString());
		// aMiki to ItemSet + set entropy - determine what to return
		return (aMaxH / Math.log(2.0));
	}

	// computes the base-10 logarithm, see fraction()
	private static final double computeEntropy(List<Permutation> thePermutations, double N)
	{
		double H = 0.0;

		for (Permutation p : thePermutations)
		{
			H += fraction(p.sum - p.one, N);
			H += fraction(        p.one, N);
			p.one = 0;
		}

		return H;
	}

	// division by Math.log(2.0) should be done only once, after sum is computed 
	private static final double fraction(int theCount, double N)
	{
		return (theCount == 0) ? 0.0 : -(theCount * Math.log(theCount / N)) / N;
	}

	// Permutation.bits only needs to be extended for a 1, based on theExtension
	private static final void extendPermutations(Permutation[] theRows, List<Permutation> thePermutations, BitSet theExtension, int theCurrentSize)
	{
		for (int i = 0, m = (MASK >>> theCurrentSize), n = (theCurrentSize + 1); i < theRows.length; ++i)
		{
			Permutation p = theRows[i];

			if (p.size != n)
			{
				p.size = n;
				p.sum  = 0;
			}

			if (!theExtension.get(i))
			{
				++p.sum;
			}
			else
			{
				Permutation pe = p.extenedWithOne;
				if ((pe == null) || (pe.size <= theCurrentSize) || ((pe.bits & m) == 0))
					theRows[i] = extend(thePermutations, p, m, n);
				else
					theRows[i] = pe;
				++theRows[i].sum;
			}
		}
	}

	private static final Permutation extend(List<Permutation> thePermutations, Permutation theExtendee, int theMask, int theNewSize)
	{
		Permutation pe             = new Permutation();
		pe.bits                    = (theExtendee.bits | theMask);
		pe.size                    = theNewSize;
		pe.sum                     = 0;
		theExtendee.extenedWithOne = pe;
		thePermutations.add(pe); // the addition to thePermutations is crucial
		return pe;
	}
}
