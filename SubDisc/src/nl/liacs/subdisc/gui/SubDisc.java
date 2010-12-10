package nl.liacs.subdisc.gui;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.FileHandler.Action;

public class SubDisc
{
	public static void main(String[] args)
	{
		//FREEZE
		try
		{
			Thread.sleep(3000);
		}
		catch(Exception theException) {}

		FileHandler aLoader = new FileHandler(Action.OPEN_FILE);
		Table aTable = aLoader.getTable();
		SearchParameters aSearchParameters = aLoader.getSearchParameters();

		MiningWindow aWindow;
		if (aTable == null)
			aWindow = new MiningWindow();
		else if (aSearchParameters == null)
			aWindow = new MiningWindow(aTable);
		else
			aWindow = new MiningWindow(aTable, aSearchParameters);

		aWindow.setLocation(100, 100);
	}

}
