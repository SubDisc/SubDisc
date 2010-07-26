package nl.liacs.subdisc.gui;

import java.io.File;

import javax.swing.JFileChooser;

import nl.liacs.subdisc.FileLoaderARFF;
import nl.liacs.subdisc.FileLoaderTXT;

public class SubDisc
{
	static public void main(String[] args) throws Exception
	{
//		try
//		{
//			Thread.sleep(2000);
//			SplashScreen.getSplashScreen().close();
//		}
//		catch(Exception theException) {}

		JFileChooser aChooser = new JFileChooser(new File("."));
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
		// TODO get selected file type and use appropriated loader
		if (aResult == JFileChooser.APPROVE_OPTION)
		{
			File aFile = aChooser.getSelectedFile();
			MiningWindow aMiningWindow;

			if(aFile.getName().toLowerCase().endsWith(".txt"))
				aMiningWindow = new MiningWindow(new FileLoaderTXT().loadFile(aFile));
			else
				aMiningWindow = new MiningWindow(new FileLoaderARFF().loadFile(aFile));
			aMiningWindow.setVisible(true);
		}
	}
}
