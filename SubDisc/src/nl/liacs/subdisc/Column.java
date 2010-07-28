package nl.liacs.subdisc;

import java.util.*;

public class Column
{
	private int itsType; //types in Attribute
	private ArrayList<Float> itsFloats;
	private ArrayList<String> itsNominals;
	private BitSet itsBinaries;
	private int itsSize;
	private float itsMin = 0.0f;
	private float itsMax = 0.0f;

	public Column(int theType, int theNrRows)
	{
		itsSize = 0;
		itsType = theType;
		switch (theType)
		{
			case Attribute.NUMERIC :
			case Attribute.ORDINAL : { itsFloats = new ArrayList<Float>(theNrRows); break; }
			case Attribute.NOMINAL : { itsNominals = new ArrayList<String>(theNrRows); break; }
			case Attribute.BINARY : { itsBinaries = new BitSet(theNrRows); break; }
		}
	}

	public void add(float theFloat) { itsFloats.add(new Float(theFloat)); itsSize++; }
	public void add(boolean theBinary)
	{
		if (theBinary)
			itsBinaries.set(itsSize);
		itsSize++;
	}
	public void add(String theNominal) { itsNominals.add(theNominal); itsSize++; }
	public int size() { return itsSize; }
	public int getType() { return itsType; }
	public float getFloat(int theIndex) { return itsFloats.get(theIndex).floatValue(); }
	public String getNominal(int theIndex) { return itsNominals.get(theIndex); }
	public boolean getBinary(int theIndex) { return itsBinaries.get(theIndex); }
	public String getString(int theIndex)
	{
		switch (itsType)
		{
			case Attribute.NUMERIC :
			case Attribute.ORDINAL : { return itsFloats.get(theIndex).toString(); }
			case Attribute.NOMINAL : { return getNominal(theIndex); }
			case Attribute.BINARY : { return getBinary(theIndex)?"1":"0"; }
		}
		return itsNominals.get(theIndex);
	}
	public BitSet getBinaries() { return itsBinaries; }

	public boolean isNominalType() { return itsType == Attribute.NOMINAL; }
	public boolean isNumericType() { return itsType == Attribute.NUMERIC; }
	public boolean isOrdinalType() { return itsType == Attribute.ORDINAL; }
	public boolean isBinaryType() { return itsType == Attribute.BINARY; }

	public float getMin()
	{
		updateMinMax();
		return itsMin;
	}

	public float getMax()
	{
		updateMinMax();
		return itsMax;
	}

	private void updateMinMax()
	{
		if (itsMin == itsMax)
			for (int i=0; i<itsSize; i++)
			{
				float aValue = getFloat(i);
				if (aValue > itsMax)
					itsMax = aValue;
				if (aValue < itsMin)
					itsMin = aValue;
			}
	}

	public void print()
	{
		switch (itsType)
		{
			case Attribute.NUMERIC :
			case Attribute.ORDINAL : { Log.logCommandLine(itsFloats.toString()); break; }
			case Attribute.NOMINAL : { Log.logCommandLine(itsNominals.toString()); break; }
			case Attribute.BINARY : { Log.logCommandLine(itsBinaries.toString()); break; }
		}
	}

	public TreeSet<String> getDomain()
	{
		TreeSet<String> aResult = new TreeSet<String>();
		if (isBinaryType())
		{
			aResult.add("0");
			aResult.add("1");
			return aResult;
		}

		for (int i=0; i<itsSize; i++)
			if (isNominalType())
				aResult.add(itsNominals.get(i));
			else if (isNumericType())
				aResult.add(Float.toString(itsFloats.get(i)));
			//TODO ordinal?

		return aResult;
	}
}
