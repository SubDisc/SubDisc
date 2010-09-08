package nl.liacs.subdisc.gui;

import javax.swing.table.*;

import nl.liacs.subdisc.*;

/**
 * TableTableModel is the Model for a JTable containing all data from a
 * {@link Table Table}. TableTableModel extends AbstractTableModel and all
 * methods it overrides are straightforward. The only noteworthy change is the
 * return String for the getColumnName() method, which returns a 2-line String
 * that contains both the name of the {@Attribute Attribute} of that
 * {@link Column Column}, and the number of distinct values for that Attribute.
  */
public class TableTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;

	public TableTableModel(Table theTable)
	{
		if (theTable != null)
			itsTable = theTable;
		else
			return;	// TODO throw warning. All methods should check null also.
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
