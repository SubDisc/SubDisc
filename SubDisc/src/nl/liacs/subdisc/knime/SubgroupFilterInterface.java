package nl.liacs.subdisc.knime;

import org.knime.core.data.DataRow;

public interface SubgroupFilterInterface {

	public abstract boolean matches(DataRow row);

}