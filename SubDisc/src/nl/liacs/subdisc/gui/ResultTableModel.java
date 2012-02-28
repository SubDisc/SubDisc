package nl.liacs.subdisc.gui;

import java.util.*;

import javax.swing.table.*;

import nl.liacs.subdisc.*;

public class ResultTableModel extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	private SubgroupSet itsSubgroupSet;
	private TargetType itsTargetType;

	public ResultTableModel(SubgroupSet theSubgroupSet, TargetType theType)
	{
		itsSubgroupSet = theSubgroupSet;
		itsTargetType = theType;
	}

	@Override
	public int getRowCount()
	{
		return itsSubgroupSet.size();
	}

	@Override
	public int getColumnCount()
	{
		return 8;
	}

	@Override
	public String getColumnName(int theColumnIndex)
	{
		if (itsTargetType == TargetType.SINGLE_NOMINAL)
		{
			if (theColumnIndex == 4)
				return "Prob.";
			else if (theColumnIndex == 5)
				return "Positives";
		}
		else if (itsTargetType == TargetType.SINGLE_NUMERIC)
		{
			if (theColumnIndex == 4)
				return "Average";
			else if (theColumnIndex == 5)
				return "St. Dev.";
		}
		else if (itsTargetType == TargetType.DOUBLE_REGRESSION)
		{
			if (theColumnIndex == 4)
				return "Slope";
			else if (theColumnIndex == 5)
				return "Intercept";
		}
		else if (itsTargetType == TargetType.DOUBLE_CORRELATION)
		{
			if (theColumnIndex == 4)
				return "Correlation";
			else if (theColumnIndex == 5)
				return "Distance";
		}
		else if (itsTargetType == TargetType.MULTI_LABEL)
		{
			if (theColumnIndex == 4)
				return "Edit Dist.";
			else if (theColumnIndex == 5)
				return "Entropy";
		}

		switch(theColumnIndex)
		{
			case 0 : return "Nr.";
			case 1 : return "Depth";
			case 2 : return "Coverage";
			case 3 : return "Quality";
			case 4 : return "-";
			case 5 : return "-";
			case 6 : return "p-Value";
			case 7 : return "Conditions";
			default : return "";
		}
	}

	@Override
	public Object getValueAt(int theRowIndex, int theColumnIndex)
	{
		// will crash if (!anIterator.hasNext())
		// !(0 <= theRowIndex < itsSubgroupSet.size())
		Iterator<Subgroup> anIterator;
		if (theRowIndex <= (itsSubgroupSet.size() / 2))
		{
			anIterator = itsSubgroupSet.iterator();
			for (int i = 0 ; i < theRowIndex; ++i)
				anIterator.next();
		}
		else
		{
			anIterator = itsSubgroupSet.descendingIterator();
			for (int i = itsSubgroupSet.size()-1; i > theRowIndex; --i)
				anIterator.next();
		}
		Subgroup aSubgroup = anIterator.next();

		switch(theColumnIndex)
		{
			case 0: return aSubgroup.getID();
			case 1: return aSubgroup.getNrConditions();
			case 2: return aSubgroup.getCoverage();
			case 3: return RendererNumber.FORMATTER.format(aSubgroup.getMeasureValue());
			case 4: return RendererNumber.FORMATTER.format(aSubgroup.getSecondaryStatistic());
			case 5: return RendererNumber.FORMATTER.format(aSubgroup.getTertiaryStatistic());
			case 6:
			{
				double aPValue = aSubgroup.getPValue();
				return ((Double.isNaN(aPValue)) ? "  -" : (float)aPValue);
			}
			case 7: return aSubgroup.getConditions().toString();
			default : return "---";
		}
	}
}
