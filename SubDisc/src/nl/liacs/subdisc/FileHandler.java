package nl.liacs.subdisc;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class FileHandler
{
	public static enum Action { OPEN_FILE, OPEN_DATABASE, SAVE };
	public static Table itsTable;	// there can be only one

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
		JFileChooser aChooser = new JFileChooser(new File("."));
		aChooser.setFileFilter (
			new FileFilter()
			{
				public boolean accept(File f)
				{
					return f.isDirectory() ||
						f.getName().toLowerCase().endsWith(".txt") ||
						f.getName().toLowerCase().endsWith(".arff");
				}
				public String getDescription() { return "Data Files"; }
			}
		);

		if (aChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			File aFile = aChooser.getSelectedFile();
			String aFileName = aFile.getName().toLowerCase();

			if(aFileName.endsWith(".txt"))
			{
				try
				{
					itsTable = new FileLoaderTXT().loadFile(aFile);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if(aFileName.endsWith(".arff"))
				itsTable = new FileLoaderARFF().loadFile(aFile);

			itsTable.print();
		}

	}

	private static void openDatabase()
	{
		
	}

	private static void save()
	{
		
	}
}
