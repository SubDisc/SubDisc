package nl.liacs.subdisc;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class RankComparator
{
	private static final int RESULT_NUMBER_OF_COLUMNS = 8;
	private static final int RANK_INDEX = 0;
	private static final int COVERAGE_INDEX = 2;
	private static final int DESCRIPTION_INDEX = RESULT_NUMBER_OF_COLUMNS-1;
	private static final String COMMENT = "#";

	/**
	 * Parses standard Cortana result files, listing the rank for each
	 * subgroup description in each of the result files.
	 *
	 * @param args The paths of the files to compare.
	 */
	public static void main(String[] args)
	{
		foo(args);
	}

	private static final void foo(String[] files)
	{
		File[] fa = new File[files.length];
		for (int i = 0; i < fa.length; ++i)
			fa[i] = new File(files[i]);
		foo(fa);
	}

	private static final void foo(File[] files)
	{
		Map<String, int[]> map = new TreeMap<String, int[]>();
		for (int i = 0; i < files.length; ++i)
			bar(files[i], i, files.length, map);

		print(files, map);
		tau(files, map);
	}

	private static final void bar(File file, int index, int nrFiles, Map<String, int[]> map)
	{
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new FileReader(file));
			String aLine;
			String[] sa;
			String key;
			// skip header
			br.readLine();
			while ((aLine = br.readLine()) != null)
			{
				if (aLine.isEmpty())
					continue;
				// could be done by using line counter
				sa = aLine.split("\t", -1);
				if (sa.length != RESULT_NUMBER_OF_COLUMNS)
					throw new IllegalArgumentException("ERROR NUMBER OF COLUMNS ON LINE: " + sa.length);

				// FIXME MM - COVERAGE HACK
				key = (sa[DESCRIPTION_INDEX] + DELIMITER + sa[COVERAGE_INDEX]);
				int[] ia = map.get(key);

				if (ia == null)
				{
					ia = new int[nrFiles];
					map.put(key, ia);
				}

				ia[index] = Integer.parseInt(sa[RANK_INDEX]);
			}
		}
		catch (IOException e)
		{
			// TODO MM
		}
		finally
		{
			if (br != null)
			{
				try
				{
					br.close();
				}
				catch(IOException e)
				{
					// TODO MM
				}
			}
		}
	}

	// TODO MM sort on rank of first file, so using int[0]
	private static final String DELIMITER = "\t";
	private static final void print(File[] files, Map<String, int[]> map)
	{
		// header
		System.out.print(COMMENT + "condition" + DELIMITER + "size");
		for (File f : files)
		{
			System.out.print(DELIMITER);
			System.out.print(f.getName());
		}
		System.out.println();
		// conditions + corresponding ranks in input files
		for (Entry<String, int[]> e : map.entrySet())
		{
			System.out.print(e.getKey());
			for (int i : e.getValue())
			{
				System.out.print(DELIMITER);
				System.out.print(i);
			}
			System.out.println();
		}
	}

	private static final void tau(File[] files, Map<String, int[]> map)
	{
		int[][] rankings = createRankings(map.values());

		for (int i = 1; i < files.length; ++i)
		{
			String s = files[i].getName();
			for (int j = 0; j < i; ++j)
			{
				System.out.println();
				System.out.println(COMMENT + files[j]);
				System.out.println(COMMENT + s);
				System.out.println(COMMENT + tau(rankings[j], rankings[i]));
			}
		}
	}

	private static final int[][] createRankings(Collection<int[]> input)
	{
		int r = input.iterator().next().length;
		int n = input.size();
		int[][] rankings = new int[r][n];

		int row = -1;
		for (Iterator<int[]> i = input.iterator(); i.hasNext(); )
		{
			++row;
			int[] ia = i.next();
			for (int j = 0; j < r; ++j)
				rankings[j][row] = ia[j];
		}

		return rankings;
	}

	// simple O(n^2) version - O(n*log(n)) exists
	// no checks what so ever - assumes no ties in rankings
	// fails on rankings > 50.000 (d*d-1 overflows)
	private static final double tau(int[] x, int[] y)
	{
		int d = x.length;

		long n = 0;
		for (int i = 1; i < d; ++i)
			for (int j = 0; j < i; ++j)
				n += ( Math.signum(x[i]-x[j]) * Math.signum(y[i]-y[j]) );

		return n / ((d * (d-1)) / 2.0);
	}
}
