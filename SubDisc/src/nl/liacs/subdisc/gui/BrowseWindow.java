package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

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
	private final Table itsTable;
	private Subgroup itsSubgroup;
	private JTable itsJTable;
	private boolean itsTPOnly = false;
	/*
	 * TODO Hacked in for now. Used to bring column into focus in
	 * findColumn(). Will be replaced with clean code.
	 */
	private int[] itsOffsets;

	public BrowseWindow(Table theTable)
	{
		itsTable = theTable;
		if (itsTable == null)
		{
			Log.logCommandLine(
				"BrowseWindow Constructor: parameter can not be 'null'.");
			return;
		}
		else
		{
			initComponents(itsTable);
			setTitle("Data for: " + theTable.getName());
			setIconImage(MiningWindow.ICON);
			setLocation(100, 100);
			setSize(GUI.WINDOW_DEFAULT_SIZE);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setVisible(true);
		}
	}

	public BrowseWindow(Table theTable, Subgroup theSubgroup)
	{
		itsTable = theTable;
		if (itsTable == null || theSubgroup == null)
		{
			Log.logCommandLine(
			"BrowseWindow Constructor: parameter(s) must be non-null.");
			return;
		}
		else
		{
			itsSubgroup = theSubgroup;
			initComponents(itsTable);

			int i = itsJTable.convertColumnIndexToModel(itsSubgroup.getConditions().get(0).getAttribute().getIndex());
			itsJTable.setCellSelectionEnabled(true);
			itsJTable.setColumnSelectionInterval(i, i);
			itsJTable.setRowSelectionInterval(0, itsSubgroup.getCoverage() - 1);
			itsJTable.scrollRectToVisible(new Rectangle(itsOffsets[i], 0, 0, 0));

			setTitle(theSubgroup.getCoverage() + " members in subgroup: " + theSubgroup.getConditions());
			setIconImage(MiningWindow.ICON);
			setLocation(100, 100);
			setSize(GUI.WINDOW_DEFAULT_SIZE);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setVisible(true);
		}
	}

	private void initComponents(Table theTable)
	{
		// JTable viewport for theTable
		// TODO use/ allow only one BrowseTableModel per Table
		itsJTable = new JTable(new BrowseTableModel(theTable));
		itsJTable.setRowSorter(((BrowseTableModel)itsJTable.getModel()).getRowSorter());
		itsJTable.setDefaultRenderer(Float.class, CustomRenderer.RENDERER);
		itsJTable.setDefaultRenderer(Boolean.class, CustomRenderer.RENDERER);
		itsJTable.setPreferredScrollableViewportSize(GUI.WINDOW_DEFAULT_SIZE);
		initColumnSizes();
		itsJTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		itsJTable.setFillsViewportHeight(true);
		getContentPane().add(new JScrollPane(itsJTable), BorderLayout.CENTER);

		// close button
		final JPanel aButtonPanel = new JPanel();
		JButton aSaveButton = GUI.buildButton("Save Table", 'S', "save", this);

		// bioinformatics setting
		if (theTable.getDomainList() != null)
			aButtonPanel.add(aSaveButton);

		// Browse Subgroup
		if (itsSubgroup != null)
		{
			// enable only for NOMINAL setting
			if (itsSubgroup.getParentSet().getBinaryTargetClone() != null)
				aButtonPanel.add(GUI.buildButton("True Positives", 'P', "positives", this));

			filter();
			// disable save button as it saves whole Table
			aSaveButton.setVisible(false);
		}

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
	 * TODO Put in SwingWorker background thread.
	 */
	private void initColumnSizes()
	{
		int aHeaderWidth = 0;
		int aTotalWidth = 0;
		itsOffsets = new int[itsJTable.getColumnModel().getColumnCount() + 1]; // ;)
		TableCellRenderer aRenderer = itsJTable.getTableHeader().getDefaultRenderer();

		for (int i = 0, j = itsJTable.getColumnModel().getColumnCount(); i < j; i++)
		{
			// 91 is width of "(999 distinct)"
			aHeaderWidth = Math.max(aRenderer.getTableCellRendererComponent(
									null, itsTable.getColumn(i).getName(),
									false, false, 0, 0).getPreferredSize().width,
									91);

			itsJTable.getColumnModel().getColumn(i).setPreferredWidth(aHeaderWidth);
			itsOffsets[i + 1] = aTotalWidth += aHeaderWidth;
		}
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String anEvent = theEvent.getActionCommand();

		if ("save".equals(anEvent))
			itsTable.toFile();
		else if ("positives".equals(anEvent))
		{
			// TODO clicks on JTable interfere with this ActionEvent/itsTPOnly
			// NOTE button should only be enabled in NOMINAL setting
			itsTPOnly = !itsTPOnly;
			if (itsTPOnly)
			{
				BitSet aTP = itsSubgroup.getParentSet().getBinaryTargetClone();
				aTP.and(itsSubgroup.getMembers());

				for (int i = aTP.nextSetBit(0); i >= 0; i = aTP.nextSetBit(i + 1))
				{
					int k = itsJTable.convertRowIndexToView(i);
					itsJTable.addRowSelectionInterval(k, k);
				}
			}
			else
				itsJTable.clearSelection();
		}
		else if ("close".equals(anEvent))
			dispose();
	}

	// TODO memory hog, cleaner would be to allow only one TableModel per Table
	// and filter on per-view basis
	@SuppressWarnings("unchecked")
	// RowSorter
	private void filter()
	{
		final BitSet aMembers = itsSubgroup.getMembers();

		RowFilter<? super AbstractTableModel, ? super Integer> subgroupFilter = new RowFilter<AbstractTableModel, Integer>() {
			@Override
			public boolean include(Entry<? extends AbstractTableModel, ? extends Integer> entry) {
				return aMembers.get(entry.getIdentifier());
			}
		};

		((DefaultRowSorter<BrowseTableModel, Integer>) itsJTable.getRowSorter()).setRowFilter(subgroupFilter);
	}

	// renders Number using 6 decimals, Boolean as 0/1
	// may be split into separate Boolean and Number renderers
	// may be used for other JTables later
	private static final class CustomRenderer extends DefaultTableCellRenderer {
		public static final CustomRenderer RENDERER;
		public static final NumberFormat FORMATTER;

		static
		{
			RENDERER = new CustomRenderer();
			FORMATTER  = NumberFormat.getNumberInstance();
			FORMATTER.setMaximumFractionDigits(6);
		}
		// only one/uninstantiable
		private CustomRenderer() {}

		@Override
		public void setValue(Object aValue) {
			if (aValue == null) {
				super.setValue(aValue);
				return;
			} else if (aValue instanceof Boolean) {
				setText(((Boolean)aValue) ? "1" : "0");
				return;
			} else if (aValue instanceof Number) {
				setText(FORMATTER.format((Number)aValue));
				return;
			} else {
				super.setValue(aValue);
			}
		}
	}
}
