package nl.liacs.subdisc;

import java.util.Arrays;

public class LabelRankingMatrix
{
	//Note that values in the matrix can be fractional
	//the matrix is full, not a half triangle (easier for now)
	//...

	private final int itsSize; //number of labels
	private float[][] itsMatrix; //the actual values.

	public LabelRankingMatrix(int theSize)
	{
		itsSize = theSize;
		itsMatrix = new float[theSize][theSize];
		Arrays.fill(itsMatrix, 0f);
	}

	public LabelRankingMatrix(LabelRanking theRanking)
	{
		itsSize = theRanking.getSize();

		for (int i=0; i<itsSize; i++)
			for (int j=0; j<=itsSize; j++)
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
			for (int j=0; j<=itsSize; j++)
				itsMatrix[i][j] += theMatrix.itsMatrix[i][j];
	}

	public void divide(float theValue)
	{
		for (int i=0; i<itsSize; i++)
			for (int j=0; j<=itsSize; j++)
				itsMatrix[i][j] /= theValue;
	}

	public float get(int i, int j) { return itsMatrix[i][j]; }

	public void print()
	{
		for (int i=0; i<itsSize; i++)
		{
			String aRow = new String("[");
			for (int j=0; j<=itsSize; j++)
				aRow += (", " + itsMatrix[i][j]);
			Log.logCommandLine(aRow);
		}
	}
}
