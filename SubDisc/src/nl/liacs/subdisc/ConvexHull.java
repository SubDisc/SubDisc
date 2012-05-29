package nl.liacs.subdisc;

import java.util.*;


/*
 Simple class for 2D points, having 2 labels.
 */
class HullPoint
{
	public float itsLabel1;
	public float itsLabel2;
	public float itsX;
	public float itsY;
	public HullPoint() { itsLabel1 = 0; itsLabel2 = 0; itsX = 0; itsY = 0; }
}


/*
 Class containing for maintaining and constructing convex hulls in 2D.
 A hull is split into an upper and lower part for convenience.
 */
public class ConvexHull
{
	private HullPoint [][] itsHullPoints;
	private int[] itsLength;

	/* thePoints structure:
	 - constains upper & lower hull
	 - points given as label;x;y
	 - sorted by x coord
	 */
	public ConvexHull(float[][][] thePoints)
	{
		itsHullPoints = new HullPoint[2][];
		for (int aSide = 0; aSide < 2; aSide++)
		{
			itsLength[aSide] = thePoints[aSide].length;
			itsHullPoints[aSide] = new HullPoint[itsLength[aSide]];
			for (int i = 0; i < itsLength[aSide]; i++)
			{
				itsHullPoints[aSide][i].itsLabel1 = thePoints[aSide][i][0];
				itsHullPoints[aSide][i].itsX = thePoints[aSide][i][1];
				itsHullPoints[aSide][i].itsY = thePoints[aSide][i][2];
			}
		}
		
		return;
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
			while (aNextList[aCurr] < itsLength[aSide] - 1)
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
					if (aCurr > 0) {
						aCurr = aPrevList[aCurr];
						aPruneCnt++;
					}
				}
			}

			// put convexhullpoints in a new list
			itsLength[aSide] -= aPruneCnt;
			itsHullPoints[aSide] = new HullPoint[itsLength[aSide]];
			aCurr = 0;
			int i = 0;
			while (i < itsLength[aSide]) {
				itsHullPoints[aSide][i] = itsHullPoints[aSide][aCurr];
				aCurr = aNextList[aCurr];
				i++;
			}

		}

		return;
	}

	/* assumes this.x < theOther.x, i.e., no overlap between the hulls
	   hence linear time complexity
	 */
	public void appendOtherConvexHull(ConvexHull theOther)
	{
		for (int aSide = 0; aSide < 2; aSide++)
		{
			HullPoint [] aNewHullPoints = new HullPoint[itsLength[aSide] + theOther.itsLength[aSide]];
			for (int i = 0; i < itsLength[aSide]; i++)
				aNewHullPoints[i] = itsHullPoints[aSide][i];
			for (int i = 0; i < theOther.itsLength[aSide]; i++)
				aNewHullPoints[itsLength[aSide]+i] = theOther.itsHullPoints[aSide][i];
			itsHullPoints[aSide] = aNewHullPoints;
			itsLength[aSide] += theOther.itsLength[aSide];
		}

		grahamScanSorted();

		return;
	}

	/*
	 Compute the Minkowski difference of two convex polygons.
	 Again, linear time complexity.
	 */
	private void minkowskiDifference(ConvexHull theOther)
	{
		for (int aSide = 0 ; aSide < 2; aSide++)
		{
			int aSign = (aSide==0) ? 1 : -1 ;

			int aNewSize = itsLength[aSide] + theOther.itsLength[1-aSide];
			HullPoint[] aHull = new HullPoint[aNewSize];
			int aHullSize = 0;

			int i = 0;
			int j = theOther.itsLength[1-aSide] - 1;
			while (i < itsLength[aSide] - 1 || j > 0)
			{
				float aSlope1, aSlope2;
				
				if (i == itsLength[aSide]-1)
					aSlope1 = -1; // dummy for last
				else
					aSlope1 = (itsHullPoints[aSide][i+1].itsX - itsHullPoints[aSide][i].itsX) / (itsHullPoints[aSide][i+1].itsX - itsHullPoints[aSide][i].itsX + itsHullPoints[aSide][i+1].itsY - itsHullPoints[aSide][i].itsY);
				if (j == 0)
					aSlope2 = -1; // dummy for last
				else
					aSlope2 = (theOther.itsHullPoints[1-aSide][j-1].itsX - theOther.itsHullPoints[1-aSide][j].itsX) / (theOther.itsHullPoints[1-aSide][j-1].itsX - theOther.itsHullPoints[1-aSide][j].itsX + theOther.itsHullPoints[1-aSide][j-1].itsY - theOther.itsHullPoints[1-aSide][j].itsY);

				if (aSign * aSlope1 >= aSign * aSlope2)
				{
					aHull[aHullSize].itsLabel1 = itsHullPoints[aSide][i].itsLabel1;
					aHull[aHullSize].itsLabel2 = aSide; 
					aHull[aHullSize].itsX = itsHullPoints[aSide][i].itsX;
					aHull[aHullSize].itsY = itsHullPoints[aSide][i].itsY;
					aHullSize++;
					i++;
				}
				if (aSign * aSlope1 <= aSign * aSlope2)
				{
					aHull[aHullSize].itsLabel1 = theOther.itsHullPoints[1-aSide][j].itsLabel1;
					aHull[aHullSize].itsLabel2 = 1 - aSide;
					aHull[aHullSize].itsX = theOther.itsHullPoints[1-aSide][j].itsX;
					aHull[aHullSize].itsY = theOther.itsHullPoints[1-aSide][j].itsY;
					aHullSize++;
					j--;
				}
			}
			aHull[aHullSize].itsLabel1 = itsHullPoints[aSide][i].itsLabel1;
			aHull[aHullSize].itsLabel2 = aSide;
			aHull[aHullSize].itsX = itsHullPoints[aSide][i].itsX;
			aHull[aHullSize].itsY = itsHullPoints[aSide][i].itsY;
			aHullSize++;
			aHull[aHullSize].itsLabel1 = theOther.itsHullPoints[1-aSide][j].itsLabel1;
			aHull[aHullSize].itsLabel2 = 1 - aSide;
			aHull[aHullSize].itsX = theOther.itsHullPoints[1-aSide][j].itsX;
			aHull[aHullSize].itsY = theOther.itsHullPoints[1-aSide][j].itsY;
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
				aNewHull[k].itsX = aHull[aVertex].itsX - aHull[aNextVertex].itsX;
				aNewHull[k].itsY = aHull[aVertex].itsX - aHull[aNextVertex].itsY;
				// set its own label and the other's label
				// for intevals these are the rhs and lhs end points, resp.
				aNewHull[k].itsLabel1 = aHull[aNextVertex].itsLabel1;
				aNewHull[k].itsLabel2 = aHull[aVertex].itsLabel1;
			}

			itsHullPoints[aSide] = aNewHull;
			itsLength[aSide] = aHullSize;

		}

		// should actually only remove the degenerate hull points
		grahamScanSorted();

		return;
	}

}
