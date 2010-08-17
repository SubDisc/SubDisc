package nl.liacs.subdisc.gui;

import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.JFrame;

public class ROCCurveWindow extends JFrame
{
	private static final long serialVersionUID = 1L;

	private ROCCurve itsROCCurve;
	GeneralPath itsCurve;

	public ROCCurveWindow(ROCRuleList theROCRuleList, RuleSet theRuleSet,
							float theXMin, float theYMin, int theMin, float theXMax, float theYMax, int theMax)
	{
		initComponents();

		NumberFormat aFormatter = NumberFormat.getNumberInstance();
		aFormatter.setMaximumFractionDigits(3);
		setTitle("ROC Curve (area under curve: " + aFormatter.format(theROCRuleList.getAreaUnderCurve()) + ")");

		Vector<Point2D> aCurve = new Vector<Point2D>();
		ListIterator<Rule> aListIterator = theROCRuleList.listIterator();
		while (aListIterator.hasNext())
		{
			Rule aRule = aListIterator.next();
			Point2D aPoint = new Point2D.Float(aRule.getFalsePositiveRate(),
										 aRule.getTruePositiveRate());
			aCurve.add(aPoint);
		}

		Vector<Point2D> aPoints = new Vector<Point2D>();
		Iterator<Rule> anIterator = theRuleSet.iterator();
		while (anIterator.hasNext())
		{
			Rule aRule = anIterator.next();
			Point2D aPoint = new Point2D.Float(aRule.getFalsePositiveRate(),
										 aRule.getTruePositiveRate());
			aPoints.add(aPoint);
		}

		itsROCCurve = new ROCCurve(aCurve, aPoints, theXMin, theYMin, theMin, theXMax, theYMax, theMax);
		jScrollPaneCenter.setViewportView(itsROCCurve);
		setIconImage(MiningWindow.ICON);
		pack();
	}

	public ROCCurveWindow(DecisionList theDecisionList)
	{
		initComponents();

		NumberFormat aFormatter = NumberFormat.getNumberInstance();
		aFormatter.setMaximumFractionDigits(3);
		setTitle("ROC Curve (area under curve: " + aFormatter.format(theDecisionList.getAreaUnderCurve()) + ")");

		Vector<Point2D> aCurve = new Vector<Point2D>();
		ListIterator<Rule> aListIterator = theDecisionList.listIterator();
		int anIndex = 0;

		while (aListIterator.hasNext())
		{
			aListIterator.next();
			Point2D aPoint = new Point2D.Float(theDecisionList.getFalsePositiveRate(anIndex),
												theDecisionList.getTruePositiveRate(anIndex));
			aCurve.add(aPoint);
			anIndex ++;
		}

		itsROCCurve = new ROCCurve(aCurve);
		jScrollPaneCenter.setViewportView(itsROCCurve);
		setIconImage(MiningWindow.ICON);
		pack();
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
