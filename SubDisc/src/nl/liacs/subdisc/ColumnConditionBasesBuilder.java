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
public enum ColumnConditionBasesBuilder
{
	// the only instance
	FACTORY;

	// as List for easy access, called Set, as elements are sorted and unique
	List<ColumnConditionBases> getColumnConditionBasesSet(Table theTable, SearchParameters theSearchParameters)
	{
		return Collections.unmodifiableList(get(theTable, theSearchParameters));
	}

	private static final List<ColumnConditionBases> get(Table theTable, SearchParameters theSearchParameters)
	{
		TargetConcept tc = theSearchParameters.getTargetConcept();

		boolean useSets = theSearchParameters.getNominalSets();
		// set-valued only allowed for SINGLE_NOMINAL
		assert (!useSets || (tc.getTargetType() == TargetType.SINGLE_NOMINAL));
		NumericOperatorSetting nos = theSearchParameters.getNumericOperatorSetting();

		List<ColumnConditionBases> result = new ArrayList<ColumnConditionBases>(theTable.getNrColumns());

		for (Column c : theTable.getColumns())
		{
			// ignore
			if (!c.getIsEnabled() || tc.isTargetAttribute(c))
				continue;

			// no useful Refinements are possible
			if (c.getCardinality() <= 1)
				continue;

			switch (c.getType())
			{
				case NOMINAL :
				{
					if (useSets)
						result.add(FACTORY.new ColumnConditionBasesNominalElementOf(c));
					else
						result.add(FACTORY.new ColumnConditionBasesNominalEquals(c));
					break;
				}
				case NUMERIC :
				{
					// checking NumericOperatorSetting once would be enough
					// but code is called only once per SubgroupDiscovery.mine()
					// and the number of Columns is generally low
					final ColumnConditionBasesNumeric cb;
					switch (nos)
					{
						case NUMERIC_NORMAL    : cb = FACTORY.new ColumnConditionBasesNumericNormal(c);    break;
						case NUMERIC_LEQ       : cb = FACTORY.new ColumnConditionBasesNumericLEQ(c);       break;
						case NUMERIC_GEQ       : cb = FACTORY.new ColumnConditionBasesNumericGEQ(c);       break;
						case NUMERIC_ALL       : cb = FACTORY.new ColumnConditionBasesNumericAll(c);       break;
						case NUMERIC_EQ        : cb = FACTORY.new ColumnConditionBasesNumericEquals(c);    break;
						case NUMERIC_INTERVALS : cb = FACTORY.new ColumnConditionBasesNumericIntervals(c); break;
						default :
							throw new AssertionError();
					}
					result.add(cb);
					break;
				}
				// no fall-through, ORDINAL is not implemented
				case ORDINAL :
				{
					throw new AssertionError(c.getType());
				}
				case BINARY :
				{
					// assumes just 1 BINARY Operator
					result.add(FACTORY.new ColumnConditionBasesBinary(c));
					break;
				}
				default :
					throw new AssertionError(c.getType());
			}
		}

		return result;
	}

	/*
	 * nominal classes are based on Operator
	 * numeric classes are based on NumericOperatorSetting
	 *
	 * size() and get() are currently irrelevant for BINARY and NOMINAL, this
	 * might change, and including it for all classes makes them uniform
	 *
	 * with every sub-type there is an assert testing the the assumption of the
	 * number of valid Operators for the Column AttributeType
	 *
	 * the following asserts are in there for future code changes
	 */
	static { assert (!isOperatorCodeCheckRequired() && !isNumericOperatorSettingCodeCheckRequired()); }

	private static final boolean isOperatorCodeCheckRequired()
	{
		EnumSet<Operator> a = EnumSet.allOf(Operator.class);
		EnumSet<Operator> b = EnumSet.of(Operator.ELEMENT_OF,
											Operator.EQUALS,
											Operator.LESS_THAN_OR_EQUAL,
											Operator.GREATER_THAN_OR_EQUAL,
											Operator.BETWEEN);
		return (!a.equals(b));
	}

	private static final boolean isNumericOperatorSettingCodeCheckRequired()
	{
		EnumSet<NumericOperatorSetting> a = EnumSet.allOf(NumericOperatorSetting.class);
		EnumSet<NumericOperatorSetting> b = EnumSet.of(NumericOperatorSetting.NUMERIC_NORMAL,
														NumericOperatorSetting.NUMERIC_LEQ,
														NumericOperatorSetting.NUMERIC_GEQ,
														NumericOperatorSetting.NUMERIC_ALL,
														NumericOperatorSetting.NUMERIC_EQ,
														NumericOperatorSetting.NUMERIC_INTERVALS);
		return (!a.equals(b));
	}

	abstract class ColumnConditionBases
	{
		abstract int size();

		abstract ConditionBase get(int index);
	}

	// unlike old code, do not combine Binary and Nominal cases, force a split
	// package-private final class: omit assert on Column.getType()
	// static assert to check that there is only one valid Operator for BINARY
	static { assert (Operator.getOperators(AttributeType.BINARY).equals(EnumSet.of(Operator.EQUALS))); }
	final class ColumnConditionBasesBinary extends ColumnConditionBases
	{
		// only ConditionBase, assumes a single Operator (EQUALS) for BINARY
		private final ConditionBase itsConditionBase;

		private ColumnConditionBasesBinary(Column theColumn)
		{
			itsConditionBase = new ConditionBase(theColumn, Operator.EQUALS);
		}

		@Override
		final int size() { return 1; }

		// do not bother checking the supplied parameter
		@Override
		final ConditionBase get(int ignored) { return itsConditionBase; }
	}

	// NOTE assertion is not complete, there is no convenient way to check that
	//      the algorithm enforce the use of at most one Operator for NOMINAL
	// NOTE when asserts are not enabled, this is a no-op
	static { assert (elementOfIsValidForNominalOnly()); }
	private static final boolean elementOfIsValidForNominalOnly()
	{
		EnumSet<AttributeType> e = EnumSet.allOf(AttributeType.class);
		e.remove(AttributeType.NOMINAL);

		for (AttributeType a : e)
			if (Operator.ELEMENT_OF.isValidFor(a))
				return false;

		return true;
	}

	final class ColumnConditionBasesNominalElementOf extends ColumnConditionBases
	{
		// only ConditionBase, assumes a single Operator (ELEMENT_OF)
		private final ConditionBase itsConditionBase;

		private ColumnConditionBasesNominalElementOf(Column theColumn)
		{
			itsConditionBase = new ConditionBase(theColumn, Operator.ELEMENT_OF);
		}

		@Override
		final int size() { return 1; }

		// do not bother checking the supplied parameter
		@Override
		final ConditionBase get(int ignored) { return itsConditionBase; }
	}

	// NOTE see comment for ColumnConditionBasesNominalElementOf()
	//      but the situation is worse here, as EQUALS is also used for NUMERIC
	static { assert (Operator.EQUALS.isValidFor(AttributeType.NOMINAL)); }
	final class ColumnConditionBasesNominalEquals extends ColumnConditionBases
	{
		// only ConditionBase, assumes a single Operator (EQUALS)
		private final ConditionBase itsConditionBase;

		private ColumnConditionBasesNominalEquals(Column theColumn)
		{
			itsConditionBase = new ConditionBase(theColumn, Operator.EQUALS);
		}

		@Override
		public final int size() { return 1; }

		// do not bother checking the supplied parameter
		@Override
		public final ConditionBase get(int ignored) { return itsConditionBase; }
	}

	// ColumnConditionBasesNumeric such that all sub-types are treated uniformly
	abstract class ColumnConditionBasesNumeric extends ColumnConditionBases {}

	static { assert (NumericOperatorSetting.NUMERIC_NORMAL.getOperators().equals(EnumSet.of(Operator.LESS_THAN_OR_EQUAL, Operator.GREATER_THAN_OR_EQUAL))); }
	final class ColumnConditionBasesNumericNormal extends ColumnConditionBasesNumeric
	{
		private final ConditionBase itsConditionBaseLEQ;
		private final ConditionBase itsConditionBaseGEQ;

		private ColumnConditionBasesNumericNormal(Column theColumn)
		{
			itsConditionBaseLEQ = new ConditionBase(theColumn, Operator.LESS_THAN_OR_EQUAL);
			itsConditionBaseGEQ = new ConditionBase(theColumn, Operator.GREATER_THAN_OR_EQUAL);
		}

		final int size() { return 2; }

		final ConditionBase get(int zeroOrOne)
		{
			assert ((zeroOrOne == 0) || zeroOrOne == 1);
			return (zeroOrOne == 0 ? itsConditionBaseLEQ : itsConditionBaseGEQ); 
		}
	}

	static { assert (NumericOperatorSetting.NUMERIC_LEQ.getOperators().equals(EnumSet.of(Operator.LESS_THAN_OR_EQUAL))); }
	final class ColumnConditionBasesNumericLEQ extends ColumnConditionBasesNumeric
	{
		private final ConditionBase itsConditionBase;

		private ColumnConditionBasesNumericLEQ(Column theColumn)
		{
			itsConditionBase = new ConditionBase(theColumn, Operator.LESS_THAN_OR_EQUAL);
		}

		@Override
		final int size() { return 1; }

		// do not bother checking the supplied parameter
		@Override
		final ConditionBase get(int ignored) { return itsConditionBase; }
	}

	static { assert (NumericOperatorSetting.NUMERIC_GEQ.getOperators().equals(EnumSet.of(Operator.GREATER_THAN_OR_EQUAL))); }
	final class ColumnConditionBasesNumericGEQ extends ColumnConditionBasesNumeric
	{
		private final ConditionBase itsConditionBase;

		private ColumnConditionBasesNumericGEQ(Column theColumn)
		{
			itsConditionBase = new ConditionBase(theColumn, Operator.GREATER_THAN_OR_EQUAL);
		}

		@Override
		final int size() { return 1; }

		// do not bother checking the supplied parameter
		@Override
		final ConditionBase get(int ignored) { return itsConditionBase; }
	}

	static { assert (NumericOperatorSetting.NUMERIC_ALL.getOperators().equals(EnumSet.of(Operator.EQUALS, Operator.LESS_THAN_OR_EQUAL, Operator.GREATER_THAN_OR_EQUAL))); }
	final class ColumnConditionBasesNumericAll extends ColumnConditionBasesNumeric
	{
		private final ConditionBase itsConditionBaseEquals;
		private final ConditionBase itsConditionBaseLEQ;
		private final ConditionBase itsConditionBaseGEQ;

		private ColumnConditionBasesNumericAll(Column theColumn)
		{
			itsConditionBaseEquals = new ConditionBase(theColumn, Operator.EQUALS);
			itsConditionBaseLEQ = new ConditionBase(theColumn, Operator.LESS_THAN_OR_EQUAL);
			itsConditionBaseGEQ = new ConditionBase(theColumn, Operator.GREATER_THAN_OR_EQUAL);
		}

		@Override
		final int size() { return 3; }

		@Override
		final ConditionBase get(int zeroOrOneOrTwo)
		{
			int i = zeroOrOneOrTwo;
			assert ((i == 0) || i == 1 || i == 2);
			return (i == 0 ? itsConditionBaseEquals : (i == 1 ? itsConditionBaseLEQ : itsConditionBaseGEQ));
		}
	}

	static { assert (NumericOperatorSetting.NUMERIC_EQ.getOperators().equals(EnumSet.of(Operator.EQUALS))); }
	final class ColumnConditionBasesNumericEquals extends ColumnConditionBasesNumeric
	{
		private final ConditionBase itsConditionBase;

		private ColumnConditionBasesNumericEquals(Column theColumn)
		{
			itsConditionBase = new ConditionBase(theColumn, Operator.EQUALS);
		}

		@Override
		final int size() { return 1; }

		// do not bother checking the supplied parameter
		@Override
		final ConditionBase get(int ignored) { return itsConditionBase; }
	}

	static { assert (NumericOperatorSetting.NUMERIC_INTERVALS.getOperators().equals(EnumSet.of(Operator.BETWEEN))); }
	final class ColumnConditionBasesNumericIntervals extends ColumnConditionBasesNumeric
	{
		private final ConditionBase itsConditionBase;

		private ColumnConditionBasesNumericIntervals(Column theColumn)
		{
			itsConditionBase = new ConditionBase(theColumn, Operator.BETWEEN);
		}

		@Override
		final int size() { return 1; }

		// do not bother checking the supplied parameter
		@Override
		final ConditionBase get(int ignored) { return itsConditionBase; }
	}
}
