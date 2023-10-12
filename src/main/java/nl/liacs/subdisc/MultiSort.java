package nl.liacs.subdisc;

import java.util.*;

import nl.liacs.subdisc.FileHandler.Action;

public final class MultiSort
{
	public final static int[] sort(Column... theColumns)
	{
		if (!isValid(theColumns))
			throw new IllegalArgumentException("Requires 2 or more non-null Columns of same size.");

		Integer[] sortedOrder = initOrder(theColumns[0].size());

		Arrays.sort(sortedOrder, new MultiColumnComparator(theColumns));

		return toIntArray(sortedOrder);
	}

	private static final boolean isValid(Column... theColumns)
	{
		if (theColumns == null)
			return false;

		int length = theColumns.length;
		if (length < 2)
			return false;

		for (int i = 0; i < length; ++i)
			if (theColumns[i] == null)
				return false;

		int size = theColumns[0].size();
		for (int i = 1; i < length; ++i)
			if (theColumns[i].size() != size)
				return false;

		return true;
	}

	private static final Integer[] initOrder(int theSize)
	{
		Integer[] ia = new Integer[theSize];
		for (int i = 0; i < theSize; ++i)
			ia[i] = i;
		return ia;
	}

	private static final int[] toIntArray(Integer[] theIntegers)
	{
		int length = theIntegers.length;
		int[] ia = new int[length];
		for (int i = 0; i < length; ++i)
			ia[i] = theIntegers[i];
		return ia;
	}

	private static final class MultiColumnComparator implements Comparator<Integer>
	{
		private final Column[] itsColumns;

		MultiColumnComparator(Column[] theColumns)
		{
			itsColumns = theColumns;
		}

		@Override
		public int compare(Integer arg0, Integer arg1)
		{
			int i0 = arg0;
			int i1 = arg1;
			if (i0 == i1)
				return 0;

			// if data for first column is equal, continue with next
			for (int i = 0; i < itsColumns.length; ++i)
			{
				int cmp = compare(itsColumns[i], i0, i1);
				if (cmp != 0)
					return cmp;
			}

			// identical data, return based on initial order (index)
			return i0-i1;
		}

		private static final int compare(Column theColumn, int arg0, int arg1)
		{
			switch (theColumn.getType())
			{
				case BINARY :
				{
					boolean b1 = theColumn.getBinary(arg0);
					boolean b2 = theColumn.getBinary(arg1);
					return (b1 ^ b2) ? (b1 ? 1 : -1) : 0;
				}
				case NUMERIC :
					return Float.compare(theColumn.getFloat(arg0), theColumn.getFloat(arg1));
				case NOMINAL :
					return theColumn.getNominal(arg0).compareTo(theColumn.getNominal(arg1));
				default :
					throw new AssertionError(theColumn.getType());
			}
		}
	}

	public static void main(String[] args)
	{
		FileHandler f = new FileHandler(Action.OPEN_FILE);
		Table t = f.getTable();
		t.update();
		Column[] ca = { t.getColumn(8), t.getColumn(9) };
		int[] ia = sort(ca);
		for (int i = 0; i < ia.length; ++i)
		{
			System.out.print(i);
			System.out.print("\t");
			System.out.print(ia[i]);
			for (int j = 0; j < ca.length; ++j)
			{
				System.out.print("\t");
				System.out.print(ca[j].getString(ia[i]));
			}
			System.out.println();
		}
		System.out.println(t.getColumn(9).getType());
	}
}
