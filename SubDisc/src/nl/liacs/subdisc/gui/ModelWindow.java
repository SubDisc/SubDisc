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
import org.jfree.data.xy.*;

public class ModelWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private JScrollPane itsJScrollPaneCenter = new JScrollPane();

	// SINGLE_NUMERIC: show distribution over numeric target

	public ModelWindow(Column theDomain, ProbabilityDensityFunction thePDF)
	{
		initComponents();
		String aName = "probability density function";

		XYSeries aSeries = new XYSeries("data");
		for (int i = 0, j = thePDF.size(); i < j; ++i)
			aSeries.add(thePDF.getMiddle(i), thePDF.getDensity(i));
		XYSeriesCollection aDataSet = new XYSeriesCollection(aSeries);
		JFreeChart aChart =
			ChartFactory.createXYLineChart(aName, theDomain.getName(), "density", aDataSet, PlotOrientation.VERTICAL, false, true, false);
		aChart.setAntiAlias(true);
		XYPlot plot = aChart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.gray);
		plot.setRangeGridlinePaint(Color.gray);
		plot.getRenderer().setSeriesPaint(0, Color.black);
		plot.getRenderer().setSeriesShape(0, new Rectangle2D.Float(0.0f, 0.0f, 2.5f, 2.5f));

		itsJScrollPaneCenter.setViewportView(new ChartPanel(aChart));

		setTitle("Base Model");
		setIconImage(MiningWindow.ICON);
		setLocation(50, 50);
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	//correlation and regression ===============================

	//TODO There should never be this much code in a constructor
	public ModelWindow(Column theXColumn, Column theYColumn, RegressionMeasure theRM, Subgroup theSubgroup)
	{
		initComponents();
		String aName = (theRM == null ? "2D distribution" :
						String.format("y = %f + %f * x",
								(float) theRM.getIntercept(),
								(float) theRM.getSlope()));

		//data
		BitSet aMembers = (theSubgroup == null) ? null : theSubgroup.getMembers();
		XYSeries aSeries = new XYSeries("data");

		//if complete database
		if (aMembers == null)
			for (int i = 0, j = theXColumn.size(); i < j; ++i)
				aSeries.add(theXColumn.getFloat(i), theYColumn.getFloat(i));
		//if i is a member of the specified subgroup
		else
			for (int i = 0, j = theXColumn.size(); i < j; ++i)
				if (aMembers.get(i))
					aSeries.add(theXColumn.getFloat(i), theYColumn.getFloat(i));
		XYSeriesCollection aDataSet = new XYSeriesCollection(aSeries);

		// create the chart
		JFreeChart aChart =
			ChartFactory.createScatterPlot(aName, theXColumn.getName(), theYColumn.getName(), aDataSet, PlotOrientation.VERTICAL, false, true, false);
		aChart.setAntiAlias(true);
		XYPlot plot = aChart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.gray);
		plot.setRangeGridlinePaint(Color.gray);
		plot.getRenderer().setSeriesPaint(0, Color.black);
		plot.getRenderer().setSeriesShape(0, new Rectangle2D.Float(0.0f, 0.0f, 2.5f, 2.5f));

		//line
		if (theRM != null)
		{
			StandardXYItemRenderer aLineRenderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
			aDataSet = new XYSeriesCollection(); // ?
			aSeries = new XYSeries("line");
			aSeries.add(theXColumn.getMin(), theRM.getBaseFunctionValue(theXColumn.getMin()));
			aSeries.add(theXColumn.getMax(), theRM.getBaseFunctionValue(theXColumn.getMax()));
			aDataSet.addSeries(aSeries); //add second series to represent line
			plot.setDataset(1, aDataSet);
			plot.setRenderer(1, aLineRenderer);
			aLineRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
		}

		itsJScrollPaneCenter.setViewportView(new ChartPanel(aChart));

		setTitle("Base Model");
		setIconImage(MiningWindow.ICON);
		setLocation(50, 50);
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	//DAG ==================================================

	public ModelWindow(DAG theDAG, int theDAGWidth, int theDAGHeight)
	{
		initComponents();
		DAGView aDAGView = new DAGView(theDAG);
		aDAGView.setDAGArea(theDAGWidth, theDAGHeight);
		aDAGView.drawDAG();
		itsJScrollPaneCenter.setViewportView(aDAGView);

		setTitle("Base Model: Bayesian Network");
		setIconImage(MiningWindow.ICON);
		setLocation(0, 0);
		setSize(theDAGWidth, theDAGHeight);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}
/*
	//never used yet
	//create window with the same layout of nodes as the one in theDAGView
	public ModelWindow(DAG theDAG, int theDAGWidth, int theDAGHeight, DAGView theDAGView)
	{
		initComponents();
		itsDAGView = new DAGView(theDAG);
		itsDAGView.setDAGArea(theDAGWidth, theDAGHeight);
		itsDAGView.drawDAG(theDAGView);
		jScrollPaneCenter.setViewportView(itsDAGView);
//		setIconImage(MiningWindow.ICON);
		pack();
	}
*/
	private void initComponents()
	{
		JPanel aPanel = new JPanel();

		aPanel.add(GUI.buildButton("Close", 'C', "close", this));
		getContentPane().add(itsJScrollPaneCenter, BorderLayout.CENTER);
		getContentPane().add(aPanel, BorderLayout.SOUTH);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		if ("close".equals(theEvent.getActionCommand()))
			dispose();
	}
}
