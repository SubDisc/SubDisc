package nl.liacs.subdisc.gui;

import javax.swing.table.*;

import nl.liacs.subdisc.*;

public class MetaDataTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	private Table itsTable;

	public enum MetaDataTableHeader
	{
		ATTRIBUTE(0, "Attribute"),
		CARDINALITY(1, "Cardinality"),
		TYPE(2, "Type"),
		ENABLED(3, "Enabled"),
		HAS_MISSING(4, "Missing Values");

		public final int columnNr;
		public final String guiText;

		private MetaDataTableHeader(int theColumnNr, String theGuiText)
		{
			columnNr = theColumnNr;
			guiText = theGuiText;
		}

		public static String getColumnName(int theColumnIndex)
		{
			for (MetaDataTableHeader h : MetaDataTableHeader.values())
				if (h.columnNr == theColumnIndex)
						return h.guiText;
			Log.logCommandLine("Error in MetaDataTableHeader.getColumnName(): invalid index '" + theColumnIndex + "'.");
			return "Incorrect column index.";
		}
	};

	public MetaDataTableModel(Table theTable)
	{
		if (theTable == null)
		{
			LogError(" Constructor()");
			return;
		}
		else
			itsTable = theTable;
	}

	@Override
	public int getColumnCount() { return MetaDataTableHeader.values().length; }

	@Override
	public String getColumnName(int theColumnIndex)
	{
		return MetaDataTableHeader.getColumnName(theColumnIndex);
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
			return itsTable.getNrColumns();
	}

	@Override
	public Object getValueAt(int row, int col)
	{
		if (itsTable == null)
		{
			LogError(".getValueAt()");
			return null;
		}
		else
		{
			if (col == MetaDataTableHeader.ATTRIBUTE.columnNr)
				return itsTable.getColumn(row).getName();
			else if (col == MetaDataTableHeader.CARDINALITY.columnNr)
				return itsTable.getColumn(row).getCardinality();
			else if (col == MetaDataTableHeader.TYPE.columnNr)
				return itsTable.getColumn(row).getType();
			else if (col == MetaDataTableHeader.ENABLED.columnNr)
				return itsTable.getColumn(row).getIsEnabled() ? "yes" : "no";
			else if (col == MetaDataTableHeader.HAS_MISSING.columnNr)
				return itsTable.getColumn(row).getHasMissingValues() ? "yes" : "no";
			else
			{
				Log.logCommandLine("Error in MetaDataTableModel.getValueAt(): " + "invalid index: '" + col + "' for MetaDataTableHeader.");
				return null;
			}
		}
	}

	private void LogError(String theMethod)
	{
		Log.logCommandLine("Error in MetaDataTableWindow" + theMethod + ": Table is 'null'.");
	}
}

