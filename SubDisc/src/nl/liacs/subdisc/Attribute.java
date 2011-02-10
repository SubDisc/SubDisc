package nl.liacs.subdisc;

import java.util.*;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

	/**
	 * There is only a limited number of AttributeTypes an
	 * {@link Attribute Attribute} can have. The AttributeType <code>enum</code>
	 * contains them all. The
	 * <code>public final String DEFAULT_MISSING_VALUE</code> gives the default
	 * missing value for that AttributeType.
	 */
	public enum AttributeType
	{
		NOMINAL("?"),
		NUMERIC("0.0"),
		ORDINAL("0.0"),
		BINARY("0");

		// used for FileLoading/Column setMissingValue
		private static final TreeSet<String> BOOLEAN_POSITIVES =
			new TreeSet<String>(
					Arrays.asList(new String[] { "1", "true", "t", "yes" }));
		private static final TreeSet<String> BOOLEAN_NEGATIVES =
			new TreeSet<String>(
					Arrays.asList(new String[] { "0", "false", "f", "no" }));

		/*
		 * NOTE if DEFAULT_MISSING_VALUE is changed for NUMERIC/ORDINAL, check
		 * the switch() code for the Column constructor:
		 * public Column(Attribute theAttribute, int theNrRows)
		 */
		/**
		 * The default missing value for each AttributeType. To set a different
		 * missing value use
		 * {@link Column#setNewMissingValue(String theNewValue) Column.setNewMissingValue()}.
		 */
		public final String DEFAULT_MISSING_VALUE;

		private AttributeType(String theDefaultMissingValue)
		{
			DEFAULT_MISSING_VALUE = theDefaultMissingValue; 
		}

		/**
		 * Returns the AttributeType corresponding to the <code>String</code>
		 * parameter. If the corresponding AttributeType can not be found, the
		 * default AttributeType NOMINAL is returned. This method is case
		 * insensitive.
		 * 
		 * @param theType the <code>String</code> corresponding to an
		 * AtrtibuteType.
		 * 
		 * @return the AttributeType corresponding to the <code>String</code>
		 * parameter, or AttributeType NOMINAL if no corresponding AttributeType
		 * is found.
		 */
		public static AttributeType getAttributeType(String theType)
		{
			for (AttributeType at : AttributeType.values())
				if (at.toString().equalsIgnoreCase(theType))
					return at;

			/*
			 * theType cannot be resolved to an AttibuteType. Log error and
			 * return default.
			 */
			Log.logCommandLine(
				String.format(
						"'%s' is not a valid AttributeType. Returning '%s'.",
						theType,
						AttributeType.getDefaultType()));
			return AttributeType.getDefaultType();
		}

		public static boolean isValidBinaryTrueValue(String theBooleanValue)
		{
			return BOOLEAN_POSITIVES.contains(theBooleanValue.toLowerCase().trim());
		}

		public static boolean isValidBinaryFalseValue(String theBooleanValue)
		{
			return BOOLEAN_NEGATIVES.contains(theBooleanValue.toLowerCase().trim());
		}

		public static AttributeType getDefaultType()
		{
			return AttributeType.NOMINAL;
		}
	}

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
			itsType = AttributeType.getDefaultType();
			constructorErrorLog("type can not be 'null'. Using: ",
								itsType.toString());
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
