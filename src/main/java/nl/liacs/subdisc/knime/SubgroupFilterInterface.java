package nl.liacs.subdisc.knime;

import org.knime.core.data.*;

public interface SubgroupFilterInterface {

	public abstract boolean matches(DataRow row);

}