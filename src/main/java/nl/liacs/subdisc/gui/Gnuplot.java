package nl.liacs.subdisc.gui;

import java.io.*;
import java.util.*;

import nl.liacs.subdisc.*;

public class Gnuplot
{
	private static final String DELIMITER = "\t";
	private static final String COLUMN_HEADER =
		"threshold" + DELIMITER + "FPR" + DELIMITER + "TPR"; // x y z
	private static final String DATA_EXT = ".dat";
	private static final String SCRIPT_EXT = ".gp";

	// uninstantiable
	private Gnuplot() {};


	// TODO null checks + theStatistics.size() > 0
	public static void writeSplotSccript(Column theColumn, List<List<Double>> theStatistics)
	{
		final String aBaseName = String.format("%s_%d",
						theColumn.getName(),
						System.currentTimeMillis());

		writeData(aBaseName, theStatistics);
		writeSplotScript(theColumn.getName(), aBaseName, theStatistics);
	}

	private static void writeData(String theBaseName, List<List<Double>> theStatistics)
	{
		BufferedWriter br = null;

		try
		{
			final File f  = new File(theBaseName + DATA_EXT);
			br = new BufferedWriter(new FileWriter(f));

			int aSize = theStatistics.size();
			br.write(getColumnNumber(aSize));
			br.write(getHeader(theStatistics));
			br.write(getHeader2(aSize));
			// 3 > threshold FPR TPR ...
			for (int i = 3, j = getMax(theStatistics); i < j; i+=2)
				br.write(getDataLine(i, j, theStatistics));
			log("data", f);
		}
		catch (IOException e) {}
		finally
		{
			if (br != null)
			{
				try { br.close(); }
				catch (IOException e) {}
			}
		}
	}

	private static String getColumnNumber(int theSize)
	{
		StringBuilder sb = new StringBuilder(theSize*6);
		sb.append("#"); // commented header line
		sb.append(1);
		// *3 = threshold fpr tpr
		for (int i = 1, j = theSize*3; i < j; )
		{
			sb.append(DELIMITER);
			sb.append(++i);
		}
		return sb.append("\n").toString();
	}

	private static String getHeader(List<List<Double>> theStatistics)
	{
		StringBuilder sb = new StringBuilder(theStatistics.size()*64);
		sb.append("#"); // commented header line
		sb.append(makeTitle(theStatistics.get(0)));
		for (int i = 1, j = theStatistics.size(); i < j; ++i)
		{
			sb.append(DELIMITER);
			sb.append(DELIMITER);
			sb.append(DELIMITER);
			sb.append(makeTitle(theStatistics.get(i)));
		}
		return sb.append("\n").toString();
	}

	private static String makeTitle(List<Double> theStats)
	{
		return String.format("t=%f_n=%f_auc=%f",
					theStats.get(0),
					theStats.get(1),
					theStats.get(2));
	}

	private static String getHeader2(int theSize)
	{
		StringBuilder sb = new StringBuilder(theSize*20);
		sb.append("#"); // commented header line
		sb.append(COLUMN_HEADER);
		for (int i = 1, j = theSize; i < j; ++i)
		{
			sb.append(DELIMITER);
			sb.append(COLUMN_HEADER);
		}
		return sb.append("\n").toString();
	}

	private static int getMax(List<List<Double>> theStatistics)
	{
		int aMax = 0;
		for (List<Double> l : theStatistics)
			if (l.size() > aMax)
				aMax = l.size();
		return aMax;
	}

	private static String getDataLine(int theIndex, int theMaxSize, List<List<Double>> theStatistics)
	{
		StringBuilder sb = new StringBuilder(theStatistics.size()*20);
		sb.append(getDatum(theIndex, theStatistics.get(0)));
		for (int i = 1, j = theStatistics.size(); i < j; ++i)
		{
			sb.append(DELIMITER);
			sb.append(getDatum(theIndex, theStatistics.get(i)));
		}
		return sb.append("\n").toString();
	}

	// NOTE script could use the less portable: 'set datafile missing = "?"'
	private static String getDatum(int theIndex, List<Double> theStats)
	{
		if (theStats.size() <= theIndex)
			return getDatum(theStats.get(0), 1.0, 1.0);
		else
			return getDatum(theStats.get(0),
					theStats.get(theIndex),
					theStats.get(++theIndex));
	}

	private static String getDatum(double theThreshold, double theFPR, double theTPR)
	{
		return String.format("%f%s%f%s%f",
			theThreshold, DELIMITER, theFPR, DELIMITER, theTPR);
	}

	private static void writeSplotScript(String theTitle, String theBaseName, List<List<Double>> theStatistics)
	{
		BufferedWriter br = null;

		try
		{
			File f = new File(theBaseName + SCRIPT_EXT);
			br = new BufferedWriter(new FileWriter(f));

			br.write(parameterise(theTitle,
						theBaseName,
						theStatistics));
			log("script", f);
		}
		catch (IOException e) {}
		finally
		{
			if (br != null)
			{
				try { br.close(); }
				catch (IOException e) {}
			}
		}
	}

	private static String parameterise(String theTitle, String theBaseName, List<List<Double>> theStatistics)
	{
		int aSize = theStatistics.size();
		return String.format(PLOT_CODE,
					theBaseName + DATA_EXT, // tricky
					1, // 0 for plot, !0 for splot
					aSize*3,
					theStatistics.get(0).get(0),
					theStatistics.get(aSize-1).get(0),
					theTitle);
	}

	private static void log(String theType, File theFile)
	{
		Log.logCommandLine(String.format("Gnuplot %s written: '%s'",
						theType,
						theFile.getAbsolutePath()));
	}

	// YES I want this in code
	public static final String PLOT_CODE =
		"##### DECLARATION OF DEFAULTS #####\n" +
		"\n" +
		// based on input parameters
		"INPUT_FILE = '%s'\n" +
		"SPLOT = %d\n" + 
		"OUTPUT_FILE = SPLOT ? INPUT_FILE.'.3d.' : INPUT_FILE.'.2d.'\n" +
		"\n" +
		"NR_COLUMNS = %d\n" +
		"THRESHOLD_MIN = %f\n" +
		"THRESHOLD_MAX = %f\n" +
		"\n" +
		"TITLE = '%s'\n" +
		// end based on input parameters
		"\n" +
		"FONT = 'Helvetica'\n" +
		"FONT_SIZE = 14\n" +
		"\n" +
		"XLABEL = SPLOT ? 'Threshold' : 'FPR'\n" +
		"YLABEL = SPLOT ? 'FPR' : 'TPR'\n" +
		"ZLABEL = 'TPR'\n" +
		"\n" +
		"TIC_SIZE = 0.2\n" +
		"USE_MINOR_TICS = 0\n" +
		"\n" +
		"LINE_STYLE = 1\n" +
		"LINE_WIDTH = 1\n" +
		"\n" +
		"\n" +
		"\n" +
		"##### SETUP #####\n" +
		"\n" +
		"set terminal postscript eps enhanced FONT FONT_SIZE\n" +
		"set output OUTPUT_FILE.'eps'\n" +
		"\n" +
		"set title TITLE\n" +
		"\n" +
		"set xlabel XLABEL\n" +
		"set ylabel YLABEL\n" +
		"set zlabel ZLABEL\n" +
		"\n" +
		"if (!SPLOT) set xtics 0 TIC_SIZE\n" +
		"set ytics 0 TIC_SIZE\n" +
		"set ztics 0 TIC_SIZE\n" +
		"\n" +
		"# NOTE will also set mxtics\n" +
		"if (USE_MINOR_TICS) set mxtics; set mytics; set mztics;\n" +
		"\n" +
		"if (!SPLOT) set xrange [0 : 1]; else set xrange [THRESHOLD_MIN : THRESHOLD_MAX]\n" +
		"set yrange [0 : 1]\n" +
		"set zrange [0 : 1]\n" +
		"\n" +
		"# forces ground plain to height 0.0\n" +
		"set ticslevel 0\n" +
		"\n" +
		"\n" +
		"\n" +
		"##### PLOTTING #####\n" +
		"\n" +
		"if (SPLOT) \\\n" +
		"	splot for [i=1 : NR_COLUMNS-2 : 3] \\\n" +
		"		INPUT_FILE u i : i+1 : i+2 w l ls LINE_STYLE lw LINE_WIDTH notitle; \\\n" +
		"else \\\n" +
		"	plot for [i=1 : NR_COLUMNS-2 : 3] \\\n" +
		"		INPUT_FILE u i+1 : i+2 w l ls LINE_STYLE lw LINE_WIDTH notitle\n" +
		"\n" +
		"#set terminal pslatex\n" +
		"#set output OUTPUT_FILE.'tex'\n" +
		"#replot\n" +
		"\n" +
		"#set terminal postscript landscape enhanced FONT 8\n" +
		"#set output OUTPUT_FILE.'ps'\n" +
		"#replot\n" +
		"\n" +
		"#set terminal postscript eps FONT FONT_SZIE\n" +
		"#set output OUTPUT_FILE.'eps'\n" +
		"#replot\n" +
		"\n" +
		"#set terminal svg\n" +
		"#set output OUTPUT_FILE.'svg'\n" +
		"\n" +
		"#replot\n" +
		"set output\n" +
		"#set terminal windows\n" +
		"#platform-independent way of restoring terminal by push/pop\n" +
		"set terminal pop\n" +
		"#set size 1,1\n\n";

	// YES again...
	// takes 3 String parameters in order: data_file_name, x_label, y_label
	// where data_file_name is the name of the file that holds the grid data
	public static final String PLOT_CODE_PDF_2D =
		"##### PUT THE NAME OF THE VARIABLES HERE #####\n" +
		"DATA_FILE='%s'\n" +	// parameter for data file name
		"X_LABEL='%s'\n" +	// parameter for xlabel
		"Y_LABEL='%s'\n" +	// parameter for ylabel
		"\n" +
		"##### DO NOT CHANGE ANYTING BELOW #####\n" +
		"PWD=\"`pwd`/\"\n" +
		"INPUT_FILE=PWD.DATA_FILE\n" +
		"TITLE=sprintf(\"PDF for: %%s versus %%s.\", X_LABEL, Y_LABEL)\n" +
		"\n" +
		"set terminal postscript eps enhanced 'Helvetica' 14\n" +
		"set output INPUT_FILE.'.eps'\n" +
		"\n" +
		"#set encoding iso_8859_1\n" +
		"#set title TITLE\n" +
		"\n" +
		"# set key spacing 1 when outputting to .(e)ps\n" +
		"set key spacing 1\n" +
		"set xlabel X_LABEL\n" +
		"set ylabel Y_LABEL\n" +
		"set zlabel 'KDE'\n" +
		"\n" +
		"set xtics\n" +
		"set ytics\n" +
		"set ztics\n" +
		"\n" +
		"set zrange [-1:1]\n" + 
		"\n" +
		"set view 60,30,.8\n" +
		"unset colorbox\n" +
		"set hidden3d\n" +
		"\n" +
		"set pm3d\n" +
		"unset surface\n" +
		"set contour\n" +
		"\n" +
		"unset key\n" +
		"\n" +
		"splot INPUT_FILE using 1:2:3 with line\n" +
		"\n" +
		"set output\n" +
		"# platform-independent way of restoring terminal by push/pop\n" +
		"set terminal pop\n\n";

	// YES once more...
	// takes 3 String parameters in order: data_file_name, x_label, y_label
	public static final String PLOT_CODE_PDF_2D_HEATMAP =
		"##### PUT THE NAME OF THE VARIABLES HERE #####\n" +
		"DATA_FILE='%s'\n" +	// parameter for data file name
		"X_LABEL='%s'\n" +	// parameter for xlabel
		"Y_LABEL='%s'\n" +	// parameter for ylabel
		"\n" +
		"##### DO NOT CHANGE ANYTING BELOW #####\n" +
		"PWD=\"`pwd`/\"\n" +
		"INPUT_FILE=PWD.DATA_FILE\n" +
		"TITLE=sprintf(\"Heatmap for: %%s versus %%s.\", X_LABEL, Y_LABEL)\n" +
		"\n" +
		"set terminal postscript eps enhanced 'Helvetica' 14\n" +
		"set output INPUT_FILE.'.heatmap.eps'\n" +
		"\n" +
		"#set encoding iso_8859_1\n" +
		"#set title TITLE\n" +
		"\n" +
		"# set key spacing 1 when outputting to .(e)ps\n" +
		"set key spacing 1\n" +
		"set xlabel X_LABEL\n" +
		"set ylabel Y_LABEL\n" +
		"set cblabel 'KDE'\n" +
		"\n" +
		"set xtics scale 0,0\n" +
		"set ytics scale 0,0\n" +
		"#set nocbtics\n" +
		"\n" +
		"#set style data lines\n" +
		"# range to large for most settings - picture will be single color only\n" +
		"#set cbrange [ -1.0 : 1.0 ] noreverse nowriteback\n" +
		"\n" +
		"#set view map scale 1\n" +
		"set view map\n" +
		"set colorbox\n" +
		"unset contour\n" +
		"\n" +
		"# use splot and pm3d to allow for smoothing in eps\n" +
		"set pm3d map\n" +
		"# set the level of smoothing (0,0 lets gnuplot decide)\n" +
		"set pm3d interpolate 0,0\n" +
		"\n" +
		"# palette useful only for color terminals (svg) - red/green instead of default\n" +
		"#set palette rgbformulae -7, 2, -7\n" +
		"\n" +
		"unset key\n" +
		"\n" +
		"# smoothed image - for pixelated image use 'splot INPUT_FILE with image'\n" +
		"splot INPUT_FILE\n" +
		"\n" +
		"set output\n" +
		"# platform-independent way of restoring terminal by push/pop\n" +
		"set terminal pop\n\n";
}
