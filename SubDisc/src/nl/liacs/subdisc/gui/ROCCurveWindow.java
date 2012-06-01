package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class ROCCurveWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private SubgroupSet itsSubgroupSet;

	public ROCCurveWindow(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters, QualityMeasure theQualityMeasure)
	{
		ROCCurve aROCCurve = new ROCCurve(theSubgroupSet, theSearchParameters, theQualityMeasure);
		itsSubgroupSet = theSubgroupSet;

		JPanel aClosePanel = new JPanel();
		aClosePanel.add(GUI.buildButton("GnuPlot", 'G', "gnuplot", this));
		aClosePanel.add(GUI.buildButton("Close", 'C', "close", this));

		// needs to be run after new ROCCurve
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
		if ("gnuplot".equals(theEvent.getActionCommand()))
			createGnuPlotFile();
	}

	private void createGnuPlotFile()
	{
		float aSize = 0.005f;
		int aCount = 0;
		String aContent = "set xlabel \"fpr\";set ylabel \"tpr\";set xrange [0:1];set yrange [0:1];lg(x) = log(x)/log(2);H(x) = -x*lg(x)-(1-x)*lg(1-x);IG(x,y) = 1 - 0.5*(x+y)*H(x/(x+y)) - 0.5*(2-x-y)*H((1-x)/(2-x-y));\n";
		for (Subgroup aSubgroup : itsSubgroupSet)
		{
			aCount++;
			float anX = aSubgroup.getFalsePositiveRate();
			float aY = aSubgroup.getTruePositiveRate();
			aContent += "set object " + aCount + " rect from " + anX + "," + aY + " to " + (anX+aSize) + "," + (aY+aSize) + " fc rgb \"black\"\n";
		}
		aContent += "set isosamples 100; set contour; set cntrparam cubicspline; set cntrparam order 5; set cntrparam points 20; set cntrparam levels 20; unset clabel; set view map; unset surface; splot IG(x,y) lt 1;\n";
		aContent += "set terminal postscript eps size 5,5; set output \'roc.eps\'; replot;\n";
		aContent += "set output; set terminal pop; set size 1,1;\n";
		Log.logCommandLine(aContent);
	}
}
