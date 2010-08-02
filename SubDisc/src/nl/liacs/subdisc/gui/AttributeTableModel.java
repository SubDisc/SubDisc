package nl.liacs.subdisc.gui;

import java.util.BitSet;

import javax.swing.table.AbstractTableModel;

import nl.liacs.subdisc.Table;

public class AttributeTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;
	private static final String[] itsColumnNames = {"Select", "Attribute", "Type", "Set" };
	private BitSet itsSelectedAttributes;
	private Table itsTable;

	public AttributeTableModel(Table theTable)
	{
		itsTable = theTable;
		itsSelectedAttributes = new BitSet(itsTable.getNrColumns());
	}

	@Override
	public int getColumnCount() { return itsColumnNames.length; }

	@Override
	public String getColumnName(int theColumnIndex)
	{
		return itsColumnNames[theColumnIndex];
	}

	@Override
	public int getRowCount() { return itsTable.getNrColumns(); }

	@Override
	public Object getValueAt(int row, int col)
	{
		switch(col)
		{
			case 0 : return itsSelectedAttributes.get(row);
			case 1 : return itsTable.getAttribute(row).getName();
			case 2 : return itsTable.getAttribute(row).getTypeName();
			default : return "Button_PlaceHolder";
		}
	}

	@Override
	public Class getColumnClass(int c) { return getValueAt(0, c).getClass(); }

	@Override
	public boolean isCellEditable(int row, int col)
	{
		return (col == 0 || col == 2);
	}

	public void setValueAt(Object value, int row, int col)
	{
		switch(col)
		{
			case 0 : itsSelectedAttributes.flip(row); break;
			case 2 : itsTable.getAttribute(row).setType(((String) value)); break;
		}
		fireTableCellUpdated(row, col);
	}
}