package nl.liacs.subdisc.knime;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

public abstract class SubgroupFilter extends RowFilter {
	final int index;
	final String operator;
	final String value;

	// TODO use NumericOperator based regular expression for splitting
	public SubgroupFilter(int columnIndex, String[] condition) {
		index = columnIndex;
		operator = condition[1];
		value = condition[2];
/*
		index = columnIndex;
		operator = condition.substring(0, condition.indexOf(" "));
		String tmp = condition.split("\\s+")[1];
		if (tmp.startsWith("'") && tmp.endsWith("'"))
			this.value = tmp.substring(1, tmp.length()-1);
		else
			this.value = tmp;
*/
		// DEBUG ONLY
		System.out.println("condition: '"  + condition[0] + "'");
		System.out.println("operator : '"  + operator + "'");
		System.out.println("value    : '"  + value + "'");
	}

	public abstract boolean matches(DataRow row);

	@Override
	public boolean matches(DataRow row, int rowIndex) throws EndOfTableException, IncludeFromNowOn {
		return this.matches(row);
	}

	// IGNORE BELOW
	@Override
	public void loadSettingsFrom(NodeSettingsRO cfg) throws InvalidSettingsException {
	}

	@Override
	protected void saveSettings(NodeSettingsWO cfg) {
	}

	@Override
	public DataTableSpec configure(DataTableSpec inSpec) throws InvalidSettingsException {
		return inSpec;
	}
}
