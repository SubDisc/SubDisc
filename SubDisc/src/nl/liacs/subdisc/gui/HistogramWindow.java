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
	private JComboBox itsAttributeColumnsBox;
	private JComboBox itsTargetColumnsBox;

	// TODO should be tied to parent window
	// TODO supply TargetColumn if set in MainWindow
	public HistogramWindow(Table theTable)
	{
		if (theTable == null || theTable.getNrColumns() == 0)
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
		setLayout(new BorderLayout());

		// SOUTH PANEL, initialises comboBoxes used by createHistogram
		JPanel aSouthPanel = new JPanel();
		// TODO create permanent 'columnName-columnIndex map' in Table?
		// map can also be used directly by MiningWindow/ BrowseWindow
		String[] sa = new String[itsTable.getNrColumns()];
		for (int i = 0, j = itsTable.getNrColumns(); i < j; ++i)
			sa[i] = itsTable.getColumn(i).getName();

		itsAttributeColumnsBox = GUI.buildComboBox(sa, this);
		itsAttributeColumnsBox.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);
		itsTargetColumnsBox = GUI.buildComboBox(sa, this);
		itsTargetColumnsBox.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);

		aSouthPanel.add(itsAttributeColumnsBox);
		aSouthPanel.add(itsTargetColumnsBox);

		JButton aButton = GUI.buildButton("Close", "close", this);
		aSouthPanel.add(aButton);
		GUI.focusComponent(aButton, this);
		add(aSouthPanel, BorderLayout.SOUTH);

		// CHART PANEL
		itsChartPanel = new ChartPanel(createHistogram());
		add(itsChartPanel, BorderLayout.CENTER);
	}

	// TODO getPlotAxis: use integer, not floating point, for y-axis
	private JFreeChart createHistogram()
	{
		return ChartFactory.createStackedBarChart(null, // no title
													null, // no x-axis label
													null, // no y-axis label
													createDataset(),
													PlotOrientation.VERTICAL,
													true,
													true,
													false);
	}

	// alternative could be a targetValues Map for each attributeValue
	// Map<AttributeValues, Map<TargetValues, index>> but it would be bigger
	//
	// TODO for numeric attributes all values are now used as separate value
	// should use bins
	private CategoryDataset createDataset()
	{
		Column a = itsTable.getColumn(itsAttributeColumnsBox.getSelectedIndex());
		Column t = itsTable.getColumn(itsTargetColumnsBox.getSelectedIndex());

		// although only one map needs to be updated at selection change
		// it would require permanent storage of both aMap and tMap

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
		// (compared to linear array/ TreeSet lookups (O(n)))
		for (int i = 0, j = a.size(); i < j; ++i)
			++counts[aMap.get(a.getString(i))][tMap.get(t.getString(i))];

		// TODO for testing only
		for (int[] ia : counts)
			System.out.println(Arrays.toString(ia));

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (Entry<String, Integer> ae : aMap.entrySet())
			for (Entry<String, Integer> te : tMap.entrySet())
				dataset.addValue(counts[ae.getValue()][te.getValue()], te.getKey(), ae.getKey());

		return dataset;
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String anEvent = theEvent.getActionCommand();

		if ("comboBoxChanged".equals(anEvent))
			itsChartPanel.setChart(createHistogram());
		else if ("close".equals(anEvent))
			dispose();
	}
}
