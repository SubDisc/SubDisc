package nl.liacs.subdisc;

import java.io.*;
import java.util.*;

import javax.swing.*;

import nl.liacs.subdisc.FileHandler.Action;
import nl.liacs.subdisc.XMLDocument.*;
import nl.liacs.subdisc.gui.*;

import org.w3c.dom.*;

public class XMLAutoRun
{
	private Document itsDocument;

	public enum AutoRun { CREATE, ADD }

	public XMLAutoRun(SearchParameters theSearchParameters, Table theTable, AutoRun theFileOption)
	{
		if (theSearchParameters == null || theTable == null)
			return;
		else
		{
			File aFile = new FileHandler(Action.SAVE).getFile();

			if (aFile == null)
				return;
			else
				buildDocument(aFile, theSearchParameters, theTable, theFileOption);
		}

	}

	private void buildDocument(File theFile, SearchParameters theSearchParameters, Table theTable, AutoRun theFileOption)
	{
		// TODO from  here create new method, should not be in constructor
		if (theFileOption == AutoRun.CREATE)
			itsDocument = XMLDocument.buildDocument(XMLType.AUTORUN);
		else
			itsDocument = XMLDocument.parseXMLFile(theFile);

		if (itsDocument == null)
			return;	// TODO error message in XMLDocument

		Node anAutorunNode = itsDocument.getLastChild();
		anAutorunNode
			.appendChild(buildExperimentElement(theSearchParameters, theTable));

		// do this only after trying to insert new node
		((Element) anAutorunNode).setAttribute("nr_experiments",
												String.valueOf(anAutorunNode
																.getChildNodes()
																.getLength()));

		XMLDocument.saveDocument(itsDocument, theFile);
	}

	private Node buildExperimentElement(SearchParameters theSearchParameters, Table theTable)
	{
		// reset all experiment ids, avoids trouble when the autorun.xml file is
		// edited by hand and experiments are added/removed.
		NodeList aNodeList = itsDocument.getLastChild().getChildNodes();
		int aLength = aNodeList.getLength();
		for (int i = 0; i < aLength; i++)
			((Element) aNodeList.item(i)).setAttribute("id", String.valueOf(i));

		Node anExperimentNode = itsDocument.createElement("experiment");
		((Element) anExperimentNode).setAttribute("id",
													String.valueOf(aLength));
//		((Element) anExperimentNode).setIdAttribute("id", true);

		for (XMLNode aNode : XMLNode.values())
			aNode.createNode(anExperimentNode, theSearchParameters, theTable);

		return anExperimentNode;
	}

	/*
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
			if (this == TARGET_CONCEPT)
				createTargetConceptNode(theExperimentNode,
										theSearchParameters.getTargetConcept());
			else if (this == SEARCH_PARAMETERS)
				createSearchParametersNode(theExperimentNode,
											theSearchParameters);
			else if (this == TABLE)
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

		private void createTableNode(Node theExperimentNode, Table theTable)
		{
			theTable.addNodeTo(theExperimentNode);
		}
	}

	/**
	 * Loads an <code>AutoRun File</code> and runs a SubgroupDiscovery
	 * based on the information contained in the file.
	 * 
	 * @param the <code>String[]</code> containing the command-line
	 * parameters.
	 * 
	 * @return <code>true</code> if the parameters are valid,
	 * <code>false</code> otherwise.
	 */
	public static boolean autoRunSetting(String[] args)
	{
		File aFile = null;
		boolean showWindows = false;

		if (args.length == 1 || args.length == 2)
		{
			if (!args[0].endsWith(".xml"))
				showHelp();
			else
				aFile = new File(args[0]);

			if (args.length == 2)
				showWindows = AttributeType.isValidBinaryTrueValue(args[1]);
	
			runAllFromFile(aFile, showWindows);
			return true;
		}
		else
			return false;
	}

	private static void runAllFromFile(File theFile, boolean showWindows)
	{
		NodeList allExperiments = XMLDocument.parseXMLFile(theFile).getLastChild().getChildNodes();

		for (int i = 0, j = allExperiments.getLength(); i < j; ++i)
			runSubgroupDiscovery(allExperiments.item(i), theFile, showWindows);
	}

	private static void runSubgroupDiscovery(Node theExperimentNode, File theFile, boolean showWindows)
	{
		NodeList aSettings = theExperimentNode.getChildNodes();
		Table aTable = new Table(aSettings.item(2), theFile.getParent() == null ? "." : theFile.getParent());
		aTable.update();
		SearchParameters aSearchParameters = new SearchParameters(aSettings.item(1));
		aSearchParameters.setTargetConcept(new TargetConcept(aSettings.item(0), aTable));

		TargetType aTargetType = aSearchParameters.getTargetType();
		SubgroupDiscovery aSubgroupDiscovery;

		//TODO other types not implemented yet
		if (!TargetType.isImplemented(aTargetType))
			return;

		MiningWindow.echoMiningStart();
		long aBegin = System.currentTimeMillis();

		switch(aTargetType)
		{
			case SINGLE_NOMINAL :
			{
				TargetConcept aTargetConcept = aSearchParameters.getTargetConcept();
				//recompute this number, as we may be dealing with cross-validation here, and hence a smaller number
				int itsPositiveCount = aTable.countValues(aTable.getIndex(aTargetConcept.getPrimaryTarget().getName()), aTargetConcept.getTargetValue());
				Log.logCommandLine("positive count: " + itsPositiveCount);
				aSubgroupDiscovery = new SubgroupDiscovery(aSearchParameters, aTable, itsPositiveCount);
				break;
			}

			case SINGLE_NUMERIC:
			{
				TargetConcept aTargetConcept = aSearchParameters.getTargetConcept();
				//recompute this number, as we may be dealing with cross-validation here, and hence a different value
				float itsTargetAverage = aTable.getAverage(aTable.getIndex(aTargetConcept.getPrimaryTarget().getName()));
				Log.logCommandLine("average: " + itsTargetAverage);
				aSubgroupDiscovery = new SubgroupDiscovery(aSearchParameters, aTable, itsTargetAverage);
				break;
			}
			case MULTI_LABEL :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(aSearchParameters, aTable);
				break;
			}
			case DOUBLE_REGRESSION :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(aSearchParameters, aTable, true);
				break;
			}
			case DOUBLE_CORRELATION :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(aSearchParameters, aTable, false);
				break;
			}
			default :
			{
				Log.logCommandLine("XMLAutoRun Unknown TargetType: "+ aTargetType);
				return;
			}
		}
		aSubgroupDiscovery.Mine(System.currentTimeMillis());
		long anEnd = System.currentTimeMillis();
		if (anEnd > aBegin + (long)(aSearchParameters.getMaximumTime()*60*1000))
		{
			String aMessage = "Mining process ended prematurely due to time limit.";
			if (showWindows)
				JOptionPane.showMessageDialog(null,
								aMessage,
								"Time Limit",
								JOptionPane.INFORMATION_MESSAGE);
			else
				Log.logCommandLine(aMessage);
		}

		MiningWindow.echoMiningEnd(anEnd - aBegin, aSubgroupDiscovery.getNumberOfSubgroups());
		// always save result TODO search parameters based filename
		save(aSubgroupDiscovery.getResult(), (theFile.getAbsolutePath().replace(".xml", ("_"+ aBegin + ".txt"))));

		// following is only needed if windows will be shown
		if (showWindows)
		{
			// ignore cross-validate
			BitSet aBitSet = new BitSet(aTable.getNrRows());
			aBitSet.set(0, aTable.getNrRows());
			switch (aTargetType)
			{
				case MULTI_LABEL :
				{
					BinaryTable aBinaryTable = new BinaryTable(aTable, aSearchParameters.getTargetConcept().getMultiTargets());
					new ResultWindow(aTable, aSubgroupDiscovery, aBinaryTable, 0, aBitSet);
					break;
				}
				default :
				{
					new ResultWindow(aTable, aSubgroupDiscovery, null, 0, aBitSet);
					break;
				}
			}
		}
	}

	public static void save(SubgroupSet theSubgroupSet, String theFileName)
	{
		if (theSubgroupSet == null || theFileName == null)
			return;

		BufferedWriter aWriter = null;

		try
		{
			String aDelimiter = "\t";
			aWriter = new BufferedWriter(new FileWriter(theFileName));

			// hardcoded
			aWriter.write("nr\tdepth\tcoverage\tmeasure\tp-value\tconditionlist\n");

			Iterator<Subgroup> anIterator = theSubgroupSet.iterator();
			Subgroup aSubgroup;
			while (anIterator.hasNext())
			{
				aSubgroup = anIterator.next();

				aWriter.write(String.valueOf(aSubgroup.getID()));
				aWriter.write(aDelimiter);
				aWriter.write(String.valueOf(aSubgroup.getDepth()));
				aWriter.write(aDelimiter);
				aWriter.write(String.valueOf(aSubgroup.getCoverage()));
				aWriter.write(aDelimiter);
				aWriter.write(String.valueOf(aSubgroup.getMeasureValue()));
				aWriter.write(aDelimiter);
				aWriter.write(String.valueOf(aSubgroup.getPValue()));
				aWriter.write(aDelimiter);
				aWriter.write(aSubgroup.getConditions().toString());
				aWriter.write("\n");
			}
		}
		catch (IOException e)
		{
			Log.logCommandLine("Error on file: " + theFileName);
		}
		finally
		{
			try
			{
				if (aWriter != null)
					aWriter.close();
			}
			catch (IOException e)
			{
				Log.logCommandLine("Error on file: " + theFileName);
			}
		}
	}

	private static void showHelp()
	{
		Log.logCommandLine("");
		Log.logCommandLine("Usage: SubDisc /path/to/file.xml [showWindows]");
		Log.logCommandLine("");
		Log.logCommandLine("filepath can be relative");
		Log.logCommandLine("filename must end with '.xml'");
		Log.logCommandLine("optional showWindows:");
		Log.logCommandLine("'true' to show result window");
		Log.logCommandLine("'false' to suppress all GUI elements (default)");
		System.exit(0);
	}
}
