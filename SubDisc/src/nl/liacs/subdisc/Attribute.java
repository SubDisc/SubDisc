package nl.liacs.subdisc;

import org.w3c.dom.*;

/**
 * Each {@link Column Column} in a {@link Table Table} is identified by its
 * Attribute. Attributes can be used as PrimaryTarget or SecondaryTarget, but
 * only when the {@link AttributeType AttributeType} of the Attribute is
 * appropriate for the {@link TargetConcept TargetConcept}s
 * {@link nl.liacs.subdisc.TargetConcept.TargetType TargetType}. All Attribute constructors and
 * setters ensure that its AttributeType is never <code>null</code>
 * (<code>NOMINAL</code> by default).
 */
public class Attribute implements XMLNodeInterface
{
	// when adding/removing members be sure to update addNodeTo() and loadNode()
	private AttributeType itsType;
	private String itsName;
	private String itsShort;
	private int itsIndex;

	//TXT, ARFF
	public Attribute(String theName, String theShort, AttributeType theType, int theIndex)
	{
		if (!isValidIndex(theIndex))
			return;
		else
			itsIndex = theIndex;

		checkAndSetName(theName);
		itsShort = theShort;
		checkAndSetType(theType);
	}

	//MRML
	public Attribute(String theName, String theShort, AttributeType theType)
	{
		checkAndSetName(theName);
		itsShort = theShort;
		checkAndSetType(theType);
	}

	/**
	 * Create an Attribute from an XML AttributeNode.
	 * 
	 * @param theAttributeNode
	 */
	public Attribute(Node theAttributeNode)
	{
		if (theAttributeNode == null)
		{
			Log.logCommandLine(
			"Attribute Constructor: parameter can not be 'null'. Nothing set.");
			return;
		}

		NodeList aChildren = theAttributeNode.getChildNodes();
		for (int i = 0, j = aChildren.getLength(); i < j; ++i)
		{
			Node aSetting = aChildren.item(i);
			String aNodeName = aSetting.getNodeName();
			if ("name".equalsIgnoreCase(aNodeName))
			{
				checkAndSetName(aSetting.getTextContent());
			}
			else if ("short".equalsIgnoreCase(aNodeName))
			{
				itsShort = aSetting.getTextContent().isEmpty() ?
											null : aSetting.getTextContent();
			}
			else if ("type".equalsIgnoreCase(aNodeName))
			{
				itsType =
					AttributeType.getAttributeType(aSetting.getTextContent());
			}
			else if ("index".equalsIgnoreCase(aNodeName))
			{
				int tempInt = -1;
				try
				{
					tempInt = Integer.parseInt(aSetting.getTextContent());
				}
				catch (NumberFormatException ex)
				{
					constructorErrorLog("'" + aSetting.getTextContent() + "'",
										" can not be parsed as 'int'");
				}
				// defaults to 0 otherwise
				if (isValidIndex(tempInt))
					itsIndex = tempInt;
			}
		}
	}

	private boolean isValidIndex(int theIndex)
	{
		boolean isValid = (theIndex >= 0);

		if (!isValid)
			Log.logCommandLine(
				"Attribute Constructor: index can not be < 0. Index not set.");

		return isValid;
	}

	private void checkAndSetName(String theName)
	{
		if (theName == null || theName.isEmpty())
		{
			itsName = String.valueOf(System.nanoTime());
			constructorErrorLog("name can not be 'null' or empty. Using: ",
								itsName);
		}
		else
			itsName = theName;
	}

	private void checkAndSetType(AttributeType theType)
	{
		if (theType == null)
		{
			itsType = AttributeType.getDefault();
			constructorErrorLog("type can not be 'null'. Using: ",
								itsType.name());
		}
		else
			itsType = theType;
	}

	private void constructorErrorLog(String theMessage, String theAlternative)
	{
		Log.logCommandLine("Attribute Constructor: " +
							theMessage +
							theAlternative);
	}

	public int getIndex() { return itsIndex; }	// is never set for MRML
	public AttributeType getType() { return itsType; }
	public String getName() { return itsName; }
	public String getShort() { return hasShort() ? itsShort : ""; }
	public boolean hasShort() { return (itsShort != null) ; }
	public String getNameAndShort()
	{
		return itsName + (hasShort() ? " (" + getShort() + ")" : "");
	}
	public String getNameOrShort() { return hasShort() ? itsShort : itsName; }
	public String getTypeName() { return itsType.toString().toLowerCase(); }

	public void print()
	{
		Log.logCommandLine(itsIndex + ":" + getNameAndShort() + " " +
							getTypeName());
	}

	public boolean isNominalType() { return itsType == AttributeType.NOMINAL; }
	public boolean isNumericType() { return itsType == AttributeType.NUMERIC; }
	public boolean isOrdinalType() { return itsType == AttributeType.ORDINAL; }
	public boolean isBinaryType() { return itsType == AttributeType.BINARY; }

	/*
	 * AttributeType can never be 'null', guaranteed by constructor, or become
	 * 'null', guaranteed here.
	 */
	/**
	 * Sets the {@link AttributeType AttributeType} for this Attribute. This is
	 * used for changing the AttributeType of a {@link Column Column}. The
	 * Column is responsible for checking whether its AttributeType can be
	 * changed to this new AttributeType.
	 * 
	 * @param theType the AttibuteType to set as this Attributes' new
	 * AttributeType.
	 * 
	 * @return <code>false</code> if the AttributeType passed in as a parameter
	 * is <code>null</code>, <code>true</code> otherwise.
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

	//called after removing (domain) columns from Table
	void setIndex(int theIndex)
	{
		itsIndex = theIndex;
	}

	/**
	 * Creates an {@link XMLNode XMLNode} representation of this Attribute.
	 * 
	 * @param theParentNode the Node of which this Node will be a ChildNode.
	 * 
	 * @return a Node that contains all the information of this Attribute.
	 */
	@Override
	public void addNodeTo(Node theParentNode)
	{
		Node aNode = XMLNode.addNodeTo(theParentNode, "attribute");
		XMLNode.addNodeTo(aNode, "name", itsName);
		XMLNode.addNodeTo(aNode, "short", (itsShort == null ? "" : itsShort));
		XMLNode.addNodeTo(aNode, "type", itsType);
		XMLNode.addNodeTo(aNode, "index", itsIndex);
	}
}
