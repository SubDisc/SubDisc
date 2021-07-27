package nl.liacs.subdisc;

import java.util.*;

public enum Operator
{
	ELEMENT_OF("in")
	{
		@Override
		public boolean isValidFor(AttributeType theType)
		{
			return theType == AttributeType.NOMINAL;
		}
	},
	// XXX DEPTH_FIRST relies on EQUALS being before LEQ / GEQ
	EQUALS("=")
	{
		@Override
		public boolean isValidFor(AttributeType theType)
		{
			return theType == AttributeType.NOMINAL ||
				theType == AttributeType.NUMERIC ||
				// ORDINAL is not implemented -> do not use
				// all code should be checked when it is
				theType == AttributeType.BINARY;
		}
	},
	LESS_THAN_OR_EQUAL("<=")
	{
		@Override
		public boolean isValidFor(AttributeType theType)
		{
			// ORDINAL is not implemented -> do not use
			// all code should be checked when it is
			return theType == AttributeType.NUMERIC;
		}
	},
	GREATER_THAN_OR_EQUAL(">=")
	{
		@Override
		public boolean isValidFor(AttributeType theType)
		{
			// ORDINAL is not implemented -> do not use
			// all code should be checked when it is
			return theType == AttributeType.NUMERIC;
		}
	},
	BETWEEN("in")
	{
		@Override
		public boolean isValidFor(AttributeType theType)
		{
			// ORDINAL is not implemented -> do not use
			// all code should be checked when it is
			return theType == AttributeType.NUMERIC;
		}
	};

	public final String GUI_TEXT;

	private Operator(String theGuiText)
	{
		GUI_TEXT = theGuiText;
	}

	// abstract to enforce implementation for each (new) Operator
	/**
	 * Indicates whether this Operator is valid for the supplied
	 * {@link AttributeType}.
	 * 
	 * @return {@code true} if this {@code Operator} is valid for the
	 * supplied {@link AttributeType}.
	 */
	abstract public boolean isValidFor(AttributeType theType);

	@Override
	public String toString()
	{
		return GUI_TEXT;
	}

	/**
	 * Returns the <code>Operator</code> for the supplied argument, or
	 * <code>Operator,NOT_AN_OPERATOR</code> if the argument can not be
	 * mapped to an <code>Operator</code>.
	 * <p>
	 * 
	 * @param theText The <code>GUI_TEXT</code> of an <code>Operator</code>.
	 * 
	 * @return An <code>Operator</code>.
	 */
	public static Operator fromString(String theText)
	{
		for (Operator o : Operator.values())
			if (o.GUI_TEXT.equals(theText))
				return o;

		throw new IllegalArgumentException("Operator.formString(), unknown Operator: " + theText);
	}

	/**
	 * Returns all <code>Operators</code> as an immutable
	 * <code>EnumSet</code>.
	 * 
	 * @return An <code>EnumSet</code> of all <code>Operators</code>.
	 */
	public static Set<Operator> set()
	{
		final EnumSet<Operator> set = EnumSet.noneOf(Operator.class);
		for (Operator o : Operator.values())
			set.add(o);
		return Collections.unmodifiableSet(set);
	}

	// for future code safety
	public static Set<Operator> getOperators(AttributeType theAttributeType)
	{
		final EnumSet<Operator> set = EnumSet.noneOf(Operator.class);
		for (Operator o : Operator.values())
			if (o.isValidFor(theAttributeType))
				set.add(o);
		return Collections.unmodifiableSet(set);
	}

	public boolean isSimple() // is it =, <= or >=?
	{
		return (this == EQUALS || this == LESS_THAN_OR_EQUAL || this == GREATER_THAN_OR_EQUAL);
	}
}
