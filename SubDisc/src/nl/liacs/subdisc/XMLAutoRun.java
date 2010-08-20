package nl.liacs.subdisc;

import java.io.File;

import nl.liacs.subdisc.FileHandler.Action;
import nl.liacs.subdisc.XMLDocument.XMLType;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

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

		itsDocument.getLastChild().appendChild(buildExperimentElement(theSearchParameters, theTable));
		XMLDocument.saveDocument(itsDocument, theFile);
	}

	private Node buildExperimentElement(SearchParameters theSearchParameters, Table theTable)
	{
		Node anExperimentNode = itsDocument.createElement("experiment");

		for(XMLNode x : XMLNode.values())
			x.createNode(anExperimentNode, theSearchParameters, theTable);

		return anExperimentNode;
	}
/*
	public String[] getAllSearchParameters()
	{
		String[] aSearchParameterArray = new String[28];
		TargetConcept aTargetConcept = getTargetConcept();

		aSearchParameterArray[0] = "TargetConcept: ";
		aSearchParameterArray[1] = String.valueOf(aTargetConcept.getNrTargetAttributes());
		aSearchParameterArray[2] = aTargetConcept.getTargetType().name();

		Attribute aPrimaryTarget = aTargetConcept.getPrimaryTarget();
		aSearchParameterArray[3] = String.valueOf(aPrimaryTarget.getIndex());
		aSearchParameterArray[4] = aPrimaryTarget.getName();
		aSearchParameterArray[5] = aPrimaryTarget.getShort();
		aSearchParameterArray[6] = aPrimaryTarget.getType().name();

		aSearchParameterArray[7] = aTargetConcept.getTargetValue();

		Attribute aSecondaryTarget = aTargetConcept.getSecondaryTarget();	// TODO
//		aSearchParameterArray[8] = String.valueOf(aSecondaryTarget.getIndex());
//		aSearchParameterArray[9] = aSecondaryTarget.getName();
//		aSearchParameterArray[10] = aSecondaryTarget.getShort();
//		aSearchParameterArray[11] = aSecondaryTarget.getType().name();

//		aTargetConcept.getSecondaryTargets(); 
		
		aSearchParameterArray[13] = getQualityMeasureString();
		aSearchParameterArray[14] = String.valueOf(getQualityMeasureMinimum());

		aSearchParameterArray[15] = String.valueOf(getSearchDepth());
		aSearchParameterArray[16] = String.valueOf(getMinimumCoverage());
		aSearchParameterArray[17] = String.valueOf(getMaximumCoverage());
		aSearchParameterArray[18] = String.valueOf(getMaximumSubgroups());
		aSearchParameterArray[19] = String.valueOf(getMaximumTime());

		aSearchParameterArray[20] = getSearchStrategyName(getSearchStrategy());
		aSearchParameterArray[21] = String.valueOf(getSearchStrategyWidth());
		aSearchParameterArray[22] = getNumericStrategy().name();

		aSearchParameterArray[23] = String.valueOf(getNrSplitPoints());
		aSearchParameterArray[24] = String.valueOf(getAlpha());
		aSearchParameterArray[25] = String.valueOf(getBeta());
		aSearchParameterArray[26] = String.valueOf(getPostProcessingCount());
		aSearchParameterArray[27] = String.valueOf(getMaximumPostProcessingSubgroups());

		for(String s : aSearchParameterArray)
			System.out.println(s);
		return aSearchParameterArray;
	}
*/

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

		private Node createTargetConceptNode(Node theExperimentNode, TargetConcept theTargetConcept)
		{
			Document d = theExperimentNode.getOwnerDocument();
			Node aTargetConceptNode = theExperimentNode.appendChild(d.createElement("target_concept"));

			for(XMLNodeTargetConcept x : XMLNodeTargetConcept.values())
				aTargetConceptNode.appendChild(d.createElement(x.toString().toLowerCase()))
												.setTextContent(x.getValueFromData(theTargetConcept));

			return aTargetConceptNode;
		}

		private Node createSearchParametersNode(Node theExperimentNode, SearchParameters theSearchParameters)
		{
			Document d = theExperimentNode.getOwnerDocument();
			Node aSearchParametersNode = theExperimentNode.appendChild(d.createElement("search_parameters"));

			for(XMLNodeSearchParameter s : XMLNodeSearchParameter.values())
				aSearchParametersNode.appendChild(d.createElement(s.toString().toLowerCase()))
													.setTextContent(s.getValueFromData(theSearchParameters));

			return aSearchParametersNode;
		}

		private Node createTableNode(Node theExperimentNode, Table theTable)
		{
			Document d = theExperimentNode.getOwnerDocument();
			Node aTableNode = theExperimentNode.appendChild(d.createElement("table"));

			// create a createXML method in Table/Column

			return aTableNode;
		}
	}	
}
