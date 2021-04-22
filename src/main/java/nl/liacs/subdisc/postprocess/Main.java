package nl.liacs.subdisc.postprocess;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import nl.liacs.subdisc.*;

public class Main
{
	private static final String MAIN = "/home/marvin/data/wrk/svn/SubgroupDiscovery/publications/BinningStrategies/";
	private static final String EXP = MAIN + "experiments/all_experiments_orig/";
	private static final String NOM_EXP = EXP + "nominal_target/results/";
	private static final String NUM_EXP = EXP + "numeric_target/results/";
	private static final String NOM_OUT = EXP + "nominal_target/summary/";
	private static final String NUM_OUT = EXP + "numeric_target/summary/";

	// column names of output tables
	private static final String COLUMN_NAME_DATASET = "dataset";
	private static final String COLUMN_NAME_DEPTH = "depth";
	private static final String COLUMN_NAME_TOP_K = "top-k";

	public static void main(String[] args)
	{
		boolean nominal = true;
		String exp = nominal ? NOM_EXP : NUM_EXP;
		String out = nominal ? NOM_OUT : NUM_OUT;

		String aPath = (args.length == 0) ? exp : args[0];

		List<Mean> aMeans = getMeans(aPath, new int[]{ 1, 10 , 100 });

		// use null for no relative computation (relative to self)
		Map<Mean, Mean> pMap = pivot(aMeans, "dhea", 0);
		Map<Map.Entry<Mean, Mean>, Double> rMap = relative(pMap);

		Table aTable = toTable(rMap);

//		MeanTable aMeanTable = new MeanTable(aMeans);
//		aMeanTable.write(getFile(out + "mean-table-long-"));
//
//		List<MeanAggregate> anAggregates = aMeanTable.getAggregate();
//		MeanAggregateTable aMeanAggregateTable = new MeanAggregateTable(anAggregates);
//		aMeanAggregateTable.write(getFile(out + "mean-table-long-aggregate-"));

		
//
//		Table aTable = getTable(aMeans);
//		aTable.toFile(getFile(out + aTable.getName() + "-"));
//
//		Table anAggregate = aggregate(aTable);
//		anAggregate.toFile(getFile(out + anAggregate.getName() + "-"));
	}

	private static final List<Mean> getMeans(String thePath, int[] theTopKs)
	{
		ResultFile[] ra = Parser.getResults(new File(thePath));

		List<Mean> aMeans = new ArrayList<Mean>(ra.length * theTopKs.length);

		for (ResultFile r : ra)
			for (int topK : theTopKs)
				aMeans.add(new Mean(r, topK));

		return aMeans;
	}

	private static final File getFile(String theDir)
	{
		return new File(theDir + System.currentTimeMillis() + ".csv");
	}

	private static final Table getTable(List<Mean> theMeans)
	{
		// determine sorted distinct datasets, depths, topKs, strategies
		List<List<String>> aDistincts = getDistincts(theMeans);

		Table aTable = createTable(aDistincts.get(STRATEGY_INDEX));

		fillTable(theMeans, aDistincts, aTable);

		aTable.update();

		return aTable;
	}

	// hack
	private static final int DATASET_INDEX = 0;
	private static final int DEPTH_INDEX = 1;
	private static final int TOP_K_INDEX = 2;
	private static final int STRATEGY_INDEX = 3;
	private static final List<List<String>> getDistincts(List<Mean> theMeans)
	{
		// Integer sets are order numerically
		Set<String> aDatasets = new TreeSet<String>();
		Set<Integer> aDepths = new TreeSet<Integer>();
		Set<Integer> aTopKs = new TreeSet<Integer>();
		Set<String> aStrategies = new TreeSet<String>();

		for (Mean m : theMeans)
		{
			aDatasets.add(m.itsDataset);
			aDepths.add(m.itsDepth);
			aTopKs.add(m.itsTopK);
			aStrategies.add(getStrategy(m));
		}

		List<List<String>> aLists = new ArrayList<List<String>>();
		aLists.add(DATASET_INDEX, new ArrayList<String>(aDatasets));
		aLists.add(DEPTH_INDEX, getStringList(aDepths));
		aLists.add(TOP_K_INDEX, getStringList(aTopKs));
		aLists.add(STRATEGY_INDEX, new ArrayList<String>(aStrategies));

		return aLists;
	}

	private static final String getStrategy(Mean theMean)
	{
		return String.format("%s%04d", theMean.itsStrategy, theMean.itsNrBins);
	}

	private static final List<String> getStringList(Set<Integer> theSet)
	{
		List<String> aList = new ArrayList<String>(theSet.size());

		for (Integer i : theSet)
			aList.add(Integer.toString(i));

		return aList;
	}

	private static final Table createTable(List<String> theStrategies)
	{
		String aName = "mean-table-wide";
		Table aTable = new Table(new File(aName), aName);
		List<Column> aColumns = aTable.getColumns();
		int i = 0;

		// these columns are always created
		aColumns.add(createColumn(COLUMN_NAME_DATASET, AttributeType.NOMINAL, i++));
		aColumns.add(createColumn(COLUMN_NAME_DEPTH, AttributeType.NOMINAL, i++));
		aColumns.add(createColumn(COLUMN_NAME_TOP_K, AttributeType.NOMINAL, i++));

		// each distinct strategy serves as a column
		// NOMINAL allows non-numeric / missing values (like 'R=22')
		for (String s : theStrategies)
			aColumns.add(createColumn(s, AttributeType.NOMINAL, i++));

		return aTable;
	}

	private static final Column createColumn(String theName, AttributeType theType, int theIndex)
	{
		return new Column(theName, theName, theType, theIndex, 0);
	}

	private static final void fillTable(List<Mean> theMeans, List<List<String>> theDistincts, Table theTable)
	{
		// setup storage for each dataset-depth-topK 
		// NOTE do not rely on data order of List<Mean> theMeans
		Map<String, Map<Integer, Map<Integer, List<Mean>>>> aDatasets = new TreeMap<String, Map<Integer, Map<Integer, List<Mean>>>>();
		for (String aDataset : theDistincts.get(DATASET_INDEX))
		{
			Map<Integer, Map<Integer, List<Mean>>> aDepths = new TreeMap<Integer, Map<Integer, List<Mean>>>();
			aDatasets.put(aDataset, aDepths);

			for (String aDepth : theDistincts.get(DEPTH_INDEX))
			{
				Map<Integer, List<Mean>> aTopKs = new TreeMap<Integer, List<Mean>>();
				aDepths.put(Integer.valueOf(aDepth), aTopKs);

				for (String aTopK : theDistincts.get(TOP_K_INDEX))
					aTopKs.put(Integer.valueOf(aTopK), new ArrayList<Mean>());
			}
		}

		// determine dataset-depth-topK for each Mean
		for (Mean m : theMeans)
			aDatasets.get(m.itsDataset).get(m.itsDepth).get(m.itsTopK).add(m);

		// invariable
		int aNrColumns = theTable.getColumns().size();
		Column d = theTable.getColumn(COLUMN_NAME_DATASET);
		Column e = theTable.getColumn(COLUMN_NAME_DEPTH);
		Column t = theTable.getColumn(COLUMN_NAME_TOP_K);

		// fill in Table in correct order
		for (Entry<String, Map<Integer, Map<Integer, List<Mean>>>> ds : aDatasets.entrySet())
		{
			for (Entry<Integer, Map<Integer, List<Mean>>> de : ds.getValue().entrySet())
			{
				for (Entry<Integer, List<Mean>> te : de.getValue().entrySet())
				{
					// write out row
					d.add(ds.getKey());
					e.add(de.getKey().toString());
					t.add(te.getKey().toString());
					// this loop assumes that an add will be
					// performed for every Column
					// if not, row will be invalid
					int adds = 3;
					for (Mean m : te.getValue())
					{
						String s;
						if (Double.isNaN(m.itsMean))
							s = "R=" + m.itsResultFileSize;
						else
							s = Double.toString(m.itsMean);

						theTable.getColumn(getStrategy(m)).add(s);
						++adds;
					}

					if (adds != aNrColumns)
						throw new AssertionError();
				}
			}
		}
	}

	private static final List<Set<Mean>> group(List<Mean> theMeans)
	{
		List<Set<Mean>> aList = new ArrayList<Set<Mean>>();

		OUTER:
		for (Mean m : theMeans)
		{
			for (Set<Mean> set : aList)
			{
				// next() is safe, sets are non-empty by design
				if (set.iterator().next().canAggregate(m))
				{
					set.add(m);
					continue OUTER;
				}
			}

			// no comparable Means found, create new set
			Set<Mean> aSet = new LinkedHashSet<Mean>();
			aSet.add(m);
			aList.add(aSet);
		}

		return aList;
	}

	private static final Map<Mean, Mean> pivot(List<Mean> theMeans, String thePivotStrategy, int thePivotNrBins)
	{
		Map<Mean, Mean> aMap = new LinkedHashMap<Mean, Mean>();

		// special case
		if (thePivotStrategy == null)
		{
			for (Mean m : theMeans)
				aMap.put(m, m);

			return aMap;
		}

		Set<Mean> thePivots = filterMeans(theMeans, thePivotStrategy, thePivotNrBins);

		OUTER:
		for (Mean m : theMeans)
		{
			for (Mean n : thePivots)
			{
				if (pivotCompatible(m, n))
				{
					aMap.put(m, n);
					continue OUTER;
				}
			}

			throw new AssertionError();
		}

		return aMap;
	}

	private static final Set<Mean> filterMeans(List<Mean> theMeans, String theStrategy, int theNrBins)
	{
		Set<Mean> aMeans = new HashSet<Mean>();

		for (Mean m : theMeans)
			if ((m.itsStrategy.equals(theStrategy)) && (m.itsNrBins == theNrBins))
				aMeans.add(m);

		return aMeans;
	}

	private static final boolean pivotCompatible(Mean theMean, Mean thePivotMean)
	{
		return ((theMean.itsDataset.equals(thePivotMean.itsDataset)) &&
			(theMean.itsDepth == thePivotMean.itsDepth) &&
			(theMean.itsTopK == thePivotMean.itsTopK));
	}

	private static final Map<Map.Entry<Mean, Mean>, Double> relative(Map<Mean, Mean> thePivotMap)
	{
		Map<Map.Entry<Mean, Mean>, Double> aMap = new LinkedHashMap<Map.Entry<Mean, Mean>, Double>();

		for (Map.Entry<Mean, Mean> e : thePivotMap.entrySet())
			aMap.put(e, e.getKey().itsMean / e.getValue().itsMean);

		return aMap;
	}

	private static final void toTable(Map<Map.Entry<Mean, Mean>, Double> theMap)
	{
		for (Map.Entry<Map.Entry<Mean, Mean>, Double> e : theMap.entrySet())
		{
			
		}
	}

	private static final Table aggregate(Table theTable)
	{
		// new Table is like old
		String aName = theTable.getName() + "-aggregate";
		Table aTable = new Table(new File(aName), aName);
		List<Column> aColumns = aTable.getColumns();
		for (Column c : theTable.getColumns())
			aColumns.add(createColumn(c.getName(), c.getType(), c.getIndex()));

		int aNrRows = theTable.getNrRows();
		int aNrColumns = theTable.getNrColumns();

		Column aDepths = theTable.getColumn(COLUMN_NAME_DEPTH);
		Column aTopKs = theTable.getColumn(COLUMN_NAME_TOP_K);
		// not efficient, but Tables are small so no problem
		for (String d : aDepths.getDomain())
		{
			for (String t : aTopKs.getDomain())
			{
				double[] sum = new double[aNrColumns];
				double[] div = new double[aNrColumns];
	
				for (int i = 0; i < aNrRows; ++i)
				{
					if (!d.equals(aDepths.getNominal(i)))
						continue;
					if (!t.equals(aTopKs.getNominal(i)))
						continue;

					// copy this record into new Table
					for (int j = 0; j < aNrColumns; ++j)
					{
						// uses same names / indexes
						Column o = theTable.getColumn(j);
						Column n = aTable.getColumn(j);

						String v = o.getNominal(i);

						if (COLUMN_NAME_DATASET.equals(o.getName()))
						{
							n.add(MeanTable.COMMENT + v);
							continue;
						}

						if (!v.startsWith("R="))
						{
							sum[j] += Double.parseDouble(v);
							div[j]++;
						}

						n.add(v);
					}
				}
	
				// add aggregate row
				for (int j = 0; j < aNrColumns; ++j)
				{
					Column aColumn = aTable.getColumn(j);
					String aColumnName = aColumn.getName();

					String aValue;
					if (COLUMN_NAME_DATASET.equals(aColumnName))
						aValue = "aggregate";
					else if (COLUMN_NAME_DEPTH.equals(aColumnName))
						aValue = d;
					else if (COLUMN_NAME_TOP_K.equals(aColumnName))
						aValue = t;
					else
						aValue = Double.toString(sum[j] / div[j]);

					aColumn.add(aValue);
				}
			}
		}

		aTable.update();

		return aTable;
	}
}
