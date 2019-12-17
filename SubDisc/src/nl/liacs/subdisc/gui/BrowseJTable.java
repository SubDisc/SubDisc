package nl.liacs.subdisc.gui;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import nl.liacs.subdisc.*;

public class BrowseJTable extends JTable
{
	private static final long serialVersionUID = 1L;
	private final BitSet itsMembers;
	private final BitSet itsTruePositives;
	private final boolean isNominal; // fast
	/*
	 * TODO Hacked in for now. Used to bring column into focus in
	 * findColumn(). Will be replaced with clean code.
	 * Problems occur when resizing columns. (Quick-fix: disable resizing.)
	 */
	private int[] itsOffsets;

	public BrowseJTable(Table theTable, Subgroup theSubgroup)
	{
		if (theTable == null)
		{
			itsMembers       = null;
			itsTruePositives = null;
			isNominal        = false;
			return;	// warning
		}
		else if (theSubgroup == null)
		{
			itsMembers       = null;
			itsTruePositives = null;
		}
		else
		{
			itsMembers       = theSubgroup.getMembers();
			itsTruePositives = theSubgroup.getParentSet().getBinaryTargetClone();
			if (itsTruePositives != null)
				itsTruePositives.and(itsMembers);
		}
		isNominal = (itsTruePositives != null);

		BrowseTableModel b                 = new BrowseTableModel(theTable);
		TableRowSorter<BrowseTableModel> t = new TableRowSorter<>(new BrowseTableModel(theTable));
		t.setRowFilter(new RowFilterBitSet(itsMembers));
		super.setModel(b);
		super.setRowSorter(t);

		super.setRowSelectionAllowed(false);
		super.setColumnSelectionAllowed(true);
		super.setDefaultRenderer(Float.class, RendererNumber.RENDERER);
		super.setDefaultRenderer(Boolean.class, RendererBoolean.RENDERER);
		super.setPreferredScrollableViewportSize(GUI.WINDOW_DEFAULT_SIZE);
		initColumnSizes(theTable);
		super.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		super.setFillsViewportHeight(true);
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
	{
		final Component c = super.prepareRenderer(renderer, row, column);
		// Row color based on TruePositive/FalsePositive (NOMINAL only)
		if (!isColumnSelected(column))
		{
			if (isNominal && !itsTruePositives.get(convertRowIndexToModel(row)))
				c.setBackground(GUI.RED);
			else
				c.setBackground(getBackground());
		}
		return c;
	}

	/*
	 * Based on Swing tutorial TableRenderDemo.java.
	 * This method picks column sizes, based on column heads only.
	 * TODO Put in SwingWorker background thread.
	 */
	private void initColumnSizes(Table theTable)
	{
		int aHeaderWidth = 0;
		int aTotalWidth = 0;
		TableColumnModel aColumnModel = super.getColumnModel();
		itsOffsets = new int[aColumnModel.getColumnCount() + 1]; // ;)
		TableCellRenderer aRenderer = super.getTableHeader().getDefaultRenderer();

		for (int i = 0, j = aColumnModel.getColumnCount(); i < j; ++i)
		{
			// 91 is width of "(999 distinct)"
			aHeaderWidth = Math.max(aRenderer.getTableCellRendererComponent(
									null, theTable.getColumn(i).getName(),
									false, false, 0, 0).getPreferredSize().width,
									91);

			aColumnModel.getColumn(i).setPreferredWidth(aHeaderWidth);
			itsOffsets[i + 1] = aTotalWidth += aHeaderWidth;
		}
	}

	public void focusColumn(int theModelIndex)
	{
		super.scrollRectToVisible(new Rectangle(0, 0, 0, 0)); // HACK
		if (theModelIndex < 0 ||
				theModelIndex >= super.getColumnModel().getColumnCount())
			super.clearSelection();
		else
		{
			int i = super.convertColumnIndexToView(theModelIndex);
			super.setColumnSelectionInterval(i, i);
			super.scrollRectToVisible(new Rectangle(itsOffsets[theModelIndex],
													0,
													itsOffsets[theModelIndex + 1],
													0));
		}
	}
}
