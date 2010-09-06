package nl.liacs.subdisc.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.FileHandler.Action;
import nl.liacs.subdisc.SearchParameters.NumericStrategy;
import nl.liacs.subdisc.TargetConcept.TargetType;
import nl.liacs.subdisc.XMLAutoRun.AutoRun;

public class MiningWindow extends JFrame
{
	static final long serialVersionUID = 1L;

	// TODO get image
	public static final Image ICON = Toolkit.getDefaultToolkit().getImage(MiningWindow.class.getResource("/icon.gif"));

	private Table itsTable;
	private int itsTotalCount;

	// target info
	private int itsPositiveCount; // nominal target
	private double itsTargetAverage; // numeric target

	// TODO there should be at most 1 MiningWindow();
	private SearchParameters itsSearchParameters = new SearchParameters();
	private TargetConcept itsTargetConcept = new TargetConcept();

	public MiningWindow()
	{
		initMiningWindow();
	}

	public MiningWindow(Table theTable)
	{
		itsTable = theTable;
		itsTotalCount = itsTable.getNrRows();
		initMiningWindow();
		initGuiComponents();
	}

	public MiningWindow(Table theTable, SearchParameters theSearchParameters)
	{
		itsTable = theTable;
		itsTotalCount = itsTable.getNrRows();
		initMiningWindow();

		if(theSearchParameters != null)
			itsTargetConcept = theSearchParameters.getTargetConcept();
		itsSearchParameters = theSearchParameters;
		initGuiComponentsFromFile();
	}

	private void initMiningWindow()
	{
		// Initialise graphical components
		initComponents();
		initNeverChangingGuiComponents();
		enableTableDependentComponents(itsTable != null);
		setTitle("Subgroup Discovery");
		// setIconImage(ICON);
		pack();
		setSize(700, 600);
		setVisible(true);

		// Open log/debug files
		Log.openFileOutputStreams();
	}

	private void initNeverChangingGuiComponents()
	{
		// TODO disable if no table?
		// should do for-each combobox/field
//		boolean hasTable = (itsTable != null);
		// Add all implemented TargetTypes
		for (TargetType t : TargetType.values())
			if (t.isImplemented())
				jComboBoxTargetType.addItem(t.TEXT);
//		jComboBoxTargetType.setEnabled(hasTable);

		// Add all SearchStrategies
		for (int i = 0; i <= CandidateQueue.LAST_SEARCH_STRATEGY; i++)
			jComboBoxSearchStrategyType.addItem(SearchParameters.getSearchStrategyName(i));
//		jComboBoxSearchStrategyType.setEnabled(hasTable);

		// Add all Numeric Strategies
		for (NumericStrategy n : SearchParameters.NumericStrategy.values())
			jComboBoxSearchStrategyNumeric.addItem(n.TEXT);
//		jComboBoxSearchStrategyNumeric.setEnabled(hasTable);
	}

	private void initGuiComponents()
	{
		//dataset
		initGuiComponentsDataSet();

		// target concept
		itsTargetConcept.setTargetType(getTargetTypeName());	// could use SINGLE_NOMINAL as default in TargetConcept
		switch(itsTargetConcept.getTargetType())
		{
			case DOUBLE_CORRELATION :
			case DOUBLE_REGRESSION	:
			case MULTI_LABEL		: break;
			default : jButtonBaseModel.setEnabled(false); break;
		}

		// search conditions
		setSearchDepthMaximum("1");
		setSearchCoverageMaximum("1.0");
		setSubgroupsMaximum("50");
		setSearchTimeMaximum("1.0");

		// search strategy
		setSearchStrategyWidth("100");
		setSearchStrategyNrBins("8");
	}

	private void initGuiComponentsFromFile()
	{
		initGuiComponentsDataSet();

		// TODO disable all ActionListeners while setting values
		// TODO some field may be set automatically, order is very important

		// search strategy
		setSearchStrategyType(SearchParameters.getSearchStrategyName(itsSearchParameters.getSearchStrategy()));
		setSearchStrategyWidth(String.valueOf(itsSearchParameters.getSearchStrategyWidth()));
		setNumericStrategy(itsSearchParameters.getNumericStrategy().TEXT);
		setSearchStrategyNrBins(String.valueOf(itsSearchParameters.getNrBins()));	// TODO use nrBins/splitPoints

		// search conditions
		// NOTE setSearchStrategyType() calls setSearchCoverageMinimum(), so
		// setSearchCoverageMinimum must be called AFTER setSearchStrategyType
		setSearchDepthMaximum(String.valueOf(itsSearchParameters.getSearchDepth()));
		setSearchCoverageMinimum(String.valueOf(itsSearchParameters.getMinimumCoverage()));
		setSearchCoverageMaximum(String.valueOf(itsSearchParameters.getMaximumCoverage()));
		setSubgroupsMaximum(String.valueOf(itsSearchParameters.getMaximumSubgroups()));
		setSearchTimeMaximum(String.valueOf(itsSearchParameters.getMaximumTime()));

		// target concept
		setTargetTypeName(itsTargetConcept.getTargetType().TEXT);
		setQualityMeasureMinimum(String.valueOf(itsSearchParameters.getQualityMeasureMinimum()));	// TODO must be called before setQualityMeasure (prevents overwriting)
		setQualityMeasure(itsSearchParameters.getQualityMeasureString());
		setQualityMeasureMinimum(String.valueOf(itsSearchParameters.getQualityMeasureMinimum()));	// TODO see above, but needs to be set again
		setTargetAttribute(itsTargetConcept.getPrimaryTarget().getName());
//		setMiscField(itsTargetConcept.getSecondaryTarget());
//		setMiscField(itsTargetConcept.getTargetValue());
		// setSecondaryTargets(); // TODO initialised from primaryTargetList
	}

	private void initGuiComponentsDataSet()
	{
		if (itsTable != null)
		{
			jLFieldTargetTable.setText(itsTable.getTableName());
			// TODO the following fields need to be updated after data/attribute change
			jLFieldNrExamples.setText(String.valueOf(itsTotalCount));
			jLFieldNrColumns.setText(String.valueOf(itsTable.getNrColumns()));
			int[] aCounts = itsTable.getTypeCounts();
			jLFieldNrNominals.setText(String.valueOf(aCounts[0]));
			jLFieldNrNumerics.setText(String.valueOf(aCounts[1]));
			jLFieldNrBinaries.setText(String.valueOf(aCounts[2]));
		}
	}

	/**
	 * This method is called from within the constructor to Initialise the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the FormEditor.
	 */
	private void initComponents()
	{
		// menu items
		jMiningWindowMenuBar = new JMenuBar();
		jMenuFile = new JMenu();
		jMenuItemOpenFile = new JMenuItem();
		jMenuItemDataExplorer = new JMenuItem();
		jMenuItemBrowseTarget = new JMenuItem();
		jMenuItemAttributeTypeChange = new JMenuItem();
		jSeparator2 = new JSeparator();
		jMenuItemSubgroupDiscovery = new JMenuItem();
		jSeparator3 = new JSeparator();
		jMenuItemCreateAutoRunFile = new JMenuItem();
		jMenuItemAddToAutoRunFile = new JMenuItem();
//		jMenuItemLoadAutoRunFile = new JMenuItem();
		jSeparator4 = new JSeparator();
		jMenuItemExit = new JMenuItem();
		jMenuAbout = new JMenu();
		jMenuItemAboutSubDisc = new JMenuItem();

		jPanelCenter = new JPanel();	// 4 panels
		jPanelSouth = new JPanel();		// mining buttons

		// dataset
		jPanelRuleTarget = new JPanel();
		// dataset - labels
		jPanelRuleTargetLabels = new JPanel();
		jLabelTargetTable = new JLabel();
		jLabelNrExamples = new JLabel();
		jLabelNrColumns = new JLabel();
		jLabelNrNominals = new JLabel();
		jLabelNrNumerics = new JLabel();
		jLabelNrBinaries = new JLabel();
		// dataset - fields
		jPanelRuleTargetFields = new JPanel();
		jLFieldTargetTable = new JLabel();
		jLFieldNrExamples = new JLabel();
		jLFieldNrColumns = new JLabel();
		jLFieldNrNominals = new JLabel();
		jLFieldNrNumerics = new JLabel();
		jLFieldNrBinaries = new JLabel();
		// dataset - buttons
		jPanelRuleTargetButtons = new JPanel();
		jButtonBrowse = initButton("Browse", 'B');
		jButtonAttributeTypeChange = initButton("Attribute Type Change", 'A');	// TODO change to 'AttributeChange'

		// target concept
		jPanelRuleEvaluation = new JPanel();
		// target concept - labels
		jPanelEvaluationLabels = new JPanel();
		jLabelTargetType = new JLabel();
		jLabelQualityMeasure = new JLabel();
		jLabelEvaluationTreshold = new JLabel();
		jLabelTargetAttribute = new JLabel();
		jLabelMiscField = new JLabel(); // used for target value or secondary target
		jLabelSecondaryTargets = new JLabel();
		jLabelTargetInfo = new JLabel();
		// target concept - fields
		jPanelEvaluationFields = new JPanel();
		jComboBoxTargetType = new JComboBox();
		jComboBoxQualityMeasure = new JComboBox();
		jTextFieldQualityMeasureMinimum = new JTextField();
		jComboBoxTargetAttribute = new JComboBox();
		jComboBoxMiscField = new JComboBox(); // used for target value or secondary target
		jListSecondaryTargets = new JList(new DefaultListModel());
		SecondaryTargets = new JScrollPane(jListSecondaryTargets);
		jLFieldTargetInfo = new JLabel();
		jButtonBaseModel = new JButton();

		//search conditions
		jPanelSearchParameters = new JPanel();
		//search conditions - label
		jPanelSearchParameterLabels = new JPanel();
		jLabelSearchDepth = new JLabel();
		jLabelSearchCoverageMinimum = new JLabel();
		jLabelSearchCoverageMaximum = new JLabel();
		jLabelSubgroupsMaximum = new JLabel();
		jLabelSearchTimeMaximum = new JLabel();
		//search conditions - fields
		jPanelSearchParameterFields = new JPanel();
		jTextFieldSearchDepth = new JTextField();
		jTextFieldSearchCoverageMinimum = new JTextField();
		jTextFieldSearchCoverageMaximum = new JTextField();
		jTextFieldSubgroupsMaximum = new JTextField();
		jTextFieldSearchTimeMaximum = new JTextField();

		// search strategy
		jPanelSearchStrategy = new JPanel();
		// search strategy - labels
		jPanelSearchStrategyLabels = new JPanel();
		jLabelStrategyType = new JLabel();
		jLabelStrategyWidth = new JLabel();
		jLabelSearchStrategyNumericFrr = new JLabel();
		jLabelSearchStrategyNrBins = new JLabel();
		// search strategy - fields
		jPanelSearchStrategyFields = new JPanel();
		jComboBoxSearchStrategyType = new JComboBox();
		jTextFieldSearchStrategyWidth = new JTextField();
		jComboBoxSearchStrategyNumeric = new JComboBox();
		jTextFieldSearchStrategyNrBins = new JTextField();

		// mining buttons
//		jLabelLayoutFiller0 = new JLabel();
//		jPanelLayoutFiller1 = new JPanel();
		jPanelMineButtons = new JPanel();
		jButtonSubgroupDiscovery = new JButton();
		jButtonRandomSubgroups = new JButton();
		jButtonRandomConditions = new JButton();
//		jPanelLayoutFiller2 = new JPanel();
//		jLabelLayoutFiller3 = new JLabel();

		// setting up - menu items
		jMiningWindowMenuBar.setFont(DEFAULT_FONT);

		jMenuFile.setFont(DEFAULT_FONT);
		jMenuFile.setText("File");
		jMenuFile.setMnemonic('F');

		jMenuItemOpenFile.setFont(DEFAULT_FONT);
		jMenuItemOpenFile.setText("Open File");
		jMenuItemOpenFile.setMnemonic('O');
		jMenuItemOpenFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		jMenuItemOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemOpenFileActionPerformed(evt);
			}
		});
		jMenuFile.add(jMenuItemOpenFile);

		jMenuItemDataExplorer.setFont(DEFAULT_FONT);
		jMenuItemDataExplorer.setText("Data Explorer");
		jMenuItemDataExplorer.setMnemonic('E');
		jMenuItemDataExplorer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		jMenuItemDataExplorer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				DataExplorerActionPerformed(evt);
			}
		});
		jMenuFile.add(jMenuItemDataExplorer);

		jMenuItemBrowseTarget.setFont(DEFAULT_FONT);
		jMenuItemBrowseTarget.setText("Browse");
		jMenuItemBrowseTarget.setMnemonic('B');
		jMenuItemBrowseTarget.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK));
		jMenuItemBrowseTarget.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BrowseActionPerformed(evt);
			}
		});
		jMenuFile.add(jMenuItemBrowseTarget);

		jMenuItemAttributeTypeChange.setFont(DEFAULT_FONT);
		jMenuItemAttributeTypeChange.setText("Change Attribute Type");
		jMenuItemAttributeTypeChange.setMnemonic('A');
		jMenuItemAttributeTypeChange.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
		jMenuItemAttributeTypeChange.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				AttributeTypeChangeActionPerformed(evt);
			}
		});
		jMenuFile.add(jMenuItemAttributeTypeChange);

		jMenuFile.add(jSeparator2);

		jMenuItemSubgroupDiscovery.setFont(DEFAULT_FONT);
		jMenuItemSubgroupDiscovery.setText("Subgroup Discovery");
		jMenuItemSubgroupDiscovery.setMnemonic('S');
		jMenuItemSubgroupDiscovery.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		jMenuItemSubgroupDiscovery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonSubgroupDiscoveryActionPerformed(evt);
			}
		});
		jMenuFile.add(jMenuItemSubgroupDiscovery);

		jMenuFile.add(jSeparator3);

		jMenuItemCreateAutoRunFile.setFont(DEFAULT_FONT);
		jMenuItemCreateAutoRunFile.setText("Create Autorun File");
//		jMenuItemCreateAutoRunFile.setMnemonic();
//		jMenuItemCreateAutoRunFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
		jMenuItemCreateAutoRunFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAutoRunFileActionPerformed(AutoRun.CREATE);
			}
		});
		jMenuFile.add(jMenuItemCreateAutoRunFile);

		jMenuItemAddToAutoRunFile.setFont(DEFAULT_FONT);
		jMenuItemAddToAutoRunFile.setText("Add to Autorun File");
//		jMenuItemAddToAutoRunFile.setMnemonic();
//		jMenuItemAddToAutoRunFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
		jMenuItemAddToAutoRunFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAutoRunFileActionPerformed(AutoRun.ADD);
			}
		});
		jMenuFile.add(jMenuItemAddToAutoRunFile);
/*
		jMenuItemLoadAutoRunFile.setFont(DEFAULT_FONT);
		jMenuItemLoadAutoRunFile.setText("Load Autorun File");
//		jMenuItemLoadAutoRunFile.setMnemonic();
//		jMenuItemLoadAutoRunFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
		jMenuItemLoadAutoRunFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAutoRunFileActionPerformed(AutoRun.LOAD);
			}
		});
		jMenuFile.add(jMenuItemLoadAutoRunFile);
*/
		jMenuFile.add(jSeparator4);

		jMenuItemExit.setFont(DEFAULT_FONT);
		jMenuItemExit.setText("Exit");
		jMenuItemExit.setMnemonic('X');
		jMenuItemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
		jMenuItemExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemExitActionPerformed(evt);
			}
		});
		jMenuFile.add(jMenuItemExit);
		jMiningWindowMenuBar.add(jMenuFile);

		jMenuAbout.setFont(DEFAULT_FONT);
		jMenuAbout.setText("About");
		jMenuAbout.setMnemonic('A');

		jMenuItemAboutSubDisc.setFont(DEFAULT_FONT);
		jMenuItemAboutSubDisc.setText("SubDisc");
		jMenuItemAboutSubDisc.setMnemonic('I');
		jMenuItemAboutSubDisc.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		jMenuItemAboutSubDisc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAboutSubDiscActionPerformed(evt);
			}
		});
		jMenuAbout.add(jMenuItemAboutSubDisc);

		jMiningWindowMenuBar.add(jMenuAbout);

		jPanelCenter.setLayout(new GridLayout(2, 2));

		// setting up - dataset ================================================
		jPanelRuleTarget.setLayout(new BorderLayout(40, 0));
		jPanelRuleTarget.setBorder(new TitledBorder(new EtchedBorder(),
				"dataset", 4, 2, new Font("Dialog", 1, 11)));
		jPanelRuleTarget.setFont(new Font("Dialog", 1, 12));

		jPanelRuleTargetLabels.setLayout(new GridLayout(7, 1));

		jLabelTargetTable = initJLabel(" target table");
		jPanelRuleTargetLabels.add(jLabelTargetTable);

		jLabelNrExamples = initJLabel(" # examples");
		jPanelRuleTargetLabels.add(jLabelNrExamples);

		jLabelNrColumns = initJLabel(" # columns");
		jPanelRuleTargetLabels.add(jLabelNrColumns);

		jLabelNrNominals = initJLabel(" # nominals");
		jPanelRuleTargetLabels.add(jLabelNrNominals);

		jLabelNrNumerics = initJLabel(" # numerics");
		jPanelRuleTargetLabels.add(jLabelNrNumerics);

		jLabelNrBinaries = initJLabel(" # binaries");
		jPanelRuleTargetLabels.add(jLabelNrBinaries);

		jPanelRuleTarget.add(jPanelRuleTargetLabels, BorderLayout.WEST);

		jPanelRuleTargetFields.setLayout(new GridLayout(7, 1));

		jLFieldTargetTable.setForeground(Color.black);
		jLFieldTargetTable.setFont(DEFAULT_FONT);
		jPanelRuleTargetFields.add(jLFieldTargetTable);

		jLFieldNrExamples.setForeground(Color.black);
		jLFieldNrExamples.setFont(DEFAULT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrExamples);

		jLFieldNrColumns.setForeground(Color.black);
		jLFieldNrColumns.setFont(DEFAULT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrColumns);

		jLFieldNrNominals.setForeground(Color.black);
		jLFieldNrNominals.setFont(DEFAULT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrNominals);

		jLFieldNrNumerics.setForeground(Color.black);
		jLFieldNrNumerics.setFont(DEFAULT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrNumerics);

		jLFieldNrBinaries.setForeground(Color.black);
		jLFieldNrBinaries.setFont(DEFAULT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrBinaries);

		jPanelRuleTarget.add(jPanelRuleTargetFields, BorderLayout.CENTER);

		jPanelRuleTargetButtons.setLayout(new BoxLayout(jPanelRuleTargetButtons , BoxLayout.X_AXIS));
//		jButtonDataExplorer = initButton("Data Explorer", 'E');
//		jButtonDataExplorer.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent evt) {
//				DataExplorerActionPerformed(evt);
//			}
//		});
//		jPanelRuleTargetButtons.add(jButtonDataExplorer);

		jButtonBrowse = initButton("Browse", 'B');
		jButtonBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				BrowseActionPerformed(evt);
			}
		});
		jPanelRuleTargetButtons.add(jButtonBrowse);

		jButtonAttributeTypeChange = initButton("Attributes", 'A');
		jButtonAttributeTypeChange.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				AttributeTypeChangeActionPerformed(evt);
			}
		});
		jPanelRuleTargetButtons.add(jButtonAttributeTypeChange);

		jPanelRuleTarget.add(jPanelRuleTargetButtons, BorderLayout.SOUTH);
		jPanelCenter.add(jPanelRuleTarget);	// MM

		// setting up - target concept - labels ================================
		jPanelRuleEvaluation.setLayout(new BoxLayout(jPanelRuleEvaluation, 0));
		jPanelRuleEvaluation.setBorder(new TitledBorder(new EtchedBorder(),
				"target concept", 4, 2, new Font("Dialog", 1, 11)));
		jPanelRuleEvaluation.setFont(new Font("Dialog", 1, 12));

		jPanelEvaluationLabels.setLayout(new GridLayout(8, 1));

		jComboBoxTargetType.setPreferredSize(new Dimension(86, 22));
		jComboBoxTargetType.setMinimumSize(new Dimension(86, 22));
		jComboBoxTargetType.setFont(DEFAULT_FONT);
		jComboBoxTargetType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxTargetTypeActionPerformed(evt);
			}
		});
		jPanelEvaluationFields.add(jComboBoxTargetType);

		jLabelTargetType = initJLabel(" target type");
		jPanelEvaluationLabels.add(jLabelTargetType);

		jLabelQualityMeasure = initJLabel(" quality measure");
		jPanelEvaluationLabels.add(jLabelQualityMeasure);

		jLabelEvaluationTreshold = initJLabel(" measure minimum");
		jPanelEvaluationLabels.add(jLabelEvaluationTreshold);

		jLabelTargetAttribute = initJLabel(" primary target");
		jPanelEvaluationLabels.add(jLabelTargetAttribute);

		jLabelMiscField = initJLabel("");
		jPanelEvaluationLabels.add(jLabelMiscField);

		jLabelSecondaryTargets = initJLabel(" secondary targets");
		jPanelEvaluationLabels.add(jLabelSecondaryTargets);

		jLabelTargetInfo = initJLabel("");;
		jPanelEvaluationLabels.add(jLabelTargetInfo);
		jPanelRuleEvaluation.add(jPanelEvaluationLabels);

		// setting up - target concept - fields ================================
		jPanelEvaluationFields.setLayout(new GridLayout(8, 1));

		jComboBoxQualityMeasure.setPreferredSize(new Dimension(86, 22));
		jComboBoxQualityMeasure.setMinimumSize(new Dimension(86, 22));
		jComboBoxQualityMeasure.setFont(DEFAULT_FONT);
		jComboBoxQualityMeasure.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxQualityMeasureActionPerformed(evt);
			}
		});
		jPanelEvaluationFields.add(jComboBoxQualityMeasure);

		jTextFieldQualityMeasureMinimum.setPreferredSize(new Dimension(86, 22));
		jTextFieldQualityMeasureMinimum.setFont(DEFAULT_FONT);
		jTextFieldQualityMeasureMinimum.setText("0");
		jTextFieldQualityMeasureMinimum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldQualityMeasureMinimum.setMinimumSize(new Dimension(86, 22));
		jPanelEvaluationFields.add(jTextFieldQualityMeasureMinimum);

		jComboBoxTargetAttribute.setPreferredSize(new Dimension(86, 22));
		jComboBoxTargetAttribute.setMinimumSize(new Dimension(86, 22));
		jComboBoxTargetAttribute.setFont(DEFAULT_FONT);
		jComboBoxTargetAttribute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxTargetAttributeActionPerformed(evt);
			}
		});
		jPanelEvaluationFields.add(jComboBoxTargetAttribute);

		jComboBoxMiscField.setFont(DEFAULT_FONT);
		jComboBoxMiscField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxMiscFieldActionPerformed(evt);
			}
		});
		jPanelEvaluationFields.add(jComboBoxMiscField);

		jListSecondaryTargets.setPreferredSize(new Dimension(86, 30));
		jListSecondaryTargets.setMinimumSize(new Dimension(86, 22));
		jListSecondaryTargets.setFont(DEFAULT_FONT);
		jListSecondaryTargets.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent evt) {
				jListSecondaryTargetsActionPerformed(evt);
			}
		});
		jPanelEvaluationFields.add(jListSecondaryTargets);

		jLFieldTargetInfo.setForeground(Color.black);
		jLFieldTargetInfo.setFont(DEFAULT_FONT);
		jPanelEvaluationFields.add(jLFieldTargetInfo);

//		jButtonBaseModel.setPreferredSize(new Dimension(86, 22));
//		jButtonBaseModel.setMaximumSize(new Dimension(95, 25));
//		jButtonBaseModel.setMinimumSize(new Dimension(82, 25));
		jButtonBaseModel = initButton("Base Model", 'M');
		jButtonBaseModel.setMnemonic('M');
		jButtonBaseModel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonBaseModelActionPerformed(evt);
			}
		});
		jPanelEvaluationFields.add(jButtonBaseModel);

		jPanelRuleEvaluation.add(jPanelEvaluationFields);
		jPanelCenter.add(jPanelRuleEvaluation);		// MM

		// setting up - search conditions ======================================
		jPanelSearchParameters.setLayout(new BoxLayout(jPanelSearchParameters, 0));
		jPanelSearchParameters.setBorder(new TitledBorder(new EtchedBorder(),
				"search conditions", 4, 2, new Font("Dialog", 1, 11)));
		jPanelSearchParameters.setFont(new Font("Dialog", 1, 12));

		jPanelSearchParameterLabels.setLayout(new GridLayout(7, 1));

		jLabelSearchDepth = initJLabel(" refinement depth");
		jPanelSearchParameterLabels.add(jLabelSearchDepth);

		jLabelSearchCoverageMinimum = initJLabel(" minimum coverage");
		jPanelSearchParameterLabels.add(jLabelSearchCoverageMinimum);

		jLabelSearchCoverageMaximum = initJLabel(" coverage fraction");
		jPanelSearchParameterLabels.add(jLabelSearchCoverageMaximum);

		jLabelSubgroupsMaximum = initJLabel(" maximum subgroups");
		jPanelSearchParameterLabels.add(jLabelSubgroupsMaximum);

		jLabelSearchTimeMaximum = initJLabel(" maximum time (min)");
		jPanelSearchParameterLabels.add(jLabelSearchTimeMaximum);

		jPanelSearchParameters.add(jPanelSearchParameterLabels);

		jPanelSearchParameterFields.setLayout(new GridLayout(7, 1));

		jTextFieldSearchDepth.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchDepth.setFont(DEFAULT_FONT);
		jTextFieldSearchDepth.setText("0");
		jTextFieldSearchDepth.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchDepth.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSearchDepth);

		jTextFieldSearchCoverageMinimum.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchCoverageMinimum.setFont(DEFAULT_FONT);
		jTextFieldSearchCoverageMinimum.setText("0");
		jTextFieldSearchCoverageMinimum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchCoverageMinimum.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSearchCoverageMinimum);

		jTextFieldSearchCoverageMaximum.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchCoverageMaximum.setFont(DEFAULT_FONT);
		jTextFieldSearchCoverageMaximum.setText("0");
		jTextFieldSearchCoverageMaximum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchCoverageMaximum.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSearchCoverageMaximum);

		jTextFieldSubgroupsMaximum.setPreferredSize(new Dimension(86, 22));
		jTextFieldSubgroupsMaximum.setFont(DEFAULT_FONT);
		jTextFieldSubgroupsMaximum.setText("0");
		jTextFieldSubgroupsMaximum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSubgroupsMaximum.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSubgroupsMaximum);

		jTextFieldSearchTimeMaximum.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchTimeMaximum.setFont(DEFAULT_FONT);
		jTextFieldSearchTimeMaximum.setText("0");
		jTextFieldSearchTimeMaximum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchTimeMaximum.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSearchTimeMaximum);

		jPanelSearchParameters.add(jPanelSearchParameterFields);
		jPanelCenter.add(jPanelSearchParameters);	// MM

		// setting up - search strategy ========================================
		jPanelSearchStrategy.setLayout(new BoxLayout(jPanelSearchStrategy, 0));
		jPanelSearchStrategy.setBorder(new TitledBorder(
			new EtchedBorder(), "search strategy", 4, 2, new Font("Dialog", 1, 11)));
		jPanelSearchStrategy.setFont(new Font("Dialog", 1, 12));

		jPanelSearchStrategyLabels.setLayout(new GridLayout(7, 1));

		jLabelStrategyType = initJLabel(" strategy type");
		jPanelSearchStrategyLabels.add(jLabelStrategyType);

		jLabelStrategyWidth = initJLabel(" search width");
		jPanelSearchStrategyLabels.add(jLabelStrategyWidth);

		jLabelSearchStrategyNumericFrr = initJLabel(" best numeric");
		jPanelSearchStrategyLabels.add(jLabelSearchStrategyNumericFrr);

		jLabelSearchStrategyNrBins = initJLabel(" number of bins");
		jPanelSearchStrategyLabels.add(jLabelSearchStrategyNrBins);

		jPanelSearchStrategy.add(jPanelSearchStrategyLabels);

		jPanelSearchStrategyFields.setLayout(new GridLayout(7, 1));

		jComboBoxSearchStrategyType.setPreferredSize(new Dimension(86, 22));
		jComboBoxSearchStrategyType.setMinimumSize(new Dimension(86, 22));
		jComboBoxSearchStrategyType.setFont(DEFAULT_FONT);
		jComboBoxSearchStrategyType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxSearchStrategyTypeActionPerformed(evt);
			}
		});
		jPanelSearchStrategyFields.add(jComboBoxSearchStrategyType);

		jTextFieldSearchStrategyWidth.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchStrategyWidth.setFont(DEFAULT_FONT);
		jTextFieldSearchStrategyWidth.setText("0");
		jTextFieldSearchStrategyWidth.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchStrategyWidth.setMinimumSize(new Dimension(86, 22));
		jPanelSearchStrategyFields.add(jTextFieldSearchStrategyWidth);

		jComboBoxSearchStrategyNumeric.setPreferredSize(new Dimension(86, 22));
		jComboBoxSearchStrategyNumeric.setMinimumSize(new Dimension(86, 22));
		jComboBoxSearchStrategyNumeric.setFont(DEFAULT_FONT);
		jComboBoxSearchStrategyNumeric.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxSearchStrategyNumericActionPerformed(evt);
			}
		});
		jPanelSearchStrategyFields.add(jComboBoxSearchStrategyNumeric);

		jTextFieldSearchStrategyNrBins.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchStrategyNrBins.setFont(DEFAULT_FONT);
		jTextFieldSearchStrategyNrBins.setText("0");
		jTextFieldSearchStrategyNrBins.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchStrategyNrBins.setMinimumSize(new Dimension(86, 22));
		jPanelSearchStrategyFields.add(jTextFieldSearchStrategyNrBins);

		jPanelSearchStrategy.add(jPanelSearchStrategyFields);
		jPanelCenter.add(jPanelSearchStrategy);	// MM

		// setting up - mining buttons =========================================
		jPanelSouth.setFont(DEFAULT_FONT);
/*
		jLabelLayoutFiller0.setPreferredSize(new Dimension(0, 40));
		jLabelLayoutFiller0.setMinimumSize(new Dimension(0, 40));
		jPanelSouth.add(jLabelLayoutFiller0);

		jPanelSouth.add(jPanelLayoutFiller1);
*/
		jPanelMineButtons.setMinimumSize(new Dimension(0, 40));

		jButtonSubgroupDiscovery = initButton("Subgroup Discovery", 'S');
		jButtonSubgroupDiscovery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonSubgroupDiscoveryActionPerformed(evt);
			}
		});
		jPanelMineButtons.add(jButtonSubgroupDiscovery);

		jButtonRandomSubgroups = initButton("Random Subgroups", 'R');
		jButtonRandomSubgroups.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonRandomSubgroupsActionPerformed(evt);
			}
		});
		jPanelMineButtons.add(jButtonRandomSubgroups);

		jButtonRandomConditions = initButton("Random Conditions", 'C');
		jButtonRandomConditions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonRandomConditionsActionPerformed(evt);
			}
		});
		jPanelMineButtons.add(jButtonRandomConditions);

		jPanelSouth.add(jPanelMineButtons);
//		jPanelSouth.add(jPanelLayoutFiller2);
//		jPanelSouth.add(jLabelLayoutFiller3);

		getContentPane().add(jPanelSouth, BorderLayout.SOUTH);
		getContentPane().add(jPanelCenter, BorderLayout.CENTER);

		setFont(DEFAULT_FONT);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				exitForm(evt);
			}
		});

		setJMenuBar(jMiningWindowMenuBar);
	}

	private void enableTableDependentComponents(boolean theSetting)
	{
		List<? extends AbstractButton> dataModelDependentComponents =
			new ArrayList<AbstractButton>(Arrays.asList(new AbstractButton[]
														{
															jMenuItemDataExplorer,
															jMenuItemBrowseTarget,
															jMenuItemAttributeTypeChange,
															jMenuItemSubgroupDiscovery,
															jMenuItemCreateAutoRunFile,
															jMenuItemAddToAutoRunFile,
//															jButtonDataExplorer,  //TODO add when implemented
															jButtonBrowse,
															jButtonAttributeTypeChange,
															jButtonSubgroupDiscovery,
															jButtonRandomSubgroups,
															jButtonRandomConditions,
															jButtonBaseModel
														}));

		for (AbstractButton a : dataModelDependentComponents)
			a.setEnabled(theSetting);
	}

	/* MENU ITEMS */
	private void jMenuItemOpenFileActionPerformed(ActionEvent evt)
	{
		Table aTable = new FileHandler(Action.OPEN_FILE).getTable();
		if (aTable != null)
		{
			itsTable = aTable;
// TODO			initGuiComponents(false);
		}
	}

	private void jMenuItemAutoRunFileActionPerformed(AutoRun theFileOption)
	{
		setupSearchParameters();
		new XMLAutoRun(itsSearchParameters, itsTable, theFileOption);
	}

	private void jMenuItemAboutSubDiscActionPerformed(ActionEvent evt)
	{
		// TODO
		JOptionPane.showMessageDialog(null, "Subgroup Discovery", "About SubDisc",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void jMenuItemExitActionPerformed(ActionEvent evt)
	{
		Log.logCommandLine("exit");
		dispose();
		System.exit(0);
	}

	/* DATASET BUTTONS */
	private void BrowseActionPerformed(ActionEvent evt)
	{
		new TableWindow(itsTable);
	}

	private void DataExplorerActionPerformed(ActionEvent evt)
	{
		// TODO
		// DataExplorerWindow aDataExplorerWindow = new
		// DataExplorerWindow(itsDataModel);
		// aDataExplorerWindow.setLocation(30, 150);
		// aDataExplorerWindow.setTitle("Explore data model: " +
		// itsDataModel.getName());
		// aDataExplorerWindow.setVisible(true);
	}

	private void AttributeTypeChangeActionPerformed(ActionEvent evt)
	{
		AttributeChangeWindow anACW = new AttributeChangeWindow(itsTable);
		anACW.setLocation(100, 100);
		anACW.setSize(800, 700);
	}

	/* Exit application */
	private void exitForm(WindowEvent evt)
	{
		dispose();
		System.exit(0);
	}

	private void jComboBoxSearchStrategyTypeActionPerformed(ActionEvent evt)
	{
		String aName = getSearchStrategyName();
		if (aName != null)
		{
			boolean aBestFirst = SearchParameters.getSearchStrategyName(CandidateQueue.BESTFIRST).equalsIgnoreCase(aName);
			itsSearchParameters.setSearchStrategy(aName);
			jTextFieldSearchStrategyWidth.setEnabled(!aBestFirst);
		}
	}

	private void jComboBoxSearchStrategyNumericActionPerformed(ActionEvent evt)
	{
		String aName = getNumericStrategy();
		if (aName != null)
		{
			itsSearchParameters.setNumericStrategy(aName);
			boolean aBin = (itsSearchParameters.getNumericStrategy() == NumericStrategy.NUMERIC_BINS);
			jTextFieldSearchStrategyNrBins.setEnabled(aBin);
		}
	}

	private void jComboBoxQualityMeasureActionPerformed(ActionEvent evt)
	{
		itsSearchParameters.setQualityMeasureMinimum(getQualityMeasureMinimum());
		initEvaluationMinimum();
	}

	private void jComboBoxTargetAttributeActionPerformed(ActionEvent evt)
	{
		itsTargetConcept.setPrimaryTarget(itsTable.getAttribute(getTargetAttributeName()));
		itsSearchParameters.setTargetConcept(itsTargetConcept);

		TargetType aTargetType = itsTargetConcept.getTargetType();

		if (getTargetAttributeName() != null &&
			(aTargetType == TargetType.SINGLE_NOMINAL ||
				aTargetType == TargetType.MULTI_BINARY_CLASSIFICATION))
		{
			Log.logCommandLine("init");
			initTargetValueItems();
		}

		// TODO these test could be member functions in TargetType?
		// has MiscField?
		boolean hasMiscField = (aTargetType == TargetType.SINGLE_NOMINAL ||
								aTargetType == TargetType.DOUBLE_REGRESSION ||
								aTargetType == TargetType.DOUBLE_CORRELATION ||
								aTargetType == TargetType.MULTI_BINARY_CLASSIFICATION);
		jComboBoxMiscField.setVisible(hasMiscField);
		jLabelMiscField.setVisible(hasMiscField);

		if (aTargetType == TargetType.SINGLE_NOMINAL ||
			aTargetType == TargetType.MULTI_BINARY_CLASSIFICATION)
			jLabelMiscField.setText(" target value");
		else
			jLabelMiscField.setText(" secondary target");

		// has secondary targets (JList)?
		boolean hasSecondaryTargets = (aTargetType == TargetType.MULTI_LABEL ||
										aTargetType == TargetType.MULTI_BINARY_CLASSIFICATION);
		jLabelSecondaryTargets.setVisible(hasSecondaryTargets);
		jListSecondaryTargets.setVisible(hasSecondaryTargets);
		SecondaryTargets.setVisible(hasSecondaryTargets);

		// has target attribute?
		boolean hasTargetAttribute = (aTargetType != TargetType.MULTI_LABEL);
		jLabelTargetAttribute.setVisible(hasTargetAttribute);
		jComboBoxTargetAttribute.setVisible(hasTargetAttribute);

		// has base model?
		boolean hasBaseModel = (aTargetType == TargetType.DOUBLE_CORRELATION ||
								aTargetType == TargetType.DOUBLE_REGRESSION ||
								aTargetType == TargetType.MULTI_LABEL);
		jButtonBaseModel.setEnabled(hasBaseModel);
	}

	private void jComboBoxTargetTypeActionPerformed(ActionEvent evt)
	{
		if (itsTable == null)
			return;

		itsTargetConcept.setTargetType(getTargetTypeName());
		itsSearchParameters.setTargetConcept(itsTargetConcept);

		initQualityMeasure();
		initTargetAttributeItems();
	}

	private void jComboBoxMiscFieldActionPerformed(ActionEvent evt)
	{
		switch(itsTargetConcept.getTargetType())
		{
			case SINGLE_NOMINAL :
			case MULTI_BINARY_CLASSIFICATION :
			{
					itsTargetConcept.setTargetValue(getMiscFieldName());
					break;
			}
			case DOUBLE_REGRESSION :
			case DOUBLE_CORRELATION :
			{
				itsTargetConcept.setSecondaryTarget(itsTable.getAttribute(getTargetAttributeName()));
				break;
			}
			default : break;
		}
		itsSearchParameters.setTargetConcept(itsTargetConcept);

		if (getMiscFieldName() != null)
			initTargetInfo();
	}

	private void jListSecondaryTargetsActionPerformed(ListSelectionEvent evt)
	{
		// compute selected targets and update TargetConcept
		int[] aSelection = jListSecondaryTargets.getSelectedIndices();
		ArrayList<Attribute> aList = new ArrayList<Attribute>(aSelection.length);
		for (int anIndex : aSelection)
			aList.add(itsTable.getAttribute(itsTable.getBinaryIndex(anIndex)));
		itsTargetConcept.setMultiTargets(aList);

		//update GUI
		initTargetInfo();
	}

	private void jButtonBaseModelActionPerformed(ActionEvent evt)
	{
		try
		{
			setupSearchParameters();

			ModelWindow aWindow;
			switch(itsTargetConcept.getTargetType())
			{
				case DOUBLE_REGRESSION :
				{
					Attribute aPrimaryTarget = itsTargetConcept.getPrimaryTarget();
					Column aPrimaryColumn = itsTable.getColumn(aPrimaryTarget);
					Attribute aSecondaryTarget = itsTargetConcept.getSecondaryTarget();
					Column aSecondaryColumn = itsTable.getColumn(aSecondaryTarget);
					RegressionMeasure anRM = new RegressionMeasure(itsSearchParameters.getQualityMeasure(),
						aPrimaryColumn, aSecondaryColumn, null);

					aWindow = new ModelWindow(aPrimaryColumn, aSecondaryColumn,
							aPrimaryTarget.getName(), aSecondaryTarget.getName(), anRM);
					aWindow.setLocation(50, 50);
					aWindow.setSize(700, 700);
					aWindow.setVisible(true);
					aWindow.setTitle("Base Model");
					break;
				}
				case DOUBLE_CORRELATION :
				{
					Attribute aPrimaryTarget = itsTargetConcept.getPrimaryTarget();
					Column aPrimaryColumn = itsTable.getColumn(aPrimaryTarget);
					Attribute aSecondaryTarget = itsTargetConcept.getSecondaryTarget();
					Column aSecondaryColumn = itsTable.getColumn(aSecondaryTarget);

					aWindow = new ModelWindow(aPrimaryColumn, aSecondaryColumn,
							aPrimaryTarget.getName(), aSecondaryTarget.getName(), null); //no trendline
					aWindow.setLocation(50, 50);
					aWindow.setSize(700, 700);
					aWindow.setVisible(true);
					aWindow.setTitle("Base Model");
					break;
				}
				case MULTI_LABEL :
				{
					ArrayList<Attribute> aList = itsTargetConcept.getMultiTargets();
					String[] aNames = new String[aList.size()];
					int aCount = 0;
					for (Attribute anAttribute : aList)
					{
						aNames[aCount] = anAttribute.getName();
						aCount++;
					}

					// compute base model
					Bayesian aBayesian =
						new Bayesian(new BinaryTable(itsTable, aList), aNames);
					aBayesian.climb();
					DAG aBaseDAG = aBayesian.getDAG();
					aBaseDAG.print();

					aWindow = new ModelWindow(aBaseDAG, 1200, 900);
					aWindow.setLocation(0, 0);
					aWindow.setSize(1200, 900);
					aWindow.setVisible(true);
					aWindow.setTitle("Base Model: Bayesian Network");
					break;
				}
				default: return; // TODO other types not implemented yet
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/* MINING BUTTONS */
	private void jButtonSubgroupDiscoveryActionPerformed(ActionEvent evt)
	{
		try
		{
			setupSearchParameters();

			//TODO other types not implemented yet
			if (!itsTargetConcept.getTargetType().isImplemented())
				return;

			echoMiningStart();
			long aBegin = System.currentTimeMillis();

			SubgroupDiscovery aSubgroupDiscovery;
			switch(itsTargetConcept.getTargetType())
			{
				case SINGLE_NOMINAL :
				{
					aSubgroupDiscovery = new SubgroupDiscovery(itsSearchParameters, itsTable, itsPositiveCount);
					break;
				}
				case MULTI_LABEL :
				{
					aSubgroupDiscovery = new SubgroupDiscovery(itsSearchParameters, itsTable);
					break;
				}
				case DOUBLE_REGRESSION :
				{
					aSubgroupDiscovery = new SubgroupDiscovery(itsSearchParameters, itsTable, true);
					break;
				}
				case DOUBLE_CORRELATION :
				{
					aSubgroupDiscovery = new SubgroupDiscovery(itsSearchParameters, itsTable, false);
					break;
				}
				default : return; // TODO should never get here, throw warning
			}
			aSubgroupDiscovery.Mine(System.currentTimeMillis());

			//ResultWindow
			SubgroupSet aPreliminaryResults = aSubgroupDiscovery.getResult();
			ResultWindow aResultWindow;
			switch (itsTargetConcept.getTargetType())
			{
				case MULTI_LABEL :
				{
					BinaryTable aBinaryTable = new BinaryTable(itsTable, itsTargetConcept.getMultiTargets());
					aResultWindow = new ResultWindow(aPreliminaryResults, itsSearchParameters, null, itsTable, aBinaryTable, aSubgroupDiscovery.getQualityMeasure(), itsTotalCount);
					break;
				}
				default :
				{
					aResultWindow = new ResultWindow(aPreliminaryResults, itsSearchParameters, null, itsTable, aSubgroupDiscovery.getQualityMeasure(), itsTotalCount);
				}
			}
			aResultWindow.setLocation(0, 0);
			aResultWindow.setSize(1200, 900);
			aResultWindow.setVisible(true);

			long anEnd = System.currentTimeMillis();
			if (anEnd > aBegin + (long)(itsSearchParameters.getMaximumTime()*60*1000))
				JOptionPane.showMessageDialog(null, "Mining process ended prematurely due to time limit.",
												"Time Limit", JOptionPane.INFORMATION_MESSAGE);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			ErrorWindow aWindow = new ErrorWindow(e);
			aWindow.setLocation(200, 200);
			aWindow.setVisible(true);
		}
	}

	private void jButtonRandomSubgroupsActionPerformed(ActionEvent evt)
	{
		try
		{
			setupSearchParameters();

			String inputValue = JOptionPane.showInputDialog("Number of random subgroups to be used\nfor distribution estimation:");
			int aNrRepetitions;
			try
			{
				aNrRepetitions = Integer.parseInt(inputValue);
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(null, "Not a valid number.");
				return;
			}

			QualityMeasure aQualityMeasure;
			switch(itsTargetConcept.getTargetType())
			{
				case SINGLE_NOMINAL :
				{
					aQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsTable.getNrRows(), itsPositiveCount);
					break;
				}
				case MULTI_LABEL :
				{
					// base model
					BinaryTable aBaseTable = new BinaryTable(itsTable, itsTargetConcept.getMultiTargets());
					Bayesian aBayesian = new Bayesian(aBaseTable);
					aBayesian.climb();
					aQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), aBayesian.getDAG(), itsTotalCount, itsSearchParameters.getAlpha(), itsSearchParameters.getBeta());
					break;
				}
				case DOUBLE_REGRESSION :
				case DOUBLE_CORRELATION :
				{
					//base model
					aQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), itsTable.getNrRows(), 100); //TODO fix 100, is useless?
					break;
				}
				default : return; // TODO should never get here
			}

			Validation aValidation = new Validation(itsSearchParameters, itsTable, aQualityMeasure);
			NormalDistribution aDistro = aValidation.RandomSubgroups(aNrRepetitions);

			int aMethod = JOptionPane.showOptionDialog(null,
				"The following quality measure thresholds were computed:\n" +
				"1% significance level: " + aDistro.getOnePercentSignificance() + "\n" +
				"5% significance level: " + aDistro.getFivePercentSignificance() + "\n" +
				"10% significance level: " + aDistro.getTenPercentSignificance() + "\n" +
				"Would you like to keep one of these thresholds as search constraint?",
				"Keep quality measure threshold?",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new String[] {"1% significance", "5% significance", "10% significance", "Ignore statistics"},
				"1% significance");
			switch (aMethod)
			{
				case 0:
				{
					setQualityMeasureMinimum(Double.toString(aDistro.getOnePercentSignificance()));
					break;
				}
				case 1:
				{
					setQualityMeasureMinimum(Double.toString(aDistro.getFivePercentSignificance()));
					break;
				}
				case 2:
				{
					setQualityMeasureMinimum(Double.toString(aDistro.getTenPercentSignificance()));
					break;
				}
				case 3:
				{
					break; //discard statistics
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			ErrorWindow aWindow = new ErrorWindow(e);
			aWindow.setLocation(200, 200);
			aWindow.setVisible(true);
		}
	}

	private void jButtonRandomConditionsActionPerformed(ActionEvent evt)
	{
		setupSearchParameters();

		String inputValue = JOptionPane.showInputDialog("Number of random conditions to be used\nfor distribution estimation:");
		int aNrRepetitions;
		try
		{
			aNrRepetitions = Integer.parseInt(inputValue);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "Not a valid number.");
			return;
		}
		double[] aQualities = new double[aNrRepetitions];

		// base model
		BinaryTable aBaseTable = new BinaryTable(itsTable, itsTargetConcept.getMultiTargets());
		Bayesian aBayesian = new Bayesian(aBaseTable);
		aBayesian.climb();
		QualityMeasure aQualityMeasure = new QualityMeasure(itsSearchParameters.getQualityMeasure(), aBayesian.getDAG(), itsTotalCount, itsSearchParameters.getAlpha(), itsSearchParameters.getBeta());

		Validation aValidation = new Validation(itsSearchParameters, itsTable, aQualityMeasure);
		NormalDistribution aDistro = aValidation.RandomConditions(aNrRepetitions);

		int aMethod = JOptionPane.showOptionDialog(null,
			"The following quality measure thresholds were computed:\n" +
			"1% significance level: " + aDistro.getOnePercentSignificance() + "\n" +
			"5% significance level: " + aDistro.getFivePercentSignificance() + "\n" +
			"10% significance level: " + aDistro.getTenPercentSignificance() + "\n" +
			"Would you like to keep one of these thresholds as search constraint?",
			"Keep quality measure threshold?",
			JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
			new String[] {"1% significance", "5% significance", "10% significance", "Ignore statistics"},
			"1% significance");
		switch (aMethod)
		{
			case 0:
			{
				setQualityMeasureMinimum(Double.toString(aDistro.getOnePercentSignificance()));
				break;
			}
			case 1:
			{
				setQualityMeasureMinimum(Double.toString(aDistro.getFivePercentSignificance()));
				break;
			}
			case 2:
			{
				setQualityMeasureMinimum(Double.toString(aDistro.getTenPercentSignificance()));
				break;
			}
			case 3:
			{
				break; //discard statistics
			}
		}
	}

	private void setupSearchParameters()
	{
		initSearchParameters(itsSearchParameters);
		initTargetConcept();
	}

	// TODO can do without theSearchParameters, there is only itsSearchParameters
	private void initSearchParameters(SearchParameters theSearchParameters)
	{
		// theSearchParameters.setTarget(itsTable.getAttribute(getTargetAttributeName()));
		// theSearchParameters.setTargetAttributeShort(getTargetAttributeName());
		// theSearchParameters.setTargetValue(getMiscFieldName()); //only makes
		// sense for certain target concepts

		theSearchParameters.setQualityMeasure(getQualityMeasureName());
		theSearchParameters.setQualityMeasureMinimum(getQualityMeasureMinimum());

		theSearchParameters.setSearchDepth(getSearchDepthMaximum());
		theSearchParameters.setMinimumCoverage(getSearchCoverageMinimum());
		theSearchParameters.setMaximumCoverage(getSearchCoverageMaximum());
		theSearchParameters.setMaximumSubgroups(getSubgroupsMaximum());
		theSearchParameters.setMaximumTime(getSearchTimeMaximum());

		theSearchParameters.setSearchStrategy(getSearchStrategyName());
		theSearchParameters.setSearchStrategyWidth(getSearchStrategyWidth());
		theSearchParameters.setNumericStrategy(getNumericStrategy());
		theSearchParameters.setNrBins(getSearchStrategyNrBins());

		theSearchParameters.setPostProcessingCount(20);
		theSearchParameters.setMaximumPostProcessingSubgroups(100);

		// Bayesian stuff
		if (QualityMeasure.getMeasureString(QualityMeasure.EDIT_DISTANCE).equals(getQualityMeasureName()))
			theSearchParameters.setAlpha(0.0f);
		else
			theSearchParameters.setAlpha(0.5f);
		theSearchParameters.setBeta(1f);
	}

	// Obsolete, this info is already up to date through *ActionPerformed methods
	private void initTargetConcept()
	{
		Attribute aTarget = itsTable.getAttribute(getTargetAttributeName());
		itsTargetConcept.setPrimaryTarget(aTarget);

		// target value
		switch(itsTargetConcept.getTargetType())
		{
			case SINGLE_NOMINAL	:
			case MULTI_BINARY_CLASSIFICATION :
				itsTargetConcept.setTargetValue(getMiscFieldName());
				break;
			case DOUBLE_CORRELATION	:
			case DOUBLE_REGRESSION :
				itsTargetConcept.setSecondaryTarget(itsTable.getAttribute(getMiscFieldName())); break;
			default	: break;
		}
		// TODO add more details of target concept from GUI
	}

	public void echoMiningStart()
	{
		Log.logCommandLine("Mining process started");
	}

	public void echoMiningEnd(long theMilliSeconds, int theNumberOfSubgroups)
	{
		int seconds = Math.round(theMilliSeconds / 1000);
		int minutes = Math.round(theMilliSeconds / 60000);
		int secondsRemainder = seconds - (minutes * 60);
		String aString = new String("Mining process finished in " + minutes
				+ " minutes and " + secondsRemainder + " seconds.\n");

		if (theNumberOfSubgroups == 0)
			aString += "   No subgroups found that match the search criterion.\n";
		else if (theNumberOfSubgroups == 1)
			aString += "   1 subgroup found.\n";
		else
			aString += "   " + theNumberOfSubgroups + " subgroups found.\n";
		Log.logCommandLine(aString);
	}

	/* INITIALIZATION METHODS OF Window COMPONENTS */

	private void initTargetAttributeItems()
	{
		TargetType aTargetType = itsTargetConcept.getTargetType();

		// clear all
		removeAllTargetAttributeItems();
		if ((aTargetType == TargetType.DOUBLE_REGRESSION) ||
				(aTargetType == TargetType.DOUBLE_CORRELATION))
			removeAllMiscFieldItems();

		// primary target and MiscField
		boolean isEmpty = true;
		for (int i = 0; i < itsTable.getNrColumns(); i++) {
			Attribute anAttribute = itsTable.getAttribute(i);
			if ((aTargetType == TargetType.SINGLE_NOMINAL && anAttribute.isNominalType()) ||
					(aTargetType == TargetType.SINGLE_NOMINAL && anAttribute.isBinaryType()) ||
					(aTargetType == TargetType.SINGLE_NUMERIC && anAttribute.isNumericType()) ||
					(aTargetType == TargetType.SINGLE_ORDINAL && anAttribute.isNumericType()) ||
					(aTargetType == TargetType.DOUBLE_REGRESSION && anAttribute.isNumericType()) ||
					(aTargetType == TargetType.DOUBLE_CORRELATION && anAttribute.isNumericType()) ||
					(aTargetType == TargetType.MULTI_LABEL && anAttribute.isNumericType()) ||
					(aTargetType == TargetType.MULTI_BINARY_CLASSIFICATION && anAttribute.isBinaryType()) ||
					(aTargetType == TargetType.MULTI_BINARY_CLASSIFICATION && anAttribute.isNominalType()))
			{
				addTargetAttributeItem(itsTable.getAttribute(i).getName());
				if ((aTargetType == TargetType.DOUBLE_REGRESSION) ||
						(aTargetType == TargetType.DOUBLE_CORRELATION))
					addMiscFieldItem(itsTable.getAttribute(i).getName());

				isEmpty = false;
			}
		}
		if (aTargetType == TargetType.SINGLE_NOMINAL && isEmpty) // no target attribute selected
			removeAllMiscFieldItems();

		// multi targets =======================================
		if (aTargetType == TargetType.MULTI_LABEL && jListSecondaryTargets.getSelectedIndices().length == 0)
		{
			int aCount = 0;
			for (int i = 0; i < itsTable.getNrColumns(); i++)
			{
				Attribute anAttribute = itsTable.getAttribute(i);
				if (anAttribute.isBinaryType())
				{
					addSecondaryTargetsItem(itsTable.getAttribute(i).getName());
					aCount++;
				}
			}
			jListSecondaryTargets.setSelectionInterval(0, aCount - 1);
		}
	}

	private void initTargetValueItems()
	{
		removeAllMiscFieldItems();
		if (jComboBoxTargetAttribute.getItemCount() == 0) // no attributes for selected target concept type?
			return;

		// single target attribute
		// if(!TargetConcept.isEMM(getTargetTypeName()))
		// {
		Attribute aTarget = itsTable.getAttribute(getTargetAttributeName());
		TreeSet<String> aValues = itsTable.getDomain(aTarget.getIndex());
		for (String aValue : aValues)
			addMiscFieldItem(aValue);
		// }
	}

	private void initTargetInfo()
	{
		switch(itsTargetConcept.getTargetType())
		{
			case SINGLE_NOMINAL :
			{
				Attribute aTarget = itsTable.getAttribute(getTargetAttributeName());
				String aSelectedTargetValue = getMiscFieldName();
				itsPositiveCount = itsTable.countValues(aTarget.getIndex(), aSelectedTargetValue);
				float aPercentage = ((float) itsPositiveCount * 100f) / (float) itsTotalCount;
				NumberFormat aFormatter = NumberFormat.getNumberInstance();
				aFormatter.setMaximumFractionDigits(1);
				jLFieldTargetInfo.setText(itsPositiveCount + " (" + aFormatter.format(aPercentage) + " %)");
				jLabelTargetInfo.setText(" # positive");
				break;
			}
			case SINGLE_NUMERIC :
			{
				Attribute aTarget = itsTable.getAttribute(getTargetAttributeName());
				itsTargetAverage = itsTable.getAverage(aTarget.getIndex());
				jLabelTargetInfo.setText(" average");
				jLFieldTargetInfo.setText(String.valueOf(itsTargetAverage));
				break;
			}
			case SINGLE_ORDINAL :
			case DOUBLE_REGRESSION :
			case DOUBLE_CORRELATION :
			{
				initTargetConcept();
				Column aPrimaryColumn = itsTable.getColumn(itsTargetConcept.getPrimaryTarget());
				Column aSecondaryColumn = itsTable.getColumn(itsTargetConcept.getSecondaryTarget());
				CorrelationMeasure aCM =
					new CorrelationMeasure(QualityMeasure.CORRELATION_R, aPrimaryColumn, aSecondaryColumn);
				jLabelTargetInfo.setText(" correlation");
				jLFieldTargetInfo.setText(Double.toString(aCM.getEvaluationMeasureValue()));
				break;
			}
			case MULTI_LABEL :
			{
				int[] aSelection = jListSecondaryTargets.getSelectedIndices();
				jLFieldTargetInfo.setText(String.valueOf(aSelection.length));
				jLabelTargetInfo.setText(" binary targets");
				break;
			}
			case MULTI_BINARY_CLASSIFICATION :
			{
				jLabelTargetInfo.setText(" target info");
				jLFieldTargetInfo.setText("none");
				break;
			}
		}

		setSearchCoverageMinimum(Integer.toString(itsTotalCount / 10));
	}

	private void initQualityMeasure()
	{
		removeAllQualityMeasureItems();
		TargetType aTargetType = itsTargetConcept.getTargetType();

		for(int i = QualityMeasure.getFirstEvaluationMesure(aTargetType); i <= QualityMeasure.getLastEvaluationMesure(aTargetType); i++)
			addQualityMeasureItem(QualityMeasure.getMeasureString(i));
		initEvaluationMinimum();
	}

	private void initEvaluationMinimum()
	{
		if (getQualityMeasureName() != null)
			setQualityMeasureMinimum(QualityMeasure.getMeasureMinimum(getQualityMeasureName(), itsTargetAverage));
	}

	/* FIELD METHODS OF SUBDISC COMPONENTS */

	// target type - target type
	private String getTargetTypeName() { return (String) jComboBoxTargetType.getSelectedItem(); }
	private void setTargetTypeName(String aName) { jComboBoxTargetType.setSelectedItem(aName); }

	// target type - quality measure
	private String getQualityMeasureName() { return (String) jComboBoxQualityMeasure.getSelectedItem(); }
	private void setQualityMeasure(String aName) { jComboBoxQualityMeasure.setSelectedItem(aName); }
	private void addQualityMeasureItem(String anItem) { jComboBoxQualityMeasure.addItem(anItem); }
	private void removeAllQualityMeasureItems()
	{
		//	if(jComboBoxQualityMeasure.getItemCount() > 0)
		jComboBoxQualityMeasure.removeAllItems();
	}

	// target type - quality measure minimum (member of itsSearchParameters)
	private float getQualityMeasureMinimum() { return getValue(0.0F, jTextFieldQualityMeasureMinimum.getText()); }
	private void setQualityMeasureMinimum(String aValue) { jTextFieldQualityMeasureMinimum.setText(aValue); }

	// target type - target attribute
	private String getTargetAttributeName() { return (String) jComboBoxTargetAttribute.getSelectedItem(); }
	private void setTargetAttribute(String aName) { jComboBoxTargetAttribute.setSelectedItem(aName); }
	private void addTargetAttributeItem(String anItem) { jComboBoxTargetAttribute.addItem(anItem); }
	private void removeAllTargetAttributeItems() { jComboBoxTargetAttribute.removeAllItems(); }

	// target type - misc field (target value/secondary target)
	private void addMiscFieldItem(String anItem) { jComboBoxMiscField.addItem(anItem); }
	private void removeAllMiscFieldItems() { jComboBoxMiscField.removeAllItems(); }
	private String getMiscFieldName() { return (String) jComboBoxMiscField.getSelectedItem(); }

	private void addSecondaryTargetsItem(String theItem) { ((DefaultListModel) jListSecondaryTargets.getModel()).addElement(theItem); }
	private void removeAllSecondaryTargetsItems() { jListSecondaryTargets.removeAll(); }



	private void setSearchDepthMaximum(String aValue) { jTextFieldSearchDepth.setText(aValue); }
	private int getSearchDepthMaximum() { return getValue(1, jTextFieldSearchDepth.getText()); }
	private void setSearchCoverageMinimum(String aValue) { jTextFieldSearchCoverageMinimum.setText(aValue); }
	private void setSearchCoverageMaximum(String aValue) { jTextFieldSearchCoverageMaximum.setText(aValue); }
	private void setSubgroupsMaximum(String aValue) { jTextFieldSubgroupsMaximum.setText(aValue); }
	private void setSearchTimeMaximum(String aValue) { jTextFieldSearchTimeMaximum.setText(aValue); }
	private int getSearchCoverageMinimum() { return getValue(0, jTextFieldSearchCoverageMinimum.getText()); }
	private float getSearchCoverageMaximum() { return getValue(1.0F, jTextFieldSearchCoverageMaximum.getText()); }
	private int getSubgroupsMaximum() { return getValue(50, jTextFieldSubgroupsMaximum.getText());}
	private float getSearchTimeMaximum() { return getValue(1.0F, jTextFieldSearchTimeMaximum.getText()); }

	// search strategy - search strategy
	private String getSearchStrategyName() { return (String) jComboBoxSearchStrategyType.getSelectedItem(); }
	private void setSearchStrategyType(String aValue) { jComboBoxSearchStrategyType.setSelectedItem(aValue); }

	// search strategy - search width
	private int getSearchStrategyWidth() { return getValue(100, jTextFieldSearchStrategyWidth.getText()); }
	private void setSearchStrategyWidth(String aValue) { jTextFieldSearchStrategyWidth.setText(aValue); }

	// search strategy - numeric strategy
	private String getNumericStrategy() { return (String) jComboBoxSearchStrategyNumeric.getSelectedItem(); }
	private void setNumericStrategy(String aStrategy) { jComboBoxSearchStrategyNumeric.setSelectedItem(aStrategy); }

	// search strategy - number of bins
	private int getSearchStrategyNrBins() { return getValue(8, jTextFieldSearchStrategyNrBins.getText()); }
	private void setSearchStrategyNrBins(String aValue) { jTextFieldSearchStrategyNrBins.setText(aValue); }

	private int getValue(int theDefaultValue, String theText)
	{
		int aValue = theDefaultValue;
		try
		{
			aValue = Integer.parseInt(theText);
		}
		catch (Exception ex) {}	// TODO warning dialog
		return aValue;
	}

	private float getValue(float theDefaultValue, String theText)
	{
		float aValue = theDefaultValue;
		try
		{
			aValue = Float.parseFloat(theText);
		}
		catch (Exception ex) {}	// TODO warning dialog
		return aValue;
	}

	private JMenuBar jMiningWindowMenuBar;
	private JMenu jMenuFile;
	private JMenuItem jMenuItemOpenFile;
//	private JMenuItem jMenuItemShowDataModel;
	private JMenuItem jMenuItemDataExplorer;
	private JMenuItem jMenuItemBrowseTarget;
	private JMenuItem jMenuItemAttributeTypeChange;
	private JSeparator jSeparator2;
	private JMenuItem jMenuItemSubgroupDiscovery;
	private JSeparator jSeparator3;
	private JMenuItem jMenuItemCreateAutoRunFile;
	private JMenuItem jMenuItemAddToAutoRunFile;
//	private JMenuItem jMenuItemLoadAutoRunFile;	//TODO part of jMenuItemOpenFile
	private JSeparator jSeparator4;
	private JMenuItem jMenuItemExit;
	private JMenu jMenuAbout;
	private JMenuItem jMenuItemAboutSubDisc;
	private JPanel jPanelSouth;
//	private JLabel jLabelLayoutFiller0;
//	private JPanel jPanelLayoutFiller1;
	private JPanel jPanelMineButtons;
	// private JButton jButtonDataModel;
	private JButton jButtonDataExplorer;
	private JButton jButtonBrowse;
	private JButton jButtonAttributeTypeChange;
	private JButton jButtonSubgroupDiscovery;
	private JButton jButtonRandomSubgroups;
	private JButton jButtonRandomConditions;
//	private JPanel jPanelLayoutFiller2;
//	private JLabel jLabelLayoutFiller3;
	private JPanel jPanelCenter;
	private JPanel jPanelRuleTarget;
	private JPanel jPanelRuleTargetLabels;
	private JPanel jPanelRuleTargetButtons;
	private JLabel jLabelTargetTable;
	private JLabel jLabelTargetAttribute;
	private JLabel jLabelMiscField;
	private JLabel jLabelSecondaryTargets;
	private JLabel jLabelNrExamples;
	private JLabel jLabelNrColumns;
	private JLabel jLabelNrNominals;
	private JLabel jLabelNrNumerics;
	private JLabel jLabelNrBinaries;
	private JLabel jLabelTargetInfo;
	private JPanel jPanelRuleTargetFields;
	private JLabel jLFieldTargetTable;
	private JComboBox jComboBoxTargetAttribute;
	private JComboBox jComboBoxMiscField;
	private JList jListSecondaryTargets;
	private JScrollPane SecondaryTargets;
	private JLabel jLFieldNrExamples;
	private JLabel jLFieldNrColumns;
	private JLabel jLFieldNrNominals;
	private JLabel jLFieldNrNumerics;
	private JLabel jLFieldNrBinaries;
	private JLabel jLFieldTargetInfo;
	private JButton jButtonBaseModel;
	private JPanel jPanelRuleEvaluation;
	private JPanel jPanelEvaluationLabels;
	private JLabel jLabelTargetType;
	private JLabel jLabelQualityMeasure;
	private JLabel jLabelEvaluationTreshold;
	private JPanel jPanelEvaluationFields;
	private JComboBox jComboBoxQualityMeasure;
	private JComboBox jComboBoxTargetType;
	private JTextField jTextFieldQualityMeasureMinimum;
	private JPanel jPanelSearchParameters;
	private JPanel jPanelSearchParameterLabels;
	private JLabel jLabelSearchDepth;
	private JLabel jLabelSearchCoverageMinimum;
	private JLabel jLabelSearchCoverageMaximum;
	private JLabel jLabelSubgroupsMaximum;
	private JLabel jLabelSearchTimeMaximum;
	private JPanel jPanelSearchParameterFields;
	private JTextField jTextFieldSearchDepth;
	private JTextField jTextFieldSearchCoverageMinimum;
	private JTextField jTextFieldSearchCoverageMaximum;
	private JTextField jTextFieldSubgroupsMaximum;
	private JTextField jTextFieldSearchTimeMaximum;
	private JPanel jPanelSearchStrategy;
	private JPanel jPanelSearchStrategyLabels;
	private JLabel jLabelStrategyType;
	private JLabel jLabelStrategyWidth;
	private JLabel jLabelSearchStrategyNumericFrr;
	private JLabel jLabelSearchStrategyNrBins;
	private JPanel jPanelSearchStrategyFields;
	private JComboBox jComboBoxSearchStrategyType;
	private JTextField jTextFieldSearchStrategyWidth;
	private JComboBox jComboBoxSearchStrategyNumeric;
	private JTextField jTextFieldSearchStrategyNrBins;

	// GUI defaults and convenience methods
	private static final Font DEFAULT_FONT = new Font("Dialog", 0, 10);
//	private static final Font DEFAULT_BUTTON_FONT = new Font("Dialog", 1, 11);

	private static JButton initButton(String theName, int theMnemonic)
	{
		JButton aButton = new JButton();
		aButton.setPreferredSize(new Dimension(120, 25));
		aButton.setBorder(new BevelBorder(0));
		aButton.setMinimumSize(new Dimension(82, 25));
		aButton.setMaximumSize(new Dimension(130, 25));
		aButton.setFont(new Font ("Dialog", 1, 11));
		aButton.setText(theName);
		aButton.setMnemonic(theMnemonic);
		return aButton;
	}
	private enum Mnomics { }

	private static JLabel initJLabel(String theName)
	{
		JLabel aJLable = new JLabel(theName);
		aJLable.setFont(DEFAULT_FONT);
		return aJLable;
	}
}
