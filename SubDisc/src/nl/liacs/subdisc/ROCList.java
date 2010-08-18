package nl.liacs.subdisc;

import java.util.ArrayList;

public class ROCList extends ArrayList<Subgroup>
{
	private static final long serialVersionUID = 1L;

	public ROCList(ArrayList<Subgroup> aSubgroupList) { super.addAll(aSubgroupList); }

	@Override
	public boolean add(Subgroup theSubgroup)
	{
		float aTPR = theSubgroup.getTruePositiveRate();
		float aFPR = theSubgroup.getFalsePositiveRate();

		if (aTPR <= aFPR) //always under curve
			return false;

		super.add(theSubgroup);
		if (size() == 1)
			return true;
		else
		{
//			boolean addOrSet = false;
			int anIndex = size() - 1;

			//move new rule from end to correct position
			Float theOtherFPR = get(anIndex - 1).getFalsePositiveRate();
			while ((anIndex > 0) && (theOtherFPR >= aFPR))
			{
				Subgroup anotherSubgroup = get(anIndex - 1);
				set(anIndex - 1, theSubgroup);
				set(anIndex, anotherSubgroup);
				anIndex--;
			}

			if (get(anIndex + 1).getFalsePositiveRate() == get(anIndex).getFalsePositiveRate())
			{
				if (get(anIndex + 1).getTruePositiveRate() > get((anIndex)).getTruePositiveRate())
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

			return true;
		}
	}

	private float getSlopeAt(int theIndex)
	{
		if(size() == 0)
			return 0.0F;
		if(theIndex == -1)
			return get(0).getTruePositiveRate()/get(0).getFalsePositiveRate();
		if(theIndex == size())
			return 0.0F;
		if(theIndex == size() - 1)
			return (1.0F - get(theIndex).getTruePositiveRate())/
					(1.0F - get(theIndex).getFalsePositiveRate());
		if(get(theIndex + 1).getFalsePositiveRate() == get(theIndex ).getFalsePositiveRate())
			return 0.0F;
		return (get(theIndex + 1).getTruePositiveRate() - get(theIndex).getTruePositiveRate())/
				(get(theIndex + 1).getFalsePositiveRate() - get(theIndex).getFalsePositiveRate());
	}

	public float getAreaUnderCurve()
	{
		float anArea = 0.0F;

		for (int i = -1; i < size(); i++)
		{
			float aWidth = get(i+i).getFalsePositiveRate() - get(i).getFalsePositiveRate();
			anArea += aWidth * ((get(i).getTruePositiveRate() + get(i + 1).getTruePositiveRate())/2.0F);
		}
		return anArea;
	}
}
