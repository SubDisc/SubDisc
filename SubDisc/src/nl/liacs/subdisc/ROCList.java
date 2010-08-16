package nl.liacs.subdisc;

import java.util.ArrayList;
import java.util.BitSet;

public class ROCList extends ArrayList<Subgroup>
{
	private static final long serialVersionUID = 1L;
	private final BitSet itsBinaryTarget;
	private final int itsTotalCoverage;
	private final float itsTotalTargetCoverage;

	public ROCList(int theNrRows, BitSet theBinaryTarget)
	{
		itsBinaryTarget = theBinaryTarget;
		itsTotalCoverage = theNrRows;	// TODO itsBinaryTarget.size() always n*2^8
		itsTotalTargetCoverage = (float)itsBinaryTarget.cardinality();
	}

	public boolean add(Subgroup theSubgroup)
	{
		int anIndex = size();

		float aTPR = getTruePositiveRate(theSubgroup);
		float aFPR = getFalsePositiveRate(theSubgroup);

		if (aTPR <= aFPR) //always under curve
			return false;

		super.add(theSubgroup);
		if (size() > 1)
		{
/*
			//move new rule from end to correct position
			while ((anIndex > 0) && (getFalsePositiveRateAt(anIndex - 1) >= aFPR))
			{
				Rule aRule = getRule(anIndex - 1);
				set(anIndex - 1, theRule);
				set(anIndex, aRule);
				anIndex--;
			}

			if (getFalsePositiveRateAt(anIndex + 1) == getFalsePositiveRateAt(anIndex))
			{
				if (getTruePositiveRateAt(anIndex + 1) > getTruePositiveRateAt(anIndex))
					remove(anIndex);
				else
					remove(anIndex + 1);
			}

			if (getSlopeAt(anIndex - 1) <= getSlopeAt(anIndex))
			{
				remove(anIndex);
//				return false;
			}
			while ((anIndex > 0) && (getSlopeAt(anIndex - 2) <= getSlopeAt(anIndex - 1)))
			{
				remove(anIndex - 1);
				anIndex--;
			}
			while ((anIndex < size() - 1) && (getSlopeAt(anIndex) <= getSlopeAt(anIndex + 1)))
			{
				remove(anIndex + 1);
			}
*/
		}
		return true;
	}

	private float getTruePositiveRate(Subgroup theSubgroup)
	{
		BitSet tmp = (BitSet) itsBinaryTarget.clone();
		tmp.and(theSubgroup.getMembers());
		int aHeadBody = tmp.cardinality();

		System.out.println("aHeadBody = " + aHeadBody + "itsTotalTargetCoverage = " + itsTotalTargetCoverage + " getTruePositiveRate = " + aHeadBody / itsTotalTargetCoverage);
		return aHeadBody / itsTotalTargetCoverage;
	}

	private float getFalsePositiveRate(Subgroup theSubgroup)
	{
		BitSet tmp = (BitSet) itsBinaryTarget.clone();
		tmp.and(theSubgroup.getMembers());
		int aHeadBody = tmp.cardinality();

		System.out.println("getFalsePositiveRate = " + (itsTotalTargetCoverage - aHeadBody) / (itsTotalCoverage - itsTotalTargetCoverage));
		return (itsTotalTargetCoverage - aHeadBody) / (itsTotalCoverage - itsTotalTargetCoverage);
	}

	public float getFalsePositiveRateAt(int theIndex)
	{
		if (theIndex == -1)
			return 0.0F;
		if (theIndex == size())
			return 1.0F;
		else
			return 0.0F;//getRule(theIndex).getFalsePositiveRate();
	}
}
