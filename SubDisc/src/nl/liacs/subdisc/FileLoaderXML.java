package nl.liacs.subdisc;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FileLoaderXML implements FileLoaderInterface
{
	private boolean itsRunAutomatic = true;
	private boolean itsRunAll = true;
	private Document itsDocument;
	private Table itsTable;
	private SearchParameters itsSearchParameters;
	private TargetConcept itsTargetConcept;

	public FileLoaderXML(File theFile)
	{
		if(theFile != null && theFile.exists())
			loadFile(theFile);
		else
			;	// new ErrorDialog(e, ErrorDialog.noSuchFileError);
	}

	private void loadFile(File theFile)
	{
		itsDocument = XMLDocument.parseXMLFile(theFile);
		Node theAutoRunNode = itsDocument.getLastChild();
		NodeList theSettings = theAutoRunNode.getFirstChild().getChildNodes();
		for(int i = 0, j = theSettings.getLength(); i < j; ++i)
			System.out.println(theSettings.item(i).getNodeName());
	}

	public void runAutomatic(boolean theSetting) { itsRunAutomatic = theSetting; }
	public void runAll(boolean theSetting) { itsRunAll = theSetting; }
	@Override
	public Table getTable() { return itsTable; }
	public SearchParameters getSearchParameters() { return itsSearchParameters; }
	public TargetConcept getTargetConcept() { return itsTargetConcept; }
}
