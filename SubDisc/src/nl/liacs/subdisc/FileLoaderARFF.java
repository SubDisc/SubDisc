package nl.liacs.subdisc;

import java.io.File;

public class FileLoaderARFF implements FileLoaderInterface
{
	private Table itsTable;
	private String itsSeparator = FileLoaderInterface.DEFAULT_SEPARATOR;

	@Override
	public Table loadFile(File theFile) throws Exception
	{
		return null;
	}

}
