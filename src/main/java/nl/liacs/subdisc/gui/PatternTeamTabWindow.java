package nl.liacs.subdisc.gui;
 
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

import nl.liacs.subdisc.*;
 
public class PatternTeamTabWindow
{
	private ArrayList<SubgroupSet> itsGrouping;
	private SearchParameters itsSearchParameters;

	public PatternTeamTabWindow(ArrayList<SubgroupSet> theGrouping, SearchParameters theSearchParameters)
	{
		itsGrouping = theGrouping;
		itsSearchParameters = theSearchParameters;
	}
	
	public void createWindow() 
	{
		System.out.println("=============");

 		final JFrame frame = new JFrame("Split Pane Example");
 
 		// Display the window.
		frame.setSize(GUI.WINDOW_DEFAULT_SIZE.width, GUI.WINDOW_DEFAULT_SIZE.height);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
		// set grid layout for the frame
		frame.getContentPane().setLayout(new GridLayout(1, 1));

		JTabbedPane aTabbedPane = new JTabbedPane(JTabbedPane.TOP);

		int i = 1;
		for (SubgroupSet anSS : itsGrouping)
	 	{
			ResultTableModel aResultTableModel = new ResultTableModel(anSS, itsSearchParameters.getTargetConcept().getTargetType());
			JTable aSubgroupsTable = new JTable(aResultTableModel);

			// NOTE scaling is based on 8 columns, there are 20 unitWidths
			int aUnitWidth = (int)(0.05f * GUI.WINDOW_DEFAULT_SIZE.width);

			aSubgroupsTable.getColumnModel().getColumn(0).setPreferredWidth(aUnitWidth);
			aSubgroupsTable.getColumnModel().getColumn(1).setPreferredWidth(aUnitWidth);
			aSubgroupsTable.getColumnModel().getColumn(2).setPreferredWidth((int)(1.75f * aUnitWidth));
			aSubgroupsTable.getColumnModel().getColumn(3).setPreferredWidth((int)(1.75f * aUnitWidth));
			aSubgroupsTable.getColumnModel().getColumn(4).setPreferredWidth((int)(1.75f * aUnitWidth));
			aSubgroupsTable.getColumnModel().getColumn(5).setPreferredWidth((int)(1.75f * aUnitWidth));
			aSubgroupsTable.getColumnModel().getColumn(6).setPreferredWidth(2 * aUnitWidth);
			aSubgroupsTable.getColumnModel().getColumn(7).setPreferredWidth(9 * aUnitWidth);


			aSubgroupsTable.setRowSelectionAllowed(true);
			aSubgroupsTable.setColumnSelectionAllowed(false);
			aSubgroupsTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

			aTabbedPane.addTab("Group " + i + " (" + anSS.size() + ")", aSubgroupsTable);
			aTabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
			i++;
		}

		aTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		frame.getContentPane().add(aTabbedPane);

		frame.setIconImage(MiningWindow.ICON);
		frame.setLocation(250, 250);
		frame.setSize(GUI.WINDOW_DEFAULT_SIZE);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
}