package nl.liacs.subdisc;

import java.io.File;

public interface FileLoaderInterface
{
	public static final String DEFAULT_SEPARATOR = ",";

	// TODO use different separator in FileLoaders?
//	void setSeparator(String theNewSeparator);

	Table loadFile(File theFile) throws Exception;
}
