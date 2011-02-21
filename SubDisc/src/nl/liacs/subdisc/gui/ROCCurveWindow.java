package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class ROCCurveWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private JScrollPane itsROCScrollPane;

	public ROCCurveWindow(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters)
	{
		initComponents();

		ROCCurve aROCCurve = new ROCCurve(theSubgroupSet, theSearchParameters);
		itsROCScrollPane.setViewportView(aROCCurve);

		setTitle("ROC Curve (area under curve: " + aROCCurve.getAreaUnderCurve() + ")");
		setIconImage(MiningWindow.ICON);
		setLocation(100, 100);
		setSize(GUI.ROC_WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	private void initComponents()
	{
		itsROCScrollPane = new JScrollPane();
		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));

		getContentPane().add(itsROCScrollPane, BorderLayout.CENTER);
		getContentPane().add(aClosePanel, BorderLayout.SOUTH);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		if ("close".equals(theEvent.getActionCommand()))
			dispose();
	}
}
