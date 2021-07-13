package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class PDFWindow2D extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	// TODO MM make Base Model use PDFPlot2, make use of other constructor
	public PDFWindow2D(ProbabilityDensityFunction2_2D thePDF, String theTitle, String theXAxis, String theYAxis)
	{
		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));
		add(aClosePanel, BorderLayout.SOUTH);

		JPanel aCenterPanelM = new JPanel();
		aCenterPanelM.setLayout(new GridLayout(1, 1));

		JPanel aCenterPanel = new JPanel();
		aCenterPanel.setLayout(new GridLayout(1, 3));

		PDFPlot aPDFPlotSubgroup = new PDFPlot(thePDF, "Dataset PDF", theXAxis, theYAxis);
		aCenterPanel.add(aPDFPlotSubgroup);

		aCenterPanelM.add(aCenterPanel);
		add(aCenterPanelM, BorderLayout.CENTER);

		setTitle(theTitle + ": Probability Density Function");
		setIconImage(MiningWindow.ICON);
		setLocation(100, 100);
		Dimension d = GUI.MATRIX_WINDOW_DEFAULT_SIZE;
		this.setSize(d.width/3, d.height);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	public PDFWindow2D(float[][][] theGrids, double[] theStats, double[][] theLimits, String theTitle, String theXAxis, String theYAxis)
	{
		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));
		add(aClosePanel, BorderLayout.SOUTH);

		JPanel aCenterPanelM = new JPanel();
		aCenterPanelM.setLayout(new GridLayout(1, 1));

		JPanel aCenterPanel = new JPanel();
		aCenterPanel.setLayout(new GridLayout(1, 3));

		// subgroup
		PDFPlot2 aPDFPlotSubgroup = new PDFPlot2(theGrids[0], theStats, theLimits, "Subgroup PDF", theXAxis, theYAxis);
		aCenterPanel.add(aPDFPlotSubgroup);

		// complement
		PDFPlot2 aPDFPlotComplement = new PDFPlot2(theGrids[1], theStats, theLimits, "Complement PDF", theXAxis, theYAxis);
		aCenterPanel.add(aPDFPlotComplement);

		// difference (subgroup-complement)
		PDFPlot2 aPDFPlotDifference = new PDFPlot2(theGrids[2], theStats, theLimits, "Density Difference", theXAxis, theYAxis);
		aCenterPanel.add(aPDFPlotDifference);

		aCenterPanelM.add(aCenterPanel);
		add(aCenterPanelM, BorderLayout.CENTER);

		setTitle(theTitle + ": Probability Density Function");
		setIconImage(MiningWindow.ICON);
		setLocation(100, 100);
		setSize(GUI.MATRIX_WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		if ("close".equals(theEvent.getActionCommand()))
			dispose();
	}
}
