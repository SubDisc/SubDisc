package nl.liacs.subdisc;

import java.util.*;

public class NumericDomain
{
	private float[] itsValues;
	private Column itsColumn;
	private ArrayList<Float> itsThresholds; //used?

	public static final int MIN = 0;
	public static final int MAX = 1;
	public static final int AVG = 2;

	//builds a sorted (with potential duplicates) domain of theColumn
	public NumericDomain(Column theColumn)
	{
		itsValues = new float[theColumn.size()];

		for (int i=0; i<theColumn.size(); i++)
			itsValues[i] = theColumn.getFloat(i);

		Arrays.sort(itsValues);
	}

	//builds a sorted (with potential duplicates) domain of theColumn
	public NumericDomain(Column theColumn, BitSet theSubset)
	{
		itsValues = new float[theSubset.cardinality()];

		int j=0;
		for (int i=0; i<theColumn.size(); i++)
			if (theSubset.get(i))
			{
				itsValues[j] = theColumn.getFloat(i);
				j++;
			}

		Arrays.sort(itsValues);
	}

	public int size()
	{
		return itsValues.length;
	}

	public float getValue(int theIndex) { return itsValues[theIndex]; }
	public float[] getValues() { return itsValues; }

	public float getValueAbove(float theValue, boolean isFirstColumn)
	{
		float aMin = Float.POSITIVE_INFINITY;

		for (int i=0; i<size(); i++)
		{
			float aValue = getValue(i);
			if ((aValue > theValue) && (aValue < aMin))
				aMin = aValue;
		}

		if (aMin == Float.POSITIVE_INFINITY)
			return getValueBetween(theValue, theValue + 1.0f);
		else
			return getValueBetween(theValue, aMin);
	}

	public float getValueBelow(float theValue, boolean isFirstColumn)
	{
		float aMax = Float.NEGATIVE_INFINITY;

		for (int i=0; i<size(); i++)
		{
			float aValue = getValue(i);
			if ((aValue < theValue) && (aValue > aMax))
				aMax = aValue;
		}

		if (aMax == Float.NEGATIVE_INFINITY)
			return getValueBetween(theValue - 1.0f, theValue);
		else
			return getValueBetween(aMax, theValue);
	}

	public float getValueBetween(float theLow, float theHigh)
	{
		float aBetween = (theLow + theHigh)/2.0f;
		float aShiftRight = 1000000;
		float aShiftLeft = 1;

		for (int i=0; i<40; i++)
		{
			float aShift = aShiftLeft/aShiftRight;
			float aValue = ((long)(aBetween*aShift))/aShift;
			aShiftLeft = 10*aShiftLeft;
			if ((aValue < theHigh) && (aValue > theLow))
				return aValue;
		}

		return aBetween;
	}

    public void print()
	{
        Log.logCommandLine("NumericDomain (" + size() + " rows):");
		for (int i = 0; i < itsValues.length; i++)
			Log.logCommandLine(i + ":  " + itsValues[i] );
		Log.logCommandLine("");
    }

	//compute sum of itsValues between S and E (excluding E)
	public float computeSum(int theStart, int theEnd)
	{
		float aSum = 0;
		if (theEnd > size())
			theEnd = size();
		for (int i=theStart; i<theEnd; i++)
			aSum += itsValues[i];
		return aSum;
	}

	//XXX: Barbara
	//compute sum squared deviations of the values between S and E (excluding E)
	//the sum squared deviations is calculated: E[(X-mu)^2]. From this value, the standard deviation can be computed.
	//the sample standard deviation is calculated by dividing by n-1 instead of dividing by n
	//(division is done in RuleMeasure, for z-score and t-test)
	public float computeSumSquaredDeviations(int theStart, int theEnd)
	{
		if (theEnd > size())
			theEnd = size();
			float aMean = computeSum(theStart, theEnd)/(theEnd-theStart);
			float aSum = 0;
			for (int i=theStart; i<theEnd; i++){
			    aSum += ((itsValues[i]-aMean)*(itsValues[i]-aMean));
			}
			return aSum;
	}

	public int[] computeMedianFrequencyCounts(float theMedian, int theStart, int theEnd){
		int aBelowMedian = 0;
		int anAboveMedian = 0;
		float[] values = getValues(theStart, theEnd);
		for(int i=0;i<values.length;i++)
		{
			if(values[i]<=theMedian) aBelowMedian++;
			else if(values[i]>theMedian) anAboveMedian++;
		}
		int[] aCount = {anAboveMedian,aBelowMedian};
		return aCount;
	}

	//compute the median of a list of values (by sorting them first)
	public float computeMedian(int theStart, int theEnd)
	{
		return computeMedian(getValues(theStart, theEnd));
	}

	public float computeMedian(float[] theValues)
	{
		float aMedian = 0;
		Arrays.sort(theValues);

	  	if ((theValues.length & 1) == 0){//even, bit check (even numbers end with 0 as last bit)
			aMedian = (theValues[(theValues.length / 2) - 1] + theValues[theValues.length / 2]) / 2;
	  	}
	  	else aMedian = theValues[theValues.length / 2];
	  	return aMedian;
	}

	//compute median absolute deviation
	public float computeMedianAD(int theStart, int theEnd)
	{
		//compute median of whole subgroup
		float aMedian = computeMedian(theStart, theEnd);

		if (theEnd > size())
			theEnd = size();
		float anAbsDeviations[] = new float[theEnd-theStart];
		int j = 0;
		//compute the absolute deviations of the elements in the subgroup.
		for(int i=theStart; i<theEnd; i++)
		{
			anAbsDeviations[j] = Math.abs(itsValues[i]-aMedian);
			j++;
		}
		//compute the MAD: the median of absolute deviations
		return computeMedian(anAbsDeviations);
	}

	//Get a list of the values of the subgroup. Especially useful when working with medians etc.
	private float[] getValues(int theStart, int theEnd)
	{
		if(theEnd > size())
			theEnd = size();
		float[] aValues = new float[theEnd-theStart];
		int j = 0;
		for (int i=theStart; i<theEnd; i++)
		{
			aValues[j] = itsValues[i];
			j++;
		}
		return aValues;
	}

	public void equalHeight(int theNrBags)
	{
		itsThresholds = new ArrayList<Float>();

		for (int i=1; i<theNrBags; i++)
		{
			float anIndex = (i*itsValues.length)/(float)theNrBags;
			itsThresholds.add(new Float(itsValues[(int)anIndex]));
			Log.logCommandLine("  " + itsValues[(int)anIndex]);
		}
	}
}