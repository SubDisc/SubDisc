package nl.liacs.subdisc;

import java.util.Arrays;

public class LabelRankingMatrix
{
	//Note that values in the matrix can be fractional
	//the matrix is full, not a half triangle (easier for now)
	//...

	private int itsSize; //number of labels
	private float[][] itsMatrix; //the actual values.
	int[] pairwVector;

	public LabelRankingMatrix(int theSize)
	{
		itsSize = theSize;
		itsMatrix = new float[theSize][theSize];
		for (int i=0; i<theSize; i++)
			Arrays.fill(itsMatrix[i], 0f);
	}

 	public LabelRankingMatrix(LabelRanking theRanking)
	{
		itsSize = theRanking.getSize();
		itsMatrix = new float[itsSize][itsSize];

		for (int i=0; i<itsSize; i++)
			for (int j=0; j<itsSize; j++)
				if (i == j)
					itsMatrix[i][j] = 0;
				else if (theRanking.getRank(i) > theRanking.getRank(j))
					itsMatrix[i][j] = 1;
				else
					itsMatrix[i][j] = -1;
	}

	public void add(LabelRankingMatrix theMatrix)
	{
		for (int i=0; i<itsSize; i++)
			for (int j=0; j<itsSize; j++)
				itsMatrix[i][j] += theMatrix.itsMatrix[i][j];
	}

	public void divide(float theValue)
	{
		for (int i=0; i<itsSize; i++)
			for (int j=0; j<itsSize; j++)
				itsMatrix[i][j] /= theValue;
	}
	
	public float distance(LabelRankingMatrix theMatrix)
	{
		float aDistance = 0;
		for (int i=0; i<itsSize; i++)
			for (int j=(i+1); j<=itsSize; j++)
				aDistance += Math.abs(itsMatrix[i][j] - theMatrix.itsMatrix[i][j]);
		return aDistance;
	}
	
	public float altDistance(LabelRankingMatrix theMatrix)
	{
		float aParameter = 1;
		float aDistanceTest = 0;
		float aDistance = 0;
		for (int i=0; i<itsSize; i++)
			for (int j=(i+1); j<=itsSize; j++) {
				aDistanceTest = Math.abs(itsMatrix[i][j] - theMatrix.itsMatrix[i][j]);
				if ( aDistanceTest >= aParameter) {
					aDistance += 1;
				} else {
					aDistance += 0;
				}
			}
		return aDistance;
	}

	public float get(int i, int j) { return itsMatrix[i][j]; }

	public void print()
	{
		Log.logCommandLine("  ================================");
		for (int i=0; i<itsSize; i++)
		{
			String aRow = new String(LabelRanking.getLabel(i) + " [");
			for (int j=0; j<itsSize; j++)
				if (j==0)
					aRow += (itsMatrix[i][j]);
				else
					aRow += (", " + itsMatrix[i][j]);
			Log.logCommandLine(aRow + "]");
		}
		Log.logCommandLine("  ================================");
	}

	public static void main(String[] args)
	{
        System.out.println("na boa");

		int aQuantity = 3;
        LabelRanking a1 = new LabelRanking("abcd");
		LabelRanking a2 = new LabelRanking("dbac");
		LabelRanking a3 = new LabelRanking("bdac");
		LabelRankingMatrix anAverageMatrix = new LabelRankingMatrix(4); //empty matrix first
		anAverageMatrix.add(new LabelRankingMatrix(a1));
		anAverageMatrix.add(new LabelRankingMatrix(a2));
		anAverageMatrix.add(new LabelRankingMatrix(a3));
		anAverageMatrix.divide(aQuantity);

		anAverageMatrix.print();
    }
}
