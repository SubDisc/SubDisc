package nl.liacs.subdisc.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.UIManager.*;
import javax.swing.border.*;

import nl.liacs.subdisc.*;
import nl.liacs.subdisc.FileHandler.Action;
import nl.liacs.subdisc.XMLAutoRun.*;
import nl.liacs.subdisc.cui.*;

/*
 * TODO correlation info field is not updated when changing primary target
 */
public class MiningWindow extends JFrame
{
	static final long serialVersionUID = 1L;

	public static final Image ICON = new ImageIcon(Toolkit.getDefaultToolkit().getImage(MiningWindow.class.getResource("/icon.jpg"))).getImage();

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
		setIconImage(ICON);
		setLocation(100, 100);
		setSize(700, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initJMenuGui(); // XXX for GUI testing only
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
				jComboBoxTargetType.addItem(t.GUI_TEXT);
//		jComboBoxTargetType.setEnabled(hasTable);

		// Add all SearchStrategies
		for (SearchStrategy s : SearchStrategy.values())
			jComboBoxSearchStrategyType.addItem(s.GUI_TEXT);
//		jComboBoxSearchStrategyType.setEnabled(hasTable);

		// Add all Numeric Operators choices
		for (NumericOperators n : NumericOperators.values())
			jComboBoxNumericOperators.addItem(n.GUI_TEXT);

		// Add all Numeric Strategies
		for (NumericStrategy n : NumericStrategy.values())
			jComboBoxSearchStrategyNumeric.addItem(n.GUI_TEXT);
//		jComboBoxSearchStrategyNumeric.setEnabled(hasTable);
	}

	// only called when Table is present, so using itsTotalCount is safe
	private void initGuiComponents()
	{
		//dataset
		initGuiComponentsDataSet();

		// target concept
		enableBaseModelButtonCheck();

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
		setSearchStrategyType(itsSearchParameters.getSearchStrategy().GUI_TEXT);
		setSearchStrategyWidth(String.valueOf(itsSearchParameters.getSearchStrategyWidth()));
		setNumericStrategy(itsSearchParameters.getNumericStrategy().GUI_TEXT);
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

		setTargetTypeName(itsTargetConcept.getTargetType().GUI_TEXT);
		setQualityMeasure(itsSearchParameters.getQualityMeasureString());
		// reset original value
		itsSearchParameters.setQualityMeasureMinimum(originalMinimum);
		setQualityMeasureMinimum(String.valueOf(itsSearchParameters.getQualityMeasureMinimum()));
		if (TargetType.hasTargetValue(itsTargetConcept.getTargetType()))
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
		jMenuItemExplore = new JMenuItem();
		jMenuItemMetaData = new JMenuItem();
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
		jButtonExplore = new JButton();
		jButtonMetaData = new JButton();
		jButtonCrossValidate = new JButton();

		// target concept
		jPanelRuleEvaluation = new JPanel();
		// target concept - labels
		jPanelEvaluationLabels = new JPanel();
		jLabelTargetType = new JLabel();
		jLabelQualityMeasure = new JLabel();
		jLabelEvaluationThreshold = new JLabel();
		jLabelTargetAttribute = new JLabel();
		jLabelMiscField = new JLabel(); // used for target value or secondary target
		jLabelMultiRegressionTargets = new JLabel();
		jLabelMultiTargets = new JLabel();
		jLabelTargetInfo = new JLabel();
		// target concept - fields
		jPanelEvaluationFields = new JPanel();
		jComboBoxTargetType = new JComboBox();
		jComboBoxQualityMeasure = new JComboBox();
		jTextFieldQualityMeasureMinimum = new JTextField();
		jComboBoxTargetAttribute = new JComboBox();
		jComboBoxMiscField = new JComboBox(); // used for target value or secondary target
		jButtonMultiRegressionTargets = new JButton();
		jButtonMultiTargets = new JButton();	// shows jListMultiTargets
		jListMultiRegressionTargets = new JList(new DefaultListModel());
		jListMultiTargets = new JList(new DefaultListModel());	// permanently maintained
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
		jLabelNotEquals = new JLabel();
		jLabelNumericOperators = new JLabel();
		jLabelSearchStrategyNumeric = new JLabel();
		jLabelSearchStrategyNrBins = new JLabel();
		// search strategy - fields
		jPanelSearchStrategyFields = new JPanel();
		jComboBoxSearchStrategyType = new JComboBox();
		jTextFieldSearchStrategyWidth = new JTextField();
		jCheckBoxNotEquals = new javax.swing.JCheckBox();
		jComboBoxNumericOperators = new JComboBox();
		jComboBoxSearchStrategyNumeric = new JComboBox();
		jTextFieldSearchStrategyNrBins = new JTextField();
		jTextFieldNrThreads = new JTextField();

		// mining buttons
		jPanelMineButtons = new JPanel();
		jButtonSubgroupDiscovery = new JButton();
		jButtonThreshold = new JButton();

		// setting up - menu items
		jMiningWindowMenuBar.setFont(GUI.DEFAULT_TEXT_FONT);

		jMenuFile.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuFile.setText("File");
		jMenuFile.setMnemonic('F');

		jMenuItemOpenFile.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemOpenFile.setText("Open File");
		jMenuItemOpenFile.setMnemonic(Mnemonic.OPEN_FILE.MNEMONIC);
		jMenuItemOpenFile.setAccelerator(KeyStroke.getKeyStroke(Mnemonic.OPEN_FILE.MNEMONIC, InputEvent.CTRL_MASK));
		jMenuItemOpenFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemOpenFileActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemOpenFile);

		jMenuItemBrowse.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemBrowse.setText("Browse...");
		jMenuItemBrowse.setMnemonic(Mnemonic.BROWSE.MNEMONIC);
		jMenuItemBrowse.setAccelerator(KeyStroke.getKeyStroke(Mnemonic.BROWSE.MNEMONIC, InputEvent.CTRL_MASK));
		jMenuItemBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				browseActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemBrowse);

		jMenuItemExplore.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemExplore.setText("Explore...");
		jMenuItemExplore.setMnemonic(Mnemonic.EXPLORE.MNEMONIC);
		jMenuItemExplore.setAccelerator(KeyStroke.getKeyStroke(Mnemonic.EXPLORE.MNEMONIC, InputEvent.CTRL_MASK));
		jMenuItemExplore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				exploreActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemExplore);

		jMenuItemMetaData.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemMetaData.setText("Meta Data...");
		jMenuItemMetaData.setMnemonic(Mnemonic.META_DATA.MNEMONIC);
		jMenuItemMetaData.setAccelerator(KeyStroke.getKeyStroke(Mnemonic.META_DATA.MNEMONIC, InputEvent.CTRL_MASK));
		jMenuItemMetaData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				metaDataActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemMetaData);

		jMenuFile.addSeparator();

		jMenuItemSubgroupDiscovery.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemSubgroupDiscovery.setText("Subgroup Discovery");
		jMenuItemSubgroupDiscovery.setMnemonic(Mnemonic.SUBGROUP_DISCOVERY.MNEMONIC);
		jMenuItemSubgroupDiscovery.setAccelerator(KeyStroke.getKeyStroke(Mnemonic.SUBGROUP_DISCOVERY.MNEMONIC, InputEvent.CTRL_MASK));
		jMenuItemSubgroupDiscovery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonSubgroupDiscoveryActionPerformed();
			}
		});
		jMenuFile.add(jMenuItemSubgroupDiscovery);

		jMenuFile.addSeparator();

		jMenuItemCreateAutoRunFile.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemCreateAutoRunFile.setText("Create Autorun File");
		jMenuItemCreateAutoRunFile.setMnemonic(Mnemonic.CREATE_AUTORUN_FILE.MNEMONIC);
		jMenuItemCreateAutoRunFile.setAccelerator(KeyStroke.getKeyStroke(Mnemonic.CREATE_AUTORUN_FILE.MNEMONIC, InputEvent.CTRL_MASK));
		jMenuItemCreateAutoRunFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAutoRunFileActionPerformed(AutoRun.CREATE);
			}
		});
		jMenuFile.add(jMenuItemCreateAutoRunFile);

		jMenuItemAddToAutoRunFile.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemAddToAutoRunFile.setText("Add to Autorun File");
		jMenuItemAddToAutoRunFile.setMnemonic(Mnemonic.ADD_TO_AUTORUN_FILE.MNEMONIC);
		jMenuItemAddToAutoRunFile.setAccelerator(KeyStroke.getKeyStroke(Mnemonic.ADD_TO_AUTORUN_FILE.MNEMONIC, InputEvent.CTRL_MASK));
		jMenuItemAddToAutoRunFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAutoRunFileActionPerformed(AutoRun.ADD);
			}
		});
		jMenuFile.add(jMenuItemAddToAutoRunFile);

		jMenuFile.addSeparator();

		jMenuItemExit.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemExit.setText("Exit");
		jMenuItemExit.setMnemonic(Mnemonic.EXIT.MNEMONIC);
		jMenuItemExit.setAccelerator(KeyStroke.getKeyStroke(Mnemonic.EXIT.MNEMONIC, InputEvent.CTRL_MASK));
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
/*
		// TODO add when implemented
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

		// TODO add when implemented
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
*/
		jMenuItemRemoveEnrichmentSource.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemRemoveEnrichmentSource.setText("Remove Enrichment Source");
		jMenuItemRemoveEnrichmentSource.setMnemonic('R');
//		jMenuItemRemoveEnrichmentSource.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
		jMenuItemRemoveEnrichmentSource.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemRemoveEnrichmentSourceActionPerformed();
			}
		});
		jMenuItemRemoveEnrichmentSource.setEnabled(false);
		jMenuEnrichment.add(jMenuItemRemoveEnrichmentSource);

		jMiningWindowMenuBar.add(jMenuEnrichment);

		jMenuAbout.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuAbout.setText("About");
		jMenuAbout.setMnemonic('A');

		jMenuItemAboutCortana.setFont(GUI.DEFAULT_TEXT_FONT);
		jMenuItemAboutCortana.setText("Cortana");
		jMenuItemAboutCortana.setMnemonic(Mnemonic.ABOUT_CORTANA.MNEMONIC);
		jMenuItemAboutCortana.setAccelerator(KeyStroke.getKeyStroke(Mnemonic.ABOUT_CORTANA.MNEMONIC, InputEvent.CTRL_MASK));
		jMenuItemAboutCortana.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jMenuItemAboutCortanaActionPerformed();
			}
		});
		jMenuAbout.add(jMenuItemAboutCortana);

		jMiningWindowMenuBar.add(jMenuAbout);
/*
		// XXX uncomment for GUI testing only
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

		jLabelTargetTable = initJLabel("target table");
		jPanelRuleTargetLabels.add(jLabelTargetTable);

		jLabelNrExamples = initJLabel("# examples");
		jPanelRuleTargetLabels.add(jLabelNrExamples);

		jLabelNrColumns = initJLabel("# columns");
		jPanelRuleTargetLabels.add(jLabelNrColumns);

		jLabelNrNominals = initJLabel("# nominals");
		jPanelRuleTargetLabels.add(jLabelNrNominals);

		jLabelNrNumerics = initJLabel("# numerics");
		jPanelRuleTargetLabels.add(jLabelNrNumerics);

		jLabelNrBinaries = initJLabel("# binaries");
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

		//jPanelRuleTargetButtons.setLayout(new BoxLayout(jPanelRuleTargetButtons , BoxLayout.X_AXIS));
		final JPanel aButtonPanel = new JPanel();
		aButtonPanel.setLayout(new GridLayout(2, 2));

		jButtonBrowse = initButton("Browse...", Mnemonic.BROWSE);
		jButtonBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				browseActionPerformed();
			}
		});
		//jPanelRuleTargetButtons.add(jButtonBrowse);
		aButtonPanel.add(jButtonBrowse);

		jButtonExplore = initButton("Explore...", Mnemonic.EXPLORE);
		jButtonExplore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				exploreActionPerformed();
			}
		});
		//jPanelRuleTargetButtons.add(jButtonExplore);
		aButtonPanel.add(jButtonExplore);

		jButtonMetaData = initButton("Meta Data...", Mnemonic.META_DATA);
		jButtonMetaData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				metaDataActionPerformed();
			}
		});
		//jPanelRuleTargetButtons.add(jButtonMetaData);
		aButtonPanel.add(jButtonMetaData);

		jPanelRuleTargetButtons.add(aButtonPanel);

		jPanelRuleTarget.add(jPanelRuleTargetButtons, BorderLayout.SOUTH);
		jPanelCenter.add(jPanelRuleTarget);	// MM

		// setting up - target concept - labels ================================
		jPanelRuleEvaluation.setLayout(new BoxLayout(jPanelRuleEvaluation, 0));
		jPanelRuleEvaluation.setBorder(new TitledBorder(new EtchedBorder(),
				"Target Concept", 4, 2, new Font("Dialog", 1, 11)));
		jPanelRuleEvaluation.setFont(new Font("Dialog", 1, 12));

		jPanelEvaluationLabels.setLayout(new GridLayout(9, 1));

		jComboBoxTargetType.setPreferredSize(new Dimension(86, 22));
		jComboBoxTargetType.setMinimumSize(new Dimension(86, 22));
		jComboBoxTargetType.setFont(GUI.DEFAULT_TEXT_FONT);
		jComboBoxTargetType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxTargetTypeActionPerformed();
			}
		});
		jPanelEvaluationFields.add(jComboBoxTargetType);

		jLabelTargetType = initJLabel("target type");
		jPanelEvaluationLabels.add(jLabelTargetType);

		jLabelQualityMeasure = initJLabel("quality measure");
		jPanelEvaluationLabels.add(jLabelQualityMeasure);

		jLabelEvaluationThreshold = initJLabel("measure minimum");
		jPanelEvaluationLabels.add(jLabelEvaluationThreshold);

		jLabelTargetAttribute = initJLabel("primary target");
		jPanelEvaluationLabels.add(jLabelTargetAttribute);

		jLabelMiscField = initJLabel("");
		jPanelEvaluationLabels.add(jLabelMiscField);

		jLabelMultiRegressionTargets = initJLabel("secondary/tertiary targets");
		jPanelEvaluationLabels.add(jLabelMultiRegressionTargets);

		// TODO for stable jar, disable
		jLabelMultiRegressionTargets.setVisible(false);

		jLabelMultiTargets = initJLabel("targets and settings");
		jPanelEvaluationLabels.add(jLabelMultiTargets);

		jLabelTargetInfo = initJLabel("");;
		jPanelEvaluationLabels.add(jLabelTargetInfo);
		jPanelRuleEvaluation.add(jPanelEvaluationLabels);

		// setting up - target concept - fields ================================
		jPanelEvaluationFields.setLayout(new GridLayout(9, 1));

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

		// note in cui setting this is often to small
		jComboBoxMiscField.setPreferredSize(new Dimension(86, 22));
		jComboBoxMiscField.setMinimumSize(new Dimension(86, 22));
		jComboBoxMiscField.setFont(GUI.DEFAULT_TEXT_FONT);
		jComboBoxMiscField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxMiscFieldActionPerformed();
			}
		});
		jPanelEvaluationFields.add(jComboBoxMiscField);

		jButtonMultiRegressionTargets = initButton("Secondary/Tertiary Targets", Mnemonic.SECONDARY_TERTIARY_TARGETS);
		jButtonMultiRegressionTargets.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonMultiRegressionTargetsActionPerformed();
			}
		});
		jPanelEvaluationFields.add(jButtonMultiRegressionTargets);

		// TODO for stable jar, disable
		jButtonMultiRegressionTargets.setVisible(false);

		jButtonMultiTargets = initButton("Targets and Settings", Mnemonic.TARGETS_AND_SETTINGS);
		jButtonMultiTargets.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonMultiTargetsActionPerformed();
			}
		});
		jPanelEvaluationFields.add(jButtonMultiTargets);

		jLFieldTargetInfo.setForeground(Color.black);
		jLFieldTargetInfo.setFont(GUI.DEFAULT_TEXT_FONT);
		jPanelEvaluationFields.add(jLFieldTargetInfo);

		jButtonBaseModel = initButton("Base Model", Mnemonic.BASE_MODEL);
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

		jLabelSearchDepth = initJLabel("refinement depth");
		jPanelSearchParameterLabels.add(jLabelSearchDepth);

		jLabelSearchCoverageMinimum = initJLabel("minimum coverage");
		jPanelSearchParameterLabels.add(jLabelSearchCoverageMinimum);

		jLabelSearchCoverageMaximum = initJLabel("maximum coverage (fraction)");
		jPanelSearchParameterLabels.add(jLabelSearchCoverageMaximum);

		jLabelSubgroupsMaximum = initJLabel("<html> maximum subgroups (0 = &#8734;)</html>)");
		jPanelSearchParameterLabels.add(jLabelSubgroupsMaximum);

		jLabelSearchTimeMaximum = initJLabel("<html> maximum time (min) (0 = &#8734;)</html>)");
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

		jLabelStrategyType = initJLabel("strategy type");
		jPanelSearchStrategyLabels.add(jLabelStrategyType);

		jLabelStrategyWidth = initJLabel("search width");
		jPanelSearchStrategyLabels.add(jLabelStrategyWidth);

		jLabelNotEquals = initJLabel("<html>include &#8800; (nominal)</html>");
		jPanelSearchStrategyLabels.add(jLabelNotEquals);

		jLabelNumericOperators = initJLabel("numeric operators");
		jPanelSearchStrategyLabels.add(jLabelNumericOperators);

		jLabelSearchStrategyNumeric = initJLabel("numeric strategy");
		jPanelSearchStrategyLabels.add(jLabelSearchStrategyNumeric);

		jLabelSearchStrategyNrBins = initJLabel("number of bins");
		jPanelSearchStrategyLabels.add(jLabelSearchStrategyNrBins);

		jLabelNrThreads = initJLabel("threads (0 = all available)");
		jPanelSearchStrategyLabels.add(jLabelNrThreads);

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

		jCheckBoxNotEquals.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
		jCheckBoxNotEquals.setForeground(new java.awt.Color (102, 102, 153));
		jCheckBoxNotEquals.setFont(GUI.DEFAULT_TEXT_FONT);
		jPanelSearchStrategyFields.add(jCheckBoxNotEquals);

		jComboBoxNumericOperators.setPreferredSize(new Dimension(86, 22));
		jComboBoxNumericOperators.setMinimumSize(new Dimension(86, 22));
		jComboBoxNumericOperators.setFont(GUI.DEFAULT_TEXT_FONT);
		jComboBoxNumericOperators.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jComboBoxNumericOperatorsActionPerformed();
			}
		});
		jPanelSearchStrategyFields.add(jComboBoxNumericOperators);

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

		jTextFieldNrThreads.setPreferredSize(new Dimension(86, 22));
		jTextFieldNrThreads.setFont(GUI.DEFAULT_TEXT_FONT);
		jTextFieldNrThreads.setText(String.valueOf(Runtime.getRuntime().availableProcessors()));
		jTextFieldNrThreads.setHorizontalAlignment(SwingConstants.RIGHT);
		jTextFieldNrThreads.setMinimumSize(new Dimension(86, 22));
		jPanelSearchStrategyFields.add(jTextFieldNrThreads);

		jPanelSearchStrategy.add(jPanelSearchStrategyFields);
		jPanelCenter.add(jPanelSearchStrategy);	// MM

		// setting up - mining buttons =========================================
		jPanelSouth.setFont(GUI.DEFAULT_TEXT_FONT);

		jPanelMineButtons.setMinimumSize(new Dimension(0, 40));

		jButtonSubgroupDiscovery = initButton("Subgroup Discovery", Mnemonic.SUBGROUP_DISCOVERY);
		jButtonSubgroupDiscovery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonSubgroupDiscoveryActionPerformed();
			}
		});
		jPanelMineButtons.add(jButtonSubgroupDiscovery);

		jButtonCrossValidate = initButton("Cross-Validate", Mnemonic.CROSS_VALIDATE);
		jButtonCrossValidate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonCrossValidateActionPerformed();
			}
		});
		jPanelMineButtons.add(jButtonCrossValidate);

		jButtonThreshold = initButton("Compute Threshold", Mnemonic.COMPUTE_THRESHOLD);
		jButtonThreshold.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButtonRandomQualitiesActionPerformed();
			}
		});
		jPanelMineButtons.add(jButtonThreshold);

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
						jMenuItemExplore,
						jMenuItemMetaData,
						jMenuItemSubgroupDiscovery,
						jMenuItemCreateAutoRunFile,
						jMenuItemAddToAutoRunFile,
						jMenuItemAddCuiEnrichmentSource,
						jMenuItemAddGoEnrichmentSource,
						jMenuItemAddCustomEnrichmentSource,
						jButtonBrowse,
						jButtonExplore,
						jButtonMetaData,
						jButtonCrossValidate,
						jButtonSubgroupDiscovery,
						jButtonThreshold,
						jButtonMultiTargets,
						jButtonMultiRegressionTargets};
		enableBaseModelButtonCheck();

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
			removeAllMultiTargetsItems(); // hack for now

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

			jMenuItemRemoveEnrichmentSource.setEnabled(false);
			jComboBoxTargetTypeActionPerformed();	// update hack
		}
	}

	private void jMenuItemAutoRunFileActionPerformed(final AutoRun theFileOption)
	{
		setupSearchParameters();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new XMLAutoRun(itsSearchParameters, itsTable, theFileOption);
			}
		});
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
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new FileHandler(itsTable, theType);
				update();
				jMenuItemRemoveEnrichmentSource
					.setEnabled(itsTable.getDomainList() != null &&
								itsTable.getDomainList().getComponentCount() > 0);
			}
		});
	}

	private void jMenuItemRemoveEnrichmentSourceActionPerformed()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				JList aDomainList = itsTable.getDomainList();
				new RemoveDomainWindow(aDomainList);
//				int aComponentCount = aDomainList.getModel().getSize();
				int aComponentCount = aDomainList.getComponentCount();

				if ((aDomainList != null) && (aComponentCount > 0))
				{
					for (int i = 0, j = aComponentCount; i < j; ++i)
						if (aDomainList.isSelectedIndex(i))
							itsTable.removeDomain(i);

					update();
					jMenuItemRemoveEnrichmentSource
						.setEnabled(aDomainList == null || aComponentCount == 0);
				}
			}
		});
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
	//not on Event Dispatching Thread, may take a long time to load
	private void browseActionPerformed()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new BrowseWindow(itsTable, null);
			}
		});
	}

	//not on Event Dispatching Thread, may take a long time to load
	private void exploreActionPerformed()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new HistogramWindow(itsTable);
			}
		});
	}

	//not on Event Dispatching Thread, may take a long time to load
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

	private void jComboBoxSearchStrategyTypeActionPerformed()
	{
		String aName = getSearchStrategyName();
		if (aName != null)
		{
			itsSearchParameters.setSearchStrategy(aName);
			jTextFieldSearchStrategyWidth.setEnabled(!SearchStrategy.BEST_FIRST.GUI_TEXT.equalsIgnoreCase(aName));
		}
	}

	private void jComboBoxNumericOperatorsActionPerformed()
	{
		String aName = getNumericOperators();
		if (aName != null)
			itsSearchParameters.setNumericOperators(aName);
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
		initEvaluationMinimum();
		itsSearchParameters.setQualityMeasureMinimum(getQualityMeasureMinimum());

		// this ALWAYS resets alpha if switching TO EDIT_DISTANCE
		// remove upon discretion
		if (QualityMeasure.getMeasureString(QualityMeasure.EDIT_DISTANCE).equals(getQualityMeasureName()))
			itsSearchParameters.setAlpha(SearchParameters.ALPHA_EDIT_DISTANCE);
	}

	private void jComboBoxTargetTypeActionPerformed()
	{
		if (itsTable == null)
			return;

		itsTargetConcept.setTargetType(getTargetTypeName());
		itsSearchParameters.setTargetConcept(itsTargetConcept);

		initQualityMeasure();
		initTargetAttributeItems();

		TargetType aTargetType = itsTargetConcept.getTargetType();
		// has MiscField?
		boolean hasMiscField = TargetType.hasMiscField(aTargetType);
		jComboBoxMiscField.setVisible(hasMiscField);
		jLabelMiscField.setVisible(hasMiscField);

		switch (aTargetType)
		{
			case SINGLE_NOMINAL :
			{
				jLabelTargetInfo.setText(" # positive");
				break;
			}
			case SINGLE_ORDINAL :
			case SINGLE_NUMERIC :
			{
				jLabelTargetInfo.setText(" average");
				break;
			}
			case DOUBLE_REGRESSION :
			case DOUBLE_CORRELATION :
			{
				jLabelTargetInfo.setText(" correlation");
				break;
			}
			case MULTI_LABEL :
			{
				jLabelTargetInfo.setText(" # binary targets");
				break;
			}
			case MULTI_BINARY_CLASSIFICATION :
			{
				jLabelTargetInfo.setText(" target info");
				break;
			}
		}

// TODO for stable jar, disable, was added in revision 848
/*
		boolean hasMultiRegressionTargets = TargetType.hasMultiRegressionTargets(aTargetType);
		jLabelMultiRegressionTargets.setVisible(hasMultiRegressionTargets);
		jButtonMultiRegressionTargets.setVisible(hasMultiRegressionTargets);
		// disable if not enough numeric attributes TODO should be itsTable.field
		// jListMultiRegressionTargets is populated through initTargetAttributeItems above
		jButtonMultiRegressionTargets.setEnabled(jListMultiRegressionTargets.getSelectedIndices().length > 1);
*/

		// has secondary targets (JList)?
		boolean hasMultiTargets = TargetType.hasMultiTargets(aTargetType);
		jLabelMultiTargets.setVisible(hasMultiTargets);
		jButtonMultiTargets.setVisible(hasMultiTargets);
		// disable if no binary attributes TODO should be itsTable.field
		// jListMultiTargets is populated through initTargetAttributeItems above
		jButtonMultiTargets.setEnabled(jListMultiTargets.getSelectedIndices().length != 0);

		// has base model?
		enableBaseModelButtonCheck();

		// has target attribute?
		boolean hasTargetAttribute = TargetType.hasTargetAttribute(aTargetType);
		jLabelTargetAttribute.setVisible(hasTargetAttribute);
		jComboBoxTargetAttribute.setVisible(hasTargetAttribute);
	}

	private void jComboBoxTargetAttributeActionPerformed()
	{
		//itsTargetConcept.setPrimaryTarget(itsTable.getAttribute(getTargetAttributeName()));
		itsTargetConcept.setPrimaryTarget(itsTable.getColumn(getTargetAttributeName()));
		itsSearchParameters.setTargetConcept(itsTargetConcept);

		TargetType aTargetType = itsTargetConcept.getTargetType();

		if (getTargetAttributeName() != null &&
			(aTargetType == TargetType.SINGLE_NOMINAL ||
				aTargetType == TargetType.MULTI_BINARY_CLASSIFICATION))
		{
			initTargetValueItems();
		}

		//itsTable.getAttribute(getTargetAttributeName()).makePrimaryTarget();

		if (getTargetAttributeName() != null && aTargetType == TargetType.DOUBLE_REGRESSION)
			computeMultiRegressionTargets();

		//update misc field? Other types are updated through action listeners
		if (aTargetType == TargetType.SINGLE_NUMERIC)
			initTargetInfo();
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
				//itsTargetConcept.setSecondaryTarget(itsTable.getAttribute(getTargetAttributeName()));
				itsTargetConcept.setSecondaryTarget(itsTable.getColumn(getTargetAttributeName()));
				break;
			}
			default : break;
		}
		itsSearchParameters.setTargetConcept(itsTargetConcept);

		if (getMiscFieldName() != null)
			initTargetInfo();
	}

	private void jButtonMultiRegressionTargetsActionPerformed()
	{
		//itsTable.getAttribute(getTargetAttributeName()).makePrimaryTarget();
		itsTable.getColumn(getTargetAttributeName()).makePrimaryTarget();
		//itsTable.getColumn(getTargetAttributeName()).makePrimaryTarget();
		new MultiRegressionTargetsWindow(jListMultiRegressionTargets, itsSearchParameters, itsTable, this);
	}

	private void jButtonMultiTargetsActionPerformed()
	{
		// is modal, blocks all input to other windows until closed
		new MultiTargetsWindow(jListMultiTargets, itsSearchParameters);

		Object[] aSelection = jListMultiTargets.getSelectedValues();
		int aNrBinary = aSelection.length;
		List<Column> aList = new ArrayList<Column>(aNrBinary);

		// aSelection is in order and names are always present in itsColumns
		for (int i = 0, j = 0; i < aNrBinary; ++j)
		{
			if (aSelection[i].equals(itsTable.getColumn(j).getName()))
			{
				aList.add(itsTable.getColumn(j));
				++i;
			}
		}
		itsTargetConcept.setMultiTargets(aList);
		//update GUI, relies on disabling "Select Targets" if nrBinaries == 0
		initTargetInfo();
	}

	private void jButtonBaseModelActionPerformed()
	{
		setupSearchParameters();

		switch (itsTargetConcept.getTargetType())
		{
			case DOUBLE_REGRESSION :
			{
				Column aPrimaryColumn = itsTargetConcept.getPrimaryTarget();
				Column aSecondaryColumn = itsTargetConcept.getSecondaryTarget();

				RegressionMeasure anRM =
					new RegressionMeasure(itsSearchParameters.getQualityMeasure(), aPrimaryColumn, aSecondaryColumn);

				new ModelWindow(aPrimaryColumn, aSecondaryColumn, anRM, null); //trendline, no subset
				break;
			}
			case DOUBLE_CORRELATION :
			{
				new ModelWindow(itsTargetConcept.getPrimaryTarget(),
						itsTargetConcept.getSecondaryTarget(),
						null,
						null); //no trendline, no subset
				break;
			}
			case MULTI_LABEL :
			{
				List<Column> aList = itsTargetConcept.getMultiTargets();

				// compute base model
				Bayesian aBayesian =
					new Bayesian(new BinaryTable(itsTable, aList), aList);
				aBayesian.climb();
				DAG aBaseDAG = aBayesian.getDAG();
				aBaseDAG.print();

				new ModelWindow(aBaseDAG, 1200, 900);
				break;
			}
			default: return; // TODO other types not implemented yet
		}
	}

	/* MINING BUTTONS */
	private void jButtonSubgroupDiscoveryActionPerformed()
	{
		// takes care of setupSearchParameters();
		runSubgroupDiscovery(itsTable, 0, null);
	}

	private void runSubgroupDiscovery(Table theTable, int theFold, BitSet theBitSet)
	{
		setupSearchParameters();
		runSubgroupDiscovery(theTable, theFold, theBitSet, itsSearchParameters, true, getNrThreads());
	}

	// public, but does not perform ANY sanity checks
	public static SubgroupDiscovery runSubgroupDiscovery(Table theTable, int theFold, BitSet theBitSet, SearchParameters theSearchParameters, boolean showWindows, int theNrThreads)
	{
		TargetType aTargetType = theSearchParameters.getTargetConcept().getTargetType();
		//TODO other types not implemented yet
		if (!TargetType.isImplemented(aTargetType))
			return null;

		SubgroupDiscovery aSubgroupDiscovery = null;
		echoMiningStart();
		long aBegin = System.currentTimeMillis();

		switch(aTargetType)
		{
			case SINGLE_NOMINAL :
			{
				TargetConcept aTargetConcept = theSearchParameters.getTargetConcept();
				//recompute this number, as we may be dealing with cross-validation here, and hence a smaller number
				//int itsPositiveCount = theTable.countValues(theTable.getIndex(aTargetConcept.getPrimaryTarget().getName()), aTargetConcept.getTargetValue());
				int itsPositiveCount = aTargetConcept.getPrimaryTarget().countValues(aTargetConcept.getTargetValue());
				Log.logCommandLine("positive count: " + itsPositiveCount);
				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, itsPositiveCount);
				break;
			}

			case SINGLE_NUMERIC:
			{
				TargetConcept aTargetConcept = theSearchParameters.getTargetConcept();
				//recompute this number, as we may be dealing with cross-validation here, and hence a different value
				//float itsTargetAverage = theTable.getAverage(theTable.getIndex(aTargetConcept.getPrimaryTarget().getName()));
				float itsTargetAverage = theSearchParameters.getTargetConcept().getPrimaryTarget().getAverage();
				Log.logCommandLine("average: " + itsTargetAverage);
				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, itsTargetAverage);
				break;
			}
			case MULTI_LABEL :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable);
				break;
			}
			case DOUBLE_REGRESSION :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, true);
				break;
			}
			case DOUBLE_CORRELATION :
			{
				aSubgroupDiscovery = new SubgroupDiscovery(theSearchParameters, theTable, false);
				break;
			}
			default : return null; // TODO should never get here, throw warning
		}
		aSubgroupDiscovery.mine(System.currentTimeMillis(), theNrThreads);
		// if 2nd argument to above mine() is 0, you effectively run:
		//aSubgroupDiscovery.mine(System.currentTimeMillis());

		long anEnd = System.currentTimeMillis();
		float aMaxTime = theSearchParameters.getMaximumTime();

		if (aMaxTime > 0.0f && (anEnd > (aBegin + aMaxTime*60*1000)))
		{
			String aMessage = "Mining process ended prematurely due to time limit.";
			if (showWindows)
				JOptionPane.showMessageDialog(null,
								aMessage,
								"Time Limit",
								JOptionPane.INFORMATION_MESSAGE);
			else
				Log.logCommandLine(aMessage);
		}

		echoMiningEnd(anEnd - aBegin, aSubgroupDiscovery.getNumberOfSubgroups());

		// following is only needed if windows will be shown
		if (showWindows)
		{
			BinaryTable aBinaryTable = null;

			if (aTargetType == TargetType.MULTI_LABEL)
				aBinaryTable = new BinaryTable(theTable, theSearchParameters.getTargetConcept().getMultiTargets());

			new ResultWindow(theTable, aSubgroupDiscovery, aBinaryTable, theFold, theBitSet);
		}

		return aSubgroupDiscovery;
	}
/*
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
		aSubgroupDiscovery.mine(System.currentTimeMillis(), 0);
		// if 2nd argument to above mine() is 0, you effectively run:
		//aSubgroupDiscovery.mine(System.currentTimeMillis());

		long anEnd = System.currentTimeMillis();
		if (itsSearchParameters.getMaximumTime() > 0.0f)
			if (anEnd > aBegin + (((long) itsSearchParameters.getMaximumTime()) * 60 * 1000))
			JOptionPane.showMessageDialog(null, "Mining process ended prematurely due to time limit.",
											"Time Limit", JOptionPane.INFORMATION_MESSAGE);

		echoMiningEnd(anEnd - aBegin, aSubgroupDiscovery.getNumberOfSubgroups());

		switch (aTargetType)
		{
			case MULTI_LABEL :
			{
				BinaryTable aBinaryTable = new BinaryTable(aTable, itsTargetConcept.getMultiTargets());
				new ResultWindow(itsTable, aSubgroupDiscovery, aBinaryTable, theFold, theBitSet);
				break;
			}
			default :
			{
				new ResultWindow(aTable, aSubgroupDiscovery, null, theFold, theBitSet);
			}
		}
	}
*/
	private void jButtonRandomQualitiesActionPerformed()
	{
		String[] aSetup = new RandomQualitiesWindow().getSettings();
		if (!RandomQualitiesWindow.isValidRandomQualitiesSetup(aSetup))
			return;

		// same as setup for runSubgroupDiscovery?
		setupSearchParameters();

		QualityMeasure aQualityMeasure;
		switch (itsTargetConcept.getTargetType())
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
			default :
			{
				Log.logCommandLine("Unable to compute random qualities for " +
							itsTargetConcept.getTargetType().GUI_TEXT);
				return; // TODO also throw JDialog?
			}
		}

		Validation aValidation = new Validation(itsSearchParameters, itsTable, aQualityMeasure);
		double[] aQualities = aValidation.getQualities(aSetup);
		if (aQualities == null)
			return;

		NormalDistribution aDistro = new NormalDistribution(aQualities);

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

		// may take long, do this after window is shown
		Arrays.sort(aQualities);
		for (double aQuality : aQualities)
			Log.logCommandLine("" + aDistro.zTransform(aQuality));
		Log.logCommandLine("mu: " + aDistro.getMu());
		Log.logCommandLine("sigma: " + aDistro.getSigma());
	}

	private void jButtonCrossValidateActionPerformed()
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

	/* Setups */
	private void setupSearchParameters()
	{
		initSearchParameters();
		initTargetConcept();
	}

	private void initSearchParameters()
	{
		// theSearchParameters.setTarget(itsTable.getAttribute(getTargetAttributeName()));
		// theSearchParameters.setTargetAttributeShort(getTargetAttributeName());
		// theSearchParameters.setTargetValue(getMiscFieldName()); //only makes
		// sense for certain target concepts

		itsSearchParameters.setQualityMeasure(getQualityMeasureName());
		itsSearchParameters.setQualityMeasureMinimum(getQualityMeasureMinimum());

		itsSearchParameters.setSearchDepth(getSearchDepthMaximum());
		itsSearchParameters.setMinimumCoverage(getSearchCoverageMinimum());
		itsSearchParameters.setMaximumCoverage(getSearchCoverageMaximum());
		itsSearchParameters.setMaximumSubgroups(getSubgroupsMaximum());
		itsSearchParameters.setMaximumTime(getSearchTimeMaximum());

		itsSearchParameters.setSearchStrategy(getSearchStrategyName());
		itsSearchParameters.setSearchStrategyWidth(getSearchStrategyWidth());
		itsSearchParameters.setNominalNotEquals(getNominalNotEquals());
		itsSearchParameters.setNumericStrategy(getNumericStrategy());
		itsSearchParameters.setNrBins(getSearchStrategyNrBins());
		// TODO
		//itsSearchParameters.setNrThreads(getSearchStrategyNrBins());
/*
 * These values are no longer 'static', but can be changed in MultiTargetsWindow
		itsSearchParameters.setPostProcessingCount(SearchParameters.POST_PROCESSING_COUNT_DEFAULT);
//		itsSearchParameters.setMaximumPostProcessingSubgroups(100); // TODO not used

		// Bayesian stuff
		 // TODO This will overwrite values set in RandomQualitiesWindow
		if (QualityMeasure.getMeasureString(QualityMeasure.EDIT_DISTANCE).equals(getQualityMeasureName()))
			itsSearchParameters.setAlpha(SearchParameters.ALPHA_EDIT_DISTANCE);
		else
			itsSearchParameters.setAlpha(SearchParameters.ALPHA_DEFAULT);
		itsSearchParameters.setBeta(SearchParameters.BETA_DEFAULT);
*/
	}

	// Obsolete, this info is already up to date through *ActionPerformed methods
	private void initTargetConcept()
	{
		//itsTargetConcept.setPrimaryTarget(itsTable.getAttribute(getTargetAttributeName()));
		itsTargetConcept.setPrimaryTarget(itsTable.getColumn(getTargetAttributeName()));

		// target value
		switch (itsTargetConcept.getTargetType())
		{
			case SINGLE_NOMINAL :
			case MULTI_BINARY_CLASSIFICATION :
				itsTargetConcept.setTargetValue(getMiscFieldName());
				break;
			case DOUBLE_CORRELATION :
			case DOUBLE_REGRESSION :
				//itsTargetConcept.setSecondaryTarget(itsTable.getAttribute(getMiscFieldName()));
				itsTargetConcept.setSecondaryTarget(itsTable.getColumn(getMiscFieldName()));
				break;
			default : break;
		}
		// TODO add more details of target concept from GUI
	}

	public static void echoMiningStart()
	{
		Log.logCommandLine("Mining process started");
	}

	public static void echoMiningEnd(long theMilliSeconds, int theNumberOfSubgroups)
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
		final TargetType aTargetType = itsTargetConcept.getTargetType();

		// clear all
		removeAllTargetAttributeItems();
		if ((aTargetType == TargetType.DOUBLE_REGRESSION) ||
				(aTargetType == TargetType.DOUBLE_CORRELATION))
			removeAllMiscFieldItems();

		// primary target and MiscField
		boolean isEmpty = true;
		for (int i = 0; i < itsTable.getNrColumns(); i++) {
			Column anAttribute = itsTable.getColumn(i);
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
				addTargetAttributeItem(anAttribute.getName());
				if ((aTargetType == TargetType.DOUBLE_REGRESSION) ||
						(aTargetType == TargetType.DOUBLE_CORRELATION))
					addMiscFieldItem(anAttribute.getName());

				isEmpty = false;
			}
		}
		if (aTargetType == TargetType.SINGLE_NOMINAL && isEmpty) // no target attribute selected
			removeAllMiscFieldItems();

		if (aTargetType == TargetType.DOUBLE_REGRESSION)
		{
			computeMultiRegressionTargets();
		}

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
			((DefaultListModel) jListMultiTargets.getModel()).clear();

			int aCount = 0;
			List<Column> aList = new ArrayList<Column>();
			for (Column c: itsTable.getColumns())
				if (c.isBinaryType() && c.getIsEnabled())
				{
					addMultiTargetsItem(c.getName());
					aList.add(c);
					aCount++;
				}
			itsTargetConcept.setMultiTargets(aList);
			jListMultiTargets.setSelectionInterval(0, jListMultiTargets.getModel().getSize() - 1);
			jLFieldTargetInfo.setText(String.valueOf(aCount));
		}
	}

	private void computeMultiRegressionTargets()
	{
		((DefaultListModel) jListMultiRegressionTargets.getModel()).clear();

		int aCount = 0;
		List<Column> aList = new ArrayList<Column>();
		for (Column c: itsTable.getColumns())
			if (c.isNumericType() && c.getIsEnabled() && !itsTargetConcept.getPrimaryTarget().equals(c))
			{
				addMultiRegressionTargetsItem(c.getName());
				aList.add(c);
				aCount++;
			}
		itsTargetConcept.setMultiRegressionTargets(aList);
		jListMultiRegressionTargets.setSelectionInterval(0, jListMultiRegressionTargets.getModel().getSize() - 1);
		jLFieldTargetInfo.setText(String.valueOf(aCount));
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
		//TreeSet<String> aValues =
		//	itsTable.getDomain(
		//		itsTable.getIndex(getTargetAttributeName()));
		TreeSet<String> aValues = itsTable.getColumn(getTargetAttributeName()).getDomain();
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
					//itsTable.countValues(itsTable.getIndex(getTargetAttributeName()), getMiscFieldName());
					itsTable.getColumn(getTargetAttributeName()).countValues(getMiscFieldName());
				float aPercentage = ((float) itsPositiveCount * 100.0f) / (float) itsTotalCount;
				NumberFormat aFormatter = NumberFormat.getNumberInstance();
				aFormatter.setMaximumFractionDigits(1);
				jLabelTargetInfo.setText(" # positive");
				jLFieldTargetInfo.setText(itsPositiveCount + " (" + aFormatter.format(aPercentage) + " %)");
				break;
			}
			case SINGLE_NUMERIC :
			{
				String aTarget = getTargetAttributeName();
				if (aTarget != null) //initTargetInfo might be called before item is actually selected
				{
					//itsTargetAverage = itsTable.getAverage(itsTable.getIndex(aTarget));
					itsTargetAverage = itsTable.getColumn(aTarget).getAverage();
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
				Column aPrimaryColumn = itsTargetConcept.getPrimaryTarget();
				Column aSecondaryColumn = itsTargetConcept.getSecondaryTarget();
				CorrelationMeasure aCM =
					new CorrelationMeasure(QualityMeasure.CORRELATION_R, aPrimaryColumn, aSecondaryColumn);
				jLabelTargetInfo.setText(" correlation");
				jLFieldTargetInfo.setText(Double.toString(aCM.getEvaluationMeasureValue()));
				break;
			}
			case MULTI_LABEL :
			{
				jLabelTargetInfo.setText(" # binary targets");
				jLFieldTargetInfo.setText(String.valueOf(itsTargetConcept.getMultiTargets().size()));
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

	private void enableBaseModelButtonCheck()
	{
		TargetType aType = itsTargetConcept.getTargetType();
		boolean isEnabled = TargetType.hasBaseModel(aType);
		if (aType == TargetType.MULTI_LABEL && jListMultiTargets.getSelectedIndices().length == 0)
			isEnabled = false;
		jButtonBaseModel.setEnabled(isEnabled);
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
	private void addMultiTargetsItem(String theItem) { ((DefaultListModel) jListMultiTargets.getModel()).addElement(theItem); }
	private void removeAllMultiTargetsItems() { ((DefaultListModel) jListMultiTargets.getModel()).clear(); }

	// target type - jList secondary targets
	private void addMultiRegressionTargetsItem(String theItem) { ((DefaultListModel) jListMultiRegressionTargets.getModel()).addElement(theItem); }
	private void removeAllMultiRegressionTargetsItems() { ((DefaultListModel) jListMultiRegressionTargets.getModel()).clear(); }


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

	// search strategy - nominal not equals
	private boolean getNominalNotEquals() { return jCheckBoxNotEquals.isSelected(); }

	// search strategy - numeric operators
	private String getNumericOperators() { return (String) jComboBoxNumericOperators.getSelectedItem(); }

	// search strategy - numeric strategy
	private String getNumericStrategy() { return (String) jComboBoxSearchStrategyNumeric.getSelectedItem(); }
	private void setNumericStrategy(String aStrategy) { jComboBoxSearchStrategyNumeric.setSelectedItem(aStrategy); }

	// search strategy - number of bins
	private int getSearchStrategyNrBins() { return getValue(8, jTextFieldSearchStrategyNrBins.getText()); }
	private void setSearchStrategyNrBins(String aValue) { jTextFieldSearchStrategyNrBins.setText(aValue); }

	// search strategy - number of threads
	private int getNrThreads() { return getValue(Runtime.getRuntime().availableProcessors(), jTextFieldNrThreads.getText()); }
	private void setNrThreads(String aValue) { jTextFieldNrThreads.setText(aValue); }

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
	private JMenuItem jMenuItemExplore;
	private JMenuItem jMenuItemMetaData;
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
	private JMenu jMenuGui;	// XXX for testing only
	private JPanel jPanelSouth;
	private JPanel jPanelMineButtons;
	private JButton jButtonBrowse;
	private JButton jButtonExplore;
	private JButton jButtonMetaData;
	private JButton jButtonCrossValidate;
	private JButton jButtonSubgroupDiscovery;
	private JButton jButtonThreshold;
	private JPanel jPanelCenter;

	private JPanel jPanelRuleTarget;
	private JPanel jPanelRuleTargetLabels;
	private JPanel jPanelRuleTargetButtons;
	private JLabel jLabelTargetTable;
	private JLabel jLabelTargetAttribute;
	private JLabel jLabelMiscField;
	private JLabel jLabelMultiRegressionTargets;
	private JLabel jLabelMultiTargets;
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
	private JButton jButtonMultiRegressionTargets;
	private JButton jButtonMultiTargets;
	private JList jListMultiRegressionTargets;
	private JList jListMultiTargets;
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
	private JLabel jLabelEvaluationThreshold;
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
	private JLabel jLabelNotEquals;
	private JLabel jLabelNumericOperators;
	private JLabel jLabelSearchStrategyNumeric;
	private JLabel jLabelSearchStrategyNrBins;
	private JLabel jLabelNrThreads;
	private JPanel jPanelSearchStrategyFields;
	private JComboBox jComboBoxSearchStrategyType;
	private JTextField jTextFieldSearchStrategyWidth;
	private JCheckBox jCheckBoxNotEquals;
	private JComboBox jComboBoxNumericOperators;
	private JComboBox jComboBoxSearchStrategyNumeric;
	private JTextField jTextFieldSearchStrategyNrBins;
	private JTextField jTextFieldNrThreads;

	// GUI defaults and convenience methods
	private static JButton initButton(String theName, Mnemonic theMnemonic)
	{
		JButton aButton = new JButton();
		aButton.setPreferredSize(GUI.BUTTON_DEFAULT_SIZE);
		aButton.setBorder(new BevelBorder(0));
		aButton.setMinimumSize(GUI.BUTTON_MINIMUM_SIZE);
		aButton.setMaximumSize(GUI.BUTTON_MAXIMUM_SIZE);
		aButton.setFont(GUI.DEFAULT_BUTTON_FONT);
		aButton.setText(theName);
		aButton.setMnemonic(theMnemonic.MNEMONIC);
		return aButton;
	}

	// TODO include accelerator
	private enum Mnemonic
	{
		// never use accelerator for TargetType/ setting dependent items
		FILE('F'),			// no accelerator
		OPEN_FILE('O'),			// accelerator
		OPEN_GENE_RANK('G'),		// not used for now
		BROWSE('B'),			// accelerator
		EXPLORE('E'),			// accelerator
		META_DATA('D'),			// accelerator // TODO does not work?
		SUBGROUP_DISCOVERY('S'),	// accelerator
		CREATE_AUTORUN_FILE('C'),	// accelerator
		ADD_TO_AUTORUN_FILE('A'),	// accelerator
		EXIT('X'),			// accelerator
		ABOUT('A'),			// no accelerator, 'A' used 2x
		ABOUT_CORTANA('I'),		// accelerator
		TARGETS_AND_SETTINGS('T'),	// no accelerator
		SECONDARY_TERTIARY_TARGETS('Y'),// not used for now
		BASE_MODEL('M'),		// no accelerator
		CROSS_VALIDATE('V'),		// no accelerator
		COMPUTE_THRESHOLD('P'),		// no accelerator
		RANDOM_SUBGROUPS('R'),		// not used for now
		RANDOM_CONDITIONS('C'),		// not used for now, 'C' used 2x
		SWAP_RANDOMISE('W');		// not used for now

		final int MNEMONIC;
		private Mnemonic(int theMnemonic) { MNEMONIC = theMnemonic; }
	}

	private static JLabel initJLabel(String theName)
	{
		JLabel aJLable = new JLabel(theName);
		aJLable.setFont(GUI.DEFAULT_TEXT_FONT);
		return aJLable;
	}

	// XXX for GUI debugging only, DO NOT REMOVE
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
	// XXX end GUI test
}
