package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class ROCCurveWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	public ROCCurveWindow(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters)
	{
		JScrollPane aROCScrollPane = new JScrollPane();
		ROCCurve aROCCurve = new ROCCurve(theSubgroupSet, theSearchParameters);
		aROCScrollPane.setViewportView(aROCCurve);

		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));

		getContentPane().add(aROCScrollPane, BorderLayout.CENTER);
		getContentPane().add(aClosePanel, BorderLayout.SOUTH);

		setTitle("ROC Curve (area under curve: " + aROCCurve.getAreaUnderCurve() + ")");
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
