package nl.liacs.subdisc;

import java.util.Arrays;

public class LabelRankingMatrix
{
	//Note that values in the matrix can be fractional
	//the matrix is full, not a half triangle (easier for now)
	//...

	private int itsSize; //number of labels
	private float[][] itsMatrix; //the actual values.

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
				if (theRanking.getRank(i) < theRanking.getRank(j))
					itsMatrix[i][j] = 1;
				else if (theRanking.getRank(i) > theRanking.getRank(j))
					itsMatrix[i][j] = -1;
				else
					itsMatrix[i][j] = 0;
	}

	public Object clone()
	{
		LabelRankingMatrix aClone = new LabelRankingMatrix(itsSize);
		for (int i=0; i<itsSize; i++)
			for (int j=0; j<itsSize; j++)
				aClone.itsMatrix[i][j] = itsMatrix[i][j];
		return aClone;
	}

	public int getSize() { return itsSize; }

	public void add(LabelRankingMatrix theMatrix)
	{
		for (int i=0; i<itsSize; i++)
			for (int j=0; j<itsSize; j++)
				itsMatrix[i][j] += theMatrix.itsMatrix[i][j];
	}

	public void subtract(LabelRankingMatrix theMatrix)
	{
		for (int i=0; i<itsSize; i++)
			for (int j=0; j<itsSize; j++)
				itsMatrix[i][j] -= theMatrix.itsMatrix[i][j];
	}

	public void divide(float theValue)
	{
		for (int i=0; i<itsSize; i++)
			for (int j=0; j<itsSize; j++)
				itsMatrix[i][j] /= theValue;
	}
	
	public float frobeniunsNorm(LabelRankingMatrix theMatrix)
	{
		double fNorm = 0;
		int count = 0;
		for (int i=0; i<itsSize; i++)
			for (int j=i+1; j<itsSize; j++)
			{
				fNorm += Math.pow(theMatrix.itsMatrix[i][j],2);
				count += 1;
			}
		return (float) Math.sqrt(fNorm)/count;
	}
	
//	public float normDistance(LabelRankingMatrix theMatrix)
//	{
//		double aDistance = 0;
//		subtract(theMatrix);
//		return (float) Math.sqrt(aDistance);
//	}
	
	public float normDistance(LabelRankingMatrix theMatrix)
	{
		double aDistance = 0;
		int count = 0;
		for (int i=0; i<itsSize; i++)
			for (int j=i+1; j<itsSize; j++)
			{
				aDistance += Math.pow((itsMatrix[i][j] - theMatrix.itsMatrix[i][j])/2,2);
				count += 1;
			}
		return (float) Math.sqrt(aDistance)/count;
	}
	
	public float wnormDistance(LabelRankingMatrix theMatrix)
	{
		return normDistance(theMatrix)*frobeniunsNorm(theMatrix);
	}
	
	public float minDistance(LabelRankingMatrix theMatrix)
	{
		
		float aMin = 1f/0f;
		for (int i=0; i<itsSize; i++)
		{
			float aDistance = 0;
			for (int j=0; j<itsSize; j++)
			{
				if (i!=j) aDistance += Math.pow((itsMatrix[i][j] - theMatrix.itsMatrix[i][j])/2, 2);
			}
			aDistance = (float) Math.sqrt(aDistance)/(itsSize-1);
			aMin = Math.min(aMin, aDistance);
		}
		return aMin;
	}
	
	public float labelwiseMax(LabelRankingMatrix theMatrix)
	{
		
		float aMax = -1f/0f;
		for (int i=0; i<itsSize; i++)
		{
			float aDistance = 0;
			for (int j=0; j<itsSize; j++)
			{
				if (i!=j) aDistance += Math.pow((itsMatrix[i][j] - theMatrix.itsMatrix[i][j])/2, 2);
			}
			aDistance = (float) Math.sqrt(aDistance)/(itsSize-1);
			aMax = Math.max(aMax, aDistance);
		}
		return aMax;
	}
	
	public float pairwiseMax(LabelRankingMatrix theMatrix)
	{
		float aMax = -1f/0f;
		for (int i=0; i<itsSize; i++)
			for (int j=i+1; j<itsSize; j++)
				aMax = Math.max(aMax, Math.abs((itsMatrix[i][j] - theMatrix.itsMatrix[i][j])/2));
		return aMax;
	}
	
//	public float avgDistance(LabelRankingMatrix theMatrix)
//	{
//		float aDistance = 0;
//		int count=0;
//		for (int i=0; i<itsSize; i++)
//			for (int j=i+1; j<itsSize; j++)
//			{
//				aDistance += Math.abs(itsMatrix[i][j] - theMatrix.itsMatrix[i][j]);
//				count += 1;
//			}
//		return aDistance/count;
//	}
	
	public float covDistance(LabelRankingMatrix theMatrix)
	{
		float aCovariance = 0;
		for (int i=0; i<itsSize; i++)
		{
			float aDistance = 0;
			float theLabelMean = labelMean(theMatrix.itsMatrix, i);
			float theLabelMean2 = labelMean(itsMatrix, i);
			for (int j=0; j<itsSize; j++)
			{
				if (i!=j)	aDistance += (theMatrix.itsMatrix[i][j] - theLabelMean)*(itsMatrix[i][j] - theLabelMean2);
			}
			aCovariance += -aDistance/(itsSize-2);
		}
		return (float) aCovariance/itsSize;
	}
	
	public float labelMean(float[][] theRankingMatrix, int label)
	{
		 float sum = 0;
		 for (int i = 0; i < itsSize; i++)
			 sum += theRankingMatrix[label][i];
		 return sum / (itsSize-1);
	}

	public float homogeneity(LabelRankingMatrix theMatrix)
	{
		float aDistance = 0;

		for (int i=0; i<itsSize; i++)
			for (int j=i+1; j<itsSize; j++)
			{
				aDistance += Math.abs(theMatrix.itsMatrix[i][j]);
			}
		aDistance = aDistance/((itsSize*(itsSize+1))/2);
		//Log.logCommandLine("<>" + aDistance + "<>");
		return aDistance;
	}

	public float get(int i, int j) { return itsMatrix[i][j]; }

	public void print()
	{
		Log.logCommandLine("  ================================");
		for (int i=0; i<itsSize; i++)
		{
			String aRow = new String(LabelRanking.getLetter(i) + " [");
			for (int j=0; j<itsSize; j++)
				if (j==0)
					aRow += (itsMatrix[i][j]);
				else
					aRow += (", " + itsMatrix[i][j]);
			Log.logCommandLine(aRow + "]");
		}
		Log.logCommandLine("  ================================");
	}

	/*
	 * Print the higher values in the LabelRankingMatrix
	 */
//	String anOutputPrint;
//	public void printMax()
//	{
//
//		float aFloat = 0;
//
//		Log.logCommandLine("  ================================");
//		for (int i=0; i<itsSize; i++)
//		{
//			for (int j=i+1; j<itsSize; j++) {
//				if (aFloat < Math.abs(itsMatrix[i][j])) {
//					anOutputPrint = new String(LabelRanking.getLetter(i) + ">" + LabelRanking.getLetter(j) + ": " + itsMatrix[i][j] + "  (" + i + ">" + j + ")");
//					aFloat = Math.abs(itsMatrix[i][j]);
//				}
//			}
//		}
//		Log.logCommandLine(anOutputPrint);
//		Log.logCommandLine("  ================================");
//	}
	
	String anOutput;
	public String findMax()
	{
		float aFloat = 0;
		float roundedValue;

		for (int i=0; i<itsSize; i++)
		{
			for (int j=i+1; j<itsSize; j++) {
				if (aFloat < Math.abs(itsMatrix[i][j])) {
					roundedValue = Math.round(itsMatrix[i][j]*1000f)/1000f;
					anOutput = new String(LabelRanking.getLetter(i) + ">" + LabelRanking.getLetter(j) + " " + roundedValue);
					aFloat = Math.abs(itsMatrix[i][j]);
				}
			}
		}
		return anOutput;
	}
	
	public String findMaxLabel()
	{
		float aFloat = -1f/0f;
		float roundedValue;
		
		for (int i=0; i<itsSize; i++)
		{
			float theLabelSum = 0;
			for (int j=0; j<itsSize; j++) {
				if (i!=j)	theLabelSum += Math.abs(itsMatrix[i][j]);
			}
			theLabelSum = theLabelSum/(itsSize-1);
			if (aFloat < theLabelSum) {
				roundedValue = Math.round(aFloat*1000f)/1000f;
				anOutput = new String(LabelRanking.getLetter(i) + " (" + roundedValue + ")");
				aFloat = theLabelSum;
			}
		}
		return anOutput;
	}

//	public LabelRanking LabelRankingMatrix2ranking(LabelRankingMatrix theMatrix)
//	{
//		itsSize = theMatrix.getSize();
//		aRanking = new int[itsSize];
//
//		for (int i=0; i<itsSize; i++)
//			for (int j=0; j<itsSize; j++)
//				if (itsMatrix[i][j] >= 0)
//					itsMatrix[i][j] = 0;
//				else if (theRanking.getRank(i) > theRanking.getRank(j))
//					itsMatrix[i][j] = 1;
//				else
//					itsMatrix[i][j] = -1;
//
//	}
}
