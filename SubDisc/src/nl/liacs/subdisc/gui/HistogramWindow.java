package nl.liacs.subdisc.gui;

import java.util.*;
import java.util.Map.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.category.*;

public class HistogramWindow extends JFrame
{
	private static final long serialVersionUID = 1L;

	public HistogramWindow(Table theTable)
	{
		if (theTable == null)
			return;

		add(createHistogram(theTable));

		setTitle("Histogram");
		setIconImage(MiningWindow.ICON);
		setLocation(50, 50);
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	private ChartPanel createHistogram(Table theTable)
	{
		final JFreeChart aChart =
			ChartFactory.createStackedBarChart(
				null, // no title
				null, // no x-axis label
				"Count", // remove?
				createDataset(theTable),
				PlotOrientation.VERTICAL,
				true,
				true,
				false);

		// TODO getPlotAxis: use integer, not floating point, for y-axis
		return new ChartPanel(aChart);
	}

	private CategoryDataset createDataset(Table theTable)
	{
		// TODO add dropBox to select Column
		Column aColumn = null;
		for (Column c : theTable.getColumns())
		{
			if (c.getType() == AttributeType.NOMINAL)
			{
				aColumn = c;
				break;
			}
		}
		if (aColumn == null)
			return null;

		// TODO create (nominal) Column function getDistinctValues
		// recent code update created permanent array for this
		// permanent counts-Map in Column may be to memory demanding
		// and works for Nominal/Binary
		// Numeric is more complicated (could use nr_bins)
		Map<String, Integer> aMap =
			new HashMap<String, Integer>(aColumn.getCardinality());
		for (int i = 0, j = aColumn.size(); i < j; ++i)
		{
			String s = aColumn.getString(i);
			if (!aMap.containsKey(s))
				aMap.put(s, 1);
			else
				aMap.put(s, 1+aMap.get(s).intValue());
		}

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (Entry<String, Integer> e : aMap.entrySet())
			dataset.addValue(e.getValue(), e.getKey(), aColumn.getName());

		return dataset;
	}
}
