package nl.liacs.subdisc;

import org.w3c.dom.Node;

/**
 * Each Column in a Table is identified by its Attribute. Attributes can be used
 * as PrimaryTarget or SecondaryTarget, but only when the AttributeType of the
 * Attribute is appropriate for the TargetConcepts' TargetType.
 */
public class Attribute implements XMLNodeInterface
{
	// when adding/removing members be sure to update addNodeTo() and loadNode()
	private int itsIndex;
	private AttributeType itsType;
	private String itsName;
	private String itsShort;

	public enum AttributeType { NOMINAL, NUMERIC, ORDINAL, BINARY; }

	public Attribute(String theName, String theShort, AttributeType theType, int theIndex)
	{
		itsName = (theName == null ? String.valueOf(System.nanoTime()) : theName);	// TODO throw warning
		itsShort = (theShort == null ? "" :theShort);
		itsType = (theType == null ? AttributeType.NOMINAL :theType);	// TODO this is a quick hack for now
		itsIndex = theIndex;
	}

	//MRML
	public Attribute(String theName, String theShort, AttributeType theType)
	{
		itsName = (theName == null ? String.valueOf(System.nanoTime()) : theShort);	// TODO throw warning
		itsShort = (theShort == null ? "" :theShort);
		itsType = (theType == null ? AttributeType.NOMINAL :theType);	// TODO this is a quick hack for now
	}

	public int getIndex() { return itsIndex; }	// TODO check, is null for ARFF/MRML
	public AttributeType getType() { return itsType; }
	public String getName() { return itsName; }
	public String getShort() { return itsShort; }
	public boolean hasShort() { return (!itsShort.isEmpty()); }
	public String getNameAndShort() { return itsName + (hasShort() ? " (" + getShort() + ")" : ""); }
	public String getNameOrShort() { return hasShort() ? itsShort : itsName; }
	public String getTypeName() { return itsType.name().toLowerCase(); }

	public void print() { Log.logCommandLine(itsIndex + ":" + getNameAndShort() + " " + getTypeName()); }

	public boolean isNominalType() { return itsType == AttributeType.NOMINAL; }
	public boolean isNumericType() { return itsType == AttributeType.NUMERIC; }
	public boolean isOrdinalType() { return itsType == AttributeType.ORDINAL; }
	public boolean isBinaryType() { return itsType == AttributeType.BINARY; }

	/**
	 * NEW Methods for AttributeType change
	 * Needs more data/type checking
	 */
	public void setType(String theType)
	{
		for(AttributeType at : AttributeType.values())
		{
			if(at.toString().equalsIgnoreCase(theType))
			{
				itsType = at;
				break;
			}
		}
	}

	/**
	 * Create a XML Node representation of this Attribute.
	 * @param theParentNode, the Node of which this Node will be a ChildNode.
	 * @return A Node that contains all the information of this Attribute.
	 */
	@Override
	public void addNodeTo(Node theParentNode)
	{
		Node aNode = XMLNode.addNodeTo(theParentNode, "attribute");
		XMLNode.addNodeTo(aNode, "name", itsName);
		XMLNode.addNodeTo(aNode, "short", itsShort);
		XMLNode.addNodeTo(aNode, "type", itsType);
		XMLNode.addNodeTo(aNode, "index", itsIndex);
	}
}
