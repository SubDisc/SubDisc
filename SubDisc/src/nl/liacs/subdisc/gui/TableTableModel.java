package nl.liacs.subdisc.gui;

import javax.swing.table.*;

import nl.liacs.subdisc.*;

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
	public String getColumnName(int theColumnIndex)
	{
		Column aColumn = itsTable.getColumn(theColumnIndex);
		return String.format("<html><center>%s<br>(%d distinct)</html>",
								aColumn.getName(),
								aColumn.getNrDistinct());
	}

	@Override
	public int getRowCount() { return itsTable.getNrRows(); }

	@Override
	public Object getValueAt(int row, int col) { return itsTable.getColumn(col).getString(row); }
}
