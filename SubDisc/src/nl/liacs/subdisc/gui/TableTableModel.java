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
		if (theTable == null)
		{
			Log.logCommandLine(" Constructor()");
			return;
		}
		else
			itsTable = theTable;
	}

	@Override
	public int getColumnCount()
	{
		if (itsTable == null)
		{
			LogError(".getColumnCount()");
			return 0;
		}
		else
			return itsTable.getNrColumns();
	}

	// TODO Put in SwingWorker background thread.
	@Override
	public String getColumnName(int theColumnIndex)
	{
		if (itsTable == null)
		{
			LogError(".getColumnName()");
			return "Incorrect column index.";
		}
		else
		{
			Column aColumn = itsTable.getColumn(theColumnIndex);
			return String.format("<html><center>%s<br>(%d distinct)</html>",
									aColumn.getName(),
									aColumn.getCardinality());
		}
	}

	@Override
	public int getRowCount()
	{
		if (itsTable == null)
		{
			LogError(".getRowCount()");
			return 0;
		}
		else
			return itsTable.getNrRows();
	}

	@Override
	public Object getValueAt(int theRow, int theColumn)
	{
		if (itsTable == null)
		{
			LogError(".getValueAt()");
			return null;
		}
		else
			return itsTable.getColumn(theColumn).getString(theRow);
	}

	private void LogError(String theMethod)
	{
		Log.logCommandLine(
			"Error in TableTableWindow" + theMethod + ": Table is 'null'.");
	}
}
