package nl.liacs.subdisc.gui;

import java.awt.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.FileHandler.Action;

public class SubDisc
{
	public static void main(String[] args)
	{
		if (!GraphicsEnvironment.isHeadless() && (SplashScreen.getSplashScreen() != null))
		{
			// assume it is an XML-autorun experiment
			if (args.length > 0)
				SplashScreen.getSplashScreen().close();
			else
			{
				try { Thread.sleep(3000); }
				catch (InterruptedException e) {};
				SplashScreen.getSplashScreen().close();
			}
		}

		if (XMLAutoRun.autoRunSetting(args))
			return;

		FileHandler aLoader = new FileHandler(Action.OPEN_FILE);
		Table aTable = aLoader.getTable();
		SearchParameters aSearchParameters = aLoader.getSearchParameters();

		if (aTable == null)
			new MiningWindow();
		else if (aSearchParameters == null)
			new MiningWindow(aTable);
		else
			new MiningWindow(aTable, aSearchParameters);
	}
}
