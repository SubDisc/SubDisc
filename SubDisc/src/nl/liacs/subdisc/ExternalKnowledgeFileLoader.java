package nl.liacs.subdisc;

import java.io.*;
import java.util.*;

import nl.liacs.subdisc.ConditionListBuilder.ConditionListA;

public class ExternalKnowledgeFileLoader
{
	private static final String EXTERNAL_KNOWLEDGE_STANDARD_DIR = "";
	private static final Set<String> OPERATORS; // " in ", " = ", " <= ", " >= "

	private final List<ConditionList> externalInfoLocal;
	private final List<ConditionList> externalInfoGlobal;
	private final List<String> linesLocal;
	private final List<String> linesGlobal;

	static
	{
		Set<String> s = new TreeSet<String>();

		// keep in sync with 'official' Operator-string-value
		for (Operator o : Operator.set())
			s.add(String.format(" %s ", o.GUI_TEXT));

		OPERATORS = Collections.unmodifiableSet(s);
	}

	public ExternalKnowledgeFileLoader(String theStringF)
	{
		externalInfoLocal = new ArrayList<ConditionList>();
		externalInfoGlobal = new ArrayList<ConditionList>();
		linesGlobal = new ArrayList<String>();
		linesLocal = new ArrayList<String>();

		File f = new File(theStringF);

		// load global knowledge file
		readFiles(f.listFiles(new OnlyExt("gkf")), linesGlobal);

		// load local knowledge file
		readFiles(f.listFiles(new OnlyExt("lkf")), linesLocal);

		print();
	}

	private static void readFiles(File[] theFiles, List<String> theLines)
	{
		if (theFiles.length == 0)
			return;

		// only one file is loaded for each type of knowledge
		// change to (File f : theFiles) or (i < j)
		for (int i = 0, j = theFiles.length; i < 1; ++i)
			addLinesFromFile(theFiles[i], theLines);
	}

	private static void addLinesFromFile(File theFile, List<String> theLines)
	{
		BufferedReader br = null;

		try
		{
			br = new BufferedReader(new FileReader(theFile));

			String aLine;
			while ((aLine = br.readLine()) != null)
				theLines.add(aLine);
		}
		catch (IOException e)
		{
			Log.logCommandLine("Error while reading File: " + theFile);
			if (br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e1)
				{
					Log.logCommandLine("Error while closing File: " + theFile);
				}
			}
		}
	}

	private void print()
	{
		Log.logCommandLine("\nGlobal External Knowledge:");
		for (String s : linesGlobal)
			Log.logCommandLine(s);

		Log.logCommandLine("\nLocal External Knowledge:");
		for (String s : linesLocal)
			Log.logCommandLine(s);

		Log.logCommandLine("");
	}

	public void createConditionListLocal(Table theTable)
	{
		if (externalInfoLocal.size() == 0)
			knowledgeToConditions(linesLocal, externalInfoLocal, theTable);
	}

	public void createConditionListGlobal(Table theTable)
	{
		if (externalInfoGlobal.size() == 0)
			knowledgeToConditions(linesGlobal, externalInfoGlobal, theTable);
	}

	public static List<ConditionList> knowledgeToConditions(Table theTable, List<String> theKnowledge) 
	{
		// JvR: Function used in OpenML Evaluation engine :)
		List<ConditionList> result = new ArrayList<ConditionList>();
		knowledgeToConditions(theKnowledge, result, theTable);
		return result;
	}

	private static void knowledgeToConditions(List<String> theKnowledge, List<ConditionList> theConditionLists, Table theTable)
	{
		for (String aLine : theKnowledge)
		{
			String[] aConjuncts = getConjuncts(aLine);
			ConditionList aConditionList = new ConditionList(aConjuncts.length);

			// add every conjunct to the ConditionList
			for (String conjunct : aConjuncts)
			{
				String[] sa = disect(conjunct);
				Column col = theTable.getColumn(sa[0]);
				Operator op = Operator.fromString(sa[1]);

				ConditionBase b = new ConditionBase(col, op);
				String aValue = sa[2];
				Condition aCondition;
				switch (col.getType())
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
						throw new AssertionError(col.getType());
				}

				aConditionList.add(aCondition);
			}

			theConditionLists.add(aConditionList);
			Log.logCommandLine(aConditionList.toString());
		}
	}

	private static String[] getConjuncts(String theConjunction)
	{
		// assume ' AND ' does not appear in column names
		return theConjunction.split(" AND ", -1);
	}

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

	public List<ConditionList> getLocal()
	{
		return externalInfoLocal;
	}

	public List<ConditionList> getGlobal()
	{
		return externalInfoGlobal;
	}

	////////////////////////////////////////////////////////////////////////////
	///// NEW VERSION - CLEAN AND USING ConditionListA /////////////////////////
	////////////////////////////////////////////////////////////////////////////
	// FIXME make final after testing
	private List<ConditionListA> itsExternalKnowledgeGlobal;
	private List<ConditionListA> itsExternalKnowledgeLocal;

	public ExternalKnowledgeFileLoader(Table theTable, ConditionBaseSet theConsitionBases)
	{
		List<String> g = addLinesFromFile(".gkf");
		List<String> l = addLinesFromFile(".lkf");

		print(true, g);
		print(false, l);

		itsExternalKnowledgeGlobal = knowledgeToConditions(theTable, theConsitionBases, g);
		itsExternalKnowledgeLocal  = knowledgeToConditions(theTable, theConsitionBases, l);

		// FIXME - to be removed, for testing only
		externalInfoLocal  = null;
		externalInfoGlobal = null;
		linesLocal         = null;
		linesGlobal        = null;
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
	private static List<ConditionListA> knowledgeToConditions(Table theTable, ConditionBaseSet theConditionBases, List<String> theKnowledge)
	{
		if (theKnowledge.isEmpty())
			return Collections.emptyList();

		List<ConditionBase> cbl = theConditionBases.getConditionBases();
		List<ConditionListA> cll = new ArrayList<>();

		for (String aLine : theKnowledge)
		{
			assert (!aLine.isEmpty());

			// assume ' AND ' does not appear in column names
			String[] aConjuncts = aLine.split(" AND ", -1);
			ConditionListA cl   = ConditionListBuilder.emptyList();

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

	// reuse ConditionBases as much as possible, returns null when Column not in
	// Table or Operator not used in current Mining session
	private static final ConditionBase getConditionBase(List<ConditionBase> theConditionBases, Column theColumn, Operator theOperator)
	{
		for (ConditionBase c : theConditionBases)
			if ((c.getColumn() == theColumn) && (c.getOperator() == theOperator))
				return c;

		return null;
	}

	public List<ConditionListA> getKnowledge(boolean isGlobal)
	{
		return isGlobal ? itsExternalKnowledgeGlobal : itsExternalKnowledgeLocal;
	}
}
