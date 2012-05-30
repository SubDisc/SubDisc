package nl.liacs.subdisc;

import java.util.*;


/*
 Simple class for 2D points, having 2 labels.
 */
class HullPoint
{
	public float itsX;
	public float itsY;
	public float itsLabel1;
	public float itsLabel2;

	public HullPoint(float theX, float theY, float theLabel1, float theLabel2) 
	{
		itsX = theX;
		itsY = theY;
		itsLabel1 = theLabel1;
		itsLabel2 = theLabel2;
	}

	public HullPoint() {
		this(0, 0, 0, 0);
	}

	public HullPoint(HullPoint theOther)
	{
		this(theOther.itsX, theOther.itsY, theOther.itsLabel1, theOther.itsLabel2);
	}
	
	public void print()
	{
		Log.logCommandLine("HullPoint (" + itsX + "," + itsY + ") " + itsLabel1 + ", " + itsLabel2);
	}

}



/*
 Class containing for maintaining and constructing convex hulls in 2D.
 A hull is split into an upper and lower part for convenience.
 Sorted by x coordinate.
 */
public class ConvexHull
{
	private HullPoint [][] itsHullPoints;
	private int[] itsLength;
	private static final float itsDefaultLabel = Float.NEGATIVE_INFINITY;


	private ConvexHull()
	{
		itsHullPoints = new HullPoint[2][];
		itsLength = new int[2];
		
		return;
	}

	/* construct single point hull
	 */
	public ConvexHull(float theX, float theY, float theLabel1, float theLabel2)
	{
		this();

		for (int aSide = 0; aSide < 2; aSide++)
		{
			itsHullPoints[aSide] = new HullPoint[1];
			itsLength[aSide] = 1;
			itsHullPoints[aSide][0] = new HullPoint(theX, theY, theLabel1, theLabel2);
		}

		return;
	}


	public int getSize(int theSide)
	{
		return itsLength[theSide];
	}


	public HullPoint getPoint(int theSide, int theIndex)
	{
		return itsHullPoints[theSide][theIndex];
	}


	/* assumes points on upper and lower hull are already 
	   sorted by x coord hence linear time complexity
	 */
	public void grahamScanSorted()
	{
		for (int aSide = 0; aSide < 2; aSide++)
		{
			if (itsLength[aSide] < 3)
				continue;

			int aSign = (aSide == 0) ? 1 : -1;

			int aPruneCnt = 0;
			int[] aNextList = new int[itsLength[aSide]];
			int[] aPrevList = new int[itsLength[aSide]];
			for (int i = 0; i < itsLength[aSide]; i++) {
				aNextList[i] = i + 1;
				aPrevList[i] = i - 1;
			}

			int aCurr = 0;
			while (aNextList[aCurr] < itsLength[aSide] && aNextList[aNextList[aCurr]] < itsLength[aSide] )
			{
				float aX1 = itsHullPoints[aSide][aCurr].itsX;
				float aY1 = itsHullPoints[aSide][aCurr].itsY;
				float aX2 = itsHullPoints[aSide][aNextList[aCurr]].itsX;
				float aY2 = itsHullPoints[aSide][aNextList[aCurr]].itsY;
				float aX3 = itsHullPoints[aSide][aNextList[aNextList[aCurr]]].itsX;
				float aY3 = itsHullPoints[aSide][aNextList[aNextList[aCurr]]].itsY;
				
				if ( aSign * (aY2-aY1) * (aX3-aX2) > aSign * (aY3-aY2) * (aX2-aX1) ) //convex, go to next point
				{
					aCurr = aNextList[aCurr];
				}
				else // not convex, remove middle point, go to previous point
				{
					aPrevList[aNextList[aNextList[aCurr]]] = aCurr;
					aNextList[aCurr] = aNextList[aNextList[aCurr]];
					aPruneCnt++;
					if (aCurr > 0)
						aCurr = aPrevList[aCurr];
				}
			}

			// put convexhullpoints in a new list
			itsLength[aSide] -= aPruneCnt;
			HullPoint [] aNewHullPoints = new HullPoint[itsLength[aSide]];
			aCurr = 0;
			int i = 0;
			while (i < itsLength[aSide])
			{
				aNewHullPoints[i] = itsHullPoints[aSide][aCurr];
				aCurr = aNextList[aCurr];
				i++;
			}
			itsHullPoints[aSide] = aNewHullPoints;

		}

		return;
	}


	/* assumes this.x < theOther.x, i.e., no overlap between the hulls
	   hence linear time complexity
	 */
	public ConvexHull concatenate(ConvexHull theOther)
	{
		ConvexHull aResult = new ConvexHull();
		
		for (int aSide = 0; aSide < 2; aSide++)
		{
			aResult.itsLength[aSide] = itsLength[aSide] + theOther.itsLength[aSide];
			aResult.itsHullPoints[aSide] = new HullPoint[aResult.itsLength[aSide]];
			for (int i = 0; i < itsLength[aSide]; i++)
				aResult.itsHullPoints[aSide][i] = itsHullPoints[aSide][i];
			for (int i = 0; i < theOther.itsLength[aSide]; i++)
				aResult.itsHullPoints[aSide][itsLength[aSide]+i] = theOther.itsHullPoints[aSide][i];
		}

		aResult.grahamScanSorted();

		return aResult;
	}


	/*
	 Compute the Minkowski difference of two convex polygons.
	 Again, linear time complexity.
	 */
	public ConvexHull minkowskiDifference(ConvexHull theOther)
	{
		return minkowskiDifference(theOther, true);
	}


	public ConvexHull minkowskiDifference(ConvexHull theOther, boolean thePruneDegenerate)
	{
		ConvexHull aResult = new ConvexHull();

		for (int aSide = 0 ; aSide < 2; aSide++)
		{
			int aSign = (aSide==0) ? 1 : -1 ;

			int aNewSize = itsLength[aSide] + theOther.itsLength[1-aSide];
			HullPoint[] aHull = new HullPoint[aNewSize];
			int aHullSize = 0;

			int i = 0;
			int j = theOther.itsLength[1-aSide] - 1;
			float aSlope1, aSlope2;
			while (i < itsLength[aSide] - 1 || j > 0)
			{
				if (i == itsLength[aSide]-1)
					aSlope1 = aSign * Float.NEGATIVE_INFINITY; // dummy for last
				else
					aSlope1 = (itsHullPoints[aSide][i+1].itsY - itsHullPoints[aSide][i].itsY) / (itsHullPoints[aSide][i+1].itsX - itsHullPoints[aSide][i].itsX + itsHullPoints[aSide][i+1].itsY - itsHullPoints[aSide][i].itsY);
				if (j == 0)
					aSlope2 = aSign * Float.NEGATIVE_INFINITY; // dummy for last
				else
					aSlope2 = (theOther.itsHullPoints[1-aSide][j-1].itsY - theOther.itsHullPoints[1-aSide][j].itsY) / (theOther.itsHullPoints[1-aSide][j-1].itsX - theOther.itsHullPoints[1-aSide][j].itsX + theOther.itsHullPoints[1-aSide][j-1].itsY - theOther.itsHullPoints[1-aSide][j].itsY);

				if (aSign * aSlope1 >= aSign * aSlope2)
				{
					aHull[aHullSize] = new HullPoint(itsHullPoints[aSide][i]);
					aHull[aHullSize].itsLabel2 = aSide; 
					aHullSize++;
					i++;
				}
				if (aSign * aSlope1 <= aSign * aSlope2)
				{
					aHull[aHullSize] = new HullPoint(theOther.itsHullPoints[1-aSide][j]);
					aHull[aHullSize].itsLabel2 = 1 - aSide;
					aHullSize++;
					j--;
				}
				
			}
			aHull[aHullSize] = new HullPoint(itsHullPoints[aSide][i]);
			aHull[aHullSize].itsLabel2 = aSide;
			aHullSize++;
			aHull[aHullSize] = new HullPoint(theOther.itsHullPoints[1-aSide][j]);
			aHull[aHullSize].itsLabel2 = 1 - aSide;
			aHullSize++;

			// build final hull
			HullPoint[] aNewHull = new HullPoint[aHullSize];

			for (int k = 0; k < aHullSize; k++)
			{
				int aVertex = k;
				int aNextVertex = (aVertex + 1) % aHullSize;
				while (aHull[aVertex].itsLabel2 == aHull[aNextVertex].itsLabel2)
					aNextVertex = (aNextVertex + 1) % aHullSize;
				if (aHull[aNextVertex].itsLabel1 >= aHull[aVertex].itsLabel1)
				{
					int tmp=aNextVertex; aNextVertex=aVertex; aVertex=tmp;
				}
				aNewHull[k] = new HullPoint();
				aNewHull[k].itsX = aHull[aVertex].itsX - aHull[aNextVertex].itsX;
				aNewHull[k].itsY = aHull[aVertex].itsY - aHull[aNextVertex].itsY;
				// set its own label and the other's label
				// for intevals these are the rhs and lhs end points, resp.
				aNewHull[k].itsLabel2 = aHull[aNextVertex].itsLabel1;
				aNewHull[k].itsLabel1 = aHull[aVertex].itsLabel1;
			}

			aResult.itsHullPoints[aSide] = aNewHull;
			aResult.itsLength[aSide] = aHullSize;

		}

		if (thePruneDegenerate)
			aResult.grahamScanSorted();

		return aResult;
	}

}
