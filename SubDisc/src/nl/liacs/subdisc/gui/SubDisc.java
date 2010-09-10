package nl.liacs.subdisc.gui;

import nl.liacs.subdisc.FileHandler;
import nl.liacs.subdisc.FileHandler.Action;

public class SubDisc
{
	public static void main(String[] args)
	{
		FileHandler aLoader = new FileHandler(Action.OPEN_FILE);
		if (aLoader.getTable() == null)
			new MiningWindow();
		else if (aLoader.getSearchParameters() == null)
			new MiningWindow(aLoader.getTable());
		else
			new MiningWindow(aLoader.getTable(), aLoader.getSearchParameters());
	}

}
