/**
 * @author marvin
 * This is a convenience class for XML Node creation. It is uninstantiable and
 * contains only static methods. Therefore it does not implement the
 * XMLNodeInterface.
 */
package nl.liacs.subdisc;

import org.w3c.dom.Node;

public class XMLNode
{
	private XMLNode() {}

	/**
	 * Create and add a Node to theParentNode. The element name is always
	 * converted to lowercase.
	 * @param theParentNode, the Node to which to add the new Node
	 * @param theElementName, the name of the new Node
	 * @return the newly created Node
	 */
	public static Node addNodeTo(Node theParentNode, String theElementName)
	{
		return theParentNode.appendChild(theParentNode
											.getOwnerDocument()
											.createElement(theElementName.toLowerCase()));
	}

	/**
	 * Create and add a Node to theParentNode. The element name is always
	 * converted to lowercase, and for any Object passed in its toString()
	 * method is used as input string for the setTextContent() method for the
	 * new Node. This works fine for build in Java types (eg. Float and Double),
	 * but may cause trouble for self defined Objects. In that case override the
	 * toString() method for the Object, or pass in a String.
	 * @param theParentNode, the Node to which to add the new Node
	 * @param theElementName, the name of the new Node
	 * @param theTextContent, the text content for the new Node
	 */
	public static void addNodeTo(Node theParentNode, String theElementName, Object theTextContent)
	{
		theParentNode.appendChild(theParentNode
									.getOwnerDocument()
									.createElement(theElementName.toLowerCase()))
									.setTextContent(theTextContent.toString());
	}
}
