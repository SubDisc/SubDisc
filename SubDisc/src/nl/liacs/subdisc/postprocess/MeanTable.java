package nl.liacs.subdisc.postprocess;

import java.io.*;
import java.util.*;

public class MeanTable
{
	public static final String COMMENT = "#";

	private static final String[] COLUMNS =
	{
		"result-file",
		"result-file-size",
		"dataset",
		"datasetIndex",
		"depth",
		"topK",
		"topKIndex",
		"strategy",
		"nr-bins",
		"mean"
	};
	private static final String FMT = getFormatString(COLUMNS.length, "\t", "\n");

	private final List<Mean> itsMeans;

	MeanTable(List<Mean> theMeans)
	{
		itsMeans = theMeans;
	}

	void write(File theFile)
	{
		try (BufferedWriter br = new BufferedWriter(new FileWriter(theFile)))
		{
			// obtain distinct datasets and topks for indexes
			List<String> aDatasets = new ArrayList<String>(Mean.distinctDatasets(itsMeans));
			List<String> aTopKs = new ArrayList<String>(Mean.distinctTopKs(itsMeans));

			Object[] oa = new Object[COLUMNS.length];

			// write header
			br.write(COMMENT + String.format(FMT, (Object[]) COLUMNS));
			// write data for all means
			for (Mean m : itsMeans)
			{
				int i = 0;
				oa[i++] = m.itsFile;
				oa[i++] = m.itsResultFileSize;
				oa[i++] = m.itsDataset;
				oa[i++] = aDatasets.indexOf(m.itsDataset);
				oa[i++] = m.itsDepth;
				oa[i++] = m.itsTopK;
				oa[i++] = aTopKs.indexOf(Integer.toString(m.itsTopK));
				oa[i++] = m.itsStrategy;
				oa[i++] = m.itsNrBins;
				oa[i++] = m.itsMean;

				br.write(String.format(FMT, oa));
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	List<MeanAggregate> getAggregate()
	{
		List<MeanAggregate> aggregates = new ArrayList<MeanAggregate>();

		// crude method
		BitSet used = new BitSet(itsMeans.size());
		for (int i = 0; i < itsMeans.size(); ++i)
		{
			if (used.get(i))
				continue;

			List<Mean> similar = new ArrayList<Mean>();

			Mean m = itsMeans.get(i);
			used.set(i);
			similar.add(m);

			for (int j = i+1; j < itsMeans.size(); ++j)
			{
				Mean n = itsMeans.get(j);
				if (m.canAggregate(n))
				{
					used.set(j);
					similar.add(n);
				}
			}

			aggregates.add(new MeanAggregate(similar));
		}

		return aggregates;
	}

	static String getFormat() { return FMT; };
	static String[] getColumns() { return Arrays.copyOf(COLUMNS, COLUMNS.length); };

	private static final String getFormatString(int nrFields, String delimiter, String lineEnd)
	{
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < nrFields-1; ++i)
			sb.append("%s").append(delimiter);
		sb.append("%s").append(lineEnd);

		return sb.toString();
	}
}
