package nl.liacs.subdisc.postprocess;

import java.io.*;
import java.util.*;

public class MeanAggregateTable
{
	private final List<MeanAggregate> itsAggregates;

	MeanAggregateTable(List<MeanAggregate> theMeanAggregates)
	{
		itsAggregates = theMeanAggregates;
	}

	// duplicate code - see MeanTable.write(File)
	void write(File theFile)
	{
		try (BufferedWriter br = new BufferedWriter(new FileWriter(theFile)))
		{
			List<Mean> aMeans = new ArrayList<Mean>();
			for (MeanAggregate a : itsAggregates)
				aMeans.addAll(a.getMeans());

			// obtain distinct datasets and topks for indexes
			List<String> aDatasets = new ArrayList<String>(Mean.distinctDatasets(aMeans));
			List<String> aTopKs = new ArrayList<String>(Mean.distinctTopKs(aMeans));

			String s = MeanTable.getFormat();

			// write header
			Object[] oa = MeanTable.getColumns();
			br.write(MeanTable.COMMENT + String.format(s, oa));
			// write data for all means
			for (MeanAggregate a : itsAggregates)
			{
				// write Means for MeanAggregate (commented)
				for (Mean m : a.getMeans())
				{
					int i = 0;
					oa[i++] = m.itsFile;
					oa[i++] = Integer.toString(m.itsResultFileSize);
					oa[i++] = m.itsDataset;
					oa[i++] = Integer.toString(aDatasets.indexOf(m.itsDataset));
					oa[i++] = Integer.toString(m.itsDepth);
					oa[i++] = Integer.toString(m.itsTopK);
					oa[i++] = Integer.toString(aTopKs.indexOf(Integer.toString(m.itsTopK)));
					oa[i++] = m.itsStrategy;
					oa[i++] = Integer.toString(m.itsNrBins);
					oa[i++] = Double.toString(m.itsMean);

					br.write(MeanTable.COMMENT + String.format(s, oa));
				}

				// write aggregate - re-uses some values
				int i = 0;
				oa[i++] = "aggregate";
				oa[i++] = "";
				oa[i++] = "";
				oa[i] = "";
				oa[++i] = oa[i]; // re-use depth
				oa[++i] = oa[i]; // re-use topK
				oa[++i] = oa[i]; // re-use topKIndex
				oa[++i] = oa[i]; // re-use strategy
				oa[++i] = oa[i]; // re-use nrBins
				oa[++i] = Double.toString(a.getAggregate());

				br.write(String.format(s, oa));
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
}
