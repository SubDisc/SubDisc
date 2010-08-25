package nl.liacs.subdisc;

import java.io.File;

import org.w3c.dom.NodeList;

public class FileLoaderXML implements FileLoaderInterface
{
	private boolean itsRunAutomatic = true;
	private boolean itsRunAll = true;
	private Table itsTable;
	private SearchParameters itsSearchParameters;

	public FileLoaderXML(File theFile)
	{
		if(theFile != null && theFile.exists())
			loadFile(theFile);
		else
			;	// new ErrorDialog(e, ErrorDialog.noSuchFileError);
	}

	private void loadFile(File theFile)
	{
		NodeList aSettings = XMLDocument.parseXMLFile(theFile)
										.getLastChild()
										.getFirstChild()
										.getChildNodes();

		// order of nodes is know but use fail-safe checking for now 
		for(int i = aSettings.getLength() - 1; i >= 0; --i)
		{
			String aNodeName = aSettings.item(i).getNodeName();
			if("table".equalsIgnoreCase(aNodeName))
			{
				itsTable = new Table(aSettings.item(i));
				itsTable.update();
			}
			else if("search_parameters".equalsIgnoreCase(aNodeName))
				itsSearchParameters = new SearchParameters(aSettings.item(i));
			else if("target_concept".equalsIgnoreCase(aNodeName))	// TODO NOTE order sensitive
				itsSearchParameters.setTargetConcept(new TargetConcept(aSettings.item(i)));
		}
	}

	public void runAutomatic(boolean theSetting) { itsRunAutomatic = theSetting; }
	public void runAll(boolean theSetting) { itsRunAll = theSetting; }
	@Override
	public Table getTable() { return itsTable; }
	public SearchParameters getSearchParameters() { return itsSearchParameters; }
}
