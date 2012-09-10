package nl.liacs.subdisc.knime;

import org.knime.base.node.preproc.filter.row.rowfilter.AndRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

public class SubgroupRowFilter extends RowFilter {
private final RowFilter[] filters;

	public SubgroupRowFilter(DataTableSpec tableSpec, String[] rules) throws Exception {
		this.filters = new RowFilter[rules.length];
		for (int i = 0; i < rules.length; i++) {
			String conjunction = rules[i];
			String[] conjuncts = getConjuncts(conjunction);

			// XXX could also be obtained from depth-column
			int k = conjuncts.length;
			RowFilter[] subFilters = new RowFilter[k];
			for (int m = 0; m < k; ++m) {
				String[] sa = disect(conjuncts[m]);
				int index = tableSpec.findColumnIndex(sa[0]);
				if (index < 0)
					throw new Exception("Column not found.");

				DataType type = tableSpec.getColumnSpec(index).getType();
				// collectionType -> type.getElementType
				// boolean compatible with double so check it first
				if (type.isCompatible(BooleanValue.class))
					subFilters[m] = new NominalBinaryFilter(index, sa);
				else if (type.isCompatible(DoubleValue.class))
					subFilters[m] = new NumericFilter(index, sa);
				else
					subFilters[m] = new NominalBinaryFilter(index, sa);
			}

			// combine the filters for a single rule
			//filters[i] = subFilters[0]; // for 1-conjunct rule
			// else more filter
			filters[i] = subFilters[0];
			for (int m = 1; m < k; ++m) {
				filters[i] = new AndRowFilter(filters[i], subFilters[m]);
			}
/*
			String col = string.substring(1, string.indexOf("\"", 1));
			int index = tableSpec.findColumnIndex(col);
			if (index < 0)
				throw new Exception("Column not found.");
			DataType type = tableSpec.getColumnSpec(index).getType();
			// collectionType -> type.getElementType
			// boolean compatible with double so check it first
			if (type.isCompatible(BooleanValue.class))
				filters[i] = new NominalBinaryFilter(index, string.substring(col.length()+2).trim());
			else if (type.isCompatible(DoubleValue.class))
				filters[i] = new NumericFilter(index, string.substring(col.length()+2).trim());
			else
				filters[i] = new NominalBinaryFilter(index, string.substring(col.length()+2).trim());
*/
		}
	}

	// Extremely inefficient, but KNIME works on rows, not columns
	@Override
	public boolean matches(DataRow row, int rowIndex) throws EndOfTableException, IncludeFromNowOn {
		for (RowFilter s : filters) {
			if (!s.matches(row, rowIndex))
				return false;
		}
		return true;
	}

	private static String[] getConjuncts(String conjunction) {
		// assume ' AND ' does not appear in column names
		return conjunction.split(" AND ", -1);
	}

	private static final String[] OPERATORS = { " = ", " != ", " <= ", " >= ", " in " };
	private static String[] disect(String condition) {
		// assume OPERATORS do not appear in column name
		for (String s : OPERATORS) {
			if (condition.contains(s)) {
				final String[] tmp = condition.split(s);
				// remove outer quotes from column name
//				tmp[0] = tmp[0].substring(1, tmp[0].length()-1);
				if (tmp[1].startsWith("'") && tmp[1].startsWith("'"))
					tmp[1] = tmp[1].substring(1, tmp[1].length());
				return new String[] { tmp[0] , s.trim(), tmp[1] };
			}
		}
		return null; // throw Exception
	}

	/*
	 * IGNORE the methods below
	 */
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
