package nl.liacs.subdisc;

import java.util.*;

import org.w3c.dom.*;

/**
 * Depending on the {@link TargetType TargetType} of a TargetConcept, it holds
 * the <code>PrimaryTarget</code> and/or <code>SecondaryTarget</code>/<code>
 * MultiTargets</code>. The TargetType indicates what type of search setting
 * will be used in the experiment. All TargetConcept constructors and setters
 * ensure that its TargetType is never <code>null</code> (<code>SINGLE_NOMINAL
 * </code> by default).
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
	private List<Attribute> itsMultiTargets;

	public TargetConcept()
	{
		itsTargetType = TargetType.getDefault();
	}

	// creation of TargetConcept relies on Table being loaded first
	public TargetConcept(Node theTargetConceptNode, Table theTable)
	{
		if (theTargetConceptNode == null)
			return;	// TODO throw warning dialog

		NodeList aChildren = theTargetConceptNode.getChildNodes();
		for (int i = 0, j = aChildren.getLength(); i < j; ++i)
		{
			Node aSetting = aChildren.item(i);
			String aNodeName = aSetting.getNodeName();
			if ("nr_target_attributes".equalsIgnoreCase(aNodeName))
				itsNrTargetAttributes = Integer.parseInt(aSetting.getTextContent());
			if ("target_type".equalsIgnoreCase(aNodeName))
				itsTargetType = (TargetType.getTargetType(aSetting.getTextContent()));
			else if ("primary_target".equalsIgnoreCase(aNodeName))
				itsPrimaryTarget = theTable.getAttribute(aSetting.getTextContent());
			else if ("target_value".equalsIgnoreCase(aNodeName))
				itsTargetValue = aSetting.getTextContent();
			else if ("secondary_target".equalsIgnoreCase(aNodeName))
				itsSecondaryTarget = theTable.getAttribute(aSetting.getTextContent());
			else if ("multi_targets".equalsIgnoreCase(aNodeName))
			{
				if (!aSetting.getTextContent().isEmpty())
				{
					itsMultiTargets = new ArrayList<Attribute>();
					for (String s : aSetting.getTextContent().split(",", -1))
						itsMultiTargets.add(theTable.getAttribute(s));
				}
			}
			else
				;	// TODO throw warning dialog
		}
	}

	// member methods
	public int getNrTargetAttributes() { return itsNrTargetAttributes; }
	public void setNrTargetAttributes(int theNr) { itsNrTargetAttributes = theNr; }
	public TargetType getTargetType() { return itsTargetType; }
	public void setTargetType(String theTargetTypeName)
	{
			itsTargetType = TargetType.getTargetType(theTargetTypeName);
	}

	public Attribute getPrimaryTarget() { return itsPrimaryTarget; }
	public void setPrimaryTarget(Attribute thePrimaryTarget) { itsPrimaryTarget = thePrimaryTarget; }
	public String getTargetValue() { return itsTargetValue; }
	public void setTargetValue(String theTargetValue) { itsTargetValue = theTargetValue; }

	public Attribute getSecondaryTarget() { return itsSecondaryTarget; }
	public void setSecondaryTarget(Attribute theSecondaryTarget) { itsSecondaryTarget = theSecondaryTarget; }

	public List<Attribute> getMultiTargets() { return itsMultiTargets; }
	public void setMultiTargets(ArrayList<Attribute> theMultiTargets)
	{
		itsMultiTargets = theMultiTargets;
	}

	public boolean isSingleNominal() { return (itsTargetType == TargetType.SINGLE_NOMINAL); }

	public boolean isTargetAttribute(Attribute theAttribute)
	{
		switch (itsTargetType)
		{
			case SINGLE_NOMINAL :
			case SINGLE_NUMERIC :
				return (theAttribute == itsPrimaryTarget);
			case DOUBLE_REGRESSION :
			case DOUBLE_CORRELATION :
				return (theAttribute == itsPrimaryTarget || theAttribute == itsSecondaryTarget);
			case MULTI_LABEL :
				return (itsMultiTargets.contains(theAttribute));
			default :
				return false;
		}
	}

	/**
	 * Creates an {@link XMLNode XMLNode} representation of this TargetConcept.
	 * @param theParentNode the Node of which this Node will be a ChildNode
//	 * @return a Node that contains all the information of this TargetConcept
	 */
	@Override
	public void addNodeTo(Node theParentNode)
	{
		Node aNode = XMLNode.addNodeTo(theParentNode, "target_concept");
		XMLNode.addNodeTo(aNode, "nr_target_attributes", itsNrTargetAttributes);
		XMLNode.addNodeTo(aNode, "target_type", itsTargetType.GUI_TEXT);

		if (itsPrimaryTarget == null)
			XMLNode.addNodeTo(aNode, "primary_target");
		else
			XMLNode.addNodeTo(aNode, "primary_target", itsPrimaryTarget.getName());

		XMLNode.addNodeTo(aNode, "target_value", itsTargetValue);

		if (itsSecondaryTarget == null)
			XMLNode.addNodeTo(aNode, "secondary_target");
		else
			XMLNode.addNodeTo(aNode, "secondary_target", itsSecondaryTarget.getName());

		if (itsMultiTargets == null || itsMultiTargets.size() == 0)
			XMLNode.addNodeTo(aNode, "multi_targets");
		else
		{
			StringBuilder sb = new StringBuilder(itsMultiTargets.size() * 10);
			for (Attribute a : itsMultiTargets)
				sb.append(a.getName() + ",");
//			sb.deleteCharAt(sb.length() - 1);	// removes last comma
			XMLNode.addNodeTo(aNode, "multi_targets", sb.substring(0, sb.length() - 1));
		}
	}
}
