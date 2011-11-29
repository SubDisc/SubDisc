package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Map.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.category.*;

public class HistogramWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private Table itsTable;
	private JFreeChart itsChart;

	// TODO should be tied to parent window
	public HistogramWindow(Table theTable)
	{
		if (theTable == null)
			return;

		itsTable = theTable;
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(createHistogram(), BorderLayout.CENTER);
		mainPanel.add(createSouthPanel());
		add(mainPanel);

		GUI.focusComponent(itsButton, this);
		setTitle("Histogram");
		setIconImage(MiningWindow.ICON);
		setLocation(50, 50);
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	private ChartPanel createHistogram()
	{
		itsChart =
			ChartFactory.createStackedBarChart(
				null, // no title
				null, // no x-axis label
				"Count", // remove?
				createDataset(null),
				PlotOrientation.VERTICAL,
				true,
				true,
				false);

		// TODO getPlotAxis: use integer, not floating point, for y-axis
		return new ChartPanel(itsChart);
	}

	private CategoryDataset createDataset(Column theColumn)
	{
		if (theColumn == null)
		{
			for (Column c : itsTable.getColumns())
			{
				if (c.getType() == AttributeType.NOMINAL ||
						c.getType() == AttributeType.BINARY)
				{
					theColumn = c;
					break;
				}
			}
			if (theColumn == null)
				return null;
		}

		// permanent counts-Map in Column may be to memory demanding
		// works for Nominal/Binary
		// Numeric is more complicated (could use nr_bins)
		Map<String, Integer> aMap =
			new HashMap<String, Integer>(theColumn.getCardinality());
		// Column.itsDistinctValues TreeSet is alphabetically ordered
		for (String s : theColumn.getDomain())
			aMap.put(s, 0);
		for (int i = 0, j = theColumn.size(); i < j; ++i)
		{
			String s = theColumn.getString(i);
			aMap.put(s, 1+aMap.get(s).intValue());
		}

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		//for (Entry<String, Integer> e : aMap.entrySet())
		//	dataset.addValue(e.getValue(), e.getKey(), aColumn.getName());

		for (Entry<String, Integer> e : aMap.entrySet())
		{
			dataset.addValue(e.getValue()/10, "true", e.getKey());
			dataset.addValue(e.getValue(), "false", e.getKey());
		}

		return dataset;
	}

	JButton itsButton;
	JComboBox itsAttributeColumnsBox;
	JComboBox itsTargetColumnsBox;
	private JPanel createSouthPanel()
	{
		JPanel aPanel = new JPanel();

		// TODO create permanent 'columnName-columnIndex map' in Table?
		// map can also be used directly by MiningWindow/ BrowseWindow
		String[] sa = new String[itsTable.getNrColumns()];
		for (int i = 0, j = itsTable.getNrColumns(); i < j; ++i)
			sa[i] = itsTable.getColumn(i).getName();

		itsAttributeColumnsBox = GUI.buildComboBox(sa, this);
		itsAttributeColumnsBox.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);
		itsAttributeColumnsBox.setActionCommand("attribute");
		aPanel.add(itsAttributeColumnsBox);

		itsTargetColumnsBox = GUI.buildComboBox(sa, this);
		itsTargetColumnsBox.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);
		itsTargetColumnsBox.setActionCommand("target");
		aPanel.add(itsTargetColumnsBox);

		aPanel.add(itsButton = GUI.buildButton("Button", "button", this));

		return aPanel;
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String anEvent = theEvent.getActionCommand();
		System.out.println(anEvent);

		if("button".equals(anEvent))
			;
		else if("attribute".equals(anEvent))
			updateDataset();
		else if("target".equals(anEvent))
			// if attribute==target do nothing
			;
		else if ("close".equals(anEvent))
			dispose();
	}

	private void updateDataset()
	{
		createDataset(itsTable.getColumn(itsAttributeColumnsBox.getSelectedIndex()));
		itsChart.fireChartChanged();
		//getPlot(). = createDataset(itsTable.getColumn(itsAttributeColumnsBox.getSelectedIndex()));
	}
}
