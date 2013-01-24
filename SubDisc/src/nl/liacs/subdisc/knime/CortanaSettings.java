package nl.liacs.subdisc.knime;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import nl.liacs.subdisc.*;

import org.knime.core.data.*;
import org.knime.core.node.*;
import org.knime.core.node.defaultnodesettings.*;
import org.w3c.dom.*;
import org.w3c.dom.Node;

public class CortanaSettings {
	// retrieve some data from Cortana
	public static final String[] QUALITY_MEASURES;
	static {
		List<String> list = new ArrayList<String>();
// FIXME MM	for (int i = QualityMeasure.getFirstEvaluationMeasure(TargetType.SINGLE_NOMINAL); i <= QualityMeasure.getLastEvaluationMesure(TargetType.SINGLE_NOMINAL); ++i)
// FIXME MM		list.add(QualityMeasure.getMeasureString(i));
		for (QM qm : QM.getQualityMeasures(TargetType.SINGLE_NOMINAL))
			list.add(qm.GUI_TEXT);
		QUALITY_MEASURES = list.toArray(new String[0]);
	}
	public static final String[] SEARCH_STRATEGIES = toStringArray(SearchStrategy.class);
	public static final String[] NUMERIC_OPERATORS = toStringArray(NumericOperatorSetting.class);
	public static final String[] NUMERIC_STRATEGIES = toStringArray(NumericStrategy.class);
	// TODO add to Cortana.EnumInterface
	private static String[] toStringArray(Class<? extends EnumInterface> enumClass) {
		EnumInterface[] ea = enumClass.getEnumConstants();
		final String[] sa = new String[ea.length];
		for (int i = 0; i < ea.length; ++i)
			sa[i] = ea[i].toString();
		return sa;
	}

	// Target Concept
	final SettingsModelString sm_PrimaryTarget;
	final SettingsModelString sm_QualityMeasure;
	final SettingsModelDouble sm_MeasureMinimum;
	final SettingsModelString sm_TargetValue;
	// Search Conditions
	final SettingsModelInteger sm_RefinementDepth;
	final SettingsModelInteger sm_MinimumCoverage;
	final SettingsModelDouble sm_MaximumCoverageFraction;
	final SettingsModelInteger sm_MaximumSubgroups;
	final SettingsModelDouble sm_MaximumTime;
	// Search Strategy
	final SettingsModelString sm_SearchStrategy;
	final SettingsModelInteger sm_SearchWidth;
	final SettingsModelBoolean sm_SetValuedNominals;
	final SettingsModelString sm_NumericOperators;
	final SettingsModelString sm_NumericStrategy;
	final SettingsModelInteger sm_NumberOfBins;
	final SettingsModelInteger sm_NumberOfThreads;
	private final SettingsModel[] models;
	private DataTableSpec tableSpec;

	/* 
	 * NOTE can not loop over Parameter.values() because cast is done here.
	 * Else casting to the correct type is forced upon the caller.
	 */
	public CortanaSettings() {
		models = new SettingsModel[16];
		// Target Concept
		models[0] = (sm_PrimaryTarget = (SettingsModelString) Parameter.PRIMARY_TARGET.getSettingModel());
		models[1] = (sm_QualityMeasure = (SettingsModelString) Parameter.QUALITY_MEASURE.getSettingModel());
		models[2] = (sm_MeasureMinimum = (SettingsModelDouble) Parameter.MEASURE_MINIMUM.getSettingModel());
		models[3] = (sm_TargetValue = (SettingsModelString) Parameter.TARGET_VALUE.getSettingModel());
		// Search Conditions
		models[4] = (sm_RefinementDepth = (SettingsModelInteger) Parameter.REFINEMENT_DEPTH.getSettingModel());
		models[5] = (sm_MinimumCoverage = (SettingsModelInteger) Parameter.MINIMUM_COVERAGE.getSettingModel());
		models[6] = (sm_MaximumCoverageFraction = (SettingsModelDouble) Parameter.MAXIMUM_COVERAGE_FRACTION.getSettingModel());
		models[7] = (sm_MaximumSubgroups = (SettingsModelInteger) Parameter.MAXIMUM_SUBGROUPS.getSettingModel());
		models[8] = (sm_MaximumTime = (SettingsModelDouble) Parameter.MAXIMUM_TIME.getSettingModel());
		// Search Strategy
		models[9] = (sm_SearchStrategy = (SettingsModelString) Parameter.SEARCH_STRATEGY.getSettingModel());
		models[10] = (sm_SearchWidth = (SettingsModelInteger) Parameter.SEACH_WIDTH.getSettingModel());
		models[11] = (sm_SetValuedNominals = (SettingsModelBoolean) Parameter.SET_VALUED_NOMINALS.getSettingModel());
		models[12] = (sm_NumericOperators = (SettingsModelString) Parameter.NUMERIC_OPERATORS.getSettingModel());
		models[13] = (sm_NumericStrategy = (SettingsModelString) Parameter.NUMERIC_STRATEGY.getSettingModel());
		models[14] = (sm_NumberOfBins = (SettingsModelInteger) Parameter.NUMBER_OF_BINS.getSettingModel());
		models[15] = (sm_NumberOfThreads = (SettingsModelInteger) Parameter.NUMBER_OF_THREADS.getSettingModel());
	}

	// FIXME move to constructor?
	void addDataTableSpec(DataTableSpec dataTableSpec) {
		tableSpec = dataTableSpec;
	}

	protected void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		for (SettingsModel s : models)
			s.loadSettingsFrom(settings);
	}

	protected void saveSettingsTo(NodeSettingsWO settings) {
		for (SettingsModel s : models)
			s.saveSettingsTo(settings);
	}

	public void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		for (SettingsModel s : models)
			s.validateSettings(settings);
	}

	public void toAutorunXML(String xmlPath, String csvFile) {
		Document doc = buildDocument();
		Node autorunNode = doc.getLastChild();
		Node experimentNode = addExperimentNode(doc);
		autorunNode.appendChild(experimentNode);
		addTargetConceptNode(experimentNode);
		addSearchParametersNode(experimentNode);
		addTableNode(experimentNode, csvFile);
		// set this number after all experiments are added
		// currently always 1
		((Element) autorunNode).setAttribute("nr_experiments",
				String.valueOf(autorunNode
						.getChildNodes()
						.getLength()));
		saveDocument(doc, new File(xmlPath));
	}

	private static Document buildDocument()
	{
		Document document = null;

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
//			builder.setErrorHandler(XMLErrorHandler.THE_ONLY_INSTANCE); // log to logger
			DOMImplementation domImplementation = builder.getDOMImplementation();
			document = domImplementation.createDocument(null,
									"autorun",
									domImplementation.createDocumentType("autorun",
														"autorun",
														"autorun.dtd"));
		} catch (Exception e) {
			// TODO to logger
			e.printStackTrace();
		}

		return document;
	}

	private static Node addExperimentNode(Document document) {
		Node node = document.createElement("experiment");
		((Element) node).setAttribute("id", "0");
		return node;
	}

	private void addTargetConceptNode(Node experimentNode) {
		// target_concept
		Node node = experimentNode.appendChild(experimentNode
							.getOwnerDocument()
							.createElement("target_concept"));

		// nr_target_attributes XXX always 1 for now
		Element e = node.getOwnerDocument().createElement("nr_target_attributes");
		e.setTextContent("1");
		node.appendChild(e);
		// target_type XXX always single nominal for now
		Element e2 = node.getOwnerDocument().createElement("target_type");
		e2.setTextContent("single nominal");
		node.appendChild(e2);
		// primary_target XXX quotes rely on FileWriterSettings setting in CortanaNodeModel.execute()
		Element e3 = node.getOwnerDocument().createElement("primary_target");
		e3.setTextContent("\"" + sm_PrimaryTarget.getStringValue() + "\"");
//		e3.setTextContent(sm_PrimaryTarget.getStringValue().replaceAll("\t", "\\t"));
		node.appendChild(e3);
		// target_value XXX quotes rely on FileWriterSettings setting in CortanaNodeModel.execute()
		Element e4 = node.getOwnerDocument().createElement("target_value");
		e4.setTextContent("\"" + sm_TargetValue.getStringValue() + "\"");
//		e4.setTextContent(sm_TargetValue.getStringValue().replaceAll("\t", "\\t"));
		node.appendChild(e4);
		// secondary_target XXX always empty for now
		Element e5 = node.getOwnerDocument().createElement("secondary_target");
		node.appendChild(e5);
		// multi_targets XXX always empty for now
		Element e6 = node.getOwnerDocument().createElement("multi_targets");
		node.appendChild(e6);
	}

	private void addSearchParametersNode(Node experimentNode) {
		// search_parameters
		Node node = experimentNode.appendChild(experimentNode
							.getOwnerDocument()
							.createElement("search_parameters"));

		// quality_measure
		Element e = node.getOwnerDocument().createElement("quality_measure");
		e.setTextContent(sm_QualityMeasure.getStringValue());
		node.appendChild(e);
		// // quality_measure_minimum
		Element e2 = node.getOwnerDocument().createElement("quality_measure_minimum");
		e2.setTextContent(String.valueOf(sm_MeasureMinimum.getDoubleValue()));
		node.appendChild(e2);
		// search_depth
		Element e3 = node.getOwnerDocument().createElement("search_depth");
		e3.setTextContent(String.valueOf(sm_RefinementDepth.getIntValue()));
		node.appendChild(e3);
		// minimum_coverage
		Element e4 = node.getOwnerDocument().createElement("minimum_coverage");
		e4.setTextContent(String.valueOf(sm_MinimumCoverage.getIntValue()));
		node.appendChild(e4);
		// maximum_coverage_fraction
		Element e5 = node.getOwnerDocument().createElement("maximum_coverage_fraction");
		e5.setTextContent(String.valueOf(sm_MaximumCoverageFraction.getDoubleValue()));
		node.appendChild(e5);
		// maximum_subgroups
		Element e6 = node.getOwnerDocument().createElement("maximum_subgroups");
		e6.setTextContent(String.valueOf(sm_MaximumSubgroups.getIntValue()));
		node.appendChild(e6);
		// maximum_time
		Element e7 = node.getOwnerDocument().createElement("maximum_time");
		e7.setTextContent(String.valueOf(sm_MaximumTime.getDoubleValue()));
		node.appendChild(e7);

		// search_strategy
		Element e8 = node.getOwnerDocument().createElement("search_strategy");
		e8.setTextContent(sm_SearchStrategy.getStringValue());
		node.appendChild(e8);
		// set-valued_nominals
		Element e9 = node.getOwnerDocument().createElement("use_nominal_sets");
		e9.setTextContent(String.valueOf(sm_SetValuedNominals.getBooleanValue()));
		node.appendChild(e9);
		// search_strategy_width
		Element e10 = node.getOwnerDocument().createElement("search_strategy_width");
		e10.setTextContent(String.valueOf(sm_SearchWidth.getIntValue()));
		node.appendChild(e10);
		// numeric_operators
		Element e11 = node.getOwnerDocument().createElement("numeric_operators");
		e11.setTextContent(sm_NumericOperators.getStringValue());
		node.appendChild(e11);
		// numeric_strategy
		Element e12 = node.getOwnerDocument().createElement("numeric_strategy");
		e12.setTextContent(sm_NumericStrategy.getStringValue());
		node.appendChild(e12);
		// nr_bins
		Element e13 = node.getOwnerDocument().createElement("nr_bins");
		e13.setTextContent(String.valueOf(sm_NumberOfBins.getIntValue()));
		node.appendChild(e13);
		// nr_threads
		Element e14 = node.getOwnerDocument().createElement("nr_threads");
		e14.setTextContent(String.valueOf(sm_NumberOfThreads.getIntValue()));
		node.appendChild(e14);

		// NOTE these are hard coded based on Cortana defaults (r1285)
		// alpha
		Element e15 = node.getOwnerDocument().createElement("alpha");
		e15.setTextContent("0.5");
		node.appendChild(e15);
		// beta
		Element e16 = node.getOwnerDocument().createElement("beta");
		e16.setTextContent("1.0");
		node.appendChild(e16);
		// post_processing_do_autorun
		Element e17 = node.getOwnerDocument().createElement("post_processing_do_autorun");
		e17.setTextContent("true");
		node.appendChild(e17);
		// post_processing_count
		Element e18 = node.getOwnerDocument().createElement("post_processing_count");
		e18.setTextContent("20");
		node.appendChild(e18);
	}

	private void addTableNode(Node experimentNode, String csvFile) {
		// table
		Node node = experimentNode.appendChild(experimentNode
							.getOwnerDocument()
							.createElement("table"));

		// TODO a .csv file will be created for which the url is known
		Element e = node.getOwnerDocument().createElement("table_name");
		e.setTextContent(tableSpec.getName().isEmpty() ? "NO NAME" : tableSpec.getName());
		node.appendChild(e);
		// source FIXME
		Element e2 = node.getOwnerDocument().createElement("source");
		e2.setTextContent(csvFile);
		node.appendChild(e2);

		for (int i = 0, j = tableSpec.getNumColumns(); i < j; ++i) {
			Node column = node.appendChild(experimentNode
								.getOwnerDocument()
								.createElement("column"));

			DataColumnSpec columnSpec = tableSpec.getColumnSpec(i);
			String cortanaType = mapType(columnSpec.getType());
			// type
			Element type = node.getOwnerDocument().createElement("type");
			type.setTextContent(cortanaType);
			column.appendChild(type);
			// name XXX quotes rely on FileWriterSettings setting in CortanaNodeModel.execute()
			Element name = node.getOwnerDocument().createElement("name");
			name.setTextContent("\"" + columnSpec.getName() + "\"");
//			name.setTextContent(columnSpec.getName().replaceAll("\t", "\\t"));
			column.appendChild(name);
			// short
			Element shortName = node.getOwnerDocument().createElement("short");
			shortName.setTextContent("");
			column.appendChild(shortName);
			// index
			Element index = node.getOwnerDocument().createElement("index");
			index.setTextContent(String.valueOf(i));
			column.appendChild(index);
			// missing_value
			// need to set missing values to some (Cortana) default "?", "0.0", "false"
			Element missing_value = node.getOwnerDocument().createElement("missing_value");
			missing_value.setTextContent(missingValue(cortanaType));
			column.appendChild(missing_value);
			// enabled XXX only enabled columns are present in the DataTableSpec
			Element enabled = node.getOwnerDocument().createElement("enabled");
			enabled.setTextContent("true");
			column.appendChild(enabled);
		}
	}

	private static String mapType(DataType dataType) {
		if (dataType.isCompatible(BooleanValue.class))
			return "binary";
		else if (dataType.isCompatible(DoubleValue.class))
			return "numeric";
		else
			return "nominal";
	}

	private static String missingValue(String type) {
		if ("binary".equals(type))
			return "0";
		else if ("numeric".equals(type))
			return "0.0";
		else
			return "?";
	}

	private static void saveDocument(Document theXMLDoc, File theOutputFile)
	{
		FileOutputStream aFileOutputStream = null;

		try {
			aFileOutputStream = new FileOutputStream(theOutputFile);
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty("doctype-system", theXMLDoc.getDoctype().getSystemId());
			t.setOutputProperty("indent", "yes");
			t.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
			t.transform(new DOMSource(theXMLDoc), new StreamResult(aFileOutputStream));
		} catch (Exception e) {
			e.printStackTrace();
//			new ErrorDialog(e, ErrorDialog.writeError);
		} finally {
			try {
				if (aFileOutputStream != null) {
					aFileOutputStream.flush();
					aFileOutputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
//				new ErrorDialog(e, ErrorDialog.fileOutputStreamError);
			}
		}
	}
}

