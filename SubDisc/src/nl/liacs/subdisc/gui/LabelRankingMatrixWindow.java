package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import nl.liacs.subdisc.*;

public class LabelRankingMatrixWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public LabelRankingMatrixWindow(LabelRankingMatrix theLRM)
	{
		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));
		MatrixPlot aMatrixPlot = new MatrixPlot(theLRM);
		add(aMatrixPlot, BorderLayout.CENTER);
		add(aClosePanel, BorderLayout.SOUTH);

		setTitle("Label Ranking Matrix");
		setIconImage(MiningWindow.ICON);
		setLocation(100, 100);
		setSize(GUI.ROC_WINDOW_DEFAULT_SIZE);
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
