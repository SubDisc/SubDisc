package nl.liacs.subdisc.gui;

import nl.liacs.subdisc.FileHandler;
import nl.liacs.subdisc.FileHandler.Action;

public class SubDisc
{
	static public void main(String[] args)
	{
		new MiningWindow(new FileHandler(Action.OPEN_FILE).getTable()).setVisible(true);
	}

}
