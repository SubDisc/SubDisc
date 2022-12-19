package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.event.*;

import nl.liacs.subdisc.*;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.block.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.title.*;
import org.jfree.data.category.*;
import org.jfree.chart.ui.*;

public class HistogramWindow extends JFrame implements ActionListener, ChangeListener
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
	private JComboBox<String> itsAttributeColumnsBox;
	private JComboBox<String> itsTargetColumnsBox;
	private Map<?, Integer> itsAMap;
	private Map<?, Integer> itsTMap;
	private JButton itsAttributePlotButton;
	private JButton itsTargetPlotButton;

	// TODO use configurable sliders for Numeric Attribute/Target
	private JSlider itsAttributeBinsSlider;
	private JSlider itsTargetBinsSlider;

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
		setupSlider(itsAttributeBinsSlider = new JSlider());
		anAttributePanel.add(itsAttributeBinsSlider);
		itsAttributePlotButton = GUI.buildButton("Plot Attribute", 'A', "attributeplot", this);
		anAttributePanel.add(itsAttributePlotButton);
		aSouthPanel.add(anAttributePanel);

		// TARGET PANEL (duplicate code)
		JPanel aTargetPanel = new JPanel();
		aTargetPanel.setLayout(new BoxLayout(aTargetPanel, BoxLayout.Y_AXIS));
		aTargetPanel.setBorder(GUI.buildBorder("Select Target"));
		itsTargetColumnsBox = GUI.buildComboBox(sa, this);
		itsTargetColumnsBox.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);
		aTargetPanel.add(itsTargetColumnsBox);
		setupSlider(itsTargetBinsSlider = new JSlider());
		aTargetPanel.add(itsTargetBinsSlider);
		itsTargetPlotButton = GUI.buildButton("Plot Target", 'T', "targetplot", this);
		aTargetPanel.add(itsTargetPlotButton);
		aSouthPanel.add(aTargetPanel);

		// MISC PANEL
		JPanel aMiscPanel = new JPanel();
		aMiscPanel.setLayout(new BoxLayout(aMiscPanel, BoxLayout.Y_AXIS));
		aMiscPanel.setBorder(GUI.buildBorder("Other"));
		//aMiscPanel.add(GUI.buildButton("Save", "save", this));
		//aMiscPanel.add(GUI.buildButton("Print", "print", this));
		aMiscPanel.add(GUI.buildButton("CrossTable", 'R', "crosstable", this));

		JButton aButton = GUI.buildButton("Close", 'C', "close", this);
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

	private void setupSlider(JSlider theSlider) 
	{
		theSlider.setMinimum(MIN_NR_BINS);
		theSlider.setMaximum(MAX_NR_BINS);
		theSlider.setValue(DEFAULT_NR_BINS);
		//theSlider.setMajorTickSpacing(8);
		theSlider.setMinorTickSpacing(1);
		theSlider.setPaintTicks(true);
		theSlider.setPaintLabels(true);
		// labels (within range of MIN-MAX)
		Hashtable<Integer, JLabel> aTable = new Hashtable<Integer, JLabel>();
		aTable.put(Integer.valueOf(1), new JLabel(String.valueOf(1)));
		aTable.put(Integer.valueOf(8), new JLabel(String.valueOf(8)));
		aTable.put(Integer.valueOf(16), new JLabel(String.valueOf(16)));
		aTable.put(Integer.valueOf(24), new JLabel(String.valueOf(24)));
		aTable.put(Integer.valueOf(32), new JLabel(String.valueOf(32)));
		theSlider.setLabelTable(aTable);
		theSlider.addChangeListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String anEvent = theEvent.getActionCommand();

		if ("comboBoxChanged".equals(anEvent))
			update(itsAttributeColumnsBox.equals(theEvent.getSource()));
		else if ("crosstable".equals(anEvent))
			new CrossTableWindow(((CategoryPlot)itsChartPanel.getChart().getPlot()).getDataset());
		else if ("attributeplot".equals(anEvent))
			new PlotWindow(itsTable.getColumn(itsAttributeColumnsBox.getSelectedIndex()));
		else if ("targetplot".equals(anEvent))
			new PlotWindow(itsTable.getColumn(itsTargetColumnsBox.getSelectedIndex()));
		else if ("close".equals(anEvent))
			dispose();
	}

	@Override
	public void stateChanged(ChangeEvent theEvent)
	{
		if (!((JSlider)theEvent.getSource()).getValueIsAdjusting())
			// assumes only 2 sources exist (Attribute and Target slider)
			update(theEvent.getSource() == itsAttributeBinsSlider);
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
		JComboBox<String> aBox = isAttributeChanged ? itsAttributeColumnsBox : itsTargetColumnsBox;
		Column aColumn = itsTable.getColumn(aBox.getSelectedIndex());
		AttributeType aType = aColumn.getType();

		if (isAttributeChanged) 
		{
			itsAttributeBinsSlider.setEnabled(aType == AttributeType.NUMERIC);
			itsAttributePlotButton.setEnabled(aType == AttributeType.NUMERIC);

			switch (aType) 
			{
				case NOMINAL : itsAMap = new LinkedHashMap<String, Integer>(); break;
				case ORDINAL : break;	// no use case yet
				case NUMERIC : itsAMap = new LinkedHashMap<Float, Integer>(); break;
				case BINARY : itsAMap = new LinkedHashMap<Boolean, Integer>(); break;
				default : {
					unknownAttributeType("updateMap", aType);
					return;
				}
			}
		}
		else 
		{
			itsTargetBinsSlider.setEnabled(aType == AttributeType.NUMERIC);
			itsTargetPlotButton.setEnabled(aType == AttributeType.NUMERIC);

			switch (aType) 
			{
				case NOMINAL : itsTMap = new LinkedHashMap<String, Integer>(); break;
				case ORDINAL : break;	// no use case yet
				case NUMERIC : itsTMap = new LinkedHashMap<Float, Integer>(); break;
				case BINARY : itsTMap = new LinkedHashMap<Boolean, Integer>(); break;
				default : {
					unknownAttributeType("updateMap", aType);
					return;
				}
			}
		}

		@SuppressWarnings("unchecked")
		Map<? super Object, Integer> aMap = (Map<? super Object, Integer>) (isAttributeChanged ? itsAMap : itsTMap);

		switch (aType) 
		{
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
				safariiHisto(aColumn, aMap);
				break;
			}
			case BINARY : {
				// NOTE map is not used for count-mapping in createDataset
				// but order [0, 1] is still essential
				aMap.put(Boolean.FALSE, 0);
				aMap.put(Boolean.TRUE, 1);
				break;
			}
			default : {
				unknownAttributeType("updateMap", aType);
				return;
			}
		}
	}

	private void updateChartPanel()
	{
		final CategoryDataset aDataset = createDataset();

		// TODO create GUI toggle
		boolean showLegend = aDataset.getRowCount() < TARGET_VALUES_MAX;

		final JFreeChart aChart = ChartFactory.createStackedBarChart(null, // no title
										null, // no x-axis label
										"", // no y-axis label
										aDataset, PlotOrientation.VERTICAL, showLegend,	true, false);

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

		itsChartPanel.setPreferredSize(new Dimension(aNrColumns*20, 450));
		itsChartPanel.setChart(aChart);
		itsChartPanel.revalidate();
	}

	// relies on up-to-date itsAMap and itsTMap
	private CategoryDataset createDataset()
	{
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		Column a = itsTable.getColumn(itsAttributeColumnsBox.getSelectedIndex());
		Column t = itsTable.getColumn(itsTargetColumnsBox.getSelectedIndex());
		int[][] counts = new int[itsAMap.size()][itsTMap.size()];

		// 16 possibilities (NOMINAL, ORDINAL, NUMERIC, BINARY)^2
		switch (a.getType()) 
		{
			// Attribute NOMINAL
			case NOMINAL : 
			{
				switch (t.getType()) 
				{
					case NOMINAL : {
						// loops over all values in Attribute Column
						// for each possible Attribute value the corresponding Target value
						// count is incremented
						// uses aMap and tMap for fast indexing (O(1))
						// (compared to linear array/ TreeSet lookups (O(n)))
						for (int i = 0, j = a.size(); i < j; ++i)
							++counts[itsAMap.get(a.getNominal(i))][itsTMap.get(t.getNominal(i))];
						break;
					}
					case ORDINAL : {
						unknownAttributeType("createDataset", t.getType());
						break;
					}
					case NUMERIC : 
					{
						Float[] tBins = itsTMap.keySet().toArray(new Float[0]);

						for (int i = 0, j = a.size(); i < j; ++i)
							if (!a.getMissing(i) && !t.getMissing(i))
							{
								// binarySearch could be more efficient
								int ti = Arrays.binarySearch(tBins, t.getFloat(i));
								++counts[itsAMap.get(a.getNominal(i))][ti < 0 ? -ti-1: ti];
							}
						break;
					}
					case BINARY : {
						for (int i = 0, j = a.size(); i < j; ++i)
							// NOTE order of Boolean map should be [0, 1]
							++counts[itsAMap.get(a.getNominal(i))][t.getBinary(i) ? 1 : 0];
						break;
					}
					default : {
						unknownAttributeType("createDataset", t.getType());
						break;
					}
				}
				break;
			}
			// Attribute ORDINAL
			case ORDINAL : 
			{
				unknownAttributeType("createDataset", a.getType());
				break;
			}
			// Attribute NUMERIC
			case NUMERIC : 
			{
				Float[] aBins = itsAMap.keySet().toArray(new Float[0]);

				switch (t.getType()) 
				{
					case NOMINAL : 
					{
						for (int i = 0, j = a.size(); i < j; ++i) 
							if (!a.getMissing(i) && !t.getMissing(i))
							{
								int ai = Arrays.binarySearch(aBins, a.getFloat(i));
								++counts[ai < 0 ? -ai-1: ai][itsTMap.get(t.getNominal(i))];
							}
						break;
					}
					case ORDINAL : {
						unknownAttributeType("createDataset", t.getType());
						break;
					}
					case NUMERIC : 
					{
						Float[] tBins = itsTMap.keySet().toArray(new Float[0]);

						for (int i = 0, j = a.size(); i < j; ++i)
							if (!a.getMissing(i) && !t.getMissing(i))
							{
								int ai = Arrays.binarySearch(aBins, a.getFloat(i));
								int ti = Arrays.binarySearch(tBins, t.getFloat(i));
								++counts[ai < 0 ? -ai-1: ai][ti < 0 ? -ti-1: ti];
							}
						break;
					}
					case BINARY : {
						for (int i = 0, j = a.size(); i < j; ++i) {
							int ai = Arrays.binarySearch(aBins, a.getFloat(i));
							++counts[ai < 0 ? -ai-1: ai][t.getBinary(i) ? 1 : 0];
						}
						break;
					}
					default : {
						unknownAttributeType("createDataset", t.getType());
						break;
					}
				}
				break;
			}
			// Attribute BINARY
			case BINARY : 
			{
				switch (t.getType()) 
				{
					case NOMINAL : {
						for (int i = 0, j = a.size(); i < j; ++i)
							++counts[a.getBinary(i) ? 1 : 0][itsTMap.get(t.getNominal(i))];
						break;
					}
					case ORDINAL : {
						unknownAttributeType("createDataset", t.getType());
						break;
					}
					case NUMERIC : {
						Float[] tBins = itsTMap.keySet().toArray(new Float[0]);

						for (int i = 0, j = a.size(); i < j; ++i) {
							// binarySearch could be more efficient
							int ti = Arrays.binarySearch(tBins, t.getFloat(i));
							++counts[a.getBinary(i) ? 1 : 0][ti < 0 ? -ti-1: ti];
						}
						break;
					}
					case BINARY : {
						for (int i = 0, j = a.size(); i < j; ++i)
							++counts[a.getBinary(i) ? 1 : 0][t.getBinary(i) ? 1 : 0];
						break;
					}
					default : {
						unknownAttributeType("createDataset", t.getType());
						break;
					}
				}
				break;
			}
			// Attribute unknown
			default : {
				unknownAttributeType("createDataset", a.getType());
				break;
			}
		}

		for (Entry<?, Integer> ae : itsAMap.entrySet())
			for (Entry<?, Integer> te : itsTMap.entrySet())
			{
				if (ae.getValue() < itsAMap.size() && te.getValue() < itsTMap.size())
					dataset.addValue(counts[ae.getValue()][te.getValue()], te.getKey().toString(), ae.getKey().toString());
				else
				{
					System.out.println("data outside matrix! " + ae.getValue());
					System.out.println("data outside matrix! " + te.getValue());
					Thread.dumpStack();
				}
			}

		return dataset;
	}

	private void unknownAttributeType(String theSource, AttributeType theType) 
	{
		Log.logCommandLine(String.format("%s.%s(), unknown AttributeType: '%s'", getClass().getSimpleName(), theSource,	theType));
	}

	// NOTE not save for overflow/ NaN
	private void safariiHisto(Column theColumn, Map<? super Object, Integer> theMap)
	{
		// assumes only 2 maps exist (Attribute and Target map)
		int aNrBins = (theMap == itsAMap ? itsAttributeBinsSlider.getValue() : itsTargetBinsSlider.getValue());

		float aSum = 0.0f;
		int aCount = 0;
		for (int i = 0, j = theColumn.size(); i < j; ++i)
			if (!theColumn. getMissing(i))
			{
				aSum += theColumn.getFloat(i);
				aCount++;
			}
		float anAvg = (aCount > 0) ? aSum/aCount : 0;

		float aStDev = 0.0f;
		for (int i = 0, j = theColumn.size(); i < j; ++i)
			if (!theColumn. getMissing(i))
				aStDev += Math.pow(anAvg-theColumn.getFloat(i), 2.0);
		aStDev = (float) Math.sqrt(aStDev);

		float aStart = Math.max(anAvg - 2.3f * aStDev, theColumn.getMin());
		float aStop = Math.min(anAvg + 2.3f * aStDev, theColumn.getMax());

		if (aStart == aStop)
		{
			aStart--;
			aStop++;
		}
		float aStep = aNrBins > 2 ? (aStop - aStart)/(aNrBins-2) : 0.0f;

		for(int i = 0; i < aNrBins-1; ++i)
			theMap.put(aStart + i*aStep, i);
		theMap.put(Float.POSITIVE_INFINITY, aNrBins-1);
	}
}
