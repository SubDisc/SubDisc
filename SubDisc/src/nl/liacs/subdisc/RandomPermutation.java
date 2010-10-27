package nl.liacs.subdisc;

import java.util.Random;

public class RandomPermutation{
	private int itsSize;
	private static Random itsRandom;
	
	public RandomPermutation(int theSize)
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
			int aSwitchIndex = (int) (itsRandom.nextDouble()*i);
			int aTemp = result[aSwitchIndex];
			result[aSwitchIndex] = result[i-1];
			result[i-1] = aTemp;
		}
		
		// ... and presto!
		return result;
	}
}