package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class ROCCurveWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private SubgroupSet itsSubgroupSet;
	private SearchParameters itsSearchParameters;

	public ROCCurveWindow(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters, QualityMeasure theQualityMeasure)
	{
		ROCCurve aROCCurve = new ROCCurve(theSubgroupSet, theSearchParameters, theQualityMeasure);
		itsSubgroupSet = theSubgroupSet;
		itsSearchParameters = theSearchParameters;

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
		float anX, aY;
		float aSize = 0.001f;
		int aCount = 0;
		String aContent = "set xlabel \"fpr\";set ylabel \"tpr\";set xrange [0:1];set yrange [0:1];set xtics 0.1; set ytics 0.1;\n";
		aContent += "N = " + itsSubgroupSet.getTotalCoverage() + ".0\n";
		aContent += "p = " + itsSubgroupSet.getTotalTargetCoverage() + ".0\n";
		aContent += "n = N-p\n";
		aContent += "pos(y) = y*p\n"; //number of positives covered by subgroup
		aContent += "neg(x) = x*(N-p)\n"; //number of negatives covered by subgroup
		aContent += "aTotalTargetCoverageNotBody(y) = p-pos(y)\n";
		aContent += "aCountNotHeadNotBody(x) = N-(p+neg(x))\n";
		aContent += "aCountBody(x,y) = neg(x)+pos(y)\n";
		aContent += "sqr(x) = x*x\n";
		aContent += "expect(x,y,z) = x*(y/x)*(z/x)\n";
		aContent += "e11(x,y,z) = expect(x,y,z)\n";
		aContent += "e01(x,y,z) = expect(x,x-y,z)\n";
		aContent += "e10(x,y,z) = expect(x,y,x-z)\n";
		aContent += "e00(x,y,z) = expect(x,x-y,x-z)\n";
		aContent += "lg(x) = log(x)/log(2); H(x) = -x*lg(x)-(1-x)*lg(1-x)\n";


		aContent += "WRAcc(x,y) = (pos(y)/N)-(p/N)*(aCountBody(x,y)/N)\n";
		aContent += "absWRAcc(x,y) = abs((pos(y)/N)-(p/N)*(aCountBody(x,y)/N))\n";
		aContent += "Chi2(x,y) = sqr(pos(y) - e11(N,aCountBody(x,y),p)) / e11(N,aCountBody(x,y),p) +";
		aContent += 		   " sqr(p - pos(y) - e01(N,aCountBody(x,y),p)) / e01(N,aCountBody(x,y),p) +";
		aContent += 		   " sqr(aCountBody(x,y) - pos(y) - e10(N,aCountBody(x,y),p)) / e10(N,aCountBody(x,y),p) +";
		aContent += 		   " sqr(N - p - aCountBody(x,y) + pos(y) - e00(N,aCountBody(x,y),p)) / e00(N,aCountBody(x,y),p); \n";
		aContent += "IG(x,y) = 1 - 0.5*(x+y)*H(x/(x+y)) - 0.5*(2-x-y)*H((1-x)/(2-x-y))\n";
		aContent += "Binomial(x,y) = sqrt(aCountBody(x,y)/N) * (pos(y)/aCountBody(x,y) - p/N)\n";
		aContent += "Jaccard(x,y) = pos(y)/(aCountBody(x,y) + aTotalTargetCoverageNotBody(y))\n";
		aContent += "Coverage(x,y) = aCountBody(x,y)\n";
		aContent += "Accuracy(x,y) = pos(y)/aCountBody(x,y)\n";
//		aContent += "Binomial(x,y) = sqrt(aCountBody(x,y)/N) * (pos(y)/aCountBody(x,y) - p/N)\n";
//		aContent += "Binomial(x,y) = sqrt(aCountBody(x,y)/N) * (pos(y)/aCountBody(x,y) - p/N)\n";
//		aContent += "Binomial(x,y) = sqrt(aCountBody(x,y)/N) * (pos(y)/aCountBody(x,y) - p/N)\n";
//		aContent += "Binomial(x,y) = sqrt(aCountBody(x,y)/N) * (pos(y)/aCountBody(x,y) - p/N)\n";
		aContent += "\n";

		//subgroups
		for (Subgroup aSubgroup : itsSubgroupSet)
		{
			aCount++;
			anX = aSubgroup.getFalsePositiveRate();
			aY = aSubgroup.getTruePositiveRate();
			aContent += "set object circle at " + anX + "," + aY + " size " + aSize + " fc rgb \"black\"\n";
		}

		//subgroups
		anX = 0f;
		aY = 0f;
		for (SubgroupROCPoint aPoint : itsSubgroupSet.getROCList())
		{
			aCount++;
			float aNewX = aPoint.getFPR();
			float aNewY = aPoint.getTPR();
			aContent += "set arrow from " + anX + "," + aY + " to " + aNewX + "," + aNewY + " nohead lt 1 lw 2 lc rgb \"black\"\n";
			anX = aNewX;
			aY = aNewY;
		}
		aContent += "set arrow from " + anX + "," + aY + " to 1,1 nohead lt 1 lw 2 lc rgb \"black\"\n";

		aContent += "set isosamples 100; set contour; set cntrparam cubicspline; set cntrparam order 5; set cntrparam points 20; set cntrparam levels 20; unset clabel; set view map; unset surface;\n";

		switch (itsSearchParameters.getQualityMeasure())
		{
			case (QualityMeasure.WRACC) : aContent += "splot WRAcc(x,y) lt 3 lw 0.5;\n"; break;
			case (QualityMeasure.ABSWRACC) : aContent += "splot absWRAcc(x,y) lt 3 lw 0.5;\n"; break;
			case (QualityMeasure.CHI_SQUARED) : aContent += "splot Chi2(x,y) lt 3 lw 0.5;\n"; break;
			case (QualityMeasure.INFORMATION_GAIN) : aContent += "splot IG(x,y) lt 3 lw 0.5;\n"; break;
			case (QualityMeasure.BINOMIAL) : aContent += "splot Binomial(x,y) lt 3 lw 0.5;\n"; break;
			case (QualityMeasure.JACCARD) : aContent += "splot Jaccard(x,y) lt 3 lw 0.5;\n"; break;
			case (QualityMeasure.COVERAGE) : aContent += "splot Coverage(x,y) lt 3 lw 0.5;\n"; break;
			case (QualityMeasure.ACCURACY) : aContent += "splot Accuracy(x,y) lt 3 lw 0.5;\n"; break;
		}
		aContent += "set terminal postscript eps size 5,5; set output \'roc.eps\'; replot;\n";
		aContent += "set output; set terminal pop; set size 1,1;\n";
		Log.logCommandLine(aContent);
	}
}
