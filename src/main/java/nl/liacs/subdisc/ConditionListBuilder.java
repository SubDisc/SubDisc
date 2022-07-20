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
	private final ConditionList EMPTY_LIST = new ConditionList0();

	/**
	 * Returns the canonical instance of the empty ConditionList, there is
	 * only one such list.
	 * 
	 * @return The empty ConditionList.
	 * 
	 * @see Condition
	 */
	static final ConditionList emptyList()
	{
		return FACTORY.EMPTY_LIST;
	}

	/**
	 * Returns a ConditionList the argument as its only Condition.
	 * 
	 * @return A ConditionList with one Condition.
	 * 
	 * @see Condition
	 */
	static final public ConditionList createList(Condition theCondition)
	{
		return FACTORY.new ConditionList1(theCondition);
	}

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
	static final ConditionList createList(ConditionList theConditionList, Condition theCondition)
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
	public abstract class ConditionList implements Comparable<ConditionList>
	{
		static final String CONJUNCT_SIGN = " AND ";

		private ConditionList(){};

		public abstract int size();

		public abstract Condition get(int index);

		// FIXME lazy implementation for now, will create get(idx, searchOrder)
		public abstract Condition getCanonical(int index);

		@Override
		public abstract int compareTo(ConditionList theConditionList);

		@Override
		public abstract String toString();

		/*
		 * A ConditionList strictlySpecialises another ConditionList if the subgroup of the former is a logical subset of the subgroup of the latter.
		 * The following are some examples:
		 * (A^B) strictlySpecialises (A)
		 * (A^B) strictlySpecialises (B)
		 * (A^B^C) strictlySpecialises (A^B) etc.
		 * (A^B^C) strictlySpecialises (A) etc.
		 * but also the following:
		 * (a > 10) strictlySpecialises (a > 5), but not the inverse
		 */
		public abstract boolean strictlySpecialises(ConditionList theOtherSubgroup);
	}

	private final class ConditionList0 extends ConditionList
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

		@Override
		public final Condition getCanonical(int index)
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
		public final int compareTo(ConditionList theConditionList)
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

		@Override
		public boolean strictlySpecialises(ConditionList theOtherCL)
		{
			return false; //the empty ConditionList is not a specialisation of anything
		}
	}

	// ConditionList1 holds just one Condition, it is always ordered
	private final class ConditionList1 extends ConditionList
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

		@Override
		public final Condition getCanonical(int index)
		{
			if (index != 0)
				throw new IndexOutOfBoundsException();

			return itsCondition;
		}

		// throws NullPointerException if supplied argument is null
		@Override
		public final int compareTo(ConditionList theConditionList)
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

		@Override
		public boolean strictlySpecialises(ConditionList theOtherCL)
		{
			if (theOtherCL.size() > size()) //a shorter CL cannot specialise a longer CL
				return false;
			if (theOtherCL.size() == 0) //any non-empty CL specialises the empty CL
				return true;
			//only ConditionList1 remains as an option

			ConditionList1 anOtherCL = (ConditionList1) theOtherCL;
			return itsCondition.strictlySpecialises(anOtherCL.itsCondition);
		}
	}

	/*
	 * ConditionList2 holds two distinct Conditions
	 * isSearchOrder indicates whether (canonical order equals search order)
	 */
	private final class ConditionList2 extends ConditionList
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
				System.out.format("ERROR ConditionList2: duplicate conjuncts%n\t%s%n\t%s%n", theConditionList.toString(), theCondition.toString());

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

		@Override
		public final Condition getCanonical(int index)
		{
			if (index != 0 && index != 1)
				throw new IndexOutOfBoundsException();

			return (index == 0) ? itsFirst: itsSecond;
		}

		// throws NullPointerException if supplied argument is null
		@Override
		public final int compareTo(ConditionList theConditionList)
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

		@Override
		public boolean strictlySpecialises(ConditionList theOtherCL)
		{
			if (theOtherCL.size() > size()) //a shorter CL cannot specialise a longer CL
				return false;
			if (theOtherCL.size() == 0) //any non-empty CL specialises the empty CL
				return true;

			//at this point theOtherCL is sizes 1 or 2 (and this is of size 2)
			if (theOtherCL instanceof ConditionList1) //size 1
			{
				Condition anOtherCondition = ((ConditionList1) theOtherCL).itsCondition;

				if (itsFirst.logicallyEquivalent(anOtherCondition) || itsSecond.logicallyEquivalent(anOtherCondition)) //this implies theOtherCl is contained in this 
					return true;
				if (itsFirst.strictlySpecialises(anOtherCondition) || itsSecond.strictlySpecialises(anOtherCondition))
					return true;
			}
			if (theOtherCL instanceof ConditionList2) //size 2
			{
				ConditionList2 anOtherCL = (ConditionList2) theOtherCL;

				if (itsFirst.logicallyEquivalent(anOtherCL.itsFirst) && itsSecond.logicallyEquivalent(anOtherCL.itsSecond)) //identical
					return false;
				if (itsFirst.logicallyEquivalent(anOtherCL.itsSecond) && itsSecond.logicallyEquivalent(anOtherCL.itsFirst)) //conditions swapped but otherwise identical
					return false;

				if (itsFirst.logicallyEquivalent(anOtherCL.itsFirst) && itsSecond.strictlySpecialises(anOtherCL.itsSecond)) //firsts identical and seconds strictly spec
					return true;
				if (itsFirst.strictlySpecialises(anOtherCL.itsFirst) && itsSecond.logicallyEquivalent(anOtherCL.itsSecond))
					return true;

				if (itsFirst.logicallyEquivalent(anOtherCL.itsSecond) && itsSecond.strictlySpecialises(anOtherCL.itsFirst))
					return true;
				if (itsFirst.strictlySpecialises(anOtherCL.itsSecond) && itsSecond.logicallyEquivalent(anOtherCL.itsFirst))
					return true;
			}
			return false;
		}
	}

	/*
	 * ConditionList3 holds three distinct Conditions
	 * isSearchOrder indicates the search order
	 */
	private final class ConditionList3 extends ConditionList
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
				System.out.format("ERROR ConditionList3: duplicate conjuncts%n\t%s%n\t%s%n", theConditionList.toString(), theCondition.toString());
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

		@Override
		public final Condition getCanonical(int index)
		{
			if (index != 0 && index != 1 && index != 2)
				throw new IndexOutOfBoundsException();

			return (index == 0) ? itsFirst : (index == 1) ? itsSecond : itsThird;
		}

		// throws NullPointerException if supplied argument is null
		@Override
		public final int compareTo(ConditionList theConditionList)
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

		@Override
		public boolean strictlySpecialises(ConditionList theOtherCL)
		{
			return false; //implement
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
	private final class ConditionListN extends ConditionList
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
					System.out.format("ERROR ConditionListN-N: duplicate conjuncts%n\t%s%n\t%s%n", theConditionList.toString(), theCondition.toString());
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
				System.out.format("ERROR ConditionListN-3: duplicate conjuncts%n\t%s%n\t%s%n", theConditionList.toString(), theCondition.toString());

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

		// throws IndexOutOfBoundsException when !(0 <= index < size)
		@Override
		public final Condition getCanonical(int index)
		{
			return itsCanonicalOrder[index];
		}

		// throws NullPointerException if supplied argument is null
		@Override
		public final int compareTo(ConditionList theConditionList)
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

		@Override
		public boolean strictlySpecialises(ConditionList theOtherCL)
		{
			return false; //implement
		}
	}

	/* toString() methods for special ConditionList classes ***************/

	// ConditionList2
	private static final String toString(Condition theFirst, Condition theSecond)
	{
		return new StringBuilder(64)
					.append(theFirst.toString())
					.append(ConditionList.CONJUNCT_SIGN)
					.append(theSecond.toString())
					.toString();
	}

	// ConditionList3
	private static final String toString(Condition theFirst, Condition theSecond, Condition theThird)
	{
		return new StringBuilder(128)
					.append(theFirst.toString())
					.append(ConditionList.CONJUNCT_SIGN)
					.append(theSecond.toString())
					.append(ConditionList.CONJUNCT_SIGN)
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
			sb.append(ConditionList.CONJUNCT_SIGN);
			sb.append(theSearchOrder[i].toString());
		}

		return sb.toString();
	}

	// used for testing results obtained in multi-threaded experiments
	static final String toCanonicalOrderString(ConditionList theConditionList)
	{
		// abuses method for simple implementation
		return toString(toCanonicalOrder(theConditionList));
	}

	// test (ConditionList order == ConditionBaseSet order) - should change soon
	// for more information, refer to isInConditionBaseOrder(ConditionList)
	static final Condition[] toCanonicalOrder(ConditionList theConditionList)
	{
		int aSize = theConditionList.size();
		Condition[] aConditions = new Condition[aSize];

		// get() returns Conditions in search order
		for (int i = 0; i < aSize; ++i)
			aConditions[i] = theConditionList.get(i);

		// put Conditions in canonical order
		Arrays.sort(aConditions);

		return aConditions;
	}

	// FIXME MM static method for now, might change later (for each sub-class)
	static final boolean isCanonicalisedProperSubSetOf(ConditionList theFormer, ConditionList theLatter)
	{
		if (theFormer.size() >= theLatter.size())
			return false;

		Condition[] aFormer = canonicalise(theFormer);
		Condition[] aLatter = canonicalise(theLatter);

		// if every Condition in the former occurs in the latter -> subset
	OUTER: for (int i = 0; i < aFormer.length; ++i)
		{
			Condition c = aFormer[i];

			// all Conditions in theFormer are checked and occurred in theLatter
			if (aFormer[i] == null)
				return true;

			boolean aFound = false;
			for (Condition d : aLatter)
			{
				if (d == null)
					return false;

				if (c.compareTo(d) == 0)
					continue OUTER;
			}

			// c is not in aLatter, so theFormer is not a subset of theLatter
			if (!aFound)
				return false;
		}

		return true;
	}

	// FIXME MM  lazy implementation for now
	private static final Condition[] canonicalise(ConditionList theConditionList)
	{
		int aSize = theConditionList.size();

		Condition[] aList = new Condition[aSize];
		for (int i = 0; i < aSize; ++i)
			aList[i] = theConditionList.get(i);

		if (aSize <= 1)
		{
			return aList;
		}

		Arrays.sort(aList);

		for (int i = 0; i < aSize-1; ++i)
		{
			Condition ci = aList[i];
			if (ci == null)
				continue;

			Column cc = ci.getColumn();

			Operator oi = ci.getOperator();
			if (oi != Operator.LESS_THAN_OR_EQUAL && oi != Operator.GREATER_THAN_OR_EQUAL)
				continue;

			boolean isLEQ = (oi == Operator.LESS_THAN_OR_EQUAL);

			for (int j = i+1; j < aSize; ++j)
			{
				Condition cj = aList[j];
				if ((cj == null) || (cc != cj.getColumn()) || (oi != cj.getOperator()))
					continue;

				boolean isLEQ2 = (cj.getOperator() == Operator.LESS_THAN_OR_EQUAL);
				if (isLEQ != isLEQ2)
					continue;

				// equal values in conditions for same attribute + operator must
				// never occur; after sorting the former bound should always be
				// smaller that latter (during search, first used bound is
				// higher for <=, and smaller for >=)
				assert (Float.compare(ci.getNumericValue(), cj.getNumericValue()) < 0);
				// remove least specific bound
				if (isLEQ)
					aList[j] = null;
				else
				{
					aList[i] = null;
					break;
				}
			}
		}

		for (int i = 0, j = -1; i < aSize; ++i)
		{
			Condition c = aList[i];
			if (c != null)
			{
				aList[i] = null; // might be reset below
				aList[++j] = c;
			}
		}

		return aList;
	}
}
