package nl.liacs.subdisc.gui;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import nl.liacs.subdisc.Table;

public class TableWindow extends JFrame
{
	private static final long serialVersionUID = 1L;

	public TableWindow(Table theTable)
	{
		JTable aJTable = new JTable(new TableTableModel(theTable));
		aJTable.setPreferredScrollableViewportSize(new Dimension(1024, 800));
		aJTable.setFillsViewportHeight(true);
		JPanel jPanelMain = new JPanel(new GridLayout(1, 1));
		JScrollPane aScrollPane = new JScrollPane(aJTable);
		jPanelMain.add(aScrollPane);
		getContentPane().add(jPanelMain);
		setTitle("Data for: " + theTable.itsName);
		pack();
		setVisible(true);
	}
}
