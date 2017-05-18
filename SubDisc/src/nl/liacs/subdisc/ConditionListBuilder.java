package nl.liacs.subdisc;

import java.util.*;

/*
 * class is implemented as an enum to enforce a single unique instance
 * class offers only static methods, these internally call the sole instance
 * 
 * methods that can be called externally are:
 * emptyList(),
 * createList(Condition),
 * createList(ConditionList, Condition)
 */
public enum ConditionListBuilder
{
	// the only instance
	FACTORY;

	// there is only one canonical empty list, it is a member of FACTORY
	private final ConditionListA EMPTY_LIST = new ConditionList0();

	/**
	 * Returns the canonical instance of the empty ConditionList, there is
	 * only one such list.
	 * 
	 * @return The empty ConditionList.
	 * 
	 * @see Condition
	 */
	static final ConditionListA emptyList()
	{
		return FACTORY.EMPTY_LIST;
	}

//	this method is never called in the current code base
//	/**
//	 * Returns a ConditionList the argument as its only Condition.
//	 * 
//	 * @return A ConditionList with one Condition.
//	 * 
//	 * @see Condition
//	 */
//	static final ConditionListA createList(Condition theCondition)
//	{
//		return FACTORY.new ConditionList1(theCondition);
//	}

	/**
	 * Returns a ConditionList with as conjuncts all of the Conditions in
	 * the supplied ConditionList, and the supplied Condition.
	 * 
	 * The number of conjuncts in the returned ConditionList is equal to the
	 * size of the supplied ConditionList plus one.
	 * 
	 * @return A ConditionList with multiple conjuncts.
	 * 
	 * @see Condition
	 */
	static final ConditionListA createList(ConditionListA theConditionList, Condition theCondition)
	{
		if (theConditionList instanceof ConditionList0)
			return FACTORY.new ConditionList1(theCondition);
		if (theConditionList instanceof ConditionList1)
			return FACTORY.new ConditionList2((ConditionList1) theConditionList, theCondition);
		if (theConditionList instanceof ConditionList2)
			return FACTORY.new ConditionList3((ConditionList2) theConditionList, theCondition);
		if (theConditionList instanceof ConditionList3)
			return FACTORY.new ConditionListN((ConditionList3) theConditionList, theCondition);
		if (theConditionList instanceof ConditionListN)
			return FACTORY.new ConditionListN((ConditionListN) theConditionList, theCondition);
		// should never happen
		throw new AssertionError(theConditionList);
	}

	/*
	 **********************************************************************
	 * the classes below all keep the conjuncts in a canonical order
	 * this allows for very simple equivalence checking
	 * additionally, the use of a number of different ConditionListX types
	 * optimises comparison and creation further
	 * there are special classes for up to 3 conjuncts, as these would be
	 * most common in typical data mining experiments
	 * 
	 * all classes are all inside the same parent class
	 * therefore they can access each others (Condition) members directly
	 * these members are in canonical order
	 * as a result, creating a new _ordered_ extended conjunction is easy
	 *
	 **********************************************************************
	 *
	 * implementation classes do not extend each other
	 * it would allow casting of a sub-type to a super-type
	 * this is considered invalid
	 * {Condition1} is not a superset of {Condition1 AND Condition2}
	 * 
	 * ConditionList2 c2 = new ConditionList2(...);
	 * ConditionList1 c1 = (ConditionList1) c2; // throws CastException
	 * 
	 * it also forces instanceof not to consider a sub-type of a super-type
	 * (c2 == instanceof c1) // returns false, by design as c2 !extends c1
	 * 
	 * all implementation classes are private
	 * none of the internals are relevant to the outside world
	 * and implementation updates do not require API changes
	 * 
	 * all implementation classes are final
	 * this disallows extending, and therefore behaviour is guaranteed
	 */

	// all implementation classes should extend this abstract class
	// this ensures that they implement all necessary methods
	public abstract class ConditionListA implements Comparable<ConditionListA>
	{
		static final String CONJUNCT_SIGN = " AND ";

		private ConditionListA(){};

		public abstract int size();

		public abstract Condition get(int index);

		@Override
		public abstract int compareTo(ConditionListA theConditionList);

		@Override
		public abstract String toString();
	}

	private final class ConditionList0 extends ConditionListA
	{
		// can not be instantiated
		private ConditionList0() {}

		@Override
		public final int size() { return 0; }

		@Override
		public final Condition get(int index)
		{
			throw new IndexOutOfBoundsException();
		}

		/*
		 * comparison assumes there is only one canonical empty list
		 * therefore this list is smaller than all other lists
		 * 
		 * throws NullPointerException if supplied argument is null
		 */
		@Override
		public final int compareTo(ConditionListA theConditionList)
		{
			// per Comparable contract
			if (theConditionList == null)
				throw new NullPointerException();

			return (this == theConditionList) ? 0 : -1;
		}

		@Override
		public final String toString()
		{
			return "(empty)";
		}
	}

	// ConditionList1 holds just one Condition, it is always ordered
	private final class ConditionList1 extends ConditionListA
	{
		// the only Condition in this list
		private final Condition itsCondition;

		// throws IllegalArgumentException if supplied argument is null
		private ConditionList1(Condition theCondition)
		{
			if (theCondition == null)
				throw new IllegalArgumentException();

			itsCondition = theCondition;
		}

		@Override
		public final int size() { return 1; }

		@Override
		public final Condition get(int index)
		{
			if (index != 0)
				throw new IndexOutOfBoundsException();

			return itsCondition;
		}

		// throws NullPointerException if supplied argument is null
		@Override
		public final int compareTo(ConditionListA theConditionList)
		{
			if (this == theConditionList)
				return 0;

			int cmp = 1 - theConditionList.size();
			if (cmp != 0)
				return cmp;

			ConditionList1 other = (ConditionList1) theConditionList;
			return this.itsCondition.compareTo(other.itsCondition);
		}

		@Override
		public final String toString()
		{
			return itsCondition.toString();
		}
	}

	/*
	 * ConditionList2 holds two distinct Conditions
	 * isSearchOrder indicates whether (canonical order equals search order)
	 */
	private final class ConditionList2 extends ConditionListA
	{
		// internally Conditions are always in canonical order
		private final Condition itsFirst;
		private final Condition itsSecond;
		// if true search order is [itsFirst, itsSecond]
		// else search order is [itsSecond, itsFirst]
		private final boolean isInSearchOrder;

		// throws NullPointerException if supplied argument is null
		private ConditionList2(ConditionList1 theConditionList, Condition theCondition)
		{
			Condition c = theConditionList.itsCondition;

			// conjunctions of identical Conditions make no sense
			int cmp = c.compareTo(theCondition);
			if (cmp == 0)
				//throw new IllegalArgumentException();
// FIXME MM throw exception when SubgroupDiscovery.Filter is enabled
System.out.format("ERROR: duplicate conjuncts%n\t%s%n\t%s%n", theConditionList.toString(), theCondition.toString());

			isInSearchOrder = (cmp < 0);
			itsFirst = isInSearchOrder ? c : theCondition;
			itsSecond = isInSearchOrder ? theCondition : c;
		}

		@Override
		public final int size() { return 2; }

		// always return Conditions based on search order
		@Override
		public final Condition get(int index)
		{
			if (index != 0 && index != 1)
				throw new IndexOutOfBoundsException();

			if (index == 0)
				return isInSearchOrder ? itsFirst: itsSecond;
			return isInSearchOrder ? itsSecond : itsFirst;
		}

		// throws NullPointerException if supplied argument is null
		@Override
		public final int compareTo(ConditionListA theConditionList)
		{
			if (this == theConditionList)
				return 0;

			int cmp = 2 - theConditionList.size();
			if (cmp != 0)
				return cmp;

			ConditionList2 other = (ConditionList2) theConditionList;
			cmp = this.itsFirst.compareTo(other.itsFirst);
			return cmp != 0 ? cmp : this.itsSecond.compareTo(other.itsSecond);
		}

		// always print ConditionList in search order
		@Override
		public final String toString()
		{
			if (isInSearchOrder)
				return ConditionListBuilder.toString(itsFirst, itsSecond);
			return ConditionListBuilder.toString(itsSecond, itsFirst);
		}
	}

	/*
	 * ConditionList3 holds three distinct Conditions
	 * isSearchOrder indicates the search order
	 */
	private final class ConditionList3 extends ConditionListA
	{
		// internally Conditions are always in canonical order
		private final Condition itsFirst;
		private final Condition itsSecond;
		private final Condition itsThird;
		// see getOrder() for interpretation of this number
		private final byte itsSearchOrder;

		// throws NullPointerException if supplied argument is null
		private ConditionList3(ConditionList2 theConditionList, Condition theCondition)
		{
			assert (theConditionList.itsFirst != theConditionList.itsSecond);

			boolean cl2IsInOrder = theConditionList.isInSearchOrder;

			Condition x = theConditionList.itsFirst;
			Condition y = theConditionList.itsSecond;
			Condition z = theCondition;

			int i = z.compareTo(x);
			int j = z.compareTo(y);

			// conjunctions of identical Conditions make no sense
			if (i == 0 || j == 0)
//				throw new IllegalArgumentException();
// FIXME MM throw exception when SubgroupDiscovery.Filter is enabled
System.out.format("ERROR: duplicate conjuncts%n\t%s%n\t%s%n", theConditionList.toString(), theCondition.toString());
			if (i < 0)
			{
				itsFirst = theCondition;
				itsSecond = x;
				itsThird = y;

				itsSearchOrder = getOrder(cl2IsInOrder, 1);
			}
			else if (j < 0)
			{
				itsFirst = x;
				itsSecond = theCondition;
				itsThird = y;

				itsSearchOrder = getOrder(cl2IsInOrder, 2);
			}
			else
			{
				itsFirst = x;
				itsSecond = y;
				itsThird = theCondition;

				itsSearchOrder = getOrder(cl2IsInOrder, 3);
			}
		}

		/*
		 * table is used by:
		 * getOrder()
		 * find()
		 * toString()
		 * on modification all methods require update(s)
		 * assumes ConditionList items are in canonical order
		 * 
		 * 6 permutations
		 * 0 = f s t
		 * 1 = f t s
		 * 2 = s f t
		 * 3 = s t f
		 * 4 = t f s
		 * 5 = t s f
		 */
		private byte getOrder(boolean cl2IsInOrder, int newPos)
		{
			switch (newPos)
			{
				// (s t f) or (t s f)
				case 1 : return (byte) (cl2IsInOrder ? 3 : 5);
				// (f t s) or (t f s)
				case 2 : return (byte) (cl2IsInOrder ? 1 : 4);
				// (f s t) of (s f t)
				case 3 : return (byte) (cl2IsInOrder ? 0 : 2);
				// should never happen
				default :
					throw new AssertionError(newPos);
			}
		}

		// see getOrder() for table of canonical / search order mapping
		private Condition find(int searchOrderIndex)
		{
			boolean f = (searchOrderIndex == 0);
			boolean s = (searchOrderIndex == 1);

			switch (itsSearchOrder)
			{
				case 0 : return f ? itsFirst : s ? itsSecond : itsThird;
				case 1 : return f ? itsFirst : s ? itsThird : itsSecond;
				case 2 : return f ? itsSecond : s ? itsFirst : itsThird;
				case 3 : return f ? itsSecond : s ? itsThird : itsFirst;
				case 4 : return f ? itsThird : s ? itsFirst : itsSecond;
				case 5 : return f ? itsThird : s ? itsSecond : itsFirst;
				// should never happen
				default :
					throw new AssertionError(itsSearchOrder);
			}
		}

		@Override
		public final int size() { return 3; }

		// always return Conditions based on search order
		@Override
		public final Condition get(int index)
		{
			if (index != 0 && index != 1 && index != 2)
				throw new IndexOutOfBoundsException();

			return find(index);
		}

		// throws NullPointerException if supplied argument is null
		@Override
		public final int compareTo(ConditionListA theConditionList)
		{
			if (this == theConditionList)
				return 0;

			int cmp = 3 - theConditionList.size();
			if (cmp != 0)
				return cmp;

			ConditionList3 other = (ConditionList3) theConditionList;

			// test all Conditions (in canonical order)
			cmp = this.itsFirst.compareTo(other.itsFirst);
			if (cmp != 0)
				return cmp;
			cmp = this.itsSecond.compareTo(other.itsSecond);
			if (cmp != 0)
				return cmp;
			return this.itsThird.compareTo(other.itsThird);
		}

		// always print ConditionList in search order
		// see getOrder() for table of canonical / search order mapping
		@Override
		public final String toString()
		{
			switch (itsSearchOrder)
			{
				case 0 : return ConditionListBuilder.toString(itsFirst, itsSecond, itsThird);
				case 1 : return ConditionListBuilder.toString(itsFirst, itsThird, itsSecond);
				case 2 : return ConditionListBuilder.toString(itsSecond, itsFirst, itsThird);
				case 3 : return ConditionListBuilder.toString(itsSecond, itsThird, itsFirst);
				case 4 : return ConditionListBuilder.toString(itsThird, itsFirst, itsSecond);
				case 5 : return ConditionListBuilder.toString(itsThird, itsSecond, itsFirst);
				default :
					throw new AssertionError();
			}
		}
	}

	/*
	 * ConditionListN holds four or more Conditions
	 * it holds the Conditions in canonical order and search order
	 * 
	 * NOTE itsSearchOrder could be int[] with indices into canonical[]
	 * but it complicate class logic
	 * only for 64-bit JVMs there is a difference between (64-bit) reference
	 * pointers and (32-bit) int indices
	 * this difference is small, and ConditionListN will not be used much
	 */
	private final class ConditionListN extends ConditionListA
	{
		private final Condition[] itsCanonicalOrder;
		private final Condition[] itsSearchOrder;

		// throws NullPointerException if supplied argument is null
		private ConditionListN(ConditionListN theConditionList, Condition theCondition)
		{
			Condition[] ca = theConditionList.itsCanonicalOrder;
			int size = ca.length;

			assert (size == theConditionList.itsSearchOrder.length);
			assert (size == new TreeSet<Condition>(Arrays.asList(ca)).size());

			itsCanonicalOrder = new Condition[size+1];
			itsSearchOrder = new Condition[size+1];

			for (int i = 0; i < size; ++i)
			{
				Condition c = ca[i];
				int cmp = theCondition.compareTo(c);

				if (cmp < 0)
				{
					itsCanonicalOrder[i] = theCondition;
					itsCanonicalOrder[++i] = c;
					while (++i <= size)
						itsCanonicalOrder[i] = ca[i-1];

					break; // breaks on (i < size) anyway
				}
				// conjunctions of identical Conditions make no sense
				else if (cmp == 0)
//					throw new IllegalArgumentException();
// FIXME MM throw exception when SubgroupDiscovery.Filter is enabled
System.out.format("ERROR: duplicate conjuncts%n\t%s%n\t%s%n", theConditionList.toString(), theCondition.toString());
				else // (cmp > 0)
				{
					itsCanonicalOrder[i] = c;
					if (i == size-1)
					{
						itsCanonicalOrder[size] = theCondition;
						break; // breaks on (i < size)
					}
				}
			}

			System.arraycopy(theConditionList.itsSearchOrder, 0, this.itsSearchOrder, 0, size);
			itsSearchOrder[size] = theCondition;
		}

		// throws NullPointerException if supplied argument is null
		private ConditionListN(ConditionList3 theConditionList, Condition theCondition)
		{
			assert (theConditionList.itsFirst != theConditionList.itsSecond);
			assert (theConditionList.itsFirst != theConditionList.itsThird);
			assert (theConditionList.itsSecond != theConditionList.itsThird);
			assert (theConditionList.itsSearchOrder >= 0);
			assert (theConditionList.itsSearchOrder <= 5);

			Condition x = theConditionList.itsFirst;
			Condition y = theConditionList.itsSecond;
			Condition z = theConditionList.itsThird;
			Condition c = theCondition;

			int i = c.compareTo(x);
			int j = c.compareTo(y);
			int k = c.compareTo(z);

			// conjunctions of identical Conditions make no sense
			if (i == 0 || j == 0 || k == 0)
//				throw new IllegalArgumentException();
// FIXME MM throw exception when SubgroupDiscovery.Filter is enabled
System.out.format("ERROR: duplicate conjuncts%n\t%s%n\t%s%n", theConditionList.toString(), theCondition.toString());

			if (i < 0)
				itsCanonicalOrder = new Condition[] {c, x, y, z};
			else if (j < 0)
				itsCanonicalOrder = new Condition[] {x, c, y, z};
			else if (k < 0)
				itsCanonicalOrder = new Condition[] {x, y, c, z};
			else
				itsCanonicalOrder = new Condition[] {x, y, z, c};

			//itsSearchOrder = create(theConditionList, c);
			itsSearchOrder = new Condition[4];
			itsSearchOrder[0] = theConditionList.get(0);
			itsSearchOrder[1] = theConditionList.get(1);
			itsSearchOrder[2] = theConditionList.get(2);
			itsSearchOrder[3] = theCondition;
		}

		@Override
		public final int size() { return itsCanonicalOrder.length; }

		// throws IndexOutOfBoundsException when !(0 <= index < size)
		@Override
		public final Condition get(int index)
		{
			return itsSearchOrder[index];
		}

		// throws NullPointerException if supplied argument is null
		@Override
		public final int compareTo(ConditionListA theConditionList)
		{
			if (this == theConditionList)
				return 0;

			int cmp = itsCanonicalOrder.length - theConditionList.size();
			if (cmp != 0)
				return cmp;

			ConditionListN other = (ConditionListN) theConditionList;
			Condition[] oa = other.itsCanonicalOrder;
			for (int i = 0; i < itsCanonicalOrder.length; ++i)
			{
				cmp = itsCanonicalOrder[i].compareTo(oa[i]);
				if (cmp != 0)
					return cmp;
			}

			return 0;
		}

		@Override
		public final String toString()
		{
			return ConditionListBuilder.toString(itsSearchOrder);
		}
	}

	/* toString() methods for special ConditionList classes ***************/

	// ConditionList2
	private static final String toString(Condition theFirst, Condition theSecond)
	{
		return new StringBuilder(64)
					.append(theFirst.toString())
					.append(ConditionListA.CONJUNCT_SIGN)
					.append(theSecond.toString())
					.toString();
	}

	// ConditionList3
	private static final String toString(Condition theFirst, Condition theSecond, Condition theThird)
	{
		return new StringBuilder(128)
					.append(theFirst.toString())
					.append(ConditionListA.CONJUNCT_SIGN)
					.append(theSecond.toString())
					.append(ConditionListA.CONJUNCT_SIGN)
					.append(theThird.toString())
					.toString();
	}

	// ConditionListN
	private static final String toString(Condition[] theSearchOrder)
	{
		int size = theSearchOrder.length;

		StringBuilder sb = new StringBuilder(size * 32);
		sb.append(theSearchOrder[0].toString());
		for (int i = 1; i < size; ++i)
		{
			sb.append(ConditionListA.CONJUNCT_SIGN);
			sb.append(theSearchOrder[i].toString());
		}

		return sb.toString();
	}

	// used for testing results obtained in multi-threaded experiments
	static final String toCanonicalOrder(ConditionListA theConditionList)
	{
		int aSize = theConditionList.size();
		Condition[] aConditions = new Condition[aSize];

		// get() return Conditions in search order
		for (int i = 0; i < aSize; ++i)
			aConditions[i] = theConditionList.get(i);

		// put Conditions in canonical order
		Arrays.sort(aConditions);

		// abuses method for simple implementation
		return toString(aConditions);
	}
}
