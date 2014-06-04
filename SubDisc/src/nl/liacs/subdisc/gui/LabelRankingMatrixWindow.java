package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import nl.liacs.subdisc.*;

public class LabelRankingMatrixWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public LabelRankingMatrixWindow(LabelRankingMatrix theBaseLRM, LabelRankingMatrix theLRM, String theTitle)
	{
		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));
		add(aClosePanel, BorderLayout.SOUTH);

		JPanel aCenterPanelM = new JPanel();
		aCenterPanelM.setLayout(new GridLayout(2, 1));
		
		JPanel aCenterPanel = new JPanel();
		aCenterPanel.setLayout(new GridLayout(1, 3));

		MatrixPlot aMatrixPlot1, aMatrixPlot2, aMatrixPlot3;
		aMatrixPlot1 = new MatrixPlot(theBaseLRM, "Base Matrix");
		aCenterPanel.add(aMatrixPlot1);
		
		if (theLRM != null)
		{
			aMatrixPlot2 = new MatrixPlot(theLRM, "Subgroup Matrix");
			LabelRankingMatrix aClone = (LabelRankingMatrix) theBaseLRM.clone();
			aClone.subtract(theLRM);
			aMatrixPlot3 = new MatrixPlot(aClone, "Difference");
			aCenterPanel.add(aMatrixPlot2);
			aCenterPanel.add(aMatrixPlot3);
		}
		aCenterPanelM.add(aCenterPanel);
		
		if (theLRM != null)
		{
			JPanel a2CenterPanel = new JPanel();
			//a2CenterPanel.setLayout(new GridLayout(1, 3));
			String aString = "<html>Pairwise Max: "+ theLRM.findMax()  + " (" + theLRM.pairwiseMax(theLRM) + ") <br>" ;
			aString += "Most different label: "+ theLRM.findMaxLabel() + "</html>";
			JLabel two  = new JLabel(aString);
			a2CenterPanel.add(two);
			
			aCenterPanelM.add(a2CenterPanel);
		}
		add(aCenterPanelM, BorderLayout.CENTER);
			
		setTitle("Label Ranking Matrix: " + theTitle);
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
