package nl.liacs.subdisc.gui;

import java.util.BitSet;

import javax.swing.table.AbstractTableModel;

import nl.liacs.subdisc.Table;

public class AttributeTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;
	private BitSet itsSelectedAttributes;
	private Table itsTable;

	// TODO change name to avoid confusion with Column class
	public enum Column
	{
		SELECT(0),
		ATTRIBUTE(1),
		TYPE(2);

		public final int columnNr;

		private Column(int theColumnNr) { columnNr = theColumnNr; }

		public static String getColumnName(int theColumnIndex)
		{
			for(Column c : Column.values())
				if(c.columnNr == theColumnIndex)
					return c.toString();
			return "Incorrect column index.";
		}
	};

	public AttributeTableModel(Table theTable)
	{
		itsTable = theTable;
		itsSelectedAttributes = new BitSet(itsTable.getNrColumns());
	}

	@Override
	public int getColumnCount() { return Column.values().length; }

	@Override
	public String getColumnName(int theColumnIndex) { return Column.getColumnName(theColumnIndex); }

	@Override
	public int getRowCount() { return itsTable.getNrColumns(); }

	@Override
	public Object getValueAt(int row, int col)
	{
		switch(col)
		{
			case 0 : return itsSelectedAttributes.get(row);
			case 1 : return itsTable.getColumns().get(row).getAttribute().getName();
			case 2 : return itsTable.getColumns().get(row).getAttribute().getTypeName();
			default : return "Button_PlaceHolder";
		}
	}

	@Override
	public Class getColumnClass(int c) { return getValueAt(0, c).getClass(); }

	@Override
	public boolean isCellEditable(int row, int col)
	{
		return (col == Column.SELECT.columnNr || col == Column.TYPE.columnNr);
	}

	public void setValueAt(Object value, int row, int col)
	{
		switch(col)
		{
			case 0 : itsSelectedAttributes.flip(row); break;
			case 2 : itsTable.getColumns().get(row).setType(((String) value)); break;
		}
		fireTableCellUpdated(row, col);
	}
}