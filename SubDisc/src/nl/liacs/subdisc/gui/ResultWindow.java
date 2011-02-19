package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import nl.liacs.subdisc.*;

public class ResultWindow extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;
//	public static final Image ICON = Toolkit.getDefaultToolkit().getImage(ResultWindow.class.getResource("/Safarii.gif"));

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
			return;	// TODO
		else
		{
			itsTable = theTable;
			itsNrRecords = itsTable.getNrRows();
			itsSubgroupSet = theSubgroupDiscovery.getResult();
			itsSearchParameters = theSubgroupDiscovery.getSearchParameters();
			itsQualityMeasure = theSubgroupDiscovery.getQualityMeasure();
//			itsDAGView = theDAGView;
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
			setLocation(100, 100);
			setSize(GUI.WINDOW_DEFAULT_SIZE);
			pack ();
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
		setLocation(100, 100);
		setSize(GUI.WINDOW_DEFAULT_SIZE);
		pack ();
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
		itsSubgroupTable.getColumnModel().getColumn(0).setPreferredWidth(10);
		itsSubgroupTable.getColumnModel().getColumn(1).setPreferredWidth(10);
		itsSubgroupTable.getColumnModel().getColumn(2).setPreferredWidth(20);
		itsSubgroupTable.getColumnModel().getColumn(3).setPreferredWidth(50);
		itsSubgroupTable.getColumnModel().getColumn(4).setPreferredWidth(90);
		itsSubgroupTable.getColumnModel().getColumn(5).setPreferredWidth(600);

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

		jButtonShowModel = GUI.buildButton("Show Model", 'S', "model", this);
		aSubgroupPanel.add(jButtonShowModel);
		jButtonShowModel.setVisible(itsSearchParameters.getTargetType() != TargetType.SINGLE_NOMINAL);

		jButtonROC = GUI.buildButton("ROC", 'R', "roc", this);
		aSubgroupPanel.add(jButtonROC);
		jButtonROC.setVisible(itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL);

		jButtonDeleteSubgroups = GUI.buildButton("Delete Pattern", 'D', "delete", this);
		aSubgroupPanel.add(jButtonDeleteSubgroups);

		jButtonDumpPatterns = GUI.buildButton("Dump Patterns", 'U', "dump", this);
		aSubgroupPanel.add(jButtonDumpPatterns);

		jButtonPostprocess = GUI.buildButton("Post-process", 'O', "post_process", this);
		aSubgroupPanel.add(jButtonPostprocess);
		jButtonPostprocess.setVisible(itsSearchParameters.getTargetType() == TargetType.MULTI_LABEL);

		jButtonPValues = GUI.buildButton("Compute p-Values", 'P', "compute_p", this);
		aSubgroupPanel.add(jButtonPValues);

		jButtonRegressionTest = GUI.buildButton("Regression Test", 'T', "regression", this);
		aSubgroupPanel.add(jButtonRegressionTest);

		jButtonEmpirical = GUI.buildButton("Empirical p-Values", 'E', "empirical_p", this);
		aSubgroupPanel.add(jButtonEmpirical);

		jButtonFold = GUI.buildButton("Fold members", 'F', "fold", this);
		aSubgroupPanel.add(jButtonFold);
		jButtonFold.setVisible(itsFold != 0);

		jButtonCloseWindow = GUI.buildButton("Close", 'C', "close", this);
		aSubgroupSetPanel.add(jButtonCloseWindow);

		//disable result dependent buttons
		if (itsSubgroupSet.isEmpty())
		{
			jButtonShowModel.setEnabled(false);
			jButtonROC.setEnabled(false);
			jButtonDeleteSubgroups.setEnabled(false);
			jButtonDumpPatterns.setEnabled(false);
			jButtonPostprocess.setEnabled(false);
//			jButtonPValues.setEnabled(false);
			jButtonRegressionTest.setEnabled(false);
			jButtonEmpirical.setEnabled(false);
			jButtonFold.setEnabled(false);
		}

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
		else if ("delete".equals(aCommand))
			jButtonDeleteSubgroupsActionPerformed();
		else if ("dump".equals(aCommand))
			jButtonDumpPatternsActionPerformed();
		else if ("post_process".equals(aCommand))
			jButtonPostprocessActionPerformed();
		else if ("compute_p".equals(aCommand))
			jButtonPValuesActionPerformed();
		else if ("regression".equals(aCommand))
			jButtonRegressionTestActionPerformed();
		else if ("emperical_p".equals(aCommand))
			jButtonEmpiricalActionPerformed();
		else if ("fold".equals(aCommand))
			jButtonFoldActionPerformed();
		else if ("close".equals(aCommand))
			dispose();
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
						new ModelWindow(itsTable.getColumn(aTargetConcept.getPrimaryTarget()),
										itsTable.getColumn(aTargetConcept.getSecondaryTarget()),
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

	private void jButtonDumpPatternsActionPerformed()
	{
		StringBuffer aStringBuffer = new StringBuffer();
		aStringBuffer.append("\n\nnr,coverage,measure,conditionlist\n");
		Iterator<Subgroup> anIterator = itsSubgroupSet.iterator();
		while (anIterator.hasNext())
		{
			Subgroup aSubgroup = anIterator.next();

			aStringBuffer.append(aSubgroup.getID() + ","
									+ aSubgroup.getCoverage() + ","
									+ aSubgroup.getMeasureValue() + ","
									+ aSubgroup.getConditions().toString()
									+ "\n");
		}

		Log.toUniqueFile("patterns", aStringBuffer.toString());
	}

	private void jButtonPostprocessActionPerformed()
	{
		String inputValue = JOptionPane.showInputDialog("# DAGs fitted to each subgroup.");
		try
		{
			itsSearchParameters.setPostProcessingCount(Integer.parseInt(inputValue));
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Your input is unsound.");
			return;
		}

		if (itsSubgroupSet.isEmpty())
			return;

		// Create quality measures on whole dataset
		Log.logCommandLine("Creating quality measures.");
		int aPostProcessingCount = itsSearchParameters.getPostProcessingCount();
		double aPostProcessingCountSquare = Math.pow(aPostProcessingCount, 2);

		QualityMeasure[] aQMs = new QualityMeasure[aPostProcessingCount];
		for (int i = 0; i < aPostProcessingCount; i++)
		{
			Bayesian aGlobalBayesian = new Bayesian(itsBinaryTable);
			aGlobalBayesian.climb();
			aQMs[i] = new QualityMeasure(itsSearchParameters, aGlobalBayesian.getDAG(), itsNrRecords);
		}

		// Iterate over subgroups
		SubgroupSet aNewSubgroupSet = new SubgroupSet(itsSearchParameters.getMaximumSubgroups());
		int aCount = 0;
		for (Subgroup s : itsSubgroupSet)
		{
			Log.logCommandLine("Postprocessing subgroup " + ++aCount);
			double aTotalQuality = 0.0;
			BinaryTable aSubgroupTable = itsBinaryTable.selectRows(s.getMembers());
			for (int i = 0; i < aPostProcessingCount; i++)
			{
				Bayesian aLocalBayesian = new Bayesian(aSubgroupTable);
				aLocalBayesian.climb();
				s.setDAG(aLocalBayesian.getDAG());
				for (int j = 0; j < aPostProcessingCount; j++)
					aTotalQuality += aQMs[j].calculate(s);
			}
			s.setMeasureValue(aTotalQuality / aPostProcessingCountSquare);
			s.renouncePValue();
			aNewSubgroupSet.add(s);
		}
		aNewSubgroupSet.setIDs();

		//why is it bigger and re-located?
		// Display postprocessed results
//		ResultWindow aResultWindow = new ResultWindow(itsTable, aNewSubgroupSet, itsSearchParameters, itsQualityMeasure, itsBinaryTable, itsFold, itsBitSet);
//		aResultWindow.setLocation(0, 0);
//		aResultWindow.setSize(1200, 900);
//		aResultWindow.setVisible(true);
		new ResultWindow(itsTable, aNewSubgroupSet, itsSearchParameters, itsQualityMeasure, itsBinaryTable, itsFold, itsBitSet);
		dispose();
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
			if (RandomQualitiesWindow.RANDOM_SUBGROUPS.equals(aMethod))
				return aValidation.randomSubgroups(aNrRepetitions);
			else if (RandomQualitiesWindow.RANDOM_CONDITIONS.equals(aMethod))
				return aValidation.randomConditions(aNrRepetitions);
			else
				return null;
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
	private JButton jButtonDeleteSubgroups;
	private JButton jButtonDumpPatterns;
	private JButton jButtonFold;
	private JButton jButtonPostprocess;
	private JButton jButtonPValues;
	private JButton jButtonRegressionTest;
	private JButton jButtonEmpirical;
	private JButton jButtonROC;
	private JButton jButtonCloseWindow;
	private JScrollPane itsScrollPane;
}
