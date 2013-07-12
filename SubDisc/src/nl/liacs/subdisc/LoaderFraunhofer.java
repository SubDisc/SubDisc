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
			itsSearchParameters.setBeamSeedLoaded(true);
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

	private ConditionList convertToConditionList(String theString)
	{
		ConditionList aConditionList = new ConditionList();
		String[] aConditions = theString.split(" ");
		for (int i=0; i<aConditions.length; i++)
		{
			String anAtom = aConditions[i];
			// assuming that CFTP delivers only equality-conditions, which it currently (July 11, 2013) does
			String[] aRefinement = anAtom.split("=");
			// assuming that the loaded results match the loaded dataset. This is easy to break!
			Column col = itsTable.getColumn(aRefinement[0]);
			// again, assuming that CFTP only uses the equality operator
			Operator op = Operator.EQUALS;

			// create Condition using Column and Operator
			Condition aCondition = new Condition(col, op);
			// set Condition value
			aCondition.setValue(aRefinement[1]);
			aConditionList.add(aCondition);
		}
		return(aConditionList);
	}
}
