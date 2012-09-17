package nl.liacs.subdisc.knime;

import org.knime.core.data.*;

public class NominalBinaryFilter extends SubgroupFilter {
	public NominalBinaryFilter(int columnIndex, String[] condition) {
		super(columnIndex, condition);
	}

	@Override
	public boolean matches(DataRow row) {
		DataCell cell = row.getCell(index);
		if (cell.isMissing())
			return false;

		// FIXME check boolean output Cortana
		// KNIME uses true/false, Cortana uses 0/1

		String s = cell.toString();
		if ("=".equals(operator))
			return value.equals(s);
		// TODO != operator still exist?
		else if ("!=".equals(operator))
			return !value.equals(s);
		else
			return false; // throw Exception
	}
}
