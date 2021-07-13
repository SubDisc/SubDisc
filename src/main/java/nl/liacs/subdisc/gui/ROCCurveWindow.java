package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

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

		add(new JScrollPane(aJTable), BorderLayout.NORTH);
		add(aROCCurve, BorderLayout.CENTER);
		add(aClosePanel, BorderLayout.SOUTH);

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
		BufferedWriter aWriter = null;
		File aFile = new FileHandler(FileType.PLT).getFile();

		if (aFile == null)
			return;
		else
		{
			try
			{
				aWriter = new BufferedWriter(new FileWriter(aFile));
				aWriter.write(getGnuPlotString());
			}
			catch (IOException e)
			{
				Log.logCommandLine("Error while writing: " + aFile);
			}
			finally
			{
				try
				{
					if (aWriter != null)
						aWriter.close();
				}
				catch (IOException e)
				{
					Log.logCommandLine("Error while writing: " + aFile);
				}
			}
		}
	}

	private String getGnuPlotString()
	{
		// Formatter to print all non-zero decimal digits, always use .
		// 340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
		DecimalFormat aDF = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		aDF.setMaximumFractionDigits(340);

		double aSize = 0.001;
		String sz = aDF.format(aSize);

		String aFmtSG = "set object circle at %s, %s, %d size " + sz + " fc rgb \"black\"\n";
		String aFmtHull = "set arrow from %s, %s, %d to %s, %s, %d nohead lt 1 lw 2 lc rgb \"black\"\n";
		String aFmtPlot = "splot %s(x,y) lt 3 lw 0.5\n";

		ROCList anROCList = itsSubgroupSet.getROCList();
		String auc = aDF.format(anROCList.getAreaUnderCurve()).replace(".", "p");
		String aFmtFile = "roc-%s-auc"+ auc + ".eps";

		int init = (itsSubgroupSet.size() * 87) + (anROCList.size() * 128) + 500;
		StringBuilder sb = new StringBuilder(init);
		// general plot settings
		sb.append("# general plot settings\n");
		sb.append("set xlabel \"fpr\"\n");
		sb.append("set ylabel \"tpr\"\n");
		sb.append("set xrange [0:1]\n");
		sb.append("set yrange [0:1]\n");
		sb.append("set xtics 0.1\n");
		sb.append("set ytics 0.1\n");
		sb.append("\n");
		// basic functions for Quality Measures
		sb.append("# basic functions for Quality Measures\n");
		sb.append("N = " + itsSubgroupSet.getTotalCoverage() + ".0\n");
		sb.append("p = " + itsSubgroupSet.getTotalTargetCoverage() + ".0\n");
		sb.append("n = N-p\n");
		sb.append("pos(y) = y*p\n");     //number of positives covered by subgroup
		sb.append("neg(x) = x*(N-p)\n"); //number of negatives covered by subgroup
		sb.append("aTotalTargetCoverageNotBody(y) = p-pos(y)\n");
		sb.append("aCountNotHeadNotBody(x) = N-(p+neg(x))\n");
		sb.append("aCountBody(x,y) = neg(x)+pos(y)\n");
		sb.append("aCountNotHeadBody(x,y) = aCountBody(x,y) - pos(y)\n");
		sb.append("sqr(x) = x*x\n");
		sb.append("max(x,y) = x>y?x:y\n");
		sb.append("expect(x,y,z) = x*(y/x)*(z/x)\n");
		sb.append("e11(x,y,z) = expect(x,y,z)\n");
		sb.append("e01(x,y,z) = expect(x,x-y,z)\n");
		sb.append("e10(x,y,z) = expect(x,y,x-z)\n");
		sb.append("e00(x,y,z) = expect(x,x-y,x-z)\n");
		sb.append("lg(x) = log(x)/log(2)\n");
		sb.append("H(x) = -x*lg(x)-(1-x)*lg(1-x)\n");
		sb.append("mi(x,y,z) = x*log(x/((x+y)*(x+z)))\n");
		sb.append("\n");
		// QM.getQualityMeasures(TargetType.SINGLE_NOMINAL)
		// TODO MM
		// not all nominal QMs are implemented, add comment in QM
		sb.append("# definitions of implmented Quality Measures\n");
		for (QM q : QM.getQualityMeasures(TargetType.SINGLE_NOMINAL))
			sb.append(QM.getDefinition(q) + "\n");
		sb.append("\n");

		// a point for every subgroup
		// NOTE
		// for image, only unique x,y points are needed
		// but adding every Subgroup serves as 'documentation'
		sb.append("# a point for every subgroup\n");
		for (Subgroup aSubgroup : itsSubgroupSet)
		{
			double anX = aSubgroup.getFalsePositiveRate();
			double aY  = aSubgroup.getTruePositiveRate();
			int aZ  = aSubgroup.getID();

			sb.append(String.format(aFmtSG, aDF.format(anX), aDF.format(aY), aZ));
		}
		sb.append("\n");

		// points/lines for subgroups on the convex hull
		sb.append("# points/lines for subgroups on the convex hull\n");
		double anOldX = 0.0;
		double anOldY = 0.0;
		int anOldZ = 0;
		for (SubgroupROCPoint aPoint : anROCList)
		{
			double aNewX = aPoint.getFPR();
			double aNewY = aPoint.getTPR();
			int aNewZ = aPoint.ID;

			sb.append(String.format(aFmtHull,
						aDF.format(anOldX), aDF.format(anOldY), anOldZ,
						aDF.format(aNewX), aDF.format(aNewY), aNewZ));

			anOldX = aNewX;
			anOldY = aNewY;
			anOldZ = aNewZ;
		}
		sb.append(String.format(aFmtHull,
					aDF.format(anOldX), aDF.format(anOldY), anOldZ,
					aDF.format(1.0), aDF.format(1.0), 0));
		sb.append("\n");

		// Quality Measure isometrics
		sb.append("# Quality Measure isometrics\n");
		sb.append("# if the 'splot' command below does not generate any output\n");
		sb.append("# check that the function it calls is included in the\n");
		sb.append("# list of Quality Measure function definitions above\n");
		// name conversion should be same as in QM.getDefinition(QM)
		final QM aQM = itsSearchParameters.getQualityMeasure();
		String s = aQM.GUI_TEXT.replaceAll(" ", "_").replaceAll("-", "_");
		sb.append(String.format(aFmtPlot, s));
		sb.append("\n");

		// settings for Quality Measure isometrics
		sb.append("# settings for Quality Measure isometrics\n");
		sb.append("set isosamples 100\n");
		sb.append("set contour\n");
		sb.append("set cntrparam cubicspline\n");
		sb.append("set cntrparam order 5\n");
		sb.append("set cntrparam points 20\n");
		sb.append("set cntrparam levels 20\n");
		sb.append("unset clabel\n");
		sb.append("set view map\n");
		sb.append("unset surface\n");
		sb.append("\n");

		// replot does not work when QM is unimplemented / splot failed
		sb.append("set terminal postscript eps size 5,5\n");
		sb.append("set output \"" + String.format(aFmtFile, s) + "\"\n");
		sb.append("replot\n");
		sb.append("set output\n");
		sb.append("set terminal pop\n");
		sb.append("set size 1,1\n");
		sb.append("\n");

		return sb.toString();
	}
}
