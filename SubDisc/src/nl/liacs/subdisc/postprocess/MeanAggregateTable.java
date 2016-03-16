package nl.liacs.subdisc.postprocess;

import java.io.*;
import java.util.*;

public class MeanAggregateTable
{
	public static final String COMMENT = "#";

	private final List<MeanAggregate> itsAggregates;

	MeanAggregateTable(List<MeanAggregate> theMeanAggregates)
	{
		itsAggregates = theMeanAggregates;
	}

	void write(File theFile)
	{
		try (BufferedWriter br = new BufferedWriter(new FileWriter(theFile)))
		{
			String s = MeanTable.getFormat();

			// write header
			Object[] oa = MeanTable.getColumns();
			br.write(String.format(s, oa));
			// write data for all means
			for (MeanAggregate a : itsAggregates)
			{
				// write Means for MeanAggregate (commented)
				for (Mean m : a.getMeans())
				{
					oa[0] = m.itsFile;
					oa[1] = Integer.toString(m.itsResultFileSize);
					oa[2] = m.itsDataset;
					oa[3] = Integer.toString(m.itsDepth);
					oa[4] = Integer.toString(m.itsTopK);
					oa[5] = m.itsStrategy;
					oa[6] = Integer.toString(m.itsNrBins);
					oa[7] = Double.toString(m.itsMean);

					br.write(COMMENT + String.format(s, oa));
				}

				// write aggregate - re-uses some values
				oa[0] = "aggregate";
				oa[1] = "";
				oa[2] = "";
				//oa[3] = oa[3]; // re-use depth
				//oa[4] = oa[4]; // re-use topK
				//oa[5] = oa[5]; // re-use strategy
				//oa[6] = oa[6]; // re-use nrBins
				oa[7] = Double.toString(a.getAggregate());

				br.write(String.format(s, oa));
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
}
