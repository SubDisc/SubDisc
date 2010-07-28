package nl.liacs.subdisc.gui;

import nl.liacs.subdisc.FileHandler;
import nl.liacs.subdisc.FileHandler.Action;

public class SubDisc
{
	static public void main(String[] args)
	{
		new FileHandler(Action.OPEN_FILE);

		if(FileHandler.itsTable != null)
			new MiningWindow(FileHandler.itsTable).setVisible(true);
		else
			new MiningWindow().setVisible(true);
	}

}
