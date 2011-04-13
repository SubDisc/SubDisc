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
		ROCCurve aROCCurve = new ROCCurve(theSubgroupSet, theSearchParameters);

		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));

		// needs to be run after after new ROCCurve
		JTable aJTable = new JTable(theSubgroupSet.getROCListSubgroups(), SubgroupSet.ROC_HEADER);
		aJTable.setPreferredScrollableViewportSize(new Dimension(100, 80));

		getContentPane().add(new JScrollPane(aJTable), BorderLayout.NORTH);
		getContentPane().add(new JScrollPane(aROCCurve), BorderLayout.CENTER);
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
