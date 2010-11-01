package nl.liacs.subdisc;

import java.util.Random;
import java.util.BitSet;

public class CrossValidation{
	private int itsSize;
	private static Random itsRandom;
	
	public CrossValidation(int theSize)
	{
		itsSize = theSize;
		itsRandom = new Random(System.currentTimeMillis());
	}
	
	// returns a random permutation of the integers [1,...,itsSize]. To be used for cross-validation.
	public int[] getRandomPermutation()
	{
		int[] result = new int[itsSize];
		
		// initialize result array to be [1,2,...,itsSize]
		for (int i=0; i<itsSize; i++)
			result[i] = i+1;
		
		// Knuth shuffle
		// notice i>1 in for-loop; for i=1 we will always swap the first element with itself, hence we can skip this step
		for (int i=itsSize; i>1; i--)
		{
			int aSwitchIndex = itsRandom.nextInt(i);
			int aTemp = result[aSwitchIndex];
			result[aSwitchIndex] = result[i-1];
			result[i-1] = aTemp;
		}
		
		return result;
	}
	
	// generates k test sets for cross-validation
	public BitSet[] getKTestSets(int theK)
	{
		// generate the random permutation on basis of which the k test sets will be filled
		int[] aRandomPermutation = getRandomPermutation();
		
		// initialize the test sets as k empty BitSets in an array
		BitSet[] aResult = new BitSet[theK];
		for (int i=0; i<theK; i++)
			aResult[i] = new BitSet(itsSize);
		
		// fill the test sets by looping over the BitSets and setting the bit corresponding to the next element of the random permutation
		for (int i=0; i<itsSize; i++)
			aResult[i % theK].set(aRandomPermutation[i]-1);
		
		return aResult;
	}
	
	// given k test sets, generates the corresponding training sets
	public BitSet[] getKTrainingSets(int theK, BitSet[] theTestSets)
	{
		// initialize result
		BitSet[] aResult = new BitSet[theK];
		
		// clone and logical NOT on the test sets
		for (int i=0; i<theK; i++)
		{
			aResult[i] = (BitSet) theTestSets[i].clone();
			aResult[i].flip(0,itsSize);
		}
		
		return aResult;
	}
}