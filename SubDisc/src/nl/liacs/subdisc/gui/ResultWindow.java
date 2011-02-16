package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import nl.liacs.subdisc.*;

public class ResultWindow extends JFrame
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
//			setIconImage(ICON);
			initialise();
			setTitle();
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
//			setIconImage(ICON);
		initialise();
		setTitle();
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

			public void keyReleased(KeyEvent key) {	}

			public void keyTyped(KeyEvent key) {	}
		});

		pack ();
	}

	private void initComponents()
	{
		jPanelSouth = new JPanel(new GridLayout(2, 1));
		JPanel aSubgroupPanel = new JPanel();
		JPanel aSubgroupSetPanel = new JPanel();

		itsScrollPane = new JScrollPane();
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				exitForm();
			}
		});

		jButtonShowModel = initButton("Show Model", 'S');
		jButtonShowModel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonShowModelActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonShowModel);
		jButtonShowModel.setVisible(itsSearchParameters.getTargetType() != TargetType.SINGLE_NOMINAL);

		jButtonROC = initButton("ROC", 'R');
		jButtonROC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonROCActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonROC);
		jButtonROC.setVisible(itsSearchParameters.getTargetType() == TargetType.SINGLE_NOMINAL);

		jButtonDeleteSubgroups = initButton("Delete Pattern", 'D');
		jButtonDeleteSubgroups.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonDeleteSubgroupsActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonDeleteSubgroups);

		jButtonDumpPatterns = initButton("Dump Patterns", 'U');
		jButtonDumpPatterns.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonDumpPatternsActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonDumpPatterns);

		jButtonPostprocess = initButton("Post-process", 'O');
		jButtonPostprocess.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonPostprocessActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonPostprocess);
		jButtonPostprocess.setVisible(itsSearchParameters.getTargetType() == TargetType.MULTI_LABEL);

		jButtonPValues = initButton("Compute p-Values", 'P');
		jButtonPValues.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonPValuesActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonPValues);

		jButtonRegressionTest = initButton("Regression Test", 'T');
		jButtonRegressionTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonRegressionTestActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonRegressionTest);

		jButtonEmpirical = initButton("Empirical p-Values", 'E');
		jButtonEmpirical.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonEmpiricalActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonEmpirical);

		jButtonFold = initButton("Fold members", 'F');
		jButtonFold.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonFoldActionPerformed();
			}
		});
		aSubgroupPanel.add(jButtonFold);
		jButtonFold.setVisible(itsFold != 0);

		//possibly disable buttons
		if (itsSubgroupSet.isEmpty())
		{
			jButtonShowModel.setEnabled(false);
			jButtonROC.setEnabled(false);
			jButtonDeleteSubgroups.setEnabled(false);
			jButtonPostprocess.setEnabled(false);
		}

		//close button
		jButtonCloseWindow = initButton("Close", 'C');
		jButtonCloseWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				exitForm();
			}
		});

		aSubgroupSetPanel.add(jButtonCloseWindow);

		jPanelSouth.add(aSubgroupPanel);
		jPanelSouth.add(aSubgroupSetPanel);
		getContentPane().add(jPanelSouth, BorderLayout.SOUTH);
		getContentPane().add(itsScrollPane, BorderLayout.CENTER);
	}

	private JButton initButton(String theName, int theMnemonic)
	{
		JButton aButton = new JButton();
		aButton.setPreferredSize(new Dimension(110, 25));
		aButton.setBorder(new BevelBorder(0));
		aButton.setMinimumSize(new Dimension(82, 25));
		aButton.setMaximumSize(new Dimension(110, 25));
		aButton.setFont(new Font ("Dialog", 1, 11));
		aButton.setText(theName);
		aButton.setMnemonic(theMnemonic);
		return aButton;
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
						TargetConcept aTargetConcept = itsSearchParameters.getTargetConcept();
						Attribute aPrimaryTarget = aTargetConcept.getPrimaryTarget();
						Column aPrimaryColumn = itsTable.getColumn(aPrimaryTarget);
						Attribute aSecondaryTarget = aTargetConcept.getSecondaryTarget();
						Column aSecondaryColumn = itsTable.getColumn(aSecondaryTarget);

						ModelWindow aWindow = new ModelWindow(aPrimaryColumn, aSecondaryColumn,
							aPrimaryTarget.getName(), aSecondaryTarget.getName(), null, aSubgroup); //no trendline
						aWindow.setLocation(50, 50);
						aWindow.setSize(GUI.WINDOW_DEFAULT_SIZE);
						aWindow.setVisible(true);
						aWindow.setTitle("Subgroup " + (aCount+1));
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
						{
							ModelWindow aWindow = new ModelWindow(s.getDAG(), 1000, 800);
							aWindow.setLocation(0, 0);
							aWindow.setSize(1000, 800);
							aWindow.setVisible(true);
							aWindow.setTitle("Bayesian net induced from subgroup " + (aSelectionIndex[i]+1));
						}
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

		// Display postprocessed results
		ResultWindow aResultWindow = new ResultWindow(itsTable, aNewSubgroupSet, itsSearchParameters, itsQualityMeasure, itsBinaryTable, itsFold, itsBitSet);
		aResultWindow.setLocation(0, 0);
		aResultWindow.setSize(1200, 900);
		aResultWindow.setVisible(true);
		exitForm();
	}

	private void jButtonPValuesActionPerformed()
	{
		// Obtain input
		double[] aQualities = obtainRandomQualities();
		if (aQualities == null || aQualities[0] == Math.PI)
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
		if (aQualities == null || aQualities[0] == Math.PI)
			return;
		Validation aValidation = new Validation(itsSearchParameters, itsTable, itsQualityMeasure);
//		double aRegressionTestScore = aValidation.performRegressionTest(aQualities, aNrSubgroups, itsSubgroupSet);
//		JOptionPane.showMessageDialog(null, "The regression test score equals\n" + aRegressionTestScore);
		double[] aRegressionTestScore = aValidation.performRegressionTest(aQualities, itsSubgroupSet);
		JOptionPane.showMessageDialog(null, "The regression test score equals\nfor k =  1 : "+aRegressionTestScore[0]+"\nfor k = 10 : "+aRegressionTestScore[1]);
	}

	private void jButtonEmpiricalActionPerformed()
	{
		// Obtain input
		double[] aQualities = obtainRandomQualities();
		if (aQualities == null || aQualities[0] == Math.PI)
			return;

		Validation aValidation = new Validation(itsSearchParameters, itsTable, itsQualityMeasure);
		double aPValue = aValidation.computeEmpiricalPValue(aQualities, itsSubgroupSet);
		JOptionPane.showMessageDialog(null, "The empirical p-value is p = " + aPValue);
	}

	private void jButtonFoldActionPerformed()
	{
		Log.logCommandLine("Members of the training set of fold " + itsFold);
		Log.logCommandLine(itsBitSet.toString());
	}

	private double[] obtainRandomQualities()
	{
		double[] aPi = {Math.PI};
		int aMethod = JOptionPane.showOptionDialog(null,
				"By which method should the random qualities be computed?",
				"Which method?",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new String[] {"Random subgroups", "Random conditions"},
				"Random subgroups");
		if (!(aMethod == 0 || aMethod == 1))
		{
			JOptionPane.showMessageDialog(null, "No method selected;\nrandom qualities cannot be computed.");
			return aPi;
		}

		String inputValue = JOptionPane.showInputDialog("Number of random subgroups to be used\nfor random quality estimation:", 1000);
		int aNrRepetitions;
		try
		{
			aNrRepetitions = Integer.parseInt(inputValue);
			// TODO more than one?
			if (aNrRepetitions <= 1)
			{
				JOptionPane.showMessageDialog(null, "Number should be > 1;\nrandom qualities cannot be computed.");
				return aPi;
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Not a valid number;\nrandom qualities cannot be computed.");
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

	private void exitForm() { dispose(); }

	private javax.swing.JPanel jPanelSouth;
	private javax.swing.JButton jButtonShowModel;
	private javax.swing.JButton jButtonDeleteSubgroups;
	private javax.swing.JButton jButtonDumpPatterns;
	private javax.swing.JButton jButtonFold;
	private javax.swing.JButton jButtonPostprocess;
	private javax.swing.JButton jButtonPValues;
	private javax.swing.JButton jButtonRegressionTest;
	private javax.swing.JButton jButtonEmpirical;
	private javax.swing.JButton jButtonROC;
	private javax.swing.JButton jButtonCloseWindow;
	private javax.swing.JScrollPane itsScrollPane;
}
