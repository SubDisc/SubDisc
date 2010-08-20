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

	// TODO catch exception @ FileLoaderTXT
	private void openFile()
	{
		showFileChooser(Action.OPEN_FILE);

		if(itsFile == null)
			return;

		String aFileName = itsFile.getName().toLowerCase();

		if(aFileName.endsWith(".txt"))
		{
			try
			{
				itsTable = new FileLoaderTXT().loadFile(itsFile);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else if(aFileName.endsWith(".arff"))
			itsTable = new FileLoaderARFF().loadFile(itsFile);
		else if(aFileName.endsWith(".xml"))
			;//				itsTable = new FileLoaderXML(aFile);

//			itsTable.print();
	}

	private void openDatabase()
	{

	}

	private File save()
	{
		showFileChooser(Action.SAVE);
		return itsFile;
	}

	/**
	 * Shows a JChooser dialog for opening and saving files.
	 * @param The file types to be shown, based on extensions.
	 * @return The selected file if one is chosen, null otherwise.
	 */
	private void showFileChooser(Action theAction)
	{
		//setIconImage(MiningWindow.ICON)
		JFileChooser aChooser = new JFileChooser(new File(itsLastFileLocation));
		aChooser.setFileFilter (new FileTypeFilter(FileType.ALL_DATA));	// TODO ALL_DATA for now

		int theOption = -1;

		if(theAction == Action.OPEN_FILE)
			theOption = aChooser.showOpenDialog(this);
		else if(theAction == Action.SAVE)
			theOption = aChooser.showSaveDialog(this);

		if(theOption == JFileChooser.APPROVE_OPTION)
		{
			itsFile = aChooser.getSelectedFile();
			itsLastFileLocation = itsFile.getParent();
		}
	}

	/**
	 * If a JFileChooser dialog was show and the user selected a file, use this
	 * method to retrieve it. Return null if the user has not made an approved
	 * selection.
	 * @return The last file selected, and approved, by the user.
	 */
	public File getFile() { return itsFile; };

	/**
	 * If the FileHandler has successfully loaded a Table from a file or a
	 * database this method returns the Table, else it returns null.
	 * @return itsTable if present, null otherwise
	 */
	public Table getTable() { return itsTable; };
}
