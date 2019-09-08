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

		NumericOperatorSetting n = theSearchParameters.getNumericOperatorSetting();
		boolean isInterval = (n == NumericOperatorSetting.NUMERIC_INTERVALS);
		// BestInterval only allowed for SINGLE_NOMINAL
		assert (!isInterval || (tc.getTargetType() == TargetType.SINGLE_NOMINAL));

		EnumSet<Operator> o = n.getOperators();
		boolean useEquals = (!isInterval && o.contains(Operator.EQUALS));
		boolean useLeq    = (!isInterval && o.contains(Operator.LESS_THAN_OR_EQUAL));
		boolean useGeq    = (!isInterval && o.contains(Operator.GREATER_THAN_OR_EQUAL));
		// special case for NUMERIC_EQ, when used with (BEST_)BINS: use BETWEEN
		NumericStrategy s = theSearchParameters.getNumericStrategy();
		assert (!s.isDiscretiser() || ((s == NumericStrategy.NUMERIC_BEST_BINS) || (s == NumericStrategy.NUMERIC_BINS)));
		Operator e = (!useEquals ? null : (s.isDiscretiser() ? Operator.BETWEEN : Operator.EQUALS));

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
					if (!isInterval)
						result.add(FACTORY.new ColumnConditionBasesNumericRegular(c, e, useLeq, useGeq));
					else
						result.add(FACTORY.new ColumnConditionBasesNumericIntervals(c));
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

		// do not bother checking the supplied parameter
		@Override
		public final ConditionBase get(int ignored) { return itsConditionBase; }
	}

	static { assert (regularIsEqualsLeqGeq()); }
	private static final boolean regularIsEqualsLeqGeq()
	{
		EnumSet<Operator> e = EnumSet.noneOf(Operator.class);
		EnumSet<NumericOperatorSetting> nos = EnumSet.allOf(NumericOperatorSetting.class);
		nos.remove(NumericOperatorSetting.NUMERIC_INTERVALS);

		for (NumericOperatorSetting n : nos)
			e.addAll(n.getOperators());

		return EnumSet.of(Operator.EQUALS, Operator.LESS_THAN_OR_EQUAL, Operator.GREATER_THAN_OR_EQUAL).equals(e);
	}

	// wastes some bytes as not all member fields are always populated, but the
	// design is simplified and there are only few (shared) ColumnConditionBases
	final class ColumnConditionBasesNumericRegular extends ColumnConditionBases
	{
		private final ConditionBase itsConditionBaseEquals;
		private final ConditionBase itsConditionBaseLEQ;
		private final ConditionBase itsConditionBaseGEQ;

		private ColumnConditionBasesNumericRegular(Column theColumn, Operator equals, boolean useLeq, boolean useGeq)
		{
			itsConditionBaseEquals = equals != null ? new ConditionBase(theColumn, equals)                         : null;
			itsConditionBaseLEQ    = useLeq         ? new ConditionBase(theColumn, Operator.LESS_THAN_OR_EQUAL)    : null;
			itsConditionBaseGEQ    = useGeq         ? new ConditionBase(theColumn, Operator.GREATER_THAN_OR_EQUAL) : null;
		}

		@Override
		final ConditionBase get(int zeroOrOneOrTwo)
		{
			int i = zeroOrOneOrTwo;
			assert ((i == 0) || i == 1 || i == 2);
			return (i == 0 ? itsConditionBaseEquals : (i == 1 ? itsConditionBaseLEQ : itsConditionBaseGEQ));
		}
	}

	static { assert (NumericOperatorSetting.NUMERIC_INTERVALS.getOperators().equals(EnumSet.of(Operator.BETWEEN))); }
	final class ColumnConditionBasesNumericIntervals extends ColumnConditionBases
	{
		private final ConditionBase itsConditionBase;

		private ColumnConditionBasesNumericIntervals(Column theColumn)
		{
			itsConditionBase = new ConditionBase(theColumn, Operator.BETWEEN);
		}

		// do not bother checking the supplied parameter
		@Override
		final ConditionBase get(int ignored) { return itsConditionBase; }
	}
}
