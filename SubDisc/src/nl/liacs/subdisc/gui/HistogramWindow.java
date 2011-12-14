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
	private static final int MIN_NR_BINS = 1; // all data
	private static final int DEFAULT_NR_BINS = 8;
	private static final int MAX_NR_BINS = 32;

	private Table itsTable;
	private ChartPanel itsChartPanel;
	private JComboBox itsAttributeColumnsBox;
	private JComboBox itsTargetColumnsBox;
	private Map<?, Integer> itsAMap;
	private Map<?, Integer> itsTMap;

	// TODO use configurable sliders for Numeric Attribute/Target
	private JSlider itsABins = new JSlider(JSlider.HORIZONTAL, MIN_NR_BINS, MAX_NR_BINS, DEFAULT_NR_BINS);
	private JSlider itsTBins = new JSlider(JSlider.HORIZONTAL, MIN_NR_BINS, MAX_NR_BINS, DEFAULT_NR_BINS);

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
		aSouthPanel.setLayout(new GridLayout(1, 3));
		// TODO create permanent 'columnName-columnIndex map' in Table?
		// map can also be used directly by MiningWindow/ BrowseWindow
		String[] sa = new String[itsTable.getNrColumns()];
		for (int i = 0, j = itsTable.getNrColumns(); i < j; ++i)
			sa[i] = itsTable.getColumn(i).getName();

		// ATTRIBUTE PANEL
		JPanel anAttributePanel = new JPanel();
		anAttributePanel.setLayout(new BoxLayout(anAttributePanel, BoxLayout.Y_AXIS));
		anAttributePanel.setBorder(GUI.buildBorder("Select Attribute"));
		itsAttributeColumnsBox = GUI.buildComboBox(sa, this);
		itsAttributeColumnsBox.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);
		anAttributePanel.add(itsAttributeColumnsBox);
		anAttributePanel.add(itsABins);
		aSouthPanel.add(anAttributePanel);

		// TARGET PANEL (duplicate code)
		JPanel aTargetPanel = new JPanel();
		aTargetPanel.setLayout(new BoxLayout(aTargetPanel, BoxLayout.Y_AXIS));
		aTargetPanel.setBorder(GUI.buildBorder("Select Target"));
		itsTargetColumnsBox = GUI.buildComboBox(sa, this);
		itsTargetColumnsBox.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);
		aTargetPanel.add(itsTargetColumnsBox);
		aTargetPanel.add(itsTBins);
		aSouthPanel.add(aTargetPanel);

		// MISC PANEL (duplicate code)
		JPanel aMiscPanel = new JPanel();
		aMiscPanel.setLayout(new BoxLayout(aMiscPanel, BoxLayout.Y_AXIS));
		aMiscPanel.setBorder(GUI.buildBorder("Other"));
		//aMiscPanel.add(GUI.buildButton("Save", "save", this));
		//aMiscPanel.add(GUI.buildButton("Print", "print", this));
		JButton aButton = GUI.buildButton("Close", "close", this);
		GUI.focusComponent(aButton, this);
		aMiscPanel.add(aButton);
		aSouthPanel.add(aMiscPanel);

		add(aSouthPanel, BorderLayout.SOUTH);

		// MAPS
		updateMap(true);
		updateMap(false);
		// CHART PANEL
		itsChartPanel = new ChartPanel(null);
		updateChartPanel();
		add(new JScrollPane(itsChartPanel), BorderLayout.CENTER);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String anEvent = theEvent.getActionCommand();

		if ("comboBoxChanged".equals(anEvent))
			update(itsAttributeColumnsBox.equals(theEvent.getSource()));
		else if ("close".equals(anEvent))
			dispose();
	}

	// start point for updating
	private void update(boolean isAttributeChanged)
	{
		updateMap(isAttributeChanged);
		updateChartPanel();
	}

	// NOTE BINARY uses String for now, allows for uniform createDataset() code
	// with 4 use cases, instead of 9 (NOMINAL, NUMERIC, BINARY)^2
	private void updateMap(boolean isAttributeChanged)
	{
		JComboBox aBox = isAttributeChanged ? itsAttributeColumnsBox : itsTargetColumnsBox;
		Column aColumn = itsTable.getColumn(aBox.getSelectedIndex());
		AttributeType aType = aColumn.getType();

		if (isAttributeChanged) {
			switch (aType) {
				case NOMINAL : itsAMap = new LinkedHashMap<String, Integer>(); break;
				case ORDINAL : break;	// no use case yet
				case NUMERIC : itsAMap = new LinkedHashMap<Float, Integer>(); break;
				case BINARY : itsAMap = new LinkedHashMap<String, Integer>(); break;
				default : System.out.println("--- ERROR --- HistogramWindow.updateMap()"); return;
			}
		}
		else {
			switch (aType) {
				case NOMINAL : itsTMap = new LinkedHashMap<String, Integer>(); break;
				case ORDINAL : break;	// no use case yet
				case NUMERIC : itsTMap = new LinkedHashMap<Float, Integer>(); break;
				case BINARY : itsTMap = new LinkedHashMap<String, Integer>(); break;
			}
		}

		@SuppressWarnings("unchecked")
		Map<? super Object, Integer> aMap =
			(Map<? super Object, Integer>) (isAttributeChanged ? itsAMap : itsTMap);

		switch (aType) {
			case NOMINAL : {
				// all possible attribute values are stored in an (ordered) Map
				Iterator<String> anIterator = aColumn.getDomain().iterator();
				int idx = -1;
				while (anIterator.hasNext())
					aMap.put(anIterator.next(), ++idx);
				break;
			}
			case ORDINAL: {
				// no use case yet
				break;
			}
			case NUMERIC : {
				safariiHisto(aColumn, aMap, DEFAULT_NR_BINS);
				break;
			}
			case BINARY : {
				aMap.put("0", 0);
				aMap.put("1", 1);
				break;
			}
		}
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

		itsChartPanel.setPreferredSize(new Dimension(aNrColumns*20, 490));
		itsChartPanel.setChart(aChart);
		itsChartPanel.revalidate();
	}

	// relies on up-to-date itsAMap and itsTMap
	private CategoryDataset createDataset()
	{
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		Column a = itsTable.getColumn(itsAttributeColumnsBox.getSelectedIndex());
		Column t = itsTable.getColumn(itsTargetColumnsBox.getSelectedIndex());
		boolean aNumeric = a.isNumericType(); // || a.isOrdinalType();
		boolean tNumeric = t.isNumericType(); // || t.isOrdinalType();
		int[][] counts = new int[itsAMap.size()][itsTMap.size()];

		// 4 use cases
		// XXX NOTE !NUMERIC may mean NOMINAL, ORDINAL or BINARY for now
		// uses getString() instead of getNominal() (which crashes on BINARY)
		// even though that continuously, uselessly, switches on AttributeType
		if (!aNumeric && !tNumeric)
		{
			// loops over all values in Attribute Column
			// for each possible Attribute value the corresponding Target value
			// count is incremented
			// uses aMap and tMap for fast indexing (O(1))
			// (compared to linear array/ TreeSet lookups (O(n)))
			for (int i = 0, j = a.size(); i < j; ++i)
				++counts[itsAMap.get(a.getString(i))][itsTMap.get(t.getString(i))];
		}
		else if (!aNumeric && tNumeric)
		{
			Float[] tBins = itsTMap.keySet().toArray(new Float[0]);

			for (int i = 0, j = a.size(); i < j; ++i) {
				// binarySearch could be more efficient
				int ti = Arrays.binarySearch(tBins, t.getFloat(i));
				++counts[itsAMap.get(a.getString(i))][ti < 0 ? -ti-1: ti];
			}
		}
		else if (aNumeric && !tNumeric)
		{
			Float[] aBins = itsAMap.keySet().toArray(new Float[0]);

			for (int i = 0, j = a.size(); i < j; ++i) {
				int ai = Arrays.binarySearch(aBins, a.getFloat(i));
				++counts[ai < 0 ? -ai-1: ai][itsTMap.get(t.getString(i))];
			}
		}
		else if (aNumeric && tNumeric)
		{
			Float[] aBins = itsAMap.keySet().toArray(new Float[0]);
			Float[] tBins = itsTMap.keySet().toArray(new Float[0]);

			for (int i = 0, j = a.size(); i < j; ++i) {
				int ai = Arrays.binarySearch(aBins, a.getFloat(i));
				int ti = Arrays.binarySearch(tBins, t.getFloat(i));
				++counts[ai < 0 ? -ai-1: ai][ti < 0 ? -ti-1: ti];
			}
		}
		else
			// used ORDINAL?
			System.out.println("--- ERROR --- HistogramWindow.createDataset()");

		for (Entry<?, Integer> ae : itsAMap.entrySet())
			for (Entry<?, Integer> te : itsTMap.entrySet())
				dataset.addValue(counts[ae.getValue()][te.getValue()], te.getKey().toString(), ae.getKey().toString());

		return dataset;
	}

	// NOTE not save for overflow/ NaN
	private void safariiHisto(Column theColumn, Map<? super Object, Integer> theMap, int theNrBins)
	{
		float aSum = 0.0f;
		for (int i = 0, j = theColumn.size(); i < j; ++i)
			aSum += theColumn.getFloat(i);

		float anAvg = aSum / theColumn.size();
		float aStDev = 0.0f;
		for (int i = 0, j = theColumn.size(); i < j; ++i)
			aStDev += Math.pow(anAvg-theColumn.getFloat(i), 2.0);
		aStDev = (float) Math.sqrt(aStDev);

		float aStart = Math.max(anAvg - 2.3f * aStDev, theColumn.getMin());
		float aStop = Math.min(anAvg + 2.3f * aStDev, theColumn.getMax());
		if (aStart == aStop)
		{
			aStart--;
			aStop++;
		}
		float aValue = aStart;
		float aStep = theNrBins > 2 ? (aStop - aStart)/(theNrBins-2) : 0.0f;

		for(int i = 0; i < theNrBins-1; ++i)
			theMap.put(aValue + i*aStep, i);
		theMap.put(Float.POSITIVE_INFINITY, theNrBins-1);
	}

	/*
	 * CODE BELOW IS OBSOLETE NOW
	 */

	// TODO binarySearch also works on Object[], change aBins/tBins to Float[]
	private Float[] createBins(Map<?, Integer> theMap)
	{
		return theMap.keySet().toArray(new Float[0]);
/*
		float[] aBins = new float[theMap.size()];
		Iterator<? super Object> iter = (Iterator<? super Object>) theMap.keySet().iterator();
		for (int i = 0, j = theMap.size(); i < j; ++i)
			aBins[i] = (Float) iter.next();

		return aBins;
 */
	}

	// alternative could be a targetValues Map for each attributeValue
	// Map<AttributeValues, Map<TargetValues, index>> but it would be bigger
	//
	// TODO for numeric attributes all values are now used as separate value
	// should use bins
	private CategoryDataset createDatasetObsolete()
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
		if (a.getType() == AttributeType.NUMERIC && t.getType() == AttributeType.NUMERIC)
			createDatasetForNumeric();
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

	private void createDatasetForNumeric() {
		Column a = itsTable.getColumn(itsAttributeColumnsBox.getSelectedIndex());
		Column t = itsTable.getColumn(itsTargetColumnsBox.getSelectedIndex());

		int DEFAULT_NR_BINS = 8;
		double min = a.getMin();
		double range = (a.getMax()-min) / (DEFAULT_NR_BINS-1);

		double[] aBins = new double[DEFAULT_NR_BINS];
		for (int i = 0, j = DEFAULT_NR_BINS; i < j; ++i)
			aBins[i] = min + (range*i);

		min = t.getMin();
		range = (t.getMax()-min) / (DEFAULT_NR_BINS-1);
		double[] tBins = new double[DEFAULT_NR_BINS];
		for (int i = 0, j = DEFAULT_NR_BINS; i < j; ++i)
			tBins[i] = min + (range*i);

		int[][] counts = new int[DEFAULT_NR_BINS][DEFAULT_NR_BINS];
		for (int i = 0, j = a.size(); i < j; ++i) {
			int ai = Arrays.binarySearch(aBins, a.getFloat(i));
			int ti = Arrays.binarySearch(tBins, t.getFloat(i));
			++counts[ai < 0 ? -ai-1: ai][ti < 0 ? -ti-1: ti];
		}

		// TODO for testing only
		System.out.println("***** BOTH NUMERIC *****");
		System.out.print(Arrays.toString(tBins));
		System.out.println("\t< target values/ attribute values v");
		int i = -1;
		for (int[] ia : counts)
			System.out.println(Arrays.toString(ia) + "\t" + aBins[++i]);
		System.out.println("***** END BOTH NUMERIC *****");
	}
}
