/**
 * TODO use fileNameExtension class.
 */
package nl.liacs.subdisc;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class FileHandler extends JFrame
{
	private static final long serialVersionUID = 1L;

	public static enum Action { OPEN_FILE, OPEN_DATABASE, SAVE };

	// remember the directory of the last used file, defaults to users' 
	// (platform specific) home directory if the the path cannot be resolved
	private static String itsLastFileLocation = ".";

	private Table itsTable;
	private SearchParameters itsSearchParameters;
	private File itsFile;

	public FileHandler(Action theAction)
	{
		switch(theAction)
		{
			case OPEN_FILE : openFile(); break;
			case OPEN_DATABASE : openDatabase(); break;
			case SAVE : save(); break;
			default : break;
		}
	}

	public FileHandler(File theFile)
	{
		if (theFile == null || !theFile.exists())
			return;
		else
			itsFile = theFile;
	}

	private void openFile()
	{
		showFileChooser(Action.OPEN_FILE);

		if (itsFile == null || !itsFile.exists())
			return;

		switch (FileType.getFileType(itsFile))
		{
			case TXT : itsTable = new FileLoaderTXT(itsFile).getTable(); break;
			case ARFF :
			{
				itsTable = new FileLoaderARFF(itsFile).getTable();
				break;
			}
			case XML :
			{
				FileLoaderXML aLoader = new FileLoaderXML(itsFile);
				itsTable = aLoader.getTable();
				itsSearchParameters =  aLoader.getSearchParameters();
				break;
			}
			// unknown FileType, log error
			default :
			{
				Log.logCommandLine(
					String.format(
								"FileHandler: unknown FileType for File '%s'.",
								itsFile.getName()));
				break;
			}
		}
//			itsTable.print();	// TODO
	}

	private void openDatabase()
	{

	}

	private File save()
	{
		showFileChooser(Action.SAVE);
		return itsFile;
	}

	private void showFileChooser(Action theAction)
	{
		//setIconImage(MiningWindow.ICON)
		JFileChooser aChooser = new JFileChooser(new File(itsLastFileLocation));
		aChooser.addChoosableFileFilter(new FileTypeFilter(FileType.TXT));
		aChooser.addChoosableFileFilter(new FileTypeFilter(FileType.ARFF));
		aChooser.addChoosableFileFilter(new FileTypeFilter(FileType.XML));
		aChooser.setFileFilter (new FileTypeFilter(FileType.ALL_DATA_FILES));

		int theOption = -1;

		if (theAction == Action.OPEN_FILE)
			theOption = aChooser.showOpenDialog(this);
		else if (theAction == Action.SAVE)
			theOption = aChooser.showSaveDialog(this);

		if(theOption == JFileChooser.APPROVE_OPTION)
		{
			itsFile = aChooser.getSelectedFile();
			itsLastFileLocation = itsFile.getParent();
		}
	}

	/**
	 * If a <code>JFileChooser</code> dialog was shown and a <code>File</code>
	 * was selected, use this method to retrieve it.
	 * 
	 * @return a <code>File</code>, or <code>null</code> if no approved
	 * selection was made
	 */
	public File getFile() { return itsFile; };

	/**
	 * If this FileHandler successfully loaded a {@link Table Table} from a
	 * <code>File</code> or a database, use this method to retrieve it.
	 * 
	 * @return the <code>Table</code> if present, <code>null</code> otherwise
	 */
	public Table getTable() { return itsTable; };

	/**
	 * If the FileHandler successfully loaded the
	 * {@link SearchParameters SearchParameters} from a <code>File</code>, use
	 * this method to retrieve them.
	 * 
	 * @return the <code>SearchParameters</code> if present, <code>null</code>
	 * otherwise
	 */
	public SearchParameters getSearchParameters()
	{
		return itsSearchParameters;
	};

}
