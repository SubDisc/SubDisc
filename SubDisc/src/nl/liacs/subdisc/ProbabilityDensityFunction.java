package nl.liacs.subdisc;

public class ProbabilityDensityFunction
{
	private float[] itsDensity;
	private float itsMin, itsMax, itsBinWidth;
	private int itsNrBins = 100;

	public ProbabilityDensityFunction(Column theData)
	{
		//TODO include outlier treatment
		itsMin = theData.getMin();
		itsMax = theData.getMax();
		itsBinWidth = (itsMax-itsMin)/itsNrBins;
		itsDensity = new float[itsNrBins];

		int aSize = theData.size();
		for (int i=0; i<aSize; i++)
		{
			float aValue = theData.getFloat(i);
			add(aValue);
		}
	}

	public int getDensity(float theValue)
	{
		int anIndex = getIndex(theValue);

		if (anIndex == -1)
			return 0;
		else
			return getDensity(anIndex);
	}

	private int getIndex(float aValue)
	{
		if (aValue == itsMax)
			return itsNrBins-1;
		else
			return (int) ((aValue-itsMin)/itsBinWidth);
	}

	private void add(float aValue)
	{
		int aBin = getIndex(aValue);
		itsDensity[aBin]++;
	}

	public void print()
	{
		Log.logCommandLine("ProbabilityDensityFunction:\n");
		for (int i = 0; i < itsDensity.length; i++)
			Log.logCommandLine("  " + i + "	" + itsDensity[i]);
		Log.logCommandLine("");
	}
}
