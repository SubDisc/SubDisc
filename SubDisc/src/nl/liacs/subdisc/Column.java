package nl.liacs.subdisc;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.TreeSet;

import nl.liacs.subdisc.Attribute.AttributeType;

public class Column
{
//	private AttributeType itsType; //types in Attribute
	private Attribute itsAttribute;
	private ArrayList<Float> itsFloats;
	private ArrayList<String> itsNominals;
	private BitSet itsBinaries;
	private int itsSize;
	private float itsMin = Float.POSITIVE_INFINITY;
	private float itsMax = Float.NEGATIVE_INFINITY;
	private boolean isEnabled = true;

	public Column(Attribute theAttribute, int theNrRows)
	{
		itsSize = 0;
		itsAttribute = theAttribute;
		switch(itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL : itsFloats = new ArrayList<Float>(theNrRows); break;
			case NOMINAL : itsNominals = new ArrayList<String>(theNrRows); break;
			case BINARY : itsBinaries = new BitSet(theNrRows); break;
			default : itsNominals = new ArrayList<String>(theNrRows); break;	// TODO throw warning
		}
	}

	public void add(float theFloat) { itsFloats.add(new Float(theFloat)); itsSize++; }
	public void add(boolean theBinary)
	{
		if(theBinary)
			itsBinaries.set(itsSize);
		itsSize++;
	}
	public void add(String theNominal) { itsNominals.add(theNominal); itsSize++; }
	public int size() { return itsSize; }
	public Attribute getAttribute() { return itsAttribute; }
	public AttributeType getType() { return itsAttribute.getType(); }
	public String getName() {return itsAttribute.getName(); }
	public float getFloat(int theIndex) { return itsFloats.get(theIndex).floatValue(); }
	public String getNominal(int theIndex) { return itsNominals.get(theIndex); }
	public boolean getBinary(int theIndex) { return itsBinaries.get(theIndex); }
	public String getString(int theIndex)
	{
		switch (itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL : return itsFloats.get(theIndex).toString();
			case NOMINAL : return getNominal(theIndex);
			case BINARY : return getBinary(theIndex)?"1":"0";
			default : return ("Unknown type: " + itsAttribute.getTypeName());
		}
	}
	public BitSet getBinaries() { return itsBinaries; }

	public boolean isNominalType() { return itsAttribute.isNominalType(); }
	public boolean isNumericType() { return itsAttribute.isNumericType(); }
	public boolean isOrdinalType() { return itsAttribute.isOrdinalType(); }
	public boolean isBinaryType() { return itsAttribute.isBinaryType(); }

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
		if(itsMax == Float.NEGATIVE_INFINITY) //never computed?
			for(int i=0; i<itsSize; i++)
			{
				float aValue = getFloat(i);
				if(aValue > itsMax)
					itsMax = aValue;
				if(aValue < itsMin)
					itsMin = aValue;
			}
	}

	public void print()
	{
		Log.logCommandLine((isEnabled ? "Enabled" : "Disbled") + " Column " + this.getName() + ":"); // TODO TEST ONLY
		switch(itsAttribute.getType())
		{
			case NUMERIC :
			case ORDINAL : Log.logCommandLine(itsFloats.toString()); break;
			case NOMINAL : Log.logCommandLine(itsNominals.toString()); break;
			case BINARY : Log.logCommandLine(itsBinaries.toString()); break;
			default : Log.logCommandLine("Unknown type: " + itsAttribute.getTypeName()); break;
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

	/**
	 * NEW Methods for AttributeType change
	 * TODO update
	 * itsType
	 * (itsTable.)itsAttribute.setType()
	 * itsFloats / itsNominals / itsBinaries
	 * @return 
	 */
	public void setType(String theType) { itsAttribute.setType(theType); }
	public boolean getIsEnabled() { return isEnabled; }
	public void setIsEnabled(boolean theSetting) { isEnabled = theSetting; }
}
