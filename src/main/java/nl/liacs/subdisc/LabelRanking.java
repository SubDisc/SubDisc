package nl.liacs.subdisc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class LabelRanking
{
	private final int[] itsRanking;
	private static String[] itsIndex;
	private final int itsSize;
	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

	//labels are numbered from 0 to k-1
	//rankings are provided as input with a string of letters. 'a' corresponds to rank 0
	//ties are not implemented properly yet

	public LabelRanking(String theString)
	{
		String[] thePrefString = theString.split(">");
		int itsPrefSize = thePrefString.length;

		itsSize = theString.replace(">","").length();


		itsRanking = new int[itsSize];
		for (int i=0; i<itsPrefSize; i++)
		{
			String theSubString = thePrefString[i];
			for(int j = 0; j < theSubString.length(); j++)
			{
				//Log.logCommandLine("substring:" + theSubString + "-" + theSubString.charAt(j));
				int anIndex = Math.max(0, Character.getNumericValue(theSubString.charAt(j)) - 10); //0 means 'a'
				anIndex = Math.min(anIndex, itsSize-1); // why is this needed?
				itsRanking[anIndex] = i;

				//Log.logCommandLine("ranking:" + i + " index: " + anIndex);
			}
		}

		itsIndex = new String[itsSize];
	}

	final public float kendallTau(LabelRanking anLR)
	{
		int aConcordant = 0;
		int aDiscordant = 0;

		float aNumer = 0;
		for (int i=1; i<itsSize; i++)
			for (int j=0; j<=i-1; j++)
			{
//				Log.logCommandLine("rank " + getRank(i) + "," + getRank(j) + "  " + anLR.getRank(i) + "," + anLR.getRank(j));
				aNumer += Math.signum((float) getRank(i) - getRank(j)) * Math.signum((float) anLR.getRank(i) - anLR.getRank(j));
			}
		return 2 * aNumer / (itsSize*(itsSize-1));
	}

	final public void print()
	{
		Log.logCommandLine("ranking:" + getRanking());
	}

	final public String getRanking()
	{
		String aString = "";

		if (makeIndex()) //sanity check for robustness
			for (int i=0; i<itsSize; i++)
			{
				
				aString += itsIndex[i];
				if (itsIndex[i] != "" && i < (itsSize - 1))
					aString += ">";
			}
		else
			aString = "(incorrect ranking target)";

		return aString;
	}

	public boolean makeIndex()
	{
		Arrays.fill(itsIndex, "");
		for (int i=0; i<itsSize; i++)
		{
			int anIndex = itsRanking[i];
			if (anIndex>=0 && anIndex<itsIndex.length) //this should normally not be necessary, but the target column may be mall-formed
				itsIndex[anIndex] += getLetter(i);
			else
			{
				Log.logCommandLine("This target column doesn't seem to be a correct label ranking.");
				return false; //failed to complete
			}
		}
		return true;
	}

	static public String getLetter(int theLabel)
	{
		return ALPHABET.substring(theLabel, theLabel+1);
	}
	static public String getLabel(int theLabel)
	{
		return itsIndex[theLabel];
	}

	public final int getRank(int anIndex)
	{
		if (anIndex >= 0 && anIndex <itsSize) //not all columns are necessarily well-formed, so this is just to add robustness
			return itsRanking[anIndex];
		else
			return 0;
	}

	public void setRank(int anIndex, int aRank)
	{
		itsRanking[anIndex] = aRank;
	}
	public final int getSize() { return itsSize; }
}
