package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.FileHandler.Action;
import nl.liacs.subdisc.Timer;

public class ResultWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private Table itsTable;
	private SearchParameters itsSearchParameters;
	private SubgroupDiscovery itsSubgroupDiscovery;
	private SubgroupSet itsSubgroupSet;
	private QualityMeasure itsQualityMeasure;

	private RegressionMeasure itsRegressionMeasureBase;
	private int itsFold;
	private BitSet itsBitSet;

	private ResultTableModel itsResultTableModel;
	private JTable itsSubgroupTable;

	//from mining window
	public ResultWindow(Table theTable, SubgroupDiscovery theSubgroupDiscovery, int theFold, BitSet theBitSet)
	{
		if (theTable == null || theSubgroupDiscovery == null)
		{
			Log.logCommandLine("ResultWindow Constructor: parameter(s) can not be 'null'.");
			return;
		}

		itsTable = theTable;
		itsSubgroupDiscovery = theSubgroupDiscovery;
		itsSubgroupSet = theSubgroupDiscovery.getResult();
		itsSearchParameters = theSubgroupDiscovery.getSearchParameters();
		itsQualityMeasure = theSubgroupDiscovery.getQualityMeasure();

		// only for DOUBLE_REGRESSION, avoids recalculation
		itsRegressionMeasureBase = theSubgroupDiscovery.getRegressionMeasureBase();

		// only used in MULTI_LABEL setting for now
		// if theFold == 0, itsBitSet is never used
		itsFold = theFold;
		itsBitSet = theBitSet;

		itsResultTableModel = new ResultTableModel(itsSubgroupSet, itsSearchParameters.getTargetConcept().getTargetType());
		itsSubgroupTable = new JTable(itsResultTableModel);
		if (!itsSubgroupSet.isEmpty())
			itsSubgroupTable.addRowSelectionInterval(0, 0);
		initialise();
	}

	//from parent result window (pattern team)
	public ResultWindow(ResultWindow theParentWindow, SubgroupSet thePatternTeam)
	{
		itsTable = theParentWindow.itsTable;
		itsSubgroupDiscovery = theParentWindow.itsSubgroupDiscovery;
		itsSubgroupSet = thePatternTeam;
		itsSearchParameters = itsSubgroupDiscovery.getSearchParameters();
		itsQualityMeasure = itsSubgroupDiscovery.getQualityMeasure();

		// only for DOUBLE_REGRESSION, avoids recalculation
		itsRegressionMeasureBase = itsSubgroupDiscovery.getRegressionMeasureBase();

		itsResultTableModel = new ResultTableModel(itsSubgroupSet, itsSearchParameters.getTargetConcept().getTargetType());
		itsSubgroupTable = new JTable(itsResultTableModel);
		if (!itsSubgroupSet.isEmpty())
			itsSubgroupTable.addRowSelectionInterval(0, 0);
		initialise();
	}

	private void initialise()
	{
		initComponents();

		// NOTE scaling is based on 8 columns, there are 20 unitWidths
		int aUnitWidth = (int)(0.05f * GUI.WINDOW_DEFAULT_SIZE.width);

		itsSubgroupTable.getColumnModel().getColumn(0).setPreferredWidth(aUnitWidth);
		itsSubgroupTable.getColumnModel().getColumn(1).setPreferredWidth(aUnitWidth);
		itsSubgroupTable.getColumnModel().getColumn(2).setPreferredWidth((int)(1.75f * aUnitWidth));
		itsSubgroupTable.getColumnModel().getColumn(3).setPreferredWidth((int)(1.75f * aUnitWidth));
		itsSubgroupTable.getColumnModel().getColumn(4).setPreferredWidth((int)(1.75f * aUnitWidth));
		itsSubgroupTable.getColumnModel().getColumn(5).setPreferredWidth((int)(1.75f * aUnitWidth));
		itsSubgroupTable.getColumnModel().getColumn(6).setPreferredWidth(2 * aUnitWidth);
		itsSubgroupTable.getColumnModel().getColumn(7).setPreferredWidth(9 * aUnitWidth);

		itsScrollPane.add(itsSubgroupTable);
		itsScrollPane.setViewportView(itsSubgroupTable);

		itsSubgroupTable.setRowSelectionAllowed(true);
		itsSubgroupTable.setColumnSelectionAllowed(false);
		itsSubgroupTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//sorting of rows disables, as it destroys the order of subgroups
		//itsSubgroupTable.setAutoCreateRowSorter(true);

		itsSubgroupTable.addKeyListener(new KeyListener()
		{
			public void keyPressed(KeyEvent key)
			{
				if (key.getKeyCode() == KeyEvent.VK_ENTER)
					if (itsSubgroupTable.getRowCount() > 0 && itsSearchParameters.getTargetType() != TargetType.SINGLE_NOMINAL)
						jButtonShowModelActionPerformed();
				if (key.getKeyCode() == KeyEvent.VK_DELETE)
					if(itsSubgroupTable.getRowCount() > 0)
						jButtonDeleteSubgroupsActionPerformed();
			}

			public void keyReleased(KeyEvent key) {}

			public void keyTyped(KeyEvent key) {}
		});

		setTitle();
		setIconImage(MiningWindow.ICON);
		setLocation(200, 200);
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
		setAlwaysOnTop(true);
		setAlwaysOnTop(false);
	}

	private void initComponents()
	{
		jPanelSouth = new JPanel(new GridLayout(3, 1));
		JPanel aSubgroupPanel = new JPanel();
		JPanel aSubgroupSetPanel = new JPanel();
		JPanel aClosePanel = new JPanel();

		itsScrollPane = new JScrollPane();

		//selected subgroups ********************************

		jButtonShowModel = GUI.buildButton("Show Model", 'M', "model", this);
		aSubgroupPanel.add(jButtonShowModel);

		jButtonBrowseSubgroups = GUI.buildButton("Browse", 'B', "browse", this);
		aSubgroupPanel.add(jButtonBrowseSubgroups);

		jButtonDeleteSubgroups = GUI.buildButton("Delete", 'D', "delete", this);
		aSubgroupPanel.add(jButtonDeleteSubgroups);

		//subgroup set *********************************

		jButtonPatternTeam = GUI.buildButton("Pattern Team", 'P', "patternteam", this);
		aSubgroupSetPanel.add(jButtonPatternTeam);

		jButtonROC = GUI.buildButton("ROC", 'R', "roc", this);
		jButtonROC.setPreferredSize(GUI.BUTTON_MEDIUM_SIZE);
		aSubgroupSetPanel.add(jButtonROC);

		jButtonSave = GUI.buildButton("Save", 'S', "save", this);
		jButtonSave.setPreferredSize(GUI.BUTTON_MEDIUM_SIZE);
		aSubgroupSetPanel.add(jButtonSave);

		jButtonPrint = GUI.buildButton("Print", 'I', "print", this);
		jButtonPrint.setPreferredSize(GUI.BUTTON_MEDIUM_SIZE);
		aSubgroupSetPanel.add(jButtonPrint);

		jButtonPValues = GUI.buildButton("Gaussian p-Values", 'V', "compute_p", this);
		aSubgroupSetPanel.add(jButtonPValues);

		jButtonRegressionTest = GUI.buildButton("Regression Test", 'T', "regression", this);
		aSubgroupSetPanel.add(jButtonRegressionTest);

		jButtonEmpirical = GUI.buildButton("Empirical p-Values", 'E', "empirical_p", this);
		aSubgroupSetPanel.add(jButtonEmpirical);

		jButtonFold = GUI.buildButton("Fold members", 'F', "fold", this);
		aSubgroupSetPanel.add(jButtonFold);

		//close *********************************

		jButtonCloseWindow = GUI.buildButton("Close", 'C', "close", this);
		aClosePanel.add(jButtonCloseWindow);

		enableButtonsCheck();
		jPanelSouth.add(aSubgroupPanel);
		jPanelSouth.add(aSubgroupSetPanel);
		jPanelSouth.add(aClosePanel);
		getContentPane().add(jPanelSouth, BorderLayout.SOUTH);
		getContentPane().add(itsScrollPane, BorderLayout.CENTER);
	}

	public void setTitle()
	{
		StringBuilder s = new StringBuilder(150);
		if (itsSubgroupSet.isEmpty())
			s.append("No subgroups found that match the set criterion");
		else
			s.append(itsSubgroupSet.size() + " subgroups found");

		TargetType aTargetType = itsSearchParameters.getTargetType();
		TargetConcept aTC = itsSearchParameters.getTargetConcept();

		if (aTargetType == TargetType.SINGLE_NOMINAL || aTargetType == TargetType.SINGLE_NUMERIC)
		{
			s.append("; target = ");
			s.append(aTC.getPrimaryTarget().getName());
		}

		if (TargetType.hasTargetValue(aTargetType))
		{
			s.append("; value = ");
			if (aTargetType == TargetType.SINGLE_NOMINAL)
				s.append(itsSearchParameters.getTargetConcept().getTargetValue());
			// else DOUBLE_REGRESSION or DOUBLE_CORRELATION
			else
				s.append(itsSearchParameters.getTargetConcept().getSecondaryTarget().getName());
		}
		if (itsFold != 0)
			s.append("; fold = " + itsFold);

		s.append("; quality measure = ");
		s.append(itsSearchParameters.getQualityMeasure().GUI_TEXT);

		NumberFormat aFormatter = NumberFormat.getNumberInstance();
		aFormatter.setMaximumFractionDigits(3);
		if (!Double.isNaN(itsSubgroupSet.getJointEntropy()))
			s.append("; joint entropy = " + aFormatter.format(itsSubgroupSet.getJointEntropy()));
		setTitle(s.toString());
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String aCommand = theEvent.getActionCommand();
		if ("model".equals(aCommand))
			jButtonShowModelActionPerformed();
		else if ("patternteam".equals(aCommand))
			jButtonPatternTeamActionPerformed();
		else if ("roc".equals(aCommand))
			jButtonROCActionPerformed();
		else if ("browse".equals(aCommand))
			jButtonBrowseSubgroupsActionPerformed();
		else if ("delete".equals(aCommand))
			jButtonDeleteSubgroupsActionPerformed();
		else if ("compute_p".equals(aCommand))
			jButtonPValuesActionPerformed();
		else if ("regression".equals(aCommand))
			jButtonRegressionTestActionPerformed();
		else if ("empirical_p".equals(aCommand))
			jButtonEmpiricalActionPerformed();
		else if ("fold".equals(aCommand))
			jButtonFoldActionPerformed();
		else if ("save".equals(aCommand))
			jButtonSaveActionPerformed();
		else if ("print".equals(aCommand))
			jButtonPrintActionPerformed();
		else if ("close".equals(aCommand))
			dispose();
	}

	private void enableButtonsCheck()
	{
		if (itsSubgroupSet.isEmpty())
		{
			jButtonShowModel.setEnabled(false);
			jButtonROC.setEnabled(false);
			jButtonPatternTeam.setEnabled(false);
			jButtonBrowseSubgroups.setEnabled(false);
			jButtonDeleteSubgroups.setEnabled(false);
			jButtonPValues.setEnabled(false);
			jButtonRegressionTest.setEnabled(false);
			jButtonEmpirical.setEnabled(false);
			jButtonFold.setEnabled(false);
			jButtonSave.setEnabled(false);
			jButtonPrint.setEnabled(false);
			return;
		}
		else
		{
			TargetType aTargetType = itsSearchParameters.getTargetType();

			jButtonShowModel.setVisible(TargetType.hasBaseModel(aTargetType));
			jButtonROC.setVisible(aTargetType == TargetType.SINGLE_NOMINAL);

			jButtonFold.setVisible(itsFold != 0);
		}
	}

	private void jButtonShowModelActionPerformed()
	{
		Subgroup[] aSelectedSubgroups = getSelectedSubgroups();
		if (aSelectedSubgroups.length == 0)
			return;

		switch (itsSearchParameters.getTargetType())
		{
			case SINGLE_NUMERIC :
			{
				showModelWindowSingleNumeric(aSelectedSubgroups);
				break;
			}
			case MULTI_NUMERIC :
			{
				// FIXME MM ShowModel only for 2D targets
				showModelWindowMultiNumeric(aSelectedSubgroups);
				break;
			}
			case DOUBLE_REGRESSION :
			{
				showModelWindowDoubleCorrelationDoubleRegression(TargetType.DOUBLE_REGRESSION, aSelectedSubgroups);
				break;
			}
			case DOUBLE_CORRELATION :
			{
				showModelWindowDoubleCorrelationDoubleRegression(TargetType.DOUBLE_CORRELATION, aSelectedSubgroups);
				break;
			}
			case SCAPE :
			{
				showModelWindowScape(aSelectedSubgroups);
				break;
			}
			case MULTI_LABEL :
			{
				showModelWindowMultiLabel(aSelectedSubgroups);
				break;
			}
			case LABEL_RANKING :
			{
				showModelWindowLabelRanking(aSelectedSubgroups);
				break;
			}
			default :
				throw new AssertionError(itsSearchParameters.getTargetType());
		}
	}

	private final void showModelWindowSingleNumeric(Subgroup[] theSelectedSubgroups)
	{
		Column aTarget = itsSearchParameters.getTargetConcept().getPrimaryTarget();
		ProbabilityDensityFunction aPDF;
		// DEBUG
		if (!ProbabilityDensityFunction.USE_ProbabilityDensityFunction2)
			aPDF = new ProbabilityDensityFunction(aTarget);
		else
			aPDF = new ProbabilityDensityFunction2(aTarget);
		aPDF.smooth();

		for (Subgroup s : theSelectedSubgroups)
		{
			ProbabilityDensityFunction aSubgroupPDF;
			// DEBUG
			if (!ProbabilityDensityFunction.USE_ProbabilityDensityFunction2)
				aSubgroupPDF = new ProbabilityDensityFunction(aPDF, s.getMembers());
			else
				aSubgroupPDF = new ProbabilityDensityFunction2(aPDF, s.getMembers());
			aSubgroupPDF.smooth();
			new ModelWindow(aTarget, aPDF, aSubgroupPDF, createTitle(s), false);
		}
	}

//	private final void showModelWindowMultiNumeric(Subgroup[] theSelectedSubgroups)
//	{
//// CODE IS ALSO USED IN MiningWindow showModel()
//		List<Column> aList = itsSearchParameters.getTargetConcept().getMultiTargets();
//		if (aList .size() != 2)
//			throw new AssertionError(TargetType.MULTI_NUMERIC + " base model only available for 2 dimensions");
//
//Timer t1 = new Timer();
//		// compute model for each subgroup
//		for (Subgroup s : theSelectedSubgroups)
//		{
//			setBusy(true);
//			ProbabilityDensityFunction2_2D aPdf = new ProbabilityDensityFunction2_2D(aList, s.getMembers());
//System.out.println("OLD: " + t1.getElapsedTimeString());
//
//Timer t2 = new Timer();
//float[][][] grids = itsSubgroupDiscovery.itsPDF_ND.getDensityDifference2D(s.getMembers(), true, null);
//System.out.println("NEW: "+ t2.getElapsedTimeString());
//			setBusy(false);
//
//			new PDFWindow2D(aPdf, createTitle(s), aList.get(0).getName(), aList.get(1).getName());
//		}
//
//		// DEBUG -- DO NOT WRITE OUT FILES
//		if (true)
//			return;
//
//		ProbabilityDensityFunction_ND aPdf = itsSubgroupDiscovery.itsPDF_ND;
//
//		for (Subgroup s : theSelectedSubgroups)
//			writePdfs(aPdf, s);
//	}

	private final void showModelWindowMultiNumeric(Subgroup[] theSelectedSubgroups)
	{
// CODE IS ALSO USED IN MiningWindow showModel()
		List<Column> aList = itsSearchParameters.getTargetConcept().getMultiTargets();
		if (aList .size() != 2)
			throw new AssertionError(TargetType.MULTI_NUMERIC + " base model only available for 2 dimensions");

		ProbabilityDensityFunction_ND aPdf = itsSubgroupDiscovery.itsPDF_ND;
		double[][] aLimits = aPdf.getLimits();

		for (Subgroup s : theSelectedSubgroups)
		{
			setBusy(true);
			// TODO MM synchronized to ensure the stats obtained are
			// for the retrieved grid
			// this part of the code is not multi-threaded (yet)
			// the code in ProbabilityDensityFunction_ND is clumsy
			// and error prone and will be cleaned one day
			float[][][] aGrids;
			double[] aStats;
			synchronized (aPdf)
			{
				// { PDF_SG, PDF_COMP, PDF_DIFF}
				aGrids = aPdf.getDensityDifference2D(s.getMembers(), true, null);
				aStats = aPdf.lastDXDY;
			}
			setBusy(false);

			float aNormaliser = normaliseGrids(aGrids);

			new PDFWindow2D(aGrids, aStats, aLimits, createTitle(s), aList.get(1).getName(), aList.get(0).getName());

			// DEBUG -- DO NOT WRITE OUT FILES
			if (false) return;

			writePdfs(s, aGrids, aStats, aNormaliser, aList.get(0).getName(), aList.get(1).getName());
		}
	}

	private static final float normaliseGrids(float[][][] theGrids)
	{
		float aMax = normaliseGridsGetMax(theGrids);

		for (float[][] theGrid : theGrids)
			for (float[] row : theGrid)
				for (int i = 0, j = row.length; i < j; ++i)
					row[i] /= aMax;

		// normaliser will be written out to .dat files
		return aMax;
	}

	/*
	 * finds the MAX_VALUE over all 3 grids of SG | COMPL | DIFF
	 * this will be used as normalisation factor such that all plots will
	 * be on the same (pdf) scale
	 */
	private static final float normaliseGridsGetMax(float[][][] theGrids)
	{
		float max = -Float.MAX_VALUE;

		for (float[][] theGrid : theGrids)
			for (float[] row : theGrid)
				for (float z : row)
					if (z > max)
						max = z;

		return max;
	}

	private static final void writePdfs(Subgroup theSubgroup, float[][][] theGrids, double[] theStats, float theNormaliser, String aX, String aY)
	{
		Log.logCommandLine("writing pdf grid files for: " + createTitle(theSubgroup));

		// avoids overwriting + keeps 3 grids together in file browsers
		String s = String.format("%s_%s_%s%s", System.nanoTime(), "%s", theSubgroup.getID(), ".dat");
		String info = createTitle(theSubgroup);

		writePdf(theGrids[0], theStats, theNormaliser, String.format(s, "subgroup"), info);
		writeGnuplotScript(String.format(s, "subgroup"), aX, aY, false);
		writeGnuplotScript(String.format(s, "subgroup"), aX, aY, true);
		writePdf(theGrids[1], theStats, theNormaliser, String.format(s, "complement"), info);
		writeGnuplotScript(String.format(s, "complement"), aX, aY, false);
		writeGnuplotScript(String.format(s, "complement"), aX, aY, true);
		writePdf(theGrids[2], theStats, theNormaliser, String.format(s, "difference"), info);
		writeGnuplotScript(String.format(s, "difference"), aX, aY, false);
		writeGnuplotScript(String.format(s, "difference"), aX, aY, true);
	}

	private static final void writePdf(float[][] theGrid, double[] theStats, float theNormaliser, String theFileName, String theSubgroupInfo)
	{
		BufferedWriter bw = null;
		try
		{
			File f = new File(theFileName);
			bw = new BufferedWriter(new FileWriter(f));
			Log.logCommandLine("writing: " + f.getAbsolutePath());

			double x_min = theStats[0];
			double y_min = theStats[3];
			double dx = theStats[6];
			double dy = theStats[7];

			bw.write("# " + theSubgroupInfo + "\n");
			bw.write("# stats " + Arrays.toString(theStats) + "\n");
			bw.write("# normalisation " + Double.toString(theNormaliser) + "\n");
			bw.write("# y\tx\tz\n");	// NOTICE SWAP!!!

			// x-y axis are swapped, simplifies loop a lot
			for (int i = 0; i < theGrid.length; ++i)
			{
				float[] row = theGrid[i];

				double x = x_min + (dx * i);
				for (int j = 0; j < row.length; ++j)
				{
					double y = y_min + (dy * j);
					double z = row[j];
					// NOTICE SWAP!!!
					bw.write(y + "\t" + x + "\t" + z + "\n");
				}
				bw.write("\n"); // required by gnuplot at y-change
			}

			Log.logCommandLine("Done\n");
		}
		catch (IOException e)
		{
			// TODO MM
			e.printStackTrace();
		}
		finally
		{
			if (bw != null)
			try
			{
				bw.close();
			}
			catch (IOException e)
			{
				// TODO MM
				e.printStackTrace();
			}
		}
	}

	private static final void writeGnuplotScript(String theDataFile, String aX, String aY, boolean isHeatMap)
	{
		BufferedWriter bw = null;
		try
		{
			String ext = isHeatMap ? ".heatmap.gp" : ".gp";
			File f = new File(theDataFile + ext);
			bw = new BufferedWriter(new FileWriter(f));
			Log.logCommandLine("writing: " + f.getAbsolutePath());
			String script = isHeatMap ?
					Gnuplot.PLOT_CODE_PDF_2D_HEATMAP :
					Gnuplot.PLOT_CODE_PDF_2D;
			// NOTICE SWAP OF X AND Y
			bw.write(String.format(script, theDataFile, aY, aX));
			Log.logCommandLine("Done\n");
		}
		catch (IOException e)
		{
			// TODO MM
			e.printStackTrace();
		}
		finally
		{
			if (bw != null)
			try
			{
				bw.close();
			}
			catch (IOException e)
			{
				// TODO MM
				e.printStackTrace();
			}
		}
	}

	private final void showModelWindowDoubleCorrelationDoubleRegression(TargetType theTargetType, Subgroup[] theSelectedSubgroups)
	{
		// isRegressionSetting relies on binary choice
		assert (theTargetType == TargetType.DOUBLE_CORRELATION || theTargetType == TargetType.DOUBLE_REGRESSION);

		final TargetConcept aTargetConcept = itsSearchParameters.getTargetConcept();
		final boolean isRegressionSetting = (theTargetType == TargetType.DOUBLE_REGRESSION);
		RegressionMeasure aRM = null;

		for (Subgroup s : theSelectedSubgroups)
		{
			if (isRegressionSetting)
			{
				aRM = new RegressionMeasure(itsRegressionMeasureBase, s.getMembers());
				aRM.getEvaluationMeasureValue();
			}

			new ModelWindow(itsTable,
					aTargetConcept.getPrimaryTarget(),
					aTargetConcept.getSecondaryTarget(),
					aRM,
					s);
		}
	}

	private final void showModelWindowScape(Subgroup[] theSelectedSubgroups)
	{
		Column aBinaryTarget = itsSearchParameters.getTargetConcept().getPrimaryTarget();
		Column aNumericTarget = itsSearchParameters.getTargetConcept().getSecondaryTarget();
		BitSet aBinaries = aBinaryTarget.getBinaries();
		ProbabilityDensityFunction aPDF;
		// DEBUG
		if (!ProbabilityDensityFunction.USE_ProbabilityDensityFunction2)
			aPDF = new ProbabilityDensityFunction(aNumericTarget);
		else
			aPDF = new ProbabilityDensityFunction2(aNumericTarget);
		aPDF.smooth();

		for (Subgroup s : theSelectedSubgroups)
		{
			BitSet aPositiveMembers = s.getMembers();
			aPositiveMembers.and(aBinaries);

			BitSet aNegativeMembers = s.getMembers();
			BitSet aNonTarget = (BitSet) aBinaries.clone();
			aNonTarget.flip(0,aNonTarget.length());
			aNegativeMembers.and(aNonTarget);

			ProbabilityDensityFunction aPositivePDF;
			// DEBUG
			if (!ProbabilityDensityFunction.USE_ProbabilityDensityFunction2)
				aPositivePDF = new ProbabilityDensityFunction(aPDF, aPositiveMembers);
			else
				aPositivePDF = new ProbabilityDensityFunction2(aPDF, aPositiveMembers);
			aPositivePDF.smooth();

			ProbabilityDensityFunction aNegativePDF;
			// DEBUG
			if (!ProbabilityDensityFunction.USE_ProbabilityDensityFunction2)
				aNegativePDF = new ProbabilityDensityFunction(aPDF, aNegativeMembers);
			else
				aNegativePDF = new ProbabilityDensityFunction2(aPDF, aNegativeMembers);
			aNegativePDF.smooth();

			new ModelWindow(aNumericTarget, aPositivePDF, aNegativePDF, createTitle(s), true);
		}
	}

	/*
	 * TODO inefficient loop
	 * use convertViewToModel(i) to select correct s, then use s.ID
	 */
	// FIXME MM CLEANUP: use getSelectedSubgroups()
	// when ResultWindow sorting is re-enabled the use of i-indices fails
	private final void showModelWindowMultiLabel(Subgroup[] theSelectedSubgroups)
	{
		int[] aSelectionIndex = itsSubgroupTable.getSelectedRows();
		int i = 0;
		while (i < aSelectionIndex.length)
		{
			Log.logCommandLine("subgroup " + (aSelectionIndex[i]+1));
			int aCount = 0;
			for (Subgroup s : itsSubgroupSet)
			{
				if (aCount == aSelectionIndex[i])
					new ModelWindow(s.getDAG(), 1000, 800)
						.setTitle("Subgroup " +
							Integer.toString(aSelectionIndex[i]+1) +
							": induced Bayesian Network");

				aCount++;
			}

			i++;
		}
	}

	private void showModelWindowLabelRanking(Subgroup[] theSelectedSubgroups)
	{
		for (Subgroup s : theSelectedSubgroups)
		{
			LabelRankingMatrix aLRM = s.getLabelRankingMatrix();
			aLRM.print();
			new LabelRankingMatrixWindow(itsQualityMeasure.getBaseLabelRankingMatrix(),
							aLRM,
							createTitle(s) + "   " + s.getLabelRanking().getRanking());
		}
	}

	private void jButtonROCActionPerformed()
	{
		new ROCCurveWindow(itsSubgroupSet, itsSearchParameters, itsQualityMeasure);
	}

	private void jButtonPatternTeamActionPerformed()
	{
		String anInputString= JOptionPane.showInputDialog("Set pattern team size:");
		if (anInputString == null || anInputString.equals(""))
			return;

		setBusy(true);
		try
		{
			int aValue = new Integer(anInputString).intValue();
			SubgroupSet aPatternTeam = itsSubgroupSet.getPatternTeam(itsTable, aValue);
			new ResultWindow(this, aPatternTeam);
		}
		catch (Exception e)
		{
			Log.logCommandLine(e.toString());
			JOptionPane.showMessageDialog(null, "Not a valid input value!", "Warning", JOptionPane.ERROR_MESSAGE);
		}
		setBusy(false);
	}

	private void jButtonBrowseSubgroupsActionPerformed()
	{
		for (Subgroup s : getSelectedSubgroups())
			new BrowseWindow(itsTable, s);
	}

	private void jButtonDeleteSubgroupsActionPerformed()
	{
		if (itsSubgroupSet.isEmpty())
			return;

		int aSubgroupSetIndex = itsSubgroupSet.size();
		int[] aSelectionIndex = itsSubgroupTable.getSelectedRows();
		Iterator<Subgroup> anIterator = itsSubgroupSet.descendingIterator();

		for (int i = aSelectionIndex.length - 1; i >= 0; i--)
		{
			while (--aSubgroupSetIndex >= aSelectionIndex[i])
				anIterator.next();
			anIterator.remove();

			if (anIterator.hasNext())
				anIterator.next();
		}
		itsSubgroupTable.getSelectionModel().clearSelection();
		itsSubgroupTable.repaint();

		if (!itsSubgroupSet.isEmpty())
			itsSubgroupTable.addRowSelectionInterval(0, 0);
		else
			jButtonDeleteSubgroups.setEnabled(false);
	}

	private void jButtonPValuesActionPerformed()
	{
		setBusy(true);
		// Obtain input
		double[] aQualities = obtainRandomQualities();
		if (aQualities == null)
			return;
		NormalDistribution aDistro = new NormalDistribution(aQualities);

		for (Subgroup aSubgroup : itsSubgroupSet)
			aSubgroup.setPValue(aDistro);

		itsSubgroupTable.repaint();
		setBusy(false);
	}

	private void jButtonRegressionTestActionPerformed()
	{
		setBusy(true);
		// Obtain input
		double[] aQualities = obtainRandomQualities();
		if (aQualities == null)
			return;
		Validation aValidation = new Validation(itsSearchParameters, itsTable, itsQualityMeasure);
		double[] aRegressionTestScore = aValidation.performRegressionTest(aQualities, itsSubgroupSet);
		setBusy(false);
		JOptionPane.showMessageDialog(null, "The regression test score equals\nfor k =  1 : "+aRegressionTestScore[0]+"\nfor k = 10 : "+aRegressionTestScore[1]);
	}

	private void jButtonEmpiricalActionPerformed()
	{
		setBusy(true);
		// Obtain input
		double[] aQualities = obtainRandomQualities();
		if ( aQualities == null)
			return;

		Validation aValidation = new Validation(itsSearchParameters, itsTable, itsQualityMeasure);
		double aPValue = aValidation.computeEmpiricalPValue(aQualities, itsSubgroupSet);
		JOptionPane.showMessageDialog(this, "The empirical p-value is p = " + aPValue);

		for (Subgroup aSubgroup : itsSubgroupSet)
			aSubgroup.setEmpiricalPValue(aQualities);

		itsSubgroupTable.repaint();
		setBusy(false);
	}

	private void jButtonFoldActionPerformed()
	{
		Log.logCommandLine("Members of the training set of fold " + itsFold);
		Log.logCommandLine(itsBitSet.toString());
	}

	private double[] obtainRandomQualities()
	{
		String[] aSetup = new RandomQualitiesWindow(itsSearchParameters.getTargetType()).getSettings();

		Log.logCommandLine(aSetup[0] + "=" + aSetup[1]);
		if (!RandomQualitiesWindow.isValidRandomQualitiesSetup(aSetup))
			return null;

		// Compute qualities
		Validation aValidation = new Validation(itsSearchParameters, itsTable, itsQualityMeasure);
		return aValidation.getQualities(aSetup);
	}

	private void jButtonSaveActionPerformed()
	{
		File aFile = new FileHandler(Action.SAVE).getFile();
		if (aFile == null)
			return; // cancelled

		XMLAutoRun.save(itsSubgroupSet, aFile.getAbsolutePath(), itsSearchParameters.getTargetType());
	}

	private void jButtonPrintActionPerformed()
	{
		try
		{
			itsSubgroupTable.print();
		}
		catch (PrinterException e)
		{
			JOptionPane.showMessageDialog(this,
							"Print error!",
							"Warning",
							JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
		}
	}

	private void setBusy(boolean isBusy)
	{
		if (isBusy)
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		else
			this.setCursor(Cursor.getDefaultCursor());
	}

	// loop over itsSubgroupSet instead of using .toArray() and then select
	// required items --- itsSubgroupSet can be very large
	// the selection is probably small
	// TODO MM convertViewToModel(i) would allow sorting of result table
	private Subgroup[] getSelectedSubgroups()
	{
		int[] aSelectedRows = itsSubgroupTable.getSelectedRows();
		Subgroup[] aSelectedSubgroups = new Subgroup[aSelectedRows.length];
		Iterator<Subgroup> anIterator = itsSubgroupSet.iterator();

		for (int i = 0, j = aSelectedRows.length, k = 0; k < j; ++i, ++k)
		{
			int aNext = aSelectedRows[k];
			while (i != aNext)
			{
				anIterator.next();
				++i;
			}
			aSelectedSubgroups[k] = anIterator.next();
		}

		return aSelectedSubgroups;
	}

	/**
	 * Creates a title for model windows, based on subgroup id and 
	 * description.
	 * 
	 * @param theSubgroup from which to derive the title.
	 * 
	 * @return the title to be used for a model window.
	 * 
	 * @throws IllegalArgumentException if the argument is {@code null}.
	 */
	static final String createTitle(Subgroup theSubgroup)
	{
		return String.format("Subgroup %d (%s)",
					theSubgroup.getID(),
					theSubgroup.toString());
	}

	private JPanel jPanelSouth;
	private JButton jButtonShowModel;
	private JButton jButtonBrowseSubgroups;
	private JButton jButtonDeleteSubgroups;
	private JButton jButtonFold;
	private JButton jButtonPValues;
	private JButton jButtonRegressionTest;
	private JButton jButtonEmpirical;
	private JButton jButtonPatternTeam;
	private JButton jButtonROC;
	private JButton jButtonSave;
	private JButton jButtonPrint;
	private JButton jButtonCloseWindow;
	private JScrollPane itsScrollPane;
}
