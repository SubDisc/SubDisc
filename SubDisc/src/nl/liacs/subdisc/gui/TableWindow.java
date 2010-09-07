package nl.liacs.subdisc.gui;

import javax.swing.*;
import javax.swing.table.*;

import nl.liacs.subdisc.*;

public class TableWindow extends JFrame
{
	private static final long serialVersionUID = 1L;

	public TableWindow(Table theTable)
	{
		JTable aJTable = new JTable(new TableTableModel(theTable));
		aJTable.setPreferredScrollableViewportSize(GUI.DEFAULT_WINDOW_DIMENSION);
		initColumnSizes(theTable, aJTable);
		aJTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		aJTable.setFillsViewportHeight(true);
		JPanel jPanelMain = new JPanel();
		jPanelMain.add(new JScrollPane(aJTable));
		getContentPane().add(jPanelMain);
		setTitle("Data for: " + theTable.getTableName());
		pack();
		setVisible(true);
	}

	/*
	 * Based on Swing tutorial TableRenderDemo.java.
	 * This method picks column sizes, based on column heads only.
	 * Could use JTable tables' itsTable for sizes instead (1 less parameter).
	 */
	private int initColumnSizes(Table theTable, JTable table)
	{
		int headerWidth = 0;
		int aWidth = 0;

		TableCellRenderer headerRenderer =
			table.getTableHeader().getDefaultRenderer();

		for (int i = 0, j = table.getColumnModel().getColumnCount(); i < j; i++)
		{
			// 91 is width of "(999 distinct)"
			headerWidth = Math.max(headerRenderer.getTableCellRendererComponent(
									null, theTable.getAttribute(i).getName(),
									false, false, 0, 0).getPreferredSize().width
									, 91);

			table.getColumnModel().getColumn(i).setPreferredWidth(headerWidth);
			aWidth += headerWidth;
		}

		return aWidth;
	}

}
