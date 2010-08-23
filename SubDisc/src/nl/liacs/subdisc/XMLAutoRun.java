package nl.liacs.subdisc;

import java.io.File;

import nl.liacs.subdisc.FileHandler.Action;
import nl.liacs.subdisc.XMLDocument.XMLType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLAutoRun
{
	private Document itsDocument;

	public enum AutoRun { CREATE, ADD }

	public XMLAutoRun(SearchParameters theSearchParameters, Table theTable, AutoRun theFileOption)
	{
		if(theSearchParameters == null || theTable == null)
			return;

		File aFile = new FileHandler(Action.SAVE).getFile();

		if(aFile == null)
			return;

		buildDocument(aFile, theSearchParameters, theTable, theFileOption);
	}

	/**
	 * TODO move to FileLoaderXML
	 * Load AutoRun file and create the SearchParameters and Table it describes.
	 * @param theFile
	 * @return true iif the file is parsed successfully and all data is set.
	 */
	public static boolean loadAutoRunFile(File theFile)
	{
		boolean succes = false;
		return succes;
	}

	private void buildDocument(File theFile, SearchParameters theSearchParameters, Table theTable, AutoRun theFileOption)
	{
		// TODO from  here create new method, should not be in constructor
		if(theFileOption == AutoRun.CREATE)
			itsDocument = XMLDocument.buildDocument(XMLType.AUTORUN);
		else
			itsDocument = XMLDocument.parseXMLFile(theFile);

		if(itsDocument == null)
			return;	// TODO error message in XMLDocument

		Node autorun = itsDocument.getLastChild();
		autorun.appendChild(buildExperimentElement(theSearchParameters, theTable));

		// do this only after trying to insert new node
		((Element) autorun).setAttribute("nr_experiments",
											String.valueOf(autorun.getChildNodes().getLength()));

		XMLDocument.saveDocument(itsDocument, theFile);
	}

	private Node buildExperimentElement(SearchParameters theSearchParameters, Table theTable)
	{
		// reset all experiment ids, avoids trouble when the autorun.xml file is
		// edited by hand and experiments are added/removed.
		NodeList aNodeList = itsDocument.getLastChild().getChildNodes();
		int aLength = aNodeList.getLength();
		for(int i = 0; i < aLength; ++i)
			((Element)aNodeList.item(i)).setAttribute("id", String.valueOf(i));

		Node anExperimentNode = itsDocument.createElement("experiment");
		((Element) anExperimentNode).setAttribute("id", String.valueOf(aLength));
//		((Element) anExperimentNode).setIdAttribute("id", true);

		for(XMLNode x : XMLNode.values())
			x.createNode(anExperimentNode, theSearchParameters, theTable);

		return anExperimentNode;
	}

	/**
	 * TODO for PrimaryTarget/SecodaryTarget(s) the index, name, short and type
	 * can be taken from the table, no need to include them as Nodes.
	 * TODO Make a Node for MRML?
	 * TODO Make a Node for TARGET_CONCEPT
	 * TODO Make a Node for SEARCH_PARAMETERS
	 * TODO Make a Node for the whole table. --- 
	 * 
	 * XMLNode lists all items that will go into the autorun.xml. It contains;
	 * 1. all searchParameters
	 * 2. an XML version of the original Table
	 */
	private enum XMLNode
	{
		TARGET_CONCEPT, SEARCH_PARAMETERS, TABLE;

		public void createNode(Node theExperimentNode, SearchParameters theSearchParameters, Table theTable)
		{
			if(this == TARGET_CONCEPT)
				createTargetConceptNode(theExperimentNode, theSearchParameters.getTargetConcept());
			else if(this == SEARCH_PARAMETERS)
				createSearchParametersNode(theExperimentNode, theSearchParameters);
			else if(this == TABLE)
				createTableNode(theExperimentNode, theTable);
/*
			else
				theExperimentNode.appendChild(theExperimentNode
												.getOwnerDocument()
												.createElement(toString().toLowerCase()))
												.setTextContent(getValueFromData(theSearchParameters, theTable));
*/
		}

		private void createTargetConceptNode(Node theExperimentNode, TargetConcept theTargetConcept)
		{
			theTargetConcept.addNodeTo(theExperimentNode);
		}

		private void createSearchParametersNode(Node theExperimentNode, SearchParameters theSearchParameters)
		{
			theSearchParameters.addNodeTo(theExperimentNode);
		}

		private Node createTableNode(Node theExperimentNode, Table theTable)
		{
			Node aTableNode = theExperimentNode.appendChild(theExperimentNode
															.getOwnerDocument()
															.createElement("table"));

			for(Column c : theTable.getColumns())
				c.addNodeTo(aTableNode);
			// create a createXML method in Table/Column

			return aTableNode;
		}
	}	
}
