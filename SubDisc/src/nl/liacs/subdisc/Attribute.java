package nl.liacs.subdisc;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Each {@link Column Column} in a {@link Table Table} is identified by its
 * Attribute. Attributes can be used as PrimaryTarget or SecondaryTarget, but
 * only when the {@link AttributeType AttributeType} of the Attribute is
 * appropriate for the {@link TargetConcept TargetConcept}s
 * {@link TargetType TargetType}. All Attribute constructors and setters ensure
 * that its AttributeType is never null (NOMINAL by default).
 */
public class Attribute implements XMLNodeInterface
{
	// when adding/removing members be sure to update addNodeTo() and loadNode()
	private AttributeType itsType;
	private String itsName;
	private String itsShort;
	private int itsIndex;

	public enum AttributeType
	{
		NOMINAL,
		NUMERIC,
		ORDINAL,
		BINARY;

		public static AttributeType getAttributeType(String theType)
		{
			for (AttributeType at : AttributeType.values())
				if (at.toString().equals(theType))
					return at;

			/*
			 * theType cannot be resolved to an AttibuteType. Log error and
			 * return default.
			 */
			Log.logCommandLine(String.format("'%s' is not a valid AttributeType. Returning NOMINAL.", theType));
			return AttributeType.NOMINAL;
		}
	}

	public Attribute(String theName, String theShort, AttributeType theType, int theIndex)
	{
		itsName = (theName == null ? String.valueOf(System.nanoTime()) : theName);	// TODO throw warning
		itsShort = (theShort == null ? "" : theShort);
		itsType = (theType == null ? AttributeType.NOMINAL : theType);
		itsIndex = theIndex;	// TODO should not be < 0
	}

	//MRML
	public Attribute(String theName, String theShort, AttributeType theType)
	{
		itsName = (theName == null ? String.valueOf(System.nanoTime()) : theShort);	// TODO throw warning
		itsShort = (theShort == null ? "" : theShort);
		itsType = (theType == null ? AttributeType.NOMINAL : theType);
	}

	/**
	 * Create an Attribute from an XML AttributeNode.
	 */
	public Attribute(Node theAttributeNode)
	{
		if (theAttributeNode == null)
			return;	// TODO throw warning dialog

		NodeList aChildren = theAttributeNode.getChildNodes();
		for (int i = 0, j = aChildren.getLength(); i < j; ++i)
		{
			Node aSetting = aChildren.item(i);
			String aNodeName = aSetting.getNodeName();
			if ("name".equalsIgnoreCase(aNodeName))
				itsName = aSetting.getTextContent();
			else if ("short".equalsIgnoreCase(aNodeName))
				itsShort = aSetting.getTextContent();
			else if ("type".equalsIgnoreCase(aNodeName))
				itsType = setType(aSetting.getTextContent());
			else if ("index".equalsIgnoreCase(aNodeName))
				itsIndex = Integer.parseInt(aSetting.getTextContent());
		}
	}

	public int getIndex() { return itsIndex; }	// TODO check, is null for ARFF/MRML
	public AttributeType getType() { return itsType; }
	public String getName() { return itsName; }
	public String getShort() { return itsShort; }
	public boolean hasShort() { return (!itsShort.isEmpty()); }
	public String getNameAndShort() { return itsName + (hasShort() ? " (" + getShort() + ")" : ""); }
	public String getNameOrShort() { return hasShort() ? itsShort : itsName; }
	public String getTypeName() { return itsType.toString().toLowerCase(); }

	public void print() { Log.logCommandLine(itsIndex + ":" + getNameAndShort() + " " + getTypeName()); }

	public boolean isNominalType() { return itsType == AttributeType.NOMINAL; }
	public boolean isNumericType() { return itsType == AttributeType.NUMERIC; }
	public boolean isOrdinalType() { return itsType == AttributeType.ORDINAL; }
	public boolean isBinaryType() { return itsType == AttributeType.BINARY; }

	/*
	 * TODO this method should be made obsolete.
	 */
	/**
	 * Sets the {@link AttributeType AttributeType} for this Attribute. This is
	 * used for changing the AttributeType of a {@link Column Column}. The
	 * Column is responsible for checking whether its AttributeType can be
	 * changed to this new AttributeType.
	 * 
	 * @return The new AttributeType, or the default AttributeType.NOMINAL if
	 * the String passed in as a parameter cannot be resolved to a valid
	 * AttributeType.
	 */
	public AttributeType setType(String theType)
	{
		for (AttributeType at : AttributeType.values())
		{
			if (at.toString().equals(theType))
			{
				itsType = at;
				break;
			}
		}
		return (itsType == null ? AttributeType.NOMINAL : itsType);
	}

	// TODO this method should replace setType(String theType), and ensure an
	// AttributeType is always set (if itsType == null > AttributeType.NOMINAL).
	/**
	 * Sets the {@link AttributeType AttributeType} for this Attribute. This is
	 * used for changing the AttributeType of a {@link Column Column}. The
	 * Column is responsible for checking whether its AttributeType can be
	 * changed to this new AttributeType.
	 * 
	 * @return False if the AttributeType passed in as a parameter is null, true
	 * otherwise.
	 */
	public boolean setType(AttributeType theType)
	{
		if (theType != null)
		{
			itsType = theType;
			return true;
		}
		else
			return false;
	}

	/**
	 * Create a XML Node representation of this Attribute.
	 * @param theParentNode the Node of which this Node will be a ChildNode
	 * @return A Node that contains all the information of this Attribute
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
