package nl.liacs.subdisc.knime;

import java.util.*;

import javax.swing.*;

import org.knime.base.node.viz.histogram.util.*;
import org.knime.core.data.*;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.node.util.ColumnFilterPanel.ValueClassFilter;
import org.knime.core.node.util.*;

/**
 * <code>NodeDialog</code> for the "Cortana" Node.
 * Subgroup Discovery using Cortana.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Marvin Meeng
 */
public class CortanaNodeDialog extends NodeDialogPane {
	private final CortanaSettings settings = new CortanaSettings();

	// A component for each adjustable Cortana parameter
	// Target Concept
	private final DialogComponentColumnNameSelection dc_targetColumn;
	private final DialogComponentStringSelection dc_qualityMeasure;
	private final DialogComponentNumberEdit dc_measureMinimum;
	private final DialogComponentStringSelection dc_targetValue;
	private final JLabel targetInfoLabel;
	private final JLabel targetInfoText;

	// Search Conditions
	private final DialogComponentNumberEdit dc_refinementDepth;
	private final DialogComponentNumberEdit dc_minimumCoverage;
	private final DialogComponentNumberEdit dc_maximumCoverage;
	private final DialogComponentNumberEdit dc_maximumSubgroups;
	private final DialogComponentNumberEdit dc_maximumTime;

	// Search Strategy
	private final DialogComponentStringSelection dc_searchStrategy;
	private final DialogComponentNumberEdit dc_searchWidth;
	private final DialogComponentBoolean dc_setValuedNominals;
	private final DialogComponentStringSelection dc_numericOperators;
	private final DialogComponentStringSelection dc_numericStrategy;
	private final DialogComponentNumberEdit dc_numberOfBins;
	private final DialogComponentNumberEdit dc_numberOfThreads;

	private DataTableSpec tableSpec = null;
	// TODO could use package private CortanaSetting.sm_primaryTarget
	private SettingsModelString settingsModelPrimaryTarget;

	/**
	 * New pane for configuring Cortana node dialog.
	 * This is just a suggestion to demonstrate possible default dialog
	 * components.
	 */
	protected CortanaNodeDialog() {
		settingsModelPrimaryTarget = settings.sm_PrimaryTarget;
		settingsModelPrimaryTarget.addChangeListener(new javax.swing.event.ChangeListener() {
			@Override
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				String columnName = settingsModelPrimaryTarget.getStringValue();
				DataColumnSpec columnSpec = tableSpec.getColumnSpec(columnName);
				DataColumnDomain domain = columnSpec.getDomain();
				Set<DataCell> values = domain.getValues();
				List<String> newItems = new ArrayList<String>();
				for (DataCell c : values)
					newItems.add(c.toString());
				// retrieve currently selected
				// if (there is one), keep
				// else select first value of domain
				dc_targetValue.replaceListItems(newItems, null);
			}
		});

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		// Target Concept
		JPanel subPanel = createSubPanel("Target Concept");
		// Target Column
		dc_targetColumn = new DialogComponentColumnNameSelection(settingsModelPrimaryTarget, Parameter.PRIMARY_TARGET.toString(), 0, getValidNominalsFilter());
		addAligned(subPanel, dc_targetColumn.getComponentPanel());
		// Quality Measure
		dc_qualityMeasure = new DialogComponentStringSelection(settings.sm_QualityMeasure, Parameter.QUALITY_MEASURE.toString(), CortanaSettings.QUALITY_MEASURES );
		addAligned(subPanel, dc_qualityMeasure.getComponentPanel());
		// Measure minimum
		dc_measureMinimum = new DialogComponentNumberEdit(settings.sm_MeasureMinimum, Parameter.MEASURE_MINIMUM.toString());
		addAligned(subPanel, dc_measureMinimum.getComponentPanel());
		// Target Value
		dc_targetValue = new DialogComponentStringSelection(settings.sm_TargetValue, Parameter.TARGET_VALUE.toString(), new String[] {""} );
		addAligned(subPanel, dc_targetValue.getComponentPanel());
		// Target Info
		JPanel targetInfoPanel = new JPanel();
		targetInfoPanel.add(targetInfoLabel = new JLabel("# positives"));
		targetInfoPanel.add(targetInfoText = new JLabel("x (y.z %)"));
		addAligned(subPanel, targetInfoPanel);
		panel.add(subPanel);

		// Search Conditions
		subPanel = createSubPanel("Search Conditions");
		dc_refinementDepth = new DialogComponentNumberEdit(settings.sm_RefinementDepth, Parameter.REFINEMENT_DEPTH.toString());
		addAligned(subPanel, dc_refinementDepth.getComponentPanel());
		dc_minimumCoverage = new DialogComponentNumberEdit(settings.sm_MinimumCoverage, Parameter.MINIMUM_COVERAGE.toString());
		addAligned(subPanel, dc_minimumCoverage.getComponentPanel());
		dc_maximumCoverage = new DialogComponentNumberEdit(settings.sm_MaximumCoverageFraction, Parameter.MAXIMUM_COVERAGE_FRACTION.toString());
		addAligned(subPanel, dc_maximumCoverage.getComponentPanel());
		dc_maximumSubgroups = new DialogComponentNumberEdit(settings.sm_MaximumSubgroups, Parameter.MAXIMUM_SUBGROUPS.toString());
		addAligned(subPanel, dc_maximumSubgroups.getComponentPanel());
		dc_maximumTime = new DialogComponentNumberEdit(settings.sm_MaximumTime, Parameter.MAXIMUM_TIME.toString());
		addAligned(subPanel, dc_maximumTime.getComponentPanel());
		panel.add(subPanel);

		// Search Strategy
		subPanel = createSubPanel("Search Strategy");
		dc_searchStrategy = new DialogComponentStringSelection(settings.sm_SearchStrategy, Parameter.SEARCH_STRATEGY.toString(), CortanaSettings.SEARCH_STRATEGIES );
		addAligned(subPanel, dc_searchStrategy.getComponentPanel());
		dc_searchWidth = new DialogComponentNumberEdit(settings.sm_SearchWidth, Parameter.SEACH_WIDTH.toString());
		addAligned(subPanel, dc_searchWidth.getComponentPanel());
		dc_setValuedNominals = new DialogComponentBoolean(settings.sm_SetValuedNominals, Parameter.SET_VALUED_NOMINALS.toString());
		addAligned(subPanel, dc_setValuedNominals.getComponentPanel());
		dc_numericOperators = new DialogComponentStringSelection(settings.sm_NumericOperators, Parameter.NUMERIC_OPERATORS.toString(), CortanaSettings.NUMERIC_OPERATORS );
		addAligned(subPanel, dc_numericOperators.getComponentPanel());
		dc_numericStrategy = new DialogComponentStringSelection(settings.sm_NumericStrategy, Parameter.NUMERIC_STRATEGY.toString(), CortanaSettings.NUMERIC_STRATEGIES );
		addAligned(subPanel, dc_numericStrategy.getComponentPanel());
		dc_numberOfBins = new DialogComponentNumberEdit(settings.sm_NumberOfBins, Parameter.NUMBER_OF_BINS.toString());
		addAligned(subPanel, dc_numberOfBins.getComponentPanel());
		dc_numberOfThreads = new DialogComponentNumberEdit(settings.sm_NumberOfThreads, Parameter.NUMBER_OF_THREADS.toString());
		addAligned(subPanel, dc_numberOfThreads.getComponentPanel());
		panel.add(subPanel);

		addTab("Settings", panel);
	}

	private static void addAligned(JPanel panel, JComponent component)
	{
		// FIXME RIGHT_ALIGNMENT has no effect
		component.setAlignmentX(JFrame.RIGHT_ALIGNMENT);
		panel.add(component);
	}

	private static JPanel createSubPanel(final String title) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title));
		return panel;
	}

	@SuppressWarnings("unchecked") // DataValue -> NominalValue
	static CombinedColumnFilter getValidNominalsFilter() {
		return new CombinedColumnFilter(new ValueClassFilter(NominalValue.class), NoDomainColumnFilter.getInstance());
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
		tableSpec = specs[0];
		// FIXME
		this.settings.addDataTableSpec(tableSpec);
		dc_targetColumn.loadSettingsFrom(settings, specs);

		try {
			this.settings.loadSettingsFrom(settings);
		} catch (InvalidSettingsException e) {
			throw new NotConfigurableException(e.getMessage());
		}
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		this.settings.saveSettingsTo(settings);
	}
}
