package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import nl.liacs.subdisc.*;

/**
 * A BrowseWindow contains a JTable that shows all data in a {@link Table Table}
 * , which in turn is read from a <code>File</code> or database. For each
 * {@link Column Column}, the header displays both the name of its
 * {@link Attribute Attribute} and the number of distinct values for that
 * Attribute.
 */
public class BrowseWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public BrowseWindow(Table theTable)
	{
		if (theTable == null)
		{
			Log.logCommandLine(
				"BrowseWindow Constructor: parameter can not be 'null'.");
			return;
		}
		else
		{
			initComponents(theTable);
			setTitle("Data for: " + theTable.getName());
			setIconImage(MiningWindow.ICON);
			setLocation(100, 100);
			setSize(GUI.WINDOW_DEFAULT_SIZE);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//			pack();
			setVisible(true);
		}
	}

	private void initComponents(Table theTable)
	{
		// JTable viewport for theTable
		JTable aJTable = new JTable(new BrowseTableModel(theTable));
		aJTable.setPreferredScrollableViewportSize(GUI.WINDOW_DEFAULT_SIZE);
		initColumnSizes(theTable, aJTable);
		aJTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		aJTable.setFillsViewportHeight(true);
		getContentPane().add(new JScrollPane(aJTable), BorderLayout.CENTER);

		// close button
		final JPanel aButtonPanel = new JPanel();

		// bioinformatics setting
		if (theTable.getDomainList() != null)
			aButtonPanel.add(GUI.buildButton("Save Table", 'S', "save", this));

		final JButton aCloseButton = GUI.buildButton("Close", 'C', "close", this);
		aButtonPanel.add(aCloseButton);
		getContentPane().add(aButtonPanel, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
				aCloseButton.requestFocusInWindow();
			}
		});
	}

	/*
	 * Based on Swing tutorial TableRenderDemo.java.
	 * This method picks column sizes, based on column heads only.
	 * Could use JTable tables' itsTable for sizes instead (1 less parameter).
	 * TODO Put in SwingWorker background thread.
	 */
	private void initColumnSizes(Table theDataTable, JTable theJTable)
	{
		int aHeaderWidth = 0;

		TableCellRenderer aRenderer = theJTable.getTableHeader().getDefaultRenderer();

		for (int i = 0, j = theJTable.getColumnModel().getColumnCount(); i < j; i++)
		{
			// 91 is width of "(999 distinct)"
			aHeaderWidth = Math.max(aRenderer.getTableCellRendererComponent(
									null, theDataTable.getAttribute(i).getName(),
									false, false, 0, 0).getPreferredSize().width,
									91);

			theJTable.getColumnModel().getColumn(i).setPreferredWidth(aHeaderWidth);
		}
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		if ("save".equals(theEvent.getActionCommand()))
			; // TODO save aggregated table to file 
		else if ("close".equals(theEvent.getActionCommand()))
			dispose();
	}

}
