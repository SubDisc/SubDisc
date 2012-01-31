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
		if (theTable == null || theSubgroupDiscovery == null)
		{
			Log.logCommandLine("ResultWindow Constructor: parameter(s) can not be 'null'.");
			return;
		}

		itsTable = theTable;
		itsNrRecords = itsTable.getNrRows();
		itsSubgroupSet = theSubgroupDiscovery.getResult();
		itsSearchParameters = theSubgroupDiscovery.getSearchParameters();
		itsQualityMeasure = theSubgroupDiscovery.getQualityMeasure();
//			itsDAGView = theDAGView;
		itsBinaryTable = theBinaryTable;

		// only used in MULTI_LABEL setting for now
		// if theFold == 0, itsBitSet is never used
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

		jButtonROC = GUI.buildButton("ROC", 'R', "roc", this);
		aSubgroupPanel.add(jButtonROC);

		jButtonBrowseSubgroups = GUI.buildButton("Browse Selected", 'B', "browse", this);
		aSubgroupPanel.add(jButtonBrowseSubgroups);

		jButtonDeleteSubgroups = GUI.buildButton("Delete Selected", 'D', "delete", this);
		aSubgroupPanel.add(jButtonDeleteSubgroups);

		jButtonPValues = GUI.buildButton("Gaussian p-Values", 'V', "compute_p", this);
		aSubgroupPanel.add(jButtonPValues);

		jButtonRegressionTest = GUI.buildButton("Regression Test", 'T', "regression", this);
		aSubgroupPanel.add(jButtonRegressionTest);

		jButtonEmpirical = GUI.buildButton("Empirical p-Values", 'E', "empirical_p", this);
		aSubgroupPanel.add(jButtonEmpirical);

		jButtonFold = GUI.buildButton("Fold members", 'F', "fold", this);
		aSubgroupPanel.add(jButtonFold);

		jButtonSave = GUI.buildButton("Save", 'S', "save", this);
		aSubgroupPanel.add(jButtonSave);

		jButtonPrint = GUI.buildButton("Print", 'P', "print", this);
		aSubgroupPanel.add(jButtonPrint);

		jButtonCloseWindow = GUI.buildButton("Close", 'C', "close", this);
		aSubgroupSetPanel.add(jButtonCloseWindow);

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

	/*
	 * TODO use Subgroup.getID/getConditions() for title
	 * Current naming relies on non-sortable columns
	 */
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
				/*
				 * TODO inefficient loop
				 * use convertViewToModel(i) to select correct s, then use s.ID
				 */
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
			{
				int i=0;
				Log.logCommandLine("======================================================");
				Log.logCommandLine("Global model:");
				Log.logCommandLine(itsSearchParameters.getTargetConcept().getGlobalRegressionModel());
				Log.logCommandLine("------------------------------------------------------");
				while (i<aSelectionIndex.length)
				{
					Log.logCommandLine("Model for subgroup " + (aSelectionIndex[i]+1) + ":");
					int aCount = 0;
					for (Subgroup s : itsSubgroupSet)
					{
						if (aCount == aSelectionIndex[i])
							Log.logCommandLine(s.getRegressionModel());
						aCount++;
					}

					i++;
				}
				Log.logCommandLine("======================================================");
				break;
			}
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
		double[] aRegressionTestScore = aValidation.performRegressionTest(aQualities, itsSubgroupSet);
		JOptionPane.showMessageDialog(null, "The regression test score equals\nfor k =  1 : "+aRegressionTestScore[0]+"\nfor k = 10 : "+aRegressionTestScore[1]);
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
			else if (RandomQualitiesWindow.SWAP_RANDOMIZATION.equals(aMethod))
				return aValidation.swapRandomization(aNrRepetitions);
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
