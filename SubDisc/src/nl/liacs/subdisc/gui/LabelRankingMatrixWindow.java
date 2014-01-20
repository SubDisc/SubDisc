package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import nl.liacs.subdisc.*;

public class LabelRankingMatrixWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public LabelRankingMatrixWindow(LabelRankingMatrix theBaseLRM, LabelRankingMatrix theLRM)
	{
		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));
		add(aClosePanel, BorderLayout.SOUTH);

		JPanel aCenterPanel = new JPanel();
		aCenterPanel.setLayout(new GridLayout(1, 3));

		MatrixPlot aMatrixPlot1 = new MatrixPlot(theBaseLRM, "Base Matrix");
		MatrixPlot aMatrixPlot2 = new MatrixPlot(theLRM, "Subgroup Matrix");
		LabelRankingMatrix aClone = (LabelRankingMatrix) theBaseLRM.clone();
		aClone.subtract(theLRM);
		MatrixPlot aMatrixPlot3 = new MatrixPlot(aClone, "Difference");
		aCenterPanel.add(aMatrixPlot1);
		aCenterPanel.add(aMatrixPlot2);
		aCenterPanel.add(aMatrixPlot3);
		add(aCenterPanel, BorderLayout.CENTER);

		setTitle("Label Ranking Matrix");
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
