package nl.liacs.subdisc.gui;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.FileHandler.Action;

public class SubDisc
{
	public static void main(String[] args)
	{
		if (XMLAutoRun.autoRunSetting(args))
			return;

		//FREEZE
		try
		{
			Thread.sleep(3000);
		}
		catch(Exception theException) {}

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
