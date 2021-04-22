package nl.liacs.subdisc.knime;

import java.io.*;

import org.knime.base.node.io.csvwriter.*;
import org.knime.base.node.io.filereader.*;
import org.knime.core.data.*;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.node.util.*;
import org.knime.core.util.tokenizer.SettingsStatus;

/**
 * This is the model implementation of Cortana.
 * Subgroup Discovery using Cortana.
 *
 * @author Marvin Meeng
 */
public class CortanaNodeModel extends NodeModel {
	// the logger instance
	private static final NodeLogger LOGGER = NodeLogger.getLogger(CortanaNodeModel.class);

	private CortanaSettings settings = new CortanaSettings();

	/**
	 * Constructor for the node model.
	 */
	protected CortanaNodeModel() {
		super(1, 2);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
		final String[] columnNames = inData[0].getDataTableSpec().getColumnNames();

		// useless if no there are no column
		if (columnNames.length <= 0)
			throw new Exception("Invalid input table, 0 columns.");

		// create tmp dir for XML and csv data
		final String tmpDir = "cortana_tmp/";
		final long timestamp = System.nanoTime();
		final String tmpXML = tmpDir + timestamp + ".xml";
		final String csvFile = timestamp + ".csv";
		final String csvPath = tmpDir + csvFile;
		final File dir = new File(tmpDir);
		dir.mkdir();

		// write settings to .xml file
		exec.setMessage("Creating xml file...");
		settings.toAutorunXML(tmpXML, csvFile);

		// write .csv data file based on inData[0]
		LOGGER.info("Creating csv file...");
		FileWriterSettings fws = new FileWriterSettings();
//		fws.setQuoteMode(FileWriterSettings.quoteMode.REPLACE);
//		fws.setSeparatorReplacement("\\t");
		fws.setWriteColumnHeader(true);
		final CSVWriter csvWriter = new CSVWriter(new FileWriter(csvPath), fws);
		csvWriter.write(inData[0], exec.createSubProgress(0.1));
		csvWriter.newLine();
		csvWriter.close();

		// run Cortana
		exec.checkCanceled();	// auto throws exception if true
		exec.setMessage("Running Cortana...");
		nl.liacs.subdisc.XMLAutoRun.autoRunSetting(new String[] { tmpXML });
		exec.setProgress(0.7, "Cortana run finished, retrieving result ...");

		// retrieve auto generated Cortana result
		File result = null;
		for (File f : dir.listFiles()) {
			if (f.getName().startsWith(timestamp + "_")) {
				result = f;
				break;
			}
		}
		if (result == null)
			throw new Exception("No Cortana result file found.");

		FileReaderNodeSettings frns = new FileReaderNodeSettings();
		frns.setDataFileLocationAndUpdateTableName(result.toURI().toURL());
		frns.setFileHasRowHeaders(false);
		frns.setFileHasRowHeadersUserSet(true);
		frns.setFileHasColumnHeaders(true);
		frns.setFileHasColumnHeadersUserSet(true);
		frns.addRowDelimiter("\n", true);
//		frns.addQuotePattern("_", "_");
//		frns.setQuoteUserSet(true);
		frns.addDelimiterPattern("\t", false, false, false);
		frns.setDelimiterUserSet(true);
		frns = FileAnalyzer.analyze(frns,null);
		SettingsStatus statusOfSettings = frns.getStatusOfSettings();
		if (statusOfSettings.getNumOfErrors() > 0)
			throw new Exception("Invalid Cortana result file.");
/*
		// can not put output columnSpec in configure, as the types change based on the target type

		// CSV-reader can get ColumnSpecs dynamically, somehow
		String[] names = new String[] { "Nr.", "Depth", "Coverage", "Quality", "Probability", "Positives", "p-Value", "Conditions" };
		DataType[] types = new DataType[] { IntCell.TYPE, IntCell.TYPE, IntCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE, StringCell.TYPE, StringCell.TYPE };
		DataColumnSpec[] columnSpecs = new DataColumnSpec[names.length];
		DataColumnSpecCreator dcsc = new DataColumnSpecCreator(names[0], types[0]);
		columnSpecs[0] = dcsc.createSpec();
		for (int i = 1; i< columnSpecs.length; ++i) {
			dcsc.setName(names[i]);
			dcsc.setType(types[i]);
			columnSpecs[i] = dcsc.createSpec();
		}
		DataTableSpec tableSpec = new DataTableSpec(columnSpecs);
*/
//		exec.setMessage("");
		FileTable fileTable = new FileTable(frns.createDataTableSpec(), frns, exec.createSubExecutionContext(0.1));
		BufferedDataTable bdt = exec.createBufferedDataTable(fileTable, exec.createSubExecutionContext(0.1));

		// remove tmp dir? else use unique timestamps

		return new BufferedDataTable[]{ inData[0], bdt };
		// used as input for SubgroupViewNodes
		// use condition as data.rowFilter to show rows (subgroup members)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO Code executed on reset.
		// Models build during execute are cleared here.
		// Also data handled in load/saveInternals will be erased here.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		// TODO: check if user settings are available, fit to the incoming
		// table structure, and the incoming types are feasible for the node
		// to execute. If the node can execute in its current state return
		// the spec of its output data table(s) (if you can, otherwise an array
		// with null elements), or throw an exception with a useful user message

		// XXX set a new one whenever the input data changes
		settings.addDataTableSpec(inSpecs[0]);

		CombinedColumnFilter filter = CortanaNodeDialog.getValidNominalsFilter();

		SettingsModelString primaryTarget = settings.sm_PrimaryTarget;
		if (primaryTarget.getStringValue() == null) {
			for (DataColumnSpec d : inSpecs[0]) {
				if (filter.includeColumn(d)) {
					primaryTarget.setStringValue(d.getName());
					setWarningMessage("Target column has been preset to: " + d.getName());
					// TODO set settignsModel target here
						break;
					}
				}
		} else {
			if (!inSpecs[0].containsName(primaryTarget.getStringValue()))
				throw new InvalidSettingsException("Primary target not present in input table.");
		}
		// TODO check for existence of TargetValue here
		// DataTable may have changed since the last load/save cycle

		return new DataTableSpec[]{inSpecs[0], null};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		this.settings.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.validateSettings(settings);
	}

/*
 * TODO STUFF HERE
 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {

	// TODO load internal data. 
	// Everything handed to output ports is loaded automatically (data
	// returned by the execute method, models loaded in loadModelContent,
	// and user settings set through loadSettingsFrom - is all taken care 
	// of). Load here only the other internals that need to be restored
	// (e.g. data used by the views).
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
	// TODO save internal models. 
	// Everything written to output ports is saved automatically (data
	// returned by the execute method, models saved in the saveModelContent,
	// and user settings saved through saveSettingsTo - is all taken care 
	// of). Save here only the other internals that need to be preserved
	// (e.g. data used by the views).
	}
}
