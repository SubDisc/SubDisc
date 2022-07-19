package nl.liacs.subdisc;

import java.io.*;
import java.util.*;

import nl.liacs.subdisc.ConditionListBuilder.ConditionList;

public class ExternalKnowledgeFileLoader
{
	private static final String EXTERNAL_KNOWLEDGE_STANDARD_DIR = "";
	private static final Set<String> OPERATORS; // " in ", " = ", " <= ", " >= "

	static
	{
		Set<String> s = new TreeSet<String>();

		// keep in sync with 'official' Operator-string-value
		for (Operator o : Operator.set())
			s.add(String.format(" %s ", o.GUI_TEXT));

		OPERATORS = Collections.unmodifiableSet(s);
	}

	// FIXME make final after testing
	private List<ConditionList> itsExternalKnowledgeGlobal;
	private List<ConditionList> itsExternalKnowledgeLocal;

	public ExternalKnowledgeFileLoader(Table theTable, ConditionBaseSet theConsitionBases)
	{
		List<String> g = addLinesFromFile(".gkf");
		List<String> l = addLinesFromFile(".lkf");

		print(true, g);
		print(false, l);

		itsExternalKnowledgeGlobal = knowledgeToConditions(theTable, theConsitionBases, g);
		itsExternalKnowledgeLocal  = knowledgeToConditions(theTable, theConsitionBases, l);
	}

	private static final List<String> addLinesFromFile(final String theExtention)
	{
		File[] fa = new File(EXTERNAL_KNOWLEDGE_STANDARD_DIR).getAbsoluteFile().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(theExtention);
			}
		});

		// only one file is loaded for each type of knowledge
		// MM: the above is how the method was originally implemented, keep it
		if (fa.length == 0)
			Collections.emptyList();

		List<String> l = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(fa[0])))
		{
			String aLine;
			while (((aLine = br.readLine()) != null) && !aLine.isEmpty())
				l.add(aLine);
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }

		return l;
	}

	private static final void print(boolean isGlobal, List<String> theLines)
	{
		Log.logCommandLine(isGlobal ? "\nGlobal External Knowledge:" : "Local External Knowledge:");
		for (String s : theLines)
			Log.logCommandLine(s);
		Log.logCommandLine("");
	}

	// TODO merge with KNIME and LoaderFraunHofer code
	private static List<ConditionList> knowledgeToConditions(Table theTable, ConditionBaseSet theConditionBases, List<String> theKnowledge)
	{
		if (theKnowledge.isEmpty())
			return Collections.emptyList();

		List<ConditionBase> cbl = theConditionBases.getConditionBases();
		List<ConditionList> cll = new ArrayList<>();

		for (String aLine : theKnowledge)
		{
			assert (!aLine.isEmpty());

			// assume ' AND ' does not appear in column names
			String[] aConjuncts = aLine.split(" AND ", -1);
			ConditionList cl   = ConditionListBuilder.emptyList();

			// add every conjunct to the ConditionList
			for (String s : aConjuncts)
			{
				// FIXME check: should always be of length 3
				String[] sa = disect(s);
				// FIXME check: should exist
				Column c    = theTable.getColumn(sa[0]);
				// FIXME check: should success
				Operator o  = Operator.fromString(sa[1]);

				ConditionBase cb = getConditionBase(cbl, c, o);
				ConditionBase b = ((cb == null) ? cb : new ConditionBase(c, o));

				String aValue   = sa[2];
				final Condition aCondition;
				switch (c.getType())
				{
					case NOMINAL :
						aCondition = new Condition(b, aValue);
						break;
					case NUMERIC :
						// Column data unknown, so can not set sort index
						aCondition = new Condition(b, Float.parseFloat(aValue), Condition.UNINITIALISED_SORT_INDEX);
						break;
					case ORDINAL :
						throw new AssertionError(AttributeType.ORDINAL);
					case BINARY :
						if (!AttributeType.isValidBinaryValue(aValue))
							throw new IllegalArgumentException(aValue + " is not a valid BINARY value");
						aCondition = new Condition(b, AttributeType.isValidBinaryTrueValue(aValue));
						break;
					default :
						throw new AssertionError(c.getType());
				}
				cl = ConditionListBuilder.createList(cl, aCondition);

				if (cb == null)
					Log.logCommandLine(String.format("Irrelevant knowledge: '%s' in '%s'", aCondition, aLine));
			}

			cll.add(cl);
			// FIXME kept for historic purposed / testing only -> REMOVE
			Log.logCommandLine("ADDED=" + cl.toString());
		}

		return Collections.unmodifiableList(cll);
	}

///// COPY - COPY - COPY - COPY - COPY - COPY - COPY - COPY - COPY - COPY //////
	// TODO mapping a Condition back to its constituents should be made a
	// Condition.method().
	private static String[] disect(String theCondition)
	{
		// assume OPERATORS do not appear in column name
		for (String s : OPERATORS)
		{
			if (theCondition.contains(s))
			{
				final String[] tmp = theCondition.split(s);
				// remove outer quotes from column name
//				tmp[0] = tmp[0].substring(1, tmp[0].length()-1);
				if (tmp[1].startsWith("'") && tmp[1].endsWith("'"))
					tmp[1] = tmp[1].substring(1, tmp[1].length()-1);
				return new String[] { tmp[0] , s.trim(), tmp[1] };
			}
		}

		throw new IllegalArgumentException(ExternalKnowledgeFileLoader.class.getSimpleName() + " can not parse: " + theCondition);
	}
////////////////////////////////////////////////////////////////////////////////

	// reuse ConditionBases as much as possible, returns null when Column not in
	// Table or Operator not used in current Mining session
	private static final ConditionBase getConditionBase(List<ConditionBase> theConditionBases, Column theColumn, Operator theOperator)
	{
		for (ConditionBase c : theConditionBases)
			if ((c.getColumn() == theColumn) && (c.getOperator() == theOperator))
				return c;

		return null;
	}

	public List<ConditionList> getKnowledge(boolean isGlobal)
	{
		return isGlobal ? itsExternalKnowledgeGlobal : itsExternalKnowledgeLocal;
	}
}
