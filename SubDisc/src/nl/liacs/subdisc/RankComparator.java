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
	private static BufferedWriter OUT;

	/**
	 * Parses standard Cortana result files, listing the rank for each
	 * subgroup description in each of the result files.
	 *
	 * @param args The paths of the files to compare.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		OUT = new BufferedWriter(new FileWriter(System.nanoTime() + ".csv"));
//		args = new String[] {
//				"/home/marvin/data/wrk/svn/SubgroupDiscovery/publications/MultiNumeric/ecmlpkdd2015_journal/experiments/discretisation_loop_runs_NEW_KDE_SG_VS_COMPLEMENT/waterqual_all_targets/waterqual.arff_260804118589660_EH_00021.txt",
//				"/home/marvin/data/wrk/svn/SubgroupDiscovery/publications/MultiNumeric/ecmlpkdd2015_journal/experiments/discretisation_loop_runs_NEW_KDE_SG_VS_COMPLEMENT/waterqual_all_targets/waterqual.arff_260804118589660_ORIG.txt",
//		};
		foo(args);
		//temporaryCode("/home/marvin/data/wrk/svn/SubgroupDiscovery/SubDisc/0_MULTI_NUMERIC_2015/boston_housing_kde_vs_eh/d1/");
		OUT.close();
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
		out(COMMENT + "condition" + DELIMITER + "size");
		for (File f : files)
		{
			out(DELIMITER);
			out(f.getName());
		}
		out("\n");
		// conditions + corresponding ranks in input files
		for (Entry<String, int[]> e : map.entrySet())
		{
			out(e.getKey());
			for (int i : e.getValue())
			{
				out(DELIMITER);
				out(Integer.toString(i));
			}
			out("\n");
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
				out("\n");
				out(COMMENT + files[j] + "\n");
				out(COMMENT + s + "\n");
				out(COMMENT + tau(rankings[j], rankings[i]) + "\n");
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

	static final void temporaryCode(String theDirectory)
	{
		// foo
		File aDir = new File(theDirectory);
		List<File> aList = new ArrayList<File>(Arrays.asList(aDir.listFiles()));
		for (int i = aList.size()-1; i >= 0; --i)
			if (!aList.get(i).getName().endsWith(".txt"))
				aList.remove(i);
 
		// sort of helpful helps but not entirely
		Collections.sort(aList);

		// listFiles could use a FileName(Extension)Filter
		Map<String, int[]> aMap = new TreeMap<String, int[]>();
		for (int i = 0; i < aList.size(); ++i)
			bar(aList.get(i), i, aList.size(), aMap);

		print(aList.toArray(new File[0]), aMap);

		// special tau
		int[][] rankings = createRankings(aMap.values());

		int origFile = -1;
		for (int i = 0; i < aList.size(); ++i)
		{
			if (aList.get(i).getName().contains("_ORIG.txt"))
			{
				origFile = i;
				break;
			}
		}
		if (origFile == -1)
			throw new IllegalArgumentException("NO *_ORIG.txt file found");
		String origName = aList.get(origFile).getName();
		int[] origRanking = rankings[origFile];

		for (int i = 0; i < aList.size(); ++i)
		{
			if (i == origFile)
				continue;
			out("\n");
			out(COMMENT + aList.get(i).getName() + "\n");
			out(COMMENT + origName + "\n");
			out(COMMENT + tau(rankings[i], origRanking) + "\n");
		}
	}

	// lazy
	private static final void out(String s)
	{
		try { OUT.write(s);}
		catch (IOException e) {}
	}
}
