package nl.liacs.subdisc;

import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class saveAsSQL extends JFrame
{
	private static final long serialVersionUID = 1L;

	private JFileChooser itsChooser;
	private String itsName;
	private File itsFile;
	private boolean isValid = false;

	public File getFile() { return itsFile; }
	public String getName() { return itsName; }
	public boolean isValid() { return isValid; }

	public saveAsSQL()
	{
		itsChooser = new JFileChooser();
		itsChooser.setCurrentDirectory(new File("."));
		itsChooser.setFileFilter (new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
			}
			public String getDescription() { return "txt files"; }
		});

		int r = itsChooser.showSaveDialog(this);
		if (r != JFileChooser.APPROVE_OPTION)
			return;
		itsFile = itsChooser.getSelectedFile();
		itsName = itsFile.getName();
		isValid = true;
	}

	public void saveSQLViewsToFile(String theContent)
	{
		boolean errorMade = false;
		java.io.OutputStream aFileStream = System.out;
		FileOutputStream aFile = null;
		String aFileName = getName();

		if ( !aFileName.endsWith(".txt") )
			aFileName += ".txt";

		try
		{
			aFile = new java.io.FileOutputStream(itsChooser.getCurrentDirectory() + "\\" + aFileName);
		}
		catch (Exception ex) { errorMade = true; }

		if (!errorMade)
		{
			aFileStream = aFile;
			try
			{
				aFileStream.write(charsToBytes(theContent.toCharArray()));
			}
			catch (Exception ex) { }
		}

		try
		{
			aFileStream.flush(); aFileStream.close();
		}
		catch (Exception ex) { }
	}

	private static byte[] charsToBytes(char[] ca)
	{
		byte[] ba = new byte[ca.length];
		for (int i = 0; i < ca.length; i++)
			ba[i] = (byte)ca[i];
		return ba;
	}
}