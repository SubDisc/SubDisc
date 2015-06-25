package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.*;
import org.jfree.data.xy.*;

public class ModelWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	static final String BASE_MODEL_TEXT = "Base Model (all data)";

	private JScrollPane itsJScrollPaneCenter = new JScrollPane();
	private BitSet itsSample = null;
	private final int SAMPLE_SIZE = 200000;
	private Table itsTable;
	private Column itsXColumn;
	private Column itsYColumn;
	private RegressionMeasure itsRM;
	private Subgroup itsSubgroup;
	private JFreeChart itsChart = null;

//	// SINGLE_NUMERIC: show distribution over numeric target and Subgroup =====================================
//	// SCAPE: show distribution of numeric target, for positives and negatives in binary target ===============
//	public ModelWindow(Column theDomain, ProbabilityDensityFunction theDatasetPDF, ProbabilityDensityFunction theSubgroupPDF, String theTitle, boolean isScapeSetting)
//	{
//		initComponents();
//
//		final boolean addSubgroup = (theSubgroupPDF != null);
//
//		XYSeries aDatasetSeries;
//		XYSeries aSubgroupSeries;
//		if (!isScapeSetting)
//		{
//			aDatasetSeries = new XYSeries("dataset");
//			aSubgroupSeries = addSubgroup ? new XYSeries("subgroup") : null;
//		}
//		else
//		{
//			aDatasetSeries = new XYSeries("positives");
//			aSubgroupSeries = addSubgroup ? new XYSeries("negatives") : null;
//		}
//		for (int i = 0, j = theDatasetPDF.size(); i < j; ++i)
//		{
//			aDatasetSeries.add(theDatasetPDF.getMiddle(i), theDatasetPDF.getDensity(i));
//			if (addSubgroup)
//			{
////				float aScale = theSubgroupPDF.getAbsoluteCount()/(float)theDatasetPDF.getAbsoluteCount();
//				float aScale = 1;
//				aSubgroupSeries.add(theSubgroupPDF.getMiddle(i), theSubgroupPDF.getDensity(i)*aScale);
//			}
//		}
//		XYSeriesCollection aDataCollection;
//		if (addSubgroup) // if there is a subgroup, add that one first. Otherwise just add the dataset first
//		{
//			aDataCollection = new XYSeriesCollection(aSubgroupSeries);
//			aDataCollection.addSeries(aDatasetSeries);
//		}
//		else
//			aDataCollection = new XYSeriesCollection(aDatasetSeries);
//
//		JFreeChart aChart =
//			ChartFactory.createXYLineChart("", theDomain.getName(), "density", aDataCollection, PlotOrientation.VERTICAL, false, true, false);
//		aChart.setAntiAlias(true);
//		XYPlot aPlot = aChart.getXYPlot();
//		aPlot.setBackgroundPaint(Color.white);
//		aPlot.setDomainGridlinePaint(Color.gray);
//		aPlot.setRangeGridlinePaint(Color.gray);
//		Paint aDatasetPaint = Color.black;
//		Paint aSubgroupPaint = Color.lightGray;
//		float aDatasetWidth = 1.5f;
//		float aSubgroupWidth = 2.5f;
//		if (isScapeSetting)
//		{
//			aDatasetPaint = Color.red;
//			aSubgroupPaint = Color.blue;
//			aDatasetWidth = 2.5f;
//		}
//		if (addSubgroup)
//		{
//			aPlot.getRenderer().setSeriesPaint(1, aSubgroupPaint);
//			aPlot.getRenderer().setSeriesStroke(1, new BasicStroke(aSubgroupWidth));
//			aPlot.getRenderer().setSeriesPaint(0, aDatasetPaint); //subgroup
//			aPlot.getRenderer().setSeriesStroke(0, new BasicStroke(aDatasetWidth)); //subgroup
//			aChart.addLegend(new LegendTitle(aPlot));
//		}
//		else
//		{
//			aPlot.getRenderer().setSeriesPaint(0, aDatasetPaint);
//			aPlot.getRenderer().setSeriesStroke(0, new BasicStroke(aSubgroupWidth));
//		}
//
//		itsJScrollPaneCenter.setViewportView(new ChartPanel(aChart));
//
//		setTitle(theTitle + ": Numeric Distribution");
//		setIconImage(MiningWindow.ICON);
//		setLocation(50, 50);
//		setSize(GUI.WINDOW_DEFAULT_SIZE);
//		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//		setVisible(true);
//	}
	// FIXME MM same as above --- but with temporary histogram hacked in
	public ModelWindow(Column theDomain, ProbabilityDensityFunction theDatasetPDF, ProbabilityDensityFunction theSubgroupPDF, String theTitle, boolean isScapeSetting)
	{
		initComponents();

		final boolean addSubgroup = (theSubgroupPDF != null);

		XYSeries aDatasetSeries;
		XYSeries aSubgroupSeries;
		if (!isScapeSetting)
		{
			aDatasetSeries = new XYSeries("complement");	// XXX MM complement not dataset
			aSubgroupSeries = addSubgroup ? new XYSeries("subgroup") : null;
		}
		else
		{
			aDatasetSeries = new XYSeries("positives");
			aSubgroupSeries = addSubgroup ? new XYSeries("negatives") : null;
		}
		for (int i = 0, j = theDatasetPDF.size(); i < j; ++i)
		{
			aDatasetSeries.add(theDatasetPDF.getMiddle(i), theDatasetPDF.getDensity(i));
			if (addSubgroup)
			{
//				float aScale = theSubgroupPDF.getAbsoluteCount()/(float)theDatasetPDF.getAbsoluteCount();
				float aScale = 1;
				aSubgroupSeries.add(theSubgroupPDF.getMiddle(i), theSubgroupPDF.getDensity(i)*aScale);
			}
		}
		XYSeriesCollection aDataCollection;
		if (addSubgroup) // if there is a subgroup, add that one first. Otherwise just add the dataset first
		{
			aDataCollection = new XYSeriesCollection(aSubgroupSeries);
			aDataCollection.addSeries(aDatasetSeries);
		}
		else
			aDataCollection = new XYSeriesCollection(aDatasetSeries);

		JFreeChart aChart =
			ChartFactory.createXYLineChart("", theDomain.getName(), "density", aDataCollection, PlotOrientation.VERTICAL, false, true, false);
		aChart.setAntiAlias(true);
		XYPlot aPlot = aChart.getXYPlot();
		aPlot.setBackgroundPaint(Color.white);
		aPlot.setDomainGridlinePaint(Color.gray);
		aPlot.setRangeGridlinePaint(Color.gray);
		Paint aDatasetPaint = Color.black;
		Paint aSubgroupPaint = Color.lightGray;
		float aDatasetWidth = 1.5f;
		float aSubgroupWidth = 2.5f;
		if (isScapeSetting)
		{
			aDatasetPaint = Color.red;
			aSubgroupPaint = Color.blue;
			aDatasetWidth = 2.5f;
		}
		if (addSubgroup)
		{
			aPlot.getRenderer().setSeriesPaint(1, aSubgroupPaint);
			aPlot.getRenderer().setSeriesStroke(1, new BasicStroke(aSubgroupWidth));
			aPlot.getRenderer().setSeriesPaint(0, aDatasetPaint); //subgroup
			aPlot.getRenderer().setSeriesStroke(0, new BasicStroke(aDatasetWidth)); //subgroup
			aChart.addLegend(new LegendTitle(aPlot));
		}
		else
		{
			aPlot.getRenderer().setSeriesPaint(0, aDatasetPaint);
			aPlot.getRenderer().setSeriesStroke(0, new BasicStroke(aSubgroupWidth));
		}

		itsJScrollPaneCenter.setViewportView(new ChartPanel(aChart));

		setTitle(theTitle + ": Numeric Distribution");
		setIconImage(MiningWindow.ICON);
		setLocation(50, 50);
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	// DOUBLE_CORRELATION and DOUBLE_REGRESSION ============================

	//TODO There should never be this much code in a constructor
	public ModelWindow(Table theTable, Column theXColumn, Column theYColumn, RegressionMeasure theRM, Subgroup theSubgroup)
	{
		itsTable = theTable;
		itsXColumn = theXColumn;
		itsYColumn = theYColumn;
		itsRM = theRM;
		itsSubgroup = theSubgroup;

		//sampling
		int aSize = theTable.getNrRows();
		if (aSize > SAMPLE_SIZE)
			itsSample = theTable.getRandomSubgroupMembers(SAMPLE_SIZE);

		initComponents();
		final boolean isRegression = (theRM != null);
		final boolean forSubgroup = (theSubgroup != null);

		String aName;
		if (isRegression)
			aName = String.format("%s = %f + %f * %s", theYColumn.getName(), (float) theRM.getIntercept(), (float) theRM.getSlope(), theXColumn.getName());
		else
		{
			if (forSubgroup)
				aName = String.format("2D distribution (r = %f)", (float) theSubgroup.getSecondaryStatistic());
			else
				aName = "2D distribution";
		}
		if (forSubgroup)
			aName += "\n(" + theSubgroup.toString() + ", " + theSubgroup.getCoverage() + " examples)";
		else
			aName += " (all data)";

		// create the chart
		XYSeriesCollection aDataSet = getDataPoints();
		itsChart = ChartFactory.createScatterPlot(aName, theXColumn.getName(), theYColumn.getName(), aDataSet, PlotOrientation.VERTICAL, false, true, false);
		itsChart.setAntiAlias(true);
		itsChart.getTitle().setFont(new Font("title", Font.BOLD, 12));

		XYPlot aPlot = itsChart.getXYPlot();
		aPlot.setBackgroundPaint(Color.white);
		aPlot.setDomainGridlinePaint(Color.gray);
		aPlot.setRangeGridlinePaint(Color.gray);
		aPlot.getRenderer().setSeriesPaint(0, Color.black);
		aPlot.getRenderer().setSeriesShape(0, new Rectangle2D.Float(-1.25f, -1.25f, 1.25f, 1.25f));
		if (forSubgroup) //if subgroup is also shown, make remainder gray
		{
			aPlot.getRenderer().setSeriesPaint(1, Color.lightGray);
			aPlot.getRenderer().setSeriesShape(1, new Rectangle2D.Float(-1.25f, -1.25f, 1.25f, 1.25f));
		}
		else
		{
			aPlot.getRenderer().setSeriesPaint(1, Color.black);
			aPlot.getRenderer().setSeriesShape(1, new Rectangle2D.Float(-1.25f, -1.25f, 1.25f, 1.25f));
		}

		//line
		if (isRegression)
		{
			StandardXYItemRenderer aLineRenderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
			aDataSet = new XYSeriesCollection(); // ?
			XYSeries aSeries = new XYSeries("line");
			aSeries.add(theXColumn.getMin(), theRM.getBaseFunctionValue(theXColumn.getMin()));
			aSeries.add(theXColumn.getMax(), theRM.getBaseFunctionValue(theXColumn.getMax()));
			aDataSet.addSeries(aSeries); //add second series to represent line
			aPlot.setDataset(1, aDataSet);
			aPlot.setRenderer(1, aLineRenderer);
			aLineRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
		}

		itsJScrollPaneCenter.setViewportView(new ChartPanel(itsChart));

		String aWho = forSubgroup ? ResultWindow.createTitle(theSubgroup) : BASE_MODEL_TEXT;
		String aType = isRegression ? "Regression" : "Correlation";
		String aSample = (itsSample == null) ? "" : " (sampled)";
		String aTitle = String.format("%s: %s%s", aWho, aType, aSample);

		setTitle(aTitle);
		setIconImage(MiningWindow.ICON);
		setLocation(50, 50);
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	public XYSeriesCollection getDataPoints()
	{
		XYSeries aSeries = new XYSeries("dataset");
		XYSeries aSubgroupSeries = new XYSeries("subgroup");

		//if i is a member of the specified subgroup
		if (itsSubgroup != null)
		{
			BitSet aMembers = itsSubgroup.getMembers();
			if (itsSample != null)
				aMembers.and(itsSample);
			for (int i = 0, j = itsXColumn.size(); i < j; ++i)
				if (aMembers.get(i))
					aSubgroupSeries.add(itsXColumn.getFloat(i), itsYColumn.getFloat(i));
		}
		XYSeriesCollection aDataSet = new XYSeriesCollection(aSubgroupSeries);

		//complete database
		if (itsSample == null)
			for (int i = 0, j = itsXColumn.size(); i < j; ++i)
				aSeries.add(itsXColumn.getFloat(i), itsYColumn.getFloat(i));
		else //only show a sample
		{
			Log.logCommandLine("Sampling before plotting dataset.");
			for (int i = 0, j = itsXColumn.size(); i < j; ++i)
				if (itsSample.get(i))
					aSeries.add(itsXColumn.getFloat(i), itsYColumn.getFloat(i));
		}
		aDataSet.addSeries(aSeries);
		return aDataSet;
	}



	// MULTI_LABEL: show Subgroup induced DAG ==============================

	public ModelWindow(DAG theDAG, int theDAGWidth, int theDAGHeight)
	{
		initComponents();
		DAGView aDAGView = new DAGView(theDAG);
		aDAGView.setDAGArea(theDAGWidth, theDAGHeight);
		aDAGView.drawDAG();
		itsJScrollPaneCenter.setViewportView(aDAGView);

		setTitle(BASE_MODEL_TEXT + ": Bayesian Network");
		setIconImage(MiningWindow.ICON);
		setLocation(0, 0);
		setSize(theDAGWidth, theDAGHeight);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	private void initComponents()
	{
		JPanel aPanel = new JPanel();

		if (itsSample != null)
			aPanel.add(GUI.buildButton("Resample", 'R', "resample", this));
		aPanel.add(GUI.buildButton("Close", 'C', "close", this));
		getContentPane().add(itsJScrollPaneCenter, BorderLayout.CENTER);
		getContentPane().add(aPanel, BorderLayout.SOUTH);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		if ("close".equals(theEvent.getActionCommand()))
			dispose();
		else if ("resample".equals(theEvent.getActionCommand()))
		{
			Log.logCommandLine("attempting to resample");
			XYSeriesCollection aDataSet = getDataPoints();
			itsChart.getXYPlot().setDataset(aDataSet);
		}
	}
}
