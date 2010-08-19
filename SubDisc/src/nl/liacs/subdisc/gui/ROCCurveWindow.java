package nl.liacs.subdisc.gui;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import nl.liacs.subdisc.SearchParameters;
import nl.liacs.subdisc.SubgroupSet;

public class ROCCurveWindow extends JFrame
{
	private static final long serialVersionUID = 1L;

	public ROCCurveWindow(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters)
	{
		initComponents();

		ROCCurve aROCCurve = new ROCCurve(theSubgroupSet, theSearchParameters);
		setTitle("ROC Curve (area under curve: " + aROCCurve.getAreaUnderCurve() + ")");
		jScrollPaneCenter.setViewportView(aROCCurve);
//		setIconImage(MiningWindow.ICON);
		pack();
		setLocation(100, 100);
		setSize(400, 400);
		setVisible(true);
	}

	private void initComponents()
	{
		jPanel1 = new javax.swing.JPanel();
		jButtonClose = new javax.swing.JButton();
		jScrollPaneCenter = new javax.swing.JScrollPane();
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				exitForm(evt);
			}
		});

		jButtonClose.setPreferredSize(new java.awt.Dimension(80, 25));
		jButtonClose.setBorder(new javax.swing.border.BevelBorder(0));
		jButtonClose.setMaximumSize(new java.awt.Dimension(80, 25));
		jButtonClose.setFont(new java.awt.Font ("Dialog", 1, 11));
		jButtonClose.setText("Close");
		jButtonClose.setMnemonic('C');
		jButtonClose.setMinimumSize(new java.awt.Dimension(80, 25));
		jButtonClose.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButtonCloseActionPerformed(evt);
			}
		});
		jPanel1.add(jButtonClose);

		getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);
		getContentPane().add(jScrollPaneCenter, java.awt.BorderLayout.CENTER);
	}

	private void jButtonCloseActionPerformed(ActionEvent evt) { dispose(); }
	private void exitForm(WindowEvent evt) { dispose(); }

	private javax.swing.JPanel jPanel1;
	private javax.swing.JButton jButtonClose;
	private javax.swing.JScrollPane jScrollPaneCenter;
}
