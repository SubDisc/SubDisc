package nl.liacs.subdisc;

import java.util.*;
import java.io.*;

/**
 * A SubgroupSet is a <code>TreeSet</code> of {@link Subgroup Subgroup}s. If its
 * size is set to <= 0, the SubgroupSet has no maximum size, else the number of
 * Subgroups it can contain is limited by its size. In a nominal target setting
 * ({@link nl.liacs.subdisc.TargetConcept.TargetType TargetType}) a
 * {@link ROCList ROCList} can be obtained from this SubgroupSet to create a
 * {@link nl.liacs.subdisc.gui.ROCCurve ROCCurve} in a
 * {@link nl.liacs.subdisc.gui.ROCCurveWindow ROCCurveWindow}.
 * @see ROCList
 * @see nl.liacs.subdisc.gui.ROCCurve
 * @see nl.liacs.subdisc.gui.ROCCurveWindow
 * @see Subgroup
 */
public class SubgroupSet extends TreeSet<Subgroup>
{
	private static final long serialVersionUID = 1L;

	// For SubgroupSet in nominal target setting (used for TPR/FPR in ROCList)
	private final boolean nominalTargetSetting;
	private final int itsTotalCoverage;
	private final float itsTotalTargetCoverage;
	private final BitSet itsBinaryTarget;
	private int itsMaximumSize;
	private ROCList itsROCList;

	/*
	 * SubgroupSets' other members are only used in a nominal target setting,
	 * but still set so the members can be final.
	 */
	/**
	 * Create a SubgroupSet of a certain size.
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no
	 * maximum size.
	 */
	public SubgroupSet(int theSize)
	{
		nominalTargetSetting = false;
		itsMaximumSize = theSize;
		itsTotalCoverage = -1;
		itsBinaryTarget = null;
		itsTotalTargetCoverage = -1;
	}

	/**
	 * Creates a SubgroupSet of a certain size, but in a nominal target setting
	 * theTotalCoverage and theBinaryTarget should also be set.
	 * @param theSize the size of this SubgroupSet, use theSize <= 0 for no
	 * maximum size.
	 * @param theTotalCoverage the total number of instances in the data (number
	 * of rows in the {@link Table Table}).
	 * @param theBinaryTarget a <code>BitSet</code> with <code>bit</code>s set
	 * for the instances covered by the target value.
	 */
	public SubgroupSet(int theSize, int theTotalCoverage, BitSet theBinaryTarget)
	{
		nominalTargetSetting = true;
		itsMaximumSize = theSize;
		itsTotalCoverage = theTotalCoverage;
		itsBinaryTarget = theBinaryTarget;

		if (theTotalCoverage <= 0)
			Log.logCommandLine("SubgroupSet constructor(): theTotalCoverage = '"
								+ theTotalCoverage + "', but can not be <= 0");

		if (itsBinaryTarget == null)
		{
			itsTotalTargetCoverage = -1.0f;
			Log.logCommandLine("SubgroupSet constructor() ERROR");	// TODO this gives an error when running SINGLE_NUMERIC
		}
		else
			itsTotalTargetCoverage = (float)theBinaryTarget.cardinality();
	}

	/**
	 * Creates a SubgroupSet just like the argument, except empty.
	 */
	public SubgroupSet(SubgroupSet theOriginal)
	{
		nominalTargetSetting = theOriginal.nominalTargetSetting;
		itsMaximumSize = theOriginal.itsMaximumSize;
		itsTotalCoverage = theOriginal.itsTotalCoverage;
		itsBinaryTarget = theOriginal.itsBinaryTarget;
		itsTotalTargetCoverage = theOriginal.itsTotalTargetCoverage;
		itsROCList = theOriginal.itsROCList;
	}

	/**
	 * Only the top result is needed in this setting. Setting maximum size
	 * to 1 saves memory and insertion lookup time (Olog(n) for Java's 
	 * red-black tree implementation of TreeSet).
	 */
	protected void useSwapRandomisationSetting() {
		itsMaximumSize = 1;
	}

	/**
	 * Tries to add the {@link Subgroup Subgroup} passed in as parameter to this
	 * SubgroupSet. Also ensures this SubgroupSet never exceeds its maximum size
	 * (if one is set).
	 * @param theSubgroup theSubgroup to add to this SubgroupSet.
	 * @return <code>true</code> if this SubgroupSet did not already contain the
	 * specified SubGroup, <code>false</code> otherwise and if the Subgroup is
	 * <code>null</code>.
	 */
	@Override
	public boolean add(Subgroup theSubgroup)
	{
		if (theSubgroup == null)
			return false;
		else
		{
			boolean aResult = super.add(theSubgroup);
			if ((itsMaximumSize > 0) && (size() > itsMaximumSize))
				remove(last());
			
			return aResult;
		}
	}

	public Subgroup getBestSubgroup() { return first(); }

	public void setIDs()
	{
		int aCount = 1;
		for(Subgroup s : this)
			s.setID(aCount++);
	}

	public void print()
	{
		for (Subgroup s : this)
			Log.logCommandLine(String.format("%d,%d,%d",
												s.getID(),
												s.getCoverage(),
												s.getMeasureValue()));
	}

	public void saveExtent(BufferedWriter theWriter, Table theTable, BitSet theSubset, TargetConcept theTargetConcept)
	{
		Log.logCommandLine("saving extent...");
		try
		{
			for (int i = 0, j = theTable.getNrRows(); i < j; ++i)
			{
				// 5 + aNrRows*(,1) + 2*float
				StringBuilder aRow = new StringBuilder(j*2 + 100);
				aRow.append(theSubset.get(i) ? "train":"test ");
				//aRow.append(",");
				//aRow.append(i);

				//add subgroup extents to current row
				for (Subgroup aSubgroup: this)
					aRow.append(aSubgroup.getMembers().get(i) ? ",1" : ",0");

				//add targets

				Column aPrimaryAttribute = theTargetConcept.getPrimaryTarget(); //almost always there is a primary target
				switch (theTargetConcept.getTargetType())
				{
					case SINGLE_NOMINAL :
					{
						aRow.append(",");
						aRow.append(aPrimaryAttribute.getNominal(i));
						break;
					}
					case SINGLE_NUMERIC :
					{
						aRow.append(",");
						aRow.append(aPrimaryAttribute.getFloat(i));
						break;
					}
					case DOUBLE_REGRESSION :
					case DOUBLE_CORRELATION :
					{
						aRow.append(",");
						aRow.append(aPrimaryAttribute.getFloat(i));
						aRow.append(",");
						aRow.append(theTargetConcept.getSecondaryTarget().getFloat(i));
						break;
					}
					case MULTI_LABEL :
					{
						for (Column aTarget: theTargetConcept.getMultiTargets())
							aRow.append(aTarget.getBinary(i) ? ",1" : ",0");
						break;
					}
				}

				//Log.logCommandLine(aRow.toString());
				theWriter.write(aRow.append("\n").toString());
			}
		}
		catch (IOException e)
		{
			Log.logCommandLine("Error on file: " + e.getMessage());
		}
	}

	/*
	 * ROCList functions.
	 * TODO update a single ROCList instance?
	 */
	/**
	 * Returns a <b>copy of</b> this SubgroupSets' BinaryTarget
	 * <code>BitSet</code>. SubgroupSets only have a BinaryTarget
	 * <code>BitSet<code> in a nominal target setting, meaning the
	 * {@link nl.liacs.subdisc.Attribute.AttributeType AttributeType} of the
	 * PrimaryTarget in the {@link TargetConcept TargetConcept} is of type
	 * AttributeType.NOMINAL.
	 *
	 * @return a clone of this SubgroupSets' BinaryTarget <code>BitSet</code>,
	 * or <code>null</code> if this SubgroupSet has no BinaryTarget
	 * <code>BitSet</code>.
	 */
	public BitSet getBinaryTargetClone()
	{
		if (!nominalTargetSetting || itsBinaryTarget == null)
			return null;
		else
			return (BitSet) itsBinaryTarget.clone();
	}
	public int getTotalCoverage() { return itsTotalCoverage; }
	public float getTotalTargetCoverage() { return itsTotalTargetCoverage; }




	public SubgroupSet postProcess(SearchStrategy theSearchStrategy)
	{
		if (theSearchStrategy != SearchStrategy.COVER_BASED_BEAM_SELECTION) //only valid for COVER_BASED_BEAM_SELECTION
			return this;

		int aSize = 100; //TODO
		Log.logCommandLine("subgroups found: " + size());
		SubgroupSet aResult = new SubgroupSet(this); //make empty copy
		int aLoopSize = Math.min(aSize, size());
		BitSet aUsed = new BitSet(size());
		for (int i=0; i<aLoopSize; i++)
		{
			Log.logCommandLine("loop " + i);
			Subgroup aBest = null;
			double aMaxQuality = Float.NEGATIVE_INFINITY;
			int aCount = 0;
			int aChosen = 0;
			for (Subgroup aSubgroup : this)
			{
				if (!aUsed.get(aCount)) //is this one still available
				{
					double aQuality = computeMultiplicativeWeight(aResult, aSubgroup) * aSubgroup.getMeasureValue();
					if (aQuality > aMaxQuality)
					{
						aMaxQuality = aQuality;
						aBest = aSubgroup;
						aChosen = aCount;
					}
				}
				aCount++;
			}
			Log.logCommandLine("best (" + aChosen + "): " + aBest.getMeasureValue() + ", " + computeMultiplicativeWeight(aResult, aBest) + ", " + aMaxQuality + "\n");
			aUsed.set(aChosen, true);
			aResult.add(aBest);
		}

		Log.logCommandLine("========================================================");
		Log.logCommandLine("used: " + aUsed.toString());
		for (Subgroup aSubgroup : aResult)
			Log.logCommandLine("result: " + aSubgroup.getMeasureValue());
		return aResult;
	}

	/**
	* Computes the cover count of a particular example: the number of times this example is a member of a subgroup
	* See van Leeuwen & Knobbe, ECML PKDD 2011
	*/
	public int computeCoverCount(SubgroupSet theSet, int theRow)
	{
		int aResult = 0;
		for (Subgroup aSubgroup : theSet)
		{
			if (aSubgroup.covers(theRow))
				aResult++;
		}
		return aResult;
	}

	/**
	* Computes the multiplicative weight of a subgroup \n
	* See van Leeuwen & Knobbe, ECML PKDD 2011.
	*/
	public double computeMultiplicativeWeight(SubgroupSet theSet, Subgroup theSubgroup)
	{
		double aResult = 0;
		double anAlpha = 0.9;
		BitSet aMember = theSubgroup.getMembers();

		for(int i=aMember.nextSetBit(0); i>=0; i=aMember.nextSetBit(i+1))
		{
			int aCoverCount = computeCoverCount(theSet, i);
			aResult += Math.pow(anAlpha, aCoverCount);
		}
		return aResult/theSubgroup.getCoverage();
	}




	/**
	 * Returns a new {@link ROCList ROCList}. If {@link Subgroup Subgroup}s are
	 * removed from this SubgroupSet, this new ROCList reflects these changes.
	 * This method only returns a ROCList in a nominal target setting, meaning
	 * the {@link nl.liacs.subdisc.Attribute.AttributeType AttributeType} of the
	 * PrimaryTarget in the {@link TargetConcept TargetConcept} is of type
	 * AttributeType.NOMINAL.
	 *
	 * @return a ROCList, or <code>null</code> if not in a nominal target
	 * setting.
	 */
	public ROCList getROCList()
	{
		if (!nominalTargetSetting || itsBinaryTarget == null)
			return null;
		else
		{
			itsROCList = new ROCList(this);
			return itsROCList;
		}
	}

	/*
	 * solely for ROCCurveWindow
	 * extremely inefficient, should be member of ROCList
	 * could be more efficient when first ordering ROCList
	 * but most ROC code should change as it is overly complex
	 */
	public static final Object[] ROC_HEADER = { "ID", "FPR", "TPR", "Conditions" };
	public Object[][] getROCListSubgroups()
	{
		int aSize = itsROCList.size();
		Object[][] aSubgroupList = new Object[aSize][ROC_HEADER.length];

		for (int i = 0, j = aSize; i < j; ++i)
		{
			SubgroupROCPoint p = itsROCList.get(i);
			Subgroup s;
			Iterator<Subgroup> it = iterator();

			while ((s = it.next()).getID() < p.ID);

			aSubgroupList[i] =
				new Object[] { s.getID(),
						p.getFPR(),
						p.getTPR(),
						s.getConditions().toString() };
		}

		return aSubgroupList;
	}
}
