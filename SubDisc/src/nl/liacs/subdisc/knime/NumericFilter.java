package nl.liacs.subdisc.knime;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;

public class NumericFilter extends SubgroupFilter {
	public NumericFilter(int columnIndex, String[] condition) {
		super(columnIndex, condition);
	}

	@Override
	public boolean matches(DataRow row) {
		DataCell cell = row.getCell(index);
		// FIXME NaN comparisons always return false
		if (cell instanceof DoubleValue) {
			DoubleValue name = (DoubleValue) cell;
			double d = name.getDoubleValue();
			double v = Double.parseDouble(value);

			if ("<=".equals(operator))
				return d <= v;
			else if (">=".equals(operator))
				return d >= v;
			else if ("=".equals(operator))
				return d == v;
//			else if ("!=".equals(op))
//				return d != value;
//			else if ("in".equals(op))
//				return d >= value;
			else
				return false;
		}
		// else is MissingCell
		else
			return false;
	}
}
