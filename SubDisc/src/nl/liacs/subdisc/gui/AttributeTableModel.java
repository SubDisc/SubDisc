package nl.liacs.subdisc.gui;

import java.util.BitSet;

import javax.swing.table.AbstractTableModel;

import nl.liacs.subdisc.Table;
import nl.liacs.subdisc.Attribute.AttributeType;

public class AttributeTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;
	public static final AttributeTableModel THE_ONLY_INSTANCE = new AttributeTableModel();

	private static BitSet itsSelectedAttributes;
	private static Table itsTable;

	// TODO change name to avoid confusion with Column class
	public enum ColumnHeader
	{
		SELECT(0),
		ATTRIBUTE(1),
		TYPE(2);

		public final int columnNr;

		private ColumnHeader(int theColumnNr) { columnNr = theColumnNr; }

		public static String getColumnName(int theColumnIndex)
		{
			for(ColumnHeader c : ColumnHeader.values())
				if(c.columnNr == theColumnIndex)
					return c.toString();
			return "Incorrect column index.";
		}
	};

	public enum Selection { ALL, INVERT }	// TODO REMOVE

	private AttributeTableModel() {}

	public AttributeTableModel setup(Table theTable)
	{
		itsTable = theTable;
		itsSelectedAttributes = new BitSet(itsTable.getNrColumns());
		return this;
	}

	@Override
	public int getColumnCount() { return ColumnHeader.values().length; }

	@Override
	public String getColumnName(int theColumnIndex) { return ColumnHeader.getColumnName(theColumnIndex); }

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
	public Class<?> getColumnClass(int c) { return getValueAt(0, c).getClass(); }

	@Override
	public boolean isCellEditable(int row, int col)
	{
		return (col == ColumnHeader.SELECT.columnNr || col == ColumnHeader.TYPE.columnNr);
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

	public static BitSet getSelectedAttributes() { return (BitSet)itsSelectedAttributes.clone(); }
	public static void setSelectedAttributes(Selection theSelection)	// TODO will change
	{
		switch(theSelection)
		{
		case ALL : itsSelectedAttributes.set(0, itsSelectedAttributes.size()); break;
		case INVERT : itsSelectedAttributes.flip(0, itsSelectedAttributes.size()); break;
//		default : selectType(theSelection);
		}
	}

	public static void selectAllType(AttributeType theType, boolean selected)
	{
		for(int i = 0, j = itsTable.getColumns().size(); i < j; ++i)
		{
			if(itsTable.getColumn(i).getType() == theType)
			{
				if(selected)
					itsSelectedAttributes.set(i);
				else
					itsSelectedAttributes.clear(i);
			}
		}
		// TODO update table/window
	}

	
}