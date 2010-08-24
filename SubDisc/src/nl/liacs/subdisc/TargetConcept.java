package nl.liacs.subdisc;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Depending on the TargetType of a TargetConcept, it holds the PrimaryTarget
 * and/or SecondaryTarget(s). The TargetType indicates what type of search
 * setting will be used in the experiment.
 */
public class TargetConcept implements XMLNodeInterface
{

	// when adding/removing members be sure to update addNodeTo() and loadNode()
	// itsMembers
	private int			itsNrTargetAttributes = 1;	// always 1 in current code
	private TargetType	itsTargetType;
	private Attribute	itsPrimaryTarget;
	private String		itsTargetValue;
	private Attribute	itsSecondaryTarget;
	private List<Attribute> itsSecondaryTargets;	// better use empty one by default, for-each/null safe

	public enum TargetType
	{
		SINGLE_NOMINAL("single nominal"),
		SINGLE_NUMERIC("single numeric"),
		SINGLE_ORDINAL("single ordinal"),
		DOUBLE_REGRESSION("double regression"),
		DOUBLE_CORRELATION("double correlation"),
		MULTI_LABEL("multi-label"),
		MULTI_BINARY_CLASSIFICATION("multi binary classification");

		public final String TEXT;

		private TargetType(String theText) { TEXT = theText; }

		public boolean isEMM()
		{
			switch(this)
			{
				case DOUBLE_REGRESSION :
				case DOUBLE_CORRELATION :
				case MULTI_LABEL :
				case MULTI_BINARY_CLASSIFICATION : return true;
				default : return false;
			}
		}

		public boolean isImplemented()
		{
			switch(this)
			{
				case SINGLE_NOMINAL :
				case DOUBLE_REGRESSION :
				case DOUBLE_CORRELATION :
				case MULTI_LABEL : return true;
				default : return false;
			}
		}

		public boolean hasSecondaryTarget()
		{
			switch(this)
			{
				case DOUBLE_REGRESSION :
				case DOUBLE_CORRELATION : return true;
				default : return false;
			}
		}
	}

	public TargetConcept(Node theTargetConceptNode)
	{
		if(theTargetConceptNode == null)
			return;	// TODO throw warning dialog

		NodeList aChildren = theTargetConceptNode.getChildNodes();
		for(int i = 0, j = aChildren.getLength(); i < j; ++i)
		{
			Node aSetting = aChildren.item(i);
			String aNodeName = aSetting.getNodeName();

			if("nr_target_attributes".equalsIgnoreCase(aNodeName))
				itsNrTargetAttributes = Integer.parseInt(aSetting.getTextContent());
			else if("primary_target".equalsIgnoreCase(aNodeName))
				itsPrimaryTarget = new Attribute(aSetting.getTextContent(), null, null);	// TODO
			else if("target_value".equalsIgnoreCase(aNodeName))
				itsTargetValue = aSetting.getTextContent();
			else if("secondary_target".equalsIgnoreCase(aNodeName))
				itsSecondaryTarget = new Attribute(aSetting.getTextContent(), null, null);	// TODO
			else if("secondary_targets".equalsIgnoreCase(aNodeName))
			{
				itsSecondaryTargets = new ArrayList<Attribute>();
				for(String s : aSetting.getTextContent().split(",", -1))
					itsSecondaryTargets.add(new Attribute(s, null, null));
			}
			else
				;	// TODO throw warning dialog
		}
	}

	public TargetConcept() {} // TODO for now

	// member methods
	public int getNrTargetAttributes() { return itsNrTargetAttributes; }
	public void setNrTargetAttributes(int theNr) { itsNrTargetAttributes = theNr; }
	public TargetType getTargetType() { return itsTargetType; }
	public void setTargetType(String theTargetType)
	{
		for(TargetType t : TargetType.values())
		{
			if(t.TEXT.equalsIgnoreCase(theTargetType))
			{
				itsTargetType = t;
				return;
			}
		}
	}

	public Attribute getPrimaryTarget() { return itsPrimaryTarget; }
	public void setPrimaryTarget(Attribute thePrimaryTarget) { itsPrimaryTarget = thePrimaryTarget; }
	public String getTargetValue() { return itsTargetValue; }
	public void setTargetValue(String theTargetValue) { itsTargetValue = theTargetValue; }

	public Attribute getSecondaryTarget() { return itsSecondaryTarget; }
	public void setSecondaryTarget(Attribute theSecondaryTarget) { itsSecondaryTarget = theSecondaryTarget; }

	public boolean isSingleNominal() { return (itsTargetType == TargetType.SINGLE_NOMINAL); }
//	public boolean isEMM() { return itsTargetType.isEMM(); }
//	public boolean isImplemented() { return itsTargetType.isImplemented(); }

	/**
	 * Create a XML Node representation of this TaretConcept.
	 * @param theParentNode, the Node of which this Node will be a ChildNode.
	 * @return A Node that contains all the information of this TargetConcept.
	 */
	@Override
	public void addNodeTo(Node theParentNode)
	{
		Node aNode = XMLNode.addNodeTo(theParentNode, "target_concept");
		XMLNode.addNodeTo(aNode, "nr_target_attributes", itsNrTargetAttributes);
		XMLNode.addNodeTo(aNode, "target_type", itsTargetType.TEXT);
		XMLNode.addNodeTo(aNode, "primary_target", itsPrimaryTarget.getName());
		XMLNode.addNodeTo(aNode, "target_value", itsTargetValue);
		if(itsSecondaryTarget == null)
			XMLNode.addNodeTo(aNode, "secondary_target");
		else
			XMLNode.addNodeTo(aNode, "secondary_target", itsSecondaryTarget.getName());
		if(itsSecondaryTargets == null || itsSecondaryTargets.size() == 0)
			XMLNode.addNodeTo(aNode, "secondary_targets");
		else
		{
			StringBuilder sb = new StringBuilder(itsSecondaryTargets.size() * 10);
			for(Attribute a : itsSecondaryTargets)
				sb.append(a.getName() + ",");
			sb.deleteCharAt(sb.length() - 1);	// removes last comma
			XMLNode.addNodeTo(aNode, "secondary_targets", sb);
		}
	}
/*
	public static boolean isImplemented(int theTargetType )
	{
		return (theTargetType == SINGLE_NOMINAL ||
		theTargetType == DOUBLE_REGRESSION ||
		theTargetType == DOUBLE_CORRELATION ||
		theTargetType == MULTI_LABEL);
	}
	
	public static int getFirstTargetType()	{ return SINGLE_NOMINAL; }
	public static int getLastTargetType()	{ return MULTI_BINARY_CLASSIFICATION; }

	public static String getTypeString(int theType)
	{
		String aType = new String();
		switch(theType)
		{
			//NOMINAL
			case SINGLE_NOMINAL				: { aType = "single nominal"; break; }
			case SINGLE_NUMERIC 			: { aType = "single numeric"; break; }
			case SINGLE_ORDINAL				: { aType = "single ordinal"; break; }
			case DOUBLE_REGRESSION			: { aType = "double regression"; break; }
			case DOUBLE_CORRELATION			: { aType = "double correlation"; break; }
			case MULTI_LABEL				: { aType = "multi-label"; break; }
			case MULTI_BINARY_CLASSIFICATION: { aType = "multi binary classification"; break; }
		}
		return aType;
	}

	public static int getTypeCode(String theType)
	{
		String aType = theType.toLowerCase().trim();
		//NOMINAL
		if ("single nominal".equals(aType)) 					return SINGLE_NOMINAL;
		else if ("single numeric".equals(aType))				return SINGLE_NUMERIC;
		else if ("single ordinal".equals(aType)) 				return SINGLE_ORDINAL;
		else if ("double regression".equals(aType)) 			return DOUBLE_REGRESSION;
		else if ("double correlation".equals(aType)) 			return DOUBLE_CORRELATION;
		else if ("multi-label".equals(aType)) 					return MULTI_LABEL;
		else if ("multi binary classification".equals(aType)) 	return MULTI_BINARY_CLASSIFICATION;
		//default
		return SINGLE_NOMINAL;
	}

	public static boolean isEMM(String theType)
	{
		int aType = getTypeCode(theType);
		return (aType == DOUBLE_REGRESSION ||
			aType == DOUBLE_CORRELATION ||
			aType == MULTI_LABEL ||
			aType == MULTI_BINARY_CLASSIFICATION);
	}
*/
	
	
}
