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
 		final JFrame aFrame = new JFrame("Grouping of Subgroups According to Pattern Teams");
 		aFrame.setIconImage(MiningWindow.ICON);
		aFrame.setSize(GUI.WINDOW_DEFAULT_SIZE);
		aFrame.setLocation(300, 300);
		aFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		aFrame.setVisible(true);
 
		JTabbedPane aTabbedPane = new JTabbedPane(JTabbedPane.TOP);
		aTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		aFrame.getContentPane().add(aTabbedPane);

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

			JScrollPane aScrollPane = new JScrollPane(aSubgroupsTable);
			aTabbedPane.addTab("Group " + i + " (" + anSS.size() + ")", aScrollPane);
			i++;
		}
	}
}