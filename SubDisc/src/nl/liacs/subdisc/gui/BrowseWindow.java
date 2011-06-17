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
	private BitSet itsSubgroupMembers;
	private BitSet itsTP;	// NOMINAL only
	private JTable itsJTable;
	private JComboBox itsColumnsBox;

	/*
	 * TODO Hacked in for now. Used to bring column into focus in
	 * findColumn(). Will be replaced with clean code.
	 * Problems occur when resizing columns. (Quick-fix: disable resizing.)
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
			itsSubgroupMembers = theSubgroup.getMembers();
			if (theSubgroup.getParentSet().getBinaryTargetClone() != null)
			{
				itsTP = theSubgroup.getParentSet().getBinaryTargetClone();
				itsTP.and(itsSubgroupMembers);
			}
			initComponents(itsTable);
			// by default always focus is on first attribute
			itsColumnsBox.setSelectedIndex(theSubgroup.getConditions().get(0)
													.getAttribute().getIndex());

			setTitle(theSubgroup.getCoverage() +
						" members in subgroup: " + theSubgroup.getConditions());
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
		// TODO create BrowseTable extends JTable class, greatly simplifies code
		itsJTable = new JTable(new BrowseTableModel(theTable))
		{
			private static final long serialVersionUID = 1L;
			private final boolean NOMINAL = (itsTP != null); // fast

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				final Component c = super.prepareRenderer(renderer, row, column);
				// Row color based on TruePositive/FalsePositive (NOMINAL only)
				if (!isColumnSelected(column))
				{
					if (NOMINAL)
						c.setBackground(itsTP.get(convertRowIndexToModel(row)) ?
													Color.GREEN : Color.RED);
					else
						c.setBackground(getBackground());
				}
				return c;
			}
		};
		itsJTable.setRowSelectionAllowed(false);
		itsJTable.setColumnSelectionAllowed(true);
		itsJTable.setRowSorter(((BrowseTableModel)itsJTable.getModel()).getRowSorter());
		itsJTable.setDefaultRenderer(Float.class, NumberRenderer.RENDERER);
		itsJTable.setDefaultRenderer(Boolean.class, BoolRenderer.RENDERER);
		itsJTable.setPreferredScrollableViewportSize(GUI.WINDOW_DEFAULT_SIZE);
		initColumnSizes();
		itsJTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		itsJTable.setFillsViewportHeight(true);
		getContentPane().add(new JScrollPane(itsJTable), BorderLayout.CENTER);

		final JPanel aButtonPanel = new JPanel();

		int aNrColumns = itsTable.getNrColumns();
		String[] aColumnNames = new String[aNrColumns];
		for (int i = 0, j = aNrColumns; i < j; ++i)
			aColumnNames[i] = itsTable.getColumn(i).getName();
		itsColumnsBox = GUI.buildComboBox(aColumnNames, this);
		itsColumnsBox.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);
		aButtonPanel.add(itsColumnsBox);

		JButton aSaveButton = GUI.buildButton("Save Table", 'S', "save", this);

		// bioinformatics setting
		if (theTable.getDomainList() != null)
			aButtonPanel.add(aSaveButton);

		// Browse Subgroup
		if (itsSubgroupMembers != null)
		{
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
		TableColumnModel aColumnModel = itsJTable.getColumnModel();
		itsOffsets = new int[aColumnModel.getColumnCount() + 1]; // ;)
		TableCellRenderer aRenderer = itsJTable.getTableHeader().getDefaultRenderer();

		for (int i = 0, j = aColumnModel.getColumnCount(); i < j; ++i)
		{
			// 91 is width of "(999 distinct)"
			aHeaderWidth = Math.max(aRenderer.getTableCellRendererComponent(
									null, itsTable.getColumn(i).getName(),
									false, false, 0, 0).getPreferredSize().width,
									91);

			aColumnModel.getColumn(i).setPreferredWidth(aHeaderWidth);
			itsOffsets[i + 1] = aTotalWidth += aHeaderWidth;
		}
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String anEvent = theEvent.getActionCommand();

		// relies on only one comboBox being present
		if("comboBoxEdited".equals(anEvent))
			;
		else if("comboBoxChanged".equals(anEvent))
			focusColumn(itsColumnsBox.getSelectedIndex());
		else if ("save".equals(anEvent))
			// TODO allow subgroup to be saved, pass BitSet itsSubgroupMembers
			itsTable.toFile();
		else if ("close".equals(anEvent))
			dispose();
	}

	private void focusColumn(int theModelIndex)
	{
		int i = itsJTable.convertColumnIndexToView(theModelIndex);
		itsJTable.setColumnSelectionInterval(i, i);
		itsJTable.scrollRectToVisible(new Rectangle(itsOffsets[theModelIndex],
													0,
													itsOffsets[theModelIndex + 1],
													0));
	}

	// TODO memory hog, cleaner would be to allow only one TableModel per Table
	// and filter on per-view basis
	@SuppressWarnings("unchecked")
	// RowSorter
	private void filter()
	{
		RowFilter<? super AbstractTableModel, ? super Integer> subgroupFilter =
			new RowFilter<AbstractTableModel, Integer>() {
				final BitSet aMembers = itsSubgroupMembers;

				@Override
				public boolean include(Entry<? extends AbstractTableModel, ? extends Integer> entry)
				{
					return aMembers.get(entry.getIdentifier());
				}
			};

		((DefaultRowSorter<BrowseTableModel, Integer>) itsJTable.getRowSorter()).setRowFilter(subgroupFilter);
	}

	// both *Renderer may be used for other JTables later
	// renders Number using 6 decimals, Boolean as 0/1
	private static final class NumberRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		public static final NumberRenderer RENDERER = new NumberRenderer();
		public static final NumberFormat FORMATTER;

		static
		{
			FORMATTER  = NumberFormat.getNumberInstance();
			FORMATTER.setMaximumFractionDigits(6);
		}
		// only one/uninstantiable
		private NumberRenderer() {}

		@Override
		public void setValue(Object aValue)
		{
			if (aValue instanceof Number)
				setText(FORMATTER.format((Number)aValue));
			else	// not a Number, or null
				super.setValue(aValue);
		}
	}

	// renders Boolean as 0/1
	private static final class BoolRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = 1L;

		public static final BoolRenderer RENDERER = new BoolRenderer();
		private BoolRenderer() {} // only one/uninstantiable

		@Override
		public void setValue(Object aValue)
		{
			if (aValue instanceof Boolean)
				setText(((Boolean)aValue) ? "1" : "0");
			else	// not a Boolean, or null
				super.setValue(aValue);
		}
	}
}
