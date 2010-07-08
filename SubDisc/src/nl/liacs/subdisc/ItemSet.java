package nl.liacs.subdisc;

import java.util.BitSet;

public class ItemSet extends BitSet
{
	private static final long serialVersionUID = 1L;
	private int itsDimensions;

	//empty itemset
	public ItemSet(int theDimensions)
	{
		super(theDimensions);
		itsDimensions = theDimensions;
	}

	//itemset with first theCount items set.
	public ItemSet(int theDimensions, int theCount)
	{
		super(theDimensions);
		itsDimensions = theDimensions;

		if (theCount>itsDimensions)
			set(0, itsDimensions);
		else
			set(0, theCount);
	}

	public int getDimensions()
	{
		return itsDimensions;
	}

	public int getItemCount()
	{
		int aCount = 0;

		for (int i=0; i<itsDimensions; i++)
		{
			if (get(i))
				aCount++;
		}
		return aCount;
	}

	public int getItem(int theIndex)
	{
		int aCount = 0;

		for (int i=0; i<itsDimensions; i++)
		{
			if (get(i))
			{
				aCount++;
				if (aCount == theIndex)
					return i;
			}
		}
		return -1;
	}

	public ItemSet symmetricDifference(ItemSet theSet)
	{
		ItemSet aSet = new ItemSet(itsDimensions);

		for (int i=0; i<itsDimensions; i++)
			if (get(i) ^ theSet.get(i))
				aSet.set(i);
		return aSet;
	}

	public ItemSet getExtension(int theIndex)
	{
		ItemSet aSet = (ItemSet) clone();
		aSet.set(theIndex);
		return aSet;
	}

	public ItemSet getNextItemSet()
	{
		int aCount = 0;
		int aLast = 0;
		boolean aFound = false;

		//find last occurence of ...10...
		for (int i=itsDimensions-1; i>0; i--)
		{
			if (get(i))
				aCount++;
			if (!get(i) && get(i-1))
			{
				aLast = i-1;
				aFound = true;
				break;
			}
		}
		if (!aFound)//last itemset
			return null;

		//create new itemset
		ItemSet aSet = new ItemSet(itsDimensions);
		for (int i=0; i<itsDimensions; i++)
		{
			//copy, or..
			if ((i<aLast) && get(i))
				aSet.set(i);
			//add new
			if ((i>aLast) && (aCount>=0))
			{
				aSet.set(i);
				aCount--;
			}
		}

		return aSet;
	}

	public boolean isFresh(int l)
	{
		int aCount = 0;
		boolean aStart = false;

		for (int i=itsDimensions-1; i>0; i--)
		{
			if (aStart == true)
			{
				if (!get(i) && (aCount < l+1))
					return false;
			}
			if (get(i))
			{
				aStart = true;
				aCount++;
			}
		}
		return true;
	}

	//skip all itemsets with same first l items, and proceed with next
	public ItemSet skipItemSets(int l)
	{
		int aCount = 0;
		int aLast = 0;

		//find lth itemset
		for (int i=0; i<itsDimensions; i++)
		{
			if (get(i))
				aCount++;
			if (aCount == l)
			{
				aLast = i;
				break;
			}
		}

		//create new itemset
		ItemSet aSet = new ItemSet(itsDimensions);
		for (int i=0; i<itsDimensions; i++)
		{
			//copy, or..
			if ((i<=aLast) && get(i))
				aSet.set(i);
			//add new
			if ((i == itsDimensions+aCount-getItemCount()) && (aCount<getItemCount()))
			{
				aSet.set(i);
				aCount++;
			}
		}

		if (aCount < getItemCount())//skipping to the end
			return null;
		else
			return aSet;
	}
}
