package nl.liacs.subdisc.postprocess;

import nl.liacs.subdisc.*;

public class ResultFile
{
	private final String itsFile;
	private final float[] itsScores;

	ResultFile(Table theTable)
	{
		itsFile = theTable.getSource();
		itsScores = theTable.getColumn("Quality").getFloats();
	}

	String getFile() { return itsFile; }

	int size() { return itsScores.length; }

	double getMean(int topK)
	{
		if (topK > size())
			return Double.NaN;

		double sum = 0.0;

		for (int i = 0; i < topK; ++i)
			sum += itsScores[i];

		return sum / topK;
	}
}
