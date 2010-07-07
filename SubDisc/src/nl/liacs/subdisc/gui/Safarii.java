package nl.liacs.subdisc.gui;

import java.io.File;

import javax.swing.JFileChooser;

import nl.liacs.subdisc.Table;

public class Safarii
{
	static public void main(String[] args)
	{
		String aName;
		File aDataFile;
//		try
//		{
//			Thread.sleep(2000);
//			SplashScreen.getSplashScreen().close();
//		}
//		catch(Exception theException) {}

		JFileChooser aChooser = new JFileChooser();
		aChooser.setCurrentDirectory(new File("."));
		aChooser.setFileFilter (new javax.swing.filechooser.FileFilter()
		{
			public boolean accept(File f)
			{
				return f.isDirectory() ||
					f.getName().toLowerCase().endsWith(".txt") ||
					f.getName().toLowerCase().endsWith(".arff");
			}
			public String getDescription() { return "Data Files"; }
		});
		int aResult = aChooser.showOpenDialog(null);
		if (aResult == JFileChooser.APPROVE_OPTION)
		{
			aDataFile = aChooser.getSelectedFile();
			aName = aDataFile.getName();

			Table aTable = new Table(aDataFile);

			MiningWindow aMiningWindow = new MiningWindow(aTable);
			aMiningWindow.setVisible(true);
		}
	}
}
