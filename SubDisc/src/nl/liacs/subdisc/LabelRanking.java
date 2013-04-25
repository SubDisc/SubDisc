package nl.liacs.subdisc;

public class LabelRanking
{
	private final int[] itsRanking;
	private final int itsSize;
	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

	//labels are numbered from 0 to k-1
	//rankings are provided as input with a string of letters. 'a' corresponds to rank 0
	//ties are not implemented properly yet

	public LabelRanking(String theString)
	{
		itsSize = theString.length();
		itsRanking = new int[itsSize];
		for (int i=0; i<itsSize; i++)
			itsRanking[i] = Character.getNumericValue(theString.charAt(i)) - 10; //0 means 'a'
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
		String aString = "[";
		for (int i=0; i<itsSize; i++)
			aString += getLabel(getRank(i));
		return aString + "]";
	}

	static public String getLabel(int theLabel)
	{
		return ALPHABET.substring(theLabel, theLabel+1);
	}

	public final int getRank(int anIndex)	{ return itsRanking[anIndex]; }
	public void setRank(int anIndex, int aRank)	{ itsRanking[anIndex] = aRank; }
	public final int getSize() { return itsSize; }
}
