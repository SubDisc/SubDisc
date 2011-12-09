package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Map.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.block.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.title.*;
import org.jfree.data.category.*;
import org.jfree.ui.*;

public class HistogramWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	// do not show legend when target has many values
	private static final int TARGET_VALUES_MAX = 30;
	private static final int ATTRIBUTE_VALUES_MAX = 100;
	// use smaller font if nr columns > ATTRIBUTE_VALUES_MAX
	private static final Font SMALL_FONT = new Font("Dialog", 0, 8);

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
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//pack();
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
		itsChartPanel = new ChartPanel(null);
		updateChartPanel();
		add(new JScrollPane(itsChartPanel), BorderLayout.CENTER);
	}

	private void updateChartPanel()
	{
		final CategoryDataset aDataset = createDataset();

		// TODO create GUI toggle
		boolean showLegend = aDataset.getRowCount() < TARGET_VALUES_MAX;

		final JFreeChart aChart =
			ChartFactory.createStackedBarChart(null, // no title
												null, // no x-axis label
												"", // no y-axis label
												aDataset,
												PlotOrientation.VERTICAL,
												showLegend,
												true,
												false);

		final CategoryPlot aPlot = aChart.getCategoryPlot();
		aPlot.setBackgroundPaint(Color.WHITE);
		aPlot.setRangeGridlinePaint(Color.BLACK);
		aPlot.getDomainAxis().setTickMarkStroke(new BasicStroke(0.1f));
		aPlot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		final BarRenderer aRenderer = (BarRenderer)aPlot.getRenderer();
		aRenderer.setBarPainter(new StandardBarPainter());
		aRenderer.setShadowVisible(false);


		int aNrColumns = aDataset.getColumnCount();
		if (aNrColumns > ATTRIBUTE_VALUES_MAX)
		{
			if (showLegend)
			{
				final LegendTitle aLegend = aChart.getLegend();
				aLegend.setFrame(new LineBorder(Color.BLACK, new BasicStroke(0), new RectangleInsets()));
				aLegend.setItemFont(SMALL_FONT);
			}

			aPlot.setRangeGridlinesVisible(false);
			aPlot.getDomainAxis().setTickLabelsVisible(false);
			//aDomainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
			aPlot.getRangeAxis().setTickLabelFont(SMALL_FONT);
	}

		itsChartPanel.setPreferredSize(new Dimension(aNrColumns*20, 500));
		itsChartPanel.setChart(aChart);
		itsChartPanel.revalidate();
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
		System.out.print(tMap.keySet().toString());
		System.out.println("\t< target values/ attribute values v");
		String[] sa = aMap.keySet().toArray(new String[0]);
		int i = -1;
		for (int[] ia : counts)
			System.out.println(Arrays.toString(ia) + "\t" + sa[++i]);

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
			updateChartPanel();
		else if ("close".equals(anEvent))
			dispose();
	}
}
