package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.List;

import javax.swing.*;

import nl.liacs.subdisc.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;

public class CAUCWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private static final NumberFormat FORMATTER;

	static
	{
		FORMATTER = NumberFormat.getNumberInstance();
		FORMATTER.setMaximumFractionDigits(2);
	}

	public CAUCWindow(Column theTarget, List<List<Float>> theStatistics)
	{
		if (theTarget == null)
			return;

		initComponents(theTarget, theStatistics);

		setTitle("CAUC Window " + theTarget.getName());
		setIconImage(MiningWindow.ICON);
		setLocation(50, 50);
		setSize(GUI.WINDOW_DEFAULT_SIZE); // TODO bigger + squared 700 x 800
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//pack();
		setVisible(true);
	}

	private void initComponents(Column theTarget, List<List<Float>> theStatistics)
	{
		setLayout(new BorderLayout());

		add(new JScrollPane(new ChartPanel(createChart(theTarget, theStatistics))),
			BorderLayout.CENTER);

		final JPanel aPanel = new JPanel();
		aPanel.add(GUI.buildButton("Close", 'C',"close", this));
		add(aPanel, BorderLayout.SOUTH);
	}

	private JFreeChart createChart(Column theColumn, List<List<Float>> theStatistics)
	{
		// fastest + little memory
		JFreeChart aChart =
			ChartFactory.createXYLineChart("",
							null,
							null,
							createDataset(theStatistics),
							PlotOrientation.VERTICAL,
							false,
							true,
							false);

		// TODO  both axis [0-1]

		XYPlot aPlot = aChart.getXYPlot();
//		aChart.getTitle().setFont(aPlot.getDomainAxis().getLabelFont());
		aPlot.setBackgroundPaint(Color.WHITE);
		aPlot.setDomainGridlinePaint(Color.GRAY);
		aPlot.setRangeGridlinePaint(Color.GRAY);
//		aPlot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());


//		itsRenderer = aPlot.getRenderer();
//		itsRenderer.setSeriesPaint(0, Color.BLACK);
//		itsRenderer.setSeriesShape(0, new Ellipse2D.Float(0.0f, 0.0f, 1.0f, 1.0f));
//		itsRenderer.setSeriesPaint(1, Color.RED);
//		itsRenderer.setSeriesShape(1, new Ellipse2D.Float(0.0f, 0.0f, 1.0f, 1.0f));
//		itsRenderer.setSeriesVisible(1, Boolean.FALSE);
		// TODO render order, see Quickie.PlotWindow.drawRegressionLine
		//aPlot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		return aChart;
	}

	private XYSeriesCollection createDataset(List<List<Float>> theStatistics)
	{
		XYSeriesCollection c = new XYSeriesCollection();

		for (List<Float> l : theStatistics)
		{
			XYSeries s = new XYSeries(makeTitle(l), false, false);

			// hard coded for now, needs to change
			for (int i = 3, j = l.size(); i < j; ++i)
				s.add(l.get(i), l.get(++i));
	
			c.addSeries(s);
		}

		return c;
	}

	private String makeTitle(List<Float> theStatistics)
	{
		return String.format("t=%s n=%s auc=%s", 
					FORMATTER.format(theStatistics.get(0)),
					FORMATTER.format(theStatistics.get(1)),
					FORMATTER.format(theStatistics.get(2)));
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		if ("close".equals(theEvent.getActionCommand()))
			dispose();
	}
}
