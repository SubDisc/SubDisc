package nl.liacs.subdisc.gui;

import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

// FIXME MM: only used by BrowseJTable, make it a static member of that class
public class RowFilterBitSet extends RowFilter<AbstractTableModel, Integer>
{
	private final BitSet MEMBERS;

	public RowFilterBitSet(BitSet theMembers)
	{
		MEMBERS = theMembers;
	}

	@Override
	public boolean include(Entry<? extends AbstractTableModel, ? extends Integer> entry)
	{
		// if no BitSet is given, include all by default
		return (MEMBERS == null ? true :
					MEMBERS.get(entry.getIdentifier()));
	}
}