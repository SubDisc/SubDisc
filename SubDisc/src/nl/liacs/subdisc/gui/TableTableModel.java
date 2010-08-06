package nl.liacs.subdisc.gui;

import javax.swing.table.AbstractTableModel;

import nl.liacs.subdisc.Table;

public class TableTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;

	public TableTableModel(Table theTable)
	{
		itsTable = theTable;
	}

	@Override
	public int getColumnCount() { return itsTable.getNrColumns(); }

	@Override
	public String getColumnName(int theColumnIndex) { return itsTable.getColumn(theColumnIndex).getName(); }

	@Override
	public int getRowCount() { return itsTable.getNrRows(); }

	@Override
	public Object getValueAt(int row, int col) { return itsTable.getColumn(col).getString(row); }
}
