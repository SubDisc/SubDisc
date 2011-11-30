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
	private ChartPanel itsChartPanel;
	private JButton itsButton;
	private JComboBox itsAttributeColumnsBox;
	private JComboBox itsTargetColumnsBox;

	// TODO should be tied to parent window
	public HistogramWindow(Table theTable)
	{
		if (theTable == null)
			return;

		itsTable = theTable;

		initComponents();

		setTitle("Histogram");
		setIconImage(MiningWindow.ICON);
		setLocation(50, 50);
		//setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);
	}

	private void initComponents()
	{
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		itsChartPanel = new ChartPanel(null);
		createHistogram(null);
		mainPanel.add(itsChartPanel, BorderLayout.CENTER);
		mainPanel.add(createSouthPanel(), BorderLayout.SOUTH);
		add(mainPanel);

		GUI.focusComponent(itsButton, this);
	}

	private void createHistogram(Column theColumn)
	{
		JFreeChart aChart =
			ChartFactory.createStackedBarChart(
				null, // no title
				null, // no x-axis label
				"Count", // remove?
				createDataset(theColumn, null),
				PlotOrientation.VERTICAL,
				true,
				true,
				false);

		// TODO getPlotAxis: use integer, not floating point, for y-axis
		itsChartPanel.setChart(aChart);
	}

	private CategoryDataset createDataset(Column theColumn, Column theTargetColumn)
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
			new LinkedHashMap<String, Integer>(theColumn.getCardinality());
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
			//dataset.addValue(e.getValue()/10, "true", e.getKey());
			dataset.addValue(e.getValue(), "false", e.getKey());
		}

		return dataset;
	}

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
		else if("attribute".equals(anEvent)) {
			createHistogram(itsTable.getColumn(itsAttributeColumnsBox.getSelectedIndex()));
			count();
		}
		else if("target".equals(anEvent))
			// if attribute==target do nothing
			count();
		else if ("close".equals(anEvent))
			dispose();
	}

	// alternative could be a targetValues Map for each attributeValue
	// Map<AttributeValues, Map<TargetValues, index>> but it would be bigger
	//
	// TODO for numeric attributes all values are now used as separate value
	// should use bins
	private void count()
	{
		Column a = itsTable.getColumn(itsAttributeColumnsBox.getSelectedIndex());
		Column t = itsTable.getColumn(itsTargetColumnsBox.getSelectedIndex());

		//if (a == t)
		//	return;

		// although only one map needs to be updated at selection change
		// it would requires permanent storage of both aMap and tMap

		// all possible attribute values are stored in an (ordered) Map
		Map<String, Integer> aMap = new LinkedHashMap<String, Integer>();
		Iterator<String> anIterator = a.getDomain().iterator();
		int idx = -1;
		while (anIterator.hasNext())
			aMap.put(anIterator.next(), ++idx);

		// all possible target values are stored in an (ordered) Map
		Map<String, Integer> tMap = new LinkedHashMap<String, Integer>();
		anIterator = t.getDomain().iterator();
		idx = -1;
		while (anIterator.hasNext())
			tMap.put(anIterator.next(), ++idx);

		int[][] counts = new int[aMap.size()][tMap.size()];

		// loops over all values in Attribute Column
		// for each possible Attribute value the corresponding Target value
		// count is incremented
		// uses aMap and tMap for fast indexing (O(1))
		// (compared to linear array/ TreeSet lookups (O(l)))
		for (int i = 0, j = a.size(); i < j; ++i)
			++counts[aMap.get(a.getString(i))][tMap.get(t.getString(i))];

		// TODO for testing only
		for (int[] ia : counts)
			System.out.println(Arrays.toString(ia));
	}
}
