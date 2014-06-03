package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import nl.liacs.subdisc.*;

public class PDFWindow2D extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public PDFWindow2D(ProbabilityDensityFunction2_2D thePDF, String theTitle)
	{
		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));
		add(aClosePanel, BorderLayout.SOUTH);

		JPanel aCenterPanelM = new JPanel();
		aCenterPanelM.setLayout(new GridLayout(2, 1));
		
		JPanel aCenterPanel = new JPanel();
		aCenterPanel.setLayout(new GridLayout(1, 3));

		PDFPlot aPDFPlot = new PDFPlot(thePDF, "PDF");
		aCenterPanel.add(aPDFPlot);
		aCenterPanelM.add(aCenterPanel);
		
//		JPanel a2CenterPanel = new JPanel();
//		String aString = "Pairwise Max: "+ theLRM.pairwiseMax(theBaseLRM);
//		JLabel two  = new JLabel(aString);
//		a2CenterPanel.add(two);
//		aCenterPanelM.add(a2CenterPanel);
		
		add(aCenterPanelM, BorderLayout.CENTER);
			
		setTitle("Probability Density Function: " + theTitle);
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
