package nl.liacs.subdisc;

import java.io.*;
import java.text.*;
import java.util.*;

import nl.liacs.subdisc.XMLDocument.XMLType;
import nl.liacs.subdisc.gui.*;

import org.w3c.dom.*;

public class XMLAutoRun
{
	// leave false in svn - write result with Conditions in canonical order
	// this makes results invocation invariant
	private static final boolean IS_MULTI_THREAD_TEST = false;

	public static final String RESULT_SET_DELIMITER = "\t";

	private Document itsDocument;

	public enum AutoRun { CREATE, ADD }

	public XMLAutoRun(SearchParameters theSearchParameters, Table theTable, AutoRun theFileOption)
	{
		if (theSearchParameters == null || theTable == null)
			throw new IllegalArgumentException("arguments can not be null");

		File aFile = new FileHandler(FileType.XML).getFile();
		if (aFile == null)
			return;

		buildDocument(aFile, theSearchParameters, theTable, theFileOption);
	}

	/* Other constructor uses GUI for file-destination via FileHandler. */
	public XMLAutoRun(SearchParameters theSearchParameters, Table theTable, File theOutputFile)
	{
		if (theSearchParameters == null || theTable == null || theOutputFile == null)
			throw new IllegalArgumentException("XMLAutoRun<init>: arguments can not be null");

		buildDocument(theOutputFile, theSearchParameters, theTable, AutoRun.CREATE);
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
		((Element) anExperimentNode).setAttribute("id", String.valueOf(aLength));
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
	 * @param args the <code>String[]</code> containing the command-line
	 * parameters.
	 *
	 * @return <code>true</code> if the parameters are valid,
	 * <code>false</code> otherwise.
	 */
	public static boolean autoRunSetting(String[] args)
	{
		if (args.length == 0)
			return false;

		File aFile = null;
		boolean showWindows = false;
		int aNrThreads = Integer.MIN_VALUE;

		if (!args[0].endsWith(".xml") || args.length > 3)
			showHelp();
		else
			aFile = new File(args[0]);

		if (args.length >= 2)
			showWindows = AttributeType.isValidBinaryTrueValue(args[1]);

		if (args.length == 3)
		{
			try { aNrThreads = Integer.parseInt(args[2]); }
			catch (NumberFormatException e) { showHelp(); }
		}

		runAllFromFile(aFile, showWindows, aNrThreads);

//		// this seems unnecessary, there should be no windows in this case
//		// though this deals with ErrorDialogs / incorrectly opened windows
//		if (!showWindows)
//			return true;

		// returns only when all windows are closed
		return true;
	}

	private static void runAllFromFile(File theFile, boolean showWindows, int theNrThreads)
	{
		NodeList allExperiments = XMLDocument.parseXMLFile(theFile).getLastChild().getChildNodes();

		for (int i = 0, j = allExperiments.getLength(); i < j; ++i)
			runSubgroupDiscovery(allExperiments.item(i), theFile, showWindows, theNrThreads);
	}

	private static void runSubgroupDiscovery(Node theExperimentNode, File theFile, boolean showWindows, int theNrThreads)
	{
		NodeList aSettings = theExperimentNode.getChildNodes();
		Table aTable = new Table(aSettings.item(2), theFile.getParent() == null ? "." : theFile.getParent(), showWindows);
		aTable.update();
		SearchParameters aSearchParameters = new SearchParameters(aSettings.item(1));
		// allows to overwrite the number defined in the XML file
		if (theNrThreads == Integer.MIN_VALUE)
			theNrThreads = aSearchParameters.getNrThreads();
		aSearchParameters.setTargetConcept(new TargetConcept(aSettings.item(0), aTable));

		long aBegin = System.currentTimeMillis();
		SubgroupDiscovery aSubgroupDiscovery =
			Process.runSubgroupDiscovery(aTable, 0, null, aSearchParameters, showWindows, theNrThreads, null); //null means no progress update to mainwindow
		// always save result TODO search parameters based filename
		String aTimeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(aBegin));
		save(aSubgroupDiscovery.getResult(), theFile.getAbsolutePath().replace(".xml", ("_"+ aTimeStamp + ".txt")), aSearchParameters.getTargetType());
	}

	public static void save(SubgroupSet theSubgroupSet, String theFileName, TargetType theTargetType)
	{
		if (theSubgroupSet == null || theFileName == null)
			return;

		BufferedWriter aWriter = null;

		try
		{
			String aDelimiter = RESULT_SET_DELIMITER;
			aWriter = new BufferedWriter(new FileWriter(theFileName));

			aWriter.write(ResultTableModel.getColumnName(0, theTargetType));
			for (int i = 1, j = ResultTableModel.COLUMN_COUNT; i < j; ++i)
			{
				aWriter.write(aDelimiter);
				aWriter.write(ResultTableModel.getColumnName(i, theTargetType));
			}
			aWriter.write("\n");

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
				aWriter.write(String.valueOf(aSubgroup.getSecondaryStatistic()));
				aWriter.write(aDelimiter);
				aWriter.write(String.valueOf(aSubgroup.getTertiaryStatistic()));
				aWriter.write(aDelimiter);
				aWriter.write(String.valueOf(aSubgroup.getPValue()));
				aWriter.write(aDelimiter);
				if (IS_MULTI_THREAD_TEST)
					aWriter.write(ConditionListBuilder.toCanonicalOrderString(aSubgroup.getConditions()));
				else
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
		Log.logCommandLine("Usage: java -jar subdisc-gui-2.xxxx.jar /path/to/file.xml [showWindows] [nrThreads]");
		Log.logCommandLine("");
		Log.logCommandLine("filepath can be relative");
		Log.logCommandLine("filename must end with '.xml'");
		Log.logCommandLine("");
		Log.logCommandLine("optional showWindows:");
		Log.logCommandLine("'true' to show result window");
		Log.logCommandLine("'false' to suppress all GUI elements (default)");
		Log.logCommandLine("");
		Log.logCommandLine("optional nrThreads:");
		Log.logCommandLine("positive integer indicating the number of threads to use");
		Log.logCommandLine("default is " + Runtime.getRuntime().availableProcessors());
		Log.logCommandLine("(determined through java.lang.Runtime.getRuntime().availableProcessors())");
		Log.logCommandLine("");
		System.exit(0);
	}
}
