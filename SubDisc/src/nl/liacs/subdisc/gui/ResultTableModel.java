package nl.liacs.subdisc.gui;

import java.text.*;
import java.util.*;

import javax.swing.table.*;

import nl.liacs.subdisc.*;

public class ResultTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	private SubgroupSet itsSubgroupSet;

	public ResultTableModel(SubgroupSet theSubgroupSet)
	{
		itsSubgroupSet = theSubgroupSet;
	}

	public int getRowCount()
	{
		return itsSubgroupSet.size();
	}

	public int getColumnCount() { return 6; }

	public String getColumnName(int theColumnIndex)
	{
		switch(theColumnIndex)
		{
			case 0 : return "Nr.";
			case 1 : return "Depth";
			case 2 : return "Coverage";
			case 3 : return "Measure";
			case 4 : return "p-value";
			case 5 : return "Conditions";
			default : return "";
		}
	}

	public Object getValueAt(int theRowIndex, int theColumnIndex)
	{
		Iterator<Subgroup> anIterator = itsSubgroupSet.iterator();

		// Good way to walk trough sorted list?
		for (int i = 0 ; i < theRowIndex; i++)
			anIterator.next();

		// Next Subgroup is the one
		Subgroup aSubgroup = anIterator.next();

		switch(theColumnIndex)
		{
			case 0: return aSubgroup.getID();
			case 1: return aSubgroup.getNrConditions();
			case 2: return aSubgroup.getCoverage();
			case 3: {
						NumberFormat aFormatter = NumberFormat.getNumberInstance();
						aFormatter.setMaximumFractionDigits(6);
						return aFormatter.format(aSubgroup.getMeasureValue());
					}
			case 4: {
//						NumberFormat aFormatter = NumberFormat.getNumberInstance();
//						aFormatter.setMaximumFractionDigits(6);
						double aPValue = aSubgroup.getPValue();
						return ((Double.isNaN(aPValue)) ? "  -" : (float)aPValue);
					}
			case 5: return aSubgroup.getConditions().toString();
			default : return "---";
		}
	}
}
