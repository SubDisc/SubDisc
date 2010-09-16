package nl.liacs.subdisc.gui;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import nl.liacs.subdisc.*;

/**
 * A TableWindow contains a JTable that shows all data in a {@link Table Table},
 * which in turn is read from a file or database. For each {@link Column Column}
 * , the header displays both the name of its {@link Attribute Attribute} and
 * the number of distinct values for that Attribute.
 */
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
		JPanel jPanelMain = new JPanel(new GridLayout(1, 1));
		jPanelMain.add(new JScrollPane(aJTable));
		getContentPane().add(jPanelMain);
		setTitle("Data for: " + theTable.getName());
		pack();
		setVisible(true);
	}

	/*
	 * Based on Swing tutorial TableRenderDemo.java.
	 * This method picks column sizes, based on column heads only.
	 * Could use JTable tables' itsTable for sizes instead (1 less parameter).
	 */
	private void initColumnSizes(Table theTable, JTable table)
	{
		int aHeaderWidth = 0;

		TableCellRenderer aRenderer = table.getTableHeader().getDefaultRenderer();

		for (int i = 0, j = table.getColumnModel().getColumnCount(); i < j; i++)
		{
			// 91 is width of "(999 distinct)"
			aHeaderWidth = Math.max(aRenderer.getTableCellRendererComponent(
									null, theTable.getAttribute(i).getName(),
									false, false, 0, 0).getPreferredSize().width,
									91);

			table.getColumnModel().getColumn(i).setPreferredWidth(aHeaderWidth);
		}
	}

}
