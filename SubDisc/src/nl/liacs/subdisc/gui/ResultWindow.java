package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.FileHandler.Action;

public class ResultWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	private SearchParameters itsSearchParameters;
	private ResultTableModel itsResultTableModel;
	private JTable itsSubgroupTable;
	private SubgroupSet itsSubgroupSet;
//	private DAGView itsDAGView; //layout of the graph on the whole database
	private Table itsTable;
	private BinaryTable itsBinaryTable;
	private QualityMeasure itsQualityMeasure;
	private int itsNrRecords;
	private int itsFold;
	private BitSet itsBitSet;

//	public ResultWindow(Table theTable, SubgroupDiscovery theSubgroupDiscovery, DAGView theDAGView, BinaryTable theBinaryTable, int theFold, BitSet theBitSet)
	public ResultWindow(Table theTable, SubgroupDiscovery theSubgroupDiscovery, BinaryTable theBinaryTable, int theFold, BitSet theBitSet)
	{
		if (theTable == null ||theSubgroupDiscovery == null)
		{
			Log.logCommandLine("ResultWindow Constructor: parameter(s) can not be 'null'.");

			return;
		}
		else
		{
			itsTable = theTable;
			itsNrRecords = itsTable.getNrRows();
			itsSubgroupSet = theSubgroupDiscovery.getResult();
			itsSearchParameters = theSubgroupDiscovery.getSearchParameters();
			itsQualityMeasure = theSubgroupDiscovery.getQualityMeasure();
//			itsDAGView = theDAGView;
			itsBinaryTable = theBinaryTable;
			itsFold = theFold; // only used in MULTI_LABEL setting for now
			itsBitSet = theBitSet; // only used in MULTI_LABEL setting for now

			itsResultTableModel = new ResultTableModel(itsSubgroupSet);
			itsSubgroupTable = new JTable(itsResultTableModel);
			if (!itsSubgroupSet.isEmpty())
				itsSubgroupTable.addRowSelectionInterval(0, 0);

			initComponents ();
			initialise();
			setTitle();
			setIconImage(MiningWindow.ICON);
			setLocation(100, 100);
			setSize(GUI.WINDOW_DEFAULT_SIZE);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setVisible(true);
		}
	}

	//only used for post-processing
	private ResultWindow(Table theTable, SubgroupSet theSubgroupSet, SearchParameters theSearchParameters, QualityMeasure theQualityMeasure, BinaryTable theBinaryTable, int theFold, BitSet theBitSet)
	{
		itsTable = theTable;
		itsNrRecords = itsTable.getNrRows();
		itsSubgroupSet = theSubgroupSet;
		itsSearchParameters = theSearchParameters;
		itsQualityMeasure = theQualityMeasure;
//		itsDAGView = theDAGView;
		itsBinaryTable = theBinaryTable;
		itsFold = theFold;
		itsBitSet = theBitSet;

		itsResultTableModel = new ResultTableModel(itsSubgroupSet);
		itsSubgroupTable = new JTable(itsResultTableModel);
		if (!itsSubgroupSet.isEmpty())
			itsSubgroupTable.addRowSelectionInterval(0, 0);

		initComponents ();
		initialise();
		setTitle();
		setIconImage(MiningWindow.ICON);
		setLocation(100, 100);
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

/*
	public ResultWindow(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters, DAGView theDAGView, Table theTable, QualityMeasure theQualityMeasure, int theNrRecords, int theFold, BitSet theBitSet)
	{
		itsSubgroupSet = theSubgroupSet;
		itsSearchParameters = theSearchParameters;
		itsDAGView = theDAGView;
		itsResultTableModel = new ResultTableModel(itsSubgroupSet);
		itsSubgroupTable = new JTable(itsResultTableModel);
		if (!itsSubgroupSet.isEmpty())
			itsSubgroupTable.addRowSelectionInterval(0, 0);

		itsTable = theTable;
		itsBinaryTable = null;
		itsQualityMeasure = theQualityMeasure;
		itsNrRecords = theNrRecords;
		itsFold = theFold;
		itsBitSet = theBitSet;

		initComponents ();
//		setIconImage(ICON);
		initialise();

		setTitle();
	}

	public ResultWindow(SubgroupSet theSubgroupSet, SearchParameters theSearchParameters, DAGView theDAGView, Table theTable, BinaryTable theBinaryTable, QualityMeasure theQualityMeasure, int theNrRecords, int theFold, BitSet theBitSet)
	{
		itsSubgroupSet = theSubgroupSet;
		itsSearchParameters = theSearchParameters;
		itsDAGView = theDAGView;
		itsResultTableModel = new ResultTableModel(itsSubgroupSet);
		itsSubgroupTable = new JTable(itsResultTableModel);
		if (!itsSubgroupSet.isEmpty())
			itsSubgroupTable.addRowSelectionInterval(0, 0);

		initComponents ();
//		setIconImage(ICON);
		initialise();

		itsTable = theTable;
		itsBinaryTable = theBinaryTable;
		itsQualityMeasure = theQualityMeasure;
		itsNrRecords = theNrRecords;
		itsFold = theFold;
		itsBitSet = theBitSet;

		setTitle();
	}
*/

	public void setTitle()
	{
		StringBuilder s = new StringBuilder(150);
		if (itsSubgroupSet.isEmpty())
			s.append("No subgroups found that match the set criterion");
		else
			s.append(itsSubgroupSet.size() + " subgroups found");

		s.append(";  target table = ");
		s.append(itsTable.getName());

		s.append(";  quality measure = ");
		s.append(QualityMeasure.getMeasureString(itsSearchParameters.getQualityMeasure()));

		TargetType aTargetType = itsSearchParameters.getTargetType();
		if (TargetType.hasTargetValue(aTargetType))
		{
			s.append(";  target value = ");
			if (aTargetType == TargetType.SINGLE_NOMINAL)
				s.append(itsSearchParameters.getTargetConcept().getTargetValue());
			// else DOUBLE_REGRESSION or DOUBLE_CORRELATION
			else
				s.append(itsSearchParameters.getTargetConcept().getSecondaryTarget().getName());
		}
		if (itsFold != 0)
			s.append(";  fold = " + itsFold);
		setTitle(s.toString());
	}

	public void initialise()
	{
		// NOTE scaling is based on the assumption of 6 columns being present
		int aUnitWidth = (int)(0.05f * GUI.WINDOW_DEFAULT_SIZE.width);

		itsSubgroupTable.getColumnModel().getColumn(0).setPreferredWidth(aUnitWidth);
		itsSubgroupTable.getColumnModel().getColumn(1).setPreferredWidth(aUnitWidth);
		itsSubgroupTable.getColumnModel().getColumn(2).setPreferredWidth((int)(1.5f * aUnitWidth));
		itsSubgroupTable.getColumnModel().getColumn(3).setPreferredWidth((int)(1.5f * aUnitWidth));
		itsSubgroupTable.getColumnModel().getColumn(4).setPreferredWidth(2 * aUnitWidth);
		itsSubgroupTable.getColumnModel().getColumn(5).setPreferredWidth(13 * aUnitWidth);

		itsScrollPane.add(itsSubgroupTable);
		itsScrollPane.setViewportView(itsSubgroupTable);

		itsSubgroupTable.setRowSelectionAllowed(true);
		itsSubgroupTable.setColumnSelectionAllowed(false);
		itsSubgroupTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

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
	}

	private void initComponents()
	{
		jPanelSouth = new JPanel(new GridLayout(2, 1));
		JPanel aSubgroupPanel = new JPanel();
		JPanel aSubgroupSetPanel = new JPanel();

		itsScrollPane = new JScrollPane();

		jButtonShowModel = GUI.buildButton("Show Model", 'M', "model", this);
		aSubgroupPanel.add(jButtonShowModel);
//		jButtonShowModel.setVisible(itsSearchParameters.getTargetType() != TargetType.SINGLE_NOMINAL);

		jButtonROC = GUI.buildButton("ROC", 'R', "roc", this);
		aSubgroupPanel.add(jButtonROC);
//		jButtonROC.setVisible(aTargetType == TargetType.SINGLE_NOMINAL);

		jButtonBrowseSubgroups = GUI.buildButton("Browse Selected", 'B', "browse", this);
		aSubgroupSetPanel.add(jButtonBrowseSubgroups);

		jButtonDeleteSubgroups = GUI.buildButton("Delete Selected", 'D', "delete", this);
		aSubgroupPanel.add(jButtonDeleteSubgroups);

		jButtonPValues = GUI.buildButton("Compute p-Values", 'V', "compute_p", this);
		aSubgroupPanel.add(jButtonPValues);

		jButtonRegressionTest = GUI.buildButton("Regression Test", 'T', "regression", this);
		aSubgroupPanel.add(jButtonRegressionTest);

		jButtonEmpirical = GUI.buildButton("Empirical p-Values", 'E', "empirical_p", this);
		aSubgroupPanel.add(jButtonEmpirical);

		jButtonFold = GUI.buildButton("Fold members", 'F', "fold", this);
		aSubgroupPanel.add(jButtonFold);
//		jButtonFold.setVisible(itsFold != 0);

		jButtonSave = GUI.buildButton("Save", 'S', "save", this);
		aSubgroupPanel.add(jButtonSave);

		jButtonPrint = GUI.buildButton("Print", 'P', "print", this);
		aSubgroupPanel.add(jButtonPrint);

		jButtonCloseWindow = GUI.buildButton("Close", 'C', "close", this);
		aSubgroupSetPanel.add(jButtonCloseWindow);
/*
		//disable result dependent buttons
		if (itsSubgroupSet.isEmpty())
		{
			jButtonShowModel.setEnabled(false);
			jButtonROC.setEnabled(false);
			jButtonDeleteSubgroups.setEnabled(false);
			jButtonPValues.setEnabled(false);
			jButtonRegressionTest.setEnabled(false);
			jButtonEmpirical.setEnabled(false);
			jButtonFold.setEnabled(false);
			jButtonSave.setEnabled(false);
			jButtonPrint.setEnabled(false);
		}
*/
		enableButtonsCheck();
		jPanelSouth.add(aSubgroupPanel);
		jPanelSouth.add(aSubgroupSetPanel);
		getContentPane().add(jPanelSouth, BorderLayout.SOUTH);
		getContentPane().add(itsScrollPane, BorderLayout.CENTER);
	}

	@Override
	public void actionPerformed(ActionEvent theEvent)
	{
		String aCommand = theEvent.getActionCommand();
		if ("model".equals(aCommand))
			jButtonShowModelActionPerformed();
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
			// TargetType.SINGLE_NUMERIC shortcut for cmsb versions
			jButtonPValues.setVisible(aTargetType != TargetType.SINGLE_NUMERIC);
			jButtonRegressionTest.setVisible(aTargetType != TargetType.SINGLE_NUMERIC);
			jButtonEmpirical.setVisible(aTargetType != TargetType.SINGLE_NUMERIC);

			jButtonFold.setVisible(itsFold != 0);
		}
	}

	private void jButtonShowModelActionPerformed()
	{
		int[] aSelectionIndex = itsSubgroupTable.getSelectedRows();
		if (aSelectionIndex.length == 0)
			return;

		switch (itsSearchParameters.getTargetType())
		{
			case DOUBLE_CORRELATION :
			{
				int aCount = 0;
				for (Subgroup aSubgroup : itsSubgroupSet)
				{
					if (aCount == aSelectionIndex[0]) //just the first selection gets a window
					{
						/*
						TargetConcept aTargetConcept = itsSearchParameters.getTargetConcept();
						Attribute aPrimaryTarget = aTargetConcept.getPrimaryTarget();
						Column aPrimaryColumn = itsTable.getColumn(aPrimaryTarget);
						Attribute aSecondaryTarget = aTargetConcept.getSecondaryTarget();
						Column aSecondaryColumn = itsTable.getColumn(aSecondaryTarget);

						//no trendline
						new ModelWindow(aPrimaryColumn, aSecondaryColumn, aPrimaryTarget.getName(), aSecondaryTarget.getName(), null, aSubgroup).setTitle("Subgroup " + (aCount+1));
						break;
						*/
						TargetConcept aTargetConcept = itsSearchParameters.getTargetConcept();

						//no trendline
						new ModelWindow(aTargetConcept.getPrimaryTarget(),
										aTargetConcept.getSecondaryTarget(),
										null,
										aSubgroup).setTitle("Subgroup " + (aCount + 1));
						break;
					}
					aCount++;
				}
				break;
			}
			case MULTI_LABEL :
			{
				int i = 0;
				while (i < aSelectionIndex.length)
				{
					Log.logCommandLine("subgroup " + (aSelectionIndex[i]+1));
					int aCount = 0;
					for (Subgroup s : itsSubgroupSet)
					{
						if (aCount == aSelectionIndex[i])
							new ModelWindow(s.getDAG(), 1000, 800)
								.setTitle("Bayesian net induced from subgroup " +
											(aSelectionIndex[i]+1));

						aCount++;
					}

					i++;
				}
				break;
			}
			case DOUBLE_REGRESSION :
				//TODO implement modelwindow with scatterplot and line
				break;
			default :
				break;
		}
	}

	private void jButtonROCActionPerformed()
	{
		new ROCCurveWindow(itsSubgroupSet, itsSearchParameters);
	}

	private void jButtonBrowseSubgroupsActionPerformed() {
		if (itsSubgroupSet.isEmpty())
			return;

		int[] aSelection = itsSubgroupTable.getSelectedRows();
		Iterator<Subgroup> anIterator = itsSubgroupSet.iterator();

		for (int i = 0, j = aSelection.length, k = 0; i < j; ++k)
		{
			int aNext = aSelection[k];
			while (i++ < aNext)
				anIterator.next();
			new BrowseWindow(itsTable, anIterator.next());
		}
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
		// Obtain input
		double[] aQualities = obtainRandomQualities();
		if (aQualities == null)
			return;
		NormalDistribution aDistro = new NormalDistribution(aQualities);

		for (Subgroup aSubgroup : itsSubgroupSet)
			aSubgroup.setPValue(aDistro);

		itsSubgroupTable.repaint();
	}

	private void jButtonRegressionTestActionPerformed()
	{
		// Obtain input
		double[] aQualities = obtainRandomQualities();
		if (aQualities == null)
			return;
		Validation aValidation = new Validation(itsSearchParameters, itsTable, itsQualityMeasure);
//		double aRegressionTestScore = aValidation.performRegressionTest(aQualities, aNrSubgroups, itsSubgroupSet);
//		JOptionPane.showMessageDialog(null, "The regression test score equals\n" + aRegressionTestScore);
		double[] aRegressionTestScore = aValidation.performRegressionTest(aQualities, itsSubgroupSet);
		JOptionPane.showMessageDialog(this, "The regression test score equals\nfor k =  1 : "+aRegressionTestScore[0]+"\nfor k = 10 : "+aRegressionTestScore[1]);
	}

	private void jButtonEmpiricalActionPerformed()
	{
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
	}

	private void jButtonFoldActionPerformed()
	{
		Log.logCommandLine("Members of the training set of fold " + itsFold);
		Log.logCommandLine(itsBitSet.toString());
	}

	private double[] obtainRandomQualities()
	{
		String[] aSettings = new RandomQualitiesWindow(null).getSettings();
		String aMethod = aSettings[0];
		String aNrRepetitionsString = aSettings[1];
		int aNrRepetitions = 0;

		if (aMethod == null || aNrRepetitionsString == null ||
			((aNrRepetitions = Integer.parseInt(aNrRepetitionsString)) <= 1))
			return null;
		else
		{
			// Compute qualities
			Validation aValidation = new Validation(itsSearchParameters, itsTable, itsQualityMeasure);
			if (RandomQualitiesWindow.RANDOM_SUBSETS.equals(aMethod))
				return aValidation.randomSubgroups(aNrRepetitions);
			else if (RandomQualitiesWindow.RANDOM_DESCRIPTIONS.equals(aMethod))
				return aValidation.randomConditions(aNrRepetitions);
			else
				return null;
		}
	}

	private void jButtonSaveActionPerformed()
	{
		File aFile = new FileHandler(Action.SAVE).getFile();
		if (aFile == null)
			return; // cancelled

		XMLAutoRun.save(itsSubgroupSet, aFile.getAbsolutePath());
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

/*
	private double[] obtainRandomQualities()
	{
		double[] aPi = {Math.PI};
		int aMethod = JOptionPane.showOptionDialog(this,
				"By which method should the random qualities be computed?",
				"Which method?",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
				null,
				new String[] {"Random subgroups", "Random conditions"},
				"Random subgroups");
		if (!(aMethod == 0 || aMethod == 1))
		{
			JOptionPane.showMessageDialog(this, "No method selected;\nrandom qualities cannot be computed.");
			return aPi;
		}

		String inputValue = JOptionPane.showInputDialog("Number of random subgroups to be used\nfor random quality estimation:", 1000);
		int aNrRepetitions;
		try
		{
			aNrRepetitions = Integer.parseInt(inputValue);
			if (aNrRepetitions <= 1)
			{
				JOptionPane.showMessageDialog(this, "Number should be > 1;\nrandom qualities cannot be computed.");
				return aPi;
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Not a valid number;\nrandom qualities cannot be computed.");
			return aPi;
		}

		// Compute qualities
		Validation aValidation = new Validation(itsSearchParameters, itsTable, itsQualityMeasure);
		double[] aQualities;
		switch (aMethod)
		{
			case 0:
			{
				aQualities = aValidation.randomSubgroups(aNrRepetitions);
				break;
			}
			case 1:
			{
				aQualities = aValidation.randomConditions(aNrRepetitions);
				break;
			}
			default:
			{	// Should never reach this code
				aQualities = null;
			}
		}
		return aQualities;
	}
*/
	private JPanel jPanelSouth;
	private JButton jButtonShowModel;
	private JButton jButtonBrowseSubgroups;
	private JButton jButtonDeleteSubgroups;
	private JButton jButtonFold;
	private JButton jButtonPValues;
	private JButton jButtonRegressionTest;
	private JButton jButtonEmpirical;
	private JButton jButtonROC;
	private JButton jButtonSave;
	private JButton jButtonPrint;
	private JButton jButtonCloseWindow;
	private JScrollPane itsScrollPane;
}
