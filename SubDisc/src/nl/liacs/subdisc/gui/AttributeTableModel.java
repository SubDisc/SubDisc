package nl.liacs.subdisc.gui;

import javax.swing.table.*;

import nl.liacs.subdisc.*;

public class AttributeTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	private Table itsTable;

	public enum AttributeTableHeader
	{
		ATTRIBUTE(0, "Attribute"),
		TYPE(1, "Type"),
		ENABLED(2, "Enabled"),
		MISSING(3, "Missing Value");

		public final int columnNr;
		public final String guiText;

		private AttributeTableHeader(int theColumnNr, String theGuiText)
		{
			columnNr = theColumnNr;
			guiText = theGuiText;
		}

		public static String getColumnName(int theColumnIndex)
		{
			for (AttributeTableHeader h : AttributeTableHeader.values())
				if (h.columnNr == theColumnIndex)
						return h.guiText;
			Log.logCommandLine(
				"Error in AttributeTableHeader.getColumnName(): invalid index '"
				+ theColumnIndex + "'.");
			return "Incorrect column index.";
		}
	};

	public AttributeTableModel(Table theTable)
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
	public int getColumnCount() { return AttributeTableHeader.values().length; }

	@Override
	public String getColumnName(int theColumnIndex)
	{
		return AttributeTableHeader.getColumnName(theColumnIndex);
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
			if (col == AttributeTableHeader.ATTRIBUTE.columnNr)
				return itsTable.getColumns().get(row).getAttribute().getName();
			else if (col == AttributeTableHeader.TYPE.columnNr)
				return itsTable.getColumns().get(row).getAttribute().getTypeName();
			else if (col == AttributeTableHeader.ENABLED.columnNr)
				return itsTable.getColumns().get(row).getIsEnabled() ? "yes" : "no";
			else if (col == AttributeTableHeader.MISSING.columnNr)
				return itsTable.getColumns().get(row).getMissingValue();
			else
			{
				Log.logCommandLine(
					"Error in AttributeTableModel.getValueAt(): " +
					"invalid index: '" + col + "' for AttributeTableHeader.");
				return null;
			}
		}
	}

//	@Override
//	public Class<?> getColumnClass(int c) { return getValueAt(0, c).getClass(); }

	private void LogError(String theMethod)
	{
		Log.logCommandLine(
			"Error in AttributeTableWindow" + theMethod + ": Table is 'null'.");
	}
/*
	@Override
	public boolean isCellEditable(int row, int col)
	{
		return (col == ColumnHeader.SELECT.columnNr || col == ColumnHeader.TYPE.columnNr);
	}

	public void setValueAt(Object value, int row, int col)
	{
		switch(col)
		{
//			case 0 : itsSelectedAttributes.flip(row); break;
			case 2 : itsTable.getColumns().get(row).setType(((String) value)); break;
		}
		fireTableCellUpdated(row, col);
	}
*/
/*
//	public static BitSet getSelectedAttributes() { return (BitSet)itsSelectedAttributes.clone(); }
	public static void setSelectedAttributes(Selection theSelection)	// TODO will change
	{
		switch (theSelection)
		{
//		case ALL : itsSelectedAttributes.set(0, itsSelectedAttributes.size()); break;
//		case INVERT : itsSelectedAttributes.flip(0, itsSelectedAttributes.size()); break;
//		default : selectType(theSelection);
		}
	}
*/
/*
	public void selectAllType(AttributeType theType, boolean selected)
	{
		for (int i = 0, j = itsTable.getColumns().size(); i < j; ++i)
		{
			if (itsTable.getColumn(i).getType() == theType)
			{
//				if(selected)
//					itsSelectedAttributes.set(i);
//				else
//					itsSelectedAttributes.clear(i);
			}
		}
		// TODO update table/window
		
	}
*/
}

