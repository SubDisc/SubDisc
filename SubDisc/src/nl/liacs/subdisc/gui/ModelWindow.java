package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import javax.swing.*;

import nl.liacs.subdisc.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;

public class ModelWindow extends JFrame
{
	private JPanel jPanel;
	private JButton jButton;
	private JScrollPane jScrollPaneCenter;
	private static final long serialVersionUID = 1L;

	private DAGView itsDAGView;

	//correlation and regression ===============================

	public ModelWindow(Column theXColumn, Column theYColumn, String theX, String theY, RegressionMeasure theRM, Subgroup theSubgroup)
	{
		initComponents();
		String aName = "2D distribution";
		if (theRM != null)
			aName = "y = " + (float)theRM.getIntercept() + " + " + (float)theRM.getSlope() + " * x";

		//data
		BitSet aMembers = (theSubgroup == null) ? null : theSubgroup.getMembers();
		XYSeries aSeries = new XYSeries("data");
		for (int i = 0; i < theXColumn.size(); i++)
			if (theSubgroup ==null || aMembers.get(i)) //if complete database, or i is a member of the specified subgroup
				aSeries.add(theXColumn.getFloat(i), theYColumn.getFloat(i));
		XYSeriesCollection aDataSet = new XYSeriesCollection();
		aDataSet.addSeries(aSeries);

		// create the chart
		JFreeChart aChart =
			ChartFactory.createScatterPlot(aName, theX, theY,	aDataSet, PlotOrientation.VERTICAL, false, true, false);
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
			aDataSet = new XYSeriesCollection();
			aSeries = new XYSeries("line");
			aSeries.add(theXColumn.getMin(), theRM.getBaseFunctionValue(theXColumn.getMin()));
			aSeries.add(theXColumn.getMax(), theRM.getBaseFunctionValue(theXColumn.getMax()));
			aDataSet.addSeries(aSeries); //add second series to represent line
			plot.setDataset(1, aDataSet);
			plot.setRenderer(1, aLineRenderer);
			aLineRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
		}

		ChartPanel aChartPanel = new ChartPanel(aChart);
		jScrollPaneCenter.setViewportView(aChartPanel);

//		setIconImage(MiningWindow.ICON);
		pack();
	}



	//DAG ==================================================

	public ModelWindow(DAG theDAG, int theDAGWidth, int theDAGHeight)
	{
		initComponents();
		itsDAGView = new DAGView(theDAG);
		itsDAGView.setDAGArea(theDAGWidth, theDAGHeight);
		itsDAGView.drawDAG();
		jScrollPaneCenter.setViewportView(itsDAGView);
//		setIconImage(MiningWindow.ICON);
		pack();
	}

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

	private void initComponents()
	{
		jPanel = new javax.swing.JPanel();
		jButton = new javax.swing.JButton();
		jScrollPaneCenter = new javax.swing.JScrollPane();
		addWindowListener(
			new java.awt.event.WindowAdapter()
			{
				public void windowClosing(java.awt.event.WindowEvent evt)
				{
					exitForm(evt);
				}
			}
		);

		jButton.setPreferredSize(new java.awt.Dimension(80, 25));
		jButton.setBorder(new javax.swing.border.BevelBorder(0));
		jButton.setMaximumSize(new java.awt.Dimension(80, 25));
		jButton.setFont(new java.awt.Font ("Dialog", 1, 11));
		jButton.setText("Close");
		jButton.setMnemonic('C');
		jButton.setMinimumSize(new java.awt.Dimension(80, 25));
		jButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent evt)
				{
					jButtonActionPerformed(evt);
				}
			}
		);

		jPanel.add(jButton);
		getContentPane().add(jScrollPaneCenter, java.awt.BorderLayout.CENTER);
		getContentPane().add(jPanel, java.awt.BorderLayout.SOUTH);
	}

	private void jButtonActionPerformed(java.awt.event.ActionEvent evt)
	{
		dispose();
	}

	private void exitForm(java.awt.event.WindowEvent evt)
	{
		dispose();
	}
}
