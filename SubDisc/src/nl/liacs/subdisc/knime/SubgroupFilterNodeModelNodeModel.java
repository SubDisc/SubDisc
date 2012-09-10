package nl.liacs.subdisc.knime;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.preproc.filter.row.RowFilterTable;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of SubgroupFilterNodeModel.
 * Subgroup Filter for Cortana results.
 *
 * @author Marvin Meeng
 */
public class SubgroupFilterNodeModelNodeModel extends NodeModel {
    
    /**
     * Constructor for the node model.
     */
    protected SubgroupFilterNodeModelNodeModel() {
    
        // TODO: Specify the amount of input and output ports needed.
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
	    BufferedDataTable resultTable = inData[1];
	    int numColumns = resultTable.getDataTableSpec().getNumColumns();
	    String[] rules = new String[resultTable.getRowCount()];
	    int i = 0;
	    for (DataRow dataRow : resultTable) {
		    rules[i++] = dataRow.getCell(numColumns-1).toString();
	    }

	    SubgroupRowFilter filter = new SubgroupRowFilter(inData[0].getDataTableSpec(), rules);
	    RowFilterTable rowFilterTable = new RowFilterTable(inData[0], filter);
	    BufferedDataTable result = exec.createBufferedDataTable(rowFilterTable, exec);


        return new BufferedDataTable[]{ result };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[] { inSpecs[0] };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }

}

