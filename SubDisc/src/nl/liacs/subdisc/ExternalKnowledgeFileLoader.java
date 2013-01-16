package nl.liacs.subdisc;

import java.io.*;
import java.util.*;

public class ExternalKnowledgeFileLoader
{
	private List<ConditionList> externalInfoLocal;
	private List<ConditionList> externalInfoGlobal;
	private List<String> linesLocal;
	private List<String> linesGlobal;

	public ExternalKnowledgeFileLoader(String theStringF) throws IOException
	{
		externalInfoLocal = new ArrayList<ConditionList>();
		externalInfoGlobal = new ArrayList<ConditionList>();
		linesGlobal = new ArrayList<String>();
		linesLocal = new ArrayList<String>();

		File f = new File(theStringF);
		FilenameFilter extLocalKnowledge = new OnlyExt("lkf");
		FilenameFilter extGlobalKnowledge = new OnlyExt("gkf");
		File[] fileGlobal = f.listFiles(extGlobalKnowledge);
		File[] fileLocal = f.listFiles(extLocalKnowledge);

		if (fileGlobal.length > 0)
		{
			FileReader aFr = new FileReader(fileGlobal[0]);
			BufferedReader in = new BufferedReader(aFr);

			String conjunction = null;

			while ((conjunction = in.readLine()) != null)
			{
				System.out.println(conjunction);
				linesGlobal.add(conjunction);
				System.out.println(linesGlobal);
			}
		}

		if (fileLocal.length > 0)
		{
			FileReader aFr = new FileReader(fileLocal[0]);
			BufferedReader in = new BufferedReader(aFr);
			String conjunction = null;

			while ((conjunction = in.readLine()) != null)
				linesLocal.add(conjunction);
		}
	}

	public void createConditionListLocal(Table theTable)
	{
		for (int i=0; i<linesLocal.size(); i++)
		{
			String conjunction = linesLocal.get(i);
			String[] conjuncts = getConjuncts(conjunction);
			ConditionList cl = new ConditionList();
			for (int j=0; j<conjuncts.length; j++)
			{
				//fill conditionlist with conditions here, first create object by column and operator, then set value
				String[] sa = disect(conjuncts[j]);
				Column col = theTable.getColumn(sa[0]); //the column
				//now get the operator
				// int ELEMENT_OF		= 1;
				// int EQUALS			= 2;
				// int LESS_THAN_OR_EQUAL	= 3;
				// int GREATER_THAN_OR_EQUAL	= 4;
				// int BETWEEN = 5;
				// int NOT_AN_OPERATOR		= 99;
//				int op = 99;
//				if (sa[1].compareTo("=")==0){
//					op = 2;
//				}else if(sa[1].compareTo("!=")==0){
//					op = 99;
//				}else if(sa[1].compareTo("<=")==0){
//					op = 3;
//				}else if(sa[1].compareTo(">=")==0){
//					op = 4;
//				}else if(sa[1].compareTo("in")==0){
//					op =1;
//				}
				Operator o = Operator.fromString(sa[1]);
				System.out.println("operator:");
//				System.out.println(op);
				System.out.println(o);
				System.out.println(sa[1]);
//				Condition c = new Condition(col,op);
				Condition c = new Condition(col, o);
				//now set the value of the condition
				System.out.println("Value");
				System.out.println(sa[2]);
				c.setValue(sa[2]);
				cl.add(c);
			}

			externalInfoLocal.add(cl);
		}
	}

	public void createConditionListGlobal(Table theTable)
	{
		for (int i=0; i<linesGlobal.size(); i++)
		{
			String conjunction = linesGlobal.get(i);
			String[] conjuncts = getConjuncts(conjunction);
			ConditionList cl = new ConditionList();
			for (int j=0; j<conjuncts.length; j++)
			{
				//fill conditionlist with conditions here, first create object by column and operator, then set value
				String[] sa = disect(conjuncts[j]);
				Column col = theTable.getColumn(sa[0]); //the column
				//now get the operator
				// int ELEMENT_OF		= 1;
				// int EQUALS			= 2;
				// int LESS_THAN_OR_EQUAL	= 3;
				// int GREATER_THAN_OR_EQUAL	= 4;
				// int BETWEEN = 5;
				// int NOT_AN_OPERATOR		= 99;
//				int op = 99;
//				if (sa[1].compareTo("=")==0){
//					op = 2;
//				}else if(sa[1].compareTo("!=")==0){
//					op = 99;
//				}else if(sa[1].compareTo("<=")==0){
//					op = 3;
//				}else if(sa[1].compareTo(">=")==0){
//					op = 4;
//				}else if(sa[1].compareTo("in")==0){
//					op =1;
//				}
//				Condition c = new Condition(col,op);
				Condition c = new Condition(col, Operator.fromString(sa[1]));
				//now set the value of the condition

				c.setValue(sa[2]);
				cl.add(c);
			}

			externalInfoGlobal.add(cl);
		}
	}

	private static String[] getConjuncts(String conjunction)
	{
		// assume ' AND ' does not appear in column names
		return conjunction.split(" AND ", -1);
	}

	// " in ", " = ", " <= ", " >= ", 
	private static final String[] OPERATORS = getOperatorStrings();

	// keep in sync with 'official' Operator-string-values 
	private static String[] getOperatorStrings()
	{
		final ArrayList<String> aList = new ArrayList<String>();
		for (Operator o : Operator.set())
		{
			if (o == Operator.NOT_AN_OPERATOR)
				continue;

			final String s = new StringBuilder(4)
							.append(" ")
							.append(o.GUI_TEXT)
							.append(" ")
							.toString();
			if (!aList.contains(s))
				aList.add(s);
		}

		return aList.toArray(new String[0]);
	}

	// TODO mapping a Condition back to its constituents should be made a
	// Condition.method().
	private static String[] disect(String condition)
	{
		// assume OPERATORS do not appear in column name
		for (String s : OPERATORS)
		{
			if (condition.contains(s))
			{
				final String[] tmp = condition.split(s);
				// remove outer quotes from column name
//				tmp[0] = tmp[0].substring(1, tmp[0].length()-1);
				if (tmp[1].startsWith("'") && tmp[1].endsWith("'"))
					tmp[1] = tmp[1].substring(1, tmp[1].length()-1);
				return new String[] { tmp[0] , s.trim(), tmp[1] };
			}
		}

		return null; // throw Exception
	}

	public List<ConditionList> getLocal()
	{
		return externalInfoLocal;
	}

	public List<ConditionList> getGlobal()
	{
		return externalInfoGlobal;
	}
}
