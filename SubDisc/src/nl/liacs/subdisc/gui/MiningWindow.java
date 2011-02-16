package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.UIManager.*;
import javax.swing.border.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.FileHandler.Action;
import nl.liacs.subdisc.SearchParameters.*;
import nl.liacs.subdisc.XMLAutoRun.*;
import nl.liacs.subdisc.cui.*;

public class MiningWindow extends JFrame
{
	static final long serialVersionUID = 1L;

	// TODO get image
	public static final Image ICON = Toolkit.getDefaultToolkit().getImage(MiningWindow.class.getResource("/icon.gif"));

	private Table itsTable;
	private int itsTotalCount;

	// target info
	private int itsPositiveCount; // nominal target
	private float itsTargetAverage; // numeric target

	// TODO there should be at most 1 MiningWindow();
	private final MiningWindow masterWindow = this;
	private SearchParameters itsSearchParameters = new SearchParameters();
	private TargetConcept itsTargetConcept = new TargetConcept();

	public MiningWindow()
	{
		initMiningWindow();
	}

	public MiningWindow(Table theTable)
	{
		if (theTable != null)
		{
			itsTable = theTable;
			itsTotalCount = itsTable.getNrRows();
			initMiningWindow();
			initGuiComponents();
		}
		else
			initMiningWindow();
	}

	// loaded from XML
	public MiningWindow(Table theTable, SearchParameters theSearchParameters)
	{
		if (theTable != null)
		{
			itsTable = theTable;
			itsTotalCount = itsTable.getNrRows();
			initMiningWindow();

			if (theSearchParameters != null)
				itsTargetConcept = theSearchParameters.getTargetConcept();
			itsSearchParameters = theSearchParameters;
			initGuiComponentsFromFile();
		}
		else
			initMiningWindow();
	}

	private void initMiningWindow()
	{
		// Initialise graphical components
		initComponents();
		initStaticGuiComponents();
		enableTableDependentComponents(itsTable != null);
		setTitle("CORTANA: Subgroup Discovery Tool");
		// setIconImage(ICON);
		initJMenuGui();
		pack();
		setSize(700, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);

		// Open log/debug files
		Log.openFileOutputStreams();
	}

	private void initStaticGuiComponents()
	{
		// TODO disable if no table?
		// should do for-each combobox/field
//		boolean hasTable = (itsTable != null);
		// Add all implemented TargetTypes
		for (TargetType t : TargetType.values())
			if (TargetType.isImplemented(t))
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

	// only called when Table is present, so using itsTotalCount is safe
	private void initGuiComponents()
	{
		//dataset
		initGuiComponentsDataSet();

		// target concept
		jButtonBaseModel.setEnabled(TargetType.hasBaseModel(itsTargetConcept.getTargetType()));

		// search conditions
		setSearchDepthMaximum("1");
		setSearchCoverageMinimum(String.valueOf(itsTotalCount / 10));
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
		// some fields may be set automatically, order is very important

		// search strategy
		setSearchStrategyType(SearchParameters.getSearchStrategyName(itsSearchParameters.getSearchStrategy()));
		setSearchStrategyWidth(String.valueOf(itsSearchParameters.getSearchStrategyWidth()));
		setNumericStrategy(itsSearchParameters.getNumericStrategy().TEXT);
		setSearchStrategyNrBins(String.valueOf(itsSearchParameters.getNrBins()));

		// search conditions
		/*
		 * setSearchStrategyType() above calls setSearchCoverageMinimum(),
		 * setting a wrong value in the GUI
		 * setSearchCoverageMinimum must be called AFTER setSearchStrategyType
		 */
		setSearchDepthMaximum(String.valueOf(itsSearchParameters.getSearchDepth()));
		setSearchCoverageMinimum(String.valueOf(itsSearchParameters.getMinimumCoverage()));
		setSearchCoverageMaximum(String.valueOf(itsSearchParameters.getMaximumCoverage()));
		setSubgroupsMaximum(String.valueOf(itsSearchParameters.getMaximumSubgroups()));
		setSearchTimeMaximum(String.valueOf(itsSearchParameters.getMaximumTime()));

		// target concept
		/*
		 * Remember for later reference, value will be overwritten by both
		 * setTargetTypeName() and setQualityMeasure()
		 */
		float originalMinimum = itsSearchParameters.getQualityMeasureMinimum();

		setTargetTypeName(itsTargetConcept.getTargetType().TEXT);
		setQualityMeasure(itsSearchParameters.getQualityMeasureString());
		// reset original value
		itsSearchParameters.setQualityMeasureMinimum(originalMinimum);
		setQualityMeasureMinimum(String.valueOf(itsSearchParameters.getQualityMeasureMinimum()));
		setTargetAttribute(itsTargetConcept.getPrimaryTarget().getName());

		/*
		 * Text in jTextFieldSearchCoverageMinimum is overwritten by
		 * initTargetInfo() which is called through:
		 * jComboBoxTargetTypeActionPerformed - initTargetAttributeItems.
		 */
		setSearchCoverageMinimum(String.valueOf(itsSearchParameters.getMinimumCoverage()));

//		setMiscField(itsTargetConcept.getSecondaryTarget());
//		setMiscField(itsTargetConcept.getTargetValue());
//		setSecondaryTargets(); // TODO initialised from primaryTargetList
	}

	private void initGuiComponentsDataSet()
	{
		if (itsTable != null)
		{
			jLFieldTargetTable.setText(itsTable.getName());
			jLFieldNrExamples.setText(String.valueOf(itsTotalCount));

			int[][] aCounts = itsTable.getTypeCounts();
			int[] aTotals = new int[] { itsTable.getNrColumns(), 0 };
			for (int[] ia : aCounts)
				aTotals[1] += ia[1];
			jLFieldNrColumns.setText(initGuiComponentsDataSetHelper(aTotals));
			jLFieldNrNominals.setText(initGuiComponentsDataSetHelper(aCounts[0]));
			jLFieldNrNumerics.setText(initGuiComponentsDataSetHelper(aCounts[1]));
//			jLFieldNrOrdinals.setText(initGuiComponentsDataSetHelper(aCounts[2]));
			jLFieldNrBinaries.setText(initGuiComponentsDataSetHelper(aCounts[3]));
		}
	}

	private String initGuiComponentsDataSetHelper(int[] theCounts)
	{
		int aCount = theCounts[0];
		String aNrEnabled = (aCount == 0 ? "" : " (" + theCounts[1] + " enabled)");
		String aSpacer = "          ";
		int i = 0;
		while ((aCount /= 10) > 0)
			i+=2;
		return String.format("%d%s%s", theCounts[0], aSpacer.substring(i), aNrEnabled);
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
		jMenuItemBrowse = new JMenuItem();
		jMenuItemMetaData = new JMenuItem();
//		jMenuItemDataExplorer = new JMenuItem();	// TODO add when implemented
		jMenuItemSubgroupDiscovery = new JMenuItem();
		jMenuItemCreateAutoRunFile = new JMenuItem();
		jMenuItemAddToAutoRunFile = new JMenuItem();
		jMenuItemExit = new JMenuItem();

		jMenuEnrichment = new JMenu();
		jMenuItemAddCuiEnrichmentSource = new JMenuItem();
		jMenuItemAddGoEnrichmentSource = new JMenuItem();
		jMenuItemAddCustomEnrichmentSource = new JMenuItem();
		jMenuItemRemoveEnrichmentSource = new JMenuItem();

		jMenuAbout = new JMenu();
		jMenuItemAboutCortana = new JMenuItem();
		jMenuGui = new JMenu();

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
		// dataset - number enabled fields
		jPanelRuleTargetFieldsEnabled = new JPanel();
		jLFieldNrColumnsEnabled = new JLabel();
		jLFieldNrNominalsEnabled = new JLabel();
		jLFieldNrNumericsEnabled = new JLabel();
		jLFieldNrBinariesEnabled = new JLabel();
		// dataset - buttons
		jPanelRuleTargetButtons = new JPanel();
		jButtonBrowse = new JButton();
		jButtonMetaData = new JButton();
		jButtonSwapRandomize = new JButton();
		jButtonCrossValidate = new JButton();

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
		jButtonSecondaryTargets = new JButton();	// shows jListSecondaryTargets
		jListSecondaryTargets = new JList(new DefaultListModel());	// permanently maintained
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
		jPanelMineButtons = new JPanel();
		jButtonSubgroupDiscovery = new JButton();
		jButtonRandomSubgroups = new JButton();
		jButtonRandomConditions = new JButton();

		// setting up - menu items
		jMiningWindowMenuBar.setFont(GUI.DEFAULT_TEXT_FONT);

		jMenuFile.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuFile.setText("File");
		jMenuFile.setMnemonic('F');

		jMenuItemOpenFile.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemOpenFile.setText("Open File");
		jMenuItemOpenFile.setMnemonic('O');
		jMenuItemOpenFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		jMenuItemOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemOpenFileActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemOpenFile);

		jMenuItemBrowse.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemBrowse.setText("Browse");
		jMenuItemBrowse.setMnemonic('B');
		jMenuItemBrowse.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK));
		jMenuItemBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				browseActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemBrowse);

		jMenuItemMetaData.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemMetaData.setText("Meta Data...");
		jMenuItemMetaData.setMnemonic('M');
		jMenuItemMetaData.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK));
		jMenuItemMetaData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				metaDataActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemMetaData);

/*
		// TODO add when implemented
		jMenuItemDataExplorer.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemDataExplorer.setText("Data Explorer");
		jMenuItemDataExplorer.setMnemonic('E');
		jMenuItemDataExplorer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		jMenuItemDataExplorer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				dataExplorerActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemDataExplorer);
*/

		jMenuFile.addSeparator();

		jMenuItemSubgroupDiscovery.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemSubgroupDiscovery.setText("Subgroup Discovery");
		jMenuItemSubgroupDiscovery.setMnemonic('S');
		jMenuItemSubgroupDiscovery.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		jMenuItemSubgroupDiscovery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonSubgroupDiscoveryActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemSubgroupDiscovery);

		jMenuFile.addSeparator();

		jMenuItemCreateAutoRunFile.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemCreateAutoRunFile.setText("Create Autorun File");
		jMenuItemCreateAutoRunFile.setMnemonic('C');
		jMenuItemCreateAutoRunFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		jMenuItemCreateAutoRunFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAutoRunFileActionPerformed(AutoRun.CREATE);
			}
		});
		jMenuFile.add(jMenuItemCreateAutoRunFile);

		jMenuItemAddToAutoRunFile.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemAddToAutoRunFile.setText("Add to Autorun File");
		jMenuItemAddToAutoRunFile.setMnemonic('A');
		jMenuItemAddToAutoRunFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
		jMenuItemAddToAutoRunFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAutoRunFileActionPerformed(AutoRun.ADD);
			}
		});
		jMenuFile.add(jMenuItemAddToAutoRunFile);

		jMenuFile.addSeparator();

		jMenuItemExit.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemExit.setText("Exit");
		jMenuItemExit.setMnemonic('X');
		jMenuItemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
		jMenuItemExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemExitActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemExit);
		jMiningWindowMenuBar.add(jMenuFile);

		jMenuEnrichment.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuEnrichment.setText("Enrichment");
		jMenuEnrichment.setMnemonic('E');

		jMenuItemAddCuiEnrichmentSource.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemAddCuiEnrichmentSource.setText("Add CUI Domain");
		jMenuItemAddCuiEnrichmentSource.setMnemonic('C');
//		jMenuItemAddCuiEnrichmentSource.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		jMenuItemAddCuiEnrichmentSource.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAddEnrichmentSourceActionPerformed(EnrichmentType.CUI);
			}
		});
		jMenuEnrichment.add(jMenuItemAddCuiEnrichmentSource);

		jMenuItemAddGoEnrichmentSource.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemAddGoEnrichmentSource.setText("Add GO Domain");
		jMenuItemAddGoEnrichmentSource.setMnemonic('G');
//		jMenuItemAddGoEnrichmentSource.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK));
		jMenuItemAddGoEnrichmentSource.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAddEnrichmentSourceActionPerformed(EnrichmentType.GO);
			}
		});
		jMenuEnrichment.add(jMenuItemAddGoEnrichmentSource);

		jMenuItemAddCustomEnrichmentSource.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemAddCustomEnrichmentSource.setText("Add Custom Source");
		jMenuItemAddCustomEnrichmentSource.setMnemonic('U');
//		jMenuItemAddCustomEnrichmentSource.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_MASK));
		jMenuItemAddCustomEnrichmentSource.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAddEnrichmentSourceActionPerformed(EnrichmentType.CUSTOM);
			}
		});
		jMenuEnrichment.add(jMenuItemAddCustomEnrichmentSource);

		jMenuItemRemoveEnrichmentSource.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemRemoveEnrichmentSource.setText("Remove Enrichment Source");
		jMenuItemRemoveEnrichmentSource.setMnemonic('R');
//		jMenuItemRemoveEnrichmentSource.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		jMenuItemRemoveEnrichmentSource.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemRemoveEnrichmentSourceActionPerformed();
			}
		});
		jMenuEnrichment.add(jMenuItemRemoveEnrichmentSource);

		jMiningWindowMenuBar.add(jMenuEnrichment);

		jMenuAbout.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuAbout.setText("About");
		jMenuAbout.setMnemonic('A');

		jMenuItemAboutCortana.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemAboutCortana.setText("Cortana");
		jMenuItemAboutCortana.setMnemonic('I');
		jMenuItemAboutCortana.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		jMenuItemAboutCortana.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAboutCortanaActionPerformed();
			}
		});
		jMenuAbout.add(jMenuItemAboutCortana);

		jMiningWindowMenuBar.add(jMenuAbout);
/*
		// TODO for testing only
		jMenuGui.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuGui.setText("Gui");
		jMenuGui.setMnemonic('G');
		jMiningWindowMenuBar.add(jMenuGui);
*/
		jPanelCenter.setLayout(new GridLayout(2, 2));

		// setting up - dataset ================================================
		jPanelRuleTarget.setLayout(new BorderLayout(40, 0));
		jPanelRuleTarget.setBorder(new TitledBorder(new EtchedBorder(),
				"Dataset", 4, 2, new Font("Dialog", 1, 11)));
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

		// number of instances per AttributeType
		jPanelRuleTargetFields.setLayout(new GridLayout(7, 1));

		jLFieldTargetTable.setForeground(Color.black);
		jLFieldTargetTable.setFont(GUI.DEFAULT_TEXT_FONT);
		jPanelRuleTargetFields.add(jLFieldTargetTable);

		jLFieldNrExamples.setForeground(Color.black);
		jLFieldNrExamples.setFont(GUI.DEFAULT_TEXT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrExamples);

		jLFieldNrColumns.setForeground(Color.black);
		jLFieldNrColumns.setFont(GUI.DEFAULT_TEXT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrColumns);

		jLFieldNrNominals.setForeground(Color.black);
		jLFieldNrNominals.setFont(GUI.DEFAULT_TEXT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrNominals);

		jLFieldNrNumerics.setForeground(Color.black);
		jLFieldNrNumerics.setFont(GUI.DEFAULT_TEXT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrNumerics);

		jLFieldNrBinaries.setForeground(Color.black);
		jLFieldNrBinaries.setFont(GUI.DEFAULT_TEXT_FONT);
		jPanelRuleTargetFields.add(jLFieldNrBinaries);

		jPanelRuleTarget.add(jPanelRuleTargetFields, BorderLayout.CENTER);

		// number of enabled instances per AttributeType
		jPanelRuleTargetFieldsEnabled.setLayout(new GridLayout(7, 1));

		jPanelRuleTargetFieldsEnabled.add(new JLabel(""));
		jPanelRuleTargetFieldsEnabled.add(new JLabel(""));

		jLFieldNrColumnsEnabled.setForeground(Color.black);
		jLFieldNrColumnsEnabled.setFont(GUI.DEFAULT_TEXT_FONT);
		jLFieldNrColumnsEnabled.setHorizontalAlignment(SwingConstants.LEFT);
		jPanelRuleTargetFieldsEnabled.add(jLFieldNrColumnsEnabled);

		jLFieldNrNominalsEnabled.setForeground(Color.black);
		jLFieldNrNominalsEnabled.setFont(GUI.DEFAULT_TEXT_FONT);
		jLFieldNrNominalsEnabled.setHorizontalAlignment(SwingConstants.LEFT);
		jPanelRuleTargetFieldsEnabled.add(jLFieldNrNominalsEnabled);

		jLFieldNrNumericsEnabled.setForeground(Color.black);
		jLFieldNrNumericsEnabled.setFont(GUI.DEFAULT_TEXT_FONT);
		jLFieldNrNumericsEnabled.setHorizontalAlignment(SwingConstants.LEFT);
		jPanelRuleTargetFieldsEnabled.add(jLFieldNrNumericsEnabled);

		jLFieldNrBinariesEnabled.setForeground(Color.black);
		jLFieldNrBinariesEnabled.setFont(GUI.DEFAULT_TEXT_FONT);
		jLFieldNrBinariesEnabled.setHorizontalAlignment(SwingConstants.LEFT);
		jPanelRuleTargetFieldsEnabled.add(jLFieldNrBinariesEnabled);

		jPanelRuleTarget.add(jPanelRuleTargetFieldsEnabled, BorderLayout.EAST);

//		jPanelRuleTargetButtons.setLayout(new BoxLayout(jPanelRuleTargetButtons , BoxLayout.X_AXIS));
/*
		// TODO add when implemented
		jButtonDataExplorer = initButton("Data Explorer", 'D');
		jButtonDataExplorer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				dataExplorerActionPerformed();
			}
		});
		jPanelRuleTargetButtons.add(jButtonDataExplorer);
*/
		jButtonBrowse = initButton("Browse", 'B');
		jButtonBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				browseActionPerformed();
			}
		});
		jPanelRuleTargetButtons.add(jButtonBrowse);

		jButtonMetaData = initButton("Meta Data...", 'M');
		jButtonMetaData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				metaDataActionPerformed();
			}
		});
		jPanelRuleTargetButtons.add(jButtonMetaData);

		jPanelRuleTarget.add(jPanelRuleTargetButtons, BorderLayout.SOUTH);
		jPanelCenter.add(jPanelRuleTarget);	// MM

		// setting up - target concept - labels ================================
		jPanelRuleEvaluation.setLayout(new BoxLayout(jPanelRuleEvaluation, 0));
		jPanelRuleEvaluation.setBorder(new TitledBorder(new EtchedBorder(),
				"Target Concept", 4, 2, new Font("Dialog", 1, 11)));
		jPanelRuleEvaluation.setFont(new Font("Dialog", 1, 12));

		jPanelEvaluationLabels.setLayout(new GridLayout(8, 1));

		jComboBoxTargetType.setPreferredSize(new Dimension(86, 22));
		jComboBoxTargetType.setMinimumSize(new Dimension(86, 22));
		jComboBoxTargetType.setFont(GUI.DEFAULT_TEXT_FONT);
		jComboBoxTargetType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxTargetTypeActionPerformed();
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

		jLabelSecondaryTargets = initJLabel(" binary targets");
		jPanelEvaluationLabels.add(jLabelSecondaryTargets);

		jLabelTargetInfo = initJLabel("");;
		jPanelEvaluationLabels.add(jLabelTargetInfo);
		jPanelRuleEvaluation.add(jPanelEvaluationLabels);

		// setting up - target concept - fields ================================
		jPanelEvaluationFields.setLayout(new GridLayout(8, 1));

		jComboBoxQualityMeasure.setPreferredSize(new Dimension(86, 22));
		jComboBoxQualityMeasure.setMinimumSize(new Dimension(86, 22));
		jComboBoxQualityMeasure.setFont(GUI.DEFAULT_TEXT_FONT);
		jComboBoxQualityMeasure.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxQualityMeasureActionPerformed();
			}
		});
		jPanelEvaluationFields.add(jComboBoxQualityMeasure);

		jTextFieldQualityMeasureMinimum.setPreferredSize(new Dimension(86, 22));
		jTextFieldQualityMeasureMinimum.setFont(GUI.DEFAULT_TEXT_FONT);
		jTextFieldQualityMeasureMinimum.setText("0");
		jTextFieldQualityMeasureMinimum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldQualityMeasureMinimum.setMinimumSize(new Dimension(86, 22));
		jPanelEvaluationFields.add(jTextFieldQualityMeasureMinimum);

		jComboBoxTargetAttribute.setPreferredSize(new Dimension(86, 22));
		jComboBoxTargetAttribute.setMinimumSize(new Dimension(86, 22));
		jComboBoxTargetAttribute.setFont(GUI.DEFAULT_TEXT_FONT);
		jComboBoxTargetAttribute.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxTargetAttributeActionPerformed();
			}
		});
		jPanelEvaluationFields.add(jComboBoxTargetAttribute);

		jComboBoxMiscField.setFont(GUI.DEFAULT_TEXT_FONT);
		jComboBoxMiscField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxMiscFieldActionPerformed();
			}
		});
		jPanelEvaluationFields.add(jComboBoxMiscField);

		jButtonSecondaryTargets = initButton("Select Targets", 'T');
		jButtonSecondaryTargets.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonSecondaryTargetsActionPerformed();
			}
		});
		jPanelEvaluationFields.add(jButtonSecondaryTargets);

		jLFieldTargetInfo.setForeground(Color.black);
		jLFieldTargetInfo.setFont(GUI.DEFAULT_TEXT_FONT);
		jPanelEvaluationFields.add(jLFieldTargetInfo);

//		jButtonBaseModel.setPreferredSize(new Dimension(86, 22));
//		jButtonBaseModel.setMaximumSize(new Dimension(95, 25));
//		jButtonBaseModel.setMinimumSize(new Dimension(82, 25));
		jButtonBaseModel = initButton("Base Model", 'B');
		jButtonBaseModel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonBaseModelActionPerformed();
			}
		});
		jPanelEvaluationFields.add(jButtonBaseModel);

		jPanelRuleEvaluation.add(jPanelEvaluationFields);
		jPanelCenter.add(jPanelRuleEvaluation);		// MM

		// setting up - search conditions ======================================
		jPanelSearchParameters.setLayout(new BoxLayout(jPanelSearchParameters, 0));
		jPanelSearchParameters.setBorder(new TitledBorder(new EtchedBorder(),
				"Search Conditions", 4, 2, new Font("Dialog", 1, 11)));
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
		jTextFieldSearchDepth.setFont(GUI.DEFAULT_TEXT_FONT);
		jTextFieldSearchDepth.setText("0");
		jTextFieldSearchDepth.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchDepth.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSearchDepth);

		jTextFieldSearchCoverageMinimum.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchCoverageMinimum.setFont(GUI.DEFAULT_TEXT_FONT);
		jTextFieldSearchCoverageMinimum.setText("0");
		jTextFieldSearchCoverageMinimum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchCoverageMinimum.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSearchCoverageMinimum);

		jTextFieldSearchCoverageMaximum.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchCoverageMaximum.setFont(GUI.DEFAULT_TEXT_FONT);
		jTextFieldSearchCoverageMaximum.setText("0");
		jTextFieldSearchCoverageMaximum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchCoverageMaximum.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSearchCoverageMaximum);

		jTextFieldSubgroupsMaximum.setPreferredSize(new Dimension(86, 22));
		jTextFieldSubgroupsMaximum.setFont(GUI.DEFAULT_TEXT_FONT);
		jTextFieldSubgroupsMaximum.setText("0");
		jTextFieldSubgroupsMaximum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSubgroupsMaximum.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSubgroupsMaximum);

		jTextFieldSearchTimeMaximum.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchTimeMaximum.setFont(GUI.DEFAULT_TEXT_FONT);
		jTextFieldSearchTimeMaximum.setText("0");
		jTextFieldSearchTimeMaximum.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchTimeMaximum.setMinimumSize(new Dimension(86, 22));
		jPanelSearchParameterFields.add(jTextFieldSearchTimeMaximum);

		jPanelSearchParameters.add(jPanelSearchParameterFields);
		jPanelCenter.add(jPanelSearchParameters);	// MM

		// setting up - search strategy ========================================
		jPanelSearchStrategy.setLayout(new BoxLayout(jPanelSearchStrategy, 0));
		jPanelSearchStrategy.setBorder(new TitledBorder(
			new EtchedBorder(), "Search Strategy", 4, 2, new Font("Dialog", 1, 11)));
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
		jComboBoxSearchStrategyType.setFont(GUI.DEFAULT_TEXT_FONT);
		jComboBoxSearchStrategyType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxSearchStrategyTypeActionPerformed();
			}
		});
		jPanelSearchStrategyFields.add(jComboBoxSearchStrategyType);

		jTextFieldSearchStrategyWidth.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchStrategyWidth.setFont(GUI.DEFAULT_TEXT_FONT);
		jTextFieldSearchStrategyWidth.setText("0");
		jTextFieldSearchStrategyWidth.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchStrategyWidth.setMinimumSize(new Dimension(86, 22));
		jPanelSearchStrategyFields.add(jTextFieldSearchStrategyWidth);

		jComboBoxSearchStrategyNumeric.setPreferredSize(new Dimension(86, 22));
		jComboBoxSearchStrategyNumeric.setMinimumSize(new Dimension(86, 22));
		jComboBoxSearchStrategyNumeric.setFont(GUI.DEFAULT_TEXT_FONT);
		jComboBoxSearchStrategyNumeric.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxSearchStrategyNumericActionPerformed();
			}
		});
		jPanelSearchStrategyFields.add(jComboBoxSearchStrategyNumeric);

		jTextFieldSearchStrategyNrBins.setPreferredSize(new Dimension(86, 22));
		jTextFieldSearchStrategyNrBins.setFont(GUI.DEFAULT_TEXT_FONT);
		jTextFieldSearchStrategyNrBins.setText("0");
		jTextFieldSearchStrategyNrBins.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldSearchStrategyNrBins.setMinimumSize(new Dimension(86, 22));
		jPanelSearchStrategyFields.add(jTextFieldSearchStrategyNrBins);

		jPanelSearchStrategy.add(jPanelSearchStrategyFields);
		jPanelCenter.add(jPanelSearchStrategy);	// MM

		// setting up - mining buttons =========================================
		jPanelSouth.setFont(GUI.DEFAULT_TEXT_FONT);

		jPanelMineButtons.setMinimumSize(new Dimension(0, 40));

		jButtonSubgroupDiscovery = initButton("Subgroup Discovery", 'S');
		jButtonSubgroupDiscovery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonSubgroupDiscoveryActionPerformed();
			}
		});
		jPanelMineButtons.add(jButtonSubgroupDiscovery);

		jButtonSwapRandomize = initButton("Swap Randomize", 'R');
		jButtonSwapRandomize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				swapRandomizeActionPerformed();
			}
		});
		jPanelMineButtons.add(jButtonSwapRandomize);

		jButtonCrossValidate = initButton("Cross-Validate", 'V');
		jButtonCrossValidate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				crossValidateActionPerformed();
			}
		});
		jPanelMineButtons.add(jButtonCrossValidate);

		jButtonRandomSubgroups = initButton("Random Subgroups", 'R');
		jButtonRandomSubgroups.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonRandomSubgroupsActionPerformed();
			}
		});
		jPanelMineButtons.add(jButtonRandomSubgroups);

		jButtonRandomConditions = initButton("Random Conditions", 'C');
		jButtonRandomConditions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonRandomConditionsActionPerformed();
			}
		});
		jPanelMineButtons.add(jButtonRandomConditions);

		jPanelSouth.add(jPanelMineButtons);

		getContentPane().add(jPanelSouth, BorderLayout.SOUTH);
		getContentPane().add(jPanelCenter, BorderLayout.CENTER);

		setFont(GUI.DEFAULT_TEXT_FONT);
		setJMenuBar(jMiningWindowMenuBar);
	}

	private void enableTableDependentComponents(boolean theSetting)
	{
		AbstractButton[] anAbstractButtonArray =
			new AbstractButton[] {	jMenuItemBrowse,
									jMenuItemMetaData,
									//jMenuItemDataExplorer,	//TODO add when implemented
									jMenuItemSubgroupDiscovery,
									jMenuItemCreateAutoRunFile,
									jMenuItemAddToAutoRunFile,
									jMenuItemAddCuiEnrichmentSource,
									jMenuItemAddGoEnrichmentSource,
									jMenuItemAddCustomEnrichmentSource,
									jMenuItemRemoveEnrichmentSource,
									jButtonBrowse,
									jButtonMetaData,
									jButtonSwapRandomize,
									jButtonCrossValidate,
									//jButtonDataExplorer,	//TODO add when implemented
									jButtonSubgroupDiscovery,
									jButtonRandomSubgroups,
									jButtonRandomConditions,
									jButtonSecondaryTargets,
									jButtonBaseModel};

		for (AbstractButton a : anAbstractButtonArray)
			a.setEnabled(theSetting);
	}

	// used if Table already exists, but is changed
	public void update()
	{
		// hack on hack, prevent resetting of primary target/measure minimum
		String aPrimaryTarget = getTargetAttributeName();
		String aMeasureMinimum = jTextFieldQualityMeasureMinimum.getText();

		initGuiComponentsDataSet();
		jComboBoxTargetTypeActionPerformed();	// update hack
		setTargetAttribute(aPrimaryTarget);
		setQualityMeasureMinimum(aMeasureMinimum);
	}

	/* MENU ITEMS */
	private void jMenuItemOpenFileActionPerformed()
	{
		FileHandler aFileHandler =  new FileHandler(Action.OPEN_FILE);

		Table aTable = aFileHandler.getTable();
		SearchParameters aSearchParameters = aFileHandler.getSearchParameters();

		if (aTable != null)
		{
			removeAllSecondaryTargetsItems(); // hack for now

			itsTable = aTable;
			itsTotalCount = itsTable.getNrRows();
			enableTableDependentComponents(true);

			// loaded from regular file
			if (aSearchParameters == null)
				initGuiComponents();
			// loaded from XML
			else
			{
				itsSearchParameters = aSearchParameters;
				// should not happen
				if (itsSearchParameters.getTargetConcept() == null)
					itsTargetConcept = new TargetConcept();
				else
					itsTargetConcept = itsSearchParameters.getTargetConcept();
				initGuiComponentsFromFile();
			}

			jComboBoxTargetTypeActionPerformed();	// update hack
		}
	}

	// TODO not on EDT
	private void jMenuItemAutoRunFileActionPerformed(AutoRun theFileOption)
	{
		setupSearchParameters();
		new XMLAutoRun(itsSearchParameters, itsTable, theFileOption);
	}

	private void jMenuItemExitActionPerformed()
	{
		Log.logCommandLine("exit");
		dispose();
		System.exit(0);
	}

	//cannot be run from the Event Dispatching Thread
	private void jMenuItemAddEnrichmentSourceActionPerformed(final EnrichmentType theType)
	{
		Thread aThread = new Thread()
		{
			public void run()
			{
				new FileHandler(itsTable, theType);
				update();
			}
		};
		aThread.start();
	}

	//TODO only present list of domains to remove
	private void jMenuItemRemoveEnrichmentSourceActionPerformed()
	{
		//TODO only works on all.txt
		String aDomainName = itsTable.getColumn(6).getName().substring(8);
		itsTable.removeDomain(aDomainName);
		update();
	}

	private void jMenuItemAboutCortanaActionPerformed()
	{
		// TODO
		JOptionPane.showMessageDialog(null,
						"Cortana: Subgroup Discovery Tool",
						"About Cortana",
						JOptionPane.INFORMATION_MESSAGE);
	}

	/* DATASET BUTTONS */
	private void browseActionPerformed()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new BrowseWindow(itsTable);
			}
		});
	}

	// TODO not on EDT
	private void dataExplorerActionPerformed()
	{
		// TODO
		// DataExplorerWindow aDataExplorerWindow = new
		// DataExplorerWindow(itsDataModel);
		// aDataExplorerWindow.setLocation(30, 150);
		// aDataExplorerWindow.setTitle("Explore data model: " +
		// itsDataModel.getName());
		// aDataExplorerWindow.setVisible(true);
	}

	private void metaDataActionPerformed()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new MetaDataWindow(masterWindow, itsTable);
			}
		});
	}

	private void swapRandomizeActionPerformed()
	{
		itsTable.swapRandomizeTarget(itsTargetConcept);
		JOptionPane.showMessageDialog(null, "Target column(s) have been swap-randomized.");
	}

	private void crossValidateActionPerformed()
	{
		int aK = 10; //TODO set k from GUI
		CrossValidation aCV = new CrossValidation(itsTable.getNrRows(), aK);
		for (int i=0; i<aK; i++)
		{
			BitSet aSet = aCV.getSet(i, true);
			Log.logCommandLine("size: " + aSet.cardinality());
			Table aTable = itsTable.select(aSet);

			runSubgroupDiscovery(aTable, (i+1), aSet);
		}
	}

	private void jComboBoxSearchStrategyTypeActionPerformed()
	{
		String aName = getSearchStrategyName();
		if (aName != null)
		{
			boolean aBestFirst = SearchParameters.getSearchStrategyName(CandidateQueue.BESTFIRST).equalsIgnoreCase(aName);
			itsSearchParameters.setSearchStrategy(aName);
			jTextFieldSearchStrategyWidth.setEnabled(!aBestFirst);
		}
	}

	private void jComboBoxSearchStrategyNumericActionPerformed()
	{
		String aName = getNumericStrategy();
		if (aName != null)
		{
			itsSearchParameters.setNumericStrategy(aName);
			boolean aBin = (itsSearchParameters.getNumericStrategy() == NumericStrategy.NUMERIC_BINS);
			jTextFieldSearchStrategyNrBins.setEnabled(aBin);
		}
	}

	private void jComboBoxQualityMeasureActionPerformed()
	{
		itsSearchParameters.setQualityMeasureMinimum(getQualityMeasureMinimum());
		initEvaluationMinimum();
	}

	private void jComboBoxTargetAttributeActionPerformed()
	{
		itsTargetConcept.setPrimaryTarget(itsTable.getAttribute(getTargetAttributeName()));
		itsSearchParameters.setTargetConcept(itsTargetConcept);

		TargetType aTargetType = itsTargetConcept.getTargetType();

		if (getTargetAttributeName() != null &&
			(aTargetType == TargetType.SINGLE_NOMINAL ||
				aTargetType == TargetType.MULTI_BINARY_CLASSIFICATION))
		{
			initTargetValueItems();
		}

		// has MiscField?
		boolean hasMiscField = TargetType.hasMiscField(aTargetType);
		jComboBoxMiscField.setVisible(hasMiscField);
		jLabelMiscField.setVisible(hasMiscField);

		if (aTargetType == TargetType.SINGLE_NOMINAL ||
			aTargetType == TargetType.MULTI_BINARY_CLASSIFICATION)
			jLabelMiscField.setText(" target value");
		else
		{
			jLabelTargetInfo.setText(" # binary targets");

			//TODO: this is a hack
//			int aCount = 0;
//			for (Column c: itsTable.getColumns())
//				if (c.getAttribute().isBinaryType() && c.getIsEnabled())
//					aCount++;
//			jLFieldTargetInfo.setText(String.valueOf(aCount));
		}

		// has secondary targets (JList)?
		boolean hasSecondaryTargets = TargetType.hasSecondaryTargets(aTargetType);
		jLabelSecondaryTargets.setVisible(hasSecondaryTargets);
		jButtonSecondaryTargets.setVisible(hasSecondaryTargets);
		// disable if no binary attributes TODO should be itsTable.field
		boolean hasBinaryAttributes = "0".equalsIgnoreCase(jLFieldNrBinaries.getText().trim());
		jLabelSecondaryTargets.setEnabled(!hasBinaryAttributes);
		jButtonSecondaryTargets.setEnabled(!hasBinaryAttributes);

		// has target attribute?
		boolean hasTargetAttribute = TargetType.hasTargetAttribute(aTargetType);
		jLabelTargetAttribute.setVisible(hasTargetAttribute);
		jComboBoxTargetAttribute.setVisible(hasTargetAttribute);

		// has base model?
		jButtonBaseModel.setEnabled(TargetType.hasBaseModel(aTargetType));

		//update misc field? Other types are updated through action listeners
		if (aTargetType == TargetType.SINGLE_NUMERIC)
			initTargetInfo();
	}

	private void jComboBoxTargetTypeActionPerformed()
	{
		if (itsTable == null)
			return;

		itsTargetConcept.setTargetType(TargetType.getTargetType(getTargetTypeName()));
		itsSearchParameters.setTargetConcept(itsTargetConcept);

		initQualityMeasure();
		initTargetAttributeItems();
	}

	private void jComboBoxMiscFieldActionPerformed()
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

	private void jButtonSecondaryTargetsActionPerformed()
	{
		// is modal, blocks all input to other windows until closed
		new SecondaryTargetsWindow(jListSecondaryTargets);

		Object[] aSelection = jListSecondaryTargets.getSelectedValues();
		int aNrBinary = aSelection.length;
		ArrayList<Attribute> aList = new ArrayList<Attribute>(aNrBinary);

		// aSelection is in order and names are always present in itsColumns
		for (int i = 0, j = 0; i < aNrBinary; j++)
		{
			if (aSelection[i].equals(itsTable.getColumn(j).getName()))
			{
				aList.add(itsTable.getColumn(j).getAttribute());
				i++;
			}
		}
		itsTargetConcept.setMultiTargets(aList);
		//update GUI, relies on disabling "Select Targets" if nrBinaries == 0
		initTargetInfo();
	}

	// TODO remove duplicate ModelWindow code
	private void jButtonBaseModelActionPerformed()
	{
		try
		{
			setupSearchParameters();

			ModelWindow aWindow;
			switch (itsTargetConcept.getTargetType())
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
						aPrimaryTarget.getName(), aSecondaryTarget.getName(), anRM, null); //trendline, no subset
					aWindow.setLocation(50, 50);
					aWindow.setSize(GUI.WINDOW_DEFAULT_SIZE);
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
						aPrimaryTarget.getName(), aSecondaryTarget.getName(), null, null); //no trendline, no subset
					aWindow.setLocation(50, 50);
					aWindow.setSize(GUI.WINDOW_DEFAULT_SIZE);
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
	private void runSubgroupDiscovery(Table aTable, int theFold, BitSet theBitSet)
	{
		setupSearchParameters();
		TargetType aTargetType = itsTargetConcept.getTargetType();

		//TODO other types not implemented yet
		if (!TargetType.isImplemented(aTargetType))
			return;

		echoMiningStart();
		long aBegin = System.currentTimeMillis();

		SubgroupDiscovery aSubgroupDiscovery;
		String aTarget = getTargetAttributeName();

		switch(aTargetType)
		{
			case SINGLE_NOMINAL :
			{
				//recompute this number, as we may be dealing with cross-validation here, and hence a smaller number
				itsPositiveCount = aTable.countValues(aTable.getIndex(aTarget), getMiscFieldName());
				Log.logCommandLine("positive count: " + itsPositiveCount);
				aSubgroupDiscovery = new SubgroupDiscovery(itsSearchParameters, aTable, itsPositiveCount);
				break;
			}

			case SINGLE_NUMERIC:
			{
				//recompute this number, as we may be dealing with cross-validation here, and hence a different value
				itsTargetAverage = itsTable.getAverage(itsTable.getIndex(aTarget));
				Log.logCommandLine("average: " + itsTargetAverage);
				aSubgroupDiscovery = new SubgroupDiscovery(itsSearchParameters, aTable, itsTargetAverage);
				break;
			}
			case MULTI_LABEL :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(itsSearchParameters, aTable);
				break;
			}
			case DOUBLE_REGRESSION :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(itsSearchParameters, aTable, true);
				break;
			}
			case DOUBLE_CORRELATION :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(itsSearchParameters, aTable, false);
				break;
			}
			default : return; // TODO should never get here, throw warning
		}
		aSubgroupDiscovery.Mine(System.currentTimeMillis());

		long anEnd = System.currentTimeMillis();
		if (anEnd > aBegin + (long)(itsSearchParameters.getMaximumTime()*60*1000))
			JOptionPane.showMessageDialog(null, "Mining process ended prematurely due to time limit.",
											"Time Limit", JOptionPane.INFORMATION_MESSAGE);

		echoMiningEnd(anEnd - aBegin, aSubgroupDiscovery.getNumberOfSubgroups());

		//ResultWindow
//			SubgroupSet aPreliminaryResults = aSubgroupDiscovery.getResult();
		ResultWindow aResultWindow;
		switch (aTargetType)
		{
			case MULTI_LABEL :
			{
				BinaryTable aBinaryTable = new BinaryTable(aTable, itsTargetConcept.getMultiTargets());
//					aResultWindow = new ResultWindow(aPreliminaryResults, itsSearchParameters, null, itsTable, aBinaryTable, aSubgroupDiscovery.getQualityMeasure(), itsTotalCount, theFold, theBitSet);
				aResultWindow = new ResultWindow(itsTable, aSubgroupDiscovery, aBinaryTable, theFold, theBitSet);
				break;
			}
			default :
			{
//					aResultWindow = new ResultWindow(aPreliminaryResults, itsSearchParameters, null, aTable, aSubgroupDiscovery.getQualityMeasure(), itsTotalCount, theFold, theBitSet);
				aResultWindow = new ResultWindow( aTable, aSubgroupDiscovery, null, theFold, theBitSet);
			}
		}
		aResultWindow.setLocation(100, 100);
		aResultWindow.setSize(GUI.WINDOW_DEFAULT_SIZE);
		aResultWindow.setVisible(true);
	}

	private void jButtonSubgroupDiscoveryActionPerformed()
	{
		BitSet aBitSet = new BitSet(itsTable.getNrRows());
		aBitSet.set(0,itsTable.getNrRows());
		runSubgroupDiscovery(itsTable, 0, aBitSet);
	}

	private void jButtonRandomSubgroupsActionPerformed()
	{
		try
		{
			setupSearchParameters();

			String inputValue = JOptionPane.showInputDialog("Number of random subgroups to be used\nfor distribution estimation:", 1000);
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
					aQualityMeasure = new QualityMeasure(itsSearchParameters, aBayesian.getDAG(), itsTotalCount);
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
			NormalDistribution aDistro = new NormalDistribution(aValidation.randomSubgroups(aNrRepetitions));

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
					setQualityMeasureMinimum(Float.toString(aDistro.getOnePercentSignificance()));
					break;
				}
				case 1:
				{
					setQualityMeasureMinimum(Float.toString(aDistro.getFivePercentSignificance()));
					break;
				}
				case 2:
				{
					setQualityMeasureMinimum(Float.toString(aDistro.getTenPercentSignificance()));
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

	private void jButtonRandomConditionsActionPerformed()
	{
		try
		{
			setupSearchParameters();

			String inputValue = JOptionPane.showInputDialog("Number of random conditions to be used\nfor distribution estimation:", 1000);
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
					aQualityMeasure = new QualityMeasure(itsSearchParameters, aBayesian.getDAG(), itsTotalCount);
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
			NormalDistribution aDistro = new NormalDistribution(aValidation.randomConditions(aNrRepetitions));

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
					setQualityMeasureMinimum(Float.toString(aDistro.getOnePercentSignificance()));
					break;
				}
				case 1:
				{
					setQualityMeasureMinimum(Float.toString(aDistro.getFivePercentSignificance()));
					break;
				}
				case 2:
				{
					setQualityMeasureMinimum(Float.toString(aDistro.getTenPercentSignificance()));
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
		theSearchParameters.setBeta(1.0f);
	}

	// Obsolete, this info is already up to date through *ActionPerformed methods
	private void initTargetConcept()
	{
		Attribute aTarget = itsTable.getAttribute(getTargetAttributeName());
		itsTargetConcept.setPrimaryTarget(aTarget);

		// target value
		switch (itsTargetConcept.getTargetType())
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
		if (aTargetType == TargetType.MULTI_LABEL)
		{
			/*
			 *  TODO expensive recreation of up-to-date list for now
			 * reflects changes for columns if type/isEnabled is changed
			 * Needs changes to Table and MetaDataWindow for efficient code
			 * where changes are immediately pushed to underlying Table
			 * (which holds a secondaryTargets member).
			 */
			((DefaultListModel) jListSecondaryTargets.getModel()).clear();

			int aCount = 0;
			ArrayList<Attribute> aList = new ArrayList<Attribute>();
			for (Column c: itsTable.getColumns())
				if (c.getAttribute().isBinaryType() && c.getIsEnabled())
				{
					addSecondaryTargetsItem(c.getName());
					aList.add(c.getAttribute());
					aCount++;
				}
			itsTargetConcept.setMultiTargets(aList);
			jListSecondaryTargets.setSelectionInterval(0, jListSecondaryTargets.getModel().getSize() - 1);
			jLFieldTargetInfo.setText(String.valueOf(aCount));
		}
	}

	private void initTargetValueItems()
	{
		removeAllMiscFieldItems();
		// no attributes for selected target concept type?
		if (jComboBoxTargetAttribute.getItemCount() == 0)
			return;

		// single target attribute
		// if(!TargetConcept.isEMM(getTargetTypeName()))
		// {
		TreeSet<String> aValues =
			itsTable.getDomain(
				itsTable.getIndex(getTargetAttributeName()));
		for (String aValue : aValues)
			addMiscFieldItem(aValue);
		// }
	}

	private void initTargetInfo()
	{

		switch (itsTargetConcept.getTargetType())
		{
			case SINGLE_NOMINAL :
			{
				itsPositiveCount =
					itsTable.countValues(itsTable.getIndex(getTargetAttributeName()), getMiscFieldName());
				float aPercentage = ((float) itsPositiveCount * 100.0f) / (float) itsTotalCount;
				NumberFormat aFormatter = NumberFormat.getNumberInstance();
				aFormatter.setMaximumFractionDigits(1);
				jLFieldTargetInfo.setText(itsPositiveCount + " (" + aFormatter.format(aPercentage) + " %)");
				jLabelTargetInfo.setText(" # positive");
				break;
			}
			case SINGLE_NUMERIC :
			{
				String aTarget = getTargetAttributeName();
				if (aTarget != null) //initTargetInfo might be called before item is actually selected
				{
					itsTargetAverage = itsTable.getAverage(itsTable.getIndex(aTarget));
					jLabelTargetInfo.setText(" average");
					jLFieldTargetInfo.setText(String.valueOf(itsTargetAverage));
				}
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
				jLFieldTargetInfo.setText(String.valueOf(itsTargetConcept.getMultiTargets().size()));
				jLabelTargetInfo.setText(" # binary targets");
				break;
			}
			case MULTI_BINARY_CLASSIFICATION :
			{
				jLabelTargetInfo.setText(" target info");
				jLFieldTargetInfo.setText("none");
				break;
			}
		}
	}

	private void initQualityMeasure()
	{
		removeAllQualityMeasureItems();
		TargetType aTargetType = itsTargetConcept.getTargetType();

		for (int i = QualityMeasure.getFirstEvaluationMeasure(aTargetType); i <= QualityMeasure.getLastEvaluationMesure(aTargetType); i++)
			addQualityMeasureItem(QualityMeasure.getMeasureString(i));
		initEvaluationMinimum();
	}

	private void initEvaluationMinimum()
	{
		if (getQualityMeasureName() != null)
			setQualityMeasureMinimum(QualityMeasure.getMeasureMinimum(getQualityMeasureName(), itsTargetAverage));
	}

	/* FIELD METHODS OF CORTANA COMPONENTS */

	// target type - target type
	private String getTargetTypeName() { return (String) jComboBoxTargetType.getSelectedItem(); }
	private void setTargetTypeName(String aName) { jComboBoxTargetType.setSelectedItem(aName); }

	// target type - quality measure
	private String getQualityMeasureName() { return (String) jComboBoxQualityMeasure.getSelectedItem(); }
	private void setQualityMeasure(String aName) { jComboBoxQualityMeasure.setSelectedItem(aName); }
	private void addQualityMeasureItem(String anItem) { jComboBoxQualityMeasure.addItem(anItem); }
	private void removeAllQualityMeasureItems() { jComboBoxQualityMeasure.removeAllItems(); }

	// target type - quality measure minimum (member of itsSearchParameters)
	private float getQualityMeasureMinimum() { return getValue(0.0f, jTextFieldQualityMeasureMinimum.getText()); }
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

	// target type - jList secondary targets
	private void addSecondaryTargetsItem(String theItem) { ((DefaultListModel) jListSecondaryTargets.getModel()).addElement(theItem); }
	private void removeAllSecondaryTargetsItems() { ((DefaultListModel) jListSecondaryTargets.getModel()).clear(); }



	private void setSearchDepthMaximum(String aValue) { jTextFieldSearchDepth.setText(aValue); }
	private int getSearchDepthMaximum() { return getValue(1, jTextFieldSearchDepth.getText()); }
	private void setSearchCoverageMinimum(String aValue) { jTextFieldSearchCoverageMinimum.setText(aValue); }
	private void setSearchCoverageMaximum(String aValue) { jTextFieldSearchCoverageMaximum.setText(aValue); }
	private void setSubgroupsMaximum(String aValue) { jTextFieldSubgroupsMaximum.setText(aValue); }
	private void setSearchTimeMaximum(String aValue) { jTextFieldSearchTimeMaximum.setText(aValue); }
	private int getSearchCoverageMinimum() { return getValue(0, jTextFieldSearchCoverageMinimum.getText()); }
	private float getSearchCoverageMaximum() { return getValue(1.0f, jTextFieldSearchCoverageMaximum.getText()); }
	private int getSubgroupsMaximum() { return getValue(50, jTextFieldSubgroupsMaximum.getText());}
	private float getSearchTimeMaximum() { return getValue(1.0f, jTextFieldSearchTimeMaximum.getText()); }

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
		try { aValue = Integer.parseInt(theText); }
		catch (Exception ex) {}	// TODO warning dialog
		return aValue;
	}

	private float getValue(float theDefaultValue, String theText)
	{
		float aValue = theDefaultValue;
		try { aValue = Float.parseFloat(theText); }
		catch (Exception ex) {}	// TODO warning dialog
		return aValue;
	}

	private JMenuBar jMiningWindowMenuBar;
	private JMenu jMenuFile;
	private JMenuItem jMenuItemOpenFile;
	private JMenuItem jMenuItemBrowse;
	private JMenuItem jMenuItemMetaData;
//	private JMenuItem jMenuItemDataExplorer;
	private JMenuItem jMenuItemSubgroupDiscovery;
	private JMenuItem jMenuItemCreateAutoRunFile;
	private JMenuItem jMenuItemAddToAutoRunFile;
	private JMenuItem jMenuItemExit;
	private JMenu jMenuEnrichment;
	private JMenuItem jMenuItemAddCuiEnrichmentSource;
	private JMenuItem jMenuItemAddGoEnrichmentSource;
	private JMenuItem jMenuItemAddCustomEnrichmentSource;
	private JMenuItem jMenuItemRemoveEnrichmentSource;
	private JMenu jMenuAbout;
	private JMenuItem jMenuItemAboutCortana;
	private JMenu jMenuGui;	// TODO for testing only
	private JPanel jPanelSouth;
	private JPanel jPanelMineButtons;
	private JButton jButtonBrowse;
	private JButton jButtonMetaData;
	private JButton jButtonSwapRandomize;
	private JButton jButtonCrossValidate;
//	private JButton jButtonDataExplorer;
	private JButton jButtonSubgroupDiscovery;
	private JButton jButtonRandomSubgroups;
	private JButton jButtonRandomConditions;
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
	private JPanel jPanelRuleTargetFieldsEnabled;
	private JLabel jLFieldTargetTable;
	private JComboBox jComboBoxTargetAttribute;
	private JComboBox jComboBoxMiscField;
	private JButton jButtonSecondaryTargets;
	private JList jListSecondaryTargets;
	private JLabel jLFieldNrExamples;
	private JLabel jLFieldNrColumns;
	private JLabel jLFieldNrColumnsEnabled;
	private JLabel jLFieldNrNominals;
	private JLabel jLFieldNrNominalsEnabled;
	private JLabel jLFieldNrNumerics;
	private JLabel jLFieldNrNumericsEnabled;
	private JLabel jLFieldNrBinaries;
	private JLabel jLFieldNrBinariesEnabled;
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
	private static JButton initButton(String theName, int theMnemonic)
	{
		JButton aButton = new JButton();
		aButton.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);
		aButton.setBorder(new BevelBorder(0));
		aButton.setMinimumSize(GUI.BUTTON_MINIMUM_SIZE);
		aButton.setMaximumSize(GUI.BUTTON_MAXIMUM_SIZE);
		aButton.setFont(GUI.DEFAULT_BUTTON_FONT);
		aButton.setText(theName);
		aButton.setMnemonic(theMnemonic);
		return aButton;
	}

	// TODO include accelerator
	private enum Mnemonic
	{
		FILE('F'),	// (no accelerator)
		OPEN_FILE('O'),
		OPEN_GENE_RANK('G'),
		BROWSE('B'),
		META_DATA('M'),
		DATA_EXPLORER('D'),
		SUBGROUP_DISCOVERY('S'),
		CREATE_AUTORUN_FILE('C'),
		ADD_TO_AUTORUN_FILE('A'),
		EXIT('X'),
		ABOUT('A'),	// 2x (no accelerator)
		ABOUT_CORTANA('I'),
		SECONDARY_TARGETS('T'),
		BASE_MODEL('B'),	// 2x (no accelerator)
		RANDOM_SUBGROUPS('R'),
		RANDOM_CONDITIONS('C');	// 2x (no accelerator)

		final int mnemonic;
		private Mnemonic(int theMnemonic) { mnemonic = theMnemonic; }
	}

	private static JLabel initJLabel(String theName)
	{
		JLabel aJLable = new JLabel(theName);
		aJLable.setFont(GUI.DEFAULT_TEXT_FONT);
		return aJLable;
	}

	// TODO for testing only
	private void initJMenuGui()
	{
		JRadioButtonMenuItem aLookAndFeel;
		ButtonGroup itsLookAndFeels = new ButtonGroup();
		String aCurrent = UIManager.getLookAndFeel().getName();

		for (final LookAndFeelInfo l : UIManager.getInstalledLookAndFeels())
		{
			String aName = l.getName();
			aLookAndFeel = new JRadioButtonMenuItem(aName);
			aLookAndFeel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					jMenuGuiActionPerformed(l.getClassName());
				}
			});
			jMenuGui.add(aLookAndFeel);
			itsLookAndFeels.add(aLookAndFeel);
			if (aName.equals(aCurrent))
				aLookAndFeel.setSelected(true);
		}
	}

	private void jMenuGuiActionPerformed(String theLookAndFeelClassName)
	{
		try
		{
			UIManager.setLookAndFeel(theLookAndFeelClassName);
			SwingUtilities.updateComponentTreeUI(this);
		}
		catch (ClassNotFoundException e) { e.printStackTrace(); }
		catch (InstantiationException e) { e.printStackTrace(); }
		catch (IllegalAccessException e) { e.printStackTrace(); }
		catch (UnsupportedLookAndFeelException e) { e.printStackTrace(); }
	}
	// TODO end test

}
