package nl.liacs.subdisc;

import java.util.*;

// FIXME @author, this class redefines default BitSet methods, why?
// FIXME favour encapsulation over extension
//       this class allows a number of bugs, mostly related to this extends
//       there is no way to guarantee that super.cardinality() == getItemCount()
public class ItemSet extends BitSet
{
	private static final long serialVersionUID = 1L;
	// TODO make itsDimensions final
	private int itsDimensions;
	private double itsJointEntropy = Double.NaN;

	//empty itemset
	public ItemSet(int theDimensions)
	{
		super(theDimensions); // itsCardinality = 0; itsBitSet = new BitSet(theDimensions);
		itsDimensions = theDimensions;
	}

	//itemset with first theCount items set.
	// FIXME why is no IllegalArgumentException thrown when count > dimensions
	public ItemSet(int theDimensions, int theCount)
	{
		super(theDimensions); // itsBitSet = new BitSet(theDimensions);
		itsDimensions = theDimensions;

		// itsCardinality = Math.min(itsDimensions, theCount);
		// itsBitSet.set(0, itsCardinality)
		if (theCount>itsDimensions)
			set(0, itsDimensions);
		else
			set(0, theCount);
	}

// FIXME LEAVE IN - THIS CLASS WILL BE UPDATED TO MAKE IT MORE SAFE AND FASTER
//	private final BitSet itsBitSet;
//	private int itsCardinality = -1;
//
//	final boolean get(int theIndex)
//	{
//		checkIndex(theIndex, itsDimensions, "get");
//
//		return itsBitSet.get(theIndex);
//	}
//
//	final void set(int theIndex)
//	{
//		checkIndex(theIndex, itsDimensions, "set");
//
//		if (!itsBitSet.get(theIndex))
//		{
//			itsBitSet.set(theIndex);
//			++itsCardinality;
//		}
//	}
//
//	final void clear(int theIndex)
//	{
//		checkIndex(theIndex, itsDimensions, "clear");
//
//		if (itsBitSet.get(theIndex))
//		{
//			itsBitSet.clear(theIndex);
//			--itsCardinality;
//		}
//	}
//
//	private static final void checkIndex(int theIndex, int theDimensions, String theMethod)
//	{
//		if (theIndex < 0 || theIndex >= theDimensions)
//			throw new IllegalArgumentException(String.format("%s.%s(): invalid index %d", ItemSet.class.getName(), theMethod, theIndex));
//	}

	// MM could/ should be defined in terms of BitSet.xor()
	// ((BitSet)theSet.clone()).xor(this);
	// TODO
	// method is only called by Basysian.rcar() and scoreNext(), all these need
	// to know is whether there is exactly one mutually exclusive bit (xor)
	// and that it is at a certain position
	// so this method can be replaced by one that is much simpler, efficient
	// and only returns a boolean
	public ItemSet symmetricDifference(ItemSet theSet)
	{
		ItemSet aSet = new ItemSet(itsDimensions);

		for (int i=0; i<itsDimensions; i++)
			if (get(i) ^ theSet.get(i))
				aSet.set(i);
		return aSet;
	}

	// MM did you mean  BitSet.cardinality() ?
	// TODO encapsulation allows to easily track this number, withou computation
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

	public int getDimensions()
	{
		return itsDimensions;
	}

	// NOTE only clones BitSet, not itsDimensions and itsEntropy
	// FIXME especially the former seems wrong
	public ItemSet getExtension(int theIndex)
	{
		ItemSet aSet = (ItemSet) clone();
		aSet.set(theIndex);
		return aSet;
	}

	final double getJointEntropy()                     { return itsJointEntropy; }
	final void setJointEntropy(double theJointEntropy) { itsJointEntropy = theJointEntropy; }

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	///// start of obsolete code                                           /////
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the index of the <em>n</em>-th set bit.
	 * 
	 * @param theIndex
	 * 
	 * @return the index of the <em>n</em>-th set bit, or <code>-1</code> if
	 * it can not be found.
	 */
	@Deprecated
	private int getItem(int theIndex)
	{
// MM why are default BitSet methods not used for this?
//		if (theIndex <= 0 || theIndex > length())
//			return -1;
//		for (int i = nextSetBit(0), j = 0; i >= 0; i = nextSetBit(i+1))
//			if (theIndex == ++j)
//				return i;
//		return -1;

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

	@Deprecated
	private ItemSet getNextItemSet()
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

	// l+1 = number consecutive bits that need to be set (when counting back
	// from index)
	// never used
	@Deprecated
	boolean isFresh(int l)
	{
// MM fast, concise alternative
//		final int i = length()-1; // index of highest set bit
//		return (i < 0) ? true : (previousClearBit(i) < (i - l));

		int aCount = 0;
		boolean aStart = false;

		// i>0 or i>=0, current loop does not test for get(0)
		// also as soon as (++aCount == l+1) the loop can break
		// as it will never return false anymore
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
	// never used
	@Deprecated
	private ItemSet skipItemSets(int l)
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
