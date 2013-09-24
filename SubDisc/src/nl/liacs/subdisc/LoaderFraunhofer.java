package nl.liacs.subdisc;

import java.util.*;
//import java.awt.*;
import java.io.*;

public class LoaderFraunhofer
{
	private List<ConditionList> itsBeamSeed;
	private Table itsTable;
	private SearchParameters itsSearchParameters;

	// default file loader
	public LoaderFraunhofer(File theFile, Table theTable, SearchParameters theSearchParameters)
	{
//		beamseed = new CandidateQueue(theSearchParameters, new Candidate(aStart));
		itsBeamSeed = new ArrayList<ConditionList>();
		itsTable = theTable;
		itsSearchParameters = theSearchParameters;
		BufferedReader aReader = null;
		try
		{
			aReader = new BufferedReader(new FileReader(theFile));
			String aLine;
			int aLineNr = 0;

			while ((aLine = aReader.readLine()) != null)
			{
				++aLineNr;

				//Log.logCommandLine(aLine);
				ConditionList aConditionList = convertToConditionList(aLine);
				itsBeamSeed.add(aConditionList);
				//Subgroup aSubgroup = convertToSubgroup(aConditionList);

				if (aLineNr % 100 == 0)
					Log.logCommandLine(aLineNr + " lines read");
			}
			itsSearchParameters.setBeamSeed(itsBeamSeed);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (aReader != null)
					aReader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/*
	 * TODO MM
	 * KNIME and ExternalKnowledge do the same, merge code
	 *
	 * example from svn/SubgroupDiscovery/publications/Sampling/CFTP/out.txt
	 * no AND between conjunctions, just space
	 * no space in Column names
	 * space at EOL
	 * 
	 * 'spectacle-prescrip=myope age=presbyopic '
	 * 'age=young tear-prod-rate=reduced spectacle-prescrip=hypermetrope '
	 * 'age=young contact-lenses=soft spectacle-prescrip=hypermetrope '
	 */
	private ConditionList convertToConditionList(String theString)
	{
		ConditionList aConditionList = new ConditionList();
		String[] aConditions = theString.split(" ");
		for (int i=0; i<aConditions.length; i++)
		{
			String anAtom = aConditions[i];
			// assuming that CFTP delivers only equality-conditions, which it currently (July 11, 2013) does
			// and that both the column-name and value do not contain the '=' sign
			String[] aRefinement = anAtom.split("=");
			// assuming that the loaded results match the loaded dataset. This is easy to break!
			Column col = itsTable.getColumn(aRefinement[0]);
			// again, assuming that CFTP only uses the equality operator
			Operator op = Operator.EQUALS;

			// NOMINAL and BINARY are allowed, NUMERIC is not
			ConditionBase b = new ConditionBase(col, op);
			String aValue = aRefinement[1];
			Condition aCondition;
			switch (col.getType())
			{
				case NOMINAL :
					aCondition = new Condition(b, aValue);
					break;
				case NUMERIC :
					throw new AssertionError(AttributeType.NUMERIC);
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
		return(aConditionList);
	}
}
