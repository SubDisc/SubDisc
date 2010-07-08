package nl.liacs.subdisc.gui;

import java.io.File;

import javax.swing.JFileChooser;

import nl.liacs.subdisc.Table;

public class SubDisc
{
	static public void main(String[] args)
	{
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
			MiningWindow aMiningWindow = new MiningWindow(new Table(aChooser.getSelectedFile()));
			aMiningWindow.setVisible(true);
		}
	}
}
